# Ring 5 golf 1 — stroom W-biomes: 86 (na INT1: 86b, 86c)

Scope: `worldgen` — pakket 86 in branch `ring5/biomes-g1`, worktree `P:/Projecten/Java/wt/r5-biomes-g1`, poort n = 2
(server 25572, RCON 25582), basis = de startcommit van `golf-1.md`. Na INT1, op main en sequentieel: **86b** (echte
ring 5-blokken in de surface) en **86c** (gespleten rots op de schotsen van de breukzone), elk een eigen worktree vanaf
de INT1-merge en een eigen `worldgen:`-commit.

Bronnen: `contract.md` §2, §4 (router, biomehaak V12-A), §8, §11; `golf-0.md` §7 (V12-A); `82-worldgen.md`;
`fase-1-wereld.md` → "Ring 5" (biomes, sfeer); sjabloon ring 4 (`docs/plans/ring-4/golf-1-biomes.md`, `Ring4Biomes`,
`tools/worldgen/ring4_surface.py`). `CODEBASE.md` kent golf 0 nog niet: lees de CODEBASE-delta's van `81-infra.md`,
`82-worldgen.md`, `81b-infra.md` erbij.

**Bezit** (`waves.md` §1/§5): `Ring5Biomes`, `Ring5BiomeModifiers`, `Ring5Features`, `Ring5Carvers`, `Ring5BiomeTags`,
`Ring5BiomeNames`, `Ring5SignText` (leeg tot 94), `noise_settings/overworld.json` (de ring 5-surfacetak), preset
`corrupted_world.json` overworld `rings.ring_5`, `tools/worldgen/ring5_surface.py` (nieuw), `SplitRockFeature`/
`SunderedReachFeatures` (86c). **Niet:** de router-sleutels in `ring_blend/*` en `density_function/ring_5/**`
(W-terrein); de sfeer-builders in `BiomeAmbience` (A, 98); `MapPalette` (INT).

**Doel 86:** de 6 ring 5-biomes bestaan en staan op de juiste plek (binnen 3, buiten 3), met kleuren, klimaat, spawns,
tags en lang; voorlopige surface met vanilla-blokken; de preset kent ze; `transitions.ring_4` blijft `sundered_reach`.

---

## 1. Werk 86

1. **`Ring5Biomes`** (bootstrap uit 81): lijsten `INNER` (`corrupted_wastes`, `glitch_fields`, `false_meadow`), `OUTER`
   (`void_scars`, `inverted_hills`, `shattered_expanse`), `ALL`, `SILENT` (`false_meadow`). Per biome een `Look` (kleuren,
   gras/blad, neerslag, downfall), zoals `Ring4Biomes`; de sfeerhaak (`BiomeAmbience.ring5`) is van A in 98.
   **Kleuren** (voorstel; sky / fog / water / waterFog / gras / blad; binnen grijs-violet, buiten naar zwart, contract §6):
   `corrupted_wastes` 5E5868/8C8A92/3E3A48/1A1820/7E7C80/6E6C72; `glitch_fields` 5A4E6E/857C94/4A3A66/1E1430/7C7488/
   6C6478; `false_meadow` exact vanilla plains (78A7FF/C0D8FF/3F76E4/050533/91BD59/77AB2F); `void_scars`
   1E1A26/3A3640/2A2632/0A080E/5E5C62/54525A; `inverted_hills` 221C2E/403A4A/2E2838/0C0A10/66606E/5A5462;
   `shattered_expanse` 08060C/141218/1A1820/040306/4E4C52/44424A. Sterren, dikke fog en de zwarte lucht overdag zijn
   client-sfeer van A (98), niet van de biome-JSON.
2. **Indeling** in de preset (§2).
3. **Klimaat:** temperatuur 0,5 (geen sneeuw), **geen neerslag** in alle 6 (ook `false_meadow`: een weide waar het nooit
   regent is "net fout"); downfall 0. Het ringweer van A rendert zijn eigen neerslag, ook zonder biome-neerslag.
4. **Spawns** (contract §2): geen dieren; monsters vanilla (`BiomeDefaultFeatures.monsters` zonder vleermuizen, tot fase
   4); **`false_meadow` niets** (geen monsters, geen dieren).
