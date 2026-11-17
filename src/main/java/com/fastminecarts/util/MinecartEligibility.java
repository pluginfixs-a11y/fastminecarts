package com.fastminecarts.util;

import com.fastminecarts.config.ModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.boat.Boat;

import java.util.List;

/**
 * Shared helpers for deciding which minecarts get the speed/damage behavior,
 * and which nearby entities are valid runover damage targets.
 * <p>
 * TODO: Class names below (EntityType, LivingEntity, TamableAnimal, Enemy, Boat,
 * ArmorStand, AbstractMinecart) reflect standard Mojang-mapping conventions used
 * across recent 1.21.x releases. Verify against the actual 1.21.11 Mojang mappings
 * (see FabricMC/yarn repo, "1.21.11" branch/tag, which mirrors the official names)
 * before compiling, in case any of these were renamed.
 */
public final class MinecartEligibility {

    private MinecartEligibility() {
    }

    /**
     * A minecart is eligible for the speed boost / runover damage only if:
     * - the mod is enabled
     * - it is a plain vanilla rideable minecart (EntityTypes.MINECART), not a subtype
     * - it is ticking in a ServerLevel
     * - its first passenger is a ServerPlayer
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

    /** Convenience: returns the riding ServerPlayer, or null if there isn't one in the first seat. */
    public static ServerPlayer getRidingPlayer(AbstractMinecart minecart) {
        List<Entity> passengers = minecart.getPassengers();
        if (passengers.isEmpty()) {
            return null;
        }
        Entity first = passengers.get(0);
        return first instanceof ServerPlayer player ? player : null;
    }

    /**
     * Checks whether the given entity is a valid runover-damage target for the given
     * minecart, according to the hard exclusion list and the configurable filters.
     * This intentionally rejects as early/cheaply as possible.
     */
    public static boolean isValidDamageTarget(Entity target, AbstractMinecart minecart, ModConfig config) {
        if (target == minecart) {
            return false;
        }
        if (!target.isAlive() || target.isRemoved()) {
            return false;
        }
        // Never damage the minecart's own passengers (rider + any additional passengers).
        if (minecart.getPassengers().contains(target)) {
            return false;
        }

        // Hard exclusions regardless of config: entities that should never be treated
        // as runover victims because they are non-living utility/decoration/vehicle
        // entities or otherwise inappropriate targets.
        if (!(target instanceof LivingEntity living)) {
            return false;
        }

        if (target instanceof ServerPlayer player && player.isSpectator()) {
            return false;
        }
        if (target instanceof ServerPlayer player && player.isCreative()) {
            return false;
        }

        if (target instanceof Boat) {
            return false;
        }
        if (target instanceof AbstractMinecart) {
            return false;
        }

        // Now apply configurable target filters.
        if (target instanceof ServerPlayer) {
            return config.damagePlayers;
        }

        if (target instanceof ArmorStand) {
            // ArmorStand is technically a LivingEntity in vanilla, but it is excluded
            // by default and only included if explicitly enabled.
            return config.damageArmorStands;
        }

        if (living instanceof Enemy) {
            return config.damageHostileMobs;
        }

        if (living instanceof TamableAnimal tamable && tamable.isTame()) {
            return config.damageTamedMobs;
        }

        if (living instanceof Animal) {
            return config.damagePassiveMobs;
        }

        // Any other LivingEntity not covered above (e.g. villagers, other utility mobs)
        // is treated as "passive" for filtering purposes.
        return config.damagePassiveMobs;
    }
}

