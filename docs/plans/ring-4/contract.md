# Ring 4 — contract (namen, sleutels, eigenaarschap)

Scope: ring 4 + Nether-ring 4 (bouwplan blok F, `fase-1-wereld.md` §3). Opgesteld 2026-10-10 uit de sparring ring 4 (`fase-1-wereld.md` → Ring 4, `02-beslissingenlog.md`) en de werkwijze in `waves.md`.

**Regels**
- Alles in dit bestand is **vast** zodra golf 0 gecommit is. Tot dan kan het golf 0-plan het aanvullen; de open punten (§9) worden daar met de gebruiker beslist. Daarna: een stroom die iets anders of extra nodig heeft, stuurt de vraag naar main; main beslist (of vraagt de gebruiker), past dit bestand aan en meldt het aan alle stromen. Nooit zelf in andermans bestand werken.
- Namen zijn registry-namen onder `corruptedadventure:` (Engels, snake_case, ring-thema herkenbaar). De werktitels uit de sparring (`tidestone`, `moonpetal`, `withered_moonpetal`, `tidesilver`, de biomenamen) staan vast. "(voorlopig)" = een voorstel dat het golf 0-plan nog mag veranderen.
- **Te bepalen in het golf 0-plan** = nog niet besloten; niet zelf kiezen.

---

## 1. Ringgrenzen (config, pakket 58)

| Sleutel (COMMON-config `corruptedadventure-common.toml`) | Nu (37c) | Nieuw |
|---|---|---|
| `ring1OuterRadius` | 1500 | 1500 |
| `ring2OuterRadius` | 3500 | 3500 |
| `ring3OuterRadius` | 6000 | 6000 |
| `ring4OuterRadius` | 7800 | **8000** |
| `ring5OuterRadius` | 9500 | **9700** |
| `voidOuterRadius` | 10000 | **10200** |
| `boundaryNoise`, `transitionWidth` | 96, 128 | ongewijzigd; de bredere zone 4→5 (256): zie hieronder |

- Nether ÷4: Nether-4 1.500 – 2.000, Nether-5 2.000 – 2.425, void-rand ~2.550. World border = void-rand (overworld 10.200, Nether ~2.550).
- `/ca tpring` (snapshot): midden ring 4 = 7.000, ring 5 = 8.850, void ~9.950 (Nether ÷4).
- Geldt alleen voor **nieuwe** werelden. Alle ring 4-tests gebruiken een verse wereld (seed `20260930`, preset `corrupted_world`).
- **Zone 4→5 256 breed** (besluit) via een **overgangsbreedte per grens in de config** (golf 0-plan 2026-10-13): nieuwe sleutel naast `transitionWidth` (standaard 128 voor de andere grenzen, 256 voor 4→5), ook in de snapshot en de layout-sync; `ringAt` kent de echte zone, dus biome, condities, kompas en weer zien dezelfde grens. Oude werelden/snapshots zonder de sleutel: elke grens `transitionWidth`. **Nether ÷4** zoals de radii: zone 4→5 in de Nether 64 breed. **Vorm (58, 2026-10-13):** config-sleutel `transitionWidthOverrides`, een lijst van precies 5 ints per grens in de volgorde 1|2, 2|3, 3|4, 4|5, 5|void; 0 = gebruik `transitionWidth`; standaard **[0, 0, 0, 256, 0]**. In de snapshot `transition_width_overrides` (optioneel bij het lezen, altijd geschreven), in `RingLayoutPayload` als var-int-lijst (`PROTOCOL_VERSION` 6 → 7). Java: `RingSettings.transitionWidth(boundary)` geeft de effectieve breedte (0 = 1|2 … 4 = 5|void); `validate()` rekent per grens.
- **Migratie** zoals 37c (golf 0-plan 2026-10-13; 58): **beide** oude standaardsets, pre-37c (1.500 / 3.500 / 5.500 / 7.500 / 9.500, void 10.000) en 37c (1.500 / 3.500 / 6.000 / 7.800 / 9.500, void 10.000), worden — alleen als radii **en** void exact die set zijn — verplaatst naar de nieuwe standaard (radii + void) + opslaan + INFO-log; andere afwijkende waarden blijven (WARN-log). Een config zonder `transitionWidthOverrides` krijgt de standaard [0, 0, 0, 256, 0] van NeoForge bij het laden.
- **Ring 3-code** (58): `VolcanicField.regions` en `VolcanoPlacement.place` lezen de halve zonebreedte per grens (`transitionWidth(1)` voor 2|3, `transitionWidth(2)` voor 3|4) i.p.v. de ene `transitionWidth`; bij de standaard bit-voor-bit gelijk (test in `VolcanicFieldTest`).

