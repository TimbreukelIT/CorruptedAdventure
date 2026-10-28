"""Preview renders of The Lost Keeper, posed along a curve the way the game poses it (no Blockbench needed).

    python tools/textures/lost_keeper_preview.py [out.png] [crown_branches_left] [animation] [seconds]

Builds the model with lost_keeper_model.py, lays the segment bones along an S-curve with the same rule as the client
(LostKeeperModel: each segment's pivot on its chain point, its +z along the chain), and rasterises the textured cubes
(base with alpha cut-out, the translucent membrane pass, the emissive glow mask unlit) from two angles side by side.
Default output: build/test/shots/r4-preview.png. Development tool only; the game does not use it.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import lost_keeper_model as m  # noqa: E402

W, H = 900, 760


def catmull(points, samples=60):
    pts = [np.array(p, float) for p in points]
    pts = [pts[0] * 2 - pts[1]] + pts + [pts[-1] * 2 - pts[-2]]
    out = []
    for i in range(1, len(pts) - 2):
        p0, p1, p2, p3 = pts[i - 1], pts[i], pts[i + 1], pts[i + 2]
        for t in np.linspace(0, 1, samples, endpoint=False):
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(pts[-2])
    return np.array(out)


def at_length(curve, s):
    seg = np.linalg.norm(np.diff(curve, axis=0), axis=1)
    cum = np.concatenate([[0], np.cumsum(seg)])
    s = min(s, cum[-1])
    i = max(0, min(len(seg) - 1, np.searchsorted(cum, s) - 1))
    f = (s - cum[i]) / max(seg[i], 1e-9)
    return curve[i] + (curve[i + 1] - curve[i]) * f


def segment_pose(front, back):
    """The client's rule: Ry(yaw) @ Rx(pitch) turns +z onto the chain direction (Rz = 0)."""
    d = back - front
    d = d / np.linalg.norm(d)
    yaw = math.atan2(d[0], d[2])
    pitch = math.asin(max(-1.0, min(1.0, -d[1])))
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return ry @ rx


ANIM_FILE = os.path.join(m.ASSETS, "animations/entity/the_lost_keeper.animation.json")


def sample(channel, t):
    """Linear keyframe value of a Bedrock channel at time t."""
    times = sorted(channel, key=float)
    values = [channel[k] for k in times]
    ts = [float(k) for k in times]
    if t <= ts[0]:
        return np.array(values[0], float)
    for i in range(1, len(ts)):
        if t <= ts[i]:
            f = (t - ts[i - 1]) / max(ts[i] - ts[i - 1], 1e-9)
            return np.array(values[i - 1], float) * (1 - f) + np.array(values[i], float) * f
    return np.array(values[-1], float)


def animation_offsets(name, t):
    """Bone -> (rotation offset in Blockbench degrees, scale) of an animation at time t. The file's x and y rotations
    are negated on load (Bedrock), so they are negated back here."""
    if not name:
        return {}
    import json
    with open(ANIM_FILE, encoding="utf-8") as f:
        anim = json.load(f)["animations"]["animation.the_lost_keeper." + name]
    out = {}
    for bone, ch in anim["bones"].items():
        rot = sample(ch["rotation"], t) if "rotation" in ch else np.zeros(3)
        scale = sample(ch["scale"], t) if "scale" in ch else np.ones(3)
        out[bone] = (np.array([-rot[0], -rot[1], rot[2]]), scale)
    return out


def posed_nodes(b, curve, anim=None, time=0.0):
    """Group name -> function(points) to world, with the segment bones placed on the curve."""
    rest = m.nodes_of(b)
    offsets = animation_offsets(anim, time)
    chain = [at_length(curve, m.NECK_Z + i * m.SEG_LEN) for i in range(m.SEGMENTS + 1)]
    head_pos = curve[0]
    posed = {}

    def transform(g):
        if g.name in posed:
            return posed[g.name]
        if g.name.startswith("seg_"):
            i = int(g.name[4:]) - 1
            rot = segment_pose(chain[i], chain[i + 1])
            pivot = np.array(g.origin)
            target = chain[i]
            fn = (lambda rot, pivot, target: lambda p: (p - pivot) @ rot.T + target)(rot, pivot, target)
        elif g.name == "root":
            offset = head_pos - np.array([0, m.AXIS_Y, 0])
            fn = lambda p, offset=offset: p + offset  # noqa: E731
        else:
            node = rest[g.name]
            parent = transform(g.parent)
            rot, scale = node.rot, np.ones(3)
            if g.name in offsets:
                extra, scale = offsets[g.name]
                rot = m.rotation(np.array(g.rotation) + extra)
            fn = (lambda rot, scale, origin, parent: lambda p: parent(((p - origin) * scale) @ rot.T + origin))(
                rot, scale, node.origin, parent)
        posed[g.name] = fn
        return fn

    for g in b.groups:
        transform(g)
    return posed


