package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

/**
 * Settings of {@link AshCastFeature}.
 *
 * @param templates the cast templates (one figure each, facing +x in the template)
 * @param count     how many figures stand in the group
 * @param spread    how far the others stand from the first
 * @param ground    the ground a figure may stand on
 * @param materials the blocks of a template that are placed (the figure's ash; anything else in it is left out)
 */
public record AshCastConfiguration(List<ResourceLocation> templates, IntProvider count, int spread, HolderSet<Block> ground,
                                   HolderSet<Block> materials)
        implements FeatureConfiguration {
    public static final Codec<AshCastConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("templates").forGetter(AshCastConfiguration::templates),
            IntProvider.codec(1, 4).fieldOf("count").forGetter(AshCastConfiguration::count),
            Codec.intRange(0, 6).fieldOf("spread").forGetter(AshCastConfiguration::spread),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(AshCastConfiguration::ground),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("materials").forGetter(AshCastConfiguration::materials)
    ).apply(instance, AshCastConfiguration::new));
}
