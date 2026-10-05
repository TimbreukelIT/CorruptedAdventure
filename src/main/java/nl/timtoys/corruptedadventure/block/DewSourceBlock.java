package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Nether ring 2's water-like natural block: it grows dew crystal on its sides, like vanilla's budding amethyst. A plain
 * {@link Block} (not an amethyst block): no amethyst chime on projectile hits, and no hard link to the amethyst buds.
 * Not obtainable, so every source in the world comes from worldgen.
 */
public class DewSourceBlock extends Block {
    public static final MapCodec<DewSourceBlock> CODEC = simpleCodec(DewSourceBlock::new);
    /** A random tick grows a crystal with chance 1 in this many (vanilla budding amethyst's pace). */
    public static final int GROWTH_CHANCE = 5;
    /** Growth order: each stage becomes the next on the same face; the last one does not grow further. */
    private static final List<Supplier<? extends Block>> STAGES = List.of(ModBlocks.SMALL_DEW_CRYSTAL_BUD,
            ModBlocks.MEDIUM_DEW_CRYSTAL_BUD, ModBlocks.LARGE_DEW_CRYSTAL_BUD, ModBlocks.DEW_CRYSTAL_CLUSTER);

    public DewSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DewSourceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROWTH_CHANCE) != 0) {
            return;
        }
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos neighbour = pos.relative(direction);
        BlockState grown = grownState(level.getBlockState(neighbour), direction);
        if (grown != null) {
            level.setBlockAndUpdate(neighbour, grown);
        }
    }

    /**
     * What {@code neighbour} becomes when a crystal grows on the source's {@code direction} side: air or a full water
     * source gets a small bud (waterlogged in water), a dew crystal stage facing the same way gets the next stage;
     * anything else (the cluster, other blocks, a bud facing elsewhere, flowing water) stays: {@code null}.
     */
    @Nullable
    static BlockState grownState(BlockState neighbour, Direction direction) {
        if (BuddingAmethystBlock.canClusterGrowAtState(neighbour)) {
            return ModBlocks.SMALL_DEW_CRYSTAL_BUD.get().defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, direction)
                    .setValue(AmethystClusterBlock.WATERLOGGED, neighbour.getFluidState().getType() == Fluids.WATER);
        }
        for (int i = 0; i < STAGES.size() - 1; i++) {
            if (neighbour.is(STAGES.get(i).get()) && neighbour.getValue(BlockStateProperties.FACING) == direction) {
                return STAGES.get(i + 1).get().withPropertiesOf(neighbour);
            }
        }
        return null;
    }

    /** Drips water below the source while there is room under it (a full block below catches the drops). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0 && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            ParticleUtils.spawnParticleBelow(level, pos, random, ParticleTypes.DRIPPING_WATER);
        }
    }
}
