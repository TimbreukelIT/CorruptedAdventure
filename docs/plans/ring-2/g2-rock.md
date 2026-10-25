# Ring 2 — G2 substroom `rock`: plateau, kloofbodem en hoodoos

Scope: `worldgen`. Besluiten G2-R2-4, 5 en 6 (`docs/design/fase-1-wereld.md` §G2 "Uitwerking ring 2"; voorstellen C1,
C2, C3, H1, H2 in `docs/plans/ring-2/g2-walk.md` §7). Branch `g2r2/rock` (worktree `wt/g2r2-rock`, stroom `g2r2-rock`,
poort n = 9), vanaf `g2/ring2` `8524351d`. Alle maten zijn startwaarden.

## Plan en client/server
Alleen worldgen-data en datagen: surface rules (C1, C3, H2), één nieuwe density function voor de pilaarkans (H1, de
enige DF-wijziging in ring 2), en chunk-lokale features in een nieuwe bootstrap-class `Ring2RockFeatures` (C2, C3,
puin in de geulen). Geen netwerk, geen client-code; alles draait in de wereldgeneratie op de server.

## Gebouwd
| # | Wat | Hoe | Startwaarden |
|---|---|---|---|
| C1 | Bodemvlekken op plateau en tafelbergen | surface rule `canyonlands`, niet steil, Y ≥ 118: waar `ring_2_plateau_patches` (−5, [1, 1]) ≥ 0,2 kiest vanilla's `minecraft:surface`-ruis het materiaal: ≥ 0,3 rode zandsteen ("plaveisel"), ≥ 0 grind, ≥ −0,35 grove aarde, anders gebarsten klei | ~27 % van de ruis (test), gemeten 21–26 % van de plateaukolommen |
| C2 | Bosjes dode bomen/jeneverbes + struiken | `canyon_grove_trees`: `jittered_grid` 8/marge 3/0,35 × `ClusterDensity` `ring_2_canyon_groves` (−6) 0,17 → 0,37; jeneverbes (rechte stam dood hout 2–3, eikenblad-blob r 1–2, grove aarde) 0,6 / anders de dode bomen van `dry_riverbeds`; vlak (relief ≤ 3 binnen 2), op rood zand/grove aarde/klei. `canyon_grove_shrubs`: raster 4, 0,45 × hetzelfde bosjesveld: dorre struik 4, dode struik 2, tonnetjescactus 1, woestijnbloem 1, losse blijvende eikenbladeren 3 (bladstruik) | bosjesveld ~20 % van het land (test 19,8 %) |
| C3 | Kleipannen opbreken | surface rule kloofbodem (Y < 96): in de klei waar `ring_2_clay_breakup` (−4) ≥ 0,3 rood zand, ≤ −0,35 grind | ~33 % van de pannen (test) |
| C3 | Sijpelplekken | `canyon_seep` (`ground_patch` mos 3 / gras 2 / grove aarde 1 op rood zand/klei/grind, r 2–3, rek 1,3): `canyon_spring_seeps` (raster 8/marge 3, water binnen 3 op Y −1/−2: de bronpoel en het stroompje) en `canyon_floor_seeps` (raster 8/marge 3/1,0, `ring_2_canyon_seeps` ≥ 0,1, vlak); daarna `canyon_seep_greenery` (raster 4, 0,6: varen 4, kort gras 3, mostapijt 2, grote varen 1, azalea 1 op mos/gras onder Y 100, ook op de bronranden) | |
| H1 | Dichte en open stukken in de hoodoovelden | `density_function/ring_2/overworld/hoodoo_pillar_weight.json` = `flat_cache`(`hoodoo_rock_weight` × spline op `ring_2_hoodoo_density` (−6): 0 bij −0,30 → 1 bij −0,24), het `weight` van `hoodoos` en `hoodoos_offset` (`min_weight` 0,5); `hoodoo_arches` houdt het oude gewicht (een boog mag in een open plek staan). `rock_pillars` leest het gewicht alleen per cel op 9 punten: verwaarloosbare kosten | ~21 % van de ruis open → ~30 % minder pilaren |
| H2 | Droge geulen in de hoodoovloer | surface rule hoodoovloer (Y < 96, niet steil): \|`ring_2_hoodoo_gullies`\| (−8, [1, 0,5]) < 0,018 → kern \|n\| < 0,008 grind, rand grind/rood zand op `minecraft:surface`; `hoodoo_gully_rubble` (`canyon_rubble`, raster 8, 0,5) in dezelfde band (`Ring2RockFeatures.GULLY_HALF_WIDTH`) | band 4,6 % van de ruis (test) |

