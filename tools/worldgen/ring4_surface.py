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
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"

# The order of Ring4Biomes.WITH_TRANSITION.
RING_4 = ["cascade_vale", "crystal_lagoon", "lotus_shallows", "glowroot_swamp", "mist_fjords",
          "drowned_forest", "peat_moors", "reed_marsh", "sky_gardens", "withered_gardens", "sundered_reach"]
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


def ring_4_branch():
    return cond(biome(*RING_4), seq(*[cond(biome(name), surface(*SURFACES[name])) for name in RING_4]))


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
    assert sorted(SURFACES) == sorted(RING_4)
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
