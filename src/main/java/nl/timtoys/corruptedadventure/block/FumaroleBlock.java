package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * A volcanic vent that puffs steam. Purely visual and client-side ({@code animateTick}, which vanilla only calls on the
 * client); its heat comes from the conditions ({@code #heat_sources}).
 */
public class FumaroleBlock extends Block {
    public static final MapCodec<FumaroleBlock> CODEC = simpleCodec(FumaroleBlock::new);
    /** One puff sound per this many ticks per client, nearer vents first, however many fumaroles are near (tuning value). */
    private static final AmbientSoundLimiter PUFF_SOUND = new AmbientSoundLimiter(10);
    /** Volume of the hiss: a background sound, many vents hiss in turn. */
    private static final float PUFF_VOLUME = 0.22F;

    public FumaroleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FumaroleBlock> codec() {
        return CODEC;
    }

    /**
     * A puff of steam from the vent while it is open (the geyser's rule), now and then with a soft hiss. Normal
     * particles, so the client's particle setting thins them out: worldgen places many fumaroles.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!GeyserBlock.ventOpen(level, pos)) {
            return;
        }
        if (random.nextInt(3) != 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            level.addParticle(ModParticleTypes.STEAM_PUFF.get(), x, pos.getY() + 1.05, z,
                    0.0, 0.04 + random.nextDouble() * 0.04, 0.0);
        }
        if (random.nextInt(30) == 0 && PUFF_SOUND.tryAcquire(level, pos)) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ModSoundEvents.FUMAROLE_PUFF.get(),
                    SoundSource.BLOCKS, PUFF_VOLUME, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }
}
