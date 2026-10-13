# Ring 4 golf 1 — stroom W-terrein (60–63)

Scope: `worldgen`. Branch `ring4/terrain-g1` (worktree, poort n = 1: server 25571, RCON 25581), basis `main` 7ffbfeb. Contract: `contract.md` §1 en §4; golf: `golf-1.md`; werkwijze: `waves.md` §2, §4, §5. Eén commit per pakket (`worldgen:`).

## Architectuur (alle pakketten)

Het ring 4-terrein is één Java-veld, zoals ring 3: **`IslandField`** (type `island_field`, JSON `ring_4/overworld/islands.json`) wordt het veld van het hele ring 4-overworldterrein. Het rekent per kolom (`ColumnTable`, per thread rond de chunk) en per cel (`Decisions`, eenmaal per wereld):
- `Ring4Relief` (60): het basisterrein uit de regio-assen (exact gelezen, nooit via een `flat_cache`), de band (`Ring4Band`) en eigen ruis;
- de eilanden met hun gaten, meren en vallen (61), de klif 3→4 (62) en `sundered_reach` (63).

Uitgangen via een nieuw type **`island_output`** (`IslandOutput`, patroon `volcanic_output`): per kolom `height` (zonder gaten), `surface` (met gaten), `initial_height`, `island_mask`, `island_hole`, `cliff`, `sundered`; per blok `thin` (+1 eilandblok, −1 open (bron), 0 = het terrein beslist) en `cave_lift`. De gedeelde DF's uit contract §4 (`height`, `island_mask`, `island_hole`, `cliff`, `sundered`) worden `flat_cache`'s van die uitgangen.

De dichtheid `ring_4/overworld.json` volgt het ring 3-schema: `range_choice` op `thin` (eilanden exact per blok, buiten de interpolatie) → anders de geïnterpoleerde stapel `0,125 · (surface − y)` + vanilla-grotten (+ `cave_lift`), met de bekende slides. `initial_height` = ondergrens van `surface` rond de quart (−2), per wereld onthouden zoals ring 3 (valkuil Y 55: de preliminary surface moet ook in gatmeren en de zee kloppen, anders krijgen meren lucht of grotten de verkeerde vloeistof).

De publieke records (`Island`, `Lift`, `Waterfall`, `WorldTree`) en accessors van 59 blijven; de volledige eilandvorm zit in een interne record.

## Pakket 60 — basisterrein
- Laag rond Y 63; water ~50/50 (gemeten: 49 %, mythisch 49 %, bedorven 48 %, testseed).
- Landvorm per biome op de drempels van W-biomes (Q1 = B, afgestemd met `r4-biomes`): conventie region_water HOOG = open water, region_growth HOOG = begroeid, band < 0 mythisch. Mythisch: growth < −0,075 → `crystal_lagoon` (water ≥ 0: zandbanken/atollen tot Y ~64, troggen tot Y 42) | `mist_fjords` (water < 0: plateau Y ~71–103, fjordgeulen tot Y 40); growth ≥ −0,075 → `lotus_shallows` (water ≥ 0,125: ondiep Y ~58–62) | `glowroot_swamp` (±0,125: rond Y 63) | `cascade_vale` (< −0,125: treden van 6 met steile stootborden, tot Y ~110). Mythische helft van de bandovergang (`mist_fjords`): lage fjordplateau-rand, nooit lagune. Bedorven: water ≥ 0,125 → `drowned_forest` (Y ~51–59); daaronder growth ≥ 0 `reed_marsh` (rond Y 63), < 0 `peat_moors` (vlak Y ~65–68 met zinkgaten, 1 per cel van 40 met kans 0,5, straal 3–7, diepte 7–16). Overvloeien ±0,045 (~25 blokken) rond elke drempel; mythisch ↔ bedorven over band −0,3..0,3.
- Rivieren (Q3-A): geribde ruis (−9), bed Y 57 (~11 breed + oevers), buiten open water; op zeeniveau, bevaarbaar.
- `initial_height`: laagste `surface` op het midden en twee ringen (6, 12) rond de quart, zinkgaten erbij, −2; per wereld onthouden. Test: ondergrens van elke kolom van de quart.
- Surface vanilla (W-biomes).
- Gemeten (testseed): surface 40–112, 6 % boven Y 85.

