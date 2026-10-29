package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.layered.LayeredBurrow;
import nl.timtoys.corruptedadventure.entity.layered.LayeredImpact;
import nl.timtoys.corruptedadventure.entity.layered.LayeredSpit;
import nl.timtoys.corruptedadventure.entity.layered.TheLayered;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

import java.util.List;

/**
 * The Layered's life round: a stagger lets go of a held player, a hit on its belly from below is a hit on its weak spot,
 * under the sand nothing hurts it (and nothing of that is saved), it burrows only through sand and gravel, the blow of
 * its pounce stays within its radius and spares other Nightmares, and the dead heads' spit blinds what it reaches but
 * not through a wall (pigs as the targets of those two). Without free will, so it never attacks the mock players
 * itself. In the empty 9 × 7 × 9 {@code gametest/hollow_arena}. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LayeredLifeGameTests {
    private static final BlockPos MIDDLE = new BlockPos(4, 0, 4);
    private static final String ARENA = "gametest/hollow_arena";

    private LayeredLifeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void aStaggerLetsGoOfTheHeldPlayer(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        ServerPlayer held = playerAt(helper, 4.5, 0.0, 1.5);
        helper.assertTrue(layered.grab(held), "grabbed");
        helper.assertTrue(layered.stagger(), "it staggers");
        helper.assertTrue(held.getVehicle() == null, "the stagger lets go of the held player");
        helper.assertFalse(layered.mayStartAttack(), "no attack while staggered");
        GameTestPlayers.remove(helper, held);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aHitOnItsBellyFromBelowIsAHitOnItsWeakSpot(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        layered.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        ServerPlayer player = playerAt(helper, 1.5, 0.0, 1.5);
        layered.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F);
        helper.assertFalse(layered.isStaggered(), "a small hit on its back does not stagger it");
        layered.invulnerableTime = 0;
        Vec3 below = helper.absoluteVec(new Vec3(1.5, -3.0, 1.5));
        player.moveTo(below.x, below.y, below.z);
        layered.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F);
        helper.assertTrue(layered.isStaggered(), "a small hit on its belly from below staggers it");
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void underTheSandNothingHurtsIt(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        ServerPlayer player = playerAt(helper, 1.5, 0.0, 1.5);
        float full = layered.getHealth();
        layered.submerge();
        helper.assertFalse(layered.hurt(helper.getLevel().damageSources().playerAttack(player), 10.0F), "not hurt");
        helper.assertFalse(layered.getParts()[0].hurt(helper.getLevel().damageSources().playerAttack(player), 10.0F),
                "not on its head either");
        helper.assertFalse(layered.isPickable(), "and it cannot be aimed at");
        helper.assertTrue(layered.getHealth() == full, "no damage under the sand");
        layered.emerge();
        layered.invulnerableTime = 0;
        helper.assertTrue(layered.hurt(helper.getLevel().damageSources().playerAttack(player), 10.0F), "back up: hurt");
        GameTestPlayers.remove(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void savedUnderTheSandItComesBackOnItWithItsGravity(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        layered.submerge();
        helper.assertFalse(layered.isNoGravity(), "under the sand it keeps its gravity flag (it is saved)");
        CompoundTag saved = layered.saveWithoutId(new CompoundTag());
        TheLayered loaded = ModEntityTypes.THE_LAYERED.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "created");
        loaded.load(saved);
        helper.assertFalse(loaded.isBurrowed(), "loaded: not under the sand");
        helper.assertFalse(loaded.isNoGravity(), "loaded: it falls again");
        helper.assertFalse(loaded.isInvisible(), "loaded: visible");
        helper.assertFalse(loaded.noPhysics, "loaded: it collides again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void itBurrowsOnlyThroughSandAndGravel(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.SAND);
            helper.setBlock(new BlockPos(x, 1, 6), Blocks.GRAVEL);
        }
        helper.setBlock(new BlockPos(4, 1, 1), ModBlocks.RED_CANYON_STONE.get());
        helper.setBlock(new BlockPos(6, 1, 4), Blocks.SAND);
        helper.setBlock(new BlockPos(6, 2, 4), Blocks.WATER);
        var level = helper.getLevel();
        helper.assertTrue(LayeredBurrow.isBurrowGround(level, feet(helper, 1, 1)), "sand");
        helper.assertTrue(LayeredBurrow.isBurrowGround(level, feet(helper, 2, 6)), "gravel");
        helper.assertFalse(LayeredBurrow.isBurrowGround(level, feet(helper, 4, 1)), "not canyon stone");
        helper.assertFalse(LayeredBurrow.isBurrowGround(level, feet(helper, 6, 4)), "not under water");

        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        helper.assertTrue(LayeredBurrow.step(level, layered, feet(helper, 2, 1), feet(helper, 3, 1)) != null,
                "on along the sand");
        helper.assertTrue(LayeredBurrow.step(level, layered, feet(helper, 3, 1), feet(helper, 4, 1)) == null,
                "the sand ends at the canyon stone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theBlowOfItsPounceStaysWithinItsRadius(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        // Pigs: a fresh mock player cannot be hurt for its first 3 seconds.
        Pig near = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(5, 0, 4));
        Pig far = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(8, 0, 4));
        double radius = ServerConfig.LAYERED.pounceRadius.get();
        List<LivingEntity> hit = LayeredImpact.hit(helper.getLevel(), layered, radius, 6.0, 0.5, 0.3);
        helper.assertTrue(hit.contains(near), "the pig beside it is hit");
        helper.assertFalse(hit.contains(far), "the pig 4 blocks away (radius " + radius + ") is not");
        TheLayered other = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), new BlockPos(4, 0, 5));
        helper.assertFalse(LayeredImpact.hit(helper.getLevel(), layered, radius, 6.0, 0.5, 0.3).contains(other),
                "another Nightmare beside it is not hit");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void theSpitBlindsWhoItReachesButNotThroughAWall(GameTestHelper helper) {
        TheLayered layered = helper.spawnWithNoFreeWill(ModEntityTypes.THE_LAYERED.get(), MIDDLE);
        Pig open = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 1));
        Pig behind = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 7));
        for (int y = 0; y <= 4; y++) {
            for (int z = 6; z <= 8; z++) {
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
        Vec3 from = helper.absoluteVec(new Vec3(1.5, 1.5, 1.5));
        Vec3 fromBehind = helper.absoluteVec(new Vec3(1.5, 1.5, 7.5));
        LayeredSpit atOpen = new LayeredSpit(from, open.getEyePosition());
        LayeredSpit atBehind = new LayeredSpit(fromBehind, behind.getEyePosition());
        for (int i = 0; i < 40; i++) {
            atOpen.tick(helper.getLevel(), layered);
            atBehind.tick(helper.getLevel(), layered);
        }
        helper.assertTrue(open.hasEffect(MobEffects.BLINDNESS), "the cloud blinds the pig in the open");
        helper.assertFalse(behind.hasEffect(MobEffects.BLINDNESS), "the wall stops the cloud");
        helper.succeed();
    }

    private static Vec3 feet(GameTestHelper helper, int x, int z) {
        return helper.absoluteVec(new Vec3(x + 0.5, 2.0, z + 0.5));
    }

    private static ServerPlayer playerAt(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer player = GameTestPlayers.survival(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return player;
    }
}
