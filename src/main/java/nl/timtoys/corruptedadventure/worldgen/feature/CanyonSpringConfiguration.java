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
 * Configuration of {@code corruptedadventure:canyon_spring} (see {@link SpringLayout} for the shape).
 *
 * @param springHeight  height of the source in the wall above the floor (3–8)
 * @param streamLength  length of the trench from the fall to the pool, the fall included (3–6)
 * @param poolSize      width and length of the pool (2–3 each, drawn separately)
 * @param poolDepth     depth of the pool (1–2)
 * @param wallHeight    how much the wall must rise within three blocks of its foot
 * @param candidates    wall feet tried per chunk (see {@code wallHeight}), each with its own shape
 * @param minWaterY     lowest Y of the stream (the floor); the pool may go {@code poolDepth − 1} deeper
 * @param maxFloorY     highest floor: canyon floors and steps, not the plateau
 * @param floor         ground the trench and pool may be dug in
 * @param rim           ground around the pool
 * @param rimPlants     plants on the rim
 * @param plantChance   chance of a plant on a rim block
 * @param sugarCane     chance that a plant beside the water is sugar cane instead
 */
public record CanyonSpringConfiguration(IntProvider springHeight, IntProvider streamLength, IntProvider poolSize, IntProvider poolDepth,
                                        int wallHeight, int candidates, int minWaterY, int maxFloorY, HolderSet<Block> floor,
                                        BlockStateProvider rim, BlockStateProvider rimPlants, float plantChance, float sugarCane)
        implements FeatureConfiguration {
    public static final Codec<CanyonSpringConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(2, 12).fieldOf("spring_height").forGetter(CanyonSpringConfiguration::springHeight),
            IntProvider.codec(2, SpringLayout.MAX_STREAM).fieldOf("stream_length").forGetter(CanyonSpringConfiguration::streamLength),
            IntProvider.codec(1, 3).fieldOf("pool_size").forGetter(CanyonSpringConfiguration::poolSize),
            IntProvider.codec(1, 2).fieldOf("pool_depth").forGetter(CanyonSpringConfiguration::poolDepth),
            Codec.intRange(2, 32).fieldOf("wall_height").forGetter(CanyonSpringConfiguration::wallHeight),
            Codec.intRange(1, 64).fieldOf("candidates").forGetter(CanyonSpringConfiguration::candidates),
            Codec.INT.fieldOf("min_water_y").forGetter(CanyonSpringConfiguration::minWaterY),
            Codec.INT.fieldOf("max_floor_y").forGetter(CanyonSpringConfiguration::maxFloorY),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("floor").forGetter(CanyonSpringConfiguration::floor),
            BlockStateProvider.CODEC.fieldOf("rim").forGetter(CanyonSpringConfiguration::rim),
            BlockStateProvider.CODEC.fieldOf("rim_plants").forGetter(CanyonSpringConfiguration::rimPlants),
            Codec.floatRange(0.0F, 1.0F).fieldOf("plant_chance").forGetter(CanyonSpringConfiguration::plantChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("sugar_cane").forGetter(CanyonSpringConfiguration::sugarCane)
    ).apply(instance, CanyonSpringConfiguration::new));
}
