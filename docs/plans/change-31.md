# Change 31 — Overgangsbiome 2→3 `dry_riverbeds`

Scope: `worldgen` — change 31

**Doel:** de overgangszone van ~128 blokken rond grens 2 (ring 2 → ring 3) krijgt een eigen biome, de **drooggevallen rivierbeddingen** (`corruptedadventure:dry_riverbeds`): brede lege beddingen met gebarsten klei, gestrande botten en resten, en richting ring 3 steeds meer zwart geblakerde grond. Het vertelt het verhaal van het gestolen water. We volgen het patroon van `withered_expanse` (change 11): het veld `transitions` van `RingBiomeSource`, een bootstrap in `TransitionBiomes`, features met `ring_chance` (signaal `transition`) en een eigen surface-rule-tak. Er komt geen tweede aanpak bij. Nieuw is alleen dat de zone een lichte terreinvorm krijgt (de beddingen, vraag 1). Die komt uit JSON, met bestaande types.

Bron: `fase-1-wereld.md` §1.3 en §1.14 (overgangszones, `transitions`), §1.10 (1 overgangsbiome per grens), §1.15 (`ring_blend`), §2 Ring 2 (overgangsbiome 2→3, blokken), §2 Ring 3 (aansluiting: aslandschap, geen natuurlijk water, lucht rood-oranje), bouwplan rij 31. Overdracht uit `change-23.md` ("31 `dry_riverbeds`"), `change-25.md` (ertsen in de zone), `change-26.md` (biome-lijst zandgrotten) en `change-21.md` (`cracked_clay` in `#dead_bush_may_place_on`).

Buiten scope: het echte ring 3-terrein en de ring 3-blokken (as, verkoold hout: changes 38/39), definitieve lucht/fog/ambient van ring 2 (change 32), textures (37), structures zoals wrakken en bruggen (fase 3), mobs (fase 4). De Nether-grens 2→3 krijgt geen overgangsbiome: de Nether heeft geen overgangen (§1.14).

---

## Afstemming (2026-10-01)

Afgestemd met 23, 25, 26, 30 en 32 (basis: de aanbevolen opties).
- **Plan 30 bestaat:** `height.json` heeft dan regio's (`flat + relief_weight · (rock − flat)`, klem ≥ 66); de beddingen-term komt op het eindresultaat. Hoodoo-pilaren staan op de echte `height` (30), dus niet zwevend boven een bedding.
- **Ertsen (25):** per gastgesteente; zonnesteen ook in `scorched_sandstone` (oeverhellingen) binnen Y 95–150 (doorgerekend op 23 vraag 5 B) — in de praktijk alleen bij beddingen op plateaus; op de vlakte (Y 66–74) niet.
- **Hoogtes (23 vraag 5 = B, keuze gebruiker):** plateau Y 125–140, kloofbodem ~70, vlakte Y 66–74. In de zone 2→3 mengt dat met placeholder ring 3 (Y ~120): vlaktes stijgen, plateaus zakken iets. De beddingen (3–6 diep, relatief aan het lokale oppervlak) veranderen niet.
- **`dry_riverbeds` in `Ring2Biomes.ALL` (23)**, hoewel de bootstrap in `TransitionBiomes` staat; tags komen uit die lijst.
- **Kleuren zijn hier geen vraag meer:** placeholder `Palette.RING_2.desaturated(0.3)`; de definitieve kleur is change 32 vraag 3 (`Palette.DRY_RIVERBEDS` vervangt `DRY_RIVERBEDS_FADE`). Vraag 5 gaat nu over klimaat, spawns en grotgeluid.
- **Lijsten:** lagenblok en zandgrot-tak via de controlelijst van 23 bijwerken.
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37).

---

## Afhankelijkheden en aannames

