"""Nether ring 5 terrain (87): writes density_function/ring_5/nether/{edge,region,terrain}.json and the hash noise
noise/nether_ring_5_cathedral_cells.json, and removes the placeholder density_function/ring_5/nether.json.

Usage (from the repo root, Python 3, standard library only):  python tools/worldgen/nether_ring5_terrain.py

Stream W-nether's own script; idempotent (every file it owns is rewritten, JSON with indent 2). The shape follows
docs/plans/ring-5/golf-1-nether.md: the white, silent Nether ring 5 (white_halls, with the rare hollow_cathedral, V10-A)
is one pure field of cells, the density function type corruptedadventure:nether_cathedrals (NetherCathedrals), with
two outputs from the same settings:
- region (the router's temperature of ring 5, read by the preset's multi_noise): the cathedral mask, 1 inside a
  cathedral, 0 outside, over its rim (hollow_cathedral at >= 0.5);
- terrain (ring 5's input of ring_blend/nether), exact per block, never interpolated: a flat floor (top block Y 31,
  solid down to the bedrock, so there is no open space and no lava anywhere: fluid_levels.ring_5 = -1), raised plains
  2-6 high on the tiles between the columns (quantised from the region noise nether_ring_5_region of 82), a flat
  ceiling from Y 104, square columns 4 x 4 on a grid of 24 with an offset per cell of 400 (no jitter), and per cell at
  most one cathedral (diameter 120-180): the roof lifted to Y 120 over a rim of 8-16 (a smoothstep) and one giant column
  (diameter 20-30) in its middle. No column or plain within 16 of a cathedral.
settings.dressing (96): the light band, the white core and the giant column's vein and floor imprint, which the
surface paints (nether_ring5_surface.py); the density ignores it.
edge = ring_edge of ring 5, as wide as the zone 4->5 (half of 64 Nether blocks) plus the margin of 24: a cathedral's
outline, a column and a plain only stand where it is 0, so all of them keep clear of both zones.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                    "src", "main", "resources", "data", "corruptedadventure", "worldgen")
CA = "corruptedadventure:"
DIR = "ring_5/nether/"

ZONE_HALF = 32              # zone 4->5 in the Nether: 256 / 4 wide (the wider of the two zones of ring 5)
ZONE_MARGIN = 24            # a cathedral with this margin stays clear of both zones
SETTINGS = {
    "cathedrals": {
        # ~30 all round (NetherCathedralsTest: 25-35 on 5 seeds; measured 27-32)
        "cell_size": 400, "chance": 0.9, "attempts": 24,
        "radius": [60, 90],         # the footprint: diameter 120-180
        "rim": [8, 16],             # the roof rises from the ceiling to the lifted roof over this width
        "pillar_radius": [10, 15],  # the giant column: diameter 20-30
        "clearance": 16,            # no grid column or plain this close to the footprint
    },
    "halls": {
        "floor_top": 31, "ceiling": 104, "roof": 120,
        "grid": 24, "pillar_size": 4, "cell_margin": 4,
        # ~30 % of the open hall floor raised (NetherRing5JsonTest measures it; the noise's 70th percentile lies at
        # ~0.1-0.2 depending on the seed), one block higher per step above the threshold (6 high above ~0.4, ~the 96th)
        "plains_threshold": 0.08, "plains_step": 0.08, "plains_height": [2, 6],
    },
    # 96 (the user's decision of 2026-10-23), painted by the surface (nether_ring5_surface.py, condition
    # nether_ring_5_dressing); the terrain's density does not read it.
    "dressing": {
        # still light: a band 2 high through every grid column, its lowest Y per column one of 38, 44, 50, 56
        # (above the highest plain, Y 37; the giant column gets none)
        "light_y": [38, 56], "light_step": 6, "light_height": 2,
        # white core: in ~1 grid column of 10 a core of 1-3 ore stacked in one inner column, centred halfway between
        # the floor and the ceiling (Y 67)
        "core_chance": 0.1, "core_size": [1, 3],
        # the giant column: a vein of ore up its middle column (radius 0) from above the floor to below the roof,
        # and an imprint 8 wide in the floor round its foot (polished, brick borders 1 wide inside and out)
        "vein_radius": 0.0, "imprint_width": 8, "imprint_border": 1,
    },
}
NOISES = {
    "nether_ring_5_cathedral_cells": (-4, [1.0]),   # only a seeded hash for nether_cathedrals
}


def field(output):
    return {"type": CA + "nether_cathedrals", "output": output,
            "cells": CA + "nether_ring_5_cathedral_cells", "plains": CA + "nether_ring_5_region",
            "edge": CA + DIR + "edge", "settings": SETTINGS}


def files():
    return {
        "edge": {"type": CA + "ring_edge", "dimension": "minecraft:the_nether", "ring": "ring_5",
                 "width": float(ZONE_HALF + ZONE_MARGIN)},
        "region": {"type": "minecraft:flat_cache", "argument": field("mask")},
        # A density function file must be an object or a number, never a bare reference.
        "terrain": field("terrain"),
    }


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for name, data in files().items():
        write(os.path.join(ROOT, "density_function", "ring_5", "nether", name + ".json"), data)
    for name, (octave, amplitudes) in NOISES.items():
        write(os.path.join(ROOT, "noise", name + ".json"), {"firstOctave": octave, "amplitudes": amplitudes})
    placeholder = os.path.join(ROOT, "density_function", "ring_5", "nether.json")
    if os.path.exists(placeholder):
        os.remove(placeholder)
    print("Nether ring 5 terrain written")


if __name__ == "__main__":
    main()
