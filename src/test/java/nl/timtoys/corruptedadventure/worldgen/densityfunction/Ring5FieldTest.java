package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ring 5's terrain field (82): the four castle sites (V5-A), the skeleton terrain, found through the router. */
public class Ring5FieldTest {
    public static final long SEED = IslandFieldTest.SEED;
    public static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    /** Zone 4→5 (256 wide) and zone 5→void (128 wide), in effective distance. */
    private static final double ZONE_45_END = 8128.0;
    private static final double ZONE_5V_START = 9636.0;

    /** The field as {@code ring_5/overworld/field.json} configures it, on the test seed and layout. */
    public static Ring5Field field() {
        return field(SEED);
    }

    static Ring5Field field(long seed) {
        RingLayout layout = seed == SEED ? LAYOUT : new RingLayout(LAYOUT.settings(), RingLayout.noiseSeed(seed));
        JsonObject json = WorldgenData.read("density_function/ring_5/overworld/field.json");
        Ring5Field.Settings settings = Ring5Field.Settings.CODEC.parse(JsonOps.INSTANCE, json.get("settings")).getOrThrow();
        return new Ring5Field(IslandFieldTest.noise(key(json, "hash"), seed), IslandFieldTest.noise(key(json, "placeholder"), seed),
                settings, () -> layout);
    }

    private static String key(JsonObject json, String field) {
        return ResourceLocation.parse(json.get(field).getAsString()).getPath();
    }

    private static double angleBetween(double a, double b) {
        return ((b - a) % 360.0 + 360.0) % 360.0;
    }

    /**
     * On many seeds: exactly four sites a quarter turn apart (± twice the jitter), index 0 closest to east, sizes in
     * their ranges, each centre at 75 % of ring 5 in the outer band, the whole pinnacle with its belt inside ring 5 (not
     * in the zone 4→5 or 5→void).
     */
    @Test
    void fourSitesAQuarterApartInTheOuterBandInsideRing5() {
        for (long seed = SEED; seed < SEED + 40; seed++) {
            Ring5Field field = field(seed);
            RingLayout layout = field.ringLayout();
            List<Ring5Field.CastleSite> sites = field.castleSites();
            assertEquals(4, sites.size());
            double east = Double.MAX_VALUE;
            for (Ring5Field.CastleSite site : sites) {
                east = Math.min(east, Math.min(site.angle(), 360.0 - site.angle()));
            }
            Ring5Field.CastleSite first = sites.getFirst();
            assertEquals(east, Math.min(first.angle(), 360.0 - first.angle()), 1e-9, "index 0 closest to east, seed " + seed);
            for (int i = 0; i < 4; i++) {
                Ring5Field.CastleSite site = sites.get(i);
                assertEquals(i, site.index());
                double step = angleBetween(site.angle(), sites.get((i + 1) % 4).angle());
                assertTrue(Math.abs(step - 90.0) <= 2 * Ring5Field.CASTLE_JITTER + 1e-9, "step " + step + ", seed " + seed);
                assertTrue(site.radius() >= 40 && site.radius() <= 56, "radius " + site.radius());
                assertTrue(site.top() >= 120 && site.top() <= 140, "top " + site.top());
                assertTrue(site.belt() >= 40 && site.belt() <= 80, "belt " + site.belt());
                assertEquals(9275.0, layout.effectiveDistance(site.x(), site.z()), 1.0, "75 % of ring 5, seed " + seed);
                assertTrue(Ring5Band.of(layout).outer(site.x(), site.z()), "outer band, seed " + seed);
                double reach = site.radius() + site.belt();
                for (int k = 0; k < 64; k++) {
                    double a = 2.0 * Math.PI * k / 64.0;
                    double effective = layout.effectiveDistance(site.x() + reach * Math.cos(a), site.z() + reach * Math.sin(a));
                    assertTrue(effective > ZONE_45_END && effective < ZONE_5V_START, "belt edge at " + effective + ", seed " + seed);
                }
            }
        }
    }

