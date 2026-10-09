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
 * Settings of {@link GroundPatchFeature}.
 *
 * @param material    the new ground blocks
 * @param replaceable the ground the patch may replace (other ground stays: the patch follows the terrain)
 * @param radius      the half width before stretching, 1–{@link GroundPatchFeature#MAX_RADIUS}
 * @param stretch     how much longer than wide (1 = round), along a random direction
 * @param depth       how many blocks deep the ground is replaced
 * @param raise       layers stacked on top of the core (0 = flat); each layer covers less
 * @param raised      the material of the raised layers
 */
public record GroundPatchConfiguration(BlockStateProvider material, HolderSet<Block> replaceable, IntProvider radius, float stretch,
                                       IntProvider depth, IntProvider raise, BlockStateProvider raised) implements FeatureConfiguration {
    public static final Codec<GroundPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("material").forGetter(GroundPatchConfiguration::material),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(GroundPatchConfiguration::replaceable),
            IntProvider.codec(1, GroundPatchFeature.MAX_RADIUS).fieldOf("radius").forGetter(GroundPatchConfiguration::radius),
            Codec.floatRange(1.0F, 3.0F).optionalFieldOf("stretch", 1.0F).forGetter(GroundPatchConfiguration::stretch),
            IntProvider.codec(1, 4).fieldOf("depth").forGetter(GroundPatchConfiguration::depth),
            IntProvider.codec(0, 3).optionalFieldOf("raise", net.minecraft.util.valueproviders.ConstantInt.ZERO).forGetter(GroundPatchConfiguration::raise),
            BlockStateProvider.CODEC.fieldOf("raised").forGetter(GroundPatchConfiguration::raised)
    ).apply(instance, GroundPatchConfiguration::new));
}
