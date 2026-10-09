"""Ring 3 surface rules (change 39c): writes the ring 3 branches of noise_settings/overworld.json and the surface noises
noise/ring_3_surface_*.json. The Nether ring 3 branch of noise_settings/nether.json has its own script since 53b
(nether_ring3_surface.py, stream W-nether).

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/ring3_surface.py

The surface rules are hand-written JSON (CLAUDE.md §9); this script is their source for ring 3, because the shared
pieces (craters, cones, lava banks) would otherwise be copied by hand. Edit the rules here, run the script, commit the
script and the JSON together. It is idempotent:
- overworld (39f): one ring 3 branch (all 8 biomes) where the ring 3 branches stood, and a fast path for the deep blocks
  of the ring 3 core at the start of the rules above the preliminary surface; older layouts (one branch per biome plus
  shared branches, 39c–39e) are recognised and replaced; (42) the walls of the lava tubes and magma chambers as the
  second top-level rule (right after the bedrock floor), replaced if present;
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
    """Banks of the lava rivers (the bed and ~3 blocks beside the lava, whatever the river's width; 39d): scorched
    blackstone with a little magma right at the edge."""
    return cond(volc("lava_bank", 0.001, 2.0), seq(
        cond(TOP, seq(
            all_of([volc("lava_bank", 0.5, 2.0), noise(G, 0.45)], block("magma_block")),
            cond(noise(G, 0.75), block("magma_block")),
            cond(noise(D, 0.2), block("basalt")),
            block("blackstone"))),
        cond(DEPTH, block("blackstone"))))


def cliffs():
    """Cliffs and steep walls of the landforms (39d: plateau risers with their column prisms, rift flanks, gully walls,
    sea stacks): bare rock instead of ash, in every ring 3 biome."""
    return cond(volc("cliff", 0.45, 2.0), seq(
        cond(TOP, seq(
            cond(noise(P, -INF, -0.25), block("blackstone")),
            cond(noise(D, 0.35), block("tuff")),
            block("basalt"))),
        cond(DEPTH, block("basalt"))))


def thin_kind(lo, hi):
    """39f: the kind of thin solid at a block (per block): 0 biome surface, 2 obsidian, 3 basalt, 4 island, 5 rubble."""
    return volc("thin_kind", lo, hi)


THIN_ANY = thin_kind(-0.5, 6.0)
THIN_BIOME = thin_kind(-0.5, 0.5)
# The depth to which the ring 3 rules reach (the volcanic ground package below the surface); below it vanilla stone.
GATE = depth("floor", 9, True, 6)


def thin_solids():
    """39f: the thin solids, painted whole (whatever their height; 39d painted only their top, so tall needles, pillars
    and arches showed bands of stone below it): obsidian needles with winding glowing veins, basalt (prisms, pillars,
    bridges, sea stacks), floating islands (ash on top, dark rock with glowing seams below), rubble."""
    return seq(
        cond(thin_kind(1.5, 2.5), seq(
            cond(volc("vein", 0.0, 0.06), block("crying_obsidian")),
            cond(volc("vein", 0.5, 0.525), block("magma_block")),
            block("obsidian"))),
        cond(thin_kind(2.5, 3.5), seq(
            cond(noise(K, -0.03, 0.03), block("blackstone")),
            cond(noise(D, 0.35), block("smooth_basalt")),
            block("basalt"))),
        cond(thin_kind(3.5, 4.5), seq(
            cond(TOP, seq(
                cond(noise(P, 0.3), mod("pumice")),
                cond(noise(D, 0.45), block("basalt")),
                mod("ash_block"))),
            cond(DEPTH, mod("ash_block")),
            cond(volc("vein", 0.0, 0.035), block("magma_block")),
            cond(noise(P, -INF, -0.3), block("blackstone")),
            block("basalt"))),
        cond(thin_kind(4.5, 5.5), rubble()))


def rubble():
    """Fallen rock (39f): the floor of a collapse."""
    return seq(
        cond(noise(G, 0.35), block("blackstone")),
        cond(noise(G, -INF, -0.35), block("tuff")),
        cond(noise(D, 0.25), mod("pumice")),
        block("basalt"))


def collapse_floor():
    """39f: the floor of a collapse (inside its rim, not a wall) is rubble on top."""
    return cond(volc("collapse", 0.99, 2.0), cond(TOP, rubble()))


def shared_volcanic():
    """Craters, cones, lava banks, cliffs and collapses look the same in every ring 3 biome (before the biome branches,
    inside the ring 3 branch). The floor of a dormant crater (caldera_dormant >= 0.3) keeps its biome's own surface."""
    return seq(
        crater_interior(),
        dormant_walls(),
        cond(NOT(volc("caldera_dormant", 0.3, 2.0)), seq(cone_body(), lava_banks(), cliffs(), collapse_floor())))


def shared_ground():
    """After the biome branches: below the surface depth a volcanic package that follows the surface (~13-21 blocks
    below the top): tuff or smooth basalt, then basalt with blackstone veins; vanilla stone below (V1 = A). The branch
    only runs within GATE, which is this package's own depth."""
    return seq(
        cond(depth("floor", 4, True), seq(cond(noise(P, 0.25), block("tuff")), block("smooth_basalt"))),
        cond(noise(K, -0.04, 0.04), block("blackstone")),
        block("basalt"))


# ---------------------------------------------------------------- overworld, per biome
# Each biome: (top, rest). `top` is the sequence for its surface block; thin solids of the biome kind (bones, arches,
# hoodoos, the micro relief) are painted whole with it, without the THIN_ROOF crust (a falling block).
def ashen_wastes():
    top = [
        cond(noise(P, 0.42), seq(cond(noise(D, 0.3), mod("ash_block")), mod("pumice"))),
        cond(noise(P, -INF, -0.45), seq(cond(noise(D, 0.25), block("basalt")), block("smooth_basalt"))),
        cond(noise(D, 0.5), block("gravel")),
        cond(noise(D, -INF, -0.55), block("smooth_basalt")),
        mod("ash_block")]
    return top, [cond(DEPTH, mod("ash_block"))]


def volcanic_peaks():
    top = [
        all_of([noise(K, -0.02, 0.02), noise(D, 0.0)], block("magma_block")),
        cond(noise(P, 0.25), seq(cond(noise(D, 0.35), mod("pumice")), mod("ash_block"))),
        cond(noise(P, -INF, -0.38), block("blackstone")),
        cond(noise(D, 0.45), block("smooth_basalt")),
        block("basalt")]
    return top, [cond(DEPTH, block("blackstone"))]


def ember_woods():
    # 39f: no more flat patches of coarse dirt; pumice and tuff by the step of the relief (`tread`: each block of height
    # its own share), so their edges follow the steps; coarse dirt only as small specks.
    top = [
        all_of([noise(K, -0.022, 0.022), noise(P, -0.1), noise(D, 0.15)], mod("smoldering_ash")),
        cond(noise(D, 0.55), mod("smoldering_ash")),
        cond(noise(P, -INF, -0.5), block("smooth_basalt")),
        cond(volc("tread", 0.0, 0.24), seq(cond(noise(P, 0.1), mod("pumice")), block("tuff"))),
        all_of([noise(S, 0.45), noise(G, 0.4)], block("coarse_dirt")),
        mod("ash_block")]
    return top, [
        cond(depth("floor", 1), mod("ash_block")),
        cond(DEPTH, seq(cond(noise(P, 0.15), block("tuff")), mod("ash_block")))]


def basalt_flows():
    top = [
        cond(noise(K, -0.009, 0.009), block("magma_block")),
        cond(noise(K, -0.03, 0.03), mod("smoldering_stone")),
        cond(noise(P, 0.35), seq(cond(noise(D, 0.4), mod("ash_block")), block("smooth_basalt"))),
        cond(noise(P, -INF, -0.42), block("blackstone")),
        cond(noise(G, 0.55), block("blackstone")),
        cond(noise(D, 0.5), mod("ash_block")),
        block("basalt")]
    return top, [cond(DEPTH, block("basalt"))]


def sulfur_fields():
    top = [
        cond(noise(P, -INF, -0.42), block("smooth_basalt")),
        all_of([noise(P, -0.42, -0.3), noise(G, 0.2)], block("yellow_terracotta")),
        cond(noise(P, 0.45), seq(cond(noise(D, 0.3), block("smooth_basalt")), mod("ash_block"))),
        all_of([noise(P, 0.36, 0.45), noise(G, 0.3)], block("calcite")),
        cond(noise(G, 0.6), block("yellow_terracotta")),
        cond(noise(D, 0.55), block("calcite")),
        mod("sulfur_crust")]
    return top, [
        cond(depth("floor", 1), mod("sulfur_crust")),
        cond(DEPTH, block("smooth_basalt"))]


def dried_seabed():
    # 39f: the gravel and clay patches are smaller (specks inside the large patches).
    top = [
        cond(noise(P, 0.5), seq(cond(noise(D, 0.3), block("sand")), mod("ash_block"))),
        cond(noise(D, 0.55), mod("pumice")),
        all_of([noise(S, 0.35), noise(D, 0.15)], block("gravel")),
        all_of([noise(S, -INF, -0.5), noise(D, -INF, -0.15)], block("clay")),
        block("sand")]
    return top, [
        cond(STEEP, cond(DEPTH, block("sandstone"))),
        cond(DEPTH, block("sandstone"))]


def titan_fields():
    top = [
        cond(noise(P, 0.12), seq(cond(noise(D, 0.5), mod("ash_block")), block("soul_soil"))),
        cond(noise(P, -INF, -0.4), seq(cond(noise(D, 0.3), block("smooth_basalt")), block("basalt"))),
        cond(noise(D, 0.45), block("soul_soil")),
        mod("ash_block")]
    return top, [
        cond(STEEP, cond(DEPTH, block("basalt"))),
        cond(DEPTH, block("basalt"))]


def dormant_caldera():
    top = [
        cond(volc("caldera_mask", -1.0, 0.5), seq(
            cond(noise(D, 0.2), mod("ash_block")),
            cond(noise(P, -INF, -0.3), block("basalt")),
            mod("pumice"))),
        cond(noise(S, -INF, -0.3), block("coarse_dirt")),
        cond(noise(P, 0.45), mod("ash_block")),
        block("moss_block")]
    return top, [
        cond(STEEP, cond(DEPTH, block("basalt"))),
        cond(DEPTH, block("coarse_dirt"))]


# Biomes whose top gets the one-block crust of ash over a thin roof.
THIN_ROOF_CRUST = {"ashen_wastes", "volcanic_peaks", "ember_woods", "titan_fields"}


def biome_branch(name):
    top, rest = BIOMES[name]()
    head = [cond(THIN_ROOF, mod("ash_crust"))] if name in THIN_ROOF_CRUST else []
    # The STEEP rules (before TOP) keep their place in front.
    steep = [r for r in rest if r.get("if_true") == STEEP]
    rest = [r for r in rest if r.get("if_true") != STEEP]
    return cond(biome(name), seq(*steep, cond(TOP, seq(*head, *top)), *rest))


def uses_cracks(rule):
    """Whether a rule reads the crack noise: a 2D line pattern, which on a tall thin solid would run as a straight
    vertical stripe (39f), so the thin solids of the biome kind leave those rules out."""
    return CA + K in json.dumps(rule)


def ring_3_branch():
    """39f: all of ring 3 in one branch. First the thin solids (any depth); then, within GATE only, the shared volcanic
    rules, the biome branches and the ground package. Cheap tests come first: the per-block thin test returns at once
    for columns without thin shapes, the depth gate skips the deep blocks."""
    thin_biome = [cond(biome(n), seq(*[r for r in BIOMES[n]()[0] if not uses_cracks(r)])) for n in RING_3]
    return cond(biome(*RING_3), seq(
        cond(THIN_ANY, seq(thin_solids(), cond(THIN_BIOME, seq(*thin_biome)))),
        cond(GATE, seq(shared_volcanic(), *[biome_branch(n) for n in RING_3], shared_ground()))))


def reach(ring):
    return {"type": CA + "ring_reach", "dimension": "minecraft:overworld", "ring": ring}


def ring_3_deep():
    """39f: the deep blocks of the ring 3 core (below GATE, no thin solid, neither ring 2 nor ring 4 within reach) stay
    stone at once. Without this every such block above the preliminary surface asked for its biome in each branch
    (the ring 2 and vanilla branches test their biomes per block); stone is what they became anyway."""
    return all_of([reach("ring_3"), NOT(reach("ring_2")), NOT(reach("ring_4")), NOT(GATE), NOT(THIN_ANY)], block("stone"))


def cave_walls():
    """42: the walls of the lava tubes, skylights and magma chambers, at any depth (most lie far below the preliminary
    surface, where no other ring 3 rule runs): a smooth basalt skin, a basalt shell, winding blackstone veins, a crust
    of blackstone and magma where the skin borders lava (a gutter, a chamber's lake). The walls stop three blocks under
    the ground, so at a mouth or skylight the biome's own rules paint the rim (and the 39c ash crust a roof of one
    block). Cheap tests first: the ring, then one per column, then the block."""
    return all_of([reach("ring_3"), volc("cave_walls", 0.5, 2.0)], seq(
        cond(volc("cave_wall", 2.5, 3.5), seq(cond(noise(G, 0.3), block("magma_block")), block("blackstone"))),
        cond(volc("cave_wall", 0.5, 1.5), seq(cond(volc("vein", 0.0, 0.05), block("blackstone")), block("smooth_basalt"))),
        cond(volc("cave_wall", 1.5, 2.5), seq(cond(volc("vein", 0.0, 0.08), block("blackstone")), block("basalt")))))


BIOMES = {f.__name__: f for f in [ashen_wastes, volcanic_peaks, ember_woods, basalt_flows, sulfur_fields, dried_seabed,
                                  titan_fields, dormant_caldera]}

# ---------------------------------------------------------------- write
def dump(path, data):
    text = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n"
    with open(path, "wb") as f:
        f.write(text.encode("utf-8"))


def is_ring_3(rule):
    """A ring 3 branch of any layout: one ring 3 biome, several (39c shared branches, the 39f branch)."""
    biomes = rule.get("if_true", {}).get("biome_is", [])
    return len(biomes) > 0 and all(b in [CA + n for n in RING_3] for b in biomes)


def is_cave_walls(rule):
    """The top-level rule of the cave walls (42)."""
    conds = rule.get("if_true", {})
    return conds == reach("ring_3") and rule.get("then_run", {}).get("if_true") == volc("cave_walls", 0.5, 2.0)


def is_ring_3_deep(rule):
    c = rule.get("if_true", {})
    return c == reach("ring_3")


def main():
    path = os.path.join(ROOT, "noise_settings", "overworld.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    top = data["surface_rule"]["sequence"]
    top[:] = [r for r in top if not is_cave_walls(r)]
    rules = top[1]["then_run"]["sequence"]
    rules[:] = [r for r in rules if not is_ring_3_deep(r)]
    positions = [i for i, r in enumerate(rules) if is_ring_3(r)]
    assert positions == list(range(positions[0], positions[0] + len(positions))), positions
    first = positions[0]
    rules[first:first + len(positions)] = [ring_3_branch()]
    rules.insert(0, ring_3_deep())
    assert top[0].get("if_true", {}).get("random_name") == "minecraft:bedrock_floor", top[0]
    top.insert(1, cave_walls())
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