**Gecontroleerd in de code** (stand na change 17, commit `7714b77`):
- `RingBiomeSource` (`worldgen/biome/RingBiomeSource.java` r. 31–92): `transitions` is generiek. De sleutel is de binnenring (`ring_2` = grens 2→3), `void` is ongeldig, `collectPossibleBiomes` neemt de unie van `rings` en `transitions`, en `getNoiseBiome` kiest in `inTransition()` de overgangssource van `position.inner()`. **Voor grens 2 hoeft er geen Java bij**: alleen een preset-entry.
- `TransitionBiomes` (r. 27–87): bootstrap met alleen `withered_expanse`. `globalOverworldGeneration` is een private kopie van vanilla. Kleuren via `BiomeAmbience.Palette.RING_1.desaturated(0.4F)`, mood `STRANGE`. Het commentaar zegt al "één per grens".
- `RingChanceFilter.transition(Ring inner, from, to)` (r. 60, 108): het signaal werkt voor elke grens, dus `transition(RING_2, …)` = blend in de zone 2→3, anders geen signaal (kans 0).
- `RingBlend` (r. 30–31): `rings` gebruikt `DensityFunction.HOLDER_HELPER_CODEC`, dus een verwijzing, een inline functie of een **getal**. Een `ring_blend` met constante invoer (ring 3 = 1, de rest 0) geeft zo het smoothstep-gewicht van ring 3 als density function, zonder nieuwe Java (stap 2).
- `WitheredExpanseFeatures`: het sjabloon voor een feature-bootstrap per overgangsbiome (configured + placed, `ring_chance` vóór de heightmap of de biome-filter, `disk` met een `RuleBasedBlockStateProvider`).
- Surface rules (`noise_settings/overworld.json`): de tak van `withered_expanse` is het eerste element van `above_preliminary_surface`. Afspraak in `CODEBASE.md`: eigen takken komen als losse blokken bovenaan.
- `BiomeAmbienceTest` test de kleuren en de mood van `withered_expanse`. `RingBiomeSourceTest` (`transitions`) test één overgang.

**Aangenomen** (changes 18–30 zijn nog niet gebouwd; hun plannen zijn de bron). **Bij de start nalopen in `CODEBASE.md` en de code, en namen en paden hieronder aanpassen.**
- **A1 — change 21:** `cracked_clay` (in `#minecraft:dead_bush_may_place_on` en `#overworld_carver_replaceables`), `dry_shrub` (grond `#corruptedadventure:desert_plant_soil`, met `cracked_clay` erin), `scorched_sandstone`, de kloofsteentinten met tag `#corruptedadventure:canyon_stones`.
- **A2 — change 23:**
  - de ring 2-invoer in `density_function/ring_2/overworld/`, met een 2D-`height.json` (`flats + relief_weight · (plateau − flats) − relief_weight · canyon_depth`, kloofbodem en vlakte geklemd op ≥ Y 66) en `terrain.json` = `(height − y) / S`;
  - de poelen in `flats_height.json` via `ring_2_pools`, tot Y ~59;
  - `ring_blend/overworld_initial` ring 2 = `0,390625 + ring_2/overworld/terrain`;
  - `Ring2Biomes.generation(...)` als gedeelde helper (vanilla `globalOverworldGeneration` package-private, ertsenslot);
  - `BiomeAmbience.Palette.RING_2`;
  - tags `is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld`;
  - het kloofsteenlagenblok in de top-level-`sequence` met `biome_is [canyonlands]`;
  - de ring gate in `RingBlend.mapAll` (vraag 3 van change 23).

  Heeft change 23 geen 2D-hoogte (bijvoorbeeld een andere opbouw), dan geldt stap 2 op de plek waar de oppervlaktehoogte van ring 2 wordt bepaald. Is er geen hoogte-domein, dan valt vraag 1 terug op optie B.
- **A3 — change 25:** `add_ring_2_ores` (`add_features` op `#is_ring_2`, stap `UNDERGROUND_ORES`), met de geschaalde vanilla-ores (`ring_chance` `progress` ring 2: **kans 0 buiten ring 2**) en zonnesteen en turkoois (per gastgesteente een `BlockMatchTest` → variant uit change 24; zonnesteen ook in `scorched_sandstone`, dus ook in de oeverhellingen van deze zone binnen Y 95–150). In de ring 2-biomes staat `addDefaultOres` vervangen door alleen de niet-ertsen. De vanilla-ore veins staan alleen in ring 1 aan.
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

