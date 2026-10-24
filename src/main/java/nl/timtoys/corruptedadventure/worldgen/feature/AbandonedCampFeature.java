package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code corruptedadventure:abandoned_camp} (94, V6-A): a player's camp, left behind. A tent of white or light grey wool
 * (an A-frame on two fence posts, a few cloths torn away), a bed inside, a chest with a few everyday things
 * ({@link #LOOT}), a campfire gone out with a log to sit on, and sometimes one of Herobrine's signs beside it.
 * <p>
 * On level ground only: every column of its {@link #REACH}-square at the middle's height or one lower (the lower ones
 * filled up: with the ground's soil, or with cobble on rock, which no ore of a neighbouring chunk replaces before or after
 * the camp stands), and never on a memory ({@link Ring5Memories}; the placement's filter looks where the camp was asked,
 * this where it is moved to). A ground object: its own random, the frozen heightmap, its own chunk.
 */
public final class AbandonedCampFeature extends Feature<NoneFeatureConfiguration> {
    static final int REACH = 4;
    /** How far the camp's blocks reach from its middle (the corners of its square). */
    static final int FOOTPRINT = 6;
    static final float TORN_CHANCE = 0.15F;
    static final float SIGN_CHANCE = 0.4F;
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            CorruptedAdventure.id("chests/ring_5/abandoned_camp"));
    private static final long SALT = 0xCA_4B5L;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public AbandonedCampFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = GroundObjects.fit(context.origin(), REACH);
        Ring5World world = Ring5Terrain.world(level);
        if (world == null || Ring5Memories.near(world, origin.getX(), origin.getZ(), FOOTPRINT)) {
            return false;
        }
        RandomSource random = GroundObjects.random(level.getSeed(), origin, SALT);
        int y0 = GroundObjects.surface(level, origin.getX(), origin.getZ());
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                int y = GroundObjects.surface(level, origin.getX() + dx, origin.getZ() + dz);
                if (y > y0 || y < y0 - 1) {
                    return false;
                }
            }
        }
        BlockState ground = level.getBlockState(new BlockPos(origin.getX(), y0 - 1, origin.getZ()));
        if (!ground.isFaceSturdy(level, origin.atY(y0 - 1), Direction.UP)) {
            return false;
        }
        // The ground block itself may be a neighbour's ore or not yet: the fill copies soil only.
        BlockState fill = ground.is(BlockTags.DIRT) ? ground : ModBlocks.CORRUPTED_COBBLESTONE.get().defaultBlockState();
        Direction along = Direction.from2DDataValue(random.nextInt(4));
        Direction across = along.getClockWise();
        BlockState wool = (random.nextBoolean() ? Blocks.WHITE_WOOL : Blocks.LIGHT_GRAY_WOOL).defaultBlockState();
        BlockPos floor = origin.atY(y0);

        // Level ground first: the lower columns filled up.
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                BlockPos column = floor.offset(dx, -1, dz);
                if (GroundObjects.surface(level, column.getX(), column.getZ()) < y0) {
                    level.setBlock(column, fill, FLAGS);
                }
            }
        }
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        // The tent: an A-frame, 5 long, the ridge 2 up, a fence post at each end of the ridge.
        for (int l = -2; l <= 2; l++) {
            for (int c = -2; c <= 2; c++) {
                int up = 2 - Math.abs(c);
                if (random.nextFloat() >= TORN_CHANCE || c == 0) {
                    blocks.put(at(floor, along, across, l, c, up), wool);
                }
            }
        }
        for (int l : new int[]{-3, 3}) {
            for (int up = 0; up <= 2; up++) {
                blocks.put(at(floor, along, across, l, 0, up), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        // Inside: the bed (head at the back), the chest beside it.
        BlockState bed = (random.nextBoolean() ? Blocks.RED_BED : Blocks.BROWN_BED).defaultBlockState()
                .setValue(BedBlock.FACING, along.getOpposite());
        blocks.put(at(floor, along, across, -1, 0, 0), bed.setValue(BedBlock.PART, BedPart.FOOT));
        blocks.put(at(floor, along, across, -2, 0, 0), bed.setValue(BedBlock.PART, BedPart.HEAD));
        BlockPos chest = at(floor, along, across, 1, -1, 0);
        blocks.put(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, along));
        // Outside the opening: the dead campfire and a log to sit on.
        blocks.put(at(floor, along, across, 4, 0, 0), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        blocks.put(at(floor, along, across, 4, 2, 0), Blocks.OAK_LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, along.getAxis()));
        blocks.forEach((pos, state) -> level.setBlock(pos, state, FLAGS));
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
        if (random.nextFloat() < SIGN_CHANCE) {
            HerobrineSignFeature.placeSign(level, at(floor, along, across, 3, -3, 0), random);
        }
        return true;
    }

    /** The block {@code l} along, {@code c} across and {@code up} above the floor's middle. */
    private static BlockPos at(BlockPos floor, Direction along, Direction across, int l, int c, int up) {
        return floor.relative(along, l).relative(across, c).above(up);
    }
}
