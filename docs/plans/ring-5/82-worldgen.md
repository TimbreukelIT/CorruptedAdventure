# Pakket 82 — golf 0 ring 5 (worldgen): ring 5-skelet

Scope: `worldgen` — pakket 82, worktree `/home/user/wt/ring5-g0` op `ring5/golf0` (`golf-0.md` §2, met de antwoorden van
§7: V1-A, V3-A, V4-A, V5-A, **V9-B**, V12-A). Contract: `contract.md` §4.

## Gedaan
1. **Router** (`ring_blend/*`): ring_5 → `ring_5/overworld/region_rot` (temperature), `region_glitch` (vegetation), `band`
   (weirdness), `breakup` (erosion, V12-A), floodedness 0, initial `0,390625 + ring_5/overworld/initial_terrain`,
   `ring_blend/overworld` ring_5 = `ring_5/overworld` (nu het skeletterrein). Nether: `nether_temperature` ring_5 →
   `ring_5/nether/region`, `ring_blend/nether` ring_5 → `ring_5/nether/terrain` (= `add(0, ring_5/nether)`, de
   placeholder; Nether-5 dus gelijk). `switch_at` ongewijzigd (`["ring_5"]`).
2. **Klimaatassen:** `noise/ring_5_rot`, `ring_5_glitch` (−10, [1, 0,5, 0,25]), `noise/nether_ring_5_region` (−9, [1, 1, 0,5]),
   als flat_cache-DF's. Hash-noise `noise/ring_5_cells` (0, [1]).
3. **Stubs** (`0.0`): `ring_5/overworld/height`, `scars`, `glitch`, `inverted`, `caves`; `zone_4_5/overworld/fracture`.
   `breakup` en `castles` zijn flat_caches van `ring_5_output` (in golf 0 alleen de kasteelgordels).
4. **Band-as** `ring/Ring5Band` (puur, `of(layout)`; `value` ∈ [−1, 1], `outer` (≥ 0), `inner`, `signedDistance`):
   lijn op 50 % van ring 5, ruis ±15 % (255 blokken, V1-A), 2 simplex-octaven 1/600 en 1/150 (0,7/0,3) op `noiseSeed` met
   hash `corruptedadventure:ring_5_band`; waarde = getekende afstand / (`SLOPE` · 100), **`SLOPE` 1,8** gemeten (mediaan
   overgang 198 blokken op seed 20260930). `RingLayout.ring5Band()` lazy (zoals ring 4). DF-type `ring_5_band`
   (`Ring5BandFunction`), JSON `ring_5/overworld/band` = flat_cache daarvan.
5. **`Ring5Field`** (`worldgen/densityfunction`, type `ring_5_field`, JSON `ring_5/overworld/field.json`: `hash`
   `ring_5_cells`, `placeholder` `placeholder_terrain`, `settings.cell_size` 128) met eigen `Column`, `ColumnTable` en
   `Decisions` (salt, kasteelplekken; eenmaal per wereld, gedeeld door de kopieën van de noise chunk). Uitgangen via
   `ring_5_output` (`Ring5Output`): `breakup`, `castles`, `initial_height`, `terrain` (3D). Gevonden via
   `RingChunkGenerator.ring5Field(random)` en `worldgen/chunk/Ring5Fields` (per dimensie bij `LevelEvent.Load`, leeg bij
   `ServerStopped`, geregistreerd in `CorruptedAdventure`).
   - **Kasteelplekken (V5-A):** θ0 = 90° · u uit de salt; plek k op θ0 + k · 90° ± 8°; op de straal het punt met
     effectieve afstand 75 % van ring 5 (9.275; bisectie); straal 40–56, top Y 120–140, gordel 40–80 (uniform, gehele
     getallen). Volgorde: index 0 = kleinste hoek tot oost (+x), dan oplopende hoek (oost → zuid → west → noord).
   - Accessors: `castleSites()`, `castleAt(x, z)`, `breakupAt`, `solidGround`, `initialHeight`, `terrain`, `column`;
     `bridgesWithin`, `fragmentsWithin` leeg (84). Records `CastleSite`, `Bridge`, `Fragment`, `Settings`.
