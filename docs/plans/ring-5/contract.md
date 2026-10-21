# Ring 5 — contract (namen, sleutels, eigenaarschap)

Scope: ring 5 + Nether-ring 5 + de herziening van zone 4→5 (bouwplan blok G, herzien in `waves.md` §8). Opgesteld
2026-10-21 uit de sparring ring 5 (`docs/plans/ring-5-sparring.md`, rondes 1–5; `fase-1-wereld.md` → Ring 5, blok
"Sparring ring 5") en de code op `6e1bffb`.

**Regels**
- Alles hier staat **vast** zodra golf 0 gecommit is. Tot dan vult het golf 0-plan het aan met de antwoorden op de open
  punten (`golf-0.md` §6, "V1"–"V12"); die zijn hier gemarkeerd als **→ Vn**. Daarna: een stroom die iets anders of extra
  nodig heeft, vraagt het via main; main beslist (of vraagt de gebruiker), past dit bestand aan en meldt het aan alle
  stromen. Nooit in andermans bestand werken.
- Namen zijn registry-namen onder `corruptedadventure:` (Engels, snake_case, ring-thema herkenbaar). De werktitels uit
  de sparring (`corrupted_wastes`, `glitch_fields`, `false_meadow`, `void_scars`, `inverted_hills`, `white_halls`,
  `hollow_cathedral`) staan vast; `shattered_expanse` is een werktitel (→ V12). **"(voorlopig)"** = een voorstel dat
  vastligt tenzij de korte vragenronde aan het begin van golf 1 iets anders besluit (zoals ring 4, 2026-10-13).
- Getallen die de vorm bepalen staan in **één Java-klasse per veld** (zoals `Ring4Band`); de ringgrenzen alleen in de
  config (CLAUDE.md §6). Aandelen van de ring rekenen op de radii van de snapshot.

---

## 1. Ringgrenzen en zones (config; geen wijziging tenzij V2/V7)

| Sleutel (COMMON `corruptedadventure-common.toml`) | Nu | Ring 5 |
|---|---|---|
| `ring4OuterRadius` / `ring5OuterRadius` / `voidOuterRadius` | 8000 / 9700 / 10200 | ongewijzigd |
| `transitionWidth` | 128 | ongewijzigd |
| `transitionWidthOverrides` (1\|2, 2\|3, 3\|4, 4\|5, 5\|void) | [0, 0, 0, 256, 0] | 4\|5 → **V7**; 5\|void → **V2** |

- Ring 5 nominaal 8.000 – 9.700 (1.700 breed). Met de standaardzones: zone 4→5 7.872 – 8.128, zone 5→void
  9.636 – 9.764, border 10.200. Nether ÷4: Nether-5 2.000 – 2.425, zone 4→5 1.968 – 2.032, border ~2.550.
- Wijzigt een standaard (V2/V7), dan migreert 81 alleen de exacte oude standaardlijst [0, 0, 0, 256, 0] (zoals 58),
  alleen voor nieuwe werelden; snapshot en `RingLayoutPayload` dragen de lijst al (geen `PROTOCOL_VERSION`-verhoging).
- `/ca tpring 5` blijft het nominale midden (8.850); `/ca castles` (81b) teleporteert naar een kasteelplek.

## 2. Biomes

| Sleutel | Dimensie | Band / rol | Eigenaar |
|---|---|---|---|
| `corrupted_wastes` | overworld | binnen: grijze dode vlakte, corrupted gras/aarde, dode bomen (kern) | W-biomes |
| `glitch_fields` | overworld | binnen: blokkerig terrein, zwevende kubussen, verschoven lagen, glitchblokken (kern) | W-biomes |
| `false_meadow` | overworld | binnen: perfecte vanilla-weide die net fout is (stil, geen mobs, gespiegelde bomen) | W-biomes |
| `void_scars` | overworld | buiten: scheuren en kraters tot in de void, voidsteen-randen | W-biomes |
| `inverted_hills` | overworld | buiten: omgekeerde heuvels in de lucht, bomen ondersteboven | W-biomes |
| `shattered_expanse` (werktitel) | overworld | buiten: de void zelf met brokken en bruggen (→ V12) | W-biomes |
| `white_halls` | Nether | Nether-5: witte vlaktes, zuilenrasters, spiegelsteen | W-nether |
| `hollow_cathedral` | Nether | Nether-5: enorme lege witte ruimte met één gigantische zuil (→ V10) | W-nether |

