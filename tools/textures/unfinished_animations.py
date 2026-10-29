"""The Unfinished's animations (GeckoLib / Bedrock animation format 1.8.0).

    python tools/textures/unfinished_animations.py        (from the repository root)

Writes src/main/resources/assets/corruptedadventure/animations/entity/the_unfinished.animation.json. Names and lengths
match entity/unfinished/UnfinishedAnimations and the server's timings ([nightmares.unfinished] defaults): the slam lands
at 1.25 s (slamWindupTicks 25), the rift sink holds from 1.5 s (riftStepSinkTicks 30) until the rise, the grab closes at
0.8 s (grabReachTicks 16) and lets go at 3.8 s (grabHoldTicks 60), the ash rain, the lure and the void pull last as long
as their spells start them, death lasts 5.0 s (DEATH_TICKS 100). The life round (L5) adds the glitch step's loop, the
throw (released at 1.5 s, throwRiseTicks 30) and the unloading (the patch opens at 2.0 s, unloadWarnTicks 40).

Conventions as in hollow_animations.py (checked there in Blockbench): a negative x rotation swings a limb forward (toward
the face, -z); a positive x rotation tips the top of a bone forward. Keyframes are linear; "stutter" keys 0.05 s apart make
a part skip, as if it has not finished loading. The flicker of the unloaded parts and the swap to their loaded variants
is not here: the renderer does it from the synced state, so it never fights these animations.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/animations/entity/the_unfinished.animation.json")


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


def stutter(times, offset, length):
    """Position keys that sit at rest and jump by offset for 0.05 s at each time: a part that skips."""
    out = [(0, (0, 0, 0))]
    for t in times:
        out += [(t, (0, 0, 0)), (t + 0.02, offset), (t + 0.07, offset), (t + 0.09, (0, 0, 0))]
    out.append((length, (0, 0, 0)))
    return out


def idle():
    length = 6.0
    return {"loop": True, "animation_length": length, "bones": {
        # A slow, heavy breath; the torso leans and settles.
        "torso": channel(rotation=[(0, (0, 0, 0)), (3.0, (2, 0, -1.5)), (6.0, (0, 0, 0))]),
        "torso_upper": channel(rotation=[(0, (0, 0, 0)), (3.0, (-2, 0, 1)), (6.0, (0, 0, 0))]),
        "crown_core": channel(scale=[(0, (1, 1, 1)), (1.5, (1.08, 1.08, 1.08)), (3.0, (1, 1, 1)), (4.5, (1.1, 1.1, 1.1)),
                                     (6.0, (1, 1, 1))]),
        # The four heads each turn and snap on their own.
        "head_holle": channel(rotation=[(0, (0, 0, 0)), (1.2, (6, 12, 4)), (2.0, (6, 12, 4)), (2.05, (14, 8, 10)),
                                        (2.2, (6, 12, 4)), (4.0, (-4, -8, 0)), (6.0, (0, 0, 0))]),
        "head_gelaagde": channel(rotation=[(0, (0, 0, 0)), (0.8, (-10, -15, 0)), (1.0, (12, -15, 0)), (1.2, (-10, -15, 0)),
                                           (3.6, (0, 10, -6)), (6.0, (0, 0, 0))]),
        "head_herrijzende": channel(rotation=[(0, (0, 0, 0)), (2.5, (-8, 18, 0)), (3.4, (-8, 18, 0)), (3.5, (6, 10, 4)),
                                              (6.0, (0, 0, 0))]),
        "head_hoeder": channel(rotation=[(0, (0, 0, 0)), (1.8, (5, -14, -4)), (3.8, (-6, 6, 4)), (4.2, (12, 6, 4)),
                                         (4.5, (-6, 6, 4)), (6.0, (0, 0, 0))]),
        "graft_holle_arm": channel(rotation=[(0, (0, 0, 0)), (3.0, (-3, 0, 2)), (6.0, (0, 0, 0))]),
        "graft_holle_hand": channel(rotation=[(0, (0, 0, 0)), (2.6, (6, 0, 0)), (6.0, (0, 0, 0))]),
        # The Layered's legs tap the ground, one after the other.
        "graft_gelaagde_leg_1": channel(rotation=[(0, (0, 0, 0)), (1.0, (0, 0, 0)), (1.3, (0, 0, -5)), (1.6, (0, 0, 0)),
                                                  (6.0, (0, 0, 0))]),
        "graft_gelaagde_leg_2": channel(rotation=[(0, (0, 0, 0)), (1.4, (0, 0, 0)), (1.7, (0, 0, -5)), (2.0, (0, 0, 0)),
                                                  (6.0, (0, 0, 0))]),
        "graft_gelaagde_leg_3": channel(rotation=[(0, (0, 0, 0)), (1.8, (0, 0, 0)), (2.1, (0, 0, -5)), (2.4, (0, 0, 0)),
                                                  (6.0, (0, 0, 0))]),
        "graft_herrijzende_wing": channel(rotation=[(0, (0, 0, 0)), (3.0, (0, -6, 4)), (6.0, (0, 0, 0))]),
        "graft_hoeder_tail": channel(rotation=[(0, (0, 0, 0)), (2.0, (0, 8, 0)), (4.0, (0, -6, 2)), (6.0, (0, 0, 0))]),
        "debris": channel(rotation=[(0, (0, 0, 0)), (6.0, (0, 360, 0))],
                          position=[(0, (0, 0, 0)), (3.0, (0, 8, 0)), (6.0, (0, 0, 0))]),
        "rift": channel(scale=[(0, (1, 1, 1)), (3.0, (1.04, 1, 1.04)), (6.0, (1, 1, 1))]),
        "unloaded_slice_1": channel(position=stutter([0.7, 3.1, 4.4], (6, 0, -2), length)),
        "unloaded_slice_2": channel(position=stutter([1.6, 2.2, 5.3], (-5, 1, -2), length)),
        "unloaded_slice_3": channel(position=stutter([0.3, 3.9], (4, -1, -3), length)),
    }}


def walk():
    length = 3.0
    return {"loop": True, "animation_length": length, "bones": {
        # One great step per 1.5 s: the body heaves up, lurches forward and drops, the giant hand plants and drags.
        "torso": channel(rotation=[(0, (6, 0, 4)), (0.5, (-2, 0, 0)), (1.5, (6, 0, -4)), (2.0, (-2, 0, 0)), (3.0, (6, 0, 4))],
                         position=[(0, (0, -6, 0)), (0.2, (0, -2, 0)), (0.6, (0, 6, 0)), (1.5, (0, -6, 0)), (1.7, (0, -2, 0)),
                                   (2.1, (0, 6, 0)), (3.0, (0, -6, 0))]),
        "torso_upper": channel(rotation=[(0, (-3, 0, -2)), (1.5, (-3, 0, 2)), (3.0, (-3, 0, -2))]),
        "graft_holle_arm": channel(rotation=[(0, (-25, 0, 0)), (0.6, (-40, 0, 4)), (1.5, (10, 0, 0)), (3.0, (-25, 0, 0))]),
        "graft_holle_arm_lower": channel(rotation=[(0, (-10, 0, 0)), (0.6, (-25, 0, 0)), (1.5, (6, 0, 0)), (3.0, (-10, 0, 0))]),
        "graft_holle_hand": channel(rotation=[(0, (10, 0, 0)), (0.6, (-15, 0, 0)), (1.5, (20, 0, 0)), (3.0, (10, 0, 0))]),
        "graft_gelaagde_leg_1": channel(rotation=[(0, (-12, 0, 0)), (0.5, (-20, 0, -14)), (1.0, (14, 0, 0)), (3.0, (-12, 0, 0))]),
        "graft_gelaagde_leg_1_lower": channel(rotation=[(0, (0, 0, 0)), (0.5, (-15, 0, 0)), (1.0, (0, 0, 0)), (3.0, (0, 0, 0))]),
        "graft_gelaagde_leg_2": channel(rotation=[(0, (8, 0, 0)), (1.0, (-14, 0, 0)), (1.5, (-18, 0, -14)), (2.0, (12, 0, 0)),
                                                  (3.0, (8, 0, 0))]),
        "graft_gelaagde_leg_2_lower": channel(rotation=[(0, (0, 0, 0)), (1.5, (-15, 0, 0)), (2.0, (0, 0, 0)), (3.0, (0, 0, 0))]),
        "graft_gelaagde_leg_3": channel(rotation=[(0, (14, 0, 0)), (2.0, (-12, 0, 0)), (2.5, (-20, 0, -14)), (3.0, (14, 0, 0))]),
        "graft_gelaagde_leg_3_lower": channel(rotation=[(0, (0, 0, 0)), (2.5, (-15, 0, 0)), (3.0, (0, 0, 0))]),
        "graft_herrijzende_wing": channel(rotation=[(0, (0, -4, 6)), (0.6, (0, 4, -4)), (1.5, (0, -4, 6)), (2.1, (0, 4, -4)),
                                                    (3.0, (0, -4, 6))]),
        "graft_hoeder_tail": channel(rotation=[(0, (0, 10, 0)), (1.5, (0, -10, 0)), (3.0, (0, 10, 0))]),
        "head_holle": channel(rotation=[(0, (0, 0, 0)), (0.6, (8, 0, 6)), (1.5, (0, 0, 0)), (2.1, (8, 0, -4)), (3.0, (0, 0, 0))]),
        "head_hoeder": channel(rotation=[(0, (0, -6, 0)), (1.5, (0, 6, 0)), (3.0, (0, -6, 0))]),
        "rift": channel(scale=[(0, (1, 1, 1)), (0.2, (1.08, 1, 1.08)), (0.6, (1, 1, 1)), (1.7, (1.08, 1, 1.08)), (2.1, (1, 1, 1)),
                               (3.0, (1, 1, 1))]),
    }}


def slam():
    return {"animation_length": 2.2, "bones": {
        # The Hollow's arm rises high, the body twists into it, and at 1.25 s the fist comes down in front of him.
        "torso": channel(rotation=[(0, (0, 0, 0)), (1.0, (-8, -14, -4)), (1.25, (12, 10, 4)), (1.6, (10, 8, 3)), (2.2, (0, 0, 0))],
                         position=[(0, (0, 0, 0)), (1.25, (0, 0, 0)), (1.3, (0, -8, 0)), (1.7, (0, 0, 0))]),
        "graft_holle_arm": channel(rotation=[(0, (0, 0, 0)), (0.6, (-90, 0, -10)), (1.05, (-150, 0, -20)), (1.25, (-50, 0, 14)),
                                             (1.6, (-48, 0, 12)), (2.2, (0, 0, 0))]),
        "graft_holle_arm_lower": channel(rotation=[(0, (0, 0, 0)), (1.05, (-40, 0, 0)), (1.25, (-10, 0, 0)), (2.2, (0, 0, 0))]),
        "graft_holle_hand": channel(rotation=[(0, (0, 0, 0)), (1.05, (-30, 0, 0)), (1.25, (25, 0, 0)), (1.8, (20, 0, 0)),
                                              (2.2, (0, 0, 0))]),
        "head_holle": channel(rotation=[(0, (0, 0, 0)), (1.0, (-20, 20, 0)), (1.25, (18, 0, 6)), (2.2, (0, 0, 0))]),
        "head_gelaagde": channel(rotation=[(0, (0, 0, 0)), (1.2, (-14, 0, 0)), (1.3, (16, 0, 0)), (2.2, (0, 0, 0))]),
        "head_herrijzende": channel(rotation=[(0, (0, 0, 0)), (1.25, (10, 0, -8)), (2.2, (0, 0, 0))]),
        "graft_herrijzende_wing": channel(rotation=[(0, (0, 0, 0)), (1.0, (0, 10, 12)), (1.3, (0, -6, -10)), (2.2, (0, 0, 0))]),
        "debris": channel(position=[(0, (0, 0, 0)), (1.25, (0, 0, 0)), (1.4, (0, 14, 0)), (2.2, (0, 0, 0))]),
    }}


def rift_sink():
    return {"animation_length": 1.5, "loop": "hold_on_last_frame", "bones": {
        # Verspringen (the Hollow's memory): he sinks into his own void, head last; the hand claws at the rim.
        "torso": channel(position=[(0, (0, 0, 0)), (0.3, (0, 8, 0)), (1.4, (0, -330, 0)), (1.5, (0, -340, 0))],
                         rotation=[(0, (0, 0, 0)), (0.3, (-8, 0, 0)), (1.4, (14, 0, 6)), (1.5, (14, 0, 6))]),
        "graft_holle_arm": channel(rotation=[(0, (0, 0, 0)), (0.5, (-120, 0, -10)), (1.1, (-170, 0, -10)), (1.5, (-170, 0, -10))]),
        "rift": channel(scale=[(0, (1, 1, 1)), (0.4, (1.3, 1, 1.3)), (1.5, (0.6, 1, 0.6))]),
        "debris": channel(position=[(0, (0, 0, 0)), (1.5, (0, -200, 0))]),
        "crown_core": channel(position=stutter([0.4, 0.75, 1.1], (6, -4, 0), 1.5)),
    }}


def rift_rise():
    return {"animation_length": 1.5, "bones": {
        "torso": channel(position=[(0, (0, -340, 0)), (0.9, (0, -40, 0)), (1.2, (0, 10, 0)), (1.5, (0, 0, 0))],
                         rotation=[(0, (14, 0, -6)), (1.0, (-8, 0, 0)), (1.5, (0, 0, 0))]),
        "graft_holle_arm": channel(rotation=[(0, (-170, 0, -10)), (0.7, (-150, 0, 0)), (1.1, (-30, 0, 10)), (1.5, (0, 0, 0))]),
        "graft_holle_hand": channel(rotation=[(0, (0, 0, 0)), (1.1, (25, 0, 0)), (1.5, (0, 0, 0))]),
        "rift": channel(scale=[(0, (0.6, 1, 0.6)), (0.3, (1.4, 1, 1.4)), (1.5, (1, 1, 1))]),
        "debris": channel(position=[(0, (0, -200, 0)), (1.5, (0, 0, 0))]),
    }}


def grab():
    return {"animation_length": 4.4, "bones": {
        # Grijpen (the Layered's memory): the front leg swings out and forward, the claw closes at 0.8 s, then he lifts
        # his prey high and lets go at 3.8 s.
        "torso": channel(rotation=[(0, (0, 0, 0)), (0.6, (6, 20, -6)), (0.8, (8, 24, -8)), (3.8, (-6, 10, -4)), (4.4, (0, 0, 0))]),
        "graft_gelaagde_leg_1": channel(rotation=[(0, (0, 0, 0)), (0.5, (-55, -20, -30)), (0.8, (-70, -20, -10)),
                                                  (2.0, (-100, -10, -40)), (3.8, (-115, -10, -50)), (4.0, (-90, 0, -20)),
                                                  (4.4, (0, 0, 0))]),
        "graft_gelaagde_leg_1_lower": channel(rotation=[(0, (0, 0, 0)), (0.5, (30, 0, 0)), (0.8, (-20, 0, 0)), (3.8, (-30, 0, 0)),
                                                        (4.0, (20, 0, 0)), (4.4, (0, 0, 0))]),
        "head_gelaagde": channel(rotation=[(0, (0, 0, 0)), (0.8, (-20, -20, 0)), (0.85, (15, -20, 0)), (1.0, (-20, -20, 0)),
                                           (3.8, (-30, -10, 0)), (4.4, (0, 0, 0))]),
        "graft_gelaagde_leg_2": channel(rotation=[(0, (0, 0, 0)), (0.8, (10, 0, 6)), (3.8, (10, 0, 6)), (4.4, (0, 0, 0))]),
    }}


def ash_rain():
    return {"animation_length": 3.2, "bones": {
        # Asregen (the Risen's memory): the burnt wing unfolds high and beats three times; the ash falls from 1 s.
        "graft_herrijzende_wing": channel(rotation=[(0, (0, 0, 0)), (0.6, (0, -20, 35)), (1.0, (0, 10, -20)), (1.4, (0, -20, 35)),
                                                    (1.8, (0, 10, -20)), (2.2, (0, -20, 35)), (2.6, (0, 10, -20)),
                                                    (3.2, (0, 0, 0))]),
        "head_herrijzende": channel(rotation=[(0, (0, 0, 0)), (0.5, (-30, 0, 0)), (2.8, (-30, 0, 0)), (3.2, (0, 0, 0))],
                                    position=stutter([1.2, 2.0], (2, 2, 0), 3.2)),
        "torso": channel(rotation=[(0, (0, 0, 0)), (1.0, (-4, 0, 6)), (2.6, (-4, 0, 6)), (3.2, (0, 0, 0))]),
        "torso_upper": channel(rotation=[(0, (0, 0, 0)), (1.0, (2, 0, 0)), (1.4, (-2, 0, 0)), (1.8, (2, 0, 0)), (2.2, (-2, 0, 0)),
                                         (3.2, (0, 0, 0))]),
    }}


def lure():
    return {"animation_length": 4.0, "bones": {
        # Lokroep (the Keeper's memory): the dragon head rises and sings, swaying slowly; the rest goes still.
        "head_hoeder": channel(rotation=[(0, (0, 0, 0)), (0.6, (-25, 0, 0)), (1.6, (-25, 12, -6)), (2.6, (-25, -12, 6)),
                                         (3.4, (-25, 0, 0)), (4.0, (0, 0, 0))],
                               scale=[(0, (1, 1, 1)), (0.6, (1.12, 1.12, 1.12)), (3.4, (1.12, 1.12, 1.12)), (4.0, (1, 1, 1))]),
        "graft_hoeder_tail": channel(rotation=[(0, (0, 0, 0)), (1.0, (0, 14, 4)), (2.0, (0, -14, -4)), (3.0, (0, 14, 4)),
                                               (4.0, (0, 0, 0))]),
        "torso": channel(rotation=[(0, (0, 0, 0)), (0.8, (-3, 0, 0)), (3.4, (-3, 0, 0)), (4.0, (0, 0, 0))]),
        "crown_core": channel(scale=[(0, (1, 1, 1)), (2.0, (0.9, 0.9, 0.9)), (4.0, (1, 1, 1))]),
    }}


def void_pull():
    return {"animation_length": 4.0, "bones": {
        # Naar de void trekken: the crown's void core swells and the four heads scream outward; the rift gapes.
        "crown_core": channel(scale=[(0, (1, 1, 1)), (0.6, (1.6, 1.6, 1.6)), (3.4, (1.7, 1.7, 1.7)), (4.0, (1, 1, 1))],
                              position=stutter([0.8, 1.5, 2.1, 2.9], (3, 3, 0), 4.0)),
        "head_holle": channel(rotation=[(0, (0, 0, 0)), (0.6, (-20, 30, 20)), (3.4, (-20, 30, 20)), (4.0, (0, 0, 0))]),
        "head_gelaagde": channel(rotation=[(0, (0, 0, 0)), (0.6, (-25, 25, 0)), (3.4, (-25, 25, 0)), (4.0, (0, 0, 0))]),
        "head_herrijzende": channel(rotation=[(0, (0, 0, 0)), (0.6, (-35, -20, -15)), (3.4, (-35, -20, -15)), (4.0, (0, 0, 0))]),
        "head_hoeder": channel(rotation=[(0, (0, 0, 0)), (0.6, (-25, -30, -15)), (3.4, (-25, -30, -15)), (4.0, (0, 0, 0))]),
        "torso": channel(rotation=[(0, (0, 0, 0)), (0.6, (-8, 0, 0)), (3.4, (-8, 0, 0)), (4.0, (0, 0, 0))]),
        "rift": channel(scale=[(0, (1, 1, 1)), (0.6, (1.5, 1, 1.5)), (3.4, (1.5, 1, 1.5)), (4.0, (1, 1, 1))]),
        "debris": channel(rotation=[(0, (0, 0, 0)), (4.0, (0, -720, 0))],
                          position=[(0, (0, 0, 0)), (0.6, (0, 30, 0)), (3.4, (0, 30, 0)), (4.0, (0, 0, 0))]),
    }}


def flinch():
    return {"animation_length": 1.2, "bones": {
        # A graft tears off (or a grab is broken): the whole body jerks and stutters.
        "torso": channel(rotation=[(0, (0, 0, 0)), (0.1, (-10, 8, 8)), (0.15, (6, -4, -4)), (0.25, (-6, 2, 6)), (1.2, (0, 0, 0))],
                         position=stutter([0.3, 0.5, 0.7], (8, 0, 4), 1.2)),
        "crown_core": channel(scale=[(0, (1, 1, 1)), (0.1, (1.4, 1.4, 1.4)), (0.4, (0.8, 0.8, 0.8)), (1.2, (1, 1, 1))]),
    }}


def glitch():
    length = 1.0
    return {"loop": True, "animation_length": length, "bones": {
        # Glitchsprong: he has frozen mid-move and jerks in place, frame by frame; the heads and the crown skip on their
        # own beat (stepped keys, no easing), as if the parts load at different speeds. The server moves him meanwhile.
        "torso": channel(rotation=[(0, (4, 0, -3)), (0.15, (4, 0, -3)), (0.16, (-6, 8, 5)), (0.4, (-6, 8, 5)),
                                   (0.41, (2, -10, 0)), (0.7, (2, -10, 0)), (0.71, (8, 4, -6)), (1.0, (4, 0, -3))],
                         position=stutter([0.1, 0.35, 0.55, 0.8], (10, 0, -4), length)),
        "torso_upper": channel(position=stutter([0.2, 0.62], (-6, 3, 2), length)),
        "crown_core": channel(scale=[(0, (1, 1, 1)), (0.2, (1, 1, 1)), (0.21, (1.5, 0.6, 1.5)), (0.3, (1.5, 0.6, 1.5)),
                                     (0.31, (1, 1, 1)), (0.65, (1, 1, 1)), (0.66, (0.6, 1.4, 0.6)), (0.75, (0.6, 1.4, 0.6)),
                                     (0.76, (1, 1, 1)), (1.0, (1, 1, 1))]),
        "head_holle": channel(position=stutter([0.05, 0.45, 0.85], (4, -2, 0), length)),
        "head_gelaagde": channel(position=stutter([0.25, 0.6], (-4, 2, 2), length)),
        "head_herrijzende": channel(position=stutter([0.15, 0.5, 0.9], (3, 3, 0), length)),
        "head_hoeder": channel(position=stutter([0.3, 0.75], (-3, -3, 2), length)),
        "graft_holle_arm": channel(position=stutter([0.12, 0.58], (6, 0, 4), length)),
        "graft_herrijzende_wing": channel(position=stutter([0.32, 0.88], (-8, 4, 0), length)),
        "graft_hoeder_tail": channel(position=stutter([0.42, 0.95], (0, 0, 8), length)),
        "rift": channel(scale=[(0, (1.2, 1, 1.2)), (0.5, (1.2, 1, 1.2)), (0.51, (0.8, 1, 0.8)), (1.0, (1.2, 1, 1.2))]),
    }}


def throw():
    return {"animation_length": 2.2, "bones": {
        # Brokken gooien: a memory tears off his chest (0 s), he rears back and lifts it over his crown with his arm and
        # wing (until 1.5 s, throwRiseTicks 30), then hurls himself forward and the arm swings through.
        "torso": channel(rotation=[(0, (0, 0, 0)), (0.2, (6, 0, 0)), (1.3, (-16, 0, 4)), (1.5, (-18, 0, 4)), (1.65, (18, 0, -4)),
                                   (2.2, (0, 0, 0))],
                         position=[(0, (0, 0, 0)), (0.2, (0, 0, 0)), (0.25, (0, 0, 4)), (0.3, (0, 0, 0)), (2.2, (0, 0, 0))]),
        "torso_upper": channel(rotation=[(0, (0, 0, 0)), (1.3, (-8, 0, 0)), (1.65, (10, 0, 0)), (2.2, (0, 0, 0))]),
        "graft_holle_arm": channel(rotation=[(0, (0, 0, 0)), (0.5, (-110, 0, -20)), (1.3, (-175, 0, -15)), (1.5, (-178, 0, -15)),
                                             (1.7, (-40, 0, 10)), (2.2, (0, 0, 0))]),
        "graft_holle_arm_lower": channel(rotation=[(0, (0, 0, 0)), (1.3, (-30, 0, 0)), (1.7, (0, 0, 0)), (2.2, (0, 0, 0))]),
        "graft_holle_hand": channel(rotation=[(0, (0, 0, 0)), (1.3, (-40, 0, 0)), (1.7, (30, 0, 0)), (2.2, (0, 0, 0))]),
        "graft_herrijzende_wing": channel(rotation=[(0, (0, 0, 0)), (1.3, (0, -15, 40)), (1.7, (0, 10, -25)), (2.2, (0, 0, 0))]),
        "crown_core": channel(scale=[(0, (1, 1, 1)), (0.1, (1.4, 1.4, 1.4)), (0.3, (1, 1, 1)), (1.5, (1.2, 1.2, 1.2)),
                                     (1.6, (1, 1, 1))]),
        "head_holle": channel(rotation=[(0, (0, 0, 0)), (1.3, (-30, 10, 0)), (1.65, (20, 0, 0)), (2.2, (0, 0, 0))]),
        "head_gelaagde": channel(rotation=[(0, (0, 0, 0)), (1.3, (-30, -10, 0)), (1.65, (20, 0, 0)), (2.2, (0, 0, 0))]),
        "head_herrijzende": channel(rotation=[(0, (0, 0, 0)), (1.3, (-40, 0, 0)), (1.65, (15, 0, 0)), (2.2, (0, 0, 0))]),
        "head_hoeder": channel(rotation=[(0, (0, 0, 0)), (1.3, (-35, 0, 0)), (1.65, (20, 0, 0)), (2.2, (0, 0, 0))]),
        "unloaded_slice_1": channel(position=stutter([0.1, 0.2], (8, 2, -4), 2.2)),
    }}


def unload():
    return {"animation_length": 2.8, "bones": {
        # Ontladen grond: he hunches over his target and plants the Hollow's palm on the ground (0.4 s); his void core
        # shrinks as it pours into the ground; at 2.0 s (unloadWarnTicks 40) the patch gives way and he jerks back.
        "torso": channel(rotation=[(0, (0, 0, 0)), (0.4, (16, 0, -4)), (1.9, (20, 0, -4)), (2.0, (20, 0, -4)), (2.1, (-8, 0, 4)),
                                   (2.8, (0, 0, 0))],
                         position=[(0, (0, 0, 0)), (0.4, (0, -6, 0)), (2.0, (0, -8, 0)), (2.1, (0, 2, 0)), (2.8, (0, 0, 0))]),
        "graft_holle_arm": channel(rotation=[(0, (0, 0, 0)), (0.3, (-70, 0, -10)), (0.4, (-40, 0, 6)), (2.0, (-38, 0, 6)),
                                             (2.2, (-80, 0, -10)), (2.8, (0, 0, 0))]),
        "graft_holle_hand": channel(rotation=[(0, (0, 0, 0)), (0.4, (40, 0, 0)), (2.0, (40, 0, 0)), (2.8, (0, 0, 0))]),
        "crown_core": channel(scale=[(0, (1, 1, 1)), (0.4, (0.8, 0.8, 0.8)), (1.9, (0.5, 0.5, 0.5)), (2.0, (1.6, 1.6, 1.6)),
                                     (2.8, (1, 1, 1))],
                              position=stutter([0.7, 1.1, 1.4, 1.7], (3, -3, 0), 2.8)),
        "head_holle": channel(rotation=[(0, (0, 0, 0)), (0.4, (30, 0, 0)), (2.0, (30, 0, 0)), (2.1, (-25, 0, 0)), (2.8, (0, 0, 0))]),
        "head_gelaagde": channel(rotation=[(0, (0, 0, 0)), (0.4, (35, 0, 0)), (2.0, (35, 0, 0)), (2.1, (-25, 0, 0)),
                                           (2.8, (0, 0, 0))]),
        "head_herrijzende": channel(rotation=[(0, (0, 0, 0)), (0.4, (25, 0, 0)), (2.0, (25, 0, 0)), (2.1, (-30, 0, 0)),
                                              (2.8, (0, 0, 0))]),
        "head_hoeder": channel(rotation=[(0, (0, 0, 0)), (0.4, (30, 0, 0)), (2.0, (30, 0, 0)), (2.1, (-20, 0, 0)), (2.8, (0, 0, 0))]),
        "rift": channel(scale=[(0, (1, 1, 1)), (0.4, (1.3, 1, 1.3)), (2.0, (1.5, 1, 1.5)), (2.8, (1, 1, 1))]),
    }}


def death():
    return {"animation_length": 5.0, "bones": {
        # He stalls, everything stutters, the grafts sag, then he sinks back into his void, the crown last; the void
        # closes over him.
        "torso": channel(rotation=[(0, (0, 0, 0)), (0.8, (-10, 0, 8)), (1.6, (6, 0, -6)), (2.4, (14, 0, 10)), (5.0, (20, 0, 14))],
                         position=[(0, (0, 0, 0)), (1.6, (0, 0, 0)), (1.65, (6, -6, 0)), (1.75, (0, 0, 0)), (2.2, (0, 0, 0)),
                                   (4.6, (0, -360, 0)), (5.0, (0, -380, 0))]),
        "graft_holle_arm": channel(rotation=[(0, (0, 0, 0)), (1.0, (-40, 0, -20)), (2.2, (-150, 0, -10)), (5.0, (-170, 0, -10))]),
        "graft_herrijzende_wing": channel(rotation=[(0, (0, 0, 0)), (1.2, (0, 20, -30)), (2.4, (0, 30, -50)), (5.0, (0, 30, -50))]),
        "graft_hoeder_tail": channel(rotation=[(0, (0, 0, 0)), (1.6, (20, 20, 0)), (5.0, (30, 30, 0))]),
        "graft_gelaagde_leg_1": channel(rotation=[(0, (0, 0, 0)), (1.4, (0, 0, 20)), (5.0, (0, 0, 30))]),
        "graft_gelaagde_leg_2": channel(rotation=[(0, (0, 0, 0)), (1.6, (0, 0, 24)), (5.0, (0, 0, 32))]),
        "graft_gelaagde_leg_3": channel(rotation=[(0, (0, 0, 0)), (1.8, (0, 0, 18)), (5.0, (0, 0, 28))]),
        "head_holle": channel(rotation=[(0, (0, 0, 0)), (1.0, (20, 0, 30)), (5.0, (30, 0, 40))]),
        "head_gelaagde": channel(rotation=[(0, (0, 0, 0)), (1.2, (30, 0, -10)), (5.0, (40, 0, -20))]),
        "head_herrijzende": channel(rotation=[(0, (0, 0, 0)), (0.8, (-40, 0, 0)), (1.4, (25, 0, 0)), (5.0, (30, 0, 10))]),
        "head_hoeder": channel(rotation=[(0, (0, 0, 0)), (0.9, (-35, 0, 0)), (1.6, (30, 0, -20)), (5.0, (35, 0, -25))]),
        "crown_core": channel(scale=[(0, (1, 1, 1)), (0.6, (1.8, 1.8, 1.8)), (1.6, (0.4, 0.4, 0.4)), (1.7, (1.3, 1.3, 1.3)),
                                     (3.0, (0.2, 0.2, 0.2)), (5.0, (0, 0, 0))]),
        "rift": channel(scale=[(0, (1, 1, 1)), (2.0, (1.5, 1, 1.5)), (4.6, (1.2, 1, 1.2)), (5.0, (0, 1, 0))]),
        "debris": channel(position=[(0, (0, 0, 0)), (1.0, (0, 40, 0)), (5.0, (0, -260, 0))]),
    }}


def main():
    animations = {
        "animation.the_unfinished.idle": idle(),
        "animation.the_unfinished.walk": walk(),
        "animation.the_unfinished.slam": slam(),
        "animation.the_unfinished.rift_sink": rift_sink(),
        "animation.the_unfinished.rift_rise": rift_rise(),
        "animation.the_unfinished.grab": grab(),
        "animation.the_unfinished.ash_rain": ash_rain(),
        "animation.the_unfinished.lure": lure(),
        "animation.the_unfinished.void_pull": void_pull(),
        "animation.the_unfinished.flinch": flinch(),
        "animation.the_unfinished.glitch": glitch(),
        "animation.the_unfinished.throw": throw(),
        "animation.the_unfinished.unload": unload(),
        "animation.the_unfinished.death": death(),
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
