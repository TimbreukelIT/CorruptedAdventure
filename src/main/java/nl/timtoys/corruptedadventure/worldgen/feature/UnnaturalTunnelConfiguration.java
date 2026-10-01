package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link UnnaturalTunnelFeature}. The length limits keep every tunnel inside the 3×3 chunks a feature may
 * touch (see {@link TunnelPath}).
 *
 * @param firstLength      cells in the first segment
 * @param secondLength     cells after the turn
 * @param turnChance       chance of one right-angle turn
 * @param replaceable      rock the tunnel may cut through; its mouth must enter it
 * @param minSolidFraction share of the tunnel's blocks that must be {@code replaceable} rock (the rest is cave)
 */
public record UnnaturalTunnelConfiguration(
        IntProvider firstLength,
        IntProvider secondLength,
        float turnChance,
        HolderSet<Block> replaceable,
        float minSolidFraction
) implements FeatureConfiguration {
    public static final Codec<UnnaturalTunnelConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, TunnelPath.MAX_FIRST_LENGTH).fieldOf("first_length").forGetter(UnnaturalTunnelConfiguration::firstLength),
            IntProvider.codec(1, TunnelPath.MAX_SECOND_LENGTH).fieldOf("second_length").forGetter(UnnaturalTunnelConfiguration::secondLength),
            Codec.floatRange(0.0F, 1.0F).fieldOf("turn_chance").forGetter(UnnaturalTunnelConfiguration::turnChance),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(UnnaturalTunnelConfiguration::replaceable),
            Codec.floatRange(0.0F, 1.0F).fieldOf("min_solid_fraction").forGetter(UnnaturalTunnelConfiguration::minSolidFraction)
    ).apply(instance, UnnaturalTunnelConfiguration::new));
}
