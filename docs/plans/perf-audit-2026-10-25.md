# Prestatiemeting 2026-10-25 (onderbroken) en plan voor 2026-10-26

Gemeten op main 9d49e232, fase 2 golf 1. Er is 1 warme pass gedraaid met `perf.sh` op 31 gebieden; 20 daarvan zijn
gemeten. De run is gestopt omdat ring 5 bleef hangen. Worktree: `P:/Projecten/Java/wt/perf-audit`, met de JFR-dumps in
`build/test/perf-all-20261025/`.

## Uitkomst: noise-stap mod/vanilla (budget ≤ 1,75×)
| Gebied | Noise | Gebied | Noise |
|---|---|---|---|
| ring1 | 0,94 | sulfur | 1,08 |
| z12 | 1,46 | great | 1,11 |
| canyon | 1,43 | ashen | 0,99 |
| hoodoo | 1,04 | titan | 1,12 |
| dunes | 1,22 | seabed | 1,22 |
| mesa | 1,08 | steamfens | 1,30 |
| zone23 | 1,50 | vfbasin | 1,12 |
| ember | 1,00 | vgorge | 0,92 |
| tds | 1,04 | tunmyth | 0,90 |
| tree | 1,05 | **zone45** | **692×** (15,8 s/chunk tegen 23 ms) |

- Ring 1–4 en de zones 1→2 en 2→3 blijven binnen het budget, ook na de G2-opknapbeurt van ring 2 en 3.
- De surface-stap ligt op 1,5–5,2× (canyon 5,2, mesa 5,0, zone23 4,4). Daar geldt geen budget, maar het is wel een
  kandidaat voor optimalisatie.
- **Niet gemeten:** wastes, glitch, meadow en alle Nether-gebieden (n1–n4, halls, cathedral).

## Het ring 5-probleem: hangt af van de wereldgeschiedenis
- Zone45 **los** op een verse wereld (HEAD, 16×16, 1 warm) geeft noise **1,43×**, dus normaal. Een los stuk van 6×6
  chunks duurt 9,4 s.
- De 692× ontstaat pas **nadat dezelfde server eerst 19 andere gebieden** (ring 1–4) heeft gegenereerd. Daarna zijn
  vrijwel alle chunks van zone45 én wastes traag (50–78 s per chunk). De server liep vast in de forceload en moest met
  taskkill gestopt worden.
- Tijdens de hang zaten alle 23 `wgen_fill_noise`-threads in `Ring5Field.terrain`/`column` (de gelaagde tak van de
  glitch-velden), via `Ring5Output.compute` ← `RingBlend.fillArray` ← `ZoneFracture.fillArray`. Die jstack-dumps zijn
  gemaakt zonder DebugNonSafepoints, dus het beeld is grof.
- Uitgesloten: 373787a7 (`Column.pit` in fillInner) is op zich niet de oorzaak, en de vensterverschuiving in
  ColumnTable ook niet (een laagverschuiving van ≤ 6 valt binnen de marge van 8).
- Hypotheses:
  - Een cache of lijst die per wereld onbegrensd groeit, waardoor heap en GC druk krijgen. Kandidaten van deze dagen:
    VolcanicLandmarks (ring 3), Ring2RockLandmarks en Ring2FlatLandmarks PLANS, Ring5PlanCache en Ring5Memories,
    KeyedSlots-contentie.
  - Een wisselwerking tussen ring 4 en ring 5 (gedeelde state in RingBlend/ZoneFracture).
- **Het risico is groot:** elke wereld die lang gespeeld wordt, kan in ring 5 vastlopen.

## Diagnose 2026-10-26: een JIT-deoptimalisatiestorm, geen cache
- De opname van de hangende run (`p1-partial.jfr`, 1,6 GB) bestond voor 1,5 GB uit `jdk.Deoptimization`: **56,4 miljoen**
  keer `unstable_if` met action `none`, allemaal in de **eerste** C2-compilatie van `Ring5Field.terrain` (regels 363, 368,
  373 en 376: de spans, de eigen grotten, de void en de glitchlagen). Daarnaast 23.041 in `Ring5Field.caveZone` regel
  1007, en kleinere aantallen in `Ring4Band.blighted`, `IslandDressingFeature.place` en vanilla's `ChunkSkyLightSources`.
