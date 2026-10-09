# Golf 2 — stroom W-terrein: 42 (lavabuizen, magmakamers, grotten ring 3, geode-holtes)

Scope: `worldgen` — werkpakket 42, branch `ring3/terrain-g2`, poort 1 (server 25571, RCON 25581). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

> **Start pas ná de merge van change 39f.** 39f schaaft het terrein bij in dezelfde bestanden: `VolcanicField` (en `VolcanoPlacement`, `ThinShapes`, `VolcanicCaches`), `Landmarks`, `tools/worldgen/ring3_surface.py` en de gegenereerde `noise_settings/overworld.json`. 42 takt af van main ná 39f. Optie van opdracht 05: de 39f-agent doet daarna ook 42.

**Doel:** de ondergrond van ring 3 krijgt een eigen karakter en een schuilroute (teststop 4b: "lavabuizen als schuilroute"):
- **lavabuizen:** lange, gladde basalttunnels, deels met lava, bruikbaar als reisroute;
- **magmakamers:** grote holtes boven een lavameer, met vulkaniet en vuurrobijn in de wanden;
- ring 3 kiest zijn eigen floodedness/erosion (backlog r. 89);
- de grotmonden op kraterranden en kegelflanken worden gedempt (backlog r. 183);
- **geode-holtes** (vuurkristal) als DF (sjabloon `DewGeodes`), bekleed door W-biomes in 41b.

**Bron:**
- `fase-1-wereld.md` Ring 3 "Grotten", "Ores";
- `contract.md` §4;
- `golf-1-terrain.md` (backlog 42), `change-39d.md` (de lavabuis-sleuven/-tunnels van `basalt_flows`, `thin`, `LavaRivers`), `change-39e.md` (§5: gate, per-kolomtabel `RingAquifer`);
- backlog r. 89, 183, 184, 194, 195;
- sjablonen `DewGeodes`/`DewGeodeShape` (holte + wandtest), `LavaRivers` (pure getraceerde paden, per wereld gecachet), `ThinShapes` (open en vaste intervallen per kolom buiten de interpolatie), `RingAquifer` (lava per kolom).

## 1. Werk (één commit; splitsen in 42a/42b als het groot wordt, melden aan main)
1. **Lavabuizen** (karakter: G2-WT1). Voorgestelde aanpak (implementatiekeuze):
   - buizen als **getraceerde 2D-paden met een hoogteprofiel** (sjabloon `LavaRivers`: pure functie van seed + cel, één keer per wereld getraceerd en gedeeld);
   - per kolom een open interval (zoals `thin`/de `void`-intervallen van 39d), geen 3D-ruis. Goedkoop, glad en deterministisch.
   - Ze sluiten aan op de sleuven en tunnels van 39d in `basalt_flows` (ingestorte stukken = open dak); daklichten waar het dak dun wordt.
   - Lava: een gootje via de per-kolomtabel van `RingAquifer` (lava per kolom, met korst waar nodig) of droog, volgens G2-WT1.
   - **Askorst boven dunne buisdaken** (backlog r. 194): de buis is een DF-holte, dus de surface ziet hem. Een dak van één blok wordt `ash_crust` (de regel van 39c), en die stort in als een speler eronder loopt: de buis wordt dan ineens een open sleuf (zon, hitte).
2. **Magmakamers** (schaal: G2-WT2).
   - Per grote cel (jittered grid) een afgeplatte holte boven een lavameer (lava per kolom uit de aquifer, korst tegen open lucht), verbonden met de buizen.
   - Wanden: een schil van basalt (en blackstone). Daarin komt de basaltvariant van vuurrobijn (41, W-biomes) vanzelf.
3. **Wandmateriaal:** buis- en kamerwanden van basalt/smooth basalt met blackstone-aders, het ondergrondpakket doorgetrokken (backlog r. 195).
   - Via eigen takken in `ring3_surface.py` (`stone_depth` floor/ceiling werkt op elke diepte), met goedkope condities vóór de dure (tip 39e).
   - Let op de surface-stap (nu 1,5–2,8×; meten).
