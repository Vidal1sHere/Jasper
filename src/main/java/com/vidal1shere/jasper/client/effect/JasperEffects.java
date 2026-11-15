package com.vidal1shere.jasper.client.effect;

import com.mojang.serialization.MapCodec;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.effect.afterimage.AfterimageEffect;
import com.vidal1shere.jasper.client.effect.particle.ParticleEffect;
import com.vidal1shere.jasper.client.effect.screen.FlashEffect;
import com.vidal1shere.jasper.client.effect.screen.FovEffect;
import com.vidal1shere.jasper.client.effect.screen.HitStopEffect;
import com.vidal1shere.jasper.client.effect.sound.SoundEffect;
import com.vidal1shere.jasper.client.effect.trail.TrailEffect;
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
    public static final ResourceLocation PARTICLE = register(Jasper.id("particle"), ParticleEffect.CODEC);
    public static final ResourceLocation FLASH = register(Jasper.id("flash"), FlashEffect.CODEC);
    public static final ResourceLocation FOV = register(Jasper.id("fov"), FovEffect.CODEC);
    public static final ResourceLocation HIT_STOP = register(Jasper.id("hit_stop"), HitStopEffect.CODEC);
    public static final ResourceLocation TRAIL = register(Jasper.id("trail"), TrailEffect.CODEC);
    public static final ResourceLocation AFTERIMAGE = register(Jasper.id("afterimage"), AfterimageEffect.CODEC);

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
