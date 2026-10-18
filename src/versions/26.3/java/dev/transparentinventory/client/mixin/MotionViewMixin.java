package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.motion.FirstPersonMotion;
import dev.transparentinventory.client.motion.VisualMotionController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MotionViewMixin {
    @Inject(method = "bobView", at = @At("TAIL"))
    private void motion$view(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
        VisualMotionController.INSTANCE.apply(pose);
    }

    @WrapOperation(method = "renderItemInHand", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"))
    private void motion$preparedHands(FirstPersonHandsAndItemsRenderer renderer, float tick, PoseStack pose,
                                      SubmitNodeCollector collector, PlayerRenderState player,
                                      FirstPersonHandsAndItemsRenderState hands, Operation<Void> original) {
        var client = Minecraft.getInstance();
        var config = InventoryConfig.get().motion;
        if (FirstPersonMotion.active(client, hands.mainHandItem) && FirstPersonMotion.active(client, hands.offHandItem)
                && config.itemSwap && config.firstPersonIntensity > 0) {
            prepare(hands, InteractionHand.MAIN_HAND);
            prepare(hands, InteractionHand.OFF_HAND);
        } else FirstPersonMotion.reset();
        original.call(renderer, tick, pose, collector, player, hands);
    }

    @org.spongepowered.asm.mixin.Unique
    private static void prepare(FirstPersonHandsAndItemsRenderState hands, InteractionHand hand) {
        var client = Minecraft.getInstance();
        var state = FirstPersonMotion.hand(hand);
        boolean main = hand == InteractionHand.MAIN_HAND;
        state.update(client.player.getItemInHand(hand), main ? client.player.getInventory().getSelectedSlot() : -1, System.nanoTime());
        var visual = state.rendered();
        var vanilla = main ? hands.mainHandItem : hands.offHandItem;
        if (!ItemStack.isSameItemSameComponents(visual, vanilla)) {
            var model = main ? hands.mainHandRenderState : hands.offHandRenderState;
            var arm = main ? client.player.getMainArm() : client.player.getMainArm().getOpposite();
            var context = arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            model.clear();
            client.getItemModelResolver().updateForTopItem(model, visual, context, client.level, client.player, client.player.getId() + context.ordinal());
        }
        float height = (float) (1 - state.lower() * (1 + .15 * InventoryConfig.get().motion.firstPersonIntensity));
        if (main) { hands.mainHandItem = visual; hands.mainHandHeight = hands.oldMainHandHeight = height; }
        else { hands.offHandItem = visual; hands.offHandHeight = hands.oldOffHandHeight = height; }
    }
}
