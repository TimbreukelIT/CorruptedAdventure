# Change 39f — ring 3-terrein bijschaven (archipel, put, traplijnen, vlekken, naald, kruinen, riftbogen, surface-prestatie)

Scope: `worldgen`. Fix-change na de hertest van teststop 4a (`teststop-4a-feedback.md`, "Hertest na 39d" en "Beslissingen 39f"). Eigen worktree vanaf main `088708e`, branch `ring3/terrain-39f`, parallel aan golf 2.

## Doel
1. **Archipel:** eilanden onder de wolkenlaag (tops Y ~150–185); grillige, verlopende basaltpilaren met uitsteeksels; echte lavavallen tot de grond of het kratermeer; een natuurlijke onderkant (geen losse hangende kegel). Zwevende vulkaan op dezelfde punten nagelopen.
2. **Rechthoekige put:** oorzaak gevonden en vervangen door grillige zinkgaten/instortingen.
3. **Traplijnen op de vlakte:** grilliger 2D-microreliëf.
4. **Vlakke vlekken:** grove aarde/zand → ring 3-blokken met randen die het reliëf volgen.
5. **Obsidiaannaald:** obsidiaan met verspreide gloei-aders, geen banden.
6. **Kruinen van ribben en bogen:** biome-surface overal.
7. **Riftbogen:** ~1 per riftdal.
8. **Surface-stap ring 3** goedkoper (goedkope condities eerst, gedeelde takken binnen de biome-condities).
9. **39e-restpunten** (gate-interface, Javadoc, Fill-cache, `fillArray`, `iterateNoiseColumn`).
10. Officiële prestatiemeting (ook de open 39e-meting) en client-smoke.

## Diagnose
- **Put** (save van de gebruiker, `New World (1)`, (4314, ~100, −484), ember_woods, ~200 blokken NW van de afgrond (4531, −413)): een **vanilla-grotingang** (`minecraft:overworld/caves/entrances`; de ring 3-stapel laat `min(terrain, 5 · entrances)` door het maaiveld snijden, zoals vanilla). De 4 × 8 × 4-interpolatie geeft de asgerichte wanden en hoeken van 90°. De lichtgrijze banden zijn vanilla `stone`: de surface is maar ~13–21 blokken dik (TOP, DEPTH = grove aarde in ember_woods = de "strakke aardelaag", dan tuf/basalt, dan stone). Dezelfde oorzaak geeft zulke schachten overal in ring 3.
- **Banden op naalden, pilaren en hoge bogen:** dezelfde surface-diepte: een dunne vorm die hoger is dan ~13–21 blokken toont onder zijn top stone.
- **Statische lavaplaatjes:** de bronnen staan op 0,8 × de straal op het eiland en lava stroomt in de overworld maar ~3 blokken; wat wel over de rand valt, valt met 30 ticks per blok (150 blokken ≈ 4 min).
- **Hangende kegel:** de onderkant van een eiland is een kegel (`(1 − d)^1,15`) die door de verticale interpolatie (cellen van 8) in ringen trapt en onderin als losse punt uitsteekt.
- **Vierkante pilaren:** een cirkel met straal 3–5,5 (een blokkige ronde kolom), kaarsrecht.

## Plan
**Bestanden (W-terrein)**
- `VolcanicField`, `VolcanoPlacement`, `Volcano` (eiland, pilaar, val), `ThinShapes`, `Landmarks`, `Ring3Relief`, `CellShapes`, `VolcanicOutput` (nieuwe per-blok-uitgang), `VolcanicCaches`.
- `RingAquifer` (lavavallen), `VolcanicThresholdCondition` (per-blok-uitgang).
- `density_function/ring_3/overworld.json` (eilanden in `thin`, ingangen), `field.json`.
- `tools/worldgen/ring3_surface.py` + gegenereerde `noise_settings/overworld.json`.
- 39e-restpunten: `RingBlend`, `RingFillGate`, `RingGate`, `CellEdges`, `RockPillars`, `RingChunkGenerator`.
- Tests: `VolcanicFieldTest`, `Ring3LandformsTest` e.a.

**Client/server-impact.** Alleen worldgen (server) en data; geen netwerk, geen client-code. De lavavallen zijn gewone vloeistofblokken met vanilla-updates.

