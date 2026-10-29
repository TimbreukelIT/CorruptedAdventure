"""The Lost Keeper's animations (GeckoLib / Bedrock animation format 1.8.0).

    python tools/textures/lost_keeper_animations.py        (from the repository root)

Writes src/main/resources/assets/corruptedadventure/animations/entity/the_lost_keeper.animation.json. Names and lengths
match entity/lostkeeper/KeeperAnimations and the server's timings: the breath starts at 0.75 s (breathWindupTicks 15) and
lasts until 3.0 s (+ breathTicks 45), the grab snaps at 0.5 s (grabWindupTicks 10), the tail whip strikes from 0.4 s to
0.9 s, death lasts 3.0 s (DEATH_TICKS 60). Life round: the bubble call goes off at 1.5 s (bubbleWarnTicks 30); the dive,
the burst and the squeeze start with their moment on the server; the coil loops while the ring lasts.

Only the head's parts, the wings and the tail fin move here. The segment bones (seg_01..seg_12) and neck_mount are laid
along the body chain by the client every frame; an animation on them would be overwritten, so there is none.

Bedrock conventions (as in hollow_animations.py): x and y rotations are negated on load, so in this file a positive x
tips the front (-z end) of a bone down: for the head that lowers the snout, for the jaw (open at rest) that opens it
wider; a negative x closes it. z is not negated: for the right wing (+x) a negative z lowers the outer edge, for the
left wing a positive z. Keyframes are linear. Checked with tools/textures/lost_keeper_preview.py (animation argument).
"""
import json
import math
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/animations/entity/the_lost_keeper.animation.json")


def keys(*pairs):
    return {str(round(float(t), 3)): [round(float(c), 3) for c in v] for t, v in pairs}


def channel(rotation=None, position=None, scale=None):
    out = {}
    if rotation:
        out["rotation"] = keys(*rotation)
    if position:
        out["position"] = keys(*position)
    if scale:
        out["scale"] = keys(*scale)
    return out


def wave(length, steps, fn):
    """Keyframes of a looping wave: fn(phase 0..1) -> (x, y, z), sampled `steps` times plus the closing key."""
    return [(length * k / steps, fn(k / steps)) for k in range(steps + 1)]


def wings(length, beat, lift, sweep, phase=0.0):
    """A stroke of both wings: z for the up/down beat, y for the forward/back sweep. Mirrored for the left wing."""
    def right(t):
        a = 2 * math.pi * (t + phase)
        return (0, -sweep * math.cos(a), -lift + beat * math.sin(a))

    def left(t):
        x, y, z = right(t)
        return (x, -y, -z)

    return {"wing_right": channel(rotation=wave(length, 8, right)), "wing_left": channel(rotation=wave(length, 8, left))}


def gills(length, flare, phase=0.0):
    def right(t):
        return (0, -flare * (0.5 + 0.5 * math.sin(2 * math.pi * (t + phase))), 0)

    def left(t):
        x, y, z = right(t)
        return (x, -y, z)

    return {"gill_fin_right": channel(rotation=wave(length, 4, right)), "gill_fin_left": channel(rotation=wave(length, 4, left))}


def barbels(length, swing):
    def right(t):
        return (swing * math.sin(2 * math.pi * t), 0, swing * 0.5 * math.cos(2 * math.pi * t))

    def left(t):
        return (swing * math.sin(2 * math.pi * t + 0.8), 0, -swing * 0.5 * math.cos(2 * math.pi * t + 0.8))

    return {"barbel_right": channel(rotation=wave(length, 4, right)), "barbel_left": channel(rotation=wave(length, 4, left))}


def tail(length, swing):
    return {"tail_fin": channel(rotation=wave(length, 4, lambda t: (0, swing * math.sin(2 * math.pi * t), 0)))}


def merge(*parts):
    out = {}
    for part in parts:
        for bone, ch in part.items():
            out.setdefault(bone, {}).update(ch)
    return out


def hover():
    length = 4.0
    return {"loop": True, "animation_length": length, "bones": merge(
        wings(length, 10, 0, 6), gills(length, 12), barbels(length, 6), tail(length, 10),
        {"jaw": channel(rotation=[(0, (0, 0, 0)), (2.0, (-6, 0, 0)), (4.0, (0, 0, 0))]),
         "head": channel(rotation=[(0, (0, 0, 0)), (2.0, (-3, 0, 2)), (4.0, (0, 0, 0))]),
         "crown": channel(scale=[(0, (1, 1, 1)), (2.0, (1.04, 1.04, 1.04)), (4.0, (1, 1, 1))])})}


def swim():
    length = 1.6
    return {"loop": True, "animation_length": length, "bones": merge(
        wings(length, 24, 0, 16), gills(length, 18, 0.25), barbels(length, 14), tail(length, 22),
        {"jaw": channel(rotation=[(0, (0, 0, 0)), (0.8, (-4, 0, 0)), (1.6, (0, 0, 0))])})}


