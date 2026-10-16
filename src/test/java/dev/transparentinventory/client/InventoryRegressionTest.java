package dev.transparentinventory.client;

import dev.transparentinventory.client.anim.InventoryAnimator;
import dev.transparentinventory.client.anim.SlideTracker;
import dev.transparentinventory.client.layout.InventoryLayout;
import dev.transparentinventory.client.look.LookTracker;

/**
 * Plain-Java regression tests for the mod's simulation and layout maths.
 *
 * <p>This class intentionally does NOT reference any Minecraft type: it runs on the
 * standard JVM via the {@code physicsTest} Gradle task and fails fast with an
 * {@link AssertionError} describing the violated invariant.
 */
public final class InventoryRegressionTest {

    private static int checks;

    public static void main(String[] args) {
        testFrameRateIndependence();
        testSlideCompletes();
        testBounceOvershoot();
        testCloseAnimation();
        testLayoutMath();
        testLookTracker();
        testSlideTracker();
        System.out.println("[physicsTest] all checks passed (" + checks + " assertions)");
    }

    // ------------------------------------------------------------------
    // 1. Frame-rate independence
    // ------------------------------------------------------------------

    private static void testFrameRateIndependence() {
        double[] fpsValues = {30.0, 60.0, 144.0, 240.0};
        double[] sampleTimes = {0.15, 0.25, 0.35, 0.45, 0.60, 0.80};
        double target = 718.0;
        double extent = 166.0;
        double startOffset = target + extent + 60.0;

        for (double t : sampleTimes) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (double fps : fpsValues) {
                double pos = posAt(fps, t, target, extent, startOffset);
                min = Math.min(min, pos);
                max = Math.max(max, pos);
            }
            double spread = max - min;
            check(spread < 1.0,
                    "frame-rate independence at t=" + t + "s: pos spread " + spread + "px exceeds 1px");
        }

