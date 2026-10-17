package dev.transparentinventory.client.anim;

/** Monotonic-clock opening/closing interpolation, independent of frame rate. */
public final class SpatialAnimationController {
    /** Cubic overshoot coefficients; peaks at ~10% near t=0.6 for a punchy slam-in. */
    private static final double OVERSHOOT_B = 1.70158;
    private static final double OVERSHOOT_A = OVERSHOOT_B + 1;
    private static final double LANDING_T = 1 - OVERSHOOT_B / OVERSHOOT_A;
    private static final long IMPACT_DECAY_NS = 140_000_000L;

    private final long openedAt;
    private long closedAt = -1;
    private double closeFrom;
    private double closeMotionFrom;
    private long impactAt = -1;

    public SpatialAnimationController(long now) { openedAt = now; }

    public double progress(long now, int durationMs) {
        if (closedAt >= 0) return closeFrom * (1 - ease(now - closedAt, durationMs));
        return ease(now - openedAt, durationMs);
    }

    public void close(long now, int durationMs) {
        if (closedAt < 0) {
            closeFrom = progress(now, durationMs);
            closeMotionFrom = motion(now, durationMs);
            closedAt = now;
        }
    }

    public double motion(long now, int durationMs) {
        if (closedAt >= 0) return closeMotionFrom * (1 - ease(now - closedAt, durationMs));
        double t = durationMs <= 0 ? 1 : clamp((now - openedAt) / (durationMs * 1_000_000.0), 0, 1);
        double u = t - 1;
        double value = 1 + OVERSHOOT_A * u * u * u + OVERSHOOT_B * u * u;
        // Pulse when the panel first crosses its resting position, independent of FPS.
        if (durationMs > 0 && t >= LANDING_T) {
            impactAt = openedAt + (long) (durationMs * 1_000_000.0 * LANDING_T);
        }
        return value;
    }

    /** Exponential impact pulse, 1 at landing and decaying to 0; 0 before landing. */
    public double impact(long now) {
        if (impactAt < 0 || closedAt >= 0) return 0;
        return Math.exp(-(now - impactAt) / (double) IMPACT_DECAY_NS);
    }

    public boolean closing() { return closedAt >= 0; }
    public boolean finished(long now, int durationMs) {
        return closing() && (durationMs <= 0 || now - closedAt >= durationMs * 1_000_000L);
    }

    private static double ease(long elapsed, int durationMs) {
        double t = durationMs <= 0 ? 1 : clamp(elapsed / (durationMs * 1_000_000.0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }
}
