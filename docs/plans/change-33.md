# Change 33 — Nether-ring 2-blokken: druipzwam-set, doorweekt netherrack, sporenmos, slijmstalactieten, gloeischimmel, zwamlamp

Scope: `blocks` — change 33

**Doel:** de natuur- en bouwblokken van Nether-ring 2 (doorweekte schimmel-Nether) bestaan als speelbare blokken: de **reuzenzwam-set** (druipzwam-steel en -hyfen, gestript, planks + houtset, hoed), **doorweekt netherrack**, **sporenmos** (spreidt via random ticks over doorweekt netherrack, zoals gras/nylium), **slijmstalactieten** (dripstone-gedrag: punt, dikte-states, vallen, schade), **lichtgevende schimmel** en de **zwamlamp**. Met blockstates/models, placeholder-textures van eigen pixels (ring 2-texturescript), loot tables, tags, lang (en + nl), compost/vuurwaarden en een plek in de creative tab. Plaatsing in de wereld volgt in change 35 (`worldgen`: zwamkathedralen, reuzenzwam-feature, sporenmos-eilanden), sfeer (druppels, sporen) in change 36, definitieve textures in change 37. De dauwbron en het dauwkristal zijn change 34.

Bron: `fase-1-wereld.md` §2 Ring 2 → "Nether-tegenpool — Nether-ring 2" ("reuzenzwam-set (druipzwam-steel, -hoed, planks + houtset), doorweekt netherrack, sporenmos (spreidt zoals nylium), slijmstalactieten (dripstone-gedrag), lichtgevende schimmel + zwamlamp"; client/server: "sporenmos-spreiding: vanilla-mechanismen / random ticks, server-side"), definitie "houtset" (§2 Ring 2), §1.4 (textures, creative tab), bouwplan rij 33 (B, D, S). Beslissingenlog 2026-09-30 "Nether-ring 2-blokken". Sjablonen: change 10 (dood-hout-set, `DeadGrassBlock`-spreiding), change 15 (Nether-blokken, tag- en loottests via RCON), change 21/22 (ring 2-blokken, `Ring2Textures.java`, bloempotten, compost).

---

## Afstemming (2026-10-01)

Afgestemd met 34, 35, 36 en 37 (basis: de aanbevolen opties).
- **Namen zijn het contract:** `dripshroom_*`, `dripshroom_cap`, `dripshroom_lamp`, `glow_mold`, `slime_stalactite`, `soaked_netherrack`, `spore_moss`. Plan 35 (dat `dripcap_*`, `glowing_mold`, `fungus_lamp` aannam) en 36/37 zijn hierop aangepast. Biome-naam 35 vraag 9 A volgt het prefix: `dripshroom_forest` + `spore_caves`. Kiest de gebruiker in vraag 1 een ander prefix, dan volgen 35-9 en 36/37 mee.
- **Blok-tag `corruptedadventure:fungal_growths` wordt hier aangemaakt** (`blocks`): stam, hyfen, gestripte varianten, hoed, zwamlamp, slijmstalactiet, gloeischimmel. Change 34 voegt de dauwkristalstadia toe; change 35 gebruikt hem voor zijn determinismeregel (geen placeholder-tag in 35). Opnemen in `ModTags.Blocks.FUNGAL_GROWTHS`, de tag-provider en `CODEBASE.md`.
- **Vraag 4 B:** de key `corruptedadventure:dripshroom_fungus` registreert change 35.
- **PNG-telling gecorrigeerd:** 26 (niet 27).
- **`spore_moss_side`-placeholder:** onder de mosrand exact de pixels van `soaked_netherrack` (regel §1.4; 37 controleert).
- `Ring2Textures.java` bestaat (21); placeholders `still`; animatie/emissive = 37 vraag 4.
- **Geodes (35 vraag 6 = B, keuze gebruiker):** change 35 bouwt een eigen deterministische `dew_geode`-feature met **alleen bestaande blokken** uit 33/34 (schil `soaked_netherrack` + binnenlaag `spore_moss`/`glow_mold`, binnenwand `dew_source` met knoppen). Deze change hoeft daarvoor niets extra te leveren.
- Teststop: geen (W-achteraf in de gebundelde eindcheck ring 2 + Nether-2, teststop 3 ná change 37).

---

## Afstemming na bouw 18–30c (2026-10-05)

Nagelopen tegen de stand na `3abf6f3` (changes 0–30, 23b, 23c, 30b, 30c gebouwd; 31 en 32 volgen eerst). Antwoorden blijven staan; dit zijn feiten uit de code.
- **`ModBlocks`** heeft de secties `// ---- Ring 1 ----`, `// ---- Nether ring 1 ----` en `// ---- Ring 2 ----` (nu inclusief de zout-set en de ertsen). `// ---- Nether ring 2 ----` komt **achteraan**, ná de ring 2-sectie (de creative-tab-volgorde volgt de registratievolgorde). `registerWithoutItem` en `registerDoor` bestaan; `ModFlowerPots`, `ModFlammables` en `StrippableLogBlock` ook.
- **`ModWoodTypes`** kent `DEAD_SET`/`DEAD` en `PALM_SET`/`PALM` met de **korte** constructors (`new BlockSetType(id)`, `new WoodType(id, set)`, zonder eigen geluiden). `DRIPSHROOM_SET`/`DRIPSHROOM` met de crimson-velden en nether-hout-geluiden is de eerste set met eigen geluiden: de vanilla-constructor `WoodType(String, BlockSetType, SoundType, SoundType, SoundEvent, SoundEvent)` is publiek, controleer de signatuur bij de bouw.
- **Compost:** `ModDataMapProvider` heeft `NeoForgeDataMaps.COMPOSTABLES` al (`palm_leaves`, `palm_sapling` 0,3); deze change voegt `dripshroom_cap`, `dripshroom_lamp`, `glow_mold` toe aan hetzelfde `builder(...)`.
- **Textures:** `tools/textures/Ring2Textures.java` bestaat (≈ 1.380 regels; `still(...)`, `pulsing(name, glow, painter)`, `Animation.STILL`, `check` vóór het schrijven, `HAND_EDITED` leeg, helpers o.a. `Cells`, `valueNoise`, `soot`, `twig`, kristal-/zoutpatronen). Er staat nog **geen** Nether-ring 2-blok in het script; de "Nether ring 2"-sectie komt achteraan `TEXTURES`. `ModBlockStateProvider.crystalCluster(Block)` bestaat (voor 34) maar is `private`.
- **Valkuil random ticks:** geforceloade chunks krijgen zonder speler geen random ticks (en bonemeal via dispenser werkt wel): het sporenmos-gedrag komt dus in de W-lijst, de dispenser-test blijft in S.
- **Sjabloon spreiding:** `DeadGrassBlock` bestaat zoals beschreven (`isAreaLoaded`-guards, `LightEngine.getLightBlockInto`).
- **"Levendig" geldt ook voor de Nether** (teststop 2b/2c-les). Het blokkenpakket van dit plan heeft geen kleine bodemplant: de vloer van het woud is mos met zuilen, stalactieten en schimmel op de wanden. Zie vraag 9.

### Gevolgen van antwoord 9 (2026-10-05)
- **9 A:** één extra blok `spore_sprouts` achteraan de Nether ring 2-sectie (20 blokken in plaats van 19): kleine plant, kruismodel `cutout`, plat item, staat op `spore_moss`/`soaked_netherrack` (eigen `canSurvive`/grondtag; niet `#nylium`), licht 0, geen random tick, geen eigen drop-gedrag buiten zichzelf met schaar/hand (`dropSelf`), `#replaceable`, in `fungal_growths`, compost 0,3, vuurkans 0. **Bonemeal:** `SporeMossBlock.isValidBonemealTarget` wordt `true` voor een vrije plek erboven (kans, in vlekken) en plant `spore_sprouts`; dit vervangt de regel "bonemeal op sporenmos doet niets" van antwoord 5 A voor sporenmos zelf en sluit de backlogregel af. Texture `block/spore_sprouts.png` (+1 PNG, 27) in `Ring2Textures.java`, lang en/nl ("Spore Sprouts" / "Sporenspruiten"), loot `dropSelf`. Plaatsing in de wereld: change 35.

## Afhankelijkheden en aannames

Gecontroleerd in de code op 2026-10-01 (stand na change 17, commit `7714b77`), ververst op 2026-10-05 (zie het blok hierboven). **Changes 18–30c zijn gebouwd; 31 en 32 volgen nog vóór deze change.** Wat hieronder uit 31/32 komt is aanname en wordt bij de start nagelopen (echte namen uit `CODEBASE.md` volgen).