## 2. Biomes

| Sleutel | Dimensie | Rol | Eigenaar |
|---|---|---|---|
| `cascade_vale` | overworld | mythisch: watervallenwoud met terrassen, `moonpetal` | W-biomes |
| `crystal_lagoon` | overworld | mythisch: lagune, zandbanken, koraal | W-biomes |
| `lotus_shallows` | overworld | mythisch: ondiep water, reuzenlelies, lotus | W-biomes |
| `glowroot_swamp` | overworld | mythisch: reuzenmangroves, gloeizwammen | W-biomes |
| `mist_fjords` | overworld | mythisch: kliffen en fjorden met mist; grenst aan de bedorven band | W-biomes |
| `drowned_forest` | overworld | bedorven: verdorde moonpetals in diep donker water | W-biomes |
| `peat_moors` | overworld | bedorven: veen, zinkgaten, gifpoelen, moerasgas | W-biomes |
| `reed_marsh` | overworld | bedorven: riet, gifzwammen | W-biomes |
| `sky_gardens` | overworld | hoogte-biome: hoge eilanden boven de mythische band (hemelmeren) | W-biomes (haak: W-terrein) |
| `withered_gardens` | overworld | hoogte-biome: eilanden boven de bedorven band | W-biomes (haak: W-terrein) |
| `sundered_reach` | overworld | overgang 4→5 (`transitions.ring_4`) | W-biomes (terrein: W-terrein, 63) |
| `bone_dunes`, `crystal_spires`, `geode_hollows` | Nether | Nether-ring 4 | W-nether |

- Samen 10 ring 4-biomes + `sundered_reach` (overworld) en 3 Nether-4-biomes. `steam_fens` (zone 3→4) blijft van ring 3.

**Biome-tags** (namen in `ModTags` via 58; inhoud door de eigenaar van de biomes, via datagen):
- `corruptedadventure:is_ring_4`: alle ring 4-overworld-biomes, **inclusief `sundered_reach`** (golf 0-plan 2026-10-13; zoals `steam_fens` in `is_ring_3`).
- `corruptedadventure:is_nether_ring_4`.
- `corruptedadventure:ring_4_blighted` (voorlopig): de bedorven band + `withered_gardens` (lezers: C voor de gifbronnen, A voor gifregen en kleur).
- `corruptedadventure:purifying` (voorlopig): de Nether-4-biomes (zuiverend, lees C).
- `c:`-tags: te kiezen in 64 en 65. Geen `minecraft:has_structure/*` (fase 3).

## 3. Blokken en items (eigenaar B)

Alle blokken krijgen een blockitem met dezelfde naam, tenzij vermeld. Volgorde = creative-tab-volgorde: ring 4 direct na Nether-ring 3, dan Nether-ring 4. **Namen hieronder zijn voorlopig** waar ze geen werktitel uit de sparring zijn.

**66 — natuurblokken**
- `tidestone`-set: `tidestone`, `cobbled_tidestone` (voorlopig), `polished_tidestone`, `tidestone_bricks`, elk met `_stairs`, `_slab`, `_wall` (bij de stenen enkelvoud: `tidestone_brick_*`, patroon `pumice`).
- `moonpetal`-houtset (houtset §2 Ring 2): `moonpetal_log`, `moonpetal_wood`, `stripped_moonpetal_log`, `stripped_moonpetal_wood`, `moonpetal_planks` + stairs, slab, fence, fence_gate, door, trapdoor, pressure_plate, button; `moonpetal_leaves` (bloesem). Verdorde variant `withered_moonpetal_*`. Beslist (golf 0-plan 2026-10-13): **`moonpetal_sapling`** (+ potted) groeit tot een gewone cascadeboom (niet de wereldboom) via de configured feature `corruptedadventure:ring_4/moonpetal_tree` (voorlopig; key van W-biomes, 75; tot dan groeit de sapling niet of met een placeholder); **`withered_moonpetal`** krijgt alleen `withered_moonpetal_log`, `_wood`, `stripped_withered_moonpetal_log`, `_wood` (geen bladeren, geen planken-set); al het hout brandbaar zoals vanilla-hout.
- `reeds` (voorlopig; 2–3 hoog), `peat` (brandstof), `hanging_swamp_moss` (voorlopig), `giant_lily_pad` (voorlopig; 2×2/3×3, beloopbaar: blok of multiblok te bepalen in 66), `lotus` (roze/wit).
- Gloeiflora: `glowcap` (voorlopig; gloeizwam blauw/paars), `glow_bloom` (voorlopig; lichtgevende bloem), `glowing_hanging_roots` (voorlopig). De gifzwammen van `reed_marsh` zijn de **bedorven variant van de gloeizwam** (golf 0-plan 2026-10-13): zelfde vorm en blokklasse als `glowcap`, eigen naam `blighted_glowcap` (voorlopig), ziekelijk groen/grijs met een zwakke gloed; geen gifbron. Lichtwaarden: B (66).
- `bubble_spring` (voorlopig; bellenbron, `animateTick`, geen stroming) — in 72 als het gedrag nodig heeft, anders 66.

