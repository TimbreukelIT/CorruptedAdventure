package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.timtoys.corruptedadventure.block.GiantLilyPadBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * {@code corruptedadventure:giant_lily_pad} (package 75): the whole 2 × 2 giant lily pad (contract ring 4 §3: worldgen
 * places the whole) with its north-west corner at the origin, on still water: each of the four spots is air over a
 * water source and lies in the origin's chunk. Otherwise nothing. Writes only in its own chunk.
 */
public final class GiantLilyPadFeature extends Feature<NoneFeatureConfiguration> {
    public GiantLilyPadFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos northWest = context.origin();
        if ((northWest.getX() & 15) == 15 || (northWest.getZ() & 15) == 15) {
            return false;
        }
        BlockState pad = ModBlocks.GIANT_LILY_PAD.get().defaultBlockState();
        for (GiantLilyPadBlock.Part part : GiantLilyPadBlock.Part.values()) {
            BlockPos pos = part.from(northWest);
            if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos.below()).isSource()
                    || !pad.canSurvive(level, pos)) {
                return false;
            }
        }
        for (GiantLilyPadBlock.Part part : GiantLilyPadBlock.Part.values()) {
            level.setBlock(part.from(northWest), pad.setValue(GiantLilyPadBlock.PART, part), Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
