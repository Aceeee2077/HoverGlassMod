package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.motion.PlayerAnimationController;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Append-only avatar motion. Hooking the shared humanoid setup covers both the
 * player model and the separate armor models that the equipment layer renders;
 * otherwise armor arms would keep the vanilla swing while the body moved.
 *
 * <p>The {@link AvatarRenderState} guard limits this to player avatars. Vanilla
 * has already posed walking, crouching, riding and the base arm swing by the
 * time this runs; the controller only adds a small offset on top.
 */
@Mixin(HumanoidModel.class)
public abstract class PlayerAnimationMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL"))
    private void hoverglass$playerMotion(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar) {
            PlayerAnimationController.INSTANCE.apply((HumanoidModel<?>) (Object) this, avatar);
        }
    }
}
