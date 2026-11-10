package dev.transparentinventory.client;

/**
 * Coherent camera framing bundles for the camera angles page.
 *
 * <p>Each preset moves the pull-back distance, the sideways/vertical framing,
 * the panel tilt and the transition duration together, so the settled view and
 * the sweep into it stay matched. Every value stays inside the ranges that
 * {@link InventoryConfig#sanitize()} accepts, so applying one never needs a
 * second clamps pass. The framing angle offsets are reset to zero: a preset is
 * a complete framing, not an adjustment on top of hand-tuned angles.
 */
public final class CameraPresets {
    private static final double TOLERANCE = 1.0E-4;

    private CameraPresets() {
    }

    public enum Preset {
        /** Closer, flatter framing with a quick transition. */
        SUBTLE(2.4, 0.45, 2.0, 0, 0, 300),
        /** The framing and timing the default config ships with. */
        BALANCED(3.0, 0.72, 4.0, 0, 0, 420),
        /** A wider pull-back with a slower sweep. */
        CINEMATIC(3.8, 0.90, 7.0, 0, 0, 600);

        private final double distance;
        private final double side;
        private final double tilt;
        private final double yawOffset;
        private final double pitchOffset;
        private final int durationMs;

        Preset(double distance, double side, double tilt,
               double yawOffset, double pitchOffset, int durationMs) {
            this.distance = distance;
            this.side = side;
            this.tilt = tilt;
            this.yawOffset = yawOffset;
            this.pitchOffset = pitchOffset;
            this.durationMs = durationMs;
        }

        public double distance() { return distance; }
        public double side() { return side; }
        public double tilt() { return tilt; }
        public double yawOffset() { return yawOffset; }
        public double pitchOffset() { return pitchOffset; }
        public int durationMs() { return durationMs; }
    }

    /** Writes every framing value from {@code preset} in one step. */
    public static void apply(InventoryConfig config, Preset preset) {
        config.cameraDistance = preset.distance;
        config.inventoryCameraSide = preset.side;
        config.inventoryTiltDegrees = preset.tilt;
        config.cameraYawOffset = preset.yawOffset;
        config.cameraPitchOffset = preset.pitchOffset;
        config.cameraTransitionDurationMs = preset.durationMs;
    }

    /** True when both angle offsets are zero, i.e. the camera is level. */
    public static boolean isLevel(InventoryConfig config) {
        return config != null && config.cameraYawOffset == 0 && config.cameraPitchOffset == 0;
    }

    /**
     * Zeroes both angle offsets while leaving distance, side, tilt and transition
     * duration alone.
     *
     * @return true when the config actually changed
     */
    public static boolean level(InventoryConfig config) {
        if (config == null) {
            return false;
        }
        boolean changed = config.cameraYawOffset != 0 || config.cameraPitchOffset != 0;
        config.cameraYawOffset = 0;
        config.cameraPitchOffset = 0;
        return changed;
    }

    /** True when the config still carries exactly this preset's six values. */
    public static boolean matches(InventoryConfig config, Preset preset) {
        return close(config.cameraDistance, preset.distance)
                && close(config.inventoryCameraSide, preset.side)
                && close(config.inventoryTiltDegrees, preset.tilt)
                && close(config.cameraYawOffset, preset.yawOffset)
                && close(config.cameraPitchOffset, preset.pitchOffset)
                && config.cameraTransitionDurationMs == preset.durationMs;
    }

    /** The preset the config currently matches, or {@code null} for hand-tuned framing. */
    public static Preset current(InventoryConfig config) {
        for (Preset preset : Preset.values()) {
            if (matches(config, preset)) return preset;
        }
        return null;
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
    }
}
