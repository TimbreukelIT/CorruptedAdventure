# Change 31 — Overgangsbiome 2→3 `dry_riverbeds`

Scope: `worldgen` — change 31

**Doel:** de overgangszone van ~128 blokken rond grens 2 (ring 2 → ring 3) krijgt een eigen biome, de **drooggevallen rivierbeddingen** (`corruptedadventure:dry_riverbeds`): brede lege beddingen met gebarsten klei, gestrande botten en resten, en richting ring 3 steeds meer zwart geblakerde grond. Het vertelt het verhaal van het gestolen water. We volgen het patroon van `withered_expanse` (change 11): het veld `transitions` van `RingBiomeSource`, een bootstrap in `TransitionBiomes`, features met `ring_chance` (signaal `transition`) en een eigen surface-rule-tak. Er komt geen tweede aanpak bij. Nieuw is alleen dat de zone een lichte terreinvorm krijgt (de beddingen, vraag 1). Die komt uit JSON, met bestaande types.

Bron: `fase-1-wereld.md` §1.3 en §1.14 (overgangszones, `transitions`), §1.10 (1 overgangsbiome per grens), §1.15 (`ring_blend`), §2 Ring 2 (overgangsbiome 2→3, blokken), §2 Ring 3 (aansluiting: aslandschap, geen natuurlijk water, lucht rood-oranje), bouwplan rij 31. Overdracht uit `change-23.md` ("31 `dry_riverbeds`"), `change-25.md` (ertsen in de zone), `change-26.md` (biome-lijst zandgrotten) en `change-21.md` (`cracked_clay` in `#dead_bush_may_place_on`).

Buiten scope: het echte ring 3-terrein en de ring 3-blokken (as, verkoold hout: changes 38/39), definitieve lucht/fog/ambient van ring 2 (change 32), textures (37), structures zoals wrakken en bruggen (fase 3), mobs (fase 4). De Nether-grens 2→3 krijgt geen overgangsbiome: de Nether heeft geen overgangen (§1.14).

---

## Afstemming (2026-10-01)

Afgestemd met 23, 25, 26, 30 en 32 (basis: de aanbevolen opties).
- **Plan 30 bestaat:** `height.json` heeft dan regio's (`flat + relief_weight · (rock − flat)`, klem ≥ 66); de beddingen-term komt op het eindresultaat. Hoodoo-pilaren staan op de echte `height` (30), dus niet zwevend boven een bedding.
- **Ertsen (25):** per gastgesteente; zonnesteen ook in `scorched_sandstone` (oeverhellingen) binnen Y 95–150 (doorgerekend op 23 vraag 5 B; na 23b Y 95–180) — in de praktijk alleen bij beddingen op plateaus; op de vlakte (Y 66–74) niet.
- **Hoogtes (23 vraag 5 = B, keuze gebruiker):** plateau Y 125–140, kloofbodem ~70, vlakte Y 66–74. In de zone 2→3 mengt dat met placeholder ring 3 (Y ~120): vlaktes stijgen, plateaus zakken iets. De beddingen (3–6 diep, relatief aan het lokale oppervlak) veranderen niet.
- **`dry_riverbeds` in `Ring2Biomes.ALL` (23)**, hoewel de bootstrap in `TransitionBiomes` staat; tags komen uit die lijst.
- **Kleuren zijn hier geen vraag meer:** placeholder `Palette.RING_2.desaturated(0.3)`; de definitieve kleur is change 32 vraag 3 (`Palette.DRY_RIVERBEDS` vervangt `DRY_RIVERBEDS_FADE`). Vraag 5 gaat nu over klimaat, spawns en grotgeluid.
- **Lijsten:** lagenblok en zandgrot-tak via de controlelijst van 23 bijwerken.
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37). **Bijgewerkt 2026-10-01:** deze change begint pas na akkoord op **teststop 2b** (blokkerend, na change 30; keuze gebruiker 23b-17).

## Afstemming na teststop 2 (2026-10-01)

Afgestemd met de plannen 23b en 23c (antwoorden 2026-10-01). Waar de tekst hieronder iets anders zegt, geldt dit blok; de belangrijkste plekken zijn inline gecorrigeerd.
- **Beddingen op het eindresultaat van `height`:** sinds 23b bevat `height` tafelbergen (`plateau_surface` = `plateau_height` + `mesa_height`), treden en kolken in de kloofbodem (`canyon_floor`) en het nieuwe `canyon_depth` (zijtakken, puinhellingen); 30 voegt de regio's toe. De bedding-term blijft `min(h, max(66, h − riverbed_mask · riverbeds))` op dat eindresultaat. Een bedding over een tafelberg = een geul in de tafelberg; "3–6 diep, relatief" blijft kloppen.
- **De poel-term heet `pools.json` (23b).** Daar komt `× (1 − riverbed_mask)` (niet in `flats_height.json`), zodat ook `initial_height` (die `pools.json` gebruikt) de droge poelen volgt en de aquifer er geen water zet. `ring_2_pools` heeft sinds 23b-8 een extra grove octaaf (poelen plaatselijk breder); dat verandert niets aan het masker.
- **Oasezones:** de grasoever en overgangsring van `oasis_flats` (surface rules op `ring_2_pools`) gelden alleen in de `oasis_flats`-tak. De zone is biome `dry_riverbeds` met een eigen tak: geen grasringen rond droge poelen. Bij de bouw nalopen dat de `dry_riverbeds`-tak vóór of los van de oasezones staat.
- **Fijne vormen vervallen in de zone:** spleten, rotsvormen, richels/nissen en tafelbergen van 23b hangen aan `core_mask` (ring 2 = 1 buiten de zones). In de zone 2→3 is het terrein dus vlakker dan het kloofgebied; dat past bij "brede lege beddingen". Hoodoos (30) vervallen er ook.
- **Zonnesteen (25, bijgewerkt):** `trapezoid` Y 95–180 (`plateau` 60), 14 aders. Voor `scorched_sandstone`-oeverhellingen verandert er praktisch niets (die liggen bij beddingen op plateaus; op de vlakte niet).
- **Features (23c):** gestrande botten/resten kunnen `rubble` + `slope` hergebruiken (in dezelfde volgorde binnen de stap); oasefeatures vallen in de zone weg via de biome-filter. Liever `jittered_grid`/`noise_band`/`slope` dan `count` + `in_square` (geen toeval-knotten).
- **Prestatiebudget (23b-9 C, keuze gebruiker):** ring 2-noise-stap **≤ 1,75× vanilla**, gedeeld met 23b/26/30 (was 1,3×).

---

## Afstemming na bouw 23b–30c (2026-10-05)

Nagelopen tegen de stand na `3abf6f3` (changes 0–30, 23b, 23c, 30b, 30c gebouwd; teststop 2c akkoord, "levendig genoeg om door te gaan"). Waar de tekst verderop iets anders zegt, geldt dit blok; de belangrijkste plekken zijn inline gecorrigeerd.
- **`height.json` is anders opgebouwd.** Sinds 30: `flat_cache(flat_height · (1 − relief_weight) + relief_weight · rock_height)`. De klem ≥ Y 66 zit in de delen (`rock_height` = `max(66, …)`, zout 66, duinbasis 67 ± 2, `flats_height` `max(59, …)` tot de poelen), niet bovenop. De bedding-term wordt een wikkel: `h − min(riverbed_mask · riverbeds, max(0, h − 66))`. Let op de valkuil "elke JSON-verwijzing wordt apart uitgerekend": `h` komt twee keer voor, dus hernoem de huidige `height.json` naar `height_base.json` (flat_cache) en laat `height.json` een dunne `flat_cache`-wikkel zijn; meet de noise-stap.
- **`initial_height.json` bestaat apart** (30; `initial_terrain` leest hem via `cache_2d`) en kent de bedding niet. Hij moet de bedding-term **ook** krijgen in het vlakke deel (`flats`: `70 + 4 · hills`, tot 74), anders ligt het preliminary surface tot ~6 blokken **boven** de echte bodem van de bedding en krijgt de bedding geen `above_preliminary_surface`-regels (geen gebarsten klei, valkuil change 23). Zout (66), duin (66) en het rotsdeel (69 ± 2, onder de echte kloofbodem) liggen er al onder en blijven ongewijzigd. Controle in S: per 10.000 bedding-kolommen 0 × "echte bodem lager dan preliminary surface" zonder dat de bodem een oppervlakteregel mist.
- **`pools.json` is het enige poelen-bestand** (gebruikt door `flats_height` en `initial_height`; de poel-term is `spline(ring_2_pools) × spline(moisture)`). De droogval-term `× (1 − pools_dry)` komt daar. **Eigen steile ramp**, niet de `riverbed_mask`: de hele zone is al `dry_riverbeds` vanaf blend > 0, maar `riverbed_mask` is pas 1 vanaf w3 0,25; met die ramp bleven er in de eerste kwart-zone nog diepe poelen met water in een biome die "droog" heet. `pools_dry` = spline op `ring_weight/overworld/ring_3`: 0 bij 0 → 1 bij 0,03.
- **Wat er in de zone aan terreinregio's ligt** (30): zout (Y 66, `salt_weight`), duinen (Y 66–99), hoodoovloer (~Y 87, pilaren vervallen via `core_mask`), oase-vlakte (Y 66–74) en kloof/plateau. `core_mask` (= ring-2-gewicht) dooft alleen `canyon_spire_weight`, `hoodoo_rock_weight`, `mesa_limit`, `plateau_rock_weight`, `slot_weight` en `wall_weight`; `canyon_depth`, `plateau_surface` en de regio-gewichten blijven. De zone is dus niet overal "vlak": canyonwanden en duinen lopen er (gemengd met ring 3 op Y ~120) in door. Gevolg voor de klem ≥ 66: zie vraag 7.
- **Surface-rule-structuur (30–30c).** `above_preliminary_surface` heeft takken voor `withered_expanse` [0], `canyonlands` [1], `oasis_flats` [2], `hoodoo_fields` [3], `salt_flats` [4], `dune_sea` [5], dan vanilla's badlands. De `dry_riverbeds`-tak komt als **[6]** (voor de `stone_depth`-restregel; de volgorde tussen biome-takken doet er niet toe). De `canyonlands`-tak volgt het patroon "niet steil → bovenlaag, steil → val door naar de top-level lagen [3]". Neem dat patroon over voor kloofwanden in de zone (antwoord 6 A: de lagen lopen door); alleen steile **bedding-oevers** (binnen de `|n| < 0,07`-band van het bedding-ruisveld) krijgen `scorched_sandstone`. Zo botsen antwoord 2 A (scorched op hellingen) en 6 A (lagen onder de zone) niet.
- **Top-level lijsten (nu):** [2] zandgrottak (twee `biome_is`-lijsten: plafond en vloer), [3] kloofsteenlagen `canyonlands, hoodoo_fields`, [4] oasis-lagen. `dry_riverbeds` komt in [2] en [3] (controlelijst `CODEBASE.md`, "Plekken die ring 2-biomes opsommen"), plus `Ring2Biomes.ALL` (tags en `Ring2BiomesTest`).
- **Bestaande hulpmiddelen hergebruiken in plaats van nieuw bouwen:**
  - gestrande botten = bestaande `corruptedadventure:surface_fossil` (30c; sjablonen `fossil/spine_1…4`, `skull_1…4`, half begraven, verweerd, chunk-lokaal, voetafdruk [1, 14] van de chunk, alleen botblokken) + `bone_rubble` (`rubble` van `bone_block`). **`StrandedFossilFeature`/`-Configuration`/`-Test` vervallen** (zie vraag 3);
  - plaatsing met `jittered_grid` (geen toeval-knotten), `noise_band` (vlekken op geseede ruis, bijv. een eigen of bestaande `ring_2_*`-ruis), `slope` en `height_band`; geen `count` + `in_square` en geen `rarity_filter` voor zichtbare velden;
  - `rubble` (keien, ook donkere/geblakerde stenen) en `canyon_rockfall` voor stenen in de bedding, `outcrop` voor een enkele rotsnaald;
  - `desert_fossil`-config (grond zand/rood zand) moet voor de zone een variant met grond `red_sand`/`coarse_dirt`/`cracked_clay`/`scorched_sandstone` krijgen (nieuwe configured `riverbed_fossil`, zelfde sjablonen);
  - `dry_shrub`, `dead_bush`, `barrel_cactus`-groepen (`cactus_column`, `short_cactus_column` met `ring_2_cactus_groups`), kort/hoog droog gras (`dry_grass`) en de harde-dalbodem-regel van 30c (`coarse_dirt`/`cracked_clay` + kort gras) zijn voorbeelden voor de oevers.
