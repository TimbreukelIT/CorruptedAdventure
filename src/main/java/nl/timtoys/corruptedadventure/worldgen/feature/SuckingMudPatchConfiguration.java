package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link SuckingMudPatchFeature}.
 *
 * @param width       each side of the patch (blocks, at most {@link SuckingMudPatchFeature#MAX_WIDTH})
 * @param depth       how deep the mud reaches (G1 = A: 2–3)
 * @param replaceable the ground the mud may replace
 * @param biomes      the biomes whose columns the patch may take (it may reach past the bank column's biome)
 * @param bankReach   a bank has water at most this many blocks away, straight along x or z
 * @param bankDrop    and that water's top block lies level with the bank's ground or at most this many blocks lower
 *                    (mud-5b: 4, the high peat rims count too)
 */
public record SuckingMudPatchConfiguration(IntProvider width, IntProvider depth, HolderSet<Block> replaceable,
                                           HolderSet<Biome> biomes, int bankReach, int bankDrop) implements FeatureConfiguration {
    public static final Codec<SuckingMudPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, SuckingMudPatchFeature.MAX_WIDTH).fieldOf("width").forGetter(SuckingMudPatchConfiguration::width),
            IntProvider.codec(1, 4).fieldOf("depth").forGetter(SuckingMudPatchConfiguration::depth),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(SuckingMudPatchConfiguration::replaceable),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(SuckingMudPatchConfiguration::biomes),
            Codec.intRange(1, 4).fieldOf("bank_reach").forGetter(SuckingMudPatchConfiguration::bankReach),
            Codec.intRange(0, 8).fieldOf("bank_drop").forGetter(SuckingMudPatchConfiguration::bankDrop)
    ).apply(instance, SuckingMudPatchConfiguration::new));
}
