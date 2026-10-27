package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.function.Function;

/**
 * Item tags of the gear tiers (golf 2: sunstone), written by {@code ModItemTagsProvider}: the vanilla tags netherite
 * gear is in, so enchanting, the anvil and the coatings ({@code #trimmable_armor}) treat it the same, plus NeoForge's
 * {@code c:tools/*} like vanilla's pickaxes, swords and axes.
 */
public final class GearItemTags {
    private GearItemTags() {
    }

    public static void build(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tag) {
        tag.apply(ItemTags.SWORDS).add(ModItems.SUNSTONE_SWORD.getKey());
        tag.apply(ItemTags.AXES).add(ModItems.SUNSTONE_AXE.getKey());
        tag.apply(ItemTags.PICKAXES).add(ModItems.SUNSTONE_PICKAXE.getKey());
        tag.apply(ItemTags.SHOVELS).add(ModItems.SUNSTONE_SHOVEL.getKey());
        tag.apply(ItemTags.HOES).add(ModItems.SUNSTONE_HOE.getKey());
        tag.apply(Tags.Items.MINING_TOOL_TOOLS).add(ModItems.SUNSTONE_PICKAXE.getKey());
        tag.apply(Tags.Items.MELEE_WEAPON_TOOLS).add(ModItems.SUNSTONE_SWORD.getKey(), ModItems.SUNSTONE_AXE.getKey());
        tag.apply(ItemTags.HEAD_ARMOR).add(ModItems.SUNSTONE_HELMET.getKey());
        tag.apply(ItemTags.CHEST_ARMOR).add(ModItems.SUNSTONE_CHESTPLATE.getKey());
        tag.apply(ItemTags.LEG_ARMOR).add(ModItems.SUNSTONE_LEGGINGS.getKey());
        tag.apply(ItemTags.FOOT_ARMOR).add(ModItems.SUNSTONE_BOOTS.getKey());
        tag.apply(ItemTags.TRIMMABLE_ARMOR).add(ModItems.SUNSTONE_HELMET.getKey(), ModItems.SUNSTONE_CHESTPLATE.getKey(),
                ModItems.SUNSTONE_LEGGINGS.getKey(), ModItems.SUNSTONE_BOOTS.getKey());
    }
}
