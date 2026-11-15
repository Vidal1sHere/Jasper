package com.vidal1shere.jasper.client.effect.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public record SoundEffect(ResourceLocation sound, float volume, float pitch, Optional<SoundSource> source, boolean loop, int loops) implements AnimationEffect {
    private static final Codec<SoundSource> SOURCE = Codec.STRING.comapFlatMap(
            name -> Arrays.stream(SoundSource.values()).filter(source -> source.getName().equals(name)).findFirst().map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown sound source '" + name + "', expected one of " + Arrays.stream(SoundSource.values()).map(SoundSource::getName).toList())),
            SoundSource::getName
    );

    public static final MapCodec<SoundEffect> CODEC = RecordCodecBuilder.<SoundEffect>mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("sound").forGetter(SoundEffect::sound),
            Codec.floatRange(0.0F, Float.MAX_VALUE).optionalFieldOf("volume", 1.0F).forGetter(SoundEffect::volume),
            Codec.floatRange(0.0F, Float.MAX_VALUE).optionalFieldOf("pitch", 1.0F).forGetter(SoundEffect::pitch),
            SOURCE.optionalFieldOf("source").forGetter(SoundEffect::source),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(SoundEffect::loop),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("loops", 0).forGetter(SoundEffect::loops)
    ).apply(instance, SoundEffect::new)).validate(effect -> effect.loops > 0 && !effect.loop
            ? DataResult.error(() -> "loops needs loop to be true")
            : DataResult.success(effect));

    @Override
    public boolean visual() {
        return false;
    }

    @Override
    public PlayingEffect start(EffectContext context) {
        return new PlayingSound(this, source.orElseGet(() -> context.entity().getSoundSource()), context);
    }
}
