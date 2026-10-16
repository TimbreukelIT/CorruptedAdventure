package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The client ambience of ring 4 (change 80): the grey fade of zone 4→5 and the rarity of the silhouette. */
class Ring4AmbienceTest {
    /** Q17 = A: nothing at the zone's inner edge, rising linearly to the most at its outer edge; nowhere else. */
    @Test
    void zoneFourFiveFadesLinearlyAcrossTheZoneOnly() {
        assertEquals(0.0F, SunderedFade.amount(new RingPosition(Ring.RING_4, Ring.RING_5, 0.0)));
        assertEquals(SunderedFade.MAX_GREY / 2, SunderedFade.amount(new RingPosition(Ring.RING_4, Ring.RING_5, 0.5)), 1e-6);
        assertEquals(SunderedFade.MAX_GREY, SunderedFade.amount(new RingPosition(Ring.RING_4, Ring.RING_5, 1.0)), 1e-6);
        assertEquals(0.0F, SunderedFade.amount(RingPosition.within(Ring.RING_4)));
        assertEquals(0.0F, SunderedFade.amount(RingPosition.within(Ring.RING_5)));
        assertEquals(0.0F, SunderedFade.amount(new RingPosition(Ring.RING_3, Ring.RING_4, 0.7)));
        assertTrue(SunderedFade.MAX_GREY > 0.5F && SunderedFade.MAX_GREY < 1.0F);
    }

    @Test
    void greyKeepsTheLumaAndRemovesTheColour() {
        Vec3 colour = new Vec3(0.2, 0.8, 0.4);
        double luma = 0.299 * 0.2 + 0.587 * 0.8 + 0.114 * 0.4;
        Vec3 grey = SunderedFade.grey(colour, 1.0F);
        assertEquals(luma, grey.x, 1e-6);
        assertEquals(luma, grey.y, 1e-6);
        assertEquals(luma, grey.z, 1e-6);
        assertEquals(colour, SunderedFade.grey(colour, 0.0F));
    }

    /** The water tint's shortcut: only near the 4|5 radius (half the zone plus the boundary noise) is the layout sampled. */
    @Test
    void onlyPositionsNearTheZoneAreSampled() {
        RingSettings settings = new RingSettings(List.of(1500, 3500, 6000, 8000, 9700), 10200, 96, 128);
        assertTrue(SunderedFade.nearZone(settings, 8000, 0));
        assertTrue(SunderedFade.nearZone(settings, 0, -8000 - 64 - 96));
        assertFalse(SunderedFade.nearZone(settings, 8000 + 64 + 96 + 2, 0));
        assertFalse(SunderedFade.nearZone(settings, 7000, 0));
        assertFalse(SunderedFade.nearZone(settings, 100, 100));
    }

    /** Q18 = A: at most one silhouette per ~20 minutes per player, in water at least 20 deep. */
    @Test
    void theSilhouetteIsRare() {
        assertTrue(Ring4Ambience.SILHOUETTE_COOLDOWN >= 20 * 60 * 20);
        assertTrue(Ring4Ambience.SILHOUETTE_MIN_DEPTH >= 20);
        assertTrue(Ring4Ambience.SILHOUETTE_CHANCE < 0.001F);
    }
}
