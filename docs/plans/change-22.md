# Change 22 — Palm-set (houtset + bladeren + sapling, zonder tree feature)

Scope: `blocks` — change 22

**Doel:** de palm-set van ring 2 bestaat als speelbare blokken: log, wood, gestripte log en wood, planks + houtset (trap, plaat, hek, poort, deur, valluik, drukplaat, knop), bladeren en een sapling. Met blockstates/models, placeholder-textures (eigen pixels uit het ring 2-texturescript), loot tables, tags, lang (en + nl), vuurkansen en een plek in de creative tab. De **palm tree feature** komt in change 23 (`worldgen`); tot dan groeit de sapling niet, zonder crash (zie §4). Definitieve textures in change 37.

Bron: `fase-1-wereld.md` §2 Ring 2 ("Palm-set: log, wood, planks, leaves, sapling + houtset; eigen tree feature" + definitie "houtset"), §1.4 (textures), bouwplan rij 22. Sjabloon: change 10 (dood-hout-set), `CODEBASE.md` valkuil "Blokken toevoegen".

---

## Afstemming (2026-10-01)

Afgestemd met 21 en 23 (basis: de aanbevolen opties).
- `Ring2Textures.java` bestaat (21) met `still`/`pulsing(glow)` en de ingebouwde controles; **niet zelf aanmaken** — ontbreekt het: stoppen en vragen.
- `ModFlowerPots`, `registerWithoutItem` en het tab-/testfilter bestaan (21 vraag 8 A): alleen uitbreiden met `potted_palm_sapling`. Alleen bij 21 vraag 8 B maakt deze change ze aan.
- **Grondregel palmzaailing (vraag 1 A, aangepast):** `super.mayPlaceOn(...) || state.is(ModTags.Blocks.DESERT_PLANT_SOIL)` — dezelfde grondtag als de woestijnflora van 21 (zand, rood zand, terracotta, aarde/gras, gebarsten klei). Eén grondregel voor alle ring 2-planten. Zonder die tag (21 vraag 7 B/C): `#dirt` + `#sand` zoals eerst gepland. Change 23 gebruikt `would_survive(palm_sapling)` ongewijzigd.
- Key `ModConfiguredFeatures.PALM_TREE` = `corruptedadventure:palm_tree`: change 23 registreert de feature onder precies die key (bevestigd in plan 23).
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37).

---

## Afhankelijkheden en aannames

Gecontroleerd in de code op 2026-10-01 (stand na change 17); changes 18–21 bij de start opnieuw nalopen.

- **Houtset-sjabloon uit change 10 bestaat en wordt 1-op-1 gevolgd:**
  - `registry/ModBlocks`: `register(name, supplier)` (blok + `BlockItem`), `registerDoor` (`DoubleHighBlockItem`), `entries()`; registratievolgorde = creative-tab-volgorde, secties per ring (`// ---- Ring 1 ----`, `// ---- Nether ring 1 ----`).
  - `block/StrippableLogBlock(Supplier<Block> stripped, Properties)` (bijl → gestript, as behouden); gestripte varianten zijn gewone `RotatedPillarBlock`.
  - `block/ModWoodTypes`: `BlockSetType DEAD_SET = BlockSetType.register(new BlockSetType(id("dead")))` en `WoodType DEAD` **zonder** `WoodType.register` (geen bord-textures). Palm komt hier naast.
  - `block/ModFlammables`: vuurkansen via `FireBlock#setFlammable` in `FMLCommonSetupEvent#enqueueWork`; log/wood 5/5, planks-familie 5/20 (zoals eik).
  - Datagen: `ModBlockStateProvider` (`logBlock`, `axisBlock`, `stairsBlock`, `slabBlock`, `fenceBlock`, `fenceGateBlock`, `doorBlockWithRenderType(…, "cutout")`, `trapdoorBlockWithRenderType(…, true, "cutout")`, `pressurePlateBlock`, `buttonBlock`), `ModItemModelProvider` (`blockItem`, `fenceInventory`, `buttonInventory`, valluik `_bottom`, deur `basicItem`), `ModBlockLootProvider` (`getKnownBlocks` = alle `ModBlocks` → elk blok een table), `ModBlockTagsProvider` / `ModItemTagsProvider` (kopie), `EnglishLanguageProvider` / `DutchLanguageProvider`, `ModDataMapProvider` (nu alleen `villager_types`).
  - `registry/ModTags.Blocks/Items.DEAD_LOGS` (`corruptedadventure:dead_logs`, in `#logs_that_burn`).
  - `registry/ModConfiguredFeatures`: alleen `ResourceKey`s, de features zelf komen uit datagen.
  - `src/test/…/registry/ModBlocksTest`: elk blok heeft een `BlockItem` met dezelfde naam; de creative tab toont eerst `ModBlocks.entries()` (via `block.get().asItem()`), dan de losse items. `ModCreativeTabs` doet `output.accept(block.get())` voor elk blok.
