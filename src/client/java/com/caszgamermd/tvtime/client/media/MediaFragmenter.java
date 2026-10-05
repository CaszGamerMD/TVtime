package com.caszgamermd.tvtime.client.media;

import com.caszgamermd.tvtime.network.MediaKind;
import com.caszgamermd.tvtime.network.payload.MediaFragmentPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MediaFragmenter {
    private MediaFragmenter() {
    }

    public static List<MediaFragmentPayload> fragment(
        UUID sessionId,
        MediaKind kind,
        long sequence,
        long presentationTimeMicros,
        boolean keyFrame,
        byte[] encoded
    ) {
        int max = MediaFragmentPayload.MAX_FRAGMENT_BYTES;
        int count = Math.max(1, (encoded.length + max - 1) / max);

        if (count > MediaFragmentPayload.MAX_FRAGMENTS) {
            throw new IllegalArgumentException(
                "Encoded media frame needs " + count + " fragments"
            );
        }

        List<MediaFragmentPayload> fragments = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int start = i * max;
            int length = Math.min(max, encoded.length - start);
            byte[] bytes = new byte[length];
            System.arraycopy(encoded, start, bytes, 0, length);

            fragments.add(new MediaFragmentPayload(
                sessionId,
                kind,
                sequence,
                presentationTimeMicros,
                keyFrame,
                i,
                count,
                bytes
            ));
        }

        return List.copyOf(fragments);
    }
}
