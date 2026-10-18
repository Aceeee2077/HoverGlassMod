package dev.transparentinventory.client.effects;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.motion.VisualMotionController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded material-particle proxy, never edits chunk geometry or collision. */
public final class BlockFeedback {
    private static final Particle[] OWNED = new Particle[48];
    private static int cursor;
    private static boolean ownsParticles;
    private static ClientLevel level;
    private static long lastBurst, emitted;
    private BlockFeedback() { }

    public static void burst(BlockPos pos, BlockState state, boolean placement) {
        var client = Minecraft.getInstance();
        var config = InventoryConfig.get().motion;
        if (!VisualMotionController.available(client) || state.isAir() || config.particleMultiplier <= 0
                || (placement ? !config.blockPlaceAnimation : !config.blockBreakEffects)
                || client.player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 100) return;
        var status = client.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        long now = System.nanoTime();
        if (now - lastBurst < 40_000_000L) return;
        lastBurst = now;
        int count = Math.min(16, (int) Math.round((placement ? 8 : 10) * config.particleMultiplier * (status == ParticleStatus.DECREASED ? .5 : 1)));
        var option = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            double radius = placement ? .53 : .28;
            var particle = client.particleEngine.createParticle(option,
                    pos.getX() + .5 + dx * radius, pos.getY() + (placement ? .18 : .5), pos.getZ() + .5 + dz * radius,
                    dx * .07, placement ? .025 : .10 + (i % 3) * .02, dz * .07);
            if (particle == null) continue;
            particle.setParticleSpeed(dx * .07, placement ? .025 : .10 + (i % 3) * .02, dz * .07);
            particle.scale(placement ? .55F : .85F);
            particle.setLifetime(placement ? 4 : 10 + i % 3);
            if (OWNED[cursor] != null) OWNED[cursor].remove();
            OWNED[cursor] = particle; cursor = (cursor + 1) % OWNED.length; emitted++; ownsParticles = true; level = client.level;
        }
    }
    public static long emitted() { return emitted; }
    public static int activeCount() { int count = 0; for (var particle : OWNED) if (particle != null && particle.isAlive()) count++; return count; }
    public static void frame(Minecraft client) {
        if (ownsParticles && (level != client.level || !VisualMotionController.available(client))) clear();
    }
    public static void clear() {
        if (!ownsParticles) return;
        for (int i = 0; i < OWNED.length; i++) { if (OWNED[i] != null) OWNED[i].remove(); OWNED[i] = null; }
        cursor = 0; lastBurst = 0; ownsParticles = false; level = null;
    }
}
