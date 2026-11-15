package com.vidal1shere.jasper.entity;

import com.vidal1shere.jasper.ServerAnimations;
import com.vidal1shere.jasper.client.ClientAnimations;
import com.vidal1shere.jasper.layer.AnimationDriver;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface AnimatedEntity {
    AnimationDriver LOCOMOTION = entity -> ((AnimatedEntity) entity).isMoving() ? ((AnimatedEntity) entity).walkAnimation() : ((AnimatedEntity) entity).idleAnimation();

    ResourceLocation idleAnimation();

    ResourceLocation walkAnimation();

    // How far it moved during the last tick; the walk animation speed decays for several ticks after it stops
    default boolean isMoving() {
        LivingEntity entity = (LivingEntity) this;
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        return dx * dx + dz * dz > 1.0E-4;
    }

    default void setAnimation(ResourceLocation animation) {
        setAnimation(JasperLayers.BASE, animation);
    }

    default void setAnimation(Layer layer, ResourceLocation animation) {
        LivingEntity entity = (LivingEntity) this;
        if (entity.level().isClientSide()) {
            ClientAnimations.play(entity, layer, AnimationTarget.MODEL, animation, PlayOptions.DEFAULT);
        } else {
            ServerAnimations.play(entity, layer, AnimationTarget.MODEL, animation, PlayOptions.DEFAULT);
        }
    }

    default void stopAnimation() {
        stopAnimation(JasperLayers.BASE);
    }

    default void stopAnimation(Layer layer) {
        LivingEntity entity = (LivingEntity) this;
        if (entity.level().isClientSide()) {
            ClientAnimations.stop(entity, layer, AnimationTarget.MODEL);
        } else {
            ServerAnimations.stop(entity, layer, AnimationTarget.MODEL);
        }
    }

    @Nullable
    default AnimationDriver driver(Layer layer, AnimationTarget target) {
        return layer == JasperLayers.BASE && target == AnimationTarget.MODEL ? LOCOMOTION : null;
    }
}
