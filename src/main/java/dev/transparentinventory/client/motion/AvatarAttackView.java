package dev.transparentinventory.client.motion;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * Reads the 26.1/26.2 swing fields. 26.3 replaced them with a SwingDescription
 * and a 0..1 progress value, so that target overrides this class from
 * {@code src/versions/26.3/java}.
 */
public final class AvatarAttackView {
    private AvatarAttackView() {
    }

    public record Attack(float progress, int side, AvatarMotionMath.Style style) {
    }

    public static Attack of(AvatarRenderState state) {
        if (state == null || state.swingAnimationType == null
                || state.swingAnimationType == net.minecraft.world.item.SwingAnimationType.NONE
                || !(state.attackTime > 0)) {
            return null;
        }
        HumanoidArm arm = state.attackArm == HumanoidArm.LEFT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        ItemStack stack = arm == HumanoidArm.LEFT ? state.leftHandItemStack : state.rightHandItemStack;
        AvatarMotionMath.Style style = AvatarWeaponStyle.of(state.swingAnimationType, stack);
        if (style == AvatarMotionMath.Style.NONE) return null;
        return new Attack(state.attackTime, arm == HumanoidArm.LEFT ? -1 : 1, style);
    }
}
