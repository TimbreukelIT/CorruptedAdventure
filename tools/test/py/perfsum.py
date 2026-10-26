"""Table of a performance run: per area the median over the warm rounds of mod/vanilla per step, the spread of the
noise step, the cold round, and optionally the noise of a reference run (an earlier integration).

    python perfsum.py <run dir with p*.json> [--ref <earlier run dir>] [--out table.md]

Ratios < 1 mean the mod is faster than vanilla. Budget noise <= 1.75 per area (waves.md §6).
"""
import argparse
import glob
import json
import os
import statistics

VANILLA = {"minecraft:overworld": "perftest:vanilla", "minecraft:the_nether": "perftest:vanilla_nether"}


def rounds(d):
    out = []
    for f in sorted(glob.glob(os.path.join(d, "p*.json"))):
        with open(f, encoding="utf-8") as fh:
            out.append(json.load(fh))
    return out


def ratios(rs, area, step):
    out = []
    for r in rs:
        s = r["summary"]
        for mod, van in VANILLA.items():
            m, v = s.get("%s|%s|%s" % (area, mod, step)), s.get("%s|%s|%s" % (area, van, step))
            if m and v:
                out.append(m["median"] / v["median"])
    return out


def fmt(xs):
    return ("%.2f" % statistics.median(xs)).replace(".", ",") if xs else "-"


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("dir")
    p.add_argument("--ref")
    p.add_argument("--out")
    a = p.parse_args()
    rs = rounds(a.dir)
    warm = [r for r in rs if not r.get("cold")]
    cold = [r for r in rs if r.get("cold")]
    ref = [r for r in rounds(a.ref) if not r.get("cold")] if a.ref else []
    areas = []
    for r in rs:
        for k in r["summary"]:
            name, dim, step = k.split("|")
            if dim in VANILLA and step == "noise" and name not in areas:
                areas.append(name)
    head = ["Gebied", "noise mediaan (%d warm)" % len(warm), "min-max", "surface", "features",
            "noise koud", "surface koud", "features koud"] + (["noise ref"] if ref else [])
    lines = ["| " + " | ".join(head) + " |", "|" + "---|" * len(head)]
    for ar in areas:
        n = ratios(warm, ar, "noise")
        spread = ("%.2f-%.2f" % (min(n), max(n))).replace(".", ",") if n else "-"
        row = [ar, fmt(n), spread, fmt(ratios(warm, ar, "surface")), fmt(ratios(warm, ar, "features")),
               fmt(ratios(cold, ar, "noise")), fmt(ratios(cold, ar, "surface")), fmt(ratios(cold, ar, "features"))]
        if ref:
            row.append(fmt(ratios(ref, ar, "noise")))
        lines.append("| " + " | ".join(row) + " |")
    for r in rs:
        d = r.get("deopt")
        if d and d["storms"]:
            lines.append("")
            lines.append("**Deopt-storm** in ronde %d: %s." % (r["round"], "; ".join("%d x %s" % (c, s) for c, s in d["storms"])))
    deopts = ["%d: %d" % (r["round"], r["deopt"]["none"]) for r in rs if r.get("deopt")]
    if deopts:
        lines.append("")
        lines.append("Deopts met action none per ronde (een storm is meer dan 10.000 op één plek): %s." % ", ".join(deopts))
    aborted = [str(r["round"]) for r in rs if r.get("aborted")]
    if aborted:
        lines.append("")
        lines.append("Onvolledig (harde stop): ronde %s." % ", ".join(aborted))
    text = "\n".join(lines)
    print(text)
    if a.out:
        with open(a.out, "w", encoding="utf-8") as f:
            f.write(text + "\n")


if __name__ == "__main__":
    main()
