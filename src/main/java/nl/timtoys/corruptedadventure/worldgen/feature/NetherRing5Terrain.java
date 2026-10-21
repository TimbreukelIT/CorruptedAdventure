package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.NetherCathedrals;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Read access of the Nether ring 5 surface and features (87, 96) to the pure Nether ring 5 terrain: the
 * {@code nether_cathedrals} terrain wired into the level's random state (found once per random state, like
 * {@link NetherRing4Terrain}). Its cells are decided once per world in the map the terrain's own copies share, so a
 * reader sees exactly the halls, columns and cathedrals the terrain made.
 */
public final class NetherRing5Terrain {
    // One random state per level and dimension; weak, so a closed world's state can go.
    private static final Map<RandomState, Optional<NetherCathedrals>> CATHEDRALS = Collections.synchronizedMap(new WeakHashMap<>());

    private NetherRing5Terrain() {
    }

    /** The Nether ring 5 terrain of {@code random}, or null (no Nether ring 5 in this dimension). */
    @Nullable
    public static NetherCathedrals cathedrals(RandomState random) {
        return CATHEDRALS.computeIfAbsent(random, state -> Optional.ofNullable(NetherCathedrals.find(state.router().finalDensity()))).orElse(null);
    }

    @Nullable
    public static NetherCathedrals cathedrals(WorldGenLevel level) {
        return cathedrals(level.getLevel().getChunkSource().randomState());
    }
}
