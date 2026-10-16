package dev.transparentinventory.client.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** In 26.1.x Minecraft owns the current Screen directly. */
public final class MinecraftAccess {
    private MinecraftAccess() { }
    public static Screen screen(Minecraft client) { return client.screen; }
    public static void screen(Minecraft client, Screen screen) { client.setScreen(screen); }
    public static Camera camera(Minecraft client) { return client.gameRenderer.getMainCamera(); }
}
