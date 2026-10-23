package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ZoneFracture;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Read access of the zone 4→5's features (the split rock, 86c) to the fracture zone: the {@code zone_fracture} wired into
 * the level's random state as its final density (found once per random state, like {@link Ring4Terrain} and
 * {@link NetherRing5Terrain}). Its landmarks are decided once per world and shared by the terrain's own copies, so a
 * reader sees exactly the floes the terrain raised.
 */
public final class ZoneFractureTerrain {
    private static final Logger LOGGER = LogUtils.getLogger();
    // One random state per level and dimension; weak, so a closed world's state can go.
    private static final Map<RandomState, Optional<ZoneFracture>> FRACTURES = Collections.synchronizedMap(new WeakHashMap<>());

    private ZoneFractureTerrain() {
    }

    /**
     * The fracture zone of {@code level}'s generator, or null. Only the zone 4→5's biome asks, so a miss is a wiring
     * fault (86c: the router's holder hid it, and the rocks were silently gone): logged once per random state.
     */
    @Nullable
    public static ZoneFracture fracture(WorldGenLevel level) {
        RandomState random = level.getLevel().getChunkSource().randomState();
        return FRACTURES.computeIfAbsent(random, state -> {
            ZoneFracture found = ZoneFracture.find(state.router().finalDensity());
            if (found == null) {
                LOGGER.warn("No zone_fracture in the final density of {}: the zone 4→5 gets no split rocks", level.getLevel().dimension().location());
            }
            return Optional.ofNullable(found);
        }).orElse(null);
    }
}
