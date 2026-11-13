package com.vidal1shere.jasper.client.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationPose;
import com.vidal1shere.jasper.client.animation.BonePose;
import com.vidal1shere.jasper.client.attachment.PlayerAnimationData;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClientPlayerAnimations {
    private static final MolangContext CONTEXT = new MolangContext();
    private static final AnimationPose CURRENT = new AnimationPose();
    private static final AnimationPose PREVIOUS = new AnimationPose();
    private static final BonePose VANILLA = new BonePose();
    private static final BonePose FROM = new BonePose();
    private static final BonePose TO = new BonePose();
    private static final BonePose IDENTITY = new BonePose();
    private static final Quaternionf ROTATION = new Quaternionf();

    private static final Matrix3f WORLD = new Matrix3f();
    private static final Matrix3f MODEL = new Matrix3f();

    private static int frameEntity = -1;
    private static int lookEntity = -1;
    @Nullable
    private static PlayerAnimation current;
    @Nullable
    private static PlayerAnimation previous;
    private static float weight;
    private static boolean mirrored;

    public static void play(int entityId, InteractionHand hand, ResourceLocation animation, long startTick, float speed) {
        PlayerAnimationData data = ClientAttachments.of(entityId, ClientAttachments.PLAYER_ANIMATIONS);
        if (data != null) {
            data.setHand(hand);
            data.playback().set(animation, startTick, speed);
        }
    }

    public static void stop(int entityId, long tick) {
        PlayerAnimationData data = ClientAttachments.of(entityId, ClientAttachments.PLAYER_ANIMATIONS);
        if (data != null) {
            data.playback().set(null, tick, 1.0F);
        }
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
        float fade = state.playback().fadeOut(state.playback().current(), now);
        if (fade > 0.0F) {
            fill(entity, state, partialTick);
            weight = 1.0F - fade;
            current = null;
            previous = state.playback().evaluateLastFrame(state.playback().current(), CONTEXT, PREVIOUS);
            if (previous != null) {
                mirror(entity, state);
                frameEntity = entity.getId();
            }
            return;
        }
        if (!state.playback().isActive(now)) {
            return;
        }
        fill(entity, state, partialTick);
        weight = state.playback().weight(now);
        current = state.playback().evaluate(state.playback().current(), now, CONTEXT, CURRENT);
        previous = weight < 1.0F ? state.playback().evaluate(state.playback().previous(), now, CONTEXT, PREVIOUS) : null;
        if (current != null || previous != null) {
            mirror(entity, state);
            frameEntity = entity.getId();
        }
    }

    private static void fill(LivingEntity entity, PlayerAnimationData state, float partialTick) {
        AnimationQueries.fill(CONTEXT, entity, state.hand(), partialTick);
        fillLook(CONTEXT, entity, partialTick);
    }

    // Outside of a render the look queries keep the head rotation
    public static void fillLook(MolangContext context, LivingEntity entity, float partialTick) {
        if (lookEntity == entity.getId()) {
            AnimationQueries.fillLook(context, entity, partialTick, WORLD, MODEL);
        }
    }

    // Animations are written for the right arm and mirrored when the hand is on the left one
    private static void mirror(LivingEntity entity, PlayerAnimationData state) {
        mirrored = AnimationQueries.isLeftArm(entity, state.hand());
        if (mirrored) {
            CURRENT.mirror(PlayerBones.PAIRS);
            PREVIOUS.mirror(PlayerBones.PAIRS);
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
        transform(poseStack, blend(PlayerBones.ROOT), PlayerBones.ROOT_PIVOT);
    }

    public static void applyItem(HumanoidArm arm, PoseStack poseStack) {
        boolean right = arm == HumanoidArm.RIGHT;
        transform(poseStack, blend(right ? PlayerBones.RIGHT_ITEM : PlayerBones.LEFT_ITEM), right ? PlayerBones.RIGHT_ITEM_PIVOT : PlayerBones.LEFT_ITEM_PIVOT);
    }

    public static void applyModel(PlayerModel<?> model) {
        part(PlayerBones.HEAD, model.head, model.hat);
        part(PlayerBones.TORSO, model.body, model.jacket);
        part(PlayerBones.RIGHT_ARM, model.rightArm, model.rightSleeve);
        part(PlayerBones.LEFT_ARM, model.leftArm, model.leftSleeve);
        part(PlayerBones.RIGHT_LEG, model.rightLeg, model.rightPants);
        part(PlayerBones.LEFT_LEG, model.leftLeg, model.leftPants);
    }

    private static BonePose blend(String bone) {
        FROM.set(previous == null ? IDENTITY : PREVIOUS.get(bone));
        FROM.lerp(current == null ? IDENTITY : CURRENT.get(bone), weight);
        return FROM;
    }

    public static void transform(PoseStack poseStack, BonePose pose, Vector3f pivot) {
        if (!pose.isAnimated()) {
            return;
        }
        poseStack.translate((pivot.x + pose.position.x) / 16.0F, (pivot.y + pose.position.y) / 16.0F, (pivot.z + pose.position.z) / 16.0F);
        poseStack.mulPose(ROTATION.rotationZYX(pose.rotation.z, pose.rotation.y, pose.rotation.x));
        poseStack.scale(pose.scale.x, pose.scale.y, pose.scale.z);
        poseStack.translate(-pivot.x / 16.0F, -pivot.y / 16.0F, -pivot.z / 16.0F);
    }

    private static void part(String bone, ModelPart part, ModelPart overlay) {
        BonePose from = previous == null ? null : PREVIOUS.get(bone);
        BonePose to = current == null ? null : CURRENT.get(bone);
        if ((from == null || !from.isAnimated()) && (to == null || !to.isAnimated())) {
            return;
        }
        read(part, VANILLA);
        PartPose initial = part.getInitialPose();
        resolve(previous, bone, from, initial, FROM);
        resolve(current, bone, to, initial, TO);
        FROM.lerp(TO, weight);
        write(FROM, part);
        overlay.copyFrom(part);
    }

    private static void resolve(@Nullable PlayerAnimation animation, String bone, @Nullable BonePose pose, PartPose initial, BonePose out) {
        out.set(VANILLA);
        if (animation == null || pose == null) {
            return;
        }
        boolean replace = animation.mode(mirrored ? PlayerBones.counterpart(bone) : bone) == PlayerAnimation.BlendMode.REPLACE;
        if (pose.hasPosition) {
            if (replace) {
                out.position.set(initial.x, initial.y, initial.z);
            }
            out.position.add(pose.position);
        }
        if (pose.hasRotation) {
            if (replace) {
                out.rotation.set(initial.xRot, initial.yRot, initial.zRot);
            }
            out.rotation.add(pose.rotation);
        }
        if (pose.hasScale) {
            if (replace) {
                out.scale.set(pose.scale);
            } else {
                out.scale.mul(pose.scale);
            }
        }
    }

    private static void read(ModelPart part, BonePose out) {
        out.reset();
        out.position.set(part.x, part.y, part.z);
        out.rotation.set(part.xRot, part.yRot, part.zRot);
        out.scale.set(part.xScale, part.yScale, part.zScale);
    }

    private static void write(BonePose pose, ModelPart part) {
        part.x = pose.position.x;
        part.y = pose.position.y;
        part.z = pose.position.z;
        part.xRot = pose.rotation.x;
        part.yRot = pose.rotation.y;
        part.zRot = pose.rotation.z;
        part.xScale = pose.scale.x;
        part.yScale = pose.scale.y;
        part.zScale = pose.scale.z;
    }
}
