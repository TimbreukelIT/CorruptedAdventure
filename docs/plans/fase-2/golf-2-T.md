# Fase 2 golf 2 — stroom T (textures en modellen)

Branch `f2g2/textures`, worktree `P:/Projecten/Java/wt/f2g2-textures`, n = 64. Categorie: `textures`.
Bron: `docs/design/fase-2-items.md` §5.6 (+ besluiten `73c115c5`: `cooled`-effect, veldfles-weergave).

## Wat
1. Voorbeeld voor het akkoord: `sunstone_ingot`, `sunstone_sword`, `sunstone_chestplate`, `sunstone_layer_1`.
2. Na akkoord: rest van de set (bijl, houweel, schep, schoffel, helm, beenstukken, laarzen, `sunstone_layer_2`),
   `sunstone_upgrade_smithing_template`, `canteen_empty`/`canteen`, effecticoon `cooled`, `cactus_juice`,
   `salted_jerky`, `soul_crystal_block`, `turquoise_inlaid_sandstone` (+ vormen), blokmodel `salt_lamp`.

## Palet zonnesteen (barnsteen-goud)
| teken | kleur | rol |
|---|---|---|
| O | `#3D1606` | omlijning (donker koper) |
| 1 | `#6B2A0C` | diepe schaduw |
| 2 | `#99441A` | koper |
| 3 | `#C46A1E` | donker amber |
| 4 | `#E08E26` | amber (basis) |
| 5 | `#F2B13C` | goud |
| 6 | `#FFD25E` | zongeel highlight |
| 7 | `#FFEDB0` | glans |
Greep zwaard: `#24140C` `#45291A` `#684226` `#8A5E36`. Roder/oranjer dan vanilla-goud (`#FDFF76`/`#EAEE57`), aansluitend
op het erts (`Ring2Textures.SUNSTONE`), geen emissive.

## Voortgang
- 2026-10-27 — setup T 64; vanilla-referenties uitgepakt; Blockbench-MCP hangt na de eerste `risky_eval` (alle
  aanroepen time-out, gemeld aan main). Voorbeeld gemaakt met een kladscript (pixelrasters, Python) en een eigen
  box-renderer (speler + harnas-layer); vergelijkingsblad `build/t-preview/sunstone-preview.png`. Wacht op akkoord.
- 2026-10-27 — akkoord gebruiker (via main); bron als Java-scripts (keuze gebruiker). Blockbench blijft geblokkeerd
  (bestandstoestemming van de plugin, beslissing gebruiker): gewerkt zonder Blockbench, eindcontrole met de eigen
  renderer (`build/t-preview/golf2-T-final.png`). Main `6f4316f6` (stroom M) ingevoegd.
- 2026-10-27 — `tools/textures/Golf2SunstoneTextures.java` (14 PNG's) en `tools/textures/Golf2Ring2Textures.java`
  (11 PNG's) gemaakt; de 4 voorbeeld-PNG's zijn pixel-identiek gebleven.

## Voorlopig (bevestigen bij de teststop)
- Texturenamen van V (`canteen_empty`, `canteen`, `cactus_juice`, `salted_jerky`, `mob_effect/cooled`) aangenomen
  volgens golf-2.md/main; V bevestigt.
- Verschil lege/gevulde veldfles: leeg = bleker, droog leer, open hals, kurk aan een touwtje; vol = kurk erin.
- `cooled`-icoon: koele druppel met een rijpschittering, 18 x 18 zoals de vanilla-effecticonen.

## W-checklist voor INT
- In-game: zonnesteen-items in de hand/inventaris en het harnas op een speler (layer 1 + 2) bekijken naast goud/netherite.
- Smithing-scherm: het zonnesteen-template naast de andere drie (golf 1-tablet).
- Veldfles leeg/vol (model-overrides van V), cactussap, jerky, effecticoon `cooled` rechtsboven en in de inventaris.
- Zielkristalblok, ingelegd zandsteen (blok, trap, plaat, muur: naden), zoutlamp geplaatst (zijkant/boven/onder).

## CODEBASE-delta
- Nieuw `tools/textures/Golf2SunstoneTextures.java` (fase 2 golf 2, stroom T; single-file Java, alleen JDK, geen random):
  pixelrasters met eigen palet per texture; `java tools/textures/Golf2SunstoneTextures.java` vanuit de repo-root;
  `item/sunstone_{ingot,sword,axe,pickaxe,shovel,hoe,helmet,chestplate,leggings,boots,upgrade_smithing_template}`,
  `models/armor/sunstone_layer_{1,2}` (64 x 32); controles maat/alfa/palet; contactblad `build/texture-preview/golf2sunstonetextures.png`.
  Palet barnsteen-goud (zie boven).
- Nieuw `tools/textures/Golf2Ring2Textures.java` (zelfde opzet): `item/canteen_empty`, `item/canteen`, `item/cactus_juice`,
  `item/salted_jerky`, `mob_effect/cooled` (18 x 18), `block/soul_crystal_block`, `block/turquoise_inlaid_sandstone{,_top}`
  (raster over `scorched_sandstone{,_top}` als basis; '.' houdt de basispixel), `block/salt_lamp{,_top,_bottom}`.
- `models/block/salt_lamp.json` (handgeschreven): `cube_bottom_top` met side/top/bottom.
- Weg: `tools/textures/Golf2MaterialTextures.java` (placeholders van M, alle vier vervangen). `Golf1MaterialTextures`
  schrijft `item/sunstone_ingot` niet meer, `Golf1TemplateTextures` het zonnesteen-template niet meer (beide nu uit
  `Golf2SunstoneTextures`).

## Beslissingen
- 2026-10-27, textures: zonnesteen-voorbeeld (palet barnsteen-goud, ingot als gegoten staaf, zwaard met zonnesteen in de
  kruisgreep, borstplaat/harnas met zonne-embleem) goedgekeurd door de gebruiker.
- 2026-10-27, textures: pixelbron van nieuwe golf 2-textures = Java-scripts in `tools/textures/` (gebruiker); Blockbench
  niet gebruikt omdat de plugin op een bestandstoestemming wachtte.
- 2026-10-27, textures: zoutlamp = volle kubus (optie A, gebruiker): `cube_bottom_top`, warm zout met gloed van binnen en
  een houten voetrand in de zijtexture, hout aan de onderkant. Geen code-wijziging.

## Backlog
- [textures] De drie andere upgrade-templates (vulkaniet, getijzilver, nulliet) zijn nog golf 1-placeholders; het
  zonnesteen-template heeft nu de definitieve stijl (bij hun golf meenemen).
