"""Toxic sludge and swamp gas against air or water per area (ring 4, golf 2). Offline, on the saved region files.

    python poisonscan.py <world> <areas> [--dim ow] [--only name ...] [--list 10] [--json out.json] [--strict]

Toxic sludge (`corruptedadventure:toxic_pool`, the 72 fluid) lies only in closed basins: in the tunnel net (pool halls,
73) and at the surface (`toxic_pool` feature, 76), every side and the bottom poison or dry solid ground; the top is the
open surface. An overtreding (`over`) is a sludge block with
  open    air, cave_air, swamp gas or a non-solid block a fluid washes away (grass, ferns, flowers, vines, carpets,
          snow layers, the ring 4 glow plants and the verdant flora; OPEN_PLANTS) beside it or below it: it would flow at
          the first update. A block with a `waterlogged` property is closed (vanilla lets only water into it), or
  water   water on any of its six sides: water, a waterlogged block, seagrass, kelp or a bubble column (sludge next to
          water turns into mud, `ToxicPoolInteractions`).
Flowing sludge (level > 0) is counted on its own (`flow`): worldgen places sources only.

Swamp gas (`corruptedadventure:swamp_gas`) is informative except against water: gas pockets (76), sinkhole layers (76)
and gas halls (73) are dry, so `gas~w` (gas with water on any side) is an overtreding as well. Gas against air is
listed (`gas~a`) but not counted: the gas halls open into the dry tunnels and the sinkhole layers lie under open air.

Neighbours outside the area (or in unsaved chunks) count as neither air nor water. Blocks are grouped 6-connected; the
list shows the lowest block of each group. Exit code 1 with --strict when any area has an overtreding.
"""
import argparse
import json
import os
import re
import sys
from collections import Counter

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import region  # noqa: E402
from lavascan import N6, groups, shifted  # noqa: E402

AIR = ("minecraft:air", "minecraft:cave_air")
# Non-solid blocks a fluid flows into (and breaks): they count as open next to sludge. A heuristic by name; a block
# with a waterlogged property (glow lichen, hanging roots, propagules, reeds, hanging moss) never counts.
OPEN_PLANTS = re.compile(r"(short_grass|tall_grass|:fern$|large_fern|dead_bush|vine|^minecraft:snow$|dandelion|poppy|"
                         r"orchid|allium|bluet|tulip|daisy|cornflower|lily_of_the_valley|torchflower|sunflower|lilac|"
                         r"rose_bush|peony|sapling|_mushroom$|_carpet$|glow_bloom|glowcap|slime_trail|verdant_fern|"
                         r"verdant_tall_fern|verdant_bromeliad|verdant_blossom_bush|verdant_giant_leaf|verdant_fruit_pod)")
WATERY = ("minecraft:water", "minecraft:seagrass", "minecraft:tall_seagrass", "minecraft:kelp", "minecraft:kelp_plant",
          "minecraft:bubble_column")
SLUDGE = "corruptedadventure:toxic_pool"
GAS = "corruptedadventure:swamp_gas"
SIDES = ((0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0))


def base(name):
    return name.split("[", 1)[0]


def is_flowing(name):
    """A fluid block state with level > 0 (region.py spells the level out with fluid_levels=True)."""
    if "[" not in name:
        return False
    for prop in name[name.index("[") + 1:-1].split(","):
        k, _, v = prop.partition("=")
        if k == "level":
            return v != "0"
    return False


def load(world, a):
    """sludge, flowing sludge, gas, open (air and washable plants) and water masks [y][z][x] over the whole area, and the number of missing chunks."""
    d = a.dim
    w, h = (a.cx1 - a.cx0 + 1) * 16, (a.cz1 - a.cz0 + 1) * 16
    sludge = np.zeros((d.height, h, w), bool)
    flowing = np.zeros_like(sludge)
    gas = np.zeros_like(sludge)
    air = np.zeros_like(sludge)
    water = np.zeros_like(sludge)
    missing = 0
    for cx, cz in areas.chunks(a):
        r = region.chunk_blocks(world, cx, cz, d, fluid_levels=True, waterlogged=True)
        if r is None:
            missing += 1
            continue
        names, ids = r
        bases = [base(n) for n in names]
        sl = (slice(None), slice((cz - a.cz0) * 16, (cz - a.cz0) * 16 + 16), slice((cx - a.cx0) * 16, (cx - a.cx0) * 16 + 16))
        sludge[sl] = np.array([b == SLUDGE for b in bases])[ids]
        flowing[sl] = np.array([b == SLUDGE and is_flowing(n) for b, n in zip(bases, names)])[ids]
        gas[sl] = np.array([b == GAS for b in bases])[ids]
        air[sl] = np.array([b in AIR or (b == n and bool(OPEN_PLANTS.search(b))) for b, n in zip(bases, names)])[ids]
        water[sl] = np.array([b in WATERY or n.endswith("[waterlogged]") for b, n in zip(bases, names)])[ids]
    region.clear_cache()
    return sludge, flowing, gas, air, water, missing


