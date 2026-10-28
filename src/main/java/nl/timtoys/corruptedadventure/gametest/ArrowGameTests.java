package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
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

import java.util.List;

/**
 * The fire, poison and glitch arrows (phase 2 golf 3 + 4 and 5, fase-2-items.md §6.4, §6.7, §7.4): what a hit does to
 * a mob, that a fire arrow lights no blocks, where a glitch arrow's mob may hop, and that the bow, the crossbow and the
 * dispenser take them. The arrows fly along x in the 9 × 3 × 3 {@code gametest/arrow_range}; the glitch hops start in
 * the middle of the 11 × 4 × 11 {@code gametest/glitch_pad}, so every hop of up to 5 blocks stays on its floor. Run like
 * {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArrowGameTests {
    private static final BlockPos TARGET = new BlockPos(6, 0, 1);
    private static final BlockPos PAD_MIDDLE = new BlockPos(5, 0, 5);

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

    @GameTest(template = "gametest/glitch_pad")
    public static void aGlitchArrowMakesAMobHopToASafeSpot(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, PAD_MIDDLE);
        BlockPos start = pig.blockPosition();
        shootAcrossThePad(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the arrow hurts the pig");
            BlockPos landed = pig.blockPosition();
            int dx = landed.getX() - start.getX();
            int dz = landed.getZ() - start.getZ();
            helper.assertTrue(dx * dx + dz * dz >= 1 && dx * dx + dz * dz <= 25,
                    "the pig hops 1-5 blocks: " + start + " -> " + landed);
            helper.assertValueEqual(landed.getY(), start.getY(), "the pig lands on the floor");
            helper.assertTrue(helper.getLevel().noCollision(pig), "the pig lands in no block");
        });
    }

    @GameTest(template = "gametest/glitch_pad")
    public static void aGlitchArrowNeverHopsAMobIntoBlocks(GameTestHelper helper) {
        // Stone everywhere but the pig's place and the arrow's way to it, 1 high: no spot where a mob stands safely.
        fill(helper, new BlockPos(0, 0, 0), new BlockPos(10, 3, 10));
        BlockPos.betweenClosed(new BlockPos(1, 0, 5), PAD_MIDDLE).forEach(pos -> helper.setBlock(pos, Blocks.AIR));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, PAD_MIDDLE);
        BlockPos start = pig.blockPosition();
        shootAcrossThePad(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the arrow hurts the pig");
            helper.assertValueEqual(pig.blockPosition(), start, "the pig stays without a safe spot");
        });
    }

    @GameTest(template = "gametest/glitch_pad")
    public static void aGlitchArrowOnlyHopsAMobWhereItFits(GameTestHelper helper) {
        // A roof 2 above the floor, open 3 × 3 over the golem: a player would fit under it, a golem (2.7 high) does not.
        fill(helper, new BlockPos(0, 2, 0), new BlockPos(10, 3, 10));
        BlockPos.betweenClosed(new BlockPos(4, 2, 4), new BlockPos(6, 3, 6)).forEach(pos -> helper.setBlock(pos, Blocks.AIR));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, PAD_MIDDLE);
        BlockPos start = golem.blockPosition();
        shootAcrossThePad(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "the arrow hurts the golem");
            helper.assertValueEqual(golem.blockPosition(), start, "the golem stays where it fits nowhere");
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
        dispense(helper, ArrowEffect.POISON);
    }

    @GameTest(template = "gametest/arrow_range")
    public static void aDispenserShootsGlitchArrows(GameTestHelper helper) {
        dispense(helper, ArrowEffect.GLITCH);
    }

    private static void dispense(GameTestHelper helper, ArrowEffect effect) {
        BlockPos dispenser = new BlockPos(1, 0, 1);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity contents = helper.getBlockEntity(dispenser);
        contents.setItem(0, new ItemStack(effect.item()));
        // Powered from above: a redstone block next to the floor could reach the test's own structure block below it.
        helper.pulseRedstone(dispenser.above(), 2);
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(effect.entityType());
            helper.assertTrue(contents.isEmpty(), "the dispenser shot its arrow");
        });
    }

    /** Fires an arrow of {@code effect} from the start of the range along x at {@link #TARGET}. */
    private static void shoot(GameTestHelper helper, ArrowEffect effect) {
        shoot(helper, effect, new Vec3(1.5, 0.5, 1.5), TARGET);
    }

    /** Fires a glitch arrow along x at the middle of the pad. */
    private static void shootAcrossThePad(GameTestHelper helper) {
        shoot(helper, ArrowEffect.GLITCH, new Vec3(1.5, 0.5, 5.5), PAD_MIDDLE);
    }

    private static void shoot(GameTestHelper helper, ArrowEffect effect, Vec3 start, BlockPos target) {
        Vec3 from = helper.absoluteVec(start);
        Vec3 to = helper.absoluteVec(Vec3.atBottomCenterOf(target).add(0.0, 0.45, 0.0));
        EffectArrow arrow = new EffectArrow(effect, helper.getLevel(), from.x, from.y, from.z, new ItemStack(effect.item()));
        Vec3 direction = to.subtract(from);
        arrow.shoot(direction.x, direction.y, direction.z, 2.0F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
    }

    private static void fill(GameTestHelper helper, BlockPos from, BlockPos to) {
        BlockPos.betweenClosed(from, to).forEach(pos -> helper.setBlock(pos, Blocks.STONE));
    }

    private static void assertNoFire(GameTestHelper helper) {
        BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(8, 2, 2))
                .forEach(pos -> helper.assertBlockNotPresent(Blocks.FIRE, pos));
    }
}
