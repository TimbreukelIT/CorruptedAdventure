"""The Unfinished (phase 4, ring 5 Nightmare): model, UV layout and textures from the Blockbench mockup.

    python tools/textures/unfinished_model.py        (from the repository root)

Reads the approved mockup docs/design/models/nightmare-ring5-de-onvoltooide.bbmodel (geometry and bones; its shared
material swatches only tell which material a face is) and writes:

- assets/corruptedadventure/geo/entity/the_unfinished.geo.json      Bedrock geometry 1.12.0 (conversion as in
                                                                     hollow_model.py: x mirrored, x/y rotations negated)
- assets/corruptedadventure/textures/entity/the_unfinished.png       base: a mosaic of all rings sewn with void and glitch
- assets/corruptedadventure/textures/entity/the_unfinished_glowmask.png   emissive (GeckoLib AutoGlowingGeoLayer):
                                                                     Herobrine's eyes, embers, lava, glitch seams, stars
- assets/corruptedadventure/textures/entity/the_unfinished_glitch.png (+ .mcmeta)   the glitch overlay: 8 frames of
                                                                     tearing scan lines and dead pixels in world space
- docs/design/models/the_unfinished.bbmodel                          the same model with the new UVs and the base
                                                                     texture, for Blockbench (previews, animation work)

Changes to the mockup: the ground blocks go (the world is the ground); the void cube becomes a flat void pool round his
base (he drags his void along; the real tear in the ground is step 2); the grafts hang under the torso so they sway with
it; one root bone; four weak spots, one per graft (the Hollow's forearm, a Layered leg, the Risen's wing, the Keeper's
tail), each an "unloaded" variant (wire frame, missing texture, shifted slice) and a "loaded" variant (the real material
with sky glass rims) that the renderer swaps. Every face gets its own place in the atlas and is painted from where its
pixels are in the world. Deterministic: the same input gives the same output.
"""
import base64
import copy
import json
import math
import os
import random
import sys
import uuid as uuidlib

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hollow_model import Node, face_frame, fbm, hash3, plates, value_noise  # noqa: E402  (shared helpers)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "docs/design/models/nightmare-ring5-de-onvoltooide.bbmodel")
OUT_BB = os.path.join(ROOT, "docs/design/models/the_unfinished.bbmodel")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure")
OUT_GEO = os.path.join(ASSETS, "geo/entity/the_unfinished.geo.json")
OUT_TEX = os.path.join(ASSETS, "textures/entity/the_unfinished.png")
OUT_GLOW = os.path.join(ASSETS, "textures/entity/the_unfinished_glowmask.png")
OUT_GLITCH = os.path.join(ASSETS, "textures/entity/the_unfinished_glitch.png")

ATLAS_W = 1024
FRAMES = 4


def hexes(*values):
    return np.array([[int(v[i:i + 2], 16) for i in (0, 2, 4)] for v in values], dtype=np.float64)


# Palettes: the mockup's swatches (fase-4-nightmares-concept.md, plate of De Onvoltooide), darkest first.
P = {
    "stone": hexes("1E1D22", "2E2D33", "3E3D44", "4F4E55", "63626A", "7A7980"),
    "moss": hexes("2E3A1E", "3B4A26", "4F6330", "66803C"),
    "canyon": hexes("5E2A1A", "9B462D", "B55F36", "CC8036", "C7B08C", "D8C39F", "E6D4B2"),
    "ash": hexes("0A0809", "141011", "2E292A", "3B3536", "50403F", "58504F"),
    "ember": hexes("6E1C08", "A82C0C", "DC5414", "FA8828", "FDA742", "FFC25A"),
    "pearl": hexes("5E586C", "868092", "A29DAE", "BCB6C5", "D0CCD9", "E6E5EE"),
    "pearl_glint": hexes("B8E0FF", "F4F2FF"),
    "rot": hexes("0C0A0C", "1A2408", "24320A", "34460C", "485E10", "627A14", "8FA226"),
    "corrupt_stone": hexes("2A2930", "4C4B54", "5D5C65", "68676F", "737279", "7E7D84"),
    "corruption": hexes("0C0911", "140E1A", "2B1F38", "45305C", "6A4A8C", "9C7AC4", "C8B4F0", "E0D1FB"),
    "void": hexes("030204", "07050A", "0C0911", "110D18"),
    "star": hexes("C8B4F0", "E8DCFF", "FFFFFF"),
    "bone": hexes("332B25", "5A5040", "857A62", "AFA284", "D2C7A6", "ECE4CB"),
    "charred": hexes("141011", "1F1B1C", "3A3538", "4B4546", "5D5655", "746C68", "968C84"),
    "scale_grey": hexes("2A2730", "5A566A", "8A8496", "A29DAE", "BAB6C4", "CFCCD8"),
    "coral": hexes("6E2A48", "B0507A", "D06E96", "E892B2", "F6BCD0", "FFE6EE"),
    "sky": hexes("4C9CC4", "7FC8E8", "B8E6FA", "E8F8FF"),
}
GLITCH = hexes("F800F8")[0]
CYAN = hexes("00E8F0")[0]
WHITE = np.array([255.0, 255.0, 255.0])
BLACK = np.zeros(3)

# The mockup's swatches (8 x 3 grid of 64 px) -> material.
SWATCH = {0: "stone", 1: "canyon", 2: "ash", 3: "pearl", 4: "rot", 5: "corrupt_stone", 6: "void", 7: "void_stars",
          8: "corruption", 9: "wire", 10: "missing", 11: "eye", 12: "flow", 13: "holle_stone", 14: "bone_dirty",
          15: "feather", 16: "lava", 17: "charred", 18: "basalt", 19: "scale_grey", 20: "coral", 21: "hole",
          22: "glitch_cyan", 23: "bone"}
# Materials that are flat enough to get a quarter of the texels (mostly hidden or near-uniform).
LOW_DENSITY = {"void", "hole", "eye", "glitch_cyan"}

ROOT_UUID = "6a1d0c55-0e4b-4f1e-9a5f-52f1a0c0d001"


def uid(name):
    return str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, "corruptedadventure:the_unfinished/" + name))


# --- the restructured model ------------------------------------------------------------------------------------------
# Preview 1 (2026-10-28, user): keep the wire-frame/missing-texture weak spots; the torso must be a colossus (hunched,
# broad chest and shoulders, narrow waist, large uneven grafted chunks instead of a grid), the crown of four heads much
# bigger and readable at a glance as his face, and the Keeper's tail curling round his body. So the torso, the crown and
# the tail are built here; the grafted arm, legs and wing, the rift and the debris come from the mockup.

KEPT_GRAFTS = ("graft_holle_arm", "graft_gelaagde_leg_1", "graft_gelaagde_leg_2", "graft_gelaagde_leg_3",
               "graft_herrijzende_wing")

# The weak spots (bb units): parent bone, centre, size, the material of the loaded variant. Positions on the rest pose:
# along the forearm (0.6), the front shin (0.8), the wing's elbow (above the shoulder); the tail's on the coil behind
# him (TAIL_WEAK_T).
WEAK_SPOTS = {
    "weak_holle": ("graft_holle_arm_lower", (113.0, 67.0, -49.0), (30.0, 30.0, 18.0), "flow"),
    "weak_gelaagde": ("graft_gelaagde_leg_1_lower", (-160.0, 54.0, -47.6), (18.0, 30.0, 18.0), "bone"),
    "weak_herrijzende": ("graft_herrijzende_wing", (121.0, 267.0, 6.0), (22.0, 26.0, 22.0), "charred"),
}
TAIL_WEAK_T = 0.75

