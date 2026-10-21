# Ring 5 golf 1 — stroom W-terrein: 84, 85, 83

Scope: `worldgen` — drie pakketten, één commit per pakket (`worldgen:`), na elkaar in **deze volgorde: 84 → 85 → 83**
(`golf-1.md` §2: teststop 6-pre-1 vraagt eerst 84 en 85). Branch `ring5/terrain-g1`, worktree
`P:/Projecten/Java/wt/r5-terrain-g1`, poort n = 1 (server 25571, RCON 25581), basis = de startcommit van `golf-1.md`.

Bronnen: `contract.md` §1, §4, §9 en §11; `golf-0.md` §5 (kaarten) en §7 (V1–V12); `82-worldgen.md` (wat er staat,
implementatiekeuzes, backlog); `waves.md` §1, §3, §5, §7; `fase-1-wereld.md` → "Ring 5" en → Ring 4 "Grens 4→5".
`CODEBASE.md` kent golf 0 nog niet: lees de CODEBASE-delta's van `81-infra.md`, `82-worldgen.md`, `81b-infra.md` erbij
(tenzij main ze al heeft toegevoegd, `golf-1.md` §4).

**Bezit** (`waves.md` §1/§5): `density_function/ring_5/overworld/**`, `density_function/zone_4_5/**`, `noise/ring_5_*`,
de ring 5-sleutels in `ring_blend/overworld*`, `final_density` in `noise_settings/overworld.json`, `RingFluidPicker`/
`RingAquifer` (ring 5-regel en zone 4→5), `Ring5Band`, `Ring5Field` (+ `Ring5Output`, `Ring5Fields`), `ZoneFracture`,
`Ring4Sundered`, `IslandField` (alleen de zone 4→5-delen), `Ring4Tunnels` (alleen als 85 het vraagt), `density_function/void/
overworld.json` (84, V2-A) en de bijbehorende DF-types (blok `// ring 5 — W-terrein` in `ModDensityFunctionTypes`).
**Niet:** surface rules, preset, biomes, features (W-biomes); `SplitRockFeature`/`SunderedReachFeatures` (W-biomes, 86c).

---

## 1. Architectuur (alle pakketten)

### 1.1 Eén veld
Het hele ring 5-overworldterrein blijft **één Java-veld, `Ring5Field`** (zoals `IslandField` voor ring 4): per cel
(`cell_size` 128) beslissingen in `Decisions` (eenmaal per wereld, puur per cel en per wereld, nooit via een
`flat_cache`), per kolom een `Column` in de `ColumnTable`. Uitgangen via `ring_5_output` (`Ring5Output`); de gedeelde
DF's van contract §4 (`height`, `scars`, `castles`, `glitch`, `inverted`, `breakup`, `initial_height`) worden
`flat_cache`'s of `interpolated` van die uitgangen. Het skeletterrein van 82 (`SURFACE_BASE` 85 + 8 · placeholder,
band-trede 6) verdwijnt pakket voor pakket; de placeholder-ruis wordt niet meer gelezen zodra 83 klaar is.

Getallen die de vorm bepalen staan als constanten in **één klasse per deelvorm** (zoals `Ring4Band`), bv.
`Ring5Outer` (84), `Ring5Castles` (84, de pinakel- en brugmaten), `Ring5Inner` (83); `ZoneFracture` (85). Nieuwe
ruis: `noise/ring_5_*.json` (eigen bezit).

### 1.2 Dichtheid
Zoals ring 3/4: `ring_5/overworld` = `interpolated` van de uitgang `terrain`, met `0,125 · (top − y)` geklemd en exacte
wanden per blok waar het 4-blokraster te grof is (pinakelwanden, brugranden, scheurranden: een `thin`-achtige uitgang
buiten de interpolatie, patroon `island_output thin`). `initial_terrain` blijft `0,125 · cache_2d(initial_height)` +
gradiënt; `initial_height` = ondergrens van het echte oppervlak rond de quart, en `VOID_HEIGHT` (−128) waar de kolom
geen grond heeft (valkuil uit de review van golf 0). **Geen grotten in golf 1** (93); geen vanilla-noise-caves.

