"""Photo walk: photographs a list of camera poses in the running world of this stream (server + client 1).

    python tools/test/py/walk.py <poses.txt> <out dir> [--only label ...] [--client 1|2] [--max-wait 25]

Pose file, one per line (# comments): `label dim day|night x y z yaw pitch`, dim `ow` or `nether` (or a full id).
Puts the player in spectator, clears the ring weather of rings 2-5, hides the HUD (F1), stops the daylight cycle and
sets the time per pose,
teleports and shoots until two shots in a row match (the studio's stable_shot), then shows the HUD again and puts the
old time back. Start the server with --client and client.sh start first (studio.sh prepare for the fixed FOV).
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.dont_write_bytecode = True  # no __pycache__ next to the studio driver
sys.path.insert(0, os.path.join(ROOT, "tools", "structures"))
sys.path.insert(0, os.path.join(ROOT, "tools", "test", "py"))
import studio  # noqa: E402
from rcon import Rcon  # noqa: E402

DIMS = {"ow": "minecraft:overworld", "nether": "minecraft:the_nether"}


def poses(path):
    out = []
    for line in open(path, encoding="utf-8"):
        line = line.split("#", 1)[0].split()
        if not line:
            continue
        label, dim, daytime, x, y, z, yaw, pitch = line
        out.append((label, DIMS.get(dim, dim), daytime, x, y, z, yaw, pitch))
    return out


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("poses")
    p.add_argument("out")
    p.add_argument("--only", nargs="*")
    p.add_argument("--client", default="1", choices=["1", "2"])
    p.add_argument("--max-wait", type=float, default=25.0)
    a = p.parse_args()
    player = studio.need_running(a.client)
    todo = [q for q in poses(a.poses) if not a.only or any(q[0].startswith(o) for o in a.only)]
    os.makedirs(a.out, exist_ok=True)
    rcon = Rcon()
    rcon.cmd("gamemode spectator " + player)
    rcon.cmd("weather clear")
    for dim in sorted({q[1] for q in todo}):
        for ring in range(2, 6):
            rcon.cmd("execute in %s run ca weather %d clear" % (dim, ring))
    m = re.search(r"(\d+)", rcon.cmd("time query daytime"))
    old_time = int(m.group(1)) if m else None
    # A walk takes longer than a day: the time stands still while it runs.
    rcon.cmd("gamerule doDaylightCycle false")
    studio.run_client("key", "F1", a.client)
    last_dim, last_time, last_xz = None, None, None
    try:
        for label, dim, daytime, x, y, z, yaw, pitch in todo:
            if daytime != last_time:
                rcon.cmd("time set %d" % (6000 if daytime == "day" else 18000))
            rcon.cmd("execute in %s run tp %s %s %s %s %s %s" % (dim, player, x, y, z, yaw, pitch))
            # A new spot has to load and render its chunks; another view of the same spot mostly re-renders.
            far = last_xz is None or dim != last_dim or abs(float(x) - last_xz[0]) + abs(float(z) - last_xz[1]) > 64
            path = os.path.join(a.out, label + ".png")
            ok = studio.stable_shot(path, a.client, 8 if far else 2, a.max_wait)
            print(("shot " if ok else "FAILED ") + label, flush=True)
            last_dim, last_time, last_xz = dim, daytime, (float(x), float(z))
    finally:
        studio.run_client("key", "F1", a.client)
        rcon.cmd("gamerule doDaylightCycle true")
        if old_time is not None:
            rcon.cmd("time set %d" % old_time)


if __name__ == "__main__":
    main()
