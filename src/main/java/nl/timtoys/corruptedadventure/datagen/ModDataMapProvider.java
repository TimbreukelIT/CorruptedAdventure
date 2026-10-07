package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.BiomeVillagerType;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;

import java.util.concurrent.CompletableFuture;

/**
 * NeoForge data maps: villager types (vanilla looks them up by biome key, so the ring 1 copies need their own entries)
 * and compostables.
 */
final class ModDataMapProvider extends DataMapProvider {
    ModDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        // Same as vanilla's VillagerType.BY_BIOME; every other biome falls back to plains.
        var villagers = builder(NeoForgeDataMaps.VILLAGER_TYPES);
        villager(villagers, VillagerType.DESERT, Biomes.BADLANDS, Biomes.DESERT, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
        villager(villagers, VillagerType.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE);
        villager(villagers, VillagerType.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.SAVANNA, Biomes.WINDSWEPT_SAVANNA);
        villager(villagers, VillagerType.SNOW, Biomes.DEEP_FROZEN_OCEAN, Biomes.FROZEN_OCEAN, Biomes.FROZEN_RIVER,
                Biomes.ICE_SPIKES, Biomes.SNOWY_BEACH, Biomes.SNOWY_TAIGA, Biomes.SNOWY_PLAINS, Biomes.GROVE,
                Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS);
        villager(villagers, VillagerType.SWAMP, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
        villager(villagers, VillagerType.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
                Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.TAIGA, Biomes.WINDSWEPT_FOREST);

        // Vanilla's chance for leaves and saplings.
        builder(NeoForgeDataMaps.COMPOSTABLES)
                .add(ModBlocks.PALM_LEAVES.getId(), new Compostable(0.3F), false)
                .add(ModBlocks.PALM_SAPLING.getId(), new Compostable(0.3F), false)
                .add(ModBlocks.SPORE_SPROUTS.getId(), new Compostable(0.3F), false)
                // Like nether wart blocks, shroomlight and glow lichen.
                .add(ModBlocks.DRIPSHROOM_CAP.getId(), new Compostable(0.85F), false)
                .add(ModBlocks.DRIPSHROOM_LAMP.getId(), new Compostable(0.65F), false)
                .add(ModBlocks.GLOW_MOLD.getId(), new Compostable(0.5F), false)
                // Ring 3: like leaves and short grass (0.3) and flowers (0.65).
                .add(ModBlocks.EMBER_LEAVES.getId(), new Compostable(0.3F), false)
                .add(ModBlocks.ASH_SPROUTS.getId(), new Compostable(0.3F), false)
                .add(ModBlocks.FIRE_BLOOM.getId(), new Compostable(0.65F), false);
    }

    @SafeVarargs
    private static void villager(Builder<BiomeVillagerType, Biome> builder, VillagerType type, ResourceKey<Biome>... vanilla) {
        for (ResourceKey<Biome> biome : vanilla) {
            builder.add(VanillaBiomeCopies.copyOf(biome), new BiomeVillagerType(type), false);
        }
    }
}
