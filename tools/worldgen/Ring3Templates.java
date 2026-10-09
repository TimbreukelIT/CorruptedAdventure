import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

/**
 * Generates the ring 3 structure templates (worldgen features, wave 2: 47, 47b, 49) as {@code .nbt} files under
 * {@code src/main/resources/data/corruptedadventure/structure/ring_3/}. Only the JDK; every shape comes from a fixed seed,
 * so two runs write identical bytes (gzip header without a time stamp). Run from the repository root:
 * {@code java tools/worldgen/Ring3Templates.java}.
 * <p>
 * The features ({@code surface_fossil}, {@code titan_rock}, {@code ash_cast}) keep only the blocks of their own list
 * from a template, so the templates hold only those blocks: no air, no structure voids.
 */
public final class Ring3Templates {
    /** Minecraft 1.21.1. */
    private static final int DATA_VERSION = 3955;
    private static final Path OUT = Path.of("src/main/resources/data/corruptedadventure/structure/ring_3");
    private static final String BONE = "minecraft:bone_block";
    private static final String ASH = "corruptedadventure:ash_block";
    private static final String BASALT = "minecraft:basalt";
    private static final String SMOOTH_BASALT = "minecraft:smooth_basalt";
    private static final String BLACKSTONE = "minecraft:blackstone";
    private static final String SMOLDERING_ASH = "corruptedadventure:smoldering_ash";

