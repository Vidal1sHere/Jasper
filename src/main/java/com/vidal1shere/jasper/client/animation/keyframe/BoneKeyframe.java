package com.vidal1shere.jasper.client.animation.keyframe;

import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.client.molang.MolangValue;
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

    public Vector3f evaluate(MolangContext context, Vector3f out) {
        return out.set(x.get(context), y.get(context), z.get(context));
    }
}
