# Pakket 59 — golf 0 ring 4 (worldgen): ring 4-skelet

Scope: `worldgen` — pakket 59, in de hoofdwerkmap op `main` (`waves.md` §2 golf 0 punt 3). Contract: `contract.md` §4 (namen hier vastgezet).

## Wat gedaan
1. **Router en stubs** (`src/main/resources/data/corruptedadventure/worldgen/`):
   - `ring_blend/overworld_temperature` ring_4 → `ring_4/overworld/region_water`, `_vegetation` → `region_growth`, `_weirdness` → `band`; `_erosion` ring_4 → 0 (geen deep-dark-route van de aquifer, zoals ring 2/3); `_initial` ring_4 → `0,390625 + ring_4/overworld/initial_terrain`. `switch_at` en `transitions` ongewijzigd (64).
   - Regio-assen: flat_cache van `noise/ring_4_water` en `ring_4_growth` (firstOctave −10, [1, 0,5, 0,25]).
   - Stubs `0.0`: `ring_4/overworld/height`, `island_mask`, `island_hole`, `cliff`, `sundered`, `tunnels`.
   - Nether: `ring_4/nether/region` (flat_cache van `nether_ring_4_region`, −9, [1, 1, 0,5]) in `ring_blend/nether_temperature`; `ring_4/nether/terrain` = verwijzing naar de placeholder `ring_4/nether` (constante 0 zou Nether-4 leeg maken), ingevoerd in `ring_blend/nether`. Nether-4 dus bitgelijk.
2. **Band-as** — één bron: `ring/Ring4Band` (puur, `of(layout).value(x, z)` ∈ [−1, 1], `blighted(x, z)` = waarde ≥ 0). Getekende afstand `effectiveDistance − (r3 + 0,6 · breedte) + 0,225 · breedte · ruis`; ruis = 2 simplex-octaven (~700 / ~180, gewicht 0,7 / 0,3) op `RingLayout.noiseSeed()` met hash `corruptedadventure:ring_4_band`. Waarde = getekende afstand / (2,5 · 32): de ruis maakt de afstand dwars op de lijn ~2,5× zo steil als radiaal (gemeten mediaan), dus de overgang is ~64 blokken (mediaan gemeten 65). Altijd mythisch tot ~33 % van de ring, altijd bedorven vanaf ~87 %; gemiddeld 60/40. DF-type `corruptedadventure:ring_4_band` (`Ring4BandFunction`, geen parameters) roept alleen `Ring4Band` aan; JSON `ring_4/overworld/band` = flat_cache daarvan.
3. **`initial_terrain`** zoals ring 3: `0,125 · cache_2d(initial_height) + gradiënt (−64 → 8, 320 → −40)`, nul op Y = hoogte. `initial_height` = nu precies het placeholder-oppervlak `64 + 8 · noise(placeholder_terrain)`; 60 laat het naar de echte hoogte wijzen.
4. **`IslandField`-skelet** (`worldgen/densityfunction/IslandField`, type `island_field`, JSON `ring_4/overworld/islands.json` met hash-noise `ring_4_island_cells`): eigen raster (`Settings.cellSize`, standaard 128), eigen `Column` (eilandmasker, watervalbereik), `ColumnTable` + eigen `Decisions` (per cel een lijst eilanden, eenmaal per wereld), `find(root)`; accessors `islandAt`, `islandsWithin`, `liftsWithin`, `waterfallsWithin`, `worldTrees` (records `Island`, `Lift`, `Waterfall`, `WorldTree`). Plaatsing (`placeCell`) nog leeg; **nog niet in de final density** (61), dus `find` geeft nu null.
5. **Vinden via de router:** `RingChunkGenerator.islandField(random)` (zoals `volcanicField`); `worldgen/chunk/IslandFields` bewaart per dimensie het veld van de level, gevuld bij `LevelEvent.Load` (vóór de spawnchunks) en geleegd bij `ServerStopped`.
6. **Hoogte-biomehaak** in `RingBiomeSource`: optioneel codec-veld `island_biomes` (`IslandBiomes`: `mythic`, `blighted`, `min_top` 140). Alleen voor quarts die ring 4 raken (`inner` of `outer` = ring 4); eiland via `IslandField.islandAt` op het midden van de quart; per eiland: midden in ring 4 (`ring()`), top ≥ 140, blok-Y ≥ onderkant − 4 → band op het midden (`Ring4Band.blighted`) kiest. Ook boven overgangszones. In de preset beide op `placeholder_ring_4` tot 64.
7. **Vloeistofregel ring 4** (`RingFluidPicker`, `RingAquifer`):
   - zee-water Y 63 vanilla (geen `fluid_levels.ring_4`);
   - `waterOnlyFrom` ook voor ring 4: lokale aquiferlava wordt water boven Y −54, met de stenen lid op −54 zoals ring 2;
   - `ring4DryBelow`: open ruimte onder Y −54 is lucht in ring 4 (droog, geen lavalaag);
   - `ring4DamBelow`: dam van gesteente onder Y −54 over de hele zone 4→5 (beide helften; ring 5 houdt zijn lavalaag pas daarbuiten) en over de ring 4-helft van zone 3→4 (de ring 3-helft is al dicht onder Y 63 door `ring3Seal`);
   - **carver-lava**: vanilla carvers zetten lava op Y ≤ −56 zonder de aquifer te vragen (`steam_fens` gebruikt de ring 2-carvers, met tunnels tot ~112 blokken ring 4 in). Mixin `WorldCarverLavaMixin` (common, op `WorldCarver.getCarveState`) laat in een ringwereld `RingAquifer.carverLava` beslissen via `CarverLava`: in ring 4 en de dammen (`ring4NoLava`) het antwoord van de aquifer (droge bodem lucht, dam null = niet snijden), elders vanilla's lava zonder de update-vlaggen te raken (ring 3 bitgelijk);
   - water-tak: `RingAquifer` leest per kolom `IslandField.Column.fallTop/fallBottom/fallFlowBelow` en zet vallend water (level 8), onderaan stromend; nu leeg;
   - afgrondzee Y 0: in 63.

