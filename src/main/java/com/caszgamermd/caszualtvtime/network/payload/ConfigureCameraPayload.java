package com.caszgamermd.caszualtvtime.network.payload;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigureCameraPayload(
    BlockPos consolePos, BlockPos cameraPos,
    String name, String channel, boolean active, float pan, float tilt, float zoom
) implements CustomPacketPayload {
    public static final Type<ConfigureCameraPayload> TYPE = new Type<>(CaszualTvTime.id("configure_camera"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureCameraPayload> CODEC = new StreamCodec<>() {
        @Override public ConfigureCameraPayload decode(RegistryFriendlyByteBuf buf) {
            return new ConfigureCameraPayload(buf.readBlockPos(), buf.readBlockPos(), buf.readUtf(32),
                buf.readUtf(64), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, ConfigureCameraPayload p) {
            buf.writeBlockPos(p.consolePos());
            buf.writeBlockPos(p.cameraPos());
            buf.writeUtf(p.name(), 32);
            buf.writeUtf(p.channel(), 64);
            buf.writeBoolean(p.active());
            buf.writeFloat(p.pan());
            buf.writeFloat(p.tilt());
            buf.writeFloat(p.zoom());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