- **Changes 18–20** (`infra`, `conditions`, `atmosphere`) raken geen blok-registries.
- **Change 21** (`blocks`, kloofsteen/scorched sandstone/woestijnflora) is gebouwd vóór deze change. Aangenomen: hij opent in `ModBlocks` een sectie `// ---- Ring 2 ----` (na ring 1, vóór of na Nether-ring 1 — volg wat hij kiest) en een ring 2-texturescript **`tools/textures/Ring2Textures.java`** (zelfde opzet als `Ring1Textures`: `TEXTURES`, `still`/`pulsing`, `HAND_EDITED`, paletten bovenin, contactblad `build/texture-preview/ring2.png`). **Afstemming 2026-10-01:** change 21 maakt het script aan; deze change maakt het **niet** zelf aan — ontbreekt het bij de start, dan stoppen en vragen. `registerWithoutItem`, `ModFlowerPots` en het tab-/testfilter komen uit change 21 (vraag 8 A) en worden hier alleen uitgebreid; alleen als change 21 vraag 8 B koos, maakt deze change ze aan.
- **Houtset-definitie** (`fase-1-wereld.md` §2 Ring 2): deur, valluik, hek, poort, trap, plaat, knop, drukplaat + gestripte log en wood. **Geen borden, hangborden of boten** (backlog, change 10). Bladeren en sapling staan expliciet in de palm-set.
- **Recipes horen bij `items` (fase 2)** → backlog. Tag-gebaseerde vanilla-recipes (stokken, werkbank, kist, houten tools, houtskool) werken meteen via `#planks` / `#logs_that_burn`.
- **Tree feature:** change 23 (`worldgen`) registreert de configured feature onder de key die deze change vastlegt (`corruptedadventure:palm_tree`, §4).

---

## Plan

### 1. Registry-namen en blokken

Volgorde in `ModBlocks` (= creative tab), achteraan de ring 2-sectie, in de volgorde van de dood-hout-set met bladeren en sapling erachter:

| Registry-naam | Class | Basis (`ofFullCopy`) | Opmerking |
|---|---|---|---|
| `palm_log` | `StrippableLogBlock` | `oak_log` | bijl → `stripped_palm_log`; `mapColor` per as (zie onder) |
| `palm_wood` | `StrippableLogBlock` | `oak_wood` | bijl → `stripped_palm_wood` |
| `stripped_palm_log` | `RotatedPillarBlock` | `stripped_oak_log` | |
| `stripped_palm_wood` | `RotatedPillarBlock` | `stripped_oak_wood` | |
| `palm_planks` | `Block` | `oak_planks` | |
| `palm_stairs` | `StairBlock` | `oak_stairs` | basis = `palm_planks` |
| `palm_slab` | `SlabBlock` | `oak_slab` | |
| `palm_fence` | `FenceBlock` | `oak_fence` | |
| `palm_fence_gate` | `FenceGateBlock` | `oak_fence_gate` | `ModWoodTypes.PALM` |
| `palm_door` | `DoorBlock` | `oak_door` | `ModWoodTypes.PALM_SET`; via `registerDoor` |
| `palm_trapdoor` | `TrapDoorBlock` | `oak_trapdoor` | `PALM_SET` |
| `palm_pressure_plate` | `PressurePlateBlock` | `oak_pressure_plate` | `PALM_SET` |
| `palm_button` | `ButtonBlock` | `oak_button` | `PALM_SET`, 30 ticks |
| `palm_leaves` | `LeavesBlock` | `oak_leaves` | vanilla-verval (`distance`/`persistent`/`waterlogged`) op `#logs` |
| `palm_sapling` | `block/PalmSaplingBlock` (vraag 1) of `SaplingBlock` | `oak_sapling` | `ModTreeGrowers.PALM`; `stage` 0/1 |
| `potted_palm_sapling` | `FlowerPotBlock` (NeoForge-constructor met `Supplier`s) | `potted_oak_sapling` | **alleen bij vraag 3 A**; geen item |

