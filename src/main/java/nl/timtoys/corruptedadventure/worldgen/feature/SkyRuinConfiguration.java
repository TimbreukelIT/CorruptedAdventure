package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link SkyRuinFeature}.
 *
 * @param shaft   the pillars and the arch, per block
 * @param capital the top of a pillar that still stands whole
 * @param floor   the old floor between the pillars (in the ground, in patches)
 * @param rubble  fallen blocks on the ground
 * @param altar   the block in the middle
 * @param light   what stands on the altar (a crystal: the ruin's glow)
 * @param moss    a carpet on broken tops and rubble
 * @param vine    vines on the pillars (one face set per side)
 */
public record SkyRuinConfiguration(BlockStateProvider shaft, BlockStateProvider capital, BlockStateProvider floor,
                                   BlockStateProvider rubble, BlockState altar, BlockState light, BlockState moss,
                                   BlockState vine) implements FeatureConfiguration {
    public static final Codec<SkyRuinConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("shaft").forGetter(SkyRuinConfiguration::shaft),
            BlockStateProvider.CODEC.fieldOf("capital").forGetter(SkyRuinConfiguration::capital),
            BlockStateProvider.CODEC.fieldOf("floor").forGetter(SkyRuinConfiguration::floor),
            BlockStateProvider.CODEC.fieldOf("rubble").forGetter(SkyRuinConfiguration::rubble),
            BlockState.CODEC.fieldOf("altar").forGetter(SkyRuinConfiguration::altar),
            BlockState.CODEC.fieldOf("light").forGetter(SkyRuinConfiguration::light),
            BlockState.CODEC.fieldOf("moss").forGetter(SkyRuinConfiguration::moss),
            BlockState.CODEC.fieldOf("vine").forGetter(SkyRuinConfiguration::vine)
    ).apply(instance, SkyRuinConfiguration::new));
}
