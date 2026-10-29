"""The shadow The Risen casts on the ground as it passes over at its arrival (phase 4 step 2, A3).

Writes textures/entity/the_risen_shadow.png (128 x 64): the phoenix seen from below, wings spread, as a soft black
silhouette with alpha. u runs along the wings (left tip -> right tip), v from the head (top) to the tail (bottom); the
client (client/entity/risen/RisenShadow) maps it onto the block tops under it. A transparent margin keeps the edge clean
when a block's corners sample just outside. Ragged primaries and a split tail, so it reads as ash and torn feathers,
not as a toy bird. Run from the repo root: python tools/textures/risen_shadow.py
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

W, H = 128, 64
S = 8  # supersampling
OUT = os.path.join("src", "main", "resources", "assets", "corruptedadventure", "textures", "entity",
                   "the_risen_shadow.png")


def p(x, y):
    return (x * S, y * S)


def wing(draw, side):
    """One wing from the shoulder to the tip; side = -1 (left) or 1 (right)."""
    cx = W / 2
    def X(dx):
        return cx + side * dx
    # Leading edge: shoulder forward, a bend at the wrist, then swept back to the tip.
    leading = [(4, 24), (16, 20), (30, 19), (42, 21), (52, 25), (58, 29)]
    # Trailing edge with the secondaries as ragged scallops, back to the body.
    trailing = []
    for i in range(10):
        t = i / 9
        dx = 50 - t * 46
        dy = 33 + math.sin(t * math.pi) * 9
        jag = 3.0 if i % 2 == 0 else 0.0
        trailing.append((dx, dy + jag))
    outline = [p(X(dx), dy) for dx, dy in leading] + [p(X(dx), dy) for dx, dy in trailing]
    draw.polygon(outline, fill=255)
    # Primaries: long, separated fingers fanning from the wrist past the tip, frayed at the ends.
    for i, (angle, length, width) in enumerate([(-8, 12, 2.6), (2, 13, 2.4), (12, 12, 2.2), (22, 10, 2.0), (32, 8, 1.8)]):
        rad = math.radians(angle)
        bx, by = 50 - i * 1.5, 27 + i * 1.6
        tx, ty = bx + math.cos(rad) * length, by + math.sin(rad) * length
        nx, ny = -math.sin(rad) * width, math.cos(rad) * width
        finger = [(bx - nx, by - ny), (bx + nx, by + ny), (tx + nx * 0.3, ty + ny * 0.3), (tx + math.cos(rad) * 1.5, ty),
                  (tx - nx * 0.3, ty - ny * 0.3)]
        draw.polygon([p(X(dx), dy) for dx, dy in finger], fill=255)


def body(draw):
    cx = W / 2
    # Gaunt body, the long neck and the skull, the hooked beak.
    draw.ellipse([p(cx - 5, 18), p(cx + 5, 42)], fill=255)
    draw.ellipse([p(cx - 2.5, 8), p(cx + 2.5, 20)], fill=255)
    draw.polygon([p(cx - 3.5, 9), p(cx + 3.5, 9), p(cx, 2)], fill=255)
    # Split, torn tail fan.
    for angle, length in [(-26, 14), (-12, 18), (0, 16), (12, 18), (26, 14)]:
        rad = math.radians(90 + angle)
        tx, ty = cx + math.cos(rad) * length, 40 + math.sin(rad) * length
        nx, ny = -math.sin(rad) * 1.8, math.cos(rad) * 1.8
        draw.polygon([p(cx - nx, 40 - ny), p(cx + nx, 40 + ny), p(tx + nx * 0.4, ty + ny * 0.4),
                      p(tx - nx * 0.4, ty - ny * 0.4)], fill=255)


def main():
    mask = Image.new("L", (W * S, H * S), 0)
    draw = ImageDraw.Draw(mask)
    wing(draw, -1)
    wing(draw, 1)
    body(draw)
    # Soft edge: a shadow cast from high up is blurred; the body stays darker than the wing tips.
    mask = mask.filter(ImageFilter.GaussianBlur(S * 0.8)).resize((W, H), Image.LANCZOS)
    pixels = mask.load()
    cx = W / 2
    for y in range(H):
        for x in range(W):
            if x < 2 or x >= W - 2 or y < 1 or y >= H - 1:
                pixels[x, y] = 0
                continue
            fade = 1.0 - 0.35 * min(1.0, abs(x + 0.5 - cx) / (W / 2))
            pixels[x, y] = int(pixels[x, y] * 0.85 * fade)
    image = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    image.putalpha(mask)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    image.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
