# Change 11 — Overgangsbiome 1→2 `withered_expanse`

**Scope: `worldgen` — change 11**

**Doel:** de overgangszone van ~128 blokken rond grens 1 (ring 1 → ring 2) krijgt een eigen biome, de **verdorde vlakte** (`corruptedadventure:withered_expanse`): bladloze bossen, vaal gras, zandsteenranden, een voorbode van ring 2. Daarvoor krijgt `RingBiomeSource` het optionele veld voor overgangsbiomes dat §1.3/§1.14 al aankondigen. Het verloop binnen de zone (ring 1-kant meer bomen, ring 2-kant meer zand en zandsteen) loopt via een eigen placement modifier `ring_chance`, die change 12 uitbreidt. Andere grenzen (en de hele Nether) houden de harde wissel op gewicht 0,5.

**Beslissingen (2026-09-30, zie "Open ontwerpvragen"):** 1a verloop via placement modifier · 2a dood gras als hoofdbodem · 3a overgangsbiome ook boven water · 4a droog als savanne, vanilla-monsters, alleen konijnen.

---

## Afhankelijkheden en aannames

- **Changes 0–7 klaar** (zoals in `CODEBASE.md`).
- **Change 8** is gebouwd: ring 1 is een `minecraft:multi_noise` met eigen kopieën van de vanilla-biomes. `DataGenerators` bouwt biomes al met `context.lookup(Registries.PLACED_FEATURE)` en `context.lookup(Registries.CONFIGURED_CARVER)` (nodig voor `OverworldBiomes`), dus vanilla placed features en carvers zijn in de biome-bootstrap bereikbaar.
- **Change 9** is gebouwd: ring 1-terrein = vanilla noise router als ring 1-invoer van `ring_blend`. De surface rules in `worldgen/noise_settings/overworld.json` zijn nog de vanilla-kopie (plus eventuele change 9-aanpassingen buiten `above_preliminary_surface`).
- **Change 10** is gebouwd met (werknamen; de definitieve namen uit change 10 gaan voor):
  - `dead_grass_block` (grasblok-variant, bovenlaag) en `withered_dirt` (aarde-variant);
  - `dead_log` (+ `dead_wood`, `dead_planks`, houtset).
  - **Tags:** `dead_grass_block` en `withered_dirt` zitten in `#minecraft:dirt` (dan kunnen bomen, struiken en kort gras erop). Staat dat niet in change 10, dan gebruikt deze change een `matching_blocks`-predicaat en komt er een backlog-regel `[blocks]`.
- Ring 2 is nog `placeholder_ring_2` (geen surface rules, geen features). Het terrein in de zone is de smoothstep-menging van ring 1 (vanilla) en het placeholder-terrein van ring 2 (Y ~95).
- Eigen features zijn er nog niet; `worldgen/feature/` en de registries voor configured/placed features en placement modifiers ontstaan in deze change.

### Afstemming met change 12 (één modifier, geen twee varianten)
- Change 11 bouwt **één** placement modifier `corruptedadventure:ring_chance` (`RingChanceFilter`) met een `signal`-veld. Change 11 levert alleen het signaal `transition`.
- Change 12 bouwt **geen** eigen type `ring_progress` / `RingProgressFilter`, maar **breidt `ring_chance` uit** met het signaal `progress` (plus `RingLayout.ringProgress` zoals in het change 12-plan). Kanscurve, codec, registratie en tests zijn dan gedeeld.
- Veldnamen van change 12 vertalen zo: `min_chance` → `from_chance`, `max_chance` → `to_chance`; `ring`, `start` en `exponent` bestaan al (stap 3). Het change 12-plan moet dit bij de bouw overnemen; `dead_tree` uit deze change wordt daar ook hergebruikt.

---

## Plan

