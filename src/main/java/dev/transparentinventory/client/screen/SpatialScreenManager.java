package dev.transparentinventory.client.screen;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anchor.SpatialAnchor;
import dev.transparentinventory.client.anchor.SpatialAnchorResolver;
import dev.transparentinventory.client.compat.ScreenCompatibilityManager;
import dev.transparentinventory.client.mixin.ContainerScreenAccessor;
import dev.transparentinventory.client.mixin.RecipeBookScreenAccessor;
import dev.transparentinventory.client.anim.SpatialAnimationController;
import dev.transparentinventory.client.render.SpatialTransform;
import dev.transparentinventory.client.render.InventoryEntrance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

/** Owns one screen session, including its anchor, animation and last presented transform. */
public final class SpatialScreenManager {
    public static final SpatialScreenManager INSTANCE = new SpatialScreenManager();
    private Screen screen;
    private ClientLevel sessionLevel;
    private SpatialScreenProfile profile;
    private SpatialAnchor anchor;
    private SpatialAnimationController animation;
    private SpatialTransform presented = SpatialTransform.IDENTITY;
    private double presentedImpact;
    private double flashEdgeX, flashEdgeY, flashHalfHeight;
    private boolean forceClose;
    private boolean vanillaFallback;
    private boolean presentedSpatial;
    private boolean firstPersonInventory;
    private SpatialSettingsScreen settings;
    private SpatialScreenManager() { }

    public void synchronize() {
        Minecraft client = Minecraft.getInstance();
        Screen current = dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
        SpatialScreenProfile resolved = client.level == null ? null : SpatialScreenRegistry.INSTANCE.resolve(current);
        if (current == screen && resolved == profile && sessionLevel == client.level) return;
        // Vanilla may redirect InventoryScreen to CreativeModeInventoryScreen after
        // the camera has already switched to third person. Preserve the opening intent.
        boolean inventoryHandoff = sessionLevel == client.level && firstPersonInventory && profile != null
                && resolved != null && profile.anchor() == dev.transparentinventory.client.anchor.AnchorType.PLAYER
                && resolved.anchor() == dev.transparentinventory.client.anchor.AnchorType.PLAYER;
        if (current != screen || sessionLevel != client.level) dismissSettings();
        screen = current;
        sessionLevel = client.level;
        profile = resolved;
        firstPersonInventory = resolved != null && resolved.anchor() == dev.transparentinventory.client.anchor.AnchorType.PLAYER
                && (client.options.getCameraType().isFirstPerson() || inventoryHandoff);
        presented = SpatialTransform.IDENTITY;
        presentedSpatial = false;
        forceClose = false;
        vanillaFallback = false;
        animation = resolved == null ? null : inventoryHandoff && animation != null && !animation.closing()
                ? animation : new SpatialAnimationController(System.nanoTime());
        anchor = resolved == null ? SpatialAnchor.screenSpace() : SpatialAnchorResolver.resolve(resolved.anchor());
        if (resolved == null) SpatialAnchorResolver.clear();
    }

    private boolean supported(Screen candidate) {
        return candidate != null && candidate == screen && profile != null && !ScreenCompatibilityManager.failed(candidate);
    }
    public boolean active(Screen candidate) { return supported(candidate) && !vanillaFallback; }
    public boolean active() { return active(dev.transparentinventory.client.compat.MinecraftAccess.screen(Minecraft.getInstance())); }
    public boolean closing() { return active() && animation.closing(); }
    public SpatialTransform transform() { return dev.transparentinventory.client.compat.MinecraftAccess.screen(Minecraft.getInstance()) == screen ? presented : SpatialTransform.IDENTITY; }
    /** Impact pulse [0,1] right after the panel lands; always 0 outside cinematic mode. */
    public double impactFlash() { return active() ? presentedImpact : 0; }
    public double flashEdgeX() { return flashEdgeX; }
    public double flashEdgeY() { return flashEdgeY; }
    public double flashHalfHeight() { return flashHalfHeight; }
    public SpatialAnchor anchor() { return anchor; }
    public SpatialSettingsScreen settings() { return settings; }
    public boolean cinematicInventory() { return firstPersonInventory && InventoryConfig.get().cinematicInventory; }
    public boolean mapsInput() { return presentedSpatial && dev.transparentinventory.client.compat.MinecraftAccess.screen(Minecraft.getInstance()) == screen && settings == null; }
    public boolean blocksInput() { return closing() && settings == null; }
    public Screen inputTarget(Screen original) { return settings == null ? original : settings; }

    public void toggleSettings() {
        synchronize();
        if (settings != null) { settings.onClose(); return; }
        Screen current = dev.transparentinventory.client.compat.MinecraftAccess.screen(Minecraft.getInstance());
        if (current instanceof SpatialSettingsScreen) { current.onClose(); return; }
        // The settings overlay does not replace an open container: its menu remains alive.
        if (current instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
            settings = new SpatialSettingsScreen(current, true);
            settings.init(current.width, current.height);
        } else {
            dev.transparentinventory.client.compat.MinecraftAccess.screen(Minecraft.getInstance(), new SpatialSettingsScreen(current, false));
        }
    }

    public void closeSettings() { settings = null; }
    public void dismissSettings() {
        if (settings != null) dev.transparentinventory.client.ConfigLoader.save();
        settings = null;
    }

    public boolean requestClose(Screen candidate) {
        synchronize();
        if (settings != null) { settings.onClose(); return true; }
        if (!active(candidate) || forceClose || !InventoryConfig.get().closeAnimation) return false;
        animation.close(System.nanoTime(), InventoryConfig.get().openingAnimationDurationMs);
        return true;
    }

