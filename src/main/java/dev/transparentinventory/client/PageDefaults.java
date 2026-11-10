package dev.transparentinventory.client;

/**
 * Restores the values a single settings page exposes, without touching the rest
 * of the config.
 *
 * <p>Two pages are deliberately not resettable: the compatibility page holds
 * hand-written exclusions and per-screen rules, and the clipboard actions have no
 * stored values at all. Resetting everything remains available on the
 * transparency page.
 */
public final class PageDefaults {
    private PageDefaults() {
    }

    /** True when {@code page} has values that {@link #reset(InventoryConfig, int)} restores. */
    public static boolean supports(int page) {
        return switch (page) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14 -> true;
            default -> false;
        };
    }

    /**
     * Puts every control shown on {@code page} back to the value a fresh config
     * starts with.
     *
     * @return false when the page has nothing to reset, leaving the config untouched
     */
    public static boolean reset(InventoryConfig config, int page) {
        if (config == null || !supports(page)) return false;
        switch (page) {
            case 0 -> {
                config.panelOpacity = 0.18F;
                config.worldDimOpacity = 0F;
                config.screenOpacities.clear();
                config.liquidGlassUi = false;
            }
            case 1 -> {
                config.guiScale = 1.0;
                config.cameraDistance = 3.0;
                config.cameraTransitionDurationMs = 420;
                config.cameraEnabled = true;
                config.worldAnchoringEnabled = true;
                config.containerScale = 1.35;
                config.containerCameraEnabled = true;
            }
            case 2 -> {
                config.cinematicInventory = true;
                config.closeAnimation = true;
                config.inventoryTiltDegrees = 4.0;
                config.openingAnimationDurationMs = 480;
                config.glassInventory = true;
                config.inventoryCameraSide = 0.72;
                config.reopenSnapWindowMs = 400;
            }
            case 3 -> {
                config.playerLookAtInventory = true;
                config.maxHeadYaw = 50F;
                config.maxHeadPitch = 35F;
                config.lookSmoothing = 11.0;
            }
            case 4 -> {
                config.glassHotbar = true;
                config.hotbarOpacity = 0.16F;
                config.hotbarBorderOpacity = 0.72F;
            }
            case 5 -> {
                config.motion.enabled = true;
                config.motion.reduceMotion = false;
                config.motion.itemSwap = true;
                config.motion.attackAnimation = true;
                config.motion.firstPersonIntensity = 0.65;
            }
            case 6 -> {
                config.motion.sprintSway = true;
                config.motion.jumpMotion = true;
                config.motion.landingMotion = true;
                config.motion.hitShake = true;
                config.motion.cameraIntensity = 0.5;
            }
            case 7 -> {
                config.motion.blockBreakEffects = true;
                config.motion.blockPlaceAnimation = true;
                config.motion.particleMultiplier = 1;
            }
            case 8 -> {
                config.motion.playerAnimations = true;
                config.motion.playerAttackAnimation = true;
                config.motion.craftingAnimation = true;
                config.motion.sprintStride = true;
                config.motion.airborneMotion = true;
                config.motion.idleSway = true;
                config.motion.playerIntensity = 0.8;
            }
            case 10 -> {
                config.cameraDistance = 3.0;
                config.inventoryCameraSide = 0.72;
                config.inventoryTiltDegrees = 4.0;
                config.cameraYawOffset = 0;
                config.cameraPitchOffset = 0;
                config.cameraTransitionDurationMs = 420;
            }
            case 11 -> GlassMaterialPresets.apply(config, GlassMaterialPresets.Preset.FROSTED);
            case 12 -> {
                config.defaultSpatialHandledScreens = true;
                config.fallbackToScreenSpace = true;
                config.settingsKey = "key.keyboard.f8";
                config.enabled = true;
            }
            case 13 -> {
                config.playerHeightRatio = 0.47;
                config.playerCenterXRatio = 0.77;
                config.playerCenterYRatio = 0.55;
                config.panelCenterRatio = 0.42;
                config.recipeBookOffset = 77.0;
            }
            case 14 -> {
                config.slideSpeed = 2600.0;
                config.slideBrake = 3450.0;
                config.bounce = 0.26;
                config.closeSpeed = 2200.0;
                config.spring = 500.0;
                config.damping = 38.0;
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
