package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.attachment.LayerState;
import com.vidal1shere.jasper.client.model.ModelParts;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class ItemAdapter {
    public static final String BONE = "item";
    private static final Set<String> NOT_PARTS = Set.of(BONE);
    private static final float PIXEL = 1.0F / 16.0F;

    private static final MolangContext CONTEXT = new MolangContext();
    private static final AnimationPose POSE = new AnimationPose();
    private static final Vector3f PIVOT = new Vector3f();
    private static final ItemTransform BLENDED = new ItemTransform();
    private static final ItemTransform LAYER = new ItemTransform();
    private static final List<Predicate<LivingEntity>> SUPPRESSORS = new CopyOnWriteArrayList<>();
    private static final List<TrackFrame> FRAMES = new ArrayList<>();
    // The item has no counterpart bone, so a mirrored frame only flips the poses
    private static final String[][] NO_PAIRS = new String[0][];

    public static void suppressWhen(Predicate<LivingEntity> suppressor) {
        SUPPRESSORS.add(suppressor);
    }

    public static boolean isActive(LivingEntity entity, InteractionHand hand, float partialTick) {
        ClientAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ANIMATIONS);
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
        ClientAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ANIMATIONS);
        if (data == null || isSuppressed(entity)) {
            return;
        }
        double now = entity.level().getGameTime() + partialTick;
        boolean filled = false;
        boolean mirrored = BuiltinQueries.isLeftArm(entity, hand);
        int frames = 0;
        for (Layer layer : JasperLayers.sorted()) {
            LayerState track = data.existing(target(hand), layer);
            if (!isActive(track, now)) {
                continue;
            }
            if (!filled) {
                BuiltinQueries.fill(CONTEXT, entity, hand, partialTick);
                filled = true;
            }
            apply(track.track(), now, mirrored, poseStack, buffer);
            // Only the locators need the poses per bone that the item transform does not keep
            if (hasLocators(track.track())) {
                if (FRAMES.size() <= frames) {
                    FRAMES.add(new TrackFrame());
                }
                FRAMES.get(frames++).evaluate(track.track(), now, CONTEXT, mirrored ? NO_PAIRS : null);
            }
        }
        if (frames > 0) {
            Matrix4f item = new Matrix4f(poseStack.last().pose());
            LocatorCapture.capture(entity, target(hand), FRAMES, frames, bone -> bone.equals(BONE) ? item : null, UnaryOperator.identity(), PIXEL);
        }
    }

    // The parts of a 3D item, from the same hand track as the item bone
    public static void applyParts(LivingEntity entity, InteractionHand hand, ModelParts parts, float partialTick) {
        ClientAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ANIMATIONS);
        if (data == null || isSuppressed(entity)) {
            return;
        }
        double now = entity.level().getGameTime() + partialTick;
        boolean filled = false;
        boolean mirrored = BuiltinQueries.isLeftArm(entity, hand);
        int frames = 0;
        for (Layer layer : JasperLayers.sorted()) {
            LayerState track = data.existing(target(hand), layer);
            if (!isActive(track, now)) {
                continue;
            }
            if (!filled) {
                BuiltinQueries.fill(CONTEXT, entity, hand, partialTick);
                filled = true;
            }
            if (FRAMES.size() <= frames) {
                FRAMES.add(new TrackFrame());
            }
            TrackFrame frame = FRAMES.get(frames++);
            frame.evaluate(track.track(), now, CONTEXT, mirrored ? NO_PAIRS : null);
            parts.apply(frame, NOT_PARTS);
        }
    }

    private static boolean hasLocators(AnimationTrack track) {
        for (int i = 0; i < track.size(); i++) {
            JasperAnimation animation = track.animation(i);
            if (animation != null && !animation.locators().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static AnimationTarget target(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? AnimationTarget.MAIN_HAND : AnimationTarget.OFF_HAND;
    }

    private static boolean isActive(@Nullable LayerState track, double now) {
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
            ItemTransform.debugRenderPivot(poseStack, buffer, top.pivot());
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
    private static JasperAnimation evaluate(AnimationTrack track, int layer, double now, boolean mirrored, ItemTransform accumulator) {
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
