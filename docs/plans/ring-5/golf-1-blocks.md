# Ring 5 golf 1 — stroom B (blokken): 88, 88r, 89, 90

Scope: `blocks` (88, 89, 90) en `items` (88r, de recepten van de houtset). Eén commit per pakket, in deze volgorde, op
branch `ring5/blocks-g1`, worktree `P:/Projecten/Java/wt/r5-blocks-g1`, poort n = 0 (server 25570, RCON 25580), basis =
de startcommit van `golf-1.md`. **89 en 90 wachten op het antwoord op `golf-1.md` §6 OV2** (mijn-niveau nulliet en witte
kern); 88 en 88r kunnen meteen.

Bronnen: `contract.md` §3 en **§11** (namen vast; gedrag als de vanilla-tegenhanger; volledige houtset met vanilla-
recepten; alleen de 9 tabelblokken in `#corruptedadventure:corrupted` met `healthy_block`); `81-infra.md` (`ModDataMaps`,
tag-keys, `Ring5BlockNames`); `fase-1-wereld.md` → "Ring 5" (unieke blokken, ores) en "Nether-ring 5" (blokken);
sjablonen: tidestone-set en moonpetal-houtset (`docs/plans/ring-4/golf-1-blocks.md`), ring 4-ertsen (`oreIn`, emissive
overlay van tidesilver), `lagoon_sandstone`-recepten in `ModRecipeProvider` (66d/66g). `CODEBASE.md` kent golf 0 nog niet:
lees de CODEBASE-delta van `81-infra.md` erbij.

**Bezit** (`waves.md` §1/§5): `ModBlocks`, `ModItems`, `ModTags` (inhoud), `block/*`, `ModWoodTypes`, `ModFlammables`,
`ModCreativeTabs` (volgorde), blok-datagen (`ModBlockStateProvider`, `ModItemModelProvider`, `ModBlockLootProvider`,
`ModBlockTagsProvider`, `ModItemTagsProvider`, `ModDataMapProvider` (inhoud `healthy_block`, compost)),
`Ring5BlockNames`; voor 88r `ModRecipeProvider` (eigen methode `ring5()`); als de bordjes het vragen een nieuwe
`registry/ModBlockEntityTypes` en de client-registratie van de bordjesrenderer in `client/` (zoals B in ring 4
`client/fluid/ToxicPoolClientExtensions` maakte). Elk in een eigen methode/blok "ring 5" / "Nether ring 5".

