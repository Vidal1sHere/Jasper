package com.vidal1shere.jasper.client.effect.screen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.effect.EffectCodecs;
import com.vidal1shere.jasper.client.effect.spec.Fade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Milliseconds on the real clock, so it keeps fading while a hit stop freezes the game
@OnlyIn(Dist.CLIENT)
public record FlashEffect(TextColor color, float opacity, float duration, Fade fadeIn, Fade fadeOut) implements ScreenEffect {
    public static final MapCodec<FlashEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TextColor.CODEC.optionalFieldOf("color", TextColor.fromLegacyFormat(ChatFormatting.WHITE)).forGetter(FlashEffect::color),
            EffectCodecs.number(0.0F, 1.0F).optionalFieldOf("opacity", 0.5F).forGetter(FlashEffect::opacity),
            EffectCodecs.number(0.0F, Float.MAX_VALUE).optionalFieldOf("duration", 50.0F).forGetter(FlashEffect::duration),
            Fade.CODEC.optionalFieldOf("fade_in", Fade.NONE).forGetter(FlashEffect::fadeIn),
            Fade.CODEC.optionalFieldOf("fade_out", Fade.of(150.0F)).forGetter(FlashEffect::fadeOut)
    ).apply(instance, FlashEffect::new));

    @Override
    public boolean enabled() {
        return JasperClientConfig.FLASH.get();
    }

    @Override
    public PlayingEffect play(EffectContext context) {
        return ScreenFlashes.start(this, context.bound());
    }
}