### 1.3 Assen en de afspraak met W-biomes
De router van 82 blijft: temperature = `region_rot`, vegetation = `region_glitch`, weirdness = `band`, erosion =
`breakup`. De biomedrempels zijn van W-biomes (`golf-1-biomes.md` §2) en gelden hier ook voor de vormen per biome:
- **binnenband** (band < 0): `region_glitch` ≥ 0,125 → glitchvormen (`glitch_fields`); anders `region_rot` ≥ 0,244 →
  weidevormen (`false_meadow`); anders dode vlakte (`corrupted_wastes`). Overvloeien ±0,04 rond elke drempel.
- **buitenband** (band ≥ 0): `breakup` ≥ 0,6 → `shattered_expanse`; anders `region_rot` < 0 → `void_scars`
  (scheuren en kraters), ≥ 0 → `inverted_hills` (omgekeerde heuvels).
- **Pinakel-as (84):** de pinakel moet `void_scars` krijgen (V12-A) ongeacht `region_rot`. 84 maakt een nieuwe DF
  `ring_5/overworld/climate_rot` = `region_rot`, behalve op het pinakelplateau (`castles` ≥ 0,99) = **−2**, en zet
  `ring_blend/overworld_temperature` ring_5 daarop. `castles` is precies 1 binnen de pinakelstraal (plateau), lager op
  bruggen en de verwoeste rand. Het terrein zelf leest `region_rot` (niet `climate_rot`).
Wijzigt W-biomes een drempel, dan meldt main dat; de terreinconstanten volgen.

### 1.4 Vloeistof (V9-B, 82) en bodem (V3-A)
Ring 5 droog, behalve de lavalaag onder −54 in de binnenband achter de dam van 32 (`RingFluidPicker.ring5LavaColumn`/
`ring5Dam`). Bedrock alleen onder volle kolommen (vanilla-regel: niets op lucht); scheuren en de void tot onder `min_y`
leeg; buitenband zwevend met onderkant ~Y −40 (bandlijn) → Y 20–40 (rand). Geen losse groepjes van 1–2 blokken boven de
void (voidscan-regel `loose`): elke brok, rand en brugrand minstens 3 × 3 × 3, of hij hangt vast.

### 1.5 Determinisme en prestatie
Alle beslissingen per cel puur uit de salt van `ring_5_cells` (wereldseed) en de layout; accessors alleen lezen. Geen
volgorde-afhankelijkheid tussen chunks (les 75c). Per kolom één `Column`-opzoeking; zoekradius van brokken/bruggen ≤
`cell_size` (of expliciet getest). Prestatie: indicatief per pakket niet nodig; INT1 meet (≤ 1,75×).

---

## 2. Pakket 84 — buitenband, kastelen, bruggen, buitenrand

**Wat** (V2-A, V3-A, V4-A, V5-A, V6-A, V12-A)
1. **Breukas `breakup`** (0 vast … 1 void zonder brok): van ~0 op de bandlijn naar gemiddeld **~85 % void** aan de
   ringrand (9.700), met ruis (grillige randen, schotsen); daarna doorlopend in de buitenrand (zone 5→void en ring
   `VOID`): steeds kleinere brokken, **leeg vanaf ~10.050**. `Ring5Field.breakupAt` en de DF `breakup` echt.
2. **Schotsen** (de vaste grond van de buitenband): toppen Y ~70–110, bij de bandlijn aansluitend op de binnenband (Y
   ~78); onderkant van ~Y −40 bij de bandlijn naar Y 20–40 bij de rand, gerafeld (geen vlakke plaat); daaronder void.
   `solidGround(x, z)` echt.
3. **Brokken** (`Fragment(x, z, radius, bottom, top, tilt)`, `fragmentsWithin`): Y 40–160, Ø ~6–40, licht gekanteld
   (≤ 20°), meer en kleiner naar buiten; minimaal 3 × 3 × 3.
