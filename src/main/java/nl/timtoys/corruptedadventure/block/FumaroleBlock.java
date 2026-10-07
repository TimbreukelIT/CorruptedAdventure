package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A volcanic vent that puffs steam. Purely visual and client-side ({@code animateTick}, which vanilla only calls on the
 * client); its heat comes from the conditions ({@code #heat_sources}). Vanilla particles until atmosphere delivers
 * {@code steam_puff}.
 */
public class FumaroleBlock extends Block {
    public static final MapCodec<FumaroleBlock> CODEC = simpleCodec(FumaroleBlock::new);

    public FumaroleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FumaroleBlock> codec() {
        return CODEC;
    }

    /**
     * A puff of steam from the vent while it is open (the geyser's rule), now and then with a faint hiss. Normal
     * particles, so the client's particle setting thins them out: worldgen places many fumaroles.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!GeyserBlock.ventOpen(level, pos)) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.1, z,
                    0.0, 0.05 + random.nextDouble() * 0.03, 0.0);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 1.05, z, 0.0, 0.08, 0.0);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.BLOCKS, 0.2F, 0.6F + random.nextFloat() * 0.3F, false);
        }
    }
}
