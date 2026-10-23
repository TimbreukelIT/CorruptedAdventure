"""Bottom scan of ring 5 (82, V3-A, V9-B): what lies over the void and what the dry rule allows. Offline, on the saved
region files.

    python -I voidscan.py <world> <areas> [--rings rings.txt] [--only name ...] [--wet name ...] [--list 10] [--json out.json] [--strict]

--rings: the ring of every column of the areas (made by RingColumnsExportTest, see there). A column of ring 4 lies in
the noisy margin of the zone 4→5 and keeps ring 4's own rules, so no fault is counted there except `fluidvoid`
(ring 4 water pouring into the void is still a fault); the column is still counted as `ring4`. Without --rings every
column counts; the z45* areas reach into that margin and refuse to run without --rings.

Per area (overworld only; Nether areas are skipped) it counts:

  void      columns without any solid block (open down past the bottom): information, not a fault.
  bedrock   bedrock above Y -60 (vanilla's floor is -64..-60) or bedrock with nothing solid below it down to the
            bottom: the floor may only lie under full ground (V3-A: none under the scars, the belts and the floes).
            Air under a single bedrock block with more floor below (a carver's cave) is vanilla and not counted.
  loose     solid blocks in a 6-connected group of 1 or 2 blocks with nothing solid below them down to the bottom
            (a loose block over the void). The area's outer rim is skipped (its neighbours are unknown).
  fluidvoid water or lava with the void beside it (a horizontal neighbour column without any solid block) or with
            nothing solid below it: fluid that pours into the void.
  water     any water (ring 5 is dry), and
  lavahigh  lava at or above Y -54 (only vanilla's lava layer below -54 in the inner band may stay; its dam and the
            outer band are checked by `lavascan.py` (leaks) and `fluidvoid`).
            Both only in dry areas: every area except those named with --wet (default: none; the zone 4→5 is dry since 85).

Exit code 1 with --strict when any area has a fault (everything but `void`).
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import region  # noqa: E402

AIR = ("minecraft:air", "minecraft:cave_air", "minecraft:void_air")
LAVA_LAYER_TOP = -54
BEDROCK_TOP = -60
DIRS = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))  # (dy, dz, dx)


def load(world, a):
    """solid, water, lava and bedrock masks [y][z][x] over the area, plus the number of missing chunks."""
    d = a.dim
    w, h = (a.cx1 - a.cx0 + 1) * 16, (a.cz1 - a.cz0 + 1) * 16
    solid = np.zeros((d.height, h, w), bool)
    water = np.zeros_like(solid)
    lava = np.zeros_like(solid)
    bedrock = np.zeros_like(solid)
    missing = 0
    for cx, cz in areas.chunks(a):
        r = region.chunk_blocks(world, cx, cz, d)
        if r is None:
            missing += 1
            continue
        names, ids = r
        is_water = np.array([n.startswith("minecraft:water") for n in names])
        is_lava = np.array([n.startswith("minecraft:lava") for n in names])
        is_air = np.array([n in AIR for n in names])
        is_bedrock = np.array([n == "minecraft:bedrock" for n in names])
        sl = (slice(None), slice((cz - a.cz0) * 16, (cz - a.cz0) * 16 + 16), slice((cx - a.cx0) * 16, (cx - a.cx0) * 16 + 16))
        solid[sl] = (~is_air & ~is_water & ~is_lava)[ids]
        water[sl] = is_water[ids]
        lava[sl] = is_lava[ids]
        bedrock[sl] = is_bedrock[ids]
    return solid, water, lava, bedrock, missing


def shifted(m, dy, dz, dx):
    """m moved so that out[y][z][x] = m[y+dy][z+dz][x+dx]; outside the area False."""
    out = np.zeros_like(m)
    ys = slice(max(0, -dy), m.shape[0] - max(0, dy))
    zs = slice(max(0, -dz), m.shape[1] - max(0, dz))
    xs = slice(max(0, -dx), m.shape[2] - max(0, dx))
    ys2 = slice(max(0, dy), m.shape[0] - max(0, -dy))
    zs2 = slice(max(0, dz), m.shape[1] - max(0, -dz))
    xs2 = slice(max(0, dx), m.shape[2] - max(0, -dx))
    out[ys, zs, xs] = m[ys2, zs2, xs2]
    return out


def load_rings(path):
    """{(x, z): ring 4 (True) or not} from an export of RingColumnsExportTest: lines `x z RING_n transition`."""
    out = {}
    for line in open(path, encoding="utf-8"):
        x, z, ring, transition = line.split()
        out[(int(x), int(z))] = ring == "RING_4" and transition == "false"
    return out


def ring4_columns(rings, a):
    """Per column of the area (z, x): whether the layout puts it in ring 4. Raises when a column is missing."""
    w, h = (a.cx1 - a.cx0 + 1) * 16, (a.cz1 - a.cz0 + 1) * 16
    out = np.zeros((h, w), bool)
    for z in range(h):
        for x in range(w):
            key = (a.cx0 * 16 + x, a.cz0 * 16 + z)
            if key not in rings:
                raise SystemExit("--rings has no column %d,%d of area %s" % (key[0], key[1], a.name))
            out[z, x] = rings[key]
    return out


def points(mask, a, list_n):
    ys, zs, xs = np.nonzero(mask)
    return [(int(a.cx0 * 16 + x), int(y + a.dim.min_y), int(a.cz0 * 16 + z)) for y, z, x in list(zip(ys, zs, xs))[:list_n]]


def scan(world, a, wet, list_n, ring4=None):
    solid, water, lava, bedrock, missing = load(world, a)
    min_y = a.dim.min_y
    # Ring 4's columns (--rings) keep ring 4's own rules: they are not counted as faults of the zone or of ring 5.
    keep = np.ones_like(solid) if ring4 is None else ~np.broadcast_to(ring4, solid.shape)
    fluid = water | lava
    water &= keep
    lava &= keep
    bedrock &= keep
    # Anything solid strictly below each block of its column.
    below = np.zeros_like(solid)
    below[1:] = np.logical_or.accumulate(solid, axis=0)[:-1]
    void_column = ~solid.any(axis=0)
    open_below = ~below
    result = {"missing": missing, "void": int(void_column.sum())}

    ys = np.arange(solid.shape[0])[:, None, None] + min_y
    # Above the floor's top, or over the void (nothing solid below; a carver's air under one bedrock block is vanilla).
    bad_bedrock = bedrock & ((ys > BEDROCK_TOP) | open_below & (ys > min_y))
    result["bedrock"] = int(bad_bedrock.sum())

    n = np.zeros(solid.shape, np.int8)
    for d in DIRS:
        n += shifted(solid, *d)
    single = solid & (n == 0)
    one = solid & (n == 1)
    pair = np.zeros_like(solid)
    for d in DIRS:
        pair |= one & shifted(one, *d)
    loose = (single | pair) & open_below
    loose[:, :1, :] = loose[:, -1:, :] = False
    loose[:, :, :1] = loose[:, :, -1:] = False
    loose &= keep
    result["loose"] = int(loose.sum())
    result["ring4"] = 0 if ring4 is None else int(ring4.sum())

    beside_void = np.zeros_like(solid)
    vc = np.broadcast_to(void_column, solid.shape)
    for d in DIRS[2:]:
        beside_void |= shifted(vc, *d)
    fluid_void = fluid & (beside_void | open_below)
    result["fluidvoid"] = int(fluid_void.sum())

    dry = not wet
    result["water"] = int(water.sum()) if dry else 0
    lava_high = lava & (ys >= LAVA_LAYER_TOP) if dry else np.zeros_like(lava)
    result["lavahigh"] = int(lava_high.sum())
    result["dry"] = dry
    if list_n:
        result["examples"] = {k: points(m, a, list_n) for k, m in
                              (("bedrock", bad_bedrock), ("loose", loose), ("fluidvoid", fluid_void),
                               ("water", water if dry else np.zeros_like(water)), ("lavahigh", lava_high)) if m.any()}
    return result


MARGIN_PREFIX = "z45"
FAULTS = ("bedrock", "loose", "fluidvoid", "water", "lavahigh")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("world")
    p.add_argument("areas")
    p.add_argument("--only", nargs="*")
    p.add_argument("--wet", nargs="*", help="areas without the dry rule (default: none)")
    p.add_argument("--list", type=int, default=5)
    p.add_argument("--rings", help="ring of each column (RingColumnsExportTest); ring 4 columns count no faults")
    p.add_argument("--json")
    p.add_argument("--strict", action="store_true")
    args = p.parse_args()
    results = {}
    faults = 0
    rings = load_rings(args.rings) if args.rings else None
    for a in areas.load(args.areas, only_dim="ow", names=args.only):
        if rings is None and a.name.startswith(MARGIN_PREFIX):
            raise SystemExit("area %s reaches into ring 4's margin: pass --rings (RingColumnsExportTest)" % a.name)
        wet = a.name in args.wet if args.wet is not None else False
        r = scan(args.world, a, wet, args.list, None if rings is None else ring4_columns(rings, a))
        region.clear_cache()
        results[a.name] = r
        total = sum(r[k] for k in FAULTS)
        faults += total
        print("%-8s void=%-6d bedrock=%-4d loose=%-4d fluidvoid=%-4d water=%-5d lavahigh=%-4d ring4=%-6d%s%s" % (
            a.name, r["void"], r["bedrock"], r["loose"], r["fluidvoid"], r["water"], r["lavahigh"], r["ring4"],
            "" if r["dry"] else " (wet)", " MISSING %d chunks" % r["missing"] if r["missing"] else ""))
        for kind, pts in r.get("examples", {}).items():
            print("    %s: %s" % (kind, " ".join("%d,%d,%d" % pt for pt in pts)))
    print("total faults: %d" % faults)
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=1)
    sys.exit(1 if args.strict and faults else 0)


if __name__ == "__main__":
    main()
