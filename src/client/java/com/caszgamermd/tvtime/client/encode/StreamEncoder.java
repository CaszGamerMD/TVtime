package com.caszgamermd.tvtime.client.encode;

import com.caszgamermd.tvtime.client.capture.CapturedAudioChunk;
import com.caszgamermd.tvtime.client.capture.CapturedVideoFrame;

import java.util.function.Consumer;

public interface StreamEncoder extends AutoCloseable {
    void start(Consumer<EncodedPacket> output) throws Exception;

    void encodeVideo(CapturedVideoFrame frame);

    void encodeAudio(CapturedAudioChunk chunk);

    void stop();

    boolean running();

    @Override
    default void close() {
        stop();
    }
}
