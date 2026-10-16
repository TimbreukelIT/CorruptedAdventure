# Ring 4 golf 2 — stroom W-biomes: 75, 76, 77, 75b

Scope: `worldgen`. Branch `ring4/biomes-g2` (worktree, basis `main` d1a37f11: 72/72b van B, 71b van T, 80 van A, 78 van W-nether en de `steam_fens`-prestatiefix). Poort n = 2 (server 25572, RCON 25582). Bron: `golf-2.md` §2 W-biomes, Q4, Q8, Q10, Q13, Q14, G1, G4, H5, F1–F12, H1–H4, §6b; `golf-2-blocks.md` (aanwijzingen voor 75b); `contract.md` §2–§4; `waves.md` §4–§5.

## Gedeelde basis (75)
- `Ring4Terrain`: het pure ring 4-terrein (`IslandField`) van het niveau voor de features (patroon `Ring3Terrain`), plus `blighted(x, z)` (de bandgrens van weer en gif).
- `Ring4Filter` (`corruptedadventure:ring4_filter`): bereiken op de 2D-uitgangen van het veld (patroon `volcanic_filter`, incl. `slope`), `ground_only` (niet op eilanden, bogen of brugdekken), `fall_clearance` (afstand tot elke vastgelegde val) en `knob_clearance` (afstand tot elke klimknop).
- `Ring4Placements`: sleutels `ring_4/…`, de bevroren worldgen-heightmaps (`SURFACE` = `WORLD_SURFACE_WG`, `FLOOR` = `OCEAN_FLOOR_WG`), predicaten (`ON_LAND`, `ON_WATER`, `UNDER_WATER`, `waterAbove`, `waterNear`) en `register`/`perChunk`.
- Determinisme (23c): plaatsing in de eigen chunk op de bevroren heightmaps, `JitteredGridPlacement`, features die alleen in de eigen chunk schrijven; alleen boomkruinen reiken in de buren (3 × 3-venster).

## 75 — mythische band, `sky_gardens`, gloed onder de eilanden
| Waar | Wat |
|---|---|
| alle grondbiomes (onder de eilanden) | `island_glow` (eerst): per cel van 4 × 4 één lichtbron op droog land in de eilandschaduw (licht ≥ 1 overal op vlak land). Mythisch (per kolom via `Ring4Band`): `glow_bloom`/`glowcap` + `aquamarine`-knoppen; bedorven: `glowcap`/`toxic_crystal_cluster` (licht 5) + `blighted_glowcap` en gifkristalknoppen; geen budding-blokken (H5 = A). Daarna `island_underside`: strengen `hanging_swamp_moss` (2–7), losse `glowing_hanging_roots`/`hanging_roots`, en onder lage eilanden (gat ≤ 40) af en toe een streng tot één blok boven de grond (klimbaar). |
| `cascade_vale` | `ring_4/moonpetal_tree` (kersvorm in moonpetal-hout; ook de sapling), gras/varens, `glow_bloom` langs de meren, zeegras |
| `crystal_lagoon` | koraal (`local_coral`: vanilla-vormen, blijft in de chunk), zeegras, sea pickles |
| `lotus_shallows` | `giant_lily_pad` (2 × 2 geheel), `lotus`, rietaccenten op de oevers, zeegras, gras, `glow_bloom` |
| `glowroot_swamp` | reuzenmangrove (`ring_4/swamp_tree`: vanilla hoge mangrove een maat groter, `glowcap`/mostapijt op de wortels, geen vines/bijen), `glowcap`, mostapijt, gras, zeegras |
| `mist_fjords` | spar/den, af en toe donkere eik op de vlakke toppen (helling < 0,8), varens, mostapijt, zeegras |
| `sky_gardens` | moonpetal-bomen, `glow_bloom` + bloemen, gras |
| diepe poelen (lagune, lotus, moeras, fjorden) | `bubble_spring` als bodemblok onder ≥ 6 water |

