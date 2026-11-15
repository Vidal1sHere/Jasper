package com.vidal1shere.jasper.client.animation.locator;

import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Not animated when it only stands for a bone
@OnlyIn(Dist.CLIENT)
public record Locator(String parent, PartPose rest, boolean animated) {
    public static Locator bone(String bone) {
        return new Locator(bone, PartPose.ZERO, false);
    }

    public PartPose rest(boolean mirrored) {
        return mirrored ? PartPose.offset(-rest.x, rest.y, rest.z) : rest;
    }
}