4. **Floodedness en erosion ring 3** (`ring_blend/overworld_floodedness`, `_erosion`, de ring 3-sleutels):
   - floodedness zo dat aquiferwater alleen onder Y ~0 blijft (V8) en geen water in buizen/kamers boven Y 0;
   - erosion constant buiten het deep-dark-bereik (ring 3 heeft geen deep dark).
   - De bestaande regels (`ring3Dry`, `ring3Seal`) blijven de grens.
5. **Grotmonden dempen** (backlog r. 183): de vanilla-ruisgrotten (spaghetti/entrances) in de stapel `ring_3/overworld.json` worden gedempt waar `volcano_rise` hoog is (kraterranden, kegelflanken) en boven Y ~140 (eilanden). Ze blijven in de vlaktes (G2-WT3).
6. **Geode-holtes** (vuurkristal, voor 41b):
   - DF `ring_3/overworld/fire_geodes` (sjabloon `DewGeodes`: −1 in de holte, +10 erbuiten, wandtest);
   - pure vorm `FireGeodeShape`, die W-biomes in 41b alleen leest;
   - Y-band en frequentie volgens G2-WB2 (W-biomes-plan);
   - niet binnen de wandtest van een afgrondwand, buis of kamer, tenzij G2-WB2 = A ("soms aangesneden door een buis": dan alleen buizen, nooit afgronden).
7. **Contract:** `fire_geodes` en eventueel een buismasker (bijv. `ring_3/overworld/lava_tube`, 0..1, voor C of A als die het willen lezen) zijn nieuwe gedeelde DF's. Main zet ze in `contract.md` §4 vóór de bouw.

## 2. Bestanden
- **Eigen:**
  - `density_function/ring_3/overworld/**`, nieuwe `noise/ring_3_*`;
  - `VolcanicField` en familie (`Landmarks`, `ThinShapes`, `VolcanicCaches`, `VolcanoPlacement`), nieuwe klassen (bijv. `LavaTubes`, `MagmaChambers`, `FireGeodes`/`FireGeodeShape`);
  - `RingAquifer`/`RingFluidPicker` (de ring 3-regel);
  - de ring 3-sleutels in `ring_blend/overworld_floodedness` en `_erosion`.
- **Append-blokken:** `ModDensityFunctionTypes` (`// ring 3 — W-terrein`).
- **Gedeeld:** `tools/worldgen/ring3_surface.py` + `noise_settings/overworld.json`.
  - 42 voegt alleen eigen takken toe (wanden van buizen en kamers).
  - W-biomes (48: `steam_fens`) voegt ook een tak toe. Wie als tweede merget, draait het script opnieuw (idempotent); geen handmatige conflictoplossing in de JSON.
- **Niet aanraken:** `ring_blend/overworld_{temperature,vegetation,weirdness}` (in 48 past W-biomes `switch_at` aan), `Ring3Biomes`/features (W-biomes).

