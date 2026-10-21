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
