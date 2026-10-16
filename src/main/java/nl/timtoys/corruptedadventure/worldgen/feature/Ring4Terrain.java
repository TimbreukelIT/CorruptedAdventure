package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Read access of the ring 4 features to the pure ring 4 terrain ({@code island_field}), as {@link Ring3Terrain} is for
 * ring 3: the instance wired into the level's random state, whose queries are pure functions of the seed and x, z
 * (never a {@code flat_cache}, never world blocks), so any thread may ask and the answer never depends on which chunks
 * exist or decorated first. Owned by W-biomes; the terrain classes stay W-terrain's (read only).
 * <p>
 * A {@link IslandField.Column} is the thread's cache slot: read what is needed before asking for another column.
 */
public final class Ring4Terrain {
    // One random state per level and dimension; weak, so a closed world's state can go.
    private static final Map<RandomState, Optional<IslandField>> FIELDS = Collections.synchronizedMap(new WeakHashMap<>());

    private Ring4Terrain() {
    }

    /** The ring 4 terrain of {@code level}'s generator, or null (no ring 4 terrain in this dimension). */
    @Nullable
    public static IslandField field(WorldGenLevel level) {
        RandomState random = level.getLevel().getChunkSource().randomState();
        return FIELDS.computeIfAbsent(random, state -> Optional.ofNullable(IslandField.find(state.router().finalDensity()))).orElse(null);
    }

    /** Whether (x, z) lies in the blighted band of ring 4 ({@link Ring4Band#blighted}: the line of weather and poison). */
    public static boolean blighted(int x, int z) {
        return Ring4Band.of(RingLayout.current()).blighted(x, z);
    }
}
