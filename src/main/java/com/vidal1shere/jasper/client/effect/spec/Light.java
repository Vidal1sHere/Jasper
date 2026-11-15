package com.vidal1shere.jasper.client.effect.spec;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum Light implements StringRepresentable {
    FULL("full"),
    WORLD("world");

    public static final Codec<Light> CODEC = StringRepresentable.fromEnum(Light::values);

    private final String name;

    Light(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
