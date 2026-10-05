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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import nl.timtoys.corruptedadventure.ring.Ring;

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

    private final ResourceKey<Level> dimension;
    private final Map<Ring, Integer> fluidLevels;

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

    private void createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState random) {
        chunk.getOrCreateNoiseChunk(access -> {
            NoiseChunk noiseChunk = NoiseChunk.forChunk(access, random, Beardifier.forStructuresInChunk(structureManager, access.getPos()),
                    generatorSettings().value(), globalFluidPicker.get(), blender);
            noiseChunk.aquifer = new RingAquifer(dimension, noiseChunk.aquifer);
            return noiseChunk;
        });
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
        createNoiseChunk(chunk, structureManager, blender, random);
        super.buildSurface(chunk, context, random, structureManager, biomeManager, biomes, blender);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        createNoiseChunk(chunk, structureManager, Blender.of(level), random);
        super.applyCarvers(level, seed, random, biomeManager, structureManager, chunk, step);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}
