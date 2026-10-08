package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The tunable numbers of {@link VolcanicField} (ring 3 terrain). Every field is optional in JSON; missing ones take
 * {@link #DEFAULT}.
 * <p>
 * The region thresholds are the biome rectangles of ring 3 on {@code region_heat} (h) and {@code region_ground} (g),
 * owned by the biome layout of the preset: {@code hot} = the hottest band (h ≥ hot: {@code volcanic_peaks} for g &lt; 0,
 * {@code basalt_flows} for g ≥ 0), {@code cool} = the coolest band (h &lt; cool: {@code dried_seabed} for
 * g &lt; {@code seabed_ground}, {@code titan_fields} for g ≥ {@code titan_ground}). The terrain leans on the same
 * numbers so that the shapes and the biomes coincide; change them together with the preset.
 *
 * @param plainY       mean height of the ash plains
 * @param plainAmp     amplitude (blocks) of the broad swells of the plains
 * @param rollingAmp   amplitude (blocks) of the rolling waves on the plains
 * @param hot          lower bound of the hottest region band
 * @param cool         upper bound of the coolest region band
 * @param seabedGround upper bound of {@code region_ground} for the seabed bowl
 * @param titanGround  lower bound of {@code region_ground} for the titan fields
 * @param cellSize     cell of the jittered volcano grid (one volcano or caldera at most per cell)
 * @param greatMinY    lowest rim of a great (horizon) volcano
 * @param greatMaxY    highest rim of a great volcano
 * @param coneMinRise  lowest rise of an ordinary cone's rim above the plain (39d: medium cones)
 * @param coneMaxRise  highest rise of an ordinary cone's rim above the plain (capped 10 below {@code greatMinY})
 * @param islandMinY   lowest top of a floating island
 * @param islandMaxY   highest top of a floating island
 * @param islandChance chance that an active caldera with a low lake (or a lava river cell) carries an island cluster
 */
public record VolcanicSettings(double plainY, double plainAmp, double rollingAmp, double hot, double cool, double seabedGround,
                               double titanGround, int cellSize, double greatMinY, double greatMaxY, double coneMinRise,
                               double coneMaxRise, double islandMinY, double islandMaxY, double islandChance) {
    public static final VolcanicSettings DEFAULT = new VolcanicSettings(89.0, 16.0, 7.0, 0.166, -0.051, -0.122, 0.122,
            320, 250.0, 285.0, 80.0, 140.0, 150.0, 210.0, 0.85);

    public static final Codec<VolcanicSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(-64.0, 320.0).optionalFieldOf("plain_y", DEFAULT.plainY).forGetter(VolcanicSettings::plainY),
            Codec.doubleRange(0.0, 64.0).optionalFieldOf("plain_amplitude", DEFAULT.plainAmp).forGetter(VolcanicSettings::plainAmp),
            Codec.doubleRange(0.0, 64.0).optionalFieldOf("rolling_amplitude", DEFAULT.rollingAmp).forGetter(VolcanicSettings::rollingAmp),
            Codec.doubleRange(-2.0, 2.0).optionalFieldOf("hot", DEFAULT.hot).forGetter(VolcanicSettings::hot),
            Codec.doubleRange(-2.0, 2.0).optionalFieldOf("cool", DEFAULT.cool).forGetter(VolcanicSettings::cool),
            Codec.doubleRange(-2.0, 2.0).optionalFieldOf("seabed_ground", DEFAULT.seabedGround).forGetter(VolcanicSettings::seabedGround),
            Codec.doubleRange(-2.0, 2.0).optionalFieldOf("titan_ground", DEFAULT.titanGround).forGetter(VolcanicSettings::titanGround),
            Codec.intRange(128, 1024).optionalFieldOf("cell_size", DEFAULT.cellSize).forGetter(VolcanicSettings::cellSize),
            Codec.doubleRange(-64.0, 300.0).optionalFieldOf("great_min_y", DEFAULT.greatMinY).forGetter(VolcanicSettings::greatMinY),
            Codec.doubleRange(-64.0, 300.0).optionalFieldOf("great_max_y", DEFAULT.greatMaxY).forGetter(VolcanicSettings::greatMaxY),
            Codec.doubleRange(0.0, 300.0).optionalFieldOf("cone_min_rise", DEFAULT.coneMinRise).forGetter(VolcanicSettings::coneMinRise),
            Codec.doubleRange(0.0, 300.0).optionalFieldOf("cone_max_rise", DEFAULT.coneMaxRise).forGetter(VolcanicSettings::coneMaxRise),
            Codec.doubleRange(-64.0, 300.0).optionalFieldOf("island_min_y", DEFAULT.islandMinY).forGetter(VolcanicSettings::islandMinY),
            Codec.doubleRange(-64.0, 300.0).optionalFieldOf("island_max_y", DEFAULT.islandMaxY).forGetter(VolcanicSettings::islandMaxY),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("island_chance", DEFAULT.islandChance).forGetter(VolcanicSettings::islandChance)
    ).apply(instance, VolcanicSettings::new));
}
