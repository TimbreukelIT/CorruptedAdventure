package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Settings of {@link IslandGlowFeature}: one palette per band of ring 4 and the spacing of the light sources.
 *
 * @param mythic   the palette in the mythical band
 * @param blighted the palette in the blighted band
 * @param spacing  the side of a cell (blocks, dividing 16) that gets one light source
 */
public record IslandGlowConfiguration(Palette mythic, Palette blighted, int spacing) implements FeatureConfiguration {
    public static final Codec<IslandGlowConfiguration> CODEC = RecordCodecBuilder.<IslandGlowConfiguration>create(instance -> instance.group(
            Palette.CODEC.fieldOf("mythic").forGetter(IslandGlowConfiguration::mythic),
            Palette.CODEC.fieldOf("blighted").forGetter(IslandGlowConfiguration::blighted),
            Codec.intRange(2, 8).fieldOf("spacing").forGetter(IslandGlowConfiguration::spacing)
    ).apply(instance, IslandGlowConfiguration::new)).validate(IslandGlowConfiguration::check);

    private static DataResult<IslandGlowConfiguration> check(IslandGlowConfiguration config) {
        return 16 % config.spacing == 0 ? DataResult.success(config)
                : DataResult.error(() -> "island_glow: spacing must divide 16, got " + config.spacing);
    }

    /**
     * What glows in one band.
     *
     * @param light        the light source of a cell (it must light the cell: light ≥ 5)
     * @param fallback     the source where {@code light}'s block cannot stand (e.g. a flower on stone)
     * @param accent       an extra block without the light duty (buds, the faint blighted glowcap)
     * @param accentChance per cell: the chance of an accent
     */
    public record Palette(BlockStateProvider light, BlockStateProvider fallback, BlockStateProvider accent, float accentChance) {
        public static final Codec<Palette> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("light").forGetter(Palette::light),
                BlockStateProvider.CODEC.fieldOf("fallback").forGetter(Palette::fallback),
                BlockStateProvider.CODEC.fieldOf("accent").forGetter(Palette::accent),
                Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(Palette::accentChance)
        ).apply(instance, Palette::new));
    }
}
