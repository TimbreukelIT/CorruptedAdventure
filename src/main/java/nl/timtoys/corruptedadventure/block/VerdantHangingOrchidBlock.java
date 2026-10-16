package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HangingRootsBlock;

/**
 * The hanging orchid under ledges, branches and crowns (72b): vanilla's hanging roots (hangs under a sturdy underside,
 * waterloggable, drops without it) that pops a copy with bonemeal.
 */
public class VerdantHangingOrchidBlock extends HangingRootsBlock implements PoppingBonemealable {
    public static final MapCodec<HangingRootsBlock> CODEC = simpleCodec(VerdantHangingOrchidBlock::new);

    public VerdantHangingOrchidBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<HangingRootsBlock> codec() {
        return CODEC;
    }
}
