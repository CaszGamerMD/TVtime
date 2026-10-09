package com.caszgamermd.caszualcaszual_tv_time.client.capture;

import java.util.List;

public interface WindowCaptureBackend extends AutoCloseable {
    interface Listener {
        void onVideoFrame(CapturedVideoFrame frame);
        void onAudioChunk(CapturedAudioChunk chunk);
        void onCaptureStopped(String reason);
    }

    String name();

    boolean available();

    List<CaptureWindow> listWindows();

    void start(
        CaptureWindow window,
        CaptureOptions options,
        Listener listener
    ) throws Exception;

    void stop();

    boolean running();

    @Override
    default void close() {
        stop();
    }
}
