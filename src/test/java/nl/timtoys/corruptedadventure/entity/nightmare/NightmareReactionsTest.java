package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the shared reactions (life round): stagger window, pace, search, aggro. */
class NightmareReactionsTest {
    @Test
    void damageWithinTheWindowStaggersThenTheCooldownHolds() {
        NightmareStagger stagger = new NightmareStagger();
        assertFalse(stagger.addDamage(0, 10.0F, 18.0F, 80));
        assertFalse(stagger.addDamage(80, 10.0F, 18.0F, 80), "the first hit fell out of the window");
        assertTrue(stagger.addDamage(100, 10.0F, 18.0F, 80));
        stagger.start(100, 30, 200);
        assertTrue(stagger.isStaggered());
        assertFalse(stagger.addDamage(110, 100.0F, 18.0F, 80), "no new stagger while staggered");
        for (int i = 0; i < 29; i++) {
            assertFalse(stagger.tick());
        }
        assertTrue(stagger.tick(), "ends after its ticks");
        assertFalse(stagger.isStaggered());
        assertFalse(stagger.isReady(329), "cooling down");
        assertFalse(stagger.addDamage(320, 100.0F, 18.0F, 80), "cooldown hits do not add up");
        assertTrue(stagger.isReady(330));
        assertFalse(stagger.addDamage(330, 10.0F, 18.0F, 80), "fresh damage needed after the cooldown");
        assertFalse(stagger.addDamage(331, 1.0F, 0.0F, 80), "threshold 0: never");
    }

    @Test
    void aBurstEndsInARestWindowAndResets() {
        NightmarePacing pacing = new NightmarePacing();
        RandomSource random = RandomSource.create(1);
        assertFalse(pacing.count(random, 3, 3, 40, 1.0));
        assertFalse(pacing.count(random, 3, 3, 40, 1.0));
        assertTrue(pacing.count(random, 3, 3, 40, 1.0), "the third attack ends the burst");
        assertTrue(pacing.isResting());
        assertEquals(NightmarePacing.Style.BACK_OFF, pacing.style(), "back-off chance 1");
        for (int i = 0; i < 39; i++) {
            assertFalse(pacing.tick());
        }
        assertTrue(pacing.tick());
        assertFalse(pacing.isResting());
        assertFalse(pacing.count(random, 2, 2, 40, 0.0));
        pacing.reset();
        assertFalse(pacing.count(random, 2, 2, 40, 0.0), "a reset starts a fresh burst");
        assertTrue(pacing.count(random, 2, 2, 40, 0.0));
        assertEquals(NightmarePacing.Style.CIRCLE, pacing.style(), "back-off chance 0");
        NightmarePacing noRest = new NightmarePacing();
        assertFalse(noRest.count(random, 1, 1, 0, 0.5), "rest 0: never resting");
        assertFalse(noRest.isResting());
    }

    @Test
    void outOfSightItSearchesTheLastPlaceThenSniffsTheTargetOut() {
        NightmareSearch search = new NightmareSearch();
        Vec3 seen = new Vec3(1, 64, 1);
        Vec3 hidden = new Vec3(9, 64, 9);
        assertNull(search.lastKnown());
        assertFalse(search.seen(seen));
        assertFalse(search.unseen(30, hidden, 60, 100));
        assertEquals(seen, search.lastKnown(), "it goes where it saw the target");
        assertTrue(search.unseen(30, hidden, 60, 100), "searches after 60 ticks unseen");
        assertTrue(search.isSearching());
        assertEquals(seen, search.lastKnown());
        search.arrive();
        assertTrue(search.hasArrived());
        assertFalse(search.unseen(99, hidden, 60, 100));
        assertTrue(search.isSearching());
        assertFalse(search.unseen(1, hidden, 60, 100));
        assertFalse(search.isSearching(), "the search runs out");
        assertEquals(hidden, search.lastKnown(), "and it sniffed the target out");
        assertTrue(search.unseen(60, hidden, 60, 100), "a new search after another while unseen");
        assertTrue(search.seen(seen), "sight ends a search");
        assertFalse(search.isSearching());
        search.reset();
        assertNull(search.lastKnown());
        assertFalse(search.unseen(5, hidden, 60, 100));
        assertEquals(hidden, search.lastKnown(), "found through walls: it knows the place at once");
    }

    @Test
    void rememberedDamageFadesWithItsHalfLife() {
        NightmareAggro aggro = new NightmareAggro();
        UUID a = new UUID(0, 1);
        aggro.add(a, 40.0F);
        aggro.add(a, 40.0F);
        assertEquals(80.0F, aggro.damageBy(a));
        float factor = NightmareAggro.decayFactor(20, 200);
        for (int i = 0; i < 10; i++) {
            aggro.decay(factor);
        }
        assertEquals(40.0F, aggro.damageBy(a), 0.01F, "half after one half-life");
        for (int i = 0; i < 200; i++) {
            aggro.decay(factor);
        }
        assertEquals(0.0F, aggro.damageBy(a), "forgotten");
        assertEquals(25.0, NightmareAggro.score(10.0F, true, 15.0));
        assertEquals(10.0, NightmareAggro.score(10.0F, false, 15.0));
        assertTrue(NightmareAggro.beats(16.0, 10.0, 1.5));
        assertFalse(NightmareAggro.beats(15.0, 10.0, 1.5), "the margin must be beaten");
        assertTrue(NightmareAggro.beats(0.1, 0.0, 1.5));
        assertFalse(NightmareAggro.beats(0.0, 0.0, 1.5), "nobody scoring: no switch");
    }
}
