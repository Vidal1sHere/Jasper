package com.vidal1shere.jasper.client.molang;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@OnlyIn(Dist.CLIENT)
public final class BuiltinQueries {
    private static final Vector3f LOOK = new Vector3f();
    private static final List<QuerySource> SOURCES = new CopyOnWriteArrayList<>();

    public static void addSource(QuerySource source) {
        SOURCES.add(source);
    }

    public static void fill(MolangContext context, LivingEntity entity, InteractionHand hand, float partialTick) {
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        context.set(MolangQuery.GROUND_SPEED, (float) Math.sqrt(dx * dx + dz * dz) * 20.0F);
        context.set(MolangQuery.VERTICAL_SPEED, (float) (entity.getY() - entity.yo) * 20.0F);
        context.set(MolangQuery.IS_ON_GROUND, entity.onGround());
        context.set(MolangQuery.IS_SPRINTING, entity.isSprinting());
        context.set(MolangQuery.IS_SNEAKING, entity.isCrouching());
        context.set(MolangQuery.IS_SWIMMING, entity.isVisuallySwimming());
        context.set(MolangQuery.IS_RIDING, entity.isPassenger());
        context.set(MolangQuery.HEAD_X_ROTATION, Mth.lerp(partialTick, entity.xRotO, entity.getXRot()));
        context.set(MolangQuery.HEAD_Y_ROTATION, Mth.wrapDegrees(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot)));
        context.set(MolangQuery.LOOK_X_ROTATION, context.query(MolangQuery.HEAD_X_ROTATION));
        context.set(MolangQuery.LOOK_Y_ROTATION, context.query(MolangQuery.HEAD_Y_ROTATION));
        context.set(MolangQuery.HAND_SIDE, isLeftArm(entity, hand) ? -1.0F : 1.0F);
        for (QuerySource source : SOURCES) {
            source.fill(context, entity, hand, partialTick);
        }
    }

    // Look direction in the space of the rendered model, so body rotations such as elytra count
    public static void fillLook(MolangContext context, LivingEntity entity, float partialTick, Matrix3f world, Matrix3f model) {
        float pitch = entity.getViewXRot(partialTick) * Mth.DEG_TO_RAD;
        float yaw = -entity.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        LOOK.set(Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        world.transform(LOOK);
        model.transformTranspose(LOOK);
        float horizontal = Mth.sqrt(LOOK.x * LOOK.x + LOOK.z * LOOK.z);
        float lookYaw = horizontal < 1.0E-4F ? 0.0F : (float) Mth.atan2(-LOOK.x, -LOOK.z) * Mth.RAD_TO_DEG * Math.min(1.0F, horizontal * 10.0F);
        context.set(MolangQuery.LOOK_X_ROTATION, (float) Math.asin(Mth.clamp(LOOK.y, -1.0F, 1.0F)) * Mth.RAD_TO_DEG);
        context.set(MolangQuery.LOOK_Y_ROTATION, lookYaw);
    }

    public static boolean isLeftArm(LivingEntity entity, InteractionHand hand) {
        HumanoidArm main = entity.getMainArm();
        return (hand == InteractionHand.MAIN_HAND ? main : main.getOpposite()) == HumanoidArm.LEFT;
    }
}