- **Kaartkleur** (`mapColor`, blokgedrag): planks-familie, gestript en de kopse kant van de log `MapColor.SAND` (licht zandgeel hout); zijkant van log/wood `MapColor.TERRACOTTA_BROWN` (grijsbruine palmschors); bladeren houden `PLANT` van eik. Bijstellen mag in change 37 als het definitieve palet sterk afwijkt (dan als `blocks`-backlogpunt).
- **`block/ModWoodTypes`**: `BlockSetType PALM_SET = BlockSetType.register(new BlockSetType(id("palm")))` (standaard houtgeluiden, zoals `DEAD_SET`) en `WoodType PALM = new WoodType(id("palm"), PALM_SET)` — **niet** `WoodType.register` (zelfde reden als `DEAD`).
- **`block/PalmSaplingBlock extends SaplingBlock`** (alleen bij vraag 1 A of C): override `mayPlaceOn(state, level, pos)` → `super.mayPlaceOn(...) || state.is(ModTags.Blocks.DESERT_PLANT_SOIL)` (A, afgestemd: de grondtag van change 21 = zand, terracotta, aarde/gras, gebarsten klei; zonder die tag `|| state.is(BlockTags.SAND)`) of alleen `#sand` (C). NeoForge's `BushBlock.canSurvive` vraagt eerst `canSustainPlant` van de grond en valt bij `DEFAULT` terug op `mayPlaceOn`: bij de bouw in de 21.1.252-sources controleren dat zand `DEFAULT` geeft. Eigen `codec()` (`RecordCodecBuilder.mapCodec`: `TreeGrower.CODEC.fieldOf("tree")` + `propertiesCodec()`), omdat `SaplingBlock.CODEC` een `SaplingBlock` maakt.
- **`block/ModTreeGrowers`** (nieuw, één verantwoordelijkheid): `TreeGrower PALM = new TreeGrower("corruptedadventure:palm", Optional.empty(), Optional.of(ModConfiguredFeatures.PALM_TREE), Optional.empty())`. De `TreeGrower`-constructor meldt zich zelf aan in vanilla's naam-map (voor `TreeGrower.CODEC`); de naam moet dus uniek zijn → mod-namespace.
- **`registry/ModConfiguredFeatures`**: één nieuwe key `PALM_TREE = key("palm_tree")`, met commentaar "feature in change 23". Alleen een `ResourceKey`, geen datagen-entry (minimale placeholder richting `worldgen`, zie backlog).
- **Potplant (vraag 3 A):** `ModBlocks` krijgt `registerWithoutItem(name, supplier)` (of hergebruik uit change 21); de pot wordt in common setup aangemeld: `((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(PALM_SAPLING.getId(), POTTED_PALM_SAPLING)` in `enqueueWork`, in een nieuw klein class **`block/ModFlowerPots`** (zelfde patroon als `ModFlammables`, geregistreerd in `CorruptedAdventure`). `ModCreativeTabs` en `ModBlocksTest` filteren blokken zonder item (`asItem() == Items.AIR`) — anders gooit de tab "stack size must be exactly one".

### 2. Vuurkansen en compost
- **`ModFlammables`**: palm-log/wood/gestript 5/5; planks, trap, plaat, hek, poort 5/20; **`palm_leaves` 30/60** (zoals vanilla-bladeren). Deur, valluik, knop, drukplaat, sapling niet (vanilla).
- **`ModDataMapProvider`**: `NeoForgeDataMaps.COMPOSTABLES` → `palm_leaves` 0,3, `palm_sapling` 0,3 (vanilla-waarden voor bladeren/saplings).
- Brandstof gaat vanzelf via NeoForge's `furnace_fuels`-data map op `#logs`, `#planks`, `#wooden_*`, `#fence_gates`, `#saplings` (bij de bouw nagaan of `#saplings` erin staat).

