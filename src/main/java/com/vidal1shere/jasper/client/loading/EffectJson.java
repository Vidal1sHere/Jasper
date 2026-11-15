package com.vidal1shere.jasper.client.loading;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.animation.locator.Locator;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectEntry;
import com.vidal1shere.jasper.client.effect.EffectTimeline;
import com.vidal1shere.jasper.client.effect.JasperEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class EffectJson {
    private static final Set<String> COMMON_FIELDS = Set.of("type", "locator", "follow", "until", "persist");

    // A locator that is not declared but names a bone (any name when the bones are open) is added to locators as that bone
    public static EffectTimeline parse(JsonElement json, float length, Map<String, Locator> locators, @Nullable Set<String> bones) {
        JsonObject times = GsonHelper.convertToJsonObject(json, "effects");
        List<EffectEntry> entries = new ArrayList<>();
        for (Map.Entry<String, JsonElement> time : times.entrySet()) {
            String field = "effects." + time.getKey();
            float at;
            try {
                at = Float.parseFloat(time.getKey());
            } catch (NumberFormatException e) {
                throw new JsonParseException(field + ": effect time must be a number");
            }
            if (at < 0.0F || at > length) {
                throw new JsonParseException(field + ": effect time must be between 0 and the animation length " + length);
            }
            JsonElement value = time.getValue();
            if (value.isJsonArray()) {
                for (int i = 0; i < value.getAsJsonArray().size(); i++) {
                    String item = field + "[" + i + "]";
                    entries.add(entry(GsonHelper.convertToJsonObject(value.getAsJsonArray().get(i), item), item, at, length, locators, bones));
                }
            } else {
                entries.add(entry(GsonHelper.convertToJsonObject(value, field), field, at, length, locators, bones));
            }
        }
        entries.sort(Comparator.comparingDouble(EffectEntry::time));
        return new EffectTimeline(List.copyOf(entries));
    }

    private static EffectEntry entry(JsonObject json, String field, float time, float length, Map<String, Locator> locators, @Nullable Set<String> bones) {
        String typeName = GsonHelper.getAsString(json, "type");
        ResourceLocation type = typeName.indexOf(':') < 0 ? ResourceLocation.tryBuild(Jasper.MOD_ID, typeName) : ResourceLocation.tryParse(typeName);
        MapCodec<? extends AnimationEffect> codec = type == null ? null : JasperEffects.get(type);
        if (codec == null) {
            throw new JsonParseException(field + ".type: unknown effect type '" + typeName + "', expected one of " + JasperEffects.ids());
        }
        Set<String> allowed = new HashSet<>(COMMON_FIELDS);
        codec.keys(JsonOps.INSTANCE).forEach(key -> allowed.add(key.getAsString()));
        for (String key : json.keySet()) {
            if (!allowed.contains(key)) {
                throw new JsonParseException(field + "." + key + ": unknown field, expected one of " + allowed);
            }
        }
        AnimationEffect effect = codec.decode(JsonOps.INSTANCE, JsonOps.INSTANCE.getMap(json).getOrThrow())
                .getOrThrow(message -> new JsonParseException(field + ": " + message));
        String locator = json.has("locator") ? GsonHelper.getAsString(json, "locator") : null;
        if (locator != null) {
            locator(locator, field + ".locator", locators, bones);
        }
        for (String read : effect.locators()) {
            locator(read, field + ".locators", locators, bones);
        }
        return new EffectEntry(time, effect, locator, GsonHelper.getAsBoolean(json, "follow", false), until(json, field, time, length), GsonHelper.getAsBoolean(json, "persist", true));
    }

    private static void locator(String name, String field, Map<String, Locator> locators, @Nullable Set<String> bones) {
        if (locators.containsKey(name)) {
            return;
        }
        if (bones != null && !bones.contains(name)) {
            throw new JsonParseException(field + ": unknown locator '" + name + "', expected one of " + locators.keySet() + " or a bone " + bones);
        }
        locators.put(name, Locator.bone(name));
    }

    private static float until(JsonObject json, String field, float time, float length) {
        if (!json.has("until")) {
            return EffectEntry.NO_UNTIL;
        }
        JsonElement until = json.get("until");
        if (until.isJsonPrimitive() && until.getAsJsonPrimitive().isString() && "end".equals(until.getAsString())) {
            return EffectEntry.UNTIL_END;
        }
        if (!until.isJsonPrimitive() || !until.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + ".until: expected a time or \"end\", got " + until);
        }
        float at = until.getAsFloat();
        if (at <= time || at > length) {
            throw new JsonParseException(field + ".until: must be after the effect time " + time + " and at most the animation length " + length);
        }
        return at;
    }
}
