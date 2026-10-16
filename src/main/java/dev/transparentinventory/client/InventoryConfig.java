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
    /** Legacy drop-inventory settings; the universal path preserves vanilla previews. */
    public boolean playerPreview = true;
    public boolean playerLookAtInventory = true;
    public boolean closeAnimation = true;

    public boolean defaultSpatialHandledScreens = true;
    public boolean worldAnchoringEnabled = true;
    public boolean fallbackToScreenSpace = true;
    public boolean cameraEnabled = true;
    public boolean thirdPersonOnOpen = true;
    public boolean cinematicInventory = true;
    public double inventoryTiltDegrees = 4.0;
    public double cameraDistance = 3.0;
    public double cameraYawOffset = 0;
    public double cameraPitchOffset = 0;
    public int cameraTransitionDurationMs = 250;
    public double guiScale = 1.0;
    public double guiDistance = 1.5;
    public double guiVerticalOffset = 0.3;
    public int openingAnimationDurationMs = 320;
    public float worldDimOpacity = 0.0F;
    public List<String> excludedScreens = new ArrayList<>();
    /** Non-container screens are opt-in so menus/chat are never spatialized by accident. */
    public List<String> additionalScreens = new ArrayList<>();
    /** Fully qualified screen class name -> background opacity; missing means global. */
    public Map<String, Float> screenOpacities = new LinkedHashMap<>();

    public float opacityFor(String className) { return screenOpacities.getOrDefault(className, panelOpacity); }

    // ------------------------------------------------------------------
    // Visual alpha values
    // ------------------------------------------------------------------

    /** Vanilla panel/skin opacity, 0 = invisible, 1 = opaque. */
    public float panelOpacity = 0.65F;
    // Remaining chrome/physics/layout/look parameters are retained for old config
    // compatibility and the legacy math regressions, not the universal renderer.
    /** Slot fill alpha, spec range [0.02, 0.15]. */
    public float slotOpacity = 0.09F;
    /** Panel border alpha, spec range [0.20, 0.35]. */
    public float borderOpacity = 0.28F;
    /** Slot border alpha, spec range [0.16, 0.28]. */
    public float slotBorderOpacity = 0.22F;
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

    /** Virtual distance used to convert pixel offsets into yaw/pitch angles. */
    public double lookDistance = 600.0;
    /** Exponential smoothing speed for the look angles. */
    public double lookSmoothing = 11.0;
    /** Exponential smoothing speed for the player preview slide. */
    public double playerSlideSmoothing = 12.0;
    /** Maximum head yaw in degrees. */
    public float maxHeadYaw = 50.0F;
    /** Maximum head pitch in degrees. */
    public float maxHeadPitch = 35.0F;
    /** Maximum body yaw in degrees. */
    public float maxBodyYaw = 16.0F;
    /** Fraction of the head yaw that the body follows. */
    public double bodyYawRatio = 0.34;

    /** Public no-arg constructor required for Gson and tests. */
    public InventoryConfig() {
    }

    /**
     * Clamps every value into a safe range so a hand-edited or corrupted config
     * file can never produce NaN/absurd physics or off-screen layouts.
     */
    public void sanitize() {
        panelOpacity = clamp(panelOpacity, 0F, 1F);
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
        cameraTransitionDurationMs = (int) clamp(cameraTransitionDurationMs, 0, 2000);
        openingAnimationDurationMs = (int) clamp(openingAnimationDurationMs, 0, 2000);
        guiScale = clamp(guiScale, 0.5, 1.5);
        guiDistance = clamp(guiDistance, 0.5, 6);
        guiVerticalOffset = clamp(guiVerticalOffset, -2, 3);
        if (excludedScreens == null) excludedScreens = new ArrayList<>();
        if (additionalScreens == null) additionalScreens = new ArrayList<>();
        excludedScreens.removeIf(value -> value == null || value.isBlank());
        additionalScreens.removeIf(value -> value == null || value.isBlank());
        if (screenOpacities == null) screenOpacities = new LinkedHashMap<>();
        screenOpacities.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null);
        screenOpacities.replaceAll((name, value) -> clamp(value, 0F, 1F));

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

        lookDistance = clamp(lookDistance, 100.0, 3000.0);
        lookSmoothing = clamp(lookSmoothing, 1.0, 40.0);
        playerSlideSmoothing = clamp(playerSlideSmoothing, 2.0, 40.0);
        maxHeadYaw = clamp(maxHeadYaw, 0.0F, 90.0F);
        maxHeadPitch = clamp(maxHeadPitch, 0.0F, 90.0F);
        maxBodyYaw = clamp(maxBodyYaw, 0.0F, 60.0F);
        bodyYawRatio = clamp(bodyYawRatio, 0.0, 0.80);
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