### 3. Datagen
- **`ModBlockStateProvider`** (sectie palm, zelfde aanroepen als dood hout): `logBlock` ×2, `axisBlock` (wood rondom log-zijkant; stripped idem), `simpleBlock` planks, `stairsBlock`/`slabBlock`/`fenceBlock`/`fenceGateBlock`/`pressurePlateBlock`/`buttonBlock` op `palm_planks`, `doorBlockWithRenderType(…, "cutout")`, `trapdoorBlockWithRenderType(…, true, "cutout")`, en:
  - `palm_leaves`: vraag 2 A → `models().cubeAll("palm_leaves", block/palm_leaves).renderType("cutout_mipped")` (geen `tintindex`, net als dood gras); vraag 2 B → parent `minecraft:block/leaves` (met `tintindex` 0) + `cutout_mipped`.
  - `palm_sapling`: `models().cross("palm_sapling", block/palm_sapling).renderType("cutout")`, `simpleBlock` (de `stage`-state verandert het model niet).
  - `potted_palm_sapling` (vraag 3 A): parent `minecraft:block/flower_pot_cross`, texture `plant` = `block/palm_sapling`, `renderType("cutout")`.
- **`ModItemModelProvider`**: blockitems op het blockmodel; `fenceInventory`/`buttonInventory` op `palm_planks`; valluik `palm_trapdoor_bottom`; deur `basicItem` (`item/palm_door`); bladeren op het blockmodel; sapling plat (`item/generated`, `layer0` = `block/palm_sapling`, zoals vanilla). Geen itemmodel voor de pot.
- **`ModBlockLootProvider`**: `dropSelf` voor log/wood/gestript/planks/trap/hek/poort/valluik/drukplaat/knop/sapling; `createSlabItemTable` (plaat); `createDoorTable` (deur); `palm_leaves` → `createLeavesDrops(leaves, palm_sapling, NORMAL_LEAVES_SAPLING_CHANCES)` (vraag 4); pot → `createPotFlowerItemTable(palm_sapling)`.
- **`ModTags`**: `Blocks.PALM_LOGS` / `Items.PALM_LOGS` = `corruptedadventure:palm_logs`.
- **`ModBlockTagsProvider`**:
  - `corruptedadventure:palm_logs` = log, wood, gestripte log en wood; in `minecraft:logs_that_burn` (→ `#logs`: bladverval, `#mineable/axe`, houtskool).
  - `minecraft:planks`, `wooden_stairs`, `wooden_slabs`, `wooden_fences`, `fence_gates`, `wooden_doors`, `wooden_trapdoors`, `wooden_pressure_plates`, `wooden_buttons`.
  - `minecraft:leaves` (`palm_leaves`): geeft via vanilla-ketens `#mineable/hoe`, `#sword_efficient`, `#replaceable_by_trees` (nodig voor change 23), `#parrots_spawnable_on`, `#lava_pool_stone_cannot_replace` (bij de bouw de 1.21.1-tags nalopen).
  - `minecraft:saplings` (`palm_sapling`); `minecraft:flower_pots` (`potted_palm_sapling`, vraag 3 A).
  - `c:stripped_logs`, `c:stripped_woods`, `c:fence_gates/wooden` (zoals dood hout).
- **`ModItemTagsProvider`**: `copy` van `palm_logs`, `logs_that_burn`, `planks`, alle `wooden_*`, `fence_gates`, `leaves`, `saplings`, `c:stripped_*`, `c:fence_gates/wooden`.
- **Lang:**

| Key (blok) | en_us | nl_nl |
|---|---|---|
| `palm_log` / `palm_wood` | Palm Log / Palm Wood | Palmstam / Palmhout |
| `stripped_palm_log` / `_wood` | Stripped Palm Log / Wood | Gestripte palmstam / Gestript palmhout |
| `palm_planks` | Palm Planks | Palmhouten planken |
| `palm_stairs` / `_slab` | Palm Stairs / Slab | Palmhouten trap / plaat |
| `palm_fence` / `_fence_gate` | Palm Fence / Fence Gate | Palmhouten hek / poort |
| `palm_door` / `_trapdoor` | Palm Door / Trapdoor | Palmhouten deur / valluik |
| `palm_button` / `_pressure_plate` | Palm Button / Pressure Plate | Palmhouten knop / drukplaat |
| `palm_leaves` | Palm Leaves | Palmbladeren |
| `palm_sapling` | Palm Sapling | Palmzaailing |
| `potted_palm_sapling` | Potted Palm Sapling | Palmzaailing in pot |

