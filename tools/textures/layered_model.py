"""The Layered (phase 4, ring 2 Nightmare): model, UV layout and textures from the Blockbench mockup.

    python tools/textures/layered_model.py            (from the repository root)

Reads the approved mockup docs/design/models/nightmare-ring2-de-gelaagde.bbmodel (geometry and bones; its shared
material swatches only tell which material a face is) and writes:

- assets/corruptedadventure/geo/entity/the_layered.geo.json      Bedrock geometry 1.12.0, as Blockbench's own Bedrock
                                                                  export writes it (x mirrored, x/y rotations negated)
- assets/corruptedadventure/textures/entity/the_layered.png       base: back plates in the canyon stone bands, giant's
                                                                  bone legs, dusty fur belly, dead heads, prey bones
- assets/corruptedadventure/textures/entity/the_layered_glowmask.png   emissive (GeckoLib AutoGlowingGeoLayer): only
                                                                  Herobrine's white eyes in the living head
- docs/design/models/the_layered.bbmodel                          the same model with the new UVs and the base texture,
                                                                  for Blockbench (previews, animation work)

Bone changes against the mockup: one root bone over everything (animations flip and drop the whole body) and the chains
hang from the front shins they are wrapped around, so they follow the legs.

The back is camouflage: its bands follow the length of the body, so when it lies flat against a canyon wall, head up,
the bands run level like the strata (red, orange, cream; the palettes of the three canyon stone blocks). Every face gets
its own place in the atlas (1 texel per model unit) and is painted from where its pixels are in the world, so bands,
dust and grime run on across cubes. Deterministic: the same input gives the same output.
Palettes: docs/design/img/nightmare-ring2-de-gelaagde.png and the canyon stone block textures.
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
SRC = os.path.join(ROOT, "docs/design/models/nightmare-ring2-de-gelaagde.bbmodel")
OUT_BB = os.path.join(ROOT, "docs/design/models/the_layered.bbmodel")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure")
OUT_GEO = os.path.join(ASSETS, "geo/entity/the_layered.geo.json")
OUT_TEX = os.path.join(ASSETS, "textures/entity/the_layered.png")
OUT_GLOW = os.path.join(ASSETS, "textures/entity/the_layered_glowmask.png")

ATLAS_W = 512


def hexes(*values):
    return np.array([[int(v[i:i + 2], 16) for i in (0, 2, 4)] for v in values], dtype=np.float64)


RED = hexes("6E2E1E", "8A3B26", "A44C30", "BC6040", "CF7A55")
ORANGE = hexes("8E4A1C", "A85E24", "C2742E", "D68B3E", "E4A55A")
CREAM = hexes("B49C7A", "C7B08C", "D8C39F", "E6D4B2", "F1E3C6")
SAND = hexes("C9B98A", "D6C799", "DFD1A5", "E8DCB2")
BONE = hexes("5A5040", "857A62", "AFA284", "D2C7A6", "ECE4CB")
DEAD = hexes("4A463E", "6E6A5E", "8F8A7A", "ACA694", "C4BEAA")
FUR = hexes("221C18", "332B25", "463B32", "5A4C40", "716050")
IRON = hexes("1E1B19", "2E2A27", "45403B", "5E5750", "6E4A30", "8A5A34")
BLACK = hexes("0C0A0C")[0]
SEAM = hexes("4A2418")[0]
WHITE = np.array([255.0, 255.0, 255.0])

# Strata along the body, from the head end backwards: (palette, thickness in px). Uneven like the canyon walls.
STRATA = [(ORANGE, 9), (RED, 13), (CREAM, 2), (RED, 8), (ORANGE, 10), (RED, 6), (CREAM, 2), (ORANGE, 7), (RED, 11)]
# The back is pulled this far toward the colour of canyon dust: dull, never clean.
DUST = hexes("8A7460")[0]
DUST_MIX = 0.26
GRIME = hexes("3A2A20")[0]


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


ROOT_UUID = "5b2e7c14-0d6a-4f3e-9c1b-2a7e0f8b0001"
# The middle of the body: the fall animation rolls it onto its back around this point.
ROOT_PIVOT = [0, 36, 6]
# Chains move to the shin bone they wrap: chain set 0 is on the front left leg, set 1 on the front right leg.
CHAIN_PARENTS = {"0": "leg_front_left_lower", "1": "leg_front_right_lower"}


def load_model():
    with open(SRC, encoding="utf-8") as f:
        model = json.load(f)
    groups = {g["uuid"]: g for g in model["groups"]}
    by_name = {}
    for g in model["groups"]:
        by_name.setdefault(g["name"], g)

    # Move the chain elements under the shins (Blockbench stores absolute positions, so they stay where they are).
    def find(children, uuid):
        for child in children:
            if isinstance(child, dict):
                if child["uuid"] == uuid:
                    return child
                hit = find(child.get("children", []), uuid)
                if hit:
                    return hit
        return None

    elements = {e["uuid"]: e for e in model["elements"]}
    chains = next(c for c in model["outliner"] if isinstance(c, dict) and groups[c["uuid"]]["name"] == "chains_optional")
    for child in chains["children"]:
        name = elements[child]["name"]
        target = by_name[CHAIN_PARENTS[name.split("_")[2]]]
        find(model["outliner"], target["uuid"])["children"].append(child)
    model["outliner"].remove(chains)
    model["groups"] = [g for g in model["groups"] if g["uuid"] != chains["uuid"]]

    # One root bone over the loose top-level bones, so animations can move all of it.
    root = copy.deepcopy(model["groups"][0])
    root.update({"name": "root", "uuid": ROOT_UUID, "origin": ROOT_PIVOT, "rotation": [0, 0, 0]})
    model["groups"].insert(0, root)
    model["outliner"] = [{"uuid": ROOT_UUID, "isOpen": True, "children": model["outliner"]}]
    groups = {g["uuid"]: g for g in model["groups"]}
    placed = []  # (element, node of its parent group)
    bones = []  # (group, parent group name or None, elements, node)

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

def material(name, ancestors):
    if name.startswith("eye_"):
        return "eye"
    if name.startswith(("socket_left", "socket_right", "cranium_hole", "nose_cavity", "crack", "scar", "beak_split",
                        "beak_groove")):
        return "hole"
    if name.startswith("chain_"):
        return "iron"
    if name.startswith(("plate_", "abdomen_base_", "carapace_", "thorax_side_plate_", "spine_shard_")):
        return "stone"
    if name.startswith(("belly_fur", "tuft_", "thorax_tuft_", "cephalothorax")) or name == "waist":
        return "fur"
    if name.startswith(("claw", "broken_spur")):
        return "claw"
    if name.startswith("tooth_"):
        return "tooth"
    if "prey_bones" in ancestors:
        return "prey"
    if "head_dead_left" in ancestors or "head_dead_right" in ancestors:
        return "dead"
    return "bone"


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


def cell_hash(p, seed):
    """A per-texel hash on integer world cells: crisp pixel speckle that stays put across faces."""
    i = np.floor(p + 1e-4).astype(np.int64)
    return hash3(i[:, 0], i[:, 1], i[:, 2], seed)


def tone(palette, shade):
    return palette[np.clip(np.round(shade), 0, len(palette) - 1).astype(int)]


# --- painting -------------------------------------------------------------------------------------------------------

def stratum(world):
    """Palette index (into STRATA) and the position inside the band (0 top .. 1 bottom) along the body. The bands run
    across the length (z) and climb a little with the stepped plates (y), barely warped: strata are calm and level."""
    period = sum(t for _, t in STRATA)
    warp = (fbm(world, (0.09, 0.09, 0.09), 101, octaves=2) - 0.5) * 2.6
    s = world[:, 2] + 0.25 * (world[:, 1] - 40.0) + warp + 30.0
    s = np.mod(s, period)
    edges = np.cumsum([0] + [t for _, t in STRATA])
    idx = np.searchsorted(edges, s, side="right") - 1
    idx = np.clip(idx, 0, len(STRATA) - 1)
    inside = (s - edges[idx]) / np.array([t for _, t in STRATA])[idx]
    return idx, inside


def unspeckle(tone, passes=2):
    """Replaces every pixel that shares its tone with none of its 4 neighbours by the middle tone around it, so tones
    come in clusters of a few pixels and never as single specks (as in hollow_model.py)."""
    h, w = tone.shape
    if h < 2 or w < 2:
        return tone
    for _ in range(passes):
        pad = np.pad(tone, 1, mode="edge")
        around = np.stack([pad[1 + dy:1 + dy + h, 1 + dx:1 + dx + w] for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0))])
        lonely = ~np.any(around == tone, axis=0)
        tone = np.where(lonely, np.sort(around, axis=0)[2], tone)
    return tone


def add_cracks(h, w, rng, count):
    """Thin cracks as short random walks across the face."""
    crack = np.zeros((h, w), dtype=bool)
    for _ in range(count):
        r, c = rng.randrange(h), rng.randrange(w)
        horizontal = rng.random() < 0.5
        for _ in range(rng.randint(3, 9)):
            if 0 <= r < h and 0 <= c < w:
                crack[r, c] = True
            if horizontal:
                c += 1
                r += rng.choice((-1, 0, 0, 1))
            else:
                r += 1
                c += rng.choice((-1, 0, 0, 1))
    return crack


def chips(h, w, rng, count):
    """Broken-off corners and nicks along the edges: small ragged bites out of the plate, 2-4 px."""
    hole = np.zeros((h, w), dtype=bool)
    if h < 3 or w < 3:
        return hole
    jj, ii = np.meshgrid(np.arange(h), np.arange(w), indexing="ij")
    for _ in range(count):
        size = rng.randint(2, 4)
        if rng.random() < 0.5:
            r = rng.choice((0, h - 1))
            c = rng.choice((0, w - 1)) if rng.random() < 0.6 else rng.randrange(w)
        else:
            r = rng.randrange(h)
            c = rng.choice((0, w - 1))
        hole |= (np.abs(jj - r) + np.abs(ii - c) + (np.sin(jj * 1.7 + ii * 2.3) > 0.6)) < size
    return hole


def paint_stone(world, normal, fv, h, w, rng):
    """Back plates: canyon stone in strata, dull and dusty so it disappears against a canyon wall: tones in clusters,
    thin cream layers, cracks, chipped corners and broken edges, dust on what faces up, grime toward the underside."""
    idx, inside = stratum(world)
    shade = (2.0 + (value_noise(world, (0.22, 0.22, 0.22), 103) - 0.5) * 1.8
             + (value_noise(world, (0.09, 0.09, 0.09), 104) - 0.5) * 1.2)
    # Band tops a little lighter, band feet darker: the strata read at a distance.
    shade += (0.5 - inside) * 0.7
    # Lamination: soft darker streaks running with the band, a few pixels long.
    lam = (np.mod(world[:, 2] + 0.25 * world[:, 1] + (value_noise(world, (0.2, 0.2, 0.2), 105) - 0.5) * 2.0, 3.0) < 0.8) & (
        value_noise(world, (0.5, 0.5, 0.5), 107) > 0.45)
    shade = np.where(lam, shade - 0.7, shade)
    tone = unspeckle(np.clip(np.round(shade), 0, 4).astype(int).reshape(h, w)).reshape(-1)
    color = np.zeros((len(world), 3))
    for k, (palette, _) in enumerate(STRATA):
        m = idx == k
        color[m] = palette[tone[m]]
    # Dull and dusty: everything pulled toward the colour of canyon dust.
    color = color * (1 - DUST_MIX) + DUST * DUST_MIX
    # Band boundaries: a dark seam where one stratum meets the next.
    seam = (inside < 0.12) & (value_noise(world, (0.6, 0.6, 0.6), 111) > 0.3)
    color[seam] = color[seam] * 0.7 + SEAM * 0.3
    # Ledges: worn top rim of a side face, dark shadow under the plate.
    side = np.abs(normal[:, 1]) < 0.5
    rim = side & (fv < 0.12) & (value_noise(world, (0.7, 0.7, 0.7), 112) > 0.35)
    color[rim] = np.minimum(color[rim] * 1.06 + 6, 255)
    under = side & (fv > 0.7)
    color[under] = color[under] * 0.66 + SEAM * 0.12
    # Dust and loose sand lying on top, in drifts.
    drift = np.clip((value_noise(world, (0.25, 0.25, 0.25), 113) - 0.5) * 2.0, 0, 0.5) * (normal[:, 1] > 0.5)
    sand = SAND[np.clip((value_noise(world, (0.6, 0.6, 0.6), 115) * 3.99).astype(int), 0, 3)] * 0.9
    color = color * (1 - drift[:, None]) + sand * drift[:, None]
    # Grime: old dirt toward the underside and the lower body, in smudges.
    smudge = np.clip((value_noise(world, (0.15, 0.15, 0.15), 116) - 0.45) * 2.0, 0, 1)
    low = np.clip((46.0 - world[:, 1]) / 10.0, 0, 1)
    grime = np.clip(low * 0.4 + smudge * 0.25, 0, 0.55)
    color = color * (1 - grime[:, None]) + GRIME * grime[:, None]
    color = color.reshape(h, w, 3)
    # Cracks and broken-off corners.
    crack = add_cracks(h, w, rng, max(1, (h * w) // 70))
    color[crack] = color[crack] * 0.45 + SEAM * 0.35
    hole = chips(h, w, rng, rng.randint(0, 2))
    color[hole] = color[hole] * 0.55 + SEAM * 0.2
    return color.reshape(-1, 3)


def along_axis(local, size):
    """Position along the element's longest axis, 0 at its 'from' end and 1 at its 'to' end."""
    axis = int(np.argmax(size))
    return np.clip(local[:, axis] / max(size[axis], 1e-6), 0, 1), axis


