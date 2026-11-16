package com.vidal1shere.jasper.client.item.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Set;

// An item model drawn at a part, placed like a display transform: translation in pixels, rotation in degrees
@OnlyIn(Dist.CLIENT)
public record ItemAttachment(ResourceLocation model, String bone, Vector3f translation, Vector3f rotation, float scale) {
    private static final Set<String> FIELDS = Set.of("model", "bone", "translation", "rotation", "scale");

    static ItemAttachment read(JsonObject json, String field) {
        for (String key : json.keySet()) {
            if (!FIELDS.contains(key)) {
                throw new JsonParseException(field + "." + key + ": unknown field, expected one of " + FIELDS);
            }
        }
        ResourceLocation model = ResourceLocation.tryParse(GsonHelper.getAsString(json, "model"));
        if (model == null) {
            throw new JsonParseException(field + ".model: invalid id");
        }
        return new ItemAttachment(model, GsonHelper.getAsString(json, "bone"), vector(json, "translation", field), vector(json, "rotation", field), GsonHelper.getAsFloat(json, "scale", 1.0F));
    }

    private static Vector3f vector(JsonObject json, String name, String field) {
        if (!json.has(name)) {
            return new Vector3f();
        }
        JsonArray array = GsonHelper.getAsJsonArray(json, name);
        if (array.size() != 3) {
            throw new JsonParseException(field + "." + name + ": expected 3 numbers");
        }
        float[] values = new float[3];
        for (int i = 0; i < 3; i++) {
            JsonElement element = array.get(i);
            values[i] = GsonHelper.convertToFloat(element, field + "." + name);
        }
        return new Vector3f(values[0], values[1], values[2]);
    }
}
