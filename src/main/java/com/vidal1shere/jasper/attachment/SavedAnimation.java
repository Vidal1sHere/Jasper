package com.vidal1shere.jasper.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.Locale;

// Keeps the original start tick: the game time goes on across saves, so a finished play once stays finished
public record SavedAnimation(AnimationTarget target, ResourceLocation layer, ResourceLocation animation, long startTick, PlayOptions options) {
    private static final Codec<AnimationTarget> TARGET_CODEC = Codec.STRING.xmap(name -> AnimationTarget.valueOf(name.toUpperCase(Locale.ROOT)), target -> target.name().toLowerCase(Locale.ROOT));
    private static final Codec<InteractionHand> HAND_CODEC = Codec.STRING.xmap(name -> InteractionHand.valueOf(name.toUpperCase(Locale.ROOT)), hand -> hand.name().toLowerCase(Locale.ROOT));
    private static final MapCodec<PlayOptions> OPTIONS_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(PlayOptions::speed),
            HAND_CODEC.optionalFieldOf("hand", InteractionHand.MAIN_HAND).forGetter(PlayOptions::hand),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(PlayOptions::loop)
    ).apply(instance, PlayOptions::new));

    public static final Codec<SavedAnimation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TARGET_CODEC.optionalFieldOf("target", AnimationTarget.MODEL).forGetter(SavedAnimation::target),
            ResourceLocation.CODEC.fieldOf("layer").forGetter(SavedAnimation::layer),
            ResourceLocation.CODEC.fieldOf("animation").forGetter(SavedAnimation::animation),
            Codec.LONG.fieldOf("start_tick").forGetter(SavedAnimation::startTick),
            OPTIONS_CODEC.forGetter(SavedAnimation::options)
    ).apply(instance, SavedAnimation::new));

    public boolean sameKey(AnimationTarget target, ResourceLocation layer) {
        return this.target == target && this.layer.equals(layer);
    }
}
