package io.github.playerblocker.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Set;

/**
 * Enables the optional-module mixins only when the mod they target is actually installed.
 *
 * <p>Three of our channels live inside third-party mods (Xaero radar, Jade crosshair tooltip,
 * Simple Voice Chat audio). Their mixins must not be applied when those mods are absent, or the
 * game dies with "target class not found" before it even reaches the title screen. This is the
 * supported way to express "soft dependency" at the bytecode layer.</p>
 */
public class PlayerBlockerMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".XaeroRadarMixin")) {
            return loaded("xaerominimap", "xaerominimapfabric");
        }
        if (mixinClassName.endsWith(".JadeRayTraceMixin")) {
            return loaded("jade");
        }
        if (mixinClassName.endsWith(".VoiceChatMixin")) {
            return loaded("voicechat");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean loaded(String... ids) {
        for (String id : ids) {
            if (FabricLoader.getInstance().isModLoaded(id)) {
                return true;
            }
        }
        return false;
    }
}
