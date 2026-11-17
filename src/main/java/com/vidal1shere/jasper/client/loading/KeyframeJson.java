package com.vidal1shere.jasper.client.loading;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.vidal1shere.jasper.client.animation.keyframe.BoneChannels;
import com.vidal1shere.jasper.client.animation.keyframe.BoneTimeline;
import com.vidal1shere.jasper.client.animation.keyframe.Easing;
import com.vidal1shere.jasper.client.animation.keyframe.BoneKeyframe;
import com.vidal1shere.jasper.client.molang.MolangParser;
import com.vidal1shere.jasper.client.molang.MolangValue;
import com.vidal1shere.jasper.util.JsonFields;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class KeyframeJson {
    public static final Vector3f POSITION_UNITS = new Vector3f(1.0F, -1.0F, 1.0F);
    private static final Vector3f ROTATION_UNITS = new Vector3f(Mth.DEG_TO_RAD);
    private static final Vector3f SCALE_UNITS = new Vector3f(1.0F);

    private static final Set<String> ROOT_FIELDS = Set.of("format_version", "geckolib_format_version", "animations", "locators");
    private static final Set<String> ANIMATION_FIELDS = Set.of("animation_length", "loop", "bones", "blend_ticks", "pre_animation", "locators", "effects");
    private static final Set<String> UNSUPPORTED = Set.of("sound_effects", "particle_effects", "timeline");
    private static final Set<String> CHANNELS = Set.of("position", "rotation", "scale");
    private static final Set<String> KEYFRAME_FIELDS = Set.of("vector", "pre", "post", "easing", "easingArgs", "lerp_mode");

    public static void checkRoot(JsonObject root) {
        JsonFields.check(root, ROOT_FIELDS, "");
    }

    public static BoneTimeline parse(JsonObject json, @Nullable Set<String> bones, Set<String> extraFields) {
        for (String field : UNSUPPORTED) {
            if (json.has(field)) {
                throw new JsonParseException(field + ": not supported yet");
            }
        }
        JsonFields.check(json, Sets.union(ANIMATION_FIELDS, extraFields), "");
        JsonObject bonesJson = GsonHelper.getAsJsonObject(json, "bones");
        Map<String, BoneChannels> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> bone : bonesJson.entrySet()) {
            String name = bone.getKey();
            String field = "bones." + name;
            if (bones != null && !bones.contains(name)) {
                throw new JsonParseException(field + ": unknown bone, expected one of " + bones);
            }
            JsonObject channels = GsonHelper.convertToJsonObject(bone.getValue(), field);
            for (String channel : channels.keySet()) {
                if (!CHANNELS.contains(channel)) {
                    throw new JsonParseException(field + "." + channel + ": unknown channel, expected one of " + CHANNELS);
                }
            }
            BoneChannels animation = new BoneChannels(
                    channel(channels, "position", field, POSITION_UNITS),
                    channel(channels, "rotation", field, ROTATION_UNITS),
                    channel(channels, "scale", field, SCALE_UNITS)
            );
            parsed.put(name, animation);
        }
        return new BoneTimeline(Map.copyOf(parsed), preAnimation(json));
    }

    public static Vector3f constantVector(JsonElement element, String field) {
        MolangValue[] values = vector(element, field, SCALE_UNITS);
        for (MolangValue value : values) {
            if (!value.isConstant()) {
                throw new JsonParseException(field + ": expected numbers");
            }
        }
        return new Vector3f(values[0].get(null), values[1].get(null), values[2].get(null));
    }

    private static MolangValue preAnimation(JsonObject json) {
        if (!json.has("pre_animation")) {
            return MolangValue.ZERO;
        }
        JsonElement element = json.get("pre_animation");
        StringBuilder script = new StringBuilder();
        if (element.isJsonArray()) {
            JsonArray lines = element.getAsJsonArray();
            for (int i = 0; i < lines.size(); i++) {
                script.append(GsonHelper.convertToString(lines.get(i), "pre_animation[" + i + "]")).append(';');
            }
        } else {
            script.append(GsonHelper.convertToString(element, "pre_animation"));
        }
        try {
            return MolangParser.parse(script.toString());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("pre_animation: " + e.getMessage());
        }
    }

    private static BoneKeyframe[] channel(JsonObject bone, String name, String boneField, Vector3f units) {
        if (!bone.has(name)) {
            return BoneChannels.NONE;
        }
        String field = boneField + "." + name;
        JsonElement element = bone.get(name);
        List<BoneKeyframe> keyframes = new ArrayList<>();
        if (element.isJsonObject() && !element.getAsJsonObject().has("vector")) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                keyframe(entry.getKey(), entry.getValue(), field + "." + entry.getKey(), units, keyframes);
            }
        } else {
            keyframe("0", element, field, units, keyframes);
        }
        keyframes.sort(Comparator.comparingDouble(BoneKeyframe::time));
        return keyframes.toArray(BoneKeyframe[]::new);
    }

    private static void keyframe(String time, JsonElement value, String field, Vector3f units, List<BoneKeyframe> out) {
        float timestamp;
        try {
            timestamp = Float.parseFloat(time);
        } catch (NumberFormatException e) {
            throw new JsonParseException(field + ": keyframe time must be a number");
        }
        if (!value.isJsonObject()) {
            out.add(create(timestamp, vector(value, field, units), Easing.LINEAR, 0.0F));
            return;
        }
        JsonObject object = value.getAsJsonObject();
        JsonFields.check(object, KEYFRAME_FIELDS, field);
        Easing easing = easing(object, field);
        float argument = easingArgument(object, field, easing);
        if (object.has("pre") || object.has("post")) {
            if (object.has("vector")) {
                throw new JsonParseException(field + ": use either 'vector' or 'pre'/'post'");
            }
            if (object.has("pre")) {
                out.add(create(timestamp, prePost(object.get("pre"), field + ".pre", units), easing, argument));
            }
            if (object.has("post")) {
                out.add(create(timestamp, prePost(object.get("post"), field + ".post", units), easing, argument));
            }
            return;
        }
        if (!object.has("vector")) {
            throw new JsonParseException(field + ": missing 'vector'");
        }
        out.add(create(timestamp, vector(object.get("vector"), field + ".vector", units), easing, argument));
    }

    private static MolangValue[] prePost(JsonElement element, String field, Vector3f units) {
        if (!element.isJsonObject()) {
            return vector(element, field, units);
        }
        JsonObject object = element.getAsJsonObject();
        if (object.size() != 1 || !object.has("vector")) {
            throw new JsonParseException(field + ": expected a vector or an object with only 'vector'");
        }
        return vector(object.get("vector"), field + ".vector", units);
    }

    private static BoneKeyframe create(float time, MolangValue[] values, Easing easing, float argument) {
        return new BoneKeyframe(time, values[0], values[1], values[2], easing, argument);
    }

    private static Easing easing(JsonObject object, String field) {
        Easing easing = Easing.LINEAR;
        if (object.has("lerp_mode")) {
            String mode = GsonHelper.getAsString(object, "lerp_mode");
            easing = switch (mode) {
                case "linear" -> Easing.LINEAR;
                case "catmullrom" -> Easing.CATMULLROM;
                case "step" -> Easing.STEP;
                default -> throw new JsonParseException(field + ".lerp_mode: expected linear, catmullrom or step, got '" + mode + "'");
            };
        }
        if (object.has("easing")) {
            String name = GsonHelper.getAsString(object, "easing");
            easing = Easing.byName(name);
            if (easing == null) {
                throw new JsonParseException(field + ".easing: unknown easing '" + name + "', expected one of " + Easing.names());
            }
        }
        return easing;
    }

    private static float easingArgument(JsonObject object, String field, Easing easing) {
        if (!object.has("easingArgs")) {
            return easing.defaultArgument();
        }
        JsonArray args = GsonHelper.getAsJsonArray(object, "easingArgs");
        if (!easing.usesArgument() || args.size() != 1 || !args.get(0).isJsonPrimitive() || !args.get(0).getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + ".easingArgs: " + (easing.usesArgument() ? "expected one number" : "easing does not take arguments"));
        }
        return args.get(0).getAsFloat();
    }

    private static MolangValue[] vector(JsonElement element, String field, Vector3f units) {
        if (element.isJsonPrimitive()) {
            MolangValue value = value(element.getAsJsonPrimitive(), field);
            return new MolangValue[]{scaled(value, units.x()), scaled(value, units.y()), scaled(value, units.z())};
        }
        if (!element.isJsonArray() || element.getAsJsonArray().size() != 3) {
            throw new JsonParseException(field + ": expected a number, a molang string or an array of 3");
        }
        JsonArray array = element.getAsJsonArray();
        MolangValue[] values = new MolangValue[3];
        for (int i = 0; i < 3; i++) {
            JsonElement value = array.get(i);
            if (!value.isJsonPrimitive()) {
                throw new JsonParseException(field + "[" + i + "]: expected a number or a molang string");
            }
            values[i] = scaled(value(value.getAsJsonPrimitive(), field + "[" + i + "]"), units.get(i));
        }
        return values;
    }

    private static MolangValue value(JsonPrimitive primitive, String field) {
        if (primitive.isNumber()) {
            return MolangValue.constant(primitive.getAsFloat());
        }
        if (!primitive.isString()) {
            throw new JsonParseException(field + ": expected a number or a molang string");
        }
        try {
            return MolangParser.parse(primitive.getAsString());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(field + ": " + e.getMessage());
        }
    }

    private static MolangValue scaled(MolangValue value, float factor) {
        if (factor == 1.0F) {
            return value;
        }
        if (value.isConstant()) {
            return MolangValue.constant(value.get(null) * factor);
        }
        return context -> value.get(context) * factor;
    }
}
