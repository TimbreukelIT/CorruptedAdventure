package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.risen.RisenAshRain;
import nl.timtoys.corruptedadventure.entity.risen.RisenFire;
import nl.timtoys.corruptedadventure.entity.risen.RisenMeltPool;
import nl.timtoys.corruptedadventure.entity.risen.RisenState;
import nl.timtoys.corruptedadventure.entity.risen.TheRisen;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The Risen (phase 4 step 1): the ash layers (slower per layer, held fast at six, knocked off by another player's hit),
 * the melt pool (only natural blocks, lava only where the pool holds it, nothing of it left once it has set) and its
 * wings taking less damage than its body; from the leven-ronde its burning ground (only on free ground, goes out by
 * itself, sets who stands in it alight, changes no block) and its attacks restarting from circling when loaded. The
 * flight itself needs a client to judge (teststop). In the empty
 * 9 × 7 × 9 {@code gametest/hollow_arena}. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RisenGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 2, 4);

    private RisenGameTests() {
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void ashSlowsPerLayerHoldsFastAtSixAndAHitKnocksItOff(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        RisenAshRain.addLayer(player);
        helper.assertValueEqual(RisenAshRain.layers(player), 1, "layers after one");
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) < speed, "one layer slows");
        for (int i = 0; i < RisenAshRain.MAX_LAYERS + 2; i++) {
            RisenAshRain.addLayer(player);
        }
        helper.assertValueEqual(RisenAshRain.layers(player), RisenAshRain.MAX_LAYERS, "layers stop at the maximum");
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) < 1.0E-6, "held fast");
        helper.assertTrue(RisenAshRain.knockOff(player), "a hit knocks ash off");
        helper.assertValueEqual(RisenAshRain.layers(player),
                RisenAshRain.MAX_LAYERS - ServerConfig.RISEN.ashKnockOff.get(), "layers after a hit");
        while (RisenAshRain.knockOff(player)) {
            helper.assertTrue(RisenAshRain.layers(player) < RisenAshRain.MAX_LAYERS, "knocking off goes down");
        }
        helper.assertValueEqual(RisenAshRain.layers(player), 0, "all ash off");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1.0E-6, "full speed again");
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void theMeltPoolTakesOnlyNaturalGroundHoldsItsLavaAndSetsToAScar(GameTestHelper helper) {
        // A two-thick stone floor, a player's plank in it, a hole under one block and a torch on another.
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
        helper.setBlock(5, 1, 4, Blocks.OAK_PLANKS);
        helper.setBlock(2, 0, 4, Blocks.AIR);
        helper.setBlock(4, 1, 6, Blocks.COBBLESTONE);
        helper.setBlock(4, 2, 6, Blocks.TORCH);
        RisenMeltPool pool = RisenMeltPool.plan(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 4)), 3);
        pool.glow(helper.getLevel());
        helper.assertBlockPresent(Blocks.MAGMA_BLOCK, 4, 1, 4);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, 5, 1, 4);
        helper.assertBlockPresent(Blocks.TORCH, 4, 2, 6);
        pool.open(helper.getLevel());
        helper.assertBlockPresent(Blocks.LAVA, 4, 1, 4);
        helper.assertBlockPresent(Blocks.LAVA, 6, 1, 4);
        helper.assertBlockPresent(Blocks.MAGMA_BLOCK, 2, 1, 4);
        pool.set(helper.getLevel());
        helper.assertTrue(pool.isSet(), "set");
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.assertBlockNotPresent(Blocks.LAVA, x, 1, z);
            }
        }
        helper.assertBlockPresent(Blocks.OAK_PLANKS, 5, 1, 4);
        helper.assertBlockPresent(Blocks.COBBLESTONE, 4, 1, 6);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void itsWingsTakeLessDamageThanItsBody(GameTestHelper helper) {
        TheRisen risen = helper.spawnWithNoFreeWill(ModEntityTypes.THE_RISEN.get(), MIDDLE);
        PartEntity<?>[] parts = risen.getParts();
        helper.assertTrue(parts != null && parts.length == 3, "two wings and a head");
        float full = risen.getHealth();
        helper.assertTrue(parts[0].hurt(helper.getLevel().damageSources().magic(), 10.0F), "a wing hit lands");
        float wing = full - risen.getHealth();
        risen.invulnerableTime = 0;
        helper.assertTrue(parts[2].hurt(helper.getLevel().damageSources().magic(), 10.0F), "a head hit lands");
        float head = full - wing - risen.getHealth();
        double factor = ServerConfig.RISEN.wingDamageFactor.get();
        helper.assertTrue(Math.abs(wing - head * factor) < 0.01, "wing " + wing + " vs head " + head + " x " + factor);
        helper.succeed();
    }

    @GameTest(template = "gametest/hollow_arena", timeoutTicks = 200)
    public static void itsFireLiesOnlyOnFreeGroundGoesOutByItselfAndChangesNoBlock(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
        helper.setBlock(4, 1, 4, Blocks.WATER);
        helper.setBlock(6, 1, 6, Blocks.AIR);
        helper.setBlock(2, 2, 6, Blocks.OAK_PLANKS);
        RisenFire fire = new RisenFire();
        var level = helper.getLevel();
        var config = ServerConfig.RISEN;
        helper.assertTrue(fire.light(level, helper.absolutePos(new BlockPos(2, 2, 2)), config), "free ground burns");
        helper.assertTrue(fire.light(level, helper.absolutePos(new BlockPos(3, 2, 2)), config), "free ground burns");
        helper.assertFalse(fire.light(level, helper.absolutePos(new BlockPos(4, 2, 4)), config), "not on water");
        helper.assertFalse(fire.light(level, helper.absolutePos(new BlockPos(6, 2, 6)), config), "not over a hole");
        helper.assertFalse(fire.light(level, helper.absolutePos(new BlockPos(2, 2, 6)), config), "not in a block");
        helper.assertValueEqual(fire.size(), 2, "tiles");
        // A block put on a tile puts it out.
        helper.setBlock(3, 2, 2, Blocks.COBBLESTONE);
        helper.onEachTick(() -> fire.tick(level, null, 1.0F, config));
        helper.runAfterDelay(2, () -> helper.assertFalse(fire.isBurning(helper.absolutePos(new BlockPos(3, 2, 2))),
                "covered tile is out"));
        helper.runAfterDelay(config.fireSeconds.get() * 20 + 2, () -> {
            helper.assertValueEqual(fire.size(), 0, "all out");
            helper.assertBlockPresent(Blocks.AIR, 2, 2, 2);
            helper.assertBlockPresent(Blocks.STONE, 2, 1, 2);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void whoeverStandsInItsFireCatchesFire(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
        ServerPlayer player = GameTestPlayers.survival(helper);
        BlockPos tile = helper.absolutePos(MIDDLE);
        player.moveTo(tile.getX() + 0.5, tile.getY(), tile.getZ() + 0.5);
        RisenFire fire = new RisenFire();
        helper.assertTrue(fire.light(helper.getLevel(), tile, ServerConfig.RISEN), "lit");
        helper.onEachTick(() -> fire.tick(helper.getLevel(), null, 1.0F, ServerConfig.RISEN));
        helper.succeedWhen(() -> {
            helper.assertTrue(player.isOnFire(), "on fire");
            GameTestPlayers.remove(helper, player);
        });
    }

    @GameTest(template = "gametest/hollow_arena")
    public static void anAttackInProgressStartsAgainFromCirclingWhenLoaded(GameTestHelper helper) {
        TheRisen risen = helper.spawnWithNoFreeWill(ModEntityTypes.THE_RISEN.get(), MIDDLE);
        CompoundTag tag = risen.saveWithoutId(new CompoundTag());
        risen.discard();
        for (RisenState state : RisenState.values()) {
            tag.putByte("RisenState", (byte) state.ordinal());
            TheRisen loaded = ModEntityTypes.THE_RISEN.get().create(helper.getLevel());
            helper.assertTrue(loaded != null, "created");
            loaded.load(tag);
            boolean restarts = state == RisenState.DIVING || state == RisenState.LANDING || state == RisenState.SOARING
                    || state == RisenState.SWOOPING || state == RisenState.EMBER_RAIN;
            helper.assertValueEqual(loaded.state(), restarts ? RisenState.CIRCLING : state, "loaded " + state);
            loaded.discard();
        }
        helper.succeed();
    }
}
