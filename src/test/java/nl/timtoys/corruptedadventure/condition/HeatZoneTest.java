package nl.timtoys.corruptedadventure.condition;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModTags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The evaporation refuges (user decision 2026-10-15, Q2 = B): placed water stays only in {@code #evaporation_refuges}
 * (the ring 3 part of {@code verdant_falls}); a heat refuge alone (the dormant caldera) still lets it evaporate. The
 * open sky and the zone need a server (W).
 */
class HeatZoneTest {
    private static final String GENERATED_TAGS = "src/generated/resources/data/corruptedadventure/tags/worldgen/biome/";
    private static final String DORMANT_CALDERA = "\"corruptedadventure:dormant_caldera\"";

    private Holder<Biome> evaporationRefuge;
    private Holder<Biome> heatRefuge;
    private Holder<Biome> plain;

    @BeforeEach
    void bindTags() {
        MappedRegistry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        evaporationRefuge = biomes.register(key("evaporation_refuge"), biome(), RegistrationInfo.BUILT_IN);
        heatRefuge = biomes.register(key("heat_refuge"), biome(), RegistrationInfo.BUILT_IN);
        plain = biomes.register(key("plain"), biome(), RegistrationInfo.BUILT_IN);
        biomes.bindTags(Map.of(
                ModTags.Biomes.EVAPORATION_REFUGES, List.of(evaporationRefuge),
                ModTags.Biomes.HEAT_REFUGES, List.of(heatRefuge)));
    }

    @Test
    void waterStaysInAnEvaporationRefuge() {
        assertTrue(HeatZone.isEvaporationRefuge(evaporationRefuge));
    }

    @Test
    void aHeatRefugeAloneStillEvaporates() {
        assertFalse(HeatZone.isEvaporationRefuge(heatRefuge));
        assertFalse(HeatZone.isEvaporationRefuge(plain));
    }

    /** The generated tags: the dormant caldera cools (heat refuge) but is no evaporation refuge, so it stays dry. */
    @Test
    void theDormantCalderaStaysDry() throws IOException {
        assertTrue(Files.readString(generatedTags().resolve("heat_refuges.json")).contains(DORMANT_CALDERA));
        assertFalse(Files.readString(generatedTags().resolve("evaporation_refuges.json")).contains(DORMANT_CALDERA));
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, CorruptedAdventure.id(name));
    }

    private static Biome biome() {
        return new Biome.BiomeBuilder().hasPrecipitation(false).temperature(2.0F).downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder().fogColor(0).waterColor(0).waterFogColor(0).skyColor(0).build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY).generationSettings(BiomeGenerationSettings.EMPTY).build();
    }

    /** The generated biome tags, found upwards from the working directory. */
    private static Path generatedTags() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED_TAGS))) {
                return dir.resolve(GENERATED_TAGS);
            }
        }
        throw new IllegalStateException("No " + GENERATED_TAGS + " above " + Path.of("").toAbsolutePath());
    }
}
