package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.item.weakness.SunstoneFlashItem;
import nl.timtoys.corruptedadventure.item.weakness.VolcaniteHornItem;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * The weakness items on ordinary mobs (phase 4, stream I): the sunstone flash dazzles the husk it faces and not the one
 * behind; the volcanite horn stuns the husks round it, never a player, and the stun wears off. A dazzled or stunned
 * husk drops its target and takes no new one. Each test has its own batch: the flash and the horn reach further than
 * the 9 × 7 × 9 {@code gametest/hollow_arena} and would touch the mobs of a test beside it. Run like
 * {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WeaknessGameTests {
    private WeaknessGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena", batch = "weakness_flash")
    public static void theSunstoneFlashDazzlesTheMobItFacesNotTheOneBehind(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, 4.5, 4.5);
        try {
            Mob front = helper.spawnWithNoFreeWill(EntityType.HUSK, 4.5F, 0.0F, 7.5F);
            Mob behind = helper.spawnWithNoFreeWill(EntityType.HUSK, 4.5F, 0.0F, 1.5F);
            front.setTarget(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUNSTONE_FLASH.get()));
            ItemStack flash = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();

            helper.assertTrue(front.hasEffect(ModMobEffects.DAZZLED), "the husk in front is dazzled");
            helper.assertValueEqual(front.getEffect(ModMobEffects.DAZZLED).getDuration(),
                    ServerConfig.WEAKNESS_ITEMS.dazzledSeconds.get() * 20, "dazzled ticks");
            helper.assertFalse(behind.hasEffect(ModMobEffects.DAZZLED), "the husk behind is not");
            helper.assertFalse(player.hasEffect(ModMobEffects.DAZZLED), "the user is not");
            helper.assertTrue(front.getTarget() == null, "a dazzled husk drops its target");
            front.setTarget(player);
            helper.assertTrue(front.getTarget() == null, "and takes no new one");
            behind.setTarget(player);
            helper.assertTrue(behind.getTarget() == player, "the other one still can");

            helper.assertValueEqual(flash.getDamageValue(), 1, "one flash, one point of durability");
            helper.assertValueEqual(flash.getMaxDamage(), SunstoneFlashItem.DURABILITY, "durability");
            helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.SUNSTONE_FLASH.get()), "the flash cools down");
        } finally {
            GameTestPlayers.remove(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", batch = "weakness_horn", timeoutTicks = 120)
    public static void theVolcaniteHornStunsTheMobsAroundButNoPlayerAndItWearsOff(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, 4.5, 4.5);
        ServerPlayer friend = playerAt(helper, 6.5, 4.5);
        Mob near = helper.spawnWithNoFreeWill(EntityType.HUSK, 4.5F, 0.0F, 7.5F);
        Mob other = helper.spawnWithNoFreeWill(EntityType.HUSK, 1.5F, 0.0F, 1.5F);
        double speed = near.getAttributeValue(Attributes.MOVEMENT_SPEED);
        near.setTarget(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VOLCANITE_HORN.get()));
        ItemStack horn = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();

        helper.assertTrue(near.hasEffect(ModMobEffects.STUNNED) && other.hasEffect(ModMobEffects.STUNNED), "both husks are stunned");
        helper.assertFalse(friend.hasEffect(ModMobEffects.STUNNED) || player.hasEffect(ModMobEffects.STUNNED), "no player is");
        helper.assertValueEqual(near.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.0, "a stunned husk cannot walk");
        helper.assertTrue(near.getTarget() == null, "a stunned husk drops its target");
        near.setTarget(friend);
        helper.assertTrue(near.getTarget() == null, "and takes no new one");
        helper.assertValueEqual(horn.getDamageValue(), 1, "one blow, one point of durability");
        helper.assertValueEqual(horn.getMaxDamage(), VolcaniteHornItem.DURABILITY, "durability");
        helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.VOLCANITE_HORN.get()), "the horn cools down");
        helper.assertTrue(player.isUsingItem(), "the horn pose");

        helper.startSequence()
                .thenExecuteAfter(ServerConfig.WEAKNESS_ITEMS.stunnedSeconds.get() * 20 + 5, () -> {
                    helper.assertFalse(near.hasEffect(ModMobEffects.STUNNED), "the stun wears off");
                    helper.assertValueEqual(near.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "its speed back");
                    near.setTarget(friend);
                    helper.assertTrue(near.getTarget() == friend, "and it can take a target again");
                    GameTestPlayers.remove(helper, player);
                    GameTestPlayers.remove(helper, friend);
                })
                .thenSucceed();
    }

    private static ServerPlayer playerAt(GameTestHelper helper, double x, double z) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, 0.0, z));
        // Facing +z (yaw 0), level.
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return player;
    }
}
