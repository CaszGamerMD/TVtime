package com.caszgamermd.tvtime.network;

import com.caszgamermd.tvtime.TVtime;
import com.caszgamermd.tvtime.broadcast.BroadcastSession;
import com.caszgamermd.tvtime.config.StreamLimits;
import com.caszgamermd.tvtime.display.TvDisplaySettings;
import com.caszgamermd.tvtime.block.ModBlocks;
import com.caszgamermd.tvtime.network.payload.MediaRelayPayload;
import com.caszgamermd.tvtime.network.payload.ConfigureTvPayload;
import com.caszgamermd.tvtime.network.payload.ChannelSessionPayload;
import com.caszgamermd.tvtime.network.payload.StartBroadcastAckPayload;
import com.caszgamermd.tvtime.network.payload.StartBroadcastPayload;
import com.caszgamermd.tvtime.network.payload.StopBroadcastPayload;
import com.caszgamermd.tvtime.network.payload.SubscribeChannelPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class TVtimeNetworking {
    private static final StreamLimits LIMITS = StreamLimits.defaults();
    private static final BroadcastSubscriptions SUBSCRIPTIONS = new BroadcastSubscriptions();
    private static final MediaRateLimiter RATE_LIMITER = new MediaRateLimiter();

    private TVtimeNetworking() {
    }

    public static BroadcastSubscriptions subscriptions() {
        return SUBSCRIPTIONS;
    }

    public static void initialize() {
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
            ConfigureTvPayload.TYPE,
            ConfigureTvPayload.CODEC
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
            ChannelSessionPayload.TYPE,
            ChannelSessionPayload.CODEC
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
            ConfigureTvPayload.TYPE,
            (payload, context) -> handleConfigureTv(context.player(), payload)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.getPlayer().getUUID();

            TVtime.broadcasts().all().stream()
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
                    TVtime.broadcasts().remove(session.id());
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
            TVtime.LOGGER.warn(
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
        TVtime.broadcasts().removeForBroadcaster(player.getUUID());

        final BroadcastSession session;
        try {
            session = TVtime.broadcasts().create(player.getUUID(), channel);
        } catch (IllegalStateException channelInUse) {
            TVtime.LOGGER.warn(
                "{} tried to start occupied TVtime channel '{}'",
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

        TVtime.LOGGER.info(
            "{} started TVtime channel '{}' ({})",
            player.getGameProfile().name(),
            session.channel(),
            session.id()
        );
    }

    private static void handleStop(ServerPlayer player, StopBroadcastPayload payload) {
        TVtime.broadcasts().byId(payload.sessionId()).ifPresent(session -> {
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
            TVtime.broadcasts().remove(session.id());
        });
    }

    private static void handleSubscription(ServerPlayer player, SubscribeChannelPayload payload) {
        String channel = sanitizeChannel(payload.channel());
        if (channel.isEmpty()) {
            return;
        }

        if (payload.subscribed()) {
            SUBSCRIPTIONS.subscribeChannel(channel, player.getUUID());

            TVtime.broadcasts().byChannel(channel).ifPresent(session -> {
                SUBSCRIPTIONS.subscribe(session.id(), player.getUUID());
                ServerPlayNetworking.send(
                    player,
                    new ChannelSessionPayload(session.channel(), session.id(), true)
                );
            });
            return;
        }

        SUBSCRIPTIONS.unsubscribeChannel(channel, player.getUUID());

        TVtime.broadcasts().byChannel(channel).ifPresent(session -> {
            SUBSCRIPTIONS.unsubscribe(session.id(), player.getUUID());
            ServerPlayNetworking.send(
                player,
                new ChannelSessionPayload(session.channel(), session.id(), false)
            );
        });
    }

    private static void handleConfigureTv(ServerPlayer player, ConfigureTvPayload payload) {
        long dx = (long) player.blockPosition().getX() - payload.pos().getX();
        long dy = (long) player.blockPosition().getY() - payload.pos().getY();
        long dz = (long) player.blockPosition().getZ() - payload.pos().getZ();

        if (dx * dx + dy * dy + dz * dz > 64L) {
            return;
        }

        if (!player.level().getBlockState(payload.pos()).is(ModBlocks.TV)) {
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

    private static void handleMedia(ServerPlayer sender, MediaRelayPayload payload) {
        if (payload.payload().length > MediaRelayPayload.MAX_MEDIA_BYTES) {
            return;
        }

        TVtime.broadcasts().byId(payload.sessionId()).ifPresent(session -> {
            if (!session.broadcaster().equals(sender.getUUID())) {
                TVtime.LOGGER.warn(
                    "Rejected spoofed TVtime media packet from {} for session {}",
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
