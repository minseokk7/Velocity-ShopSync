package me.minseok.shopsystem.velocity.messaging;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import me.minseok.shopsystem.velocity.VelocityShopSystem;
import me.minseok.shopsystem.velocity.ShopConfigManager;
import org.slf4j.Logger;

public class PluginMessageListener {

    private final ProxyServer server;
    private final ShopConfigManager configManager;
    private final Logger logger;

    public PluginMessageListener(ProxyServer server, ShopConfigManager configManager, Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        logger.info("DEBUG: Received message on channel: " + event.getIdentifier().getId());

        if (!event.getIdentifier().getId().equals("shopsystem:sync")) {
            return;
        }

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        // Handle incoming messages from backend servers
        if (event.getSource() instanceof ServerConnection) {
            ServerConnection connection = (ServerConnection) event.getSource();
            @SuppressWarnings("null")
            com.google.common.io.ByteArrayDataInput in = com.google.common.io.ByteStreams.newDataInput(event.getData());
            String subChannel = in.readUTF();

            if (subChannel.equals("REQUEST_CONFIG")) {
                logger.info("Received config request from " + connection.getServerInfo().getName());
                configManager.sendConfigToServer(connection.getServer());
            } else if (subChannel.equals("PRICE_UPDATE")) {
                String item = in.readUTF();
                double buyPrice = in.readDouble();
                double sellPrice = in.readDouble();
                String sourceServer = "unknown";
                try {
                    sourceServer = in.readUTF();
                } catch (Exception e) {
                    // Legacy or missing source
                }

                logger.info(
                        "Velocity received PRICE_UPDATE for " + item + " from " + connection.getServerInfo().getName()
                                + " (Source: " + sourceServer + ")");

                // Forward to all other servers
                com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                out.writeUTF("PRICE_UPDATE");
                out.writeUTF(item);
                out.writeDouble(buyPrice);
                out.writeDouble(sellPrice);
                out.writeUTF(sourceServer);
                byte[] data = out.toByteArray();

                for (com.velocitypowered.api.proxy.server.RegisteredServer serverNode : server
                        .getAllServers()) {
                    if (!serverNode.getServerInfo().getName().equals(connection.getServerInfo().getName())) {
                        if (!serverNode.getPlayersConnected().isEmpty()) {
                            serverNode.sendPluginMessage(VelocityShopSystem.CHANNEL, data);
                            logger
                                    .info("Velocity forwarding PRICE_UPDATE to "
                                            + serverNode.getServerInfo().getName());
                        } else {
                            logger.info("Skipping " + serverNode.getServerInfo().getName() + " (no players)");
                        }
                    }
                }
            } else if (subChannel.equals("REQUEST_GLOBAL_RELOAD")) {
                logger.info("Received global reload request from " + connection.getServerInfo().getName());
                configManager.sendConfigToAll();
            }
        }
    }
}