- **"Levendig" is de norm** (teststop 2b/2c): de plan-aantallen in stap 5 (3 flora-features, ~4 pogingen per chunk) zijn van vóór de teststops en worden als ondergrens gezien; zie vraag 8.
- **Water:** de pekelpoelen, mini-oases, wandbronnen en oase-poelen van 23c/30b vallen in de zone weg via de biome-filter. Er is geen water in de zone boven Y 60 (S-eis blijft).
- **Prestaties:** hoogste meetpunt ring 2 nu **1,54×** vanilla (x 2.500, duinen); budget 1,75× → ruimte voor deze change ≈ 0,2×. De bedding-DF is 2D en gaat in `flat_cache`/`cache_2d`; meet vóór en na op een zonemeetpunt (tot 30 had dat punt het hoogste getal). Een tweede referentie naar `height_base` is een risico (zie hierboven).
- **Teststop 2b/2c is gedaan:** de zone zelf is er niet getest. Het werk van 31 gaat mee in teststop 3.

### Gevolgen van de antwoorden 7–9 (2026-10-05)
- **7 A:** geen klem-wijziging; stap 2 blijft zoals hierboven. In de surface rule krijgt de `dry_riverbeds`-tak voor de bedding-bodem de `noise_threshold` op `dry_riverbeds_channel` ook op terrein dat te laag ligt voor een geul (lint gebarsten klei). S meet bedding-kolommen op zout/duin apart.
- **8 A:** de features-tabel in stap 5 is de bouwopdracht (matig, niveau 30c); `riverbed_stones` en de oever-cactusgroepjes horen erbij; geen `outcrop`/wrakken.
- **9 A:** geen extra features voor een zachte rand; de W-lijst vraagt alleen of de harde wissel acceptabel is.

## Afhankelijkheden en aannames

**Gecontroleerd in de code** (stand na change 17, commit `7714b77`; ververst op 2026-10-05, zie het blok hierboven):
- `RingBiomeSource` (`worldgen/biome/RingBiomeSource.java` r. 31–92): `transitions` is generiek. De sleutel is de binnenring (`ring_2` = grens 2→3), `void` is ongeldig, `collectPossibleBiomes` neemt de unie van `rings` en `transitions`, en `getNoiseBiome` kiest in `inTransition()` de overgangssource van `position.inner()`. **Voor grens 2 hoeft er geen Java bij**: alleen een preset-entry.
- `TransitionBiomes` (r. 27–87): bootstrap met alleen `withered_expanse`. `globalOverworldGeneration` is een private kopie van vanilla. Kleuren via `BiomeAmbience.Palette.RING_1.desaturated(0.4F)`, mood `STRANGE`. Het commentaar zegt al "één per grens".
- `RingChanceFilter.transition(Ring inner, from, to)` (r. 60, 108): het signaal werkt voor elke grens, dus `transition(RING_2, …)` = blend in de zone 2→3, anders geen signaal (kans 0).
- `RingBlend` (r. 30–31): `rings` gebruikt `DensityFunction.HOLDER_HELPER_CODEC`, dus een verwijzing, een inline functie of een **getal**. Een `ring_blend` met constante invoer (ring 3 = 1, de rest 0) geeft zo het smoothstep-gewicht van ring 3 als density function, zonder nieuwe Java (stap 2).
- `WitheredExpanseFeatures`: het sjabloon voor een feature-bootstrap per overgangsbiome (configured + placed, `ring_chance` vóór de heightmap of de biome-filter, `disk` met een `RuleBasedBlockStateProvider`).
- Surface rules (`noise_settings/overworld.json`): de tak van `withered_expanse` is het eerste element van `above_preliminary_surface`. Afspraak in `CODEBASE.md`: eigen takken komen als losse blokken bovenaan.
- `BiomeAmbienceTest` test de kleuren en de mood van `withered_expanse`. `RingBiomeSourceTest` (`transitions`) test één overgang.

**Aangenomen bij het schrijven; changes 18–30c zijn inmiddels gebouwd en nagelopen (2026-10-05).** Wat afwijkt staat in "Afstemming na bouw 23b–30c"; bij de start nog één keer naast `CODEBASE.md` leggen.
- **A1 — change 21:** `cracked_clay` (in `#minecraft:dead_bush_may_place_on` en `#overworld_carver_replaceables`), `dry_shrub` (grond `#corruptedadventure:desert_plant_soil`, met `cracked_clay` erin), `scorched_sandstone`, de kloofsteentinten met tag `#corruptedadventure:canyon_stones`.
- **A2 — change 23:**
  - de ring 2-invoer in `density_function/ring_2/overworld/`, met een 2D-`height.json` (`flats + relief_weight · (plateau − flats) − relief_weight · canyon_depth`, kloofbodem en vlakte geklemd op ≥ Y 66) en `terrain.json` = `(height − y) / S`;
  - de poelen in `flats_height.json` via `ring_2_pools`, tot Y ~59 (**na 23b:** eigen bestand `pools.json`, ook gebruikt door `initial_height`);
  - `ring_blend/overworld_initial` ring 2 = `0,390625 + ring_2/overworld/terrain`;
  - `Ring2Biomes.generation(...)` als gedeelde helper (vanilla `globalOverworldGeneration` package-private, ertsenslot);
  - `BiomeAmbience.Palette.RING_2`;
  - tags `is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld`;
  - het kloofsteenlagenblok in de top-level-`sequence` met `biome_is [canyonlands]`;
  - de ring gate in `RingBlend.mapAll` (vraag 3 van change 23).

  Heeft change 23 geen 2D-hoogte (bijvoorbeeld een andere opbouw), dan geldt stap 2 op de plek waar de oppervlaktehoogte van ring 2 wordt bepaald. Is er geen hoogte-domein, dan valt vraag 1 terug op optie B.
- **A3 — change 25:** `add_ring_2_ores` (`add_features` op `#is_ring_2`, stap `UNDERGROUND_ORES`), met de geschaalde vanilla-ores (`ring_chance` `progress` ring 2: **kans 0 buiten ring 2**) en zonnesteen en turkoois (per gastgesteente een `BlockMatchTest` → variant uit change 24; zonnesteen ook in `scorched_sandstone`, dus ook in de oeverhellingen van deze zone binnen Y 95–150; na 23b Y 95–180). In de ring 2-biomes staat `addDefaultOres` vervangen door alleen de niet-ertsen. De vanilla-ore veins staan alleen in ring 1 aan.
- **A4 — change 26:** de ring 2-biomes hebben vanilla-carvers en geen lavameren. De surface-rule-tak voor zandgrotten heeft een `biome_is`-lijst. Afspraak: nieuwe ring 2-biomes (30/31) komen in die lijst.
- **A5 — change 30** (plan, afgestemd 2026-10-01): `salt_flats`, `hoodoo_fields` en `dune_sea` komen als regio's in dezelfde `height.json` (`flat + relief_weight · (rock − flat)`, daarna de klem ≥ 66), en `hoodoo_fields` komt in het lagenblok. Ze liggen dus ook in de zone 2→3. De beddingen (stap 2) snijden door alle regio's; de hoodoo-pilaren staan op de echte `height` (change 30, `floor` = `height`) en zweven dus niet boven een bedding.
- **A6 — changes 27/28** (uitdroging, ringweer) werken hard op `ringAt(...).ring()`. Deze change verandert niets aan hun gedrag in de zone.
- Ring 3 is tot change 39 een placeholder: `placeholder_ring_3`, terrein Y ~120, geen features. In de zone mengt het terrein dus van het ring 2-niveau (vlakte Y 66–74, plateau Y 125–140; 23 vraag 5 B) naar Y ~120: vlaktes stijgen, plateaus zakken iets.

---

## Achtergrond (uitgezocht, niet opnieuw doen)