**Doel:** alle blokken en items van ring 5 en Nether-5 uit contract §3/§11 bestaan, met gedrag, loot, tags,
blockstates/models (naar T's paden, §5), creative tab en lang (Engels + Nederlands). Creative-tab-volgorde =
registratievolgorde: ring 5 direct na de laatste Nether-ring 4-blokken, dan Nether-ring 5; de losse items na het laatste
ring 4-item.

---

## 1. Pakket 88 — corrupted varianten + houtset (`blocks`)

**De 9 contractblokken** (tag `#corruptedadventure:corrupted` = precies deze 9; data map `healthy_block` = precies deze
9, `contract.md` §3). Eigenschappen, hardheid en geluid van de vanilla-tegenhanger; kaartkleur grijs; geen tint (de
kleur zit in de texture, ook bij gras en bladeren).

| Blok | Klasse / gedrag | Loot | Tags (naast `corrupted`) | `healthy_block` |
|---|---|---|---|---|
| `corrupted_grass_block` | gewone `Block`, **geen random ticks** (spreidt niet, sterft niet, wordt nooit gras); geen snowy-staat; geen schoffel/pad (backlog) | `corrupted_dirt`, Silk Touch zichzelf | `#dirt`, `#mineable/shovel`, `#overworld_carver_replaceables` | `grass_block` |
| `corrupted_dirt` | gewone `Block` (geen schoffel/pad) | zichzelf | `#dirt`, `#mineable/shovel`, `#overworld_carver_replaceables` | `dirt` |
| `corrupted_stone` | steen | `corrupted_cobblestone`, Silk Touch zichzelf | `#base_stone_overworld`, `#overworld_carver_replaceables`, `#mineable/pickaxe`, `ring_5_stones` | `stone` |
| `corrupted_cobblestone` | keien | zichzelf | `#mineable/pickaxe`, `c:cobblestones`; item: `#stone_tool_materials`, `#stone_crafting_materials` | `cobblestone` |
| `corrupted_sand` | `ColoredFallingBlock` (valt, grijze stofkleur) | zichzelf | `#sand`, `c:sands`, `#mineable/shovel`, `#overworld_carver_replaceables`; item `#sand`, `c:sands`, `#smelts_to_glass` | `sand` |
| `corrupted_log` | `StrippableLogBlock` → `stripped_corrupted_log` | zichzelf | `corruptedadventure:corrupted_logs` (blok + item, nieuw) → `#logs_that_burn` | `oak_log` |
| `corrupted_wood` | `StrippableLogBlock` → `stripped_corrupted_wood` | zichzelf | `corrupted_logs` | `oak_wood` |
| `corrupted_planks` | planken | zichzelf | `#planks` (blok + item) | `oak_planks` |
| `corrupted_leaves` | `LeavesBlock` (vergaat zonder log binnen 6, zoals vanilla) | stokken zoals eikenblad (geen sapling, geen appel); schaar/Silk Touch zichzelf | `#leaves`, `#mineable/hoe`; item `#leaves` | `oak_leaves` |

**De houtset** (§11; vanilla-hout-tags, **niet** in `corrupted` en zonder `healthy_block`):
`stripped_corrupted_log`, `stripped_corrupted_wood` (`corrupted_logs`), `corrupted_stairs`, `corrupted_slab`,
`corrupted_fence`, `corrupted_fence_gate`, `corrupted_door`, `corrupted_trapdoor`, `corrupted_button`,
`corrupted_pressure_plate`, `corrupted_sign` + `corrupted_wall_sign`, `corrupted_hanging_sign` +
`corrupted_wall_hanging_sign` (de wand-varianten zonder eigen item; items `corrupted_sign` (`SignItem`) en
`corrupted_hanging_sign` (`HangingSignItem`)). Tags zoals vanilla eik: `#wooden_stairs`, `#wooden_slabs`,
`#wooden_fences`, `#fence_gates`, `#wooden_doors`, `#wooden_trapdoors`, `#wooden_buttons`, `#wooden_pressure_plates`,
`#standing_signs`, `#wall_signs`, `#ceiling_hanging_signs`, `#wall_hanging_signs`, `#mineable/axe`; items `#signs`,
`#hanging_signs` en de item-tegenhangers. Loot zoals vanilla (slab dubbel, deur alleen onderste helft). **Geen boot**
(niet in §11; backlog).

**Houttype:** `BlockSetType` + `WoodType` `corruptedadventure:corrupted` met de geluiden en het gedrag van eik
(`ModWoodTypes`). **Deze `WoodType` wordt wél geregistreerd** (`WoodType.register`): de bordjes hebben de sign-materialen
nodig (de andere houtsoorten van de mod doen dat bewust niet; T levert de bordtextures, §5).

**Bordjes (eerste in de mod):** de vanilla-`BlockEntityType` voor (hangende) bordjes accepteert alleen vanilla-blokken.
Kies de eenvoudigste werkende route in NeoForge 21.1 en noteer hem: (a) een NeoForge-event dat blokken aan een bestaand
block-entity-type toevoegt, als 21.1.x dat heeft; anders (b) eigen types in een nieuwe `registry/ModBlockEntityTypes`
(`corrupted_sign`, `corrupted_hanging_sign`) met subklassen van `SignBlockEntity`/`HangingSignBlockEntity`, en in
`client/` (alleen `Dist.CLIENT`) de renderers (`SignRenderer`, `HangingSignRenderer`) en zo nodig `Sheets.addWoodType` in
de client-setup. Het bewerkscherm is vanilla. **Rendering kan alleen met een client gecontroleerd worden:** B start
geen client; zet de check (bord plaatsen, tekst, hangend bord) op de lijst voor INT1/main.

**Overig:** `ModFlammables` zoals eik (stammen/hout 5/5, planken-set 5/20, bladeren 30/60); compost `corrupted_leaves`
0,3 (`ModDataMapProvider`); brandstof volgt de vanilla-tags (controleren dat `#logs`/`#planks`/houten items via de
NeoForge-`furnace_fuels` meetellen). `ring_5_stones` krijgt hier `corrupted_stone` (voidsteen in 89).

**Data map** (`ModDataMapProvider`, methode `ring5HealthyBlocks()`): de 9 regels uit de tabel.

**Tests (B):** `ModBlocksTest` (ring 5 na Nether-4, Nether-5 daarna; gras zonder random ticks; zand valt; bladeren
vergaan); `Ring5HealthyBlockTest`: elk blok in `#corrupted` heeft een mapping, geen mapping zonder tag, precies 9, en de
houtset staat er niet in (leest de gegenereerde tag- en data map-JSON, patroon `PoisonDamageTypeTagsTest`); strippen
(log → stripped, wood → stripped wood).

## 2. Pakket 88r — recepten van de houtset (`items`)

`ModRecipeProvider.ring5()` (fase 1-uitzondering zoals de `lagoon_sandstone`-set, contract §11), met de vanilla-
helpers en vanilla-aantallen: planken uit `#corrupted_logs` (1 → 4), hout (4 stammen → 3), gestript hout (4 → 3), trap
(6 → 4), plaat (3 → 6), hek (4 planken + 2 stokken → 3), hekpoort (2 planken + 4 stokken → 1), deur (6 → 3), luik (6 → 2),
knop (1 → 1), drukplaat (2 → 1), bord (6 planken + 1 stok → 3), hangend bord (6 gestripte stammen + 2 kettingen → 6).
Advancements zoals de vanilla-generator (ontgrendeld door planken/stammen). Via de tags werken de vanilla-recepten voor
stokken, werkbank, kist, vat enz. vanzelf; ook `corrupted_sand` → glas (via `#smelts_to_glass`). **Geen andere recepten**
(geen witsteen-set, geen glitchscherf-recepten: fase 2, backlog).

**Tests:** D bevat de 13 recepten + advancements; `ModRecipeProviderTest` (of een JSON-test): de 13 ids bestaan en de
uitvoeraantallen kloppen.

## 3. Pakket 89 — glitch, void, nulliet (`blocks`; na OV2)

| Blok / item | Eigenschappen (voorstel; OV2 beslist de ertsen) | Loot | Tags |
|---|---|---|---|
| `glitch_block` | steen-hardheid (1,5), houweel; geen licht; geanimeerd via `.mcmeta` (geen code), opaak | 1–3 `glitch_shard` (fortune +, max 4), Silk Touch zichzelf | `#mineable/pickaxe`, `void_sources` |
| `glitch_shard` (item) | placeholder-item, geen recept (fase 2) | — | — |
| `voidstone` | deepslate-hardheid (3,0/6), houweel; `animateTick` met **vanilla `ParticleTypes.REVERSE_PORTAL`** als placeholder (zelden, 1 op ~10 ticks; `void_mote` van A volgt in golf 2, 97/98); geen bloklicht (de sterren zijn emissive in het model) | zichzelf | `#mineable/pickaxe`, `#base_stone_overworld`, `#overworld_carver_replaceables`, `ring_5_stones`, `void_sources` |
| `nullite_ore` | in `corrupted_stone`; OV2-A: `needs_diamond_tool`, hardheid +1,5, XP 5–9; emissive overlay (zoals tidesilver), geen bloklicht | `raw_nullite` (`createOreDrop`), Silk Touch zichzelf | `c:ores/nullite`, `c:ores_in_ground/corrupted_stone`, `c:ore_rates/singular`, tool-tag uit OV2, `#mineable/pickaxe` |
| `voidstone_nullite_ore` | in `voidstone`, verder gelijk | idem | idem, `c:ores_in_ground/voidstone` |
| `raw_nullite` (item) | placeholder, geen recept | — | `c:raw_materials/nullite` |

Erts-helper: de gedeelde `oreIn(vanillaOre, extraHardness)` van ring 3/4 (ring 3/4-output byte-gelijk houden).

**Tests:** `ModBlocksTest` (licht, ticks, tool-tags); `void_sources` = precies `glitch_block`, `voidstone`;
`ring_5_stones` = `corrupted_stone`, `voidstone`; loot van `glitch_block` (aantallen).

## 4. Pakket 90 — Nether-5 (`blocks`; na OV2)

- `chalkrack` (kalkrack, wit netherrack): netherrack-eigenschappen, gewone `Block`; `#base_stone_nether`, `c:netherracks`,
  `#infiniburn_overworld`, `#mineable/pickaxe`, `nether_ring_5_stones`.
- **Witsteen-set** (patroon tidestone): `white_stone`, `polished_white_stone`, `white_stone_bricks` elk met `_stairs`/
  `_slab`/`_wall` (bij de bricks `white_stone_brick_*`), plus `white_stone_pillar` (`RotatedPillarBlock`, zoals
  `quartz_pillar`, zonder trap/plaat/muur). Steen-eigenschappen, bleekwitte kaartkleur. `white_stone` in
  `nether_ring_5_stones` en `#nether_carver_replaceables`, **niet** in `#base_stone_nether` (zoals fossielsteen in 68: geen
  vanilla-Nether-ertsvlekken in het witte terrein). Trappen/platen/muren in de vanilla-tags. Geen recepten (backlog).
- `mirror_stone` (spiegelsteen): glad en glanzend (texture), steen-eigenschappen, **normale wrijving** (begaanbaar, geen ijs).
- `still_light` (stil licht): licht 15 (zoals shroomlight), geen `animateTick`, geen ambient-geluid; breek-/stapgeluid
  vanilla (glas); dropt zichzelf.
- `white_core_ore` (in `white_stone`; OV2-A: `needs_diamond_tool`, +1,5, XP 6–10, zachte gloed licht 3, emissive
  overlay) → item `white_core` (één per erts, fortune telt; geen gedrag in fase 1). Tags `c:ores/white_core`,
  `c:ores_in_ground/white_stone`, `c:gems/white_core` (item), tool-tag uit OV2.
- 17 blokken, 1 item.

**Tests:** `ModBlocksTest` (Nether-5 na ring 5; licht `still_light` 15, `white_core_ore` 3); `nether_ring_5_stones` =
`white_stone`, `chalkrack`; `chalkrack` in `#base_stone_nether`, `white_stone` niet.

## 5. Texturepaden (gelijk aan `golf-1-textures.md` §2)

Bestandsnaam = bloknaam; trap/plaat/muur/hek/knop/drukplaat/bord gebruiken de basis (planken, witsteen, …); logs
`<naam>` + `<naam>_top`; `corrupted_wood` = `corrupted_log` rondom, `stripped_corrupted_wood` = `stripped_corrupted_log`
rondom; deuren `_top`/`_bottom` + `item/<naam>_door`.

- `block/`: `corrupted_grass_block_top`, `corrupted_grass_block_side` (zonder overlay/tint; bodem = `corrupted_dirt`),
  `corrupted_dirt`, `corrupted_stone`, `corrupted_cobblestone`, `corrupted_sand`, `corrupted_log`, `corrupted_log_top`,
  `stripped_corrupted_log`, `stripped_corrupted_log_top`, `corrupted_planks`, `corrupted_leaves` (cutout_mipped, geen tint),
  `corrupted_door_top`, `corrupted_door_bottom`, `corrupted_trapdoor`; `glitch_block` (+ `.mcmeta`), `voidstone` +
  `voidstone_glow` (emissive sterren), `nullite_ore_overlay` (emissive, over `corrupted_stone` en `voidstone`);
  `chalkrack`, `white_stone`, `polished_white_stone`, `white_stone_bricks`, `white_stone_pillar`,
  `white_stone_pillar_top`, `mirror_stone`, `still_light`, `white_core_ore_overlay` (emissive, over `white_stone`).
- `item/`: `corrupted_door`, `corrupted_sign`, `corrupted_hanging_sign`, `glitch_shard`, `raw_nullite`, `white_core`.
- `entity/signs/corrupted` (64 × 32), `entity/signs/hanging/corrupted` (64 × 32), `gui/hanging_signs/corrupted` (16 × 16).
- Het deeltje van een bord = `corrupted_planks`, van een hangend bord = `stripped_corrupted_log`.
- Ontbrekende PNG's bij `runData`: tijdelijke kopieën, **niet gecommit** (lijst in `build/b-placeholders.txt`, na
  `runData` verwijderd). INT merget T vóór B's runData-controle.

