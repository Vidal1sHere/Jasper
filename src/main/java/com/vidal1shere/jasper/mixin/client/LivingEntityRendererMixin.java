package com.vidal1shere.jasper.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vidal1shere.jasper.client.effect.afterimage.Afterimages;
import com.vidal1shere.jasper.client.player.PlayerAdapter;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void jasper$captureWorldRotation(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof Player) {
            PlayerAdapter.captureWorld(poseStack);
        }
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;prepareMobModel(Lnet/minecraft/world/entity/Entity;FFF)V"))
    private void jasper$preparePlayerAnimation(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof Player) {
            PlayerAdapter.prepare(entity, partialTicks, poseStack);
            if (PlayerAdapter.isAnimating(entity)) {
                PlayerAdapter.applyRoot(poseStack);
            }
        }
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void jasper$capturePlayerLocators(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof Player && PlayerAdapter.isAnimating(entity) && ((LivingEntityRenderer<?, ?>) (Object) this).getModel() instanceof PlayerModel<?> model) {
            PlayerAdapter.captureModelLocators(entity, model, poseStack);
        }
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void jasper$captureAfterimages(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (Afterimages.wants(entity)) {
            LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
            Afterimages.capture(entity, renderer.getModel(), poseStack, packedLight, renderer.getTextureLocation(entity));
        }
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void jasper$finishPlayerAnimation(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof Player) {
            PlayerAdapter.finish();
        }
    }
}
