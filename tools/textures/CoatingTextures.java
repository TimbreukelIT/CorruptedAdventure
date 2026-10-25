import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the coating textures (phase 2 golf 1, stream C) from our own pixels: no vanilla pixels are read or copied.
 * Run from the repository root: {@code java tools/textures/CoatingTextures.java}.
 *
 * <ul>
 *   <li>{@code trims/color_palettes/coating_palette.png}: our palette key, 8 greys from light to dark. The atlas source
 *       ({@code CoatingAtlasProvider}) swaps exactly these greys for a material's colours, so every pattern pixel uses
 *       one of them.</li>
 *   <li>{@code trims/color_palettes/<material>.png}: 8 colours per coating material, light to dark like the key.</li>
 *   <li>{@code trims/models/armor/<coating>.png} (helmet, chestplate, boots: the 64×32 armor layer 1) and
 *       {@code <coating>_leggings.png} (layer 2): a band per armor piece in the coating's own style, and the coating's
 *       emblem on the chest.</li>
 *   <li>{@code item/<coating>_smithing_template.png}: a stone tablet with the emblem in the material colour.</li>
 * </ul>
 * Deterministic (no randomness). Every texture is checked before it is written (size, alpha 0/255 only, pattern pixels
 * only in key greys). A preview with every pattern in its material's colours goes to
 * {@code build/texture-preview/coatings.png}.
 */
public final class CoatingTextures {
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final Path PREVIEW = Path.of("build/texture-preview/coatings.png");
    private static final int TRANSPARENT = 0x00000000;

    /** The palette key: index 0 lightest, 7 darkest. */
    private static final int[] KEY = {0xF2F2F2, 0xDADADA, 0xC2C2C2, 0xAAAAAA, 0x929292, 0x7A7A7A, 0x626262, 0x4A4A4A};

    /** Material → base colour (RGB); the palette runs from a light tint to a deep shade of it. */
    private static final Map<String, Integer> MATERIALS = new LinkedHashMap<>();

    static {
        MATERIALS.put("turquoise", 0x3FAF9F);
        MATERIALS.put("dew_crystal", 0x7FB8CC);
        MATERIALS.put("fire_crystal", 0xD06A22);
        MATERIALS.put("primordial_ice", 0xA6CCEA);
        MATERIALS.put("white_core", 0xD8D6E6);
    }

    /** Coating → its material, band style and emblem. */
    private record Coating(String name, String material, Band band, String[] emblem) {
    }

    private interface Band {
        /** Key index (0-7) or -1 for nothing, at column x of a band of 3 rows (row 0 top). */
        int at(int x, int row);
    }

    private static final List<Coating> COATINGS = List.of(
            new Coating("dehydration_coating", "turquoise",
                    // A sand wave: the light crest climbs and falls every 6 pixels.
                    (x, row) -> row == wave(x) ? 1 : row == 1 ? 4 : 5,
                    new String[]{
                            "...#....",
                            "...#....",
                            "..+++...",
                            "##+++##.",
                            "..+++...",
                            "...#....",
                            "...#....",
                            "........"}),
            new Coating("heat_coating", "dew_crystal",
                    // Flame teeth: the top row only every other pixel pair, the rest a glowing strip.
                    (x, row) -> row == 0 ? ((x / 2) % 2 == 0 ? 2 : -1) : row == 1 ? 1 : 4,
                    new String[]{
                            "...#....",
                            "..##....",
                            "..#+#...",
                            ".#++#.#.",
                            ".#+++##.",
                            "#++#++#.",
                            "#+###+#.",
                            ".#####.."}),
            new Coating("cold_coating", "fire_crystal",
                    // Crystals: a strip with a light diamond every 4 pixels.
                    (x, row) -> {
                        int phase = x % 4;
                        if (row == 1) {
                            return phase == 1 ? 0 : 3;
                        }
                        return phase == 1 ? 2 : 6;
                    },
                    new String[]{
                            "#..#..#.",
                            ".#.#.#..",
                            "..#+#...",
                            "###+###.",
                            "..#+#...",
                            ".#.#.#..",
                            "#..#..#.",
                            "........"}),
            new Coating("poison_coating", "primordial_ice",
                    // A strip with drips hanging from it every 5 pixels.
                    (x, row) -> row == 0 ? 2 : row == 1 ? 4 : (x % 5 == 2 ? 3 : -1),
                    new String[]{
                            "...#....",
                            "..#+#...",
                            "..#+#...",
                            ".#+++#..",
                            ".#+++#..",
                            ".#+++#..",
                            "..###...",
                            "........"}),
            new Coating("void_coating", "white_core",
                    // A broken strip: segments of 3 with a gap, the gap one row lower (a glitch).
                    (x, row) -> {
                        boolean gap = x % 4 == 3;
                        if (gap) {
                            return row == 2 ? 6 : -1;
                        }
                        return row == 0 ? 2 : row == 1 ? 3 : -1;
                    },
                    new String[]{
                            "###.###.",
                            "#.....#.",
                            "#.+.+.#.",
                            "......#.",
                            "#.+.+...",
                            "#.....#.",
                            "###.###.",
                            "........"}),
            new Coating("silence_coating", "white_core",
                    // Two thin lines with nothing between: a hush.
                    (x, row) -> row == 0 ? 1 : row == 2 ? 5 : -1,
                    new String[]{
                            "..###...",
                            ".#...#..",
                            "#..+..#.",
                            "#.+++.#.",
                            "#..+..#.",
                            ".#...#..",
                            "..###...",
                            "........"}));

