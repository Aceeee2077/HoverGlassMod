package dev.transparentinventory.client.render;

public final class Opacity {
    private Opacity() { }
    public static int multiply(int argb, double opacity) {
        double safe = Double.isFinite(opacity) ? Math.clamp(opacity, 0, 1) : 1;
        return (argb & 0x00FFFFFF) | ((int) Math.round((argb >>> 24) * safe) << 24);
    }
}
