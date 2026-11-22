package com.vidal1shere.jasper.client.item.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

// jasper:item_model, an item model baked with the 3D item, drawn through its own render passes and render types
@OnlyIn(Dist.CLIENT)
public class ItemModelAttachment extends JasperModelAttachment {
    public static final MapCodec<ItemModelAttachment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentPlacement.CODEC.forGetter(JasperModelAttachment::placement),
            ResourceLocation.CODEC.fieldOf("model").forGetter(ItemModelAttachment::model)
    ).apply(instance, ItemModelAttachment::new));

    private final ResourceLocation model;
    @Nullable
    private BakedModel baked;

    public ItemModelAttachment(AttachmentPlacement placement, ResourceLocation model) {
        super(placement);
        this.model = model;
    }

    public ResourceLocation model() {
        return model;
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
        modelGetter.apply(model).resolveParents(modelGetter);
    }

    @Override
    public void bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter) {
        baked = baker.bake(model, BlockModelRotation.X0_Y0, spriteGetter);
    }

    @Override
    public ResourceLocation texture(AttachmentContext context) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(AttachmentContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (baked == null) {
            // Declared in code, so not baked with the item JSON: a model registered through ModelEvent.RegisterAdditional
            baked = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(model));
        }
        // The item model spans 0 to 16 around the attachment
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        for (BakedModel pass : baked.getRenderPasses(context.stack(), true)) {
            for (RenderType type : pass.getRenderTypes(context.stack(), true)) {
                renderer.renderModelLists(pass, context.stack(), packedLight, packedOverlay, poseStack, ItemRenderer.getFoilBufferDirect(buffer, type, true, context.stack().hasFoil()));
            }
        }
    }
}
