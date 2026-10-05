package nl.timtoys.corruptedadventure.worldgen.carver;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.NetherWorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.function.Function;

/**
 * {@code corruptedadventure:ring_nether_cave} (change 35): vanilla's Nether cave carver, but what it carves into comes from
 * the chunk's aquifer (the fluid picker of the generator, {@code RingFluidPicker}) instead of lava up to a hardcoded
 * {@code minGenY + 31}. In rings with the dimension's default lava sea (ring 1) that is the same block for block; in Nether
 * ring 2 it keeps the lower lava sea (Y 22) free of lava-filled tunnels above it.
 */
public final class RingNetherCarver extends NetherWorldCarver {
    public RingNetherCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean carveBlock(CarvingContext context, CaveCarverConfiguration config, ChunkAccess chunk,
                                 Function<BlockPos, Holder<Biome>> biomeGetter, CarvingMask mask, BlockPos.MutableBlockPos pos,
                                 BlockPos.MutableBlockPos checkPos, Aquifer aquifer, MutableBoolean reachedSurface) {
        if (!canReplaceBlock(config, chunk.getBlockState(pos))) {
            return false;
        }
        BlockState state = aquifer.computeSubstance(new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ()), 0.0);
        if (state == null) {
            return false;
        }
        chunk.setBlockState(pos, state.isAir() ? CAVE_AIR : state, false);
        return true;
    }
}
