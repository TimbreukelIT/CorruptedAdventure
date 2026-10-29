package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.PlayerConditions;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.lostkeeper.KeeperBody;
import nl.timtoys.corruptedadventure.entity.lostkeeper.KeeperThrow;
import nl.timtoys.corruptedadventure.entity.lostkeeper.KeeperWater;
import nl.timtoys.corruptedadventure.entity.lostkeeper.TheLostKeeper;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The Lost Keeper (phase 4 step 1): its body chain and hitboxes, the crown (a hit breaks one branch and hurts more; the
 * last branch makes it fall, land hard and lie stunned), the hold (no sneaking off; damage sets free) and its poison
 * cloud; from the life round where a fighter stands (water, shore, land, air), the launch out of a boat and the ring of
 * its body, and the shared stagger on a crown hit (lets go, opens the ring, closes the gate). Without free will it never
 * attacks the mock players; the fight's bookkeeping runs without AI. Its own batch: its 24-block body reaches into
 * neighbouring tests. In {@code gametest/hollow_arena} (an empty 9 × 7 × 9 room).
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
                    double landing = keeper.virtualMaxHealth() * ServerConfig.LOST_KEEPER.landingDamage.get() / keeper.healthScale();
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

    // --- life round -------------------------------------------------------------------------------------------------

    /** A pool 3 × 3 and 4 deep in a stone rim at the arena's corner, for the water tests. */
    private static void pool(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    boolean rim = x == 0 || x == 4 || z == 0 || z == 4;
                    helper.setBlock(new BlockPos(x, y, z), rim ? Blocks.STONE : Blocks.WATER);
                }
            }
        }
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void itReadsWhereAFighterStandsInTheWaterOnTheShoreOnLandOrInTheAir(GameTestHelper helper) {
        pool(helper);
        helper.setBlock(new BlockPos(7, 0, 7), Blocks.STONE);
        int shore = ServerConfig.LOST_KEEPER.burstShoreReach.get();
        int depth = ServerConfig.LOST_KEEPER.burstMinDepth.get();
        ServerPlayer player = GameTestPlayers.survival(helper);
        helper.runAfterDelay(1, () -> {
            Object[][] cases = {
                    {new Vec3(2.5, 3.0, 2.5), KeeperWater.Footing.WATER},
                    {new Vec3(0.5, 5.0, 2.5), KeeperWater.Footing.SHORE},
                    {new Vec3(7.5, 1.0, 7.5), KeeperWater.Footing.LAND},
                    {new Vec3(7.5, 5.0, 7.5), KeeperWater.Footing.AIR}};
            for (Object[] c : cases) {
                Vec3 at = helper.absoluteVec((Vec3) c[0]);
                player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
                helper.assertValueEqual(KeeperWater.footing(player, shore, depth), c[1], "footing at " + c[0]);
            }
            KeeperWater.Column column = KeeperWater.below(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 3.0, 2.5)));
            helper.assertTrue(column != null && column.depth() == 4, "the pool is 4 deep: " + column);
            GameTestPlayers.remove(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aLaunchThrowsABoatsRiderOutAndTheBoatUpWithoutBreakingIt(GameTestHelper helper) {
        pool(helper);
        ServerPlayer player = GameTestPlayers.survival(helper);
        Boat boat = helper.spawn(EntityType.BOAT, new Vec3(2.5, 4.6, 2.5));
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(player.startRiding(boat, true), "in the boat");
            KeeperThrow.launch(player, 1.4, Vec3.ZERO);
            helper.assertTrue(player.getVehicle() == null, "the rider falls out");
            helper.assertTrue(Math.abs(player.getDeltaMovement().y - 1.4) < 1.0E-6, "the rider goes up");
            helper.assertTrue(boat.getDeltaMovement().y > 1.0, "the boat goes up too");
            helper.assertTrue(boat.isAlive(), "the boat stays whole");
            GameTestPlayers.remove(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void coiledItsBodyAndHitboxesLieOnTheRingAndUncoiledItLetsGo(GameTestHelper helper) {
        TheLostKeeper keeper = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE.above(2));
        double radius = 4.0;
        Vec3[] centre = new Vec3[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    // Looking along +z, the head is on the ring at angle 0 going round with the angle growing.
                    Vec3 head = keeper.headCentre();
                    centre[0] = new Vec3(head.x - radius, head.y - 1.3, head.z);
                    keeper.coil(centre[0], radius, 1);
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(keeper.isCoiling(), "coiling");
                    helper.assertValueEqual(keeper.mode(), TheLostKeeper.Mode.COIL, "mode");
                    for (int i = 2; i <= KeeperBody.SEGMENTS; i++) {
                        Vec3 node = keeper.body().node(i);
                        double r = Math.hypot(node.x - centre[0].x, node.z - centre[0].z);
                        helper.assertTrue(Math.abs(r - radius) < 0.8, "node " + i + " at " + r + " from the centre");
                    }
                    Entity tail = keeper.getParts()[5];
                    double r = Math.hypot(tail.getX() - centre[0].x, tail.getZ() - centre[0].z);
                    helper.assertTrue(Math.abs(r - radius) < 0.8, "the tail's hitbox is on the ring: " + r);
                    keeper.uncoil();
                    helper.assertFalse(keeper.isCoiling(), "uncoiled");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aBlowOnTheCrownMakesItReelLetGoAndOpenItsRing(GameTestHelper helper) {
        TheLostKeeper keeper = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LOST_KEEPER.get(), MIDDLE.above(2));
        ServerPlayer held = GameTestPlayers.survival(helper);
        ServerPlayer friend = GameTestPlayers.survival(helper);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(keeper.hold(held), "it holds a fighter");
                    keeper.coil(keeper.headCentre().add(-4.0, -1.3, 0.0), 4.0, 1);
                    helper.assertTrue(keeper.mayStartAttack(), "the gate is open before");
                    helper.assertTrue(keeper.crownPart().hurt(playerAttack(helper, friend), 1.0F), "the crown hit lands");
                    helper.assertTrue(keeper.isStaggered(), "it reels");
                    helper.assertFalse(keeper.mayStartAttack(), "no attack while it reels");
                    helper.assertFalse(keeper.isHolding(), "it lets go");
                    helper.assertFalse(keeper.isCoiling(), "the ring opens");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(keeper.mode(), TheLostKeeper.Mode.STAGGER, "mode");
                    GameTestPlayers.remove(helper, held);
                    GameTestPlayers.remove(helper, friend);
                })
                .thenSucceed();
    }

    private static DamageSource playerAttack(GameTestHelper helper, Player player) {
        return helper.getLevel().damageSources().playerAttack(player);
    }
}
