package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.WaterlilyBlock;

/**
 * The verdant water bloom on still lakes (72b, H2 = A): vanilla's lily pad (floats on a water source, walkable, boats
 * break it) that pops a copy with bonemeal.
 */
public class VerdantWaterBloomBlock extends WaterlilyBlock implements PoppingBonemealable {
    public static final MapCodec<WaterlilyBlock> CODEC = simpleCodec(VerdantWaterBloomBlock::new);

    public VerdantWaterBloomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<WaterlilyBlock> codec() {
        return CODEC;
    }
}
