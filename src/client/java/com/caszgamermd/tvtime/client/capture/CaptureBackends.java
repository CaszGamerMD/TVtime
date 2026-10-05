package com.caszgamermd.tvtime.client.capture;

import java.nio.file.Path;
import java.util.Locale;

public final class CaptureBackends {
    private CaptureBackends() {
    }

    public static WindowCaptureBackend createDefault() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (!os.contains("win")) {
            return new UnsupportedCaptureBackend(
                "TVtime window capture currently targets Windows clients."
            );
        }

        String configured = System.getProperty("tvtime.capture.helper", "").trim();
        if (configured.isEmpty()) {
            configured = System.getenv("TVTIME_CAPTURE_HELPER");
            configured = configured == null ? "" : configured.trim();
        }

        if (!configured.isEmpty()) {
            HelperProcessCaptureBackend backend =
                new HelperProcessCaptureBackend(Path.of(configured));
            if (backend.available()) {
                return backend;
            }
        }

        return new UnsupportedCaptureBackend(
            "TVtime Windows capture helper is not bundled in this development build. "
                + "Set -Dtvtime.capture.helper=<path-to-tvtime-capture.exe> "
                + "or TVTIME_CAPTURE_HELPER to test the native helper."
        );
    }
}
