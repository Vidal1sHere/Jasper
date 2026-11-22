package com.vidal1shere.jasper.client.item.attachment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// The holder and the hand are null on the ground, in frames and in the GUI
@OnlyIn(Dist.CLIENT)
public record AttachmentContext(ItemStack stack, @Nullable LivingEntity holder, @Nullable InteractionHand hand, ItemDisplayContext displayContext, float partialTick) {
}
