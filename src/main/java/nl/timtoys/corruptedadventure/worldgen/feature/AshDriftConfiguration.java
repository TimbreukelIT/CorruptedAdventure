package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * Settings of {@link AshDriftFeature}.
 *
 * @param layer      a layered block (vanilla's snow layer behaviour, {@code layers} 1–8)
 * @param ground     the blocks a drift may lie on
 * @param noise      the patchiness of the drifts (world noise, {@code y} 0)
 * @param noiseScale xz scale of {@code noise}
 * @param windAngle  the wind direction in radians (the drifts pile up in the lee, downwind of higher ground)
 * @param biomes     where drifts lie and how thick (per column, by the column's biome)
 */
public record AshDriftConfiguration(Block layer, HolderSet<Block> ground, ResourceKey<NormalNoise.NoiseParameters> noise,
                                    double noiseScale, double windAngle, List<BiomeAmount> biomes) implements FeatureConfiguration {
    public static final Codec<AshDriftConfiguration> CODEC = RecordCodecBuilder.<AshDriftConfiguration>create(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("layer").forGetter(AshDriftConfiguration::layer),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(AshDriftConfiguration::ground),
            ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(AshDriftConfiguration::noise),
            Codec.doubleRange(1.0E-4, 16.0).fieldOf("noise_scale").forGetter(AshDriftConfiguration::noiseScale),
            Codec.DOUBLE.fieldOf("wind_angle").forGetter(AshDriftConfiguration::windAngle),
            BiomeAmount.CODEC.listOf().fieldOf("biomes").forGetter(AshDriftConfiguration::biomes)
    ).apply(instance, AshDriftConfiguration::new)).validate(AshDriftConfiguration::validate);

    private static DataResult<AshDriftConfiguration> validate(AshDriftConfiguration config) {
        return config.layer.defaultBlockState().hasProperty(SnowLayerBlock.LAYERS) ? DataResult.success(config)
                : DataResult.error(() -> "ash_drift: " + BuiltInRegistries.BLOCK.getKey(config.layer) + " has no layers");
    }

    /**
     * @param amount   scale of the drifts (1 = the full wind drift)
     * @param flatOnly only on flat ground (relief ≤ 2 within 2 blocks), e.g. the level parts of the peaks
     * @param steps    whether the one-block steps of the plain get a ramp of layers against their riser
     */
    public record BiomeAmount(ResourceKey<Biome> biome, float amount, boolean flatOnly, boolean steps) {
        public static final Codec<BiomeAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(BiomeAmount::biome),
                Codec.floatRange(0.0F, 2.0F).fieldOf("amount").forGetter(BiomeAmount::amount),
                Codec.BOOL.optionalFieldOf("flat_only", false).forGetter(BiomeAmount::flatOnly),
                Codec.BOOL.optionalFieldOf("steps", true).forGetter(BiomeAmount::steps)
        ).apply(instance, BiomeAmount::new));
    }
}
