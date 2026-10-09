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
 * Generates the ring 3 structure templates (worldgen features, wave 2: 47) as {@code .nbt} files under
 * {@code src/main/resources/data/corruptedadventure/structure/ring_3/}. Only the JDK; every shape comes from a fixed seed,
 * so two runs write identical bytes (gzip header without a time stamp). Run from the repository root:
 * {@code java tools/worldgen/Ring3Templates.java}.
 * <p>
 * The features ({@code surface_fossil}) keep only the blocks of their own list from a template, so the templates hold
 * only those blocks: no air, no structure voids.
 */
public final class Ring3Templates {
    /** Minecraft 1.21.1. */
    private static final int DATA_VERSION = 3955;
    private static final Path OUT = Path.of("src/main/resources/data/corruptedadventure/structure/ring_3");
    private static final String BONE = "minecraft:bone_block";

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
