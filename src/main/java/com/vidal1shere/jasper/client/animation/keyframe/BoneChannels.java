package com.vidal1shere.jasper.client.animation.keyframe;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BoneChannels(BoneKeyframe[] position, BoneKeyframe[] rotation, BoneKeyframe[] scale) {
    public static final BoneKeyframe[] NONE = new BoneKeyframe[0];
}
