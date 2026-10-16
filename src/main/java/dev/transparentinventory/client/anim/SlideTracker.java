package dev.transparentinventory.client.anim;

/**
 * Frame-rate independent exponential smoother for a single offset value, used to
 * slide the player preview in from the right (and out again on close) without any
 * Minecraft types.
 *
 * <p>Uses {@code alpha = 1 - exp(-speed * dt)} so the motion is identical at 30, 60,
 * 144 and 240 FPS. {@link #begin(double)} sets the initial offset; subsequent
 * {@link #update(double, double, double)} calls ease towards the requested target.
 */
public final class SlideTracker {

    private boolean primed;
    private double offset;

    /** Defines the starting offset; the next updates ease away from it. */
    public void begin(double startOffset) {
        this.offset = startOffset;
        this.primed = true;
    }

    /**
     * Eases the offset towards {@code targetOffset}.
     *
     * @param dt    seconds since the previous update; values &le; 0 keep the offset
     * @param speed exponential smoothing speed; higher is snappier
     */
    public void update(double targetOffset, double dt, double speed) {
        if (!primed || dt <= 0.0) {
            return;
        }
        double alpha = 1.0 - Math.exp(-Math.max(0.0, speed) * dt);
        alpha = Math.max(0.0, Math.min(1.0, alpha));
        offset += (targetOffset - offset) * alpha;
    }

    public double offset() {
        return offset;
    }
}
