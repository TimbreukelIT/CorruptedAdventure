"""The Lost Keeper (phase 4, ring 4 Nightmare): model, UV layout and textures.

    python tools/textures/lost_keeper_model.py        (from the repository root)

The approved mockup docs/design/models/nightmare-ring4-de-verloren-hoeder.bbmodel is a posed sculpture (an S-curve), not
a rest pose a game can bend. So the head (with jaw, horns, crown, gill fins and barbels) is taken over from the mockup
as it is, straightened and moved to the origin; the body is rebuilt in the mockup's vocabulary (core, ridge, flank
plates, belly scutes, crest spikes and membrane, glow spots, lesions, exposed ribs, rot strands, vin wings, torn tail
fin) as a straight chain of 12 segment bones behind the head. In game the client lays each segment bone along the
body chain (client/entity/lostkeeper/LostKeeperModel); the numbers below must match entity/lostkeeper/KeeperBody.

Model space (Blockbench): the head looks to -z, y is up, 1 block = 16 units. The head's centre sits at (0, AXIS_Y, 0),
which is the centre of the entity's own hitbox; segment i has its pivot on its front joint (0, AXIS_Y, NECK_Z + i * SEG_LEN).
Segment bones are siblings under root (never nested), so the client can place each one on its own.

Writes:
- assets/corruptedadventure/geo/entity/the_lost_keeper.geo.json         Bedrock geometry 1.12.0 (x mirrored, x/y
                                                                         rotations negated, as Blockbench exports)
- assets/corruptedadventure/textures/entity/the_lost_keeper.png         base: pearl scales, the rot, bone, coral
- assets/corruptedadventure/textures/entity/the_lost_keeper_membrane.png   the see-through fins and crest (translucent pass)
- assets/corruptedadventure/textures/entity/the_lost_keeper_glowmask.png   emissive (GeckoLib AutoGlowingGeoLayer)
- docs/design/models/the_lost_keeper.bbmodel                            the same model for Blockbench (previews, animation)

Every face gets its own place in the atlas (1 texel per model unit) and is painted from where its texels are in the rest
pose, so scales, the rot front and veins run on across cubes. Deterministic: the same input gives the same output.
Palettes: the concept plate docs/design/img/nightmare-ring4-de-verloren-hoeder.png.
"""
import base64
import copy
import json
import math
import os
import sys
import uuid as uuidlib

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "docs/design/models/nightmare-ring4-de-verloren-hoeder.bbmodel")
OUT_BB = os.path.join(ROOT, "docs/design/models/the_lost_keeper.bbmodel")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure")
OUT_GEO = os.path.join(ASSETS, "geo/entity/the_lost_keeper.geo.json")
OUT_TEX = os.path.join(ASSETS, "textures/entity/the_lost_keeper.png")
OUT_MEMBRANE = os.path.join(ASSETS, "textures/entity/the_lost_keeper_membrane.png")
OUT_GLOW = os.path.join(ASSETS, "textures/entity/the_lost_keeper_glowmask.png")

ATLAS_W = 512

# Must match KeeperBody (entity/lostkeeper): head centre height, neck joint, segment length and count, in model units.
AXIS_Y = 20.0
NECK_Z = 25.6
SEG_LEN = 26.0
SEGMENTS = 12
# Width per segment (height is 0.85 of it): thick behind the head, thin at the tail.
WIDTHS = [20, 21, 21, 20, 19, 17.5, 16, 14, 12, 10, 8, 6]
# Where the mockup's head centre was; it moves to (0, AXIS_Y, 0).
MOCKUP_HEAD_CENTRE = np.array([-16.54087, 145.6, -89.5])
CROWN_SCALE = 1.35


def hexes(*values):
    return np.array([[int(v[i:i + 2], 16) for i in (0, 2, 4)] for v in values], dtype=np.float64)


# Sampled from the concept plate's swatches.
PEARL = hexes("8A8496", "A29DAE", "BAB6C4", "CFCCD8", "E2E0EA", "F2F1F7")
PASTEL = hexes("B078AE", "CE96C8", "E6B6DE", "F6D8EE", "86C2FF", "B8E0FF", "E6F6FF")
CORAL = hexes("B0507A", "D06E96", "E892B2", "F6BCD0", "FFE6EE", "FFFFFF")
ROT = hexes("24320A", "34460C", "485E10", "627A14", "82961C", "A6B428")
TOXIC = hexes("4E6424", "6A8430", "86A040", "A2B85A", "BACD7A")
EYE_BLACK = hexes("0C0A0C")[0]
WHITE = np.array([255.0, 255.0, 255.0])
IVORY = hexes("8F8A72", "B8B29A", "D9D4C2", "EEE8D6")
GUM = hexes("3A1426", "5A2438", "7A3450")
SCAR = hexes("4A3A55", "6E5A78")
DEEP = hexes("1A1420", "2A2234")


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


def stable_uuid(name):
    return str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, "corruptedadventure/the_lost_keeper/" + name))


class Group:
    def __init__(self, name, origin, rotation_deg=(0, 0, 0), parent=None):
        self.name, self.origin, self.rotation = name, [float(v) for v in origin], [float(v) for v in rotation_deg]
        self.parent, self.children, self.elements = parent, [], []
        self.uuid = stable_uuid("group/" + name)
        if parent:
            parent.children.append(self)


class Builder:
    def __init__(self):
        self.groups = []
        self.names = set()

    def group(self, name, origin, rotation_deg=(0, 0, 0), parent=None):
        g = Group(name, origin, rotation_deg, parent)
        self.groups.append(g)
        return g

    def cube(self, group, name, frm, to, origin=None, rot=(0, 0, 0), **info):
        base, n = name, 1
        while name in self.names:
            n += 1
            name = f"{base}_{n}"
        self.names.add(name)
        frm, to = [float(v) for v in frm], [float(v) for v in to]
        if origin is None:
            origin = [(a + b) / 2 for a, b in zip(frm, to)]
        el = {"name": name, "from": frm, "to": to, "origin": [float(v) for v in origin],
              "rotation": [float(v) for v in rot], "uuid": stable_uuid("cube/" + name), "info": info}
        group.elements.append(el)
        return el


# --- the head, taken over from the mockup ----------------------------------------------------------------------------

