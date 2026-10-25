package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * The wood sets get exactly oak's recipes (crimson's for the Nether stems): planks from the logs tag, wood (or hyphae)
 * from four logs, and the planks family (stairs, slab, fence, fence gate, door, trapdoor, button, pressure plate) with
 * oak's counts, groups and unlock conditions. Sticks, crafting tables, chests, wooden tools and charcoal come from the
 * vanilla tags the sets are in.
 */
public final class WoodSetRecipes extends RecipeSet {
    private WoodSetRecipes() {
    }

    public static void build(RecipeOutput output) {
        woodSet(output, ModTags.Items.DEAD_LOGS, ModBlocks.DEAD_LOG.get(), ModBlocks.DEAD_WOOD.get(),
                ModBlocks.STRIPPED_DEAD_LOG.get(), ModBlocks.STRIPPED_DEAD_WOOD.get(), planks(ModBlocks.DEAD_PLANKS.get())
                        .button(ModBlocks.DEAD_BUTTON.get()).fence(ModBlocks.DEAD_FENCE.get())
                        .fenceGate(ModBlocks.DEAD_FENCE_GATE.get()).pressurePlate(ModBlocks.DEAD_PRESSURE_PLATE.get())
                        .slab(ModBlocks.DEAD_SLAB.get()).stairs(ModBlocks.DEAD_STAIRS.get())
                        .door(ModBlocks.DEAD_DOOR.get()).trapdoor(ModBlocks.DEAD_TRAPDOOR.get()));
        woodSet(output, ModTags.Items.PALM_LOGS, ModBlocks.PALM_LOG.get(), ModBlocks.PALM_WOOD.get(),
                ModBlocks.STRIPPED_PALM_LOG.get(), ModBlocks.STRIPPED_PALM_WOOD.get(), planks(ModBlocks.PALM_PLANKS.get())
                        .button(ModBlocks.PALM_BUTTON.get()).fence(ModBlocks.PALM_FENCE.get())
                        .fenceGate(ModBlocks.PALM_FENCE_GATE.get()).pressurePlate(ModBlocks.PALM_PRESSURE_PLATE.get())
                        .slab(ModBlocks.PALM_SLAB.get()).stairs(ModBlocks.PALM_STAIRS.get())
                        .door(ModBlocks.PALM_DOOR.get()).trapdoor(ModBlocks.PALM_TRAPDOOR.get()));
        woodSet(output, ModTags.Items.DRIPSHROOM_STEMS, ModBlocks.DRIPSHROOM_STEM.get(), ModBlocks.DRIPSHROOM_HYPHAE.get(),
                ModBlocks.STRIPPED_DRIPSHROOM_STEM.get(), ModBlocks.STRIPPED_DRIPSHROOM_HYPHAE.get(),
                planks(ModBlocks.DRIPSHROOM_PLANKS.get())
                        .button(ModBlocks.DRIPSHROOM_BUTTON.get()).fence(ModBlocks.DRIPSHROOM_FENCE.get())
                        .fenceGate(ModBlocks.DRIPSHROOM_FENCE_GATE.get()).pressurePlate(ModBlocks.DRIPSHROOM_PRESSURE_PLATE.get())
                        .slab(ModBlocks.DRIPSHROOM_SLAB.get()).stairs(ModBlocks.DRIPSHROOM_STAIRS.get())
                        .door(ModBlocks.DRIPSHROOM_DOOR.get()).trapdoor(ModBlocks.DRIPSHROOM_TRAPDOOR.get()));
        woodSet(output, ModTags.Items.CHARRED_LOGS, ModBlocks.CHARRED_LOG.get(), ModBlocks.CHARRED_WOOD.get(),
                ModBlocks.STRIPPED_CHARRED_LOG.get(), ModBlocks.STRIPPED_CHARRED_WOOD.get(), planks(ModBlocks.CHARRED_PLANKS.get())
                        .button(ModBlocks.CHARRED_BUTTON.get()).fence(ModBlocks.CHARRED_FENCE.get())
                        .fenceGate(ModBlocks.CHARRED_FENCE_GATE.get()).pressurePlate(ModBlocks.CHARRED_PRESSURE_PLATE.get())
                        .slab(ModBlocks.CHARRED_SLAB.get()).stairs(ModBlocks.CHARRED_STAIRS.get())
                        .door(ModBlocks.CHARRED_DOOR.get()).trapdoor(ModBlocks.CHARRED_TRAPDOOR.get()));
        woodSet(output, ModTags.Items.RIMESHROOM_STEMS, ModBlocks.RIMESHROOM_STEM.get(), ModBlocks.RIMESHROOM_HYPHAE.get(),
                ModBlocks.STRIPPED_RIMESHROOM_STEM.get(), ModBlocks.STRIPPED_RIMESHROOM_HYPHAE.get(),
                planks(ModBlocks.RIMESHROOM_PLANKS.get())
                        .button(ModBlocks.RIMESHROOM_BUTTON.get()).fence(ModBlocks.RIMESHROOM_FENCE.get())
                        .fenceGate(ModBlocks.RIMESHROOM_FENCE_GATE.get()).pressurePlate(ModBlocks.RIMESHROOM_PRESSURE_PLATE.get())
                        .slab(ModBlocks.RIMESHROOM_SLAB.get()).stairs(ModBlocks.RIMESHROOM_STAIRS.get())
                        .door(ModBlocks.RIMESHROOM_DOOR.get()).trapdoor(ModBlocks.RIMESHROOM_TRAPDOOR.get()));
        woodSet(output, ModTags.Items.MOONPETAL_LOGS, ModBlocks.MOONPETAL_LOG.get(), ModBlocks.MOONPETAL_WOOD.get(),
                ModBlocks.STRIPPED_MOONPETAL_LOG.get(), ModBlocks.STRIPPED_MOONPETAL_WOOD.get(),
                planks(ModBlocks.MOONPETAL_PLANKS.get())
                        .button(ModBlocks.MOONPETAL_BUTTON.get()).fence(ModBlocks.MOONPETAL_FENCE.get())
                        .fenceGate(ModBlocks.MOONPETAL_FENCE_GATE.get()).pressurePlate(ModBlocks.MOONPETAL_PRESSURE_PLATE.get())
                        .slab(ModBlocks.MOONPETAL_SLAB.get()).stairs(ModBlocks.MOONPETAL_STAIRS.get())
                        .door(ModBlocks.MOONPETAL_DOOR.get()).trapdoor(ModBlocks.MOONPETAL_TRAPDOOR.get()));
        witheredMoonpetal(output);
        woodSet(output, ModTags.Items.VERDANT_LOGS, ModBlocks.VERDANT_LOG.get(), ModBlocks.VERDANT_WOOD.get(),
                ModBlocks.STRIPPED_VERDANT_LOG.get(), ModBlocks.STRIPPED_VERDANT_WOOD.get(), planks(ModBlocks.VERDANT_PLANKS.get())
                        .button(ModBlocks.VERDANT_BUTTON.get()).fence(ModBlocks.VERDANT_FENCE.get())
                        .fenceGate(ModBlocks.VERDANT_FENCE_GATE.get()).pressurePlate(ModBlocks.VERDANT_PRESSURE_PLATE.get())
                        .slab(ModBlocks.VERDANT_SLAB.get()).stairs(ModBlocks.VERDANT_STAIRS.get())
                        .door(ModBlocks.VERDANT_DOOR.get()).trapdoor(ModBlocks.VERDANT_TRAPDOOR.get()));
        // The corrupted set (88r, contract ring 5 §11) also has oak's sign and hanging sign.
        woodSet(output, ModTags.Items.CORRUPTED_LOGS, ModBlocks.CORRUPTED_LOG.get(), ModBlocks.CORRUPTED_WOOD.get(),
                ModBlocks.STRIPPED_CORRUPTED_LOG.get(), ModBlocks.STRIPPED_CORRUPTED_WOOD.get(),
                planks(ModBlocks.CORRUPTED_PLANKS.get())
                        .button(ModBlocks.CORRUPTED_BUTTON.get()).fence(ModBlocks.CORRUPTED_FENCE.get())
                        .fenceGate(ModBlocks.CORRUPTED_FENCE_GATE.get()).pressurePlate(ModBlocks.CORRUPTED_PRESSURE_PLATE.get())
                        .sign(ModBlocks.CORRUPTED_SIGN.get(), ModBlocks.CORRUPTED_WALL_SIGN.get())
                        .slab(ModBlocks.CORRUPTED_SLAB.get()).stairs(ModBlocks.CORRUPTED_STAIRS.get())
                        .door(ModBlocks.CORRUPTED_DOOR.get()).trapdoor(ModBlocks.CORRUPTED_TRAPDOOR.get()));
        hangingSign(output, ModBlocks.CORRUPTED_HANGING_SIGN, ModBlocks.STRIPPED_CORRUPTED_LOG);
    }

