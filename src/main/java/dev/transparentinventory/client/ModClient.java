package dev.transparentinventory.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point. All menu logic remains vanilla; no server component is required.
 */
public final class ModClient implements ClientModInitializer {

    public static final String MOD_ID = "transparent_inventory";

    public static final Logger LOGGER = LoggerFactory.getLogger("TransparentInventory");

    @Override
    public void onInitializeClient() {
        ConfigLoader.load();
        LOGGER.info("[Hoverglass] Initialized. F8 opens transparency settings.");
    }
}