def lure():
    length = 2.0
    return {"loop": True, "animation_length": length, "bones": merge(
        # Spread wide and nearly still, the head raised, the crown pulsing: the glow and the song.
        wings(length, 5, -18, 3), gills(length, 30), barbels(length, 4), tail(length, 6),
        {"head": channel(rotation=[(0, (-22, 0, 0)), (1.0, (-26, 0, 3)), (2.0, (-22, 0, 0))]),
         "jaw": channel(rotation=[(0, (-12, 0, 0)), (1.0, (-16, 0, 0)), (2.0, (-12, 0, 0))]),
         "crown": channel(scale=[(0, (1.04, 1.04, 1.04)), (1.0, (1.12, 1.12, 1.12)), (2.0, (1.04, 1.04, 1.04))])})}


def breath():
    # 0–0.75 s rears back and draws in; 0.75–3.0 s breathes the cloud with the jaw wide; then closes.
    tremble = [(0.75 + 0.15 * k, (0, 0, 2 if k % 2 else -2)) for k in range(16)]
    return {"animation_length": 3.4, "bones": merge(wings(3.4, 0, -10, 0), {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.6, (-28, 0, 0)), (0.75, (14, 0, 0)), (3.0, (10, 0, 0)), (3.4, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (0.6, (-14, 0, 0)), (0.75, (22, 0, 0)), (3.0, (22, 0, 0)), (3.4, (0, 0, 0))]),
        "gill_fin_right": channel(rotation=[(0, (0, 0, 0)), (0.6, (0, -30, 0)), (3.0, (0, -30, 0)), (3.4, (0, 0, 0))]),
        "gill_fin_left": channel(rotation=[(0, (0, 0, 0)), (0.6, (0, 30, 0)), (3.0, (0, 30, 0)), (3.4, (0, 0, 0))]),
        "barbel_right": channel(rotation=tremble),
        "barbel_left": channel(rotation=tremble),
    })}


def grab():
    return {"animation_length": 1.0, "bones": {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.4, (-14, 0, 0)), (0.5, (12, 0, 0)), (0.7, (8, 0, 0)), (1.0, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (0.4, (20, 0, 0)), (0.5, (-30, 0, 0)), (0.85, (-30, 0, 0)), (1.0, (0, 0, 0))]),
        "wing_right": channel(rotation=[(0, (0, 0, 0)), (0.4, (0, 20, 12)), (0.55, (0, -25, -20)), (1.0, (0, 0, 0))]),
        "wing_left": channel(rotation=[(0, (0, 0, 0)), (0.4, (0, -20, -12)), (0.55, (0, 25, 20)), (1.0, (0, 0, 0))]),
    }}


def hold():
    length = 1.0
    shake = [(0, (0, 0, -4)), (0.25, (2, 0, 5)), (0.5, (0, 0, -3)), (0.75, (2, 0, 4)), (1.0, (0, 0, -4))]
    return {"loop": True, "animation_length": length, "bones": merge(wings(length, 30, 0, 18), tail(length, 25), {
        "head": channel(rotation=shake),
        "jaw": channel(rotation=[(0, (-26, 0, 0)), (1.0, (-26, 0, 0))])})}


def tail_whip():
    return {"animation_length": 1.2, "bones": {
        "tail_fin": channel(rotation=[(0, (0, 0, 0)), (0.35, (0, -35, 0)), (0.45, (0, 45, 0)), (0.9, (0, 30, 0)),
                                      (1.2, (0, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (0.35, (-8, 0, -6)), (0.6, (4, 0, 8)), (1.2, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (0.35, (10, 0, 0)), (0.6, (-10, 0, 0)), (1.2, (0, 0, 0))]),
    }}


def roar():
    shake = [(0.3 + 0.1 * k, (-30 + (3 if k % 2 else -3), 0, 4 if k % 2 else -4)) for k in range(8)]
    return {"animation_length": 1.6, "bones": merge(wings(1.6, 0, -25, 0), gills(1.6, 0), {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.3, (-30, 0, 0))] + shake + [(1.6, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (0.3, (24, 0, 0)), (1.2, (24, 0, 0)), (1.6, (0, 0, 0))]),
        "gill_fin_right": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, -40, 0)), (1.2, (0, -40, 0)), (1.6, (0, 0, 0))]),
        "gill_fin_left": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 40, 0)), (1.2, (0, 40, 0)), (1.6, (0, 0, 0))]),
        "crown": channel(scale=[(0, (1, 1, 1)), (0.3, (1.1, 1.1, 1.1)), (1.6, (1, 1, 1))]),
    })}


