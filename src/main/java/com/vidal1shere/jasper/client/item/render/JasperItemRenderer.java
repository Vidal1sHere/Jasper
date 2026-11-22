package com.vidal1shere.jasper.client.item.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.effect.afterimage.Afterimages;
import com.vidal1shere.jasper.client.item.ItemAdapter;
import com.vidal1shere.jasper.client.item.attachment.AttachmentContext;
import com.vidal1shere.jasper.client.item.attachment.AttachmentPlacement;
import com.vidal1shere.jasper.client.item.attachment.JasperModelAttachment;
import com.vidal1shere.jasper.client.item.model.AnimatedItemModel;
import com.vidal1shere.jasper.client.item.model.JasperItemModel;
import com.vidal1shere.jasper.client.item.model.JasperItemModels;
import com.vidal1shere.jasper.item.AnimatedItem;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class JasperItemRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PoseStack SCRATCH = new PoseStack();
    private static final Map<ModelLayerLocation, Optional<AnimatedItemModel>> MODELS = new HashMap<>();
    private static final int MAX_CHAIN = 16;
    private static final Set<JasperModelAttachment> CLASHES = new HashSet<>();

    // renderByItem gets no entity, so the hand renderers set it
    @Nullable
    private static LivingEntity holder;
    @Nullable
    private static InteractionHand holderHand;

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            MODELS.clear();
            CLASHES.clear();
        });
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
        List<JasperModelAttachment> attachments = attachments(model, baked);
        Map<String, ModelPart> poses = new HashMap<>();
        for (JasperModelAttachment attachment : attachments) {
            attachment.pose().resetPose();
            if (!model.parts().has(attachment.name())) {
                poses.put(attachment.name(), attachment.pose());
            } else if (CLASHES.add(attachment)) {
                LOGGER.error("{} has an attachment named '{}' like one of its parts, its animations move the part", model.parts().owner(), attachment.name());
            }
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        if (holder != null && holderHand != null) {
            ItemAdapter.applyParts(holder, holderHand, model.parts(), poses, displayContext.firstPerson(), poseStack, partialTick);
        }
        AttachmentContext context = new AttachmentContext(stack, holder, holderHand, displayContext, partialTick);
        if (holder != null && holderHand != null && Afterimages.wants(holder, AnimationTarget.of(holderHand), displayContext.firstPerson())) {
            List<Afterimages.AttachmentDraw> draws = attachments.stream()
                    .map(attachment -> new Afterimages.AttachmentDraw(attachment.name(), attachment.texture(context), recording -> renderAttachment(model, attachments, attachment, context, poseStack, recording, packedLight, packedOverlay)))
                    .toList();
            Afterimages.captureItem(holder, AnimationTarget.of(holderHand), displayContext.firstPerson(), model, poseStack, packedLight, texture, draws);
        }
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, model.renderType(texture), false, stack.hasFoil());
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay);
        for (JasperModelAttachment attachment : attachments) {
            renderAttachment(model, attachments, attachment, context, poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
        return true;
    }

    private static void renderAttachment(AnimatedItemModel model, List<JasperModelAttachment> attachments, JasperModelAttachment attachment, AttachmentContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (place(model, attachments, attachment, poseStack, 0)) {
            attachment.render(context, poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    // The JSON ones, then the ones of the model class; a code attachment named like a JSON one is dropped
    private static List<JasperModelAttachment> attachments(AnimatedItemModel model, JasperItemModel baked) {
        if (model.attachments().isEmpty()) {
            return baked.attachments();
        }
        List<JasperModelAttachment> all = new ArrayList<>(baked.attachments());
        for (JasperModelAttachment attachment : model.attachments()) {
            if (baked.attachments().stream().noneMatch(other -> other.name().equals(attachment.name()))) {
                all.add(attachment);
            } else if (CLASHES.add(attachment)) {
                LOGGER.error("{} declares an attachment named '{}' in code and in its item JSON, the JSON one is used", model.parts().owner(), attachment.name());
            }
        }
        return all;
    }

    // At the transform of its bone, a model part first, else another attachment, then its own placement; false when the bone does not exist or the chain loops
    private static boolean place(AnimatedItemModel model, List<JasperModelAttachment> attachments, JasperModelAttachment attachment, PoseStack poseStack, int depth) {
        if (depth > MAX_CHAIN) {
            if (CLASHES.add(attachment)) {
                LOGGER.error("{} has attachments whose bones loop through '{}', they are not drawn", model.parts().owner(), attachment.name());
            }
            return false;
        }
        AttachmentPlacement placement = attachment.placement();
        List<ModelPart> chain = model.parts().has(placement.bone()) ? model.parts().chain(placement.bone()) : null;
        if (chain != null) {
            for (ModelPart part : chain) {
                part.translateAndRotate(poseStack);
            }
            // Back to item space at the bone, Y up
            poseStack.scale(-1.0F, -1.0F, 1.0F);
        } else {
            JasperModelAttachment parent = attachments.stream().filter(other -> other != attachment && other.name().equals(placement.bone())).findFirst().orElse(null);
            if (parent == null || !place(model, attachments, parent, poseStack, depth + 1)) {
                return false;
            }
        }
        poseStack.translate(placement.translation().x / 16.0F, placement.translation().y / 16.0F, placement.translation().z / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationXYZ(placement.rotation().x * Mth.DEG_TO_RAD, placement.rotation().y * Mth.DEG_TO_RAD, placement.rotation().z * Mth.DEG_TO_RAD));
        poseStack.scale(placement.scale(), placement.scale(), placement.scale());
        // The animated pose reads like a part's, in model space with Y down
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        attachment.pose().translateAndRotate(poseStack);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        return true;
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
            return Optional.of(JasperItemModels.create(layer, Minecraft.getInstance().getEntityModels().bakeLayer(layer)));
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Item model layer {} is not registered, the items using it render nothing", layer);
            return Optional.empty();
        }
    }
}
