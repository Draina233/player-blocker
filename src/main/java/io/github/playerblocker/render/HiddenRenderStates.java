package io.github.playerblocker.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bridges the entity-based stage of the pipeline to the state-based stage.
 *
 * <p>In 26.2 an entity is converted to {@code EntityRenderState} by
 * {@code EntityRenderDispatcher.extractEntity}, and the actual draw call is
 * {@code EntityRenderer.submit(EntityRenderState, ...)} — the state no longer carries a
 * reference to the entity, so "should this be drawn?" has to be decided at extraction time and
 * remembered until submission.</p>
 *
 * <p>Only hidden players are ever inserted, so the map holds a handful of entries at most.
 * Keys are weak: if a state is extracted but culled before submission, it collects normally.</p>
 */
public final class HiddenRenderStates {

    private static final Map<EntityRenderState, Boolean> HIDDEN =
            Collections.synchronizedMap(new WeakHashMap<>());

    private HiddenRenderStates() {
    }

    /** Called every frame per extracted entity, so it also clears stale marks. */
    public static void set(EntityRenderState state, boolean hidden) {
        if (state == null) {
            return;
        }
        if (hidden) {
            HIDDEN.put(state, Boolean.TRUE);
        } else {
            HIDDEN.remove(state);
        }
    }

    public static boolean isHidden(EntityRenderState state) {
        return state != null && HIDDEN.containsKey(state);
    }
}
