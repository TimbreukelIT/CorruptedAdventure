package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The eye-catchers of ring 2's flat biomes (G2 ring 2, decision G2-R2-1, 2026-10-25): a salt dome with a crystal cave in
 * {@code salt_flats}, a half-buried giant skeleton in {@code dune_sea} and an ancient giant palm beside a spring pool in
 * {@code oasis_flats}. Worldgen features, no structures.
 * <p>
 * Each kind has its own {@link Landmarks.Grid} of ~384 blocks: at most one landmark per cell, its place and shape a pure
 * function of the world seed, the cell and the pure terrain. Ring 2's terrain is JSON density functions, so "pure" here
 * means the generator's own column iteration ({@link ChunkGenerator#getBaseHeight}: the noise, never a block of a chunk)
 * and the biome source. Every chunk the landmark reaches computes the same plan and writes only its own part
 * ({@link Ring2FlatLandmarkFeature}); the whole footprint lies {@link Kind#biomeMargin} inside the biome, so every chunk
 * it touches runs the feature. Amounts are starting values.
 */
public final class Ring2FlatLandmarks {
    /** The landmark kinds. */
    public enum Kind implements StringRepresentable {
        /** A dome of salt blocks, 8–15 high and 20–30 wide, hollow, lined with salt crystals, one or two ways in. */
        SALT_DOME("salt_dome", 0x5A_17_D0_3EL, ModBiomes.SALT_FLATS, new Landmarks.Grid(384, 0.8F, 8, 40), 16, 10),
        /** The skeleton of something huge, 22–30 long, ribs as arches out of the sand, half sunk into a dune. */
        DUNE_SKELETON("dune_skeleton", 0x5C_E1_E7_0AL, ModBiomes.DUNE_SEA, new Landmarks.Grid(384, 0.75F, 8, 40), 20, 8),
        /** An ancient palm with a 2 × 2 trunk, 20–28 high, beside a small spring pool under a rock. */
        GIANT_PALM("giant_palm", 0x6A_1A_9A_13L, ModBiomes.OASIS_FLATS, new Landmarks.Grid(384, 0.85F, 10, 40), 22, 6);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;
        private final long salt;
        private final ResourceKey<Biome> biome;
        private final Landmarks.Grid grid;
        private final int reach;
        private final int biomeMargin;

        Kind(String name, long salt, ResourceKey<Biome> biome, Landmarks.Grid grid, int reach, int biomeMargin) {
            this.name = name;
            this.salt = salt;
            this.biome = biome;
            this.grid = grid;
            this.reach = reach;
            this.biomeMargin = biomeMargin;
        }

        public ResourceKey<Biome> biome() {
            return biome;
        }

        public Landmarks.Grid grid() {
            return grid;
        }

        /** The farthest block of a landmark from its anchor, along x or z. */
        public int reach() {
            return reach;
        }

        /** The biome holds all round the footprint this far beyond its reach. */
        public int biomeMargin() {
            return biomeMargin;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** One landmark: kind, anchor column, its pure ground (first open block) and the seed of its shape. */
    public record Site(Kind kind, int x, int z, int ground, long seed) {
    }

    /** What sites and shapes are decided from: one world's pure terrain and biomes. */
    interface Terrain {
        /** The first open block over the terrain at (x, z) (water counts as terrain). */
        int surface(int x, int z);

        /** The first open block over the terrain's solid ground at (x, z) (under water: the floor). */
        int floor(int x, int z);

        /** Whether (x, z) and eight columns {@code radius} round it lie in {@code biome}. */
        boolean biomeAround(int x, int z, int radius, ResourceKey<Biome> biome);
    }

    // The plans of the last few sites (all chunks round a landmark ask for it in a row).
    private static final Map<Site, LandmarkPlan> PLANS = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Site, LandmarkPlan> eldest) {
            return size() > 32;
        }
    });

    private Ring2FlatLandmarks() {
    }

    /** The landmarks of {@code kind} whose anchor lies within {@code reach} (per axis) of the chunk. */
    static List<Site> reaching(WorldGenLevel level, Kind kind, ChunkPos chunk, int reach) {
        Terrain terrain = terrain(level);
        List<Site> sites = new ArrayList<>();
        // A snapped anchor lies up to SNAP from its candidate: ask a little farther, then keep the true reach.
        for (Landmarks.Candidate candidate : Landmarks.reaching(Landmarks.world(level), level.getSeed(), kind.salt, kind.grid, chunk,
                reach + SNAP, c -> accepts(terrain, kind, anchorX(kind, c.x()), anchorZ(kind, c.z())))) {
            Site site = site(terrain, kind, candidate);
            if (site.x() >= chunk.getMinBlockX() - reach && site.x() <= chunk.getMaxBlockX() + reach
                    && site.z() >= chunk.getMinBlockZ() - reach && site.z() <= chunk.getMaxBlockZ() + reach) {
                sites.add(site);
            }
        }
        return sites;
    }

    /** Every landmark of every kind whose anchor lies in the box ({@code /ca flatmarks}: test tooling). */
    public static List<Site> centredIn(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ) {
        Terrain terrain = terrain(level);
        List<Site> found = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            int size = kind.grid.cellSize();
            for (int cz = Math.floorDiv(minZ, size); cz <= Math.floorDiv(maxZ, size); cz++) {
                for (int cx = Math.floorDiv(minX, size); cx <= Math.floorDiv(maxX, size); cx++) {
                    Landmarks.Candidate candidate = Landmarks.cachedInCell(Landmarks.world(level), level.getSeed(), kind.salt, kind.grid, cx, cz,
                            c -> accepts(terrain, kind, anchorX(kind, c.x()), anchorZ(kind, c.z())));
                    if (candidate != null) {
                        Site site = site(terrain, kind, candidate);
                        if (site.x() >= minX && site.x() <= maxX && site.z() >= minZ && site.z() <= maxZ) {
                            found.add(site);
                        }
                    }
                }
            }
        }
        return found;
    }

    /** Whether (x, z) lies within {@code margin} of the footprint (anchor ± reach) of a landmark of {@code kind}. */
    static boolean near(WorldGenLevel level, Kind kind, int x, int z, int margin) {
        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        for (Site site : reaching(level, kind, chunk, kind.reach + margin)) {
            if (Math.abs(site.x() - x) <= kind.reach + margin && Math.abs(site.z() - z) <= kind.reach + margin) {
                return true;
            }
        }
        return false;
    }

    /** The shape of {@code site} (cached; pure in the site and the terrain). */
    static LandmarkPlan plan(WorldGenLevel level, Site site) {
        LandmarkPlan cached = PLANS.get(site);
        if (cached != null) {
            return cached;
        }
        LandmarkPlan plan = build(site, memo(terrain(level)));
        PLANS.put(site, plan);
        return plan;
    }

    static LandmarkPlan build(Site site, Terrain terrain) {
        return switch (site.kind()) {
            case SALT_DOME -> SaltDomeShape.build(site);
            case DUNE_SKELETON -> DuneSkeletonShape.build(site, terrain::surface);
            case GIANT_PALM -> GiantPalmShape.build(site, terrain);
        };
    }

    private static Site site(Terrain terrain, Kind kind, Landmarks.Candidate candidate) {
        int x = anchorX(kind, candidate.x());
        int z = anchorZ(kind, candidate.z());
        return new Site(kind, x, z, terrain.surface(x, z), candidate.random().nextLong());
    }

    /** How far {@link #anchorX} may move a candidate. */
    static final int SNAP = 8;

    /**
     * The anchor of a candidate: the giant palm's spring pool is a {@code surface_pool} in one chunk, so its anchor sits
     * in the middle of its chunk (the pool and its rim then never shift); the other kinds keep the candidate.
     */
    static int anchorX(Kind kind, int x) {
        return kind == Kind.GIANT_PALM ? snap(x) : x;
    }

    static int anchorZ(Kind kind, int z) {
        return kind == Kind.GIANT_PALM ? snap(z) : z;
    }

    /**
     * Whether the pure terrain and biomes suit a landmark of {@code kind} anchored at (x, z): the biome all round, the
     * anchor's column, then the ground round it.
     */
    static boolean accepts(Terrain terrain, Kind kind, int x, int z) {
        // The biome first: nine lookups of the biome source cost far less than one column of the generator.
        if (!terrain.biomeAround(x, z, kind.reach + kind.biomeMargin, kind.biome)) {
            return false;
        }
        int ground = terrain.surface(x, z);
        boolean first = switch (kind) {
            // The bare crust (Y 65, first open block 66), not a raised sand island.
            case SALT_DOME -> ground == SaltDomeShape.CRUST_TOP && terrain.floor(x, z) == ground;
            // A dune valley or a lower flank, never a crest.
            case DUNE_SKELETON -> ground <= DuneSkeletonShape.MAX_GROUND && terrain.floor(x, z) == ground;
            // Dry flat sand for the spring pool (the anchor) on the oasis plain.
            case GIANT_PALM -> ground <= GiantPalmShape.MAX_GROUND && terrain.floor(x, z) == ground;
        };
        if (!first) {
            return false;
        }
        return switch (kind) {
            case SALT_DOME -> ringLevel(terrain, x, z, new int[] {6, 11, 16}, ground, 0, true);
            case DUNE_SKELETON -> ringLevel(terrain, x, z, new int[] {8, 15}, ground, 9, false);
            case GIANT_PALM -> GiantPalmShape.suits(terrain, x, z, ground);
        };
    }

    /**
     * Whether the ground on rings of {@code radii} round (x, z) (eight directions each) lies within {@code tolerance} of
     * {@code ground}, and (if {@code dry}) is dry land.
     */
    static boolean ringLevel(Terrain terrain, int x, int z, int[] radii, int ground, int tolerance, boolean dry) {
        for (int r : radii) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                int px = x + (int) Math.round(Math.cos(angle) * r);
                int pz = z + (int) Math.round(Math.sin(angle) * r);
                int g = terrain.surface(px, pz);
                if (Math.abs(g - ground) > tolerance || dry && terrain.floor(px, pz) != g) {
                    return false;
                }
            }
        }
        return true;
    }

    /** {@code terrain} with its heights remembered per column (a shape asks for the same columns many times). */
    static Terrain memo(Terrain terrain) {
        Map<Long, Integer> surface = new HashMap<>();
        Map<Long, Integer> floor = new HashMap<>();
        return new Terrain() {
            @Override
            public int surface(int x, int z) {
                return surface.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> terrain.surface(x, z));
            }

            @Override
            public int floor(int x, int z) {
                return floor.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> terrain.floor(x, z));
            }

            @Override
            public boolean biomeAround(int x, int z, int radius, ResourceKey<Biome> biome) {
                return terrain.biomeAround(x, z, radius, biome);
            }
        };
    }

    /** The pure terrain of {@code level}'s generator: its column iteration and its biome source. */
    static Terrain terrain(WorldGenLevel level) {
        ChunkGenerator generator = level.getLevel().getChunkSource().getGenerator();
        RandomState random = level.getLevel().getChunkSource().randomState();
        return new Terrain() {
            @Override
            public int surface(int x, int z) {
                return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
            }

            @Override
            public int floor(int x, int z) {
                return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
            }

            @Override
            public boolean biomeAround(int x, int z, int radius, ResourceKey<Biome> biome) {
                return Landmarks.biomeAround(level, x, z, radius, biome);
            }
        };
    }

    /** A random for a part of a site's shape. */
    static RandomSource random(Site site, long part) {
        return RandomSource.create(site.seed() ^ part * 0x9E3779B97F4A7C15L);
    }

    /** The anchor's chunk-relative block, for anchors snapped to a chunk's middle. */
    static int snap(int block) {
        return (block & ~15) + Mth.clamp(block & 15, 7, 8);
    }
}
