package com.vidal1shere.jasper;

import com.vidal1shere.jasper.attachment.AnimationData;
import com.vidal1shere.jasper.attachment.EntityOverlayData;
import com.vidal1shere.jasper.attachment.animation.AnimationLayer;
import com.vidal1shere.jasper.attachment.animation.AnimationSlot;
import com.vidal1shere.jasper.attachment.animation.Channel;
import com.vidal1shere.jasper.attachment.animation.PlayingAnimation;
import com.vidal1shere.jasper.network.JasperNetwork;
import com.vidal1shere.jasper.registry.JasperAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = Jasper.MOD_ID)
public final class JasperAnimations {
    public static long playItem(ServerPlayer player, InteractionHand hand, ResourceLocation animation, float speed) {
        return start(player, AnimationLayer.ITEM, Channel.ACTION, hand, animation, speed);
    }

    public static long playPlayer(ServerPlayer player, InteractionHand hand, ResourceLocation animation, float speed) {
        return start(player, AnimationLayer.PLAYER, Channel.ACTION, hand, animation, speed);
    }

    public static long setItemOverlay(ServerPlayer player, InteractionHand hand, ResourceLocation animation, float speed) {
        return start(player, AnimationLayer.ITEM, Channel.OVERLAY, hand, animation, speed);
    }

    public static long setPlayerOverlay(ServerPlayer player, InteractionHand hand, ResourceLocation animation, float speed) {
        return start(player, AnimationLayer.PLAYER, Channel.OVERLAY, hand, animation, speed);
    }

    public static void stopItem(ServerPlayer player, InteractionHand hand) {
        stop(player, AnimationSlot.of(AnimationLayer.ITEM, Channel.ACTION, hand), null);
    }

    public static void stopItem(ServerPlayer player, InteractionHand hand, long startTick) {
        stop(player, AnimationSlot.of(AnimationLayer.ITEM, Channel.ACTION, hand), startTick);
    }

    public static void stopPlayer(ServerPlayer player) {
        stop(player, AnimationSlot.of(AnimationLayer.PLAYER, Channel.ACTION, InteractionHand.MAIN_HAND), null);
    }

    public static void stopPlayer(ServerPlayer player, long startTick) {
        stop(player, AnimationSlot.of(AnimationLayer.PLAYER, Channel.ACTION, InteractionHand.MAIN_HAND), startTick);
    }

    public static void clearItemOverlay(ServerPlayer player, InteractionHand hand) {
        stop(player, AnimationSlot.of(AnimationLayer.ITEM, Channel.OVERLAY, hand), null);
    }

    public static void clearPlayerOverlay(ServerPlayer player) {
        stop(player, AnimationSlot.of(AnimationLayer.PLAYER, Channel.OVERLAY, InteractionHand.MAIN_HAND), null);
    }

    public static long playEntity(LivingEntity entity, ResourceLocation animation, float speed) {
        return start(entity, AnimationLayer.ENTITY, Channel.ACTION, InteractionHand.MAIN_HAND, animation, speed);
    }

    public static void stopEntity(LivingEntity entity) {
        stop(entity, AnimationSlot.of(AnimationLayer.ENTITY, Channel.ACTION, InteractionHand.MAIN_HAND), null);
    }

    public static void stopEntity(LivingEntity entity, long startTick) {
        stop(entity, AnimationSlot.of(AnimationLayer.ENTITY, Channel.ACTION, InteractionHand.MAIN_HAND), startTick);
    }

    public static long setEntityOverlay(LivingEntity entity, ResourceLocation animation, float speed) {
        entity.setData(JasperAttachments.ENTITY_OVERLAY, new EntityOverlayData(animation, speed));
        return start(entity, AnimationLayer.ENTITY, Channel.OVERLAY, InteractionHand.MAIN_HAND, animation, speed);
    }

    public static void clearEntityOverlay(LivingEntity entity) {
        entity.removeData(JasperAttachments.ENTITY_OVERLAY);
        stop(entity, AnimationSlot.of(AnimationLayer.ENTITY, Channel.OVERLAY, InteractionHand.MAIN_HAND), null);
    }

    public static void stopAll(LivingEntity entity) {
        AnimationData tracks = entity.removeData(JasperAttachments.ANIMATIONS);
        if (tracks != null) {
            tracks.playing().values().forEach(playing -> JasperNetwork.sendStop(entity, playing));
        }
    }

    private static long start(LivingEntity entity, AnimationLayer layer, Channel channel, InteractionHand hand, ResourceLocation animation, float speed) {
        long now = entity.level().getGameTime();
        PlayingAnimation playing = new PlayingAnimation(layer, channel, hand, animation, now, speed, layer == AnimationLayer.ENTITY ? Items.AIR : entity.getItemInHand(hand).getItem());
        entity.getData(JasperAttachments.ANIMATIONS).playing().put(playing.slot(), playing);
        JasperNetwork.sendAnimation(entity, playing);
        return now;
    }

    private static void stop(LivingEntity entity, AnimationSlot slot, @Nullable Long startTick) {
        AnimationData tracks = entity.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        PlayingAnimation playing = tracks == null ? null : tracks.playing().get(slot);
        if (playing == null || startTick != null && playing.startTick() != startTick) {
            return;
        }
        tracks.playing().remove(slot);
        JasperNetwork.sendStop(entity, playing);
        if (tracks.playing().isEmpty()) {
            entity.removeData(JasperAttachments.ANIMATIONS);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AnimationData tracks = player.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks == null) {
            return;
        }
        for (PlayingAnimation playing : List.copyOf(tracks.playing().values())) {
            if (playing.layer() != AnimationLayer.ENTITY && player.getItemInHand(playing.hand()).getItem() != playing.item()) {
                stop(player, playing.slot(), null);
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
            stopAll(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity && entity.hasData(JasperAttachments.ENTITY_OVERLAY)) {
            EntityOverlayData overlay = entity.getData(JasperAttachments.ENTITY_OVERLAY);
            start(entity, AnimationLayer.ENTITY, Channel.OVERLAY, InteractionHand.MAIN_HAND, overlay.animation(), overlay.speed());
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stopAll(player);
        }
    }
}
