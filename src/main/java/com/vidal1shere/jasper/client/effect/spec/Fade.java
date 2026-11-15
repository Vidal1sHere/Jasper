package com.vidal1shere.jasper.client.effect.spec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.animation.keyframe.Easing;
import com.vidal1shere.jasper.client.effect.EffectCodecs;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

// In the time unit of the effect that uses it
@OnlyIn(Dist.CLIENT)
public record Fade(float duration, Easing easing, float argument) {
    public static final Fade NONE = new Fade(0.0F, Easing.LINEAR, 0.0F);

    private static final Codec<Float> DURATION = EffectCodecs.number(0.0F, Float.MAX_VALUE);
    private static final Codec<Fade> FULL = RecordCodecBuilder.create(instance -> instance.group(
            DURATION.fieldOf("duration").forGetter(Fade::duration),
            Easing.CODEC.optionalFieldOf("easing", Easing.LINEAR).forGetter(Fade::easing),
            EffectCodecs.number(-Float.MAX_VALUE, Float.MAX_VALUE).optionalFieldOf("argument").forGetter(fade -> fade.easing.usesArgument() ? Optional.of(fade.argument) : Optional.empty())
    ).apply(instance, (duration, easing, argument) -> new Fade(duration, easing, argument.orElse(easing.defaultArgument()))));

    public static final Codec<Fade> CODEC = Codec.either(DURATION, FULL).xmap(
            either -> either.map(duration -> new Fade(duration, Easing.LINEAR, 0.0F), fade -> fade),
            fade -> fade.easing == Easing.LINEAR ? Either.left(fade.duration) : Either.right(fade)
    );

    public static Fade of(float duration) {
        return new Fade(duration, Easing.LINEAR, 0.0F);
    }

    public float progress(double elapsed) {
        if (duration <= 0.0F) {
            return 1.0F;
        }
        return easing.apply((float) Mth.clamp(elapsed / duration, 0.0, 1.0), argument);
    }
}
