package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import com.vidal1shere.jasper.client.attachment.LayerData;
import com.vidal1shere.jasper.client.attachment.layer.LayerTrack;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class ClientItemAnimations {
    public static final String BONE = "item";

    private static final MolangContext CONTEXT = new MolangContext();
    private static final AnimationPose POSE = new AnimationPose();
    private static final Vector3f PIVOT = new Vector3f();
    private static final ItemTransformAccumulator BLENDED = new ItemTransformAccumulator();
    private static final ItemTransformAccumulator LAYER = new ItemTransformAccumulator();
    private static final List<Predicate<LivingEntity>> SUPPRESSORS = new CopyOnWriteArrayList<>();

    public static void suppressWhen(Predicate<LivingEntity> suppressor) {
        SUPPRESSORS.add(suppressor);
    }

    public static boolean isActive(LivingEntity entity, InteractionHand hand, float partialTick) {
        LayerData data = entity.getExistingDataOrNull(ClientAttachments.LAYERS);
        if (data == null || isSuppressed(entity)) {
            return false;
        }
        double now = entity.level().getGameTime() + partialTick;
        for (Layer layer : JasperLayers.sorted()) {
            if (isActive(data.existing(target(hand), layer), now)) {
                return true;
            }
        }
        return false;
    }

    public static void apply(LivingEntity entity, InteractionHand hand, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        LayerData data = entity.getExistingDataOrNull(ClientAttachments.LAYERS);
        if (data == null || isSuppressed(entity)) {
            return;
        }
        double now = entity.level().getGameTime() + partialTick;
        boolean filled = false;
        for (Layer layer : JasperLayers.sorted()) {
            LayerTrack track = data.existing(target(hand), layer);
            if (!isActive(track, now)) {
                continue;
            }
            if (!filled) {
                AnimationQueries.fill(CONTEXT, entity, hand, partialTick);
                filled = true;
            }
            apply(track.track(), now, AnimationQueries.isLeftArm(entity, hand), poseStack, buffer);
        }
    }

    public static AnimationTarget target(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? AnimationTarget.MAIN_HAND : AnimationTarget.OFF_HAND;
    }

    private static boolean isActive(@Nullable LayerTrack track, double now) {
        if (track == null) {
            return false;
        }
        track.track().update(now);
        return track.track().isActive();
    }

    private static void apply(AnimationTrack track, double now, boolean mirrored, PoseStack poseStack, MultiBufferSource buffer) {
        JasperAnimation top = null;
        for (int i = 0; i < track.size(); i++) {
            top = evaluate(track, i, now, mirrored, i == 0 ? BLENDED : LAYER);
            if (i > 0) {
                BLENDED.lerp(LAYER, track.blend(i));
            }
        }
        BLENDED.applyTo(poseStack);
        if (top != null && top.debug() && track.blend(track.size() - 1) >= 1.0F) {
            ItemTransformAccumulator.debugRenderPivot(poseStack, buffer, top.pivot());
        }
    }

    private static boolean isSuppressed(LivingEntity entity) {
        for (Predicate<LivingEntity> suppressor : SUPPRESSORS) {
            if (suppressor.test(entity)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static JasperAnimation evaluate(AnimationTrack track, int layer, double now, boolean mirrored, ItemTransformAccumulator accumulator) {
        accumulator.reset();
        JasperAnimation animation = track.evaluate(layer, now, CONTEXT, POSE);
        if (animation != null) {
            BonePose pose = POSE.get(BONE);
            PIVOT.set(animation.pivot());
            if (mirrored) {
                pose.mirror();
                PIVOT.x = -PIVOT.x;
            }
            accumulator.set(pose, PIVOT);
        }
        return animation;
    }
}