def render(b, faces_by_el, textures, posed, view_yaw, view_pitch, hidden):
    base, membrane, glow = textures
    th, tw = base.shape[:2]
    color = np.zeros((H, W, 3))
    color[:] = (26, 25, 31)
    depth = np.full((H, W), -1e9)
    cy, sy = math.cos(math.radians(view_yaw)), math.sin(math.radians(view_yaw))
    cp, sp = math.cos(math.radians(view_pitch)), math.sin(math.radians(view_pitch))
    view = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    light = np.array([0.4, 0.8, -0.45])
    light /= np.linalg.norm(light)
    tris = []
    for g in b.groups:
        if any(g.name == h or ancestor_named(g, h) for h in hidden):
            continue
        fn = posed[g.name]
        for el in g.elements:
            e_rot = m.rotation(el["rotation"])
            eo = np.array(el["origin"])
            for fc in faces_by_el[el["uuid"]]:
                c, u, v = fc["corner"], fc["u"], fc["v"]
                quad = np.array([c, c + u, c + u + v, c + v])
                quad = (quad - eo) @ e_rot.T + eo
                world = fn(quad)
                uv = np.array([[0, 0], [1, 0], [1, 1], [0, 1]], float)
                sx, sy_, fw, fh = fc["sx"], fc["sy"], fc["w"], fc["h"]
                atlas = np.stack([sx + uv[:, 0] * fw, sy_ + uv[:, 1] * fh], axis=1)
                for idx in ((0, 1, 2), (0, 2, 3)):
                    tris.append((world[list(idx)], atlas[list(idx)], el["info"].get("mat") == "membrane"))
    allp = np.concatenate([t[0] for t in tris]) @ view.T
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    scale = min((W - 60) / (hi[0] - lo[0]), (H - 60) / (hi[1] - lo[1]))
    centre = (lo + hi) / 2

    def project(p):
        q = p @ view.T
        return np.stack([(q[:, 0] - centre[0]) * scale + W / 2, H / 2 - (q[:, 1] - centre[1]) * scale, q[:, 2]], axis=1)

    def raster(tri, atlas, mode):
        pr = project(tri)
        n = np.cross(tri[1] - tri[0], tri[2] - tri[0])
        if np.linalg.norm(n) < 1e-9:
            return
        n = n / np.linalg.norm(n)
        shade = 0.55 + 0.45 * abs(float(n @ light))
        x0, x1 = int(max(0, np.floor(pr[:, 0].min()))), int(min(W - 1, np.ceil(pr[:, 0].max())))
        y0, y1 = int(max(0, np.floor(pr[:, 1].min()))), int(min(H - 1, np.ceil(pr[:, 1].max())))
        if x1 < x0 or y1 < y0:
            return
        xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        (ax, ay, az), (bx, by, bz), (cx, cy_, cz) = pr
        den = (by - cy_) * (ax - cx) + (cx - bx) * (ay - cy_)
        if abs(den) < 1e-9:
            return
        w0 = ((by - cy_) * (xs - cx) + (cx - bx) * (ys - cy_)) / den
        w1 = ((cy_ - ay) * (xs - cx) + (ax - cx) * (ys - cy_)) / den
        w2 = 1 - w0 - w1
        inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
        if not inside.any():
            return
        z = w0 * az + w1 * bz + w2 * cz
        tu = w0 * atlas[0, 0] + w1 * atlas[1, 0] + w2 * atlas[2, 0]
        tv = w0 * atlas[0, 1] + w1 * atlas[1, 1] + w2 * atlas[2, 1]
        iu = np.clip(tu.astype(int), 0, tw - 1)
        iv = np.clip(tv.astype(int), 0, th - 1)
        py, px = ys.astype(int), xs.astype(int)
        if mode == "opaque":
            tex = base[iv, iu]
            ok = inside & (tex[..., 3] > 0) & (z > depth[py, px])
            g = glow[iv, iu]
            lit = tex[..., :3] * shade
            lit = np.where((g[..., 3] > 0)[..., None], np.maximum(g[..., :3], lit), lit)
            color[py[ok], px[ok]] = lit[ok]
            depth[py[ok], px[ok]] = z[ok]
        else:
            tex = membrane[iv, iu]
            ok = inside & (tex[..., 3] > 0) & (z > depth[py, px])
            a = (tex[..., 3:4] / 255.0)
            mixed = color[py, px] * (1 - a) + tex[..., :3] * shade * a
            color[py[ok], px[ok]] = mixed[ok]

    for tri, atlas, is_mem in tris:
        if not is_mem:
            raster(tri, atlas, "opaque")
    mem = [t for t in tris if t[2]]
    mem.sort(key=lambda t: float((t[0].mean(axis=0) @ view.T)[2]))
    for tri, atlas, _ in mem:
        raster(tri, atlas, "membrane")
    return np.clip(color, 0, 255).astype(np.uint8)