def fall():
    length = 0.6
    return {"loop": True, "animation_length": length, "bones": merge(wings(length, 45, 10, 30), tail(length, 35), {
        "head": channel(rotation=[(0, (-20, 0, -10)), (0.3, (-10, 0, 12)), (0.6, (-20, 0, -10))]),
        "jaw": channel(rotation=[(0, (20, 0, 0)), (0.6, (20, 0, 0))])})}


def stunned():
    length = 2.0
    return {"loop": True, "animation_length": length, "bones": merge(wings(length, 3, 35, 0), {
        "head": channel(rotation=[(0, (16, 0, 8)), (1.0, (18, 0, 10)), (2.0, (16, 0, 8))]),
        "jaw": channel(rotation=[(0, (12, 0, 0)), (2.0, (12, 0, 0))]),
        "gill_fin_right": channel(rotation=[(0, (0, 25, 0)), (2.0, (0, 25, 0))]),
        "gill_fin_left": channel(rotation=[(0, (0, -25, 0)), (2.0, (0, -25, 0))])})}


def crawl():
    length = 1.6

    def right(t):
        a = 2 * math.pi * t
        return (0, -30 * math.cos(a), 20 + 12 * max(0.0, math.sin(a)))

    def left(t):
        a = 2 * math.pi * t + math.pi
        return (0, 30 * math.cos(a), -20 - 12 * max(0.0, math.sin(a)))

    return {"loop": True, "animation_length": length, "bones": merge(tail(length, 12), {
        # The wings push like flippers, one after the other; the head stays low.
        "wing_right": channel(rotation=wave(length, 8, right)),
        "wing_left": channel(rotation=wave(length, 8, left)),
        "head": channel(rotation=[(0, (8, 0, -4)), (0.8, (8, 0, 4)), (1.6, (8, 0, -4))])})}


def death():
    return {"animation_length": 3.0, "bones": {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.4, (-30, 0, 0)), (0.9, (-34, 0, 6)), (1.8, (20, 0, 14)), (3.0, (34, 0, 20))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (0.4, (24, 0, 0)), (1.8, (18, 0, 0)), (3.0, (26, 0, 0))]),
        "wing_right": channel(rotation=[(0, (0, 0, 0)), (0.5, (0, 0, 25)), (2.0, (0, 40, -40)), (3.0, (0, 45, -45))]),
        "wing_left": channel(rotation=[(0, (0, 0, 0)), (0.5, (0, 0, -25)), (2.0, (0, -40, 40)), (3.0, (0, -45, 45))]),
        "gill_fin_right": channel(rotation=[(0, (0, 0, 0)), (3.0, (0, 30, 0))]),
        "gill_fin_left": channel(rotation=[(0, (0, 0, 0)), (3.0, (0, -30, 0))]),
        "crown": channel(scale=[(0, (1, 1, 1)), (0.4, (1.1, 1.1, 1.1)), (3.0, (0.92, 0.92, 0.92))]),
        "tail_fin": channel(rotation=[(0, (0, 0, 0)), (1.0, (0, 30, 0)), (3.0, (0, 10, 0))]),
    }}


def bubble_call():
    # 0–1.5 s (bubbleWarnTicks 30): the head sinks, the jaw opens slowly, the gills flare and the wings tremble low;
    # at 1.5 s a jerk up as the trap goes off.
    tremble = [(0.1 * k, (0, 4 if k % 2 else -4, -14 + (3 if k % 2 else -3))) for k in range(16)]
    tremble_left = [(t, (x, -y, -z)) for t, (x, y, z) in tremble]
    return {"animation_length": 2.0, "bones": {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.5, (24, 0, 0)), (1.4, (28, 0, 4)), (1.55, (-18, 0, 0)), (2.0, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (1.4, (18, 0, 0)), (1.55, (26, 0, 0)), (2.0, (0, 0, 0))]),
        "gill_fin_right": channel(rotation=[(0, (0, 0, 0)), (0.5, (0, -35, 0)), (1.5, (0, -35, 0)), (2.0, (0, 0, 0))]),
        "gill_fin_left": channel(rotation=[(0, (0, 0, 0)), (0.5, (0, 35, 0)), (1.5, (0, 35, 0)), (2.0, (0, 0, 0))]),
        "wing_right": channel(rotation=tremble + [(2.0, (0, 0, 0))]),
        "wing_left": channel(rotation=tremble_left + [(2.0, (0, 0, 0))]),
        "crown": channel(scale=[(0, (1, 1, 1)), (1.4, (1.08, 1.08, 1.08)), (2.0, (1, 1, 1))]),
    }}


