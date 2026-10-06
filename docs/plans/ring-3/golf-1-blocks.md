# Golf 1 — stroom B (blokken): 38, 38b, 40, 40b, 46, 51, 52

Scope: `blocks` — één commit per werkpakket, in deze volgorde, in worktree-branch `ring3/blocks-g1`.

**Doel:** alle blokken en items van ring 3 en Nether-ring 3 uit `contract.md` §3 bestaan na golf 1:
- met gedrag, loot, tags, blockstates/models, creative tab en lang (`Ring3BlockNames`);
- de models verwijzen naar de texturepaden van T (`contract.md` §7).

Daardoor kan 39c ze in de surface zetten en kunnen de features van golf 2 ze gebruiken.

Bron: `fase-1-wereld.md` Ring 3 "Unieke blokken" + "Uitwerking sparring ring 3"; Nether-3 "Blokken"; `contract.md` §3. Sjablonen: houtsets (`ModWoodTypes`, dood hout/palm/druipzwam), kloofsteen-set (trap/plaat/muur), `loose_sand` (`LooseSandTrigger`), zoutkristal/dauwstadia (`AmethystClusterBlock`, `crystalCluster`), `StalactiteBlock`, `SoakedNetherrackBlock`.

## Pakketten
- **38 — as, puimsteen, smeulsteen, verkoold hout.**
  - `ash_layer` gedraagt zich als `SnowLayerBlock`, maar smelt niet en heeft geen vanilla-sneeuwtags.
  - Smeulsteen: licht (voorstel 7), geen eigen schade (de hitte komt van C via `heat_sources`).
  - Verkoold hout is niet brandbaar (Nether-hout-patroon, zoals `DRIPSHROOM`), `BlockSetType`/`WoodType` `charred`, geluiden als `nether_wood` of `basalt` (agent kiest).
- **38b — smeulwoud en vuurflora.**
  - `ember_leaves`: bladeren die niet vervallen; geen bloklicht, alleen een emissive texture (V3 = A).
  - `fire_bloom` (+ pot) en `ash_sprouts`, met de grondregel "as, coarse dirt, `ash_layer`" in een eigen grondtag (`corruptedadventure:ash_plant_soil`, naam melden aan main).
  - `ember_berry_bush` (V7 = A): zeldzame eetbare struik, drop voorlopig vanilla `sweet_berries`; grond as/`coarse_dirt`/mos.
  - `ash_crust`: instort-gedrag via `LooseSandTrigger` (generaliseren naar een tag `corruptedadventure:collapsing_blocks` of een tweede blok in de trigger; implementatiedetail).
  - `smoldering_ash`: emissive scheuren, licht 0–3.
- **40 — ertsen.** `DropExperienceBlock`; XP, sterkte en tool-tier als ring 2 + één stap (vulkaniet `needs_iron_tool`, vuurrobijn `needs_iron_tool`, zwavel steen). Vuurrobijn-overlay emissive. Ruwe items als placeholders.
- **40b — vuurkristal.** Stadia als `AmethystClusterBlock` (licht 1/2/4/5, emissive), `fire_crystal_block` als amethistblok (geen groei).
- **46 — zwavelkorst, fumarole, kokende modder, geiser.**
  - Fumarole: `animateTick` met vanilla `CAMPFIRE_COSY_SMOKE`/`CLOUD` tot A `steam_puff` levert.
  - Modder: vertraagt als honing of poedersneeuw (keuze agent).
  - Geiser: block entity of scheduled tick; periodieke duw omhoog op entities erboven, server-side, met de interval in een constante.
  - Hittebronnen via de tag.
- **51 — Nether-3-blokken.** Koudlava-set (als basalt), `nether_ice`/`packed_nether_ice` (glad als ijs, smelt niet: eigen `IceBlock`-variant zonder smelten), `rime_netherrack` (als netherrack), rijpzwam-houtset (Nether-hout, niet brandbaar), `rimeshroom_cap` (wratblok), `ice_stalactite` (`StalactiteBlock`), `glowing_fissure` (licht, warmtebron).
- **52 — oerijs** `primordial_ice`: loot alleen met Silk Touch, anders niets.