- Mechanisme (JDK 21u `Compile::too_many_recompiles`): als de decompile-tellers van de wortelmethode en alle geïnlinede
  methodes samen ≥ 201 zijn, krijgt elke nieuwe reinterpret-trap action `none`. Elke doorgang valt dan terug naar de
  interpreter, zonder hercompilatie. Gedeelde hulpmethoden (`ColumnTable.get`, `ThreadOwned.get`, `Math.floorDiv`, ...)
  verzamelen decompiles in de loop van een sessie. Code die pas laat voor het eerst compileert, is daardoor vergiftigd.
  Dat verklaart waarom de hang van de geschiedenis afhangt.
- Repro van 2026-10-26 (19 gebieden, daarna zone45 en wastes, 7 worldgen-threads, `build/test/r5repro/`): **geen storm**
  in `terrain` (opgeteld 50 decompiles, onder de grens, normaal hercompileerd; zone45 22 s, wastes 19,7 s). Wel weer
  23.041 action-`none`-deopts in `caveZone`. De storm hangt dus van de timing af (gisteren 23 threads).
- Stresstest `-XX:PerMethodRecompilationCutoff=20` (grens 11) op de oude build: ook de warm-up in ring 1–3 stormt al
  (81 chunks in 5 min; alle noise-threads in `RingFluidPicker.computeFluid`). Het risico zit dus in elke ring.
- Besluit (gebruiker): een warm-up van alle ringtypes bij serverstart, per-blok-paden van ring 5 zonder
  regio-afhankelijke takken en een storm-detector in de perf-tests (`tools/test/py/deopt.py`, `2b61b386`). Zie
  `docs/design/01-techniek.md` "JIT-robuustheid van worldgen".

**A/B warm-up (2026-10-26, `opt/warmup` 6639c41b, standaardvlaggen, 23 gebieden: ring 1–4, dan zone45, wastes, glitch en
meadow; beide armen tegelijk, `jfr-prof`-opnames per gebied):**
- **Zonder warm-up:** een storm in alle drie de ring 5-biomes (`Ring5Field.terrain` r. 369): wastes 144.374, glitch
  201.498, meadow 225.381 deopts met action `none`.
- **Met warm-up:** geen storm in de echte generatie (wastes 948, glitch 963, meadow 861; de rest is vanilla
  `ChunkSkyLightSources`). Ring 5 is 1–3 s per gebied sneller.
- **Wel:** de warm-up-thread zelf stormde kort tijdens het eerste gebied (162.368 × `caveZone` r. 1016). De JVM was al
  ~20 s na de start vergiftigd, op een zwaar belaste machine (warm-up 25,1 s). De ring 5-hotpaths (`opt/r5hot`) zijn
  daarom de tweede linie.
- Onder de stressvlag `PerMethodRecompilationCutoff=100` is de JVM al bij de start vergiftigd: dat test de warm-up
  niet eerlijk.

**Doel (gebruiker, 2026-10-26):** noise ≤ 1,5× per gebied (mediaan van ≥ 5 warme rondes, rustige machine); 1,75× blijft de
harde grens. Op het randje volgens de meting van 2026-10-25: zone23 1,50, z12 1,46, canyon 1,43, zone45 1,43–1,64; nog
niet gemeten: wastes, glitch, meadow en de Nether.

## Volgende optimalisatiedoelen (uit de profielen van 2026-10-26, `build/test/r5repro/*.txt` in `wt/perf-audit`)
- **Surface ring 2/3 (3–5×):** 99,9 % zit in vanilla `SurfaceRules` (`LazyCondition.test` 11–13 %,
  `BiomeManager.getFiddledDistance`/`getBiome` 15–17 %, de LCG-hash 5–6 %); de eigen condities zijn 1–4 %. De
  regelboom (6.709 knopen, 96 biome-condities) test waarschijnlijk biomes per steenblok vóór de goedkope diepte- en
  y-checks.
