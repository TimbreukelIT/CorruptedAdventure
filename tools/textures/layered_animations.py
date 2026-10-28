"""The Layered's animations (GeckoLib / Bedrock animation format 1.8.0).

    python tools/textures/layered_animations.py        (from the repository root)

Writes src/main/resources/assets/corruptedadventure/animations/entity/the_layered.animation.json. Names and lengths match
entity/layered/LayeredAnimations and the server's timings: the bite of the grab lands at 0.5 s (grabWindupTicks 10), the
rock fall breaks loose at 0.7 s (rockfallWindupTicks 14), the release lets go at 0.15 s, death lasts 3.0 s (DEATH_TICKS
60). On a wall the renderer turns the whole model onto the wall, so walk doubles as the climb.

Bedrock conventions (as Blockbench shows them; the creature faces -z): a negative x rotation lifts the front of a bone
that points forward and swings a hanging limb forward; on the hips of the -x ("left") legs a negative z lifts the leg and
a positive y swings it forward; the +x ("right") legs mirror that (positive z lifts, negative y swings forward). Keyframes
are linear.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/animations/entity/the_layered.animation.json")

LEGS = ("front", "mid", "back")
# Tripods: front left, mid right and back left move together; the other three half a cycle later.
TRIPOD_A = {("front", "left"), ("mid", "right"), ("back", "left")}


def keys(*pairs):
    return {str(round(float(t), 3)): list(v) for t, v in pairs}


def channel(rotation=None, position=None, scale=None):
    out = {}
    if rotation:
        out["rotation"] = keys(*rotation)
    if position:
        out["position"] = keys(*position)
    if scale:
        out["scale"] = keys(*scale)
    return out


def side_sign(side):
    """Mirror for the hip: (lift z sign, forward y sign)."""
    return (-1, 1) if side == "left" else (1, -1)


def shift(frames, offset, length):
    """Shifts a looping key list by offset seconds (wrapping); the new keys at 0 and length interpolate across the seam."""
    moved = sorted(((t + offset) % length, v) for t, v in frames if t < length)
    (t_last, v_last), (t_first, v_first) = moved[-1], moved[0]
    span = (length - t_last) + t_first
    a = (length - t_last) / span if span > 0 else 0
    v0 = tuple(v_last[i] * (1 - a) + v_first[i] * a for i in range(3))
    return [(0, v0)] + [m for m in moved if m[0] > 0] + [(length, v0)]


def walk():
    length = 1.2
    bones = {
        "body": channel(position=[(0, (0, 0, 0)), (0.3, (0, 0.8, 0)), (0.6, (0, 0, 0)), (0.9, (0, 0.8, 0)),
                                  (1.2, (0, 0, 0))],
                        rotation=[(0, (0, 0, 1.5)), (0.6, (0, 0, -1.5)), (1.2, (0, 0, 1.5))]),
        "abdomen": channel(rotation=[(0, (1, -2, 0)), (0.6, (-1, 2, 0)), (1.2, (1, -2, 0))]),
        "head_alive": channel(rotation=[(0, (2, 0, -2)), (0.6, (-1, 0, 2)), (1.2, (2, 0, -2))]),
    }
    for pair in LEGS:
        for side in ("left", "right"):
            lift, fwd = side_sign(side)
            # Swing forward lifted (0-0.6 s), then the stroke back on the ground (0.6-1.2 s).
            hip = [(0, (0, -12 * fwd, 0)), (0.3, (0, 0, 14 * lift)), (0.6, (0, 12 * fwd, 0)), (1.2, (0, -12 * fwd, 0))]
            knee = [(0, (0, 0, 0)), (0.3, (0, 0, -9 * lift)), (0.6, (0, 0, 0)), (1.2, (0, 0, 0))]
            offset = 0 if (pair, side) in TRIPOD_A else length / 2
            bones[f"leg_{pair}_{side}"] = channel(rotation=shift(hip, offset, length))
            bones[f"leg_{pair}_{side}_lower"] = channel(rotation=shift(knee, offset, length))
    return {"loop": True, "animation_length": length, "bones": bones}


def idle():
    return {"loop": True, "animation_length": 4.0, "bones": {
        # Breathing: the abdomen swells, the plates part a little.
        "abdomen": channel(scale=[(0, (1, 1, 1)), (2.0, (1.03, 1.04, 1.02)), (4.0, (1, 1, 1))],
                           rotation=[(0, (0, 0, 0)), (2.0, (-1.5, 0, 0)), (4.0, (0, 0, 0))]),
        "body": channel(position=[(0, (0, 0, 0)), (2.0, (0, -0.6, 0)), (4.0, (0, 0, 0))]),
        "head_alive": channel(rotation=[(0, (0, 0, 0)), (1.4, (-4, 6, 0)), (2.8, (2, -5, 0)), (4.0, (0, 0, 0))]),
        "beak_lower": channel(rotation=[(0, (0, 0, 0)), (1.8, (0, 0, 0)), (2.1, (9, 0, 0)), (2.5, (0, 0, 0)),
                                        (4.0, (0, 0, 0))]),
        # A spider's twitch: one front leg taps the ground, a mid leg shifts its weight.
        "leg_front_left": channel(rotation=[(0, (0, 0, 0)), (1.0, (0, 0, 0)), (1.15, (0, 4, -12)), (1.3, (0, 2, 0)),
                                            (1.45, (0, 4, -9)), (1.6, (0, 0, 0)), (4.0, (0, 0, 0))]),
        "leg_front_left_lower": channel(rotation=[(0, (0, 0, 0)), (1.15, (0, 0, 8)), (1.3, (0, 0, 0)),
                                                  (1.45, (0, 0, 6)), (1.6, (0, 0, 0)), (4.0, (0, 0, 0))]),
        "leg_mid_right": channel(rotation=[(0, (0, 0, 0)), (2.9, (0, 0, 0)), (3.1, (0, -5, 8)), (3.3, (0, -3, 0)),
                                           (4.0, (0, 0, 0))]),
        "leg_back_left": channel(rotation=[(0, (0, 0, 0)), (2.0, (0, -2, -1)), (4.0, (0, 0, 0))]),
        "leg_back_right": channel(rotation=[(0, (0, 0, 0)), (2.0, (0, 2, 1)), (4.0, (0, 0, 0))]),
    }}


def heads():
    """The dead heads, on their own controller so they never stop: they loll, and snap out of step with each other.
    The renderer turns them toward the player on top of this."""
    return {"loop": True, "animation_length": 3.4, "bones": {
        "head_dead_left": channel(rotation=[(0, (0, 0, 0)), (0.8, (4, -6, 5)), (1.3, (-3, 4, -2)), (1.35, (6, 2, 8)),
                                            (2.2, (2, -3, 3)), (3.4, (0, 0, 0))]),
        "jaw_left": channel(rotation=[(0, (0, 0, 0)), (1.2, (8, 0, 0)), (1.3, (-36, 0, 0)), (1.42, (4, 0, 0)),
                                      (2.6, (0, 0, 0)), (2.66, (-30, 0, 0)), (2.78, (0, 0, 0)), (3.4, (0, 0, 0))]),
        "head_dead_right": channel(rotation=[(0, (0, 0, 0)), (0.6, (-3, 5, -4)), (1.9, (5, -4, 3)), (2.5, (5, -4, 3)),
                                             (2.55, (-2, 8, -6)), (3.4, (0, 0, 0))]),
        "jaw_right": channel(rotation=[(0, (0, 0, 0)), (0.45, (0, 0, 0)), (0.5, (-18, 0, 0)), (0.62, (10, 0, 0)),
                                       (2.0, (6, 0, 0)), (2.5, (14, 0, 0)), (2.55, (-18, 0, 0)), (2.7, (0, 0, 0)),
                                       (3.4, (0, 0, 0))]),
    }}


def grab():
    return {"animation_length": 1.0, "bones": {
        # Rears up, the beak gapes, then the head stabs down and the beak clamps shut at 0.5 s.
        "body": channel(rotation=[(0, (0, 0, 0)), (0.35, (-16, 0, 0)), (0.5, (6, 0, 0)), (0.7, (-10, 0, 0)),
                                  (1.0, (-10, 0, 0))],
                        position=[(0, (0, 0, 0)), (0.35, (0, 4, 0)), (0.5, (0, -1, 0)), (0.7, (0, 2, 0)), (1.0, (0, 2, 0))]),
        "abdomen": channel(rotation=[(0, (0, 0, 0)), (0.35, (12, 0, 0)), (0.5, (-4, 0, 0)), (0.7, (8, 0, 0)),
                                     (1.0, (8, 0, 0))]),
        "head_alive": channel(rotation=[(0, (0, 0, 0)), (0.35, (-25, 0, 0)), (0.48, (28, 0, 0)), (0.6, (10, 0, 0)),
                                        (1.0, (-12, 0, 0))]),
        "beak_lower": channel(rotation=[(0, (0, 0, 0)), (0.3, (38, 0, 0)), (0.46, (40, 0, 0)), (0.5, (0, 0, 0)),
                                        (1.0, (0, 0, 0))]),
        "leg_front_left": channel(rotation=[(0, (0, 0, 0)), (0.35, (0, 10, -22)), (0.5, (0, 14, -4)), (1.0, (0, 0, 0))]),
        "leg_front_right": channel(rotation=[(0, (0, 0, 0)), (0.35, (0, -10, 22)), (0.5, (0, -14, 4)), (1.0, (0, 0, 0))]),
    }}


def hold():
    """Loops while it carries a player in its beak: front raised, beak clamped, the head jerks."""
    return {"loop": True, "animation_length": 1.0, "bones": {
        "head_alive": channel(rotation=[(0, (-12, 0, 0)), (0.2, (-16, 6, 3)), (0.3, (-10, -4, -2)),
                                        (0.6, (-14, 3, 0)), (0.7, (-9, -5, 2)), (1.0, (-12, 0, 0))]),
        "beak_lower": channel(rotation=[(0, (-3, 0, 0)), (0.5, (-1, 0, 0)), (1.0, (-3, 0, 0))]),
    }}


def rockfall():
    return {"animation_length": 1.4, "bones": {
        # Rears on its hind legs and hammers the stone with both front legs; the rock breaks loose at 0.7 s.
        "body": channel(rotation=[(0, (0, 0, 0)), (0.3, (-20, 0, 0)), (0.45, (-6, 0, 0)), (0.55, (-22, 0, 0)),
                                  (0.7, (-4, 0, 0)), (1.0, (-4, 0, 0)), (1.4, (0, 0, 0))],
                        position=[(0, (0, 0, 0)), (0.3, (0, 5, 0)), (0.45, (0, 1, 0)), (0.55, (0, 5, 0)),
                                  (0.7, (0, 0, 0)), (1.0, (0, 1, 0)), (1.4, (0, 0, 0))]),
        "abdomen": channel(rotation=[(0, (0, 0, 0)), (0.3, (15, 0, 0)), (0.45, (4, 0, 0)), (0.55, (16, 0, 0)),
                                     (0.7, (3, 0, 0)), (1.4, (0, 0, 0))]),
        "leg_front_left": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 12, -40)), (0.45, (0, 16, 6)), (0.55, (0, 12, -40)),
                                            (0.7, (0, 16, 8)), (1.4, (0, 0, 0))]),
        "leg_front_left_lower": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 0, 25)), (0.45, (0, 0, -6)),
                                                  (0.55, (0, 0, 25)), (0.7, (0, 0, -8)), (1.4, (0, 0, 0))]),
        "leg_front_right": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, -12, 40)), (0.45, (0, -16, -6)),
                                             (0.55, (0, -12, 40)), (0.7, (0, -16, -8)), (1.4, (0, 0, 0))]),
        "leg_front_right_lower": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 0, -25)), (0.45, (0, 0, 6)),
                                                   (0.55, (0, 0, -25)), (0.7, (0, 0, 8)), (1.4, (0, 0, 0))]),
        "head_alive": channel(rotation=[(0, (0, 0, 0)), (0.3, (-18, 0, 0)), (0.7, (14, 0, 0)), (1.4, (0, 0, 0))]),
        "beak_lower": channel(rotation=[(0, (0, 0, 0)), (0.3, (30, 0, 0)), (0.7, (34, 0, 0)), (1.0, (0, 0, 0))]),
    }}


def release():
    return {"animation_length": 0.6, "bones": {
        # A flick of the head: the beak springs open at 0.15 s and the player drops.
        "head_alive": channel(rotation=[(0, (-12, 0, 0)), (0.12, (-22, -8, 0)), (0.2, (8, 10, 0)), (0.6, (0, 0, 0))]),
        "beak_lower": channel(rotation=[(0, (0, 0, 0)), (0.15, (42, 0, 0)), (0.4, (20, 0, 0)), (0.6, (0, 0, 0))]),
        "body": channel(rotation=[(0, (-10, 0, 0)), (0.2, (-4, 0, 0)), (0.6, (0, 0, 0))]),
    }}


def fallen():
    """Loops while it lies on its back: legs flailing, the soft belly up. The controller's transition rolls it over."""
    length = 1.0
    bones = {
        "root": channel(rotation=[(0, (0, 0, 180)), (length, (0, 0, 180))],
                        position=[(0, (0, -9, 0)), (0.5, (0, -8.4, 0)), (length, (0, -9, 0))]),
        "head_alive": channel(rotation=[(0, (20, 0, 0)), (0.3, (10, 12, 0)), (0.6, (24, -10, 0)), (length, (20, 0, 0))]),
        "beak_lower": channel(rotation=[(0, (20, 0, 0)), (0.25, (36, 0, 0)), (0.5, (10, 0, 0)), (0.75, (36, 0, 0)),
                                        (length, (20, 0, 0))]),
    }
    for i, pair in enumerate(LEGS):
        for side in ("left", "right"):
            lift, fwd = side_sign(side)
            hip = [(0, (0, 8 * fwd, 10 * lift)), (0.25, (0, -6 * fwd, 28 * lift)), (0.5, (0, 10 * fwd, 4 * lift)),
                   (0.75, (0, -4 * fwd, 24 * lift)), (length, (0, 8 * fwd, 10 * lift))]
            knee = [(0, (0, 0, -20 * lift)), (0.25, (0, 0, -45 * lift)), (0.5, (0, 0, -15 * lift)),
                    (0.75, (0, 0, -40 * lift)), (length, (0, 0, -20 * lift))]
            offset = (i * 0.17 + (0.4 if side == "right" else 0)) % length
            bones[f"leg_{pair}_{side}"] = channel(rotation=shift(hip, offset, length))
            bones[f"leg_{pair}_{side}_lower"] = channel(rotation=shift(knee, offset, length))
    return {"loop": True, "animation_length": length, "bones": bones}


