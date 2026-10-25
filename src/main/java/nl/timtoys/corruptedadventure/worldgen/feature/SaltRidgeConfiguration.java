package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Settings of {@link SaltRidgeFeature}.
 *
 * @param edge           the top block that marks a crack of the crust (the polygon edges of the surface rule)
 * @param ridge          the block stacked on it
 * @param maxY           the highest top block that gets a ridge (the flat crust; pillars and islands stand higher)
 * @param patches        where the ridges lie: the noise …
 * @param patchMin       … at least this
 * @param gap            the chance a crack column stays open (short breaks in the ridge)
 * @param junctionChance the chance a junction (three or four cracks meeting) is two high
 */
public record SaltRidgeConfiguration(BlockState edge, BlockState ridge, int maxY, ResourceKey<NormalNoise.NoiseParameters> patches,
                                     double patchMin, float gap, float junctionChance) implements FeatureConfiguration {
    public static final Codec<SaltRidgeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("edge").forGetter(SaltRidgeConfiguration::edge),
            BlockState.CODEC.fieldOf("ridge").forGetter(SaltRidgeConfiguration::ridge),
            Codec.INT.fieldOf("max_y").forGetter(SaltRidgeConfiguration::maxY),
            ResourceKey.codec(Registries.NOISE).fieldOf("patches").forGetter(SaltRidgeConfiguration::patches),
            Codec.DOUBLE.fieldOf("patch_min").forGetter(SaltRidgeConfiguration::patchMin),
            Codec.floatRange(0.0F, 1.0F).fieldOf("gap").forGetter(SaltRidgeConfiguration::gap),
            Codec.floatRange(0.0F, 1.0F).fieldOf("junction_chance").forGetter(SaltRidgeConfiguration::junctionChance)
    ).apply(instance, SaltRidgeConfiguration::new));
}
