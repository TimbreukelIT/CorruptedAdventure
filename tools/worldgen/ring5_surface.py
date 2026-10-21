"""Ring 5 surface rules (package 86): writes the ring 5 branch of noise_settings/overworld.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/ring5_surface.py

The surface rules are hand-written JSON (CLAUDE.md §9); this script is their source for ring 5, like
ring4_surface.py for ring 4. Edit the rules here, run the script, commit the script and the JSON together. It is
idempotent: one ring 5 branch (the six ring 5 biomes; the zone 4->5 keeps ring 4's sundered_reach) inside the rules
above the preliminary surface, right after the ring 4 branch (so before vanilla's rules), replaced if present;
everything else stays byte-identical (a json round trip with indent 2 and CRLF reproduces the file exactly).

Golf 1 (86) uses vanilla blocks only, a first version; the real ring 5 blocks of stream B follow after integration 1
(86b). Ring 5 is dry (V9-B), so there is no wet variant, and no block here falls: a falling block (86b's corrupted
sand) needs a ceiling guard like ring 4's. Bedrock stays vanilla's rule 0: it puts nothing on air (V3-A).
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


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def depth(kind="floor", offset=0, add=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": add,
            "secondary_depth_range": secondary, "surface_type": kind}


def biome(*names):
    return {"type": "minecraft:biome", "biome_is": [CA + n for n in names]}


def below(y):
    """Blocks below Y y."""
    return {"type": "minecraft:not", "invert": {"type": "minecraft:y_above", "anchor": {"absolute": y},
                                                "surface_depth_multiplier": 0, "add_stone_depth": False}}


TOP = depth()                       # the surface block
DEPTH = depth(add=True)             # the surface depth (3-6 blocks)
# The three blocks above air under an overhang: the underside of the inverted hills.
UNDERSIDE = depth("ceiling", offset=2)
STEEP = {"type": "minecraft:steep"}
# corrupted_wastes' pit floors and low places: the dead ground lies around Y 78 (V4-A, 83), its round pits are 2-4 deep;
# a surface below Y 76 is a pit floor or a low place. 83 shapes the pits; this needs no density function of its own.
LOW = below(76)


def surface(top, under):
    """A biome's surface: a top block and a layer of the surface depth. Both unconditional, so vanilla's grass and dirt
    never reach a ring 5 biome."""
    return [cond(TOP, block(top)), cond(DEPTH, block(under))]


def corrupted_wastes():
    """Dead soil; tuff on the pit floors and in the low places."""
    return seq(cond(TOP, seq(cond(LOW, block("tuff")), block("coarse_dirt"))), cond(DEPTH, block("dirt")))


def void_scars():
    """Tuff and stone; the walls of the scars and craters deepslate for contrast (the voidstone rims are 95): every block of
    a steep column above the preliminary surface, which is the upper part of a wall down to the void."""
    return seq(cond(STEEP, block("deepslate")), *surface("tuff", "stone"))


def inverted_hills():
    """Dead soil on top; the undersides of the hills hanging over the void stone, never dirt."""
    return seq(cond(UNDERSIDE, block("stone")), *surface("coarse_dirt", "dirt"))


# Vanilla blocks per biome (first version, 86): top, surface depth; or a function for a biome with more.
SURFACES = {
    "corrupted_wastes": corrupted_wastes,
    # The cubes and shifted layers (83) are terrain of the same blocks.
    "glitch_fields": ("tuff", "stone"),
    # The "perfect" vanilla meadow: this is also its final surface (86b keeps it).
    "false_meadow": ("grass_block", "dirt"),
    "void_scars": void_scars,
    "inverted_hills": inverted_hills,
    # Chunks and bridges over the void.
    "shattered_expanse": ("tuff", "stone"),
}


def ring_5_branch():
    rules = []
    for name in RING_5:
        rule = SURFACES[name]
        rules.append(cond(biome(name), rule() if callable(rule) else seq(*surface(*rule))))
    return cond(biome(*RING_5), seq(*rules))


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
    dump(path, data)
    print("ring 5 surface written")


if __name__ == "__main__":
    main()
