package com.vidal1shere.jasper.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.effect.trail.Trails;
import com.vidal1shere.jasper.client.item.ItemAdapter;
import com.vidal1shere.jasper.client.item.render.JasperItemRenderer;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void jasper$setItemHolder(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
        JasperItemRenderer.setHolder(player, hand);
        ItemAdapter.drive(player, hand, stack, BuiltinQueries.isLeftArm(player, hand) ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, partialTicks);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void jasper$clearItemHolder(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
        JasperItemRenderer.clearHolder();
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    ordinal = 1
            )
    )
    private void jasper$applyItemAnimation(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
        ItemAdapter.apply(player, hand, poseStack, buffer, partialTicks);
    }

    @Inject(method = "renderHandsWithItems", at = @At("TAIL"))
    private void jasper$renderHandTrails(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer, LocalPlayer player, int combinedLight, CallbackInfo ci) {
        Trails.onHandsRendered(partialTicks);
    }
}
