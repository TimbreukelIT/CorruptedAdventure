package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The ground of the ash drifts (47d, INT2): whether an ore feature from a neighbouring chunk changes the block below
 * before or after the drift is decided, the answer is the same. Unit tests have no datapack: the tags are bound by
 * hand with (part of) their contents.
 */
class AshDriftGroundTest {
    @BeforeEach
    void bindTags() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(
                BlockTags.BASE_STONE_OVERWORLD, holders(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF, Blocks.DEEPSLATE),
                Tags.Blocks.ORES, holders(Blocks.COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COAL_ORE, ModBlocks.VOLCANITE_ORE.get(),
                        ModBlocks.DEEPSLATE_VOLCANITE_ORE.get(), ModBlocks.BASALT_SULFUR_ORE.get(), ModBlocks.SULFUR_ORE.get()),
                ModTags.Blocks.RING_3_STONES, holders(Blocks.BASALT, Blocks.BLACKSTONE, ModBlocks.PUMICE.get(), ModBlocks.ASH_BLOCK.get())));
    }

    @AfterEach
    void resetTags() {
        BuiltInRegistries.BLOCK.resetTags();
    }

    @Test
    void anOreBlobBeforeOrAfterGivesTheSameLayer() {
        HolderSet<Block> listed = Ring3DressingFeatures.openGround();
        // What the ore features of the ring do to a block: host → product.
        Block[][] changes = {
                {Blocks.TUFF, Blocks.DEEPSLATE_IRON_ORE}, {Blocks.DEEPSLATE, Blocks.DEEPSLATE_COAL_ORE}, {Blocks.STONE, Blocks.COAL_ORE},
                {Blocks.STONE, Blocks.TUFF}, {Blocks.STONE, Blocks.GRAVEL}, {Blocks.STONE, Blocks.DIRT}, {Blocks.STONE, Blocks.GRANITE},
                {Blocks.BASALT, ModBlocks.BASALT_SULFUR_ORE.get()}, {Blocks.STONE, ModBlocks.VOLCANITE_ORE.get()},
                {Blocks.TUFF, ModBlocks.DEEPSLATE_VOLCANITE_ORE.get()}, {Blocks.STONE, ModBlocks.SULFUR_ORE.get()}};
        for (Block[] change : changes) {
            assertEquals(AshDriftFeature.carries(change[0].defaultBlockState(), listed), AshDriftFeature.carries(change[1].defaultBlockState(), listed),
                    change[0] + " → " + change[1]);
        }
        // Still never on heat or liquid.
        assertFalse(AshDriftFeature.carries(Blocks.MAGMA_BLOCK.defaultBlockState(), listed));
        assertFalse(AshDriftFeature.carries(Blocks.LAVA.defaultBlockState(), listed));
    }

    private static List<Holder<Block>> holders(Block... blocks) {
        return Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::wrapAsHolder).toList();
    }
}
