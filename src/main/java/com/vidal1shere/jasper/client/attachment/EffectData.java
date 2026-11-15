package com.vidal1shere.jasper.client.attachment;


import java.util.ArrayList;
import java.util.List;

// ClientEffects moves the effects that persist out of it when their animation ends
public final class EffectData {
    private final List<BoundEffect> running = new ArrayList<>();

    public List<BoundEffect> running() {
        return running;
    }
}