- Zone 4→5 houdt `sundered_reach` (`transitions.ring_4`, biome van ring 4; kleur, sfeer en grijsfade blijven). Zone
  5→void heeft geen overgangsbiome (de void-rand is `void_edge`).
- **Kastelen hebben geen eigen biome** (sparring ronde 2); welke biome de pinakel draagt → V12.
- **Biome-tags** (keys in `ModTags` via 81; inhoud door de eigenaar, via datagen):
  - `corruptedadventure:is_ring_5` — de 6 ring 5-biomes (niet `sundered_reach`: die blijft in `is_ring_4`);
  - `corruptedadventure:is_nether_ring_5` — `white_halls`, `hollow_cathedral`;
  - `corruptedadventure:ring_5_inner`, `ring_5_outer` (voorlopig) — lezers: A (lucht, sterren), C (alleen als
    vangnet; de band zelf komt uit `Ring5Band`);
  - `corruptedadventure:silent` (voorlopig) — biomes zonder ambient en muziek (`false_meadow`, Nether-5); lezer A.
  - `c:`-tags: te kiezen in 86/87. Geen `minecraft:has_structure/*` (fase 3); nooit `is_overworld`/stronghold.
- **Spawns:** ring 5 voorlopig geen dieren (dood); monsters vanilla tot fase 4; `false_meadow` en Nether-5 geen mobs.

## 3. Blokken en items (eigenaar B)

Alle blokken met een blockitem van dezelfde naam, tenzij vermeld. Creative-tab: ring 5 direct na Nether-ring 4, dan
Nether-ring 5. Namen **(voorlopig)** tenzij werktitel.

**88 — corrupted varianten** (vanilla-vorm, grijs uitgeloogd met dunne violet-zwarte aders; geen gedrag in fase 1)

| Blok | Gezonde tegenhanger (data map) |
|---|---|
| `corrupted_grass_block` | `minecraft:grass_block` |
| `corrupted_dirt` | `minecraft:dirt` |
| `corrupted_stone` | `minecraft:stone` |
| `corrupted_cobblestone` | `minecraft:cobblestone` |
| `corrupted_sand` | `minecraft:sand` |
| `corrupted_log` | `minecraft:oak_log` |
| `corrupted_wood` | `minecraft:oak_wood` |
| `corrupted_planks` | `minecraft:oak_planks` |
| `corrupted_leaves` | `minecraft:oak_leaves` |

- **Data map** `corruptedadventure:healthy_block` (registry `minecraft:block`, waarde een blok-id; type geregistreerd in
  81 via `RegisterDataMapTypesEvent` in `ModDataMaps`, inhoud via datagen door B). Test: elk blok in
  `#corruptedadventure:corrupted` heeft een mapping, en geen mapping zonder tag.
- **Blok-tag** `corruptedadventure:corrupted`: precies deze 9 (fase 5 leest tag + data map voor genezen/vervallen).
- Vanilla-tags waar het past (`#logs`, `#leaves`, `#dirt`, `#sand`, `#base_stone_overworld`, …). Gedrag zoals de
  tegenhanger (gras verspreidt zich niet; zand valt; leaves vergaan — te bevestigen in de vragenronde golf 1).
- Open voor de vragenronde golf 1 (niet blokkerend voor golf 0): stripped log/wood en een houtset (stairs/slab) ja/nee.

**89 — glitch, void, nulliet**
- `glitch_block` (missing-texture paars-zwart, geanimeerd via `.mcmeta`; geen code) → dropt `glitch_shard`
  (item, loot table; geen recipe in fase 1).
