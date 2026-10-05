package com.caszgamermd.tvtime;

import com.caszgamermd.tvtime.broadcast.BroadcastManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TVtime implements ModInitializer {
    public static final String MOD_ID = "tvtime";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final BroadcastManager BROADCAST_MANAGER = new BroadcastManager();

    public static BroadcastManager broadcasts() {
        return BROADCAST_MANAGER;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TVtime");
    }
}
