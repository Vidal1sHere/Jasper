package com.vidal1shere.jasper.network;

import com.vidal1shere.jasper.attachment.animation.Channel;
import com.vidal1shere.jasper.attachment.animation.PlayingAnimation;
import com.vidal1shere.jasper.client.entity.ClientEntityAnimations;
import com.vidal1shere.jasper.client.item.ClientItemAnimations;
import com.vidal1shere.jasper.client.player.ClientPlayerAnimations;
import com.vidal1shere.jasper.network.packet.AnimationPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

public final class JasperNetwork {
    private static final String VERSION = "2";

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
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new AnimationPayload(entity.getId(), playing.layer(), playing.channel(), playing.hand(), Optional.empty(), entity.level().getGameTime(), 1.0F));
    }

    private static AnimationPayload payload(Entity entity, PlayingAnimation playing) {
        return new AnimationPayload(entity.getId(), playing.layer(), playing.channel(), playing.hand(), Optional.of(playing.animation()), playing.startTick(), playing.speed());
    }

    private static void handleAnimation(AnimationPayload payload, IPayloadContext context) {
        boolean overlay = payload.channel() == Channel.OVERLAY;
        switch (payload.layer()) {
            case ITEM -> ClientItemAnimations.set(payload.entityId(), payload.hand(), overlay, payload.animation().orElse(null), payload.startTick(), payload.speed());
            case PLAYER -> ClientPlayerAnimations.set(payload.entityId(), payload.hand(), overlay, payload.animation().orElse(null), payload.startTick(), payload.speed());
            case ENTITY -> ClientEntityAnimations.set(payload.entityId(), overlay, payload.animation().orElse(null), payload.startTick(), payload.speed());
        }
    }
}
