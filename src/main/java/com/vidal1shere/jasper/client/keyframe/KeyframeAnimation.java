package com.vidal1shere.jasper.client.keyframe;

import com.vidal1shere.jasper.client.molang.MolangValue;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public record KeyframeAnimation(float lengthInSeconds, LoopType loop, float blendTicks, Map<String, BoneAnimation> bones, MolangValue preAnimation) {
    public static final float DEFAULT_BLEND_TICKS = 2.0F;

    public enum LoopType {
        PLAY_ONCE,
        LOOP,
        HOLD_ON_LAST_FRAME
    }

    public boolean finished(float elapsedSeconds) {
        return loop == LoopType.PLAY_ONCE && elapsedSeconds >= lengthInSeconds;
    }

    public float localTime(float elapsedSeconds) {
        return switch (loop) {
            case LOOP -> lengthInSeconds > 0.0F ? elapsedSeconds % lengthInSeconds : 0.0F;
            case HOLD_ON_LAST_FRAME, PLAY_ONCE -> Math.min(elapsedSeconds, lengthInSeconds);
        };
    }

    public static Builder builder(float lengthInSeconds) {
        return new Builder(lengthInSeconds);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final float length;
        private final Map<String, List<AnimationKeyframe>[]> bones = new LinkedHashMap<>();
        private LoopType loop = LoopType.PLAY_ONCE;
        private float blendTicks = DEFAULT_BLEND_TICKS;
        private MolangValue preAnimation = MolangValue.ZERO;

        private Builder(float length) {
            this.length = length;
        }

        public Builder loop(LoopType loop) {
            this.loop = loop;
            return this;
        }

        public Builder looping() {
            return loop(LoopType.LOOP);
        }

        public Builder holding() {
            return loop(LoopType.HOLD_ON_LAST_FRAME);
        }

        public Builder blendTicks(float blendTicks) {
            this.blendTicks = blendTicks;
            return this;
        }

        public Builder preAnimation(MolangValue preAnimation) {
            this.preAnimation = preAnimation;
            return this;
        }

        public Builder position(String bone, AnimationKeyframe... keyframes) {
            return add(bone, 0, keyframes);
        }

        public Builder rotation(String bone, AnimationKeyframe... keyframes) {
            return add(bone, 1, keyframes);
        }

        public Builder scale(String bone, AnimationKeyframe... keyframes) {
            return add(bone, 2, keyframes);
        }

        @SuppressWarnings("unchecked")
        private Builder add(String bone, int channel, AnimationKeyframe... keyframes) {
            List<AnimationKeyframe>[] channels = bones.computeIfAbsent(bone, key -> new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()});
            channels[channel].addAll(List.of(keyframes));
            return this;
        }

        public KeyframeAnimation build() {
            Map<String, BoneAnimation> built = new LinkedHashMap<>();
            bones.forEach((bone, channels) -> built.put(bone, new BoneAnimation(sorted(channels[0]), sorted(channels[1]), sorted(channels[2]))));
            return new KeyframeAnimation(length, loop, blendTicks, Map.copyOf(built), preAnimation);
        }

        private static AnimationKeyframe[] sorted(List<AnimationKeyframe> keyframes) {
            if (keyframes.isEmpty()) {
                return BoneAnimation.NONE;
            }
            return keyframes.stream().sorted(Comparator.comparingDouble(AnimationKeyframe::time)).toArray(AnimationKeyframe[]::new);
        }
    }
}
