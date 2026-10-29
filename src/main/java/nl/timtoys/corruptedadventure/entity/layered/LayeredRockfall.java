package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.config.LayeredConfig;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The blocks of The Layered's rock fall. Only natural stone and sand (block tag
 * {@code corruptedadventure:layered_rockfall}, never a block entity, never outside the world border) comes loose; it
 * falls as real blocks ({@link FallingBlockEntity}, hurting like an anvil per {@code rockfallDamagePerBlockFallen}) and
 * stays where it lands, so the damage to the canyon is permanent (design decision). Canyon stone falls as canyon
 * cobblestone and sandstone as loose sand; sand, gravel and loose sand fall as themselves.
 * <p>
 * First the undersides of overhangs above the target (one block per column, within {@link #RADIUS} blocks and 3 to
 * {@code rockfallReachUp} blocks above its feet, not over a lower ceiling); if that is not enough, blocks pried from the
 * wall it clings to, thrown at the target.
 */
public final class LayeredRockfall {
    private static final int RADIUS = 2;
    private static final int MIN_UP = 3;
    /** Ticks a thrown rock takes to reach the target (roughly: the falling block's drag is left out). */
    private static final double THROW_TICKS = 16.0;
    private static final double GRAVITY = 0.04;
    /** Entity tag on the blocks of a rock fall: they do not hurt The Layered ({@link #isRock}). */
    private static final String ROCK_TAG = "corruptedadventure.layered_rock";

    private LayeredRockfall() {
    }

    /** Breaks up to {@code max} blocks loose for a rock fall at {@code target}; returns how many fell. */
    public static int drop(ServerLevel level, TheLayered layered, Entity target, int max) {
        if (max <= 0) {
            return 0;
        }
        LayeredConfig settings = TheLayered.settings();
        RandomSource random = layered.getRandom();
        List<BlockPos> overhead = overhead(level, target.blockPosition(), settings.rockfallReachUp.get());
        Util.shuffle(overhead, random);
        int count = 0;
        for (BlockPos pos : overhead) {
            if (count >= max) {
                return count;
            }
            if (loosen(level, pos, null, settings, layered.strength()) != null) {
                count++;
            }
        }
        if (layered.isOnWall()) {
            List<BlockPos> face = wallFace(level, layered);
            Util.shuffle(face, random);
            for (BlockPos pos : face) {
                if (count >= max) {
                    break;
                }
                Vec3 from = Vec3.atBottomCenterOf(pos);
                if (loosen(level, pos, throwVelocity(from, target.position()), settings, layered.strength()) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The lowest loose-able block with air under it in each column above the feet: the underside of an overhang. */
    public static List<BlockPos> overhead(Level level, BlockPos feet, int reachUp) {
        List<BlockPos> result = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS + 1) {
                    continue;
                }
                for (int dy = 1; dy <= reachUp; dy++) {
                    BlockPos pos = feet.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (dy < MIN_UP) {
                        if (!state.getCollisionShape(level, pos).isEmpty()) {
                            // A low ceiling: what falls from above lands on it, not on the target.
                            break;
                        }
                        continue;
                    }
                    if (state.isAir()) {
                        continue;
                    }
                    if (canLoosen(level, pos, state) && level.getBlockState(pos.below()).isAir()) {
                        result.add(pos);
                    }
                    // The first solid block in the column closes it: nothing above it can fall through.
                    break;
                }
            }
        }
        return result;
    }

    /** Exposed blocks of the wall beside it (2 to each side, from its feet up 4), with air in front of them. */
    private static List<BlockPos> wallFace(Level level, TheLayered layered) {
        Direction wall = layered.getSurface();
        Direction side = wall.getClockWise();
        List<BlockPos> result = new ArrayList<>();
        double reach = layered.getBbWidth() / 2.0 + 0.5;
        for (int up = 0; up <= 4; up++) {
            for (int lateral = -2; lateral <= 2; lateral++) {
                BlockPos pos = BlockPos.containing(
                        layered.getX() + wall.getStepX() * reach + side.getStepX() * lateral,
                        layered.getY() + up,
                        layered.getZ() + wall.getStepZ() * reach + side.getStepZ() * lateral);
                BlockState state = level.getBlockState(pos);
                if (canLoosen(level, pos, state) && level.getBlockState(pos.relative(wall.getOpposite())).isAir()) {
                    result.add(pos);
                }
            }
        }
        return result;
    }

    public static boolean canLoosen(Level level, BlockPos pos, BlockState state) {
        return state.is(ModTags.Blocks.LAYERED_ROCKFALL) && !state.hasBlockEntity() && level.getWorldBorder().isWithinBounds(pos)
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    /** What a block falls as: canyon stone crumbles to cobblestone, sandstone to loose sand. */
    public static BlockState fallingState(BlockState state) {
        if (state.getBlock() instanceof FallingBlock) {
            return state;
        }
        if (state.is(ModTags.Blocks.CANYON_STONES)) {
            return ModBlocks.CANYON_COBBLESTONE.get().defaultBlockState();
        }
        if (state.is(Tags.Blocks.SANDSTONE_BLOCKS) || state.is(ModBlocks.SCORCHED_SANDSTONE.get())) {
            return ModBlocks.LOOSE_SAND.get().defaultBlockState();
        }
        return state;
    }

    @Nullable
    private static FallingBlockEntity loosen(ServerLevel level, BlockPos pos, @Nullable Vec3 velocity, LayeredConfig settings,
                                             float strength) {
        BlockState state = level.getBlockState(pos);
        if (!canLoosen(level, pos, state)) {
            return null;
        }
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        FallingBlockEntity rock = FallingBlockEntity.fall(level, pos, fallingState(state));
        rock.addTag(ROCK_TAG);
        rock.setHurtsEntities(settings.rockfallDamagePerBlockFallen.get().floatValue() * strength,
                Math.round(settings.rockfallMaxDamage.get() * strength));
        if (velocity != null) {
            rock.setDeltaMovement(velocity);
        }
        return rock;
    }

    /** Whether {@code entity} is a block of a Layered's rock fall (saved with it, so also after a reload). */
    public static boolean isRock(@Nullable Entity entity) {
        return entity instanceof FallingBlockEntity && entity.getTags().contains(ROCK_TAG);
    }

    /** A throw that lands near {@code to} after about {@link #THROW_TICKS} ticks under gravity. */
    private static Vec3 throwVelocity(Vec3 from, Vec3 to) {
        double t = THROW_TICKS;
        return new Vec3((to.x - from.x) / t, (to.y - from.y) / t + 0.5 * GRAVITY * t, (to.z - from.z) / t);
    }
}
