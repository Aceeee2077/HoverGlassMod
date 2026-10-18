package dev.transparentinventory.client.anim;

/** Lower, swap at the hidden point, raise with a small overshoot. Uses absolute monotonic time. */
public final class ItemSwapTimeline {
    private static final long LOWER = 110_000_000L, RAISE = 210_000_000L;
    private int visible, target;
    private long started;
    private double from, lower;
    private boolean primed;
    private int phase; // 0 idle, 1 lowering, 2 raising

    public void select(int key, long now) {
        sample(now);
        if (!primed) { primed = true; visible = target = key; return; }
        if (target == key) return;
        target = key;
        from = lower;
        started = now;
        phase = key == visible ? 2 : 1;
    }

    public void sample(long now) {
        if (phase == 1) {
            double t = Math.max(0, (now - started) / (double) LOWER);
            lower = from + (1 - from) * Easing.smoothstep(t);
            if (t >= 1) { visible = target; phase = 2; from = 1; started += LOWER; }
        }
        if (phase == 2) {
            double t = Math.max(0, (now - started) / (double) RAISE);
            lower = from * (1 - Easing.outBack(t));
            if (t >= 1) { lower = 0; phase = 0; }
        }
    }
    public int visibleKey() { return visible; }
    public boolean active() { return phase != 0; }
    public double lower() { return lower; }
    public void reset() { primed = false; phase = 0; lower = from = 0; }
}
