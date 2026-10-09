package com.caszgamermd.caszualtvtime.client.capture;

public record CaptureOptions(
    int maxWidth,
    int maxHeight,
    int maxFps,
    boolean captureAudio
) {
    public CaptureOptions {
        if (maxWidth <= 0 || maxHeight <= 0 || maxFps <= 0) {
            throw new IllegalArgumentException("Capture limits must be positive");
        }
    }

    public static CaptureOptions defaults() {
        return new CaptureOptions(1280, 720, 30, true);
    }
}
