package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link IslandUndersideFeature}.
 *
 * @param strand        a strand's block (it hangs under a sturdy face and under itself; climbable)
 * @param accent        a single block right under the island where no strand hangs (the glowing roots)
 * @param strandChance  per column of an island's underside: the chance of a dangling strand
 * @param minLength     the shortest dangling strand
 * @param maxLength     the longest dangling strand
 * @param climbChance   per column: the chance that the strand reaches down to one block above the ground (only under
 *                      islands no higher than {@code maxClimbGap}: the low islands are climbable)
 * @param maxClimbGap   the largest gap under an island that a climbing strand fills
 * @param accentChance  per column: the chance of an accent (when no strand hangs)
 */
public record IslandUndersideConfiguration(BlockState strand, BlockStateProvider accent, float strandChance, int minLength,
                                           int maxLength, float climbChance, int maxClimbGap, float accentChance)
        implements FeatureConfiguration {
    public static final Codec<IslandUndersideConfiguration> CODEC = RecordCodecBuilder.<IslandUndersideConfiguration>create(instance -> instance.group(
            BlockState.CODEC.fieldOf("strand").forGetter(IslandUndersideConfiguration::strand),
            BlockStateProvider.CODEC.fieldOf("accent").forGetter(IslandUndersideConfiguration::accent),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(IslandUndersideConfiguration::strandChance),
            Codec.intRange(1, 32).fieldOf("min_length").forGetter(IslandUndersideConfiguration::minLength),
            Codec.intRange(1, 32).fieldOf("max_length").forGetter(IslandUndersideConfiguration::maxLength),
            Codec.floatRange(0.0F, 1.0F).fieldOf("climb_chance").forGetter(IslandUndersideConfiguration::climbChance),
            Codec.intRange(2, 128).fieldOf("max_climb_gap").forGetter(IslandUndersideConfiguration::maxClimbGap),
            Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(IslandUndersideConfiguration::accentChance)
    ).apply(instance, IslandUndersideConfiguration::new)).validate(IslandUndersideConfiguration::check);

    private static DataResult<IslandUndersideConfiguration> check(IslandUndersideConfiguration config) {
        if (config.minLength > config.maxLength) {
            return DataResult.error(() -> "island_underside needs min_length <= max_length");
        }
        return config.strandChance + config.climbChance + config.accentChance <= 1.0F ? DataResult.success(config)
                : DataResult.error(() -> "island_underside needs strand_chance + climb_chance + accent_chance <= 1");
    }
}
