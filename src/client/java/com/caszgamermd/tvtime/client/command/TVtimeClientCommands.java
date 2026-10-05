package com.caszgamermd.tvtime.client.command;

import com.caszgamermd.tvtime.broadcast.DisplayMode;
import com.caszgamermd.tvtime.client.capture.CaptureBroadcastController;
import com.caszgamermd.tvtime.client.capture.CaptureWindow;
import com.caszgamermd.tvtime.client.network.TestNetworkBroadcaster;
import com.caszgamermd.tvtime.network.payload.ConfigureTvPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

                                context.getSource().sendFeedback(Component.literal(
                                    controller.running()
                                        ? "TVtime is capturing channel '" + controller.channel() + "'."
                                        : "TVtime window capture is idle."
                                ));
                                return controller.running() ? 1 : 0;
                            })
                    )
                    .then(tuneCommand())
            );
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<
        net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
    > tuneCommand() {
        return ClientCommands.literal("tune")
            .then(
                ClientCommands.argument("channel", StringArgumentType.word())
                    .executes(context -> {
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
                                DisplayMode.FIT,
                                true
                            )
                        );

                        context.getSource().sendFeedback(
                            Component.literal(
                                "Tuning connected TV display to '" + channel + "'."
                            )
                        );
                        return 1;
                    })
            );
    }
}
