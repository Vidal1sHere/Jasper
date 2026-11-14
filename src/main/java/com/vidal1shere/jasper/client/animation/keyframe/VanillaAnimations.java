package com.vidal1shere.jasper.client.animation.keyframe;

import com.vidal1shere.jasper.client.animation.BlendMode;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class VanillaAnimations {
    public static JasperAnimation convert(AnimationDefinition definition) {
        return convert(definition, JasperAnimation.Look.NONE);
    }

    public static JasperAnimation convert(AnimationDefinition definition, JasperAnimation.Look look) {
        return convert(definition, definition.looping() ? KeyframeAnimation.LoopType.LOOP : KeyframeAnimation.LoopType.PLAY_ONCE, look);
    }

    public static JasperAnimation convert(AnimationDefinition definition, KeyframeAnimation.LoopType loop, JasperAnimation.Look look) {
        KeyframeAnimation.Builder builder = KeyframeAnimation.builder(definition.lengthInSeconds()).loop(loop);
        for (Map.Entry<String, List<AnimationChannel>> bone : definition.boneAnimations().entrySet()) {
            for (AnimationChannel channel : bone.getValue()) {
                AnimationChannel.Target target = channel.target();
                // Vanilla stores the scale minus one, so an untouched channel is zero
                float offset = target == AnimationChannel.Targets.SCALE ? 1.0F : 0.0F;
                AnimationKeyframe[] keyframes = new AnimationKeyframe[channel.keyframes().length];
                for (int i = 0; i < keyframes.length; i++) {
                    keyframes[i] = keyframe(channel.keyframes()[i], offset);
                }
                if (target == AnimationChannel.Targets.POSITION) {
                    builder.position(bone.getKey(), keyframes);
                } else if (target == AnimationChannel.Targets.ROTATION) {
                    builder.rotation(bone.getKey(), keyframes);
                } else {
                    builder.scale(bone.getKey(), keyframes);
                }
            }
        }
        return new JasperAnimation(builder.build(), BlendMode.REPLACE, Map.of(), look, JasperAnimation.NO_PIVOT, false);
    }

    private static AnimationKeyframe keyframe(Keyframe keyframe, float offset) {
        Vector3f value = keyframe.target();
        Easing easing = keyframe.interpolation() == AnimationChannel.Interpolations.CATMULLROM ? Easing.CATMULLROM : Easing.LINEAR;
        return AnimationKeyframe.of(keyframe.timestamp(), value.x + offset, value.y + offset, value.z + offset, easing);
    }
}
