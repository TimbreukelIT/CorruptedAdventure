"""Hot methods of a JFR recording (jdk.ExecutionSample on the chunk generation threads `Worker-Main-*`).

    python jfrhot.py <jfr.exe> <rec.jfr> [--top 20] [--step noise] [--json out.json]

Every sample is put under the chunk step its stack runs in (`noise` = NoiseBasedChunkGenerator.doFill, `surface`,
`carvers`, `features`, `biomes`, ...; `truncated` when the stack was cut off before the step's frame - jfr-prof.sh
starts the server with a deep stack depth so this stays rare). Prints the share per step, then for the whole
recording and for --step (default noise): the top self frames (the method on top of the stack) and the inclusive share
of every mod method (`nl.timtoys.*`, counted once per sample however often it recurs in the stack).
As a module: `hot(jfr, rec)` returns a list of samples (step, frame names from the top).
"""
import argparse
import collections
import json
import subprocess

MOD = "nl.timtoys."
STEPS = (("NoiseBasedChunkGenerator.doFill", "noise"), ("ChunkStatusTasks.generateSurface", "surface"),
         ("ChunkStatusTasks.generateCarvers", "carvers"), ("ChunkStatusTasks.generateFeatures", "features"),
         ("ChunkStatusTasks.generateBiomes", "biomes"), ("ChunkStatusTasks.generateStructureStarts", "structures"),
         ("ChunkStatusTasks.initializeLight", "light"), ("ChunkStatusTasks.light", "light"))


def frame_name(f):
    m = f["method"]
    return "%s.%s" % (m["type"]["name"].replace("/", "."), m["name"])


def short(name):
    cls, _, method = name.rpartition(".")
    return "%s.%s" % (cls.rpartition(".")[2], method)


def step_of(names, truncated):
    for n in names:
        s = short(n)
        for key, step in STEPS:
            if s == key:
                return step
    return "truncated" if truncated else "other"


def hot(jfr, rec):
    out = subprocess.run([jfr, "print", "--json", "--stack-depth", "4096", "--events", "jdk.ExecutionSample", rec],
                         capture_output=True, text=True, check=True).stdout
    samples = []
    for ev in json.loads(out)["recording"]["events"]:
        trace = ev["values"].get("stackTrace") or {}
        frames = trace.get("frames") or []
        thread = (ev["values"].get("sampledThread") or {}).get("javaName") or ""
        if not frames or "Worker-Main" not in thread:
            continue
        names = [frame_name(f) for f in frames]
        samples.append((step_of(names, trace.get("truncated")), names))
    return samples


def summary(samples, top):
    n = len(samples)
    self_c, incl_c = collections.Counter(), collections.Counter()
    for _, names in samples:
        self_c[short(names[0])] += 1
        seen = set()
        for name in names:
            if name.startswith(MOD):
                k = short(name)
                if k not in seen:
                    seen.add(k)
                    incl_c[k] += 1
    lines = ["  -- self (top frame)"]
    lines += ["  %5.1f%%  %s" % (100.0 * c / max(n, 1), k) for k, c in self_c.most_common(top)]
    lines.append("  -- inclusive, mod methods")
    lines += ["  %5.1f%%  %s" % (100.0 * c / max(n, 1), k) for k, c in incl_c.most_common(top + 10)]
    return lines, self_c, incl_c


def report(samples, top, step="noise"):
    n = len(samples)
    steps = collections.Counter(s for s, _ in samples)
    lines = ["samples in chunk generation: %d" % n,
             "steps: " + ", ".join("%s %.1f%%" % (s, 100.0 * c / max(n, 1)) for s, c in steps.most_common())]
    lines.append("== all steps")
    lines += summary(samples, top)[0]
    sel = [x for x in samples if x[0] == step]
    lines.append("== step %s (%d samples; shares of this step)" % (step, len(sel)))
    lines += summary(sel, top)[0]
    return "\n".join(lines)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("jfr")
    p.add_argument("rec")
    p.add_argument("--top", type=int, default=20)
    p.add_argument("--step", default="noise")
    p.add_argument("--json")
    a = p.parse_args()
    samples = hot(a.jfr, a.rec)
    print(report(samples, a.top, a.step))
    if a.json:
        _, self_c, incl_c = summary([x for x in samples if x[0] == a.step], a.top)
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump({"samples": len(samples), "step": a.step, "self": self_c.most_common(),
                       "inclusive": incl_c.most_common()}, f, indent=1)


if __name__ == "__main__":
    main()