def add_head(b, root):
    with open(SRC, encoding="utf-8") as f:
        mock = json.load(f)
    groups = {g["uuid"]: g for g in mock["groups"]}
    elements = {e["uuid"]: e for e in mock["elements"]}
    head_entry = next(c for c in mock["outliner"] if isinstance(c, dict) and groups[c["uuid"]]["name"] == "head")
    offset = np.array([0.0, AXIS_Y, 0.0]) - MOCKUP_HEAD_CENTRE

    def moved(v):
        return list(np.round(np.array(v, float) + offset, 5))

    # The neck mount carries the look pitch (set by the client each frame); the head under it is free for animations.
    neck = b.group("neck_mount", [0, AXIS_Y, 0], parent=root)

    crown_centre = None

    def walk(entry, parent, in_crown=False):
        nonlocal crown_centre
        g = groups[entry["uuid"]]
        if g["name"] == "crown":
            in_crown = True
            crown_centre = np.array(moved(g["origin"]))

        def place(v):
            # The crown is the weak spot: a third bigger than in the mockup, so it reads (and is hit) from afar.
            q = np.array(moved(v))
            return list(np.round(crown_centre + (q - crown_centre) * CROWN_SCALE, 5)) if in_crown else list(q)

        # The mockup posed the head (tilted and turned) and the jaw (open); the head goes straight, the jaw stays open.
        rot = [0, 0, 0] if g["name"] == "head" else g.get("rotation", [0, 0, 0])
        grp = b.group(g["name"], place(g["origin"]), rot, parent)
        for child in entry.get("children", []):
            if isinstance(child, str):
                e = elements[child]
                mat = head_material(e["name"])
                extra = {"membrane": "gill"} if mat == "membrane" else {}
                frm, to = place(e["from"]), place(e["to"])
                b.cube(grp, e["name"], frm, to, place(e["origin"]), e.get("rotation", [0, 0, 0]), mat=mat, **extra)
            else:
                child_grp = walk(child, grp, in_crown)
                if child_grp.name.startswith("coral_"):
                    add_stump(b, grp, child_grp)
        return grp

    walk(head_entry, neck)


def add_stump(b, crown, coral):
    """What stays of a coral branch once it is broken off: the lower third of its base, with a raw break on top. It sits
    in the crown (not in the branch's bone), so it shows when the client hides the broken branch."""
    base = next(e for e in coral.elements if e["name"].startswith("branch_0"))
    frm, to = base["from"], list(base["to"])
    to[1] = frm[1] + (to[1] - frm[1]) * 0.38
    b.cube(crown, f"coral_stump_{coral.name[-2:]}", frm, to, base["origin"], base["rotation"], mat="coral", stump=True)


def head_material(name):
    prefixes = [
        (("eye_",), "eye"), (("socket_", "nostril_"), "hole"), (("tooth_",), "tooth"),
        (("mouth_roof", "tongue_shadow"), "mouth"), (("horn_",), "horn"), (("branch_", "twig"), "coral"),
        (("glow_tip",), "glowtip"), (("pearl_plate", "pearl_shard"), "nacre"), (("gill_membrane",), "membrane"),
        (("gill_ray",), "ray"), (("barbel_",), "barbel"), (("scar_",), "scar"),
        (("chin_spike", "head_spike", "brow_ridge_spike", "cheek_spike"), "spike"),
    ]
    for keys, mat in prefixes:
        if name.startswith(keys):
            return mat
    return "headscale"


# --- the body --------------------------------------------------------------------------------------------------------

