package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.item.GearTier;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mining gating of the ring ores with the real tags (phase 2 wave 1, fase-2-items.md §4.3): every pickaxe on the
 * ladder stone → iron → diamond = netherite → sunstone → volcanite → tidesilver → nullite against every ore of ours in
 * {@code c:ores}, and against the metal and gem storage blocks of phase 2 (the salt block is not gated). Sunstone,
 * volcanite and tidesilver use their real pickaxes (golf 2–4); nullite, without tools yet, uses a netherite pickaxe with
 * its tool component ({@link GearTier#createToolProperties}), exactly what its pickaxe will carry. Run: {@code ./gradlew
 * runGameTestServer} (or {@code /test runall} in a dev world).
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TierGatingGameTests {
    /** The rank of the weakest pickaxe that drops each ore family; Nether ring N like ring N. */
    private static final Map<TagKey<Block>, Integer> REQUIRED = new LinkedHashMap<>();

    static {
        REQUIRED.put(ModTags.Blocks.ORES_SOUL_CRYSTAL, 2);   // Nether ring 1: ring 1 level (vanilla iron)
        REQUIRED.put(ModTags.Blocks.ORES_SUNSTONE, 3);       // ring 2: diamond
        REQUIRED.put(ModTags.Blocks.ORES_TURQUOISE, 3);
        REQUIRED.put(ModTags.Blocks.ORES_VOLCANITE, 4);      // ring 3: sunstone
        REQUIRED.put(ModTags.Blocks.ORES_SULFUR, 4);
        REQUIRED.put(ModTags.Blocks.ORES_FIRE_RUBY, 4);
        REQUIRED.put(ModTags.Blocks.ORES_TIDESILVER, 5);     // ring 4: volcanite
        REQUIRED.put(ModTags.Blocks.ORES_JADE, 5);
        REQUIRED.put(ModTags.Blocks.ORES_NULLITE, 6);        // ring 5: tidesilver
        REQUIRED.put(ModTags.Blocks.ORES_WHITE_CORE, 6);     // Nether ring 5: tidesilver
        // The storage blocks need what their ore needs (like vanilla's iron, diamond and netherite blocks).
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_RAW_SUNSTONE, 3);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_SUNSTONE, 3);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_RAW_TURQUOISE, 3);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_TURQUOISE, 3);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_RAW_VOLCANITE, 4);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_VOLCANITE, 4);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_FIRE_RUBY, 4);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_RAW_TIDESILVER, 5);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_TIDESILVER, 5);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_JADE, 5);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_RAW_NULLITE, 6);
        REQUIRED.put(ModTags.Blocks.STORAGE_BLOCKS_NULLITE, 6);
    }

    private TierGatingGameTests() {
    }

    @GameTest(template = "gametest/empty")
    public static void ringOresNeedTheTierOfTheRingBefore(GameTestHelper helper) {
        Map<String, Integer> pickaxes = new LinkedHashMap<>();
        Map<String, ItemStack> stacks = new LinkedHashMap<>();
        pickaxe(pickaxes, stacks, "stone", new ItemStack(Items.STONE_PICKAXE), 1);
        pickaxe(pickaxes, stacks, "iron", new ItemStack(Items.IRON_PICKAXE), 2);
        pickaxe(pickaxes, stacks, "diamond", new ItemStack(Items.DIAMOND_PICKAXE), 3);
        pickaxe(pickaxes, stacks, "netherite", new ItemStack(Items.NETHERITE_PICKAXE), 3);
        for (GearTier tier : GearTier.values()) {
            pickaxe(pickaxes, stacks, tier.getSerializedName(), tierPickaxe(tier), tier.ordinal() + 4);
        }

        List<String> failures = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        int ores = 0;
        for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES)) {
            if (isOurs(holder.value())) {
                ores++;
                blocks.add(holder.value());
            }
        }
        int storageBlocks = 0;
        for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.STORAGE_BLOCKS)) {
            if (isOurs(holder.value()) && requiredRank(holder.value()) != null) {
                storageBlocks++;
                blocks.add(holder.value());
            }
        }
        for (Block block : blocks) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            Integer required = requiredRank(block);
            if (required == null) {
                failures.add(id + " is in no known ore family");
                continue;
            }
            BlockState state = block.defaultBlockState();
            if (!state.requiresCorrectToolForDrops()) {
                failures.add(id + " drops without the correct tool");
            }
            for (Map.Entry<String, Integer> pickaxe : pickaxes.entrySet()) {
                boolean expected = pickaxe.getValue() >= required;
                boolean actual = stacks.get(pickaxe.getKey()).isCorrectToolForDrops(state);
                if (actual != expected) {
                    failures.add(id + " with " + pickaxe.getKey() + ": " + actual + ", expected " + expected);
                }
            }
        }
        if (ores < 26) {
            failures.add("only " + ores + " ores of ours in c:ores");
        }
        if (storageBlocks != 12) {
            failures.add(storageBlocks + " gated storage blocks of ours in c:storage_blocks, expected 12");
        }
        if (!failures.isEmpty()) {
            helper.fail(failures.size() + " gating errors: " + String.join("; ", failures));
        }
        helper.succeed();
    }

    /** The tier's real pickaxe once it exists; until then a netherite pickaxe with the tier's tool component. */
    private static ItemStack tierPickaxe(GearTier tier) {
        return ModItems.gear(tier).map(set -> new ItemStack(set.pickaxe().get())).orElseGet(() -> {
            ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
            stack.set(DataComponents.TOOL, tier.createToolProperties(BlockTags.MINEABLE_WITH_PICKAXE));
            return stack;
        });
    }

    private static void pickaxe(Map<String, Integer> ranks, Map<String, ItemStack> stacks, String name, ItemStack stack, int rank) {
        ranks.put(name, rank);
        stacks.put(name, stack);
    }

    private static boolean isOurs(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(CorruptedAdventure.MOD_ID);
    }

    private static Integer requiredRank(Block block) {
        for (Map.Entry<TagKey<Block>, Integer> entry : REQUIRED.entrySet()) {
            if (block.defaultBlockState().is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
