package dev.transparentinventory.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.compat.MinecraftAccess;
import dev.transparentinventory.client.effects.BlockFeedback;
import dev.transparentinventory.client.motion.FirstPersonMotion;
import dev.transparentinventory.client.motion.VisualMotionController;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real client/server interactions in a separate disposable world; never included in the release. */
@SuppressWarnings("UnstableApiUsage")
public final class MotionGameTest implements FabricClientGameTest {
    private ClientGameTestContext context;
    private TestSingleplayerContext world;
    private static final BlockPos BLOCK = new BlockPos(0, 200, 3);

    @Override public void runTest(ClientGameTestContext context) {
        this.context = context;
        context.runOnClient(client -> {
            InventoryConfig.setInstance(new InventoryConfig());
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.options.bobView().set(true);
            client.options.screenEffectScale().set(1.0);
            client.options.damageTiltStrength().set(1.0);
            client.options.particles().set(ParticleStatus.ALL);
            VisualMotionController.INSTANCE.reset();
        });
        try (var singleplayer = context.worldBuilder().create()) {
            world = singleplayer;
            TestWorldAccess.chunks(world);
            command("fill -10 199 -10 10 199 15 minecraft:stone");
            command("fill -10 200 -10 10 207 15 minecraft:air");
            command("tp @a 0.5 200 0.5 0 0");
            command("gamemode survival @a");
            command("item replace entity @a hotbar.0 with minecraft:diamond_sword");
            command("item replace entity @a hotbar.1 with minecraft:iron_axe");
            command("item replace entity @a hotbar.2 with minecraft:stone 64");
            context.waitFor(client -> client.player.onGround());
            switchingAndAttack();
            cameraMotion();
            confirmedHits();
            blockFeedback();
            vanillaUseItems();
            settingsAndGuards();
        }
        check(context.computeOnClient(client -> VisualMotionController.INSTANCE.x() == 0
                && VisualMotionController.INSTANCE.y() == 0 && BlockFeedback.activeCount() == 0), "world unload clears motion and owned particles");
        System.out.println("[motionGameTest] item swap, attack, sprint, jump/landing, confirmed hit, block feedback and accessibility passed");
    }

    private void command(String command) { world.getServer().runCommand(command); TestWorldAccess.clientPackets(context, world); }

