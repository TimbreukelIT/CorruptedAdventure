package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The rare landmarks of the blighted band (ring 4 decor B, 2026-10-19): a drowned giant tree in {@code drowned_forest},
 * a circle of leaning menhirs round a toxic pool in {@code peat_moors} and a great, half rotten bone carcass in
 * {@code peat_moors} or {@code reed_marsh}. Worldgen features, no structures.
 * <p>
 * Each landmark is a pure function of the world seed and a fixed world cell of {@link #CELL} blocks per kind: a few
 * candidate centres per cell (hashes of the seed, the kind, the cell and the try), the first whose pure terrain
 * ({@link IslandField}) and biome (the biome source) suit the kind wins. Its shape ({@link LandmarkPlan}) is drawn from
 * the site's own seed and the pure terrain only. So every chunk the landmark covers computes the same landmark and
 * writes its own part ({@link BlightedLandmarkFeature}), whatever the order of generation; a landmark reaches at most
 * {@link #MAX_REACH} from its centre, so every chunk it touches has the centre's biome within its 3 × 3 chunks and runs
 * the feature.
 */
public final class BlightedLandmarks {
    /** The size of a landmark cell (blocks): at most one landmark of a kind per cell. */
    static final int CELL = 320;
    /** Candidate centres keep this far from a cell's edge: two landmarks of a kind lie at least twice this apart. */
    static final int CELL_MARGIN = 40;
    /** Candidate centres tried per cell, the first that suits wins. */
    static final int TRIES = 6;
    /** The farthest block of a landmark from its centre, along x or z. */
    public static final int MAX_REACH = 16;
    /** A bone carcass keeps this far from a menhir circle. */
    static final int CARCASS_CLEARANCE = 64;
    /** The water's top (the sea level minus one): ring 4's open space under it is water (73). */
    static final int WATER_TOP = 62;

    private static final Set<ResourceKey<Biome>> BLIGHTED = Set.of(ModBiomes.DROWNED_FOREST, ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH);

    /** The landmark kinds. */
    public enum Kind implements StringRepresentable {
        /** A drowned giant tree, half under water, bare branches, moss and fungi ({@code drowned_forest}). */
        DROWNED_GIANT("drowned_giant", 0x5D_0A_61L, 15, Set.of(ModBiomes.DROWNED_FOREST)),
        /** A circle of leaning mossy menhirs round a toxic pool ({@code peat_moors}). */
        MENHIR_CIRCLE("menhir_circle", 0x3E_4C_17L, 16, Set.of(ModBiomes.PEAT_MOORS)),
        /** A great, half rotten carcass of bone blocks, its ribs as arches ({@code peat_moors}, {@code reed_marsh}). */
        BONE_CARCASS("bone_carcass", 0xB0_4E_CAL, 16, Set.of(ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH));

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;
        private final long salt;
        private final int radius;
        private final Set<ResourceKey<Biome>> biomes;

        Kind(String name, long salt, int radius, Set<ResourceKey<Biome>> biomes) {
            this.name = name;
            this.salt = salt;
            this.radius = radius;
            this.biomes = biomes;
        }

        /** The radius of the landmark's footprint (blocks; at most {@link #MAX_REACH}). */
        public int radius() {
            return radius;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * One landmark: its kind, centre column, the first open block over the centre's ground (pure: the terrain's
     * surface, under water the floor) and the seed of its shape.
     */
    public record Site(Kind kind, int x, int z, int ground, long seed) {
        /**
         * Whether (px, pz) lies within the landmark's footprint widened by {@code margin}: the square the shapes keep
         * their blocks in (|dx|, |dz| ≤ radius).
         */
        boolean covers(int px, int pz, int margin) {
            int r = kind.radius() + margin;
            return Math.abs(px - x) <= r && Math.abs(pz - z) <= r;
        }
    }

    /** What the sites are decided from: the pure terrain and biomes of one world. */
    interface World {
        long seed();

        /** The first open block over the ground at (x, z) (the floor under water), from the pure terrain. */
        int ground(int x, int z);

        /** Whether the pure terrain suits a landmark at (x, z): no island over it, no sinkhole, no fall or lift near. */
        boolean plain(int x, int z, int reach);

        double slope(int x, int z);

        /** The biome at the column's ground (null: unregistered). */
        @Nullable
        ResourceKey<Biome> biome(int x, int z);
    }

    // Per world (random state): the decided sites per kind and cell; weak, so a closed world's cache can go.
    private static final Map<RandomState, Map<Long, Optional<Site>>> SITES = Collections.synchronizedMap(new WeakHashMap<>());
    // The plans of the last few sites (several chunks ask for the same one in a row).
    private static final Map<Site, LandmarkPlan> PLANS = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Site, LandmarkPlan> eldest) {
            return size() > 48;
        }
    });

    private BlightedLandmarks() {
    }

    /** Every landmark whose centre lies in the box, kind by kind ({@code /ca landmarks}: test tooling). */
    public static List<Site> centredIn(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ) {
        List<Site> found = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (Site site : within(level, kind, minX, minZ, maxX, maxZ, -kind.radius())) {
                if (site.x() >= minX && site.x() <= maxX && site.z() >= minZ && site.z() <= maxZ) {
                    found.add(site);
                }
            }
        }
        return found;
    }

    /** The landmarks of {@code kind} whose footprint (widened by {@code margin}) reaches into the box. */
    static List<Site> within(WorldGenLevel level, Kind kind, int minX, int minZ, int maxX, int maxZ, int margin) {
        World world = world(level);
        if (world == null) {
            return List.of();
        }
        RandomState state = level.getLevel().getChunkSource().randomState();
        Map<Long, Optional<Site>> cache = SITES.computeIfAbsent(state, key -> new ConcurrentHashMap<>());
        return within(world, cache, kind, minX, minZ, maxX, maxZ, margin);
    }

    static List<Site> within(World world, Map<Long, Optional<Site>> cache, Kind kind, int minX, int minZ, int maxX, int maxZ, int margin) {
        int reach = kind.radius() + margin;
        List<Site> found = new ArrayList<>();
        for (int i = Math.floorDiv(minX - reach, CELL); i <= Math.floorDiv(maxX + reach, CELL); i++) {
            for (int j = Math.floorDiv(minZ - reach, CELL); j <= Math.floorDiv(maxZ + reach, CELL); j++) {
                Site site = site(world, cache, kind, i, j);
                if (site != null && site.x() + reach >= minX && site.x() - reach <= maxX && site.z() + reach >= minZ
                        && site.z() - reach <= maxZ) {
                    found.add(site);
                }
            }
        }
        return found;
    }

    /** Whether (x, z) lies within {@code margin} of any landmark's footprint. */
    static boolean near(WorldGenLevel level, int x, int z, int margin) {
        World world = world(level);
        if (world == null) {
            return false;
        }
        Map<Long, Optional<Site>> cache = SITES.computeIfAbsent(level.getLevel().getChunkSource().randomState(),
                key -> new ConcurrentHashMap<>());
        for (Kind kind : Kind.values()) {
            for (Site site : within(world, cache, kind, x, z, x, z, margin)) {
                if (site.covers(x, z, margin)) {
                    return true;
                }
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
        World world = world(level);
        LandmarkPlan plan = world == null ? new LandmarkPlan() : build(world, site);
        PLANS.put(site, plan);
        return plan;
    }

    static LandmarkPlan build(World world, Site site) {
        return switch (site.kind()) {
            case DROWNED_GIANT -> DrownedGiantShape.build(site);
            case MENHIR_CIRCLE -> MenhirCircleShape.build(site, world::ground);
            case BONE_CARCASS -> BoneCarcassShape.build(site, world::ground);
        };
    }

    /** The site of {@code kind} in cell (i, j), or null; a function of the seed, the kind and the cell only. */
    @Nullable
    static Site site(World world, Map<Long, Optional<Site>> cache, Kind kind, int i, int j) {
        long key = ((long) kind.ordinal() << 58) ^ ((long) i << 29) ^ (j & 0x1FFF_FFFFL);
        Optional<Site> cached = cache.get(key);
        if (cached == null) {
            cached = Optional.ofNullable(decide(world, cache, kind, i, j));
            cache.put(key, cached);
        }
        return cached.orElse(null);
    }

    @Nullable
    private static Site decide(World world, Map<Long, Optional<Site>> cache, Kind kind, int i, int j) {
        long cell = mix(world.seed() ^ kind.salt ^ mix(i * 0x9E3779B97F4A7C15L + j * 0xC2B2AE3D27D4EB4FL));
        for (int t = 0; t < TRIES; t++) {
            int x = i * CELL + CELL_MARGIN + (int) (unit(cell, 3 * t) * (CELL - 2 * CELL_MARGIN));
            int z = j * CELL + CELL_MARGIN + (int) (unit(cell, 3 * t + 1) * (CELL - 2 * CELL_MARGIN));
            if (kind == Kind.MENHIR_CIRCLE) {
                // The pool in the middle stays in its chunk, a block from the edge (SludgePuddle): the centre at 3..12.
                x = (x & ~15) + Mth.clamp(x & 15, 3, 12);
                z = (z & ~15) + Mth.clamp(z & 15, 3, 12);
            }
            if (suits(world, cache, kind, x, z)) {
                return new Site(kind, x, z, world.ground(x, z), mix(cell + 0x51_7E_5EEDL * (t + 1)));
            }
        }
        return null;
    }

    /** Whether the pure terrain and biomes suit a landmark of {@code kind} at (x, z). */
    static boolean suits(World world, Map<Long, Optional<Site>> cache, Kind kind, int x, int z) {
        if (!in(kind.biomes, world.biome(x, z))) {
            return false;
        }
        int r = kind.radius();
        // The whole footprint in the blighted band: every chunk it touches then runs the feature, and it is not cut
        // by a mythical biome's look.
        int[][] around = {{r, 0}, {-r, 0}, {0, r}, {0, -r}, {r * 7 / 10, r * 7 / 10}, {-r * 7 / 10, r * 7 / 10},
                {r * 7 / 10, -r * 7 / 10}, {-r * 7 / 10, -r * 7 / 10}};
        for (int[] d : around) {
            if (!in(BLIGHTED, world.biome(x + d[0], z + d[1]))) {
                return false;
            }
        }
        if (!world.plain(x, z, r)) {
            return false;
        }
        int ground = world.ground(x, z);
        return switch (kind) {
            // Standing in the drowned forest's water, 4 to 14 deep.
            case DROWNED_GIANT -> ground <= WATER_TOP - 3 && ground >= WATER_TOP - 13;
            // On level dry peat: the ring of stones within two blocks of the centre's height.
            case MENHIR_CIRCLE -> ground > WATER_TOP + 1 && world.slope(x, z) < 0.5 && level(world, x, z, ground, around, 2);
            // On land or in water one deep, roughly level along the spine, away from the stone circles.
            case BONE_CARCASS -> ground >= WATER_TOP && world.slope(x, z) < 0.6 && level(world, x, z, ground, around, 3)
                    && clearOfCircles(world, cache, x, z);
        };
    }

    private static boolean in(Set<ResourceKey<Biome>> biomes, @Nullable ResourceKey<Biome> biome) {
        return biome != null && biomes.contains(biome);
    }

    private static boolean level(World world, int x, int z, int ground, int[][] around, int tolerance) {
        for (int[] d : around) {
            if (Math.abs(world.ground(x + d[0], z + d[1]) - ground) > tolerance) {
                return false;
            }
        }
        return true;
    }

    private static boolean clearOfCircles(World world, Map<Long, Optional<Site>> cache, int x, int z) {
        for (Site circle : within(world, cache, Kind.MENHIR_CIRCLE, x, z, x, z, CARCASS_CLEARANCE)) {
            if (Mth.square(circle.x() - x) + Mth.square(circle.z() - z) < CARCASS_CLEARANCE * CARCASS_CLEARANCE) {
                return false;
            }
        }
        return true;
    }

    /** The pure terrain and biomes of {@code level}'s generator, or null (no ring 4 terrain here). */
    @Nullable
    static World world(WorldGenLevel level) {
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return null;
        }
        long seed = level.getSeed();
        BiomeSource biomes = level.getLevel().getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getLevel().getChunkSource().randomState().sampler();
        return new World() {
            @Override
            public long seed() {
                return seed;
            }

            @Override
            public int ground(int x, int z) {
                return Mth.ceil(field.column(x, z).surface());
            }

            @Override
            public boolean plain(int x, int z, int reach) {
                for (int dz = -reach; dz <= reach; dz += reach) {
                    for (int dx = -reach; dx <= reach; dx += reach) {
                        IslandField.Column column = field.column(x + dx, z + dz);
                        if (column.islandMask() > 0.0 || !Double.isNaN(column.sinkholeFloor())) {
                            return false;
                        }
                    }
                }
                return field.waterfallsWithin(x - reach, z - reach, x + reach, z + reach).isEmpty()
                        && field.liftsWithin(x - reach, z - reach, x + reach, z + reach).isEmpty()
                        && field.worldTrees(x - reach, z - reach, x + reach, z + reach).isEmpty();
            }

            @Override
            public double slope(int x, int z) {
                return field.slope(x, z);
            }

            @Override
            public ResourceKey<Biome> biome(int x, int z) {
                int y = ground(x, z);
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler);
                return biome.unwrapKey().orElse(null);
            }
        };
    }

    static long mix(long z) {
        return RandomSupport.mixStafford13(z);
    }

    static double unit(long hash, int n) {
        return (mix(hash + n * 0x632BE59BD9B4E019L) >>> 11) * 0x1.0p-53;
    }
}
