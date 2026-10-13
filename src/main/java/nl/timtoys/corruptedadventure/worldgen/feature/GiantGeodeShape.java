package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeededHash;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure geometry of a giant geode of Nether ring 4 (65), shared by the density function {@code giant_geodes} (which
 * hollows it out) and the lining feature of 78, so both agree on where a geode is. Like {@link DewGeodeShape}: one geode
 * per cell of a world-fixed 2D grid at most, placed so that the whole hollow, doorways included, stays inside its cell.
 * <p>
 * A geode is a slightly stretched sphere hollowed out above a flat floor at a third of its height (you walk on the
 * floor), with one to {@link Settings#doorsMax()} arched doorways at floor level, each running from the wall to the first
 * open terrain in its direction (a tunnel, a neighbouring hall, the dunes): see {@link #doors}.
 */
public final class GiantGeodeShape {
    private static final int SALT = 41;
    /** How much the hollow differs from a sphere: axis stretch in [1 − this, 1 + this]. */
    private static final double STRETCH = 0.15;
    /** How many centres a cell tries, in order; the first the density function accepts is the geode of the cell. */
    public static final int ATTEMPTS = 24;
    /** How many directions a geode tries for its doorways. */
    public static final int DOOR_DIRECTIONS = 8;
    /** How many points {@link Geode#domeTestPoints} gives. */
    public static final int DOME_POINTS = 65;

    private GiantGeodeShape() {
    }

    /** {@link SeededHash} of a cell, whatever noise it comes from (a density function's holder, a feature's noise). */
    @FunctionalInterface
    public interface CellHash {
        long hash(int cellX, int cellZ, int salt);
    }

    /** Is the block open in the terrain the doorways must reach (outside the geode)? */
    @FunctionalInterface
    public interface OpenAt {
        boolean open(int x, int y, int z);
    }

    /**
     * @param regionMax      the geode's centre must lie at or below this region value (deep inside the geode hollows)
     * @param regionReachMax the whole geode, doorways included, must lie at or below this region value (where the
     *                       hollows have weight in the terrain: no geode cut in half by the mass's edge)
     * @param floorMin       lowest Y of the flat floor
     * @param shell          solid rock wanted above the dome
     * @param doorLength     how far a doorway may run past the wall to reach open terrain
     * @param doorsMax       at most this many doorways (at least one, or the attempt is no geode)
     */
    public record Settings(int cellSize, double chance, int radiusMin, int radiusMax, int floorMin, int floorMax, double regionMax,
                           double regionReachMax, int shell, int doorWidth, int doorHeight, int doorLength, int doorsMax) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.<Settings>create(instance -> instance.group(
                Codec.intRange(32, 512).fieldOf("cell_size").forGetter(Settings::cellSize),
                Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(Settings::chance),
                Codec.intRange(4, 40).fieldOf("radius_min").forGetter(Settings::radiusMin),
                Codec.intRange(4, 40).fieldOf("radius_max").forGetter(Settings::radiusMax),
                Codec.INT.fieldOf("floor_min").forGetter(Settings::floorMin),
                Codec.INT.fieldOf("floor_max").forGetter(Settings::floorMax),
                Codec.DOUBLE.fieldOf("region_max").forGetter(Settings::regionMax),
                Codec.DOUBLE.fieldOf("region_reach_max").forGetter(Settings::regionReachMax),
                Codec.intRange(0, 16).fieldOf("shell").forGetter(Settings::shell),
                Codec.intRange(1, 16).fieldOf("door_width").forGetter(Settings::doorWidth),
                Codec.intRange(1, 16).fieldOf("door_height").forGetter(Settings::doorHeight),
                Codec.intRange(1, 64).fieldOf("door_length").forGetter(Settings::doorLength),
                Codec.intRange(1, 4).fieldOf("doors_max").forGetter(Settings::doorsMax)
        ).apply(instance, Settings::new)).validate(Settings::validate);

        private static DataResult<Settings> validate(Settings settings) {
            if (settings.radiusMin > settings.radiusMax) {
                return DataResult.error(() -> "giant geode: radius min above max");
            }
            if (settings.floorMin > settings.floorMax) {
                return DataResult.error(() -> "giant geode: floor min above max");
            }
            if (settings.cellSize % 16 != 0) {
                // The lining feature (78) takes the cell of a chunk's corner: a chunk must lie within one cell.
                return DataResult.error(() -> "giant geode: cell_size must be a multiple of 16");
            }
            if (settings.cellSize - 2 * settings.margin() < 4) {
                return DataResult.error(() -> "giant geode: cell_size too small for the radii and the doorways");
            }
            return DataResult.success(settings);
        }

        /** Distance from a cell edge to the nearest possible centre: the whole geode with its doorways fits in the cell. */
        public int margin() {
            return (int) Math.ceil(radiusMax * (1.0 + STRETCH) + doorLength + doorWidth * 0.5) + 1;
        }

        /** The highest Y any hollow reaches. */
        public int highest() {
            return floorMax + (int) Math.ceil(radiusMax * (1.0 + STRETCH) * 4.0 / 3.0) + 1;
        }
    }

    /** A doorway: leaves the centre in direction ({@code dirX}, {@code dirZ}) (a unit vector) and ends {@code end} blocks from it. */
    public record Door(double dirX, double dirZ, double end) {
    }

    /**
     * A geode: centre ({@code x}, {@code y}, {@code z}), the hollow is the ellipsoid {@link #distance}{@code <= radius}
     * above {@code floor}, with its doorways (arched, {@code doorWidth} wide and {@code doorHeight} high, on the floor).
     */
    public record Geode(int x, int y, int z, int radius, double stretchX, double stretchY, double stretchZ, int floor,
                        List<Door> doors, int doorWidth, int doorHeight) {
        /** Stretched distance in blocks from the centre to the block. */
        public double distance(int blockX, int blockY, int blockZ) {
            double dx = (blockX - x) / stretchX;
            double dy = (blockY - y) / stretchY;
            double dz = (blockZ - z) / stretchZ;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /** Whether the block is open: in the hollow above the floor, or in a doorway. */
        public boolean hollow(int blockX, int blockY, int blockZ) {
            if (blockY < floor) {
                return false;
            }
            if (distance(blockX, blockY, blockZ) <= radius) {
                return true;
            }
            int dx = blockX - x;
            int dz = blockZ - z;
            double up = (blockY - floor) / (double) doorHeight;
            for (Door door : doors) {
                double along = dx * door.dirX() + dz * door.dirZ();
                if (along < 0.0 || along > door.end()) {
                    continue;
                }
                double lateral = (-dx * door.dirZ() + dz * door.dirX()) / (doorWidth * 0.5);
                if (lateral * lateral + up * up <= 1.0) {
                    return true;
                }
            }
            return false;
        }

        /** Horizontal distance from the centre to the farthest point of the wall. */
        public double wall() {
            return radius * Math.max(stretchX, stretchZ);
        }

        /** The same geode with these doorways. */
        public Geode withDoors(List<Door> doors) {
            return new Geode(x, y, z, radius, stretchX, stretchY, stretchZ, floor, List.copyOf(doors), doorWidth, doorHeight);
        }

        /** Largest horizontal distance in blocks from the centre to any open block. */
        public int reach() {
            double reach = wall();
            for (Door door : doors) {
                reach = Math.max(reach, door.end() + doorWidth * 0.5);
            }
            return (int) Math.ceil(reach);
        }

        /**
         * The {@link #DOME_POINTS} points over the dome: straight up and 16 directions round at 4 heights (15°, 35°, 55°,
         * 75° above the horizon), {@code shell} blocks out from the stretched wall, where the rock must still be solid. The
         * sides below 15° are not tested: they may open.
         */
        public List<int[]> domeTestPoints(int shell) {
            List<int[]> points = new ArrayList<>(DOME_POINTS);
            points.add(new int[]{x, y + (int) Math.round(radius * stretchY + shell), z});
            for (double elevation : new double[]{15.0, 35.0, 55.0, 75.0}) {
                double up = Math.sin(Math.toRadians(elevation));
                double out = Math.cos(Math.toRadians(elevation));
                for (int i = 0; i < 16; i++) {
                    double angle = i * Math.PI / 8.0;
                    double dx = out * Math.cos(angle);
                    double dz = out * Math.sin(angle);
                    points.add(new int[]{x + (int) Math.round(dx * (radius * stretchX + shell)),
                            y + (int) Math.round(up * (radius * stretchY + shell)),
                            z + (int) Math.round(dz * (radius * stretchZ + shell))});
                }
            }
            return points;
        }
    }

    /** Whether cell ({@code cellX}, {@code cellZ}) has a geode at all (one draw per cell). */
    public static boolean hasGeode(Settings settings, CellHash hash, int cellX, int cellZ) {
        return SeededHash.unit(hash.hash(cellX, cellZ, SALT), 0) < settings.chance();
    }

    /** The hash attempt {@code attempt} of a cell draws from (its position, size and doorway directions). */
    public static long attemptHash(CellHash hash, int cellX, int cellZ, int attempt) {
        return hash.hash(cellX, cellZ, SALT + 1 + attempt);
    }

    /**
     * Attempt {@code attempt} of cell ({@code cellX}, {@code cellZ}), still without doorways ({@link #doors}). Pure: the
     * same seed (through {@code hash}) gives the same answer.
     */
    public static Geode candidate(Settings settings, CellHash hash, int cellX, int cellZ, int attempt) {
        long h = attemptHash(hash, cellX, cellZ, attempt);
        int margin = settings.margin();
        int span = settings.cellSize() - 2 * margin;
        int x = cellX * settings.cellSize() + margin + (int) (SeededHash.unit(h, 1) * span);
        int z = cellZ * settings.cellSize() + margin + (int) (SeededHash.unit(h, 2) * span);
        int radius = settings.radiusMin() + (int) (SeededHash.unit(h, 3) * (settings.radiusMax() - settings.radiusMin() + 1));
        int floor = settings.floorMin() + (int) (SeededHash.unit(h, 4) * (settings.floorMax() - settings.floorMin() + 1));
        double stretchY = stretch(h, 6);
        // The floor lies at a third of the hollow's height: centre = floor + radius_y / 3.
        int y = floor + (int) Math.round(radius * stretchY / 3.0);
        return new Geode(x, y, z, radius, stretch(h, 5), stretchY, stretch(h, 7), floor, List.of(),
                settings.doorWidth(), settings.doorHeight());
    }

    /**
     * The doorways of {@code geode}: of {@link #DOOR_DIRECTIONS} evenly spread directions from a seeded start angle (drawn
     * from {@code attemptHash}), the first {@link Settings#doorsMax()} whose ray along the floor (at {@code floor + 2})
     * meets open terrain past the wall within {@link Settings#doorLength()} blocks; each runs 2 blocks into that opening,
     * so it always leads somewhere. Directions within 60 degrees of an earlier doorway are skipped. Pure.
     */
    public static List<Door> doors(Settings settings, Geode geode, long attemptHash, OpenAt open) {
        return doors(settings, geode, attemptHash, open, door -> true);
    }

    /** {@link #doors(Settings, Geode, long, OpenAt)}, keeping only the doorways {@code accept} takes (a refused one leaves its place to the next direction). */
    public static List<Door> doors(Settings settings, Geode geode, long attemptHash, OpenAt open, java.util.function.Predicate<Door> accept) {
        List<Door> doors = new ArrayList<>(settings.doorsMax());
        double start = SeededHash.unit(attemptHash, 8) * Math.PI * 2.0;
        int y = geode.floor() + 2;
        double last = geode.wall() + settings.doorLength();
        for (int i = 0; i < DOOR_DIRECTIONS && doors.size() < settings.doorsMax(); i++) {
            // 0, 4, 2, 6, 1, 5, 3, 7: the first doorways spread round the geode
            int k = Integer.reverse(i) >>> (Integer.SIZE - 3);
            double angle = start + k * Math.PI * 2.0 / DOOR_DIRECTIONS;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            if (doors.stream().anyMatch(d -> d.dirX() * dirX + d.dirZ() * dirZ > 0.5)) {
                continue;
            }
            for (double t = Math.ceil(geode.wall()) + 1.0; t <= last; t += 1.0) {
                if (open.open(geode.x() + (int) Math.round(dirX * t), y, geode.z() + (int) Math.round(dirZ * t))) {
                    Door door = new Door(dirX, dirZ, Math.min(t + 2.0, last));
                    if (accept.test(door)) {
                        doors.add(door);
                    }
                    break;
                }
            }
        }
        return doors;
    }

    private static double stretch(long hash, int n) {
        return 1.0 - STRETCH + 2.0 * STRETCH * SeededHash.unit(hash, n);
    }
}