- **Blok-infrastructuur (gebouwd, change 10/15):** `registry/ModBlocks` (`register` = blok + blockitem, `registerDoor`, `entries()`, volgorde = creative tab, secties `// ---- Ring 1 ----`, `// ---- Nether ring 1 ----`), `block/StrippableLogBlock(Supplier<Block>, Properties)`, `block/ModWoodTypes` (`DEAD_SET` geregistreerd, `WoodType DEAD` bewust niet), `block/ModFlammables` (`enqueueWork`), `registry/ModTags` (`Blocks`/`Items`/`Biomes`, `common(path)`), datagen-providers (`ModBlockStateProvider`, `ModItemModelProvider`, `ModBlockLootProvider` met `getKnownBlocks` = alle `ModBlocks`, `ModBlockTagsProvider`, `ModItemTagsProvider` (`copy`), `EnglishLanguageProvider`, `DutchLanguageProvider`), `ModBlocksTest`.
- **Sjabloon random-tick-spreiding (gebouwd):** `block/DeadGrassBlock#randomTick` — eigen familie (dood gras ↔ verdorde aarde), `isAreaLoaded`-guards, `LightEngine.getLightBlockInto` voor "bedekt". Sporenmos volgt dezelfde opbouw.
- **Gebouwd (change 21; 2026-10-05 bevestigd):** sectie `// ---- Ring 2 ----` in `ModBlocks` ná Nether-ring 1; helper `ModBlocks.registerWithoutItem`; `block/ModFlowerPots` (bloempotten in `enqueueWork`); `ModCreativeTabs` en `ModBlocksTest` slaan blokken zonder item over; **texturescript `tools/textures/Ring2Textures.java`** (zelfde opzet als `Ring1Textures`: `TEXTURES`, `still`/`pulsing`, vaste seed per bestandsnaam, `HAND_EDITED`, paletten bovenin, contactblad `build/texture-preview/ring2.png`). Ontbreekt het script bij de start: **stoppen en vragen** (het hoort bij change 21).
- **Gebouwd (change 22; 2026-10-05 bevestigd):** `ModWoodTypes.PALM_SET`/`PALM` (korte constructors), `NeoForgeDataMaps.COMPOSTABLES` in `ModDataMapProvider` (alleen uitbreiden).
- **Gebouwd (changes 24/29):** hebben `Ring2Textures.java` uitgebreid; deze change voegt een eigen blok "Nether ring 2" toe aan hetzelfde script.
- **Textures Nether-ring 2 → `Ring2Textures.java`.** Afspraak "één texturescript per ring" (opdracht ring 2, change 21 §5 noemt 33/34 expliciet), en de Nether-tegenpool hoort bij zijn ring (ring 1: zielkristal staat in `Ring1Textures.java`; change 37 = "ring 2 + Nether-ring 2"). Geen apart `NetherRing2Textures.java`.
- **Changes 18–20, 23, 25–28, 30–32** raken geen blok-registries; deze change hangt alleen naast hun registraties.
- **Houtset-definitie** (`fase-1-wereld.md` §2 Ring 2): deur, valluik, hek, poort, trap, plaat, knop, drukplaat + gestripte log en wood; geen borden/hangborden/boten (backlog). Voor een zwam heten log/wood vanilla-stijl **stem/hyphae** (vraag 2).
- **Recipes horen bij `items` (fase 2)** → backlog. Tag-gebaseerde vanilla-recipes (stokken, werkbank, kist, houten tools) werken meteen via `#planks`.
- **Change 35** gebruikt de registry-namen en tags uit deze change (zie §8 "Overdracht").
- Lijst hieronder = **aanbevolen opties**; bij een ander antwoord groeit/krimpt hij.

---

## Plan

### 1. Registry-namen en blokken (`registry/ModBlocks`, nieuwe sectie `// ---- Nether ring 2 ----` ná ring 2)

Volgorde (= creative tab): grond, druipzwam-set, hoed en lamp, schimmel, stalactiet. Change 34 (dauwbron, dauwkristal) komt daarna in dezelfde sectie. Prefix `dripshroom_` = vraag 1 A.

| Registry-naam | Class | Basis (`ofFullCopy`) | Opmerking |
|---|---|---|---|
| `soaked_netherrack` | `block/SoakedNetherrackBlock` (`extends Block implements BonemealableBlock`) | `netherrack` | vraag 6 A: geluid `SoundType.MUD`, `MapColor.TERRACOTTA_BROWN`; bonemeal → sporenmos (vraag 5 A) |
| `spore_moss` | `block/SporeMossBlock` (`extends Block implements BonemealableBlock`) | `crimson_nylium` | `MapColor.WARPED_NYLIUM`-achtig grijsgroen (`COLOR_GRAY`/`TERRACOTTA_GREEN`, bij de bouw kiezen), `SoundType.NYLIUM`, `randomTicks()` |
| `dripshroom_stem` | `StrippableLogBlock` | `crimson_stem` | bijl → `stripped_dripshroom_stem` |
| `dripshroom_hyphae` | `StrippableLogBlock` | `crimson_hyphae` | bijl → `stripped_dripshroom_hyphae` |
| `stripped_dripshroom_stem` | `RotatedPillarBlock` | `stripped_crimson_stem` | |
| `stripped_dripshroom_hyphae` | `RotatedPillarBlock` | `stripped_crimson_hyphae` | |
| `dripshroom_planks` | `Block` | `crimson_planks` | |
| `dripshroom_stairs` / `_slab` | `StairBlock` / `SlabBlock` | `crimson_stairs` / `_slab` | basis = `dripshroom_planks` |
| `dripshroom_fence` / `_fence_gate` | `FenceBlock` / `FenceGateBlock` | `crimson_fence` / `_fence_gate` | poort met `ModWoodTypes.DRIPSHROOM` |
| `dripshroom_door` / `_trapdoor` | `DoorBlock` / `TrapDoorBlock` | `crimson_door` / `_trapdoor` | `DRIPSHROOM_SET`; deur via `registerDoor` |
| `dripshroom_pressure_plate` / `_button` | `PressurePlateBlock` / `ButtonBlock` | `crimson_pressure_plate` / `_button` | `DRIPSHROOM_SET`, knop 30 ticks |
| `dripshroom_cap` | `Block` (vraag 3 A) | `nether_wart_block` | hoed: schoffel, geluid `WART_BLOCK` |
| `dripshroom_lamp` | `Block` | `shroomlight` | zwamlamp: licht 15, schoffel, geluid `SHROOMLIGHT` (vraag 8) |
| `glow_mold` | `GlowLichenBlock` (vanilla-class, eigen instantie; vraag 8 A) | `glow_lichen` | multiface, `lightLevel(GlowLichenBlock.emission(7))`, `MapColor.GLOW_LICHEN` |
| `slime_stalactite` | `block/StalactiteBlock` (vraag 7 A) | `pointed_dripstone` | geluid `SoundType.SLIME_BLOCK`, `MapColor.COLOR_LIGHT_GREEN`, `offsetType(XZ)`, `dynamicShape()`; geen `randomTicks()` |

19 blokken, alle met blockitem (deur: `DoubleHighBlockItem`). Bij vraag 4 B komen er `dripshroom_fungus` + `potted_dripshroom_fungus` bij.

**Kaartkleur** (blokgedrag, `mapColor`): stam/hyfen zijkant donker grijsgroen, planks-familie en gestript lichter (bij de bouw een passende `MapColor` kiezen; bijstellen mag in change 37 als `blocks`-backlogpunt, zoals change 22).

### 2. Gedrag (common code, alleen server-effect)

**`block/ModWoodTypes`** (vraag 2 A):
- `BlockSetType DRIPSHROOM_SET = BlockSetType.register(new BlockSetType(id("dripshroom"), …))` met de velden van vanilla `BlockSetType.CRIMSON` (nether-hout-geluiden voor deur, valluik, drukplaat, knop; `canOpenByHand`, wind charge, pijlen zoals crimson).
- `WoodType DRIPSHROOM = new WoodType(id("dripshroom"), DRIPSHROOM_SET, SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN, SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN)` — **niet** `WoodType.register` (geen bord-textures; zelfde reden als `DEAD`).
- Generieke opbouw: change 51 (rijpzwam-set) komt hier naast.

**`block/SporeMossBlock`** (vraag 5 A; zelfde opbouw als `DeadGrassBlock`):
- `randomTick`:
  1. **Bedekt** (`!canStayMoss`: het blok erboven blokkeert alle licht volgens `LightEngine.getLightBlockInto(...) >= level.getMaxLightLevel()` — zoals vanilla `NyliumBlock.canBeNylium`, niet afhankelijk van lichtniveau, want de Nether is donker): als `isAreaLoaded(pos, 1)` → `soaked_netherrack`.
  2. Anders, als `isAreaLoaded(pos, 3)`: **één** poging (gras doet er 4) op een willekeurig doel binnen 3×5×3 (`offset(rnd(3)−1, rnd(5)−3, rnd(3)−1)`, zoals gras): is het doel `soaked_netherrack` en zou sporenmos er niet bedekt zijn → `spore_moss`.
  3. **Spreidt nooit naar vanilla-netherrack** (geen woekering naar Nether-ring 1 of netherrack-bouwwerken).
