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
| `voidOuterRadius` | 10000 | **~10200** (exacte waarde: te bepalen in het golf 0-plan) |
| `boundaryNoise`, `transitionWidth` | 96, 128 | ongewijzigd; de bredere zone 4→5 (~256): zie hieronder |

- Nether ÷4: Nether-4 1.500 – 2.000, Nether-5 2.000 – 2.425, void-rand ~2.550. World border = void-rand (overworld ~10.200, Nether ~2.550).
- `/ca tpring` (snapshot): midden ring 4 = 7.000, ring 5 = 8.850, void ~9.950 (Nether ÷4).
- Geldt alleen voor **nieuwe** werelden. Alle ring 4-tests gebruiken een verse wereld (seed `20260930`, preset `corrupted_world`).
- **Zone 4→5 ~256 breed** (besluit). *Te bepalen in het golf 0-plan:* een overgangsbreedte per grens in de config (nieuwe sleutel, ook in de snapshot en de sync) of een zonevorm binnen de ringinvoeren met de gewone zone van 128; de Nether-breedte (÷4 ≈ 64).
- *Te bepalen in het golf 0-plan:* migratie van de config-standaard zoals 37c (alleen de exacte oude standaard verplaatsen).

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
- `corruptedadventure:is_ring_4`: alle ring 4-overworld-biomes. *Te bepalen in het golf 0-plan:* of `sundered_reach` erin hoort (zoals `steam_fens` in `is_ring_3`).
- `corruptedadventure:is_nether_ring_4`.
- `corruptedadventure:ring_4_blighted` (voorlopig): de bedorven band + `withered_gardens` (lezers: C voor de gifbronnen, A voor gifregen en kleur).
- `corruptedadventure:purifying` (voorlopig): de Nether-4-biomes (zuiverend, lees C).
- `c:`-tags: te kiezen in 64 en 65. Geen `minecraft:has_structure/*` (fase 3).

## 3. Blokken en items (eigenaar B)

Alle blokken krijgen een blockitem met dezelfde naam, tenzij vermeld. Volgorde = creative-tab-volgorde: ring 4 direct na Nether-ring 3, dan Nether-ring 4. **Namen hieronder zijn voorlopig** waar ze geen werktitel uit de sparring zijn.

**66 — natuurblokken**
- `tidestone`-set: `tidestone`, `cobbled_tidestone` (voorlopig), `polished_tidestone`, `tidestone_bricks`, elk met `_stairs`, `_slab`, `_wall` (bij de stenen enkelvoud: `tidestone_brick_*`, patroon `pumice`).
- `moonpetal`-houtset (houtset §2 Ring 2): `moonpetal_log`, `moonpetal_wood`, `stripped_moonpetal_log`, `stripped_moonpetal_wood`, `moonpetal_planks` + stairs, slab, fence, fence_gate, door, trapdoor, pressure_plate, button; `moonpetal_leaves` (bloesem). Verdorde variant `withered_moonpetal_*`. *Te bepalen in het golf 0-plan:* sapling, welke delen de verdorde variant krijgt, brandbaarheid.
- `reeds` (voorlopig; 2–3 hoog), `peat` (brandstof), `hanging_swamp_moss` (voorlopig), `giant_lily_pad` (voorlopig; 2×2/3×3, beloopbaar: blok of multiblok te bepalen in 66), `lotus` (roze/wit).
- Gloeiflora: `glowcap` (voorlopig; gloeizwam blauw/paars), `glow_bloom` (voorlopig; lichtgevende bloem), `glowing_hanging_roots` (voorlopig). *Te bepalen in het golf 0-plan:* de gifzwammen van `reed_marsh` als eigen blok of als bedorven variant van de gloeizwam; lichtwaarden.
- `bubble_spring` (voorlopig; bellenbron, `animateTick`, geen stroming) — in 72 als het gedrag nodig heeft, anders 66.

**67 — ertsen**
- `tidesilver_ore` + gastgesteente-varianten (voorlopig: `tidestone_tidesilver_ore`, `deepslate_tidesilver_ore`); item `raw_tidesilver`.
- Gifkristal, jade, aquamarijn: erts of kristal/geode (amethist-patroon)? *Te bepalen in het golf 0-plan* per materiaal (aquamarijn ligt in geodes onder water, gifkristal "groeit" in gifgrotten). Items als placeholder zonder recipes (zoals `raw_sunstone`).
- Ertstags `c:ores/<materiaal>`.

**68 — Nether-4**
- `bone_sand` (voorlopig; valt zoals zand), `bleached_netherrack`, fossielsteen-set `fossil_stone`, `polished_fossil_stone`, `fossil_stone_bricks` (+ stairs, slab, wall), zuiverkristal `purity_crystal_block` (voorlopig), `purity_crystal_shard`/cluster, `purity_crystal_lamp`.

