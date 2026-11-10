package dev.transparentinventory.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side configuration for the Hoverglass mod.
 *
 * <p>Plain Java only (no Minecraft types) so it can be exercised by unit tests
 * and serialized by Gson without any adapter.
 */
public final class InventoryConfig {

    private static volatile InventoryConfig instance = new InventoryConfig();

    /** Current immutable-ish config instance used by the client code. */
    public static InventoryConfig get() {
        return instance;
    }

    /** Replaces the active config. A {@code null} value falls back to defaults. */
    public static void setInstance(InventoryConfig config) {
        InventoryConfig next = (config == null) ? new InventoryConfig() : config;
        next.sanitize();
        instance = next;
    }

    // ------------------------------------------------------------------
    // Feature toggles
    // ------------------------------------------------------------------

    public boolean enabled = true;
    /** Function keys reachable with the settings-key button, in cycle order. */
    public static final List<String> SETTINGS_KEYS = List.of(
            "key.keyboard.f6", "key.keyboard.f7", "key.keyboard.f8",
            "key.keyboard.f9", "key.keyboard.f10");
    /** Function key that opens the settings screen, stored as an InputConstants name. */
    public String settingsKey = "key.keyboard.f8";
    /** Settings page to reopen after a restart; kept in sync with the page cycle. */
    public int settingsPage = 0;
    /** Legacy player-preview settings. */
    public boolean playerPreview = true;
    public boolean playerLookAtInventory = true;
    public boolean closeAnimation = true;

    public boolean defaultSpatialHandledScreens = true;
    public boolean worldAnchoringEnabled = true;
    public boolean fallbackToScreenSpace = true;
    public boolean cameraEnabled = true;
    /** Whether chests and other containers may pull the camera back; the player inventory keeps its own framing. */
    public boolean containerCameraEnabled = true;
    public boolean thirdPersonOnOpen = true;
    public boolean cinematicInventory = true;
    /** Replace survival and creative inventory textures with outlined glass. */
    public boolean glassInventory = true;
    /** Optional menu/widget theme, independent of the inventory glass preset. */
    public boolean liquidGlassUi = false;
    /** HUD hotbar chrome; item rendering and selection remain vanilla. */
    public boolean glassHotbar = true;
    public float hotbarOpacity = 0.16F;
    /** HUD hotbar rim strength, independent of the inventory panel border. */
    public float hotbarBorderOpacity = 0.72F;
    public MotionOptions motion = new MotionOptions();

    /** Optional 26.x visual extensions, stored inside the existing config file. */
    public static final class MotionOptions {
        public boolean enabled = true;
        public boolean itemSwap = true;
        public boolean attackAnimation = true;
        public double firstPersonIntensity = 0.65;
        public boolean sprintSway = true;
        public boolean jumpMotion = true;
        public boolean landingMotion = true;
        public boolean hitShake = true;
        public double cameraIntensity = 0.5;
        public boolean blockBreakEffects = true;
        public boolean blockPlaceAnimation = true;
        public double particleMultiplier = 1;
        /** Whole-body third-person combat and workstation poses. */
        public boolean playerAnimations = true;
        public boolean playerAttackAnimation = true;
        /** Local-player hands-at-the-bench loop while a crafting table menu is open. */
        public boolean craftingAnimation = true;
        /** Extra leg/arm stride while sprinting, layered over the vanilla walk cycle. */
        public boolean sprintStride = true;
        /** Tuck while jumping and arms-out balance while falling. */
        public boolean airborneMotion = true;
        /** Subtle breathing and weight shift while standing still. */
        public boolean idleSway = true;
        /** 0 disables the added pose, 1 uses the full authored range. */
        public double playerIntensity = 0.8;
        public boolean reduceMotion;

        public void sanitize() {
            firstPersonIntensity = clamp(firstPersonIntensity, 0, 1);
            cameraIntensity = clamp(cameraIntensity, 0, 1);
            particleMultiplier = clamp(particleMultiplier, 0, 2);
            playerIntensity = clamp(playerIntensity, 0, 1);
        }
    }
    /** Migration marker: seed the newly supported creative screen once, respecting overrides. */
    public boolean creativeGlassDefaultsApplied;
    public double inventoryCameraSide = 0.72;
    public double inventoryTiltDegrees = 4.0;
    public double cameraDistance = 3.0;
    public double cameraYawOffset = 0;
    public double cameraPitchOffset = 0;
    public int cameraTransitionDurationMs = 420;
    /** Reopening inside this window skips the entrance and camera animation; 0 disables it. */
    public int reopenSnapWindowMs = 400;
    public double guiScale = 1.0;
    /** Readable screen-space size for chests and other non-player containers. */
    public double containerScale = 1.35;
    public double guiDistance = 1.5;
    public double guiVerticalOffset = 0.3;
    public int openingAnimationDurationMs = 480;
    public float worldDimOpacity = 0.0F;
    public List<String> excludedScreens = new ArrayList<>();
    /** Non-container screens are opt-in so menus/chat are never spatialized by accident. */
    public List<String> additionalScreens = new ArrayList<>();
    /** Fully qualified screen class name -> background opacity; missing means global. */
    public Map<String, Float> screenOpacities = new LinkedHashMap<>();