### 1. `RingBiomeSource`: optioneel veld `transitions`
- Nieuw codec-veld `transitions`: `Codec.unboundedMap(Ring.CODEC, BiomeSource.CODEC).optionalFieldOf("transitions", Map.of())`. Sleutel = **binnenring van de grens** (`ring_1` = grens 1→2, …, `ring_5` = grens 5→void). Validatie: sleutel `void` → `DataResult.error` (er is geen grens buiten de void).
- `getNoiseBiome(layout, x, y, z, sampler)`:
  ```java
  RingPosition position = layout.sample(dimension, QuartPos.toBlock(x), QuartPos.toBlock(z));
  BiomeSource transition = position.inTransition() ? transitions.get(position.inner()) : null;
  BiomeSource source = transition != null ? transition : rings.get(position.ring());
  return source.getNoiseBiome(x, y, z, sampler);
  ```
  Dus: **de hele overgangszone** (`inTransition()`, ~128 blokken, Nether ~32) krijgt het overgangsbiome, **ook boven water** (antwoord 3a: geen uitzondering voor ring 1-oceanen/-rivieren). Grenzen zonder overgangsbiome wisselen zoals nu hard op 0,5.
- `collectPossibleBiomes()`: unie van `rings` én `transitions`.
- Javadoc van de class bijwerken (zin "until transition biomes get their own optional field" vervalt).
- Geneste source is een normale biome source (nu `minecraft:fixed`); later kan een overgang dus ook varianten krijgen zonder codec-wijziging.

### 2. Preset
`data/corruptedadventure/worldgen/world_preset/corrupted_world.json`, alleen de overworld:
```json
"transitions": {
  "ring_1": { "type": "minecraft:fixed", "biome": "corruptedadventure:withered_expanse" }
}
```
De Nether krijgt geen `transitions` (optioneel veld; gedrag ongewijzigd).

### 3. Placement modifier `ring_chance` (antwoord 1a)
- `worldgen/feature/RingChanceFilter.java` (extends `PlacementFilter`), type `corruptedadventure:ring_chance`. Codec-velden:
  - `signal`: enum `RingChanceFilter.Signal` (`StringRepresentable`), in change 11 alleen `transition`; change 12 voegt `progress` toe.
  - `ring` (`Ring.CODEC`): bij `transition` de **binnenring van de grens** (`ring_1` = grens 1→2); bij `progress` (change 12) de ring zelf.
  - `from_chance`, `to_chance` (0–1; `from` mag groter zijn dan `to`, voor afnemende dichtheid).
  - `start` (0–1, standaard 0) en `exponent` (> 0, standaard 1).
