package nl.timtoys.corruptedadventure.block;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A budding crystal block in vanilla's amethyst pattern (ring 4: toxic crystal, aquamarine): a random tick grows a small
 * bud on one of its faces, or the next stage of a bud already there, in air or a water source (waterlogged then).
 * Server-side only, at budding amethyst's pace. The stages are given small to cluster; the cluster grows no further.
 */
public class BuddingCrystalBlock extends AmethystBlock {
    public static final MapCodec<BuddingCrystalBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.BLOCK.byNameCodec().listOf().fieldOf("stages").forGetter(BuddingCrystalBlock::stages),
                    propertiesCodec())
            .apply(instance, (stages, properties) -> new BuddingCrystalBlock(() -> stages, properties)));
    /** A random tick grows a crystal with chance 1 in this many (vanilla budding amethyst's pace). */
    public static final int GROWTH_CHANCE = 5;

    /** Resolved once, on first use: the stages register after this block. */
    private final Supplier<List<Block>> stages;

    public BuddingCrystalBlock(Supplier<List<Block>> stages, Properties properties) {
        super(properties);
        this.stages = Suppliers.memoize(stages::get);
    }

    @Override
    public MapCodec<BuddingCrystalBlock> codec() {
        return CODEC;
    }

    /** The growth stages, small bud first, cluster last. */
    public List<Block> stages() {
        return stages.get();
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
     * What {@code neighbour} becomes when a crystal grows on this block's {@code direction} face: air or a water source
     * gets the small bud (waterlogged in water); a stage facing the same way gets the next one. Anything else (the
     * cluster, a bud facing elsewhere, flowing water, other blocks) stays: {@code null}.
     */
    @Nullable
    BlockState grownState(BlockState neighbour, Direction direction) {
        List<Block> stages = stages();
        if (BuddingAmethystBlock.canClusterGrowAtState(neighbour)) {
            return stages.getFirst().defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, direction)
                    .setValue(AmethystClusterBlock.WATERLOGGED, neighbour.getFluidState().getType() == Fluids.WATER);
        }
        for (int i = 0; i < stages.size() - 1; i++) {
            if (neighbour.is(stages.get(i)) && neighbour.getValue(AmethystClusterBlock.FACING) == direction) {
                return stages.get(i + 1).withPropertiesOf(neighbour);
            }
        }
        return null;
    }
}