def paint_bone(world, local, size, palette, seed, grime_floor=26.0):
    """Giant's bone: fibres along the bone, dark joints, long cracks, old stains, grime and red canyon dust toward the
    ground."""
    n = len(world)
    t, axis = along_axis(local, size)
    across = np.delete(local, axis, axis=1)
    fibre_p = np.column_stack([across[:, 0], across[:, 1], local[:, axis] * 0.15])
    fibre = fbm(fibre_p, (0.9, 0.9, 0.9), seed, octaves=2)
    shade = 2.2 + (fibre - 0.5) * 2.0 + (fbm(world, (0.1, 0.1, 0.1), seed + 3) - 0.5) * 1.6
    # The ends of a long bone are darker and rougher: that is where the joints grind.
    if size[axis] > 12:
        shade -= np.clip(1.0 - np.minimum(t, 1 - t) / 0.12, 0, 1) * 1.0
    # Grime collects low: the shins stand in the canyon dust.
    low = np.clip((grime_floor - world[:, 1]) / grime_floor, 0, 1) if grime_floor > 0 else np.zeros(n)
    shade -= low * 1.2
    speck = cell_hash(world, seed + 5)
    shade = np.where(speck > 0.975, shade + 0.9, shade)
    shade = np.where(speck < 0.025, shade - 1.0, shade)
    color = tone(palette, shade)
    # Old stains, brown-black: grime, dried blood, the marrow of what it ate.
    stain = fbm(world, (0.16, 0.16, 0.16), seed + 13)
    s_amt = np.clip((stain - 0.5) * 2.6, 0, 0.6)
    color = color * (1 - s_amt[:, None]) + SEAM * 0.8 * s_amt[:, None]
    # Dirt runs: dark streaks along the bone where grime washed down it.
    runs = fbm(np.column_stack([across[:, 0] * 1.3, across[:, 1] * 1.3, local[:, axis] * 0.05]), (0.7, 0.7, 0.7), seed + 15,
               octaves=1) > 0.66
    color[runs] = color[runs] * 0.72 + SEAM * 0.28
    red_dust = low * 0.5 * (fbm(world, (0.25, 0.25, 0.25), seed + 7) > 0.42)
    color = color * (1 - red_dust[:, None]) + RED[1] * red_dust[:, None]
    # Long cracks running with the bone.
    crack_p = np.column_stack([across[:, 0], across[:, 1], local[:, axis] * 0.08])
    crack = (np.abs(fbm(crack_p, (0.35, 0.35, 0.35), seed + 9, octaves=1) - 0.5) < 0.01) & (
        fbm(world, (0.08, 0.08, 0.08), seed + 10) > 0.5)
    color[crack] = palette[0] * 0.75
    # A few gnaw and pit marks.
    pits = value_noise(world, (0.9, 0.9, 0.9), seed + 11) > 0.955
    color[pits] = palette[1] * 0.8
    return color


