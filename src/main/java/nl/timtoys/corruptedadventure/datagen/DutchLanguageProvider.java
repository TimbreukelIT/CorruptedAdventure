package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.LanguageProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

/** {@code nl_nl}: optional; missing keys fall back to {@code en_us}. */
final class DutchLanguageProvider extends LanguageProvider {
    DutchLanguageProvider(PackOutput output) {
        super(output, CorruptedAdventure.MOD_ID, "nl_nl");
    }

    @Override
    protected void addTranslations() {
        add("generator.corruptedadventure.corrupted_world", "Corrupte wereld");

        biome(ModBiomes.PLACEHOLDER_RING_1, "Placeholder ring 1");
        biome(ModBiomes.PLACEHOLDER_RING_2, "Placeholder ring 2");
        biome(ModBiomes.PLACEHOLDER_RING_3, "Placeholder ring 3");
        biome(ModBiomes.PLACEHOLDER_RING_4, "Placeholder ring 4");
        biome(ModBiomes.PLACEHOLDER_RING_5, "Placeholder ring 5");
        biome(ModBiomes.VOID_EDGE, "Voidrand");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_1, "Placeholder Nether-ring 1");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_2, "Placeholder Nether-ring 2");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_3, "Placeholder Nether-ring 3");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_4, "Placeholder Nether-ring 4");
        biome(ModBiomes.PLACEHOLDER_NETHER_RING_5, "Placeholder Nether-ring 5");
        biome(ModBiomes.NETHER_VOID_EDGE, "Nether-voidrand");
    }

    private void biome(ResourceKey<Biome> biome, String name) {
        add(biome.location().toLanguageKey("biome"), name);
    }
}