    /**
     * The withered moonpetal has no planks of its own (voorlopig, golf 1): its logs give moonpetal planks, like the
     * living tree's, and four logs give its own wood.
     */
    private static void witheredMoonpetal(RecipeOutput output) {
        TagKey<Item> logs = ModTags.Items.WITHERED_MOONPETAL_LOGS;
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MOONPETAL_PLANKS, 4)
                .requires(logs)
                .group("planks")
                .unlockedBy("has_logs", has(logs))
                .save(output, CorruptedAdventure.id("moonpetal_planks_from_withered_moonpetal_logs"));
        woodFromLogs(output, ModBlocks.WITHERED_MOONPETAL_WOOD, ModBlocks.WITHERED_MOONPETAL_LOG);
        woodFromLogs(output, ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD, ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG);
    }

    private static void woodSet(RecipeOutput output, TagKey<Item> logs, ItemLike log, ItemLike wood, ItemLike strippedLog,
                                ItemLike strippedWood, BlockFamily.Builder family) {
        BlockFamily planks = family.recipeGroupPrefix("wooden").recipeUnlockedBy("has_planks").getFamily();
        planksFromLogs(output, planks.getBaseBlock(), logs, 4);
        woodFromLogs(output, wood, log);
        woodFromLogs(output, strippedWood, strippedLog);
        generateRecipes(output, planks, FeatureFlagSet.of(FeatureFlags.VANILLA));
    }

    /** Like vanilla's {@code BlockFamilies.OAK_PLANKS}; built here, outside vanilla's family registry. */
    private static BlockFamily.Builder planks(Block planks) {
        return new BlockFamily.Builder(planks);
    }
}