    /** The tablet of the template items: light rim top left, dark rim bottom right (RGB). */
    private static final int[] TABLET = {0x5E6470, 0x464B56, 0x353943, 0x24272F, 0x16181E};

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        if (!Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the repository root (src/main/resources not found)");
        }
        Map<String, BufferedImage> written = new LinkedHashMap<>();
        written.put("trims/color_palettes/coating_palette", palette(KEY));
        for (Map.Entry<String, Integer> material : MATERIALS.entrySet()) {
            written.put("trims/color_palettes/" + material.getKey(), palette(materialPalette(material.getValue())));
        }
        for (Coating coating : COATINGS) {
            written.put("trims/models/armor/" + coating.name(), checkedPattern(layer1(coating)));
            written.put("trims/models/armor/" + coating.name() + "_leggings", checkedPattern(layer2(coating)));
            written.put("item/" + coating.name() + "_smithing_template", template(coating));
        }
        for (Map.Entry<String, BufferedImage> entry : written.entrySet()) {
            checkAlpha(entry.getKey(), entry.getValue());
            Path file = OUTPUT.resolve(entry.getKey() + ".png");
            Files.createDirectories(file.getParent());
            ImageIO.write(entry.getValue(), "png", file.toFile());
            System.out.println("wrote " + file);
        }
        writePreview(written);
    }

    // ---- palettes ----

    private static BufferedImage palette(int[] colors) {
        BufferedImage image = new BufferedImage(colors.length, 1, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < colors.length; i++) {
            image.setRGB(i, 0, 0xFF000000 | colors[i]);
        }
        return image;
    }

    /** 8 shades: index 0 halfway to white, 3 the base, 7 at 35 % brightness. */
    static int[] materialPalette(int base) {
        int[] colors = new int[KEY.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = i <= 3 ? mix(base, 0xFFFFFF, (3 - i) / 6.0) : mix(base, 0x000000, (i - 3) * 0.1625);
        }
        return colors;
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ---- armor patterns (64 × 32, the humanoid armor layout) ----

    /** Helmet (brow band), chestplate (chest band, shoulder bands, emblem) and boots (ankle band). */
    private static BufferedImage layer1(Coating coating) {
        BufferedImage image = blank(64, 32);
        band(image, coating.band(), 0, 32, 9);     // head: all four sides, at the brow
        band(image, coating.band(), 16, 40, 28);   // body: around the belly, below the emblem
        band(image, coating.band(), 40, 56, 20);   // arms: around the shoulders
        band(image, coating.band(), 0, 16, 28);    // legs (the boots of layer 1): around the ankles
        emblem(image, coating.emblem(), 20, 20);   // chest front (8 × 12 from 20,20), rows 20-27
        return image;
    }

    /** Leggings: a waist band and a stripe down the front of each leg. */
    private static BufferedImage layer2(Coating coating) {
        BufferedImage image = blank(64, 32);
        band(image, coating.band(), 16, 40, 29);   // body: the waist, just above the legs
        band(image, coating.band(), 0, 16, 23);    // legs: around the thighs
        for (int y = 26; y < 32; y++) {            // leg front (4 wide from 4,20): a stripe down the shin
            set(image, 5, y, y % 2 == 0 ? 2 : 4);
            set(image, 6, y, 5);
        }
        return image;
    }

    private static void band(BufferedImage image, Band band, int x0, int x1, int y) {
        for (int x = x0; x < x1; x++) {
            for (int row = 0; row < 3; row++) {
                int index = band.at(x - x0, row);
                if (index >= 0) {
                    set(image, x, y + row, index);
                }
            }
        }
    }

    private static void emblem(BufferedImage image, String[] emblem, int x0, int y0) {
        for (int y = 0; y < emblem.length; y++) {
            for (int x = 0; x < emblem[y].length(); x++) {
                char c = emblem[y].charAt(x);
                if (c != '.') {
                    set(image, x0 + x, y0 + y, c == '+' ? 1 : 3);
                }
            }
        }
    }

    private static int wave(int x) {
        int phase = x % 6;
        return phase < 2 ? 0 : phase < 3 ? 1 : phase < 5 ? 2 : 1;
    }

    private static void set(BufferedImage image, int x, int y, int keyIndex) {
        image.setRGB(x, y, 0xFF000000 | KEY[keyIndex]);
    }

    private static BufferedImage checkedPattern(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) != 0 && indexOf(argb & 0xFFFFFF) < 0) {
                    throw new IllegalStateException("pattern pixel " + x + "," + y + " is not a key grey");
                }
            }
        }
        return image;
    }

    private static int indexOf(int rgb) {
        for (int i = 0; i < KEY.length; i++) {
            if (KEY[i] == rgb) {
                return i;
            }
        }
        return -1;
    }

    // ---- template items (16 × 16) ----

    private static BufferedImage template(Coating coating) {
        BufferedImage image = blank(16, 16);
        int[] material = materialPalette(MATERIALS.get(coating.material()));
        // The tablet: x 2-13, y 1-14, corners cut; rim light top/left, dark bottom/right, face in between.
        for (int y = 1; y <= 14; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean corner = (x == 2 || x == 13) && (y == 1 || y == 14);
                if (corner) {
                    continue;
                }
                int shade;
                if (x == 2 || y == 1) {
                    shade = x == 13 || y == 14 ? 3 : 0;
                } else if (x == 13 || y == 14) {
                    shade = 4;
                } else if (x == 3 || y == 2) {
                    shade = 1;
                } else if (x == 12 || y == 13) {
                    shade = 3;
                } else {
                    shade = (x + y) % 5 == 0 ? 1 : 2;
                }
                image.setRGB(x, y, 0xFF000000 | TABLET[shade]);
            }
        }
        // The emblem, engraved: material colours, with a dark notch below-right of every pixel.
        String[] emblem = coating.emblem();
        int x0 = 4;
        int y0 = 4;
        for (int y = 0; y < emblem.length; y++) {
            for (int x = 0; x < emblem[y].length(); x++) {
                char c = emblem[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                int px = x0 + x;
                int py = y0 + y;
                if (px + 1 <= 12 && py + 1 <= 13 && emblemAt(emblem, x + 1, y + 1) == '.') {
                    image.setRGB(px + 1, py + 1, 0xFF000000 | TABLET[4]);
                }
            }
        }
        for (int y = 0; y < emblem.length; y++) {
            for (int x = 0; x < emblem[y].length(); x++) {
                char c = emblem[y].charAt(x);
                if (c != '.') {
                    image.setRGB(x0 + x, y0 + y, 0xFF000000 | material[c == '+' ? 0 : 3]);
                }
            }
        }
        return image;
    }

    private static char emblemAt(String[] emblem, int x, int y) {
        return y < emblem.length && x < emblem[y].length() ? emblem[y].charAt(x) : '.';
    }

    // ---- checks and preview ----

    private static BufferedImage blank(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, TRANSPARENT);
            }
        }
        return image;
    }

    private static void checkAlpha(String name, BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    throw new IllegalStateException(name + ": alpha " + alpha + " at " + x + "," + y);
                }
            }
        }
    }

    /** Per coating: layer 1 and 2 in the material's colours at 4×, over a mid grey, and the template at 8×. */
    private static void writePreview(Map<String, BufferedImage> written) throws IOException {
        int scale = 4;
        int rowHeight = 32 * scale + 8;
        BufferedImage preview = new BufferedImage(64 * scale * 2 + 16 * 8 + 32, rowHeight * COATINGS.size(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new java.awt.Color(0x6B6B6B));
        g.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        for (int i = 0; i < COATINGS.size(); i++) {
            Coating coating = COATINGS.get(i);
            int[] colors = materialPalette(MATERIALS.get(coating.material()));
            int y = i * rowHeight;
            draw(g, recolor(written.get("trims/models/armor/" + coating.name()), colors), 0, y, scale);
            draw(g, recolor(written.get("trims/models/armor/" + coating.name() + "_leggings"), colors),
                    64 * scale + 8, y, scale);
            draw(g, written.get("item/" + coating.name() + "_smithing_template"), 64 * scale * 2 + 24, y, 8);
        }
        g.dispose();
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(preview, "png", PREVIEW.toFile());
        System.out.println("wrote " + PREVIEW);
    }

    private static BufferedImage recolor(BufferedImage pattern, int[] colors) {
        BufferedImage out = blank(pattern.getWidth(), pattern.getHeight());
        for (int y = 0; y < pattern.getHeight(); y++) {
            for (int x = 0; x < pattern.getWidth(); x++) {
                int argb = pattern.getRGB(x, y);
                if ((argb >>> 24) != 0) {
                    out.setRGB(x, y, 0xFF000000 | colors[indexOf(argb & 0xFFFFFF)]);
                }
            }
        }
        return out;
    }

    private static void draw(Graphics2D g, BufferedImage image, int x, int y, int scale) {
        g.drawImage(image, x, y, image.getWidth() * scale, image.getHeight() * scale, null);
    }
}
