package dev.transparentinventory.client.render;

/** Left entrance with a restrained overshoot and a persistent glass-panel slant. */
public final class InventoryEntrance {
    private InventoryEntrance() { }

    public static SpatialTransform transform(double width, double height, double originX, double originY,
                                             double panelWidth, double panelHeight, double requestedScale,
                                             double motion, double visibility, double tilt, double impact) {
        var unit = new SpatialTransform(originX, originY, 0, 0, 1, tilt, 0.05, 0.94);
        double scale = Math.clamp(Math.min(requestedScale,
                Math.min((width * 0.64 - 12) / (unit.halfWidth(panelWidth, panelHeight) * 2),
                        (height - 16) / (unit.halfHeight(panelWidth, panelHeight) * 2))), 0.1, 1.5);
        scale *= 1 + 0.06 * impact;
        double halfWidth = unit.halfWidth(panelWidth, panelHeight) * scale;
        double targetX = Math.max(halfWidth + 6, width * 0.34);
        double startX = -halfWidth - 16;
        return new SpatialTransform(originX, originY, startX + (targetX - startX) * motion,
                height * 0.5 + (1 - visibility) * 10, Math.max(0.1, scale * (0.94 + 0.06 * visibility)),
                -11 + (tilt + 11) * motion, 0.05, 0.94);
    }
}
