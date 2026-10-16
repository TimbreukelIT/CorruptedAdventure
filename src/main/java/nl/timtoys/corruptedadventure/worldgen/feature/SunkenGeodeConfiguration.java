package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link SunkenGeodeFeature}.
 *
 * @param crystal       the inner layer's block
 * @param budding       the inner layer's budding block (grows the buds)
 * @param buds          the buds and clusters on the inner layer (they face into the hollow; waterlogged)
 * @param middle        the middle layer
 * @param outer         the outer layer
 * @param hollow        the hollow's radius (blocks); the three layers lie around it
 * @param buddingChance per block of the inner layer: the chance of a budding block
 * @param budChance     per face of the inner layer towards the hollow: the chance of a bud
 */
public record SunkenGeodeConfiguration(BlockState crystal, BlockState budding, BlockStateProvider buds, BlockState middle,
                                       BlockState outer, IntProvider hollow, float buddingChance, float budChance)
        implements FeatureConfiguration {
    public static final Codec<SunkenGeodeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("crystal").forGetter(SunkenGeodeConfiguration::crystal),
            BlockState.CODEC.fieldOf("budding").forGetter(SunkenGeodeConfiguration::budding),
            BlockStateProvider.CODEC.fieldOf("buds").forGetter(SunkenGeodeConfiguration::buds),
            BlockState.CODEC.fieldOf("middle").forGetter(SunkenGeodeConfiguration::middle),
            BlockState.CODEC.fieldOf("outer").forGetter(SunkenGeodeConfiguration::outer),
            IntProvider.codec(1, SunkenGeodeFeature.MAX_HOLLOW).fieldOf("hollow").forGetter(SunkenGeodeConfiguration::hollow),
            Codec.floatRange(0.0F, 1.0F).fieldOf("budding_chance").forGetter(SunkenGeodeConfiguration::buddingChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("bud_chance").forGetter(SunkenGeodeConfiguration::budChance)
    ).apply(instance, SunkenGeodeConfiguration::new));
}
