package dev.transparentinventory.client.legacy;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anim.SpatialAnimationController;

/**
 * Time-based open animation for legacy (2D) targets. Pure Java: the per-era mixins
 * read motion/progress/impact and apply pose transforms themselves.
 */
public final class LegacyScreenManager {
    public static final LegacyScreenManager INSTANCE = new LegacyScreenManager();
    private SpatialAnimationController animation;
    private int travelPixels = 320;

    private LegacyScreenManager() { }

    public void open(int totalTravel) {
        if (animation == null) {
            animation = new SpatialAnimationController(System.nanoTime());
            travelPixels = Math.max(0, totalTravel);
        }
    }

    public void reset() { animation = null; }
    public boolean active() { return animation != null; }

    private int duration() { return InventoryConfig.get().openingAnimationDurationMs; }
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
