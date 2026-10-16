package nl.timtoys.corruptedadventure.worldgen.chunk;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.RingFillGate;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:ring_noise}: vanilla noise generation that keeps the void zone free of fluid. Vanilla fills
 * all open space below sea level with the default fluid and adds local aquifer fluids (lava below Y -10), which would
 * turn the void edge into a sea with lava sheets. Neither is data-driven, hence this subclass; the access transformer
 * opens the two fields it replaces.
 * <p>
 * Two layers: {@link RingFluidPicker} replaces the global sea (also used for base-height queries such as structure
 * placement), {@link RingAquifer} wraps the aquifer of every noise chunk. Every step that may create a chunk's noise
 * chunk first creates it here, because vanilla's own factory is private and a chunk loaded from disk mid-generation
 * has none yet.
 */
public final class RingChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<RingChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(generator -> generator.dimension),
            Codec.unboundedMap(Ring.CODEC, Codec.intRange(-64, 320)).optionalFieldOf("fluid_levels", Map.of())
                    .forGetter(generator -> generator.fluidLevels)
    ).apply(instance, instance.stable(RingChunkGenerator::new)));

    // The surface pass runs synchronously inside buildSurface, so a thread-local hands the rules their chunk's terrain.
    private static final ThreadLocal<VolcanicField> SURFACE_FIELD = new ThreadLocal<>();
    private static final ThreadLocal<IslandField> SURFACE_ISLANDS = new ThreadLocal<>();

    private final ResourceKey<Level> dimension;
    private final Map<Ring, Integer> fluidLevels;
    // One random state per level in practice; remembered so the router is searched once.
    private volatile VolcanicFieldLookup volcanicLookup;
    private volatile IslandFieldLookup islandLookup;

    public RingChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, ResourceKey<Level> dimension) {
        this(biomeSource, settings, dimension, Map.of());
    }

    /** @param fluidLevels per ring the Y up to and including which open space holds the default fluid (default: the dimension's sea level) */
    public RingChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, ResourceKey<Level> dimension,
                              Map<Ring, Integer> fluidLevels) {
        super(biomeSource, settings);
        this.dimension = dimension;
        this.fluidLevels = Map.copyOf(fluidLevels);
        Supplier<Aquifer.FluidPicker> vanilla = this.globalFluidPicker;
        this.globalFluidPicker = Suppliers.memoize(() -> new RingFluidPicker(dimension, vanilla.get(), this.fluidLevels));
    }

    private NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState random) {
        return chunk.getOrCreateNoiseChunk(access -> {
            Aquifer.FluidPicker picker = globalFluidPicker.get() instanceof RingFluidPicker ring ? ring.forNoiseChunk() : globalFluidPicker.get();
            // In fill mode the flat caches of rings that cannot reach this chunk stay 0 (RingFillGate); without blending
            // ring_blend also skips those rings' inputs (vf-perf2).
            NoiseChunk noiseChunk = RingFillGate.duringFill(access.getPos().getMinBlockX(), access.getPos().getMinBlockZ(),
                    blender == Blender.empty(), () -> NoiseChunk.forChunk(access, random, Beardifier.forStructuresInChunk(structureManager, access.getPos()),
                            generatorSettings().value(), picker, blender));
            VolcanicField volcanic = volcanicField(random);
            IslandField islands = islandField(random);
            // The noise chunk's memo returns its own instance of the (equal) field: one column cache for terrain and fluid.
            // vf-perf2: where ring_blend left the ring unmapped (it cannot reach the chunk) this is the random state's
            // instance; the aquifer and the surface rules hardly read it there, and a copy would fill its flat caches.
            noiseChunk.aquifer = new RingAquifer(dimension, noiseChunk.aquifer,
                    volcanic == null ? null : (VolcanicField) noiseChunk.wrap(volcanic),
                    islands == null ? null : (IslandField) noiseChunk.wrap(islands));
            return noiseChunk;
        });
    }

    /**
     * The ring 3 terrain inside this generator's final density (wired by {@code random}), or null if it has none.
     * Thread-safe; also read outside generation (the volcano plumes, package 50b).
     */
    @Nullable
    public VolcanicField volcanicField(RandomState random) {
        VolcanicFieldLookup lookup = volcanicLookup;
        if (lookup == null || lookup.random() != random) {
            lookup = new VolcanicFieldLookup(random, VolcanicField.find(random.router().finalDensity()));
            volcanicLookup = lookup;
        }
        return lookup.field();
    }

    private record VolcanicFieldLookup(RandomState random, @Nullable VolcanicField field) {
    }

    /**
     * The ring 4 islands inside this generator's final density (wired by {@code random}), or null if it has none.
     * Thread-safe; also read outside generation ({@link IslandFields}, the biome source's height hook).
     */
    @Nullable
    public IslandField islandField(RandomState random) {
        IslandFieldLookup lookup = islandLookup;
        if (lookup == null || lookup.random() != random) {
            lookup = new IslandFieldLookup(random, IslandField.find(random.router().finalDensity()));
            islandLookup = lookup;
        }
        return lookup.field();
    }

    private record IslandFieldLookup(RandomState random, @Nullable IslandField field) {
    }

    /**
     * The ring 3 terrain of the chunk whose surface (or carvers) this thread is building (the noise chunk's own
     * instance, which its density and aquifer use, with their column cache), or null: no such pass of a ring generator
     * with ring 3 terrain on this thread. Read by the surface rule condition {@code volcanic_threshold}.
     */
    @Nullable
    public static VolcanicField surfaceVolcanicField() {
        return SURFACE_FIELD.get();
    }

    /**
     * The ring 4 terrain of the chunk whose surface (or carvers) this thread is building (the noise chunk's own instance,
     * with its column cache), or null: no such pass of a ring generator with ring 4 terrain on this thread. Read by the
     * surface rule condition {@code ring4_threshold}.
     */
    @Nullable
    public static IslandField surfaceIslandField() {
        return SURFACE_ISLANDS.get();
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunk) {
        createNoiseChunk(chunk, structureManager, blender, randomState);
        return super.createBiomes(randomState, blender, structureManager, chunk);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        createNoiseChunk(chunk, structureManager, blender, randomState);
        return super.fillFromNoise(blender, randomState, structureManager, chunk);
    }

    @Override
    public void buildSurface(ChunkAccess chunk, WorldGenerationContext context, RandomState random, StructureManager structureManager,
                             BiomeManager biomeManager, Registry<Biome> biomes, Blender blender) {
        NoiseChunk noiseChunk = createNoiseChunk(chunk, structureManager, blender, random);
        withSurfaceField(noiseChunk, random, () -> super.buildSurface(chunk, context, random, structureManager, biomeManager, biomes, blender));
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        NoiseChunk noiseChunk = createNoiseChunk(chunk, structureManager, Blender.of(level), random);
        // The aquifer keeps carvers out of the band around lava (42b); it must know that a carver asks.
        RingAquifer aquifer = noiseChunk.aquifer instanceof RingAquifer ring ? ring : null;
        if (aquifer != null) {
            aquifer.carving(true);
        }
        try {
            // Carvers ask the surface rules for the top material where they cut through grass.
            withSurfaceField(noiseChunk, random, () -> super.applyCarvers(level, seed, random, biomeManager, structureManager, chunk, step));
        } finally {
            if (aquifer != null) {
                aquifer.carving(false);
            }
        }
    }

    /**
     * Runs a pass that evaluates surface rules, with this chunk's ring 3 terrain for {@link #surfaceVolcanicField} and
     * its ring 4 terrain for {@link #surfaceIslandField}.
     */
    private void withSurfaceField(NoiseChunk noiseChunk, RandomState random, Runnable pass) {
        VolcanicField volcanic = volcanicField(random);
        IslandField islands = islandField(random);
        SURFACE_FIELD.set(volcanic == null ? null : (VolcanicField) noiseChunk.wrap(volcanic));
        SURFACE_ISLANDS.set(islands == null ? null : (IslandField) noiseChunk.wrap(islands));
        try {
            pass.run();
        } finally {
            SURFACE_FIELD.remove();
            SURFACE_ISLANDS.remove();
        }
    }

    /**
     * Base heights (structure placement, spawn) run vanilla's column iteration, which builds a one-cell noise chunk of
     * its own; in fill mode its flat caches skip the rings that cannot reach the column (39f, as for a chunk). The
     * decision covers the 16 blocks from the cell's corner, a superset of the cell's own cache points.
     */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return RingFillGate.duringFill(cellCorner(x), cellCorner(z), () -> super.getBaseHeight(x, z, type, level, random));
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        return RingFillGate.duringFill(cellCorner(x), cellCorner(z), () -> super.getBaseColumn(x, z, height, random));
    }

    private int cellCorner(int block) {
        int width = generatorSettings().value().noiseSettings().getCellWidth();
        return Math.floorDiv(block, width) * width;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}
