package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;

/**
 * The hook of {@code WorldCarverLavaMixin} (59): vanilla carvers put lava at and below their lava level (Y −56) without
 * asking the aquifer, which would stand against ring 4's dry bottom. In a ring world the {@link RingAquifer} of the
 * chunk decides instead ({@link RingAquifer#carverLava}); outside ring 4 and its dams that is still vanilla's lava.
 */
public final class CarverLava {
    private CarverLava() {
    }

    /** Whether the chunk's aquifer is a ring world's, which then decides about carver lava. */
    public static boolean decides(Aquifer aquifer) {
        return aquifer instanceof RingAquifer;
    }

    /** What the carver places at {@code pos} instead of lava (null: not cut); only when {@link #decides}. */
    @Nullable
    public static BlockState at(Aquifer aquifer, BlockPos pos) {
        return ((RingAquifer) aquifer).carverLava(new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ()));
    }
}
