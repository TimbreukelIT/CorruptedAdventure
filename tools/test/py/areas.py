"""Area files and dimensions for the scans.

An area file has one area per line: `name cx0 cz0 cx1 cz1 [dim]` in chunk coordinates (inclusive); dim is `ow`
(default) or `nether`. `#` starts a comment. Run as a script to print an area line around a block position:
    python -I areas.py around <name> <x> <z> <radius-in-chunks> [dim]
"""
import os
import sys
from collections import namedtuple

# dimension id, region directory inside the world, lowest Y, height
Dim = namedtuple("Dim", "key id regiondir min_y height")
DIMS = {
    "ow": Dim("ow", "minecraft:overworld", "region", -64, 384),
    "nether": Dim("nether", "minecraft:the_nether", os.path.join("DIM-1", "region"), 0, 256),
}
ALIASES = {"overworld": "ow", "minecraft:overworld": "ow", "the_nether": "nether", "minecraft:the_nether": "nether", "ne": "nether"}

Area = namedtuple("Area", "name cx0 cz0 cx1 cz1 dim")


def dim(key):
    key = ALIASES.get(key, key)
    if key not in DIMS:
        raise SystemExit("unknown dimension %r (ow or nether)" % key)
    return DIMS[key]


def load(path, only_dim=None, names=None):
    """Areas of a file, optionally only one dimension and/or the given names."""
    out = []
    want = set(names or ())
    for line in open(path, encoding="utf-8"):
        line = line.split("#", 1)[0].split()
        if not line:
            continue
        if len(line) not in (5, 6):
            raise SystemExit("%s: bad area line %r" % (path, " ".join(line)))
        d = dim(line[5] if len(line) == 6 else "ow")
        cx0, cz0, cx1, cz1 = map(int, line[1:5])
        a = Area(line[0], min(cx0, cx1), min(cz0, cz1), max(cx0, cx1), max(cz0, cz1), d)
        if only_dim and d.key != dim(only_dim).key:
            continue
        if want and a.name not in want:
            continue
        out.append(a)
    return out


def chunks(a):
    for cx in range(a.cx0, a.cx1 + 1):
        for cz in range(a.cz0, a.cz1 + 1):
            yield cx, cz


if __name__ == "__main__":
    if len(sys.argv) >= 6 and sys.argv[1] == "around":
        name, x, z, r = sys.argv[2], int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5])
        d = sys.argv[6] if len(sys.argv) > 6 else "ow"
        cx, cz = x >> 4, z >> 4
        print("%s %d %d %d %d %s" % (name, cx - r, cz - r, cx + r, cz + r, dim(d).key))
    else:
        print(__doc__)
