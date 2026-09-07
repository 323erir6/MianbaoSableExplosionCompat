package dev.mianbaosablecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.mianbaosablecompat.ExplosionBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;
import java.util.function.Predicate;

/** Optional individual hooks: procedures have different signatures/call sites.
 * Hook coverage is verified against the supported Mianbao jar at build time.
 * Vanilla explosions are moved once from plot storage to physical space.
 */
@Mixin(targets = {
    "net.mcreator.myfirstmod.procedures.AgmexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.AntimanmineexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Antitankmineexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.Antitankmissile1explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Autocannonexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.AutocannonexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.BombexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.BombexplodebyexplosionProcedure",
    "net.mcreator.myfirstmod.procedures.BombexplodebyhitProcedure",
    "net.mcreator.myfirstmod.procedures.BombjettansheexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.CloseMissileexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.ClusterBombexplodebyexplosionProcedure",
    "net.mcreator.myfirstmod.procedures.ClusterBombexplodebyhitProcedure",
    "net.mcreator.myfirstmod.procedures.ExplosionblastworkProcedure",
    "net.mcreator.myfirstmod.procedures.ExplosionscorchedProcedure",
    "net.mcreator.myfirstmod.procedures.FarMissileexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.FarRocketexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Farrocketfireexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.FireBombexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.FireBombexplodebyexplosionProcedure",
    "net.mcreator.myfirstmod.procedures.FireBombexplodebyhitProcedure",
    "net.mcreator.myfirstmod.procedures.Fireball_tanshe_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Firemineexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.Fueltankexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.FueltankexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Gasmineexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.GroundMissileexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.GunTurret_tanshe_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Gun_defend_car_rocket_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.HeadattackexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.HeavyRocketexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Heavyrocketfireexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.LittleBombexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.MediumBombexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.MediumBombexplodebyexplosionProcedure",
    "net.mcreator.myfirstmod.procedures.MediumBombexplodebyhitProcedure",
    "net.mcreator.myfirstmod.procedures.Missile2explodeProcedure",
    "net.mcreator.myfirstmod.procedures.MissileexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.MissileturretmissiletansheexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.MortarammoexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.MortarfireammoexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.MortargasammoexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Nuke_dynamite_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Nuke_shrapnel_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Nuke_test_2Procedure",
    "net.mcreator.myfirstmod.procedures.PortableantiairmissileexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.PortableantiairsystemmissileexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.PortableantimissilemissiletansheexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Portableantitankrocketexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.PortableantitankrocketexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.RocketTurret_tanshe_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Rocket_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Rocketer_tanshe_ExplodeProcedure",
    "net.mcreator.myfirstmod.procedures.ShellrackExplosionSupport",
    "net.mcreator.myfirstmod.procedures.Shellrackexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.ShellrackexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Shellrackexplodebyexplode2Procedure",
    "net.mcreator.myfirstmod.procedures.ShellrackexplodebyexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.TNTexplode4Procedure",
    "net.mcreator.myfirstmod.procedures.TNTexplode5Procedure",
    "net.mcreator.myfirstmod.procedures.TorpedoexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.UCAVtansheexplodeProcedure",
    "net.mcreator.myfirstmod.procedures.Wasp_caller_explodeProcedure",
    "net.mcreator.myfirstmod.procedures.Wolves_grenade_explodeProcedure"
}, remap = false)
public abstract class DirectExplosionMixin {
    @WrapMethod(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDD)V", require = 0)
    private static void compat$frameCoords(LevelAccessor world, double x, double y, double z, Operation<Void> original) {
        try (ExplosionBridge.Frame frame = ExplosionBridge.enter(world, x, y, z)) {
            original.call(world, x, y, z);
        }
    }

