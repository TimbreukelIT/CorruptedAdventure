package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nether ring 5's terrain (87, written by {@code tools/worldgen/nether_ring5_terrain.py} and
 * {@code nether_ring5_surface.py}): the shape of the white halls and the cathedrals as the JSON configures
 * {@code nether_cathedrals}, the room to walk, and the surface branch.
 */
class NetherRing5JsonTest {
    private static final String DIR = NetherCathedralsTest.DIR;
    private static final String BIOMES = "[\"corruptedadventure:white_halls\",\"corruptedadventure:hollow_cathedral\"]";
    private static final NetherCathedrals TERRAIN = NetherCathedralsTest.terrain(NetherCathedralsTest.SEED);
    private static final RingLayout LAYOUT = NetherCathedralsTest.layout(NetherCathedralsTest.SEED);
    private static final NetherCathedrals.HallSettings HALLS = TERRAIN.settings().halls();
    // The blocks of stream B (90) in the surface (87b).
    private static final String MIRROR_STONE = "corruptedadventure:mirror_stone";
    private static final String CHALKRACK = "corruptedadventure:chalkrack";
    private static final String WHITE_STONE = "corruptedadventure:white_stone";
    private static final String POLISHED_WHITE_STONE = "corruptedadventure:polished_white_stone";
    private static final String WHITE_STONE_BRICKS = "corruptedadventure:white_stone_bricks";
    private static final String WHITE_STONE_PILLAR = "corruptedadventure:white_stone_pillar";
    // The dressing (96).
    private static final String STILL_LIGHT = "corruptedadventure:still_light";
    private static final String WHITE_CORE_ORE = "corruptedadventure:white_core_ore";
    private static final String DRESSING = "corruptedadventure:nether_ring_5_dressing";

    private static double density(int x, int y, int z) {
        return TERRAIN.compute(new DensityFunction.SinglePointContext(x, y, z));
    }

    /** Columns of Nether ring 5 on a raster (every {@code step} blocks), each with its depth in the ring. */
    private static List<int[]> raster(int step, double minDepth) {
        List<int[]> columns = new ArrayList<>();
        for (int x = -NetherCathedralsTest.REACH; x <= NetherCathedralsTest.REACH; x += step) {
            for (int z = -NetherCathedralsTest.REACH; z <= NetherCathedralsTest.REACH; z += step) {
                if (NetherCathedralsTest.depth(LAYOUT, x, z) >= minDepth) {
                    columns.add(new int[]{x, z});
                }
            }
        }
        return columns;
    }

    /** Open hall floor: clear of the zones (with the margin) and of every cathedral with its clearance and one tile. */
    private static boolean open(int x, int z) {
        if (NetherCathedralsTest.depth(LAYOUT, x, z) < 56.0 + HALLS.grid()) {
            return false;
        }
        NetherCathedrals.Cathedral cathedral = TERRAIN.cellAt(x, z).cathedral();
        return cathedral == null || cathedral.distance(x, z) > cathedral.radius() + TERRAIN.settings().cathedrals().clearance() + HALLS.grid();
    }

    @Test
    void ringBlendTakesTheTerrainAndTheRouterTheMask() {
        JsonObject blend = WorldgenData.read("density_function/ring_blend/nether.json").getAsJsonObject("rings");
        assertEquals("corruptedadventure:ring_5/nether/terrain", blend.get("ring_5").getAsString());
        JsonObject temperature = WorldgenData.read("density_function/ring_blend/nether_temperature.json").getAsJsonObject("rings");
        assertEquals("corruptedadventure:ring_5/nether/region", temperature.get("ring_5").getAsString());
        assertFalse(Files.exists(WorldgenData.root().resolve("density_function/ring_5/nether.json")), "the placeholder is gone");
    }

