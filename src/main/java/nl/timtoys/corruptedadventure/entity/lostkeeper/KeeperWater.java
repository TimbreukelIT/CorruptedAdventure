package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a fighter stands, as The Lost Keeper picks its attacks: in or on {@link Footing#WATER} (swimming, in a boat, or
 * water a few blocks under them), on a {@link Footing#SHORE} (on ground with deep water beside them), on {@link
 * Footing#LAND} (an island, a beach far from water) or in the {@link Footing#AIR}. Plus the water columns it dives into
 * for its burst. Block lookups only, so it works the same for a real and a GameTest player.
 */
public final class KeeperWater {
    /** Blocks of air (or a boat) between a fighter's feet and the water below that still count as "on the water". */
    private static final int AIR_ABOVE_WATER = 3;
    /** Blocks under the feet that still count as standing on the ground. */
    private static final double GROUND = 1.2;
    private static final int MAX_DEPTH = 64;

    public enum Footing {
        WATER, SHORE, LAND, AIR
    }

    /** A column of water: its top water block and how many water blocks go down from there. */
    public record Column(BlockPos top, int depth) {
        /** The water's surface above the column's centre (the top of the top block). */
        public Vec3 surface() {
            return new Vec3(top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5);
        }
    }

    private KeeperWater() {
    }

    public static Footing footing(Entity fighter, int shoreReach, int minDepth) {
        Level level = fighter.level();
        if (fighter.isInWater() || fighter.getVehicle() instanceof Boat || below(level, fighter.position()) != null) {
            return Footing.WATER;
        }
        if (!grounded(level, fighter.position())) {
            return Footing.AIR;
        }
        return beside(level, fighter.position(), shoreReach, minDepth) != null ? Footing.SHORE : Footing.LAND;
    }

    /** The water column under {@code feet} (in it, or with at most a few blocks of air between), or null. */
    @Nullable
    public static Column below(Level level, Vec3 feet) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(feet.x, feet.y + 0.5, feet.z).mutable();
        for (int k = 0; k <= AIR_ABOVE_WATER + 1; k++, pos.move(0, -1, 0)) {
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                return column(level, pos);
            }
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return null;
            }
        }
        return null;
    }

    /**
     * The nearest water column beside {@code feet} within {@code reach} blocks (horizontally) whose surface is at most a
     * few blocks below the feet and that is at least {@code minDepth} deep, or null: the water at a shore.
     */
    @Nullable
    public static Column beside(Level level, Vec3 feet, int reach, int minDepth) {
        BlockPos origin = BlockPos.containing(feet);
        Column best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double distance = dx * dx + dz * dz;
                if (distance > reach * reach || distance >= bestDistance) {
                    continue;
                }
                for (int dy = 0; dy >= -AIR_ABOVE_WATER; dy--) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getFluidState(pos).is(FluidTags.WATER)) {
                        Column column = column(level, pos);
                        if (column.depth() >= minDepth) {
                            best = column;
                            bestDistance = distance;
                        }
                        break;
                    }
                    if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                        break;
                    }
                }
            }
        }
        return best;
    }

    /** The column through a water block: up to its top, then the water blocks down from there. */
    static Column column(Level level, BlockPos water) {
        BlockPos.MutableBlockPos pos = water.mutable();
        while (level.getFluidState(pos.above()).is(FluidTags.WATER) && pos.getY() < level.getMaxBuildHeight()) {
            pos.move(0, 1, 0);
        }
        BlockPos top = pos.immutable();
        int depth = 0;
        while (depth < MAX_DEPTH && level.getFluidState(pos).is(FluidTags.WATER)) {
            depth++;
            pos.move(0, -1, 0);
        }
        return new Column(top, depth);
    }

    private static boolean grounded(Level level, Vec3 feet) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double down = 0.05; down <= GROUND; down += 0.5) {
            pos.set(feet.x, feet.y - down, feet.z);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
