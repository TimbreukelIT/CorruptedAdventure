"""Hot methods of a JFR recording (jdk.ExecutionSample on the chunk generation threads `Worker-Main-*`).

    python jfrhot.py <jfr.exe> <rec.jfr> [--top 20] [--step noise] [--callees Class.method] [--json out.json]

Every sample is put under the chunk step its stack runs in (`noise` = NoiseBasedChunkGenerator.doFill, `surface`,
`carvers`, `features`, `biomes`, ...; `truncated` when the stack was cut off before the step's frame - jfr-prof.sh
starts the server with a deep stack depth so this stays rare). Prints the share per step, then for the whole
recording and for --step (default noise): the top self frames (the method on top of the stack) and the inclusive share
of every mod method (`nl.timtoys.*`, counted once per sample however often it recurs in the stack). --callees shows,
within --step, what a method (short name, e.g. IslandField.lowestAround) spends its samples on, per callee and line.
Reads `jfr print` as text, streamed: the JSON form of execution samples runs to gigabytes.
As a module: `hot(jfr, rec)` returns a list of samples (step, [(frame name, line)] from the top).
"""
import argparse
import collections
import json
import re
import subprocess

MOD = "nl.timtoys."
STEPS = (("NoiseBasedChunkGenerator.doFill", "noise"), ("ChunkStatusTasks.generateSurface", "surface"),
         ("ChunkStatusTasks.generateCarvers", "carvers"), ("ChunkStatusTasks.generateFeatures", "features"),
         ("ChunkStatusTasks.generateBiomes", "biomes"), ("ChunkStatusTasks.generateStructureStarts", "structures"),
         ("ChunkStatusTasks.initializeLight", "light"), ("ChunkStatusTasks.light", "light"))
FRAME = re.compile(r"^\s+([\w.$/<>]+)\(.*\)\s+line:\s*(-?\d+)")


def short(name):
    cls, _, method = name.rpartition(".")
    return "%s.%s" % (cls.rpartition(".")[2], method)


def step_of(frames, truncated):
    for name, _ in frames:
        s = short(name)
        for key, step in STEPS:
            if s == key:
                return step
    return "truncated" if truncated else "other"


def hot(jfr, rec):
    proc = subprocess.Popen([jfr, "print", "--stack-depth", "4096", "--events", "jdk.ExecutionSample", rec],
                            stdout=subprocess.PIPE, text=True, encoding="utf-8", errors="replace")
    samples, frames, thread, truncated, in_stack = [], [], "", False, False
    for line in proc.stdout:
        if line.startswith("jdk.ExecutionSample"):
            frames, thread, truncated, in_stack = [], "", False, False
        elif "sampledThread = " in line:
            thread = line
        elif "stackTrace = [" in line:
            in_stack = True
        elif in_stack:
            m = FRAME.match(line)
            if m:
                frames.append((m.group(1), int(m.group(2))))
            elif line.strip() == "...":
                truncated = True
            elif line.strip().startswith("]"):
                in_stack = False
                if frames and "Worker-Main" in thread:
                    samples.append((step_of(frames, truncated), frames))
    if proc.wait() != 0:
        raise SystemExit("jfr print failed on %s" % rec)
    return samples


def summary(samples, top):
    n = len(samples)
    self_c, incl_c = collections.Counter(), collections.Counter()
    for _, frames in samples:
        self_c[short(frames[0][0])] += 1
        seen = set()
        for name, _ in frames:
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


def callees(samples, method, top):
    """What `method` (short name) spends its samples on: per callee frame (with the calling line), outermost call."""
    n = len(samples)
    c, total = collections.Counter(), 0
    for _, frames in samples:
        idx = [i for i, (name, _) in enumerate(frames) if short(name) == method]
        if not idx:
            continue
        total += 1
        i = idx[-1]
        c["<self>" if i == 0 else "%s (line %d)" % (short(frames[i - 1][0]), frames[i][1])] += 1
    lines = ["== %s: %.1f%% inclusive" % (method, 100.0 * total / max(n, 1))]
    lines += ["  %5.1f%%  %s" % (100.0 * v / max(n, 1), k) for k, v in c.most_common(top)]
    return lines


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
    p.add_argument("--callees", nargs="*", default=[])
    p.add_argument("--json")
    a = p.parse_args()
    samples = hot(a.jfr, a.rec)
    print(report(samples, a.top, a.step))
    sel = [x for x in samples if x[0] == a.step]
    for method in a.callees:
        print("\n".join(callees(sel, method, a.top)))
    if a.json:
        _, self_c, incl_c = summary(sel, a.top)
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump({"samples": len(samples), "step": a.step, "self": self_c.most_common(),
                       "inclusive": incl_c.most_common()}, f, indent=1)


if __name__ == "__main__":
    main()
