# Fase 2 golf 3 — stroom R3 (ring 3-materialen)

Branch `f2g34/ring3`, worktree `P:/Projecten/Java/wt/f2g34-ring3`, n = 69. Basis: main `9becd290`.
Bron: `docs/design/fase-2-items.md` §6.3 (hitte-template) en §6.4, `docs/plans/fase-2/golf-34.md`.

## Wat
1. **Blokken (`blocks:`)**
   - `dew_crystal_block` (Nether-2-sectie, na `dew_crystal_cluster`): vanilla `AmethystBlock`, kopie amethistblok,
     kaartkleur `COLOR_LIGHT_BLUE`, geen licht; `#crystal_sound_blocks`, pickaxe zonder tier.
   - Ring 3-sectie, achteraan (na `fire_ruby_block`):
     - `sulfur_block`: kopie kolenblok, kaartkleur geel; `c:storage_blocks/sulfur`; pickaxe zonder tier.
     - `fire_ruby_inlaid_pumice` + `_stairs`/`_slab`/`_wall`: eigenschappen van puimsteen (`pumice(Blocks.TUFF…)`);
       model `cube_column` zoals het ingelegde zandsteen van M (zijkant `fire_ruby_inlaid_pumice`, boven/onder `_top`).
     - `sulfur_lamp`: kopie zeelantaarn (glas, snel kapot, geen gereedschap nodig), licht 13, kaartkleur limoen.
     - `pumice_raft` (`PumiceRaftBlock`, een `BushBlock`): leliebladregel met lava i.p.v. water (alleen op een lavabron,
       niets vloeibaars op de eigen plek), plaat 16 × 1,5 × 16 px om op te staan, breekt als de lava weggaat; eigenschappen
       van het lelieblad (direct kapot, `DESTROY` bij zuigers) met het geluid van tuff. Item: `PlaceOnWaterBlockItem` (mikt
       op elke bron, dus ook lava).
   - `ember_berry_bush` heeft **geen eigen item meer** (`registerWithoutItem`, zoals vanilla's struik); plukken
     (`useWithoutItem`, vanilla's aantallen en geluid), de loot en pick-block geven `ember_berries`.
   - Loot: alles `dropSelf`, de plaat via `createSlabItemTable`; de struik nu met gloeibessen.
2. **Items (`items:`)**
   - `ember_berries` (ring 3, achteraan): `ItemNameBlockItem` op de struik; voedsel van zoete bessen (2 / 0,1) + 5 s
     vuurbestendigheid (kans 1). `c:foods/berry`, composteerbaar 0,3.
   - `dripshroom_stew` (Nether-2, achteraan): `Foods.MUSHROOM_STEW` (kom terug), stapel 1; `c:foods/soup`.
   - Brandstof (data map `neoforge:furnace_fuels`): `sulfur` 1600 (kolen), `sulfur_block` 16000 (kolenblok).
   - Recepten in `datagen/recipe/Golf3Ring3Recipes.java`.
3. Lang en/nl (`datagen/lang/Golf3Ring3Names`), placeholder-PNG's uit `tools/textures/Golf3Ring3Textures.java`.
4. GameTests `gametest/Golf3Ring3GameTests`; unit test in `ModBlocksTest`.

Client/server: alleen registraties, data en vanilla-gedrag (plukken, plaatsen, eten draaien op de server); geen
client-code, geen netwerk.

## Voortgang
- 2026-10-27 19:35 start (setup.sh R3 69 gedaan)
- 2026-10-27 20:05 blokken, items, datagen, recepten, textures, tests geschreven; runData loopt

## Voorlopig (bevestigen bij de teststop)
- Hitte-template: `RPR / PDP / RPR` (R vuurrobijn, P puimsteen, D dauwkristal) → 1, id `heat_coating_smithing_template_from_fire_ruby`.
- Buskruit: vormloos zwavel + houtskool → 2 (alleen houtskool, geen steenkool), id `gunpowder_from_sulfur`.
- Zwavelblok brandt niet in de wereld (zoals veen; een kolenblok wel) en zit in `c:storage_blocks/sulfur`.
- Ingelegde puimsteen: 8 puimsteen rond 1 vuurrobijn → 8; trap 6 → 4, plaat 3 → 6, muur 6 → 6; steenzaag trap 1, plaat 2, muur 1.
- Zwavellamp: ` S / SGS / S ` (4 zwavel rond glas, zoals de redstonelamp) → 1; zeelantaarn-eigenschappen, geen gereedschap.
- Vlotter: `# # / # #` (4 puimsteen; 2 × 2 is al gepolijste puimsteen) → 2; volle 16 × 16 (een rij vlotters is een dicht pad), direct kapot zoals het lelieblad.
- Dauwkristalblok: 2 × 2 → 1, geen weg terug (zoals amethist).
- `ember_berry_bush` zonder eigen item; in de creative-tab staan de gloeibessen op de plek van de struik.
- Gloeibessen: in `c:foods/berry`, composteerbaar 0,3; druipzwamsoep in `c:foods/soup`.

## W-checklist voor INT
- [ ] Tab 2 (Nether-2) eindigt met het dauwkristalblok; tab 3 heeft de gloeibessen op de plek van de struik en eindigt met zwavelblok, ingelegde puimsteen (4), zwavellamp, vlotter.
- [ ] Rijpe gloeibesstruik plukken → gloeibessen; ze planten de struik op as/mos; eten geeft 5 s vuurbestendigheid.
- [ ] Vlotter: alleen op lava te plaatsen (niet op water), je kunt erop staan, breekt als de lava weg is.
- [ ] Zwavel en zwavelblok branden in de oven zo lang als kolen en een kolenblok.
- [ ] Recepten: hitte-template (+ kopie-recept werkt nog), buskruit, zwavelblok ↔ 9, ingelegde set + steenzaag, lamp, vlotter, dauwkristalblok, druipzwamsoep (kom terug).
- [ ] Zwavellamp licht 13; dauwkristalblok tinkelt.

## CODEBASE-delta
- `block/PumiceRaftBlock` (nieuw): `BushBlock`, lavabron-regel, vorm 16 × 1,5 × 16.
- `block/EmberBerryBushBlock`: `useWithoutItem` en `getCloneItemStack` geven `ModItems.EMBER_BERRIES`.
- `registry/ModBlocks`: Nether-2 `DEW_CRYSTAL_BLOCK` (`AmethystBlock`); ring 3 achteraan `SULFUR_BLOCK`, `FIRE_RUBY_INLAID_PUMICE` + `_STAIRS`/`_SLAB`/`_WALL`, `SULFUR_LAMP` (licht 13), `PUMICE_RAFT` (`registerOnWater`, doc: elke vloeistofbron); `EMBER_BERRY_BUSH` nu `registerWithoutItem`.
- `registry/ModItems`: Nether-2 `DRIPSHROOM_STEW`; ring 3 `EMBER_BERRIES` (`ItemNameBlockItem`), `EMBER_BERRY_FIRE_RESISTANCE_TICKS` = 100.
- `registry/ModTags`: `Blocks`/`Items.STORAGE_BLOCKS_SULFUR` (`c:storage_blocks/sulfur`).
- Providers: per provider een `golf3Ring3()` (blockstates incl. het vlottermodel met 4 willekeurige draaiingen; itemmodellen; loot; blok-tags `mineable/pickaxe`, `crystal_sound_blocks`, `stairs`/`slabs`/`walls`, `c:storage_blocks/sulfur`; item-tags `c:storage_blocks/sulfur`, `c:foods/berry`, `c:foods/soup`; data maps brandstof zwavel 1600 / zwavelblok 16000, compost gloeibessen 0,3). Het itemmodel `ember_berry_bush` vervalt.
- `datagen/lang/Golf3Ring3Names` (en/nl), `datagen/recipe/Golf3Ring3Recipes` (vanuit `ModRecipeProvider`).
- `gametest/Golf3Ring3GameTests`: plukken, planten, eten (vuurbestendigheid, kom terug), vlotter op lava/niet op water/breekt, brandstof.
- `tools/textures/Golf3Ring3Textures.java`: placeholders `block/{sulfur_block,dew_crystal_block,fire_ruby_inlaid_pumice{,_top},sulfur_lamp,pumice_raft}`, `item/{pumice_raft,ember_berries,dripshroom_stew}` (stroom T vervangt ze).

## Beslissingen
_(geen nieuwe: alles binnen §6.3–§6.4 of "Voorlopig")_

## Backlog
- [blocks] Backlog r. 174 (`ember_berry_bush` niet te verplanten) is hiermee afgehandeld (INT vouwt het in).
