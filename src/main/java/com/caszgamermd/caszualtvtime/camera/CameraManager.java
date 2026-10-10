package com.caszgamermd.caszualtvtime.camera;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import com.caszgamermd.caszualtvtime.block.CameraBlockEntity;
import com.caszgamermd.caszualtvtime.block.ModBlocks;
import com.caszgamermd.caszualtvtime.broadcast.BroadcastSession;
import com.caszgamermd.caszualtvtime.network.MediaKind;
import java.util.Arrays;
import com.caszgamermd.caszualtvtime.network.CaszualTvTimeNetworking;
import com.caszgamermd.caszualtvtime.network.payload.ChannelSessionPayload;
import com.caszgamermd.caszualtvtime.network.payload.MediaRelayPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-owned camera broadcasts. A small, finite set of enabled cameras
 * gets non-persistent simulation/loading tickets while the server is running.
 */
public final class CameraManager {
    public static final int MAX_ACTIVE_PER_DIMENSION = 4;
    public static final int FPS = CameraFeedRenderer.FPS_LIMIT;
    /**
     * A camera must not silently turn an entire area into a simulated farm.
     * Chunk *loading* and block/entity *simulation* are distinct in 26.2.
     * Operators may opt into simulation explicitly with the JVM property.
     */
    private static final boolean SIMULATE_CHUNKS =
        Boolean.getBoolean("caszual_tv_time.camera.simulateChunks");
    private static final int CHUNK_RADIUS = Math.max(0, Math.min(2,
        Integer.getInteger("caszual_tv_time.camera.chunkRadius", 1)));
    private static final TicketType CAMERA_TICKET =
        new TicketType(TicketType.NO_TIMEOUT,
            SIMULATE_CHUNKS
                ? TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION
                : TicketType.FLAG_LOADING);
    private static final Map<ServerLevel, Map<BlockPos, CameraRuntime>> CAMERAS = new IdentityHashMap<>();
    private static final Map<ServerLevel, Map<Long, Integer>> TICKET_REFS = new IdentityHashMap<>();

    private CameraManager() {}