5. **Generatie:** **geen carvers** (`Ring5Carvers` leeg; grotten zijn 93), **geen features** (94/95), geen
   biome-modifiers (ertsen 95). Ring 5 krijgt dus ook geen vanilla-ertsen in golf 1.
6. **Surface** (`noise_settings/overworld.json`): één ring 5-tak (de 6 biomes) vóór vanilla's regels, uit het nieuwe script
   `tools/worldgen/ring5_surface.py` (idempotent, patroon `ring4_surface.py`; de rest van het bestand blijft byte-gelijk).
   Voorlopige vanilla-blokken (geen vallende blokken; plafondwacht als er toch een vallend blok in komt):
   | Biome | top / onder | opmerking |
   |---|---|---|
   | `corrupted_wastes` | `coarse_dirt` / `dirt` | kuilbodems `tuff` |
   | `glitch_fields` | `tuff` / `stone` | kubussen en lagen dezelfde blokken |
   | `false_meadow` | `grass_block` / `dirt` | dit is ook de eindstand (de "perfecte" vanilla-weide) |
   | `void_scars` | `tuff` / `stone` | scheurwanden `deepslate` (contrast; voidsteen-randen zijn 95) |
   | `inverted_hills` | `coarse_dirt` / `dirt` | onderkant omgekeerde heuvels `stone` |
   | `shattered_expanse` | `tuff` / `stone` | brokken en bruggen |
   Bedrock: de vanilla-regel (0) blijft; hij legt niets op lucht (V3-A).
7. **Tags** (`Ring5BiomeTags`): `is_ring_5` = de 6 (niet `sundered_reach`), `ring_5_inner` = `INNER`, `ring_5_outer` =
   `OUTER`, `silent` += `false_meadow` (W-nether zet de Nether-5-biomes erbij). `c:`-tags (keuze 86, noteren):
   voorstel `c:is_dry/overworld` alle 6, `c:is_spooky` alle 6, `c:is_wasteland` `corrupted_wastes`/`void_scars`/
   `shattered_expanse`, `c:is_dead` `corrupted_wastes`, `c:is_plains` `false_meadow`, `c:is_void` `shattered_expanse`
   (alleen als `c:is_void` geen spawn-/structure-bijwerking heeft; anders weglaten en noteren). Geen `minecraft:`-tags
   (geen `has_structure/*`, nooit `is_overworld`).
8. **Lang** (`Ring5BiomeNames`, en + nl; voorstel): Corrupted Wastes / Gecorrumpeerde Woestenij, Glitch Fields /
   Glitchvelden, False Meadow / Valse Weide, Void Scars / Voidlittekens, Inverted Hills / Omgekeerde Heuvels, Shattered
   Expanse / Verbrijzelde Vlakte.
9. **Preset:** `rings.ring_5` = de `multi_noise` van §2 (vervangt `placeholder_ring_5`; de key blijft geregistreerd tot de
   opruiming in golf 3). `transitions.ring_4` (`sundered_reach`) en `switch_at ["ring_5"]` ongemoeid.
10. **Controlelijst:** voeg aan het eigen plan (CODEBASE-delta) een lijst "Plekken die ring 5-biomes opsommen" toe, zoals
    ring 3/4 (preset, script, `Ring5BiomeNames`, tags, `MapPalette` (INT)).

## 2. Indeling (afspraak met W-terrein; W-biomes is eigenaar van deze getallen)

Assen (router van 82, plus de pinakel-as van 84): temperature = `climate_rot` (= `region_rot`, op het pinakelplateau −2),
humidity = `region_glitch`, weirdness = `band` (−1 binnen … 1 buiten; grens 0 = `Ring5Band.outer`), erosion = `breakup`
(0 vast … 1 void). Continentalness en depth vol [−2, 2]. Drempels uit de kwantielen van de ring 4-assen (zelfde
ruisinstellingen −10, [1, 0,5, 0,25]: q67 ≈ 0,125, q80 ≈ 0,244); **meet ze opnieuw** op de ring 5-ruis (5 seeds) en
noteer de gemeten waarden; wijkt iets > 0,02 af, dan de gemeten waarde nemen en main melden (W-terrein volgt).

