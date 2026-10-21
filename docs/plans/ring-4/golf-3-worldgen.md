# Ring 4 golf 3 — stroom W (`worldgen`): 74b, 75c, 77b

Branch `ring4/worldgen-g3` vanaf `aeebe14`. Plan: `golf-3.md`. Backlogregels verwijzen naar `docs/backlog.md` op
`aeebe14`. Wijzigt gegenereerde werelden (besluit gebruiker 2026-10-21: verse wereld voor teststop 5c).

## Gedaan

### 74b — eilandpunten (commit `worldgen: keep ring 4 decoration off the island debris …`)
- **r. 426** `IslandField.inDebris(x, z)` (puur, via `IslandLandmarks.inDebris` op de cel-cache) en
  `Ring4Terrain.onDebris`. `IsolatedTreeFeature` (dus ook `sky_tree`), `BoulderFeature`, `FallenTreeFeature` en
  `GroundPlantFeature` plaatsen niets als de oorsprongskolom een brok bevat. De check zit in de features, niet in
  `ON_GROUND`: die is een vanilla-`BlockPredicate` zonder eigen type, en zo dekt één check alle plaatsingen (ook de bomen,
  die niet via `ON_GROUND` lopen). `ground_plant` trekt eerst en kijkt daarna (de trekking blijft gelijk).
- **r. 428** `IslandDebris.groundUnder`: grondvrijte 8 tegen de hoogste `baseHeight` over midden en de vier hoeken van de
  box van de brok (maat 1: alleen het midden).
- **r. 429** `IslandArches.archKept` laat ook een boog vallen die een hoger gerangschikte, zelf gehouden kandidaat-boog
  kruist (`IslandArches.crosses`: een kolom met steen van beide binnen `KEEP_CLEAR` in hoogte). Recursie alleen omhoog in
  `RANK`, dus eindig en onafhankelijk van de vraagvolgorde. Op de testseed kruiste geen boog (25 bogen met en zonder de
  regel); de regel is met een synthetische test gedekt.
- **r. 427** Filter vóór `pairs()` in `IslandDebris.clear`: alleen eilanden waarvan een boog de box kan bereiken
  (`reach + ARCH_MAX_GAP + MAX_MARGIN + 2`), alleen kandidaat-bogen die de ruimte van de brok raken vragen `archKept`.
  Resultaat-identiek (gehouden bogen ⊆ kandidaten, zelfde `Arch`); de verspreiding van `archKept` bij een eerste bezoek
  wordt kleiner. Niet gemeten (geen vergelijkbare prestatiecijfers in de cloud).

### 75c — determinisme-keten (commit `worldgen: make the ring 4 ores, reeds, vines, swamp trees and rocks order-independent …`)
- **r. 420** Gekozen: **geen ertsen in eilandblokken**. De ring 4-kopieën van de vanilla-ertsen krijgen
  `ring4_filter` met `island_clearance` 6 (`Ring4OreFeatures.ORE_ISLAND_CLEARANCE`, via een `extra`-parameter van
  `RingVanillaOres.placed`): een oorsprong waarvan de blob (max. 5 opzij, 4 omhoog/omlaag; kolen grootte 17) een blok
  van een eiland, boog of brok kan raken, vervalt (`IslandField.islandBlockNear`, puur; eilanden met hun hele omtrek,
  dus een superset). De andere optie (aankleding ook op ertsblokken) is niet volledig deterministisch: ertsen met
  `discard_chance_on_air_exposure` (kolen-laag 0,5, tot Y 192) kijken of de buren lucht zijn, en een hangend blok van
  `island_dressing`/`island_underside` onder het erts verandert dat per volgorde. Eigen ring 4-ertsen liggen ≤ Y 48.
- **r. 367-rest**
  - *Riet:* `ReedClumpFeature` trok al altijd eerst; de volgorde-afhankelijkheid zat in de plaatsing: `waterNear`
    (levende blokken) liet een plaatsing wegvallen als een boom uit de buurchunk in het water stond, en dan schoven de
    trekkingen van alle volgende pollen van die placed feature. Nu `ring4_filter` `water_near` op de bevroren
    WG-heightmaps (water = tussen `OCEAN_FLOOR_WG` en `WORLD_SURFACE_WG`) voor `cascade_vale_reeds`,
    `lotus_shallows_reeds`, `lotus_shallows_reed_collars`, `blighted_bank_reeds`.
  - *`island_vines`:* gebruikt geen chunk-random (hashes). De rank (mangrovewortels) telt voor de bomen als `ROOT`;
    "alleen open lucht" zou juist volgorde-afhankelijk zijn (blad eerst: geen rank; rank eerst: blad verliest). Nu
    `IslandVinesFeature.takes`: lucht, of een boomblok dat de rank wint volgens `TreeOverlap.wins` (precies wat een
    later komende boom laat staan), en `EdgeUpdates` op een genomen plek, zodat wat aan het genomen blad hing
    (propagule, rank) valt zoals wanneer de boom later komt. Strengen/accenten onder de rank: alleen lucht (ongewijzigd).
  - *Reuzenleliebladen:* geen wijziging nodig: `GiantLilyPadFeature` trekt niets uit de random, vult alleen lucht, en
    een later komend boomblok verwijdert het hele blad via `EdgeUpdates` (deel weg → rest volgt) — hetzelfde eind als
    boom eerst.
