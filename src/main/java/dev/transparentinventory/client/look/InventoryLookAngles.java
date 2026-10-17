package dev.transparentinventory.client.look;

/** Cursor targets in the panel's own coordinates, independent of GUI scale and tilt. */
public record InventoryLookAngles(float yaw, float pitch) {
    public static InventoryLookAngles at(double x, double y, double left, double top, double width, double height,
                                         float maxYaw, float maxPitch) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(left) || !Double.isFinite(top)
                || !Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0) {
            return new InventoryLookAngles(0, 0);
        }
        double nx = Math.clamp((x - left) / width * 2 - 1, -1, 1);
        double ny = Math.clamp((y - top) / height * 2 - 1, -1, 1);
        // The panel sits left of the player, so the resting gaze favours that side.
        return new InventoryLookAngles((float) ((-0.35 + 0.65 * nx) * maxYaw), (float) (ny * maxPitch));
    }
}
