package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring2BiomesTest {
    @Test
    void bootstrapRegistersExactlyTheRing2List() {
        assertEquals(Set.copyOf(Ring2Biomes.ALL), bootstrap().keySet());
        assertEquals(Set.of(ModBiomes.CANYONLANDS, ModBiomes.OASIS_FLATS), Set.copyOf(Ring2Biomes.ALL));
    }

    @Test
    void dryHotAndTheRing2Palette() {
        BiomeAmbience.Palette palette = BiomeAmbience.Palette.RING_2;
        for (Biome biome : bootstrap().values()) {
            assertFalse(biome.hasPrecipitation());
            assertEquals(2.0F, biome.getBaseTemperature());
            BiomeSpecialEffects effects = biome.getSpecialEffects();
            assertEquals(palette.sky(), effects.getSkyColor());
            assertEquals(palette.fog(), effects.getFogColor());
            assertEquals(palette.water(), effects.getWaterColor());
            assertEquals(palette.waterFog(), effects.getWaterFogColor());
        }
    }

    @Test
    void desertSpawnsAndCamelsOnlyInTheOasis() {
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        for (Biome biome : biomes.values()) {
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.HUSK));
            assertTrue(spawns(biome, MobCategory.CREATURE, EntityType.RABBIT));
        }
        assertTrue(spawns(biomes.get(ModBiomes.OASIS_FLATS), MobCategory.CREATURE, EntityType.CAMEL));
        assertFalse(spawns(biomes.get(ModBiomes.CANYONLANDS), MobCategory.CREATURE, EntityType.CAMEL));
    }

    @Test
    void ring2BiomesAreNoVanillaCopiesSoGetNoMinecraftTags() {
        // ModBiomeTagsProvider only mirrors minecraft: tags onto the copies of vanilla biomes.
        for (ResourceKey<Biome> biome : Ring2Biomes.ALL) {
            assertFalse(VanillaBiomeCopies.COPIES.stream().map(VanillaBiomeCopies::copyOf).anyMatch(biome::equals), biome.toString());
        }
    }

    private static boolean spawns(Biome biome, MobCategory category, EntityType<?> type) {
        return biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(data -> data.type == type);
    }

    private static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        Ring2Biomes.bootstrap(new BootstrapContext<>() {
            @Override
            public Holder.Reference<Biome> register(ResourceKey<Biome> key, Biome value, Lifecycle lifecycle) {
                registered.put(key, value);
                return Holder.Reference.createStandAlone(biomes.owner(), key);
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
                return new StandaloneHolderGetter<>(key -> true);
            }
        });
        return registered;
    }
}