- **`ring_2/overworld/height.json`** (change 23) wordt `min(h, max(66, h − riverbed_mask · riverbeds))`. Een bedding verlaagt alleen, nooit onder Y 66, dus er staat geen zeewater in een bedding (zelfde klem als de kloofbodems van change 23). Op een plateau worden het droge geulen in de mesa, op de vlakte ondiepe linten.
- **Poelen vallen droog** (vraag 2 A): in `flats_height.json` (of waar change 23 de poel-term aftrekt) wordt die term `× (1 − riverbed_mask)`. In de zone liggen dan geen oasepoelen meer. Ondergrondse reservoirs (change 26) blijven.
- De initiële dichtheid volgt vanzelf (`ring_blend/overworld_initial` → `terrain` → `height`). Aquifers en surface rules zien dus hetzelfde oppervlak.
- **Buiten de zone verandert niets:** `riverbed_mask = 0` geeft `h` exact terug (vergelijkingstest in S).
- **Verloop:** de beddingen zitten alleen in de ring 2-invoer. Hun zichtbare diepte neemt dus af naarmate ring 3 (placeholder Y ~120) meeweegt: diep aan de ring 2-kant, aan de ring 3-kant lopen ze dood in geblakerde grond (stap 5). Change 39 kan hetzelfde masker (`ring_weight/overworld/ring_2` in de ring 3-invoer) gebruiken als de beddingen ook aan de ring 3-kant moeten doorlopen (overdracht).
- Ring gate (change 23): de geneste `ring_blend` met constanten is geen `interpolated`-marker. De gate wikkelt hem niet en hij kost alleen de cache-opzoeking. Controleren bij de bouw.

### 3. Biome `dry_riverbeds` (datagen, in `TransitionBiomes`)

- `ModBiomes.DRY_RIVERBEDS`. `TransitionBiomes.bootstrap` registreert hem naast `withered_expanse`.
- **Generation:** `Ring2Biomes.generation(...)` van change 23/25/26 (vanilla `globalOverworldGeneration` in vanilla-volgorde, alleen de niet-ertsen uit `addDefaultOres`, soft disks, vanilla-carvers, geen lavameren). De ertsen komen via de biome modifier (vraag 4). Eigen features achteraan per stap (stap 5). Zo heeft `dry_riverbeds` dezelfde vanilla-featurevolgorde als de ring 2-biomes, en dat voorkomt een "Feature order cycle".
- **Klimaat (vraag 5 A):** `temperature` 2,0, `downfall` 0, `hasPrecipitation` false.
- **Spawns (vraag 5 A):** dezelfde lijst als `canyonlands` (change 23 vraag 11). Bij voorkeur via een gedeelde helper in `Ring2Biomes`, anders een kopie.
- **Kleuren (placeholder, geen ontwerpvraag meer — afgestemd 2026-10-01; de definitieve kleur van `dry_riverbeds` is change 32 vraag 3, die `DRY_RIVERBEDS_FADE` vervangt door `Palette.DRY_RIVERBEDS`):** lucht/fog/water `BiomeAmbience.Palette.RING_2.desaturated(0.3F)` als voorbode van de asgrijze ring 3 (constante `DRY_RIVERBEDS_FADE` naast `WITHERED_EXPANSE_FADE`). Gras en blad als `canyonlands` (`BFB755`/`AEA42A`), 30 % naar grijs. Grotgeluid zoals de ring 2-biomes (change 32 beslist).
- **Tags** (`ModBiomeTagsProvider`): `corruptedadventure:is_ring_2` (vraag 4 A), `c:is_hot/overworld`, `c:is_dry/overworld`. **Geen** `minecraft:`-tags (geen `is_overworld`, geen `has_structure/*`).
- Lang: `biome.corruptedadventure.dry_riverbeds` → en_us "Dry Riverbeds", nl_nl "Drooggevallen Rivierbeddingen".

### 4. Surface rules (`noise_settings/overworld.json`, vraag 2 A, 6 A)

**Tak bovenaan `above_preliminary_surface`**, ná `withered_expanse` en de ring 2-takken (één los blok, duidelijk gemarkeerd):
```
condition(biome [dry_riverbeds],
  sequence[
    condition(stone_depth floor, offset 0,
      sequence[
        condition(steep, scorched_sandstone),                                  // oeverwanden en hellingen
        condition(noise_threshold dry_riverbeds_channel -0.04…0.04,             // beddingbodem (zelfde ruis als het terrein)
          sequence[ condition(noise_threshold surface 0.6…, gravel), cracked_clay ]),
        condition(noise_threshold dry_riverbeds_channel -0.07…0.07, coarse_dirt), // oever
        condition(noise_threshold surface -0.5…0.2, coarse_dirt),               // vlekken
        red_sand ]),
    condition(stone_depth floor, add_surface_depth,
      sequence[ condition(steep, scorched_sandstone), red_sandstone ]) ])
```
Startdrempels voor de surface-ruis worden in W bijgesteld (geen ontwerpvraag). De `noise_threshold`-drempels van de bedding moeten gelijk zijn aan de spline-knikken van `riverbeds.json` (één bron: een commentaarregel in beide bestanden).

