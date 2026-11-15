package com.vidal1shere.jasper.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import com.vidal1shere.jasper.client.animation.pose.PartPoses;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class TrackFrame {
    private static final BonePose IDENTITY = new BonePose();

    private final List<AnimationPose> poses = new ArrayList<>();
    private final List<JasperAnimation> animations = new ArrayList<>();
    private final float[] blends = new float[AnimationTrack.MAX_FADES];
    private final BonePose current = new BonePose();
    private final BonePose blended = new BonePose();
    private final BonePose layer = new BonePose();
    private int size;
    @Nullable
    private String[][] mirrorPairs;

    public void evaluate(AnimationTrack track, double now, MolangContext context, @Nullable String[][] mirrorPairs) {
        this.mirrorPairs = mirrorPairs;
        size = track.size();
        animations.clear();
        for (int i = 0; i < size; i++) {
            if (poses.size() <= i) {
                poses.add(new AnimationPose());
            }
            animations.add(track.evaluate(i, now, context, poses.get(i)));
            blends[i] = track.blend(i);
            if (mirrorPairs != null) {
                poses.get(i).mirror(mirrorPairs);
            }
        }
    }

    public int size() {
        return size;
    }

    @Nullable
    public JasperAnimation animation(int index) {
        return animations.get(index);
    }

    // The share of the final pose that layer i gives, once every newer layer faded in over it
    public float weight(int index) {
        float weight = blends[index];
        for (int i = index + 1; i < size; i++) {
            weight *= 1.0F - blends[i];
        }
        return weight;
    }

    public boolean animates(String bone) {
        for (int i = 0; i < size; i++) {
            if (animations.get(i) != null && poses.get(i).get(bone).isAnimated()) {
                return true;
            }
        }
        return false;
    }

    // For bones applied on a pose stack, where no animation means no change
    public BonePose blend(String bone) {
        blended.set(pose(0, bone));
        for (int i = 1; i < size; i++) {
            blended.lerp(pose(i, bone), blends[i]);
        }
        return blended;
    }

    public void transform(PoseStack poseStack, String bone, Vector3f pivot) {
        PartPoses.transform(poseStack, blend(bone), pivot);
    }

    public boolean mirrored() {
        return mirrorPairs != null;
    }

    public void apply(String bone, ModelPart part, @Nullable ModelPart overlay) {
        if (!animates(bone)) {
            return;
        }
        PartPoses.read(part, current);
        blend(bone, part.getInitialPose());
        PartPoses.write(blended, part);
        if (overlay != null) {
            overlay.copyFrom(part);
        }
    }

    // The same as apply, for bones that are only a pose, like locators
    public void resolve(String bone, BonePose pose, PartPose initial) {
        if (!animates(bone)) {
            return;
        }
        current.set(pose);
        blend(bone, initial);
        pose.set(blended);
    }

    private void blend(String bone, PartPose initial) {
        resolve(0, bone, initial, blended);
        for (int i = 1; i < size; i++) {
            resolve(i, bone, initial, layer);
            blended.lerp(layer, blends[i]);
        }
    }

    private BonePose pose(int index, String bone) {
        return animations.get(index) == null ? IDENTITY : poses.get(index).get(bone);
    }

    private void resolve(int index, String bone, PartPose initial, BonePose out) {
        JasperAnimation animation = animations.get(index);
        if (animation == null) {
            out.set(current);
            return;
        }
        PartPoses.resolve(current, initial, poses.get(index).get(bone), animation.mode(source(bone)), out);
    }

    // The bone the animation wrote for this one, before the mirror swapped them
    private String source(String bone) {
        if (mirrorPairs != null) {
            for (String[] pair : mirrorPairs) {
                if (pair[0].equals(bone)) {
                    return pair[1];
                }
                if (pair[1].equals(bone)) {
                    return pair[0];
                }
            }
        }
        return bone;
    }
}
