package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link BarePatchFeature}.
 *
 * @param radius          patch radius before the ragged edge takes its bite; at most {@link BarePatchFeature#MAX_RADIUS}
 * @param edgeJitter      how deep the edge may cut in, as a fraction of the radius (0 = circle)
 * @param surface         new top block where the ground is open to the sky
 * @param subsurface      the blocks below it, and the top block under a trunk
 * @param subsurfaceDepth how many {@code #dirt} blocks below the top become {@code subsurface}
 * @param replaceable     ground the patch may wither; other ground (sand, stone, water) is left alone
 * @param deadLog         what logs of standing trees turn into (axis kept)
 * @param trees           feature for the patch's own dead trees
 * @param treeCount       how many of those trees it tries to place
 * @param deadBushChance  chance per new surface block of a dead bush on top
 */
public record BarePatchConfiguration(
        IntProvider radius,
        float edgeJitter,
        BlockStateProvider surface,
        BlockStateProvider subsurface,
        IntProvider subsurfaceDepth,
        HolderSet<Block> replaceable,
        Block deadLog,
        Holder<ConfiguredFeature<?, ?>> trees,
        IntProvider treeCount,
        float deadBushChance
) implements FeatureConfiguration {
    public static final Codec<BarePatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, BarePatchFeature.MAX_RADIUS).fieldOf("radius").forGetter(BarePatchConfiguration::radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("edge_jitter").forGetter(BarePatchConfiguration::edgeJitter),
            BlockStateProvider.CODEC.fieldOf("surface").forGetter(BarePatchConfiguration::surface),
            BlockStateProvider.CODEC.fieldOf("subsurface").forGetter(BarePatchConfiguration::subsurface),
            IntProvider.codec(0, 4).fieldOf("subsurface_depth").forGetter(BarePatchConfiguration::subsurfaceDepth),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(BarePatchConfiguration::replaceable),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("dead_log").forGetter(BarePatchConfiguration::deadLog),
            ConfiguredFeature.CODEC.fieldOf("trees").forGetter(BarePatchConfiguration::trees),
            IntProvider.codec(0, 4).fieldOf("tree_count").forGetter(BarePatchConfiguration::treeCount),
            Codec.floatRange(0.0F, 1.0F).fieldOf("dead_bush_chance").forGetter(BarePatchConfiguration::deadBushChance)
    ).apply(instance, BarePatchConfiguration::new));
}
