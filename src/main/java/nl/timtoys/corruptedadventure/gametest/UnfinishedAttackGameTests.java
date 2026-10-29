package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.unfinished.ShardVolley;
import nl.timtoys.corruptedadventure.entity.unfinished.TheUnfinished;
import nl.timtoys.corruptedadventure.entity.unfinished.UnfinishedTempBlocks;
import nl.timtoys.corruptedadventure.entity.unfinished.UnfinishedUnload;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

import java.util.List;

/**
 * The Unfinished's own attacks of the life round (L5) and what they do to the world: a block he changes for a while comes
 * back exactly (one a player changed meanwhile is left), unloading takes only whole natural columns (never one with a
 * block entity or a built block in it), and a thrown memory's shard lies on natural ground but bursts on a build. In the
 * empty {@code gametest/hollow_arena} (9 x 7 x 9); he has no free will here.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UnfinishedAttackGameTests {
    private UnfinishedAttackGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void aTemporaryBlockComesBackExactly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stone = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos changed = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.GRANITE);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.DIRT);
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        long at = level.getGameTime() + 5;
        helper.assertTrue(data.swap(level, stone, Blocks.AIR.defaultBlockState(), at, UnfinishedTempBlocks.Kind.GROUND),
                "the granite goes");
        helper.assertTrue(data.swap(level, changed, Blocks.AIR.defaultBlockState(), at, UnfinishedTempBlocks.Kind.GROUND),
                "the dirt goes");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
        // A player builds into the hole meanwhile: his block stays.
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.OAK_PLANKS);
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(Blocks.GRANITE, new BlockPos(2, 1, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 1, 4));
            helper.assertFalse(data.isTracked(stone) || data.isTracked(changed), "nothing tracked any more");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void unloadingTakesOnlyWholeNaturalColumns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(7, 1, 7), Blocks.CHEST);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.WATER);
        BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 4));
        List<UnfinishedUnload.Column> columns = UnfinishedUnload.plan(level, centre, 4, 1_000_000, Vec3.ZERO, 0.0);
        for (BlockPos refused : List.of(new BlockPos(1, 1, 1), new BlockPos(7, 1, 7), new BlockPos(4, 1, 4),
                new BlockPos(5, 1, 4))) {
            BlockPos at = helper.absolutePos(refused);
            helper.assertFalse(columns.stream().anyMatch(c -> c.x() == at.getX() && c.z() == at.getZ()),
                    "column " + refused + " (planks, chest, water or beside water) stays");
        }
        List<UnfinishedUnload.Column> few = UnfinishedUnload.plan(level, centre, 4, 1, Vec3.ZERO, 0.0);
        helper.assertTrue(few.isEmpty(), "no column fits in a budget of one block");
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 400)
    public static void aShardLiesOnNaturalGroundButBurstsOnABuild(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TheUnfinished boss = helper.spawnWithNoFreeWill(ModEntityTypes.THE_UNFINISHED.get(), new BlockPos(4, 0, 4));
        // In the corners, clear of his main hitbox (a shard does not lie where something stands).
        BlockPos ground = new BlockPos(0, 1, 0);
        BlockPos build = new BlockPos(8, 1, 8);
        helper.setBlock(ground, Blocks.DIRT);
        helper.setBlock(build, Blocks.OAK_PLANKS);
        for (int y = 2; y <= 5; y++) {
            helper.setBlock(ground.atY(y), Blocks.AIR);
            helper.setBlock(build.atY(y), Blocks.AIR);
        }
        ShardVolley.tearOff(level, boss, Vec3.atBottomCenterOf(helper.absolutePos(ground.atY(5))), 1).drop();
        ShardVolley.tearOff(level, boss, Vec3.atBottomCenterOf(helper.absolutePos(build.atY(5))), 1).drop();
        BlockPos lies = helper.absolutePos(ground.above());
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        int linger = ServerConfig.UNFINISHED.throwLingerSeconds.get() * 20;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(data.isShard(lies), "the shard lies on the dirt"))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, build.above()))
                .thenExecuteAfter(linger + 30, () -> {
                    helper.assertBlockPresent(Blocks.AIR, ground.above());
                    helper.assertFalse(data.isShard(lies), "and is gone again");
                })
                .thenSucceed();
    }
}
