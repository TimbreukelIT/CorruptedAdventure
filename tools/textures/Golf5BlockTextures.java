import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the block textures of phase 2 golf 5 (stream T, design §7.2, §7.4): sky sand, sky glass, the sky glass
 * pane edge and the flickering glitch shard block. Our own pixels, no vanilla pixels read or copied.
 * Run from the repository root: {@code java tools/textures/Golf5BlockTextures.java}.
 *
 * <p>Sky sand and sky glass are bright, light sky-blue white and pure: sky sand is the bone sand grain in sky-white with
 * pure white sparkles and a few cream purity-crystal grains; sky glass a white-blue rim (lit top left), a double streak
 * and a small four-point sparkle, alpha 0/255 only (cutout). The glitch shard block is fused shards in the glitch
 * shard's magenta and black (no light, no emissive); its {@code .mcmeta} holds frame 0 most of the time with short
 * flickers: rows slipping sideways, a band flipping to cyan, rows dropping out.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent. Edit the
 * grids by hand and run the script again; the output is byte-for-byte reproducible. Checked before writing: size (16 x
 * 16, 16 x 16 per frame for animations, 18 x 18 for effect icons, 32 x 32 for arrow entities) and every character in
 * the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf5BlockTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    /** {@code frames}: null for a still texture, else the {@code .mcmeta} frame order as {index, ticks} pairs. */
    private record Texture(String name, Map<Character, Integer> palette, int[][] frames, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf5BlockTextures() {
    }

    public static void main(String[] args) throws IOException {
        // the bone sand grain in sky-white, pure white sparkles, a few cream grains
        add("block/sky_sand",
                palette("1=9CB6D2", "2=B2C8E0", "3=C6D8EC", "4=D8E6F6", "s=FFFFFF", "c=E8DEC0"),
                "3333334333333333",
                "3333ss33c3333333",
                "444332333ss33333",
                "1433333s43233343",
                "3333333243333331",
                "4323333343133334",
                "3333333344334333",
                "3s33332343343333",
                "323343c344333333",
                "333433333343c33c",
                "3333333333314423",
                "4313333333333333",
                "4343333233333331",
                "3343332223343333",
                "33333c4333333433",
                "33c3333333333333");
        // white-blue rim lit top left, a double streak and a four-point sparkle
        add("block/sky_glass",
                palette("W=F4FAFF", "A=DCEEFF", "B=BCDCFA", "C=9CC6F0", "D=80B0E4", "s=FFFFFF"),
                "WWWWWWWWWWWWWWWA",
                "W..............B",
                "W...WA.........B",
                "W..WA..........C",
                "W.WA..A........C",
                "W....A.........C",
                "W..............C",
                "W..............C",
                "W..............C",
                "W..............C",
                "W..........A...C",
                "A.........AsA..C",
                "W..........A...D",
                "A..............D",
                "B..............D",
                "BBCBCCCCCCCCDDDD");
        // the pane edge: the rim tones in two columns
        add("block/sky_glass_pane_top",
                palette("W=F4FAFF", "A=DCEEFF", "B=BCDCFA", "C=9CC6F0", "D=80B0E4"),
                ".......WA.......",
                ".......AB.......",
                ".......AC.......",
                ".......WB.......",
                ".......WA.......",
                ".......WB.......",
                ".......AB.......",
                ".......AC.......",
                ".......WB.......",
                ".......WA.......",
                ".......AB.......",
                ".......WC.......",
                ".......AB.......",
                ".......BD.......",
                ".......AC.......",
                ".......WB.......");
        // fused magenta shards pointing up-right; frames: still, rows slipped, a cyan band, rows dropped out
        addAnimated("block/glitch_shard_block",
                palette("0=000000", "1=14001A", "3=5A0060", "4=B000B8", "5=F800F8", "6=FF70FF", "c=00E8F0", "w=F4F4F4"),
                frames(0, 40, 1, 2, 0, 18, 2, 3, 0, 30, 3, 2, 0, 12, 1, 2),
                "1043063055431530",
                "4001530654305305",
                "4300305443053065",
                "3053000431630554",
                "163054100w305443",
                "5306543003055431",
                "3055431530654305",
                "0544306305443053",
                "5543053000431630",
                "5431530541005306",
                "4305306543003054",
                "3063055431530004",
                "15w0544306305400",
                "0305543053065431",
                "3065431530554305",
                "0544305305443063",
                "1043063055431530",
                "4001530654305305",
                "4300305443053065",
                "3053000431630554",
                "163054100w305443",
                "4315306543003055",
                "3053055431530654",
                "0530544306305443",
                "5543053000431630",
                "5431530541005306",
                "4305306543003054",
                "3063055431530004",
                "15w0544306305400",
                "0305543053065431",
                "3065431530554305",
                "0544305305443063",
                "1043063055431530",
                "4001530654305305",
                "4300305443053065",
                "3053000431630554",
                "163054100w305443",
                "5306543003055431",
                "3055431530654305",
                "0544306305443053",
                "5543053000431630",
                "c431c30c4100c30c",
                "430c30cc430030c4",
                "30c30cc431c30004",
                "15w0544306305400",
                "0305543053065431",
                "3065431530554305",
                "0544305305443063",
                "1043063055431530",
                "4001530654305305",
                "0000000000000000",
                "5300043163055430",
                "163054100w305443",
                "5306543003055431",
                "3055431530654305",
                "0544306305443053",
                "5543053000431630",
                "5431530541005306",
                "4305306543003054",
                "3063055431530004",
                "15w0544306305400",
                "w0000w0000w0000w",
                "3065431530554305",
                "0544305305443063");
        List<BufferedImage> written = new ArrayList<>();
        for (Texture texture : TEXTURES_TO_WRITE) {
            written.add(write(texture));
        }
        contactSheet(written);
    }

    private static void add(String name, Map<Character, Integer> palette, String... rows) {
        TEXTURES_TO_WRITE.add(new Texture(name, palette, null, rows));
    }

    private static void addAnimated(String name, Map<Character, Integer> palette, int[][] frames, String... rows) {
        TEXTURES_TO_WRITE.add(new Texture(name, palette, frames, rows));
    }

    /** {@code "c=RRGGBB"} pairs. */
    private static Map<Character, Integer> palette(String... entries) {
        Map<Character, Integer> palette = new HashMap<>();
        for (String entry : entries) {
            palette.put(entry.charAt(0), Integer.parseInt(entry.substring(2), 16));
        }
        return palette;
    }

    /** {@code index, ticks, index, ticks, ...} → {index, ticks} pairs. */
    private static int[][] frames(int... pairs) {
        int[][] frames = new int[pairs.length / 2][];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new int[]{pairs[2 * i], pairs[2 * i + 1]};
        }
        return frames;
    }

    private static BufferedImage write(Texture texture) throws IOException {
        String name = texture.name();
        int height = texture.rows().length;
        int width = texture.rows()[0].length();
        boolean entity = name.startsWith("entity/projectiles/");
        boolean icon = name.startsWith("mob_effect/");
        int frameCount = 1;
        if (texture.frames() != null) {
            for (int[] frame : texture.frames()) {
                frameCount = Math.max(frameCount, frame[0] + 1);
            }
        }
        int wantWidth = entity ? 32 : icon ? 18 : 16;
        int wantHeight = entity ? 32 : icon ? 18 : 16 * frameCount;
        if (width != wantWidth || height != wantHeight) {
            throw new IllegalStateException(name + ": " + width + " x " + height);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            String row = texture.rows()[y];
            if (row.length() != width) {
                throw new IllegalStateException(name + ": row " + y + " has " + row.length() + " pixels");
            }
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = texture.palette().get(c);
                if (rgb == null) {
                    throw new IllegalStateException(name + ": '" + c + "' not in the palette");
                }
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        Path file = TEXTURES.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("wrote " + file);
        if (texture.frames() != null) {
            StringBuilder json = new StringBuilder("{\n  \"animation\": {\n    \"interpolate\": false,\n    \"frames\": [\n");
            for (int i = 0; i < texture.frames().length; i++) {
                int[] frame = texture.frames()[i];
                json.append("      {\"index\": ").append(frame[0]).append(", \"time\": ").append(frame[1]).append('}')
                        .append(i + 1 < texture.frames().length ? ",\n" : "\n");
            }
            json.append("    ]\n  }\n}\n");
            Path meta = TEXTURES.resolve(name + ".png.mcmeta");
            Files.writeString(meta, json.toString(), StandardCharsets.UTF_8);
            System.out.println("wrote " + meta);
        }
        return image;
    }

    private static void contactSheet(List<BufferedImage> images) throws IOException {
        int width = SCALE;
        int height = 0;
        for (BufferedImage image : images) {
            width += (image.getWidth() + 1) * SCALE;
            height = Math.max(height, image.getHeight() * SCALE);
        }
        BufferedImage sheet = new BufferedImage(width, height + 2 * SCALE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                sheet.setRGB(x, y, 0xFF3C3C40);
            }
        }
        int left = SCALE;
        for (BufferedImage image : images) {
            for (int y = 0; y < image.getHeight() * SCALE; y++) {
                for (int x = 0; x < image.getWidth() * SCALE; x++) {
                    int argb = image.getRGB(x / SCALE, y / SCALE);
                    if (argb >>> 24 != 0) {
                        sheet.setRGB(left + x, SCALE + y, argb);
                    }
                }
            }
            left += (image.getWidth() + 1) * SCALE;
        }
        Path preview = Path.of("build/texture-preview/" + Golf5BlockTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
