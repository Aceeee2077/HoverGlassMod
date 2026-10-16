package dev.transparentinventory.client.input;

import dev.transparentinventory.client.render.SpatialTransform;

/** The inverse of the billboard projection, including drag vectors (no translation).
 * A camera-facing plane projects affinely, so an inverse 2D transform is exactly
 * the ray/plane solution without constructing a ray or allocating a vector. */
public final class SpatialInputMapper {
    private SpatialInputMapper() { }
    public static double x(SpatialTransform transform, double x, double y) {
        return usable(transform, x, y) ? transform.localX(x, y) : x;
    }
    public static double y(SpatialTransform transform, double x, double y) {
        return usable(transform, x, y) ? transform.localY(x, y) : y;
    }
    public static double dx(SpatialTransform transform, double x, double y) {
        return usable(transform, x, y) ? transform.localDeltaX(x, y) : x;
    }
    public static double dy(SpatialTransform transform, double x, double y) {
        return usable(transform, x, y) ? transform.localDeltaY(x, y) : y;
    }
    private static boolean usable(SpatialTransform transform, double x, double y) {
        return transform.valid() && Double.isFinite(x) && Double.isFinite(y);
    }
    public static double x(SpatialTransform transform, double raw) {
        return transform.valid() && Double.isFinite(raw) ? transform.localX(raw) : raw;
    }
    public static double y(SpatialTransform transform, double raw) {
        return transform.valid() && Double.isFinite(raw) ? transform.localY(raw) : raw;
    }
    public static double delta(SpatialTransform transform, double raw) {
        return transform.valid() && Double.isFinite(raw) ? transform.localDelta(raw) : raw;
    }
}
