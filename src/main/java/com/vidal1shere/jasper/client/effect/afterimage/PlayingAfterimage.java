package com.vidal1shere.jasper.client.effect.afterimage;

import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class PlayingAfterimage implements PlayingEffect {
    final AfterimageEffect effect;
    final LivingEntity entity;
    final AnimationTarget target;
    final boolean hand;
    private final boolean persist;
    private boolean emitting = true;
    private double lastCopy = Double.NEGATIVE_INFINITY;
    int copies;

    PlayingAfterimage(AfterimageEffect effect, EffectContext context) {
        this.effect = effect;
        this.entity = (LivingEntity) context.entity();
        this.target = context.target();
        this.hand = context.inHandPass();
        this.persist = context.persist();
    }

    // Alive while its copies show, so a stop without persist still removes them
    @Override
    public boolean tick(EffectContext context) {
        return emitting || copies > 0;
    }

    @Override
    public void end() {
        emitting = false;
        if (!persist) {
            Afterimages.remove(this);
        }
    }

    @Override
    public boolean endsByItself() {
        return false;
    }

    boolean done() {
        return (!emitting || entity.isRemoved()) && copies == 0;
    }

    boolean due(double now) {
        return emitting && now - lastCopy >= effect.interval().orElse(0.0F);
    }

    void copied(double now) {
        lastCopy = now;
        copies++;
        if (effect.interval().isEmpty()) {
            emitting = false;
        }
    }
}
