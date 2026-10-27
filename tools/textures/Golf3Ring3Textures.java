import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Placeholder textures of phase 2 golf 3, stream R3 (design §6.4): the sulfur and dew crystal blocks, the fire ruby
 * inlaid pumice (side and top; stairs, slab and wall reuse them), the sulfur lamp, the pumice raft (block and item), the
 * ember berries and the dripshroom stew. Stream T replaces them. Our own pixels on our own textures (polished pumice and
 * pumice as bases); no vanilla pixels. Run from the repository root: {@code java tools/textures/Golf3Ring3Textures.java}.
 *
 * <p>Grids: one character per pixel from the palette, {@code '.'} keeps the base pixel (or stays transparent). Noise
 * fills use a fixed seed, so the output is reproducible.
 */
public final class Golf3Ring3Textures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");

    private Golf3Ring3Textures() {
    }

    public static void main(String[] args) throws IOException {
        // crumbly yellow sulfur, darker seams like a packed block
        write("block/sulfur_block", framed(noise(31, 0xC8B020, 0xE0C838, 0xEEDC5A, 0xF6EC86, 0xB09A18), 0x8A7A10));
        // pale blue crystal facets
        write("block/dew_crystal_block", framed(noise(32, 0x7CC4E8, 0x9AD6F2, 0xB8E6FA, 0xDAF4FF, 0x5EA8D4), 0x3C7EAE));
        // polished pumice with a red diamond inlay (side) and a ring of ruby (top)
        write("block/fire_ruby_inlaid_pumice", grid("block/polished_pumice",
                palette("R=B0141E", "r=E0303A", "h=FF7A6A", "d=5A0A10"),
                "................",
                "................",
                "................",
                "................",
                ".......dd.......",
                "......dRrd......",
                ".....dRrhrd.....",
                "....dRRrrhrd....",
                "....dRRRrrrd....",
                ".....dRRRrd.....",
                "......dRRd......",
                ".......dd.......",
                "................",
                "................",
                "................",
                "................"));
        write("block/fire_ruby_inlaid_pumice_top", grid("block/polished_pumice",
                palette("R=B0141E", "r=E0303A", "h=FF7A6A", "d=5A0A10"),
                "................",
                "................",
                "................",
                "......dddd......",
                "....ddRrrhdd....",
                "....dRd..drd....",
                "...dRd....drd...",
                "...dRd....drd...",
                "...dRd....dRd...",
                "...dRd....dRd...",
                "....dRd..dRd....",
                "....ddRRRRdd....",
                "......dddd......",
                "................",
                "................",
                "................"));
        // a glass frame around a glowing yellow-green sulfur core
        write("block/sulfur_lamp", grid(null,
                palette("G=C8D8D0", "g=98ACA4", "1=D8F040", "2=EAF870", "3=F8FFB0", "4=B4D028"),
                "GGGGGGGGGGGGGGGG",
                "Gg44444444444gGG",
                "G4111111111114gG",
                "G4122222222214gG",
                "G4123333333214gG",
                "G4123222223214gG",
                "G4123211123214gG",
                "G4123213123214gG",
                "G4123211123214gG",
                "G4123222223214gG",
                "G4123333333214gG",
                "G4122222222214gG",
                "G4111111111114gG",
                "Gg44444444444ggG",
                "GGgggggggggggggG",
                "GGGGGGGGGGGGGGGG"));
        // the raft's top: pumice with a darker rim
        write("block/pumice_raft", framed(read("block/pumice"), 0x6E6A66));
        // a flat grey plate seen from above
        write("item/pumice_raft", grid(null,
                palette("O=3C3A38", "1=8C8884", "2=B4B0AA", "3=D2CEC8"),
                "................",
                "................",
                "................",
                "................",
                "...OOOOOOOOOO...",
                "..O3323232232O..",
                ".O323121213223O.",
                ".O232112112132O.",
                ".O321211211223O.",
                ".O232121121232O.",
                "..O2111111112O..",
                "...OOOOOOOOOO...",
                "................",
                "................",
                "................",
                "................"));
        // three glowing orange-red berries on a charred twig
        write("item/ember_berries", grid(null,
                palette("O=3A0C04", "1=B02A08", "2=E8501A", "3=FF8C3A", "4=FFD27A", "t=2A2420", "T=4E443C"),
                "................",
                ".........tT.....",
                "........tT......",
                ".......tT..t....",
                "......OtOOtT....",
                ".....O3OO2OtO...",
                "....O342O32OO2O.",
                "....O3221O3423O.",
                "....O2211O3221O.",
                ".....O11OO2211O.",
                "......OOO3O11O..",
                ".......O342OO...",
                ".......O3221O...",
                ".......O2211O...",
                "........O11O....",
                ".........OO....."));
        // a wooden bowl with purple dripshroom stew
        write("item/dripshroom_stew", grid(null,
                palette("O=2A1608", "1=5E3A1A", "2=86562A", "3=A8743E", "p=5A2A6E", "P=7E4496", "q=A86CC0", "g=8EE070"),
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "...OOOOOOOOOO...",
                "..OqPPqPPPgPPO..",
                ".OPPqpPPgPPqPPO.",
                ".O3pPPPpPPPPp3O.",
                ".O3333333333332O",
                "..O3332222221O..",
                "..O2222222111O..",
                "...O22211111O...",
                "....OOOOOOOO....",
                "................"));
    }

    /** {@code "c=RRGGBB"} pairs. */
    private static Map<Character, Integer> palette(String... entries) {
        Map<Character, Integer> palette = new HashMap<>();
        for (String entry : entries) {
            palette.put(entry.charAt(0), Integer.parseInt(entry.substring(2), 16));
        }
        return palette;
    }

    /** Random pixels from {@code colours}, the first ones more often. */
    private static BufferedImage noise(long seed, int... colours) {
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int pick = Math.min(random.nextInt(colours.length), random.nextInt(colours.length));
                image.setRGB(x, y, 0xFF000000 | colours[pick]);
            }
        }
        return image;
    }

    /** A one-pixel rim in {@code rim} around {@code image}. */
    private static BufferedImage framed(BufferedImage image, int rim) {
        for (int i = 0; i < 16; i++) {
            image.setRGB(i, 0, 0xFF000000 | rim);
            image.setRGB(i, 15, 0xFF000000 | rim);
            image.setRGB(0, i, 0xFF000000 | rim);
            image.setRGB(15, i, 0xFF000000 | rim);
        }
        return image;
    }

    private static BufferedImage read(String name) throws IOException {
        BufferedImage source = ImageIO.read(TEXTURES.resolve(name + ".png").toFile());
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, 0xFF000000 | source.getRGB(x, y));
            }
        }
        return image;
    }

    private static BufferedImage grid(String base, Map<Character, Integer> palette, String... rows) throws IOException {
        if (rows.length != 16) {
            throw new IllegalStateException(rows.length + " rows");
        }
        BufferedImage image = base == null ? new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB) : read(base);
        for (int y = 0; y < 16; y++) {
            if (rows[y].length() != 16) {
                throw new IllegalStateException("row " + y + " has " + rows[y].length() + " pixels");
            }
            for (int x = 0; x < 16; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = palette.get(c);
                if (rgb == null) {
                    throw new IllegalStateException("'" + c + "' not in the palette");
                }
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        return image;
    }

    private static void write(String name, BufferedImage image) throws IOException {
        Path file = TEXTURES.resolve(name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("wrote " + file);
    }
}