### 4. Sapling zonder tree feature (tot change 23)
- `ModTreeGrowers.PALM` verwijst naar `corruptedadventure:palm_tree`, dat tot change 23 **niet bestaat** (vraag 5 A). Gedrag in 1.21.1: `SaplingBlock.advanceTree` zet `stage` 0 → 1; bij `stage` 1 roept hij `TreeGrower.growTree` aan, die de holder opzoekt met `registryOrThrow(CONFIGURED_FEATURE).getHolder(key).orElse(null)`, het NeoForge-event `BlockGrowFeatureEvent` met `null` vuurt en bij `null` **`false`** teruggeeft. De sapling blijft staan op `stage` 1. Bonemeal wordt wel verbruikt (vanilla: `isValidBonemealTarget` is altijd waar voor saplings).
- **Bouwstap (verplicht):** in de NeoForge 21.1.252-sources (`TreeGrower.growTree`, `EventHooks.fireBlockGrowFeature`) bevestigen dat een ontbrekende holder netjes `false` geeft en het event `null` accepteert. Wijkt het af (NPE), dan valt het plan terug op vraag 5 C (sapling zonder groei) en dat wordt gemeld.
- Geen log-spam: `getHolder` op een onbekende key logt niets.
- `/place feature corruptedadventure:palm_tree` geeft tot change 23 "unknown feature" — verwacht.
- **Overdracht naar change 23** (in `CODEBASE.md` bij `ModTreeGrowers` en in het change 23-plan):
  - de configured feature **moet** `ModConfiguredFeatures.PALM_TREE` gebruiken (geen nieuwe key);
  - palmbladeren vervallen op meer dan 6 blokken van een `#logs`-blok: bladeren van een brede kroon die verder van de stam hangen (met `persistent=false`) vervallen anders; `TreeFeature` zet de `distance` zelf goed;
  - `dirt_provider`: vanilla's `TrunkPlacer` vervangt zand onder de stam door het `dirt_provider`-blok (zand is geen `#dirt`) → bij vraag 1 A/C een zandprovider kiezen;
  - placement `would_survive` / `PlacementUtils.filteredByBlockSurvival(palm_sapling)` volgt de grondregel van vraag 1.

### 5. Placeholder-textures (eigen pixels, ring 2-script)
In `tools/textures/Ring2Textures.java` (zie aannames), eerste eenvoudige versie, vaste seed per bestandsnaam, alpha alleen 0/255. Paletten als constanten bovenin, werkpaletten (definitief in change 37):

| Bestand | Patroon |
|---|---|
| `block/palm_log.png` | grijsbruine schors met **horizontale** ringen/schubben (palmstam), geen verticale nerf |
| `block/palm_log_top.png` | licht zandgeel hout met vezelige stippen, 1 px schorsrand (palmhout heeft geen echte jaarringen) |
| `block/stripped_palm_log.png`, `block/stripped_palm_log_top.png` | licht zandgeel hout met fijne lengtevezels / kopse kant zonder schors |
| `block/palm_planks.png` | 4 planken, zandgeel, naden en versprongen stoten (zelfde indeling als `dead_planks`) |
| `block/palm_door_top.png`, `block/palm_door_bottom.png` | planken-frame met gaten (cutout) |
| `block/palm_trapdoor.png` | latwerk met gaten (cutout) |
| `block/palm_leaves.png` | vaste kleur (vraag 2 A): heldergroene, lange smalle bladlobben met gaten (cutout_mipped); bij 2 B grijswaarden voor de tint |
| `block/palm_sapling.png` | kleine palm: stammetje + 3–4 waaierbladen, rest transparant |
| `item/palm_door.png` | deur-silhouet |

11 PNG's. Wood en stripped wood gebruiken de log-zijkant rondom; trap/plaat/hek/poort/knop/drukplaat gebruiken `palm_planks`; de pot hergebruikt `palm_sapling`. Volgorde: **eerst het script, dan `runData`** (`ExistingFileHelper`). `CODEBASE.md` textures-regel en het contactblad `ring2.png` bijwerken.

