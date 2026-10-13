# Ring 4 golf 1 — stroom B (blokken): 66, 67, 68

Scope: `blocks`. Eén commit per pakket, in deze volgorde, op branch `ring4/blocks-g1` (worktree, basis `main` 7ffbfeb).

**Doel:** alle blokken en items van ring 4 en Nether-ring 4 uit `contract.md` §3 (66–68) bestaan, elk met:
- gedrag, loot, tags, blockstates/models, creative tab en lang (`Ring4BlockNames`, Engels + Nederlands);
- models die naar T's texturepaden verwijzen (`contract.md` §7; paden afgestemd met T, zie onder).

Creative-tab-volgorde = registratievolgorde: ring 4 direct na Nether-ring 3 (`primordial_ice`), dan Nether-ring 4. De losse items komen na `fire_crystal`.

Sjablonen: pumice-/koudlava-set (steenfamilie), palm-set (brandbare houtset), dauw- en vuurkristal (`AmethystClusterBlock`, `crystalCluster`), `DewSourceBlock` (groei), ring 3-ertsen (`oreIn`, gedeelde overlay).

## Pakketten

### 66 — natuurblokken
- **`tidestone`-set** (patroon pumice + cobbled):
  - `tidestone`, `cobbled_tidestone`, `polished_tidestone`, `tidestone_bricks`, elk met `_stairs`/`_slab`/`_wall` (bij de bricks: `tidestone_brick_*`);
  - eigenschappen van stone / cobblestone / polished andesite / stone bricks, kaartkleur blauwgrijs (`TERRACOTTA_LIGHT_BLUE`);
  - `tidestone` breekt in `cobbled_tidestone` (Silk Touch: zichzelf), zoals stone/cobblestone en kloofsteen.
- **`moonpetal`-houtset** (brandbaar, zoals palm):
  - log, wood, stripped, planks, stairs, slab, fence, fence_gate, door, trapdoor, pressure_plate, button, `moonpetal_leaves` (bloesem, vaste kleur), `moonpetal_sapling` + `potted_moonpetal_sapling`;
  - `BlockSetType`/`WoodType` `corruptedadventure:moonpetal` met de geluiden van vanilla's kersenhout (bloesemboom);
  - de sapling groeit via `ModTreeGrowers.MOONPETAL` naar `corruptedadventure:ring_4/moonpetal_tree` (key in `ModConfiguredFeatures`, blok `// ring 4 — B`); de feature komt in 75 (W-biomes), tot dan wordt bonemeal verbruikt zonder groei (zoals palm vóór 23);
  - vuurkansen als eik; compost als bladeren/saplings.
- **`withered_moonpetal`**: alleen log, wood, stripped log, stripped wood (brandbaar, grijs).
- **`peat`**: aarde-achtig (schep), in `#dirt` (planten groeien erop), niet brandbaar in de wereld (besluit gebruiker na de review van 66), brandstof 1.200 ticks (6 items; zwakker dan kolen, want het hele oppervlak van `peat_moors` is veen).
- **`hanging_swamp_moss`**: hangt onder een stevige onderkant of onder ander mos (stapelt naar beneden), loopbaar, vervangbaar, alleen met een schaar, brandbaar als ranken.
- **`giant_lily_pad`**: multiblok 2×2 (besluit golf 1):
  - `part` = `north_west`/`north_east`/`south_west`/`south_east`; lelieblad-gedrag (op water, beloopbaar, boten breken hem);
  - plaatsen: het aangeklikte vak is de hoek bij de speler, het blad loopt in de kijkrichting en rechtsom; alleen als alle vier vakken vrij zijn en op water liggen;
  - mist een deel, dan verdwijnen de andere (net als deur/bed); de loot hangt alleen aan `north_west`, dus breken geeft precies één item; in creatief geen drop.
- **`lotus`** (V1 = A): drijft plat op het water, lelieblad-gedrag (vanilla `WaterlilyBlock`, lily-pad-item); één plat model met texture `lotus` (bovenaanzicht).
- **`reeds`** (V2 = B, `ReedsBlock`):
  - een kolom zoals suikerriet, doorloopbaar, op `#dirt`/`#sand` (ook droog);
  - het onderste blok mag waterlogged staan, in water van 1 diep;
  - het groeit zoals suikerriet (random ticks, server-side) tot 3 hoog, alleen als de voet in of naast water staat (of de grond eronder naast water ligt);
  - `top` toont de pluim (`reeds_top`), de rest de stengel (`reeds`).
