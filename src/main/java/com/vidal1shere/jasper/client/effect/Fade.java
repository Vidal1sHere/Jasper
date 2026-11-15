package com.vidal1shere.jasper.client.effect;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.animation.keyframe.Easing;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

// How an effect comes in or goes away, in the time unit of that effect: a length alone, or {"duration", "easing", "argument"}
@OnlyIn(Dist.CLIENT)
public record Fade(float duration, Easing easing, float argument) {
    public static final Fade NONE = new Fade(0.0F, Easing.LINEAR, 0.0F);

    private static final Codec<Float> DURATION = Codec.floatRange(0.0F, Float.MAX_VALUE);
    private static final Codec<Easing> EASING = Codec.STRING.comapFlatMap(name -> {
        Easing easing = Easing.byName(name);
        return easing == null ? DataResult.error(() -> "Unknown easing '" + name + "', expected one of " + Easing.names()) : DataResult.success(easing);
    }, Easing::id);
    private static final Codec<Fade> FULL = RecordCodecBuilder.create(instance -> instance.group(
            DURATION.fieldOf("duration").forGetter(Fade::duration),
            EASING.optionalFieldOf("easing", Easing.LINEAR).forGetter(Fade::easing),
            Codec.FLOAT.optionalFieldOf("argument").forGetter(fade -> fade.easing.usesArgument() ? Optional.of(fade.argument) : Optional.empty())
    ).apply(instance, (duration, easing, argument) -> new Fade(duration, easing, argument.orElse(easing.defaultArgument()))));

    public static final Codec<Fade> CODEC = Codec.either(DURATION, FULL).xmap(
            either -> either.map(duration -> new Fade(duration, Easing.LINEAR, 0.0F), fade -> fade),
            fade -> fade.easing == Easing.LINEAR ? Either.left(fade.duration) : Either.right(fade)
    );

    public static Fade of(float duration) {
        return new Fade(duration, Easing.LINEAR, 0.0F);
    }

    // From 0 to 1 over the fade, eased; a fade with no length is done at once
    public float progress(double elapsed) {
        if (duration <= 0.0F) {
            return 1.0F;
        }
        return easing.apply((float) Mth.clamp(elapsed / duration, 0.0, 1.0), argument);
    }
}
