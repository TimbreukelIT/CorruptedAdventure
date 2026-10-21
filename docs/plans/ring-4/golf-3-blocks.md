# Golf 3 — stroom B/T/IT: 66c, 71, 66d

Branch `ring4/blocks-g3` (vanaf `aeebe14`). Drie pakketten, elk een eigen commit en categorie (CLAUDE.md §2):
**66c** `blocks` (`88b8430`), **71** `textures` (`46f8949`), **66d** `items` (commit met dit document).
Bron: `golf-3.md`, `fase-1-wereld.md` → Ring 4 "Golf 3 (2026-10-21)" en §1.4, `golf-2-textures.md`; backlog r. 272, 341,
343, 344, 390, 392, 411.

## Gedaan

### 66c — `blocks`
- **`lagoon_sand`** (`ModBlocks.LAGOON_SAND`, sectie "Golf 3 blocks" direct na `lagoon_glowgrass`, vóór Nether-4):
  `ColoredFallingBlock`, `ofFullCopy(SAND)` (sterkte, geluid, gereedschap van zand), kaartkleur `QUARTZ`, stofkleur
  `F2EEDC` (wit-crème, lichter dan bottenzand `E3DCC6`). Dropt zichzelf. Tags zoals vanilla `sand`: `#minecraft:sand`
  (en daarmee alles wat vanilla via `#sand` geeft: `bamboo_plantable_on`, `dead_bush_may_place_on`, `azalea_grows_on`,
  `overworld_carver_replaceables`, `camel_sand_step_sound_blocks`; en onze `ring_4_plant_soil` → `lagoon_plant_soil`,
  `desert_plant_soil`), direct: `azalea_root_replaceable`, `enderman_holdable`, `lush_ground_replaceable`,
  `rabbits_spawnable_on`, `sculk_replaceable`, `mineable/shovel`; `c:sands`. Item-tags `minecraft:sand` en `c:sands`
  (kopie van de bloktags). Lang "Lagoon Sand" / "Lagunezand".
- **`clawed_stone`**, **`clawed_deepslate`** (direct na `lagoon_sand`): gewone `Block` met `ofFullCopy(STONE)` resp.
  `ofFullCopy(DEEPSLATE)` (hardheid, blastweerstand, geluid; pikhouweel vereist); `simpleBlock` (cube_all, ook de
  deepslate-variant: net als `clawed_tidestone` geen zuil); loot `cobblestone` / `cobbled_deepslate`, Silk Touch het blok;
  `#mineable/pickaxe`. Lang "Clawed Stone" / "Bekraste steen", "Clawed Deepslate" / "Bekraste diepleisteen" (vanilla nl_nl:
  "Diepleisteen", zoals de ring 3-ertsen).
- Models wijzen naar `block/lagoon_sand`, `block/clawed_stone`, `block/clawed_deepslate`. In de 66c-commit staan daar
  tijdelijke kopieën van onze eigen `bone_sand.png` en `clawed_tidestone.png` (geen vanilla-pixels); 71 vervangt ze.
- Test `ModBlocksTest.ring4Golf3BlocksFollowTheDecorBlocksAndKeepTheirRock` (volgorde in de tab, valblok, sterkte,
  blastweerstand, geluid, gereedschap); `netherRing4FollowsRing4InOrderAndBehavesAsPlanned` telt nu vanaf `clawed_deepslate`.

### 71 — `textures` (`tools/textures/Ring4Textures.java`)
Gerichte polijstronde (geen volledige ronde) plus de drie nieuwe textures:
- **Verwelkte maanbloesem (r. 390):** schors `WITHERED_BARK` donker bruingrijs (`221D19…675A4D`, was neutraal grijs),
  hout `WITHERED_WOOD` rottend grijsbruin; `witheredLog` = de platen (nu `witheredPlates`) + een lengtevezel per plaat,
  twee rotgaten (donker, lichte rand rechtsonder) en wat grijsgroen korstmos (`WITHERED_LICHEN`). Log-top, gestript en
  gestripte top volgen via het palet.
- **`blighted_glowcap` (r. 392):** hoed bleek ziekelijk olijf-geel (`BLIGHT_CAP`), steel grijs, grijze schilferige
  plekken (`BLIGHT_LESION`, nieuw veld `lesion` in `Fungus`; `glowcap` ongewijzigd); gloed blijft geelgroen.
