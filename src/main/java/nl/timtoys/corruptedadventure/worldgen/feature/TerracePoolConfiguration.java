package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link TerracePoolFeature}.
 *
 * @param fill   what boils in the pool (one draw per pool: every pool has one colour)
 * @param lip    the raised lip around the pool (travertine)
 * @param terrace the steps around the lip, from the lip outwards
 * @param radius the pool's radius (before the ragged edge)
 * @param ground the ground the pool and its terraces may replace
 */
public record TerracePoolConfiguration(BlockStateProvider fill, BlockStateProvider lip, BlockStateProvider terrace, IntProvider radius,
                                       HolderSet<Block> ground) implements FeatureConfiguration {
    public static final Codec<TerracePoolConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("fill").forGetter(TerracePoolConfiguration::fill),
            BlockStateProvider.CODEC.fieldOf("lip").forGetter(TerracePoolConfiguration::lip),
            BlockStateProvider.CODEC.fieldOf("terrace").forGetter(TerracePoolConfiguration::terrace),
            IntProvider.codec(1, TerracePoolFeature.MAX_RADIUS).fieldOf("radius").forGetter(TerracePoolConfiguration::radius),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(TerracePoolConfiguration::ground)
    ).apply(instance, TerracePoolConfiguration::new));
}
