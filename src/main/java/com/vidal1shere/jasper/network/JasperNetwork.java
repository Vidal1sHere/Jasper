package com.vidal1shere.jasper.network;

import com.vidal1shere.jasper.attachment.PlayingAnimation;
import com.vidal1shere.jasper.client.ClientAnimations;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.PlayOptions;
import com.vidal1shere.jasper.network.packet.AnimationPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

public final class JasperNetwork {
    private static final String VERSION = "4";

    // Optional, so a client with Jasper still joins a server without it
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION).optional();
        registrar.playToClient(AnimationPayload.TYPE, AnimationPayload.STREAM_CODEC, JasperNetwork::handleAnimation);
    }

    public static void sendAnimation(Entity entity, PlayingAnimation playing) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload(entity, playing));
    }

    public static void sendAnimationTo(ServerPlayer watcher, Entity entity, PlayingAnimation playing) {
        PacketDistributor.sendToPlayer(watcher, payload(entity, playing));
    }

    public static void sendStop(Entity entity, PlayingAnimation playing) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new AnimationPayload(entity.getId(), playing.key().target(), playing.key().layer().id(), Optional.empty(), entity.level().getGameTime(), PlayOptions.DEFAULT.withHand(playing.options().hand())));
    }

    private static AnimationPayload payload(Entity entity, PlayingAnimation playing) {
        return new AnimationPayload(entity.getId(), playing.key().target(), playing.key().layer().id(), Optional.of(playing.animation()), playing.startTick(), playing.options());
    }

    private static void handleAnimation(AnimationPayload payload, IPayloadContext context) {
        Layer layer = JasperLayers.get(payload.layer());
        if (layer != null) {
            ClientAnimations.receive(payload.entityId(), layer, payload.target(), payload.animation().orElse(null), payload.startTick(), payload.options());
        }
    }
}