- **Bloesemkruin (`moonpetal_leaves`):** minder losse gaten (1/16 i.p.v. 1/9), een donkerlila rand onder elke klont
  (overhang), bloemetjes alleen op de kern van een klont, een stap lichter.
- **Tidestone in kliffen:** twee lagen van ongelijke dikte (6–7 en 9–10 rijen) en iets andere toon, iets zachtere
  lichtgradiënt: een klif wisselt een dunne lichte en een dikke donkere band af. Werkt door in `clawed_tidestone`,
  `mossy_tidestone`, `bubble_spring_side` (zijregel blijft groen).
- **Gifpoel naast gifgroen water:** `TOXIC_POOL` troebel olijf met zuurgele slierten (`24320A…A6B428`, glans `E8EE6A`),
  plus drie meedrijvende schuimvlokjes (`TOXIC_SCUM`, periodiek in de fase); water in `peat_moors`/`reed_marsh` is
  `6FC23A`/`5DB03A`. Stroom en emmer volgen via het palet.
- **Fossielsteen op een muur (r. 272):** kortere ribboog, een wervelpaar erbij, fragmenten over de tegel verspreid, twee
  vage onderbroken gelaagdheidslijnen; palet `FOSSIL_STONE` een stap grijzer/donkerder (stenen, polished, bricks en het
  frame van `purity_crystal_lamp` volgen).
- **`bone_block`-fossielen (r. 343):** `BONE_SAND` een stap warmer en donkerder, zodat vanilla-botblokken (ribbenkasten)
  en het witte lagunezand ertegen afsteken.
- **`particle/deep_silhouette` (r. 341):** alfarand ~1,5 px i.p.v. ~3 px, staartsteel, staartvin, vinnen, rugvin en
  kaak iets dikker/wijder: bij `setColor(0.02, 0.04, 0.06)` haalt ook het dunne deel volle alfa.
- **Nieuw:** `block/lagoon_sand` (wit-crème `LAGOON_SAND`, aquamarijnkorrels `LAGOON_SPECK`, roze-witte schelpjes
  `LAGOON_SHELL`), `block/clawed_stone` en `block/clawed_deepslate` (eigen basis `stoneBase`/`deepslateBase` in een
  vanilla-achtig palet `STONE`/`DEEPSLATE`, eigen ruis, geen vanilla-pixels; klauwsporen via `clawMarks`, de taal van
  `clawed_tidestone`, die er nu ook mee getekend wordt).
- **Contactblad:** `writeGolf3Preview` → `build/texture-preview/ring4-g3.png` (klif 12×6, klauwmuren, muren, zanden,
  botblokken op bottenzand/fossielsteen, verwelkte tuin, cascade vale, gifpoel naast water, glowcap/blighted dag/nacht,
  silhouet zoals in-game getint). Stand-ins (eigen pixels, gelabeld): vanilla zand, botblok, gifgroen water, steen- en
  deepslate-basis. `--golf3-sheet <texture root> <out>` tekent hetzelfde blad uit de PNG's van een andere root
  (vóór/na). Bladen: `build/texture-preview/ring4-g3-before.png` (texturen van `aeebe14`) en `ring4-g3-after.png`.

### 66d — `items`
- Nieuwe `datagen/ModRecipeProvider` (de eerste recipe-provider; in `DataGenerators` geregistreerd): smelten
  `lagoon_sand` → `minecraft:glass`, 0,1 xp, 200 ticks, categorie `BUILDING_BLOCKS` (kookcategorie `blocks`), id
  `corruptedadventure:glass_from_smelting_lagoon_sand`, ontgrendeld bij het oppakken van lagunezand (advancement).

## Tests
- 66c: `runData` groen (blockstates, models, loot, tags, lang); `build` groen (alle tests, `ModBlocksTest` 46/46).
- 71: script 2× → md5 van alle texturen identiek; lint 0; `checkGlowLayers`/zijregels groen (anders schrijft het script
  niets); `ring4-g3.png` en `ring4-g3-after.png` pixel-identiek (zelfde code, andere bron); `build` groen.
- 66d: `runData` groen (recept + advancement, verder `src/generated` ongewijzigd); `build` groen.
- Geen `runServer` nodig: geen gedrag, netwerk of client-code (blokken zonder logica, assets, data). Client/server-impact:
  geen client-class; alles data of standaard-`Block`.

