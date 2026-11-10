package dev.transparentinventory.client.screen;

import dev.transparentinventory.client.ConfigLoader;
import dev.transparentinventory.client.ConfigTransfer;
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
    private Button inheritButton;

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
        page = SettingsPages.clamp(page);
        int row = top + 34;
        inheritButton = null;
        if (page == 0) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.opacity.global",
                    () -> 1 - InventoryConfig.get().panelOpacity, value -> InventoryConfig.get().panelOpacity = (float) (1 - value), true));
            var current = addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.opacity.current",
                    () -> screenName == null ? 0 : 1 - InventoryConfig.get().opacityFor(screenName),
                    value -> {
                        if (screenName == null) return;
                        InventoryConfig.get().screenOpacities.put(screenName, (float) (1 - value));
                        if (inheritButton != null) inheritButton.active = true;
                    }, true));
            current.active = screenName != null;
            current.setTooltip(Tooltip.create(Component.translatable("spatialui.opacity.current.hint")));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.opacity.dim",
                    () -> InventoryConfig.get().worldDimOpacity, value -> InventoryConfig.get().worldDimOpacity = (float) value, true));
            inheritButton = addRenderableWidget(Button.builder(Component.translatable("spatialui.opacity.inherit"), button -> {
                if (screenName != null) InventoryConfig.get().screenOpacities.remove(screenName);
                button.active = false;
            }).bounds(left, row + 68, contentWidth / 2 - 2, 20).build());
            inheritButton.active = screenName != null && InventoryConfig.get().hasScreenOverride(screenName);
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
            addRenderableWidget(Button.builder(toggle("spatialui.camera.container", InventoryConfig.get().containerCameraEnabled), button -> {
                InventoryConfig.get().containerCameraEnabled = !InventoryConfig.get().containerCameraEnabled;
                button.setMessage(toggle("spatialui.camera.container", InventoryConfig.get().containerCameraEnabled));
            }).bounds(left, row + 112, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.camera.container.hint")));
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
            var reopen = addRenderableWidget(new SettingSlider(left, row + 110, contentWidth, "spatialui.inventory.reopen",
                    () -> InventoryConfig.get().reopenWindowFraction(),
                    value -> InventoryConfig.get().setReopenWindowFraction(value), false));
            reopen.setTooltip(Tooltip.create(Component.translatable("spatialui.inventory.reopen.hint")));
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
            addRenderableWidget(new SettingSlider(left, row + 66, contentWidth, "spatialui.look.smoothing",
                    () -> (InventoryConfig.get().lookSmoothing - 1) / 39.0,
                    value -> InventoryConfig.get().lookSmoothing = 1 + value * 39, false));
        } else if (page == 4) {
            addRenderableWidget(Button.builder(toggle("spatialui.hotbar.glass", InventoryConfig.get().glassHotbar), button -> {
                InventoryConfig.get().glassHotbar = !InventoryConfig.get().glassHotbar;
                button.setMessage(toggle("spatialui.hotbar.glass", InventoryConfig.get().glassHotbar));
            }).bounds(left, row, contentWidth, 20).build());
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.hotbar.transparency",
                    () -> 1 - InventoryConfig.get().hotbarOpacity,
                    value -> InventoryConfig.get().hotbarOpacity = (float) (1 - value), true));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.hotbar.border",
                    () -> InventoryConfig.get().hotbarBorderOpacity,
                    value -> InventoryConfig.get().hotbarBorderOpacity = (float) value, true));
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
        } else if (page == 8) {
            playerToggle(left, row, contentWidth, "spatialui.motion.player", () -> InventoryConfig.get().motion.playerAnimations, value -> InventoryConfig.get().motion.playerAnimations = value);
            playerToggle(left, row + 22, contentWidth, "spatialui.motion.player_attack", () -> InventoryConfig.get().motion.playerAttackAnimation, value -> InventoryConfig.get().motion.playerAttackAnimation = value);
            playerToggle(left, row + 44, contentWidth, "spatialui.motion.crafting", () -> InventoryConfig.get().motion.craftingAnimation, value -> InventoryConfig.get().motion.craftingAnimation = value);
            playerToggle(left, row + 66, contentWidth, "spatialui.motion.sprint_stride", () -> InventoryConfig.get().motion.sprintStride, value -> InventoryConfig.get().motion.sprintStride = value);
            playerToggle(left, row + 110, contentWidth, "spatialui.motion.airborne", () -> InventoryConfig.get().motion.airborneMotion, value -> InventoryConfig.get().motion.airborneMotion = value);
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.motion.player_intensity",
                    () -> InventoryConfig.get().motion.playerIntensity, value -> InventoryConfig.get().motion.playerIntensity = value, true));
        } else if (page == SettingsPages.CAMERA_ANGLES) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.camera.yaw_offset",
                    () -> (InventoryConfig.get().cameraYawOffset + 60) / 120.0,
                    value -> InventoryConfig.get().cameraYawOffset = value * 120 - 60, false));
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.camera.pitch_offset",
                    () -> (InventoryConfig.get().cameraPitchOffset + 45) / 90.0,
                    value -> InventoryConfig.get().cameraPitchOffset = value * 90 - 45, false));
        } else {
            var exclude = addRenderableWidget(Button.builder(exclusionLabel(), button -> {
                InventoryConfig.get().toggleScreenExclusion(screenName);
                button.setMessage(exclusionLabel());
                button.active = screenName != null && (!InventoryConfig.get().isScreenExcluded(screenName)
                        || InventoryConfig.get().hasExactExclusion(screenName));
            }).bounds(left, row, contentWidth, 20).build());
            exclude.setTooltip(Tooltip.create(Component.translatable("spatialui.compat.exclude.hint")));
            exclude.active = screenName != null && (!InventoryConfig.get().isScreenExcluded(screenName)
                    || InventoryConfig.get().hasExactExclusion(screenName));
            addRenderableWidget(Button.builder(Component.translatable("spatialui.compat.clear_exclusions"), button -> {
                InventoryConfig.get().clearScreenExclusions();
                rebuildWidgets();
            }).bounds(left, row + 22, contentWidth, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("spatialui.compat.clear_opacity"), button -> {
                InventoryConfig.get().clearScreenOpacities();
                rebuildWidgets();
            }).bounds(left, row + 44, contentWidth, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("spatialui.transfer.copy"), button -> {
                minecraft.keyboardHandler.setClipboard(ConfigTransfer.export(InventoryConfig.get()));
                notify("spatialui.transfer.copied");
            }).bounds(left, row + 66, contentWidth, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("spatialui.transfer.paste"), button -> {
                InventoryConfig parsed = ConfigTransfer.parse(minecraft.keyboardHandler.getClipboard());
                if (parsed == null) {
                    notify("spatialui.transfer.invalid");
                    return;
                }
                InventoryConfig.setInstance(parsed);
                ConfigLoader.save();
                rebuildWidgets();
                notify("spatialui.transfer.pasted");
            }).bounds(left, row + 88, contentWidth, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("spatialui.transfer.restore"), button -> {
                InventoryConfig backup = ConfigLoader.loadBackup();
                if (backup == null) {
                    notify("spatialui.transfer.no_backup");
                    return;
                }
                InventoryConfig.setInstance(backup);
                ConfigLoader.save();
                rebuildWidgets();
                notify("spatialui.transfer.restored");
            }).bounds(left, row + 110, contentWidth, 20).build());
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = SettingsPages.previous(page);
            rebuildWidgets();
        }).bounds(left, row + 138, 20, 20).build());
        previous.setTooltip(Tooltip.create(Component.translatable("spatialui.settings.previous")
                .append(": ").append(Component.translatable(SettingsPages.previousLabelKey(page)))));
        var forward = addRenderableWidget(Button.builder(
                Component.translatable(SettingsPages.nextLabelKey(page)), button -> {
            page = SettingsPages.next(page);
            rebuildWidgets();
        }).bounds(left + 22, row + 138, contentWidth / 2 - 24, 20).build());
        forward.setTooltip(Tooltip.create(Component.translatable("spatialui.settings.next")
                .append(": ").append(Component.translatable(SettingsPages.nextLabelKey(page)))));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + contentWidth / 2 + 2, row + 138, contentWidth / 2 - 2, 20).build());
    }

    /** Short chat feedback so copy/paste results are visible without leaving the screen. */
    private void notify(String key) {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.translatable(key));
        }
    }

    /** Label for the exclusion action; disabled states explain why nothing happens. */
    private Component exclusionLabel() {
        var config = InventoryConfig.get();
        if (screenName == null) return Component.translatable("spatialui.compat.exclude");
        if (config.hasExactExclusion(screenName)) return Component.translatable("spatialui.compat.included");
        if (config.isScreenExcluded(screenName)) return Component.translatable("spatialui.compat.excluded_rule");
        return Component.translatable("spatialui.compat.exclude");
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
        // The transparency page explains the current screen; every other page names itself,
        // so the header always answers "where am I?" even when opened from the HUD.
        var source = screenName == null || page != 0 ? null : InventoryConfig.get().opacitySource(screenName);
        Component context = source == null
                ? Component.translatable(page == 0 ? "spatialui.settings.global.hint" : SettingsPages.titleKey(page))
                : Component.translatable(switch (source) {
                    case EXACT -> "spatialui.settings.custom";
                    case RULE -> "spatialui.settings.rule";
                    case GLOBAL -> "spatialui.settings.inherited";
                }, parent.getTitle());
        graphics.centeredText(font, context, width / 2, top + 15, 0xFFCCD5E0);
        super.extractRenderState(graphics, mouseX, mouseY, tick);
        graphics.centeredText(font, Component.translatable("spatialui.settings.page", page + 1, SettingsPages.COUNT)
                .append(" · ").append(Component.translatable("spatialui.settings.hint")), width / 2, top + 200, 0xFFB9C7D4);
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
                    : percentage || label.endsWith("smoothing") ? Math.round(value * 100) + "%"
                    : label.endsWith("yaw_offset") ? Math.round(value * 120 - 60) + "°"
                    : label.endsWith("pitch_offset") ? Math.round(value * 90 - 45) + "°"
                    : label.endsWith("reopen") ? Math.round(value * 1500) + " ms"
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