def death():
    bones = {
        # Shudders, sinks onto its belly, and the legs curl in over the body like a dead spider's.
        "root": channel(position=[(0, (0, 0, 0)), (0.4, (0, 1, 0)), (1.2, (0, -4, 0)), (1.8, (0, -10, 0)),
                                  (2.1, (0, -8, 0)), (2.3, (0, -10, 0)), (3.0, (0, -10, 0))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (0.2, (-6, 0, 3)), (0.3, (-3, 0, -3)), (0.4, (-8, 0, 2)),
                                  (1.8, (6, 0, -4)), (3.0, (6, 0, -4))]),
        "head_alive": channel(rotation=[(0, (0, 0, 0)), (0.4, (-30, 8, 0)), (1.8, (30, -10, 12)), (3.0, (34, -10, 14))]),
        "beak_lower": channel(rotation=[(0, (0, 0, 0)), (0.4, (45, 0, 0)), (3.0, (30, 0, 0))]),
        "head_dead_left": channel(rotation=[(0, (0, 0, 0)), (2.0, (25, 0, -20)), (3.0, (25, 0, -20))]),
        "head_dead_right": channel(rotation=[(0, (0, 0, 0)), (2.0, (20, 0, 25)), (3.0, (20, 0, 25))]),
    }
    for i, pair in enumerate(LEGS):
        for side in ("left", "right"):
            lift, fwd = side_sign(side)
            start = 1.0 + i * 0.15
            bones[f"leg_{pair}_{side}"] = channel(rotation=[
                (0, (0, 0, 0)), (0.3, (0, 0, 6 * lift)), (start, (0, 0, -4 * lift)), (start + 0.8, (0, 6 * fwd, 56 * lift)),
                (3.0, (0, 6 * fwd, 60 * lift))])
            bones[f"leg_{pair}_{side}_lower"] = channel(rotation=[
                (0, (0, 0, 0)), (start, (0, 0, 0)), (start + 0.8, (0, 0, -50 * lift)), (3.0, (0, 0, -55 * lift))])
    return {"animation_length": 3.0, "bones": bones}


def main():
    animations = {
        "animation.the_layered.idle": idle(),
        "animation.the_layered.walk": walk(),
        "animation.the_layered.heads": heads(),
        "animation.the_layered.grab": grab(),
        "animation.the_layered.hold": hold(),
        "animation.the_layered.rockfall": rockfall(),
        "animation.the_layered.release": release(),
        "animation.the_layered.fallen": fallen(),
        "animation.the_layered.death": death(),
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": animations}, f, indent="\t")
        f.write("\n")
    print(f"{len(animations)} animations -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
