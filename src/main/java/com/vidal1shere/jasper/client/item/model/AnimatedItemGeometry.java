package com.vidal1shere.jasper.client.item.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.vidal1shere.jasper.Jasper;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// "model" is "<namespace>:<path>#<layer>", the layer main by default
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public record AnimatedItemGeometry(ModelLayerLocation model, ResourceLocation texture, List<ItemAttachment> attachments) implements IUnbakedGeometry<AnimatedItemGeometry> {
    public static final ResourceLocation LOADER_ID = Jasper.id("animated");
    public static final IGeometryLoader<AnimatedItemGeometry> LOADER = (json, context) -> new AnimatedItemGeometry(layer(GsonHelper.getAsString(json, "model")), parse(GsonHelper.getAsString(json, "texture")), attachments(json));

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(LOADER_ID, LOADER);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        for (ItemAttachment attachment : attachments) {
            modelGetter.apply(attachment.model()).resolveParents(modelGetter);
        }
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));
        Map<ItemAttachment, BakedModel> baked = new LinkedHashMap<>();
        for (ItemAttachment attachment : attachments) {
            baked.put(attachment, baker.bake(attachment.model(), BlockModelRotation.X0_Y0, spriteGetter));
        }
        return new JasperItemModel(model, texture, baked, context.getTransforms(), overrides, particle, context.useBlockLight());
    }

    private static List<ItemAttachment> attachments(JsonObject json) {
        if (!json.has("attachments")) {
            return List.of();
        }
        List<ItemAttachment> attachments = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "attachments").entrySet()) {
            String field = "attachments." + entry.getKey();
            attachments.add(ItemAttachment.read(GsonHelper.convertToJsonObject(entry.getValue(), field), field));
        }
        return List.copyOf(attachments);
    }

    private static ModelLayerLocation layer(String value) {
        int split = value.indexOf('#');
        return split < 0 ? new ModelLayerLocation(parse(value), "main") : new ModelLayerLocation(parse(value.substring(0, split)), value.substring(split + 1));
    }

    private static ResourceLocation parse(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new JsonParseException("Invalid id '" + value + "' in a " + LOADER_ID + " model");
        }
        return id;
    }
}
