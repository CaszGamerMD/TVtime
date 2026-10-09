package com.caszgamermd.caszualcaszual_tv_time.client.capture;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BundledCaptureHelper {
    private static final String RESOURCE =
        "/caszual_tv_time/native/windows-x64/caszual-tv-time-capture.exe";

    private BundledCaptureHelper() {
    }

    public static Path extractIfPresent() {
        try (InputStream input =
            BundledCaptureHelper.class.getResourceAsStream(RESOURCE)) {

            if (input == null) {
                return null;
            }

            Path directory = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("caszual_tv_time")
                .resolve("native");

            Files.createDirectories(directory);

            Path target = directory.resolve("caszual-tv-time-capture.exe");
            Path temporary = directory.resolve("caszual-tv-time-capture.exe.tmp");

            Files.copy(
                input,
                temporary,
                StandardCopyOption.REPLACE_EXISTING
            );

            if (Files.isRegularFile(target)
                && Files.mismatch(target, temporary) == -1L) {
                Files.deleteIfExists(temporary);
                return target;
            }

            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicMoveUnavailable) {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }

            return target;
        } catch (IOException ignored) {
            return null;
        }
    }
}
