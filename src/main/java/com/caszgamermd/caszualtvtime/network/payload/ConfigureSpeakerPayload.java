package com.caszgamermd.caszualcaszual_tv_time.network.payload;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import com.caszgamermd.caszualcaszual_tv_time.audio.SpeakerChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigureSpeakerPayload(
    BlockPos pos,
    String channel,
    SpeakerChannel speakerChannel,
    float volume,
    int range
) implements CustomPacketPayload {
    public static final Type<ConfigureSpeakerPayload> TYPE =
        new Type<>(CaszualTvTime.id("configure_speaker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureSpeakerPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public ConfigureSpeakerPayload decode(RegistryFriendlyByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                String channel = buf.readUtf(64);

                SpeakerChannel[] values = SpeakerChannel.values();
                int roleId = buf.readUnsignedByte();
                SpeakerChannel role = roleId >= 0 && roleId < values.length
                    ? values[roleId]
                    : SpeakerChannel.FULL;

                float volume = buf.readFloat();
                int range = buf.readVarInt();

                return new ConfigureSpeakerPayload(
                    pos,
                    channel,
                    role,
                    volume,
                    range
                );
            }

            @Override
            public void encode(
                RegistryFriendlyByteBuf buf,
                ConfigureSpeakerPayload value
            ) {
                buf.writeBlockPos(value.pos());
                buf.writeUtf(value.channel(), 64);
                buf.writeByte(value.speakerChannel().ordinal());
                buf.writeFloat(value.volume());
                buf.writeVarInt(value.range());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
