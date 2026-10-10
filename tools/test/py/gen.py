"""Generate the chunks of an area file on the running server via RCON forceload (tiles of 15 x 15 chunks).

    python -I gen.py <areas> [--reverse] [--dim ow|nether] [--only name ...]

--reverse loads everything in the opposite order: the area list reversed (so with the Nether lines last, the Nether
comes first), the tiles reversed and every tile from its far corner. Ends with `save-all flush`.
"""
import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
from rcon import Rcon  # noqa: E402

TILE = 15


def tiles(a):
    for x in range(a.cx0, a.cx1 + 1, TILE):
        for z in range(a.cz0, a.cz1 + 1, TILE):
            yield x, z, min(x + TILE - 1, a.cx1), min(z + TILE - 1, a.cz1)


def generate(r, dim, x0, z0, x1, z1, reverse=False):
    box = "%d %d %d %d" % (x0 * 16, z0 * 16, x1 * 16 + 15, z1 * 16 + 15)
    r.cmd("execute in %s run forceload add %s" % (dim.id, box))
    pending = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]
    if reverse:
        pending.reverse()
    while pending:
        pending = [(x, z) for (x, z) in pending
                   if "passed" not in r.cmd("execute in %s if loaded %d 0 %d" % (dim.id, x * 16, z * 16)).lower()]
        if pending:
            time.sleep(0.5)
    r.cmd("execute in %s run forceload remove %s" % (dim.id, box))


def main():
    p = argparse.ArgumentParser()
    p.add_argument("areas")
    p.add_argument("--reverse", action="store_true")
    p.add_argument("--dim")
    p.add_argument("--only", nargs="*")
    a = p.parse_args()
    jobs = [(ar, t) for ar in areas.load(a.areas, a.dim, a.only) for t in tiles(ar)]
    if a.reverse:
        jobs.reverse()
    r = Rcon(timeout=1800)
    t0 = time.time()
    last = None
    for ar, (x0, z0, x1, z1) in jobs:
        generate(r, ar.dim, x0, z0, x1, z1, a.reverse)
        if ar.name != last:
            print("%-10s %-6s %.0fs" % (ar.name, ar.dim.key, time.time() - t0), flush=True)
            last = ar.name
    print("generated %d tiles in %.1fs (%s)" % (len(jobs), time.time() - t0, "reversed" if a.reverse else "forward"))
    print(r.cmd("save-all flush"))


if __name__ == "__main__":
    main()
