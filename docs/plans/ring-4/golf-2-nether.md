# Ring 4 golf 2 — stroom W-nether: 78 (Nether-4: fossielen als terrein + aankleding)

Scope: `worldgen` — werkpakket 78; branch `ring4/nether-g2` (vanaf `main` c5b7d11), poort n = 3. Bron: `golf-2.md` §2 (W-nether), Q15 = B, G5 = A; `golf-1-nether.md` (65); backlog r. 283 (Nether-deel), r. 284.

## Gebouwd
1. **Zeewezen-fossielen als terrein** (Q15 = B, G5 = A): nieuw DF-type `corruptedadventure:sea_fossils` (`SeaFossils`, geometrie `SeaFossilShape`), bestand `density_function/ring_4/nether/fossils.json`, ruis `nether_ring_4_fossil_cells` (alleen hash). `terrain` = `range_choice(hollows_weight > 0: min(interpolated_shape, geodes); anders range_choice(|region| < 0,14: fossils; anders interpolated_shape))`: `fossils` wikkelt `interpolated_shape` buiten de interpolatie, alleen in de duinband (de spitsen betalen niets; één interpolator blijft).
   - **Vorm:** een op de buik liggend, half begraven zeewezen langs een rechte ruggengraat: ribbenkast 40–56 lang (ribben dwars op de graat, elliptische bogen 16–24 hoog en 22–32 breed, naar beide uiteinden ~55 %, 2,5 dik, 3 langs de graat, periode 8–10 → **gangen 5–7 breed, ≥ 4 gegarandeerd** door de codec), ruggengraat met wervels (Ø ~3,6) en doornuitsteeksels erop, nek 6–10 schuin omlaag naar een **holle schedel** (18–24 lang, 14–20 breed, 10–14 hoog, schaal 2 dik, open achter en bij de bek, twee oogkassen, kaken die 25 % voorbij de snuit steken, tanden op het voorste deel), staart 24–40 die in de duinen wegzakt en opzij zwaait. Totaal **92–136 lang**. Ribben lopen 6 onder de vloer door (half begraven); 8 % ontbreekt (nooit twee na elkaar), 20 % is aan één kant afgebroken (hangt van de graat tot de breuk).
   - **Binnenkant hol en vlak:** in de ribbenkast, de nek en de schedel een vlakke vloer op `floor` (afgerond gemiddelde van de duinvloer onder de voetafdruk), dicht tot 12 eronder, open erboven; de duinen hellen er over 8 blokken naartoe (smoothstep, stap ≤ 1, getest door elke ribbengang heen).
   - **Plaatsing:** cel 256, hoogstens één fossiel per cel, tot 128 pogingen (positie, richting, maten) tot alles (vloer + helling + kaken + een vierkant rond elke wervel, 5 breder voor de gecachte regio van de noise chunk) op een raster van 2 in `|region| ≤ 0,13` ligt (de spitsen beginnen bij 0,14, de holtes bij −0,14: **alleen `bone_dunes`, nooit boven de lavazee of in een geode**) en `edge` = 0 (≥ 48 binnen de ring: **niet in de zones** (halve zone 32) en **niet op de dammen**). Het hele fossiel met helling ligt binnen zijn cel. Gemeten (4 seeds, ruis zoals in de wereld): **0,75 per 256 × 256 bottenduin** (≈ 1 per 296², ~21 per wereld in Nether-4; meer pogingen of een kleinere cel helpen niet meer: de duinband is ~50–60 breed en kronkelt). Hoogtewacht: alles wat een fossiel verandert ligt in Y 24–110 (anders geen fossiel; de helling wordt erin geklemd).
   - **Puur en thread-safe:** celhash uit de geseede ruis, plaatsing uit `region`/`edge`/`floor_height` op vaste punten (`mapAll` bedraadt alleen de ruis, zoals `GiantGeodes`); één beslissing per cel per wereld in een `ConcurrentHashMap` die de kopieën van de noise chunk delen; per thread een klein celgeheugen (`ThreadOwned`) en een kolomtabel (`ColumnTable`).
