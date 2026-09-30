# CODEBASE.md — kaart van de repo

> **Lees dit bestand altijd als eerste**, vóór je code opent of gaat zoeken.
> Werk het bij in elke change die classes, registries of databestanden toevoegt, hernoemt of verwijdert.
> Houd het beknopt: per regel *wat* en *waar*, geen uitleg die al in de code staat.

**Laatst bijgewerkt:** 2026-09-30 — repo opgezet vanuit NeoForge MDK 1.21.1 (ModDevGradle), example-code verwijderd
**Huidige fase:** 1 — Wereld (sparring sessie 1 klaar: fundament + ring 1–2; bouwplan in `docs/design/fase-1-wereld.md` §3)

---

## Status per fase

| Fase | Status |
|---|---|
| 1 — Wereld | Sparring |
| 2 — Items en tools | — |
| 3 — Structures | — |
| 4 — Mobs en bosses | — |
| 5 — Progressie en corruptie | — |

---

## Mappenstructuur

Status: ✅ bestaat · 🔲 gepland

```
src/main/java/nl/corruptedadventure/
  CorruptedAdventure.java      ✅ @Mod-entrypoint (common); MOD_ID, LOGGER, id(path). Registreert straks de DeferredRegisters
  config/                      🔲 ModConfig: ringgrenzen, balanswaarden (enige plek voor ringgrenzen)
  ring/                        🔲 Ring (enum), RingLayout.ringAt(dimension, x, z)
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
| `CorruptedAdventure` | `nl.corruptedadventure` | common entrypoint, `MOD_ID`, `LOGGER`, `id(path)` | alles |
| `CorruptedAdventureClient` | `nl.corruptedadventure.client` | client entrypoint (`Dist.CLIENT`) | — |

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
| `gradle.properties` | MC 1.21.1, NeoForge 21.1.252, parchment 2024.11.17, `mod_id`, `mod_group_id=nl.corruptedadventure`, versie |
| `build.gradle` | ModDevGradle; runs: client, server, gameTestServer, data (output naar `src/generated/resources`) |
| `.github/workflows/build.yml` | CI-build uit de MDK |

## Databestanden (handgeschreven)

| Pad | Doel |
|---|---|
| _nog geen_ | |

---

## Netwerk en persistente staat

| Naam | Soort (payload / SavedData / attachment) | Richting / scope | Doel |
|---|---|---|---|
| _nog geen_ | | | |

---

## Bekende valkuilen

- _Voeg hier dingen toe die tijd kostten om uit te zoeken, zodat een volgende sessie ze niet opnieuw hoeft te ontdekken._
