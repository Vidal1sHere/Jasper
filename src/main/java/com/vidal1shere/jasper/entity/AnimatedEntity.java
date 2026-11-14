package com.vidal1shere.jasper.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

// Read on the client every frame to pick the base animation, so the base never needs a packet
public interface AnimatedEntity {
    ResourceLocation idleAnimation();

    ResourceLocation walkAnimation();

    // How far it moved during the last tick; the walk animation speed decays for several ticks after it stops
    default boolean isMoving() {
        LivingEntity entity = (LivingEntity) this;
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        return dx * dx + dz * dz > 1.0E-4;
    }
}