- **r. 415** `glowroot_swamp_trees`: `surface_water_depth_filter` (levende heightmaps) vervangen door `ring4_filter`
  `max_water_depth` 4 op de bevroren WG-heightmaps.
- **r. 421** Oorzaak gevonden en gedicht (in onze code): headless server, seed 20260930, gebied 6144..6239 × −128..−17.
  Op 6188 59 −68 legt een zeebodemkei (`lagoon_seabed_rock`, submerged) steen op de bodem; `EdgeUpdates` gaf het zand
  eronder een vormupdate, die bij zand alleen een val-tick plant. Onder dat zand lag een open waterholte; het zand viel
  erin en liet lucht achter naast water. Fix: `EdgeUpdates` slaat `FallingBlock`-buren over (terrein blijft liggen).
  Na de fix (verse wereld): het zand ligt op 58–59, geen lucht onder zee meer in het gebied (alleen 6229 4 −37..−34,
  diep, ander geval, zie Backlog).

### 77b — inzet nieuwe blokken (merge `claude/admiring-cray-2ywybu` + commit `worldgen: lay lagoon sand …`)
- Merge van de integratiebranch (stroom I, B/T, 66c/66d/66e/71) zonder conflicten; `runData` daarna alleen met de
  77b-wijziging.
- `lagoon_sand` i.p.v. vanilla zand in `crystal_lagoon`: de surface (`tools/worldgen/ring4_surface.py` `SURFACES`, land en
  bodem, boven en eronder → `noise_settings/overworld.json`; met `sandstone` als vaste plaatsvervanger waar het aan een
  plafond zou hangen, zoals vanilla zand: `FALLING`) en de zandbanken/atollen van `lagoon_shoal`
  (`CrystalLagoonFeatures`). `Ring4BiomesTest` eist ook voor `lagoon_sand` de plafondwacht.
- `TunnelLiningFeature`: klauwsporen in de variant van het gesteente eromheen (deepslate → `clawed_deepslate`, tidestone
  → `clawed_tidestone`, overig gesteente → `clawed_stone`; dezelfde gesteentetoets als de ertsen van de bekleding).
  `rock()` telt `clawed_stone`/`clawed_deepslate` expliciet als gesteente (bewust in geen van beide steentags, besluit
  stroom B/coördinator), zodat een vloer met klauwsporen een spoor blijft dragen.

## Tests
- `Ring4IslandsTest`: bodem ≥ hoogste grond onder de voetafdruk + 15 (`IslandPlacement.highestGround`, dezelfde 9
  monsters als de plaatsing) en in de binnenste helft ≤ die grond + 60 (gestapeld: ≥ onderste eiland + `STACK_GAP`
  toegestaan; satellietkluit: tot de top van het moedereiland + 6) (r. 430b); geen val en geen lift boven een
  veenzinkgat (`inSinkhole`) (r. 435).
- `Ring4LandmarksTest`: geen kruisende gehouden bogen; `archesCrossWhereTheirRockMeets` (synthetisch); brokvrijte over
  de hoeken; `inDebris` op elke kolom van elke brok; `islandBlockNearSeesEveryIslandBlock` (6.397 proeven, 2.460 naast
  een eilandblok, niets ver onder een eiland).
- `IsolatedTreeTest.islandVinesTakeWhatALaterTreeLeavesThem`: `takes` ⇔ `!TreeOverlap.wins` andersom.
- `Ring4MythicFeaturesTest.frozenWaterLiesBetweenTheFloorAndTheFirstOpenBlock`.
- `bash ./gradlew build --no-daemon` groen; `runData` gedraaid (placed features ring 4: ertsen, riet, mangrove).
- r. 421 headless: vóór/na-vergelijking met `tools/test/py/region.py` (zie Gedaan).
- 77b: `TunnelLiningFeatureTest.clawMarksFollowTheirRock`, `Ring4BiomesTest` (plafondwacht `lagoon_sand`); `runData` en
  `build` groen na de merge.

