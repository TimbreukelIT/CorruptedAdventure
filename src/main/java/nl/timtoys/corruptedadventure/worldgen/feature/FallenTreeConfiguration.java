package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link FallenTreeFeature}.
 *
 * @param log         the wood (a log with an axis: upright in the stump, lying in the trunk)
 * @param stumpHeight the stump's height
 * @param length      the fallen trunk's length (≤ {@link FallenTreeFeature#MAX_LENGTH})
 * @param stumpOnly   the chance of a stump without a trunk
 * @param trunkOnly   the chance of a trunk without a stump (after {@code stumpOnly}; the rest gets both)
 * @param decor       what grows on the wood (moss, mushrooms), on top of a log
 * @param decorChance per log: the chance of {@code decor} on it (only in open air)
 */
public record FallenTreeConfiguration(BlockState log, IntProvider stumpHeight, IntProvider length, float stumpOnly, float trunkOnly,
                                      BlockStateProvider decor, float decorChance) implements FeatureConfiguration {
    public static final Codec<FallenTreeConfiguration> CODEC = RecordCodecBuilder.<FallenTreeConfiguration>create(instance -> instance.group(
            BlockState.CODEC.fieldOf("log").forGetter(FallenTreeConfiguration::log),
            IntProvider.codec(1, 4).fieldOf("stump_height").forGetter(FallenTreeConfiguration::stumpHeight),
            IntProvider.codec(2, FallenTreeFeature.MAX_LENGTH).fieldOf("length").forGetter(FallenTreeConfiguration::length),
            Codec.floatRange(0.0F, 1.0F).fieldOf("stump_only").forGetter(FallenTreeConfiguration::stumpOnly),
            Codec.floatRange(0.0F, 1.0F).fieldOf("trunk_only").forGetter(FallenTreeConfiguration::trunkOnly),
            BlockStateProvider.CODEC.fieldOf("decor").forGetter(FallenTreeConfiguration::decor),
            Codec.floatRange(0.0F, 1.0F).fieldOf("decor_chance").forGetter(FallenTreeConfiguration::decorChance)
    ).apply(instance, FallenTreeConfiguration::new)).validate(config -> config.stumpOnly + config.trunkOnly <= 1.0F
            ? DataResult.success(config) : DataResult.error(() -> "fallen_tree needs stump_only + trunk_only <= 1"));
}
