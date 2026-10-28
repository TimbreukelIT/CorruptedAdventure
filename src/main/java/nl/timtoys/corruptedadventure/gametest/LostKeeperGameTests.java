package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.PlayerConditions;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.lostkeeper.KeeperBody;
import nl.timtoys.corruptedadventure.entity.lostkeeper.TheLostKeeper;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The Lost Keeper (phase 4 step 1): its body chain and hitboxes, the crown (a hit breaks one branch and hurts more; the
 * last branch makes it fall, land hard and lie stunned), the hold (no sneaking off; damage sets free) and its poison
 * cloud. Without free will it never attacks the mock players; the fight's bookkeeping runs without AI. Its own batch:
 * its 24-block body reaches into neighbouring tests. In {@code gametest/hollow_arena} (an empty 9 × 7 × 9 room).
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LostKeeperGameTests {
    private static final String ARENA = "gametest/hollow_arena";
    private static final String BATCH = "lost_keeper";
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    private LostKeeperGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void itsBodyTrailsAtSegmentLengthWithSixHitboxesAndACrown(GameTestHelper helper) {
        TheLostKeeper keeper = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE);
        helper.runAfterDelay(2, () -> {
            KeeperBody body = keeper.body();
            helper.assertTrue(body.isPlaced(), "the body is laid out");
            for (int i = 1; i <= KeeperBody.SEGMENTS; i++) {
                double length = body.node(i).distanceTo(body.node(i - 1));
                helper.assertTrue(Math.abs(length - KeeperBody.SEGMENT_LENGTH) < 1.0E-6, "segment " + i + " is " + length);
            }
            helper.assertValueEqual(keeper.getParts().length, 7, "hitboxes");
            for (int i = 0; i < keeper.getParts().length; i++) {
                Entity part = keeper.getParts()[i];
                helper.assertValueEqual(part.getId(), keeper.getId() + i + 1, "part id " + i);
                helper.assertTrue(helper.getLevel().getEntityOrPart(part.getId()) == part, "the level finds part " + i);
            }
            helper.assertTrue(keeper.crownPart().getY() > keeper.getY() + 1.0, "the crown sits above the head");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aHitOnTheCrownBreaksOneBranchAndHurtsMoreThanOnTheBody(GameTestHelper helper) {
        TheLostKeeper crowned = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE);
        TheLostKeeper other = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE.above(3));
        ServerPlayer player = GameTestPlayers.survival(helper);
        helper.runAfterDelay(2, () -> {
            float full = crowned.getHealth();
            helper.assertTrue(crowned.crownPart().hurt(playerAttack(helper, player), 10.0F), "the crown hit lands");
            helper.assertValueEqual(crowned.crownBranches(), TheLostKeeper.CROWN_BRANCHES - 1, "branches after one hit");
            float crownLoss = full - crowned.getHealth();
            helper.assertTrue(other.getParts()[2].hurt(playerAttack(helper, player), 10.0F), "the body hit lands");
            float bodyLoss = full - other.getHealth();
            helper.assertTrue(crownLoss > bodyLoss, "crown " + crownLoss + " > body " + bodyLoss);
            helper.assertValueEqual(other.crownBranches(), TheLostKeeper.CROWN_BRANCHES, "a body hit breaks nothing");
            crowned.invulnerableTime = 0;
            crowned.crownPart().hurt(playerAttack(helper, player), 10.0F);
            helper.assertValueEqual(crowned.crownBranches(), TheLostKeeper.CROWN_BRANCHES - 1, "no second branch within the cooldown");
            GameTestPlayers.remove(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void withTheLastBranchGoneItLandsHardAndLiesStunned(GameTestHelper helper) {
        helper.setBlock(MIDDLE.below(), Blocks.STONE);
        TheLostKeeper keeper = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE);
        int gap = ServerConfig.LOST_KEEPER.crownBreakCooldownTicks.get() + 1;
        float[] before = new float[1];
        var sequence = helper.startSequence();
        for (int k = 0; k < TheLostKeeper.CROWN_BRANCHES; k++) {
            sequence.thenExecuteAfter(gap, () -> {
                before[0] = keeper.getHealth();
                keeper.breakBranch();
            });
        }
        sequence.thenExecuteAfter(2, () -> {
                    helper.assertFalse(keeper.hasCrown(), "the crown is dark");
                    helper.assertFalse(keeper.canSwimHere(), "no swimming in the air without it");
                    helper.assertTrue(keeper.isStunned(), "stunned after the landing");
                    double landing = keeper.getMaxHealth() * ServerConfig.LOST_KEEPER.landingDamage.get();
                    helper.assertTrue(before[0] - keeper.getHealth() > landing * 0.3,
                            "the landing hurt: " + (before[0] - keeper.getHealth()));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aHeldFighterCannotSneakOffButDamageSetsThemFree(GameTestHelper helper) {
        TheLostKeeper keeper = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE);
        ServerPlayer held = GameTestPlayers.survival(helper);
        ServerPlayer friend = GameTestPlayers.survival(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(keeper.hold(held), "it coils round the fighter");
            helper.assertTrue(held.getVehicle() == keeper, "held");
            held.setShiftKeyDown(true);
            held.stopRiding();
            helper.assertTrue(held.getVehicle() == keeper, "sneaking does not get them off");
            held.setShiftKeyDown(false);
            float free = ServerConfig.LOST_KEEPER.freeDamage.get().floatValue();
            keeper.hurt(playerAttack(helper, friend), free + 1.0F);
            helper.assertFalse(keeper.isHolding(), "the friend's blow sets them free");
            helper.assertTrue(held.getVehicle() == null, "free");
            GameTestPlayers.remove(helper, held);
            GameTestPlayers.remove(helper, friend);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void itsPoisonCloudRaisesThePoisonMeterOfPlayersInIt(GameTestHelper helper) {
        TheLostKeeper keeper = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE.above(4));
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        PlayerConditions.set(player, Condition.POISON, 0.0F);
        keeper.addCloud(player.getBoundingBox().getCenter());
        helper.runAfterDelay(10, () -> {
            float poison = PlayerConditions.get(player, Condition.POISON);
            helper.assertTrue(poison > 0.5F, "poison after 10 ticks in the cloud: " + poison);
            GameTestPlayers.remove(helper, player);
            helper.succeed();
        });
    }

    private static DamageSource playerAttack(GameTestHelper helper, Player player) {
        return helper.getLevel().damageSources().playerAttack(player);
    }
}
