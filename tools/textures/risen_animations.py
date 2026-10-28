"""The Risen's animations (GeckoLib / Bedrock animation format 1.8.0).

    python tools/textures/risen_animations.py         (from the repository root)

Writes src/main/resources/assets/corruptedadventure/animations/entity/the_risen.animation.json. Names and lengths match
entity/risen/RisenAnimations and the server's timings: the scream sounds at 1.2 s (screamWindupTicks 24), the peck lands
at 0.45 s (9 ticks), the wing buffet at 0.6 s (12 ticks), the rise lasts 5.0 s (glow 40 + rise 60 ticks), death 3.0 s
(DEATH_TICKS 60).

Bedrock conventions (as for The Hollow, checked in Blockbench): a positive x rotation tips the top of a bone forward and
swings a hanging part backward; z is as in Blockbench, so the left wing (at -x in the mockup) goes up with a negative z
and the right wing with a positive z; a negative y on the left wing (positive on the right) sweeps it back. The mockup
holds the wings in a raised V with the flight feathers hanging like a mantle: that is the perched pose. In the air the
whole bird pitches forward over its root (the middle of the body) and the feathers stream back.
"""
import json
import math
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/corruptedadventure/animations/entity/the_risen.animation.json")

PRIMARIES = {"left": [1, 2, 3, 4, 7, 8, 9, 10], "right": list(range(1, 11))}
SECONDARIES = list(range(1, 9))
TAILS = [f"tail_0{i}" for i in range(1, 6)]
CREST = [f"crest_feather_0{i}" for i in range(1, 10)]
SIDES = (("left", 1), ("right", -1))


def smooth(*pairs):
    return {str(round(float(t), 3)): {"post": [round(float(c), 3) for c in v], "lerp_mode": "catmullrom"}
            for t, v in pairs}


def linear(*pairs):
    return {str(round(float(t), 3)): [round(float(c), 3) for c in v] for t, v in pairs}


def channel(rotation=None, position=None, scale=None):
    out = {}
    if rotation:
        out["rotation"] = rotation
    if position:
        out["position"] = position
    if scale:
        out["scale"] = scale
    return out


def wave(length, steps, fn):
    """Catmull-rom keys of fn(phase 0..1) -> (x, y, z), closed so the loop has no seam."""
    return smooth(*[(length * i / steps, fn(i / steps)) for i in range(steps + 1)])


def primary(side, n):
    return f"wing_{side}_primary_{n:02d}"


def secondary(side, n):
    return f"wing_{side}_secondary_{n:02d}"


def feathers(bones, length, base_x, flutter, speed=1, steps=8):
    """Every flight feather streams back by base_x and flutters with its own phase: the figures in them move."""
    for side, sign in SIDES:
        for n in PRIMARIES[side]:
            k = n * 0.37 + (0.5 if side == "right" else 0)
            bones[primary(side, n)] = channel(rotation=wave(length, steps, lambda p, k=k: (
                base_x + flutter * math.sin((p * speed + k) * 2 * math.pi), 0,
                sign * flutter * 0.5 * math.sin((p * speed + k + 0.25) * 2 * math.pi))))
        for n in SECONDARIES:
            k = n * 0.29 + (0.5 if side == "right" else 0)
            bones[secondary(side, n)] = channel(rotation=wave(length, steps, lambda p, k=k: (
                base_x * 0.85 + flutter * 0.8 * math.sin((p * speed + k) * 2 * math.pi), 0, 0)))
    for i, tail in enumerate(TAILS):
        bones[tail] = channel(rotation=wave(length, steps, lambda p, i=i: (
            base_x * 0.8 + flutter * math.sin((p * speed + i * 0.2) * 2 * math.pi), 0, (i - 2) * 6)))


def crest_flicker(bones, length):
    for i, bone in enumerate(CREST):
        bones[bone] = channel(rotation=wave(length, 4, lambda p, i=i: (
            3 * math.sin((p * 2 + i * 0.31) * 2 * math.pi), 0, 2 * math.sin((p + i * 0.17) * 2 * math.pi))))


