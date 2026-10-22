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
Voortgang: 2026-10-21 20:40 worktree `P:/Projecten/Java/wt/r5-nether-g1` op `ring5/nether-g1` vanaf 757523bf; `run/` klaar (poort 3); eerste build groen (20:49)
Voortgang: 2026-10-21 21:00 `nether_cathedrals` (Java), terrein-/surfacescript, biomes, tags, lang, preset; gerichte tests
Voortgang: 2026-10-21 21:15 kalibratie (aantal kathedralen, aandeel vlaktes); gerichte tests groen; volledige build + runData
Voortgang: 2026-10-21 21:28 B groen (1.167 tests, 0 fouten), D groen; S-smoke `r5-87` (log 0 ERROR), kaarten in `build/test/maps/golf-1/`; zelfreview; commit

Voortgang: 2026-10-21 reviewbevindingen 3dcfb263 verwerkt (validaties, test eigen cellen per bedrading, zone 4→5-raster in de lavatrede-check); B (1.167 tests, 0 fouten) en D groen

## Tests en metingen (87)
- **B** `./gradlew build` groen: 1.167 tests, 0 fouten (incl. `ClientIsolationTest`). **D** `runData` groen;
  `src/generated`: 2 biome-JSON's, `is_nether_ring_5`, `silent`, `c:is_nether`, `c:is_dry/nether`, lang en/nl (gecommit).
- **Kathedralen** (unit, 5 seeds): 31 / 31 / 28 / 27 / 32; op seed 20260930 o.a. (2248, 212) r 90, (2189, −240) r 72,
  (2197, 588) r 79, (−539, −2149) r 90, (148, 2286) r 62 (Nether-coördinaten; `/ca castles`-achtig commando bestaat niet,
  `NetherCathedrals.cathedralsWithin`).
- **Vorm** (unit, seed 20260930): vloer overal top Y 31 en vast tot Y 0; plafond vanaf 104; zuilen 4 × 4 om de 24, geen
  binnen 16 van een kathedraal of < 56 van een middellijn; dak Y 120 binnen de rand, rand ≤ 3 per blok; reuzenzuil
  21–31 breed. Vlaktes 31 % van de open halvloer. **Ruimte:** 100 % van de halvloer beloopbaar (2 lucht boven vaste
  vloer), 99,3 % ook met stap ±1 (de rest = vlakteranden); vrije hoogte ≥ 66 in de hallen, ≥ 88 in een kathedraal.
- **S-smoke** (verse wereld `r5-87`, seed 20260930, poort 3): log 0 ERROR, geen client-class-fout;
  `/execute in minecraft:the_nether run ca terrainmap 2200 0 300 1` (1,7 s; hallen, zuilenraster, vlaktes, twee
  kathedralen, aansluiting op Nether-4 en de void-rand) en twee doorsneden (`section 1900 212 2500 212` door de kathedraal
  bij (2248, 212) en zone 4→5; `section 2300 -300 2300 300`). Echte chunks rond (2248, 212) (forceload): reuzenzuil
  `quartz_pillar`, vloer Y 31 `smooth_quartz`, eronder `diorite`, lucht Y 32–119, dak Y 120 `calcite`.
- Nether-ring 1–4: buiten zone 4→5 ongewijzigd door constructie (alleen de ring 5-invoer van `ring_blend/nether` en
  `nether_temperature`, een eigen surfacetak per biome en `fluid_levels.ring_5`); niet met kaarten vóór/na gemeten.
  Zone 4→5 verandert (mengt nu met het nieuwe Nether-5-terrein). Lavawand-scan r 2.000 en `lavascan.py`: INT1.

