"""Feature index: does ring 1/2 still look the same? Compares a world with a reference world (same seed, made from the
reference checkout, e.g. main) on biomes and terrain shape, ignoring decoration.

    python featidx.py <world> <reference-world> <areas> [--dim ow|nether] [--only name ...]

Per area: chunks with identical biomes (section palettes + data), and the terrain columns whose top "terrain" block
differs (the highest block that is not air, fluid or decoration - trees, plants, snow layers, dripstone, ...; DECO
below), with the largest height difference. Decoration itself shifts with any feature change and is not compared
(CODEBASE.md, pitfall "feature index"). Exit code 1 when biomes differ anywhere.
"""
import argparse
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import region  # noqa: E402

DECO = re.compile(r"(air|water|lava|bubble_column|leaves|_log|_wood|stem|hyphae|grass$|short_grass|tall_grass|fern|flower|bush|"
                  r"vine|sapling|cactus|mushroom|dandelion|poppy|tulip|daisy|orchid|allium|bluet|lily|peony|lilac|rose|cane|"
                  r"bamboo|^minecraft:snow$|pumpkin|melon|bee_nest|azalea|berry|shrub|lichen|pickle|seagrass|kelp|coral|"
                  r"cocoa|dripstone|torch|cobweb|spawner|chest|fence|planks|moss_carpet|hanging|root|sprout|wart|fungus|"
                  r"weeping|twisting|layer|cast|bone_block|skull)")


def tops(names, ids, height):
    terrain = np.array([not DECO.search(n) for n in names])[ids]
    return np.where(terrain.any(0), height - 1 - np.argmax(terrain[::-1], axis=0), -1)


def compare(w, ref, ar):
    n = bsame = cols = cdiff = maxd = 0
    for cx, cz in areas.chunks(ar):
        ca = region.chunk_nbt(w, cx, cz, ar.dim.regiondir)
        cb = region.chunk_nbt(ref, cx, cz, ar.dim.regiondir)
        if ca is None or cb is None:
            continue
        n += 1
        bsame += region.biomes(ca) == region.biomes(cb)
        ra = region.chunk_blocks(w, cx, cz, ar.dim, chunk=ca)
        rb = region.chunk_blocks(ref, cx, cz, ar.dim, chunk=cb)
        d = np.abs(tops(*ra, ar.dim.height) - tops(*rb, ar.dim.height))
        cols += 256
        cdiff += int((d > 0).sum())
        maxd = max(maxd, int(d.max()))
    region.clear_cache()
    return n, bsame, cols, cdiff, maxd


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("world")
    p.add_argument("reference")
    p.add_argument("areas")
    p.add_argument("--dim")
    p.add_argument("--only", nargs="*")
    a = p.parse_args()
    bad = 0
    for ar in areas.load(a.areas, a.dim, a.only):
        n, bsame, cols, cdiff, maxd = compare(a.world, a.reference, ar)
        bad += n - bsame
        print("%-10s %-6s chunks %4d  biomes identical %4d/%4d  terrain columns differing %d/%d (max %d)" % (
            ar.name, ar.dim.key, n, bsame, n, cdiff, cols, maxd), flush=True)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
