package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:titan_rock} (change 47b, V1/G2-WB6 = A): basalt silhouettes of giants, 15–35 high and up to
 * {@code 2 · MAX_REACH + 1} wide, wider than one chunk. Instead of one feature writing into its neighbours, each rock is
 * a pure function of the seed and its grid cell: position, template, rotation, and its base from the pure terrain
 * heights ({@link Ring3Terrain}) and the biome source. Every chunk the rock covers computes the same rock and writes its
 * own part (only into air or replaceable plants), so the result does not depend on the order in which chunks decorate;
 * a rock reaches at most into the 3 × 3 chunks around its centre (contract §4). Placed once per chunk.
 */
public final class TitanRockFeature extends Feature<TitanRockConfiguration> {
    /** The farthest a rock's block lies from its centre column, along x or z. */
    public static final int MAX_REACH = 15;
    private static final long SALT = 0x5DEECE66DL * 31L + 4702L;
    /** The centre stands on plain ground: no lava, crater, flank or cliff, no thin solid. */
    private static final VolcanicFilter PLAIN = VolcanicFilter.builder().ground().build();

    public TitanRockFeature(Codec<TitanRockConfiguration> codec) {
        super(codec);
    }

    /** One rock: centre column, template index and rotation. */
    record Rock(int x, int z, int template, Rotation rotation) {
    }

    @Override
    public boolean place(FeaturePlaceContext<TitanRockConfiguration> context) {
        TitanRockConfiguration config = context.config();
        WorldGenLevel level = context.level();
        VolcanicField field = Ring3Terrain.field(level);
        if (field == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        boolean placed = false;
        for (int i = Math.floorDiv(minX - MAX_REACH, config.cell()); i <= Math.floorDiv(minX + 15 + MAX_REACH, config.cell()); i++) {
            for (int j = Math.floorDiv(minZ - MAX_REACH, config.cell()); j <= Math.floorDiv(minZ + 15 + MAX_REACH, config.cell()); j++) {
                Rock rock = rock(level.getSeed(), i, j, config);
                if (rock == null || Math.abs(rock.x() - (minX + 7.5)) > 8 + MAX_REACH || Math.abs(rock.z() - (minZ + 7.5)) > 8 + MAX_REACH) {
                    continue;
                }
                placed |= place(level, field, config, rock, minX, minZ);
            }
        }
        return placed;
    }

    /** The rock of cell (i, j), or null; a function of the seed and the cell only. */
    static Rock rock(long seed, int i, int j, TitanRockConfiguration config) {
        long h = mix(seed ^ SALT ^ mix(i * 0x9E3779B97F4A7C15L + j * 0xC2B2AE3D27D4EB4FL));
        if (unit(h, 0) >= config.chance()) {
            return null;
        }
        int margin = MAX_REACH + 1;
        int x = i * config.cell() + margin + (int) (unit(h, 1) * (config.cell() - 2 * margin));
        int z = j * config.cell() + margin + (int) (unit(h, 2) * (config.cell() - 2 * margin));
        int template = (int) (unit(h, 3) * config.templates().size());
        Rotation rotation = Rotation.values()[(int) (unit(h, 4) * 4)];
        return new Rock(x, z, template, rotation);
    }

    private static boolean place(WorldGenLevel level, VolcanicField field, TitanRockConfiguration config, Rock rock, int minX, int minZ) {
        VolcanicField.Column centre = field.column(rock.x(), rock.z());
        double centreHeight = centre.height();
        if (!PLAIN.test(centre, Mth.ceil(centreHeight))) {
            return false;
        }
        RandomState random = level.getLevel().getChunkSource().randomState();
        BiomeSource biomes = level.getLevel().getChunkSource().getGenerator().getBiomeSource();
        if (!biomes.getNoiseBiome(QuartPos.fromBlock(rock.x()), QuartPos.fromBlock(Mth.floor(centreHeight)), QuartPos.fromBlock(rock.z()),
                random.sampler()).is(config.biome())) {
            return false;
        }
        StructureTemplate template = level.getLevel().getServer().getStructureManager().getOrCreate(config.templates().get(rock.template()));
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rock.rotation());
        List<StructureTemplate.StructureBlockInfo> infos = new ArrayList<>();
        for (Holder<Block> material : config.materials()) {
            infos.addAll(template.filterBlocks(BlockPos.ZERO, settings, material.value()));
        }
        if (infos.isEmpty()) {
            return false;
        }
        // Centre the footprint on the rock's column; its base sinks below the lowest pure ground of the footprint.
        int lowX = Integer.MAX_VALUE;
        int highX = Integer.MIN_VALUE;
        int lowZ = Integer.MAX_VALUE;
        int highZ = Integer.MIN_VALUE;
        int lowY = Integer.MAX_VALUE;
        for (StructureTemplate.StructureBlockInfo info : infos) {
            lowX = Math.min(lowX, info.pos().getX());
            highX = Math.max(highX, info.pos().getX());
            lowZ = Math.min(lowZ, info.pos().getZ());
            highZ = Math.max(highZ, info.pos().getZ());
            lowY = Math.min(lowY, info.pos().getY());
        }
        int offsetX = rock.x() - (lowX + highX) / 2;
        int offsetZ = rock.z() - (lowZ + highZ) / 2;
        int ground = Integer.MAX_VALUE;
        for (int x = lowX; x <= highX; x += 2) {
            for (int z = lowZ; z <= highZ; z += 2) {
                ground = Math.min(ground, Mth.floor(field.column(x + offsetX, z + offsetZ).height()));
            }
        }
        int base = ground - config.sink() - lowY;
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (StructureTemplate.StructureBlockInfo info : infos) {
            int x = info.pos().getX() + offsetX;
            int z = info.pos().getZ() + offsetZ;
            if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
                continue;
            }
            pos.set(x, base + info.pos().getY(), z);
            BlockState current = level.getBlockState(pos);
            if (current.isAir() || current.canBeReplaced() && current.getFluidState().isEmpty()) {
                level.setBlock(pos, info.state(), Block.UPDATE_CLIENTS);
                placed = true;
            }
        }
        return placed;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double unit(long hash, int n) {
        return (mix(hash + n * 0x632BE59BD9B4E019L) >>> 11) * 0x1.0p-53;
    }
}
