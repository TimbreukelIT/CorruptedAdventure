"""Nether ring 3 surface rules (39c, per biome since 53b): writes the Nether ring 3 branch of noise_settings/nether.json
and the surface noises noise/nether_ring_3_frost.json (the shape noises of the frozen falls belong to the terrain and
are written by hand next to the density functions).

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/nether_ring3_surface.py

Stream W-nether's own script (moved out of ring3_surface.py in 53b). Idempotent: the branch of the Nether ring 3 biomes
(or, the first time, of placeholder_nether_ring_3) is replaced in place; everything else stays byte-identical (a json
round trip with indent 2 and CRLF reproduces the file exactly).

Two kinds of rules:
- shape rules follow the terrain block for block, so they read the same noises as the density functions (region axis,
  pillars, cracks, falls, drapes) and not the biome (the biome is quantised to 4 blocks and fuzzy at its borders);
- dressing rules (floors, fills, ceilings) follow the biome, the one the player sees, spawns in and freezes in.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"
BIOMES = ["frozen_lava_sea", "rime_forest", "glacier_rifts"]


# ---------------------------------------------------------------- builders
def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name if ":" in name else "minecraft:" + name}}


def mod(name):
    return block(CA + name)


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


TOP = depth()

# ---------------------------------------------------------------- noises and thresholds
R = "nether_ring_3_region"           # thresholds = NetherRing3Features.RIME_THRESHOLD / RIFTS_THRESHOLD
SEA = noise(R, -0.18, 0.18)
RIFTS = noise(R, -10.0, -0.18)
SEL = "minecraft:nether_state_selector"
PILLARS = "nether_ring_3_pillars"
CRACK_PATCHES = "nether_ring_3_crack_patches"
CRACKS = "nether_ring_3_cracks"
FALLS = "nether_ring_3_falls"        # density function ring_3/nether/falls: streaks where FALLS > FALLS_FROM
FALLS_FROM = 0.25
DRAPES = "nether_ring_3_drapes"      # density function ring_3/nether/drapes: curtains where |DRAPES| < DRAPE_BAND
DRAPE_BAND = 0.12
DRAPE_PATCHES = "nether_ring_3_drape_patches"
DRAPE_PATCHES_FROM = 0.15
# The curtain paint: inside the terrain's band (DRAPE_BAND, DRAPE_PATCHES_FROM) only where the drapes term lowers the
# ceiling by about 2 blocks or more (|DRAPES| < 0.09 and DRAPE_PATCHES >= 0.25 give 12 x 0.36 x 0.5), and only the
# curtain itself (at most DRAPE_DEPTH above the air below), not the ceiling mass above it.
DRAPE_PAINT_BAND = 0.09
DRAPE_PAINT_PATCHES_FROM = 0.25
DRAPE_DEPTH = 12
FROST = "nether_ring_3_frost"
ICEFALLS = "nether_ring_3_icefalls"  # density function ring_3/nether/icefalls (54): ribs on the rift walls from 0.25
# The ice-fall paint starts a little later than the term (from 0.3 the rib stands out by about a block) and covers only
# the wall layer: the rift edge lies at |RIFTS| = 0.05 + 0.07 * gradient(Y 34-96) (+- the wobble 0.015, - the rib up
# to 0.025, see density_function ring_3/nether/glacier); per Y band the paint reaches 0.035 past the band's widest edge,
# a layer of a few blocks. The glacier's inside keeps its packed Nether ice (the blue-ice veins match on it).
ICEFALLS_FROM = 0.3
RIFTS_NOISE = "nether_ring_3_rifts"
ICEFALL_LAYERS = [(30, 50, 0.103), (50, 70, 0.126), (70, 90, 0.148), (90, 113, 0.155)]
# Glowing seams on the rift floors (54): rare warm spots where the cracks noise crosses zero inside its strongest
# patches, below RIFT_FLOOR_BELOW; mostly glowing fissure, the odd magma block.
SEAM_BAND = 0.02
SEAM_PATCHES_FROM = 0.4
RIFT_FLOOR_BELOW = 40

# The warm ring around a crack (G2-WN3 = A): no ice within ~4-6 blocks of a crack that is really open.
WARM_PATCHES_FROM = 0.36
WARM_BAND = 0.15
# The warm ring of the rifts (54b): their low floors dip to the open lava at Y 22 (the Nether-3 lava level); below this
# Y a rift floor is cold lava with the odd magma block, no ice plates and no rime. The glacier masses start here too.
RIFT_WARM_BELOW = 30


def outside_warm_ring(then):
    """then, unless the block lies in a warm ring (a sequence of the two ways out acts as an OR)."""
    return seq(cond(noise(CRACK_PATCHES, -10.0, WARM_PATCHES_FROM), then),
               cond(NOT(noise(CRACKS, -WARM_BAND, WARM_BAND)), then))

NOISES = {
    "nether_ring_3_frost": (-5, [1.0, 0.5]),
}


def ice_fall_layer(then):
    """then, in the wall layer of a rift (per Y band: |RIFTS| below the band's paint limit)."""
    return seq(*[all_of([y_above(lo), NOT(y_above(hi)), noise(RIFTS_NOISE, -limit, limit)], then)
                 for lo, hi, limit in ICEFALL_LAYERS])


def frozen_lava():
    """Cold lava with a skin of Nether ice: vertical streaks (the surface noise is 2D) of lava showing through the ice."""
    return seq(cond(noise(SEL, 0.2), mod("cold_lava")), mod("nether_ice"))


def nether():
    return cond(biome(*BIOMES), seq(
        # ---- shape rules (noise, block for block with the terrain)
        # frozen lavafalls down the pillar flanks (ribs of the falls term, wider at the foot): walls only, so the floors
        # and ceilings of the same columns keep their dressing, and never in a warm ring
        all_of([SEA, noise(PILLARS, 0.2, 0.52), noise(FALLS, FALLS_FROM), y_above(30), NOT(depth("floor", 0, True)),
                NOT(depth("ceiling", 0, True))], outside_warm_ring(frozen_lava())),
        # ice pillars of the frozen sea: frosty skin, packed body, blue core, primordial heart in the strongest pillars
        cond(SEA, cond(noise(PILLARS, 0.3, 10.0), cond(y_above(30), seq(
            all_of([noise(PILLARS, 0.66, 10.0), y_above(45), NOT(y_above(96))], mod("primordial_ice")),
            cond(noise(PILLARS, 0.62, 10.0), block("blue_ice")),
            cond(noise(PILLARS, -10.0, 0.38), mod("nether_ice")),
            block("packed_ice"))))),
        # glowing seams on the rift floors: small warm spots (54)
        all_of([RIFTS, TOP, NOT(y_above(RIFT_FLOOR_BELOW)), noise(CRACK_PATCHES, SEAM_PATCHES_FROM),
                noise(CRACKS, -SEAM_BAND, SEAM_BAND)], seq(
            cond(noise(SEL, 0.7), block("magma_block")),
            mod("glowing_fissure"))),
        # ice falls down the rift walls (ribs of the icefalls term): the wall layer only, blue and packed ice (54)
        all_of([RIFTS, noise(ICEFALLS, ICEFALLS_FROM), NOT(depth("floor", 0, True)), NOT(depth("ceiling", 0, True))],
               ice_fall_layer(seq(
                   cond(noise(SEL, 0.3), block("blue_ice")),
                   block("packed_ice")))),
        # glacier masses of the rifts (and the ice bridges, 54): smooth floors, packed Nether ice with vanilla veins
        cond(RIFTS, cond(y_above(30), cond(NOT(y_above(113)), seq(
            cond(TOP, mod("nether_ice")),
            cond(noise(SEL, 0.45, 10.0), block("blue_ice")),
            cond(noise(SEL, -10.0, -0.3), block("packed_ice")),
            mod("packed_nether_ice"))))),
        # warm cracks: glowing fissure in the walls (visible only inside the crack), a glowing seam on the floor
        cond(SEA, cond(noise(CRACK_PATCHES, 0.32, 10.0), cond(noise(CRACKS, -0.05, 0.05),
             cond(y_above(14), cond(NOT(y_above(30)), seq(
                 cond(noise(SEL, 0.5, 10.0), block("magma_block")),
                 mod("glowing_fissure"))))))),
        all_of([SEA, noise(CRACK_PATCHES, 0.4, 10.0), noise(CRACKS, -0.06, 0.06), y_above(30), TOP], mod("glowing_fissure")),
        # the warm ring: bare netherrack with magma and glowing seams, no ice and no rime
        all_of([SEA, noise(CRACK_PATCHES, WARM_PATCHES_FROM), noise(CRACKS, -WARM_BAND, WARM_BAND), y_above(28)], seq(
            cond(TOP, seq(
                cond(noise(SEL, 0.45), block("magma_block")),
                cond(noise(SEL, -10.0, -0.55), mod("glowing_fissure")),
                block("netherrack"))),
            cond(depth("floor", 0, True, 6), block("netherrack")))),
        # frozen lava curtains hanging from the sea's ceiling: the curtain itself, not the ceiling mass above it
        all_of([SEA, noise(DRAPE_PATCHES, DRAPE_PAINT_PATCHES_FROM), noise(DRAPES, -DRAPE_PAINT_BAND, DRAPE_PAINT_BAND),
                y_above(80), depth("ceiling", DRAPE_DEPTH)], frozen_lava()),
        # ---- dressing rules (biome)
        # floors: rime forest frosted netherrack with ice; the sea and the rifts' low floors cold lava with ice plates
        # and patches of frosted netherrack where the crust broke
        cond(depth("floor", 0, True), seq(
            all_of([RIFTS, NOT(y_above(RIFT_WARM_BELOW))], seq(
                cond(noise(SEL, 0.55, 10.0), block("magma_block")),
                mod("cold_lava"))),
            cond(biome("rime_forest"), seq(
                cond(noise(SEL, 0.45, 10.0), mod("nether_ice")),
                cond(noise(SEL, 0.3, 10.0), mod("packed_nether_ice")),
                mod("rime_netherrack"))),
            cond(noise(SEL, 0.25, 10.0), mod("nether_ice")),
            cond(noise(SEL, 0.1, 10.0), mod("packed_nether_ice")),
            cond(noise(FROST, 0.4), mod("rime_netherrack")),
            mod("cold_lava"))),
        # the frozen sea is solidified lava below its floor
        cond(biome("frozen_lava_sea"), cond(depth("floor", 0, True, 6), mod("cold_lava"))),
        # icy ceilings
        cond(biome("rime_forest"), cond(y_above(30), cond(depth("ceiling", 0, True), seq(
            cond(noise(SEL, 0.0, 10.0), block("packed_ice")),
            mod("nether_ice"))))),
        cond(biome("frozen_lava_sea"), cond(y_above(60), cond(depth("ceiling", 1, True), mod("rime_netherrack")))),
        cond(biome("rime_forest"), mod("rime_netherrack")),
        block("netherrack")))


# ---------------------------------------------------------------- write
def dump(path, data):
    text = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n"
    with open(path, "wb") as f:
        f.write(text.encode("utf-8"))


def is_ring_3(rule):
    biomes = rule.get("if_true", {}).get("biome_is")
    return biomes in ([CA + b for b in BIOMES], [CA + "placeholder_nether_ring_3"])


def main():
    path = os.path.join(ROOT, "noise_settings", "nether.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    rules = data["surface_rule"]["sequence"]
    index = [i for i, r in enumerate(rules) if is_ring_3(r)]
    assert len(index) == 1, index
    rules[index[0]] = nether()
    dump(path, data)

    for name, (first, amplitudes) in NOISES.items():
        crlf = "\r\n"
        text = crlf.join(["{", '  "firstOctave": %d,' % first,
                          '  "amplitudes": [%s]' % ", ".join(repr(a) for a in amplitudes), "}", ""])
        with open(os.path.join(ROOT, "noise", name + ".json"), "wb") as f:
            f.write(text.encode("utf-8"))
    print("Nether ring 3 surface written")


if __name__ == "__main__":
    main()
