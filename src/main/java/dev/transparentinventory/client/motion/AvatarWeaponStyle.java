package dev.transparentinventory.client.motion;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;

/** Maps the server-replicated swing description to a whole-body motion family. */
public final class AvatarWeaponStyle {
    private AvatarWeaponStyle() {
    }

    public static AvatarMotionMath.Style of(SwingAnimationType type, ItemStack stack) {
        if (type == null || type == SwingAnimationType.NONE) return AvatarMotionMath.Style.NONE;
        if (stack == null || stack.isEmpty()) {
            return type == SwingAnimationType.STAB ? AvatarMotionMath.Style.NONE : AvatarMotionMath.Style.UNARMED;
        }
        if (type == SwingAnimationType.STAB
                || stack.is(ItemTags.SPEARS) || stack.is(ItemTags.TRIDENT_ENCHANTABLE)) {
            return AvatarMotionMath.Style.SPEAR;
        }
        if (stack.is(ItemTags.SWORDS)) return AvatarMotionMath.Style.SWORD;
        if (stack.is(ItemTags.AXES)) return AvatarMotionMath.Style.AXE;
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) {
            return AvatarMotionMath.Style.TOOL;
        }
        return AvatarMotionMath.Style.NONE;
    }
}