**Na 74 (main 5c5f0594, in deze tak gemerged):** ranken tussen de eilandparen (`island_vines`, per chunk het eigen stuk van elke rank): voor elk paar zonder rotsboog (`IslandField.islandPairs`) een doorhangende rank van `mangrove_roots` (draagt zichzelf) van rand tot rand, 2 onder de top van elke rand, doorhang 1 + 0,08 · overspanning, vlak-aan-vlak (geen gaten), 40 % een tweede rank 2 ernaast; eronder op 20 % van de blokken 1–3 `hanging_swamp_moss`, op 6 % een `glowing_hanging_roots`. Een rank neemt alleen lucht en bladeren van featurebomen (niet persistent), dus nooit de wereldboom, water of terrein. Rond de hemelmeren: extra bloemen op de oever (`sky_lake_bank_flowers`, water binnen 1) naast de bestaande `glow_bloom` langs het water; niets in het meer. **Wereldboom vrij:** `ON_LAND`, `UNDER_WATER` en `waterAbove` eisen dat het blok eronder geen log of blad is; niets hangt onder wereldboomhout; geodes snijden geen wortels.

## 76 — bedorven band, `withered_gardens`
| Waar | Wat |
|---|---|
| surface `peat_moors` | `peat` als top- en dieptelaag (de veenkorst; onder water modder), via `ring4_surface.py` |
| `peat_moors` (`LOCAL_MODIFICATIONS`, eerst) | gesloten moerasgasbellen (`gas_pocket`: 2–4 × 2–4 × 1–2, 1–2 onder de korst, aan alle zes kanten droge vaste grond); 1–2 lagen gas op de bodem van de veenzinkgaten (`sinkhole_gas`, uit `Column.sinkholeFloor`, 2 lagen vanaf 9 diep); gifpoelen (`toxic_pool`) |
| `reed_marsh`, `drowned_forest` | gifpoelen (minder) |
| oevers `peat_moors`, `drowned_forest` | `sucking_mud_patch` (≤ 4 × 4, 2–3 diep, op de grondhoogte van de oorsprong; G1 = A), niet in `reed_marsh` |
| `drowned_forest` | `withered_moonpetal`-stammen (recht 7–13 of gevorkt) op de bodem in water tot 10 diep |
| `withered_gardens` | dezelfde kale bomen, dode struiken, `blighted_glowcap` |
| `reed_marsh` | dicht riet (pollen r 3, 2–3 hoog, ook in water één diep), `blighted_glowcap` |
| oevers `peat_moors`, `drowned_forest` | ijl riet, `blighted_glowcap`, dode struiken (veen) |

- **Gifpoel** (`toxic_pool`): schijf r 1–3, rand 1 diep, midden 1–2; alleen als de hele kom in de chunk op vlakke grond ligt, elk blok nu vaste grond is en elke zij- en onderkant van elk poelblok poel of droge vaste grond is (`ToxicPoolFeature.closed`): nooit gif tegen lucht of water opzij/onder (de bovenkant is het open oppervlak). Bronblokken zonder tick.
- **Dichter richting ring 5:** gas, poelen, modder en `blighted_glowcap` met `ring_chance` op de voortgang over ring 4 (0,35 aan de binnenrand → 1 aan de buitenrand).
- Gevaren alleen op de eigen grond van het terrein (`ring4_filter` `ground_only`), niet op eilanden; ze vormen de grond vóór het riet en de planten (riet en planten staan dus nooit op modder of aan een poelrand door toeval van volgorde).

