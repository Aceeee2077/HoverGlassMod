package dev.transparentinventory.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Fabric test synchronization changed between the 26.1 and 26.2 APIs. */
final class TestWorldAccess {
    static void chunks(TestSingleplayerContext world) { world.getConnection().waitForChunksDownload(); }
    static void clientPackets(ClientGameTestContext context, TestSingleplayerContext world) { world.getConnection().waitForClientboundPackets(); }
    static void serverPackets(ClientGameTestContext context, TestSingleplayerContext world) { world.getConnection().waitForServerboundPackets(); }
}