6. **Skeletterrein** (tot 83/84): oppervlak `85 + 8 · placeholder_terrain − 6 · max(0, band)` (de band-trede),
   dichtheid `0,125 · (top − y)` geklemd op ±1, geen grotten, geen slides (dus geen bodem-slide in de gordel); per kasteel
   een pinakel (schijf met straal en top, rots tot Y −40, eronder void) in een gordel van void tot onder `min_y` (dichtheid
   −1, `breakup` 1, `initial_height` −64; de bedrockregel legt niets op lucht). `ring_5/overworld` = `interpolated` van de
   uitgang `terrain` (RingBlend zet zijn gate erin).
7. **`initial_terrain`** zoals ring 3/4: `0,125 · cache_2d(initial_height) + gradiënt (−64 → 8, 320 → −40)`.
8. **Vloeistofregel ring 5 (V9-B)** (`RingFluidPicker`, `RingAquifer`):
   - `ring5Dry` = overworld, `ring()` = ring 5, niet de void-zone en **niet zone 4→5** (die houdt de afgrondzee van 63 tot
     85, zoals `golf-0.md` §2.8 zegt): geen zee, geen grondwater, geen lokale lava; open ruimte = lucht, nooit "flow".
   - Lavalaag: alleen in de binnenband, verder dan `RING_5_DAM` (32 blokken getekende afstand) van de bandlijn
     (`ring5LavaColumn`): onder Y −54 vanilla's lava (de picker geeft daar de vanilla-status; de band wordt alleen onder
     −54 gevraagd). **Dam** (`ring5Dam`, strook binnenband tot de lijn): open ruimte onder −54 blijft rots. Buitenband:
     lucht tot onderin. Zone 4→5 had al zijn dam onder −54 (59), dus de lava begint pas achter de zone.
   - Carver-lava: `decidesCarverLava` ook in ring 5 buiten de lavalaag (dam niet snijden, buitenband lucht); in de lavalaag
     vanilla. Injecteerbare band (`RingFluidPicker.Ring5Bands`, `CURRENT_BANDS` = `RingLayout.current()`) voor de tests.
9. **Breukzone-skelet:** `ZoneFracture` (record `argument`, type `zone_fracture`) om `noise_router.final_density` van
   `noise_settings/overworld.json`; `compute`/`fillArray` geven exact het argument, `mapAll` geeft de visitor door;
   statische `strength` (0), `crackAt`, `floeAt` (false).
10. **Biomehaak** (V12-A): geen Java; de preset houdt `placeholder_ring_5` / `placeholder_nether_ring_5`.

## Client/server-impact
Puur worldgen, server-side. `Ring5Band` is common code op de gesyncte layout-seed (de client kan hem rekenen, geen
payload). `Ring5Field`/`Ring5Fields` en de kasteelplekken bestaan alleen op de server (salt uit de wereldseed via de
hash-noise). Geen client-class, geen payload, geen `PROTOCOL_VERSION`.

## Tests
- `./gradlew runData` groen, `src/generated` ongewijzigd. `./gradlew build` groen: 1.144 tests, 0 fouten.
- Nieuw: `Ring5BandTest` (randen 35/65 %, volledig ±1 daarbuiten, 50/50 met tongen, overgang mediaan 170–230 (198),
  seed, eigen instantie per layout en client-layout = server), `Ring5FieldTest` (40 seeds: 4 plekken, 90° ± 16°,
  index 0 dichtst bij oost, maten, 9.275 ± 1, buitenband, pinakel + gordel buiten de zones 4→5 en 5→void; seed-gedrag;
  `castleAt`; pinakel/gordel/oppervlak; gevonden via router met `ZoneFracture`, kopie houdt de beslissingen),
  `Ring5JsonTest` (router, noises, stubs, band-DF = Java op een raster, uitgangen, `initial_terrain`, skeletoppervlak met
  trede, Nether-5 placeholder + regio, preset ongewijzigd), `ZoneFractureTest` (bitgelijk per punt en per array rond zone
  4→5, `mapAll` bereikt de markers, `final_density` in de router).
