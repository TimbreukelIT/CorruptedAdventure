"""One performance round (JFR `minecraft.ChunkGeneration`) on the running server: the mod's dimensions against the
vanilla datapack dimensions (perfpack/: perftest:vanilla, perftest:vanilla_nether) at the same chunks, in one server.
The order mod/vanilla alternates per area and round.

    python perf.py <round> <out.json> --run <server run dir> --jfr <jfr.exe> [--areas file] [--only name ...]
                   [--nowarm] [--deadline <epoch seconds>]

Warm-up (unless --nowarm, the cold round): 8 x 8 chunks in the middle of rings 1-3 and on the boundaries 1|2, 2|3,
3|4 in every dimension (radii from the server's config), so the JIT has seen the rings and zones before timing. Per area and step (noise, surface, features) the median duration in ms is
written to out.json ("summary": {"area|dimension|step": {"n", "median"}}). Past --deadline no new area starts; the
recording is still stopped and parsed, and the result is marked "aborted".
"""
import argparse
import collections
import glob
import json
import math
import os
import re
import statistics
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import deopt  # noqa: E402
from rcon import Rcon  # noqa: E402

VANILLA = {"ow": "perftest:vanilla", "nether": "perftest:vanilla_nether"}
STEPS = ("noise", "surface", "features")
DEFAULT_RADII = [1500, 3500, 6000, 8000]  # ring 1-4 outer radius, overworld blocks (58 defaults)


def warm_spots(run):
    """Warm-up positions (x, z) per dimension: the middle of rings 1-3 and the boundaries 1|2, 2|3, 3|4, from the ring
    radii in <run>/config/corruptedadventure-common.toml (the Nether uses them divided by 4)."""
    radii = list(DEFAULT_RADII)
    cfg = os.path.join(run, "config", "corruptedadventure-common.toml")
    if os.path.exists(cfg):
        for line in open(cfg, encoding="utf-8"):
            k, _, v = line.partition("=")
            k = k.strip()
            if k.startswith("ring") and k.endswith("OuterRadius") and k[4:-11].isdigit() and int(k[4:-11]) <= len(radii):
                radii[int(k[4:-11]) - 1] = int(float(v))
    r = [0] + radii
    dists = [(r[0] + r[1]) / 2, r[1], (r[1] + r[2]) / 2, r[2], (r[2] + r[3]) / 2, r[3]]
    out = {}
    for key, scale in (("ow", 1), ("nether", 4)):
        spots = []
        for i, d in enumerate(dists):
            ang = math.radians(37 + 67 * i)  # spread over the compass, away from the axes
            spots.append((int(d / scale * math.cos(ang)) - 64, int(d / scale * math.sin(ang)) - 64))
        out[key] = spots
    return out


TOOLS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def forceload(r, dim, x, z, size):
    box = "%d %d %d %d" % (x, z, x + size - 1, z + size - 1)
    r.cmd("execute in %s run forceload add %s" % (dim, box))
    r.cmd("execute in %s run forceload remove %s" % (dim, box))


def seconds(duration):
    """An ISO-8601 duration of JFR's JSON ("PT0.0123S", "PT1M18.2S" for a chunk step that waited over a minute)."""
    m = re.fullmatch(r"PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?", duration)
    h, mi, sec = m.groups()
    return int(h or 0) * 3600 + int(mi or 0) * 60 + float(sec or 0)


def parse(jfr, path, measured):
    res = subprocess.run([jfr, "print", "--json", "--events", "minecraft.ChunkGeneration", path],
                         capture_output=True, text=True, check=True).stdout
    times = collections.defaultdict(list)
    for ev in json.loads(res)["recording"]["events"]:
        v = ev["values"]
        step = v["status"].split(":")[-1]
        if step not in STEPS:
            continue
        level = v["level"].split(" / ")[-1].rstrip("]")
        for a in measured:
            if level in (a.dim.id, VANILLA[a.dim.key]) and a.cx0 <= v["chunkPosX"] <= a.cx1 and a.cz0 <= v["chunkPosZ"] <= a.cz1:
                times["%s|%s|%s" % (a.name, level, step)].append(seconds(v["duration"]) * 1000.0)
    return {k: {"n": len(t), "median": statistics.median(t)} for k, t in times.items()}


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("round", type=int)
    p.add_argument("out")
    p.add_argument("--run", required=True)
    p.add_argument("--jfr", required=True)
    p.add_argument("--areas", default=os.path.join(TOOLS, "areas", "perf.txt"))
    p.add_argument("--only", nargs="*")
    p.add_argument("--nowarm", action="store_true")
    p.add_argument("--deadline", type=float, default=0)
    a = p.parse_args()
    todo = areas.load(a.areas, None, a.only)
    r = Rcon(timeout=1800)
    if not a.nowarm:
        for key, spots in warm_spots(a.run).items():
            for dim in (areas.dim(key).id, VANILLA[key]):
                for (x, z) in spots:
                    forceload(r, dim, x, z, 128)
        print("round %d warm-up done" % a.round, flush=True)
    r.cmd("save-all flush")
    time.sleep(10)
    before = set(glob.glob(os.path.join(a.run, "debug", "*.jfr")))
    r.cmd("jfr start")
    time.sleep(2)
    measured, aborted = [], False
    for i, ar in enumerate(todo):
        if a.deadline and time.time() > a.deadline:
            aborted = True
            print("round %d: deadline reached, %d of %d areas measured" % (a.round, len(measured), len(todo)), flush=True)
            break
        dims = (ar.dim.id, VANILLA[ar.dim.key])
        for d in (dims if (i + a.round) % 2 == 0 else dims[::-1]):
            t0 = time.time()
            forceload(r, d, ar.cx0 * 16, ar.cz0 * 16, (ar.cx1 - ar.cx0 + 1) * 16)
            print("round %d %-10s %-24s %.1fs" % (a.round, ar.name, d, time.time() - t0), flush=True)
            time.sleep(3)
        measured.append(ar)
    r.cmd("jfr stop")
    time.sleep(5)
    new = sorted(set(glob.glob(os.path.join(a.run, "debug", "*.jfr"))) - before, key=os.path.getmtime)
    if not new:
        raise SystemExit("no new JFR recording in %s/debug" % a.run)
    summary = parse(a.jfr, new[-1], measured)
    storms = deopt.check(a.jfr, new[-1])
    with open(a.out, "w", encoding="utf-8") as f:
        json.dump({"round": a.round, "cold": a.nowarm, "aborted": aborted, "jfr": new[-1], "summary": summary,
                   "deopt": storms}, f, indent=1)
    print("R%d %s" % (a.round, deopt.line(storms)), flush=True)
    for ar in measured:
        cells = []
        for step in STEPS:
            m = summary.get("%s|%s|%s" % (ar.name, ar.dim.id, step))
            v = summary.get("%s|%s|%s" % (ar.name, VANILLA[ar.dim.key], step))
            cells.append("%s %s" % (step, ("%.2f/%.2f=%.2f" % (m["median"], v["median"], m["median"] / v["median"])) if m and v else "-"))
        print("R%d %-10s %s" % (a.round, ar.name, "  ".join(cells)), flush=True)


if __name__ == "__main__":
    main()
