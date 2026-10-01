package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.tags.BiomeTags;
import nl.timtoys.corruptedadventure.worldgen.biome.VanillaBiomeCopies;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModBiomeTagsProviderTest {
    @Test
    void strongholdAndAncientCityTagsAreNeverMirrored() {
        for (var tag : List.of(BiomeTags.IS_OVERWORLD, BiomeTags.HAS_STRONGHOLD, BiomeTags.STRONGHOLD_BIASED_TO, BiomeTags.HAS_ANCIENT_CITY)) {
            assertFalse(ModBiomeTagsProvider.MIRRORED_TAGS.contains(tag), tag.toString());
        }
    }

    @Test
    void netherCopiesGetFortressesAndBastionsButNoFossils() {
        assertTrue(ModBiomeTagsProvider.MIRRORED_TAGS.containsAll(List.of(BiomeTags.IS_NETHER, BiomeTags.HAS_BASTION_REMNANT)));
        assertFalse(ModBiomeTagsProvider.MIRRORED_TAGS.contains(BiomeTags.HAS_NETHER_FOSSIL));
    }

    @Test
    void barePatchBiomesAreUniqueCopiedBiomes() {
        assertEquals(ModBiomeTagsProvider.BARE_PATCH_BIOMES.size(), Set.copyOf(ModBiomeTagsProvider.BARE_PATCH_BIOMES).size());
        for (var biome : ModBiomeTagsProvider.BARE_PATCH_BIOMES) {
            assertTrue(VanillaBiomeCopies.OVERWORLD_COPIES.contains(biome), biome.toString());
        }
    }

    @Test
    void mirroredTagsAreUnique() {
        assertEquals(ModBiomeTagsProvider.MIRRORED_TAGS.size(), Set.copyOf(ModBiomeTagsProvider.MIRRORED_TAGS).size());
    }
}
