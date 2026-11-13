package com.vidal1shere.jasper.attachment;

import com.vidal1shere.jasper.attachment.animation.PlayingAnimation;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class AnimationData {
    private final Map<InteractionHand, PlayingAnimation> items = new EnumMap<>(InteractionHand.class);
    @Nullable
    private PlayingAnimation player;

    public Map<InteractionHand, PlayingAnimation> items() {
        return items;
    }

    @Nullable
    public PlayingAnimation player() {
        return player;
    }

    public void setPlayer(@Nullable PlayingAnimation player) {
        this.player = player;
    }

    public boolean isEmpty() {
        return items.isEmpty() && player == null;
    }
}
