package com.vidal1shere.jasper.client.effect;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum Blend implements StringRepresentable {
    ADDITIVE("additive"),
    TRANSLUCENT("translucent");

    public static final Codec<Blend> CODEC = StringRepresentable.fromEnum(Blend::values);

    private final String name;

    Blend(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
