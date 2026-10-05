package com.caszgamermd.tvtime.client.capture;

import java.util.Locale;

public final class CaptureBackends {
    private CaptureBackends() {
    }

    public static WindowCaptureBackend createDefault() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            // Replaced by WindowsGraphicsCaptureBackend once the native helper
            // is bundled. Keeping selection centralized prevents native classes
            // from ever loading on dedicated servers or non-Windows clients.
            return new UnsupportedCaptureBackend(
                "Windows capture helper is not bundled in this development build yet."
            );
        }

        return new UnsupportedCaptureBackend(
            "TVtime window capture currently targets Windows clients."
        );
    }
}
