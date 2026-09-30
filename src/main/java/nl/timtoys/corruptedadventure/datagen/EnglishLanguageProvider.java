package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.LanguageProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

/** {@code en_us}: mandatory for every key. */
final class EnglishLanguageProvider extends LanguageProvider {
    EnglishLanguageProvider(PackOutput output) {
        super(output, CorruptedAdventure.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("generator.corruptedadventure.corrupted_world", "Corrupted World");

        biome(ModBiomes.PLACEHOLDER_RING_1, "Placeholder Ring 1");
        biome(ModBiomes.PLACEHOLDER_RING_2, "Placeholder Ring 2");
        biome(ModBiomes.PLACEHOLDER_RING_3, "Placeholder Ring 3");
        biome(ModBiomes.PLACEHOLDER_RING_4, "Placeholder Ring 4");
        biome(ModBiomes.PLACEHOLDER_RING_5, "Placeholder Ring 5");
        biome(ModBiomes.VOID_EDGE, "Void Edge");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_1, "Placeholder Nether Ring 1");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_2, "Placeholder Nether Ring 2");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_3, "Placeholder Nether Ring 3");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_4, "Placeholder Nether Ring 4");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_5, "Placeholder Nether Ring 5");
        biome(ModBiomes.NETHER_VOID_EDGE, "Nether Void Edge");
    }

    private void biome(ResourceKey<Biome> biome, String name) {
        add(biome.location().toLanguageKey("biome"), name);
    }
}
