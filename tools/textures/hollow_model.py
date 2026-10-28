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
import io
import json
import math
import os
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
FLOW_PERIOD = 8.0


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


def plates(p, cell, seed):
    """Stone plates: 3D Voronoi on a jittered grid, squashed so the plates lie as horizontal slabs. Returns the cell
    tone (0..1), the seam strength (1 on a seam) and whether the pixel is on the plate's upper rim (highlight)."""
    q = p / np.asarray(cell)
    base = np.floor(q).astype(np.int64)
    best = np.full(len(p), 9e9)
    second = np.full(len(p), 9e9)
    tone = np.zeros(len(p))
    best_y = np.zeros(len(p))
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                c = base + np.array([dx, dy, dz])
                jitter = np.stack([hash3(c[:, 0], c[:, 1], c[:, 2], seed + k) for k in range(3)], axis=1)
                d = np.sum((c + jitter - q) ** 2, axis=1)
                closer = d < best
                second = np.where(closer, best, np.minimum(second, d))
                tone = np.where(closer, hash3(c[:, 0], c[:, 1], c[:, 2], seed + 7), tone)
                best_y = np.where(closer, (c[:, 1] + jitter[:, 1]) - q[:, 1], best_y)
                best = np.minimum(best, d)
    gap = np.sqrt(second) - np.sqrt(best)
    return tone, gap, best_y


# --- painting -------------------------------------------------------------------------------------------------------

def paint_face(world, normal, name, ancestors, mat, rng_seed):
    """Colours (n, 3), glow mask (n,) and overlay strength (n,) for the texels at world points."""
    n = len(world)
    color = np.zeros((n, 3))
    glow = np.zeros(n, dtype=bool)
    corr_level = corruption_level(name, ancestors)

    if mat == "void":
        color[:] = VOID
        specks = value_noise(world, (2.0, 2.0, 2.0), 91) > 0.93
        color[specks] = CORRUPTION[1]
        return color, glow, np.zeros(n)
    if mat == "eye":
        color[:] = WHITE
        glow[:] = True
        return color, glow, np.zeros(n)
    if mat == "glitch":
        color[:] = GLITCH
        glow[:] = True
        return color, glow, np.zeros(n)
    if mat == "soul":
        idx = np.clip((fbm(world, (1.3, 1.3, 1.3), 41) * 4.6 - 0.4).astype(int), 0, 3)
        color[:] = SOUL[idx]
        glow[:] = idx >= 1
        return color, glow, np.zeros(n)
    if mat == "hole":
        color[:] = VOID
        rim = value_noise(world, (1.5, 1.5, 1.5), 51) > 0.7
        color[rim] = CORRUPTION[0]
        if name.startswith("crack_"):
            seep = value_noise(world, (1.0, 0.4, 1.0), 53) > 0.6
            color[seep] = CORRUPTION[3]
            glow[seep] = True
        return color, glow, np.full(n, 0.6)
    if mat == "corruption":
        streak = fbm(world * np.array([1.0, 0.25, 1.0]), (0.9, 0.9, 0.9), 61)
        idx = np.clip((streak * 5.5 - 0.6).astype(int), 0, 4)
        color[:] = CORRUPTION[idx]
        glow[:] = idx >= 3
        pix = value_noise(world, (3.0, 3.0, 3.0), 63) > 0.95
        color[pix] = GLITCH
        glow |= pix
        return color, glow, np.ones(n)

    # Stone: flat slabs of stone and deepslate, cracked, chipped and filthy. Few, large shapes; noise stays low so the
    # plates read at a distance instead of turning into speckle.
    tone, gap, rel_y = plates(world, (9.0, 5.0, 9.0), 3)
    deepslate = hash3((tone * 1e6).astype(np.int64), 0, 0, 5) < 0.35
    grime = fbm(world, (0.12, 0.12, 0.12), 11)
    shade = 1.3 + tone * 2.2 - deepslate * 1.0 + (grime - 0.5) * 1.1
    # Within a plate: lighter toward its top, darker toward its foot.
    shade += np.clip(-rel_y, -1.0, 1.0) * 0.5
    # Top rims catch light, bottom rims sit in shadow: the plates read as ledges.
    near_seam = gap < 0.2
    upper_rim = near_seam & (rel_y < 0)
    shade = np.where(upper_rim & (normal[:, 1] > -0.5), shade + 1.0, shade)
    shade = np.where(near_seam & (rel_y >= 0), shade - 1.0, shade)
    # Wear: grime collects low on the body.
    shade -= np.clip((36.0 - world[:, 1]) / 36.0, 0, 1) * 0.8
    pits = value_noise(world, (1.6, 1.6, 1.6), 19) > 0.95
    shade = np.where(pits, shade - 1.5, shade)
    idx = np.clip(np.round(shade), 0, 4).astype(int)
    color[:] = STONE[idx]
    seam = gap < 0.08
    color[seam] = STONE[0] * 0.55
    # A few deep cracks: thin dark lines where a stretched, slow noise crosses a level.
    crack_field = fbm(world * np.array([1.0, 0.5, 1.0]), (0.22, 0.22, 0.22), 23, octaves=2)
    crack = np.abs(crack_field - 0.5) < 0.012
    color[crack] = VOID

    # Moss on what faces up and on the high ledges, hanging a little down the sides.
    moss_w = moss_level(name, ancestors)
    moss_field = fbm(world, (0.45, 0.45, 0.45), 29) + normal[:, 1] * 0.35 + (moss_w - 0.5) * 0.55
    hanging = fbm(world * np.array([1.0, 0.3, 1.0]), (0.8, 0.8, 0.8), 31)
    moss = (moss_field > 0.86) | ((moss_field > 0.7) & (hanging > 0.62) & (np.abs(normal[:, 1]) < 0.5))
    midx = np.clip((fbm(world, (1.4, 1.4, 1.4), 37) * 3.6 - 0.4).astype(int), 0, 2)
    color[moss] = MOSS[midx[moss]]

    # Corruption that has set in: a dark liquid running down in streaks, thickest on the giant arm and below the chest.
    streak = fbm(world * np.array([1.0, 0.15, 1.0]), (0.55, 0.55, 0.55), 43, octaves=2)
    chest = np.exp(-((world[:, 0] / 7.0) ** 2)) * np.clip((64.0 - world[:, 1]) / 22.0, 0, 1) * 0.45
    amount = corr_level + chest + (streak - 0.5) * 1.1
    corrupt = amount > 0.62
    body = fbm(world * np.array([1.0, 0.3, 1.0]), (0.5, 0.5, 0.5), 47, octaves=2)
    cidx = np.clip((body * 3.6 - 1.0 + (amount - 0.62) * 0.6).astype(int), 0, 3)
    cidx = np.where(cidx == 3, np.where(value_noise(world, (1.2, 0.4, 1.2), 48) > 0.75, 3, 2), cidx)
    color[corrupt] = CORRUPTION[cidx[corrupt]]
    # The brightest veins glow faintly in the dark.
    glow |= corrupt & (cidx == 3) & (value_noise(world, (1.3, 1.3, 1.3), 45) > 0.5)
    glitch = corrupt & (value_noise(world, (2.7, 2.7, 2.7), 49) > 0.965)
    color[glitch] = GLITCH
    glow |= glitch
    overlay = np.clip((amount - 0.35) * 1.6, 0, 1)
    return color, glow, overlay, corrupt


