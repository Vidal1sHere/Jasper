package com.vidal1shere.jasper.client.effect.trail;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.attachment.LocatorData;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.Deque;

@OnlyIn(Dist.CLIENT)
final class PlayingTrail implements PlayingEffect {
    private static final int MAX_RINGS = 1024;

    final TrailEffect effect;
    final Deque<Ring> rings = new ArrayDeque<>();
    private final Entity entity;
    private final AnimationTarget target;
    // Hand trails seen in first person live in the hand pass, those of a 3D item seen from outside in the world
    private final boolean hand;
    private final boolean persist;
    private boolean recording = true;
    private double lastRing = Double.NEGATIVE_INFINITY;
    private Vec3 anchor;

    PlayingTrail(TrailEffect effect, EffectContext context) {
        this.effect = effect;
        this.entity = context.entity();
        this.target = context.target();
        this.hand = target != AnimationTarget.MODEL && entity instanceof LivingEntity living && BuiltinQueries.isFirstPerson(living);
        this.persist = context.persist();
        this.anchor = entity.position();
    }

    @Override
    public boolean tick(EffectContext context) {
        return recording;
    }

    @Override
    public void end() {
        recording = false;
        if (!persist) {
            rings.clear();
        }
    }

    @Override
    public boolean endsByItself() {
        return false;
    }

    // A ring never mixes positions of two frames
    void record(long frame, double now, float partialTick) {
        if (!recording || now - lastRing < effect.interval()) {
            return;
        }
        LocatorData data = entity.getExistingDataOrNull(ClientAttachments.LOCATORS);
        if (data == null) {
            return;
        }
        Vec3 origin = origin(partialTick);
        Vec3[] points = new Vec3[effect.locators().size()];
        for (int i = 0; i < points.length; i++) {
            Vec3 point = data.inFrame(target, effect.locators().get(i), frame);
            if (point == null) {
                return;
            }
            points[i] = point.subtract(origin);
        }
        rings.addLast(new Ring(points, now));
        lastRing = now;
        if (rings.size() > MAX_RINGS) {
            rings.removeFirst();
        }
    }

    // A removed entity leaves its rings where it was
    Vec3 origin(float partialTick) {
        if (effect.space() == TrailEffect.Space.WORLD) {
            return Vec3.ZERO;
        }
        if (!entity.isRemoved()) {
            anchor = entity.getPosition(partialTick);
        }
        return anchor;
    }

    boolean hand() {
        return hand;
    }

    void trim(double now) {
        while (!rings.isEmpty() && now - rings.peekFirst().time() >= effect.lifetime()) {
            rings.removeFirst();
        }
    }

    boolean finished() {
        return !recording && rings.isEmpty();
    }

    record Ring(Vec3[] points, double time) {
    }
}