- **Aquifer:** `RingAquifer.openSpace` is 26 % van de noise-stap in canyon (via `CanyonDetails.compute`/`RockPillars`).
- **Per chunk `mapAll`:** het kopiëren van de router (`RingBlend`/`RingFillGate`/`ZoneFracture`/`CanyonDetails.mapAll`)
  is ~6–7 % van alle samples.
- **`RingFluidPicker.computeFluid`:** stormgevoelig onder de stresstest, nog geen tabel per kolom.

## Plan voor 2026-10-26: eerst optimalisatie (op verzoek van de gebruiker)
1. **Reproduceren met profiler:** jfr-prof met DebugNonSafepoints op een server die eerst de 19 ring 1–4-gebieden
   genereert en daarna zone45 (6×6). Bekijk jdk.GarbageCollection/GCHeapSummary en `jcmd GC.class_histogram` na een
   paar gebieden.
2. **Repareren:** de oorzaak begrenzen of oplossen, bijvoorbeeld met een LRU zoals `Landmarks.WINNERS`. Daarna een
   test die een lange generatiereeks nabootst (geen groei van de heap, geen vertraging).
3. **Volledige meting opnieuw:** alle 31 gebieden plus de Nether, 2 warm + 1 koud, op een rustige machine.
4. **Optimalisatieronde over alles:**
   - de surface-stap van ring 2 en 3 (3–5×);
   - de duurste noise-gebieden (zone23 1,50, z12 1,46, canyon 1,43);
   - de features;
   - een check op caches die per wereld onbegrensd groeien.
5. Daarna gaat fase 2 verder met golf 2 (ring 2), in een nieuwe sessie met Blockbench.

## Meting 2026-10-27: warm-up en r5hot op main (188bc162)

`perf.sh --areas tools/test/areas/perf-all.txt --warm 5 --cold 1` (`build/test/perf-all-20261027/`, standaardvlaggen,
zonder de zuinige modus). De machine was **niet de hele tijd rustig**: een Leap-CI (pnpm/turbo, ~22 vitest-workers) liep
in pieken van 1–4 min (10:43, 11:17, 11:28, 11:32, 11:40, 11:41, 11:58), met geheugendruk tot 2 GB vrij.
- Ronde 2 en de extra ronde (`perf-all-20261027-x1`) zijn verloren: `jfr print` crashte onder de geheugendruk
  (0xC0000142), dus er is geen JSON.
- Per gebied tellen alleen de schone metingen. Een meting telt niet mee als de belasting in haar venster viel (log per
  15 s, tijd per gebiedsregel), als er na het einde van de log geen belastingdata meer was, of als de vanilla-tijd boven
  1,5× de mediaan van het gebied lag. Daardoor vielen er 12 metingen af.
- Dat geeft **3–4 schone warme rondes per gebied** (rondes 1, 3, 4 en 5), niet de beoogde ≥ 5.
- Gemaakt met `C:/Temp/ca-wt-scripts/clean_perf.py` → `table-clean.md`. De ruwe tabel `table.md` van perfsum, zonder
  filter, verschilt hooguit 0,04.

