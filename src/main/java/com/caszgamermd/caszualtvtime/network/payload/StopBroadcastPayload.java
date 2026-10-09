package com.caszgamermd.caszualcaszual_tv_time.network.payload;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record StopBroadcastPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<StopBroadcastPayload> TYPE =
        new Type<>(CaszualTvTime.id("stop_broadcast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopBroadcastPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public StopBroadcastPayload decode(RegistryFriendlyByteBuf buf) {
                return new StopBroadcastPayload(buf.readUUID());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StopBroadcastPayload value) {
                buf.writeUUID(value.sessionId());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