## CODEBASE-delta
- `registry/ModBlocks`: `LAGOON_SAND` (`ColoredFallingBlock`, stof `LAGOON_SAND_DUST_COLOR` `F2EEDC`), `CLAWED_STONE`,
  `CLAWED_DEEPSLATE`, sectie "Golf 3 blocks (package 66c)" na `LAGOON_GLOWGRASS`, vóór Nether-4.
- Bloktabel: rij `lagoon_sand`, `clawed_stone`, `clawed_deepslate` | 4 (golf 3) | `ColoredFallingBlock`, `Block` | tags,
  loot en lang zoals hierboven; niet in `ring_4_stones` of steen-/erts-tags.
- `datagen`: `lagoonSandAndClawMarks()` in `ModBlockStateProvider`, `ModBlockLootProvider`, `ModBlockTagsProvider`;
  items in `ModItemModelProvider`; `ModItemTagsProvider` kopieert `#sand` → item `#sand` en `c:sands`; `Ring4BlockNames`.
  Nieuw `ModRecipeProvider` (de "🔲 recipes" in de datagen-regel wordt ✅ met dit ene recept).
- Gegenereerd: `data/minecraft/tags/block/{sand,azalea_root_replaceable,lush_ground_replaceable,rabbits_spawnable_on,
  sculk_replaceable}.json`, `data/minecraft/tags/item/sand.json`, `data/c/tags/{block,item}/sands.json`,
  `data/corruptedadventure/recipe/glass_from_smelting_lagoon_sand.json` + advancement.
- `tools/textures/Ring4Textures.java` (71): paletten `WITHERED_LICHEN`, `BLIGHT_LESION`, `TOXIC_SCUM`, `LAGOON_SAND`,
  `LAGOON_SPECK`, `LAGOON_SHELL`, `STONE`, `DEEPSLATE` (nieuw) en `WITHERED_BARK/WOOD`, `BLIGHT_CAP/STEM`, `TOXIC_POOL`
  (+ `TOXIC_SHEEN`), `BONE_SAND`, `FOSSIL_STONE` (gewijzigd); `Fungus.lesion`; schilders `witheredPlates`, `clawMarks`,
  `lagoonSand`, `clawedStone`, `clawedDeepslate`, `stoneBase`, `deepslateBase`; helper `indexOf`; contactblad
  `writeGolf3Preview` (+ `Source`, `fromRoot`, `standIn`/`STAND_INS`, `cell`, `drawGrid`, `wall`, `ribcage`, `POOLS`,
  `label`), `GOLF3_PREVIEW`, modus `--golf3-sheet`.
- Textures: 3 nieuwe PNG's (`block/lagoon_sand`, `clawed_stone`, `clawed_deepslate`), 20 vernieuwd.

## Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-21 | blocks | `lagoon_sand` volgt vanilla `sand` in tags (via `#sand` waar vanilla `#sand` gebruikt, anders direct), maar **niet** in `#smelts_to_glass` (blok- of itemtag) | Het glas komt uit het eigen recept van 66d; met de tag erbij zou vanilla's glasrecept het tweede pad zijn |
| 2026-10-21 | blocks | `lagoon_sand` ook in `c:sands` (niet `c:sands/colorless`) en item-`#sand` | Mods die zand zoeken vinden het; het is geen "gewoon" kleurloos zand |
| 2026-10-21 | blocks | `clawed_stone`/`clawed_deepslate` **niet** in `ring_4_stones`, `#base_stone_overworld` of erts-replaceables | `clawed_tidestone` staat in `ring_4_stones` omdat het getijdesteen is (erts-gastheer voor de tidestone-varianten); voor steen/deepslate zou die tag de tunnelbekleding de tidestone-ertsvariant laten kiezen, en de erts-tags laten een latere ader het klauwkras in erts veranderen (zie `Ring4OreFeatures`) |
| 2026-10-21 | blocks | Kaartkleur `QUARTZ`, stofkleur `F2EEDC` voor lagunezand; `clawed_deepslate` als kubus, geen zuil | Wit-crème op de kaart; zelfde vorm als `clawed_tidestone` |
| 2026-10-21 | textures | Tidestone: alleen ongelijke laagdikte + iets zachtere gradiënt | Een sterkere gradiënt, een gebogen laagnaad en een verticale voeg zijn geprobeerd en lazen op een klif als herhalend schub- of stippenmotief |
| 2026-10-21 | items | Recept-id `glass_from_smelting_lagoon_sand`, categorie `BUILDING_BLOCKS` | Vanilla-naamgeving (`<result>_from_smelting_<input>`) en vanilla's categorie voor glas |

