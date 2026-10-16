package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * A spring on the bottom of ring 4's pools: bubbles rise from it while water stands on it. Only a look (decision golf
 * 1): no current, no pull, nothing on the server. Since golf 2 (package 72) the bubbles are A's {@code updraft_bubble}
 * (it starts in the water, so it pops at the surface) and the spring bubbles audibly ({@code block.bubble_spring.bubble}).
 */
public class BubbleSpringBlock extends Block {
    public static final MapCodec<BubbleSpringBlock> CODEC = simpleCodec(BubbleSpringBlock::new);
    /** One in this many animate ticks tries the bubbling sound; the limiter keeps a pool of springs quiet. */
    private static final int SOUND_CHANCE = 40;
    private static final AmbientSoundLimiter BUBBLING = new AmbientSoundLimiter(20);

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
            level.addAlwaysVisibleParticle(ModParticleTypes.UPDRAFT_BUBBLE.get(), above.getX() + 0.2 + random.nextDouble() * 0.6,
                    above.getY() + random.nextDouble() * 0.2, above.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(SOUND_CHANCE) == 0 && BUBBLING.tryAcquire(level, pos)) {
            level.playLocalSound(above.getX() + 0.5, above.getY() + 0.5, above.getZ() + 0.5,
                    ModSoundEvents.BUBBLE_SPRING_BUBBLE.get(), SoundSource.BLOCKS, 0.5F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }
}
