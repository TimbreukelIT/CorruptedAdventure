package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

/**
 * Configuration of {@code corruptedadventure:surface_fossil} ({@link FossilLayout}).
 *
 * @param templates structure templates to pick from (vanilla's {@code minecraft:fossil/spine_1} … {@code skull_4});
 *                  only their bone blocks are placed
 * @param buried    the share of the template's height below the lowest ground of its footprint
 * @param integrity the chance each bone block is kept (weathered, as vanilla's block rot processor)
 * @param ground    the top block of every column under the fossil must be one of these
 */
public record SurfaceFossilConfiguration(List<ResourceLocation> templates, FloatProvider buried, float integrity,
                                         HolderSet<Block> ground) implements FeatureConfiguration {
    public static final Codec<SurfaceFossilConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("templates").forGetter(SurfaceFossilConfiguration::templates),
            FloatProvider.codec(0.0F, 1.0F).fieldOf("buried").forGetter(SurfaceFossilConfiguration::buried),
            Codec.floatRange(0.0F, 1.0F).fieldOf("integrity").forGetter(SurfaceFossilConfiguration::integrity),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(SurfaceFossilConfiguration::ground)
    ).apply(instance, SurfaceFossilConfiguration::new));
}