# Torso regions: bone, name, centre, half size. Waist narrow, chest and shoulders broad, the back rising into a hump;
# the bones carry the hunch (chest tipped forward, shoulders sloping down outward, hump rising behind).
REGIONS = [
    ("torso", "waist", (0, 16, 6), (28, 42, 24)),
    ("torso", "belly", (0, 80, -4), (40, 30, 32)),
    ("torso", "hip_l", (-50, 104, -2), (16, 38, 28)),
    ("chest", "chest", (0, 150, -22), (70, 46, 46)),
    ("chest", "pec_r", (38, 160, -62), (34, 30, 24)),
    ("chest", "pec_l", (-38, 156, -60), (32, 28, 24)),
    ("chest", "neck", (0, 196, -64), (30, 24, 30)),
    ("shoulder_r", "shoulder_r", (92, 212, -8), (44, 36, 44)),
    ("shoulder_l", "shoulder_l", (-90, 208, -6), (40, 34, 42)),
    ("hump", "hump", (0, 226, 30), (60, 32, 40)),
]
REGION_BONES = (("chest", (0, 110, -10), (-20, 0, 0)), ("shoulder_r", (56, 196, -8), (0, 0, -24)),
                ("shoulder_l", (-54, 192, -6), (0, 0, 24)), ("hump", (0, 200, 14), (25, 0, 0)))
# Spines along the hump: (x, y, z) base on the hump's top, length, tilt.
HUMP_SPINES = [((-40, 252, 30), 26, (20, 0, 18)), ((-14, 258, 24), 40, (24, 0, 6)), ((10, 258, 26), 34, (22, 0, -8)),
               ((34, 252, 30), 22, (18, 0, -20)), ((0, 250, 52), 28, (40, 0, 0))]
CHUNK_MATERIALS = [("stone", 3.0), ("corrupt_stone", 3.0), ("canyon", 2.0), ("ash", 2.0), ("pearl", 1.5),
                   ("rot", 1.0), ("corruption", 1.0), ("basalt", 1.0), ("bone_dirty", 0.5)]
CROWN = (0.0, 214.0, -96.0)
# The crown is the boss's face: heads and core are built at these scales round their centres.
HEAD_SCALE = 1.6
CORE_SCALE = 1.35


def face_set(mat):
    return {f: {"uv": [0, 0, 1, 1], "texture": 0, "_mat": mat} for f in ("north", "east", "south", "west", "up", "down")}


def cube(name, frm, to, mat, rotation=(0, 0, 0), origin=None, inflate=0.0):
    el = {"name": name, "box_uv": False, "render_order": "default", "locked": False, "export": True, "scope": 0,
          "allow_mirror_modeling": True, "from": [float(v) for v in frm], "to": [float(v) for v in to], "autouv": 0,
          "color": 0, "origin": [float(v) for v in origin] if origin is not None else [(a + b) / 2 for a, b in zip(frm, to)],
          "faces": face_set(mat), "type": "cube", "uuid": uid(name), "_mat": mat}
    if any(rotation):
        el["rotation"] = [float(v) for v in rotation]
    if inflate:
        el["inflate"] = inflate
    return el


def box(name, centre, size, mat, rotation=(0, 0, 0), origin=None):
    c, s = np.array(centre, float), np.array(size, float)
    return cube(name, c - s / 2, c + s / 2, mat, rotation, c if origin is None else origin)


def rod(name, base, size, length, mat, rotation):
    """A rod from base straight up (before its rotation round the base): spikes, horns, crest feathers."""
    b = np.array(base, float)
    return cube(name, b - np.array([size / 2, 0, size / 2]), b + np.array([size / 2, length, size / 2]), mat, rotation, b)


def group(template, name, origin, rotation=(0, 0, 0)):
    g = copy.deepcopy(template)
    g.update({"name": name, "uuid": uid("group/" + name), "origin": [float(v) for v in origin],
              "rotation": [float(v) for v in rotation]})
    return g


def weak_spot_elements(name, centre, size, mat):
    """The two variants of a weak spot round its centre: unloaded (a wire frame shell, a missing-texture core and a slice
    shifted out of place) and loaded (a solid chunk of the graft's material; its rims glow sky glass blue)."""
    c, s = np.array(centre), np.array(size)
    lo, hi = c - s / 2, c + s / 2
    core_lo, core_hi = c - s * 0.32, c + s * 0.32
    slice_lo = np.array([lo[0] - 3, c[1] + s[1] * 0.12, lo[2] - 2])
    slice_hi = np.array([hi[0] - 6, c[1] + s[1] * 0.3, hi[2] - 4])
    unloaded = [
        cube(name + "_wire", lo, hi, "wire"),
        cube(name + "_missing", core_lo, core_hi, "missing"),
        cube(name + "_slice", slice_lo, slice_hi, "glitch_slice"),
    ]
    loaded = [
        cube(name + "_chunk", lo + 1, hi - 1, "loaded_" + mat),
        cube(name + "_shard", c - s * 0.2 + np.array([0, s[1] * 0.35, 0]), c + s * 0.2 + np.array([0, s[1] * 0.55, 0]),
             "loaded_" + mat, rotation=(12, 30, -8)),
    ]
    return unloaded, loaded


# --- the torso: big uneven grafted chunks over a void core -------------------------------------------------------------

def pick(rng, weighted):
    total = sum(w for _, w in weighted)
    roll = rng.uniform(0, total)
    for value, weight in weighted:
        roll -= weight
        if roll <= 0:
            return value
    return weighted[-1][0]


def split(rng, u0, u1, v0, v1, smallest, largest):
    """Guillotine cuts of a face into uneven pieces between smallest and largest (bb units)."""
    w, h = u1 - u0, v1 - v0
    if max(w, h) <= largest and (max(w, h) < smallest * 2 or rng.random() < 0.45):
        return [(u0, u1, v0, v1)]
    if w >= h:
        cut = u0 + w * rng.uniform(0.32, 0.68)
        return split(rng, u0, cut, v0, v1, smallest, largest) + split(rng, cut, u1, v0, v1, smallest, largest)
    cut = v0 + h * rng.uniform(0.32, 0.68)
    return split(rng, u0, u1, v0, cut, smallest, largest) + split(rng, u0, u1, cut, v1, smallest, largest)


def region_chunks(name, centre, half, rng):
    """A void core and, on its five outer faces (not the bottom), plates of every ring with void seams between them:
    each plate its own size, thickness, protrusion and tilt."""
    c, h = np.array(centre, float), np.array(half, float)
    out = [box(name + "_core", c, h * 2 * 0.94, "void")]
    # face: (normal axis, sign, u axis, v axis)
    faces = {"north": (2, -1, 0, 1), "south": (2, 1, 0, 1), "east": (0, 1, 2, 1), "west": (0, -1, 2, 1), "up": (1, 1, 0, 2)}
    i = 0
    for face, (n, sign, ua, va) in faces.items():
        for u0, u1, v0, v1 in split(rng, -h[ua], h[ua], -h[va], h[va], 24.0, 80.0):
            gap = rng.uniform(1.0, 3.0)
            thick = rng.uniform(10.0, 20.0)
            out_by = rng.uniform(1.0, 9.0) + (8.0 if rng.random() < 0.2 else 0.0)
            lo, hi = np.zeros(3), np.zeros(3)
            lo[ua], hi[ua] = u0 + gap, u1 - gap
            lo[va], hi[va] = v0 + gap, v1 - gap
            plane = sign * h[n]
            lo[n], hi[n] = sorted((plane - sign * (thick - out_by), plane + sign * out_by))
            rot = [rng.uniform(-9, 9), rng.uniform(-9, 9), rng.uniform(-9, 9)]
            out.append(cube(f"slab_{name}_{i}", c + lo, c + hi, pick(rng, CHUNK_MATERIALS), rot))
            i += 1
    return out


# --- the crown of four heads ---------------------------------------------------------------------------------------

def at(centre, offset):
    return np.array(centre, float) + np.array(offset, float)


