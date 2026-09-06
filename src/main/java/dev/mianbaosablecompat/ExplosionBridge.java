package dev.mianbaosablecompat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Only called at explicitly selected Mianbao procedure call sites. */
public final class ExplosionBridge {
    private static final TagKey<Block> NO_BLOCK = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath("mianbaos_modernwarfare", "no_block"));
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final Matrix4dc IDENTITY = new Matrix4d();

    public static final class Frame implements AutoCloseable {
        final Frame parent;
        final ServerLevel level;
        final SubLevel source;
        final Matrix4d sourceToWorld;
        Sample last;

        Frame(Frame parent, ServerLevel level, SubLevel source, Matrix4d matrix) {
            this.parent = parent; this.level = level; this.source = source; this.sourceToWorld = matrix;
        }
        @Override public void close() {
            if (parent == null) CURRENT.remove(); else CURRENT.set(parent);
        }
    }

    private record Sample(BlockPos input, List<BlockPos> targets) {}
    private record Selected(BlockPos pos, BlockState state) {}

    public static Frame enter(LevelAccessor world, double x, double y, double z) {
        Frame parent = CURRENT.get();
        ServerLevel level = world instanceof ServerLevel server ? server : null;
        SubLevel source = level == null ? null : Sable.HELPER.getContaining(level, x, z);
        Matrix4d matrix = source == null ? new Matrix4d() : source.logicalPose().bakeIntoMatrix(new Matrix4d());
        // A nested scorching call may run after the initial vanilla blast has
        // removed its source plot. The enclosing blast still knows its pose.
        if (level != null && source == null && parent != null && parent.level == level
                && parent.source != null && Sable.HELPER.isInPlotGrid(level, ((int)Math.floor(x)) >> 4, ((int)Math.floor(z)) >> 4)) {
            source = parent.source;
            matrix.set(parent.sourceToWorld);
        }
        Frame frame = new Frame(parent, level, source, matrix);
        CURRENT.set(frame);
        return frame;
    }

    private static boolean enabled(LevelAccessor world) {
        return world instanceof ServerLevel && CompatConfig.ENABLED.get();
    }

    private static Sample sample(ServerLevel level, BlockPos input) {
        Frame frame = CURRENT.get();
        if (frame != null && frame.level == level && frame.last != null
                && frame.last.input.equals(input)) return frame.last;
        SubLevel source;
        Matrix4dc toWorld;
        if (frame != null && frame.level == level && frame.source != null
                && Sable.HELPER.isInPlotGrid(level, input.getX() >> 4, input.getZ() >> 4)) {
            source = frame.source;
            toWorld = frame.sourceToWorld;
        } else {
            source = Sable.HELPER.getContaining(level, input);
            toWorld = source == null ? IDENTITY : source.logicalPose().bakeIntoMatrix(new Matrix4d());
        }
        Set<BlockPos> targets = new LinkedHashSet<>();
        targets.add(input.immutable()); // Preserve Mianbao's original operation.
        BoundingBox3d bounds = new BoundingBox3d(input.getX(), input.getY(), input.getZ(),
                input.getX() + 1.0, input.getY() + 1.0, input.getZ() + 1.0);
        if (source != null) {
            org.joml.Vector3d corner = new org.joml.Vector3d();
            double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
            double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
            for (int i = 0; i < 8; i++) {
                toWorld.transformPosition(corner.set(input.getX() + (i & 1),
                        input.getY() + ((i >> 1) & 1), input.getZ() + ((i >> 2) & 1)));
                minX = Math.min(minX, corner.x); minY = Math.min(minY, corner.y); minZ = Math.min(minZ, corner.z);
                maxX = Math.max(maxX, corner.x); maxY = Math.max(maxY, corner.y); maxZ = Math.max(maxZ, corner.z);
            }
            if (!Double.isFinite(minX + minY + minZ + maxX + maxY + maxZ))
                return new Sample(input.immutable(), List.copyOf(targets));
            bounds = new BoundingBox3d(minX, minY, minZ, maxX, maxY, maxZ);
            if (minY < level.getMaxBuildHeight() && maxY >= level.getMinBuildHeight())
                addProjection(targets, level, input, toWorld, true, null);
        }
        for (SubLevel other : Sable.HELPER.getAllIntersecting(level, bounds)) {
            if (other == source) continue;
            Matrix4d worldToOther = other.logicalPose().bakeIntoMatrix(new Matrix4d()).invert();
            addProjection(targets, level, input, worldToOther.mul(toWorld), false, other);
        }
        Sample result = new Sample(input.immutable(), List.copyOf(targets));
        if (frame != null && frame.level == level) frame.last = result;
        return result;
    }

    private static void addProjection(Set<BlockPos> targets, ServerLevel level, BlockPos input,
                                      Matrix4dc forward, boolean terrain, SubLevel expectedPlot) {
        Matrix4d inverse = new Matrix4d(forward).invert();
        CellProjection.visit(forward, inverse, input.getX(), input.getY(), input.getZ(), (x, y, z) -> {
            if (terrain) {
                if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()
                        || Sable.HELPER.isInPlotGrid(level, x >> 4, z >> 4)) return;
            } else if (Sable.HELPER.getContaining(level, x >> 4, z >> 4) != expectedPlot) return;
            targets.add(new BlockPos(x, y, z));
        });
    }

    private static Selected select(ServerLevel level, Sample sample) {
        Selected original = null;
        for (BlockPos target : sample.targets) {
            BlockState state = readState(level, target);
            if (original == null) original = new Selected(target, state);
            if (!state.isAir()) return new Selected(target, state);
        }
        return original;
    }

    private static BlockState readState(ServerLevel level, BlockPos pos) {
        boolean plot = Sable.HELPER.isInPlotGrid(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (plot) {
            // A blast can remove its own plot before a nested scorching call.
            // Never generate a terrain chunk at a now-vacant storage address.
            if (Sable.HELPER.getContaining(level, pos) == null) return Blocks.AIR.defaultBlockState();
        } else if (level.isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        return level.getBlockState(pos);
    }

    public static BlockState getBlockState(LevelAccessor world, BlockPos pos) {
        if (!enabled(world)) return world.getBlockState(pos);
        return select((ServerLevel)world, sample((ServerLevel)world, pos)).state;
    }

    public static boolean canDestroy(LevelAccessor world, BlockPos pos) {
        if (!enabled(world)) return destructible(world, pos, world.getBlockState(pos));
        Selected selected = select((ServerLevel)world, sample((ServerLevel)world, pos));
        return destructible(world, selected.pos, selected.state);
    }

    private static boolean destructible(LevelAccessor world, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getDestroySpeed(world, pos) > 0.0F
                && !state.is(NO_BLOCK) && !(state.getBlock() instanceof LiquidBlock)
                && !state.getFluidState().isSource();
    }

    public static boolean destroyBlock(LevelAccessor world, BlockPos pos, boolean drops) {
        if (!enabled(world)) return world.destroyBlock(pos, drops);
        ServerLevel level = (ServerLevel)world;
        Sample sample = sample(level, pos);
        // Snapshot first: onRemove/neighbor callbacks can cause a nested explosion.
        List<Selected> before = new ArrayList<>();
        for (BlockPos p : sample.targets) before.add(new Selected(p, readState(level, p)));
        boolean changed = false;
        for (Selected s : before) {
            if (s.state.isAir() || readState(level, s.pos) != s.state) continue;
            if (s.pos.equals(pos) || destructible(level, s.pos, s.state))
                changed |= level.destroyBlock(s.pos, drops);
        }
        invalidate();
        return changed;
    }

    public static boolean setBlock(LevelAccessor world, BlockPos pos, BlockState state, int flags) {
        if (!enabled(world)) return world.setBlock(pos, state, flags);
        boolean air = state.isAir();
        boolean fire = state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
        boolean thermal = isThermal(state);
        // Never copy controllers, NBT host blocks, powered/time/radius property
        // changes or new ordnance into another coordinate space.
        if (!air && (!CompatConfig.THERMAL_EFFECTS.get() || !thermal))
            return world.setBlock(pos, state, flags);
        ServerLevel level = (ServerLevel)world;
        Sample sample = sample(level, pos);
        Selected selected = select(level, sample);
        List<Selected> before = new ArrayList<>();
        for (BlockPos p : sample.targets) before.add(new Selected(p, readState(level, p)));
        boolean changed = false;
        for (Selected old : before) {
            if (readState(level, old.pos) != old.state || (air && old.state.isAir())) continue;
            if (fire && (!old.state.isAir() || !state.canSurvive(level, old.pos))) continue;
            boolean original = old.pos.equals(pos);
            if (!original) {
                if (old.state.getDestroySpeed(level, old.pos) < 0 || old.state.is(NO_BLOCK)) continue;
                if (fire) {
                    if (!old.state.isAir() || !state.canSurvive(level, old.pos)) continue;
                } else if (air) {
                    if (old.state.isAir()) continue;
                } else if (old.state.isAir() || old.state.getBlock() != selected.state.getBlock()) {
                    continue; // Scorched wood must not turn nearby stone into wood.
                }
            }
            // If the original lookup borrowed a ship's state, do not materialize
            // a scorched duplicate of that block in empty terrain.
            if (original && !air && !fire && old.state.isAir()) continue;
            changed |= level.setBlock(old.pos, state, flags);
        }
        invalidate();
        return changed;
    }

    private static boolean isThermal(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.COAL_BLOCK) || state.is(Blocks.PACKED_MUD)
                || state.is(Blocks.MUD) || state.is(Blocks.SANDSTONE) || state.is(Blocks.PRISMARINE)
                || (id.getNamespace().equals("mianbaos_modernwarfare") && id.getPath().startsWith("scorched"));
    }

    public static boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader reader, BlockPos pos) {
        if (!(reader instanceof ServerLevel level) || !CompatConfig.ENABLED.get()
                || !CompatConfig.THERMAL_EFFECTS.get()) return state.canSurvive(reader, pos);
        for (BlockPos p : sample(level, pos).targets)
            if (state.canSurvive(reader, p)) return true;
        return false;
    }

    public static <T extends Entity> List<T> getEntities(LevelAccessor world, Class<T> type,
                                                       AABB bounds, Predicate<? super T> predicate) {
        if (!enabled(world) || !CompatConfig.ENTITY_QUERIES.get())
            return world.getEntitiesOfClass(type, bounds, predicate);
        ServerLevel level = (ServerLevel)world;
        Frame f = CURRENT.get();
        Matrix4d pose = null;
        if (f != null && f.level == level && f.source != null
                && Sable.HELPER.isInPlotGrid(level, ((int)Math.floor(bounds.getCenter().x)) >> 4,
                ((int)Math.floor(bounds.getCenter().z)) >> 4)) pose = f.sourceToWorld;
        else {
            SubLevel source = Sable.HELPER.getContaining(level, bounds.getCenter());
            if (source != null) pose = source.logicalPose().bakeIntoMatrix(new Matrix4d());
        }
        if (pose == null) return world.getEntitiesOfClass(type, bounds, predicate);
        org.joml.Vector3d center = pose.transformPosition(new org.joml.Vector3d(
                bounds.getCenter().x, bounds.getCenter().y, bounds.getCenter().z));
        // Mianbao's damage radius is a world-space distance, not plot scale.
        AABB projected = AABB.ofSize(new net.minecraft.world.phys.Vec3(center.x, center.y, center.z),
                bounds.getXsize(), bounds.getYsize(), bounds.getZsize());
        return world.getEntitiesOfClass(type, projected, predicate);
    }

    private static void invalidate() {
        Frame f = CURRENT.get();
        if (f != null) f.last = null;
    }
    private ExplosionBridge() {}
}
