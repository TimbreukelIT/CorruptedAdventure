"""The Hollow (phase 4, ring 1 Nightmare): model, UV layout and textures from the Blockbench mockup.

    python tools/textures/hollow_model.py            (from the repository root)

Reads the approved mockup docs/design/models/nightmare-ring1-de-holle.bbmodel (geometry and bones; its shared material
swatches only tell which material a face is) and writes:

- assets/corruptedadventure/geo/entity/the_hollow.geo.json      Bedrock geometry 1.12.0, as Blockbench's own Bedrock
                                                                 export writes it (x mirrored, x/y rotations negated)
- assets/corruptedadventure/textures/entity/the_hollow.png       base: cracked stone and deepslate plates, moss,
                                                                 the corruption that has set in, void, soul remnants
- assets/corruptedadventure/textures/entity/the_hollow_glowmask.png   emissive (GeckoLib AutoGlowingGeoLayer): Herobrine's
                                                                 eyes, soul remnants, corruption rims, glitch pixels
- assets/corruptedadventure/textures/entity/the_hollow_corruption.png (+ .mcmeta)  the overlay that slides down: 8
                                                                 frames, drips flowing down in world space
- docs/design/models/the_hollow.bbmodel                          the same model with the new UVs and the base texture,
                                                                 for Blockbench (previews, animation work)

Every face gets its own place in the atlas (1 texel per model unit) and is painted from where its pixels are in the
world, so plates, moss, corruption and drips run on across cubes. Deterministic: the same input gives the same output.
Palettes: docs/design/fase-4-nightmares-concept.md (De Holle).
"""
import base64
import copy
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "docs/design/models/nightmare-ring1-de-holle.bbmodel")
OUT_BB = os.path.join(ROOT, "docs/design/models/the_hollow.bbmodel")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure")
OUT_GEO = os.path.join(ASSETS, "geo/entity/the_hollow.geo.json")
OUT_TEX = os.path.join(ASSETS, "textures/entity/the_hollow.png")
OUT_GLOW = os.path.join(ASSETS, "textures/entity/the_hollow_glowmask.png")
OUT_CORR = os.path.join(ASSETS, "textures/entity/the_hollow_corruption.png")

ATLAS_W = 256
FRAMES = 8


def hexes(*values):
    return np.array([[int(v[i:i + 2], 16) for i in (0, 2, 4)] for v in values], dtype=np.float64)


STONE = hexes("2E2D33", "3E3D44", "4F4E55", "63626A", "7A7980")
MOSS = hexes("3B4A26", "4F6330", "66803C")
CORRUPTION = hexes("140E1A", "2B1F38", "45305C", "6A4A8C", "9C7AC4")
SOUL = hexes("17758C", "2DB8C8", "5FE3E6", "A8F7F4")
VOID = hexes("07050A")[0]
GLITCH = hexes("F800F8")[0]
WHITE = np.array([255.0, 255.0, 255.0])


# --- geometry -------------------------------------------------------------------------------------------------------

def rotation(deg):
    """Blockbench's rotation: Euler order ZYX (three.js), so M = Rz @ Ry @ Rx."""
    rx, ry, rz = (math.radians(d) for d in deg)
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return mz @ my @ mx


class Node:
    def __init__(self, name, origin, rot, parent):
        self.name, self.origin, self.rot, self.parent = name, np.array(origin, float), rotation(rot), parent

    def to_world(self, points):
        """points (n, 3) in this node's frame -> world."""
        q = (points - self.origin) @ self.rot.T + self.origin
        return self.parent.to_world(q) if self.parent else q

    def dir_to_world(self, vectors):
        q = vectors @ self.rot.T
        return self.parent.dir_to_world(q) if self.parent else q

    def ancestors(self):
        node, names = self, []
        while node:
            names.append(node.name)
            node = node.parent
        return names


ROOT_UUID = "3f0c9a52-5b1e-4c55-9a51-7d1e0f7a0001"
# The heel line: the death animation tips the whole body backwards over it.
ROOT_PIVOT = [0, 0, 8]


