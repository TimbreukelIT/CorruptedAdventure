package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The relief, landforms, rivers and fantasy landmarks of ring 3 (change 39d) on the test seed. */
class Ring3LandformsTest {
    private static VolcanicField field;
    private static int size;

    @BeforeAll
    static void setUp() {
        field = VolcanicFieldTest.field();
        size = field.settings().cellSize();
    }

    private static boolean inCore(double x, double z) {
        double d = VolcanicFieldTest.LAYOUT.effectiveDistance(x, z);
        return d > 3564 + 260 && d < 5936 - 180;
    }

    @Test
    void thePlainIsRuggedBetweenY55And240() {
        int n = 0;
        int low = 0;
        int high = 0;
        double sum = 0.0;
        double sum2 = 0.0;
        for (int x = -6000; x <= 6000; x += 48) {
            for (int z = -6000; z <= 6000; z += 48) {
                if (!inCore(x, z)) {
                    continue;
                }
                VolcanicField.Regions r = field.regions(x, z);
                if (r.seabed() > 0.0) {
                    continue;
                }
                double plain = field.plain(x, z, r);
                assertTrue(plain > 40.0 && plain < 245.0, "plain " + plain + " at " + x + "," + z);
                n++;
                sum += plain;
                sum2 += plain * plain;
                if (plain < 70.0) {
                    low++;
                }
                if (plain > 150.0) {
                    high++;
                }
            }
        }
        double mean = sum / n;
        double std = Math.sqrt(sum2 / n - mean * mean);
        // "Ruig": Y ~70–150 between the volcanoes, highlands and lowlands; the peak range higher.
        assertTrue(mean > 95.0 && mean < 135.0, "mean " + mean);
        assertTrue(std > 18.0, "std " + std);
        assertTrue(low < 0.1 * n && high < 0.3 * n, low + " low, " + high + " high of " + n);
    }

    @Test
    void theReliefFadesOutTowardsTheZoneTwoThree() {
        // Just behind the zone 2→3 the landforms and highlands are 0, so the w² blend needs no cliff.
        double inner = VolcanicFieldTest.LAYOUT.settings().ringOuterRadii().get(1)
                + VolcanicFieldTest.LAYOUT.settings().transitionWidth() / 2.0;
        for (int k = 0; k < 64; k++) {
            double angle = k * Math.PI / 32.0;
            for (double radius = inner - 300.0; radius < inner + 300.0; radius += 1.0) {
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                if (VolcanicFieldTest.LAYOUT.effectiveDistance(x, z) >= inner + 2.0) {
                    VolcanicField.Regions r = field.regions(x, z);
                    assertTrue(r.fade() < 0.01, "fade " + r.fade());
                    assertTrue(r.peaksS() < 0.01 && r.ashenS() < 0.01 && r.flowsS() < 0.01 && r.riftOpen() < 0.01);
                    break;
                }
            }
        }
    }

    @Test
    void riversFlowDownhillFromActiveVolcanoes() {
        int rivers = 0;
        double length = 0.0;
        for (int i = -6200 / size - 1; i <= 6200 / size; i++) {
            for (int j = -6200 / size - 1; j <= 6200 / size; j++) {
                Volcano v = field.volcano(i, j);
                List<LavaRivers.River> list = field.riversOf(i, j);
                if (v == null || v.dormant()) {
                    assertTrue(list.isEmpty(), "river from a dormant or missing volcano");
                    continue;
                }
                assertTrue(list.size() <= 2);
                for (LavaRivers.River r : list) {
                    rivers++;
                    length += (r.points() - 1) * LavaRivers.STEP;
                    // Starts at the foot of its volcano.
                    double d0 = Math.hypot(r.x()[0] - v.x(), r.z()[0] - v.z());
                    assertEquals(0.8 * v.radius(), d0, 1e-6);
                    for (int k = 1; k < r.points(); k++) {
                        assertTrue(r.level()[k] <= r.level()[k - 1], "lava level rises downstream");
                        assertTrue(r.half()[k] >= LavaRivers.MIN_HALF && r.half()[k] <= LavaRivers.MAX_HALF * 1.8 + 1e-9);
                        assertTrue(field.riverMayFlow(r.x()[k], r.z()[k]), "river in the seabed, titan fields or a zone");
                    }
                }
            }
        }
        assertTrue(rivers > 150, "rivers " + rivers);
        assertTrue(length / rivers > 250.0, "mean length " + length / rivers);
    }

