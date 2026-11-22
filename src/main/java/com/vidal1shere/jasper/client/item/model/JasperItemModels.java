package com.vidal1shere.jasper.client.item.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

// The model class of a model layer, like an entity model class; a layer without one gets the plain AnimatedItemModel. Register from the client mod constructor or setup, before the item first renders
@OnlyIn(Dist.CLIENT)
public final class JasperItemModels {
    private static final Map<ModelLayerLocation, BiFunction<ModelPart, String, AnimatedItemModel>> FACTORIES = new ConcurrentHashMap<>();

    public static void register(ModelLayerLocation layer, BiFunction<ModelPart, String, AnimatedItemModel> factory) {
        if (FACTORIES.putIfAbsent(layer, factory) != null) {
            throw new IllegalStateException("Item model layer " + layer + " already has a model class");
        }
    }

    public static AnimatedItemModel create(ModelLayerLocation layer, ModelPart root) {
        return FACTORIES.getOrDefault(layer, AnimatedItemModel::new).apply(root, layer.toString());
    }
}
