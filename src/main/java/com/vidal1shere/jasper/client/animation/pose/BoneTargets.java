package com.vidal1shere.jasper.client.animation.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.animation.BlendMode;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class BoneTargets {
    private static final BonePose CURRENT = new BonePose();
    private static final Quaternionf ROTATION = new Quaternionf();

    public static void read(ModelPart part, BonePose out) {
        out.reset();
        out.position.set(part.x, part.y, part.z);
        out.rotation.set(part.xRot, part.yRot, part.zRot);
        out.scale.set(part.xScale, part.yScale, part.zScale);
    }

    public static void write(BonePose pose, ModelPart part) {
        part.x = pose.position.x;
        part.y = pose.position.y;
        part.z = pose.position.z;
        part.xRot = pose.rotation.x;
        part.yRot = pose.rotation.y;
        part.zRot = pose.rotation.z;
        part.xScale = pose.scale.x;
        part.yScale = pose.scale.y;
        part.zScale = pose.scale.z;
    }

    public static void resolve(BonePose current, PartPose initial, BonePose pose, BlendMode mode, BonePose out) {
        out.set(current);
        boolean replace = mode == BlendMode.REPLACE;
        if (pose.hasPosition) {
            if (replace) {
                out.position.set(initial.x, initial.y, initial.z);
            }
            out.position.add(pose.position);
        }
        if (pose.hasRotation) {
            if (replace) {
                out.rotation.set(initial.xRot, initial.yRot, initial.zRot);
            }
            out.rotation.add(pose.rotation);
        }
        if (pose.hasScale) {
            if (replace) {
                out.scale.set(pose.scale);
            } else {
                out.scale.mul(pose.scale);
            }
        }
    }

    public static void apply(ModelPart part, @Nullable ModelPart overlay, BonePose pose, BlendMode mode) {
        if (!pose.isAnimated()) {
            return;
        }
        read(part, CURRENT);
        resolve(CURRENT, part.getInitialPose(), pose, mode, CURRENT);
        write(CURRENT, part);
        if (overlay != null) {
            overlay.copyFrom(part);
        }
    }

    public static void transform(PoseStack poseStack, BonePose pose, Vector3f pivot) {
        if (!pose.isAnimated()) {
            return;
        }
        poseStack.translate((pivot.x + pose.position.x) / 16.0F, (pivot.y + pose.position.y) / 16.0F, (pivot.z + pose.position.z) / 16.0F);
        poseStack.mulPose(ROTATION.rotationZYX(pose.rotation.z, pose.rotation.y, pose.rotation.x));
        poseStack.scale(pose.scale.x, pose.scale.y, pose.scale.z);
        poseStack.translate(-pivot.x / 16.0F, -pivot.y / 16.0F, -pivot.z / 16.0F);
    }
}
