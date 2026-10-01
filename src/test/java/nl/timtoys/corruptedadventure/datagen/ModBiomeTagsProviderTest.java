package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.tags.BiomeTags;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModBiomeTagsProviderTest {
    @Test
    void strongholdAndAncientCityTagsAreNeverMirrored() {
        for (var tag : List.of(BiomeTags.IS_OVERWORLD, BiomeTags.HAS_STRONGHOLD, BiomeTags.STRONGHOLD_BIASED_TO, BiomeTags.HAS_ANCIENT_CITY)) {
            assertFalse(ModBiomeTagsProvider.MIRRORED_TAGS.contains(tag), tag.toString());
        }
    }

    @Test
    void mirroredTagsAreUnique() {
        assertEquals(ModBiomeTagsProvider.MIRRORED_TAGS.size(), Set.copyOf(ModBiomeTagsProvider.MIRRORED_TAGS).size());
    }
}