    /**
     * The floor: its top block exactly at Y 31 (raised only on a plain, 2–6), solid down to Y 0 everywhere, so nothing
     * of Nether ring 5 is open below Y 31; open right above it; the ceiling solid from its height up to the top.
     */
    @Test
    void floorIsFlatAtY31AndSolidBelow() {
        int plains = 0, hall = 0;
        for (int[] c : raster(7, 0.0)) {
            int x = c[0], z = c[1];
            for (int y = 0; y <= HALLS.floorTop(); y++) {
                assertTrue(density(x, y, z) > 0.0, "solid at " + x + " " + y + " " + z);
            }
            if (TERRAIN.pillarAt(x, z)) {
                continue;
            }
            int top = TERRAIN.floorTop(x, z);
            assertTrue(top == HALLS.floorTop() || top >= HALLS.floorTop() + 2 && top <= HALLS.floorTop() + 6, "floor " + top);
            if (open(x, z)) {
                hall++;
                plains += top > HALLS.floorTop() ? 1 : 0;
            }
            assertTrue(density(x, top, z) > 0.0);
            assertTrue(density(x, top + 1, z) <= 0.0, "open above the floor at " + x + " " + z);
            int ceiling = TERRAIN.ceiling(x, z);
            assertTrue(density(x, ceiling - 1, z) <= 0.0, "open below the ceiling at " + x + " " + z);
            for (int y = ceiling; y < 128; y++) {
                assertTrue(density(x, y, z) > 0.0, "ceiling solid at " + x + " " + y + " " + z);
            }
        }
        // ~30 % of the open hall floor raised (the plains; none near the zones and the cathedrals)
        double share = plains / (double) hall;
        assertTrue(share > 0.2 && share < 0.4, "plains " + share);
    }

    /**
     * The columns: squares of 4 × 4 on a grid of 24 (no jitter), from the floor to the ceiling, none in the zones or
     * within the clearance of a cathedral. Checked in every cell: each standing column is a full 4 × 4, the grid is
     * regular, and no two columns of neighbouring cells touch.
     */
    @Test
    void columnsAreSquaresOnTheGrid() {
        int grid = HALLS.grid(), size = HALLS.pillarSize(), cellSize = TERRAIN.settings().cathedrals().cellSize();
        int standing = 0;
        for (int[] c : raster(5, 0.0)) {
            int x = c[0], z = c[1];
            if (!TERRAIN.pillarAt(x, z) || TERRAIN.cathedralAt(x, z) != null) {
                continue;
            }
            NetherCathedrals.Cell cell = TERRAIN.cellAt(x, z);
            int u = Math.floorMod(x - cell.cellX() * cellSize - cell.gridX(), grid);
            int v = Math.floorMod(z - cell.cellZ() * cellSize - cell.gridZ(), grid);
            assertTrue(u < size && v < size, "on the grid at " + x + " " + z);
            int px = x - u, pz = z - v;
            for (int dx = -1; dx <= size; dx++) {
                for (int dz = -1; dz <= size; dz++) {
                    boolean inside = dx >= 0 && dx < size && dz >= 0 && dz < size;
                    assertEquals(inside, TERRAIN.pillarAt(px + dx, pz + dz), "a 4 x 4 square at " + px + " " + pz);
                }
            }
            for (int y = 0; y < 128; y++) {
                assertTrue(density(x, y, z) > 0.0, "solid from bottom to top");
            }
            assertTrue(NetherCathedralsTest.depth(LAYOUT, px + size / 2, pz + size / 2) >= 56.0, "no column in or near the zones");
            standing++;
        }
        assertTrue(standing > 500, "columns stand: " + standing);
    }

