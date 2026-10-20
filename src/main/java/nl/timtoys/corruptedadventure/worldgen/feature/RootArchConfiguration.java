package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;

/**
 * Settings of {@link RootArchFeature} (the landmark arches, with a {@code grid}) and {@link RootTangleFeature} (the
 * small tangles, without).
 *
 * @param grid          the landmark cells (arches only)
 * @param biome         the biome of the anchor and both feet (arches only)
 * @param roots         the wood of the arch (waterlogged where it stands in water)
 * @param topDecor      on top of the arch, with {@code topChance} per open top
 * @param topChance     per top block
 * @param strand        hanging under the arch, with {@code strandChance}, up to {@code maxStrand} long
 * @param strandChance  per open underside block over air
 * @param maxStrand     the longest strand
 * @param tip           a single block instead of a strand (a glowing root), with {@code tipChance}
 * @param tipChance     per strand that hangs
 */
public record RootArchConfiguration(Optional<Landmarks.Grid> grid, Optional<ResourceKey<Biome>> biome, BlockState roots,
                                    BlockStateProvider topDecor, float topChance, BlockState strand, float strandChance, int maxStrand,
                                    BlockState tip, float tipChance) implements FeatureConfiguration {
    public static final Codec<RootArchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Landmarks.Grid.CODEC.optionalFieldOf("grid").forGetter(RootArchConfiguration::grid),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(RootArchConfiguration::biome),
            BlockState.CODEC.fieldOf("roots").forGetter(RootArchConfiguration::roots),
            BlockStateProvider.CODEC.fieldOf("top_decor").forGetter(RootArchConfiguration::topDecor),
            Codec.floatRange(0.0F, 1.0F).fieldOf("top_chance").forGetter(RootArchConfiguration::topChance),
            BlockState.CODEC.fieldOf("strand").forGetter(RootArchConfiguration::strand),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strand_chance").forGetter(RootArchConfiguration::strandChance),
            Codec.intRange(1, 12).fieldOf("max_strand").forGetter(RootArchConfiguration::maxStrand),
            BlockState.CODEC.fieldOf("tip").forGetter(RootArchConfiguration::tip),
            Codec.floatRange(0.0F, 1.0F).fieldOf("tip_chance").forGetter(RootArchConfiguration::tipChance)
    ).apply(instance, RootArchConfiguration::new));
}
