package dev.transparentinventory.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Older test API has no connection barrier; wait native server/client ticks. */
final class TestWorldAccess {
    static void chunks(TestSingleplayerContext world) { world.getClientLevel().waitForChunksDownload(); }
    static void clientPackets(ClientGameTestContext context, TestSingleplayerContext world) { context.waitTicks(3); }
    static void serverPackets(ClientGameTestContext context, TestSingleplayerContext world) { context.waitTicks(3); }
}
