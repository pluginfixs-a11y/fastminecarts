package com.fastminecarts;

import com.fastminecarts.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fast Minecarts - server-side-only gameplay mod.
 * <p>
 * Makes only the vanilla rideable minecart faster when ridden by a player,
 * with configurable runover damage. No client install is required; vanilla
 * clients connect and play normally, seeing only the server-driven results
 * (faster movement, damage/knockback), with no custom packets, GUI, items,
 * blocks, or models involved.
 */
public final class FastMinecarts implements ModInitializer {

    public static final String MOD_ID = "fastminecarts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile ModConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        LOGGER.info("[FastMinecarts] Loaded config. enabled={}, maxSpeed={} b/s, hardMax={} b/s",
                CONFIG.enabled, CONFIG.maxSpeedBlocksPerSecond, CONFIG.hardMaxSpeedBlocksPerSecond);
    }

    /** Returns the loaded config, or null if initialization hasn't run yet. */
    public static ModConfig getConfig() {
        return CONFIG;
    }
}