- `voidstone` (zwart met sterrenpixels, void-particles via `animateTick`; client-only particle-spawn in de bestaande
  `animateTick`-route).
- Nulliet (werktitel → `nullite`): `nullite_ore` (in `corrupted_stone`), `voidstone_nullite_ore`; item `raw_nullite`;
  tag `c:ores/nullite`. Plaatsing (95): scheurwanden en brokonderkanten in de buitenband, diep in de binnenband.
- Blok-tags: `corruptedadventure:ring_5_stones` (ertsdoelen: `corrupted_stone`, `voidstone`),
  `corruptedadventure:void_sources` (meter ↑ in de buurt; lezer C: `glitch_block`, `voidstone`).

**90 — Nether-5**
- `chalkrack` (kalkrack, wit netherrack), witsteen-set `white_stone`, `polished_white_stone`, `white_stone_bricks`,
  `white_stone_pillar` + `_stairs`, `_slab`, `_wall` (patroon `pumice`/`tidestone`), `mirror_stone` (spiegelsteen, glad en
  glanzend, begaanbaar), `still_light` (witte lichtbron zonder geluid of particles), witte kern: `white_core_ore`
  (in de kern van de zuilen) → item `white_core` (geen gedrag in fase 1).
- Blok-tag `corruptedadventure:nether_ring_5_stones` (ertsdoel: `white_stone`, `chalkrack`).

**Vanilla-blokken die ring 5 gebruikt:** bedrock (alleen waar V3 dat zegt), obsidiaan/crying obsidian als accent
(voorlopig), vanilla-weide-flora in `false_meadow`, bordjes en redstone-fakkels (Herobrine-tekens), tent/kist/kampvuur/bed
(kampen).

## 4. Worldgen-sleutels

**Gedeelde density functions.** Golf 0 (82) maakt van elke DF hieronder een stub (constante 0, tenzij vermeld). Alleen
de eigenaar vult hem in. Paden onder `data/corruptedadventure/worldgen/density_function/`.

| DF (`corruptedadventure:…`) | Betekenis / bereik | Eigenaar | Lezers |
|---|---|---|---|
| `ring_5/overworld/band` | 2D band-as: −1 binnen (dood) … 1 buiten (weg), 0 = midden van de overgangsband. Flat_cache van type `ring_5_band` (`Ring5BandFunction`, geen parameters) dat alleen `Ring5Band` aanroept. Getallen → **V1** | 82 | router `ridges` (weirdness), W-biomes, W-terrein; A en C via Java |
| `ring_5/overworld/region_rot`, `region_glitch` | 2D klimaatassen (dood/kaal ↔ "net fout"-weide; rustig ↔ glitchy), flat_cache van `noise/ring_5_rot`/`ring_5_glitch` (firstOctave −10, [1, 0,5, 0,25], zoals ring 4) | 82 | router `temperature`/`vegetation`, W-biomes |
| `ring_5/overworld/breakup` | 2D 0..1: hoeveel de grond in deze kolom is weggevallen (0 vaste grond, 1 void zonder brok); in de binnenband ~0 behalve losse scheuren. Uit `Ring5Field` (type `ring_5_output`, uitgang `breakup`) | 82 stub → 84 | router `erosion` (voorstel V12-A), W-biomes, A |
| `ring_5/overworld/initial_height`, `initial_terrain` | zoals ring 3/4: `initial_terrain` = 0,125 · `cache_2d(initial_height)` + gradiënt (nul op Y = hoogte); `initial_height` tot 83 = het placeholder-oppervlak (85 + 8 · `placeholder_terrain`) met de skeletvormen van golf 0 (`golf-0.md` §3.6) | 82 → 83/84 | router `initial_density_without_jaggedness` |
| `ring_5/overworld/height` | 2D oppervlaktehoogte van de vaste grond (zonder scheuren, void en grotten) | 83/84 | W-biomes (features, surface) |
| `ring_5/overworld/scars` | 0..1: scheur tot in de void (losse scheuren binnen, `void_scars` buiten) | 83/84 | W-biomes (voidsteen-randen, nulliet), A |
| `ring_5/overworld/castles` | 0..1: kasteelomgeving (1 = pinakeltop, bruggen, verwoeste rand); uit `Ring5Field` | 82 (posities) → 84 | W-biomes (95), fase 3 |
| `ring_5/overworld/glitch` | blokkerige glitchvormen (zwevende kubussen, verschoven lagen) | 83 | W-biomes |
| `ring_5/overworld/inverted` | omgekeerde heuvels (vorm boven de void) | 84 | W-biomes |
| `ring_5/overworld/caves` | per blok: onmogelijke gangen, void-grotten, gekopieerde grotten | 93 | W-biomes (ertsen) |
| `ring_5/overworld` | de ring 5-invoer van `ring_blend/overworld` (vervangt de placeholder) | 82 (skelet) → 83/84 | — |
| `zone_4_5/overworld/fracture` | 0..1 per kolom: breuk in zone 4→5 (spleet, schots, brok); uit `ZoneFracture` | 82 stub → 85 | W-biomes, A (sfeer), `ring_4/overworld/sundered` (wordt een verwijzing hiernaar, 85) |
| `ring_5/nether/region` | 2D regio-as Nether-5 (`white_halls` ↔ `hollow_cathedral`), flat_cache van `nether_ring_5_region` (−9) | 82 | Nether-router `temperature` |
| `ring_5/nether/terrain` | terreinvorm Nether-5; in 82 een verwijzing naar de placeholder `ring_5/nether` (constante 0 zou Nether-5 leeg maken) | 82 → 87 | `ring_blend/nether` |