def paint_claw(world, local, size):
    """Claw and spur: bone at the root, dark horn toward the tip (the low end), nicked."""
    t, _ = along_axis(local, size)
    tip = 1 - t
    shade = 3.0 - tip * 3.0 + (cell_hash(world, 131) - 0.5) * 1.0
    color = tone(BONE, shade)
    dark = tip > 0.55
    color[dark] = color[dark] * 0.4 + hexes("2A231C")[0] * 0.6
    return color


def paint_tooth(world, local, size):
    t, _ = along_axis(local, size)
    shade = 3.3 + (cell_hash(world, 141) - 0.5) * 1.2 - np.abs(t - 0.5) * 1.4
    color = tone(BONE, shade)
    stain = cell_hash(world, 143) > 0.8
    color[stain] = color[stain] * 0.65 + SEAM * 0.35
    return color


def paint_fur(world, local, size, normal, name):
    """Belly fur: matted, dusty strands hanging down, clumped; caked with canyon dust at the tips."""
    n = len(world)
    strand = np.column_stack([world[:, 0] * 1.0, world[:, 1] * 0.12, world[:, 2] * 1.0])
    s = fbm(strand, (0.8, 0.8, 0.8), 151, octaves=2)
    clump = fbm(world, (0.15, 0.15, 0.15), 153)
    shade = 1.8 + (s - 0.5) * 3.2 + (clump - 0.5) * 1.6
    gap = value_noise(strand, (1.6, 1.0, 1.6), 155) > 0.72
    shade = np.where(gap, shade - 1.5, shade)
    if name.startswith(("tuft_", "thorax_tuft_")):
        shade += 0.4
    color = tone(FUR, shade)
    # Dust caught in the fur, more toward the hanging tips and the underside.
    under = normal[:, 1] < -0.5
    dust_p = fbm(world, (0.4, 0.4, 0.4), 157)
    dusty = (cell_hash(world, 159) > 0.95 - under * 0.04) & (dust_p > 0.55)
    color[dusty] = CREAM[(cell_hash(world[dusty], 161) * 2.99).astype(int)] * 0.85
    # Red canyon dust smeared where it drags along the wall (the belly's underside).
    smear = under & (fbm(world, (0.2, 0.2, 0.2), 163) > 0.55)
    color[smear] = color[smear] * 0.6 + RED[1] * 0.4
    return color


