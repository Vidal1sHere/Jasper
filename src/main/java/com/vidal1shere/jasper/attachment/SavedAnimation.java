package com.vidal1shere.jasper.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.PlayOptions;
import com.vidal1shere.jasper.layer.PlayView;
import com.vidal1shere.jasper.util.EnumNames;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.Optional;

// Keeps the original start tick: the game time goes on across saves, so a finished play once stays finished
public record SavedAnimation(AnimationTarget target, ResourceLocation layer, ResourceLocation animation, long startTick, PlayOptions options) {
    private static final Codec<AnimationTarget> TARGET_CODEC = EnumNames.codec(AnimationTarget.class);
    private static final Codec<InteractionHand> HAND_CODEC = EnumNames.codec(InteractionHand.class);
    private static final Codec<PlayView> VIEW_CODEC = EnumNames.codec(PlayView.class);
    private static final MapCodec<PlayOptions> OPTIONS_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(PlayOptions::speed),
            HAND_CODEC.optionalFieldOf("hand", InteractionHand.MAIN_HAND).forGetter(PlayOptions::hand),
            Codec.BOOL.optionalFieldOf("loop", false).forGetter(PlayOptions::loop),
            ResourceLocation.CODEC.optionalFieldOf("first_person").forGetter(options -> Optional.ofNullable(options.firstPerson())),
            VIEW_CODEC.optionalFieldOf("view", PlayView.ALL).forGetter(PlayOptions::view)
    ).apply(instance, (speed, hand, loop, firstPerson, view) -> new PlayOptions(speed, hand, loop, firstPerson.orElse(null), view)));

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
