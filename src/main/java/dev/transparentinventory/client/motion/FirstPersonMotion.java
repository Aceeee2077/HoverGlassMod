package dev.transparentinventory.client.motion;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anim.Easing;
import dev.transparentinventory.client.anim.ItemSwapTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Visual copies only, independent for each hand; vanilla use/map coordination remains authoritative. */
public final class FirstPersonMotion {
    private static final Hand MAIN = new Hand(), OFF = new Hand();
    private static final Matrix4f ROTATION = new Matrix4f();
    private static double lastAttack;
    private FirstPersonMotion() { }

    public static boolean active(Minecraft client, ItemStack vanilla) {
        return VisualMotionController.firstPerson(client) && !client.player.isUsingItem() && !client.player.isScoping()
                && !vanilla.has(DataComponents.MAP_ID) && !client.player.getMainHandItem().has(DataComponents.MAP_ID)
                && !client.player.getOffhandItem().has(DataComponents.MAP_ID);
    }

    public static Hand hand(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? MAIN : OFF; }

    public static void attack(PoseStack pose, ItemStack stack, InteractionHand hand, float progress) {
        if (hand == InteractionHand.MAIN_HAND) lastAttack = 0;
        var config = InventoryConfig.get().motion;
        if (!config.attackAnimation || progress <= 0 || progress >= 1 || !(stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES))) return;
        double t = Easing.inOutCubic(progress), arc = Math.sin(Math.PI * t);
        double follow = Math.sin(2 * Math.PI * t) * Math.sin(Math.PI * progress);
        double strength = config.firstPersonIntensity;
        var player = Minecraft.getInstance().player;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate((float) (side * arc * .045 * strength), (float) (-arc * .025 * strength), (float) (-arc * .035 * strength));
        pose.mulPose(ROTATION.rotationXYZ((float) Math.toRadians((-8 * arc + 3 * follow) * strength),
                (float) Math.toRadians(side * (12 * arc + 3 * follow) * strength), (float) Math.toRadians(-side * 9 * arc * strength)));
        lastAttack = arc * strength;
    }
    public static double lastAttack() { return lastAttack; }
    public static void reset() { MAIN.reset(); OFF.reset(); lastAttack = 0; }

    public static final class Hand {
        private final ItemSwapTimeline timeline = new ItemSwapTimeline();
        private ItemStack rendered = ItemStack.EMPTY, pending = ItemStack.EMPTY;
        private int slot = -99, serial, visible;
        private boolean primed;

        public void update(ItemStack actual, int selectedSlot, long now) {
            timeline.sample(now); sync();
            if (!primed) {
                primed = true; slot = selectedSlot; rendered = pending = actual; visible = ++serial;
                timeline.select(serial, now); return;
            }
            if (slot != selectedSlot || !ItemStack.isSameItem(pending, actual)) {
                if (!timeline.active()) rendered = rendered.copy();
                pending = actual.copy(); slot = selectedSlot;
                timeline.select(++serial, now); sync();
            } else timeline.sample(now);
            if (!timeline.active()) rendered = pending = actual;
        }
        private void sync() {
            if (primed && visible != timeline.visibleKey()) { rendered = pending; visible = timeline.visibleKey(); }
        }
        public ItemStack rendered() { return rendered; }
        public double lower() { return timeline.lower(); }
        public boolean swapping() { return timeline.active(); }
        public void reset() { timeline.reset(); rendered = pending = ItemStack.EMPTY; primed = false; slot = -99; serial = visible = 0; }
    }
}