    @WrapMethod(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", require = 0)
    private static void compat$frameEntity(LevelAccessor world, double x, double y, double z, Entity entity, Operation<Void> original) {
        try (ExplosionBridge.Frame frame = ExplosionBridge.enter(world, x, y, z)) {
            original.call(world, x, y, z, entity);
        }
    }

    @WrapMethod(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/level/block/state/BlockState;)V", require = 0)
    private static void compat$frameState(LevelAccessor world, double x, double y, double z, BlockState state, Operation<Void> original) {
        try (ExplosionBridge.Frame frame = ExplosionBridge.enter(world, x, y, z)) {
            original.call(world, x, y, z, state);
        }
    }

    @WrapMethod(method = "explodeSalvo(Lnet/minecraft/server/level/ServerLevel;DDDFDF)V", require = 0)
    private static void compat$salvoFrame(ServerLevel world, double x, double y, double z, float strength, double radius, float damage, Operation<Void> original) {
        try (ExplosionBridge.Frame frame = ExplosionBridge.enter(world, x, y, z)) {
            original.call(world, x, y, z, strength, radius, damage);
        }
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/mcreator/myfirstmod/MianbaosModernwarfareMod;queueServerWork(ILjava/lang/Runnable;)V"), require = 0)
    private static void compat$queueServerWork(int delay, Runnable task, Operation<Void> original) {
        original.call(delay, ExplosionBridge.wrapTask(task));
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)V"), require = 0)
    private static void compat$command(Commands commands, CommandSourceStack source, String command, Operation<Void> original) {
        if (ExplosionBridge.shouldRunCommand(command))
            original.call(commands, ExplosionBridge.commandSource(source, command), command);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"), require = 0)
    private static Explosion compat$explode(net.minecraft.world.level.Level world, Entity source,
                                            double x, double y, double z, float radius,
                                            net.minecraft.world.level.Level.ExplosionInteraction interaction,
                                            Operation<Explosion> original) {
        Vec3 position = ExplosionBridge.explosionPosition(world, x, y, z);
        return original.call(world, source, position.x, position.y, position.z, radius, interaction);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"), require = 0)
    private static Explosion compat$explodeServer(ServerLevel world, Entity source,
                                                  double x, double y, double z, float radius,
                                                  net.minecraft.world.level.Level.ExplosionInteraction interaction,
                                                  Operation<Explosion> original) {
        Vec3 position = ExplosionBridge.explosionPosition(world, x, y, z);
        return original.call(world, source, position.x, position.y, position.z, radius, interaction);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 0)
    private static BlockState compat$getBlockStateAccessor(LevelAccessor world, BlockPos pos) {
        return ExplosionBridge.getBlockState(world, pos);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"), require = 0)
    private static boolean compat$destroyBlockAccessor(LevelAccessor world, BlockPos pos, boolean drops) {
        return ExplosionBridge.destroyBlock(world, pos, drops);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 0)
    private static boolean compat$setBlockAccessor(LevelAccessor world, BlockPos pos, BlockState state, int flags) {
        return ExplosionBridge.setBlock(world, pos, state, flags);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), require = 0)
    private static <T extends Entity> List<T> compat$getEntitiesOfClassAccessor(LevelAccessor world, Class<T> type, AABB bounds, Predicate<? super T> predicate) {
        return ExplosionBridge.getEntities(world, type, bounds, predicate);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 0)
    private static BlockState compat$getBlockStateLevel(net.minecraft.world.level.Level world, BlockPos pos) {
        return ExplosionBridge.getBlockState(world, pos);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"), require = 0)
    private static boolean compat$destroyBlockLevel(net.minecraft.world.level.Level world, BlockPos pos, boolean drops) {
        return ExplosionBridge.destroyBlock(world, pos, drops);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 0)
    private static boolean compat$setBlockLevel(net.minecraft.world.level.Level world, BlockPos pos, BlockState state, int flags) {
        return ExplosionBridge.setBlock(world, pos, state, flags);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), require = 0)
    private static <T extends Entity> List<T> compat$getEntitiesOfClassLevel(net.minecraft.world.level.Level world, Class<T> type, AABB bounds, Predicate<? super T> predicate) {
        return ExplosionBridge.getEntities(world, type, bounds, predicate);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 0)
    private static BlockState compat$getBlockStateServer(ServerLevel world, BlockPos pos) {
        return ExplosionBridge.getBlockState(world, pos);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"), require = 0)
    private static boolean compat$destroyBlockServer(ServerLevel world, BlockPos pos, boolean drops) {
        return ExplosionBridge.destroyBlock(world, pos, drops);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 0)
    private static boolean compat$setBlockServer(ServerLevel world, BlockPos pos, BlockState state, int flags) {
        return ExplosionBridge.setBlock(world, pos, state, flags);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), require = 0)
    private static <T extends Entity> List<T> compat$getEntitiesOfClassServer(ServerLevel world, Class<T> type, AABB bounds, Predicate<? super T> predicate) {
        return ExplosionBridge.getEntities(world, type, bounds, predicate);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/mcreator/myfirstmod/util/ExplosionBlockGuard;canDestroy(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Z"), require = 0)
    private static boolean compat$canDestroy(LevelAccessor world, BlockPos pos) {
        return ExplosionBridge.canDestroy(world, pos);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canSurvive(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"), require = 0)
    private static boolean compat$canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return ExplosionBridge.canSurvive(state, world, pos);
    }
}

