package com.caszgamermd.caszualcaszual_tv_time.client.encode;

import com.caszgamermd.caszualcaszual_tv_time.client.capture.CapturedAudioChunk;
import com.caszgamermd.caszualcaszual_tv_time.client.capture.CapturedVideoFrame;

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
