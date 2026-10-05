package com.caszgamermd.tvtime.client.audio;

import com.caszgamermd.tvtime.block.SpeakerBlockEntity;
import com.caszgamermd.tvtime.block.TvBlockEntity;
import com.caszgamermd.tvtime.client.network.ClientChannelDirectory;
import com.caszgamermd.tvtime.client.network.ClientChannelSubscriptions;
import com.caszgamermd.tvtime.display.DisplayRect;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class NearbyAudioSourceScanner {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int MAX_RANGE_BLOCKS = 128;
    private static final int TV_AUDIO_RANGE = 24;
    private static final int CHUNK_RADIUS =
        (MAX_RANGE_BLOCKS + 15) / 16;

    private static int ticks;

    private NearbyAudioSourceScanner() {
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            ticks = 0;
            return;
        }

        if (++ticks < SCAN_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        BlockPos playerPos = client.player.blockPosition();
        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;

        for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                LevelChunk chunk = client.level
                    .getChunkSource()
                    .getChunk(
                        centerChunkX + dx,
                        centerChunkZ + dz,
                        ChunkStatus.FULL,
                        false
                    );

                if (chunk == null) {
                    continue;
                }

                chunk.getBlockEntities().values().forEach(blockEntity -> {
                    if (blockEntity instanceof SpeakerBlockEntity speaker) {
                        scanSpeaker(playerPos, speaker);
                    } else if (blockEntity instanceof TvBlockEntity tv) {
                        scanTv(playerPos, tv);
                    }
                });
            }
        }
    }

    private static void scanSpeaker(
        BlockPos playerPos,
        SpeakerBlockEntity speaker
    ) {
        String channel = speaker.channel();
        if (channel.isBlank()) {
            return;
        }

        BlockPos pos = speaker.getBlockPos();
        int range = speaker.range();

        if (playerPos.distSqr(pos) > (double) range * range) {
            return;
        }

        ClientChannelSubscriptions.markSeen(channel);

        UUID sessionId = ClientChannelDirectory.sessionFor(channel);
        if (sessionId == null) {
            return;
        }

        TvAudioAnchors.markSeen(
            sessionId,
            pos,
            Vec3.atCenterOf(pos),
            speaker.speakerChannel(),
            speaker.volume(),
            range
        );
    }

    private static void scanTv(
        BlockPos playerPos,
        TvBlockEntity tv
    ) {
        if (!tv.tvAudioEnabled() || tv.channel().isBlank()) {
            return;
        }

        DisplayRect rect = tv.displayRect();
        if (!tv.getBlockPos().equals(rect.anchor())) {
            return;
        }

        Vec3 center = displayCenter(rect);

        double dx = playerPos.getX() + 0.5 - center.x;
        double dy = playerPos.getY() + 0.5 - center.y;
        double dz = playerPos.getZ() + 0.5 - center.z;

        if (dx * dx + dy * dy + dz * dz
            > (double) TV_AUDIO_RANGE * TV_AUDIO_RANGE) {
            return;
        }

        ClientChannelSubscriptions.markSeen(tv.channel());

        UUID sessionId =
            ClientChannelDirectory.sessionFor(tv.channel());

        if (sessionId == null) {
            return;
        }

        TvAudioAnchors.markSeen(
            sessionId,
            rect.anchor(),
            center,
            1.0f,
            TV_AUDIO_RANGE
        );
    }

    private static Vec3 displayCenter(DisplayRect rect) {
        Direction right = rightFor(rect.facing());
        double halfSpan = (rect.widthBlocks() - 1) / 2.0;

        double x = rect.anchor().getX()
            + 0.5
            + right.getStepX() * halfSpan;
        double z = rect.anchor().getZ()
            + 0.5
            + right.getStepZ() * halfSpan;
        double y = rect.anchor().getY()
            + rect.heightBlocks() / 2.0;

        return new Vec3(x, y, z);
    }

    private static Direction rightFor(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }
}
