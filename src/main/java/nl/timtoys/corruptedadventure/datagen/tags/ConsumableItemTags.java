package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.function.Function;

/** Item tags of the consumables (phase 2 golf 2, stream V), written by {@code ModItemTagsProvider}. */
public final class ConsumableItemTags {
    private ConsumableItemTags() {
    }

    public static void addTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tag) {
        tag.apply(ModTags.Items.SALTED_JERKY_MEATS).addTag(Tags.Items.FOODS_COOKED_MEAT).addTag(Tags.Items.FOODS_COOKED_FISH);
    }
}
