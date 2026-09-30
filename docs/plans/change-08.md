# Change 8 — Ring 1: kopieën van de vanilla overworld-biomes

**Scope: `worldgen` — change 8**

Doel: ring 1 krijgt eigen kopieën (`corruptedadventure:<vanilla-naam>`) van alle vanilla overworld-biomes behalve mushroom fields, verdeeld met de vanilla-klimaatparameters; de kopieën krijgen de vanilla-tags (villages e.d.), maar nooit `has_structure/stronghold`.

---

## Afhankelijkheden en aannames

- **Changes 0–7 zijn klaar** (zie `CODEBASE.md`). Change 8 is de eerste change van blok B; er zijn geen eerdere changes uit 8..N-1 om op te wachten.
- `RingBiomeSource` (change 5): per ring een geneste `BiomeSource`, codec ongewijzigd. De geneste source krijgt dezelfde `Climate.Sampler` als vanilla: die komt uit de noise router van `corruptedadventure:overworld`, een kopie van vanilla waarin alleen `final_density` en `initial_density_without_jaggedness` anders zijn. **Temperatuur, vochtigheid, continentalness, erosie, weirdness en depth zijn dus vanilla.** Daardoor geeft ring 1 op elke x,y,z de kopie van precies het biome dat een vanilla-wereld met dezelfde seed daar heeft.
- `PlaceholderBiomes` + `DataGenerators` (change 5): biome-datagen via `createDatapackRegistryObjects`.
- `noise_settings/overworld.json` (change 6): vanilla surface rules met `minecraft:`-biome-keys (zie stap 5).
- **Terrein is tot change 9 nog placeholder** (vlak rond Y ~70). Biomes volgen de vanilla-klimaatruis, het terrein niet. Oceaanbiomes op vlak land, "peaks" zonder bergen en cave-biomes op vanilla-diepte zijn in deze change dus te verwachten. Change 9 maakt het terrein vanilla, en daarmee klopt het weer.
- Change 11 (`withered_expanse`) voegt later het optionele overgangsveld toe aan `RingBiomeSource`. Tot dan wisselt de biome bij grens 1 hard op gewicht 0,5 naar `placeholder_ring_2`.
- Change 14 (Nether-ring 1) kan het hier gebouwde mechanisme hergebruiken (`vanilla_climate` met `preset: minecraft:nether` en dezelfde remap-bootstrap). Hier wordt niets voor de Nether gebouwd.

---

## Plan

### 1. Biome-kopieën (datagen)
- **Nieuw: `worldgen/biome/VanillaBiomeCopies.java`**
  - `copyOf(ResourceKey<Biome> vanilla)` geeft `corruptedadventure:<zelfde pad>`.
  - `OVERWORLD_COPIES`: alle keys uit `MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD.usedBiomes()`, zonder `minecraft:mushroom_fields`. Dat zijn **52 biomes**: plains … deep_dark, inclusief `cherry_grove`, `mangrove_swamp`, `deep_dark`, `lush_caves` en `dripstone_caves`.
  - `bootstrap(BootstrapContext<Biome>)` roept vanilla's `BiomeData.bootstrap(...)` aan met een **doorgeefcontext**. Die geeft `lookup()` ongewijzigd door, en `register(key, biome, lifecycle)` registreert alleen keys uit `OVERWORLD_COPIES`, onder `copyOf(key)`. Nether-, End- en void-biomes en mushroom fields worden overgeslagen; BiomeData gebruikt de retourwaarde niet. Zo komen de biomes 1-op-1 uit vanilla's eigen `OverworldBiomes`-aanroepen, met dezelfde vlaggen (sunflowers, cold enzovoort), zonder die over te typen.
- **`DataGenerators`**: in de `Registries.BIOME`-bootstrap zowel `PlaceholderBiomes::bootstrap` als `VanillaBiomeCopies::bootstrap` aanroepen, in één lambda (een registry mag maar één keer in de `RegistrySetBuilder`).
- **`PlaceholderBiomes` / `ModBiomes` / lang:** `placeholder_ring_1` verwijderen (§1.14: placeholders verdwijnen zodra de echte biomes er zijn). Voor de kopieën komen er geen 52 constanten in `ModBiomes`; daarvoor is `VanillaBiomeCopies.copyOf(Biomes.X)` er.