def wings(bones, length, z_of, y_of=lambda p: 0.0, forearm=lambda p: 0.0, hand=lambda p: 0.0, steps=8):
    """Both wings mirrored: z_of(p) lowers the wing (positive = tip down), y_of(p) sweeps it back."""
    for side, sign in SIDES:
        bones[f"wing_{side}"] = channel(rotation=wave(length, steps, lambda p, s=sign: (0, -s * y_of(p), s * z_of(p))))
        bones[f"wing_{side}_forearm"] = channel(rotation=wave(length, steps, lambda p, s=sign: (0, 0, s * forearm(p))))
        bones[f"wing_{side}_hand"] = channel(rotation=wave(length, steps, lambda p, s=sign: (0, 0, s * hand(p))))


# --- loops -------------------------------------------------------------------------------------------------------------

def perched():
    """On the ground: upright, wings held up like a mantle, the furnace breathing, the head searching."""
    length = 4.0
    bones = {
        "ribcage_choir": channel(scale=wave(length, 4, lambda p: (1 + 0.04 * math.sin(p * 2 * math.pi),) * 2 + (1,))),
        "body": channel(rotation=wave(length, 4, lambda p: (2 * math.sin(p * 2 * math.pi), 0, 0))),
        "neck": channel(rotation=wave(length, 4, lambda p: (-4 + 3 * math.sin(p * 2 * math.pi), 0, 0))),
        "head": channel(rotation=linear((0, (0, 0, 0)), (1.2, (0, 18, 4)), (1.3, (0, 18, 4)), (2.0, (6, -4, 0)),
                                        (2.9, (0, -22, -5)), (3.0, (0, -22, -5)), (4.0, (0, 0, 0)))),
        "beak_lower": channel(rotation=linear((0, (0, 0, 0)), (2.0, (0, 0, 0)), (2.2, (8, 0, 0)), (2.5, (0, 0, 0)),
                                              (4.0, (0, 0, 0)))),
    }
    wings(bones, length, lambda p: 8 + 3 * math.sin(p * 2 * math.pi), forearm=lambda p: 6 * math.sin(p * 2 * math.pi))
    feathers(bones, length, 0, 3)
    crest_flicker(bones, length)
    return {"loop": True, "animation_length": length, "bones": bones}


def fly():
    """Circling: pitched forward, slow heavy beats, the burning feathers streaming back."""
    length = 2.4

    def beat(p):
        return 30 + 38 * math.sin(p * 2 * math.pi)

    bones = {
        "root": channel(rotation=wave(length, 8, lambda p: (38 + 3 * math.sin(p * 2 * math.pi), 0, 0)),
                        position=wave(length, 8, lambda p: (0, -6 * math.sin(p * 2 * math.pi), 0))),
        "neck": channel(rotation=wave(length, 4, lambda p: (-24, 0, 0))),
        "head": channel(rotation=wave(length, 4, lambda p: (-14 + 3 * math.sin(p * 2 * math.pi), 0, 0))),
        "leg_left": channel(rotation=wave(length, 4, lambda p: (55, 0, 0))),
        "leg_right": channel(rotation=wave(length, 4, lambda p: (55, 0, 0))),
        "leg_left_lower": channel(rotation=wave(length, 4, lambda p: (-20, 0, 0))),
        "leg_right_lower": channel(rotation=wave(length, 4, lambda p: (-20, 0, 0))),
    }
    wings(bones, length, beat, y_of=lambda p: 6 * math.cos(p * 2 * math.pi),
          forearm=lambda p: -12 * math.sin((p - 0.1) * 2 * math.pi),
          hand=lambda p: -16 * math.sin((p - 0.2) * 2 * math.pi))
    feathers(bones, length, 40, 9, speed=2)
    crest_flicker(bones, length)
    return {"loop": True, "animation_length": length, "bones": bones}


def hover():
    """Hanging in the air in one place (scream, release, the rise's end): upright, quick deep beats."""
    length = 1.4

    def beat(p):
        return 18 + 50 * math.sin(p * 2 * math.pi)

    bones = {
        "root": channel(rotation=wave(length, 8, lambda p: (14, 0, 0)),
                        position=wave(length, 8, lambda p: (0, -10 * math.sin(p * 2 * math.pi), 0))),
        "neck": channel(rotation=wave(length, 4, lambda p: (-8, 0, 0))),
        "head": channel(rotation=wave(length, 4, lambda p: (-6 + 4 * math.sin(p * 2 * math.pi), 0, 0))),
        "leg_left": channel(rotation=wave(length, 4, lambda p: (15 + 6 * math.sin(p * 2 * math.pi), 0, 0))),
        "leg_right": channel(rotation=wave(length, 4, lambda p: (12 + 6 * math.sin(p * 2 * math.pi), 0, 0))),
    }
    wings(bones, length, beat, y_of=lambda p: -10 * math.cos(p * 2 * math.pi),
          forearm=lambda p: -16 * math.sin((p - 0.12) * 2 * math.pi),
          hand=lambda p: -20 * math.sin((p - 0.24) * 2 * math.pi))
    feathers(bones, length, 18, 12, speed=1)
    crest_flicker(bones, length)
    return {"loop": True, "animation_length": length, "bones": bones}