Volgorde: `canyonlands` `LOCAL_MODIFICATIONS` = de rotsen van 23c (bron laatst) + `canyon_spring_seeps`,
`canyon_floor_seeps`; `VEGETAL_DECORATION` = `canyonlands_vegetation` + bomen, bosjesstruiken, sijpelgroen.
`hoodoo_fields` `LOCAL_MODIFICATIONS` = `canyon_rockfall` + `hoodoo_gully_rubble`. Elke nieuwe placed feature hoort bij
één biome. Determinisme: elke feature schrijft en leest alleen binnen zijn chunk (marges: bomen 3 = de grootste kronkel
van vanilla's gevorkte stam zonder blad, de kroon van de jeneverbes reikt 2; waterprobe 3).

## Afstemming tijdens de bouw
- **Geulen** eerst ±0,024: 6,2 % van de ruis (te breed), nu ±0,018 (4,6 %).
- **Sijpelplekken** eerst kans 0,5 op de vloer: 5,8 % groen in de bronkloof, nu 1,0: 10,1 %.
- **Bomen** eerst één kandidaat per chunk (raster 16, marge 5 voor een gevorkte jeneverbes met blob r 2): ~0,05 boom per
  chunk, te dun voor bosjes. Nu raster 8 met marge 3 (rechte jeneverbes; de gevorkte dode bomen houden de knoestige
  vorm): ~0,1–0,2 boom per chunk gemiddeld, ~0,5–1 per bosjechunk.
- **Hoodoos:** de eerste spline (0 bij −0,25 → 1 bij −0,09, kruising −0,17 = 30 % van de ruis) haalde 60 % van de pilaren
  weg. Een smallere rand (−0,20 → −0,14) veranderde bijna niets. A/B in dezelfde gebieden (oude gewichten in `rock0`): het
  verlies is ~1,4× het open aandeel van de ruis (een voetafdruk moet op alle 9 punten ≥ 0,5 halen, en de randpilaren
  worden kleiner). Daarom de kruising naar −0,27 (21 % van de ruis → ~30 % minder pilaren gemiddeld). De gemeten plekken
  op de testseed liggen toevallig laag in de ruis (lokaal 44 % en 49 % open bij −0,17; `hoodoo2` 18 %).

## Metingen (seed 20260930, 121 chunks per spot; vóór = `run/g2walk` van de nulmeting, na = `run/rock5`/`rock3`)
| Spot | Vóór | Na |
|---|---|---|
| `mesa` plateau (Y ≥ 118) | rood zand 89 % | rood zand 68 %, grove aarde 6 %, rode zandsteen 6 %, grind 5 %, klei 5 % |
| `canyon` plateau | rood zand 89 % | rood zand 64 %, grove aarde 12 %, grind 8 %, klei 4 % |
| `hscarp` plateau | rood zand 90 % | rood zand 65 %, grind 10 %, rode zandsteen 7 %, grove aarde 7 % |
| `spring` kloofbodem (Y < 96) | klei 31 %, grind 1 %, groen 0,3 % | klei 19 %, grind 5 %, mos/gras/grove aarde 10,1 % |
| `canyon` objecten/chunk, lege 8×8 | 4,0 / 35 % | 6,3 / 30 % (rock2) |
| `spring` objecten/chunk, lege 8×8 | 4,9 / 33 % | 14,6 / 23 % (rock2, incl. mos als object) |
| bomen per chunk (`canyon`/`spring`/`hscarp`/`mesa`) | 0 | 0,21 / 0,20 / 0,10 / 0,05 |
| `hoodoo` pilaarkolommen, 8×8 zonder pilaar | 13,4 % / 41 % | 6,7 % / 67 % |
| `hoodoo2` (2660 1950) | 10,4 % / 55 % | 8,0 % / 63 % |
| `hoodoo3` (−380 −3000) | 12,6 % / 43 % | 6,5 % / 70 % |
| hoodoovloer grind (geulen) | 0,1 % | 3,2 % |

- Geen blad met `distance=7` (los blad), geen bladstruik boven lucht, in alle vier kloofgebieden.
- Serverlogs zonder ERROR of "far chunk" (5 werelden).
- **Determinisme** (`det.sh --all`, 3 × 3 chunks: een hoodoogebied, een bronkloof met bomen en sijpelplekken, een
  plateaustuk): kloof en plateau 9/9 identiek; hoodoo 7/9 met 4 blokken `air → loose_sand` op Y 9–15 (instortende
  zandgrotten boven een reservoir, change 26: bekend en geaccepteerd, los van G2).
- Unit tests: `Ring2RockFeaturesTest` (aandelen van alle nieuwe ruisvelden over 3 seeds, de banden van surface rule en
  feature gelijk, de bedrading van `hoodoo_pillar_weight`, marges); `Ring2BiomesTest` bijgewerkt (volgorde per stap).

## Open punten
- Visueel nog niet bekeken (geen client in deze substroom): bosjes (dun?), kleur van de bladstruiken (biome-tint
  `AEA42A`), leesbaarheid van de geulen en de open hoodoovlekken — bij de na-rondgang van de integratie.
- Perf: de noise-stap verandert alleen via `hoodoo_pillar_weight` (per cel, 9 lezingen); meting in de gezamenlijke run.
