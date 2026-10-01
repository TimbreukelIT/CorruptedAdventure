package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * Palm sapling: a vanilla sapling that also grows on {@code #corruptedadventure:desert_plant_soil} (sand, terracotta,
 * cracked clay), the ground rule of all ring 2 plants. Sand has no {@code canSustainPlant} override, so NeoForge's
 * {@code BushBlock.canSurvive} falls back to {@link #mayPlaceOn}.
 */
public class PalmSaplingBlock extends SaplingBlock {
    /** Own codec: {@code SaplingBlock.CODEC} would create a plain {@link SaplingBlock}. */
    public static final MapCodec<PalmSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    TreeGrower.CODEC.fieldOf("tree").forGetter(block -> block.treeGrower), propertiesCodec())
            .apply(instance, PalmSaplingBlock::new));

    public PalmSaplingBlock(TreeGrower treeGrower, Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    public MapCodec<PalmSaplingBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.is(ModTags.Blocks.DESERT_PLANT_SOIL);
    }
}