| Band (weirdness) | Biome | Vak |
|---|---|---|
| binnen [−2, 0) | `glitch_fields` | humidity [0,125, 2] (~33 %) |
| binnen | `false_meadow` | humidity [−2, 0,125), temperature [0,244, 2] (~13 %) |
| binnen | `corrupted_wastes` | humidity [−2, 0,125), temperature [−2, 0,244) (~54 %) |
| buiten [0, 2] | `shattered_expanse` | erosion [0,6, 2] |
| buiten | `void_scars` | erosion [−2, 0,6), temperature [−2, 0) — ook het pinakelplateau (−2) |
| buiten | `inverted_hills` | erosion [−2, 0,6), temperature [0, 2] |

De aandelen zijn een voorstel (`golf-1.md` §7 P1); de gebruiker stuurt bij bij 6-pre-1/6a. Geen eigen overgangsbiome
in de overgangsband (harde wissel op 0, zoals de band in gameplay).

**Testen op stubs** (`waves.md` §3): tot de merge van 84 is `breakup` alleen echt in de kasteelgordels en bestaat
`climate_rot` nog niet. Test de indeling met **lokale ruis** in de unit test (een eigen `breakup`-functie, niet gecommit
in de preset) of na een rebase op `ring5/terrain-g1` (via main). In de preset verwijst temperature naar de router-input;
zolang 84 niet gemerged is, levert die `region_rot` (zelfde verdeling, alleen zonder pinakeloverride).

## 3. Na INT1

### 86b — echte ring 5-blokken in de surface (op main, na B + W-terrein + W-biomes)
**Status:** gebouwd 2026-10-23 op `ring5/86b` (worktree `P:/Projecten/Java/wt/r5-86b`, poort 2), klaar voor review en merge door main.
- `ring5_surface.py`: constanten naar de blokken van B: `corrupted_wastes`, `inverted_hills` → `corrupted_grass_block` /
  `corrupted_dirt`; `glitch_fields`, `void_scars`, `shattered_expanse` → `corrupted_stone` (top) / `corrupted_stone`;
  kuilbodems en lage plekken in `corrupted_wastes` → `corrupted_sand` (met plafondwacht: boven lucht `corrupted_stone`);
  `false_meadow` blijft vanilla gras/aarde.
- **Basisgesteente:** in alle 6 biomes wordt de vanilla-steen van de kolom `corrupted_stone` (ook onder `false_meadow`:
  wie graaft, vindt dat het niet klopt), met dezelfde deepslate-grens als ring 4.
- Test: elke biome een onvoorwaardelijk top- en diepteblok; geen vallend blok boven lucht; `false_meadow` top =
  `minecraft:grass_block`. Kaarten opnieuw (main).

### 86c — gespleten rots naar de breukzone (V8-A, na 85)
- `SplitRockFeature`/`SunderedReachFeatures`: plaatsing op `ZoneFracture.landmarksWithin` (opgeheven, vlakke schots om de
  ~650) in plaats van de landbruggen; hoogte uit `Landmark.topY` (niet uit `Column.surface()`, backlog decor D r. 396).
- `IslandField.landBridgesNear` (deprecated sinds 85) weghalen (toestemming main, `golf-1.md`), de `@Disabled`-asserts in
  `DecorDLandmarksTest` herschrijven naar de schotsen.
- Test: elke landmark krijgt precies één rots; geen rots buiten zone 4→5; determinisme van de plaatsing (puur uit de
  accessor).

## 4. Client/server-impact
Data (biomes, preset, surface, tags, lang) en één datagen-script: server-side worldgen; de client krijgt de biomes via de
registry-sync en rendert kleuren. Geen client-class, geen payload, geen `PROTOCOL_VERSION`.

## 5. Tests (licht: B, D, unit)
- `Ring5BiomesTest`: bootstrap = precies de lijsten; kleuren per biome verschillend; geen neerslag; spawns (`false_meadow`
  leeg, de rest alleen monsters); geen carvers en geen features.
- `Ring5RegionSharesTest`: de preset noemt precies `ALL`; de vakken dekken het vlak per band zonder overlap; aandelen op de
  echte ruis (3 seeds, ± 3 pt) met een lokale `breakup`; het pinakelplateau (temperature −2, erosion 0) → `void_scars`.
