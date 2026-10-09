package com.caszgamermd.caszualcaszual_tv_time.network.payload;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StartBroadcastPayload(
    String channel,
    int width,
    int height,
    int fps,
    int videoBitrateKbps
) implements CustomPacketPayload {
    public static final Type<StartBroadcastPayload> TYPE =
        new Type<>(CaszualTvTime.id("start_broadcast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartBroadcastPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public StartBroadcastPayload decode(RegistryFriendlyByteBuf buf) {
                return new StartBroadcastPayload(
                    buf.readUtf(64),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StartBroadcastPayload value) {
                buf.writeUtf(value.channel(), 64);
                buf.writeVarInt(value.width());
                buf.writeVarInt(value.height());
                buf.writeVarInt(value.fps());
                buf.writeVarInt(value.videoBitrateKbps());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
