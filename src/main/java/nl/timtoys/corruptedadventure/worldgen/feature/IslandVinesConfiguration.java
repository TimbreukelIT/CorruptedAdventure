package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link IslandVinesFeature}.
 *
 * @param rope          the block of a vine between two islands (it must hold itself: no support needed)
 * @param strand        what hangs under the vine (hangs under a sturdy face and under itself)
 * @param accent        a single hanging block under the vine
 * @param sag           how far the vine sags in its middle per block of span (plus one)
 * @param secondChance  per pair: the chance of a second vine beside the first
 * @param strandChance  per vine block: the chance of a strand under it
 * @param accentChance  per vine block: the chance of an accent under it (where no strand hangs)
 */
public record IslandVinesConfiguration(BlockState rope, BlockState strand, BlockState accent, float sag, float secondChance,
                                       float strandChance, float accentChance) implements FeatureConfiguration {
    public static final Codec<IslandVinesConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("rope").forGetter(IslandVinesConfiguration::rope),
            BlockState.CODEC.fieldOf("strand").forGetter(IslandVinesConfiguration::strand),
            BlockState.CODEC.fieldOf("accent").forGetter(IslandVinesConfiguration::accent),
            Codec.floatRange(0.0F, 1.0F).fieldOf("sag").forGetter(IslandVinesConfiguration::sag),
            Codec.floatRange(0.0F, 1.0F).fieldOf("second_chance").forGetter(IslandVinesConfiguration::secondChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(IslandVinesConfiguration::strandChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(IslandVinesConfiguration::accentChance)
    ).apply(instance, IslandVinesConfiguration::new));
}
