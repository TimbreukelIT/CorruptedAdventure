# CODEBASE.md — kaart van de repo

> **Lees dit bestand altijd als eerste**, vóór je code opent of gaat zoeken.
> Werk het bij in elke change die classes, registries of databestanden toevoegt, hernoemt of verwijdert.
> Houd het beknopt: per regel *wat* en *waar*, geen uitleg die al in de code staat.

**Laatst bijgewerkt:** 2026-09-30 — change 5 (`RingBiomeSource`, placeholder-biomes, datagen-opzet)
**Huidige fase:** 1 — Wereld (bouw gestart; bouwplan changes 0–96 in `docs/design/fase-1-wereld.md` §3; changes 0–5 klaar, volgende: change 6)

---

## Status per fase

| Fase | Status |
|---|---|
| 1 — Wereld | In bouw — changes 0–5 klaar |
| 2 — Items en tools | — |
| 3 — Structures | — |
| 4 — Mobs en bosses | — |
| 5 — Progressie en corruptie | — |

---

## Mappenstructuur

Status: ✅ bestaat · 🔲 gepland

```
src/main/java/nl/timtoys/corruptedadventure/
  CorruptedAdventure.java      ✅ @Mod-entrypoint (common); MOD_ID, LOGGER, id(path). Registreert de DeferredRegisters en datagen
  config/                      ✅ config en per-wereld-snapshot (enige plek voor ringgrenzen)
    CommonConfig.java          ✅ COMMON-config (`corruptedadventure-common.toml`): standaard-ringgrenzen; 🔲 balanswaarden
    RingSettings.java          ✅ record + Codec + validatie van de ringgrenzen
    RingSettingsSnapshot.java  ✅ snapshot per wereld; `current()` voor `ringAt` (server-side, sync in change 18)
  ring/                        ✅ ringindeling
    Ring.java                  ✅ enum RING_1…RING_5, VOID; `CODEC` (`ring_1`…`ring_5`, `void`)
    RingPosition.java          ✅ record (inner, outer, blend 0–1); `ring()`, `inTransition()`
    RingLayout.java            ✅ `ringAt(dimension, x, z)`: seeded grensruis, Nether ×4, overgangszones
  command/                     ✅ debugcommando's `/ca …` (OP-only, server-side; vaste Engelse tekst)
    CaCommand.java             ✅ `/ca`-root, registratie via `RegisterCommandsEvent`
    RingInfoCommand.java       ✅ `/ca ring`
    RingTeleportCommand.java   ✅ `/ca tpring <1–6> [nether]`, `/ca tpboundary <1–5> [nether]` + veilige landingshoogte
    RingTargets.java           ✅ pure rekenwerk: richting, ringmidden, verruisde grens (bisectie), dichtstbijzijnde grens
  registry/                    ✅ DeferredRegisters en ResourceKeys; 🔲 ModBlocks, ModItems, ModCreativeTabs, …
    ModBiomeSources.java       ✅ biome-source-type `corruptedadventure:ring`
    ModBiomes.java             ✅ ResourceKeys van alle eigen biomes (de biomes zelf komen uit datagen)
  worldgen/
    biome/                     ✅ biome-source en biome-bootstraps
      RingBiomeSource.java     ✅ kiest per positie de geneste BiomeSource van de ring (via `RingLayout`)
      PlaceholderBiomes.java   ✅ datagen-bootstrap: placeholder per ring + void-rand, overworld en Nether
    densityfunction/           🔲 eigen density functions (alleen als JSON niet volstaat)
    feature/                   🔲 eigen features
    structure/                 🔲 (fase 3) FixedRingPlacement e.d.
  block/                       🔲 custom block-classes (alleen als gedrag nodig is)
  item/                        🔲 (fase 2)
  weather/                     🔲 server-side weerstaat per ring
  condition/                   🔲 server-side omgevingscondities per ring (tick-handler)
  network/                     🔲 CustomPacketPayloads + registratie
  data/                        🔲 SavedData (wereld) en attachments (speler)
  datagen/                     ✅ providers (`./gradlew runData`); 🔲 blockstates, models, loot, tags, recipes
    DataGenerators.java        ✅ `GatherDataEvent`: datapack-registries (biomes) + lang-providers
    EnglishLanguageProvider.java ✅ `en_us` (verplicht voor elke key)
    DutchLanguageProvider.java ✅ `nl_nl`
  client/                      ✅ ALLEEN Dist.CLIENT: fog, sky, weer-rendering, particles, renderers
    CorruptedAdventureClient.java ✅ client-entrypoint (@Mod dist = CLIENT)

src/main/templates/META-INF/neoforge.mods.toml  ✅ mod-metadata; ${…} komt uit gradle.properties
src/main/resources/                (lang staat niet hier maar in datagen: `datagen/*LanguageProvider`)
  data/corruptedadventure/
    worldgen/world_preset/        ✅ corrupted_world.json
    worldgen/noise_settings/      🔲 per dimensie
    worldgen/density_function/    🔲 terrein per ring
    dimension_type/               ✅ corrupted_nether.json (coordinate_scale 4)
  data/minecraft/tags/worldgen/world_preset/normal.json  ✅ zet de preset in het menu "World Type" (replace: false)
  assets/corruptedadventure/
    textures/                     🔲 handgemaakte textures

src/test/java/nl/timtoys/corruptedadventure/
  ring/RingLayoutTest.java     ✅ grenzen, overgang, grensruis, Nether-schaal, determinisme per seed
  command/RingTargetsTest.java ✅ ringmiddens binnen hun ring, grens-teleport op gewicht 0,5, grensafstand met teken
  worldgen/biome/RingBiomeSourceTest.java ✅ biome per ringmidden (overworld + Nether), harde wissel op gewicht 0,5, alle ringen verplicht

src/generated/resources/       ✅ datagen-output — NIET handmatig bewerken, NIET lezen om te begrijpen
  data/corruptedadventure/worldgen/biome/  12 biomes uit `PlaceholderBiomes`
  assets/corruptedadventure/lang/          en_us, nl_nl

docs/
  design/                      ✅ 00-overzicht, 01-techniek, 02-beslissingenlog, fase-1-wereld (incl. bouwplan); 🔲 fase-2..5
  prompts/                     ✅ opdrachten voor Claude Code (opdracht-01-sparring-fase-1)
  backlog.md                   ✅
```

---

## Belangrijke classes

| Class | Pakket | Verantwoordelijkheid | Gebruikt door |
|---|---|---|---|
| `CorruptedAdventure` | `nl.timtoys.corruptedadventure` | common entrypoint, `MOD_ID`, `LOGGER`, `id(path)` | alles |
| `CorruptedAdventureClient` | `nl.timtoys.corruptedadventure.client` | client entrypoint (`Dist.CLIENT`) | — |
| `CommonConfig` | `…config` | COMMON-config; ringgrenzen als standaard voor **nieuwe** werelden. `ringSettings()` is package-private: alleen de snapshot leest de config | `RingSettingsSnapshot` |
| `RingSettings` | `…config` | 5 buitenradii, void-rand, grensruis, overgangsbreedte (overworld-blokken; Nether ÷4 in `ringAt`). `CODEC` + `validate()` | snapshot, straks `RingLayout` |
| `RingSettingsSnapshot` | `…config` | leest/maakt `<wereld>/corruptedadventure/ring_settings.json` bij `ServerAboutToStartEvent`, wist bij `ServerStoppedEvent`; `current()` gooit als er geen server draait | `RingLayout` |
| `Ring` | `…ring` | enum van de ringen + `VOID`; `byIndex(0..5)`, `number()` (1..6); `CODEC` op de namen `ring_1`…`void` | alles wat ringen kent |
| `RingPosition` | `…ring` | resultaat van `ringAt`: binnen-/buitenring van de dichtstbijzijnde grens en `blend` (lineair; smoothstep is aan de gebruiker) | worldgen, condities, commando's |
| `RingLayout` | `…ring` | **de enige ringfunctie**. Static `ringAt(dimension, x, z)` via `current()` (gezet bij `ServerAboutToStartEvent` met prioriteit LOW, ná de snapshot; wereldseed uit `WorldOptions`). Instantie-API `sample`/`sampleOverworld` voor tests en worldgen; `effectiveDistance` (afstand + grensruis, read-only) en static `scale(dimension)`. Ruis: simplex, golven 1/600 + rimpels 1/150, geklemd op ±`boundaryNoise`, gesampled in overworld-coördinaten | commando's, `RingBiomeSource`, straks `ring_blend`, condities |
| `CaCommand` | `…command` | `/ca`-root (permissieniveau 2); nieuwe debug-subcommando's (bijv. `/ca weather`, change 20) hier aanhaken | — |
| `RingTeleportCommand` | `…command` | teleport langs je huidige richting vanaf 0,0 (op 0,0: oost) naar ringmidden of verruisde grens; zonder `nether` altijd naar de overworld. Geen veilige plek → Y 100 / 64 + waarschuwing | `CaCommand` |
| `RingTargets` | `…command` | pure functies in overworld-blokken; `boundaryRadius` bisecteert `effectiveDistance` binnen ±`boundaryNoise`; `nearestBoundary` meet radiaal tot de echte grens in dezelfde richting | commando's, `RingTargetsTest` |
| `RingBiomeSource` | `…worldgen.biome` | biome-source `corruptedadventure:ring`. Codec: `dimension` (level-key; de source kent zijn dimensie anders niet) + `rings` (per `Ring` een geneste BiomeSource, alle 6 verplicht). `getNoiseBiome` → `RingLayout.current().sample(dimension, …).ring()` (harde wissel op gewicht 0,5) → geneste source met dezelfde sampler. `possibleBiomes` = unie, zonder layout. Overgangsbiomes: later een optioneel veld | preset `corrupted_world` |
| `PlaceholderBiomes` | `…worldgen.biome` | bootstrap voor `Registries.BIOME` (datagen): alleen kleuren + neerslag/temperatuur; geen features, geen mob-spawns, geen tags | `DataGenerators` |
| `ModBiomes` / `ModBiomeSources` | `…registry` | biome-keys / DeferredRegister voor `Registries.BIOME_SOURCE` | worldgen, datagen |
| `DataGenerators` | `…datagen` | mod-bus-listener voor `GatherDataEvent`; nieuwe providers en datapack-registries hier aanhaken | `CorruptedAdventure` |

---

## Registries

### Blocks
| Registry-naam | Ring | Class | Opmerking |
|---|---|---|---|
| _nog geen_ | | | |

### Items
| Registry-naam | Ring | Class | Opmerking |
|---|---|---|---|
| _nog geen_ | | | |

### Biomes
| Registry-naam | Ring | Dimensie | Opmerking |
|---|---|---|---|
| `placeholder_ring_1` … `placeholder_ring_5` | 1–5 | overworld | **tijdelijk** (change 5); verdwijnen als de echte biomes van die ring er zijn. Ring 1 en 4 met neerslag |
| `void_edge` | void | overworld | blijvend; voorbij ring 5 (zwevende eilanden → void) |
| `placeholder_nether_ring_1` … `placeholder_nether_ring_5` | 1–5 | Nether | **tijdelijk** (change 5); fog-kleur per Nether-tegenpool |
| `nether_void_edge` | void | Nether | blijvend |

### Overige (biome sources, density functions, payloads, attachments, …)
| Naam | Type | Waar |
|---|---|---|
| `corruptedadventure:ring` | biome source (`RingBiomeSource`) | `ModBiomeSources`; gebruikt in `corrupted_world.json` (overworld + Nether) |

---

## Build en configuratie

| Bestand | Inhoud |
|---|---|
| `gradle.properties` | MC 1.21.1, NeoForge 21.1.252, parchment 2024.11.17, `mod_id`, `mod_group_id=nl.timtoys.corruptedadventure`, versie |
| `build.gradle` | ModDevGradle; runs: client, server, gameTestServer, data (output naar `src/generated/resources`); JUnit 5 via `neoForge.unitTest` (tests draaien in een FML-omgeving, Minecraft-classes bruikbaar) |
| `.github/workflows/build.yml` | CI-build uit de MDK |
| `run/config/corruptedadventure-common.toml` | (niet in git) COMMON-config, ontstaat bij de eerste start |
| `tools/server-smoke-test.sh` | test S: start `runServer`, wacht op `Done (` of een fout, stopt de server-JVM; log in `build/runServer.log` |

## Databestanden (handgeschreven)

| Pad | Doel |
|---|---|
| `data/corruptedadventure/worldgen/world_preset/corrupted_world.json` | preset `corruptedadventure:corrupted_world`: overworld (vanilla type + noise) en `minecraft:the_nether` met type `corrupted_nether` (vanilla Nether-noise), beide met biome-source `corruptedadventure:ring` (per ring een `minecraft:fixed` met de placeholder / void-rand); The End vanilla |
| `data/corruptedadventure/dimension_type/corrupted_nether.json` | kopie van vanilla `the_nether` met `coordinate_scale` 4 (portals ÷4) |
| `data/minecraft/tags/worldgen/world_preset/normal.json` | preset zichtbaar in "World Type" (niet de standaard) |

Testseed voor test W: **`20260930`**. Test S: `run/server.properties` staat op `level-type=corruptedadventure:corrupted_world`, `level-seed=20260930`, `level-name=biome-test` (nieuwe wereldmap per test = andere `level-name`).

---

## Netwerk en persistente staat

| Naam | Soort (payload / SavedData / attachment) | Richting / scope | Doel |
|---|---|---|---|
| `ring_settings.json` | bestand in de wereldmap (`<wereld>/corruptedadventure/`) | server | ringgrenzen bevroren per wereld (geen SavedData: moet er zijn vóór `loadLevel()`) |

---

## Bekende valkuilen

- _Voeg hier dingen toe die tijd kostten om uit te zoeken, zodat een volgende sessie ze niet opnieuw hoeft te ontdekken._
- `./gradlew runServer` stuurt stdin niet door: `stop` via een pipe werkt niet. Gebruik `tools/server-smoke-test.sh` (wacht op `Done (`, stopt dan de server-JVM). `run/eula.txt` (`eula=true`) is nodig; `run/` staat in `.gitignore`.
- `ServerAboutToStartEvent` komt vóór `loadLevel()` (dedicated en integrated); dat is het moment voor staat die vóór de eerste chunk nodig is. Een exception daar crasht de server bewust; bij het afsluiten volgt dan een onschuldige vanilla-NPE (`getWorldBorder()` op een niet-geladen level).
- Unit tests draaien mee met `./gradlew build` (FML-omgeving, eerste run ~2 min). Resultaten: `build/test-results/test/*.xml`; de console toont alleen fouten.
- Heightmaps van een niet-geladen chunk lezen als leeg (`getMinBuildHeight`): eerst `level.getChunk(cx, cz)` (laadt/genereert), dan `getHeight`. Nether: niet de heightmap gebruiken (die geeft het dak), maar van onder het dak omlaag zoeken.
- De Nether-dimensie moet de key `minecraft:the_nether` houden (met ons type `corrupted_nether`): `RingLayout.scale` en vanilla's portal-logica werken op `Level.NETHER`. Portalschaal = `coordinate_scale` bron ÷ doel.
- Lang-key van een world preset: `generator.<namespace>.<path>`.
- Testwerelden van change 4 (vaste `minecraft:plains`, zit via `#is_overworld` in `#has_structure/stronghold`) hebben nog strongholds; vanaf change 5 niet meer.
- Vanilla-structures verschijnen alleen in biomes met de bijbehorende `#has_structure/*`-tag; structure sets zonder mogelijke biome in de BiomeSource worden weggefilterd. Eigen biomes zonder tags = geen strongholds (en dus geen End-portal).
- Lang staat alleen in datagen (`src/generated`). Zet geen handgeschreven `en_us.json`/`nl_nl.json` in `src/main/resources`: dat botst met de datagen-output (twee bestanden op hetzelfde pad in de jar).
- `RingBiomeSource.getNoiseBiome` vraagt `RingLayout.current()`: alleen aanroepen zolang er een server draait (worldgen doet dat altijd). Tests gebruiken de package-private variant met een eigen `RingLayout`.
- `RingLayout.current()`/`ringAt` bestaan alleen server-side zolang er een server draait. De client kent de wereldseed niet; zie backlog (change 18).
