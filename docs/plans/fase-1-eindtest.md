# Fase 1 — eindtest (change 100, checklist)

**Scope: `worldgen`.** Eindtest van fase 1 (`fase-1-wereld.md` §H, change 100): met de testseed elke ring en elke grens
bezoeken (overworld én Nether), overgangsbiomes, buitenrand en world border, portaltest vanuit elke ring, geen
stronghold, The End onbereikbaar. Gevonden fouten klein herstellen (eigen commit) of in `docs/backlog.md`.

- **Stand:** deel 1 op `main` 6021c2d1 (ring 1, 4, 5 en alle Nether-ringen veranderen niet meer); deel 2: ring 3 en
  zone 3→4 op `main` 39c07ed3 (na de merge van G2 ring 3), ring 2 en zones 1→2, 2→3 op een lokale merge met
  `g2/ring2` 0670fd68 (om 17:49 stond G2 ring 2 nog niet op `main`; deadline 18:00).
- **Uitkomst (2026-10-25):** alles akkoord, behalve één niet-gereproduceerd verdwenen portaalframe in ring 3 oost (E3,
  backlog); geen herstel nodig. De perf-meting is overgeslagen op verzoek van de gebruiker (backlog).
- **Wereld:** vers, seed `20260930`, `corruptedadventure:corrupted_world`, standaardradii 1.500 / 3.500 / 6.000 /
  8.000 / 9.700, void-rand 10.200 (Nether ÷ 4). Worktree `P:/Projecten/Java/wt/fase1-end` (stroom `fase1-end`, n 12: n 8 was al van `g2r2-flats`, de eerste start faalde op `BindException`).
- **Dedicated server + 2 clients** (`server.sh start <level> --fresh --client both`, `client.sh start 1` / `start 2`,
  spelers `Dev` en `Dev2`): beide ingelogd, een paar keer samen geteleporteerd, 0 ERROR in de serverlog.
- **Foto's:** `walk.py` (spectator, HUD uit, ringweer helder, middag); per spot een foto in `build/test/f1end/`.
- **Portaltest:** `tools/test/py/portal.py`: per ring een obsidiaanframe + vuur via RCON op de landingsplek van
  `/ca tpring <n>`, `Dev` (creative) stapt erdoor, `/ca ring` op de aankomstplek in de Nether moet dezelfde ring geven
  (`coordinate_scale` 4), en de terugweg moet bij het eigen portaal uitkomen.
- **Vervallen:** chunk-generatietijd per ring meten (perf) — **overgeslagen op verzoek van de gebruiker, in te halen.**

Status per punt: **akkoord**, **fout** (met wat er mis is) of **overgeslagen** (met reden). Foto-paden relatief aan de
worktree.

## A. Spots per ring (overworld)
| # | Spot | Verwacht | Status | Foto |
|---|---|---|---|---|
| A1 | ring 1, midden (`/ca tpring 1`, 750 63 0) | vanilla-klimaat, ring 1 | **akkoord** (`stony_shore` aan zee, ring 1) | `build/test/f1end/shots1/r1.png` |
| A2 | ring 2, midden (2500 80 0 en 0 73 −2500) | woestijnbiome ring 2 (canyonlands / hoodoo_fields / salt_flats / dune_sea / oasis_flats) | **akkoord** (G2 ring 2 = `g2/ring2` 0670fd68, wereld `f1end-r2`: `dune_sea` oost en noord, duinen met rotsblokken en een droge poel) | `build/test/f1end/shots2/r2.png`, `r2n.png` |
| A3 | ring 3, midden (4750 92 0 en 0 97 −4750) | vulkanisch biome ring 3 | **akkoord** (na G2 ring 3, wereld `f1end-r3`: `ember_woods` oost en noord, verbrande bomen op as, oranje lucht) | `build/test/f1end/shots3/r3.png`, `r3n.png` |
| A4 | ring 4, midden (7000 63 0) | moeras/waterbiome ring 4 | **akkoord** (`lotus_shallows` onder een eiland met hangende wortels) | `build/test/f1end/shots1/r4.png` |
| A5 | ring 5, midden (8850 63 0) | corrupted biome ring 5 | **akkoord** (`corrupted_wastes`, mist, dode bomen) | `build/test/f1end/shots1/r5.png` |

