package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;
import java.util.Optional;

/**
 * Configuration of {@code corruptedadventure:outcrop} ({@link OutcropShape}).
 *
 * @param knobs          number of knobs
 * @param spread         how far the other knobs lie from the first, blocks
 * @param radius         base radius per knob
 * @param height         height of the highest knob above the ground at its centre
 * @param taper          the fraction of its base radius a knob keeps at the top
 * @param sink           ground blocks per column that become rock
 * @param overhangChance chance of a collar (shade) at the top of the highest knob
 * @param palettes       per rock one palette by weight: its blocks lie in horizontal bands of 2–3, counted from the
 *                       rock's lowest block from a random entry (each rock its own offset)
 * @param topDecoration  on the top of each knob, e.g. a salt crystal
 */
public record OutcropConfiguration(IntProvider knobs, float spread, FloatProvider radius, IntProvider height, float taper, int sink,
                                   float overhangChance, SimpleWeightedRandomList<List<BlockState>> palettes,
                                   Optional<SurfacePoolConfiguration.Scatter> topDecoration) implements FeatureConfiguration {
    public static final Codec<OutcropConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, 6).fieldOf("knobs").forGetter(OutcropConfiguration::knobs),
            Codec.floatRange(0.0F, 6.0F).fieldOf("spread").forGetter(OutcropConfiguration::spread),
            FloatProvider.codec(0.5F, 4.0F).fieldOf("radius").forGetter(OutcropConfiguration::radius),
            IntProvider.codec(1, 16).fieldOf("height").forGetter(OutcropConfiguration::height),
            Codec.floatRange(0.0F, 0.9F).fieldOf("taper").forGetter(OutcropConfiguration::taper),
            Codec.intRange(0, 3).fieldOf("sink").forGetter(OutcropConfiguration::sink),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("overhang_chance", 0.0F).forGetter(OutcropConfiguration::overhangChance),
            SimpleWeightedRandomList.wrappedCodec(BlockState.CODEC.listOf(1, 8)).fieldOf("palettes").forGetter(OutcropConfiguration::palettes),
            SurfacePoolConfiguration.Scatter.CODEC.optionalFieldOf("top_decoration").forGetter(OutcropConfiguration::topDecoration)
    ).apply(instance, OutcropConfiguration::new));
}
