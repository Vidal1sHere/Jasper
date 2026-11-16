package com.vidal1shere.jasper.client.effect.timeline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectCodecs;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.client.molang.MolangQuery;
import com.vidal1shere.jasper.client.molang.MolangValue;
import com.vidal1shere.jasper.client.molang.QuerySource;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class MolangEffect {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MolangContext CONTEXT = new MolangContext();

    private final MapCodec<? extends AnimationEffect> codec;
    private final JsonObject json;
    private final Map<String, MolangValue> expressions;
    private boolean warned;

    public MolangEffect(MapCodec<? extends AnimationEffect> codec, JsonObject json, Map<String, MolangValue> expressions) {
        this.codec = codec;
        this.json = json;
        this.expressions = expressions;
    }

    @Nullable
    public AnimationEffect resolve(Entity entity, AnimationTarget target, float animTime) {
        return resolve(entity, target, animTime, null);
    }

    @Nullable
    public AnimationEffect resolve(Entity entity, AnimationTarget target, float animTime, @Nullable QuerySource queries) {
        CONTEXT.resetVariables();
        if (entity instanceof LivingEntity living) {
            InteractionHand hand = target == AnimationTarget.OFF_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            BuiltinQueries.fill(CONTEXT, living, hand, 1.0F);
            if (queries != null) {
                queries.fill(CONTEXT, living, hand, 1.0F);
            }
        }
        CONTEXT.set(MolangQuery.ANIM_TIME, animTime);
        JsonElement resolved = substitute(json);
        return EffectCodecs.resolving(() -> codec.decode(JsonOps.INSTANCE, JsonOps.INSTANCE.getMap(resolved).getOrThrow()))
                .resultOrPartial(message -> {
                    if (!warned) {
                        warned = true;
                        LOGGER.warn("Effect {} could not be resolved: {}", json, message);
                    }
                })
                .orElse(null);
    }

    private JsonElement substitute(JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            MolangValue value = expressions.get(primitive.getAsString());
            return value == null ? primitive : new JsonPrimitive(value.get(CONTEXT));
        }
        if (element instanceof JsonObject object) {
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                copy.add(entry.getKey(), substitute(entry.getValue()));
            }
            return copy;
        }
        if (element instanceof JsonArray array) {
            JsonArray copy = new JsonArray(array.size());
            for (JsonElement item : array) {
                copy.add(substitute(item));
            }
            return copy;
        }
        return element;
    }
}
