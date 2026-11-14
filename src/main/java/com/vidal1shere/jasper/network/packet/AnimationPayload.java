package com.vidal1shere.jasper.network.packet;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.Optional;

public record AnimationPayload(int entityId, AnimationTarget target, ResourceLocation layer, Optional<ResourceLocation> animation, long startTick, PlayOptions options) implements CustomPacketPayload {
    public static final Type<AnimationPayload> TYPE = new Type<>(Jasper.id("animation"));

    public static final StreamCodec<FriendlyByteBuf, AnimationPayload> STREAM_CODEC = StreamCodec.of(AnimationPayload::write, AnimationPayload::read);

    private static void write(FriendlyByteBuf buffer, AnimationPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeEnum(payload.target);
        buffer.writeResourceLocation(payload.layer);
        buffer.writeOptional(payload.animation, FriendlyByteBuf::writeResourceLocation);
        buffer.writeVarLong(payload.startTick);
        buffer.writeFloat(payload.options.speed());
        buffer.writeEnum(payload.options.hand());
        buffer.writeBoolean(payload.options.loop());
    }

    private static AnimationPayload read(FriendlyByteBuf buffer) {
        return new AnimationPayload(buffer.readVarInt(), buffer.readEnum(AnimationTarget.class), buffer.readResourceLocation(), buffer.readOptional(FriendlyByteBuf::readResourceLocation),
                buffer.readVarLong(), new PlayOptions(buffer.readFloat(), buffer.readEnum(InteractionHand.class), buffer.readBoolean()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