    public boolean finishClose(Screen candidate) {
        if (active(candidate) && animation.finished(System.nanoTime(), InventoryConfig.get().openingAnimationDurationMs)) {
            forceClose = true;
            candidate.onClose();
            return true;
        }
        return false;
    }

    public float backgroundOpacity() {
        if (!active()) return 1;
        return (float) (InventoryConfig.get().opacityFor(screen.getClass().getName())
                * animation.progress(System.nanoTime(), InventoryConfig.get().openingAnimationDurationMs));
    }

    public double visibility() {
        return active() ? animation.progress(System.nanoTime(), InventoryConfig.get().openingAnimationDurationMs) : 1;
    }

    public SpatialTransform prepare(Screen candidate) {
        synchronize();
        if (candidate != screen) return SpatialTransform.IDENTITY;
        if (!supported(candidate)) {
            presentedSpatial = false;
            return presented = SpatialTransform.IDENTITY;
        }
        try {
            SpatialTransform computed = compute(candidate);
            if (!computed.valid()) throw new IllegalArgumentException("Invalid spatial projection");
            presentedSpatial = !vanillaFallback;
            return presented = computed;
        } catch (RuntimeException error) {
            ScreenCompatibilityManager.disable(candidate, error);
            presentedSpatial = false;
            return presented = SpatialTransform.IDENTITY;
        }
    }

    private SpatialTransform compute(Screen candidate) {
        Minecraft client = Minecraft.getInstance();
        InventoryConfig config = InventoryConfig.get();
        double width = candidate.width, height = candidate.height;
        double originX = width / 2, originY = height / 2;
        double panelWidth = width, panelHeight = height;
        if (candidate instanceof ContainerScreenAccessor bounds) {
            panelWidth = bounds.spatial$width();
            panelHeight = bounds.spatial$height();
            originX = bounds.spatial$left() + panelWidth / 2;
            originY = bounds.spatial$top() + panelHeight / 2;
            if (candidate instanceof RecipeBookScreenAccessor recipe && recipe.spatial$recipeBook().isVisible()) {
                panelWidth += 160;
                originX -= 80;
            }
            // Creative category tabs extend above and below the main panel. Reserve
            // their full footprint, including Fabric's tab-page buttons above them.
            if (candidate instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) {
                panelHeight += 92;
                originY -= 14;
            }
        }
        double centerX = width * (0.5 + profile.offsetX());
        double centerY = height * (0.5 + profile.offsetY());
        double scale = config.guiScale * profile.scale();
        boolean readableContainer = candidate instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>
                && profile.anchor() != dev.transparentinventory.client.anchor.AnchorType.PLAYER;
        if (readableContainer) scale *= config.containerScale;
        if (cinematicInventory()) {
            vanillaFallback = false;
            long now = System.nanoTime();
            double motion = animation.motion(now, config.openingAnimationDurationMs);
            double impact = animation.impact(now);
            SpatialTransform transform = InventoryEntrance.transform(width, height, originX, originY, panelWidth, panelHeight, scale,
                    motion,
                    animation.progress(now, config.openingAnimationDurationMs), config.inventoryTiltDegrees, impact);
            presentedImpact = impact;
            flashEdgeX = transform.centerX() + transform.halfWidth(panelWidth, panelHeight);
            flashEdgeY = transform.centerY();
            flashHalfHeight = transform.halfHeight(panelWidth, panelHeight);
            return transform;
        }
        presentedImpact = 0;
        Vec3 world = config.worldAnchoringEnabled ? anchor.position(client) : null;
        boolean projected = false;
        if (world != null) {
            world = world.add(0, config.guiVerticalOffset, 0);
            Vec3 relative = world.subtract(dev.transparentinventory.client.compat.MinecraftAccess.camera(client).position());
            var forward = dev.transparentinventory.client.compat.MinecraftAccess.camera(client).forwardVector();
            double depth = relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z();
            if (depth > 0.2) {
                Vec3 point = client.gameRenderer.projectPointToScreen(world);
                if (Double.isFinite(point.x) && Double.isFinite(point.y) && Math.abs(point.x) < 1.2 && Math.abs(point.y) < 1.2) {
                    centerX = (point.x + 1) * width / 2;
                    centerY = (1 - point.y) * height / 2;
                    if (!readableContainer) scale *= Math.clamp(config.guiDistance / depth, 0.65, 1.15);
                    projected = true;
                }
            }
        }
        // An invalid/missing/off-camera anchor can explicitly opt out of the effect.
        vanillaFallback = !projected && !config.fallbackToScreenSpace && profile.anchor() != dev.transparentinventory.client.anchor.AnchorType.PLAYER;
        if (vanillaFallback) return SpatialTransform.IDENTITY;
        double progress = animation.progress(System.nanoTime(), config.openingAnimationDurationMs);
        scale *= 0.92 + 0.08 * progress;
        scale = Math.clamp(Math.min(scale, Math.min((width - 20) / panelWidth, (height - 20) / panelHeight)), 0.1, 2.0);
        double halfWidth = panelWidth * scale / 2 + 4;
        double halfHeight = panelHeight * scale / 2 + 4;
        centerX = Math.clamp(centerX, Math.min(halfWidth, width / 2), Math.max(width / 2, width - halfWidth));
        centerY = Math.clamp(centerY + (1 - progress) * 12, Math.min(halfHeight, height / 2), Math.max(height / 2, height - halfHeight));
        return new SpatialTransform(originX, originY, centerX, centerY, scale);
    }
}