def head_holle(h):
    """The Hollow's stone skull with its broken crown; soul light deep in the sockets."""
    e = [box("holle_cranium", at(h, (0, 4, 4)), (40, 34, 38), "holle_stone"),
         box("holle_brow", at(h, (0, 12, -17)), (46, 9, 10), "holle_stone", (0, 0, 4)),
         box("holle_cheek_l", at(h, (-15, -6, -14)), (8, 10, 16), "holle_stone"),
         box("holle_cheek_r", at(h, (15, -6, -14)), (8, 10, 16), "holle_stone"),
         box("holle_socket_l", at(h, (-10, 4, -15.5)), (11, 9, 2), "hole"),
         box("holle_socket_r", at(h, (10, 4, -15.5)), (11, 9, 2), "hole"),
         box("holle_soul_l", at(h, (-10, 3, -16.7)), (3, 2, 1), "glitch_cyan"),
         box("holle_soul_r", at(h, (10, 3, -16.7)), (3, 2, 1), "glitch_cyan"),
         box("holle_nose", at(h, (0, -3, -16)), (6, 7, 2), "hole"),
         box("holle_tear", at(h, (-11, -7, -16.4)), (3, 12, 1), "flow"),
         box("holle_jaw", at(h, (0, -17, -4)), (34, 10, 30), "holle_stone", (10, 0, 0), at(h, (0, -12, 10))),
         box("holle_band", at(h, (0, 22, 4)), (44, 6, 42), "stone")]
    for i, x in enumerate((-12, -4, 4, 12)):
        e.append(box(f"holle_tooth_{i}", at(h, (x, -12, -17)), (4, 6, 3), "bone"))
    spikes = [((-18, -14), 26, 0), ((0, -18), 18, 28), ((18, -14), 32, 0), ((20, 10), 12, -30), ((-20, 10), 22, 8),
              ((0, 22), 16, -20)]
    for i, ((x, z), length, tilt) in enumerate(spikes):
        e.append(rod(f"holle_crown_spike_{i}", at(h, (x, 24, z + 4)), 6, length, "holle_stone", (0, 0, tilt)))
    return e


def head_gelaagde(h):
    """The Layered's long beaked bird skull: bleached bone, dark sockets, a split beak hanging open."""
    return [box("gelaagde_cranium", at(h, (0, 6, 8)), (32, 28, 34), "bone"),
            box("gelaagde_brow_l", at(h, (-9, 14, -9)), (14, 6, 8), "bone_dirty", (0, 0, 8)),
            box("gelaagde_brow_r", at(h, (9, 14, -9)), (14, 6, 8), "bone_dirty", (0, 0, -8)),
            box("gelaagde_socket_l", at(h, (-9, 6, -9.6)), (11, 11, 2), "hole"),
            box("gelaagde_socket_r", at(h, (9, 6, -9.6)), (11, 11, 2), "hole"),
            box("gelaagde_beak_0", at(h, (0, 0, -20)), (16, 14, 22), "bone"),
            box("gelaagde_beak_1", at(h, (0, -3, -38)), (13, 11, 18), "bone"),
            box("gelaagde_beak_2", at(h, (0, -7, -52)), (9, 8, 14), "bone_dirty"),
            box("gelaagde_beak_tip", at(h, (0, -11, -60)), (6, 7, 6), "bone_dirty"),
            box("gelaagde_beak_split", at(h, (0, -6, -30)), (16.5, 1.5, 30), "hole"),
            box("gelaagde_nasal", at(h, (0, 4, -21.4)), (5, 4, 2), "hole"),
            box("gelaagde_lower_beak", at(h, (0, -12, -26)), (12, 6, 34), "bone", (12, 0, 0), at(h, (0, -10, -8)))]


def head_herrijzende(h):
    """The Risen's charred hook-beaked skull with embers in its sockets and a fan of burning crest feathers."""
    e = [box("herrijzende_cranium", at(h, (0, 4, 6)), (34, 30, 36), "charred"),
         box("herrijzende_brow", at(h, (0, 12, -14)), (38, 7, 10), "basalt", (0, 0, -5)),
         box("herrijzende_socket_l", at(h, (-9, 4, -12.6)), (10, 8, 2), "hole"),
         box("herrijzende_socket_r", at(h, (9, 4, -12.6)), (10, 8, 2), "hole"),
         box("herrijzende_ember_l", at(h, (-9, 3, -13.8)), (4, 3, 1), "lava"),
         box("herrijzende_ember_r", at(h, (9, 3, -13.8)), (4, 3, 1), "lava"),
         box("herrijzende_beak", at(h, (0, 0, -24)), (14, 14, 24), "basalt"),
         box("herrijzende_hook", at(h, (0, -12, -34)), (11, 22, 9), "basalt", (-20, 0, 0)),
         box("herrijzende_hook_tip", at(h, (0, -24, -31)), (7, 8, 7), "charred"),
         box("herrijzende_lower_beak", at(h, (0, -12, -20)), (12, 6, 20), "charred", (15, 0, 0), at(h, (0, -10, -10)))]
    for i, (fan, length) in enumerate(zip((-54, -36, -18, 0, 18, 36, 54), (44, 58, 70, 76, 66, 54, 40))):
        e.append(rod(f"herrijzende_crest_{i}", at(h, (fan * 0.18, 16, 12)), 6, length, "lava" if i % 2 else "feather",
                     (-38, 0, fan)))
    return e


def head_hoeder(h):
    """The Lost Keeper's dragon head: pearl scales going to rot at the jaw, swept horns, a coral crown, jaws open."""
    jaw_pivot = at(h, (0, -10, 0))
    e = [box("hoeder_cranium", at(h, (0, 6, 10)), (38, 30, 36), "pearl"),
         box("hoeder_snout", at(h, (0, 2, -20)), (30, 18, 30), "pearl"),
         box("hoeder_snout_tip", at(h, (0, -1, -40)), (24, 14, 14), "pearl"),
         box("hoeder_brow", at(h, (0, 14, -6)), (40, 8, 12), "scale_grey"),
         box("hoeder_socket_l", at(h, (-12, 8, -12.6)), (10, 7, 2), "hole"),
         box("hoeder_socket_r", at(h, (12, 8, -12.6)), (10, 7, 2), "hole"),
         box("hoeder_nostril_l", at(h, (-6, 4, -47.2)), (4, 3, 2), "hole"),
         box("hoeder_nostril_r", at(h, (6, 4, -47.2)), (4, 3, 2), "hole"),
         box("hoeder_mouth", at(h, (0, -9, -24)), (22, 6, 36), "hole"),
         box("hoeder_jaw", at(h, (0, -18, -24)), (26, 9, 40), "rot", (28, 0, 0), jaw_pivot),
         box("hoeder_fin_l", at(h, (-21, 2, 6)), (2, 22, 24), "pearl", (0, -25, 0)),
         box("hoeder_fin_r", at(h, (21, 2, 6)), (2, 22, 24), "pearl", (0, 25, 0))]
    for i, z in enumerate((-44, -36, -28, -20)):
        for side in (-1, 1):
            e.append(box(f"hoeder_tooth_{i}_{side}", at(h, (side * 9, -10, z)), (3, 7, 3), "bone"))
            e.append(box(f"hoeder_low_tooth_{i}_{side}", at(h, (side * 8, -12, z + 2)), (3, 6, 3), "bone", (28, 0, 0),
                         jaw_pivot))
    for side in (-1, 1):
        e.append(rod(f"hoeder_horn_{side}", at(h, (side * 14, 16, 12)), 8, 46, "basalt", (-62, side * 14, 0)))
        e.append(rod(f"hoeder_horn_tip_{side}", at(h, (side * 18, 40, 46)), 5, 18, "basalt", (-80, side * 14, 0)))
    for i, (x, length, tilt) in enumerate(((-8, 18, 20), (-3, 26, 6), (3, 22, -8), (8, 16, -22))):
        e.append(rod(f"hoeder_coral_{i}", at(h, (x, 20, 4)), 5, length, "coral", (-10, 0, tilt)))
    return e


