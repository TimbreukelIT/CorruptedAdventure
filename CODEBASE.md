# CODEBASE.md — kaart van de repo

> **Lees dit bestand altijd als eerste**, vóór je code opent of gaat zoeken.
> Werk het bij in elke change die classes, registries of databestanden toevoegt, hernoemt of verwijdert.
> Houd het beknopt: per regel *wat* en *waar*, geen uitleg die al in de code staat.

**Laatst bijgewerkt:** 2026-09-30 — change 2 (`Ring`, `RingLayout.ringAt`, unit tests)
**Huidige fase:** 1 — Wereld (bouw gestart; bouwplan changes 0–96 in `docs/design/fase-1-wereld.md` §3; changes 0–2 klaar, volgende: change 3)

---

## Status per fase

| Fase | Status |
|---|---|
| 1 — Wereld | In bouw — changes 0–2 klaar |
| 2 — Items en tools | — |
| 3 — Structures | — |
| 4 — Mobs en bosses | — |
| 5 — Progressie en corruptie | — |

---

## Mappenstructuur

Status: ✅ bestaat · 🔲 gepland

```
src/main/java/nl/timtoys/corruptedadventure/
  CorruptedAdventure.java      ✅ @Mod-entrypoint (common); MOD_ID, LOGGER, id(path). Registreert straks de DeferredRegisters
  config/                      ✅ config en per-wereld-snapshot (enige plek voor ringgrenzen)
    CommonConfig.java          ✅ COMMON-config (`corruptedadventure-common.toml`): standaard-ringgrenzen; 🔲 balanswaarden
    RingSettings.java          ✅ record + Codec + validatie van de ringgrenzen
    RingSettingsSnapshot.java  ✅ snapshot per wereld; `current()` voor `ringAt` (server-side, sync in change 18)
  ring/                        ✅ ringindeling
    Ring.java                  ✅ enum RING_1…RING_5, VOID
    RingPosition.java          ✅ record (inner, outer, blend 0–1); `ring()`, `inTransition()`
    RingLayout.java            ✅ `ringAt(dimension, x, z)`: seeded grensruis, Nether ×4, overgangszones
  registry/                    🔲 ModBlocks, ModItems, ModBiomes, ModCreativeTabs, ModBiomeSources, …
  worldgen/
    biome/                     🔲 RingBiomeSource (+ codec)
    densityfunction/           🔲 eigen density functions (alleen als JSON niet volstaat)
    feature/                   🔲 eigen features
    structure/                 🔲 (fase 3) FixedRingPlacement e.d.
  block/                       🔲 custom block-classes (alleen als gedrag nodig is)
  item/                        🔲 (fase 2)
  weather/                     🔲 server-side weerstaat per ring
  condition/                   🔲 server-side omgevingscondities per ring (tick-handler)
  network/                     🔲 CustomPacketPayloads + registratie
  data/                        🔲 SavedData (wereld) en attachments (speler)
  datagen/                     🔲 providers: blockstates, models, loot, tags, recipes, lang
  client/                      ✅ ALLEEN Dist.CLIENT: fog, sky, weer-rendering, particles, renderers
    CorruptedAdventureClient.java ✅ client-entrypoint (@Mod dist = CLIENT)

src/main/resources/
  assets/corruptedadventure/lang/en_us.json  ✅ leeg; wordt straks door datagen vervangen
src/main/templates/META-INF/neoforge.mods.toml  ✅ mod-metadata; ${…} komt uit gradle.properties
  data/corruptedadventure/
    worldgen/world_preset/        🔲 corrupted_world.json
    worldgen/noise_settings/      🔲 per dimensie
    worldgen/density_function/    🔲 terrein per ring
    worldgen/biome/               🔲 biomes per ring (of via datagen)
    dimension_type/               🔲 corrupted_nether.json (coordinate_scale 4)
  assets/corruptedadventure/
    textures/                     🔲 handgemaakte textures

src/test/java/nl/timtoys/corruptedadventure/
  ring/RingLayoutTest.java     ✅ grenzen, overgang, grensruis, Nether-schaal, determinisme per seed

src/generated/resources/       🔲 datagen-output — NIET handmatig bewerken, NIET lezen om te begrijpen

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
| `Ring` | `…ring` | enum van de ringen + `VOID`; `byIndex(0..5)`, `number()` (1..6) | alles wat ringen kent |
| `RingPosition` | `…ring` | resultaat van `ringAt`: binnen-/buitenring van de dichtstbijzijnde grens en `blend` (lineair; smoothstep is aan de gebruiker) | worldgen, condities, commando's |
| `RingLayout` | `…ring` | **de enige ringfunctie**. Static `ringAt(dimension, x, z)` via `current()` (gezet bij `ServerAboutToStartEvent` met prioriteit LOW, ná de snapshot; wereldseed uit `WorldOptions`). Instantie-API `sample`/`sampleOverworld` voor tests en worldgen. Ruis: simplex, golven 1/600 + rimpels 1/150, geklemd op ±`boundaryNoise`, gesampled in overworld-coördinaten | straks BiomeSource, `ring_blend`, condities |

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
| _nog geen_ | | | |

### Overige (biome sources, density functions, payloads, attachments, …)
| Naam | Type | Waar |
|---|---|---|
| _nog geen_ | | |

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
| _nog geen_ | |

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
- `RingLayout.current()`/`ringAt` bestaan alleen server-side zolang er een server draait. De client kent de wereldseed niet; zie backlog (change 18).