    /** No grid column (nor a plain) within the clearance of a cathedral; the roof lifted to Y 120 inside the rim; the giant column 20–30 across. */
    @Test
    void cathedralsHaveTheirRoofAndGiantColumnAndNoGrid() {
        int clearance = TERRAIN.settings().cathedrals().clearance();
        List<NetherCathedrals.Cathedral> cathedrals = NetherCathedralsTest.all(TERRAIN);
        for (NetherCathedrals.Cathedral cathedral : cathedrals) {
            int reach = cathedral.radius() + clearance + 2;
            for (int x = cathedral.x() - reach; x <= cathedral.x() + reach; x++) {
                for (int z = cathedral.z() - reach; z <= cathedral.z() + reach; z++) {
                    double distance = cathedral.distance(x, z);
                    if (distance > cathedral.radius() + clearance) {
                        continue;
                    }
                    assertEquals(distance <= cathedral.pillarRadius(), TERRAIN.pillarAt(x, z), "only the giant column at " + x + " " + z);
                    if (distance > cathedral.pillarRadius()) {
                        assertEquals(HALLS.floorTop(), TERRAIN.floorTop(x, z), "no plain near a cathedral");
                    }
                }
            }
            int inner = cathedral.radius() - cathedral.rim();
            for (int r = cathedral.pillarRadius() + 1; r <= inner; r++) {
                assertEquals(HALLS.roof(), TERRAIN.ceiling(cathedral.x() + r, cathedral.z()));
                assertTrue(density(cathedral.x() + r, HALLS.roof() - 1, cathedral.z()) <= 0.0);
                assertTrue(density(cathedral.x() + r, HALLS.roof(), cathedral.z()) > 0.0);
            }
            // the rim rises without a stair: at most 3 blocks per block outwards (16 over 8 with a smoothstep)
            int last = HALLS.roof();
            for (int r = inner; r <= cathedral.radius() + 1; r++) {
                int ceiling = TERRAIN.ceiling(cathedral.x() + r, cathedral.z());
                assertTrue(ceiling <= last && last - ceiling <= 3.0, "rim " + last + " -> " + ceiling);
                last = ceiling;
            }
            assertEquals(HALLS.ceiling(), last);
            int across = 0;
            for (int x = cathedral.x() - 20; x <= cathedral.x() + 20; x++) {
                across += TERRAIN.pillarAt(x, cathedral.z()) ? 1 : 0;
            }
            assertTrue(across >= 20 && across <= 31, "giant column " + across + " across");
        }
    }

