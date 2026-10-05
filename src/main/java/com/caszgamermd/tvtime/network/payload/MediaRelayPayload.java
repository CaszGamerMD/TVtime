package com.caszgamermd.tvtime.network.payload;

import com.caszgamermd.tvtime.TVtime;
import com.caszgamermd.tvtime.network.MediaChunk;
import com.caszgamermd.tvtime.network.MediaKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record MediaRelayPayload(
    UUID sessionId,
    MediaKind kind,
    long sequence,
    long presentationTimeMicros,
    boolean keyFrame,
    byte[] payload
) implements CustomPacketPayload {
    public static final int MAX_MEDIA_BYTES = 48 * 1024;

    public static final Type<MediaRelayPayload> TYPE =
        new Type<>(TVtime.id("media_relay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MediaRelayPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public MediaRelayPayload decode(RegistryFriendlyByteBuf buf) {
                UUID sessionId = buf.readUUID();
                int kindId = buf.readUnsignedByte();
                MediaKind kind = kindId == 0 ? MediaKind.VIDEO : MediaKind.AUDIO;
                long sequence = buf.readVarLong();
                long pts = buf.readVarLong();
                boolean keyFrame = buf.readBoolean();
                byte[] payload = buf.readByteArray(MAX_MEDIA_BYTES);
                return new MediaRelayPayload(sessionId, kind, sequence, pts, keyFrame, payload);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, MediaRelayPayload value) {
                if (value.payload().length > MAX_MEDIA_BYTES) {
                    throw new IllegalArgumentException("Media packet exceeds " + MAX_MEDIA_BYTES + " bytes");
                }
                buf.writeUUID(value.sessionId());
                buf.writeByte(value.kind() == MediaKind.VIDEO ? 0 : 1);
                buf.writeVarLong(value.sequence());
                buf.writeVarLong(value.presentationTimeMicros());
                buf.writeBoolean(value.keyFrame());
                buf.writeByteArray(value.payload());
            }
        };

    public MediaChunk asChunk() {
        return new MediaChunk(
            sessionId,
            kind,
            sequence,
            presentationTimeMicros,
            keyFrame,
            payload
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