## Ontwerp (implementatiekeuzes)
**Eilanden (archipels, clusters, zwevende vulkaan)**
- Eilanden zijn nu **dunne vormen**: exact per blok in de `thin`-uitgang (geen `interpolated(islands)` meer). Geen 8-blokstreping meer op de onderkant; de bron staat precies op het eiland. `islands.json` en de uitgang `islands` zijn weg.
- **Onderkant** (`Volcano.underside`): een ronde kom (`(1 − d²)^0,8`) plus 2–3 hangende rotslobben op ~⅓ van de straal, in plaats van één kegel met een punt.
- **Hoogtes:** `island_max_y` 210 → 185. Archipels alleen boven caldera's met rand ≤ 150 (kans 0,35, was ≤ 165 en 0,2); hoofdeiland top = max(rand + 32, 160–185), dikte ≤ 34. Clusters en rivierclusters volgen `island_max_y`.
- **Zwevende vulkaan:** plaat Y 165–175, kegel tot 203–225 (door de wolken), onderkant 34–44 diep met lobben; tot 5 plaatsingspogingen per cel (de eerste vrije wint), zodat er na het zakken niet minder zijn.
- **Pilaren** (`Volcano.Pillar`, per blok getest): straal 2,6–4,2, voet uitlopend (tot 2×), top uitlopend in het eiland (1,7×), 3- en 5-voudige lobben die met de hoogte draaien, leunen 3–8 blokken (gebogen, `s^1,5`), 1–3 richels van 2–5 blokken. Ze staan op de kraterbodem en lopen tot in de laagste lob.
- **Lavavallen** (`Volcano.LavaFall`, `LavaFallTrace`): per bron loopt een lijn vanaf het midden naar buiten over de echte (gewarpte) omtrek; de laatste kolom mét eiland krijgt de bron (bovenop de rand), de kolom ernaast (gedeelde zijde) de val. In het veld krijgt die kolom `fallTop`/`fallBottom`: van de bron tot waar hij landt (grond, kratermeer, een eiland of dunne vorm eronder). `RingAquifer` zet daar **vallende lava** (`LEVEL` 8, blijft staan onder lava) en laat alleen de onderste ~4 blokken stromen (fluid update), zodat de lava waar hij landt uitspreidt. Bewuste uitzondering op "geen lava tegen lucht": alleen de val zelf. Geen buur-chunk-plaatsing: de val is één kolom van de eigen chunk, deterministisch per seed.

**Rechthoekige put → instortingen** (V1 = A)
- In de ring 3-stapel: `min(terrain, max(5 · entrances, 1 − terrain − 2 · collapse))`. `terrain` = (hoogte − y)/8, dus binnen 8 blokken onder het maaiveld kan geen vanilla-ingang meer openen, behalve onder een instorting (`collapse` = voetafdruk, platte cache).
- `CellShapes.Collapse` (cel 224, kans 0,85; filter in `placeCollapse`): rand met 2-, 3- en 5-voudige harmonischen (straal 7–13, ±20–48 %) plus warp, 12–22 diep in drie steile treden, wanden `cliff` (rots), bodem puin (dunne vormen van 1–3 blokken per 2 × 2 kolommen, soort `rubble`). Op ~45 % van de rand een overhangende lip van 3 blokken (holte tot de eerste trede). 45 % heeft een schacht (straal 2,2–3,5, grillig) tot 40 blokken onder de bodem (niet onder Y 12). Op de vlaktes (as, smeulwoud, stromen, pieken, zwavel), vrij van vulkanen (+30), rivieren (+12), rifts en afgronden. Niet in de rivier-ondergrond (`TRACE`), zodat rivieren en instortingen elkaar niet vragen.
- Uitkomst testseed: ~240 instortingen in de ring (~1 per 300 blokken op de vlaktes), ~110 met schacht.

**Traplijnen** (punt 3): `ThinShapes.microSteps`: op glooiende grond zonder andere vormen verhoogt een klonterig patroon (~6–8 blokken, `Ring3Relief.microSteps`) kolommen vlak onder de volgende trede met één blok (drie blokken dik, vult een interpolatieverschil). De traplijnen worden rafelig. Eén extra 2D-ruissample per kolom; geen 3D-ruwheid.