- Signaal `transition`: `t` = `blend` van `RingLayout.current().sample(dimension, x, z)` als die positie in de zone van de grens `ring` → `ring + 1` ligt; anders geen signaal.
- Kans: geen signaal of `t < start` → 0; anders `from + (to − from) · ((t − start) / (1 − start))^exponent`; plaatsen als `random.nextFloat() < kans` (random = vanilla's geseede decoration-random, dus deterministisch).
- Dimensie via `context.getLevel().getLevel().dimension()` (server-side `WorldGenRegion` → `ServerLevel`). `RingLayout.current()` **alleen in `shouldPlace`**, nooit in codec/constructor (zelfde valkuil als bij density functions).
- Kanscurve als package-private static functie (pure rekenwerk, testbaar zonder layout); signaalbepaling apart zodat change 12 er één `case` bij zet.
- `registry/ModPlacementModifierTypes.java`: DeferredRegister `Registries.PLACEMENT_MODIFIER_TYPE`, type `ring_chance`; registreren in `CorruptedAdventure`.

### 4. Features (datagen)
- `registry/ModConfiguredFeatures.java` en `registry/ModPlacedFeatures.java`: alleen `ResourceKey`s (zoals `ModBiomes`).
- `worldgen/feature/WitheredExpanseFeatures.java`: twee bootstraps, `configured(BootstrapContext<ConfiguredFeature<?, ?>>)` en `placed(BootstrapContext<PlacedFeature>)`. Registratie in `DataGenerators` (`Registries.CONFIGURED_FEATURE`, `Registries.PLACED_FEATURE`).

| Configured feature | Type (vanilla) | Inhoud |
|---|---|---|
| `dead_tree` | `minecraft:tree` | stam `dead_log`, `StraightTrunkPlacer(4, 2, 0)`, foliage-provider **lucht** (`BlobFoliagePlacer(0,0,0)`), `dirt` = `withered_dirt`, `ignoreVines` — kale paal, herbruikbaar in change 12 |
| `dead_tree_forked` | `minecraft:tree` | stam `dead_log`, `ForkingTrunkPlacer(5, 2, 2)` (acacia-vorm, kale takken), foliage lucht |
| `withered_trees` | `minecraft:random_selector` | 70 % `dead_tree_forked`, rest `dead_tree` |
| `withered_vegetation` | `minecraft:random_patch` | dode struik (`minecraft:dead_bush`) en wat kort gras (vaal door de biome-kleur) |
| `withered_sandstone_rock` | `minecraft:forest_rock` | `minecraft:sandstone` (vanilla keien-vorm, dan zandsteen) |
| `withered_sand_patch` | `minecraft:disk` | zand/`minecraft:coarse_dirt`-vlek aan de oppervlakte |

| Placed feature | Stap | Plaatsing |
|---|---|---|
| `withered_expanse_trees` | `VEGETAL_DECORATION` | `noise_threshold_count` (bosjes + open vlakte), in square, `ring_chance` (`transition`, `ring_1`, 1,0 → 0,2), heightmap `WORLD_SURFACE_WG`, filter op `#dirt` eronder, biome |
| `withered_expanse_vegetation` | `VEGETAL_DECORATION` | count ~3, in square, heightmap, biome |
| `withered_expanse_rocks` | `LOCAL_MODIFICATIONS` | `rarity 1/3`, in square, `ring_chance` (`transition`, `ring_1`, 0,1 → 1,0), heightmap, biome |
| `withered_expanse_sand_patches` | `TOP_LAYER_MODIFICATION` | count 2, in square, `ring_chance` (`transition`, `ring_1`, 0 → 0,8), heightmap, biome |

Aantallen zijn startwaarden en worden in test W bijgesteld (geen ontwerpvraag).

### 5. Biome `withered_expanse` (datagen)
- `worldgen/biome/TransitionBiomes.java`: bootstrap voor overgangsbiomes (later ook `dry_riverbeds`, `steam_fens`, `blighted_mire`). Registratie in `DataGenerators` (zelfde `Registries.BIOME`-bootstrap, aangeroepen naast `PlaceholderBiomes` en de change 8-biomes).
- `ModBiomes.WITHERED_EXPANSE`.
- **Generation settings in vanilla-volgorde** (om "Feature order cycle" te voorkomen): `globalOverworldGeneration`, `addDefaultOres`, `addDefaultSoftDisks`, daarna eigen features achteraan per stap (die komen in geen ander biome voor, dus geen cyclus). Vanilla-ores zoals ring 1 (afbouw hoort bij ring 2, change 25).
- **Kleuren (startwaarden; fijnafstemming in change 13, `atmosphere`):** lucht tussen ring 1 en 2 (~`0x8CB6F4`), fog vaal (~`0xD6D4C0`), gras **vaal geelgrijs** (~`0xA8A070`), blad ~`0x9A9060`, water **troebel** (~`0x4E8A92`, waterfog ~`0x1A3336`) — ook zichtbaar op ring 1-water dat de zone in loopt (antwoord 3a).
- **Klimaat (antwoord 4a):** als savanne: `temperature` 2,0, `downfall` 0,0, `hasPrecipitation` false (geen regen of sneeuw vanaf de zone).
- **Spawns (antwoord 4a):** `BiomeDefaultFeatures.commonSpawns` voor monsters ('s nachts niet veilig) en als enige dier **konijnen** (`MobSpawnSettings`, creature-categorie; gewicht als in de woestijn). Verder geen vee. Echte ring-mobs volgen in fase 4.
- **Tags:** geen `has_structure/*`, geen `#is_overworld` (anders strongholds).
- Lang: `biome.corruptedadventure.withered_expanse` → en_us "Withered Expanse", nl_nl "Verdorde Vlakte".

### 6. Surface rules (vaal gras, zandsteenranden; antwoord 2a)
In `worldgen/noise_settings/overworld.json`, als **eerste** element van de `above_preliminary_surface`-sequence (zodat het vóór de generieke grasregel komt):
```
condition(biome [withered_expanse],
  sequence[
    condition(stone_depth floor, offset 0,
      sequence[
        condition(steep, sandstone),                             // zandsteenranden op hellingen
        condition(noise_threshold surface 0.8…, coarse_dirt),    // kale plekken
        condition(noise_threshold surface -0.9…-0.6, withered_dirt), // vlekken verdorde aarde
        condition(water -1 0, dead_grass_block),                 // hoofdbodem: dood gras
        withered_dirt ]),                                        // onder water
    condition(stone_depth floor, add_surface_depth,
      sequence[ condition(steep, sandstone), withered_dirt ]) ])
```
Daaronder vanilla (steen/deepslate). Kort gras en dode struiken krijgen de vale biome-tint (`withered_vegetation`). Zandsteenranden = `steep` + keien (`withered_sandstone_rock`) + zandvlekken, die via `ring_chance` naar de ring 2-kant toenemen.

### 7. Documentatie
- `CODEBASE.md`: `RingBiomeSource` (veld `transitions`), nieuwe classes (`TransitionBiomes`, `WitheredExpanseFeatures`, `RingChanceFilter`, `ModConfiguredFeatures`, `ModPlacedFeatures`, `ModPlacementModifierTypes`), biome-tabel, "Overige" (`ring_chance`; noot: change 12 breidt uit met signaal `progress`), databestanden (preset, surface rule in `overworld.json`), status change 11.
- `fase-1-wereld.md`: §1.3/§1.14 (overgangsbiome = hele zone, ook boven water; veld `transitions` gebouwd), ring 1-sectie (uitwerking `withered_expanse`: bodem, verloop, klimaat, spawns).
- `02-beslissingenlog.md`: vier regels (2026-09-30, `worldgen`) voor de antwoorden 1a–4a, plus één regel "één placement modifier `ring_chance` met signalen, change 12 breidt uit".

---

## Client/server-impact
- Puur worldgen, server-side en deterministisch per seed (biomekeuze via `RingLayout`, feature-random geseed). Geen netwerk, geen sync, geen client-code.
- Biome-kleuren gaan via vanilla-biomedata naar de client (standaard registry-sync).
- `RingChanceFilter` roept `RingLayout.current()` aan: alleen tijdens worldgen of `/place` (server draait altijd). Geen client-class bereikbaar.
- Mob-spawns (monsters, konijnen) zijn vanilla server-side spawnregels uit de biomedata.
- Bestaande testwerelden krijgen naden bij nieuw gegenereerde chunks: test altijd in een nieuwe wereld.

---

## Tests

**B** — `./gradlew build`, met unit tests:
- `RingBiomeSourceTest` uitbreiden: met `transitions` voor `ring_1` geeft de hele zone (blend ~0,01 / 0,5 / ~0,99) het overgangsbiome; net buiten de zone ring 1- resp. ring 2-biome; grens 2 zonder overgang wisselt nog hard op 0,5; Nether zonder `transitions` ongewijzigd; `possibleBiomes` bevat het overgangsbiome; codec: `transitions` weglaten mag, sleutel `void` geeft een fout.
- `RingChanceFilterTest`: kanscurve (`from`/`to`, ook afnemend; `start`; `exponent`); signaal `transition`: `t` = blend in de zone van grens 1, geen signaal binnen ring 1, in ring 2 of in de zone van grens 2; codec-rondreis met standaardwaarden.

**D** — `./gradlew runData`: biome `withered_expanse`, configured/placed features en lang-keys verschijnen in `src/generated`; output committen.

**S** (aanbevolen, niet verplicht in de rij) — `tools/server-smoke-test.sh`: server start met de nieuwe preset; een feature-volgorde-cyclus of codecfout valt hier al op.

**W** — nieuwe wereld, seed `20260930`, preset `corrupted_world`. In-game checklist (concept):
- [ ] Wereld laadt zonder "Feature order cycle"-fout.
- [ ] `/locate biome corruptedadventure:withered_expanse` vindt een plek op ~1.400–1.600 blokken van 0,0.
- [ ] Kijk achtereenvolgens N, O, Z, W en doe `/ca tpboundary 1`: F3 toont `corruptedadventure:withered_expanse`; `/ca ring` toont ring 1/ring 2 met gewicht ~0,5.
- [ ] Loop radiaal naar binnen: op het moment dat F3 wisselt naar een ring 1-biome toont `/ca ring` gewicht ~0 (net geen overgang meer). Naar buiten: wissel naar `placeholder_ring_2` bij gewicht ~1. Afstand tussen beide wissels (via `/ca ring`-afstand of F3-coördinaten) ≈ 128 blokken.
- [ ] Bodem: dood gras als hoofdbodem, vlekken grove en verdorde aarde, zandsteen op hellingen; kort gras en dode struiken vaal getint.
- [ ] Verloop: aan de ring 1-kant (gewicht < 0,3) duidelijk meer kale bomen, aan de ring 2-kant (gewicht > 0,7) weinig bomen en meer zandvlekken en zandsteenkeien.
- [ ] `/place feature corruptedadventure:dead_tree` en `…:dead_tree_forked` werken op dood gras.
- [ ] Water: een oceaan of rivier van ring 1 die de zone in loopt, toont F3 `withered_expanse` en troebel water.
- [ ] Klimaat: `/weather rain` → geen regen in de zone (wel in ring 1 ernaast). 's Nachts spawnen monsters; overdag alleen konijnen als dier.
- [ ] Ring 1-biomes (change 8) en `placeholder_ring_2` elders ongewijzigd.
- [ ] Grens 2 (`/ca tpboundary 2`): nog harde wissel op gewicht 0,5.
- [ ] Nether: `/ca tpboundary 1 nether` → harde wissel, geen `withered_expanse` in de Nether.
- [ ] Portal bouwen in de zone: aankomst in de Nether op ÷4 (standaard-W).

---

## Risico's
- **Feature order cycle:** vanilla eist dezelfde onderlinge volgorde van features in alle biomes van één BiomeSource; anders crasht het laden van de wereld. Mitigatie: vanilla-helpers in vanilla-volgorde, eigen features achteraan per stap; test S/W.
- **Harde biome-rand aan de ring 1-kant:** bomen en dood gras stoppen daar op een lijn (die volgt wel de verruisde grens en de client-biomeblend verzacht kleuren). Aan de ring 2-kant loopt de dichtheid via `ring_chance` geleidelijk af.
- **Terrein ≠ biome:** in de zone mengt het terrein vanilla (ring 1, mogelijk oceaan/rivier) met ring 2. Besloten (3a): het biome geldt ook boven water; zeegras en koraal van vanilla-oceanen stoppen dan op de zonerand.
- **Namen/tags uit change 10** kunnen afwijken (zie aannames); dan alleen verwijzingen aanpassen.
- **Bomen met lucht als blad:** vanilla's `TreeFeature` zet op "blad"-plekken lucht; dat kan een kort-graspol op boomhoogte weghalen — verwaarloosbaar.
- **`overworld.json` is een grote vanilla-kopie:** een fout in de surface rule breekt de hele overworld; change 23 (ring 2) voegt er ook regels aan toe. Mitigatie: eigen regel als één blok bovenaan `above_preliminary_surface`, duidelijk gemarkeerd in `CODEBASE.md`.
- **Afstemming change 12:** als change 12 toch een eigen `ring_progress`-type bouwt, ontstaan twee modifiers met dezelfde kanscurve. Mitigatie: zie "Afstemming met change 12"; de reviewer van change 12 controleert dat.

---

## Open ontwerpvragen

**1. Verloop binnen de zone** — is de verdorde vlakte overal gelijk, of loopt ze van ring 1 naar ring 2 geleidelijk over?
- a) *(aanbevolen)* **Verloop via een eigen placement modifier** (leest het gewicht uit `ringAt`): aan de ring 1-kant meer kale bomen, aan de ring 2-kant meer zand en zandsteen. Eén kleine Java-class, deterministisch, server-side; herbruikbaar in change 12 ("vaker richting de rand") en de latere overgangsbiomes. Voelt als een echte voorbode.
- b) **Uniform**: dezelfde dichtheid over de hele zone. Geen Java voor features, simpelst; de overgang naar ring 2 is dan een zichtbare lijn in begroeiing.
- c) **Twee biomes** (binnen- en buitenhelft van de zone). Geen modifier nodig, maar een extra biome en een harde lijn midden in de zone; strijdig met "1 overgangsbiome per grens" (§1.10).

