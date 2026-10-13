"""Nether ring 4 surface rules (65): writes the Nether ring 4 branch of noise_settings/nether.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/nether_ring4_surface.py

Stream W-nether's own script. Idempotent: the branch of the Nether ring 4 biomes is replaced in place, or, the first
time, inserted right after the Nether ring 3 branch (before vanilla's floor rule that turns holes below Y 32 into
lava); everything else stays byte-identical (a json round trip with indent 2 and CRLF reproduces the file exactly).

Golf 1 paints with vanilla placeholders (contract §3: the Nether ring 4 blocks are stream B's package 68); the names
below map each future block to its placeholder, so the swap after integration 1 is a change of these constants:
- bone sand -> white concrete powder (falls like sand), bleached netherrack -> end stone, purity crystal -> calcite with a
  quartz core, fossil stone -> bone block (the bridges and ledges of the spires, a band in the walls of their sea),
  the bed of the lava sea -> basalt. Bone sand falls: right above air it is bleached netherrack (the ceiling guard).

Shape rules follow the same noises as the density functions (ring_4/nether/*: region axis, pillars), not the
biome; dressing rules (floors, fills) follow the biome.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"
BIOMES = ["bone_dunes", "crystal_spires", "geode_hollows"]
RING_3_BIOMES = ["frozen_lava_sea", "rime_forest", "glacier_rifts"]

BONE_SAND = "white_concrete_powder"
BLEACHED_NETHERRACK = "end_stone"
PURITY_CRYSTAL = "calcite"
PURITY_CRYSTAL_CORE = "quartz_block"
FOSSIL_STONE = "bone_block"
SCORCHED = "basalt"


# ---------------------------------------------------------------- builders
def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name if ":" in name else "minecraft:" + name}}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def all_of(conds, then):
    for c in reversed(conds):
        then = cond(c, then)
    return then


def noise(name, lo=-10.0, hi=10.0):
    return {"type": "minecraft:noise_threshold", "noise": name if ":" in name else CA + name,
            "min_threshold": lo, "max_threshold": hi}


def y_above(y):
    return {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": 0, "add_stone_depth": False}


def depth(kind="floor", offset=0, add=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": add,
            "secondary_depth_range": secondary, "surface_type": kind}


def NOT(c):
    return {"type": "minecraft:not", "invert": c}


def biome(*names):
    return {"type": "minecraft:biome", "biome_is": [CA + n for n in names]}


R = "nether_ring_4_region"            # thresholds = NetherRing4Features.SPIRES_THRESHOLD / HOLLOWS_THRESHOLD
SPIRES = noise(R, 0.14, 10.0)         # where the spires weight starts (terrain: threshold - ramp)
SEL = "minecraft:nether_state_selector"
PILLARS = "nether_ring_4_pillars"
PILLAR_SKIN_FROM = 0.25               # pillars ring_4/nether/pillars: solid from 0.40 (ledge 0.28, head 0.25)
PILLAR_CORE_FROM = 0.55
LAVA_SHORE_BELOW = 13                 # floors this low lie in the lava sea (fluid_levels.ring_4 = 10)
FOSSIL_BAND = (22, 30)                # a band of fossil stone in the walls of the spires' sea
CEILING_GUARD = depth("ceiling")      # the block right above air


def nether():
    floor = depth("floor", 0, True)
    return cond(biome(*BIOMES), seq(
        # crystal columns (shape rule: the pillar noise of the terrain, from the lava sea up)
        all_of([SPIRES, noise(PILLARS, PILLAR_SKIN_FROM, 10.0), y_above(4)], seq(
            cond(noise(PILLARS, PILLAR_CORE_FROM, 10.0), block(PURITY_CRYSTAL_CORE)),
            block(PURITY_CRYSTAL))),
        cond(floor, seq(
            # the bed of the lava sea
            cond(NOT(y_above(LAVA_SHORE_BELOW)), seq(
                cond(noise(SEL, 0.3, 10.0), block("magma_block")),
                block(SCORCHED))),
            # bone bridges and ledges in the spires: not a falling block, they are thin
            cond(biome("crystal_spires"), block(FOSSIL_STONE)),
            # floors of the dunes and hollows (the surface depth, ~3-5 deep); bone sand falls, so a block of it that
            # would hang right above air (a thin floor over a tunnel, a geode or a doorway) is bleached netherrack
            # (the ceiling guard, like sandstone under the sand of a desert)
            cond(CEILING_GUARD, block(BLEACHED_NETHERRACK)),
            block(BONE_SAND))),
        # a band of fossil stone in the walls of the spires' sea
        all_of([biome("crystal_spires"), y_above(FOSSIL_BAND[0]), NOT(y_above(FOSSIL_BAND[1])), noise(SEL, -0.2, 10.0)],
               block(FOSSIL_STONE)),
        block(BLEACHED_NETHERRACK)))


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
        ring_3 = [i for i, r in enumerate(rules) if biome_list(r) == [CA + b for b in RING_3_BIOMES]]
        assert len(ring_3) == 1, ring_3
        rules.insert(ring_3[0] + 1, nether())
    dump(path, data)
    print("Nether ring 4 surface written")


if __name__ == "__main__":
    main()