## 3. Afhankelijkheden
- **← 39f** (hard): zelfde bestanden. Rebase/branch op de 39f-merge.
- **→ W-biomes 41b** wacht op `fire_geodes` + `FireGeodeShape`.
- **→ W-biomes 41:** vuurrobijn in basalt wordt pas zichtbaar met de kamerwanden (geen code-afhankelijkheid).
- **→ C (4b):** buizen als schuilroute. Een buis onder een dak koelt (−0,5/s), maar lava binnen 4 blokken is een hittebron (×1,5). G2-WT1 bepaalt dus of de buis echt een schuilplek is.
- **→ A:** geen. Eventueel een eigen ambient in buizen; dat is A's keuze op de biome, niet op de buis.

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-WT1 — Lavabuizen: wat voor buis?**
- **A. Gladde basalttunnels 5–9 breed, Y 20–80, een netwerk dat aan de oppervlakte opent bij de sleuven van 39d en via daklichten. Lava alleen in lagere zijtakken (een gootje met een korstrand); de hoofdbuis is droog, dus een echte schuilroute (aanbevolen).** C middel (getraceerde paden + aquiferregel). S: koel reizen onder de hitte door, met af en toe een gloeiende zijtak. P: per-kolomintervallen, goedkoop.
- **B. Overal droog.** C laag. S: veilig, maar minder vulkanisch.
- **C. Elke buis met een lavastroom over de hele lengte.** C middel. S: spectaculair, maar de buis wordt een hittebron (×1,5) en dus geen schuilplek; dat botst met 4b.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WT2 — Magmakamers: hoe groot en hoe vaak?**
- **A. Groot en zeldzaam: 30–60 breed, 15–30 hoog, Y −40…10, ~1 per 400 × 400 blokken, altijd verbonden met een buis (aanbevolen).** C middel. S: een echte ontdekking met vuurrobijn als beloning. P: lokale holte, binnen het budget (meten).
- **B. Kleiner en vaker (15–25 breed, ~1 per 150 × 150).** C middel. S: vaker vuurrobijn, minder "wow".
- **C. Een paar reuzenkamers (100+ breed, 1–3 in de hele ring) als ondergrondse landmark.** C hoog (grote holte, korst, stabiliteit van het dak). S: maximaal wow; zeldzaam gevonden. P: grotere holte, meer aquifer-werk.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WT3 — Vanilla-grotten in ring 3: hoeveel blijft er?**
- **A. De vanilla-ruisgrotten blijven in de vlaktes, worden gedempt op kraterranden en kegelflanken en verdwijnen boven Y ~140; de buizen komen erbij (aanbevolen).** C laag/middel. S: vertrouwde grotten + een eigen netwerk; geen rechte geulen in kraters. P: neutraal.
- **B. Veel minder vanilla-grotten (ring 3 voelt massief; buizen en kamers domineren).** C middel. S: eigener, maar minder mijnbouwplekken (ertsen moeilijker). P: iets goedkoper.
- **C. Vanilla-grotten ongewijzigd, alleen de kraterranden dempen.** C laag. S: buizen vallen minder op.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
Geen. Alleen worldgen (density functions, aquifer, surface-takken), deterministisch per seed.

## 6. Tests
- **B:** `./gradlew build` groen.
  - Unit tests: buispaden puur en gecachet (zelfde paden in een nieuwe instantie, na `mapAll`, op twee threads);
  - kamers en geodes binnen hun cel, nooit onder de lavazee en niet bij de ringgrenzen;
  - lava in buizen/kamers nooit tegen open lucht (korst);
  - geen water boven Y 0;
  - JSON-test voor de ring 3-sleutels in floodedness/erosion;
  - `Ring3TerrainJsonTest` bijgewerkt.
- **D:** `runData` zonder diff (tenzij de surface-takken via de generator komen: dan gecommit).
- **S:** server (25571); `/ca terrainmap section …` door een buis en een kamer.
  - Metingen in 6 ring 3-gebieden: 0 water boven Y 0, 0 lava naast open lucht, 0 lava boven lucht, geen zwevende brokken > 8 blokken.
  - Portal ring 3 → Nether-3 werkt nog.
- **Determinisme:** twee verse werelden, de tweede omgekeerd geladen. Gebieden A–E van 39d + een buisgebied + een kamergebied: identiek op vloeistofstroming na.
- **Ring 1/2 en zone 2→3:** biomes en terreinvorm gelijk aan main.
- **Prestatie (officieel-achtig, budget):** JFR `minecraft.ChunkGeneration`, stap `noise`, mod tegen vanilla als datapack-dimensie in één server, 16 × 16 chunks per gebied, warm-up over alle ringtypes, ≥ 3 rondes, mediaan.
  - Gebieden: de 13 ring 3-gebieden van 39d + een buis- en een kamergebied.
  - **Budget ≤ 1,75× per gebied.** Uitgangspunt (39d na review 2): caldera 1,59, smeulwoud 1,50, as 1,45, ring 2 1,64. De marge is klein; zit een gebied erboven: stoppen, cijfers + opties aan main.
  - Ook de surface-stap meten (geen budget, wel melden als hij stijgt).
  - Lock + alle 3 slots, nooit tegelijk met een andere meting.
- **Review:** "klaar voor review-doorsturen: 42" aan main; main draait `/code-review medium`; daarna committen.