## 6. Lang
`Ring5BlockNames` (en + nl) voor alle blokken en items (B kiest de Nederlandse namen en zet ze onder "Beslissingen").

## 7. Client/server-impact
- Gedrag server-side (vallen, vergaan, loot, strippen). `animateTick` (voidsteen) draait op het client-level zonder
  client-class.
- Nieuw: een block-entity-type voor bordjes (common) en de renderer-registratie in `client/` (alleen `Dist.CLIENT`).
  Geen `Minecraft.getInstance()` buiten `client/`. `ClientIsolationTest` moet groen blijven.
- Geen payload, geen `PROTOCOL_VERSION`.

## 8. Tests (licht, per pakket) en "af als"
- **B** `./gradlew build` (incl. de tests hierboven) en **D** `runData` + `git diff --stat src/generated` (alleen ring 5-/
  Nether-5-output) per pakket.
- **S-smoke na 88** (bordjes: een nieuw block-entity-type en client-registratie): verse wereld `r5-b88`, log zonder
  ERROR en zonder client-class-fout; RCON `setblock` van een bord en een hangend bord, daarna `data get block` (de block
  entity bestaat). Geen client.
- Geen determinisme/perf (INT).
- **Af als:** alle blokken uit §1–§4 met loot, tags, models, lang; `healthy_block`/`corrupted` exact de 9; 13 recepten;
  B, D, S-smoke groen; per pakket `/code-review` verwerkt.