4. **Kasteelpinakels** (plekken uit 82, ongewijzigd): plateau met de straal en top van `CastleSite`, rotswanden tot
   ~Y −40 (exacte wand, rafelig), eronder void; de void-gordel (40–80) blijft open behalve de bruggen. Rond het
   plateau een verwoeste rand (lagere, gebroken treden binnen 8–16 van de plateaurand) als vorm. `castles` 0..1
   (1 = plateau).
5. **Bruggen** (`Bridge(siteIndex, fromX, fromZ, toX, toZ, halfWidth, deckY)`, `bridgesWithin`):
   - per kasteel **2 natuurlijke bruggen**, 5–9 breed, licht doorhangend (dek in het midden 2–6 lager), brokkelige
     randen (exacte rand per blok, gaten ≤ 1 aan de rand, dek nooit onderbroken), onderkant 4–8 dik met druppelvormen;
   - brug 1 naar binnen tot vaste grond (de dichtstbijzijnde `solidGround`-kolom richting de bandlijn), brug 2 schuin
     (30–60° van de straal) naar de grootste schots binnen bereik; `siteIndex` = kasteel;
   - in de buitenband **smalle ruggen** (3–5 breed) om de ~400–600 blokken langs de ring tussen schotsen, en
     **stroken springbare brokken** (gaten ≤ 3, hoogteverschil ≤ 1). Geen bereikbaarheidsgarantie (V6-A). Ruggen
     melden zich in `bridgesWithin` met `siteIndex` −1.
6. **`void_scars`-vormen** (waar `region_rot` < 0 en breakup < 0,6): scheuren en kraters tot in de void (tot onder
   `min_y`, geen bedrock), wanden steil en exact; `scars` 0..1 (1 = open tot de void).
7. **`inverted_hills`-vorm** (waar `region_rot` ≥ 0 en breakup < 0,6): omgekeerde heuvels die onder schotsen en
   brokken in de lucht hangen (punt naar beneden, 10–40 lang); `inverted` 0..1. Bomen ondersteboven zijn 95.
8. **Buitenrand (V2-A, was pakket 93):** de void-invoer van `ring_blend/overworld` wordt het vervolg van hetzelfde veld
   (`void/overworld.json` = verwijzing naar de ring 5-uitgang, zodat de harde wissel `switch_at ["ring_5"]` geen naad
   geeft). De oude zwevende eilanden (Y 50–110, change 6) vervallen in de overworld; `void/nether.json` blijft.
9. **Pinakel-as** `climate_rot` en de router-wijziging (§1.3).
10. `Ring5Band` ongewijzigd.

**Tests (unit, in `build`)**
- `Ring5OuterTest`: breakup ~0 op de bandlijn, gemiddeld 0,80–0,90 tussen 9.600 en 9.700, leeg (geen vast blok)
  voorbij 10.050 op een raster, monotoon stijgend gemiddelde per strook van 100; schotsonderkant per strook (−40 →
  20–40); geen brok kleiner dan 3 in een richting; brokken Y 40–160.
- `CastleBridgesTest` (40 seeds): per kasteel precies 2 bruggen, breedte 5–9, brug 1 eindigt op vaste grond, brug 2
  op een schots; dek nergens onderbroken; ruggen 3–5 breed; `bridgesWithin` vindt elke brug vanuit elk chunkvak dat hij
  raakt.
- `Ring5FieldTest` uitgebreid: pinakelwand exact, gordel open behalve de bruggen, `castles` 1 op het plateau;
  `climate_rot` −2 alleen op het plateau.
- `Ring5JsonTest`: de stubs `scars`, `inverted`, `height` zijn geen 0 meer; `void/overworld` verwijst naar het veld;
  router temperature ring_5 = `climate_rot`.

**Client/server-impact:** puur worldgen (server). Geen client-class, geen payload. `Ring5Field` blijft server-only.

**Af als:** B, D (geen wijziging in `src/generated` verwacht), unit tests groen; S-smoke op een verse wereld met de
kaarten van `golf-0.md` §5 punt 1–3 en een kaart van één kasteel met bruggen (`build/test/maps/6-pre-1/`).

