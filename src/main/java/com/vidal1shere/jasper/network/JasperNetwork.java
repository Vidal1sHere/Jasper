package com.vidal1shere.jasper.network;

import com.vidal1shere.jasper.attachment.animation.AnimationLayer;
import com.vidal1shere.jasper.attachment.animation.PlayingAnimation;
import com.vidal1shere.jasper.client.item.ClientItemAnimations;
import com.vidal1shere.jasper.client.player.ClientPlayerAnimations;
import com.vidal1shere.jasper.network.packet.AnimationPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

public final class JasperNetwork {
    private static final String VERSION = "1";

    // Optional, so a client with Jasper still joins a server without it
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION).optional();
        registrar.playToClient(AnimationPayload.TYPE, AnimationPayload.STREAM_CODEC, JasperNetwork::handleAnimation);
    }

    public static void sendAnimation(ServerPlayer player, PlayingAnimation playing) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload(player, playing));
    }

    public static void sendAnimationTo(ServerPlayer watcher, ServerPlayer player, PlayingAnimation playing) {
        PacketDistributor.sendToPlayer(watcher, payload(player, playing));
    }

    public static void sendStop(ServerPlayer player, AnimationLayer layer, InteractionHand hand) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new AnimationPayload(player.getId(), layer, hand, Optional.empty(), player.level().getGameTime(), 1.0F));
    }

    private static AnimationPayload payload(ServerPlayer player, PlayingAnimation playing) {
        return new AnimationPayload(player.getId(), playing.layer(), playing.hand(), Optional.of(playing.animation()), playing.startTick(), playing.speed());
    }

    private static void handleAnimation(AnimationPayload payload, IPayloadContext context) {
        if (payload.layer() == AnimationLayer.ITEM) {
            ClientItemAnimations.set(payload.entityId(), payload.hand(), payload.animation().orElse(null), payload.startTick(), payload.speed());
        } else if (payload.animation().isPresent()) {
            ClientPlayerAnimations.play(payload.entityId(), payload.hand(), payload.animation().get(), payload.startTick(), payload.speed());
        } else {
            ClientPlayerAnimations.stop(payload.entityId(), payload.startTick());
        }
    }
}
