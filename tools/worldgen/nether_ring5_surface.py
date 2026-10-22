"""Nether ring 5 surface rules (87): writes the Nether ring 5 branch of noise_settings/nether.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/nether_ring5_surface.py

Stream W-nether's own script. Idempotent: the branch of the Nether ring 5 biomes is replaced in place, or, the first
time, inserted right after the Nether ring 4 branch (before vanilla's floor rule that turns holes below Y 32 into
lava); everything else stays byte-identical (a json round trip with indent 2 and CRLF reproduces the file exactly).

Blocks (87: vanilla placeholders; 87b swaps the constants for the blocks of package 90): the floor's top block (Y 31)
mirror stone (smooth quartz), the rock below it chalkrack (diorite), the columns white stone pillars (quartz pillar),
everything else (plains, ceiling, walls, the cathedral's roof) white stone (calcite). The columns are a shape rule:
the surface condition nether_ring_5_pillar asks the terrain itself (nether_cathedrals) which columns stand.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"
BIOMES = ["white_halls", "hollow_cathedral"]
RING_4_BIOMES = ["bone_dunes", "crystal_spires", "geode_hollows"]

MIRROR_STONE = "smooth_quartz"      # 87b: corruptedadventure:mirror_stone
CHALKRACK = "diorite"               # 87b: corruptedadventure:chalkrack
WHITE_STONE = "calcite"             # 87b: corruptedadventure:white_stone (plains: polished_white_stone)
WHITE_STONE_PILLAR = "quartz_pillar"  # 87b: corruptedadventure:white_stone_pillar (axis y)
FLOOR_TOP = 31                      # NetherCathedrals floor_top (ring_5/nether/terrain.json)
PILLAR = {"type": CA + "nether_ring_5_pillar"}


# ---------------------------------------------------------------- builders
def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name if ":" in name else "minecraft:" + name}}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def y_above(y):
    return {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": 0, "add_stone_depth": False}


def depth(kind="floor", offset=0, add=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": add,
            "secondary_depth_range": secondary, "surface_type": kind}


def NOT(c):
    return {"type": "minecraft:not", "invert": c}


def biome(*names):
    return {"type": "minecraft:biome", "biome_is": [CA + n for n in names]}


def nether():
    return cond(biome(*BIOMES), seq(
        # the floor (top block at Y 31) and the solid rock under it, down to the bedrock (also under the columns)
        cond(NOT(y_above(FLOOR_TOP + 1)), seq(
            cond(depth("floor"), block(MIRROR_STONE)),
            block(CHALKRACK))),
        # the columns (grid and giant) from the floor to the ceiling and through it
        cond(PILLAR, block(WHITE_STONE_PILLAR)),
        # plains, ceiling, walls and the cathedral's roof
        block(WHITE_STONE)))


def dump(path, data):
    text = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n"
    with open(path, "wb") as f:
        f.write(text.encode("utf-8"))


def biome_list(rule):
    return rule.get("if_true", {}).get("biome_is")


def main():
    path = os.path.join(ROOT, "noise_settings", "nether.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    rules = data["surface_rule"]["sequence"]
    own = [i for i, r in enumerate(rules) if biome_list(r) == [CA + b for b in BIOMES]]
    if own:
        assert len(own) == 1, own
        rules[own[0]] = nether()
    else:
        ring_4 = [i for i, r in enumerate(rules) if biome_list(r) == [CA + b for b in RING_4_BIOMES]]
        assert len(ring_4) == 1, ring_4
        rules.insert(ring_4[0] + 1, nether())
    dump(path, data)
    print("Nether ring 5 surface written")


if __name__ == "__main__":
    main()
