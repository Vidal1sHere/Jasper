package com.vidal1shere.jasper.client.loading;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class KeyframeAnimationLoader<T> extends SimplePreparableReloadListener<Map<ResourceLocation, T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUFFIX = ".animation.json";

    private final String directory;
    private final String kind;
    private final Map<ResourceLocation, Supplier<T>> registered = new HashMap<>();
    private Map<ResourceLocation, T> loaded = Map.of();

    protected KeyframeAnimationLoader(String directory, String kind) {
        this.directory = directory;
        this.kind = kind;
    }

    protected abstract T parse(JsonObject json);

    public void register(ResourceLocation id, Supplier<T> animation) {
        registered.put(id, Suppliers.memoize(animation::get));
    }

    @Nullable
    public T get(ResourceLocation id) {
        T animation = loaded.get(id);
        if (animation != null) {
            return animation;
        }
        Supplier<T> supplier = registered.get(id);
        return supplier == null ? null : supplier.get();
    }

    @Override
    protected Map<ResourceLocation, T> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, T> result = new HashMap<>();
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
    protected void apply(Map<ResourceLocation, T> prepared, ResourceManager manager, ProfilerFiller profiler) {
        loaded = Map.copyOf(prepared);
        LOGGER.debug("Loaded {} {} animations", loaded.size(), kind);
    }
}
