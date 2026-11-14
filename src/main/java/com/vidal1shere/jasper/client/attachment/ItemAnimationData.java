package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import net.minecraft.world.InteractionHand;

import java.util.EnumMap;
import java.util.Map;

public final class ItemAnimationData {
    private final Map<InteractionHand, AnimationTrack> tracks = new EnumMap<>(InteractionHand.class);
    private final Map<InteractionHand, AnimationTrack> overlays = new EnumMap<>(InteractionHand.class);

    public Map<InteractionHand, AnimationTrack> tracks() {
        return tracks;
    }

    public AnimationTrack track(InteractionHand hand) {
        return tracks.computeIfAbsent(hand, key -> new AnimationTrack(JasperAnimationLoader.ITEM));
    }

    public Map<InteractionHand, AnimationTrack> overlays() {
        return overlays;
    }

    public AnimationTrack overlay(InteractionHand hand) {
        return overlays.computeIfAbsent(hand, key -> new AnimationTrack(JasperAnimationLoader.ITEM));
    }
}