- **Gloeiflora** (twee lagen: kruis + emissive kruis):
  - `glowcap` (licht 6) en `blighted_glowcap` (licht 3, zelfde klasse) op elk blok met een stevige bovenkant;
  - `glow_bloom` (licht 7): bloem, Night Vision in suspicious stew;
  - `glowing_hanging_roots` (licht 5): vanilla `HangingRootsBlock`.
  - Lichtwaarden (B kiest, "subtiel"): 6 / 3 / 7 / 5 — onder gloeikorstmos (7) en ver onder gloeibessen (14).
- **`bubble_spring`**: bodemblok (steen-eigenschappen, houweel); `animateTick` maakt vanilla `bubble_column_up`-bellen als er water boven staat; geen stroming, geen geluid (A koppelt `block.bubble_spring.bubble` in golf 2).

### 67 — ertsen
- `tidesilver_ore`, `tidestone_tidesilver_ore`, `deepslate_tidesilver_ore` → `raw_tidesilver`;
- `jade_ore`, `tidestone_jade_ore`, `deepslate_jade_ore` → `jade`.
  - V3 = B: hardheid vanilla-gastgesteente-erts +1,0, `needs_diamond_tool`, XP tidesilver 4–7 en jade 5–9; tidesilver zwakke gloed (licht 3, emissive overlay), jade mat.
- **Gifkristal en aquamarijn** (amethist-patroon):
  - `toxic_crystal_block`, `budding_toxic_crystal`, `small_/medium_/large_toxic_crystal_bud`, `toxic_crystal_cluster`, item `toxic_crystal_shard`; idem `aquamarine_*`;
  - de budding-blokken groeien via random ticks, server-side (nieuwe generieke `BuddingCrystalBlock`, stadia als parameter; ook in waterbronnen: aquamarijn onder water);
  - drop als amethist (budding niets, knoppen alleen Silk Touch, cluster 4 scherven met houweel);
  - stadia met licht 1/2/4/5 (amethist); gifkristal-stadia emissive (pulseren, zoals vuurkristal), aquamarijn mat en stil (T, 71a).
- Items zijn placeholders zonder recipes. Tags: `c:ores/tidesilver`, `c:ores/jade`, `c:raw_materials/tidesilver`, `c:gems/jade|toxic_crystal|aquamarine`, `c:budding_blocks`, `c:buds`, `c:clusters`.

### 68 — Nether-4
- `bone_sand`: valt zoals zand (`ColoredFallingBlock`, beige stof).
- `bleached_netherrack`: netherrack-eigenschappen, beige; een gewone `Block` (geen nylium-spreiding), in `#base_stone_nether`, `c:netherracks` en `#infiniburn_overworld` (zoals netherrack).
- Fossielsteen-set: `fossil_stone`, `polished_fossil_stone`, `fossil_stone_bricks` + `_stairs`/`_slab`/`_wall` (`fossil_stone_brick_*`); steen-eigenschappen, bleke kaartkleur; `fossil_stone` in `#base_stone_nether`.
- Zuiverkristal: `purity_crystal_block` (amethistblok, geen licht), `purity_crystal_cluster` (kruis, licht 5, geen groei, dropt `purity_crystal_shard` zoals amethist), `purity_crystal_lamp` (licht 15, dropt zichzelf).

## Texturepaden (afgestemd met T, 2026-10-13)
- Bestandsnaam = bloknaam; trap/plaat/muur gebruiken de basis; logs `<naam>` + `<naam>_top`; deuren `_top`/`_bottom` + `item/<naam>_door`.
- Ertsen: `tidesilver_ore_overlay`, `jade_ore_overlay` over stone/tidestone/deepslate.
- Gloeiflora: `<naam>` + `<naam>_glow`.
- `giant_lily_pad_<part>` + `item/giant_lily_pad`; `bubble_spring_top`/`_side`.
- Items: `item/raw_tidesilver`, `jade`, `toxic_crystal_shard`, `aquamarine_shard`, `purity_crystal_shard`.
- Ontbrekende PNG's bij `runData`: tijdelijke kopieën, niet gecommit (lijst in `build/b-placeholders.txt`, na `runData` verwijderd).

