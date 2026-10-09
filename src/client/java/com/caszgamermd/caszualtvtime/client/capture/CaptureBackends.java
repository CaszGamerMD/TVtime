package com.caszgamermd.caszualtvtime.client.capture;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CaptureBackends {
    private CaptureBackends() {
    }

    public static WindowCaptureBackend createDefault() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (!os.contains("win")) {
            return new UnsupportedCaptureBackend(
                "Caszual TV Time window capture currently targets Windows clients."
            );
        }

        for (Path candidate : candidatePaths()) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return new HelperProcessCaptureBackend(candidate);
            }
        }

        Path bundled = BundledCaptureHelper.extractIfPresent();
        if (bundled != null && Files.isRegularFile(bundled)) {
            return new HelperProcessCaptureBackend(bundled);
        }

        return new UnsupportedCaptureBackend(
            "Caszual TV Time Windows capture helper was not found. This build does not "
                + "contain a bundled helper. Put caszual-tv-time-capture.exe in the Minecraft "
                + "folder, config/caszual_tv_time/, or set -Dcaszual_tv_time.capture.helper=<path> / "
                + "CASZUAL_TV_TIME_CAPTURE_HELPER."
        );
    }

    private static List<Path> candidatePaths() {
        List<Path> paths = new ArrayList<>();

        String configured = System.getProperty("caszual_tv_time.capture.helper", "").trim();
        if (!configured.isEmpty()) {
            paths.add(Path.of(configured));
        }

        String environment = System.getenv("CASZUAL_TV_TIME_CAPTURE_HELPER");
        if (environment != null && !environment.isBlank()) {
            paths.add(Path.of(environment.trim()));
        }

        Path gameDir = FabricLoader.getInstance().getGameDir();
        paths.add(gameDir.resolve("caszual-tv-time-capture.exe"));
        paths.add(gameDir.resolve("config").resolve("caszual_tv_time").resolve("caszual-tv-time-capture.exe"));

        return paths;
    }
}
