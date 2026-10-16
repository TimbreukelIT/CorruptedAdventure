package nl.timtoys.corruptedadventure.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/**
 * The toxic pool of ring 4 (contract ring 4 §3, package 72): its own fluid, slow and short like lava in the overworld
 * ({@code ModFluids}). Its tell next to the safe toxic-green water: toxic bubbles that rise from a still pool and pop at
 * its surface ({@code animateTick}, client level, no client class). Mixing with water and lava: {@link ToxicPoolInteractions}.
 */
public abstract class ToxicPoolFluid extends BaseFlowingFluid {
    /** One in this many animate ticks of a source with air above starts a bubble: a pool has tens of blocks. */
    private static final int BUBBLE_CHANCE = 6;

    protected ToxicPoolFluid(Properties properties) {
        super(properties);
    }

    @Override
    protected void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (!state.isSource() || random.nextInt(BUBBLE_CHANCE) != 0 || !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        // Starts just under the surface, so the particle pops when it leaves the fluid.
        level.addParticle(ModParticleTypes.TOXIC_BUBBLE.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 0.6 + random.nextDouble() * 0.2, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
    }

    /** The still pool. */
    public static class Source extends ToxicPoolFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    /** The flowing edge (about three blocks, like lava in the overworld). */
    public static class Flowing extends ToxicPoolFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