## CODEBASE-delta
- `IslandField`: `inDebris(x, z)`, `islandBlockNear(x, y, z, reach)` (+ constanten `ISLAND_ROCK_*`).
- `IslandLandmarks`: `inDebris`, `rockWithin` (bogen en brokken van de cel).
- `IslandArches`: `MAX_MARGIN`, `crosses`; `archKept` ook tegen kruisende bogen.
- `IslandDebris`: `groundUnder`, `meets`; boogcheck via `candidatePairs` + `archKept`.
- `IslandPlacement.highestGround` (uit `shape` gehaald, resultaat gelijk).
- `Ring4Terrain.onDebris`; checks in `IsolatedTreeFeature`, `BoulderFeature`, `FallenTreeFeature`, `GroundPlantFeature`.
- `Ring4Filter`: velden `island_clearance`, `water_near`, `max_water_depth` (bevroren WG-heightmaps); builder
  `clearOfIslands`, `waterNear`, `maxWaterDepth`; `water(y, floor, surface)`.
- `RingVanillaOres.placed(…, PlacementModifier... extra)`, `tapered(…, extra)`; `Ring4OreFeatures.ORE_ISLAND_CLEARANCE`.
- `IslandVinesFeature.takes(rope, existing, below)` (+ `EdgeUpdates` waar genomen).
- `EdgeUpdates`: slaat `FallingBlock`-buren over.
- 77b: `TunnelLiningFeature.liningState` (pakket-zichtbaar) kiest de klauwvariant; `rock()` met de twee klauwblokken;
  `CrystalLagoonFeatures` shoal op `lagoon_sand`; `ring4_surface.py` `crystal_lagoon` = `lagoon_sand`, `FALLING` +
  `lagoon_sand → sandstone`; `noise_settings/overworld.json` (4 blokken); `configured_feature/ring_4/lagoon_shoal.json`.
- Gegenereerd: `placed_feature/ring_4/ore_*.json` (+ `ring4_filter`), `*_reeds.json`/`*_reed_collars.json`,
  `glowroot_swamp_trees.json`.

## Beslissingen (implementatie)
- r. 426 in de features i.p.v. een nieuw block-predicate-type (zie Gedaan); 2D-check op de oorsprongskolom.
- r. 429 "kruisen" = steen van beide bogen in één kolom binnen `KEEP_CLEAR` (2) in hoogte, dezelfde ruimte die een boog
  boven en onder andere eilanden houdt.
- r. 420 optie "geen ertsen in eilandblokken" (reden: zie Gedaan). Gevolg: geen vanilla-erts meer in de zwevende
  eilanden van ring 4 (en ook niet binnen 6 blokken ervan).
- r. 367 riet en r. 415 via bevroren heightmaps (zelfde regel als het terrein van de bomen); leliebladen ongewijzigd.
- r. 421 `EdgeUpdates` raakt zand/grind niet meer aan (geldt voor bomen en grondobjecten van ring 4).
- 77b plaatsvervanger van `lagoon_sand` onder een plafond: vanilla `sandstone` (er is geen lagunezandsteen); overig
  gesteente dan stone/deepslate/tidestone (graniet, andesiet, dioriet, tuf) krijgt `clawed_stone`.

## Backlog (nieuw)
- [worldgen] `drowned_forest`-bomen (`Ring4BlightedFeatures`, `SurfaceWaterDepthFilter.forMaxDepth(DROWNED_WATER)`) en
  `withered_gardens` (`WitheredExpanseFeatures`, `forMaxDepth(0)`) lezen nog de levende heightmaps zoals r. 415; zelfde fix
  kan (`ring4_filter` `max_water_depth`). Buiten de scope van 75c gelaten.
- [worldgen] Overige `waterNear`-plaatsingen (oeverkristallen/-rotsen `crystal_lagoon`, suikerriet `lotus_shallows`,
  `verdant_falls`-oevers) lezen levende blokken; alleen relevant waar een boom in het water staat.
- [worldgen] `island_vines`: een streng van de rank tegen een streng van een boom (beide rang `GROUND`) kan per volgorde
  verschillen (zeer zeldzaam: boomstrengen op randhoogte van een eiland).
- [worldgen] r. 427: de kruisingscheck van `archKept` (r. 429) vraagt `candidatePairs` van eilanden tot ~150 blokken om
  een boog; bij de prestatiemeting van 5c (lokaal) `perf-islands.txt` bekijken.