### 2. Nieuwe biome-source `corruptedadventure:vanilla_climate` (besloten: vraag 1 → A)
- **Nieuw: `worldgen/biome/VanillaClimateBiomeSource.java`**, geregistreerd in `ModBiomeSources`.
  - Codec (MapCodec):
    - `preset`: vanilla's `MultiNoiseBiomeSourceParameterList.Preset.CODEC`, nu `minecraft:overworld`.
    - `replace`: optioneel, een map van vanilla-key naar eigen biome-key, voor uitgesloten biomes.
    - biome-getter via `RegistryOps.retrieveGetter(Registries.BIOME)`, zoals vanilla's parameter list.
  - Opbouw: `MultiNoiseBiomeSourceParameterList.knownPresets().get(preset)` (een `Climate.ParameterList<ResourceKey<Biome>>`, publiek) → elke waarde mappen naar `replace.get(key)` of `copyOf(key)` → `Holder` via de getter → nieuwe `Climate.ParameterList<Holder<Biome>>`. Dat gebeurt eenmalig bij het decoden (R-tree). Ontbreekt een kopie in de registry, dan geeft de codec een `DataResult.error` met de ontbrekende key, geen kale exception.
  - `getNoiseBiome` = `parameters.findValue(sampler.sample(x, y, z))`, net als `MultiNoiseBiomeSource`. `collectPossibleBiomes` = de unieke waarden.
  - Encoden schrijft alleen `preset` + `replace`. `level.dat` blijft dus klein, net als bij vanilla's `"preset": "minecraft:overworld"`.
- **`corrupted_world.json`**: `rings.ring_1` wordt
  ```json
  { "type": "corruptedadventure:vanilla_climate", "preset": "minecraft:overworld",
    "replace": { "minecraft:mushroom_fields": "corruptedadventure:plains" } }
  ```
  `RingBiomeSource` en zijn codec veranderen niet.

### 3. Tags (datagen)
- **Nieuw: `datagen/ModBiomeTagsProvider.java`** (`extends BiomeTagsProvider`, NeoForge-constructor met mod-id + `ExistingFileHelper`). De provider schrijft naar de **vanilla tag-paden** (`data/minecraft/tags/worldgen/biome/…`, `replace: false`).
  - Lidmaatschap spiegelen: voor een expliciete lijst `BiomeTags`-constanten de vanilla-tag-JSON lezen via `ExistingFileHelper.getResource(...)` (vanilla-data zit in de EFH). Elke directe entry `minecraft:x` met x in `OVERWORLD_COPIES` levert `copyOf(x)` op. Entries als `#tag` hoeven niet: die lossen vanzelf op zodra de onderliggende tag gespiegeld is. Als dit te omslachtig blijkt, is een uitgeschreven spiegel van vanilla's `BiomeTagsProvider` het alternatief.
  - Gespiegeld worden alle overworld-relevante tags:
    - klasse-tags: `is_ocean`, `is_deep_ocean`, `is_beach`, `is_river`, `is_mountain`, `is_hill`, `is_badlands`, `is_jungle`, `is_forest`, `is_savanna`, `is_taiga`;
    - gedrag: `allows_surface_slime_spawns`, `spawns_*`, `more_frequent_drowned_spawns`, `has_closer_water_fog`, `water_on_map_outlines`, `plays_underwater_music`, `produces_corals_from_bonemeal`, `snow_golem_melts`, `polar_bears_spawn_on_alternate_blocks`, `increased_fire_burnout`, `reduce_water_ambient_spawns`, `allows_tropical_fish_spawns_at_any_height`, `without_*`, `mineshaft_blocking`, `required_ocean_monument_surrounding`;
    - alle `has_structure/*`-tags voor overworld-structures behalve de uitzonderingen hieronder (besloten: vraag 3 → A). Dus: villages (alle 5), desert pyramid, jungle temple, swamp hut, igloo, pillager outpost, woodland mansion, ocean monument, shipwreck (+ beached), ocean ruins (cold/warm), trail ruins, mineshaft (+ mesa), ruined portals (standard/desert/jungle/mountain/ocean/swamp), buried treasure en **trial chambers**. `has_structure/trial_chambers` noemt in vanilla de biomes direct (alle overworld behalve deep dark), dus spiegelen werkt zonder `is_overworld`.
  - **Nooit gespiegeld:**
    - `is_overworld`: vanilla's `has_structure/stronghold` = `#is_overworld`, dus lidmaatschap geeft strongholds. Vanilla-code gebruikt deze tag verder niet.
    - `has_structure/stronghold` en `stronghold_biased_to`.
    - `has_structure/ancient_city`: al besloten, deep dark zonder ancient cities in fase 1.
  - De tag-provider moet ná `createDatapackRegistryObjects` via `event.createProvider((output, lookup) -> …)` gemaakt worden. Dan krijgt hij de lookup mét de kopieën, anders faalt de tagcontrole op onbekende elementen.