**72 — gameplayblokken**
- `swamp_gas` (onzichtbaar, niet-solide, vervangbaar; ketting-ontploffing via scheduled ticks, cap per tick, blokschade volgt `mobGriefing`).
- `sucking_mud` (voorlopig; wegzakken zoals poedersneeuw, server-side).
- `toxic_pool` (voorlopig; gifpoel, herkenbaar anders dan water). *Te bepalen in het golf 0-plan:* een blok (zoals kokende modder) of een eigen vloeistof.
- `updraft` (voorlopig; opwaartse stroom: niet-solide, doorzichtig, duwt spelers en items omhoog, server-side beweging).
- Sporen: `clawed_tidestone` (voorlopig), `slime_trail` (voorlopig), `creature_nest` (voorlopig).

**Blok-tags** (namen in `ModTags` via 58, inhoud door B):
- `corruptedadventure:poison_sources` (voorlopig): gifpoel (lees C); moerasgas apart (hoofd in gas).
- `corruptedadventure:ring_4_stones` (ertsdoelen ring 4): `tidestone` + te bepalen.
- `corruptedadventure:moonpetal_logs`, `withered_moonpetal_logs` (voorlopig).
- Vanilla-tags waar het past (`#logs`, `#leaves`, `#small_flowers`, …).

**Vanilla-blokken die ring 4 gebruikt:** water, mud, mangrove-hout en -wortels (moerasboom), spar en donkere eik met mos, koraal, zeegras, sea pickles, `bone_block` (Nether-4-fossielen).

## 4. Worldgen-sleutels

**Gedeelde density functions.** Golf 0 (59) maakt van elke DF in deze tabel een stub (constante 0). Alleen de eigenaar vult hem in. Namen voorlopig; de lijst wordt in het golf 0-plan vastgezet.

| DF (`corruptedadventure:…`) | Betekenis / bereik | Eigenaar | Lezers |
|---|---|---|---|
| `ring_4/overworld/band` | 2D band-as: mythisch ↔ bedorven (binnenste ~60 % / buitenste ~40 %), oplopend naar ring 5 | 59 (W-terrein) | router (klimaat), W-biomes, W-terrein (eilanddichtheid) |
| `ring_4/overworld/region_*` | 2D klimaatassen voor de biomes binnen een band (aantal en namen: 59) | 59 (W-terrein) | router `temperature`/`vegetation`, W-biomes |
| `ring_4/overworld/height` | 2D oppervlaktehoogte (zonder gaten en grotten) | W-terrein | W-biomes (features, surface) |
| `ring_4/overworld/island_mask` | 0..1: eiland in de kolom | W-terrein | W-biomes (aankleding), hoogte-biomehaak |
| `ring_4/overworld/island_hole` | 0..1: gatmeer onder een eiland (1 = bodem) | W-terrein | W-biomes |
| `ring_4/overworld/cliff` | 0..1: klif 3→4 | W-terrein (62) | W-biomes (`steam_fens`, surface) |
| `ring_4/overworld/sundered` | 0..1: uiteenvallen in zone 4→5 | W-terrein (63) | W-biomes, A (sfeer) |
| `ring_4/overworld/tunnels` | per blok: holte van het tunnelnet (overstroomd/droog) | W-terrein (73) | W-biomes (sporen, ertsen) |
| `ring_4/nether/region` | 2D regio-as Nether-4 | 59 (W-nether) | Nether-router `temperature` |
| `ring_4/nether/terrain` | terreinvorm Nether-4 (als ring 4 in `ring_blend/nether`) | W-nether | — |

