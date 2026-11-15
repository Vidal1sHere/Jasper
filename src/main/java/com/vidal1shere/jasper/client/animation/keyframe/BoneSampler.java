package com.vidal1shere.jasper.client.animation.keyframe;

import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.client.molang.MolangQuery;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class BoneSampler {
    private static final Vector3f P0 = new Vector3f();
    private static final Vector3f P1 = new Vector3f();
    private static final Vector3f P2 = new Vector3f();
    private static final Vector3f P3 = new Vector3f();

    public static void evaluate(JasperAnimation animation, float elapsedSeconds, MolangContext context, AnimationPose pose) {
        pose.reset();
        context.resetVariables();
        context.set(MolangQuery.ANIM_TIME, elapsedSeconds);
        animation.keyframes().preAnimation().get(context);
        float time = animation.localTime(elapsedSeconds);
        for (Map.Entry<String, BoneChannels> entry : animation.keyframes().bones().entrySet()) {
            BoneChannels bone = entry.getValue();
            BonePose target = pose.get(entry.getKey());
            if (bone.position().length > 0) {
                sample(bone.position(), time, context, target.position);
                target.hasPosition = true;
            }
            if (bone.rotation().length > 0) {
                sample(bone.rotation(), time, context, target.rotation);
                target.hasRotation = true;
            }
            if (bone.scale().length > 0) {
                sample(bone.scale(), time, context, target.scale);
                target.hasScale = true;
            }
        }
    }

    private static void sample(BoneKeyframe[] keyframes, float time, MolangContext context, Vector3f out) {
        int last = keyframes.length - 1;
        int from = 0;
        while (from < last && keyframes[from + 1].time() <= time) {
            from++;
        }
        int to = Math.min(from + 1, last);
        BoneKeyframe start = keyframes[from];
        BoneKeyframe end = keyframes[to];
        if (from == to || time <= start.time()) {
            start.evaluate(context, out);
            return;
        }
        float span = end.time() - start.time();
        float progress = span <= 0.0F ? 1.0F : Mth.clamp((time - start.time()) / span, 0.0F, 1.0F);
        Easing easing = end.easing();
        if (easing == Easing.STEP) {
            (progress >= 1.0F ? end : start).evaluate(context, out);
            return;
        }
        if (easing == Easing.CATMULLROM) {
            keyframes[Math.max(0, from - 1)].evaluate(context, P0);
            start.evaluate(context, P1);
            end.evaluate(context, P2);
            keyframes[Math.min(last, to + 1)].evaluate(context, P3);
            out.set(
                    Mth.catmullrom(progress, P0.x, P1.x, P2.x, P3.x),
                    Mth.catmullrom(progress, P0.y, P1.y, P2.y, P3.y),
                    Mth.catmullrom(progress, P0.z, P1.z, P2.z, P3.z)
            );
            return;
        }
        start.evaluate(context, P1);
        end.evaluate(context, P2);
        P1.lerp(P2, easing.apply(progress, end.easingArgument()), out);
    }
}