## Voortgang
- Voortgang: 2026-10-21 20:39 worktree `P:/Projecten/Java/wt/r5-blocks-g1` (branch `ring5/blocks-g1` vanaf `3dfd50aa`), `setup.sh B 0`, eerste build groen.
- Voortgang: 2026-10-21 21:08 88 klaar: B groen (1154 tests, 0 fouten), D groen (alleen ring 5-output), S-smoke `r5-b88` schoon (0 ERROR; bord, wandbord, hangend bord en hangend wandbord via RCON: block entity `minecraft:sign`/`minecraft:hanging_sign`, tekst blijft staan, drops = `corrupted_sign`/`corrupted_hanging_sign`).
- Voortgang: 2026-10-21 21:25 88r klaar: D = 13 recepten + 13 advancements (gelijk aan eik), B groen (1157 tests). Geen S-smoke (alleen data, plan §8).
- Voortgang: 2026-10-21 21:26 OV2 beantwoord (A); 89 en 90 gestart.
- Voortgang: 2026-10-21 21:40 89 klaar: D groen (alleen ring 5-output), B groen (1162 tests). S-smoke volgt samen met 90.
- Voortgang: 2026-10-21 21:48 90 klaar: D groen, B groen (1164 tests); S-smoke `r5-b90` (89 + 90) schoon (0 ERROR): alle nieuwe blokken via `setblock`, `loot … mine` geeft glitchscherf / ruw nulliet / witte kern, voidsteen in `#void_sources`, witte-kernerts in `#needs_diamond_tool`, bord nog steeds `minecraft:sign`.

