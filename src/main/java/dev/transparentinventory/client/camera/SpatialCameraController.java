package dev.transparentinventory.client.camera;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anchor.SpatialAnchorResolver;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Changes only the camera, never player rotation. CameraMixin protects the rear
 * distance and the full diagonal shoulder displacement with obstruction rays. */
public final class SpatialCameraController {
    public static final SpatialCameraController INSTANCE = new SpatialCameraController();
    private final CameraTransition transition = new CameraTransition();
    private CameraType original;
    private CameraType applied;
    private double blend;
    private boolean cinematic;
    private SpatialCameraController() { }

    public void update() {
        Minecraft client = Minecraft.getInstance();
        SpatialScreenManager manager = SpatialScreenManager.INSTANCE;
        manager.synchronize();
        if (client.level == null || client.player == null) { reset(); return; }
        InventoryConfig config = InventoryConfig.get();
        boolean open = manager.active() && !manager.closing() && config.cameraEnabled && !client.player.isSleeping();
        if (open && original == null) {
            original = client.options.getCameraType();
            cinematic = original.isFirstPerson() && manager.cinematicInventory() && config.thirdPersonOnOpen;
            applied = original;
            if (config.thirdPersonOnOpen && original.isFirstPerson()) {
                applied = CameraType.THIRD_PERSON_BACK;
                client.options.setCameraType(applied);
            }
        }
        if (original != null && client.options.getCameraType() != applied) {
            // Respect an explicit perspective change made during the GUI session.
            original = applied = client.options.getCameraType();
            cinematic = false;
        }
        blend = transition.update(open, System.nanoTime(), config.cameraTransitionDurationMs);
        if (!open && blend <= 0) reset();
    }

    public float distance(float vanillaDistance) {
        if (original == null) return vanillaDistance;
        double baseline = original.isFirstPerson() ? 0 : vanillaDistance;
        return (float) (baseline + (InventoryConfig.get().cameraDistance - baseline) * blend);
    }
    public float yawOffset() { return (float) (InventoryConfig.get().cameraYawOffset * blend); }
    public float pitchOffset() { return (float) (InventoryConfig.get().cameraPitchOffset * blend); }
    /** Move the camera left to reveal the real player on the right, with a delayed sweep. */
    public float lateralOffset() {
        return cinematic && InventoryConfig.get().cinematicInventory
                ? (float) (-InventoryConfig.get().cameraDistance * 0.62 * blend * blend) : 0;
    }
    public void reset() {
        if (original != null) Minecraft.getInstance().options.setCameraType(original);
        original = applied = null;
        blend = 0;
        cinematic = false;
        transition.reset();
        if (Minecraft.getInstance().level == null) SpatialAnchorResolver.clear();
    }
}
