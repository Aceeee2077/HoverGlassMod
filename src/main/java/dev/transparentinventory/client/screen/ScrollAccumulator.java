package dev.transparentinventory.client.screen;

/**
 * Accumulates vertical scroll input and reports whole page steps.
 *
 * <p>Mouse wheels deliver a full notch at a time, but trackpads and high-resolution
 * wheels send many small deltas. Requiring a whole notch per event made the latter
 * unable to change pages at all; keeping a running total lets those deltas add up
 * while a single notch still flips exactly one page. Overshoot is discarded after a
 * step so a fast flick cannot skip several pages at once.
 *
 * <p>Positive scroll amounts (wheel up) move to the previous page, matching
 * {@link SettingsPages#shift(int, int)}.
 */
public final class ScrollAccumulator {

    /** Distance that counts as one notch. */
    public static final double THRESHOLD = 0.5;

    private double pending;

    /**
     * Adds one scroll event.
     *
     * @param scrollY vertical scroll amount; non-finite values are ignored
     * @return {@code -1} for a page back, {@code 1} for a page forward, {@code 0} otherwise
     */
    public int accept(double scrollY) {
        if (!Double.isFinite(scrollY)) {
            return 0;
        }
        pending += scrollY;
        if (pending >= THRESHOLD) {
            pending = 0;
            return -1;
        }
        if (pending <= -THRESHOLD) {
            pending = 0;
            return 1;
        }
        return 0;
    }

    /** Drops any partially accumulated scroll, e.g. when the page changes some other way. */
    public void reset() {
        pending = 0;
    }

    /** Scroll distance accumulated toward the next step, for tests and diagnostics. */
    public double pending() {
        return pending;
    }
}