## Uitwerking (gebouwd, implementatiekeuzes)
- **Buissystemen** (`LavaTubes`): per cel van 400 blokken (kans 0,85) één systeem, eenmaal per wereld getraceerd en per tegel van 128 blokken geïndexeerd (sjabloon `LavaRivers`).
  - **Hoofdbuis:** start bij een mond in de cel: in een cel met basalt_flows de eerste ingestorte 39d-sleuf (rijen van de as-waarde om de 12 blokken, waar het teken wisselt geïnterpoleerd), anders de eerste goede plek op een 5 × 5-raster (kern, geen krater/lava/rivier/afgrond binnen 30, grond ≤ Y 125). Het pad volgt de warp-ruis (buigt rustig, geen lussen), 35–58 stappen van 8 blokken, daarna tot 10 stappen zoeken naar een tweede mond, anders een blind einde.
  - **Vloer:** ~26 onder de grond binnen Y 20–80, gladgestreken, onder elk dak van 8 blokken rots (grond, kratermeer, lavarivier), helling ≤ 0,3; mondhellingen 0,45 tot vlak onder de grond, alleen als de hele helling onder elke lava blijft.
  - **Doorsnede:** ellips met vlakke vloer, 5–9 breed, 5–7 hoog; 1–3 daklichten waar de buis ondiep loopt (niet onder lava, op een vulkaan of bij een rivier).
  - **Zijtak:** naar het meer van een magmakamer in de cel (4 pogingen; kamer 70 blokken binnen de cel), vanaf het punt van de hoofdbuis met de kortste tak die helling 0,3 haalt (≤ 420 blokken, meander ≤ 12); anders een blinde tak van 70–110 blokken. Lavagootje vanaf 12 blokken na de splitsing.
  - **Lavagootje:** één blok diep in de vloer van de zijtak. Waar het een blok zakt (of een andere open ruimte ernaast of eronder ligt) maakt de aquifer van de lava een korstlip, dus een reeks poeltjes zonder stromende lava.
  - **Magmakamer:** afgeplatte ellipsoïde met grillige omtrek, 30–60 breed, koepel 8–16 boven en kom 6–11 onder het midden (Y −40…10), meer tot 0,55 van de kom.
- **Kolom** (`VolcanicField.Column`): opening (dunne void, buiten de interpolatie), huid (1,3) en schil (3) als blokbereiken, tot 3 blokken onder het maaiveld; lava onder de grond (`subLava`, aparte bereiken, alleen in openingen); beschermbanden (`deepGuard`, apart) om kamers, gootjes en geodes waarin vanilla-grotten dicht blijven en de aquifer rots teruggeeft (ook voor carvers).
- **Aquifer:** openingen blijven vrij van aquiferwater/-lava (lucht, of de lava van gootje/meer); rots in de beschermband blijft rots (`null`), dus geen carver snijdt een kamer, gootje of geode aan.
- **Wanden:** een eigen bovenste surface-regel direct na de bedrock-vloer (`ring_reach` ring 3 → per kolom `cave_walls` → per blok `cave_wall`). De wanden liggen ver onder de voorlopige oppervlakte, waar geen andere ring 3-regel komt. Huid smooth basalt, schil basalt, kronkelende blackstone-aders (`vein`), korst (blackstone/magma) langs gootje en meer. Bij monden en daklichten stopt de wandverf 3 blokken onder het maaiveld; de rand (en een dak van één blok: de 39c-askorst) komt uit de biome-regels.
- **Grotten dempen** (`cave_lift`, nieuwe uitgang, in de stapel opgeteld bij de grotdichtheid: ingangen, diepe grotten en noodles, nooit bij het terrein): stijgt over 20 blokken van 0 naar 2, zodat grotten geleidelijk krimpen en in ronde punten eindigen. Boven Y 146 overal dicht (vanaf Y 126 krimpend); in een vulkaanlichaam (gewicht `smoothstep(4, 16, rise)`) dicht vanaf 4 boven de vlakte waarop hij staat. `guard` blijft ±1 (alleen de banden).
- **Floodedness ring 3:** vanilla-ruis + gradiënt 0 → −2 tussen Y −24 en −8 (geen lokale aquifervloeistof erboven). **Erosion ring 3** (`ring_3/overworld/erosion`): 0 waar ring 3 het hele gewicht heeft (geen deep-dark-check van de aquifer), in de zones vanilla, zodat de zones 2→3 en 3→4 (ook de ring 4-helft) dezelfde erosion houden als vóór 42. Geen `switch_at`: erosion blijft mengen zoals het contract (§4) voor de andere routervelden eist, en botst dus niet met 48 (`steam_fens`).
- **Vuurkristal-geodes** (`FireGeodeShape`, `FireGeodes`): cel 48 (kans 0,75; gemeten 1 per 12,1 chunks), 4 pogingen per cel, straal 4–7, midden Y −50…20, bekleding tot 3 blokken (`LINING`). Alleen in de kern, niet bij de afgrond of een kamer, met grond ruim erboven. De holte is een dunne void met een beschermband; lavabuizen mogen hem aansnijden.
- **Testseed** (unit tests, ring 3 binnen ±6.400, na de review): 312 buissystemen, 224 met magmakamer, 17 startmonden in een 39d-sleuf. Een tegel heeft 5 × 5 systemen nodig (eerste tegel van een gebied ~40 ms tracering, daarna gedeeld).
- **Niet gebouwd:** het buismasker `lava_tube`: C en A hebben het niet nodig (main, 2026-10-09).

