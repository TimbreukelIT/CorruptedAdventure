import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the textures of the weakness items of phase 4 (stream I): the sunstone flash, the volcanite horn and the
 * {@code dazzled} and {@code stunned} effect icons. Our own pixels in the palettes of the ring 2/3 materials (sunstone
 * amber gold and turquoise; goat horn bone, volcanite basalt with glowing veins, fire ruby).
 * Run from the repository root: {@code java tools/textures/Phase4WeaknessTextures.java}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent. Edit the
 * grids by hand and run the script again; the output is byte-for-byte reproducible. Checked before writing: size (16 x
 * 16 for items, 18 x 18 for effect icons) and every character in the palette. Contact sheet (8x) in
 * {@code build/texture-preview/}.
 */
public final class Phase4WeaknessTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Phase4WeaknessTextures() {
    }

    public static void main(String[] args) throws IOException {
        // a riveted sunstone mirror, cracked on its dark side, the glass blazing; turquoise-wrapped grip, amber pommel
        add("item/sunstone_flash",
                palette("O=3D1606", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "7=FFEDB0", "y=FFE08A", "g=FFF3C4",
                        "w=FFFFFF", "a=E0A848", "k=0E3B3A", "t=1C6B66", "T=2E9C92", "u=4FC6B5"),
                ".......OOOOO....",
                ".....OO44744O...",
                ".....O5yggyy3O..",
                "....O5ygwwgya3O.",
                "....O4gwwwgy23O.",
                "....O7gwwgy2a5O.",
                "....O4ygwgy2a3O.",
                "....O5yygyaa22O.",
                "....OO4yyya232O.",
                "...O43O33322OO..",
                "...OTuOOOOOO....",
                "..OtTO..........",
                ".OuTO...........",
                "OtkO............",
                "O54O............",
                ".OO.............");
        // a ridged goat horn bound with two volcanite bands (glowing veins), the bell open at the top, a fire ruby
        // mouthpiece on the curled tip
        add("item/volcanite_horn",
                palette("O=1E1712", "1=4A3E30", "2=6E5D48", "3=8F7C62", "4=B09C7E", "5=CDBB9A", "6=E2D4B6", "d=140E0C",
                        "b=5C4C3A", "v=30282B", "V=554A4D", "t=E0501A", "u=FF8A2C", "U=FFC860", "r=82121E", "R=D5353F"),
                ".........OOOOO..",
                "........O5bddbO.",
                "........O6bdddbO",
                ".......O66bddb4O",
                ".......O6544bb3O",
                ".......O542232O.",
                ".......OVvtuUtO.",
                "......OVtuvvtO..",
                "......O654432O..",
                ".O...O654223O...",
                "ORO.OVtuUtvO....",
                "OrO6544332O.....",
                "Or543322O.......",
                ".O32211O........",
                "..OOOOO.........",
                "................");
        // a blinding sun: white core, amber rays, the colour of the effect (18 x 18)
        add("mob_effect/dazzled",
                palette("O=5A3A0C", "5=F2B13C", "6=FFD25E", "7=FFEDB0", "w=FFFFFF"),
                "........OO........",
                ".......O55O.......",
                ".......O55O.......",
                "....O..O66O..O....",
                "...O5O..OO..O5O...",
                "....O6OOOOOO6O....",
                ".....OO7777OO.....",
                ".OOO.O7wwww7O.OOO.",
                "O556OO7wwww7OO655O",
                "O556OO7wwww7OO655O",
                ".OOO.O7wwww7O.OOO.",
                ".....OO7777OO.....",
                "....O6OOOOOO6O....",
                "...O5O..OO..O5O...",
                "....O..O66O..O....",
                ".......O55O.......",
                ".......O55O.......",
                "........OO........");
        // an ash stone cracked with embers inside the shock of a horn blast, the colour of the effect (18 x 18)
        add("mob_effect/stunned",
                palette("O=1E1A18", "2=5A504A", "3=7A6E66", "4=9A8C84", "5=C4B8AE", "t=E0501A", "u=FF8A2C"),
                "..................",
                "..................",
                "..................",
                "..................",
                "...t..........t...",
                "..4.....OO.....3..",
                ".4...5.O5tO.5...3.",
                ".4..5.O55u4O.5..3.",
                ".4..5.O5t43O.5..3.",
                ".4..5.O4u32O.5..3.",
                ".4..5.O43t2O.5..3.",
                ".4...5.O22O.5...3.",
                "..4.....OO.....3..",
                "...t..........t...",
                "..................",
                "..................",
                "..................",
                "..................");

        List<BufferedImage> written = new ArrayList<>();
        for (Texture texture : TEXTURES_TO_WRITE) {
            written.add(write(texture));
        }
        contactSheet(written);
    }

    private static void add(String name, Map<Character, Integer> palette, String... rows) {
        TEXTURES_TO_WRITE.add(new Texture(name, palette, rows));
    }

    /** {@code "c=RRGGBB"} pairs. */
    private static Map<Character, Integer> palette(String... entries) {
        Map<Character, Integer> palette = new HashMap<>();
        for (String entry : entries) {
            palette.put(entry.charAt(0), Integer.parseInt(entry.substring(2), 16));
        }
        return palette;
    }

    private static BufferedImage write(Texture texture) throws IOException {
        String name = texture.name();
        int height = texture.rows().length;
        int width = texture.rows()[0].length();
        int want = name.startsWith("mob_effect/") ? 18 : 16;
        if (width != want || height != want) {
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
        Path preview = Path.of("build/texture-preview/" + Phase4WeaknessTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
