package com.fastminecarts.mixin;

import com.fastminecarts.util.FastMinecartsCooldownAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a small, transient, non-persisted field to every LivingEntity so the
 * runover-damage cooldown can be tracked per-entity without any external map
 * (bounded, cleanup-safe: the field lives and dies with the entity object).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCooldownMixin implements FastMinecartsCooldownAccessor {

    @Unique
    private long fastminecarts$lastRunoverTick = Long.MIN_VALUE;

    @Override
    public long fastminecarts$getLastRunoverTick() {
        return this.fastminecarts$lastRunoverTick;
    }

    @Override
    public void fastminecarts$setLastRunoverTick(long tick) {
        this.fastminecarts$lastRunoverTick = tick;
    }
}
