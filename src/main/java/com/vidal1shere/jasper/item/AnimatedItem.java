package com.vidal1shere.jasper.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// Implemented by the Item; its 3D model comes from a jasper:animated item model, chosen by that JSON per display context
public interface AnimatedItem {
    // Null keeps the texture of the item model; the holder and the hand are null where nobody holds the stack (ground, frames, menus)
    @Nullable
    default ResourceLocation texture(ItemStack stack, @Nullable LivingEntity holder, @Nullable InteractionHand hand) {
        return null;
    }
}
