package com.caszgamermd.caszualtvtime.network.payload;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record StartBroadcastAckPayload(
    UUID sessionId,
    String channel
) implements CustomPacketPayload {
    public static final Type<StartBroadcastAckPayload> TYPE =
        new Type<>(CaszualTvTime.id("start_broadcast_ack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartBroadcastAckPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public StartBroadcastAckPayload decode(RegistryFriendlyByteBuf buf) {
                return new StartBroadcastAckPayload(buf.readUUID(), buf.readUtf(64));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StartBroadcastAckPayload value) {
                buf.writeUUID(value.sessionId());
                buf.writeUtf(value.channel(), 64);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
