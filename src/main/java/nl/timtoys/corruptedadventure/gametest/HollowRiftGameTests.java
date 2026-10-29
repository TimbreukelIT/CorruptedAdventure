package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.hollow.HollowArrival;
import nl.timtoys.corruptedadventure.entity.hollow.HollowRift;
import nl.timtoys.corruptedadventure.entity.hollow.HollowScar;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The Hollow's rift (phase 4 step 2): an open rift pulls items toward it, a rift step's rift closes by itself and leaves
 * no scar, the arrival's scar turns natural ground only, the director's arrival ({@link HollowArrival}) brings The
 * Hollow through and cancelled omens close the crack without a scar. In the empty 9 × 7 × 9 {@code gametest/hollow_arena}, on a
 * grass floor the test lays at y 0. How it looks is checked in the client test. Run like {@link TierGatingGameTests}.
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

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 400)
    public static void theDirectedArrivalOpensTheRiftAndTheHollowComesThrough(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 1.0, 0.5));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        HollowArrival arrival = new HollowArrival();
        BlockPos place = helper.absolutePos(MIDDLE);
        arrival.omen(helper.getLevel(), player, place, 0);
        Nightmare appeared = arrival.appear(helper.getLevel(), ModEntityTypes.THE_HOLLOW.get(), player, place, 1.0F);
        helper.assertTrue(appeared instanceof TheHollow, "The Hollow appears");
        TheHollow hollow = (TheHollow) appeared;
        // The arrival runs without AI. With it, once through it fights the mock player, and a rift step of its own (its
        // cooldown is drawn at random) opens a new rift before the check: the test failed now and then.
        hollow.setNoAi(true);
        helper.assertTrue(hollow.isInvisible() && hollow.isArriving(), "it waits unseen in the rift");
        int emerging = HollowRift.ticksUntilEmerging();
        helper.startSequence()
                .thenExecuteAfter(emerging + 5, () -> {
                    helper.assertFalse(hollow.isInvisible(), "it comes through");
                    helper.assertTrue(hollow.isEmerging(), "pulling itself out");
                })
                .thenExecuteAfter(HollowRift.CLOSING_TICKS + 70, () -> {
                    helper.assertFalse(hollow.isArriving(), "it is through");
                    helper.assertEntityNotPresent(ModEntityTypes.HOLLOW_RIFT.get());
                    hollow.discard();
                    GameTestPlayers.remove(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 100)
    public static void cancelledOmensCloseTheCrackWithoutAScar(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = GameTestPlayers.survival(helper);
        HollowArrival arrival = new HollowArrival();
        BlockPos place = helper.absolutePos(MIDDLE);
        arrival.omen(helper.getLevel(), player, place, 0);
        helper.assertEntityPresent(ModEntityTypes.HOLLOW_RIFT.get());
        arrival.cancelled(helper.getLevel(), place);
        helper.runAfterDelay(HollowRift.CLOSING_TICKS + 5, () -> {
            helper.assertEntityNotPresent(ModEntityTypes.HOLLOW_RIFT.get());
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, MIDDLE.below());
            GameTestPlayers.remove(helper, player);
            helper.succeed();
        });
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