def dive():
    """Stooping on its prey: almost head-first, wings swept back, talons forward."""
    length = 0.6
    bones = {
        "root": channel(rotation=wave(length, 4, lambda p: (72 + 2 * math.sin(p * 2 * math.pi), 0,
                                                              2 * math.sin(p * 4 * math.pi)))),
        "neck": channel(rotation=wave(length, 2, lambda p: (-40, 0, 0))),
        "head": channel(rotation=wave(length, 2, lambda p: (-30, 0, 0))),
        "beak_lower": channel(rotation=wave(length, 2, lambda p: (18, 0, 0))),
        "leg_left": channel(rotation=wave(length, 2, lambda p: (-35, 0, 0))),
        "leg_right": channel(rotation=wave(length, 2, lambda p: (-35, 0, 0))),
        "leg_left_lower": channel(rotation=wave(length, 2, lambda p: (-30, 0, 0))),
        "leg_right_lower": channel(rotation=wave(length, 2, lambda p: (-30, 0, 0))),
    }
    wings(bones, length, lambda p: 62 + 4 * math.sin(p * 2 * math.pi), y_of=lambda p: 48,
          forearm=lambda p: 28, hand=lambda p: 22, steps=4)
    feathers(bones, length, 20, 6, speed=2, steps=4)
    crest_flicker(bones, length)
    return {"loop": True, "animation_length": length, "bones": bones}


def stunned():
    """The horn drowned out the choir: rigid, wings locked half open, head thrown back, a tremor through it all."""
    length = 0.5
    shake = lambda amp: wave(length, 6, lambda p: (amp * math.sin(p * 6 * math.pi), 0, amp * math.cos(p * 4 * math.pi)))
    bones = {
        "root": channel(rotation=shake(1.2)),
        "neck": channel(rotation=wave(length, 2, lambda p: (-30, 0, 8))),
        "head": channel(rotation=wave(length, 2, lambda p: (-35, 0, 12))),
        "beak_lower": channel(rotation=wave(length, 2, lambda p: (24, 0, 0))),
        "ribcage_choir": channel(scale=wave(length, 2, lambda p: (0.94, 0.94, 1))),
    }
    wings(bones, length, lambda p: 22 + 1.5 * math.sin(p * 8 * math.pi), forearm=lambda p: -20, hand=lambda p: -30, steps=6)
    for side, sign in SIDES:
        for n in PRIMARIES[side]:
            bones[primary(side, n)] = channel(rotation=wave(length, 2, lambda p, n=n: (10 + n * 2, 0, 0)))
    return {"loop": True, "animation_length": length, "bones": bones}


# --- one-shots ---------------------------------------------------------------------------------------------------------

def rise():
    """Out of the lava: for 2 s only the ground glows (it is still under it), then it rises folded, head first, and at
    the top tears its wings open."""
    bones = {
        "root": channel(rotation=linear((0, (-20, 0, 0)), (2.0, (-20, 0, 0)), (4.0, (-6, 0, 0)), (4.4, (8, 0, 0)),
                                        (5.0, (0, 0, 0)))),
        "neck": channel(rotation=linear((0, (-30, 0, 0)), (3.6, (-30, 0, 0)), (4.3, (-45, 0, 0)), (5.0, (0, 0, 0)))),
        "head": channel(rotation=linear((0, (-20, 0, 0)), (3.6, (-20, 0, 0)), (4.3, (-30, 0, 0)), (5.0, (0, 0, 0)))),
        "beak_lower": channel(rotation=linear((0, (0, 0, 0)), (4.0, (0, 0, 0)), (4.3, (30, 0, 0)), (5.0, (0, 0, 0)))),
        "ribcage_choir": channel(scale=linear((0, (0.85, 0.85, 1)), (4.2, (0.85, 0.85, 1)), (4.4, (1.15, 1.15, 1)),
                                              (5.0, (1, 1, 1)))),
    }
    for side, sign in SIDES:
        bones[f"wing_{side}"] = channel(rotation=linear(
            (0, (0, -sign * 40, sign * 70)), (4.0, (0, -sign * 40, sign * 70)), (4.35, (0, sign * 5, -sign * 25)),
            (5.0, (0, 0, 0))))
        bones[f"wing_{side}_forearm"] = channel(rotation=linear((0, (0, 0, sign * 40)), (4.0, (0, 0, sign * 40)),
                                                                (4.4, (0, 0, 0)), (5.0, (0, 0, 0))))
        for n in PRIMARIES[side]:
            bones[primary(side, n)] = channel(rotation=linear((0, (60, 0, 0)), (4.0, (60, 0, 0)), (4.4, (-10, 0, 0)),
                                                              (5.0, (0, 0, 0))))
    return {"animation_length": 5.0, "bones": bones}