    private Ring3Templates() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUT);
        Map<String, Shape> shapes = new LinkedHashMap<>();
        // 47: sea monster fossils on the terraces of the dried seabed (at most 14 wide: they lie in one chunk).
        for (int i = 1; i <= 3; i++) {
            shapes.put("sea_serpent_" + i, seaSerpent(new Random(4700L + i)));
        }
        for (int i = 1; i <= 2; i++) {
            shapes.put("sea_skull_" + i, seaSkull(new Random(4710L + i)));
        }
        // 47b: titan skulls, loose vertebrae and bones (one chunk), and the titan rocks (up to 31 wide, built per chunk).
        for (int i = 1; i <= 2; i++) {
            shapes.put("titan_skull_" + i, titanSkull(new Random(4720L + i)));
        }
        for (int i = 1; i <= 3; i++) {
            shapes.put("titan_vertebra_" + i, titanVertebra(new Random(4730L + i)));
        }
        for (int i = 1; i <= 2; i++) {
            shapes.put("titan_bone_" + i, titanBone(new Random(4740L + i)));
        }
        shapes.put("titan_rock_kneeling", titanKneeling(new Random(4751L)));
        shapes.put("titan_rock_hunched", titanHunched(new Random(4752L)));
        shapes.put("titan_rock_bust", titanBust(new Random(4753L)));
        // 49: casts of ash in human poses, facing +x (the feature turns them to the nearest volcano).
        shapes.put("ash_cast_crawling", cast(new Random(4901L), new int[][]{
                {0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {2, 1, 0}, {3, 0, -1}, {4, 0, -1}}));
        shapes.put("ash_cast_kneeling", cast(new Random(4902L), new int[][]{
                {0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {2, 1, 0}}));
        shapes.put("ash_cast_reaching", cast(new Random(4903L), new int[][]{
                {0, 0, 0}, {0, 1, 0}, {0, 2, 0}, {0, 2, 1}, {0, 3, 1}}));
        shapes.put("ash_cast_lying", cast(new Random(4904L), new int[][]{
                {0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {2, 0, 1}}));
        shapes.put("ash_cast_curled", cast(new Random(4905L), new int[][]{
                {0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 0}}));
        for (Map.Entry<String, Shape> entry : shapes.entrySet()) {
            Path file = OUT.resolve(entry.getKey() + ".nbt");
            Files.write(file, entry.getValue().nbt());
            System.out.println(file + ": " + entry.getValue().blocks.size() + " blocks, size " + java.util.Arrays.toString(entry.getValue().size()));
        }
    }

    // ---------------------------------------------------------------- shapes

    /** A sea serpent's spine, curving across 13–14 blocks, with pairs of ribs on the front half and a tail fin. */
    static Shape seaSerpent(Random random) {
        Shape shape = new Shape();
        double phase = random.nextDouble() * Math.PI;
        double amplitude = 1.5 + random.nextDouble();
        int length = 13 + random.nextInt(2);
        for (int x = 0; x < length; x++) {
            int z = 4 + (int) Math.round(amplitude * Math.sin(phase + x * 0.55));
            int y = x < 3 ? 1 : 0;
            shape.put(x, y, z, BONE);
            // Ribs on the front two thirds, every other vertebra: arching up and out, touching diagonally.
            if (x >= 2 && x < length * 2 / 3 && x % 2 == 0) {
                int rib = x < length / 3 ? 3 : 2;
                for (int side = -1; side <= 1; side += 2) {
                    for (int k = 1; k <= rib; k++) {
                        if (random.nextFloat() < 0.85F) {
                            shape.put(x, k == rib ? 0 : 1, z + side * k, BONE);
                        }
                    }
                }
            }
        }
        // The skull at the front: a short block with a jaw.
        int z0 = 4 + (int) Math.round(amplitude * Math.sin(phase));
        shape.put(0, 2, z0, BONE);
        shape.put(0, 1, z0 - 1, BONE);
        shape.put(0, 1, z0 + 1, BONE);
        // The tail fin: a fan of three.
        int zt = 4 + (int) Math.round(amplitude * Math.sin(phase + (length - 1) * 0.55));
        shape.put(length - 1, 1, zt - 1, BONE);
        shape.put(length - 1, 1, zt + 1, BONE);
        return shape.normalised();
    }

    /** A long-jawed sea monster skull: a domed braincase with eye sockets and two jaws full of teeth. */
    static Shape seaSkull(Random random) {
        Shape shape = new Shape();
        int jaw = 6 + random.nextInt(3);
        // Braincase 4 long, 5 wide, 3 high, hollow eye sockets on the sides.
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 0; y < 3; y++) {
                    boolean socket = x == 2 && y == 1 && (z == 0 || z == 4);
                    boolean hollow = x > 0 && x < 3 && z > 0 && z < 4 && y < 2;
                    if (!socket && !hollow && !(y == 2 && (z == 0 || z == 4))) {
                        shape.put(x, y, z, BONE);
                    }
                }
            }
        }
        // Upper and lower jaw, narrowing; teeth hang between them.
        for (int x = 4; x < 4 + jaw; x++) {
            int half = x < 4 + jaw / 2 ? 1 : 0;
            for (int z = 2 - half; z <= 2 + half; z++) {
                shape.put(x, 2, z, BONE);
                shape.put(x, 0, z, BONE);
            }
            if (x % 2 == 1 && random.nextFloat() < 0.8F) {
                shape.put(x, 1, 2 - half, BONE);
            }
        }
        return shape.normalised();
    }

    /** A titan skull, 9-11 long: a domed braincase with two deep eye sockets, a nose hole and a heavy jaw. */
    static Shape titanSkull(Random random) {
        Shape shape = new Shape();
        double rx = 4.5 + random.nextDouble();
        double ry = 4.0;
        double rz = 4.0 + random.nextDouble() * 0.8;
        int cx = 5;
        int cz = 5;
        for (int x = 0; x <= 11; x++) {
            for (int y = 0; y <= 8; y++) {
                for (int z = 0; z <= 10; z++) {
                    double dx = (x - cx) / rx;
                    double dy = (y - 3) / ry;
                    double dz = (z - cz) / rz;
                    double d = dx * dx + dy * dy + dz * dz;
                    // A shell 1-2 thick; the bottom stays open where it lies on the ground.
                    if (d <= 1.0 && d >= 0.5 && y >= 1) {
                        boolean socket = x >= cx + 2 && y >= 3 && y <= 5 && (Math.abs(z - (cz - 2)) <= 1 || Math.abs(z - (cz + 2)) <= 1);
                        boolean nose = x >= cx + 3 && y == 2 && z == cz;
                        if (!socket && !nose) {
                            shape.put(x, y, z, BONE);
                        }
                    }
                }
            }
        }
        // The jaw: a U of bone reaching forward on the ground, teeth on its rim.
        for (int x = cx; x <= cx + 6; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                boolean rim = Math.abs(z - cz) == 3 || x == cx + 6;
                if (rim) {
                    shape.put(x, 0, z, BONE);
                    if ((x + z) % 2 == 0 && random.nextFloat() < 0.8F) {
                        shape.put(x, 1, z, BONE);
                    }
                }
            }
        }
        return shape.normalised();
    }

    /** A loose titan vertebra: a ring with a spine on top and two wings, 5-7 wide. */
    static Shape titanVertebra(Random random) {
        Shape shape = new Shape();
        int r = 2 + random.nextInt(2);
        for (int y = 0; y <= 2 * r; y++) {
            for (int z = 0; z <= 2 * r; z++) {
                double d = Math.hypot(y - r, z - r);
                if (d <= r + 0.5 && d >= r - 0.9) {
                    shape.put(0, y, z, BONE);
                    shape.put(1, y, z, BONE);
                }
            }
        }
        int spine = 1 + random.nextInt(3);
        for (int k = 1; k <= spine; k++) {
            shape.put(0, 2 * r + k, r, BONE);
        }
        shape.put(0, r, -1, BONE);
        shape.put(0, r, 2 * r + 1, BONE);
        return shape.normalised();
    }

    /** A long bone of a titan limb, 7-12 long, with knobbed ends. */
    static Shape titanBone(Random random) {
        Shape shape = new Shape();
        int length = 7 + random.nextInt(6);
        for (int x = 0; x < length; x++) {
            shape.put(x, 0, 1, BONE);
        }
        for (int end : new int[]{0, length - 1}) {
            shape.put(end, 0, 0, BONE);
            shape.put(end, 0, 2, BONE);
            shape.put(end, 1, 1, BONE);
        }
        return shape.normalised();
    }

    /** A kneeling giant (~26 high, ~20 long): shins on the ground, upright thighs and torso, head bowed, arms hanging. */
    static Shape titanKneeling(Random random) {
        Body body = new Body(random);
        for (int side : new int[]{4, 12}) {
            body.capsule(2, 2, side, 14, 2, side, 2.6);
            body.capsule(14, 3, side, 12, 12, side, 3.0);
            body.capsule(12, 22, side < 8 ? 2 : 14, 15, 11, side < 8 ? 1 : 15, 1.9);
        }
        body.capsule(11, 12, 8, 12, 22, 8, 5.2);
        body.capsule(12, 22, 3, 12, 22, 13, 2.6);
        body.sphere(14, 25, 8, 3.2);
        return body.shape();
    }

    /** A seated giant hunched over the knees (~19 high). */
    static Shape titanHunched(Random random) {
        Body body = new Body(random);
        for (int side : new int[]{4, 13}) {
            body.capsule(2, 3, side, 14, 9, side, 3.0);
            body.capsule(14, 9, side, 16, 1, side, 2.6);
        }
        body.capsule(2, 3, 8, 9, 14, 8, 5.0);
        body.sphere(12, 16, 8, 3.2);
        body.capsule(8, 13, 3, 14, 10, 8, 1.8);
        body.capsule(8, 13, 14, 14, 10, 9, 1.8);
        return body.shape();
    }

    /** Head and shoulders of a buried giant breaking out of the ground (~27 high, ~29 wide). */
    static Shape titanBust(Random random) {
        Body body = new Body(random);
        body.capsule(8, 0, 5, 8, 8, 23, 5.0);
        body.capsule(8, 6, 14, 8, 16, 14, 4.0);
        body.sphere(9, 20, 14, 6.5);
        body.capsule(8, 6, 6, 16, 1, 5, 2.6);
        return body.shape();
    }

    /**
     * Basalt bodies from spheres and capsules (y below 0 cut off), weathered: smooth basalt on most upper faces, a few
     * blackstone blocks, now and then an edge block gone.
     */
    static final class Body {
        private final Random random;
        private final Shape solid = new Shape();

        Body(Random random) {
            this.random = random;
        }

        void sphere(double x, double y, double z, double r) {
            capsule(x, y, z, x, y, z, r);
        }

        void capsule(double ax, double ay, double az, double bx, double by, double bz, double r) {
            int x0 = (int) Math.floor(Math.min(ax, bx) - r);
            int x1 = (int) Math.ceil(Math.max(ax, bx) + r);
            int y0 = (int) Math.floor(Math.min(ay, by) - r);
            int y1 = (int) Math.ceil(Math.max(ay, by) + r);
            int z0 = (int) Math.floor(Math.min(az, bz) - r);
            int z1 = (int) Math.ceil(Math.max(az, bz) + r);
            double lx = bx - ax;
            double ly = by - ay;
            double lz = bz - az;
            double ll = lx * lx + ly * ly + lz * lz;
            for (int x = x0; x <= x1; x++) {
                for (int y = Math.max(0, y0); y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        double t = ll == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, ((x - ax) * lx + (y - ay) * ly + (z - az) * lz) / ll));
                        double dx = x - (ax + t * lx);
                        double dy = y - (ay + t * ly);
                        double dz = z - (az + t * lz);
                        if (dx * dx + dy * dy + dz * dz <= r * r) {
                            solid.put(x, y, z, BASALT);
                        }
                    }
                }
            }
        }

        Shape shape() {
            Shape out = new Shape();
            for (Map.Entry<Long, String> e : solid.blocks.entrySet()) {
                int x = Shape.x(e.getKey());
                int y = Shape.y(e.getKey());
                int z = Shape.z(e.getKey());
                boolean top = !solid.blocks.containsKey(Shape.key(x, y + 1, z));
                boolean edge = top || !solid.blocks.containsKey(Shape.key(x + 1, y, z)) || !solid.blocks.containsKey(Shape.key(x - 1, y, z))
                        || !solid.blocks.containsKey(Shape.key(x, y, z + 1)) || !solid.blocks.containsKey(Shape.key(x, y, z - 1));
                float roll = random.nextFloat();
                if (edge && y > 2 && roll < 0.04F) {
                    continue;
                }
                String block = top && roll < 0.55F ? SMOOTH_BASALT : roll > 0.93F ? BLACKSTONE : BASALT;
                out.put(x, y, z, block);
            }
            return out.normalised();
        }
    }

    /**
     * A figure of ash: the given blocks (x forward, y up, z to the side), about a quarter of them glowing
     * {@code smoldering_ash} (the cracks), at least one.
     */
    static Shape cast(Random random, int[][] blocks) {
        Shape shape = new Shape();
        int glowing = 0;
        for (int i = 0; i < blocks.length; i++) {
            boolean glow = random.nextFloat() < 0.25F || i == blocks.length - 1 && glowing == 0;
            glowing += glow ? 1 : 0;
            shape.put(blocks[i][0], blocks[i][1], blocks[i][2], glow ? SMOLDERING_ASH : ASH);
        }
        return shape.normalised();
    }

    // ---------------------------------------------------------------- template writing

    /** Blocks by position (x, y, z ≥ 0 after {@link #normalised()}), in a fixed order. */
    static final class Shape {
        final TreeMap<Long, String> blocks = new TreeMap<>();

        void put(int x, int y, int z, String block) {
            blocks.put(key(x, y, z), block);
        }

        static long key(int x, int y, int z) {
            return ((long) (y + 512) << 40) | ((long) (z + 512) << 20) | (x + 512);
        }

        static int x(long key) {
            return (int) (key & 0xFFFFF) - 512;
        }

        static int z(long key) {
            return (int) ((key >> 20) & 0xFFFFF) - 512;
        }

        static int y(long key) {
            return (int) (key >> 40) - 512;
        }

        Shape normalised() {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            for (long key : blocks.keySet()) {
                minX = Math.min(minX, x(key));
                minY = Math.min(minY, y(key));
                minZ = Math.min(minZ, z(key));
            }
            Shape out = new Shape();
            for (Map.Entry<Long, String> e : blocks.entrySet()) {
                out.put(x(e.getKey()) - minX, y(e.getKey()) - minY, z(e.getKey()) - minZ, e.getValue());
            }
            return out;
        }

        int[] size() {
            int[] size = new int[3];
            for (long key : blocks.keySet()) {
                size[0] = Math.max(size[0], x(key) + 1);
                size[1] = Math.max(size[1], y(key) + 1);
                size[2] = Math.max(size[2], z(key) + 1);
            }
            return size;
        }

        /** The gzipped structure template (vanilla's format: size, palette, blocks, entities, DataVersion). */
        byte[] nbt() {
            List<String> palette = new ArrayList<>();
            for (String block : blocks.values()) {
                if (!palette.contains(block)) {
                    palette.add(block);
                }
            }
            try {
                ByteArrayOutputStream raw = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(raw);
                out.writeByte(10);
                out.writeUTF("");
                int[] size = size();
                listOfInts(out, "size", size);
                out.writeByte(9);
                out.writeUTF("entities");
                out.writeByte(10);
                out.writeInt(0);
                out.writeByte(9);
                out.writeUTF("blocks");
                out.writeByte(10);
                out.writeInt(blocks.size());
                for (Map.Entry<Long, String> e : blocks.entrySet()) {
                    listOfInts(out, "pos", new int[]{x(e.getKey()), y(e.getKey()), z(e.getKey())});
                    out.writeByte(3);
                    out.writeUTF("state");
                    out.writeInt(palette.indexOf(e.getValue()));
                    out.writeByte(0);
                }
                out.writeByte(9);
                out.writeUTF("palette");
                out.writeByte(10);
                out.writeInt(palette.size());
                for (String block : palette) {
                    String name = block;
                    String properties = null;
                    int bracket = block.indexOf('[');
                    if (bracket >= 0) {
                        name = block.substring(0, bracket);
                        properties = block.substring(bracket + 1, block.length() - 1);
                    }
                    if (properties != null) {
                        out.writeByte(10);
                        out.writeUTF("Properties");
                        for (String property : properties.split(",")) {
                            String[] kv = property.split("=");
                            out.writeByte(8);
                            out.writeUTF(kv[0]);
                            out.writeUTF(kv[1]);
                        }
                        out.writeByte(0);
                    }
                    out.writeByte(8);
                    out.writeUTF("Name");
                    out.writeUTF(name);
                    out.writeByte(0);
                }
                out.writeByte(3);
                out.writeUTF("DataVersion");
                out.writeInt(DATA_VERSION);
                out.writeByte(0);
                out.flush();
                ByteArrayOutputStream zipped = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(zipped)) {
                    gzip.write(raw.toByteArray());
                }
                return zipped.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static void listOfInts(DataOutputStream out, String name, int[] values) throws IOException {
            out.writeByte(9);
            out.writeUTF(name);
            out.writeByte(3);
            out.writeInt(values.length);
            for (int value : values) {
                out.writeInt(value);
            }
        }
    }
}
