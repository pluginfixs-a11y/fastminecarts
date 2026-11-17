package com.fastminecarts.util;

/**
 * Implemented (via mixin) by LivingEntity to store a lightweight, non-persistent
 * "last hit by a fast minecart" game-time tick, used to prevent damaging the same
 * entity every single tick while it's being pushed by a minecart.
 * <p>
 * This is intentionally NOT saved to disk and NOT stored in any external map -
 * it lives as a single primitive field directly on the entity instance, so it is
 * automatically garbage collected with the entity and never grows unbounded.
 */
public interface FastMinecartsCooldownAccessor {

    long fastminecarts$getLastRunoverTick();

    void fastminecarts$setLastRunoverTick(long tick);
}