## 7. W-checklist (in-game, verse wereld seed `20260930`)
**Lichte testafspraak (2026-10-09):** per pakket alleen B + D. De in-game punten hieronder en de prestatie gaan naar **INT2**. 42 raakt de **noise-stap** (dunne voids, `guard` per blok, kolomvulling met buizen/kamers/geodes) en de **surface-stap** (een nieuwe bovenste regel), dus INT2 moet de prestatie opnieuw meten: noise per gebied plus een buis- en een kamergebied (budget 1,75×), en de surface.
Coördinaten op de testseed (uit de pure vorm):
- buismond in een 39d-sleuf: (4.851, −732), vloer Y ~101;
- systeem (−12, −1): mond (−4.724, −338), splitsing (−4.788, −439), kamer (−4.510, −282) met meer Y −16, daklicht (−4.883, −522);
- systeem (−12, −7): mond (−4.696, −2.757), kamer (−4.710, −2.632), meer Y −9;
- geode (4.018, Y 9, 407), straal 6.

- [ ] Een buisopening bij de sleuven van 39d in `basalt_flows` (~4.090, −3.570 of 4.814, −288; de agent zoekt exacte ingangen op) en erdoor lopen:
  - glad, droog, basaltwanden;
  - daklichten;
  - een gloeiende zijtak met een lavagootje en korst.
- [ ] Hitte in de buis zakt (onder een dak −0,5/s); naast een lavazijtak stijgt hij (bron ×1,5). Dat is het schuilroute-gevoel.
- [ ] Een askorst-dak boven een buis stort in als je eronder loopt.
- [ ] Een magmakamer: lavameer, basaltwanden met vuurrobijn (na 41), bereikbaar via een buis.
- [ ] Kraterranden van de horizonreus (~−3.895, 65) en de actieve caldera (~−3.870, −1.790) zonder rechte grotgeulen; geen zwevende brokken onder de archipels (2.622, −2.738).
- [ ] Geen water boven Y 0; aquiferwater alleen diep.
- [ ] Een geode-holte (coördinaten uit de pure vorm), nog zonder bekleding tot 41b.
- [ ] Wanden: smooth basalt/basalt met blackstone-aders rond buizen en kamers, ook diep (onder Y 8 geen deepslate in de wand).
- [ ] Gootje: poeltjes met korstlipjes, geen stromende lava na de eerste chunk-update; geen lava naast open lucht in buis of kamer.
- [ ] Geen carver of vanilla-grot snijdt een kamer, gootje of geode aan.
- [ ] Prestatie (INT2): noise en surface, met buis- en kamergebied.