## Tags (inhoud)
`heat_sources`, `warmth_sources`, `ring_3_stones`, `charred_logs`, `rimeshroom_stems`, `#logs`, `#planks`, `#wooden_*`, `#mineable/*`, `needs_*_tool`, `c:`-tags, `#small_flowers`, `goats_spawnable_on` (+ basalt, blackstone, `ash_block`), carver-replaceables voor as/puimsteen (grotten snijden erdoor).

## Afhankelijk van open vragen
- Geen meer (2026-10-06): V2 = A (zielvuur niet in `heat_sources`), V3 = A (geen licht), V7 = A (bes-struik, geiten-grondtag). Verkoold hout niet brandbaar = besluit van de gebruiker.

## Client/server-impact
Gedrag server-side (instorten, geiser-duw, Silk Touch). Particles via `animateTick` (client-level, geen client-class). Geen payloads.

## Tests
- **B**, inclusief `ModBlocksTest` (blockitems, creative-tab-volgorde) en nieuwe unit tests: ash-layer-vorm, oerijs-loot, geiser-interval, instort-trigger voor `ash_crust`.
- **D.**
- **S** (RCON):
  - `setblock` en `loot … mine` per blok;
  - instorten van `ash_crust` boven een holte;
  - geiser-duw met een armor stand of speler;
  - oerijs zonder en met Silk Touch;
  - stalactieten vallen.
- **W-checklist** in de eigen delta (creatief: tab, models, gloed in het donker).

## Bij afronding
Volgens `waves.md` §4: secties "CODEBASE-delta", "Beslissingen", "Backlog" onderaan dit bestand; rapport aan main.

## CODEBASE-delta

**Mappenstructuur / Belangrijke classes**
- `block/ModWoodTypes.java`: + `CHARRED_SET` (geregistreerd, `corruptedadventure:charred`) en `CHARRED` (niet geregistreerd); Nether-hout-sets via de helpers `netherWoodSet`/`netherWood` (crimson-geluiden; `DRIPSHROOM_*` gebruikt ze nu ook).
- `datagen/ModBlockStateProvider.java`: + `ring3()`, `ashLayer(full)` (lagen 1–7 = vanilla `snow_height2..14` als parent met de as-texture, 8 = het asblok-model), `glowingCube(block)` = twee lagen: basis `<naam>` + emissive cutout-overlay `<naam>_glow` (via `oreWithOverlay`).
- `datagen/ModBlockLootProvider.java`: + `ring3()`, `createAshLayerTable` (aantal = lagen).
- `datagen/lang/Ring3BlockNames.java`: namen (en + nl) van alle ring 3-/Nether-3-blokken en -items.
- Tests: `block/AshLayerTest.java` (vormen = vanilla-sneeuwlaag per laag, alleen `layers`, nooit random ticks, volle stapel blokkeert zicht); `ModWoodTypesTest` + charred; `ModBlocksTest` + ring 3-volgorde (direct na `dew_crystal_cluster`), smeulsteen licht 7 zonder `emissiveRendering`, verkoold hout niet `ignitedByLava`, asblok valt niet.