- Geen lichteis, geen dimensie-eis (meegenomen sporenmos spreidt ook in de overworld, maar alleen over doorweekt netherrack).
- `BonemealableBlock`: `isValidBonemealTarget` → **false** (geen vegetatie op sporenmos in deze change; backlog). Bonemeal wordt dan niet verbruikt.
- Eigen `codec()` (`simpleCodec`).
- Waarom geen `NyliumBlock`-subclass: vanilla `NyliumBlock` hardcodet `Blocks.NETHERRACK` (terugval) en crimson/warped-vegetatie (bonemeal).

**`block/SoakedNetherrackBlock`** (vraag 5 A + 6 A):
- `BonemealableBlock`, zoals vanilla `NetherrackBlock`: geldig doel als een buur (3×3×3, zelfde regel als vanilla) `spore_moss` is en het blok erboven geen licht blokkeert; `performBonemeal` → `spore_moss`. Vanilla `NetherrackBlock` hardcodet crimson/warped nylium, dus eigen class.
- Geen random tick (niet `randomTicks()`).
- Niet in `#minecraft:infiniburn_overworld`/`_nether`/`_end`: vuur erop dooft zoals op gewone grond (nat), in tegenstelling tot netherrack.

**`block/StalactiteBlock`** (vraag 7 A) — **generiek**, ook bedoeld voor de ijsstalactieten van Nether-ring 3 (change 51):
- **Aangepaste kopie** van vanilla `PointedDripstoneBlock` (1.21.1): `VERTICAL_DIRECTION` (`BlockStateProperties.VERTICAL_DIRECTION`), `DRIPSTONE_THICKNESS` (vanilla-enum `DripstoneThickness`: `tip_merge`, `tip`, `frustum`, `middle`, `base`), `WATERLOGGED`; vormen per dikte, `canSurvive` (hangt aan een stevige onderkant / staat op een stevige bovenkant of op hetzelfde blok), `updateShape` (dikte herberekenen, stalactiet zonder steun → scheduled tick → valt als `FallingBlockEntity`), `getStateForPlacement` (richting + samengevoegde punten), `fallOn` (stalagmiet-punt: valschade +2 en ×2, vanilla `damageSources().stalagmite()`), vallende stalactiet doet schade (`setHurtsEntities`, `damageSources().fallingStalactite`), `onProjectileHit` (trident breekt hem), `Fallable#onBrokenAfterFall` (geluid via level event 1045), `isPathfindable` false, `getMaxHorizontalOffset`.
- **Weggelaten:** groei (`randomTick` → geen `isRandomlyTicking`), druppelen/cauldron vullen, `animateTick`-particles (druppel-particles → change 36 / backlog).
- **Waarom een kopie i.p.v. een subclass:** de statische helpers van `PointedDripstoneBlock` controleren `state.is(Blocks.POINTED_DRIPSTONE)` (dikte, samenvoegen, vallen); een subclass zou zijn eigen blokken niet herkennen. **Bouwstap:** de 21.1.252-sources van `PointedDripstoneBlock` nalezen en elke `Blocks.POINTED_DRIPSTONE` vervangen door `state.is(this)`; overal waar `Blocks.DRIPSTONE_BLOCK` of vloeistoffen voorkomen (groei/druppels) die tak weglaten. Vanilla schadetypes hergebruiken (geen eigen `damage_type`-JSON).
- Eigen `codec()` (`simpleCodec`).

**`glow_mold`** (vraag 8 A): vanilla `GlowLichenBlock` werkt generiek op `this` (`MultifaceBlock`, `MultifaceSpreader`, `canBeReplaced` op het eigen item, bonemeal-spreiding). **Bouwstap:** in de sources bevestigen dat `GlowLichenBlock`/`MultifaceBlock` nergens `Blocks.GLOW_LICHEN` hardcodet; zo wel, dan een kleine subclass `block/GlowMoldBlock` met eigen codec.

**Vuurkansen (`ModFlammables`):** druipzwam-set en hoed **niet** brandbaar (vraag 2 A, zoals crimson/warped); `glow_mold` 15/100 (zoals glow lichen). Lamp, netherrack, sporenmos, stalactiet niet.

**Compost (`ModDataMapProvider`, `NeoForgeDataMaps.COMPOSTABLES`):** `dripshroom_cap` 0,85 (wart block), `dripshroom_lamp` 0,65 (shroomlight), `glow_mold` 0,5 (glow lichen).

### 3. Tags (`ModTags`, `ModBlockTagsProvider`, `ModItemTagsProvider`)

Nieuwe `TagKey`s: `Blocks.DRIPSHROOM_STEMS` / `Items.DRIPSHROOM_STEMS` = `corruptedadventure:dripshroom_stems` (stam, hyfen, gestript).

Blok-tags (bij de bouw de 1.21.1-tagbestanden van de vanilla-tegenhanger nalezen en spiegelen waar zinvol):
- `corruptedadventure:dripshroom_stems` in `minecraft:logs` (**niet** `logs_that_burn`, vraag 2 A; zoals `#crimson_stems`) → `#mineable/axe`.
- `minecraft:planks`, `wooden_stairs`, `wooden_slabs`, `wooden_fences`, `fence_gates`, `wooden_doors`, `wooden_trapdoors`, `wooden_pressure_plates`, `wooden_buttons`; `c:stripped_logs`, `c:stripped_woods`, `c:fence_gates/wooden` (zoals dood hout).
- `soaked_netherrack`: `minecraft:base_stone_nether` (vraag 6 A; via die tag ook `#nether_carver_replaceables` en `#mineable/pickaxe` — bij de bouw nagaan, anders direct), `c:netherracks`.
- `spore_moss`: `minecraft:mineable/pickaxe`, `minecraft:nether_carver_replaceables`; **niet** in `minecraft:nylium` (vraag 5 A: geen vanilla-fungi/-roots erop, bonemeal op vanilla-netherrack ernaast wordt niet verspild).
- `dripshroom_cap`: `minecraft:wart_blocks` (→ `#mineable/hoe`, `#nether_carver_replaceables`).
- `dripshroom_lamp`: `minecraft:mineable/hoe`.
- `glow_mold`: tags van `glow_lichen` spiegelen (o.a. `#replaceable`, `#replaceable_by_trees`, `#mineable/axe`, `#sword_efficient`, `#lava_pool_stone_cannot_replace` — nalezen).
- `slime_stalactite`: `minecraft:mineable/pickaxe` (zoals pointed dripstone; die heeft verder geen relevante tags — nalezen).

Item-tags: `copy` van alle bovenstaande blok-tags (waar een item-tegenhanger bestaat), plus **`minecraft:non_flammable_wood`** (item-tag) voor stam, hyfen, gestript, planks, trap, plaat, hek, poort, deur, valluik, knop, drukplaat (vraag 2 A; zoals crimson/warped — geen ovenbrandstof). **Bouwstap:** nagaan hoe NeoForge's `furnace_fuels`-data map `#planks`/`#logs` uitschrijft; als crimson-planken via die tag toch brandstof zijn, de druipzwam-items expliciet uitsluiten (of accepteren zoals vanilla-in-NeoForge zich gedraagt en dat vastleggen).

### 4. Loot (`ModBlockLootProvider`)

- `dropSelf`: stam, hyfen, gestript, planks, trap, hek, poort, valluik, drukplaat, knop, `soaked_netherrack`, `dripshroom_cap`, `dripshroom_lamp`, `slime_stalactite`.
- `createSlabItemTable` (plaat), `createDoorTable` (deur).
- `spore_moss`: `createSingleItemTableWithSilkTouch(block, SOAKED_NETHERRACK)` (zoals nylium → netherrack).
- `glow_mold`: `createMultifaceBlockDrops(block, HAS_SHEARS)` (zoals glow lichen: alleen met schaar, aantal = aantal vlakken; bij de bouw de 1.21.1-signatuur nalezen).

### 5. Models en blockstates (datagen)

- `ModBlockStateProvider`:
  - `soaked_netherrack`, `dripshroom_cap`, `dripshroom_lamp`: `simpleBlock` (`cube_all`).
  - `spore_moss`: `cube_bottom_top` (top `spore_moss`, zijkant `spore_moss_side`, onder `soaked_netherrack`), zoals nylium.
  - Druipzwam-set: zelfde aanroepen als dood hout — `logBlock` (stam, gestripte stam; `_top` voor de kopse kant), `axisBlock` (hyfen = stam-zijkant rondom, gestript idem), `stairsBlock`/`slabBlock`/`fenceBlock`/`fenceGateBlock`/`pressurePlateBlock`/`buttonBlock` op `dripshroom_planks`, `doorBlockWithRenderType(…, "cutout")`, `trapdoorBlockWithRenderType(…, true, "cutout")`.
  - `glow_mold`: multipart zoals vanilla `glow_lichen` (per vlak `north`…`down` het model met de juiste x/y-rotatie). Model: dezelfde opbouw als vanilla `block/glow_lichen.json` (bij de bouw uit de client-extra-jar nalezen, valkuil "Vanilla worldgen-JSON"; heeft hij een bruikbare parent, dan alleen de texture vervangen), `renderType("cutout")`.
  - `slime_stalactite`: variants op `vertical_direction` × `thickness` (10 modellen, `waterlogged` negeren), elk met parent `minecraft:block/pointed_dripstone` en texture `cross` = `block/slime_stalactite_<up|down>_<thickness>`, `renderType("cutout")` — zoals vanilla.
