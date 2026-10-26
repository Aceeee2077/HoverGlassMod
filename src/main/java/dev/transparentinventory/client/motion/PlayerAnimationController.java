package dev.transparentinventory.client.motion;

import dev.transparentinventory.client.InventoryConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * Third-person avatar animation layer.
 *
 * <p>This is deliberately visual-only: it adds small rotations after the vanilla
 * player model has been posed, so walking, crouching, riding, item use and the
 * vanilla swing all keep working. Attack state is replicated by the server, so
 * nearby players get the same full-body swing. Workbench motion can only be
 * inferred for the local player, because a client-only mod cannot see another
 * player's open menu.
 */
public final class PlayerAnimationController {
    public static final PlayerAnimationController INSTANCE = new PlayerAnimationController();

    private PlayerAnimationController() {
    }

    public void apply(HumanoidModel<?> model, AvatarRenderState state) {
        InventoryConfig config = InventoryConfig.get();
        if (config == null || !config.enabled || config.motion == null
                || !config.motion.enabled || !config.motion.playerAnimations
                || config.motion.reduceMotion || state == null) {
            return;
        }
        if (state.isSpectator || state.isInvisible || state.deathTime > 0 || state.isFallFlying
                || state.isVisuallySwimming || state.isPassenger || state.isAutoSpinAttack
                || state.isUsingItem || state.swimAmount > .01F || state.hasPose(Pose.SLEEPING)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        AvatarMotionMath.Pose pose = AvatarMotionMath.Pose.NONE;
        boolean crafting = config.motion.craftingAnimation
                && state.id == client.player.getId()
                && client.player.containerMenu instanceof CraftingMenu;
        if (crafting) {
            pose = AvatarMotionMath.crafting(state.ageInTicks, (float) config.motion.playerIntensity);
        } else if (config.motion.playerAttackAnimation) {
            AvatarAttackView.Attack attack = AvatarAttackView.of(state);
            if (attack != null) {
                boolean idle = state.walkAnimationSpeed < .15F;
                pose = AvatarMotionMath.attack(attack.style(), attack.progress(), attack.side(),
                        (float) config.motion.playerIntensity, idle);
            }
        }
        if (pose != AvatarMotionMath.Pose.NONE) apply(model, pose);
    }

    private static void apply(HumanoidModel<?> model, AvatarMotionMath.Pose pose) {
        add(model.body, pose.body());
        add(model.head, pose.head());
        add(model.rightArm, pose.rightArm());
        add(model.leftArm, pose.leftArm());
        add(model.rightLeg, pose.rightLeg());
        add(model.leftLeg, pose.leftLeg());
    }

    private static void add(ModelPart part, AvatarMotionMath.Limb limb) {
        if (part == null || limb == null) return;
        part.xRot += limb.pitch();
        part.yRot += limb.yaw();
        part.zRot += limb.roll();
    }
}
