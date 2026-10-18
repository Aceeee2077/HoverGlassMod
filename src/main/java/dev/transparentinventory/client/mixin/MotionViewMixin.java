package dev.transparentinventory.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.transparentinventory.client.motion.VisualMotionController;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MotionViewMixin {
    // Vanilla owns both world/hand bobbing scopes. One frame sample is reused by both.
    @Inject(method = "bobView", at = @At("TAIL"))
    private void motion$view(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
        VisualMotionController.INSTANCE.apply(pose);
    }
}