- Surface: één ring 5-tak vóór vanilla, elke biome een onvoorwaardelijk top- en diepteblok; de rest van
  `overworld.json` byte-gelijk.
- `Ring5JsonTest`: alleen de assert "preset ongewijzigd (placeholder)" voor de overworld aanpassen (de Nether-assert is
  van W-nether); `Ring3JsonTest` (switch_at) groen.
- Tags: `is_ring_5` zonder `sundered_reach`; `ring_5_inner`/`_outer`; `silent` bevat `false_meadow`.
- D: `src/generated` = 6 biome-JSON's, tags, lang (gecommit).

**Af als (86):** B, D en unit tests groen; S-smoke op een verse wereld met `/ca terrainmap 8900 0 1400 4` (biome-kaart:
de binnenband toont drie biomes, de buitenband de drie andere); PNG's in `build/test/maps/golf-1/`.

## Voortgang
- 2026-10-21 20:44: worktree `P:/Projecten/Java/wt/r5-biomes-g1` op `ring5/biomes-g1` vanaf `757523bf`; `setup.sh W-biomes 2`.
- 2026-10-21 20:55: 86 geschreven (biomes, tags, lang, preset, surface-script, tests); runData groen.
- 2026-10-21 21:05: gerichte tests groen (gemeten drempels, aandelen); volledige build loopt.
- 2026-10-21 21:15: B groen (1.160 tests, 0 fouten); D: runData na de laatste datagen-wijziging, output gecommit;
  S-smoke verse wereld `r5-86` (poort 25572): log 0 ERROR, geen client-class; `/ca ring` en `/locate biome` vinden alle 6
  biomes; `/ca terrainmap 8900 0 1400 4` → `build/test/maps/golf-1/top_overworld_8900_0_r1400_s4_{biome,height}.png`:
  binnenband `corrupted_wastes`/`glitch_fields`/`false_meadow`, buitenband `void_scars`/`inverted_hills`;
  `shattered_expanse` alleen in de kasteelgordels (`breakup` is buiten de gordels een stub tot 84; `/locate` vindt hem daar).
