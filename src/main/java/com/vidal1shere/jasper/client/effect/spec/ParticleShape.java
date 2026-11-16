package com.vidal1shere.jasper.client.effect.spec;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ParticleShape implements StringRepresentable {
    GAUSSIAN("gaussian"),
    ENTITY("entity");

    public static final Codec<ParticleShape> CODEC = StringRepresentable.fromEnum(ParticleShape::values);

    private final String name;

    ParticleShape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
