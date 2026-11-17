package com.vidal1shere.jasper.client.animation;

import com.vidal1shere.jasper.client.animation.keyframe.BoneKeyframe;
import com.vidal1shere.jasper.client.animation.keyframe.BoneTimeline;
import com.vidal1shere.jasper.client.animation.locator.Locator;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.timeline.EffectEntry;
import com.vidal1shere.jasper.client.effect.timeline.EffectTimeline;
import com.vidal1shere.jasper.client.molang.MolangValue;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The pivot only means something for the item, the one kind with no model to take pivots from
@OnlyIn(Dist.CLIENT)
public record JasperAnimation(float length, LoopType loop, float blendTicks, BoneTimeline keyframes, BlendMode mode, Map<String, BlendMode> boneModes, Look look, Vector3f pivot, Map<String, Locator> locators, EffectTimeline effects) {
    public static final Vector3f NO_PIVOT = new Vector3f();
    public static final float DEFAULT_BLEND_TICKS = 2.0F;

    public static Builder builder(LoopType loop) {
        return new Builder(loop);
    }

    public float localTime(float elapsedSeconds) {
        return switch (loop) {
            case LOOP -> length > 0.0F ? elapsedSeconds % length : 0.0F;
            case HOLD_ON_LAST_FRAME, PLAY_ONCE -> Math.min(elapsedSeconds, length);
        };
    }

    public BlendMode mode(String bone) {
        return boneModes.getOrDefault(bone, mode);
    }

    public enum LoopType {
        PLAY_ONCE,
        LOOP,
        HOLD_ON_LAST_FRAME
    }

    @OnlyIn(Dist.CLIENT)
    public record Look(boolean pitch, boolean yaw) {
        public static final Look NONE = new Look(false, false);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static final class Builder {
        private final LoopType loop;
        private final BoneTimeline.Builder keyframes = BoneTimeline.builder();
        private final Map<String, BlendMode> boneModes = new HashMap<>();
        private final Map<String, Locator> locators = new LinkedHashMap<>();
        private final List<EffectEntry> effects = new ArrayList<>();
        private float length = -1.0F;
        private float blendTicks = DEFAULT_BLEND_TICKS;
        private BlendMode mode = BlendMode.REPLACE;
        private Look look = Look.NONE;
        private Vector3f pivot = NO_PIVOT;

        private Builder(LoopType loop) {
            this.loop = loop;
        }

        // Without it the animation ends on its last keyframe
        public Builder length(float length) {
            this.length = length;
            return this;
        }

        public Builder blendTicks(float blendTicks) {
            this.blendTicks = blendTicks;
            return this;
        }

        public Builder mode(BlendMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder boneMode(String bone, BlendMode mode) {
            boneModes.put(bone, mode);
            return this;
        }

        public Builder look(Look look) {
            this.look = look;
            return this;
        }

        public Builder pivot(Vector3f pivot) {
            this.pivot = pivot;
            return this;
        }

        public Builder preAnimation(MolangValue preAnimation) {
            keyframes.preAnimation(preAnimation);
            return this;
        }

        public Builder position(String bone, BoneKeyframe... keyframes) {
            this.keyframes.position(bone, keyframes);
            return this;
        }

        public Builder rotation(String bone, BoneKeyframe... keyframes) {
            this.keyframes.rotation(bone, keyframes);
            return this;
        }

        public Builder scale(String bone, BoneKeyframe... keyframes) {
            this.keyframes.scale(bone, keyframes);
            return this;
        }

        public Builder locator(String name, String parent, Vector3f position) {
            locators.put(name, new Locator(parent, PartPose.offset(position.x, position.y, position.z), true));
            return this;
        }

        public Builder effect(float time, AnimationEffect effect) {
            return effect(time, effect, null, false, EffectEntry.NO_UNTIL, true);
        }

        // until is a later time of the animation, EffectEntry.UNTIL_END or EffectEntry.NO_UNTIL
        public Builder effect(float time, AnimationEffect effect, @Nullable String locator, boolean follow, float until, boolean persist) {
            effects.add(new EffectEntry(time, effect, null, locator, follow, until, persist));
            return this;
        }

        public JasperAnimation build() {
            BoneTimeline timeline = keyframes.build();
            float length = this.length >= 0.0F ? this.length : timeline.lastKeyframe();
            Map<String, Locator> all = new LinkedHashMap<>(locators);
            for (EffectEntry entry : effects) {
                if (entry.time() < 0.0F || entry.time() > length) {
                    throw new IllegalArgumentException("Effect at " + entry.time() + " is outside the animation length " + length);
                }
                if (!Float.isNaN(entry.until()) && !Float.isInfinite(entry.until()) && (entry.until() <= entry.time() || entry.until() > length)) {
                    throw new IllegalArgumentException("Effect at " + entry.time() + " has an until of " + entry.until() + ", which must be after it and at most " + length);
                }
                if (entry.locator() != null) {
                    all.putIfAbsent(entry.locator(), Locator.bone(entry.locator()));
                }
                entry.effect().locators().forEach(name -> all.putIfAbsent(name, Locator.bone(name)));
            }
            for (Map.Entry<String, Locator> entry : all.entrySet()) {
                String parent = entry.getValue().parent();
                if (parent.equals(entry.getKey())) {
                    continue;
                }
                for (int depth = 0; all.containsKey(parent) && !all.get(parent).parent().equals(parent); depth++) {
                    if (depth >= all.size()) {
                        throw new IllegalArgumentException("The parents of locator " + entry.getKey() + " make a cycle");
                    }
                    parent = all.get(parent).parent();
                }
            }
            List<EffectEntry> sorted = new ArrayList<>(effects);
            sorted.sort(Comparator.comparingDouble(EffectEntry::time));
            return new JasperAnimation(length, loop, blendTicks, timeline, mode, Map.copyOf(boneModes), look, pivot, Map.copyOf(all), new EffectTimeline(List.copyOf(sorted)));
        }
    }
}