- `ModItemModelProvider`: blockitems op het blockmodel; `fenceInventory`/`buttonInventory` op `dripshroom_planks`; valluik `_bottom`; deur `basicItem`; `glow_mold` plat (`item/generated`, `layer0` = `block/glow_mold`, zoals vanilla); `slime_stalactite` plat met `item/slime_stalactite`.

### 6. Placeholder-textures (`tools/textures/Ring2Textures.java` uitbreiden)

Nieuw blok "Nether ring 2" in hetzelfde script (zie aannames), `still` (geen animatie; puls/emissive en definitieve stijl in change 37), vaste seed per bestandsnaam, alpha alleen 0/255 voor cutout-textures. **Werkpaletten, geen beslissing** (definitief in change 37), als constanten bovenin:
- doorweekt netherrack: donker roodbruin met grijsgroene zweem en natte glansvlekjes — `2E1A1A 47282A 5E3634 6F4A44` + glans `8A7A70`;
- sporenmos: grijsgroen `3E4A3C 556450 6E7F66 8C9A80` + sporenpuntjes `C8D4B0`;
- druipzwam-steel: grijsgroen-paars `3A3440 4E4A58 66667A` met slijmstrepen `7FA88A`; binnenkant/planks `6E7A6A 87947F A0AC94 B8C2A8`;
- hoed: grauw groenpaars `4A3F55 5F516C 75688A` met druppelvlekken `9FC3A8`;
- zwamlamp: geelgroen licht `B8D27A CFE38F E3F0AE F4FAD2`;
- gloeischimmel: blauwgroene stippen `5FD6B8 8BEACF C2F7E6`;
- slijmstalactiet: doorschijnend ogend slijmgroen (alleen 0/255 alpha) `4E8A4A 6AA860 8CC47A B2DE9E`.

| Bestand | Voor |
|---|---|
| `block/soaked_netherrack.png` | blok + onderkant sporenmos |
| `block/spore_moss.png`, `block/spore_moss_side.png` | top / zijkant (moslaag bovenaan, doorweekt netherrack eronder) |
| `block/dripshroom_stem.png`, `block/dripshroom_stem_top.png` | stam + hyfen |
| `block/stripped_dripshroom_stem.png`, `block/stripped_dripshroom_stem_top.png` | gestript |
| `block/dripshroom_planks.png` | planks-familie (4 planken, zelfde indeling als `dead_planks`) |
| `block/dripshroom_door_top.png`, `block/dripshroom_door_bottom.png`, `block/dripshroom_trapdoor.png`, `item/dripshroom_door.png` | deur/valluik (cutout) |
| `block/dripshroom_cap.png` | hoed |
| `block/dripshroom_lamp.png` | zwamlamp |
| `block/glow_mold.png` | gloeischimmel (cutout, losse plekjes) |
| `block/slime_stalactite_{down,up}_{tip,tip_merge,frustum,middle,base}.png` (10) | stalactiet/stalagmiet per dikte (cutout) |
| `item/slime_stalactite.png` | item |

26 PNG's. **Eerst het script, dan `runData`** (`ExistingFileHelper`). Contactblad `build/texture-preview/ring2.png` toont de nieuwe sectie. Vanilla's crimson/warped-stamzijkant is geanimeerd; de druipzwam-stam blijft hier `still` (change 37 beslist).

### 7. Lang (`EnglishLanguageProvider`, `DutchLanguageProvider`)

Nieuw commentaarblok "Nether ring 2 blocks". Nederlandse termen volgen vanilla-`nl_nl` voor stam/hyfen/gestript/trap/plaat/… (asset-cache, valkuil in `CODEBASE.md`).

| Key | en_us | nl_nl (indicatief) |
|---|---|---|
| `soaked_netherrack` | Soaked Netherrack | Doorweekt netherrack |
| `spore_moss` | Spore Moss | Sporenmos |
| `dripshroom_stem` / `_hyphae` | Dripshroom Stem / Hyphae | Druipzwamsteel / Druipzwamhyfen |
| `stripped_dripshroom_stem` / `_hyphae` | Stripped Dripshroom Stem / Hyphae | Gestripte druipzwamsteel / Gestripte druipzwamhyfen |
| `dripshroom_planks` | Dripshroom Planks | Druipzwamplanken |
| `dripshroom_stairs` / `_slab` / `_fence` / `_fence_gate` / `_door` / `_trapdoor` / `_pressure_plate` / `_button` | Dripshroom Stairs / … | Druipzwamtrap / -plaat / -hek / -poort / -deur / -valluik / -drukplaat / -knop |
| `dripshroom_cap` | Dripshroom Cap | Druipzwamhoed |
| `dripshroom_lamp` | Dripshroom Lamp | Zwamlamp |
| `glow_mold` | Glow Mold | Gloeischimmel |
| `slime_stalactite` | Slime Stalactite | Slijmstalactiet |

### 8. Overdracht naar latere changes (in `CODEBASE.md` en het plan van de betreffende change)

- **Change 35 (`worldgen`):**
  - Reuzenzwam: vanilla `minecraft:huge_fungus` werkt met eigen blokken (`valid_base_block` = `spore_moss`, `stem_state` = `dripshroom_stem`, `hat_state` = `dripshroom_cap`, `decor_state` = `dripshroom_lamp`, `replaceable_blocks`); de zuilen van vloer tot plafond vragen waarschijnlijk een eigen feature. Een stam op sporenmos maakt het sporenmos eronder bij de eerste random tick tot doorweekt netherrack (zoals vanilla-nylium onder een stam) — onschuldig.
  - Terrein: `soaked_netherrack` zit in `#base_stone_nether` → vanilla-Nether-carvers en `#base_stone_nether`-ertsen (ancient debris) werken erin; quartz/goud/magma gebruiken vanilla `block_match` netherrack en dus **niet** (change 35 kiest zelf, net als zielkristal).
  - Sporenmos-eilanden op de lavazee: sporenmos op doorweekt netherrack; onbedekt blijft het sporenmos.
  - Stalactieten: vanilla `pointed_dripstone`-, `dripstone_cluster`- en `large_dripstone`-features hardcoden `pointed_dripstone`/`dripstone_block`. Plaatsen via een eigen feature of `block_column` met **expliciete dikte-states** (base → middle → frustum → tip) en altijd tegen een stevig blok; een slecht ondersteunde stalactiet valt bij de eerste buur-update.
  - Gloeischimmel: vanilla `multiface_growth`-feature (`block` = `glow_mold`, `can_place_on` doorweekt netherrack/sporenmos) volstaat.
- **Change 36 (`atmosphere`):** druppel-particles (slijmstalactiet, doorweekt netherrack) — via biome-particles of `animateTick`; niet in deze change.
- **Change 37 (`textures`):** dezelfde bestandsnamen in `Ring2Textures.java`; animatie (stam, lamp, schimmel) en emissive zijn daar een keuze.
- **Change 51 (`blocks`, Nether-ring 3):** `StalactiteBlock` hergebruiken voor ijsstalactieten (eventueel met een parameter voor geluid/schade); `ModWoodTypes` voor de rijpzwam-set naar het druipzwam-patroon.

### 9. Documentatie

- `CODEBASE.md`: blok-tabel (sectie Nether-ring 2: class, basis, gedrag, loot, tags), nieuwe classes (`SporeMossBlock`, `SoakedNetherrackBlock`, `StalactiteBlock`, eventueel `GlowMoldBlock`), `ModWoodTypes` (`DRIPSHROOM_SET`, `DRIPSHROOM`), `ModTags` (`dripshroom_stems`), compost-data map, textures-regel (`Ring2Textures.java`: Nether-ring 2), valkuilen: "`PointedDripstoneBlock` hardcodet zichzelf → kopie", "sporenmos: random ticks alleen bij een speler, bonemeal via dispenser testbaar", "stalactiet in worldgen alleen met steun en juiste dikte-states", "`non_flammable_wood` = geen ovenbrandstof". Kopregel en "Laatst bijgewerkt".
- `fase-1-wereld.md` §2 Ring 2 → Nether-ring 2: **Uitwerking (change 33)** met de blokkenlijst, namen, spreidingsregel, stalactiet-gedrag, licht, en de antwoorden.
- `02-beslissingenlog.md`: regels hieronder.
- `docs/backlog.md`:
  - `[items] Recipes druipzwam-set (steel → planken, hyfen, trap, plaat, hek, poort, deur, valluik, knop, drukplaat) en eventueel zwamlamp/hoed. (bron: change 33)` — of de bestaande regel "druipzwam-hout" (regel 20) aanvullen.
  - `[atmosphere] Druppel-particles van slijmstalactieten en doorweekt netherrack (change 36). (bron: change 33)`
  - `[blocks] Bonemeal op sporenmos (eigen Nether-2-vegetatie, bijv. gloeischimmel of kleine zwammen) — nu doet bonemeal op sporenmos niets. (bron: change 33)`
  - `[blocks] Kleine druipzwam (zaailing-equivalent) om de set hernieuwbaar te maken. (bron: change 33)` — alleen bij vraag 4 A.
  - `[infra] GameTest-opzet (lege template, `gameTestServer`) om random-tick-gedrag (dood gras, sporenmos, latere groeiblokken) automatisch te testen via `state.randomTick(level, pos, seededRandom)`. (bron: change 33)`

