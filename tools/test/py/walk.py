"""Photo walk: photographs a list of camera poses in the running world of this stream (server + client 1).

    python tools/test/py/walk.py <poses.txt> <out dir> [--only label ...] [--client 1|2] [--max-wait 25]

Pose file, one per line (# comments): `label dim day|night x y z yaw pitch`, dim `ow` or `nether` (or a full id).
Puts the player in spectator, clears the ring weather of rings 2-5, hides the HUD (F1), stops the daylight cycle and
sets the time per pose,
teleports and shoots until two shots in a row match (the studio's stable_shot), then puts everything back as it was:
the HUD, the player's game mode, the daylight cycle's game rule and the full day time (day count included). Start the server with --client and client.sh start first (studio.sh prepare for the fixed FOV).

--light R (caves, undersides, the Nether): after the teleport puts invisible light blocks (level 15) on a grid of 4 in
the air within R around the camera (R/2 up and down; `setblock ... keep`, so only air and cave air). They stay: use it
on a copy of a world (cp -r run/<level> run/<level>-light), never on one that is scanned or compared later.
"""
import argparse
import os
import re
import sys
import time

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


def light(rcon, dim, x, y, z, radius, wait):
    """Light blocks on a grid of 4 in the air around (x, y, z), once the chunks there are loaded."""
    bx, by, bz = int(x // 1), int(y // 1) + 1, int(z // 1)
    for _ in range(30):
        if "passed" in rcon.cmd("execute in %s if loaded %d %d %d" % (dim, bx, by, bz)).lower():
            break
        time.sleep(1)
    time.sleep(wait)
    for dx in range(-radius, radius + 1, 4):
        for dy in range(-(radius // 2), radius // 2 + 1, 4):
            for dz in range(-radius, radius + 1, 4):
                rcon.cmd("execute in %s run setblock %d %d %d minecraft:light[level=15] keep" % (dim, bx + dx, by + dy, bz + dz))


GAMEMODES = ("survival", "creative", "adventure", "spectator")


def gamemode(rcon, player):
    """The player's game mode, or None."""
    for mode in GAMEMODES:
        if "passed" in rcon.cmd("execute if entity @a[name=%s,gamemode=%s]" % (player, mode)).lower():
            return mode
    return None


def number(text):
    m = re.search(r"(-?\d+)\s*$", text.strip())
    return int(m.group(1)) if m else None


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("poses")
    p.add_argument("out")
    p.add_argument("--only", nargs="*")
    p.add_argument("--client", default="1", choices=["1", "2"])
    p.add_argument("--max-wait", type=float, default=25.0)
    p.add_argument("--light", type=int, default=0, metavar="R")
    a = p.parse_args()
    player = studio.need_running(a.client)
    todo = [q for q in poses(a.poses) if not a.only or any(q[0].startswith(o) for o in a.only)]
    os.makedirs(a.out, exist_ok=True)
    rcon = Rcon()
    old_mode = gamemode(rcon, player)
    old_cycle = rcon.cmd("gamerule doDaylightCycle").strip().lower().endswith("true")
    # `time set` sets the full day time: the day count (`time query day`) and the time of day together.
    day, daytime = number(rcon.cmd("time query day")), number(rcon.cmd("time query daytime"))
    old_time = day * 24000 + daytime if day is not None and daytime is not None else None
    hud_hidden = False
    last_dim, last_time, last_xz = None, None, None
    try:
        rcon.cmd("gamemode spectator " + player)
        rcon.cmd("weather clear")
        for dim in sorted({q[1] for q in todo}):
            for ring in range(2, 6):
                rcon.cmd("execute in %s run ca weather %d clear" % (dim, ring))
        # A walk takes longer than a day: the time stands still while it runs.
        rcon.cmd("gamerule doDaylightCycle false")
        studio.run_client("key", "F1", a.client)
        hud_hidden = True
        for label, dim, daytime, x, y, z, yaw, pitch in todo:
            if daytime != last_time:
                rcon.cmd("time set %d" % (6000 if daytime == "day" else 18000))
            rcon.cmd("execute in %s run tp %s %s %s %s %s %s" % (dim, player, x, y, z, yaw, pitch))
            # A new spot has to load and render its chunks; another view of the same spot mostly re-renders.
            far = last_xz is None or dim != last_dim or abs(float(x) - last_xz[0]) + abs(float(z) - last_xz[1]) > 64
            if a.light:
                light(rcon, dim, float(x), float(y), float(z), a.light, 5 if far else 1)
            path = os.path.join(a.out, label + ".png")
            ok = studio.stable_shot(path, a.client, 8 if far else 2, a.max_wait)
            print(("shot " if ok else "FAILED ") + label, flush=True)
            last_dim, last_time, last_xz = dim, daytime, (float(x), float(z))
    finally:
        if hud_hidden:
            studio.run_client("key", "F1", a.client)
        rcon.cmd("gamerule doDaylightCycle %s" % ("true" if old_cycle else "false"))
        if old_time is not None:
            rcon.cmd("time set %d" % old_time)
        if old_mode is not None:
            rcon.cmd("gamemode %s %s" % (old_mode, player))


if __name__ == "__main__":
    main()
