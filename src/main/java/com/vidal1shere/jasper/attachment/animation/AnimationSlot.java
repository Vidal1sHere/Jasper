package com.vidal1shere.jasper.attachment.animation;

import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

// The item layer has one slot per channel and hand, the player and entity layers one per channel
public record AnimationSlot(AnimationLayer layer, Channel channel, @Nullable InteractionHand hand) {
    public static AnimationSlot of(AnimationLayer layer, Channel channel, InteractionHand hand) {
        return new AnimationSlot(layer, channel, layer == AnimationLayer.ITEM ? hand : null);
    }
}
