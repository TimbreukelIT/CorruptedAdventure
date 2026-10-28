package nl.timtoys.corruptedadventure.entity.nightmare.director;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the Nightmare director: target choice, strength, threat pace, roaming, saving a state. */
class NightmareDirectorLogicTest {
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(20260930L));
    private static final NightmareRings RINGS = NightmareRings.of(LAYOUT);

    @Test
    void theTargetIsTheHighestThreatAtOrAboveTheThreshold() {
        UUID a = new UUID(0, 1);
        UUID b = new UUID(0, 2);
        UUID c = new UUID(0, 3);
        assertEquals(Optional.of(b), NightmareTargets.choose(List.of(
                new NightmareTargets.Candidate(a, 120), new NightmareTargets.Candidate(b, 150),
                new NightmareTargets.Candidate(c, 99)), 100));
        assertEquals(Optional.of(a), NightmareTargets.choose(List.of(
                new NightmareTargets.Candidate(a, 100), new NightmareTargets.Candidate(b, 100)), 100), "a tie: the first");
        assertEquals(Optional.empty(), NightmareTargets.choose(List.of(new NightmareTargets.Candidate(c, 99.9F)), 100));
        assertEquals(Optional.empty(), NightmareTargets.choose(List.of(), 100));
    }

    @Test
    void strengthGrowsPerDefeatUpToTheMaximum() {
        assertEquals(1.0F, NightmareState.strength(0, 0.25, 3));
        assertEquals(1.25F, NightmareState.strength(1, 0.25, 3));
        assertEquals(1.75F, NightmareState.strength(3, 0.25, 3));
        assertEquals(1.75F, NightmareState.strength(7, 0.25, 3), "never more than three returns");
        assertEquals(1.0F, NightmareState.strength(2, 0.25, 0));
    }

    @Test
    void threatReachesTheThresholdInTheDrawnNumberOfDays() {
        RandomSource random = RandomSource.create(7);
        for (int i = 0; i < 50; i++) {
            NightmareThreat threat = new NightmareThreat();
            threat.rise(Ring.RING_3, 1, 100, 2.0, 3.0, random);
            float days = threat.days(Ring.RING_3);
            assertTrue(days >= 2.0F && days <= 3.0F, "days " + days);
            int seconds = Math.round(days * 1200) - 1;
            threat.rise(Ring.RING_3, seconds - 1, 100, 2.0, 3.0, random);
            assertTrue(threat.get(Ring.RING_3) < 100.0F, "just below: " + threat.get(Ring.RING_3));
            threat.rise(Ring.RING_3, 2, 100, 2.0, 3.0, random);
            assertTrue(threat.get(Ring.RING_3) >= 99.99F, "reached: " + threat.get(Ring.RING_3));
        }
        NightmareThreat threat = new NightmareThreat();
        threat.rise(Ring.VOID, 100_000, 100, 2.0, 3.0, random);
        assertEquals(0.0F, threat.get(Ring.VOID), "the void has no meter of its own");
    }

    @Test
    void theVoidEdgeCountsAsRingFive() {
        assertEquals(Ring.RING_5, NightmareThreats.countedRing(Ring.VOID));
        assertEquals(Ring.RING_2, NightmareThreats.countedRing(Ring.RING_2));
    }

    @Test
    void roamingStartsAndStaysInItsRing() {
        RandomSource random = RandomSource.create(42);
        for (Ring ring : List.of(Ring.RING_1, Ring.RING_2, Ring.RING_3, Ring.RING_4, Ring.RING_5)) {
            NightmareRoaming.Spot spot = NightmareRoaming.start(ring, RINGS, random);
            assertEquals(ring, RINGS.ringAt(spot.x(), spot.z()), "start in " + ring);
            for (int step = 0; step < 300; step++) {
                spot = NightmareRoaming.step(spot, 64.0, ring, RINGS, random);
                assertEquals(ring, RINGS.ringAt(spot.x(), spot.z()), "step " + step + " in " + ring);
            }
        }
    }

    @Test
    void roamingMovesTheStepLength() {
        RandomSource random = RandomSource.create(3);
        NightmareRoaming.Spot from = NightmareRoaming.start(Ring.RING_3, RINGS, random);
        NightmareRoaming.Spot to = NightmareRoaming.step(from, 32.0, Ring.RING_3, RINGS, random);
        assertEquals(32.0, Math.hypot(to.x() - from.x(), to.z() - from.z()), 1e-6);
    }

    @Test
    void outsideItsRingItWalksHome() {
        RandomSource random = RandomSource.create(5);
        // Deep in ring 3, as if ring 2's Nightmare followed a player there.
        NightmareRoaming.Spot spot = new NightmareRoaming.Spot(4700.0, 0.0, 90.0F);
        assertEquals(Ring.RING_3, RINGS.ringAt(spot.x(), spot.z()));
        int steps = 0;
        while (RINGS.ringAt(spot.x(), spot.z()) != Ring.RING_2 && steps < 100) {
            NightmareRoaming.Spot next = NightmareRoaming.step(spot, 64.0, Ring.RING_2, RINGS, random);
            assertTrue(Math.hypot(next.x(), next.z()) < Math.hypot(spot.x(), spot.z()), "inward");
            spot = next;
            steps++;
        }
        assertEquals(Ring.RING_2, RINGS.ringAt(spot.x(), spot.z()), "home after " + steps + " steps");
        assertEquals(new NightmareRoaming.Spot(0.0, 64.0, 0.0F),
                NightmareRoaming.step(new NightmareRoaming.Spot(0.0, 0.0, 0.0F), 64.0, Ring.RING_2, RINGS, random));
    }

    @Test
    void compassPointsFollowTheYaw() {
        assertEquals("S", NightmareRoaming.compass(0.0F));
        assertEquals("W", NightmareRoaming.compass(90.0F));
        assertEquals("N", NightmareRoaming.compass(180.0F));
        assertEquals("E", NightmareRoaming.compass(-90.0F));
    }

    @Test
    void aStateSurvivesSavingAndBrokenOnesRoam() {
        NightmareState state = new NightmareState();
        state.moveTo(1234.5, -987.25, 45.0F);
        UUID target = new UUID(5, 6);
        state.coming(target, new BlockPos(10, 64, -20), true);
        state.omenTick(17);
        NightmareState loaded = NightmareState.load(state.save());
        assertEquals(NightmareState.Phase.COMING, loaded.phase());
        assertEquals(target, loaded.target());
        assertEquals(new BlockPos(10, 64, -20), loaded.place());
        assertEquals(17, loaded.omenTick());
        assertTrue(loaded.forced());
        assertEquals(1234.5, loaded.x());
        assertEquals(-987.25, loaded.z());
        assertTrue(loaded.placed());

        state.present(new UUID(7, 8));
        state.defeat(500L);
        state.defeat(900L);
        loaded = NightmareState.load(state.save());
        assertEquals(NightmareState.Phase.DEFEATED, loaded.phase());
        assertEquals(900L, loaded.defeatedUntil());
        assertEquals(2, loaded.defeats());
        assertNull(loaded.entity());

        var broken = state.save();
        broken.putString("phase", "present");
        broken.remove("entity");
        assertEquals(NightmareState.Phase.ROAMING, NightmareState.load(broken).phase(), "present without entity");
        broken.putString("phase", "flying");
        assertEquals(NightmareState.Phase.ROAMING, NightmareState.load(broken).phase(), "unknown phase");
    }
}