## 3. Pakket 85 — breukzone 4→5 (V7-A, V8-A)

**Wat**
1. **`ZoneFracture` krijgt inhoud** (puur veld van layout en een salt uit de hash-noise `zone_4_5_cells`, nieuw, op de
   wereldseed; zoals contract §4): sterkte over de zone (0 aan de binnen- en buitenrand ± 16, vol in het midden),
   per blok ná de blend:
   - **~75 % dichte grond** (Y ≥ 64) over de hele zone (gemeten per strook van 32 langs de straal: nergens < 60 %);
   - **spleten** 2–8 breed, 30–60 diep, met een **droge puinbodem** (bodem ruw, geen water); netwerk van lange
     spleten langs en schuin op de ring;
   - **schotsen** 8–30 omhoog gedrukt, kanteling ≤ 30°, randen exact;
   - **losse brokken** vooral in de buitenste helft (Ø 4–16, boven de spleten);
   - **oversteken**: langs elke spleet om de ~30 blokken (max. 40) een smalle plek (≤ 3 breed) of een ingestorte schots
     als brug; overal over te steken (te voet met hooguit 1 blok springen);
   - accessors `crackAt`, `floeAt`, `strength`, plus **`landmarksWithin(minX, minZ, maxX, maxZ)` →
     `Landmark(x, z, topY, radius)`**: een opgeheven, vlakke schots (top Ø ≥ 16) om de ~650 (± 100) blokken langs de
     zone, voor de gespleten rots (W-biomes 86c).
2. **Ring 4 ombouwen** (contract §9):
   - `Ring4Sundered`: scheuren-raster, losse brokken, **landbruggen**, afgrond −20, zee Y 0 en `CALM_SHARE` weg; wat ring 4
     nog nodig heeft (rim, calm-strook aan de binnenrand) blijft of verhuist naar `ZoneFracture`. `ring_4/overworld/
     sundered` wordt een verwijzing naar `zone_4_5/overworld/fracture`.
   - `IslandField.sunder`, `SUNDERED_CALM_SHARE`, `Column.sunderKind` weg; **`landBridgesNear` blijft tijdelijk** als
     `@Deprecated(forRemoval = true)` met een lege lijst (de gespleten rots plaatst dan niets tot 86c). In
     `DecorDLandmarksTest` alleen de brug-asserts `@Disabled("85: land bridges removed; split rock moves in 86c")`.
   - `RingFluidPicker`/`RingAquifer`: de hele zone 4→5 **droog** (`sunderedZone`/`SUNDERED_SEA_TOP`/`sunderedSealBelow`
     en het afgrondwater weg); de dam onder −54 blijft zolang ring 4 daar droog is; `ring5Dry` sluit de zone niet meer
     uit (de naad 4\|5: backlog 82 "zone 4→5 droog" afgehandeld).
   - Tests: `Ring4SunderedTest` → `ZoneFractureTest` (nieuw gedrag); zone-tests in `RingFluidPickerTest`,
     `RingAquiferTest`, `Ring4TunnelsTest` aangepast; `Ring4Tunnels.ZONE_GAP` blijft.
3. De ring 4-blokken van `sundered_reach` in `ModConfiguredFeatures`/`ModPlacedFeatures` mag 85 aanpassen/verwijderen
   (uitzondering `waves.md` §5), **in een aparte commit** binnen 85, alleen als ze zonder landbruggen/afgrond kapot gaan.

**Tests**
- `ZoneFractureTest`: buiten de zone (± 16) bitgelijk aan het argument (blijft); aandeel dicht ≥ 70 % gemiddeld en ≥ 60 %
  per strook; spleetbreedte 2–8, diepte 30–60 (gemeten op een raster), droge bodem (geen vloeistof via de picker);
  schotsen 8–30 omhoog, kanteling ≤ 30°; langs elke spleet een oversteek binnen 40 blokken; geen landbruggen;
  `landmarksWithin` om de 550–750 blokken, top Ø ≥ 16; periodiek over de naad ±π (les 63); puur (zelfde seed = zelfde
  breuk, kopie van de noise chunk = zelfde antwoord).