def load_model():
    with open(SRC, encoding="utf-8") as f:
        model = json.load(f)
    # One root bone over the mockup's loose top-level bones (body, arms, legs, glitch), so animations can move all of it.
    root = copy.deepcopy(model["groups"][0])
    root.update({"name": "root", "uuid": ROOT_UUID, "origin": ROOT_PIVOT, "rotation": [0, 0, 0]})
    model["groups"].insert(0, root)
    model["outliner"] = [{"uuid": ROOT_UUID, "isOpen": True, "children": model["outliner"]}]
    groups = {g["uuid"]: g for g in model["groups"]}
    elements = {e["uuid"]: e for e in model["elements"]}
    placed = []  # (element, node of its parent group)
    bones = []  # (group, parent group name or None)

    def walk(children, parent_node):
        for child in children:
            if isinstance(child, str):
                placed.append((elements[child], parent_node))
                continue
            group = groups[child["uuid"]]
            node = Node(group["name"], group["origin"], group.get("rotation", [0, 0, 0]), parent_node)
            bones.append((group, parent_node.name if parent_node else None, [], node))
            walk(child.get("children", []), node)

    walk(model["outliner"], None)
    for element, node in placed:
        for bone in bones:
            if bone[3] is node:
                bone[2].append(element)
    return model, placed, bones


# Per face: corner of texel (0, 0), u axis, v axis (Blockbench's default cube face orientation) and the normal.
def face_frame(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        "north": ((x1, y1, z0), (-(x1 - x0), 0, 0), (0, -(y1 - y0), 0), (0, 0, -1)),
        "south": ((x0, y1, z1), (x1 - x0, 0, 0), (0, -(y1 - y0), 0), (0, 0, 1)),
        "east": ((x1, y1, z1), (0, 0, -(z1 - z0)), (0, -(y1 - y0), 0), (1, 0, 0)),
        "west": ((x0, y1, z0), (0, 0, z1 - z0), (0, -(y1 - y0), 0), (-1, 0, 0)),
        "up": ((x0, y1, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0), (0, 1, 0)),
        "down": ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, -(z1 - z0)), (0, -1, 0)),
    }[face]


# --- what each part is made of --------------------------------------------------------------------------------------

def material(name):
    if name == "void":
        return "void"
    if name.startswith("soul_remnant"):
        return "soul"
    if name.startswith("eye_"):
        return "eye"
    if name.startswith("glitch_"):
        return "glitch"
    if name.startswith(("corruption_strand", "corruption_tear", "corruption_flow")):
        return "corruption"
    if name.startswith(("socket_", "nose_hole", "crack_")):
        return "hole"
    return "stone"


def corruption_level(name, ancestors):
    """0 = clean stone, 1 = fully corrupt. The giant arm comes through the rift first, so it is the most corrupt; the
    corruption also runs down from the chest."""
    if "hand_giant" in ancestors:
        return 0.95
    if "arm_giant" in ancestors:
        return 0.8
    table = {"shoulder_plate_giant": 0.55, "belly": 0.55, "torso_frame_bottom": 0.5, "pelvis": 0.35,
             "chipped_chunk_5": 0.5, "torso_frame_left": 0.22, "torso_frame_right": 0.3, "torso_frame_top": 0.2}
    if name in table:
        return table[name]
    if name.startswith("rib_stub"):
        return 0.45
    if "head" in ancestors:
        return 0.12
    if "glitch_fx" in ancestors:
        return 1.0
    return 0.06


def moss_level(name, ancestors):
    if name in ("hump", "shoulder_knob_thin"):
        return 0.8
    if name == "torso_back":
        return 0.6
    if name.startswith(("knee_plate", "crest_", "crown_", "foot")) or name == "shoulder_plate_giant":
        return 0.75
    if name in ("skull", "thigh", "shin"):
        return 0.45
    if "arm_giant" in ancestors:
        return 0.1
    return 0.3


# --- noise ----------------------------------------------------------------------------------------------------------

def hash3(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144665) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFF) / float(0xFFFFFF)


def value_noise(p, scale, seed):
    """Trilinear value noise in [0, 1] at world points p (n, 3); scale per axis."""
    q = p * np.asarray(scale)
    i = np.floor(q).astype(np.int64)
    f = q - i
    f = f * f * (3 - 2 * f)
    out = np.zeros(len(p))
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[:, 0] if dx else 1 - f[:, 0]) * (f[:, 1] if dy else 1 - f[:, 1]) * (f[:, 2] if dz else 1 - f[:, 2])
                out += w * hash3(i[:, 0] + dx, i[:, 1] + dy, i[:, 2] + dz, seed)
    return out


