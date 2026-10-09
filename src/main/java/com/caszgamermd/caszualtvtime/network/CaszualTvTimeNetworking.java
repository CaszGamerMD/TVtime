package com.caszgamermd.caszualcaszual_tv_time.network;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import com.caszgamermd.caszualcaszual_tv_time.camera.CameraManager;
import com.caszgamermd.caszualcaszual_tv_time.camera.CameraSavedData;
import com.caszgamermd.caszualcaszual_tv_time.block.CameraBlockEntity;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.CameraCatalogPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.ConfigureCameraPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.RequestCameraCatalogPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.Comparator;
import com.caszgamermd.caszualcaszual_tv_time.broadcast.BroadcastSession;
import com.caszgamermd.caszualcaszual_tv_time.config.StreamLimits;
import com.caszgamermd.caszualcaszual_tv_time.display.TvDisplaySettings;
import com.caszgamermd.caszualcaszual_tv_time.block.ModBlocks;
import com.caszgamermd.caszualcaszual_tv_time.block.SpeakerBlockEntity;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.MediaRelayPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.MediaFragmentPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.ConfigureTvPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.ConfigureSpeakerPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.ConfigurePortableTvPipPayload;
import com.caszgamermd.caszualcaszual_tv_time.item.PortableTvSettings;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.ChannelSessionPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.StartBroadcastAckPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.StartBroadcastPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.StopBroadcastPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.SubscribeChannelPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CaszualTvTimeNetworking {
    private static final StreamLimits LIMITS = StreamLimits.defaults();
    private static final BroadcastSubscriptions SUBSCRIPTIONS = new BroadcastSubscriptions();
    private static final MediaRateLimiter RATE_LIMITER = new MediaRateLimiter();

    private CaszualTvTimeNetworking() {
    }

    public static BroadcastSubscriptions subscriptions() {
        return SUBSCRIPTIONS;
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
            RequestCameraCatalogPayload.TYPE, RequestCameraCatalogPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
            ConfigureCameraPayload.TYPE, ConfigureCameraPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
            CameraCatalogPayload.TYPE, CameraCatalogPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
            StartBroadcastPayload.TYPE,
            StartBroadcastPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            StopBroadcastPayload.TYPE,
            StopBroadcastPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            SubscribeChannelPayload.TYPE,
            SubscribeChannelPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            MediaRelayPayload.TYPE,
            MediaRelayPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            MediaFragmentPayload.TYPE,
            MediaFragmentPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            ConfigureTvPayload.TYPE,
            ConfigureTvPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            ConfigureSpeakerPayload.TYPE,
            ConfigureSpeakerPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
            ConfigurePortableTvPipPayload.TYPE,
            ConfigurePortableTvPipPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
            StartBroadcastAckPayload.TYPE,
            StartBroadcastAckPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            MediaRelayPayload.TYPE,
            MediaRelayPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            MediaFragmentPayload.TYPE,
            MediaFragmentPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            ChannelSessionPayload.TYPE,
            ChannelSessionPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
            RequestCameraCatalogPayload.TYPE,
            (payload, context) -> sendCameraCatalog(context.player(), payload.consolePos())
        );
        ServerPlayNetworking.registerGlobalReceiver(
            ConfigureCameraPayload.TYPE,
            (payload, context) -> handleConfigureCamera(context.player(), payload)
        );
        ServerPlayNetworking.registerGlobalReceiver(
            StartBroadcastPayload.TYPE,
            (payload, context) -> handleStart(context.player(), payload)
        );

        ServerPlayNetworking.registerGlobalReceiver(
            StopBroadcastPayload.TYPE,
            (payload, context) -> handleStop(context.player(), payload)
        );

        ServerPlayNetworking.registerGlobalReceiver(
            SubscribeChannelPayload.TYPE,
            (payload, context) -> handleSubscription(context.player(), payload)
        );

        ServerPlayNetworking.registerGlobalReceiver(
            MediaRelayPayload.TYPE,
            (payload, context) -> handleMedia(context.player(), payload)
        );

        ServerPlayNetworking.registerGlobalReceiver(
            MediaFragmentPayload.TYPE,
            (payload, context) -> handleMediaFragment(context.player(), payload)
        );

        ServerPlayNetworking.registerGlobalReceiver(
            ConfigureTvPayload.TYPE,
            (payload, context) -> handleConfigureTv(context.player(), payload)
        );

        ServerPlayNetworking.registerGlobalReceiver(
            ConfigureSpeakerPayload.TYPE,
            (payload, context) -> handleConfigureSpeaker(context.player(), payload)
        );

        ServerPlayNetworking.registerGlobalReceiver(
            ConfigurePortableTvPipPayload.TYPE,
            (payload, context) ->
                handleConfigurePortableTvPip(context.player(), payload)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.getPlayer().getUUID();

            CaszualTvTime.broadcasts().all().stream()
                .filter(session -> session.broadcaster().equals(playerId))
                .toList()
                .forEach(session -> {
                    for (UUID viewerId : SUBSCRIPTIONS.viewers(session.id())) {
                        ServerPlayer viewer = server.getPlayerList().getPlayer(viewerId);
                        if (viewer != null) {
                            ServerPlayNetworking.send(
                                viewer,
                                new ChannelSessionPayload(
                                    session.channel(),
                                    session.id(),
                                    false
                                )
                            );
                        }
                    }

                    SUBSCRIPTIONS.removeSession(session.id());
                    RATE_LIMITER.remove(session.id());
                    CaszualTvTime.broadcasts().remove(session.id());
                });

            SUBSCRIPTIONS.removePlayer(playerId);
        });
    }

    private static void handleStart(ServerPlayer player, StartBroadcastPayload payload) {
        String channel = sanitizeChannel(payload.channel());
        if (channel.isEmpty()) {
            return;
        }

        if (!LIMITS.allows(
            payload.width(),
            payload.height(),
            payload.fps(),
            payload.videoBitrateKbps()
        )) {
            CaszualTvTime.LOGGER.warn(
                "Rejected stream from {} outside server limits: {}x{} @ {}fps / {}kbps",
                player.getGameProfile().name(),
                payload.width(),
                payload.height(),
                payload.fps(),
                payload.videoBitrateKbps()
            );
            return;
        }

        // One active outbound broadcast per player for now.
        CaszualTvTime.broadcasts().removeForBroadcaster(player.getUUID());

        final BroadcastSession session;
        try {
            session = CaszualTvTime.broadcasts().create(player.getUUID(), channel);
        } catch (IllegalStateException channelInUse) {
            CaszualTvTime.LOGGER.warn(
                "{} tried to start occupied Caszual TV Time channel '{}'",
                player.getGameProfile().name(),
                channel
            );
            return;
        }

        session.markLive(
            payload.width(),
            payload.height(),
            payload.fps(),
            payload.videoBitrateKbps()
        );

        SUBSCRIPTIONS.attachSession(session.id(), session.channel());
        for (UUID viewerId : SUBSCRIPTIONS.viewers(session.id())) {
            ServerPlayer viewer = player.level().getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) {
                ServerPlayNetworking.send(
                    viewer,
                    new ChannelSessionPayload(session.channel(), session.id(), true)
                );
            }
        }

        ServerPlayNetworking.send(
            player,
            new StartBroadcastAckPayload(session.id(), session.channel())
        );

        CaszualTvTime.LOGGER.info(
            "{} started Caszual TV Time channel '{}' ({})",
            player.getGameProfile().name(),
            session.channel(),
            session.id()
        );
    }

    private static void handleStop(ServerPlayer player, StopBroadcastPayload payload) {
        CaszualTvTime.broadcasts().byId(payload.sessionId()).ifPresent(session -> {
            if (!session.broadcaster().equals(player.getUUID())) {
                return;
            }

            for (UUID viewerId : SUBSCRIPTIONS.viewers(session.id())) {
                ServerPlayer viewer = player.level().getServer().getPlayerList().getPlayer(viewerId);
                if (viewer != null) {
                    ServerPlayNetworking.send(
                        viewer,
                        new ChannelSessionPayload(session.channel(), session.id(), false)
                    );
                }
            }
            SUBSCRIPTIONS.removeSession(session.id());
            RATE_LIMITER.remove(session.id());
            CaszualTvTime.broadcasts().remove(session.id());
        });
    }

    private static void handleSubscription(ServerPlayer player, SubscribeChannelPayload payload) {
        String channel = sanitizeChannel(payload.channel());
        if (channel.isEmpty()) {
            return;
        }

        if (payload.subscribed()) {
            SUBSCRIPTIONS.subscribeChannel(channel, player.getUUID());

            CaszualTvTime.broadcasts().byChannel(channel).ifPresent(session -> {
                SUBSCRIPTIONS.subscribe(session.id(), player.getUUID());
                ServerPlayNetworking.send(
                    player,
                    new ChannelSessionPayload(session.channel(), session.id(), true)
                );
            });
            return;
        }

        SUBSCRIPTIONS.unsubscribeChannel(channel, player.getUUID());

        CaszualTvTime.broadcasts().byChannel(channel).ifPresent(session -> {
            SUBSCRIPTIONS.unsubscribe(session.id(), player.getUUID());
            ServerPlayNetworking.send(
                player,
                new ChannelSessionPayload(session.channel(), session.id(), false)
            );
        });
    }

    private static boolean canUseCameraConsole(ServerPlayer player, BlockPos pos) {
        long dx = (long) player.blockPosition().getX() - pos.getX();
        long dy = (long) player.blockPosition().getY() - pos.getY();
        long dz = (long) player.blockPosition().getZ() - pos.getZ();
        return dx * dx + dy * dy + dz * dz <= 64
            && player.level().getBlockState(pos).is(ModBlocks.CAMERA_CONTROL_TABLE);
    }

    private static void sendCameraCatalog(ServerPlayer player, BlockPos console) {
        if (!canUseCameraConsole(player, console)) return;
        ServerLevel world = (ServerLevel) player.level();
        var cams = CameraSavedData.get(world).entries().stream()
            .sorted(Comparator.comparing(CameraSavedData.Entry::name))
            .limit(64)
            .map(c -> new CameraCatalogPayload.CameraEntry(
                c.pos(), c.name(), c.channel(),
                c.active(), c.pan(), c.tilt(), c.zoom()))
            .toList();
        ServerPlayNetworking.send(player, new CameraCatalogPayload(console, cams));
    }

    private static void handleConfigureCamera(ServerPlayer player, ConfigureCameraPayload p) {
        if (!canUseCameraConsole(player, p.consolePos())) return;
        ServerLevel world = (ServerLevel) player.level();
        if (!CameraSavedData.get(world).contains(p.cameraPos())) return;
        // Loading one known, offline camera chunk on explicit user interaction
        // is bounded and allows remote power-on after the ticket was released.
        CameraBlockEntity camera = CameraManager.lookup(world, p.cameraPos());
        if (camera == null) {
            world.getChunkAt(p.cameraPos());
            if (world.getBlockEntity(p.cameraPos()) instanceof CameraBlockEntity found) {
                camera = found;
                CameraManager.add(world, camera);
            }
        }
        if (camera == null || !world.getBlockState(p.cameraPos()).is(ModBlocks.CAMERA)) {
            CameraSavedData.get(world).remove(p.cameraPos());
            sendCameraCatalog(player, p.consolePos());
            return;
        }
        // A control console may affect orientation, channel and power, but never position.
        camera.configure(p.name(), p.channel(), p.active(), p.pan(), p.tilt(), p.zoom());
        CameraSavedData.get(world).put(camera);
        sendCameraCatalog(player, p.consolePos());
    }

    private static void handleConfigurePortableTvPip(
        ServerPlayer player,
        ConfigurePortableTvPipPayload payload
    ) {
        var portable = player.getOffhandItem();
        if (!portable.is(ModBlocks.PORTABLE_TV.asItem())) {
            return;
        }

        PortableTvSettings.setPipCorner(
            portable,
            payload.corner()
        );
    }

    private static void handleConfigureSpeaker(
        ServerPlayer player,
        ConfigureSpeakerPayload payload
    ) {
        long dx = (long) player.blockPosition().getX() - payload.pos().getX();
        long dy = (long) player.blockPosition().getY() - payload.pos().getY();
        long dz = (long) player.blockPosition().getZ() - payload.pos().getZ();

        if (dx * dx + dy * dy + dz * dz > 64L) {
            return;
        }

        if (!ModBlocks.isSpeaker(
            player.level().getBlockState(payload.pos())
        )) {
            return;
        }

        if (!(player.level().getBlockEntity(payload.pos()) instanceof SpeakerBlockEntity speaker)) {
            return;
        }

        speaker.configure(
            sanitizeChannel(payload.channel()),
            payload.speakerChannel(),
            payload.volume(),
            payload.range()
        );
    }

    private static void handleConfigureTv(ServerPlayer player, ConfigureTvPayload payload) {
        long dx = (long) player.blockPosition().getX() - payload.pos().getX();
        long dy = (long) player.blockPosition().getY() - payload.pos().getY();
        long dz = (long) player.blockPosition().getZ() - payload.pos().getZ();

        if (dx * dx + dy * dy + dz * dz > 64L) {
            return;
        }

        if (!ModBlocks.isTv(
            player.level().getBlockState(payload.pos())
        )) {
            return;
        }

        TvDisplaySettings.apply(
            player.level(),
            payload.pos(),
            sanitizeChannel(payload.channel()),
            payload.displayMode(),
            payload.tvAudioEnabled()
        );
    }

    private static void handleMediaFragment(
        ServerPlayer sender,
        MediaFragmentPayload payload
    ) {
        try {
            MediaFragmentPayload.validate(payload);
        } catch (IllegalArgumentException invalid) {
            return;
        }

        CaszualTvTime.broadcasts().byId(payload.sessionId()).ifPresent(session -> {
            if (!session.broadcaster().equals(sender.getUUID())) {
                CaszualTvTime.LOGGER.warn(
                    "Rejected spoofed Caszual TV Time media fragment from {} for session {}",
                    sender.getGameProfile().name(),
                    payload.sessionId()
                );
                return;
            }

            if (!RATE_LIMITER.allow(
                session.id(),
                session.videoBitrateKbps(),
                payload.payload().length
            )) {
                return;
            }

            for (UUID viewerId : SUBSCRIPTIONS.viewers(session.id())) {
                ServerPlayer viewer =
                    sender.level().getServer().getPlayerList().getPlayer(viewerId);

                if (viewer != null
                    && !viewer.getUUID().equals(sender.getUUID())) {
                    ServerPlayNetworking.send(viewer, payload);
                }
            }
        });
    }

    private static void handleMedia(ServerPlayer sender, MediaRelayPayload payload) {
        if (payload.payload().length > MediaRelayPayload.MAX_MEDIA_BYTES) {
            return;
        }

        CaszualTvTime.broadcasts().byId(payload.sessionId()).ifPresent(session -> {
            if (!session.broadcaster().equals(sender.getUUID())) {
                CaszualTvTime.LOGGER.warn(
                    "Rejected spoofed Caszual TV Time media packet from {} for session {}",
                    sender.getGameProfile().name(),
                    payload.sessionId()
                );
                return;
            }

            if (!RATE_LIMITER.allow(
                session.id(),
                session.videoBitrateKbps(),
                payload.payload().length
            )) {
                return;
            }

            for (UUID viewerId : SUBSCRIPTIONS.viewers(session.id())) {
                ServerPlayer viewer = sender.level().getServer().getPlayerList().getPlayer(viewerId);
                if (viewer != null && !viewer.getUUID().equals(sender.getUUID())) {
                    ServerPlayNetworking.send(viewer, payload);
                }
            }
        });
    }

    private static String sanitizeChannel(String channel) {
        if (channel == null) {
            return "";
        }

        String trimmed = channel.trim();
        if (trimmed.length() > 64) {
            trimmed = trimmed.substring(0, 64);
        }
        return trimmed;
    }
}
