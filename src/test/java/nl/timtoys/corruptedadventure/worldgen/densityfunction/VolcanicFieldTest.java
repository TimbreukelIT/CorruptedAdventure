package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ring 3 terrain model on the test seed, with the noises seeded as the overworld's random state seeds them. */
public class VolcanicFieldTest {
    static final long SEED = 20260930L;
    static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10000, 96, 128),
            RingLayout.noiseSeed(SEED));

    private static VolcanicField field;
    private static List<Volcano> volcanoes;

    static DensityFunction.NoiseHolder noise(String name) {
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters(name);
        return new DensityFunction.NoiseHolder(Holder.direct(parameters), NormalNoise.create(
                new XoroshiroRandomSource(SEED).forkPositional().fromHashOf(ResourceLocation.parse("corruptedadventure:" + name)), parameters));
    }

    /** The field as {@code ring_3/overworld/field.json} configures it (noise keys and settings read from the file). */
    public static VolcanicField field() {
        JsonObject json = WorldgenData.read("density_function/ring_3/overworld/field.json");
        VolcanicSettings settings = VolcanicSettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("settings"))
                .getOrThrow();
        return new VolcanicField(noise(key(json, "hash")), noise(key(json, "plains")), noise(key(json, "rolling")),
                noise(key(json, "flows")), noise(key(json, "heat")), noise(key(json, "ground")), noise(key(json, "channel")),
                noise(key(json, "ridge")), noise(key(json, "warp")), settings, () -> LAYOUT);
    }

    private static String key(JsonObject json, String field) {
        return ResourceLocation.parse(json.get(field).getAsString()).getPath();
    }

    @BeforeAll
    static void setUp() {
        field = field();
        volcanoes = new ArrayList<>();
        int size = field.settings().cellSize();
        for (int i = -6200 / size - 1; i <= 6200 / size; i++) {
            for (int j = -6200 / size - 1; j <= 6200 / size; j++) {
                Volcano v = field.place(i, j);
                if (v != null) {
                    volcanoes.add(v);
                }
            }
        }
    }

    @Test
    void columnsArePureWhateverTheOrderOrCache() {
        VolcanicField other = field();
        for (int k = 0; k < 400; k++) {
            int x = -4200 + k * 7;
            int z = 1500 - k * 3;
            VolcanicField.Column fresh = new VolcanicField.Column();
            other.fill(fresh, x, z, true);
            VolcanicField.Column cached = field.column(x, z);
            assertEquals(fresh.height, cached.height, 0.0);
            assertEquals(fresh.lavaTop, cached.lavaTop);
            assertEquals(fresh.islandCount, cached.islandCount);
        }
        // Reading the same column again after many others gives the same values (caches only remember).
        assertEquals(field.column(-4200, 1500).height, other.column(-4200, 1500).height, 0.0);
    }

    @Test
    void volcanoesStandInTheRingThreeCoreWithTheirHeights() {
        assertTrue(volcanoes.size() > 100, "volcanoes: " + volcanoes.size());
        double inner = 3500 + 64;
        double outer = 6000 - 64;
        int great = 0;
        for (Volcano v : volcanoes) {
            double d = LAYOUT.effectiveDistance(v.x(), v.z());
            double allowance = v.kind() == Volcano.Kind.GREAT ? 0.4 : 1.0;
            assertTrue(d - inner >= allowance * v.radius() - 1e-9, "foot in the zone 2→3: " + v);
            assertTrue(outer - d >= 1.05 * v.radius() - 1e-9, "foot in the zone 3→4: " + v);
            assertTrue(v.radius() >= v.crest() + 30 && v.radius() <= 0.8 * field.settings().cellSize(), "foot " + v);
            assertTrue(v.rim() - v.plain() >= 22, "rises above the plain " + v);
            switch (v.kind()) {
                case GREAT -> {
                    great++;
                    assertTrue(v.rim() >= 230 && v.rim() <= 260, "great rim " + v.rim());
                }
                case CONE -> assertTrue(v.rim() >= 140 && v.rim() <= 200, "cone rim " + v.rim());
                case CALDERA -> assertTrue(v.rim() - v.plain() >= 22 && v.rim() - v.plain() <= 44, "caldera rim " + v);
            }
            double wall = v.rim() - v.floor();
            assertTrue(wall >= 20 && wall <= 55, "crater floor 20–50 under the rim: " + wall + " " + v);
            if (v.dormant()) {
                assertTrue(Double.isNaN(v.lake()), "a dormant crater has no lava");
            } else {
                assertTrue(v.lake() <= v.rimLow() - 6.0, "lake below the lowest rim: " + v);
                assertTrue(v.lake() > v.floor(), "lake above the floor: " + v);
            }
        }
        assertTrue(great >= 30, "great volcanoes: " + great);
    }

    @Test
    void mostVolcanoesStandWhereTheRegionsGiveVolcanicPeaks() {
        VolcanicSettings s = field.settings();
        int peaks = 0;
        int ordinary = 0;
        for (Volcano v : volcanoes) {
            if (LAYOUT.effectiveDistance(v.x(), v.z()) < 3564 + 600) {
                continue; // the horizon band stands in every region
            }
            ordinary++;
            VolcanicField.Regions r = field.regions(v.x(), v.z());
            if (r.heat() >= s.hot() && r.ground() < 0.0) {
                peaks++;
            }
        }
        // volcanic_peaks covers about a seventh of the ring; most volcanoes away from the band (well over twice its share) stand there.
        assertTrue(peaks > 0.4 * ordinary, peaks + " of " + ordinary);
    }

    @Test
    void lavaLakesAreClosedBasins() {
        int checked = 0;
        for (Volcano v : volcanoes) {
            if (v.dormant() || checked == 12) {
                continue;
            }
            checked++;
            int reach = (int) Math.ceil(v.crest() + v.warp() + 4);
            int lake = (int) Math.floor(v.lake());
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int x = (int) Math.floor(v.x()) + dx;
                    int z = (int) Math.floor(v.z()) + dz;
                    VolcanicField.Column c = field.column(x, z);
                    if (c.lavaTop != lake || c.height >= lake) {
                        continue;
                    }
                    // Open lake surface: every neighbour holds the same lake or rises above it.
                    for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        VolcanicField.Column side = field.column(x + n[0], z + n[1]);
                        assertTrue(side.lavaTop == lake || side.height >= lake - 1.0,
                                "lake of " + v + " open at " + (x + n[0]) + "," + (z + n[1]) + ": height " + side.height);
                    }
                }
            }
        }
        assertTrue(checked > 5);
    }

    @Test
    void dormantCratersHaveNoLava() {
        for (Volcano v : volcanoes) {
            if (!v.dormant()) {
                continue;
            }
            VolcanicField.Column centre = field.column((int) Math.floor(v.x()), (int) Math.floor(v.z()));
            assertTrue(centre.dormant > 0.5, "dormant mask on the floor of " + v);
            assertFalse(centre.lavaAt((int) v.floor()), "lava in a dormant crater " + v);
        }
    }

    @Test
    void islandsFloatInTheirBandWellAboveTheTerrain() {
        List<List<Volcano.Island>> clusters = new ArrayList<>();
        for (Volcano v : volcanoes) {
            clusters.add(v.islands());
        }
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                clusters.add(field.channelCluster(i, j));
            }
        }
        int kept = 0;
        int islands = 0;
        for (List<Volcano.Island> cluster : clusters) {
            boolean any = false;
            for (Volcano.Island island : cluster) {
                assertTrue(island.top() >= 150 && island.top() <= 210, "island top " + island.top());
                assertTrue(island.a() >= 4 && island.a() * island.stretch() <= 15 * 1.5, "island size " + island);
                if (!field.islandClear(island)) {
                    continue; // dropped: a neighbouring volcano rises under it
                }
                any = true;
                islands++;
                VolcanicField.Column under = field.column((int) Math.floor(island.x()), (int) Math.floor(island.z()));
                assertTrue(island.bottomAt(0.0) > under.height + 10.0, "island over " + under.height + ": " + island);
            }
            if (any) {
                kept++;
            }
        }
        // About one cluster per three craters (V4 = A).
        long craters = volcanoes.size();
        assertTrue(kept >= craters / 5 && kept <= craters / 2, kept + " clusters for " + craters + " craters");
        assertTrue(islands >= kept);
    }

    @Test
    void lavaColumnsStayAboveTheirBedAndWaterlessSeabedStaysLow() {
        int lavaColumns = 0;
        for (int x = -5200; x <= -4400; x += 4) {
            for (int z = -2700; z <= -2000; z += 4) {
                VolcanicField.Column c = field.column(x, z);
                if (c.lavaTop != Integer.MIN_VALUE) {
                    lavaColumns++;
                    assertTrue(c.lavaBottom < c.lavaTop);
                    assertTrue(c.guardBottom <= c.lavaBottom && c.guardTop >= c.lavaTop, "guard covers the lava");
                }
            }
        }
        assertTrue(lavaColumns > 0, "no lava channel in the sample");
        // The seabed bowl lies at Y 30–45 deep inside dried_seabed.
        assertTrue(field.column(-3936, 864).height < 50.0, "seabed " + field.column(-3936, 864).height);
    }

    @Test
    void outsideRingThreeThereIsNoVolcanoOrLava() {
        assertNull(field.place(0, 0));
        VolcanicField.Column ringTwo = field.column(2500, 0);
        assertEquals(Integer.MIN_VALUE, ringTwo.lavaTop);
        assertEquals(0, ringTwo.islandCount);
        assertEquals(0.0, ringTwo.channel, 0.0);
    }
}
