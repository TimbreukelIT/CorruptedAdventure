package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.GearSet;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.function.Function;

/**
 * Item tags of the gear tier sets, written by {@code ModItemTagsProvider}: the vanilla tags netherite gear is in, so
 * enchanting, the anvil and the coatings ({@code #trimmable_armor}) treat it the same, plus NeoForge's
 * {@code c:tools/*} like vanilla's pickaxes, swords and axes.
 */
public final class GearItemTags {
    private GearItemTags() {
    }

    public static void build(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tag) {
        for (GearSet set : ModItems.GEAR_SETS) {
            tag.apply(ItemTags.SWORDS).add(set.sword().getKey());
            tag.apply(ItemTags.AXES).add(set.axe().getKey());
            tag.apply(ItemTags.PICKAXES).add(set.pickaxe().getKey());
            tag.apply(ItemTags.SHOVELS).add(set.shovel().getKey());
            tag.apply(ItemTags.HOES).add(set.hoe().getKey());
            tag.apply(Tags.Items.MINING_TOOL_TOOLS).add(set.pickaxe().getKey());
            tag.apply(Tags.Items.MELEE_WEAPON_TOOLS).add(set.sword().getKey(), set.axe().getKey());
            tag.apply(ItemTags.HEAD_ARMOR).add(set.helmet().getKey());
            tag.apply(ItemTags.CHEST_ARMOR).add(set.chestplate().getKey());
            tag.apply(ItemTags.LEG_ARMOR).add(set.leggings().getKey());
            tag.apply(ItemTags.FOOT_ARMOR).add(set.boots().getKey());
            tag.apply(ItemTags.TRIMMABLE_ARMOR).add(set.helmet().getKey(), set.chestplate().getKey(),
                    set.leggings().getKey(), set.boots().getKey());
        }
    }
}
