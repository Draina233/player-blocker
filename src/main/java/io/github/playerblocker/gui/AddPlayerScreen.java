package io.github.playerblocker.gui;

import io.github.playerblocker.config.BlockEntry;
import io.github.playerblocker.config.BlocklistConfig;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.util.OnlinePlayers;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/**
 * Pick someone from the current server to block.
 *
 * <p>The list comes from {@link OnlinePlayers} — the connection's player map, not
 * {@code ClientLevel#players()}. The level only contains tracked entities, so the picker used to
 * silently omit everyone who happened to not be nearby. The map is unfiltered as well, so
 * already-blocked players still show up (marked) instead of becoming impossible to manage.</p>
 */
public class AddPlayerScreen extends Screen {

    private final Screen parent;
    /** Zero-based page index; survives re-init so paging does not jump back to the first page. */
    private int page;

    public AddPlayerScreen(Screen parent) {
        super(Component.literal("添加玩家"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();

        List<PlayerInfo> players = OnlinePlayers.all();

        StringWidget title = new StringWidget(
                Component.literal("服务器在线玩家 " + players.size() + " 人（点击屏蔽，默认全局）"), this.font);
        title.setPosition(this.width / 2 - 140, 12);
        addRenderableWidget(title);

        if (players.isEmpty()) {
            StringWidget empty = new StringWidget(
                    Component.literal(this.minecraft.getConnection() == null ? "还没进入服务器" : "服务器没有在线玩家"),
                    this.font);
            empty.setPosition(this.width / 2 - 60, 60);
            addRenderableWidget(empty);
        }

        int buttonWidth = 120;
        int buttonHeight = 20;
        int gap = 4;
        // Columns come from the screen width: a fixed count overflows at the default GUI scale.
        int columns = Math.max(1, (this.width - 24 + gap) / (buttonWidth + gap));
        int gridWidth = columns * (buttonWidth + gap) - gap;
        int startX = this.width / 2 - gridWidth / 2;
        int startY = 40;
        // Reserve room for the pager row and the back button, otherwise the last row lands on them.
        int maxRows = Math.max(1, (this.height - startY - 88) / (buttonHeight + gap));
        int perPage = columns * maxRows;
        int totalPages = Math.max(1, (players.size() + perPage - 1) / perPage);
        if (this.page >= totalPages) {
            this.page = totalPages - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }

        int from = this.page * perPage;
        int to = Math.min(players.size(), from + perPage);
        for (int i = from; i < to; i++) {
            PlayerInfo info = players.get(i);
            UUID id = info.getProfile().id();
            String name = OnlinePlayers.nameOf(info);
            if (name == null) {
                continue;
            }
            boolean listed = config.find(id.toString()).isPresent();

            int slot = i - from;
            int x = startX + (slot % columns) * (buttonWidth + gap);
            int y = startY + (slot / columns) * (buttonHeight + gap);

            addRenderableWidget(Button.builder(
                            Component.literal((listed ? "✔ " : "") + name),
                            button -> {
                                add(id, name);
                                this.minecraft.setScreenAndShow(new ConfigScreen(this.parent));
                            })
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build());
        }

        if (totalPages > 1) {
            StringWidget pager = new StringWidget(
                    Component.literal((this.page + 1) + " / " + totalPages), this.font);
            pager.setPosition(this.width / 2 - 20, this.height - 52);
            addRenderableWidget(pager);

            addRenderableWidget(Button.builder(Component.literal("←"), button -> turnPage(-1))
                    .bounds(this.width / 2 - 64, this.height - 56, 36, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("→"), button -> turnPage(1))
                    .bounds(this.width / 2 + 30, this.height - 56, 36, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("返回"), button -> this.onClose())
                .bounds(this.width / 2 - 60, this.height - 32, 120, 20)
                .build());
    }

    private void turnPage(int delta) {
        this.page += delta;
        this.clearWidgets();
        this.init();
    }

    private void add(UUID id, String name) {
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();
        if (config.find(id.toString()).isPresent()) {
            return;
        }
        config.addOrReplace(new BlockEntry(id.toString(), name, BlockEntry.SCOPE_GLOBAL));
        manager.rebuildInPlace();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }
}
