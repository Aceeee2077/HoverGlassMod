package dev.transparentinventory.client.legacy;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anim.EntranceTiming;
import dev.transparentinventory.client.anim.SpatialAnimationController;

/**
 * Time-based open animation for legacy (2D) targets. Pure Java: the per-era mixins
 * read motion/progress/impact and apply pose transforms themselves.
 */
public final class LegacyScreenManager {
    public static final LegacyScreenManager INSTANCE = new LegacyScreenManager();
    private final EntranceTiming reopenTiming = new EntranceTiming();
    private SpatialAnimationController animation;
    private boolean snapSession;
    private int travelPixels = 320;

    private LegacyScreenManager() { }

    public void open(int totalTravel) {
        if (animation == null) {
            long now = System.nanoTime();
            animation = new SpatialAnimationController(now);
            snapSession = reopenTiming.snap(now, InventoryConfig.get().reopenSnapWindowMs);
            travelPixels = Math.max(0, totalTravel);
        }
    }

    public void reset() {
        if (animation != null) reopenTiming.closed(System.nanoTime());
        animation = null;
    }
    public boolean active() { return animation != null; }

    private int duration() { return snapSession ? 0 : InventoryConfig.get().effectiveOpeningDurationMs(); }
    private long now() { return System.nanoTime(); }

    /** Interpolated slide; overshoots ~10% past 1 before settling. */
    public double motion() { return animation == null ? 1 : animation.motion(now(), duration()); }
    /** Smoothstep visibility 0..1. */
    public double progress() { return animation == null ? 1 : animation.progress(now(), duration()); }
    /** Landing pulse 1 -> 0, 0 before the panel first lands. */
    public double impact() { return animation == null ? 0 : animation.impact(now()); }

    /** Signed pixels the panel still has to travel; negative while overshooting. */
    public double travel() { return (1 - motion()) * travelPixels; }
}
