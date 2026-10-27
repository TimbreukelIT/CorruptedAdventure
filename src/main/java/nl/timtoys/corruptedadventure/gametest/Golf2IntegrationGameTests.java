package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Integration of phase 2 golf 2 (docs/plans/fase-2/golf-2-INT.md): every new recipe through the server's real
 * {@code RecipeManager} with a filled grid (the recipe found must be ours and give the right result), and the mining
 * ladder of the ring 2 and ring 3 ores with real tools. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Golf2IntegrationGameTests {
    private static final ItemStack EMPTY = ItemStack.EMPTY;

    private Golf2IntegrationGameTests() {
    }

    @GameTest(template = "gametest/empty")
    public static void everyNewCraftingRecipeGivesItsResult(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> failures = new ArrayList<>();
        ItemStack s = stack(ModItems.SOUL_CRYSTAL), d = stack(Items.DIAMOND), c = stack(ModBlocks.SCORCHED_SANDSTONE);
        craft(level, failures, "sunstone_upgrade_smithing_template", ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE, 1,
                3, s, d, s, s, c, s, s, d, s);
        ItemStack t = stack(ModItems.TURQUOISE), cactus = stack(ModBlocks.BARREL_CACTUS);
        craft(level, failures, "dehydration_coating_smithing_template_from_barrel_cactus",
                ModItems.DEHYDRATION_COATING_SMITHING_TEMPLATE, 1, 3, t, c, t, c, cactus, c, t, c, t);
        ItemStack template = stack(ModItems.DEHYDRATION_COATING_SMITHING_TEMPLATE);
        craft(level, failures, "dehydration_coating_smithing_template", ModItems.DEHYDRATION_COATING_SMITHING_TEMPLATE, 2,
                3, t, template, t, t, c, t, t, t, t);
        craft(level, failures, "canteen", ModItems.CANTEEN, 1,
                3, EMPTY, stack(Items.STRING), EMPTY, stack(Items.LEATHER), stack(Items.GLASS_BOTTLE),
                stack(Items.LEATHER), EMPTY, stack(Items.LEATHER), EMPTY);
        craft(level, failures, "cactus_juice", ModItems.CACTUS_JUICE, 1, 2, cactus, stack(Items.GLASS_BOTTLE));
        craft(level, failures, "salted_jerky", ModItems.SALTED_JERKY, 2, 2, stack(ModItems.SALT), stack(Items.COOKED_BEEF));
        craft(level, failures, "salted_jerky", ModItems.SALTED_JERKY, 2, 2, stack(Items.COOKED_SALMON), stack(ModItems.SALT));
        ItemStack sc = stack(ModItems.SOUL_CRYSTAL);
        craft(level, failures, "soul_crystal_block", ModBlocks.SOUL_CRYSTAL_BLOCK, 1, 2, sc, sc, sc, sc);
        craft(level, failures, "turquoise_inlaid_sandstone", ModBlocks.TURQUOISE_INLAID_SANDSTONE, 8,
                3, c, c, c, c, t, c, c, c, c);
        craft(level, failures, "salt_lamp", ModBlocks.SALT_LAMP, 1, 1, stack(Items.TORCH), stack(ModBlocks.SALT_BLOCK));
        ItemStack i = stack(ModBlocks.TURQUOISE_INLAID_SANDSTONE);
        craft(level, failures, "turquoise_inlaid_sandstone_stairs", ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS, 4,
                3, i, EMPTY, EMPTY, i, i, EMPTY, i, i, i);
        craft(level, failures, "turquoise_inlaid_sandstone_slab", ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB, 6, 3, i, i, i);
        craft(level, failures, "turquoise_inlaid_sandstone_wall", ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL, 6,
                3, i, i, i, i, i, i);

        List<String> cut = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING,
                new SingleRecipeInput(i), level)) {
            ItemStack out = holder.value().getResultItem(level.registryAccess());
            cut.add(BuiltInRegistries.ITEM.getKey(out.getItem()).getPath() + "x" + out.getCount());
        }
        for (String expected : List.of("turquoise_inlaid_sandstone_stairsx1", "turquoise_inlaid_sandstone_slabx2",
                "turquoise_inlaid_sandstone_wallx1")) {
            if (!cut.contains(expected)) {
                failures.add("stonecutter misses " + expected + " (has " + cut + ")");
            }
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void diamondGearBecomesSunstoneAndKeepsItsEnchantments(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        List<String> failures = new ArrayList<>();
        Item[][] pairs = {
                {Items.DIAMOND_SWORD, ModItems.SUNSTONE_SWORD.get()},
                {Items.DIAMOND_AXE, ModItems.SUNSTONE_AXE.get()},
                {Items.DIAMOND_PICKAXE, ModItems.SUNSTONE_PICKAXE.get()},
                {Items.DIAMOND_SHOVEL, ModItems.SUNSTONE_SHOVEL.get()},
                {Items.DIAMOND_HOE, ModItems.SUNSTONE_HOE.get()},
                {Items.DIAMOND_HELMET, ModItems.SUNSTONE_HELMET.get()},
                {Items.DIAMOND_CHESTPLATE, ModItems.SUNSTONE_CHESTPLATE.get()},
                {Items.DIAMOND_LEGGINGS, ModItems.SUNSTONE_LEGGINGS.get()},
                {Items.DIAMOND_BOOTS, ModItems.SUNSTONE_BOOTS.get()},
        };
        for (Item[] pair : pairs) {
            ItemStack base = new ItemStack(pair[0]);
            base.enchant(unbreaking, 3);
            SmithingRecipeInput input = new SmithingRecipeInput(stack(ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE), base,
                    stack(ModItems.SUNSTONE_INGOT));
            String name = BuiltInRegistries.ITEM.getKey(pair[1]).getPath();
            var found = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
            if (found.isEmpty()) {
                failures.add(name + ": no smithing recipe");
                continue;
            }
            ItemStack out = found.get().value().assemble(input, level.registryAccess());
            if (!found.get().id().equals(CorruptedAdventure.id(name + "_smithing"))) {
                failures.add(name + ": recipe " + found.get().id());
            }
            if (!out.is(pair[1]) || out.getEnchantments().getLevel(unbreaking) != 3) {
                failures.add(name + ": got " + out + " " + out.getEnchantments());
            }
        }
        // Netherite gear is not the base of a sunstone upgrade.
        SmithingRecipeInput netherite = new SmithingRecipeInput(stack(ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(Items.NETHERITE_SWORD), stack(ModItems.SUNSTONE_INGOT));
        if (level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, netherite, level).isPresent()) {
            failures.add("netherite sword upgrades to sunstone");
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void ring2OresNeedDiamondAndRing3OresTheSunstonePickaxe(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        ItemStack diamond = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack sunstone = stack(ModItems.SUNSTONE_PICKAXE);
        for (TagKey<Block> ring2 : List.of(ModTags.Blocks.ORES_SUNSTONE, ModTags.Blocks.ORES_TURQUOISE)) {
            for (BlockState ore : states(ring2)) {
                expect(failures, ore, iron, false);
                expect(failures, ore, diamond, true);
                expect(failures, ore, sunstone, true);
            }
        }
        for (TagKey<Block> ring3 : List.of(ModTags.Blocks.ORES_VOLCANITE, ModTags.Blocks.ORES_SULFUR,
                ModTags.Blocks.ORES_FIRE_RUBY)) {
            for (BlockState ore : states(ring3)) {
                expect(failures, ore, diamond, false);
                expect(failures, ore, new ItemStack(Items.NETHERITE_PICKAXE), false);
                expect(failures, ore, sunstone, true);
            }
        }
        for (TagKey<Block> ring4 : List.of(ModTags.Blocks.ORES_TIDESILVER, ModTags.Blocks.ORES_JADE)) {
            for (BlockState ore : states(ring4)) {
                expect(failures, ore, sunstone, false);
            }
        }
        ItemStack wooden = new ItemStack(Items.WOODEN_PICKAXE);
        for (Block block : List.of(ModBlocks.SOUL_CRYSTAL_BLOCK.get(), ModBlocks.TURQUOISE_INLAID_SANDSTONE.get(),
                ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS.get(), ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB.get(),
                ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL.get(), ModBlocks.SALT_LAMP.get())) {
            expect(failures, block.defaultBlockState(), wooden, true);
        }
        helper.assertValueEqual(ModBlocks.SALT_LAMP.get().defaultBlockState().getLightEmission(), 12, "salt lamp light");
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    private static void expect(List<String> failures, BlockState state, ItemStack tool, boolean drops) {
        boolean actual = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        if (actual != drops) {
            failures.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath() + " with "
                    + BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath() + ": drops " + actual);
        }
    }

    private static List<BlockState> states(TagKey<Block> tag) {
        List<BlockState> states = new ArrayList<>();
        for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
            states.add(block.value().defaultBlockState());
        }
        if (states.isEmpty()) {
            throw new IllegalStateException("empty tag " + tag.location());
        }
        return states;
    }

    /** Crafts a {@code width}-wide grid (rows follow from the item count) and checks recipe id, item and count. */
    private static void craft(ServerLevel level, List<String> failures, String id, ItemLike result, int count,
                              int width, ItemStack... grid) {
        CraftingInput input = CraftingInput.of(width, grid.length / width, Arrays.asList(grid));
        Optional<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> found =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isEmpty()) {
            failures.add(id + ": no recipe");
            return;
        }
        ItemStack out = found.get().value().assemble(input, level.registryAccess());
        ResourceLocation expected = CorruptedAdventure.id(id);
        if (!found.get().id().equals(expected) || !out.is(result.asItem()) || out.getCount() != count) {
            failures.add(id + ": " + found.get().id() + " gives " + out.getCount() + "x " + out.getItem());
        }
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }
}
