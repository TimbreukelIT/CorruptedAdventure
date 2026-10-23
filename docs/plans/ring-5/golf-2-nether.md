# Ring 5 golf 2 — stroom W-nether: 96 (aankleding Nether-5)

Scope: `worldgen` — pakket 96 in branch `ring5/96`, worktree `P:/Projecten/Java/wt/r5-96`, poort n = 3 (server 25573,
RCON 25583), basis = main `1a31dbd6`. Parallel: 83b (binnenband ring 5), 97 (C, stilte), 98 (A, sfeer); 96 raakt hun
bestanden niet.

Bronnen: `fase-1-wereld.md` → "Nether-tegenpool — Nether-ring 5", regel **Aankleding (96, besluit 2026-10-23)** en V10-A;
`waves.md` §Golf 2; `golf-1-nether.md` (87/87b: `NetherCathedrals`, zuilraster 4 × 4 om de 24 met een offset per cel van
400, `NetherRing5PillarCondition`, `nether_ring5_surface.py`); blokken van 90 (`golf-1-blocks.md`: `still_light` licht 15,
`white_core_ore` licht 3, op `white_stone`).

**Bezit** (W-nether): `density_function/ring_5/nether/**` (via `nether_ring5_terrain.py`), de Nether-5-tak van
`noise_settings/nether.json` (via `nether_ring5_surface.py`), `NetherCathedrals`, eigen surface-condities (blok
`// ring 5 — W-nether` in `ModSurfaceConditions`), de Nether-5-tests.

**Besluit gebruiker (96):** stil licht als lichtband **in de zuilen** (vaste hoogte per zuil); witte kern in **~1 op 10
zuilen**, een kern van 1–3 erts halverwege de zuil; de reuzenzuil van `hollow_cathedral` krijgt een **verticale
witte-kernader in het hart** en een **vloer-afdruk** van gepolijst/bakstenen witsteen rond de voet (leest als bouwwerk).

---

## 1. Route (implementatie)
Alles als **surface rule** in de bestaande Nether-5-tak, niet als feature of DF:
- de zuilen en de vloer zijn al surface-blokken (87b); licht, erts en afdruk zijn alleen een ander blok op een plek die
  puur uit de vorm volgt (zuilhoek, kathedraalmidden, Y);
- per blok puur en chunk-onafhankelijk (de zuilen liggen over chunkgrenzen; een feature zou per chunk zijn deel moeten
  schrijven), geen trekking uit een decoratie-random, geen tweede pass over de chunk;
- de terrein-DF blijft byte-gelijk in zijn dichtheid: `noise` verandert niet.
`NetherRing5Features` en `NetherRing5BiomeModifiers` blijven dus **leeg** (geen features in Nether-5).

Onderdelen:
1. **Instellingen** `dressing` als derde groep in de `settings` van `nether_cathedrals` (beide uitgangen, uit één
   dictionary in `nether_ring5_terrain.py`), gevalideerd in `NetherCathedrals.Settings` samen met `halls`/`cathedrals`
   (band boven de hoogste vlakte en onder de kern, kern binnen de zuil, ader binnen de reuzenzuil, afdruk binnen de hal).
2. **Pure kolomvraag** `NetherCathedrals.dressing(x, z)` → `NetherRing5Dressing.Column` (lichtband Y-bereik, erts
   Y-bereik, afdruksoort van het vloerblok). Klasse `NetherRing5Dressing` (instellingen + rekenwerk), per zuil een
   geseede hash `SeededHash.hash(cells, px, pz, PILLAR_SALT)` op de lage hoek van de zuil.
3. **Surface-conditie** `corruptedadventure:nether_ring_5_dressing` (veld `part`: `light`, `ore`, `imprint_polished`,
   `imprint_bricks`), onthoudt zijn laatste kolom; leest de router-DF via `NetherRing5Terrain` (zoals de zuilconditie).
4. **Surface-tak** (`nether_ring5_surface.py`): in de vloerregel (Y ≤ 31, bovenblok) eerst `imprint_bricks` →
   `white_stone_bricks`, `imprint_polished` → `polished_white_stone`, dan spiegelsteen; in de zuilregel eerst `light` →
   `still_light`, `ore` → `white_core_ore`, dan `white_stone_pillar`.