        // Every frame rate must fully settle within a reasonable window.
        for (double fps : fpsValues) {
            InventoryAnimator animator = newAnimator();
            animator.beginOpen(target, startOffset, extent);
            double dt = 1.0 / fps;
            for (int i = 0; i < (int) Math.ceil(1.6 * fps); i++) {
                animator.update(dt);
            }
            check(animator.phase() == InventoryAnimator.Phase.STABLE,
                    "fps=" + fps + ": expected STABLE after 1.6s, got " + animator.phase());
            checkClose(animator.pos(), target, 1.0E-6, "fps=" + fps + ": settled position");
        }
    }

    /** Simulates at the given frame rate and stops exactly at time {@code t}. */
    private static double posAt(double fps, double t, double target, double extent, double startOffset) {
        InventoryAnimator animator = newAnimator();
        animator.beginOpen(target, startOffset, extent);
        double dt = 1.0 / fps;
        double elapsed = 0.0;
        while (elapsed + dt <= t) {
            animator.update(dt);
            elapsed += dt;
        }
        if (elapsed < t) {
            animator.update(t - elapsed);
        }
        return animator.pos();
    }

    // ------------------------------------------------------------------
    // 2. Slide completes and settles on the target
    // ------------------------------------------------------------------

    private static void testSlideCompletes() {
        double target = 718.0;
        InventoryAnimator animator = newAnimator();
        animator.beginOpen(target, target + 226.0, 176.0);
        check(animator.phase() == InventoryAnimator.Phase.SLIDING, "starts in SLIDING");
        check(animator.pos() < 0.0, "starts fully off screen to the left");
        check(animator.isStarted(), "started flag is set");

        for (int i = 0; i < 400; i++) {
            animator.update(1.0 / 60.0);
        }
        check(animator.phase() == InventoryAnimator.Phase.STABLE, "settles to STABLE");
        checkClose(animator.pos(), target, 1.0E-6, "final position equals target");
    }

    // ------------------------------------------------------------------
    // 3. Impact bounces back past the target (overshoot to the left)
    // ------------------------------------------------------------------

    private static void testBounceOvershoot() {
        double target = 718.0;
        InventoryAnimator animator = newAnimator();
        animator.beginOpen(target, target + 226.0, 176.0);

        boolean sawSettling = false;
        double leftmost = Double.MAX_VALUE;
        for (int i = 0; i < 400; i++) {
            animator.update(1.0 / 120.0);
            if (animator.phase() == InventoryAnimator.Phase.SETTLING) {
                sawSettling = true;
            }
            leftmost = Math.min(leftmost, animator.pos());
        }
        check(sawSettling, "passes through SETTLING phase");
        check(leftmost < target - 5.0,
                "impact bounces past the target by more than 5px (leftmost=" + leftmost + ")");
    }

    // ------------------------------------------------------------------
    // 4. Close animation
    // ------------------------------------------------------------------

    private static void testCloseAnimation() {
        double target = 718.0;
        double extent = 176.0;
        InventoryAnimator animator = newAnimator();
        animator.beginOpen(target, target + extent + 60.0, extent);
        for (int i = 0; i < 400; i++) {
            animator.update(1.0 / 60.0);
        }
        check(!animator.blocksInput(), "stable panel accepts input");

        animator.requestClose();
        check(animator.isClosing(), "closing flag is set");
        check(animator.blocksInput(), "closing panel blocks input");
        check(animator.velocity() < 0.0, "closing moves left");

        // A second request must be a no-op.
        double velocityBefore = animator.velocity();
        animator.requestClose();
        checkClose(animator.velocity(), velocityBefore, 1.0E-9, "requestClose is idempotent");

        for (int i = 0; i < 400 && !animator.isFinishedClosing(); i++) {
            animator.update(1.0 / 60.0);
        }
        check(animator.isFinishedClosing(), "close animation finishes");
        check(animator.pos() + extent <= 0.0,
                "panel is fully off the left edge when done (pos=" + animator.pos() + ")");
    }

    // ------------------------------------------------------------------
    // 5. Layout maths
    // ------------------------------------------------------------------

    private static void testLayoutMath() {
        InventoryConfig config = new InventoryConfig();
        config.sanitize();

        int screenW = 1920;
        int screenH = 1080;
        int imageW = 176;
        int imageH = 166;

        InventoryLayout layout = InventoryLayout.compute(screenW, screenH, imageW, imageH, false, config);

        check(layout.leftPos == 718, "1920x1080 leftPos is 718 (got " + layout.leftPos + ")");
        checkClose(layout.panelCenterX, 806.0, 0.5, "panel centre X");
        check(layout.panelCenterX < screenW * 0.5, "panel centre stays left of screen centre");
        check(layout.playerCenterX > screenW * 0.5, "player preview stays right of screen centre");

        // The player model must fit entirely inside the capture box.
        double scale = layout.playerScale;
        check(layout.playerY1 <= layout.playerCenterY - 0.8375 * scale,
                "capture box covers the top of the model");
        check(layout.playerY2 >= layout.playerCenterY + 0.9625 * scale,
                "capture box covers the feet of the model");
        check(layout.playerX1 >= 0 && layout.playerY1 >= 0
                        && layout.playerX2 <= screenW && layout.playerY2 <= screenH,
                "capture box stays on screen");

        // Recipe book open must not overlap the vanilla book position.
        InventoryLayout withBook = InventoryLayout.compute(screenW, screenH, imageW, imageH, true, config);
        check(withBook.leftPos >= (int) ((screenW - imageW) / 2.0 + config.recipeBookOffset) - 1,
                "panel clears the recipe book when it is visible");

        // Tiny screens must clamp rather than go negative.
        InventoryLayout tiny = InventoryLayout.compute(200, 180, imageW, imageH, false, config);
        check(tiny.leftPos == 0, "tiny screen clamps leftPos to 0 (got " + tiny.leftPos + ")");
        check(tiny.targetTopPos >= 0, "tiny screen keeps a non-negative target Y");
        check(tiny.playerX1 >= 0 && tiny.playerX2 <= 200, "tiny screen keeps the preview on screen");

        // Aspect ratio sweep: the layout must never push the panel off screen.
        int[][] sizes = {
                {2560, 1080},  // 21:9
                {3840, 2160},  // 4K 16:9
                {1280, 1024},  // 5:4
                {3440, 1440}   // ultrawide
        };
        for (int[] size : sizes) {
            InventoryLayout l = InventoryLayout.compute(size[0], size[1], imageW, imageH, false, config);
            check(l.leftPos >= 0 && l.leftPos + imageW <= size[0],
                    size[0] + "x" + size[1] + ": panel stays within the screen");
            check(l.playerX2 - l.playerX1 >= (int) l.playerScale,
                    size[0] + "x" + size[1] + ": capture box is large enough");
        }
    }

    // ------------------------------------------------------------------
    // 6. Look tracker
    // ------------------------------------------------------------------

    private static void testLookTracker() {
        LookTracker tracker = new LookTracker();
        tracker.update(48.0F, 30.0F, 16.0F, 1.0 / 60.0, 11.0);
        checkClose(tracker.headYaw(), 48.0, 1.0E-4, "first update snaps to target (yaw)");
        checkClose(tracker.headPitch(), 30.0, 1.0E-4, "first update snaps to target (pitch)");
        checkClose(tracker.bodyYaw(), 16.0, 1.0E-4, "first update snaps to target (body)");

        tracker.reset();
        tracker.update(0.0F, 0.0F, 0.0F, 1.0 / 60.0, 11.0);

        double previous = tracker.headYaw();
        for (int i = 0; i < 120; i++) {
            tracker.update(40.0F, 20.0F, 10.0F, 1.0 / 60.0, 11.0);
            check(tracker.headYaw() <= 40.0 + 1.0E-4, "smoothing never overshoots the target");
            check(tracker.headYaw() >= previous - 1.0E-4, "smoothing is monotonic");
            previous = tracker.headYaw();
        }
        checkClose(tracker.headYaw(), 40.0, 0.1, "converges to the target within 1s");

        // Frame-rate independence of the smoothing itself.
        LookTracker slow = new LookTracker();
        LookTracker fast = new LookTracker();
        for (int i = 0; i < 30; i++) {
            slow.update(40.0F, 20.0F, 10.0F, 1.0 / 30.0, 11.0);
        }
        for (int i = 0; i < 240; i++) {
            fast.update(40.0F, 20.0F, 10.0F, 1.0 / 240.0, 11.0);
        }
        checkClose(slow.headYaw(), fast.headYaw(), 0.5, "smoothing is frame-rate independent");
    }

    // ------------------------------------------------------------------
    // 7. Player preview slide tracker
    // ------------------------------------------------------------------

    private static void testSlideTracker() {
        SlideTracker tracker = new SlideTracker();
        tracker.begin(400.0);
        checkClose(tracker.offset(), 400.0, 1.0E-9, "begin sets the start offset");

        // dt <= 0 must not move the offset.
        tracker.update(0.0, 0.0, 12.0);
        tracker.update(0.0, -0.5, 12.0);
        checkClose(tracker.offset(), 400.0, 1.0E-9, "non-positive dt keeps the offset");

        // Eases towards the target without overshooting.
        double previous = tracker.offset();
        for (int i = 0; i < 180; i++) {
            tracker.update(0.0, 1.0 / 60.0, 12.0);
            check(tracker.offset() <= previous + 1.0E-9, "slide is monotonic");
            check(tracker.offset() >= 0.0 - 1.0E-9, "slide never overshoots the target");
            previous = tracker.offset();
        }
        checkClose(tracker.offset(), 0.0, 0.5, "converges to the target");

        // Frame-rate independence.
        SlideTracker slow = new SlideTracker();
        SlideTracker fast = new SlideTracker();
        slow.begin(500.0);
        fast.begin(500.0);
        for (int i = 0; i < 30; i++) {
            slow.update(0.0, 1.0 / 30.0, 12.0);
        }
        for (int i = 0; i < 240; i++) {
            fast.update(0.0, 1.0 / 240.0, 12.0);
        }
        checkClose(slow.offset(), fast.offset(), 0.5, "slide is frame-rate independent");

        // Exit: eases out to the right.
        SlideTracker exit = new SlideTracker();
        exit.begin(0.0);
        for (int i = 0; i < 120; i++) {
            exit.update(800.0, 1.0 / 60.0, 12.0);
        }
        checkClose(exit.offset(), 800.0, 0.5, "slides out to the exit offset");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static InventoryAnimator newAnimator() {
        InventoryConfig config = new InventoryConfig();
        config.sanitize();
        return new InventoryAnimator(config);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError("FAILED: " + message);
        }
    }

    private static void checkClose(double actual, double expected, double tolerance, String message) {
        checks++;
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError("FAILED: " + message + " (expected " + expected
                    + " +/- " + tolerance + ", got " + actual + ")");
        }
    }

    private InventoryRegressionTest() {
    }
}