## Client/server-impact
- Al het gedrag draait server-side: lelieblad-multiblok, groei van de budding-blokken, saplings, loot.
- `animateTick` (bellen) draait op het client-level, maar zonder client-class.
- Geen payloads, geen nieuwe client-code.

## Tests (licht, per pakket)
- **B:** `./gradlew build`, met:
  - `ModBlocksTest`: volgorde ring 4 na Nether-3 en Nether-4 daarna, licht en geen ticks waar gepland;
  - unit tests voor `GiantLilyPadBlock` (delen, plaatsing, afbraak) en `BuddingCrystalBlock` (groei).
- **D:** `runData` + `git diff --stat src/generated` (alleen ring 4-output).
- Geen runServer/runClient/determinisme/perf (INT).

## Voortgang
- 2026-10-13: plan; vragen V1–V3 naar main; texturepaden met T afgestemd.
- Voortgang: 20:03 66 klaar: B groen (incl. `GiantLilyPadBlockTest`, ring 4-tests in `ModBlocksTest`/`ModTreeGrowersTest`), D groen met 39 tijdelijke placeholder-PNG's (verwijderd, niet gecommit); `src/generated`: alleen ring 4-output.
- Voortgang: 20:11 67 klaar: B groen (`BuddingCrystalBlockTest`, ertsen/kristallen in `ModBlocksTest`), D groen (tijdelijke textures uit T's 3283f6a, verwijderd); `src/generated`: alleen ring 4-output.
- Voortgang: 20:16 68 klaar: B groen (Nether-4 in `ModBlocksTest`), D groen; `src/generated`: alleen Nether-4-output.
- Voortgang: 20:23 reviews 66 + 67 verwerkt: B groen (rotate/mirror-tests), D groen; `src/generated`: carver-tag zonder veen, `c:ores_in_ground/tidestone`.

## CODEBASE-delta

**66 — Belangrijke classes**
- `block/GiantLilyPadBlock.java` (nieuw): 2×2-multiblok, `PART` (`Part` = `north_west`/`north_east`/`south_west`/`south_east`, met `from`/`northWest`), lelieblad-regels (`mayPlaceOn` water/ijs, boot breekt hem). Plaatsing via `getStateForPlacement` (`northWestCorner`: aangeklikt vak = hoek bij de speler, blad in kijkrichting en rechtsom) + `setPlacedBy`; `updateShape` laat een deel zonder buurdeel verdwijnen; loot alleen op `north_west`; creatief zonder drop (`playerWillDestroy`, zoals het bed).
- `block/ReedsBlock.java` (nieuw): riet, kolom zoals suikerriet (`AGE`, `WATERLOGGED`, `TOP`), op `#dirt`/`#sand`; het onderste blok mag waterlogged staan in water van 1 diep; groeit tot `MAX_HEIGHT` 3, alleen met water bij de voet.
- `block/HangingSwampMossBlock.java` (nieuw): hangende wortels die ook onder ander mos hangen (strengen), waterlogbaar.
- `block/GlowcapBlock.java` (nieuw): gloeizwam (`glowcap`, `blighted_glowcap`) op elke stevige bovenkant; geen random ticks.
- `block/BubbleSpringBlock.java` (nieuw): `animateTick` met vanilla `bubble_column_up` als er water boven staat; verder geen gedrag.
- `block/ModWoodTypes.java`: + `MOONPETAL_SET` (geregistreerd, kersengeluiden) en `MOONPETAL` (niet geregistreerd).
- `block/ModTreeGrowers.java`: + `MOONPETAL` → `ModConfiguredFeatures.MOONPETAL_TREE` (`corruptedadventure:ring_4/moonpetal_tree`; blok `// ring 4 — B`; de feature komt in 75).
- `block/ModFlammables.java`: + moonpetal- en verdorde stammen (5/5), planken-set (5/20), bladeren (30/60), hangend mos (15/100), gloeiende hangwortels (30/60), gloeibloem (60/100); veen bewust niet.
- Na de review van 66: `GiantLilyPadBlock` draait/spiegelt `PART` mee (`rotate`/`mirror`, `Part.rotate`/`mirror`) en weigert plaatsing als een van de vier vakken bezet is door een entity (`isUnobstructed`); `ReedsBlock.canPlaceLiquid` (alleen het onderste blok, water van 1 diep); `hanging_swamp_moss` zonder zijwaartse offset.
- `block/ModFlowerPots.java`: + `potted_moonpetal_sapling`.
- `registry/ModBlocks.java`: sectie Ring 4 na Nether-ring 3; helpers `tidestone`, `moonpetal`, `glowcap`, `registerOnWater` (`PlaceOnWaterBlockItem`).
- `registry/ModTags.java`: + `Items.MOONPETAL_LOGS`, `Items.WITHERED_MOONPETAL_LOGS`.
- Datagen:
  - `ModBlockStateProvider.ring4()`, met `glowingCross` (kruis + emissive kruis, `<naam>_glow`), `flatPad` (plat model als het lelieblad), `giantLilyPad` en `crossPlanes` (gedeeld met `emissiveCross`);
  - `ModItemModelProvider.ring4()`, `ModBlockLootProvider.ring4()`, `ModBlockTagsProvider.ring4()`, `ModItemTagsProvider.ring4()`;
  - `ModDataMapProvider`: compost en `furnace_fuels` voor veen (`PEAT_BURN_TIME` 1.200);
  - `Ring4BlockNames` (EN + NL).

**66 — Registries:** 46 blokken (45 met item; `potted_moonpetal_sapling` zonder), zie contract §3.

**66 — Tags:**
- `moonpetal_logs` en `withered_moonpetal_logs` (blok + item) → `#logs_that_burn`;
- `ring_4_stones` = `tidestone`; `tidestone` in `#overworld_carver_replaceables` (`peat` via `#dirt`); `peat` in `#dirt`;
- `cobbled_tidestone` in `c:cobblestones` en in de stone-tool/crafting-materials.

**67 — Belangrijke classes**
- `block/BuddingCrystalBlock.java` (nieuw): generiek budding-blok (amethistpatroon) met de stadia als parameter (`stages()`, codec met `stages`); random tick 1 op `GROWTH_CHANCE` 5, `grownState` (lucht → kleine knop, waterbron → waterlogged knop, stadium → volgend stadium in dezelfde richting). Gebruikt door `budding_toxic_crystal` en `budding_aquamarine`.
- `registry/ModBlocks.java`: ertsen (`ring4Ore`: vanilla-erts + `RING_4_ORE_EXTRA_HARDNESS` 1,0) en kristallen na `bubble_spring`.
- `registry/ModItems.java`: `raw_tidesilver`, `jade`, `toxic_crystal_shard`, `aquamarine_shard` (placeholders, na `fire_crystal`).
- `registry/ModTags.java`: + `Blocks/Items.ORES_TIDESILVER`, `ORES_JADE`; `Items.RAW_MATERIALS_TIDESILVER`, `GEMS_JADE`, `GEMS_TOXIC_CRYSTAL`, `GEMS_AQUAMARINE`.
- Datagen: `ModBlockStateProvider.ring4Ores()` (overlay over stone/tidestone/deepslate; tidesilver emissive), loot (`createOreDrop`; kristallen zoals amethist), tags, lang.

**68 — Nether-4**
- `registry/ModBlocks.java`: sectie Nether ring 4 na de ring 4-kristallen: `bone_sand` (`ColoredFallingBlock`, `BONE_DUST_COLOR`), `bleached_netherrack` (gewone `Block`), fossielsteen-set (`recolored`, de hernoemde `crystalStage`), `purity_crystal_block`/`_cluster`/`_lamp` (licht 0/5/15).
- `registry/ModItems.java`: `purity_crystal_shard` (laatste los item). `ModTags.Items.GEMS_PURITY_CRYSTAL`.
- Datagen: `netherRing4()` in de blockstate-, item-model-, loot- en tag-providers; het cluster emissive (pulseert, T); lang.
- Tags: `#base_stone_nether` (alleen gebleekt netherrack; fossielsteen in `#nether_carver_replaceables`, review 68), `c:netherracks` en `#infiniburn_overworld` (gebleekt netherrack), `#nether_carver_replaceables` (bottenzand), `#crystal_sound_blocks` (blok, lamp), `c:clusters`, `c:gems/purity_crystal`.
- 17 blokken, 1 item.

**67 — Registries:** 18 blokken, 4 items. **Tags:** `c:ores/tidesilver|jade`, `#needs_diamond_tool`, `c:ore_rates/singular`, `c:ores_in_ground/stone|deepslate`, `#crystal_sound_blocks`, `c:budding_blocks|buds|clusters`, `c:raw_materials/tidesilver`, `c:gems/jade|toxic_crystal|aquamarine`.

## Beslissingen
- 2026-10-13, `blocks`: `lotus` drijft plat op het water als een lelieblad (blad + bloem in één model, lily-pad-item) — V1 = A, simpel en bruikbaar op elke diepte.
- 2026-10-13, `blocks`: `reeds` is een kolom zoals suikerriet die tot 3 hoog groeit (random ticks), alleen in/naast water; het onderste blok mag waterlogged staan in water van 1 diep; doorloopbaar — V2 = B, farmbaar riet.
- 2026-10-13, `blocks`: de ring 4-ertsen `tidesilver` en `jade` hebben `needs_diamond_tool` nodig, hardheid vanilla-erts +1,0, XP 4–7 / 5–9; tidesilver zwakke gloed (licht 3), jade mat — V3 = B, een duidelijke tier-stap.
- 2026-10-13, `blocks`: `purity_crystal_cluster` (kruis, geen groei, dropt `purity_crystal_shard` zoals amethist) + item `purity_crystal_shard` — de lezing van contract §3 "`purity_crystal_shard`/cluster", akkoord main.
- 2026-10-13, `blocks` (implementatie B): lichtwaarden van de gloeiflora: `glowcap` 6, `glow_bloom` 7, `glowing_hanging_roots` 5, `blighted_glowcap` 3.
- 2026-10-13, `blocks` (implementatie B): `moonpetal` met de geluiden van vanilla's kersenhout; `peat` is `#dirt` en brandstof (1.200 ticks).
- 2026-10-13, `blocks`: `peat` is wel brandstof maar niet brandbaar in de wereld (besluit gebruiker na de review van 66) — geen vuur dat over de veenvlakten trekt.

## Code-review
- **66** (review main, verwerkt in de vervolgcommit):
  - `GiantLilyPadBlock`: rotate/mirror, entity-check op de drie andere vakken;
  - `ReedsBlock.canPlaceLiquid`;
  - moss zonder offset; gloeiende hangwortels brandbaar 30/60; veen niet brandbaar; veen niet dubbel in de carver-tag.
  - **Niet opgepakt:** het categorie-punt over models. Blok-datagen inclusief models is van B (`waves.md` §1), zoals bij ring 3.
- **67** (verwerkt in dezelfde vervolgcommit):
  - stages in `BuddingCrystalBlock` gememoiseerd;
  - één `oreIn(vanillaOre, extraHardness)` voor ring 3 en 4 (ring 3-output byte-gelijk, runData-diff leeg voor ring 3);
  - `c:ores_in_ground/tidestone`.
  - **Niet opgepakt:**
    - een gedeelde groeihelper met `DewSourceBlock`: ring 2 is vergrendeld, dus naar de backlog (INT);
    - ontbrekende textures tot T gemerged is: INT merget alles vóór de smoke;
    - `CODEBASE.md`: dat doet INT.

- **68** (verwerkt in de vervolgcommit):
  - `#base_stone_nether` alleen `bleached_netherrack`; `fossil_stone` niet, zodat er geen vanilla Nether-ertsen/debris-vlekken in het fossielterrein komen. In plaats daarvan staat het in `#nether_carver_replaceables`, zodat grotten er nog door snijden.
    - **Afwijking van ring 3:** daar staat de steenfamilie `cold_lava` wél in `#base_stone_nether`.
  - `crystalStage` hernoemd naar de generieke `recolored(vanilla, color)`, die ook de fossielsteen-set bedient (geen aparte `fossilStone`-helper).
  - **Niet opgepakt:**
    - runData zonder T's textures: bekend protocol, INT merget T vóór runData/smoke;
    - `bone_sand`-plafonds: W-nether (surface-plafondwacht);
    - `CODEBASE.md`: INT.

## Backlog
- `atmosphere`: bloesem-particle (`blossom`) uit `moonpetal_leaves` (animateTick), golf 2 (A levert de sprite, B de koppeling).
- `blocks`: potten voor `glow_bloom` en `lotus` (niet in het contract).
- `blocks` (INT): één gedeelde groeihelper voor `DewSourceBlock` (ring 2) en `BuddingCrystalBlock` (review 67); ring 2 is nu vergrendeld.
- `blocks` (72): `poison_gases` (TagKey van C) vullen met `swamp_gas`; let bij de merge op `ModTags`.
