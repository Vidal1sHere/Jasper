package com.vidal1shere.jasper.client.effect.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.Fade;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// Amount changes the FOV multiplier, -0.15 being a fully drawn bow; times are game ticks, since vanilla computes the FOV once per tick and smooths it between them
@OnlyIn(Dist.CLIENT)
public record FovEffect(float amount, float duration, Fade fadeIn, Fade fadeOut) implements AnimationEffect {
    public static final MapCodec<FovEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("amount", -0.1F).forGetter(FovEffect::amount),
            Codec.floatRange(0.0F, Float.MAX_VALUE).optionalFieldOf("duration", 4.0F).forGetter(FovEffect::duration),
            Fade.CODEC.optionalFieldOf("fade_in", Fade.of(1.0F)).forGetter(FovEffect::fadeIn),
            Fade.CODEC.optionalFieldOf("fade_out", Fade.of(4.0F)).forGetter(FovEffect::fadeOut)
    ).apply(instance, FovEffect::new));

    @Override
    public boolean visual() {
        return false;
    }

    @Override
    @Nullable
    public PlayingEffect start(EffectContext context) {
        if (context.entity() != Minecraft.getInstance().player || !JasperClientConfig.FOV.get()) {
            return null;
        }
        return FovModifiers.start(this, context.bound(), context.entity().level().getGameTime());
    }
}
