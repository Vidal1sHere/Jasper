package com.vidal1shere.jasper.client.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.attachment.PlayerAnimationData;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClientPlayerAnimations {
    private static final MolangContext CONTEXT = new MolangContext();
    private static final TrackFrame FRAME = new TrackFrame();
    private static final TrackFrame OVERLAY = new TrackFrame();

    private static final Matrix3f WORLD = new Matrix3f();
    private static final Matrix3f MODEL = new Matrix3f();

    private static int frameEntity = -1;
    private static int lookEntity = -1;

    public static void play(int entityId, InteractionHand hand, ResourceLocation animation, long startTick, float speed) {
        set(entityId, hand, false, animation, startTick, speed);
    }

    public static void stop(int entityId, long tick) {
        PlayerAnimationData data = ClientAttachments.of(entityId, ClientAttachments.PLAYER_ANIMATIONS);
        if (data != null) {
            data.track().set(null, tick, 1.0F);
        }
    }

    // A stop keeps the hand, so the fade out stays mirrored like the animation it ends
    public static void set(int entityId, InteractionHand hand, boolean overlay, @Nullable ResourceLocation animation, long startTick, float speed) {
        PlayerAnimationData data = ClientAttachments.of(entityId, ClientAttachments.PLAYER_ANIMATIONS);
        if (data == null) {
            return;
        }
        if (animation != null) {
            if (overlay) {
                data.setOverlayHand(hand);
            } else {
                data.setHand(hand);
            }
        }
        (overlay ? data.overlay() : data.track()).set(animation, startTick, speed);
    }

    public static void clear() {
        frameEntity = -1;
    }

    public static void captureWorld(PoseStack poseStack) {
        WORLD.set(poseStack.last().normal());
    }

    public static void prepare(LivingEntity entity, float partialTick, PoseStack poseStack) {
        frameEntity = -1;
        MODEL.set(poseStack.last().normal());
        lookEntity = entity.getId();
        PlayerAnimationData state = entity.getExistingDataOrNull(ClientAttachments.PLAYER_ANIMATIONS);
        if (state == null) {
            return;
        }
        double now = entity.level().getGameTime() + partialTick;
        state.track().update(now);
        state.overlay().update(now);
        if (!state.track().isActive() && !state.overlay().isActive()) {
            return;
        }
        evaluate(FRAME, state.track(), state.hand(), entity, now, partialTick);
        evaluate(OVERLAY, state.overlay(), state.overlayHand(), entity, now, partialTick);
        frameEntity = entity.getId();
    }

    private static void evaluate(TrackFrame frame, AnimationTrack track, InteractionHand hand, LivingEntity entity, double now, float partialTick) {
        AnimationQueries.fill(CONTEXT, entity, hand, partialTick);
        fillLook(CONTEXT, entity, partialTick);
        // Animations are written for the right arm and mirrored when the hand is on the left one
        frame.evaluate(track, now, CONTEXT, AnimationQueries.isLeftArm(entity, hand) ? PlayerBones.PAIRS : null);
    }

    // Outside of a render the look queries keep the head rotation
    public static void fillLook(MolangContext context, LivingEntity entity, float partialTick) {
        if (lookEntity == entity.getId()) {
            AnimationQueries.fillLook(context, entity, partialTick, WORLD, MODEL);
        }
    }

    public static void finish() {
        frameEntity = -1;
        lookEntity = -1;
    }

    public static boolean isAnimating(Entity entity) {
        return frameEntity == entity.getId();
    }

    public static void applyRoot(PoseStack poseStack) {
        FRAME.transform(poseStack, PlayerBones.ROOT, PlayerBones.ROOT_PIVOT);
        OVERLAY.transform(poseStack, PlayerBones.ROOT, PlayerBones.ROOT_PIVOT);
    }

    public static void applyItem(HumanoidArm arm, PoseStack poseStack) {
        boolean right = arm == HumanoidArm.RIGHT;
        String bone = right ? PlayerBones.RIGHT_ITEM : PlayerBones.LEFT_ITEM;
        Vector3f pivot = right ? PlayerBones.RIGHT_ITEM_PIVOT : PlayerBones.LEFT_ITEM_PIVOT;
        FRAME.transform(poseStack, bone, pivot);
        OVERLAY.transform(poseStack, bone, pivot);
    }

    public static void applyModel(PlayerModel<?> model) {
        applyModel(FRAME, model);
        applyModel(OVERLAY, model);
    }

    private static void applyModel(TrackFrame frame, PlayerModel<?> model) {
        frame.apply(PlayerBones.HEAD, model.head, model.hat);
        frame.apply(PlayerBones.TORSO, model.body, model.jacket);
        frame.apply(PlayerBones.RIGHT_ARM, model.rightArm, model.rightSleeve);
        frame.apply(PlayerBones.LEFT_ARM, model.leftArm, model.leftSleeve);
        frame.apply(PlayerBones.RIGHT_LEG, model.rightLeg, model.rightPants);
        frame.apply(PlayerBones.LEFT_LEG, model.leftLeg, model.leftPants);
    }
}
