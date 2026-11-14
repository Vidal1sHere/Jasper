package com.vidal1shere.jasper.layer;

import com.vidal1shere.jasper.Jasper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Filled in the mod constructors, so it is the same on every side without a sync; the payloads carry the layer id
public final class JasperLayers {
    private static final Map<ResourceLocation, Layer> LAYERS = new HashMap<>();
    private static List<Layer> sorted = List.of();

    public static final Layer BASE = register(new Layer(Jasper.id("base"), 0, new LayerPolicy(Set.of(), false)));

    public static synchronized Layer register(Layer layer) {
        if (LAYERS.putIfAbsent(layer.id(), layer) != null) {
            throw new IllegalStateException("Animation layer " + layer.id() + " is already registered");
        }
        sorted = LAYERS.values().stream().sorted(Comparator.comparingInt(Layer::priority).thenComparing(entry -> entry.id().toString())).toList();
        return layer;
    }

    @Nullable
    public static Layer get(ResourceLocation id) {
        return LAYERS.get(id);
    }

    public static List<Layer> sorted() {
        return sorted;
    }
}
