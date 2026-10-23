"""Nether ring 5 surface rules (87, 87b): writes the Nether ring 5 branch of noise_settings/nether.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/nether_ring5_surface.py

Stream W-nether's own script. Idempotent: the branch of the Nether ring 5 biomes is replaced in place, or, the first
time, inserted right after the Nether ring 4 branch (before vanilla's floor rule that turns holes below Y 32 into
lava); everything else stays byte-identical (a json round trip with indent 2 and CRLF reproduces the file exactly).

Blocks (87b: stream B's blocks of package 90, before that vanilla stand-ins): the floor's top block (Y 31) mirror
stone, the rock below it chalkrack, the columns white stone pillars (axis y), the plains (Y 32 up to the highest plain)
polished white stone on top and white stone bricks below their top (the sides: the plains' edges), everything else
(ceiling, walls, the cathedral's roof) white stone. The columns are a shape rule: the surface condition
nether_ring_5_pillar asks the terrain itself (nether_cathedrals) which columns stand. The heights come from the
terrain's settings (ring_5/nether/terrain.json): after a change there, run this script again.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"
BIOMES = ["white_halls", "hollow_cathedral"]
RING_4_BIOMES = ["bone_dunes", "crystal_spires", "geode_hollows"]

# The blocks of stream B (90).
MIRROR_STONE = CA + "mirror_stone"
CHALKRACK = CA + "chalkrack"
WHITE_STONE = CA + "white_stone"
POLISHED_WHITE_STONE = CA + "polished_white_stone"
WHITE_STONE_BRICKS = CA + "white_stone_bricks"
WHITE_STONE_PILLAR = CA + "white_stone_pillar"
PILLAR = {"type": CA + "nether_ring_5_pillar"}


# ---------------------------------------------------------------- builders
def block(name, **properties):
    state = {"Name": name if ":" in name else "minecraft:" + name}
    if properties:
        state["Properties"] = properties
    return {"type": "minecraft:block", "result_state": state}


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


def halls():
    """The halls' settings of nether_cathedrals (floor_top, plains_height, ...)."""
    with open(os.path.join(ROOT, "density_function", "ring_5", "nether", "terrain.json"), encoding="utf-8") as f:
        return json.load(f)["settings"]["halls"]


def range_max(value):
    """The upper end of a Range (NetherCathedrals' codec takes a number or [min, max]), truncated like Java's (int)."""
    return int(value[-1] if isinstance(value, list) else value)


def nether():
    settings = halls()
    floor_top = settings["floor_top"]
    plains_top = floor_top + range_max(settings["plains_height"])
    return cond(biome(*BIOMES), seq(
        # the floor (top block at Y 31) and the solid rock under it, down to the bedrock (also under the columns)
        cond(NOT(y_above(floor_top + 1)), seq(
            cond(depth("floor"), block(MIRROR_STONE)),
            block(CHALKRACK))),
        # the columns (grid and giant) from the floor to the ceiling and through it
        cond(PILLAR, block(WHITE_STONE_PILLAR, axis="y")),
        # the plains (2-6 above the floor): polished on top, bricks below the top (their sides)
        cond(NOT(y_above(plains_top + 1)), seq(
            cond(depth("floor"), block(POLISHED_WHITE_STONE)),
            block(WHITE_STONE_BRICKS))),
        # ceiling, walls and the cathedral's roof
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