## Review (main, `/code-review medium`, 2026-10-09)
1. `addSubLava` voegde aparte lavabereiken samen (lava/rots-pilaren in kamers en buizen): nu tot 6 aparte bereiken (`subLavaCount`, `subLavaAt` per bereik, `subLavaMin`/`Max` als omhulling). Test `lavaRangesAndGuardBandsOfAColumnStaySeparate` (gootje Y 40 boven kamermeer Y −20…−15, droge buis ertussen).
2. `addDeepGuard` idem: aparte banden (`deepGuardAt`); dezelfde test.
3. De negatieve helft van de guard-fade deed niets (alleen `max(...)` in de stapel): vervangen door `cave_lift` (0…2, opgeteld bij de grotdichtheid in de stapel), zodat grotten geleidelijk krimpen; `guard` weer ±1. Test `vanillaCavesCloseGraduallyAboveY140AndInsideVolcanoes` + JSON-test (3× `cave_lift` in de stapel).
4. Wandverf rond monden kwam aan de oppervlakte: huid en schil stoppen nu 3 blokken onder het maaiveld (`addCave`, voor alle openingen; de aparte daklicht-grens is weg). Dezelfde test.
5. De `ash_crust`-regel in `cave_walls` gold voor elke dunne plaat in zo'n kolom: weggehaald; door punt 4 komt een dak van één blok bij een mond weer in de biome-tak (39c-korst). JSON-test controleert dat de wandregel geen `ash_crust` meer heeft.
6. Erosion ring 3 = 0 trok de ring 4-helft van zone 3→4 mee: nu `ring_3/overworld/erosion` (0 alleen waar ring 3 gewicht 1 heeft, anders vanilla), geen `switch_at` (contract §4, 48). JSON-test.
7. `fire_geodes` gaf −1 ook waar de holte niet uitgehold was: nu alleen waar `thin` < 0; geode vóór de buizen in `caves()`; beschermband horizontaal reach + 1 (binnen MARGIN 13). Gedocumenteerd voor 41b bij `fireGeode`.
8. Dode tak `else if (floor < 6)` weg.
9. Tegelbereik: hoofdbuis 35–58 + 10 stappen, zijtak binnen de omhulling van hoofdbuis en cel (kamer ligt in de cel), blinde tak ≤ 110, meander ≤ 12 → 5 × 5 systemen per tegel (was 9 × 9). Sleufzoektocht via tekenwissel van de as-waarde (rijen om de 12 blokken) in plaats van een raster van 6. `decisions.tubeSystems` krijgt dezelfde LIMIT-clear als de tegels. Test `everyTileKnowsTheTubesAndChambersThatReachIt` blijft groen.
10. Gedeelde helper voor het tegelcache-patroon: naar de backlog (laat in de golf, raakt ~10 plekken).
Daarna `cleanTest build` groen (592 tests), `runData` zonder diff, `ring3_surface.py` idempotent.

## Voortgang
Voortgang: 2026-10-09 ~14:15 start in worktree `.claude/worktrees/terrain-g2`, branch `ring3/terrain-g2` op main `91af98e` (na de 39f-merge); docs en code gelezen; contract-sleutels ter info naar main.
Voortgang: 14:45 `LavaTubes`, `FireGeodeShape`/`FireGeodes`, kolom/aquifer/uitgangen, floodedness/erosion, surface-regel gebouwd. Fixes uit de eigen tests: mondhelling alles-of-niets (anders een sprong van 23 blokken in de vloer), tegelbereik inclusief zijtak en kamer, kamers per cel geplaatst met de aansluiting op de dichtstbijzijnde geschikte buispunt (31 % → 81 % van de systemen), fijn zoeken naar 39d-sleuven (4 → 20 monden).
Voortgang: 15:00 `cleanTest build` groen (591 tests, nieuw: `Ring3CavesTest`), `runData` zonder diff; `ring3_surface.py` idempotent (twee keer draaien geeft dezelfde JSON).
Voortgang: 15:14 reviewpunten 1–9 opgelost (10 naar de backlog); `cleanTest build` groen (592 tests), `runData` zonder diff.

## CODEBASE-delta
Kop: "Laatst bijgewerkt: 2026-10-09 — pakket 42 (`worldgen`, golf 2): lavabuizen (droge hoofdbuis met monden en daklichten, zijtak met lavagootje), magmakamers met lavameer, vuurkristal-geodes als holte (`fire_geodes`, `FireGeodeShape`), wanden in basalt via een bovenste surface-regel, vanilla-grotten gedempt (vulkaanlichaam, boven Y 140), floodedness/erosion ring 3."

