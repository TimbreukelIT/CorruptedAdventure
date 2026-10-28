# Fase 2 golf 5 — stroom H (hemelglas)

Branch `f2g5/skyglass`, worktree `P:/Projecten/Java/wt/f2g5-skyglass`, n = 69. Basis (`git merge-base main HEAD`):
`d55ff491`. Review: `/code-review low d55ff491..f2g5/skyglass`.
Bron: `docs/design/fase-2-items.md` §4.4, §7.2–§7.4, `docs/plans/fase-2/golf-5.md` (rij H).

## Wat
### Pakket 1 (`blocks:`) — hemelglas
- Achteraan de Nether-4-sectie van `ModBlocks` (na `purity_crystal_lamp`, dus tab 4):
  - `sky_sand`: `ColoredFallingBlock` met de eigenschappen van zand, stofkleur lichtblauw `0xC6DCEC`, kaartkleur `COLOR_LIGHT_BLUE`.
  - `sky_glass`: `TransparentBlock` met `ofFullCopy(Blocks.GLASS)` (vanilla-glasgedrag).
  - `sky_glass_pane`: `IronBarsBlock` met `ofFullCopy(Blocks.GLASS_PANE)`.
- Datagen: blockstates/models (glas `cube_all`, ruit zoals vanilla, beide `render_type` translucent), item models (ruit plat op
  `block/sky_glass`), loot (zand zichzelf, glas/ruit alleen met zijdezacht), tags (zie hieronder), lang en/nl
  (`datagen/lang/Golf5SkyGlassNames`), recepten (`datagen/recipe/Golf5SkyGlassRecipes`).
- Tags: `sky_sand` → `mineable/shovel`; `sky_glass` → `minecraft:impermeable`, `c:glass_blocks` (blok + item);
  `sky_glass_pane` → `c:glass_panes` (blok + item).
- Recepten: 4 `bone_sand` + 1 `purity_crystal_shard` (vormloos) → 4 `sky_sand`; `sky_sand` smelten (oven, 200 ticks, 0,1 XP)
  → `sky_glass`; 6 `sky_glass` (`###/###`) → 16 `sky_glass_pane`.
- Placeholder-PNG's uit `tools/textures/Golf5SkyGlassTextures.java`: `block/sky_sand`, `block/sky_glass`,
  `block/sky_glass_pane_top` (T maakt de definitieve).
- Test: `ModBlocksTest` (volgorde Nether-4/ring 5, glasgedrag).

### Pakket 2 — glitchscherfblok (`blocks:`) en void/stilte (`items:`)
- `glitch_shard_block` achteraan de ring 5-sectie (na `nullite_block`, tab 5): `gemStorage(MapColor.COLOR_MAGENTA)`
  (diamantblok-eigenschappen), geen licht; `cube_all`, dropt zichzelf, `mineable/pickaxe` zonder tier (zoals het glitchblok
  waar de scherven uit komen), `c:storage_blocks/glitch_shard` (blok + item, in `c:storage_blocks`). Placeholder
  `block/glitch_shard_block.png` (16×32, 2 frames) + handgeschreven `.png.mcmeta` (frametime 3).
- Recepten 9 `glitch_shard` → blok, blok → 9 (`glitch_shard_from_glitch_shard_block`).
- Void-coating: trim-materiaal `CoatingMaterial.SKY_GLASS` (`corruptedadventure:sky_glass`, item `sky_glass`, kleur
  `0xA8D4F0`, item_model_index 0,8 = diamant); `Coating.VOID` neemt het; stilte houdt `WHITE_CORE`. Atlasbron en
  smithing_trim-/kopie-recept volgen vanzelf uit het enum (kopie-recept void: template + 7 hemelglas + voidsteen → 2).
  Palet `trims/color_palettes/sky_glass.png` via `tools/textures/CoatingTextures.java` (ook het void-template-embleem).
  Lang en/nl "Sky Glass Material" / "Materiaal: hemelglas".
