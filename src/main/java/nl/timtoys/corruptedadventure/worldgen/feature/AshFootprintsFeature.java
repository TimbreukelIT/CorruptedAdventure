package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;

/**
 * {@code corruptedadventure:ash_footprints} (change 49): trails of footprints in the ash that stop abruptly, at a cast of
 * ash, at the rim of a collapse or crater (39f accessors) or nowhere ({@link FootprintPath}). Placed once per chunk; the chunk writes the prints of every
 * trail that crosses it, so a trail is whole however the chunks generate. A print is a dent: a layer of 6–7 in full ash,
 * two layers less in a deep drift, or an empty spot in a thin one (2–3 layers); other ground keeps no print.
 * <p>
 * Reads the chunk's own live {@code WORLD_SURFACE} heightmap (after the drifts of this chunk) and its own blocks;
 * writes only its own chunk. Runs after the ash drifts.
 */
public final class AshFootprintsFeature extends Feature<AshFootprintsConfiguration> {
    /** A trail stops this far outside a hole's (mean) rim. */
    private static final double RIM_GAP = 2.0;

    public AshFootprintsFeature(Codec<AshFootprintsConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AshFootprintsConfiguration> context) {
        AshFootprintsConfiguration config = context.config();
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int reach = FootprintPath.MAX_LENGTH;
        int fromI = Math.floorDiv(minX - reach, config.cell());
        int toI = Math.floorDiv(minX + 15 + reach, config.cell());
        int fromJ = Math.floorDiv(minZ - reach, config.cell());
        int toJ = Math.floorDiv(minZ + 15 + reach, config.cell());
        VolcanicField field = Ring3Terrain.field(level);
        FootprintPath.Rims rims = field == null ? FootprintPath.Rims.NONE : (x, z) -> rim(field, x, z);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = fromI; i <= toI; i++) {
            for (int j = fromJ; j <= toJ; j++) {
                FootprintPath.Trail trail = FootprintPath.trail(level.getSeed(), i, j, config.cell(), config.chance(), rims);
                if (trail == null) {
                    continue;
                }
                for (FootprintPath.Step step : trail.steps()) {
                    if (inChunk(step.x(), step.z(), minX, minZ)) {
                        placed |= print(level, config, pos.set(step.x(), level.getHeight(Heightmap.Types.WORLD_SURFACE, step.x(), step.z()) - 1, step.z()),
                                step.left());
                    }
                }
                int endX = Mth.floor(trail.endX());
                int endZ = Mth.floor(trail.endZ());
                if (trail.end() == FootprintPath.End.CAST && inChunk(endX, endZ, minX, minZ)) {
                    StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();
                    int pick = Math.floorMod(i * 31 + j, config.cast().templates().size());
                    placed |= AshCastFeature.place(level, manager.getOrCreate(config.cast().templates().get(pick)),
                            Ring3Terrain.towards(Math.cos(trail.heading()), Math.sin(trail.heading())), new BlockPos(endX, 0, endZ), config.cast());
                }
            }
        }
        return placed;
    }

    /** The nearer of the rims of the collapse and of the nearest volcano's crater, seen from (x, z); pure. */
    @Nullable
    static FootprintPath.Rim rim(VolcanicField field, double x, double z) {
        FootprintPath.Rim best = null;
        VolcanicField.Sinkhole collapse = field.collapseAt(Mth.floor(x), Mth.floor(z));
        if (collapse != null) {
            best = rimOf(collapse.x(), collapse.z(), collapse.radius() + RIM_GAP, x, z);
        }
        VolcanicField.VolcanoInfo volcano = field.nearestVolcano(Mth.floor(x), Mth.floor(z));
        if (volcano != null) {
            FootprintPath.Rim crater = rimOf(volcano.x() + 0.5, volcano.z() + 0.5, volcano.craterRadius() + RIM_GAP, x, z);
            if (best == null || distanceSq(crater, x, z) < distanceSq(best, x, z)) {
                best = crater;
            }
        }
        return best;
    }

    /** The spot {@code radius} from (cx, cz) towards (x, z), heading back into the hole. */
    private static FootprintPath.Rim rimOf(double cx, double cz, double radius, double x, double z) {
        double angle = Math.atan2(z - cz, x - cx);
        return new FootprintPath.Rim(cx + Math.cos(angle) * radius, cz + Math.sin(angle) * radius, angle + Math.PI);
    }

    private static double distanceSq(FootprintPath.Rim rim, double x, double z) {
        return (rim.x() - x) * (rim.x() - x) + (rim.z() - z) * (rim.z() - z);
    }

    /** Dents the ash at {@code top} (the top block of the column); false where it is no ash. */
    private static boolean print(WorldGenLevel level, AshFootprintsConfiguration config, BlockPos.MutableBlockPos top, boolean left) {
        BlockState state = level.getBlockState(top);
        BlockState print;
        if (state.is(config.layer())) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers <= 1) {
                return false;
            }
            print = layers <= 3 ? Blocks.AIR.defaultBlockState()
                    : state.setValue(SnowLayerBlock.LAYERS, layers - 2);
        } else if (state.is(config.block()) && level.getBlockState(top.above()).isAir()) {
            // The feet sink a little differently.
            print = config.layer().defaultBlockState().setValue(SnowLayerBlock.LAYERS, left ? 6 : 7);
        } else {
            return false;
        }
        level.setBlock(top, print, Block.UPDATE_CLIENTS);
        return true;
    }

    private static boolean inChunk(int x, int z, int minX, int minZ) {
        return x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16;
    }
}
