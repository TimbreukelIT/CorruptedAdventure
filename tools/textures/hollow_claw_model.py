"""The void claw of The Hollow's rift rain (phase 4 life round): a stone hand, eaten by corruption, that claws up out of a
crack in the ground. Model, textures and animations for GeckoLib.

    python tools/textures/hollow_claw_model.py        (from the repository root)

Writes
- assets/corruptedadventure/geo/entity/hollow_void_claw.geo.json            Bedrock geometry 1.12.0 (box UV)
- assets/corruptedadventure/textures/entity/hollow_void_claw.png            cracked stone going over into corruption toward
                                                                            the fingers, void-black talons
- assets/corruptedadventure/textures/entity/hollow_void_claw_glowmask.png   emissive (GeckoLib AutoGlowingGeoLayer): only the
                                                                            seams at the joints and the rims of the talons
- assets/corruptedadventure/animations/entity/hollow_void_claw.animation.json
                                                                            "hidden" (under the ground, while it is a crack) and
                                                                            "grab" (HollowVoidClaw.LIFE_TICKS 16 = 0.8 s: up
                                                                            with the fingers spread, clench, sink back)

The hand stands up out of the ground: a forearm stub, the back of the hand, four long fingers of three joints each with a
knuckle at every joint and a hooked talon, and a thumb, all hooked like a hand that grabs, open enough to read as a claw. Some fingers are bent
too far or broken off short, so it is never a clean glove. Palettes from fase-4-nightmares-concept.md (De Holle): stone
2E2D33..7A7980, corruption 140E1A..9C7AC4, void 07050A. Deterministic: the same script gives the same output.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure")
OUT_GEO = os.path.join(ASSETS, "geo/entity/hollow_void_claw.geo.json")
OUT_TEX = os.path.join(ASSETS, "textures/entity/hollow_void_claw.png")
OUT_GLOW = os.path.join(ASSETS, "textures/entity/hollow_void_claw_glowmask.png")
OUT_ANIM = os.path.join(ASSETS, "animations/entity/hollow_void_claw.animation.json")

TEX_W, TEX_H = 128, 128


def hexes(*values):
    return np.array([[int(v[i:i + 2], 16) for i in (0, 2, 4)] for v in values], dtype=np.float64)


STONE = hexes("2E2D33", "3E3D44", "4F4E55", "63626A", "7A7980")
CORRUPTION = hexes("140E1A", "2B1F38", "45305C", "6A4A8C", "9C7AC4")
VOID = hexes("07050A")[0]
GLITCH = hexes("F800F8")[0]


# --- geometry -------------------------------------------------------------------------------------------------------

class Model:
    def __init__(self):
        self.bones = []
        self.cubes = []  # (bone, origin, size, material) with material in "stone", "corrupt", "talon"

    def bone(self, name, parent, pivot, rotation=(0, 0, 0)):
        self.bones.append({"name": name, "parent": parent, "pivot": list(pivot), "rotation": list(rotation)})
        return name

    def cube(self, bone, origin, size, material, inflate=0.0):
        self.cubes.append((bone, list(origin), list(size), material, inflate))


def finger(model, name, parent, base, lengths, width, curl, splay, talon, broken=False):
    """A finger of three joints standing up from {@code base} (x, y, z), curled by {@code curl} degrees per joint (negative
    x = toward the front, -z) and splayed sideways by {@code splay} (z rotation of the first joint). Every joint gets a
    knuckle; the last ends in a hooked talon, unless the finger is broken off."""
    x, y, z = base
    parent_bone = parent
    w = width
    for i, length in enumerate(lengths):
        rot = (curl[i], 0, splay if i == 0 else 0)
        bone = model.bone(f"{name}_{i + 1}", parent_bone, (x, y, z), rot)
        hw = w / 2.0
        material = "stone" if i == 0 else "corrupt"
        model.cube(bone, (x - hw, y, z - hw), (w, length, w), material)
        # The knuckle: a lump at the joint, a little wider than the finger.
        model.cube(bone, (x - hw - 0.5, y - 0.5, z - hw - 0.5), (w + 1, 2.5, w + 1), "stone" if i == 0 else "corrupt")
        y += length
        parent_bone = bone
        w = max(1.5, w - 0.6)
    if broken:
        return
    bone = model.bone(f"{name}_talon", parent_bone, (x, y, z), (talon, 0, 0))
    hw = w / 2.0
    model.cube(bone, (x - hw, y, z - hw), (w, 4, w), "talon")
    model.cube(bone, (x - hw + 0.25, y + 4, z - hw - 0.75), (max(1.0, w - 0.5), 3, max(1.0, w - 0.5)), "talon")
    model.cube(bone, (x - 0.5, y + 6.5, z - hw - 1.75), (1, 2, 1), "talon")


def build():
    m = Model()
    m.bone("root", None, (0, 0, 0))
    m.bone("arm", "root", (0, 0, 0), (-8, 0, 4))
    # The forearm stub comes up out of the crack, rough and broader at the bottom.
    m.cube("arm", (-5, -10, -5), (10, 16, 10), "stone")
    m.cube("arm", (-6, -10, -6), (12, 6, 12), "stone")
    m.cube("arm", (2.5, -2, -6.5), (4, 7, 3), "corrupt")  # a plate of corruption grown over the wrist
    m.bone("hand", "arm", (0, 6, 0), (-12, 0, 0))
    m.cube("hand", (-6.5, 6, -4.5), (13, 9, 9), "stone")
    m.cube("hand", (-5.5, 9, 3.5), (11, 6, 2), "corrupt")  # back of the hand, bulging
    m.cube("hand", (-2, 12, 4.5), (3, 3, 2), "corrupt")
    # Four fingers along the top of the hand, curled forward; the ring finger bent too far, the little one broken short.
    finger(m, "index", "hand", (-4.5, 15, -1), (8, 7, 5), 3.2, (-8, -16, -22), 16, -34)
    finger(m, "middle", "hand", (-1.5, 15, -1), (9, 8, 6), 3.4, (-6, -14, -24), 4, -38)
    finger(m, "ring", "hand", (1.5, 15, -1), (8, 7, 5), 3.2, (-14, -30, -20), -8, -32)
    finger(m, "little", "hand", (4.5, 14, -1), (6, 5, 4), 2.8, (-10, -16, -12), -22, -30, broken=True)
    # The thumb from the side, crossing in front.
    finger(m, "thumb", "hand", (-6.5, 9, -3), (6, 5, 4), 3.0, (-6, -12, -18), 48, -30)
    return m


# --- UV and textures ------------------------------------------------------------------------------------------------

def layout(cubes):
    """Box UV regions on a shelf: per cube (u, v) of a 2(w+d) x (d+h) region."""
    regions = []
    u = v = shelf = 0
    for _, _, size, _, _ in cubes:
        w, h, d = (int(math.ceil(s)) for s in size)
        rw, rh = 2 * (w + d), d + h
        if u + rw > TEX_W:
            u = 0
            v += shelf
            shelf = 0
        regions.append((u, v, w, h, d))
        u += rw
        shelf = max(shelf, rh)
    assert v + shelf <= TEX_H, "atlas too small"
    return regions


def pick(palette, t):
    t = min(max(t, 0.0), 0.999)
    return palette[int(t * len(palette))]


def paint(cubes, regions):
    rng = random.Random(4127)
    tex = np.zeros((TEX_H, TEX_W, 4), dtype=np.float64)
    glow = np.zeros((TEX_H, TEX_W, 4), dtype=np.float64)
    for (bone, origin, size, material, _), (u, v, w, h, d) in zip(cubes, regions):
        rw, rh = 2 * (w + d), d + h
        # Clustered tones: coarse noise, then a fine grain.
        coarse = np.array([[rng.random() for _ in range(rw // 3 + 2)] for _ in range(rh // 3 + 2)])
        for py in range(rh):
            for px in range(rw):
                n = coarse[py // 3][px // 3] * 0.7 + rng.random() * 0.3
                side = py >= d  # the four side faces; py - d counts down from the top
                height = (py - d) / max(1, h - 1) if side else 0.0
                if material == "talon":
                    colour = VOID * 0.6 + pick(CORRUPTION[:3], n * 0.8) * 0.4
                elif material == "corrupt":
                    colour = pick(CORRUPTION[1:4], n * 0.9)
                    if n > 0.82:
                        colour = pick(STONE[:3], n - 0.5)  # stone showing through
                else:
                    colour = pick(STONE, n)
                    # Corruption creeps up from the cracks and gathers low (grime) and in streaks.
                    if side and (height > 0.75 and n < 0.45 or n < 0.12):
                        colour = pick(CORRUPTION[:3], n * 2.0)
                # Dark cracks: short broken lines.
                if rng.random() < 0.05:
                    colour = colour * 0.55
                tex[v + py, u + px, :3] = colour
                tex[v + py, u + px, 3] = 255
                # Emissive only in the seams: the top and bottom rows of the side faces (the joints), sparse.
                if side and material != "talon" and (py == d or py == rh - 1) and rng.random() < 0.55:
                    g = GLITCH if rng.random() < 0.06 else pick(CORRUPTION[3:], rng.random())
                    glow[v + py, u + px, :3] = g
                    glow[v + py, u + px, 3] = 255
                # The talons' rims: the side faces' outer columns glow faintly at the tip.
                if material == "talon" and side and (px % (w + d) in (0, d)) and rng.random() < 0.7:
                    glow[v + py, u + px, :3] = CORRUPTION[4]
                    glow[v + py, u + px, 3] = 255
    return tex, glow


def geo_json(model, regions):
    by_bone = {}
    for (bone, origin, size, material, inflate), (u, v, _, _, _) in zip(model.cubes, regions):
        cube = {"origin": [round(c, 3) for c in origin], "size": [round(s, 3) for s in size], "uv": [u, v]}
        if inflate:
            cube["inflate"] = inflate
        by_bone.setdefault(bone, []).append(cube)
    bones = []
    for b in model.bones:
        out = {"name": b["name"], "pivot": b["pivot"]}
        if b["parent"]:
            out["parent"] = b["parent"]
        if any(b["rotation"]):
            out["rotation"] = b["rotation"]
        if b["name"] in by_bone:
            out["cubes"] = by_bone[b["name"]]
        bones.append(out)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.hollow_void_claw", "texture_width": TEX_W, "texture_height": TEX_H,
                        "visible_bounds_width": 3, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones}]}


def keys(*pairs):
    return {str(round(float(t), 3)): list(v) for t, v in pairs}


def animations():
    fingers = ["index", "middle", "ring", "little", "thumb"]
    grab_bones = {
        # Up out of the crack in 3 ticks, stands, sinks back over the last 6 (LIFE_TICKS 16).
        "root": {"position": keys((0, (0, -36, 0)), (0.15, (0, 0, 0)), (0.5, (0, 0, 0)), (0.8, (0, -40, 0)))},
        "arm": {"rotation": keys((0, (8, 0, -6)), (0.15, (-4, 0, 6)), (0.3, (-10, 0, 2)), (0.5, (-8, 0, 3)), (0.8, (0, 0, 0)))},
    }
    for f in fingers:
        # Spread wide as it comes up (uncurl), then clench past the rest pose.
        grab_bones[f + "_1"] = {"rotation": keys((0, (30, 0, 0)), (0.15, (28, 0, 0)), (0.3, (-4, 0, 0)), (0.5, (-6, 0, 0)),
                                                 (0.8, (0, 0, 0)))}
        grab_bones[f + "_2"] = {"rotation": keys((0, (30, 0, 0)), (0.15, (26, 0, 0)), (0.3, (-6, 0, 0)), (0.5, (-8, 0, 0)),
                                                 (0.8, (0, 0, 0)))}
        if f != "little":
            grab_bones[f + "_talon"] = {"rotation": keys((0, (25, 0, 0)), (0.15, (20, 0, 0)), (0.3, (-10, 0, 0)),
                                                         (0.8, (0, 0, 0)))}
    return {"format_version": "1.8.0", "animations": {
        "animation.hollow_void_claw.hidden": {"loop": True, "animation_length": 1.0,
                                              "bones": {"root": {"position": keys((0, (0, -48, 0)))}}},
        "animation.hollow_void_claw.grab": {"loop": "hold_on_last_frame", "animation_length": 0.8, "bones": grab_bones},
    }}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


def main():
    model = build()
    regions = layout(model.cubes)
    tex, glow = paint(model.cubes, regions)
    write_json(OUT_GEO, geo_json(model, regions))
    Image.fromarray(tex.clip(0, 255).astype(np.uint8), "RGBA").save(OUT_TEX)
    Image.fromarray(glow.clip(0, 255).astype(np.uint8), "RGBA").save(OUT_GLOW)
    write_json(OUT_ANIM, animations())
    print(f"{len(model.bones)} bones, {len(model.cubes)} cubes -> {os.path.relpath(OUT_GEO, ROOT)} + textures + animations")


if __name__ == "__main__":
    main()