## 77 — ertsen, sporen, dieren
- **Biome modifier `add_ring_4_ores`** (`UNDERGROUND_ORES`, `Ring4Biomes.ALL` + `sundered_reach`, niet `verdant_falls`): vanilla ×0,2 / ×0,15 / ×0,1 (`RingVanillaOres`, `ring_4/ore_*`), dan `ring_4/tidesilver_ore` (8, 5×, driehoek Y −48…48) en `ring_4/jade_ore` (6, 3×, Y −64…0), elk met de varianten steen / `ring_4_stones` / deepslate en een `ring_chance` op ring 4 (de ring 5-helft van `sundered_reach` krijgt niets).
- **Aquamarijn-geodes** (`sunken_geode`, `LOCAL_MODIFICATIONS` in `crystal_lagoon` en `mist_fjords`): holte r 2–3 + kristal (10 % budding) + calciet + tidestone, half open in de bodem onder ≥ 8 water (alleen het deel op/onder de bodem van elke kolom); de holte vol water, knoppen (45 % per wandvlak) waterlogged naar binnen. Alleen als de geode in de chunk ligt, geen blok lucht is (grot) en elke kolom water boven de bodem heeft: nooit water tegen lucht.
- **Dieren:** gecontroleerd tegen V4 (geen wijziging); papegaaien in `verdant_falls` komen met 75b.
- **Tunnelnet (73, main b6d7925d gemerged):** `tunnel_lining` (`UNDERGROUND_DECORATION`, per chunk, alle grondbiomes) bekleedt de wanden, vloeren en plafonds van het net (`IslandField.tunnelWall`/`tunnelFill`): droog bedorven net (tunnels, gaskamers, gifpoelen) gifkristal-plekken (2 %, in gaskamers 5 %; 30 % budding: budding alleen ondergronds, H5 = A) met een knop/cluster ervoor alleen in de lucht van het droge net; overstroomd net aquamarijn (1,5 %, 30 % budding) met een waterlogged knop in het water; tidesilver (1,2 %) in beide; jade (2 %) alleen in het overstroomde net onder Y 0; klauwkras (`clawed_tidestone`, 3 %) alleen in zijwanden. Vloeren: droog net slijmspoor (6 %), botten (`bone_block`, 0,6 %), nest (0,4 %); overstroomd net botten. Alleen vaste droge wandblokken (steen/`ring_4_stones`/ertsen) worden ander vast blok; ervoor alleen in lucht van het droge net of water van het overstroomde net: nooit water/gif tegen lucht, niets in het gas van een gaskamer, niets dat gas ontsteekt. Beslissingen per blok uit een hash van de positie (geen random), alleen de eigen chunk.

## 75b — tropische flora `verdant_falls` (§6b)
| Deel | Wat |
|---|---|
| klimroutes (`ledge_strand`) | strengen `verdant_vine_plant` met de kop `verdant_vine` op leeftijd 25 (groeit niet door), 35 % bloeiend (per streng, op positie: knoppen en losse strengen kiezen gelijk); accent `verdant_hanging_orchid` onder richels en het richelpad; nooit in water. De strengen komen vóór alle andere flora, en wandplanten nemen alleen lucht: geen plant maakt een route korter (100 % blijft beklimbaar). |
| meeroevers (stille meren, Y ≥ 65) en kloofranden | emergente boom (`ring_4/verdant_emergent_tree`): stam 14–22, 2–4 plankwortels (`verdant_wood`, trapsgewijs 3/2/1, tot op de grond), 8 takken onder een schermkroon (straal 5–6, koepel, afhangende rand), onder elke tak een liaan (1–4, deels bloeiend) of een hangorchidee; vruchtpeulen (6 % per zijvlak, stadium 0–2, `facing` naar de stam) en orchideeën (5 %) op de stam. Oevers: water binnen 4; randen: een kolom binnen 6 ligt ≥ 12 lager (`slope` `drop`, nieuw). Nooit op een wand (`cliff` < 0,5), ≥ 5 van elke val en klimknop, alleen op eigen grond. |
| treden (helling < 0,4) en stootborden (0,4–1,5) | boomvarens (`ring_4/verdant_tree_fern`, ook de sapling): stam 4–7, acht bladarmen met afhangende punten; ≥ 3 van vallen en knoppen |
| grond overal | `verdant_fern`; treden `verdant_tall_fern`, `verdant_giant_leaf`, `verdant_bromeliad` |
| oevers en bassinranden | `verdant_giant_leaf` (water binnen 2), `verdant_blossom_bush` (water binnen 3); nooit op de calcietlippen en drempels (planten hebben aarde nodig) |
| kloofwanden | `wall_plant`: vanaf de voet van een wand (`slope` `rise` ≥ 6 binnen 2) tot 28 omhoog `verdant_orchid` en `verdant_wall_bromeliad`, `facing` van de wand af; ≥ 3 van de vallen |
| stille meren | een paar `lotus` en `verdant_water_bloom` (water 2 blokken in alle vier richtingen: geen beek; Y ≥ 65: niet op de voetrivier; ≥ 5 van de vallen, dus niet in plonspoelen) |
| spawns | papegaaien (gewicht 10, 1–2) naast kikkers en schildpadden (F10 = A) |

Vrij (F4 = A): vallen en lippen (afstand tot de vastgelegde vallen, calciet draagt geen planten), richelpad, klimroutes (afstand tot de knoppen, strengen eerst), brugdekken en rotsbogen (`ground_only`), nissen en tunnels (onder het oppervlak; nisjes liggen achter vallen), de voetrivier (Y ≥ 65 voor waterplanten en oeverbomen).