## 2. Getallen (afstemming, van mij; bijsturen in de afstemlus)
- **Lichtband:** 2 blokken hoog rond de zuil (de buitenste ring van 12 kolommen; na de review niet meer in de 4
  binnenkolommen, waar je het nooit ziet); onderkant per zuil uit de hash op een van de
  niveaus Y **38, 44, 50, 56** (`light_y` [38, 56], `light_step` 6, `light_height` 2). Boven de hoogste vlakte (37), ver
  onder de kern. Alleen rasterzuilen (de reuzenzuil: zie §6).
- **Witte kern:** kans **0,1** per rasterzuil (`core_chance`), **1–3** erts (`core_size`, gelijk verdeeld) boven elkaar
  in één van de 4 binnenkolommen (de binnenste 2 × 2; van buiten onzichtbaar), gecentreerd op het midden van de zuil
  tussen vloer en plafond: (32 + 103) / 2 = **Y 67** (1: 67; 2: 67–68; 3: 66–68).
- **Ader in de reuzenzuil:** de middenkolom van de reuzenzuil (`vein_radius` **0**, één kolom recht omhoog), van Y 32
  (boven de vloer) tot Y 119 (onder het dak): 88 erts per kathedraal, 10–15 blokken diep in de zuil.
- **Vloer-afdruk:** een ring van **8** breed rond de voet (`imprint_width`), alleen het vloerblok (Y 31): buitenste en
  binnenste **1** blok (`imprint_border`) `white_stone_bricks`, daartussen `polished_white_stone`; afstand t = d − r_zuil
  uit het (gehele) kathedraalmidden, t ∈ (0, 1] en (7, 8] bakstenen, (1, 7] gepolijst.

## 3. Client/server-impact
Geen client-code; worldgen (server-side), één nieuwe surface-conditie (common, puur), DF-settings in JSON. Geen
payload, geen `PROTOCOL_VERSION`. Geen datagen-wijziging verwacht (`src/generated` gelijk).

## 4. Determinisme
Alles uit de geseede celhash-noise (`nether_ring_5_cathedral_cells`), de gedeelde cellen en de positie; geen
decoratie-random, geen volgorde-afhankelijkheid. `det.sh` op een klein Nether-5-gebied (eigen areas-bestand, niet
gecommit als het alleen voor 96 is: `build/test/areas-96.txt`).

## 5. Tests en poorten
- Unit (`NetherRing5DressingTest`): band per zuil op een van de niveaus, 2 hoog, boven 37 en onder de kern, alle 16
  kolommen van de zuil; aandeel zuilen met kern 0,07–0,13 over de ring; kern 1–3, binnenkolom, rond Y 67; ader alleen de
  middenkolom van elke reuzenzuil, Y 32–119; afdruk alleen rond de reuzenzuil, binnen de hal, bakstenen-ringen binnen en
  buiten; niets buiten zuilen/vloer; puur per seed (twee keer dezelfde, andere seed andere banden); instellingen weigeren
  een band in een vlakte of op de kern.
- `NetherRing5JsonTest.surfaceBranchPaintsStreamBsBlocks` bijgewerkt (nieuwe volgorde/blokken, `still_light`,
  `white_core_ore`).
- **B** `./gradlew build --offline` groen; **D** `runData --offline` groen (verwacht: geen wijziging in `src/generated`).
- **S-smoke** `server.sh start r5-96 --fresh`, log 0 ERROR; `/execute in minecraft:the_nether run ca terrainmap 2200 0
  400 2`; blokscan echte chunks (hallen + een kathedraal): `still_light` per zuil op één niveau, kernen per zuil, ader en
  afdruk bij de reuzenzuil; stop.
- **det.sh** op het eigen gebied: 0 verschillen.
- **perf.sh** `--areas tools/test/areas/perf-r5.txt --only "halls cathedral" --cold-only "cathedral"`: noise ≤ 1,75.
- `/code-review medium` op `main...ring5/96`.

## 6. Vragen tijdens de bouw (beantwoord)
- **Besluit gebruiker 2026-10-23:** "vaste hoogte per zuil" = **elke zuil zijn eigen bandhoogte** uit Y 38/44/50/56,
  2 hoog (A; niet één hoogte voor het hele raster).
- **Besluit gebruiker 2026-10-23:** de reuzenzuil krijgt **geen lichtband**, alleen de ader en de vloer-afdruk.

