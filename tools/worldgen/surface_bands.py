"""Surface rule speed-up (opt/surface, 2026-10-26): replaces every long chain of y_above tests in the surface rules of
noise_settings/overworld.json and nether.json with one corruptedadventure:y_bands rule (YBandsRule), which gives the
same block per height with one table lookup.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/surface_bands.py

A chain is a sequence whose rules only test y_above (absolute anchor, add_stone_depth false, one surface depth
multiplier) and give plain blocks, nested to any depth, and that always gives a block (it ends in one). The rock strata
of ring 2 (hand-written, 13 copies of 48 bands) are the only such chains today. Flattening keeps the order of the tests:
a rule `if y_above(a) then R` becomes the bands of R, each with its lower edge raised to at least a, followed by
(a, the block R ends in); a total rule ends the sequence (what follows it can never run). The script checks every
replaced chain against the old one at every height around its bands, writes nothing if any differs, and is idempotent
(a second run finds no chains). Everything else stays byte-identical.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "data",
                    "corruptedadventure", "worldgen", "noise_settings")
MIN_BANDS = 8


class NotAChain(Exception):
    pass


def y_test(condition, mult):
    """The absolute anchor of a y_above that a chain with this multiplier may hold; NotAChain otherwise."""
    if (condition.get("type") != "minecraft:y_above" or condition.get("add_stone_depth")
            or list(condition.get("anchor", {})) != ["absolute"]):
        raise NotAChain()
    if mult.setdefault("value", condition["surface_depth_multiplier"]) != condition["surface_depth_multiplier"]:
        raise NotAChain()
    return condition["anchor"]["absolute"]


def flatten(rule, mult):
    """(bands, below) of a chain rule: bands as [from, block] in test order, below the block it ends in (None: it can
    give no block)."""
    kind = rule.get("type")
    if kind == "minecraft:block":
        return [], rule["result_state"]
    if kind == "minecraft:sequence":
        bands = []
        for child in rule["sequence"]:
            sub, below = flatten(child, mult)
            bands += sub
            if below is not None:
                return bands, below
        return bands, None
    if kind == "minecraft:condition":
        edge = y_test(rule["if_true"], mult)
        sub, below = flatten(rule["then_run"], mult)
        bands = [[max(start, edge), block] for start, block in sub]
        if below is not None:
            bands.append([edge, below])
        return bands, None
    raise NotAChain()


def old_rule(rule, v):
    """The block the old rule gives at reduced height v (y − surface depth × multiplier), as vanilla evaluates it."""
    kind = rule["type"]
    if kind == "minecraft:block":
        return rule["result_state"]
    if kind == "minecraft:sequence":
        for child in rule["sequence"]:
            block = old_rule(child, v)
            if block is not None:
                return block
        return None
    return old_rule(rule["then_run"], v) if v >= rule["if_true"]["anchor"]["absolute"] else None


def new_rule(bands, below, v):
    return next((block for start, block in bands if v >= start), below)


def bands_rule(rule):
    """The y_bands rule for a chain, or None if the rule is no (long enough) chain."""
    if rule.get("type") != "minecraft:sequence":
        return None
    mult = {}
    try:
        bands, below = flatten(rule, mult)
    except NotAChain:
        return None
    if below is None or len(bands) < MIN_BANDS:
        return None
    # A band no lower than an earlier one is never reached (the earlier one matches first); the rest descend.
    bands = [band for i, band in enumerate(bands) if all(band[0] < earlier[0] for earlier in bands[:i])]
    starts = [start for start, _ in bands]
    for v in range(min(starts) - 16, max(starts) + 16):
        if old_rule(rule, v) != new_rule(bands, below, v):
            raise SystemExit("y_bands differs from the chain at %d" % v)
    return {"type": "corruptedadventure:y_bands", "surface_depth_multiplier": mult["value"],
            "bands": [{"from": start, "block": block} for start, block in bands], "below": below}


def replace(node, counter):
    """Replaces the chains below node (outermost first); returns the node's replacement."""
    found = bands_rule(node)
    if found is not None:
        counter.append(len(found["bands"]))
        return found
    if node.get("type") == "minecraft:sequence":
        node["sequence"] = [replace(child, counter) for child in node["sequence"]]
    elif node.get("type") == "minecraft:condition":
        node["then_run"] = replace(node["then_run"], counter)
    return node


def main():
    for name in ("overworld", "nether"):
        path = os.path.join(ROOT, name + ".json")
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read()
        newline = "\r\n" if "\r\n" in text else "\n"
        data = json.loads(text)
        counter = []
        data["surface_rule"] = replace(data["surface_rule"], counter)
        out = json.dumps(data, indent=2, ensure_ascii=False).replace("\n", newline) + newline
        if out != text:
            with open(path, "w", encoding="utf-8", newline="") as f:
                f.write(out)
        print("%s: %d chains -> y_bands %s, %s" % (name, len(counter), counter, "changed" if out != text else "unchanged"))


if __name__ == "__main__":
    main()
