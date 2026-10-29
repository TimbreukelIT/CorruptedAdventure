package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirector;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirectorData;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareRings;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareState;
import nl.timtoys.corruptedadventure.entity.risen.RisenAnimations;
import nl.timtoys.corruptedadventure.entity.risen.RisenArrival;
import nl.timtoys.corruptedadventure.entity.risen.RisenState;
import nl.timtoys.corruptedadventure.entity.risen.RisenTrace;
import nl.timtoys.corruptedadventure.entity.risen.TheRisen;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.List;

/**
 * The Risen's own arrival (phase 4 step 2, {@link RisenArrival}), through a director of its own
 * ({@link NightmareDirector#forTest}, "ring 3" = the test's bounds, the omens ticked through at once like
 * {@code /ca nightmare arrive}): under the open sky it passes high over its target and then dives; under a roof the
 * ground in front of its target melts (no lava where the sky is open, a player's block untouched) and it rises, and the
 * pool sets to a scar without lava. Also: only free-standing ash is a cast, the omens change no block, and the roaming
 * traces are capped and change no block. In the empty 9 × 7 × 9 {@code gametest/hollow_arena}. How it looks is
 * checked in the client test.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RisenArrivalGameTests {
    /** Its own batch: no player of another test is near enough to count in its fight (the two in it look up their own). */
    private static final String BATCH = "risen_arrival";
    /** Ticks for the light to settle after the test built its roof, or for forced chunks to start ticking. */
    private static final int LIGHT_TICKS = 20;
    /**
     * Chunks round the arena kept ticking for the pass: no real player holds them in the test world, and outside them
     * it would stand still (passDistance 40 + passBeyond 16).
     */
    private static final int PASS_CHUNKS = 4;

    private RisenArrivalGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 400)
    public static void underTheOpenSkyItPassesHighOverItsTargetAndDives(GameTestHelper helper) {
        floor(helper, Blocks.GRASS_BLOCK);
        ServerPlayer player = playerAt(helper, 4.5, 1.0, 4.5, 0.0F);
        forceChunks(helper, true);
        helper.startSequence()
                .thenExecuteAfter(LIGHT_TICKS, () -> {
                    helper.assertTrue(RisenArrival.underOpenSky(helper.getLevel(), player), "the test needs open sky");
                    TheRisen risen = arrive(helper, player);
                    helper.assertTrue(risen.state() == RisenState.PASSING, "it passes over: " + risen.state());
                    helper.assertFalse(risen.isInvisible(), "seen in the sky");
                    helper.assertTrue(risen.getY() > player.getY() + 10.0, "high up: " + (risen.getY() - player.getY()));
                    helper.assertTrue(risen.getTarget() == player, "its target");
                })
                .thenWaitUntil(() -> {
                    TheRisen risen = risen(helper, player);
                    helper.assertTrue(risen.state() == RisenState.DIVING, "it dives: " + risen.state() + " at "
                            + risen.position().subtract(player.position()) + ", ticks " + risen.tickCount + ", ticking "
                            + helper.getLevel().isPositionEntityTicking(risen.blockPosition()));
                })
                .thenExecute(() -> {
                    risen(helper, player).discard();
                    GameTestPlayers.remove(helper, player);
                    forceChunks(helper, false);
                })
                .thenSucceed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 200)
    public static void underARoofTheGroundMeltsItRisesAndThePoolSetsToAScar(GameTestHelper helper) {
        // A two-thick stone floor, a stone roof with one hole over the pool, a player's plank in the pool.
        floor(helper, Blocks.STONE);
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
                helper.setBlock(x, 6, z, Blocks.STONE);
            }
        }
        BlockPos skyCell = new BlockPos(6, 1, 4);
        helper.setBlock(skyCell.getX(), 6, skyCell.getZ(), Blocks.AIR);
        BlockPos plank = new BlockPos(3, 1, 5);
        helper.setBlock(plank, Blocks.OAK_PLANKS);
        BlockPos centre = new BlockPos(4, 1, 4);
        // In the corner, looking to the middle: 6 blocks in front is the middle of the arena.
        ServerPlayer player = playerAt(helper, 0.5, 2.0, 0.5, -45.0F);
        helper.startSequence()
                .thenExecuteAfter(LIGHT_TICKS, () -> {
                    helper.assertFalse(RisenArrival.underOpenSky(helper.getLevel(), player), "the test needs a roof");
                    TheRisen risen = arrive(helper, player);
                    helper.assertTrue(risen.state() == RisenState.RISING, "it rises: " + risen.state());
                    helper.assertTrue(risen.isInvisible(), "still under the ground");
                    helper.assertBlockPresent(Blocks.MAGMA_BLOCK, centre);
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, plank);
                    helper.assertTrue(player.distanceToSqr(risen.getX(), player.getY(), risen.getZ()) >= 25.0,
                            "not under its target");
                })
                .thenExecuteAfter(RisenAnimations.RISE_GLOW_TICKS + 5, () -> {
                    helper.assertBlockPresent(Blocks.LAVA, centre);
                    helper.assertBlockPresent(Blocks.MAGMA_BLOCK, skyCell);
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, plank);
                    TheRisen risen = risen(helper, player);
                    helper.assertFalse(risen.isInvisible(), "it rises out of the pool");
                    // Gone before the pool set by itself: it sets now.
                    risen.discard();
                    for (int x = 0; x < 9; x++) {
                        for (int z = 0; z < 9; z++) {
                            helper.assertBlockNotPresent(Blocks.LAVA, x, 1, z);
                            if (x != skyCell.getX() || z != skyCell.getZ()) {
                                helper.assertBlockPresent(Blocks.STONE, x, 6, z);
                            }
                        }
                    }
                    BlockState scar = helper.getBlockState(centre);
                    helper.assertTrue(scar.is(Blocks.OBSIDIAN) || scar.is(Blocks.BASALT) || scar.is(Blocks.MAGMA_BLOCK),
                            "a scar: " + scar);
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, plank);
                    GameTestPlayers.remove(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void onlyFreeStandingAshIsACastAndTheOmensChangeNoBlock(GameTestHelper helper) {
        floor(helper, ModBlocks.SMOLDERING_ASH.get());
        // A kneeling figure two high, and one lying down two long.
        helper.setBlock(2, 1, 2, ModBlocks.ASH_BLOCK.get());
        helper.setBlock(2, 2, 2, ModBlocks.SMOLDERING_ASH.get());
        helper.setBlock(6, 1, 6, ModBlocks.ASH_BLOCK.get());
        helper.setBlock(7, 1, 6, ModBlocks.ASH_BLOCK.get());
        ServerLevel level = helper.getLevel();
        List<BlockPos> casts = RisenArrival.castsNear(level, helper.absolutePos(new BlockPos(4, 1, 4)), 4.0);
        helper.assertValueEqual(casts.size(), 2, "casts " + casts);
        helper.assertTrue(casts.contains(helper.absolutePos(new BlockPos(2, 2, 2))), "the kneeling one smokes at its top");

        ServerPlayer player = playerAt(helper, 4.5, 1.0, 4.5, 0.0F);
        RisenArrival arrival = new RisenArrival();
        BlockPos place = player.blockPosition();
        for (int tick = 0; tick < 60; tick++) {
            arrival.omen(level, player, place, tick);
        }
        arrival.cancelled(level, place);
        helper.assertBlockPresent(ModBlocks.SMOLDERING_ASH.get(), 2, 2, 2);
        helper.assertBlockPresent(ModBlocks.ASH_BLOCK.get(), 7, 1, 6);
        helper.assertBlockPresent(ModBlocks.SMOLDERING_ASH.get(), 4, 0, 4);
        helper.assertEntityNotPresent(ModEntityTypes.THE_RISEN.get());
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void roamingTracesAreCappedAndChangeNoBlock(GameTestHelper helper) {
        floor(helper, Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        RisenConfig config = ServerConfig.RISEN;
        int max = config.traceMax.get();
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
        for (int i = 0; i <= max; i++) {
            boolean left = RisenTrace.leave(level, at, config);
            helper.assertTrue(left == i < max, "trace " + i + " left: " + left);
        }
        List<? extends RisenTrace> traces = level.getEntities(ModEntityTypes.RISEN_TRACE.get(), trace -> true);
        helper.assertValueEqual(traces.size(), max, "traces");
        helper.assertTrue(traces.get(0).blockPosition().getX() == at.getX() && traces.get(0).blockPosition().getZ() == at.getZ(),
                "at its column: " + traces.get(0).blockPosition());
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, 4, 0, 4);
        traces.forEach(RisenTrace::discard);
        helper.succeed();
    }

    /** Ticks a director of its own through the omens (at once, like the command) until The Risen is here. */
    private static TheRisen arrive(GameTestHelper helper, ServerPlayer player) {
        NightmareDirectorData data = new NightmareDirectorData();
        // One arrival for every tick, like the registered one: its casts are remembered per place.
        RisenArrival arrival = new RisenArrival();
        NightmareDirector director = NightmareDirector.forTest(rings(helper), data, kind -> arrival);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(director.arrive(level, NightmareKind.RISEN, player) == NightmareDirector.ArriveResult.COMING,
                "coming");
        NightmareState state = data.get(NightmareKind.RISEN);
        int limit = ServerConfig.RISEN.omenSeconds.get() * 20 + 100;
        for (long now = 0; now < limit; now++) {
            director.tick(level, now);
            if (state.phase() == NightmareState.Phase.PRESENT) {
                if (level.getEntity(state.entity()) instanceof TheRisen risen) {
                    return risen;
                }
                helper.fail("present without an entity");
            }
        }
        helper.fail("it never came: " + state.phase());
        throw new IllegalStateException();
    }

    /** The Risen after {@code player}: the other test of the batch has its own, near enough to be found too. */
    private static TheRisen risen(GameTestHelper helper, ServerPlayer player) {
        // By class: a lookup by type also finds its parts (their type is the Risen's).
        List<TheRisen> found = helper.getLevel().getEntitiesOfClass(TheRisen.class, helper.getBounds().inflate(128.0),
                risen -> risen.isAlive() && risen.getTarget() == player);
        if (found.isEmpty()) {
            helper.fail("no Risen");
        }
        return found.get(0);
    }

    /** Ring 3 is exactly the test's bounds; outside lies the void. */
    private static NightmareRings rings(GameTestHelper helper) {
        AABB bounds = helper.getBounds();
        return new NightmareRings() {
            @Override
            public Ring ringAt(double x, double z) {
                return x >= bounds.minX && x <= bounds.maxX && z >= bounds.minZ && z <= bounds.maxZ ? Ring.RING_3 : Ring.VOID;
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

    private static ServerPlayer playerAt(GameTestHelper helper, double x, double y, double z, float yaw) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        player.setYHeadRot(yaw);
        return player;
    }

    private static void forceChunks(GameTestHelper helper, boolean forced) {
        BlockPos middle = helper.absolutePos(new BlockPos(4, 0, 4));
        int cx = middle.getX() >> 4;
        int cz = middle.getZ() >> 4;
        for (int x = cx - PASS_CHUNKS; x <= cx + PASS_CHUNKS; x++) {
            for (int z = cz - PASS_CHUNKS; z <= cz + PASS_CHUNKS; z++) {
                helper.getLevel().setChunkForced(x, z, forced);
            }
        }
    }

    private static void floor(GameTestHelper helper, Block block) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 0, z, block);
            }
        }
    }
}
