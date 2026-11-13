package com.vidal1shere.jasper.attachment.animation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;

public record PlayingAnimation(AnimationLayer layer, InteractionHand hand, ResourceLocation animation, long startTick, float speed, Item item) {
}
