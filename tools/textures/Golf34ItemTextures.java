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
 * Generates the item textures of phase 2 golf 3 + 4 (stream T, design §6.3, §6.4, §6.6, §6.7) apart from the gear:
 * dew draught, purified water, the hand warmer (charged and empty), the {@code cleansed} and {@code warmed} effect icons,
 * fire and poison arrows (item and entity), ember berries, dripshroom stew and the pumice raft item. Our own pixels in
 * the palettes of the ring 3/4 materials (dew crystal, fire crystal, jade, sulfur, toxic crystal, dripshroom, pumice);
 * the arrow entities keep the vanilla 32 x 32 arrow layout (UVs of the arrow model), not its pixels. Run from the
 * repository root: {@code java tools/textures/Golf34ItemTextures.java}.
 *
 * <p>Every texture is a pixel grid with its own palette: one character per pixel, {@code '.'} is transparent (or, with
 * a base texture, keeps the base pixel). Edit the grids by hand and run the script again; the output is
 * byte-for-byte reproducible. Checked before writing: size (16 x 16, 18 x 18 for effect icons, 32 x 32 for arrow
 * entities), alpha 0/255 only, every character in the palette. Contact sheet (8x) in {@code build/texture-preview/}.
 */
public final class Golf34ItemTextures {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/corruptedadventure/textures");
    private static final int SCALE = 8;

    private record Texture(String name, String base, Map<Character, Integer> palette, String[] rows) {
    }

    private static final List<Texture> TEXTURES_TO_WRITE = new ArrayList<>();

    private Golf34ItemTextures() {
    }