    private void switchingAndAttack() {
        context.getInput().pressKey(InputConstants.KEY_1);
        context.waitFor(client -> FirstPersonMotion.hand(InteractionHand.MAIN_HAND).rendered().is(Items.DIAMOND_SWORD)
                && !FirstPersonMotion.hand(InteractionHand.MAIN_HAND).swapping());
        context.getInput().pressKey(InputConstants.KEY_2);
        check(context.computeOnClient(client -> client.player.getInventory().getSelectedSlot() == 1
                && client.player.getMainHandItem().is(Items.IRON_AXE)), "actual held item changes immediately via vanilla");
        context.waitFor(client -> FirstPersonMotion.hand(InteractionHand.MAIN_HAND).swapping()
                && FirstPersonMotion.hand(InteractionHand.MAIN_HAND).lower() > .15);
        context.takeScreenshot("motion-item-lowering");
        context.waitFor(client -> FirstPersonMotion.hand(InteractionHand.MAIN_HAND).rendered().is(Items.IRON_AXE)
                && !FirstPersonMotion.hand(InteractionHand.MAIN_HAND).swapping());
        context.takeScreenshot("motion-item-raised");
        context.getInput().pressKey(InputConstants.KEY_1);
        context.getInput().pressKey(InputConstants.KEY_2);
        context.getInput().pressKey(InputConstants.KEY_1);
        context.waitFor(client -> FirstPersonMotion.hand(InteractionHand.MAIN_HAND).rendered().is(Items.DIAMOND_SWORD)
                && !FirstPersonMotion.hand(InteractionHand.MAIN_HAND).swapping());
        check(context.computeOnClient(client -> client.player.getInventory().getItem(0).getCount() == 1
                && client.player.getInventory().getItem(1).getCount() == 1), "rapid visual swaps preserve inventory");
        var angles = context.computeOnClient(client -> new float[]{client.player.getYRot(), client.player.getXRot()});
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> FirstPersonMotion.lastAttack() > .1);
        context.takeScreenshot("motion-sword-swing");
        check(context.computeOnClient(client -> client.player.getYRot() == angles[0] && client.player.getXRot() == angles[1]), "swing transforms never write player angles");
        check(context.computeOnClient(client -> VisualMotionController.INSTANCE.lastHit() == 0), "empty swing does not trigger confirmed-hit feedback");
    }

    private void cameraMotion() {
        context.waitTicks(10);
        context.getInput().holdKey(InputConstants.KEY_W);
        context.getInput().holdKey(InputConstants.KEY_LCONTROL);
        context.waitFor(client -> client.player.isSprinting() && Math.abs(VisualMotionController.INSTANCE.x()) > .0001);
        context.takeScreenshot("motion-sprint-sway");
        context.runOnClient(client -> client.options.bobView().set(false));
        context.waitTicks(2);
        check(context.computeOnClient(client -> VisualMotionController.INSTANCE.x() == 0 && VisualMotionController.INSTANCE.y() == 0), "vanilla bobbing off disables added camera motion");
        context.runOnClient(client -> client.options.bobView().set(true));
        context.getInput().releaseKey(InputConstants.KEY_LCONTROL);
        context.getInput().releaseKey(InputConstants.KEY_W);
        context.waitTicks(5);
        context.getInput().holdKey(InputConstants.KEY_SPACE);
        context.waitFor(client -> VisualMotionController.INSTANCE.lastJump() > 0);
        context.getInput().releaseKey(InputConstants.KEY_SPACE);
        context.takeScreenshot("motion-jump");
        context.waitFor(client -> client.player.onGround() && VisualMotionController.INSTANCE.lastLanding() > 0);
        context.takeScreenshot("motion-landing");
        long previous = context.computeOnClient(client -> VisualMotionController.INSTANCE.lastLanding());
        command("tp @a 0.5 204 0.5 0 0");
        context.waitFor(client -> client.player.onGround() && VisualMotionController.INSTANCE.lastLanding() > previous);
        context.takeScreenshot("motion-hard-landing");
        context.runOnClient(client -> client.options.screenEffectScale().set(0.0));
        context.waitTicks(2);
        check(context.computeOnClient(client -> VisualMotionController.INSTANCE.y() == 0), "zero screen effects suppress camera impulses");
        context.runOnClient(client -> client.options.screenEffectScale().set(1.0));
    }

    private void confirmedHits() {
        command("tp @a 0.5 200 0.5 0 0");
        command("summon minecraft:pig 0.5 200 2.5 {NoAI:1b,Silent:1b,Tags:[\"motion_target\"]}");
        context.getInput().pressKey(InputConstants.KEY_1); context.waitTicks(20);
        context.runOnClient(client -> {
            var pig = client.level.getEntitiesOfClass(Pig.class, new AABB(-2, 199, 0, 2, 203, 4)).getFirst();
            client.gameMode.attack(client.player, pig);
        });
        context.waitFor(client -> VisualMotionController.INSTANCE.lastHit() > 0);
        context.waitFor(client -> Math.abs(VisualMotionController.INSTANCE.pitch()) > .0001);
        context.takeScreenshot("motion-confirmed-hit");
        long hit = context.computeOnClient(client -> VisualMotionController.INSTANCE.lastHit());
        command("data merge entity @e[tag=motion_target,limit=1] {Invulnerable:1b}");
        context.waitTicks(20);
        context.runOnClient(client -> {
            var pig = client.level.getEntitiesOfClass(Pig.class, new AABB(-2, 199, 0, 2, 203, 5)).getFirst();
            client.gameMode.attack(client.player, pig);
        });
        context.waitTicks(8);
        check(context.computeOnClient(client -> VisualMotionController.INSTANCE.lastHit() == hit), "rejected attack has no new impact pulse");
        world.getServer().runOnServer(server -> server.overworld().getEntitiesOfClass(Pig.class,
                new AABB(-3, 199, -2, 3, 203, 6)).forEach(net.minecraft.world.entity.Entity::discard));
        TestWorldAccess.clientPackets(context, world);
    }

    private void blockFeedback() {
        command("tp @a 0.5 200 0.5 0 20");
        command("gamemode creative @a");
        command("setblock 0 200 3 minecraft:stone");
        context.runOnClient(client -> { TestWorldAccess.clearToasts(client); TestWorldAccess.clearChat(client); });
        context.getInput().pressKey(InputConstants.KEY_2); context.waitTicks(3);
        long before = context.computeOnClient(client -> BlockFeedback.emitted());
        check(context.computeOnClient(client -> client.gameMode.startDestroyBlock(BLOCK, Direction.NORTH)), "vanilla accepts block break");
        context.waitFor(client -> BlockFeedback.emitted() > before);
        check(context.computeOnClient(client -> BlockFeedback.activeCount() <= 16), "break particle burst is bounded");
        context.waitTicks(1);
        context.takeScreenshot("motion-block-break");
        TestWorldAccess.serverPackets(context, world); TestWorldAccess.clientPackets(context, world);
        context.waitFor(client -> client.level.getBlockState(BLOCK).isAir());
        context.getInput().pressKey(InputConstants.KEY_3); context.waitTicks(8);
        long placementBefore = context.computeOnClient(client -> BlockFeedback.emitted());
        context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(.5, 200, 3.5), Direction.UP, BLOCK.below(), false)));
        context.waitFor(client -> BlockFeedback.emitted() > placementBefore);
        context.waitTicks(1);
        context.takeScreenshot("motion-block-place-pulse");
        TestWorldAccess.serverPackets(context, world); TestWorldAccess.clientPackets(context, world);
        check(world.getServer().computeOnServer(server -> !server.overworld().getBlockState(BLOCK).isAir()), "placement still reaches vanilla server");
        context.waitTicks(8);
        context.runOnClient(client -> client.options.particles().set(ParticleStatus.MINIMAL));
        long minimalBefore = context.computeOnClient(client -> BlockFeedback.emitted());
        context.runOnClient(client -> client.gameMode.startDestroyBlock(BLOCK, Direction.NORTH));
        context.waitTicks(5);
        check(context.computeOnClient(client -> BlockFeedback.emitted() == minimalBefore), "minimal vanilla particles suppress extra fragments");
        context.runOnClient(client -> client.options.particles().set(ParticleStatus.ALL));
        command("gamemode survival @a");
    }

    private void vanillaUseItems() {
        command("item replace entity @a hotbar.3 with minecraft:filled_map[minecraft:map_id=0]");
        context.getInput().pressKey(InputConstants.KEY_4); context.waitTicks(10);
        check(context.computeOnClient(client -> !FirstPersonMotion.active(client, client.player.getMainHandItem())), "map hand coordination bypasses custom motion");
        context.takeScreenshot("motion-vanilla-map");
        command("item replace entity @a hotbar.4 with minecraft:potion");
        context.getInput().pressKey(InputConstants.KEY_5); context.waitTicks(10);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitFor(client -> client.player.isUsingItem());
        check(context.computeOnClient(client -> !FirstPersonMotion.active(client, client.player.getMainHandItem())), "drinking keeps vanilla use animation");
        context.takeScreenshot("motion-vanilla-drinking");
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitTicks(3);
        command("item replace entity @a hotbar.5 with minecraft:bow");
        command("give @a minecraft:arrow 16");
        context.getInput().pressKey(InputConstants.KEY_6); context.waitTicks(10);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitFor(client -> client.player.isUsingItem());
        check(context.computeOnClient(client -> !FirstPersonMotion.active(client, client.player.getMainHandItem())), "bow use retains vanilla hand selection");
        context.takeScreenshot("motion-vanilla-bow");
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitTicks(3);
    }

    private void settingsAndGuards() {
        context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(2);
        check(context.computeOnClient(client -> VisualMotionController.INSTANCE.x() == 0 && VisualMotionController.INSTANCE.y() == 0), "third person disables first-person camera effects");
        context.runOnClient(client -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            InventoryConfig.get().liquidGlassUi = true;
            TestWorldAccess.clearToasts(client); TestWorldAccess.clearChat(client);
        });
        context.getInput().pressKey(InputConstants.KEY_F8);
        for (String key : new String[]{"spatialui.settings.more", "spatialui.settings.entrance", "spatialui.settings.look",
                "spatialui.settings.hotbar", "spatialui.settings.motion"}) click(key);
        click("spatialui.motion.reduce");
        context.takeScreenshot("motion-accessibility-settings");
        click("spatialui.settings.camera_motion");
        click("spatialui.motion.hit");
        check(context.computeOnClient(client -> !InventoryConfig.get().motion.hitShake), "hit feedback has an independent F8 toggle");
        context.takeScreenshot("motion-camera-settings");
        click("spatialui.settings.world_effects");
        click("spatialui.motion.break");
        check(context.computeOnClient(client -> !InventoryConfig.get().motion.blockBreakEffects), "world feedback has an independent F8 toggle");
        context.takeScreenshot("motion-world-settings");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        check(context.computeOnClient(client -> InventoryConfig.get().motion.reduceMotion), "reduce motion enabled from real F8 UI");
        context.getInput().pressKey(InputConstants.KEY_1);
        context.waitTicks(4);
        check(context.computeOnClient(client -> !VisualMotionController.available(client)
                && !FirstPersonMotion.hand(InteractionHand.MAIN_HAND).swapping() && BlockFeedback.activeCount() == 0), "reduce motion disables all new systems");
        context.runOnClient(client -> {
            try {
                var path = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(dev.transparentinventory.client.ConfigLoader.FILE_NAME);
                var saved = new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(path), InventoryConfig.class);
                check(saved.motion.reduceMotion && saved.glassHotbar && saved.liquidGlassUi, "new settings persist alongside existing glass config");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
            InventoryConfig.get().motion.reduceMotion = false; InventoryConfig.get().motion.enabled = false;
        });
        context.waitTicks(2);
        check(context.computeOnClient(client -> !VisualMotionController.available(client)), "motion master off restores vanilla paths");
        context.runOnClient(client -> InventoryConfig.get().motion.enabled = true);
        command("gamemode spectator @a");
        context.waitFor(client -> client.player.isSpectator());
        check(context.computeOnClient(client -> !VisualMotionController.firstPerson(client)), "spectator has no added camera/hand motion");
        command("gamemode survival @a");
    }

    private void click(String key) {
        var point = context.computeOnClient(client -> {
            var button = MinecraftAccess.screen(client).children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(widget -> widget.getMessage().getString().startsWith(Component.translatable(key).getString())).findFirst().orElseThrow();
            var window = client.getWindow();
            return new double[]{(button.getX() + button.getWidth() / 2.0) * window.getScreenWidth() / window.getGuiScaledWidth(),
                    (button.getY() + 10.0) * window.getScreenHeight() / window.getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(point[0], point[1]); context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); context.waitTicks(1);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