## B. Overgangsbiomes en grenzen (overworld, `/ca tpboundary <n>`)
| # | Grens | Overgangsbiome | Status | Foto |
|---|---|---|---|---|
| B1 | 1→2 (1472 65 0) | `withered_expanse` | **akkoord** (`withered_expanse` aan het water, de mesa's van ring 2 erachter) | `build/test/f1end/shots2/b12.png` |
| B2 | 2→3 (3458 125 0 en 0 85 3540) | `dry_riverbeds` | **akkoord** (`dry_riverbeds`: rode grond met as-plekken en dode stammen, de overgang naar ring 3) | `build/test/f1end/shots2/b23.png`, `b23s.png` |
| B3 | 3→4 (6044 88 0 en 0 80 6036; `steam_fens` op 5930 0) | `steam_fens` (+ `verdant_falls`-afdaling) | **akkoord**: de grens (gewicht 0,50) ligt in `verdant_falls` (groen, de afdaling naar ring 4); binnen gewicht 0,234 `steam_fens` met modder, zwavel en stoom | `build/test/f1end/shots3/b34.png`, `b34s.png`, `b34f.png` |
| B4 | 4→5 (8014 72 0) | `sundered_reach` (breukzone) | **akkoord** (`sundered_reach`, overgang 4 → 5 gewicht 0,50; gebroken terrasblokken) | `build/test/f1end/shots1/b45.png` |

## C. Nether: spots per ring en grenzen (`/ca tpring <n> nether`, `/ca tpboundary <n> nether`)
| # | Spot | Verwacht | Status | Foto |
|---|---|---|---|---|
| C1 | Nether-1, midden (187 37 0) | vanilla Nether-klimaat | **akkoord** (`nether_wastes`, lavazee; het portaal van E1 staat ernaast) | `build/test/f1end/shots1/n1.png` |
| C2 | Nether-2, midden (625 76 0) | `dripshroom_forest` / `spore_caves` | **akkoord** (`dripshroom_forest`) | `build/test/f1end/shots1/n2.png` |
| C3 | Nether-3, midden (1187 51 0) | `frozen_lava_sea` / `rime_forest` / `glacier_rifts` | **akkoord** (`rime_forest`) | `build/test/f1end/shots1/n3.png` |
| C4 | Nether-4, midden (1750 85 0) | `bone_dunes` / `crystal_spires` / `geode_hollows` | **akkoord** (`geode_hollows`) | `build/test/f1end/shots1/n4.png` |
| C5 | Nether-5, midden (2212 32 0) | `white_halls` / `hollow_cathedral` | **akkoord** (`white_halls`, witte zuilen) | `build/test/f1end/shots1/n5.png` |
| C6 | grens 1→2 (Nether, 368 71 0) | ringgrens zonder lavalek | **akkoord** (overgang 1 → 2, `nether_wastes`; de landingsplek is een kleine netherrack-holte, geen lek) | `build/test/f1end/shots1/nb12.png`, `nb12-light.png` |
| C7 | grens 2→3 (Nether, 864 39 0) | idem | **akkoord** (overgang 2 → 3, `spore_caves`) | `build/test/f1end/shots1/nb23.png` |
| C8 | grens 3→4 (Nether, 1511 41 0) | idem | **akkoord** (overgang 3 → 4, `frozen_lava_sea`) | `build/test/f1end/shots1/nb34.png` |
| C9 | grens 4→5 (Nether, 2003 39 0) | idem | **akkoord** (overgang 4 → 5, `bone_dunes` → witte zuilen) | `build/test/f1end/shots1/nb45.png` |

## D. Buitenrand, world border, stronghold, The End
| # | Punt | Verwacht | Status |
|---|---|---|---|
| D1 | buitenrand ring 5 → void (`/ca tpboundary 5`, en r ≈ 10.100) | void-rand, `void_edge` | **akkoord**: grens 5 op 9696 (geen vaste grond, `shattered_expanse`, losse brokken boven de void); 10.050 = void (6). Foto's `build/test/f1end/shots1/b5v.png`, `edge.png`, `border.png` |
| D2 | world border overworld (`/ca border`) | grootte 20.400 (radius 10.200), midden 0,0 | **akkoord** (`size 20400 (radius 10200), center 0.0, 0.0`) |
| D3 | world border Nether (`execute in minecraft:the_nether run ca border`) | grootte 5.100 (radius 2.550) | **akkoord** (`size 5100 (radius 2550)`) |
| D4 | `/locate structure minecraft:stronghold` (overworld) | niets gevonden | **akkoord** (`Could not find a structure of type "minecraft:stronghold" nearby`; ook in de Nether, en `end_city`) |
| D5 | The End onbereikbaar | geen stronghold dus geen eindportaal; `minecraft:the_end` alleen via commando | **akkoord** (de preset houdt `minecraft:the_end` als dimensie, maar zonder stronghold bestaat er geen eindportaal) |

## E. Portaltest (overworld ↔ Nether, per ring)
| # | Ring | Aankomst Nether (`/ca ring`) | Terugweg bij eigen portaal | Status |
|---|---|---|---|---|
| E1 | ring 1 | Nether-1 | ja: frame 752 63 3 → Nether 187,5 37 1,5 (ring 1) → terug op 0 blokken van het frame | **akkoord**; het vuur ontstak het frame hier niet (landingsplek op zeeniveau naast water), dus portaalblokken gezet |
| E2 | ring 2 | Nether-2 | ja: frame 2503 80 0 (vuur) → Nether 622,5 79 −3,5 (ring 2) → terug op 0 blokken, frame staat nog | **akkoord** |
| E3 | ring 3 | Nether-3 | zuid: frame 3 99 4750 (vuur) → Nether 13,5 41 1178,5 (ring 3) → terug op 0 blokken | **akkoord** (zuid). Oost (frame 4752 89 23, in het bladerdak van `ember_woods`): aankomst Nether-3 klopte, maar het frame was bij terugkomst verdwenen en het spel maakte een nieuw portaal op 57 blokken (`build/test/f1end/shots3/p3a.png`, `p3b.png`); niet gereproduceerd (de herhaling oost hield het frame, maar koos het nieuwe portaal als dichtste). In de backlog |
| E4 | ring 4 | Nether-4 | ja: frame 7002 69 34 (vuur) → Nether 1750,5 86 4,5 (ring 4) → terug op 0 blokken | **akkoord** |
| E5 | ring 5 | Nether-5 | ja: frame 8852 65 44 (vuur) → Nether 2214,5 32 11,5 (ring 5) → terug op 0 blokken | **akkoord** |

## F. Multiplayer en log
| # | Punt | Status |
|---|---|---|
| F1 | dedicated server start zonder client-class-fouten | **akkoord** (deel 1, wereld `f1end`) |
| F2 | `Dev` en `Dev2` tegelijk ingelogd, samen geteleporteerd (ring 1, 4, 5, Nether) | **akkoord**: 5 keer samen (ring 4, ring 5, Nether-4, Nether-5, ring 1), beide in dezelfde dimensie; Dev2 ziet Dev in ring 5 (`build/test/f1end/mp-dev2.png`) |
| F3 | 0 ERROR in de serverlog (`build/test/server-<level>.log`) | **akkoord** deel 1: 0 ERROR (`build/test/f1end/server-deel1.log`); ring 3-run `f1end-r3` ook 0 (`server-deel2-r3.log`) |
| F4 | idem na de merges (deel 2) | **akkoord**: ring 3-run `f1end-r3` (server + Dev) en ring 2-run `f1end-r2` (server + Dev + Dev2, 4 keer samen geteleporteerd: ring 2, ring 3, Nether-2, zone 1→2), beide 0 ERROR (`build/test/f1end/server-deel2-r3.log`, `server-deel2-r2.log`) |

## G. Vervallen / in te halen
- Chunk-generatietijd per ring (perf): overgeslagen op verzoek van de gebruiker (2026-10-25), in te halen.
