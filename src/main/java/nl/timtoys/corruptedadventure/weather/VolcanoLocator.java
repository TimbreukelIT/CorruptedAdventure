package nl.timtoys.corruptedadventure.weather;

import net.minecraft.server.level.ServerLevel;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload.Plume;
import nl.timtoys.corruptedadventure.worldgen.chunk.RingChunkGenerator;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import java.util.List;

/**
 * Where the active volcanoes are, for their smoke plumes ({@link VolcanoPlumes}): read-only, server side, from the pure
 * worldgen decisions of the ring 3 terrain ({@link VolcanicField#volcanoesWithin}, change 39f; no chunk loading). The
 * field comes from the level's own generator, which already remembers it; the decisions themselves are cached per
 * volcano cell inside the field, so a repeated lookup costs a few map reads.
 */
public final class VolcanoLocator {
    private VolcanoLocator() {
    }

    /** Every active volcano whose crater centre lies within {@code radius} blocks of (x, z), any order. */
    static List<Plume> plumesNear(ServerLevel level, int x, int z, int radius) {
        if (!(level.getChunkSource().getGenerator() instanceof RingChunkGenerator generator)) {
            return List.of();
        }
        VolcanicField field = generator.volcanicField(level.getChunkSource().randomState());
        return field == null ? List.of() : plumes(field, x, z, radius);
    }

    /** The plumes of {@code field} within the radius: dormant volcanoes have none. */
    static List<Plume> plumes(VolcanicField field, int x, int z, int radius) {
        return field.volcanoesWithin(x, z, radius).stream()
                .filter(volcano -> !volcano.dormant())
                .map(volcano -> new Plume(volcano.x(), volcano.z(), volcano.craterY(), volcano.craterRadius(), volcano.great()))
                .toList();
    }
}
