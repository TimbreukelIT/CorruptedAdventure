package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.GiantGeodes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Read access of the Nether ring 4 features and surface (78) to the pure Nether ring 4 terrain: the
 * {@code giant_geodes} and {@code sea_fossils} wired into the level's random state (found once per random state, like
 * {@link Ring3Terrain}). Their cells are decided once per world in the map the terrain's own copies share, so a feature
 * sees exactly the geode or fossil the terrain made, whatever thread asks and whatever generated first.
 */
public final class NetherRing4Terrain {
    // One random state per level and dimension; weak, so a closed world's state can go.
    private static final Map<RandomState, Optional<GiantGeodes>> GEODES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<RandomState, Optional<SeaFossils>> FOSSILS = Collections.synchronizedMap(new WeakHashMap<>());

    private NetherRing4Terrain() {
    }

    /** The giant geodes of {@code random}'s terrain, or null (no Nether ring 4 in this dimension). */
    @Nullable
    public static GiantGeodes geodes(RandomState random) {
        return GEODES.computeIfAbsent(random, state -> Optional.ofNullable(GiantGeodes.find(state.router().finalDensity()))).orElse(null);
    }

    @Nullable
    public static GiantGeodes geodes(WorldGenLevel level) {
        return geodes(level.getLevel().getChunkSource().randomState());
    }

    /** The sea fossils of {@code random}'s terrain, or null (no Nether ring 4 in this dimension). */
    @Nullable
    public static SeaFossils fossils(RandomState random) {
        return FOSSILS.computeIfAbsent(random, state -> Optional.ofNullable(SeaFossils.find(state.router().finalDensity()))).orElse(null);
    }

    @Nullable
    public static SeaFossils fossils(WorldGenLevel level) {
        return fossils(level.getLevel().getChunkSource().randomState());
    }
}
