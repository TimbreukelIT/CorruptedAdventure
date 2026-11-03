package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.lostkeeper.KeeperArrival;
import nl.timtoys.corruptedadventure.entity.lostkeeper.KeeperMistTraces;
import nl.timtoys.corruptedadventure.entity.lostkeeper.TheLostKeeper;
import nl.timtoys.corruptedadventure.entity.nightmare.DefaultNightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirector;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirectorData;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareRings;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareState;
import nl.timtoys.corruptedadventure.ring.Ring;

/**
 * The Lost Keeper's own arrival through the director ({@link KeeperArrival}, as {@code /ca nightmare arrive}): next to
 * big water it rises out of it, without water it appears in the air ahead of its target and swims to them; and the
 * wisps it leaves while it roams stay bounded. The director is a test one whose ring 4 is the test's bounds. The arrival
 * runs without AI afterwards (no fight against the mock player). Water and air in batches of their own: the keeper's
 * 24-block body and the pool reach into neighbouring tests, and the air test must find no water. In
 * {@code gametest/hollow_arena} (an empty 9 × 7 × 9 room); the pools with sky access, or the barrier roof hides the water
 * from the heightmap.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KeeperArrivalGameTests {
    private static final String ARENA = "gametest/hollow_arena";
    /** The pool: water from y 1 to 5 inside stone walls round x and z 1–7. */
    private static final int POOL_TOP = 5;
    /** Ticks after the omens within which it must have come. */
    private static final int APPEAR_TICKS = 60;

    private KeeperArrivalGameTests() {
    }

    @GameTest(template = ARENA, batch = "lost_keeper_arrival_water", timeoutTicks = 100, skyAccess = true)
    public static void nextToBigWaterItRisesOutOfIt(GameTestHelper helper) {
        pool(helper);
        // On the wall's corner, beside the pool.
        ServerPlayer player = playerAt(helper, new Vec3(0.5, POOL_TOP + 1.0, 0.5));
        TheLostKeeper keeper = arrive(helper, player);
        double surface = helper.absolutePos(new BlockPos(0, POOL_TOP, 0)).getY() + 1.0;
        // Not ticked yet, so isInWater() is not set: its head is in the water.
        helper.assertTrue(helper.getLevel().getFluidState(BlockPos.containing(keeper.headCentre())).is(FluidTags.WATER),
                "it starts in the water");
        helper.assertTrue(keeper.headCentre().y < surface - 1.0, "under the surface: " + keeper.headCentre().y);
        helper.assertTrue(helper.getBounds().contains(keeper.position()), "in the pool: " + keeper.position());
        helper.assertTrue(keeper.isArriving() && keeper.isDirected(), "arriving, for the director");
        helper.assertTrue(keeper.getTarget() == player, "after its target");
        keeper.discard();
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "lost_keeper_arrival_air", timeoutTicks = 100)
    public static void withoutWaterItAppearsInTheAirAheadAndSwimsToItsTarget(GameTestHelper helper) {
        // High above the arena: no water within reach of the search, free air all round.
        ServerPlayer player = playerAt(helper, new Vec3(4.5, 40.0, 4.5));
        TheLostKeeper keeper = arrive(helper, player);
        double distance = keeper.position().distanceTo(player.position());
        double air = ServerConfig.LOST_KEEPER.arrivalAirDistance.get();
        helper.assertFalse(helper.getLevel().getFluidState(BlockPos.containing(keeper.headCentre())).is(FluidTags.WATER),
                "not in water");
        helper.assertTrue(distance > air * 0.5 && distance < air + 10.0, "some way ahead in the air: " + distance);
        helper.assertTrue(keeper.getY() > player.getY(), "above its target");
        helper.assertTrue(keeper.isArriving() && keeper.isDirected(), "arriving, for the director");
        keeper.discard();
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "lost_keeper_arrival_water", skyAccess = true)
    public static void itsWispsLieOnTheWaterAndStayBounded(GameTestHelper helper) {
        pool(helper);
        KeeperMistTraces.clear();
        ServerLevel level = helper.getLevel();
        BlockPos over = helper.absolutePos(new BlockPos(4, POOL_TOP + 1, 4));
        int max = ServerConfig.LOST_KEEPER.maxTraces.get();
        for (int k = 0; k < max + 2; k++) {
            helper.assertTrue(KeeperMistTraces.leave(level, over), "a wisp on the water");
        }
        helper.assertValueEqual(KeeperMistTraces.count(), max, "wisps at most");
        // Nothing in the world changed: the water is still water, nothing on it.
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(4, POOL_TOP, 4));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, POOL_TOP + 1, 4));
        KeeperMistTraces.clear();
        helper.succeed();
    }

    /** Starts its omens through the director and ticks it until it is here; fails if it never comes. */
    private static TheLostKeeper arrive(GameTestHelper helper, ServerPlayer player) {
        ServerLevel level = helper.getLevel();
        NightmareDirectorData data = new NightmareDirectorData();
        KeeperArrival arrival = new KeeperArrival();
        NightmareDirector director = NightmareDirector.forTest(rings(helper), data,
                kind -> kind == NightmareKind.LOST_KEEPER ? arrival : DefaultNightmareArrival.INSTANCE);
        helper.assertTrue(director.arrive(level, NightmareKind.LOST_KEEPER, player) == NightmareDirector.ArriveResult.COMING,
                "the omens start");
        NightmareState state = data.get(NightmareKind.LOST_KEEPER);
        int ticks = arrival.omenTicks() + APPEAR_TICKS;
        for (long now = 0; now < ticks; now++) {
            director.tick(level, now);
            if (state.phase() == NightmareState.Phase.PRESENT) {
                if (level.getEntity(state.entity()) instanceof TheLostKeeper keeper) {
                    helper.assertTrue(now >= arrival.omenTicks(), "not before the omens are over: " + now);
                    // Without AI from here: no fight against the mock player.
                    keeper.setNoAi(true);
                    return keeper;
                }
                helper.fail("present without its entity");
            }
            helper.assertTrue(state.phase() == NightmareState.Phase.COMING, "coming: " + state.phase());
        }
        helper.fail("it never came: " + state.phase());
        throw new IllegalStateException();
    }

    /** Stone floor and walls with water 5 deep inside (7 × 7). */
    private static void pool(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                boolean wall = x == 0 || x == 8 || z == 0 || z == 8;
                for (int y = 1; y <= POOL_TOP; y++) {
                    helper.setBlock(x, y, z, wall ? Blocks.STONE : Blocks.WATER);
                }
            }
        }
    }

    private static ServerPlayer playerAt(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(relative);
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return player;
    }

    /** Ring 4 is exactly the test's bounds (and the air above them); outside lies the void. */
    private static NightmareRings rings(GameTestHelper helper) {
        AABB bounds = helper.getBounds();
        return new NightmareRings() {
            @Override
            public Ring ringAt(double x, double z) {
                return x >= bounds.minX && x <= bounds.maxX && z >= bounds.minZ && z <= bounds.maxZ ? Ring.RING_4 : Ring.VOID;
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
}