def dive():
    # The wings fold flat along the body, the head tips down into the dive, the tail fin flicks.
    return {"animation_length": 1.5, "bones": {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.3, (20, 0, 0)), (1.2, (14, 0, 0)), (1.5, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (0.3, (-6, 0, 0)), (1.5, (0, 0, 0))]),
        "wing_right": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 35, 30)), (1.2, (0, 35, 30)), (1.5, (0, 0, 0))]),
        "wing_left": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, -35, -30)), (1.2, (0, -35, -30)), (1.5, (0, 0, 0))]),
        "gill_fin_right": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 20, 0)), (1.5, (0, 0, 0))]),
        "gill_fin_left": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, -20, 0)), (1.5, (0, 0, 0))]),
        "tail_fin": channel(rotation=[(0, (0, 0, 0)), (0.2, (0, 30, 0)), (0.4, (0, -30, 0)), (0.6, (0, 0, 0))]),
    }}


def burst():
    # Out of the water: head thrown up, jaws wide, wings flung open and up, a shake at the top.
    shake = [(0.5 + 0.1 * k, (-26 + (3 if k % 2 else -3), 0, 4 if k % 2 else -4)) for k in range(5)]
    return {"animation_length": 1.6, "bones": merge(gills(1.6, 40), {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.2, (-34, 0, 0))] + shake + [(1.6, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (0, 0, 0)), (0.2, (30, 0, 0)), (1.1, (28, 0, 0)), (1.6, (0, 0, 0))]),
        "wing_right": channel(rotation=[(0, (0, 0, 0)), (0.2, (0, -20, 40)), (0.8, (0, -10, 25)), (1.6, (0, 0, 0))]),
        "wing_left": channel(rotation=[(0, (0, 0, 0)), (0.2, (0, 20, -40)), (0.8, (0, 10, -25)), (1.6, (0, 0, 0))]),
        "crown": channel(scale=[(0, (1, 1, 1)), (0.2, (1.12, 1.12, 1.12)), (1.6, (1, 1, 1))]),
    })}


def coil():
    # Coiling the ring: the head low and turned in, jaw half open in a hiss, the wings pressed tight and quivering.
    length = 1.0
    return {"loop": True, "animation_length": length, "bones": merge(wings(length, 6, 22, 4), tail(length, 18), {
        "head": channel(rotation=[(0, (10, -12, 0)), (0.5, (12, -14, 3)), (1.0, (10, -12, 0))]),
        "jaw": channel(rotation=[(0, (14, 0, 0)), (0.25, (18, 0, 0)), (0.5, (14, 0, 0)), (0.75, (18, 0, 0)), (1.0, (14, 0, 0))]),
        "gill_fin_right": channel(rotation=[(0, (0, -30, 0)), (1.0, (0, -30, 0))]),
        "gill_fin_left": channel(rotation=[(0, (0, 30, 0)), (1.0, (0, 30, 0))])})}


def squeeze():
    # The ring closes: a snap of the head down and the jaw shut, then a roar up.
    return {"animation_length": 1.2, "bones": {
        "head": channel(rotation=[(0, (0, 0, 0)), (0.15, (22, 0, 0)), (0.35, (-24, 0, 0)), (0.9, (-20, 0, 0)), (1.2, (0, 0, 0))]),
        "jaw": channel(rotation=[(0, (14, 0, 0)), (0.15, (-28, 0, 0)), (0.35, (26, 0, 0)), (0.9, (24, 0, 0)), (1.2, (0, 0, 0))]),
        "wing_right": channel(rotation=[(0, (0, 0, 0)), (0.35, (0, -15, 30)), (1.2, (0, 0, 0))]),
        "wing_left": channel(rotation=[(0, (0, 0, 0)), (0.35, (0, 15, -30)), (1.2, (0, 0, 0))]),
    }}


def main():
    animations = {
        "animation.the_lost_keeper.hover": hover(),
        "animation.the_lost_keeper.swim": swim(),
        "animation.the_lost_keeper.lure": lure(),
        "animation.the_lost_keeper.breath": breath(),
        "animation.the_lost_keeper.grab": grab(),
        "animation.the_lost_keeper.hold": hold(),
        "animation.the_lost_keeper.tail_whip": tail_whip(),
        "animation.the_lost_keeper.roar": roar(),
        "animation.the_lost_keeper.fall": fall(),
        "animation.the_lost_keeper.stunned": stunned(),
        "animation.the_lost_keeper.crawl": crawl(),
        "animation.the_lost_keeper.death": death(),
        "animation.the_lost_keeper.bubble_call": bubble_call(),
        "animation.the_lost_keeper.dive": dive(),
        "animation.the_lost_keeper.burst": burst(),
        "animation.the_lost_keeper.coil": coil(),
        "animation.the_lost_keeper.squeeze": squeeze(),
    }
    for anim in animations.values():
        anim["bones"] = {bone: ch for bone, ch in anim["bones"].items() if ch}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": animations}, f, indent="\t")
        f.write("\n")
    print(f"{len(animations)} animations -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