    /**
     * Opacity for a screen class. An exact entry wins; otherwise the most specific
     * matching wildcard rule (see {@link ScreenRules}) applies, then the global panel opacity.
     */
    public float opacityFor(String className) { return ScreenRules.opacity(screenOpacities, className, panelOpacity); }

    /** True when this screen has its own exact override and does not inherit a wildcard. */
    public boolean hasScreenOverride(String className) { return ScreenRules.hasExactOverride(screenOpacities, className); }

    /** Whether this screen uses the global value, a wildcard rule or its own exact entry. */
    public ScreenRules.Source opacitySource(String className) { return ScreenRules.source(screenOpacities, className); }

    /** True when an exact entry or a wildcard rule excludes this screen from spatial handling. */
    public boolean isScreenExcluded(String className) { return ScreenRules.matchesAny(excludedScreens, className); }

    /** True when an exact exclusion entry exists for this screen (wildcard rules are not removed). */
    public boolean hasExactExclusion(String className) { return screenNameIn(excludedScreens, className); }

    /**
     * Adds or removes an exact exclusion for this screen.
     * A wildcard rule that already excludes it is left alone, so the button is a no-op then.
     *
     * @return true when the screen is excluded afterwards
     */
    public boolean toggleScreenExclusion(String className) {
        String clean = ScreenRules.cleanName(className);
        if (clean == null) return false;
        if (excludedScreens.remove(clean)) return false;
        // A wildcard rule already excludes it; adding an exact entry would change nothing.
        if (ScreenRules.matchesAny(excludedScreens, clean)) return true;
        excludedScreens.add(clean);
        return true;
    }

    /** Removes every exact and wildcard exclusion. */
    public void clearScreenExclusions() { excludedScreens.clear(); }

    /** Short label for the configured key, e.g. {@code F8}. */
    public String settingsKeyName() {
        String name = settingsKey == null ? "key.keyboard.f8" : settingsKey;
        int dot = name.lastIndexOf('.');
        return (dot < 0 ? name : name.substring(dot + 1)).toUpperCase(java.util.Locale.ROOT);
    }

    /** Moves to the next allowed function key, wrapping around; the settings button drives this. */
    public void cycleSettingsKey() {
        settingsKey = cleanSettingsKey(settingsKey);
        settingsKey = SETTINGS_KEYS.get((SETTINGS_KEYS.indexOf(settingsKey) + 1) % SETTINGS_KEYS.size());
    }

    private static String cleanSettingsKey(String value) {
        if (value == null) return "key.keyboard.f8";
        String clean = value.trim().toLowerCase(java.util.Locale.ROOT);
        return SETTINGS_KEYS.contains(clean) ? clean : "key.keyboard.f8";
    }

    /** True when this screen is opted in to the spatial UI, exactly or by a wildcard rule. */
    public boolean isScreenOptedIn(String className) { return ScreenRules.matchesAny(additionalScreens, className); }

    /** True when an exact opt-in entry exists for this screen (wildcard rules are not removed). */
    public boolean hasExactOptIn(String className) { return screenNameIn(additionalScreens, className); }

    /**
     * Adds or removes an exact opt-in for a non-container screen.
     * A wildcard rule that already opts it in is left alone, so the button is a no-op then.
     *
     * @return true when the screen is opted in afterwards
     */
    public boolean toggleScreenOptIn(String className) {
        String clean = ScreenRules.cleanName(className);
        if (clean == null) return false;
        if (additionalScreens.remove(clean)) return false;
        if (ScreenRules.matchesAny(additionalScreens, clean)) return true;
        additionalScreens.add(clean);
        return true;
    }

    /** Removes every per-screen opacity override; screens fall back to the global value. */
    public void clearScreenOpacities() { screenOpacities.clear(); }

    /** Copies the glass panel's rim strength onto the hotbar's independent rim. */
    public void matchHotbarRim() { hotbarBorderOpacity = borderOpacity; }

    /** Copies the glass panel's fill opacity onto the hotbar's independent value. */
    public void matchHotbarOpacity() { hotbarOpacity = panelOpacity; }

