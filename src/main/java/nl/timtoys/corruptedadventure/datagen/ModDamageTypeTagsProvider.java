package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.datagen.tags.Ring3DamageTypeTags;
import nl.timtoys.corruptedadventure.datagen.tags.Ring4DamageTypeTags;
import nl.timtoys.corruptedadventure.registry.ModDamageTypes;

import java.util.concurrent.CompletableFuture;

/**
 * Damage-type tags of the conditions. Dehydration has exactly the tags of vanilla's {@code starve} (vanilla's and
 * NeoForge's tag providers): through armour, shields and wolf armour, no effects (resistance), no knockback.
 */
final class ModDamageTypeTagsProvider extends DamageTypeTagsProvider {
    ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              ExistingFileHelper existingFileHelper) {
        super(output, lookup, CorruptedAdventure.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.DEHYDRATION);
        tag(DamageTypeTags.BYPASSES_EFFECTS).add(ModDamageTypes.DEHYDRATION);
        tag(DamageTypeTags.NO_KNOCKBACK).add(ModDamageTypes.DEHYDRATION);
        tag(DamageTypeTags.BYPASSES_WOLF_ARMOR).add(ModDamageTypes.DEHYDRATION);
        tag(Tags.DamageTypes.IS_ENVIRONMENT).add(ModDamageTypes.DEHYDRATION);
        // Ring 3: the conditions stream adds its tags in its own helper.
        Ring3DamageTypeTags.addTags(this::tag);
        Ring4DamageTypeTags.addTags(this::tag);
    }
}
