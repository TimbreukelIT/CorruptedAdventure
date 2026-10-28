package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.hollow.HollowRift;
import nl.timtoys.corruptedadventure.entity.hollow.HollowScar;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The Hollow's rift (phase 4 step 2): an open rift pulls items toward it, a rift step's rift closes by itself and leaves
 * no scar, and the arrival's scar turns natural ground only. In the empty 9 × 7 × 9 {@code gametest/hollow_arena}, on a
 * grass floor the test lays at y 0. The full arrival (omens, eyes, The Hollow coming through) is checked in the client
 * test. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HollowRiftGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    private HollowRiftGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 100)
    public static void anOpenRiftPullsItemsTowardIt(GameTestHelper helper) {
        floor(helper);
        Vec3 riftAt = helper.absoluteVec(Vec3.atBottomCenterOf(MIDDLE));
        HollowRift.step(helper.getLevel(), riftAt, 0.0F, 1, 80);
        Vec3 itemAt = helper.absoluteVec(new Vec3(0.5, 1.0, 4.5));
        ItemEntity item = new ItemEntity(helper.getLevel(), itemAt.x, itemAt.y, itemAt.z, new ItemStack(Items.STICK));
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
        double before = horizontal(item.position(), riftAt);
        helper.runAfterDelay(30, () -> {
            double after = horizontal(item.position(), riftAt);
            helper.assertTrue(after < before - 0.3, "the item came closer: " + before + " -> " + after);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 120)
    public static void aStepRiftClosesByItselfWithoutAScar(GameTestHelper helper) {
        floor(helper);
        HollowRift.step(helper.getLevel(), helper.absoluteVec(Vec3.atBottomCenterOf(MIDDLE)), 0.0F, 2, 10);
        helper.runAfterDelay(2 + 10 + 40, () -> {
            helper.assertEntityNotPresent(ModEntityTypes.HOLLOW_RIFT.get());
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, MIDDLE.below());
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void theScarTurnsNaturalGroundOnly(GameTestHelper helper) {
        floor(helper);
        BlockPos built = MIDDLE.below().east();
        helper.setBlock(built, Blocks.OAK_PLANKS);
        int changed = HollowScar.place(helper.getLevel(), helper.absolutePos(MIDDLE), 2.5, helper.getLevel().getRandom());
        helper.assertTrue(changed > 0, "some ground turned");
        helper.assertBlockPresent(Blocks.OAK_PLANKS, built);
        helper.assertFalse(helper.getBlockState(MIDDLE.below()).is(Blocks.GRASS_BLOCK), "the middle turned");
        helper.assertTrue(helper.getBlockState(MIDDLE.below()).is(ModBlocks.CORRUPTED_GRASS_BLOCK.get())
                || helper.getBlockState(MIDDLE.below()).is(ModBlocks.GLITCH_BLOCK.get()), "into corruption");
        helper.assertEntityNotPresent(EntityType.ITEM);
        helper.succeed();
    }

    /** Grass at y 0 under the whole arena. */
    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
            }
        }
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }
}
