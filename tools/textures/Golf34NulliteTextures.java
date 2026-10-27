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
 * Generates the nullite upgrade smithing template of phase 2 golf 3 + 4 (stream T), in the style of the sunstone
 * template; the nullite gear itself follows with its golf. Our own pixels, no vanilla pixels read or copied.
 * Run from the repository root: {@code java tools/textures/Golf34NulliteTextures.java}.
 *
 * <p>Palette: voidstone tablet {@code a}-{@code e} (outline to lit rim), void ring {@code 2}-{@code 5} (the nullite
 * purple, dark to light), black core {@code 1}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 64 x 32 for armor
 * layers), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf34NulliteTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf34NulliteTextures() {
    }

    public static void main(String[] args) throws IOException {
        // the sunstone tablet in voidstone: a purple void ring around a black core, lit at the top right
        add("item/nullite_upgrade_smithing_template", null,
                palette("a=07040C", "b=18111F", "c=241A30", "d=30243E", "e=3E304E", "1=0B0612", "2=5E2E92", "3=8A4FD8", "4=B488F0", "5=E6D6FF"),
                "................",
                "....aaaaaaaaa...",
                "...aeeeeeeeeca..",
                "..aeddddddddcba.",
                "..aedd3445ddcba.",
                "..aed3dddd5dcba.",
                "..ae3dddddd4cba.",
                "..ae2dd11dd4cba.",
                "..ae2dd11dd3cba.",
                "..ae2dddddd3cba.",
                "..aed2dddd3dcba.",
                "..aedd2233ddcba.",
                "..adccccccccbba.",
                "...abbbbbbbbba..",
                "....aaaaaaaaa...",
                "................");
        List<BufferedImage> written = new ArrayList<>();
        for (Texture texture : TEXTURES_TO_WRITE) {
            written.add(write(texture));
        }
        contactSheet(written);
    }

    private static void add(String name, String base, Map<Character, Integer> palette, String... rows) {
        TEXTURES_TO_WRITE.add(new Texture(name, base, palette, rows));
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
        int height = texture.rows().length;
        int width = texture.rows()[0].length();
        boolean icon = texture.name().startsWith("mob_effect/");
        boolean layer = texture.name().startsWith("models/armor/");
        int wantWidth = layer ? 64 : icon ? 18 : 16;
        int wantHeight = layer ? 32 : icon ? 18 : 16;
        if (width != wantWidth || height != wantHeight) {
            throw new IllegalStateException(texture.name() + ": " + width + " x " + height);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (texture.base() != null) {
            BufferedImage base = ImageIO.read(TEXTURES.resolve(texture.base() + ".png").toFile());
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, 0xFF000000 | base.getRGB(x, y));
                }
            }
        }
        for (int y = 0; y < height; y++) {
            String row = texture.rows()[y];
            if (row.length() != width) {
                throw new IllegalStateException(texture.name() + ": row " + y + " has " + row.length() + " pixels");
            }
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = texture.palette().get(c);
                if (rgb == null) {
                    throw new IllegalStateException(texture.name() + ": '" + c + "' not in the palette");
                }
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) {
                    throw new IllegalStateException(texture.name() + ": alpha " + alpha + " at " + x + "," + y);
                }
            }
        }
        Path file = TEXTURES.resolve(texture.name() + ".png");
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
        Path preview = Path.of("build/texture-preview/" + Golf34NulliteTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
