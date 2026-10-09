package com.caszgamermd.caszualcaszual_tv_time.client.encode;

import com.caszgamermd.caszualcaszual_tv_time.network.MediaKind;

import java.util.Objects;

public record EncodedPacket(
    MediaKind kind,
    long sequence,
    long presentationTimeMicros,
    boolean keyFrame,
    byte[] bytes
) {
    public EncodedPacket {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(bytes, "bytes");
    }
}
