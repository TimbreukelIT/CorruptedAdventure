package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4Tunnels;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Package 77, the tunnel net's lining: crystals by band, ores, claw marks in side walls, tracks on the floors. */
class TunnelLiningFeatureTest {
    @Test
    void theCrystalFollowsTheBand() {
        assertEquals(TunnelLiningFeature.Lining.TOXIC_CRYSTAL, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.TOXIC_CRYSTAL, TunnelLiningFeature.lining(Ring4Tunnels.Fill.GAS, false, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.TOXIC_CRYSTAL, TunnelLiningFeature.lining(Ring4Tunnels.Fill.POOL, false, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.AQUAMARINE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.WATER, true, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.AQUAMARINE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DOME_AIR, true, 20, 0.0F));
    }

    /** Jade only deep in the flooded net; claw marks only in side walls; the rest stays rock. */
    @Test
    void jadeDeepAndClawsInTheSideWalls() {
        float afterOres = TunnelLiningFeature.AQUAMARINE + TunnelLiningFeature.TIDESILVER + 0.001F;
        assertEquals(TunnelLiningFeature.Lining.JADE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.WATER, true, -10, afterOres));
        assertTrue(TunnelLiningFeature.lining(Ring4Tunnels.Fill.WATER, true, 10, afterOres) != TunnelLiningFeature.Lining.JADE);
        assertTrue(TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, -10, TunnelLiningFeature.TOXIC_CRYSTAL
                + TunnelLiningFeature.TIDESILVER + 0.001F) != TunnelLiningFeature.Lining.JADE, "no jade in the dry net");
        float claw = TunnelLiningFeature.TOXIC_CRYSTAL + TunnelLiningFeature.TIDESILVER + 0.001F;
        assertEquals(TunnelLiningFeature.Lining.CLAW_MARKS, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, 20, claw));
        assertEquals(TunnelLiningFeature.Lining.NONE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, false, 20, claw), "not in a floor");
        assertEquals(TunnelLiningFeature.Lining.NONE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, 20, 0.5F));
    }

    /**
     * 77b: the claw marks follow the rock around them (stone, deepslate, tidestone), and the clawed stone and deepslate
     * stay rock for the floors' tracks although they are in neither stone tag.
     */
    @Test
    void clawMarksFollowTheirRock() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(
                BlockTags.BASE_STONE_OVERWORLD, holders(Blocks.STONE, Blocks.DEEPSLATE),
                BlockTags.DEEPSLATE_ORE_REPLACEABLES, holders(Blocks.DEEPSLATE),
                ModTags.Blocks.RING_4_STONES, holders(ModBlocks.TIDESTONE.get())));
        try {
            assertEquals(ModBlocks.CLAWED_STONE.get(), claws(Blocks.STONE));
            assertEquals(ModBlocks.CLAWED_DEEPSLATE.get(), claws(Blocks.DEEPSLATE));
            assertEquals(ModBlocks.CLAWED_TIDESTONE.get(), claws(ModBlocks.TIDESTONE.get()));
            // A claw-marked host keeps its rock (review golf 3, finding 2).
            assertEquals(ModBlocks.CLAWED_DEEPSLATE.get(), claws(ModBlocks.CLAWED_DEEPSLATE.get()));
            assertEquals(ModBlocks.CLAWED_STONE.get(), claws(ModBlocks.CLAWED_STONE.get()));
            assertEquals(ModBlocks.DEEPSLATE_TIDESILVER_ORE.get(), TunnelLiningFeature.liningState(TunnelLiningFeature.Lining.TIDESILVER,
                    ModBlocks.CLAWED_DEEPSLATE.get().defaultBlockState(), 0.5F).getBlock());
            assertTrue(TunnelLiningFeature.rock(ModBlocks.CLAWED_STONE.get().defaultBlockState()));
            assertTrue(TunnelLiningFeature.rock(ModBlocks.CLAWED_DEEPSLATE.get().defaultBlockState()));
        } finally {
            BuiltInRegistries.BLOCK.resetTags();
        }
    }

    private static Block claws(Block host) {
        return TunnelLiningFeature.liningState(TunnelLiningFeature.Lining.CLAW_MARKS, host.defaultBlockState(), 0.5F).getBlock();
    }

    private static List<Holder<Block>> holders(Block... blocks) {
        return Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::wrapAsHolder).toList();
    }

    /** Dry floors: trails, bones, nests; flooded floors only bones; nothing elsewhere. */
    @Test
    void tracksOnTheFloors() {
        assertTrue(TunnelLiningFeature.track(true, false, 0.0F, 0.2F).getBlock().getDescriptionId().contains("slime_trail"));
        assertTrue(TunnelLiningFeature.track(true, false, TunnelLiningFeature.SLIME_TRAIL + 0.001F, 0.2F).getBlock().getDescriptionId().contains("bone_block"));
        assertTrue(TunnelLiningFeature.track(true, false, TunnelLiningFeature.SLIME_TRAIL + TunnelLiningFeature.BONES + 0.001F, 0.2F)
                .getBlock().getDescriptionId().contains("creature_nest"));
        assertNull(TunnelLiningFeature.track(true, false, 0.9F, 0.2F));
        assertTrue(TunnelLiningFeature.track(false, true, 0.0F, 0.7F).getBlock().getDescriptionId().contains("bone_block"));
        assertNull(TunnelLiningFeature.track(false, true, TunnelLiningFeature.BONES + 0.001F, 0.7F));
        assertNull(TunnelLiningFeature.track(false, false, 0.0F, 0.7F), "gas, pools and domes get no tracks");
    }

    /** The position hash spreads evenly enough for the small chances. */
    @Test
    void thePositionHashIsEven() {
        Map<Integer, Integer> buckets = new HashMap<>();
        int n = 0;
        for (int x = 0; x < 64; x++) {
            for (int y = -20; y < 20; y++) {
                for (int z = 0; z < 16; z++) {
                    float u = TunnelLiningFeature.unit(20260930L, x, y, z, 0);
                    assertTrue(u >= 0.0F && u < 1.0F);
                    buckets.merge((int) (u * 10), 1, Integer::sum);
                    n++;
                }
            }
        }
        for (int b = 0; b < 10; b++) {
            double share = buckets.getOrDefault(b, 0) / (double) n;
            assertTrue(Math.abs(share - 0.1) < 0.02, "bucket " + b + ": " + share);
        }
    }
}