- Ringrecepten: void `G V G / V H V / G V G` (glitch_shard, voidstone, sky_glass) → 1, id
  `void_coating_smithing_template_from_sky_glass`; stilte `Z W Z / W K W / Z W Z` (purity_crystal_shard, white_stone,
  white_core) → 1, id `silence_coating_smithing_template_from_white_core`. Kopie-recepten blijven.
- Tests: `CoatingSmithingTest` (void = hemelglas, stilte = witte kern), `ModBlocksTest` (ring 5-volgorde).

Client/server: alleen registraties en data; geen gameplaylogica, geen client-code.

## Voortgang
- 2026-10-28 16:20 start (setup.sh H 69)
- 2026-10-28 16:37 pakket 1: build (incl. unit tests) en runData groen; commit `fb1f490b`, gemeld aan main
- 2026-10-28 16:55 pakket 2: build, runData, GameTests groen; commits `5a413d69` (blocks), `e6a1e231` (items); palet-pad aan main gemeld
- 2026-10-28 /code-review low d55ff491..f2g5/skyglass: geen bevindingen
- 2026-10-28 main (A + G) gemerged (`1c98ac13`, zonder conflicten); runData zonder diff, build en GameTests (25/25) groen

## Voorlopig (bevestigen bij de teststop)
- Hemelglas en -ruit renderen `translucent` (zoals gekleurd glas), zodat T een getinte texture kan maken.
- `sky_sand` niet in `minecraft:sand` / `c:sands` (gemaakt tussenproduct zoals bottenzand; geen glas via andere recepten);
  `sky_glass` niet in `c:glass_blocks/colorless` / `cheap` (getint en duur).
- Smelten alleen in de oven (zoals vanilla-glas), 0,1 XP.
- Glitchscherfblok: diamantblok-eigenschappen in magenta, elke pickaxe; naam "Glitch Shard Block" / "Glitchscherfblok".
- Template-ringrecepten ontgrendelen bij de hoekscherf (glitchscherf resp. zuiverkristalscherf).
- Hemelglas-trim: item_model_index 0,8 (diamant), tooltipkleur `0xA8D4F0`, paletbasis `0x9CCAEC`.

## W-checklist voor INT
- [ ] Creative-tab 4 toont `sky_sand`, `sky_glass`, `sky_glass_pane` achteraan de Nether-4-blokken.
- [ ] Werkbank: 4 bottenzand + 1 zuiverkristalscherf → 4 hemelzand; 6 hemelglas → 16 ruiten. Oven: hemelzand → hemelglas.
- [ ] Hemelzand valt; hemelglas en ruit breken zonder drop, met zijdezacht droppen ze zichzelf; ruiten verbinden met elkaar en met glas.
- [ ] Tab 5: `glitch_shard_block` na `nullite_block`; flikkert, geeft geen licht; 9 ↔ 1 met glitchscherven.
- [ ] Smidstafel: void-coating met hemelglas (blauwe trim op het harnas), stilte met witte kern; witte kern geeft geen void-coating meer.
- [ ] Werkbank: void- en stilte-template uit het ringrecept; kopie-recepten (void nu met 7 hemelglas) werken.

## CODEBASE-delta
- `ModBlocks`: `SKY_SAND`, `SKY_GLASS`, `SKY_GLASS_PANE` (Nether-4-sectie, fase 2 golf 5).
- `datagen/lang/Golf5SkyGlassNames`, `datagen/recipe/Golf5SkyGlassRecipes`; `golf5SkyGlass()` in de block-state-, loot-,
  block-tag-, item-model- en item-tag-provider.
- `ModBlocks.GLITCH_SHARD_BLOCK` (ring 5-sectie); `ModTags.{Blocks,Items}.STORAGE_BLOCKS_GLITCH_SHARD`.
- `CoatingMaterial.SKY_GLASS`; `Coating.VOID` → hemelglas (fase-2-items.md §4.4: index-lijst aanvullen met "hemelglas = diamant (0,8)").
- `tools/textures/Golf5SkyGlassTextures.java` (placeholders); `CoatingTextures` met materiaal `sky_glass`.

## Beslissingen
- (geen nieuwe; zie "Voorlopig")

## Backlog
- (nog niets)
