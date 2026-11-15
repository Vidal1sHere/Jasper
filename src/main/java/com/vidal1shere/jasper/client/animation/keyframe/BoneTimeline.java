package com.vidal1shere.jasper.client.animation.keyframe;

import com.vidal1shere.jasper.client.molang.MolangValue;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public record BoneTimeline(Map<String, BoneChannels> bones, MolangValue preAnimation) {
    public float lastKeyframe() {
        float last = 0.0F;
        for (BoneChannels bone : bones.values()) {
            last = Math.max(last, Math.max(last(bone.position()), Math.max(last(bone.rotation()), last(bone.scale()))));
        }
        return last;
    }

    private static float last(BoneKeyframe[] keyframes) {
        return keyframes.length == 0 ? 0.0F : keyframes[keyframes.length - 1].time();
    }

    public static Builder builder() {
        return new Builder();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Map<String, List<BoneKeyframe>[]> bones = new LinkedHashMap<>();
        private MolangValue preAnimation = MolangValue.ZERO;

        public Builder preAnimation(MolangValue preAnimation) {
            this.preAnimation = preAnimation;
            return this;
        }

        public Builder position(String bone, BoneKeyframe... keyframes) {
            return add(bone, 0, keyframes);
        }

        public Builder rotation(String bone, BoneKeyframe... keyframes) {
            return add(bone, 1, keyframes);
        }

        public Builder scale(String bone, BoneKeyframe... keyframes) {
            return add(bone, 2, keyframes);
        }

        @SuppressWarnings("unchecked")
        private Builder add(String bone, int channel, BoneKeyframe... keyframes) {
            List<BoneKeyframe>[] channels = bones.computeIfAbsent(bone, key -> new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()});
            channels[channel].addAll(List.of(keyframes));
            return this;
        }

        public BoneTimeline build() {
            Map<String, BoneChannels> built = new LinkedHashMap<>();
            bones.forEach((bone, channels) -> built.put(bone, new BoneChannels(sorted(channels[0]), sorted(channels[1]), sorted(channels[2]))));
            return new BoneTimeline(Map.copyOf(built), preAnimation);
        }

        private static BoneKeyframe[] sorted(List<BoneKeyframe> keyframes) {
            if (keyframes.isEmpty()) {
                return BoneChannels.NONE;
            }
            return keyframes.stream().sorted(Comparator.comparingDouble(BoneKeyframe::time)).toArray(BoneKeyframe[]::new);
        }
    }
}
