package com.caszgamermd.tvtime.client.command;

import com.caszgamermd.tvtime.broadcast.DisplayMode;
import com.caszgamermd.tvtime.audio.SpeakerChannel;
import com.caszgamermd.tvtime.client.capture.CaptureBroadcastController;
import com.caszgamermd.tvtime.client.capture.CaptureWindow;
import com.caszgamermd.tvtime.client.network.TestNetworkBroadcaster;
import com.caszgamermd.tvtime.network.payload.ConfigureTvPayload;
import com.caszgamermd.tvtime.network.payload.ConfigureSpeakerPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public final class TVtimeClientCommands {
    private TVtimeClientCommands() {
    }

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommands.literal("tvtime_test")
                    .then(
                        ClientCommands.literal("start")
                            .then(
                                ClientCommands.argument("channel", StringArgumentType.word())
                                    .executes(context -> {
                                        String channel = StringArgumentType.getString(context, "channel");
                                        boolean started = TestNetworkBroadcaster.start(channel);
                                        context.getSource().sendFeedback(Component.literal(
                                            started
                                                ? "TVtime test broadcast requested on channel '" + channel + "'."
                                                : "TVtime test broadcast could not start."
                                        ));
                                        return started ? 1 : 0;
                                    })
                            )
                    )
                    .then(
                        ClientCommands.literal("stop")
                            .executes(context -> {
                                TestNetworkBroadcaster.stop();
                                context.getSource().sendFeedback(
                                    Component.literal("TVtime test broadcast stopped.")
                                );
                                return 1;
                            })
                    )
                    .then(tuneCommand())
                    .then(speakerCommand())
            );

            dispatcher.register(
                ClientCommands.literal("tvtime_capture")
                    .then(
                        ClientCommands.literal("windows")
                            .executes(context -> {
                                try {
                                    List<CaptureWindow> windows =
                                        CaptureBroadcastController.instance().windows();

                                    context.getSource().sendFeedback(Component.literal(
                                        "TVtime capturable windows: " + windows.size()
                                    ));

                                    int shown = Math.min(20, windows.size());
                                    for (int i = 0; i < shown; i++) {
                                        CaptureWindow window = windows.get(i);
                                        context.getSource().sendFeedback(Component.literal(
                                            "[" + i + "] " + window.displayName()
                                                + " (PID " + window.processId() + ")"
                                        ));
                                    }

                                    if (windows.size() > shown) {
                                        context.getSource().sendFeedback(Component.literal(
                                            "...and " + (windows.size() - shown) + " more."
                                        ));
                                    }

                                    return windows.size();
                                } catch (RuntimeException ex) {
                                    context.getSource().sendFeedback(Component.literal(
                                        "TVtime capture unavailable: " + ex.getMessage()
                                    ));
                                    return 0;
                                }
                            })
                    )
                    .then(
                        ClientCommands.literal("start")
                            .then(
                                ClientCommands.argument(
                                    "window",
                                    IntegerArgumentType.integer(0)
                                ).then(
                                    ClientCommands.argument(
                                        "channel",
                                        StringArgumentType.word()
                                    ).executes(context -> {
                                        int window = IntegerArgumentType.getInteger(
                                            context,
                                            "window"
                                        );
                                        String channel = StringArgumentType.getString(
                                            context,
                                            "channel"
                                        );

                                        try {
                                            CaptureBroadcastController.instance().start(
                                                window,
                                                channel
                                            );
                                            context.getSource().sendFeedback(Component.literal(
                                                "TVtime requested real window broadcast on channel '"
                                                    + channel + "'."
                                            ));
                                            return 1;
                                        } catch (RuntimeException ex) {
                                            context.getSource().sendFeedback(Component.literal(
                                                "TVtime capture failed: " + ex.getMessage()
                                            ));
                                            return 0;
                                        }
                                    })
                                )
                            )
                    )
                    .then(
                        ClientCommands.literal("stop")
                            .executes(context -> {
                                CaptureBroadcastController.instance().stop();
                                context.getSource().sendFeedback(Component.literal(
                                    "TVtime window capture stopped."
                                ));
                                return 1;
                            })
                    )
                    .then(
                        ClientCommands.literal("status")
                            .executes(context -> {
                                CaptureBroadcastController controller =
                                    CaptureBroadcastController.instance();

                                if (!controller.running()) {
                                    context.getSource().sendFeedback(
                                        Component.literal("TVtime window capture is idle.")
                                    );
                                    return 0;
                                }

                                var stats = controller.stats();
                                context.getSource().sendFeedback(Component.literal(
                                    "TVtime channel '" + controller.channel() + "'"
                                        + " | " + String.format("%.1f", stats.fps()) + " FPS"
                                        + " | " + String.format("%.0f", stats.kbps()) + " kbps"
                                        + " | video "
                                        + String.format("%.1f%%", stats.compressionRatio() * 100.0)
                                        + " of raw size"
                                ));
                                return 1;
                            })
                    )
                    .then(tuneCommand())
                    .then(speakerCommand())
            );
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<
        FabricClientCommandSource
    > speakerCommand() {
        return ClientCommands.literal("speaker")
            .then(
                ClientCommands.argument("channel", StringArgumentType.word())
                    .then(
                        ClientCommands.argument("role", StringArgumentType.word())
                            .executes(context ->
                                configureSpeaker(context, 1.0f, 32)
                            )
                            .then(
                                ClientCommands.argument(
                                    "volume",
                                    FloatArgumentType.floatArg(0.0f, 2.0f)
                                ).executes(context ->
                                    configureSpeaker(
                                        context,
                                        FloatArgumentType.getFloat(context, "volume"),
                                        32
                                    )
                                ).then(
                                    ClientCommands.argument(
                                        "range",
                                        IntegerArgumentType.integer(1, 128)
                                    ).executes(context ->
                                        configureSpeaker(
                                            context,
                                            FloatArgumentType.getFloat(context, "volume"),
                                            IntegerArgumentType.getInteger(context, "range")
                                        )
                                    )
                                )
                            )
                    )
            );
    }

    private static int configureSpeaker(
        CommandContext<FabricClientCommandSource> context,
        float volume,
        int range
    ) {
        String channel = StringArgumentType.getString(context, "channel");
        String rawRole = StringArgumentType.getString(context, "role");

        final SpeakerChannel role;
        try {
            role = SpeakerChannel.valueOf(
                rawRole.trim().toUpperCase().replace('-', '_')
            );
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFeedback(Component.literal(
                "Unknown speaker role. Use FULL, LEFT, RIGHT, CENTER, "
                    + "REAR_LEFT, REAR_RIGHT, or LFE."
            ));
            return 0;
        }

        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            context.getSource().sendFeedback(
                Component.literal("Look directly at a TVtime speaker block first.")
            );
            return 0;
        }

        if (!ClientPlayNetworking.canSend(ConfigureSpeakerPayload.TYPE)) {
            context.getSource().sendFeedback(
                Component.literal("Server does not accept TVtime speaker settings.")
            );
            return 0;
        }

        ClientPlayNetworking.send(new ConfigureSpeakerPayload(
            blockHit.getBlockPos(),
            channel,
            role,
            volume,
            range
        ));

        context.getSource().sendFeedback(Component.literal(
            "Speaker tuned to '" + channel + "' as " + role
                + " (volume " + volume + ", range " + range + ")."
        ));
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<
        net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
    > tuneCommand() {
        return ClientCommands.literal("tune")
            .then(
                ClientCommands.argument("channel", StringArgumentType.word())
                    .executes(context ->
                        configureTv(context, DisplayMode.FIT, true)
                    )
                    .then(
                        ClientCommands.argument("mode", StringArgumentType.word())
                            .executes(context -> {
                                DisplayMode mode = parseDisplayMode(context);
                                return mode == null
                                    ? 0
                                    : configureTv(context, mode, true);
                            })
                            .then(
                                ClientCommands.argument(
                                    "audio",
                                    BoolArgumentType.bool()
                                ).executes(context -> {
                                    DisplayMode mode = parseDisplayMode(context);
                                    if (mode == null) {
                                        return 0;
                                    }

                                    return configureTv(
                                        context,
                                        mode,
                                        BoolArgumentType.getBool(context, "audio")
                                    );
                                })
                            )
                    )
            );
    }

    private static DisplayMode parseDisplayMode(
        CommandContext<FabricClientCommandSource> context
    ) {
        String raw = StringArgumentType.getString(context, "mode");

        try {
            return DisplayMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFeedback(Component.literal(
                "Unknown display mode. Use FIT, FILL, or STRETCH."
            ));
            return null;
        }
    }

    private static int configureTv(
        CommandContext<FabricClientCommandSource> context,
        DisplayMode mode,
        boolean audioEnabled
    ) {
        String channel = StringArgumentType.getString(context, "channel");
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;

        if (!(hit instanceof BlockHitResult blockHit)) {
            context.getSource().sendFeedback(
                Component.literal("Look directly at a TV block first.")
            );
            return 0;
        }

        if (!ClientPlayNetworking.canSend(ConfigureTvPayload.TYPE)) {
            context.getSource().sendFeedback(
                Component.literal(
                    "Server does not accept TVtime TV configuration packets."
                )
            );
            return 0;
        }

        ClientPlayNetworking.send(
            new ConfigureTvPayload(
                blockHit.getBlockPos(),
                channel,
                mode,
                audioEnabled
            )
        );

        context.getSource().sendFeedback(Component.literal(
            "Tuning connected TV display to '" + channel
                + "' (" + mode
                + ", TV audio " + (audioEnabled ? "on" : "off") + ")."
        ));
        return 1;
    }
}
