package com.vidal1shere.jasper.layer;

import net.minecraft.resources.ResourceLocation;

public record Layer(ResourceLocation id, int priority, LayerPolicy policy) {
}
