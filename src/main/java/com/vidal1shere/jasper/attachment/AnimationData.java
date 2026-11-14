package com.vidal1shere.jasper.attachment;

import com.vidal1shere.jasper.attachment.animation.AnimationSlot;
import com.vidal1shere.jasper.attachment.animation.PlayingAnimation;

import java.util.HashMap;
import java.util.Map;

public final class AnimationData {
    private final Map<AnimationSlot, PlayingAnimation> playing = new HashMap<>();

    public Map<AnimationSlot, PlayingAnimation> playing() {
        return playing;
    }
}
