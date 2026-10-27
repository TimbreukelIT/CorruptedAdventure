package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
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
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModDataComponents;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The consumables against dehydration (phase 2 golf 2, fase-2-items.md §5.3): filling the canteen at a water cauldron,
 * a dew source and a water source, and what a sip and a cactus juice do to the meter. Run like
 * {@link TierGatingGameTests}.
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

    private static int cooledTicks(ServerPlayer player) {
        MobEffectInstance cooled = player.getEffect(ModMobEffects.COOLED);
        return cooled == null ? 0 : cooled.getDuration();
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
