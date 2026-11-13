package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationPlayback;
import com.vidal1shere.jasper.client.animation.AnimationPose;
import com.vidal1shere.jasper.client.animation.BonePose;
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
    private static final MolangContext CONTEXT = new MolangContext();
    private static final AnimationPose POSE = new AnimationPose();
    private static final Vector3f PIVOT = new Vector3f();
    private static final ItemTransformAccumulator CURRENT = new ItemTransformAccumulator();
    private static final ItemTransformAccumulator PREVIOUS = new ItemTransformAccumulator();
    private static final List<Predicate<LivingEntity>> SUPPRESSORS = new CopyOnWriteArrayList<>();

    public static void suppressWhen(Predicate<LivingEntity> suppressor) {
        SUPPRESSORS.add(suppressor);
    }

    public static void set(int entityId, InteractionHand hand, @Nullable ResourceLocation id, long startTick, float speed) {
        ItemAnimationData data = ClientAttachments.of(entityId, ClientAttachments.ITEM_ANIMATIONS);
        if (data != null) {
            data.playback(hand).set(id, startTick, speed);
        }
    }

    public static boolean isActive(LivingEntity entity, InteractionHand hand, float partialTick) {
        AnimationPlayback<ItemAnimation> playback = playback(entity, hand);
        return playback != null && !isSuppressed(entity) && playback.isActive(entity.level().getGameTime() + partialTick);
    }

    public static void apply(LivingEntity entity, InteractionHand hand, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        AnimationPlayback<ItemAnimation> playback = playback(entity, hand);
        if (playback == null || isSuppressed(entity)) {
            return;
        }
        double now = entity.level().getGameTime() + partialTick;
        AnimationQueries.fill(CONTEXT, entity, hand, partialTick);
        boolean mirrored = AnimationQueries.isLeftArm(entity, hand);
        ItemAnimation current = evaluate(playback, playback.current(), now, mirrored, CURRENT);
        float weight = playback.weight(now);
        if (weight >= 1.0F) {
            if (current != null) {
                CURRENT.applyTo(poseStack);
                if (current.debug()) {
                    ItemTransformAccumulator.debugRenderPivot(poseStack, buffer, current.pivot());
                }
            }
            return;
        }
        ItemAnimation previous = evaluate(playback, playback.previous(), now, mirrored, PREVIOUS);
        if (current != null || previous != null) {
            PREVIOUS.lerp(CURRENT, weight);
            PREVIOUS.applyTo(poseStack);
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
    private static AnimationPlayback<ItemAnimation> playback(LivingEntity entity, InteractionHand hand) {
        ItemAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ITEM_ANIMATIONS);
        return data == null ? null : data.playbacks().get(hand);
    }

    @Nullable
    private static ItemAnimation evaluate(AnimationPlayback<ItemAnimation> playback, AnimationPlayback.Entry entry, double now, boolean mirrored, ItemTransformAccumulator accumulator) {
        accumulator.reset();
        ItemAnimation animation = playback.evaluate(entry, now, CONTEXT, POSE);
        if (animation != null) {
            BonePose pose = POSE.get(ItemAnimation.BONE);
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