**Ondergrond (vraag 6 A):** `dry_riverbeds` komt in de `biome_is`-lijst van het kloofsteenlagenblok van change 23. Kloven die de zone in lopen, houden zo hun lagen, en de beddingen snijden door de lagen. **Zandgrotten** (change 26, afspraak): `dry_riverbeds` komt in de `biome_is`-lijst van die tak.

### 5. Features (datagen): `worldgen/feature/DryRiverbedsFeatures.java` (sjabloon `WitheredExpanseFeatures`)

Keys in `ModConfiguredFeatures`/`ModPlacedFeatures`. Alles chunk-lokaal (`in_square`, geen `random_patch` of `count_on_every_layer`: afspraak change 23, valkuil "Generatievolgorde").

| Configured | Type | Inhoud |
|---|---|---|
| `scorched_patch` | `minecraft:disk` | geblakerde vlek (vraag 2 A): `blackstone` 3, `basalt` 2, `scorched_sandstone` 2 (alleen gesteente, geen ertsen); `RuleBasedBlockStateProvider` met `scorched_sandstone` boven lucht (zoals de zandvlek). Doel: `red_sand`, `coarse_dirt`, `cracked_clay`, `gravel`. Straal 2–5 |
| `stranded_fossil` | eigen `corruptedadventure:stranded_fossil` (vraag 3 A) | vanilla-fossielsjablonen (`spine_1…4`, `skull_1…4`; `_coal`-overlays niet: kolen aan de oppervlakte zijn een exploit) half begraven in de bodem |
| `riverbed_vegetation` | `minecraft:simple_block` | `dry_shrub` 3, `dead_bush` 2 (grond via `would_survive`) |

| Placed | Stap | Plaatsing |
|---|---|---|
| `dry_riverbeds_scorched_patches` | `LOCAL_MODIFICATIONS` (vóór de begroeiing) | `count` 3 → `in_square` → `ring_chance` (`transition`, `ring_2`, 0 → 0,9, `exponent` 1,5) → heightmap `WORLD_SURFACE_WG` → `biome` |
| `dry_riverbeds_fossils` | `SURFACE_STRUCTURES` | `rarity_filter` 12 → `in_square` → `ring_chance` (`transition`, `ring_2`, 1,0 → 0,3) → `biome` (Y bepaalt de feature zelf) |
| `dry_riverbeds_dead_trees` | `VEGETAL_DECORATION` | **hergebruik** `corruptedadventure:dead_tree` (change 11): `rarity_filter` 3 → `in_square` → `surface_water_depth_filter` 0 → heightmap → filter `#dirt` eronder (oevers: `coarse_dirt`) → `ring_chance` (`transition`, `ring_2`, 0,8 → 0,1) → `biome` |
| `dry_riverbeds_vegetation` | `VEGETAL_DECORATION` | `count` 4 → `in_square` → heightmap → `block_predicate_filter` (lucht + `would_survive(dry_shrub)`) → `biome` |

Aantallen zijn startwaarden (W).

