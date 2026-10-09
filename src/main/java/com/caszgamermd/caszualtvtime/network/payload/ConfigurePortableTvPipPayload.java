package com.caszgamermd.caszualcaszual_tv_time.network.payload;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import com.caszgamermd.caszualcaszual_tv_time.item.PipCorner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigurePortableTvPipPayload(
    PipCorner corner
) implements CustomPacketPayload {
    public static final Type<ConfigurePortableTvPipPayload> TYPE =
        new Type<>(CaszualTvTime.id("configure_portable_tv_pip"));

    public static final StreamCodec<
        RegistryFriendlyByteBuf,
        ConfigurePortableTvPipPayload
    > CODEC = new StreamCodec<>() {
        @Override
        public ConfigurePortableTvPipPayload decode(
            RegistryFriendlyByteBuf buf
        ) {
            int ordinal = buf.readUnsignedByte();
            PipCorner[] values = PipCorner.values();
            PipCorner corner = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : PipCorner.BOTTOM_RIGHT;
            return new ConfigurePortableTvPipPayload(corner);
        }

        @Override
        public void encode(
            RegistryFriendlyByteBuf buf,
            ConfigurePortableTvPipPayload value
        ) {
            buf.writeByte(value.corner().ordinal());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
