"""Icon of the ashen effect (The Risen's ash rain): a heap of ash with flakes still falling on it, 18 x 18.

    python tools/textures/ashen_icon.py              (from the repository root)

Writes src/main/resources/assets/corruptedadventure/textures/mob_effect/ashen.png. Palette: the ash of the plate
docs/design/img/nightmare-ring3-de-herrijzende.png, outlined like the other effect icons.
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/textures/mob_effect/ashen.png")

OUTLINE = (0x1E, 0x19, 0x1B, 255)
ASH = [(0x4B, 0x45, 0x46, 255), (0x5D, 0x56, 0x55, 255), (0x6F, 0x67, 0x63, 255), (0x82, 0x79, 0x73, 255),
       (0x96, 0x8C, 0x84, 255), (0xBD, 0xB4, 0xAB, 255)]
EMBER = (0xFA, 0x88, 0x28, 255)
FLAKES = [(4, 2), (9, 1), (13, 3), (6, 5), (11, 6)]


def main():
    img = Image.new("RGBA", (18, 18))
    px = img.load()
    # The heap: a rounded mound, lit from the upper left.
    for y in range(7, 17):
        for x in range(1, 17):
            dx = (x - 8.5) / 7.5
            dy = (16.5 - y) / 9.0
            if dx * dx + dy * dy <= 1.0 and y >= 16 - int(9 * (1 - dx * dx) ** 0.5):
                light = 0.5 - dx * 0.35 + dy * 0.45 + ((x * 7 + y * 13) % 5 - 2) * 0.06
                px[x, y] = ASH[max(0, min(len(ASH) - 1, int(light * len(ASH))))]
    px[7, 14] = EMBER
    px[11, 15] = EMBER
    for x, y in FLAKES:
        px[x, y] = ASH[5]
    # Outline every coloured pixel's empty neighbours.
    filled = {(x, y) for x in range(18) for y in range(18) if px[x, y][3]}
    for x, y in filled:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < 18 and 0 <= ny < 18 and (nx, ny) not in filled and px[nx, ny][3] == 0:
                px[nx, ny] = OUTLINE
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print(os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
