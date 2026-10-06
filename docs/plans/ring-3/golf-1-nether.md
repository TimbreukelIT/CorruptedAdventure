# Golf 1 — stroom W-nether: 53 (Nether-ring 3-terrein)

Scope: `worldgen` — werkpakket 53, alleen terrein (de biomes zitten in 53b, golf 2); branch `ring3/nether-g1`.

**Doel:** de vorm van de bevroren Nether in Nether-ring 3 (875–1.500, 625 breed), als eigen ring 3-invoer van `ring_blend/nether`:
- `frozen_lava_sea`-vlakte (gestolde lavazee met ijspilaren van vloer tot plafond);
- de `rime_forest`-hallen;
- de `glacier_rifts`-kloven en -tunnels (vorm alvast, het biome volgt in 54).
De warme scheuren worden als vorm voorbereid (smalle spleten in de vlakte). De levendig-eis en het eigen wow (bevroren lavavallen, ijspegels) zijn features in golf 2. Surface tijdelijk vanilla (`packed_ice`, `blue_ice`, basalt, netherrack).

Bron: `fase-1-wereld.md` Nether-ring 3 + sparring-aanvulling; `contract.md` §4; sjabloon change 35 (`ring_2/nether/*`, `ring_edge`-dam, `fluid_levels`, `ring_nether_cave`, determinismeregel Nether, valkuilen "Lavazee per ring", "Nether-carver zet zelf lava").

## Werk
1. `ring_3/nether/terrain` (+ de verwijzing in `ring_blend/nether`):
   - vlakte op Y ~35 met ijspilaren (3D-ruis of kolommen, zoals `masses` van change 35);
   - plafond ~110;
   - regio's via `ring_3/nether/region` (39a).
2. **Vloeistof:** geen zichtbare lavazee; `fluid_levels.ring_3` = 22 (39a, antwoord 2026-10-06: gelijk aan ring 2, dus geen trede en geen lavawanden op 2→3). De ring 2-dam (`ring_edge`) aan de 2→3-grens moet blijven werken.
   - **Harde eis (39a-review):** trede 3|4 (22/32). Aan de ring 3-kant van zone 3→4 vast tot boven Y 32 (eigen `ring_edge`-dam, carvers niet erdoor). `Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround` faalt bewust zodra het placeholder-terrein weg is: vervang die tak door een controle op de eigen dam. Test S: `tools/nether-lava-walls.py` op r 875 en 1500 → 0 wanden. Zie `contract.md` §4.
   - Biomes: de router-`temperature` wisselt vanaf zone 2→3 hard op de middellijn (`switch_at` in `ring_blend/nether_temperature`); Nether-3-biomes lezen tot de middellijn alleen `ring_3/nether/region`.
   - Ring 3-biomes gebruiken `ring_nether_cave`, zodat de vanilla-carver geen lava zet.
   - Grens 3→4: let op het lavaniveau van Nether-4 (later); nu placeholder.
3. **Ruimte-check** (vooruitlopend op 54): voelt 625 breed met biome-wissels genoeg? Meet de hal- en kloofmaten.
4. **Prestatie:** indicatief (Nether-2 kostte ~1,95×; het budget voor ring 3 is ≤ 1,75×).

## Client/server-impact
Geen; puur worldgen.

## Tests
- **B**, **D**, **S** (`/ca tpring 3 nether`, `tpboundary 2|3 nether`).
- Grens 2→3 zonder lavawand.
- Nether-1/2 onveranderd (biomes + terreinvorm).
- **W-checklist** voor 4a (een eerste blik: vorm en ruimte).

## Bij afronding
`waves.md` §4.