def h01(*keys):
    """A deterministic hash in [0, 1) of a few integers (shape choices, not texels)."""
    h = 2166136261
    for k in keys:
        h = ((h ^ (int(k) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFF) / float(0xFFFFFF)


def seg_rot(i):
    """How rotten segment i (0-based) is, for shape choices: pearl in front, rot behind, a transition in between."""
    return min(1.0, max(0.0, (i - 4.5) / 3.0))


def spike_tip_points(z_base, y_base, s, lean_base, lean_tip):
    a, t = math.radians(lean_base), math.radians(lean_tip)
    top = (z_base + s * 0.62 * math.sin(a), y_base + s * 0.62 * math.cos(a))
    tip = (top[0] + s * 0.5 * math.sin(t), top[1] + s * 0.5 * math.cos(t))
    return top, tip


def add_body(b, root):
    crest_lines = []  # (z_base, y_base, z_tip, y_tip, torn) for the membrane masks
    for i in range(SEGMENTS):
        w = WIDTHS[i]
        h = round(w * 0.85 * 2) / 2
        z0 = NECK_Z + i * SEG_LEN
        z1 = z0 + SEG_LEN
        zc = (z0 + z1) / 2
        r = seg_rot(i)
        y_top = AXIS_Y + h / 2
        y_bot = AXIS_Y - h / 2
        seg = b.group(f"seg_{i + 1:02d}", [0, AXIS_Y, z0], parent=root)
        info = {"seg": i}
        # The core reaches forward into the segment before it, so a bend never opens a gap on the outside.
        overlap = min(7.0, w * 0.35)
        b.cube(seg, f"core_{i}", [-w / 2, y_bot, z0 - overlap], [w / 2, y_top, z1], mat="scale", **info)
        b.cube(seg, f"ridge_{i}", [-w * 0.22, y_top - 0.5, z0 + 1], [w * 0.22, y_top + 2.2, z1 - 1], mat="scale", **info)
        # Flank plates: two staggered per side, slightly tilted, so the flanks read as overlapping armour.
        for side in (-1, 1):
            for k, (za, zb, dy) in enumerate(((z0 + 1, z0 + SEG_LEN * 0.58, 0.0), (z0 + SEG_LEN * 0.42, z1 - 1, -1.2))):
                if r > 0.5 and h01(i, side, k, 1) < 0.35:
                    continue  # fallen off in the rot
                x_in, x_out = side * (w / 2 - 0.8), side * (w / 2 + 1.0 + 0.3 * k)
                # Lying flat in the pearl half, so the scale rows run on unbroken; lifted and twisted in the rot.
                tilt = side * (-7 - 6 * h01(i, side, k, 2)) * r
                twist = side * (3 - 6 * h01(i, side, k, 3)) * r
                b.cube(seg, f"flank_plate_{i}_{side}_{k}", [min(x_in, x_out), AXIS_Y - h * 0.34 + dy, za],
                       [max(x_in, x_out), AXIS_Y + h * 0.36 + dy, zb], rot=(0, twist, tilt), mat="scale", plate=True, **info)
        for k in range(2):
            za = z0 + 1 + k * SEG_LEN * 0.48
            b.cube(seg, f"belly_scute_{i}_{k}", [-w * 0.36, y_bot - 1.4, za], [w * 0.36, y_bot + 0.8, za + SEG_LEN * 0.52],
                   origin=[0, y_bot, za], rot=(-4, 0, 0), mat="scute", **info)

        # Crest: two spikes per segment, leaning back; torn and broken in the rot.
        s_full = 20.0 - i * 1.15
        spikes = 3 if r < 0.5 else 2
        for k in range(spikes):
            zb = z0 + SEG_LEN * ((0.18 + 0.32 * k) if spikes == 3 else (0.28 + 0.5 * k))
            yb = y_top + 1.6
            s = s_full * (0.9 + 0.2 * h01(i, k, 7))
            broken = r > 0.3 and h01(i, k, 8) < 0.25 + 0.4 * r
            if broken:
                s *= 0.45 + 0.25 * h01(i, k, 9)
            lean_b, lean_t = 36 + 8 * h01(i, k, 10), 55 + 10 * h01(i, k, 11)
            bend = (h01(i, k, 12) - 0.5) * (8 + 16 * r)
            thick = 2.8 - i * 0.1
            b.cube(seg, f"crest_spike_{i}_{k}", [-thick / 2, yb - 1, zb - thick / 2], [thick / 2, yb + s * 0.62, zb + thick / 2],
                   origin=[0, yb, zb], rot=(lean_b, 0, bend), mat="spike", **info)
            top, tip = spike_tip_points(zb, yb, s, lean_b, lean_t)
            if not broken:
                tt = max(1.0, thick * 0.55)
                b.cube(seg, f"crest_spike_tip_{i}_{k}", [-tt / 2, top[1], top[0] - tt / 2], [tt / 2, top[1] + s * 0.5, top[0] + tt / 2],
                       origin=[0, top[1], top[0]], rot=(lean_t - lean_b, 0, bend * 1.4), mat="spike", tip=True, **info)
                crest_lines.append((i, zb, yb, tip[0], tip[1]))
            else:
                crest_lines.append((i, zb, yb, top[0], top[1]))
        y_mem = y_top + 1.0
        b.cube(seg, f"crest_membrane_{i}", [-0.2, y_mem, z0 - 3], [0.2, y_mem + s_full * 1.05, z1 + 2],
               mat="membrane", membrane="crest", **info)

        if r < 0.5:
            # Glow spots along the mythical flanks: the pastel glow of the old guardian.
            for side in (-1, 1):
                for k in range(2 if i < 4 else 1):
                    zs = z0 + 5 + k * 11 + 3 * h01(i, side, k, 20)
                    ys = AXIS_Y + h * (0.05 + 0.2 * h01(i, side, k, 21))
                    x = side * (w / 2 + 1.1)
                    b.cube(seg, f"glow_spot_{i}_{side}_{k}", [min(x, x - side * 1.6), ys - 1, zs - 1],
                           [max(x, x - side * 1.6), ys + 1, zs + 1], mat="glowspot", **info)
        if r > 0.0:
            # The rot: open lesions, hanging strands; the deep rot shows its ribs.
            for side in (-1, 1):
                if h01(i, side, 30) < 0.35 + 0.6 * r:
                    zl = z0 + 3 + 14 * h01(i, side, 31)
                    yl = AXIS_Y - h * 0.15 + h * 0.4 * h01(i, side, 32)
                    size = 3.0 + 3.0 * h01(i, side, 33) * (w / 16)
                    x = side * (w / 2 + 0.6)
                    b.cube(seg, f"lesion_{i}_{side}", [min(x, x - side * 2.4), yl - size * 0.4, zl],
                           [max(x, x - side * 2.4), yl + size * 0.4, zl + size], rot=(0, 0, side * 5), mat="lesion", **info)
            for k in range(1 + int(r > 0.6)):
                xs = (h01(i, k, 40) - 0.5) * w * 0.6
                zs = z0 + 4 + 16 * h01(i, k, 41)
                length = 4 + 7 * h01(i, k, 42)
                b.cube(seg, f"rot_strand_{i}_{k}", [xs - 0.5, y_bot - length, zs - 0.5], [xs + 0.5, y_bot + 0.5, zs + 0.5],
                       origin=[xs, y_bot, zs], rot=(8 * (h01(i, k, 43) - 0.5), 0, 14 * (h01(i, k, 44) - 0.5)),
                       mat="strand", **info)
        if i in (7, 9):
            # A gash on one flank (the right one at 7, the left at 9): bare ribs over a dark hollow.
            side = 1 if i == 7 else -1
            x = side * (w / 2 + 0.2)
            b.cube(seg, f"gash_{i}", [min(x, x - side * 1.0), AXIS_Y - h * 0.3, z0 + 4], [max(x, x - side * 1.0), AXIS_Y + h * 0.3, z0 + 20],
                   mat="hole", **info)
            for k in range(3):
                zr = z0 + 6 + k * 5.5
                xr = side * (w / 2 + 0.9)
                b.cube(seg, f"exposed_rib_{i}_{k}", [min(xr, xr - side * 1.4), AXIS_Y - h * 0.38, zr - 0.7],
                       [max(xr, xr - side * 1.4), AXIS_Y + h * 0.38, zr + 0.7], rot=(-10 + 6 * k, 0, side * 6), mat="rib", **info)

        if i == 0:
            add_wings(b, seg, w, h, z0)
        if i == SEGMENTS - 1:
            add_tail_fin(b, seg, z1)
    return crest_lines


WING_ANGLES = [8, 28, 48, 68, 88]
WING_LENGTHS = [40, 46, 44, 38, 30]
WING_TILT = 30


def wing_pivot(side, w, z0):
    return [side * (w / 2 - 1), AXIS_Y - 3, z0 + 9]


def add_wings(b, seg, w, h, z0):
    for side, name in ((-1, "wing_left"), (1, "wing_right")):
        p = wing_pivot(side, w, z0)
        # Rest pose: spread outwards and back, the outer edge tilted down.
        wing = b.group(name, p, (0, 0, -side * WING_TILT), seg)
        b.cube(wing, f"wing_root_plate_{side}", [p[0] - 3, p[1] - 3, p[2] - 4], [p[0] + 3, p[1] + 4, p[2] + 4],
               rot=(20, 0, side * 10), mat="scale", seg=0)
        for k, (ang, length) in enumerate(zip(WING_ANGLES, WING_LENGTHS)):
            yaw = -side * ang  # Ry(-a) turns +x towards +z; mirrored for the left wing
            half = length * 0.45
            if side > 0:
                inner = ([p[0], p[1] - 1.0, p[2] - 1.0], [p[0] + half, p[1] + 1.0, p[2] + 1.0])
                outer = ([p[0] + half, p[1] - 0.6, p[2] - 0.6], [p[0] + length, p[1] + 0.6, p[2] + 0.6])
            else:
                inner = ([p[0] - half, p[1] - 1.0, p[2] - 1.0], [p[0], p[1] + 1.0, p[2] + 1.0])
                outer = ([p[0] - length, p[1] - 0.6, p[2] - 0.6], [p[0] - half, p[1] + 0.6, p[2] + 0.6])
            b.cube(wing, f"wing_ray_{side}_{k}", inner[0], inner[1], origin=p, rot=(0, yaw, 0), mat="ray", seg=0)
            b.cube(wing, f"wing_ray_tip_{side}_{k}", outer[0], outer[1], origin=p, rot=(0, yaw, 0), mat="ray", seg=0)
        reach = max(WING_LENGTHS) + 2
        x0, x1 = (p[0], p[0] + reach) if side > 0 else (p[0] - reach, p[0])
        b.cube(wing, f"wing_membrane_{side}", [x0, p[1] - 0.25, p[2] - 4], [x1, p[1] + 0.25, p[2] + reach],
               mat="membrane", membrane="wing", side=side, pivot=p, seg=0)


TAIL_ANGLES = [-50, -25, 0, 25, 50]
TAIL_LENGTHS = [20, 27, 31, 24, 17]


def add_tail_fin(b, seg, z_end):
    fin = b.group("tail_fin", [0, AXIS_Y, z_end - 2], parent=seg)
    p = [0, AXIS_Y, z_end - 2]
    for k, (ang, length) in enumerate(zip(TAIL_ANGLES, TAIL_LENGTHS)):
        if k == 3:
            length *= 0.55  # snapped off
        b.cube(fin, f"tail_ray_{k}", [-0.6, p[1] - 0.6, p[2]], [0.6, p[1] + 0.6, p[2] + length], origin=p, rot=(-ang, 0, 0),
               mat="ray", seg=SEGMENTS - 1)
    b.cube(fin, "tail_tip", [-1.2, p[1] - 1.2, p[2] - 2], [1.2, p[1] + 1.2, p[2] + 7], mat="spike", seg=SEGMENTS - 1)
    b.cube(fin, "tail_membrane", [-0.2, p[1] - 30, p[2] - 1], [0.2, p[1] + 30, p[2] + 33], mat="membrane", membrane="tail",
           pivot=p, seg=SEGMENTS - 1)


# --- noise ----------------------------------------------------------------------------------------------------------

def hash3(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144665) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFF) / float(0xFFFFFF)


def value_noise(p, scale, seed):
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


def cells(p, cell, seed):
    """Jittered 3D Voronoi: per texel the cell's tone, the gap to the second-nearest cell (0 on a seam) and the offset
    from the cell's centre (in cell units)."""
    q = p / np.asarray(cell)
    base = np.floor(q).astype(np.int64)
    best = np.full(len(p), 9e9)
    second = np.full(len(p), 9e9)
    tone = np.zeros(len(p))
    off = np.zeros((len(p), 3))
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                c = base + np.array([dx, dy, dz])
                jitter = np.stack([hash3(c[:, 0], c[:, 1], c[:, 2], seed + k) for k in range(3)], axis=1) * 0.8 + 0.1
                d_vec = q - (c + jitter)
                d = np.sum(d_vec ** 2, axis=1)
                closer = d < best
                second = np.where(closer, best, np.minimum(second, d))
                tone = np.where(closer, hash3(c[:, 0], c[:, 1], c[:, 2], seed + 7), tone)
                off = np.where(closer[:, None], d_vec, off)
                best = np.minimum(best, d)
    return tone, np.sqrt(second) - np.sqrt(best), off


# --- painting -------------------------------------------------------------------------------------------------------

def pick(palette, idx):
    return palette[np.clip(np.round(idx), 0, len(palette) - 1).astype(int)]


# Scales: rows of overlapping scales like roof tiles, ROW_PX high and SCALE_PX wide, each row shifted half a scale. On the
# flanks the rows stack downwards (the rim at the bottom), on top they run towards the tail. The upper (or front) row lies
# over the next, so every scale shows a rounded rim.
ROW_PX = 4.0
SCALE_PX = 5.0
BULGE = 0.75


def scale_coords(world, normal):
    """Per texel (across, towards the rim) in model units: on a flank (normal mostly x) along the body and downwards; on
    top or underneath (normal mostly y) across and towards the tail; facing the head or tail across and downwards."""
    axis = int(np.argmax(np.abs(normal[0])))
    if axis == 0:
        return world[:, 2], -world[:, 1]
    if axis == 1:
        return world[:, 0], world[:, 2]
    return world[:, 0], -world[:, 1]


def scale_cells(a, b):
    """The scale owning each texel: its row and column, how far down the scale the texel is (0 at its top, 1 at its
    rim) and whether it is on the 1-unit dark rim line."""
    rows = []
    for r in (np.floor(b / ROW_PX), np.floor(b / ROW_PX) - 1):
        off = np.mod(r, 2) * SCALE_PX / 2.0
        c = np.floor((a + off) / SCALE_PX)
        da = (a + off) - (c + 0.5) * SCALE_PX
        bottom = r * ROW_PX + ROW_PX + ROW_PX * BULGE * (1.0 - (2.0 * da / SCALE_PX) ** 2)
        covers = (b >= r * ROW_PX) & (b < bottom)
        depth = (b - r * ROW_PX) / (bottom - r * ROW_PX)
        rows.append((r, c, depth, bottom - b, covers))
    (r0, c0, d0, rim0, _), (r1, c1, d1, rim1, cov1) = rows
    # The row above lies over the one below.
    row = np.where(cov1, r1, r0)
    col = np.where(cov1, c1, c0)
    depth = np.where(cov1, d1, d0)
    rim = np.where(cov1, rim1, rim0) < 1.0
    return row.astype(np.int64), col.astype(np.int64), depth, rim


def infection(z):
    """0 = pearl, 1 = rot, along the body (model z): the rot creeps forward over about two segments."""
    t = np.clip((z - 140.0) / 80.0, 0, 1)
    return t * t * (3 - 2 * t)


def rot_field(world):
    """The infection with blotches: the rot does not keep to a line."""
    blotch = value_noise(world, (0.06, 0.06, 0.06), 109)
    return infection(world[:, 2] + (blotch - 0.5) * 70.0)


def rotten(world):
    """Per texel pearl or rot for parts without scales, mottled in the transition rather than a line."""
    return rot_field(world) > value_noise(world, (0.35, 0.35, 0.35), 113)


def paint_scales(world, normal, local, local_normal):
    """Pearl scales in readable rows, the rot taking them one by one: loose green scales far into the white half, white
    ones left in the rot. Returns colour, glow, membrane-alpha (0)."""
    n = len(world)
    # The rows follow the cube's own (unrotated) faces, so they stay on the texel grid of a tilted plate too.
    a, b = scale_coords(local, local_normal)
    row, col, depth, _ = scale_cells(a, b)

    def owner(shift):
        r, c, _, _ = scale_cells(a, b + shift)
        return r * 100003 + c

    me = row * 100003 + col
    # Outlines from the neighbours one texel towards the rim and towards the top: a whole, unbroken scallop per scale.
    rim = owner(1.0) != me
    shadow = ~rim & (owner(2.0) != me)
    top1 = ~rim & (owner(-1.0) != me)
    top2 = ~rim & ~top1 & (owner(-2.0) != me)
    # Scales on different faces must not share ids.
    face = np.round(normal[:, 0] * 3 + normal[:, 1] * 7 + normal[:, 2] * 11).astype(np.int64)
    h = hash3(row, col, face, 101)
    h2 = hash3(row, col, face, 103)
    h3 = hash3(row, col, face, 107)
    up = normal[:, 1]
    low = np.clip((AXIS_Y - 4 - world[:, 1]) / 10.0, 0, 1)

    p = rot_field(world)
    rot_scale = h < p * 0.93
    tinted = ~rot_scale & (h < p * 0.93 + 0.22 * (4 * p * (1 - p)) + 0.035 * (world[:, 2] > 60))

    # Pearl: a light, pastel nacre sheen along the top of each scale, the body of it pale, a darker band and a dark
    # 1-unit line along its rim.
    tone = 3.3 + (h2 - 0.5) * 0.5 + up * 0.5 - low * 1.0
    pearl = pick(PEARL, tone)
    pearl[shadow] = pick(PEARL, tone[shadow] - 1.0)
    pearl[rim] = pick(PEARL, tone[rim] - 3.0) * 0.8
    hue = np.where(h3 < 0.34, 0, np.where(h3 < 0.67, 1, 2))
    tint = np.stack([PASTEL[3], PASTEL[5], PASTEL[2]])[hue]
    pearl[top1] = PEARL[5] * 0.45 + tint[top1] * 0.55
    pearl[top2] = pick(PEARL, tone[top2] + 1.0) * 0.7 + tint[top2] * 0.3
    rsheen = top1 | top2

    # Green-tinted: an infected pearl scale.
    tinted_c = pearl * 0.5 + pick(TOXIC, 2.0 + rsheen * 1.5) * 0.5
    tinted_c[rim] = ROT[1]

    # Rot: olive scales with a sickly sheen at the top and dark rims; some scales gone (a dark hole), mould on others.
    rtone = 2.6 + (h2 - 0.5) * 0.7 + up * 0.5 - low * 0.8
    rot_c = pick(ROT, rtone)
    rot_c[shadow] = pick(ROT, rtone[shadow] - 1.0)
    rot_c[rsheen] = pick(TOXIC, rtone[rsheen] - 0.6)
    rot_c[rim] = ROT[0] * 0.8
    gone = h3 > 0.93
    rot_c[gone] = ROT[0] * 0.6
    mould = (h3 > 0.8) & (h3 <= 0.93) & ~rim & ~rsheen & (depth > 0.3)
    rot_c[mould] = TOXIC[3]

    color = np.where(rot_scale[:, None], rot_c, np.where(tinted[:, None], tinted_c, pearl))
    # A few rot scales glow poison-green at their top.
    glow = rot_scale & (h2 > 0.96) & rsheen
    color[glow] = TOXIC[4]

    # Veins of the infection over the pearl from halfway the white half on: thin dark-green lines, bright here and there.
    reach = np.clip((world[:, 2] - 85.0) / 90.0, 0, 1)
    vein_field = fbm(world * np.array([1.0, 1.0, 0.3]), (0.14, 0.14, 0.14), 43, octaves=2)
    veins = (np.abs(vein_field - 0.5) < 0.012 + 0.016 * reach) & ~rot_scale & (reach > 0.0) & \
            (value_noise(world, (0.07, 0.07, 0.07), 47) < 0.15 + 0.85 * reach)
    color[veins] = ROT[2]
    hot = veins & (value_noise(world, (0.5, 0.5, 0.5), 49) > 0.75)
    color[hot] = TOXIC[3]
    glow = glow | (hot & (reach > 0.3))
    # Old scars through the pearl.
    scar = (np.abs(fbm(world * np.array([1.0, 0.5, 0.25]), (0.18, 0.18, 0.18), 17, octaves=2) - 0.5) < 0.005) & ~rot_scale
    color[scar] = SCAR[1]
    return color, glow, np.zeros(n)


def membrane_mask(world, info):
    """Where a membrane exists (bool) and how far into the torn rot it is."""
    kind = info["membrane"]
    if kind == "crest":
        return crest_mask(world, info)
    if kind == "wing":
        p = np.array(info["pivot"])
        side = info["side"]
        dx = (world[:, 0] - p[0]) * side
        dz = world[:, 2] - p[2]
        ang = np.degrees(np.arctan2(dz, dx))
        rad = np.hypot(dx, dz)
        edge = np.interp(ang, WING_ANGLES, WING_LENGTHS)
        frac = np.interp(ang, WING_ANGLES, np.arange(len(WING_ANGLES))) % 1.0
        edge = edge * (1 - 0.2 * np.sin(np.pi * frac))
        inside = (ang >= WING_ANGLES[0] - 2) & (ang <= WING_ANGLES[-1] + 2) & (rad < edge) & (rad > 2)
        return inside
    if kind == "tail":
        p = np.array(info["pivot"])
        dz = world[:, 2] - p[2]
        dy = world[:, 1] - p[1]
        ang = np.degrees(np.arctan2(dy, dz))
        rad = np.hypot(dy, dz)
        lengths = list(TAIL_LENGTHS)
        lengths[3] *= 0.55
        edge = np.interp(ang, TAIL_ANGLES, lengths) * (1 - 0.25 * np.sin(np.pi * (np.interp(ang, TAIL_ANGLES, np.arange(5)) % 1.0)))
        ragged = fbm(world, (0.5, 0.5, 0.5), 61) * 7
        return (ang > TAIL_ANGLES[0] - 3) & (ang < TAIL_ANGLES[-1] + 3) & (rad < edge - ragged) & (rad > 1)
    return np.ones(len(world), dtype=bool)


CREST = []


def crest_mask(world, info):
    """The crest membrane hangs behind each spike's line and sags towards the next spike's base."""
    z, y = world[:, 2], world[:, 1]
    edge = np.full(len(world), -1e9)
    base_y = np.full(len(world), 1e9)
    for (i, zb, yb, zt, yt) in CREST:
        if abs(i - info["seg"]) > 1:
            continue
        # Along the spike: the line from base to tip.
        t = np.clip((z - zb) / max(zt - zb, 1e-3), 0, 1)
        along = (z >= zb - 1) & (z <= zt)
        line_y = yb + (yt - yb) * t
        edge = np.where(along, np.maximum(edge, line_y), edge)
        # Behind the tip it sags back down.
        behind = z > zt
        sag = yt - (z - zt) * 1.25
        edge = np.where(behind, np.maximum(edge, sag), edge)
        base_y = np.minimum(base_y, yb - 1.5)
    ragged = (fbm(world, (0.45, 0.45, 0.45), 67) - 0.5) * 3
    return (y < edge - 0.8 + ragged) & (y > base_y - 0.5)


def paint_membrane(world, normal, inside, info):
    """The membrane texture (alpha < 255) for see-through fins; returns rgba (n, 4) and a glow mask."""
    n = len(world)
    rgba = np.zeros((n, 4))
    is_rot = rotten(world) if info.get("seg", 0) > 3 else np.zeros(n, dtype=bool)
    flow = fbm(world, (0.25, 0.25, 0.25), 71)
    mythic = pick(PASTEL, 1 + flow * 2.4)
    rot_c = pick(TOXIC, flow * 3.2)
    color = np.where(is_rot[:, None], rot_c, mythic)
    alpha = np.where(is_rot, 205.0, 150.0) + (flow - 0.5) * 40
    # Fine veins through the membrane, darker.
    vein = np.abs(fbm(world * np.array([0.4, 1.0, 0.4]), (0.6, 0.6, 0.6), 73, octaves=2) - 0.5) < 0.03
    color[vein] = np.where(is_rot[vein, None], ROT[1], PASTEL[0])
    alpha[vein] = 225
    # Torn: holes and slits in the rot, a few nicks in the mythical half.
    tear = value_noise(world, (0.35, 0.35, 0.35), 79) > 0.62
    torn = (is_rot & (tear | (value_noise(world, (1.2, 1.2, 1.2), 81) > 0.8))) | (~is_rot & (value_noise(world, (0.5, 0.5, 0.5), 83) > 0.93))
    show = inside & ~torn
    rgba[:, :3] = color
    rgba[:, 3] = np.where(show, np.clip(alpha, 0, 240), 0)
    glow = show & ~is_rot & (value_noise(world, (0.8, 0.8, 0.8), 85) > 0.82)
    return rgba, glow


def paint_face(fc, world, normal, local):
    el = fc["element"]
    info = el["info"]
    mat = info.get("mat", "scale")
    n = len(world)
    color = np.zeros((n, 3))
    glow = np.zeros(n, dtype=bool)
    tone = fbm(world, (0.4, 0.4, 0.4), 5)

    if mat in ("scale", "headscale"):
        color, glow, _ = paint_scales(world, normal, local, np.repeat(fc["normal"][None, :], len(world), axis=0))
        if info.get("plate"):
            # The rim of a flank plate catches light; the plate's foot sits in shadow.
            rim = np.abs(normal[:, 0]) < 0.5
            color[rim & (normal[:, 1] > 0.5)] = np.minimum(color[rim & (normal[:, 1] > 0.5)] * 1.12, 255)
        return color, glow
    if mat == "scute":
        band = np.mod(world[:, 2], 2.6) < 0.7
        rot = rotten(world)
        c_pearl = pick(PEARL, 3.4 + tone * 1.6 + normal[:, 1] * 0.5)
        c_rot = pick(ROT, 3.0 + tone * 2.2)
        color = np.where(rot[:, None], c_rot, c_pearl)
        color[band] = np.where(rot[band, None], ROT[1], PEARL[1])
        cracks = np.abs(fbm(world, (0.3, 0.3, 0.3), 89) - 0.5) < 0.02
        color[cracks] = DEEP[0]
        return color, glow
    if mat == "spike":
        along = fbm(world * np.array([1.0, 0.25, 1.0]), (0.8, 0.8, 0.8), 91)
        height = np.clip((world[:, 1] - AXIS_Y) / 30.0, 0, 1)
        rot = rotten(world)
        c_pearl = pick(PEARL, 2.0 + along * 2.5 + height * 1.5)
        if info.get("tip"):
            c_pearl = pick(CORAL, 1.5 + along * 2.0 + height * 1.5)
        c_rot = pick(IVORY, along * 2.2) * 0.55 + pick(ROT, 2 + along * 2) * 0.45
        color = np.where(rot[:, None], c_rot, c_pearl)
        notch = value_noise(world, (1.5, 1.5, 1.5), 93) > 0.9
        color[notch] = color[notch] * 0.7
        return color, glow
    if mat == "horn":
        rings = np.mod(world[:, 1] + world[:, 2] * 0.4, 3.0) < 0.8
        color = pick(PEARL, 1.2 + tone * 2.5 + normal[:, 1] * 0.6)
        color[rings] = PEARL[0] * 0.85
        chipped = el["name"].startswith("horn_chipped")
        if chipped:
            color = pick(DEEP, tone * 1.5) * 0.5 + PEARL[0] * 0.5
        return color, glow
    if mat == "tooth":
        color = pick(IVORY, 1.6 + tone * 2.0 + (world[:, 1] - 10) * 0.08)
        return color, glow
    if mat == "mouth":
        color = pick(GUM, tone * 2.6)
        return color, glow
    if mat == "eye":
        color[:] = WHITE
        glow[:] = True
        return color, glow
    if mat == "hole":
        color[:] = EYE_BLACK
        rim = value_noise(world, (1.2, 1.2, 1.2), 95) > 0.75
        color[rim] = DEEP[1]
        return color, glow
    if mat == "coral" and info.get("stump"):
        color = pick(CORAL, tone * 1.5) * 0.7
        if fc["face"] == "up":
            color = pick(IVORY, 1 + tone * 2)  # the raw break
        return color, glow
    if mat == "coral":
        height = np.clip((world[:, 1] - 28) / 22.0, 0, 1)
        color = pick(CORAL, height * 3.2 + tone * 1.2)
        pores = value_noise(world, (2.0, 2.0, 2.0), 97) > 0.85
        color[pores] = CORAL[0] * 0.8
        glow = height > 0.55
        return color, glow
    if mat == "glowtip":
        color = pick(CORAL, 3.5 + tone * 2)
        glow[:] = True
        return color, glow
    if mat == "nacre":
        bands = np.sin(world[:, 1] * 1.3 + world[:, 0] * 0.7 + tone * 4)
        color = np.where((bands > 0.3)[:, None], PASTEL[5], np.where((bands < -0.5)[:, None], PASTEL[3], PEARL[5]))
        glow = bands > -0.2
        return color, glow
    if mat == "ray":
        rot = rotten(world) if info.get("seg", 0) > 3 else np.zeros(n, dtype=bool)
        color = np.where(rot[:, None], pick(ROT, 2 + tone * 2), pick(PASTEL, tone * 1.8))
        return color, glow
    if mat == "barbel":
        tipward = np.clip((AXIS_Y - world[:, 1]) / 12.0, 0, 1)
        color = pick(PEARL, 3.5 - tipward * 2 + tone)
        color[tipward > 0.7] = PASTEL[2]
        return color, glow
    if mat == "glowspot":
        color = pick(PASTEL, 4 + tone * 2.5)
        glow[:] = True
        return color, glow
    if mat == "lesion":
        core = value_noise(world, (0.9, 0.9, 0.9), 99) > 0.45
        color = pick(ROT, tone * 1.5)
        color[core] = pick(TOXIC, 2.5 + tone[core] * 2)
        glow = core & (normal[:, 0] != 0)
        return color, glow
    if mat == "rib":
        color = pick(IVORY, 1.2 + tone * 2.5)
        stain = value_noise(world, (0.8, 0.8, 0.8), 101) > 0.6
        color[stain] = color[stain] * 0.6 + TOXIC[1] * 0.4
        return color, glow
    if mat == "strand":
        color = pick(TOXIC, tone * 2.5)
        drip = world[:, 1] < (AXIS_Y - 10)
        glow = drip & (value_noise(world, (1.5, 1.5, 1.5), 103) > 0.6)
        return color, glow
    if mat == "scar":
        color = pick(SCAR, tone * 1.5)
        return color, glow
    raise ValueError(f"unknown material {mat} for {el['name']}")


# --- per face pipeline (as hollow_model.py) ---------------------------------------------------------------------------

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


def pack(faces):
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


def nodes_of(b):
    """Group name -> Node (rest pose)."""
    nodes = {}

    def make(g):
        if g.name not in nodes:
            parent = make(g.parent) if g.parent else None
            nodes[g.name] = Node(g.name, g.origin, g.rotation, parent)
        return nodes[g.name]

    for g in b.groups:
        make(g)
    return nodes


def build():
    b = Builder()
    root = b.group("root", [0, 0, 0])
    add_head(b, root)
    CREST.clear()
    CREST.extend(add_body(b, root))
    return b


def main():
    b = build()
    nodes = nodes_of(b)
    faces = []
    for g in b.groups:
        for el in g.elements:
            e_node = Node(el["name"], el["origin"], el["rotation"], nodes[g.name])
            for face in ("north", "east", "south", "west", "up", "down"):
                corner, uax, vax, nrm = face_frame(face, el["from"], el["to"])
                w, h = np.linalg.norm(uax), np.linalg.norm(vax)
                faces.append({"element": el, "face": face, "node": e_node, "corner": np.array(corner, float),
                              "u": np.array(uax, float), "v": np.array(vax, float), "normal": np.array(nrm, float),
                              "w": w, "h": h, "sw": max(1, int(math.ceil(w - 1e-6))), "sh": max(1, int(math.ceil(h - 1e-6)))})
    atlas_h = pack(faces)
    base = np.zeros((atlas_h, ATLAS_W, 4), dtype=np.uint8)
    membrane = np.zeros_like(base)
    glowmap = np.zeros_like(base)

    for fc in faces:
        el = fc["element"]
        info = el["info"]
        sw, sh = fc["sw"], fc["sh"]
        jj, ii = np.meshgrid(np.arange(sh), np.arange(sw), indexing="ij")
        uu = ((ii + 0.5) / sw).reshape(-1, 1)
        vv = ((jj + 0.5) / sh).reshape(-1, 1)
        local = fc["corner"] + uu * fc["u"] + vv * fc["v"]
        world = fc["node"].to_world(local)
        normal = np.repeat(fc["node"].dir_to_world(fc["normal"][None, :]), len(world), axis=0)
        ys, xs = jj.reshape(-1) + fc["sy"], ii.reshape(-1) + fc["sx"]
        fc["uv"] = [fc["sx"], fc["sy"], fc["sx"] + fc["w"], fc["sy"] + fc["h"]]
        if info.get("mat") == "membrane":
            # Only the broad faces carry the membrane; its thin edges stay empty. The base pass draws nothing here.
            broad = ("up", "down") if info["membrane"] == "wing" else ("east", "west")
            if fc["face"] in broad:
                if info["membrane"] == "gill":
                    inside = gill_mask(uu.reshape(-1), vv.reshape(-1))
                else:
                    inside = membrane_mask(world, info)
                rgba, glow = paint_membrane(world, normal, inside, info)
                membrane[ys, xs] = np.clip(rgba, 0, 255).astype(np.uint8)
                glowmap[ys, xs, :3] = np.clip(rgba[:, :3], 0, 255).astype(np.uint8)
                glowmap[ys, xs, 3] = np.where(glow, 200, 0)
            continue
        color, glow = paint_face(fc, world, normal, local)
        if info.get("mat") not in ("eye", "glowtip", "glowspot", "scale", "headscale"):
            color, glow = unspeckle(color, glow, sw, sh)
        base[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        base[ys, xs, 3] = 255
        glowmap[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        glowmap[ys, xs, 3] = np.where(glow, 255, 0)

    for path in (OUT_TEX, OUT_MEMBRANE, OUT_GLOW, OUT_GEO):
        os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(base, "RGBA").save(OUT_TEX, optimize=True)
    Image.fromarray(membrane, "RGBA").save(OUT_MEMBRANE, optimize=True)
    Image.fromarray(glowmap, "RGBA").save(OUT_GLOW, optimize=True)
    write_geo(b, faces, atlas_h)
    write_bbmodel(b, faces, atlas_h)
    n_cubes = sum(len(g.elements) for g in b.groups)
    print(f"atlas {ATLAS_W}x{atlas_h}, {n_cubes} cubes, {len(b.groups)} bones, {len(faces)} faces")


def unspeckle(color, glow, w, h, passes=2):
    """Every texel whose colour none of its 4 neighbours shares takes the middle (by brightness) of those neighbours, so
    colours come in clusters of a few texels and never as lone specks (as hollow_model.py)."""
    if w < 2 or h < 2:
        return color, glow
    grid = color.reshape(h, w, 3).copy()
    lit = glow.reshape(h, w).copy()
    for _ in range(passes):
        pad = np.pad(grid, ((1, 1), (1, 1), (0, 0)), mode="edge")
        pad_lit = np.pad(lit, 1, mode="edge")
        shifts = ((0, 1), (0, -1), (1, 0), (-1, 0))
        around = np.stack([pad[1 + dy:1 + dy + h, 1 + dx:1 + dx + w] for dy, dx in shifts])
        around_lit = np.stack([pad_lit[1 + dy:1 + dy + h, 1 + dx:1 + dx + w] for dy, dx in shifts])
        same = np.all(around == grid[None], axis=-1)
        lonely = ~np.any(same, axis=0)
        order = np.argsort(around.sum(axis=-1), axis=0)[2]
        pick_c = np.take_along_axis(around, order[None, :, :, None], axis=0)[0]
        pick_l = np.take_along_axis(around_lit, order[None], axis=0)[0]
        grid = np.where(lonely[..., None], pick_c, grid)
        lit = np.where(lonely, pick_l, lit)
    return grid.reshape(-1, 3), lit.reshape(-1)


def gill_mask(u, v):
    """The gill fins: a rounded fan (face-local), notched between the rays."""
    a = np.arctan2(1 - v, u)
    r = np.hypot(u, 1 - v)
    notch = 0.12 * np.abs(np.sin(a * 3))
    return r < 0.98 - notch


def rounded(values):
    return [round(float(v), 5) for v in values]


def write_geo(b, faces, atlas_h):
    by_element = {}
    for fc in faces:
        by_element.setdefault(fc["element"]["uuid"], {})[fc["face"]] = fc["uv"]
    out_bones = []
    for g in b.groups:
        o = g.origin
        bone = {"name": g.name}
        if g.parent:
            bone["parent"] = g.parent.name
        bone["pivot"] = rounded([-o[0], o[1], o[2]])
        r = g.rotation
        if any(r):
            bone["rotation"] = rounded([-r[0], -r[1], r[2]])
        cubes = []
        for el in g.elements:
            f, t = el["from"], el["to"]
            cube = {"origin": rounded([-t[0], f[1], f[2]]), "size": rounded([t[i] - f[i] for i in range(3)])}
            r = el["rotation"]
            if any(r):
                eo = el["origin"]
                cube["pivot"] = rounded([-eo[0], eo[1], eo[2]])
                cube["rotation"] = rounded([-r[0], -r[1], r[2]])
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
        "description": {"identifier": "geometry.the_lost_keeper", "texture_width": ATLAS_W, "texture_height": atlas_h,
                        "visible_bounds_width": 8, "visible_bounds_height": 6, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": out_bones}]}
    with open(OUT_GEO, "w", encoding="utf-8", newline="\n") as f:
        json.dump(geo, f, indent="\t")
        f.write("\n")


def write_bbmodel(b, faces, atlas_h):
    by_element = {}
    for fc in faces:
        by_element.setdefault(fc["element"]["uuid"], {})[fc["face"]] = fc["uv"]
    elements, groups = [], []
    for g in b.groups:
        groups.append({"name": g.name, "uuid": g.uuid, "export": True, "locked": False, "origin": rounded(g.origin),
                       "rotation": rounded(g.rotation), "color": 0, "children": [], "reset": False, "shade": True,
                       "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": False})
        for el in g.elements:
            elements.append({"name": el["name"], "box_uv": False, "render_order": "default", "locked": False,
                             "export": True, "from": rounded(el["from"]), "to": rounded(el["to"]), "autouv": 0,
                             "color": 0, "rotation": rounded(el["rotation"]), "origin": rounded(el["origin"]),
                             "faces": {face: {"uv": rounded(by_element[el["uuid"]][face]), "texture": 0}
                                       for face in ("north", "east", "south", "west", "up", "down")},
                             "type": "cube", "uuid": el["uuid"]})

    def outline(g):
        return {"uuid": g.uuid, "isOpen": False,
                "children": [el["uuid"] for el in g.elements] + [outline(c) for c in g.children]}

    roots = [g for g in b.groups if g.parent is None]
    with open(OUT_TEX, "rb") as f:
        data = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")
    model = {"meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
             "name": "the_lost_keeper", "model_identifier": "the_lost_keeper", "visible_box": [8, 6, 1.5],
             "bedrock_animation_mode": "entity", "resolution": {"width": ATLAS_W, "height": atlas_h},
             "elements": elements, "groups": groups, "outliner": [outline(g) for g in roots],
             "textures": [{"name": "the_lost_keeper.png", "path": "", "folder": "", "namespace": "", "id": "0",
                           "width": ATLAS_W, "height": atlas_h, "uv_width": ATLAS_W, "uv_height": atlas_h,
                           "particle": False, "render_mode": "default", "render_sides": "auto", "visible": True,
                           "internal": True, "saved": True, "uuid": stable_uuid("texture/base"), "source": data}]}
    with open(OUT_BB, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    sys.exit(main())