- Uitgebreid/aangepast: `RingFluidPickerTest` (ring 5 droog, lavalaag alleen binnen achter de dam, band alleen onder −54
  gevraagd, zone 4→5/5→void/Nether niet), `RingAquiferTest` (`ring5IsDryWithItsLavaLayerOnlyInTheInnerBand`,
  `carverLavaInRing5OnlyInTheInnerBandsLavaLayer`; ring 5 uit de "houdt lava"-lijsten), `Ring3JsonTest` (ring 5 niet
  meer 0 in de router).
- S-smoke (tussentijds): verse wereld `r5-82`, log alleen de Yggdrasil-ERROR, `/ca terrainmap 8900 0 1400 4` werkt
  (band-trede en zone 4→5 zichtbaar). Bitgelijkheid ring 1–4: zie `81b-infra.md` (kaartvergelijking vóór/na).

## CODEBASE-delta
Nieuw: `ring/Ring5Band`, `worldgen/densityfunction/Ring5BandFunction`, `Ring5Field`, `Ring5Output`, `ZoneFracture`,
`worldgen/chunk/Ring5Fields`; DF-types `ring_5_band`, `ring_5_field`, `ring_5_output`, `zone_fracture`
(`ModDensityFunctionTypes`, blok "ring 5 — W-terrein"); `RingLayout.ring5Band()`; `RingChunkGenerator.ring5Field`;
`RingFluidPicker` (`ring5Dry`, `ring5LavaColumn`, `ring5Dam`, `RING_5_DAM`, `Ring5Bands`/`CURRENT_BANDS`, picker-regel);
`RingAquifer` (`airFrom`, dam, carver-lava ring 5); JSON `density_function/ring_5/overworld/*`, `ring_5/nether/{region,terrain}`,
`zone_4_5/overworld/fracture`, noises `ring_5_rot`, `ring_5_glitch`, `ring_5_cells`, `nether_ring_5_region`;
`noise_settings/overworld.json` `final_density` = `zone_fracture`. Valkuil: `RingFluidPicker.CURRENT_BANDS` vraagt
`RingLayout.current()` (alleen met een draaiende server; tests geven een eigen band).

## Beslissingen
Gebruiker: V1-A, V3-A, V4-A, V5-A, V9-B, V12-A (`golf-0.md` §7). Implementatiekeuzes:
- `SLOPE` 1,8 (gemeten) voor een mediane overgang van ~200.
- Dam van de lavalaag als strook van 32 blokken getekende afstand binnen de bandlijn (minstens ~6 blokken waar de lijn het
  steilst is); de dam rond de scheuren van de binnenband volgt in 83 (scars bestaan nog niet).
- Zone 4→5 blijft in 82 bij zijn eigen vloeistofregel (afgrondzee Y 0); `ring5Dry` sluit hem uit. `golf-0.md` §2.8 noemt
  ook "de ring 5-helft van zone 4→5" maar zegt in dezelfde alinea dat de zone tot 85 blijft zoals hij is; de tests
  ("zone 4→5 ongewijzigd") volgen dat laatste.
- `CastleSite` kreeg een zevende component `belt` (gordelbreedte), nodig voor terrein en `/ca castles`; posities als
  gehele blokcoördinaten.
- Skeletterrein in Java (uitgang `terrain`) in plaats van JSON: de pinakel en gordel komen uit het veld; geklemd op ±1 zodat
  de interpolatie de wand halverwege legt; geen vanilla-slides (de bodem-slide zou de gordel dichtmaken).
- `ring_5/nether/terrain` = `add(0, placeholder)`: een DF-bestand kan geen kale verwijzing zijn.

## Backlog
- [worldgen] 83: de dam van de lavalaag ook rond de scheuren van de binnenband (scheuren tot in de void).
- [worldgen] 85: zone 4→5 droog (V7-A); tot dan kan afgrondwater aan de buitenrand van de zone tegen een droge ring 5-grot
  staan (vroeger lava).
