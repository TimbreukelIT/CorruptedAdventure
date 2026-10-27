package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.function.Function;

/**
 * Item tags of the fire and poison arrows (phase 2 golf 3 + 4, stream A), written by {@code ModItemTagsProvider}: the
 * bow and the crossbow take what is in {@code #minecraft:arrows}.
 */
public final class ArrowItemTags {
    private ArrowItemTags() {
    }

    public static void build(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tag) {
        tag.apply(ItemTags.ARROWS).add(ModItems.FIRE_ARROW.getKey(), ModItems.POISON_ARROW.getKey());
    }
}
