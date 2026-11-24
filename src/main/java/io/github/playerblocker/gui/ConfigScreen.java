package io.github.playerblocker.gui;

import io.github.playerblocker.config.BlockEntry;
import io.github.playerblocker.config.BlocklistConfig;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Blocklist editor.
 *
 * <p>Built from vanilla widgets only. Cloth Config would have been tidier, but it is another
 * third-party API to track across Minecraft versions, and this screen does three things: change
 * scope, toggle channels, delete. Buttons are enough.</p>
 *
 * <p>Reached from Mod Menu (the "config" button on our entry) or from {@code /pb gui}.</p>
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Player Blocker"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        BlocklistManager manager = BlocklistManager.get();
        BlocklistConfig config = manager.config();

        StringWidget title = new StringWidget(this.title, this.font);
        title.setPosition(this.width / 2 - 100, 12);
        addRenderableWidget(title);

        List<BlockEntry> entries = config.entries;
        int rowHeight = 26;
        int top = 40;
        int bottomBar = 62;
        int maxRows = Math.max(1, (this.height - top - bottomBar) / rowHeight);

        if (entries.isEmpty()) {
            StringWidget empty = new StringWidget(
                    Component.literal("还没有屏蔽任何人。点“添加”，或在聊天栏用 /pb add <名字>（支持 Tab 补全）"),
                    this.font);
            empty.setPosition(this.width / 2 - 200, top + 10);
            addRenderableWidget(empty);
        }

        for (int i = 0; i < entries.size() && i < maxRows; i++) {
            BlockEntry entry = entries.get(i);
            int y = top + i * rowHeight;

            // Name button doubles as the scope switch: [全局] <-> [限服]
            addRenderableWidget(Button.builder(
                            Component.literal(entry.name + (entry.isServerScoped() ? "  [限服]" : "  [全局]")),
                            button -> toggleScope(entry))
                    .bounds(this.width / 2 - 200, y, 170, 20)
                    .build());

            addRenderableWidget(Button.builder(
                            Component.literal("通道 " + entry.enabledTargets().size() + "/"
                                    + HideTarget.values().length),
                            button -> this.minecraft.setScreenAndShow(new ChannelScreen(this, entry)))
                    .bounds(this.width / 2 - 20, y, 100, 20)
                    .build());

            addRenderableWidget(Button.builder(
                            Component.literal("删除"),
                            button -> {
                                config.remove(entry.uuid);
                                BlocklistManager.get().rebuildInPlace();
                                this.rebuildWidgets();
                            })
                    .bounds(this.width / 2 + 90, y, 60, 20)
                    .build());
        }

        if (entries.size() > maxRows) {
            StringWidget more = new StringWidget(
                    Component.literal("还有 " + (entries.size() - maxRows) + " 条未显示，可调大窗口或从文件编辑"),
                    this.font);
            more.setPosition(this.width / 2 - 150, top + maxRows * rowHeight + 4);
            addRenderableWidget(more);
        }

        int buttonWidth = 100;
        int gap = 6;
        int barWidth = 3 * buttonWidth + 2 * gap;
        int barX = this.width / 2 - barWidth / 2;
        int barY = this.height - 32;

        addRenderableWidget(Button.builder(Component.literal("添加"),
                        button -> this.minecraft.setScreenAndShow(new AddPlayerScreen(this)))
                .bounds(barX, barY, buttonWidth, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal(manager.revealOverride() ? "透视: 开" : "透视: 关"),
                        button -> {
                            manager.setRevealOverride(!manager.revealOverride());
                            this.rebuildWidgets();
                        })
                .bounds(barX + buttonWidth + gap, barY, buttonWidth, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> this.onClose())
                .bounds(barX + 2 * (buttonWidth + gap), barY, buttonWidth, 20)
                .build());
    }

    /** Global means "everywhere"; limiting an entry to the current server is the opt-in case. */
    private void toggleScope(BlockEntry entry) {
        entry.scope = entry.isServerScoped() ? BlockEntry.SCOPE_GLOBAL : BlocklistManager.get().scope();
        BlocklistManager.get().config().save();
        BlocklistManager.get().rebuildInPlace();
        this.rebuildWidgets();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }
}
