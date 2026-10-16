package dev.transparentinventory.client.anchor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** World identity is part of an anchor: entity IDs/positions cannot leak between worlds. */
public record SpatialAnchor(AnchorType type, ClientLevel level, BlockPos block, int entityId) {
    public static SpatialAnchor player(ClientLevel level) { return new SpatialAnchor(AnchorType.PLAYER, level, null, -1); }
    public static SpatialAnchor screenSpace() { return new SpatialAnchor(AnchorType.SCREEN_SPACE, null, null, -1); }

    public Vec3 position(Minecraft client) {
        if (level == null || client.level != level || client.player == null) return null;
        if (type == AnchorType.BLOCK) {
            return level.getBlockState(block).isAir() ? null : Vec3.atCenterOf(block).add(0, 0.8, 0);
        }
        if (type == AnchorType.ENTITY) {
            Entity entity = level.getEntity(entityId);
            return entity == null || entity.isRemoved() ? null : entity.position().add(0, entity.getBbHeight() + 0.35, 0);
        }
        return null; // PLAYER uses player-relative framing rather than the eye position.
    }
}
