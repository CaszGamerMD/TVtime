package com.caszgamermd.caszualcaszual_tv_time.display;

import com.caszgamermd.caszualcaszual_tv_time.block.TvBlock;
import com.caszgamermd.caszualcaszual_tv_time.block.TvBlockEntity;
import com.caszgamermd.caszualcaszual_tv_time.broadcast.DisplayMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class TvDisplaySettings {
    private TvDisplaySettings() {
    }

    public static void apply(
        Level level,
        BlockPos anyTv,
        String channel,
        DisplayMode displayMode,
        boolean tvAudioEnabled
    ) {
        if (level.getBlockState(anyTv).is(com.caszgamermd.caszualcaszual_tv_time.block.ModBlocks.PORTABLE_TV)) {
            if (level.getBlockEntity(anyTv) instanceof TvBlockEntity tv) {
                tv.setChannel(channel);
                tv.setDisplayMode(displayMode);
                tv.setTvAudioEnabled(tvAudioEnabled);
            }
            return;
        }

        DisplayRect rect = TvDisplayScanner.scan(level, anyTv);
        Direction right = rightFor(rect.facing());

        for (int y = 0; y < rect.heightBlocks(); y++) {
            for (int x = 0; x < rect.widthBlocks(); x++) {
                BlockPos pos = rect.anchor().above(y).relative(right, x);
                if (level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
                    tv.setChannel(channel);
                    tv.setDisplayMode(displayMode);
                    tv.setTvAudioEnabled(tvAudioEnabled);
                }
            }
        }
    }

    private static Direction rightFor(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("TV facing must be horizontal");
        };
    }
}
