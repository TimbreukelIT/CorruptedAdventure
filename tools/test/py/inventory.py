"""Block inventory per area plus one exposed sample position per (area, block).

    python inventory.py <world> <areas> <out.json> [--dim ow|nether] [--only name ...] [--grep text]

Counts every block name; a sample is a block with air (or cave_air) directly above or beside it. --grep prints the
counts and samples of the names containing the text (e.g. `--grep ice`).
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

AIR = ("minecraft:air", "minecraft:cave_air")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("world")
    p.add_argument("areas")
    p.add_argument("out")
    p.add_argument("--dim")
    p.add_argument("--only", nargs="*")
    p.add_argument("--grep")
    a = p.parse_args()
    res = {}
    for ar in areas.load(a.areas, a.dim, a.only):
        counts = Counter()
        samples = {}
        for cx, cz in areas.chunks(ar):
            r = region.chunk_blocks(a.world, cx, cz, ar.dim)
            if r is None:
                continue
            names, ids = r
            u, c = np.unique(ids, return_counts=True)
            for i, k in zip(u, c):
                counts[names[i]] += int(k)
            airish = np.isin(ids, [k for k, n in enumerate(names) if n in AIR])
            exposed = np.zeros_like(airish)
            exposed[:-1] |= airish[1:]
            exposed[:, 1:, :] |= airish[:, :-1, :]
            exposed[:, :-1, :] |= airish[:, 1:, :]
            exposed[:, :, 1:] |= airish[:, :, :-1]
            exposed[:, :, :-1] |= airish[:, :, 1:]
            for i in u:
                n = names[i]
                if n in samples or n in AIR:
                    continue
                pos = np.argwhere((ids == i) & exposed)
                if len(pos):
                    y, z, x = pos[len(pos) // 2]
                    samples[n] = [int(cx * 16 + x), int(y + ar.dim.min_y), int(cz * 16 + z)]
        region.clear_cache()
        res[ar.name] = {"dim": ar.dim.key, "counts": dict(counts.most_common()), "samples": samples}
        line = "%-10s %-6s %d kinds" % (ar.name, ar.dim.key, len(counts))
        if a.grep:
            hits = ["%s %d %s" % (n, k, samples.get(n, "-")) for n, k in counts.most_common() if a.grep in n]
            line += ": " + ("; ".join(hits) if hits else "no %r" % a.grep)
        print(line, flush=True)
    with open(a.out, "w", encoding="utf-8") as f:
        json.dump(res, f, indent=0)


if __name__ == "__main__":
    main()