| Gebied | noise mediaan schoon | n | min-max | alle warm | surface | features | noise koud | noise 25-10 |
|---|---|---|---|---|---|---|---|---|
| ring1 | 0,93 | 4 | 0,80-1,16 | 0,93 | 1,52 | 0,85 | - | 0,94 |
| z12 | 1,37 | 4 | 1,34-1,52 | 1,37 | 2,92 | 0,94 | - | 1,46 |
| canyon | 1,40 | 4 | 1,34-1,45 | 1,40 | 5,33 | 0,85 | - | 1,43 |
| hoodoo | 1,08 | 4 | 1,03-1,40 | 1,08 | 3,90 | 0,83 | 1,04 | 1,04 |
| dunes | 1,13 | 4 | 1,04-1,16 | 1,13 | 2,19 | 0,81 | - | 1,22 |
| mesa | 1,25 | 3 | 1,21-1,26 | 1,23 | 5,16 | 0,91 | - | 1,08 |
| zone23 | 1,39 | 3 | 1,38-1,40 | 1,38 | 4,41 | 0,69 | 1,36 | 1,50 |
| ember | 1,02 | 4 | 1,01-1,06 | 1,02 | 2,10 | 0,86 | 1,00 | 1,00 |
| sulfur | 1,05 | 4 | 1,03-1,25 | 1,05 | 2,67 | 0,41 | - | 1,08 |
| great | 1,17 | 3 | 1,06-1,26 | 1,21 | 2,80 | 0,44 | - | 1,11 |
| ashen | 1,03 | 3 | 1,02-1,03 | 1,02 | 2,28 | 1,12 | - | 0,99 |
| titan | 1,05 | 3 | 1,04-1,09 | 1,05 | 2,08 | 0,91 | - | 1,12 |
| seabed | 1,08 | 3 | 1,07-1,10 | 1,09 | 1,91 | 0,89 | - | 1,22 |
| steamfens | 1,32 | 4 | 1,21-1,36 | 1,32 | 2,79 | 0,46 | 1,41 | 1,30 |
| vfbasin | 1,04 | 4 | 0,97-1,13 | 1,04 | 3,09 | 0,47 | 0,96 | 1,12 |
| vgorge | 0,97 | 4 | 0,90-1,19 | 0,97 | 2,69 | 0,50 | - | 0,92 |
| tds | 0,94 | 4 | 0,93-0,99 | 0,94 | 2,07 | 1,02 | - | 1,04 |
| tunmyth | 0,81 | 4 | 0,74-0,89 | 0,81 | 1,61 | 0,80 | - | 0,90 |
| tree | 1,00 | 4 | 0,77-1,14 | 1,00 | 2,18 | 1,46 | - | 1,05 |
| zone45 | 1,48 | 4 | 1,26-1,58 | 1,48 | 3,27 | 0,17 | 1,46 | 691,78 |
| wastes | 1,18 | 4 | 1,13-1,24 | 1,18 | 3,75 | 0,21 | 1,18 | - |
| glitch | 1,02 | 3 | 0,99-1,04 | 1,00 | 3,18 | 0,12 | - | - |
| meadow | 1,01 | 3 | 0,94-1,12 | 0,99 | 3,16 | 0,15 | - | - |
| n1 | 1,12 | 4 | 1,00-1,19 | 1,12 | 1,18 | 0,84 | - | - |
| n2 | 1,39 | 3 | 1,31-1,41 | 1,40 | 0,76 | 0,09 | 1,41 | - |
| n3e | 1,23 | 3 | 1,22-1,23 | 1,23 | 1,50 | 0,51 | - | - |
| n3rifts | 1,04 | 3 | 0,98-1,30 | 1,12 | 1,01 | 0,27 | - | - |
| n4dunes | 1,11 | 3 | 1,07-1,17 | 1,11 | 0,59 | 0,10 | - | - |
| n4hollows | 1,36 | 4 | 1,06-1,63 | 1,36 | 1,36 | 0,14 | - | - |
| halls | 0,98 | 4 | 0,94-1,09 | 0,98 | 0,80 | 0,06 | - | - |
| cathedral | 1,03 | 4 | 0,96-1,07 | 1,03 | 0,90 | 0,17 | - | - |

Uitkomst:
- **Harde grens 1,75×: elk gebied haalt hem**, ook in elke losse ronde. De hoogste losse metingen zijn n4hollows 1,63
  (de Nether-tijden zijn klein, 4–6 ms per chunk, dus ze schommelen) en zone45 1,58.
- **Doel 1,5×: elke mediaan ligt eronder, maar voorlopig** (n = 3–4 in plaats van ≥ 5, geen volledig rustige machine).
  Het krapst zijn zone45 1,48 (1,26–1,58), canyon 1,40, zone23 1,39, n2 1,39, z12 1,37 en n4hollows 1,36.
- **De ring 5-hang is weg:** zone45 1,48 tegen 692× op 2026-10-25. In geen enkele ronde stormde ring 5.
  `Ring5Field.spotShift` r. 924 gaf 7–10 k traps met action `none`, onder de stormgrens.
