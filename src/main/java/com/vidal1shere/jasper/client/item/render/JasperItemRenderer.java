package com.vidal1shere.jasper.client.item.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.effect.afterimage.Afterimages;
import com.vidal1shere.jasper.client.item.ItemAdapter;
import com.vidal1shere.jasper.client.item.model.AnimatedItemModel;
import com.vidal1shere.jasper.client.item.model.ItemAttachment;
import com.vidal1shere.jasper.client.item.model.JasperItemModel;
import com.vidal1shere.jasper.item.AnimatedItem;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class JasperItemRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PoseStack SCRATCH = new PoseStack();
    private static final Map<ModelLayerLocation, Optional<AnimatedItemModel>> MODELS = new HashMap<>();

    // renderByItem gets no entity, so the hand renderers set it
    @Nullable
    private static LivingEntity holder;
    @Nullable
    private static InteractionHand holderHand;

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> MODELS.clear());
    }

    public static void setHolder(LivingEntity entity, InteractionHand hand) {
        holder = entity;
        holderHand = hand;
    }

    public static void clearHolder() {
        holder = null;
        holderHand = null;
    }

    // False when the model of this display context is not a jasper:animated one, so the caller draws the item
    public static boolean render(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        JasperItemModel baked = resolve(stack, displayContext);
        if (baked == null) {
            return false;
        }
        ModelLayerLocation layer = baked.model();
        if (stack.getItem() instanceof AnimatedItem item) {
            String override = item.modelLayer(stack, holder, holderHand);
            if (override != null) {
                layer = new ModelLayerLocation(layer.getModel(), override);
            }
        }
        AnimatedItemModel model = MODELS.computeIfAbsent(layer, JasperItemRenderer::bake).orElse(null);
        if (model == null) {
            return true;
        }
        ResourceLocation texture = baked.texture();
        if (stack.getItem() instanceof AnimatedItem item) {
            ResourceLocation override = item.texture(stack, holder, holderHand);
            if (override != null) {
                texture = override;
            }
        }
        poseStack.pushPose();
        // Placed like GeckoLib draws a geo item, so Blockbench GeckoLib display transforms fit
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        model.parts().root().getAllParts().forEach(ModelPart::resetPose);
        if (holder != null && holderHand != null) {
            ItemAdapter.applyParts(holder, holderHand, model.parts(), displayContext.firstPerson(), poseStack, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        }
        if (holder != null && holderHand != null && Afterimages.wants(holder, AnimationTarget.of(holderHand), displayContext.firstPerson())) {
            Afterimages.captureItem(holder, AnimationTarget.of(holderHand), displayContext.firstPerson(), model, poseStack, packedLight, texture);
        }
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, model.renderType(texture), false, stack.hasFoil());
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay);
        for (Map.Entry<ItemAttachment, BakedModel> attachment : baked.attachments().entrySet()) {
            renderAttachment(stack, model, attachment.getKey(), attachment.getValue(), poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
        return true;
    }

    private static void renderAttachment(ItemStack stack, AnimatedItemModel model, ItemAttachment attachment, BakedModel attached, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        List<ModelPart> chain = model.parts().chain(attachment.bone());
        if (chain == null) {
            return;
        }
        poseStack.pushPose();
        for (ModelPart part : chain) {
            part.translateAndRotate(poseStack);
        }
        // Back to item space at the bone, Y up, then the item model spans 0 to 16 around it
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(attachment.translation().x / 16.0F, attachment.translation().y / 16.0F, attachment.translation().z / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationXYZ(attachment.rotation().x * Mth.DEG_TO_RAD, attachment.rotation().y * Mth.DEG_TO_RAD, attachment.rotation().z * Mth.DEG_TO_RAD));
        poseStack.scale(attachment.scale(), attachment.scale(), attachment.scale());
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        for (BakedModel pass : attached.getRenderPasses(stack, true)) {
            for (RenderType type : pass.getRenderTypes(stack, true)) {
                renderer.renderModelLists(pass, stack, packedLight, packedOverlay, poseStack, ItemRenderer.getFoilBufferDirect(buffer, type, true, stack.hasFoil()));
            }
        }
        poseStack.popPose();
    }

    // renderByItem gets no baked model, so it is resolved again for this display context
    @Nullable
    private static JasperItemModel resolve(ItemStack stack, ItemDisplayContext displayContext) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, holder, 0);
        SCRATCH.setIdentity();
        return model.applyTransform(displayContext, SCRATCH, false) instanceof JasperItemModel jasper ? jasper : null;
    }

    private static Optional<AnimatedItemModel> bake(ModelLayerLocation layer) {
        try {
            return Optional.of(new AnimatedItemModel(Minecraft.getInstance().getEntityModels().bakeLayer(layer), layer.toString()));
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Item model layer {} is not registered, the items using it render nothing", layer);
            return Optional.empty();
        }
    }
}
