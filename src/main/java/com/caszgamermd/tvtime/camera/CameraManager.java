package com.caszgamermd.tvtime.camera;

import com.caszgamermd.tvtime.TVtime;
import com.caszgamermd.tvtime.block.CameraBlockEntity;
import com.caszgamermd.tvtime.block.ModBlocks;
import com.caszgamermd.tvtime.broadcast.BroadcastSession;
import com.caszgamermd.tvtime.network.MediaKind;
import com.caszgamermd.tvtime.network.TVtimeNetworking;
import com.caszgamermd.tvtime.network.payload.ChannelSessionPayload;
import com.caszgamermd.tvtime.network.payload.MediaRelayPayload;
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
    public static final int FPS = 1;
    private static final TicketType CAMERA_TICKET =
        new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);
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
                        ("tvtime:camera:" + world.dimension().identifier() + ":" + camera.getBlockPos().asLong())
                            .getBytes(StandardCharsets.UTF_8));
                    BroadcastSession session = TVtime.broadcasts().create(owner, channel);
                    session.markLive(CameraFeedRenderer.WIDTH, CameraFeedRenderer.HEIGHT, FPS, 256);
                    runtime.session = session;
                    TVtimeNetworking.subscriptions().attachSession(session.id(), channel);
                    notifyViewers(world, session, true);
                } catch (IllegalStateException channelInUse) {
                    // Camera waits for conflicting broadcaster/channel to free up.
                    continue;
                }
            }
            if (tick % 20 != Math.floorMod(camera.getBlockPos().asLong(), 20L) || runtime.session == null) continue;
            var subscribers = TVtimeNetworking.subscriptions().viewers(runtime.session.id());
            if (subscribers.isEmpty()) continue;
            byte[] frame = CameraFeedRenderer.render(world, camera);
            var packet = new MediaRelayPayload(
                runtime.session.id(), MediaKind.VIDEO, runtime.sequence++,
                tick * 50_000L, true, frame
            );
            for (UUID viewerId : subscribers) {
                ServerPlayer viewer = world.getServer().getPlayerList().getPlayer(viewerId);
                if (viewer != null) ServerPlayNetworking.send(viewer, packet);
            }
        }
    }

    private static void notifyViewers(ServerLevel world, BroadcastSession session, boolean active) {
        var subs = TVtimeNetworking.subscriptions();
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
        TVtimeNetworking.subscriptions().removeSession(session.id());
        TVtime.broadcasts().remove(session.id());
        runtime.session = null;
        runtime.sequence = 0;
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
        if (before == 0) world.getChunkSource().addTicketWithRadius(CAMERA_TICKET, ChunkPos.containing(pos), 2);
    }

    private static void releaseTicket(ServerLevel world, BlockPos pos) {
        Map<Long, Integer> counts = TICKET_REFS.get(world);
        if (counts == null) return;
        long key = ChunkPos.containing(pos).pack();
        int before = counts.getOrDefault(key, 0);
        if (before <= 1) {
            counts.remove(key);
            if (before == 1) world.getChunkSource().removeTicketWithRadius(CAMERA_TICKET, ChunkPos.containing(pos), 2);
        } else counts.put(key, before - 1);
    }

    private static final class CameraRuntime {
        private final CameraBlockEntity camera;
        private boolean ticketed;
        private BroadcastSession session;
        private long sequence;
        private CameraRuntime(CameraBlockEntity camera) { this.camera = camera; }
    }
}