def ancestor_named(g, name):
    p = g.parent
    while p:
        if p.name == name:
            return True
        p = p.parent
    return False


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(m.ROOT, "build/test/shots/r4-preview.png")
    crown_left = int(sys.argv[2]) if len(sys.argv) > 2 else 6
    anim = sys.argv[3] if len(sys.argv) > 3 else None
    time = float(sys.argv[4]) if len(sys.argv) > 4 else 0.0
    b = m.build()
    nodes = m.nodes_of(b)
    faces = []
    for g in b.groups:
        for el in g.elements:
            for face in ("north", "east", "south", "west", "up", "down"):
                corner, uax, vax, nrm = m.face_frame(face, el["from"], el["to"])
                w, h = np.linalg.norm(uax), np.linalg.norm(vax)
                faces.append({"element": el, "face": face, "corner": np.array(corner, float), "u": np.array(uax, float),
                              "v": np.array(vax, float), "w": w, "h": h, "sw": max(1, int(math.ceil(w - 1e-6))),
                              "sh": max(1, int(math.ceil(h - 1e-6)))})
    m.pack(faces)
    by_el = {}
    for fc in faces:
        by_el.setdefault(fc["element"]["uuid"], []).append(fc)
    textures = [np.array(Image.open(p).convert("RGBA")).astype(np.float64) for p in (m.OUT_TEX, m.OUT_MEMBRANE, m.OUT_GLOW)]
    # An S-curve like the concept plate: the head high on the right, the body looping down and back.
    curve = catmull([(0, 150, 0), (0, 150, 30), (6, 128, 70), (12, 90, 82), (16, 56, 58), (12, 36, 14), (4, 30, -30),
                     (-4, 46, -80), (-8, 78, -110)])
    posed = posed_nodes(b, curve, anim, time)
    hidden = [f"coral_{k:02d}" for k in range(crown_left + 1, 7)]
    left = render(b, by_el, textures, posed, -90, 8, hidden)
    right = render(b, by_el, textures, posed, -135, 18, hidden)
    img = np.concatenate([left, right], axis=1)
    # Close-ups of the head from the front quarter: the crown whole, and with only two branches left.
    head = {"neck_mount", "seg_01"} | {g.name for g in b.groups if ancestor_named(g, "neck_mount") or ancestor_named(g, "seg_01")}
    body = [g.name for g in b.groups if g.name.startswith("seg_") and g.name not in head]
    close = render(b, by_el, textures, posed, -150, 12, hidden + body)
    broken = render(b, by_el, textures, posed, -150, 12, [f"coral_{k:02d}" for k in range(3, 7)] + body)
    img = np.concatenate([img, np.concatenate([close, broken], axis=1)], axis=0)
    # A close-up of the middle of the body, where the rot comes in.
    middle = [g.name for g in b.groups if g.name.startswith("seg_") and not 3 <= int(g.name[4:]) <= 8]
    mid = render(b, by_el, textures, posed, -90, 8, hidden + middle + ["neck_mount"])
    img = np.concatenate([img, np.concatenate([mid, render(b, by_el, textures, posed, 90, 8, hidden + middle + ["neck_mount"])], axis=1)], axis=0)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    Image.fromarray(img).save(out)
    print(out)


if __name__ == "__main__":
    main()
