package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
 * The Layered (phase 4 step 1): its armoured back against its belly (an arrow counts where it strikes), freeing a
 * grabbed player by hitting its head, that sneaking does not (but a teleport and logging out do, and it stays in the
 * world then), lying on its back, which blocks its rock fall may break loose, and that only its own rock fall spares it.
 * It has no free will here, so it never attacks the mock players. In the empty 9 × 7 × 9 {@code gametest/hollow_arena} (shared with The Hollow).
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
    public static void anArrowCountsWhereItStrikesNotWhereItWasShotFrom(GameTestHelper helper) {
        TheLayered layered = unarmoured(helper);
        // The archer's eyes are far under its feet, which alone made every arrow a hit on the belly before.
        ServerPlayer archer = playerAt(helper, 1.5, -3.0, 1.5);
        float factor = ServerConfig.LAYERED.backDamageMultiplier.get().floatValue();
        float full = layered.getHealth();
        layered.hurt(arrowHit(helper, archer, new Vec3(1.5, 1.0, 4.5), new Vec3(2.0, 0.0, 0.0)), 10.0F);
        assertNear(helper, full - layered.getHealth(), 10.0F * factor, "an arrow into its side lands on the back");

        float before = layered.getHealth();
        layered.invulnerableTime = 0;
        layered.hurt(arrowHit(helper, archer, new Vec3(4.5, -1.0, 4.5), new Vec3(0.0, 2.0, 0.0)), 10.0F);
        assertNear(helper, before - layered.getHealth(), 10.0F, "an arrow up into its underside lands on the belly");
        GameTestPlayers.remove(helper, archer);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sneakingDoesNotGetTheHeldPlayerOffButATeleportDoes(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        ServerPlayer held = playerAt(helper, 4.5, 0.0, 1.5);
        helper.assertTrue(layered.grab(held), "grabbed");
        // As a client does while the key is held: sneaking again before every tick.
        helper.onEachTick(() -> held.setShiftKeyDown(true));
        Vec3[] seat = new Vec3[1];
        helper.runAfterDelay(2, () -> {
            seat[0] = held.position().subtract(layered.position());
            layered.setPos(layered.position().add(1.0, 0.0, 0.0));
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(held.getVehicle() == layered, "sneaking does not get the held player off");
            Vec3 now = held.position().subtract(layered.position());
            helper.assertTrue(now.distanceTo(seat[0]) < 0.01,
                    "and while sneaking they still go along with it: " + now + ", expected " + seat[0]);
            held.setShiftKeyDown(true);
            // What an ender pearl and chorus fruit do before they teleport.
            held.stopRiding();
            helper.assertTrue(held.getVehicle() == null, "a teleport gets them off, sneaking or not");
            GameTestPlayers.remove(helper, held);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void aHeldPlayerWhoLogsOutIsLetGoAndItStays(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        ServerPlayer held = playerAt(helper, 4.5, 0.0, 1.5);
        helper.assertTrue(layered.grab(held), "grabbed");
        // PlayerList.remove, as on a real logout: it saves the player and takes a vehicle they still ride along.
        GameTestPlayers.remove(helper, held);
        helper.assertFalse(layered.isRemoved(), "it stays in the world");
        helper.assertFalse(layered.isVehicle(), "and has let go");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hitsOnItsHeadFreeTheHeldPlayer(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        ServerPlayer held = playerAt(helper, 4.5, 0.0, 1.5);
        ServerPlayer friend = playerAt(helper, 1.5, 0.0, 1.5);
        helper.assertTrue(layered.grab(held), "grabbed");

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

    @GameTest(template = ARENA)
    public static void aLowCeilingClosesItsColumnToTheRockFall(GameTestHelper helper) {
        BlockPos feet = new BlockPos(4, 1, 4);
        helper.setBlock(feet.offset(0, 4, 0), ModBlocks.RED_CANYON_STONE.get());
        helper.setBlock(feet.offset(1, 4, 0), ModBlocks.RED_CANYON_STONE.get());
        helper.setBlock(feet.offset(1, 2, 0), Blocks.STONE);
        List<BlockPos> loose = LayeredRockfall.overhead(helper.getLevel(), helper.absolutePos(feet), 6);
        helper.assertTrue(loose.size() == 1 && loose.contains(helper.absolutePos(feet.offset(0, 4, 0))),
                "only the column without a block 2 above the feet, found " + loose);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void onlyItsOwnRockFallSparesIt(GameTestHelper helper) {
        BlockPos feet = new BlockPos(2, 0, 2);
        helper.setBlock(feet.offset(0, 4, 0), ModBlocks.RED_CANYON_STONE.get());
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, feet);
        TheLayered layered = unarmoured(helper);
        helper.assertTrue(LayeredRockfall.drop(helper.getLevel(), layered, target, 1) == 1, "one rock comes loose");
        List<FallingBlockEntity> rocks = helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class,
                new AABB(helper.absolutePos(feet)).inflate(16.0), LayeredRockfall::isRock);
        helper.assertTrue(rocks.size() == 1, "the rock is marked as its rock fall");
        DamageSources sources = helper.getLevel().damageSources();
        helper.assertFalse(layered.hurt(sources.fallingBlock(rocks.get(0)), 4.0F), "its own rock does not hurt it");
        FallingBlockEntity other = EntityType.FALLING_BLOCK.create(helper.getLevel());
        helper.assertTrue(other != null && !LayeredRockfall.isRock(other), "an unmarked falling block");
        helper.assertTrue(layered.hurt(sources.fallingBlock(other), 4.0F), "another falling block does hurt it");
        rocks.get(0).discard();
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

    /** An arrow by {@code archer} at {@code at} (relative), about to fly {@code velocity} this tick. */
    private static DamageSource arrowHit(GameTestHelper helper, Player archer, Vec3 at, Vec3 velocity) {
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setOwner(archer);
        Vec3 pos = helper.absoluteVec(at);
        arrow.setPos(pos.x, pos.y, pos.z);
        arrow.setDeltaMovement(velocity);
        return helper.getLevel().damageSources().arrow(arrow, archer);
    }

    private static DamageSource playerAttack(GameTestHelper helper, Player player) {
        return helper.getLevel().damageSources().playerAttack(player);
    }

    private static void assertNear(GameTestHelper helper, float actual, float expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01F, what + ": " + actual + ", expected " + expected);
    }
}