2. **`bone_block` via een surface-conditie:** `corruptedadventure:sea_fossil` (`SeaFossilCondition`), de eerste regel van de Nether-4-tak (vóór de vloeren): waar het fossiel bot heeft, ook in de vloer (half begraven). Leest het DF uit de router van de random state (`NetherRing4Terrain.fossils`), dus dezelfde cellen als het terrein; per kolom één botmasker (64 bits), per blok één bittest.
3. **Echte blokken in de surface** (N1 = A, backlog r. 283 Nether-deel): `nether_ring4_surface.py` gebruikt `bone_sand`, `bleached_netherrack`, `fossil_stone` (bruggen, richels, band in de zeewand) en `purity_crystal_block` (zuilen, huid en kern één materiaal; de kwartskern vervalt). Lavabed blijft `basalt`/`magma_block`.
4. **Geodebekleding** (N3 = A, backlog r. 284): feature `corruptedadventure:giant_geode_lining` (`GiantGeodeLiningFeature`), configured/placed `nether_ring_4/geode_lining(s)`: vindt de geode via `GiantGeodes.find(root)` + `geodeAt(x, z)` (dezelfde cellenkaart als het terrein); van de holte uit `purity_crystal_block`, `calcite`, `bleached_netherrack` (methode en `layers` van `FireGeodeFeature`), lampen (`purity_crystal_lamp`, 2 %) in de binnenlaag van de koepel, clusters (`purity_crystal_cluster`, 25 % van de open blokken tegen de wand). **Vloer en deurvloeren blijven bottenzand**: niets onder de geodevloer wordt bekleed, dus geen cluster op de vloer; deuren alleen bekleed tot 1,5 voorbij de koepelwand.
5. **Bekleding van de kristalzuilen** (N2 = A): feature `corruptedadventure:pillar_cladding` (`PillarCladdingFeature`), `nether_ring_4/pillar_cladding(s)`: clusters (18 %) aan de zijkanten en onder de kop (nooit bovenop, nooit in de 2 blokken boven een looppad of vloer: richels en bruggen blijven vrij), lampen (1 op 150 blootliggende huidblokken). Chunks zonder zuilen (regio < 0,16 op een raster van 4, of geen huid op de peilhoogtes Y 60/100) slaan het lezen over; clusters alleen met open lucht eronder.
6. **Botten-aankleding** (N4 = A): feature `corruptedadventure:dune_rubble` (`DuneRubbleFeature`, vorm `RubbleShape`, grond door omlaag te scannen i.p.v. heightmap: de Nether heeft een plafond): `nether_ring_4/dune_bones` (losse `bone_block`, liggend, 1–2, ~1 per 2 chunks) en `nether_ring_4/dune_boulders` (`fossil_stone`-keien 2–3, ~1 per 6 chunks), alleen in `bone_dunes`, alleen op bottenzand/gebleekt netherrack (nooit op looppaden of botten), ≥ 3 vrij van elk fossiel (bot, vloer, helling). De `bone_dust`-particle blijft van A (80).
7. **Biomes:** `NetherRing4Biomes.generation` met `NetherRing4Features.features(biome, step)`: `UNDERGROUND_DECORATION` [geode_linings, pillar_claddings] in alle drie (een geode of zuil kan een chunk van een ander biome raken; zelfde volgorde overal), `LOCAL_MODIFICATIONS` [dune_bones, dune_boulders] alleen in `bone_dunes`. Nog steeds geen carvers.

## Client/server-impact
Geen client-code; alles worldgen (server-side), puur en deterministisch. Geen payload, geen `PROTOCOL_VERSION`.

## Determinisme
- Terrein: `sea_fossils` puur van de seed (hash), `region`, `edge`, `floor_height` en de positie; één beslissing per cel per wereld (gedeelde kaart).
- Features: per chunk, eigen chunk schrijven; de bekleding leest 3 rond de chunk (eigen blokken tellen als rots, clusters als open, zoals 41b), de cladding 1 rond (lamp = huid, cluster = open), de rubble alleen de eigen chunk. Vaste volgorde van random-trekkingen (één per blok, gebruikt of niet).

