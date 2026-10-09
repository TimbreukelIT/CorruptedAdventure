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

/**
 * {@code corruptedadventure:ash_footprints} (change 49): trails of footprints in the ash that stop abruptly, at a cast of
 * ash, at a crater rim or nowhere ({@link FootprintPath}). Placed once per chunk; the chunk writes the prints of every
 * trail that crosses it, so a trail is whole however the chunks generate. A print is a dent: a layer of 6–7 in full ash,
 * two layers less in a deep drift, or an empty spot in a thin one (2–3 layers); other ground keeps no print.
 * <p>
 * Reads the chunk's own live {@code WORLD_SURFACE} heightmap (after the drifts of this chunk) and its own blocks;
 * writes only its own chunk. Runs after the ash drifts.
 */
public final class AshFootprintsFeature extends Feature<AshFootprintsConfiguration> {
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
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = fromI; i <= toI; i++) {
            for (int j = fromJ; j <= toJ; j++) {
                FootprintPath.Trail trail = FootprintPath.trail(level.getSeed(), i, j, config.cell(), config.chance());
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
