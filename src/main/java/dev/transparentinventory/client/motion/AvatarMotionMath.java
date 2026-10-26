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