**67 — ertsen**
- `tidesilver_ore` + gastgesteente-varianten (voorlopig: `tidestone_tidesilver_ore`, `deepslate_tidesilver_ore`); item `raw_tidesilver`.
- Beslist (golf 0-plan 2026-10-13):
  - **jade** als erts: `jade_ore` + gastgesteente-varianten zoals `tidesilver` (voorlopig: `tidestone_jade_ore`, `deepslate_jade_ore`); item `jade`;
  - **gifkristal** en **aquamarijn** volgens het **amethist-patroon**: kristalblok, budding-blok (groeit via random ticks), 4 groeistadia (small/medium/large bud, cluster) en een scherf. Namen voorlopig: `toxic_crystal_block`, `budding_toxic_crystal`, `small_toxic_crystal_bud`, …, `toxic_crystal_cluster`, `toxic_crystal_shard`; idem `aquamarine_*`. Aquamarijn in geodes onder water, gifkristal in de gifgrotten.
- Items als placeholder zonder recipes (zoals `raw_sunstone`).
- Ertstags `c:ores/<materiaal>`.

**68 — Nether-4**
- `bone_sand` (voorlopig; valt zoals zand), `bleached_netherrack`, fossielsteen-set `fossil_stone`, `polished_fossil_stone`, `fossil_stone_bricks` (+ stairs, slab, wall), zuiverkristal `purity_crystal_block` (voorlopig), `purity_crystal_shard`/cluster, `purity_crystal_lamp`.

**72 — gameplayblokken**
- `swamp_gas` (onzichtbaar, niet-solide, vervangbaar; ketting-ontploffing via scheduled ticks, cap per tick, blokschade volgt `mobGriefing`).
- `sucking_mud` (voorlopig; wegzakken zoals poedersneeuw, server-side).
- `toxic_pool` (voorlopig; gifpoel, herkenbaar anders dan water) als **eigen vloeistof** (golf 0-plan 2026-10-13): `FluidType` + bron/stromend + `LiquidBlock`; client-rendering (`IClientFluidTypeExtensions`) alleen in `client/`. Gedrag (golf 0-plan 2026-10-13): stroomt **traag en kort** zoals lava in de overworld (~3 blokken), zwemmen trager dan in water; emmer `toxic_pool_bucket` (voorlopig, placeholder-item, nut in fase 2); **menging** via NeoForge's `FluidInteractionRegistry`: gif tegen water → `mud`, gif tegen lava → `stone` (water blijft altijd veilig). Worldgen: alleen bronblokken in gesloten kommen, nooit tegen water (geen menging tijdens generatie); de offline scan telt gif tegen lucht en tegen water. Eigenaar B, ook de client-extensie in `client/`; T levert de vloeistoftextures (`toxic_pool_still`, `toxic_pool_flow`, voorlopig); de blok-tag `poison_sources` bevat het `LiquidBlock`.
- `updraft` (voorlopig; opwaartse stroom: niet-solide, doorzichtig, duwt spelers en items omhoog, server-side beweging).
- Sporen: `clawed_tidestone` (voorlopig), `slime_trail` (voorlopig), `creature_nest` (voorlopig).

**Blok-tags** (namen in `ModTags` via 58, inhoud door B):
- `corruptedadventure:poison_sources` (voorlopig): gifpoel (lees C); moerasgas apart (hoofd in gas).
- `corruptedadventure:ring_4_stones` (ertsdoelen ring 4): `tidestone` + te bepalen.
- `corruptedadventure:moonpetal_logs`, `withered_moonpetal_logs` (voorlopig).
- Vanilla-tags waar het past (`#logs`, `#leaves`, `#small_flowers`, …).

