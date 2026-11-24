package io.github.playerblocker.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/**
 * Deferred screen opening.
 *
 * <p>Opening a screen straight from a client command does not work: {@code ChatScreen#keyPressed}
 * runs the command first and then calls {@code Gui.setScreen(null)} in the same frame, so the
 * screen we just installed is immediately torn down. The symptom is "the command works but
 * nothing opens" with no exception anywhere in the log.</p>
 *
 * <p>So instead of pushing the screen inline, stash a factory and install it on the next client
 * tick, after the chat screen has finished closing itself.</p>
 */
public final class Screens {

    private static Supplier<Screen> pending;

    private Screens() {
    }

    /** Schedules {@code factory} to be opened on the next client tick. */
    public static void openNextTick(Supplier<Screen> factory) {
        pending = factory;
    }

    /** Called once per client tick. */
    public static void tick(Minecraft client) {
        Supplier<Screen> next = pending;
        if (next == null) {
            return;
        }
        pending = null;
        Screen screen;
        try {
            screen = next.get();
        } catch (RuntimeException e) {
            // A broken screen factory must never take the client down mid-tick.
            io.github.playerblocker.PlayerBlockerClient.LOGGER.error(
                    "[playerblocker] could not build screen", e);
            return;
        }
        client.setScreenAndShow(screen);
    }
}
