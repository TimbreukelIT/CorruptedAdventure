"""Contact sheet of screenshots: python sheet.py <dir-or-files...> <out.png> [--cols 4] [--width 427]
Every image scaled to width x width*9/16 with its file name above it."""
import argparse
import glob
import os

from PIL import Image, ImageDraw


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("inputs", nargs="+")
    p.add_argument("out")
    p.add_argument("--cols", type=int, default=4)
    p.add_argument("--width", type=int, default=427)
    a = p.parse_args()
    files = []
    for i in a.inputs:
        files += sorted(glob.glob(os.path.join(i, "*.png"))) if os.path.isdir(i) else [i]
    w, h = a.width, a.width * 9 // 16
    rows = (len(files) + a.cols - 1) // a.cols
    sheet = Image.new("RGB", (a.cols * w, max(1, rows) * (h + 14)), "black")
    d = ImageDraw.Draw(sheet)
    for i, f in enumerate(files):
        x, y = i % a.cols * w, i // a.cols * (h + 14)
        sheet.paste(Image.open(f).convert("RGB").resize((w, h)), (x, y + 14))
        d.text((x + 2, y), os.path.splitext(os.path.basename(f))[0], fill="white")
    sheet.save(a.out)
    print(a.out, len(files))


if __name__ == "__main__":
    main()
