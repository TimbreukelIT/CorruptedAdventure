package nl.timtoys.corruptedadventure.worldgen.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.biome.RingBiomeSource;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Warms the JIT on the ring terrain right after the server starts ({@code [performance] worldgenWarmup} in the common
 * config). C2 compiles a method with the decompile counts of everything it inlines; shared worldgen helpers collect
 * those during a session, and code compiled for the first time late in a session can get traps that deoptimize on every
 * pass and never recompile (2026-10-26: ring 5 terrain 692 times slower after a session in rings 1–4). Code that runs
 * while the JVM is young traps, recompiles and remembers the trap; so this generates the region types of every ring
 * dimension ({@link WarmupPlan}) early, {@link #ROUNDS} times each.
 * <p>
 * Detached chunks only: a fresh {@link ProtoChunk} per spot, never added to the level and never saved, through the
 * biome, noise (with the aquifer) and surface steps; no carvers, features or structures, no level access beyond reads.
 * One chunk at a time: a daemon thread at minimum priority runs the plan and the surface step, the biome and noise
 * steps run as tasks on the shared worldgen pool (one at a time, so one of its workers). Once per JVM, since the JIT's
 * state is the process's. A stopping server cancels it and waits at most {@link #STOP_WAIT_MILLIS} for the chunk in
 * flight; a thread that is still running after that only finishes that chunk (it writes nothing), and no new warm-up
 * starts until it has ended.
 */
public final class WorldgenWarmup {
    /** Rounds over every region type; 48 chunks per round with the default rings (27 overworld, 21 Nether). */
    static final int ROUNDS = 2;
    private static final long STOP_WAIT_MILLIS = 1_000L;
    /** The warm-up whose thread is alive; only that thread clears it, when it ends. */
    private static final AtomicReference<WorldgenWarmup> RUNNING = new AtomicReference<>();
    /** A warm-up ran to the end in this JVM. */
    private static volatile boolean done;

    private final RingLayout layout;
    private final Map<ResourceKey<Level>, Target> targets;
    private final Thread thread;
    private volatile boolean cancelled;

    private WorldgenWarmup(RingLayout layout, Map<ResourceKey<Level>, Target> targets) {
        this.layout = layout;
        this.targets = targets;
        this.thread = new Thread(this::run, "ca-worldgen-warmup");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ServerStartedEvent.class, event -> start(event.getServer()));
        gameBus.addListener(ServerStoppingEvent.class, event -> stop());
    }

    private static void start(MinecraftServer server) {
        if (done || !CommonConfig.worldgenWarmup()) {
            return;
        }
        if (RUNNING.get() != null) {
            // A singleplayer world switch while the last world's warm-up still finishes its chunk.
            skipped();
            return;
        }
        try {
            Map<ResourceKey<Level>, Target> targets = new LinkedHashMap<>();
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getChunkSource().getGenerator() instanceof RingChunkGenerator generator
                        && generator.getBiomeSource() instanceof RingBiomeSource) {
                    WorldOptions options = server.getWorldData().worldGenOptions();
                    targets.put(level.dimension(), new Target(level, generator, options));
                }
            }
            if (targets.isEmpty()) {
                return;
            }
            WorldgenWarmup warmup = new WorldgenWarmup(RingLayout.current(), targets);
            if (RUNNING.compareAndSet(null, warmup)) {
                warmup.thread.start();
            } else {
                skipped();
            }
        } catch (RuntimeException failure) {
            // An optimisation: never a reason for the server not to start.
            CorruptedAdventure.LOGGER.warn("Worldgen warm-up could not start", failure);
        }
    }

    private static void skipped() {
        CorruptedAdventure.LOGGER.info("Worldgen warm-up skipped: the previous server's warm-up is still stopping");
    }

    private static void stop() {
        WorldgenWarmup warmup = RUNNING.get();
        if (warmup == null) {
            return;
        }
        warmup.cancelled = true;
        try {
            warmup.thread.join(STOP_WAIT_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void run() {
        long start = System.nanoTime();
        Map<ResourceKey<Level>, Integer> counts = new LinkedHashMap<>();
        targets.keySet().forEach(key -> counts.put(key, 0));
        WarmupPlan.Spot current = null;
        try {
            List<WarmupPlan.Dimension> dimensions = new ArrayList<>();
            targets.values().forEach(target -> dimensions.add(target.planned()));
            List<WarmupPlan.Spot> spots = WarmupPlan.spots(layout, dimensions, ROUNDS);
            long noGlitch = spots.stream().filter(spot -> spot.region().equals(WarmupPlan.NO_GLITCH_FIELD)).count();
            if (noGlitch > 0) {
                CorruptedAdventure.LOGGER.info("Worldgen warm-up found no glitch field along ring 5's inner band in {} "
                        + "of {} rounds; it warms a plain inner band column there", noGlitch, ROUNDS);
            }
            for (WarmupPlan.Spot spot : spots) {
                if (cancelled) {
                    break;
                }
                current = spot;
                targets.get(spot.dimension()).generate(spot.chunk());
                counts.merge(spot.dimension(), 1, Integer::sum);
            }
            String perDimension = counts.entrySet().stream()
                    .map(entry -> entry.getValue() + " chunks in " + entry.getKey().location())
                    .collect(Collectors.joining(", "));
            CorruptedAdventure.LOGGER.info(String.format(Locale.ROOT, "Worldgen warm-up %s %.1f s: %s",
                    cancelled ? "stopped by the server stopping after" : "done in", (System.nanoTime() - start) / 1e9,
                    perDimension));
            done = !cancelled;
        } catch (RuntimeException failure) {
            // A stopping server clears the ring layout under a chunk in flight; only a failure while running is news.
            if (!cancelled) {
                CorruptedAdventure.LOGGER.warn("Worldgen warm-up failed after {} chunks (at {}); skipping the rest",
                        counts.values().stream().mapToInt(Integer::intValue).sum(), current, failure);
            }
        } finally {
            RUNNING.compareAndSet(this, null);
        }
    }

    /** One ring dimension: what a detached chunk needs, captured on the server thread. */
    private static final class Target {
        private final ServerLevel level;
        private final RingChunkGenerator generator;
        private final RandomState random;
        private final StructureManager structures;
        private final Registry<Biome> biomes;
        private final WorldGenerationContext context;
        private final long biomeZoomSeed;

        Target(ServerLevel level, RingChunkGenerator generator, WorldOptions options) {
            this.level = level;
            this.generator = generator;
            this.random = level.getChunkSource().randomState();
            this.structures = new NoStructures(level, options);
            this.biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            // Like the WorldGenRegion of the surface step: the level's height, the generator's depth.
            this.context = new WorldGenerationContext(generator, level);
            this.biomeZoomSeed = BiomeManager.obfuscateSeed(level.getSeed());
        }

        /** The plan's view of this dimension; ring 5's extras where the generator has ring 5 terrain. */
        WarmupPlan.Dimension planned() {
            Ring5Field ring5 = generator.ring5Field(random);
            if (ring5 == null) {
                return new WarmupPlan.Dimension(level.dimension(), null);
            }
            List<WarmupPlan.Point> castles = ring5.castleSites().stream()
                    .map(site -> new WarmupPlan.Point(site.x(), site.z())).toList();
            int quartY = QuartPos.fromBlock(generator.getSeaLevel());
            return new WarmupPlan.Dimension(level.dimension(), new WarmupPlan.Ring5Spots(castles,
                    (x, z) -> generator.getBiomeSource()
                            .getNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z), random.sampler())
                            .is(ModBiomes.GLITCH_FIELDS)));
        }

        /** The biome, noise and surface steps of a new chunk at {@code pos} that nothing else ever sees. */
        void generate(ChunkPos pos) {
            ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, biomes, null);
            // A new world's chunks have no old chunks around them: no blending, as here.
            generator.createBiomes(random, Blender.empty(), structures, chunk).join();
            // Each step finds the chunk at the status the pipeline gives it after the step before.
            chunk.setPersistedStatus(ChunkStatus.BIOMES);
            generator.fillFromNoise(Blender.empty(), random, structures, chunk).join();
            chunk.setPersistedStatus(ChunkStatus.NOISE);
            // The surface's zoomed biome lookups reach into the neighbours; a region would read those chunks' biomes,
            // which are the biome source's.
            BiomeManager biomeManager = new BiomeManager((x, y, z) ->
                    QuartPos.toSection(x) == pos.x && QuartPos.toSection(z) == pos.z
                            ? chunk.getNoiseBiome(x, y, z)
                            : generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()),
                    biomeZoomSeed);
            generator.buildSurface(chunk, context, random, structures, biomeManager, biomes, Blender.empty());
        }
    }

    /**
     * A structure manager for a detached chunk: no structures anywhere, so no beardifier. The real one would ask the
     * level for the chunk's structure references, which loads (or generates) the real chunk. Every lookup that reaches
     * the level or the structure check answers "none" (the others go through these), so a later caller cannot stop the
     * warm-up.
     */
    private static final class NoStructures extends StructureManager {
        NoStructures(ServerLevel level, WorldOptions options) {
            // No structure check: nothing here places structures.
            super(level, options, null);
        }

        @Override
        public List<StructureStart> startsForStructure(ChunkPos chunkPos, Predicate<Structure> structurePredicate) {
            return List.of();
        }

        @Override
        public List<StructureStart> startsForStructure(SectionPos sectionPos, Structure structure) {
            return List.of();
        }

        @Override
        public void fillStartsForStructure(Structure structure, LongSet structureRefs,
                                           Consumer<StructureStart> startConsumer) {
        }

        @Override
        public boolean hasAnyStructureAt(BlockPos pos) {
            return false;
        }

        @Override
        public Map<Structure, LongSet> getAllStructuresAt(BlockPos pos) {
            return Map.of();
        }

        @Override
        public StructureCheckResult checkStructurePresence(ChunkPos chunkPos, Structure structure,
                                                           StructurePlacement placement, boolean skipKnownStructures) {
            return StructureCheckResult.START_NOT_PRESENT;
        }

        @Override
        public void addReference(StructureStart structureStart) {
        }
    }
}
