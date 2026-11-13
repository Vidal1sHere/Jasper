package com.vidal1shere.jasper;

import com.vidal1shere.jasper.attachment.AnimationData;
import com.vidal1shere.jasper.attachment.animation.AnimationLayer;
import com.vidal1shere.jasper.attachment.animation.PlayingAnimation;
import com.vidal1shere.jasper.network.JasperNetwork;
import com.vidal1shere.jasper.registry.JasperAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = Jasper.MOD_ID)
public final class JasperAnimations {
    public static long playItem(ServerPlayer player, InteractionHand hand, ResourceLocation animation, float speed) {
        return start(player, AnimationLayer.ITEM, hand, animation, speed);
    }

    public static long playPlayer(ServerPlayer player, InteractionHand hand, ResourceLocation animation, float speed) {
        return start(player, AnimationLayer.PLAYER, hand, animation, speed);
    }

    public static void stopItem(ServerPlayer player, InteractionHand hand) {
        AnimationData tracks = player.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks != null && tracks.items().remove(hand) != null) {
            JasperNetwork.sendStop(player, AnimationLayer.ITEM, hand);
            clean(player, tracks);
        }
    }

    public static void stopItem(ServerPlayer player, InteractionHand hand, long startTick) {
        AnimationData tracks = player.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        PlayingAnimation item = tracks == null ? null : tracks.items().get(hand);
        if (item != null && item.startTick() == startTick) {
            stopItem(player, hand);
        }
    }

    public static void stopPlayer(ServerPlayer player) {
        AnimationData tracks = player.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks != null && tracks.player() != null) {
            InteractionHand hand = tracks.player().hand();
            tracks.setPlayer(null);
            JasperNetwork.sendStop(player, AnimationLayer.PLAYER, hand);
            clean(player, tracks);
        }
    }

    public static void stopPlayer(ServerPlayer player, long startTick) {
        AnimationData tracks = player.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks != null && tracks.player() != null && tracks.player().startTick() == startTick) {
            stopPlayer(player);
        }
    }

    public static void stopAll(ServerPlayer player) {
        AnimationData tracks = player.removeData(JasperAttachments.ANIMATIONS);
        if (tracks == null) {
            return;
        }
        tracks.items().keySet().forEach(hand -> JasperNetwork.sendStop(player, AnimationLayer.ITEM, hand));
        if (tracks.player() != null) {
            JasperNetwork.sendStop(player, AnimationLayer.PLAYER, tracks.player().hand());
        }
    }

    private static long start(ServerPlayer player, AnimationLayer layer, InteractionHand hand, ResourceLocation animation, float speed) {
        long now = player.level().getGameTime();
        PlayingAnimation playing = new PlayingAnimation(layer, hand, animation, now, speed, player.getItemInHand(hand).getItem());
        AnimationData tracks = player.getData(JasperAttachments.ANIMATIONS);
        if (layer == AnimationLayer.ITEM) {
            tracks.items().put(hand, playing);
        } else {
            tracks.setPlayer(playing);
        }
        JasperNetwork.sendAnimation(player, playing);
        return now;
    }

    private static void clean(ServerPlayer player, AnimationData tracks) {
        if (tracks.isEmpty()) {
            player.removeData(JasperAttachments.ANIMATIONS);
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
        Iterator<Map.Entry<InteractionHand, PlayingAnimation>> iterator = tracks.items().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<InteractionHand, PlayingAnimation> entry = iterator.next();
            InteractionHand hand = entry.getKey();
            if (player.getItemInHand(hand).getItem() != entry.getValue().item()) {
                iterator.remove();
                JasperNetwork.sendStop(player, AnimationLayer.ITEM, hand);
            }
        }
        if (tracks.player() != null && player.getItemInHand(tracks.player().hand()).getItem() != tracks.player().item()) {
            stopPlayer(player);
        }
        clean(player, tracks);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        AnimationData tracks = target.getExistingDataOrNull(JasperAttachments.ANIMATIONS);
        if (tracks != null) {
            tracks.items().values().forEach(playing -> JasperNetwork.sendAnimationTo(watcher, target, playing));
            if (tracks.player() != null) {
                JasperNetwork.sendAnimationTo(watcher, target, tracks.player());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stopAll(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stopAll(player);
        }
    }
}
