package com.vidal1shere.jasper.client.effect;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.keyframe.BoneTimeline;
import com.vidal1shere.jasper.client.attachment.EffectData;
import com.vidal1shere.jasper.client.attachment.LayerData;
import com.vidal1shere.jasper.client.attachment.effect.BoundEffect;
import com.vidal1shere.jasper.client.attachment.layer.LayerState;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.LayerKey;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class ClientEffects {
    // A play this client learns about later than this was joined late (a relog, a new tracking), so its past keyframes do not fire
    private static final long LATE_TICKS = 10L;
    // Below 0, so the keyframes at 0 of a new play fire
    private static final float BEFORE_START = -1.0F;

    // Effects that outlived their animation or their entity, until they end by themselves
    private static final List<BoundEffect> DETACHED = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        long now = minecraft.level.getGameTime();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            LayerData layers = entity.getExistingDataOrNull(ClientAttachments.LAYERS);
            if (layers != null) {
                layers.forEach((key, track) -> fire(entity, key, track, now));
            }
            EffectData effects = entity.getExistingDataOrNull(ClientAttachments.EFFECTS);
            if (effects != null) {
                tick(layers, effects, now);
            }
        }
        DETACHED.removeIf(running -> !running.instance().tick(running.context()));
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        EffectData effects = event.getEntity().getExistingDataOrNull(ClientAttachments.EFFECTS);
        if (effects == null) {
            return;
        }
        for (BoundEffect running : effects.running()) {
            if (running.persist() && running.instance().endsByItself()) {
                DETACHED.add(running);
            } else {
                running.instance().end();
            }
        }
        effects.running().clear();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DETACHED.forEach(running -> running.instance().end());
        DETACHED.clear();
    }

    private static void fire(Entity entity, LayerKey key, LayerState layer, long now) {
        AnimationTrack track = layer.track();
        track.update(now);
        AnimationTrack.Entry entry = track.entry(track.size() - 1);
        float elapsed = entry.elapsed(now);
        boolean visible = visible(entity, key.target());
        float from;
        if (entry == layer.firedEntry()) {
            from = layer.firedTime();
        } else {
            // What the previous entry had left before it was replaced, which a render can do between two ticks
            AnimationTrack.Entry previous = layer.firedEntry();
            int index = previous == null ? -1 : track.indexOf(previous);
            if (index >= 0) {
                fire(entity, key, previous, track.animation(index), layer.firedTime(), previous.elapsed(Math.min(now, entry.startTick())), visible);
            }
            from = now - entry.startTick() <= LATE_TICKS ? BEFORE_START : elapsed;
        }
        layer.fired(entry, elapsed);
        fire(entity, key, entry, track.animation(track.size() - 1), from, elapsed, visible);
    }

    private static void fire(Entity entity, LayerKey key, AnimationTrack.Entry entry, @Nullable JasperAnimation animation, float from, float to, boolean visible) {
        if (animation == null || animation.effects().isEmpty()) {
            return;
        }
        BoneTimeline keyframes = animation.keyframes();
        animation.effects().fire(from, to, keyframes.lengthInSeconds(), entry.cycles(keyframes), (effect, at) -> {
            if (visible || !effect.effect().visual()) {
                start(entity, key, entry, effect, at);
            }
        });
    }

    private static void start(Entity entity, LayerKey key, AnimationTrack.Entry entry, EffectEntry effect, float at) {
        EffectContext context = new EffectContext(entity, key.target(), effect.locator(), effect.follow());
        PlayingEffect instance = effect.effect().start(context);
        if (instance == null) {
            return;
        }
        float until = Float.isNaN(effect.until()) || Float.isInfinite(effect.until()) ? effect.until() : at - effect.time() + effect.until();
        entity.getData(ClientAttachments.EFFECTS).running().add(new BoundEffect(key, entry, instance, context, until, effect.persist()));
    }

    private static void tick(@Nullable LayerData layers, EffectData effects, long now) {
        effects.running().removeIf(running -> {
            LayerState layer = layers == null ? null : layers.existing(running.key().target(), running.key().layer());
            boolean playing = layer != null && layer.track().entry(layer.track().size() - 1) == running.entry();
            if (playing && running.until() <= running.entry().elapsed(now)) {
                running.instance().end();
                return true;
            }
            if (!playing) {
                if (running.persist() && running.instance().endsByItself()) {
                    DETACHED.add(running);
                } else {
                    running.instance().end();
                }
                return true;
            }
            return !running.instance().tick(running.context());
        });
    }

    // Visual effects fire where their target is seen: the hands in first person, the model everywhere else
    private static boolean visible(Entity entity, AnimationTarget target) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean firstPerson = entity == minecraft.player && entity == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
        return target == AnimationTarget.MODEL ? !firstPerson : firstPerson;
    }
}
