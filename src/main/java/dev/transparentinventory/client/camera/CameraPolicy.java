package dev.transparentinventory.client.camera;

import dev.transparentinventory.client.InventoryConfig;

/**
 * Whether the camera may leave the vanilla view for the current screen.
 *
 * <p>Containers and the player inventory share the spatial camera, but players often
 * want chests to stay in first person while keeping the glass panel. This policy
 * separates the two without touching the render path. Plain Java for the regression
 * suite; the controller supplies the anchor kind.
 */
public final class CameraPolicy {

    private CameraPolicy() {
    }

    /**
     * @param playerAnchored true for the player-preview inventory, false for chests,
     *                       furnaces, merchants and other world-anchored containers
     */
    public static boolean enabled(InventoryConfig config, boolean playerAnchored) {
        if (config == null || !config.cameraEnabled) return false;
        return playerAnchored || config.containerCameraEnabled;
    }
}