    @Test
    void riversAreTheSameWhateverAsksFirst() {
        VolcanicField other = VolcanicFieldTest.field();
        // Ask for a far tile first in the other field, then compare columns and rivers.
        double[] a = new double[3];
        double[] b = new double[3];
        other.nearestRiver(5200.0, -900.0, a);
        for (int i = -18; i <= 18; i += 3) {
            for (int j = -18; j <= 18; j += 3) {
                List<LavaRivers.River> x = field.riversOf(i, j);
                List<LavaRivers.River> y = other.riversOf(i, j);
                assertEquals(x.size(), y.size());
                for (int k = 0; k < x.size(); k++) {
                    assertArrayEquals(x.get(k).x(), y.get(k).x(), 0.0);
                    assertArrayEquals(x.get(k).level(), y.get(k).level(), 0.0);
                }
            }
        }
        for (int k = 0; k < 300; k++) {
            int x = -4800 + 13 * k;
            int z = 2600 - 29 * k;
            boolean ha = field.nearestRiver(x, z, a);
            boolean hb = other.nearestRiver(x, z, b);
            assertEquals(ha, hb);
            if (ha) {
                assertArrayEquals(a, b, 0.0);
            }
        }
    }

    @Test
    void riverLavaLiesInItsBedWithBanksAbove() {
        int lava = 0;
        for (int i = -6200 / size - 1; i <= 6200 / size && lava < 400; i++) {
            for (int j = -6200 / size - 1; j <= 6200 / size && lava < 400; j++) {
                for (LavaRivers.River r : field.riversOf(i, j)) {
                    int k = r.points() / 2;
                    int x = (int) Math.floor(r.x()[k]);
                    int z = (int) Math.floor(r.z()[k]);
                    VolcanicField.Column c = field.column(x, z);
                    if (!c.hasLava() || c.height >= c.lavaTop) {
                        continue;
                    }
                    lava++;
                    assertTrue(c.lavaBottom < c.height, "lava bed " + c.lavaBottom + " " + c.height + " top " + c.lavaTop + " pre " + c.preHeight
                            + " channel " + c.channel + " at " + x + "," + z + " level " + r.level()[k]);
                    assertTrue(c.guardBottom <= c.lavaBottom && c.guardTop >= c.lavaTop, "guard covers the lava");
                    // Shallow: the banks stand just above the lava, so it shows from afar.
                    assertTrue(c.lavaTop - c.height <= LavaRivers.DROP + 2.5, "deep bed " + (c.lavaTop - c.height));
                }
            }
        }
        assertTrue(lava > 50, "river lava columns " + lava);
    }

    @Test
    void landformsStayInTheirRanges() {
        Ring3Relief relief = field.relief();
        Ring3Relief.Detail d = new Ring3Relief.Detail();
        double maxDune = 0.0;
        double maxRift = 0.0;
        int rifts = 0;
        for (int x = -6000; x <= 6000; x += 37) {
            for (int z = -6000; z <= 6000; z += 41) {
                relief.dunes(x, z, d);
                assertTrue(d.height >= -13.0 - 1e-9 && d.height <= 25.0 + 1e-9, "dune " + d.height);
                maxDune = Math.max(maxDune, d.height);
                double terrace = relief.terraces(x, z);
                assertTrue(Math.abs(terrace) <= Ring3Relief.TERRACE_STEP * 3 + 1.2 + 1e-9, "terrace " + terrace);
                double level = relief.plateauLevel(x, z);
                assertTrue(level >= 0.0 && level <= Ring3Relief.PLATEAU_STEPS + 1e-9);
                double cut = relief.rift(x, z, 0.0, d);
                assertTrue(cut >= 0.0 && cut <= 40.0 + 1e-9, "rift " + cut);
                if (cut > 15.0) {
                    rifts++;
                }
                maxRift = Math.max(maxRift, cut);
            }
        }
        assertTrue(maxDune > 18.0, "dunes " + maxDune);
        assertTrue(maxRift > 25.0 && rifts > 0, "rifts " + maxRift);
    }

