package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.effect.timeline.EffectHandle;

import java.util.ArrayList;
import java.util.List;

public final class EffectData {
    private final List<EffectHandle> running = new ArrayList<>();

    public List<EffectHandle> running() {
        return running;
    }
}
