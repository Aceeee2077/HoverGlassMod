package dev.transparentinventory.client;

import dev.transparentinventory.client.anim.EntranceTiming;
import dev.transparentinventory.client.anim.InventoryAnimator;
import dev.transparentinventory.client.anim.SlideTracker;
import dev.transparentinventory.client.layout.InventoryLayout;
import dev.transparentinventory.client.look.LookTracker;
import dev.transparentinventory.client.motion.AvatarMotionMath;
import dev.transparentinventory.client.screen.SettingsPages;

import java.util.LinkedHashMap;
import java.util.Map;

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
        testCinematicTimeline();
        testGlassPreset();
        testCreativeGlassMigration();
        testScreenRules();
        testReducedMotion();
        testConfigRecovery();
        testCameraPolicy();
        testQuickReopen();
        testCompatibilityActions();
        testSettingsPages();
        testInventoryCursorLook();
        testMotionUtilities();
        testAvatarMotion();
        System.out.println("[physicsTest] all checks passed (" + checks + " assertions)");
    }

    // ------------------------------------------------------------------
    // 1. Frame-rate independence
    // ------------------------------------------------------------------

    private static void testMotionUtilities() {
        for (double t : new double[]{0, .1, .25, .5, .9, 1}) {
            check(dev.transparentinventory.client.anim.Easing.smoothstep(t) >= 0 && dev.transparentinventory.client.anim.Easing.smoothstep(t) <= 1, "bounded easing");
        }
        double reference = 0, referenceVelocity = 0;
        for (int fps : new int[]{30, 60, 144, 240}) {
            var spring = new dev.transparentinventory.client.anim.DampedSpring();
            spring.impulse(.8);
            for (int frame = 0; frame < fps / 2; frame++) spring.update(1.0 / fps);
            if (fps == 30) { reference = spring.position(); referenceVelocity = spring.velocity(); }
            checkClose(spring.position(), reference, 1E-12, "analytic spring independent of FPS");
            checkClose(spring.velocity(), referenceVelocity, 1E-12, "spring velocity independent of FPS");
            spring.update(4); checkClose(spring.position(), 0, 1E-7, "spring settles");
        }
        long start = 1_000_000_000L;
        var swap = new dev.transparentinventory.client.anim.ItemSwapTimeline();
        swap.select(1, start); swap.select(2, start);
        swap.sample(start + 90_000_000L);
        check(swap.visibleKey() == 1 && swap.lower() > .8, "old item retained while lowering");
        swap.sample(start + 110_000_000L);
        check(swap.visibleKey() == 2 && swap.lower() == 1, "item changes only at hidden point");
        swap.sample(start + 250_000_000L);
        check(swap.lower() < 0, "item raise has small overshoot");
        swap.sample(start + 320_000_000L);
        check(!swap.active() && swap.lower() == 0, "swap ends exactly at idle");
        swap.select(3, start + 400_000_000L); swap.sample(start + 450_000_000L);
        double before = swap.lower(); swap.select(4, start + 450_000_000L);
        checkClose(swap.lower(), before, 1E-12, "rapid reselection preserves position");
        swap.sample(start + 2_000_000_000L);
        check(swap.visibleKey() == 4 && !swap.active(), "dropped frames complete latest swap");
        for (int fps : new int[]{30, 60, 144, 240}) {
            var clock = new dev.transparentinventory.client.anim.ItemSwapTimeline();
            clock.select(1, start); clock.select(2, start);
            for (int i = 1; i < fps / 4; i++) clock.sample(start + (long) (i * 1E9 / fps));
            clock.sample(start + 250_000_000L);
            var direct = new dev.transparentinventory.client.anim.ItemSwapTimeline();
            direct.select(1, start); direct.select(2, start); direct.sample(start + 250_000_000L);
            checkClose(clock.lower(), direct.lower(), 1E-12, "item timeline independent of FPS");
        }
        var config = new InventoryConfig(); config.motion = null; config.sanitize();
        check(config.motion != null, "missing motion config migrates safely");
        config.motion.firstPersonIntensity = Double.NaN; config.motion.cameraIntensity = 9; config.motion.particleMultiplier = -4;
        config.sanitize();
        check(config.motion.firstPersonIntensity == 0 && config.motion.cameraIntensity == 1 && config.motion.particleMultiplier == 0, "motion config clamps invalid values");
    }

    private static void testAvatarMotion() {
        AvatarMotionMath.Style[] styles = {
                AvatarMotionMath.Style.SWORD, AvatarMotionMath.Style.AXE, AvatarMotionMath.Style.TOOL,
                AvatarMotionMath.Style.SPEAR, AvatarMotionMath.Style.UNARMED
        };
        for (AvatarMotionMath.Style style : styles) {
            check(AvatarMotionMath.attack(style, 0, 1, 1, true) == AvatarMotionMath.Pose.NONE,
                    "avatar attack starts at rest: " + style);
            check(AvatarMotionMath.attack(style, 1, 1, 1, true) == AvatarMotionMath.Pose.NONE,
                    "avatar attack ends at rest: " + style);
            check(AvatarMotionMath.attack(style, .5F, 1, 0, true) == AvatarMotionMath.Pose.NONE,
                    "zero avatar intensity is an exact no-op: " + style);
            for (float t : new float[]{.01F, .25F, .5F, .75F, .99F}) {
                AvatarMotionMath.Pose pose = AvatarMotionMath.attack(style, t, 1, 1, true);
                check(finite(pose), "avatar attack is finite: " + style + " at " + t);
                check(Math.abs(pose.body().pitch()) <= .3F && Math.abs(pose.body().yaw()) <= .3F
                                && Math.abs(pose.head().pitch()) <= .3F && Math.abs(pose.head().yaw()) <= .3F
                                && Math.abs(pose.rightArm().roll()) <= .3F
                                && Math.abs(pose.leftArm().roll()) <= .3F,
                        "avatar attack stays within the authored budget: " + style + " at " + t);
            }
        }
        check(AvatarMotionMath.attack(AvatarMotionMath.Style.NONE, .5F, 1, 1, true) == AvatarMotionMath.Pose.NONE,
                "no swing family is an exact no-op");

        AvatarMotionMath.Pose right = AvatarMotionMath.attack(AvatarMotionMath.Style.SWORD, .25F, 1, 1, true);
        AvatarMotionMath.Pose left = AvatarMotionMath.attack(AvatarMotionMath.Style.SWORD, .25F, -1, 1, true);
        checkClose(right.body().yaw(), -left.body().yaw(), 1E-5, "avatar torso twist mirrors between hands");
        checkClose(right.rightArm().roll(), -left.leftArm().roll(), 1E-5, "attack arm roll mirrors between hands");
        checkClose(right.rightLeg().pitch(), -left.rightLeg().pitch(), 1E-5, "idle step mirrors between hands");

        AvatarMotionMath.Pose full = AvatarMotionMath.attack(AvatarMotionMath.Style.SWORD, .5F, 1, 1, true);
        AvatarMotionMath.Pose half = AvatarMotionMath.attack(AvatarMotionMath.Style.SWORD, .5F, 1, .5F, true);
        checkClose(half.body().pitch(), full.body().pitch() * .5, 1E-6, "avatar intensity scales linearly");
        AvatarMotionMath.Pose moving = AvatarMotionMath.attack(AvatarMotionMath.Style.SWORD, .5F, 1, 1, false);
        check(moving.rightLeg().pitch() == 0 && moving.leftLeg().pitch() == 0,
                "walking does not add a second idle step");
        check(full.rightLeg().pitch() != 0, "standing attack includes a step");

        for (float age = 0; age < 400; age += 7.3F) {
            check(finite(AvatarMotionMath.crafting(age, .8F)), "crafting pose is finite at age " + age);
        }
        AvatarMotionMath.Pose craft = AvatarMotionMath.crafting(0, 1);
        check(craft.rightArm().pitch() < -.5F && craft.leftArm().pitch() < -.5F,
                "crafting raises both hands toward the bench");
        check(craft.head().pitch() > 0, "crafting looks down at the bench");
        check(AvatarMotionMath.crafting(0, 0) == AvatarMotionMath.Pose.NONE,
                "zero crafting intensity is an exact no-op");
        check(AvatarMotionMath.crafting(Float.NaN, 1) == AvatarMotionMath.Pose.NONE,
                "invalid crafting time is rejected");

        check(AvatarMotionMath.stride(0, 0, 1) == AvatarMotionMath.Pose.NONE,
                "zero walk speed stops the sprint stride");
        check(AvatarMotionMath.stride(0, 1, 0) == AvatarMotionMath.Pose.NONE,
                "zero stride intensity is an exact no-op");
        check(AvatarMotionMath.stride(0, .04F, 1) == AvatarMotionMath.Pose.NONE,
                "a nearly stopped walk does not jitter the stride");
        check(AvatarMotionMath.stride(Float.NaN, 1, 1) == AvatarMotionMath.Pose.NONE,
                "invalid walk phase is rejected");
        check(AvatarMotionMath.stride(0, Float.NaN, 1) == AvatarMotionMath.Pose.NONE,
                "invalid walk speed is rejected");
        for (float pos = 0; pos < 6.3F; pos += .45F) {
            AvatarMotionMath.Pose pose = AvatarMotionMath.stride(pos, 1, 1);
            check(finite(pose), "sprint stride is finite at phase " + pos);
            check(Math.abs(pose.body().pitch()) <= .1F && Math.abs(pose.head().pitch()) <= .05F
                            && Math.abs(pose.rightArm().pitch()) <= .3F && Math.abs(pose.leftArm().pitch()) <= .3F
                            && Math.abs(pose.rightLeg().pitch()) <= .5F && Math.abs(pose.leftLeg().pitch()) <= .5F,
                    "sprint stride stays within the authored budget at " + pos);
        }
        AvatarMotionMath.Pose sprint = AvatarMotionMath.stride(0, 1, 1);
        checkClose(sprint.rightLeg().pitch(), -sprint.leftLeg().pitch(), 1E-6, "stride legs alternate");
        checkClose(sprint.rightArm().pitch(), -sprint.leftArm().pitch(), 1E-6, "stride arms alternate");
        check(sprint.rightLeg().pitch() * sprint.rightArm().pitch() < 0,
                "each arm counters the same-side leg");
        check(sprint.body().pitch() > 0, "sprint leans the torso forward");
        check(sprint.head().pitch() < 0, "the head counteracts the sprint lean");
        AvatarMotionMath.Pose flipped = AvatarMotionMath.stride((float) (Math.PI / .6662), 1, 1);
        checkClose(flipped.rightLeg().pitch(), -sprint.rightLeg().pitch(), 1E-4,
                "half a walk cycle flips the stride");
        AvatarMotionMath.Pose halfStride = AvatarMotionMath.stride(0, 1, .5F);
        checkClose(halfStride.rightLeg().pitch(), sprint.rightLeg().pitch() * .5, 1E-6,
                "stride intensity scales linearly");
        AvatarMotionMath.Pose slowStride = AvatarMotionMath.stride(0, .5F, 1);
        checkClose(slowStride.rightLeg().pitch(), sprint.rightLeg().pitch() * .5, 1E-6,
                "stride follows walk speed");
        check(AvatarMotionMath.stride(0, 5, 1).rightLeg().pitch() == sprint.rightLeg().pitch(),
                "stride speed is clamped");

        InventoryConfig config = new InventoryConfig();
        config.motion.playerIntensity = 9;
        config.sanitize();
        checkClose(config.motion.playerIntensity, 1, 1E-9, "avatar intensity has a safe upper limit");
        config.motion.playerIntensity = Double.NaN;
        config.sanitize();
        checkClose(config.motion.playerIntensity, 0, 1E-9, "invalid avatar intensity falls back to rest");
    }

    private static boolean finite(AvatarMotionMath.Pose pose) {
        return finite(pose.body()) && finite(pose.head()) && finite(pose.rightArm())
                && finite(pose.leftArm()) && finite(pose.rightLeg()) && finite(pose.leftLeg());
    }

    private static boolean finite(AvatarMotionMath.Limb limb) {
        return Float.isFinite(limb.pitch()) && Float.isFinite(limb.yaw()) && Float.isFinite(limb.roll());
    }

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
    // Cinematic regressions: endpoints, visible landing pulse, reverse continuity and hit mapping.
    // ------------------------------------------------------------------

    private static void testCinematicTimeline() {
        long start = 1_000_000_000L;
        var animation = new dev.transparentinventory.client.anim.SpatialAnimationController(start);
        checkClose(animation.motion(start, 480), 0, 1E-9, "entrance starts at zero without a negative jump");
        checkClose(animation.motion(start + 480_000_000L, 480), 1, 1E-9, "entrance ends at target");
        var sampled = new dev.transparentinventory.client.anim.SpatialAnimationController(start);
        for (int ms = 0; ms <= 250; ms += 10) sampled.motion(start + ms * 1_000_000L, 480);
        var skipped = new dev.transparentinventory.client.anim.SpatialAnimationController(start);
        skipped.motion(start + 250_000_000L, 480);
        check(sampled.impact(start + 250_000_000L) > 0.5, "flash happens during arrival, before settling");
        checkClose(sampled.impact(start + 250_000_000L), skipped.impact(start + 250_000_000L), 1E-9,
                "landing flash timing survives skipped frames");
        double peak = 0;
        for (int ms = 0; ms <= 480; ms += 5) {
            long now = start + ms * 1_000_000L;
            double motion = animation.motion(now, 480), visibility = animation.progress(now, 480);
            peak = Math.max(peak, motion);
            check(visibility >= 0 && visibility <= 1, "opacity remains valid during overshoot");
            var transform = dev.transparentinventory.client.render.InventoryEntrance.transform(427, 240, 213, 120,
                    176, 166, 1.12, motion, visibility, 3, animation.impact(now));
            double x = transform.screenX(154, 95), y = transform.screenY(154, 95);
            checkClose(transform.localX(x, y), 154, 1E-8, "animated slanted slot X inverse");
            checkClose(transform.localY(x, y), 95, 1E-8, "animated slanted slot Y inverse");
        }
        check(peak > 1.08 && peak < 1.12, "entrance has controlled overshoot");
        long closingAt = start + 240_000_000L;
        double before = animation.motion(closingAt, 480);
        animation.close(closingAt, 480);
        checkClose(animation.motion(closingAt, 480), before, 1E-9, "mid-entrance close does not jump");
        checkClose(animation.impact(closingAt), 0, 1E-9, "closing never replays opening flash");
        var instant = new dev.transparentinventory.client.anim.SpatialAnimationController(start);
        checkClose(instant.motion(start, 0), 1, 1E-9, "zero-duration entrance is immediate");
        checkClose(instant.impact(start), 0, 1E-9, "zero-duration entrance has no phantom flash");
    }

    private static void testGlassPreset() {
        var config = new InventoryConfig();
        config.screenOpacities.put("other.container", 0.43F);
        config.applyGlassPreset();
        checkClose(config.opacityFor("net.minecraft.client.gui.screens.inventory.InventoryScreen"), 0.16, 1E-6,
                "reference preset gives 84% transparent inventory");
        checkClose(config.opacityFor("other.container"), 0.43, 1E-6, "preset preserves other container overrides");
        config.inventoryCameraSide = Double.POSITIVE_INFINITY;
        config.sanitize();
        checkClose(config.inventoryCameraSide, 0.95, 1E-9, "camera side offset has safe upper limit");
    }

    private static InventoryAnimator newAnimator() {
        InventoryConfig config = new InventoryConfig();
        config.sanitize();
        return new InventoryAnimator(config);
    }

    private static void testCreativeGlassMigration() {
        String creative = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen";
        var config = new InventoryConfig();
        config.panelOpacity = 0.65F; // Existing pre-creative config.
        config.sanitize();
        checkClose(config.opacityFor(creative), 0.16, 1E-6, "upgrade seeds creative glass without requiring a preset");
        checkClose(config.panelOpacity, 0.65, 1E-6, "upgrade preserves global opacity");
        config.screenOpacities.remove(creative);
        config.sanitize();
        checkClose(config.opacityFor(creative), 0.65, 1E-6, "choosing inheritance survives save/sanitize");
        var custom = new InventoryConfig();
        custom.screenOpacities.put(creative, 0.42F);
        custom.sanitize();
        checkClose(custom.opacityFor(creative), 0.42, 1E-6, "upgrade respects an existing creative override");
        custom.applyGlassPreset();
        checkClose(custom.opacityFor(creative), 0.16, 1E-6, "reference preset also applies to creative inventory");
    }

    private static void testScreenRules() {
        String merchant = "net.minecraft.client.gui.screens.inventory.MerchantScreen";
        check(ScreenRules.isPattern("a*") && ScreenRules.isPattern("?"), "wildcards are recognised");
        check(!ScreenRules.isPattern(merchant), "a plain class name is not a pattern");
        check(ScreenRules.matches("*", ""), "a lone star matches the empty name");
        check(ScreenRules.matches("a*", "a") && ScreenRules.matches("a*c", "ac"), "a star may match nothing");
        check(ScreenRules.matches("a*c", "abbbc"), "a star spans several characters");
        check(ScreenRules.matches("a?c", "abc"), "a question mark matches one character");
        check(!ScreenRules.matches("a?c", "ac") && !ScreenRules.matches("a?c", "abbc"), "a question mark matches exactly one");
        check(ScreenRules.matches("net.minecraft.client.gui.screens.inventory.*", merchant),
                "a package wildcard covers a screen");
        check(ScreenRules.matches("*MerchantScreen", merchant), "a suffix wildcard matches");
        check(ScreenRules.matches("*Merchant*", merchant), "a two-sided wildcard matches");
        check(!ScreenRules.matches("*MerchantScreen", merchant + "Extra"), "a suffix wildcard respects the end");
        check(ScreenRules.matches("net.minecraft.?lient.gui.screens.inventory.MerchantScreen", merchant),
                "a single-character wildcard matches inside a class name");
        check(!ScreenRules.matches(merchant, merchant.toLowerCase(java.util.Locale.ROOT)),
                "screen rules are case-sensitive");
        check(!ScreenRules.matches("*a*a*a*a*a*b", "aaaaaaaaaaaaaaaaaaaaaaaa"),
                "a pathological rule terminates with no match");
        check(ScreenRules.matches(merchant, merchant), "an exact rule matches itself");
        check(!ScreenRules.matches(merchant, merchant + "Extra") && !ScreenRules.matches("", merchant),
                "non-pattern rules require full equality");
        check(!ScreenRules.matches(null, merchant) && !ScreenRules.matches(merchant, null),
                "null rules and names never match");

        Map<String, Float> overrides = new LinkedHashMap<>();
        overrides.put("*Screen", 0.5F);
        overrides.put("net.minecraft.client.gui.screens.inventory.*", 0.3F);
        checkClose(ScreenRules.opacity(overrides, merchant, 0.9F), 0.3, 1E-6,
                "the most specific wildcard wins");
        overrides.put(merchant, 0.2F);
        checkClose(ScreenRules.opacity(overrides, merchant, 0.9F), 0.2, 1E-6,
                "an exact entry beats every wildcard");
        check(ScreenRules.hasExactOverride(overrides, merchant), "an exact override is reported");

        Map<String, Float> tie = new LinkedHashMap<>();
        tie.put("a*", 0.1F);
        tie.put("*b", 0.2F);
        checkClose(ScreenRules.opacity(tie, "ab", 0.9F), 0.1, 1E-6,
                "equally specific patterns keep the earliest entry");
        checkClose(ScreenRules.opacity(null, merchant, 0.7F), 0.7, 1E-6, "a missing map uses the fallback");
        checkClose(ScreenRules.opacity(overrides, null, 0.7F), 0.7, 1E-6, "a missing name uses the fallback");
        check(ScreenRules.bestKey(overrides, null) == null, "no key is resolved for a null name");
        check(ScreenRules.matchesAny(java.util.List.of("a*", "b"), "abc"), "matchesAny accepts patterns");
        check(!ScreenRules.matchesAny(java.util.List.of("a*", "b"), "cde"), "matchesAny rejects non-matches");
        check(!ScreenRules.matchesAny(null, "abc"), "a missing rule list matches nothing");

        var config = new InventoryConfig();
        config.panelOpacity = 0.2F;
        config.screenOpacities.put("net.minecraft.client.gui.screens.*", 0.55F);
        checkClose(config.opacityFor("net.minecraft.client.gui.screens.ChatScreen"), 0.55, 1E-6,
                "wildcard opacity applies through the config");
        check(!config.hasScreenOverride("net.minecraft.client.gui.screens.ChatScreen"),
                "a wildcard hit is inherited, not a per-screen override");
        check(config.opacitySource("net.minecraft.client.gui.screens.ChatScreen") == ScreenRules.Source.RULE,
                "the status line can name the wildcard source");
        config.screenOpacities.put("net.minecraft.client.gui.screens.ChatScreen", 0.12F);
        checkClose(config.opacityFor("net.minecraft.client.gui.screens.ChatScreen"), 0.12, 1E-6,
                "a per-screen slider value beats the wildcard");
        check(config.hasScreenOverride("net.minecraft.client.gui.screens.ChatScreen"),
                "an exact slider value is reported as an override");
        check(config.opacitySource("net.minecraft.client.gui.screens.ChatScreen") == ScreenRules.Source.EXACT,
                "the status line reports an exact override");
        checkClose(config.opacityFor("com.example.UnknownScreen"), 0.2, 1E-6,
                "unmatched screens still use the global opacity");
        check(config.opacitySource("com.example.UnknownScreen") == ScreenRules.Source.GLOBAL,
                "unmatched screens report the global source");

        var messy = new InventoryConfig();
        messy.excludedScreens.add("  com.example.FancyScreen  ");
        messy.excludedScreens.add("");
        messy.excludedScreens.add("com.example.FancyScreen");
        messy.excludedScreens.add("x".repeat(300));
        messy.additionalScreens.add(null);
        messy.additionalScreens.add("  com.example.ShopScreen ");
        messy.screenOpacities.put("  com.example.* ", 3F);
        messy.screenOpacities.put("   ", 0.5F);
        messy.sanitize();
        check(messy.excludedScreens.size() == 1 && messy.excludedScreens.get(0).equals("com.example.FancyScreen"),
                "screen rules are trimmed, de-duplicated and length-capped");
        check(messy.additionalScreens.size() == 1 && messy.additionalScreens.get(0).equals("com.example.ShopScreen"),
                "opt-in screens are cleaned the same way");
        checkClose(messy.opacityFor("com.example.Thing"), 1, 1E-6,
                "wildcard opacity values are clamped like exact entries");
        check(ScreenRules.matchesAny(messy.additionalScreens, "com.example.ShopScreen"),
                "cleaned opt-in screens still match");
    }

    private static void testReducedMotion() {
        var config = new InventoryConfig();
        check(!config.reducedMotion(), "reduced motion is off by default");
        check(config.effectiveOpeningDurationMs() == 480, "the authored entrance duration is kept");
        check(config.effectiveCameraTransitionDurationMs() == 420, "the authored camera duration is kept");
        config.motion.reduceMotion = true;
        check(config.reducedMotion(), "the preference is reported");
        check(config.effectiveOpeningDurationMs() == 0, "reduced motion snaps the panel into place");
        check(config.effectiveCameraTransitionDurationMs() == 0, "reduced motion snaps the camera");
        config.motion = null;
        config.sanitize();
        check(!config.reducedMotion() && config.effectiveOpeningDurationMs() == 480,
                "missing motion options fall back to the standard entrance");

        long start = 3_000_000_000L;
        var instant = new dev.transparentinventory.client.anim.SpatialAnimationController(start);
        checkClose(instant.motion(start, 0), 1, 1E-9, "a zero-length entrance starts at rest");
        checkClose(instant.progress(start, 0), 1, 1E-9, "a zero-length entrance is fully visible");
        checkClose(instant.impact(start), 0, 1E-9, "a zero-length entrance has no landing flash");
        instant.close(start, 0);
        check(instant.finished(start, 0), "a zero-length close finishes immediately");
        checkClose(instant.progress(start, 0), 0, 1E-9, "a zero-length close hides the panel at once");

        var camera = new dev.transparentinventory.client.camera.CameraTransition();
        checkClose(camera.update(true, start, 0), 1, 1E-9, "a zero-length camera transition opens this frame");
        checkClose(camera.update(false, start + 1, 0), 0, 1E-9, "a zero-length camera transition returns this frame");
        checkClose(camera.update(true, start + 2, 0), 1, 1E-9, "reopening snaps again");

        var eased = new dev.transparentinventory.client.camera.CameraTransition();
        checkClose(eased.update(true, start, 420), 0, 1E-9, "a timed transition starts at the first-person value");
        checkClose(eased.update(true, start + 210_000_000L, 420), 0.5, 1E-6, "a timed transition is half-way at half time");
        checkClose(eased.update(true, start + 420_000_000L, 420), 1, 1E-9, "a timed transition completes");
        checkClose(eased.update(false, start + 421_000_000L, 420), 1, 1E-9, "closing starts from the open value");

        var ordinary = new dev.transparentinventory.client.anim.SpatialAnimationController(start);
        double finalMotion = ordinary.motion(start + 480_000_000L, 480);
        double finalVisibility = ordinary.progress(start + 480_000_000L, 480);
        var settled = dev.transparentinventory.client.render.InventoryEntrance.transform(1920, 1080, 806, 540,
                176, 166, 1.12, finalMotion, finalVisibility, 4, 0);
        var snapped = dev.transparentinventory.client.render.InventoryEntrance.transform(1920, 1080, 806, 540,
                176, 166, 1.12, 1, 1, 4, 0);
        checkClose(snapped.centerX(), settled.centerX(), 1E-9, "reduced motion lands on the settled X");
        checkClose(snapped.centerY(), settled.centerY(), 1E-9, "reduced motion lands on the settled height");
        checkClose(snapped.scale(), settled.scale(), 1E-9, "reduced motion lands on the settled scale");
        checkClose(snapped.tiltDegrees(), 4, 1E-9, "reduced motion keeps the configured tilt");
        checkClose(snapped.centerX(), 1920 * 0.34, 1E-9, "reduced motion uses the resting panel centre");
        var midway = dev.transparentinventory.client.render.InventoryEntrance.transform(1920, 1080, 806, 540,
                176, 166, 1.12, .4, .4, 4, 0);
        check(midway.centerX() < snapped.centerX() - 1, "an ordinary entrance still starts left of the resting pose");
    }

    private static void testConfigRecovery() {
        check(ConfigFileRecovery.backupName("transparent_inventory.json", 0L)
                        .equals("transparent_inventory.json.corrupt-19700101-000000"),
                "backup names are deterministic and Windows-safe");
        check(!ConfigFileRecovery.backupName("config.json", 1_000L)
                        .equals(ConfigFileRecovery.backupName("config.json", 2_000L)),
                "two quarantines never collide");
        check(ConfigFileRecovery.backupName("dir\\config.json", 0L)
                        .equals(ConfigFileRecovery.backupName("dir/config.json", 0L)),
                "directory components are stripped");
        check(ConfigFileRecovery.backupName(null, 0L).startsWith("config.corrupt-"),
                "a missing file name still yields a usable backup name");
        check(ConfigFileRecovery.backupName("a:b?.json", 0L).equals("ab.json.corrupt-19700101-000000"),
                "reserved characters are removed from the backup name");
        String oversized = "x".repeat(200);
        String backup = ConfigFileRecovery.backupName(oversized, 0L);
        check(backup.startsWith("x".repeat(128) + ".corrupt-"), "backup base names are length-capped");
    }

    private static void testCameraPolicy() {
        var config = new InventoryConfig();
        check(config.containerCameraEnabled, "container camera is on by default");
        check(dev.transparentinventory.client.camera.CameraPolicy.enabled(config, true),
                "the inventory may use its camera by default");
        check(dev.transparentinventory.client.camera.CameraPolicy.enabled(config, false),
                "containers may use the camera by default");
        config.containerCameraEnabled = false;
        check(dev.transparentinventory.client.camera.CameraPolicy.enabled(config, true),
                "turning containers off keeps the inventory entrance");
        check(!dev.transparentinventory.client.camera.CameraPolicy.enabled(config, false),
                "containers stay in first person when disabled");
        config.cameraEnabled = false;
        check(!dev.transparentinventory.client.camera.CameraPolicy.enabled(config, true),
                "the master camera switch still wins");
        config.cameraEnabled = true;
        config.containerCameraEnabled = true;
        check(dev.transparentinventory.client.camera.CameraPolicy.enabled(config, false),
                "re-enabling restores the container camera");
        check(!dev.transparentinventory.client.camera.CameraPolicy.enabled(null, true),
                "a missing config disables the camera");
    }

    private static void testQuickReopen() {
        var timing = new EntranceTiming();
        check(!timing.snap(0, 400), "a first open always animates");
        check(timing.expired(0, 400), "no recorded close is already expired");
        timing.closed(1_000_000_000L);
        check(timing.snap(1_000_000_000L, 400), "an immediate reopen snaps");
        check(timing.snap(1_399_999_999L, 400), "the last nanosecond inside the window snaps");
        check(!timing.snap(1_400_000_000L, 400), "the window boundary animates again");
        check(!timing.snap(1_000_000_000L, 0), "a zero window disables snapping");
        check(!timing.snap(999_999_999L, 400), "a backwards clock animates");
        check(timing.expired(1_400_000_000L, 400), "expiry follows the same window");
        timing.reset();
        check(!timing.snap(1_000_000_000L, 400), "reset forgets the last close");

        var config = new InventoryConfig();
        check(config.reopenSnapWindowMs == 400, "quick reopen is enabled by default");
        config.reopenSnapWindowMs = 5000;
        config.sanitize();
        check(config.reopenSnapWindowMs == 1500, "the reopen window is capped");
        config.reopenSnapWindowMs = -20;
        config.sanitize();
        check(config.reopenSnapWindowMs == 0, "a negative reopen window becomes zero");
        config.reopenSnapWindowMs = 400;
        config.motion.reduceMotion = true;
        check(config.effectiveOpeningDurationMs() == 0, "reduced motion still snaps every open");
    }

    private static void testCompatibilityActions() {
        String chest = "com.example.ChestScreen";
        var config = new InventoryConfig();
        check(!config.isScreenExcluded(chest), "screens start included");
        check(config.toggleScreenExclusion("  com.example.ChestScreen "), "the button adds a trimmed exact exclusion");
        check(config.hasExactExclusion(chest) && config.isScreenExcluded(chest), "the exclusion is visible");
        check(!config.toggleScreenExclusion(chest), "toggling again re-includes the screen");
        check(!config.isScreenExcluded(chest), "a re-included screen is handled again");
        check(!config.toggleScreenExclusion(null) && !config.toggleScreenExclusion("   "),
                "invalid names change nothing");

        config.excludedScreens.add("com.example.mods.*");
        check(config.isScreenExcluded("com.example.mods.FancyScreen"), "wildcard rules exclude matching screens");
        check(!config.hasExactExclusion("com.example.mods.FancyScreen"), "a wildcard hit is not an exact entry");
        check(config.toggleScreenExclusion("com.example.mods.FancyScreen"), "clicking cannot override a wildcard");
        check(config.isScreenExcluded("com.example.mods.FancyScreen"), "the wildcard still excludes after the click");
        check(!config.excludedScreens.contains("com.example.mods.FancyScreen"), "no redundant exact entry is added");

        config.clearScreenExclusions();
        check(config.excludedScreens.isEmpty() && !config.isScreenExcluded(chest),
                "clearing removes every exclusion");

        config.screenOpacities.put(chest, 0.4F);
        config.screenOpacities.put("com.example.*", 0.5F);
        config.clearScreenOpacities();
        check(config.screenOpacities.isEmpty(), "clearing per-screen values removes exact and wildcard entries");
        checkClose(config.opacityFor(chest), config.panelOpacity, 1E-6,
                "cleared screens fall back to the global opacity");
    }

    private static void testSettingsPages() {
        check(SettingsPages.COUNT == 10, "the settings screen has ten pages");
        check(SettingsPages.COMPATIBILITY == SettingsPages.COUNT - 1, "compatibility is the last page");
        for (int page = 0; page < SettingsPages.COUNT; page++) {
            check(SettingsPages.clamp(page) == page, "valid page indices are kept");
            check(SettingsPages.next(page) == (page + 1) % SettingsPages.COUNT,
                    "next advances and wraps at the last page");
            check(SettingsPages.previous(page) == (page + SettingsPages.COUNT - 1) % SettingsPages.COUNT,
                    "previous steps back and wraps at the first page");
            check(SettingsPages.titleKey(page).startsWith("spatialui.settings."),
                    "every page has a settings title key");
            check(SettingsPages.nextLabelKey(page).equals(SettingsPages.titleKey(SettingsPages.next(page))),
                    "the next button names the upcoming page");
            check(SettingsPages.previousLabelKey(page).equals(SettingsPages.titleKey(SettingsPages.previous(page))),
                    "the previous button names the page it returns to");
        }
        check(SettingsPages.next(8) == SettingsPages.COMPATIBILITY,
                "player motion is followed by compatibility");
        check(SettingsPages.next(9) == 0, "the last page wraps to the first");
        check(SettingsPages.previous(0) == 9, "the first page wraps to the last");
        check(SettingsPages.previousLabelKey(0).equals("spatialui.settings.compatibility"),
                "going back from the first page lands on compatibility");
        check(SettingsPages.clamp(-4) == 0 && SettingsPages.clamp(37) == 0,
                "out-of-range pages restart at the first page");
        check(SettingsPages.titleKey(-1).equals(SettingsPages.titleKey(0)),
                "invalid titles fall back to the first page");
    }

    private static void testInventoryCursorLook() {
        var left = dev.transparentinventory.client.look.InventoryLookAngles.at(10, 20, 10, 20, 176, 166, 50, 35);
        var right = dev.transparentinventory.client.look.InventoryLookAngles.at(186, 186, 10, 20, 176, 166, 50, 35);
        check(left.yaw() < right.yaw() - 40, "head turns as cursor moves across inventory");
        check(left.pitch() < -30 && right.pitch() > 30, "head follows vertical cursor movement");
        for (int i = -10; i <= 20; i++) {
            var target = dev.transparentinventory.client.look.InventoryLookAngles.at(i * 40, i * 30, 10, 20, 176, 166, 50, 35);
            check(Math.abs(target.yaw()) <= 50 && Math.abs(target.pitch()) <= 35, "cursor outside panel cannot over-rotate head");
        }
        var transform = new dev.transparentinventory.client.render.SpatialTransform(98, 103, 145, 120, 1.12, 4, 0.05, 0.94);
        double sx = transform.screenX(45, 70), sy = transform.screenY(45, 70);
        var projected = dev.transparentinventory.client.look.InventoryLookAngles.at(transform.localX(sx, sy), transform.localY(sx, sy),
                10, 20, 176, 166, 50, 35);
        var direct = dev.transparentinventory.client.look.InventoryLookAngles.at(45, 70, 10, 20, 176, 166, 50, 35);
        checkClose(projected.yaw(), direct.yaw(), 1E-5, "head target follows tilted panel coordinates");
        checkClose(projected.pitch(), direct.pitch(), 1E-5, "head pitch is independent of GUI scale");
        var invalid = dev.transparentinventory.client.look.InventoryLookAngles.at(Double.NaN, 0, 0, 0, 176, 166, 50, 35);
        check(invalid.yaw() == 0 && invalid.pitch() == 0, "invalid mouse input yields neutral head pose");
        var config = new InventoryConfig();
        config.containerScale = Double.POSITIVE_INFINITY;
        config.sanitize();
        checkClose(config.containerScale, 2, 1E-9, "container size is safely bounded");
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
