package com.vidal1shere.jasper.client.effect.screen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.client.animation.keyframe.Easing;
import com.vidal1shere.jasper.client.effect.EffectCodecs;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.effect.spec.Fade;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Intensity in degrees, frequency in oscillations per second, times in game ticks
@OnlyIn(Dist.CLIENT)
public record CameraShakeEffect(float intensity, float frequency, float duration, Fade fadeIn, Fade fadeOut) implements ScreenEffect {
    public static final MapCodec<CameraShakeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EffectCodecs.number(0.0F, Float.MAX_VALUE).optionalFieldOf("intensity", 1.0F).forGetter(CameraShakeEffect::intensity),
            EffectCodecs.number(0.0F, Float.MAX_VALUE).optionalFieldOf("frequency", 15.0F).forGetter(CameraShakeEffect::frequency),
            EffectCodecs.number(0.0F, Float.MAX_VALUE).optionalFieldOf("duration", 0.0F).forGetter(CameraShakeEffect::duration),
            Fade.CODEC.optionalFieldOf("fade_in", Fade.NONE).forGetter(CameraShakeEffect::fadeIn),
            Fade.CODEC.optionalFieldOf("fade_out", new Fade(6.0F, Easing.EASE_OUT_QUAD, 0.0F)).forGetter(CameraShakeEffect::fadeOut)
    ).apply(instance, CameraShakeEffect::new));

    @Override
    public boolean enabled() {
        return JasperClientConfig.CAMERA_SHAKE.get();
    }

    @Override
    public PlayingEffect play(EffectContext context) {
        return CameraShakes.start(this, context.bound());
    }
}
