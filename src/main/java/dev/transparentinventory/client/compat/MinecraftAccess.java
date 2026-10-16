package dev.transparentinventory.client.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Version-specific access to GUI ownership and the main camera. */
public final class MinecraftAccess {
    private MinecraftAccess() { }
    public static Screen screen(Minecraft client) { return client.gui.screen(); }
    public static void screen(Minecraft client, Screen screen) { client.gui.setScreen(screen); }
    public static Camera camera(Minecraft client) { return client.gameRenderer.mainCamera(); }
}
