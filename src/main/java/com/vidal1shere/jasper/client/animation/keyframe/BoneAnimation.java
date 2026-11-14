package com.vidal1shere.jasper.client.animation.keyframe;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BoneAnimation(AnimationKeyframe[] position, AnimationKeyframe[] rotation, AnimationKeyframe[] scale) {
    public static final AnimationKeyframe[] NONE = new AnimationKeyframe[0];
}