## Client/server-impact
Alles worldgen, server-side; geen client-class, geen payload, geen `PROTOCOL_VERSION`. `Ring4Band` is gewone common code op de layout-noiseSeed: de client kan dezelfde band berekenen met de gesyncte layout (voor A: weer per band tekenen). `IslandFields` en de haak bestaan alleen op de server.

## Determinisme en prestatie
- Band: noiseSeed (hash van de wereldseed); regio-assen: noise-registry; IslandField-beslissingen puur, nooit via een flat_cache.
- `RingBlend` rekent alleen de ringen van een positie en zet `RingFillGate` in elke flat_cache: de nieuwe assen kosten buiten ring 4 niets. De haak doet niets buiten quarts die ring 4 raken.
- **Bitgelijk:** ring 1–3 en Nether 1–3 buiten de zone 3→4. In de overworld-zone 3→4 veranderen: de klimaatmenging (biome blijft `steam_fens`, fixed), `initial_density` (preliminary surface; aquifer en `above_preliminary_surface`) en erosion. De ring 3-helft blijft onder Y 63 dicht (`ring3Seal`).

## Tests
`./gradlew build` groen (754 tests), `./gradlew runData` groen; `src/generated`: geen wijziging van 59 (alleen de bekende, niet-gecommitte `sounds.json`).
- Nieuw: `Ring4BandTest` (randen, 60/40 met tongen en eilandjes, overgang ~64, seed), `Ring4JsonTest` (router, stubs, band-DF = Java op een raster, `initial_terrain` = placeholder-oppervlak, IslandField-JSON, Nether-4, preset-haak), `IslandFieldTest` (find, kopie gelijk, skelet leeg, voetafdruk).
- Uitgebreid: `RingBiomeSourceTest` (haak: top 139/140, band op het midden, Y onder het eiland, voetafdruk, andere ringen en zonder haak ongemoeid, codec), `RingFluidPickerTest`/`RingAquiferTest` (ring 4 water boven −54, lucht eronder, dammen in zone 4→5 en de ring 4-helft van 3→4, ring 5 houdt lava; carver-lava: ring 4 lucht, dammen gesteente, ring 3 (ook zijn helft van 3→4), ring 1/2/5 en de Nether lava met ongewijzigde update-vlag; skelet zonder vallen), `CommonMixinsTest` (3 common mixins, `WorldCarverLavaMixin` toegepast).
- Aangepast: `Ring3JsonTest` (ring 4 is niet meer 0 in de router), `RingAquiferTest.otherRingsAndTheNetherKeepLava` (zonder ring 4).
- De water-tak zelf krijgt een echte test in 61 (het skelet plaatst geen vallen).

## CODEBASE-delta
`Ring4Band`, `Ring4BandFunction`, `IslandField`, `IslandFields`, `IslandBiomes`; `RingBiomeSource` (`island_biomes`), `RingChunkGenerator.islandField`, `RingFluidPicker` (`ring4DryBelow`, `ring4DamBelow`, `ring4FallColumn`, `waterOnlyFrom` ook ring 4), `RingAquifer` (droog/dam/water-tak), `ModDensityFunctionTypes` (`ring_4_band`, `island_field`), `CarverLava` + `mixin/WorldCarverLavaMixin` (in `corruptedadventure.common.mixins.json`), JSON- en noise-bestanden ring 4, preset (`island_biomes`).

## Beslissingen (gebruiker, 2026-10-13; `02-beslissingenlog.md`)
Q1 A (amplitude 22,5 % ≈ 450, 2 octaven ~700/~180, overgang ~64); Q2 A (`Ring4Band` op de layout-noiseSeed + DF-type); Q3 A (band → weirdness, `region_water` → temperature, `region_growth` → vegetation, −10); Q4 A; Q5 afwijkend: ring 4 droog onder −54, dam over de hele zone 4→5; vervolg carver-lava A: mixin + dam in de ring 4-helft van zone 3→4; Q6 A (erosion 0, floodedness in 73); Q7 A/A/A (register per dimensie, vanaf onderkant − 4, preset nu met placeholders); Q8 A (per eiland op het midden, ook over overgangszones); Q9 A/A (eigen `Column`, nog niet aangesloten); Q10 A (afgrondzee in 63).

Implementatiekeuzes (niet door de gebruiker besloten): `SLOPE` 2,5 in `Ring4Band` om de overgang op ~64 te krijgen bij de goedgekeurde ruis (daardoor puur mythisch tot ~33 % en puur bedorven vanaf ~87 %, i.p.v. ~36/~84 % in het plan); `IslandField.Settings.cellSize` 128 voorlopig (61); de haak kijkt alleen in quarts die ring 4 raken (een eiland dat verder dan de zone over ring 3 of 5 hangt, verliest daar zijn hoogte-biome).

## Backlog
- [worldgen] Naad op de middellijn 4|5 tussen Y −54 en −10: lokale aquiferlava van ring 5 naast water van ring 4 (zoals nu bij 1|2); op te lossen met de vloeistofregel van ring 5.
