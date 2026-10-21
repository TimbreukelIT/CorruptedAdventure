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

## CODEBASE-delta

## Beslissingen

## Backlog
