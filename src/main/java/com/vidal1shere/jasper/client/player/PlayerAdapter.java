package com.vidal1shere.jasper.client.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.animation.LayerFrames;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.Layer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class PlayerAdapter {
    // Model parts and their locators are in pixels
    private static final float MODEL_UNIT = 1.0F / 16.0F;
    // Animations are written for the right arm and mirrored when the hand is on the left one
    private static final LayerFrames FRAMES = new LayerFrames(PlayerBones.PAIRS, (context, entity, hand, partialTick) -> fillLook(context, entity, partialTick));

    private static final Matrix3f WORLD = new Matrix3f();
    private static final Matrix3f MODEL = new Matrix3f();

    private static int frameEntity = -1;
    private static int lookEntity = -1;

    public static void clear() {
        frameEntity = -1;
    }

    public static void captureWorld(PoseStack poseStack) {
        WORLD.set(poseStack.last().normal());
    }

    public static void prepare(LivingEntity entity, float partialTick, PoseStack poseStack) {
        MODEL.set(poseStack.last().normal());
        lookEntity = entity.getId();
        frameEntity = FRAMES.evaluate(entity, AnimationTarget.MODEL, null, partialTick) > 0 ? entity.getId() : -1;
    }

    // Outside of a render the look queries keep the head rotation
    public static void fillLook(MolangContext context, LivingEntity entity, float partialTick) {
        if (lookEntity == entity.getId()) {
            BuiltinQueries.fillLook(context, entity, partialTick, WORLD, MODEL);
        }
    }

    public static void finish() {
        frameEntity = -1;
        lookEntity = -1;
    }

    public static boolean isAnimating(Entity entity) {
        return frameEntity == entity.getId();
    }

    public static boolean isAnimating(Entity entity, Layer layer) {
        return frameEntity == entity.getId() && FRAMES.contains(layer);
    }

    public static void applyRoot(PoseStack poseStack) {
        for (int i = 0; i < FRAMES.size(); i++) {
            FRAMES.frame(i).transform(poseStack, PlayerBones.ROOT, PlayerBones.ROOT_PIVOT);
        }
    }

    public static void applyItem(HumanoidArm arm, PoseStack poseStack) {
        boolean right = arm == HumanoidArm.RIGHT;
        String bone = right ? PlayerBones.RIGHT_ITEM : PlayerBones.LEFT_ITEM;
        Vector3f pivot = right ? PlayerBones.RIGHT_ITEM_PIVOT : PlayerBones.LEFT_ITEM_PIVOT;
        for (int i = 0; i < FRAMES.size(); i++) {
            FRAMES.frame(i).transform(poseStack, bone, pivot);
        }
    }

    // At the model render, with the root bone already on the pose stack; the item bones are captured by the item layer
    public static void captureModelLocators(LivingEntity entity, PlayerModel<?> model, PoseStack poseStack) {
        Matrix4f root = new Matrix4f(poseStack.last().pose());
        LocatorCapture.capture(entity, AnimationTarget.MODEL, FRAMES.frames(), FRAMES.size(), bone -> switch (bone) {
            case PlayerBones.ROOT -> root;
            case PlayerBones.TORSO -> LocatorCapture.part(root, model.body);
            case PlayerBones.HEAD -> LocatorCapture.part(root, model.head);
            case PlayerBones.RIGHT_ARM -> LocatorCapture.part(root, model.rightArm);
            case PlayerBones.LEFT_ARM -> LocatorCapture.part(root, model.leftArm);
            case PlayerBones.RIGHT_LEG -> LocatorCapture.part(root, model.rightLeg);
            case PlayerBones.LEFT_LEG -> LocatorCapture.part(root, model.leftLeg);
            default -> null;
        }, PlayerBones::counterpart, MODEL_UNIT);
    }

    public static void captureItemLocators(LivingEntity entity, HumanoidArm arm, PoseStack poseStack) {
        String bone = arm == HumanoidArm.RIGHT ? PlayerBones.RIGHT_ITEM : PlayerBones.LEFT_ITEM;
        Matrix4f item = new Matrix4f(poseStack.last().pose());
        LocatorCapture.capture(entity, AnimationTarget.MODEL, FRAMES.frames(), FRAMES.size(), name -> name.equals(bone) ? item : null, PlayerBones::counterpart, MODEL_UNIT);
    }

    public static void applyModel(PlayerModel<?> model) {
        for (int i = 0; i < FRAMES.size(); i++) {
            TrackFrame frame = FRAMES.frame(i);
            frame.apply(PlayerBones.HEAD, model.head, model.hat);
            frame.apply(PlayerBones.TORSO, model.body, model.jacket);
            frame.apply(PlayerBones.RIGHT_ARM, model.rightArm, model.rightSleeve);
            frame.apply(PlayerBones.LEFT_ARM, model.leftArm, model.leftSleeve);
            frame.apply(PlayerBones.RIGHT_LEG, model.rightLeg, model.rightPants);
            frame.apply(PlayerBones.LEFT_LEG, model.leftLeg, model.leftPants);
        }
    }
}