### 4. Villager-types (datagen)
- **Nieuw: `datagen/ModDataMapProvider.java`** (`extends DataMapProvider`): `NeoForgeDataMaps.VILLAGER_TYPES` voor de kopieën, gelijk aan vanilla's `VillagerType.BY_BIOME` (29 entries: badlands/desert → desert, jungles → jungle, savannas → savanna, sneeuw/ijs → snow, swamps → swamp, taiga/windswept → taiga). Zonder deze data map krijgen dorpen in een kopie-woestijn plains-villagers, omdat vanilla op key zoekt. Output: `data/neoforge/data_maps/worldgen/biome/villager_types.json`.

### 5. Surface rules
- **`src/main/resources/data/corruptedadventure/worldgen/noise_settings/overworld.json`**: in alle `minecraft:biome`-condities (`biome_is`) `minecraft:<x>` vervangen door `corruptedadventure:<x>`. Zonder deze stap krijgt de kopie-woestijn gras in plaats van zand, en badlands, peaks, stony shore en oceanen hun vanilla-oppervlak niet. De tak voor `minecraft:mushroom_fields` (mycelium) verwijderen. Verder blijft de JSON vanilla.

### 6. Lang
- `EnglishLanguageProvider` / `DutchLanguageProvider`: `biome.corruptedadventure.<pad>` voor alle 52 kopieën. Engels = de vanilla-naam; Nederlands = de vanilla `nl_nl`-naam, met de hand overgenomen (die staat niet in de jar). De entry van `placeholder_ring_1` verdwijnt.

### 7. Tests (unit)
- **Nieuw: `VanillaClimateBiomeSourceTest`**:
  - voor elk parameterpunt van vanilla's overworld-lijst geeft onze lijst `copyOf(vanilla)` of de vervanger;
  - de mushroom-punten geven `corruptedadventure:plains`;
  - `possibleBiomes` = 52 kopieën, zonder mushroom;
  - bij een ontbrekende kopie geeft de codec een duidelijke fout.
  - De holder-getter is fake, met standalone references.
- **Nieuw: `VanillaBiomeCopiesTest`**: de bootstrap (fake `BootstrapContext`) registreert precies `OVERWORLD_COPIES`, allemaal in onze namespace, zonder nether/end/void/mushroom.
- **Tag-exclusies**: een test dat de lijst gespiegelde tags `is_overworld`, `has_structure/stronghold`, `stronghold_biased_to` en `has_structure/ancient_city` niet bevat.

### 8. Documentatie
- `CODEBASE.md`: nieuwe classes, biome-source `vanilla_climate`, biomes-tabel (52 kopieën; `placeholder_ring_1` weg), tags en data map in `src/generated`, surface-rule-aanpassing, valkuilen (surface rules op key, `is_overworld` → stronghold, villager-type op key, tag-provider na datapack-registries).
- `fase-1-wereld.md` §1.7 (uitwerking change 8) en `02-beslissingenlog.md`: drie regels (2026-09-30, `worldgen`):
  - ring 1 via eigen biome-source `vanilla_climate`;
  - mushroom-gebied wordt `plains`;
  - alle vanilla-structures behalve stronghold en ancient city, inclusief trial chambers.
