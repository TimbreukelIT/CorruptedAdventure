"""CPU profile of chunk generation per area on the running server (used by jfr-prof.sh).

    python jfrprof.py <out dir> --run <server run dir> --jfr <jfr.exe> [--areas file] [--only name ...] [--nowarm]
                      [--top 20]

Uses the server's own profiler (RCON `jfr start` / `jfr stop`, Minecraft's flightrecorder-config.jfc samples Java
stacks every 10 ms), so nothing has to attach to the JVM. Warm-up like perf.py (unless --nowarm), then per area one
recording: `jfr start`, forceload the area (the command returns once the chunks are generated), `jfr stop`. The
recording goes to <out>/<area>.jfr, the hot methods (py/jfrhot.py: per step, and the noise step alone) to
<out>/<area>.txt. Only the mod's dimension is loaded (no vanilla comparison: perf.sh does the timing).
"""
import argparse
import glob
import os
import shutil
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import areas  # noqa: E402
import jfrhot  # noqa: E402
from perf import TOOLS, forceload, warm_spots  # noqa: E402
from rcon import Rcon  # noqa: E402


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("out")
    p.add_argument("--run", required=True)
    p.add_argument("--jfr", required=True)
    p.add_argument("--areas", default=os.path.join(TOOLS, "areas", "perf.txt"))
    p.add_argument("--only", nargs="*")
    p.add_argument("--nowarm", action="store_true")
    p.add_argument("--top", type=int, default=20)
    a = p.parse_args()
    os.makedirs(a.out, exist_ok=True)
    todo = areas.load(a.areas, None, a.only)
    r = Rcon(timeout=1800)
    if not a.nowarm:
        for key, spots in warm_spots(a.run).items():
            for (x, z) in spots:
                forceload(r, areas.dim(key).id, x, z, 128)
        print("warm-up done", flush=True)
    debug = os.path.join(a.run, "debug")
    for ar in todo:
        before = set(glob.glob(os.path.join(debug, "*.jfr")))
        r.cmd("jfr start")
        time.sleep(2)
        t0 = time.time()
        forceload(r, ar.dim.id, ar.cx0 * 16, ar.cz0 * 16, (ar.cx1 - ar.cx0 + 1) * 16)
        took = time.time() - t0
        time.sleep(2)
        r.cmd("jfr stop")
        new = []
        for _ in range(30):
            time.sleep(1)
            new = sorted(set(glob.glob(os.path.join(debug, "*.jfr"))) - before, key=os.path.getmtime)
            if new:
                break
        if not new:
            raise SystemExit("no new JFR recording in %s for %s" % (debug, ar.name))
        time.sleep(2)
        rec = os.path.join(a.out, ar.name + ".jfr")
        shutil.move(new[-1], rec)
        text = "%s: %.1f s\n%s" % (ar.name, took, jfrhot.report(jfrhot.hot(a.jfr, rec), a.top))
        with open(os.path.join(a.out, ar.name + ".txt"), "w", encoding="utf-8") as f:
            f.write(text + "\n")
        print(text, flush=True)


if __name__ == "__main__":
    main()
