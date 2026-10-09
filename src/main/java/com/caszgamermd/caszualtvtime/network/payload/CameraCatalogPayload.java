package com.caszgamermd.caszualtvtime.network.payload;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record CameraCatalogPayload(BlockPos consolePos, List<CameraEntry> cameras) implements CustomPacketPayload {
    public static final Type<CameraCatalogPayload> TYPE = new Type<>(CaszualTvTime.id("camera_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraCatalogPayload> CODEC = new StreamCodec<>() {
        @Override public CameraCatalogPayload decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int count = buf.readVarInt();
            if (count < 0 || count > 64) throw new IllegalArgumentException("Camera list too large");
            List<CameraEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(new CameraEntry(buf.readBlockPos(), buf.readUtf(64),
                    buf.readUtf(64), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
            }
            return new CameraCatalogPayload(pos, entries);
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, CameraCatalogPayload p) {
            buf.writeBlockPos(p.consolePos());
            buf.writeVarInt(p.cameras().size());
            for (CameraEntry entry : p.cameras()) {
                buf.writeBlockPos(entry.pos());
                buf.writeUtf(entry.name(), 64);
                buf.writeUtf(entry.channel(), 64);
                buf.writeBoolean(entry.active());
                buf.writeFloat(entry.pan());
                buf.writeFloat(entry.tilt());
                buf.writeFloat(entry.zoom());
            }
        }
    };
    public record CameraEntry(BlockPos pos, String name, String channel,
                              boolean active, float pan, float tilt, float zoom) {}
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
