package com.caszgamermd.caszualtvtime.client.capture;

import java.util.List;

public final class UnsupportedCaptureBackend implements WindowCaptureBackend {
    private final String reason;

    public UnsupportedCaptureBackend(String reason) {
        this.reason = reason;
    }

    @Override
    public String name() {
        return "Unavailable";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<CaptureWindow> listWindows() {
        return List.of();
    }

    @Override
    public void start(CaptureWindow window, CaptureOptions options, Listener listener) {
        throw new IllegalStateException(reason);
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean running() {
        return false;
    }

    public String reason() {
        return reason;
    }
}
