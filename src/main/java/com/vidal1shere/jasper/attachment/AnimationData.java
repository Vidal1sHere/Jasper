package com.vidal1shere.jasper.attachment;

import com.vidal1shere.jasper.layer.LayerKey;

import java.util.HashMap;
import java.util.Map;

public final class AnimationData {
    private final Map<LayerKey, PlayingAnimation> playing = new HashMap<>();

    public Map<LayerKey, PlayingAnimation> playing() {
        return playing;
    }
}