### 6. Documentatie
- `CODEBASE.md`: `ModWoodTypes` (`PALM_SET`, `PALM`), nieuwe classes `PalmSaplingBlock`, `ModTreeGrowers`, (`ModFlowerPots`), `ModConfiguredFeatures.PALM_TREE` (alleen key), blokkentabel ring 2, tags (`palm_logs`, `leaves`, `saplings`), compost-data map, textures-lijst ring 2, testen; valkuilen: "sapling zonder feature groeit niet maar crasht niet", "potplant: blok zonder item, tab/test filteren", "bladeren: `cutout_mipped`, verval 6 blokken op `#logs`". Kopregel: change 22, volgende change 23.
- `fase-1-wereld.md` Ring 2: uitwerking palm-set (namen, grondregel sapling, bladkleur, pot, bladloot, gedrag vóór change 23).
- `02-beslissingenlog.md`: regels hieronder.
- `docs/backlog.md`:
  - `[items]` Recipes palm-set (log → planks, wood, trap, plaat, hek, poort, deur, valluik, knop, drukplaat). (bron: change 22)
  - `[worldgen]` Change 22 legt alleen de key `corruptedadventure:palm_tree` vast (`ModConfiguredFeatures.PALM_TREE`); change 23 registreert de feature onder die key, anders groeit de palmzaailing nooit. (bron: change 22)
  - `[items]` Idee: palmvrucht (dadels/kokosnoot) als extra bladdrop of voedsel in de droge ring. (bron: change 22) — alleen als vraag 4 dat niet al uitsluit.
  - `[textures]` Bij vraag 2 B: biome-tint van palmbladeren evalueren in change 37. (bron: change 22)

---

## Client/server-impact
- Blokken, items, `BlockSetType`, `TreeGrower` zijn common; NeoForge synct registry-ID's bij het joinen.
- Gedrag server-side: sapling-groei (random tick, bonemeal), bladverval (random tick), strippen (`getToolModifiedState`, ook client-voorspelling, zonder client-classes), vuurkansen, potplant-aanmelding in common setup (`enqueueWork`).
- **Vraag 2 A: geen client-code** — render types in de model-JSON (`cutout`, `cutout_mipped`), kleuren in de PNG's. **Vraag 2 B:** `RegisterColorHandlersEvent.Block`/`.Item` in `client/` (alleen `Dist.CLIENT`) → test S wordt verplicht.
- Geen netwerk, geen SavedData. Het texturescript staat buiten `src/` en zit niet in de jar.
- Worldgen-determinisme: niet geraakt (geen features in deze change).

## Tests
- **Script** — `java tools/textures/Ring2Textures.java` draait; tweede keer → `git status` schoon (deterministisch); alle nieuwe PNG's 16×16, alpha alleen 0/255.
- **B** — `./gradlew build` slaagt, incl. unit tests:
  - `ModBlocksTest` bijgewerkt: elk blok **met item** heeft een `BlockItem` met dezelfde naam; blokken zonder item zijn precies de potplanten (vraag 3 A); de creative tab slaat ze over.
  - nieuw `block/PalmSaplingBlockTest` (vraag 1 A/C): `mayPlaceOn` waar voor zand, rood zand, aarde, `dead_grass_block` (A ook terracotta en `cracked_clay`); onwaar voor steen (A) / aarde (C).
  - nieuw `block/ModTreeGrowersTest`: `TreeGrower.CODEC` encodeert `PALM` als `"corruptedadventure:palm"` en decodeert terug naar dezelfde instantie.
- **D** — `./gradlew runData` slaagt (loot voor elk blok, geen "texture does not exist"); output gecommit (`blockstates/`, `models/`, `loot_table/blocks/`, `tags/`, `data_maps/item/compostables.json`, lang).
- **S** — aanbevolen (blokgedrag + common setup; verplicht bij vraag 2 B): `tools/server-smoke-test.sh` start zonder fouten of client-class-meldingen. Via RCON: `setblock` van elk palmblok; `execute if block … #minecraft:logs` / `#minecraft:leaves` / `#minecraft:saplings`; `loot insert <kist> mine <pos> <tool>` voor bladeren (schaar → bladeren; zonder → af en toe sapling/stokken), plaat, deur, pot.
- **W-achteraf (in-game checklist, runClient, creative + survival)** — niet blokkerend:
  - [ ] Creative tab: palm-set achter de eerdere ring 2-blokken, namen in en/nl, geen pot in de tab.
  - [ ] Alle blokken plaatsbaar, geen paars-zwart; log/wood draaien mee met de as; bijl → gestript.
  - [ ] Deur/valluik/bladeren/sapling: transparantie klopt; hek verbindt met eikenhek; knop en drukplaat geven redstone.
  - [ ] Sapling: blijft staan op de grond van vraag 1, valt weg op ongeldige grond; bonemeal ×5 → **geen crash, geen groei**, server-log zonder exceptions (bevestigt §4); in de pot zetten (vraag 3 A).
  - [ ] Bladeren naast een palmstam vervallen niet; los geplaatste (niet-persistente) bladeren via `/setblock corruptedadventure:palm_leaves[persistent=false]` zonder stam vervallen (`/gamerule randomTickSpeed 100`).
  - [ ] Vuur: stam, planken en bladeren branden; compost: sapling/bladeren vullen de composter.

