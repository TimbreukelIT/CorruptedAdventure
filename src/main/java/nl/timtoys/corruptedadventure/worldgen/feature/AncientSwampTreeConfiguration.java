package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link AncientSwampTreeFeature}.
 *
 * @param grid        the landmark cells
 * @param biome       the biome at the anchor
 * @param height      the trunk's height
 * @param branches    how many limbs carry the crown
 * @param log         trunk and limbs (set along their axis)
 * @param roots       the prop roots in open space (waterlogged in water)
 * @param muddyRoots  where a root enters mud
 * @param leaves      the crown
 * @param strand      moss hanging from the crown, in long strands
 * @param tip         a single glowing root under a leaf or limb
 * @param rootDecor   moss and glowcaps on the roots and limbs
 * @param propagule   hanging under the leaves now and then
 */
public record AncientSwampTreeConfiguration(Landmarks.Grid grid, ResourceKey<Biome> biome, IntProvider height, IntProvider branches,
                                            BlockState log, BlockState roots, BlockState muddyRoots, BlockState leaves,
                                            BlockState strand, BlockState tip, BlockStateProvider rootDecor, BlockState propagule)
        implements FeatureConfiguration {
    public static final Codec<AncientSwampTreeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Landmarks.Grid.CODEC.fieldOf("grid").forGetter(AncientSwampTreeConfiguration::grid),
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(AncientSwampTreeConfiguration::biome),
            IntProvider.codec(12, AncientSwampTreeFeature.MAX_HEIGHT).fieldOf("height").forGetter(AncientSwampTreeConfiguration::height),
            IntProvider.codec(2, 10).fieldOf("branches").forGetter(AncientSwampTreeConfiguration::branches),
            BlockState.CODEC.fieldOf("log").forGetter(AncientSwampTreeConfiguration::log),
            BlockState.CODEC.fieldOf("roots").forGetter(AncientSwampTreeConfiguration::roots),
            BlockState.CODEC.fieldOf("muddy_roots").forGetter(AncientSwampTreeConfiguration::muddyRoots),
            BlockState.CODEC.fieldOf("leaves").forGetter(AncientSwampTreeConfiguration::leaves),
            BlockState.CODEC.fieldOf("strand").forGetter(AncientSwampTreeConfiguration::strand),
            BlockState.CODEC.fieldOf("tip").forGetter(AncientSwampTreeConfiguration::tip),
            BlockStateProvider.CODEC.fieldOf("root_decor").forGetter(AncientSwampTreeConfiguration::rootDecor),
            BlockState.CODEC.fieldOf("propagule").forGetter(AncientSwampTreeConfiguration::propagule)
    ).apply(instance, AncientSwampTreeConfiguration::new));
}