Mappenstructuur / classes (`…worldgen.densityfunction` tenzij anders):
- `LavaTubes.java` (nieuw, 42): buissystemen per cel van 400 (`trace`, `TubeSystem`, `Tube`, `Skylight`, `Chamber`), tegels van 128 (`tile`, `Tile`), kolombijdrage (`addTo`: opening, huid, schil, `subLava`, `deepGuard`). Puur, eenmaal per wereld getraceerd (`Decisions.tubeSystems`/`tubeTiles`).
- `FireGeodes.java` (nieuw, 42): plaatsing van de vuurkristal-geode per cel (`place`: kern, geen afgrond, geen kamer, grond erboven) en de holte per kolom (`cavity`, exact `inCavity`).
- `worldgen/feature/FireGeodeShape.java` (nieuw, 42): pure vorm (sjabloon `DewGeodeShape`): `CELL` 48, `CHANCE` 0,75, `ATTEMPTS` 4, straal 4–7, Y −50…20, `LINING` 3, `MARGIN`; `Geode` (`distance`, `inCavity`, `reach`), `candidate`. **Voor 41b:** de bekleding leest alleen deze vorm; de geode die het terrein nam: `VolcanicField.fireGeode(x, z)`. Het veld vind je met `VolcanicField.find(randomState.router().finalDensity())`.
- `VolcanicField.java`: 42 — `caves` (geode, dan buizen, daklichten, kamers per kolom in de kern), `caveFade`; `guard` ±1 inclusief de beschermbanden; `caveLift` (vulkaanlichaam, boven `CAVE_CEILING` 146, over `CAVE_FADE` 20 blokken tot `CAVE_LIFT` 2); `tubeSystem`, `fireGeodeCell`, `caveWall`, `fireGeodes`; accessor `fireGeode(x, z)`. `Column`: `MAX_VOIDS` 8, `addCave`/`caveWall` (huid/schil/korst, tot 3 onder het maaiveld), `addSubLava`/`subLavaAt`/`subLavaMin`/`Max` en `addDeepGuard`/`deepGuardAt`/`deepGuardMin`/`Max` (aparte bereiken, `MAX_BANDS` 6), `voidAt`, `voidMin`/`voidMax`, `guard(y)`, `caveLift(y)`.
- `VolcanicCaches.java`: `tubeSystems`, `tubeTiles`, `geodes`; `TubeTile`.
- `VolcanicOutput.java`: uitgangen `cave_walls` (kolom, 0/1), `cave_wall` (per blok: 0, 1 huid, 2 schil, 3 korst), `cave_lift` (per blok 0…2) en `fire_geodes` (per blok −1 waar de holte echt open is, anders +10).
- `Ring3Relief.java`: `lavaTubeAxis` (de as-waarde van de 39d-lavabuizen, voor het zoeken naar sleuven).
- `worldgen/chunk/RingAquifer.java`: 42 — in de openingen van buizen/kamers/geodes lucht of hun lava (korstlip waar open lucht ernaast/eronder ligt), rots in de beschermband (ook voor carvers); per kolom `caveBottom`/`caveTop`.

Databestanden:
- `density_function/ring_3/overworld/fire_geodes.json` (nieuw): `volcanic_output` `fire_geodes`.
- `density_function/ring_3/overworld/floodedness.json` (nieuw): vanilla-floodedness + gradiënt 0 → −2 (Y −24 → −8).
- `density_function/ring_3/overworld/erosion.json` (nieuw): 0 waar ring 3 gewicht 1 heeft (`range_choice` op een eigen `ring_blend`-gewicht), anders vanilla-erosion.
- `density_function/ring_3/overworld/cave_lift.json` (nieuw): `volcanic_output` `cave_lift`.
- `density_function/ring_3/overworld.json`: 42 — `cave_lift` opgeteld bij de grotdichtheid (bij de ingangen binnen `min(terrain, …)`, bij de diepe grotten en bij de noodles).
- `ring_blend/overworld_floodedness.json`: ring 3 → `ring_3/overworld/floodedness`. `ring_blend/overworld_erosion.json`: ring 3 → `ring_3/overworld/erosion` (geen `switch_at`).
- `noise_settings/overworld.json` (uit `ring3_surface.py`): bovenste regel 1 (na de bedrock-vloer) = de wanden van buizen en kamers.
- `tools/worldgen/ring3_surface.py`: `cave_walls()`, `is_cave_walls`; `main()` zoekt de regels boven de voorlopige oppervlakte op index 1 na het weghalen van de wandregel.

Tests: `Ring3CavesTest` (nieuw); bijgewerkt: `Ring3TerrainJsonTest` (floodedness/erosion, `fire_geodes`, surface-regel), `RingAquiferTest` (geode en kamermeer), **`Ring3BiomesTest`** (W-biomes: zoekt de regels boven de voorlopige oppervlakte nu op type in plaats van op index 1).