def crown_core(c):
    """The void core in the middle of the four heads, Herobrine's white eyes in it, a broken glitch ring round it."""
    e = [box("void_core", c, (46, 42, 34), "void_stars"),
         box("void_core_lobe_0", at(c, (-20, 10, 4)), (20, 22, 24), "void_stars"),
         box("void_core_lobe_1", at(c, (20, -8, 4)), (22, 20, 24), "void_stars"),
         box("void_core_lobe_2", at(c, (0, -18, 6)), (28, 14, 26), "void_stars"),
         box("eye_left", at(c, (-9, 2, -17.6)), (10, 3, 1), "eye"),
         box("eye_right", at(c, (9, 2, -17.6)), (10, 3, 1), "eye")]
    for i in range(10):
        if i in (3, 7):
            continue  # the ring is broken
        angle = i / 10 * 2 * math.pi + 0.3
        x, y = math.cos(angle) * 27, math.sin(angle) * 25
        e.append(box(f"glitch_ring_{i}", at(c, (x, y, -16)), (9, 2, 2), "glitch_cyan" if i % 2 else "missing",
                     (0, 0, math.degrees(angle) + 90)))
    return e


HEADS = {
    # name: (centre, bone rotation, builder): the Hollow upper right on its arm's side, the Risen upper left with its
    # crest high, the Layered and the Keeper lower down; all round the core, facing forward.
    "head_holle": ((54, 252, -96), (8, -12, -14), head_holle),
    "head_herrijzende": ((-50, 258, -92), (0, 15, 8), head_herrijzende),
    "head_gelaagde": ((58, 170, -104), (18, -14, 6), head_gelaagde),
    "head_hoeder": ((-60, 166, -100), (-6, 16, 10), head_hoeder),
}


def scaled(elements, centre, k):
    """The elements scaled by k round centre (sizes, places and pivots; rotations stay)."""
    c = np.array(centre, float)
    for el in elements:
        for key in ("from", "to", "origin"):
            el[key] = [float(v) for v in c + (np.array(el[key], float) - c) * k]
    return elements


# --- the Keeper's tail: a coil round his waist and belly -------------------------------------------------------------

def tail_point(t):
    """Centre of the coil at t in [0, 1]: from the void behind him (below the ground) up round his belly, 1.3 turns."""
    angle = math.radians(70) + t * 1.3 * 2 * math.pi
    radius = 40 + t * 26
    return np.array([math.cos(angle) * radius, -30 + t * 150, math.sin(angle) * radius])


def tail():
    """Segments along the coil, each turned along it, thick at the root and thin at the tip; spines on the outside."""
    e = []
    steps = 22
    for i in range(steps):
        t0, t1 = i / steps, (i + 1) / steps
        a, b = tail_point(t0), tail_point(t1)
        mid, d = (a + b) / 2, b - a
        length = float(np.linalg.norm(d)) + 4
        yaw = math.degrees(math.atan2(d[0], d[2]))
        pitch = -math.degrees(math.asin(d[1] / np.linalg.norm(d)))
        thick = 26 - 16 * t0
        mat = "rot" if t0 < 0.8 else "pearl"
        e.append(box(f"tail_{i}", mid, (thick, thick * 0.85, length), mat, (pitch, yaw, 0)))
        if i % 2 == 0 and i > 1:
            out = np.array([mid[0], 0, mid[2]])
            out = out / max(np.linalg.norm(out), 1e-9)
            spine = mid + out * thick * 0.4 + np.array([0, thick * 0.3, 0])
            e.append(rod(f"tail_spike_{i}", spine, 4, 10 + 8 * (1 - t0), "scale_grey" if i % 4 else "bone_dirty",
                         (0, yaw, -25)))
    return e


def tail_weak_centre():
    p = tail_point(TAIL_WEAK_T)
    out = np.array([p[0], 0, p[2]])
    return p + out / max(np.linalg.norm(out), 1e-9) * 10


# --- the unloaded bits on the torso ----------------------------------------------------------------------------------

def unloaded_bits():
    """The decorative flicker: a wire-frame patch on the chest, a missing-texture shard on the belly, three shifted slices."""
    return {
        "unloaded_wire_chest": [box("wire_patch", (34, 150, -66), (34, 40, 8), "wire"),
                                box("wire_patch_2", (-50, 96, -20), (6, 36, 30), "wire")],
        "unloaded_missing_shard": [box("missing_block", (-20, 70, -40), (12, 12, 8), "missing", (-4, -4, 14))],
        "unloaded_slice_1": [box(f"slice_1_{i}", (-36 + i * 36, 128, -66), (34, 5, 12), "glitch_slice:" + m)
                             for i, m in enumerate(("canyon", "corrupt_stone", "ash"))],
        "unloaded_slice_2": [box(f"slice_2_{i}", (-40 + i * 40, 184, -62), (38, 5, 12), "glitch_slice:" + m)
                             for i, m in enumerate(("pearl", "stone", "rot"))],
        "unloaded_slice_3": [box(f"slice_3_{i}", (-14 + i * 28, 102, -42), (26, 4, 10), "glitch_slice:" + m)
                             for i, m in enumerate(("void_stars", "charred"))],
    }