## Backlog
- `[blocks]` Herhaling van tidestone op grote kliffen is per texture beperkt te verhelpen: willekeurig gespiegelde/
  gedraaide modelvarianten in de blockstate (zoals vanilla stone/deepslate) zouden het motief breken. Idee, niet gebouwd.
- `[worldgen]` (77b) `TunnelLiningFeature.rock()` telt `clawed_tidestone` als rots (via `ring_4_stones`), maar
  `clawed_stone`/`clawed_deepslate` niet (geen `#base_stone_overworld`): als de volgorde van naburige bekledingen
  ertoe doet, kiest W of `rock()` de klauwblokken expliciet meeneemt (of een tag) — bewust niet in 66c beslist.
- `[textures]` In-game beoordelen bij teststop 5c: rottere `withered_moonpetal_*`-stronken, olijfkleurige
  `blighted_glowcap` overdag, kruin van `moonpetal_leaves`, tidestone-kliffen, `toxic_pool` naast het water van
  `peat_moors`/`reed_marsh`, `fossil_stone`-muren, botblokken op bottenzand, het silhouet in diep water, lagunezand op
  de zandbanken (na 77b) en klauwkras in steen/deepslate.
- `[textures]` De stand-ins op `ring4-g3.png` (vanilla zand, botblok, gifgroen water, steen/deepslate-basis) zijn
  benaderingen; `VanillaCompare` voor ring 4 zou het echte naast elkaar tonen (alleen in `build/`).

## 66e — tidestone in gespiegelde en gedraaide varianten (`blocks`, 2026-10-21)
Besluit gebruiker 2026-10-21 (open vraag 1 van 71): zoals vanilla stone. `ModBlockStateProvider.mirroredAndRotated`:
`tidestone` = `cube_all` + `tidestone_mirrored` (`minecraft:block/cube_mirrored_all`), elk in 4 Y-rotaties (8 varianten,
gelijk gewicht); trap, plaat en muur ongewijzigd. Alleen client-rendering, geen server-impact. Tests: `runData`
(blockstate + 1 model), `build` groen.
- CODEBASE-delta: `ModBlockStateProvider.mirroredAndRotated(Block)`; gegenereerd `models/block/tidestone_mirrored.json`.

## 66f / 71c / 66g / 77c — lagunezandsteen (2026-10-21)
Besluit gebruiker 2026-10-21 (`02-beslissingenlog.md`): een eigen `lagoon_sandstone`-set zoals vanilla zandsteen, onder
een plafond in `crystal_lagoon` i.p.v. vanilla `sandstone`. Vier commits, elk één categorie: **66f** `blocks`
(`31921ed`), **71c** `textures` (`4aadbef`), **66g** `items` (`e173515`), **77c** `worldgen` (commit met dit stuk).

### Gedaan
- **66f `blocks`:** tien blokken, precies vanilla's zandsteenfamilie: `lagoon_sandstone` (+ `_stairs`, `_slab`, `_wall`),
  `cut_lagoon_sandstone` (+ `_slab`), `chiseled_lagoon_sandstone`, `smooth_lagoon_sandstone` (+ `_stairs`, `_slab`).
  Elk `ofFullCopy` van zijn vanilla-tweeling (sterkte, blastweerstand, geluid, pikhouweel vereist) met kaartkleur `QUARTZ`
  (als `lagoon_sand`), helper `ModBlocks.lagoon(Block)`. In de tab direct na `lagoon_sand`, vóór `clawed_stone` (volgorde
  zoals de scorched-set: blok, trap, plaat, muur, cut, cut-plaat, chiseled, smooth, smooth-trap, smooth-plaat).
  Models zoals vanilla: blok/trap/plaat `cube_bottom_top`-stijl (side/bottom/top), muur op de side, cut en chiseled
  `cube_column` met de top als einde (cut-plaat: side cut, boven/onder top), smooth overal de top. Loot: alles dropt
  zichzelf, dubbele platen twee. Tags zoals vanilla zandsteen: `#mineable/pickaxe`, `#stairs`, `#slabs`, `#walls`; het
  gewone blok ook `#overworld_carver_replaceables` en `#sculk_replaceable`; `c:sandstone/blocks|slabs|stairs` zoals de
  scorched-set (item-tags via de bestaande `copy`). Lang en_us + nl_nl volgens vanilla's nl_nl-patroon ("Zandsteen",
  "Zandstenen trap", "Gesneden zandsteen", "Gebeiteld zandsteen", "Glad zandsteen", "Gladde zandstenen plaat") met
  "lagune-": "Lagunezandsteen", "Lagunezandstenen trap/plaat/muur", "Gesneden lagunezandsteen", "Gesneden
  lagunezandstenen plaat", "Gebeiteld lagunezandsteen", "Glad lagunezandsteen", "Gladde lagunezandstenen trap/plaat".
  In de 66f-commit tijdelijk kopieën van onze eigen scorched-sandstone-PNG's; 71c vervangt ze.
