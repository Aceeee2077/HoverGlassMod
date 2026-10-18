package dev.transparentinventory.client.motion;

import dev.transparentinventory.client.effects.BlockFeedback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/** Registered only for spatial targets; legacy jars keep their original sole entrypoint. */
public final class MotionClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            var client = Minecraft.getInstance();
            if (level == client.level && player == client.player) BlockFeedback.burst(pos, state, false);
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            var client = Minecraft.getInstance();
            if (level == client.level && player == client.player) VisualMotionController.INSTANCE.attackIntent(entity.getId());
            return InteractionResult.PASS;
        });
    }
}
