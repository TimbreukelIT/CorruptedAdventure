package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link DeadTreeFeature}.
 *
 * @param log          the trunk (vertical)
 * @param wood         the branches and the root knobs (bark all round)
 * @param height       how far the trunk rises above the water or the ground
 * @param lean         how far the top leans out of line (blocks)
 * @param branches     how many bare branches
 * @param branchLength how long a branch is
 * @param maxWater     the deepest water the tree stands in
 * @param vineChance   per branch block, the chance of a vine strand hanging beside it
 * @param vineLength   how long a strand is
 * @param decor        moss, mushrooms and the like on the tops of the wood
 * @param decorChance  per top of the wood above the water, the chance of a {@code decor}
 */
public record DeadTreeConfiguration(BlockState log, BlockState wood, IntProvider height, IntProvider lean, IntProvider branches,
                                    IntProvider branchLength, int maxWater, float vineChance, IntProvider vineLength,
                                    BlockStateProvider decor, float decorChance) implements FeatureConfiguration {
    public static final Codec<DeadTreeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("log").forGetter(DeadTreeConfiguration::log),
            BlockState.CODEC.fieldOf("wood").forGetter(DeadTreeConfiguration::wood),
            IntProvider.codec(1, 16).fieldOf("height").forGetter(DeadTreeConfiguration::height),
            IntProvider.codec(0, DeadTreeFeature.MAX_LEAN).fieldOf("lean").forGetter(DeadTreeConfiguration::lean),
            IntProvider.codec(0, 6).fieldOf("branches").forGetter(DeadTreeConfiguration::branches),
            IntProvider.codec(1, DeadTreeFeature.MAX_BRANCH).fieldOf("branch_length").forGetter(DeadTreeConfiguration::branchLength),
            Codec.intRange(0, 16).fieldOf("max_water").forGetter(DeadTreeConfiguration::maxWater),
            Codec.floatRange(0.0F, 1.0F).fieldOf("vine_chance").forGetter(DeadTreeConfiguration::vineChance),
            IntProvider.codec(1, 12).fieldOf("vine_length").forGetter(DeadTreeConfiguration::vineLength),
            BlockStateProvider.CODEC.fieldOf("decor").forGetter(DeadTreeConfiguration::decor),
            Codec.floatRange(0.0F, 1.0F).fieldOf("decor_chance").forGetter(DeadTreeConfiguration::decorChance)
    ).apply(instance, DeadTreeConfiguration::new));
}
