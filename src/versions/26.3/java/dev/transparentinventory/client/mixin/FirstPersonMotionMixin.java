package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.motion.FirstPersonMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** 26.3 models are prepared before submission; MotionViewMixin updates the matching model snapshot. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonMotionMixin {
    @WrapMethod(method = "submitArmWithItem")
    private void motion$hand(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands,
                             float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
                             float equip, PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original) {
        if (!FirstPersonMotion.active(Minecraft.getInstance(), stack) || InventoryConfig.get().motion.firstPersonIntensity <= 0) {
            original.call(player, hands, partialTick, pitch, hand, swing, stack, equip, pose, collector, light);
            return;
        }
        pose.pushPose();
        try {
            FirstPersonMotion.attack(pose, stack, hand, swing);
            original.call(player, hands, partialTick, pitch, hand, swing, stack, equip, pose, collector, light);
        } finally { pose.popPose(); }
    }
}