## CODEBASE-delta
- **Klassen:**
  - `worldgen/densityfunction/NetherCathedrals` ✅ 87: DF-type `corruptedadventure:nether_cathedrals`, het hele
    Nether-5-veld als pure cellen van 400 (per cel een eigen offset van het zuilraster en hoogstens één kathedraal).
    Twee uitgangen uit dezelfde settings (`output`): `mask` (2D 0..1, 1 binnen de rand van een kathedraal; de
    router-temperature van ring 5) en `terrain` (3D dichtheid, exact per blok, niet geïnterpoleerd: vloer top Y 31 en
    vast tot de bodem, vlaktes 2–6 op de tegels tussen de zuilen uit `nether_ring_5_region`, plafond vanaf Y 104,
    zuilen 4 × 4 om de 24, kathedraal: dak naar Y 120 over een smoothstep-rand van 8–16, reuzenzuil r 10–15).
    Codec: `output`, `cells` (hash-noise), `plains` (noise), `edge` (DF), `settings` {`cathedrals`, `halls`}.
    Accessors: `mask`, `cathedralAt`, `cathedralsWithin`, `cellAt`, `pillarAt`, `floorTop`, `ceiling`, `reader()`
    (eigen geheugen per gebruiker/thread), `find(root)` (de `terrain`-uitgang). Cellen eenmaal per wereld, gedeeld door
    de kopieën (`mapAll` bedraadt alleen de noises, zoals `GiantGeodes`); per thread een geheugen van cellen, tegels en
    kolommen. Een kathedraal ligt met footprint + `clearance` (16) in zijn eigen cel en met `edge` = 0 op de hele omtrek
    (punten om de 4 blokken).
  - `worldgen/feature/NetherRing5Terrain` ✅ vindt de `nether_cathedrals`-terrein-DF per random state (zoals
    `NetherRing4Terrain`); voor de surface en straks 96.
  - `worldgen/surface/NetherRing5PillarCondition` ✅ surface-conditie `corruptedadventure:nether_ring_5_pillar`: waar
    een zuil (raster of reuzenzuil) staat, via `NetherCathedrals.Reader`. `registry/ModSurfaceConditions` blok
    `// ring 5 — W-nether`.
  - `registry/ModDensityFunctionTypes` ✅ blok `// ring 5 — W-nether`: `nether_cathedrals`.
  - `worldgen/biome/NetherRing5Biomes` ✅ `white_halls`, `hollow_cathedral` (`ALL`), temperatuur 0,5, geen neerslag,
    geen spawns/carvers/features, fog `E8E8EC` / `F4F4F6`, lucht zwart, geen muziek/ambient/particles (tijdelijk, tot
    A in 98); `CATHEDRAL_THRESHOLD` 0,5 (preset).
  - `datagen/tags/NetherRing5BiomeTags` ✅ `is_nether_ring_5`, `silent`, `c:is_nether`, `c:is_dry/nether`;
    `datagen/lang/NetherRing5BiomeNames` ✅ White Halls / Witte Hallen, Hollow Cathedral / Holle Kathedraal.
- **Databestanden:**
  - `density_function/ring_5/nether/`: `edge` (`ring_edge` ring_5, breedte 56 = halve zone 4→5 + marge 24),
    `region` (`flat_cache` van `nether_cathedrals` `mask`), `terrain` (`nether_cathedrals` `terrain`); de placeholder
    `ring_5/nether.json` is weg. Noise `nether_ring_5_cathedral_cells` (−4, [1], alleen hash); `nether_ring_5_region`
    (82) is nu de vlakte-ruis.
  - `noise_settings/nether.json`: ring 5-surfacetak direct na Nether-4 (index 6, vóór `hole` → lava).
  - preset: Nether `rings.ring_5` = `multi_noise` op temperature (≥ 0,5 `hollow_cathedral`, anders `white_halls`),
    `fluid_levels.ring_5` = **−1** (geen lava).
  - `tools/worldgen/nether_ring5_terrain.py`, `nether_ring5_surface.py` (W-nether; idempotent).
- **Tests:** `NetherCathedralsTest`, `NetherRing5JsonTest`, `NetherRing5BiomesTest`; `Ring3JsonTest` (ring 5-tak in de
  lavatrede-check: ring 5 is nu de lage kant van 4\|5), `Ring5JsonTest` (alleen de Nether-asserts).
