"""Ring 3 surface rules (change 39c): writes the ring 3 branches of noise_settings/overworld.json, the Nether ring 3
branch of noise_settings/nether.json and the surface noises noise/ring_3_surface_*.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/ring3_surface.py

The surface rules are hand-written JSON (CLAUDE.md §9); this script is their source for ring 3, because the shared
pieces (craters, cones, lava banks) would otherwise be copied by hand. Edit the rules here, run the script, commit the
script and the JSON together. It is idempotent:
- overworld: the 8 single-biome ring 3 branches are replaced in place; shared ring 3 branches (several ring 3 biomes
  only) are removed and written again, the volcanic one before the biome branches, the ground one after them;
- Nether: the branch of placeholder_nether_ring_3 is replaced;
- everything else stays byte-identical (a json round trip with indent 2 and CRLF reproduces the files exactly).
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
INF = 1.7976931348623157e+308
CA = "corruptedadventure:"


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


def noise(name, lo=-INF, hi=INF):
    return {"type": "minecraft:noise_threshold", "noise": name if ":" in name else CA + name,
            "min_threshold": lo, "max_threshold": hi}


def y_above(y, mult=0):
    return {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": mult, "add_stone_depth": False}


def depth(kind="floor", offset=0, add=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": offset, "add_surface_depth": add,
            "secondary_depth_range": secondary, "surface_type": kind}


def volc(output, lo, hi):
    """corruptedadventure:volcanic_threshold: a per-column value of the ring 3 terrain in [lo, hi)."""
    return {"type": CA + "volcanic_threshold", "output": output, "min": lo, "max": hi}


def NOT(c):
    return {"type": "minecraft:not", "invert": c}


def biome(*names):
    return {"type": "minecraft:biome", "biome_is": [CA + n for n in names]}


STEEP = {"type": "minecraft:steep"}
TOP = depth()                       # the surface block
DEPTH = depth(add=True)             # the surface depth (3-6 blocks)
THIN_ROOF = depth("ceiling", 0)     # a roof of one block: air (or lava) right below

P = "ring_3_surface_patches"        # patches ~50-150 blocks
D = "ring_3_surface_detail"         # speckles ~4-10 blocks
K = "ring_3_surface_cracks"         # thin lines where |K| is small
G = "ring_3_surface_grain"          # grain ~2-4 blocks
S = "minecraft:surface"

NOISES = {
    "ring_3_surface_patches": (-7, [1.0, 1.0, 0.5]),
    "ring_3_surface_detail": (-4, [1.0, 1.0]),
    "ring_3_surface_cracks": (-6, [1.0, 0.6]),
    "ring_3_surface_grain": (-2, [1.0, 1.0]),
}

RING_3 = ["ashen_wastes", "volcanic_peaks", "ember_woods", "basalt_flows", "sulfur_fields", "dried_seabed",
          "titan_fields", "dormant_caldera"]


# ---------------------------------------------------------------- overworld, shared pieces
def crater_interior():
    """Inside an active crater (to the crest): scorched dark rock, glowing near the lake."""
    return all_of([volc("caldera_mask", 0.12, 2.0), volc("caldera_dormant", -1.0, 0.02)], seq(
        cond(TOP, seq(
            cond(volc("caldera_mask", 0.65, 2.0), seq(
                cond(noise(K, -0.035, 0.035), mod("smoldering_stone")),
                cond(noise(G, 0.2), block("magma_block")),
                block("blackstone"))),
            all_of([noise(K, -0.02, 0.02), noise(D, -0.15)], mod("smoldering_stone")),
            cond(noise(G, 0.5), block("magma_block")),
            cond(noise(P, -INF, -0.15), block("basalt")),
            block("blackstone"))),
        cond(DEPTH, block("blackstone"))))


def dormant_walls():
    """The walls of a dormant crater: old, cooled pumice and ash (its floor stays the biome's own)."""
    return all_of([volc("caldera_dormant", 0.02, 2.0), volc("caldera_mask", -1.0, 0.5)], seq(
        cond(TOP, seq(cond(noise(D, 0.15), mod("ash_block")), mod("pumice"))),
        cond(DEPTH, mod("pumice"))))


def cone():
    """Volcano flanks, whatever the biome below: basalt flanks with ash, a mixed belt, a pumice-and-ash cap."""
    return seq(
        cond(TOP, seq(
            cond(y_above(196, 1), seq(
                cond(noise(D, 0.5), block("blackstone")),
                cond(noise(P, 0.3), mod("ash_block")),
                mod("pumice"))),
            cond(y_above(168, 1), seq(
                cond(noise(P, 0.2), mod("ash_block")),
                cond(noise(D, 0.0), mod("pumice")),
                block("basalt"))),
            cond(noise(P, -INF, -0.38), block("blackstone")),
            cond(noise(P, 0.15), seq(cond(noise(D, 0.35), block("basalt")), mod("ash_block"))),
            cond(noise(D, 0.4), block("smooth_basalt")),
            cond(noise(G, 0.55), mod("ash_block")),
            block("basalt"))),
        cond(DEPTH, block("basalt")))


def cone_body():
    # The foot is ragged: from 12 blocks above the plain in half the patches, from 24 everywhere. Surface rules have no
    # "or", so the body is written twice (from this one function).
    return seq(
        cond(volc("volcano_rise", 24.0, 1000.0), cone()),
        all_of([volc("volcano_rise", 12.0, 1000.0), noise(P, 0.0)], cone()))


def lava_banks():
    """Banks of the lava channels that hold lava (out to 1.3 half widths): scorched blackstone with magma."""
    return cond(volc("lava_bank", 0.001, 2.0), seq(
        cond(TOP, seq(
            all_of([volc("lava_bank", 0.15, 2.0), noise(G, 0.3)], block("magma_block")),
            cond(noise(G, 0.6), block("magma_block")),
            cond(noise(D, 0.2), block("basalt")),
            block("blackstone"))),
        cond(DEPTH, block("blackstone"))))


def shared_volcanic():
    """Before the biome branches: craters, cones and lava banks look the same in every ring 3 biome. The floor of a
    dormant crater (caldera_dormant >= 0.3) keeps its biome's own surface (green in dormant_caldera)."""
    return cond(biome(*RING_3), seq(
        crater_interior(),
        dormant_walls(),
        cond(NOT(volc("caldera_dormant", 0.3, 2.0)), seq(cone_body(), lava_banks()))))


def shared_ground():
    """After the biome branches: below the surface depth a volcanic package that follows the surface (~13-21 blocks
    below the top): tuff or smooth basalt, then basalt with blackstone veins; vanilla stone below (V1 = A)."""
    return cond(biome(*RING_3), cond(depth("floor", 9, True, 6), seq(
        cond(depth("floor", 4, True), seq(cond(noise(P, 0.25), block("tuff")), block("smooth_basalt"))),
        cond(noise(K, -0.04, 0.04), block("blackstone")),
        block("basalt"))))


# ---------------------------------------------------------------- overworld, per biome
def ashen_wastes():
    return [
        cond(TOP, seq(
            cond(THIN_ROOF, mod("ash_crust")),
            cond(noise(P, 0.42), seq(cond(noise(D, 0.3), mod("ash_block")), mod("pumice"))),
            cond(noise(P, -INF, -0.45), seq(cond(noise(D, 0.25), block("basalt")), block("smooth_basalt"))),
            cond(noise(D, 0.5), block("gravel")),
            cond(noise(D, -INF, -0.55), block("smooth_basalt")),
            mod("ash_block"))),
        cond(DEPTH, mod("ash_block"))]


def volcanic_peaks():
    return [
        cond(TOP, seq(
            cond(THIN_ROOF, mod("ash_crust")),
            all_of([noise(K, -0.02, 0.02), noise(D, 0.0)], block("magma_block")),
            cond(noise(P, 0.25), seq(cond(noise(D, 0.35), mod("pumice")), mod("ash_block"))),
            cond(noise(P, -INF, -0.38), block("blackstone")),
            cond(noise(D, 0.45), block("smooth_basalt")),
            block("basalt"))),
        cond(DEPTH, block("blackstone"))]


def ember_woods():
    return [
        cond(TOP, seq(
            cond(THIN_ROOF, mod("ash_crust")),
            all_of([noise(K, -0.022, 0.022), noise(P, -0.1), noise(D, 0.15)], mod("smoldering_ash")),
            cond(noise(D, 0.55), mod("smoldering_ash")),
            cond(noise(P, -INF, -0.5), block("smooth_basalt")),
            cond(noise(S, 0.05), block("coarse_dirt")),
            mod("ash_block"))),
        cond(depth("floor", 1), seq(mod("ash_block"))),
        cond(DEPTH, block("coarse_dirt"))]


def basalt_flows():
    return [
        cond(TOP, seq(
            cond(noise(K, -0.009, 0.009), block("magma_block")),
            cond(noise(K, -0.03, 0.03), mod("smoldering_stone")),
            cond(noise(P, 0.35), seq(cond(noise(D, 0.4), mod("ash_block")), block("smooth_basalt"))),
            cond(noise(P, -INF, -0.42), block("blackstone")),
            cond(noise(G, 0.55), block("blackstone")),
            cond(noise(D, 0.5), mod("ash_block")),
            block("basalt"))),
        cond(DEPTH, block("basalt"))]


def sulfur_fields():
    return [
        cond(TOP, seq(
            cond(noise(P, -INF, -0.42), block("smooth_basalt")),
            all_of([noise(P, -0.42, -0.3), noise(G, 0.2)], block("yellow_terracotta")),
            cond(noise(P, 0.45), seq(cond(noise(D, 0.3), block("smooth_basalt")), mod("ash_block"))),
            all_of([noise(P, 0.36, 0.45), noise(G, 0.3)], block("calcite")),
            cond(noise(G, 0.6), block("yellow_terracotta")),
            cond(noise(D, 0.55), block("calcite")),
            mod("sulfur_crust"))),
        cond(depth("floor", 1), seq(mod("sulfur_crust"))),
        cond(DEPTH, block("smooth_basalt"))]


def dried_seabed():
    return [
        cond(STEEP, cond(DEPTH, block("sandstone"))),
        cond(TOP, seq(
            cond(noise(P, 0.5), seq(cond(noise(D, 0.3), block("sand")), mod("ash_block"))),
            cond(noise(D, 0.55), mod("pumice")),
            cond(noise(S, 0.25), block("gravel")),
            cond(noise(S, -INF, -0.4), block("clay")),
            block("sand"))),
        cond(DEPTH, block("sandstone"))]


def titan_fields():
    return [
        cond(STEEP, cond(DEPTH, block("basalt"))),
        cond(TOP, seq(
            cond(THIN_ROOF, mod("ash_crust")),
            cond(noise(P, 0.12), seq(cond(noise(D, 0.5), mod("ash_block")), block("soul_soil"))),
            cond(noise(P, -INF, -0.4), seq(cond(noise(D, 0.3), block("smooth_basalt")), block("basalt"))),
            cond(noise(D, 0.45), block("soul_soil")),
            mod("ash_block"))),
        cond(DEPTH, block("basalt"))]


def dormant_caldera():
    return [
        cond(STEEP, cond(DEPTH, block("basalt"))),
        cond(TOP, seq(
            cond(volc("caldera_mask", -1.0, 0.5), seq(
                cond(noise(D, 0.2), mod("ash_block")),
                cond(noise(P, -INF, -0.3), block("basalt")),
                mod("pumice"))),
            cond(noise(S, -INF, -0.3), block("coarse_dirt")),
            cond(noise(P, 0.45), mod("ash_block")),
            block("moss_block"))),
        cond(DEPTH, block("coarse_dirt"))]


BIOMES = {f.__name__: f for f in [ashen_wastes, volcanic_peaks, ember_woods, basalt_flows, sulfur_fields, dried_seabed,
                                  titan_fields, dormant_caldera]}

# ---------------------------------------------------------------- Nether ring 3
R = "nether_ring_3_region"           # thresholds = NetherRing3Features.RIME_THRESHOLD / RIFTS_THRESHOLD
SEA = noise(R, -0.18, 0.18)
RIME = noise(R, 0.18, 10.0)
RIFTS = noise(R, -10.0, -0.18)
SEL = "minecraft:nether_state_selector"
PILLARS = "nether_ring_3_pillars"


def nether():
    return cond({"type": "minecraft:biome", "biome_is": [CA + "placeholder_nether_ring_3"]}, seq(
        # ice pillars of the frozen sea: frosty skin, packed body, blue core, primordial heart in the strongest pillars
        cond(SEA, cond(noise(PILLARS, 0.3, 10.0), cond(y_above(30), seq(
            all_of([noise(PILLARS, 0.66, 10.0), y_above(45), NOT(y_above(96))], mod("primordial_ice")),
            cond(noise(PILLARS, 0.62, 10.0), block("blue_ice")),
            cond(noise(PILLARS, -10.0, 0.38), mod("nether_ice")),
            block("packed_ice"))))),
        # glacier masses of the rifts: smooth floors, packed Nether ice with vanilla veins
        cond(RIFTS, cond(y_above(30), cond(NOT(y_above(113)), seq(
            cond(TOP, mod("nether_ice")),
            cond(noise(SEL, 0.45, 10.0), block("blue_ice")),
            cond(noise(SEL, -10.0, -0.3), block("packed_ice")),
            mod("packed_nether_ice"))))),
        # warm cracks: glowing fissure in the walls (visible only inside the crack), a glowing seam on the floor
        cond(SEA, cond(noise("nether_ring_3_crack_patches", 0.32, 10.0), cond(noise("nether_ring_3_cracks", -0.05, 0.05),
             cond(y_above(14), cond(NOT(y_above(30)), seq(
                 cond(noise(SEL, 0.5, 10.0), block("magma_block")),
                 mod("glowing_fissure"))))))),
        all_of([SEA, noise("nether_ring_3_crack_patches", 0.4, 10.0), noise("nether_ring_3_cracks", -0.06, 0.06),
                y_above(30), TOP], mod("glowing_fissure")),
        # floors
        cond(depth("floor", 0, True), seq(
            cond(RIME, seq(
                cond(noise(SEL, 0.45, 10.0), mod("nether_ice")),
                cond(noise(SEL, 0.3, 10.0), mod("packed_nether_ice")),
                mod("rime_netherrack"))),
            cond(noise(SEL, 0.25, 10.0), mod("nether_ice")),
            cond(noise(SEL, 0.1, 10.0), mod("packed_nether_ice")),
            mod("cold_lava"))),
        # the frozen sea is solidified lava below its floor
        cond(SEA, cond(depth("floor", 0, True, 6), mod("cold_lava"))),
        # icy ceilings
        cond(RIME, cond(y_above(30), cond(depth("ceiling", 0, True), seq(
            cond(noise(SEL, 0.0, 10.0), block("packed_ice")),
            mod("nether_ice"))))),
        cond(SEA, cond(y_above(60), cond(depth("ceiling", 1, True), mod("rime_netherrack")))),
        cond(RIME, mod("rime_netherrack")),
        block("netherrack")))


# ---------------------------------------------------------------- write
def dump(path, data):
    text = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n"
    with open(path, "wb") as f:
        f.write(text.encode("utf-8"))


def is_shared_ring_3(rule):
    biomes = rule.get("if_true", {}).get("biome_is", [])
    return len(biomes) > 1 and all(b in [CA + n for n in RING_3] for b in biomes)


def main():
    path = os.path.join(ROOT, "noise_settings", "overworld.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    rules = data["surface_rule"]["sequence"][1]["then_run"]["sequence"]
    rules[:] = [r for r in rules if not is_shared_ring_3(r)]
    positions = []
    for i, r in enumerate(rules):
        biomes = r.get("if_true", {}).get("biome_is")
        if biomes and len(biomes) == 1 and biomes[0].startswith(CA) and biomes[0][len(CA):] in BIOMES:
            name = biomes[0][len(CA):]
            rules[i] = cond(biome(name), seq(*BIOMES[name]()))
            positions.append(i)
    assert positions == list(range(positions[0], positions[0] + len(BIOMES))), positions
    rules.insert(positions[-1] + 1, shared_ground())
    rules.insert(positions[0], shared_volcanic())
    dump(path, data)

    path = os.path.join(ROOT, "noise_settings", "nether.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    rules = data["surface_rule"]["sequence"]
    index = [i for i, r in enumerate(rules) if r.get("if_true", {}).get("biome_is") == [CA + "placeholder_nether_ring_3"]]
    assert len(index) == 1, index
    rules[index[0]] = nether()
    dump(path, data)

    for name, (first, amplitudes) in NOISES.items():
        crlf = "\r\n"
        text = crlf.join(["{", '  "firstOctave": %d,' % first,
                          '  "amplitudes": [%s]' % ", ".join(repr(a) for a in amplitudes), "}", ""])
        with open(os.path.join(ROOT, "noise", name + ".json"), "wb") as f:
            f.write(text.encode("utf-8"))
    print("ring 3 surface written")


if __name__ == "__main__":
    main()
