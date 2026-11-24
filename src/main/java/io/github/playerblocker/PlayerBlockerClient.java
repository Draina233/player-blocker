package io.github.playerblocker;

import io.github.playerblocker.command.PlayerBlockerCommand;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.gui.Screens;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint.
 *
 * <p>Responsibility split: {@link BlocklistManager} owns "who is hidden for which channel",
 * mixins only ask it questions. The manager never touches Minecraft classes, which keeps the
 * decision logic unit-testable and independent of the render pipeline du jour.</p>
 */
public class PlayerBlockerClient implements ClientModInitializer {

    public static final String MOD_ID = "playerblocker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        BlocklistManager.get();
        PlayerBlockerCommand.register();
        ClientTickEvents.END_CLIENT_TICK.register(Screens::tick);
        ClientTickEvents.END_CLIENT_TICK.register(PlayerBlockerClient::refreshScope);
        LOGGER.info("[playerblocker] ready");
    }

    /**
     * Keeps the active scope in sync with the server being played, so an entry created on one
     * server does not silently hide a same-named player elsewhere.
     */
    private static void refreshScope(Minecraft client) {
        BlocklistManager manager = BlocklistManager.get();
        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            manager.setScope("global");
            return;
        }
        ServerData data = connection.getServerData();
        if (data == null || data.ip == null || data.ip.isBlank()) {
            manager.setScope("global");
            return;
        }
        manager.setScope("server:" + data.ip);
    }
}
