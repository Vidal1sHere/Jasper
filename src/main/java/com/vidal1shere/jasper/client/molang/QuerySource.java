package com.vidal1shere.jasper.client.molang;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface QuerySource {
    void fill(MolangContext context, LivingEntity entity, InteractionHand hand, float partialTick);
}
