package com.vidal1shere.jasper.layer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface AnimationDriver {
    @Nullable
    ResourceLocation pick(LivingEntity entity);
}