    @Test
    void sameSeedSameSitesOtherSeedOtherTurn() {
        assertEquals(field().castleSites(), field(SEED).castleSites());
        assertNotEquals(field().castleSites().getFirst().angle(), field(SEED + 1).castleSites().getFirst().angle());
        Ring5Field field = field();
        assertSame(field.castleSites(), field.castleSites(), "decided once");
    }

    @Test
    void castleAtCoversThePlateauOnly() {
        Ring5Field field = field();
        for (Ring5Field.CastleSite site : field.castleSites()) {
            assertEquals(site, field.castleAt(site.x(), site.z()));
            assertEquals(site, field.castleAt(site.x() + site.radius() - 1, site.z()));
            assertNull(field.castleAt(site.x() + site.radius() + 2, site.z()));
        }
        // The inner band has no castle.
        for (int angle = 0; angle < 360; angle += 5) {
            assertNull(field.castleAt((int) (8300 * Math.cos(Math.toRadians(angle))), (int) (8300 * Math.sin(Math.toRadians(angle)))));
        }
    }

    /**
     * The skeleton terrain: a pinnacle from its top down to Y −40 with void below, a belt of void down past the bottom
     * (breakup 1, initial height −64), elsewhere the placeholder surface with the band step.
     */
    @Test
    void skeletonTerrainHasPinnaclesInVoidBelts() {
        Ring5Field field = field();
        for (Ring5Field.CastleSite site : field.castleSites()) {
            int x = site.x();
            int z = site.z();
            assertTrue(field.terrain(x, site.top() - 1, z) > 0.0);
            assertTrue(field.terrain(x, site.top(), z) <= 0.0);
            assertTrue(field.terrain(x, Ring5Field.PINNACLE_BOTTOM + 1, z) > 0.0);
            assertTrue(field.terrain(x, Ring5Field.PINNACLE_BOTTOM - 1, z) < 0.0);
            assertTrue(field.terrain(x, -64, z) < 0.0);
            assertEquals(site.top(), field.initialHeight(x, z));
            assertEquals(0.0, field.breakupAt(x, z));
            assertEquals(1.0, field.column(x, z).castle());
            int beltX = (int) Math.round(x + (site.radius() + site.belt() / 2.0) * Math.cos(Math.toRadians(site.angle())));
            int beltZ = (int) Math.round(z + (site.radius() + site.belt() / 2.0) * Math.sin(Math.toRadians(site.angle())));
            for (int y = -64; y < 320; y += 7) {
                assertEquals(-1.0, field.terrain(beltX, y, beltZ), "belt at y " + y);
            }
            assertEquals(1.0, field.breakupAt(beltX, beltZ));
            assertTrue(!field.solidGround(beltX, beltZ));
            assertEquals(-64.0, field.initialHeight(beltX, beltZ));
            double castle = field.column(beltX, beltZ).castle();
            assertTrue(castle > 0.0 && castle < 1.0, "castle surroundings " + castle);
        }
    }

    /** A noise chunk's copy keeps the decisions of the random state's instance; the field is found through a router. */
    @Test
    void foundThroughTheRouterAndCopiesKeepTheDecisions() {
        Ring5Field field = field();
        DensityFunction router = DensityFunctions.add(DensityFunctions.interpolated(new Ring5Output(field, Ring5Output.Output.TERRAIN)),
                DensityFunctions.zero());
        // find maps the router: it gives the mapped copy, equal and with the same decisions.
        Ring5Field found = Ring5Field.find(new ZoneFracture(router));
        assertEquals(field, found);
        assertSame(field.decisions(), found.decisions());
        DensityFunction copy = field.mapAll(function -> function);
        assertSame(field.decisions(), ((Ring5Field) copy).decisions());
        assertEquals(field, copy);
        assertNull(Ring5Field.find(DensityFunctions.zero()));
    }
}
