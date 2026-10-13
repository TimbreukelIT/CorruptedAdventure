"""Nether ring 4 terrain (65): writes density_function/ring_4/nether/*.json (except region.json, which is 59's) and the
shape noises noise/nether_ring_4_*.json (except nether_ring_4_region.json).

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/nether_ring4_terrain.py

Stream W-nether's own script; idempotent (every file it owns is rewritten, JSON with indent 2). The shape follows
docs/plans/ring-4/golf-1-nether.md:
- the region axis ring_4/nether/region (59) splits the ring: bone dunes in the middle, crystal spires high, geode hollows
  low, at NetherRing4Features.SPIRES_THRESHOLD / HOLLOWS_THRESHOLD (+-0.18); the weights ramp over +-0.04 around them;
- bone dunes: a plain of dunes at Y ~43, closed down to Y ~38 (the lava is only seen in the spires, V2 = C);
- crystal spires: open down to the lava sea (fluid_levels.ring_4 = 10), crystal columns rising from it to the ceiling
  with a narrow ledge at walking height, and a mesh of bone bridges (Y 36-42) between them, flush with the dunes;
- geode hollows: a rock mass up to Y ~88 with tunnels; the geodes themselves are the density function type
  corruptedadventure:giant_geodes (terrain = min(interpolated shape, geodes));
- the dam: solid up to Y 33 within 48 blocks of both ring boundaries (steps 3|4 22/10 and 4|5 10/32), every other
  shape fades out there through core = 1 - edge. No carvers (the biomes have none).
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"
DIR = "ring_4/nether/"

# ---------------------------------------------------------------- parameters
SPIRES_THRESHOLD = 0.18     # NetherRing4Features.SPIRES_THRESHOLD
HOLLOWS_THRESHOLD = -0.18   # NetherRing4Features.HOLLOWS_THRESHOLD
RAMP = 0.04                 # weights go 0 -> 1 over threshold -+ RAMP
EDGE_WIDTH = 48.0           # ring_edge width of the dam
DAM_TOP = 33                # solid up to here near both boundaries (covers 22 and 32)

DUNES_FLOOR = 43.0          # dunes and hollows stay closed down to Y ~38 (V2 = C)
HOLLOWS_FLOOR = 40.0
SEA_FLOOR = 4.0             # crystal spires: open down to the lava sea (lava up to fluid_levels.ring_4 = 10)
DUNE_AMPLITUDE = 3.0        # dune crests in the bone dunes, none in the spires (the bridges meet a smooth plateau)
BRIDGE_TOP, BRIDGE_THICKNESS = 42, 6                       # bone bridges at walking height, top flush with the dunes
BRIDGE_BAND = 0.07          # a bridge where min(|a|, |b|) of two noises < band: two meshes of lines, no dead ends
CEILING, CEILING_AMPLITUDE, CEILING_MIN, CEILING_MAX = 112.0, 6.0, 104.0, 120.0
PILLAR_THRESHOLD, PILLAR_FLARE = 0.40, 0.15
LEDGE_WIDEN, LEDGE_BOTTOM, LEDGE_TOP = 0.12, 32, 42        # a narrow ledge round every pillar, top at bridge height
MASS_TOP, MASS_AMPLITUDE = 88.0, 6.0
TUNNEL_Y, TUNNEL_Y_AMPLITUDE = 50.0, 6.0
GEODES = {                  # GiantGeodeShape.Settings: hollows of diameter 20-40 with a flat floor and 1-3 doorways
    "cell_size": 128, "chance": 0.85, "radius_min": 10, "radius_max": 20, "floor_min": 42, "floor_max": 48,
    "region_max": HOLLOWS_THRESHOLD - RAMP, "shell": 3, "door_width": 6, "door_height": 6, "door_length": 24, "doors_max": 3,
}

NOISES = {
    "nether_ring_4_dunes": (-5, [1.0, 0.5]),
    "nether_ring_4_bridges_a": (-6, [1.0, 0.5]),
    "nether_ring_4_bridges_b": (-6, [1.0, 0.5]),
    "nether_ring_4_ceiling": (-7, [1.0, 1.0]),
    "nether_ring_4_pillars": (-4, [1.0, 1.0]),
    "nether_ring_4_mass": (-6, [1.0, 1.0]),
    "nether_ring_4_tunnels": (-7, [1.0, 1.0]),
    "nether_ring_4_tunnel_height": (-6, [1.0, 1.0]),
    "nether_ring_4_rough": (-4, [1.0, 0.7, 0.5]),
    "nether_ring_4_geode_cells": (-4, [1.0]),   # only a seeded hash for giant_geodes
}


# ---------------------------------------------------------------- builders
def ref(name):
    return CA + DIR + name


def noise(name, y_scale=0.0):
    return {"type": "minecraft:noise", "noise": CA + name, "xz_scale": 1.0, "y_scale": y_scale}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def neg(a):
    return mul(-1.0, a)


def mx(a, b):
    return {"type": "minecraft:max", "argument1": a, "argument2": b}


def mn(a, b):
    return {"type": "minecraft:min", "argument1": a, "argument2": b}


def clamp(a, lo, hi):
    return {"type": "minecraft:clamp", "input": a, "min": lo, "max": hi}


def absf(a):
    return {"type": "minecraft:abs", "argument": a}


def flat(a):
    return {"type": "minecraft:flat_cache", "argument": a}


def grad(from_y, to_y, from_value, to_value):
    return {"type": "minecraft:y_clamped_gradient", "from_y": from_y, "to_y": to_y,
            "from_value": from_value, "to_value": to_value}


Y = grad(0, 128, 0.0, 128.0)


def below(height):
    """Density of 'solid below height': (height - y) / 8, clamped to +-2.5."""
    return clamp(mul(add(height, neg(Y)), 0.125), -2.5, 2.5)


def above(height):
    return clamp(mul(add(Y, neg(height)), 0.125), -2.5, 2.5)


def weighted(weight, term):
    """A term of the shape's max that vanishes (-2.5, not 0) where weight is 0."""
    return add(mul(weight, add(term, 2.5)), -2.5)