## Voortgang
Voortgang: 2026-10-23 worktree `P:/Projecten/Java/wt/r5-96` op `ring5/96` vanaf `1a31dbd6`; `setup.sh W3 3`; plan
Voortgang: 2026-10-23 15:05 `NetherRing5Dressing`, `nether_ring_5_dressing`, scripts, tests; gerichte tests groen (vraag band/reuzenzuil aan main: antwoord §6)
Voortgang: 2026-10-23 15:25 B groen, D groen; S-smoke `r5-96` (0 ERROR), kaart, blokscan 4 gebieden; det-96 0
Voortgang: 2026-10-23 15:40 review (medium, 6 punten) verwerkt; B groen; S-smoke + blokscan opnieuw; det-96 0; perf binnen budget

## Tests en metingen
- **Unit** (`NetherRing5DressingTest`, seed 20260930, heel Nether-5): 5.679 rasterzuilen; bandniveaus 38/44/50/56 =
  1.424 / 1.435 / 1.392 / 1.428; kernen 550 (9,7 %), groottes 1, 2 en 3; elke reuzenzuil ader Y 32–119 alleen in de
  middenkolom, afdruk 8 breed (bakstenen t ≤ 1 en t > 7). `NetherRing5JsonTest.surfaceBranchPaintsStreamBsBlocks`
  bijgewerkt (vloer: afdruk vóór spiegelsteen; zuil: licht, erts, dan zuilblok).
- **B** `./gradlew build --offline` groen: 1.292 tests, 0 fouten, 1 overgeslagen (incl. `ClientIsolationTest`).
  **D** `runData --offline` groen, geen wijziging in `src/generated` (alleen handgeschreven JSON).
- **S-smoke** (verse wereld `r5-96`, seed 20260930, poort 3): log 0 ERROR, geen client-class-fout; `/execute in
  minecraft:the_nether run ca terrainmap 2200 0 400 2` (1,0 s; vorm ongewijzigd, de kaart toont geen surface) in
  `build/test/maps/96/`.
- **Blokscan** echte chunks (`gen.py` + eigen scan op `region.py`, niet gecommit): gebieden `hall96` (chunks 129..136 ×
  −8..−1), `cath96` (137..144 × 10..17, kathedraal (2248, 212)), `hall96b` (122..136 × −45..−31), `hall96c` (−7..7 ×
  130..144, kathedraal (−110, 2174)):
  - 158 volledige rasterzuilen: elke zuil precies één band van 2 `still_light` rond de buitenring (24 per zuil, na de
    review; 0 in de binnenkolommen), niveaus 38/44/50/56 = 37 / 40 / 42 / 39; 0 zuilen met gemengde banden.
  - Kernen 15 van 158 (9,5 %), groottes 1: 5, 2: 5, 3: 5; erts alleen Y 66–68, alleen binnenkolommen (30 binnen, 0 buiten),
    nooit over meer dan één kolom.
  - Reuzenzuilen (2248, 212) r 10 en (−110, 2174): ader precies de middenkolom, Y 32–119, 88 `white_core_ore`; 0 licht
    in de reuzenzuil. Afdruk rond (2248, 212) op Y 31: t ≤ 1 60 bakstenen, t 2–7 524 gepolijst, t ≤ 8 108 bakstenen,
    daarbuiten spiegelsteen; rond (−110, 2174) (deels in het gebied) hetzelfde patroon (53 / 362 / 77).
  - De metingen zijn herhaald op een verse wereld na de reviewfixes (zelfde getallen, behalve de band: 24 i.p.v. 32 per
    zuil).
- **det-96** (`build/test/areas-96.txt`, hall96 + cath96): 128/128 chunks gelijk, own 0, other 0, noise 0.
- **B na de review** groen: 1.292 tests, 0 fouten, 1 overgeslagen.
- **det-96 na de review** (16:02): opnieuw 128/128 gelijk, own 0.
- **perf** (`perf.sh --areas tools/test/areas/perf-r5.txt --only "halls cathedral" --cold-only "cathedral"`, 3 warm + 1
  koud, mod/vanilla, op de code na de review; `build/test/perf-20261023-1531/table.md`): `halls` noise **1,10** (1,05–1,17),
  surface 0,74, features 0,05; `cathedral` noise **1,10** (1,07–1,13), surface 0,85, features 0,18; koud noise 1,04,
  surface 0,86, features 0,22. Budget noise ≤ 1,75: ruim binnen. Surface blijft onder vanilla (< 1) met de aankleding.

