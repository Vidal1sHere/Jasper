package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import com.vidal1shere.jasper.client.attachment.ItemAnimationData;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
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

    public static void set(int entityId, InteractionHand hand, @Nullable ResourceLocation id, long startTick, float speed) {
        set(entityId, hand, false, id, startTick, speed);
    }

    public static void set(int entityId, InteractionHand hand, boolean overlay, @Nullable ResourceLocation id, long startTick, float speed) {
        ItemAnimationData data = ClientAttachments.of(entityId, ClientAttachments.ITEM_ANIMATIONS);
        if (data != null) {
            (overlay ? data.overlay(hand) : data.track(hand)).set(id, startTick, speed);
        }
    }

    public static boolean isActive(LivingEntity entity, InteractionHand hand, float partialTick) {
        ItemAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ITEM_ANIMATIONS);
        if (data == null || isSuppressed(entity)) {
            return false;
        }
        double now = entity.level().getGameTime() + partialTick;
        return isActive(data.tracks().get(hand), now) || isActive(data.overlays().get(hand), now);
    }

    public static void apply(LivingEntity entity, InteractionHand hand, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        ItemAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ITEM_ANIMATIONS);
        if (data == null || isSuppressed(entity)) {
            return;
        }
        double now = entity.level().getGameTime() + partialTick;
        AnimationTrack track = data.tracks().get(hand);
        AnimationTrack overlay = data.overlays().get(hand);
        boolean main = isActive(track, now);
        if (!main && !isActive(overlay, now)) {
            return;
        }
        AnimationQueries.fill(CONTEXT, entity, hand, partialTick);
        boolean mirrored = AnimationQueries.isLeftArm(entity, hand);
        if (main) {
            apply(track, now, mirrored, poseStack, buffer);
        }
        if (isActive(overlay, now)) {
            apply(overlay, now, mirrored, poseStack, buffer);
        }
    }

    private static boolean isActive(@Nullable AnimationTrack track, double now) {
        if (track == null) {
            return false;
        }
        track.update(now);
        return track.isActive();
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
