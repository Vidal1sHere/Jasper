package com.vidal1shere.jasper.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record EntityOverlayData(ResourceLocation animation, float speed) {
    public static final Codec<EntityOverlayData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("animation").forGetter(EntityOverlayData::animation),
            Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(EntityOverlayData::speed)
    ).apply(instance, EntityOverlayData::new));
}
