package com.vidal1shere.jasper.client.effect.spec;

import com.mojang.serialization.Codec;
import com.vidal1shere.jasper.util.EnumNames;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum Light {
    FULL,
    WORLD;

    public static final Codec<Light> CODEC = EnumNames.codec(Light.class);
}
