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
