"""Nether ring 4 surface rules (65, 78): writes the Nether ring 4 branch of noise_settings/nether.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/nether_ring4_surface.py

Stream W-nether's own script. Idempotent: the branch of the Nether ring 4 biomes is replaced in place, or, the first
time, inserted right after the Nether ring 3 branch (before vanilla's floor rule that turns holes below Y 32 into
lava); everything else stays byte-identical (a json round trip with indent 2 and CRLF reproduces the file exactly).

Blocks (78, N1 = A: the Nether ring 4 blocks of package 68 replace golf 1's vanilla placeholders): bone sand on the
floors (falls like sand: right above air it is bleached netherrack, the ceiling guard), bleached netherrack as the rock,
fossil stone on the bridges and ledges of the spires and in a band in the walls of their sea, purity crystal for the
columns (skin and core one material), basalt and magma for the bed of the lava sea. The sea-creature fossils of the
dunes (terrain, density function sea_fossils) are vanilla bone block, through the surface condition sea_fossil, before
every other rule.

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

BONE_SAND = CA + "bone_sand"
BLEACHED_NETHERRACK = CA + "bleached_netherrack"
PURITY_CRYSTAL = CA + "purity_crystal_block"
FOSSIL_STONE = CA + "fossil_stone"
FOSSIL_BONE = "bone_block"          # the sea-creature fossils (contract §3: vanilla bone block)
SCORCHED = "basalt"
SEA_FOSSIL = {"type": CA + "sea_fossil"}


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
SPIRES_ZONE = noise(R, 0.14, 10.0)    # where the spires weight (and with it the bridges) starts: threshold - ramp
SEL = "minecraft:nether_state_selector"
PILLARS = "nether_ring_4_pillars"
PILLAR_SKIN_FROM = 0.25               # pillars ring_4/nether/pillars: solid from 0.40 (ledge 0.28, head 0.25)
# The terrain has its columns where sea = spires weight x core > 0.5 (core = 1 - ring_edge, 48 wide); the crystal is
# only painted where both are at least ~0.75: region >= 0.20 and 32 blocks inside the ring, so no column skin lands on
# the walls of the slopes where the sea fades out.
COLUMNS_REGION_FROM = 0.20
COLUMNS_DEPTH_FROM = 32.0
BRIDGES = ["nether_ring_4_bridges_a", "nether_ring_4_bridges_b"]
BRIDGE_PAINT_BAND = 0.08              # bridges ring_4/nether/bridges: min(|a|, |b|) < 0.07
WALKWAY_BELOW = 44                    # bridge and ledge tops lie at Y 42 (the dunes from Y 40 up)
LAVA_SHORE_BELOW = 13                 # floors this low lie in the lava sea (fluid_levels.ring_4 = 10)
FOSSIL_BAND = (22, 30)                # a band of fossil stone in the walls of the spires' sea
CEILING_GUARD = depth("ceiling")      # the block right above air


def ring_depth(minimum):
    return {"type": CA + "ring_depth", "dimension": "minecraft:the_nether", "ring": "ring_4", "min_depth": minimum}


def walkway(then):
    """then, on a bone walkway: a bridge line or a column's ledge, anywhere the spires zone starts (also in the band
    where the biome is still bone_dunes), or any floor of the crystal_spires biome (a sequence acts as an OR)."""
    zone = [SPIRES_ZONE, NOT(y_above(WALKWAY_BELOW))]
    return seq(*[all_of(zone + [noise(b, -BRIDGE_PAINT_BAND, BRIDGE_PAINT_BAND)], then) for b in BRIDGES],
               all_of(zone + [noise(PILLARS, PILLAR_SKIN_FROM, 10.0)], then),
               cond(biome("crystal_spires"), then))


def nether():
    floor = depth("floor", 0, True)
    return cond(biome(*BIOMES), seq(
        # the sea-creature fossils of the dunes (terrain): bone everywhere in their bones, floors included
        cond(SEA_FOSSIL, block(FOSSIL_BONE)),
        # floors first: no crystal and no falling block on a floor that is not the column's own
        cond(floor, seq(
            # the bed of the lava sea
            cond(NOT(y_above(LAVA_SHORE_BELOW)), seq(
                cond(noise(SEL, 0.3, 10.0), block("magma_block")),
                block(SCORCHED))),
            # bone bridges and ledges: fossil stone, never a falling block (they are thin)
            walkway(block(FOSSIL_STONE)),
            # floors of the dunes and hollows (the surface depth, ~3-5 deep); bone sand falls, so a block of it that
            # would hang right above air (a thin floor over a tunnel, a geode or a doorway) is bleached netherrack
            # (the ceiling guard, like sandstone under the sand of a desert)
            cond(CEILING_GUARD, block(BLEACHED_NETHERRACK)),
            block(BONE_SAND))),
        # crystal columns (shape rule: the pillar noise of the terrain, from the lava sea up, where the terrain has them)
        all_of([noise(R, COLUMNS_REGION_FROM, 10.0), noise(PILLARS, PILLAR_SKIN_FROM, 10.0), y_above(4),
                ring_depth(COLUMNS_DEPTH_FROM)], block(PURITY_CRYSTAL)),
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
