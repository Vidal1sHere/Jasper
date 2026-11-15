package com.vidal1shere.jasper.client.effect.particle;

import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.client.particle.Particle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// An emitter while it follows; without persist it also removes what it spawned when it ends
@OnlyIn(Dist.CLIENT)
final class PlayingParticles implements PlayingEffect {
    private final ParticleEffect effect;
    private final boolean follow;
    @Nullable
    private final List<Particle> spawned;
    private int wait;

    PlayingParticles(ParticleEffect effect, EffectContext context) {
        this.effect = effect;
        this.follow = context.follow();
        this.spawned = context.persist() ? null : new ArrayList<>();
        this.wait = effect.interval();
        effect.spawn(context.position(), spawned);
    }

    @Override
    public boolean tick(EffectContext context) {
        if (spawned != null) {
            spawned.removeIf(particle -> !particle.isAlive());
        }
        if (!follow) {
            return spawned != null && !spawned.isEmpty();
        }
        if (--wait <= 0) {
            wait = effect.interval();
            effect.spawn(context.position(), spawned);
        }
        return true;
    }

    @Override
    public void end() {
        if (spawned != null) {
            spawned.forEach(Particle::remove);
            spawned.clear();
        }
    }

    @Override
    public boolean endsByItself() {
        return !follow;
    }
}
