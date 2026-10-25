"""Spots for a ring walk (change 100): `/ca tpring` / `/ca tpboundary` with the player, written as walk.py poses.

    python tools/test/py/ringspots.py <poses out> [--player Dev] [--json out.json] SPEC ...

SPEC is `<label>:<ring|boundary><n>[:nether][:<heading°>]`, e.g. `r1:ring1`, `b12:boundary1`, `n3:ring3:nether`,
`b45n:boundary4:nether:90`. Heading in degrees from +x towards +z (0 = east, 90 = south; default 0). Per spot: the
landing position, `/ca ring` there and the biome (`execute if biome` over the mod's biomes, else `vanilla`). The pose
looks outward along the heading: overworld from 60 blocks further in and 40 up, the Nether from the landing spot.
"""
import argparse
import json
import math
import os
import re
import sys

sys.dont_write_bytecode = True
from rcon import Rcon  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
BIOME_DIR = os.path.join(ROOT, "src", "generated", "resources", "data", "corruptedadventure", "worldgen", "biome")
DIMS = {False: "minecraft:overworld", True: "minecraft:the_nether"}


def mod_biomes():
    return sorted("corruptedadventure:" + f[:-5] for f in os.listdir(BIOME_DIR) if f.endswith(".json"))


def biome_at(rcon, dim, x, y, z, biomes):
    for b in biomes:
        if "passed" in rcon.cmd("execute in %s if biome %d %d %d %s" % (dim, x, y, z, b)).lower():
            return b
    return "vanilla"


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("poses")
    p.add_argument("specs", nargs="+")
    p.add_argument("--player", default="Dev")
    p.add_argument("--json")
    a = p.parse_args()
    rcon = Rcon()
    biomes = mod_biomes()
    rcon.cmd("gamemode creative %s" % a.player)
    lines, results = [], []
    for spec in a.specs:
        parts = spec.split(":")
        label, target = parts[0], parts[1]
        nether = "nether" in parts[2:]
        heading = next((float(q) for q in parts[2:] if q != "nether"), 0.0)
        kind, n = re.match(r"(ring|boundary)(\d)", target).groups()
        dim = DIMS[nether]
        hx, hz = math.cos(math.radians(heading)), math.sin(math.radians(heading))
        rcon.cmd("execute in %s run tp %s %.1f 200 %.1f" % (dim, a.player, 100 * hx, 100 * hz))
        out = rcon.cmd("execute as %s run ca %s %s%s" % (a.player, "tpring" if kind == "ring" else "tpboundary", n,
                                                        " nether" if nether else ""))
        m = re.search(r"at (-?\d+) (-?\d+) (-?\d+)", out)
        if not m:
            results.append({"label": label, "error": out.strip()})
            print("FAILED %s: %s" % (label, out.strip()), flush=True)
            continue
        x, y, z = (int(v) for v in m.groups())
        ring = rcon.cmd("execute in %s positioned %d %d %d run ca ring" % (dim, x, y, z)).splitlines()
        biome = biome_at(rcon, dim, x, y, z, biomes)
        yaw = heading - 90.0  # Minecraft yaw: 0 = +z (south), -90 = +x (east)
        if nether:
            cx, cy, cz, pitch = x + 0.5, y + 1.6, z + 0.5, 5
        else:
            cx, cy, cz, pitch = x - 60 * hx, y + 40, z - 60 * hz, 20
        lines.append("%s %s day %.1f %.1f %.1f %.0f %d" % (label, "nether" if nether else "ow", cx, cy, cz, yaw, pitch))
        r = {"label": label, "spec": spec, "landing": [x, y, z], "ring": ring[0] if ring else "", "biome": biome,
             "landing_line": out.strip().splitlines()[0]}
        results.append(r)
        print(json.dumps(r), flush=True)
    with open(a.poses, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=1)


if __name__ == "__main__":
    main()
