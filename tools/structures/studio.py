"""Photo studio driver (S0): run through tools/structures/studio.sh, which sets STUDIO_BASH and STUDIO_ROOT.

Places a template or structure with /ca studio over RCON, photographs every pose of /ca studio shots with
tools/test/client.sh shot (after waiting until the picture stops changing: chunks loaded and rendered), and builds the
contact sheet build/studio/<name>/sheet.png: one row per group (iso by day, elevations, top + cut-aways, iso by night),
every tile labelled. See studio.sh for the options.
"""
import argparse
import os
import re
import subprocess
import sys
import time

from PIL import Image, ImageChops, ImageDraw, ImageFont, ImageStat

ROOT = os.environ.get("STUDIO_ROOT") or os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BASH = os.environ.get("STUDIO_BASH", "bash")
sys.path.insert(0, os.path.join(ROOT, "tools", "test", "py"))
from rcon import Rcon  # noqa: E402

CLIENT_SH = os.path.join(ROOT, "tools", "test", "client.sh")
POSE = re.compile(r"^pose (\S+) (\S+) (day|night) (\S+) (\S+) (\S+) (\S+) (\S+) (\S+)$")
TILE_W = 480
LABEL_H = 20
COLS = 4
# Mean absolute difference (0-255, on a small grey copy) below which two shots in a row count as the same picture.
STABLE_DIFF = 1.5


def state(name):
    path = os.path.join(ROOT, "build", "test", name + ".state")
    out = {}
    if os.path.exists(path):
        for line in open(path, encoding="utf-8"):
            if "=" in line:
                k, v = line.rstrip("\r\n").split("=", 1)
                out[k] = v
    return out


def need_running(client):
    server, cl = state("server"), state("client%s" % client)
    if not server.get("PID") or not cl.get("PID"):
        sys.exit("studio: no running server and client %s of this stream. Start them first:\n"
                 "  bash tools/test/studio.sh prepare   (once: fixed FOV, no clouds)\n"
                 "  bash tools/test/server.sh start studio --studio --client\n"
                 "  bash tools/test/client.sh start".replace("tools/test/studio.sh", "tools/structures/studio.sh"))
    return cl.get("NAME", "Dev" if client == "1" else "Dev2")


def run_client(*args):
    return subprocess.run([BASH, CLIENT_SH, *args], capture_output=True, text=True)


WARNED = set()


def shot(path, client):
    r = run_client("shot", path, client)
    if r.returncode != 0:
        print(r.stdout + r.stderr, file=sys.stderr)
        return False
    for line in r.stderr.splitlines():
        # The F2 fallback (display off) comes with every shot: say it once.
        key = "F2" if "falling back to F2" in line else line
        if "warning" in line and key not in WARNED:
            WARNED.add(key)
            print(line.strip() + (" (said once)" if key == "F2" else ""), file=sys.stderr)
    return True


def difference(a, b):
    with Image.open(a) as ia, Image.open(b) as ib:
        sa = ia.convert("L").resize((160, 90))
        sb = ib.convert("L").resize((160, 90))
        return ImageStat.Stat(ImageChops.difference(sa, sb)).mean[0]


def stable_shot(path, client, first_wait, max_wait):
    """Waits first_wait s, then shoots until two shots in a row match (or max_wait s passed); keeps the last."""
    time.sleep(first_wait)
    tmp = path + ".prev.png"
    t0 = time.time()
    if not shot(tmp, client):
        return False
    while True:
        time.sleep(1.0)
        if not shot(path, client):
            return False
        diff = difference(tmp, path)
        if diff < STABLE_DIFF or time.time() - t0 > max_wait:
            if diff >= STABLE_DIFF:
                print("warning: %s still changing after %d s (diff %.1f)" % (os.path.basename(path), max_wait, diff),
                      file=sys.stderr)
            os.remove(tmp)
            return True
        os.replace(path, tmp)