- `docs/backlog.md` (zie Risico's).

---

## Client/server-impact

- Puur worldgen en data; deterministisch per seed (geen eigen random).
- Biomes, tags en de `villager_types`-data map worden via vanilla/NeoForge naar de client gesynct. Er zijn geen eigen payloads of client-code.
- `VanillaClimateBiomeSource` is common code zonder client-imports. `getNoiseBiome` roept (via `RingBiomeSource`) `RingLayout.current()` aan, dus alleen met een draaiende server. Dat is ongewijzigd.

---

## Tests

**B** — `./gradlew build` (incl. de nieuwe unit tests).

**D** — `./gradlew runData`:
- 52 biome-JSONs in `src/generated/resources/data/corruptedadventure/worldgen/biome/`, geen `mushroom_fields`, geen `placeholder_ring_1`;
- tags onder `data/minecraft/tags/worldgen/biome/**` met `replace: false`;
- `villager_types.json` + lang.

Output committen. Controle: `grep -r corruptedadventure src/generated/resources/data/minecraft/tags/worldgen/biome/is_overworld.json has_structure/stronghold.json has_structure/ancient_city.json` vindt niets (die bestanden horen er niet eens te zijn).

**S** (extra, goedkoop): `tools/server-smoke-test.sh` met een **nieuwe** `level-name` (bijv. `change8-test`). De preset moet decoderen, de server moet starten en de spawn-chunks genereren zonder fouten. Oude testwerelden (met `placeholder_ring_1` in `level.dat`) laden niet meer; dat is verwacht.

**W** — nieuwe wereld, seed `20260930`, preset `corrupted_world`. Checklist:
- [ ] `/ca tpring 1`: F3 toont `corruptedadventure:<vanilla-biome>`. Rondvliegen door ring 1: meerdere biomes, vertrouwde overgangen (bijv. geen woestijn naast sneeuw zonder tussenbiome).
- [ ] **Vergelijk met vanilla:** maak een vanilla-wereld met seed `20260930`. Op 5 vaste punten binnen ring 1 (bijv. 0,0 / 500,0 / 0,-800 / -1000,300 / 700,700) is de biome in F3 dezelfde, alleen met onze namespace. Cave-biomes (lush/dripstone/deep dark) op dezelfde Y ook.
- [ ] `/locate biome corruptedadventure:plains` (en desert, forest, taiga, ocean, river) → gevonden binnen ~1.500 van 0,0 (niet elk biome komt in ring 1 voor; dat is prima). `/locate biome minecraft:plains` → niet gevonden.
- [ ] `/locate structure #minecraft:village` vanaf 0,0 → gevonden in ring 1. Erheen: villagers hebben het type van het biome (woestijndorp = woestijn-villagers, indien aanwezig).
- [ ] `/locate structure minecraft:stronghold` → niet gevonden. `/locate structure minecraft:ancient_city` → niet gevonden. `/locate structure minecraft:trial_chambers` → gevonden in ring 1, en niet in deep dark.
- [ ] Mushroom-gebied: `/locate biome corruptedadventure:mushroom_fields` geeft een fout (bestaat niet). Waar vanilla met dezelfde seed mushroom fields heeft (als dat binnen ring 1 valt), toont F3 `corruptedadventure:plains`.
- [ ] Oppervlak per biome klopt, ondanks het vlakke placeholder-terrein: woestijn zand, badlands terracotta, beach zand, stony shore steen, peaks sneeuw/steen.
- [ ] Features: bomen, bloemen en grasskleur per biome; ores aanwezig (`/fill … replace` tellen is niet nodig, zichtbaar in een kuil).
- [ ] Mobs: dieren in plains, 's nachts husks in de woestijn, slimes in swamp (tag `allows_surface_slime_spawns`).
- [ ] `/ca tpboundary 1`: de biome wisselt precies op de grens naar `placeholder_ring_2`. Ring 2–5, de void en de Nether zijn ongewijzigd.
- [ ] Portal-test vanuit ring 1: werkt zoals voorheen (Nether ÷4).
- [ ] Generatiesnelheid: geen merkbare vertraging bij `/ca tpring 1` vergeleken met change 7.

---

## Risico's / aandachtspunten

- **Terrein-mismatch tot change 9** (zie aannames). Niet oplossen in deze change.
- **Surface rules werken op biome-key.** Stap 5 is verplicht, anders klopt het oppervlak niet. Latere eigen biomes (ring 2–5) krijgen hun eigen surface-rule-takken.
- **Feature-volgorde:** `RingBiomeSource.possibleBiomes` is de unie van alle ringen. Latere ringbiomes die vanilla-features in een andere volgorde gebruiken, kunnen vanilla's "Feature order cycle"-crash geven. Nu nog niet relevant (placeholders hebben geen features); noteren als valkuil.
- **`minecraft:is_overworld` bevat onze kopieën bewust niet.** Mods die op die tag leunen, zien ring 1 niet als overworld.
- **Wolf-varianten** (1.21 `wolf_variant`-registry) verwijzen naar vanilla-keys, zoals forest → woods, grove → snowy, snowy_taiga → ashen. Op kopieën spawnt de standaard (pale).
- **Andere vanilla-verwijzingen op key:** de advancement *Adventuring Time* (`adventure/adventuring_time`) is niet te halen, en de visloot-junk bamboe in jungles (`gameplay/fishing/junk`) valt weg.
- Performance: gelijk aan vanilla multi-noise (R-tree, per thread de laatste treffer gecachet); één keer opbouwen bij decode.
- **Voor de backlog** (niet in deze change):
  - `[mobs]` Wolf-varianten op de kopieën (vanilla `wolf_variant`-entries uitbreiden).
  - `[progression]` *Adventuring Time* (en later *Hot Tourist Destinations*) onhaalbaar met kopie-biomes; vervangen of aanpassen in fase 5.
  - `[items]` Visloot: bamboe-junk in jungle-kopieën (`location_check` op vanilla-keys).
  - `[worldgen]` NeoForge `c:`-biome-tags (`c:is_hot/overworld`, …) voor de kopieën, voor mod-compatibiliteit.

---

## Open ontwerpvragen

### 1. Hoe komt de vanilla-klimaatverdeling in ring 1?
- **A — Eigen biome-source-type `corruptedadventure:vanilla_climate` (aanbevolen).** Klein Java-type (~80 regels) dat vanilla's overworld-parameterlijst bij het laden omzet naar onze kopieën. `level.dat` en de preset blijven klein; volgt vanilla automatisch; herbruikbaar voor Nether-ring 1 (change 14). `RingBiomeSource`-codec blijft gelijk.
- **B — `minecraft:multi_noise` met een uitgeschreven `biomes`-lijst.** Geen eigen Java-type. Wel moet de preset door datagen gegenereerd worden (~7.500 parameterpunten, meerdere MB JSON), en die hele lijst wordt bij elke save in `level.dat` meegeschreven (NBT, ~1 MB ongecomprimeerd). De handgeschreven `corrupted_world.json` verdwijnt naar `src/generated`.

Antwoord: **A** — eigen biome-source-type `corruptedadventure:vanilla_climate` (2026-09-30).

### 2. Wat komt er waar vanilla mushroom fields zou plaatsen?
Vanilla zet mushroom fields in de laagste continentalness (−1,2 tot −1,05). Met het vanilla-terrein uit change 9 zijn dat eilandjes ver in de oceaan. Binnen de 1.500 blokken van ring 1 is dat zeldzaam, maar het kan.
- **A — `corruptedadventure:plains` (aanbevolen).** Een gewoon grasland-eiland met gewone mob-spawns (dus ook monsters, geen "veilige haven"). Eén regel in `replace`, geen terreinwerk.
- **B — `corruptedadventure:deep_ocean`.** Consequent "oceaan", maar het eiland zelf blijft land (grasoppervlak, oceaan-spawns en -muziek op land). Voelt vreemd.
- **C — Oceaan + terrein afvlakken in change 9.** De ring 1-invoer klemt continentalness zodat er daar geen eilanden ontstaan, plus `deep_ocean` als biome. Het schoonst, maar extra werk en afwijking van vanilla-terrein in change 9.

Antwoord: **A** — `corruptedadventure:plains` (2026-09-30).

### 3. Welke vanilla-structures krijgen de ring 1-kopieën (via `has_structure/*`)?
Stronghold (nooit) en ancient city (fase 1 niet) liggen al vast.
- **A — Alle overige vanilla-structures (aanbevolen):** villages, pyramides, tempels, hutten, iglo's, outposts, mansions, monuments, shipwrecks, ruins, trail ruins, mineshafts, ruined portals, buried treasure én **trial chambers**. Het meest "vertrouwd vanilla". Trial chambers geven vroeg sterke loot (mace, ominous vaults), maar ring-gear ligt per ring een tier hoger.
- **B — Als A, maar zonder trial chambers.** Houdt de 1.21-loot (mace, heavy core, sterke enchants) uit het begin van de progressie. Minder "compleet vanilla".
- **C — Alleen de klassieke kleine structures** (villages, tempels, hutten, iglo's, shipwrecks, ruins, mineshafts, ruined portals, buried treasure). Geen mansions, monuments, outposts of trial chambers: ring 1 blijft rustiger, en de grote vanilla-structures laten ruimte voor eigen structures in fase 3.

Antwoord: **A** — alle overige vanilla-structures, inclusief trial chambers (2026-09-30).
