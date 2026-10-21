# Ring 5 golf 1 — stroom W-nether: 87 (na INT1: 87b)

Scope: `worldgen` — pakket 87 in branch `ring5/nether-g1`, worktree `P:/Projecten/Java/wt/r5-nether-g1`, poort n = 3
(server 25573, RCON 25583), basis = de startcommit van `golf-1.md`. Na INT1, op main: **87b** (echte Nether-5-blokken in de
surface), eigen worktree en `worldgen:`-commit.

Bronnen: `contract.md` §1 (Nether ÷ 4), §2, §4 (Nether-router, vloeistof Nether-5), §11; `golf-0.md` §7 (V10-A);
`fase-1-wereld.md` → "Nether-tegenpool — Nether-ring 5"; `docs/plans/ring-3/contract.md` §4 (dam-regel); sjabloon
Nether-4 (`docs/plans/ring-4/golf-1-nether.md`: `GiantGeodes`, `nether_ring4_terrain.py`, `nether_ring4_surface.py`,
`ring_depth`). `CODEBASE.md` kent golf 0 nog niet: lees de CODEBASE-delta van `82-worldgen.md` erbij.

**Bezit** (`waves.md` §1/§5): `density_function/ring_5/nether/**`, `noise/nether_ring_5_*`, `noise_settings/nether.json`
(ring 5-tak), preset Nether `rings.ring_5` en `fluid_levels.ring_5`, een eigen methode voor Nether-5 in `RingFluidPicker`
(als 87 die kiest), `NetherRing5Biomes`, `NetherRing5BiomeModifiers`, `NetherRing5Features` (leeg tot 96),
`NetherRing5Carvers` (leeg), `NetherRing5BiomeTags`, `NetherRing5BiomeNames`, nieuwe scripts
`tools/worldgen/nether_ring5_terrain.py`/`nether_ring5_surface.py`, eigen DF-types/surface-condities (blok
`// ring 5 — W-nether` in `ModDensityFunctionTypes`/`ModSurfaceConditions`).

**Doel 87:** de 'zuivere' witte, stille Nether-5 (2.000–2.425; zones 1.968–2.032 en ~2.409–2.441) als eigen ring 5-invoer
van `ring_blend/nether`, met `white_halls` als basis en `hollow_cathedral` als zeldzame enorme holtes (V10-A); geen lava;
de trede 4\|5 afgedekt; genoeg loopruimte.

---

## 1. Werk 87

1. **Kathedraalveld** (nieuw DF-type, voorstel `corruptedadventure:nether_cathedrals`, patroon `giant_geodes`: pure
   celhash op de wereldseed, hash-noise `nether_ring_5_cathedral_cells`): per cel van **400 × 400** Nether-blokken
   hoogstens één kathedraal (kans ~0,8, zodat er ~30 rondom liggen), **Ø 120–180**, het midden zo dat de hele hal plus
   24 marge binnen Nether-5 valt (buiten beide zones). Accessor `cathedralAt(x, z)` (en een `find` zoals `GiantGeodes`).
2. **Regio-as:** `ring_5/nether/region` (de Nether-router-temperature van ring 5, sinds 82) wordt het kathedraalmasker
   (1 binnen de hal, 0 erbuiten, korte rand). De ruis `nether_ring_5_region` mag blijven als variatie voor de witte
   vlaktes (punt 3).
3. **Terrein** `ring_5/nether/terrain` (vervangt de verwijzing naar de placeholder; `ring_5/nether.json` weg), voorstel
   (bijsturen bij 6a):
   - **vloer**: precies vlak op **Y 31** (waar de lavazee lag; wordt de spiegelsteen-vloer), daaronder vast tot de
     bedrockvloer (geen open ruimte onder Y 31 in heel Nether-5);
   - **witte vlaktes**: rechthoekige, lage verhogingen (2–6 hoog, randen op het zuilraster) op ~30 % van de vloer, uit de
     regio-ruis gekwantiseerd;
   - **plafond**: vlakke onderkant op Y ~104, dicht tot het bedrockdak;
   - **zuilenrasters** (`white_halls`): vierkante zuilen 4 × 4 van vloer tot plafond, raster om de 24 blokken,
     zonder jitter (de orde is het punt), per kathedraalcel een eigen offset zodat het niet één eindeloos raster is;
     geen zuilen binnen 16 van een kathedraal of in de zones;
   - **`hollow_cathedral`**: cilinder van de vloer (Y 31) tot een opgetild dak op Y ~120 (onder het bedrockdak), leeg,
     met **één reuzenzuil Ø 20–30** in het midden van vloer tot dak (de vorm; de witte kern erin en het stille licht zijn
     96); de rand van de hal loopt over 8–16 blokken van 104 naar 120 (geen trap);
   - **aansluiting**: in zone 4→5 mengt `ring_blend` met de bottenduinen van Nether-4 (vloer Y ~43); in zone 5→void
     de bestaande Nether-void (`void/nether.json` blijft).
4. **Vloeistof (contract §4, V10):** geen lava in Nether-5. Kies **`fluid_levels.ring_5`** (laag genoeg dat er nergens
   lava staat) of een eigen `RingFluidPicker.netherRing5Dry`; noteer de keuze. **Trede 4\|5**: Nether-4 heeft lava op Y 10
   en een dam vast tot Y 33 binnen ~24 van zijn grenzen (65); Nether-5 heeft geen open ruimte onder Y 31, dus geen
   lavawand van Nether-4 naar Nether-5. Test de trede in `Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround`
   (ring 5-tak erbij).
