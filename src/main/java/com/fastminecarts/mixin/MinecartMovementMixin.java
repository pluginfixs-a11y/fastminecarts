package com.fastminecarts.mixin;

import com.fastminecarts.FastMinecarts;
import com.fastminecarts.config.ModConfig;
import com.fastminecarts.util.MinecartEligibility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractMinecart.class)
public abstract class MinecartMovementMixin {

    private static final double SAFE_TURN_SPEED = 0.4; // 8 блоков/сек на поворотах и подъемах

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void fastminecarts$overrideMaxSpeed(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        ModConfig config = FastMinecarts.getConfig();
        if (config != null && MinecartEligibility.isEligible(self, config)) {
            cir.setReturnValue(config.getMaxSpeedBlocksPerTick());
        }
    }

    // --- ОТКЛЮЧЕНИЕ ТВЕРДОЙ ФИЗИКИ СТОЛКНОВЕНИЙ ---

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void fastminecarts$canCollideWith(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        ModConfig config = FastMinecarts.getConfig();
        if (config != null && MinecartEligibility.isEligible(self, config)) {
            cir.setReturnValue(false); // Проходить сквозь сущностей
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void fastminecarts$isPushable(CallbackInfoReturnable<Boolean> cir) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        ModConfig config = FastMinecarts.getConfig();
        if (config != null && MinecartEligibility.isEligible(self, config)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void fastminecarts$onPush(Entity entity, CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        ModConfig config = FastMinecarts.getConfig();
        if (config != null && MinecartEligibility.isEligible(self, config)) {
            ci.cancel(); // Отменяем ванильную остановку
        }
    }

    // --- ОСНОВНОЙ ТИК (ДВИЖЕНИЕ + РАЗЛЕТ МОБОВ) ---

    @Inject(method = "tick", at = @At("TAIL"))
    private void fastminecarts$onTailTick(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        ModConfig config = FastMinecarts.getConfig();
        if (config == null || !MinecartEligibility.isEligible(self, config)) {
            return;
        }

        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 1. Управление скоростью на поворотах и прямых
        fastminecarts$updateSpeedAndTurnLogic(self, serverLevel, config);

        // 2. Отбрасывание всех мобов и игроков по курсу движения
        fastminecarts$knockbackEntities(self, serverLevel, config);
    }

    private void fastminecarts$updateSpeedAndTurnLogic(
            AbstractMinecart self,
            ServerLevel serverLevel,
            ModConfig config
    ) {
        Vec3 velocity = self.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 1.0e-5) {
            return;
        }

        double maxSpeed = config.getMaxSpeedBlocksPerTick();
        BlockPos currentPos = self.blockPosition();
        BlockState currentState = serverLevel.getBlockState(currentPos);

        Vec3 dir = velocity.normalize();
        BlockState nextState = getRailStateAhead(serverLevel, currentPos, dir);

        // Торможение на поворотах и подъемах
        if (isSlowRail(currentState) || isSlowRail(nextState)) {
            if (speed > SAFE_TURN_SPEED) {
                self.setDeltaMovement(dir.scale(SAFE_TURN_SPEED));
            }
            return;
        }

        // Разгон на прямых участках
        boolean onPoweredRail = currentState.getBlock() instanceof PoweredRailBlock
                && currentState.getValue(PoweredRailBlock.POWERED);

        if (config.maintainHighSpeedOnSparsePoweredRails || onPoweredRail) {
            self.setDeltaMovement(dir.scale(maxSpeed));
        } else if (speed > maxSpeed) {
            self.setDeltaMovement(dir.scale(maxSpeed));
        }
    }

    private void fastminecarts$knockbackEntities(
            AbstractMinecart self,
            ServerLevel serverLevel,
            ModConfig config
    ) {
        Vec3 velocity = self.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.1) {
            return; // Если вагонетка почти стоит — не отталкиваем
        }

        Vec3 dir = velocity.normalize();

        // Поноразмерная коробка захвата сущностей перед вагонеткой
        AABB searchBox = self.getBoundingBox()
                .expandTowards(dir.x * 2.0, dir.y, dir.z * 2.0)
                .inflate(1.2, 0.8, 1.2);

        List<Entity> targets = serverLevel.getEntities(self, searchBox, target -> {
            if (target == self) return false;
            if (self.hasPassenger(target)) return false; // Не трогать пассажира в этой вагонетке
            if (!target.isAlive() || target.isRemoved()) return false;
            if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
            return true;
        });

        for (Entity target : targets) {
            // Рассчитываем вектор отброса вбок от рельс
            Vec3 sideDir = new Vec3(-dir.z, 0, dir.x);
            Vec3 toTarget = target.position().subtract(self.position());
            if (toTarget.dot(sideDir) < 0) {
                sideDir = sideDir.scale(-1);
            }

            // Сила выстреливания мобов (настраивается или берется мощный дефолт)
            double pushStrength = Math.max(1.5, config.runoverKnockbackStrength * 1.5);
            double verticalStrength = Math.max(0.6, config.runoverKnockbackVertical);

            Vec3 pushImpulse = new Vec3(
                    dir.x * pushStrength + sideDir.x * 0.8,
                    verticalStrength,
                    dir.z * pushStrength + sideDir.z * 0.8
            );

            // Применяем новый вектор движения
            target.setDeltaMovement(target.getDeltaMovement().add(pushImpulse));

            // Для игроков отправляем сетевой пакет (чтобы клиент мгновенно прочувствовал полет)
            if (target instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            } else if (target instanceof LivingEntity living) {
                living.hurtMarked = true; // Заставляет сервер обновить движение моба
            }
        }
    }

    private BlockState getRailStateAhead(ServerLevel level, BlockPos currentPos, Vec3 dir) {
        int dx = (int) Math.round(dir.x);
        int dz = (int) Math.round(dir.z);

        if (dx == 0 && dz == 0) {
            return level.getBlockState(currentPos);
        }

        BlockPos posSame = currentPos.offset(dx, 0, dz);
        BlockState stateSame = level.getBlockState(posSame);
        if (stateSame.getBlock() instanceof BaseRailBlock) {
            return stateSame;
        }

        BlockPos posUp = currentPos.offset(dx, 1, dz);
        BlockState stateUp = level.getBlockState(posUp);
        if (stateUp.getBlock() instanceof BaseRailBlock) {
            return stateUp;
        }

        BlockPos posDown = currentPos.offset(dx, -1, dz);
        BlockState stateDown = level.getBlockState(posDown);
        if (stateDown.getBlock() instanceof BaseRailBlock) {
            return stateDown;
        }

        return stateSame;
    }

    private boolean isSlowRail(BlockState state) {
        if (!(state.getBlock() instanceof BaseRailBlock railBlock)) {
            return false;
        }
        Property<RailShape> shapeProperty = railBlock.getShapeProperty();
        if (!state.hasProperty(shapeProperty)) {
            return false;
        }
        RailShape shape = state.getValue(shapeProperty);

        boolean isTurn = shape == RailShape.SOUTH_EAST
                || shape == RailShape.SOUTH_WEST
                || shape == RailShape.NORTH_WEST
                || shape == RailShape.NORTH_EAST;

        boolean isAscending = shape == RailShape.ASCENDING_EAST
                || shape == RailShape.ASCENDING_WEST
                || shape == RailShape.ASCENDING_NORTH
                || shape == RailShape.ASCENDING_SOUTH;

        return isTurn || isAscending;
    }
}