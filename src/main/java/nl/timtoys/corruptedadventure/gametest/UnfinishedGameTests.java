package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.unfinished.Graft;
import nl.timtoys.corruptedadventure.entity.unfinished.TheUnfinished;
import nl.timtoys.corruptedadventure.entity.unfinished.UnfinishedPart;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The Unfinished (phase 4 step 1): his mosaic body takes only a share of a blow, his weak spots nothing until loaded (as
 * a look through the sky glass lens does) and then double, a weak spot that took enough tears its graft off, and his
 * parts sit on the rest pose and turn with him. He has no free will here (no attacks, no lens check, no turning): the
 * damage bookkeeping and the parts run without AI. In the empty {@code gametest/hollow_arena}; he is far bigger than it,
 * but he collides with nothing. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UnfinishedGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 0, 4);

    private UnfinishedGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void hisBodyTakesOnlyAShareOfABlow(GameTestHelper helper) {
        TheUnfinished boss = helper.spawnWithNoFreeWill(ModEntityTypes.THE_UNFINISHED.get(), MIDDLE);
        ServerPlayer player = GameTestPlayers.survival(helper);
        float full = boss.getHealth();
        helper.assertTrue(boss.hurt(playerAttack(helper, player), 20.0F), "a blow on his body lands");
        float share = ServerConfig.UNFINISHED.bodyDamageFactor.get().floatValue();
        double expected = full - afterArmor(boss, helper, player, 20.0F * share);
        assertHealth(helper, boss, expected);
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void aWeakSpotTakesNothingUntilLoadedThenDouble(GameTestHelper helper) {
        TheUnfinished boss = helper.spawnWithNoFreeWill(ModEntityTypes.THE_UNFINISHED.get(), MIDDLE);
        ServerPlayer player = GameTestPlayers.survival(helper);
        UnfinishedPart weak = part(boss, "weak_holle");
        float full = boss.getHealth();
        helper.assertFalse(boss.isLoaded(), "not loaded at first");
        helper.assertFalse(weak.hurt(playerAttack(helper, player), 10.0F), "an unloaded weak spot is not there yet");
        helper.assertValueEqual(boss.getHealth(), full, "health after a blow on an unloaded weak spot");
        boss.load(100);
        helper.assertTrue(boss.isLoaded(), "loaded");
        helper.assertTrue(weak.hurt(playerAttack(helper, player), 10.0F), "a loaded weak spot is hit");
        float factor = ServerConfig.UNFINISHED.weakDamageFactor.get().floatValue();
        double expected = full - afterArmor(boss, helper, player, 10.0F * factor);
        assertHealth(helper, boss, expected);
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void aWeakSpotThatTookEnoughTearsItsGraftOff(GameTestHelper helper) {
        TheUnfinished boss = helper.spawnWithNoFreeWill(ModEntityTypes.THE_UNFINISHED.get(), MIDDLE);
        ServerPlayer player = GameTestPlayers.survival(helper);
        UnfinishedPart weak = part(boss, "weak_hoeder");
        boss.load(100);
        double share = ServerConfig.UNFINISHED.graftBreakShare.get();
        // In blows that, after the factor and his armor, stay below and then pass the share of his max health.
        float atShare = (float) (share * boss.getMaxHealth() / ServerConfig.UNFINISHED.weakDamageFactor.get());
        weak.hurt(playerAttack(helper, player), atShare * 0.5F);
        helper.assertTrue(boss.hasGraft(Graft.HOEDER), "the tail holds below the share");
        boss.invulnerableTime = 0;
        weak.hurt(playerAttack(helper, player), atShare * 0.8F);
        helper.assertFalse(boss.hasGraft(Graft.HOEDER), "the tail is torn off");
        helper.assertFalse(weak.isPickable(), "its weak spot is gone");
        helper.assertTrue(boss.hasGraft(Graft.HOLLE), "the other grafts stay");
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void hisPartsSitOnTheRestPoseAndTurnWithHim(GameTestHelper helper) {
        TheUnfinished boss = helper.spawnWithNoFreeWill(ModEntityTypes.THE_UNFINISHED.get(), MIDDLE);
        boss.setYRot(90.0F);
        helper.runAfterDelay(2, () -> {
            for (PartEntity<?> entity : boss.getParts()) {
                UnfinishedPart part = (UnfinishedPart) entity;
                UnfinishedPart.Spec spec = part.spec();
                Vec3 expected = boss.localToWorld(spec.left(), spec.up(), spec.forward());
                helper.assertTrue(part.position().distanceTo(expected) < 1.0E-6,
                        spec.name() + " at " + part.position() + ", expected " + expected);
            }
            // Facing west (yaw 90), his right is north (-z): the Hollow's arm (the model's +x) is north of him.
            helper.assertTrue(part(boss, "arm").getZ() < boss.getZ() - 5.0, "the arm on his right");
            helper.succeed();
        });
    }

    private static UnfinishedPart part(TheUnfinished boss, String name) {
        for (PartEntity<?> entity : boss.getParts()) {
            if (entity instanceof UnfinishedPart part && part.spec().name().equals(name)) {
                return part;
            }
        }
        throw new IllegalArgumentException(name);
    }

    private static void assertHealth(GameTestHelper helper, TheUnfinished boss, double expected) {
        helper.assertTrue(Math.abs(boss.getHealth() - expected) < 0.01, "health " + boss.getHealth() + ", expected " + expected);
    }

    /** A blow as it lands after his armor. */
    private static float afterArmor(TheUnfinished boss, GameTestHelper helper, ServerPlayer player, float damage) {
        return CombatRules.getDamageAfterAbsorb(boss, damage, playerAttack(helper, player), boss.getArmorValue(),
                (float) boss.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    private static DamageSource playerAttack(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().damageSources().playerAttack(player);
    }
}