**Blokkerende handmatige test: nee.** Alles wat een crash of datafout kan geven, dekken B/D/S + RCON; de enige paden die een speler of random ticks nodig hebben (bonemeal op de sapling, bladverval) zijn vanilla-code die bij een ontbrekende feature `false` geeft (bouwstap §4 bevestigt dat in de sources). Change 23 hangt niet af van het uiterlijk. Dus **W-achteraf**.

## Risico's / aandachtspunten
- **`growTree` met een ontbrekende feature:** als NeoForge 21.1.252 toch een NPE geeft (event met `null`), crasht bonemeal op de server. Mitigatie: bouwstap §4 (sources lezen) vóór het schrijven van `ModTreeGrowers`; fallback vraag 5 C.
- **Naam-map van `TreeGrower`** is globaal (vanilla `GROWERS`): een dubbele naam overschrijft stil. Altijd met namespace (`corruptedadventure:palm`).
- **Zand als grond** (vraag 1 A/C) werkt via `mayPlaceOn`; als NeoForge's `canSustainPlant` voor zand iets anders dan `DEFAULT` teruggeeft, gaat dat vóór. Controleren en de unit test op `canSurvive` in een echte wereld in de W-lijst houden.
- **Potplant zonder item** breekt `ModBlocksTest` en de creative tab (lege stack gooit) als je niet filtert.
- **`cutout_mipped` via model-JSON:** NeoForge's model-render-type gaat vóór vanilla's "fast graphics = bladeren solid"-schakelaar; bladeren blijven dus ook bij Fast doorzichtig. Klein visueel verschil, geen crash; accepteren.
- **Kaartkleur/palet** kan in change 37 wijzigen; dan een `blocks`-backlogpunt, niet in de textures-change.
- **Bladverval en kroonvorm** zijn een randvoorwaarde voor change 23 (max. 6 blokken van een stam); doorgeven in het change 23-plan.
- Bestaande testwerelden: geen probleem (alleen nieuwe blokken, geen worldgen).

## Beslissingenlog (antwoorden 2026-10-01)
- `| 2026-10-01 | blocks | Palm-set = log, wood, gestripte log en wood, planks + houtset, palm_leaves, palm_sapling (geen borden/boten) | Houtset-definitie ring 2 |`
- `| 2026-10-01 | blocks | Palmzaailing groeit op #dirt + corruptedadventure:desert_plant_soil | Eén grondregel voor alle ring 2-planten; palmen direct op zand |`
- `| 2026-10-01 | blocks | Palmbladeren vaste kleur (geen biome-tint) | Geen client-code; lijn van dood gras (change 17) |`
- `| 2026-10-01 | blocks | Potplant palmzaailing: ja | Zoals elke vanilla-sapling |`
- `| 2026-10-01 | blocks | Bladloot palm zoals eikenbladeren | Vertrouwd; puur data |`
- `| 2026-10-01 | blocks | Palmzaailing verwijst vóór change 23 naar de nog niet bestaande feature corruptedadventure:palm_tree: bonemeal verbruikt, geen groei, geen crash | Change 23 registreert alleen de feature; geen blokwerk meer |`

---

## Open ontwerpvragen

### 1. Op welke grond groeit de palmzaailing?
- **A. `#dirt` + de grondtag `corruptedadventure:desert_plant_soil` van change 21 (zand, terracotta, gebarsten klei) (aanbevolen; afgestemd 2026-10-01, was `#dirt` + `#sand`).** Eén grondregel voor alle ring 2-planten. Palmen in `oasis_flats` staan op zand én aan groene oeverplekken; spelers kunnen ze overal in de woestijn planten. Eigen class `PalmSaplingBlock` (één override + codec). Change 23 kan de tree feature direct op zand plaatsen (`would_survive`) en kiest een zand-`dirt_provider`. Geen multiplayer-impact.
- **B. Alleen `#dirt` (vanilla).** Geen eigen class (gewone `SaplingBlock`). Maar palmen in een zandvlakte vragen dan in change 23 een aarde-plek onder elke boom of een placement zonder `would_survive`; spelers moeten aarde meeslepen in de woestijn — voelt onlogisch.
- **C. Alleen `#sand`.** Sterk thema ("palm = woestijn"), zelfde class als A. Spelers kunnen palmen niet in ring 1 op gras planten; oase-oevers met gras krijgen geen palmen tenzij er zand ligt.

