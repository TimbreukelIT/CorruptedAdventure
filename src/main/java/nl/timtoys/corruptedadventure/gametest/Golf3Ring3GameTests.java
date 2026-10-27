package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The ring 3 extras with behaviour of their own (phase 2 golf 3, fase-2-items.md §6.4): picking and planting the ember
 * berry bush, eating its berries and the dripshroom stew, the pumice raft on lava, and sulfur as fuel. Run like
 * {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Golf3Ring3GameTests {
    private static final BlockPos GROUND = BlockPos.ZERO;
    private static final BlockPos ABOVE = GROUND.above();

    private Golf3Ring3GameTests() {
    }

    @GameTest(template = "gametest/empty")
    public static void pickingARipeEmberBerryBushGivesEmberBerries(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.MOSS_BLOCK);
        helper.setBlock(ABOVE, ModBlocks.EMBER_BERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        BlockPos bush = helper.absolutePos(ABOVE);
        helper.getBlockState(ABOVE).useWithoutItem(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL),
                new BlockHitResult(Vec3.atCenterOf(bush), Direction.UP, bush, false));
        helper.assertBlockProperty(ABOVE, SweetBerryBushBlock.AGE, 1);
        helper.assertItemEntityPresent(ModItems.EMBER_BERRIES.get(), ABOVE, 2.0);
        helper.assertItemEntityNotPresent(Items.SWEET_BERRIES, ABOVE, 2.0);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void emberBerriesPlantTheBush(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.MOSS_BLOCK);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EMBER_BERRIES.get(), 2));
        BlockPos ground = helper.absolutePos(GROUND);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0), Direction.UP, ground, false)));
        helper.assertBlockPresent(ModBlocks.EMBER_BERRY_BUSH.get(), ABOVE);
        helper.assertBlockProperty(ABOVE, SweetBerryBushBlock.AGE, 0);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "berries left");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void emberBerriesGiveFireResistanceAndTheStewItsBowl(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        try {
            new ItemStack(ModItems.EMBER_BERRIES.get()).finishUsingItem(helper.getLevel(), player);
            MobEffectInstance fireResistance = player.getEffect(MobEffects.FIRE_RESISTANCE);
            helper.assertTrue(fireResistance != null, "fire resistance after an ember berry");
            helper.assertValueEqual(fireResistance.getDuration(), ModItems.EMBER_BERRY_FIRE_RESISTANCE_TICKS, "ticks");

            ItemStack left = new ItemStack(ModItems.DRIPSHROOM_STEW.get()).finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(left.is(Items.BOWL), "the stew gives its bowl back");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void aPumiceRaftGoesOnLavaCarriesYouAndBreaksWithoutIt(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.LAVA);
        Player player = placeRaftLookingDown(helper);
        helper.assertBlockPresent(ModBlocks.PUMICE_RAFT.get(), ABOVE);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "rafts left");
        BlockPos raft = helper.absolutePos(ABOVE);
        double top = helper.getBlockState(ABOVE).getCollisionShape(helper.getLevel(), raft).max(Direction.Axis.Y);
        helper.assertValueEqual(top, 1.5 / 16.0, "the raft's walkable top");

        helper.setBlock(GROUND, Blocks.AIR);
        helper.assertBlockNotPresent(ModBlocks.PUMICE_RAFT.get(), ABOVE);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void aPumiceRaftDoesNotGoOnWater(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.WATER);
        placeRaftLookingDown(helper);
        helper.assertBlockNotPresent(ModBlocks.PUMICE_RAFT.get(), ABOVE);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void sulfurBurnsLikeCoalAndItsBlockLikeACoalBlock(GameTestHelper helper) {
        for (RecipeType<?> furnace : new RecipeType<?>[] {RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING}) {
            helper.assertValueEqual(new ItemStack(ModItems.SULFUR.get()).getBurnTime(furnace),
                    new ItemStack(Items.COAL).getBurnTime(furnace), "sulfur in " + furnace);
            helper.assertValueEqual(new ItemStack(ModBlocks.SULFUR_BLOCK.get()).getBurnTime(furnace),
                    new ItemStack(Items.COAL_BLOCK).getBurnTime(furnace), "sulfur block in " + furnace);
        }
        helper.succeed();
    }

    /**
     * Uses two rafts like a player standing in the fluid and looking straight down (the item aims at a fluid source), as
     * in {@code ConsumableGameTests}: the test's barrier box closes in at y 2. The mock player is not in the level, so
     * it does not stand in the raft's way.
     */
    private static Player placeRaftLookingDown(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 0.1, 0.5));
        player.moveTo(feet.x, feet.y, feet.z, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.PUMICE_RAFT.get(), 2));
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        return player;
    }
}
