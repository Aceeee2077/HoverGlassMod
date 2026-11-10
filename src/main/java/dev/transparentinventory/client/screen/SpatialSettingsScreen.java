package dev.transparentinventory.client.screen;

import dev.transparentinventory.client.CameraPresets;
import dev.transparentinventory.client.ConfigLoader;
import dev.transparentinventory.client.ConfigTransfer;
import dev.transparentinventory.client.GlassMaterialPresets;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.PageDefaults;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
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
    private final Class<?> openedFrom;
    private int page;
    private int top;
    private Button inheritButton;
    private final java.util.List<Button> presetButtons = new java.util.ArrayList<>();
    private final java.util.List<Button> cameraPresetButtons = new java.util.ArrayList<>();
    private Button levelCameraButton;
    private Button matchRimButton;
    private Button matchOpacityButton;
    private final ConfirmPress resetConfirm = new ConfirmPress();
    private final ConfirmPress restoreConfirm = new ConfirmPress();
    private final ConfirmPress snapshotConfirm = new ConfirmPress();
    private final ConfirmPress presetConfirm = new ConfirmPress();
    private final ScrollAccumulator scroll = new ScrollAccumulator();
    /** Config JSON as it was when the screen opened, so an untouched close saves nothing. */
    private String openedJson;

    public SpatialSettingsScreen(Screen parent, boolean overlay) {
        super(Component.translatable("spatialui.settings.title").append(" · "
                + net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("transparent_inventory")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?")));
        this.parent = parent;
        this.overlay = overlay;
        this.openedFrom = parent == null ? null : parent.getClass();
        this.screenName = parent instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>
                ? parent.getClass().getName() : null;
    }

    @Override protected void init() {
        int contentWidth = Math.min(320, width - 16);
        int left = (width - contentWidth) / 2;
        top = Math.max(6, (height - 220) / 2);
        page = SettingsPages.remembered();
        if (openedJson == null) openedJson = ConfigLoader.toJson(InventoryConfig.get());
        int row = top + 34;
        inheritButton = null;
        levelCameraButton = null;
        matchRimButton = null;
        matchOpacityButton = null;
        presetButtons.clear();
        cameraPresetButtons.clear();
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
            addRenderableWidget(Button.builder(Component.translatable(
                    resetConfirm.armed() ? "spatialui.settings.reset.confirm" : "spatialui.settings.reset"), button -> {
                if (!resetConfirm.press()) {
                    button.setMessage(Component.translatable("spatialui.settings.reset.confirm"));
                    return;
                }
                InventoryConfig.setInstance(new InventoryConfig());
                rebuildWidgets();
            }).bounds(left + contentWidth / 2 + 2, row + 68, contentWidth / 2 - 2, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.settings.reset.hint")));
            addRenderableWidget(Button.builder(Component.translatable(presetConfirm.armed()
                    ? "spatialui.inventory.preset.confirm" : "spatialui.inventory.preset"), button -> {
                if (!presetConfirm.press()) {
                    button.setMessage(Component.translatable("spatialui.inventory.preset.confirm"));
                    return;
                }
                InventoryConfig.get().applyGlassPreset();
                rebuildWidgets();
            }).bounds(left, row + 90, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.inventory.preset.hint")));
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
                    () -> InventoryConfig.get().cameraTransitionDurationMs / 2000.0,
                    value -> InventoryConfig.get().cameraTransitionDurationMs = (int) (value * 2000), false));
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
            }).bounds(left, row, contentWidth / 2 - 2, 20).build());
            addRenderableWidget(Button.builder(toggle("spatialui.inventory.close", InventoryConfig.get().closeAnimation), button -> {
                InventoryConfig.get().closeAnimation = !InventoryConfig.get().closeAnimation;
                button.setMessage(toggle("spatialui.inventory.close", InventoryConfig.get().closeAnimation));
            }).bounds(left + contentWidth / 2 + 2, row, contentWidth / 2 - 2, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.inventory.close.hint")));
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.inventory.tilt",
                    () -> (InventoryConfig.get().inventoryTiltDegrees + 12) / 24,
                    value -> InventoryConfig.get().inventoryTiltDegrees = value * 24 - 12, false));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.inventory.duration",
                    () -> InventoryConfig.get().openingAnimationDurationMs / 2000.0,
                    value -> InventoryConfig.get().openingAnimationDurationMs = (int) (value * 2000), false));
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
            matchRimButton = addRenderableWidget(Button.builder(Component.translatable("spatialui.hotbar.match_rim"), button -> {
                InventoryConfig.get().matchHotbarRim();
                rebuildWidgets();
            }).bounds(left, row + 66, contentWidth, 20).build());
            matchOpacityButton = addRenderableWidget(Button.builder(Component.translatable("spatialui.hotbar.match_opacity"), button -> {
                InventoryConfig.get().matchHotbarOpacity();
                rebuildWidgets();
            }).bounds(left, row + 88, contentWidth, 20).build());
            matchRimButton.setTooltip(Tooltip.create(Component.translatable("spatialui.hotbar.match_rim.hint")));
            matchOpacityButton.setTooltip(Tooltip.create(Component.translatable("spatialui.hotbar.match_opacity.hint")));
            refreshMatchButtons();
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
            playerToggle(left, row + 110, contentWidth / 2 - 2, "spatialui.motion.airborne", () -> InventoryConfig.get().motion.airborneMotion, value -> InventoryConfig.get().motion.airborneMotion = value);
            playerToggle(left + contentWidth / 2 + 2, row + 110, contentWidth / 2 - 2, "spatialui.motion.idle", () -> InventoryConfig.get().motion.idleSway, value -> InventoryConfig.get().motion.idleSway = value);
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.motion.player_intensity",
                    () -> InventoryConfig.get().motion.playerIntensity, value -> InventoryConfig.get().motion.playerIntensity = value, true));
        } else if (page == SettingsPages.CAMERA_ANGLES) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.camera.yaw_offset",
                    () -> (InventoryConfig.get().cameraYawOffset + 60) / 120.0,
                    value -> InventoryConfig.get().cameraYawOffset = value * 120 - 60, false));
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.camera.pitch_offset",
                    () -> (InventoryConfig.get().cameraPitchOffset + 45) / 90.0,
                    value -> InventoryConfig.get().cameraPitchOffset = value * 90 - 45, false));
            CameraPresets.Preset[] framing = CameraPresets.Preset.values();
            int gap = 4;
            int presetWidth = (contentWidth - gap * (framing.length - 1)) / framing.length;
            for (int index = 0; index < framing.length; index++) {
                CameraPresets.Preset preset = framing[index];
                String key = "spatialui.camera.preset." + preset.name().toLowerCase(java.util.Locale.ROOT);
                Button button = Button.builder(Component.translatable(key), pressed -> {
                    CameraPresets.apply(InventoryConfig.get(), preset);
                    rebuildWidgets();
                }).bounds(left + index * (presetWidth + gap), row + 44, presetWidth, 20).build();
                button.active = !CameraPresets.matches(InventoryConfig.get(), preset);
                button.setTooltip(Tooltip.create(Component.translatable(key + ".hint")));
                cameraPresetButtons.add(addRenderableWidget(button));
            }
            var levelCamera = addRenderableWidget(Button.builder(Component.translatable("spatialui.camera.level"), button -> {
                CameraPresets.level(InventoryConfig.get());
                rebuildWidgets();
            }).bounds(left, row + 66, contentWidth, 20).build());
            levelCamera.active = !CameraPresets.isLevel(InventoryConfig.get());
            levelCamera.setTooltip(Tooltip.create(Component.translatable("spatialui.camera.level.hint")));
            levelCameraButton = levelCamera;
            addRenderableWidget(Button.builder(toggle("spatialui.camera.third_person", InventoryConfig.get().thirdPersonOnOpen), button -> {
                InventoryConfig.get().thirdPersonOnOpen = !InventoryConfig.get().thirdPersonOnOpen;
                button.setMessage(toggle("spatialui.camera.third_person", InventoryConfig.get().thirdPersonOnOpen));
                dev.transparentinventory.client.camera.SpatialCameraController.INSTANCE.reset();
            }).bounds(left, row + 88, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.camera.third_person.hint")));
        } else if (page == SettingsPages.GLASS_MATERIAL) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.material.slot",
                    () -> InventoryConfig.get().slotOpacity,
                    value -> InventoryConfig.get().slotOpacity = (float) value, true));
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.material.panel_rim",
                    () -> InventoryConfig.get().borderOpacity,
                    value -> InventoryConfig.get().borderOpacity = (float) value, true));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.material.slot_rim",
                    () -> InventoryConfig.get().slotBorderOpacity,
                    value -> InventoryConfig.get().slotBorderOpacity = (float) value, true));
            addRenderableWidget(new SettingSlider(left, row + 66, contentWidth, "spatialui.material.hover",
                    () -> InventoryConfig.get().hoverOpacity,
                    value -> InventoryConfig.get().hoverOpacity = (float) value, true));
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.material.selected",
                    () -> InventoryConfig.get().selectedOpacity,
                    value -> InventoryConfig.get().selectedOpacity = (float) value, true));
            GlassMaterialPresets.Preset[] presets = GlassMaterialPresets.Preset.values();
            int gap = 4;
            int presetWidth = (contentWidth - gap * (presets.length - 1)) / presets.length;
            for (int index = 0; index < presets.length; index++) {
                GlassMaterialPresets.Preset preset = presets[index];
                String key = "spatialui.material.preset." + preset.name().toLowerCase(java.util.Locale.ROOT);
                Button button = Button.builder(Component.translatable(key), pressed -> {
                    GlassMaterialPresets.apply(InventoryConfig.get(), preset);
                    rebuildWidgets();
                }).bounds(left + index * (presetWidth + gap), row + 110, presetWidth, 20).build();
                button.active = !GlassMaterialPresets.matches(InventoryConfig.get(), preset);
                button.setTooltip(Tooltip.create(Component.translatable(key + ".hint")));
                presetButtons.add(addRenderableWidget(button));
            }
        } else if (page == SettingsPages.SCREEN_SUPPORT) {
            addRenderableWidget(Button.builder(toggle("spatialui.support.handled", InventoryConfig.get().defaultSpatialHandledScreens), button -> {
                InventoryConfig.get().defaultSpatialHandledScreens = !InventoryConfig.get().defaultSpatialHandledScreens;
                button.setMessage(toggle("spatialui.support.handled", InventoryConfig.get().defaultSpatialHandledScreens));
            }).bounds(left, row, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.support.handled.hint")));
            addRenderableWidget(Button.builder(toggle("spatialui.support.fallback", InventoryConfig.get().fallbackToScreenSpace), button -> {
                InventoryConfig.get().fallbackToScreenSpace = !InventoryConfig.get().fallbackToScreenSpace;
                button.setMessage(toggle("spatialui.support.fallback", InventoryConfig.get().fallbackToScreenSpace));
            }).bounds(left, row + 22, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.support.fallback.hint")));
            var optIn = addRenderableWidget(Button.builder(optInLabel(), button -> {
                InventoryConfig.get().toggleScreenOptIn(openedName());
                button.setMessage(optInLabel());
                button.active = canToggleOptIn();
            }).bounds(left, row + 44, contentWidth, 20).build());
            optIn.active = canToggleOptIn();
            optIn.setTooltip(Tooltip.create(Component.translatable("spatialui.support.optin.hint")));
            addRenderableWidget(Button.builder(Component.translatable("spatialui.support.key")
                            .append(": " + InventoryConfig.get().settingsKeyName()), button -> {
                InventoryConfig.get().cycleSettingsKey();
                button.setMessage(Component.translatable("spatialui.support.key")
                        .append(": " + InventoryConfig.get().settingsKeyName()));
            }).bounds(left, row + 66, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.support.key.hint")));
            addRenderableWidget(Button.builder(Component.translatable("spatialui.support.clear_optins"), button -> {
                InventoryConfig.get().clearScreenOptIns();
                rebuildWidgets();
            }).bounds(left, row + 88, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.support.clear_optins.hint")));
            addRenderableWidget(Button.builder(toggle("spatialui.enabled", InventoryConfig.get().enabled), button -> {
                InventoryConfig.get().enabled = !InventoryConfig.get().enabled;
                button.setMessage(toggle("spatialui.enabled", InventoryConfig.get().enabled));
            }).bounds(left, row + 110, contentWidth, 20).build())
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.enabled.hint")));
        } else if (page == SettingsPages.PREVIEW_LAYOUT) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.layout.player_height",
                    () -> (InventoryConfig.get().playerHeightRatio - 0.25) / 0.45,
                    value -> InventoryConfig.get().playerHeightRatio = 0.25 + value * 0.45, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.layout.player_height.hint")));
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.layout.player_x",
                    () -> (InventoryConfig.get().playerCenterXRatio - 0.50) / 0.45,
                    value -> InventoryConfig.get().playerCenterXRatio = 0.50 + value * 0.45, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.layout.player_x.hint")));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.layout.player_y",
                    () -> (InventoryConfig.get().playerCenterYRatio - 0.30) / 0.50,
                    value -> InventoryConfig.get().playerCenterYRatio = 0.30 + value * 0.50, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.layout.player_y.hint")));
            addRenderableWidget(new SettingSlider(left, row + 66, contentWidth, "spatialui.layout.panel_x",
                    () -> (InventoryConfig.get().panelCenterRatio - 0.20) / 0.45,
                    value -> InventoryConfig.get().panelCenterRatio = 0.20 + value * 0.45, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.layout.panel_x.hint")));
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.layout.recipe_book",
                    () -> InventoryConfig.get().recipeBookOffset / 160.0,
                    value -> InventoryConfig.get().recipeBookOffset = value * 160.0, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.layout.recipe_book.hint")));
        } else if (page == SettingsPages.PANEL_FEEL) {
            addRenderableWidget(new SettingSlider(left, row, contentWidth, "spatialui.feel.open_speed",
                    () -> (InventoryConfig.get().slideSpeed - 600) / 5400.0,
                    value -> InventoryConfig.get().slideSpeed = 600 + value * 5400, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.feel.open_speed.hint")));
            addRenderableWidget(new SettingSlider(left, row + 22, contentWidth, "spatialui.feel.slide_brake",
                    () -> (InventoryConfig.get().slideBrake - 200) / 7800.0,
                    value -> InventoryConfig.get().slideBrake = 200 + value * 7800, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.feel.slide_brake.hint")));
            addRenderableWidget(new SettingSlider(left, row + 44, contentWidth, "spatialui.feel.bounce",
                    () -> InventoryConfig.get().bounce / 0.6,
                    value -> InventoryConfig.get().bounce = value * 0.6, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.feel.bounce.hint")));
            addRenderableWidget(new SettingSlider(left, row + 66, contentWidth, "spatialui.feel.close_speed",
                    () -> (InventoryConfig.get().closeSpeed - 600) / 5400.0,
                    value -> InventoryConfig.get().closeSpeed = 600 + value * 5400, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.feel.close_speed.hint")));
            addRenderableWidget(new SettingSlider(left, row + 88, contentWidth, "spatialui.feel.spring",
                    () -> (InventoryConfig.get().spring - 40) / 860.0,
                    value -> InventoryConfig.get().spring = 40 + value * 860, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.feel.spring.hint")));
            addRenderableWidget(new SettingSlider(left, row + 110, contentWidth, "spatialui.feel.damping",
                    () -> (InventoryConfig.get().damping - 5) / 85.0,
                    value -> InventoryConfig.get().damping = 5 + value * 85, false))
                    .setTooltip(Tooltip.create(Component.translatable("spatialui.feel.damping.hint")));
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
            }).bounds(left, row + 66, contentWidth / 2 - 2, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("spatialui.transfer.paste"), button -> {
                InventoryConfig parsed = ConfigTransfer.parse(minecraft.keyboardHandler.getClipboard());
                if (parsed == null) {
                    notify("spatialui.transfer.invalid");
                    return;
                }
                InventoryConfig.setInstance(parsed);
                boolean saved = saveNow();
                rebuildWidgets();
                notify(saved ? "spatialui.transfer.pasted" : "spatialui.settings.save_failed");
            }).bounds(left + contentWidth / 2 + 2, row + 66, contentWidth / 2 - 2, 20).build());
            addRenderableWidget(Button.builder(Component.translatable(restoreConfirm.armed()
                    ? "spatialui.transfer.confirm" : "spatialui.transfer.restore"), button -> {
                if (!restoreConfirm.press()) {
                    button.setMessage(Component.translatable("spatialui.transfer.confirm"));
                    return;
                }
                InventoryConfig backup = ConfigLoader.loadBackup();
                if (backup == null) {
                    rebuildWidgets();
                    notify("spatialui.transfer.no_backup");
                    return;
                }
                InventoryConfig.setInstance(backup);
                boolean saved = saveNow();
                rebuildWidgets();
                notify(saved ? "spatialui.transfer.restored" : "spatialui.settings.save_failed");
            }).bounds(left, row + 88, contentWidth, 20).build());
            int snapshotCount = ConfigLoader.snapshotCount();
            var snapshotButton = addRenderableWidget(Button.builder(Component.translatable(
                    snapshotConfirm.armed() ? "spatialui.transfer.confirm" : "spatialui.transfer.snapshot")
                            .append(" (" + snapshotCount + ")"), button -> {
                if (!snapshotConfirm.press()) {
                    button.setMessage(Component.translatable("spatialui.transfer.confirm")
                            .append(" (" + snapshotCount + ")"));
                    return;
                }
                InventoryConfig snapshot = ConfigLoader.loadNewestSnapshot();
                if (snapshot == null) {
                    rebuildWidgets();
                    notify("spatialui.transfer.snapshot.none");
                    return;
                }
                InventoryConfig.setInstance(snapshot);
                boolean saved = saveNow();
                rebuildWidgets();
                notify(saved ? "spatialui.transfer.snapshot.restored" : "spatialui.settings.save_failed");
            }).bounds(left, row + 110, contentWidth, 20).build());
            snapshotButton.active = snapshotCount > 0;
            long snapshotMillis = snapshotCount > 0 ? ConfigLoader.newestSnapshotMillis() : -1;
            var snapshotHint = Component.translatable(snapshotCount > 0
                    ? "spatialui.transfer.snapshot.hint" : "spatialui.transfer.snapshot.none");
            if (snapshotMillis > 0) {
                snapshotHint = snapshotHint.append(Component.literal("\n")).append(Component.translatable(
                        "spatialui.transfer.snapshot.latest", snapshotStamp(snapshotMillis)));
            }
            snapshotButton.setTooltip(Tooltip.create(snapshotHint));
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            goTo(SettingsPages.previous(page));
        }).bounds(left, row + 138, 20, 20).build());
        previous.setTooltip(Tooltip.create(Component.translatable("spatialui.settings.previous")
                .append(": ").append(Component.translatable(SettingsPages.previousLabelKey(page)))));
        var resetPage = addRenderableWidget(Button.builder(
                Component.translatable("spatialui.settings.reset_page"), button -> {
            if (PageDefaults.reset(InventoryConfig.get(), page)) rebuildWidgets();
        }).bounds(left + 22, row + 138, 20, 20).build());
        resetPage.active = PageDefaults.supports(page);
        resetPage.setTooltip(Tooltip.create(Component.translatable(PageDefaults.supports(page)
                ? "spatialui.settings.reset_page.hint" : "spatialui.settings.reset_page.none")));
        var forward = addRenderableWidget(Button.builder(
                Component.translatable(SettingsPages.nextLabelKey(page)), button -> {
            goTo(SettingsPages.next(page));
        }).bounds(left + 44, row + 138, contentWidth / 2 - 46, 20).build());
        forward.setTooltip(Tooltip.create(Component.translatable("spatialui.settings.next")
                .append(": ").append(Component.translatable(SettingsPages.nextLabelKey(page)))));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + contentWidth / 2 + 2, row + 138, contentWidth / 2 - 2, 20).build());
    }

    /** Short chat feedback so copy/paste results are visible without leaving the screen. */
    /** Changes page, remembers it for the next open and rebuilds the controls. */
    private void goTo(int page) {
        this.page = SettingsPages.remember(page);
        scroll.reset();
        rebuildWidgets();
    }

    /**
     * Rebuilding the widgets invalidates any pending confirmation: leaving the
     * page or pressing another button must not leave the reset button armed.
     */
    @Override protected void rebuildWidgets() {
        resetConfirm.disarm();
        restoreConfirm.disarm();
        snapshotConfirm.disarm();
        presetConfirm.disarm();
        super.rebuildWidgets();
    }

    private void notify(String key) {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.translatable(key));
        }
    }

    /**
     * Saves immediately. A successful write becomes the close-time baseline; a
     * failed one keeps the old baseline so closing retries and reports the error.
     */
    private boolean saveNow() {
        boolean saved = ConfigLoader.save();
        if (saved) openedJson = ConfigLoader.toJson(InventoryConfig.get());
        return saved;
    }

    /** Label for the exclusion action; disabled states explain why nothing happens. */
    private Component exclusionLabel() {
        var config = InventoryConfig.get();
        if (screenName == null) return Component.translatable("spatialui.compat.exclude");
        if (config.hasExactExclusion(screenName)) return Component.translatable("spatialui.compat.included");
        if (config.isScreenExcluded(screenName)) return Component.translatable("spatialui.compat.excluded_rule");
        return Component.translatable("spatialui.compat.exclude");
    }

    private String openedName() { return openedFrom == null ? null : openedFrom.getName(); }

    /** Real containers already get the panel, and a wildcard rule cannot be undone here. */
    private boolean canToggleOptIn() {
        String name = openedName();
        if (name == null || screenName != null) return false;
        var config = InventoryConfig.get();
        return !config.isScreenOptedIn(name) || config.hasExactOptIn(name);
    }

    private Component optInLabel() {
        String name = openedName();
        if (name == null || screenName != null) return Component.translatable("spatialui.support.optin.container");
        var config = InventoryConfig.get();
        if (config.hasExactOptIn(name)) return Component.translatable("spatialui.support.optin.remove");
        if (config.isScreenOptedIn(name)) return Component.translatable("spatialui.support.optin.rule");
        return Component.translatable("spatialui.support.optin");
    }

    private static Component toggle(String key, boolean enabled) {
        return Component.translatable(key).append(": ").append(Component.translatable(enabled ? "options.on" : "options.off"));
    }

    /** Greys out each match button while its hotbar value already equals the panel's. */
    private void refreshMatchButtons() {
        var config = InventoryConfig.get();
        if (matchRimButton != null) {
            boolean matched = config.hotbarRimMatches();
            if (matchRimButton.active == matched) {
                matchRimButton.active = !matched;
                matchRimButton.setTooltip(Tooltip.create(Component.translatable(matched
                        ? "spatialui.hotbar.match_rim.done" : "spatialui.hotbar.match_rim.hint")));
            }
        }
        if (matchOpacityButton != null) {
            boolean matched = config.hotbarOpacityMatches();
            if (matchOpacityButton.active == matched) {
                matchOpacityButton.active = !matched;
                matchOpacityButton.setTooltip(Tooltip.create(Component.translatable(matched
                        ? "spatialui.hotbar.match_opacity.done" : "spatialui.hotbar.match_opacity.hint")));
            }
        }
    }

    /** Formats a snapshot timestamp in the player's local time, to the minute. */
    private static String snapshotStamp(long epochMillis) {
        return java.time.Instant.ofEpochMilli(epochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.ROOT));
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
        // Dragging a material slider does not rebuild the page, so refresh the
        // preset buttons each frame to keep "already applied" honest.
        GlassMaterialPresets.Preset[] presets = GlassMaterialPresets.Preset.values();
        for (int index = 0; index < presetButtons.size() && index < presets.length; index++) {
            presetButtons.get(index).active = !GlassMaterialPresets.matches(InventoryConfig.get(), presets[index]);
        }
        CameraPresets.Preset[] framing = CameraPresets.Preset.values();
        for (int index = 0; index < cameraPresetButtons.size() && index < framing.length; index++) {
            cameraPresetButtons.get(index).active = !CameraPresets.matches(InventoryConfig.get(), framing[index]);
        }
        if (levelCameraButton != null) {
            levelCameraButton.active = !CameraPresets.isLevel(InventoryConfig.get());
        }
        refreshMatchButtons();
        super.extractRenderState(graphics, mouseX, mouseY, tick);
        graphics.centeredText(font, Component.translatable("spatialui.settings.page", page + 1, SettingsPages.COUNT)
                .append(" · ").append(Component.translatable(PageDefaults.supports(page)
                        ? "spatialui.settings.hint" : "spatialui.settings.hint.no_reset")), width / 2, top + 200, 0xFFB9C7D4);
    }

    @Override public void onClose() {
        // Closing without touching anything must not rewrite the file: every save
        // copies the previous version into the rotating history and would push a
        // real snapshot out in favour of an identical one.
        boolean unchanged = openedJson != null && openedJson.equals(ConfigLoader.toJson(InventoryConfig.get()));
        boolean saved = unchanged || saveNow();
        if (!saved && minecraft.player != null) minecraft.player.sendSystemMessage(Component.translatable("spatialui.settings.save_failed"));
        if (overlay) SpatialScreenManager.INSTANCE.closeSettings();
        else dev.transparentinventory.client.compat.MinecraftAccess.screen(minecraft, parent);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = scroll.accept(scrollY);
        if (step == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        goTo(SettingsPages.shift(page, step));
        return true;
    }

    @Override public boolean keyPressed(KeyEvent key) {
        if (key.key() == InputConstants.KEY_PAGEUP) {
            goTo(SettingsPages.previous(page));
            return true;
        }
        if (key.key() == InputConstants.KEY_PAGEDOWN) {
            goTo(SettingsPages.next(page));
            return true;
        }
        if (key.key() == InputConstants.KEY_HOME) {
            goTo(0);
            return true;
        }
        if (key.key() == InputConstants.KEY_END) {
            goTo(SettingsPages.last());
            return true;
        }
        if (key.key() == InputConstants.KEY_DELETE && PageDefaults.reset(InventoryConfig.get(), page)) {
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(key);
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
            setMessage(Component.translatable(label).append(": " + SliderFormat.value(label, value, percentage)));
        }
        @Override protected void applyValue() { write.accept(percentage ? Math.round(value * 100) / 100.0 : value); }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int x, int y, float tick) {
            value = read.getAsDouble();
            updateMessage();
            super.extractWidgetRenderState(graphics, x, y, tick);
        }
    }
}
