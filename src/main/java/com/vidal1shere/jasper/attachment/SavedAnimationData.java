package com.vidal1shere.jasper.attachment;

import com.mojang.serialization.Codec;
import com.vidal1shere.jasper.attachment.savedanimation.SavedAnimation;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SavedAnimationData(List<SavedAnimation> animations) {
    public static final SavedAnimationData EMPTY = new SavedAnimationData(List.of());
    public static final Codec<SavedAnimationData> CODEC = SavedAnimation.CODEC.listOf().xmap(SavedAnimationData::new, SavedAnimationData::animations);

    public SavedAnimationData with(SavedAnimation animation) {
        List<SavedAnimation> kept = new ArrayList<>(without(animation.target(), animation.layer()).animations);
        kept.add(animation);
        return new SavedAnimationData(List.copyOf(kept));
    }

    public SavedAnimationData without(AnimationTarget target, ResourceLocation layer) {
        return new SavedAnimationData(animations.stream().filter(animation -> !animation.sameSlot(target, layer)).toList());
    }
}
