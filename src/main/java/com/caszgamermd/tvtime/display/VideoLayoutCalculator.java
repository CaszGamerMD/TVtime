package com.caszgamermd.tvtime.display;

import com.caszgamermd.tvtime.broadcast.DisplayMode;

public final class VideoLayoutCalculator {
    private VideoLayoutCalculator() {
    }

    public static VideoLayout calculate(
        int sourceWidth,
        int sourceHeight,
        float canvasWidth,
        float canvasHeight,
        DisplayMode mode
    ) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) {
            throw new IllegalArgumentException("Source and canvas dimensions must be positive");
        }

        if (mode == DisplayMode.STRETCH) {
            return new VideoLayout(0, 0, canvasWidth, canvasHeight, 0, 0, 1, 1);
        }

        float sourceAspect = (float) sourceWidth / sourceHeight;
        float canvasAspect = canvasWidth / canvasHeight;

        if (mode == DisplayMode.FIT) {
            float width;
            float height;

            if (sourceAspect > canvasAspect) {
                width = canvasWidth;
                height = width / sourceAspect;
            } else {
                height = canvasHeight;
                width = height * sourceAspect;
            }

            return new VideoLayout(
                (canvasWidth - width) / 2.0f,
                (canvasHeight - height) / 2.0f,
                width,
                height,
                0,
                0,
                1,
                1
            );
        }

        float visibleU = 1.0f;
        float visibleV = 1.0f;

        if (sourceAspect > canvasAspect) {
            visibleU = canvasAspect / sourceAspect;
        } else {
            visibleV = sourceAspect / canvasAspect;
        }

        float uMargin = (1.0f - visibleU) / 2.0f;
        float vMargin = (1.0f - visibleV) / 2.0f;

        return new VideoLayout(
            0,
            0,
            canvasWidth,
            canvasHeight,
            uMargin,
            vMargin,
            1.0f - uMargin,
            1.0f - vMargin
        );
    }
}
