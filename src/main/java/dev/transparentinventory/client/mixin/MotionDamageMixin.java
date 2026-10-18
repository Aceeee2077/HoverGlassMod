package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.motion.VisualMotionController;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MotionDamageMixin {
    // TAIL is after vanilla's thread handoff. Observe the notification without replacing its handling.
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void motion$confirmedHit(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        VisualMotionController.INSTANCE.damage(packet);
    }
}
