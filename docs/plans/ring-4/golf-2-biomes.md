# Ring 4 golf 2 — stroom W-biomes: 75, 76, 77, 75b

Scope: `worldgen`. Branch `ring4/biomes-g2` (worktree, basis `main` d1a37f11: 72/72b van B, 71b van T, 80 van A, 78 van W-nether en de `steam_fens`-prestatiefix). Poort n = 2 (server 25572, RCON 25582). Bron: `golf-2.md` §2 W-biomes, Q4, Q8, Q10, Q13, Q14, G1, G4, H5, F1–F12, H1–H4, §6b; `golf-2-blocks.md` (aanwijzingen voor 75b); `contract.md` §2–§4; `waves.md` §4–§5.

## Gedeelde basis (75)
- `Ring4Terrain`: het pure ring 4-terrein (`IslandField`) van het niveau voor de features (patroon `Ring3Terrain`), plus `blighted(x, z)` (de bandgrens van weer en gif).
- `Ring4Filter` (`corruptedadventure:ring4_filter`): bereiken op de 2D-uitgangen van het veld (patroon `volcanic_filter`, incl. `slope`), `ground_only` (niet op eilanden, bogen of brugdekken), `fall_clearance` (afstand tot elke vastgelegde val) en `knob_clearance` (afstand tot elke klimknop).
- `Ring4Placements`: sleutels `ring_4/…`, de bevroren worldgen-heightmaps (`SURFACE` = `WORLD_SURFACE_WG`, `FLOOR` = `OCEAN_FLOOR_WG`), predicaten (`ON_LAND`, `ON_WATER`, `UNDER_WATER`, `waterAbove`, `waterNear`) en `register`/`perChunk`.
- Determinisme (23c): plaatsing in de eigen chunk op de bevroren heightmaps, `JitteredGridPlacement`, features die alleen in de eigen chunk schrijven; alleen boomkruinen reiken in de buren (3 × 3-venster).

## 75 — mythische band, `sky_gardens`, gloed onder de eilanden
| Waar | Wat |
|---|---|
| alle grondbiomes (onder de eilanden) | `island_glow` (eerst): per cel van 4 × 4 één lichtbron op droog land in de eilandschaduw (licht ≥ 1 overal op vlak land). Mythisch (per kolom via `Ring4Band`): `glow_bloom`/`glowcap` + `aquamarine`-knoppen; bedorven: `glowcap`/`toxic_crystal_cluster` (licht 5) + `blighted_glowcap` en gifkristalknoppen; geen budding-blokken (H5 = A). Daarna `island_underside`: strengen `hanging_swamp_moss` (2–7), losse `glowing_hanging_roots`/`hanging_roots`, en onder lage eilanden (gat ≤ 40) af en toe een streng tot één blok boven de grond (klimbaar). |
| `cascade_vale` | `ring_4/moonpetal_tree` (kersvorm in moonpetal-hout; ook de sapling), gras/varens, `glow_bloom` langs de meren, zeegras |
| `crystal_lagoon` | koraal (`local_coral`: vanilla-vormen, blijft in de chunk), zeegras, sea pickles |
| `lotus_shallows` | `giant_lily_pad` (2 × 2 geheel), `lotus`, rietaccenten op de oevers, zeegras, gras, `glow_bloom` |
| `glowroot_swamp` | reuzenmangrove (`ring_4/swamp_tree`: vanilla hoge mangrove een maat groter, `glowcap`/mostapijt op de wortels, geen vines/bijen), `glowcap`, mostapijt, gras, zeegras |
| `mist_fjords` | spar/den, af en toe donkere eik op de vlakke toppen (helling < 0,8), varens, mostapijt, zeegras |
| `sky_gardens` | moonpetal-bomen, `glow_bloom` + bloemen, gras |
| diepe poelen (lagune, lotus, moeras, fjorden) | `bubble_spring` als bodemblok onder ≥ 6 water |

Nog niet (wacht op 74, W-terrein-b): ranken tussen de eilandparen, planten rond de hemelmeren.

## CODEBASE-delta
**Nieuwe classes (`worldgen.feature`)**
- `Ring4Terrain`, `Ring4Filter` (+ `ModPlacementModifierTypes.RING4_FILTER`, blok `// ring 4 — W-biomes g2`), `Ring4Placements`.
- `IslandGlowFeature`/`IslandGlowConfiguration` (`island_glow`), `IslandUndersideFeature`/`IslandUndersideConfiguration` (`island_underside`), `GiantLilyPadFeature` (`giant_lily_pad`), `ReedClumpFeature`/`ReedClumpConfiguration` (`reed_clump`), `LocalCoralFeature` (`local_coral`); in `ModFeatures` (blok `// ring 4 — W-biomes g2`).
- `Ring4MythicFeatures` (configured/placed 75).

**Gewijzigd**
- `Ring4Features.ORDER` (+ helpers `vegetal`, `ground`), `configured`/`placed` roepen `Ring4MythicFeatures` aan.
- `Ring4Biomes` (javadoc), `Ring4BiomesTest` (verdant_falls alleen eigen features, gloed eerst in elke grondbiome).

**Registries:** 5 feature-types, 1 placement modifier; configured `ring_4/moonpetal_tree` (key van B) en 17 eigen; 22 placed.

## Beslissingen
- 2026-10-16, `worldgen` (implementatie W-biomes, 75): de gloed onder de eilanden als vast rooster (cellen van 4, bron 1–2 in de cel) i.p.v. een losse kans, zodat elk vlak blok in de schaduw licht ≥ 1 krijgt; de palet per kolom via de band (`Ring4Band.blighted`), niet per biome.
- 2026-10-16, `worldgen` (implementatie, 75): moerasboom = vanilla's hoge mangrove een maat groter (stam 5–14, wortels 3–7) met `glowcap` en mostapijt op de wortels, zonder vanilla-vines (geen vanilla vine in ring 4) en zonder bijen. Fjordbomen: vanilla spar (default), den 40 %, donkere eik 10 %.
- 2026-10-16, `worldgen` (implementatie, 75): `moonpetal_tree` = vanilla's kersvorm met moonpetal-blokken (ook de sapling).
- 2026-10-16, `worldgen` (implementatie, 75): koraal via eigen `local_coral` (vanilla-vormen, maar alleen ≥ 1 blok van de chunkrand) omdat vanilla tot 6 blokken in buurchunks schrijft (23c-regel).

## Backlog
- `worldgen` (75, na 74 op main): ranken tussen de eilandparen (`IslandField`-accessor van 74), planten rond de hemelmeren.
- `blocks`: `glow_bloom`, `glowcap`, `verdant_bromeliad`, `verdant_blossom_bush` niet in `#replaceable_by_trees`: een boomkruin uit een buurchunk die later komt, laat zo'n plant staan, een eerdere niet (zeldzaam: kruinen op grondhoogte). INT-determinisme let erop.

## Voortgang
- 2026-10-16: 75 gebouwd; B (incl. `Ring4MythicFeaturesTest`, `Ring4BiomesTest`, `FeatureOrderTest`) en D (twee keer, tweede keer 0 geschreven) groen.
