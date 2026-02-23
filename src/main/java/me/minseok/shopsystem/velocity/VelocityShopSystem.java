package me.minseok.shopsystem.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.minseok.shopsystem.velocity.commands.ShopManagerCommand;
import me.minseok.shopsystem.velocity.messaging.PluginMessageListener;
import org.slf4j.Logger;

@Plugin(id = "shopsync", name = "Velocity-ShopSync", version = "1.0.1", description = "Centralized shop price synchronization across all servers", authors = {
        "minseok" })
public class VelocityShopSystem {

    private final ProxyServer server;
    private final Logger logger;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("shopsystem", "sync");

    private final ShopConfigManager configManager;

    @Inject
    public VelocityShopSystem(ProxyServer server, Logger logger,
            @com.velocitypowered.api.plugin.annotation.DataDirectory java.nio.file.Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.configManager = new ShopConfigManager(this, dataDirectory.toFile());
    }

    public ShopConfigManager getConfigManager() {
        return configManager;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Register messaging channel
        server.getChannelRegistrar().register(CHANNEL);

        // Register message listener
        server.getEventManager().register(this, new PluginMessageListener(server, configManager, logger));

        // Register commands
        CommandManager commandManager = server.getCommandManager();
        commandManager.register(
                commandManager.metaBuilder("shopmanager").build(),
                new ShopManagerCommand(server, configManager));

        logger.info("VelocityShopSystem has been enabled!");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
