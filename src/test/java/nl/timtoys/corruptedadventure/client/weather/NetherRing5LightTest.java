package nl.timtoys.corruptedadventure.client.weather;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nether ring 5's lightmap (walk 1, question 1 = A): neutral and a little lighter, only where the player is in it. */
class NetherRing5LightTest {
    @Test
    void outsideNetherRing5TheLightmapStaysTheNethers() {
        Vector3f warm = new Vector3f(0.6F, 0.45F, 0.3F);
        NetherRing5Light.adjust(warm, 0.0F);
        assertEquals(new Vector3f(0.6F, 0.45F, 0.3F), warm);
    }

    @Test
    void insideTheLightTurnsNeutralAndLighter() {
        Vector3f warm = new Vector3f(0.6F, 0.45F, 0.3F);
        NetherRing5Light.adjust(warm, 1.0F);
        assertEquals(warm.x, warm.y, 1e-6F);
        assertEquals(warm.y, warm.z, 1e-6F);
        assertTrue(warm.x > 0.6F && warm.x <= 1.0F, "lifted " + warm.x);
        Vector3f dark = new Vector3f(0.0F, 0.0F, 0.0F);
        NetherRing5Light.adjust(dark, 1.0F);
        assertEquals(NetherRing5Light.LIFT, dark.x, 1e-6F);
        Vector3f full = new Vector3f(1.0F, 1.0F, 0.9F);
        NetherRing5Light.adjust(full, 1.0F);
        assertEquals(1.0F, full.z, 1e-6F);
    }
}
