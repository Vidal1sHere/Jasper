package com.vidal1shere.jasper.client.effect.afterimage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.Blend;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.Fade;
import com.vidal1shere.jasper.client.effect.Light;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

// Game ticks
@OnlyIn(Dist.CLIENT)
public record AfterimageEffect(List<String> bones, TextColor color, float alpha, Fade fadeIn, float duration, Fade fadeOut, Optional<Float> interval,
                               boolean texture, Blend blend, Light light) implements AnimationEffect {
    private static final Codec<Float> TICKS = Codec.floatRange(0.0F, Float.MAX_VALUE);

    public static final MapCodec<AfterimageEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("bones", List.of()).forGetter(AfterimageEffect::bones),
            TextColor.CODEC.optionalFieldOf("color", TextColor.fromLegacyFormat(ChatFormatting.WHITE)).forGetter(AfterimageEffect::color),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("alpha", 0.5F).forGetter(AfterimageEffect::alpha),
            Fade.CODEC.optionalFieldOf("fade_in", Fade.NONE).forGetter(AfterimageEffect::fadeIn),
            TICKS.optionalFieldOf("duration", 2.0F).forGetter(AfterimageEffect::duration),
            Fade.CODEC.optionalFieldOf("fade_out", Fade.of(6.0F)).forGetter(AfterimageEffect::fadeOut),
            Codec.floatRange(0.05F, Float.MAX_VALUE).optionalFieldOf("interval").forGetter(AfterimageEffect::interval),
            Codec.BOOL.optionalFieldOf("texture", false).forGetter(AfterimageEffect::texture),
            Blend.CODEC.optionalFieldOf("blend", Blend.TRANSLUCENT).forGetter(AfterimageEffect::blend),
            Light.CODEC.optionalFieldOf("light", Light.FULL).forGetter(AfterimageEffect::light)
    ).apply(instance, AfterimageEffect::new));

    @Override
    @Nullable
    public PlayingEffect start(EffectContext context) {
        if (context.target() != AnimationTarget.MODEL || !(context.entity() instanceof LivingEntity)) {
            return null;
        }
        return Afterimages.start(this, context);
    }

    float lifetime() {
        return fadeIn.duration() + duration + fadeOut.duration();
    }

    int argb(float age) {
        float strength;
        if (age < fadeIn.duration()) {
            strength = fadeIn.progress(age);
        } else if (age < fadeIn.duration() + duration) {
            strength = 1.0F;
        } else {
            strength = 1.0F - fadeOut.progress(age - fadeIn.duration() - duration);
        }
        return FastColor.ARGB32.color(Math.round(alpha * strength * 255.0F), color.getValue());
    }
}
