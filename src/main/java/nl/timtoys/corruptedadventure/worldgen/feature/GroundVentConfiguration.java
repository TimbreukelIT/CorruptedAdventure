package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

/**
 * Settings of {@link GroundVentFeature}.
 *
 * @param vent        the vent block in the ground's top
 * @param collar      the ring around it, in the ground's top
 * @param collarRadius radius of the ring (1–{@link GroundVentFeature#MAX_COLLAR})
 * @param collarChance share of the ring's blocks that change (a ragged ring)
 * @param ground      the top blocks vent and collar may replace
 * @param below       what lies under the vent: the first rule that matches a block wins
 * @param belowCount  how many blocks under the vent and its inner ring follow {@code below}
 */
public record GroundVentConfiguration(BlockState vent, BlockStateProvider collar, IntProvider collarRadius, float collarChance,
                                      HolderSet<Block> ground, List<OreConfiguration.TargetBlockState> below,
                                      IntProvider belowCount) implements FeatureConfiguration {
    public static final Codec<GroundVentConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("vent").forGetter(GroundVentConfiguration::vent),
            BlockStateProvider.CODEC.fieldOf("collar").forGetter(GroundVentConfiguration::collar),
            IntProvider.codec(0, GroundVentFeature.MAX_COLLAR).fieldOf("collar_radius").forGetter(GroundVentConfiguration::collarRadius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("collar_chance").forGetter(GroundVentConfiguration::collarChance),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("ground").forGetter(GroundVentConfiguration::ground),
            OreConfiguration.TargetBlockState.CODEC.listOf().fieldOf("below").forGetter(GroundVentConfiguration::below),
            IntProvider.codec(0, 8).fieldOf("below_count").forGetter(GroundVentConfiguration::belowCount)
    ).apply(instance, GroundVentConfiguration::new));
}