## CODEBASE-delta
**88 (blocks):**
- Blocks (na Nether-ring 4, sectie "Ring 5"): `corrupted_grass_block` (`Block`, gras-hardheid/-geluid, geen random ticks, geen snowy; loot → `corrupted_dirt`, Silk Touch zichzelf), `corrupted_dirt`, `corrupted_stone` (loot → `corrupted_cobblestone`), `corrupted_cobblestone`, `corrupted_sand` (`ColoredFallingBlock`, stof `0x8C8792`), `corrupted_log`/`corrupted_wood` (`StrippableLogBlock` → `stripped_corrupted_log`/`_wood`), `stripped_corrupted_log`/`_wood`, `corrupted_planks`, `_stairs`, `_slab`, `_fence`, `_fence_gate`, `_door` (`DoubleHighBlockItem`), `_trapdoor`, `_pressure_plate`, `_button` (30 ticks), `corrupted_sign` + `corrupted_wall_sign` (`StandingSignBlock`/`WallSignBlock`, item `SignItem` 16), `corrupted_hanging_sign` + `corrupted_wall_hanging_sign` (`CeilingHangingSignBlock`/`WallHangingSignBlock`, item `HangingSignItem` 16), `corrupted_leaves` (`LeavesBlock`; loot zoals mangrove: schaar/Silk Touch zichzelf, anders stokken). Kaartkleur `COLOR_GRAY` (gras, aarde, steen, keien, schors, bladeren), `COLOR_LIGHT_GRAY` (zand, planken-familie, gestript, bordjes). Wandbordjes zonder eigen item/loot table (`lootFrom` het staande bord).
- `ModWoodTypes.CORRUPTED_SET`/`CORRUPTED` (`corruptedadventure:corrupted`, eik-geluiden) — de **enige geregistreerde** `WoodType` van de mod (`WoodType.register`, voor de bordmaterialen).
- Nieuw `block/ModSigns` (common): `BlockEntityTypeAddBlocksEvent` voegt de 4 bordblokken toe aan vanilla `BlockEntityType.SIGN`/`HANGING_SIGN` (geen eigen block-entity-type, geen eigen renderer). Geregistreerd in `CorruptedAdventure`.
- Nieuw `client/block/SignMaterials` (client): `Sheets.addWoodType(ModWoodTypes.CORRUPTED)` in `FMLClientSetupEvent.enqueueWork`; geregistreerd in `CorruptedAdventureClient` (regel "ring 5 — B (88)").
- Tags: `ModTags.Blocks/Items.CORRUPTED_LOGS` (nieuw, `corruptedadventure:corrupted_logs` → `#logs_that_burn`); inhoud `#corruptedadventure:corrupted` (de 9), `ring_5_stones` (`corrupted_stone`); vanilla: `#dirt`, `#sand`, `#base_stone_overworld` (eerste mod-blok erin), `#overworld_carver_replaceables`, `#mineable/*`, `#planks`, `#wooden_*`, `#fence_gates`, `#standing_signs`, `#wall_signs`, `#ceiling_hanging_signs`, `#wall_hanging_signs`, `#leaves`; `c:cobblestones`, `c:sands`, `c:stripped_logs`/`_woods`, `c:fence_gates/wooden`; items: `#stone_tool_materials`, `#stone_crafting_materials`, `#smelts_to_glass`, `#signs`, `#hanging_signs` (+ de kopieën).
- `ModDataMapProvider.ring5HealthyBlocks()` (`healthy_block`, de 9); compost `corrupted_leaves` 0,3. `ModFlammables`: houtset als eik. `Ring5BlockNames` gevuld (en + nl).
- Tests: `ModBlocksTest` (+5: volgorde ring 5, gedrag, strippen, wandbord-loot, sign-BE-types), `ModWoodTypesTest` (+1), nieuw `Ring5HealthyBlockTest` (tag = data map = de 9; houtset in geen van beide).
- Valkuilen: `Properties.ofFullCopy` kopieert het `drops`-veld; daarom krijgen de wandbordjes expliciet `lootFrom(<staand bord>)`, zodat ze nooit de loot van het eiken-origineel erven (test `corruptedWallSignsDropTheirStandingSign`); `WallSignBlock.getDescriptionId()` = die van het item, dus geen lang-key voor wandbordjes (dubbele sleutel breekt `runData`); `ofFullCopy(GRASS_BLOCK)` kopieert de random ticks — daarom `Properties.of()`.

