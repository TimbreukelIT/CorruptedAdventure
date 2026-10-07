package nl.timtoys.corruptedadventure.command.terrainmap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;
import org.jetbrains.annotations.Nullable;

/**
 * What a terrain map reads from one dimension, captured on the server thread: the generator, its random state (the
 * same noises as the world) and the vertical range. Immutable; every worker thread makes its own
 * {@link TerrainSampler} from it.
 *
 * @param layout the ring layout, or {@code null} outside a ring world (then no ring boundaries are drawn)
 */
record TerrainSource(ResourceKey<Level> dimension, NoiseBasedChunkGenerator generator, RandomState randomState,
                     int minY, int maxY, int cellHeight, boolean ceiling, boolean aquifers, int seaLevel,
                     @Nullable RingLayout layout) {

    /** {@code null} if the dimension has no noise-based generator (e.g. a flat world). */
    @Nullable
    static TerrainSource of(ServerLevel level) {
        ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
        if (!(chunkGenerator instanceof NoiseBasedChunkGenerator generator)) {
            return null;
        }
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        NoiseSettings noise = settings.noiseSettings().clampToHeightAccessor(level);
        RingLayout layout = RingWorld.isActive(level.getServer()) ? RingLayout.current() : null;
        return new TerrainSource(level.dimension(), generator, level.getChunkSource().randomState(),
                noise.minY(), noise.minY() + noise.height() - 1, noise.getCellHeight(),
                level.dimensionType().hasCeiling(), settings.aquifersEnabled(), settings.seaLevel(), layout);
    }

    TerrainSampler newSampler() {
        return new TerrainSampler(this);
    }
}
