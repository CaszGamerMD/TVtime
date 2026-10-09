package com.caszgamermd.caszualcaszual_tv_time.network.payload;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import com.caszgamermd.caszualcaszual_tv_time.broadcast.DisplayMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigureTvPayload(
    BlockPos pos,
    String channel,
    DisplayMode displayMode,
    boolean tvAudioEnabled
) implements CustomPacketPayload {
    public static final Type<ConfigureTvPayload> TYPE =
        new Type<>(CaszualTvTime.id("configure_tv"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureTvPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public ConfigureTvPayload decode(RegistryFriendlyByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                String channel = buf.readUtf(64);
                int mode = buf.readUnsignedByte();
                DisplayMode[] values = DisplayMode.values();
                DisplayMode displayMode = mode >= 0 && mode < values.length
                    ? values[mode]
                    : DisplayMode.FIT;
                boolean audio = buf.readBoolean();
                return new ConfigureTvPayload(pos, channel, displayMode, audio);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ConfigureTvPayload value) {
                buf.writeBlockPos(value.pos());
                buf.writeUtf(value.channel(), 64);
                buf.writeByte(value.displayMode().ordinal());
                buf.writeBoolean(value.tvAudioEnabled());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
