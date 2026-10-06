# Ring 3 — contract (namen, sleutels, eigenaarschap)

Scope: ring 3 + Nether-ring 3 (bouwplan blok E, `fase-1-wereld.md` §3). Opgesteld 2026-10-06 uit de sparring (`docs/plans/ring-3-sparring.md`, antwoorden 1–22) en de werkwijze in `waves.md`.

**Regels**
- Alles in dit bestand is **vast** zodra golf 0 gecommit is. Een stroom die iets anders of extra nodig heeft, stuurt de vraag naar main (SendMessage). Main beslist, past dit bestand aan en meldt het aan alle stromen. Nooit zelf in andermans bestand werken.
- Namen zijn registry-namen onder `corruptedadventure:` (Engels, snake_case, ring-thema herkenbaar). "(voorlopig)" = mag in de eigen stroom nog veranderen tot de golf-1-commit; meld de definitieve naam in je rapport.
- Wat een **open vraag** raakt (§9), is gemarkeerd met *[V#]*. Tot het antwoord er is, bouwt geen stroom dat deel.

---

## 1. Ringgrenzen (config, pakket 37c)

| Sleutel (COMMON-config `corruptedadventure-common.toml`) | Oud | Nieuw |
|---|---|---|
| `ring1OuterRadius` | 1500 | 1500 |
| `ring2OuterRadius` | 3500 | 3500 |
| `ring3OuterRadius` | 5500 | **6000** |
| `ring4OuterRadius` | 7500 | **7800** |
| `ring5OuterRadius` | 9500 | 9500 |
| `voidOuterRadius`, `boundaryNoise`, `transitionWidth` | 10000, 96, 128 | ongewijzigd |

- Nether ÷4: ring 3 = 875–1.500, ring 4 = 1.500–1.950, ring 5 = 1.950–2.375.
- `/ca tpring` gebruikt de snapshot: midden ring 3 = 4.750 (Nether 1.187), ring 4 = 6.900, ring 5 = 8.650.
- Geldt alleen voor **nieuwe** werelden. Alle ring 3-tests vanaf golf 1 gebruiken een verse wereld (seed `20260930`, preset `corrupted_world`).

## 2. Biomes

| Sleutel | Dimensie | Rol | Eigenaar |
|---|---|---|---|
| `ashen_wastes` | overworld | kern: asvlakte, verkoolde bomen, aslagen, afgietsels | W-biomes |
| `volcanic_peaks` | overworld | kern: kegels, caldera's, lavastromen, heetste plek, geiten | W-biomes |
| `ember_woods` | overworld | smeulwoud (gloeiende kronen, vuurflora) | W-biomes |
| `basalt_flows` | overworld | gestolde stromen met gloeiende scheuren, lavarivieren | W-biomes |
| `sulfur_fields` | overworld | zwavelkorst, fumarolen, giftige damp | W-biomes |
| `dried_seabed` | overworld | lege zeekom, dode koraalbanken | W-biomes |
| `titan_fields` (voorlopig) | overworld | fantasy: reuzenskeletten, zielvuur, titanen-rotsen | W-biomes |
| `dormant_caldera` *[V8]* | overworld | slapende krater (oase): koel, groen | W-biomes |
| `steam_fens` | overworld | overgang 3→4 (`transitions.ring_3`), golf 2 | W-biomes |
| `frozen_lava_sea`, `rime_forest`, `glacier_rifts` | Nether | Nether-ring 3 | W-nether |

**Biome-tags** (namen in `ModTags` via 37c; inhoud door de eigenaar van de biomes, via datagen):
- `corruptedadventure:is_ring_3`: alle ring 3-overworld-biomes. Lege tag tot 39b; dat is geen fout. `steam_fens` erin zoals `dry_riverbeds` in `is_ring_2`.
- `corruptedadventure:is_nether_ring_3`.
- `corruptedadventure:heat_peaks`: `volcanic_peaks` (hitte ×2, lees C).
- `corruptedadventure:heat_refuges`: de slapende krater (hitte ↓ 2,0/s, lees C) *[V8]*.
- `c:is_hot/overworld`, `c:is_dry/overworld` (ring 3) en `c:is_cold/nether` (Nether-3). Geen `minecraft:has_structure/*` (fase 3).

## 3. Blokken en items (eigenaar B-blokken)

Alle blokken krijgen een blockitem met dezelfde naam, tenzij vermeld. Volgorde = creative-tab-volgorde: ring 3 direct na Nether-ring 2, dan Nether-ring 3. Losse items achteraan, zoals nu.

**38 — as, puimsteen, smeulsteen, verkoold hout**
- `ash_block` (valt niet, schop), `ash_layer` (gedrag als sneeuwlaag: 1–8 lagen; geen smelten).
- Puimsteen: `pumice`, `polished_pumice`, `pumice_bricks`, elk met `_stairs`, `_slab`, `_wall` (patroon kloofsteen-set).
- `smoldering_stone`: licht (waarde in plan 38), in `#corruptedadventure:heat_sources`.
- Verkoold hout (houtset §2 Ring 2): `charred_log`, `charred_wood`, `stripped_charred_log`, `stripped_charred_wood`, `charred_planks`, `charred_stairs`, `charred_slab`, `charred_fence`, `charred_fence_gate`, `charred_door`, `charred_trapdoor`, `charred_pressure_plate`, `charred_button`. **Niet brandbaar** (Nether-hout-patroon zoals `dripshroom`; voorkomt bosbranden naast lava). `BlockSetType`/`WoodType` `corruptedadventure:charred`.

**38b — smeulwoud, vuurflora, askorst, gloeiende afgietsels**
- `ember_leaves` (smeulblad: bladeren aan verkoolde stammen, gloeiende texture; licht *[V3]*; vervalt niet als vanilla-bladeren, want de stam is dood: `persistent`-gedrag in plan 38b).
- `fire_bloom` (vuurbloem, `#small_flowers`) + `potted_fire_bloom`; `ash_sprouts` (askruid, kruismodel).
- `ember_berry_bush` (voorlopig; eetbare vuurflora) *[V7]*.
- `ash_crust`: instortende askorst boven holtes en lavabuizen; hergebruikt het `loose_sand`-mechanisme (`LooseSandTrigger`), dropt `ash_block`.
- `smoldering_ash`: as met gloeiende scheuren (emissive, licht 0–3), materiaal van de gloeiende as-afgietsels.

**40 — ertsen**
- Vulkaniet: `volcanite_ore`, `deepslate_volcanite_ore`, `basalt_volcanite_ore` (voorlopig: welke gastgesteenten precies, in plan 40 met W-biomes afgestemd via main).
- Zwavel: `sulfur_ore`, `basalt_sulfur_ore`.
- Vuurrobijn: `fire_ruby_ore`, `deepslate_fire_ruby_ore`.
- Items: `raw_volcanite`, `sulfur`, `fire_ruby` (placeholder-items zonder recipes, zoals `raw_sunstone`).
- Ertstags `c:ores/volcanite`, `c:ores/sulfur`, `c:ores/fire_ruby`.

**40b — vuurkristal**
- `fire_crystal_block` (wand van de geode), `small_fire_crystal_bud`, `medium_fire_crystal_bud`, `large_fire_crystal_bud`, `fire_crystal_cluster` (gloeiend, emissive).
- Item `fire_crystal` (`c:gems/fire_crystal`). Geen groei (alleen worldgen); nut in fase 2.

**46 — zwavel, fumarole, modder, geiser**
- `sulfur_crust`, `fumarole` (stoompuf via `animateTick`; in `heat_sources`), `boiling_mud` (vertraagt; in `heat_sources`), `geyser` (periodieke duw, server-side).
- Golf 1: registratie + basisgedrag. Eigen geluiden volgen in golf 2 via A (§6); tot dan vanilla-`SoundEvents`.

**51 — Nether-3**
- Koudlava-set: `cold_lava`, `polished_cold_lava`, `cold_lava_bricks`, elk met `_stairs`, `_slab`, `_wall`.
- `nether_ice` (smelt niet, glad), `packed_nether_ice`, `rime_netherrack`.
- Rijpzwam-houtset: `rimeshroom_stem`, `rimeshroom_hyphae`, `stripped_rimeshroom_stem`, `stripped_rimeshroom_hyphae`, `rimeshroom_planks` + houtset (stairs, slab, fence, fence_gate, door, trapdoor, pressure_plate, button), `rimeshroom_cap`.
- `ice_stalactite` (hergebruik `StalactiteBlock`).
- `glowing_fissure` (gloeiende scheur, warmtebron, licht).

**52 — oerijs**
- `primordial_ice`: alleen met Silk Touch te winnen, anders breekt het zonder drop.

**Blok-tags** (namen in `ModTags` via 37c, inhoud door B):
- `corruptedadventure:heat_sources` (hitte ↑ binnen ~4 blokken): `minecraft:lava`, `minecraft:magma_block`, `minecraft:fire`, `minecraft:campfire`, `smoldering_stone`, `fumarole`, `boiling_mud`, `glowing_fissure`; zielvuur *[V2]*.
- `corruptedadventure:warmth_sources` (kou ↓ in Nether-3): `lava`, `fire`, `soul_fire`, `campfire`, `soul_campfire`, `magma_block`, `glowing_fissure`, `smoldering_stone`.
- `corruptedadventure:ring_3_stones` (ertsdoelen ring 3): basalt, blackstone, `pumice`, `ash_block` *(voorlopig)*.
- `corruptedadventure:charred_logs` (in `#logs`, niet in `#logs_that_burn`); `corruptedadventure:rimeshroom_stems`.
- `minecraft:goats_spawnable_on` + basalt, blackstone, `ash_block` *[V7]*. Let op: die tag is globaal, maar geiten spawnen alleen waar een biome ze noemt.

**Vanilla-blokken die ring 3 gebruikt** (geen eigen variant): `basalt`, `smooth_basalt`, `blackstone`, `magma_block`, `obsidian`, `bone_block` (skeletten), `soul_soil` + `soul_fire` (zielvuur), dood koraal (zeekom), `coarse_dirt`/`moss_block`/varens (slapende krater, voorlopig).

## 4. Worldgen-sleutels

**Gedeelde density functions.** Golf 0 (39a) maakt van elke DF in deze tabel een stub (constante 0) zodat elke worktree laadt. Alleen de eigenaar vult hem in.

| DF (`corruptedadventure:…`) | Betekenis / bereik | Eigenaar | Lezers |
|---|---|---|---|
| `ring_3/overworld/region_heat` | 2D regio-as, ~[−1, 1]: heet (pieken, stromen) ↔ koel (zeekom, as) | 39a (W-terrein) | router `temperature`, W-biomes |
| `ring_3/overworld/region_ground` | 2D regio-as, ~[−1, 1]: begroeid/vreemd (smeulwoud, titanen) ↔ mineraal (zwavel, as) | 39a (W-terrein) | router `vegetation`, W-biomes |
| `ring_3/overworld/height` | 2D oppervlaktehoogte (zonder grotten) | W-terrein | W-biomes (features, surface) |
| `ring_3/overworld/caldera_mask` | 0..1: binnen een caldera (bodem 1) | W-terrein | W-biomes |
| `ring_3/overworld/caldera_dormant` | 1 = slapende caldera, 0 = actief/geen | W-terrein | W-biomes (biome *[V8]*), C (via biome-tag) |
| `ring_3/overworld/lava_channel` | 0..1: lavarivierbedding (1 = midden) | W-terrein | W-biomes (surface lava, lavavallen) |
| `ring_3/overworld/island_mask` | 0..1: zwevende eilanden aanwezig in de kolom | W-terrein | W-biomes (aankleding) |
| `ring_3/nether/region` | 2D regio-as Nether-3 | 39a (W-nether) | Nether-router `temperature` |
| `ring_3/nether/terrain` | terreinvorm Nether-3 (ingevoerd als ring 3 in `ring_blend/nether`) | W-nether | — |

- **Router:** de ring 3-sleutels in `ring_blend/overworld_temperature` en `_vegetation` wijzen naar de regio-assen (39a). Een derde as voor de slapende krater hangt af van *[V8]*.
- **Vloeistof (W-terrein):** ring 3 heeft geen natuurlijk water boven ~Y 0. Lava in kanalen en caldera's via surface rules of `RingFluidPicker`; de keuze ligt bij W-terrein, de regel komt in `RingFluidPicker`. Aquifers onder de grond: *[V8]*.
- **Nether-3 lavaniveau** in de preset (`fluid_levels.ring_3`): voorlopig **geen lavazee** (bevroren). Lava alleen in de gloeiende scheuren (features). 39a zet de waarde.
- **Noise-sleutels:** de eigenaar kiest `noise/ring_3_*` (overworld) en `noise/nether_ring_3_*`. Alleen de regio-noises (`ring_3_heat`, `ring_3_ground`, `nether_ring_3_region`) zijn van 39a.
- **Hoogtes (voorlopig):** vlaktes Y ~80–100. Kegels: zie *[V5]*. Calderabodem 20–50 onder de rand. Eilanden: zie *[V4]*. Zeekom-bodem Y ~30–45. Nether-3: vloer Y ~35, plafond ~110.
- **Features:** keys `corruptedadventure:ring_3/<naam>` en `corruptedadventure:nether_ring_3/<naam>`. De determinismeregels van 23c (overworld) en 35 (Nether) gelden. Skeletten, titanen-rotsen en eilandaankleding blijven binnen 3×3 chunks rond de eigen chunk *[V1]*.
- **Spawns:** ring 3-biomes zonder vee, behalve geiten in `volcanic_peaks` *[V7]*. Monsters vanilla tot fase 4.

## 5. Condities (eigenaar C)

| Sleutel | Waarde (start, SERVER-config) |
|---|---|
| `Condition.HEAT` (naam `heat`), HUD-kleur oranjerood | — |
| `[conditions.heat] sunRisePerSecond` | 0,55 (0 → 100 in ~3 min) |
| `sourceMultiplier` / `sourceRadius` | 1,5 / 4 blokken (tag `heat_sources`) |
| `peakMultiplier` | 2,0 (biome-tag `heat_peaks`) |
| `nightFall`, `shadeFall`, `undergroundFall`, `waterFall`, `refugeFall` | 0,5 / 0,25 / 0,5 / 2,0 / 2,0 (startwaarden; afstemmen in 4b) |
| `fireResistanceFactor` | 0,5 |
| drempels | 25 traagheid I · 50 zwakte I · 75 mining fatigue I · 100 in brand (`ThresholdEffect` ignite) |
| damage type | `corruptedadventure:heat` (eigen doodsbericht) |
| zone | overworld, `ringAt(...).ring() == RING_3` (hard op 0,5); daarbuiten ↓ 0,5/s |
| `Condition.COLD` (naam `cold`), `[conditions.cold]` | golf 2 (55); warmte via `warmth_sources` |
| water verdampt (44) | geen config; alle plaatsingsroutes (emmer, dispenser, ijs smelten/breken) in ring 3 onder open lucht |

- "Schaduw" overdag remt hitte niet volledig (alleen dak of ondergronds). De exacte regel staat in plan 43.
- **Ringweer-modifiers op de hitte** (`ASH_RAIN` ×1,5, `ERUPTION` ×2, `ASH_CLOUD` ×3) horen in ring 3 bij **C** (afwijking van het ring 2-patroon, waar 28 de hittegolf-modifier bouwde). Zo hangt C in golf 1 niet van A af. C leest `RingWeather.state` en de factoren uit `[weather.ring3]` (A). `ASH_CLOUD` bestaat pas na de merge van A; C koppelt die modifier in golf 2. De volgorde van de modifiers (backlog r. 95) legt C vast in 43.

## 6. Weer, geluid, particles (eigenaar A)

- **Weertypes** (`RingWeatherType`): `ASH_RAIN`, `ERUPTION` (bestaan al), **`ASH_CLOUD`** (nieuw, ring 3, overworld).
- **Config `[weather.ring3]`:** `clearMinSeconds`, `clearMaxSeconds`, `weatherMinSeconds`, `weatherMaxSeconds`, `ashRainWeight`, `eruptionWeight`, `ashCloudWeight` *[V6]*, `ashRainHeatMultiplier` 1,5, `eruptionHeatMultiplier` 2,0, `ashCloudHeatMultiplier` 3,0, `lavaBombsPerMinute`, `lavaBombRadius`, `lavaBombPeakFactor`.
- **Lavabommen:** vanilla `SmallFireball`, server-side, rond spelers in ring 3 onder open lucht, dichter in `heat_peaks`. Vuur volgt `mobGriefing`, geen explosie.
- **Sound events** (`ModSoundEvents` + `ModSoundDefinitionsProvider`; audio uit `tools/sounds/Ring3Sounds.java`):
  - weer: `weather.ring_3.ash_rain`, `weather.ring_3.eruption`, `weather.ring_3.ash_cloud`;
  - ambient: `ambient.ring_3.rumble`, `ambient.ring_3.embers`, `ambient.ring_3.lava`, `ambient.ring_3.titan_hum` (voorlopig), `ambient.ring_3.caldera` (slapende krater), `ambient.nether_ring_3.wind`, `ambient.nether_ring_3.ice_creak`;
  - blokken: `block.fumarole.puff`, `block.geyser.burst`, `block.boiling_mud.bubble` (golf 2).
- **Particle types** (`ModParticleTypes` + `ModParticleDescriptionProvider`; sprites van T): `ash_flake`, `ember`, `steam_puff`, `ice_glint`.
- **Payloads:** geen nieuwe gepland in golf 1. Is er toch een nodig, dan main melden; de integratie verhoogt `ModNetwork.PROTOCOL_VERSION` één keer per golf.
- **Lucht-/fogkleur bij een uitbarsting:** eigen client-aanpak (mixin of `DimensionSpecialEffects`) in `client/`; A kiest en legt het vast in plan 45.

## 7. Textures (eigenaar T)

- Script `tools/textures/Ring3Textures.java` (37b-stijl: licht linksboven, 5–7 tonen, selectieve omlijning, vlakheidscontrole).
- Bestandsnamen = blok-/itemnaam (`block/<naam>.png`, `item/<naam>.png`, `particle/<naam>_<n>.png`). Zijtexturen volgen het patroon van ring 2 (`_top`, `_side`, `_bottom`, `_overlay` voor ertsen, `_stage` voor knoppen).
- Gloed: `smoldering_stone`, `smoldering_ash`, `ember_leaves`, vuurkristal en vuurrobijn-overlay emissive en pulserend (§1.4); vulkaniet en zwavel mat.
- B verwijst in de models naar deze paden. Ontbreekt een PNG bij `runData` in B's worktree, dan legt B een tijdelijke kopie van een bestaande ring 2-texture onder de juiste naam neer, **niet** gecommit. De integratie gebruikt T's versie.

## 8. Lang

- Helpers per stroom (37c): `datagen/lang/Ring3BlockNames` (B), `Ring3BiomeNames` (W-biomes, W-nether), `Ring3ConditionText` (C: meter, doodsbericht), `Ring3WeatherText` (A: ondertitels).
- Engels verplicht, Nederlands erbij.
- Elke helper heeft één methode voor en en één voor nl; de providers roepen ze aan (37c zet de aanroepen klaar).

---

## 9. Open vragen (vóór golf 1 te beantwoorden)

**Vraag 1 — Reuzenskeletten en titanen-rotsen: hoe groot en hoe gemaakt?**
- **A. Skeletten 20–40 blokken lang, half begraven, van `bone_block` met verweerd basalt; titanen-rotsen als liggende/geknielde reuzensilhouetten van basalt (15–35 hoog). Beide als sjabloon-features binnen 3×3 chunks (hergebruik `surface_fossil`/`FossilLayout` met eigen `.nbt`-sjablonen uit een deterministisch script) (aanbevolen).** Herkenbaar en deterministisch; feature-grens ~40–48 blokken; sjablonen moeten we zelf maken.
- **B. Kolossaal (60–120 blokken).** Kan alleen als structure (jigsaw/template, categorie `structures`, fase 3). In fase 1 alleen kleine skeletten; het wow schuift naar later.
- **C. Als terreinvorm: ribben en schedelbogen uit de grond (density function, basalt/puimsteen, geen botmateriaal).** Elke maat mogelijk en goedkoop; leest abstracter (ribben/rotsbogen), niet als "skelet".

**Vraag 2 — Wat doet het zielvuur rond de skeletten?**
- **A. Vanilla zielvuur op zielgrond: blijvend, licht 10, brandschade bij aanraking, geen hittebron (aanbevolen).** Geen code; blauw vuur als fantasy-contrast; telt niet mee in de hitte.
- **B. Zelfde, maar wel een hittebron (`heat_sources`).** Titanenveld wordt een gevaarzone; nog minder rust.
- **C. "Koud vuur": hitte ↓ binnen 3 blokken.** Een fantasy-mechaniek en tweede schuilplek; ondermijnt de schaarste en concurreert met de slapende krater.

**Vraag 3 — Smeulwoud-bomen (`ember_woods`): hoe gloeien de kronen?**
- **A. Verkoolde stam + `ember_leaves` met emissive texture, zonder bloklicht; bomen 6–12 hoog, dicht genoeg voor een woud (aanbevolen).** Gloeit in het donker zonder lichtberekening bij generatie; monsters spawnen er gewoon ('s nachts spannend).
- **B. `ember_leaves` met echt licht (7).** Het mooiste in het donker; veel lichtbronnen per chunk = duurdere generatie; weinig monsters in het woud.
- **C. Kale verkoolde stammen met hier en daar een gloeiknop (lichtblok).** Goedkoop; voelt minder als woud.

**Vraag 4 — Zwevende basalteilanden: hoe vaak en hoe hoog?**
- **A. Y 150–210, alleen boven calderameren en lavarivieren, ~1 cluster van 1–4 eilanden (8–30 breed) per ~3 caldera's, met lavavalletjes eraf (aanbevolen).** Zeldzaam landmark dat de lavavallen verklaart; 3D-ruis alleen lokaal (masker), binnen het budget.
- **B. Vaker, ook boven asvlaktes en het titanenveld.** Meer fantasy; duurder (budget 1,75× in gevaar) en minder bijzonder.
- **C. Heel zeldzaam (≤ 1 cluster per ~2.000 blokken).** Een echte vondst; veel spelers zien ze nooit.

**Vraag 5 — Hoe hoog zijn de horizon-vulkanen?**
- **A. De grootste kegels Y 230–260 (boven de wolken op 192, ruimte voor pluim-particles tot de bouwlimiet 320); gewone kegels Y 140–200 (aanbevolen).** Zichtbaar vanuit `dry_riverbeds` bij normale zichtafstand (12+ chunks); veel ondergrond, maar 2D-hoogte is goedkoop.
- **B. Tot Y ~300.** Maximaal wow; de krater en pluim botsen met de bouwlimiet; meer ondergrond (prestatie).
- **C. Y 190–210 (net rond de wolken).** Goedkoper; vanuit ring 2 minder zichtbaar.

**Vraag 6 — Hoe vaak komt de aswolk?**
- **A. Zeldzaam: gewichten asregen 45 / uitbarsting 40 / aswolk 15; aswolk korter (120–240 s) met ~20 s waarschuwing (gerommel, verdonkering) (aanbevolen).** Een piekmoment dat je kunt ontvluchten; vanilla-ritme blijft.
- **B. Alleen als nasleep van een uitbarsting (kans ~30 %).** Een verhaal-logische keten; vraagt een volgorde-regel in de weercyclus.
- **C. Gelijke gewichten.** Vaak; risico dat het zwaar en vermoeiend wordt.

**Vraag 7 — Geiten en eetbare vuurflora (vervolg op antwoord 12): botsen ze met de schaarste?**
- **A. Geiten alleen in `volcanic_peaks` (lage spawnweight, groepjes 1–3, op basalt/blackstone/as); eetbare vuurflora blijft, maar zeldzaam (één plant, alleen in `ember_woods` en slapende kraters) (aanbevolen).** Voedsel blijft schaars en riskant (geiten op de heetste plek); geen botsing. Let op: magma doet geiten pijn; spawn alleen op `goats_spawnable_on`.
- **B. Geiten, geen eetbare flora.** Eten komt alleen van de pieken (risico = beloning); minder werk (geen bes-struik).
- **C. Geiten + ruimere flora.** Milder; de schaarste verdwijnt grotendeels.

**Vraag 8 — De slapende krater (oase): eigen biome, en is er water?**
- **A. Eigen micro-biome `dormant_caldera` (zoals `oasis_flats`: hitte ↓ 2,0/s, groen, eigen kleuren en geluid), zonder natuurlijk water; water in ring 3 alleen diep ondergronds (aquifers onder Y ~0) (aanbevolen).** Duidelijk rustpunt via biome-tag `heat_refuges`; past bij "gestolen water". Vraagt een derde router-as (`ring_blend` op `weirdness`) of een biome-keuze uit `caldera_dormant`. Is een 8e biome naast de 7 sub-biomes.
- **B. Micro-biome + een kleine warmwaterbron in de krater (natuurlijk water, verdampt niet).** Het meest uitnodigend; ondermijnt "water gestolen" en de schaarste.
- **C. Geen biome: alleen aankleding (groen, varens) + koelte via nabijheid van mos/varens.** Geen extra biome; koelte-regel per blok is duurder en minder duidelijk voor de speler.
