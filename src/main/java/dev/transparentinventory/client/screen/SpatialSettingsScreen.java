package dev.transparentinventory.client.screen;

import dev.transparentinventory.client.ConfigLoader;
import dev.transparentinventory.client.InventoryConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Can render as a modal overlay without replacing/removing a live vanilla menu. */
public final class SpatialSettingsScreen extends Screen {
    private final Screen parent;
    private final boolean overlay;
    private final String screenName;
    private int page;
    private int top;

    public SpatialSettingsScreen(Screen parent, boolean overlay) {
        super(Component.translatable("spatialui.settings.title").append(" · "
                + net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("transparent_inventory")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?")));
        this.parent = parent;
        this.overlay = overlay;
        this.screenName = parent instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>
                ? parent.getClass().getName() : null;
    }

    @Override protected void init() {
        int contentWidth = Math.min(320, width - 16);
        int left = (width - contentWidth) / 2;
        top = Math.max(6, (height - 220) / 2);
        int row = top + 34;
        if (page == 0) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.opacity.global",
                    () -> 1 - InventoryConfig.get().panelOpacity, value -> InventoryConfig.get().panelOpacity = (float) (1 - value), true));
            var current = addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.opacity.current",
                    () -> screenName == null ? 0 : 1 - InventoryConfig.get().opacityFor(screenName),
                    value -> { if (screenName != null) InventoryConfig.get().screenOpacities.put(screenName, (float) (1 - value)); }, true));
            current.active = screenName != null;
            current.setTooltip(Tooltip.create(Component.translatable("spatialui.opacity.current.hint")));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.opacity.dim",
                    () -> InventoryConfig.get().worldDimOpacity, value -> InventoryConfig.get().worldDimOpacity = (float) value, true));
            addRenderableWidget(Button.builder(Component.translatable("spatialui.opacity.inherit"), button -> {
                if (screenName != null) InventoryConfig.get().screenOpacities.remove(screenName);
            }).bounds(left, row + 68, contentWidth / 2 - 2, 20).build()).active = screenName != null;
            addRenderableWidget(Button.builder(Component.translatable("spatialui.settings.reset"), button -> {
                InventoryConfig.setInstance(new InventoryConfig());
                rebuildWidgets();
            }).bounds(left + contentWidth / 2 + 2, row + 68, contentWidth / 2 - 2, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("spatialui.inventory.preset"), button -> {
                InventoryConfig.get().applyGlassPreset();
                rebuildWidgets();
            }).bounds(left, row + 90, contentWidth, 20).build());
            addRenderableWidget(Button.builder(toggle("spatialui.ui.liquid_glass", InventoryConfig.get().liquidGlassUi), button -> {
                InventoryConfig.get().liquidGlassUi = !InventoryConfig.get().liquidGlassUi;
                button.setMessage(toggle("spatialui.ui.liquid_glass", InventoryConfig.get().liquidGlassUi));
            }).bounds(left, row + 112, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.ui.liquid_glass.hint")));
        } else if (page == 1) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.scale",
                    () -> (InventoryConfig.get().guiScale - 0.5), value -> InventoryConfig.get().guiScale = 0.5 + value, false));
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.camera.distance",
                    () -> InventoryConfig.get().cameraDistance / 8, value -> InventoryConfig.get().cameraDistance = value * 8, false));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.camera.duration",
                    () -> InventoryConfig.get().cameraTransitionDurationMs / 1000.0,
                    value -> InventoryConfig.get().cameraTransitionDurationMs = (int) (value * 1000), false));
            addRenderableWidget(Button.builder(toggle("spatialui.camera", InventoryConfig.get().cameraEnabled), button -> {
                InventoryConfig.get().cameraEnabled = !InventoryConfig.get().cameraEnabled;
                button.setMessage(toggle("spatialui.camera", InventoryConfig.get().cameraEnabled));
            }).bounds(left, row + 68, contentWidth / 2 - 2, 20).build());
            addRenderableWidget(Button.builder(toggle("spatialui.anchors", InventoryConfig.get().worldAnchoringEnabled), button -> {
                InventoryConfig.get().worldAnchoringEnabled = !InventoryConfig.get().worldAnchoringEnabled;
                button.setMessage(toggle("spatialui.anchors", InventoryConfig.get().worldAnchoringEnabled));
            }).bounds(left + contentWidth / 2 + 2, row + 68, contentWidth / 2 - 2, 20).build());
            addRenderableWidget(new SettingSlider(left, row + 90, contentWidth, "spatialui.container_scale",
                    () -> (InventoryConfig.get().containerScale - 0.75) / 1.25,
                    value -> InventoryConfig.get().containerScale = 0.75 + value * 1.25, false));
        } else if (page == 2) {
            addRenderableWidget(Button.builder(toggle("spatialui.inventory.cinematic", InventoryConfig.get().cinematicInventory), button -> {
                InventoryConfig.get().cinematicInventory = !InventoryConfig.get().cinematicInventory;
                button.setMessage(toggle("spatialui.inventory.cinematic", InventoryConfig.get().cinematicInventory));
            }).bounds(left, row, contentWidth, 20).build());
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.inventory.tilt",
                    () -> (InventoryConfig.get().inventoryTiltDegrees + 12) / 24,
                    value -> InventoryConfig.get().inventoryTiltDegrees = value * 24 - 12, false));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.inventory.duration",
                    () -> InventoryConfig.get().openingAnimationDurationMs / 1000.0,
                    value -> InventoryConfig.get().openingAnimationDurationMs = (int) (value * 1000), false));
            addRenderableWidget(Button.builder(toggle("spatialui.inventory.glass", InventoryConfig.get().glassInventory), button -> {
                InventoryConfig.get().glassInventory = !InventoryConfig.get().glassInventory;
                button.setMessage(toggle("spatialui.inventory.glass", InventoryConfig.get().glassInventory));
            }).bounds(left, row + 66, contentWidth, 20).build());
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.inventory.side",
                    () -> InventoryConfig.get().inventoryCameraSide / 0.95,
                    value -> InventoryConfig.get().inventoryCameraSide = value * 0.95, true));
        } else if (page == 3) {
            addRenderableWidget(Button.builder(toggle("spatialui.look.enabled", InventoryConfig.get().playerLookAtInventory), button -> {
                InventoryConfig.get().playerLookAtInventory = !InventoryConfig.get().playerLookAtInventory;
                button.setMessage(toggle("spatialui.look.enabled", InventoryConfig.get().playerLookAtInventory));
            }).bounds(left, row, contentWidth, 20).build());
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.look.head_yaw",
                    () -> InventoryConfig.get().maxHeadYaw / 90.0,
                    value -> InventoryConfig.get().maxHeadYaw = (float) (value * 90), false));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.look.head_pitch",
                    () -> InventoryConfig.get().maxHeadPitch / 90.0,
                    value -> InventoryConfig.get().maxHeadPitch = (float) (value * 90), false));
        } else if (page == 4) {
            addRenderableWidget(Button.builder(toggle("spatialui.hotbar.glass", InventoryConfig.get().glassHotbar), button -> {
                InventoryConfig.get().glassHotbar = !InventoryConfig.get().glassHotbar;
                button.setMessage(toggle("spatialui.hotbar.glass", InventoryConfig.get().glassHotbar));
            }).bounds(left, row, contentWidth, 20).build());
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.hotbar.transparency",
                    () -> 1 - InventoryConfig.get().hotbarOpacity,
                    value -> InventoryConfig.get().hotbarOpacity = (float) (1 - value), true));
        } else if (page == 5) {
            motionToggle(left, row, contentWidth, "spatialui.motion.enabled", () -> InventoryConfig.get().motion.enabled, value -> InventoryConfig.get().motion.enabled = value);
            motionToggle(left, row + 22, contentWidth, "spatialui.motion.reduce", () -> InventoryConfig.get().motion.reduceMotion, value -> InventoryConfig.get().motion.reduceMotion = value);
            motionToggle(left, row + 44, contentWidth, "spatialui.motion.swap", () -> InventoryConfig.get().motion.itemSwap, value -> InventoryConfig.get().motion.itemSwap = value);
            motionToggle(left, row + 66, contentWidth, "spatialui.motion.attack", () -> InventoryConfig.get().motion.attackAnimation, value -> InventoryConfig.get().motion.attackAnimation = value);
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.motion.hand_intensity",
                    () -> InventoryConfig.get().motion.firstPersonIntensity, value -> InventoryConfig.get().motion.firstPersonIntensity = value, true));
        } else if (page == 6) {
            motionToggle(left, row, contentWidth, "spatialui.motion.sprint", () -> InventoryConfig.get().motion.sprintSway, value -> InventoryConfig.get().motion.sprintSway = value);
            motionToggle(left, row + 22, contentWidth, "spatialui.motion.jump", () -> InventoryConfig.get().motion.jumpMotion, value -> InventoryConfig.get().motion.jumpMotion = value);
            motionToggle(left, row + 44, contentWidth, "spatialui.motion.landing", () -> InventoryConfig.get().motion.landingMotion, value -> InventoryConfig.get().motion.landingMotion = value);
            motionToggle(left, row + 66, contentWidth, "spatialui.motion.hit", () -> InventoryConfig.get().motion.hitShake, value -> InventoryConfig.get().motion.hitShake = value);
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.motion.camera_intensity",
                    () -> InventoryConfig.get().motion.cameraIntensity, value -> InventoryConfig.get().motion.cameraIntensity = value, true));
        } else if (page == 7) {
            motionToggle(left, row, contentWidth, "spatialui.motion.break", () -> InventoryConfig.get().motion.blockBreakEffects, value -> InventoryConfig.get().motion.blockBreakEffects = value);
            motionToggle(left, row + 22, contentWidth, "spatialui.motion.place", () -> InventoryConfig.get().motion.blockPlaceAnimation, value -> InventoryConfig.get().motion.blockPlaceAnimation = value);
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.motion.particle_multiplier",
                    () -> InventoryConfig.get().motion.particleMultiplier / 2, value -> InventoryConfig.get().motion.particleMultiplier = value * 2, false));
        } else {
            playerToggle(left, row, contentWidth, "spatialui.motion.player", () -> InventoryConfig.get().motion.playerAnimations, value -> InventoryConfig.get().motion.playerAnimations = value);
            playerToggle(left, row + 22, contentWidth, "spatialui.motion.player_attack", () -> InventoryConfig.get().motion.playerAttackAnimation, value -> InventoryConfig.get().motion.playerAttackAnimation = value);
            playerToggle(left, row + 44, contentWidth, "spatialui.motion.crafting", () -> InventoryConfig.get().motion.craftingAnimation, value -> InventoryConfig.get().motion.craftingAnimation = value);
            addRenderableWidget(new SettingSlider(left, row + 66, contentWidth, "spatialui.motion.player_intensity",
                    () -> InventoryConfig.get().motion.playerIntensity, value -> InventoryConfig.get().motion.playerIntensity = value, true));
        }
        addRenderableWidget(Button.builder(Component.translatable(page == 0 ? "spatialui.settings.more"
                : page == 1 ? "spatialui.settings.entrance" : page == 2 ? "spatialui.settings.look"
                : page == 3 ? "spatialui.settings.hotbar" : page == 4 ? "spatialui.settings.motion"
                : page == 5 ? "spatialui.settings.camera_motion" : page == 6 ? "spatialui.settings.world_effects"
                : page == 7 ? "spatialui.settings.player_motion" : "spatialui.settings.opacity"), button -> {
            page = (page + 1) % 9;
            rebuildWidgets();
        }).bounds(left, row + 138, contentWidth / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + contentWidth / 2 + 2, row + 138, contentWidth / 2 - 2, 20).build());
    }

    private static Component toggle(String key, boolean enabled) {
        return Component.translatable(key).append(": ").append(Component.translatable(enabled ? "options.on" : "options.off"));
    }

    private void motionToggle(int x, int y, int width, String key, java.util.function.BooleanSupplier read, java.util.function.Consumer<Boolean> write) {
        addRenderableWidget(Button.builder(toggle(key, read.getAsBoolean()), button -> {
            write.accept(!read.getAsBoolean());
            dev.transparentinventory.client.motion.VisualMotionController.INSTANCE.reset();
            dev.transparentinventory.client.effects.BlockFeedback.clear();
            button.setMessage(toggle(key, read.getAsBoolean()));
        }).bounds(x, y, width, 20).build());
    }

    private void playerToggle(int x, int y, int width, String key, java.util.function.BooleanSupplier read, java.util.function.Consumer<Boolean> write) {
        addRenderableWidget(Button.builder(toggle(key, read.getAsBoolean()), button -> {
            write.accept(!read.getAsBoolean());
            button.setMessage(toggle(key, read.getAsBoolean()));
        }).bounds(x, y, width, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        if (dev.transparentinventory.client.render.LiquidGlassRenderer.enabled()) {
            graphics.fill(0, 0, width, height, overlay ? 0x58070E1B : 0xA0101929);
            dev.transparentinventory.client.render.LiquidGlassRenderer.surface(graphics,
                    (width - Math.min(340, width - 8)) / 2, top - 5, Math.min(340, width - 8), 218,
                    1, false, true, 0.55);
            return;
        }
        graphics.fill(0, 0, width, height, overlay ? 0x90000000 : 0xF0101520);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.centeredText(font, title, width / 2, top, 0xFFFFFFFF);
        Component context = screenName == null ? Component.translatable("spatialui.settings.global.hint")
                : Component.translatable(InventoryConfig.get().screenOpacities.containsKey(screenName) ? "spatialui.settings.custom" : "spatialui.settings.inherited", parent.getTitle());
        graphics.centeredText(font, context, width / 2, top + 15, 0xFFCCD5E0);
        super.extractRenderState(graphics, mouseX, mouseY, tick);
        graphics.centeredText(font, Component.translatable("spatialui.settings.hint"), width / 2, top + 200, 0xFFB9C7D4);
    }

    @Override public void onClose() {
        boolean saved = ConfigLoader.save();
        if (!saved && minecraft.player != null) minecraft.player.sendSystemMessage(Component.translatable("spatialui.settings.save_failed"));
        if (overlay) SpatialScreenManager.INSTANCE.closeSettings();
        else dev.transparentinventory.client.compat.MinecraftAccess.screen(minecraft, parent);
    }

    @Override public boolean isPauseScreen() { return !overlay && parent != null && parent.isPauseScreen(); }

    private static final class SettingSlider extends AbstractSliderButton {
        private final String label;
        private final DoubleSupplier read;
        private final DoubleConsumer write;
        private final boolean percentage;
        SettingSlider(int x, int y, int width, String label, DoubleSupplier read, DoubleConsumer write, boolean percentage) {
            super(x, y, width, 20, Component.empty(), read.getAsDouble());
            this.label = label; this.read = read; this.write = write; this.percentage = percentage;
            updateMessage();
        }
        @Override protected void updateMessage() {
            String number = label.endsWith("container_scale") ? Math.round((0.75 + value * 1.25) * 100) + "%"
                    : label.endsWith("particle_multiplier") ? String.format(java.util.Locale.ROOT, "%.2f×", value * 2)
                    : label.endsWith("head_yaw") || label.endsWith("head_pitch") ? Math.round(value * 90) + "°"
                    : percentage ? Math.round(value * 100) + "%"
                    : label.endsWith("duration") ? Math.round(value * 1000) + " ms"
                    : label.endsWith("tilt") ? String.format(java.util.Locale.ROOT, "%.1f°", value * 24 - 12)
                    : String.format(java.util.Locale.ROOT, "%.2f", label.endsWith("distance") ? value * 8 : value + 0.5);
            setMessage(Component.translatable(label).append(": " + number));
        }
        @Override protected void applyValue() { write.accept(percentage ? Math.round(value * 100) / 100.0 : value); }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int x, int y, float tick) {
            value = read.getAsDouble();
            updateMessage();
            super.extractWidgetRenderState(graphics, x, y, tick);
        }
    }
}