**88r (items):**
- `ModRecipeProvider.ring5()`: `planksFromLogs` (`#corruptedadventure:corrupted_logs` → 4), `woodFromLogs` (hout, gestript hout), `hangingSign` (gestripte stam + ketting → 6) en de plankenfamilie via vanilla's `generateRecipes` met een eigen `BlockFamily` (niet in `BlockFamilies`' map), groep `wooden_*`, `has_planks`. Ids = resultaat-item (`corruptedadventure:corrupted_*`).
- Test: nieuw `datagen/Ring5RecipesTest` (13 ids + aantallen, advancement per recept, geen andere `corrupted`-recepten).

**89 (blocks):**
- Blocks (na `corrupted_leaves`): `glitch_block` (`Block`, steen-kopie, `COLOR_PURPLE`; loot 1–3 `glitch_shard`, Fortune uniform bonus, max 4, Silk Touch zichzelf), `voidstone` (nieuw `block/VoidstoneBlock`: deepslate-kopie, `COLOR_BLACK`, `animateTick` 1 op 10 een vanilla `REVERSE_PORTAL` uit een open zijde, zoals crying obsidian), `nullite_ore` (diamanterts-kopie +1,5, XP 5–9, `CORRUPTED_COLOR`), `voidstone_nullite_ore` (deepslate-diamanterts +1,5, XP 5–9). Geen bloklicht; sterren en kristallen emissive via `oreWithOverlay` (`voidstone` + `voidstone_glow`, `nullite_ore_overlay`).
- `ModBlocks.RING_5_ORE_EXTRA_HARDNESS = 1.5F` (OV2 = A).
- Items (na `purity_crystal_shard`): `glitch_shard`, `raw_nullite` (placeholders, geen recepten).
- Tags: `void_sources` = glitch_block + voidstone; `ring_5_stones` += voidstone; voidstone in `#base_stone_overworld`, `#overworld_carver_replaceables`; nieuwe keys `ModTags.Blocks/Items.ORES_NULLITE` (`c:ores/nullite`), `ORES_IN_GROUND_CORRUPTED_STONE`, `ORES_IN_GROUND_VOIDSTONE`, `Items.RAW_MATERIALS_NULLITE`; nulliet in `c:ores`, `c:ore_rates/singular`, `#mineable/pickaxe`, `#needs_diamond_tool`.
- Tests: `ModBlocksTest.glitchVoidAndNulliteHaveTheirStrengthXpAndNoLight`, volgorde-test uitgebreid; nieuw `Ring5BlockDataTest` (void_sources, ring_5_stones, nulliet-tags, glitch-loot).