- **Ring-gewicht als density function zonder Java:** `ring_blend` met constante invoer `{ring_3: 1, de rest: 0}` = `w3` = het smoothstep-gewicht van ring 3. Dat is 0 in ring 2, stijgt in de zone 2→3 en is 1 in ring 3. Binnen de **ring 2-invoer** (die alleen telt waar ring 2 gewicht heeft) is `w3 > 0` dus precies de zone 2→3. In de zone 1→2 is `w3 = 0`. Het kost één opzoeking via `RingColumnCache`, zoals elke `ring_blend`.
- **Zelfde ruis in terrein en surface rule:** een `minecraft:noise` met `xz_scale` 1 en `y_scale` 0 sampelt `getValue(x, 0, z)`, precies wat de surface-conditie `noise_threshold` doet. Beide halen de instantie uit `RandomState.getOrCreateNoise(key)`. Een bedding die het terrein uitsnijdt waar `|n| < drempel`, wordt dus door de surface rule op exact dezelfde kolommen herkend. **Bij de bouw verifiëren** in de 1.21.1-bronnen (`RandomState.NoiseWiringHelper`, `SurfaceRules.NoiseThresholdConditionSource`).
- Surface rules kennen geen ringgewicht. Een verloop richting ring 3 kan daarom alleen via features met `ring_chance` (zoals de zandvlekken van `withered_expanse`) of via een eigen Java-conditie. Die laatste houden we buiten deze change.
- Vanilla's `FossilFeature` legt fossielen altijd 15–25 blokken **onder** het laagste oppervlak van de footprint. Aan de oppervlakte zie je ze niet. Fossielsjablonen zijn vanilla-structure-templates (`minecraft:fossil/spine_1…4`, `skull_1…4` + `_coal`-overlays).

---

## Plan

Volgorde = bouwvolgorde. Het plan volgt de aanbevolen opties. Bij een ander antwoord verandert alleen de stap van die vraag.

### 1. Biome-source: `transitions.ring_2` (preset)

`data/corruptedadventure/worldgen/world_preset/corrupted_world.json`, alleen de overworld:
```json
"transitions": {
  "ring_1": { "type": "minecraft:fixed", "biome": "corruptedadventure:withered_expanse" },
  "ring_2": { "type": "minecraft:fixed", "biome": "corruptedadventure:dry_riverbeds" }
}
```
De hele zone (`inTransition`, ~128 blokken) wordt `dry_riverbeds`, ook boven water (afspraak change 11; in de zone staat met vraag 2 geen water meer aan de oppervlakte). De Nether blijft ongewijzigd. Er is geen Java-wijziging aan `RingBiomeSource`.

### 2. Beddingen in het terrein (vraag 1 A, puur JSON)

Handgeschreven, in `data/corruptedadventure/worldgen/`:

| Bestand | Inhoud |
|---|---|
| `density_function/ring_weight/overworld/ring_3.json` | `flat_cache(ring_blend{dimension overworld, ring_3: 1, ring_1/2/4/5/void: 0})` = `w3`. Generiek: latere zones (3→4 `steam_fens`, 4→5 `blighted_mire`) maken op dezelfde manier `ring_4`, `ring_5` |
| `noise/dry_riverbeds_channel.json` | eigen ruis voor het beddingennetwerk (startwaarde `firstOctave` −8, 2 octaven: meanderende linten om de ~150–300 blokken) |
| `density_function/ring_2/overworld/riverbed_mask.json` | `spline` op `ring_weight/overworld/ring_3`: 0 bij 0, 1 vanaf 0,25 → de beddingen beginnen kort na de binnenrand van de zone |
| `density_function/ring_2/overworld/riverbeds.json` | 2D-diepte: `minecraft:noise` `dry_riverbeds_channel` (`xz_scale` 1, `y_scale` 0; zie Achtergrond), `abs` → `spline`: \|n\| ≤ 0,04 → volle diepte 5 (vlakke, brede bodem, 15–40 blokken), 0,04–0,07 → oever met 1–2 treden, ≥ 0,07 → 0 |

- **`ring_2/overworld/height.json`** (wikkel, zie "Afstemming na bouw") wordt `h − min(riverbed_mask · riverbeds, max(0, h − 66))` met `h` = de huidige opbouw, hernoemd naar `height_base.json`. Een bedding verlaagt alleen, nooit onder Y 66, dus er staat geen zeewater in een bedding (zelfde klem als de kloofbodems van change 23). Op een plateau worden het droge geulen in de mesa, op de vlakte ondiepe linten, in zout (Y 66) en duindalen (66–67) is er geen diepte meer over (vraag 7).
- **Poelen vallen droog** (vraag 2 A): in `pools.json` (de poel-term van `flats_height` én `initial_height`) wordt de term `× (1 − pools_dry)`, met een eigen steile ramp (`pools_dry` = spline op `ring_weight/overworld/ring_3`, 0 bij 0 → 1 bij 0,03; niet de `riverbed_mask`, zie "Afstemming na bouw"). In de zone liggen dan geen oasepoelen meer. Ondergrondse reservoirs (change 26) blijven.
- De initiële dichtheid volgt **niet** vanzelf: `ring_blend/overworld_initial` → `initial_terrain` → `initial_height` (apart bestand, 30). `initial_height` krijgt dezelfde bedding-term in het `flats`-deel (zie "Afstemming na bouw"); zout (66), duin (66) en het rotsdeel (69 ± 2) liggen al onder de echte bodem. Een bedding van ≤ 6 diep op de vlakte blijft ≥ Y 66 en dus boven zeeniveau; nalopen dat er onder het preliminary surface geen nieuwe open ruimte onder Y 63 ontstaat (valkuil change 23).
- **Buiten de zone verandert niets:** `riverbed_mask = 0` geeft `h` exact terug (vergelijkingstest in S).
- **Verloop:** de beddingen zitten alleen in de ring 2-invoer. Hun zichtbare diepte neemt dus af naarmate ring 3 (placeholder Y ~120) meeweegt: diep aan de ring 2-kant, aan de ring 3-kant lopen ze dood in geblakerde grond (stap 5). (De `ring_blend` over de ring 2-invoer weegt de hoogte met `w2`; de bedding-diepte schaalt daar dus automatisch mee.) Change 39 kan hetzelfde masker (`ring_weight/overworld/ring_2` in de ring 3-invoer) gebruiken als de beddingen ook aan de ring 3-kant moeten doorlopen (overdracht).
- Ring gate (change 23): de geneste `ring_blend` met constanten is geen `interpolated`-marker. De gate wikkelt hem niet en hij kost alleen de cache-opzoeking. Controleren bij de bouw.

### 3. Biome `dry_riverbeds` (datagen, in `TransitionBiomes`)

- `ModBiomes.DRY_RIVERBEDS`. `TransitionBiomes.bootstrap` registreert hem naast `withered_expanse`.
- **Generation:** `Ring2Biomes.generation(...)` (package-private, zelfde package als `TransitionBiomes`; vanilla-volgorde, `underwater_magma` als enige van `addDefaultOres`, chunk-lokale disks `ring_2/disk_*` i.p.v. `addDefaultSoftDisks`, diepe `Ring2Carvers`, geen lavameren/`spring_lava`). De ertsen komen via de biome modifier (vraag 4). Eigen features achteraan per stap (stap 5), in een vaste volgorde. `TransitionBiomes.globalOverworldGeneration` blijft alleen voor `withered_expanse`. Zo heeft `dry_riverbeds` dezelfde vanilla-featurevolgorde als de ring 2-biomes, en dat voorkomt een "Feature order cycle".
- **Klimaat (vraag 5 A):** `temperature` 2,0, `downfall` 0, `hasPrecipitation` false.
- **Spawns (vraag 5 A):** dezelfde lijst als `canyonlands`: `Ring2Biomes.spawns(false)` (package-private, gedeeld door canyonlands/hoodoos/duinen; geen kamelen).
- **Kleuren (placeholder, geen ontwerpvraag meer — afgestemd 2026-10-01; `Palette.RING_2` is nu de placeholder `9FC4E8`/`E8D7A8`/`32A598`/`0B3A35` en `Palette.desaturated` bestaat; de definitieve kleur van `dry_riverbeds` is change 32 vraag 3, die `DRY_RIVERBEDS_FADE` vervangt door `Palette.DRY_RIVERBEDS`):** lucht/fog/water `BiomeAmbience.Palette.RING_2.desaturated(0.3F)` als voorbode van de asgrijze ring 3 (constante `DRY_RIVERBEDS_FADE` naast `WITHERED_EXPANSE_FADE`). Gras en blad als `canyonlands` (`BFB755`/`AEA42A`), 30 % naar grijs. Grotgeluid zoals de ring 2-biomes (change 32 beslist).
- **Tags** (`ModBiomeTagsProvider`): `corruptedadventure:is_ring_2` (vraag 4 A), `c:is_hot/overworld`, `c:is_dry/overworld`, via `Ring2Biomes.ALL` (6e entry; `Ring2BiomesTest` leest die lijst: bootstrap = `ALL`, dus de test dekt `dry_riverbeds` mee; `TransitionBiomes.bootstrap` moet hem dus ook registreren). **Geen** `minecraft:`-tags (geen `is_overworld`, geen `has_structure/*`).
- Lang: `biome.corruptedadventure.dry_riverbeds` → en_us "Dry Riverbeds", nl_nl "Drooggevallen Rivierbeddingen".

### 4. Surface rules (`noise_settings/overworld.json`, vraag 2 A, 6 A)

**Tak in `above_preliminary_surface`**, als element [6] ná `dune_sea` (één los blok, duidelijk gemarkeerd; zie "Afstemming na bouw" voor het steil/niet-steil-patroon):
```
condition(biome [dry_riverbeds],
  sequence[
    condition(stone_depth floor, offset 0,
      sequence[
        condition(steep AND noise_threshold dry_riverbeds_channel -0.07…0.07, scorched_sandstone), // alleen oevers van de bedding; andere steile wanden (kloof) vallen door naar de lagen [3]
        condition(noise_threshold dry_riverbeds_channel -0.04…0.04,             // beddingbodem (zelfde ruis als het terrein)
          sequence[ condition(noise_threshold surface 0.6…, gravel), cracked_clay ]),
        condition(noise_threshold dry_riverbeds_channel -0.07…0.07, coarse_dirt), // oever
        condition(noise_threshold surface -0.5…0.2, coarse_dirt),               // vlekken
        red_sand ]),
    condition(stone_depth floor, add_surface_depth,
      sequence[ condition(steep, scorched_sandstone), red_sandstone ]) ])
```
Startdrempels voor de surface-ruis worden in W bijgesteld (geen ontwerpvraag). De `noise_threshold`-drempels van de bedding moeten gelijk zijn aan de spline-knikken van `riverbeds.json` (één bron: een commentaarregel in beide bestanden).

