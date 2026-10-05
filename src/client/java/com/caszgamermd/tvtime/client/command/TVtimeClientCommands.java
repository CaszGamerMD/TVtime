package com.caszgamermd.tvtime.client.command;

import com.caszgamermd.tvtime.client.network.TestNetworkBroadcaster;
import com.caszgamermd.tvtime.network.payload.ConfigureTvPayload;
import com.caszgamermd.tvtime.broadcast.DisplayMode;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
                    .then(
                        ClientCommands.literal("tune")
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
                                                Component.literal("Server does not accept TVtime TV configuration packets.")
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
                                            Component.literal("Tuning connected TV display to '" + channel + "'.")
                                        );
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }
}
