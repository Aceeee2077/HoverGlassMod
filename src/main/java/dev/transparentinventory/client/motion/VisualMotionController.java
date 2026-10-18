package dev.transparentinventory.client.motion;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anim.DampedSpring;
import dev.transparentinventory.client.anim.Easing;
import dev.transparentinventory.client.compat.MinecraftAccess;
import dev.transparentinventory.client.effects.BlockFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.joml.Matrix4f;

/** Once-per-camera-frame sampling; never writes entity movement, camera angles or FOV. */
public final class VisualMotionController {
    public static final VisualMotionController INSTANCE = new VisualMotionController();
    private final DampedSpring vertical = new DampedSpring(), impact = new DampedSpring();
    private final Matrix4f rotation = new Matrix4f();
    private ClientLevel level;
    private LocalPlayer player;
    private long lastFrame, pendingAttackAt, lastJump, lastLanding, lastHit;
    private int pendingTarget = -1;
    private boolean grounded;
    private double fallSpeed, phase, sway, x, y, roll, pitch;

    private VisualMotionController() { }

    public static boolean available(Minecraft client) {
        var config = InventoryConfig.get();
        return config.enabled && config.motion.enabled && !config.motion.reduceMotion
                && client.level != null && client.player != null && client.player.isAlive()
                && !client.player.isSpectator() && !client.player.isSleeping() && !client.isPaused()
                && MinecraftAccess.screen(client) == null;
    }

    public static boolean firstPerson(Minecraft client) {
        return available(client) && client.options.getCameraType().isFirstPerson()
                && MinecraftAccess.camera(client).entity() == client.player
                && !client.player.isPassenger() && !client.player.getAbilities().flying && !client.player.isFallFlying();
    }

    public void frame() {
        var client = Minecraft.getInstance();
        BlockFeedback.frame(client);
        if (!firstPerson(client)) { reset(); return; }
        long now = System.nanoTime();
        if (level != client.level || player != client.player || lastFrame == 0 || now - lastFrame > 250_000_000L) {
            reset(); level = client.level; player = client.player; lastFrame = now;
            grounded = player.onGround(); return;
        }
        double dt = Math.max(0, (now - lastFrame) / 1E9);
        lastFrame = now;
        var config = InventoryConfig.get().motion;
        if (!client.options.bobView().get() || config.cameraIntensity <= 0 || client.options.screenEffectScale().get() <= 0) {
            vertical.reset(); impact.reset(); sway = x = y = roll = pitch = 0;
            grounded = player.onGround(); fallSpeed = 0; return;
        }
        boolean ground = player.onGround();
        double velocityY = player.getDeltaMovement().y;
        if (grounded && !ground && velocityY > .04 && config.jumpMotion) { vertical.impulse(.35); lastJump = now; }
        if (!grounded && ground && fallSpeed < -.15 && config.landingMotion) {
            vertical.impulse(-Math.clamp(-fallSpeed * .75, .18, 1.2)); lastLanding = now;
        }
        fallSpeed = ground ? 0 : Math.min(fallSpeed, velocityY);
        grounded = ground;
        vertical.update(dt); impact.update(dt);
        double speed = Math.hypot(player.getDeltaMovement().x, player.getDeltaMovement().z);
        double target = config.sprintSway && player.isSprinting() && ground ? Math.clamp(speed / .28, 0, 1) : 0;
        sway = Easing.approach(sway, target, 12, dt);
        phase = (phase + dt * (9 + speed * 8)) % (Math.PI * 2);
        double strength = config.cameraIntensity * client.options.screenEffectScale().get();
        x = Math.sin(phase) * sway * .012 * strength;
        y = (Math.cos(phase * 2) * sway * .008 + Math.clamp(vertical.position(), -.045, .045)) * strength;
        roll = Math.sin(phase) * sway * .3 * strength;
        pitch = config.hitShake ? impact.position() * 14 * strength * client.options.damageTiltStrength().get() : 0;
    }

    public void apply(PoseStack pose) {
        if (!firstPerson(Minecraft.getInstance())) return;
        pose.translate((float) x, (float) y, 0);
        if (pitch != 0 || roll != 0) pose.mulPose(rotation.rotationXYZ((float) Math.toRadians(pitch), 0, (float) Math.toRadians(roll)));
    }

    public void attackIntent(int entityId) {
        if (!firstPerson(Minecraft.getInstance())) return;
        pendingTarget = entityId; pendingAttackAt = System.nanoTime();
    }

    /** Vanilla server notification only; speculative swings never create impact feedback. */
    public void damage(ClientboundDamageEventPacket packet) {
        var client = Minecraft.getInstance();
        if (!firstPerson(client) || !InventoryConfig.get().motion.hitShake || !client.options.bobView().get()
                || client.options.damageTiltStrength().get() <= 0 || client.options.screenEffectScale().get() <= 0) return;
        long now = System.nanoTime();
        if (packet.entityId() != pendingTarget || now - pendingAttackAt > 1_000_000_000L
                || packet.sourceCauseId() != client.player.getId() || packet.sourceDirectId() != client.player.getId()) return;
        pendingTarget = -1;
        impact.impulse(.28); lastHit = now;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double roll() { return roll; }
    public double pitch() { return pitch; }
    public long lastJump() { return lastJump; }
    public long lastLanding() { return lastLanding; }
    public long lastHit() { return lastHit; }

    public void reset() {
        level = null; player = null; lastFrame = pendingAttackAt = lastJump = lastLanding = lastHit = 0;
        pendingTarget = -1; grounded = false; fallSpeed = phase = sway = x = y = roll = pitch = 0;
        vertical.reset(); impact.reset(); FirstPersonMotion.reset();
    }
}
