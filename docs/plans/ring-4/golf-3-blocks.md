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
