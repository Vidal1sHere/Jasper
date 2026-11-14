package com.vidal1shere.jasper.client.entity;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.keyframe.KeyframeAnimation;
import com.vidal1shere.jasper.client.attachment.EntityAnimationData;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.client.molang.AnimationQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.entity.AnimatedEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientEntityAnimations {
    private static final MolangContext CONTEXT = new MolangContext();
    private static final TrackFrame MAIN = new TrackFrame();
    private static final TrackFrame OVERLAY = new TrackFrame();
    private static final Set<String> BONES = new HashSet<>();

    public static void set(int entityId, boolean overlay, @Nullable ResourceLocation animation, long startTick, float speed) {
        EntityAnimationData data = ClientAttachments.of(entityId, ClientAttachments.ENTITY_ANIMATIONS);
        if (data == null) {
            return;
        }
        if (overlay) {
            data.overlay().set(animation, startTick, speed);
        } else {
            data.setAction(animation, startTick, speed);
        }
    }

    public static <T extends LivingEntity & AnimatedEntity> void apply(T entity, AnimatedModel<T> model, float ageInTicks, float netHeadYaw, float headPitch) {
        EntityAnimationData data = entity.getData(ClientAttachments.ENTITY_ANIMATIONS);
        float partialTick = ageInTicks - entity.tickCount;
        double now = entity.level().getGameTime() + partialTick;
        select(entity, data, now);
        data.main().update(now);
        data.overlay().update(now);
        AnimationQueries.fill(CONTEXT, entity, InteractionHand.MAIN_HAND, partialTick);
        MAIN.evaluate(data.main(), now, CONTEXT, null);
        OVERLAY.evaluate(data.overlay(), now, CONTEXT, null);
        apply(MAIN, model);
        look(model.head(), netHeadYaw, headPitch);
        apply(OVERLAY, model);
    }

    // Before the track updates, so a finished action hands over to the base instead of fading to nothing
    private static void select(AnimatedEntity entity, EntityAnimationData data, double now) {
        ResourceLocation action = data.action();
        if (action != null && alive(action, data.actionStart(), data.actionSpeed(), now)) {
            if (!data.showingAction() || !action.equals(data.shown()) || data.actionStart() != data.shownStart()) {
                data.main().set(action, data.actionStart(), data.actionSpeed());
                data.setShown(action, data.actionStart(), true);
            }
            return;
        }
        ResourceLocation base = entity.isMoving() ? entity.walkAnimation() : entity.idleAnimation();
        if (data.showingAction() || !base.equals(data.shown())) {
            long start = (long) now;
            data.main().set(base, start, 1.0F);
            data.setShown(base, start, false);
        }
    }

    private static boolean alive(ResourceLocation id, long start, float speed, double now) {
        JasperAnimation animation = JasperAnimationLoader.ENTITY.get(id);
        if (animation == null) {
            return false;
        }
        KeyframeAnimation keyframes = animation.keyframes();
        return keyframes.loop() != KeyframeAnimation.LoopType.PLAY_ONCE || now < start + keyframes.lengthInSeconds() * 20.0 / speed;
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

    // On the axes the main animations let follow the look, by the weight of those animations
    private static void look(ModelPart head, float netHeadYaw, float headPitch) {
        float pitch = 0.0F;
        float yaw = 0.0F;
        for (int i = 0; i < MAIN.size(); i++) {
            JasperAnimation animation = MAIN.animation(i);
            if (animation != null) {
                float weight = MAIN.weight(i);
                pitch += animation.look().pitch() ? weight : 0.0F;
                yaw += animation.look().yaw() ? weight : 0.0F;
            }
        }
        head.xRot += pitch * (headPitch * Mth.DEG_TO_RAD - head.getInitialPose().xRot);
        head.yRot += yaw * (netHeadYaw * Mth.DEG_TO_RAD - head.getInitialPose().yRot);
    }
}
