"""Minimal Anvil reader for 1.21 chunks (pure Python + numpy): NBT, block states per section, heightmaps, biomes.

chunk_blocks(world, cx, cz, dim) -> (names, ids[y][z][x]) with ids indexing names. With fluid_levels=True a flowing
fluid keeps its level in the name ("minecraft:lava[level=8]"; a source stays "minecraft:lava").
"""
import os
import struct
import zlib

import numpy as np


class _R:
    __slots__ = ("b", "i")

    def __init__(self, b):
        self.b = b
        self.i = 0

    def take(self, n):
        v = self.b[self.i:self.i + n]
        self.i += n
        return v


def _payload(r, t):
    if t == 1:
        return struct.unpack(">b", r.take(1))[0]
    if t == 2:
        return struct.unpack(">h", r.take(2))[0]
    if t == 3:
        return struct.unpack(">i", r.take(4))[0]
    if t == 4:
        return struct.unpack(">q", r.take(8))[0]
    if t == 5:
        return struct.unpack(">f", r.take(4))[0]
    if t == 6:
        return struct.unpack(">d", r.take(8))[0]
    if t == 7:
        n = struct.unpack(">i", r.take(4))[0]
        return r.take(n)
    if t == 8:
        n = struct.unpack(">H", r.take(2))[0]
        return r.take(n).decode("utf-8", "replace")
    if t == 9:
        et = r.take(1)[0]
        n = struct.unpack(">i", r.take(4))[0]
        return [_payload(r, et) for _ in range(n)]
    if t == 10:
        d = {}
        while True:
            et = r.take(1)[0]
            if et == 0:
                return d
            nl = struct.unpack(">H", r.take(2))[0]
            name = r.take(nl).decode("utf-8", "replace")
            d[name] = _payload(r, et)
    if t == 11:
        n = struct.unpack(">i", r.take(4))[0]
        return np.frombuffer(r.take(4 * n), dtype=">i4")
    if t == 12:
        n = struct.unpack(">i", r.take(4))[0]
        return np.frombuffer(r.take(8 * n), dtype=">i8")
    raise ValueError("tag %d" % t)


def nbt(data):
    r = _R(data)
    t = r.take(1)[0]
    nl = struct.unpack(">H", r.take(2))[0]
    r.take(nl)
    return _payload(r, t)


_region_cache = {}


def chunk_nbt(world, cx, cz, regiondir="region"):
    """The chunk's NBT compound, or None if the chunk was never saved."""
    path = os.path.join(world, regiondir, "r.%d.%d.mca" % (cx >> 5, cz >> 5))
    if path not in _region_cache:
        if not os.path.exists(path):
            return None
        with open(path, "rb") as f:
            _region_cache[path] = f.read()
    buf = _region_cache[path]
    idx = 4 * ((cx & 31) + (cz & 31) * 32)
    off = int.from_bytes(buf[idx:idx + 3], "big")
    if off == 0:
        return None
    o = off * 4096
    length = struct.unpack(">i", buf[o:o + 4])[0]
    comp = buf[o + 4]
    raw = buf[o + 5:o + 4 + length]
    if comp == 2:
        raw = zlib.decompress(raw)
    elif comp == 1:
        import gzip
        raw = gzip.decompress(raw)
    elif comp != 3:
        raise ValueError("chunk %d,%d: compression %d (external .mcc not supported)" % (cx, cz, comp))
    return nbt(raw)


def clear_cache():
    _region_cache.clear()


def _unpack(longs, bits, count):
    per = 64 // bits
    mask = (1 << bits) - 1
    arr = np.asarray(longs, dtype=">i8").astype(np.int64).view(np.uint64)
    shifts = np.arange(per, dtype=np.uint64) * np.uint64(bits)
    vals = (arr[:, None] >> shifts[None, :]) & np.uint64(mask)
    return vals.reshape(-1)[:count].astype(np.int32)


def _state_name(p, fluid_levels):
    n = p["Name"]
    if fluid_levels and n in ("minecraft:lava", "minecraft:water", "corruptedadventure:toxic_pool"):
        level = p.get("Properties", {}).get("level", "0")
        if level != "0":
            return "%s[level=%s]" % (n, level)
    return n


def is_full(c):
    return c is not None and c.get("Status") in ("minecraft:full", "full")


def chunk_blocks(world, cx, cz, dim=None, fluid_levels=False, chunk=None):
    """(names, ids[y][z][x] int32) for a saved chunk, or None. dim: an areas.Dim (default overworld)."""
    regiondir, min_y, height = (dim.regiondir, dim.min_y, dim.height) if dim else ("region", -64, 384)
    c = chunk if chunk is not None else chunk_nbt(world, cx, cz, regiondir)
    if c is None:
        return None
    names = ["minecraft:air"]
    index = {"minecraft:air": 0}
    out = np.zeros((height, 16, 16), dtype=np.int32)
    for sec in c.get("sections", []):
        base = sec["Y"] * 16 - min_y
        if base < 0 or base >= height:
            continue
        bs = sec.get("block_states")
        if bs is None:
            continue
        ids = []
        for p in bs["palette"]:
            n = _state_name(p, fluid_levels)
            if n not in index:
                index[n] = len(names)
                names.append(n)
            ids.append(index[n])
        ids = np.array(ids, dtype=np.int32)
        if "data" not in bs:
            out[base:base + 16] = ids[0]
        else:
            bits = max(4, (len(bs["palette"]) - 1).bit_length())
            out[base:base + 16] = ids[_unpack(bs["data"], bits, 4096)].reshape(16, 16, 16)
    return names, out


def chunk_states(world, cx, cz, dim=None, fluid_levels=False):
    """Block names as an object array [y][z][x], or None."""
    r = chunk_blocks(world, cx, cz, dim, fluid_levels)
    if r is None:
        return None
    return np.array(r[0], dtype=object)[r[1]]


def biomes(c):
    """Per section: (Y, palette, packed data bytes) - equal tuples mean equal biomes."""
    out = []
    for s in c.get("sections", []):
        b = s.get("biomes", {})
        pal = [x["Name"] if isinstance(x, dict) else x for x in b.get("palette", [])]
        out.append((s["Y"], pal, np.asarray(b.get("data", [])).tobytes()))
    return sorted(out, key=lambda t: t[0])


def heightmap(c, kind="WORLD_SURFACE", min_y=-64):
    v = _unpack(c["Heightmaps"][kind], 9, 256)
    return (v + min_y).reshape(16, 16)  # [z][x], top block y + 1
