package com.vidal1shere.jasper.client.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.keyframe.AnimationPose;
import com.vidal1shere.jasper.client.keyframe.BonePose;
import com.vidal1shere.jasper.client.keyframe.KeyframeAnimations;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@OnlyIn(Dist.CLIENT)
public final class ArmPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> ONE_HANDED = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) ArmPoses::apply);
    public static final EnumProxy<HumanoidModel.ArmPose> TWO_HANDED = new EnumProxy<>(HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) ArmPoses::apply);

    private static final MolangContext CONTEXT = new MolangContext();
    private static final AnimationPose POSE = new AnimationPose();
    private static final List<Resolver> RESOLVERS = new CopyOnWriteArrayList<>();

    public static void addResolver(Resolver resolver) {
        RESOLVERS.add(resolver);
    }

    public static HumanoidModel.ArmPose select(AbstractClientPlayer player, InteractionHand hand, HumanoidModel.ArmPose vanilla) {
        if (vanilla != HumanoidModel.ArmPose.ITEM && vanilla != HumanoidModel.ArmPose.EMPTY) {
            return vanilla;
        }
        PlayerAnimation pose = pose(player, hand);
        if (pose == null) {
            return vanilla;
        }
        return (pose.animation().bones().containsKey(PlayerBones.LEFT_ARM) ? TWO_HANDED : ONE_HANDED).getValue();
    }

    // Arm rotations go through the vanilla arm pose, so the vanilla swing still adds on top
    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        PlayerAnimation pose = pose(entity, hand);
        if (pose == null) {
            return;
        }
        boolean mirrored = evaluate(entity, hand, pose);
        rotate(pose, PlayerBones.RIGHT_ARM, mirrored, model.rightArm);
        rotate(pose, PlayerBones.LEFT_ARM, mirrored, model.leftArm);
    }

    // After the vanilla setupAnim: arm positions land on top of the crouch offsets and the body follows the swing twist
    public static boolean applyModel(PlayerModel<?> model, LivingEntity entity) {
        boolean applied = false;
        for (InteractionHand hand : InteractionHand.values()) {
            PlayerAnimation pose = active(model, entity, hand);
            if (pose == null) {
                continue;
            }
            boolean mirrored = evaluate(entity, hand, pose);
            place(pose, PlayerBones.RIGHT_ARM, mirrored, model.rightArm, model.rightSleeve, false);
            place(pose, PlayerBones.LEFT_ARM, mirrored, model.leftArm, model.leftSleeve, false);
            if (!applied) {
                place(pose, PlayerBones.TORSO, mirrored, model.body, model.jacket, true);
                place(pose, PlayerBones.HEAD, mirrored, model.head, model.hat, true);
                place(pose, PlayerBones.RIGHT_LEG, mirrored, model.rightLeg, model.rightPants, true);
                place(pose, PlayerBones.LEFT_LEG, mirrored, model.leftLeg, model.leftPants, true);
            }
            applied = true;
        }
        return applied;
    }

    public static void applyItem(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm, PoseStack poseStack) {
        InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        PlayerAnimation pose = active(model, entity, hand);
        if (pose == null) {
            return;
        }
        evaluate(entity, hand, pose);
        boolean right = arm == HumanoidArm.RIGHT;
        ClientPlayerAnimations.transform(poseStack, POSE.get(right ? PlayerBones.RIGHT_ITEM : PlayerBones.LEFT_ITEM), right ? PlayerBones.RIGHT_ITEM_PIVOT : PlayerBones.LEFT_ITEM_PIVOT);
    }

    // A hand pose only counts when select() put it on that arm
    @Nullable
    private static PlayerAnimation active(HumanoidModel<?> model, LivingEntity entity, InteractionHand hand) {
        HumanoidModel.ArmPose armPose = AnimationQueries.isLeftArm(entity, hand) ? model.leftArmPose : model.rightArmPose;
        if (armPose != ONE_HANDED.getValue() && armPose != TWO_HANDED.getValue()) {
            return null;
        }
        return pose(entity, hand);
    }

    private static boolean evaluate(LivingEntity entity, InteractionHand hand, PlayerAnimation pose) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        AnimationQueries.fill(CONTEXT, entity, hand, partialTick);
        ClientPlayerAnimations.fillLook(CONTEXT, entity, partialTick);
        KeyframeAnimations.evaluate(pose.animation(), (entity.tickCount + partialTick) / 20.0F, CONTEXT, POSE);
        boolean mirrored = AnimationQueries.isLeftArm(entity, hand);
        if (mirrored) {
            POSE.mirror(PlayerBones.PAIRS);
        }
        return mirrored;
    }

    private static boolean replaces(PlayerAnimation pose, String bone, boolean mirrored) {
        return pose.mode(mirrored ? PlayerBones.counterpart(bone) : bone) == PlayerAnimation.BlendMode.REPLACE;
    }

    private static void rotate(PlayerAnimation pose, String bone, boolean mirrored, ModelPart part) {
        BonePose values = POSE.get(bone);
        if (!values.hasRotation) {
            return;
        }
        boolean replace = replaces(pose, bone, mirrored);
        PartPose initial = part.getInitialPose();
        part.xRot = (replace ? initial.xRot : part.xRot) + values.rotation.x;
        part.yRot = (replace ? initial.yRot : part.yRot) + values.rotation.y;
        part.zRot = (replace ? initial.zRot : part.zRot) + values.rotation.z;
    }

    private static void place(PlayerAnimation pose, String bone, boolean mirrored, ModelPart part, ModelPart overlay, boolean withRotation) {
        BonePose values = POSE.get(bone);
        if (!values.hasPosition && !values.hasScale && !(withRotation && values.hasRotation)) {
            return;
        }
        boolean replace = replaces(pose, bone, mirrored);
        PartPose initial = part.getInitialPose();
        if (values.hasPosition) {
            part.x = (replace ? initial.x : part.x) + values.position.x;
            part.y = (replace ? initial.y : part.y) + values.position.y;
            part.z = (replace ? initial.z : part.z) + values.position.z;
        }
        if (withRotation) {
            rotate(pose, bone, mirrored, part);
        }
        if (values.hasScale) {
            part.xScale = (replace ? 1.0F : part.xScale) * values.scale.x;
            part.yScale = (replace ? 1.0F : part.yScale) * values.scale.y;
            part.zScale = (replace ? 1.0F : part.zScale) * values.scale.z;
        }
        overlay.copyFrom(part);
    }

    @Nullable
    private static PlayerAnimation pose(LivingEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) {
            return null;
        }
        for (Resolver resolver : RESOLVERS) {
            ResourceLocation id = resolver.resolve(entity, hand, stack);
            if (id != null) {
                return PlayerAnimationLoader.ARM_POSES.get(id);
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @FunctionalInterface
    public interface Resolver {
        @Nullable
        ResourceLocation resolve(LivingEntity entity, InteractionHand hand, ItemStack stack);
    }
}
