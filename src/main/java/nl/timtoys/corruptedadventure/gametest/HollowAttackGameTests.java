package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.hollow.HollowShockwave;
import nl.timtoys.corruptedadventure.entity.hollow.HollowSweep;
import nl.timtoys.corruptedadventure.entity.hollow.HollowVoidClaw;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The Hollow's extra attacks (life round): the ground slam's wave hits who stands on the ground and passes under a
 * jump, a void claw bites only after its crack and not at all when cancelled, the sweep hits in front and knocks away
 * but not behind, and none of them leaves anything behind. Husks stand in for players (a fresh mock player is
 * invulnerable after joining and never ticks). In the empty 9 × 7 × 9 {@code gametest/hollow_arena}, on a grass floor
 * the test lays at y 0. How they look is checked in the client test. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HollowAttackGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    private HollowAttackGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 100)
    public static void theShockWaveHitsWhoStandsOnTheGroundButPassesUnderAJump(GameTestHelper helper) {
        floor(helper);
        Husk standing = husk(helper, new BlockPos(4, 1, 7));
        Husk jumping = husk(helper, new BlockPos(1, 1, 4));
        jumping.setNoGravity(true);
        // Mid-jump: feet 0.8 above the ground the wave runs over.
        Vec3 up = jumping.position().add(0.0, 0.8, 0.0);
        jumping.setPos(up.x, up.y, up.z);
        jumping.setOnGround(false);
        HollowShockwave.spawn(helper.getLevel(), null, helper.absoluteVec(Vec3.atBottomCenterOf(MIDDLE)), 4.0F, 0.5F, 4.0F, 0.5);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(standing.getHealth() < standing.getMaxHealth(), "the wave hit the one on the ground");
            helper.assertValueEqual(jumping.getHealth(), jumping.getMaxHealth(), "health of the one in the air");
            helper.assertEntityNotPresent(ModEntityTypes.HOLLOW_SHOCKWAVE.get());
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 100)
    public static void aVoidClawBitesOnlyAfterItsCrackAndNotWhenCancelled(GameTestHelper helper) {
        floor(helper);
        Husk biting = husk(helper, new BlockPos(2, 1, 4));
        Husk spared = husk(helper, new BlockPos(6, 1, 4));
        HollowVoidClaw.spawn(helper.getLevel(), null, biting.position(), 0.0F, 20, 4.0F);
        HollowVoidClaw cancelled = HollowVoidClaw.spawn(helper.getLevel(), null, spared.position(), 0.0F, 20, 4.0F);
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertValueEqual(biting.getHealth(), biting.getMaxHealth(), "health during the crack");
                    cancelled.cancel();
                })
                .thenExecuteAfter(15, () -> {
                    helper.assertTrue(biting.getHealth() < biting.getMaxHealth(), "the claw bit");
                    helper.assertValueEqual(spared.getHealth(), spared.getMaxHealth(), "health over a cancelled claw");
                })
                .thenExecuteAfter(HollowVoidClaw.LIFE_TICKS + 2,
                        () -> helper.assertEntityNotPresent(ModEntityTypes.HOLLOW_VOID_CLAW.get()))
                .thenSucceed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void theSweepHitsInFrontAndKnocksAwayButNotBehind(GameTestHelper helper) {
        floor(helper);
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        Husk front = husk(helper, new BlockPos(4, 1, 7));
        Husk behind = husk(helper, new BlockPos(4, 1, 1));
        // Yaw 0 faces south (+z): the front husk is there.
        int hits = HollowSweep.strike(helper.getLevel(), hollow, 0.0F);
        helper.assertValueEqual(hits, 1, "hits");
        helper.assertTrue(front.getHealth() < front.getMaxHealth(), "the one in front was hit");
        helper.assertTrue(front.getDeltaMovement().z > 0.5, "and knocked away: " + front.getDeltaMovement());
        helper.assertValueEqual(behind.getHealth(), behind.getMaxHealth(), "health of the one behind");
        helper.succeed();
    }

    private static Husk husk(GameTestHelper helper, BlockPos at) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, at);
        husk.setOnGround(true);
        return husk;
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
            }
        }
    }
}
