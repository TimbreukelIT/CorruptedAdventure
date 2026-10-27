package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

import java.util.concurrent.CompletableFuture;

/**
 * Entity-type tags. The fire and poison arrows are in {@code #minecraft:arrows} like vanilla's arrows, so they count
 * as impact projectiles too (they break pointed dripstone, chorus flowers and decorated pots).
 */
final class ModEntityTypeTagsProvider extends EntityTypeTagsProvider {
    ModEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              ExistingFileHelper existingFileHelper) {
        super(output, lookup, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(EntityTypeTags.ARROWS).add(ModEntityTypes.FIRE_ARROW.get(), ModEntityTypes.POISON_ARROW.get());
    }
}