    @Test
    void cinderConesAndBonesHaveTheirSizes() {
        int cones = 0;
        for (int i = -60; i <= 60; i++) {
            for (int j = -60; j <= 60; j++) {
                CinderCones.Cone c = CinderCones.place(field, i, j);
                if (c == null) {
                    continue;
                }
                cones++;
                assertTrue(c.height() >= 10.0 && c.height() <= 40.0);
                assertTrue(field.clearOfVolcanoes(c.x(), c.z(), c.radius()), "cinder cone on a volcano");
                assertTrue(c.at(0.0) < c.height() && c.at(c.crater()) == c.height(), "mini crater");
            }
        }
        assertTrue(cones > 30, "cinder cones " + cones);
        int ribs = 0;
        int cages = 0;
        for (int i = -60; i <= 60; i++) {
            for (int j = -60; j <= 60; j++) {
                TitanBones.Rib rib = TitanBones.rib(field, i, j);
                if (rib != null) {
                    ribs++;
                    assertTrue(rib.height() >= 20.0 && rib.height() <= 60.0);
                    double[] out = new double[2];
                    // An arch: solid high up over its middle with open space below (an overhang).
                    assertTrue(rib.interval(rib.x(), rib.z(), out) && out[0] > 10.0, "rib crown " + out[0]);
                }
            }
        }
        for (int i = -16; i <= 16; i++) {
            for (int j = -16; j <= 16; j++) {
                if (TitanBones.cage(field, i, j) != null) {
                    cages++;
                }
            }
        }
        assertTrue(ribs > 10, "ribs " + ribs);
        assertTrue(cages >= 1 && cages <= 6, "ribcages " + cages);
    }

    @Test
    void landmarksAreRareAndFreeOfTheGround() {
        List<Landmarks.FloatingVolcano> floating = new ArrayList<>();
        for (int i = -4; i <= 3; i++) {
            for (int j = -4; j <= 3; j++) {
                Landmarks.FloatingVolcano f = field.floatingVolcano(i, j);
                if (f != null) {
                    floating.add(f);
                    assertTrue(f.lowestAt(0.0) > field.traceGround(f.x(), f.z()) + 15.0, "floating volcano too low");
                    assertTrue(f.lakeTop() < f.topAt(f.crater() / f.radius()), "crater lake below the rim");
                    // 39f: the plateau below the clouds (Y 192–196), only the cone pierces them.
                    assertTrue(f.base() + 3.0 < 192.0 && f.base() + f.cone() > 200.0, "floating volcano plateau " + f.base());
                    assertTrue(f.falls().size() >= 3, "lava falls " + f.falls());
                    // A spring sits on the plateau's edge; its lava falls in the column beside it down to the ground.
                    Volcano.LavaFall fall = f.falls().get(0);
                    VolcanicField.Column spring = field.column(fall.springX(), fall.springZ());
                    assertTrue(spring.springAt(fall.springY()) && spring.islandAt(fall.springY() - 1), "spring on the edge");
                    VolcanicField.Column falling = field.column(fall.fallX(), fall.fallZ());
                    assertTrue(falling.fallTop() == fall.springY() && falling.fallBottom() <= falling.height() + 2.0,
                            "fall to the ground: " + falling.fallBottom() + " over " + falling.height());
                }
            }
        }
        assertTrue(!floating.isEmpty() && floating.size() <= 6, "floating volcanoes " + floating.size());
        int abysses = 0;
        for (int i = -5; i <= 4; i++) {
            for (int j = -5; j <= 4; j++) {
                Landmarks.Abyss a = field.abyssAt(i, j);
                if (a == null) {
                    continue;
                }
                abysses++;
                VolcanicField.Column c = field.column((int) Math.floor(a.x()), (int) Math.floor(a.z()));
                assertTrue(c.height < Landmarks.ABYSS_LAVA, "abyss floor " + c.height);
                assertTrue(c.lavaAt(Landmarks.ABYSS_LAVA - 1) && c.guardTop >= Landmarks.ABYSS_LAVA, "lava sea");
                assertTrue(a.bridges().length >= 1);
            }
        }
        assertTrue(abysses >= 2 && abysses <= 12, "abysses " + abysses);
        int archipelagos = 0;
        for (int i = -6200 / size - 1; i <= 6200 / size; i++) {
            for (int j = -6200 / size - 1; j <= 6200 / size; j++) {
                Volcano v = field.volcano(i, j);
                if (v != null && !v.pillars().isEmpty()) {
                    archipelagos++;
                    for (Volcano.Pillar p : v.pillars()) {
                        assertTrue(p.top() > field.traceGround(p.x(), p.z()) + 20.0, "pillar under an archipelago");
                    }
                }
            }
        }
        assertTrue(archipelagos >= 2 && archipelagos <= 15, "archipelagos " + archipelagos);
    }
}
