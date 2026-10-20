package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link IslandDressingFeature}.
 *
 * @param moss          the block that turns an island's underside green (in patches)
 * @param mossCover     the patch noise's value above which the underside turns to {@code moss} (−1 … 1: lower is more)
 * @param hanging       a single block right under the underside (glow lichen, spore blossoms, roots, crystal buds)
 * @param hangingChance per underside column: the chance of {@code hanging}
 * @param strand        the block of a hanging strand (it hangs under a sturdy face and under itself)
 * @param strandChance  per underside column: the chance of a strand
 * @param minLength     the shortest strand
 * @param maxLength     the longest strand
 * @param crystal       a crystal block set into the underside, now and then
 * @param crystalBud    what hangs under it (facing down)
 * @param crystalChance per underside column: the chance of a crystal
 * @param vine          the vine on the island's outer walls (one face set per wall)
 * @param vineChance    per wall column and side: the chance of a curtain of vines down the wall
 * @param vineBelow     how far at most a curtain hangs on below the wall
 */
public record IslandDressingConfiguration(BlockState moss, float mossCover, BlockStateProvider hanging, float hangingChance,
                                          BlockState strand, float strandChance, int minLength, int maxLength,
                                          BlockState crystal, BlockStateProvider crystalBud, float crystalChance,
                                          BlockState vine, float vineChance, int vineBelow) implements FeatureConfiguration {
    public static final Codec<IslandDressingConfiguration> CODEC = RecordCodecBuilder.<IslandDressingConfiguration>create(instance -> instance.group(
            BlockState.CODEC.fieldOf("moss").forGetter(IslandDressingConfiguration::moss),
            Codec.floatRange(-1.0F, 1.0F).fieldOf("moss_cover").forGetter(IslandDressingConfiguration::mossCover),
            BlockStateProvider.CODEC.fieldOf("hanging").forGetter(IslandDressingConfiguration::hanging),
            Codec.floatRange(0.0F, 1.0F).fieldOf("hanging_chance").forGetter(IslandDressingConfiguration::hangingChance),
            BlockState.CODEC.fieldOf("strand").forGetter(IslandDressingConfiguration::strand),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(IslandDressingConfiguration::strandChance),
            Codec.intRange(1, 32).fieldOf("min_length").forGetter(IslandDressingConfiguration::minLength),
            Codec.intRange(1, 32).fieldOf("max_length").forGetter(IslandDressingConfiguration::maxLength),
            BlockState.CODEC.fieldOf("crystal").forGetter(IslandDressingConfiguration::crystal),
            BlockStateProvider.CODEC.fieldOf("crystal_bud").forGetter(IslandDressingConfiguration::crystalBud),
            Codec.floatRange(0.0F, 1.0F).fieldOf("crystal_chance").forGetter(IslandDressingConfiguration::crystalChance),
            BlockState.CODEC.fieldOf("vine").forGetter(IslandDressingConfiguration::vine),
            Codec.floatRange(0.0F, 1.0F).fieldOf("vine_chance").forGetter(IslandDressingConfiguration::vineChance),
            Codec.intRange(0, 16).fieldOf("vine_below").forGetter(IslandDressingConfiguration::vineBelow)
    ).apply(instance, IslandDressingConfiguration::new)).validate(IslandDressingConfiguration::check);

    private static DataResult<IslandDressingConfiguration> check(IslandDressingConfiguration config) {
        if (config.minLength > config.maxLength) {
            return DataResult.error(() -> "island_dressing needs min_length <= max_length");
        }
        return config.hangingChance + config.strandChance + config.crystalChance <= 1.0F ? DataResult.success(config)
                : DataResult.error(() -> "island_dressing needs hanging_chance + strand_chance + crystal_chance <= 1");
    }
}
