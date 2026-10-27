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
 * Generates the volcanite gear textures of phase 2 golf 3 (stream T, design §6.2): ingot, the five tools, the four
 * armor items, the upgrade smithing template and the two armor layers. Our own pixels, no vanilla pixels read or copied.
 * Run from the repository root: {@code java tools/textures/Golf34VolcaniteTextures.java}.
 *
 * <p>Palette black basalt with glowing veins: outline {@code O}, basalt {@code 1}-{@code 7} (dark to glint), veins
 * {@code r}-{@code v} (dim red to hot yellow-orange); charred grip {@code i}-{@code k}. The veins are plain pixels, no
 * emissive.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 64 x 32 for armor
 * layers), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf34VolcaniteTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf34VolcaniteTextures() {
    }

    public static void main(String[] args) throws IOException {
        // a cast basalt bar with a glowing crack over the top and down the front
        add("item/volcanite_ingot", null,
                palette("O=0A0809", "1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C"),
                "................",
                "................",
                "................",
                "................",
                "....OOOOOOOO....",
                "...O65555554O...",
                "..O5544s44443O..",
                ".O3666ut666653O.",
                "O45555t55555443O",
                "O44444s44r44332O",
                "O3333s3333r3221O",
                "O2222r222222211O",
                "OOOOOOOOOOOOOOOO",
                "................",
                "................",
                "................");
        // basalt blade with a glowing core, an ember in the guard, charred grip
        add("item/volcanite_sword", null,
                palette("O=0A0809", "1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "s=A82A0C", "t=E0501A", "u=FF8A2C", "v=FFC860", "i=1C1412", "j=34241E", "k=4E362A"),
                ".............OO.",
                "............O65O",
                "...........O6u2O",
                "..........O5t2O.",
                "....O....O4s1O..",
                "...OtO..O5t2O...",
                "...O3sOO5u21O...",
                "....O3sut21O....",
                ".....O5vt1O.....",
                ".....Oiu2O......",
                "....OkjOs3O.....",
                "...OkiO.O3tO....",
                ".OOijO...OO.....",
                "Ou5jO...........",
                "O3tO............",
                ".OO.............");
        // a broad basalt blade with a zigzag crack near the edge
        add("item/volcanite_axe", null,
                palette("O=0A0809", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "s=A82A0C", "t=E0501A", "u=FF8A2C", "i=1C1412", "j=34241E", "k=4E362A"),
                "........OO......",
                ".......O55O.....",
                "......O6544OOO..",
                ".....O65t433iiO.",
                "....O6u4s32kj2O.",
                "....O5t432kjOO..",
                ".....Os2OiiO....",
                "......OOkjO.....",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OkjO............",
                ".OO.............");
        // basalt arc, a crack through the head and down the right arm
        add("item/volcanite_pickaxe", null,
                palette("O=0A0809", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "i=1C1412", "j=34241E", "k=4E362A"),
                "................",
                ".....OOOOOOOO...",
                "....O655t5544O..",
                "...O3555s44432O.",
                "....OOOOOOOkj32O",
                ".........OkjO42O",
                "........OiiOOt2O",
                ".......OkjO.Os2O",
                "......OkjO..O32O",
                ".....OiiO...Or2O",
                "....OkjO.....O2O",
                "...OkjO.......O.",
                "..OiiO..........",
                ".OkjO...........",
                "OkjO............",
                ".OO.............");
        // a basalt spade split by a glowing crack
        add("item/volcanite_shovel", null,
                palette("O=0A0809", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "s=A82A0C", "t=E0501A", "u=FF8A2C", "i=1C1412", "j=34241E", "k=4E362A"),
                "............OO..",
                "..........OO65O.",
                ".........O65t44O",
                ".........O5s544O",
                "........O5u5443O",
                "........O5t443O.",
                "........Os443O..",
                ".......OkjOOO...",
                "......OkjO......",
                ".....OiiO.......",
                "....OkjO........",
                "...OkjO.........",
                "..OiiO..........",
                ".OkjO...........",
                "OkjO............",
                ".OO.............");
        // basalt blade with a crack running into the hook
        add("item/volcanite_hoe", null,
                palette("O=0A0809", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C", "i=1C1412", "j=34241E", "k=4E362A"),
                "........OO......",
                ".......O55OO.OO.",
                "......O655t4O32O",
                ".......O54u3s22O",
                "........OO43s2O.",
                ".........Ok3rO..",
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
        // basalt dome with a crack down to an ember in the brow band
        add("item/volcanite_helmet", null,
                palette("O=0A0809", "1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C"),
                "................",
                "................",
                "......OOOO......",
                "....OO6555OO....",
                "...O55544s33O...",
                "..O54434t4322O..",
                "..O4433su3222O..",
                "..O3222rt2221O..",
                "..O43OOOOOO32O..",
                "..O32O....O21O..",
                "..O21O....O11O..",
                "...OO......OO...",
                "................",
                "................",
                "................",
                "................");
        // basalt plate cracked open around a glowing ember core
        add("item/volcanite_chestplate", null,
                palette("O=0A0809", "1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C", "v=FFC860"),
                "................",
                "................",
                "..OOO......OOO..",
                ".O655O....O543O.",
                ".O54t4OOOO4s32O.",
                ".O543s2112t321O.",
                ".O4443r33s3221O.",
                ".O44333t3t3322O.",
                "..OO332uvs32OO..",
                "...O34stus22O...",
                "...O43s3r322O...",
                "...O433s3t22O...",
                "...O2222r221O...",
                "....O111111O....",
                ".....OOOOOO.....",
                "................");
        // ember buckle, cracks down both legs
        add("item/volcanite_leggings", null,
                palette("O=0A0809", "1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C"),
                "................",
                "................",
                "...OOOOOOOOOO...",
                "..O2222ut2221O..",
                "..O5554564332O..",
                "..O5s44554332O..",
                "..O4t43OO4s32O..",
                "..O5s3O..O4u2O..",
                "..O543O..O3t2O..",
                "..O4r3O..O332O..",
                "..O433O..Or22O..",
                "..O322O..O221O..",
                "..O211O..O111O..",
                "...OOO....OOO...",
                "................",
                "................");
        // basalt boots with glowing cracks, dark soles
        add("item/volcanite_boots", null,
                palette("O=0A0809", "1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A"),
                "................",
                "................",
                "................",
                "...OOO....OOO...",
                "..O655O..O554O..",
                "..O5t4O..O443O..",
                "..O4s3O..O4t3O..",
                "..O433O..O3s2O..",
                "..O4r32O.O4332OO",
                ".O554332OO554s32",
                ".O5443t22O544332",
                ".O1111111O111111",
                "..OOOOOOO.OOOOOO",
                "................",
                "................",
                "................");
        // the sunstone tablet in basalt: a flame rising out of a glowing crack
        add("item/volcanite_upgrade_smithing_template", null,
                palette("r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C", "v=FFC860", "a=0A0809", "b=1F1A1D", "c=2B2427", "d=3A3134", "e=554A4D"),
                "................",
                "....aaaaaaaaa...",
                "...aeeeeeeeeca..",
                "..aeddddddddcba.",
                "..aeddddtdddcba.",
                "..aedddtudddcba.",
                "..aeddtuusddcba.",
                "..aeddtuvuddcba.",
                "..aedsuvvutdcba.",
                "..aedtuvvusdcba.",
                "..aeddrssrddcba.",
                "..aerrddddrrcba.",
                "..adccccccccbba.",
                "...abbbbbbbbba..",
                "....aaaaaaaaa...",
                "................");
        // basalt plates with joints; glowing crest seam, brow ember, chest core, cracks on every part
        add("models/armor/volcanite_layer_1", null,
                palette("1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C", "v=FFC860"),
                "........54554545................................................",
                "........55554445................................................",
                "........5555s545................................................",
                "........5555t545................................................",
                "........5555u445................................................",
                "........3445s433................................................",
                "........24554342................................................",
                "........24554442................................................",
                "34r555534555555435555r4334s55443................................",
                "33s44443334444433344s433244t4343................................",
                "334t4443334sr4433445t433344u4333................................",
                "22222222231ut1222222222222222222................................",
                "232s333332....22233s23422343t322................................",
                "2323r3323......4233r333223233s23................................",
                "222222223......32222222223333r22................................",
                "........................22222222................................",
                "........1111........4544554322222222........4664................",
                "........1111........3343344322222222........3554................",
                "........1111........3444344322222222........4454................",
                "........1111........3445344322222222........3454................",
                "................3343545115543442444444345555555555555555........",
                "................33334s5545r4333244r444344s444r44444444r4........",
                "................3s4345r54s542442444s433444t444s444s444s4........",
                "................2222343sr3432r32344t4434333333t333333333........",
                "................23r2344uv34323323454t4432222222222222222........",
                "................3322344tu34323s23444s433........................",
                "5555555555555555232234s33t43233233444533........................",
                "4s444r444s4444r423223r4443s32332344r4343........................",
                "44t444s444444s443222333332r3223223333332........................",
                "333355t533333333232233333233234223333332........................",
                "3333444433333333222222222222222222222222........................",
                "1111111111111111111111111111111111111111........................");
        // leggings: basalt plates with a glowing knee and cracks, belt with an ember buckle
        add("models/armor/volcanite_layer_2", null,
                palette("1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "r=6E1408", "s=A82A0C", "t=E0501A", "u=FF8A2C", "v=FFC860"),
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
                "....44432222................22222222............................",
                "....44442222................22222222............................",
                "....53342222................22222222............................",
                "....44452222................22222222............................",
                "455346544r444554................................................",
                "45s346r445s44543................................................",
                "45t346s445t43s54................................................",
                "3s42264t343334t3................................................",
                "3r432643344234r3................................................",
                "22222ts222222222................................................",
                "3442254334423442................................................",
                "344325s333423r432222221su122222222222222........................",
                "232325t33s323s323333331tv133333333333333........................",
                "3233253r33r2333344s44444434444r43444s443........................",
                "3232353332332333333r43333322233s322t3233........................",
                "2222222222222222222222222222222222222222........................");
        // the three nuggets of the ring 3 texture in basalt, the old glints now embers
        add("item/raw_volcanite", null,
                palette("O=0A0809", "1=161214", "2=231D20", "3=30282B", "4=40363A", "5=554A4D", "6=6E6164", "u=FF8A2C"),
                "................",
                ".........111....",
                "........1665O...",
                "....111166u5O...",
                "..11666566543O..",
                ".166665525443O..",
                "166u655442433O..",
                "1666544366632O..",
                "165544366655O...",
                ".OO2466665542O..",
                "...O26u6554332O.",
                "....O655443332O.",
                ".....O544332OO..",
                "......OO222O....",
                "........OOO.....",
                "................");
        // the golf 1 lumps in basalt, the crevices between them glowing
        add("block/raw_volcanite_block", null,
                palette("3=30282B", "4=40363A", "5=554A4D", "r=6E1408", "s=A82A0C"),
                "3r45554334543rr3",
                "333454333443r333",
                "4434343r3333r344",
                "55433rr334443355",
                "55433r3444553345",
                "45443r3454443334",
                "343333345543rr33",
                "r3344r344443rrsr",
                "s345433434443srs",
                "sr4433rr34543rrr",
                "rr3453sr34443433",
                "3333333rr3334544",
                "34544443rr334554",
                "3454554343r34544",
                "3444444454333433",
                "333454334343333r");
        // the golf 1 bevelled plate in basalt, specks as glowing pits, two cracks from the bevel
        add("block/volcanite_block", null,
                palette("1=161214", "2=231D20", "4=40363A", "5=554A4D", "r=6E1408", "s=A82A0C", "t=E0501A"),
                "1111111111111111",
                "1555555555555551",
                "1544r4r444444421",
                "1544s44444444421",
                "15444t4445444521",
                "1544ss4444445521",
                "1544s4r5r4444421",
                "15445544544r4s21",
                "1544444444444s21",
                "1555444444444r21",
                "1544454444rts421",
                "154445544ss4s421",
                "155444444r444421",
                "1544s44444444521",
                "1522222222222221",
                "1111111111111111");
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
        Path preview = Path.of("build/texture-preview/" + Golf34VolcaniteTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
