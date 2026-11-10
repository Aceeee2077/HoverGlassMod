package dev.transparentinventory.client.camera;

/** Reversible smooth interpolation, continuous even when a screen changes mid-transition. */
public final class CameraTransition {
    private double value, from, target;
    private long changedAt;
    public double update(boolean open, long now, int durationMs) {
        sample(now, durationMs);
        double requested = open ? 1 : 0;
        if (requested != target) {
            from = value;
            target = requested;
            changedAt = now;
            // A zero-length transition (reduced motion, or a 0 ms slider value)
            // must land on the new target this frame instead of one frame later.
            if (durationMs <= 0) value = requested;
        }
        return value;
    }
    private void sample(long now, int durationMs) {
        double t = durationMs <= 0 ? 1 : Math.clamp((now - changedAt) / (durationMs * 1_000_000.0), 0, 1);
        value = from + (target - from) * t * t * (3 - 2 * t);
    }
    public void reset() { value = from = target = 0; changedAt = 0; }
}
