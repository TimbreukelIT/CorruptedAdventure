"""The Risen (phase 4, ring 3 Nightmare): model, UV layout and textures from the Blockbench mockup.

    python tools/textures/risen_model.py             (from the repository root)

Reads the approved mockup docs/design/models/nightmare-ring3-de-herrijzende.bbmodel (geometry and bones; its shared
material swatches only tell which material a face is) and writes:

- assets/corruptedadventure/geo/entity/the_risen.geo.json        Bedrock geometry 1.12.0, as Blockbench's own Bedrock
                                                                  export writes it (x mirrored, x/y rotations negated)
- assets/corruptedadventure/textures/entity/the_risen.png         base: charred and ash-grey bone, dark horn, feathers
                                                                  with smouldering edges and burnt-through holes, the
                                                                  furnace in the ribcage with the fused faces of the choir
- assets/corruptedadventure/textures/entity/the_risen_glowmask.png    emissive (GeckoLib AutoGlowingGeoLayer): eyes,
                                                                  furnace, embers, hot seams and feather edges
- assets/corruptedadventure/textures/entity/the_risen_embers.png (+ .mcmeta)  additive overlay, 8 frames: the furnace
                                                                  breathes, sparks run out along the feathers, the faces
                                                                  of the choir sing one after another
- docs/design/models/the_risen.bbmodel                            the same model with the new UVs and the base texture

Every face gets its own place in the atlas (1 texel per model unit). Bone, horn and furnace are painted from where their
pixels are in the world; feathers from their own length and width, so holes and the ragged tip go through both sides.
Deterministic: the same input gives the same output. Palettes: the plate docs/design/img/nightmare-ring3-de-herrijzende.png.
"""
import base64
import copy
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hollow_model import Node, face_frame, fbm, hash3, plates, rounded, value_noise  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "docs/design/models/nightmare-ring3-de-herrijzende.bbmodel")
OUT_BB = os.path.join(ROOT, "docs/design/models/the_risen.bbmodel")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure")
OUT_GEO = os.path.join(ASSETS, "geo/entity/the_risen.geo.json")
OUT_TEX = os.path.join(ASSETS, "textures/entity/the_risen.png")
OUT_GLOW = os.path.join(ASSETS, "textures/entity/the_risen_glowmask.png")
OUT_EMBERS = os.path.join(ASSETS, "textures/entity/the_risen_embers.png")

ATLAS_W = 512
FRAMES = 8


def hexes(*values):
    return np.array([[int(v[i:i + 2], 16) for i in (0, 2, 4)] for v in values], dtype=np.float64)


CHAR = hexes("141011", "2E292A", "3B3536", "494243", "58504F")
ASH = hexes("3A3538", "4B4546", "5D5655", "6F6763", "827973", "968C84")
EMBER = hexes("701808", "A82C0C", "DC5414", "FA8828", "FFC25A", "FFF0BC")
FURNACE = hexes("6E1608", "B0300C", "E4601A", "FF962E", "FFCC5E", "FFF4C4")
BASALT = hexes("151214", "1E191B", "282223", "332B2B", "403533", "50423D")
WHITE = hexes("FFFFFF")[0]
VOID = hexes("0C0A0C")[0]

# The mockup's 4 x 4 grid of 64 px material swatches: (column, row) of a face's uv -> material.
SWATCHES = {(0, 0): "char", (1, 0): "ash", (2, 0): "flame", (3, 0): "smoulder", (0, 1): "furnace",
            (1, 1): "furnace_heart", (2, 1): "feather", (3, 1): "feather_burnt", (0, 2): "feather", (1, 2): "eye",
            (2, 2): "choir_face", (3, 2): "horn", (1, 3): "choir_hole", (2, 3): "feather"}
FEATHERS = ("feather", "feather_burnt")
# The middle of the furnace, where the ribs are lit from (model units).
FURNACE_CENTRE = np.array([0.0, 131.0, -1.0])

