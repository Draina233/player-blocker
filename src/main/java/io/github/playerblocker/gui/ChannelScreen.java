package io.github.playerblocker.gui;

import io.github.playerblocker.config.BlockEntry;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Per-channel switches for one blocked player.
 *
 * <p>Every channel is independent on purpose: the usual setup is "hide his body and name, but
 * still let his messages through", which only works if the decisions are not welded together.</p>
 *
 * <p>The grid is laid out from the current screen width instead of a fixed column count — at the
 * default GUI scale a fixed 6-column grid is wider than the window and pushes buttons off-screen.</p>
 */
public class ChannelScreen extends Screen {

    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    private final Screen parent;
    private final BlockEntry entry;

    public ChannelScreen(Screen parent, BlockEntry entry) {
        super(Component.literal("通道 - " + entry.name));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        StringWidget title = new StringWidget(Component.literal(entry.name + " 的屏蔽通道"), this.font);
        title.setPosition(this.width / 2 - 60, 12);
        addRenderableWidget(title);

        HideTarget[] targets = HideTarget.values();
        int columns = Math.max(1, (this.width - 24 + GAP) / (BUTTON_WIDTH + GAP));
        int gridWidth = columns * (BUTTON_WIDTH + GAP) - GAP;
        int startX = this.width / 2 - gridWidth / 2;
        int startY = 40;

        for (int i = 0; i < targets.length; i++) {
            HideTarget target = targets[i];
            int x = startX + (i % columns) * (BUTTON_WIDTH + GAP);
            int y = startY + (i / columns) * (BUTTON_HEIGHT + GAP);
            addRenderableWidget(Button.builder(label(target), button -> {
                        entry.set(target, !entry.enabled(target));
                        BlocklistManager.get().config().save();
                        BlocklistManager.get().rebuildInPlace();
                        this.rebuildWidgets();
                    })
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        int barWidth = 2 * 120 + GAP;
        int barX = this.width / 2 - barWidth / 2;
        int barY = this.height - 32;

        addRenderableWidget(Button.builder(Component.literal("全部开启"), button -> {
                    for (HideTarget target : targets) {
                        entry.set(target, true);
                    }
                    BlocklistManager.get().config().save();
                    BlocklistManager.get().rebuildInPlace();
                    this.rebuildWidgets();
                })
                .bounds(barX, barY, 120, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("返回"), button -> this.onClose())
                .bounds(barX + 120 + GAP, barY, 120, 20)
                .build());
    }

    private Component label(HideTarget target) {
        String name = switch (target) {
            case MODEL -> "模型";
            case NAMETAG -> "名牌";
            case TAB -> "Tab 列表";
            case CHAT -> "玩家聊天";
            case SOUND -> "音效";
            case PARTICLES -> "粒子";
            case SUGGESTIONS -> "命令补全";
            case VOICE -> "语音";
            case MINIMAP -> "小地图";
            case TOOLTIP -> "准星提示";
            case OUTLINE -> "描边/影子";
            case SYSTEM -> "系统消息";
            case SIDEBAR -> "侧边栏";
        };
        return Component.literal((entry.enabled(target) ? "✔ " : "✘ ") + name);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }
}