5. **Biomes** (`NetherRing5Biomes`, `ALL`): `white_halls`, `hollow_cathedral`; temperatuur 0,5, geen neerslag;
   **geen spawns** (contract §2), **geen carvers** (dammen heel), **geen features** (96), geen biome-modifiers.
   Effecten tijdelijk: fog wit (`E8E8EC` / `F4F4F6`), lucht zwart, **geen muziek, geen ambient-geluid, geen
   ambient-particles**; A levert in 98 de builder in `BiomeAmbience`.
6. **Preset:** Nether `rings.ring_5` = `multi_noise` op temperature (= het masker): ≥ 0,5 → `hollow_cathedral`, anders
   `white_halls` (vervangt `placeholder_nether_ring_5`; de key blijft tot golf 3).
7. **Surface** (`noise_settings/nether.json`, ring 5-tak direct na Nether-4, vóór vanilla's `hole` → lava; uit
   `tools/worldgen/nether_ring5_surface.py`), vanilla-placeholders: vloer (top op Y 31) `smooth_quartz` (spiegelsteen),
   onder de vloer `diorite` (kalkrack), vlaktes, plafond en wanden `calcite` (witsteen), zuilen `quartz_pillar` (witsteen-
   zuil). Zuil ↔ plafond onderscheiden via een eigen surface-conditie (voorstel `corruptedadventure:nether_ring_5_pillar`,
   leest het zuilraster; patroon `ring_depth`) of een andere route; noteren. De wissel naar B's blokken (87b) is een
   wijziging van de constanten in het script.
8. **Tags** (`NetherRing5BiomeTags`): `is_nether_ring_5` (beide), `silent` (beide; W-biomes zet `false_meadow` erbij),
   `c:is_nether`, `c:is_dry/nether`; geen `minecraft:`-tags.
9. **Lang** (`NetherRing5BiomeNames`, voorstel): White Halls / Witte Hallen, Hollow Cathedral / Holle Kathedraal.
10. **Ruimte-check** (test of script, niet gecommit als script): beloopbaar = 2 lucht boven een vaste, niet-vloeibare
    vloer, stap ±1.

## 2. Na INT1: 87b — echte Nether-5-blokken in de surface
Constanten in `nether_ring5_surface.py` naar B's blokken (90): vloer `mirror_stone`, onder de vloer `chalkrack`, vlaktes
`polished_white_stone`, plafond en wanden `white_stone`, zuilen `white_stone_pillar` (as y), de rand van de vlaktes
`white_stone_bricks` (voorstel). Test: elk blok uit de tak bestaat; geen vallend blok. Kaarten opnieuw (main).

## 3. Client/server-impact
Geen client-code; worldgen (server-side), data-driven plus één DF-type en eventueel één surface-conditie (common, puur).
Geen payload, geen `PROTOCOL_VERSION`. Biomes/tags/lang via datagen.

## 4. Determinisme
Terrein uitsluitend uit density functions op de wereldseed. Het kathedraalveld is puur: geseede celhash, vaste punten;
`mapAll` bedraadt alleen de noises (geen `flat_cache`-markers die per chunk anders antwoorden), zoals `giant_geodes`.

## 5. Tests (licht: B, D, unit)
- `NetherRing5BiomesTest`: bootstrap = `ALL`; geen spawns, carvers, features; geen muziek/ambient.
- `NetherCathedralsTest`: per cel ≤ 1; Ø 120–180; hal + marge binnen Nether-5 (beide zones vrij); aantal op 5 seeds
  25–35; kopie van de noise chunk geeft dezelfde hal; puur per seed.
- `NetherRing5JsonTest`: vloer exact Y 31 en vast eronder (raster over de ring); zuilen 4 × 4 om de 24 van vloer tot
  plafond; geen zuil binnen 16 van een kathedraal; dak in de hal ~Y 120; reuzenzuil Ø 20–30; elk bestand een object, de
  noises bestaan; surface-tak vóór de lava-regel.
- `Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround` met de ring 5-tak (trede 4\|5); `Ring5JsonTest`: alleen de
  Nether-5-placeholder-assert aanpassen (de overworld-assert is van W-biomes).
- Ruimte: ≥ 95 % van de halvloer beloopbaar; vrije hoogte boven de vloer ≥ 60 (hallen) en ≥ 80 (kathedraal).
- D: `src/generated` = 2 biome-JSON's, tags, lang (gecommit).

**Af als (87):** B, D en unit tests groen; S-smoke op een verse wereld met `/execute in minecraft:the_nether run ca
terrainmap 2200 0 300 1` (hoogte + biome: hallen, zuilenraster, minstens één kathedraal) en een doorsnede door een
kathedraal (`section`), PNG's in `build/test/maps/golf-1/`; positie van een kathedraal aan main (voor `ring5.txt`).
De lavawand-scan (`tools/nether-lava-walls.py` r 2.000, `lavascan.py`) doet INT1.

## Voortgang

## CODEBASE-delta

## Beslissingen

## Backlog
