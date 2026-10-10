"""Lava against air per area, split into deliberate (trede, val) and leaks (lek). Offline, on the saved region files.

    python -I lavascan.py <world> <areas> [--dim ow|nether] [--only name ...] [--fall-min 4] [--list 10]
                          [--json out.json] [--strict]

A lava block is *exposed* when a horizontal neighbour or the block below is air (air/cave_air). Above-air is an open
lava surface and fine. Neighbours outside the area (or in unsaved chunks) count as not air. Every exposed block gets
one class (heuristic, see tools/test/README.md, "Lavascan"):

  val    a lava fall, deliberate (39f island falls, 47c wall falls). The exposed blocks are grouped (6-connected);
         a group is a fall if it holds a vertical run of >= --fall-min falling lava blocks (level >= 8), or if it is an
         untouched 47c source (no fluid tick yet): one lava source, solid all round except one air side, under which
         the air drops >= --fall-min blocks onto lava.
         Sub-label `eiland` when the solid block the fall starts from has open air below it within 48 blocks (a
         floating mass), else `wand` (a wall above lava, 47c). Approximate; only the sum matters for the check.
  trede  a river step (42b, RingAquifer `becomesLava`): no air below, and every air side has lava directly under it
         (the neighbouring column's lava lies one block lower; after a fluid tick the overflow on top of it counts too).
  lek    everything else: lava that will flow when a block update reaches it. Listed per group (largest first).

Exit code 1 with --strict when any area has a leak.
"""
import argparse
import json
import os
import sys
from collections import Counter, deque

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import region  # noqa: E402

AIR = ("minecraft:air", "minecraft:cave_air")
SIDES = ((0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0))  # (dy, dz, dx)
N6 = SIDES + ((1, 0, 0), (-1, 0, 0))


def load(world, a):
    """lava, flowing, falling, air and solid masks [y][z][x] over the whole area, plus the number of missing chunks."""
    d = a.dim
    w, h = (a.cx1 - a.cx0 + 1) * 16, (a.cz1 - a.cz0 + 1) * 16
    lava = np.zeros((d.height, h, w), bool)
    flowing = np.zeros_like(lava)
    falling = np.zeros_like(lava)
    air = np.zeros_like(lava)
    solid = np.zeros_like(lava)
    missing = 0
    for cx, cz in areas.chunks(a):
        r = region.chunk_blocks(world, cx, cz, d, fluid_levels=True)
        if r is None:
            missing += 1
            continue
        names, ids = r
        is_lava = np.array([n.startswith("minecraft:lava") for n in names])
        is_flowing = np.array([n.startswith("minecraft:lava[level=") for n in names])
        is_falling = np.array([n.startswith("minecraft:lava[level=") and int(n[21:-1]) >= 8 for n in names])
        is_air = np.array([n in AIR for n in names])
        is_solid = ~is_air & ~is_lava & ~np.array([n.startswith("minecraft:water") for n in names])
        sl = (slice(None), slice((cz - a.cz0) * 16, (cz - a.cz0) * 16 + 16), slice((cx - a.cx0) * 16, (cx - a.cx0) * 16 + 16))
        lava[sl] = is_lava[ids]
        flowing[sl] = is_flowing[ids]
        falling[sl] = is_falling[ids]
        air[sl] = is_air[ids]
        solid[sl] = is_solid[ids]
    region.clear_cache()
    return lava, flowing, falling, air, solid, missing


def shifted(m, dy, dz, dx):
    """out[p] = m[p + d] (False outside)."""
    out = np.zeros_like(m)
    src = tuple(slice(max(0, k), m.shape[i] + min(0, k)) for i, k in enumerate((dy, dz, dx)))
    dst = tuple(slice(max(0, -k), m.shape[i] + min(0, -k)) for i, k in enumerate((dy, dz, dx)))
    out[dst] = m[src]
    return out


def groups(points):
    """6-connected groups of a set of (y, z, x)."""
    left = set(points)
    while left:
        start = left.pop()
        g = [start]
        q = deque([start])
        while q:
            y, z, x = q.popleft()
            for dy, dz, dx in N6:
                p = (y + dy, z + dz, x + dx)
                if p in left:
                    left.remove(p)
                    g.append(p)
                    q.append(p)
        yield g


def air_drop(air, lava, y, z, x):
    """Blocks of air straight down from (y, z, x) (inclusive) and whether lava lies below them."""
    n = 0
    while y - n >= 0 and air[y - n, z, x]:
        n += 1
    return n, y - n >= 0 and lava[y - n, z, x]


def inside(m, y, z, x):
    return 0 <= y < m.shape[0] and 0 <= z < m.shape[1] and 0 <= x < m.shape[2]


def untouched_source(p, lava, flowing, air, solid, fall_min):
    """A 47c source before its first tick: a lava source enclosed by solid blocks except one air side, under which the
    air drops >= fall_min blocks onto lava."""
    y, z, x = p
    if flowing[p]:
        return False
    open_sides = []
    for dy, dz, dx in N6:
        q = (y + dy, z + dz, x + dx)
        if not inside(air, *q):
            return False
        if air[q] and dy == 0:
            open_sides.append(q)
        elif not solid[q]:
            return False
    if len(open_sides) != 1:
        return False
    n, onto = air_drop(air, lava, *open_sides[0])
    return onto and n >= fall_min


