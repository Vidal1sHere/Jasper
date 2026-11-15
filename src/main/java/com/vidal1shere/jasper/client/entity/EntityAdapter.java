package com.vidal1shere.jasper.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.BlendMode;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.attachment.LayerState;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.entity.AnimatedEntity;
import com.vidal1shere.jasper.layer.AnimationDriver;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

@OnlyIn(Dist.CLIENT)
public final class EntityAdapter {
    private static final MolangContext CONTEXT = new MolangContext();
    private static final List<TrackFrame> FRAMES = new ArrayList<>();
    // Model parts and their locators are in pixels
    private static final float MODEL_UNIT = 1.0F / 16.0F;

    // The frames stay valid from setupAnim to the render of the same model, where the locators are captured
    @Nullable
    private static LivingEntity frameEntity;
    @Nullable
    private static AnimatedModel<?> frameModel;
    private static int frameCount;

    public static <T extends LivingEntity & AnimatedEntity> void apply(T entity, AnimatedModel<T> model, float ageInTicks, float netHeadYaw, float headPitch) {
        ClientAnimationData data = entity.getData(ClientAttachments.ANIMATIONS);
        float partialTick = ageInTicks - entity.tickCount;
        double now = entity.level().getGameTime() + partialTick;
        drive(entity, data, now);
        BuiltinQueries.fill(CONTEXT, entity, InteractionHand.MAIN_HAND, partialTick);
        int frames = 0;
        for (Layer layer : JasperLayers.sorted()) {
            LayerState track = data.existing(AnimationTarget.MODEL, layer);
            if (track == null) {
                continue;
            }
            track.track().update(now);
            if (!track.track().isActive()) {
                continue;
            }
            if (FRAMES.size() <= frames) {
                FRAMES.add(new TrackFrame());
            }
            TrackFrame frame = FRAMES.get(frames++);
            frame.evaluate(track.track(), now, CONTEXT, null);
            model.parts().apply(frame, Set.of());
        }
        look(frames, model.head(), netHeadYaw, headPitch);
        frameEntity = entity;
        frameModel = model;
        frameCount = frames;
    }

    public static void captureLocators(AnimatedModel<?> model, PoseStack poseStack) {
        if (model != frameModel || frameEntity == null) {
            return;
        }
        Matrix4f root = new Matrix4f(poseStack.last().pose());
        LocatorCapture.capture(frameEntity, AnimationTarget.MODEL, FRAMES, frameCount, bone -> model.matrix(root, bone), UnaryOperator.identity(), MODEL_UNIT);
        frameEntity = null;
    }

    // Before the tracks update, so a play once that just ended hands over to the driver instead of fading to nothing
    private static <T extends LivingEntity & AnimatedEntity> void drive(T entity, ClientAnimationData data, double now) {
        for (Layer layer : JasperLayers.sorted()) {
            AnimationDriver driver = entity.driver(layer, AnimationTarget.MODEL);
            if (driver == null) {
                continue;
            }
            LayerState track = data.track(AnimationTarget.MODEL, layer, JasperAnimationLoader.ENTITY);
            if (track.played() != null) {
                if (alive(track.played(), track.playedStart(), track.playedOptions(), now)) {
                    continue;
                }
                track.endPlay();
            }
            ResourceLocation pick = driver.pick(entity);
            if (!Objects.equals(pick, track.driven())) {
                track.track().set(pick, (long) now, 1.0F, false);
                track.setDriven(pick);
            }
        }
    }

    private static boolean alive(ResourceLocation id, long start, PlayOptions options, double now) {
        JasperAnimation animation = JasperAnimationLoader.ENTITY.get(id);
        if (animation == null) {
            return false;
        }
        return options.loop() || animation.loop() != JasperAnimation.LoopType.PLAY_ONCE || now < start + animation.length() * 20.0 / options.speed();
    }

    // From the top layer down, each one hides by its replace weight the look of the layers below it
    private static void look(int frames, ModelPart head, float netHeadYaw, float headPitch) {
        float pitch = 0.0F;
        float yaw = 0.0F;
        float visible = 1.0F;
        for (int f = frames - 1; f >= 0 && visible > 0.0F; f--) {
            TrackFrame frame = FRAMES.get(f);
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
