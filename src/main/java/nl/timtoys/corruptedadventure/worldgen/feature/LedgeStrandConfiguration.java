package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link LedgeStrandFeature}.
 *
 * @param strand       the block of a strand (it must hang under a sturdy face and under itself)
 * @param accent       a single hanging block right under a ledge, where no strand hangs
 * @param strandChance per overhang: the chance of a strand
 * @param accentChance per overhang: the chance of an accent (after the strand's chance)
 * @param minGap       the smallest gap under a ledge that gets a strand (a strand leaves one air block above the floor)
 * @param maxGap       the largest gap a strand fills; under a deeper drop nothing hangs
 * @param reach        how far around the column open sky must reach down to the ledge's underside (no strands in caves)
 */
public record LedgeStrandConfiguration(BlockState strand, BlockState accent, float strandChance, float accentChance,
                                       int minGap, int maxGap, int reach) implements FeatureConfiguration {
    public static final Codec<LedgeStrandConfiguration> CODEC = RecordCodecBuilder.<LedgeStrandConfiguration>create(instance -> instance.group(
            BlockState.CODEC.fieldOf("strand").forGetter(LedgeStrandConfiguration::strand),
            BlockState.CODEC.fieldOf("accent").forGetter(LedgeStrandConfiguration::accent),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(LedgeStrandConfiguration::strandChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(LedgeStrandConfiguration::accentChance),
            Codec.intRange(2, 64).fieldOf("min_gap").forGetter(LedgeStrandConfiguration::minGap),
            Codec.intRange(2, 64).fieldOf("max_gap").forGetter(LedgeStrandConfiguration::maxGap),
            Codec.intRange(0, 8).fieldOf("reach").forGetter(LedgeStrandConfiguration::reach)
    ).apply(instance, LedgeStrandConfiguration::new)).validate(LedgeStrandConfiguration::check);

    private static DataResult<LedgeStrandConfiguration> check(LedgeStrandConfiguration config) {
        if (config.minGap > config.maxGap) {
            return DataResult.error(() -> "ledge_strand needs min_gap <= max_gap");
        }
        return config.strandChance + config.accentChance <= 1.0F ? DataResult.success(config)
                : DataResult.error(() -> "ledge_strand needs strand_chance + accent_chance <= 1");
    }
}