**Java-velden (één bron per vorm)**
- **`ring.Ring5Band`** (82): `Ring5Band.of(layout).value(x, z)` ∈ [−1, 1]; `.outer(x, z)` (waarde ≥ 0 = buitenband) en
  `.inner(x, z)` (waarde < 0); overworld-coördinaten. Geseed op `RingLayout.noiseSeed()` met hash
  `corruptedadventure:ring_5_band` (zoals `Ring4Band`), dus de client rekent dezelfde band uit met de gesyncte layout
  (A: lucht per band; geen payload). `RingLayout` houdt zijn eigen instantie (lazy, zoals `ring4Band`). Lezers: DF,
  W-terrein, C (teleport alleen bij `inner`), A. Getallen → V1.
- **`worldgen.densityfunction.Ring5Field`** (82): het veld van ring 5 zoals `IslandField` (eigen raster, eigen `Column`,
  `ColumnTable` en `Decisions`, plaatsing zonder `flat_cache`, gevonden via `RingChunkGenerator.ring5Field(random)` en voor
  lezers zonder random state `worldgen.chunk.Ring5Fields.current(dimension)`, gevuld bij `LevelEvent.Load`, geleegd bij
  `ServerStopped`). Salt uit hash-noise `ring_5_cells` (wereldseed, **alleen server**: de client kan de kasteelplekken
  niet afleiden). Accessors (alleen lezen):
  - `castleSites()` → 4 × `CastleSite(index, angle, x, z, radius, top)` (index 0 = dichtst bij oost na de draaiing);
  - `castleAt(x, z)`, `bridgesWithin(minX, minZ, maxX, maxZ)` → `Bridge(siteIndex, fromX, fromZ, toX, toZ, halfWidth, deckY)`
    (84; ook de bruggen van de buitenband, V6);
  - `fragmentsWithin(…)` → `Fragment(x, z, radius, bottom, top, tilt)` (84), `breakupAt(x, z)`, `solidGround(x, z)`.
  Kasteelplekken → **V5**, bruggen → **V6**, onderkant → **V3**.
