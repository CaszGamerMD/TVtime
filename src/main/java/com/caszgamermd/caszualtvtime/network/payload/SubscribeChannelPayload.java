package com.caszgamermd.caszualtvtime.network.payload;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SubscribeChannelPayload(
    String channel,
    boolean subscribed
) implements CustomPacketPayload {
    public static final Type<SubscribeChannelPayload> TYPE =
        new Type<>(CaszualTvTime.id("subscribe_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubscribeChannelPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public SubscribeChannelPayload decode(RegistryFriendlyByteBuf buf) {
                return new SubscribeChannelPayload(buf.readUtf(64), buf.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, SubscribeChannelPayload value) {
                buf.writeUtf(value.channel(), 64);
                buf.writeBoolean(value.subscribed());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
