package com.caszgamermd.caszualcaszual_tv_time.config;

public record StreamLimits(
    int maxWidth,
    int maxHeight,
    int maxFps,
    int maxVideoBitrateKbps,
    int maxAudioBitrateKbps
) {
    public static StreamLimits defaults() {
        return new StreamLimits(1280, 720, 30, 4_000, 160);
    }

    public boolean allows(int width, int height, int fps, int videoBitrateKbps) {
        return width > 0
            && height > 0
            && fps > 0
            && width <= maxWidth
            && height <= maxHeight
            && fps <= maxFps
            && videoBitrateKbps <= maxVideoBitrateKbps;
    }
}
