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

Client/server: alleen registraties en data; geen gameplaylogica, geen client-code.

## Voortgang
- 2026-10-28 16:20 start (setup.sh H 69)
- 2026-10-28 16:37 pakket 1: build (incl. unit tests) en runData groen; commit

## Voorlopig (bevestigen bij de teststop)
- Hemelglas en -ruit renderen `translucent` (zoals gekleurd glas), zodat T een getinte texture kan maken.
- `sky_sand` niet in `minecraft:sand` / `c:sands` (gemaakt tussenproduct zoals bottenzand; geen glas via andere recepten);
  `sky_glass` niet in `c:glass_blocks/colorless` / `cheap` (getint en duur).
- Smelten alleen in de oven (zoals vanilla-glas), 0,1 XP.

## W-checklist voor INT
- [ ] Creative-tab 4 toont `sky_sand`, `sky_glass`, `sky_glass_pane` achteraan de Nether-4-blokken.
- [ ] Werkbank: 4 bottenzand + 1 zuiverkristalscherf → 4 hemelzand; 6 hemelglas → 16 ruiten. Oven: hemelzand → hemelglas.
- [ ] Hemelzand valt; hemelglas en ruit breken zonder drop, met zijdezacht droppen ze zichzelf; ruiten verbinden met elkaar en met glas.

## CODEBASE-delta
- `ModBlocks`: `SKY_SAND`, `SKY_GLASS`, `SKY_GLASS_PANE` (Nether-4-sectie, fase 2 golf 5).
- `datagen/lang/Golf5SkyGlassNames`, `datagen/recipe/Golf5SkyGlassRecipes`; `golf5SkyGlass()` in de block-state-, loot-,
  block-tag-, item-model- en item-tag-provider.
- `tools/textures/Golf5SkyGlassTextures.java` (placeholders).

## Beslissingen
- (geen nieuwe; zie "Voorlopig")

## Backlog
- (nog niets)