- `RingFluidPickerTest`/`RingAquiferTest`: zone 4→5 droog op elke hoogte boven de dam; geen water tegen lucht.

**Client/server-impact:** worldgen, server. Sfeer van `sundered_reach` (grijsfade, drone) blijft. Geen payload.

**Af als:** B, D, unit tests groen; S-smoke + kaarten: `terrainmap 8000 0 400 1` (zone oost), `section 7800 -600 8200
600` en een tweede doorsnede radiaal (`section 7700 0 8300 0`) in `build/test/maps/6-pre-1/`. **Daarna meldt W-terrein
aan main: klaar voor teststop 6-pre-1** (`golf-1.md` §5), en gaat door met 83.

## 4. Pakket 83 — binnenband (V4-A)

**Wat**
1. **Dode grond:** basis Y ~78, lage heuvels Y 70–95 (eigen ruis `ring_5_hills`, −8), aansluitend op de buitenrand van
   zone 4→5 (Y ≥ 64) en op de schotsen van 84 bij de bandlijn (de band-trede van 82 verdwijnt).
2. **"Net fout"-vormen** (voorstel; bijsturen bij 6a):
   - `corrupted_wastes`: vlakte met **te regelmatige trappen** (treden van precies 4 hoog, rechte randen op het
     blokraster, 1 per cel van 128 met kans 0,3, 3–6 treden) en **perfect ronde, ondiepe kuilen** (Ø 8–20, 2–4 diep);
   - `false_meadow`: zachte, vanilla-achtige weide (Y 72–80) die **te glad en te symmetrisch** is: een glooiing als
     regelmatige golf (sinus op twee assen, amplitude 3) in plaats van ruis;
   - `glitch_fields`: **blokkerig terrein** via `quantize` (DF-type `corruptedadventure:quantize`, record `argument`,
     `xz_step`, `y_step`; 83 beslist JSON of Java en noteert het), **verschoven lagen** (schijven van 4–6 hoog,
     horizontaal 2–6 verschoven per laag) en **zwevende kubussen** (3–8 groot, 6–30 boven de grond, per cel van 32 kans
     0,25); `glitch` 0..1.
3. **Losse scheuren** (binnenband): smal (2–6 breed), 20–120 lang, tot in de void (tot onder `min_y`, geen bedrock);
   ~1 per 256 × 256 (voorstel); `scars` 0..1 ook in de binnenband. **Dam van de lavalaag rond elke scheur**: geen open
   ruimte en geen lava onder −54 binnen ≥ 6 blokken van een scheur (backlog 82); `ring5LavaColumn` sluit die strook uit.
4. `height` (de vaste grond zonder scheuren), `initial_height` (ondergrens rond de quart, scheuren niet meegeteld, kubussen
   niet) echt; de placeholder-ruis en de band-trede weg.

**Tests**
- `Ring5InnerTest`: oppervlak 70–95 (≥ 95 % van een raster), mediaan 76–80; trappen exact 4 hoog; kubussen zweven
  (lucht eronder), ≥ 3 groot; `quantize` blokkerig (stapgrootte gemeten); scheuren tot onder `min_y` zonder vast blok;
  aandeel scheuren in de binnenband ≤ 2 %.
- `RingFluidPickerTest`: geen lava en geen open ruimte onder −54 binnen 6 van een scheur; lavalaag verder ongewijzigd.
- `Ring5JsonTest`: `glitch`, `height`, `initial_height` echt; geen placeholder meer in `ring_5/overworld/*`.

**Client/server-impact:** worldgen, server. Geen payload.

**Af als:** B, D, unit tests groen; S-smoke + kaart `terrainmap 8500 0 400 2` (binnenband oost) en een doorsnede
`section 8000 0 9000 0` in `build/test/maps/golf-1/`.

---

## 5. Afhankelijkheden
- Alleen golf 0 (`Ring5Band`, `Ring5Field`, `ZoneFracture`-skelet, router). Niets van andere stromen.
- W-biomes leest `breakup`, `castles`, `climate_rot`; die zijn tot de merge van 84 stubs/skelet. W-biomes test met
  lokale ruis (niet gecommit) of na een rebase op deze branch (main regelt dat).
