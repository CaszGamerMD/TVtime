package com.caszgamermd.tvtime;

import com.caszgamermd.tvtime.block.ModBlockEntities;
import com.caszgamermd.tvtime.block.ModBlocks;
import com.caszgamermd.tvtime.broadcast.BroadcastManager;
import com.caszgamermd.tvtime.network.TVtimeNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TVtime implements ModInitializer {
    public static final String MOD_ID = "tvtime";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final BroadcastManager BROADCAST_MANAGER = new BroadcastManager();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static BroadcastManager broadcasts() {
        return BROADCAST_MANAGER;
    }

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        TVtimeNetworking.initialize();
        LOGGER.info("Initializing TVtime");
    }
}
