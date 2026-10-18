package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.transparentinventory.client.camera.SpatialCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void motion$frame(net.minecraft.client.DeltaTracker tracker, CallbackInfo ci) {
        dev.transparentinventory.client.motion.VisualMotionController.INSTANCE.frame();
    }
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "alignWithEntity", at = @At("HEAD"))
    private void spatial$update(float partialTick, CallbackInfo ci) {
        SpatialCameraController.INSTANCE.update();
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), index = 0)
    private float spatial$distance(float requested) {
        return SpatialCameraController.INSTANCE.distance(requested);
    }

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V"))
    private void spatial$shoulder(Camera camera, float forward, float up, float right, Operation<Void> original) {
        float lateral = SpatialCameraController.INSTANCE.lateralOffset(forward);
        if (lateral == 0 || Minecraft.getInstance().level == null) {
            original.call(camera, forward, up, right);
            return;
        }
        right += lateral;
        Vec3 displacement = new Vec3(new Vector3f(right, up, -forward).rotate(camera.rotation()));
        double length = displacement.length();
        double fraction = 1;
        // Protect the entire diagonal sweep, including the new sideways component.
        for (int i = 0; i < 8 && length > 1E-6; i++) {
            Vec3 start = camera.position().add(((i & 1) * 2 - 1) * 0.1,
                    (((i >> 1) & 1) * 2 - 1) * 0.1, (((i >> 2) & 1) * 2 - 1) * 0.1);
            var hit = Minecraft.getInstance().level.clip(new ClipContext(start, start.add(displacement),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, camera.entity()));
            if (hit.getType() != HitResult.Type.MISS) fraction = Math.min(fraction, Math.max(0, (hit.getLocation().distanceTo(start) - 0.05) / length));
        }
        original.call(camera, (float) (forward * fraction), (float) (up * fraction), (float) (right * fraction));
    }

    @Inject(method = "alignWithEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;detached:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void spatial$rotation(float partialTick, CallbackInfo ci) {
        var controller = SpatialCameraController.INSTANCE;
        setRotation(yRot + controller.yawOffset(), Math.clamp(xRot + controller.pitchOffset(), -89.9F, 89.9F));
    }
}
