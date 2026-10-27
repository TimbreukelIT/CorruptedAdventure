# Fase 2 golf 3 + 4 — plan (ring 3 + Nether-2, ring 4 + Nether-3)

Bron van waarheid: `docs/design/fase-2-items.md` §6 (besluiten 2026-10-27). Basis: main `18b15c90` (golf 2 compleet).
Regels: `build/agent-rules-fase2.md` (lokaal) — met de poorten en de basis hieronder. Werkwijze en afspraken zoals in
`golf-2.md` ("Afspraken tussen stromen"): placeholder-PNG's op de definitieve paden, T wint; eigen datagen-helpers per
stroom; nieuwe items achteraan in hun ringsectie; "Voorlopig" voor vrije receptvormen.

## Stromen
| Stroom | Branch / worktree | n | Categorie(ën) | Inhoud |
|---|---|---|---|---|
| G | `f2g34/gear` · `wt/f2g34-gear` | 66 | `items` | §6.1 de curve (alle 4 tiers) + `GearTierTest`; `volcanite_*` en `tidesilver_*` (2 × 9), smithing zonnesteen → vulkaniet → getijdezilver (18), de twee template-recepten; `TierGatingGameTests` met de echte vulkaniet- en getijdezilver-pickaxe. Hergebruik `GearItems`/`GearRecipes`/`GearNames`/`GearItemTags` van S |
| C | `f2g34/consumables` · `wt/f2g34-consumables` | 67 | `conditions`, `items` | §6.3 + §6.6: `cooled` ook tegen hitte; effecten `cleansed` (gezuiverd, gif) en `warmed` (verwarmd, kou); `dew_draught`, `purified_water`, `hand_warmer` (4 ladingen, bijvullen met vuurkristal). Hergebruik het raamwerk van V (`ConditionDose`, `ConsumableDoses`, `DoseDrinkItem`, …) |
| A | `f2g34/arrows` · `wt/f2g34-arrows` | 68 | `items` | §6.4 + §6.7: `fire_arrow` en `poison_arrow` (item, entity, renderer in `client/`, dispenser, recepten) |
| R3 | `f2g34/ring3` · `wt/f2g34-ring3` | 69 | `blocks`, `items` | §6.3 hitte-template-recept; §6.4: buskruit, zwavelbrandstof + `sulfur_block`, `fire_ruby_inlaid_pumice` (+ vormen), `ember_berries` (plant de struik, 5 s vuurbestendigheid), `sulfur_lamp`, `pumice_raft`, `dew_crystal_block`, `dripshroom_stew` |
| R4 | `f2g34/ring4` · `wt/f2g34-ring4` | 61 | `blocks`, `items` | §6.6 gif- en kou-template-recepten; §6.7: `jade_inlaid_tidestone`, `aquamarine_inlaid_lagoon_sandstone` (+ vormen), veen als brandstof |
| T | `f2g34/textures` · `wt/f2g34-textures` | 62 | `textures` | alle nieuwe textures (namen hieronder); eerst een voorbeeld vulkaniet + getijdezilver (ingot, zwaard, borstplaat, layer 1) voor het akkoord |

## Texturenamen (vast, zodat T meteen kan beginnen)
- G: `item/{volcanite,tidesilver}_{sword,axe,pickaxe,shovel,hoe,helmet,chestplate,leggings,boots}`, `models/armor/{volcanite,tidesilver}_layer_{1,2}`; T maakt ook `item/{volcanite,tidesilver}_ingot` en de upgrade-templates van vulkaniet, getijdezilver en nulliet in de stijl van het zonnesteen-template.
- C: `item/dew_draught`, `item/purified_water`, `item/hand_warmer` (+ `item/hand_warmer_empty` als C een lege staat heeft), `mob_effect/cleansed`, `mob_effect/warmed`.
- A: `item/fire_arrow`, `item/poison_arrow`, `entity/projectiles/fire_arrow`, `entity/projectiles/poison_arrow`.
- R3: `block/sulfur_block`, `block/fire_ruby_inlaid_pumice` + `_top`, `block/pumice_raft` (+ `item/pumice_raft`), `block/sulfur_lamp`, `block/dew_crystal_block`, `item/ember_berries`, `item/dripshroom_stew`.
- R4: `block/jade_inlaid_tidestone` + `_top`, `block/aquamarine_inlaid_lagoon_sandstone` + `_top`.
Wijkt een stroom af (bv. een extra staat), dan meldt hij dat aan main; main geeft het door aan T.

## Merge-volgorde
Elke stroom na review; main merget in volgorde van binnenkomst, T als laatste. Daarna INT (golf 3 + 4) en de
gebundelde teststop-checklist golf 1–4 (`docs/plans/fase-2/teststop-golf-1-4.md`).
