package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.item.GearTier;
import nl.timtoys.corruptedadventure.item.coating.Coating;
import nl.timtoys.corruptedadventure.registry.GearSet;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The whole smithing ladder diamond → sunstone → volcanite → tidesilver → nullite (fase-2-items.md §4.3, §5.1, §6.2,
 * §6.5, §7.1) through the server's real {@code RecipeManager}: an enchanted sword and an enchanted, coated chestplate
 * keep their enchantment and coating on every step; netherite gear and skipped steps are no way in. Run like
 * {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SmithingLadderGameTests {
    private SmithingLadderGameTests() {
    }

    @GameTest(template = "gametest/empty")
    public static void diamondGearClimbsToNulliteAndKeepsItsEnchantmentAndCoating(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        List<String> failures = new ArrayList<>();

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(unbreaking, 3);
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chestplate.enchant(unbreaking, 2);
        chestplate = coat(level, chestplate, Coating.HEAT);
        if (!hasCoating(chestplate, Coating.HEAT)) {
            helper.fail("the heat coating does not go on a diamond chestplate: " + chestplate);
        }

        for (GearTier tier : GearTier.values()) {
            sword = upgrade(level, failures, tier, sword, set -> set.sword().get());
            chestplate = upgrade(level, failures, tier, chestplate, set -> set.chestplate().get());
            if (sword.getEnchantments().getLevel(unbreaking) != 3) {
                failures.add(tier.getSerializedName() + " sword lost its enchantment: " + sword.getEnchantments());
            }
            if (chestplate.getEnchantments().getLevel(unbreaking) != 2) {
                failures.add(tier.getSerializedName() + " chestplate lost its enchantment: " + chestplate.getEnchantments());
            }
            if (!hasCoating(chestplate, Coating.HEAT)) {
                failures.add(tier.getSerializedName() + " chestplate lost its coating: " + chestplate.get(DataComponents.TRIM));
            }
        }
        if (!sword.is(ModItems.NULLITE_SWORD.get()) || !chestplate.is(ModItems.NULLITE_CHESTPLATE.get())) {
            failures.add("the ladder ends at " + sword.getItem() + " and " + chestplate.getItem());
        }
        // The top of the ladder takes a coating of its own too (our armour is in #minecraft:trimmable_armor).
        if (!hasCoating(coat(level, chestplate, Coating.VOID), Coating.VOID)) {
            failures.add("the void coating does not replace the heat coating on a nullite chestplate");
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void netheriteGearAndSkippedStepsAreNoWayIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> failures = new ArrayList<>();
        Item[] netherite = {Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL,
                Items.NETHERITE_HOE, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS};
        for (GearTier tier : GearTier.values()) {
            for (Item piece : netherite) {
                expectNone(level, failures, tier, new ItemStack(piece));
            }
            // Only the tier right below goes in: diamond gear takes no volcanite template, sunstone no tidesilver one...
            if (tier != GearTier.SUNSTONE) {
                expectNone(level, failures, tier, new ItemStack(Items.DIAMOND_SWORD));
                expectNone(level, failures, tier, new ItemStack(Items.DIAMOND_CHESTPLATE));
            }
            for (GearTier lower : GearTier.values()) {
                if (lower.ordinal() < tier.ordinal() - 1 || lower == tier) {
                    expectNone(level, failures, tier, new ItemStack(ModItems.gear(lower).sword().get()));
                    expectNone(level, failures, tier, new ItemStack(ModItems.gear(lower).chestplate().get()));
                }
            }
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    private static ItemStack upgrade(ServerLevel level, List<String> failures, GearTier tier, ItemStack base,
                                     Function<GearSet, Item> piece) {
        Item expected = piece.apply(ModItems.gear(tier));
        String name = BuiltInRegistries.ITEM.getKey(expected).getPath();
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(template(tier)), base, new ItemStack(ingot(tier)));
        Optional<RecipeHolder<SmithingRecipe>> found = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
        if (found.isEmpty()) {
            failures.add(name + ": no smithing recipe from " + base.getItem());
            return new ItemStack(expected);
        }
        if (!found.get().id().equals(CorruptedAdventure.id(name + "_smithing"))) {
            failures.add(name + ": recipe " + found.get().id());
        }
        ItemStack out = found.get().value().assemble(input, level.registryAccess());
        if (!out.is(expected) || out.getCount() != 1) {
            failures.add(name + ": got " + out);
        }
        return out;
    }

    private static void expectNone(ServerLevel level, List<String> failures, GearTier tier, ItemStack base) {
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(template(tier)), base, new ItemStack(ingot(tier)));
        level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).ifPresent(found -> failures.add(
                BuiltInRegistries.ITEM.getKey(base.getItem()).getPath() + " upgrades with the " + tier.getSerializedName()
                        + " template (" + found.id() + ")"));
    }

    /** {@code armor} through the real coating recipe of {@code coating}; empty if no recipe takes it. */
    static ItemStack coat(ServerLevel level, ItemStack armor, Coating coating) {
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(coating.template()), armor,
                new ItemStack(coating.material().item()));
        return level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level)
                .map(found -> found.value().assemble(input, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    static boolean hasCoating(ItemStack armor, Coating coating) {
        ArmorTrim trim = armor.get(DataComponents.TRIM);
        return trim != null && trim.pattern().is(coating.pattern()) && trim.material().is(coating.material().key());
    }

    private static Item template(GearTier tier) {
        return switch (tier) {
            case SUNSTONE -> ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE.get();
            case VOLCANITE -> ModItems.VOLCANITE_UPGRADE_SMITHING_TEMPLATE.get();
            case TIDESILVER -> ModItems.TIDESILVER_UPGRADE_SMITHING_TEMPLATE.get();
            case NULLITE -> ModItems.NULLITE_UPGRADE_SMITHING_TEMPLATE.get();
        };
    }

    private static Item ingot(GearTier tier) {
        return switch (tier) {
            case SUNSTONE -> ModItems.SUNSTONE_INGOT.get();
            case VOLCANITE -> ModItems.VOLCANITE_INGOT.get();
            case TIDESILVER -> ModItems.TIDESILVER_INGOT.get();
            case NULLITE -> ModItems.NULLITE_INGOT.get();
        };
    }
}
