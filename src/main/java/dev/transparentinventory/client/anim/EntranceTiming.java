package dev.transparentinventory.client.anim;

/**
 * Decides when a reopened screen should skip its entrance animation.
 *
 * <p>Players often close and reopen a container within a few hundred milliseconds.
 * Replaying the full slide and camera sweep then feels sluggish, so an entrance that
 * starts inside the window snaps straight to its resting pose. Plain Java with a
 * caller-supplied clock, so the rule is covered by the regression suite.
 */
public final class EntranceTiming {

    private static final long NEVER = Long.MIN_VALUE;

    private long closedAt = NEVER;

    /** Records the moment the previous panel finished closing. */
    public void closed(long nowNanos) {
        closedAt = nowNanos;
    }

    /** Forgets the last close, for example after leaving the world. */
    public void reset() {
        closedAt = NEVER;
    }

    /**
     * True when an entrance starting at {@code nowNanos} should snap.
     * A window of zero or less disables the behaviour; a backwards clock also animates.
     */
    public boolean snap(long nowNanos, int windowMs) {
        if (closedAt == NEVER || windowMs <= 0) return false;
        long elapsed = nowNanos - closedAt;
        return elapsed >= 0 && elapsed < windowMs * 1_000_000L;
    }

    /** True once a close has been recorded and the window has elapsed. */
    public boolean expired(long nowNanos, int windowMs) {
        if (closedAt == NEVER) return true;
        long elapsed = nowNanos - closedAt;
        return elapsed < 0 || windowMs <= 0 || elapsed >= windowMs * 1_000_000L;
    }
}