## CODEBASE-delta
**Nieuwe classes (`worldgen.feature`)**
- `Ring4Terrain`, `Ring4Filter` (+ `ModPlacementModifierTypes.RING4_FILTER`, blok `// ring 4 — W-biomes g2`), `Ring4Placements`.
- `IslandGlowFeature`/`IslandGlowConfiguration` (`island_glow`), `IslandUndersideFeature`/`IslandUndersideConfiguration` (`island_underside`), `GiantLilyPadFeature` (`giant_lily_pad`), `ReedClumpFeature`/`ReedClumpConfiguration` (`reed_clump`), `LocalCoralFeature` (`local_coral`); in `ModFeatures` (blok `// ring 4 — W-biomes g2`).
- `Ring4MythicFeatures` (configured/placed 75).

**Gewijzigd**
- `Ring4Features.ORDER` (+ helpers `vegetal`, `ground`), `configured`/`placed` roepen `Ring4MythicFeatures` aan.
- `Ring4Biomes` (javadoc), `Ring4BiomesTest` (verdant_falls alleen eigen features, gloed eerst in elke grondbiome).

**Registries:** 5 feature-types, 1 placement modifier; configured `ring_4/moonpetal_tree` (key van B) en 17 eigen; 22 placed.

**75 na 74:** `IslandVinesFeature`/`…Configuration` (`island_vines`), `Ring4Placements.OFF_TREES`; placed `ring_4/island_vines`, `ring_4/sky_lake_bank_flowers`, configured `ring_4/island_vine`.

**76 — nieuwe classes:** `SuckingMudPatchFeature`/`…Configuration` (`sucking_mud_patch`), `ToxicPoolFeature`/`…Configuration` (`toxic_pool`), `GasPocketFeature`/`…Configuration` (`gas_pocket`), `SinkholeGasFeature`/`…Configuration` (`sinkhole_gas`), `Ring4BlightedFeatures`. **Gewijzigd:** `ModFeatures` (blok `// ring 4 — W-biomes g2`), `Ring4Features.ORDER` (+ `local`), `tools/worldgen/ring4_surface.py` + `noise_settings/overworld.json` (`peat_moors` = veen). **Registries:** 4 feature-types; 11 configured, 12 placed.

**77 — nieuwe classes:** `SunkenGeodeFeature`/`…Configuration` (`sunken_geode`), `Ring4OreFeatures`. **Gewijzigd:** `Ring4BiomeModifiers` (`ADD_RING_4_ORES`, `ores()`, `oreBiomes()`), `ModFeatures`, `Ring4Features.ORDER`. **Registries:** 1 feature-type, 1 biome modifier; 3 configured, 16 + 3 placed (`ring_4/ore_*` vanilla, tidesilver, jade, geodes).

**77 tunnels:** `TunnelLiningFeature` (`tunnel_lining`, constanten in de klasse), placed `ring_4/tunnel_linings` (`UNDERGROUND_DECORATION`, grondbiomes).

**75b — nieuwe classes:** `VerdantTrunkPlacer` (`verdant_trunk_placer`, in `ModTrunkPlacerTypes`, blok `// ring 4 — W-biomes`), `VerdantFoliagePlacer` (`verdant_foliage_placer`, `Shape` UMBRELLA/FRONDS, in `ModFoliagePlacerTypes`), `WallPlantFeature`/`…Configuration` (`wall_plant`). **Gewijzigd:** `VerdantFallsFeatures` (flora; `ring_4/verdant_tree_fern` = key van B), `LedgeStrandConfiguration` (optioneel `tip`, `variant` + `variant_chance`; constructor met 7 argumenten blijft), `LedgeStrandFeature.strand` (tip + variant op positie), `LedgeStrands.inVariant`, `SlopeFilter` (meting `drop`, `rimTop`), `Ring4Biomes` (papegaaien in `verdant_falls`), `Ring4Features.ORDER`. **Registries:** 1 feature-type, 1 trunk- en 1 foliage-placer-type; `SuckingMudPatchConfiguration` kreeg `biomes`; 9 + 2 configured, 13 placed.

