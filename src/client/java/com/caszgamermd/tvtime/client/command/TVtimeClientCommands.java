package com.caszgamermd.tvtime.client.command;

import com.caszgamermd.tvtime.client.network.TestNetworkBroadcaster;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

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
            );
        });
    }
}
