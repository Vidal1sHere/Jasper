package com.vidal1shere.jasper.client.item.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BuiltInModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class JasperItemModel extends BuiltInModel {
    private final ModelLayerLocation model;
    private final ResourceLocation texture;
    private final Map<ItemAttachment, BakedModel> attachments;

    public JasperItemModel(ModelLayerLocation model, ResourceLocation texture, Map<ItemAttachment, BakedModel> attachments, ItemTransforms transforms, ItemOverrides overrides, TextureAtlasSprite particle, boolean usesBlockLight) {
        super(transforms, overrides, particle, usesBlockLight);
        this.model = model;
        this.texture = texture;
        this.attachments = attachments;
    }

    public Map<ItemAttachment, BakedModel> attachments() {
        return attachments;
    }

    public ModelLayerLocation model() {
        return model;
    }

    public ResourceLocation texture() {
        return texture;
    }
}