**90 (blocks):**
- Blocks (Nether-ring 5, na `voidstone_nullite_ore`, eindigen de blokken): `chalkrack` (netherrack-kopie, `QUARTZ`, gewone `Block`), `white_stone` + `_stairs`/`_slab`/`_wall`, `polished_white_stone` + idem, `white_stone_bricks` + `white_stone_brick_stairs`/`_slab`/`_wall` (patroon fossielsteen: steen / gepolijst andesiet / steenbakstenen; muren `forceSolidOn`), `white_stone_pillar` (`RotatedPillarBlock`, steen-hardheid), `mirror_stone` (steen, normale wrijving), `still_light` (shroomlight-kopie, licht 15, glasgeluid, geen `animateTick`), `white_core_ore` (diamanterts +1,5, XP 6–10, licht 3 + emissive overlay op `white_stone`). Kaartkleur `SNOW` (witsteen-familie, spiegelsteen, stil licht, erts), `QUARTZ` (kalkrack). 17 blokken.
- Item `white_core` (na `raw_nullite`, laatste los item).
- Tags: `nether_ring_5_stones` = white_stone + chalkrack; chalkrack in `#base_stone_nether`, `c:netherracks`, `#infiniburn_overworld`; white_stone **niet** in `#base_stone_nether`, wel in `#nether_carver_replaceables`; `#stairs`/`#slabs`/`#walls`; `still_light` in `#mineable/hoe` (zoals shroomlight); nieuwe keys `ORES_WHITE_CORE` (`c:ores/white_core`), `ORES_IN_GROUND_WHITE_STONE`, `Items.GEMS_WHITE_CORE` (in `c:gems`); witte kern in `c:ores`, `c:ore_rates/singular`, `#needs_diamond_tool`.
- Tests: `ModBlocksTest.netherRing5BlocksBehaveAsPlanned`, volgorde-test (`ring5AndNetherRing5FollowNetherRing4InOrder`, 17 Nether-5-blokken eindigen de lijst; losse items eindigen op `white_core`); `Ring5BlockDataTest` (+1: Nether-5-stenen en base_stone).