    public static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof CameraBlockEntity camera) add(world, camera);
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof CameraBlockEntity camera) remove(world, camera.getBlockPos());
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) tick(level);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Reload enabled cameras after restart, even when no player is near them.
            // Limit synchronous startup work to the same bound as active camera tickets.
            for (ServerLevel level : server.getAllLevels()) {
                int count = 0;
                for (var entry : CameraSavedData.get(level).entries()) {
                    if (!entry.active() || count >= MAX_ACTIVE_PER_DIMENSION) continue;
                    count++;
                    level.getChunkAt(entry.pos());
                    if (level.getBlockEntity(entry.pos()) instanceof CameraBlockEntity camera) {
                        add(level, camera);
                    } else {
                        CameraSavedData.get(level).remove(entry.pos());
                    }
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (var level : new ArrayList<>(CAMERAS.keySet())) {
                for (var pos : new ArrayList<>(CAMERAS.get(level).keySet())) remove(level, pos);
            }
            CAMERAS.clear();
            TICKET_REFS.clear();
        });
    }

    public static List<CameraBlockEntity> cameras(ServerLevel world) {
        Map<BlockPos, CameraRuntime> indexed = CAMERAS.get(world);
        if (indexed == null) return List.of();
        return indexed.values().stream().map(runtime -> runtime.camera).toList();
    }

    public static CameraBlockEntity lookup(ServerLevel world, BlockPos pos) {
        Map<BlockPos, CameraRuntime> indexed = CAMERAS.get(world);
        CameraRuntime runtime = indexed == null ? null : indexed.get(pos);
        return runtime == null ? null : runtime.camera;
    }

    public static void add(ServerLevel world, CameraBlockEntity camera) {
        Map<BlockPos, CameraRuntime> index = CAMERAS.computeIfAbsent(world, ignored -> new LinkedHashMap<>());
        BlockPos pos = camera.getBlockPos().immutable();
        CameraRuntime prior = index.get(pos);
        if (prior != null && prior.camera == camera) return;
        if (prior != null) release(world, prior);
        index.put(pos, new CameraRuntime(camera));
    }

    public static void remove(ServerLevel world, BlockPos pos) {
        Map<BlockPos, CameraRuntime> index = CAMERAS.get(world);
        if (index == null) return;
        CameraRuntime runtime = index.remove(pos);
        if (runtime != null) release(world, runtime);
    }

    private static void tick(ServerLevel world) {
        Map<BlockPos, CameraRuntime> index = CAMERAS.get(world);
        if (index == null || index.isEmpty()) return;
        long tick = world.getGameTime();
        int active = 0;
        for (CameraRuntime runtime : new ArrayList<>(index.values())) {
            CameraBlockEntity camera = runtime.camera;
            if (camera.isRemoved() || !world.getBlockState(camera.getBlockPos()).is(ModBlocks.CAMERA)) {
                remove(world, camera.getBlockPos());
                continue;
            }
            CameraSavedData.get(world).put(camera);
            boolean allowed = camera.active() && active < MAX_ACTIVE_PER_DIMENSION;
            if (!allowed) { release(world, runtime); continue; }
            active++;
            if (!runtime.ticketed) {
                acquireTicket(world, camera.getBlockPos());
                runtime.ticketed = true;
            }
            String channel = camera.channel();
            if (channel.isBlank()) {
                stopSession(world, runtime);
                continue;
            }
            if (runtime.session != null && !runtime.session.channel().equals(channel)) {
                stopSession(world, runtime);
            }
            if (runtime.session == null) {
                try {
                    UUID owner = UUID.nameUUIDFromBytes(
                        ("caszual_tv_time:camera:" + world.dimension().identifier() + ":" + camera.getBlockPos().asLong())
                            .getBytes(StandardCharsets.UTF_8));
                    BroadcastSession session = CaszualTvTime.broadcasts().create(owner, channel);
                    session.markLive(CameraFeedRenderer.WIDTH, CameraFeedRenderer.HEIGHT, FPS, 256);
                    runtime.session = session;
                    CaszualTvTimeNetworking.subscriptions().attachSession(session.id(), channel);
                    notifyViewers(world, session, true);
                } catch (IllegalStateException channelInUse) {
                    // Camera waits for conflicting broadcaster/channel to free up.
                    continue;
                }
            }
            // The render budget is applied after all cameras are examined.
            // No subscribers = no ray tracing, irrespective of chunk-loader state.
            if (runtime.session == null
                || CaszualTvTimeNetworking.subscriptions().viewers(runtime.session.id()).isEmpty()) {
                runtime.frame = null;
                continue;
            }
            if (runtime.frame != null && !runtime.frame.matches(camera)) {
                runtime.frame = null; // Pan/tilt/zoom changed during an in-flight frame.
            }
            if (runtime.frame == null
                && tick - runtime.lastPublishedTick >= CameraFeedRenderer.FRAME_INTERVAL_TICKS) {
                runtime.frame = new CameraFeedRenderer.Frame(camera);
            }
        }

        // A *global* per-dimension pixel/raycast budget, shared across cameras.
        // More simultaneous feeds reduce frame rate, rather than freezing the server.
        List<CameraRuntime> pending = index.values().stream()
            .filter(runtime -> runtime.frame != null && runtime.session != null)
            .toList();
        int remaining = CameraFeedRenderer.TOTAL_RAYS_PER_TICK;
        int cursor = 0;
        for (CameraRuntime runtime : pending) {
            int budget = Math.max(1, remaining / (pending.size() - cursor++));
            remaining -= budget;
            if (!runtime.frame.renderNext(world, budget)) continue;
            byte[] frame = runtime.frame.data();
            runtime.frame = null;
            runtime.lastPublishedTick = tick;
            // Do not resend unchanged still images every second, but refresh
            // independent keyframes for players who tune into the channel later.
            boolean refresh = tick - runtime.lastSentTick >= 40;
            if (!refresh && Arrays.equals(frame, runtime.lastFrame)) continue;
            runtime.lastFrame = frame;
            runtime.lastSentTick = tick;
            byte[] encoded = CameraFrameEncoder.encode(
                CameraFeedRenderer.WIDTH, CameraFeedRenderer.HEIGHT, frame);
            var packet = new MediaRelayPayload(
                runtime.session.id(), MediaKind.VIDEO, runtime.sequence++,
                tick * 50_000L, true, encoded
            );
            for (UUID viewerId : CaszualTvTimeNetworking.subscriptions().viewers(runtime.session.id())) {
                ServerPlayer viewer = world.getServer().getPlayerList().getPlayer(viewerId);
                if (viewer != null) ServerPlayNetworking.send(viewer, packet);
            }
        }
    }

    private static void notifyViewers(ServerLevel world, BroadcastSession session, boolean active) {
        var subs = CaszualTvTimeNetworking.subscriptions();
        for (UUID viewerId : subs.viewersForChannel(session.channel())) {
            ServerPlayer viewer = world.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) ServerPlayNetworking.send(viewer,
                new ChannelSessionPayload(session.channel(), session.id(), active));
        }
    }

    private static void stopSession(ServerLevel world, CameraRuntime runtime) {
        BroadcastSession session = runtime.session;
        if (session == null) return;
        notifyViewers(world, session, false);
        CaszualTvTimeNetworking.subscriptions().removeSession(session.id());
        CaszualTvTime.broadcasts().remove(session.id());
        runtime.session = null;
        runtime.frame = null;
        runtime.sequence = 0;
        runtime.lastFrame = null;
        runtime.lastSentTick = Long.MIN_VALUE / 2;
    }

    private static void release(ServerLevel world, CameraRuntime runtime) {
        stopSession(world, runtime);
        if (runtime.ticketed) {
            releaseTicket(world, runtime.camera.getBlockPos());
            runtime.ticketed = false;
        }
    }

    private static void acquireTicket(ServerLevel world, BlockPos pos) {
        long key = ChunkPos.containing(pos).pack();
        Map<Long, Integer> counts = TICKET_REFS.computeIfAbsent(world, ignored -> new HashMap<>());
        int before = counts.getOrDefault(key, 0);
        counts.put(key, before + 1);
        if (before == 0) world.getChunkSource().addTicketWithRadius(CAMERA_TICKET, ChunkPos.containing(pos), CHUNK_RADIUS);
    }

    private static void releaseTicket(ServerLevel world, BlockPos pos) {
        Map<Long, Integer> counts = TICKET_REFS.get(world);
        if (counts == null) return;
        long key = ChunkPos.containing(pos).pack();
        int before = counts.getOrDefault(key, 0);
        if (before <= 1) {
            counts.remove(key);
            if (before == 1) world.getChunkSource().removeTicketWithRadius(CAMERA_TICKET, ChunkPos.containing(pos), CHUNK_RADIUS);
        } else counts.put(key, before - 1);
    }

    private static final class CameraRuntime {
        private final CameraBlockEntity camera;
        private boolean ticketed;
        private BroadcastSession session;
        private long sequence;
        private long lastPublishedTick = Long.MIN_VALUE / 2;
        private CameraFeedRenderer.Frame frame;
        private byte[] lastFrame;
        private long lastSentTick = Long.MIN_VALUE / 2;
        private CameraRuntime(CameraBlockEntity camera) { this.camera = camera; }
    }
}
