package com.vidal1shere.jasper.client.item;

import com.vidal1shere.jasper.client.animation.AnimationKeyframe;
import com.vidal1shere.jasper.client.animation.KeyframeAnimation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public record ItemAnimation(KeyframeAnimation animation, Vector3f pivot, boolean debug) {
    public static final String BONE = "item";
    public static final Vector3f NO_PIVOT = new Vector3f();

    public ItemAnimation(KeyframeAnimation animation, Vector3f pivot) {
        this(animation, pivot, false);
    }

    public static AnimationKeyframe position(float time, float x, float y, float z) {
        return AnimationKeyframe.of(time, x, -y, z);
    }

    public static AnimationKeyframe rotation(float time, float xDegrees, float yDegrees, float zDegrees) {
        return AnimationKeyframe.of(time, xDegrees * Mth.DEG_TO_RAD, yDegrees * Mth.DEG_TO_RAD, zDegrees * Mth.DEG_TO_RAD);
    }
}
