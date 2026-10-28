package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.NightmareDirectorConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareScaling;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirector;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirectorData;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareRings;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareState;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareThreat;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareThreats;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.jetbrains.annotations.Nullable;

/**
 * The Nightmare director (phase 4 steps 2 and 3): the threat per player and ring, whom it comes for, defeat and return
 * with more strength, retreat and a missing entity back to virtual, a stale directed copy removing itself, the cooldown
 * after an arrival without room, and the health above vanilla's cap of 1024. The
 * GameTest world is no ring world, so each test drives a director of its own ({@link NightmareDirector#forTest}) whose
 * "ring 1" is exactly the test's bounds (everything outside is the void), with its own fresh data and an arrival of 3
 * ticks of omens that puts The Hollow, without free will, in the middle of the empty {@code gametest/hollow_arena}.
 * The tests with a Hollow share a batch of their own, so no player of another test counts in its fight; health is
 * compared against its own fighter count anyway.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightmareDirectorGameTests {
    private static final String BATCH = "nightmare_director";
    private static final BlockPos MIDDLE = new BlockPos(4, 0, 4);
    private static final int OMEN_TICKS = 3;
    private static final float EPSILON = 0.01F;

    private NightmareDirectorGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void threatRisesAtItsDrawnPaceOnlyForFightersAndDropsToZeroAfterAVisit(GameTestHelper helper) {
        NightmareDirectorConfig config = ServerConfig.NIGHTMARE_DIRECTOR;
        float threshold = config.threatThreshold.get().floatValue();
        double[] days = config.threatDays();
        ServerPlayer survivor = playerAt(helper, 2.5, 2.5);
        ServerPlayer creative = helper.makeMockServerPlayerInLevel();

        NightmareThreats.rise(survivor, Ring.RING_2, 600);
        NightmareThreat threat = NightmareThreats.of(survivor);
        float drawn = threat.days(Ring.RING_2);
        helper.assertTrue(drawn >= days[0] - EPSILON && drawn <= days[1] + EPSILON, "pace " + drawn + " days");
        float expected = threshold * 600.0F / (drawn * NightmareDirectorConfig.DAY_SECONDS);
        helper.assertTrue(Math.abs(threat.get(Ring.RING_2) - expected) < EPSILON,
                "ring 2 threat " + threat.get(Ring.RING_2) + ", expected " + expected);
        helper.assertValueEqual(threat.get(Ring.RING_3), 0.0F, "the other rings stay");

        NightmareThreats.rise(creative, Ring.RING_2, 600);
        helper.assertValueEqual(NightmareThreats.of(creative).get(Ring.RING_2), 0.0F, "creative gains nothing");

        NightmareThreats.rise(survivor, Ring.RING_2, 10_000_000);
        helper.assertTrue(Math.abs(threat.get(Ring.RING_2) - 2.0F * threshold) < EPSILON, "capped at twice the threshold");
        helper.assertValueEqual(threat.days(Ring.RING_2), drawn, "the pace stays during one climb");

        Tag saved = NightmareThreat.CODEC.encodeStart(NbtOps.INSTANCE, threat).getOrThrow();
        NightmareThreat loaded = NightmareThreat.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertValueEqual(loaded.get(Ring.RING_2), threat.get(Ring.RING_2), "saved threat");
        helper.assertValueEqual(loaded.days(Ring.RING_2), drawn, "saved pace");

        threat.reset(Ring.RING_2);
        helper.assertValueEqual(threat.get(Ring.RING_2), 0.0F, "after a visit");
        helper.assertValueEqual(threat.days(Ring.RING_2), 0.0F, "a new pace is drawn next time");
        GameTestPlayers.remove(helper, survivor);
        GameTestPlayers.remove(helper, creative);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void itComesForTheFighterInItsRingWithTheMostThreatAboveTheThreshold(GameTestHelper helper) {
        float threshold = ServerConfig.NIGHTMARE_DIRECTOR.threatThreshold.get().floatValue();
        NightmareDirectorData data = new NightmareDirectorData();
        NightmareDirector director = NightmareDirector.forTest(rings(helper), data, kind -> new TestArrival(helper));
        ServerPlayer most = playerAt(helper, 2.5, 2.5);
        ServerPlayer less = playerAt(helper, 6.5, 6.5);
        ServerPlayer creative = helper.makeMockServerPlayerInLevel();
        Vec3 inside = helper.absoluteVec(new Vec3(4.5, 0.0, 4.5));
        creative.moveTo(inside.x, inside.y, inside.z);
        ServerPlayer outside = playerAt(helper, -40.5, 4.5);
        NightmareThreats.of(most).set(Ring.RING_1, threshold * 0.9F);
        NightmareThreats.of(less).set(Ring.RING_1, threshold * 0.8F);
        ServerLevel level = helper.getLevel();
        NightmareState state = data.get(NightmareKind.HOLLOW);

        director.tick(level, 0);
        helper.assertTrue(state.phase() == NightmareState.Phase.ROAMING, "below the threshold nobody is chosen");
        helper.assertTrue(state.placed(), "but it got a virtual position");

        NightmareThreats.of(most).set(Ring.RING_1, threshold * 1.5F);
        NightmareThreats.of(less).set(Ring.RING_1, threshold * 1.2F);
        NightmareThreats.of(creative).set(Ring.RING_1, threshold * 1.9F);
        NightmareThreats.of(outside).set(Ring.RING_1, threshold * 1.9F);
        NightmareThreats.of(most).set(Ring.RING_2, threshold * 1.9F);
        director.tick(level, 1);
        helper.assertTrue(state.phase() == NightmareState.Phase.ROAMING, "it only looks once a second");
        director.tick(level, 20);
        helper.assertTrue(state.phase() == NightmareState.Phase.COMING, "coming: " + state.phase());
        helper.assertValueEqual(state.target(), most.getUUID(), "the target");
        helper.assertValueEqual(state.place(), most.blockPosition(), "the place");

        for (ServerPlayer player : new ServerPlayer[]{most, less, creative, outside}) {
            GameTestPlayers.remove(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 100)
    public static void aDefeatStartsTheReturnTimerAndItComesBackStronger(GameTestHelper helper) {
        NightmareDirectorConfig config = ServerConfig.NIGHTMARE_DIRECTOR;
        NightmareConfig hollowConfig = ServerConfig.HOLLOW.common;
        float threshold = config.threatThreshold.get().floatValue();
        NightmareDirectorData data = new NightmareDirectorData();
        TestArrival arrival = new TestArrival(helper);
        NightmareDirector director = NightmareDirector.forTest(rings(helper), data, kind -> arrival);
        ServerLevel level = helper.getLevel();
        NightmareState state = data.get(NightmareKind.HOLLOW);
        ServerPlayer player = playerAt(helper, 2.5, 2.5);

        NightmareThreats.of(player).set(Ring.RING_1, threshold);
        Nightmare first = arriveFrom(helper, director, state, 0);
        helper.assertValueEqual(arrival.omens, OMEN_TICKS, "omen ticks played");
        helper.assertTrue(first.isDirected(), "directed");
        helper.assertValueEqual(first.strength(), 1.0F, "first strength");
        helper.assertValueEqual(state.entity(), first.getUUID(), "followed entity");
        helper.assertValueEqual(NightmareThreats.of(player).get(Ring.RING_1), 0.0F, "threat after the visit");
        helper.assertTrue(director.isCurrent(first), "it is the current one");

        long killedAt = level.getGameTime();
        first.kill();
        helper.assertTrue(state.phase() == NightmareState.Phase.DEFEATED, "defeated: " + state.phase());
        helper.assertValueEqual(state.defeats(), 1, "defeats");
        helper.assertValueEqual(state.defeatedUntil(), killedAt + config.returnTicks(), "back at");

        NightmareThreats.of(player).set(Ring.RING_1, threshold * 2.0F);
        director.tick(level, state.defeatedUntil() - 1);
        helper.assertTrue(state.phase() == NightmareState.Phase.DEFEATED, "still defeated just before");
        director.tick(level, state.defeatedUntil());
        helper.assertTrue(state.phase() == NightmareState.Phase.ROAMING, "roams again at the end: " + state.phase());

        long next = (state.defeatedUntil() / 20 + 1) * 20;
        Nightmare second = arriveFrom(helper, director, state, next);
        float strength = 1.0F + config.strengthPerReturn.get().floatValue();
        helper.assertValueEqual(second.strength(), strength, "strength after one defeat");
        assertStrength(helper, second, hollowConfig, strength);
        helper.assertValueEqual(second.getHealth(), second.getMaxHealth(), "it comes at full health");

        second.discard();
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 100)
    public static void aRetreatOrALostEntityGoesBackToVirtualWithoutADefeat(GameTestHelper helper) {
        float threshold = ServerConfig.NIGHTMARE_DIRECTOR.threatThreshold.get().floatValue();
        int missing = ServerConfig.NIGHTMARE_DIRECTOR.missingSeconds.get();
        NightmareDirectorData data = new NightmareDirectorData();
        NightmareDirector director = NightmareDirector.forTest(rings(helper), data, kind -> new TestArrival(helper));
        ServerLevel level = helper.getLevel();
        NightmareState state = data.get(NightmareKind.HOLLOW);
        ServerPlayer player = playerAt(helper, 2.5, 2.5);
        ServerPlayer other = playerAt(helper, 6.5, 6.5);

        NightmareThreats.of(player).set(Ring.RING_1, threshold);
        Nightmare first = arriveFrom(helper, director, state, 0);
        NightmareThreats.of(other).set(Ring.RING_1, threshold * 2.0F);
        NightmareThreats.of(player).set(Ring.RING_1, threshold * 2.0F);
        director.tick(level, 1000);
        helper.assertTrue(state.phase() == NightmareState.Phase.PRESENT, "never a second one while it is here");
        helper.assertTrue(director.arrive(level, NightmareKind.HOLLOW, other) == NightmareDirector.ArriveResult.ALREADY_PRESENT,
                "nor by command");

        first.retreat();
        helper.assertTrue(first.isRemoved(), "gone");
        helper.assertTrue(state.phase() == NightmareState.Phase.ROAMING, "roaming after the retreat: " + state.phase());
        helper.assertValueEqual(state.defeats(), 0, "a retreat is no defeat");

        Nightmare second = arriveFrom(helper, director, state, 2000);
        // As if its chunk unloaded: the director no longer finds it.
        second.discard();
        for (int s = 1; s < missing; s++) {
            director.tick(level, 3000 + s * 20L);
        }
        helper.assertTrue(state.phase() == NightmareState.Phase.PRESENT, "still waiting for it");
        director.tick(level, 3000 + missing * 20L);
        helper.assertTrue(state.phase() == NightmareState.Phase.ROAMING, "lost: back to virtual, " + state.phase());
        helper.assertValueEqual(state.defeats(), 0, "a lost one is no defeat");

        GameTestPlayers.remove(helper, player);
        GameTestPlayers.remove(helper, other);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 100)
    public static void strengthMultipliesHealthAndDamageOnTopOfThePlayersAndAStaleCopyLeaves(GameTestHelper helper) {
        NightmareDirector director = NightmareDirector.forTest(rings(helper), new NightmareDirectorData(),
                kind -> new TestArrival(helper));
        ServerPlayer first = playerAt(helper, 2.5, 2.5);
        ServerPlayer second = playerAt(helper, 6.5, 6.5);
        Nightmare hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        hollow.direct(director, 1.5F);
        assertStrength(helper, hollow, ServerConfig.HOLLOW.common, 1.5F);
        helper.assertTrue(hollow.fighters().size() >= 2, "both players count in the fight");
        helper.assertValueEqual(hollow.getHealth(), hollow.getMaxHealth(), "full health");
        // The director never sent it (its state is roaming), so at its next tick it leaves as a stale copy.
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.isRemoved(), "the stale copy removed itself");
            GameTestPlayers.remove(helper, first);
            GameTestPlayers.remove(helper, second);
        });
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void withoutRoomItGivesUpAndWaitsTheCooldownBeforeNewOmens(GameTestHelper helper) {
        NightmareDirectorConfig config = ServerConfig.NIGHTMARE_DIRECTOR;
        float threshold = config.threatThreshold.get().floatValue();
        int tries = config.appearRetrySeconds.get();
        long cooldown = config.failedArrivalCooldownSeconds.get() * 20L;
        NightmareDirectorData data = new NightmareDirectorData();
        TestArrival arrival = new TestArrival(helper);
        arrival.room = false;
        NightmareDirector director = NightmareDirector.forTest(rings(helper), data, kind -> arrival);
        ServerLevel level = helper.getLevel();
        NightmareState state = data.get(NightmareKind.HOLLOW);
        ServerPlayer player = playerAt(helper, 2.5, 2.5);
        NightmareThreats.of(player).set(Ring.RING_1, threshold * 1.5F);

        director.tick(level, 0);
        helper.assertTrue(state.phase() == NightmareState.Phase.COMING, "coming");
        long now = 1;
        for (; now < 1 + OMEN_TICKS + tries * 20L && state.phase() == NightmareState.Phase.COMING; now++) {
            director.tick(level, now);
        }
        helper.assertTrue(state.phase() == NightmareState.Phase.ROAMING, "gave up: " + state.phase());
        helper.assertValueEqual(arrival.cancels, 1, "the omens were cancelled");
        helper.assertTrue(NightmareThreats.of(player).get(Ring.RING_1) >= threshold, "the threat stays");
        long nextSecond = (now / 20 + 1) * 20;
        director.tick(level, nextSecond);
        helper.assertTrue(state.phase() == NightmareState.Phase.ROAMING, "no new omens during the cooldown");
        long after = ((state.nextSeekTick() + 19) / 20) * 20;
        helper.assertTrue(after - nextSecond >= cooldown - 40, "cooldown " + (after - nextSecond));
        director.tick(level, after);
        helper.assertTrue(state.phase() == NightmareState.Phase.COMING, "after the cooldown it comes again: " + state.phase());
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 100)
    public static void aboveTheHealthCapItTakesTheHitsOfItsFullHealth(GameTestHelper helper) {
        NightmareDirector director = NightmareDirector.forTest(rings(helper), new NightmareDirectorData(),
                kind -> new TestArrival(helper));
        Nightmare hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        NightmareConfig config = ServerConfig.HOLLOW.common;
        // 300 × 4 = 1200 with one fighter or none: above vanilla's cap of 1024.
        hollow.direct(director, 4.0F);
        double full = NightmareScaling.maxHealth(config.baseHealth.get(), config.healthPerExtraPlayer.get(),
                hollow.fighters().size()) * 4.0;
        helper.assertTrue(full > 1024.0, "the test needs a value above the cap: " + full);
        helper.assertValueEqual(hollow.getMaxHealth(), 1024.0F, "the real attribute is capped");
        helper.assertTrue(Math.abs(hollow.virtualMaxHealth() - full) < EPSILON, "virtual " + hollow.virtualMaxHealth());
        helper.assertTrue(Math.abs(hollow.virtualHealth() - full) < 0.1, "full virtual health " + hollow.virtualHealth());
        helper.assertTrue(hollow.hurt(helper.getLevel().damageSources().magic(), 120.0F), "the hit lands");
        helper.assertTrue(Math.abs(hollow.virtualHealth() - (full - 120.0)) < 0.1,
                "virtual health after 120 damage: " + hollow.virtualHealth());
        double ratio = hollow.getHealth() / hollow.getMaxHealth();
        helper.assertTrue(Math.abs(ratio - (full - 120.0) / full) < 1.0e-4, "the boss bar ratio: " + ratio);
        hollow.discard();
        helper.succeed();
    }

    /** Ticks the director from {@code from} until the Hollow is here; fails after a few seconds of game time. */
    private static Nightmare arriveFrom(GameTestHelper helper, NightmareDirector director, NightmareState state, long from) {
        ServerLevel level = helper.getLevel();
        for (long now = from; now < from + 200; now++) {
            director.tick(level, now);
            if (state.phase() == NightmareState.Phase.PRESENT) {
                if (level.getEntity(state.entity()) instanceof Nightmare nightmare) {
                    return nightmare;
                }
                helper.fail("present without an entity");
            }
        }
        helper.fail("it never came: " + state.phase());
        throw new IllegalStateException();
    }

    private static void assertStrength(GameTestHelper helper, Nightmare nightmare, NightmareConfig config, float strength) {
        double maxHealth = NightmareScaling.maxHealth(config.baseHealth.get(), config.healthPerExtraPlayer.get(),
                nightmare.fighters().size()) * strength;
        helper.assertTrue(Math.abs(nightmare.getMaxHealth() - maxHealth) < EPSILON,
                "max health " + nightmare.getMaxHealth() + ", expected " + maxHealth);
        double damage = config.attackDamage.get() * strength;
        double actual = nightmare.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs(actual - damage) < EPSILON, "attack damage " + actual + ", expected " + damage);
    }

    /** Ring 1 is exactly the test's bounds; outside lies the void. */
    private static NightmareRings rings(GameTestHelper helper) {
        AABB bounds = helper.getBounds();
        return new NightmareRings() {
            @Override
            public Ring ringAt(double x, double z) {
                return x >= bounds.minX && x <= bounds.maxX && z >= bounds.minZ && z <= bounds.maxZ ? Ring.RING_1 : Ring.VOID;
            }

            @Override
            public double innerRadius(Ring ring) {
                return 0.0;
            }

            @Override
            public double outerRadius(Ring ring) {
                return 1.0;
            }
        };
    }

    private static ServerPlayer playerAt(GameTestHelper helper, double x, double z) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, 0.0, z));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return player;
    }

    /** Three ticks of omens, counted; then The Hollow without free will in the middle of the arena. */
    private static final class TestArrival implements NightmareArrival {
        private final GameTestHelper helper;
        int omens;
        int cancels;
        boolean room = true;

        TestArrival(GameTestHelper helper) {
            this.helper = helper;
        }

        @Override
        public int omenTicks() {
            return OMEN_TICKS;
        }

        @Override
        public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
            omens++;
        }

        @Override
        @Nullable
        public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target,
                                BlockPos place, float strength) {
            return room ? helper.spawnWithNoFreeWill(type, MIDDLE) : null;
        }

        @Override
        public void cancelled(ServerLevel level, BlockPos place) {
            cancels++;
        }
    }
}
