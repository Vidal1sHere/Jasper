package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ItemTransform {
    private float tx, ty, tz;
    private final Quaternionf rotation = new Quaternionf();
    private float sx = 1.0f, sy = 1.0f, sz = 1.0f;

    public void reset() {
        this.tx = 0.0f;
        this.ty = 0.0f;
        this.tz = 0.0f;
        this.rotation.identity();
        this.sx = 1.0f;
        this.sy = 1.0f;
        this.sz = 1.0f;
    }

    // The pose position is in pixels, the pivot in blocks
    public void set(BonePose pose, Vector3f pivot) {
        this.tx = pose.position.x / 16.0f;
        this.ty = pose.position.y / 16.0f;
        this.tz = pose.position.z / 16.0f;
        this.rotation.identity().rotateX(pose.rotation.x).rotateY(pose.rotation.y).rotateZ(pose.rotation.z);
        this.sx = pose.scale.x;
        this.sy = pose.scale.y;
        this.sz = pose.scale.z;
        bakePivot(pivot);
    }

    // Baked into the translation so two accumulators can be blended
    private void bakePivot(Vector3f pivot) {
        if (pivot.x() == 0.0f && pivot.y() == 0.0f && pivot.z() == 0.0f) {
            return;
        }
        Vector3f moved = this.rotation.transform(new Vector3f(pivot.x() * this.sx, pivot.y() * this.sy, pivot.z() * this.sz));
        this.tx += pivot.x() - moved.x();
        this.ty += pivot.y() - moved.y();
        this.tz += pivot.z() - moved.z();
    }

    public void lerp(ItemTransform target, float t) {
        this.tx = Mth.lerp(t, this.tx, target.tx);
        this.ty = Mth.lerp(t, this.ty, target.ty);
        this.tz = Mth.lerp(t, this.tz, target.tz);
        this.rotation.slerp(target.rotation, t);
        this.sx = Mth.lerp(t, this.sx, target.sx);
        this.sy = Mth.lerp(t, this.sy, target.sy);
        this.sz = Mth.lerp(t, this.sz, target.sz);
    }

    public void applyTo(PoseStack poseStack) {
        if (this.tx != 0.0f || this.ty != 0.0f || this.tz != 0.0f) {
            poseStack.translate(this.tx, this.ty, this.tz);
        }
        if (!this.rotation.equals(new Quaternionf())) {
            poseStack.mulPose(this.rotation);
        }
        if (this.sx != 1.0f || this.sy != 1.0f || this.sz != 1.0f) {
            poseStack.scale(this.sx, this.sy, this.sz);
        }
    }
}
