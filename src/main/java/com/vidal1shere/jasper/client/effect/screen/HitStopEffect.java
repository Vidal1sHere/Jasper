package com.vidal1shere.jasper.client.effect.screen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.effect.EffectCodecs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// Until and persist do not apply: a frozen client never reaches an until
@OnlyIn(Dist.CLIENT)
public record HitStopEffect(float duration) implements ScreenEffect {
    public static final MapCodec<HitStopEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EffectCodecs.number(0.0F, Float.MAX_VALUE).optionalFieldOf("duration", 50.0F).forGetter(HitStopEffect::duration)
    ).apply(instance, HitStopEffect::new));

    @Override
    public boolean enabled() {
        return JasperClientConfig.HIT_STOP_SCALE.get() > 0.0;
    }

    @Override
    @Nullable
    public PlayingEffect play(EffectContext context) {
        HitStops.start(duration);
        return null;
    }
}
