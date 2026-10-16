package dev.transparentinventory.client.anchor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/** A successful interaction is consumed by at most one screen within 1.5 seconds.
 * Inventory explicitly consumes/discards it, preventing an old chest from binding to it. */
public final class SpatialAnchorResolver {
    private static SpatialAnchor pending;
    private static long capturedAt;
    private SpatialAnchorResolver() { }

    public static void block(BlockPos pos) {
        pending = new SpatialAnchor(AnchorType.BLOCK, Minecraft.getInstance().level, pos.immutable(), -1);
        capturedAt = System.nanoTime();
    }
    public static void entity(Entity entity) {
        pending = new SpatialAnchor(AnchorType.ENTITY, Minecraft.getInstance().level, null, entity.getId());
        capturedAt = System.nanoTime();
    }
    public static void clear() { pending = null; }

    public static SpatialAnchor resolve(AnchorType preference) {
        Minecraft client = Minecraft.getInstance();
        SpatialAnchor candidate = pending;
        clear();
        if (preference == AnchorType.PLAYER) return SpatialAnchor.player(client.level);
        if (preference == AnchorType.SCREEN_SPACE) return SpatialAnchor.screenSpace();
        if (candidate == null || client.player == null || System.nanoTime() - capturedAt > 1_500_000_000L)
            return SpatialAnchor.screenSpace();
        if (preference == AnchorType.ENTITY && candidate.type() != AnchorType.ENTITY) return SpatialAnchor.screenSpace();
        var position = candidate.position(client);
        return position != null && client.player.position().distanceToSqr(position) <= 100
                ? candidate : SpatialAnchor.screenSpace();
    }
}
