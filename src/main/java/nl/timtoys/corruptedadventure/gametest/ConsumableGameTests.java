package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
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
import nl.timtoys.corruptedadventure.item.HandWarmerItem;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModDataComponents;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The consumables against the conditions (fase-2-items.md §5.3, §6.3, §6.6): filling the canteen at a water cauldron,
 * a dew source and a water source; what a sip, a cactus juice, a dew draught, a purified water and a hand warmer charge
 * do to their meter and effect; filling up the hand warmer with a fire crystal. Run like {@link TierGatingGameTests}.
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