def restructure(model):
    groups = {g["uuid"]: g for g in model["groups"]}
    elements = {e["uuid"]: e for e in model["elements"]}
    template = next(g for g in model["groups"] if g["name"] == "torso")

    def swatch(el):
        counts = {}
        for face in el["faces"].values():
            if face.get("texture") is None:
                continue
            u, v = face["uv"][0], face["uv"][1]
            idx = int(v // 64) * 8 + int(u // 64)
            counts[idx] = counts.get(idx, 0) + 1
        return SWATCH[max(counts, key=counts.get)]

    for el in model["elements"]:
        el["_mat"] = swatch(el)

    outline = {entry["uuid"]: entry for entry in model["outliner"] if isinstance(entry, dict)}
    top = {groups[u]["name"]: entry for u, entry in outline.items()}

    def new_bone(name, origin, rotation=(0, 0, 0), members=()):
        g = group(template, name, origin, rotation)
        model["groups"].append(g)
        groups[g["uuid"]] = g
        model["elements"].extend(members)
        return {"uuid": g["uuid"], "isOpen": False, "children": [e["uuid"] for e in members]}

    # The rift: drop the ground blocks, flatten the void into a pool on the ground; keep the jagged rims.
    rift = top["void_rift"]
    keep, debris = [], []
    for child in rift["children"]:
        el = elements[child]
        if el["name"].startswith("ground_"):
            continue
        if el["name"] == "void":
            el["from"] = [-85.1, -1.0, -62.1]
            el["to"] = [85.1, 0.35, 62.1]
            el["origin"] = [0, 0, 0]
        if el["name"].startswith("floating_debris"):
            debris.append(child)
            continue
        keep.append(child)
    rift["children"] = keep
    groups[rift["uuid"]]["name"] = "rift"
    debris_entry = new_bone("debris", (0, 30, 0))
    debris_entry["children"] = debris

    rng = random.Random("the_unfinished/torso")
    torso = new_bone("torso", (0, 20, 0))
    upper = new_bone("torso_upper", (0, 118, 0))
    torso["children"].append(upper)
    sub = {"torso": torso}
    for bone, pivot, rotation in REGION_BONES:
        sub[bone] = new_bone(bone, pivot, rotation)
        upper["children"].append(sub[bone])
    for bone, name, centre, half in REGIONS:
        chunks = region_chunks(name, centre, half, rng)
        model["elements"].extend(chunks)
        sub[bone]["children"].extend(e["uuid"] for e in chunks)

    spines = [rod(f"hump_spine_{i}", base, 7, length, "bone_dirty" if i % 2 else "basalt", tilt)
              for i, (base, length, tilt) in enumerate(HUMP_SPINES)]
    model["elements"].extend(spines)
    sub["hump"]["children"].extend(e["uuid"] for e in spines)

    crown = new_bone("crown_core", CROWN, (0, 0, 0), scaled(crown_core(CROWN), CROWN, CORE_SCALE))
    upper["children"].append(crown)
    for name, (centre, rotation, build) in HEADS.items():
        crown["children"].append(new_bone(name, centre, rotation, scaled(build(centre), centre, HEAD_SCALE)))

    for name in KEPT_GRAFTS:
        upper["children"].append(top[name])
    torso["children"].append(new_bone("graft_hoeder_tail", (0, 0, 0), (0, 0, 0), tail()))
    for name, bits in unloaded_bits().items():
        upper["children"].append(new_bone(name, np.mean([np.array(e["origin"]) for e in bits], axis=0), (0, 0, 0), bits))

    def find(entry, name):
        if groups[entry["uuid"]]["name"] == name:
            return entry
        for child in entry.get("children", []):
            if isinstance(child, dict):
                hit = find(child, name)
                if hit:
                    return hit
        return None

    find(torso, "graft_gelaagde_leg_1")["children"].append(top["unloaded_wire_leg"])

    spots = dict(WEAK_SPOTS)
    spots["weak_hoeder"] = ("graft_hoeder_tail", tuple(tail_weak_centre()), (26.0, 28.0, 24.0), "rot")
    for name, (parent, centre, size, mat) in spots.items():
        unloaded, loaded = weak_spot_elements(name, centre, size, mat)
        entry = new_bone(name, centre)
        entry["children"] = [new_bone(name + "_unloaded", centre, (0, 0, 0), unloaded),
                             new_bone(name + "_loaded", centre, (0, 0, 0), loaded)]
        find(torso, parent)["children"].append(entry)
        c = np.array(centre) / 16
        print(f"{name}: left {c[0]:.2f}, centre up {c[1]:.2f}, forward {-c[2]:.2f} blocks")

    root = group(template, "root", [0, 0, 0])
    root["uuid"] = ROOT_UUID
    model["groups"].insert(0, root)
    model["outliner"] = [{"uuid": ROOT_UUID, "isOpen": True, "children": [rift, torso, debris_entry]}]
    return model


def walk_model(model):
    groups = {g["uuid"]: g for g in model["groups"]}
    elements = {e["uuid"]: e for e in model["elements"]}
    placed, bones = [], []

    def walk(children, parent_node):
        for child in children:
            if isinstance(child, str):
                placed.append((elements[child], parent_node))
                bones_by_node[id(parent_node)][2].append(elements[child])
                continue
            g = groups[child["uuid"]]
            node = Node(g["name"], g["origin"], g.get("rotation", [0, 0, 0]), parent_node)
            bone = (g, parent_node.name if parent_node else None, [], node)
            bones.append(bone)
            bones_by_node[id(node)] = bone
            walk(child.get("children", []), node)

    bones_by_node = {}
    walk(model["outliner"], None)
    used = {e["uuid"] for e, _ in placed}
    model["elements"] = [e for e in model["elements"] if e["uuid"] in used]
    return placed, bones


# --- painting --------------------------------------------------------------------------------------------------------

def ramp(palette, t):
    """Palette colour at t in [0, 1] (n,), nearest step."""
    idx = np.clip(np.round(t * (len(palette) - 1)), 0, len(palette) - 1).astype(int)
    return palette[idx]


def grain_axis(fc):
    """The element's longest axis in the world: bone grain, feather vanes and tail scales run along it."""
    el = fc["element"]
    size = np.array(el["to"]) - np.array(el["from"])
    axis = np.zeros(3)
    axis[int(np.argmax(size))] = 1.0
    return fc["node"].dir_to_world(axis[None, :])[0]


def stone(world, normal, palette, seed, cell=(12.0, 7.0, 12.0)):
    """Cracked plates (Voronoi slabs) with lit upper rims, shadowed feet, pits and grime: rough stone at a distance."""
    tone, gap, rel_y = plates(world, cell, seed)
    grime = fbm(world, (0.12, 0.12, 0.12), seed + 11)
    n = len(palette) - 1
    shade = 0.25 + tone * 0.5 + (grime - 0.5) * 0.45 + np.clip(-rel_y, -1.0, 1.0) * 0.12
    near = gap < 0.2
    shade = np.where(near & (rel_y < 0) & (normal[:, 1] > -0.5), shade + 0.22, shade)
    shade = np.where(near & (rel_y >= 0), shade - 0.22, shade)
    shade = np.where(value_noise(world, (0.9, 0.9, 0.9), seed + 19) > 0.93, shade - 0.3, shade)
    color = ramp(palette, np.clip(shade, 0, 1))
    color[gap < 0.08] = palette[0] * 0.5
    crack = np.abs(fbm(world * np.array([1.0, 0.5, 1.0]), (0.22, 0.22, 0.22), seed + 23, octaves=2) - 0.5) < 0.012
    color[crack] = P["void"][0]
    return color, gap, crack


def scales(uv, palette, size=6):
    """Overlapping scales in face texel space: rows offset by half a scale, dark rims, light crowns."""
    u, v = uv[:, 0], uv[:, 1]
    row = np.floor(v / (size * 0.66))
    off = (row % 2) * size / 2
    cu = (u + off) % size - size / 2
    cv = (v % (size * 0.66)) / (size * 0.66)
    d = np.sqrt((cu / (size / 2)) ** 2 + (1 - cv) ** 2)
    t = np.clip(1.0 - d * 0.75, 0, 1)
    color = ramp(palette, t * 0.9 + 0.05)
    color[d > 1.0] = palette[0]
    return color, t


def paint(fc, world, uv, normal):
    """Colour (n, 3), alpha (n,) and glow mask (n,) of the texels of one face."""
    mat = fc["mat"]
    n = len(world)
    color = np.zeros((n, 3))
    alpha = np.full(n, 255.0)
    glow = np.zeros(n, dtype=bool)

    sky_rims = mat.startswith("loaded_")
    if sky_rims:
        mat = mat[len("loaded_"):]
    shifted = mat.startswith("glitch_slice")
    if shifted:
        mat = mat.split(":")[1] if ":" in mat else "corrupt_stone"

    if mat == "void":
        color[:] = ramp(P["void"], fbm(world, (0.3, 0.3, 0.3), 91))
        specks = value_noise(world, (2.1, 2.1, 2.1), 92) > 0.95
        color[specks] = P["corruption"][3]
    elif mat == "void_stars":
        color[:] = ramp(P["void"], fbm(world, (0.2, 0.2, 0.2), 93))
        star = value_noise(world, (3.0, 3.0, 3.0), 94) > 0.955
        color[star] = ramp(P["star"], value_noise(world, (1.0, 1.0, 1.0), 95)[star])
        glow |= star
        haze = (fbm(world, (0.08, 0.08, 0.08), 96) > 0.62) & ~star
        color[haze] = P["corruption"][2]
    elif mat == "hole":
        color[:] = P["void"][0]
        rim = value_noise(world, (1.5, 1.5, 1.5), 51) > 0.8
        color[rim] = P["corruption"][1]
    elif mat == "eye":
        color[:] = WHITE
        glow[:] = True
    elif mat == "glitch_cyan":
        color[:] = np.where((value_noise(world, (2.0, 2.0, 2.0), 97) > 0.5)[:, None], CYAN, hexes("A8E8FF")[0])
        dead = value_noise(world, (2.7, 2.7, 2.7), 98) > 0.9
        color[dead] = GLITCH
        glow[:] = True
    elif mat == "missing":
        check = ((np.floor(uv[:, 0] / 4) + np.floor(uv[:, 1] / 4)) % 2).astype(bool)
        color[:] = np.where(check[:, None], GLITCH, BLACK)
        glow[:] = check
    elif mat == "wire":
        # A wire frame: the face's border and a grid every 8 texels; between the wires there is nothing.
        w, h = fc["sw"], fc["sh"]
        border = (uv[:, 0] < 1) | (uv[:, 1] < 1) | (uv[:, 0] >= w - 1) | (uv[:, 1] >= h - 1)
        grid = (uv[:, 0] % 8 < 1) | (uv[:, 1] % 8 < 1)
        line = border | grid
        color[:] = CYAN
        color[line & (value_noise(world, (1.0, 1.0, 1.0), 99) > 0.7)] = hexes("6FB8D8")[0]
        alpha = np.where(line, 255.0, 0.0)
        glow = line.copy()
    elif mat == "stone" or mat == "holle_stone":
        color, gap, crack = stone(world, normal, P["stone"], 3)
        moss_field = fbm(world, (0.45, 0.45, 0.45), 29) + normal[:, 1] * 0.35
        hanging = fbm(world * np.array([1.0, 0.3, 1.0]), (0.8, 0.8, 0.8), 31)
        moss = (moss_field > 0.8) | ((moss_field > 0.66) & (hanging > 0.62) & (np.abs(normal[:, 1]) < 0.5))
        color[moss] = ramp(P["moss"], fbm(world, (1.4, 1.4, 1.4), 37)[moss])
        if mat == "holle_stone":
            streak = fbm(world * np.array([1.0, 0.15, 1.0]), (0.55, 0.55, 0.55), 43, octaves=2)
            corrupt = streak > 0.56
            color[corrupt] = ramp(P["corruption"][1:6], fbm(world, (0.5, 0.2, 0.5), 47)[corrupt])
            vein = corrupt & (value_noise(world, (1.3, 1.3, 1.3), 45) > 0.8)
            color[vein] = P["corruption"][6]
            glow |= vein
    elif mat == "corrupt_stone":
        color, gap, crack = stone(world, normal, P["corrupt_stone"], 5, cell=(10.0, 8.0, 10.0))
        vein = (np.abs(fbm(world, (0.12, 0.12, 0.12), 7, octaves=2) - 0.5) < 0.012)
        color[vein] = P["corruption"][6]
        glow |= vein & (value_noise(world, (1.1, 1.1, 1.1), 8) > 0.4)
        stain = fbm(world, (0.2, 0.2, 0.2), 9) > 0.66
        color[stain] = color[stain] * 0.55 + P["corruption"][3] * 0.45
    elif mat == "corruption":
        color, gap, crack = stone(world, normal, P["corruption"][1:6], 13, cell=(6.0, 4.0, 6.0))
        edge = gap < 0.12
        color[edge] = P["corruption"][7]
        glow |= edge & (value_noise(world, (1.7, 1.7, 1.7), 14) > 0.35)
    elif mat == "flow":
        streak = fbm(world * np.array([1.0, 0.2, 1.0]), (0.7, 0.7, 0.7), 61)
        body = np.clip(streak * 1.35 - 0.18, 0, 1)
        color[:] = ramp(P["corruption"][:6], body)
        bright = body > 0.82
        glow |= bright
        knuckle = stone(world, normal, P["corrupt_stone"], 17, cell=(10.0, 6.0, 10.0))[0]
        rock = fbm(world, (0.25, 0.25, 0.25), 62) > 0.6
        color[rock & ~bright] = knuckle[rock & ~bright] * 0.6 + P["corruption"][2] * 0.4
        pix = value_noise(world, (3.0, 3.0, 3.0), 63) > 0.965
        color[pix] = GLITCH
        glow |= pix
    elif mat == "canyon":
        warp = (fbm(world, (0.08, 0.08, 0.08), 71) - 0.5) * 9.0
        band = np.floor((world[:, 1] + warp) / 3.2)
        tone = hash3(band.astype(np.int64), 0, 0, 72)
        grit = (value_noise(world, (1.8, 1.8, 1.8), 73) - 0.5) * 0.25
        color[:] = ramp(P["canyon"], np.clip(tone * 0.85 + 0.1 + grit, 0, 1))
        lip = ((world[:, 1] + warp) / 3.2) % 1.0 < 0.14
        color[lip] = color[lip] * 0.62
        crack = np.abs(fbm(world * np.array([1.0, 0.3, 1.0]), (0.25, 0.25, 0.25), 74, octaves=2) - 0.5) < 0.014
        color[crack] = P["canyon"][0] * 0.5
    elif mat in ("ash", "basalt", "charred"):
        palette = {"ash": P["ash"], "basalt": P["ash"], "charred": P["charred"]}[mat]
        color, gap, crack = stone(world, normal, palette, 81, cell=(9.0, 6.0, 9.0))
        soot = fbm(world, (0.15, 0.15, 0.15), 82) > 0.64
        color[soot] = color[soot] * 0.6
        ember_amount = {"ash": 0.45, "basalt": 0.3, "charred": 0.15}[mat]
        ember = ((gap < 0.06) | crack) & (fbm(world, (0.18, 0.18, 0.18), 83, octaves=2) < ember_amount)
        color[ember] = ramp(P["ember"][1:], value_noise(world, (2.0, 2.0, 2.0), 84)[ember])
        glow |= ember
    elif mat == "lava":
        heat = fbm(world, (0.6, 0.6, 0.6), 85)
        color[:] = ramp(P["ember"][2:], heat)
        glow[:] = True
        crust = fbm(world, (0.35, 0.35, 0.35), 86) > 0.62
        color[crust] = ramp(P["ash"][1:4], value_noise(world, (1.5, 1.5, 1.5), 87)[crust])
        glow[crust] = False
    elif mat == "feather":
        axis = grain_axis(fc)
        along = world @ axis
        across = np.linalg.norm(world - along[:, None] * axis[None, :], axis=1)
        vane = np.sin(across * 2.4 + fbm(world, (0.3, 0.3, 0.3), 88) * 4.0)
        color[:] = ramp(P["ash"][1:], np.clip(0.35 + vane * 0.25 + (value_noise(world, (1.2, 1.2, 1.2), 89) - 0.5) * 0.3, 0, 1))
        w, h = fc["sw"], fc["sh"]
        edge = (uv[:, 0] < 1.5) | (uv[:, 0] >= w - 1.5) | (uv[:, 1] < 1.5) | (uv[:, 1] >= h - 1.5)
        smoulder = edge & (value_noise(world, (0.9, 0.9, 0.9), 90) > 0.38)
        color[smoulder] = ramp(P["ember"], value_noise(world, (2.2, 2.2, 2.2), 101)[smoulder])
        glow |= smoulder
        burn = (fbm(world, (0.5, 0.5, 0.5), 102) > 0.7) & ~edge
        color[burn] = P["ember"][1]
        glow |= burn & (value_noise(world, (1.7, 1.7, 1.7), 103) > 0.55)
        hole = (value_noise(world, (0.9, 0.9, 0.9), 104) > 0.9) & ~edge
        alpha[hole] = 0
    elif mat in ("bone", "bone_dirty"):
        axis = grain_axis(fc)
        along = world @ axis
        grain = fbm(world - along[:, None] * axis[None, :] * 0.9, (0.45, 0.45, 0.45), 105, octaves=2)
        base = 0.62 + (grain - 0.5) * 0.7 if mat == "bone" else 0.45 + (grain - 0.5) * 0.7
        dirt = fbm(world, (0.08, 0.08, 0.08), 106)
        color[:] = ramp(P["bone"], np.clip(base - (dirt - 0.5) * 0.7, 0, 1))
        crack = np.abs(fbm(world - along[:, None] * axis[None, :] * 0.7, (0.2, 0.2, 0.2), 107, octaves=2) - 0.5) < 0.012
        color[crack] = P["bone"][0]
        blood = (fbm(world, (0.25, 0.25, 0.25), 108) > 0.72)
        color[blood] = color[blood] * 0.5 + hexes("3A1A14")[0] * 0.5
    elif mat in ("pearl", "scale_grey"):
        palette = P[mat]
        color, t = scales(uv, palette, size=10 if mat == "pearl" else 7)
        if mat == "pearl":
            glint = (t > 0.9) & (value_noise(world, (1.0, 1.0, 1.0), 111) > 0.6)
            color[glint] = P["pearl_glint"][0]
        rot = fbm(world, (0.12, 0.12, 0.12), 112) > 0.66
        color[rot] = color[rot] * 0.5 + P["rot"][4] * 0.5
    elif mat == "rot":
        mottle = fbm(world, (0.45, 0.45, 0.45), 121)
        color[:] = ramp(P["rot"][1:], np.clip(mottle * 1.3 - 0.15, 0, 1))
        tone, gap, _ = plates(world, (4.0, 4.0, 4.0), 122)
        pit = gap > 0.55
        color[pit] = P["rot"][0]
        boil = (tone > 0.82) & (gap > 0.3)
        color[boil] = P["rot"][6]
        glow |= boil & (value_noise(world, (1.0, 1.0, 1.0), 123) > 0.5)
        scale_c, _ = scales(uv, P["rot"][2:6], size=5)
        scaly = fbm(world, (0.2, 0.2, 0.2), 124) > 0.55
        color[scaly & ~pit] = scale_c[scaly & ~pit]
    elif mat == "coral":
        tone, gap, _ = plates(world, (1.8, 1.8, 1.8), 131)
        color[:] = ramp(P["coral"][1:], np.clip(tone * 0.7 + 0.2, 0, 1))
        pore = gap > 0.5
        color[pore] = P["coral"][0]
        tip = value_noise(world, (1.2, 1.2, 1.2), 132) > 0.75
        glow |= tip & ~pore
    else:
        raise ValueError("unknown material " + mat)

    if mat not in ("wire", "missing", "eye", "void", "void_stars", "hole", "glitch_cyan", "lava") and fc["face"] not in ("up", "down"):
        # Chunky edges: the top row of a side face catches light, the bottom two sit in shadow.
        h = fc["sh"]
        color[uv[:, 1] < 1] *= 1.22
        color[uv[:, 1] >= h - 2] *= 0.62
    if mat not in NO_DESPECKLE:
        color, glow = despeckle(color, glow, fc["sh"], fc["sw"])
    if shifted:
        # A slice that slid out of place: rows torn sideways and a glitch scan line through it.
        tear = hash3(np.floor(uv[:, 1] / 2).astype(np.int64), 3, 0, 141) > 0.6
        color[tear] = np.roll(color, 3, axis=0)[tear]
        scan = (uv[:, 1] % 5 < 1) & (value_noise(world, (0.8, 0.8, 0.8), 142) > 0.5)
        color[scan] = np.where((value_noise(world, (2.0, 2.0, 2.0), 143) > 0.5)[scan][:, None], GLITCH, CYAN)
        glow |= scan
    if sky_rims:
        w, h = fc["sw"], fc["sh"]
        rim = (uv[:, 0] < 1.5) | (uv[:, 0] >= w - 1.5) | (uv[:, 1] < 1.5) | (uv[:, 1] >= h - 1.5)
        color[rim] = ramp(P["sky"], value_noise(world, (1.5, 1.5, 1.5), 151)[rim])
        glow |= rim
        sheen = (np.abs(fbm(world, (0.4, 0.4, 0.4), 152, octaves=2) - 0.5) < 0.02) & ~rim
        color[sheen] = P["sky"][3]
        glow |= sheen
    return color, alpha, glow


NO_DESPECKLE = {"wire", "missing", "eye", "glitch_cyan", "void_stars", "hole"}


def despeckle(color, glow, h, w, passes=2):
    """No lone pixels (the Hollow's lesson, preview 3): a texel whose colour none of its 4 neighbours shares takes the
    colour most of them have, so tones come in clusters of a few pixels. A replaced texel stops glowing."""
    if h < 3 or w < 3:
        return color, glow
    grid = np.round(color).astype(np.int64).reshape(h, w, 3)
    key = (grid[..., 0] << 16) | (grid[..., 1] << 8) | grid[..., 2]
    lit = glow.reshape(h, w).copy()
    for _ in range(passes):
        pad = np.pad(key, 1, mode="edge")
        around = np.stack([pad[1 + dy:1 + dy + h, 1 + dx:1 + dx + w] for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0))])
        lonely = ~np.any(around == key, axis=0)
        srt = np.sort(around, axis=0)
        # The most common of the four: a pair in the sorted list, else the second darkest-keyed.
        pick = np.where(srt[0] == srt[1], srt[0], np.where(srt[1] == srt[2], srt[1], np.where(srt[2] == srt[3], srt[2], srt[1])))
        key = np.where(lonely, pick, key)
        lit = lit & ~lonely
    out = np.stack([(key >> 16) & 255, (key >> 8) & 255, key & 255], axis=-1).reshape(-1, 3).astype(np.float64)
    return out, lit.reshape(-1)