def fbm(p, scale, seed, octaves=3):
    total, amp, norm = np.zeros(len(p)), 1.0, 0.0
    for o in range(octaves):
        total += amp * value_noise(p, np.asarray(scale) * (2 ** o), seed + o * 31)
        norm += amp
        amp *= 0.5
    return total / norm


# --- painting -------------------------------------------------------------------------------------------------------
# Every face is painted in a "gravity canvas": its pixels turned so that row 0 is the highest edge in the world and rows
# run downward. Plates then lie as horizontal courses with their light rim on top, and corruption drips run down, also
# on the rotated arm and leg cubes. Few, flat shapes: plates of one tone, 1 px seams, line cracks, no pixel noise.

def gravity_mode(fc):
    u = fc["node"].dir_to_world(fc["u"][None, :])[0]
    v = fc["node"].dir_to_world(fc["v"][None, :])[0]
    gu = -u[1] / max(np.linalg.norm(u), 1e-9)
    gv = -v[1] / max(np.linalg.norm(v), 1e-9)
    if max(abs(gu), abs(gv)) < 0.35:
        return "flat"
    if abs(gv) >= abs(gu):
        return "v+" if gv > 0 else "v-"
    return "u+" if gu > 0 else "u-"


def to_canvas(a, mode):
    if mode == "v-":
        return a[::-1]
    if mode == "u+":
        return a.swapaxes(0, 1)
    if mode == "u-":
        return a.swapaxes(0, 1)[::-1]
    return a


def from_canvas(a, mode):
    if mode == "v-":
        return a[::-1]
    if mode == "u+":
        return a.swapaxes(0, 1)
    if mode == "u-":
        return a[::-1].swapaxes(0, 1)
    return a


def face_rng(name, face):
    return random.Random(f"{name}/{face}")


def plates(p, cell, seed):
    """Irregular stone plates: 3D Voronoi on a jittered grid, squashed into flat slabs. Per point the plate's tone
    (0..1), the distance to the nearest break line and whether the point lies in the upper part of its plate."""
    q = p / np.asarray(cell)
    base = np.floor(q).astype(np.int64)
    best = np.full(len(p), 9e9)
    second = np.full(len(p), 9e9)
    tone = np.zeros(len(p))
    rel_y = np.zeros(len(p))
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                c = base + np.array([dx, dy, dz])
                jitter = np.stack([hash3(c[:, 0], c[:, 1], c[:, 2], seed + k) for k in range(3)], axis=1)
                d = np.sum((c + jitter - q) ** 2, axis=1)
                closer = d < best
                second = np.where(closer, best, np.minimum(second, d))
                tone = np.where(closer, hash3(c[:, 0], c[:, 1], c[:, 2], seed + 7), tone)
                rel_y = np.where(closer, (c[:, 1] + jitter[:, 1]) - q[:, 1], rel_y)
                best = np.minimum(best, d)
    return tone, np.sqrt(second) - np.sqrt(best), rel_y


def unspeckle(tone, passes=2):
    """Replaces every pixel that shares its tone with none of its 4 neighbours by the middle tone around it, so tones
    come in clusters of a few pixels and never as single specks."""
    h, w = tone.shape
    if h < 2 or w < 2:
        return tone
    for _ in range(passes):
        pad = np.pad(tone, 1, mode="edge")
        around = np.stack([pad[1 + dy:1 + dy + h, 1 + dx:1 + dx + w] for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0))])
        lonely = ~np.any(around == tone, axis=0)
        tone = np.where(lonely, np.sort(around, axis=0)[2], tone)
    return tone


