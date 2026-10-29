package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmarePacing;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareTargetGoal;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The shared reactions of the life round on a real Nightmare (The Hollow, without free will; the reactions tick without
 * AI, the target is set by hand or by {@link NightmareTargetGoal} driven by the test): the stagger and the gate, the
 * rest window after a burst, the search for the last known place, and the target switch to whoever hits hardest or to
 * near, weak prey. Each test with players in the target choice has its own batch, so no player of a test beside it
 * counts. In the empty 9 × 7 × 9 {@code gametest/hollow_arena}. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightmareReactionGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 0, 4);
    private static final String ARENA = "gametest/hollow_arena";

    private NightmareReactionGameTests() {
    }

    private static NightmareConfig config() {
        return ServerConfig.HOLLOW.common;
    }

    @GameTest(template = ARENA, batch = "nightmare_reactions", timeoutTicks = 400)
    public static void muchDamageInAShortTimeStaggersItAndClosesTheGate(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        ServerPlayer player = playerAt(helper, 1.5, 1.5);
        helper.assertTrue(hollow.mayStartAttack(), "the gate is open at first");
        helper.assertTrue(hollow.hurt(playerAttack(helper, player), 2.0F), "a small hit lands");
        helper.assertFalse(hollow.isStaggered(), "a small hit does not stagger");
        hollow.invulnerableTime = 0;
        helper.assertTrue(hollow.hurt(playerAttack(helper, player), 60.0F), "a big hit lands");
        helper.assertTrue(hollow.isStaggered(), "much damage in a short time staggers it");
        helper.assertFalse(hollow.mayStartAttack(), "no attack while staggered");
        int ticks = config().staggerTicks.get();
        helper.startSequence()
                .thenExecuteAfter(ticks + 2, () -> {
                    helper.assertFalse(hollow.isStaggered(), "the stagger wears off");
                    helper.assertTrue(hollow.mayStartAttack(), "and the gate opens again");
                    hollow.invulnerableTime = 0;
                    hollow.hurt(playerAttack(helper, player), 60.0F);
                    helper.assertFalse(hollow.isStaggered(), "no new stagger in the cooldown");
                    helper.assertFalse(hollow.hitWeakSpot(), "not even from the weak spot");
                    GameTestPlayers.remove(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "nightmare_reactions", timeoutTicks = 100)
    public static void aHitOnTheWeakSpotStaggersAtOnce(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        helper.assertTrue(hollow.hitWeakSpot(), "the weak spot staggers it");
        helper.assertTrue(hollow.isStaggered(), "staggered");
        helper.assertFalse(hollow.hitWeakSpot(), "not twice");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "nightmare_reactions", timeoutTicks = 400)
    public static void aBurstOfAttacksEndsInARestWindow(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        ServerPlayer player = playerAt(helper, 7.5, 4.5);
        hollow.setTarget(player);
        int burstMax = config().burstMaxAttacks.get();
        int attacks = 0;
        while (!hollow.isResting() && attacks < burstMax) {
            helper.assertTrue(hollow.mayStartAttack(), "the gate is open within a burst");
            hollow.countAttack();
            attacks++;
        }
        helper.assertTrue(hollow.isResting(), "resting after " + attacks + " attacks");
        helper.assertTrue(attacks >= config().burstMinAttacks.get(), "not before the shortest burst");
        helper.assertFalse(hollow.mayStartAttack(), "no attack in the rest window");
        Vec3 point = hollow.restPoint();
        helper.assertTrue(point != null, "a rest point");
        double distance = Math.sqrt(Math.pow(point.x - player.getX(), 2) + Math.pow(point.z - player.getZ(), 2));
        double expected = hollow.restStyle() == NightmarePacing.Style.BACK_OFF ? config().restDistance.get() : 4.0;
        helper.assertTrue(Math.abs(distance - expected) < 0.01,
                hollow.restStyle() + " point at " + distance + " from the target, expected " + expected);
        helper.startSequence()
                .thenExecuteAfter(config().restMaxSeconds.get() * 20 + 2, () -> {
                    helper.assertFalse(hollow.isResting(), "the rest window ends");
                    helper.assertTrue(hollow.mayStartAttack(), "and the next burst may start");
                    GameTestPlayers.remove(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "nightmare_search", timeoutTicks = 400)
    public static void outOfSightItSearchesWhereItLastSawItsTarget(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), new BlockPos(2, 0, 4));
        ServerPlayer player = playerAt(helper, 7.5, 1.5);
        Vec3 seen = player.position();
        hollow.setTarget(player);
        Vec3 hidden = helper.absoluteVec(new Vec3(7.5, 0.0, 7.5));
        int searchAfter = config().searchAfterSeconds.get() * 20;
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    helper.assertValueEqual(hollow.lastKnownPosition(), seen, "the place it saw its target");
                    wall(helper, true);
                    player.moveTo(hidden.x, hidden.y, hidden.z, 0.0F, 0.0F);
                })
                .thenExecuteAfter(searchAfter + 10, () -> {
                    helper.assertTrue(hollow.isSearching(), "out of sight it searches");
                    helper.assertValueEqual(hollow.lastKnownPosition(), seen, "where it last saw the target");
                    helper.assertFalse(hollow.mayStartAttack(), "no attack while searching");
                    wall(helper, false);
                })
                .thenExecuteAfter(10, () -> {
                    helper.assertFalse(hollow.isSearching(), "in sight again: the search ends");
                    helper.assertValueEqual(hollow.lastKnownPosition(), player.position(), "it knows the new place");
                    GameTestPlayers.remove(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "nightmare_target_hardest", timeoutTicks = 200)
    public static void itSwitchesToWhoeverHitsItHardest(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        ServerPlayer near = playerAt(helper, 1.5, 1.5);
        ServerPlayer far = playerAt(helper, 7.5, 7.5);
        NightmareTargetGoal goal = new NightmareTargetGoal(hollow);
        helper.assertTrue(goal.canUse(), "fighters in range");
        goal.start();
        helper.assertValueEqual(hollow.getTarget(), near, "nobody hit it yet: the nearest");
        hollow.hurt(playerAttack(helper, far), 10.0F);
        helper.startSequence()
                .thenExecuteAfter(21, () -> {
                    goal.tick();
                    helper.assertValueEqual(hollow.getTarget(), far, "whoever hits hardest");
                    GameTestPlayers.remove(helper, near);
                    GameTestPlayers.remove(helper, far);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "nightmare_target_prey", timeoutTicks = 200)
    public static void itSwitchesToNearWeakPrey(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        ServerPlayer weak = playerAt(helper, 1.5, 1.5);
        ServerPlayer hitter = playerAt(helper, 7.5, 7.5);
        hollow.hurt(playerAttack(helper, hitter), 3.0F);
        NightmareTargetGoal goal = new NightmareTargetGoal(hollow);
        helper.assertTrue(goal.canUse(), "fighters in range");
        goal.start();
        helper.assertValueEqual(hollow.getTarget(), hitter, "the one who hit it");
        weak.setHealth(2.0F);
        helper.startSequence()
                .thenExecuteAfter(21, () -> {
                    goal.tick();
                    helper.assertValueEqual(hollow.getTarget(), weak, "near and weak beats a small hit");
                    GameTestPlayers.remove(helper, weak);
                    GameTestPlayers.remove(helper, hitter);
                })
                .thenSucceed();
    }

    /** A stone wall across the arena at x 5, full height: no line of sight between the two halves. */
    private static void wall(GameTestHelper helper, boolean up) {
        for (int y = 0; y < 7; y++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(5, y, z), up ? Blocks.STONE : Blocks.AIR);
            }
        }
    }

    private static ServerPlayer playerAt(GameTestHelper helper, double x, double z) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, 0.0, z));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return player;
    }

    private static DamageSource playerAttack(GameTestHelper helper, Player player) {
        return helper.getLevel().damageSources().playerAttack(player);
    }
}