def font(size):
    try:
        return ImageFont.load_default(size=size)
    except TypeError:  # Pillow < 10.1
        return ImageFont.load_default()


def group(label):
    if label.endswith("_night"):
        return 3
    if label.startswith("elev_"):
        return 1
    if label.startswith(("top", "cut_")):
        return 2
    return 0


def sheet(folder, title=None):
    files = sorted(f for f in os.listdir(folder) if re.match(r"^\d\d_.*\.png$", f))
    if not files:
        sys.exit("studio: no shots in " + folder)
    rows = []
    for g in range(4):
        tiles = [f for f in files if group(f[3:-4]) == g]
        for i in range(0, len(tiles), COLS):
            rows.append(tiles[i:i + COLS])
    with Image.open(os.path.join(folder, files[0])) as first:
        tile_h = TILE_W * first.height // first.width
    head = LABEL_H + 6 if title else 0
    out = Image.new("RGB", (COLS * TILE_W, head + len(rows) * (tile_h + LABEL_H)), (24, 24, 24))
    draw = ImageDraw.Draw(out)
    if title:
        draw.text((6, 3), title, fill="white", font=font(16))
    for r, row in enumerate(rows):
        for c, f in enumerate(row):
            x, y = c * TILE_W, head + r * (tile_h + LABEL_H)
            with Image.open(os.path.join(folder, f)) as im:
                out.paste(im.convert("RGB").resize((TILE_W, tile_h)), (x, y + LABEL_H))
            draw.text((x + 4, y + 2), f[:-4].replace("_", " "), fill="white", font=font(15))
    path = os.path.join(folder, "sheet.png")
    out.save(path)
    print("sheet", path, "(%d shots)" % len(files))
    return path


def check(response, what):
    if not response.startswith(what):
        sys.exit("studio: " + (response.strip() or "no answer"))
    return response


def options_render_distance(client):
    opts = os.path.join(ROOT, "run", "options.txt" if client == "1" else os.path.join("client2", "options.txt"))
    if os.path.exists(opts):
        for line in open(opts, encoding="utf-8"):
            if line.startswith("renderDistance:"):
                return int(line.split(":", 1)[1])
    return 12


def photograph(rcon, args, name):
    player = need_running(args.client)
    folder = os.path.join(ROOT, "build", "studio", name)
    os.makedirs(folder, exist_ok=True)
    for f in os.listdir(folder):
        if f.endswith(".png"):
            os.remove(os.path.join(folder, f))
    listing = check(rcon.cmd("ca studio shots" + (" " + args.floors if args.floors else "")), "")
    poses = [m.groups() for m in (POSE.match(line) for line in listing.splitlines()) if m]
    if not poses:
        sys.exit("studio: " + listing.strip())
    what = listing.splitlines()[0]
    view = re.search(r"^view (\d+)", listing, re.M)
    if view and int(view.group(1)) > options_render_distance(args.client):
        print("warning: the poses need render distance %s, the client has %d (studio.sh prepare --render %s, then "
              "restart the client)" % (view.group(1), options_render_distance(args.client), view.group(1)), file=sys.stderr)
    in_situ = not poses[0][1].startswith("corruptedadventure:studio")
    rcon.cmd("gamemode spectator " + player)
    rcon.cmd("weather clear")
    old_time = None
    if in_situ:
        m = re.search(r"(\d+)", rcon.cmd("time query daytime"))
        old_time = int(m.group(1)) if m else None
        for ring in range(2, 6):
            rcon.cmd("execute in %s run ca weather %d clear" % (poses[0][1], ring))
    run_client("key", "F1", args.client)  # hide the HUD (and the chat lines of these commands)
    last_dim, last_time = None, None
    try:
        for label, dim, daytime, x, y, z, yaw, pitch, cut in poses:
            if in_situ and daytime != last_time:
                rcon.cmd("time set %d" % (6000 if daytime == "day" else 18000))
            if cut != "-":
                rcon.cmd("ca studio cut " + cut)
            rcon.cmd("execute in %s run tp %s %s %s %s %s %s" % (dim, player, x, y, z, yaw, pitch))
            # A new dimension (or the first pose) has to load and render every chunk; the next pose mostly re-renders.
            first_wait = 6 if dim != last_dim else 2
            path = os.path.join(folder, label + ".png")
            ok = stable_shot(path, args.client, first_wait, args.max_wait)
            if cut != "-":
                rcon.cmd("ca studio restore")
            print(("shot " if ok else "FAILED ") + label)
            last_dim, last_time = dim, daytime
    finally:
        run_client("key", "F1", args.client)
        if old_time is not None:
            rcon.cmd("time set %d" % old_time)
    path = sheet(folder, title="%s  (%s)" % (what, name))
    # In situ the terrain goes back by default: the snapshot to restore it lives only in the server's memory.
    if args.clear or (in_situ and not args.keep):
        print(rcon.cmd("ca studio clear").strip())
    return path


