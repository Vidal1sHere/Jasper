package com.vidal1shere.jasper.network.packet;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.attachment.animation.AnimationLayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.Optional;

public record AnimationPayload(int entityId, AnimationLayer layer, InteractionHand hand, Optional<ResourceLocation> animation, long startTick, float speed) implements CustomPacketPayload {
    public static final Type<AnimationPayload> TYPE = new Type<>(Jasper.id("animation"));

    public static final StreamCodec<FriendlyByteBuf, AnimationPayload> STREAM_CODEC = StreamCodec.of(AnimationPayload::write, AnimationPayload::read);

    private static void write(FriendlyByteBuf buffer, AnimationPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeEnum(payload.layer);
        buffer.writeEnum(payload.hand);
        buffer.writeOptional(payload.animation, FriendlyByteBuf::writeResourceLocation);
        buffer.writeVarLong(payload.startTick);
        buffer.writeFloat(payload.speed);
    }

    private static AnimationPayload read(FriendlyByteBuf buffer) {
        return new AnimationPayload(buffer.readVarInt(), buffer.readEnum(AnimationLayer.class), buffer.readEnum(InteractionHand.class),
                buffer.readOptional(FriendlyByteBuf::readResourceLocation), buffer.readVarLong(), buffer.readFloat());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
