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

## 7. W-checklist (in-game, verse wereld seed `20260930`)
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

## Voortgang

## CODEBASE-delta

## Beslissingen

## Backlog