def mosaic_seams(fc, uv, world, color, glow):
    """The torso tiles are sewn together: a void seam round every face, stitched with glitch thread."""
    if not fc["seamed"]:
        return
    w, h = fc["sw"], fc["sh"]
    seam = (uv[:, 0] < 1) | (uv[:, 1] < 1) | (uv[:, 0] >= w - 1) | (uv[:, 1] >= h - 1)
    color[seam] = P["void"][1]
    along = np.where((uv[:, 0] < 1) | (uv[:, 0] >= w - 1), uv[:, 1], uv[:, 0])
    # Stitches in short dashes, on stretches of a seam only, so the seams read as lines and not as dotted frames.
    stitch = seam & (along % 7 < 2) & (value_noise(world, (0.18, 0.18, 0.18), 161) > 0.55)
    pick = value_noise(world, (0.3, 0.3, 0.3), 162) > 0.5
    color[stitch] = np.where(pick[stitch][:, None], GLITCH, CYAN)
    glow |= stitch


def glitch_overlay(world, frame):
    """One frame of the glitch overlay: scan-line tears crawling up the body and dead pixels that blink."""
    period = 48.0
    t = frame / FRAMES
    lane = np.floor(world[:, 1] / 3.0)
    active = hash3(lane.astype(np.int64), frame, 0, 171) > 0.82
    tear = active & (np.mod(world[:, 0] + world[:, 2] + hash3(lane.astype(np.int64), 0, 1, 172) * period
                            + t * period, period) < 14.0)
    dead = value_noise(world + frame * 3.1, (2.5, 2.5, 2.5), 173) > 0.985
    color = np.zeros((len(world), 3))
    alpha = np.zeros(len(world))
    pick = hash3(lane.astype(np.int64), frame, 2, 174) > 0.5
    color[tear] = np.where(pick[tear][:, None], GLITCH, CYAN)
    alpha[tear] = 150
    color[dead] = WHITE
    alpha[dead] = 230
    return color, alpha


