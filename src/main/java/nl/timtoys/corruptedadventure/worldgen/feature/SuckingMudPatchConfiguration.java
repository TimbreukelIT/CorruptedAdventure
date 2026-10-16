package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Settings of {@link SuckingMudPatchFeature}.
 *
 * @param width       each side of the patch (blocks, at most {@link SuckingMudPatchFeature#MAX_WIDTH})
 * @param depth       how deep the mud reaches (G1 = A: 2–3)
 * @param replaceable the ground the mud may replace
 */
public record SuckingMudPatchConfiguration(IntProvider width, IntProvider depth, HolderSet<Block> replaceable) implements FeatureConfiguration {
    public static final Codec<SuckingMudPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, SuckingMudPatchFeature.MAX_WIDTH).fieldOf("width").forGetter(SuckingMudPatchConfiguration::width),
            IntProvider.codec(1, 4).fieldOf("depth").forGetter(SuckingMudPatchConfiguration::depth),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(SuckingMudPatchConfiguration::replaceable)
    ).apply(instance, SuckingMudPatchConfiguration::new));
}
