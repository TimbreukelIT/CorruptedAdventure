package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.layered.LayeredArrival;
import nl.timtoys.corruptedadventure.entity.layered.LayeredClawMarks;
import nl.timtoys.corruptedadventure.entity.layered.LayeredWallSpot;
import nl.timtoys.corruptedadventure.entity.layered.TheLayered;
import nl.timtoys.corruptedadventure.entity.nightmare.DefaultNightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirector;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirectorData;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareRings;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareState;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.Optional;

/**
 * The Layered's own arrival (phase 4 step 2, {@link LayeredArrival}): through the director it comes loose from the
 * natural wall near its target and unfolds, without a block changing; without a wall it bursts up out of the ground
 * and hits who stands there; its claw marks go on a flat natural wall and fade. In the empty 9 × 7 × 9
 * {@code gametest/hollow_arena}, with the floor and the wall the tests lay themselves. How it looks is checked in the
 * client test.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LayeredArrivalGameTests {
    private static final String ARENA = "gametest/hollow_arena";
    /** The wall the tests build: x 8, y 1–6, the whole depth of the arena. */
    private static final int WALL_X = 8;
    private static final int WALL_TOP = 6;

    private LayeredArrivalGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void throughTheDirectorItUnfoldsFromTheWallAndNoBlockChanges(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        wall(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 4.5));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        NightmareDirectorData data = new NightmareDirectorData();
        LayeredArrival arrival = new LayeredArrival();
        NightmareDirector director = NightmareDirector.forTest(ring2(helper), data,
                kind -> kind == NightmareKind.LAYERED ? arrival : DefaultNightmareArrival.INSTANCE);
        helper.assertTrue(director.arrive(level, NightmareKind.LAYERED, player) == NightmareDirector.ArriveResult.COMING,
                "its omens start");
        NightmareState state = data.get(NightmareKind.LAYERED);
        Nightmare nightmare = null;
        for (long now = 0; now < arrival.omenTicks() + 100 && nightmare == null; now++) {
            director.tick(level, now);
            if (state.phase() == NightmareState.Phase.PRESENT) {
                nightmare = level.getEntity(state.entity()) instanceof Nightmare present ? present : null;
                helper.assertTrue(nightmare != null, "present with an entity");
            }
        }
        helper.assertTrue(nightmare instanceof TheLayered, "The Layered comes: " + state.phase());
        TheLayered layered = (TheLayered) nightmare;
        // Without free will: with it, it would fight the mock player once unfolded.
        layered.setNoAi(true);
        helper.assertTrue(layered.isUnfolding(), "it unfolds");
        helper.assertTrue(layered.getSurface() == Direction.EAST, "on the wall: " + layered.getSurface());
        helper.assertFalse(layered.canAct(), "no attacks while it unfolds");
        helper.assertTrue(layered.getX() > helper.absoluteVec(new Vec3(5.0, 0.0, 0.0)).x, "against the wall, not on the player");
        helper.runAfterDelay(ServerConfig.LAYERED.arrivalUnfoldTicks.get() + 5, () -> {
            helper.assertFalse(layered.isUnfolding(), "unfolded");
            assertWallAndFloor(helper, Blocks.STONE);
            layered.discard();
            GameTestPlayers.remove(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void withoutAWallItBurstsUpOutOfTheGroundAndHitsWhoStandsThere(GameTestHelper helper) {
        floor(helper, Blocks.SAND);
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(0.5, 1.0, 0.5));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        // A pig: a fresh mock player cannot be hurt for its first 3 seconds.
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 5));
        float full = pig.getHealth();
        LayeredArrival arrival = new LayeredArrival();
        Nightmare appeared = arrival.appear(helper.getLevel(), ModEntityTypes.THE_LAYERED.get(), player,
                helper.absolutePos(new BlockPos(4, 1, 4)), 1.0F);
        helper.assertTrue(appeared instanceof TheLayered, "The Layered appears");
        TheLayered layered = (TheLayered) appeared;
        layered.setNoAi(true);
        helper.assertTrue(layered.isBurrowed() && layered.isInvisible(), "first under the ground");
        helper.assertFalse(layered.isUnfolding(), "no wall to unfold from");
        helper.runAfterDelay(15, () -> {
            helper.assertFalse(layered.isBurrowed() || layered.isInvisible(), "it burst up");
            helper.assertTrue(pig.getHealth() < full, "the pig on top of it is hit");
            assertFloor(helper, Blocks.SAND);
            layered.discard();
            GameTestPlayers.remove(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void clawMarksGoOnAFlatNaturalWallAndFade(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        wall(helper);
        ServerLevel level = helper.getLevel();
        Optional<LayeredWallSpot.Face> face = LayeredWallSpot.findFace(level, helper.absoluteVec(new Vec3(5.5, 1.0, 4.5)), 10.0);
        helper.assertTrue(face.isPresent(), "a face on the wall");
        helper.assertTrue(face.get().facing() == Direction.WEST, "the wall's face, looking out: " + face.get().facing());
        helper.assertValueEqual(face.get().block().getX(), helper.absolutePos(new BlockPos(WALL_X, 0, 0)).getX(), "on the wall");
        LayeredClawMarks marks = LayeredClawMarks.place(level, face.get(), 20);
        helper.assertTrue(marks.facing() == Direction.WEST, "marks face out of the wall");
        helper.assertEntityPresent(ModEntityTypes.LAYERED_CLAW_MARKS.get());
        helper.runAfterDelay(25, () -> {
            helper.assertEntityNotPresent(ModEntityTypes.LAYERED_CLAW_MARKS.get());
            assertWallAndFloor(helper, Blocks.STONE);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void aBuiltWallIsNoPlaceToComeFrom(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        for (int y = 1; y <= WALL_TOP; y++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(WALL_X, y, z, Blocks.STONE_BRICKS);
            }
        }
        BlockPos spot = new BlockPos(WALL_X - 2, 3, 4);
        helper.assertTrue(LayeredWallSpot.at(helper.getLevel(), ModEntityTypes.THE_LAYERED.get(), helper.absolutePos(spot)).isEmpty(),
                "no spot against bricks");
        helper.assertTrue(LayeredWallSpot.findFace(helper.getLevel(), helper.absoluteVec(new Vec3(5.5, 1.0, 4.5)), 3.0).isEmpty(),
                "no claw marks on bricks");
        wall(helper);
        helper.assertTrue(LayeredWallSpot.at(helper.getLevel(), ModEntityTypes.THE_LAYERED.get(), helper.absolutePos(spot)).isPresent(),
                "a spot against stone");
        helper.succeed();
    }

    /** "Ring 2" is exactly the test's bounds; outside lies the void. */
    private static NightmareRings ring2(GameTestHelper helper) {
        AABB bounds = helper.getBounds();
        return new NightmareRings() {
            @Override
            public Ring ringAt(double x, double z) {
                return x >= bounds.minX && x <= bounds.maxX && z >= bounds.minZ && z <= bounds.maxZ ? Ring.RING_2 : Ring.VOID;
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

    private static void floor(GameTestHelper helper, Block block) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 0, z, block);
            }
        }
    }

    private static void wall(GameTestHelper helper) {
        for (int y = 1; y <= WALL_TOP; y++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(WALL_X, y, z, Blocks.STONE);
            }
        }
    }

    private static void assertFloor(GameTestHelper helper, Block block) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.assertBlockPresent(block, new BlockPos(x, 0, z));
            }
        }
    }

    private static void assertWallAndFloor(GameTestHelper helper, Block block) {
        assertFloor(helper, block);
        for (int y = 1; y <= WALL_TOP; y++) {
            for (int z = 0; z < 9; z++) {
                helper.assertBlockPresent(block, new BlockPos(WALL_X, y, z));
            }
        }
    }
}