**Vanilla-blokken die ring 4 gebruikt:** water, mud, mangrove-hout en -wortels (moerasboom), spar en donkere eik met mos, koraal, zeegras, sea pickles, `bone_block` (Nether-4-fossielen).

## 4. Worldgen-sleutels

**Gedeelde density functions.** Golf 0 (59) maakt van elke DF in deze tabel een stub (constante 0). Alleen de eigenaar vult hem in. Namen **vastgezet in 59** (2026-10-13). Uitzondering: `ring_4/nether/terrain` is een verwijzing naar de placeholder `ring_4/nether` (constante 0 zou Nether-4 leeg maken) en zit al in `ring_blend/nether`.

| DF (`corruptedadventure:…`) | Betekenis / bereik | Eigenaar | Lezers |
|---|---|---|---|
| `ring_4/overworld/band` | 2D band-as: mythisch ↔ bedorven (binnenste ~60 % / buitenste ~40 %), oplopend naar ring 5; −1 mythisch … 1 bedorven, 0 = midden van de overgang. Vastgezet (59): flat_cache van type `corruptedadventure:ring_4_band` (`Ring4BandFunction`, geen parameters), dat alleen `Ring4Band` aanroept. Ruis: amplitude 22,5 % van de ringbreedte (~450), 2 octaven (~700 / ~180). Waarde = getekende afstand / (`SLOPE` 2,5 · 32): de ruis maakt de afstand dwars op de lijn ~2,5× zo steil als radiaal, zo blijft de overgang ~64 blokken; daardoor puur mythisch tot ~33 % en puur bedorven vanaf ~87 % van de ring, gemiddeld 60/40 (akkoord gebruiker 2026-10-13). **Eigen as** (golf 0-plan 2026-10-13): geseede 2D-ruis op een radiaal verloop, zodat bedorven tongen de mythische band in steken en er mythische eilandjes in de bedorven band liggen; het verloop houdt de binnenrand mythisch en de buitenrand bedorven | 59 (W-terrein) | router (klimaat), W-biomes, W-terrein (eilanddichtheid), A en C via de Java-tegenhanger (zie onder) |
| `ring_4/overworld/region_water` | 2D klimaatas (open water ↔ land), flat_cache van `noise/ring_4_water` (firstOctave −10, [1, 0,5, 0,25]) | 59 (W-terrein) | router `temperature`, W-biomes |
| `ring_4/overworld/region_growth` | 2D klimaatas (begroeid ↔ kaal/rots), flat_cache van `noise/ring_4_growth` (idem) | 59 (W-terrein) | router `vegetation`, W-biomes |
| `ring_4/overworld/initial_height` / `initial_terrain` | `initial_terrain` = 0,125 · `cache_2d(initial_height)` + gradiënt (nul op Y = hoogte), zoals ring 3; `initial_height` = tot 60 precies het placeholder-oppervlak (64 + 8 · `placeholder_terrain`) | 59 → W-terrein (60: echte hoogte) | router `initial_density_without_jaggedness` |
| `ring_4/overworld/islands` | het `IslandField` (type `corruptedadventure:island_field`, hash-noise `ring_4_island_cells`); nog niet in de final density (61) | W-terrein | via `IslandFields` / `RingChunkGenerator.islandField` |
| `ring_4/overworld/height` | 2D oppervlaktehoogte (zonder gaten en grotten) | W-terrein | W-biomes (features, surface) |
| `ring_4/overworld/island_mask` | 0..1: eiland in de kolom | W-terrein | W-biomes (aankleding), hoogte-biomehaak |
| `ring_4/overworld/island_hole` | 0..1: gatmeer onder een eiland (1 = bodem) | W-terrein | W-biomes |
| `ring_4/overworld/cliff` | 0..1: klif 3→4. **Klif aan de buitenrand van zone 3→4** (golf 0-plan 2026-10-13): `steam_fens` is een plateau op ring 3-hoogte over de hele zone, de klif valt aan de ring 4-kant (~6.064 + grensruis), de voet is echte ring 4 | W-terrein (62) | W-biomes (`steam_fens`, surface) |
| `ring_4/overworld/sundered` | 0..1: uiteenvallen in zone 4→5 | W-terrein (63) | W-biomes, A (sfeer) |
| `ring_4/overworld/tunnels` | per blok: holte van het tunnelnet (overstroomd/droog) | W-terrein (73) | W-biomes (sporen, ertsen) |
| `ring_4/nether/region` | 2D regio-as Nether-4 | 59 (W-nether) | Nether-router `temperature` |
| `ring_4/nether/terrain` | terreinvorm Nether-4 (als ring 4 in `ring_blend/nether`) | W-nether | — |