    /** Removes every opt-in rule, including wildcards, leaving only built-in containers. */
    public void clearScreenOptIns() { additionalScreens.clear(); }

    private static boolean screenNameIn(List<String> rules, String className) {
        String clean = ScreenRules.cleanName(className);
        return clean != null && rules.contains(clean);
    }

    // ------------------------------------------------------------------
    // Visual alpha values
    // ------------------------------------------------------------------

    /** Panel opacity, 0 = invisible, 1 = opaque; glass rims stay independently visible. */
    public float panelOpacity = 0.18F;
    /** Glass slot fill alpha, multiplied by panel opacity. */
    public float slotOpacity = 0.09F;
    /** Glass panel border alpha [0, 1], independent of panel opacity. */
    public float borderOpacity = 0.72F;
    /** Glass slot border alpha [0, 1], independent of panel opacity. */
    public float slotBorderOpacity = 0.58F;
    /** Hovered slot alpha, spec range [0.12, 0.22]. */
    public float hoverOpacity = 0.18F;
    /** Selected (quick-craft) slot alpha, spec range [0.25, 0.40]. */
    public float selectedOpacity = 0.32F;

    // ------------------------------------------------------------------
    // Slide physics (pixels / seconds)
    // ------------------------------------------------------------------

    /** Initial rightward speed when the panel slides in from the left. */
    public double slideSpeed = 2600.0;
    /** Braking deceleration applied while sliding in. */
    public double slideBrake = 3450.0;
    /** Restitution factor applied when hitting the resting position [0, 0.6]. */
    public double bounce = 0.26;

    // ------------------------------------------------------------------
    // Settling spring (critically-ish damped)
    // ------------------------------------------------------------------

    /** Spring stiffness for the settle phase. */
    public double spring = 500.0;
    /** Damping coefficient for the settle phase. */
    public double damping = 38.0;

    // ------------------------------------------------------------------
    // Close physics
    // ------------------------------------------------------------------

    /** Upward initial speed when closing. */
    public double closeSpeed = 2200.0;
    /** Upward acceleration while closing. */
    public double closeAcceleration = 2400.0;

    // ------------------------------------------------------------------
    // Layout ratios (relative to screen size)
    // ------------------------------------------------------------------

    /** Horizontal centre of the inventory panel, as a fraction of screen width. */
    public double panelCenterRatio = 0.42;
    /** Horizontal centre of the player preview, as a fraction of screen width. */
    public double playerCenterXRatio = 0.77;
    /** Vertical centre of the player preview, as a fraction of screen height. */
    public double playerCenterYRatio = 0.55;
    /** Player model height as a fraction of screen height. */
    public double playerHeightRatio = 0.47;
    /** Horizontal offset applied to the panel when the recipe book is visible. */
    public double recipeBookOffset = 77.0;
    /** Player preview slide-in distance, as a fraction of the screen width. */
    public double playerSlideRatio = 0.22;

    // ------------------------------------------------------------------
    // Look-at behaviour
    // ------------------------------------------------------------------

    /** Exponential smoothing speed for the look angles. */
    public double lookSmoothing = 11.0;
    /** Maximum head yaw in degrees. */
    public float maxHeadYaw = 50.0F;
    /** Maximum head pitch in degrees. */
    public float maxHeadPitch = 35.0F;

    /** Public no-arg constructor required for Gson and tests. */
    public InventoryConfig() {
    }

    /** True when the user asked for fewer moving effects. */
    public boolean reducedMotion() { return motion != null && motion.reduceMotion; }

    /** The quick-reopen window as a slider fraction of its 1500 ms maximum. */
    public double reopenWindowFraction() { return reopenSnapWindowMs / 1500.0; }

    /** Sets the quick-reopen window from a slider fraction, clamped to 0–1500 ms. */
    public void setReopenWindowFraction(double fraction) {
        reopenSnapWindowMs = (int) clamp(fraction * 1500.0, 0, 1500);
    }

    /**
     * Opening/closing animation length after the reduced-motion preference.
     * Zero means the panel appears and disappears at its resting pose without a slide.
     */
    public int effectiveOpeningDurationMs() { return reducedMotion() ? 0 : openingAnimationDurationMs; }

    /** Camera transition length after the reduced-motion preference; zero snaps the view. */
    public int effectiveCameraTransitionDurationMs() { return reducedMotion() ? 0 : cameraTransitionDurationMs; }

