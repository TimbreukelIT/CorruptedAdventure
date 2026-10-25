package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
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
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * The biome-own landmarks of ring 3 (G2, decision 3 = A, 2026-10-25): one per kind per world cell of {@link #CELL}
 * blocks at most, in about half the cells: a charred giant trunk half in a dune ({@code ashen_wastes}), a solidified
 * obsidian flow down a flank ({@code volcanic_peaks}), a whale skeleton half in the sand ({@code dried_seabed}), a basalt
 * organ ({@code basalt_flows}), an ember giant with a glowing hollow ({@code ember_woods}), a flight of sinter terraces
 * with boiling pools ({@code sulfur_fields}) and a titan's weapon slanting out of the ground ({@code titan_fields}).
 * Worldgen features, no structures, no loot; none in {@code dormant_caldera} and {@code steam_fens}.
 * <p>
 * As the ring 4 landmarks ({@link BlightedLandmarks}): each landmark is a pure function of the world seed and its cell.
 * A cell that tries at all ({@link #CHANCE}) draws up to {@link #TRIES} candidate anchors (hashes of the seed, the kind,
 * the cell and the try) and keeps the first whose pure terrain ({@link VolcanicField}) and biome suit: the anchor on
 * plain ground of the kind's biome, the shape ({@link VolcanicPlan}, drawn from the site's own seed and the pure ground
 * only) accepts the terrain (level or a slope, per kind), and every column the shape writes into lies on plain ground
 * of the same biome ({@link #fits}). So every chunk the landmark covers computes the same landmark and writes its own part
 * ({@link VolcanicLandmarkFeature}), whatever the order of generation; and since one column of every 8 × 8 block square
 * of the shape is in the biome, every chunk the shape touches has the biome within its 3 × 3 chunks and runs the feature,
 * even where a landmark (the flow, the terraces) reaches further than a chunk from its anchor. An anchor keeps its
 * kind's reach from the cell's edge, so two landmarks of a kind never meet.
 */
public final class VolcanicLandmarks {
    /** The size of a landmark cell (blocks): at most one landmark of a kind per cell. */
    static final int CELL = 400;
    /** The share of cells that try at all: about one landmark per 550 blocks of its biome. */
    static final double CHANCE = 0.5;
    /** Candidate anchors tried per cell, the first that suits wins. */
    static final int TRIES = 6;
    /** The highest a thin solid (needle, arch, rib, hoodoo) may reach over a column the landmark takes (blocks). */
    private static final int THIN_SCAN = 40;

    /** The landmark kinds. */
    public enum Kind implements StringRepresentable {
        /** A dead 2 × 2 trunk of charred wood, 15–25 high, broken branches, its foot half in an ash dune. */
        CHARRED_GIANT("charred_giant", 0xC4_A2_6DL, 10, ModBiomes.ASHEN_WASTES),
        /** A solidified tongue of obsidian (crying obsidian, a blackstone rim), 30–60 long, down a flank. */
        OBSIDIAN_FLOW("obsidian_flow", 0x0B_51_D1L, 64, ModBiomes.VOLCANIC_PEAKS),
        /** The skeleton of a whale, 25–35 long, ribs as arches, half in the sand. */
        WHALE_SKELETON("whale_skeleton", 0x3A_1E_5CL, 20, ModBiomes.DRIED_SEABED),
        /** A cluster of 10–30 basalt columns, 8–20 high, smooth basalt heads. */
        BASALT_ORGAN("basalt_organ", 0x0A_6A_17L, 14, ModBiomes.BASALT_FLOWS),
        /** An ember tree of 20–28 with a 2 × 2 trunk, a broad crown and a glowing hollow in its foot. */
        EMBER_GIANT("ember_giant", 0xE3_B6_1AL, 12, ModBiomes.EMBER_WOODS),
        /** A flight of 4–6 sinter terraces with boiling pools, 20–40 wide, against a slope. */
        SINTER_TERRACES("sinter_terraces", 0x51_7E_6AL, 24, ModBiomes.SULFUR_FIELDS),
        /** A titan's sword or spear, half buried, slanting 20–30 out of the ground. */
        TITAN_WEAPON("titan_weapon", 0x71_7A_4EL, 22, ModBiomes.TITAN_FIELDS);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;
        private final long salt;
        private final int reach;
        private final ResourceKey<Biome> biome;

        Kind(String name, long salt, int reach, ResourceKey<Biome> biome) {
            this.name = name;
            this.salt = salt;
            this.reach = reach;
            this.biome = biome;
        }

        /** The farthest block of the landmark from its anchor, along x or z. */
        public int reach() {
            return reach;
        }

        public ResourceKey<Biome> biome() {
            return biome;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * One landmark: its kind, anchor column (the flow's source, the middle of the others), the first open block over
     * the anchor's pure ground and the seed of its shape.
     */
    public record Site(Kind kind, int x, int z, int ground, long seed) {
        /** Whether (px, pz) lies within the landmark's reach widened by {@code margin}. */
        boolean covers(int px, int pz, int margin) {
            int r = kind.reach() + margin;
            return Math.abs(px - x) <= r && Math.abs(pz - z) <= r;
        }
    }

    /** What the sites are decided from: the pure terrain and biomes of one world. */
    interface World {
        long seed();

        /** The first open block over the pure terrain at (x, z). */
        int ground(int x, int z);

        /**
         * Whether the pure terrain at (x, z) suits a landmark: no lava (river, bank, lake), crater, collapse, floating
         * island, volcano flank or cliff, no thin solid standing over it.
         */
        boolean plain(int x, int z);

        /** The biome at the column's ground (null: unregistered). */
        @Nullable
        ResourceKey<Biome> biome(int x, int z);
    }

    // Per world (random state): the decided sites per kind and cell; weak, so a closed world's cache can go.
    private static final Map<RandomState, Map<Long, Optional<Site>>> SITES = Collections.synchronizedMap(new WeakHashMap<>());
    // The plans of the last few sites (several chunks ask for the same one in a row).
    private static final Map<Site, VolcanicPlan> PLANS = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Site, VolcanicPlan> eldest) {
            return size() > 48;
        }
    });

    private VolcanicLandmarks() {
    }

    /** Every landmark whose anchor lies in the box, kind by kind ({@code /ca landmarks}: test tooling). */
    public static List<Site> centredIn(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ) {
        List<Site> found = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (Site site : within(level, kind, minX, minZ, maxX, maxZ, -kind.reach())) {
                if (site.x() >= minX && site.x() <= maxX && site.z() >= minZ && site.z() <= maxZ) {
                    found.add(site);
                }
            }
        }
        return found;
    }

    /** The landmarks of {@code kind} whose reach (widened by {@code margin}) comes into the box. */
    static List<Site> within(WorldGenLevel level, Kind kind, int minX, int minZ, int maxX, int maxZ, int margin) {
        World world = world(level);
        if (world == null) {
            return List.of();
        }
        return within(world, cache(level), kind, minX, minZ, maxX, maxZ, margin);
    }

    static List<Site> within(World world, Map<Long, Optional<Site>> cache, Kind kind, int minX, int minZ, int maxX, int maxZ, int margin) {
        int reach = kind.reach() + margin;
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

    /** Every kind: a ground feature keeps clear of all of them (each kind only stands in its own biome). */
    static final Set<Kind> ALL = java.util.Collections.unmodifiableSet(java.util.EnumSet.allOf(Kind.class));

    /** Whether (x, z) lies within {@code margin} of the reach of a landmark of one of {@code kinds}. */
    static boolean near(WorldGenLevel level, Set<Kind> kinds, int x, int z, int margin) {
        World world = world(level);
        if (world == null) {
            return false;
        }
        Map<Long, Optional<Site>> cache = cache(level);
        for (Kind kind : kinds) {
            for (Site site : within(world, cache, kind, x, z, x, z, margin)) {
                if (site.covers(x, z, margin)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<Long, Optional<Site>> cache(WorldGenLevel level) {
        return SITES.computeIfAbsent(level.getLevel().getChunkSource().randomState(), key -> new ConcurrentHashMap<>());
    }

    /** The shape of {@code site} (cached; pure in the site and the terrain). */
    static VolcanicPlan plan(WorldGenLevel level, Site site) {
        VolcanicPlan cached = PLANS.get(site);
        if (cached != null) {
            return cached;
        }
        World world = world(level);
        VolcanicPlan plan = world == null ? null : build(world, site);
        if (plan == null) {
            plan = new VolcanicPlan(site.x(), site.z(), 0);
        }
        PLANS.put(site, plan);
        return plan;
    }

    /** The shape of {@code site}, or null where the terrain does not suit it. */
    @Nullable
    static VolcanicPlan build(World world, Site site) {
        IntBinaryOperator ground = world::ground;
        return switch (site.kind()) {
            case CHARRED_GIANT -> CharredGiantShape.build(site, ground);
            case OBSIDIAN_FLOW -> ObsidianFlowShape.build(site, ground);
            case WHALE_SKELETON -> WhaleSkeletonShape.build(site, ground);
            case BASALT_ORGAN -> BasaltOrganShape.build(site, ground);
            case EMBER_GIANT -> EmberGiantShape.build(site, ground);
            case SINTER_TERRACES -> SinterTerraceShape.build(site, ground);
            case TITAN_WEAPON -> TitanWeaponShape.build(site, ground);
        };
    }

    /** The site of {@code kind} in cell (i, j), or null; a function of the seed, the kind and the cell only. */
    @Nullable
    static Site site(World world, Map<Long, Optional<Site>> cache, Kind kind, int i, int j) {
        long key = ((long) kind.ordinal() << 58) ^ ((long) i << 29) ^ (j & 0x1FFF_FFFFL);
        Optional<Site> cached = cache.get(key);
        if (cached == null) {
            cached = Optional.ofNullable(decide(world, kind, i, j));
            cache.put(key, cached);
        }
        return cached.orElse(null);
    }

    @Nullable
    private static Site decide(World world, Kind kind, int i, int j) {
        long cell = mix(world.seed() ^ kind.salt ^ mix(i * 0x9E3779B97F4A7C15L + j * 0xC2B2AE3D27D4EB4FL));
        if (unit(cell, 99) >= CHANCE) {
            return null;
        }
        int margin = kind.reach() + 1;
        for (int t = 0; t < TRIES; t++) {
            int x = i * CELL + margin + (int) (unit(cell, 3 * t) * (CELL - 2 * margin));
            int z = j * CELL + margin + (int) (unit(cell, 3 * t + 1) * (CELL - 2 * margin));
            if (!kind.biome().equals(world.biome(x, z)) || !world.plain(x, z)) {
                continue;
            }
            Site site = new Site(kind, x, z, world.ground(x, z), mix(cell + 0x51_7E_5EEDL * (t + 1)));
            VolcanicPlan plan = build(world, site);
            if (plan != null && !plan.isEmpty() && fits(world, kind, plan)) {
                PLANS.put(site, plan);
                return site;
            }
        }
        return null;
    }

    /**
     * Whether every column of the plan suits: one column of each 8 × 8 block square it touches in the kind's biome, and
     * plain ground at every fourth column along x and z (and at that one column).
     */
    static boolean fits(World world, Kind kind, VolcanicPlan plan) {
        Set<Long> squares = new HashSet<>();
        for (BlockPos column : plan.columns()) {
            int x = column.getX();
            int z = column.getZ();
            boolean square = squares.add((long) (x >> 3) << 32 | (z >> 3) & 0xFFFFFFFFL);
            if (square && !kind.biome().equals(world.biome(x, z))) {
                return false;
            }
            if ((square || (x & 3) == 0 && (z & 3) == 0) && !world.plain(x, z)) {
                return false;
            }
        }
        return true;
    }

    /** The pure terrain and biomes of {@code level}'s generator, or null (no ring 3 terrain here). */
    @Nullable
    static World world(WorldGenLevel level) {
        VolcanicField field = Ring3Terrain.field(level);
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
                return Mth.ceil(field.column(x, z).height());
            }

            @Override
            public boolean plain(int x, int z) {
                return VolcanicLandmarks.plain(field.column(x, z));
            }

            @Override
            public ResourceKey<Biome> biome(int x, int z) {
                int y = ground(x, z);
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler);
                return biome.unwrapKey().orElse(null);
            }
        };
    }

    /**
     * The plain ground rules of {@link VolcanicFilter.Builder#ground()} on the pure column (no lava bank, crater,
     * collapse, volcano flank or cliff), and no floating island, no lava at the ground and no thin solid (needle, arch,
     * rib, hoodoo; the micro relief's step of a block or two aside) over it.
     */
    static boolean plain(VolcanicField.Column column) {
        if (value(column, VolcanicOutput.Output.LAVA_BANK) >= 0.05 || value(column, VolcanicOutput.Output.CALDERA_MASK) >= 0.001
                || value(column, VolcanicOutput.Output.COLLAPSE) >= 0.001 || value(column, VolcanicOutput.Output.ISLAND_MASK) > 0.0
                || value(column, VolcanicOutput.Output.VOLCANO_RISE) >= 20.0 || value(column, VolcanicOutput.Output.CLIFF) >= 0.3) {
            return false;
        }
        int ground = Mth.ceil(column.height());
        if (column.lavaAt(ground - 1) || column.lavaAt(ground)) {
            return false;
        }
        for (int y = ground + 2; y <= ground + THIN_SCAN; y += 2) {
            if (column.thinSolidAt(y)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the pure ground on two rings round (x, z) (radius and half of it, 8 points each) lies within
     * {@code tolerance} of {@code base}: a level spot for the upright landmarks.
     */
    static boolean level(IntBinaryOperator ground, double x, double z, int radius, int tolerance, int base) {
        for (int ring = 1; ring <= 2; ring++) {
            double r = radius * ring / 2.0;
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4.0 + ring * 0.39;
                if (Math.abs(ground.applyAsInt(Mth.floor(x + r * Math.cos(a)), Mth.floor(z + r * Math.sin(a))) - base) > tolerance) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double value(VolcanicField.Column column, VolcanicOutput.Output output) {
        return VolcanicOutput.columnValue(column, output);
    }

    static long mix(long z) {
        return RandomSupport.mixStafford13(z);
    }

    static double unit(long hash, int n) {
        return (mix(hash + n * 0x632BE59BD9B4E019L) >>> 11) * 0x1.0p-53;
    }
}