def stone_canvas(h, w, world, rng, deep_bias):
    """Weathered stone and deepslate, organic rather than masonry: tones flowing into each other in clusters of 2-4 px,
    irregular break lines (Voronoi edges, never a grid) with a light lip above them, grime low on the body and stains
    that shift the hue a little. Returns colours (h, w, 3)."""
    flat = world.reshape(-1, 3)
    plate_tone, gap, rel_y = plates(flat, (11.0, 6.0, 11.0), 3)
    deep = (hash3((plate_tone * 1e6).astype(np.int64), 0, 0, 5) < 0.3 + deep_bias)
    shade = (2.0 + (value_noise(flat, (0.3, 0.3, 0.3), 11) - 0.5) * 2.4 + (value_noise(flat, (0.12, 0.12, 0.12), 13) - 0.5) * 1.4
             + (plate_tone - 0.5) * 1.2 - deep * 0.9)
    shade = np.where((gap < 0.17) & (rel_y < 0), shade + 0.9, shade)
    tone = unspeckle(np.clip(np.round(shade), 0, 4).astype(int).reshape(h, w))
    color = STONE[tone]
    color[(gap < 0.06).reshape(h, w)] = STONE[0] * 0.55
    # Grime: darker toward the feet, smoothly.
    low = np.clip((34.0 - world[..., 1]) / 34.0, 0, 1)[..., None]
    color = color * (1.0 - 0.3 * low)
    # Stains: greenish damp and a purple bruise of corruption, a few blocks across, stretched downward.
    stretched = (world * np.array([1.0, 0.4, 1.0])).reshape(-1, 3)
    damp = np.clip((value_noise(stretched, (0.2, 0.2, 0.2), 15) - 0.62) * 4.0, 0, 1).reshape(h, w, 1)
    bruise = np.clip((value_noise(stretched, (0.17, 0.17, 0.17), 17) - 0.7) * 4.0, 0, 1).reshape(h, w, 1)
    color = color * (1 - 0.3 * damp) + MOSS[0] * 0.3 * damp
    color = color * (1 - 0.25 * bruise) + CORRUPTION[1] * 0.25 * bruise
    return color


def add_cracks(h, w, rng, count):
    crack = np.zeros((h, w), dtype=bool)
    for _ in range(count):
        r, c = rng.randrange(h), rng.randrange(w)
        for _ in range(rng.randint(3, 8)):
            if 0 <= r < h and 0 <= c < w:
                crack[r, c] = True
            r += 1
            c += rng.choice((-1, 0, 0, 1))
    return crack


def drips(h, w, rng, level):
    """Corruption running down as a glossy liquid: a coat along the top that breaks up into drips of different widths
    (1-4 px, swelling and thinning, wandering a pixel now and then), with stone between them. Palette index per pixel
    (-1 = none), the specular pixels and the streams (column, width, length) for the overlay."""
    idx = np.full((h, w), -1, dtype=int)
    spec = np.zeros((h, w), dtype=bool)
    streams = []
    if level <= 0.15:
        return idx, spec, streams
    c = rng.randrange(4)
    while c < w:
        if rng.random() < min(0.9, level * 1.05):
            base = rng.choice((1, 1, 2, 2, 3, 4) if level > 0.6 else (1, 1, 1, 2, 2, 3))
            full = rng.random() < (level - 0.7) * 1.5
            length = h if full else min(h, max(2, int(round(h * (0.2 + 0.8 * level * rng.random())))))
            x = c
            for r in range(length):
                # Swells near the top, thins toward the end; drifts a pixel sideways now and then.
                width = base + (1 if r < length * 0.3 and rng.random() < 0.5 else 0) - (1 if r > length * 0.6 and base > 1 else 0)
                if r > 0 and rng.random() < 0.12:
                    x += rng.choice((-1, 1))
                lo, hi = max(0, x), min(w, x + max(1, width))
                if lo >= hi:
                    continue
                idx[r, lo:hi] = 1
                idx[r, lo] = 3 if hi - lo >= 2 else 2
                if hi - lo >= 3:
                    idx[r, hi - 1] = 0
            spot = length // 3
            if 0 <= x < w:
                idx[spot, max(0, x)] = 4
                spec[spot, max(0, x)] = True
            if length < h:
                tip = length - 1
                lo, hi = max(0, x), min(w, x + base + 1)
                if lo < hi:
                    idx[tip, lo:hi] = 2
                    idx[tip, lo] = 4
                    spec[tip, lo] = True
                    if tip + 1 < h and hi - lo >= 2:
                        idx[tip + 1, lo + 1] = 1
            streams.append((max(0, min(w - 1, c)), base, length))
            c += base
        c += rng.randint(1, 4) if level < 0.8 else rng.randint(1, 3)
    # The coat at the top, thicker where there is more corruption, ragged at its lower edge.
    coat = int(round(level * 2.5))
    for col in range(w):
        d = max(0, coat + rng.choice((-1, 0, 0, 1, 1)))
        for r in range(min(d, h)):
            if idx[r, col] < 0:
                idx[r, col] = 1
        if d > 0 and idx[0, col] in (0, 1):
            idx[0, col] = 2
    return idx, spec, streams


