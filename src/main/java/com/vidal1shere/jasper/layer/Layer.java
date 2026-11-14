package com.vidal1shere.jasper.layer;

import net.minecraft.resources.ResourceLocation;

// Layers apply from the lowest priority to the highest, each one over what the others left
public record Layer(ResourceLocation id, int priority, LayerPolicy policy) {
}