- **71c `textures`** (`Ring4Textures.java`): palet `LAGOON_SANDSTONE` (de tinten van `LAGOON_SAND` plus één diepere voor
  groeven); `lagoon_sandstone_top` (rustige korrel, ook smooth en de einden van cut/chiseled), `lagoon_sandstone` (side:
  lichte band, donkere lijn, korte lagen, donkerder voet, zoals vanilla), `lagoon_sandstone_bottom`, `cut_lagoon_sandstone`
  (kader + één groef), `chiseled_lagoon_sandstone` (twee gegroefde banden met een gebeitelde **lotus** op een blad,
  symmetrisch: donkere groef, binnenin iets lichter, lichte lip eronder; masker `LOTUS_CARVING`). Aquamarijnkorrels en
  een schelpje van het lagunezand in de steen (`lagoonInclusions`; cut 1 korrel, chiseled geen). Contactblad
  `ring4-g3.png`: nieuwe rij (vijf 6×6-muren, een muur op lagunezand, lagunezand boven zandsteen boven een holte).
- **66g `items`** (`ModRecipeProvider.lagoonSandstone`): de 19 recepten van vanilla's zandsteenfamilie, zelfde vormen,
  aantallen, categorieën en unlocks: 4 `lagoon_sand` → 1 `lagoon_sandstone`; plaat (uit blok of chiseled), trap (uit
  blok, chiseled of cut), muur (`DECORATIONS`), cut 4→4, cut-plaat, chiseled uit 2 platen; smelten → smooth (0,1 xp,
  200 ticks); smooth-plaat en -trap; 9 steenzaagrecepten (plaat ×2, cut-plaat ×2 uit blok en uit cut, trap, muur, cut,
  chiseled, smooth-plaat ×2, smooth-trap). Ids zoals vanilla in onze namespace (`lagoon_sandstone`,
  `cut_lagoon_sandstone_slab_from_lagoon_sandstone_stonecutting`, …); eigen helper `stonecutting` omdat vanilla's
  `stonecutterResultFromBase` onder een kale string-id opslaat (→ `minecraft:`).
- **77c `worldgen`:** `ring4_surface.py` `FALLING`: `lagoon_sand` → `corruptedadventure:lagoon_sandstone` (was
  `minecraft:sandstone`); script gedraaid → `noise_settings/overworld.json` (4 blokken in de `crystal_lagoon`-tak, verder
  ongewijzigd). `Ring4BiomesTest` eist nu `lagoon_sandstone` als plafondwacht van lagunezand. Overige vanilla
  zandsteen in `crystal_lagoon`: geen. De zandbanken/atollen (`ShoalFeature`) bouwen vanaf de bodem op en vervangen alleen
  water en lucht, dus hangen niet; geen ring 4-feature gebruikt vanilla `sandstone` (alleen ring 1/2-features).

### Tests
- 66f: `runData` groen (10 blockstates, 19 block- en 10 itemmodels, 10 loot tables, tags, lang); `build` groen, nieuwe
  test `ModBlocksTest.lagoonSandstoneMirrorsVanillaSandstone` (klasse, sterkte, blastweerstand, geluid, gereedschap per
  tweeling) en de tabvolgorde in `ring4Golf3BlocksFollowTheDecorBlocksAndKeepTheirRock` (ModBlocksTest 47/47).
- 71c: script 2× → md5 van alle texturen en van `ring4-g3.png` identiek; lint 0; alleen de 5 nieuwe PNG's gewijzigd;
  `build` groen.
- 66g: `runData` groen (19 recepten + advancements); elk recept JSON-vergeleken met vanilla's (namespace/naam
  genormaliseerd): identiek, op één expliciete `"count": 1` in het smeltresultaat na (zelfde betekenis); `build` groen.
- 77c: script + `runData` + `build` groen (`Ring4BiomesTest` 8/8). Geen `runServer`: een andere stroom draait een server
  op deze machine; de wereldcheck (lagunezandsteen onder plafonds) hoort bij INT/teststop 5c.
