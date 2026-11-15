package com.vidal1shere.jasper.client.effect;

import com.mojang.serialization.MapCodec;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.effect.sound.SoundEffect;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Filled in the client mod constructor, before the first resource load reads the animation files
@OnlyIn(Dist.CLIENT)
public final class JasperEffects {
    private static final Map<ResourceLocation, MapCodec<? extends AnimationEffect>> TYPES = new ConcurrentHashMap<>();

    public static final ResourceLocation SOUND = register(Jasper.id("sound"), SoundEffect.CODEC);

    public static ResourceLocation register(ResourceLocation id, MapCodec<? extends AnimationEffect> codec) {
        if (TYPES.putIfAbsent(id, codec) != null) {
            throw new IllegalStateException("Effect type " + id + " is already registered");
        }
        return id;
    }

    @Nullable
    public static MapCodec<? extends AnimationEffect> get(ResourceLocation id) {
        return TYPES.get(id);
    }

    public static Set<ResourceLocation> ids() {
        return Set.copyOf(TYPES.keySet());
    }
}
