package dev.transparentinventory.client.anim;

/** Small shared curves; no engine state or allocations. */
public final class Easing {
    private Easing() { }
    public static double linear(double t) { return Math.max(0, Math.min(1, t)); }
    public static double smoothstep(double t) { t = linear(t); return t * t * (3 - 2 * t); }
    public static double outCubic(double t) { double u = 1 - linear(t); return 1 - u * u * u; }
    public static double outQuart(double t) { double u = 1 - linear(t); return 1 - u * u * u * u; }
    public static double inOutCubic(double t) { t = linear(t); return t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
    public static double outBack(double t) { t = linear(t); if (t == 0 || t == 1) return t; double u = t - 1; return 1 + 2.70158 * u * u * u + 1.70158 * u * u; }
    public static double approach(double current, double target, double speed, double dt) {
        return current + (target - current) * (1 - Math.exp(-Math.max(0, speed) * Math.max(0, dt)));
    }
}
