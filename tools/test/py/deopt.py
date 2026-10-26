"""Deoptimization storms in a JFR recording (java/DeoptStorm.java, run with the server's own JDK).

    python deopt.py <jfr.exe of the server's JDK> <recording.jfr>...

A storm: one site with more than STORM deoptimizations of action `none` (C2 compiled a trap that never recompiles,
so every pass drops into the interpreter; 2026-10-26 ring 5: 56 million in Ring5Field.terrain, 692x slower). Normal
runs have at most a few thousand in total.
"""
import os
import subprocess
import sys

STORM = 10000
SOURCE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "java", "DeoptStorm.java")


def java_of(jfr):
    """The java launcher next to a JDK's jfr tool."""
    d, name = os.path.split(jfr)
    return os.path.join(d, "java" + os.path.splitext(name)[1])


def check(jfr, *recordings):
    """{"total": n, "none": n, "sites": [[count, site], ...], "storms": [[count, site], ...]}, with "error" if the
    count failed: never fatal, a perf round or a profile must not be lost to its deopt line."""
    res = {"total": 0, "none": 0, "sites": [], "storms": []}
    try:
        out = subprocess.run([java_of(jfr), SOURCE] + list(recordings), capture_output=True, text=True,
                             check=True).stdout
    except (OSError, subprocess.CalledProcessError) as e:
        res["error"] = str(e)
        return res
    for line in out.splitlines():
        key, _, rest = line.partition(" ")
        if key in ("total", "none"):
            res[key] = int(rest)
        elif key == "site":
            count, _, site = rest.partition(" ")
            res["sites"].append([int(count), site])
    res["storms"] = [s for s in res["sites"] if s[0] > STORM]
    return res


def line(res):
    """One line for a report: the counts, and the storms (or the biggest site of action none)."""
    if "error" in res:
        return "deopt count failed: " + res["error"]
    text = "deopt %d, action none %d" % (res["total"], res["none"])
    if res["storms"]:
        return text + "  STORM: " + "; ".join("%d x %s" % (c, s) for c, s in res["storms"])
    if res["sites"]:
        return text + " (most %d x %s)" % tuple(res["sites"][0])
    return text


if __name__ == "__main__":
    r = check(sys.argv[1], *sys.argv[2:])
    print(line(r))
    sys.exit(1 if r["storms"] else 0)
