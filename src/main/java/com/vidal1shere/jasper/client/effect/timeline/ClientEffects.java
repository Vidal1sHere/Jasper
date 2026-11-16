package com.vidal1shere.jasper.client.effect.timeline;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.attachment.EffectData;
import com.vidal1shere.jasper.client.attachment.LocatorData;
import com.vidal1shere.jasper.client.attachment.LayerState;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import com.vidal1shere.jasper.client.molang.QuerySource;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.LayerKey;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class ClientEffects {
    // A play this client learns about later than this was joined late (a relog, a new tracking), so its past keyframes do not fire
    private static final long LATE_TICKS = 10L;
    private static final float BEFORE_START = -1.0F;

    // The effects that outlive their entity
    private static final List<EffectHandle> DETACHED = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        long now = minecraft.level.getGameTime();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            ClientAnimationData layers = entity.getExistingDataOrNull(ClientAttachments.ANIMATIONS);
            if (layers != null) {
                layers.forEach((key, track) -> fire(entity, key, track, now));
            }
            EffectData effects = entity.getExistingDataOrNull(ClientAttachments.EFFECTS);
            if (effects != null) {
                effects.running().removeIf(handle -> !handle.tick(now, layers));
            }
        }
        DETACHED.removeIf(handle -> !handle.tick(now, null));
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
        for (EffectHandle handle : effects.running()) {
            handle.end();
            if (!handle.ended()) {
                DETACHED.add(handle);
            }
        }
        effects.running().clear();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DETACHED.forEach(EffectHandle::discard);
        DETACHED.clear();
    }

    // An entry of EffectJson.parseEntry started on an entity outside any animation; the position stands in without a captured locator, the queries add molang values of the caller
    @Nullable
    public static EffectHandle play(Entity entity, AnimationTarget target, EffectEntry entry, Supplier<Vec3> position, @Nullable QuerySource queries) {
        return play(entity, target, entry, 0.0F, null, null, entry.until(), position, queries);
    }

    // The one way an effect starts, from a keyframe (key and the entry that fired it) or from outside
    @Nullable
    private static EffectHandle play(Entity entity, AnimationTarget target, EffectEntry effect, float at, @Nullable LayerKey key, @Nullable AnimationTrack.Entry entry, float until,
                                     Supplier<Vec3> position, @Nullable QuerySource queries) {
        EffectContext context = new EffectContext(entity, target, effect.locator(), effect.follow(), effect.persist(), !Float.isNaN(effect.until()), position);
        AnimationEffect resolved = effect.molang() == null ? effect.effect() : effect.molang().resolve(entity, target, at, queries);
        PlayingEffect instance = resolved == null ? null : resolved.start(context);
        if (instance == null) {
            return null;
        }
        EffectHandle handle = new EffectHandle(instance, context, key, entry, entity.level().getGameTime(), until, effect.persist());
        entity.getData(ClientAttachments.EFFECTS).running().add(handle);
        return handle;
    }

    private static void fire(Entity entity, LayerKey key, LayerState layer, long now) {
        AnimationTrack track = layer.track();
        track.update(now);
        AnimationTrack.Entry entry = track.entry(track.size() - 1);
        float elapsed = entry.elapsed(now);
        boolean firstPerson = entity instanceof LivingEntity living && BuiltinQueries.isFirstPerson(living);
        float from;
        if (entry == layer.firedEntry()) {
            from = layer.firedTime();
        } else {
            // What the previous entry had left before it was replaced, which a render can do between two ticks
            AnimationTrack.Entry previous = layer.firedEntry();
            int index = previous == null ? -1 : track.indexOf(previous);
            if (index >= 0) {
                fire(entity, key, previous, track.animation(index, firstPerson), layer.firedTime(), previous.elapsed(Math.min(now, entry.startTick())), now);
            }
            from = now - entry.startTick() <= LATE_TICKS ? BEFORE_START : elapsed;
        }
        layer.fired(entry, elapsed);
        fire(entity, key, entry, track.animation(track.size() - 1, firstPerson), from, elapsed, now);
    }

    private static void fire(Entity entity, LayerKey key, AnimationTrack.Entry entry, @Nullable JasperAnimation animation, float from, float to, long now) {
        if (animation == null || animation.effects().isEmpty()) {
            return;
        }
        animation.effects().fire(from, to, animation.length(), entry.cycles(animation), (effect, at) -> {
            if (!effect.effect().visual() || visible(entity, key.target(), effect, now)) {
                start(entity, key, entry, effect, at);
            }
        });
    }

    private static void start(Entity entity, LayerKey key, AnimationTrack.Entry entry, EffectEntry effect, float at) {
        float until = Float.isNaN(effect.until()) || Float.isInfinite(effect.until()) ? effect.until() : at - effect.time() + effect.until();
        play(entity, key.target(), effect, at, key, entry, until, entity::position, null);
    }

    private static boolean visible(Entity entity, AnimationTarget target, EffectEntry effect, long now) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean firstPerson = entity == minecraft.player && entity == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
        if (target == AnimationTarget.MODEL) {
            return !firstPerson;
        }
        if (firstPerson || !effect.effect().placed()) {
            return true;
        }
        // Outside first person only 3D items capture hand locators; a flat item's effects would land at the feet
        LocatorData data = entity.getExistingDataOrNull(ClientAttachments.LOCATORS);
        if (data == null || effect.locator() == null && effect.effect().locators().isEmpty()) {
            return false;
        }
        if (effect.locator() != null && data.get(target, effect.locator(), now - EffectContext.FRESH_TICKS) == null) {
            return false;
        }
        for (String locator : effect.effect().locators()) {
            if (data.get(target, locator, now - EffectContext.FRESH_TICKS) == null) {
                return false;
            }
        }
        return true;
    }
}
