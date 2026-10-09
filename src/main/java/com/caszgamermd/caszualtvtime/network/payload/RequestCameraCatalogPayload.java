package com.caszgamermd.caszualcaszual_tv_time.network.payload;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestCameraCatalogPayload(BlockPos consolePos) implements CustomPacketPayload {
    public static final Type<RequestCameraCatalogPayload> TYPE = new Type<>(CaszualTvTime.id("camera_catalog_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCameraCatalogPayload> CODEC = new StreamCodec<>() {
        @Override public RequestCameraCatalogPayload decode(RegistryFriendlyByteBuf buf) {
            return new RequestCameraCatalogPayload(buf.readBlockPos());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, RequestCameraCatalogPayload value) {
            buf.writeBlockPos(value.consolePos());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