- **`worldgen.densityfunction.ZoneFracture`** (82 skelet, 85 inhoud): de breukzone 4→5 als puur veld van layout en salt.
  DF-type `corruptedadventure:zone_fracture` (record `argument`), gezet om `noise_router.final_density` van de overworld:
  buiten zone 4→5 (± 16 marge) en bij sterkte 0 exact `argument` (in golf 0 overal identiteit, bitgelijk). Binnen de zone
  snijdt hij **per blok ná de blend** (spleten → lucht, schotsen/brokken → extra vast), zodat ring 4 en ring 5 dezelfde
  breuk zien. Accessors voor de aquifer en features: `crackAt`, `floeAt`, `strength`. Vorm → **V7**, omvang van de
  ombouw → **V8**. `mapAll` geeft de visitor door aan `argument` (RingBlend-gates blijven werken).
- **Kopieerveld** (93, voorlopig `Ring5Copies`): paren bron/doel-cellen; `source(x, z)` geeft de gespiegelde of
  verschoven bronpositie. De terrein-DF van het doel samplet de bron (gespiegelde dubbelganger); de grotten-DF doet
  hetzelfde in 3D (gekopieerde grotten). Features worden **niet** gekopieerd in fase 1 (de dubbelganger is terrein).
- **Glitchvormen:** generiek DF-type `corruptedadventure:quantize` (voorlopig; record `argument`, `xz_step`, `y_step`):
  samplet het argument op een grof raster (blokkerig terrein). Alleen als JSON niet volstaat (83 beslist en noteert).

**Router (82)**
- `ring_blend/overworld_temperature` ring_5 → `ring_5/overworld/region_rot`; `_vegetation` → `region_glitch`;
  `_weirdness` → `band`; `_erosion` ring_5 → `breakup` (V12-A; anders 0); `_floodedness` ring_5 → 0 (droog, V9-A);
  `_initial` ring_5 → `0,390625 + ring_5/overworld/initial_terrain`; `_vein_toggle` blijft 0.
- `switch_at` blijft `["ring_5"]` (grens 5→void hard). Zone 4→5 heeft `transitions.ring_4`; `ring_4` staat er niet in.
- Nether: `nether_temperature` ring_5 → `ring_5/nether/region`; `ring_blend/nether` ring_5 → `ring_5/nether/terrain`.

**Vloeistofregel ring 5** (W-terrein, `RingFluidPicker`/`RingAquifer`; → **V9**)
- Voorstel (V9-A): **ring 5 is droog**: geen zee, geen lokale aquifervloeistof, geen lavalaag; open ruimte = lucht.
  Methode `ring5Dry(dimension, position)` op `ring()` = ring 5 (harde wissel op 0,5) en de ring 5-helft van zone 4→5;
  `noFluid` blijft vanaf zone 5→void.
- Carver-lava: `RingAquifer.decidesCarverLava` ook voor ring 5 (antwoord lucht; `WorldCarverLavaMixin` bestaat).
- Naad 4\|5 (backlog r. 259): de hele zone 4→5 volgt de breukzone-regel uit V7 (droog of water, nooit lokale lava);
  ring 5 erachter droog, dus geen naad water ↔ lava meer.
- Nether-5 (W-nether, 87; → V10): geen lavazee; `fluid_levels.ring_5` of een eigen `netherRing5Dry` (87 kiest de vorm),
  met de dam-regel van `docs/plans/ring-3/contract.md` §4 op de trede 4\|5 (Nether-4 lava Y 10).

**Biomehaak** (→ **V12**): voorstel A = geen Java: `rings.ring_5` = inline `minecraft:multi_noise` op temperature
(`region_rot`), vegetation (`region_glitch`), weirdness (`band`) en erosion (`breakup`); terrein en biome lezen dezelfde
named DF's (patroon §1.2). Alternatief B = optioneel codec-veld in `RingBiomeSource` zoals `island_biomes`.

**Overig**
- **Bedrock en void:** vanilla's bedrockvloer (surface rule 0) legt alleen bedrock op vaste blokken; een kolom die tot
  `min_y` lucht is, krijgt geen bedrock. Wat er onder de buitenband en de scheuren zit → **V3**.
