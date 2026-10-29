"""The Hollow's animations (GeckoLib / Bedrock animation format 1.8.0).

    python tools/textures/hollow_animations.py        (from the repository root)

Writes src/main/resources/assets/corruptedadventure/animations/entity/the_hollow.animation.json. Names and lengths match
entity/hollow/HollowAnimations and the server's timings: the slam lands at 0.6 s (slamWindupTicks 12), the rift step goes
at 1.0 s (riftStepWarningTicks 20), the swap at 0.75 s (swapWarningTicks 15), death and the emerging from the arrival
rift last 3.0 s (DEATH_TICKS, EMERGE_TICKS 60). The extra attacks (life round): the ground slam lands at 1.25 s
(groundSlamWindupTicks 25), the rift rain's hand is in the ground at 1.0 s (riftRainWindupTicks 20) and comes out when the
claws are up (+ riftRainWarningTicks 25), the sweep goes through at 0.75 s (sweepWindupTicks 15); each recovers in 0.75 s
(RECOVERY_TICKS 15 in HollowExtraAttackGoal).

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


def ground_slam():
    """Both arms go up over its head (the giant one and the withered one together) while it rises and arches back, then
    they come down on the ground in front of it at 1.25 s and the whole body drops with them."""
    return {"animation_length": 2.0, "bones": {
        "root": channel(position=[(0, (0, 0, 0)), (1.0, (0, 1, 0)), (1.2, (0, 1, 0)), (1.28, (0, -3, 0)), (1.6, (0, -2, 0)),
                                  (2.0, (0, 0, 0))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (1.0, (-18, 0, 2)), (1.2, (-20, 0, 2)), (1.3, (26, 0, -2)), (1.6, (20, 0, 0)),
                                  (2.0, (0, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (1.0, (-10, 0, -8)), (1.3, (12, 0, 16)), (1.4, (8, 0, 20)), (2.0, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (0.6, (-120, 0, -10)), (1.05, (-165, 0, -16)), (1.2, (-170, 0, -16)),
                                       (1.28, (8, 0, 2)), (1.6, (6, 0, 2)), (2.0, (0, 0, 0))]),
        "arm_giant_lower": channel(rotation=[(0, (0, 0, 0)), (1.05, (-25, 0, 0)), (1.28, (0, 0, 0)), (2.0, (0, 0, 0))]),
        "hand_giant": channel(rotation=[(0, (0, 0, 0)), (1.05, (-30, 0, 0)), (1.28, (25, 0, 0)), (1.7, (10, 0, 0)),
                                        (2.0, (0, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (0.6, (-110, 0, 6)), (1.05, (-155, 0, 10)), (1.2, (-158, 0, 10)),
                                      (1.28, (4, 0, 2)), (1.6, (4, 0, 2)), (2.0, (0, 0, 0))]),
        "arm_thin_mid": channel(rotation=[(0, (0, 0, 0)), (1.05, (-20, 0, 0)), (1.28, (0, 0, 0)), (2.0, (0, 0, 0))]),
        "leg_left": channel(rotation=[(0, (0, 0, 0)), (1.0, (-6, 0, 0)), (1.28, (-14, 0, 0)), (1.6, (-10, 0, 0)), (2.0, (0, 0, 0))]),
        "leg_left_lower": channel(rotation=[(0, (0, 0, 0)), (1.28, (18, 0, 0)), (1.6, (12, 0, 0)), (2.0, (0, 0, 0))]),
        "leg_right": channel(rotation=[(0, (0, 0, 0)), (1.0, (6, 0, 0)), (1.28, (-8, 0, 0)), (2.0, (0, 0, 0))]),
        "leg_right_lower": channel(rotation=[(0, (0, 0, 0)), (1.28, (14, 0, 0)), (2.0, (0, 0, 0))]),
        "chest_void": channel(scale=[(0, (1, 1, 1)), (1.2, (1.25, 1.25, 1)), (1.3, (0.9, 0.9, 1)), (2.0, (1, 1, 1))]),
        "glitch_fx": channel(scale=[(0, (1, 1, 1)), (1.2, (1.8, 1.8, 1.8)), (1.3, (2.2, 2.2, 2.2)), (1.6, (1, 1, 1))]),
    }}


def rift_rain():
    """The giant hand claws into the ground in front of it at 1.0 s and stays there, twitching, while the cracks open
    round its target; when the claws come up (2.25 s) it rips the hand out and straightens."""
    hold = [(1.0, (34, 0, 0)), (1.4, (32, 0, 1)), (1.45, (35, 0, -1)), (1.5, (32, 0, 1)), (1.9, (33, 0, 0)),
            (1.95, (30, 0, 2)), (2.0, (33, 0, 0)), (2.25, (32, 0, 0))]
    return {"animation_length": 3.0, "bones": {
        "root": channel(position=[(0, (0, 0, 0)), (0.6, (0, -3, 0)), (1.0, (0, -6, 0)), (2.25, (0, -6, 0)),
                                  (2.35, (0, -2, 0)), (3.0, (0, 0, 0))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (0.8, (30, 0, 0))] + hold + [(2.35, (10, 0, 0)), (3.0, (0, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (1.0, (15, 0, 18)), (1.6, (12, 0, 22)), (1.65, (18, 0, 8)),
                                  (1.75, (12, 0, 20)), (2.35, (-8, 0, 10)), (3.0, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (0.5, (-70, 0, -8)), (0.85, (-30, 0, -4)), (1.0, (-22, 0, -4)),
                                       (1.7, (-22, 0, -4)), (1.75, (-25, 0, -2)), (1.8, (-22, 0, -4)), (2.25, (-22, 0, -4)),
                                       (2.35, (-80, 0, -10)), (3.0, (0, 0, 0))]),
        "arm_giant_lower": channel(rotation=[(0, (0, 0, 0)), (0.5, (-20, 0, 0)), (1.0, (6, 0, 0)), (2.25, (6, 0, 0)),
                                             (2.35, (-15, 0, 0)), (3.0, (0, 0, 0))]),
        "hand_giant": channel(rotation=[(0, (0, 0, 0)), (0.85, (-35, 0, 0)), (1.0, (20, 0, 0)), (2.25, (24, 0, 0)),
                                        (2.35, (-40, 0, 0)), (3.0, (0, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (1.0, (-30, 0, 10)), (2.25, (-30, 0, 10)), (2.4, (-10, 0, 4)),
                                      (3.0, (0, 0, 0))]),
        "leg_left": channel(rotation=[(0, (0, 0, 0)), (1.0, (-20, 0, 0)), (2.25, (-20, 0, 0)), (3.0, (0, 0, 0))]),
        "leg_left_lower": channel(rotation=[(0, (0, 0, 0)), (1.0, (35, 0, 0)), (2.25, (35, 0, 0)), (3.0, (0, 0, 0))]),
        "leg_right": channel(rotation=[(0, (0, 0, 0)), (1.0, (-14, 0, 0)), (2.25, (-14, 0, 0)), (3.0, (0, 0, 0))]),
        "leg_right_lower": channel(rotation=[(0, (0, 0, 0)), (1.0, (28, 0, 0)), (2.25, (28, 0, 0)), (3.0, (0, 0, 0))]),
        # The void pours out of its chest into the ground while the hand is in it.
        "chest_void": channel(scale=[(0, (1, 1, 1)), (1.0, (1.15, 1.15, 1)), (2.2, (1.3, 1.3, 1)), (2.35, (0.9, 0.9, 1)),
                                     (3.0, (1, 1, 1))]),
        "glitch_fx": channel(scale=[(0, (1, 1, 1)), (1.0, (1.6, 1.6, 1.6)), (2.25, (1.9, 1.9, 1.9)), (2.4, (1, 1, 1))],
                             position=[(0, (0, 0, 0)), (1.2, (0, 0, 0)), (1.25, (1, -1, 0)), (1.3, (0, 0, 0)),
                                       (1.85, (0, 0, 0)), (1.9, (-1, 1, 1)), (1.95, (0, 0, 0)), (3.0, (0, 0, 0))]),
    }}


def sweep():
    """The giant arm is drawn back to its side and behind it, low, knuckles on the ground (0.55 s), tenses, then sweeps
    across its whole front at 0.75 s with the body turning into it, and follows through to the other side."""
    return {"animation_length": 1.5, "bones": {
        "root": channel(rotation=[(0, (0, 0, 0)), (0.55, (0, 15, 0)), (0.7, (0, 17, 0)), (0.85, (0, -18, 0)),
                                  (1.0, (0, -22, 0)), (1.5, (0, 0, 0))],
                        position=[(0, (0, 0, 0)), (0.55, (0, -1.5, 0)), (0.85, (0, -2, 0)), (1.5, (0, 0, 0))]),
        "body": channel(rotation=[(0, (0, 0, 0)), (0.55, (-6, 0, 4)), (0.7, (-8, 0, 5)), (0.85, (14, 0, -4)),
                                  (1.0, (12, 0, -6)), (1.5, (0, 0, 0))]),
        "head": channel(rotation=[(0, (0, 0, 0)), (0.55, (0, 0, -10)), (0.85, (6, 0, 18)), (1.0, (4, 0, 22)),
                                  (1.5, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (0, 0, 0)), (0.55, (-40, 110, 0)), (0.7, (-44, 116, 0)), (0.78, (-42, 40, 0)),
                                       (0.85, (-40, -25, 0)), (1.0, (-34, -60, 0)), (1.5, (0, 0, 0))]),
        "arm_giant_lower": channel(rotation=[(0, (0, 0, 0)), (0.55, (-15, 0, 0)), (0.85, (0, 0, 0)), (1.5, (0, 0, 0))]),
        "hand_giant": channel(rotation=[(0, (0, 0, 0)), (0.55, (-20, 0, 0)), (0.85, (15, 0, 0)), (1.5, (0, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (0, 0, 0)), (0.55, (-20, 0, 10)), (0.85, (25, 0, -5)), (1.5, (0, 0, 0))]),
        "leg_left": channel(rotation=[(0, (0, 0, 0)), (0.55, (10, 0, 0)), (0.85, (-12, 0, 0)), (1.5, (0, 0, 0))]),
        "leg_right": channel(rotation=[(0, (0, 0, 0)), (0.55, (-10, 0, 0)), (0.85, (12, 0, 0)), (1.5, (0, 0, 0))]),
        "glitch_fx": channel(scale=[(0, (1, 1, 1)), (0.7, (1.3, 1.3, 1.3)), (0.85, (2.0, 2.0, 2.0)), (1.1, (1, 1, 1))]),
    }}


def emerge():
    """The arrival (EMERGE_TICKS 60): the giant hand comes through the rift first and grips its edge, then The Hollow
    hauls itself out from behind the rift's plane (the root starts ~2 blocks back, hidden by the void) and straightens."""
    return {"animation_length": 3.0, "bones": {
        "root": channel(position=[(0, (0, -3, 32)), (0.9, (0, -2, 28)), (1.6, (0, -1, 15)), (2.4, (0, 0, 3)),
                                  (3.0, (0, 0, 0))]),
        "body": channel(rotation=[(0, (28, 0, 0)), (1.0, (30, 0, -6)), (1.6, (24, 0, 4)), (2.4, (8, 0, 0)), (3.0, (0, 0, 0))]),
        "head": channel(rotation=[(0, (10, 0, -12)), (1.2, (6, 0, -4)), (1.25, (6, 0, 12)), (2.2, (0, 0, 14)),
                                  (3.0, (0, 0, 0))]),
        "arm_giant": channel(rotation=[(0, (-90, 0, 0)), (0.6, (-100, 0, -10)), (1.0, (-72, 0, -20)), (1.6, (-45, 0, -16)),
                                       (2.4, (-12, 0, -4)), (3.0, (0, 0, 0))]),
        "hand_giant": channel(rotation=[(0, (-25, 0, 0)), (0.9, (-10, 0, 0)), (1.0, (30, 0, 0)), (2.2, (25, 0, 0)),
                                        (2.6, (0, 0, 0))]),
        "arm_thin": channel(rotation=[(0, (-20, 0, 8)), (1.3, (-65, 0, 22)), (1.9, (-55, 0, 18)), (2.5, (-10, 0, 4)),
                                      (3.0, (0, 0, 0))]),
        "leg_left": channel(rotation=[(0, (10, 0, 0)), (1.4, (10, 0, 0)), (1.8, (-22, 0, 0)), (2.3, (4, 0, 0)), (3.0, (0, 0, 0))]),
        "leg_left_lower": channel(rotation=[(0, (0, 0, 0)), (1.6, (28, 0, 0)), (2.3, (0, 0, 0))]),
        "leg_right": channel(rotation=[(0, (10, 0, 0)), (1.9, (10, 0, 0)), (2.3, (-20, 0, 0)), (2.8, (0, 0, 0))]),
        "leg_right_lower": channel(rotation=[(0, (0, 0, 0)), (2.1, (26, 0, 0)), (2.8, (0, 0, 0))]),
        "glitch_fx": channel(scale=[(0, (2, 2, 2)), (2.0, (1.4, 1.4, 1.4)), (3.0, (1, 1, 1))]),
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
        "animation.the_hollow.emerge": emerge(),
        "animation.the_hollow.ground_slam": ground_slam(),
        "animation.the_hollow.rift_rain": rift_rain(),
        "animation.the_hollow.sweep": sweep(),
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": animations}, f, indent="\t")
        f.write("\n")
    print(f"{len(animations)} animations -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