def paint_dead(world, local, size, name):
    """The dead heads: old grey bone, bleached and cracked, dust in every crevice."""
    color = paint_bone(world, local, size, DEAD, 171, grime_floor=0.0)
    moss_free_dirt = fbm(world, (0.35, 0.35, 0.35), 173) > 0.62
    color[moss_free_dirt] = color[moss_free_dirt] * 0.7 + SEAM * 0.3
    return color


def paint_prey(world, local, size):
    """The giants' bones sticking out of him: yellowed, older than his own, broken ends dark with old marrow."""
    color = paint_bone(world, local, size, BONE, 181, grime_floor=0.0)
    color = color * 0.82 + hexes("B08A4A")[0] * 0.18
    t, _ = along_axis(local, size)
    marrow = (t > 0.88) & (cell_hash(world, 183) > 0.3)
    color[marrow] = SEAM * 0.8
    return color


def paint_iron(world, local, size):
    shade = 2.2 + (fbm(world, (0.6, 0.6, 0.6), 191) - 0.5) * 2.4
    color = tone(IRON[:4], shade)
    rust = fbm(world, (0.5, 0.5, 0.5), 193) > 0.55
    color[rust] = IRON[4 + (cell_hash(world[rust], 195) > 0.6).astype(int)]
    return color


