package dev.transparentinventory.client.motion;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * Minecraft 26.3 moved swing animation metadata from separate attack fields to
 * {@code LivingEntity.SwingDescription}; the shared controller only needs the
 * normalized progress, side and weapon family.
 */
public final class AvatarAttackView {
    private AvatarAttackView() {
    }

    public record Attack(float progress, int side, AvatarMotionMath.Style style) {
    }

    public static Attack of(AvatarRenderState state) {
        if (state == null || state.currentSwing == null || !(state.swingAnimation > 0)) return null;
        LivingEntity.SwingDescription swing = state.currentSwing;
        SwingAnimation animation = swing.animation();
        SwingAnimationType type = animation == null ? null : animation.type();
        HumanoidArm arm = arm(state, swing.hand());
        ItemStack stack = arm == HumanoidArm.LEFT ? state.leftHandItemStack : state.rightHandItemStack;
        AvatarMotionMath.Style style = AvatarWeaponStyle.of(type, stack);
        if (style == AvatarMotionMath.Style.NONE) return null;
        // The 26.3 animation wraps from 1 back to 0 on the last interpolation frame;
        // keep that frame at the resting end instead of restarting the swing.
        return new Attack(Math.min(state.swingAnimation, .999F), arm == HumanoidArm.LEFT ? -1 : 1, style);
    }

    private static HumanoidArm arm(AvatarRenderState state, InteractionHand hand) {
        HumanoidArm main = state.mainArm == null ? HumanoidArm.RIGHT : state.mainArm;
        return hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
    }
}
