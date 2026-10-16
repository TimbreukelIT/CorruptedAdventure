package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.CoralClawFeature;
import net.minecraft.world.level.levelgen.feature.CoralMushroomFeature;
import net.minecraft.world.level.levelgen.feature.CoralTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Optional;

/**
 * {@code corruptedadventure:local_coral} (package 75, {@code crystal_lagoon}'s living coral): vanilla's warm-ocean coral
 * (tree, claw or mushroom, one in three, a random coral colour) whose blocks stay in the origin's chunk: a coral block
 * is only set at least one block from the chunk's edge, so neither it nor its wall fans reach a neighbour. Vanilla's
 * coral writes up to six blocks into the neighbours, which makes the result depend on the order chunks decorate in
 * (the 23c rule); here a shape near the edge is just cut off.
 */
public final class LocalCoralFeature extends Feature<NoneFeatureConfiguration> {
    public LocalCoralFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int shape = random.nextInt(3);
        Optional<Block> block = BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.CORAL_BLOCKS, random).map(Holder::value);
        if (block.isEmpty()) {
            return false;
        }
        BlockState state = block.get().defaultBlockState();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        return switch (shape) {
            case 0 -> new Tree(chunkX, chunkZ).grow(context.level(), random, origin, state);
            case 1 -> new Claw(chunkX, chunkZ).grow(context.level(), random, origin, state);
            default -> new Mushroom(chunkX, chunkZ).grow(context.level(), random, origin, state);
        };
    }

    /** Whether a coral block at {@code pos} and its wall fans stay inside the chunk. */
    static boolean inside(BlockPos pos, int chunkX, int chunkZ) {
        int x = pos.getX() - (chunkX << 4);
        int z = pos.getZ() - (chunkZ << 4);
        return x >= 1 && x <= 14 && z >= 1 && z <= 14;
    }

    private static final class Tree extends CoralTreeFeature {
        private final int chunkX;
        private final int chunkZ;

        Tree(int chunkX, int chunkZ) {
            super(NoneFeatureConfiguration.CODEC);
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        boolean grow(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
            return placeFeature(level, random, pos, state);
        }

        @Override
        protected boolean placeCoralBlock(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
            return inside(pos, chunkX, chunkZ) && super.placeCoralBlock(level, random, pos, state);
        }
    }

    private static final class Claw extends CoralClawFeature {
        private final int chunkX;
        private final int chunkZ;

        Claw(int chunkX, int chunkZ) {
            super(NoneFeatureConfiguration.CODEC);
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        boolean grow(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
            return placeFeature(level, random, pos, state);
        }

        @Override
        protected boolean placeCoralBlock(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
            return inside(pos, chunkX, chunkZ) && super.placeCoralBlock(level, random, pos, state);
        }
    }

    private static final class Mushroom extends CoralMushroomFeature {
        private final int chunkX;
        private final int chunkZ;

        Mushroom(int chunkX, int chunkZ) {
            super(NoneFeatureConfiguration.CODEC);
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        boolean grow(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
            return placeFeature(level, random, pos, state);
        }

        @Override
        protected boolean placeCoralBlock(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
            return inside(pos, chunkX, chunkZ) && super.placeCoralBlock(level, random, pos, state);
        }
    }
}
