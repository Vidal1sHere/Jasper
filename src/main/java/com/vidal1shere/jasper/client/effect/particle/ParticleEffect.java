package com.vidal1shere.jasper.client.effect.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.effect.EffectCodecs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public record ParticleEffect(ParticleOptions particle, int count, Vec3 spread, float speed, Vec3 velocity, Optional<Integer> lifetime, int interval) implements AnimationEffect {
    private static final double MAX_DISTANCE_SQUARED = 32.0 * 32.0;

    public static final MapCodec<ParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf("particle").forGetter(ParticleEffect::particle),
            EffectCodecs.integer(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(ParticleEffect::count),
            EffectCodecs.vector().optionalFieldOf("spread", Vec3.ZERO).forGetter(ParticleEffect::spread),
            EffectCodecs.number(0.0F, Float.MAX_VALUE).optionalFieldOf("speed", 0.0F).forGetter(ParticleEffect::speed),
            EffectCodecs.vector().optionalFieldOf("velocity", Vec3.ZERO).forGetter(ParticleEffect::velocity),
            EffectCodecs.integer(1, Integer.MAX_VALUE).optionalFieldOf("lifetime").forGetter(ParticleEffect::lifetime),
            EffectCodecs.integer(1, Integer.MAX_VALUE).optionalFieldOf("interval", 1).forGetter(ParticleEffect::interval)
    ).apply(instance, ParticleEffect::new));

    @Override
    @Nullable
    public PlayingEffect start(EffectContext context) {
        PlayingParticles playing = new PlayingParticles(this, context);
        return context.follow() || !context.persist() ? playing : null;
    }

    // The particle engine returns the particles so they can be removed, but skips the checks of addParticle: the particles option and the camera distance
    void spawn(Vec3 at, @Nullable List<Particle> spawned) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        boolean force = particle.getType().getOverrideLimiter();
        if (!force && minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(at) > MAX_DISTANCE_SQUARED) {
            return;
        }
        ParticleStatus status = minecraft.options.particles().get();
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            if (!force && (status == ParticleStatus.MINIMAL || status == ParticleStatus.DECREASED && random.nextInt(3) == 0)) {
                continue;
            }
            Particle created = minecraft.particleEngine.createParticle(particle,
                    at.x + random.nextGaussian() * spread.x, at.y + random.nextGaussian() * spread.y, at.z + random.nextGaussian() * spread.z,
                    velocity.x + random.nextGaussian() * speed, velocity.y + random.nextGaussian() * speed, velocity.z + random.nextGaussian() * speed);
            if (created == null) {
                continue;
            }
            lifetime.ifPresent(created::setLifetime);
            if (spawned != null) {
                spawned.add(created);
            }
        }
    }
}