- W-biomes 86c leest `ZoneFracture.landmarksWithin` (85).

## 6. Tests per pakket (samenvatting)
B + D + unit tests + S-smoke met kaarten (§2–§4). **Niet per pakket:** `det.sh`, `voidscan.py`, `lavascan.py`,
`waterscan.sh`, `perf.sh` (INT1). Wil je tussentijds de bodemregel controleren, dan mag één `world.sh` + `voidscan.py`
op `castles.txt` (klein), in overleg met main (slot).

## Voortgang
- Voortgang: 2026-10-21 20:41 worktree `P:/Projecten/Java/wt/r5-terrain-g1` op `ring5/terrain-g1` (vanaf 3dfd50aa), `setup.sh W-terrein 1`, eerste compile groen; start 84.
- Voortgang: 2026-10-21 21:49 84 klaar: build groen (1.153 tests), runData zonder wijziging, S-smoke `r84` (log schoon, `/ca castles`, kaarten oost + kasteel #1 + doorsnede); commit.
- Voortgang: 2026-10-21 22:16 85 klaar: build groen (1.154 tests, 1 overgeslagen), runData zonder wijziging, S-smoke `pre1` (log schoon) + kaartenset 6-pre-1 in `build/test/maps/6-pre-1/`; commit, daarna 83.

## CODEBASE-delta
**84:** `Ring5Field` echt voor de buitenband: codec-veld `noises` (`Noises`: `rot` = `ring_5_rot`, `relief`, `detail`, `scars`;
nieuwe noises `noise/ring_5_relief` (−8), `ring_5_detail` (−4), `ring_5_scars` (−7)); schotsen op een gejitterd raster van
40 (`floeCell`, `floeSolid`, gecachet in `Decisions.floes`), `void_scars` (scheuren langs de nullijnen van `ring_5_scars` +
kraters), `inverted_hills` (omgekeerde heuvels onder de schotsen), brokken (`fragment`, `fragmentsWithin`), ruggen
(`ridges`, in `bridgesWithin` met `siteIndex` −1) en stroken springstenen (`strips`), kasteelpinakel met verwoeste rand
(`rimWidth`) en per kasteel 2 bruggen (`castleBridges`, landingsheuvel `mounded`); `groundAt` (puur, zonder cache) voor
de beslissingen. `Column` kreeg `scars`, `inverted`, `height` en exacte spans (bruggen, ruggen, brokken). Records:
`Bridge` + `endY`, `sag`; `Fragment` + `direction`. Nieuwe constanten-klassen `Ring5Outer` (buitenband/buitenrand) en
`Ring5Castles` (pinakel, rand, bruggen). `Ring5Output` + uitgangen `height`, `scars`, `inverted`. JSON:
`ring_5/overworld` = `ring_5_output terrain` **zonder** `interpolated` (exact per blok); `ring_5/overworld/{height,scars,
inverted}` = flat_caches van de uitgangen; nieuw `ring_5/overworld/climate_rot` (−2 op het plateau) als router-temperature
ring_5; `void/overworld` = `add(0, ring_5/overworld)` (V2-A; de oude zwevende eilanden en `void_islands` zijn in de
overworld weg, Nether ongewijzigd); `ring_blend/overworld_initial` void = `ring_5/overworld/initial_terrain`;
`ring_blend/overworld_erosion` + `switch_at ["ring_5"]` (verzoek main na de review van 86). Tests: `Ring5OuterTest`,
`CastleBridgesTest`, `Ring5FieldTest`/`Ring5JsonTest`/`Ring3JsonTest` aangepast.
**85:** `ZoneFracture` is nu een klasse (geen record) met codec-velden `argument` en `hash` (`noise/zone_4_5_cells`, nieuw);
vorm in `ZoneFractureShape` (constanten + beslissingen per kolom), 2D-waarde `ZoneFractureValue` (DF-type
`zone_fracture_value`, `zone_4_5/overworld/fracture` = flat_cache ervan). Accessors: statisch `strength(layout, eff)`;
instance `crackAt`, `floeAt`, `landmarksWithin`/`landmarks` → `Landmark(x, z, topY, radius)`, `find(root)`;
`CALM_SHARE`. `ring_4/overworld/sundered` = `add(0, zone_4_5/overworld/fracture)`. `Ring4Sundered` teruggebracht tot
zonegrenzen + rand (`zoneStart`, `zoneEnd`, `inRim`, `RIM_TOP`, `RIM_BEFORE`); weg: scheurraster, losse brokken,
landbruggen, afgrond, zee Y 0, `CALM_SHARE`. `IslandField`: `sunder`, `Sunder`, `SUNDERED_CALM_SHARE`,
`Column.sundered/sunderKind/abyssSeaTop/abyssFloor` weg; `landBridgesNear` `@Deprecated(forRemoval = true)` en leeg.
`IslandOutput.SUNDERED` weg (ook uit `Ring4Filter`). `RingFluidPicker`: `sunderedZone` → `fractureZone` (public),
`sunderedSealBelow` → `fractureSealBelow`, `SUNDERED_SEA_TOP` weg; zone 4→5 overal droog (picker `NO_FLUID`); `ring5Dry`
sluit de zone niet meer uit (zoneregel gaat voor). `RingAquifer`: zone droog vanaf Y −54 (`airFrom`), abyss-takken weg.
Tests: `Ring4SunderedTest` weg, `ZoneFractureTest` nieuw, `RingFluidPickerTest`/`RingAquiferTest`/`Ring4JsonTest`/
`Ring5JsonTest`/`Ring5FieldTest` aangepast, `DecorDLandmarksTest` brug-test `@Disabled`.

## Beslissingen
**84 (implementatiekeuzes):**
- Terrein exact per blok (geen `interpolated`): elke kolom is een 2D-top en -onderkant plus exacte spans, dus wanden,
  brugranden en scheuren liggen precies op het blok (§1.2 vraagt exacte wanden; met een 2D-hoogteveld geeft de
  interpolatie geen winst). Dichtheid blijft `0,125 · afstand` geklemd op ±1, zodat de menging in zone 4→5 glad blijft.
- Schotsen = cellen van 40 op een gebogen raster; elke cel is schots of void met kans `share(p)` (p = 0 op de bandlijn,
  1 op de ringrand); `share` 0 tot p 0,04, 0,86 op de rand (`SHARE_POWER` 0,85), daarna in 140 blokken naar 1; de
  buitenrand is leeg vanaf 70 % van rand→border (10.050). Gemeten (testseed): breakup 0,008 op de lijn, 0,875 tussen
  9.600 en 9.700, per strook van 100 stijgend.
- `breakup` per kolom: schots 0,4 · share (< 0,6, dus nooit `shattered_expanse`), void 1, void met brok/brug/rug erboven
  0,9; scheurkolommen houden de schotswaarde (biome blijft `void_scars`), `scars` = 1.
- Onderkant schots: −40 + 56 · p (+ rafel ±7), naar de rand van een schots 50 % dunner over 20 blokken; gemeten mediaan
  −38,7 bij de lijn, 38,7 bij de rand (p 0,95–1). Nooit onder −56 (geen bedrock), minstens 6 dik.
- Pinakel: plateau = de straal van `CastleSite` (castles 1), verwoeste rand 8–16 breed **binnen** de gordel (treden 3 breed,
  2 lager, naar buiten tot 45 % weggebroken, onderkant stijgt 3 per blok), daarbuiten open gordel (castles 0,6 → 0).
- Kasteelbruggen: het dek begint op de plateautop (`deckY`) en loopt naar de landing (`endY`), met doorhang 2–6; is het
  verval meer dan 0,5 per blok, dan landt de brug hoger en draagt de grond daar een landingsheuvel (helling 0,8). Brug 1
  langs de straal naar binnen tot de eerste grond (3 blokken landing), brug 2 onder 30/40/50/60° naar de grootste schots
  (geteld in schotscellen, max. 120) binnen 260 (anders 900). `Bridge` kreeg `endY` en `sag` erbij (contract-record +2
  componenten, zoals `belt` in 82).
- Ruggen: één per ~500 langs de lijnstraal (±100), radiaal van de bandlijn tot p 0,92, 3–5 breed, 4–6 dik, dek volgt de
  (virtuele) schotstop; stroken springstenen halverwege twee ruggen (Ø 7, om de 8, hoogteverschil ≤ 1), alleen boven void.
  Niet in kasteelgordels.
- Brokken: één cel van 48, kans 0,2 → 0,65 (p 0,15 → 1), daarna naar 0 aan de lege rand; straal 20 → 5 naar buiten
  (× 0,35–1); top zo dat Y 40–160 ook met kanteling (≤ 20°) klopt; rand ≥ 3 dik; alleen met het midden boven void.
- `height` = top van de vaste grond (scheurkolommen: de schotstop), `VOID_HEIGHT` boven de void.
- Kaarten: `/ca terrainmap` zoekt de top in grove stappen, dus dunne dekken (4–8) zien er op de bovenkaart gestippeld uit;
  de doorsnede toont ze heel.
**85 (implementatiekeuzes):**
- Spleten = randen van een gejitterd raster van 48 (langs de ring) × 26 (dwars), gebogen met ±7: een netwerk langs en
  schuin op de ring. Halve breedte 1–4 × sterkte (2–8 breed), bodem 30–60 onder de grond (± 2 ruw). De sterkte is 0 in
  de eerste en laatste 16 blokken van de zone en vol na 32 meer. Gemeten (testseed): dicht 0,76–0,79 per strook van 32 in
  het midden, 0,98 aan de randen, gemiddeld 0,82; mediane spleet radiaal 4 breed.
- De grond onder de snede komt uit de twee terreinen die de blend mengt (`IslandField`-oppervlak en `Ring5Field`-top,
  gewogen met de blend en hun dichtheidshelling 0,08/0,125); zonder die velden (tests) Y 72. Diepte, schotsen en brokken
  rekenen vanaf die grond.
- Oversteken: langs elke spleetrand om de 24–34 blokken een dichte plek van 4 lang (de "ingestorte schots" op
  grondhoogte: lopend over te steken, niet springen). De variant "smalle plek ≤ 3" is niet gebouwd.
- Opgeheven schotsen: 20 % van de cellen, 8–30 omhoog, kanteling ≤ 30°, rots tot 12 onder de grond. Losse brokken: per
  cel van 40 kans 0,45 (buitenste helft) / 0,1 (binnenste), alleen boven een spleet, Ø 4–16, 4–16 boven de grond, ≥ 3 dik.
- Landmarks: om de ~650 (± 100) langs de zone op 55 % van de zone, straal 9–13, 10–16 boven de grond, vlak, 4 blokken
  rondom zonder spleet. W-biomes 86c leest `ZoneFracture.landmarksWithin` (via `find` op de router); tot dan plaatst de
  gespleten rots niets (`landBridgesNear` leeg).
- Vloeistof: zone 4→5 (beide helften) droog op elke hoogte boven de dam −54; de kalme eerste strook (`CALM_SHARE`
  16/256) blijft onder Y 63 dicht (zoals de oude `sunderedSealBelow`), zodat ring 4's zee in een grot over de binnenrand
  niet naast lucht staat.
- `sundered_reach`-features: ongewijzigd (de gespleten rots vindt geen bruggen meer en plaatst niets); geen aparte commit
  nodig.

## Backlog
- [worldgen] 86c (W-biomes): `SplitRockFeature` naar `ZoneFracture.landmarksWithin` en `IslandField.landBridgesNear` +
  `LandBridge` + de uitgeschakelde test in `DecorDLandmarksTest` verwijderen (compile-waarschuwing `[removal]` tot dan).
- [infra] `/ca terrainmap` (bovenkaart) mist dunne lagen (bruggen, ruggen, springstenen) door de grove stap van
  `ColumnScan`; voor ring 5 een fijnere stap of een "spans"-pad (infra/tooling).
