package io.github.playerblocker.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.playerblocker.config.BlockEntry;
import io.github.playerblocker.config.BlocklistConfig;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import io.github.playerblocker.gui.AddPlayerScreen;
import io.github.playerblocker.gui.ConfigScreen;
import io.github.playerblocker.gui.Screens;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/**
 * Client commands. Everything is resolved against the players actually present on the server,
 * so no Mojang API call is ever needed — the UUID we store is exactly the one the server uses,
 * which is also correct on offline-mode servers.
 *
 * <p>Scope defaults to {@code global}: blocking someone is meant to hold wherever you meet them.
 * {@code here} narrows a single entry to the current server, for the rare case of two servers
 * using the same name for different people.</p>
 */
public final class PlayerBlockerCommand {

    private PlayerBlockerCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("pb")
                        .then(ClientCommands.literal("add")
                                // Bare "/pb add" opens the picker; with an argument it blocks by name.
                                .executes(ctx -> openAddScreen())
                                .then(ClientCommands.argument("player", StringArgumentType.word())
                                        .suggests(PlayerSuggestions.ONLINE)
                                        .executes(ctx -> add(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"), false))
                                        .then(ClientCommands.literal("global")
                                                .executes(ctx -> add(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"), false)))
                                        .then(ClientCommands.literal("here")
                                                .executes(ctx -> add(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"), true)))))
                        .then(ClientCommands.literal("remove")
                                .executes(ctx -> gui(ctx.getSource()))
                                .then(ClientCommands.argument("player", StringArgumentType.word())
                                        .suggests(PlayerSuggestions.BLOCKED)
                                        .executes(ctx -> remove(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player")))))
                        .then(ClientCommands.literal("scope")
                                .then(ClientCommands.argument("player", StringArgumentType.word())
                                        .suggests(PlayerSuggestions.BLOCKED)
                                        .then(ClientCommands.literal("global")
                                                .executes(ctx -> scope(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"), false)))
                                        .then(ClientCommands.literal("here")
                                                .executes(ctx -> scope(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"), true)))))
                        .then(ClientCommands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(ClientCommands.literal("gui")
                                .executes(ctx -> gui(ctx.getSource())))
                        .then(ClientCommands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
                        .then(ClientCommands.literal("reveal")
                                .executes(ctx -> reveal(ctx.getSource())))
                        .then(ClientCommands.literal("reload")
                                .executes(ctx -> reload(ctx.getSource())))
                        .then(ClientCommands.literal("dryrun")
                                .executes(ctx -> dryRun(ctx.getSource())))
                        .then(ClientCommands.literal("debug")
                                .executes(ctx -> debug(ctx.getSource())))));
    }

    private static PlayerInfo findOnline(String name) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return null;
        }
        return connection.getPlayerInfoIgnoreCase(name);
    }

    private static int add(FabricClientCommandSource source, String name, boolean hereOnly) {
        PlayerInfo info = findOnline(name);
        if (info == null) {
            source.sendError(Component.literal("当前服务器没有叫 " + name + " 的在线玩家")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();
        UUID id = info.getProfile().id();
        String realName = info.getProfile().name();

        String scope = hereOnly ? manager.scope() : BlockEntry.SCOPE_GLOBAL;
        if (hereOnly && BlockEntry.SCOPE_GLOBAL.equals(scope)) {
            source.sendError(Component.literal("当前不在服务器里，无法使用 here；请改用 global")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (config.find(id.toString()).isPresent()) {
            source.sendFeedback(Component.literal(realName + " 已在屏蔽列表中，如需改作用域用 /pb scope")
                    .withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        config.addOrReplace(new BlockEntry(id.toString(), realName, scope));
        manager.rebuildInPlace();

        source.sendFeedback(Component.literal("已屏蔽 " + realName + "  " + scopeLabel(scope))
                .withStyle(ChatFormatting.GREEN));
        source.sendFeedback(Component.literal("模型/名牌/Tab/聊天/系统消息/音效/补全 默认开启；侧边栏默认关闭")
                .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int remove(FabricClientCommandSource source, String name) {
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();
        BlockEntry target = config.findByName(name).orElse(null);
        if (target == null) {
            PlayerInfo info = findOnline(name);
            if (info != null) {
                target = config.find(info.getProfile().id().toString()).orElse(null);
            }
        }
        if (target == null) {
            source.sendError(Component.literal("列表里没有 " + name).withStyle(ChatFormatting.RED));
            return 0;
        }
        String removedName = target.name;
        config.remove(target.uuid);
        manager.rebuildInPlace();
        source.sendFeedback(Component.literal("已取消屏蔽 " + removedName).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int scope(FabricClientCommandSource source, String name, boolean hereOnly) {
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();
        BlockEntry target = config.findByName(name).orElse(null);
        if (target == null) {
            PlayerInfo info = findOnline(name);
            if (info != null) {
                target = config.find(info.getProfile().id().toString()).orElse(null);
            }
        }
        if (target == null) {
            source.sendError(Component.literal("列表里没有 " + name).withStyle(ChatFormatting.RED));
            return 0;
        }
        String scope = hereOnly ? manager.scope() : BlockEntry.SCOPE_GLOBAL;
        if (hereOnly && BlockEntry.SCOPE_GLOBAL.equals(scope)) {
            source.sendError(Component.literal("当前不在服务器里，无法使用 here").withStyle(ChatFormatting.RED));
            return 0;
        }
        config.setScope(target.uuid, scope);
        manager.rebuildInPlace();
        source.sendFeedback(Component.literal(target.name + " 的作用域改为 " + scopeLabel(scope))
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int list(FabricClientCommandSource source) {
        BlocklistManager manager = BlocklistManager.get();
        List<BlockEntry> entries = manager.entriesForScope();
        if (entries.isEmpty()) {
            source.sendFeedback(Component.literal("屏蔽列表为空（当前服务器：" + manager.scope() + "）")
                    .withStyle(ChatFormatting.GRAY));
            return 1;
        }
        source.sendFeedback(Component.literal("屏蔽列表 (" + entries.size() + ")").withStyle(ChatFormatting.AQUA));
        for (BlockEntry entry : entries) {
            source.sendFeedback(Component.literal(" - " + entry.name + "  " + scopeLabel(entry.scope) + "  "
                            + entry.enabledTargets().size() + "/" + HideTarget.values().length + " 通道")
                    .withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int status(FabricClientCommandSource source) {
        BlocklistManager manager = BlocklistManager.get();
        int global = 0;
        int server = 0;
        for (BlockEntry entry : manager.config().entries) {
            if (entry.isServerScoped()) {
                server++;
            } else {
                global++;
            }
        }
        source.sendFeedback(Component.literal("当前服务器: " + manager.scope()).withStyle(ChatFormatting.AQUA));
        source.sendFeedback(Component.literal("条目: 全局 " + global + " / 限服 " + server)
                .withStyle(ChatFormatting.GRAY));
        source.sendFeedback(Component.literal("总开关: " + (manager.masterEnabled() ? "开" : "关")
                + " | dry-run: " + (manager.dryRun() ? "开" : "关")
                + " | 临时透视: " + (manager.revealOverride() ? "开" : "关")
                + " | debug: " + (manager.debug() ? "开" : "关")).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    /**
     * Screens are opened on the next tick, never inline: the chat screen tears itself down right
     * after the command returns, which would close whatever we set in the same frame.
     */
    private static int gui(FabricClientCommandSource source) {
        Screens.openNextTick(() -> new ConfigScreen(null));
        return 1;
    }

    private static int openAddScreen() {
        Screens.openNextTick(() -> new AddPlayerScreen(null));
        return 1;
    }

    private static int reveal(FabricClientCommandSource source) {
        BlocklistManager manager = BlocklistManager.get();
        boolean next = !manager.revealOverride();
        manager.setRevealOverride(next);
        source.sendFeedback(Component.literal(next ? "临时透视：已显示全部" : "临时透视：已恢复屏蔽")
                .withStyle(next ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        return 1;
    }

    private static int reload(FabricClientCommandSource source) {
        BlocklistManager.get().reload();
        source.sendFeedback(Component.literal("配置已重新读取").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int dryRun(FabricClientCommandSource source) {
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();
        config.settings.dryRun = !config.settings.dryRun;
        config.save();
        manager.rebuildInPlace();
        source.sendFeedback(Component.literal("dry-run: " + (manager.dryRun() ? "开（只记录不隐藏）" : "关"))
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    /** Verbose mode: prints every chat line and suggestion the filters look at. */
    private static int debug(FabricClientCommandSource source) {
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();
        config.settings.debug = !config.settings.debug;
        config.save();
        source.sendFeedback(Component.literal("debug 日志: " + (manager.debug() ? "开" : "关")
                        + "（输出到 logs/latest.log，前缀 [pb/debug]）")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static String scopeLabel(String scope) {
        if (scope == null || BlockEntry.SCOPE_GLOBAL.equals(scope)) {
            return "[全局]";
        }
        String server = scope.startsWith("server:") ? scope.substring("server:".length()) : scope;
        return "[仅 " + server + "]";
    }
}
