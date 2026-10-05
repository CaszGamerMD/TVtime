package com.caszgamermd.tvtime.display;

import com.caszgamermd.tvtime.block.ModBlocks;
import com.caszgamermd.tvtime.block.TvBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class TvDisplayScanner {
    private static final int MAX = ConnectedDisplay.MAX_WIDTH_BLOCKS;

    private TvDisplayScanner() {
    }

    public static DisplayRect scan(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!originState.is(ModBlocks.TV)) {
            throw new IllegalArgumentException("Origin is not a TV block");
        }

        Direction facing = originState.getValue(TvBlock.FACING);
        Direction right = rightFor(facing);

        int leftExtent = extent(level, origin, right.getOpposite(), facing, MAX - 1);
        int rightExtent = extent(level, origin, right, facing, MAX - 1);

        int width = Math.min(MAX, leftExtent + 1 + rightExtent);
        BlockPos rowStart = origin.relative(right.getOpposite(), leftExtent);

        int downExtent = rowExtent(level, rowStart, width, right, Direction.DOWN, facing, MAX - 1);
        int upExtent = rowExtent(level, rowStart, width, right, Direction.UP, facing, MAX - 1);

        int height = Math.min(MAX, downExtent + 1 + upExtent);
        BlockPos anchor = rowStart.below(downExtent);

        return new DisplayRect(anchor.immutable(), width, height, facing);
    }

    private static int extent(
        Level level,
        BlockPos start,
        Direction direction,
        Direction facing,
        int limit
    ) {
        int found = 0;
        for (int i = 1; i <= limit; i++) {
            if (!isMatchingTv(level, start.relative(direction, i), facing)) {
                break;
            }
            found++;
        }
        return found;
    }

    private static int rowExtent(
        Level level,
        BlockPos baseRowStart,
        int width,
        Direction right,
        Direction vertical,
        Direction facing,
        int limit
    ) {
        int found = 0;
        for (int row = 1; row <= limit; row++) {
            BlockPos candidateStart = baseRowStart.relative(vertical, row);
            if (!rowMatches(level, candidateStart, width, right, facing)) {
                break;
            }
            found++;
        }
        return found;
    }

    private static boolean rowMatches(
        Level level,
        BlockPos rowStart,
        int width,
        Direction right,
        Direction facing
    ) {
        for (int x = 0; x < width; x++) {
            if (!isMatchingTv(level, rowStart.relative(right, x), facing)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMatchingTv(Level level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.TV)
            && state.hasProperty(TvBlock.FACING)
            && state.getValue(TvBlock.FACING) == facing;
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