- [worldgen] Lucht onder zee op 6229 4 −37..−34 (seed 20260930, vier blokken op Y 4): niet onderzocht (diep, waarschijnlijk
  tunnelnet/grot), bestond al vóór 75c.

## Te checken bij INT
- Det-check (wereld b omgekeerd geladen) op `tools/test/areas/islands.txt`, `decor-c.txt`, `trees.txt` en
  `perf-islands.txt`: geen ertswissels meer in eilandonderkanten, geen verschil in mos/strengen/knoppen eronder.
- Riet en mangroves: `glowroot_swamp` west rond −6975 284 (gebied `archw`), `lotus_shallows`/`cascade_vale`-oevers.
- Waterscan op 6144..6239 × −128..−17 (6188 59 −68 moet schoon zijn) en de andere `crystal_lagoon`-gebieden.
- Brokken: geen boom/kei/stam/plant op een brok (bijv. rond reuzeneilanden in `islands.txt`).
- 77b: `crystal_lagoon` (bijv. 6144..6239 × −128..−17 en de lagunegebieden van `decor.txt`): `lagoon_sand` op oevers,
  bodem en zandbanken, geen vanilla zand meer; sandstone alleen onder plafonds. Tunnelnet (`tools/test/areas` met de
  tunnels van 73): klauwsporen in stone/deepslate/tidestone volgens de wand, sporen op klauwvloeren; det-check op de
  tunnelgebieden.

## Review-fixes (`/code-review` op `aeebe14..d77a385`)
Eén commit `worldgen: fix golf 3 review findings (…)`. Alles blijft puur (seed + positie, bevroren WG-heightmaps); geen
levende blokken erbij.

1. **`Ring4Terrain.onDebris` 2D → Y-bewust — echt, gefixt.** Nu `onDebris(level, x, y, z)` met `y` = het laagste blok
   van de feature (boven zijn grond): telt alleen een brok met een blok op of boven `y − 1` (`IslandField.inDebris(x, z,
   fromY)`, `IslandLandmarks.inDebris(x, z, fromY)`). Een brok onder een hoger eilandtop telt niet meer (eilanden houden
   `KEEP_CLEAR_Y` 3 vrij, dus die top is nooit van de brok); op de brok staan en eronder (een boom die erin groeit) blijft
   geweigerd zoals in 74b. `y`: boom = oorsprong, `ground_plant` = de plantplek, kei/stam = `foot` uit de WG-heightmap
   (de kei kijkt nu ná `ground`, dat niets trekt). Test: `Ring4LandmarksTest.debrisFloatsFreeRoundTheBigIslands` (op de
   brok wel, op een eilandtop erboven niet; aantal kolommen onder een eilandtop wordt geprint).
2. **`TunnelLiningFeature` klauwhost — latent, toch gefixt.** In worldgen niet bereikbaar: elk blok wordt één keer
   bekleed (alleen de eigen chunk, één doorgang), klauwen komen alleen in zijwanden en een spoorvloer is nooit een
   zijwand, dus een `clawed_*`-host komt niet voor. Wel bij een tweede doorgang (`/place`, latere wijziging): daarom
   `liningState` telt `clawed_deepslate` als deepslate (één regel; `clawed_tidestone` zat al in `ring_4_stones`). `rock()`
   ongewijzigd (77b-intentie). Test: `TunnelLiningFeatureTest.clawMarksFollowTheirRock` (+ klauw-op-klauw, tidesilver in
   `clawed_deepslate` → deepslate-variant).
3. **`Ring4Filter` `water_near` onder een eiland — echt (deels), gefixt zonder levende reads.** De rietoorsprong zelf ligt
   via `WORLD_SURFACE_WG` altijd op de bovenste top (onder een overhang kan dus nooit riet staan, ook vóór 75c niet via
   deze placement); wat wegviel is een open oever naast water dat onder een overhang ligt. Nu: zegt de heightmap "bedekt
   op `y`" (`WORLD_SURFACE_WG > y`, geen water), dan beslist het pure terrein: `IslandField.Column.underIsland(y)` (een
   eiland-, boog- of brokblok boven `y`, geen op `y`) en `Ring4Filter.waterUnderIsland(y, surface, lakeTop, flooded)` =
   open boven de grond (`ceil(surface)`, zoals `onGround`) en onder ring 4's vlakke zee (`RingFluidPicker.ring4Flooded`
   (nu public) → t/m `Ring4Tunnels.SEA_TOP` 62) of in een meer boven zee (`lakeTop`). Ring via `RingColumnCache.ringAt`.
   Bewust niet gezien: water dat features plaatsen (zou volgorde-afhankelijk zijn), grotten/carvers onder het eiland en
   een wereldboom boven de oever (die zit niet in de eilandintervallen). Tests:
   `Ring4MythicFeaturesTest.waterUnderAnIslandFollowsTheTerrain`, `Ring4LandmarksTest` (`underIsland` onder/in een brok).
