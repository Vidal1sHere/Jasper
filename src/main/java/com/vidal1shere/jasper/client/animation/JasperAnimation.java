package com.vidal1shere.jasper.client.animation;

import com.vidal1shere.jasper.client.animation.keyframe.KeyframeAnimation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Map;

// The pivot and debug fields only mean something for the item, the one kind with no model to take pivots from
@OnlyIn(Dist.CLIENT)
public record JasperAnimation(KeyframeAnimation keyframes, BlendMode mode, Map<String, BlendMode> boneModes, Look look, Vector3f pivot, boolean debug) {
    public static final Vector3f NO_PIVOT = new Vector3f();

    public JasperAnimation(KeyframeAnimation keyframes) {
        this(keyframes, BlendMode.REPLACE, Map.of(), Look.NONE, NO_PIVOT, false);
    }

    public BlendMode mode(String bone) {
        return boneModes.getOrDefault(bone, mode);
    }

    // Whether the head follows the look on each axis the animation leaves alone
    @OnlyIn(Dist.CLIENT)
    public record Look(boolean pitch, boolean yaw) {
        public static final Look NONE = new Look(false, false);
    }
}