def scream():
    """The choir screams: the head goes back, the chest swells, at 1.2 s every voice at once with wings flung wide."""
    bones = {
        "neck": channel(rotation=linear((0, (0, 0, 0)), (0.9, (-28, 0, 0)), (1.2, (22, 0, 0)), (1.7, (18, 0, 0)),
                                        (2.0, (0, 0, 0)))),
        "head": channel(rotation=linear((0, (0, 0, 0)), (0.9, (-30, 0, 0)), (1.2, (12, 0, 0)), (1.3, (14, 0, 3)),
                                        (1.4, (12, 0, -3)), (1.5, (14, 0, 3)), (1.7, (12, 0, 0)), (2.0, (0, 0, 0)))),
        "beak_lower": channel(rotation=linear((0, (0, 0, 0)), (0.9, (10, 0, 0)), (1.2, (40, 0, 0)), (1.75, (40, 0, 0)),
                                              (2.0, (0, 0, 0)))),
        "ribcage_choir": channel(scale=linear((0, (1, 1, 1)), (1.1, (1.1, 1.1, 1)), (1.2, (1.25, 1.25, 1)),
                                              (1.7, (1.2, 1.2, 1)), (2.0, (1, 1, 1)))),
        "crest": channel(rotation=linear((0, (0, 0, 0)), (1.2, (-14, 0, 0)), (1.7, (-14, 0, 0)), (2.0, (0, 0, 0)))),
    }
    for side, sign in SIDES:
        bones[f"wing_{side}"] = channel(rotation=linear((0, (0, 0, 0)), (0.9, (0, -sign * 12, sign * 10)),
                                                        (1.2, (0, sign * 10, -sign * 20)), (1.75, (0, sign * 10, -sign * 20)),
                                                        (2.0, (0, 0, 0))))
    return {"animation_length": 2.0, "bones": bones}


def shake(length, amp, victims_only=False):
    """The wings shudder: ash comes off them (ash rain), or the figures tear loose (release)."""
    # Only the feathers: the wings keep beating under the base animation.
    bones = {}
    for side, sign in SIDES:
        for n in PRIMARIES[side]:
            victim = n in (2, 6, 9)
            if victims_only and not victim:
                continue
            a = amp * (2.0 if victims_only else 1.0)
            bones[primary(side, n)] = channel(rotation=linear(*[
                (length * i / 12, (a * (1 if (i + n) % 2 else -1) * (0 if i in (0, 12) else 1), 0, 0))
                for i in range(13)]))
    return {"animation_length": length, "bones": bones}


def peck():
    """On the ground: the beak hacks down in front of it at 0.45 s."""
    return {"animation_length": 0.8, "bones": {
        "neck": channel(rotation=linear((0, (0, 0, 0)), (0.3, (-25, 0, 0)), (0.45, (40, 0, 0)), (0.55, (36, 0, 0)),
                                        (0.8, (0, 0, 0)))),
        "head": channel(rotation=linear((0, (0, 0, 0)), (0.3, (-15, 0, 0)), (0.45, (25, 0, 0)), (0.8, (0, 0, 0)))),
        "beak_lower": channel(rotation=linear((0, (0, 0, 0)), (0.3, (30, 0, 0)), (0.45, (0, 0, 0)), (0.8, (0, 0, 0)))),
        "body": channel(rotation=linear((0, (0, 0, 0)), (0.3, (-6, 0, 0)), (0.45, (12, 0, 0)), (0.8, (0, 0, 0)))),
    }}


