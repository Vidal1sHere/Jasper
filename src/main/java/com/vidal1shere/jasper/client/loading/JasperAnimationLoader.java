package com.vidal1shere.jasper.client.loading;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.animation.BlendMode;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.keyframe.KeyframeAnimation;
import com.vidal1shere.jasper.client.item.ClientItemAnimations;
import com.vidal1shere.jasper.client.player.PlayerBones;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class JasperAnimationLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JasperAnimation>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUFFIX = ".animation.json";
    private static final float PIXEL = 1.0F / 16.0F;
    private static final KeyframeJson.Units PIXEL_UNITS = new KeyframeJson.Units(new Vector3f(PIXEL, -PIXEL, PIXEL), new Vector3f(Mth.DEG_TO_RAD));
    private static final KeyframeJson.Units MODEL_UNITS = new KeyframeJson.Units(new Vector3f(1.0F, -1.0F, 1.0F), new Vector3f(Mth.DEG_TO_RAD));
    private static final Set<String> POSE_FIELDS = Set.of("blend_mode", "bone_modes", "look");
    private static final Set<String> LOOK_FIELDS = Set.of("pitch", "yaw");

    public static final JasperAnimationLoader ITEM = new JasperAnimationLoader("jasper/animation/item", "item", Set.of(ClientItemAnimations.BONE), PIXEL_UNITS, Set.of("pivot"));
    public static final JasperAnimationLoader PLAYER = new JasperAnimationLoader("jasper/animation/player", "player", PlayerBones.ALL, MODEL_UNITS, POSE_FIELDS);
    public static final JasperAnimationLoader ARM_POSE = new JasperAnimationLoader("jasper/animation/arm_pose", "arm pose", PlayerBones.ARM_POSE, MODEL_UNITS, POSE_FIELDS);
    public static final JasperAnimationLoader ENTITY = new JasperAnimationLoader("jasper/animation/entity", "entity", null, MODEL_UNITS, POSE_FIELDS);

    private final String directory;
    private final String kind;
    @Nullable
    private final Set<String> bones;
    private final KeyframeJson.Units units;
    private final Set<String> extraFields;
    private final Map<ResourceLocation, Supplier<JasperAnimation>> registered = new HashMap<>();
    private Map<ResourceLocation, JasperAnimation> loaded = Map.of();

    // Null bones accept any bone, for models whose parts are only known when they render
    public JasperAnimationLoader(String directory, String kind, @Nullable Set<String> bones, KeyframeJson.Units units, Set<String> extraFields) {
        this.directory = directory;
        this.kind = kind;
        this.bones = bones;
        this.units = units;
        this.extraFields = extraFields;
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ITEM);
        event.registerReloadListener(PLAYER);
        event.registerReloadListener(ARM_POSE);
        event.registerReloadListener(ENTITY);
    }

    public void register(ResourceLocation id, Supplier<JasperAnimation> animation) {
        registered.put(id, Suppliers.memoize(animation::get));
    }

    @Nullable
    public JasperAnimation get(ResourceLocation id) {
        JasperAnimation animation = loaded.get(id);
        if (animation != null) {
            return animation;
        }
        Supplier<JasperAnimation> supplier = registered.get(id);
        return supplier == null ? null : supplier.get();
    }

    private JasperAnimation parse(JsonObject json) {
        KeyframeAnimation keyframes = KeyframeJson.parse(json, bones, units, extraFields);
        BlendMode mode = json.has("blend_mode") ? mode(GsonHelper.getAsString(json, "blend_mode"), "blend_mode") : BlendMode.REPLACE;
        Map<String, BlendMode> boneModes = new HashMap<>();
        if (json.has("bone_modes")) {
            for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "bone_modes").entrySet()) {
                String field = "bone_modes." + entry.getKey();
                if (bones != null && !bones.contains(entry.getKey())) {
                    throw new JsonParseException(field + ": unknown bone, expected one of " + bones);
                }
                boneModes.put(entry.getKey(), mode(GsonHelper.convertToString(entry.getValue(), field), field));
            }
        }
        JasperAnimation.Look look = json.has("look") ? look(GsonHelper.getAsJsonObject(json, "look")) : JasperAnimation.Look.NONE;
        Vector3f pivot = json.has("pivot") ? KeyframeJson.constantVector(json.get("pivot"), "pivot").mul(PIXEL) : JasperAnimation.NO_PIVOT;
        return new JasperAnimation(keyframes, mode, Map.copyOf(boneModes), look, pivot, false);
    }

    private static BlendMode mode(String name, String field) {
        BlendMode mode = BlendMode.byName(name);
        if (mode == null) {
            throw new JsonParseException(field + ": expected 'replace' or 'additive', got '" + name + "'");
        }
        return mode;
    }

    private static JasperAnimation.Look look(JsonObject json) {
        for (String field : json.keySet()) {
            if (!LOOK_FIELDS.contains(field)) {
                throw new JsonParseException("look." + field + ": unknown field, expected one of " + LOOK_FIELDS);
            }
        }
        return new JasperAnimation.Look(GsonHelper.getAsBoolean(json, "pitch", false), GsonHelper.getAsBoolean(json, "yaw", false));
    }

    @Override
    protected Map<ResourceLocation, JasperAnimation> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JasperAnimation> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(directory, file -> file.getPath().endsWith(SUFFIX)).entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath().substring(directory.length() + 1, file.getPath().length() - SUFFIX.length());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject root = GsonHelper.parse(reader);
                KeyframeJson.checkRoot(root);
                for (Map.Entry<String, JsonElement> animation : GsonHelper.getAsJsonObject(root, "animations").entrySet()) {
                    ResourceLocation id = ResourceLocation.tryBuild(file.getNamespace(), path + "/" + animation.getKey());
                    if (id == null) {
                        LOGGER.error("Invalid {} animation name '{}' in {}", kind, animation.getKey(), file);
                        continue;
                    }
                    try {
                        result.put(id, parse(GsonHelper.convertToJsonObject(animation.getValue(), animation.getKey())));
                    } catch (JsonParseException | IllegalArgumentException e) {
                        LOGGER.error("Failed to load {} animation {} from {}: {}", kind, id, file, e.getMessage());
                    }
                }
            } catch (IOException | JsonParseException e) {
                LOGGER.error("Failed to read {} animation file {}: {}", kind, file, e.getMessage());
            }
        }
        return result;
    }

    public int reload(ResourceManager manager) {
        apply(prepare(manager, InactiveProfiler.INSTANCE), manager, InactiveProfiler.INSTANCE);
        return loaded.size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JasperAnimation> prepared, ResourceManager manager, ProfilerFiller profiler) {
        loaded = Map.copyOf(prepared);
        LOGGER.debug("Loaded {} {} animations", loaded.size(), kind);
    }
}
