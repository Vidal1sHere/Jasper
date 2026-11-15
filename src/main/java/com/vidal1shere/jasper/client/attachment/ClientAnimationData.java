package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.LayerKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ClientAnimationData {
    private final Map<LayerKey, LayerState> tracks = new HashMap<>();

    public LayerState track(AnimationTarget target, Layer layer, JasperAnimationLoader loader) {
        return tracks.computeIfAbsent(new LayerKey(target, layer), key -> new LayerState(loader));
    }

    @Nullable
    public LayerState existing(AnimationTarget target, Layer layer) {
        return tracks.get(new LayerKey(target, layer));
    }

    public void forEach(BiConsumer<LayerKey, LayerState> action) {
        tracks.forEach(action);
    }
}