Antwoord: **a** (2026-09-30) — verloop via placement modifier `ring_chance` (signaal `transition`); change 12 breidt die uit met signaal `progress`.

**2. Bodem ("vaal gras")** — hoe ziet de grond eruit?
- a) *(aanbevolen)* **Dood gras (`dead_grass_block`) als hoofdbodem**, met vlekken grove aarde en verdorde aarde; kort gras en dode struiken met vale biome-tint. Gebruikt de blokken van change 10 waarvoor ze bedoeld zijn; duidelijk ander gebied.
- b) **Vanilla-gras met vale kleur**, dood gras alleen in vlekken. Zachtere overgang vanuit ring 1, minder eigen karakter; dood gras vooral voor change 12.
- c) **Alleen verdorde aarde en grove aarde** (geen gras). Het kaalst; sterk contrast met ring 1, weinig "vaal gras".

Antwoord: **a** (2026-09-30) — dood gras als hoofdbodem, vlekken grove en verdorde aarde, vaal kort gras en dode struiken.

**3. Water in de zone** — het ring 1-terrein kan tot in de zone oceaan of rivier zijn (het terrein mengt pas geleidelijk naar ring 2).
- a) *(aanbevolen)* **Overgangsbiome overal in de zone, ook boven water**; water krijgt de troebele kleur van de verdorde vlakte. Simpel, de zone is overal herkenbaar; vanilla-oceaanfeatures (zeegras, koraal) stoppen daar.
- b) **Ring 1-oceaan/-rivier houdt voorrang** (als de ring 1-source daar een `#is_ocean`/`#is_river`-biome geeft). Natuurlijker aan de kust, maar de zone krijgt gaten en `RingBiomeSource` moet de ring 1-source én tags raadplegen (complexer, generieke regel voor alle latere overgangen).
- c) **Geen water in de zone**: vloeistofregel via `ring_noise` (zoals de void). Past bij "gestolen water", maar geeft droge kommen/kliffen aan de kust; meer werk en raakt het terrein van change 9.

Antwoord: **a** (2026-09-30) — overgangsbiome in de hele zone, ook boven water, met troebele waterkleur.

**4. Klimaat en spawns van `withered_expanse`**
- a) *(aanbevolen)* **Droog en warm als savanne** (temperatuur 2,0, geen regen/sneeuw), **vanilla-monsters** 's nachts, **geen dieren behalve konijnen**. De regen stopt al in de zone: voorbode van de droge ring; geen veilige zone voor mobs; echte mobs in fase 4.
- b) **Als ring 1** (temperatuur 0,8, regen), vanilla-monsters en vee. Minder abrupt, maar de zone voelt nog als ring 1.
- c) **Droog, geen enkele spawn** (zoals de placeholders). Simpelst, maar een onnatuurlijk veilige strook.

Antwoord: **a** (2026-09-30) — droog als savanne (2,0, geen neerslag), vanilla-monsters, alleen konijnen als dier.
