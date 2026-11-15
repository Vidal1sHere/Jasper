package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.attachment.effect.BoundEffect;

import java.util.ArrayList;
import java.util.List;

// The effects still bound to an animation of the entity; ClientEffects moves those that persist out of it
public final class EffectData {
    private final List<BoundEffect> running = new ArrayList<>();

    public List<BoundEffect> running() {
        return running;
    }
}
