package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.effects.BlockFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockPlaceFeedbackMixin {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void motion$placed(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        // BlockItem also runs on an integrated server: the level identity guard excludes that path.
        var client = Minecraft.getInstance();
        if (cir.getReturnValue() && context.getLevel() == client.level && context.getPlayer() == client.player) {
            BlockFeedback.burst(context.getClickedPos(), state, true);
        }
    }
}
