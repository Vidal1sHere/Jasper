package com.vidal1shere.jasper.client.effect.screen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.effect.EffectCodecs;
import com.vidal1shere.jasper.client.effect.spec.Fade;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Game ticks, since vanilla computes the FOV once per tick and smooths it between them
@OnlyIn(Dist.CLIENT)
public record FovEffect(float amount, float duration, Fade fadeIn, Fade fadeOut) implements ScreenEffect {
    public static final MapCodec<FovEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EffectCodecs.number(-Float.MAX_VALUE, Float.MAX_VALUE).optionalFieldOf("amount", -0.1F).forGetter(FovEffect::amount),
            EffectCodecs.number(0.0F, Float.MAX_VALUE).optionalFieldOf("duration", 4.0F).forGetter(FovEffect::duration),
            Fade.CODEC.optionalFieldOf("fade_in", Fade.of(1.0F)).forGetter(FovEffect::fadeIn),
            Fade.CODEC.optionalFieldOf("fade_out", Fade.of(4.0F)).forGetter(FovEffect::fadeOut)
    ).apply(instance, FovEffect::new));

    @Override
    public boolean enabled() {
        return JasperClientConfig.FOV.get();
    }

    @Override
    public PlayingEffect play(EffectContext context) {
        return FovModifiers.start(this, context.bound());
    }
}
