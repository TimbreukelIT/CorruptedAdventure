import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates the item textures of the upgrade smithing templates (phase 2 wave 1, stream B) from our own pixels: no
 * vanilla pixels are read or copied. Run from the repository root: {@code java tools/textures/Golf1TemplateTextures.java}.
 *
 * <p>Placeholders: a stone tablet in the rock of the tier's ring with the tier's motif inlaid in its material colours
 * (sunstone a sun, volcanite a flame, tidesilver waves, nullite a void ring). Deterministic (fixed seed per file name);
 * every texture is checked (16×16, alpha 0/255 only). Textures in {@link #HAND_EDITED} are never overwritten. A contact
 * sheet (8×) goes to {@code build/texture-preview/golf1-templates.png}.
 */
public final class Golf1TemplateTextures {
    private static final int SIZE = 16;
    private static final int SCALE = 8;
    private static final Path OUTPUT = Path.of("src/main/resources/assets/corruptedadventure/textures/item");
    private static final Path PREVIEW = Path.of("build/texture-preview/golf1-templates.png");

    /** Retouched by hand after generation; the script skips these. */
    private static final Set<String> HAND_EDITED = Set.of();

    /** The tablet: '#' outline, 'o' body, '.' transparent. */
    private static final String[] TABLET = {
            "................",
            "....########....",
            "...#oooooooo#...",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "...#oooooooo#...",
            "....########....",
            "................"};
    /** Where the 8×8 motif sits on the tablet. */
    private static final int MOTIF_X = 4;
    private static final int MOTIF_Y = 4;

    private record Template(int[] rock, int[] inlay, String[] motif) {
    }

    private static final Map<String, Template> TEMPLATES = new LinkedHashMap<>();

    static {
        // Rock dark to light (the first is the outline), inlay dark to light (index 1..5 in the motif).
        TEMPLATES.put("sunstone_upgrade_smithing_template", new Template(
                new int[]{0x2A1810, 0x4E3222, 0x6A4630, 0x845A3E, 0x9C6E4E},
                new int[]{0x5A1A06, 0x9A300C, 0xD95A16, 0xF2862A, 0xFFC878},
                new String[]{
                        "..4..4..",
                        "...33...",
                        "4.3553.4",
                        ".355553.",
                        ".355553.",
                        "4.3553.4",
                        "...33...",
                        "..4..4.."}));
        TEMPLATES.put("volcanite_upgrade_smithing_template", new Template(
                new int[]{0x141012, 0x2A2427, 0x3B3437, 0x4C4447, 0x5E5558},
                new int[]{0x4A0C06, 0x8A1E0E, 0xC23A14, 0xE8641E, 0xFFAA50},
                new String[]{
                        "...4....",
                        "...34...",
                        "..344...",
                        "..3453..",
                        ".34553..",
                        ".345543.",
                        ".355553.",
                        "..2332.."}));
        TEMPLATES.put("tidesilver_upgrade_smithing_template", new Template(
                new int[]{0x16242A, 0x2C4650, 0x3C5C66, 0x4E727C, 0x628890},
                new int[]{0x3A5A64, 0x6A929A, 0x9CC2C8, 0xCAE8EC, 0xF2FFFF},
                new String[]{
                        "........",
                        ".55.....",
                        "4..5..55",
                        "....44..",
                        "........",
                        ".44.....",
                        "3..4..44",
                        "....33.."}));
        TEMPLATES.put("nullite_upgrade_smithing_template", new Template(
                new int[]{0x07040C, 0x18111F, 0x241A30, 0x30243E, 0x3E304E},
                new int[]{0x3A1A5E, 0x5E2E92, 0x8A4FD8, 0xB488F0, 0xE6D6FF},
                new String[]{
                        "..3333..",
                        ".3....4.",
                        "3..55..4",
                        "3.5..5.4",
                        "3.5..5.4",
                        "3..55..4",
                        ".3....4.",
                        "..3344.."}));
    }

    private Golf1TemplateTextures() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        if (!Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the repository root (src/main/resources not found)");
        }
        Files.createDirectories(OUTPUT);
        BufferedImage sheet = new BufferedImage(TEMPLATES.size() * (SIZE + 2) * SCALE, (SIZE + 2) * SCALE,
                BufferedImage.TYPE_INT_ARGB);
        int column = 0;
        for (Map.Entry<String, Template> entry : TEMPLATES.entrySet()) {
            String name = entry.getKey();
            Path file = OUTPUT.resolve(name + ".png");
            BufferedImage image;
            if (HAND_EDITED.contains(name)) {
                System.out.println("skipped (hand-edited) " + file);
                image = ImageIO.read(file.toFile());
            } else {
                image = paint(name, entry.getValue());
                check(name, image);
                ImageIO.write(image, "png", file.toFile());
                System.out.println("wrote " + file);
            }
            for (int y = 0; y < SIZE * SCALE; y++) {
                for (int x = 0; x < SIZE * SCALE; x++) {
                    sheet.setRGB(column * (SIZE + 2) * SCALE + SCALE + x, SCALE + y, image.getRGB(x / SCALE, y / SCALE));
                }
            }
            column++;
        }
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet, "png", PREVIEW.toFile());
        System.out.println("wrote " + PREVIEW);
    }

    private static BufferedImage paint(String name, Template template) {
        Random random = new Random(name.hashCode());
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] rock = template.rock();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                char c = TABLET[y].charAt(x);
                if (c == '#') {
                    image.setRGB(x, y, opaque(rock[0]));
                } else if (c == 'o') {
                    // Lit from the top left: lighter near the upper and left rim, a speck of grain either way.
                    int tone = (x + y) < 10 ? 3 : (x + y) > 20 ? 1 : 2;
                    tone = Math.max(1, Math.min(rock.length - 1, tone + random.nextInt(3) - 1));
                    if (TABLET[y - 1].charAt(x) == '#' || TABLET[y].charAt(x - 1) == '#') {
                        tone = rock.length - 1;
                    }
                    image.setRGB(x, y, opaque(rock[tone]));
                }
            }
        }
        String[] motif = template.motif();
        for (int y = 0; y < motif.length; y++) {
            for (int x = 0; x < motif[y].length(); x++) {
                char c = motif[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                image.setRGB(MOTIF_X + x, MOTIF_Y + y, opaque(template.inlay()[c - '1']));
                // The inlay sits in a groove: a shadow on the rock below and to the right of it.
                boolean belowFree = y + 1 >= motif.length || x + 1 >= motif[y + 1].length() || motif[y + 1].charAt(x + 1) == '.';
                if (belowFree && TABLET[MOTIF_Y + y + 1].charAt(MOTIF_X + x + 1) == 'o') {
                    image.setRGB(MOTIF_X + x + 1, MOTIF_Y + y + 1, opaque(rock[0]));
                }
            }
        }
        // Repaint the motif over shadows that fell on its own pixels.
        for (int y = 0; y < motif.length; y++) {
            for (int x = 0; x < motif[y].length(); x++) {
                char c = motif[y].charAt(x);
                if (c != '.') {
                    image.setRGB(MOTIF_X + x, MOTIF_Y + y, opaque(template.inlay()[c - '1']));
                }
            }
        }
        return image;
    }

    private static void check(String name, BufferedImage image) {
        if (image.getWidth() != SIZE || image.getHeight() != SIZE) {
            throw new IllegalStateException(name + ": " + image.getWidth() + "×" + image.getHeight());
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    throw new IllegalStateException(name + ": alpha " + alpha + " at " + x + "," + y);
                }
            }
        }
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }
}
