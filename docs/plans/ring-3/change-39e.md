# Change 39e — worldgen-prestatie (ring 1–3 en Nether)

Scope: `worldgen` — prestatie-change na teststop 4a (vraag gebruiker: "terreingeneratie verloopt soms langzaam, chunks laden stuk voor stuk in"), parallel aan 39d (ring 3-reliëf). Vanaf main bf63d17, branch `ring3/perf-39e`. Gedrag-neutraal: zelfde wereld per seed.

Bron: `teststop-4a.md` §E, backlog "JIT-deoptimalisatiestorm", "Surface-stap ring 3", "Overhead van het ringraamwerk".

Afbakening met 39d: `VolcanicField.java`, `VolcanicSettings.java`, `ring_3/overworld/field.json` en `tools/worldgen/ring3_surface.py` niet aangeraakt (aanbevelingen hieronder).

Client/server-impact: geen. Alles is worldgen op de server (density functions, aquifer, één surface-conditie); geen netwerk, geen client-code.

## 1. Profiel: waar de tijd zat (baseline bf63d17)

Methode: één server met mod- en vanilla-dimensies (`perftest:vanilla`, `perftest:vanilla_nether`, zelfde seed 20260930), 16 × 16 chunks per gebied via `forceload`, JFR (`jfr start`) met **alle** chunkstappen, wandtijd per gebied, deopt-events; hete methodes, parks en allocaties per gebiedsvenster met een eigen JFR-consumer (`build/c39e/tools/JfrProf.java`, niet gecommit).

- **De wandtijd is de echte klacht.** Voor 256 chunks (volledig, met features en licht): mod ~30 s, vanilla ~9,5 s, in elke overworld-ring, ook ring 1. Nether ≈ vanilla.
- **Weinig parallelisme.** De chunkpijplijn houdt gemiddeld maar 2–3 worldgen-threads bezig (de rest parkt in de ForkJoin-pool: 774 s park-tijd in een venster van 37 s). Elke ms CPU per chunk is dus direct wachttijd.
- **CPU per stap, ring 1, som over 256 chunks (koud):** biomes 8,1 s (vanilla 2,0), noise 16,3 s (10,3), surface 6,4 s (2,4). De biomes-stap was de grootste extra post, niet de noise-stap.
- **biomes:** `NoiseChunk.<init>` vulde de `flat_cache`s van álle ringen (ring 2 ~25, ring 3 ~9, Nether-3 ~14 markers, 25 punten per chunk), ook ver buiten die ringen; `RingGate` gate'te alleen `interpolated`. Daarnaast de `mapAll`-kopie van de hele router per noise chunk (`NoiseChunk.wrap`, record-hashing).
- **surface:** de twee top-level ring 2-gesteentetakken (`biome` canyonlands/hoodoo_fields/dry_riverbeds en oasis_flats, elk met `y_above 8`) gelden voor hele steenkolommen; hun `biome`-conditie zocht voor **elk steenblok in elke ring** het biome op via `BiomeManager` → `WorldGenRegion.getChunk` (~14 % van alle worker-CPU in ring 1 en 3).
- **noise:** per blok `RingColumnCache.ringAt` (ThreadLocal) in `RingBlend.compute` en `RingAquifer`, plus de per-blok-ringchecks in `RingAquifer`/`RingFluidPicker` (de plek van de deopt-storm).
- **Deopts (koude ronde, geen warm-up):** 45.482 events, `unstable_if/none` vooral in vanilla (`LevelLightEngine` 20k, `applyBiomeDecoration` 9,5k, `SurfaceSystem` 6k) en 2,2k in `RingAquifer.computeSubstance`. De stilstand van 22 min is niet gereproduceerd.

## 2. Wat er veranderd is

