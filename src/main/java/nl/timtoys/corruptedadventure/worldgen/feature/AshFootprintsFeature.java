package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;

/**
 * {@code corruptedadventure:ash_footprints} (change 49): trails of footprints in the ash that stop abruptly, at a cast of
 * ash, at the rim of a collapse or crater (39f accessors) or nowhere ({@link FootprintPath}). Placed once per chunk; the
 * chunk writes the prints of every trail that crosses it, so a trail is whole however the chunks generate. A trail ends
 * for good at its first step outside {@code biomes} (the biome source on the pure terrain height: the same answer in
 * every chunk). A print is a dent: a layer of 6–7 in full ash, two layers less in a deep drift, or an empty spot in a
 * thin one (2–3 layers); other ground keeps no print.
 * <p>
 * Prints only on plain ground ({@link VolcanicFilter.Builder#ground()}: not on islands, needles, arches or banks) and
 * only in the ground itself: the full ash just below the frozen {@code WORLD_SURFACE_WG} height with air above, or an
 * ash layer at that height. So a cast of ash (which stands on that height) or anything else standing there is never
 * dented. Reads the frozen heightmap, the pure terrain and its own blocks; writes only its own chunk. Runs after the
 * ash drifts.
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
        if (field == null) {
            return false;
        }
        FootprintPath.Rims rims = (x, z) -> rim(field, x, z);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = fromI; i <= toI; i++) {
            for (int j = fromJ; j <= toJ; j++) {
                FootprintPath.Trail trail = FootprintPath.trail(level.getSeed(), i, j, config.cell(), config.chance(), rims);
                if (trail == null || !crosses(trail, minX, minZ)) {
                    continue;
                }
                int steps = stepsInBiomes(level, field, trail, config);
                for (FootprintPath.Step step : trail.steps().subList(0, steps)) {
                    if (inChunk(step.x(), step.z(), minX, minZ)) {
                        placed |= print(level, field, config, pos, step);
                    }
                }
                int endX = Mth.floor(trail.endX());
                int endZ = Mth.floor(trail.endZ());
                if (trail.end() == FootprintPath.End.CAST && steps == trail.steps().size() && inChunk(endX, endZ, minX, minZ)
                        && inBiomes(level, field, endX, endZ, config)
                        && GROUND.test(field.column(endX, endZ), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, endX, endZ))) {
                    StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();
                    int pick = Math.floorMod(i * 31 + j, config.cast().templates().size());
                    placed |= AshCastFeature.place(level, manager.getOrCreate(config.cast().templates().get(pick)),
                            Ring3Terrain.towards(Math.cos(trail.heading()), Math.sin(trail.heading())), new BlockPos(endX, 0, endZ), config.cast());
                }
            }
        }
        return placed;
    }

    /** Plain ground of the terrain (plan §0.2–0.4), the found surface on it. */
    private static final VolcanicFilter GROUND = VolcanicFilter.builder().ground().build();

    /** Whether a step of the trail or its end lies in the chunk. */
    private static boolean crosses(FootprintPath.Trail trail, int minX, int minZ) {
        if (inChunk(Mth.floor(trail.endX()), Mth.floor(trail.endZ()), minX, minZ)) {
            return true;
        }
        return trail.steps().stream().anyMatch(step -> inChunk(step.x(), step.z(), minX, minZ));
    }

    /** How many steps of the trail lie in the configured biomes before the first that does not (pure). */
    private static int stepsInBiomes(WorldGenLevel level, VolcanicField field, FootprintPath.Trail trail, AshFootprintsConfiguration config) {
        for (int k = 0; k < trail.steps().size(); k++) {
            FootprintPath.Step step = trail.steps().get(k);
            if (!inBiomes(level, field, step.x(), step.z(), config)) {
                return k;
            }
        }
        return trail.steps().size();
    }

    /** The biome at (x, z) on the pure terrain height, from the biome source: the same answer in every chunk. */
    private static boolean inBiomes(WorldGenLevel level, VolcanicField field, int x, int z, AshFootprintsConfiguration config) {
        int y = Mth.floor(field.column(x, z).height());
        RandomState random = level.getLevel().getChunkSource().randomState();
        return config.biomes().contains(level.getLevel().getChunkSource().getGenerator().getBiomeSource()
                .getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), random.sampler()));
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

    /** Dents the ash of the step's column; false where it is no ash in the plain ground. */
    private static boolean print(WorldGenLevel level, VolcanicField field, AshFootprintsConfiguration config, BlockPos.MutableBlockPos pos,
                                 FootprintPath.Step step) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, step.x(), step.z());
        if (!GROUND.test(field.column(step.x(), step.z()), surface)) {
            return false;
        }
        boolean left = step.left();
        BlockPos.MutableBlockPos top = pos.set(step.x(), surface, step.z());
        BlockState state = level.getBlockState(top);
        BlockState print;
        if (state.is(config.layer())) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers <= 1) {
                return false;
            }
            print = layers <= 3 ? Blocks.AIR.defaultBlockState()
                    : state.setValue(SnowLayerBlock.LAYERS, layers - 2);
        } else if (state.isAir() && level.getBlockState(top.move(0, -1, 0)).is(config.block())) {
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
