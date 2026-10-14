# Ring 4 golf 1 — stroom W-nether: 65 (Nether-ring 4-terrein + biomes)

Scope: `worldgen` — werkpakket 65; branch `ring4/nether-g1` (vanaf `main` 7ffbfeb), poort n = 3 (server 25573, RCON 25583).

**Doel:** de vorm van de droge kristal-/bottenwoestijn in Nether-ring 4 (1.500–2.000, 500 breed) als eigen ring 4-invoer van `ring_blend/nether`, met de drie biomes op de Nether-regio-as van 59:
- `bone_dunes` (hoofdbiome): een duinvlakte van bottenzand;
- `crystal_spires`: de diep weggezakte lavazee met kristalzuilen van de lava tot het plafond en bottenbruggen ertussen;
- `geode_hollows`: een gesteentemassa met reusachtige holle geodes als vorm (bekleding in 78).
Plus de ruimte-check (genoeg loopruimte) en de treden 3|4 (Nether-3 Y 22 → 10) en 4|5 (10 → 32) afgedekt volgens de dam-regel.

Bron: `contract.md` §1 (Nether ÷4), §2, §4; `fase-1-wereld.md` → Nether-ring 4; `docs/plans/ring-3/contract.md` §4 (dam-regel); sjabloon Nether-3 (53/53b). Ontwerpvragen V1–V4: zie Beslissingen.

## Gebouwd
1. **Biomes** (`NetherRing4Biomes`, `ALL`): heet en droog (temperatuur 2,0, geen neerslag), **geen features** (78) en **geen carvers** (dammen heel, zoals Nether-3); effecten tijdelijk de bottenwoestijn-fog van de placeholder (`0xD8CBA6`), lucht zwart, muziek `soul_sand_valley`, tot A in 80 een haak in `BiomeAmbience` levert; spawns als `soul_sand_valley` (V4).
2. **Preset:** Nether ring 4-source = `multi_noise` op `temperature` (= `ring_4/nether/region`), drempels `NetherRing4Features.SPIRES_THRESHOLD` (0,18) / `HOLLOWS_THRESHOLD` (−0,18): midden `bone_dunes`, hoog `crystal_spires`, laag `geode_hollows` (V1); `fluid_levels.ring_4` = **10**.
3. **Tags** (`NetherRing4BiomeTags`): `is_nether_ring_4`, `purifying` (alle drie), `c:is_nether`, `c:is_hot/nether`, `c:is_dry/nether`. Geen `minecraft:`-tags.
4. **Lang** (`NetherRing4BiomeNames`): Bone Dunes / Bottenduinen, Crystal Spires / Kristalspitsen, Geode Hollows / Geodeholten.
5. **Terrein** `density_function/ring_4/nether/*` (geschreven door `tools/worldgen/nether_ring4_terrain.py`; de placeholder `ring_4/nether.json` is weg):
   - gewichten `spires_weight`/`hollows_weight` (ramp ±0,04 rond de drempels), `dunes_weight` = 1 − beide; `edge` = `flat_cache(ring_edge ring_4, 48)`, `core` = 1 − `edge`;
   - **duinen**: vloer Y 43 ± 3 (`dunes` = 1 − 2|ruis|, scherpe kammen), dicht tot ~Y 38–40 (V2 = C: geen lava zichtbaar buiten de spitsen); plafond 104–120 (`ceiling_height`);
   - **spitsen**: `sea` = `spires_weight` × `core`: de vloer zakt naar Y 4, de lavazee vult tot Y 10; kristalzuilen (`pillars`, ruis −4, drempel 0,40: ~4–8 breed) van de lava tot het plafond, met een smalle **richel** (Y 32–42, 1–3 breed) op loophoogte en een verbrede kop; **bottenbruggen** (`bridges`, Y 36–42, 2–5 breed) langs de nullijnen van twee ruisen (`min(|a|, |b|)` < 0,07: een gaas van lijnen zonder losse eindes), met eigen gewicht `bridge_weight` = clamp(40 · `sea`) zodat ze al op de rand van de duinen beginnen (bovenkant gelijk met de duinvlakte);
   - **holtes**: massa tot Y 88 ± 6 (−60 × `edge`) met tunnels (Y 50 ± 6, ~5 hoog, 6–9 breed); de **geodes** zijn het nieuwe DF-type `corruptedadventure:giant_geodes` (`GiantGeodes` + geometrie `GiantGeodeShape`): per cel van 128 hoogstens één bol (Ø 20–40, rek ±15 %), hol boven een vlakke vloer op ⅓ van de hoogte (vloer Y 42–48), alleen waar de regio ≤ −0,22, vrij van de randen en met een dichte koepel (3 blokken gesteente); **1–3 deuropeningen** (6 breed, 6 hoog, boog) die elk naar het eerste open terrein lopen (tunnel, duinen) binnen 24 blokken voorbij de wand; zonder opening geen geode. `terrain` = `range_choice(hollows_weight > 0: min(interpolated_shape, geodes); anders interpolated_shape)`, één interpolator;
   - **dam** = `5 × edge × gradiënt(33→37) − 2,5`: vast tot Y 33 binnen ~24 blokken van beide grenzen (dekt 22 en 32); zee, zuilen en bruggen lopen af met `core`; ruwheid alleen in de holtes (`rough` = 0,7 × `hollows_weight` × ruis − 0,05: duinen, bruggen en richels glad, geen gaten in de bruggen).
