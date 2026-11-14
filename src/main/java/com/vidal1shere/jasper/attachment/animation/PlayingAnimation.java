package com.vidal1shere.jasper.attachment.animation;

import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

// The item is the one held in the hand when it started, for layers that end when it changes
public record PlayingAnimation(AnimationSlot slot, ResourceLocation animation, long startTick, PlayOptions options, Item item) {
}