def rims(corrupt, w, h):
    """Corrupt texels with a clean 4-neighbour: the glowing edge of the corruption."""
    grid = corrupt.reshape(h, w)
    edge = np.zeros_like(grid)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        shifted = np.ones_like(grid)
        ys = slice(max(dy, 0), h + min(dy, 0))
        xs = slice(max(dx, 0), w + min(dx, 0))
        yd = slice(max(-dy, 0), h + min(-dy, 0))
        xd = slice(max(-dx, 0), w + min(-dx, 0))
        shifted[yd, xd] = grid[ys, xs]
        edge |= grid & ~shifted
    return edge.reshape(-1)


def flow(world, down_v, frame):
    """The overlay's drips: columns of corruption sliding down in world space, periodic so the 8 frames loop."""
    column = np.floor(world[:, 0] * 1.0) * 7 + np.floor(world[:, 2] * 1.0) * 13
    phase = hash3(column.astype(np.int64), 0, 0, 71)
    length = 0.2 + 0.25 * hash3(column.astype(np.int64), 1, 0, 73)
    # Only some columns run at all, so the drips read as single trickles.
    active = hash3(column.astype(np.int64), 2, 0, 75) > 0.6
    t = frame * FLOW_PERIOD / FRAMES
    pos = np.mod(world[:, 1] + phase * FLOW_PERIOD + t, FLOW_PERIOD) / FLOW_PERIOD
    drip = active & (pos < length)
    head = drip & (pos < 0.12)
    return drip, head


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
        ancestors = fc["node"].ancestors()
        mat = material(name)
        sw, sh = fc["sw"], fc["sh"]
        jj, ii = np.meshgrid(np.arange(sh), np.arange(sw), indexing="ij")
        uu = ((ii + 0.5) / sw).reshape(-1, 1)
        vv = ((jj + 0.5) / sh).reshape(-1, 1)
        local = fc["corner"] + uu * fc["u"] + vv * fc["v"]
        world = fc["node"].to_world(local)
        normal = np.repeat(fc["node"].dir_to_world(fc["normal"][None, :]), len(world), axis=0)
        painted = paint_face(world, normal, name, ancestors, mat, 0)
        if len(painted) == 4:
            color, glow, strength, corrupt = painted
            edge = rims(corrupt, sw, sh)
            color[edge] = CORRUPTION[3]
            glow |= edge & (value_noise(world, (1.7, 1.7, 1.7), 59) > 0.45)
        else:
            color, glow, strength = painted
        ys, xs = jj.reshape(-1) + fc["sy"], ii.reshape(-1) + fc["sx"]
        base[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        base[ys, xs, 3] = 255
        glowmap[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        glowmap[ys, xs, 3] = np.where(glow, 255, 0)
        if fc["face"] in ("up", "down"):
            continue
        for frame in range(FRAMES):
            drip, head = flow(world, None, frame)
            alpha = np.where(drip, 170 + 85 * head, 0) * strength
            tint = np.where(head[:, None], CORRUPTION[4], CORRUPTION[2])
            glitchy = drip & (value_noise(world + frame * 1.7, (3.0, 3.0, 3.0), 77) > 0.97)
            tint[glitchy] = GLITCH
            oy = ys + frame * atlas_h
            overlay[oy, xs, :3] = tint.astype(np.uint8)
            overlay[oy, xs, 3] = np.clip(alpha, 0, 255).astype(np.uint8)
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