- **Band in Java** (golf 0-plan 2026-10-13, gevolg van de eigen as): weer (A) en gif (C) moeten server-side weten in welke band een positie ligt. 59 levert naast de DF een pure, geseede Java-functie die exact dezelfde waarde geeft (één bron, test: DF = Java op een raster). **Vastgezet (59):** `nl.timtoys.corruptedadventure.ring.Ring4Band`: `Ring4Band.of(layout).value(x, z)` (∈ [−1, 1]) en `.blighted(x, z)` (waarde ≥ 0 = bedorven; de grens voor weer en gif), overworld-coördinaten. Geseed op `RingLayout.noiseSeed()` (niet de wereldseed): ook de client kan hem met de gesyncte layout berekenen (A: weer per band tekenen), zonder payload. Alle getallen staan alleen in `Ring4Band`. **Overgang tussen de banden ~64 blokken** (golf 0-plan 2026-10-13): in die strook mengen de randbiomes; weer en gif nemen het midden van de strook als grens.
- **Router:** de ring 4-sleutels in `ring_blend/overworld_temperature`, `_vegetation` en (zo nodig) `_weirdness` wijzen naar de assen van 59. **Vastgezet (59):** `temperature` → `region_water`, `vegetation` → `region_growth`, `weirdness` → `band`; `overworld_erosion` ring_4 = 0 (geen deep-dark-route van de aquifer); floodedness blijft vanilla tot 73. Nether: `nether_temperature` ring_4 → `ring_4/nether/region` (flat_cache van `nether_ring_4_region`, −9). Zone 4→5 krijgt een overgangsbiome (`transitions.ring_4` = `sundered_reach`), dus `ring_4` gaat in dezelfde change uit `switch_at` (harde regel uit `docs/plans/ring-3/contract.md` §4; de test vergelijkt met `transitions`). Zone 3→4 heeft al `steam_fens`.
- **`IslandField`** (Java, W-terrein): eigen veldklasse die `Column`/`ColumnTable`/`VolcanicCaches` van ring 3 hergebruikt, met een eigen raster (dichte eilanden botsen met de kolom-caps van ring 3: max. 6 eilanden / 8 solids per kolom), plaatsing zonder `flat_cache`, het gat als aftrek in de 2D-hoogte (zoals `sinkholeAt`). Accessors (alleen lezen; voorlopig): `islandsWithin`, `islandAt`, `liftsWithin`, `waterfallsWithin`, `worldTrees`; lezers W-biomes (75–77), B/A waar nodig. Gevonden via de router zoals `VolcanicField.find`. **Skelet (59):** `worldgen.densityfunction.IslandField` met eigen `Column` (eilandmasker, watervalbereik `fallTop`/`fallBottom`/`fallFlowBelow`), `ColumnTable` en eigen `Decisions` (per cel een lijst eilanden; plaatsing `placeCell` = 61), `Settings.cellSize` (128 voorlopig); `islandAt(x, z)`, `islandsAt(x, z)` (alle eilanden over de kolom), `islandsWithin(minX, minZ, maxX, maxZ)`, `liftsWithin`, `waterfallsWithin`, `worldTrees` (zelfde vakparameters) met records `Island(x, z, radius, bottom, top)`, `Lift`, `Waterfall`, `WorldTree`; `IslandField.find(root)`. **Eén overlapregel** (review 59): `IslandField.uppermost(eilanden)` kiest bij overlap het eiland met de hoogste onderkant, dan de hoogste top, dan de grootste x en z van het midden (onafhankelijk van de volgorde); `islandAt` en de hoogte-biomehaak gebruiken die. Gevonden door `RingChunkGenerator.islandField(random)` en, voor lezers zonder random state, `worldgen.chunk.IslandFields.current(dimension)` (gevuld bij `LevelEvent.Load`). Nog niet in de final density: 61 sluit het aan.
- **Vloeistofregel ring 4** (W-terrein, `RingFluidPicker`/`RingAquifer`):
  - zee-water op Y 63 (bestaat al in de overworld);
  - **geen lava**, ook niet diep of onder eilanden (vanilla zet onder Y −10 lava; ring 4 niet);
  - een **water-tak** voor de vastgelegde watervallen (eilanden, klif 3→4, wereldboom), naast de lavavallen van ring 3; vallend water bij generatie met een cap per chunk;
  - gatmeren tot zeeniveau of tot de rand op hooglanden; overstroomde tunnels via een eigen floodedness (en erosion: geen deep dark, zoals ring 3);
  - een echte `initial_terrain` (valkuil Y 55: overstroomde grotten);
  - `ring3Seal` blijft in ring 3's helft van zone 3→4;
  - afgrond van `sundered_reach` (golf 0-plan 2026-10-13): een stilstaande **afgrondzee op Y 0**; water dat de diepte in stort zijn **alleen vastgelegde vallen** (water-tak, cap per chunk); verder nergens vrij water tegen lucht (de waterscan van `waves.md` §4 telt dit, buiten de vastgelegde vallen). De regel hoort bij 63 (geen haak in 59).
  - **Gebouwd in 59 (besluiten 2026-10-13):** lokale aquiferlava wordt water boven Y −54 met een stenen lid op −54 (`RingFluidPicker.waterOnlyFrom`, zoals ring 2), in ring 4 **en over de hele zone 4→5** (ook de ring 5-helft, review 59); **ring 4 is droog onder Y −54** (`ring4DryBelow`: open ruimte = lucht, geen lavalaag); **dammen van gesteente onder Y −54** (`ring4DamBelow`) over de hele zone 4→5 (beide helften; ring 5 houdt zijn lavalaag pas daarbuiten) en over de ring 4-helft van zone 3→4 (de ring 3-helft heeft `ring3Seal`); **carver-lava** (vanilla zet die op Y ≤ −56 zonder de aquifer te vragen) beslist in een ringwereld `RingAquifer.carverLava` via de mixin `WorldCarverLavaMixin` op `WorldCarver.getCarveState` (`CarverLava`): in ring 4 en de dammen het antwoord van de aquifer (droge bodem lucht, dam niet snijden); elders grijpt de mixin niet in en loopt vanilla door (ring 3 bitgelijk). Ring 4-biomes (64) mogen dus vanilla-carvers gebruiken. Water-tak: `RingAquifer` zet vallend water uit `IslandField.Column` (nu leeg). Alle regels schakelen op `ring()`.
  - Naad water/lava: niet op de middellijn 4|5 (de hele zone heeft water), maar aan de buitenrand van zone 4→5, binnen ring 5; de vloeistofregel van ring 5 (81–99) lost die op.
