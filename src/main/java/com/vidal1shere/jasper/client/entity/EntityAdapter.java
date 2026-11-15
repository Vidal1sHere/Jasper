package com.vidal1shere.jasper.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.BlendMode;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.LayerFrames;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.entity.AnimatedEntity;
import com.vidal1shere.jasper.layer.AnimationDriver;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Set;
import java.util.function.UnaryOperator;

@OnlyIn(Dist.CLIENT)
public final class EntityAdapter {
    private static final LayerFrames FRAMES = new LayerFrames(null, null);
    private static final float MODEL_UNIT = 1.0F / 16.0F;

    // The frames stay valid from setupAnim to the render of the same model, where the locators are captured
    @Nullable
    private static LivingEntity frameEntity;
    @Nullable
    private static AnimatedModel<?> frameModel;

    public static <T extends LivingEntity & AnimatedEntity> void apply(T entity, AnimatedModel<T> model, float ageInTicks, float netHeadYaw, float headPitch) {
        ClientAnimationData data = entity.getData(ClientAttachments.ANIMATIONS);
        float partialTick = ageInTicks - entity.tickCount;
        double now = entity.level().getGameTime() + partialTick;
        drive(entity, data, now);
        FRAMES.evaluate(entity, AnimationTarget.MODEL, InteractionHand.MAIN_HAND, partialTick);
        FRAMES.apply(model.parts()::part, Set.of());
        look(model.head(), netHeadYaw, headPitch);
        frameEntity = entity;
        frameModel = model;
    }

    public static void captureLocators(AnimatedModel<?> model, PoseStack poseStack) {
        if (model != frameModel || frameEntity == null) {
            return;
        }
        Matrix4f root = new Matrix4f(poseStack.last().pose());
        LocatorCapture.capture(frameEntity, AnimationTarget.MODEL, FRAMES.frames(), FRAMES.size(), bone -> model.matrix(root, bone), UnaryOperator.identity(), MODEL_UNIT);
        frameEntity = null;
    }

    private static <T extends LivingEntity & AnimatedEntity> void drive(T entity, ClientAnimationData data, double now) {
        for (Layer layer : JasperLayers.sorted()) {
            AnimationDriver driver = entity.driver(layer, AnimationTarget.MODEL);
            if (driver != null) {
                data.track(AnimationTarget.MODEL, layer, JasperAnimationLoader.ENTITY).drive(driver.pick(entity), now);
            }
        }
    }

    private static void look(ModelPart head, float netHeadYaw, float headPitch) {
        float pitch = 0.0F;
        float yaw = 0.0F;
        float visible = 1.0F;
        for (int f = FRAMES.size() - 1; f >= 0 && visible > 0.0F; f--) {
            TrackFrame frame = FRAMES.frame(f);
            float covered = 0.0F;
            for (int i = 0; i < frame.size(); i++) {
                JasperAnimation animation = frame.animation(i);
                if (animation == null) {
                    continue;
                }
                float weight = frame.weight(i);
                pitch += animation.look().pitch() ? visible * weight : 0.0F;
                yaw += animation.look().yaw() ? visible * weight : 0.0F;
                covered += animation.mode() == BlendMode.REPLACE ? weight : 0.0F;
            }
            visible *= 1.0F - Math.min(1.0F, covered);
        }
        head.xRot += pitch * (headPitch * Mth.DEG_TO_RAD - head.getInitialPose().xRot);
        head.yRot += yaw * (netHeadYaw * Mth.DEG_TO_RAD - head.getInitialPose().yRot);
    }
}