- Client/server-impact: alleen standaard-`Block`/`StairBlock`/`SlabBlock`/`WallBlock`, data, assets en surface rules;
  geen client-class, geen netwerk.

### CODEBASE-delta
- `registry/ModBlocks`: `LAGOON_SANDSTONE`, `LAGOON_SANDSTONE_STAIRS`, `LAGOON_SANDSTONE_SLAB`, `LAGOON_SANDSTONE_WALL`,
  `CUT_LAGOON_SANDSTONE`, `CUT_LAGOON_SANDSTONE_SLAB`, `CHISELED_LAGOON_SANDSTONE`, `SMOOTH_LAGOON_SANDSTONE`,
  `SMOOTH_LAGOON_SANDSTONE_STAIRS`, `SMOOTH_LAGOON_SANDSTONE_SLAB` (sectie golf 3, na `LAGOON_SAND`); helper `lagoon(Block)`.
- Bloktabel: rij lagoon sandstone-set | 4 (golf 3) | `Block`/`StairBlock`/`SlabBlock`/`WallBlock` | zoals vanilla
  zandsteen; tags en lang zoals hierboven.
- `datagen`: `lagoonSandstone()` in `ModBlockStateProvider`, `ModBlockLootProvider`, `ModBlockTagsProvider`; items in
  `ModItemModelProvider` (muur via `wallInventory`); `Ring4BlockNames`; `ModRecipeProvider.lagoonSandstone` +
  `stonecutting` (de recipe-provider heeft nu 20 recepten).
- Gegenereerd: 10 blockstates, models, loot tables, 19 recepten + advancements (`building_blocks/`, `decorations/`),
  tags `mineable/pickaxe`, `stairs`, `slabs`, `walls` (blok + item), `overworld_carver_replaceables`,
  `sculk_replaceable`, `c:sandstone/{blocks,slabs,stairs}` (blok + item).
- `tools/textures/Ring4Textures.java`: palet `LAGOON_SANDSTONE`; schilders `lagoonSandstoneTop/Side/Bottom`,
  `cutLagoonSandstone`, `chiseledLagoonSandstone`, `lotusCarving` (+ `LOTUS_CARVING`), `lagoonInclusions`; contactblad
  `SANDSTONE_WALL`/`lagoonSandstoneWall`, `SANDSTONE_CEILING`. Textures: 5 PNG's (`block/lagoon_sandstone{,_top,_bottom}`,
  `cut_lagoon_sandstone`, `chiseled_lagoon_sandstone`).
- `tools/worldgen/ring4_surface.py` `FALLING` (lagoon sand → lagoon sandstone); `noise_settings/overworld.json`
  (4 blokken).

### Beslissingen
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-21 | blocks | Kaartkleur `QUARTZ` voor de hele set | Zoals `lagoon_sand`; vanilla zandsteen deelt ook de kaartkleur van zijn zand |
| 2026-10-21 | blocks | Tabvolgorde zoals de scorched-set, direct na `lagoon_sand` | Huisstijl van de eerdere zandsteenset; "naast lagoon_sand" |
| 2026-10-21 | blocks | `c:sandstone/*` zoals de scorched-set, niet `uncolored_*` | Net als `lagoon_sand` niet in `c:sands/colorless`: het is wit zandsteen, geen "gewoon" |
| 2026-10-21 | textures | Chiseled-motief: lotus op een blad | Opdracht noemde golf of lotus; de lotus leest op 16 px als bloem, een golf als streep |
| 2026-10-21 | items | Steenzaag-ids via eigen helper in onze namespace | Vanilla's helper zou `minecraft:`-ids maken |

### Backlog
- `[textures]` In-game beoordelen bij teststop 5c: de lagunezandsteenset als bouwblok (vooral de lotus op chiseled en
  de herhaling van de korrels in een grote muur) en onder plafonds in `crystal_lagoon`.
- `[worldgen]` (INT) `crystal_lagoon` onder plafonds (dunne zandbodem boven grotten/tunnels): lagunezandsteen i.p.v.
  vanilla zandsteen; `runServer`-start met de nieuwe surface rules.
- `[infra]` `ring4_surface.py` schrijft CRLF, de repo bewaart `overworld.json` met LF: na het script `sed -i 's/\r$//'`
  (anders lijkt het hele bestand gewijzigd). Zo ook bij 77c gedaan.
