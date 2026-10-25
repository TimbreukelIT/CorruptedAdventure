"""Portal test (change 100): from the middle of every ring through a Nether portal and back, with a real player.

    python tools/test/py/portal.py [--rings 1 4 5] [--heading 0] [--player Dev] [--json out.json]

Per ring: `/ca tpring <n>` (overworld, along --heading, default east), an obsidian frame (4 x 5, axis x) beside the landing spot and
fire in it (RCON `setblock`; portal blocks only if the fire did not light it), the player (creative: one tick in the
portal) into the portal, wait for the Nether, `/ca ring` at the arrival, out of the portal for the cooldown (300 ticks),
back into the portal it arrived in, wait for the overworld, and the distance to its own frame. Needs the running server
of this stream and the player online (client.sh start). Leaves the frames standing: run it on a test world.
"""
import argparse
import json
import math
import re
import sys
import time

sys.dont_write_bytecode = True
from rcon import Rcon  # noqa: E402

OW, NETHER = "minecraft:overworld", "minecraft:the_nether"
COOLDOWN_S = 17  # Entity.getDimensionChangingDelay 300 ticks, plus margin


def position(rcon, player):
    text = rcon.cmd("data get entity %s Pos" % player)
    m = re.findall(r"(-?\d+(?:\.\d+)?)d", text)
    return tuple(float(v) for v in m[:3]) if len(m) >= 3 else None


def dimension(rcon, player):
    m = re.search(r'"([a-z0-9_.-]+:[a-z0-9_/.-]+)"', rcon.cmd("data get entity %s Dimension" % player))
    return m.group(1) if m else None


def wait_dimension(rcon, player, dim, timeout):
    end = time.time() + timeout
    while time.time() < end:
        if dimension(rcon, player) == dim:
            return True
        time.sleep(0.5)
    return False


def ring_at(rcon, dim, x, z):
    text = rcon.cmd("execute in %s positioned %.1f 100 %.1f run ca ring" % (dim, x, z))
    return text.splitlines()[0].strip() if text.strip() else text


def portal_block(rcon, dim, x, y, z):
    return "passed" in rcon.cmd("execute in %s if block %d %d %d minecraft:nether_portal" % (dim, x, y, z)).lower()


def build_portal(rcon, x, y, z):
    """Frame x..x+3, y..y+4 at z (axis x), interior x+1..x+2, y+1..y+3; returns how it was lit."""
    run = lambda c: rcon.cmd("execute in %s run %s" % (OW, c))  # noqa: E731
    run("fill %d %d %d %d %d %d minecraft:air" % (x - 1, y, z - 2, x + 4, y + 5, z + 2))
    run("fill %d %d %d %d %d %d minecraft:obsidian" % (x - 1, y - 1, z - 2, x + 4, y - 1, z + 2))
    run("fill %d %d %d %d %d %d minecraft:obsidian" % (x, y, z, x + 3, y, z))
    run("fill %d %d %d %d %d %d minecraft:obsidian" % (x, y + 4, z, x + 3, y + 4, z))
    run("fill %d %d %d %d %d %d minecraft:obsidian" % (x, y + 1, z, x, y + 3, z))
    run("fill %d %d %d %d %d %d minecraft:obsidian" % (x + 3, y + 1, z, x + 3, y + 3, z))
    run("setblock %d %d %d minecraft:fire" % (x + 1, y + 1, z))
    time.sleep(0.5)
    if portal_block(rcon, OW, x + 1, y + 2, z):
        return "fire"
    run("fill %d %d %d %d %d %d minecraft:nether_portal[axis=x]" % (x + 1, y + 1, z, x + 2, y + 3, z))
    return "blocks" if portal_block(rcon, OW, x + 1, y + 2, z) else "failed"


def test_ring(rcon, player, n, timeout, heading=0.0):
    r = {"ring": n, "heading": heading}
    hx, hz = math.cos(math.radians(heading)), math.sin(math.radians(heading))
    rcon.cmd("execute in %s run tp %s %.1f 200 %.1f" % (OW, player, 100 * hx, 100 * hz))
    out = rcon.cmd("execute as %s run ca tpring %d" % (player, n))
    m = re.search(r"at (-?\d+) (-?\d+) (-?\d+)", out)
    if not m:
        r["error"] = "tpring: " + out.strip()
        return r
    x, y, z = (int(v) for v in m.groups())
    fx, fz = x + 3, z
    r["overworld_ring"] = ring_at(rcon, OW, fx + 1.5, fz + 0.5)
    r["frame"] = [fx, y, fz]
    r["lit"] = build_portal(rcon, fx, y, fz)
    if r["lit"] == "failed":
        r["error"] = "portal not lit"
        return r
    rcon.cmd("execute in %s run tp %s %.1f %d %.1f -90 0" % (OW, player, fx + 1.5, y + 1, fz + 0.5))
    if not wait_dimension(rcon, player, NETHER, timeout):
        r["error"] = "no arrival in the Nether"
        return r
    time.sleep(2)
    arrival = position(rcon, player)
    r["nether_arrival"] = [round(v, 1) for v in arrival]
    r["nether_expected"] = [round((fx + 1.5) / 4, 1), round((fz + 0.5) / 4, 1)]
    r["nether_ring"] = ring_at(rcon, NETHER, arrival[0], arrival[2])
    # Out of the portal for the cooldown (standing in it would restart it), then back into the same block.
    rcon.cmd("execute in %s run tp %s %.1f %.1f %.1f" % (NETHER, player, arrival[0], arrival[1] + 8, arrival[2]))
    time.sleep(COOLDOWN_S)
    rcon.cmd("execute in %s run tp %s %.2f %.2f %.2f" % (NETHER, player, arrival[0], arrival[1], arrival[2]))
    if not wait_dimension(rcon, player, OW, timeout):
        r["error"] = "no return to the overworld"
        return r
    time.sleep(2)
    back = position(rcon, player)
    r["overworld_return"] = [round(v, 1) for v in back]
    r["return_distance"] = round(((back[0] - fx - 1.5) ** 2 + (back[2] - fz - 0.5) ** 2) ** 0.5, 1)
    r["return_ring"] = ring_at(rcon, OW, back[0], back[2])
    # Loaded again by the player's return when it lands near the frame; "not loaded" counts as gone.
    r["frame_after"] = "passed" in rcon.cmd("execute in %s if block %d %d %d minecraft:obsidian" % (OW, fx, y, fz)).lower()
    r["ok"] = (r["nether_ring"].lower().startswith("ring %d" % n) and r["overworld_ring"].lower().startswith("ring %d" % n)
               and r["return_distance"] <= 4)
    return r


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--rings", nargs="*", type=int, default=[1, 2, 3, 4, 5])
    p.add_argument("--player", default="Dev")
    p.add_argument("--timeout", type=float, default=60.0)
    p.add_argument("--heading", type=float, default=0.0, help="degrees from +x towards +z (0 = east, 90 = south)")
    p.add_argument("--json")
    a = p.parse_args()
    rcon = Rcon()
    rcon.cmd("gamemode creative %s" % a.player)
    results = []
    for n in a.rings:
        r = test_ring(rcon, a.player, n, a.timeout, a.heading)
        results.append(r)
        print(json.dumps(r), flush=True)
    rcon.cmd("execute in %s run tp %s 100 200 0" % (OW, a.player))
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=1)
    sys.exit(0 if all(r.get("ok") for r in results) else 1)


if __name__ == "__main__":
    main()
