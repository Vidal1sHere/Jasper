package com.vidal1shere.jasper.client.animation.keyframe;

import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.client.molang.MolangValue;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public record BoneKeyframe(float time, MolangValue x, MolangValue y, MolangValue z, Easing easing, float easingArgument) {
    public static BoneKeyframe of(float time, float x, float y, float z) {
        return of(time, x, y, z, Easing.LINEAR);
    }

    public static BoneKeyframe of(float time, float x, float y, float z, Easing easing) {
        return new BoneKeyframe(time, MolangValue.constant(x), MolangValue.constant(y), MolangValue.constant(z), easing, easing.defaultArgument());
    }

    public static BoneKeyframe rotation(float time, float x, float y, float z) {
        return rotation(time, x, y, z, Easing.LINEAR);
    }

    public static BoneKeyframe rotation(float time, float x, float y, float z, Easing easing) {
        return of(time, x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD, easing);
    }

    public static BoneKeyframe position(float time, float x, float y, float z) {
        return position(time, x, y, z, Easing.LINEAR);
    }

    public static BoneKeyframe position(float time, float x, float y, float z, Easing easing) {
        return of(time, x, -y, z, easing);
    }

    public static BoneKeyframe scale(float time, float x, float y, float z) {
        return scale(time, x, y, z, Easing.LINEAR);
    }

    public static BoneKeyframe scale(float time, float x, float y, float z, Easing easing) {
        return of(time, x, y, z, easing);
    }

    public Vector3f evaluate(MolangContext context, Vector3f out) {
        return out.set(x.get(context), y.get(context), z.get(context));
    }
}