- **Het stormrisico is niet weg:** in ronde 5 stormde ring 4. `VerdantGorges.shape` kreeg op r. 1033 71.855 traps
  (de boogtak), op r. 1056 40.430 (de afbrokkelende rand) en `Route.skipped` r. 444 11.980. De belasting viel niet in
  het meetvenster van vgorge. vgorge kwam op 1,19 tegen 0,90–0,98 in de andere rondes. Dat valt binnen het budget, maar
  de tweede verdedigingslinie (per-kolom-takken zonder regio-afhankelijkheid) is ook buiten ring 5 nodig.
- **De koude ronde** (8 gebieden) ligt op het niveau van de warme rondes. Met de warm-up aan is ze niet meer JIT-koud;
  vergelijk haar dus niet met de koude rondes van vóór 2026-10-27.
- **De surface-stap** blijft 1,5–5,3× (canyon 5,33, mesa 5,16, zone23 4,41). Daar geldt geen budget; `opt/surface` ligt
  klaar als kandidaat.
- **De Nether** is voor het eerst gemeten: 0,98–1,39.

Volgende stappen (ochtend; herzien in de bevestiging hieronder):
1. Een bevestigingsrun van 2 warme rondes op een echt rustige machine (~35 min), zodat n ≥ 5 per gebied. Gedaan, zie hieronder.
2. `VerdantGorges.shape` en `Ring5Field.spotShift` takvrij maken (backlog).
3. Marge op canyon, zone23 en zone45 met `opt/aquifer` (`RingAquifer.openSpace` is 26 % van canyon). Daarna
   `opt/surface`.

### Bevestiging 2026-10-27 middag: 2 warme rondes op een rustige machine (`perf-all-20261027-c`)

De machine was de hele run rustig: 0 node-processen en ≥ 15 GB vrij volgens de log per 15 s. Beide rondes stormden
weer in `VerdantGorges.shape`: r. 1033 kreeg 72–74 k traps en r. 1056 38–40 k. **De ring 4-storm zat dus in 3 van de 6
warme rondes**, en de belasting is er niet de oorzaak van.

Gecombineerd met de rondes van de ochtend geeft dat 5–6 schone warme rondes per gebied:

