package com.fastminecarts.util;

import com.fastminecarts.config.ModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import java.util.List;

/**
 * Решает, каким вагонеткам достаётся разгон.
 */
public final class MinecartEligibility {

    private MinecartEligibility() {
    }

    /**
     * A minecart is eligible for the speed boost only if:
     * - the mod is enabled
     * - it is a plain vanilla rideable minecart (EntityTypes.MINECART), not a subtype
     * - it is ticking in a ServerLevel
     * - its first passenger is a player (when {@code requirePlayerPassenger})
     */
    public static boolean isEligible(AbstractMinecart minecart, ModConfig config) {
        if (!config.enabled) return false;
        if (!(minecart.level() instanceof ServerLevel)) return false;
        if (config.onlyVanillaMinecart && minecart.getType() != EntityTypes.MINECART) return false;

        if (config.requirePlayerPassenger) {
            List<Entity> passengers = minecart.getPassengers();
            if (passengers.isEmpty()) return false;
            return passengers.get(0) instanceof net.minecraft.world.entity.player.Player;
        }

        return true;
    }
}
