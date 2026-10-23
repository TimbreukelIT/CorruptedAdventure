package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.List;

/**
 * {@code corruptedadventure:herobrine_sign} (94, V5-A): a standing {@code corrupted_sign} with one short, translatable line
 * ({@link #TEXTS}; the client shows it in its own language), now and then on a post of {@code corrupted_fence}, its text
 * turned towards the world's middle (the player who comes in from ring 4 reads it). About one in {@link #RED} is red,
 * one of the texts is none at all (a blank sign). Waxed: nobody edits Herobrine's signs.
 * <p>
 * The placement offers one spot per few chunks; the sign keeps it with a chance that grows towards the band line and next
 * to the void of a corrupt spot or a crack ({@link #keepChance}). A ground object: its own random, the frozen heightmap,
 * its own column only.
 */
public final class HerobrineSignFeature extends Feature<NoneFeatureConfiguration> {
    /** The texts' ids: lang key {@code sign.corruptedadventure.ring_5.<id>}; {@code ""} is the blank sign. */
    public static final List<String> TEXTS = List.of("go_back", "he_sees_you", "dont_turn_around", "you_were_here",
            "not_your_world", "wake_up", "he_was_here", "no_one_is_coming", "stop_digging", "");
    static final int RED = 5;
    static final float POST_CHANCE = 0.35F;
    /** The chance to keep an offered spot: {@link #BASE_KEEP} far inside, 1 at the band line and next to the void. */
    static final double BASE_KEEP = 0.35;
    /** Over this many blocks inside the band line the chance falls to the base. */
    static final double LINE_REACH = 300.0;
    /** Within this many blocks of a crack or a corrupt spot's hole the sign is always kept. */
    static final double VOID_REACH = 48.0;
    private static final long SALT = 0x5167_B0A2DL;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public HerobrineSignFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static String key(String text) {
        return "sign.corruptedadventure.ring_5." + text;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = GroundObjects.random(level.getSeed(), origin, SALT);
        Ring5World world = Ring5Terrain.world(level);
        if (world == null || random.nextDouble() >= keepChance(world, origin.getX(), origin.getZ())) {
            return false;
        }
        return placeSign(level, origin, random);
    }

    /** The chance to keep a spot at (x, z): higher towards the band line and beside a crack or a hole. */
    static double keepChance(Ring5World world, int x, int z) {
        double inside = -world.band().signedDistance(x, z);
        double line = 1.0 - Mth.clamp(inside / LINE_REACH, 0.0, 1.0);
        double hole = world.field().innerCrackDistance(x, z) <= VOID_REACH ? 1.0 : 0.0;
        return BASE_KEEP + (1.0 - BASE_KEEP) * Math.max(line, hole);
    }

    /**
     * A Herobrine sign standing at {@code pos} (the first open block over the ground), maybe on a post; false where the
     * ground is not firm or the spot is taken. Also for the abandoned camps.
     */
    static boolean placeSign(WorldGenLevel level, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !Ring5Placements.free(level.getBlockState(pos))) {
            return false;
        }
        String text = TEXTS.get(random.nextInt(TEXTS.size()));
        boolean red = random.nextInt(RED) == 0;
        boolean post = random.nextFloat() < POST_CHANCE;
        BlockPos at = pos;
        if (post) {
            if (!Ring5Placements.free(level.getBlockState(pos.above()))) {
                return false;
            }
            level.setBlock(pos, ModBlocks.CORRUPTED_FENCE.get().defaultBlockState(), FLAGS);
            at = pos.above();
        }
        BlockState sign = ModBlocks.CORRUPTED_SIGN.get().defaultBlockState()
                .setValue(StandingSignBlock.ROTATION, facingTheMiddle(at.getX(), at.getZ()));
        level.setBlock(at, sign, FLAGS);
        if (level.getBlockEntity(at) instanceof SignBlockEntity entity) {
            SignText front = new SignText().setColor(red ? DyeColor.RED : DyeColor.BLACK);
            if (!text.isEmpty()) {
                front = front.setMessage(1, Component.translatable(key(text)));
            }
            // As saved data: the setters would tell a level the change, and worldgen has none yet.
            RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            CompoundTag tag = new CompoundTag();
            SignText.DIRECT_CODEC.encodeStart(ops, front).result().ifPresent(encoded -> tag.put("front_text", encoded));
            tag.putBoolean("is_waxed", true);
            entity.loadCustomOnly(tag, level.registryAccess());
        }
        return true;
    }

    /** The rotation whose front faces the world's middle: a reader standing between it and the middle sees the text. */
    static int facingTheMiddle(int x, int z) {
        // The yaw a reader at the middle's side has while looking at the sign, turned round (as a player places it).
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        return RotationSegment.convertToSegment(yaw + 180.0F);
    }
}
