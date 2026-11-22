package com.vidal1shere.jasper.client.item.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.item.attachment.JasperAttachmentTypes;
import com.vidal1shere.jasper.client.item.attachment.JasperModelAttachment;
import com.vidal1shere.jasper.util.JsonFields;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// "model" is "<namespace>:<path>#<layer>", the layer main by default
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public record AnimatedItemGeometry(ModelLayerLocation model, ResourceLocation texture, List<JasperModelAttachment> attachments) implements IUnbakedGeometry<AnimatedItemGeometry> {
    public static final ResourceLocation LOADER_ID = Jasper.id("animated");
    public static final IGeometryLoader<AnimatedItemGeometry> LOADER = (json, context) -> new AnimatedItemGeometry(layer(GsonHelper.getAsString(json, "model")), parse(GsonHelper.getAsString(json, "texture")), attachments(json));

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(LOADER_ID, LOADER);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        for (JasperModelAttachment attachment : attachments) {
            attachment.resolveParents(modelGetter);
        }
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));
        for (JasperModelAttachment attachment : attachments) {
            attachment.bake(baker, spriteGetter);
        }
        return new JasperItemModel(model, texture, attachments, context.getTransforms(), overrides, particle, context.useBlockLight());
    }

    // The map key is the attachment name; "type" names its kind, jasper:item_model by default
    private static List<JasperModelAttachment> attachments(JsonObject json) {
        if (!json.has("attachments")) {
            return List.of();
        }
        Map<String, JasperModelAttachment> attachments = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "attachments").entrySet()) {
            String field = "attachments." + entry.getKey();
            JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), field).deepCopy();
            ResourceLocation type = object.has("type") ? parse(GsonHelper.getAsString(object, "type")) : JasperAttachmentTypes.ITEM_MODEL;
            MapCodec<? extends JasperModelAttachment> codec = JasperAttachmentTypes.get(type);
            if (codec == null) {
                throw new JsonParseException(field + ".type: unknown attachment type " + type);
            }
            object.remove("type");
            object.addProperty("name", entry.getKey());
            Set<String> fields = codec.keys(JsonOps.INSTANCE).map(JsonElement::getAsString).collect(Collectors.toSet());
            JsonFields.check(object, fields, field);
            attachments.put(entry.getKey(), codec.codec().parse(JsonOps.INSTANCE, object).getOrThrow(message -> new JsonParseException(field + ": " + message)));
        }
        for (JasperModelAttachment attachment : attachments.values()) {
            checkChain(attachment, attachments);
        }
        return List.copyOf(attachments.values());
    }

    // An attachment whose bone is its own name sits on the model part of that name, so it never parents itself
    private static void checkChain(JasperModelAttachment attachment, Map<String, JasperModelAttachment> attachments) {
        Set<String> seen = new HashSet<>();
        JasperModelAttachment current = attachment;
        while (current != null) {
            if (!seen.add(current.name())) {
                throw new JsonParseException("attachments." + attachment.name() + ": its bone chain loops through " + current.name());
            }
            String bone = current.placement().bone();
            current = bone.equals(current.name()) ? null : attachments.get(bone);
        }
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