**`StrandedFossilFeature`** (vraag 3 A; `worldgen/feature/`, registratie `ModFeatures`, config `StrandedFossilConfiguration`: lijst van template-ids, `sink` 1–3, `max_empty_corners`):
- Kies een sjabloon en rotatie met de feature-random. De footprint komt uit `StructureTemplate.getSize` + rotatie, met als hoek de **chunk-minhoek** van de herkomst (zoals vanilla's `FossilFeature`). Zo blijft hij altijd binnen de 3×3 chunks (valkuil "Features schrijven alleen in de 3×3 chunks"; grootste fossiel ≤ 16).
- Y = laagste `WORLD_SURFACE_WG` over de footprint − `sink`, dus half begraven. Geen plaatsing als meer dan `max_empty_corners` hoeken boven lucht of vloeistof liggen (zoals vanilla).
- Plaatsen met `StructurePlaceSettings` (`BlockIgnoreProcessor.STRUCTURE_AND_AIR`, alleen bot), `Block.UPDATE_INVISIBLE`, geen vervanging van vloeistof.
- Leest alleen de heightmap van de eigen footprint. De generatievolgorde kan die nog veranderen (een boom van een buurchunk op de heightmap). Daarom `WORLD_SURFACE_WG`, die tijdens de features-stap alleen het terrein toont (de keuze van change 23 volgen) — en de determinismetest in S.
- Unit test (stap 7).

### 6. Ertsen in de zone (vraag 4 A)

- `dry_riverbeds` komt in `#is_ring_2` en krijgt zo `add_ring_2_ores` (change 25): geschaalde vanilla-ores met `progress` ring 2, dus alleen in de ring 2-helft van de zone (vanaf de verruisde grens geen signaal). Zonnesteen en turkoois komen alleen waar kloofsteen ligt (stap 4).
- De ring 3-helft is tot change 41 ertsloos voor vanilla-ores, net als `placeholder_ring_3` nu. Overdracht: change 41 hangt de ring 3-set ook aan `dry_riverbeds`. De features komen dan in alfabetische modifiervolgorde, ring 2 vóór ring 3, achteraan `UNDERGROUND_ORES`. Ring 3-biomes hebben alleen de ring 3-set, dus er ontstaat geen cyclus.
- Ore veins: blijven 0 (alleen ring 1, change 25).

### 7. Unit tests

- `RingBiomeSourceTest`: een geval met **twee** overgangen (`ring_1`, `ring_2`). Beide zones krijgen hun eigen overgangsbiome (blend ~0,01 / 0,5 / ~0,99), grens 3 wisselt nog hard op 0,5 en `possibleBiomes` bevat beide.
- `RingBlendTest`: constante invoer `{ring_3: 1, rest 0}` geeft 0 in ring 2, `smoothstep(blend)` in de zone 2→3, 1 in ring 3, 0 in de zone 1→2. Codec-rondreis met getallen als invoer (`StandaloneHolderGetter.ops`).
- `BiomeAmbienceTest` (of een nieuw `TransitionBiomesTest`): `dry_riverbeds` heeft geen neerslag, temperatuur 2,0 en lucht/fog/water = `Palette.RING_2.desaturated(DRY_RIVERBEDS_FADE)`.
- `StrandedFossilFeatureTest` (vraag 3 A): voor alle sjablonen × 4 rotaties × elke herkomst in een chunk blijft de footprint binnen de 3×3 chunks (met 1 blok marge). De config-codec is begrensd.

### 8. Documentatie

- `CODEBASE.md`:
  - kopregel en status naar change 31;
  - `TransitionBiomes` (nu twee biomes);
  - nieuwe classes `DryRiverbedsFeatures`, `StrandedFossilFeature`, `StrandedFossilConfiguration`;
  - registries (feature-type `stranded_fossil`, configured/placed features, biome);
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
| 37 textures | `cracked_clay`, `scorched_sandstone` zijn hier in grote vlakken te zien: goede testplek |
| 38/39 ring 3 | het zonemasker-patroon (`ring_weight/overworld/ring_N`); vlekken van vanilla- naar asblokken; ring 3-terrein mengt met de beddingen aan de ring 2-kant |
| 41 ores ring 3 | `dry_riverbeds` ook de ring 3-set (stap 6) |
| 48/69 overgangen 3→4, 4→5 | hetzelfde patroon: `transitions.ring_N`, `TransitionBiomes`, `<Naam>Features` met `ring_chance transition`, zonevorm via `ring_weight` in de binnenring-invoer |

---

## Client/server-impact

- Puur worldgen, server-side en deterministisch per seed. De ruis is geseed via een eigen noise-key, het ring-gewicht via `RingLayout` (wereldseed, `RingColumnCache` ThreadLocal), en de feature-random is vanilla's geseede decoration-random. Geen netwerk, geen SavedData of attachments, geen client-code.
- Biome-kleuren gaan via vanilla's registry-sync naar de client. De client kent de beddingen alleen als blokken.
- `StrandedFossilFeature` leest vanilla-structure-templates via de server (`WorldGenLevel.getLevel().getServer().getStructureManager()`, zoals vanilla's `FossilFeature`). Alleen tijdens worldgen.
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
- [ ] **Droog:** in 32 zone-chunks `fill … replace water` boven Y 60 → 0 (geen poelen, geen water in de beddingen). Onder Y 55 mag aquiferwater staan.
- [ ] **Verloop:** `scorched_patch`-blokken (`blackstone` + `basalt` aan de oppervlakte) per chunk in de binnenste vs. buitenste 40 blokken van de zone → buiten duidelijk meer. Fossiel-`bone_block` binnen > buiten.
- [ ] **Ertsen:** `coal_ore`/`iron_ore` in zone-chunks aan de ring 2-kant > 0, aan de ring 3-kant 0 (tot change 41).
- [ ] **Determinisme:** twee verse werelden, de tweede in omgekeerde volgorde geladen (zoals change 16). Per chunk (24 zone-chunks) tellingen van `bone_block`, `cracked_clay`, `blackstone`, `basalt`, `dead_log`, `dry_shrub` → identiek.
- [ ] **Performance** (JFR, stap `noise`, zoals change 9): zone 2→3 vóór en na → hooguit een paar procent erbij, binnen het budget van 1,3× vanilla. Getal in `CODEBASE.md`.

### W — blokkerende handmatige test: **nee** → W-achteraf
**Reden:** geen latere change bouwt direct op de vorm of de inhoud van de beddingen. Change 32 (sfeer) en 37 (textures) raken alleen kleuren en pixels. Change 39 herziet de zone toch, als ring 3 echt terrein krijgt. Alles wat meetbaar is (biome, zonebreedte, droog, diepte, verloop, ertsen, determinisme, naden) staat onder S. Wat overblijft is smaak en afstemming, en die kan achteraf.

W-achteraf (runClient, testseed, checklist):
- [ ] `/ca tpboundary 2` in 4 richtingen: de beddingen lezen als lege rivierbeddingen (breedte, meanderen, oevers), niet als geverfde linten. Geen verticale wanden, zwevende brokken of gaten.
- [ ] Bodem: klei in de bedding, grind-vlekjes, grove oevers, rood zand erbuiten, `scorched_sandstone` op hellingen. Aan de ring 3-kant duidelijk zwart geblakerd.
- [ ] Fossielen: half begraven, niet zwevend, niet geheel onder de grond; niet te vaak.
- [ ] Kloven die de zone in lopen houden hun lagen, zonder lelijke naad.
- [ ] F3: `dry_riverbeds`; geen regen als het in ring 1 regent; kleuren iets grauwer dan ring 2.
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

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | worldgen | Overgangsbiome 2→3 `dry_riverbeds` via `transitions.ring_2` + `TransitionBiomes` (patroon change 11); de Nether-grens 2→3 blijft hard | Eén aanpak voor alle overgangen |
| 2026-10-01 | worldgen | Beddingen: ondiepe geulen (3–6 diep, 15–40 breed, ≥ Y 66) in de ring 2-hoogte met een zonemasker in JSON | Geen Java; initiële dichtheid en aquifers kloppen vanzelf |
| 2026-10-01 | worldgen | Zonevorm via een ring-gewicht als density function: `ring_blend` met constante invoer (`ring_weight/overworld/ring_N`), geen Java | Herbruikbaar voor 3→4 en 4→5 |
| 2026-10-01 | worldgen | Bodem en blakeren: gebarsten klei, grove aarde, rood zand, `scorched_sandstone`; geblakerde vlekken vanilla `blackstone`/`basalt` via `ring_chance`; poelen vallen droog | Leest als drooggevallen en als voorbode van ring 3 |
| 2026-10-01 | worldgen | Gestrande botten: vanilla-fossielsjablonen half begraven via feature `stranded_fossil` | Leest direct als skelet; deterministisch binnen 3×3 chunks |
| 2026-10-01 | worldgen | Ertsen in de zone 2→3: ring 2-set via `#is_ring_2`; ring 3-helft in change 41 | Ertsen volgen de ring; geen exploit-strook |
| 2026-10-01 | worldgen | Klimaat/spawns `dry_riverbeds`: droog, spawns als canyonlands, grotgeluid als ring 2 (kleuren in change 32) | Ring 2 die verschraalt richting ring 3 |
| 2026-10-01 | worldgen | Ondergrond zone 2→3: kloofsteenlagen lopen door (lagenblok-lijst) | Kloven houden hun lagen in de zone |
| 2026-10-01 | worldgen | `dry_riverbeds` zonder `minecraft:`-tags (geen vanilla-structures tot fase 3); in de zandgrot-lijst van change 26 | Structures zijn fase 3; afspraak change 26 |

Commit (na `/code-review`): `worldgen: add dry riverbeds transition biome between ring 2 and 3`.
