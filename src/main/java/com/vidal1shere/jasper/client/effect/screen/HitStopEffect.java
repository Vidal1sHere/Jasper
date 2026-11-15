package com.vidal1shere.jasper.client.effect.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// Duration is milliseconds on the real clock; until and persist do not apply, since a frozen client never reaches an until
@OnlyIn(Dist.CLIENT)
public record HitStopEffect(float duration) implements AnimationEffect {
    public static final MapCodec<HitStopEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, Float.MAX_VALUE).optionalFieldOf("duration", 50.0F).forGetter(HitStopEffect::duration)
    ).apply(instance, HitStopEffect::new));

    @Override
    public boolean visual() {
        return false;
    }

    @Override
    @Nullable
    public PlayingEffect start(EffectContext context) {
        if (context.entity() == Minecraft.getInstance().player) {
            HitStops.start(duration);
        }
        return null;
    }
}
