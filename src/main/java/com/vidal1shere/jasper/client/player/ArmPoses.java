package com.vidal1shere.jasper.client.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.animation.BlendMode;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.keyframe.KeyframeAnimations;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import com.vidal1shere.jasper.client.animation.pose.BoneTargets;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
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
    private static final BonePose CHANNELS = new BonePose();
    private static final List<Resolver> RESOLVERS = new CopyOnWriteArrayList<>();

    public static void addResolver(Resolver resolver) {
        RESOLVERS.add(resolver);
    }

    public static HumanoidModel.ArmPose select(AbstractClientPlayer player, InteractionHand hand, HumanoidModel.ArmPose vanilla) {
        if (vanilla != HumanoidModel.ArmPose.ITEM && vanilla != HumanoidModel.ArmPose.EMPTY) {
            return vanilla;
        }
        JasperAnimation pose = pose(player, hand);
        if (pose == null) {
            return vanilla;
        }
        return (pose.keyframes().bones().containsKey(PlayerBones.LEFT_ARM) ? TWO_HANDED : ONE_HANDED).getValue();
    }

    // Arm rotations go through the vanilla arm pose, so the vanilla swing still adds on top
    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        JasperAnimation pose = pose(entity, hand);
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
            JasperAnimation pose = active(model, entity, hand);
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
        JasperAnimation pose = active(model, entity, hand);
        if (pose == null) {
            return;
        }
        evaluate(entity, hand, pose);
        boolean right = arm == HumanoidArm.RIGHT;
        BoneTargets.transform(poseStack, POSE.get(right ? PlayerBones.RIGHT_ITEM : PlayerBones.LEFT_ITEM), right ? PlayerBones.RIGHT_ITEM_PIVOT : PlayerBones.LEFT_ITEM_PIVOT);
    }

    // A hand pose only counts when select() put it on that arm
    @Nullable
    private static JasperAnimation active(HumanoidModel<?> model, LivingEntity entity, InteractionHand hand) {
        HumanoidModel.ArmPose armPose = AnimationQueries.isLeftArm(entity, hand) ? model.leftArmPose : model.rightArmPose;
        if (armPose != ONE_HANDED.getValue() && armPose != TWO_HANDED.getValue()) {
            return null;
        }
        return pose(entity, hand);
    }

    private static boolean evaluate(LivingEntity entity, InteractionHand hand, JasperAnimation pose) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        AnimationQueries.fill(CONTEXT, entity, hand, partialTick);
        ClientPlayerAnimations.fillLook(CONTEXT, entity, partialTick);
        KeyframeAnimations.evaluate(pose.keyframes(), (entity.tickCount + partialTick) / 20.0F, CONTEXT, POSE);
        boolean mirrored = AnimationQueries.isLeftArm(entity, hand);
        if (mirrored) {
            POSE.mirror(PlayerBones.PAIRS);
        }
        return mirrored;
    }

    private static BlendMode mode(JasperAnimation pose, String bone, boolean mirrored) {
        return pose.mode(mirrored ? PlayerBones.counterpart(bone) : bone);
    }

    private static void rotate(JasperAnimation pose, String bone, boolean mirrored, ModelPart part) {
        CHANNELS.set(POSE.get(bone));
        CHANNELS.hasPosition = false;
        CHANNELS.hasScale = false;
        BoneTargets.apply(part, null, CHANNELS, mode(pose, bone, mirrored));
    }

    private static void place(JasperAnimation pose, String bone, boolean mirrored, ModelPart part, ModelPart overlay, boolean withRotation) {
        CHANNELS.set(POSE.get(bone));
        CHANNELS.hasRotation &= withRotation;
        BoneTargets.apply(part, overlay, CHANNELS, mode(pose, bone, mirrored));
    }

    @Nullable
    private static JasperAnimation pose(LivingEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) {
            return null;
        }
        for (Resolver resolver : RESOLVERS) {
            ResourceLocation id = resolver.resolve(entity, hand, stack);
            if (id != null) {
                return JasperAnimationLoader.ARM_POSE.get(id);
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
