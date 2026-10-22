package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Voidstone (ring 5, 89): black rock that now and then breathes a void mote out of an open face. The mote is vanilla's
 * reverse portal particle as a placeholder until A's {@code void_mote} (golf 2). {@code animateTick} only runs on the
 * client level and names no client class; the stars are emissive in the model, the block gives no light.
 */
public class VoidstoneBlock extends Block {
    public static final MapCodec<VoidstoneBlock> CODEC = simpleCodec(VoidstoneBlock::new);
    /** About one mote per this many display ticks of a visible block. */
    static final int MOTE_CHANCE = 10;

    public VoidstoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VoidstoneBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(MOTE_CHANCE) != 0) {
            return;
        }
        // Like crying obsidian: only out of a face that is open, so buried voidstone stays quiet.
        Direction direction = Direction.getRandom(random);
        BlockPos neighbor = pos.relative(direction);
        if (level.getBlockState(neighbor).isFaceSturdy(level, neighbor, direction.getOpposite())) {
            return;
        }
        double x = pos.getX() + 0.5 + direction.getStepX() * 0.55 + (direction.getStepX() == 0 ? random.nextDouble() - 0.5 : 0.0);
        double y = pos.getY() + 0.5 + direction.getStepY() * 0.55 + (direction.getStepY() == 0 ? random.nextDouble() - 0.5 : 0.0);
        double z = pos.getZ() + 0.5 + direction.getStepZ() * 0.55 + (direction.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0.0);
        level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0, 0.0, 0.0);
    }
}
