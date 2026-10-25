"""Ring 2 flats surface, G2 (G2-R2-2 and 7, 2026-10-25): patches the dune_sea and oasis_flats branches of
noise_settings/overworld.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/ring2_flats_surface.py

The ring 2 surface rules are hand-written JSON (CLAUDE.md §9); this script adds the G2 parts to them and is idempotent
(a second run changes nothing); everything else stays byte-identical (a json round trip with indent 2 reproduces the
file exactly, in its own line endings).

- dune_sea: red dunes where corruptedadventure:ring_2_red_dunes >= RED_DUNES_FROM (~25 % of the dunes, patches ~300):
  every rule of the branch with sand once more in front of it, with red sand for sand and red sandstone for sandstone
  (the hard valley floor and the canyon layers stay).
- oasis_flats: on the open sand (below the pools' transition ring) a grass floor for the palm groves where
  ring_2_oasis_groves >= GROVE_GRASS_FROM (with coarse dirt specks), and coarse dirt specks in the scrub patches where
  ring_2_oasis_scrub >= SCRUB_FROM, for the dry grass.

The thresholds are those of Ring2FlatsLifeFeatures (RED_DUNES_FROM, GROVE_GRASS_FROM, SCRUB_LOW); Ring2FlatsLifeTest
checks that the JSON and the Java agree.
"""
import copy
import json
import os

PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "data",
                    "corruptedadventure", "worldgen", "noise_settings", "overworld.json")
CA = "corruptedadventure:"
MAX = 1.7976931348623157e+308

RED_DUNES_FROM = 0.2
GROVE_GRASS_FROM = 0.33
SCRUB_FROM = 0.1


def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name if ":" in name else "minecraft:" + name}}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def noise(name, lo, hi=MAX):
    return {"type": "minecraft:noise_threshold", "noise": name if ":" in name else CA + name,
            "min_threshold": lo, "max_threshold": hi}


def surface_noise(lo, hi=MAX):
    return noise("minecraft:surface", lo, hi)


def recolour(rule):
    """A copy of the rule with red sand for sand and red sandstone for sandstone."""
    swap = {"minecraft:sand": "minecraft:red_sand", "minecraft:sandstone": "minecraft:red_sandstone"}
    out = copy.deepcopy(rule)

    def walk(node):
        if isinstance(node, dict):
            if node.get("type") == "minecraft:block":
                name = node["result_state"]["Name"]
                node["result_state"]["Name"] = swap.get(name, name)
            for value in node.values():
                walk(value)
        elif isinstance(node, list):
            for value in node:
                walk(value)

    walk(out)
    return out


def biome_branch(rules, name):
    for rule in rules:
        c = rule.get("if_true", {})
        if c.get("type") == "minecraft:biome" and c.get("biome_is") == [CA + name]:
            return rule
    raise SystemExit("no surface branch for " + name)


def is_noise_rule(rule, name):
    c = rule.get("if_true", {})
    return c.get("type") == "minecraft:noise_threshold" and c.get("noise") == CA + name


def dunes(branch):
    # Each rule with sand gets its red copy right in front of it (in the red patches it decides first, and where it gives
    # no block the plain rule gives none either); rules without sand (the canyon layers) stay as they are.
    rules = branch["then_run"]["sequence"]
    plain = [r for r in rules if not is_noise_rule(r, "ring_2_red_dunes")]
    rules[:] = []
    for rule in plain:
        red = recolour(rule)
        if red != rule:
            rules.append(cond(noise("ring_2_red_dunes", RED_DUNES_FROM), red))
        rules.append(rule)


def oasis(branch):
    # then_run = seq(floor, under the floor, deeper, ...); the floor is a condition with a sequence ending in plain sand.
    floor = branch["then_run"]["sequence"][0]["then_run"]["sequence"]
    floor[:] = [r for r in floor if not is_noise_rule(r, "ring_2_oasis_groves") and not is_noise_rule(r, "ring_2_oasis_scrub")]
    if floor[-1] != block("sand"):
        raise SystemExit("oasis_flats floor does not end in sand")
    floor[-1:] = [
        cond(noise("ring_2_oasis_groves", GROVE_GRASS_FROM), seq(cond(surface_noise(-MAX, -0.5), block("coarse_dirt")),
                                                                  block("grass_block"))),
        cond(noise("ring_2_oasis_scrub", SCRUB_FROM), cond(surface_noise(0.25), block("coarse_dirt"))),
        block("sand"),
    ]


def main():
    with open(PATH, encoding="utf-8", newline="") as f:
        text = f.read()
    newline = "\r\n" if "\r\n" in text else "\n"
    data = json.loads(text)
    rules = data["surface_rule"]["sequence"]
    above = next(r for r in rules if r.get("if_true", {}).get("type") == "minecraft:above_preliminary_surface")
    branches = above["then_run"]["sequence"]
    dunes(biome_branch(branches, "dune_sea"))
    oasis(biome_branch(branches, "oasis_flats"))
    out = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", newline) + newline
    with open(PATH, "w", encoding="utf-8", newline="") as f:
        f.write(out)
    print("wrote", os.path.normpath(PATH), "unchanged" if out == text else "changed")


main()
