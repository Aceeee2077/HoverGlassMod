package dev.transparentinventory.client.screen;

import java.util.Locale;

/**
 * Turns a slider's normalized {@code [0, 1]} position into the value the user
 * actually configured.
 *
 * <p>Pure Java (no Minecraft types) so the unit conventions stay testable.
 * Every key here matches a {@code SettingSlider} label in
 * {@link SpatialSettingsScreen}; unknown keys fall back to a plain decimal.
 */
public final class SliderFormat {

    private SliderFormat() {
    }

    /**
     * Display text for a slider value.
     *
     * @param labelKey    translation key of the slider label
     * @param normalized  slider position, clamped to {@code [0, 1]}
     * @param percentage  true when the slider directly edits a fraction shown as a percentage
     */
    public static String value(String labelKey, double normalized, boolean percentage) {
        double value = Double.isFinite(normalized) ? Math.clamp(normalized, 0.0, 1.0) : 0.0;
        if (percentage) {
            return Math.round(value * 100) + "%";
        }
        String key = labelKey == null ? "" : labelKey;
        return switch (key) {
            // ------------------------------------------------------------------
            // Panel feel (physics)
            // ------------------------------------------------------------------
            case "spatialui.feel.open_speed", "spatialui.feel.close_speed" ->
                    Math.round(600 + value * 5400) + " px/s";
            case "spatialui.feel.slide_brake" -> Math.round(200 + value * 7800) + " px/s²";
            case "spatialui.feel.bounce" -> Math.round(value * 60) + "%";
            case "spatialui.feel.spring" -> String.valueOf(Math.round(40 + value * 860));
            case "spatialui.feel.damping" -> String.valueOf(Math.round(5 + value * 85));

            // ------------------------------------------------------------------
            // Preview layout
            // ------------------------------------------------------------------
            case "spatialui.layout.player_height" -> Math.round((0.25 + value * 0.45) * 100) + "%";
            case "spatialui.layout.player_x" -> Math.round((0.50 + value * 0.45) * 100) + "%";
            case "spatialui.layout.player_y" -> Math.round((0.30 + value * 0.50) * 100) + "%";
            case "spatialui.layout.panel_x" -> Math.round((0.20 + value * 0.45) * 100) + "%";
            case "spatialui.layout.recipe_book" -> Math.round(value * 160) + " px";

            // ------------------------------------------------------------------
            // Camera and entrance
            // ------------------------------------------------------------------
            case "spatialui.container_scale" -> Math.round((0.75 + value * 1.25) * 100) + "%";
            case "spatialui.camera.distance" -> String.format(Locale.ROOT, "%.2f", value * 8);
            case "spatialui.camera.duration", "spatialui.inventory.duration" ->
                    Math.round(value * 1000) + " ms";
            case "spatialui.inventory.tilt" -> String.format(Locale.ROOT, "%.1f°", value * 24 - 12);
            case "spatialui.inventory.reopen" -> Math.round(value * 1500) + " ms";
            case "spatialui.camera.yaw_offset" -> Math.round(value * 120 - 60) + "°";
            case "spatialui.camera.pitch_offset" -> Math.round(value * 90 - 45) + "°";

            // ------------------------------------------------------------------
            // Look, motion and GUI scale
            // ------------------------------------------------------------------
            case "spatialui.look.head_yaw", "spatialui.look.head_pitch" ->
                    Math.round(value * 90) + "°";
            case "spatialui.look.smoothing" -> Math.round(value * 100) + "%";
            case "spatialui.motion.particle_multiplier" -> String.format(Locale.ROOT, "%.2f×", value * 2);
            case "spatialui.scale" -> String.format(Locale.ROOT, "%.2f", value + 0.5);

            default -> String.format(Locale.ROOT, "%.2f", value + 0.5);
        };
    }
}