- **Features:** keys `corruptedadventure:ring_5/<naam>` en `nether_ring_5/<naam>`; determinismeregels van 23c/35.
  Grote vormen (pinakels, bruggen, brokken, breukzone, dubbelgangers, omgekeerde heuvels) zijn terrein, geen features.
  Horror-features (94, voorlopig): `ring_5/missing_chunk` (op het chunkraster, tot de void), `ring_5/herobrine_sign`,
  `ring_5/torch_pattern`, `ring_5/abandoned_camp`, `ring_5/memory_<oak_grove|oasis|lava_field|holed_islet>`.
- **Vanilla-ertsen** (95): vrijwel weg; startwaarden gewoon ×0,05, midden ×0,03, zeldzaam ×0 (voorlopig).
- **Hoogtes** → **V4**. **Prestatie:** noise ≤ 1,75× vanilla (officieel lokaal, §6.5 van `waves.md`).

## 5. Condities (eigenaar C)

| Sleutel | Void (91) | Stilte (97) |
|---|---|---|
| enum | `Condition.VOID` (naam `void`), HUD violet (voorlopig `0xFF8A4FD8`) | `Condition.SILENCE` (naam `silence`), HUD wit (voorlopig `0xFFE8E8EC`) |
| zone | overworld ring 5 (hard op 0,5); daarbuiten ↓, snel in Nether-4 (zuiverend bestaat) | Nether-5 (hard op 0,5) |
| ↑ | overal langzaam; sneller bij `void_sources` in de buurt, in de buitenband, 's nachts, tijdens ringweer (modifier door C, factor in `[weather.ring5]`) | overal; sneller in open ruimte (`hollow_cathedral`, vlaktes) |
| ↓ | lichtniveau ≥ 12 van bloklicht | bel, goat horn, note block, jukebox in de buurt (server-side `GameEvent`-listener); andere spelers binnen ~8 blokken remmen de opbouw |
| drempels | 1 glitch-visuals (client leest de meter) → 2 korte teleports 1–5 blokken, **alleen op vaste grond in de binnenband** (`Ring5Band.inner`), veilige plek op de server, nooit in blokken of richting void; in de buitenband in plaats daarvan sneller darkness → 3 darkness + void-schade | geluid gedempt → HUD weg (behalve hotbar) → alle geluid weg + darkness → schade (hartslag) |
| config | `[conditions.void]`: rates, drempels, `teleportMinBlocks` 1, `teleportMaxBlocks` 5, `teleportIntervalSeconds` (voorlopig) | `[conditions.silence]`: rates, drempels, `companionRadius` 8, `soundRelief` (voorlopig) |

- Getallen (rates, drempels) volgen in de vragenronde golf 1 (zoals gif: 30/60/90 en +x/s).
- Client-effecten (glitch-visuals, volume, HUD) lezen alleen de gesyncte meter; `client/` (A of C volgens het patroon
  van ring 3/4: de HUD-balk is van C, visuals/volume van A). Lang: `Ring5ConditionText`.

## 6. Weer, geluid, particles (eigenaar A)

- **Weertypes** (`RingWeatherType`, ring 5, overworld): `GLITCH_STORM` en `REVERSE_RAIN` (bestaan), **`STANDSTILL`**
  (nieuw, naam `standstill`, voorlopig): wolken en regen bevriezen midden in de lucht, geluid valt weg; kort en zeldzaam.
  Staat server-side (`RingWeatherData`, bestaat), rendering in `client/`. Geen payload-wijziging (het type is een enum-
  index in de bestaande sync). Eén cyclus voor ring 5; of een type per band geldt → vragenronde golf 1.
- **Config `[weather.ring5]`** (bestaat met `glitchStormWeight`, `reverseRainWeight`; komt bij): `standstillWeight`
  (laag, voorlopig 15), `standstillMaxSeconds` (kort, voorlopig 120), `glitchStormVoidMultiplier` 2,0,
  `reverseRainVoidMultiplier` 1,5 (de modifier zelf bouwt C).
- **Sfeer:** binnenband donkergrijs met violette zweem en dikke kleurloze fog; buitenband steeds zwarter met sterren
  (ook overdag) via een client-hook op `Ring5Band` en `breakup`; echo-voetstappen per speler client-only (niet gesynct).
