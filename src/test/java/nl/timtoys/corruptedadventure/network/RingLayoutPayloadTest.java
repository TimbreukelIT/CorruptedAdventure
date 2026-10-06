package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingLayoutPayloadTest {
    private static final List<Integer> RADII = CommonConfig.DEFAULT_OUTER_RADII;
    private static final RingSettings SETTINGS = new RingSettings(RADII, 10000, 96, 128);
    private static final RingLayout SERVER = new RingLayout(SETTINGS, RingLayout.noiseSeed(20260930L));

    @Test
    void roundTripKeepsEveryField() {
        RingLayoutPayload sent = RingLayoutPayload.of(SERVER);
        RingLayoutPayload received = roundTrip(sent);
        assertEquals(sent, received);
        assertEquals(SETTINGS, received.settings());
        assertEquals(SERVER.noiseSeed(), received.noiseSeed());
    }

    @Test
    void negativeAndExtremeSeedsSurvive() {
        for (long seed : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L}) {
            assertEquals(seed, roundTrip(new RingLayoutPayload(SETTINGS, seed)).noiseSeed());
        }
    }

    @Test
    void layoutFromTheDecodedPayloadSamplesExactlyLikeTheServer() {
        RingLayoutPayload received = roundTrip(RingLayoutPayload.of(SERVER));
        RingLayout client = new RingLayout(received.settings(), received.noiseSeed());
        int transitions = 0;
        // Every 5°, across each boundary zone (± noise and half the zone, so every zone is crossed) and out to the border.
        for (int angle = 0; angle < 360; angle += 5) {
            double radians = Math.toRadians(angle);
            for (int boundary = 0; boundary <= RADII.size(); boundary++) {
                int radius = boundary < RADII.size() ? RADII.get(boundary) : SETTINGS.voidOuterRadius();
                for (int distance = radius - 170; distance <= radius + 170; distance += 2) {
                    double x = Math.cos(radians) * distance;
                    double z = Math.sin(radians) * distance;
                    RingPosition expected = SERVER.sample(Level.OVERWORLD, x, z);
                    assertEquals(expected, client.sample(Level.OVERWORLD, x, z));
                    // Same overworld point seen from the Nether.
                    assertEquals(expected, client.sample(Level.NETHER, x / RingLayout.NETHER_SCALE, z / RingLayout.NETHER_SCALE));
                    assertEquals(SERVER.sample(Level.NETHER, x, z), client.sample(Level.NETHER, x, z));
                    if (expected.inTransition()) {
                        transitions++;
                    }
                }
            }
        }
        assertTrue(transitions > 1000, "grid should cross every transition zone, crossed " + transitions);
    }

    private static RingLayoutPayload roundTrip(RingLayoutPayload payload) {
        ByteBuf buffer = Unpooled.buffer();
        RingLayoutPayload.STREAM_CODEC.encode(buffer, payload);
        RingLayoutPayload decoded = RingLayoutPayload.STREAM_CODEC.decode(buffer);
        assertEquals(0, buffer.readableBytes(), "buffer must be fully read");
        buffer.release();
        return decoded;
    }
}
