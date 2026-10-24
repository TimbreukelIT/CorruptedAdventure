package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Read access of the ring 5 features and surface conditions (94/95) to the ring 5 terrain: the {@code ring_5_field} of
 * the level's noise router (found once per random state, like {@link Ring4Terrain} and {@link ZoneFractureTerrain}).
 * Its decisions are made once per world and shared by the terrain's own copies, so a reader sees exactly the ground the
 * terrain built.
 */
public final class Ring5Terrain {
    private static final Logger LOGGER = LogUtils.getLogger();
    // One random state per level and dimension; weak, so a closed world's state can go.
    private static final Map<RandomState, Optional<Ring5Field>> FIELDS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<RandomState, Optional<Ring5World>> WORLDS = Collections.synchronizedMap(new WeakHashMap<>());

    private Ring5Terrain() {
    }

    /** The ring 5 ground of {@code level}'s generator (field, layout, salt), or null. */
    @Nullable
    public static Ring5World world(WorldGenLevel level) {
        return world(level.getLevel().getChunkSource().randomState());
    }

    /** The ring 5 ground of the random state (the running server's layout), or null without a ring 5 field. */
    @Nullable
    public static Ring5World world(RandomState random) {
        return WORLDS.computeIfAbsent(random, state -> {
            Ring5Field field = field(state);
            return field == null ? Optional.empty() : Optional.of(Ring5World.of(field, RingLayout.current(), Ring5World.salt(state)));
        }).orElse(null);
    }

    /** The ring 5 field of {@code level}'s generator, or null. */
    @Nullable
    public static Ring5Field field(WorldGenLevel level) {
        return field(level.getLevel().getChunkSource().randomState());
    }

    /**
     * The ring 5 field of the random state, or null. Only the ring 5 biomes ask, so a miss is a wiring fault (lesson 86c):
     * logged once per random state.
     */
    @Nullable
    public static Ring5Field field(RandomState random) {
        return FIELDS.computeIfAbsent(random, state -> {
            Ring5Field found = Ring5Field.find(state.router().finalDensity());
            if (found == null) {
                LOGGER.warn("No ring_5_field in the final density of a random state: ring 5 gets no dressing there");
            }
            return Optional.ofNullable(found);
        }).orElse(null);
    }
}
