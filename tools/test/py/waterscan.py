"""Water against air per area, outside the planned falls. Offline, on the saved region files plus the fall lists.

    python -I waterscan.py <world> <areas> <falls-dir> [--dim ow] [--only name ...] [--list 10] [--json out.json] [--strict]

A water block is *exposed* when a horizontal neighbour or the block below is air (air/cave_air). Air above is a
standing surface and fine. Neighbours outside the area (or in unsaved chunks) count as not air, like lavascan.

Exempt (not an overtreding):
  sea     water on the sea plane Y 63 and the abyss sea Y 0 (contract §4, standing seas)
  fall    the falling column of a planned waterfall (x, z, Y bottom..top) and the four horizontal neighbours of its top
          block (the spring is one of them; the falls do not record it, so all four are exempt)
(The plunge pools of the cliff 3->4 had a rule of their own; the cliff gave way to verdant_falls in W-terrein 2c, whose
falls land in walled pools.)
Everything else that is exposed is an overtreding (`over`). Blocks are grouped 6-connected; the list shows the lowest
block of each group. The fall lists come from `/ca falls x0 z0 x1 z1` (tools/test/waterscan.sh writes them, one file per
area box: <x0>_<z0>_<x1>_<z1>.tsv in <world>/ca_falls); a missing list is an error, never an empty one.

Exit code 1 with --strict when any area has an overtreding.
"""
import argparse
import json
import os
import sys
from collections import Counter

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import region  # noqa: E402
from lavascan import N6, groups, shifted  # noqa: E402

AIR = ("minecraft:air", "minecraft:cave_air")
SEA_Y = 63
ABYSS_Y = 0


def fall_file(falls_dir, a):
    """The fall list of an area box, named as the /ca falls command names it."""
    x0, z0 = a.cx0 * 16, a.cz0 * 16
    x1, z1 = a.cx1 * 16 + 15, a.cz1 * 16 + 15
    return os.path.join(falls_dir, "%d_%d_%d_%d.tsv" % (x0, z0, x1, z1))


def read_falls(path):
    """[(x, z, top, bottom)] from a fall list; missing file is an error."""
    if not os.path.isfile(path):
        raise SystemExit("missing fall list %s (run tools/test/waterscan.sh, or /ca falls for this box)" % path)
    out = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            if len(parts) == 4:
                out.append(tuple(int(v) for v in parts))
    return out


def load(world, a):
    """water, air masks [y][z][x] over the whole area, and the number of missing chunks."""
    d = a.dim
    w, h = (a.cx1 - a.cx0 + 1) * 16, (a.cz1 - a.cz0 + 1) * 16
    water = np.zeros((d.height, h, w), bool)
    air = np.zeros_like(water)
    missing = 0
    for cx, cz in areas.chunks(a):
        r = region.chunk_blocks(world, cx, cz, d, fluid_levels=True)
        if r is None:
            missing += 1
            continue
        names, ids = r
        is_water = np.array([n == "minecraft:water" or n.startswith("minecraft:water[") for n in names])
        is_air = np.array([n in AIR for n in names])
        sl = (slice(None), slice((cz - a.cz0) * 16, (cz - a.cz0) * 16 + 16), slice((cx - a.cx0) * 16, (cx - a.cx0) * 16 + 16))
        water[sl] = is_water[ids]
        air[sl] = is_air[ids]
    region.clear_cache()
    return water, air, missing


def exempt_mask(a, water, falls):
    """Boolean mask of planned falls and their spring cells (array index space of the area)."""
    d = a.dim
    ex = np.zeros_like(water)
    h, w = water.shape[1], water.shape[2]
    x_base, z_base = a.cx0 * 16, a.cz0 * 16

    def idx(y, z, x):
        return y - d.min_y, z - z_base, x - x_base

    def inside(y, z, x):
        yy, zz, xx = idx(y, z, x)
        return 0 <= yy < water.shape[0] and 0 <= zz < h and 0 <= xx < w

    for x, z, top, bottom in falls:
        for y in range(bottom, top + 1):
            if inside(y, z, x):
                ex[idx(y, z, x)] = True
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if inside(top, z + dz, x + dx):
                ex[idx(top, z + dz, x + dx)] = True
    return ex


def scan(world, a, falls_dir, list_n):
    water, air, missing = load(world, a)
    falls = read_falls(fall_file(falls_dir, a))
    below_air = shifted(air, -1, 0, 0)
    side_air = np.zeros_like(water)
    for dy, dz, dx in ((0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
        side_air |= shifted(air, dy, dz, dx)
    exposed = water & (side_air | below_air)
    sea = np.zeros_like(water)
    for absolute_y in (SEA_Y, ABYSS_Y):
        sea[absolute_y - a.dim.min_y] = True
    fall_cells = exempt_mask(a, water, falls)
    exempt = exposed & (sea | fall_cells)
    over = exposed & ~exempt
    points = list(zip(*[c.tolist() for c in np.nonzero(over)]))
    groups_out = []
    for g in groups(points):
        y, z, x = min(g)
        ys = [p[0] + a.dim.min_y for p in g]
        groups_out.append({"x": a.cx0 * 16 + x, "y": y + a.dim.min_y, "z": a.cz0 * 16 + z, "blocks": len(g),
                           "ymin": min(ys), "ymax": max(ys)})
    groups_out.sort(key=lambda g: (-g["blocks"], g["x"], g["y"], g["z"]))
    return {"area": a.name, "dim": a.dim.key, "water": int(water.sum()), "exposed": int(exposed.sum()),
            "exempt": int(exempt.sum()), "falls": len(falls), "over": int(over.sum()),
            "over_groups": len(groups_out), "examples": groups_out[:list_n], "missing_chunks": missing}


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("world")
    p.add_argument("areas")
    p.add_argument("falls")
    p.add_argument("--dim", default="ow")
    p.add_argument("--only", nargs="*")
    p.add_argument("--list", type=int, default=10, help="overtreding groups listed per area")
    p.add_argument("--json")
    p.add_argument("--strict", action="store_true")
    a = p.parse_args()
    results = []
    print("%-12s %-6s %10s %8s %8s %6s %8s %7s  %s" % ("area", "dim", "water", "exposed", "exempt", "falls", "over", "groups", "examples (x y z, blocks)"))
    for ar in areas.load(a.areas, a.dim, a.only):
        r = scan(a.world, ar, a.falls, a.list)
        results.append(r)
        ex = "; ".join("%d %d %d (%d)" % (e["x"], e["y"], e["z"], e["blocks"]) for e in r["examples"][:3]) or "-"
        print("%-12s %-6s %10d %8d %8d %6d %8d %7d  %s%s" % (
            r["area"], r["dim"], r["water"], r["exposed"], r["exempt"], r["falls"], r["over"], r["over_groups"], ex,
            ("  (missing chunks %d)" % r["missing_chunks"]) if r["missing_chunks"] else ""), flush=True)
        for e in r["examples"]:
            print("    over %6d %4d %6d  y %d..%d  %d blocks" % (e["x"], e["y"], e["z"], e["ymin"], e["ymax"], e["blocks"]))
    tot = Counter()
    for r in results:
        for k in ("water", "exposed", "exempt", "falls", "over"):
            tot[k] += r[k]
    print("%-12s %-6s %10d %8d %8d %6d %8d" % ("total", "", tot["water"], tot["exposed"], tot["exempt"], tot["falls"], tot["over"]))
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=1)
    if a.strict and tot["over"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
