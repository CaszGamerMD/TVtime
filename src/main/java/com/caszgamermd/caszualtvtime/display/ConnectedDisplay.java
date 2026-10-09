package com.caszgamermd.caszualtvtime.display;

import com.caszgamermd.caszualtvtime.broadcast.DisplayMode;

import java.util.Objects;
import java.util.UUID;

public record ConnectedDisplay(
    UUID id,
    int widthBlocks,
    int heightBlocks,
    DisplayMode displayMode,
    String channel
) {
    public static final int MAX_WIDTH_BLOCKS = 16;
    public static final int MAX_HEIGHT_BLOCKS = 16;

    public ConnectedDisplay {
        Objects.requireNonNull(id);
        Objects.requireNonNull(displayMode);

        if (widthBlocks < 1 || heightBlocks < 1) {
            throw new IllegalArgumentException("Display dimensions must be positive");
        }
        if (widthBlocks > MAX_WIDTH_BLOCKS || heightBlocks > MAX_HEIGHT_BLOCKS) {
            throw new IllegalArgumentException("Display exceeds 16x16 block limit");
        }
    }

    public boolean tuned() {
        return channel != null && !channel.isBlank();
    }
}