**Ondergrond (vraag 6 A):** `dry_riverbeds` komt in de `biome_is`-lijst van het top-level kloofsteenlagenblok [3] (nu `canyonlands, hoodoo_fields`). Kloven die de zone in lopen, houden zo hun lagen, en de beddingen snijden door de lagen. **Zandgrotten** (change 26): `dry_riverbeds` komt in beide `biome_is`-lijsten van top-level element [2] (plafond en vloer). De `dry_riverbeds`-tak doet voor zout- en oaseterrein in de zone niets bijzonders: hij legt overal dezelfde bodem (rood zand / grove aarde / gebarsten klei), dus de zoutkorst, het gras en de poelranden van ring 2 stoppen aan de zonegrens.

### 5. Features (datagen): `worldgen/feature/DryRiverbedsFeatures.java` (sjabloon `WitheredExpanseFeatures` en `Ring2DressingFeatures`)

Keys in `ModConfiguredFeatures`/`ModPlacedFeatures`. Alles chunk-lokaal volgens de determinisme-regels van 23c/30b (plaatsing begint in de eigen chunk, leest alleen `WORLD_SURFACE_WG`, geseede ruis en de placement-random; schrijft alleen in de eigen chunk, ≥ 1 van de rand). **Geen** `random_patch`, `count_on_every_layer`, `environment_scan`. Zichtbare velden met `jittered_grid` (en `noise_band` voor vlekken), niet met `count` + `in_square`. Aantallen zijn startwaarden (W); de norm is "levendig" (zie vraag 8: de tabel hieronder is de aanbevolen optie).

| Configured | Type | Inhoud |
|---|---|---|
| `scorched_patch` | `minecraft:disk` | geblakerde vlek (vraag 2 A): `blackstone` 3, `basalt` 2, `scorched_sandstone` 2 (alleen gesteente, geen ertsen); `RuleBasedBlockStateProvider` met `scorched_sandstone` boven lucht (zoals de zandvlek van `withered_expanse`). Doel: `red_sand`, `coarse_dirt`, `cracked_clay`, `gravel`. Straal 2–5. **Determinisme:** vanilla `disk` kan in buurchunks schrijven; gebruik dezelfde oplossing als `ring_2/disk_*` (30): `jittered_grid` met marge ≥ de grootste straal (6), zodat de hele vlek in de eigen chunk blijft, en geen `in_square`. De `withered_expanse`-zandvlek (`count` → `in_square`) is dus **geen** sjabloon voor de plaatsing |
| `riverbed_fossil` | **bestaande** `corruptedadventure:surface_fossil` (30c) | zelfde sjablonen als `desert_fossil` (`fossil/spine_1…4`, `skull_1…4`, `buried` 0,4–0,7, `integrity` 0,85), grond `red_sand`/`coarse_dirt`/`cracked_clay`/`scorched_sandstone`. Geen nieuwe Java-class |
| `riverbed_bones` | **bestaande** `rubble` (`bone_rubble`-config van 30c) | losse botblokken (as x 2 / z 2 / y 1, grootte 1–2) |
| `riverbed_stones` | **bestaande** `rubble` | keien van `cobblestone`/`andesite`/`blackstone` (donker, "aangevoerd door het water") 1–2, op de bedding en de oevers |
| `riverbed_vegetation` | `minecraft:simple_block` / `random_boolean_selector` zoals `dune_valley_flora` | `dry_shrub` 3, `dead_bush` 2 (grond via `would_survive`), op oevers ook `short_cactus_column`-groepjes en kort/hoog droog gras (`dry_grass`) op `coarse_dirt` |

| Placed | Stap | Plaatsing |
|---|---|---|
| `dry_riverbeds_scorched_patches` | `LOCAL_MODIFICATIONS` (vóór de begroeiing) | `jittered_grid` (cel 16, marge 6 = grootste straal + 1, kans 0,5) → `ring_chance` (`transition`, `ring_2`, 0 → 0,9, `exponent` 1,5) → `WORLD_SURFACE_WG` → `biome` |
| `dry_riverbeds_fossils` | `LOCAL_MODIFICATIONS` (naast de duinfossielen) | `jittered_grid` 16/1/0,25 → `ring_chance` (`transition`, `ring_2`, 1,0 → 0,3) → `WORLD_SURFACE_WG` → bodemfilter (`red_sand`/`coarse_dirt`/`cracked_clay`) → `biome`; doel ~1 per 25–30 chunks in de ring 2-helft (`dune_sea_fossils`: 1 per 26) |
| `dry_riverbeds_bones` | `LOCAL_MODIFICATIONS` | `jittered_grid` 8/1/0,3 → `ring_chance` (1,0 → 0,5) → heightmap → bodemfilter → `biome` |
| `dry_riverbeds_stones` | `LOCAL_MODIFICATIONS` | `jittered_grid` 8/1/0,3 → `slope` (oeverhellingen/bedding) → `biome` |
| `dry_riverbeds_dead_trees` | `VEGETAL_DECORATION` | **hergebruik** `corruptedadventure:dead_tree` (change 11): `jittered_grid` 16/2/0,25 (geen knotten) → heightmap → filter `#dirt` eronder (oevers: `coarse_dirt`) → `ring_chance` (`transition`, `ring_2`, 0,8 → 0,1) → `biome` |
| `dry_riverbeds_vegetation` | `VEGETAL_DECORATION` | `jittered_grid` 4/0/0,35 → heightmap → `block_predicate_filter` (lucht + `would_survive`) → `biome`; vlekkerig via `noise_band` op `ring_2_flower_patches`/`ring_2_cactus_groups` of een eigen ruis |

Volgorde binnen de stap vast: vlekken → fossielen → botten → stenen; dode bomen → begroeiing. Nieuwe placed features achteraan de lijsten van `dry_riverbeds`.

**Geen `StrandedFossilFeature` meer.** Antwoord 3 A (vanilla-fossielsjablonen half begraven) is sinds 30c gebouwd als `surface_fossil` + `FossilLayout`, dat precies de eisen van dit plan haalt (footprint binnen de eigen chunk, hele lagen begraven, verwering zonder zwevende brokken, getest op de echte sjablonen). Een tweede feature zou dubbel werk zijn. De unit test van de footprint (`SurfaceFossilFeatureTest`) bestaat al.

### 6. Ertsen in de zone (vraag 4 A)

- `dry_riverbeds` komt in `#is_ring_2` en krijgt zo `add_ring_2_ores` (change 25): geschaalde vanilla-ores met `progress` ring 2, dus alleen in de ring 2-helft van de zone (vanaf de verruisde grens geen signaal). Zonnesteen en turkoois komen alleen waar kloofsteen ligt (stap 4).
- De ring 3-helft is tot change 41 ertsloos voor vanilla-ores, net als `placeholder_ring_3` nu. Overdracht: change 41 hangt de ring 3-set ook aan `dry_riverbeds`. De features komen dan in alfabetische modifiervolgorde, ring 2 vóór ring 3, achteraan `UNDERGROUND_ORES`. Ring 3-biomes hebben alleen de ring 3-set, dus er ontstaat geen cyclus.
- Ore veins: blijven 0 (alleen ring 1, change 25).

### 7. Unit tests

- `RingBiomeSourceTest`: een geval met **twee** overgangen (`ring_1`, `ring_2`). Beide zones krijgen hun eigen overgangsbiome (blend ~0,01 / 0,5 / ~0,99), grens 3 wisselt nog hard op 0,5 en `possibleBiomes` bevat beide.
- `RingBlendTest`: constante invoer `{ring_3: 1, rest 0}` geeft 0 in ring 2, `smoothstep(blend)` in de zone 2→3, 1 in ring 3, 0 in de zone 1→2. Codec-rondreis met getallen als invoer (`StandaloneHolderGetter.ops`).
- `BiomeAmbienceTest` (of een nieuw `TransitionBiomesTest`): `dry_riverbeds` heeft geen neerslag, temperatuur 2,0 en lucht/fog/water = `Palette.RING_2.desaturated(DRY_RIVERBEDS_FADE)`.
- Geen `StrandedFossilFeatureTest`: `SurfaceFossilFeatureTest` (30c) dekt de footprint al. Nieuw: `Ring2BiomesTest`/`TransitionBiomesTest` uitbreiden met de volgorde en de losse lijsten van `dry_riverbeds` (eigen features achteraan hun stap, alleen chunk-lokale modifiers, geen `count_on_every_layer`/`random_patch`/vanilla-`disk_*` zonder marge), en `Ring2TerrainJsonTest` met de bedding-term (één keer per bestand genoemd, `pools_dry` in `pools.json`, `initial_height` mee).

### 8. Documentatie

- `CODEBASE.md`:
  - kopregel en status naar change 31;
  - `TransitionBiomes` (nu twee biomes);
  - nieuwe class `DryRiverbedsFeatures` (geen nieuw feature-type: `surface_fossil` en `rubble` worden hergebruikt);
  - registries (configured/placed features, biome);
  - biome-tabel (`dry_riverbeds`);
  - "Overige" (`ring_weight/overworld/ring_3`);
  - databestanden: preset `transitions.ring_2`, `ring_2/overworld/{riverbed_mask,riverbeds}.json`, noise `dry_riverbeds_channel`, `height.json`/`flats_height.json` gewijzigd, surface-rule-tak, lagen- en zandgrotlijst;
  - valkuilen: (1) een ring-gewicht als density function is `ring_blend` met constanten, geen Java; (2) terrein en surface rule delen een ruis via `xz_scale` 1 / `y_scale` 0 + `noise_threshold`; (3) vanilla-fossielen liggen altijd begraven.
- `fase-1-wereld.md`:
  - §2 Ring 2: **Uitwerking (change 31)** — beddingen (vorm, diepte, klem Y 66, droge poelen), bodem, verloop, fossielen, ertsen, klimaat/spawns/kleuren;
  - §1.14: "Nu overworld grens 1 en 2";
  - §1.3: de zone mag een eigen terreinvorm krijgen via het ring-gewicht (JSON), naast de smoothstep-blend.