    public static void main(String[] args) throws IOException {
        // round bottle of pale-blue dew water, a dew crystal on the cork and in the drink
        add("item/dew_draught", null,
                palette("O=1E2A30", "g=8EA2AE", "G=D2E2EA", "H=F6FCFF", "c=8A6A3C", "C=B08A52", "2=2E6F86", "3=5E9FB4", "4=7EB7C8", "5=C8E5EC", "x=9DCDDB", "X=E2F1F6"),
                ".........OXO....",
                ".......OOXxO....",
                "......OCcOOO....",
                "......OGgO......",
                "......OGgO......",
                "......OGgO......",
                ".....OGGgGO.....",
                "....OGgggggO....",
                "...OG5H4443gO...",
                "...OGH44X33gO...",
                "..OG5443x322gO..",
                "...OG443322gO...",
                "...OG4332X2gO...",
                "....Og3222gO....",
                ".....OggggO.....",
                "......OOOO......");
        // round bottle of clear water with bubbles, an ice chip on the cork
        add("item/purified_water", null,
                palette("O=1E2A30", "g=8EA2AE", "G=D2E2EA", "H=F6FCFF", "c=8A6A3C", "C=B08A52", "3=B4E2EC", "4=D8F2F6", "5=F2FCFE", "i=A8DCEC", "I=EAFAFF"),
                "......OIIO......",
                "......OiIO......",
                "......OCcO......",
                "......OGgO......",
                "......OGgO......",
                "......OGgO......",
                ".....OGGgGO.....",
                "....OGgggggO....",
                "...OG5H5544gO...",
                "...OGH54H44gO...",
                "..OG554445H3gO..",
                "...OG544H43gO...",
                "...OG4H4433gO...",
                "....Og4433gO....",
                ".....OggggO.....",
                "......OOOO......");
        // leather pouch with a drawstring, a glowing fire crystal behind a round window
        add("item/hand_warmer", null,
                palette("O=24140A", "1=4F2F18", "2=6E4524", "3=8E5E34", "4=B07A48", "s=E2C08A", "z=7A2808", "w=C75813", "x=E38323", "y=F2AD3D", "Y=F2E3C6"),
                "................",
                "......OOOO......",
                ".....O4ss3O.....",
                "......O21O......",
                "....OO3443OO....",
                "...O44433332O...",
                "..O443OOOO322O..",
                "..O44OxYyxO21O..",
                "..O43OyyxwO21O..",
                "..O43OxwwzO21O..",
                "..O433OOOO221O..",
                "..O3s3s3s3s21O..",
                "...O22222211O...",
                "....OOOOOOOO....",
                "................",
                "................");
        // the same pouch, the crystal burnt out to ash; nothing below row 13 (charge bar)
        add("item/hand_warmer_empty", null,
                palette("O=24140A", "1=4F2F18", "2=6E4524", "3=8E5E34", "4=B07A48", "s=E2C08A", "z=1E1A19", "w=2C2624", "x=3C3431", "y=524844", "Y=6E625C"),
                "................",
                "......OOOO......",
                ".....O4ss3O.....",
                "......O21O......",
                "....OO3443OO....",
                "...O44433332O...",
                "..O443OOOO322O..",
                "..O44OxYyxO21O..",
                "..O43OyyxwO21O..",
                "..O43OxwwzO21O..",
                "..O433OOOO221O..",
                "..O3s3s3s3s21O..",
                "...O22222211O...",
                "....OOOOOOOO....",
                "................",
                "................");
        // a clean mint drop with a white sparkle (18 x 18 like the vanilla effect icons)
        add("mob_effect/cleansed", null,
                palette("O=0E2E26", "3=44886C", "4=62A688", "5=8CC4A6", "6=C4E6D2", "7=F4FFF8"),
                "..................",
                "..............O...",
                "........OO...O7O..",
                ".......O65O.O777O.",
                ".......O55O..O7O..",
                "......O6555O..O...",
                ".....O655554O.....",
                "....O66555444O....",
                "...O6655544433O...",
                "...O6555544433O...",
                "...O6775444333O...",
                "...O5755444333O...",
                "...O5554443333O...",
                "...O5544433333O...",
                "....O54443333O....",
                ".....O443333O.....",
                "......OOOOOO......",
                "..................");
        // a warm flame with a hot core and a small tongue (18 x 18)
        add("mob_effect/warmed", null,
                palette("O=3A0C04", "1=701808", "2=A82C0C", "3=DC5414", "4=FA8828", "5=FFC25A", "6=FFF0BC"),
                "..................",
                "..................",
                "........OO........",
                ".......O34O..O....",
                ".......O34O.O4O...",
                "......O2222OO3O...",
                ".....O233332OO....",
                "....O23333332O....",
                "...O2334444332O...",
                "...O3345555433O...",
                "...O3445665443O...",
                "...O3455665543O...",
                "...O3455665543O...",
                "...O3445665443O...",
                "....O34555543O....",
                ".....O222222O.....",
                "......O1111O......",
                "......OOOOOO......");
        // sulfur-coated head, fire-red fletching
        add("item/fire_arrow", null,
                palette("k=5A4128", "m=8E6E44", "n=B08A5A", "O=3A1A06", "a=9C2A0C", "b=D8501A", "B=F4905A", "3=AC961C", "4=D6BE30", "5=EDDA50", "6=FFFBD0", "u=FF8A2C"),
                "................",
                "............OO..",
                "...........O65O.",
                "..........O65u4O",
                "..........O5u43O",
                "..........mn43O.",
                ".........mnOOO..",
                "........mn......",
                ".......mn.......",
                "......mn........",
                "..B..mn.........",
                "..bBmn..........",
                "...bkm..........",
                "..akab..........",
                ".aa..ab.........",
                "................");
        // vanilla arrow entity layout (side quad 16 x 5, back cross 5 x 5), sulfur-coated head, fire-red fletching
        add("entity/projectiles/fire_arrow", null,
                palette("k=5A4128", "m=8E6E44", "n=B08A5A", "b=D8501A", "B=F4905A", "3=AC961C", "4=D6BE30", "5=EDDA50", "6=FFFBD0", "u=FF8A2C"),
                "bBb.............................",
                ".bBb.........56.................",
                "kkkmnmnnmnnu4456................",
                ".bBb.........34.................",
                "bBb.............................",
                "..b.............................",
                "..B.............................",
                "bBnBb...........................",
                "..B.............................",
                "..b.............................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................");
        // toxic crystal head, poison-green fletching
        add("item/poison_arrow", null,
                palette("k=5A4128", "m=8E6E44", "n=B08A5A", "O=10240A", "a=285A0E", "b=3B7F13", "B=79C227", "3=3B7F13", "4=54A21A", "5=79C227", "6=D6EBA0", "u=A3DA4A"),
                "................",
                "............OO..",
                "...........O65O.",
                "..........O65u4O",
                "..........O5u43O",
                "..........mn43O.",
                ".........mnOOO..",
                "........mn......",
                ".......mn.......",
                "......mn........",
                "..B..mn.........",
                "..bBmn..........",
                "...bkm..........",
                "..akab..........",
                ".aa..ab.........",
                "................");
        // vanilla arrow entity layout (side quad 16 x 5, back cross 5 x 5), toxic crystal head, poison-green fletching
        add("entity/projectiles/poison_arrow", null,
                palette("k=5A4128", "m=8E6E44", "n=B08A5A", "b=3B7F13", "B=79C227", "3=3B7F13", "4=54A21A", "5=79C227", "6=D6EBA0", "u=A3DA4A"),
                "bBb.............................",
                ".bBb.........56.................",
                "kkkmnmnnmnnu4456................",
                ".bBb.........34.................",
                "bBb.............................",
                "..b.............................",
                "..B.............................",
                "bBnBb...........................",
                "..B.............................",
                "..b.............................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................");
        // three glowing ember berries on a charred twig with a leaf
        add("item/ember_berries", null,
                palette("O=3A0C04", "1=701808", "2=A82C0C", "3=DC5414", "4=FA8828", "5=FFC25A", "6=FFF0BC", "j=2C221F", "k=433D2E", "l=57503A", "L=6C6448"),
                "..........OO....",
                ".........OlLO...",
                "........OklLLO..",
                "........OkOOO...",
                ".......OkjO.....",
                "....OOOkOOkO....",
                "...O54kO.OkO....",
                "..O5633OO543O...",
                "..O4332OO4632O..",
                "..O3321OO3321O..",
                "...O21O4O3211O..",
                "....O5633OOOO...",
                "....O43321O.....",
                "....O3321O......",
                ".....O211O......",
                "......OOO.......");
        // a wooden bowl of purple dripshroom stew with green spore flecks
        add("item/dripshroom_stew", null,
                palette("3=4A3F58", "4=5D506E", "5=72648A", "6=8A7CA4", "7=A89CC0", "g=6E9A78", "G=9CC4A2", "O=24140A", "w=4A2E18", "v=6E4526", "V=8E5E34", "W=B08050"),
                "................",
                "................",
                "................",
                "......OOO.......",
                "..OOOOWWWOOOO...",
                ".OWW7666655WWO..",
                "OW66G6555g474WO.",
                "OV665g5G44G33VO.",
                "OVVV5544333VVwO.",
                "OVvvvvVVVvvvwwO.",
                "OVVvvvvvvvvwwwO.",
                ".OVVvvvvvvwwwO..",
                "..OVvvvvvvwwO...",
                "...OVVvvwwwO....",
                "....OOOOOOO.....",
                "................");
        // a porous pumice slab seen from the front-top
        add("item/pumice_raft", null,
                palette("1=5A544E", "2=6A635C", "3=817A71", "4=938C82", "5=A49D92", "6=B5AEA2", "7=C8C2B6", "O=2E2A26"),
                "................",
                "................",
                "................",
                "...OOOOOOOOOO...",
                ".OO6777272566OO.",
                "O77776766666555O",
                "O77766666555555O",
                "O66666255555444O",
                "O26655255444424O",
                "O54455343434442O",
                "O25444444444442O",
                "O21112222212222O",
                ".OOO11222122OOO.",
                "....OOOOOOOO....",
                "................",
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
        boolean projectile = texture.name().startsWith("entity/projectiles/");
        int wantWidth = layer ? 64 : projectile ? 32 : icon ? 18 : 16;
        int wantHeight = layer ? 32 : projectile ? 32 : icon ? 18 : 16;
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
        Path preview = Path.of("build/texture-preview/" + Golf34ItemTextures.class.getSimpleName().toLowerCase() + ".png");
        Files.createDirectories(preview.getParent());
        ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("contact sheet " + preview);
    }
}
