package me.minseok.shopsystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.minseok.shopsystem.velocity.VelocityShopSystem;
import me.minseok.shopsystem.velocity.ShopConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ShopManagerCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ShopConfigManager configManager;

    public ShopManagerCommand(ProxyServer server, ShopConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(invocation);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> handleSetPrice(invocation, args);
            case "sync" -> handleSync(invocation);
            case "reload" -> handleReload(invocation);
            case "status" -> handleStatus(invocation);
            default -> sendHelp(invocation);
        }
    }

    private void handleSetPrice(Invocation invocation, String[] args) {
        if (args.length < 3) {
            invocation.source()
                    .sendMessage(Component.text("사용법: /shopmanager set <아이템> <구매가> [판매가]", NamedTextColor.RED));
            return;
        }

        String item = args[1].toUpperCase();
        double buyPrice;
        double sellPrice;

        try {
            buyPrice = Double.parseDouble(args[2]);
            sellPrice = args.length > 3 ? Double.parseDouble(args[3]) : buyPrice * 0.25;
        } catch (NumberFormatException e) {
            invocation.source().sendMessage(Component.text("유효하지 않은 가격 형식입니다", NamedTextColor.RED));
            return;
        }

        // Send update to all servers
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PRICE_UPDATE");
        out.writeUTF(item);
        out.writeDouble(buyPrice);
        out.writeDouble(sellPrice);

        int serverCount = 0;
        for (RegisteredServer serverNode : server.getAllServers()) {
            if (!serverNode.getPlayersConnected().isEmpty()) {
                serverNode.sendPluginMessage(VelocityShopSystem.CHANNEL, out.toByteArray());
                serverCount++;
            }
        }

        invocation.source()
                .sendMessage(Component.text(serverCount + "개 서버에 가격 업데이트를 전송했습니다:", NamedTextColor.GREEN));
        invocation.source().sendMessage(Component.text("아이템: " + item, NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("구매가: " + buyPrice, NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("판매가: " + sellPrice, NamedTextColor.YELLOW));
    }

    private void handleSync(Invocation invocation) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("SYNC_REQUEST");

        for (RegisteredServer serverNode : server.getAllServers()) {
            if (!serverNode.getPlayersConnected().isEmpty()) {
                serverNode.sendPluginMessage(VelocityShopSystem.CHANNEL, out.toByteArray());
            }
        }

        invocation.source().sendMessage(Component.text("모든 서버에 동기화 요청을 전송했습니다", NamedTextColor.GREEN));
    }

    private void handleReload(Invocation invocation) {
        configManager.sendConfigToAll();
        invocation.source()
                .sendMessage(Component.text("설정을 리로드하고 모든 서버에 전송했습니다", NamedTextColor.GREEN));
    }

    private void handleStatus(Invocation invocation) {
        invocation.source().sendMessage(Component.text("=== 서버 상태 ===", NamedTextColor.GOLD));

        for (RegisteredServer serverNode : server.getAllServers()) {
            int playerCount = serverNode.getPlayersConnected().size();
            String status = playerCount > 0 ? "온라인" : "플레이어 없음";
            NamedTextColor color = playerCount > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY;

            invocation.source()
                    .sendMessage(Component.text("- " + serverNode.getServerInfo().getName() + ": " + status, color));
        }
    }

    private void sendHelp(Invocation invocation) {
        invocation.source().sendMessage(Component.text("=== 상점 관리 명령어 ===", NamedTextColor.GOLD));
        invocation.source().sendMessage(Component.text("/shopmanager set <아이템> <구매가> [판매가]", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/shopmanager sync", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/shopmanager reload", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/shopmanager status", NamedTextColor.YELLOW));
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("shopmanager.admin");
    }
}
