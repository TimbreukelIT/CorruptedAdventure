# Change 39a — Ring 3 regio-assen, router en DF-stubs

Scope: `worldgen` — werkpakket 39a (golf 0, sequentieel, na 37c; eigenaar W-terrein, Nether-deel W-nether)

**Doel:** de gedeelde worldgen-sleutels uit `docs/plans/ring-3/contract.md` §4 laten bestaan, zodat W-terrein, W-biomes en W-nether in golf 1 parallel in eigen worktrees kunnen laden en testen. Dit pakket maakt:
- de twee regio-assen van ring 3 (overworld) en de regio-as van Nether-3 (echte ruis);
- de router-sleutels die ze in `ring_blend` invoeren;
- stubs (constante 0) voor alle andere gedeelde DF's;
- het Nether-3-lavaniveau in de preset.

Geen terreinvorm, geen biomes: de placeholder-biomes en het placeholder-terrein blijven tot 39/39b/53.

Bron: `contract.md` §4; `fase-1-wereld.md` §1.2 (klimaat per ring via de router), §1.10 (vlekken ~500–1.000 blokken); sjabloon `ring_2/overworld/relief`/`moisture` (change 23/30).

## Plan
1. **Noises:**
   - `noise/ring_3_heat.json` en `noise/ring_3_ground.json`: firstOctave −10, amplitudes als `ring_2_moisture` (vlekken ~500–1.000 blokken; ring 3 is breder, dus mogelijk −11; meten).
   - `noise/nether_ring_3_region.json` (÷4-schaal zoals Nether-2).
2. **Regio-assen** (`flat_cache` van de ruis, zonder offset):
   - `density_function/ring_3/overworld/region_heat.json`;
   - `ring_3/overworld/region_ground.json`;
   - `ring_3/nether/region.json`.
3. **Router:**
   - in `ring_blend/overworld_temperature.json` → ring 3 = `region_heat`; in `_vegetation.json` → ring 3 = `region_ground`;
   - in de Nether-router (zoals change 35) → ring 3 = `ring_3/nether/region`.
   - Ring 1/2 blijven blok voor blok gelijk.
   - **Derde as (V8 = A):** een derde router-veld (`weirdness` via een nieuwe `ring_blend/overworld_weirdness`, ring 1 = vanilla letterlijk, ring 3 = `caldera_dormant`, rest 0). Ring 1 moet identiek blijven: test zoals `vein_toggle`.
4. **Stubs** (constante `0`, flat): `ring_3/overworld/height`, `caldera_mask`, `caldera_dormant`, `lava_channel`, `island_mask`; `ring_3/nether/terrain` blijft voorlopig de placeholder (die bestaat al als `ring_3/nether.json`; W-nether verplaatst hem in 53).
5. **Preset:** `fluid_levels.ring_3` (Nether) volgens het contract: geen lavazee. Een niveau onder de vloer (bijv. Y 8); W-nether mag het in 53 bijstellen.

## Client/server-impact
Geen (alleen worldgen-data, deterministisch per seed).

## Tests
- **B** + bestaande worldgen-tests (`RingBlendTest`, de JSON-tests van ring 2).
- **D** (geen wijziging).
- **S**: verse wereld; `/ca tpring 3` en `tpring 3 nether` werken; terrein in ring 3 is nog het placeholder-terrein.
- Ring 1/2 ongewijzigd: biomes op 300 punten en steen/deepslate in 6 chunks gelijk aan vóór 39a (methode change 23).
- Regio-assen: een korte PNG of tabel van `region_heat`/`region_ground` over r 3.500–6.000 (vlekgrootte), en de verdeling (histogram) als input voor 39b.

## Open
- Niets: V8 = A (2026-10-06). Stap 3 hoort erbij; ring 1 moet blok voor blok gelijk blijven (biomes op 300 punten, ook de vanilla-weirdness-afhankelijke biomes).

Commit: `worldgen: add ring 3 region axes and shared density function stubs`.
