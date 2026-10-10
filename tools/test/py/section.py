"""Debug views of a saved world (offline).

    python section.py cut <world> x|z <fixed> <from> <to> <y0> <y1> <out.png> [--dim ow|nether] [--scale 4]
        vertical section along x (at z = fixed) or along z (at x = fixed), one colour per block; prints the legend
    python section.py probe <world> <x> <y> <z> [--r 4] [--dim ow|nether]
        layers y+1, y, y-1 around (x, z) as letters (. air, L lava, W water)
"""
import argparse
import os
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import region  # noqa: E402

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def colour(n, cache={}):
    if n in AIR:
        return (255, 255, 255)
    if n not in cache:
        h = zlib.crc32(n.encode())
        cache[n] = (h & 255, (h >> 8) & 255, (h >> 16) & 255)
    return cache[n]


def block(world, d, x, y, z, cache={}):
    key = (world, d.key, x >> 4, z >> 4)
    if key not in cache:  # decode every chunk once
        cache[key] = region.chunk_blocks(world, x >> 4, z >> 4, d)
    r = cache[key]
    if r is None or not (d.min_y <= y < d.min_y + d.height):
        return None
    return r[0][r[1][y - d.min_y, z & 15, x & 15]]


def cut(a, d):
    from PIL import Image
    img = Image.new("RGB", (a.to - a.frm, a.y1 - a.y0))
    seen = {}
    cache = {}
    for v in range(a.frm, a.to):
        x, z = (v, a.fixed) if a.axis == "x" else (a.fixed, v)
        key = (x >> 4, z >> 4)
        if key not in cache:
            cache[key] = region.chunk_blocks(a.world, key[0], key[1], d)
        r = cache[key]
        if r is None:
            continue
        names, ids = r
        for y in range(a.y0, a.y1):
            n = names[ids[y - d.min_y, z & 15, x & 15]]
            seen[n] = seen.get(n, 0) + 1
            img.putpixel((v - a.frm, a.y1 - 1 - y), colour(n))
    img.resize((img.width * a.scale, img.height * a.scale), Image.NEAREST).save(a.out)
    for n, c in sorted(seen.items(), key=lambda t: -t[1]):
        print(n, c, colour(n))


def probe(a, d):
    legend = {}

    def code(n):
        if n is None:
            return "?"
        s = n.split(":")[1]
        if s in ("air", "cave_air"):
            return "."
        if s == "lava":
            return "L"
        if s == "water":
            return "W"
        if s not in legend:
            legend[s] = chr(ord("a") + len(legend)) if len(legend) < 26 else chr(ord("A") + len(legend) - 26)
        return legend[s]

    for yy in (a.y + 1, a.y, a.y - 1):
        print("y", yy, "(rows z, columns x from", a.x - a.r, ")")
        for zz in range(a.z - a.r, a.z + a.r + 1):
            print("  ", "".join(code(block(a.world, d, xx, yy, zz)) for xx in range(a.x - a.r, a.x + a.r + 1)), " z", zz)
    print({v: k for k, v in legend.items()})


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("cut")
    c.add_argument("world")
    c.add_argument("axis", choices=("x", "z"))
    for k in ("fixed", "frm", "to", "y0", "y1"):
        c.add_argument(k, type=int)
    c.add_argument("out")
    c.add_argument("--scale", type=int, default=4)
    q = sub.add_parser("probe")
    q.add_argument("world")
    for k in ("x", "y", "z"):
        q.add_argument(k, type=int)
    q.add_argument("--r", type=int, default=4)
    for s in (c, q):
        s.add_argument("--dim", default="ow")
    a = p.parse_args()
    (cut if a.cmd == "cut" else probe)(a, areas.dim(a.dim))


if __name__ == "__main__":
    main()
