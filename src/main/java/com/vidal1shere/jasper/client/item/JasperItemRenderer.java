package com.vidal1shere.jasper.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.item.AnimatedItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class JasperItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PoseStack SCRATCH = new PoseStack();
    private static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return instance();
        }
    };

    @Nullable
    private static JasperItemRenderer instance;
    // renderByItem gets no entity, so the hand renderers say who holds the stack they are drawing
    @Nullable
    private static LivingEntity holder;
    @Nullable
    private static InteractionHand holderHand;

    private final Map<ModelLayerLocation, Optional<AnimatedItemModel>> models = new HashMap<>();

    private JasperItemRenderer(Minecraft minecraft) {
        super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
    }

    public static JasperItemRenderer instance() {
        if (instance == null) {
            instance = new JasperItemRenderer(Minecraft.getInstance());
        }
        return instance;
    }

    // Last, so an item whose mod gives it client extensions of its own keeps them
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof AnimatedItem && !event.isItemRegistered(item)) {
                event.registerItem(EXTENSIONS, item);
            }
        }
    }

    // Not the renderer itself: it is made on the first render, once the block entity renderers exist
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            if (instance != null) {
                instance.models.clear();
            }
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

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        JasperItemModel baked = resolve(stack, displayContext);
        if (baked == null) {
            return;
        }
        AnimatedItemModel model = models.computeIfAbsent(baked.model(), JasperItemRenderer::bake).orElse(null);
        if (model == null) {
            return;
        }
        model.parts().root().getAllParts().forEach(ModelPart::resetPose);
        if (holder != null && holderHand != null && displayContext.firstPerson()) {
            ItemAdapter.applyParts(holder, holderHand, model.parts(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        }
        ResourceLocation texture = baked.texture();
        if (stack.getItem() instanceof AnimatedItem item) {
            ResourceLocation override = item.texture(stack, holder, holderHand);
            if (override != null) {
                texture = override;
            }
        }
        poseStack.pushPose();
        // Undoes the vanilla centering and turns the model (Y down) the way GeckoLib draws a geo item, so the display transforms of a Blockbench GeckoLib item fit as they are
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, model.renderType(texture), false, stack.hasFoil());
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    // The baked model is not passed in, so it is asked again for this display context, through the overrides and separate_transforms like vanilla does
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
