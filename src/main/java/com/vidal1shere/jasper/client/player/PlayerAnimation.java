package com.vidal1shere.jasper.client.player;

import com.vidal1shere.jasper.client.keyframe.AnimationKeyframe;
import com.vidal1shere.jasper.client.keyframe.KeyframeAnimation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public record PlayerAnimation(KeyframeAnimation animation, BlendMode mode, Map<String, BlendMode> boneModes) {
    public PlayerAnimation(KeyframeAnimation animation) {
        this(animation, BlendMode.REPLACE, Map.of());
    }

    public BlendMode mode(String bone) {
        return boneModes.getOrDefault(bone, mode);
    }

    public static AnimationKeyframe position(float time, float x, float y, float z) {
        return AnimationKeyframe.of(time, x, -y, z);
    }

    public static AnimationKeyframe rotation(float time, float xDegrees, float yDegrees, float zDegrees) {
        return AnimationKeyframe.of(time, xDegrees * Mth.DEG_TO_RAD, yDegrees * Mth.DEG_TO_RAD, zDegrees * Mth.DEG_TO_RAD);
    }

    @OnlyIn(Dist.CLIENT)
    public enum BlendMode {
        REPLACE,
        ADDITIVE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static BlendMode byName(String name) {
            for (BlendMode mode : values()) {
                if (mode.id().equals(name)) {
                    return mode;
                }
            }
            return null;
        }
    }
}