def paint_face(world, local, size, normal, fv, name, ancestors, mat, h, w, rng):
    """Colours (n, 3) and glow mask (n,) for the texels at world points."""
    n = len(world)
    glow = np.zeros(n, dtype=bool)
    if mat == "eye":
        color = np.repeat(WHITE[None, :], n, axis=0)
        glow[:] = True
        return color, glow
    if mat == "hole":
        color = np.repeat(BLACK[None, :], n, axis=0)
        rim = cell_hash(world, 201) > 0.8
        color[rim] = SEAM * 0.6
        return color, glow
    if mat == "stone":
        return paint_stone(world, normal, fv, h, w, rng), glow
    if mat == "fur":
        return paint_fur(world, local, size, normal, name), glow
    if mat == "claw":
        return paint_claw(world, local, size), glow
    if mat == "tooth":
        return paint_tooth(world, local, size), glow
    if mat == "prey":
        return paint_prey(world, local, size), glow
    if mat == "dead":
        return paint_dead(world, local, size, name), glow
    if mat == "iron":
        return paint_iron(world, local, size), glow
    # The living head is his own bone: paler and fresher than the legs, stained dark around the brows and sockets.
    if "head_alive" in ancestors:
        color = paint_bone(world, local, size, BONE, 211, grime_floor=0.0)
        if name.startswith(("brow_", "forehead", "temple_", "cheekbone_")):
            stain = fbm(world, (0.5, 0.5, 0.5), 213) > 0.5
            color[stain] = color[stain] * 0.65 + SEAM * 0.35
        return color, glow
    return paint_bone(world, local, size, BONE, 221), glow


