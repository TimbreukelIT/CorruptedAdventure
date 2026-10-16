package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Optional;

/**
 * Settings of {@link LedgeStrandFeature}.
 *
 * @param strand        the block of a strand (it must hang under a sturdy face and under itself)
 * @param accent        a single hanging block right under a ledge, where no strand hangs
 * @param strandChance  per overhang: the chance of a strand
 * @param accentChance  per overhang: the chance of an accent (after the strand's chance)
 * @param minGap        the smallest gap under a ledge that gets a strand (a strand leaves one air block above the floor)
 * @param maxGap        the largest gap a strand fills; under a deeper drop nothing hangs
 * @param reach         how far around the column open sky must reach down to the ledge's underside (no strands in caves)
 * @param tip           the strand's lowest block, if it differs (a vine's tip, full grown so it stays as placed)
 * @param variant       another look of a whole strand (strand and tip, e.g. the flowering vine), if any
 * @param variantChance per strand: the chance of the variant (by position, the same for every placement)
 */
public record LedgeStrandConfiguration(BlockState strand, BlockState accent, float strandChance, float accentChance,
                                       int minGap, int maxGap, int reach, Optional<BlockState> tip,
                                       Optional<Variant> variant, float variantChance) implements FeatureConfiguration {
    public static final Codec<LedgeStrandConfiguration> CODEC = RecordCodecBuilder.<LedgeStrandConfiguration>create(instance -> instance.group(
            BlockState.CODEC.fieldOf("strand").forGetter(LedgeStrandConfiguration::strand),
            BlockState.CODEC.fieldOf("accent").forGetter(LedgeStrandConfiguration::accent),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(LedgeStrandConfiguration::strandChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(LedgeStrandConfiguration::accentChance),
            Codec.intRange(2, 64).fieldOf("min_gap").forGetter(LedgeStrandConfiguration::minGap),
            Codec.intRange(2, 64).fieldOf("max_gap").forGetter(LedgeStrandConfiguration::maxGap),
            Codec.intRange(0, 8).fieldOf("reach").forGetter(LedgeStrandConfiguration::reach),
            BlockState.CODEC.optionalFieldOf("tip").forGetter(LedgeStrandConfiguration::tip),
            Variant.CODEC.optionalFieldOf("variant").forGetter(LedgeStrandConfiguration::variant),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("variant_chance", 0.0F).forGetter(LedgeStrandConfiguration::variantChance)
    ).apply(instance, LedgeStrandConfiguration::new)).validate(LedgeStrandConfiguration::check);

    /** The plain form: one block all the way down, no variant. */
    public LedgeStrandConfiguration(BlockState strand, BlockState accent, float strandChance, float accentChance,
                                    int minGap, int maxGap, int reach) {
        this(strand, accent, strandChance, accentChance, minGap, maxGap, reach, Optional.empty(), Optional.empty(), 0.0F);
    }

    private static DataResult<LedgeStrandConfiguration> check(LedgeStrandConfiguration config) {
        if (config.minGap > config.maxGap) {
            return DataResult.error(() -> "ledge_strand needs min_gap <= max_gap");
        }
        return config.strandChance + config.accentChance <= 1.0F ? DataResult.success(config)
                : DataResult.error(() -> "ledge_strand needs strand_chance + accent_chance <= 1");
    }

    /** A strand's block at {@code y} of a strand ending at {@code bottom}, in the variant or not. */
    BlockState strandAt(int y, int bottom, boolean inVariant) {
        if (inVariant && variant.isPresent()) {
            return y == bottom ? variant.get().tip() : variant.get().strand();
        }
        return y == bottom ? tip.orElse(strand) : strand;
    }

    /**
     * Another look of a whole strand.
     *
     * @param strand the variant's strand block
     * @param tip    the variant's lowest block
     */
    public record Variant(BlockState strand, BlockState tip) {
        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("strand").forGetter(Variant::strand),
                BlockState.CODEC.fieldOf("tip").forGetter(Variant::tip)
        ).apply(instance, Variant::new));
    }
}
