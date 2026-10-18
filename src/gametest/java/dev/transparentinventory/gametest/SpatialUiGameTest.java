package dev.transparentinventory.gametest;

import dev.transparentinventory.gametest.mixin.KeyboardTestAccess;
import dev.transparentinventory.gametest.mixin.MouseTestAccess;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anchor.AnchorType;
import dev.transparentinventory.client.mixin.ContainerScreenAccessor;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.platform.InputConstants;

/** Engine tests, including native mouse/keyboard callback dispatch and real server menus.
 * Separate source set and temporary game directory: never packaged in the release jar. */
@SuppressWarnings("UnstableApiUsage")
public final class SpatialUiGameTest implements FabricClientGameTest {
    private static final BlockPos STATION = new BlockPos(0, 200, 3);
    private ClientGameTestContext context;
    private TestSingleplayerContext world;
    private int screens;

    @Override public void runTest(ClientGameTestContext testContext) {
        if ("motion".equals(System.getProperty("hoverglass.testScope"))) return;
        context = testContext;
        context.runOnClient(client -> {
            InventoryConfig config = new InventoryConfig();
            config.guiScale = 0.8; // Exercise inverse mapping at non-native scale.
            config.openingAnimationDurationMs = 0;
            config.cameraTransitionDurationMs = 0;
            InventoryConfig.setInstance(config);
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        try (var singleplayer = context.worldBuilder().create()) {
            world = singleplayer;
            TestWorldAccess.chunks(world);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 8; z++) {
                    level.getChunkAt(new BlockPos(x, 199, z));
                    level.setBlockAndUpdate(new BlockPos(x, 199, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    for (int y = 200; y <= 204; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            });
            command("tp @a 0.5 200 0.5 0 20");
            command("gamemode survival @a");
            command("give @a minecraft:stone 64");
            command("experience add @a 100 levels");
            context.waitTicks(10);
            if (!"creative".equals(System.getProperty("hoverglass.testScope"))) {
                cinematicEntrance();
                inventory();
                cameraAndCloseSafety();
                for (String station : new String[]{"chest", "barrel", "crafting_table", "furnace", "anvil", "enchanting_table",
                        "blast_furnace", "smoker", "brewing_stand", "smithing_table", "loom", "stonecutter", "grindstone",
                        "cartography_table", "beacon", "hopper", "dispenser", "dropper"}) {
                    openBlock(station);
                    assertSpatial(AnchorType.BLOCK);
                    if (station.equals("chest")) chest();
                    if (station.equals("anvil")) anvil();
                    if (station.equals("enchanting_table")) enchanting();
                    hoverInventory();
                    context.takeScreenshot("spatialui-" + station);
                    screens++;
                    close();
                }
                largeChest();
                merchant();
                horse();
            }
            creativeInventory();
            liquidGlassMenus();
            glassHotbar();
            context.getInput().pressKey(InputConstants.KEY_E); context.waitForScreen(InventoryScreen.class);
            context.getInput().pressKey(InputConstants.KEY_F8);
            check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.settings() != null), "settings active before world unload");
        }
        check(context.computeOnClient(client -> client.options.getCameraType()) == CameraType.FIRST_PERSON, "world unload restores perspective");
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.settings() == null), "world unload removes overlay");
        System.out.println("[spatialGameTest] " + screens + " screens passed rendering/input smoke checks");
    }

    private void command(String text) { world.getServer().runCommand(text); TestWorldAccess.clientPackets(context, world); }

    private void glassHotbar() {
        command("item replace entity @a hotbar.0 with minecraft:diamond_sword");
        command("item replace entity @a hotbar.1 with minecraft:firework_rocket 64");
        command("item replace entity @a hotbar.8 with minecraft:apple 32");
        command("item replace entity @a weapon.offhand with minecraft:torch 64");
        context.runOnClient(client -> {
            InventoryConfig.get().glassHotbar = true;
            InventoryConfig.get().hotbarOpacity = 0.16F;
            client.options.mainHand().set(net.minecraft.world.entity.HumanoidArm.RIGHT);
            TestWorldAccess.clearToasts(client);
            TestWorldAccess.clearChat(client);
        });
        context.getInput().pressKey(InputConstants.KEY_1);
        context.waitTicks(2);
        check(context.computeOnClient(client -> client.player.getInventory().getSelectedSlot() == 0
                && client.player.getMainHandItem().is(Items.DIAMOND_SWORD)), "glass hotbar preserves number-key selection");
        context.takeScreenshot("spatialui-hotbar-glass");
        context.getInput().pressKey(InputConstants.KEY_9);
        context.waitTicks(2);
        check(context.computeOnClient(client -> client.player.getInventory().getSelectedSlot() == 8
                && client.player.getMainHandItem().getCount() == 32), "glass highlight follows last slot without altering items");
        context.takeScreenshot("spatialui-hotbar-last-slot");
        context.getInput().scroll(-1); context.waitTicks(2);
        check(context.computeOnClient(client -> client.player.getInventory().getSelectedSlot() == 0), "glass hotbar preserves wheel wraparound");
        context.runOnClient(client -> {
            client.options.mainHand().set(net.minecraft.world.entity.HumanoidArm.LEFT);
            client.options.broadcastOptions();
        });
        context.waitFor(client -> client.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        context.takeScreenshot("spatialui-hotbar-left-handed");
        context.runOnClient(client -> {
            client.options.mainHand().set(net.minecraft.world.entity.HumanoidArm.RIGHT);
            client.options.broadcastOptions();
        });
        context.waitFor(client -> client.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        context.getInput().pressKey(InputConstants.KEY_F1);
        check(context.computeOnClient(client -> TestWorldAccess.hudHidden(client)), "F1 hides HUD including glass hotbar");
        context.takeScreenshot("spatialui-hotbar-hidden");
        context.getInput().pressKey(InputConstants.KEY_F1);
        context.getInput().pressKey(InputConstants.KEY_E);
        context.waitForScreen(InventoryScreen.class); context.waitTicks(3);
        context.takeScreenshot("spatialui-hotbar-with-inventory");
        Object menu = context.computeOnClient(client -> client.player.containerMenu);
        context.getInput().pressKey(InputConstants.KEY_F8);
        settingsButton("spatialui.settings.more");
        settingsButton("spatialui.settings.entrance");
        settingsButton("spatialui.settings.look");
        settingsButton("spatialui.settings.hotbar");
        settingsButton("spatialui.hotbar.glass");
        check(context.computeOnClient(client -> !InventoryConfig.get().glassHotbar), "HUD glass disabled through actual F8 toggle");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        check(context.computeOnClient(client -> client.player.containerMenu == menu), "HUD settings preserve live inventory menu");
        close(); context.takeScreenshot("spatialui-hotbar-vanilla");
        context.getInput().pressKey(InputConstants.KEY_F8);
        settingsButton("spatialui.settings.more");
        settingsButton("spatialui.settings.entrance");
        settingsButton("spatialui.settings.look");
        settingsButton("spatialui.settings.hotbar");
        settingsButton("spatialui.hotbar.glass");
        var point = context.computeOnClient(client -> {
            var slider = (net.minecraft.client.gui.components.AbstractWidget)
                    dev.transparentinventory.client.compat.MinecraftAccess.screen(client).children().get(1);
            return raw(client, slider.getRight() - 2, slider.getY() + 10);
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); context.waitTicks(1);
        check(context.computeOnClient(client -> InventoryConfig.get().hotbarOpacity == 0), "HUD transparency slider reaches 100 percent");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.takeScreenshot("spatialui-hotbar-fully-transparent");
        context.runOnClient(client -> {
            try {
                var file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(dev.transparentinventory.client.ConfigLoader.FILE_NAME);
                var saved = new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(file), InventoryConfig.class);
                check(saved.glassHotbar && saved.hotbarOpacity == 0, "HUD glass and opacity persist to existing config");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
            InventoryConfig.get().hotbarOpacity = 0.16F;
        });
    }

    private void liquidGlassMenus() {
        context.runOnClient(client -> InventoryConfig.get().liquidGlassUi = false);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(net.minecraft.client.gui.screens.PauseScreen.class);
        Object pause = context.computeOnClient(dev.transparentinventory.client.compat.MinecraftAccess::screen);
        context.takeScreenshot("spatialui-pause-vanilla");
        context.getInput().pressKey(InputConstants.KEY_F8);
        context.waitForScreen(dev.transparentinventory.client.screen.SpatialSettingsScreen.class);
        check(context.computeOnClient(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client).isPauseScreen()),
                "settings opened from pause keeps singleplayer paused");
        settingsButton("spatialui.ui.liquid_glass");
        check(context.computeOnClient(client -> InventoryConfig.get().liquidGlassUi), "liquid glass enabled by actual settings click");
        context.takeScreenshot("spatialui-liquid-settings");
        // Exercise native slider dragging while its chrome is replaced.
        var point = context.computeOnClient(client -> {
            var settings = dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            var slider = (net.minecraft.client.gui.components.AbstractWidget) settings.children().getFirst();
            return raw(client, slider.getX() + slider.getWidth() * 0.3, slider.getY() + 10);
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.getInput().setCursorPos(point[0] + 20, point[1]);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        check(context.computeOnClient(client -> InventoryConfig.get().panelOpacity > 0.4 && InventoryConfig.get().panelOpacity < 0.8),
                "glass slider retains native value updates");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        check(context.computeOnClient(dev.transparentinventory.client.compat.MinecraftAccess::screen) == pause,
                "settings returns to same pause menu");
        context.runOnClient(client -> {
            try {
                var file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(dev.transparentinventory.client.ConfigLoader.FILE_NAME);
                var saved = new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(file), InventoryConfig.class);
                check(saved.liquidGlassUi, "liquid glass choice persisted to JSON");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        });
        context.takeScreenshot("spatialui-pause-liquid");
        settingsButton("menu.options");
        context.waitForScreen(net.minecraft.client.gui.screens.options.OptionsScreen.class);
        context.takeScreenshot("spatialui-options-liquid");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(net.minecraft.client.gui.screens.PauseScreen.class);
        context.getInput().pressKey(InputConstants.KEY_F8);
        settingsButton("spatialui.ui.liquid_glass");
        check(context.computeOnClient(client -> !InventoryConfig.get().liquidGlassUi), "liquid glass toggles off immediately");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.takeScreenshot("spatialui-pause-restored");
        settingsButton("menu.returnToGame");
        context.waitFor(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client) == null);
        screens += 2;
    }

    private void creativeInventory() {
        // The preceding container tests change inventory on both sides. Force an
        // authoritative full snapshot for this scenario's empty destination slot.
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.inventoryMenu.sendAllDataToRemote();
        });
        TestWorldAccess.clientPackets(context, world);
        context.waitFor(client -> client.player.getInventory().getItem(0).isEmpty());
        command("gamemode creative @a");
        context.waitFor(client -> client.player.hasInfiniteMaterials());
        context.runOnClient(client -> {
            var config = new InventoryConfig();
            config.panelOpacity = 0.65F; // Simulate an existing user's global setting.
            config.openingAnimationDurationMs = 1000;
            config.cameraTransitionDurationMs = 500;
            InventoryConfig.setInstance(config);
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        long started = System.nanoTime();
        context.getInput().pressKey(InputConstants.KEY_E);
        context.waitForScreen(CreativeModeInventoryScreen.class);
        context.waitFor(client -> SpatialScreenManager.INSTANCE.mapsInput());
        double earlyX = context.computeOnClient(client -> SpatialScreenManager.INSTANCE.transform().centerX());
        context.waitFor(client -> System.nanoTime() - started > 400_000_000L);
        context.takeScreenshot("spatialui-creative-entering");
        context.waitFor(client -> System.nanoTime() - started > 1_150_000_000L);
        assertSpatial(AnchorType.PLAYER);
        context.runOnClient(client -> {
            var screen = (CreativeModeInventoryScreen) dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            var t = SpatialScreenManager.INSTANCE.transform();
            check(SpatialScreenManager.INSTANCE.cinematicInventory(), "creative inventory uses cinematic player profile");
            check(t.centerX() > earlyX && t.centerX() < screen.width * 0.4, "creative panel enters from left");
            check(Math.abs(t.tiltDegrees() - 4) < 0.01, "creative panel retains its tilt");
            var eye = client.gameRenderer.projectPointToScreen(client.player.getEyePosition());
            check(eye.x > 0.3 && eye.x < 0.9, "creative mode frames real player on right");
            check(Math.abs(InventoryConfig.get().opacityFor(screen.getClass().getName()) - 0.16F) < 1E-6,
                    "creative glass is enabled at 84% transparency without pressing preset");
            var bounds = (ContainerScreenAccessor) screen;
            check(t.screenY(bounds.spatial$left(), bounds.spatial$top() - 58) >= 0, "creative top tabs and page buttons fit window");
        });
        context.getInput().setCursorPos(10, 10);
        context.runOnClient(client -> {
            TestWorldAccess.clearToasts(client);
            TestWorldAccess.clearChat(client);
        });
        context.takeScreenshot("spatialui-creative-glass");
        headTracking("creative");
        hoverInventory();
        var picked = context.computeOnClient(client -> client.player.containerMenu.getSlot(0).getItem().getItem());
        clickSlot(0, 0);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().is(picked)), "creative catalog click picks item");
        clickSlot(45, 0);
        context.runOnClient(client -> System.out.println("[creativeGameTest] after hotbar placement: carried="
                + client.player.containerMenu.getCarried() + " slot45=" + client.player.containerMenu.getSlot(45).getItem()
                + " inventory0=" + client.player.getInventory().getItem(0)
                + " hovered=" + ((ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatial$hoveredSlot()));
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().isEmpty()
                && client.player.getInventory().getItem(0).is(picked)), "creative catalog item places in real hotbar");
        moveToSlot(0);
        context.getInput().scroll(-3); context.waitTicks(3);
        check(context.computeOnClient(client -> ((dev.transparentinventory.gametest.mixin.CreativeTestAccess)
                dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatialtest$scroll()) > 0, "creative wheel scroll maps correctly");
        var thumb = context.computeOnClient(client -> {
            var bounds = (ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            return projected(client, bounds.spatial$left() + 181, bounds.spatial$top() + 25);
        });
        context.getInput().setCursorPos(thumb[0], thumb[1]);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        var bottom = context.computeOnClient(client -> {
            var bounds = (ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            return projected(client, bounds.spatial$left() + 181, bounds.spatial$top() + 124);
        });
        context.getInput().setCursorPos(bottom[0], bottom[1]); context.waitTicks(2);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(client -> ((dev.transparentinventory.gametest.mixin.CreativeTestAccess)
                dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatialtest$scroll()) > 0.8,
                "creative scrollbar drag uses transformed coordinates");

        creativeTab(net.minecraft.world.item.CreativeModeTab.Type.SEARCH);
        context.getInput().typeChars("diamond"); context.waitTicks(4);
        check(context.computeOnClient(client -> ((dev.transparentinventory.gametest.mixin.CreativeTestAccess)
                dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatialtest$search().getValue()).equals("diamond"),
                "creative search receives native typing after transformed tab click");
        check(context.computeOnClient(client -> !client.player.containerMenu.getSlot(0).getItem().isEmpty()
                && client.player.containerMenu.getSlot(0).getItem().getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains("diamond")),
                "creative search filters actual items");
        context.getInput().setCursorPos(10, 10);
        context.takeScreenshot("spatialui-creative-search");
        creativeTab(net.minecraft.world.item.CreativeModeTab.Type.INVENTORY);
        check(context.computeOnClient(client -> ((CreativeModeInventoryScreen) dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).isInventoryOpen()),
                "creative personal inventory tab works");
        hoverInventory();
        context.getInput().setCursorPos(10, 10);
        context.takeScreenshot("spatialui-creative-personal");
        clickSlot(36, 0);
        check(context.computeOnClient(client -> !client.player.containerMenu.getCarried().isEmpty()), "creative personal hotbar pickup");
        int trash = context.computeOnClient(client -> ((dev.transparentinventory.gametest.mixin.CreativeTestAccess)
                dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatialtest$trash().index);
        clickSlot(trash, 0);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().isEmpty()), "creative trash slot still deletes carried item");
        Object menu = context.computeOnClient(client -> client.player.containerMenu);
        context.getInput().pressKey(InputConstants.KEY_F8);
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.settings() != null && client.player.containerMenu == menu),
                "creative F8 keeps actual menu alive");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.runOnClient(client -> {
            InventoryConfig.get().openingAnimationDurationMs = 0;
            InventoryConfig.get().cameraTransitionDurationMs = 0;
        });
        screens++;
        close();
        command("gamemode survival @a");
        context.waitFor(client -> !client.player.hasInfiniteMaterials());
        context.getInput().pressKey(InputConstants.KEY_E);
        context.waitForScreen(InventoryScreen.class); context.waitTicks(3);
        command("gamemode creative @a");
        context.waitForScreen(CreativeModeInventoryScreen.class); context.waitTicks(3);
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.cinematicInventory()),
                "survival-to-creative redirect preserves original first-person intent");
        close();
        command("gamemode survival @a");
        context.waitFor(client -> !client.player.hasInfiniteMaterials());
    }

    private void creativeTab(net.minecraft.world.item.CreativeModeTab.Type type) {
        var point = context.computeOnClient(client -> {
            var screen = dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            var bounds = (ContainerScreenAccessor) screen;
            var access = (dev.transparentinventory.gametest.mixin.CreativeTestAccess) screen;
            var tab = net.minecraft.world.item.CreativeModeTabs.tabs().stream().filter(t -> t.getType() == type).findFirst().orElseThrow();
            return projected(client, bounds.spatial$left() + access.spatialtest$tabX(tab) + 13,
                    bounds.spatial$top() + access.spatialtest$tabY(tab) + 16);
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); context.waitTicks(3);
    }

    private void cinematicEntrance() {
        context.runOnClient(client -> {
            InventoryConfig.get().openingAnimationDurationMs = 1000;
            InventoryConfig.get().cameraTransitionDurationMs = 500;
        });
        long started = System.nanoTime();
        context.getInput().pressKey(InputConstants.KEY_E);
        context.waitForScreen(InventoryScreen.class);
        context.waitFor(client -> SpatialScreenManager.INSTANCE.mapsInput());
        double earlyX = context.computeOnClient(client -> SpatialScreenManager.INSTANCE.transform().centerX());
        context.waitFor(client -> System.nanoTime() - started > 400_000_000L);
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.impactFlash()) > 0.1,
                "visible landing flash begins while panel is arriving");
        context.takeScreenshot("spatialui-inventory-entering");
        context.waitFor(client -> System.nanoTime() - started > 1_100_000_000L);
        context.runOnClient(client -> {
            var transform = SpatialScreenManager.INSTANCE.transform();
            check(transform.centerX() > earlyX, "inventory slides in from the left");
            check(Math.abs(transform.tiltDegrees() - 4) < 0.01, "inventory retains its resting slant");
            check(transform.centerX() < dev.transparentinventory.client.compat.MinecraftAccess.screen(client).width * 0.4, "inventory rests in left composition");
            var eye = client.gameRenderer.projectPointToScreen(client.player.getEyePosition());
            check(eye.x > 0.3 && eye.x < 0.9, "real player is framed on right of screen");
        });
        context.takeScreenshot("spatialui-inventory-cinematic");
        context.runOnClient(client -> InventoryConfig.get().openingAnimationDurationMs = 0);
        close();
        context.runOnClient(client -> {
            InventoryConfig.get().cameraTransitionDurationMs = 0;
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        context.getInput().pressKey(InputConstants.KEY_E); context.waitForScreen(InventoryScreen.class); context.waitTicks(3);
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.transform().tiltDegrees()) == 0,
                "already-third-person inventory retains standard layout");
        headTracking("third-person");
        context.runOnClient(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client).onClose());
        context.waitFor(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client) == null); context.waitTicks(3);
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
    }

    private void inventory() {
        context.getInput().pressKey(InputConstants.KEY_E);
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(8);
        assertSpatial(AnchorType.PLAYER);
        registryRules();
        int source = findItem(Items.STONE);
        clickSlot(source, 0);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().getCount()) == 64, "inventory left click");
        clickSlot(9, 1);
        check(context.computeOnClient(client -> client.player.containerMenu.getSlot(9).getItem().getCount()) == 1, "inventory right click places one");
        clickSlot(source, 0);
        shiftClickSlot(9);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().isEmpty()), "shift click leaves cursor empty");
        check(context.computeOnClient(client -> client.player.containerMenu.getSlot(9).getItem().isEmpty()), "inventory shift click");
        source = findItem(Items.STONE);
        clickSlot(source, 0);
        moveToSlot(10);
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        moveToSlot(11);
        context.waitTicks(2);
        moveToSlot(12);
        context.waitTicks(2);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().isEmpty()), "inventory drag distributes carried stack");
        int total = context.computeOnClient(client -> client.player.containerMenu.slots.stream().filter(slot -> slot.getItem().is(Items.STONE)).mapToInt(slot -> slot.getItem().getCount()).sum());
        check(total == 64, "drag conserves items");
        settings();
        recipeBookAndResize();
        glassPreset();
        headTracking("survival");
        hoverInventory();
        context.takeScreenshot("spatialui-inventory");
        screens++;
        close();
    }

    private void settings() {
        Object previous = context.computeOnClient(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client));
        context.getInput().pressKey(InputConstants.KEY_F8);
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.settings() != null), "F8 opens settings overlay");
        var xy = context.computeOnClient(client -> {
            var settings = SpatialScreenManager.INSTANCE.settings();
            var slider = (net.minecraft.client.gui.components.AbstractWidget) settings.children().get(1);
            return raw(client, slider.getX() + slider.getWidth() - 2, slider.getY() + 10);
        });
        context.getInput().setCursorPos(xy[0], xy[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(client -> InventoryConfig.get().opacityFor(InventoryScreen.class.getName())) == 0F, "per-screen slider reaches full transparency");
        xy = context.computeOnClient(client -> {
            var slider = (net.minecraft.client.gui.components.AbstractWidget) SpatialScreenManager.INSTANCE.settings().children().getFirst();
            return raw(client, slider.getX() + 2, slider.getY() + 10);
        });
        context.getInput().setCursorPos(xy[0], xy[1]); context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(client -> InventoryConfig.get().panelOpacity) == 1F, "global slider reaches opaque endpoint");
        check(context.computeOnClient(client -> InventoryConfig.get().opacityFor(InventoryScreen.class.getName())) == 0F, "global change does not overwrite independent screen");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        check(context.computeOnClient(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client)) == previous, "settings preserve original menu object");
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.settings() == null), "Escape closes only settings");
        context.runOnClient(client -> {
            try {
                var file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(dev.transparentinventory.client.ConfigLoader.FILE_NAME);
                var saved = new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(file), InventoryConfig.class);
                check(saved.opacityFor(InventoryScreen.class.getName()) == 0 && saved.panelOpacity == 1, "UI settings persist as JSON");
            } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            InventoryConfig.get().screenOpacities.clear();
            InventoryConfig.get().panelOpacity = 0.65F;
        });
    }

    private void glassPreset() {
        context.getInput().pressKey(InputConstants.KEY_F8);
        var point = context.computeOnClient(client -> {
            var widget = SpatialScreenManager.INSTANCE.settings().children().stream()
                    .filter(net.minecraft.client.gui.components.Button.class::isInstance)
                    .map(net.minecraft.client.gui.components.Button.class::cast)
                    .filter(button -> button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("spatialui.inventory.preset").getString()))
                    .findFirst().orElseThrow();
            return raw(client, widget.getX() + widget.getWidth() / 2.0, widget.getY() + 10);
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(client -> Math.abs(InventoryConfig.get().opacityFor(InventoryScreen.class.getName()) - 0.16F)) < 1E-6,
                "reference preset applies through actual settings button");
        context.takeScreenshot("spatialui-glass-settings");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(4);
        hoverInventory();
        context.takeScreenshot("spatialui-glass-reference");
        context.runOnClient(client -> {
            InventoryConfig.get().glassInventory = false;
        });
        context.waitTicks(3);
        hoverInventory();
        context.takeScreenshot("spatialui-glass-disabled");
        context.runOnClient(client -> {
            InventoryConfig.get().glassInventory = true;
            InventoryConfig.get().openingAnimationDurationMs = 0;
            InventoryConfig.get().cameraTransitionDurationMs = 0;
        });
    }

    private void recipeBookAndResize() {
        var xy = context.computeOnClient(client -> {
            var button = dev.transparentinventory.client.compat.MinecraftAccess.screen(client).children().stream().filter(net.minecraft.client.gui.components.ImageButton.class::isInstance)
                    .map(net.minecraft.client.gui.components.ImageButton.class::cast).findFirst().orElseThrow();
            return projected(client, button.getX() + 10, button.getY() + 9);
        });
        context.getInput().setCursorPos(xy[0], xy[1]); context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        check(context.computeOnClient(client -> ((dev.transparentinventory.client.mixin.RecipeBookScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatial$recipeBook().isVisible()), "recipe book toggle maps correctly");
        context.getInput().resizeWindow(1280, 720);
        context.waitTicks(6);
        hoverInventory();
        context.takeScreenshot("spatialui-recipe-book-resized");
        xy = context.computeOnClient(client -> {
            var button = dev.transparentinventory.client.compat.MinecraftAccess.screen(client).children().stream().filter(net.minecraft.client.gui.components.ImageButton.class::isInstance)
                    .map(net.minecraft.client.gui.components.ImageButton.class::cast).findFirst().orElseThrow();
            return projected(client, button.getX() + 10, button.getY() + 9);
        });
        context.getInput().setCursorPos(xy[0], xy[1]); context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
    }

    private void registryRules() {
        context.runOnClient(client -> {
            var registry = dev.transparentinventory.client.screen.SpatialScreenRegistry.INSTANCE;
            var config = InventoryConfig.get();
            var dummy = new UnknownContainer(client.player);
            check(registry.resolve(dummy).id().equals("handled"), "unknown handled screen gets generic profile");
            var custom = new dev.transparentinventory.client.screen.SpatialScreenProfile("test", AnchorType.SCREEN_SPACE, 1, 0, 0);
            registry.register(UnknownContainer.class, custom);
            check(registry.resolve(new DerivedContainer(client.player)) == custom, "nearest registered superclass profile");
            config.excludedScreens.add(UnknownContainer.class.getName());
            check(registry.resolve(new DerivedContainer(client.player)) == null, "superclass exclusion wins over registration");
            config.excludedScreens.clear();
            config.excludedScreens.add(InventoryScreen.class.getName());
            check(registry.resolve(dev.transparentinventory.client.compat.MinecraftAccess.screen(client)) == null, "live exclusion bypasses cached exact profile");
            config.excludedScreens.clear();
            var ordinary = new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("test")) { };
            check(registry.resolve(ordinary) == null, "non-container screens are opt-in");
            config.additionalScreens.add(ordinary.getClass().getName());
            check(registry.resolve(ordinary).anchor() == AnchorType.SCREEN_SPACE, "explicit non-container fallback");
            config.additionalScreens.clear();
        });
    }

    private void cameraAndCloseSafety() {
        command("fill -1 200 -1 1 204 -1 minecraft:stone");
        context.runOnClient(client -> {
            InventoryConfig.get().openingAnimationDurationMs = 180;
            InventoryConfig.get().cameraTransitionDurationMs = 250;
        });
        long started = System.nanoTime();
        context.getInput().pressKey(InputConstants.KEY_E); context.waitForScreen(InventoryScreen.class);
        context.waitFor(client -> System.nanoTime() - started > 400_000_000L);
        context.runOnClient(client -> System.out.println("[spatialGameTest] rear wall camera="
                + dev.transparentinventory.client.compat.MinecraftAccess.camera(client).position()
                + " eye=" + client.player.getEyePosition() + " yaw=" + client.player.getYRot()
                + " pitch=" + client.player.getXRot()));
        context.takeScreenshot("spatialui-rear-wall");
        check(context.computeOnClient(client -> dev.transparentinventory.client.compat.MinecraftAccess.camera(client).position().distanceTo(client.player.getEyePosition())) < 2,
                "vanilla obstruction rays shorten spatial camera distance");
        command("fill -1 200 -1 1 204 -1 minecraft:air");
        context.waitTicks(3);
        check(context.computeOnClient(client -> dev.transparentinventory.client.compat.MinecraftAccess.camera(client).position().distanceTo(client.player.getEyePosition())) > 2.8,
                "camera regains configured distance after obstruction removed");
        command("fill 1 200 -4 1 204 1 minecraft:stone");
        context.waitTicks(3);
        check(context.computeOnClient(client -> dev.transparentinventory.client.compat.MinecraftAccess.camera(client).position().x) < 0.95,
                "diagonal shoulder camera stops before side wall");
        command("fill 1 200 -4 1 204 1 minecraft:air");
        context.waitTicks(3);
        moveToSlot(findItem(Items.STONE));
        context.runOnClient(client -> {
            var screen = dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            screen.onClose(); screen.onClose();
            check(dev.transparentinventory.client.compat.MinecraftAccess.screen(client) == screen, "normal close waits for animation");
            var mouse = (MouseTestAccess) client.mouseHandler;
            var button = new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
            mouse.spatialtest$button(client.getWindow().handle(), button, InputConstants.PRESS);
            mouse.spatialtest$button(client.getWindow().handle(), button, InputConstants.RELEASE);
            check(client.player.containerMenu.getCarried().isEmpty(), "closing animation suppresses slot clicks");
        });
        context.waitFor(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client) == null);
        context.waitFor(client -> client.options.getCameraType() == CameraType.FIRST_PERSON);
        context.runOnClient(client -> { InventoryConfig.get().openingAnimationDurationMs = 0; InventoryConfig.get().cameraTransitionDurationMs = 0; });
    }

    private static class UnknownContainer extends AbstractContainerScreen<net.minecraft.world.inventory.InventoryMenu> {
        UnknownContainer(net.minecraft.world.entity.player.Player player) {
            super(player.inventoryMenu, player.getInventory(), net.minecraft.network.chat.Component.literal("Unknown"));
        }
    }
    private static final class DerivedContainer extends UnknownContainer {
        DerivedContainer(net.minecraft.world.entity.player.Player player) { super(player); }
    }

    private void openBlock(String name) {
        command("setblock 0 200 3 minecraft:" + name + " replace");
        if (name.equals("chest")) command("item replace block 0 200 3 container.0 with minecraft:diamond 32");
        if (name.equals("anvil")) command("give @a minecraft:iron_sword");
        if (name.equals("enchanting_table")) { command("give @a minecraft:diamond_sword"); command("give @a minecraft:lapis_lazuli 64"); }
        context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(STATION), Direction.NORTH, STATION, false)));
        context.waitFor(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client) instanceof AbstractContainerScreen<?>);
        context.waitTicks(8);
    }

    private void assertSpatial(AnchorType type) {
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.active()), "spatial session active");
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.anchor().type()) == type, "correct anchor " + type);
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.transform().valid()), "valid presented transform");
        check(context.computeOnClient(client -> client.options.getCameraType()) == CameraType.THIRD_PERSON_BACK, "third-person camera on open");
    }

    private void chest() {
        double size = context.computeOnClient(client -> SpatialScreenManager.INSTANCE.transform().scale());
        check(size >= 0.99, "default chest no longer shrinks its items below native GUI size");
        context.runOnClient(client -> InventoryConfig.get().cameraDistance = 6);
        context.waitTicks(4);
        check(Math.abs(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.transform().scale()) - size) < 0.01,
                "chest item size does not shrink with world-anchor distance");
        context.runOnClient(client -> InventoryConfig.get().cameraDistance = 3);
        context.waitTicks(4);
        context.waitFor(client -> client.player.containerMenu.getSlot(0).getItem().is(Items.DIAMOND));
        moveToSlot(0);
        check(context.computeOnClient(client -> ((ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatial$hoveredSlot().index) == 0, "chest hover follows transformed slot");
        clickSlot(0, 0);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().getCount()) == 32, "chest click picks up stack");
        clickSlot(1, 1);
        check(context.computeOnClient(client -> client.player.containerMenu.getSlot(1).getItem().getCount()) == 1, "chest right click");
        clickSlot(0, 0);
        shiftClickSlot(1);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().isEmpty()), "chest shift click is transfer, not pickup");
        check(context.computeOnClient(client -> client.player.containerMenu.getSlot(1).getItem().isEmpty()), "chest shift click transfers item");
        TestWorldAccess.serverPackets(context, world);
        context.getInput().pressKey(InputConstants.KEY_F8);
        settingsButton("spatialui.settings.more");
        var point = context.computeOnClient(client -> {
            var slider = SpatialScreenManager.INSTANCE.settings().children().stream()
                    .filter(net.minecraft.client.gui.components.AbstractSliderButton.class::isInstance)
                    .map(net.minecraft.client.gui.components.AbstractSliderButton.class::cast)
                    .filter(widget -> widget.getMessage().getString().startsWith(net.minecraft.network.chat.Component.translatable("spatialui.container_scale").getString()))
                    .findFirst().orElseThrow();
            return raw(client, slider.getX() + 2, slider.getY() + 10);
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        check(context.computeOnClient(client -> InventoryConfig.get().containerScale) == 0.75, "container size slider reaches minimum");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(3);
        check(context.computeOnClient(client -> SpatialScreenManager.INSTANCE.transform().scale()) < size - 0.2,
                "container size setting changes actual rendered panel");
        context.runOnClient(client -> InventoryConfig.get().containerScale = 1.35);
        context.waitTicks(3);
    }

    private void largeChest() {
        command("setblock 0 200 3 minecraft:chest[facing=north,type=left] replace");
        command("setblock 1 200 3 minecraft:chest[facing=north,type=right] replace");
        command("item replace block 0 200 3 container.0 with minecraft:diamond 32");
        context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(STATION), Direction.NORTH, STATION, false)));
        context.waitFor(client -> client.player.containerMenu.slots.size() == 90);
        context.waitTicks(4);
        context.runOnClient(client -> {
            var screen = dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            var bounds = (ContainerScreenAccessor) screen;
            var t = SpatialScreenManager.INSTANCE.transform();
            check(bounds.spatial$height() * t.scale() > screen.height * 0.85, "large chest uses readable window height");
            check(t.centerY() - t.halfHeight(bounds.spatial$width(), bounds.spatial$height()) >= 0
                    && t.centerY() + t.halfHeight(bounds.spatial$width(), bounds.spatial$height()) <= screen.height,
                    "large chest stays within window");
            TestWorldAccess.clearChat(client); TestWorldAccess.clearToasts(client);
        });
        hoverInventory();
        context.takeScreenshot("spatialui-large-chest-readable");
        screens++;
        close();
        command("setblock 1 200 3 minecraft:air");
    }

    private void headTracking(String mode) {
        float[] original = context.computeOnClient(client -> new float[]{client.player.getYRot(), client.player.getXRot(),
                client.player.yHeadRot, client.player.yBodyRot});
        moveInPanel(0.1, 0.15);
        context.waitTicks(10);
        float[] left = headPose();
        context.takeScreenshot("spatialui-head-" + mode + "-upper-left");
        moveInPanel(0.9, 0.85);
        context.waitTicks(10);
        float[] right = headPose();
        context.takeScreenshot("spatialui-head-" + mode + "-lower-right");
        check(right[0] > left[0] + 25, mode + " rendered head turns horizontally with cursor");
        check(right[1] > left[1] + 30, mode + " rendered head follows cursor vertically");
        check(Math.abs(left[2] - right[2]) < 0.01, mode + " cursor does not rotate rendered body");
        context.runOnClient(client -> {
            check(client.player.getYRot() == original[0] && client.player.getXRot() == original[1]
                    && client.player.yHeadRot == original[2] && client.player.yBodyRot == original[3],
                    "rendered gaze never modifies actual player orientation");
        });
        var serverAngles = world.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            return new float[]{player.getYRot(), player.getXRot()};
        });
        check(serverAngles[0] == original[0] && serverAngles[1] == original[1], "gaze is not sent to server");
        context.getInput().pressKey(InputConstants.KEY_F8);
        settingsButton("spatialui.settings.more");
        settingsButton("spatialui.settings.entrance");
        settingsButton("spatialui.settings.look");
        settingsButton("spatialui.look.enabled");
        check(!context.computeOnClient(client -> InventoryConfig.get().playerLookAtInventory), "head tracking toggle works");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(2);
        float[] disabled = headPose();
        check(Math.abs(disabled[0]) < 0.01 && Math.abs(disabled[1] - original[1]) < 0.01,
                "disabling head tracking immediately restores vanilla pose");
        context.runOnClient(client -> InventoryConfig.get().playerLookAtInventory = true);
    }

    private float[] headPose() {
        return context.computeOnClient(client -> {
            var renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
            var pose = (net.minecraft.client.renderer.entity.state.AvatarRenderState) renderer.createRenderState(client.player, 1F);
            return new float[]{pose.yRot, pose.xRot, pose.bodyRot};
        });
    }

    private void moveInPanel(double x, double y) {
        var point = context.computeOnClient(client -> {
            var bounds = (ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            return projected(client, bounds.spatial$left() + bounds.spatial$width() * x,
                    bounds.spatial$top() + bounds.spatial$height() * y);
        });
        context.getInput().setCursorPos(point[0], point[1]);
    }

    private void settingsButton(String key) {
        var point = context.computeOnClient(client -> {
            var target = SpatialScreenManager.INSTANCE.settings() != null ? SpatialScreenManager.INSTANCE.settings()
                    : dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            var button = target.children().stream()
                    .filter(net.minecraft.client.gui.components.Button.class::isInstance)
                    .map(net.minecraft.client.gui.components.Button.class::cast)
                    .filter(widget -> widget.getMessage().getString().startsWith(net.minecraft.network.chat.Component.translatable(key).getString()))
                    .findFirst().orElseThrow();
            return raw(client, button.getX() + button.getWidth() / 2.0, button.getY() + 10);
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(1);
    }

    private void anvil() {
        clickSlot(findItem(Items.IRON_SWORD), 0); clickSlot(0, 0);
        var point = context.computeOnClient(client -> {
            var edit = dev.transparentinventory.client.compat.MinecraftAccess.screen(client).children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
            return projected(client, edit.getX() + 15, edit.getY() + 7);
        });
        context.getInput().setCursorPos(point[0], point[1]); context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        // Fabric 0.159's TestInput currently constructs KeyEvents with modifiers=0.
        // Invoke the same native callback with the actual Ctrl bit for this chord.
        context.runOnClient(client -> ((KeyboardTestAccess) client.keyboardHandler).spatialtest$key(client.getWindow().handle(), InputConstants.PRESS,
                new net.minecraft.client.input.KeyEvent(InputConstants.KEY_A, 0, InputConstants.MOD_CONTROL)));
        context.getInput().typeChars("Spatial Blade");
        context.waitTicks(4);
        context.waitFor(client -> client.player.containerMenu.getSlot(2).getItem().getHoverName().getString().equals("Spatial Blade"));
        clickSlot(2, 0);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().getHoverName().getString()).equals("Spatial Blade"), "anvil name, cost and output stay vanilla");
        int empty = context.computeOnClient(client -> client.player.containerMenu.slots.stream().filter(slot -> slot.index >= 3 && slot.getItem().isEmpty()).findFirst().orElseThrow().index);
        clickSlot(empty, 0);
    }

    private void enchanting() {
        clickSlot(findItem(Items.DIAMOND_SWORD), 0); clickSlot(0, 0);
        clickSlot(findItem(Items.LAPIS_LAZULI), 0); clickSlot(1, 0);
        var point = context.computeOnClient(client -> {
            var bounds = (ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            return projected(client, bounds.spatial$left() + 100, bounds.spatial$top() + 23);
        });
        context.waitTicks(4);
        context.getInput().setCursorPos(point[0], point[1]); context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> client.player.containerMenu.getSlot(0).getItem().isEnchanted());
        check(context.computeOnClient(client -> client.player.containerMenu.getSlot(1).getItem().getCount()) < 64, "enchanting consumes lapis");
    }

    private void merchant() {
        command("setblock 0 200 3 minecraft:air");
        command("give @a minecraft:emerald 64");
        command("summon minecraft:villager 0.5 200 3.5 {NoAI:1b}");
        int entityId = world.getServer().computeOnServer(server -> {
            var level = server.overworld();
            var villager = level.getEntitiesOfClass(Villager.class, new AABB(STATION).inflate(4)).getFirst();
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION,
                    net.minecraft.resources.Identifier.withDefaultNamespace("farmer"));
            villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), key));
            var offers = new MerchantOffers();
            for (int index = 0; index < 10; index++) offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 3), 100, 1, 0F));
            villager.setOffers(offers);
            return villager.getId();
        });
        context.waitFor(client -> client.level.getEntity(entityId) != null);
        context.runOnClient(client -> {
            var entity = client.level.getEntity(entityId);
            client.gameMode.interact(client.player, entity, new EntityHitResult(entity), InteractionHand.MAIN_HAND);
        });
        context.waitForScreen(MerchantScreen.class); context.waitTicks(8);
        assertSpatial(AnchorType.ENTITY);
        var point = context.computeOnClient(client -> {
            var bounds = (ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            return projected(client, bounds.spatial$left() + 40, bounds.spatial$top() + 25);
        });
        context.getInput().setCursorPos(point[0], point[1]); context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> client.player.containerMenu.getSlot(2).getItem().is(Items.BREAD));
        clickSlot(2, 0);
        check(context.computeOnClient(client -> client.player.containerMenu.getCarried().getCount()) == 3, "villager trade output");
        int empty = context.computeOnClient(client -> client.player.containerMenu.slots.stream().filter(slot -> slot.index >= 3 && slot.getItem().isEmpty()).findFirst().orElseThrow().index);
        clickSlot(empty, 0);
        context.getInput().setCursorPos(point[0], point[1]); context.getInput().scroll(-3); context.waitTicks(3);
        context.runOnClient(client -> {
            // Test-only inspection of vanilla scroll state; no production reflection.
            try {
                var scroll = MerchantScreen.class.getDeclaredField("scrollOff");
                scroll.setAccessible(true);
                check(scroll.getInt(dev.transparentinventory.client.compat.MinecraftAccess.screen(client)) > 0, "villager list scroll uses remapped cursor");
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        });
        hoverInventory(); context.takeScreenshot("spatialui-merchant"); screens++; close();
        world.getServer().runOnServer(server -> server.overworld().getEntity(entityId).discard());
    }

    private void horse() {
        command("summon minecraft:horse 0.5 200 3.5 {NoAI:1b}");
        int entityId = world.getServer().computeOnServer(server -> {
            var horse = server.overworld().getEntitiesOfClass(Horse.class, new AABB(STATION).inflate(4)).getFirst();
            horse.setTamed(true); horse.setOwner(server.getPlayerList().getPlayers().getFirst());
            return horse.getId();
        });
        context.waitFor(client -> client.level.getEntity(entityId) != null);
        context.getInput().holdShift(); context.waitTicks(3);
        context.runOnClient(client -> {
            var entity = client.level.getEntity(entityId);
            client.gameMode.interact(client.player, entity, new EntityHitResult(entity), InteractionHand.MAIN_HAND);
        });
        context.waitForScreen(HorseInventoryScreen.class); context.getInput().releaseShift(); context.waitTicks(8);
        assertSpatial(AnchorType.ENTITY);
        hoverInventory(); context.takeScreenshot("spatialui-horse"); screens++; close();
    }

    private int findItem(net.minecraft.world.item.Item item) {
        context.waitFor(client -> client.player.containerMenu.slots.stream().anyMatch(slot -> slot.getItem().is(item)));
        return context.computeOnClient(client -> client.player.containerMenu.slots.stream().filter(slot -> slot.getItem().is(item)).findFirst().orElseThrow().index);
    }

    private void hoverInventory() {
        int slot = context.computeOnClient(client -> client.player.containerMenu.slots.stream().filter(s -> s.hasItem()).findFirst().orElseThrow().index);
        moveToSlot(slot);
        check(context.computeOnClient(client -> ((ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client)).spatial$hoveredSlot().index) == slot, "hover maps back to vanilla slot");
        context.waitTicks(3);
    }

    private void moveToSlot(int slotIndex) {
        var point = context.computeOnClient(client -> {
            var bounds = (ContainerScreenAccessor) dev.transparentinventory.client.compat.MinecraftAccess.screen(client);
            var slot = client.player.containerMenu.getSlot(slotIndex);
            return projected(client, bounds.spatial$left() + slot.x + 8, bounds.spatial$top() + slot.y + 8);
        });
        context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(2);
    }
    private void clickSlot(int slot, int button) { moveToSlot(slot); context.getInput().pressMouse(button == 0 ? InputConstants.MOUSE_BUTTON_LEFT : InputConstants.MOUSE_BUTTON_RIGHT); context.waitTicks(2); }
    private void shiftClickSlot(int slot) {
        moveToSlot(slot);
        context.runOnClient(client -> {
            var access = (MouseTestAccess) client.mouseHandler;
            var button = new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_SHIFT);
            access.spatialtest$button(client.getWindow().handle(), button, InputConstants.PRESS);
            access.spatialtest$button(client.getWindow().handle(), button, InputConstants.RELEASE);
        });
        context.waitTicks(2);
    }
    private static double[] projected(net.minecraft.client.Minecraft client, double x, double y) {
        var transform = SpatialScreenManager.INSTANCE.transform();
        return raw(client, transform.screenX(x, y), transform.screenY(x, y));
    }
    private static double[] raw(net.minecraft.client.Minecraft client, double x, double y) {
        var w = client.getWindow();
        return new double[]{x * w.getScreenWidth() / w.getGuiScaledWidth(), y * w.getScreenHeight() / w.getGuiScaledHeight()};
    }
    private void close() {
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitFor(client -> dev.transparentinventory.client.compat.MinecraftAccess.screen(client) == null);
        context.waitFor(client -> client.options.getCameraType() == CameraType.FIRST_PERSON);
        check(context.computeOnClient(client -> client.options.getCameraType()) == CameraType.FIRST_PERSON, "camera restored on close");
    }
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
