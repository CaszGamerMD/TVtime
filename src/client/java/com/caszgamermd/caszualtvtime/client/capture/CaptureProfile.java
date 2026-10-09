package com.caszgamermd.caszualcaszual_tv_time.client.capture;

public enum CaptureProfile {
    LOW(192, 108, 12),
    BALANCED(320, 180, 15),
    HIGH(640, 360, 20);

    private final int width;
    private final int height;
    private final int fps;

    CaptureProfile(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int fps() {
        return fps;
    }
}
