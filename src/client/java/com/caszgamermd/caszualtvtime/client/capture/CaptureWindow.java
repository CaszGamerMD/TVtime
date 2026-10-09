package com.caszgamermd.caszualcaszual_tv_time.client.capture;

public record CaptureWindow(
    long nativeHandle,
    int processId,
    String processName,
    String title
) {
    public CaptureWindow {
        processName = processName == null ? "" : processName;
        title = title == null ? "" : title;
    }

    public String displayName() {
        if (!title.isBlank() && !processName.isBlank()) {
            return title + " — " + processName;
        }
        if (!title.isBlank()) {
            return title;
        }
        return processName.isBlank() ? "Window " + nativeHandle : processName;
    }
}
