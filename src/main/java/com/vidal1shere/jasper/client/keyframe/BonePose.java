package com.vidal1shere.jasper.client.keyframe;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class BonePose {
    public final Vector3f position = new Vector3f();
    public final Vector3f rotation = new Vector3f();
    public final Vector3f scale = new Vector3f(1.0F);
    public boolean hasPosition;
    public boolean hasRotation;
    public boolean hasScale;

    public void reset() {
        position.zero();
        rotation.zero();
        scale.set(1.0F);
        hasPosition = false;
        hasRotation = false;
        hasScale = false;
    }

    public boolean isAnimated() {
        return hasPosition || hasRotation || hasScale;
    }

    public void set(BonePose other) {
        position.set(other.position);
        rotation.set(other.rotation);
        scale.set(other.scale);
        hasPosition = other.hasPosition;
        hasRotation = other.hasRotation;
        hasScale = other.hasScale;
    }

    public void mirror() {
        position.x = -position.x;
        rotation.y = -rotation.y;
        rotation.z = -rotation.z;
    }

    public void lerp(BonePose target, float t) {
        position.lerp(target.position, t);
        rotation.set(Mth.lerp(t, rotation.x, target.rotation.x), Mth.lerp(t, rotation.y, target.rotation.y), Mth.lerp(t, rotation.z, target.rotation.z));
        scale.lerp(target.scale, t);
        hasPosition |= target.hasPosition;
        hasRotation |= target.hasRotation;
        hasScale |= target.hasScale;
    }
}