## CODEBASE-delta
- **Klassen:**
  - `worldgen/densityfunction/NetherRing5Dressing` ✅ 96: `Settings` (`settings.dressing` van `nether_cathedrals`,
    gevalideerd tegen `halls`/`cathedrals`), `Part`, `Column` (`has(part, y)`), `of(field, x, z)` (puur: per rasterzuil
    een hash van de lage hoek met `PILLAR_SALT` 1000 op `cells`; kathedraal: ader en afdruk uit de vorm).
  - `NetherCathedrals`: `Settings` met derde groep `dressing`; `gridPillarCorner(x, z)` (`NO_PILLAR`), `pillarHash`,
    `dressing(x, z)`.
  - `worldgen/surface/NetherRing5DressingCondition` ✅ 96: `corruptedadventure:nether_ring_5_dressing` (`part`);
    `ModSurfaceConditions` blok `// ring 5 — W-nether`.
- **Data:** `ring_5/nether/{region,terrain}.json` met `settings.dressing`; `noise_settings/nether.json` Nether-5-tak:
  vloerbovenblok eerst `imprint_bricks` → `white_stone_bricks`, `imprint_polished` → `polished_white_stone`; zuilregel
  eerst `light` → `still_light`, `ore` → `white_core_ore`. Scripts `nether_ring5_terrain.py` (`dressing`),
  `nether_ring5_surface.py` (de regels).
- **Tests:** `NetherRing5DressingTest` (nieuw), `NetherRing5JsonTest.surfaceBranchPaintsStreamBsBlocks`.
- `NetherRing5Features`/`NetherRing5BiomeModifiers` blijven leeg.
- **Na de review:** `gridPillarCorner(cell, x, z)` package-private en de enige plek die beslist waar een rasterzuil
  staat (`gridPillar` van het terrein vraagt hem ook); `NetherRing5Dressing.of` haalt de cel één keer; de vier delen van
  de conditie delen per thread de kolom (`ThreadLocal` `Last`); de band alleen in de buitenring.

## Beslissingen
- **Gebruiker 2026-10-23:** elke zuil een eigen bandhoogte (Y 38/44/50/56, 2 hoog); de reuzenzuil geen lichtband (§6).
- (implementatie) Route surface rule (§1), niet feature of DF: per blok puur, geen chunkgrensprobleem voor zuilen die
  over chunks liggen, de dichtheid (noise) blijft gelijk. De getallen in `settings.dressing` van de terrein-JSON (één
  plek met de vorm, gevalideerd samen met `halls`), niet in de surface-JSON.
- (implementatie, afstemming) Getallen §2. De kern zit in een binnenkolom (onzichtbaar, je moet de zuil openhakken); de
  ader is één kolom recht omhoog (88 erts per kathedraal). De band loopt door de hele doorsnede van de zuil.
- (implementatie) De afdruk vervangt alleen het vloerbovenblok (Y 31); de afstand komt uit het gehele kathedraalmidden,
  dezelfde maat als de reuzenzuil zelf (`d ≤ r` is zuil), dus de afdruk sluit naadloos op de voet aan.

## Review (medium, 6 punten)
- Opgelost: (1) vier condities rekenden dezelfde kolom elk opnieuw uit → één gedeelde kolom per thread; (2) cel twee keer
  opgezocht → één keer; (3) zuilraster dubbel (terrein en aankleding) → één methode; (5) publieke gepakte long met
  schildwacht → package-private, cel-variant, schildwacht gedocumenteerd (geen hoek op x = `Integer.MIN_VALUE` in
  Nether-5); (6) onzichtbare lichtblokken in de zuilkern → band alleen in de buitenring.
- Niet overgenomen: (4) `dressing` verplicht in beide uitgangen (`region` leest het niet). Beide JSON's komen uit één
  dictionary van het script en `maskAndTerrainShareTheirSettings` bewaakt dat ze gelijk zijn; dat geldt al voor `halls`
  en `cathedrals`. Optioneel maken zou de mask-JSON laten afwijken van de terrein-JSON en die test breken.

## Backlog
- `items` (fase 2): witte kern per wereld (unit, seed 20260930): ~1.100 erts uit de rasterkernen (550 × gemiddeld 2) en
  ~2.700 uit de aders (~31 kathedralen × 88). De aders leveren dus het meeste; bij het ontwerp van de zuiveringsgear en
  de kasteeltoegang de hoeveelheid ijken (`vein_radius`, `core_chance` in het script).
- `worldgen` (6b, gebruiker): de kernen zijn van buiten niet te zien (licht 3 gaat niet door de zuil heen). Als de
  speler een aanwijzing wil (bijv. een andere band of een kenmerk aan de buitenkant), is dat een nieuw ontwerppunt.
