package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.LayerFrames;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.client.animation.pose.PartLookup;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.attachment.LayerState;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class ItemAdapter {
    public static final String BONE = "item";
    private static final Set<String> NOT_PARTS = Set.of(BONE);
    private static final float PIXEL = 1.0F / 16.0F;

    private static final Vector3f PIVOT = new Vector3f();
    private static final ItemTransform BLENDED = new ItemTransform();
    private static final ItemTransform LAYER = new ItemTransform();
    private static final List<Predicate<LivingEntity>> SUPPRESSORS = new CopyOnWriteArrayList<>();
    // The item has no counterpart bone, so a mirrored frame only flips the poses
    private static final LayerFrames FRAMES = new LayerFrames(new String[0][], null);

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
        if (evaluate(entity, hand, partialTick) == 0) {
            return;
        }
        boolean locators = false;
        for (int f = 0; f < FRAMES.size(); f++) {
            TrackFrame frame = FRAMES.frame(f);
            apply(frame, poseStack, buffer);
            locators |= hasLocators(frame);
        }
        if (locators) {
            Matrix4f item = new Matrix4f(poseStack.last().pose());
            LocatorCapture.capture(entity, target(hand), FRAMES.frames(), FRAMES.size(), bone -> bone.equals(BONE) ? item : null, UnaryOperator.identity(), PIXEL);
        }
    }

    // The parts of a 3D item, from the same hand track as the item bone
    public static void applyParts(LivingEntity entity, InteractionHand hand, PartLookup parts, float partialTick) {
        if (evaluate(entity, hand, partialTick) > 0) {
            FRAMES.apply(parts::part, NOT_PARTS);
        }
    }

    private static int evaluate(LivingEntity entity, InteractionHand hand, float partialTick) {
        return isSuppressed(entity) ? 0 : FRAMES.evaluate(entity, target(hand), hand, partialTick);
    }

    private static boolean hasLocators(TrackFrame frame) {
        for (int i = 0; i < frame.size(); i++) {
            JasperAnimation animation = frame.animation(i);
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

    // Whole transforms, blended with a slerp and the pivot baked in, which the per channel lerp of the parts cannot do
    private static void apply(TrackFrame frame, PoseStack poseStack, MultiBufferSource buffer) {
        JasperAnimation top = null;
        for (int i = 0; i < frame.size(); i++) {
            top = transform(frame, i, i == 0 ? BLENDED : LAYER);
            if (i > 0) {
                BLENDED.lerp(LAYER, frame.blend(i));
            }
        }
        BLENDED.applyTo(poseStack);
        if (top != null && top.debug() && frame.blend(frame.size() - 1) >= 1.0F) {
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
    private static JasperAnimation transform(TrackFrame frame, int index, ItemTransform accumulator) {
        accumulator.reset();
        JasperAnimation animation = frame.animation(index);
        if (animation != null) {
            PIVOT.set(animation.pivot());
            if (frame.mirrored()) {
                PIVOT.x = -PIVOT.x;
            }
            accumulator.set(frame.pose(index, BONE), PIVOT);
        }
        return animation;
    }
}