# --- atlas ----------------------------------------------------------------------------------------------------------

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

    for fc in faces:
        el = fc["element"]
        name = el["name"]
        ancestors = fc["node"].ancestors()
        mat = material(name, ancestors)
        sw, sh = fc["sw"], fc["sh"]
        jj, ii = np.meshgrid(np.arange(sh), np.arange(sw), indexing="ij")
        uu = ((ii + 0.5) / sw).reshape(-1, 1)
        vv = ((jj + 0.5) / sh).reshape(-1, 1)
        local = fc["corner"] + uu * fc["u"] + vv * fc["v"]
        world = fc["node"].to_world(local)
        normal = np.repeat(fc["node"].dir_to_world(fc["normal"][None, :]), len(world), axis=0)
        size = np.array(el["to"], float) - np.array(el["from"], float)
        rng = random.Random(f"{name}/{el['uuid']}/{fc['face']}")
        color, glow = paint_face(world, local - np.array(el["from"], float), size, normal, vv.reshape(-1), name,
                                 ancestors, mat, sh, sw, rng)
        ys, xs = jj.reshape(-1) + fc["sy"], ii.reshape(-1) + fc["sx"]
        base[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        base[ys, xs, 3] = 255
        glowmap[ys, xs, :3] = np.clip(color, 0, 255).astype(np.uint8)
        glowmap[ys, xs, 3] = np.where(glow, 255, 0)
        fc["uv"] = [fc["sx"], fc["sy"], fc["sx"] + fc["w"], fc["sy"] + fc["h"]]

    for path in (OUT_TEX, OUT_GLOW, OUT_GEO):
        os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(base, "RGBA").save(OUT_TEX, optimize=True)
    Image.fromarray(glowmap, "RGBA").save(OUT_GLOW, optimize=True)

    write_geo(bones, faces, atlas_h)
    write_bbmodel(model, faces, atlas_h)
    print(f"atlas {ATLAS_W}x{atlas_h}, {len(faces)} faces, {len(bones)} bones")


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
        "description": {"identifier": "geometry.the_layered", "texture_width": ATLAS_W, "texture_height": atlas_h,
                        "visible_bounds_width": 11, "visible_bounds_height": 5.5, "visible_bounds_offset": [0, 2.25, 0]},
        "bones": out_bones}]}
    with open(OUT_GEO, "w", encoding="utf-8", newline="\n") as f:
        json.dump(geo, f, indent="\t")
        f.write("\n")


def write_bbmodel(model, faces, atlas_h):
    out = copy.deepcopy(model)
    by_uuid = {e["uuid"]: e for e in out["elements"]}
    for fc in faces:
        by_uuid[fc["element"]["uuid"]]["faces"][fc["face"]]["uv"] = rounded(fc["uv"])
        by_uuid[fc["element"]["uuid"]]["faces"][fc["face"]]["texture"] = 0
    out["name"] = "the_layered"
    out["model_identifier"] = "the_layered"
    out["resolution"] = {"width": ATLAS_W, "height": atlas_h}
    with open(OUT_TEX, "rb") as f:
        data = "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")
    tex = out["textures"][0]
    tex.update({"name": "the_layered.png", "path": "", "relative_path": "", "width": ATLAS_W, "height": atlas_h,
                "uv_width": ATLAS_W, "uv_height": atlas_h, "source": data})
    out["textures"] = [tex]
    with open(OUT_BB, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    sys.exit(main())