    /** Apply the reference composition without erasing other containers' opacity choices. */
    public void applyGlassPreset() {
        enabled = glassInventory = cinematicInventory = cameraEnabled = thirdPersonOnOpen = true;
        screenOpacities.put("net.minecraft.client.gui.screens.inventory.InventoryScreen", 0.16F);
        screenOpacities.put("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen", 0.16F);
        worldDimOpacity = 0;
        borderOpacity = 0.78F;
        slotBorderOpacity = 0.64F;
        slotOpacity = 0.09F;
        openingAnimationDurationMs = 480;
        cameraTransitionDurationMs = 420;
        inventoryCameraSide = 0.72;
        cameraDistance = 3.0;
        inventoryTiltDegrees = 3.0;
        guiScale = 1.12;
    }

    /**
     * Clamps every value into a safe range so a hand-edited or corrupted config
     * file can never produce NaN/absurd physics or off-screen layouts.
     */
    public void sanitize() {
        if (motion == null) motion = new MotionOptions();
        motion.sanitize();
        panelOpacity = clamp(panelOpacity, 0F, 1F);
        hotbarOpacity = clamp(hotbarOpacity, 0F, 1F);
        hotbarBorderOpacity = clamp(hotbarBorderOpacity, 0F, 1F);
        slotOpacity = clamp(slotOpacity, 0F, 1F);
        borderOpacity = clamp(borderOpacity, 0F, 1F);
        slotBorderOpacity = clamp(slotBorderOpacity, 0F, 1F);
        hoverOpacity = clamp(hoverOpacity, 0F, 1F);
        selectedOpacity = clamp(selectedOpacity, 0F, 1F);
        worldDimOpacity = clamp(worldDimOpacity, 0F, 1F);
        cameraDistance = clamp(cameraDistance, 0.0, 8.0);
        cameraYawOffset = clamp(cameraYawOffset, -60, 60);
        cameraPitchOffset = clamp(cameraPitchOffset, -45, 45);
        inventoryTiltDegrees = clamp(inventoryTiltDegrees, -12, 12);
        inventoryCameraSide = clamp(inventoryCameraSide, 0, 0.95);
        cameraTransitionDurationMs = (int) clamp(cameraTransitionDurationMs, 0, 2000);
        reopenSnapWindowMs = (int) clamp(reopenSnapWindowMs, 0, 1500);
        openingAnimationDurationMs = (int) clamp(openingAnimationDurationMs, 0, 2000);
        guiScale = clamp(guiScale, 0.5, 1.5);
        containerScale = clamp(containerScale, 0.75, 2.0);
        guiDistance = clamp(guiDistance, 0.5, 6);
        guiVerticalOffset = clamp(guiVerticalOffset, -2, 3);
        excludedScreens = ScreenRules.cleanList(excludedScreens);
        additionalScreens = ScreenRules.cleanList(additionalScreens);
        settingsKey = cleanSettingsKey(settingsKey);
        // SettingsPages owns the page list; this only keeps saved indices sane for
        // legacy builds where the settings screen does not exist.
        settingsPage = Math.clamp(settingsPage, 0, 14);
        Map<String, Float> cleanedOpacities = new LinkedHashMap<>();
        if (screenOpacities != null) {
            for (Map.Entry<String, Float> entry : screenOpacities.entrySet()) {
                String name = ScreenRules.cleanName(entry.getKey());
                if (name == null || entry.getValue() == null) continue;
                cleanedOpacities.put(name, clamp(entry.getValue(), 0F, 1F));
            }
        }
        screenOpacities = cleanedOpacities;
        if (!creativeGlassDefaultsApplied) {
            screenOpacities.putIfAbsent("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen", 0.16F);
            creativeGlassDefaultsApplied = true;
        }

        slideSpeed = clamp(slideSpeed, 600.0, 6000.0);
        slideBrake = clamp(slideBrake, 200.0, 8000.0);
        bounce = clamp(bounce, 0.0, 0.6);

        spring = clamp(spring, 40.0, 900.0);
        damping = clamp(damping, 5.0, 90.0);

        closeSpeed = clamp(closeSpeed, 600.0, 6000.0);
        closeAcceleration = clamp(closeAcceleration, 300.0, 12000.0);

        panelCenterRatio = clamp(panelCenterRatio, 0.20, 0.65);
        playerCenterXRatio = clamp(playerCenterXRatio, 0.50, 0.95);
        playerCenterYRatio = clamp(playerCenterYRatio, 0.30, 0.80);
        playerHeightRatio = clamp(playerHeightRatio, 0.25, 0.70);
        recipeBookOffset = clamp(recipeBookOffset, 0.0, 160.0);
        playerSlideRatio = clamp(playerSlideRatio, 0.05, 0.60);

        lookSmoothing = clamp(lookSmoothing, 1.0, 40.0);
        maxHeadYaw = clamp(maxHeadYaw, 0.0F, 90.0F);
        maxHeadPitch = clamp(maxHeadPitch, 0.0F, 90.0F);
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
