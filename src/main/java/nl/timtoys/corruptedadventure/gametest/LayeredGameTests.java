package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.layered.LayeredRockfall;
import nl.timtoys.corruptedadventure.entity.layered.TheLayered;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

import java.util.List;

/**
 * The Layered (phase 4 step 1): its armoured back against its belly, freeing a grabbed player by hitting its head (and
 * that sneaking does not), lying on its back, and which blocks its rock fall may break loose. It has no free will here,
 * so it never attacks the mock players. In the empty 9 × 7 × 9 {@code gametest/hollow_arena} (shared with The Hollow).
 * Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LayeredGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 0, 4);
    private static final String ARENA = "gametest/hollow_arena";

    private LayeredGameTests() {
    }

    @GameTest(template = ARENA)
    public static void itsBackTakesLessDamageThanItsBelly(GameTestHelper helper) {
        TheLayered layered = unarmoured(helper);
        ServerPlayer player = playerAt(helper, 1.5, 0.0, 1.5);
        float full = layered.getHealth();
        float factor = ServerConfig.LAYERED.backDamageMultiplier.get().floatValue();
        layered.hurt(playerAttack(helper, player), 10.0F);
        assertNear(helper, full - layered.getHealth(), 10.0F * factor, "a hit from beside lands on the back");

        float before = layered.getHealth();
        layered.invulnerableTime = 0;
        Vec3 below = helper.absoluteVec(new Vec3(1.5, -3.0, 1.5));
        player.moveTo(below.x, below.y, below.z);
        layered.hurt(playerAttack(helper, player), 10.0F);
        assertNear(helper, before - layered.getHealth(), 10.0F, "a hit from below lands on the belly");
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void onItsBackEveryHitLandsOnTheBelly(GameTestHelper helper) {
        TheLayered layered = unarmoured(helper);
        ServerPlayer player = playerAt(helper, 1.5, 0.0, 1.5);
        layered.flip();
        helper.assertTrue(layered.isFallen(), "on its back");
        float full = layered.getHealth();
        layered.hurt(playerAttack(helper, player), 10.0F);
        assertNear(helper, full - layered.getHealth(), 10.0F, "a hit from beside while it lies on its back");
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hitsOnItsHeadFreeTheHeldPlayerAndSneakingDoesNot(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        ServerPlayer held = playerAt(helper, 4.5, 0.0, 1.5);
        ServerPlayer friend = playerAt(helper, 1.5, 0.0, 1.5);
        helper.assertTrue(layered.grab(held), "grabbed");
        held.setShiftKeyDown(true);
        held.stopRiding();
        helper.assertTrue(held.getVehicle() == layered, "sneaking does not get the held player off");
        held.setShiftKeyDown(false);

        int self = ServerConfig.LAYERED.freeHitsBySelf.get();
        for (int i = 1; i < self; i++) {
            hitHead(helper, layered, held);
        }
        helper.assertTrue(held.getVehicle() == layered, "fewer than " + self + " own hits on the head: still held");
        hitHead(helper, layered, friend);
        helper.assertTrue(held.getVehicle() == null, "a friend's hit on the head frees the held player");
        GameTestPlayers.remove(helper, held);
        GameTestPlayers.remove(helper, friend);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theRockFallOnlyTakesNaturalBlocksUnderAnOverhang(GameTestHelper helper) {
        BlockPos feet = new BlockPos(4, 1, 4);
        // A ceiling 4 above the feet: canyon stone, planks, and a column where planks hide canyon stone above them.
        helper.setBlock(feet.offset(0, 4, 0), ModBlocks.RED_CANYON_STONE.get());
        helper.setBlock(feet.offset(1, 4, 0), Blocks.OAK_PLANKS);
        helper.setBlock(feet.offset(-1, 4, 0), Blocks.OAK_PLANKS);
        helper.setBlock(feet.offset(-1, 5, 0), ModBlocks.ORANGE_CANYON_STONE.get());
        helper.setBlock(feet.offset(0, 4, 1), ModBlocks.SCORCHED_SANDSTONE.get());
        List<BlockPos> loose = LayeredRockfall.overhead(helper.getLevel(), helper.absolutePos(feet), 6);
        helper.assertTrue(loose.size() == 2, "two natural blocks under the overhang, found " + loose);
        helper.assertTrue(loose.contains(helper.absolutePos(feet.offset(0, 4, 0))), "the canyon stone");
        helper.assertTrue(loose.contains(helper.absolutePos(feet.offset(0, 4, 1))), "the sandstone");
        helper.assertTrue(LayeredRockfall.fallingState(ModBlocks.RED_CANYON_STONE.get().defaultBlockState())
                .is(ModBlocks.CANYON_COBBLESTONE.get()), "canyon stone falls as canyon cobblestone");
        helper.assertTrue(LayeredRockfall.fallingState(ModBlocks.SCORCHED_SANDSTONE.get().defaultBlockState())
                .is(ModBlocks.LOOSE_SAND.get()), "sandstone falls as loose sand");
        helper.succeed();
    }

    /** Without armor, so the damage factors can be read off the health (the config's armor comes back after a second). */
    private static TheLayered unarmoured(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        layered.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return layered;
    }

    private static void hitHead(GameTestHelper helper, TheLayered layered, Player hitter) {
        layered.invulnerableTime = 0;
        layered.getParts()[0].hurt(playerAttack(helper, hitter), 2.0F);
    }

    private static ServerPlayer playerAt(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return player;
    }

    private static DamageSource playerAttack(GameTestHelper helper, Player player) {
        return helper.getLevel().damageSources().playerAttack(player);
    }

    private static void assertNear(GameTestHelper helper, float actual, float expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01F, what + ": " + actual + ", expected " + expected);
    }
}