## Pakket 61 — eilanden
- Plaatsing per cel (`cell_size` 128), puur per cel: aantal, grootte en hoogte nemen toe met de ringvoortgang (binnenrand af en toe, buitenrand archipels); maten Q4 (kluiten 5–15, middel 20–60, reuzen 80–150), straal ≤ `cell_size` (notitie #6); alleen waar het eiland met zijn reikwijdte in ring 4 ligt (zones: 62/63).
- Vorm: rafelige omtrek, licht bolle top, omgekeerde kluit met lobben (hergebruik `Volcano.ragged`/`underside`); exact per blok via `thin`.
- Gat = spiegelbeeld: zelfde omtrek (iets ruimer, afgebrokkeld), diepte ≈ kluitdikte, als aftrek in `surface`. Water tot zeeniveau; op hooglanden een eigen meerpeil (laagste rand − 1) met een dijkring en geen grotten eronder (`cave_lift`); de aquifer zet het meerwater (water-tak).
- Watervallen 1–3 per middel/groot eiland: bron in de flank onder de top, vallend water in de kolom erbuiten tot het meer (water-tak, cap per chunk); `waterfallsWithin`.
- Liften: ~1 op 4 middel/grote eilanden, kolom naast de rand van het meer tot net boven de top; `liftsWithin` (blokken: B/W-biomes in golf 2).
- `IslandField` in de final density; haak: één eilandopzoeking per kolom (notitie #5).
- Uitloop van gatmeren op land naar rivier/zee (Q3-A).
- Teststop 5-pre: `/ca terrainmap`-PNG van ring 4.

## Pakket 62 — klif 3→4
- Klif 40–80 aan de buitenrand van zone 3→4 (ring 4-kant): de ring 4-invoer volgt in de zone de ring 3-hoogte (plateau `steam_fens`) en valt bij blend ~0,9 naar het ring 4-terrein; kloven en puinhellingen om de paar honderd blokken (langs de hoek), dampende watervallen van de rand (water-tak; ring 4-helft, dus `ring4FallColumn` blijft geldig, notitie #8). `ring3Seal` ongemoeid. `cliff` 0..1.

## Pakket 63 — `sundered_reach`
- Zone 4→5 (256): de grond breekt op in brokken boven een afgrond met een stilstaande zee op Y 0; landbruggen om de ~500–800 blokken; alleen vastgelegde vallen; `sundered` 0..1. Afhankelijk van Q2.

## Client/server-impact
Alles worldgen op de server; geen client-class, geen payload, geen `PROTOCOL_VERSION`. Nieuwe DF-types worden alleen in `ModDensityFunctionTypes` (blok `// ring 4 — W-terrein`) toegevoegd.

## Determinisme en prestatie
- Puur per seed: ruis via de noise-registry (`forkPositional` per sleutel), celbeslissingen uit de hash-ruis en de cel, nooit via een `flat_cache`; geen `Random` zonder seed. Unit test: veld twee keer gebouwd (en een kopie via `mapAll`) geeft dezelfde kolommen en cellen, ook in omgekeerde volgorde gevraagd.
- Kolommen lui per onderdeel (eilanden apart van het reliëf), zodat de per-blok-uitgang `thin` geen reliëfruis rekent. Buiten ring 4 en zijn zones rekent niets (`RingBlend`-gates). Indicatief ≤ 1,75× vanilla.

## Tests (licht, per pakket)
`./gradlew build`, `./gradlew runData` + `git diff --stat src/generated` (verwacht leeg), unit tests: reliëf (waterfractie, hoogtebereik per stijl, rivieren onder Y 63), determinisme, JSON-structuur, eilanden (dichtheid naar buiten, straal ≤ cel, gaten onder elk eiland, meerpeil < rand), water-tak met echte vallen in `RingAquiferTest`, haak. Alleen na 61 een korte terrainmap.

## Vragen aan team-lead (2026-10-13)
Q1 terrein ↔ biome-indeling op de assen; Q2 zone 4→5 volledig uit ring 4 (`RingBlend`); Q3 rivieren op zeeniveau; Q4 eilandmaten als diameter. Antwoorden (team-lead 2026-10-13): **Q1 = B** (W-biomes is eigenaar van de drempels, terrein volgt; conventie afgestemd met `r4-biomes`), **Q2 = B** (menging ring 4 ↔ ring 5 over zone 4→5 blijft, geen `RingBlend`-wijziging; ring 4 bouwt de hele zone, ring 5 stemt af), **Q3 = A**, **Q4 = A**.

## Voortgang
- 2026-10-13 19:45: branch aangemaakt, plan geschreven, vragen Q1–Q4 verstuurd.
- 2026-10-13 20:10: 60 gebouwd (build, runData groen; `src/generated` ongewijzigd).

## CODEBASE-delta
**60**
- `worldgen/densityfunction/IslandField` (type `island_field`) is nu het hele ring 4-overworldterrein: codec `hash`, `relief` (`Ring4Relief`), `settings`; ringlayout via `Supplier<RingLayout>` (`RingLayout::current`; tests eigen layout); kolom (`Column`: `height`, `surface`, `river`, `fjord`, `islandMask`, `islandHole`, valbereik) lui gevuld; `initialHeight` (per wereld op het 4-blokraster), `resolve`/`resolveIfBound` (door holders, markers en ring gates), `salt`.
- Nieuw `worldgen/densityfunction/Ring4Relief`: basisterrein ring 4 uit de klimaatassen (exact), band en eigen ruis; drempels van W-biomes (`WATER_SPLIT` 0,125, `MYTHIC_GROWTH` −0,075, `BLIGHTED_GROWTH` 0, `BLEND` 0,045); landvormen lagune, lotus, moeras, terrassen, fjordplateau + fjorden, diep water, rietmoeras, veen + zinkgaten; rivieren op zeeniveau.
- Nieuw `worldgen/densityfunction/IslandOutput` (type `island_output`, `ModDensityFunctionTypes.ISLAND_OUTPUT` in het blok `// ring 4 — W-terrein`): uitgangen `height`, `surface`, `initial_height`, `island_mask`, `island_hole`, `river`, `fjord`, `thin`, `cave_lift`.
- JSON `density_function/ring_4/overworld.json` (stapel als ring 3: `thin` per blok, daarna geïnterpoleerd `terrain` met vanilla-grotten + `cave_lift`); `ring_4/overworld/{islands, height, surface, terrain, thin, cave_lift, initial_height}.json`; noises `ring_4_{hills, detail, shoals, rivers, fjords, warp}`.
- Tests: `Ring4ReliefTest` (nieuw), `IslandFieldTest` (testveld uit de JSON, `field()` publiek; determinisme), `Ring4JsonTest`, `RingAquiferTest` (testveld).

## Beslissingen
- 2026-10-13 | worldgen | Ring 4-terrein volgt de biome-drempels van W-biomes (region_water hoog = open water, region_growth hoog = begroeid, band < 0 mythisch; water ±0,125/0, growth −0,075/0); de mythische helft van de bandovergang (`mist_fjords`) krijgt een lage fjordplateau-rand | Q1 = B: één indeling voor biome en landvorm, eigenaar W-biomes.
- 2026-10-13 | worldgen | Zone 4→5: de menging ring 4 ↔ ring 5 blijft (geen `RingBlend`-wijziging); ring 4 bouwt de hele zone, ring 5 stemt zijn invoer af | Q2 = B.
- 2026-10-13 | worldgen | Rivieren op zeeniveau (bevaarbaar); vallen van terrasranden via de water-tak; gatmeren met uitloop | Q3 = A.
- 2026-10-13 | worldgen | Eilandmaten (kluiten 5–15, middel 20–60, reuzen 80–150) zijn diameters | Q4 = A.

## Backlog
- [worldgen] Zone 4→5: de buitenkant van de afgrond mengt tot ring 5 met de placeholder-grond van ring 5 (Q2 = B); ring 5 (81–99) stemt zijn invoer af op `sundered_reach`.
