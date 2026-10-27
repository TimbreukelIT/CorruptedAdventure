# Fase 2 golf 2 — plan (ring 2 + Nether-1)

Bron van waarheid: `docs/design/fase-2-items.md` §5 (besluiten 2026-10-27). Basis: main `8bf883ed`.
Regels voor agents: `build/agent-rules-fase2.md` (lokaal, niet in git).

## Stromen
| Stroom | Branch / worktree | n | Categorie(ën) | Inhoud |
|---|---|---|---|---|
| S | `f2g2/sunstone` · `P:/Projecten/Java/wt/f2g2-sunstone` | 61 | `items` | §5.1: stats in `GearTier`/`ModArmorMaterials` + `GearTierTest`; 9 items `sunstone_{sword,axe,pickaxe,shovel,hoe,helmet,chestplate,leggings,boots}`; smithing-transform diamant → zonnesteen (9 recepten); template-recept (6 `soul_crystal` + 2 diamant + 1 scorched sandstone → 1); `TierGatingGameTests` met de echte zonnesteen-pickaxe op de ring 3-ertsen |
| V | `f2g2/consumables` · `P:/Projecten/Java/wt/f2g2-consumables` | 62 | `conditions` (raamwerk), `items` (items) | §5.3/§5.4: raamwerk (directe aftrek + tijdelijk rate-effect per conditie, per speler, server-side, config); `canteen` (4 slokken, vullen bij water/cauldron/`dew_source`), `cactus_juice`, `salted_jerky` (+8 uitdroging bij eten) en hun recepten |
| M | `f2g2/materials` · `P:/Projecten/Java/wt/f2g2-materials` | 63 | `blocks` (blokken), `items` (recepten) | §5.2–§5.4: `soul_crystal_block`; `turquoise_inlaid_sandstone` + `_stairs`/`_slab`/`_wall`; `salt_lamp` (licht 12); recepten daarvan; het recept van het eerste `dehydration_coating_smithing_template` (4 `turquoise` + 4 scorched sandstone + 1 barrel cactus → 1) |
| T | `f2g2/textures` · `P:/Projecten/Java/wt/f2g2-textures` | 64 | `textures` | §5.6: alleen deze stroom gebruikt Blockbench. Eerst een voorbeeld (`sunstone_ingot`, `sunstone_sword`, `sunstone_chestplate` + `sunstone_layer_1`) voor het akkoord van de gebruiker, daarna de rest |

## Afspraken tussen stromen
- **Textures:** S, V en M leggen een **placeholder-PNG** op het definitieve pad (`assets/corruptedadventure/textures/item/<naam>.png`, `…/block/<naam>.png`, harnas `…/textures/models/armor/sunstone_layer_{1,2}.png`). T overschrijft die. Bij een merge wint de PNG van T.
- **Hand-gemodelleerde blokken:** het blokmodel van `salt_lamp` staat handgeschreven in `src/main/resources/assets/corruptedadventure/models/block/salt_lamp.json` (M: placeholder `cube_all`; T: het Blockbench-model). Datagen maakt de blockstate en het itemmodel, maar **niet** dat blokmodel.
- **Veldfles-model:** V maakt het itemmodel met overrides op een item-property voor de slokken (de client registreert de property in `client/`). Texturenamen: `canteen_empty`, `canteen` (gevuld; eventueel stappen `canteen_1..4` als V dat zo bouwt; V noemt de namen in zijn plan, T volgt).
- **Datagen:** elke stroom eigen helperklassen (lang, modellen, recepten, tags), zoals in golf 1. In gedeelde providers alleen een aanroep toevoegen.
- **Registry:** nieuwe items achteraan in de ring 2-sectie (S, V, M) van `ModItems`/`ModBlocks`, of in de Nether-1-sectie voor `soul_crystal_block` (§4.1: Nether-ring 1 hoort in tab 1). Main lost botsingen bij de merge op.
- **Recepten** die in §5 niet tot op het vak vastliggen (vorm van de veldfles, ingelegd zandsteen, zoutlamp, zielkristalblok, cactussap, jerky): de stroom kiest een vanilla-achtige vorm en zet hem onder **"Voorlopig (bevestigen bij de teststop)"** in zijn plan.

## Merge-volgorde
S, M, V (elk na een review) → T als laatste (PNG's van T winnen) → INT.

## INT
Dedicated server + 2 clients; recepten en mijnbaarheid via een testscript; `git diff --exit-code src/generated` na runData; CODEBASE/backlog/design bijwerken uit de delta's; checklist teststop golf 1 + 2.
