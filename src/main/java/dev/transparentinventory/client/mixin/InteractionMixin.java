package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.anchor.SpatialAnchorResolver;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class InteractionMixin {
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void spatial$block(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction()) SpatialAnchorResolver.block(hit.getBlockPos());
        else SpatialAnchorResolver.clear();
    }
    @Inject(method = "interact", at = @At("RETURN"))
    private void spatial$entity(Player player, Entity entity, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction()) SpatialAnchorResolver.entity(entity);
        else SpatialAnchorResolver.clear();
    }
}
