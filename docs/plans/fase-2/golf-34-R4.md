# Fase 2 golf 3 + 4 — stroom R4 (ring 4-materialen)

Branch `f2g34/ring4`, worktree `P:/Projecten/Java/wt/f2g34-ring4`, n = 61. Basis: main `9becd290`.
Bron: `docs/design/fase-2-items.md` §6.6–§6.7, `docs/plans/fase-2/golf-34.md`.

## Wat
1. **Blokken (`blocks:`)** — achteraan de ring 4-sectie van `ModBlocks` (na `jade_block`):
   - `jade_inlaid_tidestone` + `_stairs`/`_slab`/`_wall`: eigenschappen van de tidestone-familie (`tidestone(Blocks.STONE…)`,
     muur `forceSolidOn` zoals `tidestone_wall`), maar dropt **zichzelf** (geen cobbled tidestone: een gemaakt blok).
   - `aquamarine_inlaid_lagoon_sandstone` + `_stairs`/`_slab`/`_wall`: eigenschappen van lagoon sandstone (`lagoon(Blocks.SANDSTONE…)`).
   - Model zoals het turkoois-ingelegde zandsteen: `cube_column` met zijkant `<blok>` en boven/onder `<blok>_top`.
   - Loot `dropSelf`, plaat `createSlabItemTable`. Tags: `mineable/pickaxe` (geen tier), `stairs`/`slabs`/`walls`;
     het lagune-blok ook `c:sandstone/blocks|slabs|stairs`. Geen carver-, ring- of `#stone`-tags (geen natuurlijk gesteente).
   - Lang en/nl (`datagen/lang/Golf4MaterialNames`). Placeholder-PNG's uit `tools/textures/Golf4MaterialTextures.java`.
2. **Recepten (`items:`)** in `datagen/recipe/Golf4MaterialRecipes.java`: gif-template, kou-template, de twee ingelegde sets.
3. **Veen als brandstof:** bestond al (pakket 66, `ModDataMapProvider.PEAT_BURN_TIME` = 1200 ticks = 6 items, beslissing
   2026-10-13). Niets gewijzigd.

Client/server: alleen registraties en data; geen gameplaylogica, geen client-code.

## Voortgang
- 2026-10-27 ~19:25 start (setup.sh R4 61); ontwerpvraag kou-template-kernmateriaal naar main
- 2026-10-27 19:41 blokken, datagen, gif-template, placeholders, ModBlocksTest; runData loopt
- 2026-10-27 kou-template na het antwoord van main (oerijs) toegevoegd

## Voorlopig (bevestigen bij de teststop)
- Ingelegde sets: 8 steen rond 1 edelsteen → 8 (zoals M), basissteen `tidestone` resp. `lagoon_sandstone`; trap 6 → 4,
  plaat 3 → 6, muur 6 → 6; steenzaag trap 1, plaat 2, muur 1.
- Ingelegde sets droppen zichzelf (tidestone zelf geeft cobbled tidestone).
- Gif-template: `J S J / S C S / J S J` (J jade, S tidestone, C oerijs) → 1, id `poison_coating_smithing_template_from_primordial_ice`.
- Kou-template: `F S F / S C S / F S F` (F vuurkristal, S `cold_lava` zoals in het kou-kopie-recept, C oerijs) → 1, id
  `cold_coating_smithing_template_from_primordial_ice`; de ringsteen `cold_lava` is voorlopig.

## W-checklist voor INT
- [ ] Creative-tab 4 toont de twee ingelegde sets achteraan de ring 4-blokken (na `jade_block`).
- [ ] Werkbank: beide ingelegde blokken (8), trap/plaat/muur; steenzaag: de drie vormen per set.
- [ ] Werkbank: gif- en kou-template uit het ringrecept; de kopie-recepten werken nog.
- [ ] Alle nieuwe blokken mijnbaar met een houten pickaxe, droppen zichzelf (dubbele plaat = 2).
- [ ] Veen in een oven: 6 items per blok.

## CODEBASE-delta
- `registry/ModBlocks` ring 4 achteraan: `JADE_INLAID_TIDESTONE` + `_STAIRS`/`_SLAB`/`_WALL` (`tidestone(Blocks.STONE…)`,
  muur `forceSolidOn`), `AQUAMARINE_INLAID_LAGOON_SANDSTONE` + `_STAIRS`/`_SLAB`/`_WALL` (`lagoon(Blocks.SANDSTONE…)`).
- Providers: per provider een `golf4Materials()` (blockstates via `inlaidSet(...)`: `cube_column` zijkant `<blok>`,
  boven/onder `<blok>_top`; itemmodellen; loot `dropSelf`/plaat; tags `mineable/pickaxe`, `stairs`/`slabs`/`walls`,
  `c:sandstone/blocks|slabs|stairs` voor het lagune-blok).
- `datagen/lang/Golf4MaterialNames` (en/nl), aangeroepen door beide taalproviders.
- `datagen/recipe/Golf4MaterialRecipes` (vanuit `ModRecipeProvider`): `poison_coating_smithing_template_from_primordial_ice`,
  `cold_coating_smithing_template_from_primordial_ice`, de twee ingelegde sets (+ familie via `generateRecipes`, steenzaag `…_stonecutting`).
- `tools/textures/Golf4MaterialTextures.java`: placeholders `block/jade_inlaid_tidestone{,_top}`,
  `block/aquamarine_inlaid_lagoon_sandstone{,_top}` (stroom T vervangt ze).
- Test: `ModBlocksTest.inlaidSetsMirrorTheirStone`; ring 4-volgorde in `netherRing4FollowsRing4InOrderAndBehavesAsPlanned`.

## Beslissingen
- 2026-10-27 · items · Kou-coating-template: kernmateriaal 1 oerijs (`primordial_ice`), dus 4 vuurkristal + 4 Nether-3-steen
  + 1 oerijs → 1 · gebruiker (optie A): vuurkristal is al het coatingmateriaal; oerijs is het eigen materiaal van Nether-3, symmetrisch met gif.

## Backlog
_(geen)_
