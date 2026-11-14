package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.attachment.layer.LayerKey;
import com.vidal1shere.jasper.client.attachment.layer.LayerTrack;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.Layer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class LayerData {
    private final Map<LayerKey, LayerTrack> tracks = new HashMap<>();

    public LayerTrack track(AnimationTarget target, Layer layer, JasperAnimationLoader loader) {
        return tracks.computeIfAbsent(new LayerKey(target, layer), key -> new LayerTrack(loader));
    }

    @Nullable
    public LayerTrack existing(AnimationTarget target, Layer layer) {
        return tracks.get(new LayerKey(target, layer));
    }
}