6. **Surface** (`noise_settings/nether.json`, ring 4-tak direct na Nether-3, vóór vanilla's `hole` → lava; `tools/worldgen/nether_ring4_surface.py`), vanilla-placeholders. **Vloeren eerst:** bodem van de lavazee (onder Y 13) `basalt`/`magma_block`; looppaden `bone_block` (fossielsteen; valt niet): op de brugnullijnen en rond de zuilen vanaf het begin van de spitsenzone (regio ≥ 0,14, dus ook in de overgangsband 0,14–0,18 waar het biome nog `bone_dunes` is) en elke vloer van `crystal_spires`; overige vloeren `white_concrete_powder` (bottenzand) met **plafondwacht** (een bottenzandblok direct boven lucht wordt `end_stone`, gebleekt netherrack). **Dan de zuilen:** `calcite` met `quartz_block`-kern alleen waar het terrein ze op sterkte heeft (regio ≥ 0,20 en ≥ 32 blokken binnen de ring via de nieuwe surface-conditie `corruptedadventure:ring_depth`; de `core`-weging van het terrein). Fossielband Y 22–30 in de wanden van de zee; rest `end_stone`. De wissel naar de echte blokken (68) is een wijziging van de constanten in het script.
7. **Tests:** `NetherRing4BiomesTest`, `NetherRing4JsonTest` (drempels, één interpolator, elk bestand een object, noises bestaan, afloop aan de randen, surface-tak vóór de lava-regel + plafondwacht vóór elk vallend blok, dam tegen de echte ruwheid), `GiantGeodesTest` (ruiscel = Nether-instellingen, trilineaire interpolatie, gedeelde cellen tussen kopieën), `GiantGeodeShapeTest` (maten, blijft in de cel, deuren 60° uit elkaar en eindigend in de opening, holte/vloer/boog); `Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround` met de ring 4-tak (nu 3 treden); `Ring4JsonTest` (59, W-terrein) alleen de Nether-test aangepast (geen placeholder meer).

## Client/server-impact
Geen client-code; alles worldgen (server-side), data-driven plus één DF-type (common, puur). Geen payload, geen `PROTOCOL_VERSION`. Biomes/tags/lang via datagen.

## Determinisme
Terrein uitsluitend uit density functions op de wereldseed. `giant_geodes` is puur: geseede celhash (`nether_ring_4_geode_cells`), de regio-, rand- en vormfunctie op vaste punten; `mapAll` bedraadt alleen de noises (geen `flat_cache`-markers die per chunk anders antwoorden), zoals `dew_geodes`. Geen features in 65.

## Tests en metingen
- **B** groen (780 tests, 0 fouten; vóór de review 774). **D** `runData` groen; `src/generated`: 3 biome-JSON's, `is_nether_ring_4`, `purifying`, `c:is_nether`/`is_hot/nether`/`is_dry/nether`, lang en/nl (gecommit).
- **Dam-check** (testwerelden n4d en n4g, seed 20260930, `C:/Temp/ca-user-test.lock` afwezig): `tools/nether-lava-walls.py` r 1.500 → lava 0, **wanden 0**; r 2.000 → lava 136–146 (Nether-5), **wanden 0** (herhaald na de review op n4i). Offline `lavascan` op 5 gebieden (3 spitsen-, 2 holtegebieden, 230.906 lavablokken): blootgesteld 0, lek 0.
- **Plafondwacht:** vallende blokken boven lucht 0 (was 41–73 per gebied zonder wacht).
- **Ruimte-check** (`build/n4/space.py`, niet gecommit; beloopbaar = 2 lucht boven een vaste, niet-vloeibare vloer, stap ±1):
  - aandeel in de ring (terreinkaart, n4b): duinen 44 %, spitsen 21 %, holtes 36 %;
  - duinen: vloer Y 40–46, plafond 104–120: 60–75 blokken lucht; 100 % van de kolommen beloopbaar;
  - spitsen (gebied in het hart, 67 % open lava): looppaden (bruggen, richels, alle `bone_block`) 13.757 cellen: **84 % verbonden met de duinen**, 5 % alleen met de gebiedsrand (onbekend), 11 % los (richels om zuilen waar geen brug komt, zuilkoppen onder het plafond; geen losse bruggen); bruggen 2–5 breed, ~30 boven de lava; zuilen 4–8 breed (na de review; daarvoor 81 / 6 / 13 %);
  - holtes: massatop Y 82–94 (20–30 lucht tot het plafond, beloopbaar), tunnels; geodes: ~1 per 1,5 cel massa, elk met deuren naar tunnels of duinen (gezien in doorsneden Y 46–62).
- Geen determinisme-/perf-runs (INT).

## Voortgang
Voortgang: 19:40 branch `ring4/nether-g1` vanaf 7ffbfeb; `run/` klaar (poort 3); ontwerpvragen V1–V4 naar team-lead
Voortgang: 19:46 biomes, tags, lang, preset (`multi_noise` + `fluid_levels.ring_4` = 10), drempelconstanten
Voortgang: 19:53 vondst: `ring_4/nether/terrain.json` van 59 was een kale string → registry laadt niet; gemeld (main fixte in d40d02a)
Voortgang: 20:00 antwoorden V1 A, V2 C, V3 A, V4 A verwerkt; terrein + surface-scripts, `giant_geodes`
Voortgang: 20:20 bruggen eigen gewicht (aansluiting op de duinen), deuren van de geodes naar open terrein
Voortgang: 20:30 plafondwacht (melding B-review); B/D groen, dam-check 0 wanden, ruimte-check
Voortgang: 20:35 commit 55c6ad8
Voortgang: 20:55 code-review 55c6ad8 (7 punten) afgehandeld, zie "Code-review (55c6ad8)"; B (780 tests) en D groen, metingen herhaald

## CODEBASE-delta
- **Klassen:**
  - `worldgen/biome/NetherRing4Biomes` ✅ 65: de drie Nether-4-biomes (`ALL`), heet/droog, geen features/carvers, placeholder-fog `0xD8CBA6` + muziek soul sand valley tot A (80), spawns als soul sand valley (skelet, ghast, enderman, strider).
  - `worldgen/feature/NetherRing4Features` ✅ `SPIRES_THRESHOLD` 0,18 / `HOLLOWS_THRESHOLD` −0,18 (terrein, surface en preset); nog geen features.
  - `worldgen/densityfunction/GiantGeodes` ✅ DF-type `corruptedadventure:giant_geodes` (65): de reuzengeodes van `geode_hollows`, −1 in de holte of een deur, +10 elders; `min(terrein, giant_geodes)` buiten de interpolatie; per cel de eerste poging die in de holtes ligt, vrij van de randen, met dichte koepel in de niet-geïnterpoleerde `shape` en minstens één deur naar open terrein; puur, `mapAll` alleen noises.
  - `worldgen/feature/GiantGeodeShape` ✅ de pure geometrie (gedeeld met de bekleding van 78): `Settings` (cel, kans, straal, vloer, regio, schil, deurmaten, `doors_max`), `Geode` (bol boven vlakke vloer, `hollow`, `wall`, `reach`, `domeTestPoints`), `Door`, `candidate`, `doors` (8 richtingen, ≥ 60° uit elkaar, eindigt 2 blokken in de opening; optioneel een `accept`-predicaat: een geweigerde deur laat zijn plek aan de volgende richting); `Settings.regionReachMax` (de hele geode waar de holtes gewicht hebben), 65 koepelpunten (`DOME_POINTS`).
  - `registry/ModDensityFunctionTypes` ✅ blok `// ring 4 — W-nether`: `giant_geodes`.
  - `worldgen/surface/RingDepthCondition` ✅ surface-conditie `corruptedadventure:ring_depth` (65): waar in de kolom de ring minstens `min_depth` blokken diep ligt (`RingColumnCache.boundaryDepth`, de maat van `ring_edge`); laat een surface-regel de `core`-weging van het terrein volgen. `registry/ModSurfaceConditions` ✅ blok `// ring 4 — W-nether`: `ring_depth`.
  - `datagen/tags/NetherRing4BiomeTags`, `datagen/lang/NetherRing4BiomeNames` ✅ gevuld (zie boven).
- **Databestanden:**
  - `density_function/ring_4/nether/*` (65, vervangt de placeholder `ring_4/nether.json`): zie "Gebouwd" punt 5 (`terrain`, `interpolated_shape`, `shape`, `spires_weight`, `hollows_weight`, `dunes_weight`, `edge`, `core`, `dunes`, `sea`, `bridge_weight`, `floor_height`, `ceiling_height`, `bridge_noise`, `bridges`, `pillar_noise`, `pillars`, `mass_top`, `tunnel_noise`, `tunnel_height`, `mass`, `dam`, `rough`, `geodes`); `region` (59) ongewijzigd.
  - `noise/nether_ring_4_*` (10): `dunes` (−5), `bridges_a`/`bridges_b` (−6), `ceiling` (−7), `pillars` (−4), `mass` (−6), `tunnels` (−7), `tunnel_height` (−6), `rough` (−4, [1, 0,7, 0,5]), `geode_cells` (−4, alleen hash).
  - `noise_settings/nether.json`: ring 4-surfacetak op index 5 (na Nether-3, vóór `hole` → lava), zie punt 6.
  - preset: Nether ring 4-source `multi_noise` (3 biomes), `fluid_levels.ring_4` = 10.
  - `tools/worldgen/nether_ring4_terrain.py`, `nether_ring4_surface.py` (W-nether; idempotent).
- **Tests:** `NetherRing4BiomesTest`, `NetherRing4JsonTest`, `GiantGeodeShapeTest`; `Ring3JsonTest` (ring 4-tak in de dam-check), `Ring4JsonTest` (Nether-test).
- **Valkuilen:**
  - een DF-bestand moet een getal of object zijn; een kale string (verwijzing) laat de registry falen (59 had dat; main d40d02a).
  - een term met gewicht w in de `max` (`w · (t + 2,5) − 2,5`) verschijnt pas boven w = 0,5: daarom hebben de bruggen een eigen, steiler gewicht (anders beginnen ze halverwege de helling, 20 blokken onder de duinrand).
  - bottenzand valt: plafondwacht (`stone_depth` ceiling 0 → gebleekt netherrack) vóór elk vallend vloerblok.
  - twee identieke inline `interpolated`-objecten zijn twee interpolators; laat beide takken één bestand noemen.

## Code-review (55c6ad8)
Opgepakt in de vervolgcommit:
1. **Deur- en koepelcontrole op het echte terrein:** `GiantGeodes` test vast/open op `shape` geïnterpoleerd over de Nether-ruiscel (4 × 8 × 4, trilineair zoals de noise chunk; `CELL_WIDTH`/`CELL_HEIGHT`, test tegen `noise_settings/nether.json`); een deur telt pas als het eindpunt 2 hoog open is; de koepel wordt op 65 punten getest (recht omhoog + 16 richtingen op 15/35/55/75°) i.p.v. 9.
2. **Cel-cache gedeeld:** de kopieën die de noise chunk van de bedrade functie maakt delen één `ConcurrentHashMap` van besliste cellen (een nieuwe bedrading = nieuwe map; per thread een klein geheugen ervoor). Gemeten (hollows2, 441 chunks): zonder delen 410 celbeslissingen, 1.715 ms; met delen ~40 beslissingen voor alle 5 testgebieden samen (~330 ms), ~140 inclusief de lavawandscan (2,6 s) — eens per cel per wereld.
3. **Calciet volgt de weging:** zie Gebouwd punt 6 (regio ≥ 0,20, `ring_depth` ≥ 32, vloeren eerst); gemeten: geen calciet- of kwartsvloeren meer (alle looppaden `bone_block`).
4. **Geen half-uitgesneden geodes:** de hele geode (wand in 8 richtingen + elke deur over zijn lengte en breedte) ligt waar regio ≤ `region_reach_max` (−0,145, dus `hollows_weight` > 0).
5. **Randcontrole diagonaal:** dezelfde punten (8 richtingen + de echte deuren) moeten `edge` = 0 hebben; een deur die de rand of de holtegrens raakt wordt geweigerd en de volgende richting probeert.
6. **Ruwheid alleen in de holtes** (zoals het plan): `rough` = (0 + 0,7 × `hollows_weight`) × ruis − 0,05; de dam-test rekent nog steeds tegen de echte ruis.
7. **Bruggen en richels overal `bone_block`**, ook in de overgangsband (zie punt 3).

Niet opgepakt (main): dedup `DewGeodes`/`RockPillars` (ring 2/3 vergrendeld) → backlog INT; `CODEBASE.md` en beslissingenlog → INT.

## Beslissingen
- 2026-10-13 · worldgen · Nether-4 regio-as: midden `bone_dunes` (~45 %), hoog `crystal_spires`, laag `geode_hollows`, drempels ±0,18 (gebruiker, V1 = A). Gemeten 44 / 21 / 36 %.
- 2026-10-13 · worldgen · Lava (Y 10) alleen zichtbaar in `crystal_spires`: zuilen die uit de lavazee rijzen met smalle bottenrichels en -bruggen; duinen en holtes dicht tot ~Y 38 (gebruiker, V2 = C). Beloopbaarheid gemeten: 84 % van de looppaden verbonden met de duinen, geen losse bruggen.
- 2026-10-13 · worldgen · `geode_hollows`: bolvormige holtes Ø 20–40 in de massa via een eigen DF-type, 1–3 ingangen (gebruiker, V3 = A).
- 2026-10-13 · worldgen · Spawns Nether-4 als soul sand valley: skeletten, ghasts, endermen, striders; geen piglins/hoglins (gebruiker, V4 = A).
- 2026-10-13 · worldgen · Bottenzand krijgt een plafondwacht in de surface (gebleekt netherrack direct boven lucht) (main, uit de review van B/68).
- (implementatie) Dam aan beide Nether-4-grenzen (ook 4|5: Nether-5 houdt 32); zuilen ruis −4/drempel 0,40, richel Y 32–42; bruggen Y 36–42 op twee nullijnen-ruisen, `bridge_weight` = clamp(40 · sea); geodes cel 128, vloer Y 42–48, schil 3, deuren 6 × 6 tot 24 voorbij de wand; geen carvers; placeholder-blokken zoals in punt 6; muziek soul sand valley.

## Backlog
- `worldgen` (INT1): `placeholder_nether_ring_4` wordt niet meer gebruikt (preset); weghalen samen met `placeholder_ring_4` (64): `PlaceholderBiomes`, `ModBiomes`, lang, `MapPalette`.
- `infra` (INT): `MapPalette` (terrainmap) heeft nog geen vaste kleuren voor `bone_dunes`, `crystal_spires`, `geode_hollows` (nu kleur uit de id-hash).
- `worldgen` (na INT1, W-nether): de echte Nether-4-blokken (68) in de surface: constanten in `nether_ring4_surface.py` (`bone_sand`, `bleached_netherrack`, `fossil_stone`, `purity_crystal_block`).
- `atmosphere` (80): `NetherRing4Biomes` neemt zijn effecten nog zelf (placeholder-fog); A zet een haak `BiomeAmbience.netherRing4(key)` en de bootstrap roept die aan.
- `worldgen` (78): de geodebekleding vindt de geodes via `GiantGeodeShape` + het DF uit de router (deuren hangen af van de `shape`-functie; de feature moet hetzelfde `GiantGeodes`-object gebruiken, bijv. een `find(root)` zoals `VolcanicField.find`).
- `infra`/`worldgen` (INT): `GiantGeodes` deelt opzet en cel-cache-patroon met `DewGeodes` (en `RockPillars`); een gedeelde basis kan pas als ring 2/3 niet meer vergrendeld zijn (review 65).
- `worldgen` (ter info): ~11 % van de looppaden in de spitsen (richels om zuilen zonder brug, zuilkoppen) is niet te bereiken; de bruggen zelf hangen samen.
