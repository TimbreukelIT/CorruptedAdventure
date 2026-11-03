"""The still raindrop of The Unfinished's trace (phase 4 step 2): rain that hangs in mid-air where he passed.

    python tools/textures/still_raindrop.py        (from the repository root)

Writes src/main/resources/assets/corruptedadventure/textures/particle/still_raindrop_0.png and _1.png: 8x8 sprites in the
pale grey of the ring 5 standstill's frozen rain (#C4C8D2), a drop with its tip up (as it was falling) and a short streak
above it. The particle does not tint them.
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/textures/particle")

EDGE = (0x7E, 0x84, 0x92, 255)
BODY = (0xC4, 0xC8, 0xD2, 255)
LIGHT = (0xEE, 0xF0, 0xF5, 255)
STREAK = (0xC4, 0xC8, 0xD2, 120)

# Rows top to bottom; "." empty, "s" faint streak, "e" edge, "b" body, "l" light.
SPRITES = [
    [
        "........",
        "...s....",
        "...s....",
        "...e....",
        "..ebe...",
        "..elb...",
        "..ebe...",
        "...e....",
    ],
    [
        "........",
        "........",
        "...s....",
        "...s....",
        "...e....",
        "..ele...",
        "..ebe...",
        "...e....",
    ],
]

COLOURS = {"s": STREAK, "e": EDGE, "b": BODY, "l": LIGHT}


def main():
    for index, rows in enumerate(SPRITES):
        image = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, cell in enumerate(row):
                if cell in COLOURS:
                    image.putpixel((x, y), COLOURS[cell])
        image.save(os.path.join(OUT, "still_raindrop_%d.png" % index))


if __name__ == "__main__":
    main()
