package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

/**
 * The pure ground of ring 5 as the dressing (94/95) reads it: the terrain's field ({@link Ring5Terrain}), the ring layout,
 * the band, and the world's salt of the dressing's own decisions. Where the ground is, how high, and how far the nearest
 * void is (a crack, a corrupt spot's hole, a clean missing chunk, the void between the floes, a castle's belt). Never from
 * blocks: a reader in one chunk gets the same answer for a column in the next one, whatever generated first.
 *
 * @param salt the world's salt of the dressing ({@link #salt(long)}; the same from a random state, {@link #salt(RandomState)})
 */
public record Ring5World(Ring5Field field, RingLayout layout, Ring5Band band, long salt) {
    /** Columns farther from the void than this count as {@link #FAR} in {@link #voidDistance}. */
    public static final int MAX_REACH = 8;
    public static final int FAR = Integer.MAX_VALUE;
    /**
     * Beyond ring 5 the dressing stops where the floes do: at this share of the way from ring 5's outer boundary to the
     * world border ({@code Ring5Outer.EMPTY_SHARE}: nothing but void from there).
     */
    public static final double EDGE_SHARE = 0.7;
    static final ResourceLocation SALT_ID = CorruptedAdventure.id("ring_5_dressing");

    public static Ring5World of(Ring5Field field, RingLayout layout, long salt) {
        return new Ring5World(field, layout, Ring5Band.of(layout), salt);
    }

    /** The salt from the world's random state (the overworld's positional random, as the surface pass has it). */
    public static long salt(RandomState random) {
        return random.getOrCreateRandomFactory(SALT_ID).at(0, 0, 0).nextLong();
    }

    /** The same salt from the world seed: {@link RandomState} forks the overworld's xoroshiro random so (for the tests). */
    public static long salt(long worldSeed) {
        return new XoroshiroRandomSource(worldSeed).forkPositional().fromHashOf(SALT_ID.toString()).forkPositional()
                .at(0, 0, 0).nextLong();
    }

    /**
     * Whether the column belongs to ring 5's own dressing: ring 5 itself or the void edge beyond it as far as the floes go
     * ({@link #EDGE_SHARE}), not the zone 4→5 (ring 4's {@code sundered_reach}, whose ground is the fracture zone's).
     */
    public boolean core(int x, int z) {
        RingPosition position = layout.sampleOverworld(x, z);
        if (position.inTransition()) {
            return position.inner() == Ring.RING_5;
        }
        return position.inner() == Ring.RING_5 || position.inner() == Ring.VOID && layout.voidEdgeProgress(x, z) <= EDGE_SHARE;
    }

    /** How far (blocks) the column lies inside ring 5, to its nearest boundary; negative outside. */
    public double ringDepth(int x, int z) {
        return layout.boundaryDepth(Ring.RING_5, layout.effectiveDistance(x, z));
    }

    /** Whether the column is void: no ground under it in the terrain, or a clean missing chunk. */
    public boolean isVoid(int x, int z) {
        return field.column(x, z).isVoid() || Ring5MissingChunks.missing(this, x >> 4, z >> 4);
    }

    /** The highest solid block of the column's ground (not its cubes or spans), or {@link Integer#MIN_VALUE} over void. */
    public int top(int x, int z) {
        Ring5Field.Column column = field.column(x, z);
        return column.isVoid() ? Integer.MIN_VALUE : Mth.ceil(column.top()) - 1;
    }

    /**
     * Whether one of the own caves of the terrain (93: a net's shaft, a corridor cutting a slope) opens the column's top
     * block, so nothing stands on it. Vanilla's damped entrances are noise and not seen here.
     */
    public boolean openTop(int x, int z) {
        int top = top(x, z);
        return top != Integer.MIN_VALUE && field.ownCave(x, top, z);
    }

    /**
     * The Chebyshev distance (1 … {@code reach}) to the nearest void column, 0 if the column itself is void, {@link #FAR}
     * beyond {@code reach} (at most {@link #MAX_REACH}).
     */
    public int voidDistance(int x, int z, int reach) {
        if (isVoid(x, z)) {
            return 0;
        }
        int limit = Math.min(reach, MAX_REACH);
        for (int d = 1; d <= limit; d++) {
            for (int k = -d; k <= d; k++) {
                if (isVoid(x + k, z - d) || isVoid(x + k, z + d) || isVoid(x - d, z + k) || isVoid(x + d, z + k)) {
                    return d;
                }
            }
        }
        return FAR;
    }

    /** The nearest castle site to (x, z). */
    public Ring5Field.CastleSite nearestCastle(int x, int z) {
        Ring5Field.CastleSite nearest = null;
        for (Ring5Field.CastleSite site : field.castleSites()) {
            if (nearest == null || site.distance(x, z) < nearest.distance(x, z)) {
                nearest = site;
            }
        }
        return nearest;
    }
}
