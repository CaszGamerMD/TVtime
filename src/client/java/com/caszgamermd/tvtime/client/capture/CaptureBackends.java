package com.caszgamermd.tvtime.client.capture;

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
                "TVtime window capture currently targets Windows clients."
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
            "TVtime Windows capture helper was not found. This build does not "
                + "contain a bundled helper. Put tvtime-capture.exe in the Minecraft "
                + "folder, config/tvtime/, or set -Dtvtime.capture.helper=<path> / "
                + "TVTIME_CAPTURE_HELPER."
        );
    }

    private static List<Path> candidatePaths() {
        List<Path> paths = new ArrayList<>();

        String configured = System.getProperty("tvtime.capture.helper", "").trim();
        if (!configured.isEmpty()) {
            paths.add(Path.of(configured));
        }

        String environment = System.getenv("TVTIME_CAPTURE_HELPER");
        if (environment != null && !environment.isBlank()) {
            paths.add(Path.of(environment.trim()));
        }

        Path gameDir = FabricLoader.getInstance().getGameDir();
        paths.add(gameDir.resolve("tvtime-capture.exe"));
        paths.add(gameDir.resolve("config").resolve("tvtime").resolve("tvtime-capture.exe"));

        return paths;
    }
}
