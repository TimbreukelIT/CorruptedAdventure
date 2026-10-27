package nl.timtoys.corruptedadventure.item;

import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.EnumMap;
import java.util.Map;

/**
 * The gear tiers of phase 2, one per ring (fase-2-items.md §1.1): sunstone (ring 2) about netherite, then a step up per
 * ring. The tool side is this {@link Tier}, like vanilla's {@code Tiers}; the armour side lives in
 * {@code ModArmorMaterials} with the numbers below. Sunstone is set (§5.1: netherite that lasts longer); the other
 * stats are provisional (§4.3) and get tuned in waves 3–5.
 *
 * <p>Mining gating: a tier drops every block except those in its {@link #incorrectForTool()} tag, which holds the
 * {@code needs_*_tool} tags of all higher tiers. The ores of ring N+1 are in the {@link #needsTool()} tag of ring N's
 * tier (ring 2's ores need vanilla diamond).
 */
public enum GearTier implements Tier, StringRepresentable {
    SUNSTONE("sunstone", 2, 2200, 9.0F, 4.0F, 15, new int[]{3, 6, 8, 3, 11}, 3.0F, 0.1F, 40,
            ModTags.Blocks.NEEDS_SUNSTONE_TOOL, ModTags.Blocks.INCORRECT_FOR_SUNSTONE_TOOL, ModTags.Items.INGOTS_SUNSTONE),
    VOLCANITE("volcanite", 3, 2500, 10.0F, 5.0F, 15, new int[]{4, 7, 9, 4, 12}, 3.5F, 0.1F, 42,
            ModTags.Blocks.NEEDS_VOLCANITE_TOOL, ModTags.Blocks.INCORRECT_FOR_VOLCANITE_TOOL, ModTags.Items.INGOTS_VOLCANITE),
    TIDESILVER("tidesilver", 4, 3000, 11.0F, 6.0F, 16, new int[]{4, 7, 10, 4, 13}, 4.0F, 0.15F, 47,
            ModTags.Blocks.NEEDS_TIDESILVER_TOOL, ModTags.Blocks.INCORRECT_FOR_TIDESILVER_TOOL, ModTags.Items.INGOTS_TIDESILVER),
    NULLITE("nullite", 5, 3600, 12.0F, 7.0F, 18, new int[]{5, 8, 10, 5, 14}, 5.0F, 0.2F, 53,
            ModTags.Blocks.NEEDS_NULLITE_TOOL, ModTags.Blocks.INCORRECT_FOR_NULLITE_TOOL, ModTags.Items.INGOTS_NULLITE);

    private final String name;
    private final int ring;
    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;
    /** Boots, leggings, chestplate, helmet, body (animal armour), as in vanilla's {@code ArmorMaterials}. */
    private final int[] defense;
    private final float toughness;
    private final float knockbackResistance;
    private final int durabilityFactor;
    private final TagKey<Block> needsTool;
    private final TagKey<Block> incorrectForTool;
    private final TagKey<Item> repairTag;

    GearTier(String name, int ring, int uses, float speed, float attackDamageBonus, int enchantmentValue, int[] defense,
             float toughness, float knockbackResistance, int durabilityFactor, TagKey<Block> needsTool,
             TagKey<Block> incorrectForTool, TagKey<Item> repairTag) {
        this.name = name;
        this.ring = ring;
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.defense = defense;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.durabilityFactor = durabilityFactor;
        this.needsTool = needsTool;
        this.incorrectForTool = incorrectForTool;
        this.repairTag = repairTag;
    }

    /** The ring whose material this tier is made of. */
    public int ring() {
        return ring;
    }

    /** Blocks that need at least this tier (the ores of the next ring). */
    public TagKey<Block> needsTool() {
        return needsTool;
    }

    /** Blocks this tier does not drop: the {@link #needsTool()} tags of all higher tiers. */
    public TagKey<Block> incorrectForTool() {
        return incorrectForTool;
    }

    /** {@code c:ingots/<tier>}: the repair material of tools and armour. */
    public TagKey<Item> repairTag() {
        return repairTag;
    }

    /** Armour points per piece. */
    public Map<ArmorItem.Type, Integer> defense() {
        Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, defense[0]);
        map.put(ArmorItem.Type.LEGGINGS, defense[1]);
        map.put(ArmorItem.Type.CHESTPLATE, defense[2]);
        map.put(ArmorItem.Type.HELMET, defense[3]);
        map.put(ArmorItem.Type.BODY, defense[4]);
        return map;
    }

    public float toughness() {
        return toughness;
    }

    public float knockbackResistance() {
        return knockbackResistance;
    }

    /** Armour durability = {@code ArmorItem.Type#getDurability(factor)}; vanilla netherite uses 37. */
    public int durabilityFactor() {
        return durabilityFactor;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectForTool;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(repairTag);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
