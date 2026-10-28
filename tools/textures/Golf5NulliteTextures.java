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
 * Generates the nullite gear textures of phase 2 golf 5 (stream T, design §7.1): ingot, raw nullite, the nullite and
 * raw nullite blocks, the five tools, the four armor items, the upgrade smithing template (the void ring of golf 3 + 4)
 * and the two armor layers. Our own pixels, no vanilla pixels read or copied.
 * Run from the repository root: {@code java tools/textures/Golf5NulliteTextures.java}.
 *
 * <p>Palette deep anthracite with purple-cyan glitch edges, no emissive: outline {@code O}, anthracite {@code 1}-{@code 7}
 * (dark to sheen) with faint scanlines; the glitch edges are a chroma split in short runs, cyan {@code x}-{@code z} on
 * upper/left edges and purple {@code p}-{@code r} on lower/right ones, and one torn line (cyan, the rest a row lower in
 * purple). Every item has one "missing" pixel: a hole (transparent) in items and layers, void black on the blocks.
 * Void-leather grip {@code i}-{@code k}. Template: voidstone tablet {@code a}-{@code e}, void ring {@code 2}-{@code 5}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 64 x 32 for armor
 * layers), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf5NulliteTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf5NulliteTextures() {
    }

    public static void main(String[] args) throws IOException {
        // an anthracite bar: cyan lit edge, purple shadow edge, a torn line and a missing pixel
        add("item/nullite_ingot", null,
                palette("O=050507", "1=0D0E11", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "7=5A5E68", "p=4A2384", "q=7A3FD0", "y=34C8D6", "z=9EF2F6"),
                "................",
                "................",
                "................",
                "................",
                "....OOOOOOOO....",
                "...Oz6666665O...",
                "..Oy555555544O..",
                ".Oy7777777766qO.",
                "O54444444444443O",
                "O4333zyqp33332qO",
                "O22222.2222221pO",
                "O11111111111111O",
                "OOOOOOOOOOOOOOOO",
                "................",
                "................",
                "................");
        // three anthracite nuggets, their glints split cyan (upper left) and purple
        add("item/raw_nullite", null,
                palette("O=050507", "1=0D0E11", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "q=7A3FD0", "y=34C8D6"),
                "................",
                ".........33.....",
                "........366O....",
                "....333365y6O...",
                "...3666655446O..",
                "..365y5454442O..",
                ".365544441422O..",
                ".365444421221O..",
                ".36444425554O...",
                ".364445555446O..",
                "..OO625q444426O.",
                "....OO64444221O.",
                "......O642221O..",
                ".......O6111O...",
                "........OOOO....",
                "................");
        // raw nullite chunks with a few cyan and purple glints
        add("block/raw_nullite_block", null,
                palette("1=0D0E11", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "q=7A3FD0", "y=34C8D6"),
                "4313133334555555",
                "543111333345566q",
                "5311334543445555",
                "3313135y54345544",
                "3111134543134333",
                "1131333331311311",
                "4413111111133334",
                "5431111131344435",
                "6543113313456545",
                "5531111313566y45",
                "5443344333455545",
                "y544454444565445",
                "55545y5554q65435",
                "4565455665655434",
                "4455345654555433",
                "3444144444565454");
        // the bevelled plate in anthracite with a torn line and one void-black pixel
        add("block/nullite_block", null,
                palette("O=050507", "1=0D0E11", "3=1E2025", "5=34373F", "7=5A5E68", "p=4A2384", "q=7A3FD0", "r=B48CFF", "x=1A7E8C", "y=34C8D6", "z=9EF2F6"),
                "1111111111111111",
                "1777777777777771",
                "1755555555575531",
                "1755553553555531",
                "1755555755535531",
                "1753555575533531",
                "175xyyz555555531",
                "1755555rqqp55531",
                "1755555553775731",
                "1735555557555531",
                "17575555555O5531",
                "1755555555757531",
                "1735555555555531",
                "1735555535555331",
                "1733333333333331",
                "1111111111111111");
        // anthracite blade, cyan tip, purple shadow run, the guard stone missing
        add("item/nullite_sword", null,
                palette("O=050507", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "7=5A5E68", "p=4A2384", "q=7A3FD0", "x=1A7E8C", "y=34C8D6", "z=9EF2F6", "i=0B0A10", "j=1A1624", "k=2B2438"),
                ".............OO.",
                "............Oz6O",
                "...........Oy53O",
                "..........O653O.",
                "....O....O754O..",
                "...OyO..O64qO...",
                "...O43OO654qO...",
                "....O345643O....",
                ".....O4.43O.....",
                ".....Oi43O......",
                "....OkjO4qO.....",
                "...OkiO.OqpO....",
                ".OOijO...OO.....",
                "OyxjO...........",
                "OqpO............",
                ".OO.............");
        // anthracite head, cyan edge at the top, purple binding, a missing pixel
        add("item/nullite_axe", null,
                palette("O=050507", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "7=5A5E68", "p=4A2384", "q=7A3FD0", "y=34C8D6", "z=9EF2F6", "i=0B0A10", "j=1A1624", "k=2B2438"),
                "........OO......",
                ".......Oz6O.....",
                "......Oy665OOO..",
                ".....O766543pqO.",
                "....O76.543qp3O.",
                "....O6q433p2OO..",
                ".....Op2OiiO....",
                "......OOkjO.....",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OyqO............",
                ".OO.............");
        // anthracite arc, cyan tip, purple binding and shadow run, a missing pixel
        add("item/nullite_pickaxe", null,
                palette("O=050507", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "p=4A2384", "q=7A3FD0", "y=34C8D6", "z=9EF2F6", "i=0B0A10", "j=1A1624", "k=2B2438"),
                "................",
                ".....OOOOOOOO...",
                "....Oz6666554O..",
                "...Oy5555.q432O.",
                "....OOOOOOO2q32O",
                ".........OkjO42O",
                "........OiiOO32O",
                ".......OkjO.O3qO",
                "......OkjO..O3pO",
                ".....OiiO...O32O",
                "....OkjO.....O2O",
                "...OkjO.......O.",
                "..OiiO..........",
                ".OkjO...........",
                "OyqO............",
                ".OO.............");
        // anthracite spade, cyan tip, purple shadow edge, a missing pixel
        add("item/nullite_shovel", null,
                palette("O=050507", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "7=5A5E68", "p=4A2384", "q=7A3FD0", "x=1A7E8C", "y=34C8D6", "z=9EF2F6", "i=0B0A10", "j=1A1624", "k=2B2438"),
                "............OO..",
                "..........OOz6O.",
                ".........Oy7654O",
                ".........O66.44O",
                "........O66544qO",
                "........O6543pO.",
                "........O5qp2O..",
                ".......Ox2OOO...",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OyqO............",
                ".OO.............");
        // anthracite blade, cyan tip, purple shadow edge, a missing pixel
        add("item/nullite_hoe", null,
                palette("O=050507", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "p=4A2384", "q=7A3FD0", "y=34C8D6", "z=9EF2F6", "i=0B0A10", "j=1A1624", "k=2B2438"),
                "........OO......",
                ".......Oz6OO.OO.",
                "......Oy6554O3qO",
                ".......O55.332qO",
                "........OO433pO.",
                ".........Op32O..",
                "........OiiOO...",
                ".......OkjO.....",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OyqO............",
                ".OO.............");
        // anthracite dome, split brow band, a missing pixel
        add("item/nullite_helmet", null,
                palette("O=050507", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "7=5A5E68", "p=4A2384", "q=7A3FD0", "x=1A7E8C", "y=34C8D6", "z=9EF2F6"),
                "................",
                "................",
                "......OOOO......",
                "....OOz665OO....",
                "...Oy6655443O...",
                "..Oy554664332O..",
                "..O55437.332qO..",
                "..Oxy222222qpO..",
                "..O43OOOOOO3qO..",
                "..O32O....O2pO..",
                "..Ox2O....OqpO..",
                "...OO......OO...",
                "................",
                "................",
                "................",
                "................");
        // anthracite plate, a torn line across the chest, a missing pixel
        add("item/nullite_chestplate", null,
                palette("O=050507", "1=0D0E11", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "p=4A2384", "q=7A3FD0", "x=1A7E8C", "y=34C8D6", "z=9EF2F6"),
                "................",
                "................",
                "..OOO......OOO..",
                ".Oz65O....O654O.",
                ".Oy554OOOO5443O.",
                ".O654p2222p432O.",
                ".O654433334432O.",
                ".O65yzz4443332O.",
                "..OO5443qqp2OO..",
                "...O54.44332O...",
                "...O5443333qO...",
                "...O4433322pO...",
                "...O1p22yx21O...",
                "....O222221O....",
                ".....OOOOOO.....",
                "................");
        // dark belt with a cyan buckle, split rims, a missing pixel
        add("item/nullite_leggings", null,
                palette("O=050507", "1=0D0E11", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "p=4A2384", "q=7A3FD0", "x=1A7E8C", "y=34C8D6"),
                "................",
                "................",
                "...OOOOOOOOOO...",
                "..O1222yx2221O..",
                "..Oy554664332O..",
                "..Oy544554.32O..",
                "..O5443OO4332O..",
                "..O543O..O432O..",
                "..O543O..O43qO..",
                "..O443O..O33qO..",
                "..O433O..O322O..",
                "..O322O..O221O..",
                "..Oyx1O..O1qpO..",
                "...OOO....OOO...",
                "................",
                "................");
        // anthracite boots, cyan cuff left, purple cuff right, a missing pixel
        add("item/nullite_boots", null,
                palette("O=050507", "1=0D0E11", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "p=4A2384", "q=7A3FD0", "x=1A7E8C", "y=34C8D6", "z=9EF2F6"),
                "................",
                "................",
                "................",
                "...OOO....OOO...",
                "..Oz65O..O654O..",
                "..Oyx2O..O2qpO..",
                "..O443O..O433O..",
                "..O433O..O332O..",
                "..O4332O.O4332OO",
                ".O654332OO65.332",
                ".O54433qpO54433q",
                ".O1111111O111111",
                "..OOOOOOO.OOOOOO",
                "................",
                "................",
                "................");
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
        // anthracite plates with scanlines; split crest, brow band, cuffs and rims; a torn line and a missing pixel on the chest
        add("models/armor/nullite_layer_1", null,
                palette("1=0D0E11", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "p=4A2384", "q=7A3FD0", "r=B48CFF", "x=1A7E8C", "y=34C8D6", "z=9EF2F6"),
                "........55555555................................................",
                "........444y4444................................................",
                "........555y5555................................................",
                "........555y5555................................................",
                "........5555q555................................................",
                "........2333q332................................................",
                "........3445q443................................................",
                "........34444443................................................",
                "35555553555555553555555335555553................................",
                "2333333233333.332333333223333332................................",
                "34444543444zy4443444444334544443................................",
                "xy2222qpxy2222qpxy2222qpxy2222qp................................",
                "3333333232....232333333223333332................................",
                "222222222......22222222222222222................................",
                "222222223......32222222224433332................................",
                "........................22222222................................",
                "........1111........4555555422222222........5665................",
                "........1111........2333333222222222........3443................",
                "........1111........3444444322222222........4554................",
                "........1111........3544444322222222........4554................",
                "................44444yx55pq43443444444445555555555555555........",
                "................23323444444323323333333344444yz444444444........",
                "................3yy345555554344344yz44444444444444444444........",
                "................2332444444432qq24444qp443333333333333333........",
                "................2332xyyz4443233245444444pqpqpqpqpqpqpqpq........",
                "................22222333rqqp322233333333........................",
                "xy55xy55xy55xy55233234444543233244444444........................",
                "4444444444444444233234.44443233244444444........................",
                "44444qq444444444233233333343233223333332........................",
                "3333555533333333222222222222222222222222........................",
                "333344443333333322yy2222p22222yy2222p222........................",
                "11qp11qp11qp11qp111111111111111111111111........................");
        // leggings: anthracite plates, split knee band and rim, cyan buckle, a missing pixel on the leg
        add("models/armor/nullite_layer_2", null,
                palette("1=0D0E11", "2=15161A", "3=1E2025", "4=282A31", "5=34373F", "6=44474F", "p=4A2384", "q=7A3FD0", "x=1A7E8C", "y=34C8D6", "z=9EF2F6"),
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
                "....44542222................22222222............................",
                "....33332222................22222222............................",
                "....44442222................22222222............................",
                "....44442222................22222222............................",
                "5554465445554554................................................",
                "3443364334433443................................................",
                "455446.445545554................................................",
                "3443364334433443................................................",
                "3453364334433443................................................",
                "xy22xy52xy22xy22................................................",
                "4443354334433444................................................",
                "34433553344334432222221zy122222222222222........................",
                "33333533333333343333331yx133333333333333........................",
                "222225222222222244qp4444444444yx44444444........................",
                "333335343333334333333333333333333333qp33........................",
                "22qp22qp22qp22qp222222222222222222222222........................");
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
        Path preview = Path.of("build/texture-preview/" + Golf5NulliteTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