# ---------------------------------------------------------------- the files
def files():
    region = ref("region")
    k = 0.5 / RAMP
    out = {
        "spires_weight": flat(clamp(add(mul(region, k), 0.5 - k * SPIRES_THRESHOLD), 0.0, 1.0)),
        "hollows_weight": flat(clamp(add(mul(region, -k), 0.5 + k * HOLLOWS_THRESHOLD), 0.0, 1.0)),
        "dunes_weight": flat(add(1.0, neg(add(ref("spires_weight"), ref("hollows_weight"))))),
        "edge": flat({"type": CA + "ring_edge", "dimension": "minecraft:the_nether", "ring": "ring_4", "width": EDGE_WIDTH}),
        "core": flat(add(1.0, neg(ref("edge")))),
        # dune crests: 1 - 2|n| in [-1, 1], sharp ridges where the noise crosses 0
        "dunes": flat(add(1.0, mul(-2.0, absf(noise("nether_ring_4_dunes"))))),
        # the sea: how far the floor sinks to the lava (0 outside the spires and near the boundaries, for the dam)
        "sea": flat(mul(ref("spires_weight"), ref("core"))),
        # the bridges reach their full strength as soon as the floor starts to sink (sea 0.025, floor ~42): they leave
        # the dunes flush with its rim instead of starting halfway down the slope
        "bridge_weight": flat(clamp(mul(ref("sea"), 40.0), 0.0, 1.0)),
        "floor_height": flat(add(
            add(DUNES_FLOOR, add(mul(SEA_FLOOR - DUNES_FLOOR, ref("sea")),
                                 mul(HOLLOWS_FLOOR - DUNES_FLOOR, ref("hollows_weight")))),
            mul(ref("dunes"), mul(DUNE_AMPLITUDE, ref("dunes_weight"))))),
        "ceiling_height": flat(clamp(add(CEILING, mul(CEILING_AMPLITUDE, noise("nether_ring_4_ceiling"))),
                                     CEILING_MIN, CEILING_MAX)),
        # distance-like value to the nearest bridge line: the zero lines of two independent noises
        "bridge_noise": flat(mn(absf(noise("nether_ring_4_bridges_a")), absf(noise("nether_ring_4_bridges_b")))),
        # a slab Y 36-42 (steep, so the roughness never holes it) along the lines, ~3-6 wide
        "bridges": clamp(mn(mul(add(BRIDGE_BAND, neg(ref("bridge_noise"))), 30.0),
                            mul(mn(add(float(BRIDGE_TOP), neg(Y)), add(Y, -float(BRIDGE_TOP - BRIDGE_THICKNESS))), 0.5)),
                         -2.5, 2.5),
        "pillar_noise": flat(noise("nether_ring_4_pillars")),
        # columns rising from the lava sea: a narrow ledge (wider band Y 32-42) at bridge height, a flared head at the ceiling
        "pillars": clamp(mul(add(ref("pillar_noise"),
                                 add(-PILLAR_THRESHOLD,
                                     add(mul(LEDGE_WIDEN, mn(grad(LEDGE_BOTTOM, LEDGE_BOTTOM + 2, 0.0, 1.0),
                                                             grad(LEDGE_TOP, LEDGE_TOP + 1, 1.0, 0.0))),
                                         mul(PILLAR_FLARE, grad(94, 112, 0.0, 1.0))))),
                             4.0), -2.5, 2.5),
        "mass_top": flat(mn(add(add(MASS_TOP, mul(MASS_AMPLITUDE, noise("nether_ring_4_mass"))), mul(-60.0, ref("edge"))),
                            add(ref("ceiling_height"), -10.0))),
        "tunnel_noise": flat(absf(noise("nether_ring_4_tunnels"))),
        "tunnel_height": flat(add(TUNNEL_Y, mul(TUNNEL_Y_AMPLITUDE, noise("nether_ring_4_tunnel_height")))),
        # tubes ~5 high and ~6-9 wide where |tunnel noise| < 0.09 at tunnel_height
        "mass": mn(below(ref("mass_top")),
                   clamp(mul(add(add(mul(ref("tunnel_noise"), 11.11111111111111),
                                     mul(absf(add(Y, neg(ref("tunnel_height")))), 0.2857142857142857)), -1.0), 1.5),
                         -2.5, 2.5)),
        "dam": add(mul(5.0, mul(ref("edge"), grad(DAM_TOP, DAM_TOP + 4, 1.0, 0.0))), -2.5),
        # rough rock in the hollows; the dunes and the spires (thin bridges and ledges) stay smooth
        "rough": add(mul(add(0.25, mul(0.45, ref("hollows_weight"))), noise("nether_ring_4_rough", 0.8)), -0.05),
    }
    out["shape"] = add(
        mx(mx(below(ref("floor_height")), above(ref("ceiling_height"))),
           mx(mx(mx(weighted(ref("sea"), ref("pillars")), weighted(ref("bridge_weight"), ref("bridges"))),
                 weighted(ref("hollows_weight"), ref("mass"))),
              ref("dam"))),
        ref("rough"))
    out["geodes"] = {"type": CA + "giant_geodes", "shape": ref("shape"), "edge": ref("edge"), "region": region,
                     "noise": CA + "nether_ring_4_geode_cells", "settings": GEODES}
    # One interpolator (both branches name the same file); the geodes cut it outside the interpolation, only where the
    # hollows have weight. A density function file must be an object or a number, never a bare reference.
    out["interpolated_shape"] = {"type": "minecraft:interpolated", "argument": ref("shape")}
    out["terrain"] = {"type": "minecraft:range_choice", "input": ref("hollows_weight"), "min_inclusive": 1e-6,
                      "max_exclusive": 2.0, "when_in_range": mn(ref("interpolated_shape"), ref("geodes")),
                      "when_out_of_range": ref("interpolated_shape")}
    return out


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for name, data in files().items():
        write(os.path.join(ROOT, "density_function", "ring_4", "nether", name + ".json"), data)
    for name, (octave, amplitudes) in NOISES.items():
        write(os.path.join(ROOT, "noise", name + ".json"), {"firstOctave": octave, "amplitudes": amplitudes})
    placeholder = os.path.join(ROOT, "density_function", "ring_4", "nether.json")
    if os.path.exists(placeholder):
        os.remove(placeholder)


if __name__ == "__main__":
    main()
