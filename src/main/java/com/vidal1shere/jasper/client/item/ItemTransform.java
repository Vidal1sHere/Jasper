package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ItemTransform {
    private static final Quaternionf IDENTITY = new Quaternionf();

    private final Vector3f translation = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1.0F);
    private final Vector3f moved = new Vector3f();

    public void reset() {
        translation.zero();
        rotation.identity();
        scale.set(1.0F);
    }

    // The pose position is in pixels, the pivot in blocks
    public void set(BonePose pose, Vector3f pivot) {
        translation.set(pose.position).div(16.0F);
        rotation.identity().rotateX(pose.rotation.x).rotateY(pose.rotation.y).rotateZ(pose.rotation.z);
        scale.set(pose.scale);
        bakePivot(pivot);
    }

    // Baked into the translation so two accumulators can be blended
    private void bakePivot(Vector3f pivot) {
        if (pivot.x == 0.0F && pivot.y == 0.0F && pivot.z == 0.0F) {
            return;
        }
        rotation.transform(moved.set(pivot).mul(scale));
        translation.add(pivot.x - moved.x, pivot.y - moved.y, pivot.z - moved.z);
    }

    public void lerp(ItemTransform target, float t) {
        translation.lerp(target.translation, t);
        rotation.slerp(target.rotation, t);
        scale.lerp(target.scale, t);
    }

    public void applyTo(PoseStack poseStack) {
        if (translation.x != 0.0F || translation.y != 0.0F || translation.z != 0.0F) {
            poseStack.translate(translation.x, translation.y, translation.z);
        }
        if (!rotation.equals(IDENTITY)) {
            poseStack.mulPose(rotation);
        }
        if (scale.x != 1.0F || scale.y != 1.0F || scale.z != 1.0F) {
            poseStack.scale(scale.x, scale.y, scale.z);
        }
    }
}