Valkuilen:
- **Surface-regels onder de voorlopige oppervlakte (42):** de ring 3-tak staat binnen `above_preliminary_surface`. Wat dieper ligt (buiswanden, kamers) krijgt alleen een regel op het hoogste niveau; zo'n regel loopt voor elk steenblok van elke ring, dus eerst `ring_reach`, dan een kolomtest.
- **Lava onder de grond (42):** het gootje zakt in treden; de aquifer maakt van elk lavablok met een droge opening ernaast of eronder rots (korstlip). Zonder die regel stroomt het bij de eerste update over de vloer.
- **Beschermband (`deepGuard`):** sluit vanilla-grotten (via `guard`) en carvers (aquifer `null`) uit rond kamers, gootjes en geodes. Banden en lavabereiken blijven per kolom apart (review 42): een vereniging maakte lava/rots-pilaren en dichte wanden over tientallen blokken.
- **Grotten dempen via `max`:** de stapel gebruikt `guard` alleen in `max(...)`, dus een waarde onder 0 doet niets en boven 0 sluit alles (een harde snede). Geleidelijk dempen gaat via `cave_lift`, opgeteld bij de grotdichtheid (nooit bij het terrein).

## Beslissingen
- 2026-10-09 | worldgen | Het lavagootje in de zijtak is een reeks poeltjes met korstlipjes (geen stromende lava); de zijtak eindigt in het meer van de magmakamer | lava nooit tegen open lucht, en de gloed waarschuwt voor de hitte; de kamer is altijd via de zijtak bereikbaar (G2-WT1/WT2 = A)
- 2026-10-09 | worldgen | Geen buismasker `ring_3/overworld/lava_tube`; gedeelde DF alleen `ring_3/overworld/fire_geodes` + accessor `VolcanicField.fireGeode(x, z)` en de pure vorm `FireGeodeShape` (contract §4, main `5a2fc01`) | C en A vragen er niet om
- 2026-10-09 | worldgen | Wanden van buizen en kamers via een eigen bovenste surface-regel (direct na de bedrock-vloer, `cave_wall` per blok): smooth basalt, basalt met blackstone-aders, korst bij lava | de wanden liggen onder de voorlopige oppervlakte, waar de ring 3-tak niet komt (akkoord main)
- 2026-10-09 | worldgen | Vanilla-grotten in ring 3 krimpen geleidelijk via `cave_lift` (over 20 blokken): weg boven Y 146 (krimpend vanaf 126), in een vulkaanlichaam weg vanaf 4 blokken boven de vlakte | geen rechte geulen in kraterranden en flanken, geen zwevende brokken onder de eilanden, geen vlakke grotplafonds (G2-WT3 = A; review 42)
- 2026-10-09 | worldgen | Erosion ring 3 = 0 alleen waar ring 3 het hele gewicht heeft (in de zones vanilla, geen `switch_at`), en eigen floodedness (geen lokale aquifervloeistof boven Y ~−8) | ring 3 heeft geen deep dark; water alleen diep (V8); de zones (ook de ring 4-helft van 3→4) blijven onveranderd en het contract (erosion mengt, 48 haalt ring 3 uit `switch_at` van het klimaat) blijft heel (review 42)

## Backlog
- `worldgen` | Lavavallen van eilanden (39f) worden niet tegen daklichten of buismonden getest. Een val die op een daklicht landt zou in de buis stromen; zeldzaam, nalopen in INT2.
- `worldgen` | Askorst-daken boven buizen komen alleen voor waar een mondhelling een dak van precies één blok laat, en alleen in de 39c-korstbiomes (niet basalt_flows). Bij teststop 4b beoordelen of er meer moeten komen (bijv. een stuk dun dak vóór elke mond).
- `worldgen` | Tweede monden openen niet in 39d-sleuven, alleen de startmond (17 in de ring). Bij teststop 4b beoordelen.
- `infra` | Het patroon "per-wereld tegel-/celcache" (sleutel `(long) i << 32 | j`, LIMIT-clear, `once`) staat op ~10 plekken in `VolcanicField`; een kleine gedeelde helper (review 42, punt 10).
