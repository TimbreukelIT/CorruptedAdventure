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
 * Settings of {@link HornitoFeature}.
 *
 * @param body   the cone's rock
 * @param cap    the glowing top of the vent (and the throat below it)
 * @param height the cone's height above the ground (2–{@link HornitoFeature#MAX_HEIGHT})
 * @param ground the ground the cone may stand on
 */
public record HornitoConfiguration(BlockStateProvider body, BlockStateProvider cap, IntProvider height, HolderSet<Block> ground)
        implements FeatureConfiguration {
    public static final Codec<HornitoConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("body").forGetter(HornitoConfiguration::body),
            BlockStateProvider.CODEC.fieldOf("cap").forGetter(HornitoConfiguration::cap),
            IntProvider.codec(2, HornitoFeature.MAX_HEIGHT).fieldOf("height").forGetter(HornitoConfiguration::height),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(HornitoConfiguration::ground)
    ).apply(instance, HornitoConfiguration::new));
}
