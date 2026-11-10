package dev.transparentinventory.client.motion;

/**
 * Additive third-person avatar poses, kept free of Minecraft types so the
 * regression suite can exercise timing and symmetry on a plain JVM.
 *
 * <p>The values are offsets added after vanilla {@code PlayerModel.setupAnim}.
 * Vanilla remains responsible for walking, crouching, riding, item use and the
 * base arm swing; this class only adds the weight shift and follow-through that
 * a whole-body weapon swing or a workstation loop needs.
 */
public final class AvatarMotionMath {

    /** Movement families that read differently on screen. */
    public enum Style { NONE, SWORD, AXE, TOOL, SPEAR, UNARMED }

    /** Rotation offsets in radians, applied to one model part. */
    public record Limb(float pitch, float yaw, float roll) {
        public static final Limb NONE = new Limb(0, 0, 0);
    }

    /** Rotations for the six parts this feature touches; everything else is vanilla. */
    public record Pose(Limb body, Limb head, Limb rightArm, Limb leftArm, Limb rightLeg, Limb leftLeg) {
        public static final Pose NONE = new Pose(Limb.NONE, Limb.NONE, Limb.NONE, Limb.NONE, Limb.NONE, Limb.NONE);
    }

    private AvatarMotionMath() {
    }

    /**
     * A whole-body swing layered over vanilla's arm animation.
     *
     * @param progress vanilla {@code attackTime}, clamped to 0..1
     * @param side     +1 for the right arm, -1 for the left arm
     * @param idle     when false the walk cycle already moves the legs, so skip the extra step
     */
    public static Pose attack(Style style, float progress, int side, float intensity, boolean idle) {
        if (style == null || style == Style.NONE) return Pose.NONE;
        double t = clamp(progress, 0, 1);
        double strength = clamp(intensity, 0, 1);
        if (t <= 0 || t >= 1 || strength <= 0) return Pose.NONE;

        double arc = Math.sin(Math.PI * t);
        double follow = Math.sin(Math.PI * 2 * t);
        double yawWeight, leanWeight, rollWeight, stepWeight;
        switch (style) {
            case SWORD -> { yawWeight = .10; leanWeight = .08; rollWeight = .06; stepWeight = .10; }
            case AXE -> { yawWeight = .14; leanWeight = .16; rollWeight = .09; stepWeight = .12; }
            case TOOL -> { yawWeight = .06; leanWeight = .10; rollWeight = .04; stepWeight = .07; }
            case SPEAR -> { yawWeight = .04; leanWeight = .18; rollWeight = .02; stepWeight = .12; }
            case UNARMED -> { yawWeight = .05; leanWeight = .05; rollWeight = .03; stepWeight = .05; }
            default -> { return Pose.NONE; }
        }

        int s = side < 0 ? -1 : 1;
        Limb body = new Limb(
                (float) (leanWeight * arc * strength),
                (float) (s * yawWeight * follow * strength),
                (float) (-s * rollWeight * arc * strength));
        // Counter the torso with the head so the player keeps looking at the target.
        Limb head = new Limb(
                (float) (.04 * arc * strength),
                (float) (-s * .08 * follow * strength),
                0);
        Limb attackArm = new Limb(
                0,
                (float) (s * .08 * arc * strength),
                (float) (-s * .14 * arc * strength));
        Limb offArm = new Limb(
                (float) (-.12 * arc * strength),
                (float) (-s * .05 * arc * strength),
                (float) (s * .06 * arc * strength));
        double step = idle ? stepWeight * arc * strength : 0;
        Limb rightStep = new Limb((float) (s * step), 0, 0);
        Limb leftStep = new Limb((float) (-s * step), 0, 0);

        return s > 0
                ? new Pose(body, head, attackArm, offArm, rightStep, leftStep)
                : new Pose(body, head, offArm, attackArm, rightStep, leftStep);
    }

