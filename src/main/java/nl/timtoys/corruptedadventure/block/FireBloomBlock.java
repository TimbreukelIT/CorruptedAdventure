package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The fire bloom of ring 3: a small flower (bees, suspicious stew) that only grows on ash ground ({@link AshPlantSoil}). */
public class FireBloomBlock extends FlowerBlock {
    public static final MapCodec<FireBloomBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EFFECTS_FIELD.forGetter(FireBloomBlock::getSuspiciousEffects), propertiesCodec())
            .apply(instance, FireBloomBlock::new));

    public FireBloomBlock(Holder<MobEffect> effect, float seconds, Properties properties) {
        super(effect, seconds, properties);
    }

    public FireBloomBlock(SuspiciousStewEffects effects, Properties properties) {
        super(effects, properties);
    }

    @Override
    public MapCodec<FireBloomBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return AshPlantSoil.supports(state);
    }
}
