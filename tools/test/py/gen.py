"""Generate the chunks of an area file on the running server via RCON forceload.

    python gen.py <areas> [--reverse] [--dim ow|nether] [--only name ...] [--block 3] [--settle 0] [--timeout 600]

Each area is cut into tiles of 15 x 15 chunks, each tile into blocks of --block x --block chunks. The blocks are
forceloaded one after the other (the next only once every chunk of the previous one is loaded), so the order in which
chunks are generated follows the list. --reverse reverses everything: the areas (with the Nether lines last, the
Nether comes first), the tiles, the blocks in a tile, and starts every block from its far corner. A tile stays
forceloaded until all its blocks are loaded, plus --settle seconds (fluid and block ticks with the chunks loaded),
then it is released. A block that does not load within --timeout seconds stops the run (exit 1).
Ends with `save-all flush`.
"""
import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
from rcon import Rcon  # noqa: E402

TILE = 15


def split(x0, z0, x1, z1, n):
    for x in range(x0, x1 + 1, n):
        for z in range(z0, z1 + 1, n):
            yield x, z, min(x + n - 1, x1), min(z + n - 1, z1)


def box(x0, z0, x1, z1):
    return "%d %d %d %d" % (x0 * 16, z0 * 16, x1 * 16 + 15, z1 * 16 + 15)


def load_block(r, dim, blk, reverse, timeout):
    x0, z0, x1, z1 = blk
    r.cmd("execute in %s run forceload add %s" % (dim.id, box(*blk)))
    pending = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]
    if reverse:
        pending.reverse()
    t0 = time.time()
    while pending:
        pending = [(x, z) for (x, z) in pending
                   if "passed" not in r.cmd("execute in %s if loaded %d 0 %d" % (dim.id, x * 16, z * 16)).lower()]
        if pending:
            if time.time() - t0 > timeout:
                raise SystemExit("gen: %d chunk(s) of block %s in %s not loaded after %ds, e.g. %s"
                                 % (len(pending), blk, dim.key, timeout, pending[0]))
            time.sleep(0.25)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("areas")
    p.add_argument("--reverse", action="store_true")
    p.add_argument("--dim")
    p.add_argument("--only", nargs="*")
    p.add_argument("--block", type=int, default=3)
    p.add_argument("--settle", type=float, default=0)
    p.add_argument("--timeout", type=float, default=600)
    a = p.parse_args()
    jobs = []
    for ar in areas.load(a.areas, a.dim, a.only):
        for tile in split(ar.cx0, ar.cz0, ar.cx1, ar.cz1, TILE):
            jobs.append((ar, tile, list(split(*tile, a.block))))
    if a.reverse:
        jobs.reverse()
        for _, _, blocks in jobs:
            blocks.reverse()
    r = Rcon(timeout=1800)
    t0 = time.time()
    done = set()
    for ar, tile, blocks in jobs:
        for blk in blocks:
            load_block(r, ar.dim, blk, a.reverse, a.timeout)
        if a.settle:
            time.sleep(a.settle)
        for blk in blocks:
            r.cmd("execute in %s run forceload remove %s" % (ar.dim.id, box(*blk)))
        if ar.name not in done:
            done.add(ar.name)
            print("%-10s %-6s first tile at %.0fs" % (ar.name, ar.dim.key, time.time() - t0), flush=True)
    print("generated %d tiles in %.1fs (%s)" % (len(jobs), time.time() - t0, "reversed" if a.reverse else "forward"))
    print(r.cmd("save-all flush"))


if __name__ == "__main__":
    main()
