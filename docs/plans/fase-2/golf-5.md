# Fase 2 golf 5 — plan (ring 5 + Nether-4/5)

Bron van waarheid: `docs/design/fase-2-items.md` §7 (besluiten 2026-10-28). Basis: main na de commit van dit plan.
Regels: `P:/Projecten/Java/The Corrupted Adventure/build/agent-rules-fase2.md` (lokaal, ook de "Aanvulling golf 5").
Werkwijze zoals in golf 3 + 4 (`golf-34.md`): placeholder-PNG's op de definitieve paden, T wint; eigen datagen-helpers
per stroom; nieuwe items achteraan in hun ringsectie (Nether-4 in tab 4, Nether-5 en ring 5 in tab 5); "Voorlopig" voor
vrije receptvormen.

## Stromen
| Stroom | Branch / worktree | n | Categorie(ën) | Inhoud |
|---|---|---|---|---|
| H | `f2g5/skyglass` · `wt/f2g5-skyglass` | 69 | `blocks`, `items` | **Pakket 1 eerst (vroege merge, de anderen hebben het nodig):** `sky_sand` (valt, 4 `bone_sand` + 1 `purity_crystal_shard` → 4), `sky_glass` (smelten, ook als glas in de tags waar vanilla-glas zit), `sky_glass_pane` (6 → 16). Commit, meld "H pakket 1 klaar" aan main. Pakket 2: `glitch_shard_block` (9 ↔ 1), void-coating-materiaal → hemelglas (`trim_material` + atlas; witte kern blijft voor stilte), void- en stilte-template-recepten (§7.3) |
| G | `f2g5/gear` · `wt/f2g5-gear` | 66 | `items` | §7.1: `nullite_*` (9), smithing getijdezilver → nulliet (9), template-recept (`sky_glass` uit H pakket 1: merge main zodra main het meldt; tot dan alleen de code), `GearTierTest`/`TierGatingGameTests` bijwerken. Hergebruik `GearItems`/`GearRecipes`/`GearNames`/`GearItemTags` |
| C | `f2g5/consumables` · `wt/f2g5-consumables` | 67 | `conditions`, `items` | §7.3: effecten `anchored` (void ×0,5) en `resounding` (stilte ×0,5); `sky_light_draught` (Hemellicht, eenmalig, fles terug); `echo_shell` (Galmschelp: duurzaamheid 16, cooldown 45 s, geluid hoorbaar voor spelers in de buurt — server speelt het af; voorlopig een vanilla-geluid of eigen audio via `tools/sounds/`, kies onder "Voorlopig"). Hergebruik het raamwerk van V/C (`ConditionDose`, `ConsumableDoses`, `DoseDrinkItem`, …) |
| A | `f2g5/arrows` · `wt/f2g5-arrows` | 68 | `items` | §7.4: `glitch_arrow` (item, entity, renderer in `client/`, dispenser, recept, GameTest). Teleport op de server, alleen naar een veilige plek (hergebruik waar het kan de veilige-plek-check van de void-teleport), nooit in of richting de void; spoor `glitch_pixel`. Een geraakte speler: voorlopig niet teleporteren (onder "Voorlopig") |
| L | `f2g5/lens` · `wt/f2g5-lens` | 61 | `items` (+ client-rendering) | §7.2: `sky_glass_lens` (verrekijkergedrag: zoom, scope-overlay, geluid), recept na H pakket 1. Server: zolang een speler kijkt elke 20 ticks een scan straal 32 (void-gaten onder de grond, glitchblokken, ontbrekende chunks; begrensd en goedkoop, geen chunk-loads), payload alleen naar die speler (`CustomPacketPayload` + `StreamCodec`; protocolversie ophogen). Client (`client/`): omlijning via een render-event, en zolang je kijkt de glitch-fog en de schermvervorming van ring 5 dempen. Unit test voor de scan, `ClientIsolationTest` groen |
| T | `f2g5/textures` · `wt/f2g5-textures` | 62 | `textures` | alle nieuwe textures (namen hieronder); eerst een voorbeeld nulliet (ingot, zwaard, borstplaat, layer 1) en hemelglas (blok + ruit) voor het akkoord via main |

## Texturenamen (vast, zodat T meteen kan beginnen)
- G: `item/nullite_{sword,axe,pickaxe,shovel,hoe,helmet,chestplate,leggings,boots}`, `models/armor/nullite_layer_{1,2}`;
  T vernieuwt ook `item/nullite_ingot`, `item/raw_nullite`, `block/nullite_block`, `block/raw_nullite_block` in het
  nulliet-palet (de erts-textures alleen als het palet botst).
- H: `block/sky_sand`, `block/sky_glass`, `block/sky_glass_pane_top`, `block/glitch_shard_block` (flikkerend: animatie
  met `.mcmeta`); het void-coating-palet voor hemelglas (`trims/color_palettes/sky_glass` of het pad dat H kiest — H
  meldt het aan main).
- C: `item/sky_light_draught`, `item/echo_shell`, `mob_effect/anchored`, `mob_effect/resounding`.
- A: `item/glitch_arrow`, `entity/projectiles/glitch_arrow`.
- L: `item/sky_glass_lens`, `misc/sky_glass_lens_scope` (scope-overlay zoals `misc/spyglass_scope`).
Wijkt een stroom af (bv. een extra staat), dan meldt hij dat aan main; main geeft het door aan T.

## Afhankelijkheden
- `sky_glass` (H pakket 1) is nodig voor de recepten van G, L en H pakket 2. H levert pakket 1 binnen ~30 min; main
  merget het direct naar main en meldt het aan G en L, die dan `git merge main` doen.
- Ga niet zelf aan andermans registries zitten; heb je iets van een andere stroom nodig, meld het aan main.

## Merge-volgorde
H pakket 1 meteen; daarna elke stroom na review in volgorde van binnenkomst, T als laatste. Daarna INT (golf 5, n 65):
build, runData, GameTests, dedicated server + 2 clients, en de eindtest-checklist fase 2 (deel golf 5 erbij in
`docs/plans/fase-2/teststop-golf-1-4.md`, hernoemd naar `eindtest-fase-2.md`), CODEBASE-delta's en backlog infolden.
