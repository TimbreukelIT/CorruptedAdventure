"""Ring 4 surface rules (change 64): writes the ring 4 branch of noise_settings/overworld.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/ring4_surface.py

The surface rules are hand-written JSON (CLAUDE.md §9); this script is their source for ring 4, like
ring3_surface.py for ring 3. Edit the rules here, run the script, commit the script and the JSON together. It is
idempotent: one ring 4 branch (all ring 4 biomes and the transition sundered_reach) inside the rules above the
preliminary surface, right after the ring 3 branch (so before vanilla's rules), replaced if present; everything else
stays byte-identical (a json round trip with indent 2 and CRLF reproduces the file exactly).

Golf 1 (64) uses vanilla blocks only, a first version; the real ring 4 blocks of stream B follow after integration 1.
Sand and gravel never hang: where the surface reaches a ceiling (a thin island, a lake floor over a cave) they become
sandstone or stone, as in vanilla.

verdant_falls (the descent of the zone 3->4) reads the terrain through the condition corruptedadventure:ring4_threshold
(Q7 = A): calcite (travertine) on the rims of the basins and the lips of the falls (density function
ring_4/overworld/rim, the terrain's island_output rim since W-terrein 2c), stone on the exact walls (ring_4/overworld/cliff)
and on steep slopes (ring_4/overworld/slope, decision 2026-10-15), grass with moss patches on the treads, sand banks
along the foot river (turtles spawn on sand only, below Y 67) and lake and brook beds of sand, clay and gravel.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"

# The order of Ring4Biomes.WITH_TRANSITION.
RING_4 = ["cascade_vale", "crystal_lagoon", "lotus_shallows", "glowroot_swamp", "mist_fjords",
          "drowned_forest", "peat_moors", "reed_marsh", "sky_gardens", "withered_gardens", "sundered_reach",
          "verdant_falls"]
RING_3 = ["ashen_wastes", "volcanic_peaks", "ember_woods", "basalt_flows", "sulfur_fields", "dried_seabed",
          "titan_fields", "dormant_caldera", "steam_fens"]


# ---------------------------------------------------------------- builders
def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name if ":" in name else "minecraft:" + name}}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def depth(kind="floor", offset=0, add=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": add,
            "secondary_depth_range": secondary, "surface_type": kind}


def water(offset, multiplier, add_stone_depth):
    return {"type": "minecraft:water", "offset": offset, "surface_depth_multiplier": multiplier,
            "add_stone_depth": add_stone_depth}


def biome(*names):
    return {"type": "minecraft:biome", "biome_is": [CA + n for n in names]}


def noise(name, lo):
    return {"type": "minecraft:noise_threshold", "noise": name, "min_threshold": lo, "max_threshold": 1.7976931348623157e+308}


def island(key, lo, hi=1.7976931348623157e+308):
    """True where the 2D ring 4 terrain value behind the density function ring_4/overworld/<key> lies in [lo, hi)."""
    return {"type": CA + "ring4_threshold", "density_function": CA + "ring_4/overworld/" + key, "min": lo, "max": hi}


def speckle(name):
    """True for about half the blocks, at random per block (vanilla's vertical gradient over a span far wider than the
    terrain: 0.56 at Y 70, 0.48 at Y 110)."""
    return {"type": "minecraft:vertical_gradient", "random_name": CA + "verdant_wall_" + name,
            "true_at_and_below": {"absolute": -160}, "false_at_and_above": {"absolute": 360}}


def below(y):
    """Blocks below Y y."""
    return {"type": "minecraft:not", "invert": {"type": "minecraft:y_above", "anchor": {"absolute": y},
                                                "surface_depth_multiplier": 0, "add_stone_depth": False}}


STEEP = {"type": "minecraft:steep"}
TOP = depth()                       # the surface block
DEPTH = depth(add=True)             # the surface depth (3-6 blocks)
CEILING = depth("ceiling")          # the block right above air: sand or gravel there would fall
DRY_TOP = water(-1, 0, False)       # at most one block under the water level: land and the shore
DRY_DEPTH = water(-6, -1, True)     # vanilla's test for the layer under land
# Each falling block with its own solid stand-in where it would hang.
FALLING = {"sand": "sandstone", "gravel": "stone"}
# Clay patches (~60 blocks, vanilla's surface noise) in the swamp's lake beds: axolotls spawn only on clay.
CLAY_PATCHES = noise("minecraft:surface", 0.12)
# verdant_falls: the travertine rims and fall lips, the exact walls (both 0..1 from the terrain).
RIM = island("rim", 0.5)
# Polish: a rim value of 0.75 or more also crusts the wet side (a lake's threshold under the water).
RIM_WET = island("rim", 0.75)
CLIFF = island("cliff", 0.5)
# Down a lip's face to the water below: the step between two treads is 8 blocks.
RIM_DEPTH = depth(offset=7)
# Polish: the lip's face is not all white: a calcite crust 1-5 deep (vanilla's secondary surface noise), below it
# moss and stone in patches with some calcite.
RIM_CRUST = depth(offset=1, secondary=4)
FACE_MOSS = noise("minecraft:surface_secondary", 0.1)
FACE_STONE = noise("minecraft:patch", -0.05)
# The banks of the foot river: turtles spawn on sand below Y 67 (sea level + 4).
SHORE = below(66)
MOSS_PATCHES = noise("minecraft:surface", 0.25)
MOSSY_STONE = noise("minecraft:surface_secondary", 0.2)
# verdant_falls' steep slopes (decision 2026-10-15): the terrain's own slope (ring_4/overworld/slope, blocks of rise per
# block, the steepest least-squares gradient over 5 x 5 columns of the column and its neighbours; the same in every
# direction and at a chunk's edge), not vanilla's minecraft:steep (one direction per axis, a window of three columns,
# clamped to the chunk: it marked loose columns on the treads). Stone from SLOPE_ROCK on, and from SLOPE_PATCHY on in
# the patches of ROCK_PATCHES (~30 blocks), so the steeper risers turn to rock in stretches.
SLOPE_ROCK = island("slope", 2.0)
SLOPE_PATCHY = island("slope", 1.5)
ROCK_PATCHES = noise(CA + "ring_4_rock_patches", 0.0)
GRAVEL_PATCHES = noise("minecraft:patch", 0.2)
# The gorges' walls (polish round): no stripes, but patches in the rock face. Mossy walls over about half the gorges'
# length (vanilla's secondary surface noise, ~64 blocks), andesite patches (the patch noise), each speckled at random per
# block (about a quarter of the blocks), so the patches have no straight edges or bands.
MOSSY_WALL = noise("minecraft:surface_secondary", 0.0)
ANDESITE_WALL = noise("minecraft:patch", 0.05)


def put(name):
    """A block; a falling one (sand, gravel) becomes its stand-in where the surface reaches a ceiling (a thin island, a
    lake bed over a cave), as in vanilla."""
    if isinstance(name, dict):
        return name
    if name in FALLING:
        return seq(cond(CEILING, block(FALLING[name])), block(name))
    return block(name)


def surface(land_top, land_under, wet_top, wet_under, steep=None):
    """A biome's surface: a top block and a layer of the surface depth, each on land and under water; optionally its
    own top on steep slopes with stone below. Every path ends in a block, so vanilla's grass and dirt never reach a
    ring 4 biome."""
    rules = []
    if steep:
        rules.append(cond(STEEP, seq(cond(TOP, seq(cond(DRY_TOP, put(steep)), put(wet_top))), cond(DEPTH, block("stone")))))
    rules.append(cond(TOP, seq(cond(DRY_TOP, put(land_top)), put(wet_top))))
    rules.append(cond(DEPTH, seq(cond(DRY_DEPTH, put(land_under)), put(wet_under))))
    return seq(*rules)


# Vanilla blocks per biome (first version): land top, land under, wet top, wet under [, steep top].
SURFACES = {
    # Mythical band: green and clear; clear lake beds of sand, the swamp on mud with clay patches.
    "cascade_vale": ("grass_block", "dirt", "sand", "sand"),
    "crystal_lagoon": ("sand", "sand", "sand", "sand"),
    "lotus_shallows": ("grass_block", "dirt", "clay", "dirt"),
    "glowroot_swamp": ("grass_block", "dirt", seq(cond(CLAY_PATCHES, block("clay")), block("mud")), "mud"),
    # Grass on the flat tops (rabbits spawn there), moss and stone on the cliffs.
    "mist_fjords": ("grass_block", "dirt", "gravel", "gravel", "moss_block"),
    # Blighted band: dead soil and mud.
    "drowned_forest": ("coarse_dirt", "dirt", "mud", "mud"),
    "peat_moors": ("podzol", "mud", "mud", "mud"),
    "reed_marsh": ("grass_block", "dirt", "mud", "mud"),
    # Height biomes: sky lakes on clay (sand would hang under thin islands).
    "sky_gardens": ("grass_block", "dirt", "clay", "dirt"),
    "withered_gardens": ("coarse_dirt", "dirt", "coarse_dirt", "dirt"),
    # The zone 4->5 drains to grey: bare, the abyss floor of stone.
    "sundered_reach": ("coarse_dirt", "stone", "stone", "stone"),
}


def verdant_falls():
    """The descent of the zone 3->4 (Q7 = A): calcite on the rims and lips (down the lip's face), stone with mossy
    cobblestone on the exact walls and on steep slopes (the terrain's slope, in patches; on the gorges' faces patches of
    mossy cobblestone and andesite), sand banks along the foot river, grass with moss on the treads; the beds of lakes,
    brooks and splash pools sand with clay and gravel patches."""
    calcite = block("calcite")
    face = seq(cond(RIM_CRUST, calcite), cond(FACE_MOSS, block("mossy_cobblestone")), cond(FACE_STONE, block("stone")), calcite)
    bed = seq(cond(CLAY_PATCHES, block("clay")), cond(GRAVEL_PATCHES, put("gravel")), put("sand"))
    wall_top = seq(cond(MOSS_PATCHES, block("moss_block")), cond(MOSSY_STONE, block("mossy_cobblestone")), block("stone"))
    wall = seq(cond(TOP, seq(cond(DRY_TOP, wall_top), bed)), cond(DEPTH, block("stone")))
    # The exact walls of the gorges: every block of the face (not only under a floor), speckled into patches; the rest
    # of the surface depth stone (else it would fall through to the treads' dirt where the slope stays under SLOPE_ROCK).
    wall_face = seq(cond(MOSSY_WALL, cond(speckle("a"), cond(speckle("b"), block("mossy_cobblestone")))),
                    cond(ANDESITE_WALL, cond(speckle("c"), cond(speckle("d"), block("andesite")))),
                    cond(DEPTH, block("stone")))
    return seq(
        cond(RIM, seq(cond(TOP, seq(cond(DRY_TOP, calcite), cond(RIM_WET, calcite))),
                      cond(RIM_DEPTH, cond(DRY_DEPTH, face)))),
        cond(CLIFF, seq(cond(TOP, seq(cond(DRY_TOP, wall_top), bed)), wall_face)),
        cond(SLOPE_ROCK, wall),
        cond(SLOPE_PATCHY, cond(ROCK_PATCHES, wall)),
        cond(SHORE, seq(cond(TOP, cond(DRY_TOP, put("sand"))), cond(DEPTH, cond(DRY_DEPTH, put("sand"))))),
        # The treads' unconditional top and layer, flat in this branch like every other biome's.
        *surface(seq(cond(MOSS_PATCHES, block("moss_block")), block("grass_block")), "dirt", bed, "dirt")["sequence"])


# Biomes with a surface of their own shape (not the five-block pattern of SURFACES).
SPECIAL = {"verdant_falls": verdant_falls}


# ---------------------------------------------------------------- W-terrein-b (74): the world trees
def tree_part(part):
    """True in the blocks of a world tree (terrain, Q7 = A) that are part: log (trunk), wood (branches, roots), leaves,
    or any of them."""
    return {"type": CA + "world_tree", "part": part}


def world_tree():
    """The world trees of ring 4 (74): the trunk moonpetal_log, branches and roots moonpetal_wood (bark all round), the
    crown persistent moonpetal_leaves (they never decay, Q7 = A). First in the ring 4 branch: whatever biome the tree
    stands in, its blocks are wood and leaves, never the biome's soil. One condition in front (any part), so a block
    outside the trees costs one test."""
    leaves = {"type": "minecraft:block", "result_state": {"Name": CA + "moonpetal_leaves",
                                                          "Properties": {"distance": "7", "persistent": "true",
                                                                         "waterlogged": "false"}}}
    return cond(tree_part("any"), seq(cond(tree_part("log"), block(CA + "moonpetal_log")),
                                      cond(tree_part("wood"), block(CA + "moonpetal_wood")),
                                      leaves))


def ring_4_branch():
    return cond(biome(*RING_4), seq(world_tree(),
                                    *[cond(biome(name), SPECIAL[name]() if name in SPECIAL else surface(*SURFACES[name]))
                                      for name in RING_4]))


# ---------------------------------------------------------------- write
def dump(path, data):
    text = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n"
    with open(path, "wb") as f:
        f.write(text.encode("utf-8"))


def biomes_of(rule):
    return rule.get("if_true", {}).get("biome_is", []) if rule.get("if_true", {}).get("type") == "minecraft:biome" else []


def is_ring_4(rule):
    biomes = biomes_of(rule)
    return len(biomes) > 0 and all(b in [CA + n for n in RING_4] for b in biomes)


def is_ring_3(rule):
    biomes = biomes_of(rule)
    return len(biomes) > 0 and all(b in [CA + n for n in RING_3] for b in biomes)


def main():
    assert sorted(list(SURFACES) + list(SPECIAL)) == sorted(RING_4)
    path = os.path.join(ROOT, "noise_settings", "overworld.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    top = data["surface_rule"]["sequence"]
    above = [r for r in top if r.get("if_true", {}).get("type") == "minecraft:above_preliminary_surface"]
    assert len(above) == 1, "one block of rules above the preliminary surface"
    rules = above[0]["then_run"]["sequence"]
    rules[:] = [r for r in rules if not is_ring_4(r)]
    ring3 = [i for i, r in enumerate(rules) if is_ring_3(r)]
    assert len(ring3) == 1, ring3
    rules.insert(ring3[0] + 1, ring_4_branch())
    dump(path, data)
    print("ring 4 surface written")


if __name__ == "__main__":
    main()
