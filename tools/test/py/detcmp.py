"""Determinism: compare two worlds block by block over an area file (world B generated in the reverse order).

    python detcmp.py <worldA> <worldB> <areas> [--dim ow|nether] [--only name ...] [--all] [--list 5] [--json out]

Per area: chunks identical in every block, and the differences split into
  own    positions where either side is a mod block (corruptedadventure:*) - must be 0;
  other  vanilla blocks outside the known noise (lava/water/air shifts that are not noise, obsidian, ...);
  noise  vanilla features known to depend on the load order (ore blobs, springs, stone variants; NOISY below).
--all lists the most common differing pairs of every kind, not only own/other. Exit code 1 when any own block differs.
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

MOD = "corruptedadventure:"
# Vanilla feature blocks known to depend on thread/load order (ore blobs, water/lava springs, stone variants).
NOISY = {"minecraft:andesite", "minecraft:diorite", "minecraft:granite", "minecraft:tuff", "minecraft:gravel", "minecraft:dirt",
         "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:copper_ore", "minecraft:deepslate_coal_ore",
         "minecraft:deepslate_iron_ore", "minecraft:deepslate_copper_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
         "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
         "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
         "minecraft:clay", "minecraft:water", "minecraft:lava", "minecraft:air", "minecraft:cave_air", "minecraft:stone",
         "minecraft:deepslate", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore", "minecraft:magma_block",
         "minecraft:soul_sand", "minecraft:blackstone", "minecraft:netherrack"}


def kind(a, b):
    if a.startswith(MOD) or b.startswith(MOD):
        return "own"
    if a in NOISY and b in NOISY:
        return "noise"
    return "other"


def compare(wa, wb, ar, nlist):
    same = chunks = missing = 0
    pairs = {"own": Counter(), "other": Counter(), "noise": Counter()}
    samples = {"own": [], "other": []}
    for cx, cz in areas.chunks(ar):
        sa = region.chunk_states(wa, cx, cz, ar.dim)
        sb = region.chunk_states(wb, cx, cz, ar.dim)
        if sa is None or sb is None:
            missing += 1
            continue
        chunks += 1
        d = sa != sb
        if not d.any():
            same += 1
            continue
        av, bv = sa[d].tolist(), sb[d].tolist()  # same order as np.argwhere(d)
        kinds = {}
        for pair, n in Counter(zip(av, bv)).items():
            kinds[pair] = kind(*pair)
            pairs[kinds[pair]][pair] += n
        if any(k != "noise" and len(samples[k]) < nlist for k in kinds.values()):
            for (y, z, x), a, b in zip(np.argwhere(d), av, bv):
                k = kinds[(a, b)]
                if k != "noise" and len(samples[k]) < nlist:
                    samples[k].append((cx * 16 + int(x), int(y) + ar.dim.min_y, cz * 16 + int(z), a, b))
    region.clear_cache()
    return {"area": ar.name, "dim": ar.dim.key, "chunks": chunks, "identical": same, "missing": missing,
            "own": sum(pairs["own"].values()), "other": sum(pairs["other"].values()), "noise": sum(pairs["noise"].values()),
            "pairs": {k: [[a, b, n] for (a, b), n in c.most_common(8)] for k, c in pairs.items()}, "samples": samples}


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("world_a")
    p.add_argument("world_b")
    p.add_argument("areas")
    p.add_argument("--dim")
    p.add_argument("--only", nargs="*")
    p.add_argument("--all", action="store_true")
    p.add_argument("--list", type=int, default=5)
    p.add_argument("--json")
    a = p.parse_args()
    results = []
    tot = Counter()
    for ar in areas.load(a.areas, a.dim, a.only):
        r = compare(a.world_a, a.world_b, ar, a.list)
        results.append(r)
        for k in ("chunks", "identical", "own", "other", "noise"):
            tot[k] += r[k]
        print("%-10s %-6s %4d/%4d chunks identical  own %d  other %d  noise %d%s" % (
            r["area"], r["dim"], r["identical"], r["chunks"], r["own"], r["other"], r["noise"],
            ("  (missing %d)" % r["missing"]) if r["missing"] else ""), flush=True)
        for k in (("own", "other", "noise") if a.all else ("own", "other")):
            if r["pairs"][k]:
                print("    %-5s %s" % (k, ", ".join("%s->%s %d" % (x.split(":")[-1], y.split(":")[-1], n) for x, y, n in r["pairs"][k])))
            for s in r["samples"].get(k, []):
                print("          at %d %d %d: %s -> %s" % s)
    print("total %d/%d chunks identical; own %d, other %d, noise %d" % (tot["identical"], tot["chunks"], tot["own"], tot["other"], tot["noise"]))
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=1)
    sys.exit(1 if tot["own"] else 0)


if __name__ == "__main__":
    main()