Antwoord: **A** — `#dirt` + grondtag `corruptedadventure:desert_plant_soil` van change 21 (eigen `PalmSaplingBlock`).

### 2. Krijgen palmbladeren een biome-tint?
- **A. Nee, vaste kleur in de texture (aanbevolen).** Zelfde lijn als dood gras (beslissing change 17): geen client-code, geen test-S-plicht, overal hetzelfde groen. Ring 2-biomes hebben nog geen definitieve foliage-kleuren, dus een tint zou nu willekeurig zijn.
- **B. Vanilla-foliage-tint via `BlockColor`/`ItemColor` in `client/`.** Bladeren passen zich aan het biome aan (in ring 1 op gras geplant: groener); wel client-code, test S verplicht, en de tint hangt af van de foliage-kleur die change 23/32 per biome kiest. Grijze texture nodig.

Antwoord: **A** — geen biome-tint; vaste kleur in de texture.

### 3. Krijgt de palmzaailing een potplant-variant?
- **A. Ja, `potted_palm_sapling` (aanbevolen).** Zoals elke vanilla-sapling; spelers verwachten het. Kost één blok zonder item, een kleine `ModFlowerPots` in common setup, een filter in de creative tab en `ModBlocksTest`. Geen nieuwe texture.
- **B. Nee, naar de backlog.** Minder werk; rechtsklikken met de zaailing op een bloempot doet niets. Valt buiten de letterlijke houtset-definitie, dus verdedigbaar.

Antwoord: **A** — ja, `potted_palm_sapling` (uitbreiding van `ModFlowerPots` uit change 21).

### 4. Wat laten palmbladeren vallen?
- **A. Zoals eikenbladeren (aanbevolen):** schaar/Silk Touch → bladeren; anders sapling 1/20 (Fortune hoger) en stokken 1/50. Vertrouwd; genoeg zaailingen uit een paar palmen. Puur data.
- **B. Zeldzamer zaailingen** (bijv. 1/40, zoals jungle-bladeren): palmen voelen kostbaarder, past bij de schaarse oase; minder herplanten in multiplayer.
- **C. Zoals A plus een palmvrucht** (dadels/kokosnoot): leuk voor de uitdrogings-/voedselrol van de oase, maar een nieuw item hoort bij `items` (fase 2) → nu niet bouwen, alleen backlog; deze change bouwt dan A.

Antwoord: **A** — zoals eikenbladeren: schaar/Silk Touch → bladeren; anders sapling 1/20 (Fortune hoger) en stokken 1/50.

### 5. Hoe gedraagt de zaailing zich tot change 23?
- **A. `TreeGrower` naar de nog niet bestaande key `corruptedadventure:palm_tree` (aanbevolen).** Geen crash (vanilla geeft `false` bij een ontbrekende feature), bonemeal wordt verbruikt, de zaailing blijft staan. Change 23 hoeft alleen de feature onder die key te registreren; geen code meer aan het blok. Klein risico: verifiëren in de sources (§4).
- **B. Tijdelijk een vanilla-boom (bijv. jungle- of acaciaboom).** Zaailing groeit meteen (testbaar), maar geeft een verkeerde boom van eikenhout; change 23 moet de key omzetten en bestaande testwerelden houden vreemde bomen. Kans op vergeten.
- **C. Zaailing zonder groei** (geen `TreeGrower`, eigen `BushBlock` zonder bonemeal tot change 23). Nul risico in deze change, maar change 23 moet het blok omzetten naar een echte sapling (`blocks`-werk in een `worldgen`-change) en de blockstate (`stage`) verandert.

Antwoord: **A** — `TreeGrower` naar de (nog niet bestaande) key `corruptedadventure:palm_tree`; bonemeal verbruikt, geen groei, geen crash tot change 23.
