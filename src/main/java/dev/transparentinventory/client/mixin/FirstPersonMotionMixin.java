package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.motion.FirstPersonMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonMotionMixin {
    /** Render-only arguments; do not intercept tick(), inventory or vanilla use/hand selection. */
    @WrapMethod(method = "submitArmWithItem")
    private void motion$hand(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                             float swing, ItemStack vanillaItem, float equip, PoseStack pose, SubmitNodeCollector collector,
                             int light, Operation<Void> original) {
        var client = Minecraft.getInstance();
        var state = FirstPersonMotion.hand(hand);
        var config = InventoryConfig.get().motion;
        if (player != client.player || !FirstPersonMotion.active(client, vanillaItem) || config.firstPersonIntensity <= 0) {
            state.reset();
            original.call(player, partialTick, pitch, hand, swing, vanillaItem, equip, pose, collector, light);
            return;
        }
        ItemStack rendered = vanillaItem;
        if (config.itemSwap) {
            state.update(player.getItemInHand(hand), hand == InteractionHand.MAIN_HAND ? client.player.getInventory().getSelectedSlot() : -1, System.nanoTime());
            rendered = state.rendered();
            equip = (float) (state.lower() * (1 + .15 * config.firstPersonIntensity));
        } else state.reset();
        pose.pushPose();
        try {
            FirstPersonMotion.attack(pose, rendered, hand, swing);
            original.call(player, partialTick, pitch, hand, swing, rendered, equip, pose, collector, light);
        } finally { pose.popPose(); }
    }
}