## Tests en metingen
- **B** groen (`./gradlew build --no-daemon`), **D** groen (`runData`; nieuw in `src/generated`: 3 biome-JSON's met features, `configured_feature`/`placed_feature/nether_ring_4/*`).
- Nieuwe tests: `SeaFossilShapeTest` (binnen de cel, ≥ 80 lang, ribbengangen ≥ 4 op het blokraster, binnenkant vlak en 3 hoog open, half begraven, alleen in de duinen en buiten de zones op 1-blokresolutie over 3 seeds, tempo, determinisme), `SeaFossilsTest` (vlakke vloer, helling met stappen ≤ 1 en 2 hoog vrij door elke ribbengang, elders het terrein, gedeelde cellen tussen kopieën, `find(root)`, kostenschatting), `GiantGeodeLiningFeatureTest` (vloer onbekleed, drie lagen over de koepel, geen cluster op de vloer, deur voorbij de mond niet bekleed), `DuneRubbleFeatureTest` (vrij van fossielen); aangepast: `NetherRing4JsonTest` (één interpolator met `fossils`, regio-band van de fossielen binnen de duinen, botregel eerst, kristal = `purity_crystal_block`, `COLUMNS_REGION_FROM`), `NetherRing4BiomesTest` (features per stap).
- **Kostenschatting** (`SeaFossilsTest.costEstimate`, microbenchmark, alleen indicatief): per blok extra ~9–17 ns buiten een fossiel (alleen duinkolommen met |regio| < 0,14 en Y 24–110; spitsen en holtes niets), ~20 ns in een fossiel; fossielen beslaan ~7,5 % van hun cel (≈ 3 % van de duinen). Cladding: twee peillagen (2 × 324 blokken) per chunk met regio ≥ 0,16, de volle lezing (18 × 18 × 123) alleen bij zuilhuid. Celbeslissing één keer per cel per wereld (een mislukte poging stopt bij het eerste punt buiten de duinen; niet apart gemeten; de tempotest beslist ~4 × 256 cellen in < 10 s inclusief de bemonstering).
- Geen perf-, determinisme-, ruimte- of in-game-runs (INT, regel gebruiker).

## Wat INT moet controleren
- **Prestatie** (73, 74, 78 raken de noise): meetpunten Nether-4 **duinen met een fossiel**, duinen zonder, spitsen (cladding), holtes (bekleding); ≤ 1,75×. Valt het tegen: de goedkopere vroege uitstap is een `flat_cache`-masker (2D) met `range_choice` in `terrain.json`.
- **Ruimte-check van 65** opnieuw met de fossielen: beloopbaarheid duinen (de helling naar de fossielvloer, stappen ≤ 1), plafond (fossieltop ≤ vloer + ~32, plafond 104–120), aandeel per biome ongewijzigd (fossielen veranderen geen biome).
- **Dammen** 3|4 en 4|5: `tools/nether-lava-walls.py` r 1.500 en 2.000 (fossielen liggen ≥ 48 binnen de ring; verwacht 0 wanden).
- Determinisme omgekeerd geladen in Nether-4 (fossielen, bekleding, cladding, rubble).
- In-game: `tpring 4 nether`, een fossiel opzoeken (de cellen staan in `SeaFossils.fossilOfCell`; ~24 per wereld), door een ribbengang lopen, schedel in; een geode (bekleding, geen clusters op de vloer); een zuil (clusters niet op richels/bruggen).

## CODEBASE-delta
- **Klassen:**
  - `worldgen/densityfunction/SeaFossilShape` ✅ 78: pure geometrie van de zeewezen-fossielen (`Settings` met `Span`-maten, `Fossil` met `touches`, `boneMask` (64-bits kolommasker vanaf `floor − 8`), `floorHalfWidth`, `floorDistance`, `floorWeight`; `fossilOfCell` (96 pogingen, `fits` op vloer/helling/kaken/wervels), `candidate`); gedeeld door DF en surface-conditie (en later de fossiele tempels van fase 3).
  - `worldgen/densityfunction/SeaFossils` ✅ DF-type `corruptedadventure:sea_fossils`: wikkelt het duinterrein; +1 in bot en onder de vloer, −1 boven de vloer tot de duinvloer + 3, anders het terrein; Y-wacht 24–110; `fossilAt`, `fossilOfCell` (gedeelde kaart), `regionAt`, `find(root)`, `SURFACE_OFFSET` 0,4.
  - `worldgen/surface/SeaFossilCondition` ✅ surface-conditie `corruptedadventure:sea_fossil` (geen velden).
  - `worldgen/feature/NetherRing4Terrain` ✅ `geodes(level|randomState)`, `fossils(...)`: de bedrade `GiantGeodes`/`SeaFossils` per random state (zoals `Ring3Terrain`).
  - `worldgen/feature/GiantGeodeLiningFeature` + `GiantGeodeLiningConfiguration` ✅ `giant_geode_lining`.
  - `worldgen/feature/PillarCladdingFeature` + `PillarCladdingConfiguration` ✅ `pillar_cladding`.
  - `worldgen/feature/DuneRubbleFeature` ✅ `dune_rubble` (`RubbleConfiguration`).
  - `worldgen/densityfunction/GiantGeodes` gewijzigd: `geodeAt(x, z)` (gedeelde cellen), `find(root)`.
  - `worldgen/feature/NetherRing4Features` gevuld: keys, `configured`/`placed`, `features(biome, step)`, `COLUMNS_REGION_FROM` 0,20, `RUBBLE_Y` 47.
  - `worldgen/biome/NetherRing4Biomes`: `generation(...)` met de features van 78.
  - `registry/ModDensityFunctionTypes` (`sea_fossils`), `ModSurfaceConditions` (`sea_fossil`), `ModFeatures` (`giant_geode_lining`, `pillar_cladding`, `dune_rubble`): blok `// ring 4 — W-nether`.
- **Databestanden:** `density_function/ring_4/nether/fossils.json` (nieuw), `terrain.json` (buiten de holtes `fossils`); `noise/nether_ring_4_fossil_cells.json`; `noise_settings/nether.json` (Nether-4-tak: botregel eerst, echte blokken, geen kwartskern); `configured_feature`/`placed_feature/nether_ring_4/{geode_lining(s), pillar_cladding(s), bone_rubble → dune_bones, fossil_boulder → dune_boulders}` (generated); de drie Nether-4-biomes (generated).
- **Scripts:** `nether_ring4_terrain.py` (`FOSSILS`, `fossils.json`, ruis), `nether_ring4_surface.py` (echte blokken, `sea_fossil`).
- **Tests:** zie "Tests en metingen".
- **Valkuilen:**
  - `fits` test de werkelijke voetafdruk (vloer + helling + wervels), niet de rechthoek: met de rechthoek paste maar 0,18 per 256² duin (de duinband is ~50–60 breed en kronkelt).
  - de vloer van de duinen ligt op `floor_height − 0,4` (`below` /8 en ruwheid −0,05): `SeaFossils.SURFACE_OFFSET`; verandert de duinformule, dan ook dit.
  - de Nether heeft een plafond: features op de vloer niet via `WORLD_SURFACE_WG` (die geeft het plafond), maar via een vaste Y en omlaag scannen.

## Beslissingen
- 2026-10-16 · worldgen · N1 = A: de echte Nether-4-blokken (68) in de surface nu (bottenzand, gebleekt netherrack, fossielsteen, zuiverkristal); zuilkern ook zuiverkristal (één materiaal) (gebruiker).
- 2026-10-16 · worldgen · N2 = A: bekleding kristalzuilen = clusters op zijkanten en onder de kop (~15–20 %), niet op richels/bruggen, plus lampen in de huid (~1 op 150) (gebruiker).
- 2026-10-16 · worldgen · N3 = A: geodebekleding zuiverkristal → calciet → gebleekt netherrack, clusters ~25 % op wand en koepel, enkele lampen in de koepel, vloer en deurvloeren bottenzand zonder clusters (gebruiker).
- 2026-10-16 · worldgen · N4 = A: `bone_dust`-aankleding = losse botten en fossielsteen-keien in `bone_dunes` (niet op fossielvloeren of looppaden); de `bone_dust`-particle blijft bij A (80) (gebruiker).
- 2026-10-16 · worldgen · Dichtheid: cel 256, ~1 per 256² bottenduin (niet per 256² ring) (implementatie, akkoord gebruiker). Gemeten 0,75 (de duinband laat niet meer toe, zie Backlog).
- (implementatie) maten zie Gebouwd 1; regio-band ±0,13 op een raster van 2 (+5 voor de quart-cache), 128 pogingen; helling 8; schedel 10–14 hoog (lager is van binnen niet beloopbaar); afgebroken ribben hangen van de graat (geen zwevende stukken); de hele vorm in één DF-type (geen aparte vloer-DF) zodat het buiten de interpolatie exact is; bekleding/cladding in alle drie de biomes, rubble alleen in de duinen.

## Backlog
- `structures` (fase 3): de fossiele tempels in de ribbenkasten kunnen `NetherRing4Terrain.fossils(level).fossilAt(x, z)` gebruiken (vloer `floor`, `floorHalfWidth(s)`, richting `cos`/`sin`): de vlakke binnenvloer ligt vast.
- `worldgen` (INT, prestatie): blijkt de vroege uitstap van `sea_fossils` (celgeheugen + rechthoek, duinband, Y 24–110) te duur, dan een fijner 2D-masker (`flat_cache`) + `range_choice` in `terrain.json`.
- `infra` (review 78): `SeaFossils.find`, `GiantGeodes.find`, `VolcanicField.find`, `IslandField.find` zijn vier kopieën van dezelfde zoek-visitor (+ per-random-state-caches in `Ring3Terrain`/`NetherRing4Terrain`): één generieke `findFirst(root, Class)` + één cache.
- `worldgen` (review 78): `DuneRubbleFeature` kopieert de plaatsing van `RubbleFeature` (alleen het `Terrain` verschilt); `RubbleFeature` een `Terrain`-haak geven (ring 2 vergrendeld, dus INT/later).
- `worldgen` (G5): het tempo is 0,75 per 256² bottenduin i.p.v. ~1; hoger kan alleen met kleinere fossielen of fossielen die de duinband volgen (gebogen ruggengraat).
- `infra`/`worldgen` (INT): `SeaFossils` deelt het cel-cache-patroon met `GiantGeodes`, `DewGeodes`, `RockPillars` (zie backlog 65).
- `textures` (teststop 5b): `bone_block`-fossielen naast `fossil_stone`-looppaden en het bottenzand in-game beoordelen; zuiverkristal-clusters en -lampen op de zuilen en in de geodes.
- `worldgen` (INT): backlog r. 283 is voor de Nether af (de overworld-helft, `ring4_surface.py`, niet); r. 284 af.
- `worldgen` (teststop): kansen bekleding (clusters 25 %, lampen 2 %), cladding (18 %, 1/150) en rubble bijstellen op het beeld.

## Code-review (8558fec, medium)
Opgepakt:
1. Cladding: een cluster staat nooit bovenop iets (het blok eronder moet open zijn), niet alleen niet boven looppaden.
2. Cladding leest de regio via de bedrade `ring_4/nether/region` (`SeaFossils.regionAt`), niet de ruwe ruis: één definitie.
3. `SeaFossils`: de hellingsvloer wordt in de Y-wacht geklemd, zodat "alles in Y 24–110" ook voor de helling geldt.
4. `terrain.json`: `sea_fossils` alleen in de duinband (`|region| < 0,14`); de spitsen betalen niets. Gevolg: de fossielen houden nu ook de gecachte regio (quart-hoek, tot ~4,2 weg) in de band; test op elke quart-hoek; tempo 0,85 → 0,75.
5. Cladding: twee peillagen (Y 60 schacht, Y 100 kop) slaan chunks zonder zuilhuid over vóór de volle lezing.
8. `DuneRubbleFeature.nearFossil`: de (hoogstens vier) cellen één keer opzoeken.
9. Rubble: scan tot 12 onder Y 47 (duinvloer aan de holterand tot ~Y 37).
Niet opgepakt: 6 (vier kopieën van `find`) en 7 (`RubbleFeature`-duplicaat) → Backlog; 10 (`CODEBASE.md`) is volgens `waves.md` §5 werk van INT (deze stroom schrijft alleen het eigen plan met CODEBASE-delta).

## Voortgang
- 2026-10-16: branch `ring4/nether-g2` vanaf c5b7d11; ontwerpvragen N1–N4 naar main, antwoorden alle A.
- 2026-10-16: 612dba1 fossielen als terrein + echte blokken in de surface; 8558fec bekleding, cladding, rubble; B en D groen.
- 2026-10-16: code-review medium afgehandeld (zie boven); B en D opnieuw groen.