4. **Boogvenster `IslandDebris` vs `archKept` — in effect niet echt, wel gelijkgetrokken.** `shapesWithin` neemt een cel
   (128) extra rondom, dus 150 i.p.v. 153,8 miste nooit een eiland; resultaat-identiek. Nu één bound:
   `IslandArches.archReach(island)` (reach + `ARCH_MAX_GAP` + `MAX_MARGIN` + 2) en `IslandArches.MAX_ARCH_WINDOW` (154)
   in `archKept`, `IslandDebris.clear` en `IslandLandmarks.cellOf` (dat had ook `MAX_REACH + MAX_GAP`). Test:
   `Ring4LandmarksTest.islandPairsAndArches` (elke kandidaat-boog binnen `archReach` van zijn eerste eiland, ≤ venster).
5. **`EdgeUpdates` + `BrushableBlock` — echt, gefixt.** Verdacht zand/grind (structures) plant bij `updateShape` ook een
   val; nu overgeslagen zoals `FallingBlock`. Geen unit-test (vraagt een level).
6. **`islandBlockNear` — echt, gefixt.** `ISLAND_ROCK_BELOW` nog maar één keer: ondergrens `min(lowest, top − 30)`
   (brokken zakken tot ~28 onder de rafelrand: drop ≤ 1,1·(5 + 16) + 4; boogsteen ~8 onder de randen). Goedkoper,
   zelfde antwoord: de kolomscan alleen voor eilanden waarvan de voetafdruk (reach) de box raakt, en bogen/brokken via
   `IslandLandmarks.rockWithin(box, lo, hi)` (cellen van de box één keer, alleen bogen/brokken waarvan de box de box
   raakt, dan de kolommen) i.p.v. een cel-lookup per kolom. Het smallere venster verandert alleen wat de oude te ruime
   poort extra toeliet; `islandBlockNearSeesEveryIslandBlock` blijft groen. Ertsplaatsing kan daardoor licht anders
   uitvallen (verse wereld voor 5c staat al vast).
7. **Javadoc `ORE_ISLAND_CLEARANCE` — gefixt** (vóór `IN_RING_4`, die zijn eigen Javadoc terugkrijgt).
8. **`IslandArches.archKept`-kosten — niet gewijzigd** (opdracht): meten bij teststop 5c lokaal (`perf-islands.txt`,
   `perf-r4.txt`; JFR `jfr-prof.sh` op `archKept`/`candidatePairs`).

### Tests (review-fixes)
- `bash ./gradlew runData --no-daemon` (geen gegenereerde wijziging) en `bash ./gradlew build --no-daemon` groen.
- Testseed: 1.338 brokken rond 312 eilanden, 213 brokkolommen onder een eilandtop (die tellen nu niet meer);
  `islandBlockNear` 6.397 proeven, 2.460 naast een eilandblok (gelijk aan vóór de fix).

### CODEBASE-delta (review-fixes)
- `IslandField.inDebris(x, z, fromY)`; `IslandField.Column.underIsland(y)`; `islandBlockNear` met voetafdruk-voorfilter.
- `IslandLandmarks.inDebris(x, z, fromY)`; `rockWithin(minX, minZ, maxX, maxZ, lo, hi)` (box) i.p.v. de kolomversie.
- `IslandArches.archReach`, `MAX_ARCH_WINDOW` (gebruikt door `IslandDebris`, `IslandLandmarks.cellOf`).
- `Ring4Terrain.onDebris(level, x, y, z)`; `Ring4Filter.waterUnderIsland`; `RingFluidPicker.ring4Flooded` public.
- `EdgeUpdates` slaat ook `BrushableBlock` over; `TunnelLiningFeature.liningState` kent `clawed_deepslate`.

### Opnieuw checken op determinisme (INT / 5c)
- Riet (`cascade_vale`, `lotus_shallows`, `blighted_bank`) naast water onder een eilandoverhang: staat er nu riet, en
  gelijk in wereld a en b (omgekeerd).
- Brokgebieden (`islands.txt`): bomen/planten op eilandtoppen boven een brok weer aanwezig, nog steeds niets op een brok;
  det-check.
- Ertsen rond eilandonderkanten (`islands.txt`, `perf-islands.txt`): det-check (het venster van `islandBlockNear` is
  smaller).