ROOT_UUID = "7d3e2b91-8c4a-4f1e-b2a6-5e0c9f3a0003"
# The middle of the body: the flight animations pitch and bank the whole bird around it.
ROOT_PIVOT = [0, 126.36267, 0]


def load_model():
    with open(SRC, encoding="utf-8") as f:
        model = json.load(f)
    root = copy.deepcopy(model["groups"][0])
    root.update({"name": "root", "uuid": ROOT_UUID, "origin": ROOT_PIVOT, "rotation": [0, 0, 0]})
    model["groups"].insert(0, root)
    model["outliner"] = [{"uuid": ROOT_UUID, "isOpen": True, "children": model["outliner"]}]
    groups = {g["uuid"]: g for g in model["groups"]}
    elements = {e["uuid"]: e for e in model["elements"]}
    placed = []  # (element, node of its parent group, the group)
    bones = []  # (group, parent group name or None, its elements, node)

    def walk(children, parent_node, parent_group):
        for child in children:
            if isinstance(child, str):
                placed.append((elements[child], parent_node, parent_group))
                continue
            group = groups[child["uuid"]]
            node = Node(group["name"], group["origin"], group.get("rotation", [0, 0, 0]), parent_node)
            bones.append((group, parent_node.name if parent_node else None, [], node))
            walk(child.get("children", []), node, group)

    walk(model["outliner"], None, None)
    for element, node, _ in placed:
        for bone in bones:
            if bone[3] is node:
                bone[2].append(element)
    return model, placed, bones


