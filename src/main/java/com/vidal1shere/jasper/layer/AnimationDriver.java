package com.vidal1shere.jasper.layer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

// Picks the animation of a layer from the state of the entity, asked every frame; a play on that layer overrides it while it plays
@FunctionalInterface
public interface AnimationDriver {
    @Nullable
    ResourceLocation pick(LivingEntity entity);
}