---

## Client/server-impact

- Blokken, items, `BlockSetType` zijn common registry-entries; NeoForge synct de registry-ID's bij het joinen.
- **Server-side gedrag:** random tick van sporenmos (alleen `ServerLevel#randomTick`), bonemeal (`performBonemeal` krijgt een `ServerLevel`), vallende stalactiet (`FallingBlockEntity`, vanilla-entity, gesynct), schade (stalagmiet, vallende punt), vuur dooft op doorweekt netherrack (vanilla fire-tick), multiface-spreiding van gloeischimmel (bonemeal). `updateShape`/`canSurvive`/`getStateForPlacement` draaien op beide kanten maar gebruiken alleen common classes.
- **Geen client-code:** render types in de model-JSON (`cutout`), kleuren in de PNG's (geen `BlockColor`), geen `animateTick` (druppels → change 36). `StalactiteBlock` mag geen client-imports krijgen bij het kopiëren van vanilla (de vanilla-`animateTick` valt weg).
- XZ-offset van de stalactiet hangt alleen van de positie af (vanilla), dus client en server tonen dezelfde vorm.
- Geen netwerk, geen SavedData/attachments. Het texturescript staat buiten `src/` en komt niet in de jar.
- Worldgen-determinisme: niet geraakt (geen features). Random ticks zijn niet deterministisch, maar dat is runtime, geen worldgen.

---

## Tests

Bouwplan: **B, D, S (random ticks server-side)**.

- [x] **Script** — `java tools/textures/Ring2Textures.java` draait; 26 nieuwe PNG's van 16×16; cutout-textures alleen alpha 0/255; tweede run → `git status` schoon (deterministisch); bestaande ring 2-PNG's en `Ring1Textures.java` ongewijzigd; contactblad toont de Nether-ring 2-sectie.
- [x] **B** — `./gradlew build` slaagt, incl. `ModBlocksTest` (19 nieuwe blokken met blockitem, tabvolgorde). Nieuwe unit tests (FML-omgeving, geen wereld nodig):
  - `block/StalactiteBlockTest`: `getStateForPlacement`-onafhankelijke delen — vorm per dikte gelijk aan vanilla `pointed_dripstone` (zelfde `VoxelShape`-grenzen per `thickness`), codec rondreis, blockstate-properties identiek aan vanilla (`vertical_direction`, `thickness`, `waterlogged`).
  - `block/ModWoodTypesTest` (of uitbreiding): `DRIPSHROOM_SET` staat in de `BlockSetType`-codec onder `corruptedadventure:dripshroom`; geluiden = crimson.
  - **Random-tick-gedrag zit niet in een unit test:** het leest de wereld (licht, buren) en een nep-`ServerLevel` is te groot; een GameTest-opzet is `infra` (backlog). Dekking via S + RCON (bonemeal, scheduled ticks) en de in-game checklist (random ticks).
- [x] **D** — `./gradlew runData` slaagt zonder "texture does not exist"; gegenereerd en gecommit: blockstates/models (stalactiet 10 varianten met parent `pointed_dripstone`, schimmel multipart, sporenmos `cube_bottom_top`, deur/valluik/schimmel/stalactiet `render_type` cutout), item-models, loot (sporenmos Silk Touch-tak → doorweekt netherrack; schimmel multiface + schaar; plaat; deur), tags (`logs` via `dripshroom_stems`, **niet** `logs_that_burn`; `base_stone_nether`, `wart_blocks`, `mineable/*`, `nether_carver_replaceables`, `c:netherracks`, `c:stripped_*`; item `non_flammable_wood`), compost-data map, lang en/nl.
- [x] **S** — `tools/server-smoke-test.sh` met `level-name=smoke-change33`: server start zonder fouten of client-class-meldingen. Via RCON (daarna RCON weer uit; `forceload add` voor de testplek):
  - **tags:** `setblock` + `execute if block <pos> #minecraft:logs` (stam, hyfen), `unless … #minecraft:logs_that_burn`, `#minecraft:base_stone_nether` (doorweekt netherrack), `unless … #minecraft:nylium` (sporenmos), `#minecraft:wart_blocks` (hoed), `#minecraft:mineable/hoe` (lamp);
  - **loot:** `loot insert <kist> mine <pos> minecraft:iron_pickaxe` op sporenmos → `soaked_netherrack`, met Silk Touch → `spore_moss`; gloeischimmel met `minecraft:shears` → `glow_mold`, met de hand → niets; dubbele plaat → 2; stalactiet → zichzelf;
  - **stalactiet (scheduled ticks werken in geforceloade chunks):** plafond `soaked_netherrack` + `slime_stalactite[vertical_direction=down,thickness=tip]` eronder → plafond `setblock … air` → na ~2 s `execute unless block <pos> corruptedadventure:slime_stalactite` (gevallen; geen exception in de log); stapel van 3 hangend plaatsen met een buur-update → `execute if block` op `thickness=base`/`frustum`/`tip`; stalagmiet op de vloer, vloer weg → breekt;
  - **bonemeal via dispenser** (werkt zonder speler): dispenser met `minecraft:bone_meal` (`item replace block … container.0`) gericht op `soaked_netherrack` naast `spore_moss` → `setblock` redstone-blok ernaast → `execute if block <doel> corruptedadventure:spore_moss`; zonder sporenmos-buur → blijft doorweekt netherrack; op sporenmos zelf → niets en bonemeal niet verbruikt (vraag 5 A); op `glow_mold` → spreidt naar een buurvlak;
  - **vuur:** `setblock` vuur op doorweekt netherrack en op netherrack → na ~60 s alleen op netherrack nog vuur (vraag 6 A).
- **W-achteraf (in-game checklist, runClient, creative + survival)** — niet blokkerend:
  - [ ] **Random ticks** (alleen met een speler in de buurt): `/gamerule randomTickSpeed 300`; veld doorweekt netherrack met één sporenmosblok → mos spreidt in enkele seconden; spreidt **niet** naar vanilla-netherrack ernaast; blok erop (steen) → het mos eronder wordt doorweekt netherrack; glas of een plaat erop → blijft mos (blokkeert geen licht volledig). Server-log zonder exceptions. Daarna `randomTickSpeed 3`.
  - [ ] Creative tab: Nether-ring 2-blokken ná ring 2, volgorde zoals §1; namen en/nl.
  - [ ] Alle blokken plaatsbaar, geen paars-zwart; stam/hyfen draaien mee met de as; bijl → gestript; deur/valluik/schimmel/stalactiet transparant; hek verbindt met crimson-hek; knop/drukplaat geven redstone met nether-hout-geluid.
  - [ ] Stalactiet: hangend en staand plaatsen, punten voegen samen (`tip_merge`), stapel krijgt de juiste dikte; plafond weg → valt en doet schade; op een stalagmiet springen → extra valschade; trident breekt hem.
  - [ ] Gloeischimmel op muur/plafond/vloer, meerdere vlakken in één blok, licht 7; zwamlamp licht 15.
  - [ ] Druipzwam-planken en -stam branden niet in vuur en zijn geen ovenbrandstof (vraag 2 A); gloeischimmel brandt; hoed/lamp/schimmel in de composter.

**Blokkerende handmatige test: nee.** Wat crasht of fout data geeft, dekken B/D/S met RCON (loot, tags, scheduled ticks voor de stalactiet, bonemeal via dispenser). Het enige dat een speler vereist — de random-tick-spreiding van sporenmos — gebruikt dezelfde opbouw als het geteste `DeadGrassBlock`, en change 35 bouwt er niet op (worldgen plaatst blokken direct; random ticks spelen pas in het spel). Dus **W-achteraf**.

---

## Risico's / aandachtspunten