- **Hoogte-biomehaak** (59, W-terrein, in `RingBiomeSource`): boven een drempelhoogte op een eiland kiest ring 4 `sky_gardens` (mythische band) of `withered_gardens` (bedorven band); lage eilanden en de grond houden het grondbiome. Beslist (golf 0-plan 2026-10-13): **per eiland**, het hele eiland krijgt het hoogte-biome als de **top op Y 140 of hoger** ligt (nooit twee biomes boven elkaar op één eiland); welk van de twee volgt uit de band op het midden van het eiland. De haak ziet het eiland **via `IslandField`**, gevonden via de router zoals `VolcanicField.find` (één bron voor de eilandvorm, geen klimaat-as). **Vastgezet (59):** optioneel codec-veld `island_biomes` van `RingBiomeSource` (`{mythic, blighted, min_top: 140}`, record `IslandBiomes`); in de preset tot 64 beide op `placeholder_ring_4`, 64 zet `sky_gardens`/`withered_gardens` (het veld is van 59, de ids van 64). Regel: alleen quarts die ring 4 raken; op het midden van de quart tellen alleen de eilanden van de kolom waar het blok niet onder ligt (Y ≥ onderkant − 4), met top ≥ `min_top` en het **midden van het eiland** in ring 4; daarvan kiest `IslandField.uppermost` (review 59: zo houdt een lager eiland onder een overlappend hoger eiland zijn eigen hoogte-biome, ook als het hogere niet telt). Dan geldt het hoogte-biome over de hele voetafdruk, ook boven overgangszones, vanaf 4 blokken onder de onderkant tot de bouwtop; de band op het midden kiest (`Ring4Band.blighted`).
- **Nether-4 lavaniveau** (`fluid_levels.ring_4`, W-nether): de lavazee is "diep weggezakt": **Y 10** (golf 0-plan 2026-10-13; 12 onder Nether-3). De trede 3|4 (Nether-3 Y 22) moet afgedekt zijn volgens de dam-regel van `docs/plans/ring-3/contract.md` §4 (test met `tools/nether-lava-walls.py` op r 1.500 en r 2.000).
- **Vanilla-ertsen** (77, golf 0-plan 2026-10-13): gewoon (kolen, ijzer, koper) ×0,2, middengroep (goud, redstone, lapis) **×0,15**, zeldzaam (diamant, smaragd) ×0,1.
- **Features:** keys `corruptedadventure:ring_4/<naam>` en `corruptedadventure:nether_ring_4/<naam>`. De determinismeregels van 23c (overworld) en 35 (Nether) gelden. Grote vormen (eilanden, gaten, wereldboom, landbruggen, klif) zijn terrein (DF/veld), geen features.
- **Hoogtes (besluiten):** basisterrein rond Y 63 met terrassen en kliffen; klif 3→4 ~40–80; eilanden binnen Y ~90–130 (15–40 boven het gat), naar buiten tot Y ~250; kluiten 5–15, middel 20–60, reuzen 80–150; wereldboom 80–140 hoog, stam 8–12; tunnels 5–12 breed.
- **Spawns:** vanilla-dieren per biome (lagune tropische vissen/schildpadden/dolfijnen, moeras kikkers/axolotls, cascades vossen/konijnen; bedorven band vrijwel leeg). Monsters vanilla tot fase 4.
- **Prestatie:** noise ≤ 1,75× vanilla.

