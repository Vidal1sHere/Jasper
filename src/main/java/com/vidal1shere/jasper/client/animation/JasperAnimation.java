package com.vidal1shere.jasper.client.animation;

import com.vidal1shere.jasper.client.animation.keyframe.BoneTimeline;
import com.vidal1shere.jasper.client.animation.locator.Locator;
import com.vidal1shere.jasper.client.effect.timeline.EffectTimeline;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Map;

// The pivot and debug fields only mean something for the item, the one kind with no model to take pivots from
@OnlyIn(Dist.CLIENT)
public record JasperAnimation(float length, LoopType loop, float blendTicks, BoneTimeline keyframes, BlendMode mode, Map<String, BlendMode> boneModes, Look look, Vector3f pivot, boolean debug, Map<String, Locator> locators, EffectTimeline effects) {
    public static final Vector3f NO_PIVOT = new Vector3f();
    public static final float DEFAULT_BLEND_TICKS = 2.0F;

    public JasperAnimation(float length, LoopType loop, BoneTimeline keyframes) {
        this(length, loop, DEFAULT_BLEND_TICKS, keyframes, BlendMode.REPLACE, Map.of(), Look.NONE, NO_PIVOT, false);
    }

    public JasperAnimation(float length, LoopType loop, float blendTicks, BoneTimeline keyframes, BlendMode mode, Map<String, BlendMode> boneModes, Look look, Vector3f pivot, boolean debug) {
        this(length, loop, blendTicks, keyframes, mode, boneModes, look, pivot, debug, Map.of(), EffectTimeline.EMPTY);
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
}
