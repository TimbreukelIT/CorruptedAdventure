package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link CrystalSpireFeature}.
 *
 * @param grid        the landmark cells ({@link Landmarks.Grid})
 * @param biome       the biome around the whole group (with {@link CrystalSpireFeature#BIOME_MARGIN} to spare)
 * @param height      the main spire's height over the water
 * @param radius      the main spire's half-width at its foot
 * @param companions  how many smaller spires lean out around it
 * @param core        inside the spires
 * @param shell       their outer blocks (the lower part of the spire turns to {@code base} by and by)
 * @param base        under and just over the water, and in streaks up the lower half
 * @param glow        light in the shell, with {@code glowChance} per shell block
 * @param glowChance  per shell block over the water
 * @param buds        crystals on the faces, set facing out (and waterlogged in water), with {@code budChance}
 * @param budChance   per open face
 */
public record CrystalSpireConfiguration(Landmarks.Grid grid, ResourceKey<Biome> biome, IntProvider height, FloatProvider radius,
                                        IntProvider companions, BlockState core, BlockStateProvider shell, BlockStateProvider base,
                                        BlockState glow, float glowChance, BlockStateProvider buds, float budChance)
        implements FeatureConfiguration {
    public static final Codec<CrystalSpireConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Landmarks.Grid.CODEC.fieldOf("grid").forGetter(CrystalSpireConfiguration::grid),
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(CrystalSpireConfiguration::biome),
            IntProvider.codec(4, CrystalSpireFeature.MAX_HEIGHT).fieldOf("height").forGetter(CrystalSpireConfiguration::height),
            FloatProvider.codec(1.0F, CrystalSpireFeature.MAX_RADIUS).fieldOf("radius").forGetter(CrystalSpireConfiguration::radius),
            IntProvider.codec(0, 4).fieldOf("companions").forGetter(CrystalSpireConfiguration::companions),
            BlockState.CODEC.fieldOf("core").forGetter(CrystalSpireConfiguration::core),
            BlockStateProvider.CODEC.fieldOf("shell").forGetter(CrystalSpireConfiguration::shell),
            BlockStateProvider.CODEC.fieldOf("base").forGetter(CrystalSpireConfiguration::base),
            BlockState.CODEC.fieldOf("glow").forGetter(CrystalSpireConfiguration::glow),
            Codec.floatRange(0.0F, 1.0F).fieldOf("glow_chance").forGetter(CrystalSpireConfiguration::glowChance),
            BlockStateProvider.CODEC.fieldOf("buds").forGetter(CrystalSpireConfiguration::buds),
            Codec.floatRange(0.0F, 1.0F).fieldOf("bud_chance").forGetter(CrystalSpireConfiguration::budChance)
    ).apply(instance, CrystalSpireConfiguration::new));
}
