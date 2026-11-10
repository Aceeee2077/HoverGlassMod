package dev.transparentinventory.client;

import dev.transparentinventory.client.anim.EntranceTiming;
import dev.transparentinventory.client.anim.InventoryAnimator;
import dev.transparentinventory.client.anim.SlideTracker;
import dev.transparentinventory.client.layout.InventoryLayout;
import dev.transparentinventory.client.look.LookTracker;
import dev.transparentinventory.client.motion.AvatarMotionMath;
import dev.transparentinventory.client.render.Opacity;
import dev.transparentinventory.client.screen.SettingsPages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        testCameraOffsets();
        testQuickReopen();
        testCompatibilityActions();
        testSettingsPages();
        testLanguageKeys();
        testConfigTransfer();
        testConfigBackups();
        testHotbarRim();
        testGlassMaterials();
        testGlassMaterialPresets();
        testPageDefaults();
        testScreenOptIn();
        testSettingsKey();
        testInventoryCursorLook();
        testMotionUtilities();
        testAvatarMotion();
        testAirbornePose();
        testClimbPose();
        testLandingPose();
        testIdleSway();
        testOpacityFade();
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

        // The F8 follow-speed slider maps onto this scale: faster means closer after
        // the same frames, and neither setting may overshoot the requested angle.
        LookTracker loose = new LookTracker();
        LookTracker snappy = new LookTracker();
        loose.update(0.0F, 0.0F, 0.0F, 1.0 / 60.0, 1.0);
        snappy.update(0.0F, 0.0F, 0.0F, 1.0 / 60.0, 1.0);
        for (int i = 0; i < 10; i++) {
            loose.update(40.0F, 0.0F, 0.0F, 1.0 / 60.0, 1.0);
            snappy.update(40.0F, 0.0F, 0.0F, 1.0 / 60.0, 40.0);
        }
        check(snappy.headYaw() > loose.headYaw(), "a higher follow speed tracks faster");
        check(snappy.headYaw() < 40.0F && loose.headYaw() < 40.0F,
                "neither follow speed overshoots the requested angle");
        checkClose(new LookTracker().headYaw(), 0.0, 1.0E-9, "a fresh tracker starts at rest");
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

    private static void testCameraOffsets() {
        InventoryConfig config = new InventoryConfig();
        checkClose(config.cameraYawOffset, 0, 1E-9, "the camera starts square on");
        checkClose(config.cameraPitchOffset, 0, 1E-9, "the camera starts level");
        config.cameraYawOffset = 500;
        config.cameraPitchOffset = -500;
        config.sanitize();
        checkClose(config.cameraYawOffset, 60, 1E-9, "the yaw offset clamps high");
        checkClose(config.cameraPitchOffset, -45, 1E-9, "the pitch offset clamps low");
        config.cameraYawOffset = Double.NaN;
        config.sanitize();
        checkClose(config.cameraYawOffset, -60, 1E-9, "a NaN yaw offset falls back to its lower bound");
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

        config.motion.reduceMotion = false;
        checkClose(config.reopenWindowFraction(), 400 / 1500.0, 1E-9,
                "the default window is a fraction of the slider range");
        config.setReopenWindowFraction(1);
        check(config.reopenSnapWindowMs == 1500, "the slider maximum selects the full window");
        config.setReopenWindowFraction(0);
        check(config.reopenSnapWindowMs == 0, "the slider minimum disables the shortcut");
        config.setReopenWindowFraction(2);
        check(config.reopenSnapWindowMs == 1500, "an out-of-range fraction clamps high");
        config.setReopenWindowFraction(-1);
        check(config.reopenSnapWindowMs == 0, "an out-of-range fraction clamps low");
        config.setReopenWindowFraction(0.5);
        check(config.reopenSnapWindowMs == 750 && Math.abs(config.reopenWindowFraction() - 0.5) < 1E-9,
                "the slider fraction round-trips");
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
        check(SettingsPages.COUNT == 15, "the settings screen has fifteen pages");
        check(SettingsPages.COMPATIBILITY == 9 && SettingsPages.CAMERA_ANGLES == 10
                        && SettingsPages.GLASS_MATERIAL == 11 && SettingsPages.SCREEN_SUPPORT == 12
                        && SettingsPages.PREVIEW_LAYOUT == 13 && SettingsPages.PANEL_FEEL == 14,
                "compatibility keeps its position and the advanced pages follow it");
        check(SettingsPages.titleKey(SettingsPages.COMPATIBILITY).equals("spatialui.settings.compatibility"),
                "the compatibility index names the compatibility page");
        check(SettingsPages.titleKey(SettingsPages.CAMERA_ANGLES).equals("spatialui.settings.camera_angles"),
                "the camera angle index names the camera angle page");
        check(SettingsPages.titleKey(SettingsPages.GLASS_MATERIAL).equals("spatialui.settings.glass_material"),
                "the glass material index names the glass material page");
        check(SettingsPages.titleKey(SettingsPages.SCREEN_SUPPORT).equals("spatialui.settings.screen_support"),
                "the screen support index names the screen support page");
        check(SettingsPages.titleKey(SettingsPages.PREVIEW_LAYOUT).equals("spatialui.settings.preview_layout"),
                "the preview layout index names the preview layout page");
        check(SettingsPages.titleKey(SettingsPages.PANEL_FEEL).equals("spatialui.settings.panel_feel"),
                "the panel feel index names the panel feel page");
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
        check(SettingsPages.next(SettingsPages.COMPATIBILITY) == SettingsPages.CAMERA_ANGLES,
                "compatibility is followed by camera angles");
        check(SettingsPages.next(SettingsPages.CAMERA_ANGLES) == SettingsPages.GLASS_MATERIAL,
                "camera angles are followed by glass material");
        check(SettingsPages.next(SettingsPages.GLASS_MATERIAL) == SettingsPages.SCREEN_SUPPORT,
                "glass material is followed by screen support");
        check(SettingsPages.next(SettingsPages.SCREEN_SUPPORT) == SettingsPages.PREVIEW_LAYOUT,
                "screen support is followed by preview layout");
        check(SettingsPages.next(SettingsPages.PREVIEW_LAYOUT) == SettingsPages.PANEL_FEEL,
                "preview layout is followed by panel feel");
        check(SettingsPages.next(SettingsPages.PANEL_FEEL) == 0, "the last page wraps to the first");
        check(SettingsPages.previous(0) == SettingsPages.PANEL_FEEL,
                "the first page wraps to the last");
        check(SettingsPages.previousLabelKey(0).equals("spatialui.settings.panel_feel"),
                "going back from the first page lands on panel feel");
        check(SettingsPages.clamp(-4) == 0 && SettingsPages.clamp(37) == 0,
                "out-of-range pages restart at the first page");
        check(SettingsPages.titleKey(-1).equals(SettingsPages.titleKey(0)),
                "invalid titles fall back to the first page");
        check(SettingsPages.shift(0, 1) == 1, "one step forward advances a page");
        check(SettingsPages.shift(SettingsPages.last(), 1) == 0, "stepping forward from the last page wraps");
        check(SettingsPages.shift(0, -1) == SettingsPages.last(), "stepping back from the first page wraps");
        check(SettingsPages.shift(5, 2 * SettingsPages.COUNT) == 5, "a full cycle of steps returns to the same page");
        check(SettingsPages.shift(5, -2 * SettingsPages.COUNT) == 5, "a full cycle backwards returns to the same page");
        check(SettingsPages.scrollStep(1) == -1, "scrolling up moves to the previous page");
        check(SettingsPages.scrollStep(-1) == 1, "scrolling down moves to the next page");
        check(SettingsPages.scrollStep(0) == 0, "no scrolling does not change pages");
        check(SettingsPages.scrollStep(0.49) == 0 && SettingsPages.scrollStep(-0.49) == 0,
                "sub-notch scroll amounts are ignored");
        check(SettingsPages.scrollStep(0.5) == -1, "a half-notch scroll still counts");
        check(SettingsPages.scrollStep(Double.NaN) == 0, "invalid scroll amounts are ignored");
        check(SettingsPages.scrollStep(Double.POSITIVE_INFINITY) == 0
                        && SettingsPages.scrollStep(Double.NEGATIVE_INFINITY) == 0,
                "infinite scroll amounts are ignored");
        check(SettingsPages.last() == SettingsPages.COUNT - 1, "End jumps to the last page");
        check(SettingsPages.remembered() == 0, "the first open starts on the transparency page");
        check(SettingsPages.remember(7) == 7 && SettingsPages.remembered() == 7,
                "the last opened page is remembered");
        check(SettingsPages.remember(42) == 0 && SettingsPages.remembered() == 0,
                "an invalid remembered page falls back to the first");
        SettingsPages.remember(0);
    }

    private static void testLanguageKeys() {
        var english = langKeys("en_us");
        var chinese = langKeys("zh_cn");
        check(!english.isEmpty() && !chinese.isEmpty(), "both language files load from the classpath");
        check(english.equals(chinese), "en_us and zh_cn define exactly the same keys");
        for (int page = 0; page < SettingsPages.COUNT; page++) {
            String key = SettingsPages.titleKey(page);
            check(english.contains(key), "every settings page title is translated: " + key);
        }
        check(english.contains("spatialui.settings.page"), "the page counter is translated");
        check(english.contains("spatialui.transfer.copy"), "the clipboard actions are translated");
    }

    private static java.util.Set<String> langKeys(String locale) {
        String path = "assets/transparent_inventory/lang/" + locale + ".json";
        try (var stream = InventoryRegressionTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return java.util.Set.of();
            var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8);
            var keys = new java.util.TreeSet<String>();
            com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().keySet().forEach(keys::add);
            return keys;
        } catch (IOException | RuntimeException e) {
            throw new AssertionError("could not read " + path, e);
        }
    }

    private static void testConfigTransfer() {
        InventoryConfig source = new InventoryConfig();
        source.panelOpacity = 0.23F;
        source.containerScale = 1.7;
        source.openingAnimationDurationMs = 610;
        source.inventoryCameraSide = 0.61;
        source.motion.reduceMotion = true;
        source.motion.particleMultiplier = 1.4;
        source.screenOpacities.put("com.example.ChestScreen", 0.44F);
        source.screenOpacities.put("com.example.*", 0.5F);
        source.excludedScreens.add("com.example.HiddenScreen");
        source.excludedScreens.add("*Debug*");

        String json = ConfigTransfer.export(source);
        check(json.startsWith("{") && json.endsWith("}"), "exported settings are a JSON object");
        check(json.contains("\n"), "exported settings are pretty-printed");

        InventoryConfig parsed = ConfigTransfer.parse(json);
        check(parsed != null, "exported settings parse again");
        checkClose(parsed.panelOpacity, 0.23, 1E-6, "panel opacity survives a round trip");
        checkClose(parsed.containerScale, 1.7, 1E-6, "container scale survives a round trip");
        check(parsed.openingAnimationDurationMs == 610, "durations survive a round trip");
        checkClose(parsed.inventoryCameraSide, 0.61, 1E-6, "camera framing survives a round trip");
        check(parsed.motion.reduceMotion && Math.abs(parsed.motion.particleMultiplier - 1.4) < 1E-9,
                "motion options survive a round trip");
        checkClose(parsed.opacityFor("com.example.ChestScreen"), 0.44, 1E-6,
                "exact screen opacity survives a round trip");
        checkClose(parsed.opacityFor("com.example.OtherScreen"), 0.5, 1E-6,
                "wildcard screen opacity survives a round trip");
        check(parsed.isScreenExcluded("com.example.HiddenScreen"), "exact exclusions survive a round trip");
        check(parsed.isScreenExcluded("aDebugb"), "wildcard exclusions survive a round trip");

        check(ConfigTransfer.parse(null) == null && ConfigTransfer.parse("   ") == null,
                "blank text is rejected");
        check(ConfigTransfer.parse("hello") == null, "arbitrary text is rejected");
        check(ConfigTransfer.parse("null") == null, "a null literal is rejected");
        check(ConfigTransfer.parse("[]") == null, "JSON arrays are rejected");
        InventoryConfig empty = ConfigTransfer.parse("{}");
        check(empty != null, "an empty object yields defaults");
        checkClose(empty.panelOpacity, new InventoryConfig().panelOpacity, 1E-6,
                "an empty object keeps the default opacity");

        InventoryConfig odd = ConfigTransfer.parse(
                "{\"panelOpacity\": 7, \"reopenSnapWindowMs\": -3, \"excludedScreens\": [\"  \", \"a\", \"a\"]}");
        check(odd != null, "out-of-range values still parse");
        checkClose(odd.panelOpacity, 1, 1E-6, "opacity is clamped on import");
        check(odd.reopenSnapWindowMs == 0, "negative windows clamp to zero on import");
        check(odd.excludedScreens.equals(java.util.List.of("a")), "screen rules are cleaned on import");

        check(ConfigTransfer.parse("x".repeat(ConfigTransfer.MAX_LENGTH + 1)) == null,
                "oversized pasted text is rejected");
    }

    private static void testAirbornePose() {
        AvatarMotionMath.Pose rising = AvatarMotionMath.airborne(0.4F, 1F);
        check(rising != AvatarMotionMath.Pose.NONE, "jumping produces an airborne pose");
        check(finite(rising), "the rise pose is finite");
        check(rising.rightLeg().pitch() > 0 && rising.rightLeg().pitch() == rising.leftLeg().pitch(),
                "both knees tuck together while rising");
        checkClose(rising.rightArm().roll(), -rising.leftArm().roll(), 1E-6, "rise arms mirror");
        check(rising.body().pitch() < 0, "the torso settles back while rising");

        AvatarMotionMath.Pose falling = AvatarMotionMath.airborne(-0.3F, 1F);
        check(falling != AvatarMotionMath.Pose.NONE, "falling produces an airborne pose");
        check(finite(falling), "the fall pose is finite");
        check(falling.rightLeg().pitch() < 0 && falling.rightLeg().pitch() == falling.leftLeg().pitch(),
                "both legs trail together while falling");
        checkClose(falling.rightArm().roll(), -falling.leftArm().roll(), 1E-6, "fall arms mirror");
        check(Math.abs(falling.rightArm().roll()) > Math.abs(falling.rightLeg().pitch()),
                "falling spreads the arms more than the legs");

        check(AvatarMotionMath.airborne(0F, 1F) == AvatarMotionMath.Pose.NONE,
                "standing still adds no pose");
        check(AvatarMotionMath.airborne(0.05F, 1F) == AvatarMotionMath.Pose.NONE,
                "small steps stay vanilla");
        check(AvatarMotionMath.airborne(Float.NaN, 1F) == AvatarMotionMath.Pose.NONE,
                "an invalid vertical speed adds no pose");
        check(AvatarMotionMath.airborne(0.4F, 0F) == AvatarMotionMath.Pose.NONE,
                "zero intensity adds no pose");

        AvatarMotionMath.Pose half = AvatarMotionMath.airborne(0.4F, .5F);
        checkClose(half.rightLeg().pitch(), rising.rightLeg().pitch() * .5, 1E-6,
                "airborne intensity scales linearly");
        check(AvatarMotionMath.airborne(5F, 1F).equals(AvatarMotionMath.airborne(0.6F, 1F)),
                "extreme speed clamps to the authored range");
    }

    private static void testConfigBackups() {
        try {
            Path dir = Files.createTempDirectory("hoverglass-backup");
            Path config = dir.resolve("config.json");
            check(ConfigBackups.backupPath(config).getFileName().toString().equals("config.json.bak"),
                    "the backup is a sibling with a .bak suffix");
            check(!ConfigBackups.copy(config), "a missing config has nothing to back up");
            check(!ConfigBackups.exists(config), "no backup exists before the first save");

            Files.writeString(config, "first");
            check(ConfigBackups.copy(config), "the save backs up the previous file");
            check(ConfigBackups.exists(config), "the backup is readable after copying");
            check(Files.readString(ConfigBackups.backupPath(config)).equals("first"),
                    "the backup holds the previous contents");

            Files.writeString(config, "second");
            check(ConfigBackups.copy(config), "the next save refreshes the backup");
            check(Files.readString(ConfigBackups.backupPath(config)).equals("second"),
                    "only the latest previous version is kept");

            Path rotation = ConfigBackups.rotationDirectory(config);
            check(rotation != null && rotation.getFileName().toString().equals("transparent_inventory-backups"),
                    "rotating snapshots live in a sibling folder");
            check(ConfigBackups.rotate(dir.resolve("missing.json"), 1L) == -1,
                    "a missing config has nothing to rotate");
            try (var stream = Files.list(rotation)) {
                check(stream.anyMatch(Files::isRegularFile), "a save records a rotating snapshot");
            }
            // Later than the system-clock snapshots above, so pruning has a fixed order.
            long stamp = 4_000_000_000_000L;
            int kept = 0;
            for (int index = 0; index < ConfigBackups.KEEP + 2; index++) {
                Files.writeString(config, "v" + index);
                kept = ConfigBackups.rotate(config, stamp + index);
            }
            check(kept == ConfigBackups.KEEP, "rotation keeps exactly the newest snapshots");
            try (var stream = Files.list(rotation)) {
                var names = stream.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString()).sorted().toList();
                check(names.size() == ConfigBackups.KEEP, "only five snapshots remain on disk");
                check(names.get(names.size() - 1).contains(String.valueOf(stamp + ConfigBackups.KEEP + 1)),
                        "the newest snapshot survives pruning");
                check(names.stream().noneMatch(name -> name.contains(String.valueOf(stamp))),
                        "the oldest snapshots are pruned first");
                check(Files.readString(rotation.resolve(names.get(names.size() - 1)))
                                .equals("v" + (ConfigBackups.KEEP + 1)),
                        "the newest snapshot holds the latest previous contents");
                Path newest = ConfigBackups.newest(config);
                check(newest != null && newest.getFileName().toString().equals(names.get(names.size() - 1)),
                        "the newest snapshot is the last one written");
                Files.writeString(rotation.resolve("notes.txt"), "unrelated");
                check(ConfigBackups.newest(config).equals(newest), "unrelated files are not snapshots");
                Files.deleteIfExists(rotation.resolve("notes.txt"));
            }
            check(ConfigBackups.newest(dir.resolve("nested").resolve("elsewhere.json")) == null,
                    "a config without a rotation folder has no newest snapshot");

            Files.deleteIfExists(config);
            check(ConfigBackups.exists(config), "the backup survives a missing config file");
            try (var stream = Files.list(rotation)) {
                for (Path snapshot : stream.toList()) Files.deleteIfExists(snapshot);
            }
            Files.deleteIfExists(rotation);
            check(ConfigBackups.newest(config) == null, "a missing rotation folder has no newest snapshot");
            Files.deleteIfExists(ConfigBackups.backupPath(config));
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            throw new AssertionError("config backup test could not use a temp directory", e);
        }
    }

    private static void testHotbarRim() {
        InventoryConfig config = new InventoryConfig();
        checkClose(config.hotbarBorderOpacity, 0.72, 1E-6,
                "the hotbar rim defaults to the standard rim strength");
        config.hotbarBorderOpacity = 3F;
        config.sanitize();
        checkClose(config.hotbarBorderOpacity, 1, 1E-6, "the hotbar rim clamps above 1");
        config.hotbarBorderOpacity = -1F;
        config.sanitize();
        checkClose(config.hotbarBorderOpacity, 0, 1E-6, "the hotbar rim clamps below 0");

        config.hotbarBorderOpacity = 0.35F;
        config.borderOpacity = 0.9F;
        config.sanitize();
        checkClose(config.hotbarBorderOpacity, 0.35, 1E-6,
                "the hotbar rim is independent of the panel border");
        checkClose(config.borderOpacity, 0.9, 1E-6, "the panel border keeps its own value");
    }

    private static void testClimbPose() {
        float reachTick = (float) (Math.PI / .7); // sin(age * .35) == 1
        AvatarMotionMath.Pose reach = AvatarMotionMath.climb(reachTick, 0.2F, 1F);
        check(reach != AvatarMotionMath.Pose.NONE, "climbing upward produces a pose");
        check(finite(reach), "the climb pose is finite");
        check(reach.rightArm().pitch() < 0 && reach.leftArm().pitch() < 0,
                "both arms stay raised while climbing");
        check(reach.rightArm().pitch() < reach.leftArm().pitch()
                        && reach.leftLeg().pitch() < reach.rightLeg().pitch(),
                "the reach is diagonal: right arm with left leg");

        AvatarMotionMath.Pose pull = AvatarMotionMath.climb(reachTick, -0.2F, 1F);
        check(pull.rightArm().pitch() > pull.leftArm().pitch()
                        && pull.rightLeg().pitch() < pull.leftLeg().pitch(),
                "climbing down mirrors the reach");
        checkClose(reach.rightArm().pitch() + reach.leftArm().pitch(),
                pull.rightArm().pitch() + pull.leftArm().pitch(), 1E-6,
                "arm base angle does not change direction");

        AvatarMotionMath.Pose half = AvatarMotionMath.climb(reachTick, 0.2F, .5F);
        checkClose(half.rightArm().pitch(), reach.rightArm().pitch() * .5, 1E-6,
                "climb intensity scales linearly");
        check(AvatarMotionMath.climb(reachTick, 0F, 1F) == AvatarMotionMath.Pose.NONE,
                "hanging still adds no climb pose");
        check(AvatarMotionMath.climb(reachTick, 0.005F, 1F) == AvatarMotionMath.Pose.NONE,
                "tiny movement stays vanilla");
        check(AvatarMotionMath.climb(reachTick, 0.2F, 0F) == AvatarMotionMath.Pose.NONE,
                "zero intensity adds no pose");
        check(AvatarMotionMath.climb(Float.NaN, 0.2F, 1F) == AvatarMotionMath.Pose.NONE,
                "an invalid clock adds no pose");
    }

    private static void testLandingPose() {
        AvatarMotionMath.Pose start = AvatarMotionMath.landing(0F, .9F, 1F);
        check(start != AvatarMotionMath.Pose.NONE, "a real fall produces a landing pose");
        check(finite(start), "the landing pose is finite");
        check(start.rightLeg().pitch() > 0 && start.rightLeg().pitch() == start.leftLeg().pitch(),
                "both knees absorb the landing symmetrically");
        check(start.rightArm().roll() < 0 && start.leftArm().roll() > 0,
                "the arms spread outward for balance");
        check(start.body().pitch() > 0, "the torso folds into the landing");
        check(start.head().pitch() < 0, "the head counter-rotates to stay level");
        for (AvatarMotionMath.Limb limb : new AvatarMotionMath.Limb[]{start.body(), start.head(),
                start.rightArm(), start.leftArm(), start.rightLeg(), start.leftLeg()}) {
            check(Math.abs(limb.pitch()) <= .5 && Math.abs(limb.yaw()) <= .5 && Math.abs(limb.roll()) <= .5,
                    "landing rotations stay bounded");
        }

        AvatarMotionMath.Pose middle = AvatarMotionMath.landing(3F, .9F, 1F);
        AvatarMotionMath.Pose end = AvatarMotionMath.landing(AvatarMotionMath.LANDING_TICKS, .9F, 1F);
        check(end == AvatarMotionMath.Pose.NONE, "the recovery ends exactly on the last tick");
        check(middle.rightLeg().pitch() < start.rightLeg().pitch(), "the crouch fades out over time");
        check(middle.rightLeg().pitch() > 0, "the crouch is still visible mid-recovery");

        check(AvatarMotionMath.landing(0F, .2F, 1F) == AvatarMotionMath.Pose.NONE,
                "step-downs stay vanilla");
        check(AvatarMotionMath.landing(0F, .9F, 0F) == AvatarMotionMath.Pose.NONE,
                "zero intensity adds no pose");
        check(AvatarMotionMath.landing(-1F, .9F, 1F) == AvatarMotionMath.Pose.NONE,
                "a negative age adds no pose");
        check(AvatarMotionMath.landing(Float.NaN, .9F, 1F) == AvatarMotionMath.Pose.NONE,
                "an invalid age adds no pose");
        check(AvatarMotionMath.landing(0F, Float.NaN, 1F) == AvatarMotionMath.Pose.NONE,
                "an invalid impact adds no pose");

        AvatarMotionMath.Pose half = AvatarMotionMath.landing(0F, .9F, .5F);
        checkClose(half.rightLeg().pitch(), start.rightLeg().pitch() * .5, 1E-6,
                "landing intensity scales linearly");

        AvatarMotionMath.Pose heavy = AvatarMotionMath.landing(0F, 1.5F, 1F);
        AvatarMotionMath.Pose light = AvatarMotionMath.landing(0F, .5F, 1F);
        check(heavy.rightLeg().pitch() >= light.rightLeg().pitch(), "harder falls absorb deeper");
        check(heavy.rightLeg().pitch() <= .4, "the hardest landing stays a small crouch");
    }

    private static void testIdleSway() {
        AvatarMotionMath.Pose idle = AvatarMotionMath.idle(10F, 0F, 1F);
        check(idle != AvatarMotionMath.Pose.NONE, "a stationary avatar breathes");
        check(finite(idle), "the idle pose is finite");
        for (AvatarMotionMath.Limb limb : new AvatarMotionMath.Limb[]{idle.body(), idle.head(),
                idle.rightArm(), idle.leftArm(), idle.rightLeg(), idle.leftLeg()}) {
            check(Math.abs(limb.pitch()) <= .05 && Math.abs(limb.yaw()) <= .05 && Math.abs(limb.roll()) <= .05,
                    "idle rotations stay subtle");
        }
        check(idle.rightArm().roll() < 0 && idle.leftArm().roll() > 0,
                "the arms drift outward on the breath");
        check(idle.rightLeg() == AvatarMotionMath.Limb.NONE && idle.leftLeg() == AvatarMotionMath.Limb.NONE,
                "the idle layer leaves the legs to vanilla");
        check(!AvatarMotionMath.idle(10F, 0F, 1F).equals(AvatarMotionMath.idle(30F, 0F, 1F)),
                "the sway changes over time");

        check(AvatarMotionMath.idle(10F, .2F, 1F) == AvatarMotionMath.Pose.NONE,
                "walking stops the idle layer");
        check(AvatarMotionMath.idle(10F, 0F, 0F) == AvatarMotionMath.Pose.NONE,
                "zero intensity adds no pose");
        check(AvatarMotionMath.idle(Float.NaN, 0F, 1F) == AvatarMotionMath.Pose.NONE,
                "an invalid clock adds no pose");
        check(AvatarMotionMath.idle(-1F, 0F, 1F) == AvatarMotionMath.Pose.NONE,
                "a negative clock adds no pose");
        check(AvatarMotionMath.idle(10F, Float.NaN, 1F) == AvatarMotionMath.Pose.NONE,
                "an invalid walk speed adds no pose");

        AvatarMotionMath.Pose half = AvatarMotionMath.idle(10F, 0F, .5F);
        checkClose(half.body().pitch(), idle.body().pitch() * .5, 1E-6,
                "idle intensity scales linearly");
    }

    private static void testGlassMaterials() {
        InventoryConfig config = new InventoryConfig();
        checkClose(config.slotOpacity, 0.09, 1E-6, "the slot fill starts subtle");
        checkClose(config.borderOpacity, 0.72, 1E-6, "the panel rim starts strong");
        checkClose(config.slotBorderOpacity, 0.58, 1E-6, "the slot rim starts mid-strength");
        checkClose(config.hoverOpacity, 0.18, 1E-6, "the hover highlight starts visible");
        checkClose(config.selectedOpacity, 0.32, 1E-6, "the selected highlight starts clearer than hover");
        check(config.selectedOpacity > config.hoverOpacity, "selection reads stronger than hover");
        config.slotOpacity = 4;
        config.borderOpacity = -2;
        config.slotBorderOpacity = Float.NaN;
        config.hoverOpacity = 9;
        config.selectedOpacity = -9;
        config.sanitize();
        checkClose(config.slotOpacity, 1, 1E-6, "the slot fill clamps above 1");
        checkClose(config.borderOpacity, 0, 1E-6, "the panel rim clamps below 0");
        checkClose(config.slotBorderOpacity, 0, 1E-6, "a NaN slot rim falls back to its lower bound");
        checkClose(config.hoverOpacity, 1, 1E-6, "the hover highlight clamps above 1");
        checkClose(config.selectedOpacity, 0, 1E-6, "the selected highlight clamps below 0");
    }

    private static void testGlassMaterialPresets() {
        InventoryConfig config = new InventoryConfig();
        check(GlassMaterialPresets.matches(config, GlassMaterialPresets.Preset.FROSTED),
                "fresh defaults match the frosted preset");
        check(!GlassMaterialPresets.matches(config, GlassMaterialPresets.Preset.CLEAR),
                "fresh defaults are not the clear preset");
        for (GlassMaterialPresets.Preset preset : GlassMaterialPresets.Preset.values()) {
            GlassMaterialPresets.apply(config, preset);
            check(GlassMaterialPresets.matches(config, preset), "applying " + preset + " matches it back");
            check(GlassMaterialPresets.current(config) == preset, "current() reports " + preset);
            checkClose(config.slotOpacity, preset.slotOpacity(), 1E-6, preset + " slot fill");
            checkClose(config.borderOpacity, preset.borderOpacity(), 1E-6, preset + " panel rim");
            checkClose(config.slotBorderOpacity, preset.slotBorderOpacity(), 1E-6, preset + " slot rim");
            checkClose(config.hoverOpacity, preset.hoverOpacity(), 1E-6, preset + " hover highlight");
            checkClose(config.selectedOpacity, preset.selectedOpacity(), 1E-6, preset + " selected highlight");
            for (float alpha : new float[]{config.slotOpacity, config.borderOpacity, config.slotBorderOpacity,
                    config.hoverOpacity, config.selectedOpacity}) {
                check(alpha >= 0 && alpha <= 1, preset + " alphas stay inside [0, 1]");
            }
            check(preset.hoverOpacity() < preset.selectedOpacity(), preset + " keeps selection stronger than hover");
        }
        GlassMaterialPresets.apply(config, GlassMaterialPresets.Preset.CLEAR);
        check(GlassMaterialPresets.Preset.CLEAR.slotOpacity() < GlassMaterialPresets.Preset.FROSTED.slotOpacity()
                        && GlassMaterialPresets.Preset.FROSTED.slotOpacity() < GlassMaterialPresets.Preset.VIVID.slotOpacity(),
                "slot fill thickens from clear to vivid");
        check(GlassMaterialPresets.Preset.CLEAR.borderOpacity() < GlassMaterialPresets.Preset.FROSTED.borderOpacity()
                        && GlassMaterialPresets.Preset.FROSTED.borderOpacity() < GlassMaterialPresets.Preset.VIVID.borderOpacity(),
                "panel rim strengthens from clear to vivid");
        check(GlassMaterialPresets.Preset.CLEAR.slotBorderOpacity() < GlassMaterialPresets.Preset.FROSTED.slotBorderOpacity()
                        && GlassMaterialPresets.Preset.FROSTED.slotBorderOpacity() < GlassMaterialPresets.Preset.VIVID.slotBorderOpacity(),
                "slot rim strengthens from clear to vivid");
        check(GlassMaterialPresets.Preset.CLEAR.hoverOpacity() < GlassMaterialPresets.Preset.FROSTED.hoverOpacity()
                        && GlassMaterialPresets.Preset.FROSTED.hoverOpacity() < GlassMaterialPresets.Preset.VIVID.hoverOpacity(),
                "hover highlight strengthens from clear to vivid");
        check(GlassMaterialPresets.Preset.CLEAR.selectedOpacity() < GlassMaterialPresets.Preset.FROSTED.selectedOpacity()
                        && GlassMaterialPresets.Preset.FROSTED.selectedOpacity() < GlassMaterialPresets.Preset.VIVID.selectedOpacity(),
                "selected highlight strengthens from clear to vivid");
        config.slotOpacity = 0.077F;
        check(GlassMaterialPresets.current(config) == null, "hand-tuned alphas match no preset");
        config.sanitize();
        GlassMaterialPresets.apply(config, GlassMaterialPresets.Preset.VIVID);
        GlassMaterialPresets.apply(config, GlassMaterialPresets.Preset.VIVID);
        check(GlassMaterialPresets.matches(config, GlassMaterialPresets.Preset.VIVID),
                "re-applying a preset stays idempotent");
    }

    private static void testPageDefaults() {
        for (int page = 0; page < SettingsPages.COUNT; page++) {
            check(PageDefaults.supports(page) == (page != SettingsPages.COMPATIBILITY),
                    "page " + page + " reset support");
        }
        check(!PageDefaults.supports(-1) && !PageDefaults.supports(SettingsPages.COUNT),
                "out-of-range pages have no reset");

        InventoryConfig config = new InventoryConfig();

        config.panelOpacity = .9F;
        config.worldDimOpacity = .5F;
        config.screenOpacities.put("net.minecraft.client.gui.screens.inventory.InventoryScreen", .4F);
        check(PageDefaults.reset(config, 0), "the transparency page resets");
        checkClose(config.panelOpacity, .18, 1E-6, "page reset restores the global transparency");
        checkClose(config.worldDimOpacity, 0, 1E-6, "page reset restores the world dim");
        check(config.screenOpacities.isEmpty(), "page reset clears per-screen overrides");

        config.guiScale = 1.5;
        config.cameraDistance = 6;
        config.cameraTransitionDurationMs = 1200;
        config.cameraEnabled = false;
        config.worldAnchoringEnabled = false;
        config.containerScale = 2;
        config.containerCameraEnabled = false;
        check(PageDefaults.reset(config, 1), "the spatial page resets");
        checkClose(config.guiScale, 1.0, 1E-6, "page reset restores the GUI scale");
        checkClose(config.cameraDistance, 3.0, 1E-6, "page reset restores the camera distance");
        check(config.cameraTransitionDurationMs == 420, "page reset restores the camera duration");
        check(config.cameraEnabled && config.worldAnchoringEnabled, "page reset re-enables camera toggles");
        checkClose(config.containerScale, 1.35, 1E-6, "page reset restores the container scale");
        check(config.containerCameraEnabled, "page reset re-enables the container camera");

        config.cinematicInventory = false;
        config.closeAnimation = false;
        config.inventoryTiltDegrees = -9;
        config.openingAnimationDurationMs = 900;
        config.glassInventory = false;
        config.inventoryCameraSide = .95;
        config.reopenSnapWindowMs = 1500;
        check(PageDefaults.reset(config, 2), "the entrance page resets");
        check(config.cinematicInventory && config.glassInventory, "page reset re-enables the entrance toggles");
        check(config.closeAnimation, "page reset re-enables the closing animation");
        check(new InventoryConfig().closeAnimation, "the closing animation ships enabled");
        checkClose(config.inventoryTiltDegrees, 4.0, 1E-6, "page reset restores the tilt");
        check(config.openingAnimationDurationMs == 480, "page reset restores the opening duration");
        checkClose(config.inventoryCameraSide, .72, 1E-6, "page reset restores the camera side");
        check(config.reopenSnapWindowMs == 400, "page reset restores the quick reopen window");

        config.playerLookAtInventory = false;
        config.maxHeadYaw = 10;
        config.maxHeadPitch = 5;
        config.lookSmoothing = 40;
        check(PageDefaults.reset(config, 3), "the head follow page resets");
        check(config.playerLookAtInventory, "page reset re-enables head tracking");
        checkClose(config.maxHeadYaw, 50, 1E-6, "page reset restores the yaw range");
        checkClose(config.maxHeadPitch, 35, 1E-6, "page reset restores the pitch range");
        checkClose(config.lookSmoothing, 11, 1E-6, "page reset restores the follow speed");

        config.glassHotbar = false;
        config.hotbarOpacity = .6F;
        config.hotbarBorderOpacity = .2F;
        check(PageDefaults.reset(config, 4), "the hotbar page resets");
        check(config.glassHotbar, "page reset re-enables the glass hotbar");
        checkClose(config.hotbarOpacity, .16, 1E-6, "page reset restores the hotbar fill");
        checkClose(config.hotbarBorderOpacity, .72, 1E-6, "page reset restores the hotbar rim");

        config.motion.enabled = false;
        config.motion.reduceMotion = true;
        config.motion.itemSwap = false;
        config.motion.attackAnimation = false;
        config.motion.firstPersonIntensity = .1;
        check(PageDefaults.reset(config, 5), "the first-person motion page resets");
        check(config.motion.enabled && !config.motion.reduceMotion, "page reset restores the motion master switches");
        check(config.motion.itemSwap && config.motion.attackAnimation, "page reset re-enables first-person layers");
        checkClose(config.motion.firstPersonIntensity, .65, 1E-6, "page reset restores the hand intensity");

        config.motion.sprintSway = false;
        config.motion.jumpMotion = false;
        config.motion.landingMotion = false;
        config.motion.hitShake = false;
        config.motion.cameraIntensity = .9;
        check(PageDefaults.reset(config, 6), "the camera motion page resets");
        check(config.motion.sprintSway && config.motion.jumpMotion && config.motion.landingMotion
                && config.motion.hitShake, "page reset re-enables camera motion layers");
        checkClose(config.motion.cameraIntensity, .5, 1E-6, "page reset restores the camera intensity");

        config.motion.blockBreakEffects = false;
        config.motion.blockPlaceAnimation = false;
        config.motion.particleMultiplier = 2;
        check(PageDefaults.reset(config, 7), "the world effects page resets");
        check(config.motion.blockBreakEffects && config.motion.blockPlaceAnimation,
                "page reset re-enables world effect layers");
        checkClose(config.motion.particleMultiplier, 1, 1E-6, "page reset restores the particle multiplier");

        config.motion.playerAnimations = false;
        config.motion.playerAttackAnimation = false;
        config.motion.craftingAnimation = false;
        config.motion.sprintStride = false;
        config.motion.airborneMotion = false;
        config.motion.idleSway = false;
        config.motion.playerIntensity = .2;
        check(PageDefaults.reset(config, 8), "the avatar motion page resets");
        check(config.motion.playerAnimations && config.motion.playerAttackAnimation
                && config.motion.craftingAnimation && config.motion.sprintStride
                && config.motion.airborneMotion && config.motion.idleSway,
                "page reset re-enables every avatar layer");
        checkClose(config.motion.playerIntensity, .8, 1E-6, "page reset restores the avatar intensity");

        config.cameraYawOffset = 45;
        config.cameraPitchOffset = -30;
        check(PageDefaults.reset(config, SettingsPages.CAMERA_ANGLES), "the camera angles page resets");
        checkClose(config.cameraYawOffset, 0, 1E-6, "page reset restores the yaw offset");
        checkClose(config.cameraPitchOffset, 0, 1E-6, "page reset restores the pitch offset");

        config.slotOpacity = .3F;
        config.borderOpacity = .1F;
        check(PageDefaults.reset(config, SettingsPages.GLASS_MATERIAL), "the material page resets");
        check(GlassMaterialPresets.matches(config, GlassMaterialPresets.Preset.FROSTED),
                "page reset restores the reference material");

        config.defaultSpatialHandledScreens = false;
        config.fallbackToScreenSpace = false;
        config.settingsKey = "key.keyboard.f10";
        check(PageDefaults.reset(config, SettingsPages.SCREEN_SUPPORT), "the screen support page resets");
        check(config.defaultSpatialHandledScreens && config.fallbackToScreenSpace,
                "page reset restores both screen support switches");
        check(config.settingsKey.equals("key.keyboard.f8"), "page reset restores the settings key");

        config.playerHeightRatio = .30;
        config.playerCenterXRatio = .60;
        config.playerCenterYRatio = .40;
        config.panelCenterRatio = .55;
        config.recipeBookOffset = 0;
        check(PageDefaults.reset(config, SettingsPages.PREVIEW_LAYOUT), "the preview layout page resets");
        checkClose(config.playerHeightRatio, .47, 1E-6, "page reset restores the preview height");
        checkClose(config.playerCenterXRatio, .77, 1E-6, "page reset restores the preview horizontal position");
        checkClose(config.playerCenterYRatio, .55, 1E-6, "page reset restores the preview vertical position");
        checkClose(config.panelCenterRatio, .42, 1E-6, "page reset restores the panel position");
        checkClose(config.recipeBookOffset, 77, 1E-6, "page reset restores the recipe book offset");

        config.slideSpeed = 6000;
        config.bounce = 0;
        config.closeSpeed = 600;
        config.spring = 900;
        config.damping = 5;
        check(PageDefaults.reset(config, SettingsPages.PANEL_FEEL), "the panel feel page resets");
        checkClose(config.slideSpeed, 2600, 1E-6, "page reset restores the opening speed");
        checkClose(config.bounce, .26, 1E-6, "page reset restores the bounce");
        checkClose(config.closeSpeed, 2200, 1E-6, "page reset restores the closing speed");
        checkClose(config.spring, 500, 1E-6, "page reset restores the settle stiffness");
        checkClose(config.damping, 38, 1E-6, "page reset restores the settle damping");

        config.panelOpacity = .5F;
        check(!PageDefaults.reset(config, SettingsPages.COMPATIBILITY),
                "the compatibility page has no reset");
        checkClose(config.panelOpacity, .5, 1E-6, "an unsupported page leaves the config untouched");
        check(!PageDefaults.reset(config, SettingsPages.COUNT), "an unknown page has no reset");
        check(!PageDefaults.reset(null, 0), "a missing config is refused");

        // Resetting a page over a fresh config must be a no-op, so the button can
        // never introduce a value different from what a new install starts with.
        InventoryConfig fresh = new InventoryConfig();
        for (int page = 0; page < SettingsPages.COUNT; page++) {
            if (!PageDefaults.supports(page)) continue;
            PageDefaults.reset(fresh, page);
        }
        checkClose(fresh.panelOpacity, .18, 1E-6, "repeated page resets keep the global transparency");
        checkClose(fresh.guiScale, 1.0, 1E-6, "repeated page resets keep the GUI scale");
        checkClose(fresh.lookSmoothing, 11, 1E-6, "repeated page resets keep the follow speed");
        checkClose(fresh.motion.playerIntensity, .8, 1E-6, "repeated page resets keep the avatar intensity");
        check(GlassMaterialPresets.matches(fresh, GlassMaterialPresets.Preset.FROSTED),
                "repeated page resets keep the reference material");
    }

    private static void testScreenOptIn() {
        InventoryConfig config = new InventoryConfig();
        check(config.additionalScreens.isEmpty(), "no screen is opted in by default");
        check(!config.isScreenOptedIn("com.example.TestScreen"), "an unknown screen is not opted in");
        check(!config.hasExactOptIn("com.example.TestScreen"), "no exact opt-in exists yet");
        check(config.toggleScreenOptIn("com.example.TestScreen"), "toggling opts the screen in");
        check(config.hasExactOptIn("com.example.TestScreen"), "an exact opt-in is recorded");
        check(config.isScreenOptedIn("com.example.TestScreen"), "the screen reports opted in");
        check(!config.toggleScreenOptIn("com.example.TestScreen"), "toggling again opts it out");
        check(!config.isScreenOptedIn("com.example.TestScreen"), "the screen reports not opted in");
        check(!config.toggleScreenOptIn(null) && !config.toggleScreenOptIn("   "),
                "blank names are refused");

        config.additionalScreens.add("com.example.*");
        check(config.isScreenOptedIn("com.example.OtherScreen"), "a wildcard rule opts a family in");
        check(!config.hasExactOptIn("com.example.OtherScreen"), "a wildcard rule is not an exact entry");
        check(config.toggleScreenOptIn("com.example.OtherScreen"),
                "toggling cannot undo a wildcard opt-in");
        check(config.additionalScreens.size() == 1, "the wildcard rule stays untouched");
        check(config.additionalScreens.get(0).equals("com.example.*"), "the rule is unchanged");

        config.additionalScreens.clear();
        config.toggleScreenOptIn("  com.example.Padded  ");
        check(config.hasExactOptIn("com.example.Padded"), "an opt-in name is trimmed");
        config.sanitize();
        check(config.isScreenOptedIn("com.example.Padded"), "a saved opt-in survives sanitizing");
    }

    private static void testSettingsKey() {
        InventoryConfig config = new InventoryConfig();
        check(config.settingsKey.equals("key.keyboard.f8"), "the settings key starts on F8");
        check(config.settingsKeyName().equals("F8"), "the short label names the key");
        config.cycleSettingsKey();
        check(config.settingsKey.equals("key.keyboard.f9"), "cycling moves to F9");
        config.cycleSettingsKey();
        check(config.settingsKey.equals("key.keyboard.f10"), "cycling moves to F10");
        config.cycleSettingsKey();
        check(config.settingsKey.equals("key.keyboard.f6"), "cycling wraps to F6");
        config.cycleSettingsKey();
        check(config.settingsKey.equals("key.keyboard.f7"), "cycling reaches F7");
        config.cycleSettingsKey();
        check(config.settingsKey.equals("key.keyboard.f8"), "cycling returns to F8");
        check(InventoryConfig.SETTINGS_KEYS.size() == 5, "five function keys are offered");

        config.settingsKey = "key.keyboard.q";
        config.sanitize();
        check(config.settingsKey.equals("key.keyboard.f8"), "a gameplay key is refused on load");
        config.settingsKey = " KEY.KEYBOARD.F7 ";
        config.sanitize();
        check(config.settingsKey.equals("key.keyboard.f7"), "the stored name is trimmed and normalized");
        check(config.settingsKeyName().equals("F7"), "the label follows the stored name");
        config.settingsKey = null;
        config.sanitize();
        check(config.settingsKey.equals("key.keyboard.f8"), "a missing key falls back to F8");

        config.settingsKey = "key.keyboard.q";
        config.cycleSettingsKey();
        check(config.settingsKey.equals("key.keyboard.f9"),
                "cycling from an invalid key starts from the F8 default");
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

    private static void testOpacityFade() {
        checkClose(Opacity.fade(0.5, 1), 0.5, 1E-9, "settled panel keeps the authored dim");
        checkClose(Opacity.fade(0.5, 0.5), 0.25, 1E-9, "dim scales with entrance progress");
        checkClose(Opacity.fade(0.5, 0), 0, 1E-9, "dim disappears before the panel appears");
        checkClose(Opacity.fade(2, 0.5), 0.5, 1E-9, "over-bright dim is clamped to one");
        checkClose(Opacity.fade(-1, 0.5), 0, 1E-9, "negative dim is clamped to zero");
        checkClose(Opacity.fade(Double.NaN, 1), 0, 1E-9, "invalid dim strength stays hidden");
        checkClose(Opacity.fade(0.6, Double.NaN), 0, 1E-9, "invalid entrance progress hides the dim");
        checkClose(Opacity.fade(0.6, Double.POSITIVE_INFINITY), 0, 1E-9, "infinite entrance progress hides the dim");
        double previous = -1;
        for (int step = 0; step <= 20; step++) {
            double faded = Opacity.fade(0.6, step / 20.0);
            check(faded >= previous, "dim never darkens then brightens during the fade");
            previous = faded;
        }
        check(Opacity.multiply(0xFF000000, Opacity.fade(1, 0.5)) == 0x80000000, "half-visible dim renders at half alpha");
        check(Opacity.multiply(0xFF000000, Opacity.fade(1, 0)) == 0x00000000, "hidden dim renders nothing");
        check(Opacity.multiply(0xFF000000, Opacity.fade(1, 1)) == 0xFF000000, "full dim renders opaque black");
        check(Opacity.multiply(0x80000000, Double.NaN) == 0x80000000, "invalid opacity keeps the authored alpha");
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