- **Correctie contract (INT1 past `contract.md` §4 toe):** de tabelregel `ring_5/nether/region` ("flat_cache van
  `nether_ring_5_region` (−9)") klopt sinds 87 niet meer: het is de `flat_cache` van het kathedraalmasker
  (`nether_cathedrals`, `output` `mask`); `nether_ring_5_region` is nu de ruis van de vlaktes (`plains`).
- **Valkuilen:** `ThreadOwned` op een gedeelde instantie (climate sampler, `/ca terrainmap`-workers) wisselt per thread
  en maakt dan steeds een nieuw geheugen: `mask` leest daarom direct de gedeelde map, de surface een eigen `Reader`.

## Beslissingen
- (implementatie) Nether-5-terrein als één Java-DF (`nether_cathedrals`, twee uitgangen) in plaats van JSON: het zuilraster
  met offset per cel, de gekwantiseerde vlaktes en de ronde hallen vragen modulo-/afstandsrekenwerk dat JSON niet heeft;
  exact per blok (geen interpolatie), dus de vloer ligt overal precies op Y 31 en de zuilen zijn scherpe 4 × 4. De
  getallen staan in de JSON (script), niet in Java.
- (implementatie) Vloeistof: `fluid_levels.ring_5` = −1 (geen eigen `RingFluidPicker`-methode): niets in Nether-5 is
  open onder Y 31, en met −1 kan er ook nergens lava ontstaan. Trede 4\|5 = 10 / −1; de lage kant is nu ring 5, gedekt
  door de vloer (vast tot Y 31 ≥ 10); Nether-4 houdt daarnaast zijn eigen dam tot Y 33.
- (implementatie) Kathedralen: cel 400, kans **0,9** (plan ~0,8), 24 pogingen: gemeten 27–32 op 5 seeds (met 0,8/8
  pogingen 21–26). Footprint Ø 120–180 (r 60–90 = waar het dak van het plafond loskomt), rand 8–16, reuzenzuil r 10–15;
  footprint + 16 in de eigen cel (geen twee hallen tegen elkaar, geen zuil van de buurcel erin); omtrek ≥ 56 van de
  middellijnen (zone 4→5 32 + marge 24; aan de voidkant dus ≥ 40 voorbij de zone).
- (implementatie) Zuilen: raster 24, 4 × 4, offset per cel (0–23), alleen hele zuilen ≥ 4 van de celrand, niet binnen
  16 van een kathedraal, niet waar `edge` > 0 (dus ≥ 56 van de middellijn). Vlaktes: tegels tussen de zuilmiddens
  (randen door de zuilen), hoogte uit `nether_ring_5_region` op het tegelmidden: drempel 0,08, +1 per 0,08, 2–6;
  gemeten 31 % van de open halvloer verhoogd; geen vlakte bij een kathedraal of de zones.
- (implementatie) Kathedraaldak: laagste vaste blok per kolom = 104 + 16 · smoothstep(rand), afgerond (≤ 3 per blok);
  binnen de rand Y 120. Masker = (r − d) / rand, biome-drempel 0,5 (midden van de rand).
- (implementatie) Surface: vloer-topblok (Y 31) `smooth_quartz`, eronder `diorite`, zuilen (conditie
  `nether_ring_5_pillar`) `quartz_pillar`, al het andere `calcite`. Bovenop een vlakte dus `calcite`, geen spiegelsteen.

- (review 3dcfb263) `Settings` weigert nu ook: een reuzenzuil die in de kleinste hal met de breedste rand niet binnen de
  rand past (`pillar_radius.max < radius.min − rim.max`), een dak boven Y 121 (vanilla's netherrack-regel `below_top 5`
  begint op Y 122) en een hoogste vlakte tot aan het plafond (`floor_top + plains_height.max < ceiling`).

## Backlog
- `atmosphere` (98, A): zonder eigen muziek speelt de client in een biome zonder `background_music` vanilla's
  `Musics.GAME`; "geen muziek" in Nether-5 vraagt een stille muziek of de `silent`-haak van A. Effecten tijdelijk in
  `NetherRing5Biomes.effects` → `BiomeAmbience.netherRing5`.
- `infra` (INT1): `MapPalette` vaste kleuren voor `white_halls` en `hollow_cathedral`.
- `worldgen` (golf 3): `placeholder_nether_ring_5` wordt niet meer gebruikt (preset); weghalen met de andere placeholders.
- `worldgen` (6a, gebruiker): de vlaktes hebben randen van 2–6 hoog; zonder blokken kom je er niet op (stap ±1: 99,3 %
  van de halvloer). Eventueel een trede of lagere randen.
- `infra` (INT1): de klasse-doc van `Ring5JsonTest` noemt Nether-5 nog placeholder (W-biomes past dezelfde test aan).