| Gebied | noise mediaan schoon | n | min-max | alle warm | surface | features | noise koud | noise 25-10 |
|---|---|---|---|---|---|---|---|---|
| ring1 | 1,01 | 6 | 0,80-1,19 | 1,01 | 1,53 | 0,85 | - | 0,94 |
| z12 | 1,44 | 6 | 1,34-1,52 | 1,44 | 2,90 | 0,94 | - | 1,46 |
| canyon | 1,45 | 6 | 1,34-1,56 | 1,45 | 5,33 | 0,85 | - | 1,43 |
| hoodoo | 1,08 | 6 | 1,03-1,40 | 1,08 | 3,89 | 0,83 | 1,04 | 1,04 |
| dunes | 1,13 | 6 | 1,04-1,16 | 1,13 | 2,19 | 0,80 | - | 1,22 |
| mesa | 1,25 | 5 | 1,19-1,26 | 1,23 | 5,14 | 0,91 | - | 1,08 |
| zone23 | 1,40 | 5 | 1,38-1,44 | 1,39 | 4,43 | 0,69 | 1,36 | 1,50 |
| ember | 1,04 | 6 | 1,01-1,12 | 1,04 | 2,11 | 0,85 | 1,00 | 1,00 |
| sulfur | 1,08 | 6 | 1,03-1,25 | 1,08 | 2,66 | 0,41 | - | 1,08 |
| great | 1,16 | 5 | 1,06-1,26 | 1,16 | 2,82 | 0,44 | - | 1,11 |
| ashen | 1,03 | 5 | 1,02-1,07 | 1,03 | 2,28 | 1,10 | - | 0,99 |
| titan | 1,09 | 5 | 1,04-1,21 | 1,07 | 2,11 | 0,91 | - | 1,12 |
| seabed | 1,10 | 5 | 1,07-1,26 | 1,11 | 1,91 | 0,90 | - | 1,22 |
| steamfens | 1,32 | 6 | 1,21-1,36 | 1,32 | 2,79 | 0,45 | 1,41 | 1,30 |
| vfbasin | 1,06 | 6 | 0,97-1,13 | 1,06 | 3,08 | 0,47 | 0,96 | 1,12 |
| vgorge | 1,08 | 6 | 0,90-1,38 | 1,08 | 2,86 | 0,56 | - | 0,92 |
| tds | 0,97 | 6 | 0,93-1,11 | 0,97 | 2,08 | 1,02 | - | 1,04 |
| tunmyth | 0,85 | 6 | 0,74-0,91 | 0,85 | 1,60 | 0,80 | - | 0,90 |
| tree | 1,01 | 6 | 0,77-1,14 | 1,01 | 2,18 | 1,44 | - | 1,05 |
| zone45 | **1,53** | 6 | 1,26-1,73 | 1,53 | 3,25 | 0,16 | 1,46 | 691,78 |
| wastes | 1,18 | 6 | 1,13-1,36 | 1,18 | 3,70 | 0,21 | 1,18 | - |
| glitch | 1,04 | 5 | 0,99-1,06 | 1,03 | 3,18 | 0,12 | - | - |
| meadow | 1,01 | 5 | 0,94-1,12 | 0,99 | 3,16 | 0,15 | - | - |
| n1 | 1,17 | 6 | 1,00-1,33 | 1,17 | 1,18 | 0,84 | - | - |
| n2 | 1,40 | 5 | 1,31-1,54 | 1,41 | 0,76 | 0,09 | 1,41 | - |
| n3e | 1,22 | 5 | 1,15-1,23 | 1,22 | 1,49 | 0,51 | - | - |
| n3rifts | 1,23 | 5 | 0,98-1,80 | 1,21 | 1,01 | 0,27 | - | - |
| n4dunes | 1,09 | 5 | 1,07-1,17 | 1,10 | 0,59 | 0,10 | - | - |
| n4hollows | 1,41 | 6 | 1,06-1,63 | 1,41 | 1,36 | 0,13 | - | - |
| halls | 0,99 | 6 | 0,94-1,09 | 0,99 | 0,80 | 0,06 | - | - |
| cathedral | 1,03 | 6 | 0,96-1,11 | 1,03 | 0,91 | 0,17 | - | - |

**Herziene uitkomst:**
- **Harde grens 1,75×:** elke mediaan haalt hem. De hoogste mediaan is zone45 met 1,53. Twee losse rondes kwamen
  erboven of dichtbij: n3rifts 1,80 (Nether, 4–5 ms per chunk) en zone45 1,73.
- **Doel 1,5×:** 30 van de 31 gebieden halen het. **zone45 haalt het niet: 1,53 (1,26–1,73).** Krap zijn canyon 1,45,
  z12 1,44, n4hollows 1,41, n2 1,40 en zone23 1,40.
- **Een volgorde-effect is er niet.** Het verschil tussen vanilla-eerst en mod-eerst is 0,006 (mediaan over de gebieden).
- **De JIT-staat per JVM telt mee.** De rondes met de storm (m5, c1, c2) zijn over de hele linie trager: geometrisch
  gemiddelde 1,15–1,20 tegen 1,08–1,11 zonder storm, ook in gebieden die vóór vgorge gemeten worden. De mod is ~2 ms per
  chunk trager, vanilla niet.
  - De storm in `VerdantGorges.shape` is dus het zichtbare deel van een slechtere compilatiestaat. Het is geen los
    ring 4-kostje.
  - Wie de ring 4-takken takvrij maakt, wint waarschijnlijk ook buiten vgorge.

**Volgende stappen (herzien):**
1. **Eerst** `VerdantGorges.shape` takvrij maken (r. 1033 boog, r. 1056 afbrokkelen, `Route.skipped` r. 444), op
   dezelfde manier als de ring 5-hotpaths. Daarna de 6-rondemeting herhalen en kijken of storm en spreiding verdwijnen.
