package com.vidal1shere.jasper.attachment;

import com.vidal1shere.jasper.layer.LayerKey;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public record PlayingAnimation(LayerKey key, ResourceLocation animation, long startTick, PlayOptions options, Item item) {
}
