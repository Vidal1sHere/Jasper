package com.vidal1shere.jasper.client.entity;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.BlendMode;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.keyframe.KeyframeAnimation;
import com.vidal1shere.jasper.client.attachment.LayerData;
import com.vidal1shere.jasper.client.attachment.layer.LayerTrack;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientEntityAnimations {
    private static final MolangContext CONTEXT = new MolangContext();
    private static final List<TrackFrame> FRAMES = new ArrayList<>();
    private static final Set<String> BONES = new HashSet<>();

    public static <T extends LivingEntity & AnimatedEntity> void apply(T entity, AnimatedModel<T> model, float ageInTicks, float netHeadYaw, float headPitch) {
        LayerData data = entity.getData(ClientAttachments.LAYERS);
        float partialTick = ageInTicks - entity.tickCount;
        double now = entity.level().getGameTime() + partialTick;
        drive(entity, data, now);
        AnimationQueries.fill(CONTEXT, entity, InteractionHand.MAIN_HAND, partialTick);
        int frames = 0;
        for (Layer layer : JasperLayers.sorted()) {
            LayerTrack track = data.existing(AnimationTarget.MODEL, layer);
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
            apply(frame, model);
        }
        look(frames, model.head(), netHeadYaw, headPitch);
    }

    // Before the tracks update, so a play once that just ended hands over to the driver instead of fading to nothing
    private static <T extends LivingEntity & AnimatedEntity> void drive(T entity, LayerData data, double now) {
        for (Layer layer : JasperLayers.sorted()) {
            AnimationDriver driver = entity.driver(layer, AnimationTarget.MODEL);
            if (driver == null) {
                continue;
            }
            LayerTrack track = data.track(AnimationTarget.MODEL, layer, JasperAnimationLoader.ENTITY);
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
        KeyframeAnimation keyframes = animation.keyframes();
        return options.loop() || keyframes.loop() != KeyframeAnimation.LoopType.PLAY_ONCE || now < start + keyframes.lengthInSeconds() * 20.0 / options.speed();
    }

    private static void apply(TrackFrame frame, AnimatedModel<?> model) {
        BONES.clear();
        for (int i = 0; i < frame.size(); i++) {
            JasperAnimation animation = frame.animation(i);
            if (animation != null) {
                BONES.addAll(animation.keyframes().bones().keySet());
            }
        }
        for (String bone : BONES) {
            ModelPart part = model.part(bone);
            if (part != null) {
                frame.apply(bone, part, null);
            }
        }
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
