"""The Hollow's animations (GeckoLib / Bedrock animation format 1.8.0).

    python tools/textures/hollow_animations.py        (from the repository root)

Writes src/main/resources/assets/corruptedadventure/animations/entity/the_hollow.animation.json. Names and lengths match
entity/hollow/HollowAnimations and the server's timings: the slam lands at 0.6 s (slamWindupTicks 12), the rift step goes
at 1.0 s (riftStepWarningTicks 20), the swap at 0.75 s (swapWarningTicks 15), death lasts 3.0 s (DEATH_TICKS 60).

Bedrock conventions (checked in Blockbench): a negative x rotation swings a limb forward (toward the face, -z); a positive x
rotation tips the top of a bone forward. The root bone pivots on the heel line, so a negative x tips the whole body
backwards. Keyframes are linear; quick "jitter" keys 0.05 s apart make a limb skip a few pixels, as if it is not quite
in this dimension.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/animations/entity/the_hollow.animation.json")


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


def idle():
    return {"loop": True, "animation_length": 4.0, "bones": {
        "body": channel(rotation=[(0, (0, 0, 0)), (2.0, (2, 0, 1)), (4.0, (0, 0, 0))]),
        "chest_void": channel(scale=[(0, (1, 1, 1)), (2.0, (1.06, 1.06, 1)), (4.0, (1, 1, 1))]),
        # The broken neck: the head lolls, and now and then it twitches.
        "head": channel(rotation=[(0, (0, 0, 0)), (1.5, (2, 0, 4)), (2.6, (0, 0, 1)), (2.65, (6, 0, 10)), (2.75, (0, 0, 1)),
                                  (4.0, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (2.0, (-3, 0, -1.5)), (4.0, (0, 0, 0))]),
        "hand_giant": channel(rotation=[(0, (0, 0, 0)), (2.2, (4, 0, 0)), (4.0, (0, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (1.2, (-4, 0, 5)), (2.4, (0, 0, 0)), (4.0, (0, 0, 0))]),
        "arm_thin_lower": channel(position=[(0, (0, 0, 0)), (1.0, (0, 0, 0)), (1.05, (1, 0, 0)), (1.2, (1, 0, 0)),
                                            (1.25, (0, 0, 0)), (4.0, (0, 0, 0))]),
        "hand_thin": channel(rotation=[(0, (0, 0, 0)), (3.1, (0, 0, 0)), (3.15, (-25, 0, 0)), (3.3, (0, 0, 0)),
                                       (4.0, (0, 0, 0))]),
        "glitch_fx": channel(position=[(0, (0, 0, 0)), (0.7, (0, 0, 0)), (0.75, (1, -1, 0)), (0.85, (0, 0, 0)),
                                       (2.9, (0, 0, 0)), (2.95, (-1, 2, 0)), (3.05, (0, 0, 0)), (4.0, (0, 0, 0))]),
    }}


def walk():
    return {"loop": True, "animation_length": 2.0, "bones": {
        # Heavy stomps at 0 and 1 s: the whole body drops onto the foot.
        "root": channel(position=[(0, (0, -1.5, 0)), (0.15, (0, -0.5, 0)), (0.5, (0, 0.5, 0)), (1.0, (0, -1.5, 0)),
                                  (1.15, (0, -0.5, 0)), (1.5, (0, 0.5, 0)), (2.0, (0, -1.5, 0))]),
        "body": channel(rotation=[(0, (5, 0, 3)), (1.0, (5, 0, -3)), (2.0, (5, 0, 3))]),
        "head": channel(rotation=[(0, (0, 0, -3)), (0.3, (0, 0, 5)), (1.0, (0, 0, 2)), (1.3, (0, 0, 7)),
                                  (2.0, (0, 0, -3))]),
        "leg_left": channel(rotation=[(0, (18, 0, 0)), (1.0, (-22, 0, 0)), (2.0, (18, 0, 0))]),
        "leg_left_lower": channel(rotation=[(0, (0, 0, 0)), (0.5, (30, 0, 0)), (1.0, (4, 0, 0)), (2.0, (0, 0, 0))]),
        "leg_right": channel(rotation=[(0, (-22, 0, 0)), (1.0, (18, 0, 0)), (2.0, (-22, 0, 0))]),
        "leg_right_lower": channel(rotation=[(0, (4, 0, 0)), (1.0, (0, 0, 0)), (1.5, (30, 0, 0)), (2.0, (4, 0, 0))]),
        # The giant arm drags its knuckles; it swings little and late.
        "arm_giant": channel(rotation=[(0, (6, 0, -2)), (1.0, (-6, 0, 1)), (2.0, (6, 0, -2))]),
        "hand_giant": channel(rotation=[(0, (-6, 0, 0)), (1.0, (8, 0, 0)), (2.0, (-6, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (-14, 0, 0)), (1.0, (14, 0, 4)), (2.0, (-14, 0, 0))]),
        "arm_thin_lower": channel(rotation=[(0, (0, 0, 0)), (1.0, (-12, 0, 0)), (2.0, (0, 0, 0))]),
    }}


def slam():
    return {"animation_length": 1.3, "bones": {
        "root": channel(position=[(0, (0, 0, 0)), (0.6, (0, 0, 0)), (0.65, (0, -2, 0)), (0.9, (0, 0, 0))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (0.45, (-10, 0, -4)), (0.6, (14, 0, 3)), (0.9, (9, 0, 2)),
                                  (1.3, (0, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (0.45, (-6, 0, -10)), (0.64, (8, 0, 14)), (1.3, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (0.3, (-60, 0, -12)), (0.5, (-110, 0, -16)), (0.6, (12, 0, 0)),
                                       (0.85, (8, 0, 0)), (1.3, (0, 0, 0))]),
        "arm_giant_lower": channel(rotation=[(0, (0, 0, 0)), (0.5, (-35, 0, 0)), (0.6, (0, 0, 0)), (1.3, (0, 0, 0))]),
        "hand_giant": channel(rotation=[(0, (0, 0, 0)), (0.5, (-25, 0, 0)), (0.6, (20, 0, 0)), (1.0, (0, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (0.45, (10, 0, 10)), (0.65, (-10, 0, -4)), (1.3, (0, 0, 0))]),
        "leg_left": channel(rotation=[(0, (0, 0, 0)), (0.5, (8, 0, 0)), (0.62, (-8, 0, 0)), (1.3, (0, 0, 0))]),
        "leg_right": channel(rotation=[(0, (0, 0, 0)), (0.5, (-6, 0, 0)), (0.62, (10, 0, 0)), (1.3, (0, 0, 0))]),
    }}


def rift_step():
    jitter = [(0, (0, 0, 0)), (0.2, (0, 0, 0)), (0.25, (1, 0, 0)), (0.3, (0, 0, 0)), (0.45, (0, 0, 0)),
              (0.5, (-1, 0, 1)), (0.55, (0, 0, 0)), (0.7, (0, 0, 0)), (0.75, (1.5, -1, 0)), (0.8, (0, 0, 0)),
              (0.88, (-1.5, 0, -1)), (0.93, (0, 0, 0)), (1.4, (0, 0, 0))]
    return {"animation_length": 1.4, "bones": {
        # It sinks and narrows into the rift, reaching in with the giant arm; after the step it rises again.
        "root": channel(position=[(0, (0, 0, 0)), (0.9, (0, -4, 0)), (1.0, (0, -4, 0)), (1.4, (0, 0, 0))],
                        scale=[(0, (1, 1, 1)), (0.95, (0.82, 1.06, 0.82)), (1.0, (0.82, 1.06, 0.82)),
                               (1.2, (1, 1, 1))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (0.9, (22, 0, 0)), (1.0, (22, 0, 0)), (1.4, (0, 0, 0))],
                        position=jitter),
        "head": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 0, 14)), (0.35, (0, 0, -6)), (0.6, (0, 0, 10)),
                                  (0.65, (0, 0, -10)), (1.0, (0, 0, 4)), (1.4, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (0.6, (-55, 0, 0)), (0.95, (-75, 0, 4)), (1.0, (-75, 0, 4)),
                                       (1.4, (0, 0, 0))]),
        "hand_giant": channel(rotation=[(0, (0, 0, 0)), (0.6, (-20, 0, 0)), (1.0, (-30, 0, 0)), (1.4, (0, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (0.8, (12, 0, 14)), (1.4, (0, 0, 0))]),
        "glitch_fx": channel(position=[(0, (0, 0, 0)), (0.3, (2, 1, 0)), (0.35, (-1, -2, 1)), (0.6, (1, 3, -1)),
                                       (0.65, (0, 0, 0)), (1.4, (0, 0, 0))],
                             scale=[(0, (1, 1, 1)), (0.9, (1.6, 1.6, 1.6)), (1.0, (1, 1, 1))]),
    }}


def phase():
    return {"loop": True, "animation_length": 1.0, "bones": {
        "root": channel(position=[(0, (0, 0, 0)), (0.1, (0, 0, 0)), (0.12, (1, 0, 0)), (0.17, (1, 0, 0)), (0.19, (0, 0, 0)),
                                  (0.55, (0, 0, 0)), (0.57, (-1, 0, 1)), (0.62, (-1, 0, 1)), (0.64, (0, 0, 0)),
                                  (1.0, (0, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (0.7, (0, 0, 0)), (0.72, (0, 0, -12)), (0.8, (0, 0, -12)),
                                  (0.82, (0, 0, 0)), (1.0, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (0.3, (0, 0, 0)), (0.32, (-4, 0, 6)), (0.4, (-4, 0, 6)),
                                       (0.42, (0, 0, 0)), (1.0, (0, 0, 0))]),
        "arm_thin_lower": channel(position=[(0, (0, 0, 0)), (0.45, (0, 0, 0)), (0.47, (-1, 1, 0)), (0.52, (-1, 1, 0)),
                                            (0.54, (0, 0, 0)), (1.0, (0, 0, 0))]),
        "chest_void": channel(scale=[(0, (1, 1, 1)), (0.5, (1.15, 1.15, 1)), (1.0, (1, 1, 1))]),
        "glitch_fx": channel(scale=[(0, (1.4, 1.4, 1.4)), (0.5, (1.8, 1.8, 1.8)), (1.0, (1.4, 1.4, 1.4))]),
    }}


def swap():
    return {"animation_length": 0.9, "bones": {
        # The withered arm points, the chest's void pulls, and at 0.75 s the players change places.
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (0.35, (-95, 0, -10)), (0.7, (-100, 0, -12)),
                                      (0.75, (-80, 0, -4)), (0.9, (0, 0, 0))]),
        "arm_thin_mid": channel(rotation=[(0, (0, 0, 0)), (0.35, (30, 0, 0)), (0.75, (10, 0, 0)), (0.9, (0, 0, 0))]),
        "hand_thin": channel(rotation=[(0, (0, 0, 0)), (0.35, (-20, 0, 0)), (0.7, (-30, 0, 0)), (0.75, (15, 0, 0)),
                                       (0.9, (0, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (0.3, (-6, 0, 15)), (0.74, (-6, 0, 15)), (0.77, (4, 0, -8)),
                                  (0.9, (0, 0, 0))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (0.4, (-6, 0, 0)), (0.9, (0, 0, 0))]),
        "chest_void": channel(scale=[(0, (1, 1, 1)), (0.7, (1.3, 1.3, 1)), (0.8, (0.9, 0.9, 1)), (0.9, (1, 1, 1))]),
    }}


def death():
    return {"animation_length": 3.0, "bones": {
        # Staggers, catches itself, then topples backwards over its heels and lies still; the void in its chest closes.
        "root": channel(rotation=[(0, (0, 0, 0)), (0.5, (-12, 0, 3)), (0.9, (-7, 0, 2)), (1.4, (-24, 0, 4)),
                                  (1.8, (-50, 0, 6)), (2.15, (-90, 0, 8)), (2.3, (-83, 0, 8)), (2.45, (-88, 0, 8)),
                                  (3.0, (-88, 0, 8))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (0.5, (10, 0, 0)), (1.4, (4, 0, 0)), (2.2, (-6, 0, 0)),
                                  (3.0, (-6, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (0.5, (10, 0, 12)), (1.8, (0, 0, 8)), (2.2, (-15, 0, 25)),
                                  (3.0, (-15, 0, 25))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (0.6, (-25, 0, -8)), (1.4, (-15, 0, -5)), (2.2, (-70, 0, -20)),
                                       (2.4, (-60, 0, -18)), (3.0, (-60, 0, -18))]),
        "hand_giant": channel(rotation=[(0, (0, 0, 0)), (2.2, (-20, 0, 0)), (3.0, (-20, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (0.6, (-30, 0, 15)), (2.2, (-40, 0, 30)), (3.0, (-40, 0, 30))]),
        "leg_left": channel(rotation=[(0, (0, 0, 0)), (0.5, (10, 0, 0)), (1.4, (0, 0, 0)), (2.2, (-25, 0, 0)),
                                      (3.0, (-25, 0, 0))]),
        "leg_left_lower": channel(rotation=[(0, (0, 0, 0)), (2.2, (20, 0, 0)), (3.0, (20, 0, 0))]),
        "leg_right": channel(rotation=[(0, (0, 0, 0)), (0.5, (-8, 0, 0)), (2.2, (-10, 0, 0)), (3.0, (-10, 0, 0))]),
        "chest_void": channel(scale=[(0, (1, 1, 1)), (2.0, (1, 1, 1)), (2.8, (0.15, 0.15, 1)), (3.0, (0.15, 0.15, 1))]),
        "glitch_fx": channel(scale=[(0, (1, 1, 1)), (1.8, (1.5, 1.5, 1.5)), (2.6, (0, 0, 0)), (3.0, (0, 0, 0))]),
    }}


def main():
    animations = {
        "animation.the_hollow.idle": idle(),
        "animation.the_hollow.walk": walk(),
        "animation.the_hollow.slam": slam(),
        "animation.the_hollow.rift_step": rift_step(),
        "animation.the_hollow.phase": phase(),
        "animation.the_hollow.swap": swap(),
        "animation.the_hollow.death": death(),
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": animations}, f, indent="\t")
        f.write("\n")
    print(f"{len(animations)} animations -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