def grouped(a, mask, list_n):
    points = list(zip(*[c.tolist() for c in np.nonzero(mask)]))
    out = []
    for g in groups(points):
        y, z, x = min(g)
        ys = [p[0] + a.dim.min_y for p in g]
        out.append({"x": a.cx0 * 16 + x, "y": y + a.dim.min_y, "z": a.cz0 * 16 + z, "blocks": len(g),
                    "ymin": min(ys), "ymax": max(ys)})
    out.sort(key=lambda g: (-g["blocks"], g["x"], g["y"], g["z"]))
    return len(out), out[:list_n]


def scan(world, a, list_n):
    sludge, flowing, gas, air, water, missing = load(world, a)
    open_ = air | gas
    side_or_below_open = shifted(open_, -1, 0, 0)
    water_any = np.zeros_like(water)
    gas_air = np.zeros_like(gas)
    for dy, dz, dx in SIDES:
        side_or_below_open |= shifted(open_, dy, dz, dx)
    for dy, dz, dx in N6:
        water_any |= shifted(water, dy, dz, dx)
        gas_air |= shifted(air, dy, dz, dx)
    over_air = sludge & side_or_below_open
    over_water = sludge & water_any
    gas_water = gas & water_any
    over = over_air | over_water | gas_water
    n_groups, examples = grouped(a, over, list_n)
    return {"area": a.name, "dim": a.dim.key, "sludge": int(sludge.sum()), "flowing": int(flowing.sum()),
            "gas": int(gas.sum()), "over_air": int(over_air.sum()), "over_water": int(over_water.sum()),
            "gas_water": int(gas_water.sum()), "gas_air": int((gas & gas_air).sum()), "over": int(over.sum()),
            "over_groups": n_groups, "examples": examples, "missing_chunks": missing}


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("world")
    p.add_argument("areas")
    p.add_argument("--dim", default="ow")
    p.add_argument("--only", nargs="*")
    p.add_argument("--list", type=int, default=10, help="overtreding groups listed per area")
    p.add_argument("--json")
    p.add_argument("--strict", action="store_true")
    a = p.parse_args()
    results = []
    cols = ("sludge", "flowing", "gas", "over_air", "over_water", "gas_water", "gas_air", "over")
    print("%-12s %-6s %8s %6s %8s %7s %7s %6s %7s %6s %6s  %s" % (
        "area", "dim", "sludge", "flow", "gas", "s~air", "s~wat", "gas~w", "gas~a", "over", "groups", "examples (x y z, blocks)"))
    for ar in areas.load(a.areas, a.dim, a.only):
        r = scan(a.world, ar, a.list)
        results.append(r)
        ex = "; ".join("%d %d %d (%d)" % (e["x"], e["y"], e["z"], e["blocks"]) for e in r["examples"][:3]) or "-"
        print("%-12s %-6s %8d %6d %8d %7d %7d %6d %7d %6d %6d  %s%s" % (
            r["area"], r["dim"], r["sludge"], r["flowing"], r["gas"], r["over_air"], r["over_water"], r["gas_water"],
            r["gas_air"], r["over"], r["over_groups"], ex,
            ("  (missing chunks %d)" % r["missing_chunks"]) if r["missing_chunks"] else ""), flush=True)
        for e in r["examples"]:
            print("    over %6d %4d %6d  y %d..%d  %d blocks" % (e["x"], e["y"], e["z"], e["ymin"], e["ymax"], e["blocks"]))
    tot = Counter()
    for r in results:
        for k in cols:
            tot[k] += r[k]
    print("%-12s %-6s %8d %6d %8d %7d %7d %6d %7d %6d" % ("total", "", *(tot[k] for k in cols)))
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=1)
    if a.strict and tot["over"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
