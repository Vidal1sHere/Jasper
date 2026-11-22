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
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.client.molang.MolangQuery;
import com.vidal1shere.jasper.item.AnimatedItem;
import com.vidal1shere.jasper.item.ItemAnimationDriver;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@OnlyIn(Dist.CLIENT)
public final class ItemAdapter {
    public static final String BONE = "item";
    private static final Set<String> NOT_PARTS = Set.of(BONE);
    private static final float PIXEL = 1.0F / 16.0F;

    private static final Vector3f PIVOT = new Vector3f();
    private static final ItemTransform BLENDED = new ItemTransform();
    private static final ItemTransform LAYER = new ItemTransform();
    private static final List<Predicate<LivingEntity>> SUPPRESSORS = new CopyOnWriteArrayList<>();
    // From the display context, since the camera is wrong for the player drawn in the inventory screen
    private static boolean firstPerson;
    // The item has no counterpart bone, so a mirrored frame only flips the poses
    private static final LayerFrames FRAMES = new LayerFrames(new String[0][], (context, entity, hand, partialTick) -> context.set(MolangQuery.IS_FIRST_PERSON, firstPerson));

    public static void suppressWhen(Predicate<LivingEntity> suppressor) {
        SUPPRESSORS.add(suppressor);
    }

    public static boolean isActive(LivingEntity entity, InteractionHand hand, Layer layer, float partialTick) {
        ClientAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ANIMATIONS);
        if (data == null || isSuppressed(entity)) {
            return false;
        }
        return isActive(data.existing(AnimationTarget.of(hand), layer), entity.level().getGameTime() + partialTick);
    }

    public static void apply(LivingEntity entity, InteractionHand hand, PoseStack poseStack, float partialTick) {
        if (evaluate(entity, hand, true, partialTick) == 0) {
            return;
        }
        for (int f = 0; f < FRAMES.size(); f++) {
            apply(FRAMES.frame(f), poseStack);
        }
        if (hasLocators()) {
            Matrix4f item = new Matrix4f(poseStack.last().pose());
            LocatorCapture.capture(entity, AnimationTarget.of(hand), FRAMES.frames(), FRAMES.size(), bone -> bone.equals(BONE) ? item : null, UnaryOperator.identity(), PIXEL);
        }
    }

    public static void applyParts(LivingEntity entity, InteractionHand hand, PartLookup parts, Map<String, ModelPart> extra, boolean firstPersonView, PoseStack poseStack, float partialTick) {
        if (evaluate(entity, hand, firstPersonView, partialTick) == 0) {
            return;
        }
        FRAMES.apply(bone -> extra.containsKey(bone) ? extra.get(bone) : parts.part(bone), NOT_PARTS);
        if (hasLocators() && (firstPersonView || LocatorCapture.inLevel())) {
            Matrix4f root = new Matrix4f(poseStack.last().pose());
            LocatorCapture.capture(entity, AnimationTarget.of(hand), FRAMES.frames(), FRAMES.size(), bone -> bone.equals(BONE) ? null : parts.matrix(root, bone), UnaryOperator.identity(), PIXEL);
        }
    }

    public static void drive(LivingEntity entity, InteractionHand hand, ItemStack stack, ItemDisplayContext displayContext, float partialTick) {
        AnimatedItem item = stack.getItem() instanceof AnimatedItem animated ? animated : null;
        ClientAnimationData data = item == null ? entity.getExistingDataOrNull(ClientAttachments.ANIMATIONS) : entity.getData(ClientAttachments.ANIMATIONS);
        if (data == null) {
            return;
        }
        double now = entity.level().getGameTime() + partialTick;
        for (Layer layer : JasperLayers.sorted()) {
            ItemAnimationDriver driver = item == null ? null : item.driver(layer);
            if (driver != null) {
                data.track(AnimationTarget.of(hand), layer, JasperAnimationLoader.ITEM).drive(driver.pick(stack, entity, hand, displayContext), now);
                continue;
            }
            LayerState track = data.existing(AnimationTarget.of(hand), layer);
            if (track != null && track.driverSeen()) {
                track.drive(null, now);
            }
        }
    }

    private static int evaluate(LivingEntity entity, InteractionHand hand, boolean firstPersonView, float partialTick) {
        if (firstPersonView && isSuppressed(entity)) {
            return 0;
        }
        firstPerson = firstPersonView;
        return FRAMES.evaluate(entity, AnimationTarget.of(hand), hand, partialTick);
    }

    private static boolean hasLocators() {
        for (int f = 0; f < FRAMES.size(); f++) {
            TrackFrame frame = FRAMES.frame(f);
            for (int i = 0; i < frame.size(); i++) {
                JasperAnimation animation = frame.animation(i);
                if (animation != null && !animation.locators().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isActive(@Nullable LayerState track, double now) {
        if (track == null) {
            return false;
        }
        track.track().update(now);
        return track.track().isActive();
    }

    private static void apply(TrackFrame frame, PoseStack poseStack) {
        for (int i = 0; i < frame.size(); i++) {
            transform(frame, i, i == 0 ? BLENDED : LAYER);
            if (i > 0) {
                BLENDED.lerp(LAYER, frame.blend(i));
            }
        }
        BLENDED.applyTo(poseStack);
    }

    private static boolean isSuppressed(LivingEntity entity) {
        for (Predicate<LivingEntity> suppressor : SUPPRESSORS) {
            if (suppressor.test(entity)) {
                return true;
            }
        }
        return false;
    }

    private static void transform(TrackFrame frame, int index, ItemTransform accumulator) {
        accumulator.reset();
        JasperAnimation animation = frame.animation(index);
        if (animation != null) {
            PIVOT.set(animation.pivot());
            if (frame.mirrored()) {
                PIVOT.x = -PIVOT.x;
            }
            accumulator.set(frame.pose(index, BONE), PIVOT);
        }
    }
}
