# Fase 2 golf 2 — stroom M (materialen)

Branch `f2g2/materials`, worktree `P:/Projecten/Java/wt/f2g2-materials`, n = 63. Basis: main `81703491`.
Bron: `docs/design/fase-2-items.md` §5.2–§5.4, `docs/plans/fase-2/golf-2.md`.

## Wat
1. **Blokken (`blocks:`)**
   - `soul_crystal_block` (Nether-1-sectie): vanilla `AmethystBlock` (tinkelt bij een treffer, `#crystal_sound_blocks`),
     eigenschappen van het amethistblok, kaartkleur `DIAMOND` (zielvuur-cyaan), geen licht; pickaxe, geen tier-eis.
   - `turquoise_inlaid_sandstone` + `_stairs`/`_slab`/`_wall` (ring 2-sectie, achteraan): eigenschappen als scorched
     sandstone (`scorched(Blocks.SANDSTONE…)`); muur met `forceSolidOn` zit al in vanilla's `SANDSTONE_WALL`-kopie.
     Model als cut sandstone: `cube_column` met zijkant `turquoise_inlaid_sandstone` en boven/onder
     `turquoise_inlaid_sandstone_top`.
   - `salt_lamp` (ring 2-sectie, achteraan): eigenschappen van het zoutblok + licht 12, **volle kubus** als hitbox.
     Blokmodel handgeschreven (`src/main/resources/.../models/block/salt_lamp.json`, placeholder `cube_all` op
     `block/salt_lamp`); datagen maakt alleen de blockstate en het itemmodel. **T mag een andere vorm voorstellen**; een
     niet-volle vorm vraagt dan een `VoxelShape` + `noOcclusion()` in `ModBlocks` (een `blocks`-change, geen T-werk).
   - Loot: alles `dropSelf`, de plaat via `createSlabItemTable`. Tags: `mineable/pickaxe`, `stairs`/`slabs`/`walls`,
     `c:sandstone/blocks|slabs|stairs` voor de ingelegde set. Lang en/nl. Placeholder-PNG's uit
     `tools/textures/Golf2MaterialTextures.java` (afgeleid van onze eigen scorched sandstone, zout, turkoois en
     zielkristal; geen vanilla-pixels).
2. **Recepten (`items:`)** in `datagen/recipe/Golf2MaterialRecipes.java`.
3. Build, runData + generated meecommitten, commits, `/code-review low`.

Client/server: alleen registraties en data; geen gameplaylogica, geen client-code.

## Voortgang
- 2026-10-27 17:17 start (setup.sh M 63 gedaan)
- 2026-10-27 17:34 blokken + recepten gebouwd; runData en build groen (ModBlocksTest: ring 2-volgorde bijgewerkt voor de nieuwe blokken)

## Voorlopig (bevestigen bij de teststop)
- `soul_crystal_block`: geen tier-eis (zoals amethist), geen licht, kaartkleur `DIAMOND`.
- `soul_crystal_block` staat in de Nether-1-sectie en komt dus in **tab 1** (§4.1: Nether-ring N in tab N), niet in tab 2 zoals `golf-2.md` noemt.
- Ingelegd zandsteen: 8 scorched sandstone rond 1 turkoois → 8 (zoals gekleurd glas/terracotta); trap 6 → 4, plaat 3 → 6, muur 6 → 6 (vanilla-familie); steenzaag: trap 1, plaat 2, muur 1.
- Ingelegd zandsteen in `c:sandstone/blocks|slabs|stairs` (zoals gebeiteld zandsteen).
- Zoutlamp: fakkel boven op een zoutblok → 1 (zoals de jack o'lantern).
- Zielkristalblok: 2×2 zielkristal → 1 (zoals amethist).
- Uitdrogings-template: `T S T / S C S / T S T` (T turkoois, S scorched sandstone, C barrel cactus) → 1, id `dehydration_coating_smithing_template_from_barrel_cactus`.

## W-checklist voor INT
- [ ] Creative-tab 1 toont `soul_crystal_block` na de zielkristalertsen; tab 2 toont het ingelegde zandsteen en de zoutlamp achteraan de ring 2-blokken.
- [ ] Zoutlamp geeft licht 12; zielkristalblok tinkelt bij een treffer.
- [ ] Alle vijf recepten in de werkbank + de drie steenzaagrecepten; het kopie-recept van het uitdrogings-template werkt nog.
- [ ] Alle nieuwe blokken mijnbaar met een houten pickaxe en ze droppen zichzelf (plaat dubbel = 2).

## CODEBASE-delta
- `registry/ModBlocks`: Nether-1 `SOUL_CRYSTAL_BLOCK` (`AmethystBlock`, kopie amethistblok, `MapColor.DIAMOND`); ring 2 achteraan `TURQUOISE_INLAID_SANDSTONE` + `_STAIRS`/`_SLAB`/`_WALL` (`scorched(Blocks.SANDSTONE…)`), `SALT_LAMP` (kopie `SALT_BLOCK`, licht 12, volle kubus).
- Providers: per provider een `golf2Materials()` (blockstates: ingelegd = `cube_column` zijkant `turquoise_inlaid_sandstone`, boven/onder `turquoise_inlaid_sandstone_top`; `salt_lamp` = blockstate op het **handgeschreven** model `src/main/resources/assets/corruptedadventure/models/block/salt_lamp.json`; itemmodellen, loot `dropSelf`/plaat, blok-tags `mineable/pickaxe`, `crystal_sound_blocks`, `stairs`/`slabs`/`walls`, `c:sandstone/blocks|slabs|stairs`).
- `datagen/lang/Golf2MaterialNames` (en/nl), aangeroepen door beide taalproviders.
- `datagen/recipe/Golf2MaterialRecipes` (vanuit `ModRecipeProvider`): `soul_crystal_block`, `turquoise_inlaid_sandstone` (+ familie via `generateRecipes`, steenzaag `…_stonecutting`), `salt_lamp`, `dehydration_coating_smithing_template_from_barrel_cactus`.
- `tools/textures/Golf2MaterialTextures.java`: placeholders `block/turquoise_inlaid_sandstone{,_top}`, `block/soul_crystal_block`, `block/salt_lamp` (stroom T vervangt ze).

## Beslissingen
_(geen nieuwe: alles binnen §5 of "Voorlopig")_

## Backlog
_(geen)_