- **Kopie van `PointedDripstoneBlock`** (~400 regels): kans op een gemiste `Blocks.POINTED_DRIPSTONE` (dan voegen punten niet samen of vallen stalactieten niet). Mitigatie: grep in de kopie op `POINTED_DRIPSTONE`/`DRIPSTONE_BLOCK` moet leeg zijn; RCON-stapeltest (dikte-states). Bij een MC-upgrade opnieuw vergelijken (valkuil in `CODEBASE.md`).
- **Vallende stalactiet in worldgen:** een stalactiet die change 35 zonder steun plaatst, valt bij de eerste buur-update (lag, losse items). Overdracht in §8.
- **Sporenmos onder reuzenzwam-stammen** wordt doorweekt netherrack (bedekt) — vanilla-gedrag van nylium; alleen visueel.
- **`GlowLichenBlock` hergebruiken:** als vanilla toch `Blocks.GLOW_LICHEN` hardcodet (bijv. in de spreider), spreidt bonemeal de verkeerde blokken; bouwstap §2 + RCON-dispensertest.
- **Ovenbrandstof en `non_flammable_wood`:** NeoForge's data map kan vanilla's uitsluiting anders afhandelen; bouwstap §3. Eventuele afwijking vastleggen, niet stil laten.
- **`#base_stone_nether`** laat ancient debris en vanilla-carvers in doorweekt netherrack toe; bewust (vraag 6), change 35 bepaalt welke features er echt komen.
- **Spreiding naar eigen familie** (geen vanilla-netherrack): voorkomt woekering, maar een speler moet doorweekt netherrack meenemen om sporenmos elders te laten groeien. Bonemeal (dispenser of hand) maakt dat snel.
- **Aantal bestanden** (19 blokken, 26 PNG's, 10 stalactietmodellen): placeholders eenvoudig houden; stijl hoort bij change 37.
- **Afhankelijk van niet-gebouwde changes** (21, 22, 24, 29): andere namen of plekken (`Ring2Textures.java`, `registerWithoutItem`, compost-data map, ring 2-sectie) bij de start nalopen en volgen; ontbreekt `Ring2Textures.java`, dan stoppen en vragen.
- Bestaande testwerelden: geen probleem (alleen nieuwe blokken, geen worldgen).

---

## Ontwerpvragen

### 1. Welke naam krijgt de reuzenzwam-set (registry-prefix)?
Spelers zien de Engelse naam; Nederlands volgt "druipzwam".
- **A. `dripshroom_` (aanbevolen):** `dripshroom_stem`, `dripshroom_planks`, `dripshroom_cap`, `dripshroom_lamp` — kort, één woord, leest als "druipende zwam"; past naast vanilla's `shroomlight`. Geen conflict met latere ringen (ring 4 heeft gifzwam, Nether-3 rijpzwam).
- **B. `drip_fungus_`:** vanilla-woord "fungus" (`crimson_fungus`), maar lange namen (`stripped_drip_fungus_hyphae`, `drip_fungus_pressure_plate`).
- **C. `soggy_`:** beschrijft het natte, maar zegt niet dat het een zwam is en kan botsen met moerasblokken in ring 4.

Antwoord: **A** — prefix `dripshroom_` (`dripshroom_stem`, `dripshroom_planks`, `dripshroom_cap`, `dripshroom_lamp`, …).

### 2. Gedraagt de druipzwam-set zich als Nether-hout (crimson/warped)?
- **A. Ja (aanbevolen):** namen `stem`/`hyphae` (vanilla-Nether), **niet brandbaar** (geen vuurkansen, niet in `logs_that_burn`, wel in `#logs`; item-tag `non_flammable_wood` → geen ovenbrandstof), nether-hout-geluiden (`BlockSetType` met de crimson-velden). Past bij "doorweekt" en de Nether; Nether-bases branden niet af. Geen extra code.
- **B. Als overworld-hout** (dood hout, palm): `_log`/`_wood`-namen, brandbaar 5/5 en 5/20, ovenbrandstof, gewone houtgeluiden. Consistent met de andere houtsets; voelt minder Nether.
- **C. Nether-namen en -geluiden, maar wel brandbaar/brandstof.** Mengvorm; vreemd naast crimson/warped.

Antwoord: **A** — ja, als Nether-hout: `stem`/`hyphae`, niet brandbaar, geen ovenbrandstof, nether-hout-geluiden.

### 3. Wat is de druipzwam-hoed?
- **A. Vol blok zoals een wratblok (`nether_wart_block`) (aanbevolen):** schoffel, dropt zichzelf, composteerbaar, in `#wart_blocks`; change 35 kan vanilla `huge_fungus` met deze hoed gebruiken. Geen eigen class.
- **B. Als A, maar zacht: valschade −80 % zoals een hooibaal.** Past bij de hoge zwamkathedralen (een sprong op een hoed is een redding); één kleine eigen class (`fallOn`), server-side.
- **C. Zoals vanilla-paddenstoelblokken (`HugeMushroomBlock`):** zes zijde-states (binnen/buiten), dropt 0–2 kleine zwammen. Vraagt een klein zwam-item/-blok (raakt vraag 4) en meer models/textures.

Antwoord: **A** — vol blok zoals een wratblok (schoffel, dropt zichzelf, composteerbaar, `#wart_blocks`).

### 4. Komt er een kleine druipzwam (zaailing-equivalent) om de set hernieuwbaar te maken?
Het design-doc noemt er geen.
- **A. Nee, nu niet; backlog (aanbevolen).** Blijft binnen het design; reuzenzwammen zijn groot, dus genoeg hout per exemplaar. Nadeel: in multiplayer raakt het hout rond spawnpunten in de Nether op.
- **B. Ja: `dripshroom_fungus` (plant op sporenmos, bonemeal → reuzenzwam) + bloempot.** Zelfde patroon als de palmzaailing (change 22): verwijst naar een configured-feature-key die change 35 registreert; tot dan groeit hij niet. +2 blokken, 1 texture, `TreeGrower`-achtige code.

Antwoord: **A** — nee, nu niet; kleine druipzwam naar de backlog.

### 5. Hoe spreidt sporenmos?
Het design zegt "spreidt zoals nylium" en "random ticks, server-side". Vanilla-nylium spreidt zelf **niet**; alleen bonemeal op netherrack naast nylium maakt nieuw nylium.
- **A. Random tick binnen de eigen familie + bonemeal (aanbevolen):** per random tick één poging naar onbedekt **doorweekt netherrack** binnen 3×5×3 (gras: 4 pogingen), geen lichteis; bedekt → doorweekt netherrack; bonemeal op doorweekt netherrack naast sporenmos → sporenmos; bonemeal op sporenmos doet niets (vegetatie → backlog). Niet in `#minecraft:nylium`. Geen woekering naar Nether-ring 1 of bases.
- **B. Precies vanilla-nylium:** geen spreiding via random ticks, alleen bonemeal. Eenvoudigst, maar wijkt af van bouwplan en design ("random ticks").
- **C. Als A, maar ook naar vanilla-netherrack.** Levendige woekering, ook in Nether-ring 1 en netherrack-bouwwerken; voelt als corruptie (dat is fase 5) en kan spelersbouw aantasten.
- **D. Als A, maar wél in `#minecraft:nylium`:** vanilla-roots, -spruiten en -fungi kunnen erop staan (minder eigen sfeer); bonemeal op vanilla-netherrack ernaast wordt verspild (vanilla zoekt alleen crimson/warped nylium).

Antwoord: **A** — random tick binnen de eigen familie (naar onbedekt doorweekt netherrack, 3×5×3) + bonemeal; niet in `#minecraft:nylium`.

### 6. Wat doet doorweekt netherrack?
- **A. Nat grondblok (aanbevolen):** netherrack-hardheid, modder-geluid, **geen eeuwig vuur** (niet in `#infiniburn_*`), wel in `#base_stone_nether` (vanilla-carvers en `#base_stone_nether`-features werken erin; change 35 kiest de ertsen) en `c:netherracks`. Alleen data + de bonemeal-regel uit vraag 5.
- **B. Als A, en het droogt op tot netherrack** buiten Nether-ring 2 of naast lava (random tick + `ringAt`, server-side). Sterk verhaal (dauw verdampt elders), maar meer code, hangt van de ringindeling af en maakt meegenomen blokken vergankelijk.
- **C. Gewoon netherrack-gedrag** (eeuwig vuur, netherrack-geluid), alleen een andere kleur. Minst werk; het "natte" zit dan alleen in de texture.

Antwoord: **A** — nat grondblok: netherrack-hardheid, modder-geluid, geen eeuwig vuur, in `#base_stone_nether` en `c:netherracks`.

### 7. Hoeveel dripstone-gedrag krijgen slijmstalactieten?
Vanilla `PointedDripstoneBlock` hardcodet zichzelf; elk echt dripstone-gedrag vraagt een aangepaste kopie (`StalactiteBlock`, herbruikbaar voor de ijsstalactieten van change 51).
- **A. Vorm en vallen, geen groei of druppels (aanbevolen):** dikte-states (punt → basis), samenvoegende punten, valt zonder steun en doet schade, stalagmiet-valschade, trident breekt hem; slijmgeluid. Geen random ticks; druppel-particles → change 36. Eén class, server-side via scheduled ticks (RCON-testbaar).
- **B. Als A plus langzame groei** (random tick onder doorweekt netherrack, zoals dripstone onder water): levendiger en hernieuwbaar, maar meer code, nog een random-tick-pad dat alleen met een speler te testen is, en kans op wildgroei in bases.
- **C. Decoratief:** één hangend/staand blok met een vorm, breekt (dropt) als de steun weg is, geen vallen of schade. Weinig code, maar niet het "dripstone-gedrag" uit het design.

Antwoord: **A** — vorm en vallen (generieke `StalactiteBlock`), geen groei of druppels.

### 8. Wat is de lichtgevende schimmel (en hoe fel zijn schimmel en zwamlamp)?
In alle opties: de zwamlamp = shroomlight-achtig vol blok, licht 15, schoffel, dropt zichzelf.
- **A. Multiface-laag zoals glow lichen (aanbevolen):** op elke muur, vloer en plafond, meerdere vlakken per blok, licht 7, alleen met schaar te oogsten, bonemeal spreidt hem; vanilla-class hergebruikt. Ideaal voor de wanden en plafonds van de sporengrotten; change 35 gebruikt vanilla `multiface_growth`.
- **B. Plantje op de grond** (kruismodel, alleen op sporenmos/doorweekt netherrack, licht 10, zoals crimson roots): eenvoudig en fel, maar verlicht alleen vloeren.
- **C. Dunne laag (carpet) op elke bovenkant, licht 5:** leest als "schimmelvloer", maar alleen horizontaal en zwakker licht.

Antwoord: **A** — multiface-laag zoals glow lichen (licht 7, schaar, bonemeal spreidt); zwamlamp licht 15.

### 9. Komt er een kleine bodemplant voor de Nether-ring 2, naast zuilen, stalactieten en schimmel? (nieuw, 2026-10-05; beantwoord 2026-10-05)
Ring 2 moet levendig aanvoelen, ook de Nether-tegenhanger. Nu bestaat de vloer uit sporenmos zonder planten; bonemeal op sporenmos doet niets (antwoord 5 A, backlog).
- **A. Ja: één kleine bodemplant `spore_sprouts` (kruismodel, staat op `spore_moss`/`soaked_netherrack`, licht 0, geen random tick) (aanbevolen).** +1 blok, +1 texture, +1 feature in change 35 (plaatsing in vlekken), bonemeal op sporenmos plant ze en geeft bonemeal een doel (lost de backlogregel op); composteerbaar; schaar of hand haalt hem weg (geen extra drop). Multiplayer: server-side bonemeal, verder niets. Speelgevoel: de vloer leeft; je ziet vanaf afstand "begroeid".
- **B. Nee, zoals het plan: alleen mos, zuilen, stalactieten, schimmel.** Minste werk, geen nieuwe texture; risico dat de vloer na teststop 3 als "kaal" wordt beoordeeld, zoals de overworld-biomes na teststop 2b.
- **C. Ja, twee planten: `spore_sprouts` (A) en een kleine lichtgevende `glow_cap` (licht 5, kruismodel).** Meer sfeer in de sporengrotten en meer licht op de vloer, maar +2 blokken, +2 textures, +2 features in 35 en een extra balans met `glow_mold` en `dripshroom_lamp` (te veel licht).

Antwoord: **A** — ja, één kleine bodemplant `spore_sprouts` (kruismodel, op `spore_moss`/`soaked_netherrack`, licht 0, geen random tick; bonemeal op sporenmos plant ze; composteerbaar; +1 blok, +1 texture, plaatsing in vlekken in change 35).

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | blocks | Reuzenzwam-set heet `dripshroom_*`: steel, hyfen, gestript, planks + houtset, hoed, zwamlamp | Kort, leest als druipende zwam; geen conflict met latere ringen |
| 2026-10-01 | blocks | Druipzwam-hout gedraagt zich als Nether-hout: niet brandbaar, geen brandstof | Past bij "doorweekt" en de Nether; geen extra code |
| 2026-10-01 | blocks | Druipzwam-hoed: vol blok zoals een wratblok | Geen eigen class; bruikbaar in vanilla `huge_fungus` |
| 2026-10-01 | blocks | Kleine druipzwam: nu niet (backlog) | Binnen het design; reuzenzwammen leveren genoeg hout |
| 2026-10-01 | blocks | Sporenmos spreidt via random tick binnen de eigen familie + bonemeal (niet in `#nylium`); bedekt → doorweekt netherrack; Silk Touch → sporenmos, anders doorweekt netherrack | Volgt het design zonder woekering naar ring 1 of bases |
| 2026-10-01 | blocks | Doorweekt netherrack: nat grondblok, geen eeuwig vuur, in `#base_stone_nether` en `c:netherracks` | Alleen data; carvers en features werken erin |
| 2026-10-01 | blocks | Slijmstalactieten: vorm en vallen, geen groei of druppels; generieke `StalactiteBlock` (kopie van vanilla pointed dripstone), ook voor de ijsstalactieten van Nether-ring 3 | Vanilla hardcodet pointed dripstone; één class voor beide ringen |
| 2026-10-01 | blocks | Lichtgevende schimmel: multiface-laag zoals glow lichen (licht 7); zwamlamp licht 15 zoals shroomlight | Ideaal voor wanden en plafonds van de sporengrotten |
| 2026-10-01 | textures | Nether-ring 2-textures in `tools/textures/Ring2Textures.java` (één script per ring, Nether-tegenpool hoort bij zijn ring); placeholders nu, definitief in change 37 | Afspraak één script per ring; ring 1 deed hetzelfde met het zielkristal |

Commit (na `/code-review`): `blocks: add nether ring 2 fungus, moss and stalactite blocks`.

---

## Bouwnotities (2026-10-05)

Gebouwd op `473565d` (changes 0-32 klaar). De aannames uit het plan klopten; afwijkingen en implementatiebeslissingen:
- **20 blokken** (19 + `spore_sprouts`), sectie `// ---- Nether ring 2 ----` achteraan `ModBlocks`, `spore_sprouts` als laatste. 27 nieuwe PNG's (26 + `spore_sprouts`), gegenereerd door `Ring2Textures.java`; tweede run: `git status` schoon (deterministisch), bestaande ring 2-PNG's onveranderd.
- **`GlowMoldBlock` is nodig** (plan: "alleen als vanilla hardcodet"): `GlowLichenBlock.canBeReplaced` vergelijkt met `Items.GLOW_LICHEN`; zonder subclass zou het eigen item een bestaand blok vervangen. De subclass overschrijft alleen `canBeReplaced` en de codec (`MapCodec<GlowLichenBlock>`, want `codec()` heeft dat returntype). Spreider, bonemeal en loot zijn generiek (`Blocks.GLOW_LICHEN` komt in `MultifaceBlock` niet voor).
- **`StalactiteBlock`:** kopie van `PointedDripstoneBlock` uit de 21.1.252-sources, zonder groei/druppels/cauldron/`animateTick`; alle helpers testen `state.is(this)` (grep op `POINTED_DRIPSTONE`/`DRIPSTONE_BLOCK` in de kopie: leeg). Properties heten `vertical_direction`, `thickness`, `waterlogged` (zelfde als vanilla). Properties zijn `Properties.of()` met de vanilla-waarden minus `randomTicks()` (`ofFullCopy` kan het niet uitzetten).
- **Sporenmos:** `canStayMoss` (package-private, gebruikt door `SporeMossBlock` zelf) = vanilla's `canBeNylium`. Bonemeal op sporenmos: 48 pogingen, random walk in vlekken (zoals gras), alleen waar `spore_sprouts.canSurvive`. Op doorweekt netherrack is het doel geldig als erboven licht doorlaat (`propagatesSkylightDown`) en een buur sporenmos is.
- **Tags:** `dripshroom_cap` moest met de hand in `#mineable/hoe` (vanilla's wratblokken staan daar los, niet via `#wart_blocks`; de S-test vond het). `spore_sprouts` en `glow_mold` spiegelen `nether_sprouts`/`glow_lichen` (`#replaceable`, `#replaceable_by_trees`, `#sword_efficient`, `#combination_step_sound_blocks`/`#inside_step_sound_blocks`; glow mold ook `#mineable/axe`). `c:netherracks` voor doorweekt netherrack (item-tag via `copy`), `#wart_blocks` item-tag via `copy`. Sporenspruiten staan alleen op `spore_moss`/`soaked_netherrack` (directe check in `mayPlaceOn`, geen tag; change 35 hoeft niets).
- **Vuurkans:** `glow_mold` 15/100 in `ModFlammables`; de druipzwam-set heeft geen kansen. NeoForge's `furnace_fuels` verwijdert `#non_flammable_wood`, dus de item-tag volstaat voor "geen ovenbrandstof" (gecontroleerd in de data map van 21.1.252).
- **Kaartkleuren (placeholder):** bast `TERRACOTTA_GRAY`, planks-familie `TERRACOTTA_LIGHT_GREEN`, hoed `TERRACOTTA_PURPLE`, lamp `COLOR_LIGHT_GREEN`, sporenmos/-spruiten `TERRACOTTA_GREEN`, doorweekt netherrack `TERRACOTTA_BROWN`, gloeischimmel `WARPED_WART_BLOCK`, stalactiet `COLOR_LIGHT_GREEN`; bijstellen kan in change 37.
- **Texturescript:** de palm-houthulpen zijn gegeneraliseerd met het palet als parameter (zelfde random-volgorde, dus palm-PNG's identiek) i.p.v. een tweede kopie; `slime_stalactite_up_*` is `down_*` verticaal gespiegeld (`flippedVertically`).
- **Tests:** unit tests `StalactiteBlockTest` (properties en vormen = vanilla), `ModWoodTypesTest`, twee tests in `ModBlocksTest` (volgorde van de sectie, tick/licht/geluid). Decoderen van de stalactiet-codec kan niet in een unit test (bevroren registry), alleen encoderen.

### Testresultaten (S, `smoke-change33`, seed 20260930, preset `corrupted_world`)
- Server start zonder fouten of client-class-meldingen; `ClientIsolationTest` groen. Dev en Dev2 (`-PquickPlay`) joinen (`Received ring layout from the server`, `Dev joined the game`, `Dev2 joined the game`), geen ontbrekende modellen/textures in de clientlog.
- **Tags (RCON `execute if block`):** stam/hyfen/gestript in `#logs` en `#dripshroom_stems`, niet `#logs_that_burn`; doorweekt netherrack `#base_stone_nether`, `#c:netherracks`, niet `#infiniburn_overworld`; sporenmos niet `#nylium`, wel `#mineable/pickaxe` en `#nether_carver_replaceables`; hoed `#wart_blocks` (+ `#mineable/hoe` na de fix), lamp `#mineable/hoe`; planks/deur in `#planks`/`#wooden_doors`; `fungal_growths` (schimmel, stam, spruiten, stalactiet); spruiten `#replaceable`.
- **Loot (`loot insert ... mine`):** sporenmos + ijzeren pickaxe -> doorweekt netherrack, + Silk Touch -> sporenmos; gloeischimmel met schaar -> `glow_mold` (3 vlakken -> 3 items), met de hand -> niets; dubbele plaat -> 2; stalactiet, lamp, hoed, doorweekt netherrack, deur, spruiten -> zichzelf.
- **Stalactiet (scheduled ticks):** punt valt na het weghalen van het plafond en dropt een item; stapel van 3 (base/frustum/tip) en 4 (base/middle/frustum/tip) krijgt de goede dikte-states; hele stapel valt als geheel; `tip_merge` met een stalagmiet; stalagmiet breekt als de vloer wegvalt; geen exceptions in de log.
- **Bonemeal via dispenser:** doorweekt netherrack naast sporenmos -> sporenmos (bonemeal 4 -> 3); zonder sporenmos-buur blijft het netherrack en wordt bonemeal niet verbruikt (4 -> 4); vanilla-netherrack naast sporenmos blijft netherrack; bonemeal op sporenmos met lucht erboven plant 7 spruiten in een vlek (bonemeal verbruikt); spruiten verdwijnen op steen en op crimson nylium, blijven op doorweekt netherrack; bonemeal op gloeischimmel -> 2 blokken.
- **Vuur:** na 100 s is het vuur op alle 6 doorweekt-netherrackblokken uit en brandt het nog op alle 6 vanilla-netherrackblokken.
- **Random ticks (Dev in de buurt, `randomTickSpeed 300`):** sporenmos spreidt in 30 s van 3 naar 88 van 121 blokken doorweekt netherrack; spreidt niet naar vanilla-netherrack ernaast; bedekt (steen erop) -> doorweekt netherrack; onder glas blijft mos. Daarna `randomTickSpeed 3`.

---

## In-game checklist (W)

Nieuwe wereld, preset `corrupted_world`, seed `20260930`, creatief (`/gamemode creative`), OP. De Nether-ring 2 heeft nog geen eigen terrein of biomes (change 35): de blokken staan alleen in de creative tab en worden met commando's geplaatst. Een plek: `/ca tpring 2 nether` (landt in Nether-ring 2, een placeholder-biome); bouw boven de lavazee-vloer, of in de overworld op `/tp @s 1000 200 1000` met eerst `/fill 995 199 995 1015 199 1015 minecraft:stone`.

1. **Creative tab:** tab "Corrupted Adventure", scroll tot na "Salt Crystal". Volgorde: Soaked Netherrack, Spore Moss, Dripshroom Stem, Hyphae, Stripped Stem, Stripped Hyphae, Planks, Stairs, Slab, Fence, Fence Gate, Door, Trapdoor, Pressure Plate, Button, Cap, Lamp, Glow Mold, Slime Stalactite, Spore Sprouts (20 stuks). Taal naar Nederlands: Doorweekt netherrack, Sporenmos, Druipzwamsteel, Druipzwamhyfen, ..., Druipzwamhoed, Zwamlamp, Gloeischimmel, Slijmstalactiet, Sporenspruiten. Geen paars-zwarte iconen.
2. **Sporenmos spreidt (random ticks, alleen met een speler):** op de bouwplek: `/fill 1000 200 1060 1010 200 1070 corruptedadventure:soaked_netherrack`, `/setblock 1005 200 1065 corruptedadventure:spore_moss`, `/setblock 1006 200 1066 minecraft:netherrack`, `/setblock 1001 200 1061 corruptedadventure:spore_moss`, `/setblock 1001 201 1061 minecraft:stone`, `/setblock 1009 200 1061 corruptedadventure:spore_moss`, `/setblock 1009 201 1061 minecraft:glass`, `/gamerule randomTickSpeed 300`. Na ~30 s: het mos is over het veld gespreid; het vanilla-netherrack ernaast blijft netherrack; onder de steen is het mos doorweekt netherrack geworden; onder het glas blijft mos. Server-log zonder exceptions. Daarna `/gamerule randomTickSpeed 3`.
3. **Bonemeal met de hand (survival, `/gamemode survival`, `/give @s bone_meal 16`):** op doorweekt netherrack naast sporenmos -> wordt mos (bonemeal verbruikt); op doorweekt netherrack zonder mos -> niets (niet verbruikt); op sporenmos met lucht erboven -> vlekken sporenspruiten; op vanilla-netherrack naast sporenmos -> niets; op gloeischimmel -> spreidt naar een buurvlak.
4. **Sporenspruiten:** `/give @s corruptedadventure:spore_sprouts`; staan op sporenmos en doorweekt netherrack, niet op steen of crimson nylium (breken); met de hand of schaar te breken (dropt zichzelf); lopen er doorheen (geen botsing); in de composter; vloeistof/zwaartekracht: weg bij het weghalen van de grond.
5. **Houtset:** stam en hyfen draaien mee met de plaatsrichting (`/give @s corruptedadventure:dripshroom_stem`); bijl -> gestript, in alle assen; deur (twee helften), valluik en hek verbinden met het crimson-hek; knop (geeft redstone, nether-hout-geluid), drukplaat; deur/valluik/schimmel transparant waar bedoeld. Plaat -> dubbele plaat; trap heeft de hoeken.
6. **Niet brandbaar:** `/setblock ~ ~1 ~ fire` naast stam/planks: het hout vat geen vuur; stam/planken/deur als brandstof in een oven (`/give @s furnace`, schuif hout in de brandstofsleuf): geen brandstof. Gloeischimmel brandt wel.
7. **Hoed en lamp:** schoffel breekt hoed en lamp snel; lamp geeft licht 15 (F3 licht in het donker, `/gamerule` niets nodig); beide in de composter (hoed 85 %, lamp 65 %, schimmel 50 %, spruiten 30 %).
8. **Gloeischimmel:** plaatsen op muur, plafond en vloer; meerdere vlakken in één blok door het item op een tweede vlak te plaatsen (niet vervangen); licht 7 bij een vlak; alleen met schaar te oogsten (anders niets); bonemeal spreidt hem; blijft zichtbaar (cutout), geen dubbele vlakken.
9. **Slijmstalactiet:** tegen een plafond plaatsen (kijk omhoog) = hangend, op de vloer (kijk omlaag) = staand; twee tegenover elkaar plaatsen = punten voegen samen (`tip_merge`), sneaken tijdens het plaatsen voegt ze niet samen; een stapel van drie krijgt basis/midden/punt; plafond wegbreken -> de hele stalactiet valt en doet schade aan wie eronder staat (creatief geen schade: survival); op een stalagmiet springen -> extra valschade; een tridentworp (`/give @s trident`) breekt hem; slijmgeluiden.
10. **Doorweekt netherrack:** modder-geluid; breekt met pickaxe; vuur erop gaat na ~1 min uit (op vanilla-netherrack blijft het branden); een portal-/Nether-carvertest: niet nodig tot change 35.
11. **Twee clients (Dev + Dev2):** beide zien dezelfde plaatsingen en de gespreide mosvlek; geen desync.
12. **Smaak:** voelt de placeholder-set als één familie (grijsgroen/paars/slijm)? Noteer afwijkingen voor change 37 (textures).