def noise2(world, a, b, scale, seed):
    pts = np.stack([world[..., a], world[..., b], np.zeros(world.shape[:2])], -1).reshape(-1, 3)
    return value_noise(pts, (scale, scale, 1.0), seed).reshape(world.shape[:2])


def paint_face(fc, world_face, name, ancestors, mat):
    """Colour (h, w, 3), glow (h, w) and overlay frames (FRAMES, h, w, 4) of one face, in face pixel order."""
    mode = gravity_mode(fc)
    world = to_canvas(world_face, mode)
    h, w = world.shape[:2]
    rng = face_rng(name, fc["face"])
    color = np.zeros((h, w, 3))
    glow = np.zeros((h, w), dtype=bool)
    over = np.zeros((FRAMES, h, w, 4))
    up = fc["world_normal"][1]

    if mat in ("void", "hole"):
        color[:] = VOID
        if name.startswith("crack_"):
            # The corruption seeps out of the cracks.
            seep = np.zeros((h, w), dtype=bool)
            seep[rng.randrange(h)::max(2, h // 2), :] = True
            color[seep] = CORRUPTION[3]
            glow |= seep
        return finish(color, glow, over, mode)
    if mat == "eye":
        color[:] = WHITE
        glow[:] = True
        return finish(color, glow, over, mode)
    if mat == "glitch":
        color[:] = GLITCH
        glow[:] = True
        return finish(color, glow, over, mode)
    if mat == "soul":
        rows = np.linspace(3, 1, h).round().astype(int) if h > 1 else np.array([2])
        color[:] = SOUL[rows][:, None, :]
        glow[:] = True
        return finish(color, glow, over, mode)

    level = 1.0 if mat == "corruption" else corruption_level(name, ancestors)
    # The corruption runs down from the chest: near the middle of the body, below its void.
    if "body" in ancestors and float(np.mean(world[..., 1])) < 66:
        level = max(level, float(np.max(np.exp(-((world[..., 0] / 7.0) ** 2)))) * 0.55)

    if mat == "corruption":
        color[:] = CORRUPTION[1]
    else:
        color[:] = stone_canvas(h, w, world, rng, 0.15 if any(a.startswith("leg") for a in ancestors) else 0.0)
        color[add_cracks(h, w, rng, (h * w) // 220)] = VOID

    moss_w = moss_level(name, ancestors)
    if mode == "flat" and up > 0.5 and moss_w >= 0.45:
        # Moss in patches on what faces up, its tones in clusters.
        moss = noise2(world, 0, 2, 0.2, 29) > 1.0 - moss_w * 0.9
        mtone = unspeckle(np.clip((noise2(world, 0, 2, 0.45, 31) * 3.2 - 0.3).astype(int), 0, 2))
        color[moss] = MOSS[mtone[moss]]
    elif mode != "flat" and moss_w >= 0.45:
        # A ragged fringe hanging over the top edge, in runs of a few columns.
        col = 0
        while col < w:
            run = rng.randint(2, 5)
            if rng.random() < moss_w * 0.7:
                d = rng.randint(1, 4)
                for c in range(col, min(col + run, w)):
                    dd = max(1, d + rng.choice((-1, 0, 0, 1)))
                    color[:dd, c] = MOSS[1]
                    color[0, c] = MOSS[2]
                    if dd < h and rng.random() < 0.4:
                        color[dd, c] = MOSS[0]
            col += run

    if mode == "flat":
        if level >= 0.5 and up > 0.5:
            pool = noise2(world, 0, 2, 0.3, 33) < level * 0.8
            color[pool] = CORRUPTION[1]
            color[pool & ~np.roll(pool, 1, axis=0)] = CORRUPTION[3]
        elif level >= 0.5:
            color[:] = CORRUPTION[1]
        return finish(color, glow, over, mode)

    idx, spec, streams = drips(h, w, rng, level)
    liquid = idx >= 0
    color[liquid] = CORRUPTION[np.clip(idx[liquid], 0, 4)]
    glow |= spec
    # A few glitch pixels, only at drip ends where the corruption is thick.
    if level >= 0.6:
        for c, width, length in streams:
            if length < h and rng.random() < 0.12:
                gc = min(w - 1, c + width - 1)
                color[length - 1, gc] = GLITCH
                glow[length - 1, gc] = True
    # The overlay: a bright drop sliding down each longer drip, one loop over the frames.
    for c, width, length in streams:
        if length < 4:
            continue
        phase = rng.random()
        for frame in range(FRAMES):
            r = int(((frame / FRAMES + phase) % 1.0) * length)
            over[frame, r, c] = (*CORRUPTION[4], 235)
            if r > 0:
                over[frame, r - 1, c] = (*CORRUPTION[3], 170)
    return finish(color, glow, over, mode)


def finish(color, glow, over, mode):
    return from_canvas(color, mode), from_canvas(glow, mode), np.stack([from_canvas(f, mode) for f in over])


# --- atlas ----------------------------------------------------------------------------------------------------------

def pack(faces):
    """Shelf packing, tallest first; returns slots (x, y) and the atlas height (a multiple of 16)."""
    order = sorted(range(len(faces)), key=lambda i: (-faces[i]["sh"], -faces[i]["sw"]))
    x = y = shelf = 0
    for i in order:
        f = faces[i]
        if x + f["sw"] > ATLAS_W:
            x, y, shelf = 0, y + shelf, 0
        f["sx"], f["sy"] = x, y
        x += f["sw"]
        shelf = max(shelf, f["sh"])
    height = y + shelf
    return int(math.ceil(height / 16.0) * 16)


def main():
    model, placed, bones = load_model()
    faces = []
    for element, node in placed:
        f, t = element["from"], element["to"]
        e_node = Node(element["name"], element["origin"], element.get("rotation", [0, 0, 0]), node)
        for face in ("north", "east", "south", "west", "up", "down"):
            corner, uax, vax, nrm = face_frame(face, f, t)
            w, h = np.linalg.norm(uax), np.linalg.norm(vax)
            faces.append({"element": element, "face": face, "node": e_node, "corner": np.array(corner, float),
                          "u": np.array(uax, float), "v": np.array(vax, float), "normal": np.array(nrm, float),
                          "w": w, "h": h, "sw": max(1, int(math.ceil(w - 1e-6))), "sh": max(1, int(math.ceil(h - 1e-6)))})
    atlas_h = pack(faces)
    base = np.zeros((atlas_h, ATLAS_W, 4), dtype=np.uint8)
    glowmap = np.zeros_like(base)
    overlay = np.zeros((atlas_h * FRAMES, ATLAS_W, 4), dtype=np.uint8)

    for fc in faces:
        el = fc["element"]
        name = el["name"]
        sw, sh = fc["sw"], fc["sh"]
        jj, ii = np.meshgrid(np.arange(sh), np.arange(sw), indexing="ij")
        uu = ((ii + 0.5) / sw).reshape(-1, 1)
        vv = ((jj + 0.5) / sh).reshape(-1, 1)
        world = fc["node"].to_world(fc["corner"] + uu * fc["u"] + vv * fc["v"]).reshape(sh, sw, 3)
        fc["world_normal"] = fc["node"].dir_to_world(fc["normal"][None, :])[0]
        color, glow, over = paint_face(fc, world, name, fc["node"].ancestors(), material(name))
        ys, xs = slice(fc["sy"], fc["sy"] + sh), slice(fc["sx"], fc["sx"] + sw)
        base[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        base[ys, xs, 3] = 255
        glowmap[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        glowmap[ys, xs, 3] = np.where(glow, 255, 0)
        for frame in range(FRAMES):
            oy = slice(fc["sy"] + frame * atlas_h, fc["sy"] + frame * atlas_h + sh)
            overlay[oy, xs] = np.clip(over[frame], 0, 255).astype(np.uint8)
        fc["uv"] = [fc["sx"], fc["sy"], fc["sx"] + fc["w"], fc["sy"] + fc["h"]]

    for fc in faces:
        if "uv" not in fc:
            fc["uv"] = [fc["sx"], fc["sy"], fc["sx"] + fc["w"], fc["sy"] + fc["h"]]

    for path in (OUT_TEX, OUT_GLOW, OUT_CORR, OUT_GEO):
        os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(base, "RGBA").save(OUT_TEX, optimize=True)
    Image.fromarray(glowmap, "RGBA").save(OUT_GLOW, optimize=True)
    Image.fromarray(overlay, "RGBA").save(OUT_CORR, optimize=True)
    with open(OUT_CORR + ".mcmeta", "w", encoding="utf-8", newline="\n") as f:
        json.dump({"animation": {"frametime": 3, "interpolate": False}}, f, indent=2)
        f.write("\n")

    write_geo(bones, faces, atlas_h)
    write_bbmodel(model, faces, atlas_h)
    print(f"atlas {ATLAS_W}x{atlas_h}, {len(faces)} faces, overlay {FRAMES} frames")


def rounded(values):
    return [round(float(v), 5) for v in values]


def write_geo(bones, faces, atlas_h):
    by_element = {}
    for fc in faces:
        by_element.setdefault(fc["element"]["uuid"], {})[fc["face"]] = fc["uv"]
    out_bones = []
    for group, parent, elements, node in bones:
        o = group["origin"]
        bone = {"name": group["name"]}
        if parent:
            bone["parent"] = parent
        bone["pivot"] = rounded([-o[0], o[1], o[2]])
        r = group.get("rotation", [0, 0, 0])
        if any(r):
            bone["rotation"] = rounded([-r[0], -r[1], r[2]])
        cubes = []
        for el in elements:
            f, t = el["from"], el["to"]
            cube = {"origin": rounded([-t[0], f[1], f[2]]), "size": rounded([t[i] - f[i] for i in range(3)])}
            r = el.get("rotation", [0, 0, 0])
            if any(r):
                eo = el["origin"]
                cube["pivot"] = rounded([-eo[0], eo[1], eo[2]])
                cube["rotation"] = rounded([-r[0], -r[1], r[2]])
            if el.get("inflate"):
                cube["inflate"] = el["inflate"]
            uv = {}
            for face in ("north", "east", "south", "west", "up", "down"):
                u0, v0, u1, v1 = by_element[el["uuid"]][face]
                if face in ("up", "down"):
                    uv[face] = {"uv": rounded([u1, v1]), "uv_size": rounded([u0 - u1, v0 - v1])}
                else:
                    uv[face] = {"uv": rounded([u0, v0]), "uv_size": rounded([u1 - u0, v1 - v0])}
            cube["uv"] = uv
            cubes.append(cube)
        if cubes:
            bone["cubes"] = cubes
        out_bones.append(bone)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.the_hollow", "texture_width": ATLAS_W, "texture_height": atlas_h,
                        "visible_bounds_width": 7, "visible_bounds_height": 8.5, "visible_bounds_offset": [0, 3.75, 0]},
        "bones": out_bones}]}
    with open(OUT_GEO, "w", encoding="utf-8", newline="\n") as f:
        json.dump(geo, f, indent="\t")
        f.write("\n")


def write_bbmodel(model, faces, atlas_h):
    out = copy.deepcopy(model)
    by_uuid = {e["uuid"]: e for e in out["elements"]}
    for fc in faces:
        by_uuid[fc["element"]["uuid"]]["faces"][fc["face"]]["uv"] = rounded(fc["uv"])
    out["name"] = "the_hollow"
    out["model_identifier"] = "the_hollow"
    out["resolution"] = {"width": ATLAS_W, "height": atlas_h}
    with open(OUT_TEX, "rb") as f:
        data = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")
    tex = out["textures"][0]
    tex.update({"name": "the_hollow.png", "path": "", "relative_path": "", "width": ATLAS_W, "height": atlas_h,
                "uv_width": ATLAS_W, "uv_height": atlas_h, "source": data})
    with open(OUT_BB, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    sys.exit(main())
