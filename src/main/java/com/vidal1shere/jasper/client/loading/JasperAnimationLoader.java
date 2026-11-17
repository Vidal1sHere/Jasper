package com.vidal1shere.jasper.client.loading;

import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.animation.BlendMode;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.keyframe.BoneTimeline;
import com.vidal1shere.jasper.client.animation.locator.Locator;
import com.vidal1shere.jasper.client.effect.timeline.EffectTimeline;
import com.vidal1shere.jasper.client.player.PlayerBones;
import com.vidal1shere.jasper.util.JsonFields;
import com.vidal1shere.jasper.util.EnumNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class JasperAnimationLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JasperAnimation>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUFFIX = ".animation.json";
    private static final float PIXEL = 1.0F / 16.0F;
    private static final Set<String> POSE_FIELDS = Set.of("blend_mode", "bone_modes", "look");
    private static final Set<String> LOOK_FIELDS = Set.of("pitch", "yaw");
    private static final Set<String> LOCATOR_FIELDS = Set.of("parent", "position");

    public static final JasperAnimationLoader ITEM = new JasperAnimationLoader("jasper/animation/item", "item", null, Set.of("pivot"));
    public static final JasperAnimationLoader PLAYER = new JasperAnimationLoader("jasper/animation/player", "player", PlayerBones.ALL, POSE_FIELDS);
    public static final JasperAnimationLoader ENTITY = new JasperAnimationLoader("jasper/animation/entity", "entity", null, POSE_FIELDS);

    private final String directory;
    private final String kind;
    @Nullable
    private final Set<String> bones;
    private final Set<String> extraFields;
    private final Map<ResourceLocation, Supplier<JasperAnimation>> registered = new HashMap<>();
    private Map<ResourceLocation, JasperAnimation> loaded = Map.of();

    public JasperAnimationLoader(String directory, String kind, @Nullable Set<String> bones, Set<String> extraFields) {
        this.directory = directory;
        this.kind = kind;
        this.bones = bones;
        this.extraFields = extraFields;
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ITEM);
        event.registerReloadListener(PLAYER);
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

    private JasperAnimation parse(JsonObject json, Map<String, Locator> fileLocators) {
        Map<String, Locator> locators = json.has("locators") ? locators(json.get("locators"), fileLocators) : new LinkedHashMap<>(fileLocators);
        Set<String> animated = bones == null ? null : Sets.union(bones, locators.keySet()).immutableCopy();
        BoneTimeline keyframes = KeyframeJson.parse(json, animated, extraFields);
        float length = json.has("animation_length") ? GsonHelper.getAsFloat(json, "animation_length") : keyframes.lastKeyframe();
        float blendTicks = GsonHelper.getAsFloat(json, "blend_ticks", JasperAnimation.DEFAULT_BLEND_TICKS);
        BlendMode mode = json.has("blend_mode") ? mode(GsonHelper.getAsString(json, "blend_mode"), "blend_mode") : BlendMode.REPLACE;
        Map<String, BlendMode> boneModes = new HashMap<>();
        if (json.has("bone_modes")) {
            for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "bone_modes").entrySet()) {
                String field = "bone_modes." + entry.getKey();
                if (animated != null && !animated.contains(entry.getKey())) {
                    throw new JsonParseException(field + ": unknown bone, expected one of " + animated);
                }
                boneModes.put(entry.getKey(), mode(GsonHelper.convertToString(entry.getValue(), field), field));
            }
        }
        JasperAnimation.Look look = json.has("look") ? look(GsonHelper.getAsJsonObject(json, "look")) : JasperAnimation.Look.NONE;
        Vector3f pivot = json.has("pivot") ? KeyframeJson.constantVector(json.get("pivot"), "pivot").mul(PIXEL) : JasperAnimation.NO_PIVOT;
        EffectTimeline effects = json.has("effects") ? EffectJson.parse(json.get("effects"), length, locators, bones) : EffectTimeline.EMPTY;
        checkParents(locators);
        return new JasperAnimation(length, loop(json), blendTicks, keyframes, mode, Map.copyOf(boneModes), look, pivot, Map.copyOf(locators), effects);
    }

    private static JasperAnimation.LoopType loop(JsonObject json) {
        if (!json.has("loop")) {
            return JasperAnimation.LoopType.PLAY_ONCE;
        }
        JsonElement loop = json.get("loop");
        if (loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean()) {
            return loop.getAsBoolean() ? JasperAnimation.LoopType.LOOP : JasperAnimation.LoopType.PLAY_ONCE;
        }
        if (loop.isJsonPrimitive() && "hold_on_last_frame".equals(loop.getAsString())) {
            return JasperAnimation.LoopType.HOLD_ON_LAST_FRAME;
        }
        throw new JsonParseException("loop: expected true, false or \"hold_on_last_frame\", got " + loop);
    }

    private Map<String, Locator> locators(JsonElement json, Map<String, Locator> inherited) {
        Map<String, Locator> locators = new LinkedHashMap<>(inherited);
        for (Map.Entry<String, JsonElement> entry : GsonHelper.convertToJsonObject(json, "locators").entrySet()) {
            String name = entry.getKey();
            String field = "locators." + name;
            if (bones != null && bones.contains(name)) {
                throw new JsonParseException(field + ": a locator cannot have the name of a bone");
            }
            JsonObject locator = GsonHelper.convertToJsonObject(entry.getValue(), field);
            JsonFields.check(locator, LOCATOR_FIELDS, field);
            Vector3f position = locator.has("position") ? KeyframeJson.constantVector(locator.get("position"), field + ".position").mul(KeyframeJson.POSITION_UNITS) : new Vector3f();
            locators.put(name, new Locator(GsonHelper.getAsString(locator, "parent"), PartPose.offset(position.x, position.y, position.z), true));
        }
        return locators;
    }

    private void checkParents(Map<String, Locator> locators) {
        for (Map.Entry<String, Locator> entry : locators.entrySet()) {
            String name = entry.getKey();
            String parent = entry.getValue().parent();
            if (parent.equals(name)) {
                continue;
            }
            if (!locators.containsKey(parent) && bones != null && !bones.contains(parent)) {
                throw new JsonParseException("locators." + name + ".parent: unknown bone or locator '" + parent + "', expected one of " + Sets.union(bones, locators.keySet()));
            }
            for (int depth = 0; locators.containsKey(parent) && !locators.get(parent).parent().equals(parent); depth++) {
                if (depth >= locators.size()) {
                    throw new JsonParseException("locators." + name + ".parent: the locator parents make a cycle");
                }
                parent = locators.get(parent).parent();
            }
        }
    }

    private static BlendMode mode(String name, String field) {
        BlendMode mode = EnumNames.byName(BlendMode.class, name);
        if (mode == null) {
            throw new JsonParseException(field + ": expected one of " + EnumNames.names(BlendMode.class) + ", got '" + name + "'");
        }
        return mode;
    }

    private static JasperAnimation.Look look(JsonObject json) {
        JsonFields.check(json, LOOK_FIELDS, "look");
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
                Map<String, Locator> fileLocators = root.has("locators") ? locators(root.get("locators"), Map.of()) : Map.of();
                for (Map.Entry<String, JsonElement> animation : GsonHelper.getAsJsonObject(root, "animations").entrySet()) {
                    ResourceLocation id = ResourceLocation.tryBuild(file.getNamespace(), path + "/" + animation.getKey());
                    if (id == null) {
                        LOGGER.error("Invalid {} animation name '{}' in {}", kind, animation.getKey(), file);
                        continue;
                    }
                    try {
                        result.put(id, parse(GsonHelper.convertToJsonObject(animation.getValue(), animation.getKey()), fileLocators));
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
