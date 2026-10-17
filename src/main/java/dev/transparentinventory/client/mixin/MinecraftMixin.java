package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.anchor.SpatialAnchorResolver;
import dev.transparentinventory.client.camera.SpatialCameraController;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    // Both disconnect(...) and clearClientLevel(...) converge here, after the
    // Minecraft.level field has been cleared. Resetting at the old method's HEAD
    // misses normal disconnects and can re-enable the camera during teardown.
    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void spatial$unload(ClientLevel next, boolean reset, CallbackInfo ci) {
        if (next != null) return;
        SpatialCameraController.INSTANCE.reset();
        dev.transparentinventory.client.look.InventoryLookController.INSTANCE.reset();
        SpatialAnchorResolver.clear();
        SpatialScreenManager.INSTANCE.dismissSettings();
        SpatialScreenManager.INSTANCE.synchronize();
    }
}
