package com.vidal1shere.jasper.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// Asked every frame the stack renders in a hand; a play on the layer overrides it while it plays
@FunctionalInterface
public interface ItemAnimationDriver {
    @Nullable
    ResourceLocation pick(ItemStack stack, LivingEntity holder, InteractionHand hand, ItemDisplayContext displayContext);
}
