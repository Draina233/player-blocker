package io.github.playerblocker.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.playerblocker.gui.ConfigScreen;

/**
 * Mod Menu hook: puts a "config" button on our entry in the in-game mod list.
 *
 * <p>This is a soft dependency. The class is only ever loaded when Mod Menu itself asks for the
 * {@code modmenu} entrypoint, so the mod still starts on a pack without Mod Menu installed.</p>
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Mod Menu passes the screen to return to on close; our screen already handles that.
        return ConfigScreen::new;
    }
}