- `02-beslissingenlog.md`: de regels hieronder.
- `docs/backlog.md`:
  - `[worldgen]` change 39: geblakerde vlekken in `dry_riverbeds` van vanilla `blackstone`/`basalt` naar as en verkoold hout (change 38); eventueel de beddingen ook in de ring 3-invoer (`ring_weight/overworld/ring_2`). (bron: change 31)
  - `[worldgen]` change 41: ring 3-ertsset ook in `dry_riverbeds` (ring 3-helft van de zone). (bron: change 31)
  - `[structures]` gestrande resten in `dry_riverbeds`: wrakken op het droge, oude bruggen en steigers over de beddingen, drooggevallen putten (fase 3); geen `has_structure/*`-tags tot dan. (bron: change 31)
  - `[atmosphere]` change 32: kleuren en grotgeluid van `dry_riverbeds` definitief. (bron: change 31)
  - `[mobs]` fase 4: ring 2-mobs ook in `dry_riverbeds`. (bron: change 31)

---

## Overdracht naar latere changes

| Change | Wat er klaarligt |
|---|---|
| 32 sfeer ring 2 | `DRY_RIVERBEDS_FADE` + `Palette.RING_2` (één plek); mood/ambient van `dry_riverbeds` |
| 37 textures | `cracked_clay`, `scorched_sandstone` zijn hier in grote vlakken te zien: goede testplek; `bone_block`-fossielen en `coarse_dirt`/`red_sand` liggen er naast de eigen planten (dorre struik, tonnetjescactus) naast vanilla `dead_bush`: beoordelen of de eigen planten genoeg afsteken |
| 38/39 ring 3 | het zonemasker-patroon (`ring_weight/overworld/ring_N`); vlekken van vanilla- naar asblokken; ring 3-terrein mengt met de beddingen aan de ring 2-kant |
| 41 ores ring 3 | `dry_riverbeds` ook de ring 3-set (stap 6) |
| 48/69 overgangen 3→4, 4→5 | hetzelfde patroon: `transitions.ring_N`, `TransitionBiomes`, `<Naam>Features` met `ring_chance transition`, zonevorm via `ring_weight` in de binnenring-invoer |

---

## Client/server-impact