- **Sound events** (voorlopig; ring 5-synthesescript zoals `Ring2Sounds.java`): `weather.ring_5.glitch_storm`,
  `weather.ring_5.reverse_rain`, `weather.ring_5.standstill`; `ambient.ring_5.void_drone`, `ambient.ring_5.glitch`,
  `ambient.ring_5.echo_step`; `ambient.nether_ring_5.silence` (vrijwel stil, voor het dempen).
- **Particle types** (voorlopig; sprites van T): `void_mote` (voidsteen, scheurranden), `glitch_pixel`,
  `reverse_raindrop`; `glitch_speck` bestaat (ring 4) en blijft.

## 7. Textures (eigenaar T)

- Script `tools/textures/Ring5Textures.java` (37b-stijl; eigen pixels, deterministisch). Bestandsnamen = blok-/itemnaam.
- Paletrichting (sparring ronde 3): corrupted varianten = de vanilla-indeling, grijs uitgeloogd met dunne violet-zwarte
  aders; `glitch_block` missing-texture paars-zwart, geanimeerd (`.mcmeta`, `cycling`, opaak); `voidstone` zwart met
  sterrenpixels (emissive sterren, 37b-regels); Nether-5 wit/koel, `mirror_stone` glanzend; `white_core` met zachte
  witte gloed. Nulliet: donker met violet-zwarte kristallen (voorstel; vragenronde golf 1).
- B verwijst in de models naar deze paden; ontbreekt een PNG bij `runData`, dan een tijdelijke, niet-gecommitte kopie.

## 8. Lang en haken (81)

- Helpers: `datagen/lang/Ring5BlockNames` (B), `Ring5BiomeNames` (W-biomes), `NetherRing5BiomeNames` (W-nether),
  `Ring5ConditionText` (C: meters, doodsberichten), `Ring5WeatherText` (A: ondertitels), `Ring5SignText` (W-biomes, 94:
  de vertaalbare Herobrine-teksten, "ga terug", "hij ziet je", …). Engels verplicht, Nederlands erbij.
- Lege haken die `DataGenerators` en de providers al aanroepen: `Ring5Biomes`, `Ring5BiomeModifiers`, `Ring5Features`,
  `Ring5Carvers`, `Ring5BiomeTags`; `NetherRing5Biomes`, `NetherRing5BiomeModifiers`, `NetherRing5Features`,
  `NetherRing5Carvers`, `NetherRing5BiomeTags`; `Ring5DamageTypes` (void-schade, stilte-hartslag: C).
- `ModDataMaps` (nieuw, 81): type `healthy_block`.

## 9. Zone 4→5 — wat er met ring 4 gebeurt (→ V7, V8)

| Onderdeel nu (ring 4, pakket 63 en decor D) | Voorstel |
|---|---|
| `Ring4Sundered`: scheuren-raster (36), losse brokken, **landbruggen** (650, `onBridge`, `bridge`), afgrond −20, zee Y 0, `RIM_TOP` 64, `CALM_SHARE` | vorm gaat naar `ZoneFracture` (ná de blend); bruggen en afgrond vervallen; wat ring 4 nog nodig heeft (rim, calm-strook) blijft of verhuist mee (V8) |
| `IslandField.sunder`, `landBridgesNear`, `SUNDERED_CALM_SHARE`, `Column.sunderKind` (vallen, aquifer) | verwijderd of naar `ZoneFracture`-accessors |
| `RingFluidPicker.sunderedZone`/`SUNDERED_SEA_TOP`/`sunderedSealBelow`, `RingAquifer` (zone-water, dammen onder −54) | regel volgens V7 (droge bodem of water op Y 0); dam onder −54 blijft zolang ring 4 droog onder −54 is |
| `SplitRockFeature`/`SunderedReachFeatures` (gespleten rots op elke landbrug), `DecorDLandmarksTest` | volgens V8 (verhuizen naar een opgeheven schots, of weg) |
| `Ring4SunderedTest`, de zone-tests in `RingFluidPickerTest`/`RingAquiferTest`/`Ring4TunnelsTest` | vervangen door `ZoneFractureTest` en aangepaste vloeistoftests; `Ring4Tunnels.ZONE_GAP` blijft |
| `sundered_reach` biome, `Ring4Atmosphere`, `SunderedFade` (grijsfade 0,9), `glitch_speck`, `ambient.ring_4.sundered_drone` | blijven |

