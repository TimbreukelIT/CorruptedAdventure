package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareScaling;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The Hollow (phase 4 step 1): its max health per player in the fight with the health ratio kept, its invulnerability
 * out of phase, and the soul crystal pin. The Hollow has no free will here, so it never attacks the mock players; the
 * fight bookkeeping (health scaling, the pin's countdown) runs without AI. The scaling test has its own batch: a player
 * of a test running beside it would count in its fight. In the empty 9 × 7 × 9 {@code gametest/hollow_arena}. Run like
 * {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HollowGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 0, 4);
    /** Longer than the Hollow's once-a-second check. */
    private static final int CHECK_DELAY = 25;

    private HollowGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena", batch = "hollow_scale", timeoutTicks = 200)
    public static void itsHealthScalesWithThePlayersInTheFightAndKeepsItsRatio(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        double base = ServerConfig.HOLLOW.common.baseHealth.get();
        double two = NightmareScaling.maxHealth(base, ServerConfig.HOLLOW.common.healthPerExtraPlayer.get(), 2);
        ServerPlayer first = playerAt(helper, 1.5, 1.5);
        ServerPlayer[] second = new ServerPlayer[1];
        helper.startSequence()
                .thenExecuteAfter(CHECK_DELAY, () -> {
                    assertHealth(helper, hollow, base, base);
                    hollow.setHealth((float) (base / 2.0));
                    second[0] = playerAt(helper, 7.5, 7.5);
                })
                .thenExecuteAfter(CHECK_DELAY, () -> {
                    assertHealth(helper, hollow, two, two / 2.0);
                    GameTestPlayers.remove(helper, second[0]);
                })
                .thenExecuteAfter(CHECK_DELAY, () -> {
                    assertHealth(helper, hollow, base, base / 2.0);
                    GameTestPlayers.remove(helper, first);
                })
                .thenSucceed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void outOfPhaseItTakesNoDamage(GameTestHelper helper) {
        TheHollow phased = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        ServerPlayer player = playerAt(helper, 1.5, 1.5);
        float full = phased.getHealth();
        helper.assertTrue(phased.startPhase(100), "it goes out of phase");
        helper.assertTrue(phased.isPhased(), "phased");
        helper.assertFalse(phased.hurt(playerAttack(helper, player), 8.0F), "a player's hit goes through");
        helper.assertFalse(phased.hurt(helper.getLevel().damageSources().generic(), 8.0F), "so does other damage");
        helper.assertValueEqual(phased.getHealth(), full, "health out of phase");
        phased.discard();

        TheHollow here = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        helper.assertTrue(here.hurt(playerAttack(helper, player), 8.0F), "in phase a hit lands");
        helper.assertTrue(here.getHealth() < full, "health in phase: " + here.getHealth());
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 200)
    public static void aSoulCrystalHitPinsItInPhaseForFourSecondsAndCostsOne(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        ServerPlayer player = playerAt(helper, 1.5, 1.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOUL_CRYSTAL.get(), 3));
        helper.assertTrue(hollow.startPhase(200), "it goes out of phase");
        helper.assertTrue(hollow.hurt(playerAttack(helper, player), 2.0F), "the crystal hit lands, even out of phase");
        helper.assertFalse(hollow.isPhased(), "pulled back into phase");
        helper.assertTrue(hollow.isPinned(), "pinned");
        helper.assertTrue(hollow.getHealth() < hollow.getMaxHealth(), "the hit did damage");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "crystals after the pin");
        helper.assertFalse(hollow.startPhase(100), "no phasing while pinned");
        hollow.invulnerableTime = 0;
        hollow.hurt(playerAttack(helper, player), 2.0F);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "a crystal on a pinned Hollow costs nothing");
        int pinTicks = ServerConfig.HOLLOW.pinSeconds.get() * 20;
        helper.startSequence()
                .thenExecuteAfter(pinTicks - 10, () -> helper.assertTrue(hollow.isPinned(), "still pinned just before the end"))
                .thenExecuteAfter(15, () -> {
                    helper.assertFalse(hollow.isPinned(), "the pin wears off");
                    helper.assertTrue(hollow.startPhase(100), "and it can phase again");
                    GameTestPlayers.remove(helper, player);
                })
                .thenSucceed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void inCreativeTheSoulCrystalPinCostsNothing(GameTestHelper helper) {
        TheHollow hollow = helper.spawnWithNoFreeWill(ModEntityTypes.THE_HOLLOW.get(), MIDDLE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOUL_CRYSTAL.get(), 3));
        hollow.startPhase(100);
        hollow.hurt(playerAttack(helper, player), 2.0F);
        helper.assertTrue(hollow.isPinned(), "pinned");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "crystals in creative");
        GameTestPlayers.remove(helper, player);
        helper.succeed();
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

    private static void assertHealth(GameTestHelper helper, TheHollow hollow, double max, double health) {
        helper.assertTrue(Math.abs(hollow.getMaxHealth() - max) < 0.01, "max health " + hollow.getMaxHealth() + ", expected " + max);
        helper.assertTrue(Math.abs(hollow.getHealth() - health) < 0.01, "health " + hollow.getHealth() + ", expected " + health);
    }
}