## Beslissingen
- **Bordjes-route (88): (a)** NeoForge 21.1.252 heeft `BlockEntityTypeAddBlocksEvent` → onze bordblokken in de vanilla sign/hanging-sign-types; vanilla renderers (`SignRenderer`/`HangingSignRenderer`, al voor die types geregistreerd) en bewerkschermen doen de rest. Geen `registry/ModBlockEntityTypes`. Client: alleen `Sheets.addWoodType` (defensief tegen class-load-volgorde). Textures volgens §5 (`entity/signs/corrupted`, `entity/signs/hanging/corrupted`, `gui/hanging_signs/corrupted`).
- **Kaartkleuren (88):** `COLOR_GRAY` voor gras/aarde/steen/keien/schors/bladeren, `COLOR_LIGHT_GRAY` voor zand, planken-familie, gestript hout en bordjes; zandstof `0x8C8792`.
- **Nederlandse namen (88):** "verdorven" (verdord = withered, ring 4): Verdorven grasblok, aarde, steen, keisteen, zand, stam, hout; Gestripte verdorven stam, Gestript verdorven hout; Verdorven planken, trap, plaat, hek, poort, deur, valluik, drukplaat, knop, bord, hangbord, bladeren.
- **Blockstates (88):** gras 4 Y-rotaties (zonder tint), aarde/zand 4 rotaties, steen gespiegeld + geroteerd (zoals vanilla steen), keien één model; hout als de verdant-set; bladeren `cutout_mipped` zonder tint.
- **Creative-volgorde (88):** de 9 + houtset in vanilla-volgorde (stam … knop), dan bord, hangend bord, bladeren.
- **Mijn-niveau (89/90): OV2 = A** (gebruiker): `needs_diamond_tool`, vanilla-erts +1,5; XP nulliet 5–9, witte kern 6–10; nulliet zwak emissive zonder bloklicht, witte kern licht 3.
- **Erts-sjablonen (89):** nulliet in `corrupted_stone` = diamanterts, in voidsteen = deepslate-diamanterts (hardheid 4,5 resp. 6,0); het voidsteen-erts toont gewone voidsteen onder de kristallen (geen sterrenlaag: één overlay per model).
- **Voidsteen-deeltje (89):** 1 op 10 display-ticks, alleen uit een zijde zonder stevige buur (begraven voidsteen is stil).
- **Namen (89):** Glitchblok, Glitchscherf, Voidsteen, Nullieterts, Voidsteen-nullieterts, Ruw nulliet.
- **Nether-5 (90):** sjablonen als fossielsteen (68); `still_light` = shroomlight (hardheid 1,0, hak) met glasgeluid en dropt zichzelf; de pilaar als `logBlock` (`white_stone_pillar` + `_top`); witte-kernerts op `white_stone` (diamanterts-sjabloon, want witsteen heeft steen-hardheid).
- **Namen (90):** Kalkrack, Witsteen (+trap/plaat/muur), Gepolijste witsteen (+…), Witstenen blokstenen (+bloksteentrap/-plaat/-muur, zoals getijdesteen), Witsteenzuil, Spiegelsteen, Stil licht, Witte-kernerts, Witte kern.
- **Bladeren-loot (88):** vanilla's mangrove-tabel (geen sapling): schaar/Silk Touch zichzelf, anders 1–2 stokken met de eikkansen.

## Backlog
- `items`: corrupted boot (niet in contract §11).
- `blocks`: schoffel/pad op `corrupted_grass_block`/`corrupted_dirt` (zou ze "genezen" tot vanilla-akkerland; fase 5 beslist).
- `infra` (INT1/main, client nodig): bordjes in-game controleren — staand, wand, hangend, hangend aan de wand plaatsen, tekst bewerken (hangbord-scherm toont `gui/hanging_signs/corrupted`), rendering met T's textures.
- `blocks`: corrupted gras/aarde/zand bewust niet in `#enderman_holdable`, `#animals_spawnable_on` e.d. (dode ring; fase 4/5 kan dat herzien).
