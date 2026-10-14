package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A spring on the bottom of ring 4's pools: bubbles rise from it while water stands on it. Only a look (decision golf
 * 1): no current, no pull, nothing on the server. Vanilla's bubble column particle (it dies outside water) until
 * atmosphere delivers its own; the bubbling sound follows in wave 2.
 */
public class BubbleSpringBlock extends Block {
    public static final MapCodec<BubbleSpringBlock> CODEC = simpleCodec(BubbleSpringBlock::new);

    public BubbleSpringBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BubbleSpringBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (!level.getFluidState(above).is(FluidTags.WATER)) {
            return;
        }
        int bubbles = 1 + random.nextInt(3);
        for (int i = 0; i < bubbles; i++) {
            level.addAlwaysVisibleParticle(ParticleTypes.BUBBLE_COLUMN_UP, above.getX() + 0.2 + random.nextDouble() * 0.6,
                    above.getY() + random.nextDouble() * 0.2, above.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.04, 0.0);
        }
    }
}