**Registries — Blocks (ring 3, volgorde = creative tab, direct na Nether-ring 2)**
| Registry-naam | Ring | Class | Opmerking |
|---|---|---|---|
| `ash_block` | 3 | `Block` | kopie `sand` (valt niet) + `COLOR_GRAY`; dropt zichzelf; `#mineable/shovel`, `#overworld_carver_replaceables`, `ring_3_stones`, `#goats_spawnable_on`; `cube_all` |
| `ash_layer` | 3 | `SnowLayerBlock` (vanilla) | eigen properties = vanilla-sneeuwlaag zonder `randomTicks` (smelt dus nooit), `COLOR_GRAY`, `SoundType.SAND`, shovel nodig voor drop; loot = `ash_layer` × lagen; `#replaceable`, `#inside_step_sound_blocks`, `#mineable/shovel`; **geen** sneeuwtag; smoort gras eronder (ook bij 1 laag); models `ash_layer_height2..14` (parent vanilla `snow_height*`), 8 lagen = `ash_block` |
| `pumice`, `polished_pumice`, `pumice_bricks` (+ `_stairs`, `_slab`, `_wall`; bakstenen: `pumice_brick_*`) | 3 | vanilla-classes | kopie tuff-familie (geluiden tuff/polished tuff/tuff bricks) + `COLOR_LIGHT_GRAY`; dropt zichzelf; `#mineable/pickaxe`, `#stairs`/`#slabs`/`#walls`; `pumice` ook `#overworld_carver_replaceables`, `ring_3_stones`, `#goats_spawnable_on` |
| `smoldering_stone` | 3 | `Block` | kopie `blackstone` + licht 7; geen schade; `heat_sources`, `warmth_sources`, `#mineable/pickaxe`, `#overworld_carver_replaceables`; model `glowingCube` (basis `smoldering_stone` + emissive `smoldering_stone_glow`) |
| `charred_log`, `charred_wood` (+ `stripped_*`), `charred_planks`, `_stairs`, `_slab`, `_fence`, `_fence_gate`, `_door`, `_trapdoor`, `_pressure_plate`, `_button` | 3 | `StrippableLogBlock` / vanilla-classes | kopie crimson-set + `COLOR_BLACK`; **niet brandbaar** (geen vuurkansen, niet `ignitedByLava`, items in `#non_flammable_wood` = geen brandstof); `BlockSetType` `corruptedadventure:charred` (nether-hout-geluiden), knop 30 ticks; `charred_logs` (in `#logs`, niet `#logs_that_burn`), `#planks`, `#wooden_*`, `c:stripped_*`, `c:fence_gates/wooden`; deur/valluik cutout |

**Registries — overige**
| `corruptedadventure:charred` | `BlockSetType` + niet-geregistreerde `WoodType`, nether-hout-geluiden | `ModWoodTypes` |

**Tags (inhoud, B)**
- `heat_sources`: `lava`, `magma_block`, `fire`, `campfire`, `smoldering_stone` (+ latere pakketten). `warmth_sources`: `lava`, `fire`, `soul_fire`, `campfire`, `soul_campfire`, `magma_block`, `smoldering_stone` (+ later).
- `ring_3_stones`: `basalt`, `blackstone`, `pumice`, `ash_block`.
- `minecraft:goats_spawnable_on` + `basalt`, `smooth_basalt`, `blackstone`, `ash_block`, `pumice` (op verzoek van W-biomes via main: de piek-bovenlaag van 39b en 39c; contract §3 noemde alleen basalt/blackstone/as).

**Valkuilen**
- Gloed in twee lagen (besluit gebruiker 2026-10-06): blokken met gloeiende delen krijgen `<naam>` + emissive cutout-overlay `<naam>_glow` (`glowingCube`), niet `emissiveRendering` (dat maakt het hele blok fullbright).
- Een vanilla-`SnowLayerBlock` smelt alleen via een random tick: properties zonder `randomTicks()` = nooit smelten, geen eigen class nodig.

## Beslissingen
- 2026-10-06 · blocks · Gloed van smeulsteen, smeulende as, smeulblad en gloeiende scheur in twee lagen (`<naam>` + emissive `<naam>_glow`) · besluit gebruiker via main.
- 2026-10-06 · blocks · Verkoold hout als Nether-hout (crimson-sjabloon, nether-hout-geluiden, geen brandstof) · gevolg van "niet brandbaar" (gebruiker).
- 2026-10-06 · blocks · `goats_spawnable_on` ook `smooth_basalt` en `pumice` · piek-bovenlaag van 39b/39c (W-biomes via main).
- 2026-10-06 · blocks · Baksteen-vormen heten `pumice_brick_*` / `cold_lava_brick_*` (vanilla-/ring 2-patroon) · akkoord main.
- 2026-10-06 · blocks · `ash_layer` = vanilla `SnowLayerBlock` zonder random ticks; dropt zichzelf × lagen (shovel); smoort gras eronder · implementatiekeuze.

## Backlog

## Voortgang
Voortgang: 2026-10-06 20:51 38 B groen (429 tests)
Voortgang: 2026-10-06 21:41 38 B+D groen (gloed als twee lagen _glow na afstemming)
Voortgang: 2026-10-06 21:44 38 S groen (loot, tags, aslaag-steun via RCON)
Voortgang: 2026-10-06 22:01 38 review verwerkt (AshLayerBlock geschrapt, inside_step_sound, gedeeld model, tagcommentaar); B/D/S opnieuw groen