**Surface** (punten 4–6, 8)
- Nieuwe per-blok-uitgangen voor `volcanic_threshold`: `thin_kind` (−1 geen; 0 biome, 2 obsidiaan, 3 basalt, 4 eiland, 5 puin), `tread` (patroon per blokhoogte, langzaam over de vlakte) en `vein` (dunne kronkelende lagen). Nieuwe kolomuitgang `collapse`. `solid_kind` is weg. Access transformer: `SurfaceRules$Context blockY`.
- **Dunne vormen geheel geschilderd** (ook onder de surface-diepte): naalden obsidiaan met kronkelende aders van crying obsidian en dunne magma-aders (geen rechte banden); basalt (prisma's, pilaren, bruggen, zeepilaren); eilanden (as/puimsteen boven, basalt/blackstone met gloeinaden eronder); puin. Ribben, wervels, bogen en hoodoos krijgen de **biome-surface overal** (besluit 4).
- **Ember_woods** (V4): puimsteen/tuf per trede (`tread` < 0,24), grove aarde alleen als spikkels, DEPTH as/tuf. **Dried_seabed:** grind-/kleivlekken als spikkels binnen de grote vlekken.
- **Eén ring 3-tak** (alle 8 biomes) op de plaats van de oude takken: eerst de dunne vormen (elke diepte), dan binnen `GATE` (de diepte van het ondergrondpakket, 9 + surface-diepte + 0–6) de gedeelde vulkaanregels, de 8 biome-takken en het pakket. Plus een **snelle weg** vooraan in `above_preliminary_surface`: diepe blokken van de ring 3-kern (ring 3 binnen bereik, ring 2 en 4 niet, onder `GATE`, geen dunne vorm) blijven direct steen; anders vroegen ze per blok hun biome op in alle takken. Gedrag gelijk: die blokken werden toch steen.

**Riftbogen** (punt 7): per cel (320) het diepste riftpunt uit 10 × 10 kandidaten (`Landmarks.riftFloor`, per wereld gecachet); een boog alleen als geen buurcel binnen 220 blokken een dieper punt heeft. Testseed: 20 bogen (39d: 3), onderling > 220 blokken; ~22 riftdalen van ≥ ~190 blokken.

**39e-restpunten** (punt 9): `RingGated` (sealed interface van `RingGate` en `RingFillGate`, `unwrap`) in de resolve-lussen van `CellEdges`, `RockPillars`, `VolcanicField`; Javadoc `RingBlend`/`RingGate`/`RingFillGate` bijgewerkt (2 blokken per as, 2,83 diagonaal); `RingFillGate.Fill` per (dimensie, ring); `RingBlend.fillArray` één ringlookup per unieke kolom (tot 32); `RingChunkGenerator.getBaseHeight`/`getBaseColumn` in `duringFill`.

**Accessors** (verzoek A en W-biomes): `VolcanicField.volcanoesWithin`, `nearestVolcano`, `moundAt`, `sinkholeAt`, `collapseAt`, `ribcageNear` met records `VolcanoInfo`, `Mound`, `Sinkhole`, `Point`, `Ribcage`. `CellShapes.moundShape`/`sinkholeShape` zijn de bron voor zowel terrein als accessor.

## W-checklist
- [x] `./gradlew build` groen (unit tests incl. `ClientIsolationTest`, 16:25); `runData` zonder diff
- [x] Vóór/na-kaarten (`build/c39f/maps/before_*`, `after1_*`) en screenshots (`build/c39f/shots/before*`, `a1_*`); een laatste setje ná de twee kleine fixes (grillige eilandrand, geen scheurstrepen) is niet gemaakt (servercrash door geheugentekort op de machine; nieuwe testafspraak: in-game in integratie 2)
- [x] Determinisme (`c39f-det-r1-a`/`-b`, omgekeerd): A–E 2.846/2.880 identiek, archipel 616/625, put 289/289; verschillen alleen lucht/lava/water en obsidiaan (stroming, lava raakt water)
- [x] Prestatie: 3 warme rondes over 19 gebieden (alle noise-medianen ≤ 1,58) + 1 koude ronde (titan 2,00 / smeulwoud 1,89 geaccepteerd als JIT-koude start, keuze A; mediaan over 4 rondes ≤ 1,60)
- [x] S: server + client join; `tpring 1|2|3`, `tpboundary 2|3`, `tpring 3 nether`; portal ring 3 (4755, 91, 16) → Nether (1197,5, 36, 9,5) en terug (met een item-entity; de client-speler beweegt niet)
- [x] Review (main, `/code-review medium`): 8 punten opgelost (zie "Review"), 2 zo gelaten; daarna `cleanTest build` groen (567 tests) en `runData` zonder diff
- [ ] Commit
- [ ] **Voor INT2 (in-game, niet in 39f gedaan):** (1) eilandranden zonder wiggen/verticale snijvlakken tot de reach-box; (2) geen basalt onder de pilaarvoet (grotten/lavabuizen onder archipel-caldera's open); (3) eilandkwabben niet in kraterwand of heuvel; (4) pilaren eindigen in het eiland (niet zwevend, ook met leuning); (5) alle lavavallen aanwezig (geen stil verdwenen val); (6) riftbogen: elk kloofdal met een kandidaat krijgt een boog of een buurboog. Plus de ná-screenshots van de laatste fixes.

## Resultaten
**Model (seed 20260930, na de review):** 21 riftbogen uit 25 kandidaten; 6 pilaren; 22 lavavallen van vrije eilanden. **Vóór de review:** 4 archipels ((−1870, 4513), (2622, −2738), (4993, −2124), (5494, 1413)), 46 vrije eilanden bij vulkanen + rivierclusters, hoogste top Y 185; 3 zwevende vulkanen ((−3576, −2200), (−1215, 4134), (1671, 3659); plaat Y 171–173, kraterrand Y 217–220), elk 4 lavavallen; ~25 lavavallen van eilanden; 239 instortingen (110 met schacht); 20 riftbogen.

**Prestatie** (JFR, 16 × 16 chunks per gebied, mod tegen vanilla-datapack in één server, warm-up, verse wereld per ronde, `build/c39f/perf-p1..3.*`):

| Gebied | noise mediaan (3 warm) | min–max | surface mediaan |
|---|---|---|---|
| ring 1 | 1,22 | 0,97–1,25 | 1,42 |
| ring 2 | 1,45 | 1,32–1,47 | 2,41 |
| horizonreus | 1,46 | 1,42–1,51 | 2,51 |
| caldera | 1,40 | 1,39–1,40 | 2,30 |
| lavarivieren | 1,39 | 1,13–1,39 | 2,02 |
| asvlakte | 1,18 | 0,93–1,35 | 1,84 |
| zeekom | 1,38 | 1,33–1,41 | 1,56 |
| titanenveld | 1,35 | 1,34–1,41 | 1,76 |
| pieken | 1,41 | 1,30–1,49 | 2,15 |
| basaltstromen | 1,39 | 1,14–1,44 | 1,93 |
| smeulwoud | 1,52 | 1,25–1,56 | 1,85 |
| zwavelvelden | 1,58 | 1,35–1,71 | 2,48 |
| slapende krater | 1,37 | 1,34–1,37 | 1,83 |
| Nether-1 | 1,21 | 1,20–1,29 | 1,11 |
| Nether-2 | 1,46 | 1,43–1,47 | 0,80 |
| Nether-3 (4) | 1,24–1,43 | 1,20–1,43 | 0,87–1,26 |

- Noise: alle medianen ≤ 1,58 (budget 1,75). Vergelijkbaar met 39d ná review 2 (1,09–1,64).
- Surface ring 3: 1,6–2,5× (39d: 1,5–2,8×). De nieuwe per-blok-tests (dunne vormen) kosten iets; de snelle weg en de `GATE` winnen dat terug, maar de stap blijft boven vanilla. Ring 2 2,4× (niet geraakt).
- **Koude ronde** (`perf-p4`, 2026-10-09; verse JVM, `NOWARM`: geen warm-up; ook de features-stap gemeten), mod/vanilla per gebied (mediaan over 256 chunks):

| Gebied | noise koud | surface koud | features koud |
|---|---|---|---|
| ring 1 | 1,18 | 1,39 | 1,07 |
| ring 2 | 1,50 | 2,76 | 0,80 |
| horizonreus | 1,58 | 2,62 | 0,09 |
| caldera | 1,52 | 2,32 | 0,12 |
| lavarivieren | 1,60 | 2,63 | 0,13 |
| asvlakte | 1,39 | 2,36 | 0,13 |
| zeekom | 1,37 | 1,76 | 0,10 |
| titanenveld | **2,00** | 2,16 | 0,10 |
| pieken | 1,36 | 2,00 | 0,10 |
| basaltstromen | 1,38 | 2,01 | 0,06 |
| smeulwoud | **1,89** | 2,20 | 0,07 |
| zwavelvelden | 1,53 | 2,69 | 0,10 |
| slapende krater | 0,87 (vanilla-uitschieter 57 ms) | 1,71 | 0,09 |
| Nether-1 | 1,31 | 1,17 | 0,74 |
| Nether-2 | 1,50 | 0,79 | 0,11 |
| Nether-3 (4) | 1,30–1,51 | 0,91–1,42 | 0,14–0,25 |

- Duiding koud: vanilla blijft ~35 ms (gelijk aan warm); alleen mod stijgt in titanenveld (51–53 → 68,5 ms) en smeulwoud (56 → 62 ms). Dat past bij JIT op landvormspecifieke codepaden die zonder warm-up pas in dat gebied voor het eerst draaien (niet bewezen; de JFR-bestanden zijn na het parsen verwijderd). Mediaan over 4 rondes: titan 1,38, smeulwoud 1,54, binnen 1,75. Geaccepteerd (keuze A van de gebruiker).
- Features ring 3: 0,3–0,4 ms per chunk tegen 2,5–6 ms vanilla (ring 3 heeft nog weinig features).

## Review (main, `/code-review medium`, 2026-10-09)
1. `Volcano.ragged` niet monotoon (noemer ongeklemd → wiggen tot de reach-box): binnen de ellips `d/(1 + h·d⁴)` (|h| ≤ 0,16, stijgend), daarbuiten één op één verder vanaf de randwaarde; ≥ 1 vanaf d ≈ 1,14. Geldt ook voor de zwevende vulkaan. Test `theRaggedOutlineRisesAndStaysOutsideBeyondTheReach`.
2. `Column.pillarFloor` werd nooit gezet (pilaar als massieve cilinder tot Y 0): `Pillar.floorY()` (één blok in de kraterbodem), gezet in `addPillar`, `reset` op `MAX_VALUE`; `contains` is onder `floorY` en boven `top` onwaar. Test `pillarsStartAtTheCraterFloorAndEndInsideTheIsland`.
3. `checkIsland` keek niet onder de kwabben: ook 8 punten op `LOBE_D` (0,38) met `lowestAt(LOBE_D)`. Test `clearIslandsHangAboveTheGroundUnderTheirLobes`.
4. Pilaartop op de verkeerde hoek en zonder leuning: `VolcanoPlacement.pillarTop` meet de onderkant (met warp, echte hoek) op de echte top (as + leuning) en 8 punten over de uitlopende top; top = hoogste onderkant + 5, hooguit 2 onder de eilandtop. Zelfde test als 2.
5. Vallen voorbij de reach: tracering tot `min(1,6 · lengte, reach − 2)` (eilanden) en `min(1,3 · straal, reach − 2)` (zwevende vulkaan). Test `lavaFallsStayWithinTheirShapesReach`.
6. Riftboog week ook voor een afgewezen buurboog: `riftArchCandidate` (geometrie) + `riftArch` wijkt alleen voor een diepere buur binnen 220 blokken waarvan de boog echt staat (diepste eerst, gelijk → cel; deterministisch). Testseed: 21 bogen uit 25 kandidaten (was 20). Test `aRiftArchOnlyYieldsToANeighbourThatHasOne` (ook omgekeerde vraagvolgorde in een tweede veld).
7. `Pillar` is geen record meer (arrays → equals is identiteit, gedocumenteerd); `reach` als veld.
8. `RingFillGate.Fill`: geen map meer; de eerste dimensie die vraagt bepaalt de fill, een andere (komt niet voor) wordt zonder onthouden beslist; `java.util`-namen geïmporteerd.
9. Zo gelaten: de accessors zonder aanroepers (contract; voor W-biomes 47c/48 en A 50b).
10. Zo gelaten: CODEBASE.md (main voegt de delta samen in INT2).
- Testaanpassing: `pillarsAreColumnsThatFlareLeanAndHaveLedges` zoekt de gelobde omtrek nu op elke blokhoogte (was elke 3): met de hogere pilaartop viel bij een smalle pilaar (straal ~2,6) het enige verschil van 2 blokken tussen de steekproeven.

## Voortgang
Voortgang: 13:50 start, branch, docs en screenshots gelezen; oorzaak put gevonden; ontwerpvragen V1–V4 naar main.
Voortgang: 14:05 antwoorden V1–V4 (alle A) vastgelegd; accessor-signaturen naar main.
Voortgang: 14:20 vóór-kaarten (`build/c39f/maps/before_*`); vóór-screenshots = 39d `final_shot_*`/`check_shot_*` (zelfde terrein als 088708e) + de twee screenshots van de gebruiker (`build/c39f/shots/before*`), want de client kon niet naast de vóór-server bouwen.
Voortgang: 15:00 code eilanden/pilaren/vallen, instortingen, microtreden, surface, riftbogen, 39e-restpunten, accessors; unit tests groen (553), nieuwe tests `Ring3PolishTest`.
Voortgang: 15:35 riftbogen met onderdrukking (20 in de ring), geen hoodoos/prisma's/naalden in instortingen; `runData` zonder diff.
Voortgang: 16:10 in-game ronde a1 (`c39f-a1`): kaarten `build/c39f/maps/after1_*`, screenshots `build/c39f/shots/a1_*` (F2 van de client; PrintWindow gaf wit). Gefixt na de beelden: grilligere eilandomtrek (`Volcano.ragged`), geen scheurruis (verticale strepen) op dunne vormen van de biome-soort. S-smoke: `tpring 1|2|3`, `tpboundary 2|3`, `tpring 3 nether` ok; portaltest mislukt doordat de testspeler doodviel (survival-tp), opnieuw in de volgende sessie.
Voortgang: 16:25 `./gradlew build` groen (incl. tests); determinisme gestart.
Voortgang: 16:45 determinisme klaar; S-smoke + portaltest (item-entity) ok. 17:25 drie warme prestatierondes klaar. De koude ronde had om 17:31 toch lock en slots gekregen (het stoppen van de achtergrondtaak stopte het script niet); om 17:40 alle eigen processen gestopt (server via RCON `stop`, scripts beëindigd), lock en slots vrijgegeven, `enable-rcon=false`, de halve wereld `c39f-p4` en de JFR-bestanden verwijderd. Geen koude meting.
Voortgang: 2026-10-09 11:47 overgenomen door een verse agent; `./gradlew build` groen (up-to-date), `runData` zonder diff.
Voortgang: 12:16 koude ronde (`perf-p4`, verse JVM zonder warm-up, ook features gemeten): noise titanenveld 2,00 en smeulwoud 1,89 boven 1,75 in deze ene ronde (mediaan over 4 rondes 1,38/1,54); overige ≤ 1,60. Vraag (A/B/C) naar main; keuze A (accepteren).
Voortgang: 13:09 ná-screenshots (`build/c39f/shotsrun.sh c39f-a2 a2`, verse wereld, eigen client via PID uit `build/39f.pids`): de server crashte om 13:10:51 tijdens het genereren van de archipel door geheugentekort op de machine ("paging file is too small", `build/c39f/hs_err_pid35632.log`, geen mod-fout); het script bleef daarna door fork-fouten hangen met lock + 2 slots vast tot 14:04. Toen gestopt, lock en slots vrijgegeven, `enable-rcon=false`, halve wereld `c39f-a2` verwijderd. Geen ná-screenshots van de laatste fixes; volgens de nieuwe testafspraak (in-game één keer in integratie 2) niet opnieuw gedaan. Laatste beelden: `build/c39f/shots/a1_*`.
Voortgang: 14:05 nieuwe testafspraak (per pakket alleen B + D); "klaar voor review-doorsturen: 39f" naar main.
Voortgang: 14:18 reviewpunten 1–8 opgelost met 5 nieuwe tests; `cleanTest build` groen (567 tests incl. `ClientIsolationTest`), `runData` zonder diff. Determinisme offline: de bestaande volgorde-tests (`columnsArePureWhateverTheOrderOrCache`, `riversAreTheSameWhateverAsksFirst`) plus de nieuwe riftboog-test in omgekeerde volgorde groen; in-game determinisme naar INT2.

## Status bij onderbreking (2026-10-08, 17:35; achterhaald, op 2026-10-09 afgerond, zie Voortgang en Review)
- **Worktree:** `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a2e6b63fe809808ca`, branch `ring3/terrain-39f` (vanaf main `088708e`). **Niets gecommit**; alle wijzigingen staan ongestaged in de worktree. Geen eigen processen meer actief, `enable-rcon=false`, lock en slots vrij.
- **Af en getest:** punten 1–9 en de accessors gebouwd; `./gradlew build` groen (553 tests incl. `Ring3PolishTest`, `ClientIsolationTest`); `runData` zonder diff; determinisme (A–E 2.846/2.880, alleen vloeistof/obsidiaan); S-smoke (join, `tpring`, `tpboundary`, portal ring 3 ↔ Nether-3); 3 warme prestatierondes (noise ≤ 1,58, surface ring 3 1,6–2,5×).
- **Nog te doen:** koude prestatieronde (`build/c39f/perfrounds.sh 4 4 cold`, daarna `python build/c39f/perfsum.py`); laatste setje ná-screenshots in een verse wereld na de twee kleine fixes (`build/c39f/srv.sh c39f-a2`, `py/gen.py areas-39f.txt`, `cli.sh start a2`, `py/shots39f.py a2`); review via main ("klaar voor review-doorsturen: 39f"), bevindingen verwerken, dan commit (`worldgen: …`, per pad stagen; niet `Scratch39fTest` (staat als `build/c39f/Scratch39fTest.java.txt`), niet `docs/prompts/`, niet `.claude/`).
- **Beelden:** kaarten `build/c39f/maps/` (`before_*` = 088708e, `after1_*`); screenshots `build/c39f/shots/` (`before39d_*` = 39d-shots van hetzelfde terrein, `before_user_*` = de twee van de gebruiker, `a1_*` = ná); blokkaarten uit de saves in `build/c39f/` (`user_pit_*` = de oorzaak van de put, `under_arch.png`, `a1_collapse_*`, `a1_ember_top.png`).
- **Scripts:** `build/c39f/` (`srv.sh`, `srvstop.sh`, `cli.sh`, `gw.sh`, `det.sh`, `perfrounds.sh`, `perfsum.py`, `py/*`).
- **Nether-deel van `ring3_surface.py`** niet gewijzigd (alleen het overworld-deel van `main()` en de overworld-regels).

## CODEBASE-delta
Kop: "Laatst bijgewerkt: 2026-10-08 — change 39f (`worldgen`, na de hertest van teststop 4a): archipels en zwevende vulkaan onder de wolken met echte lavavallen, grillige pilaren en rotsige onderkanten; vanilla-grotingangen onder het ring 3-maaiveld dicht, grillige instortingen als openingen; rafelige traplijnen; dunne vormen per blok geschilderd; riftbogen per dal; ring 3-surface sneller; read-only accessors op `VolcanicField`."

Mappenstructuur / classes (`…worldgen.densityfunction` tenzij anders):
- `VolcanicField.java`: 39f — eilanden exact per blok in `thin` (geen `islands`-uitgang meer); `thinKind`/`tread`/`vein` (per blok, voor de surface); instortingen (`collapse(i, j)`, cel 224), microtreden, lavavallen (`landFall`), pilaren per blok; `riftFloor(i, j)` (cache); accessors `volcanoesWithin`, `nearestVolcano`, `moundAt`, `sinkholeAt`, `collapseAt`, `ribcageNear` (records `VolcanoInfo`, `Mound`, `Sinkhole`, `Point`, `Ribcage`; puur, thread-safe; nog zonder aanroepers in 39f: voor W-biomes 47c/48 en A 50b, zie het contract). `islandClear` kijkt ook onder de kwabben. `Column`: solids met soort (max 8), holtes (max 3), pilaren (max 2), val (`fallTop`/`fallBottom`/`fallFlowBelow`), `collapse`.
- `Volcano.java`: `Island` (39f: lobben van de onderkant, `falls`, `columnTop`, `lowestAt`), `Pillar` (39f: final class, geen record; per blok, uitlopend, leunend, lobben, richels; `contains`, `floorY`, `reach` als veld), `LavaFall` (bron + valkolom), `underside` (kom + 2–3 lobben, diepste op `LOBE_D` 0,38), `ragged` (grillige omtrek, stijgend in d, ≥ 1 vanaf ~1,14).
- `LavaFallTrace.java` (nieuw, 39f): waar een val een zwevende vorm verlaat (laatste kolom met vorm = bron, buurkolom = val).
- `VolcanoPlacement.java`: 39f — archipels alleen boven caldera's met rand ≤ 150 (kans 0,35), tops ≤ `island_max_y` (185); eilanden met lobben en vallen (`island`, tracering binnen de reach); pilaren nieuw (`pillarTop`: in het eiland op de echte top, met leuning).
- `ThinShapes.java`: 39f — soorten per dunne vorm (biome/obsidiaan/basalt/puin); `collapse` (puin, overhang, schacht), `microSteps`; zwevende vulkaan met vallen; pilaren niet meer hier.
- `CellShapes.java`: 39f — `Collapse` + `collapseShape`/`placeCollapse` (cel 224, kans 0,85); `moundShape`/`sinkholeShape` (bron van terrein en accessor).
- `Landmarks.java`: 39f — soorten `KIND_BIOME` 0, `KIND_OBSIDIAN` 2, `KIND_BASALT` 3, `KIND_ISLAND` 4, `KIND_RUBBLE` 5 (`KIND_BONE` weg); `FloatingVolcano` (plaat Y 165–175, kegel tot 203–225, lobben, grillige rand, `falls`, 5 pogingen per cel); riftbogen: `riftFloor` (10 × 10 kandidaten per cel van 320), `riftArchCandidate` (geometrie) + `riftArch` (wijkt alleen voor een diepere buurcel binnen 220 blokken die zelf een boog heeft).
- `Ring3Relief.java`: 39f — `microSteps`, `treadPhase`, `veinPhase`.
- `VolcanicOutput.java`: 39f — uitgangen `collapse` (kolom), `thin_kind`, `tread`, `vein` (per blok, alleen voor de surface); `islands` en `solid_kind` weg; `forSurface()`, `blockValue(…)`.
- `RingGated.java` (nieuw, 39f): sealed interface van `RingGate` en `RingFillGate` (`argument()`, `unwrap`), gebruikt door de resolve-lussen van `CellEdges`, `RockPillars`, `VolcanicField`.
- `RingFillGate.java`: 39f — `Fill` per ring voor de dimensie die als eerste vraagt (een andere wordt zonder onthouden beslist); Javadoc (2 blokken per as).
- `RingBlend.java`: 39f — `fillArray`: één ringlookup per unieke kolom (tot 32); Javadoc gates.
- `worldgen/chunk/RingAquifer.java`: 39f — lavavallen: vallende lava (`LEVEL` 8) in `[fallBottom, fallTop]`, de onderste blokken met fluid update.
- `worldgen/chunk/RingChunkGenerator.java`: 39f — `getBaseHeight`/`getBaseColumn` in `RingFillGate.duringFill`.
- `worldgen/surface/VolcanicThresholdCondition.java`: 39f — ook per-blok-uitgangen (`thin_kind`, `tread`, `vein`).
- `META-INF/accesstransformer.cfg`: `SurfaceRules$Context blockY` (39f).

Databestanden:
- `density_function/ring_3/overworld.json`: 39f — geen `max(…, interpolated(islands))` meer; grotingangen: `min(terrain, max(5 · entrances, 1 − terrain − 2 · collapse))`.
- `density_function/ring_3/overworld/collapse.json` (nieuw, `flat_cache` van `collapse`); `islands.json` weg.
- `ring_3/overworld/field.json`: `island_max_y` 185.
- `noise_settings/overworld.json` (uit `ring3_surface.py`): één ring 3-tak (8 biomes) + snelle weg vooraan in `above_preliminary_surface` (`ring_reach` ring 3, niet ring 2/4, onder `GATE`, geen dunne vorm → steen).
- `tools/worldgen/ring3_surface.py`: 39f-structuur (dunne vormen, `GATE`, biome-takken als (top, rest), snelle weg); herkent de oude indeling.

Weg: uitgangen `islands` en `solid_kind`, `Landmarks.KIND_BONE`, `density_function/ring_3/overworld/islands.json`. Nieuw: `RingGated`, `LavaFallTrace`, `ring_3/overworld/collapse.json`, uitgangen `collapse` (kolom), `thin_kind`, `tread`, `vein` (per blok).

Tests: `Ring3PolishTest` (nieuw); bijgewerkt: `VolcanicFieldTest`, `Ring3LandformsTest`, `Ring3TerrainJsonTest`, `VolcanicThresholdConditionTest`, `Ring3BiomesTest` (één ring 3-tak).

Valkuilen:
- **Dunne vormen en de surface-diepte (39f):** de surface schildert per kolom vanaf de top naar beneden (TOP, DEPTH, ~13–21 blokken pakket, daaronder vanilla `stone`). Een dunne vorm die hoger is (naald, pilaar, boog, eiland) moet als geheel geschilderd worden: `volcanic_threshold` op `thin_kind` (per blok).
- **Vanilla-grotingangen (39f):** `entrances` mag in vanilla tot ~12 blokken onder het maaiveld snijden; door de 4 × 8 × 4-interpolatie wordt dat een rechthoekige schacht met stone-wanden. In ring 3 alleen nog onder een instorting.
- **Vallende lava (39f):** lava valt in de overworld met 30 ticks per blok (150 blokken ≈ 4 min); een val van een zwevend eiland wordt daarom als kolom vallende lava gegenereerd.

## Beslissingen
- 2026-10-08 | worldgen | Vanilla-grotingangen in ring 3 worden ~8 blokken onder het maaiveld afgesloten; de openingen aan de oppervlakte zijn eigen grillige zinkgaten/instortingen (~1 per 250–350 blokken op de vlaktes), een deel met een schacht naar de grotten | de asgerichte "put" was een vanilla-ingang (interpolatie 4 × 8 × 4, stone-wanden); grottoegang blijft (V1 = A)
- 2026-10-08 | worldgen | Zwevende vulkaan: eilandplaat Y ~165–175 onder de wolken, alleen de kegel steekt door de wolken (~205–225); lavavallen tot de grond | helemaal onder/boven de wolken past niet (V2 = A)
- 2026-10-08 | worldgen | Alle zwevende eilanden (archipels én clusters boven caldera's en rivieren) met tops ≤ 185 en dezelfde vormverbeteringen (onderkant, echte lavavallen) | consistent onder de wolkenlaag (V3 = A)
- 2026-10-09 | worldgen | Koude uitschieters titan 2,00× / smeulwoud 1,89× geaccepteerd als JIT-koude start; mediaan over 4 rondes binnen budget | keuze A van de gebruiker na de koude ronde van 39f
- 2026-10-08 | worldgen | Alleen ember_woods: coarse_dirt → ring 3-blokken met randen die de treden volgen, coarse_dirt alleen klein accent, DEPTH een ring 3-blok; dried_seabed houdt zand als basis, de grote grind-/kleivlekken worden kleiner | de vlakke vlekken waren ember_woods; zand hoort bij de zeebodem (V4 = A)

## Backlog
- `worldgen` | Wanden van diepe sneden (afgrond, schachten van instortingen, hoge riviermuren) tonen onder ~13–21 blokken nog vanilla `stone`: de ring 3-ondergrond is stone (V1 39c). Kandidaat voor W-terrein 42 (lavabuizen/magmakamers) of een eigen ondergrondpakket.
- `worldgen` | Riftbogen: 20 in de ring op de testseed (onderkant van "grofweg 20–40"); de onderdrukking binnen 220 blokken kiest per stuk dal de diepste plek. Bij teststop 4b beoordelen.
- `worldgen` | Lavavallen zijn één blok breed; bij de teststop beoordelen of een tweede kolom (breder gordijn) gewenst is.
- `worldgen` | Titanenveld en smeulwoud koud (verse JVM, geen warm-up) opnieuw bekijken bij teststop 4b / de officiële meting in integratie 2 (39f koud: noise 2,00× en 1,89×, warm ≤ 1,56×).
- `atmosphere` | De zwevende vulkaan steekt met zijn kegel door de wolkenlaag (V2 = A); pakket 50 (donkere aswolken) ziet hem dus deels in de wolken.
