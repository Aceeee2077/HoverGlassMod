package dev.transparentinventory.client.motion;

import dev.transparentinventory.client.InventoryConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.inventory.CraftingMenu;

import java.util.HashMap;
import java.util.Map;

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

    /** Bounded per-entity landing state; a busy server cannot grow this without limit. */
    private static final int MAX_TRACKED = 256;

    private final Map<Integer, Flight> flights = new HashMap<>();
    private Object trackedLevel;

    private PlayerAnimationController() {
    }

    /** Remembers whether an avatar was off the ground and how fast it came down. */
    private static final class Flight {
        boolean wasAirborne;
        float descentSpeed;
        float landedAtAge = Float.NaN;
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
        } else {
            AvatarMotionMath.Pose stride = config.motion.sprintStride && sprinting(client, state)
                    && state.walkAnimationSpeed > .12F
                    ? AvatarMotionMath.stride(state.walkAnimationPos, state.walkAnimationSpeed,
                            (float) config.motion.playerIntensity)
                    : AvatarMotionMath.Pose.NONE;
            AvatarAttackView.Attack attack = config.motion.playerAttackAnimation
                    ? AvatarAttackView.of(state) : null;
            AvatarMotionMath.Pose airborne = config.motion.airborneMotion
                    ? airborne(client, state, (float) config.motion.playerIntensity)
                    : AvatarMotionMath.Pose.NONE;
            AvatarMotionMath.Pose climb = config.motion.airborneMotion
                    ? climbing(client, state, (float) config.motion.playerIntensity)
                    : AvatarMotionMath.Pose.NONE;
            AvatarMotionMath.Pose landing = config.motion.landingMotion
                    ? landing(client, state, (float) config.motion.playerIntensity)
                    : AvatarMotionMath.Pose.NONE;
            AvatarMotionMath.Pose idlePose = config.motion.idleSway
                    ? AvatarMotionMath.idle(state.ageInTicks, state.walkAnimationSpeed,
                            (float) config.motion.playerIntensity)
                    : AvatarMotionMath.Pose.NONE;
            if (attack != null) {
                boolean idle = state.walkAnimationSpeed < .15F;
                pose = AvatarMotionMath.attack(attack.style(), attack.progress(), attack.side(),
                        (float) config.motion.playerIntensity, idle);
                // Keep the footwork continuous during a running swing; the attack
                // pose owns the upper body.
                if (stride != AvatarMotionMath.Pose.NONE) applyLegs(model, stride);
            } else if (climb != AvatarMotionMath.Pose.NONE) {
                // Ladders reuse the walk cycle too; give them a reach instead.
                pose = climb;
            } else if (airborne != AvatarMotionMath.Pose.NONE) {
                // The walk cycle keeps running in the air; this replaces that read.
                pose = airborne;
            } else if (landing != AvatarMotionMath.Pose.NONE) {
                // Grounded again; absorb the drop before handing back to the walk cycle.
                pose = landing;
            } else if (stride != AvatarMotionMath.Pose.NONE) {
                pose = stride;
            } else if (idlePose != AvatarMotionMath.Pose.NONE) {
                // Standing still: breathe instead of freezing in the last pose.
                pose = idlePose;
            }
        }
        if (pose != AvatarMotionMath.Pose.NONE) apply(model, pose);
    }

    /**
     * Sprint and vertical motion are replicated for every player, so nearby avatars
     * move too. The render state itself carries neither flag, hence the entity lookup.
     */
    private static boolean sprinting(Minecraft client, AvatarRenderState state) {
        Avatar avatar = avatar(client, state);
        return avatar != null && avatar.isSprinting();
    }

    /** Diagonal reach while the avatar is on a ladder or vine. */
    private static AvatarMotionMath.Pose climbing(Minecraft client, AvatarRenderState state, float intensity) {
        Avatar avatar = avatar(client, state);
        if (avatar == null || !avatar.onClimbable()) return AvatarMotionMath.Pose.NONE;
        return AvatarMotionMath.climb(state.ageInTicks, (float) avatar.getDeltaMovement().y, intensity);
    }

    /** Airborne pose for a jumping or falling avatar; ground contact and fluids stay vanilla. */
    private static AvatarMotionMath.Pose airborne(Minecraft client, AvatarRenderState state, float intensity) {
        Avatar avatar = avatar(client, state);
        if (avatar == null || avatar.onGround() || avatar.isInWater() || avatar.isInLava()
                || avatar.onClimbable()) {
            return AvatarMotionMath.Pose.NONE;
        }
        return AvatarMotionMath.airborne((float) avatar.getDeltaMovement().y, intensity);
    }

    /**
     * Landing recovery, driven by a small per-entity history because the render
     * state only reports the current tick. Descent speed is sampled while airborne
     * and replayed for a few ticks after the avatar touches down.
     */
    private AvatarMotionMath.Pose landing(Minecraft client, AvatarRenderState state, float intensity) {
        Avatar avatar = avatar(client, state);
        if (avatar == null) return AvatarMotionMath.Pose.NONE;
        if (client.level != trackedLevel) {
            flights.clear();
            trackedLevel = client.level;
        }
        if (flights.size() > MAX_TRACKED) flights.clear();
        Flight flight = flights.computeIfAbsent(state.id, id -> new Flight());
        boolean grounded = avatar.onGround() && !avatar.isInWater() && !avatar.isInLava()
                && !avatar.onClimbable();
        if (!grounded) {
            flight.wasAirborne = true;
            flight.descentSpeed = (float) Math.max(0, -avatar.getDeltaMovement().y);
            return AvatarMotionMath.Pose.NONE;
        }
        if (flight.wasAirborne) {
            flight.wasAirborne = false;
            if (flight.descentSpeed >= AvatarMotionMath.LANDING_MIN_SPEED) {
                flight.landedAtAge = state.ageInTicks;
            }
        }
        if (Float.isNaN(flight.landedAtAge)) return AvatarMotionMath.Pose.NONE;
        float age = state.ageInTicks - flight.landedAtAge;
        if (age < 0 || age >= AvatarMotionMath.LANDING_TICKS) {
            flight.landedAtAge = Float.NaN;
            return AvatarMotionMath.Pose.NONE;
        }
        return AvatarMotionMath.landing(age, flight.descentSpeed, intensity);
    }

    private static Avatar avatar(Minecraft client, AvatarRenderState state) {
        if (client.level == null) return null;
        return client.level.getEntity(state.id) instanceof Avatar avatar ? avatar : null;
    }

    private static void apply(HumanoidModel<?> model, AvatarMotionMath.Pose pose) {
        add(model.body, pose.body());
        add(model.head, pose.head());
        add(model.rightArm, pose.rightArm());
        add(model.leftArm, pose.leftArm());
        add(model.rightLeg, pose.rightLeg());
        add(model.leftLeg, pose.leftLeg());
    }

    private static void applyLegs(HumanoidModel<?> model, AvatarMotionMath.Pose pose) {
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