    /**
     * Room to walk (2 air above a solid floor, steps of ±1 between neighbours): at least 95 % of the hall floor (columns
     * excluded) is walkable, with at least 60 blocks of free height in the halls and 80 inside a cathedral's rim.
     */
    @Test
    void roomToWalk() {
        int floor = 0, walkable = 0, stepping = 0;
        for (int[] c : raster(3, 56.0)) {
            int x = c[0], z = c[1];
            if (TERRAIN.pillarAt(x, z)) {
                continue;
            }
            floor++;
            int top = TERRAIN.floorTop(x, z);
            boolean open = density(x, top, z) > 0.0 && density(x, top + 1, z) <= 0.0 && density(x, top + 2, z) <= 0.0;
            boolean step = true;
            for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
                if (!TERRAIN.pillarAt(x + d[0], z + d[1])) {
                    step &= Math.abs(TERRAIN.floorTop(x + d[0], z + d[1]) - top) <= 1;
                }
            }
            walkable += open ? 1 : 0;
            stepping += open && step ? 1 : 0;
            double free = TERRAIN.ceiling(x, z) - (top + 1);
            NetherCathedrals.Cathedral cathedral = TERRAIN.cathedralAt(x, z);
            if (cathedral == null) {
                assertTrue(free >= 60.0, "free height " + free + " at " + x + " " + z);
            } else if (cathedral.distance(x, z) <= cathedral.radius() - cathedral.rim()) {
                assertTrue(free >= 80.0, "free height " + free + " in a cathedral");
            }
        }
        assertTrue(walkable >= 0.95 * floor, "walkable " + walkable + " of " + floor);
    }

    /**
     * The lava step 4|5 (for {@code Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround}): Nether ring 5's floor reaches
     * above {@code top}, and its terrain is solid up to Y {@code top} on a grid over the whole zone 4→5 (depth −32..32 in
     * the ring, where {@code ring_blend} mixes it with Nether ring 4). Nether ring 4's input is solid there too (its dam,
     * {@code NetherRing4JsonTest.assertDamSolidUpTo}), and a mix of two solid inputs is solid.
     * <p>
     * Limitation: this samples the two inputs, not the blended density of the running world; the scan of the generated
     * world ({@code tools/nether-lava-walls.py} r 2.000, {@code lavascan.py}) is INT1's.
     */
    static void assertSolidUpTo(int top, String boundary) {
        JsonObject halls = WorldgenData.read(DIR + "terrain.json").getAsJsonObject("settings").getAsJsonObject("halls");
        assertTrue(halls.get("floor_top").getAsInt() >= top, boundary + ": Nether ring 5's floor must reach Y " + top);
        int sampled = 0;
        for (int x = -NetherCathedralsTest.REACH; x <= NetherCathedralsTest.REACH; x += 5) {
            for (int z = -NetherCathedralsTest.REACH; z <= NetherCathedralsTest.REACH; z += 5) {
                double depth = NetherCathedralsTest.depth(LAYOUT, x, z);
                if (depth < -NetherCathedralsTest.INNER_ZONE_HALF || depth > NetherCathedralsTest.INNER_ZONE_HALF
                        || Math.hypot(x, z) > (NetherCathedralsTest.INNER + NetherCathedralsTest.OUTER) / 2.0) {
                    continue;
                }
                sampled++;
                for (int y = 0; y <= top; y++) {
                    assertTrue(density(x, y, z) > 0.0, boundary + ": Nether ring 5 open at " + x + " " + y + " " + z);
                }
            }
        }
        assertTrue(sampled > 1000, boundary + ": the zone 4→5 sampled (" + sampled + ")");
    }

    /** A density function file must be a number or an object (the registry codec takes no bare reference). */
    @Test
    void everyFileIsAnObject() throws IOException {
        try (Stream<Path> files = Files.list(WorldgenData.root().resolve(DIR))) {
            for (Path file : files.toList()) {
                assertTrue(JsonParser.parseString(Files.readString(file)).isJsonObject(), file.getFileName().toString());
            }
        }
    }

    @Test
    void everyNoiseItReadsExists() throws IOException {
        Pattern noise = Pattern.compile("\"(?:noise|cells|plains)\": \"corruptedadventure:([a-z0-9_]+)\"");
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.list(WorldgenData.root().resolve(DIR))) {
            for (Path file : files.toList()) {
                Matcher matcher = noise.matcher(Files.readString(file));
                while (matcher.find()) {
                    found.add(matcher.group(1));
                    assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + matcher.group(1) + ".json")), matcher.group(1));
                }
            }
        }
        assertTrue(found.contains("nether_ring_5_region") && found.contains("nether_ring_5_cathedral_cells"), found.toString());
    }

    /**
     * The surface branch of the two biomes sits right after Nether ring 4's, before vanilla's floor rule (holes below Y 32
     * → lava), paints the columns through {@code nether_ring_5_pillar}, and ends in a catch-all; no lava, no falling block.
     */
    @Test
    void surfaceBranchComesBeforeVanillasLavaHoles() {
        JsonArray rules = surfaceRules();
        int own = ownBranch(rules), ring4 = -1, hole = -1;
        for (int i = 0; i < rules.size(); i++) {
            String rule = rules.get(i).toString();
            JsonObject test = rules.get(i).getAsJsonObject().getAsJsonObject("if_true");
            if (test != null && test.has("biome_is") && test.get("biome_is").toString().contains("corruptedadventure:bone_dunes")) {
                ring4 = i;
            } else if (hole < 0 && rule.contains("\"minecraft:hole\"")) {
                hole = i;
            }
        }
        assertEquals(ring4 + 1, own, "right after Nether ring 4");
        assertTrue(own < hole, "before the hole rule");
        JsonElement branch = rules.get(own);
        JsonArray sequence = branch.getAsJsonObject().getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals("minecraft:block", sequence.get(sequence.size() - 1).getAsJsonObject().get("type").getAsString(), "catch-all");
        String text = branch.toString();
        assertTrue(text.contains("\"corruptedadventure:nether_ring_5_pillar\""), "the columns");
        assertTrue(text.contains("\"absolute\":" + (HALLS.floorTop() + 1)), "the floor rule ends at the floor's top block");
        for (String banned : List.of("minecraft:lava", "minecraft:sand", "minecraft:gravel", "concrete_powder")) {
            assertFalse(text.contains(banned), banned);
        }
    }

    /**
     * 87b: the branch paints stream B's blocks (90) as the plan maps them, in this order: below Y 32 the floor's top block
     * mirror stone over chalkrack; the columns white stone pillars (axis y); from there up to the highest plain the plains,
     * polished white stone on top and white stone bricks below their top (their sides); everything else (ceiling, walls,
     * the cathedral's roof) white stone. No vanilla stand-in is left; every block exists, none falls, and every property
     * is one of the block's. 96: the floor's top block first asks the imprint (bricks, then polished), the columns first
     * the light band (still light) and the ore (white core ore), each through {@code nether_ring_5_dressing}.
     */
    @Test
    void surfaceBranchPaintsStreamBsBlocks() {
        JsonArray rules = surfaceRules();
        JsonObject branch = rules.get(ownBranch(rules)).getAsJsonObject();
        JsonArray sequence = branch.getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals(4, sequence.size());
        JsonObject floor = sequence.get(0).getAsJsonObject();
        assertBand(floor, HALLS.floorTop() + 1, null, CHALKRACK);
        JsonArray top = floor.getAsJsonObject("then_run").getAsJsonArray("sequence").get(0).getAsJsonObject()
                .getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals(3, top.size());
        assertDressing(top.get(0).getAsJsonObject(), "imprint_bricks", WHITE_STONE_BRICKS);
        assertDressing(top.get(1).getAsJsonObject(), "imprint_polished", POLISHED_WHITE_STONE);
        assertEquals(MIRROR_STONE, blockName(top.get(2).getAsJsonObject()));
        JsonObject pillar = sequence.get(1).getAsJsonObject();
        assertEquals("corruptedadventure:nether_ring_5_pillar", pillar.getAsJsonObject("if_true").get("type").getAsString());
        JsonArray column = pillar.getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals(3, column.size());
        assertDressing(column.get(0).getAsJsonObject(), "light", STILL_LIGHT);
        assertDressing(column.get(1).getAsJsonObject(), "ore", WHITE_CORE_ORE);
        JsonObject pillarState = column.get(2).getAsJsonObject().getAsJsonObject("result_state");
        assertEquals(WHITE_STONE_PILLAR, pillarState.get("Name").getAsString());
        assertEquals("y", pillarState.getAsJsonObject("Properties").get("axis").getAsString());
        // the plains reach floor_top + plains_height.max (NetherCathedrals; floorIsFlatAtY31AndSolidBelow)
        assertBand(sequence.get(2).getAsJsonObject(), HALLS.floorTop() + (int) HALLS.plainsHeight().max() + 1,
                POLISHED_WHITE_STONE, WHITE_STONE_BRICKS);
        assertEquals(WHITE_STONE, blockName(sequence.get(3).getAsJsonObject()));
        List<String> names = new ArrayList<>();
        walk(branch, rule -> {
            String name = blockName(rule);
            if (name == null) {
                return;
            }
            names.add(name);
            ResourceLocation id = ResourceLocation.parse(name);
            assertEquals("corruptedadventure", id.getNamespace(), "a vanilla stand-in left: " + name);
            assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "no such block: " + name);
            Block block = BuiltInRegistries.BLOCK.get(id);
            assertFalse(block instanceof FallingBlock, name + " falls");
            // the codec falls back to the default for an unknown value, so compare every value with the parsed state
            JsonObject json = rule.getAsJsonObject("result_state");
            BlockState state = BlockState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(error -> new AssertionError(name + ": " + error));
            if (json.has("Properties")) {
                for (Map.Entry<String, JsonElement> property : json.getAsJsonObject("Properties").entrySet()) {
                    Property<?> known = block.getStateDefinition().getProperty(property.getKey());
                    assertTrue(known != null, name + " has no " + property.getKey());
                    assertEquals(property.getValue().getAsString(), valueName(state, known), name + " " + property.getKey());
                }
            }
        });
        assertEquals(List.of(WHITE_STONE_BRICKS, POLISHED_WHITE_STONE, MIRROR_STONE, CHALKRACK, STILL_LIGHT, WHITE_CORE_ORE,
                WHITE_STONE_PILLAR, POLISHED_WHITE_STONE, WHITE_STONE_BRICKS, WHITE_STONE), names);
    }

    /** A rule {@code nether_ring_5_dressing} with {@code part} that paints {@code block}. */
    private static void assertDressing(JsonObject rule, String part, String block) {
        JsonObject test = rule.getAsJsonObject("if_true");
        assertEquals(DRESSING, test.get("type").getAsString());
        assertEquals(part, test.get("part").getAsString());
        assertEquals(block, blockName(rule.getAsJsonObject("then_run")));
    }

    /**
     * A band below Y {@code below}: its top block ({@code floor} depth 0) {@code top} (null: not checked, a rule of its
     * own), the rest {@code rest}.
     */
    private static void assertBand(JsonObject rule, int below, String top, String rest) {
        JsonObject test = rule.getAsJsonObject("if_true");
        assertEquals("minecraft:not", test.get("type").getAsString());
        JsonObject above = test.getAsJsonObject("invert");
        assertEquals("minecraft:y_above", above.get("type").getAsString());
        assertEquals(below, above.getAsJsonObject("anchor").get("absolute").getAsInt());
        assertEquals(0, above.get("surface_depth_multiplier").getAsInt());
        assertFalse(above.get("add_stone_depth").getAsBoolean());
        JsonArray band = rule.getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals(2, band.size());
        JsonObject depth = band.get(0).getAsJsonObject().getAsJsonObject("if_true");
        assertEquals("minecraft:stone_depth", depth.get("type").getAsString());
        assertEquals("floor", depth.get("surface_type").getAsString());
        assertEquals(0, depth.get("offset").getAsInt());
        assertFalse(depth.get("add_surface_depth").getAsBoolean());
        assertEquals(0, depth.get("secondary_depth_range").getAsInt());
        if (top != null) {
            assertEquals(top, blockName(band.get(0).getAsJsonObject().getAsJsonObject("then_run")));
        }
        assertEquals(rest, blockName(band.get(1).getAsJsonObject()));
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static JsonArray surfaceRules() {
        return WorldgenData.read("noise_settings/nether.json").getAsJsonObject("surface_rule").getAsJsonArray("sequence");
    }

    /** The index of the two biomes' branch (exactly one). */
    private static int ownBranch(JsonArray rules) {
        int own = -1;
        for (int i = 0; i < rules.size(); i++) {
            JsonObject test = rules.get(i).getAsJsonObject().getAsJsonObject("if_true");
            if (test != null && test.has("biome_is") && test.get("biome_is").toString().equals(BIOMES)) {
                assertEquals(-1, own, "one branch");
                own = i;
            }
        }
        assertTrue(own >= 0, "the branch");
        return own;
    }

    private static void walk(JsonElement element, Consumer<JsonObject> visit) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> walk(e, visit));
        } else if (element.isJsonObject()) {
            visit.accept(element.getAsJsonObject());
            element.getAsJsonObject().entrySet().forEach(e -> walk(e.getValue(), visit));
        }
    }

    private static String blockName(JsonObject rule) {
        return rule.has("type") && rule.get("type").getAsString().equals("minecraft:block")
                ? rule.getAsJsonObject("result_state").get("Name").getAsString() : null;
    }
}