def material(element, face):
    name = element["name"]
    if name.startswith("victim_"):
        return "victim"
    if name.startswith("rib_"):
        return "rib"
    if name.startswith("socket_"):
        return "socket"
    u = element["faces"][face]["uv"]
    return SWATCHES.get((int(u[0] // 64), int(u[1] // 64)), "char")


# --- feathers: painted in their own frame -----------------------------------------------------------------------------

def feather_frame(element, group, local):
    """t along the feather (0 at its root, 1 at its tip) and s across it (-1..1) for points in element space."""
    f, t = np.array(element["from"], float), np.array(element["to"], float)
    size = t - f
    axis = int(np.argmax(size))
    across = [a for a in range(3) if a != axis]
    across = across[int(np.argmax(size[across]))]
    pivot = np.array(group["origin"], float) if group else (f + t) / 2
    along = (local[:, axis] - f[axis]) / size[axis]
    # The root is the end nearer to the bone the feather hangs from.
    if abs(f[axis] - pivot[axis]) > abs(t[axis] - pivot[axis]):
        along = 1 - along
    side = (local[:, across] - f[across]) / size[across] * 2 - 1
    return along, side, size[axis]


def paint_feather(along, side, length, world, normal, burnt, seed):
    n = len(along)
    color = np.zeros((n, 3))
    glow = np.zeros(n, dtype=bool)
    alpha = np.full(n, 255)
    # Barbs: dark diagonal bands off a pale, broken shaft.
    barb = np.mod(along * length * 0.55 + np.abs(side) * 2.2 + hash3(seed, 0, 0, 3) * 4, 2.0) < 1.0
    tone = fbm(np.stack([along * length * 0.4, side * 3, np.full(n, seed * 1.0)], axis=1), (0.5, 0.5, 0.5), 101)
    idx = np.clip((tone * 3.4 - 0.6 + barb * 0.8).astype(int), 0, 3)
    color[:] = CHAR[idx]
    shaft = (np.abs(side) < 0.16) & (along < 0.92)
    color[shaft] = ASH[2]
    color[shaft & (value_noise(world, (0.7, 0.7, 0.7), 103) > 0.62)] = CHAR[1]
    # Ash settles on whatever faces up.
    dust = (normal[:, 1] > 0.3) & (fbm(world, (0.6, 0.6, 0.6), 105) > 0.52)
    color[dust] = ASH[3]
    # The edges smoulder, hotter toward the tip; one edge burns more than the other.
    edge_heat = np.clip(np.abs(side) - 0.62, 0, 1) / 0.38 * (0.35 + along * 0.9)
    edge_heat += (side > 0) * 0.15 * (hash3(seed, 1, 0, 7) > 0.5)
    flicker = value_noise(np.stack([along * length * 0.35, side, np.full(n, seed * 1.0)], axis=1), (1, 1, 1), 107)
    hot = edge_heat + (flicker - 0.5) * 0.25
    ember = hot > 0.55
    eidx = np.clip((hot * 4.0 - 1.2).astype(int), 0, 4)
    color[ember] = EMBER[eidx[ember]]
    glow |= ember & (eidx >= 2)
    # Ragged tip: burnt off unevenly, with a glowing rim just before the cut.
    jag = 0.86 + 0.1 * value_noise(np.stack([side * 2.5, np.full(n, seed * 1.0), np.zeros(n)], axis=1), (1, 1, 1), 109)
    cut = along > jag
    rim = (along > jag - 0.035) & ~cut
    color[rim] = EMBER[3]
    glow |= rim
    alpha[cut & (np.abs(side) > 0.12)] = 0
    # Burnt-through holes with glowing edges (most of all on the burnt coverts).
    holes = value_noise(np.stack([along * length * 0.18, side * 1.6, np.full(n, seed * 1.0)], axis=1), (1, 1, 1), 111)
    limit = 0.8 if burnt else 0.9
    hole = (holes > limit) & (along > 0.25) & ~shaft
    hole_rim = (holes > limit - 0.06) & ~hole & (along > 0.25)
    alpha[hole] = 0
    color[hole_rim] = EMBER[2]
    glow |= hole_rim
    return color, glow, alpha, ember | rim | hole_rim


# --- the rest: painted from the world --------------------------------------------------------------------------------

def paint_bone(world, normal, palette, seed, hot_seams):
    """Charred or ash-grey bone: few large plates, dark seams (glowing where it is hot), cracks, soot low down."""
    n = len(world)
    color = np.zeros((n, 3))
    glow = np.zeros(n, dtype=bool)
    tone, gap, _ = plates(world, (7.0, 6.0, 7.0), seed)
    grime = fbm(world, (0.12, 0.12, 0.12), seed + 11, octaves=2)
    top = len(palette) - 1
    shade = 1.2 + tone * (top - 2.2) + (grime - 0.5) * 0.8 + np.clip(normal[:, 1], -1, 1) * 0.5
    # Soot gathers low and in the joints.
    shade -= np.clip((110.0 - world[:, 1]) / 80.0, 0, 1) * 0.8
    pits = value_noise(world, (0.9, 0.9, 0.9), seed + 19) > 0.965
    shade = np.where(pits, shade - 1.5, shade)
    idx = np.clip(np.round(shade), 0, top).astype(int)
    color[:] = palette[idx]
    seam = gap < 0.09
    if hot_seams:
        heat = fbm(world, (0.2, 0.2, 0.2), seed + 23)
        burning = seam & (heat > 0.5)
        color[seam] = CHAR[0]
        color[burning] = EMBER[np.clip(((heat[burning] - 0.5) * 9 + 1).astype(int), 1, 4)]
        glow |= burning & (heat > 0.56)
    else:
        color[seam] = CHAR[1]
    crack_field = fbm(world * np.array([1.0, 0.6, 1.0]), (0.25, 0.25, 0.25), seed + 29, octaves=2)
    color[np.abs(crack_field - 0.5) < 0.013] = VOID
    # Ash dusting on top.
    dust = (normal[:, 1] > 0.4) & (fbm(world, (0.25, 0.25, 0.25), seed + 31) > 0.55)
    color[dust] = ASH[4]
    return color, glow


def paint_horn(world, normal, along, seed):
    """Beak, talons, ribs and spikes: dark horn in long streaks, worn pale at the edges."""
    n = len(world)
    streak = fbm(np.stack([along * 4, world[:, 0] * 0.3 + world[:, 2] * 0.3, world[:, 1] * 0.05], axis=1),
                 (1.0, 1.2, 1.0), seed + 41)
    idx = np.clip((streak * 6.2 - 1.0 + np.clip(normal[:, 1], -1, 1) * 0.6).astype(int), 0, 5)
    color = BASALT[idx].copy()
    chips = value_noise(world, (1.3, 1.3, 1.3), seed + 43) > 0.9
    color[chips] = ASH[1]
    return color, np.zeros(n, dtype=bool)


def paint_rib(world, normal, along, seed):
    """Ribs are lit by the furnace: the side turned to it glows, the side turned away is dark horn."""
    color, glow = paint_horn(world, normal, along, seed)
    to_core = FURNACE_CENTRE - world
    dist = np.linalg.norm(to_core, axis=1)
    facing = np.sum(normal * to_core, axis=1) / np.maximum(dist, 1e-6)
    heat = facing * 1.4 - dist / 40.0 + (value_noise(world, (0.6, 0.6, 0.6), seed + 47) - 0.5) * 0.6
    hot = heat > 0.15
    hidx = np.clip((heat * 4.5).astype(int), 0, 4)
    color[hot] = FURNACE[hidx[hot]]
    glow |= hot & (hidx >= 2)
    # Soot-black sides with ember cracks.
    cracks = (np.abs(fbm(world, (0.4, 0.4, 0.4), seed + 49, octaves=2) - 0.5) < 0.02) & ~hot
    color[cracks] = EMBER[2]
    glow |= cracks
    return color, glow


def paint_furnace(world, heart, seed):
    """Molten glow, and in it the choir: melted faces, eyes and mouths open, sunk into the heat."""
    n = len(world)
    flow = fbm(world * np.array([1.0, 0.35, 1.0]), (0.35, 0.35, 0.35), seed + 51)
    base = 2.0 + (flow - 0.5) * 3.2 + (1.2 if heart else 0.0)
    # Faces: one per cell of a staggered grid, an oval of shadow with three holes of white heat.
    cell = np.array([9.0, 11.0])
    q = np.stack([world[:, 0] / cell[0] + np.floor(world[:, 1] / cell[1]) * 0.5, world[:, 1] / cell[1]], axis=1)
    c = np.floor(q)
    f = q - c - 0.5
    jitter = (np.stack([hash3(c[:, 0].astype(np.int64), c[:, 1].astype(np.int64), 0, seed + k) for k in (1, 2)],
                       axis=1) - 0.5) * 0.18
    f = f - jitter
    melt = 0.12 * np.sin(f[:, 0] * 9 + c[:, 0])
    oval = (f[:, 0] / 0.36) ** 2 + ((f[:, 1] + melt) / 0.46) ** 2 < 1.0
    eye = ((np.abs(np.abs(f[:, 0]) - 0.14) < 0.07) & (np.abs(f[:, 1] + 0.12) < 0.07))
    mouth = (np.abs(f[:, 0]) < 0.09) & (f[:, 1] > 0.08) & (f[:, 1] < 0.3 + melt * 0.5)
    shade = np.where(oval, base - 1.6, base)
    shade = np.where(oval & (eye | mouth), 5.0, shade)
    idx = np.clip(np.round(shade), 0, 5).astype(int)
    color = FURNACE[idx].copy()
    glow = np.ones(n, dtype=bool)
    return color, glow, np.where(oval & (eye | mouth), 2, np.where(oval, 1, 0))


def paint_choir_face(world, normal, seed):
    """The fused faces in front of the furnace: melted ash flesh, drooping into the heat below and behind them."""
    n = len(world)
    lo, hi = world[:, 1].min(), world[:, 1].max()
    height = (world[:, 1] - lo) / max(hi - lo, 1e-6)
    drip = fbm(world * np.array([1.0, 0.2, 1.0]), (0.9, 0.9, 0.9), seed + 61)
    # 0 = cold char at the brow, 1 = molten at the chin: the faces are melting into the furnace.
    heat = (1 - height) * 1.1 + (drip - 0.5) * 1.2 - 0.25
    idx = np.clip((drip * 4.0 - 0.6).astype(int), 0, 3)
    color = CHAR[idx + 1].copy()
    molten = heat > 0.55
    hidx = np.clip(((heat - 0.55) * 7).astype(int), 0, 3)
    color[molten] = FURNACE[hidx[molten] + 1]
    back = normal[:, 2] > 0.5
    color[back] = FURNACE[2]
    return color, molten | back


def paint_flame(world, along, seed):
    n = len(world)
    lick = value_noise(world, (0.7, 0.4, 0.7), seed + 71)
    heat = 1.0 - along + (lick - 0.5) * 0.7
    idx = np.clip((heat * 6.0).astype(int), 0, 5)
    return EMBER[idx].copy(), np.ones(n, dtype=bool)


def paint_smoulder(world, seed):
    n = len(world)
    t = fbm(world, (0.5, 0.5, 0.5), seed + 81)
    idx = np.clip((t * 5.0 - 0.6).astype(int), 0, 4)
    color = EMBER[idx].copy()
    flakes = value_noise(world, (1.2, 1.2, 1.2), seed + 83) > 0.72
    color[flakes] = CHAR[1]
    return color, ~flakes


def paint_victim(world, normal, seed):
    """The stretched, charred figures of the choir in the flight feathers: soot black with glowing cracks."""
    color, glow = paint_bone(world, normal, CHAR, seed + 91, hot_seams=True)
    eyes = value_noise(world, (2.0, 2.0, 2.0), seed + 93) > 0.93
    color[eyes] = EMBER[4]
    return color, glow | eyes


# --- the ember overlay (additive, animated) ---------------------------------------------------------------------------

def embers_frames(kind, world, along, side, extra, frame_seed):
    """Per frame intensity (FRAMES, n) and colour (n, 3) of the additive overlay for one face."""
    n = len(world)
    out = np.zeros((FRAMES, n))
    tint = np.repeat(EMBER[4][None, :], n, axis=0)
    phase = np.arange(FRAMES)[:, None] / FRAMES
    if kind in ("furnace", "furnace_heart"):
        # Heat breathing upward through the furnace; the faces' eyes and mouths flare.
        wave = np.sin((world[None, :, 1] / 14.0 - phase) * 2 * math.pi) * 0.5 + 0.5
        out = 0.25 + wave * 0.45
        if extra is not None:
            out = np.where(extra[None, :] == 2, 0.55 + wave * 0.45, np.where(extra[None, :] == 1, out * 0.25, out))
        tint = np.repeat(FURNACE[3][None, :], n, axis=0)
    elif kind == "choir_hole":
        # The choir sings: each face's eyes and mouth flare in turn.
        voice = hash3(np.floor(world[:, 1] / 9).astype(np.int64), np.floor(world[:, 0] / 9).astype(np.int64), 0, 121)
        out = np.clip(np.sin((phase - voice[None, :]) * 2 * math.pi) * 0.8 + 0.3, 0, 1)
        tint = np.repeat(FURNACE[5][None, :], n, axis=0)
    elif kind in FEATHERS and along is not None:
        # Sparks running out along the smouldering edges toward the tip.
        lane = np.floor(side * 3).astype(np.int64)
        speed = 1 + (hash3(lane, frame_seed, 0, 123) > 0.5)
        pos = np.mod(along[None, :] * 3.0 - phase * speed[None, :] + hash3(lane, frame_seed, 1, 125)[None, :], 1.0)
        spark = (pos < 0.09) & (np.abs(side[None, :]) > 0.6)
        out = spark * 0.9
    elif kind in ("flame", "smoulder"):
        flick = np.stack([value_noise(world + f * 3.1, (0.9, 0.9, 0.9), 127) for f in range(FRAMES)])
        out = np.clip((flick - 0.45) * 2.2, 0, 1)
    return out, tint


# --- atlas ------------------------------------------------------------------------------------------------------------

def pack(faces):
    """Shelf packing, tallest first; returns the atlas height (a multiple of 16)."""
    order = sorted(range(len(faces)), key=lambda i: (-faces[i]["sh"], -faces[i]["sw"]))
    x = y = shelf = 0
    for i in order:
        f = faces[i]
        if x + f["sw"] > ATLAS_W:
            x, y, shelf = 0, y + shelf, 0
        f["sx"], f["sy"] = x, y
        x += f["sw"]
        shelf = max(shelf, f["sh"])
    return int(math.ceil((y + shelf) / 16.0) * 16)


def main():
    model, placed, bones = load_model()
    faces = []
    for element, node, group in placed:
        f, t = element["from"], element["to"]
        e_node = Node(element["name"], element["origin"], element.get("rotation", [0, 0, 0]), node)
        for face in ("north", "east", "south", "west", "up", "down"):
            corner, uax, vax, nrm = face_frame(face, f, t)
            w, h = np.linalg.norm(uax), np.linalg.norm(vax)
            faces.append({"element": element, "group": group, "face": face, "node": e_node,
                          "corner": np.array(corner, float), "u": np.array(uax, float), "v": np.array(vax, float),
                          "normal": np.array(nrm, float), "w": w, "h": h,
                          "sw": max(1, int(math.ceil(w - 1e-6))), "sh": max(1, int(math.ceil(h - 1e-6)))})
    atlas_h = pack(faces)
    base = np.zeros((atlas_h, ATLAS_W, 4), dtype=np.uint8)
    glowmap = np.zeros_like(base)
    embers = np.zeros((atlas_h * FRAMES, ATLAS_W, 4), dtype=np.uint8)

    for index, fc in enumerate(faces):
        el = fc["element"]
        kind = material(el, fc["face"])
        sw, sh = fc["sw"], fc["sh"]
        jj, ii = np.meshgrid(np.arange(sh), np.arange(sw), indexing="ij")
        uu = ((ii + 0.5) / sw).reshape(-1, 1)
        vv = ((jj + 0.5) / sh).reshape(-1, 1)
        local = fc["corner"] + uu * fc["u"] + vv * fc["v"]
        world = fc["node"].to_world(local)
        normal = np.repeat(fc["node"].dir_to_world(fc["normal"][None, :]), len(world), axis=0)
        along, side, length = feather_frame(el, fc["group"], local)
        # One seed per element, so both sides of a feather match.
        seed = int(hash3(int(abs(el["from"][0]) * 7 + abs(el["from"][2]) * 13), int(el["from"][1] * 3), 0, 131) * 9973)
        alpha = np.full(len(world), 255)
        extra = None
        if kind in FEATHERS:
            color, glow, alpha, _ = paint_feather(along, side, length, world, normal, kind == "feather_burnt", seed)
        elif kind == "char":
            color, glow = paint_bone(world, normal, CHAR, 3, hot_seams=True)
        elif kind == "ash":
            color, glow = paint_bone(world, normal, ASH, 5, hot_seams=False)
        elif kind == "horn":
            color, glow = paint_horn(world, normal, along, seed)
        elif kind == "rib":
            color, glow = paint_rib(world, normal, along, seed)
        elif kind in ("furnace", "furnace_heart"):
            color, glow, extra = paint_furnace(world, kind == "furnace_heart", 7)
        elif kind == "choir_face":
            color, glow = paint_choir_face(world, normal, seed)
        elif kind == "choir_hole":
            color, glow = FURNACE[np.full(len(world), 4)].copy(), np.ones(len(world), dtype=bool)
            color[value_noise(world, (1.5, 1.5, 1.5), 141) > 0.6] = FURNACE[5]
        elif kind == "flame":
            color, glow = paint_flame(world, along, seed)
        elif kind == "smoulder":
            color, glow = paint_smoulder(world, seed)
        elif kind == "eye":
            color, glow = np.repeat(WHITE[None, :], len(world), axis=0), np.ones(len(world), dtype=bool)
        elif kind == "socket":
            color, glow = np.repeat(VOID[None, :], len(world), axis=0), np.zeros(len(world), dtype=bool)
        elif kind == "victim":
            color, glow = paint_victim(world, normal, seed)
        else:
            raise ValueError(kind)
        ys, xs = jj.reshape(-1) + fc["sy"], ii.reshape(-1) + fc["sx"]
        base[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        base[ys, xs, 3] = alpha
        glowmap[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        glowmap[ys, xs, 3] = np.where(glow & (alpha > 0), 255, 0)
        intensity, tint = embers_frames(kind, world, along if kind in FEATHERS else None, side, extra, seed)
        for frame in range(FRAMES):
            value = np.clip(intensity[frame][:, None] * tint, 0, 255) * (alpha[:, None] > 0)
            oy = ys + frame * atlas_h
            embers[oy, xs, :3] = value.astype(np.uint8)
            embers[oy, xs, 3] = 255
        fc["uv"] = [fc["sx"], fc["sy"], fc["sx"] + fc["w"], fc["sy"] + fc["h"]]

    for path in (OUT_TEX, OUT_GLOW, OUT_EMBERS, OUT_GEO):
        os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(base, "RGBA").save(OUT_TEX, optimize=True)
    Image.fromarray(glowmap, "RGBA").save(OUT_GLOW, optimize=True)
    Image.fromarray(embers, "RGBA").save(OUT_EMBERS, optimize=True)
    with open(OUT_EMBERS + ".mcmeta", "w", encoding="utf-8", newline="\n") as f:
        # The frames are not square: give their size, or the game rejects the animation.
        json.dump({"animation": {"frametime": 3, "interpolate": True, "width": ATLAS_W, "height": atlas_h}}, f, indent=2)
        f.write("\n")

    write_geo(bones, faces, atlas_h)
    write_bbmodel(model, faces, atlas_h)
    print(f"atlas {ATLAS_W}x{atlas_h}, {len(faces)} faces, embers {FRAMES} frames")


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
        "description": {"identifier": "geometry.the_risen", "texture_width": ATLAS_W, "texture_height": atlas_h,
                        "visible_bounds_width": 34, "visible_bounds_height": 20, "visible_bounds_offset": [0, 10, 0]},
        "bones": out_bones}]}
    with open(OUT_GEO, "w", encoding="utf-8", newline="\n") as f:
        json.dump(geo, f, indent="\t")
        f.write("\n")


def write_bbmodel(model, faces, atlas_h):
    out = copy.deepcopy(model)
    by_uuid = {e["uuid"]: e for e in out["elements"]}
    for fc in faces:
        by_uuid[fc["element"]["uuid"]]["faces"][fc["face"]]["uv"] = rounded(fc["uv"])
    out["name"] = "the_risen"
    out["model_identifier"] = "the_risen"
    out["resolution"] = {"width": ATLAS_W, "height": atlas_h}
    with open(OUT_TEX, "rb") as f:
        data = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")
    tex = out["textures"][0]
    tex.update({"name": "the_risen.png", "path": "", "relative_path": "", "width": ATLAS_W, "height": atlas_h,
                "uv_width": ATLAS_W, "uv_height": atlas_h, "source": data})
    with open(OUT_BB, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    sys.exit(main())
