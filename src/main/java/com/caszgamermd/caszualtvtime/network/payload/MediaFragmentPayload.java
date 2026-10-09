package com.caszgamermd.caszualcaszual_tv_time.network.payload;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import com.caszgamermd.caszualcaszual_tv_time.network.MediaKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record MediaFragmentPayload(
    UUID sessionId,
    MediaKind kind,
    long sequence,
    long presentationTimeMicros,
    boolean keyFrame,
    int fragmentIndex,
    int fragmentCount,
    byte[] payload
) implements CustomPacketPayload {
    public static final int MAX_FRAGMENT_BYTES = 48 * 1024;
    public static final int MAX_FRAGMENTS = 32;

    public static final Type<MediaFragmentPayload> TYPE =
        new Type<>(CaszualTvTime.id("media_fragment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MediaFragmentPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public MediaFragmentPayload decode(RegistryFriendlyByteBuf buf) {
                UUID sessionId = buf.readUUID();
                MediaKind kind = buf.readUnsignedByte() == 0
                    ? MediaKind.VIDEO
                    : MediaKind.AUDIO;
                long sequence = buf.readVarLong();
                long pts = buf.readVarLong();
                boolean keyFrame = buf.readBoolean();
                int fragmentIndex = buf.readVarInt();
                int fragmentCount = buf.readVarInt();
                byte[] payload = buf.readByteArray(MAX_FRAGMENT_BYTES);

                return new MediaFragmentPayload(
                    sessionId,
                    kind,
                    sequence,
                    pts,
                    keyFrame,
                    fragmentIndex,
                    fragmentCount,
                    payload
                );
            }

            @Override
            public void encode(
                RegistryFriendlyByteBuf buf,
                MediaFragmentPayload value
            ) {
                validate(value);

                buf.writeUUID(value.sessionId());
                buf.writeByte(value.kind() == MediaKind.VIDEO ? 0 : 1);
                buf.writeVarLong(value.sequence());
                buf.writeVarLong(value.presentationTimeMicros());
                buf.writeBoolean(value.keyFrame());
                buf.writeVarInt(value.fragmentIndex());
                buf.writeVarInt(value.fragmentCount());
                buf.writeByteArray(value.payload());
            }
        };

    public static void validate(MediaFragmentPayload value) {
        if (value.fragmentCount() <= 0
            || value.fragmentCount() > MAX_FRAGMENTS
            || value.fragmentIndex() < 0
            || value.fragmentIndex() >= value.fragmentCount()) {
            throw new IllegalArgumentException("Invalid media fragment coordinates");
        }

        if (value.payload().length > MAX_FRAGMENT_BYTES) {
            throw new IllegalArgumentException(
                "Media fragment exceeds " + MAX_FRAGMENT_BYTES + " bytes"
            );
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
