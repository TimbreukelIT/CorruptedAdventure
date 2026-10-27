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
 * Generates the sunstone gear textures of phase 2 golf 2 (stream T, design §5.6): ingot, the five tools, the four armor
 * items, the upgrade smithing template and the two armor layers. Our own pixels, no vanilla pixels read or copied.
 * Run from the repository root: {@code java tools/textures/Golf2SunstoneTextures.java}.
 *
 * <p>Palette amber gold (approved 2026-10-27): copper outline {@code O}, shadow {@code 1}-{@code 2}, amber
 * {@code 3}-{@code 4}, gold {@code 5}, sun yellow {@code 6}, glint {@code 7}; leather grip {@code h}-{@code k}; template
 * rock {@code a}-{@code e}. Warmer and redder than vanilla gold, matching the sunstone ore; no emissive.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 64 x 32 for armor
 * layers), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf2SunstoneTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf2SunstoneTextures() {
    }

    public static void main(String[] args) throws IOException {
        // a cast bar seen from the front-top: light top face, bright front edge, small sun stamp
        add("item/sunstone_ingot", null,
                palette("O=3D1606", "1=6B2A0C", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0"),
                "................",
                "................",
                "................",
                "................",
                "....OOOOOOOO....",
                "...O76666665O...",
                "..O6666666654O..",
                ".O477777777663O.",
                "O45555555555443O",
                "O44445544444332O",
                "O33332233333221O",
                "O22222222222211O",
                "OOOOOOOOOOOOOOOO",
                "................",
                "................",
                "................");
        // amber blade with glints, short guard with a sun-yellow stone, leather grip, amber pommel
        add("item/sunstone_sword", null,
                palette("O=3D1606", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0", "i=45291A", "j=684226", "k=8A5E36"),
                ".............OO.",
                "............O65O",
                "...........O753O",
                "..........O653O.",
                "....O....O653O..",
                "...O3O..O653O...",
                "...O24OO7532O...",
                "....O246532O....",
                ".....O6732O.....",
                ".....Oi54O......",
                "....OkjO24O.....",
                "...OkiO.O23O....",
                ".OOijO...OO.....",
                "O64jO...........",
                "O42O............",
                ".OO.............");
        // a broad blade to the upper left, poll behind the grip
        add("item/sunstone_axe", null,
                palette("O=3D1606", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0", "i=45291A", "j=684226", "k=8A5E36"),
                "........OO......",
                ".......O66O.....",
                "......O7655OOO..",
                ".....O765443iiO.",
                "....O765432kj2O.",
                "....O65432kjOO..",
                ".....O32OiiO....",
                "......OOkjO.....",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OkjO............",
                ".OO.............");
        // two-pixel arc over the top of the grip, lit on the outside
        add("item/sunstone_pickaxe", null,
                palette("O=3D1606", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0", "i=45291A", "j=684226", "k=8A5E36"),
                "................",
                ".....OOOOOOOO...",
                "....O76666554O..",
                "...O3555544432O.",
                "....OOOOOOOkj32O",
                ".........OkjO42O",
                "........OiiOO32O",
                ".......OkjO.O32O",
                "......OkjO..O32O",
                ".....OiiO...O32O",
                "....OkjO.....O2O",
                "...OkjO.......O.",
                "..OiiO..........",
                ".OkjO...........",
                "OkjO............",
                ".OO.............");
        // a spade along the grip's diagonal, shaded across it
        add("item/sunstone_shovel", null,
                palette("O=3D1606", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0", "i=45291A", "j=684226", "k=8A5E36"),
                "............OO..",
                "..........OO76O.",
                ".........O77654O",
                ".........O66544O",
                "........O665443O",
                "........O65443O.",
                "........O5443O..",
                ".......OkjOOO...",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OkjO............",
                ".OO.............");
        // a bar across the top of the grip, blade hooked down on the right
        add("item/sunstone_hoe", null,
                palette("O=3D1606", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0", "i=45291A", "j=684226", "k=8A5E36"),
                "........OO......",
                ".......O66OO.OO.",
                "......O76554O32O",
                ".......O5543322O",
                "........OO4332O.",
                ".........Ok32O..",
                "........OiiOO...",
                ".......OkjO.....",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OkjO............",
                ".OO.............");
        // crest highlight, copper brow band
        add("item/sunstone_helmet", null,
                palette("O=3D1606", "1=6B2A0C", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0"),
                "................",
                "................",
                "......OOOO......",
                "....OO7665OO....",
                "...O66655443O...",
                "..O6554664332O..",
                "..O5543773322O..",
                "..O4222222221O..",
                "..O43OOOOOO32O..",
                "..O32O....O21O..",
                "..O21O....O11O..",
                "...OO......OO...",
                "................",
                "................",
                "................",
                "................");
        // sun emblem in a copper ring on the chest, copper belt trim
        add("item/sunstone_chestplate", null,
                palette("O=3D1606", "1=6B2A0C", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0"),
                "................",
                "................",
                "..OOO......OOO..",
                ".O766O....O654O.",
                ".O6654OOOO5432O.",
                ".O655421124321O.",
                ".O554433333321O.",
                ".O544441133322O.",
                "..OO44176132OO..",
                "...O44165132O...",
                "...O43311322O...",
                "...O44333222O...",
                "...O22222221O...",
                "....O111111O....",
                ".....OOOOOO.....",
                "................");
        // copper belt, light front ridges
        add("item/sunstone_leggings", null,
                palette("O=3D1606", "1=6B2A0C", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0"),
                "................",
                "................",
                "...OOOOOOOOOO...",
                "..O2222222221O..",
                "..O6554674332O..",
                "..O5544554332O..",
                "..O5443OO4332O..",
                "..O543O..O432O..",
                "..O543O..O432O..",
                "..O443O..O332O..",
                "..O433O..O322O..",
                "..O322O..O221O..",
                "..O211O..O111O..",
                "...OOO....OOO...",
                "................",
                "................");
        // light cuffs, copper soles
        add("item/sunstone_boots", null,
                palette("O=3D1606", "1=6B2A0C", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0"),
                "................",
                "................",
                "................",
                "...OOO....OOO...",
                "..O765O..O654O..",
                "..O544O..O443O..",
                "..O443O..O433O..",
                "..O433O..O332O..",
                "..O4332O.O4332OO",
                ".O654332OO654332",
                ".O5443322O544332",
                ".O1111111O111111",
                "..OOOOOOO.OOOOOO",
                "................",
                "................",
                "................");
        // the golf 1 tablet in sandstone-brown rock with an inlaid sun (rays, lit disc)
        add("item/sunstone_upgrade_smithing_template", null,
                palette("3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0", "a=2A1810", "b=4E3222", "c=6A4630", "d=845A3E", "e=9C6E4E"),
                "................",
                "....aaaaaaaaa...",
                "...aeeeeeeeeca..",
                "..aeddddddddcba.",
                "..ae4dd44dd4cba.",
                "..aed5d55d5dcba.",
                "..aedd6776ddcba.",
                "..ae45677654cba.",
                "..ae45676654cba.",
                "..aedd6665ddcba.",
                "..aed4d44d4dcba.",
                "..ae3dd33dd3cba.",
                "..adccccccccbba.",
                "...abbbbbbbbba..",
                "....aaaaaaaaa...",
                "................");
        // helmet (crest, brow band with a sun stone), chestplate (sun emblem, back ridge, belt), pauldrons, boots
        add("models/armor/sunstone_layer_1", null,
                palette("1=6B2A0C", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0"),
                "........44444444................................................",
                "........55466555................................................",
                "........55567555................................................",
                "........65567555................................................",
                "........54566555................................................",
                "........34466453................................................",
                "........44366343................................................",
                "........44444444................................................",
                "35555553555555553555555335555553................................",
                "34444443444444443444444334444443................................",
                "34444443444564443444444334444443................................",
                "22222222221671222222222222222222................................",
                "2333333232....232333333223333332................................",
                "233333323......32333333223333332................................",
                "222222223......32222222223333332................................",
                "........................22222222................................",
                "........1111........4555555422222222........5665................",
                "........1111........3344434422222222........4564................",
                "........1111........3434444322222222........4554................",
                "........1111........3354434222222222........4554................",
                "................3443455116553343435534435555555555555555........",
                "................3442455225543543444534454444465444444444........",
                "................3443355555543443444534444444444444444444........",
                "................2332344114432332444534443333333333333333........",
                "................2342341761432332444534442222222222222222........",
                "................233234165143233244453444........................",
                "5555555555555555233235411443233244553444........................",
                "4444464444444444233234444543233244453444........................",
                "4444444444444444233233333333233223353332........................",
                "3333555533333333233233333333233223353222........................",
                "3333444433333333222222222222222222222222........................",
                "1111111111111111111111111111111111111111........................");
        // leggings: plates with a knee band and front ridge, belt with a sun buckle
        add("models/armor/sunstone_layer_2", null,
                palette("1=6B2A0C", "2=99441A", "3=C46A1E", "4=E08E26", "5=F2B13C", "6=FFD25E", "7=FFEDB0"),
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "................................................................",
                "....44352222................22222222............................",
                "....34442222................22222222............................",
                "....44432222................22222222............................",
                "....44442222................22222222............................",
                "4554465445544554................................................",
                "4554465445534554................................................",
                "4554465445554544................................................",
                "3443364334433343................................................",
                "3443363334433453................................................",
                "2222265222222222................................................",
                "3443354334433443................................................",
                "3444354334433442222222167122222222222222........................",
                "3333353333433333333333156133333333333333........................",
                "3323353333333334444444444444444444444444........................",
                "2333353333343323333333333333333333333333........................",
                "2222222222222222222222222222222222222222........................");
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
        Path preview = Path.of("build/texture-preview/" + Golf2SunstoneTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