- Puur worldgen, server-side en deterministisch per seed. De ruis is geseed via een eigen noise-key, het ring-gewicht via `RingLayout` (wereldseed, `RingColumnCache` ThreadLocal), en de feature-random is vanilla's geseede decoration-random. Geen netwerk, geen SavedData of attachments, geen client-code.
- Biome-kleuren gaan via vanilla's registry-sync naar de client. De client kent de beddingen alleen als blokken.
- `surface_fossil` (30c) leest vanilla-structure-templates via de server (`getStructureManager()` vanuit de worldgen-thread, zoals vanilla's `FossilFeature`). Alleen tijdens worldgen.
- Bestaande testwerelden krijgen naden in de zone 2→3 (nieuwe biome, nieuw terrein). Test altijd in een nieuwe wereld.

---

## Tests

Bouwplan: **B, D, W**. Aanbevolen ook **S** (preset, codecs, surface rule en feature-volgorde worden pas bij het laden gecontroleerd), met RCON-metingen. Testseed `20260930`, per wereld een nieuwe `level-name` (`smoke-change31-…`).

### B — `./gradlew build`
- [ ] De unit tests uit stap 7 en de bestaande tests (`RingBiomeSourceTest`, `RingBlendTest`, `RingChanceFilterTest`, `BiomeAmbienceTest`) zijn groen.

### D — `./gradlew runData`
- [ ] Gegenereerd en gecommit: `worldgen/biome/dry_riverbeds.json`, de configured/placed features uit stap 5, `is_ring_2` + `c:is_hot/overworld` + `c:is_dry/overworld` met `dry_riverbeds`, lang en/nl.
- [ ] In `dry_riverbeds.json` staat dezelfde vanilla-featurevolgorde als in `canyonlands.json`, met de eigen features achteraan.

### S — server + RCON (geautomatiseerd)
- [ ] `tools/server-smoke-test.sh`: start zonder fouten (geen "Feature order cycle", preset/surface rule/density functions laden, geen client-class-melding).
- [ ] **Biome:** vanaf `/ca tpboundary 2` in 4 richtingen `execute if biome … corruptedadventure:dry_riverbeds` op gewicht ~0,5. Langs een straal: de wissel naar ring 2-biomes bij gewicht ~0 en naar `placeholder_ring_3` bij ~1, zonebreedte ≈ 128. Grens 1 blijft `withered_expanse`, grens 3 wisselt hard, de Nether-grens 2 wisselt hard.
- [ ] **Buiten de zone ongewijzigd:** wereld A (vóór change 31) en B (na), zelfde seed. Tellingen per chunk (steen, lucht, water, `red_sand`, kloofsteen) in 4 chunks ring 2-midden, 4 chunks zone 1→2 en 2 chunks ring 3 → identiek.
- [ ] **Beddingen:** in 16 zone-chunks (ring 2-helft): `cracked_clay` > 0 in de meeste chunks. 50 punten met `execute if block` bodem = `cracked_clay` → hoogte (marker) ≥ Y 66 en 3–6 lager dan het oppervlak 12 blokken ernaast (gemiddeld).
- [ ] **Droog:** in 32 zone-chunks `fill … replace water` boven Y 60 → 0 (geen poelen, geen water in de beddingen), **ook in de eerste 10 blokken van de zone** (de eigen `pools_dry`-ramp). Onder Y 55 mag aquiferwater staan.
- [ ] **Verloop:** `scorched_patch`-blokken (`blackstone` + `basalt` aan de oppervlakte) per chunk in de binnenste vs. buitenste 40 blokken van de zone → buiten duidelijk meer. Fossiel-`bone_block` binnen > buiten.
- [ ] **Levendigheid (norm teststop 2b/2c):** planten (dorre/dode struik, cactus, droog gras) per 256 bedding-/oeverkolommen in de orde van de duindalen van 30c (4–5); botten (`bone_block`-brokken) ~1 per 5 chunks, fossielen ~1 per 25–30 chunks in de ring 2-helft, dode bomen ≥ 1 per ~4 chunks aan de ring 2-kant; geen enkel gebied van 4 × 4 chunks zonder iets (struik, bot, steen of boom) op de bedding.
- [ ] **Bedding-dekking:** `initial_height` ≥ echte bodem in 0 van 10.000 bedding-kolommen (anders mist de bedding zijn `cracked_clay`); bedding-kolommen op zout/duin-terrein (klem) apart geteld (vraag 7).
- [ ] **Ertsen:** `coal_ore`/`iron_ore` in zone-chunks aan de ring 2-kant > 0, aan de ring 3-kant 0 (tot change 41).
- [ ] **Determinisme:** twee verse werelden, de tweede in omgekeerde volgorde geladen (zoals change 16). Per chunk (24 zone-chunks) tellingen van `bone_block`, `cracked_clay`, `blackstone`, `basalt`, `dead_log`, `dry_shrub` → identiek.
- [ ] **Performance** (JFR, stap `noise`, zoals change 9): zone 2→3 vóór en na → hooguit een paar procent erbij, binnen het budget van **1,75× vanilla** (23b-9 C, gedeeld; hoogste meetpunt nu 1,54× op x 2.500). Vergelijk met een git-worktree van `HEAD`, zoals 30–30c. Getal in `CODEBASE.md`.

### W — blokkerende handmatige test: **nee** → W-achteraf
**Reden:** geen latere change bouwt direct op de vorm of de inhoud van de beddingen. Change 32 (sfeer) en 37 (textures) raken alleen kleuren en pixels. Change 39 herziet de zone toch, als ring 3 echt terrein krijgt. Alles wat meetbaar is (biome, zonebreedte, droog, diepte, verloop, ertsen, determinisme, naden) staat onder S. Wat overblijft is smaak en afstemming, en die kan achteraf.

W-achteraf (runClient, testseed, checklist):
- [ ] `/ca tpboundary 2` in 4 richtingen: de beddingen lezen als lege rivierbeddingen (breedte, meanderen, oevers), niet als geverfde linten. Geen verticale wanden, zwevende brokken of gaten.
- [ ] Bodem: klei in de bedding, grind-vlekjes, grove oevers, rood zand erbuiten, `scorched_sandstone` op hellingen. Aan de ring 3-kant duidelijk zwart geblakerd.
- [ ] Fossielen: half begraven, niet zwevend, niet geheel onder de grond; niet te vaak.
- [ ] Kloven die de zone in lopen houden hun lagen, zonder lelijke naad.
- [ ] F3: `dry_riverbeds`; geen regen als het in ring 1 regent; kleuren iets grauwer dan ring 2.
- [ ] **Levendig, niet leeg** (norm van teststop 2b/2c): loop 2 minuten over een bedding en de oevers; er is steeds iets te zien (struik, bot, fossiel, steen, dode boom). Vergelijk met de duindalen van 30c. Geen kale kilometers.
- [ ] Op de vlakte/het zout in de zone: de bedding leest (afhankelijk van antwoord 7) als geul of lint, zonder gaten of water.
- [ ] Zonegrens (9): de bodemwissel (zoutkorst/gras → rood zand/klei) is acceptabel.
- [ ] Spawns zoals canyonlands ('s nachts husks e.d., overdag konijnen).
- [ ] Portal in de zone → Nether ÷4 en terug (standaard-W).
- [ ] Geen fouten of waarschuwingen in de log bij het aanmaken van de wereld.

---

## Risico's / aandachtspunten

- **Change 23 wijkt af** (geen 2D-`height.json`, andere poel-term, geen `Ring2Biomes.generation`): stap 2 en 3 aanpassen op de echte opbouw. Zonder hoogte-domein → vraag 1 B en een melding.
- **Surface rule en terrein uit de pas:** als `noise_threshold` toch anders sampelt dan `minecraft:noise` (verifiëren, Achtergrond), ligt de klei naast de bedding. Dan in de surface rule de bodem herkennen via een eigen surface-ruis-band (zelfde key, of de bedding als `stone_depth` + hoogte), of vraag 1 C.
- **Helling in de zone:** het gemengde terrein gaat naar Y ~120 (placeholder ring 3): vanaf de vlakte stijgend, vanaf het plateau (Y 125–140) licht dalend. Beddingen liggen dwars op de helling en zijn ondieper aan de ring 3-kant. Dat is geaccepteerd: change 39 herziet het.
- **Feature order cycle:** `dry_riverbeds` deelt vanilla-features met de 52 kopieën en de ring 2-biomes. Dezelfde helper en de eigen features achteraan verkleinen het risico; S vangt het.
- **Fossiel in de buurchunk:** de footprint vanaf de chunk-minhoek kan tot 16 blokken in de buurchunk reiken. De unit test bewaakt de 3×3-grens. Een buurchunk die later decoreert kan nog een dode boom in of naast een fossiel zetten (zoals bij `bare_patch`): geaccepteerd en gedocumenteerd.
- **Vanilla-blokken als "geblakerd"** (`blackstone`, `basalt`) zijn minable en lijken op Nether-blokken. Bewuste placeholder tot de asblokken van change 38 (backlog).
- **`is_ring_2` voor een overgangsbiome:** systemen die later "alle ring 2-biomes" via de tag kiezen, krijgen de zone mee. Vastleggen in het design-doc: `is_ring_2` = "biomes met ring 2-worldgen".

---

## Ontwerpvragen

Ronde 1 = vorm en bodem (1, 2, 6), ronde 2 = inhoud (3, 4, 5).

### 1. Hoe krijgen de beddingen vorm in het terrein?
- **A. Ondiepe geulen in de ring 2-hoogte, met een zonemasker in JSON (aanbevolen).** Het masker is een `ring_blend` met constante invoer (ring 3 = 1). Beddingen van 3–6 diep en 15–40 breed, nooit onder Y 66, alleen in de zone. Geen nieuwe Java, en de initiële dichtheid en aquifers kloppen vanzelf. Herbruikbaar voor 3→4 en 4→5. De beddingen lopen richting ring 3 dood, tot change 39 ze eventueel doortrekt.
- **B. Geen terreinwijziging, zoals `withered_expanse`.** De beddingen zijn alleen meanderende linten gebarsten klei (surface rule) met features. Het eenvoudigst en volgt change 11 letterlijk. Het leest wel als geverfde linten in plaats van geulen, en de oasepoelen van change 23 blijven met water in de zone staan.
- **C. Eigen terreininvoer per overgangszone (`RingBlend` krijgt `transitions`, zoals `RingBiomeSource`).** De zone krijgt een eigen vorm (bijv. een brede lage vallei vol beddingen) die aan beide randen mengt. Dit geeft de meeste vrijheid, maar vraagt Java (codec, mengformule, tests), extra rekenwerk per zone en een nieuwe naadcontrole aan beide kanten.

Antwoord: **A** — ondiepe geulen in de ring 2-hoogte met een zonemasker in JSON; beddingen 3–6 diep (relatief aan het lokale oppervlak, dus ook bij de diepe hoogtes van 23 vraag 5 B ongewijzigd) en 15–40 breed, nooit onder Y 66.

### 2. Bodem van de zone en het "zwart geblakerd" richting ring 3
- **A. Bedding gebarsten klei met grindvlekjes, oevers grove aarde, daarbuiten rood zand, `scorched_sandstone` op hellingen; geblakerde vlekken van vanilla `blackstone`/`basalt`/`scorched_sandstone` via `ring_chance` (0 → 0,9 richting ring 3); poelen in de zone vallen droog (aanbevolen).** Leest meteen als "drooggevallen" en als voorbode van ring 3. Het zelfde patroon als de zandvlekken van `withered_expanse`. Vanilla-blokken zijn een placeholder tot de as van change 38 (backlog).
- **B. Als A, maar geblakerd alleen met `scorched_sandstone` en grove aarde (geen vanilla-donkere blokken).** Rustiger en minder "Nether-achtig", maar het zwart blakeren is nauwelijks zichtbaar.
- **C. Als A zonder geblakerde vlekken; het blakeren komt pas in change 39 met de asblokken.** Minder werk nu en geen placeholder-blokken, maar de zone mist tot dan een van zijn drie kenmerken.

Antwoord: **A** — bedding gebarsten klei met grindvlekjes, oevers grove aarde, rood zand erbuiten, `scorched_sandstone` op hellingen; geblakerde vlekken van vanilla `blackstone`/`basalt`/`scorched_sandstone` via `ring_chance`; poelen in de zone vallen droog.

### 3. Hoe maken we de gestrande botten?
- **A. Vanilla-fossielsjablonen (ruggengraat, schedel) half begraven in de bodem, via een kleine eigen feature `stranded_fossil` (aanbevolen).** Leest direct als "gestrande botten" en gebruikt vanilla-assets (geen eigen NBT). Deterministisch en binnen 3×3 chunks (unit test). Eén Java-class + config + test.
- **B. Alleen data: losse botblok-stompen (1–3 hoog) in kleine groepjes.** Geen Java, maar het leest eerder als palen dan als een skelet.
- **C. Vanilla `fossil_upper` ongewijzigd.** Geen Java, maar vanilla begraaft fossielen 15–25 blokken diep: aan de oppervlakte zie je niets.

Antwoord: **A** — vanilla-fossielsjablonen half begraven via een kleine eigen feature `stranded_fossil`.

*Bijgewerkt 2026-10-05 (geen nieuwe keuze, uitvoering):* de feature uit dit antwoord bestaat sinds change 30c als `surface_fossil` (+ `FossilLayout`, configured `desert_fossil`, plus `bone_rubble`). Change 31 bouwt geen tweede feature, maar een configured `riverbed_fossil` met dezelfde sjablonen en een bedding-bodem als grond. Betekenis van antwoord A blijft: vanilla-sjablonen, half begraven, chunk-lokaal.

### 4. Welke ertsen krijgt de zone?
- **A. De ring 2-set via `#is_ring_2` (geschaalde vanilla-ores alleen in de ring 2-helft, zonnesteen/turkoois in kloofsteen); de ring 3-helft volgt in change 41 (aanbevolen).** De ertsen volgen de ring onder je voeten, zonder nieuwe features. De ring 3-helft is tijdelijk ertsarm, net als placeholder ring 3.
- **B. Volle vanilla-ores, zoals `withered_expanse`.** Het eenvoudigst, maar dan ligt er een strook van 128 blokken die rijker is dan ring 2 en ring 3 eromheen: een exploit.
- **C. Een eigen verloop via `transition` (ring 2-factor → ring 3-factor).** De zachtste overgang, maar de ring 3-factoren moeten nu al gekozen worden (change 41) en `RingVanillaOres` moet uitgebreid worden.

Antwoord: **A** — de ring 2-set via `#is_ring_2`; de ring 3-helft volgt in change 41.

### 5. Klimaat, spawns en grotgeluid van `dry_riverbeds` (kleuren: change 32 vraag 3)
- **A. Droog (2,0, geen neerslag), spawns zoals canyonlands, grotgeluid zoals ring 2 (aanbevolen).** Voelt als ring 2 die verschraalt richting de asgrijze ring 3. (Placeholder-kleur `Palette.RING_2` 30 % naar grijs zonder vraag; definitief in change 32 vraag 3.)
- **B. Als A, maar zonder dieren (alleen monsters).** Een "dode" strook die past bij het verhaal, maar zonder voedsel in een ring met uitdroging.
- **C. Als A, met het vreemde grotgeluid (`STRANGE`) zoals `withered_expanse`.** Horror bij elke grens, maar dat is sfeerwerk dat beter in change 32 beslist kan worden.

Antwoord: **A** — droog (2,0, geen neerslag), spawns zoals canyonlands, grotgeluid zoals ring 2.

### 6. Wat ligt onder en in de wanden van de zone?
- **A. De kloofsteenlagen van canyonlands lopen door onder `dry_riverbeds` (biome-lijst van het lagenblok) (aanbevolen).** Kloven die de zone in lopen houden hun lagen, en de beddingen snijden er zichtbaar door. Zonnesteen en turkoois liggen ook daar. Aan de oase-kant geeft dat onder het oppervlak een naad van zand naar lagen (alleen zichtbaar in wanden).
- **B. Een eigen ondergrond: rode zandsteen en `scorched_sandstone`.** Eén geheel per zone, maar een zichtbare naad waar kloofsteenwanden de zone in lopen.
- **C. Vanilla steen.** Geen werk, maar grijze wanden midden in een rode woestijn, met een harde naad aan de canyon-kant.

Antwoord: **A** — de kloofsteenlagen van canyonlands lopen door onder `dry_riverbeds`.

### Nieuwe vragen na de bouw van 30–30c (2026-10-05, beantwoord: alles A)

### 7. Wat doen de beddingen op lage terreinregio's (zout, duindal, oase-vlakte)?
Sinds 30 ligt er in de zone ook zout (Y 66), duindalen (66–67) en oase-vlakte (66–74). De klem ≥ Y 66 (antwoord 1 A) laat daar 0–6 diepte over; de bedding verdwijnt als geul.
- **A. Klem blijft 66; op lage regio's alleen een gebarsten-klei-lint via de surface rule (zelfde ruis), echte geulen waar het terrein ≥ ~Y 70 ligt (aanbevolen).** Laagste complexiteit en geen waterrisico. Leest op plateau, heuvels en duinruggen als geul, op de vlakte als een plat lint klei. Multiplayer: geen invloed.
- **B. Klem naar Y 64 (1 boven zeeniveau 63).** Ook op de vlakte een geul van 2–3 diep. Risico: water in een bedding bij een lage terreinruis, en de preliminary-surface-controle wordt strakker (meer metingen). Leest het meest als rivierbedding.
- **C. Zone-regio's dempen: zout/duin/oase-gewichten × (1 − zone-ramp), zodat de zone een eigen vlakke bedding-vallei wordt.** Alleen JSON, maar de duinen en het zout stoppen dan abrupt voor de zone en de zone wordt een eigen landschap (dat was antwoord 1 C, afgewezen). Speelgevoel: de zone is overal "brede lege vlakte"; verlies van ring 2-regio-variatie in de zone.

Antwoord: **A** — klem blijft Y 66; op lage regio's (zout, duindal, oase-vlakte) alleen een gebarsten-klei-lint via de surface rule met dezelfde ruis, echte geulen waar het terrein ≥ ~Y 70 ligt.

### 8. Hoe levendig worden de beddingen?
Het plan stamt van vóór teststop 2b ("alles te kaal"). Nu is "levendig" de norm.
- **A. Matig, op het niveau van de duindalen van 30c (aanbevolen):** droge struiken/dode struiken/cactusgroepjes en kort droog gras op oevers en harde bodem, botten ~1 per 5 chunks, fossielen ~1 per 25–30 chunks, dode bomen langs oevers, keien, geblakerde vlekken richting ring 3 (tabel in stap 5). Gemeten in S. Complexiteit laag (alleen bestaande features en modifiers); geen multiplayer-impact; "drooggevallen maar niet leeg".
- **B. Minimaal, zoals het oorspronkelijke plan:** 3 flora-features (~4 pogingen per chunk), fossielen en dode bomen. Minste werk en snelste noise/features-stap, maar na teststop 2b waarschijnlijk te kaal; "verhaal van het gestolen water" komt minder over.
- **C. Rijk: A plus rotsuitstekels (`outcrop`) als oriëntatiepunt, stapels `dead_log`/botten als "wrak" en kleine blokkenrijen steen langs de oevers.** Sterkste sfeer en oriëntatie, maar meer afstemming (features-stap +0,1–0,2 ms) en een kleine kans dat het op structures van fase 3 vooruitloopt.

Antwoord: **A** — matig, op het niveau van de duindalen van 30c (struiken, cactusgroepjes, droog gras, botten ~1 per 5 chunks, fossielen ~1 per 25–30 chunks, dode bomen, keien, geblakerde vlekken; tabel in stap 5).

### 9. Wat gebeurt er met de bodem aan de zonegrens (zoutkorst, oasegras)?
De zone is één biome met één bodemtak; zoutkorst, oasegras en poelranden van ring 2 stoppen op de grens. De `dry_riverbeds`-tak legt overal rood zand / grove aarde / gebarsten klei.
- **A. Harde bodemwissel accepteren (aanbevolen).** Geen extra werk; de biomewissel is toch al een rand (net als `withered_expanse`). Leest als "hier begint het uitgedroogde gebied". Biome-kleur (change 32) maskeert de rand.
- **B. Zachte rand: in de buitenste ~20 blokken van de zone vlekken van de ring 2-bodem (`ring_chance`-features met `transition` 0 → 1, zoals de zandvlekken van `withered_expanse`).** Geen Java, wel 1–2 extra features en extra determinisme-metingen; zoutkorst/oasegras kunnen dan niet als vlek (ze zijn surface-bepaald), dus alleen zand/klei-vlekken.
- **C. Zout- en oaseterrein blijven hun eigen bodem houden in de zone (surface-rule-conditie op `salt_weight`/`moisture`-ruis).** Meest samenhangend, maar de zone is dan niet meer "overal gebarsten klei" en de surface rule krijgt extra ruisconditie (kleine noise-stap/surface-kosten); vraagt een heroverweging van antwoord 2 A.

Antwoord: **A** — harde bodemwissel aan de zonegrens accepteren; de biome-kleur van change 32 maskeert de rand.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | worldgen | Overgangsbiome 2→3 `dry_riverbeds` via `transitions.ring_2` + `TransitionBiomes` (patroon change 11); de Nether-grens 2→3 blijft hard | Eén aanpak voor alle overgangen |
| 2026-10-01 | worldgen | Beddingen: ondiepe geulen (3–6 diep, 15–40 breed, ≥ Y 66) in de ring 2-hoogte met een zonemasker in JSON | Geen Java; initiële dichtheid en aquifers kloppen vanzelf |
| 2026-10-01 | worldgen | Zonevorm via een ring-gewicht als density function: `ring_blend` met constante invoer (`ring_weight/overworld/ring_N`), geen Java | Herbruikbaar voor 3→4 en 4→5 |
| 2026-10-01 | worldgen | Bodem en blakeren: gebarsten klei, grove aarde, rood zand, `scorched_sandstone`; geblakerde vlekken vanilla `blackstone`/`basalt` via `ring_chance`; poelen vallen droog | Leest als drooggevallen en als voorbode van ring 3 |
| 2026-10-01 | worldgen | Gestrande botten: vanilla-fossielsjablonen half begraven via de bestaande feature `surface_fossil` (30c) met een eigen config `riverbed_fossil`; geen tweede feature | Leest direct als skelet; deterministisch en chunk-lokaal; hergebruik i.p.v. dubbel werk |
| 2026-10-01 | worldgen | Ertsen in de zone 2→3: ring 2-set via `#is_ring_2`; ring 3-helft in change 41 | Ertsen volgen de ring; geen exploit-strook |
| 2026-10-01 | worldgen | Klimaat/spawns `dry_riverbeds`: droog, spawns als canyonlands, grotgeluid als ring 2 (kleuren in change 32) | Ring 2 die verschraalt richting ring 3 |
| 2026-10-01 | worldgen | Ondergrond zone 2→3: kloofsteenlagen lopen door (lagenblok-lijst) | Kloven houden hun lagen in de zone |
| 2026-10-01 | worldgen | `dry_riverbeds` zonder `minecraft:`-tags (geen vanilla-structures tot fase 3); in de zandgrot-lijst van change 26 | Structures zijn fase 3; afspraak change 26 |

Commit (na `/code-review`): `worldgen: add dry riverbeds transition biome between ring 2 and 3`.

---

## Bouwnotities (2026-10-05)

Gebouwd volgens het plan, met deze afwijkingen en uitvoeringsbesluiten (alles in de categorie `worldgen`):

- **De snede zit in beide terreininvoeren (afwijking, uitvoering van antwoord 1 A).** Het plan sneed alleen de ring 2-hoogte. Gemeten in de eerste wereld: de beddingen waren in de zone bijna onzichtbaar (gemiddeld 0,5 diep, op vlak terrein 0). Oorzaak: ring 2's eindwaarde loopt door `squeeze(0,64 · x)`, die verzadigt zodra het gemengde oppervlak meer dan ~12 blokken van ring 2's eigen hoogte ligt (de hele zone: ring 3 trekt het oppervlak naar Y ~120). Een snede in een verzadigde invoer verschuift niets. Oplossing, nog steeds JSON: `ring_3/overworld.json` (placeholder) krijgt `− 1/64 × riverbed_cut` (1/64 = helling van zijn gradiënt per blok), zodat de bedding in de hele zone `cut` blokken diep is. Het masker `riverbed_mask` dooft uit bij ring 3-gewicht 0,8 → 0,98 (geen beddingen in ring 3 zelf). Change 39 herziet de placeholder (backlog).
- **Extra bestanden:** `riverbed_cut.json` (`flat_cache(mask × riverbeds)`, gedeeld door `height`, `initial_height` en `ring_3/overworld`), `pools_dry.json` (de eigen ramp). `height`/`initial_height` zijn wikkels om `height_base`/`initial_height_base`; `initial_height` krijgt de bedding-term over de hele functie (monotoon in `h`, dus `initial_height ≤ height` blijft gelden en de bedding krijgt zijn oppervlakteregels).
- **Surface rule:** de bedding-tak staat als element [6] van `above_preliminary_surface`. De oever-`scorched_sandstone` (steil + `|n| < 0,07`) geldt alleen voor de bovenlaag; de grove-aarde-vlekken kregen een smaller venster (surface-ruis −0,35…0, ~30 % i.p.v. −0,5…0,2 = 60 %, gemeten).
- **Features:** `riverbed_dead_trees` (eigen `tree`-configs met grove aarde onder de stam, geen verdorde aarde) in plaats van `dead_tree` hergebruiken: op rood zand of klei zou de verdorde aarde van ring 1 verschijnen, en grove aarde-alleen als grond liet te weinig bomen toe. Extra: `riverbed_scorched_speck` (kleine spikkels naast de grote vlekken) en `dry_riverbeds_grass`/`_cactus_groups` apart (de plan-tabel noemt ze in één regel). Fossielgrond bevat ook `gravel` (de grindvlekjes van de bedding).
- **Determinisme / "buiten de zone ongewijzigd":** zie "Resultaten S" hieronder. Een nieuwe placed feature verschuift de decoratie-seeds (feature-index) van alle features erna, dus een letterlijke blokvergelijking buiten de zone is zinloos; vergeleken is de deep-solid-mask (terrein), die identiek is.

## Resultaten (B/D/S, 2026-10-05)

- **B:** `./gradlew build` groen, 333 tests (0 fouten; `ClientIsolationTest` groen). Nieuw/uitgebreid: `Ring2BiomesTest` (6 biomes, `dry_riverbeds`), `RingBiomeSourceTest` (twee overgangen), `RingBlendTest` (constante invoer = ringgewicht), `Ring2TerrainJsonTest` (wikkels, `pools_dry`, `ring_weight`, gedeelde ruis en knikken van terrein en surface rule, snede in ring 3).
- **D:** `./gradlew runData` geslaagd; gegenereerd en meegecommit: `biome/dry_riverbeds.json`, 8 configured + 9 placed features, tags `is_ring_2`/`c:is_hot`/`c:is_dry`, lang en/nl. Vanilla-volgorde in `dry_riverbeds.json` gelijk aan de andere ring 2-biomes, eigen features achteraan (`Ring2BiomesTest`).
- **S:** `tools/server-smoke-test.sh` (`level-name=smoke-change31`, seed 20260930): start zonder fouten ("Feature order cycle", preset, surface rule en density functions laden; geen client-class-melding). Eén client (`runClient -PquickPlay`, Dev) joint een dedicated server, staat na `/tp` in de zone (`execute if biome` = `dry_riverbeds`), geen client-fouten. Twee clients niet apart gedaan: puur worldgen, geen netwerk, de client kent de zone alleen als blokken en als biome-kleur via de registry-sync. Metingen op de strook x 3.376–3.615, z 0–511 (480 chunks, meetwerelden met de eindcode, uitgelezen uit de regiobestanden, RCON voor `forceload`/`save-all`/`locate`):
  - **Biome:** `/locate biome corruptedadventure:dry_riverbeds` en `withered_expanse` vinden de zones; langs +x ligt de zone bij z 0 op x 3.388–3.571 (183 breed), verder noordwaarts 75–160 breed (grensruis). Grens 1 blijft `withered_expanse`.
  - **Droog:** 0 × `water` boven Y 60 in alle chunks die de zone raken (ook de eerste 10 blokken).
  - **Beddingen:** vlakke bodem met `cracked_clay` (8,8 % van de zone-kolommen), op vlak terrein 5–6 blokken onder de oevers (`3510 104`: Y 116–117 t.o.v. 122, gemeten: gemiddeld 3,1 in het midden van de zone, 1,4 in het uitdovende laatste kwart); bedding 15–40 breed. Eerste gemeten wereld (snede alleen in ring 2): 0,5 diep, daarom de aanpassing in "Bouwnotities".
  - **Bodem (zone-kolommen):** rood zand 41,0 %, grove aarde 36,6 %, klei 8,8 %, blackstone/basalt/`scorched_sandstone` 11,2 % (laatste kwart 20 %), kloofsteen 1,5 %; geen kolom met kale `stone`/`dirt` als bovenste blok (bedding-dekking: geen oppervlakteregel gemist). Verloop blakeren: `blackstone`+`basalt` per chunk 1,8 → 10,8 → 21,6 → 34,7 over de vier kwarten van de zone.
  - **Levendigheid:** botten (losse brokken) ~1 per 5 chunks, fossielen ~1 per 28 chunks (53/45/41/41/35/… botten), dode bomen (stammen) gemiddeld 1,5–2,3 blokken per chunk aan de ring 2-kant (~0,4 bomen per chunk), 1,2–1,6 in het laatste kwart; planten (struik, dode struik, tonnetjescactus, cactus) ~4,8 per 256 kolommen, gras erbij 6,5; geen enkel 4 × 4-chunkgebied zonder struik, bot, steen of boom (78 vensters, kleinste som 125).
  - **Ertsen:** ring 2-helft (eerste twee kwarten): kolen/ijzer/koper/goud/redstone/diamant/zonnesteen aanwezig; derde kwart enkele resten (rand van de ring 2-features), laatste kwart 0.
  - **Determinisme:** twee verse werelden, de tweede in omgekeerde tegelvolgorde geladen (24 tegels van 5 × 4 chunks): per blok identiek voor alle blokken die onze features en bodemregels leggen (bone_block, cracked_clay, blackstone, basalt, dead_log, dry_shrub, dead_bush, gras, cactus, cobblestone, scorched_sandstone, coarse_dirt, red_sand) in alle 480 chunks. Verschillen blijven er alleen in vanilla-ertsblobs (granite/diorite/andesite/tuff/ertsen/grind, vanilla-gedrag: overlappende `ore`-features).
  - **Buiten de zone:** `smoke-change31-new` (na) tegen een worktree van `HEAD` (vóór), 5 groepen van 4 × 4 chunks (ring 2-midden x 2.500, zone 1→2 x 1.500, vlak vóór de zone, ring 3 x 4.500 en x 5.300): de deep-solid-mask (Y −40…55: terrein, grotten, aquifers) is in alle 80 chunks identiek (0 verschillen); in ring 3 zijn alle blokken identiek. Features in ring 2 en zone 1→2 verschillen wél: nieuwe placed features verschuiven de decoratie-seeds van alle features erna (feature-index, zie valkuil "Feature-index ook binnen ring 2"); twee nieuwe werelden met dezelfde code zijn identiek in die chunks.
  - **Prestaties** (JFR `minecraft.ChunkGeneration`, stap `noise`, tweede ronde, chunks x 211–226, z −16…−1 (zone 2→3 met een stuk ring 2 en ring 3), 3 rondes per variant afwisselend, vóór = git-worktree van `59745a5`; de machine was druk, spreiding ±20 %): vanilla-preset gemiddeld 59,7 ms (53,6 / 57,6 / 67,9), mediaan 38,7; vóór **68,5** (66,1 / 82,8 / 56,6), mediaan 61,0; na **62,6** (55,1 / 68,2 / 64,5), mediaan 59,5. Na ÷ vóór = 0,91 (gemiddelde) / 0,98 (mediaan): de snede kost niets meetbaars; t.o.v. vanilla 1,05× (gemiddelde) / 1,54× (mediaan), onder het budget van 1,75×. Hele chunk 102,1 → 104,7 ms (vanilla 84,7).

## In-game checklist (W)

Niet blokkerend; nalopen in teststop 3. Tijd ~15 min. Kern: **leest de zone als drooggevallen rivierbeddingen, levendig en steeds zwarter richting ring 3?** Coördinaten op seed `20260930`, gecontroleerd tegen de build van change 31 (meetwerelden met dezelfde code); `x y z`, y = boven de grond. De zone ligt langs de +x-as bij z = 0 tussen x ≈ 3.390 (ring 2-kant) en x ≈ 3.570 (ring 3-kant); verder noordwaarts schuift ze naar x ≈ 3.400–3.500.

**Opstarten (~2 min)**
1. `./gradlew runClient` → Singleplayer → **Create New World**. Gebruik een **nieuwe** wereld (nieuwe biome, nieuw terrein, andere feature-posities).
2. Tab *World*: World Type **Corrupted World**, Seed **20260930**. Tab *Game*: Game Mode **Creative**, **Allow Cheats: ON**. → Create.
3. `/time set noon`, `/gamerule doDaylightCycle false`, `/ca weather 2 clear 3600`. Render distance ≥ 12.

**A. Zone en biome (~3 min)**
- [ ] `/ca tpboundary 2`: je staat op de verruisde grens (blend 0,5). F3: biome `dry_riverbeds`. Loop 100 blokken westwaarts (richting ring 2) en oostwaarts: aan de ring 2-kant wissel je (rond x ≈ 3.390) naar een ring 2-biome (`dune_sea`, `canyonlands`, …), aan de ring 3-kant (rond x ≈ 3.570) naar `placeholder_ring_3`. Zone ongeveer 110–180 blokken breed.
- [ ] Kleuren iets grauwer dan ring 2 (lucht/fog); geen regen als het in ring 1 regent (`/ca weather` in de ring 2-kant is droog).
- [ ] **Zonegrens (31-9):** de bodemwissel (rood zand / zoutkorst / oasegras → grove aarde en klei) is acceptabel? Is de harde rand te opvallend (de biome-kleur van change 32 moet hem later maskeren)?

**B. Beddingen (~6 min)**
- [ ] **Bedding op vlak terrein:** `/tp @s 3510 128 104`. Een bedding van ~25–30 blokken breed (loop west-oost over `x 3490…3540`), bodem 5 blokken onder de oevers (Y 117 t.o.v. 122), vlakke bodem van gebarsten klei met grindvlekjes, oevers van grove aarde met 1–2 treden. Leest als lege rivierbedding en niet als geverfd lint? Geen verticale wanden, zwevende brokken of gaten?
- [ ] **Diepere bedding:** `/tp @s 3526 130 98` (bodem Y 117, oevers 123) en loop de bedding af (noord-zuid): meandert ze mooi, loopt ze uit richting ring 3 (rond x ≈ 3.560 verdwijnt de snede, de klei blijft als lint)?
- [ ] **Begin van de zone, bedding op een plateau/kloofrand:** `/tp @s 3430 140 280` (kleilint op het plateau Y 132, kloof met bodem Y ~93 op `3414 100 280`). Het begin van de zone (eerste kwart) heeft nog nauwelijks snede; acceptabel, of mist hier een geul? Kloofwanden die de zone in lopen houden hun lagen (rood/oranje/crème) zonder lelijke naad: kijk vanaf `3414 105 280` naar de wand.
- [ ] Geen water: nergens plassen of poelen in de zone, ook niet in de eerste 10 blokken ervan (`/tp @s 3395 140 100`). Nog wel pekel of poelen in ring 2 vlak vóór de zone.
- [ ] Bodem: klei in de bedding, grove aarde op de oevers en in vlekken, rood zand eromheen, `scorched_sandstone` op steile oevers. Past de verhouding (ongeveer 41 % rood zand, 37 % grove aarde, 9 % klei)?

**C. Geblakerd richting ring 3 (~2 min)**
- [ ] Loop van `/tp @s 3420 130 56` naar `/tp @s 3560 130 56` (West → Oost): steeds meer `blackstone`/`basalt`/`scorched_sandstone`-vlekken; aan het ring 3-einde (`3560 130 56`) duidelijk zwart geblakerd. Is dat zwart genoeg, te "Nether-achtig" (placeholder tot change 38/39)? Vergelijk met de vlekken van `withered_expanse` in zone 1→2 (`/ca tpboundary 1`).

**D. Leven (~5 min)** — norm: levendig, niet leeg
- [ ] Loop 2 minuten over een bedding en de oevers (bijv. `3510 128 104` → `3510 128 200`): steeds iets te zien (struik, bot, fossiel, steen, dode boom). Geen kale kilometers? Vergelijk met de duindalen van 30c.
- [ ] **Fossielen:** `/tp @s 3495 134 20` (groot, 53 botten), `3513 134 6`, `3480 134 42` (kleiner). Half begraven, niet zwevend, niet geheel onder de grond; niet te vaak (~1 per 25 chunks)?
- [ ] **Botten en keien:** losse botblokken (~1 per 5 chunks, bijv. `3462 128 6`) en donkere keien (cobblestone/andesiet/blackstone) op de oevers.
- [ ] **Dode bomen:** `/tp @s 3556 130 10` en `3466 130 92`: dode bomen met grove aarde onder de stam, langs de bedding; dunner richting ring 3. Genoeg (~1 per 3–4 chunks aan de ring 2-kant)?
- [ ] **Begroeiing:** dorre struiken, dode struiken, tonnetjescactussen; droog gras op grove aarde; cactusgroepje bij `/tp @s 3506 124 150`.
- [ ] Spawns zoals canyonlands ('s nachts husks e.d., overdag konijnen).

**E. Overig**
- [ ] Ertsen: in de ring 2-helft van de zone vind je kolen/ijzer (graaf of spectator onder `3430 40 100`); aan de ring 3-kant (`3550 40 100`) geen vanilla-ertsen (tot change 41).
- [ ] Portal in de zone → Nether ÷4 en terug (standaard-W).
- [ ] Geen fouten of waarschuwingen in de log bij het aanmaken van de wereld.

**Afstemwaarden om te beoordelen** (startwaarden in `DryRiverbedsFeatures`/JSON): breedte en afstand van de beddingen (`dry_riverbeds_channel` firstOctave −8, drempels 0,04/0,07), diepte (5), uitdoving van de snede (ring 3-gewicht 0,8–0,98), hoeveelheid geblakerde vlekken (`scorched_patches`/`_specks`), dichtheid van bomen, botten, fossielen en keien, verhouding grove aarde/rood zand (surface-ruis −0,35…0).