## 5. Condities (eigenaar C)

| Sleutel | Waarde |
|---|---|
| `Condition.POISON` (naam `poison`, voorlopig), HUD-kleur gifgroen | — |
| zone | overworld, ring 4 (hard op 0,5 zoals hitte); daarbuiten ↓ |
| bronnen | gifpoel (in of naast, snel; blok-tag `poison_sources`), moerasgas (hoofd in gas), gifregen (onder open lucht; modifier door C, factor in `[weather.ring4]`) |
| geen bron | water (ook gifgroen), gifzwammen |
| drempels | misselijkheid → vanilla-gif (doodt niet) → wither (kan doden); startwaarden (golf 0-plan 2026-10-13) **30 / 60 / 90** |
| noodmiddelen | melk −40, honingfles −20 |
| `[conditions.poison]` | startwaarden (golf 0-plan 2026-10-13, afstemmen in 5b): gifpoel in/naast **+3/s** (~35 s tot 100), moerasgas hoofd in gas **+2/s** (~50 s), gifregen onder open lucht **+0,5/s** (~3,5 min, zoals hitte); daling buiten de bronnen **−1/s** |
| zuiverend (79) | `RateModifier.Stage` zuiverend (bestaat): in Nether-4 (`purifying`) zakken alle meters extra snel; factor in de config, startwaarde **×3** op de daling (golf 0-plan 2026-10-13) |

## 6. Weer, geluid, particles (eigenaar A)

- **Weertypes** (`RingWeatherType`): `DENSE_FOG`, `TOXIC_RAIN` (bestaan al, ring 4), **`STORM_WIND`** (nieuw, voorlopig).
  - Mythische band: dikke mist + stormwind. Bedorven band: gifregen (alleen daar).
  - Band-regel (golf 0-plan 2026-10-13): **één cyclus voor ring 4**; het type bepaalt de band: mist en storm gelden alleen in de mythische band, gifregen alleen in de bedorven band (via de Java-bandfunctie van 59, §4; grens = midden van de overgangsstrook). Geen wijziging aan `RingWeatherData` of de payload. De banden hebben dus nooit tegelijk weer.
  - **Geen mist of storm in de bedorven band** (golf 0-plan 2026-10-13): daar alleen gifregen.
- **Config `[weather.ring4]`** (voorlopig): `clearMinSeconds`, `clearMaxSeconds`, `weatherMinSeconds`, `weatherMaxSeconds`, `denseFogWeight`, `stormWindWeight`, `toxicRainWeight`, `toxicRainPoisonMultiplier`, `stormPushStrength`, `stormPushMinY` (120).
- **Stormduw:** server-side, alleen boven Y ~120 op en tussen de eilanden (patroon zandstorm, change 28); de richting uit de `seed` van de weerstaat.
- **Mist-hook voor fase 4:** server-side vraag "is het nu mist op deze plek" via `RingWeather.stateAt` (geen client-logica); vorm te bepalen in 70.
- **Payloads:** geen nieuwe gepland (de weerstaat wordt al gesynct). Is er toch een nodig, dan main melden; INT verhoogt `ModNetwork.PROTOCOL_VERSION` één keer per golf.
- **Sound events** (voorlopig; audio uit een ring 4-synthesescript zoals `Ring3Sounds.java`): `weather.ring_4.dense_fog`, `weather.ring_4.storm_wind`, `weather.ring_4.toxic_rain`; ambient `ambient.ring_4.water`, `ambient.ring_4.night_choir`, `ambient.ring_4.reeds`, `ambient.ring_4.bubbles`, `ambient.ring_4.sundered_drone`, `ambient.nether_ring_4.wind`, `ambient.nether_ring_4.crystal_hum`; blokken `block.swamp_gas.ignite`, `block.bubble_spring.bubble`, `block.updraft.flow`.
- **Particle types** (voorlopig; sprites van T): `will_o_wisp`, `blossom`, `firefly`, `swamp_gas`, `toxic_bubble`, `updraft_bubble`, `glitch_speck`, `bone_dust`.
- **Client-only spanning:** geritsel in het riet en het silhouet onder water per speler willekeurig, niet gesynct (zoals het gerommel van ring 3). Pulserende fog Nether-4 via een fog-hook in `client/`.

