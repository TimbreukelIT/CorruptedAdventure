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
import nl.timtoys.corruptedadventure.registry.ModTags;

/** A small flower (bees, suspicious stew) that grows on {@code #corruptedadventure:desert_plant_soil}. */
public class DesertFlowerBlock extends FlowerBlock {
    public static final MapCodec<DesertFlowerBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    EFFECTS_FIELD.forGetter(DesertFlowerBlock::getSuspiciousEffects), propertiesCodec())
            .apply(instance, DesertFlowerBlock::new));

    public DesertFlowerBlock(Holder<MobEffect> effect, float seconds, Properties properties) {
        super(effect, seconds, properties);
    }

    public DesertFlowerBlock(SuspiciousStewEffects effects, Properties properties) {
        super(effects, properties);
    }

    @Override
    public MapCodec<DesertFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.DESERT_PLANT_SOIL);
    }
}