def fall_kind(g, falling, lava, air, solid):
    """'eiland' or 'wand': is the solid block the fall leaves from part of a floating mass?"""
    top = max(g)  # highest exposed block of the fall
    y, z, x = top
    for dy, dz, dx in SIDES:
        for up in (0, 1):
            yy, zz, xx = y + up, z + dz, x + dx
            if not inside(solid, yy, zz, xx):
                continue
            if solid[yy, zz, xx] or (lava[yy, zz, xx] and not falling[yy, zz, xx]):
                # the source or the wall: look for open air below it within 48 blocks
                for k in range(1, 49):
                    if yy - k < 0:
                        break
                    if air[yy - k, zz, xx]:
                        below, _ = air_drop(air, lava, yy - k, zz, xx)
                        return "eiland" if below >= 3 else "wand"
                return "wand"
    return "wand"


def scan(world, a, fall_min):
    lava, flowing, falling, air, solid, missing = load(world, a)
    below_air = shifted(air, -1, 0, 0)
    side_air = np.zeros_like(lava)
    for d in SIDES:
        side_air |= shifted(air, *d)
    exposed = lava & (side_air | below_air)
    # trede: no air below, and each air side has lava under it
    trede = exposed & ~below_air
    for dy, dz, dx in SIDES:
        side = shifted(air, dy, dz, dx)
        under = shifted(lava, dy - 1, dz, dx)
        trede &= ~side | under
    points = list(zip(*[c.tolist() for c in np.nonzero(exposed)]))
    cls = {}
    falls = Counter()
    for g in groups(points):
        gs = set(g)
        run = 0
        for (y, z, x) in g:
            if falling[y, z, x] and (y + 1, z, x) not in gs:  # top of a falling run inside the group
                n = 0
                while (y - n, z, x) in gs and falling[y - n, z, x]:
                    n += 1
                run = max(run, n)
        is_fall = run >= fall_min
        if not is_fall and len(g) == 1:
            is_fall = untouched_source(g[0], lava, flowing, air, solid, fall_min)
        if is_fall:
            kind = fall_kind(g, falling, lava, air, solid)
            falls[kind] += 1
            for p in g:
                cls[p] = "val"
        else:
            for p in g:
                cls[p] = "trede" if trede[p] else "lek"
    leaks = []
    for g in groups([p for p, c in cls.items() if c == "lek"]):
        ys = [p[0] + a.dim.min_y for p in g]
        y, z, x = max(g)
        leaks.append({"x": a.cx0 * 16 + x, "y": y + a.dim.min_y, "z": a.cz0 * 16 + z, "blocks": len(g),
                      "ymin": min(ys), "ymax": max(ys), "columns": len({(p[1], p[2]) for p in g})})
    leaks.sort(key=lambda l: -l["blocks"])
    count = Counter(cls.values())
    return {"area": a.name, "dim": a.dim.key, "lava": int(lava.sum()), "flowing": int(flowing.sum()),
            "falling": int(falling.sum()), "exposed": len(points), "trede": count["trede"], "val": count["val"],
            "falls": dict(falls), "lek": count["lek"], "leak_groups": len(leaks), "leaks": leaks, "missing_chunks": missing}


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("world")
    p.add_argument("areas")
    p.add_argument("--dim")
    p.add_argument("--only", nargs="*")
    p.add_argument("--fall-min", type=int, default=4)
    p.add_argument("--list", type=int, default=10, help="leak groups listed per area")
    p.add_argument("--json")
    p.add_argument("--strict", action="store_true")
    a = p.parse_args()
    results = []
    print("%-10s %-6s %9s %7s %7s %7s %7s  %s" % ("area", "dim", "lava", "exposed", "trede", "val", "lek", "groups / falls"))
    for ar in areas.load(a.areas, a.dim, a.only):
        r = scan(a.world, ar, a.fall_min)
        results.append(r)
        falls = ", ".join("%s %d" % kv for kv in sorted(r["falls"].items())) or "-"
        print("%-10s %-6s %9d %7d %7d %7d %7d  lek %d groups; falls: %s%s" % (
            r["area"], r["dim"], r["lava"], r["exposed"], r["trede"], r["val"], r["lek"], r["leak_groups"], falls,
            ("  (missing chunks %d)" % r["missing_chunks"]) if r["missing_chunks"] else ""), flush=True)
        for l in r["leaks"][:a.list]:
            print("    lek %6d %4d %6d  y %d..%d  %d blocks in %d columns" % (
                l["x"], l["y"], l["z"], l["ymin"], l["ymax"], l["blocks"], l["columns"]))
    tot = Counter()
    for r in results:
        for k in ("lava", "exposed", "trede", "val", "lek"):
            tot[k] += r[k]
    print("%-10s %-6s %9d %7d %7d %7d %7d" % ("total", "", tot["lava"], tot["exposed"], tot["trede"], tot["val"], tot["lek"]))
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=1)
    if a.strict and tot["lek"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
