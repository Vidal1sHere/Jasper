package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationPlayback;
import com.vidal1shere.jasper.client.item.ItemAnimation;
import com.vidal1shere.jasper.client.loading.ItemAnimationLoader;
import net.minecraft.world.InteractionHand;

import java.util.EnumMap;
import java.util.Map;

public final class ItemAnimationData {
    private final Map<InteractionHand, AnimationPlayback<ItemAnimation>> playbacks = new EnumMap<>(InteractionHand.class);

    public Map<InteractionHand, AnimationPlayback<ItemAnimation>> playbacks() {
        return playbacks;
    }

    public AnimationPlayback<ItemAnimation> playback(InteractionHand hand) {
        return playbacks.computeIfAbsent(hand, key -> new AnimationPlayback<>(ItemAnimationLoader.INSTANCE, ItemAnimation::animation));
    }
}
