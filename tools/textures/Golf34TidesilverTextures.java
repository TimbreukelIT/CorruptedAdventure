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
 * Generates the tidesilver gear textures of phase 2 golf 4 (stream T, design §6.5): ingot, the five tools, the four
 * armor items, the upgrade smithing template and the two armor layers. Our own pixels, no vanilla pixels read or copied.
 * Run from the repository root: {@code java tools/textures/Golf34TidesilverTextures.java}.
 *
 * <p>Palette bright silver with jade inlay: outline {@code O}, silver {@code 1}-{@code 7} (shadow to white glint), jade
 * {@code a}-{@code g} (the ring 4 jade, dark to light) on hilts and rims; sea-leather grip {@code i}-{@code k}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 64 x 32 for armor
 * layers), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf34TidesilverTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf34TidesilverTextures() {
    }

    public static void main(String[] args) throws IOException {
        // a bright silver bar with a small jade inlay on the front
        add("item/tidesilver_ingot", null,
                palette("O=1A2830", "1=3A4A54", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "d=44886C"),
                "................",
                "................",
                "................",
                "................",
                "....OOOOOOOO....",
                "...O66666665O...",
                "..O6666666654O..",
                ".O577777777664O.",
                "O45555555555443O",
                "O44445dd4444332O",
                "O33333333333221O",
                "O22222222222211O",
                "OOOOOOOOOOOOOOOO",
                "................",
                "................",
                "................");
        // silver blade, jade guard with a light jade stone, jade pommel
        add("item/tidesilver_sword", null,
                palette("O=1A2830", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "b=1C4A3C", "c=2E6A54", "d=44886C", "e=62A688", "f=8CC4A6", "g=C4E6D2", "i=1A2C34", "j=2A444E", "k=3C5E68"),
                ".............OO.",
                "............O76O",
                "...........O764O",
                "..........O764O.",
                "....O....O753O..",
                "...OeO..O764O...",
                "...OcdOO7643O...",
                "....Ocdf643O....",
                ".....Ofg43O.....",
                ".....Oie3O......",
                "....OkjOdcO.....",
                "...OkiO.OcbO....",
                ".OOijO...OO.....",
                "OfejO...........",
                "OdcO............",
                ".OO.............");
        // silver blade, jade binding and cap where the head meets the grip
        add("item/tidesilver_axe", null,
                palette("O=1A2830", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "c=2E6A54", "d=44886C", "e=62A688", "i=1A2C34", "j=2A444E", "k=3C5E68"),
                "........OO......",
                ".......O77O.....",
                "......O7665OOO..",
                ".....O766543ddO.",
                "....O766543ec3O.",
                "....O65433dcOO..",
                ".....O32OiiO....",
                "......OOkjO.....",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OedO............",
                ".OO.............");
        // silver arc with a jade inlay over the grip, jade binding
        add("item/tidesilver_pickaxe", null,
                palette("O=1A2830", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "d=44886C", "e=62A688", "i=1A2C34", "j=2A444E", "k=3C5E68"),
                "................",
                ".....OOOOOOOO...",
                "....O76666554O..",
                "...O35555dd432O.",
                "....OOOOOOOed32O",
                ".........OkjO42O",
                "........OiiOO32O",
                ".......OkjO.O32O",
                "......OkjO..O32O",
                ".....OiiO...O32O",
                "....OkjO.....O2O",
                "...OkjO.......O.",
                "..OiiO..........",
                ".OkjO...........",
                "OedO............",
                ".OO.............");
        // silver spade with a jade inlay at the neck, jade binding
        add("item/tidesilver_shovel", null,
                palette("O=1A2830", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "c=2E6A54", "d=44886C", "e=62A688", "i=1A2C34", "j=2A444E", "k=3C5E68"),
                "............OO..",
                "..........OO76O.",
                ".........O77654O",
                ".........O66544O",
                "........O665443O",
                "........O65443O.",
                "........O5dc3O..",
                ".......OedOOO...",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OedO............",
                ".OO.............");
        // silver blade, jade binding
        add("item/tidesilver_hoe", null,
                palette("O=1A2830", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "d=44886C", "e=62A688", "i=1A2C34", "j=2A444E", "k=3C5E68"),
                "........OO......",
                ".......O66OO.OO.",
                "......O76554O32O",
                ".......O5543322O",
                "........OO4332O.",
                ".........Oe32O..",
                "........OiiOO...",
                ".......OkjO.....",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OedO............",
                ".OO.............");
        // silver dome, jade brow band with a gem, jade cheek rims
        add("item/tidesilver_helmet", null,
                palette("O=1A2830", "1=3A4A54", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "b=1C4A3C", "c=2E6A54", "d=44886C", "f=8CC4A6", "g=C4E6D2"),
                "................",
                "................",
                "......OOOO......",
                "....OO7665OO....",
                "...O66655443O...",
                "..O6554664332O..",
                "..O5543773322O..",
                "..OdcdcgfcdcbO..",
                "..O43OOOOOO32O..",
                "..O32O....O21O..",
                "..OdcO....OcbO..",
                "...OO......OO...",
                "................",
                "................",
                "................",
                "................");
        // silver plate with a jade collar, jade gem and jade belt
        add("item/tidesilver_chestplate", null,
                palette("O=1A2830", "1=3A4A54", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "c=2E6A54", "d=44886C", "e=62A688", "f=8CC4A6", "g=C4E6D2"),
                "................",
                "................",
                "..OOO......OOO..",
                ".O776O....O765O.",
                ".O6665OOOO5543O.",
                ".Od66ecdde5432O.",
                ".O665443344432O.",
                ".O655544443332O.",
                "..OO554fe433OO..",
                "...O54gfd433O...",
                "...O544dc332O...",
                "...O44433332O...",
                "...OddeeeddcO...",
                "....O222221O....",
                ".....OOOOOO.....",
                "................");
        // jade belt with a light gem, jade lower rims
        add("item/tidesilver_leggings", null,
                palette("O=1A2830", "1=3A4A54", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "b=1C4A3C", "c=2E6A54", "d=44886C", "f=8CC4A6", "g=C4E6D2"),
                "................",
                "................",
                "...OOOOOOOOOO...",
                "..OcdcdgfdcdbO..",
                "..O6554674332O..",
                "..O5544554332O..",
                "..O5443OO4332O..",
                "..O543O..O432O..",
                "..O543O..O432O..",
                "..O443O..O332O..",
                "..O433O..O322O..",
                "..O322O..O221O..",
                "..OdcbO..OcbbO..",
                "...OOO....OOO...",
                "................",
                "................");
        // silver boots with jade cuffs
        add("item/tidesilver_boots", null,
                palette("O=1A2830", "1=3A4A54", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "c=2E6A54", "d=44886C", "e=62A688"),
                "................",
                "................",
                "................",
                "...OOO....OOO...",
                "..O765O..O654O..",
                "..OdedO..OdcdO..",
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
        // the sunstone tablet in tidestone: two rolling waves, silver crests over jade
        add("item/tidesilver_upgrade_smithing_template", null,
                palette("5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "a=16242A", "b=2C3E48", "c=3C5260", "d=4E6674", "e=6A8490", "p=1C4A3C", "q=2E6A54", "w=62A688"),
                "................",
                "....aaaaaaaaa...",
                "...aeeeeeeeeca..",
                "..aeddddddddcba.",
                "..aeddddddddcba.",
                "..aed77dd77dcba.",
                "..ae6dd56dd5cba.",
                "..aeddddddddcba.",
                "..aedwwddwwdcba.",
                "..aeqddpqddpcba.",
                "..aeddddddddcba.",
                "..aeddddddddcba.",
                "..adccccccccbba.",
                "...abbbbbbbbba..",
                "....aaaaaaaaa...",
                "................");
        // silver plates; jade brow band with a gem, jade collar and chest gem, jade belt, rims and cuffs
        add("models/armor/tidesilver_layer_1", null,
                palette("1=3A4A54", "2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "b=1C4A3C", "c=2E6A54", "d=44886C", "e=62A688", "f=8CC4A6", "g=C4E6D2"),
                "........cccccccc................................................",
                "........55566555................................................",
                "........55567555................................................",
                "........55567555................................................",
                "........55566555................................................",
                "........34466443................................................",
                "........34466443................................................",
                "........cccccccc................................................",
                "35555553555555553555555335555553................................",
                "34444443444444443444444334444443................................",
                "34444443444gf4443444444334444443................................",
                "dcdcdcdcdcbfebdcdcdcdcdcdcdcdcdc................................",
                "2333333232....232333333223333332................................",
                "233333323......32333333223333332................................",
                "bbbbbbbb3......3bbbbbbbb23333332................................",
                "........................bbbbbbbb................................",
                "........1111........4555555422222222........5665................",
                "........1111........3444444322222222........4554................",
                "........1111........3444444322222222........4554................",
                "........1111........3444444322222222........4554................",
                "................34434dcbbcd43443444644445555555555555555........",
                "................3443455cc5543443444644444444476444444444........",
                "................34434555555434434446444444444ed444444444........",
                "................2332344654432332444644443333333333333333........",
                "................2332346gf343233244464444cccccccccccccccc........",
                "................2332345ed343233244464444........................",
                "dddddddddddddddd233234432443233244464444........................",
                "4444476444444444233234444443233244464444........................",
                "4444444444444444233233333333233223364332........................",
                "3333555533333333233233333333233223364332........................",
                "3333444433333333cdcdcdcdcdcdcdcdcdcdcdcd........................",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb........................");
        // leggings: silver plates with a jade knee band and rim, jade belt with a silver buckle
        add("models/armor/tidesilver_layer_2", null,
                palette("2=5A6E78", "3=7E949C", "4=A4B8BE", "5=C6D6DA", "6=E2EEF0", "7=FFFFFF", "b=1C4A3C", "c=2E6A54", "d=44886C", "e=62A688", "f=8CC4A6", "g=C4E6D2"),
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
                "....44442222................22222222............................",
                "....44442222................22222222............................",
                "....44442222................22222222............................",
                "....44442222................22222222............................",
                "4554465445544554................................................",
                "4554465445544554................................................",
                "4554465445544554................................................",
                "3443364334433443................................................",
                "3443364334433443................................................",
                "cdcdcfedcdcdcdcd................................................",
                "3443354334433443................................................",
                "3443354334433443cdcdcd2762cdcdcdcdcdcdcd........................",
                "33333533333333333333332gf233333333333333........................",
                "3333353333333333444444444444444444444444........................",
                "3333353333333333333333333333333333333333........................",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb........................");
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
        Path preview = Path.of("build/texture-preview/" + Golf34TidesilverTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
