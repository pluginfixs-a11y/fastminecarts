package com.fastminecarts.mixin;

import com.fastminecarts.FastMinecarts;
import com.fastminecarts.config.ModConfig;
import com.fastminecarts.util.MinecartEligibility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * С 1.21.2 у вагонетки два режима движения: старый (OldMinecartBehavior) и
 * экспериментальный (NewMinecartBehavior, включается флагом MINECART_IMPROVEMENTS).
 * Старый спрашивает лимит у AbstractMinecart.getMaxSpeed (его переопределяет
 * MinecartMovementMixin), а новый считает лимит сам и до AbstractMinecart не доходит.
 * Этот миксин закрывает вторую ветку, чтобы мод работал в обоих режимах.
 * <p>
 * Поле minecart объявлено в MinecartBehavior (родитель), поэтому берём его
 * обычным обращением к protected-полю, а не через @Shadow.
 */
@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehavior {

    protected NewMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void fastminecarts$overrideNewBehaviorMaxSpeed(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        ModConfig config = FastMinecarts.getConfig();
        if (config != null && MinecartEligibility.isEligible(this.minecart, config)) {
            cir.setReturnValue(config.getMaxSpeedBlocksPerTick());
        }
    }
}
