package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Read access of the ring 3 features to the pure ring 3 terrain ({@code volcanic_field}): the instance wired into the
 * level's random state, whose queries are pure functions of the seed and x, z with per-thread caches, so any thread
 * may ask and the answer never depends on which chunks exist or decorated first (never a {@code flat_cache}, never
 * world blocks). Owned by W-biomes; the terrain classes stay W-terrain's (read only).
 * <p>
 * A {@link VolcanicField.Column} is the thread's cache slot: read what is needed before asking for another column.
 */
public final class Ring3Terrain {
    // One random state per level and dimension; weak, so a closed world's state can go.
    private static final Map<RandomState, Optional<VolcanicField>> FIELDS = Collections.synchronizedMap(new WeakHashMap<>());

    private Ring3Terrain() {
    }

    /** The ring 3 terrain of {@code level}'s generator, or null (no ring 3 terrain in this dimension). */
    @Nullable
    public static VolcanicField field(WorldGenLevel level) {
        RandomState random = level.getLevel().getChunkSource().randomState();
        return FIELDS.computeIfAbsent(random, state -> Optional.ofNullable(VolcanicField.find(state.router().finalDensity()))).orElse(null);
    }
}