1. **Flat caches achter een vul-gate** (`RingFillGate`, nieuw; `RingGate.gateCacheMarker`, was `gateInterpolated`; `RingBlend.mapAll`; `RingChunkGenerator.createNoiseChunk`): elke `flat_cache`-marker in een ringinvoer krijgt een `RingFillGate`. Die geeft alleen tijdens het bouwen van de noise chunk van een chunk (`RingFillGate.duringFill`, een ThreadLocal) 0 voor de hele chunk, als de ring geen van de 25 cachepunten bereikt (marge 32); op elk ander moment is hij het argument zelf. Eerste versie (een puntgate zoals `RingGate`) was niet gedrag-neutraal: zie "Review 1".
2. **`corruptedadventure:ring_reach`** (nieuwe surface-conditie, `RingReachCondition`, `dimension` + `ring`, marge 32 blokken, één kolomlookup per kolom) vóór de twee top-level ring 2-gesteentetakken in `noise_settings/overworld.json`. Die biomes bestaan alleen waar ring 2 gewicht heeft (eigen ring of zone); het biome van een blok komt van een quarthoek ≤ 5 blokken per as verderop. De JSON-edit is een round-trip met dezelfde opmaak als `ring3_surface.py` (indent 2, CRLF); de ring 3-takken (`sequence[1]`) zijn niet geraakt, het script van 39d blijft werken.
3. **`RingAquifer` per kolom:** rots (`substance > 0`) gaat zonder ringlookup naar vanilla (die daar `null` geeft); open ruimte leest een tabel per kolom van de chunk (16 × 16 direct-mapped, sleutel x+z) met de per-kolom-grenzen uit `RingFluidPicker` (`waterOnlyFrom`, `ring3LavaColumn`, `ring3DryFrom`, `ring3SealBelow`). Het per-blok-pad vergelijkt alleen nog Y; geen vertakking per blok op de ring (de plek van de deopt-storm). De regels en hun volgorde zijn ongewijzigd; de per-blok-predicaten in `RingFluidPicker` zijn nu een vergelijking op de per-kolom-grens (één bron).
4. **`RingBlend.fillArray`:** vallen alle punten van een array in één ring zonder menging, dan vult die invoer de array zelf (vanilla's array-paden en caches), anders per punt zoals voorheen. `switch_at` als `boolean[]` per ordinal.

Niet gedaan (bewust):
- `HeatRule.inZone`/`WaterEvaporation` (servertick, `conditions`-categorie, geen worldgen; de deopts daar zijn per event, niet per blok).
- `NoiseChunk.wrap`/`mapAll`-kopie van de router per chunk: vanilla-gedrag; vergt een mixin of een kleinere router. Backlog.
- `RingFluidPicker.computeFluid` (allocatie van een `FluidStatus` per aanroep): klein in het profiel.

## 3. Metingen

### 3a. Snelle vóór/na (warm, één ronde elk, 7 gebieden; 2026-10-07 20:10–20:31)
Wandtijd voor 16 × 16 chunks (volledige generatie), mod (vanilla in dezelfde ronde):

| Gebied | bf63d17 | 39e |
|---|---|---|
| ring 1 (20,20) | 30,7 s (9,2) | **17,7 s** (11,6) |
| ring 2 (148,−8) | 23,9 s (8,3) | 24,1 s (8,4) |
| horizonreus (−250,−4) | 27,9 s (8,8) | **21,3 s** (8,8) |
| caldera (−250,−120) | 27,2 s (8,1) | **21,0 s** (7,8) |
| basaltstromen (295,−36) | 24,6 s (9,3) | **19,7 s** (9,5) |
| Nether-2 (31,−8) | 6,6 s (9,9) | 6,8 s (9,4) |
| Nether-3 oost (66,−8) | 5,5 s (6,8) | 5,5 s (6,5) |

Mediaan ms/chunk, noise (verhouding mod/vanilla) en surface (mod / vanilla):

| Gebied | noise bf63d17 | noise 39e | surface bf63d17 | surface 39e |
|---|---|---|---|---|
| ring 1 | 48,8 (1,38×) | 38,1 (0,90×) | 21,4 / 7,6 | 11,9 / 7,7 |
| ring 2 | 52,6 (1,49×) | 49,1 (1,41×) | 16,9 / 7,1 | 19,0 / 7,6 |
| horizonreus | 61,0 (1,60×) | 50,9 (1,35×) | 29,7 / 7,1 | 19,3 / 7,2 |
| caldera | 54,3 (1,52×) | 46,4 (1,30×) | 23,9 / 6,5 | 15,6 / 6,7 |
| basaltstromen | 57,0 (1,54×) | 47,2 (1,17×) | 20,9 / 7,6 | 13,9 / 7,7 |
| Nether-2 | 8,0 (1,58×) | 7,2 (1,55×) | 9,5 / 12,0 | 9,7 / 12,1 |
| Nether-3 oost | 7,2 (1,62×) | 6,3 (1,47×) | 9,3 / 7,6 | 9,4 / 7,5 |

CPU per stap, som over 256 chunks (s), mod bf63d17 → 39e (vanilla):

| Gebied | biomes | noise | surface |
|---|---|---|---|
| ring 1 | 6,3 → 3,5 (1,8–2,9) | 16,8 → 11,4 | 5,5 → 3,1 (2,0) |
| ring 2 | 5,1 → 4,9 (1,5) | 14,6 → 12,9 | 4,5 → 5,0 (1,9) |
| horizonreus | 5,3 → 4,5 (1,7) | 16,0 → 14,3 | 8,0 → 5,2 (2,0) |
| caldera | 5,4 → 4,4 (1,6–2,0) | 14,0 → 15,2 | 6,5 → 4,5 (1,8) |
| basaltstromen | 4,9 → 4,2 (1,8–2,0) | 15,0 → 13,1 | 5,7 → 3,8 (2,0–2,1) |

Eén ronde per kant: de spreiding tussen rondes is ±20–30 %; de officiële meting (≥ 3 rondes per kant, alle 19 gebieden, plus koude rondes) volgt in één blok. Ring 2 wint nog niets: daar zijn de gesteentetakken en flat caches echt nodig.

### 3b. Koude ronde baseline (bf63d17, geen warm-up, alle 19 gebieden)
Wandtijd overworld 24–38 s per gebied (vanilla 8–11 s), Nether 7–9 s (vanilla 8–11 s). Geen stilstand; zie §1 voor de deopts.

### 3c. Officiële meting
Nog te doen (morgen, in één blok: ≥ 3 warme rondes vóór én na over alle 19 gebieden, plus koude rondes). De snelle meting van §3a is van de eerste versie (puntgate op flat caches); de `RingFillGate` spaart buiten de ringranden hetzelfde werk uit.

## 4. Determinisme
Twee verse werelden (seed 20260930) uit de snelle meting, bf63d17 (`c39e-base1`) tegen 39e (`c39e-wip1`), blok voor blok (`build/c39e/py/compare.py`, de methode van 39c/39d):
- ring 3 horizonreus, caldera, basaltstromen: 768/768 chunks identiek; Nether-3 oost 256/256 identiek.
(Deze vergelijking is van de eerste versie met de puntgate; die was in de binnengebieden identiek, maar niet bij de ringranden: zie "Review 1". De versie met `RingFillGate` is opnieuw vergeleken, ook bij de randen: zie §4b.)
- ring 1: 152/256, ring 2: 212/256, Nether-2: 232/256 identiek; de verschillen zijn alleen vanilla-features (bomen, druipsteen, sculk, erts-klodders, magma/blackstone-klodders in de Nether). Ter controle twee baseline-werelden onderling (`c39e-base-cold` tegen `c39e-base1`): ring 1 168/256, ring 2 211/256, Nether-2 241/256 met precies dezelfde soorten verschillen. Dus laadvolgorde-ruis, geen gedragswijziging.

### 4b. Ringranden, eindversie (na review 1)
Verse werelden `c39e-det-base` (bf63d17) en `c39e-det-new` (39e met `RingFillGate`), 16 × 16 chunks over elke grens:
- overworld 1→2 (x 1.344–1.599): 226/256 identiek; 2→3 (x 3.392–3.647, z = 0): 249/256; 2→3 (z 3.392–3.647, x = 0): 241/256; 3→4 (x 5.888–6.143): 256/256.
- Nether 1→2 (x 320–575): 237/256; 2→3 (x 800–1.055): 256/256; 3→4 (x 1.440–1.695): 256/256.
- Alle verschillen zijn vanilla-features met laadvolgorde-ruis (erts- en steenklodders, `disk_sand`/zeegras, karmozijnen fungi en wijnranken in Nether-1); geen terreinblok (steen/lucht/vloeistof-grens, surface-blok) wijkt af.
- De server gaf geen fouten in het log tijdens het genereren van alle ringen en grenzen.

## 5. Aanbevelingen voor 39d (bestanden van 39d)
- `VolcanicField.Column`: een publieke `lavaBottom()` naast `lavaTop()`. Dan kan `RingAquifer` de hele lavarange per kolom in zijn tabel zetten in plaats van per blok `volcanic.column(x, z).lavaAt(y)` op te vragen (nu alleen binnen lavakolommen, onder de lavatop).
- `ring3_surface.py`: zet in elke ring 3-tak de goedkope condities (`stone_depth`, `y_above`, `biome`) vóór `noise_threshold` en `volcanic_threshold`, en een gedeelde tak (`shared_volcanic`/`shared_ground`) liever binnen de biome-condities dan ervóór, zodat ring 1/2-blokken boven het preliminary surface er niet doorheen hoeven. De ring 3-surface blijft na 39e 2–2,7× vanilla (horizonreus 19,3 tegen 7,2 ms/chunk).
- `RingAquifer` (39d wijzigt hem ook: lavabron met update, geen korst tegen lava- of eilandkolommen): bij de merge komt de bron-check in de lava-tak van `openSpace` (zelfde plek als nu `ring3Lava`), `spring` wordt overal waar `suppressed`/`lid` worden gezet ook gereset; `exposed`/`open` blijven van 39d.
- Nieuwe flat caches in `ring_3/overworld/*` worden automatisch gegate; zet geen `volcanic_field` direct onder een `flat_cache`/`interpolated` (de `resolve`-methodes kijken niet door een `RingGate`).

## Review 1 (`/code-review medium`, 6 bevindingen)
1. **Puntgate op flat caches niet gedrag-neutraal** (belangrijk): `CanyonDetails` (hoogte bergop, ver buiten de chunk), `RockPillars.onGrid` (gewichten en vloer op rasterpunten tot een cel verderop) en `DewGeodes` lezen flat-gecachete invoer op andere kolommen; met een `RingGate` daar zou die buiten het bereik 0 geven en het terrein bij de ringranden veranderen. Opgelost met `RingFillGate` (alleen tijdens het vullen, alles-of-niets per chunk; lezen buiten de cache gaat altijd naar het argument). Determinisme bij de randen 1→2, 2→3, 3→4 (overworld en Nether): zie §4.
2. `RingFluidPicker`: de per-blok-predicaten (`waterOnly`, `ring3Lava`, `ring3Dry`, `ring3Seal`) verwijderd; de per-kolom-grenzen zijn de enige regel (Javadoc en tests daarop).
3. `RingAquifer`: de test-overload deelt de body (`compute(slot, …)`, slot −1 = pas opzoeken als een regel hem nodig heeft).
4. `RingBlend.fillArray`: één ringlookup per kolom (de slice loopt eerst langs Y).
5. `RingBlend`: `switches` wordt gevuld uit `this.switchAt` (de set blijft voor de codec).
6. `RingReachCondition`: Javadoc met één onderbouwd getal (≤ 5 blokken per as, ~7 diagonaal, `BiomeManager`-zoom).

## 6. W-checklist (worldgen-smoke)
- [x] `./gradlew build` (unit tests) groen, 21:15 (eindversie na review 1).
- [x] `runData`: niet nodig; geen datagen-provider geraakt (`src/generated` ongewijzigd).
- [x] runServer: start zonder fouten, genereert ring 1–4 en Nether 1–4 over alle grenzen (§4b).
- [ ] client: `/ca tpring 1|2|3`, `tpring 3 nether`, portal ring 3 ↔ Nether-3 — niet gedaan (geen client-slot vanavond; geen client-code geraakt). Bij de officiële meting meenemen.
- [x] Determinisme binnengebieden (§4) en ringranden (§4b).

## CODEBASE-delta
Direct bijgewerkt in `CODEBASE.md` (opdracht 39e): `RingBlend`, `RingGate`, `RingFluidPicker`, `RingAquifer`, `RingReachCondition`, `ModSurfaceConditions`, registry-regel `ring_reach`, valkuil "Waar de wachttijd bij het laden zit".

## Beslissingen
Geen ontwerpbeslissingen (gedrag-neutraal).

## Backlog
Bijgewerkt: "JIT-deoptimalisatiestorm", "Surface-stap ring 3", "Overhead van het ringraamwerk".

Voortgang: 20:45–21:10 review 1 verwerkt; 21:15 build groen; 21:16–21:22 determinisme ringranden; commit.
Voortgang: 19:05 start, baseline-build; 19:20–19:35 koude baseline-ronde; 19:40–20:00 profiel (JFR-consumer); 20:00–20:08 fixes 1–4 + tests; 20:10–20:31 snelle vóór/na; 20:35 determinisme en docs.
