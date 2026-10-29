package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.PlayerConditions;
import nl.timtoys.corruptedadventure.condition.Ring5DamageTypes;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;
import nl.timtoys.corruptedadventure.weather.Ring5Glitches;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ontladen grond, the world side: which columns of a patch may unload, and what the void does with whoever falls in. A
 * column unloads only whole, top to the bottom of the world (so there is true void under you): every block in it natural
 * ground, air or a plant ({@link UnfinishedNature}), no block entity and no fluid in it or beside it; any other column of
 * the patch stays standing. A player who fell {@code unloadCatchDepth} below the patch's surface is caught by the void: it
 * throws him out onto firm ground at the patch's edge, with void meter and void damage. Server-side.
 */
public final class UnfinishedUnload {
    private static final int EDGE_MIN = 2;
    private static final int EDGE_MAX = 6;
    private static final int EDGE_ANGLES = 16;

    /** A column that may unload: its blocks from {@code top} down to the bottom of the world. */
    public record Column(int x, int z, int top) {
    }

    /** A patch unloading downward, a few layers a tick, until every column reaches the bottom of the world. */
    static final class Dig {
        private final List<Column> columns;
        private final int[] nextY;
        private final long openUntil;

        Dig(List<Column> columns, long openUntil) {
            this.columns = List.copyOf(columns);
            this.nextY = new int[this.columns.size()];
            for (int i = 0; i < nextY.length; i++) {
                nextY[i] = this.columns.get(i).top();
            }
            this.openUntil = openUntil;
        }

        /** Takes the next {@code layers} layers off every column; true once all reach the bottom of the world. */
        boolean step(ServerLevel level, int layers) {
            UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
            BlockState air = Blocks.AIR.defaultBlockState();
            int bottom = level.getMinBuildHeight();
            boolean left = false;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < columns.size(); i++) {
                Column column = columns.get(i);
                if (nextY[i] < bottom) {
                    continue;
                }
                pos.set(column.x(), nextY[i], column.z());
                if (!level.isLoaded(pos)) {
                    // Its chunk went away meanwhile: the column stops where it is (reading it would load it again).
                    nextY[i] = bottom - 1;
                    continue;
                }
                int stop = Math.max(bottom, nextY[i] - layers + 1);
                for (int y = nextY[i]; y >= stop; y--) {
                    pos.setY(y);
                    if (!level.getBlockState(pos).isAir()) {
                        data.swap(level, pos.immutable(), air, openUntil, UnfinishedTempBlocks.Kind.GROUND);
                    }
                }
                nextY[i] = stop - 1;
                left |= nextY[i] >= bottom;
            }
            return !left;
        }
    }

    private UnfinishedUnload() {
    }

    /** The columns of the patch round {@code centre}, nearest first, together at most {@code maxBlocks} blocks. */
    public static List<Column> plan(ServerLevel level, BlockPos centre, int radius, int maxBlocks, Vec3 keepClear, double clearance) {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius + radius) {
                    offsets.add(new int[]{dx, dz});
                }
            }
        }
        offsets.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        List<Column> columns = new ArrayList<>();
        int blocks = 0;
        for (int[] offset : offsets) {
            int x = centre.getX() + offset[0];
            int z = centre.getZ() + offset[1];
            double kx = x + 0.5 - keepClear.x;
            double kz = z + 0.5 - keepClear.z;
            if (kx * kx + kz * kz < clearance * clearance) {
                continue;
            }
            int count = count(level, x, z);
            if (count <= 0 || blocks + count > maxBlocks) {
                continue;
            }
            blocks += count;
            columns.add(new Column(x, z, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1));
        }
        return columns;
    }

    /** The blocks the column at {@code x, z} would lose, or -1 when it may not unload (or is void already). */
    private static int count(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return -1;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            // The fluid check beside it would load a neighbour chunk; ground we cannot see may not unload.
            if (!level.hasChunkAt(new BlockPos(x + direction.getStepX(), 0, z + direction.getStepZ()))) {
                return -1;
            }
        }
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        for (int y = top; y >= level.getMinBuildHeight(); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (!UnfinishedNature.isGround(state) && !UnfinishedNature.isOpen(state)) {
                    return -1;
                }
                count++;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                side.setWithOffset(pos, direction);
                if (!level.getFluidState(side).isEmpty()) {
                    return -1;
                }
            }
        }
        return count;
    }

    /** The void catches whoever fell deep enough into the open hole. */
    static void catchFallen(ServerLevel level, UnfinishedTempBlocks.Hole hole) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        double depth = hole.surface() - config.unloadCatchDepth.get();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.getAbilities().flying || player.getY() > depth || !hole.contains(player)) {
                continue;
            }
            Vec3 to = edge(level, hole, player.position());
            if (to == null) {
                continue;
            }
            Vec3 from = player.position();
            Ring5Glitches.teleported(player, from, to);
            player.teleportTo(to.x, to.y, to.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
            player.hurtMarked = true;
            if (!player.isCreative()) {
                PlayerConditions.add(player, Condition.VOID, config.unloadCatchMeter.get().floatValue());
                float damage = config.unloadCatchDamage.get().floatValue();
                if (damage > 0.0F) {
                    player.hurt(player.damageSources().source(Ring5DamageTypes.VOID), damage);
                }
            }
        }
    }

    /** Firm ground just outside the hole, on the side the player fell in from first; null if there is none. */
    @Nullable
    private static Vec3 edge(ServerLevel level, UnfinishedTempBlocks.Hole hole, Vec3 near) {
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        double cx = hole.centre().getX() + 0.5;
        double cz = hole.centre().getZ() + 0.5;
        double start = Math.atan2(near.z - cz, near.x - cx);
        for (int out = EDGE_MIN; out <= EDGE_MAX; out++) {
            double distance = hole.radius() + out;
            for (int i = 0; i < EDGE_ANGLES; i++) {
                // 0, +1, -1, +2, -2 ... steps round from the player's side.
                int step = (i + 1) / 2 * (i % 2 == 0 ? -1 : 1);
                double angle = start + step * Math.PI * 2.0 / EDGE_ANGLES;
                int x = (int) Math.floor(cx + Math.cos(angle) * distance);
                int z = (int) Math.floor(cz + Math.sin(angle) * distance);
                if (!level.hasChunkAt(new BlockPos(x, 0, z)) || !level.getWorldBorder().isWithinBounds(x, z)) {
                    continue;
                }
                BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                BlockPos floor = feet.below();
                if (feet.getY() <= level.getMinBuildHeight() || data.isTracked(floor)
                        || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                        || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                    continue;
                }
                return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }
}
