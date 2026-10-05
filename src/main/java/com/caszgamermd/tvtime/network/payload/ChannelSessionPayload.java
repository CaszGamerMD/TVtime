package com.caszgamermd.tvtime.network.payload;

import com.caszgamermd.tvtime.TVtime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ChannelSessionPayload(
    String channel,
    UUID sessionId,
    boolean active
) implements CustomPacketPayload {
    public static final Type<ChannelSessionPayload> TYPE =
        new Type<>(TVtime.id("channel_session"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChannelSessionPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public ChannelSessionPayload decode(RegistryFriendlyByteBuf buf) {
                return new ChannelSessionPayload(
                    buf.readUtf(64),
                    buf.readUUID(),
                    buf.readBoolean()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ChannelSessionPayload value) {
                buf.writeUtf(value.channel(), 64);
                buf.writeUUID(value.sessionId());
                buf.writeBoolean(value.active());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