- Review: de geforkte `/code-review medium` keek in de hoofdmap (main's `CODEBASE.md`, 476199f7) in plaats van deze
  worktree en zag de diff van 86 niet; daarom een eigen review van de diff (correctheid: gekwantiseerde vakgrenzen,
  surface-volgorde ring 4 → ring 5, idempotentie van beide scripts, spawns/tags tegen het contract): geen bevindingen open.
- 2026-10-21 21:20: 86 gecommit op `ring5/biomes-g1`. 86b/86c pas na INT1.
- Review 86 (main): volgorde `inverted_hills` (boven → onderkant → diepte), `Ring5JsonTest.presetRing5Sources`,
  expliciete check in `Ring5Biomes.spawns`, en een router-invariant in `Ring5RegionSharesTest`: elke as waarop een
  `multi_noise` splitst, schakelt hard (`switch_at`) op een grens zonder overgangsbiome. Erosie (ring 5 = `breakup`)
  heeft dat nog niet: die ene test staat `@Disabled` tot INT1.
- 2026-10-23 12:40: 86b: worktree `P:/Projecten/Java/wt/r5-86b` op `ring5/86b` vanaf `d5e4e859`; `setup.sh W2 2`.
- 2026-10-23 12:55: 86b gebouwd: `ring5_surface.py` (blokken van 88, plafondwacht, basisgesteente), `Ring5BiomesTest`; script idempotent en stabiel na `ring4_surface.py`/`ring3_surface.py` (byte-gelijk). B groen (1.282 tests, 0 fouten, 2 overgeslagen); D zonder wijziging in `src/generated` (de surface is handgeschreven JSON); S-smoke verse wereld `r5-86b` (poort 25572): log 0 ERROR, geen client-class; `/ca ring` ok; `/ca terrainmap 8900 0 1400 4` → `build/test/maps/86b/` (biomes/hoogte; 86b raakt de vorm niet). Blokscan van 472 gegenereerde chunks (7 plekken per biome + 16 plekken rond de binnenband): zie Beslissingen 86b.
- 2026-10-23 13:10: review (medium, 7 punten): basisgesteente zonder biome-opzoeking buiten de randstroken, `is_base_rock` herkent de hele regel, test zonder NPE en over de hele boom; kuilregel en scheurwanden zoals het plan (open vraag aan main: zandaandeel). B opnieuw groen (1.282 tests), D zonder wijziging, S-smoke opnieuw schoon met dezelfde blokscan.

## CODEBASE-delta
- `worldgen/biome/Ring5Biomes` (86): lijsten `INNER` (`corrupted_wastes`, `glitch_fields`, `false_meadow`), `OUTER`
  (`void_scars`, `inverted_hills`, `shattered_expanse`), `ALL`, `SILENT` (`false_meadow`); record `Look` (palet,
  gras, blad) per biome; temperatuur 0,5, geen neerslag, downfall 0; spawns: vanilla-monsters zonder grotspawns
  (`BiomeDefaultFeatures.monsters(95, 5, 100)`, geen vleermuizen/glow squids), geen dieren, `false_meadow` niets; geen
  carvers, geen features. Kleuren nog in `Ring5Biomes` (er is geen `BiomeAmbience.ring5`; A verhuist ze in 98).
- `datagen/tags/Ring5BiomeTags`: `is_ring_5` = `ALL`, `ring_5_inner`/`_outer`, `silent` += `false_meadow`; `CONVENTION`:
  `c:is_dry/overworld` en `c:is_spooky` alle 6, `c:is_wasteland` (`corrupted_wastes`, `void_scars`, `shattered_expanse`),
  `c:is_dead` (`corrupted_wastes`), `c:is_plains` en `c:no_default_monsters` (`false_meadow`), `c:is_void`
  (`shattered_expanse`).
- `datagen/lang/Ring5BiomeNames`: en + nl.
- Preset `corrupted_world.json`: `rings.ring_5` = `multi_noise` (6 vakken, §2; `placeholder_ring_5` niet meer in de
  preset, de key/biome blijft geregistreerd tot de opruiming in golf 3). `transitions.ring_4` = `sundered_reach`
  ongewijzigd.
- `tools/worldgen/ring5_surface.py` (nieuw, idempotent): de ring 5-tak van `noise_settings/overworld.json` direct ná de
  ring 4-tak (vóór vanilla), de rest byte-gelijk.
- Tests: `Ring5BiomesTest` (bootstrap, klimaat, kleuren, spawns, generatie, preset, tags, lang, surface-tak),
  `Ring5RegionSharesTest` (vakken in vanilla's gekwantiseerde ruimte precies één keer, vanilla's lookup = de regel van §2,
  pinakelplateau, gemeten kwantielen, aandelen P1, echte ring met lokale `breakup`); `Ring5JsonTest`: de
  overworld-assert van de preset is nu `multi_noise` (Nether-assert ongewijzigd, van W-nether).
- **Valkuil (nieuw, voor CODEBASE):** `multi_noise`-vakken mogen geen gekwantiseerde waarde delen. De codec kwantiseert
  `(long) (float · 10000)` (afkappen naar nul); een gedeelde grens is een gelijke stand, en vanilla's lookup kiest dan
  op basis van het vorige resultaat van de thread (niet deterministisch). Daarom liggen de bovengrenzen op
  `drempel − 0,00005` (positief) of `−0,0001` (negatief), zodat buren aangrenzende gehele getallen zijn
  (`Ring5RegionSharesTest.theBoxesTileThePlaneExactlyOnce`). Ring 4 doet het op de bandgrens al zo (`−0,0001`).

- **86b:** `tools/worldgen/ring5_surface.py` met de blokken van 88 (`GRASS`, `DIRT`, `STONE`, `SAND`), `put` met plafondwacht
  (`FALLING`: `corrupted_sand` → `corrupted_stone` op `stone_depth` ceiling 0, zoals ring 4) en `ring_5_base_rock`: de laatste
  top-level surface-regel, ná vanilla's deepslate-gradient (`ring_reach` ring 5 → `biome_is` de 6 → `corrupted_stone`).
  Per biome: `corrupted_wastes`/`inverted_hills` `corrupted_grass_block`/`corrupted_dirt` (kuilbodems en lage plekken
  onder Y 76 `corrupted_sand`; onderkant `inverted_hills` `corrupted_stone`); `glitch_fields`/`void_scars`/
  `shattered_expanse` `corrupted_stone`; scheurwanden `void_scars` blijven `deepslate`; `false_meadow` vanilla gras/aarde.
  `Ring5BiomesTest`: blokken per biome, wacht vóór elk `corrupted_sand`, geen vanilla-standins buiten `false_meadow` en de
  scheurwanden, nieuw `theBaseRockIsCorruptedStoneBelowAllSixBiomes`.
### Plekken die ring 5-biomes opsommen (controlelijst, 86)
Java leest `Ring5Biomes.ALL`/`INNER`/`OUTER`/`SILENT` (bootstrap, tags, `Ring5BiomesTest`, `Ring5RegionSharesTest`).
Handgeschreven JSON en scripts kunnen dat niet; een nieuwe ring 5-biome moet ook in:
- preset `corrupted_world.json`: een vak in `rings.ring_5` (de vakken dekken per band het vlak precies één keer, in
  gekwantiseerde waarden; `Ring5RegionSharesTest`);
- `tools/worldgen/ring5_surface.py` (`RING_5`, `SURFACES`), dan het script draaien;
- `Ring5BiomeNames` (en + nl), `Ring5BiomeTags.CONVENTION`;
- `MapPalette` (kaartkleur; INT);
- de vormdrempels van W-terrein (`golf-1-terrain.md` §1.3) als de indeling verandert.

## Beslissingen
- **Drempels gemeten** (5 seeds, raster 401 over 128.000², `Ring5RegionSharesTest`): `region_glitch` q67 = 0,1251,
  `region_rot` q80 = 0,2480, q50 = −0,0000. Alles binnen 0,02 van het plan, dus de planwaarden blijven: 0,125 / 0,244 / 0
  (geen melding aan W-terrein nodig). Aandelen op de echte ruis (3 seeds): `glitch_fields` 33,6 %, `false_meadow` 13,8 %,
  `corrupted_wastes` 52,6 %; buiten (breakup 0) `void_scars` 50,0 %, `inverted_hills` 50,0 %.
- **Gekwantiseerde grenzen:** bovengrens van het lagere vak = drempel − 0,00005 (0,12495, 0,24395, 0,59995), resp.
  −0,0001 bij 0 (weirdness binnen, temperature `void_scars`); zie de valkuil hierboven. Gevolg: een bandwaarde in
  (−0,0001, 0) telt als buitenband (`Ring5Band.outer` zegt binnen); dat is < 0,02 blok.
- **Klimaat** als constanten in plaats van per `Look` (alle 6 gelijk: geen neerslag, downfall 0).
- **Spawns:** `BiomeDefaultFeatures.monsters(95, 5, 100, false)` (de monstergewichten van `commonSpawns`), zonder
  `caveSpawns` (vleermuizen en glow squids: niets leeft hier, en ring 5 is droog).
- **Kleuren in `Ring5Biomes`** (`BiomeAmbience.colours`): er is nog geen sfeerhaak `BiomeAmbience.ring5`; geen mood,
  loop, particles of muziek tot A (98).
- **`c:`-tags:** het voorstel van §1.7, plus `c:no_default_monsters` voor `false_meadow` (klopt: geen monsters).
  `c:is_void` mag: NeoForge zelf hangt er geen gedrag aan (bron 21.1.252 nagelopen; alleen andere mods zouden het
  kunnen lezen).
- **Nederlandse namen in zinshoofdletters** zoals ring 4 ("Gecorrumpeerde woestenij", "Valse weide", "Omgekeerde
  heuvels", "Verbrijzelde vlakte").
- **Surface 86 (voorlopig):**
  - `corrupted_wastes`: kuilbodems en lage plekken = bovenblok onder Y 76 (dode grond rond Y 78, kuilen 2–4 diep, V4-A/83)
    → `tuff`; geen eigen DF nodig. 86b leest dezelfde regel voor `corrupted_sand`.
  - `void_scars`: scheur- en kraterwanden = vanilla `minecraft:steep` → de hele steile kolom boven het voorlopige
    oppervlak `deepslate` (het bovenste deel van een wand tot in de void).
  - `inverted_hills`: onderkant = de 3 blokken boven lucht (`stone_depth` ceiling, offset 2) `stone`, ná het bovenblok
    en vóór de surface-diepte (review 86: een dunne rand houdt `coarse_dirt` bovenop).
  - Geen vallend blok in 86, dus geen plafondwacht; de test verbiedt zand/grind in de tak tot 86b er een wacht bij zet.
- **Plaats van de tak:** direct na de ring 4-tak (`ring4_surface.py` voegt in na ring 3, dus de volgorde blijft bij
  het opnieuw draaien van beide scripts; nagelopen, byte-gelijk).

- **86b (implementatiekeuzes):**
  - **Basisgesteente** als één top-level regel achteraan (ná vanilla's deepslate-gradient Y 0–8, de enige deepslate-grens
    die ring 4 heeft): elke steen van de kolom die geen andere regel krijgt (onder de surface-diepte, onderkanten van
    overhangen) wordt `corrupted_stone`; onder Y 0 blijft het deepslate, tussen 0 en 8 het verloop. `ring_reach` ring 5
    vóór de `biome`-conditie (die zoekt de biome per blok op; `RingReachCondition`). Review: waar ring 4 (zone 4→5,
    `sundered_reach`) en de void (`void_edge`) niet binnen bereik zijn, is elke biome een ring 5-biome: daar
    `corrupted_stone` zonder opzoeking (zoals `ring_3_deep`); de opzoeking alleen in die randstroken. Zelfde blokscan als
    vóór de wijziging (identieke tellingen).
  - **Plafondwacht:** alleen `corrupted_sand` valt; de wacht staat in `put` (op elk vallend blok van `FALLING`), niet alleen
    in de kuilregel, zodat een later vallend blok vanzelf een wacht krijgt.
  - **Wat blijft vanilla:** `false_meadow` gras/aarde (plan), en de scheurwanden van `void_scars` `deepslate` (het plan noemt
    ze niet bij 86b; contrast, de voidsteen-randen zijn 95).
  - **Kuilregel ongewijzigd** (onder Y 76; backlog: 83 leverde geen kuilen-DF). Gemeten in de S-smoke: `corrupted_sand` =
    24 % van de bovenblokken van `corrupted_wastes` (16 plekken rond de binnenband, 31.179 kolommen), lokaal tot 84 % (bij
    8900, 0) waar `ring_5_hills` de grond op `Ring5Inner.MIN_TOP` 70 klemt: daar is de woestenij vooral zandvlakte, niet
    alleen kuilbodem. Beoordelen bij 6a; anders de regel aan `pitAt` hangen (backlog).
  - Blokscan: 0 `corrupted_sand` boven lucht; geen vanilla-steen in ring 5-biomes behalve 3.795 blokken aan de grens met
    `sundered_reach` (99,9 % binnen één quart van een `sundered_reach`-quart: de zoom-ruis van de biome per blok). Die
    gespikkelde naad (steen/corrupted steen per blok) is dezelfde als bij elke biomegrens in de surface (gras/zand);
    bewust zo gelaten, het plan kiest per biome.
## Backlog
- [worldgen] W-terrein (eigenaar router), INT1: `switch_at ["ring_5"]` in `ring_blend/overworld_erosion` (anders mengt
  `breakup` in zone 5→void met de void en schuiven de biomegrenzen), dan `Ring3JsonTest` (zegt nu "erosion mengt")
  aanpassen en `Ring5RegionSharesTest.erosionSwitchesHardAtABoundaryWithoutATransitionBiome` aanzetten.
- [worldgen] 86b/83: als 83 een eigen kuilen-DF levert, de "lage plekken"-regel van `corrupted_wastes` (onder Y 76) daarop
  laten lezen.
- [worldgen] 95: `void_scars`-wanden via `scars` (W-terrein) in plaats van vanilla's `steep` (die kijkt alleen binnen de
  chunk, zie `ring4_surface.py`), samen met de voidsteen-randen.
- [infra] golf 3: `placeholder_ring_5` (biome, lang, `MapPalette`, `PlaceholderBiomes`) opruimen; hij staat niet meer in
  de preset.
- [atmosphere] 98: de kleuren van `Ring5Biomes.LOOKS` naar de sfeerhaak `BiomeAmbience.ring5` (zoals ring 4,
  `Ring4Atmosphere`).