def default_name(kind, ident, here):
    base = re.sub(r"[^A-Za-z0-9]+", "_", ident.split(":", 1)[-1]).strip("_")
    return base + ("_insitu" if here else "")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("command", choices=["template", "structure", "shots", "sheet"])
    p.add_argument("id", nargs="?")
    p.add_argument("--rot", default="none")
    p.add_argument("--mirror", default="none")
    p.add_argument("--seed", type=int)
    p.add_argument("--here", nargs=3, metavar=("X", "Y", "Z"))
    p.add_argument("--dim", default="minecraft:overworld")
    p.add_argument("--name")
    p.add_argument("--floors")
    p.add_argument("--client", default="1", choices=["1", "2"])
    p.add_argument("--clear", action="store_true")
    p.add_argument("--keep", action="store_true")
    p.add_argument("--max-wait", type=float, default=25.0)
    a = p.parse_args()
    if a.command == "sheet":
        if not a.id:
            sys.exit("studio: sheet needs a shot folder")
        sheet(a.id)
        return
    need_running(a.client)
    rcon = Rcon()
    if a.command == "shots":
        info = rcon.cmd("ca studio info")
        parts = info.split()
        if len(parts) < 2 or parts[0] not in ("template", "structure"):
            sys.exit("studio: " + info.strip())
        name = a.name or default_name(parts[0], parts[1], " in situ " in info)
        photograph(rcon, a, name)
        return
    if not a.id:
        sys.exit("studio: %s needs an id" % a.command)
    where = "here" if a.here else "studio"
    if a.command == "template":
        cmd = "ca studio template %s %s %s %s" % (a.id, where, a.rot, a.mirror)
    else:
        cmd = "ca studio structure %s %s" % (a.id, where) + ("" if a.seed is None else " %d" % a.seed)
    if a.here:
        x, y, z = a.here
        # Y "surface": the first free block above the ground (vanilla's heightmap, leaves don't count).
        at = "positioned %s 0 %s positioned over motion_blocking_no_leaves" % (x, z) if y == "surface" else "positioned %s %s %s" % (x, y, z)
        cmd = "execute in %s %s run %s" % (a.dim, at, cmd)
    surface = a.here and a.here[1] == "surface"
    if surface:
        # "positioned over" needs the chunk loaded: force-load it while placing.
        rcon.cmd("execute in %s run forceload add %s %s" % (a.dim, a.here[0], a.here[2]))
    try:
        answer = rcon.cmd(cmd)
        for _ in range(30):
            if not surface or "not loaded" not in answer:
                break
            time.sleep(1.0)
            answer = rcon.cmd(cmd)
    finally:
        if surface:
            rcon.cmd("execute in %s run forceload remove %s %s" % (a.dim, a.here[0], a.here[2]))
    print(check(answer, "Placed").strip())
    photograph(rcon, a, a.name or default_name(a.command, a.id, a.here))


if __name__ == "__main__":
    main()