**Opruimen (backlog r. 265):** `placeholder_ring_4` en `placeholder_nether_ring_4` weg uit `PlaceholderBiomes`, `ModBiomes` en de lang-providers (de gegenereerde biome-JSON's en langregels vervallen).

## Code-review (medium, twee review-subagents: 75+76, daarna 77+75b+ranken)
Verwerkt in de vervolgcommit:
1. Dode bomen in `drowned_forest`: hun "kruin" was een luchtblok boven elk stameinde, onder water een luchtbel tegen water. Nu een stamblok (`withered_moonpetal_log`) als kruin; stammen 9–15 (recht) en 8–14 (gevorkt), water tot 10 diep (korte stammen blijven verzonken).
2. Gasbellen (en gifpoelen) blijven ≥ 1 van de chunkrand (`SuckingMudPatchFeature.start(c, size, margin)`): `closed` leest zo alleen de eigen chunk; twee bellen van buurchunks beoordelen elkaar niet meer (volgorde).
3. Gloed en hangsels onder eilanden: bladeren van een featureboom uit een buurchunk tellen als open (scan en plek volgorde-onafhankelijk; de plant vervangt zo'n blad).
4. Gloed: faalt de getrokken plek (water, geen schaduw), dan de andere kolommen van de cel, dichtstbij eerst; bedorven lichtbron alleen `glowcap` (licht 6), clusters/knoppen/`blighted_glowcap` als accent.
5. Gloed, hangsels en wandplanten trekken al hun random vóór ze de wereld bevragen.
6. Oeverplaatsingen in de bedorven band (`bank_sucking_mud`, `blighted_bank_reeds`) en de oeverbomen van `verdant_falls`: rastermarge ≥ bereik van `waterNear` (water lezen alleen in de eigen chunk waar buren water kunnen veranderen).
7. Zuigmodder neemt alleen kolommen in `peat_moors`/`drowned_forest` (config `biomes`): nooit in het riet van `reed_marsh`.
8. Reuzenlelie alleen op een waterbron (geen gifslijk).
9. Ranken tussen eilanden: de tweede rank ligt naast de lijn; de paren worden in een ruimer vak gevraagd (geen gaten aan chunkgrenzen).
10. Emergente bomen ≥ 7 van vallen en knoppen (kroon reikt 6): geen blad in een val of een knoopstreng.
11. Plankwortels: alleen op droge, vaste grond binnen 3 omlaag (geen hout boven de kloof, nooit in water); de hangorchidee onder een tak alleen in lucht.
12. Wandplanten alleen op de eigen grond (`ground_only`: niet op brugdekken).

Niet opgepakt (bewust, backlog/INT):
- Peulen, orchideeën en lianen gaan via de stamsetter van `TreeFeature` (tellen als bron voor de bladafstand); met de huidige maten raakt geen blad ze en vergaat niets (`VerdantFloraTest`). Netter als `TreeDecorator` (nieuwe registry) → backlog.
- Mos onder een rank of eiland en bladeren van een latere buurboom (beide `#replaceable_by_trees`): cosmetisch volgorde-verschil van losse blokken.
- `CODEBASE.md` en backlog r. 265: INT (waves.md §5); de delta staat hierboven.

## Beslissingen
- 2026-10-16, `worldgen` (implementatie W-biomes, 75): de gloed onder de eilanden als vast rooster (cellen van 4, bron 1–2 in de cel) i.p.v. een losse kans, zodat elk vlak blok in de schaduw licht ≥ 1 krijgt; de palet per kolom via de band (`Ring4Band.blighted`), niet per biome.
- 2026-10-16, `worldgen` (implementatie, 75): moerasboom = vanilla's hoge mangrove een maat groter (stam 5–14, wortels 3–7) met `glowcap` en mostapijt op de wortels, zonder vanilla-vines (geen vanilla vine in ring 4) en zonder bijen. Fjordbomen: vanilla spar (default), den 40 %, donkere eik 10 %.
- 2026-10-16, `worldgen` (implementatie, 75): `moonpetal_tree` = vanilla's kersvorm met moonpetal-blokken (ook de sapling).
- 2026-10-16, `worldgen` (implementatie, 75): koraal via eigen `local_coral` (vanilla-vormen, maar alleen ≥ 1 blok van de chunkrand) omdat vanilla tot 6 blokken in buurchunks schrijft (23c-regel).

- 2026-10-16, `worldgen` (implementatie, 76): `peat_moors` krijgt veen als top- en dieptelaag (ontwerp "veen: oppervlak van `peat_moors`"); de gasbellen liggen 1–2 blokken onder die korst.
- 2026-10-16, `worldgen` (implementatie, 76): "dichter richting ring 5" = `ring_chance` over de voortgang door ring 4 (0,35 → 1) op gas, gifpoelen, zuigmodder en `blighted_glowcap`; riet en bomen niet.
- 2026-10-16, `worldgen` (implementatie, 76): dode bomen = alleen de `withered_moonpetal`-stam (recht 7–13, of gevorkt 60 %), in `drowned_forest` op de bodem in water tot 10 diep.

- 2026-10-16, `worldgen` (implementatie, 75b): de liaan in `ledge_strand` 35 % bloeiend, gekozen op positie (zo kiezen de knoopstrengen en de losse strengen hetzelfde); accent onder de richels = `verdant_hanging_orchid` (§6b: hangorchideeën onder het richelpad en de klimrichels) i.p.v. de gloeiwortels.
- 2026-10-16, `worldgen` (implementatie, 75b): emergente boom stam 14–22 (1 breed), 2–4 plankwortels, 8 takken, schermkroon straal 5–6; vruchtpeul 6 % en orchidee 5 % per zijvlak van de stam; boomvaren stam 4–7, kroon straal 2–3. Bomen ≥ 5 en boomvarens/wandplanten ≥ 3 van vallen en klimknoppen.

- 2026-10-16, `worldgen` (implementatie, 75 na 74): de ranken tussen eilanden van `mangrove_roots` (contract §3: vanilla mangrovewortels horen bij ring 4; draagt zichzelf, beloopbaar) met mos en gloeiwortels eronder; op alle paren zonder boog, 40 % dubbel. Rond de hemelmeren alleen planten op de oever, niets in het water (aanwijzing main).

## Backlog
- `worldgen`: vruchtpeulen, orchideeën en lianen van de emergente boom naar een eigen `TreeDecorator` (nu via de stamsetter; correct maar fragiel bij andere kroon-/takmaten).
- `blocks`: `verdant_vine`/`_plant` en `verdant_hanging_orchid` staan in `#replaceable_by_trees` (ook water): een kroon kan ze vervangen; nu houden de bomen afstand tot knoppen en vallen.
- `worldgen`/INT: een rank van `mangrove_roots` die een stam van een featureboom in een buurchunk kruist, maakt die boom (zeldzaam) volgorde-afhankelijk (wortels zijn geen `replaceable_by_trees`); determinismecheck rond eilandranden.
- `worldgen`/INT: een erts van een buurchunk (tidesilver-config richt op `ring_4_stones`) kan later klauwkras (`clawed_tidestone` hoort bij `ring_4_stones`) aan een chunkrand vervangen: zeldzaam volgorde-verschil van één blok.
- `blocks`: `glow_bloom`, `glowcap`, `verdant_bromeliad`, `verdant_blossom_bush` niet in `#replaceable_by_trees`: een boomkruin uit een buurchunk die later komt, laat zo'n plant staan, een eerdere niet (zeldzaam: kruinen op grondhoogte). INT-determinisme let erop.

## Voortgang
- 2026-10-16: 75 gebouwd; B (incl. `Ring4MythicFeaturesTest`, `Ring4BiomesTest`, `FeatureOrderTest`) en D (twee keer, tweede keer 0 geschreven) groen.
- 2026-10-16: 76 gebouwd; B (incl. `Ring4BlightedFeaturesTest`) en D groen.
- 2026-10-16: 75b gebouwd; B (incl. `VerdantFloraTest`: elk blad binnen afstand 6 van stam/tak, strengtip en bloeiaandeel) en D groen.
- 2026-10-16: main (73) gemerged; tunnelbekleding (77-rest); B (incl. `TunnelLiningFeatureTest`) en D groen.
- 2026-10-16: main (74) gemerged; ranken tussen eilandparen, oeverbloemen hemelmeren, wereldboom vrij; B en D groen.
- 2026-10-16: 77 gebouwd (zonder de tunneldelen: 73 nog niet op main); B (incl. `Ring4OreFeaturesTest`) en D groen.