def buffet():
    """On the ground: both wings up and then down and forward at 0.6 s, a blast that throws everyone back."""
    bones = {"body": channel(rotation=linear((0, (0, 0, 0)), (0.45, (-8, 0, 0)), (0.6, (10, 0, 0)), (1.1, (0, 0, 0))))}
    for side, sign in SIDES:
        bones[f"wing_{side}"] = channel(rotation=linear((0, (0, 0, 0)), (0.45, (0, sign * 20, -sign * 35)),
                                                        (0.6, (0, -sign * 30, sign * 55)), (0.8, (0, -sign * 20, sign * 45)),
                                                        (1.1, (0, 0, 0))))
        bones[f"wing_{side}_hand"] = channel(rotation=linear((0, (0, 0, 0)), (0.45, (0, 0, -sign * 20)),
                                                             (0.6, (0, 0, sign * 30)), (1.1, (0, 0, 0))))
    return {"animation_length": 1.1, "bones": bones}


def death():
    """It falls, the wings fold in, and it crumbles: the feathers drop away into ash and the furnace goes dark."""
    bones = {
        "root": channel(rotation=linear((0, (0, 0, 0)), (0.4, (-15, 0, 6)), (1.2, (30, 0, 14)), (1.8, (62, 0, 18)),
                                        (2.2, (80, 0, 20)), (3.0, (84, 0, 20))),
                        position=linear((0, (0, 0, 0)), (1.2, (0, -20, 0)), (2.2, (0, -60, 0)), (3.0, (0, -66, 0)))),
        "neck": channel(rotation=linear((0, (0, 0, 0)), (0.4, (-40, 0, 0)), (1.6, (-10, 0, 20)), (3.0, (20, 0, 30)))),
        "beak_lower": channel(rotation=linear((0, (0, 0, 0)), (0.4, (40, 0, 0)), (1.2, (20, 0, 0)), (3.0, (30, 0, 0)))),
        "ribcage_choir": channel(scale=linear((0, (1, 1, 1)), (0.4, (1.25, 1.25, 1)), (2.4, (0.8, 0.8, 1)),
                                              (3.0, (0.6, 0.6, 1)))),
        "crest": channel(scale=linear((0, (1, 1, 1)), (1.6, (1, 1, 1)), (2.6, (0.01, 0.01, 0.01)),
                                      (3.0, (0.01, 0.01, 0.01)))),
    }
    for side, sign in SIDES:
        bones[f"wing_{side}"] = channel(rotation=linear((0, (0, 0, 0)), (0.4, (0, sign * 10, -sign * 30)),
                                                        (1.8, (0, -sign * 30, sign * 60)), (3.0, (0, -sign * 35, sign * 75))))
        for n in PRIMARIES[side]:
            start = 1.0 + (n % 5) * 0.25
            bones[primary(side, n)] = channel(
                position=linear((0, (0, 0, 0)), (start, (0, 0, 0)), (start + 0.6, (0, -30, 6)), (3.0, (0, -30, 6))),
                scale=linear((0, (1, 1, 1)), (start, (1, 1, 1)), (start + 0.6, (0.01, 0.01, 0.01)),
                             (3.0, (0.01, 0.01, 0.01))))
    for i, tail in enumerate(TAILS):
        bones[tail] = channel(scale=linear((0, (1, 1, 1)), (0.8 + i * 0.2, (1, 1, 1)), (1.4 + i * 0.2, (0.01, 0.01, 0.01)),
                                           (3.0, (0.01, 0.01, 0.01))))
    return {"animation_length": 3.0, "bones": bones}


def main():
    animations = {
        "animation.the_risen.perched": perched(),
        "animation.the_risen.fly": fly(),
        "animation.the_risen.hover": hover(),
        "animation.the_risen.dive": dive(),
        "animation.the_risen.stunned": stunned(),
        "animation.the_risen.rise": rise(),
        "animation.the_risen.scream": scream(),
        "animation.the_risen.ash_rain": shake(1.0, 6),
        "animation.the_risen.release": shake(0.8, 10, victims_only=True),
        "animation.the_risen.peck": peck(),
        "animation.the_risen.buffet": buffet(),
        "animation.the_risen.death": death(),
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": animations}, f, indent="\t")
        f.write("\n")
    print(f"{len(animations)} animations -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
