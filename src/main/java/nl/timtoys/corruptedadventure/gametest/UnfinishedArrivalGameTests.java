package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedArrivalConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirector;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirectorData;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareRings;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareState;
import nl.timtoys.corruptedadventure.entity.unfinished.TheUnfinished;
import nl.timtoys.corruptedadventure.entity.unfinished.UnfinishedArrival;
import nl.timtoys.corruptedadventure.entity.unfinished.UnfinishedTempBlocks;
import nl.timtoys.corruptedadventure.entity.unfinished.UnfinishedTraces;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.jetbrains.annotations.Nullable;

/**
 * The Unfinished's arrival (phase 4 step 2, A5) through a director of its own whose ring 5 is the test's bounds: he climbs
 * out of the torn ground over the rim toward his target; the tear, open down to the void, loads back in with a shallow
 * void scar in its middle (voidstone floor, a rim of voidstone or corrupted dirt), only in whole natural columns (a build
 * and a chest in it stay). The arrival's own omens run, but shortened to {@link #OMEN_TICKS} and with the
 * place in the arena (its own place lies 24 blocks behind the target, outside the test's bounds). He has no free will
 * after he appears. In the empty {@code gametest/hollow_arena} (9 x 7 x 9) with a grass floor at y 0, without the barrier
 * roof (a column under a barrier may not tear).
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UnfinishedArrivalGameTests {
    private static final String BATCH = "unfinished_arrival";
    private static final int OMEN_TICKS = 3;
    /** The place: the tear runs across x (the target stands to the south), 9 long and 5 wide round it. */
    private static final BlockPos PLACE = new BlockPos(4, 1, 2);
    private static final BlockPos SCAR_MIDDLE = new BlockPos(4, 0, 2);
    private static final BlockPos SCAR_EDGE = new BlockPos(2, 0, 1);
    /** Just outside the scar along his way: its rim. */
    private static final BlockPos RIM = new BlockPos(4, 0, 0);
    private static final BlockPos TEAR_ONLY = new BlockPos(8, 0, 2);
    private static final BlockPos BUILD = new BlockPos(3, 1, 2);
    private static final BlockPos CHEST = new BlockPos(1, 1, 2);

    private UnfinishedArrivalGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 1000, skyAccess = true)
    public static void theDirectedArrivalTearsTheGroundAndLeavesAShallowScarOnlyInNaturalGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UnfinishedArrivalConfig config = ServerConfig.UNFINISHED.arrival;
        floor(helper);
        helper.setBlock(BUILD, Blocks.OAK_PLANKS);
        helper.setBlock(CHEST, Blocks.CHEST);
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 1.0, 8.5));
        player.moveTo(at.x, at.y, at.z, 180.0F, 0.0F);
        NightmareDirectorData data = new NightmareDirectorData();
        NightmareDirector director = NightmareDirector.forTest(rings(helper), data,
                kind -> new InArena(new UnfinishedArrival(), helper.absolutePos(PLACE)));
        NightmareState state = data.get(NightmareKind.UNFINISHED);
        helper.assertTrue(director.arrive(level, NightmareKind.UNFINISHED, player) == NightmareDirector.ArriveResult.COMING,
                "his omens start");
        TheUnfinished boss = arrive(helper, director, state);
        // No free will once he is here: his attacks would change the ground the checks below look at.
        boss.setNoAi(true);
        helper.assertTrue(boss.isClimbing(), "he climbs out");
        helper.assertTrue(boss.getY() < helper.absoluteVec(Vec3.ZERO).y, "from deep in the tear");
        int open = config.tearOpenSeconds.get() * 20;
        int climb = config.climbTicks.get();
        helper.startSequence()
                .thenExecuteAfter(climb + 5, () -> {
                    helper.assertFalse(boss.isClimbing(), "he is out");
                    Vec3 rel = helper.relativeVec(boss.position());
                    helper.assertTrue(rel.z > PLACE.getZ() + 3.0, "over the rim toward the target: " + rel);
                    helper.assertTrue(rel.y > 0.99, "on the ground, not in the tear: " + rel);
                    assertOpenToTheVoid(helper, TEAR_ONLY);
                    assertOpenToTheVoid(helper, SCAR_MIDDLE);
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, BUILD);
                    helper.assertBlockPresent(Blocks.GRASS_BLOCK, BUILD.below());
                    helper.assertBlockPresent(Blocks.CHEST, CHEST);
                })
                .thenExecuteAfter(open + 40 - climb, () -> {
                    helper.assertBlockPresent(Blocks.GRASS_BLOCK, TEAR_ONLY);
                    assertShallowScar(helper, SCAR_MIDDLE, config.scarDepth.get());
                    assertShallowScar(helper, SCAR_EDGE, config.scarDepth.get());
                    BlockState rim = helper.getBlockState(RIM);
                    helper.assertTrue(rim.is(ModBlocks.VOIDSTONE.get()) || rim.is(ModBlocks.CORRUPTED_DIRT.get()),
                            "the rim: " + rim);
                    helper.assertFalse(UnfinishedTempBlocks.of(level).isTracked(helper.absolutePos(TEAR_ONLY)),
                            "the tear is mended");
                    boss.discard();
                    GameTestPlayers.remove(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH, timeoutTicks = 100)
    public static void cancelledOmensLeaveTheGroundWhole(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 1.0, 8.5));
        player.moveTo(at.x, at.y, at.z, 180.0F, 0.0F);
        UnfinishedArrival arrival = new UnfinishedArrival();
        BlockPos place = helper.absolutePos(PLACE);
        int total = arrival.omenTicks();
        // Only the last omen ticks: the flicker of the tear.
        for (int tick = total - 10; tick < total; tick++) {
            arrival.omen(level, player, place, tick);
        }
        arrival.cancelled(level, place);
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(x, 0, z));
            }
        }
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = BATCH)
    public static void roamingTracesAreBounded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UnfinishedArrival arrival = new UnfinishedArrival();
        BlockPos ground = helper.absolutePos(new BlockPos(4, 1, 4));
        for (int i = 0; i < 200; i++) {
            arrival.roamed(level, ground, ground);
        }
        int max = ServerConfig.UNFINISHED.arrival.traceMax.get();
        helper.assertTrue(UnfinishedTraces.count() > 0, "the rain hangs still somewhere");
        helper.assertTrue(UnfinishedTraces.count() <= max, "at most " + max + " traces: " + UnfinishedTraces.count());
        helper.succeed();
    }

    /** Ticks the director until he is here; fails after a few seconds of game time. */
    private static TheUnfinished arrive(GameTestHelper helper, NightmareDirector director, NightmareState state) {
        ServerLevel level = helper.getLevel();
        for (long now = 0; now < 200; now++) {
            director.tick(level, now);
            if (state.phase() == NightmareState.Phase.PRESENT) {
                if (level.getEntity(state.entity()) instanceof TheUnfinished boss) {
                    return boss;
                }
                helper.fail("present without The Unfinished");
            }
        }
        helper.fail("he never came: " + state.phase());
        throw new IllegalStateException();
    }

    /** The column at {@code rel} is air from the floor down to the bottom of the world. */
    private static void assertOpenToTheVoid(GameTestHelper helper, BlockPos rel) {
        ServerLevel level = helper.getLevel();
        BlockPos top = helper.absolutePos(rel);
        for (int y = top.getY(); y >= level.getMinBuildHeight(); y--) {
            BlockPos pos = top.atY(y);
            if (!level.getBlockState(pos).isAir()) {
                helper.fail("the scar is not open at " + pos + ": " + level.getBlockState(pos));
            }
        }
    }

    /**
     * The column at {@code rel} (its top on y 0) is a shallow scar: air down to its voidstone floor {@code depth} below
     * the top (never below the bottom of the world), and the ground under that as it was.
     */
    private static void assertShallowScar(GameTestHelper helper, BlockPos rel, int depth) {
        ServerLevel level = helper.getLevel();
        BlockPos top = helper.absolutePos(rel);
        int floor = Math.max(top.getY() - depth, level.getMinBuildHeight());
        for (int y = top.getY(); y > floor; y--) {
            if (!level.getBlockState(top.atY(y)).isAir()) {
                helper.fail("the scar is not open at " + top.atY(y) + ": " + level.getBlockState(top.atY(y)));
            }
        }
        if (!level.getBlockState(top.atY(floor)).is(ModBlocks.VOIDSTONE.get())) {
            helper.fail("no voidstone floor at " + top.atY(floor) + ": " + level.getBlockState(top.atY(floor)));
        }
        for (int y = floor - 1; y >= level.getMinBuildHeight(); y--) {
            if (level.getBlockState(top.atY(y)).isAir()) {
                helper.fail("the scar is open to the void at " + top.atY(y));
            }
        }
    }

    /** Grass at y 0 under the whole arena. */
    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
            }
        }
    }

    /** Ring 5 is exactly the test's bounds; outside lies the void. */
    private static NightmareRings rings(GameTestHelper helper) {
        AABB bounds = helper.getBounds();
        return new NightmareRings() {
            @Override
            public Ring ringAt(double x, double z) {
                return x >= bounds.minX && x <= bounds.maxX && z >= bounds.minZ && z <= bounds.maxZ ? Ring.RING_5 : Ring.VOID;
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

    /** The real arrival, with a short omen and its place in the arena. */
    private record InArena(UnfinishedArrival arrival, BlockPos at) implements NightmareArrival {
        @Override
        public BlockPos place(ServerLevel level, ServerPlayer target) {
            return at;
        }

        @Override
        public int omenTicks() {
            return OMEN_TICKS;
        }

        @Override
        public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
            // The flicker and the standstill of the real omens' last ticks.
            arrival.omen(level, target, place, arrival.omenTicks() - OMEN_TICKS + tick);
        }

        @Override
        @Nullable
        public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                                float strength) {
            return arrival.appear(level, type, target, place, strength);
        }

        @Override
        public void cancelled(ServerLevel level, BlockPos place) {
            arrival.cancelled(level, place);
        }
    }
}