- **Router:** de ring 4-sleutels in `ring_blend/overworld_temperature`, `_vegetation` en (zo nodig) `_weirdness` wijzen naar de assen van 59. Zone 4→5 krijgt een overgangsbiome (`transitions.ring_4` = `sundered_reach`), dus `ring_4` gaat in dezelfde change uit `switch_at` (harde regel uit `docs/plans/ring-3/contract.md` §4; de test vergelijkt met `transitions`). Zone 3→4 heeft al `steam_fens`.
- **`IslandField`** (Java, W-terrein): eigen veldklasse die `Column`/`ColumnTable`/`VolcanicCaches` van ring 3 hergebruikt, met een eigen raster (dichte eilanden botsen met de kolom-caps van ring 3: max. 6 eilanden / 8 solids per kolom), plaatsing zonder `flat_cache`, het gat als aftrek in de 2D-hoogte (zoals `sinkholeAt`). Accessors (alleen lezen; voorlopig): `islandsWithin`, `islandAt`, `liftsWithin`, `waterfallsWithin`, `worldTrees`; lezers W-biomes (75–77), B/A waar nodig. Gevonden via de router zoals `VolcanicField.find`.
- **Vloeistofregel ring 4** (W-terrein, `RingFluidPicker`/`RingAquifer`):
  - zee-water op Y 63 (bestaat al in de overworld);
  - **geen lava**, ook niet diep of onder eilanden (vanilla zet onder Y −10 lava; ring 4 niet);
  - een **water-tak** voor de vastgelegde watervallen (eilanden, klif 3→4, wereldboom), naast de lavavallen van ring 3; vallend water bij generatie met een cap per chunk;
  - gatmeren tot zeeniveau of tot de rand op hooglanden; overstroomde tunnels via een eigen floodedness (en erosion: geen deep dark, zoals ring 3);
  - een echte `initial_terrain` (valkuil Y 55: overstroomde grotten);
  - `ring3Seal` blijft in ring 3's helft van zone 3→4;
  - *te bepalen in het golf 0-plan:* het waterniveau in de afgrond van `sundered_reach` en of water daar tegen lucht mag staan (de afgrond is open).
- **Hoogte-biomehaak** (59, W-terrein, in `RingBiomeSource`): boven een drempelhoogte op een eiland kiest ring 4 `sky_gardens` (mythische band) of `withered_gardens` (bedorven band); lage eilanden en de grond houden het grondbiome. Een biome-source krijgt alleen de klimaatsampler (`fase-1-wereld.md` §1.2): *te bepalen in het golf 0-plan* via welk routerveld of welke eigen DF de haak het eiland en de hoogte ziet, en de drempel.
- **Nether-4 lavaniveau** (`fluid_levels.ring_4`, W-nether): de lavazee is "diep weggezakt". *Te bepalen in het golf 0-plan:* het niveau. De trede 3|4 (Nether-3 Y 22) moet afgedekt zijn volgens de dam-regel van `docs/plans/ring-3/contract.md` §4 (test met `tools/nether-lava-walls.py` op r 1.500 en r 2.000).
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
| drempels | misselijkheid → vanilla-gif (doodt niet) → wither (kan doden); waarden te bepalen in het golf 0-plan |
| noodmiddelen | melk −40, honingfles −20 |
| `[conditions.poison]` | snelheden per bron en de daling: startwaarden te bepalen in het golf 0-plan, afstemmen in 5b |
| zuiverend (79) | `RateModifier.Stage` zuiverend (bestaat): in Nether-4 (`purifying`) zakken alle meters extra snel; factor in de config (te bepalen) |

## 6. Weer, geluid, particles (eigenaar A)

- **Weertypes** (`RingWeatherType`): `DENSE_FOG`, `TOXIC_RAIN` (bestaan al, ring 4), **`STORM_WIND`** (nieuw, voorlopig).
  - Mythische band: dikke mist + stormwind. Bedorven band: gifregen (alleen daar).
  - *Te bepalen in het golf 0-plan:* hoe de band-regel werkt (de weerstaat is per ring: één cyclus met een ruimtelijke beperking per band, of twee cycli) en of mist/storm ook in de bedorven band komen.
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

1. **Void-rand:** exacte standaardwaarde (~10.200) en migratie van de config-standaard (§1).
2. **Zone 4→5 (~256):** een overgangsbreedte per grens in de config of een zonevorm in de ringinvoeren; Nether-breedte (§1).
3. **Bandgrens mythisch/bedorven:** vaste radius met ruis of een eigen as; breedte van de overgang tussen de banden (§4).
4. **Hoogte-biomes:** drempelhoogte en hoe de haak eiland en hoogte ziet (§4).
5. **Klif 3→4:** waar in de zone de klif ligt en hoe `steam_fens` zich tot de klifvoet verhoudt (62).
6. **Afgrond `sundered_reach`:** waterniveau onderin en water tegen lucht (§4).
7. **Nether-4 lavaniveau** en de trede 3|4 (§4).
8. **Ringweer per band:** mechanisme en of mist/storm ook in de bedorven band komen (§6).
9. **Gif:** snelheden per bron, daling en drempelwaarden (§5).
10. **Blokken:** gifzwam (eigen blok of variant van de gloeizwam), gifpoel (blok of vloeistof), `moonpetal`-sapling en de delen van de verdorde variant, gifkristal/jade/aquamarijn als erts of kristal (§3).
11. **Vanilla-ertsen:** de factor voor de middengroep (goud/redstone/lapis); startwaarden gewoon ×0,2, zeldzaam ×0,1.
12. **`is_ring_4`:** hoort `sundered_reach` erbij (§2).
13. **Bouwplannummers:** 77–80 overlappen met blok G (ring 5); hernummering van G.
