package com.vidal1shere.jasper.client.animation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class AnimationPose {
    private final Map<String, BonePose> bones = new HashMap<>();

    public BonePose get(String bone) {
        return bones.computeIfAbsent(bone, key -> new BonePose());
    }

    public void mirror(String[][] pairs) {
        for (BonePose pose : bones.values()) {
            pose.mirror();
        }
        for (String[] pair : pairs) {
            BonePose first = get(pair[0]);
            bones.put(pair[0], get(pair[1]));
            bones.put(pair[1], first);
        }
    }

    public void reset() {
        for (BonePose pose : bones.values()) {
            pose.reset();
        }
    }
}