# --- atlas -----------------------------------------------------------------------------------------------------------

def pack(faces):
    """Skyline packing, tallest first: each face goes where the skyline under it is lowest. Returns the atlas height (a
    multiple of 16)."""
    heights = np.zeros(ATLAS_W, dtype=np.int64)
    for i in sorted(range(len(faces)), key=lambda i: (-faces[i]["sh"], -faces[i]["sw"])):
        f = faces[i]
        tops = np.lib.stride_tricks.sliding_window_view(heights, f["sw"]).max(axis=1)
        x = int(np.argmin(tops))
        f["sx"], f["sy"] = x, int(tops[x])
        heights[x:x + f["sw"]] = tops[x] + f["sh"]
    return int(math.ceil(heights.max() / 16.0) * 16)


SEAMED_PREFIXES = ("slab_", "slice_", "covert_")


def main():
    with open(SRC, encoding="utf-8") as f:
        model = restructure(json.load(f))
    placed, bones = walk_model(model)
    faces = []
    for element, node in placed:
        f, t = element["from"], element["to"]
        e_node = Node(element["name"], element["origin"], element.get("rotation", [0, 0, 0]), node)
        mat = element["_mat"]
        if mat in LOW_DENSITY or element["name"].endswith("_core"):
            density = 0.25
        elif element["name"].startswith(("slab_", "tail_")):
            # The big torso plates and the coil: a little coarser keeps the atlas at 1024 square.
            density = 0.75
        else:
            density = 1.0
        for face in ("north", "east", "south", "west", "up", "down"):
            corner, uax, vax, nrm = face_frame(face, f, t)
            w, h = np.linalg.norm(uax), np.linalg.norm(vax)
            faces.append({"element": element, "face": face, "node": e_node, "corner": np.array(corner, float),
                          "u": np.array(uax, float), "v": np.array(vax, float), "normal": np.array(nrm, float),
                          "w": w, "h": h, "d": density, "mat": mat,
                          "seamed": element["name"].startswith(SEAMED_PREFIXES),
                          "sw": max(1, int(math.ceil(w * density - 1e-6))),
                          "sh": max(1, int(math.ceil(h * density - 1e-6)))})
    atlas_h = pack(faces)
    base = np.zeros((atlas_h, ATLAS_W, 4), dtype=np.uint8)
    glowmap = np.zeros_like(base)
    overlay = np.zeros((atlas_h * FRAMES, ATLAS_W, 4), dtype=np.uint8)

    for fc in faces:
        sw, sh = fc["sw"], fc["sh"]
        jj, ii = np.meshgrid(np.arange(sh), np.arange(sw), indexing="ij")
        uu = ((ii + 0.5) / sw).reshape(-1, 1)
        vv = ((jj + 0.5) / sh).reshape(-1, 1)
        world = fc["node"].to_world(fc["corner"] + uu * fc["u"] + vv * fc["v"])
        normal = np.repeat(fc["node"].dir_to_world(fc["normal"][None, :]), len(world), axis=0)
        uv = np.stack([ii.reshape(-1), jj.reshape(-1)], axis=1).astype(np.float64)
        color, alpha, glow = paint(fc, world, uv, normal)
        mosaic_seams(fc, uv, world, color, glow)
        ys, xs = jj.reshape(-1) + fc["sy"], ii.reshape(-1) + fc["sx"]
        base[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        base[ys, xs, 3] = alpha.astype(np.uint8)
        glowmap[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        glowmap[ys, xs, 3] = np.where(glow & (alpha > 0), 255, 0)
        if fc["mat"] not in ("wire", "eye", "hole") and fc["face"] not in ("up", "down"):
            for frame in range(FRAMES):
                ocol, oalpha = glitch_overlay(world, frame)
                oy = ys + frame * atlas_h
                overlay[oy, xs, :3] = ocol.astype(np.uint8)
                overlay[oy, xs, 3] = np.where(alpha > 0, oalpha, 0).astype(np.uint8)
        fc["uv"] = [fc["sx"], fc["sy"], fc["sx"] + fc["w"] * fc["d"], fc["sy"] + fc["h"] * fc["d"]]

    for path in (OUT_TEX, OUT_GLOW, OUT_GLITCH, OUT_GEO):
        os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(base, "RGBA").save(OUT_TEX, optimize=True)
    Image.fromarray(glowmap, "RGBA").save(OUT_GLOW, optimize=True)
    # Half resolution is enough for scan-line tears (the UVs are normalised, so the layout still matches): 4 MB of
    # texture instead of 16.
    Image.fromarray(overlay[::2, ::2], "RGBA").save(OUT_GLITCH, optimize=True)
    with open(OUT_GLITCH + ".mcmeta", "w", encoding="utf-8", newline="\n") as f:
        # Frames are not square: give their size (half the atlas, see above).
        json.dump({"animation": {"frametime": 3, "interpolate": False, "width": ATLAS_W // 2, "height": atlas_h // 2}},
                  f, indent=2)
        f.write("\n")
    write_geo(bones, faces, atlas_h)
    write_bbmodel(model, faces, atlas_h)
    print(f"atlas {ATLAS_W}x{atlas_h}, {len(faces)} faces, {len(bones)} bones, glitch overlay {FRAMES} frames")


def rounded(values):
    return [round(float(v), 5) for v in values]


def write_geo(bones, faces, atlas_h):
    by_element = {}
    for fc in faces:
        by_element.setdefault(fc["element"]["uuid"], {})[fc["face"]] = fc["uv"]
    out_bones = []
    for g, parent, elements, node in bones:
        o = g["origin"]
        bone = {"name": g["name"]}
        if parent:
            bone["parent"] = parent
        bone["pivot"] = rounded([-o[0], o[1], o[2]])
        r = g.get("rotation", [0, 0, 0])
        if any(r):
            bone["rotation"] = rounded([-r[0], -r[1], r[2]])
        cubes = []
        for el in elements:
            f, t = el["from"], el["to"]
            c = {"origin": rounded([-t[0], f[1], f[2]]), "size": rounded([t[i] - f[i] for i in range(3)])}
            r = el.get("rotation", [0, 0, 0])
            if any(r):
                eo = el["origin"]
                c["pivot"] = rounded([-eo[0], eo[1], eo[2]])
                c["rotation"] = rounded([-r[0], -r[1], r[2]])
            if el.get("inflate"):
                c["inflate"] = el["inflate"]
            uv = {}
            for face in ("north", "east", "south", "west", "up", "down"):
                u0, v0, u1, v1 = by_element[el["uuid"]][face]
                if face in ("up", "down"):
                    uv[face] = {"uv": rounded([u1, v1]), "uv_size": rounded([u0 - u1, v0 - v1])}
                else:
                    uv[face] = {"uv": rounded([u0, v0]), "uv_size": rounded([u1 - u0, v1 - v0])}
            c["uv"] = uv
            cubes.append(c)
        if cubes:
            bone["cubes"] = cubes
        out_bones.append(bone)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.the_unfinished", "texture_width": ATLAS_W, "texture_height": atlas_h,
                        "visible_bounds_width": 32, "visible_bounds_height": 22, "visible_bounds_offset": [0, 10, 0]},
        "bones": out_bones}]}
    with open(OUT_GEO, "w", encoding="utf-8", newline="\n") as f:
        json.dump(geo, f, indent="\t")
        f.write("\n")


def write_bbmodel(model, faces, atlas_h):
    out = copy.deepcopy(model)
    by_uuid = {e["uuid"]: e for e in out["elements"]}
    for fc in faces:
        by_uuid[fc["element"]["uuid"]]["faces"][fc["face"]]["uv"] = rounded(fc["uv"])
    for el in out["elements"]:
        el.pop("_mat", None)
        for face in el["faces"].values():
            face.pop("_mat", None)
    out["name"] = "the_unfinished"
    out["model_identifier"] = "the_unfinished"
    out["resolution"] = {"width": ATLAS_W, "height": atlas_h}
    with open(OUT_TEX, "rb") as f:
        data = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")
    tex = out["textures"][0]
    tex.update({"name": "the_unfinished.png", "path": "", "relative_path": "", "width": ATLAS_W, "height": atlas_h,
                "uv_width": ATLAS_W, "uv_height": atlas_h, "source": data})
    with open(OUT_BB, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    sys.exit(main())
