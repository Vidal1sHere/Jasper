package com.vidal1shere.jasper;

import com.vidal1shere.jasper.attachment.AnimationData;
import com.vidal1shere.jasper.attachment.animation.AnimationSlot;
import com.vidal1shere.jasper.attachment.animation.PlayingAnimation;
import com.vidal1shere.jasper.attachment.savedanimation.SavedAnimation;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.EndCondition;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.PlayOptions;
import com.vidal1shere.jasper.network.JasperNetwork;
import com.vidal1shere.jasper.registry.JasperAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// The server side of the animations: synced to the entity and its watchers, ended and saved by the policy of each layer
@EventBusSubscriber(modid = Jasper.MOD_ID)
public final class JasperAnimations {
    public static long play(LivingEntity entity, Layer layer, AnimationTarget target, ResourceLocation animation, PlayOptions options) {
        long now = entity.level().getGameTime();
        if (layer.policy().persistent()) {
            entity.setData(JasperAttachments.SAVED_ANIMATIONS, entity.getData(JasperAttachments.SAVED_ANIMATIONS)
                    .with(new SavedAnimation(target, layer.id(), animation, now, options)));
        }
        start(entity, new AnimationSlot(target, layer), animation, now, options);
        return now;
    }

    public static void stop(LivingEntity entity, Layer layer, AnimationTarget target) {
        stop(entity, layer, target, null);
    }

    // Only stops it if the animation that started at that tick is still the one playing
    public static void stop(LivingEntity entity, Layer layer, AnimationTarget target, @Nullable Long startTick) {
        AnimationSlot slot = new AnimationSlot(target, layer);
        AnimationData tracks = entity.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        PlayingAnimation playing = tracks == null ? null : tracks.playing().get(slot);
        if (playing == null || startTick != null && playing.startTick() != startTick) {
            return;
        }
        if (entity.hasData(JasperAttachments.SAVED_ANIMATIONS)) {
            entity.setData(JasperAttachments.SAVED_ANIMATIONS, entity.getData(JasperAttachments.SAVED_ANIMATIONS).without(target, layer.id()));
        }
        tracks.playing().remove(slot);
        JasperNetwork.sendStop(entity, playing);
        if (tracks.playing().isEmpty()) {
            entity.removeData(JasperAttachments.ANIMATIONS);
        }
    }

    public static void stopAll(LivingEntity entity) {
        entity.removeData(JasperAttachments.SAVED_ANIMATIONS);
        AnimationData tracks = entity.removeData(JasperAttachments.ANIMATIONS);
        if (tracks != null) {
            tracks.playing().values().forEach(playing -> JasperNetwork.sendStop(entity, playing));
        }
    }

    private static void start(LivingEntity entity, AnimationSlot slot, ResourceLocation animation, long startTick, PlayOptions options) {
        Item item = slot.layer().policy().endsOn(EndCondition.ITEM_CHANGE) ? entity.getItemInHand(options.hand()).getItem() : Items.AIR;
        PlayingAnimation playing = new PlayingAnimation(slot, animation, startTick, options, item);
        entity.getData(JasperAttachments.ANIMATIONS).playing().put(slot, playing);
        JasperNetwork.sendAnimation(entity, playing);
    }

    private static void end(LivingEntity entity, EndCondition condition) {
        AnimationData tracks = entity.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks != null) {
            for (PlayingAnimation playing : List.copyOf(tracks.playing().values())) {
                if (playing.slot().layer().policy().endsOn(condition)) {
                    stop(entity, playing.slot().layer(), playing.slot().target());
                }
            }
        }
    }

    // Saved animations are sent again for a client that does not have them yet
    private static void restore(LivingEntity entity) {
        if (!entity.hasData(JasperAttachments.SAVED_ANIMATIONS)) {
            return;
        }
        for (SavedAnimation saved : entity.getData(JasperAttachments.SAVED_ANIMATIONS).animations()) {
            Layer layer = JasperLayers.get(saved.layer());
            if (layer != null) {
                start(entity, new AnimationSlot(saved.target(), layer), saved.animation(), saved.startTick(), saved.options());
            }
        }
    }

    private static void resend(LivingEntity entity) {
        AnimationData tracks = entity.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks != null) {
            tracks.playing().values().forEach(playing -> JasperNetwork.sendAnimation(entity, playing));
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        AnimationData tracks = entity.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks == null) {
            return;
        }
        for (PlayingAnimation playing : List.copyOf(tracks.playing().values())) {
            if (playing.slot().layer().policy().endsOn(EndCondition.ITEM_CHANGE) && entity.getItemInHand(playing.options().hand()).getItem() != playing.item()) {
                stop(entity, playing.slot().layer(), playing.slot().target());
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        AnimationData tracks = target.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks != null) {
            tracks.playing().values().forEach(playing -> JasperNetwork.sendAnimationTo(watcher, target, playing));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            end(event.getEntity(), EndCondition.DEATH);
        }
    }

    // Players come back through their own events below, once their client knows them
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity && !(entity instanceof ServerPlayer)) {
            restore(entity);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restore(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restore(player);
        }
    }

    // The client builds a new player, so whatever is still playing is sent again
    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player, EndCondition.DIMENSION_CHANGE);
            resend(player);
        }
    }
}
