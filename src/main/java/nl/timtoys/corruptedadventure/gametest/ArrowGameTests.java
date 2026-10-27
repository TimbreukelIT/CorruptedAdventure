package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.ArrowEffect;
import nl.timtoys.corruptedadventure.entity.EffectArrow;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

import java.util.List;

/**
 * The fire and poison arrows (phase 2 golf 3 + 4, fase-2-items.md §6.4, §6.7): what a hit does to a mob, that a fire
 * arrow lights no blocks, and that the bow, the crossbow and the dispenser take them. The arrows fly along x in the
 * 9 × 3 × 3 {@code gametest/arrow_range}. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArrowGameTests {
    private static final BlockPos TARGET = new BlockPos(6, 0, 1);

    private ArrowGameTests() {
    }

    @GameTest(template = "gametest/arrow_range")
    public static void aFireArrowSetsAMobOnFireButNoBlock(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, TARGET);
        shoot(helper, ArrowEffect.FIRE);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the arrow hurts the pig");
            helper.assertTrue(pig.getRemainingFireTicks() > 0, "the pig burns");
            assertNoFire(helper);
        });
    }

    @GameTest(template = "gametest/arrow_range")
    public static void aFireArrowPrimesNoTnt(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.TNT);
        helper.setBlock(TARGET.above(), Blocks.TNT);
        shoot(helper, ArrowEffect.FIRE);
        helper.runAfterDelay(20, () -> {
            helper.assertBlockPresent(Blocks.TNT, TARGET);
            helper.assertBlockPresent(Blocks.TNT, TARGET.above());
            helper.assertEntityNotPresent(EntityType.TNT);
            assertNoFire(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/arrow_range")
    public static void aPoisonArrowPoisonsAMobFor5Seconds(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, TARGET);
        shoot(helper, ArrowEffect.POISON);
        helper.succeedWhen(() -> {
            MobEffectInstance poison = pig.getEffect(MobEffects.POISON);
            helper.assertTrue(poison != null, "the pig is poisoned");
            helper.assertValueEqual(poison.getAmplifier(), 0, "poison level");
            helper.assertTrue(poison.getDuration() > 80 && poison.getDuration() <= 100, "poison for 5 s: " + poison.getDuration());
            helper.assertFalse(pig.isOnFire(), "a poison arrow does not burn");
        });
    }

    @GameTest(template = "gametest/arrow_range")
    public static void theBowAndTheCrossbowShootThem(GameTestHelper helper) {
        for (ArrowEffect effect : ArrowEffect.values()) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().add(new ItemStack(effect.item(), 4));
            for (Item weapon : List.of(Items.BOW, Items.CROSSBOW)) {
                ItemStack ammo = player.getProjectile(new ItemStack(weapon));
                helper.assertTrue(ammo.is(effect.item()), weapon + " takes the " + effect + " arrow");
                EffectArrow arrow = (EffectArrow) ((ArrowItem) ammo.getItem())
                        .createArrow(helper.getLevel(), ammo, player, new ItemStack(weapon));
                helper.assertValueEqual(arrow.effect(), effect, "the shot arrow's effect");
                helper.assertTrue(arrow.getPickupItemStackOrigin().is(effect.item()), "the arrow drops its item");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/arrow_range")
    public static void aDispenserShootsThem(GameTestHelper helper) {
        BlockPos dispenser = new BlockPos(1, 0, 1);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity contents = helper.getBlockEntity(dispenser);
        contents.setItem(0, new ItemStack(ArrowEffect.POISON.item()));
        // Powered from above: a redstone block next to the floor could reach the test's own structure block below it.
        helper.pulseRedstone(dispenser.above(), 2);
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(ModEntityTypes.POISON_ARROW.get());
            helper.assertTrue(contents.isEmpty(), "the dispenser shot its arrow");
        });
    }

    /** Fires an arrow of {@code effect} from the start of the range along x at {@link #TARGET}. */
    private static void shoot(GameTestHelper helper, ArrowEffect effect) {
        Vec3 from = helper.absoluteVec(new Vec3(1.5, 0.5, 1.5));
        Vec3 to = helper.absoluteVec(Vec3.atBottomCenterOf(TARGET).add(0.0, 0.45, 0.0));
        EffectArrow arrow = new EffectArrow(effect, helper.getLevel(), from.x, from.y, from.z, new ItemStack(effect.item()));
        Vec3 direction = to.subtract(from);
        arrow.shoot(direction.x, direction.y, direction.z, 2.0F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
    }

    private static void assertNoFire(GameTestHelper helper) {
        BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(8, 2, 2))
                .forEach(pos -> helper.assertBlockNotPresent(Blocks.FIRE, pos));
    }
}
