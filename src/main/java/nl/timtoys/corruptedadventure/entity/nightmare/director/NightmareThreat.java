package nl.timtoys.corruptedadventure.entity.nightmare.director;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.NightmareDirectorConfig;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Player attachment: the invisible threat per ring (fase-4-nightmares-concept.md "Stap 2+3"). It rises while the player
 * is in that ring; at the threshold the ring's Nightmare comes for the one with the most. Each time a ring's threat
 * starts from 0 it draws its pace: the in-game days it takes to reach the threshold (between the config's min and max),
 * so one player meets it after two days and another after three. Server only, never synced.
 */
public final class NightmareThreat {
    static final int RINGS = 5;
    /** The threat never rises above this many thresholds, so "most threat" stays meaningful but bounded. */
    static final float CAP_THRESHOLDS = 2.0F;

    private record Entry(float threat, float days) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("threat").forGetter(Entry::threat),
                Codec.FLOAT.optionalFieldOf("days", 0.0F).forGetter(Entry::days)
        ).apply(instance, Entry::new));
    }

    /** Ring name → threat and pace. Lenient: an unknown ring name is skipped, not the whole attachment. */
    public static final Codec<NightmareThreat> CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC)
            .xmap(NightmareThreat::fromMap, NightmareThreat::toMap);

    private final float[] threat = new float[RINGS];
    /** Days to the threshold at the current pace; 0 = not drawn yet (drawn on the first rise). */
    private final float[] days = new float[RINGS];

    /** The threat in a ring; 0 for the void. */
    public float get(Ring ring) {
        return hasMeter(ring) ? threat[ring.ordinal()] : 0.0F;
    }

    /** The days to the threshold drawn for this ring's current climb; 0 when not drawn yet. */
    public float days(Ring ring) {
        return hasMeter(ring) ? days[ring.ordinal()] : 0.0F;
    }

    /** Sets the threat (at least 0); the pace stays. */
    public void set(Ring ring, float value) {
        if (hasMeter(ring)) {
            threat[ring.ordinal()] = Float.isNaN(value) ? 0.0F : Math.max(0.0F, value);
        }
    }

    /** Back to 0 after a visit; the next climb draws a new pace. */
    public void reset(Ring ring) {
        if (hasMeter(ring)) {
            threat[ring.ordinal()] = 0.0F;
            days[ring.ordinal()] = 0.0F;
        }
    }

    /**
     * {@code seconds} in {@code ring}: up by {@code threshold / (days × a day)} per second, at most
     * {@link #CAP_THRESHOLDS} thresholds. The pace is drawn between {@code minDays} and {@code maxDays} the first time.
     */
    public void rise(Ring ring, int seconds, float threshold, double minDays, double maxDays, RandomSource random) {
        if (!hasMeter(ring)) {
            return;
        }
        int i = ring.ordinal();
        if (days[i] <= 0.0F) {
            days[i] = (float) (minDays + random.nextDouble() * Math.max(0.0, maxDays - minDays));
        }
        float perSecond = threshold / (Math.max(0.01F, days[i]) * NightmareDirectorConfig.DAY_SECONDS);
        threat[i] = Math.min(threshold * CAP_THRESHOLDS, threat[i] + perSecond * seconds);
    }

    private static boolean hasMeter(Ring ring) {
        return ring.ordinal() < RINGS;
    }

    private static NightmareThreat fromMap(Map<String, Entry> map) {
        NightmareThreat result = new NightmareThreat();
        map.forEach((name, entry) -> {
            Ring ring = byName(name);
            if (ring == null) {
                CorruptedAdventure.LOGGER.warn("Dropping the Nightmare threat of unknown ring '{}'", name);
            } else {
                result.set(ring, entry.threat());
                // A NaN pace would never be redrawn and makes the threat NaN for good: treat it as unset (0).
                float days = entry.days();
                result.days[ring.ordinal()] = Float.isNaN(days) ? 0.0F : Math.max(0.0F, days);
            }
        });
        return result;
    }

    private static Ring byName(String name) {
        for (int i = 0; i < RINGS; i++) {
            if (Ring.byIndex(i).getSerializedName().equals(name)) {
                return Ring.byIndex(i);
            }
        }
        return null;
    }

    private static Map<String, Entry> toMap(NightmareThreat value) {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (int i = 0; i < RINGS; i++) {
            map.put(Ring.byIndex(i).getSerializedName(), new Entry(value.threat[i], value.days[i]));
        }
        return map;
    }
}
