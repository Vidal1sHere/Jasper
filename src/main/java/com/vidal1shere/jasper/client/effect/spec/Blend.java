package com.vidal1shere.jasper.client.effect.spec;

import com.mojang.serialization.Codec;
import com.vidal1shere.jasper.util.EnumNames;
import net.minecraft.client.renderer.RenderStateShard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum Blend {
    ADDITIVE,
    TRANSLUCENT;

    public static final Codec<Blend> CODEC = EnumNames.codec(Blend.class);

    public RenderStateShard.TransparencyStateShard transparency() {
        return this == ADDITIVE ? RenderStateShard.LIGHTNING_TRANSPARENCY : RenderStateShard.TRANSLUCENT_TRANSPARENCY;
    }
}
