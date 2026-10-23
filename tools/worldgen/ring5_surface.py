"""Ring 5 surface rules (package 86): writes the ring 5 branch of noise_settings/overworld.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/ring5_surface.py

The surface rules are hand-written JSON (CLAUDE.md §9); this script is their source for ring 5, like
ring4_surface.py for ring 4. Edit the rules here, run the script, commit the script and the JSON together. It is
idempotent: one ring 5 branch (the six ring 5 biomes; the zone 4->5 keeps ring 4's sundered_reach) inside the rules
above the preliminary surface, right after the ring 4 branch (so before vanilla's rules), and one top-level rule for
the base rock of the column right after vanilla's deepslate gradient (the last rule), both replaced if present;
everything else stays byte-identical (a json round trip with indent 2 and CRLF reproduces the file exactly).

86b: the real ring 5 blocks of stream B (88). The base rock of all six biomes is corrupted stone, also under the false
meadow (whoever digs finds it is wrong); deepslate keeps vanilla's gradient at Y 0-8, the same boundary as ring 4.
Ring 5 is dry (V9-B), so there is no wet variant. The one falling block, corrupted sand, has a ceiling guard like ring
4's: where it would hang over air it is corrupted stone. Bedrock stays vanilla's rule 0: it puts nothing on air (V3-A).

94: one more top-level rule of ring 5 right after the bedrock floor (and ring 3's cave walls), guarded per column by
corruptedadventure:ring_5_ground (part core; ring 5 and the void edge, never the zone 4->5), replaced if present:
- the clean missing chunks (V1-A): every stone block of the chunk becomes air (the bedrock floor stays for the feature
  ring_5/missing_chunks, which also dams the lava under Y -54 round it).
In the ring 5 branch: glitch_fields' glitch blocks in specks and patches of 1-4 on tops and undersides (V3-A).
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"

# The order of Ring5Biomes.ALL.
RING_5 = ["corrupted_wastes", "glitch_fields", "false_meadow", "void_scars", "inverted_hills", "shattered_expanse"]
# The order of Ring4Biomes.WITH_TRANSITION (to find the ring 4 branch).
RING_4 = ["cascade_vale", "crystal_lagoon", "lotus_shallows", "glowroot_swamp", "mist_fjords",
          "drowned_forest", "peat_moors", "reed_marsh", "sky_gardens", "withered_gardens", "sundered_reach",
          "verdant_falls"]


# ---------------------------------------------------------------- builders
def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name if ":" in name else "minecraft:" + name}}


def ring_reach(ring):
    return {"type": CA + "ring_reach", "dimension": "minecraft:overworld", "ring": ring}


def NOT(c):
    return {"type": "minecraft:not", "invert": c}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def depth(kind="floor", offset=0, add=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": add,
            "secondary_depth_range": secondary, "surface_type": kind}


def biome(*names):
    return {"type": "minecraft:biome", "biome_is": [CA + n for n in names]}


def ground(part):
    return {"type": CA + "ring_5_ground", "part": part}


def speckle(salt, chance, size=1):
    return {"type": CA + "ring_5_speckle", "salt": salt, "chance": chance, "size": size}


def below(y):
    """Blocks below Y y."""
    return {"type": "minecraft:not", "invert": {"type": "minecraft:y_above", "anchor": {"absolute": y},
                                                "surface_depth_multiplier": 0, "add_stone_depth": False}}


TOP = depth()                       # the surface block
DEPTH = depth(add=True)             # the surface depth (3-6 blocks)
CEILING = depth("ceiling")          # the block right above air: sand there would fall
# The three blocks above air under an overhang: the underside of the inverted hills.
UNDERSIDE = depth("ceiling", offset=2)
STEEP = {"type": "minecraft:steep"}
# corrupted_wastes' pit floors and low places: the dead ground lies around Y 78 (V4-A, 83), its round pits are 2-4 deep;
# a surface below Y 76 is a pit floor or a low place. 83 shapes the pits (Ring5Inner.pitAt, Java only, no output of
# the field); this needs no density function of its own.
LOW = below(76)

# The blocks of stream B (88).
GRASS = CA + "corrupted_grass_block"
DIRT = CA + "corrupted_dirt"
STONE = CA + "corrupted_stone"
SAND = CA + "corrupted_sand"
# Each falling block with its solid stand-in where it would hang over air.
FALLING = {SAND: STONE}
# The blocks of stream B (89).
GLITCH = CA + "glitch_block"
# glitch_fields (V3-A): specks and patches of 1-4 glitch blocks (2 x 2 cubes) on about 3 % of the tops and undersides.
GLITCH_SPECKS = speckle(5101, 0.03, 2)


def put(name):
    """A block; a falling one becomes its stand-in where the surface reaches a ceiling (a thin edge over a scar or the
    void), so it never hangs over air."""
    if name in FALLING:
        return seq(cond(CEILING, block(FALLING[name])), block(name))
    return block(name)


def surface(top, under):
    """A biome's surface: a top block and a layer of the surface depth. Both unconditional, so vanilla's grass and dirt
    never reach a ring 5 biome."""
    return [cond(TOP, put(top)), cond(DEPTH, put(under))]


def corrupted_wastes():
    """Dead soil; corrupted sand on the pit floors and in the low places."""
    return seq(cond(TOP, seq(cond(LOW, put(SAND)), block(GRASS))), cond(DEPTH, block(DIRT)))


def glitch_fields():
    """Corrupted stone with glitch blocks in specks and patches on the tops and the undersides (cubes, layers)."""
    return seq(cond(TOP, seq(cond(GLITCH_SPECKS, block(GLITCH)), block(STONE))),
               cond(CEILING, seq(cond(GLITCH_SPECKS, block(GLITCH)), block(STONE))),
               cond(DEPTH, block(STONE)))


def void_scars():
    """Corrupted stone; the walls of the scars and craters deepslate for contrast (the voidstone rims are 95): every block
    of a steep column above the preliminary surface, which is the upper part of a wall down to the void."""
    return seq(cond(STEEP, block("deepslate")), *surface(STONE, STONE))


def inverted_hills():
    """Dead soil on top, also on a thin edge; below it the undersides of the hills hanging over the void corrupted stone,
    never dirt (before the surface depth, after the top block)."""
    top, under = surface(GRASS, DIRT)
    return seq(top, cond(UNDERSIDE, block(STONE)), under)


# The blocks per biome (86b): top, surface depth; or a function for a biome with more.
SURFACES = {
    "corrupted_wastes": corrupted_wastes,
    # The cubes and shifted layers (83) are terrain of the same blocks, with glitch blocks in them (94).
    "glitch_fields": glitch_fields,
    # The "perfect" vanilla meadow: vanilla grass on vanilla dirt (only the base rock below is wrong).
    "false_meadow": ("grass_block", "dirt"),
    "void_scars": void_scars,
    "inverted_hills": inverted_hills,
    # Chunks and bridges over the void.
    "shattered_expanse": (STONE, STONE),
}


def ring_5_branch():
    rules = []
    for name in RING_5:
        rule = SURFACES[name]
        rules.append(cond(biome(name), rule() if callable(rule) else seq(*surface(*rule))))
    return cond(biome(*RING_5), seq(*rules))


def ring_5_base_rock():
    """The rest of the column (below the surface depth, the undersides of overhangs): corrupted stone instead of the default
    stone. After vanilla's deepslate gradient, so deepslate keeps its boundary. The biome condition looks the biome up per
    block (RingReachCondition), so only columns where ring 4 (with the zone 4->5, sundered_reach) or the void (void_edge)
    can have weight ask it; everywhere else in ring 5's reach every biome is a ring 5 biome, so the stone is the same
    without the lookup (like ring 3's deep rule)."""
    return cond(ring_reach("ring_5"), seq(
        cond(NOT(ring_reach("ring_4")), cond(NOT(ring_reach("void")), block(STONE))),
        cond(biome(*RING_5), block(STONE))))


def ring_5_ground():
    """The top-level ring 5 rule (94): the clean missing chunks; see the module doc."""
    return cond(ground("core"), seq(
        cond(ground("missing_chunk"), block("air"))))


def is_ring_5_ground(rule):
    return rule.get("if_true") == ground("core")


def is_bedrock_floor(rule):
    return rule.get("if_true", {}).get("random_name") == "minecraft:bedrock_floor"


def is_ring_3_cave_walls(rule):
    return rule.get("if_true") == ring_reach("ring_3")


# ---------------------------------------------------------------- write
def dump(path, data):
    text = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n"
    with open(path, "wb") as f:
        f.write(text.encode("utf-8"))


def biomes_of(rule):
    return rule.get("if_true", {}).get("biome_is", []) if rule.get("if_true", {}).get("type") == "minecraft:biome" else []


def only_of(rule, names):
    biomes = biomes_of(rule)
    return len(biomes) > 0 and all(b in [CA + n for n in names] for b in biomes)


def blocks_in(node):
    if isinstance(node, list):
        return [b for n in node for b in blocks_in(n)]
    if not isinstance(node, dict):
        return []
    own = [node["result_state"]["Name"]] if node.get("type") == "minecraft:block" else []
    return own + [b for v in node.values() for b in blocks_in(v)]


def is_base_rock(rule):
    """The ring 5 guard and nothing but corrupted stone (also an older form of this rule): another top-level ring 5 rule
    (93's cave walls, say) keeps its place."""
    return rule.get("if_true") == ring_reach("ring_5") and set(blocks_in(rule)) == {STONE}


def main():
    assert sorted(SURFACES) == sorted(RING_5)
    path = os.path.join(ROOT, "noise_settings", "overworld.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    top = data["surface_rule"]["sequence"]
    above = [r for r in top if r.get("if_true", {}).get("type") == "minecraft:above_preliminary_surface"]
    assert len(above) == 1, "one block of rules above the preliminary surface"
    rules = above[0]["then_run"]["sequence"]
    rules[:] = [r for r in rules if not only_of(r, RING_5)]
    ring4 = [i for i, r in enumerate(rules) if only_of(r, RING_4)]
    assert len(ring4) == 1, ring4
    rules.insert(ring4[0] + 1, ring_5_branch())
    top[:] = [r for r in top if not is_base_rock(r)]
    gradient = top[-1].get("if_true", {})
    assert gradient.get("type") == "minecraft:vertical_gradient" and gradient.get("random_name") == "minecraft:deepslate", \
        "vanilla's deepslate gradient is the last top-level rule"
    top.append(ring_5_base_rock())
    top[:] = [r for r in top if not is_ring_5_ground(r)]
    assert is_bedrock_floor(top[0]), "vanilla's bedrock floor is the first top-level rule"
    at = 2 if is_ring_3_cave_walls(top[1]) else 1
    top.insert(at, ring_5_ground())
    dump(path, data)
    print("ring 5 surface written")


if __name__ == "__main__":
    main()
