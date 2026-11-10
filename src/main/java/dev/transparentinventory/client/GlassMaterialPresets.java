package dev.transparentinventory.client;

/**
 * Coherent glass alpha bundles for the material settings page.
 *
 * <p>All five values move together: thinner fills pair with lighter rims, and
 * brighter highlights keep hover/selection readable on busy backgrounds. Every
 * preset stays inside the ranges that {@link InventoryConfig#sanitize()} accepts,
 * so applying one never needs a second clamps pass.
 */
public final class GlassMaterialPresets {
    private static final float TOLERANCE = 1.0E-4F;

    private GlassMaterialPresets() {
    }

    public enum Preset {
        /** Barely-there fill with thin rims; best on dark scenes. */
        CLEAR(0.05F, 0.60F, 0.45F, 0.13F, 0.24F),
        /** The reference look the default config ships with. */
        FROSTED(0.09F, 0.72F, 0.58F, 0.18F, 0.32F),
        /** Heavier fills and rims for bright or busy backgrounds. */
        VIVID(0.14F, 0.90F, 0.75F, 0.22F, 0.40F);

        private final float slotOpacity;
        private final float borderOpacity;
        private final float slotBorderOpacity;
        private final float hoverOpacity;
        private final float selectedOpacity;

        Preset(float slotOpacity, float borderOpacity, float slotBorderOpacity,
               float hoverOpacity, float selectedOpacity) {
            this.slotOpacity = slotOpacity;
            this.borderOpacity = borderOpacity;
            this.slotBorderOpacity = slotBorderOpacity;
            this.hoverOpacity = hoverOpacity;
            this.selectedOpacity = selectedOpacity;
        }

        public float slotOpacity() { return slotOpacity; }
        public float borderOpacity() { return borderOpacity; }
        public float slotBorderOpacity() { return slotBorderOpacity; }
        public float hoverOpacity() { return hoverOpacity; }
        public float selectedOpacity() { return selectedOpacity; }
    }

    /** Writes every material alpha from {@code preset} in one step. */
    public static void apply(InventoryConfig config, Preset preset) {
        config.slotOpacity = preset.slotOpacity;
        config.borderOpacity = preset.borderOpacity;
        config.slotBorderOpacity = preset.slotBorderOpacity;
        config.hoverOpacity = preset.hoverOpacity;
        config.selectedOpacity = preset.selectedOpacity;
    }

    /** True when the config still carries exactly this preset's five alphas. */
    public static boolean matches(InventoryConfig config, Preset preset) {
        return close(config.slotOpacity, preset.slotOpacity)
                && close(config.borderOpacity, preset.borderOpacity)
                && close(config.slotBorderOpacity, preset.slotBorderOpacity)
                && close(config.hoverOpacity, preset.hoverOpacity)
                && close(config.selectedOpacity, preset.selectedOpacity);
    }

    /** The preset the config currently matches, or {@code null} for hand-tuned values. */
    public static Preset current(InventoryConfig config) {
        for (Preset preset : Preset.values()) {
            if (matches(config, preset)) return preset;
        }
        return null;
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) <= TOLERANCE;
    }
}