Oude werelden: de vorm van zone 4→5 verandert ook in bestaande testwerelden (naden bij al gegenereerde chunks); alleen
verse werelden tellen (zoals elke terreinchange).

## 10. Open punten — te beslissen vóór golf 0

Zie `golf-0.md` §6 (V1–V12). Samengevat: V1 bandlijn en ruis · V2 buitenband en buitenrand · V3 onderkant en void ·
V4 hoogtes · V5 kasteelplekken en pinakel · V6 bruggen · V7 vorm breukzone 4→5 · V8 omvang van de ombouw ring 4 ·
V9 vloeistof ring 5 · V10 Nether-5-indeling · V11 nummering blok G · V12 biome-indeling buitenband.

## 11. Besluiten vragenronde golf 1 (2026-10-21)

Gaan vóór de "voorlopig"-waarden hierboven.

- **Namen:** alle registry-namen uit §3 liggen vast (`corrupted_*`, `glitch_block`, `glitch_shard`, `voidstone`,
  `nullite_ore`, `voidstone_nullite_ore`, `raw_nullite`, `chalkrack`, `white_stone`-set, `mirror_stone`, `still_light`,
  `white_core_ore`).
- **Corrupted blokken (88):** gedrag als de vanilla-tegenhanger (zand valt, leaves vergaan zonder log); gras spreidt
  zich niet en wordt nooit gewoon gras. Geen eigen verspreiding tot fase 5.
- **Houtset (88):** volledige vanilla-set erbij: `stripped_corrupted_log`, `stripped_corrupted_wood`,
  `corrupted_stairs`, `corrupted_slab`, `corrupted_fence`, `corrupted_fence_gate`, `corrupted_door`,
  `corrupted_trapdoor`, `corrupted_button`, `corrupted_pressure_plate`, `corrupted_sign`/`corrupted_wall_sign`
  (+ hanging sign). Recipes zoals vanilla (fase 1-uitzondering, zoals de lagoon sandstone-set). Alleen de 9 blokken
  uit de tabel zitten in `#corruptedadventure:corrupted` met een `healthy_block`-mapping; de houtset volgt vanilla's
  hout-tags.
- **Void-meter (91):** sluipend. 0→100 in ~6 min in de binnenband (~0,28/s); ~3 min (~0,55/s) in de buitenband of
  binnen `void_sources`-bereik; 's nachts en tijdens ringweer ×1,5–2 (factor in `[weather.ring5]`). Drempels
  30/60/90. Bloklicht ≥ 12 laat hem zakken.
- **Teleports (91):** 1–5 blokken, elke ~20 s zolang de meter ≥ 60, alleen op vaste grond in de binnenband
  (`teleportMinBlocks` 1, `teleportMaxBlocks` 5, `teleportIntervalSeconds` 20).
- **Stilte-meter (97):** 0→100 in ~5 min (~0,33/s); open ruimte en `hollow_cathedral` ×1,5. Bel, goat horn, note
  block en jukebox binnen ~16 blokken laten hem zakken (`soundRelief`-bereik 16); andere spelers binnen 8 blokken
  halveren de opbouw (`companionRadius` 8). Drempels 30/60/90.
- **Weer (92):** één cyclus voor ring 5; het type hangt af van de band van de speler via gewichten per band:
  binnenband vooral `GLITCH_STORM` en `STANDSTILL`, buitenband vooral `REVERSE_RAIN`. Geen payload-wijziging.
- **Nulliet (99a):** donkere violet-zwarte kristallen met een violette rand, zwak emissive zodat ze in grotten net
  opvallen.
