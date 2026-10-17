package dev.transparentinventory.client.look;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.compat.MinecraftAccess;
import dev.transparentinventory.client.mixin.ContainerScreenAccessor;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;

/** Changes only the extracted model pose. Never writes player/camera angles or sends packets. */
public final class InventoryLookController {
    public static final InventoryLookController INSTANCE = new InventoryLookController();
    private final LookTracker look = new LookTracker();
    private Screen session;
    private ClientLevel level;
    private long lastFrame;

    private InventoryLookController() { }

    public void apply(Avatar entity, AvatarRenderState pose) {
        Minecraft client = Minecraft.getInstance();
        if (entity != client.player) return;
        var manager = SpatialScreenManager.INSTANCE;
        var config = InventoryConfig.get();
        Screen screen = MinecraftAccess.screen(client);
        if (!manager.active() || manager.anchor().type() != dev.transparentinventory.client.anchor.AnchorType.PLAYER
                || !config.playerLookAtInventory || client.options.getCameraType().isFirstPerson()
                || entity.isSleeping() || !entity.isAlive() || !(screen instanceof ContainerScreenAccessor bounds)) {
            reset();
            return;
        }
        long now = System.nanoTime();
        if (screen != session || client.level != level) {
            session = screen;
            level = client.level;
            look.reset();
            look.update(0, 0, 0, 1.0 / 60, config.lookSmoothing);
            lastFrame = now;
        }
        double dt = Math.clamp((now - lastFrame) / 1_000_000_000.0, 0.000001, 0.1);
        lastFrame = now;
        if (manager.settings() == null) {
            var window = client.getWindow();
            double x = MouseHandler.getScaledXPos(window, client.mouseHandler.xpos());
            double y = MouseHandler.getScaledYPos(window, client.mouseHandler.ypos());
            var transform = manager.transform();
            var target = InventoryLookAngles.at(transform.localX(x, y), transform.localY(x, y),
                    bounds.spatial$left(), bounds.spatial$top(), bounds.spatial$width(), bounds.spatial$height(),
                    config.maxHeadYaw, config.maxHeadPitch);
            look.update(target.yaw(), target.pitch(), 0, dt, config.lookSmoothing);
        }
        double blend = manager.visibility();
        pose.yRot += (look.headYaw() - pose.yRot) * (float) blend;
        pose.xRot += (look.headPitch() - pose.xRot) * (float) blend;
    }

    public void reset() {
        session = null;
        level = null;
        lastFrame = 0;
        look.reset();
    }
}