## 7. Textures (eigenaar T)

- Script `tools/textures/Ring4Textures.java` (37b-stijl). Bestandsnamen = blok-/itemnaam; zijtexturen volgens het patroon van ring 2/3.
- Paletrichting (besluit): `tidestone` blauwgrijs gelaagd; `moonpetal` wit/zilveren stam, roze-lila kruin, `withered_moonpetal` grijs; gloeiflora blauw/paars. Gloeiende blokken (gloeizwam, gloeibloem, hangwortels) met de gloed in twee lagen (gewone texture + emissive overlay), lichtwaarden van B.
- B verwijst in de models naar deze paden; ontbreekt een PNG bij `runData`, dan een tijdelijke, niet-gecommitte kopie (zoals ring 3).

## 8. Lang en haken

- Helpers per stroom (58): `datagen/lang/Ring4BlockNames` (B), `Ring4BiomeNames` (W-biomes), `NetherRing4BiomeNames` (W-nether), `Ring4ConditionText` (C: meter, doodsbericht), `Ring4WeatherText` (A: ondertitels). Engels verplicht, Nederlands erbij.
- Lege haken (58) die `DataGenerators` en de providers al aanroepen: W-biomes `Ring4Biomes`, `Ring4BiomeModifiers`, `Ring4Features`, `Ring4Carvers`, `Ring4BiomeTags`; W-nether `NetherRing4Biomes`, `NetherRing4BiomeModifiers`, `NetherRing4Features`, `NetherRing4Carvers`, `NetherRing4BiomeTags`; C `Ring4DamageTypes` (als gif een eigen damage type krijgt).

---

## 9. Open punten — te bepalen in het golf 0-plan

1. ~~**Void-rand**~~ — beslist 2026-10-13: 10.200, migratie zoals 37c (§1).
2. ~~**Zone 4→5**~~ — beslist 2026-10-13: overgangsbreedte per grens in de config (4→5 = 256), Nether ÷4 = 64 (§1).
3. ~~**Bandgrens mythisch/bedorven**~~ — beslist 2026-10-13: eigen as, overgang ~64 (§4).
4. ~~**Hoogte-biomes**~~ — beslist 2026-10-13: per eiland, top ≥ Y 140, via `IslandField` (§4).
5. ~~**Klif 3→4**~~ — beslist 2026-10-13: aan de buitenrand van de zone, `steam_fens` als plateau erboven (§4).
6. ~~**Afgrond `sundered_reach`**~~ — beslist 2026-10-13: afgrondzee Y 0, alleen vastgelegde vallen (§4).
7. ~~**Nether-4 lavaniveau**~~ — beslist 2026-10-13: Y 10; trede 3|4 met de dam-regel (§4).
8. ~~**Ringweer per band**~~ — beslist 2026-10-13: één cyclus, type bepaalt de band; bedorven band alleen gifregen (§6).
9. ~~**Gif**~~ — beslist 2026-10-13: "midden" (§5).
10. ~~**Blokken**~~ — beslist 2026-10-13: gifzwam = bedorven variant van de gloeizwam, gifpoel = eigen vloeistof (traag en kort, emmer, water → modder, lava → steen), `moonpetal`-sapling ja en verdord alleen stam, jade erts, gifkristal en aquamarijn kristal (§3).
11. ~~**Vanilla-ertsen**~~ — beslist 2026-10-13: middengroep ×0,15 (§4).
12. ~~**`is_ring_4`**~~ — beslist 2026-10-13: `sundered_reach` hoort erbij (§2).
13. ~~**Bouwplannummers**~~ — beslist 2026-10-13: blok G (ring 5) hernummerd naar 81–99, H naar 100 (`fase-1-wereld.md` §3).