2. **zone45 ~5 % goedkoper** voor marge op 1,5×. Kandidaten: `mapAll` per chunk (6–7 %), `RingAquifer.openSpace`
   (`opt/aquifer`), en de noise van zone 4→5.
3. Daarna `opt/surface`, en `spotShift` takvrij maken.

## Middag 2026-10-27: de stormen zijn (grotendeels) een meetartefact

Gevonden door de r4gorge-agent (stress-repro met `-XX:PerMethodRecompilationCutoff=100` en LogCompilation):
- De **eerste `jfr start`** van de JVM retransformeert classes voor JFR's eigen instrumentatie. HotSpot maakt bij de
  eerste class-redefinitie **alle** gecompileerde code ongeldig, in één klap ~10.000–14.000 nmethods not-entrant. Elke
  methode met C2-code krijgt daardoor +1 decompile.
- Een grote compile-root die daarna pas compileert (bijvoorbeeld `VerdantGorges.shape`, 2.323 B, met ~450 geïnlinede
  parses) komt zo over de grens van 201 opgetelde decompiles. Van de som van shape kwam ~316 uit dat ene moment en maar
  ~18–27 uit gewone deopts. Elke tak die daarna trapt, krijgt action `none`: dat is de storm.
- `perf.py` en `jfr-prof.sh` doen die `jfr start` na de warm-up en vóór het eerste gebied. **De meting vergiftigde zichzelf.**
  Dat verklaart de ring 4-storm in 3 van de 6 rondes, de algemene vertraging in die rondes, en waarschijnlijk ook de
  692× in ring 5 van 2026-10-25 (ook een perf-run met JFR).
- **Een gewone server start geen JFR** en ziet dit dus niet, tenzij een beheerder `/jfr start` geeft of een profiler
  classes retransformeert. Daarom blijven de warm-up en het kolomprofiel van ring 5 nuttig.
- **Fix (infra, `infra/perf-jfr`):** JFR wordt al bij de JVM-start geïnitialiseerd. De ene invalidatie valt dan op t=0
  (~1.460 nmethods, alleen bootstrap); rond de `jfr start` van de meting zie je alleen de normale churn (≤ ~110/s). Een
  korte perf-run (ring1, steamfens, vgorge) gaf **0** deopts met action `none` (405 in totaal).
- **Gevolg:** de waarden van vandaag zijn een bovengrens. Rondes zonder storm (m1, m3, m4) gaven een geometrisch
  gemiddelde van 1,08–1,11, rondes met storm 1,15–1,20. **De volledige 6-rondemeting moet opnieuw met de gerepareerde
  tooling.** Pas dan is duidelijk of zone45 onder 1,5 komt en of `VerdantGorges` zonder het artefact nog stormt.

### Optimalisaties van vanmiddag (3 warme rondes branch vs. base, oude tooling)
- **`opt/aquifer`** (kolomprofiel in `RingAquifer`, `VanillaSky`, `ChunkPicker`): noise z12 1,37 → 1,10, canyon 1,49 →
  1,15, zone23 1,45 → 1,32, vfbasin 1,06 → 0,96, zone45 1,47 → 1,50 (gelijk). Oracle 94 M aanroepen identiek, detcmp
  0/0 in 5 gebieden.
- **`opt/surface`** (snelle biome-opzoeking, `y_bands`, biome-switches): surface-stap canyon 4,93 → 2,71, mesa 4,89 →
  2,71, zone23 4,22 → 2,51, hoodoo 3,56 → 2,10, ember 2,19 → 1,81, sulfur 2,73 → 2,32. detcmp own 0 over 2.410 chunks.
- **Profiel van zone45** (base): `ZoneFracture.fillArray` 54 %, met daarin `RingBlend.fillArray` 42 %; `ColumnTable.get`
  18 %, `Ring5Output.compute` 16 %, `RingAquifer` 9 %. Voorstellen: `cave_zone`/`cave_closure` in `ring_5/overworld.json`
  één keer per blok lezen (`cache_once` of een geheugen van één kolom in `Ring5Field`, ~3–5 %), en de terugval per punt
  van `RingBlend.fillArray` in de menging 4→5 (backlog).

