package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.PlayerConditions;
import nl.timtoys.corruptedadventure.item.CanteenItem;
import nl.timtoys.corruptedadventure.item.EchoShellItem;
import nl.timtoys.corruptedadventure.item.HandWarmerItem;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModDataComponents;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The consumables against the conditions (fase-2-items.md §5.3, §6.3, §6.6, §7.3): filling the canteen at a water
 * cauldron, a dew source and a water source; what a sip, a cactus juice, a dew draught, a purified water, a hand warmer
 * charge, a sky light and a blow on the echo shell do to their meter and effect; filling up the hand warmer with a fire
 * crystal; the echo shell's wear and cooldown. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConsumableGameTests {
    private static final BlockPos BLOCK = BlockPos.ZERO;

    private ConsumableGameTests() {
    }

    @GameTest(template = "gametest/empty")
    public static void aCanteenFillsAtAWaterCauldronWhichDropsOneLevel(GameTestHelper helper) {
        helper.setBlock(BLOCK, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack canteen = useOn(helper, player, withSips(1));
        helper.assertValueEqual(CanteenItem.sips(canteen), CanteenItem.MAX_SIPS, "sips after filling");
        helper.assertBlockProperty(BLOCK, LayeredCauldronBlock.LEVEL, 2);

        useOn(helper, player, canteen);
        helper.assertBlockProperty(BLOCK, LayeredCauldronBlock.LEVEL, 2); // a full canteen takes nothing
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void aCanteenFillsAtADewSourceWhichStays(GameTestHelper helper) {
        helper.setBlock(BLOCK, ModBlocks.DEW_SOURCE.get());
        ItemStack canteen = useOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), withSips(0));
        helper.assertValueEqual(CanteenItem.sips(canteen), CanteenItem.MAX_SIPS, "sips after filling");
        helper.assertBlockPresent(ModBlocks.DEW_SOURCE.get(), BLOCK);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void aCanteenFillsFromAWaterSourceWhichStays(GameTestHelper helper) {
        helper.setBlock(BLOCK, Blocks.WATER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Standing in the water, looking straight down: the test's barrier box closes in at y 2.
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 0.1, 0.5));
        player.moveTo(feet.x, feet.y, feet.z, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, withSips(2));
        ItemStack canteen = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();
        helper.assertValueEqual(CanteenItem.sips(canteen), CanteenItem.MAX_SIPS, "sips after filling");
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(BLOCK)).isSourceOfType(Fluids.WATER),
                "the water source stays");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void aSipLowersDehydrationAndCools(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false; // a survival canteen loses its sip
        try {
            PlayerConditions.set(player, Condition.DEHYDRATION, 50.0F);
            ItemStack canteen = withSips(CanteenItem.MAX_SIPS);
            canteen = canteen.finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(PlayerConditions.get(player, Condition.DEHYDRATION), 25.0F, "meter after a sip");
            helper.assertValueEqual(CanteenItem.sips(canteen), CanteenItem.MAX_SIPS - 1, "sips after drinking");
            helper.assertValueEqual(cooledTicks(player), 45 * 20, "cooled ticks");

            ItemStack juice = new ItemStack(ModItems.CACTUS_JUICE.get());
            ItemStack left = juice.finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(PlayerConditions.get(player, Condition.DEHYDRATION), 10.0F, "meter after juice");
            helper.assertTrue(left.is(Items.GLASS_BOTTLE), "the juice gives its bottle back");
            helper.assertValueEqual(cooledTicks(player), 45 * 20, "a shorter juice does not cut the canteen's time");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void theDrinksOfRing3And4LowerTheirMeterAndGiveTheirEffect(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        try {
            PlayerConditions.set(player, Condition.HEAT, 50.0F);
            ItemStack left = new ItemStack(ModItems.DEW_DRAUGHT.get()).finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(PlayerConditions.get(player, Condition.HEAT), 30.0F, "heat after a dew draught");
            helper.assertTrue(left.is(Items.GLASS_BOTTLE), "the dew draught gives its bottle back");
            helper.assertValueEqual(effectTicks(player, ModMobEffects.COOLED), 45 * 20, "cooled ticks");

            PlayerConditions.set(player, Condition.POISON, 50.0F);
            left = new ItemStack(ModItems.PURIFIED_WATER.get()).finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(PlayerConditions.get(player, Condition.POISON), 30.0F, "poison after purified water");
            helper.assertTrue(left.is(Items.GLASS_BOTTLE), "purified water gives its bottle back");
            helper.assertValueEqual(effectTicks(player, ModMobEffects.CLEANSED), 45 * 20, "cleansed ticks");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void aHandWarmerChargeWarmsAndAFireCrystalFillsItUp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        try {
            PlayerConditions.set(player, Condition.COLD, 50.0F);
            ItemStack warmer = new ItemStack(ModItems.HAND_WARMER.get());
            warmer.set(ModDataComponents.CHARGES, HandWarmerItem.MAX_CHARGES);
            warmer = warmer.finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(PlayerConditions.get(player, Condition.COLD), 30.0F, "cold after a charge");
            helper.assertValueEqual(HandWarmerItem.charges(warmer), HandWarmerItem.MAX_CHARGES - 1, "charges after use");
            helper.assertValueEqual(effectTicks(player, ModMobEffects.WARMED), 45 * 20, "warmed ticks");

            // Hotbar slot 0 is slot 36 of the inventory menu; a right click with two crystals on the cursor.
            player.getInventory().setItem(0, warmer);
            player.inventoryMenu.setCarried(new ItemStack(ModItems.FIRE_CRYSTAL.get(), 2));
            player.inventoryMenu.clicked(36, 1, ClickType.PICKUP, player);
            helper.assertValueEqual(HandWarmerItem.charges(player.getInventory().getItem(0)), HandWarmerItem.MAX_CHARGES,
                    "charges after a fire crystal");
            helper.assertValueEqual(player.inventoryMenu.getCarried().getCount(), 1, "one crystal used");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void skyLightAnchorsAndTheEchoShellResoundsWearsAndCoolsDown(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        try {
            PlayerConditions.set(player, Condition.VOID, 50.0F);
            ItemStack left = new ItemStack(ModItems.SKY_LIGHT_DRAUGHT.get()).finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(PlayerConditions.get(player, Condition.VOID), 30.0F, "void after a sky light");
            helper.assertTrue(left.is(Items.GLASS_BOTTLE), "the sky light gives its bottle back");
            helper.assertValueEqual(effectTicks(player, ModMobEffects.ANCHORED), 45 * 20, "anchored ticks");

            PlayerConditions.set(player, Condition.SILENCE, 50.0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ECHO_SHELL.get()));
            ItemStack shell = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();
            helper.assertValueEqual(PlayerConditions.get(player, Condition.SILENCE), 30.0F, "silence after a blow");
            helper.assertValueEqual(effectTicks(player, ModMobEffects.RESOUNDING), 45 * 20, "resounding ticks");
            helper.assertValueEqual(shell.getDamageValue(), 1, "one blow, one point of durability");
            helper.assertValueEqual(shell.getMaxDamage(), EchoShellItem.DURABILITY, "durability");
            helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.ECHO_SHELL.get()), "the shell cools down");
            helper.assertTrue(player.isUsingItem(), "the horn pose");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void saltedJerkyFeedsAndMakesThirsty(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        try {
            player.getFoodData().setFoodLevel(10);
            PlayerConditions.set(player, Condition.DEHYDRATION, 20.0F);
            ItemStack left = new ItemStack(ModItems.SALTED_JERKY.get(), 2).finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(player.getFoodData().getFoodLevel(), 16, "food after a jerky");
            helper.assertValueEqual(PlayerConditions.get(player, Condition.DEHYDRATION), 28.0F, "dehydration after a jerky");
            helper.assertValueEqual(left.getCount(), 1, "jerky left");
        } finally {
            GameTestPlayers.remove(helper, player);
        }
        helper.succeed();
    }

    /**
     * Every dose of a consumable, with its value and the length of its effect; then "the longest remaining time wins"
     * (fase-2-items.md §5.3, §6.6): a dose does not cut a longer effect short, and lengthens a shorter one.
     */
    @GameTest(template = "gametest/empty")
    public static void everyDoseLowersItsMeterAndTheLongestRemainingTimeWins(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        List<String> failures = new ArrayList<>();
        try {
            List<Dose> doses = List.of(
                    new Dose("canteen sip", Condition.DEHYDRATION, 25.0F, ModMobEffects.COOLED, 45,
                            p -> withSips(CanteenItem.MAX_SIPS).finishUsingItem(p.level(), p)),
                    new Dose("cactus juice", Condition.DEHYDRATION, 15.0F, ModMobEffects.COOLED, 20,
                            p -> new ItemStack(ModItems.CACTUS_JUICE.get()).finishUsingItem(p.level(), p)),
                    new Dose("dew draught", Condition.HEAT, 20.0F, ModMobEffects.COOLED, 45,
                            p -> new ItemStack(ModItems.DEW_DRAUGHT.get()).finishUsingItem(p.level(), p)),
                    new Dose("purified water", Condition.POISON, 20.0F, ModMobEffects.CLEANSED, 45,
                            p -> new ItemStack(ModItems.PURIFIED_WATER.get()).finishUsingItem(p.level(), p)),
                    new Dose("hand warmer charge", Condition.COLD, 20.0F, ModMobEffects.WARMED, 45,
                            p -> withCharges(HandWarmerItem.MAX_CHARGES).finishUsingItem(p.level(), p)),
                    new Dose("sky light", Condition.VOID, 20.0F, ModMobEffects.ANCHORED, 45,
                            p -> new ItemStack(ModItems.SKY_LIGHT_DRAUGHT.get()).finishUsingItem(p.level(), p)),
                    new Dose("echo shell blow", Condition.SILENCE, 20.0F, ModMobEffects.RESOUNDING, 45,
                            p -> new ItemStack(ModItems.ECHO_SHELL.get()).use(p.level(), p, InteractionHand.MAIN_HAND)));
            for (Dose dose : doses) {
                int ticks = dose.seconds() * 20;
                player.removeEffect(dose.effect());
                PlayerConditions.set(player, dose.condition(), 50.0F);
                dose.consume().accept(player);
                expect(failures, dose.name() + " meter", PlayerConditions.get(player, dose.condition()), 50.0F - dose.amount());
                expect(failures, dose.name() + " ticks", effectTicks(player, dose.effect()), ticks);

                player.removeEffect(dose.effect());
                player.addEffect(new MobEffectInstance(dose.effect(), ticks + 600));
                dose.consume().accept(player);
                expect(failures, dose.name() + " on a longer effect", effectTicks(player, dose.effect()), ticks + 600);

                player.removeEffect(dose.effect());
                player.addEffect(new MobEffectInstance(dose.effect(), 100));
                dose.consume().accept(player);
                expect(failures, dose.name() + " on a shorter effect", effectTicks(player, dose.effect()), ticks);
            }
        } finally {
            GameTestPlayers.remove(helper, player);
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void aDrinkFromAStackPutsItsBottleInTheInventory(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        List<String> failures = new ArrayList<>();
        try {
            for (Item drink : List.of(ModItems.CACTUS_JUICE.get(), ModItems.DEW_DRAUGHT.get(),
                    ModItems.PURIFIED_WATER.get(), ModItems.SKY_LIGHT_DRAUGHT.get())) {
                player.getInventory().clearContent();
                ItemStack left = new ItemStack(drink, 2).finishUsingItem(helper.getLevel(), player);
                if (!left.is(drink) || left.getCount() != 1) {
                    failures.add(drink + " leaves " + left);
                }
                if (player.getInventory().countItem(Items.GLASS_BOTTLE) != 1) {
                    failures.add(drink + " puts no glass bottle in the inventory");
                }
            }
        } finally {
            GameTestPlayers.remove(helper, player);
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void anEmptyCanteenAndAnEmptyHandWarmerDoNothing(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        try {
            for (ItemStack empty : List.of(withSips(0), withCharges(0), new ItemStack(ModItems.HAND_WARMER.get()))) {
                player.setItemInHand(InteractionHand.MAIN_HAND, empty);
                InteractionResultHolder<ItemStack> result = empty.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == InteractionResult.PASS, empty.getItem() + " empty: " + result.getResult());
                helper.assertFalse(player.isUsingItem(), empty.getItem() + " empty is not used");
            }
            PlayerConditions.set(player, Condition.DEHYDRATION, 50.0F);
            PlayerConditions.set(player, Condition.COLD, 50.0F);
            withSips(0).finishUsingItem(helper.getLevel(), player);
            withCharges(0).finishUsingItem(helper.getLevel(), player);
            helper.assertValueEqual(PlayerConditions.get(player, Condition.DEHYDRATION), 50.0F, "dehydration after an empty sip");
            helper.assertValueEqual(PlayerConditions.get(player, Condition.COLD), 50.0F, "cold after an empty charge");
            helper.assertFalse(player.hasEffect(ModMobEffects.COOLED) || player.hasEffect(ModMobEffects.WARMED),
                    "no effect from an empty canteen or hand warmer");
        } finally {
            GameTestPlayers.remove(helper, player);
        }
        helper.succeed();
    }

    /** Through the server's own use path, which refuses an item on cooldown; the 16th blow breaks the shell. */
    @GameTest(template = "gametest/empty")
    public static void theEchoShellWaitsForItsCooldownAndBreaksAfterSixteenBlows(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ECHO_SHELL.get()));
            blow(helper, player);
            helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "damage after one blow");
            PlayerConditions.set(player, Condition.SILENCE, 50.0F);
            InteractionResult again = blow(helper, player);
            helper.assertTrue(again == InteractionResult.PASS, "a blow during the cooldown: " + again);
            helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "damage after a blow during the cooldown");
            helper.assertValueEqual(PlayerConditions.get(player, Condition.SILENCE), 50.0F, "silence after a blow during the cooldown");
            for (int blows = 2; blows < EchoShellItem.DURABILITY; blows++) {
                player.getCooldowns().removeCooldown(ModItems.ECHO_SHELL.get());
                blow(helper, player);
            }
            helper.assertValueEqual(player.getMainHandItem().getDamageValue(), EchoShellItem.DURABILITY - 1, "damage after 15 blows");
            player.getCooldowns().removeCooldown(ModItems.ECHO_SHELL.get());
            blow(helper, player);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "the 16th blow breaks the shell: " + player.getMainHandItem());
        } finally {
            GameTestPlayers.remove(helper, player);
        }
        helper.succeed();
    }

    private static InteractionResult blow(GameTestHelper helper, ServerPlayer player) {
        InteractionResult result = player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND);
        player.stopUsingItem();
        return result;
    }

    private static void expect(List<String> failures, String what, float actual, float expected) {
        if (actual != expected) {
            failures.add(what + ": " + actual + ", expected " + expected);
        }
    }

    private static ItemStack withCharges(int charges) {
        ItemStack stack = new ItemStack(ModItems.HAND_WARMER.get());
        stack.set(ModDataComponents.CHARGES, charges);
        return stack;
    }

    private record Dose(String name, Condition condition, float amount, Holder<MobEffect> effect, int seconds,
                        Consumer<ServerPlayer> consume) {
    }

    private static int cooledTicks(ServerPlayer player) {
        return effectTicks(player, ModMobEffects.COOLED);
    }

    private static int effectTicks(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? 0 : instance.getDuration();
    }

    private static ItemStack withSips(int sips) {
        ItemStack stack = new ItemStack(ModItems.CANTEEN.get());
        stack.set(ModDataComponents.SIPS, sips);
        return stack;
    }

    private static ItemStack useOn(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(BLOCK);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        return player.getMainHandItem();
    }
}
