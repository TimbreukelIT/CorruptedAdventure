package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MudBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Boiling mud: vanilla mud's shape (you sink in two pixels) with the slowness of honey (speed and jump factor in its
 * properties, so the server moves entities and the client predicts the same). Bubbles are client-side
 * ({@code animateTick}); the heat comes from the conditions ({@code #heat_sources}).
 */
public class BoilingMudBlock extends MudBlock {
    public static final MapCodec<MudBlock> CODEC = simpleCodec(BoilingMudBlock::new);
    /** One bubble sound per this many ticks per client, nearer blocks first: a mud pool has hundreds of blocks (tuning value). */
    private static final AmbientSoundLimiter BUBBLE_SOUND = new AmbientSoundLimiter(5);

    public BoilingMudBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MudBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
        if (random.nextInt(60) == 0 && BUBBLE_SOUND.tryAcquire(level, pos)) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ModSoundEvents.BOILING_MUD_BUBBLE.get(),
                    SoundSource.BLOCKS, 0.4F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }
}