    /**
     * Extra stride on top of vanilla's walk cycle while the avatar is sprinting.
     *
     * <p>Vanilla already swings the legs and arms from {@code walkAnimationPos}.
     * This layer follows the same phase so the added motion reinforces the
     * existing step instead of fighting it: legs open wider, arms pump with a
     * slight elbow-out roll, and the torso leans into the run.
     *
     * @param walkPos    vanilla {@code walkAnimationPos}
     * @param walkSpeed  vanilla {@code walkAnimationSpeed}; 0 stops the layer
     * @param intensity  0..1 user strength
     */
    public static Pose stride(float walkPos, float walkSpeed, float intensity) {
        if (!Float.isFinite(walkPos) || !Float.isFinite(walkSpeed)) return Pose.NONE;
        double strength = clamp(intensity, 0, 1);
        double amplitude = clamp(walkSpeed, 0, 1);
        if (strength <= 0 || amplitude <= .05) return Pose.NONE;

        double phase = walkPos * .6662;
        double swing = Math.cos(phase);
        double sway = Math.sin(phase);
        double scale = amplitude * strength;

        Limb body = new Limb(
                (float) (.08 * scale),
                (float) (.04 * sway * scale),
                (float) (-.025 * sway * scale));
        // Keep the gaze level against the forward lean.
        Limb head = new Limb((float) (-.04 * scale), (float) (-.02 * sway * scale), 0);
        // Same phase as vanilla's arm swing, so the pump is amplified rather than cancelled.
        Limb rightArm = new Limb((float) (-.28 * swing * scale), 0, (float) (.03 * scale));
        Limb leftArm = new Limb((float) (.28 * swing * scale), 0, (float) (-.03 * scale));
        Limb rightLeg = new Limb((float) (.45 * swing * scale), 0, 0);
        Limb leftLeg = new Limb((float) (-.45 * swing * scale), 0, 0);
        return new Pose(body, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    /**
     * Airborne pose: knees tuck while rising, arms spread for balance while falling.
     *
     * <p>Vanilla keeps the walking cycle running while a player is in the air,
     * which reads as running on nothing. This layer replaces that read with a
     * small, symmetric jump/fall silhouette. It is intentionally additive and
     * small so crouching, riding, swimming and item use remain untouched.
     *
     * @param verticalSpeed vertical movement in blocks per tick (+ up, - down)
     * @param intensity     0..1 user strength
     */
    public static Pose airborne(float verticalSpeed, float intensity) {
        if (!Float.isFinite(verticalSpeed)) return Pose.NONE;
        double strength = clamp(intensity, 0, 1);
        if (strength <= 0) return Pose.NONE;
        double lift = clamp(verticalSpeed / .6, -1, 1);
        // Slow step-downs and tiny bumps stay vanilla; only a real jump or fall reads.
        if (Math.abs(lift) < .13) return Pose.NONE;
        double scale = Math.abs(lift) * strength;

        if (lift > 0) {
            // Rising: knees come forward and up, arms lift slightly ahead, torso settles back.
            Limb body = new Limb((float) (-.06 * scale), 0, 0);
            Limb head = new Limb((float) (.04 * scale), 0, 0);
            Limb rightArm = new Limb((float) (-.24 * scale), 0, (float) (-.10 * scale));
            Limb leftArm = new Limb((float) (-.24 * scale), 0, (float) (.10 * scale));
            Limb rightLeg = new Limb((float) (.20 * scale), 0, 0);
            Limb leftLeg = new Limb((float) (.20 * scale), 0, 0);
            return new Pose(body, head, rightArm, leftArm, rightLeg, leftLeg);
        }

        // Falling: arms out to the sides, gaze down, legs trail slightly behind.
        Limb body = new Limb((float) (.05 * scale), 0, 0);
        Limb head = new Limb((float) (-.03 * scale), 0, 0);
        Limb rightArm = new Limb((float) (-.10 * scale), 0, (float) (-.32 * scale));
        Limb leftArm = new Limb((float) (-.10 * scale), 0, (float) (.32 * scale));
        Limb rightLeg = new Limb((float) (-.14 * scale), 0, 0);
        Limb leftLeg = new Limb((float) (-.14 * scale), 0, 0);
        return new Pose(body, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    /**
     * Idle breathing and weight shift for a stationary avatar.
     *
     * <p>Vanilla leaves a standing player completely rigid between swings. This
     * tiny additive layer breathes once every few seconds: the torso rises with
     * the chest, the arms drift a hair outward, and the weight shifts slowly side
     * to side. Amplitudes stay under three hundredths of a radian so it reads as
     * breathing rather than fidgeting, and any real movement hands back to the
     * walk cycle.
     *
     * @param ageTicks  render age in ticks, used as the slow clock
     * @param walkSpeed vanilla {@code walkAnimationSpeed}; anything above a shuffle stops it
     * @param intensity 0..1 user strength
     */
    public static Pose idle(float ageTicks, float walkSpeed, float intensity) {
        if (!Float.isFinite(ageTicks) || !Float.isFinite(walkSpeed)) return Pose.NONE;
        double strength = clamp(intensity, 0, 1);
        if (strength <= 0 || walkSpeed > .06F || ageTicks < 0) return Pose.NONE;

        double phase = ageTicks * .055;
        double breath = Math.sin(phase * 2);
        double shift = Math.sin(phase * .5);
        double scale = strength;

        Limb body = new Limb((float) (.022 * breath * scale), (float) (.010 * shift * scale),
                (float) (-.008 * shift * scale));
        Limb head = new Limb((float) (-.012 * breath * scale), (float) (-.006 * shift * scale), 0);
        Limb rightArm = new Limb(0, 0, (float) (-.016 * breath * scale));
        Limb leftArm = new Limb(0, 0, (float) (.016 * breath * scale));
        return new Pose(body, head, rightArm, leftArm, Limb.NONE, Limb.NONE);
    }

    /** How long the landing recovery lasts, in ticks. */
    public static final float LANDING_TICKS = 6F;

    /** Landing speed below which the drop reads as a step-down, not a fall. */
    public static final float LANDING_MIN_SPEED = .32F;

    /**
     * Short recovery after touching down: knees absorb, arms swing out for balance.
     *
     * <p>Vanilla snaps straight back to the walk cycle on the landing tick, which
     * makes a real fall look weightless. This layer eases a crouch in and out over
     * {@link #LANDING_TICKS} ticks, scaled by the downward speed measured just
     * before landing and by the avatar strength setting. Small step-downs stay
     * vanilla.
     *
     * @param ageTicks    ticks elapsed since the landing tick
     * @param impactSpeed downward speed at the landing tick, in blocks per tick (positive)
     * @param intensity   0..1 user strength
     */
    public static Pose landing(float ageTicks, float impactSpeed, float intensity) {
        if (!Float.isFinite(ageTicks) || !Float.isFinite(impactSpeed)) return Pose.NONE;
        double strength = clamp(intensity, 0, 1);
        double impact = clamp(impactSpeed, 0, 1);
        if (strength <= 0 || impact < LANDING_MIN_SPEED || ageTicks < 0 || ageTicks >= LANDING_TICKS) {
            return Pose.NONE;
        }
        double t = clamp(ageTicks / LANDING_TICKS, 0, 1);
        double falloff = 1 - t * t * (3 - 2 * t);
        double scale = clamp((impact - LANDING_MIN_SPEED) / .58, 0, 1) * strength * falloff;
        if (scale <= .001) return Pose.NONE;

        // Knees drive forward, the torso folds a little, the head stays level.
        Limb body = new Limb((float) (.12 * scale), 0, 0);
        Limb head = new Limb((float) (-.06 * scale), 0, 0);
        // Arms swing slightly back and out to absorb the drop.
        Limb rightArm = new Limb((float) (.16 * scale), 0, (float) (-.14 * scale));
        Limb leftArm = new Limb((float) (.16 * scale), 0, (float) (.14 * scale));
        Limb rightLeg = new Limb((float) (.28 * scale), 0, 0);
        Limb leftLeg = new Limb((float) (.28 * scale), 0, 0);
        return new Pose(body, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    /**
     * Alternating reach while climbing a ladder or vine.
     *
     * <p>Vanilla keeps the ordinary walking cycle on ladders, so the avatar looks
     * like it is jogging upward. This layer adds a diagonal reach: one arm and the
     * opposite leg advance while the other pair pulls back, scaled by how fast the
     * avatar is actually moving vertically.
     *
     * @param ageTicks       render age in ticks, used as the loop clock
     * @param verticalSpeed  vertical movement in blocks per tick (+ up, - down)
     * @param intensity      0..1 user strength
     */
    public static Pose climb(float ageTicks, float verticalSpeed, float intensity) {
        if (!Float.isFinite(ageTicks) || !Float.isFinite(verticalSpeed)) return Pose.NONE;
        double strength = clamp(intensity, 0, 1);
        double motion = clamp(Math.abs(verticalSpeed) / .2, 0, 1);
        if (strength <= 0 || motion <= .05) return Pose.NONE;

        double phase = ageTicks * .35;
        double reach = Math.sin(phase) * motion * strength;
        double direction = verticalSpeed < 0 ? -1 : 1;
        double pull = .30 * reach * direction;

        Limb body = new Limb(0, (float) (.03 * reach), (float) (-.02 * reach));
        Limb head = new Limb((float) (-.05 * motion * strength), 0, 0);
        // Diagonal gait: the right arm reaches with the left leg and vice versa.
        Limb rightArm = new Limb((float) (-.30 * motion * strength - pull), 0, (float) (-.06 * reach));
        Limb leftArm = new Limb((float) (-.30 * motion * strength + pull), 0, (float) (.06 * reach));
        Limb rightLeg = new Limb((float) (.18 * motion * strength + pull), 0, 0);
        Limb leftLeg = new Limb((float) (.18 * motion * strength - pull), 0, 0);
        return new Pose(body, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    /**
     * A reversible "hands at the bench" loop. The player is stationary while a
     * crafting menu is open, so this only supplies the upper-body work.
     */
    public static Pose crafting(float ageTicks, float intensity) {
        double strength = clamp(intensity, 0, 1);
        if (!Float.isFinite(ageTicks) || ageTicks < 0 || strength <= 0) return Pose.NONE;

        double phase = ageTicks * .18;
        double wave = Math.sin(phase);
        double tap = Math.sin(phase * 2);
        Limb body = new Limb(
                (float) ((.10 + .02 * tap) * strength),
                0,
                (float) (.015 * wave * strength));
        Limb head = new Limb(
                (float) ((.22 + .02 * tap) * strength),
                (float) (.04 * wave * strength),
                0);
        Limb rightArm = new Limb(
                (float) ((-.80 + .18 * wave) * strength),
                (float) (.04 * strength),
                (float) (.10 * strength));
        Limb leftArm = new Limb(
                (float) ((-.80 - .18 * wave) * strength),
                (float) (-.04 * strength),
                (float) (-.10 * strength));
        return new Pose(body, head, rightArm, leftArm, Limb.NONE, Limb.NONE);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
