package dev.transparentinventory.client.render;

/** Projection of a flat billboard parallel to the camera, in GUI-scaled pixels.
 * Vanilla coordinates remain unchanged. The same immutable snapshot is used for
 * drawing and every input event until the next frame is presented. */
public record SpatialTransform(double originX, double originY, double centerX, double centerY, double scale,
                               double tiltDegrees, double shear, double widthRatio) {
    public SpatialTransform(double originX, double originY, double centerX, double centerY, double scale) {
        this(originX, originY, centerX, centerY, scale, 0, 0, 1);
    }
    public static final SpatialTransform IDENTITY = new SpatialTransform(0, 0, 0, 0, 1);

    public boolean valid() {
        return Double.isFinite(originX) && Double.isFinite(originY)
                && Double.isFinite(centerX) && Double.isFinite(centerY)
                && Double.isFinite(scale) && scale >= 0.1 && scale <= 4
                && Double.isFinite(tiltDegrees) && Math.abs(tiltDegrees) <= 45
                && Double.isFinite(shear) && Math.abs(shear) <= 0.5
                && Double.isFinite(widthRatio) && widthRatio >= 0.5 && widthRatio <= 1.5;
    }

    public double screenX(double x) { return centerX + (x - originX) * scale; }
    public double screenY(double y) { return centerY + (y - originY) * scale; }
    public double localX(double x) { return originX + (x - centerX) / scale; }
    public double localY(double y) { return originY + (y - centerY) / scale; }
    public double localDelta(double delta) { return delta / scale; }

    // R * shearX * scale: use these same coefficients in rendering and input.
    public double a() { return Math.cos(Math.toRadians(tiltDegrees)) * scale * widthRatio; }
    public double b() { return Math.sin(Math.toRadians(tiltDegrees)) * scale * widthRatio; }
    public double c() { return (Math.cos(Math.toRadians(tiltDegrees)) * shear - Math.sin(Math.toRadians(tiltDegrees))) * scale; }
    public double d() { return (Math.sin(Math.toRadians(tiltDegrees)) * shear + Math.cos(Math.toRadians(tiltDegrees))) * scale; }
    public double screenX(double x, double y) { return centerX + a() * (x - originX) + c() * (y - originY); }
    public double screenY(double x, double y) { return centerY + b() * (x - originX) + d() * (y - originY); }
    public double localX(double x, double y) { return originX + localDeltaX(x - centerX, y - centerY); }
    public double localY(double x, double y) { return originY + localDeltaY(x - centerX, y - centerY); }
    public double localDeltaX(double x, double y) { return (d() * x - c() * y) / (scale * scale * widthRatio); }
    public double localDeltaY(double x, double y) { return (-b() * x + a() * y) / (scale * scale * widthRatio); }
    public double halfWidth(double width, double height) { return (Math.abs(a()) * width + Math.abs(c()) * height) / 2; }
    public double halfHeight(double width, double height) { return (Math.abs(b()) * width + Math.abs(d()) * height) / 2; }
}
