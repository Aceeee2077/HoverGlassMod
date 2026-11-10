package dev.transparentinventory.client.render;

public final class Opacity {
    private Opacity() { }
    public static int multiply(int argb, double opacity) {
        double safe = Double.isFinite(opacity) ? Math.clamp(opacity, 0, 1) : 1;
        return (argb & 0x00FFFFFF) | ((int) Math.round((argb >>> 24) * safe) << 24);
    }

    /**
     * Scene alpha modulated by the panel's entrance progress, so a world dim
     * fades in and out with the panel instead of popping.
     *
     * @param alpha      authored dim strength, 0..1
     * @param visibility entrance progress, 0..1; anything non-finite hides the dim
     */
    public static double fade(double alpha, double visibility) {
        double safeAlpha = Double.isFinite(alpha) ? Math.clamp(alpha, 0, 1) : 0;
        double safeVisibility = Double.isFinite(visibility) ? Math.clamp(visibility, 0, 1) : 0;
        return safeAlpha * safeVisibility;
    }
}
