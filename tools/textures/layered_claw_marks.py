"""The Layered's claw marks on a canyon wall (phase 4 step 2, a trace of its roaming).

    python tools/textures/layered_claw_marks.py        (from the repository root)

Writes src/main/resources/assets/corruptedadventure/textures/entity/layered_claw_marks.png: 48 x 48 (3 x 3 blocks at the
16 pixels per block of the stone it lies on, entity/layered/LayeredClawMarks.SIZE). Four deep gouges raked down the
wall, tapering at both ends: a dark core, the torn stone of their walls, a pale rim of fresh stone on the lit upper
side, a shadowed lip below, chips and a little rock dust trickling down from them. Everything else is clear. Hard pixel
alpha (the renderer fades the whole quad); deterministic.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/textures/entity/layered_claw_marks.png")
SIZE = 48

CORE = (34, 22, 16, 255)
DEPTH = (64, 42, 29, 255)
WALL = (98, 66, 45, 255)
RIM = (226, 205, 168, 255)
RIM_DIM = (196, 172, 134, 255)
LIP = (78, 54, 38, 210)
DUST = (214, 192, 156, 120)


def gouge(start, end, bend, width, steps=64):
    """A slightly bent stroke from start to end: points along it and the half width at each (tapered)."""
    (x0, y0), (x1, y1) = start, end
    nx, ny = -(y1 - y0), x1 - x0
    length = math.hypot(nx, ny)
    nx, ny = nx / length, ny / length
    points = []
    for i in range(steps + 1):
        t = i / steps
        sway = math.sin(t * math.pi) * bend
        half = width * (math.sin(t * math.pi) ** 0.55) * (0.85 + 0.15 * math.sin(t * 7.0))
        points.append((x0 + (x1 - x0) * t + nx * sway, y0 + (y1 - y0) * t + ny * sway, half, t))
    return points


def nearest(points, px, py):
    best = (1e9, 0.0, 0.0, 0.0)
    for x, y, half, t in points:
        d = math.hypot(px - x, py - y)
        if d < best[0]:
            best = (d, half, t, (px - x) + (py - y))
    return best


def main():
    rng = random.Random(20261029)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    # Four claws raked down and a little to the left, the middle two deepest; each a little out of line.
    strokes = []
    for i, (offset, depth) in enumerate(((-13, 1.5), (-4, 2.2), (5, 2.1), (13, 1.4))):
        top = (26 + offset + rng.uniform(-1, 1), 5 + abs(offset) * 0.25 + rng.uniform(0, 2))
        bottom = (17 + offset * 0.8 + rng.uniform(-1, 1), 43 - abs(offset) * 0.3 - rng.uniform(0, 2))
        strokes.append(gouge(top, bottom, rng.uniform(-1.5, 1.5), depth))
    for y in range(SIZE):
        for x in range(SIZE):
            cx, cy = x + 0.5, y + 0.5
            for points in strokes:
                d, half, t, side = nearest(points, cx, cy)
                if half <= 0.2:
                    continue
                # A little ragged: the stone breaks unevenly along the edge.
                edge = half + (rng.random() - 0.5) * 0.6
                if d < edge * 0.4:
                    px[x, y] = CORE
                elif d < edge * 0.75:
                    px[x, y] = DEPTH
                elif d < edge:
                    px[x, y] = WALL
                elif d < edge + 1.0:
                    # Lit from above-left: a pale rim of fresh stone there, a shadowed lip on the other side.
                    if side < 0:
                        px[x, y] = RIM if rng.random() < 0.8 else RIM_DIM
                    elif px[x, y][3] == 0:
                        px[x, y] = LIP
                else:
                    continue
                break
    # Chips knocked out round the gouges, and dust trickling down below their ends.
    for points in strokes:
        for _ in range(9):
            x, y, half, t = points[rng.randint(8, len(points) - 9)]
            cx, cy = int(x + rng.uniform(-4, 4)), int(y + rng.uniform(-3, 3))
            if 0 <= cx < SIZE and 0 <= cy < SIZE and px[cx, cy][3] == 0:
                px[cx, cy] = RIM_DIM if rng.random() < 0.6 else LIP
        x, y, _, _ = points[-6]
        for k in range(rng.randint(3, 6)):
            cx, cy = int(x + rng.uniform(-1.5, 1.5)), int(y + 2 + k + rng.uniform(0, 1.5))
            if 0 <= cx < SIZE and 0 <= cy < SIZE and px[cx, cy][3] == 0:
                px[cx, cy] = DUST
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print(f"claw marks -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
