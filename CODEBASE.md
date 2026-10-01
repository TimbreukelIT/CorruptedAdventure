# CODEBASE.md — kaart van de repo

> **Lees dit bestand altijd als eerste**, vóór je code opent of gaat zoeken.
> Werk het bij in elke change die classes, registries of databestanden toevoegt, hernoemt of verwijdert.
> Houd het beknopt: per regel *wat* en *waar*, geen uitleg die al in de code staat.

**Laatst bijgewerkt:** 2026-10-01 — change 13 (sfeer ring 1: stille/vreemde grotgeluiden per biome via `mood_sound`, valere lucht/fog/water in `withered_expanse`; eerste sound event)
**Huidige fase:** 1 — Wereld (bouw gestart; bouwplan changes 0–96 in `docs/design/fase-1-wereld.md` §3; changes 0–13 klaar, volgende: change 14)

---

## Status per fase

| Fase | Status |
|---|---|
| 1 — Wereld | In bouw — changes 0–13 klaar |
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
    RingLayout.java            ✅ `ringAt(dimension, x, z)`: seeded grensruis, Nether ×4, overgangszones; `voidEdgeProgress`, `ringProgress`
    RingColumnCache.java       ✅ per-thread kolomcache van `RingLayout` voor worldgen (ring + void-voortgang per x,z)
  command/                     ✅ debugcommando's `/ca …` (OP-only, server-side; vaste Engelse tekst)
    CaCommand.java             ✅ `/ca`-root, registratie via `RegisterCommandsEvent`
    RingInfoCommand.java       ✅ `/ca ring`
    RingTeleportCommand.java   ✅ `/ca tpring <1–6> [nether]`, `/ca tpboundary <1–5> [nether]` + veilige landingshoogte
    BorderInfoCommand.java     ✅ `/ca border`: border (grootte, midden, lerp) van de huidige dimensie
    RingTargets.java           ✅ pure rekenwerk: richting, ringmidden, verruisde grens (bisectie), dichtstbijzijnde grens
  registry/                    ✅ DeferredRegisters, ResourceKeys en TagKeys
    ModBlocks.java             ✅ alle blokken + hun blockitem (`register` / `registerDoor`); volgorde = creative-tab-volgorde; `entries()`
    ModItems.java              ✅ alle items (nu alleen blockitems via `ModBlocks`); `ITEMS` package-private; `entries()`
    ModCreativeTabs.java       ✅ één tab `corruptedadventure:main` met alle `ModItems` in registratievolgorde
    ModTags.java               ✅ eigen TagKeys (`Blocks`/`Items`: `dead_logs`; `Biomes`: `has_feature/bare_patch`, `has_feature/unnatural_tunnel`)
    ModBiomeSources.java       ✅ biome-source-types `corruptedadventure:ring`, `vanilla_climate`
    ModBiomes.java             ✅ ResourceKeys van alle eigen biomes (de biomes zelf komen uit datagen)
    ModDensityFunctionTypes.java ✅ density-function-types `ring_blend`, `void_edge`, `radial_falloff`
    ModChunkGenerators.java    ✅ chunk-generator-type `ring_noise`
    ModPlacementModifierTypes.java ✅ placement-modifier-type `ring_chance`
    ModFeatures.java           ✅ feature-types met eigen code: `bare_patch`, `unnatural_tunnel`
    ModConfiguredFeatures.java ✅ ResourceKeys van de eigen configured features (de features zelf komen uit datagen)
    ModPlacedFeatures.java     ✅ ResourceKeys van de eigen placed features (idem)
    ModSoundEvents.java        ✅ sound events (common: biome-JSON noemt ze): `ambient.ring_1.strange_cave`
  worldgen/
    biome/                     ✅ biome-source en biome-bootstraps
      RingBiomeSource.java     ✅ kiest per positie de geneste BiomeSource van de ring (via `RingLayout`)
      PlaceholderBiomes.java   ✅ datagen-bootstrap: placeholder per ring (overworld vanaf ring 2) + void-rand, overworld en Nether
      VanillaBiomeCopies.java  ✅ ring 1: `OVERWORLD_COPIES` (52 vanilla-keys), `copyOf(key)`, `MOODS` (stille kopieën), datagen-bootstraps via vanilla's `BiomeData` en `WolfVariants`
      BiomeAmbience.java       ✅ sfeer in datagen: mood-profielen `SILENT`/`STRANGE`, `withMood` (biome herbouwen met alleen een andere mood), `desaturate`, `Palette` (`RING_1`)
      VanillaClimateBiomeSource.java ✅ `vanilla_climate`: vanilla multi-noise-preset met onze kopieën (+ `replace`)
      TransitionBiomes.java    ✅ datagen-bootstrap van de overgangsbiomes (één per grens): `withered_expanse`
      Ring1BiomeModifiers.java ✅ datagen-bootstrap biome modifiers `add_bare_patch`, `add_unnatural_tunnel` (NeoForge `add_features` op de `has_feature/*`-tags)
    densityfunction/           ✅ eigen density functions (alleen als JSON niet volstaat)
      RingBlend.java           ✅ `ring_blend`: terrein per ring, smoothstep in de overgangszone
      VoidEdge.java            ✅ `void_edge`: 0 op de grens van ring 5 → 1 op de border (voor de void-JSON)
      RadialFalloff.java       ✅ `radial_falloff`: 1 binnen `inner_radius` van 0,0 → 0 vanaf `outer_radius` (smoothstep)
    chunk/                     ✅ chunk-generator
      RingChunkGenerator.java  ✅ `ring_noise`: vanilla `noise` + geen vloeistof in de void-zone
      RingFluidPicker.java     ✅ globale zee: geen vloeistof vanaf de zone ring 5 → void, anders vanilla
      RingAquifer.java         ✅ wrapper om de aquifer van elke noise chunk: ook geen lokale (lava)vloeistof in de void-zone
    border/                    ✅ world border (server-side)
      RingWorldBorder.java     ✅ zet de overworld-border bij de start (alleen vanilla-standaard, alleen `ring_noise`-werelden), leidt de Nether-border af, stuurt die naar Nether-spelers
      ScaledBorderListener.java ✅ listener op de overworld-border: geschaalde dimensie = grootte ÷ `coordinate_scale` (ook lerps); `clientView` met ongeschaald midden
    feature/                   ✅ eigen features en placement modifiers
      RingChanceFilter.java    ✅ placement modifier `ring_chance`: kans volgt een ringsignaal (`transition`, `progress`)
      WitheredExpanseFeatures.java ✅ datagen-bootstraps configured + placed features van `withered_expanse` (kale bomen, begroeiing, keien, zandvlekken)
      BarePatchConfiguration.java ✅ config van `bare_patch` (record + codec)
      BarePatchFeature.java    ✅ kale plek: rafelige plek dood gras/verdorde aarde, bestaande bomen verdorren, eigen dode bomen + dode struiken
      TunnelPath.java          ✅ pure padvorm van een tunnel (richting, lengtes, bocht) + mondkandidaten binnen de 3×3 chunks
      UnnaturalTunnelConfiguration.java ✅ config van `unnatural_tunnel` (record + codec)
      UnnaturalTunnelFeature.java ✅ onnatuurlijke 2×1-tunnel: mond zoeken, hele pad valideren (alles-of-niets), uithakken
      Ring1HorrorFeatures.java ✅ datagen-bootstraps configured + placed features `bare_patch`, `unnatural_tunnel`
    structure/                 🔲 (fase 3) FixedRingPlacement e.d.
  block/                       ✅ custom block-classes (alleen als gedrag nodig is) en blok-hulpklassen
    DeadGrassBlock.java        ✅ dood gras: `SnowyDirtBlock` met eigen random tick (→ verdorde aarde / spreidt alleen naar verdorde aarde), schop/schoffel
    WitheredDirtBlock.java     ✅ verdorde aarde: schop → pad, schoffel → farmland; `dirtToolState` gedeeld met `DeadGrassBlock`
    StrippableLogBlock.java    ✅ log/wood die een bijl naar een gestripte variant (`Supplier<Block>`) maakt, as behouden; voor elke houtset
    ModWoodTypes.java          ✅ `BlockSetType DEAD_SET` (geregistreerd) en `WoodType DEAD` (bewust niet geregistreerd); latere houtsets hier
    ModFlammables.java         ✅ vuurkansen (zoals eikenhout) via `FireBlock#setFlammable` in `FMLCommonSetupEvent#enqueueWork`
  item/                        🔲 (fase 2)
  weather/                     🔲 server-side weerstaat per ring
  condition/                   🔲 server-side omgevingscondities per ring (tick-handler)
  network/                     🔲 CustomPacketPayloads + registratie
  data/                        🔲 SavedData (wereld) en attachments (speler)
  datagen/                     ✅ providers (`./gradlew runData`); 🔲 recipes
    DataGenerators.java        ✅ `GatherDataEvent`: datapack-registries (configured/placed features, biomes, wolf-varianten, biome modifiers) + tags, data maps, lang, blockstates/models, loot
    ModBlockStateProvider.java ✅ blockstates + blockmodels (NeoForge-helpers; `render_type` cutout voor deur/valluik in de model-JSON)
    ModItemModelProvider.java  ✅ itemmodels: blockmodel als parent; hek/knop inventory-model, valluik `_bottom`, deur plat item
    ModBlockLootProvider.java  ✅ blok-loot (`create` → `LootTableProvider`); `getKnownBlocks` = alle `ModBlocks`, dus elk blok moet een table hebben
    ModBlockTagsProvider.java  ✅ blok-tags (vanilla `wooden_*`/`planks`/`logs_that_burn`/`dirt`/`mineable/shovel`, `c:`-tags, `dead_logs`)
    ModItemTagsProvider.java   ✅ item-tags: kopie van de blok-tags
    ModSoundDefinitionsProvider.java ✅ `sounds.json` (client-asset): eigen events als `event`-verwijzing naar vanilla-geluiden + ondertitel
    ModBiomeTagsProvider.java  ✅ spiegelt vanilla biome-tags naar de ring 1-kopieën (`data/minecraft/tags/…`, `replace: false`) + eigen `has_feature/*`-tags
    ModDataMapProvider.java    ✅ NeoForge-data maps: `villager_types` voor de kopieën
    EnglishLanguageProvider.java ✅ `en_us` (verplicht voor elke key)
    DutchLanguageProvider.java ✅ `nl_nl`
  client/                      ✅ ALLEEN Dist.CLIENT: fog, sky, weer-rendering, particles, renderers
    CorruptedAdventureClient.java ✅ client-entrypoint (@Mod dist = CLIENT)

src/main/templates/META-INF/neoforge.mods.toml  ✅ mod-metadata; ${…} komt uit gradle.properties
src/main/resources/                (lang staat niet hier maar in datagen: `datagen/*LanguageProvider`)
  data/corruptedadventure/
    worldgen/world_preset/        ✅ corrupted_world.json
    worldgen/noise_settings/      ✅ overworld.json, nether.json (kopie vanilla, terrein via `ring_blend`)
    worldgen/density_function/    ✅ `ring_blend/<dim>` (+ `overworld_initial`), `ring_1…5/<dim>`, `ring_1/overworld/*` (vanilla-keten), `void/<dim>` (dim = overworld, nether)
    worldgen/noise/               ✅ placeholder_terrain, void_islands (noise-parameters)
    dimension_type/               ✅ corrupted_nether.json (coordinate_scale 4)
  data/minecraft/tags/worldgen/world_preset/normal.json  ✅ zet de preset in het menu "World Type" (replace: false)
  META-INF/accesstransformer.cfg  ✅ opent `NoiseBasedChunkGenerator.globalFluidPicker` en `NoiseChunk.aquifer` (voor `ring_noise`)
  assets/corruptedadventure/
    textures/                     ✅ uit `tools/textures/Ring1Textures.java` (gecommit): `block/` withered_dirt, dead_grass_block_{top,side,snow}, dead_log(_top), stripped_dead_log(_top), dead_planks, dead_door_{top,bottom}, dead_trapdoor; `item/dead_door`

src/test/java/nl/timtoys/corruptedadventure/
  ring/RingLayoutTest.java     ✅ grenzen, overgang, grensruis, Nether-schaal, determinisme per seed, `ringProgress` (ring 1 en ring 2)
  command/RingTargetsTest.java ✅ ringmiddens binnen hun ring, grens-teleport op gewicht 0,5, grensafstand met teken
  worldgen/biome/RingBiomeSourceTest.java ✅ biome per ringmidden (overworld + Nether), harde wissel op gewicht 0,5, alle ringen verplicht; `transitions`: hele zone = overgangsbiome, andere grenzen hard, `possibleBiomes`, codec (optioneel, `void` fout)
  worldgen/biome/VanillaBiomeCopiesTest.java ✅ 52 kopieën zonder mushroom, bootstrap registreert precies de kopieën in onze namespace, wolf-varianten krijgen de kopieën
  worldgen/biome/BiomeAmbienceTest.java ✅ `withMood` verandert alleen de mood (alle vanilla-biomes: klimaat, generation, spawns, overige effecten gelijk), `Palette.RING_1` = vanilla plains, `desaturate`, `withered_expanse` vreemd + vaal, `MOODS`-kopieën stil en de rest vanilla
  worldgen/biome/VanillaClimateBiomeSourceTest.java ✅ elk vanilla-parameterpunt → kopie/vervanger, lookup = vanilla, possibleBiomes, fouten (ontbrekende kopie, onbekende `replace`), encode
  worldgen/biome/StandaloneHolderGetter.java ✅ test-hulp: nep-`HolderGetter` met losse references; `ops(registry, known)` = JSON-`RegistryOps` voor codec-tests
  worldgen/feature/RingChanceFilterTest.java ✅ kanscurve (op-/aflopend, `start`, `exponent`), signaal `transition` alleen in de eigen zone, signaal `progress` (0 op 0,0 → ≈ 1 aan de rand, 0 buiten de ring, Nether ×4), codec (rondreis, standaardwaarden, fouten)
  worldgen/feature/TunnelPathTest.java ✅ zelfde seed = zelfde pad, lengtes binnen de providers, bocht 90°, recht en horizontaal, elke mondkandidaat houdt het pad (+ 1 blok buren) binnen de 3×3 chunks
  datagen/ModBiomeTagsProviderTest.java ✅ `is_overworld`, stronghold-tags en `has_structure/ancient_city` worden nooit gespiegeld; `BARE_PATCH_BIOMES` uniek en allemaal gekopieerd
  ring/RingColumnCacheTest.java ✅ cache = layout (ook na verdringen), Nether/overworld niet verward, nieuwe layout wist
  worldgen/densityfunction/RingBlendTest.java ✅ ring kiezen, smoothstep-menging, bereik, ontbrekende ring
  registry/ModBlocksTest.java  ✅ elk blok heeft een blockitem met dezelfde naam; de creative tab toont alle items in registratievolgorde
  worldgen/densityfunction/RadialFalloffTest.java ✅ 1 binnen / 0 buiten, 0,5 halverwege, monotoon, horizontale afstand, codec (inner < outer, niet negatief)
  worldgen/chunk/RingFluidPickerTest.java ✅ zee in de ringen, geen vloeistof vanaf de zone ring 5 → void
  worldgen/chunk/RingAquiferTest.java ✅ geen lokale lava in de void-zone, vast blijft vast, ringen houden vanilla-vloeistof
  worldgen/border/RingWorldBorderTest.java ✅ startgrootte, vanilla-standaard (grootte én midden), Nether ÷4 (ook lerp), client-view houdt het ongeschaalde midden

src/generated/resources/       ✅ datagen-output — NIET handmatig bewerken, NIET lezen om te begrijpen
  data/corruptedadventure/worldgen/biome/  11 uit `PlaceholderBiomes` + 52 kopieën uit `VanillaBiomeCopies` + `withered_expanse` (`TransitionBiomes`)
  data/corruptedadventure/worldgen/{configured,placed}_feature/  eigen features (`WitheredExpanseFeatures`, `Ring1HorrorFeatures`)
  data/corruptedadventure/neoforge/biome_modifier/  `add_bare_patch`, `add_unnatural_tunnel` (`Ring1BiomeModifiers`)
  data/corruptedadventure/tags/worldgen/biome/has_feature/  eigen biome-tags voor de biome modifiers (`ModBiomeTagsProvider`)
  data/minecraft/tags/worldgen/biome/**    gespiegelde vanilla-tags met de kopieën (`ModBiomeTagsProvider`)
  data/neoforge/data_maps/worldgen/biome/villager_types.json  villager-types van de kopieën
  data/minecraft/wolf_variant/             6 overschreven vanilla-wolf-varianten (vanilla-biome + kopie)
  assets/corruptedadventure/lang/          en_us, nl_nl
  assets/corruptedadventure/sounds.json    sound-definities (`ModSoundDefinitionsProvider`)
  assets/corruptedadventure/blockstates/, models/block/, models/item/   blokken en items (`ModBlockStateProvider`, `ModItemModelProvider`)
  data/corruptedadventure/loot_table/blocks/  blok-loot (`ModBlockLootProvider`)
  data/{corruptedadventure,minecraft,c}/tags/{block,item}/  blok- en item-tags (`ModBlockTagsProvider`, `ModItemTagsProvider`)

docs/
  design/                      ✅ 00-overzicht, 01-techniek, 02-beslissingenlog, fase-1-wereld (incl. bouwplan); 🔲 fase-2..5
  prompts/                     ✅ opdrachten voor Claude Code (opdracht-01-sparring-fase-1)
  plans/                       ✅ uitgewerkte bouwplannen per change (`change-NN.md`: scope, stappen, tests, beantwoorde ontwerpvragen); klaar: 08–17. Een bouw-subagent leest eerst zijn plan
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
| `RingColumnCache` | `…ring` | ThreadLocal, direct-mapped op de lage 5 bits van x/z (32×32 kolommen), sleutel in overworld-coördinaten; wist zichzelf bij een andere `RingLayout` (nieuwe wereld). Static `ringAt`/`voidEdgeProgress` via `RingLayout.current()` | `ring_blend`, `void_edge`, `RingFluidPicker` |
| `RingLayout` | `…ring` | **de enige ringfunctie**. Static `ringAt(dimension, x, z)` via `current()` (gezet bij `ServerAboutToStartEvent` met prioriteit LOW, ná de snapshot; wereldseed uit `WorldOptions`). Instantie-API `sample`/`sampleOverworld` voor tests en worldgen; `effectiveDistance` (afstand + grensruis, read-only), `voidEdgeProgress` (0 op de verruisde grens van ring 5, 1 op `voidOuterRadius` zonder ruis, geklemd), `ringProgress(ring, x, z)` (0 op de binnengrens — ring 1: 0,0 — tot 1 op de verruisde buitengrens, beide grenzen verschoven met de ruis op die plek; leeg buiten de ring, zelfde wissel als `ring()`; `VOID` gooit) en static `scale(dimension)`. Ruis: simplex, golven 1/600 + rimpels 1/150, geklemd op ±`boundaryNoise`, gesampled in overworld-coördinaten | commando's, `RingBiomeSource`, `RingColumnCache`, straks condities |
| `CaCommand` | `…command` | `/ca`-root (permissieniveau 2); nieuwe debug-subcommando's (bijv. `/ca weather`, change 20) hier aanhaken | — |
| `RingTeleportCommand` | `…command` | teleport langs je huidige richting vanaf 0,0 (op 0,0: oost) naar ringmidden of verruisde grens; zonder `nether` altijd naar de overworld. Geen veilige plek → Y 100 / 64 + waarschuwing | `CaCommand` |
| `RingTargets` | `…command` | pure functies in overworld-blokken; `boundaryRadius` bisecteert `effectiveDistance` binnen ±`boundaryNoise`; `nearestBoundary` meet radiaal tot de echte grens in dezelfde richting | commando's, `RingTargetsTest` |
| `RingBiomeSource` | `…worldgen.biome` | biome-source `corruptedadventure:ring`. Codec: `dimension` (level-key; de source kent zijn dimensie anders niet) + `rings` (per `Ring` een geneste BiomeSource, alle 6 verplicht; overworld ring 1 = `vanilla_climate`). `getNoiseBiome` → `RingLayout.current().sample(dimension, …).ring()` (harde wissel op gewicht 0,5) → geneste source met dezelfde sampler. `possibleBiomes` = unie van ringen en overgangen, zonder layout. Optioneel `transitions` (sleutel = binnenring van de grens, `void` ongeldig): in een overgangszone met overgangsbiome wint die source in de **hele** zone (ook boven water); andere grenzen hard op 0,5 | preset `corrupted_world` |
| `TransitionBiomes` | `…worldgen.biome` | datagen-bootstrap van de overgangsbiomes. Vanilla-helpers in vanilla-volgorde (eigen kopie van het private `globalOverworldGeneration`), eigen features achteraan per stap (anders "Feature order cycle") | `DataGenerators` |
| `RingChanceFilter` | `…worldgen.feature` | placement modifier `corruptedadventure:ring_chance`: `signal` (enum `transition`, `progress`), `ring`, `from_chance`, `to_chance`, `start` (0), `exponent` (1). Kans = 0 zonder signaal of onder `start`, anders `from + (to − from) · ((t − start)/(1 − start))^exponent`; trekking met vanilla's geseede decoration-random. `transition`: `t` = blend als de positie in de zone van grens `ring` → `ring + 1` ligt. `progress`: `t` = `RingLayout.ringProgress(ring, x·s, z·s)` (`s` = `scale(dim)`), geen signaal buiten `ring`; `void` ongeldig. `RingLayout.current()` alleen in `shouldPlace`; dimensie via `context.getLevel().getLevel()`. Nieuw signaal = enumwaarde + één `case` | placed features van `withered_expanse`, `bare_patch` |
| `BarePatchFeature` | `…worldgen.feature` | `corruptedadventure:bare_patch`. Rafelige rand: 8 punten rond de cirkel met elk een eigen inkeping (`edge_jitter`), lineair ertussen. Per kolom vanaf `WORLD_SURFACE` max. 32 blokken omlaag: bladeren → lucht (of water als ze waterlogged waren; tot 3 blokken buiten de rand met afnemende kans; een sneeuwlaag erop verdwijnt mee), binnen de plek `#logs` → `dead_log` (as behouden) en kleine planten weg zodra er `replaceable` grond is; stopt bij vloeistof of andere grond. Bovenste blok → `surface` (onder een stam `subsurface`), `snowy` volgt de sneeuwlaag; daaronder `subsurface_depth` × `#dirt` → `subsurface`. Daarna `tree_count` bomen op nieuwe `surface`-blokken binnen 0,7 × straal met 2 lucht erboven, dan dode struiken (`dead_bush_chance`). Max. straal 9 (codec) | `Ring1HorrorFeatures` |
| `UnnaturalTunnelFeature` | `…worldgen.feature` | `corruptedadventure:unnatural_tunnel`. `TunnelPath.random` → mond = eerste kandidaat (langs de richting, door de herkomst, op herkomsthoogte) met 2 lege blokken ervoor en 2 `replaceable` blokken erin → valideren (geen vloeistof in/naast een cel incl. vloer en dak, geen `FallingBlock` in het dak, geen rots in een cel zonder dak — anders geul of prop —, geen `#features_cannot_replace`, ≥ `min_solid_fraction` rots) → alle rotsblokken van de cellen → `cave_air`. Alles-of-niets | `Ring1HorrorFeatures` |
| `TunnelPath` | `…worldgen.feature` | record (richting, optionele bocht, lengtes); `cells(mouth)` en `mouthCandidates(origin)`: alleen monden waarbij het blok ervoor en alle cellen (+ 1 blok buren) binnen de 3×3 chunks rond de herkomstchunk blijven. Max. lengtes 32 / 15 (codec) | `UnnaturalTunnelFeature`, `TunnelPathTest` |
| `Ring1HorrorFeatures` | `…worldgen.feature` | bootstraps `configured`/`placed`. `bare_patch`: straal 4–9, `edge_jitter` 0,35, `dead_grass_block`/`withered_dirt` (1–2 diep), grond grass/dirt/coarse dirt/podzol/rooted dirt, bomen `withered_trees` (1–3), struiken 0,05; placed `in_square` → `ring_chance` (`progress`, ring 1, 0 → 0,12, start 0,33, exponent 2) → heightmap `MOTION_BLOCKING_NO_LEAVES` → `biome`. `unnatural_tunnel`: 12–28 + 50 % bocht 6–14, `#overworld_carver_replaceables`, rots ≥ 0,7; placed `rarity_filter` 12 → `in_square` → Y −48…160 → `biome` | `DataGenerators` |
| `Ring1BiomeModifiers` | `…worldgen.biome` | NeoForge `add_features`: `add_bare_patch` (`#has_feature/bare_patch`, `VEGETAL_DECORATION`), `add_unnatural_tunnel` (`#has_feature/unnatural_tunnel`, `TOP_LAYER_MODIFICATION`); elk achteraan zijn stap. Features in de kopieën altijd via modifiers, niet in `VanillaBiomeCopies` | `DataGenerators` |
| `WitheredExpanseFeatures` | `…worldgen.feature` | bootstraps `configured`/`placed`. `dead_tree`/`dead_tree_forked` = `minecraft:tree` met `dead_log` en lucht als blad (`BlobFoliagePlacer` 0), ondergrond `withered_dirt` (alleen als de grond geen `#dirt` is); het placed feature filtert zelf op `#dirt` eronder (geen sapling) | `DataGenerators` |
| `VanillaBiomeCopies` | `…worldgen.biome` | `OVERWORLD_COPIES` = vanilla's `Preset.OVERWORLD.usedBiomes()` zonder mushroom fields (52); `copyOf` = `corruptedadventure:<zelfde pad>`. `bootstrap` draait vanilla's `BiomeData.bootstrap` met een doorgeefcontext die alleen die keys registreert, onder `copyOf`; kopieën in `MOODS` (dark forest, old growth pine/spruce taiga, dripstone caves → `SILENT`) gaan door `BiomeAmbience.withMood`. `bootstrapWolfVariants` draait vanilla's `WolfVariants.bootstrap` en overschrijft alleen de varianten met een biome-lijst (pale, snowy, black, ashen, woods, chestnut): vanilla-biome + kopie; tag-varianten blijven vanilla | `DataGenerators`, `VanillaClimateBiomeSource`, tags, data map, lang |
| `VanillaClimateBiomeSource` | `…worldgen.biome` | biome-source `corruptedadventure:vanilla_climate`. Codec: `preset` (vanilla `minecraft:overworld`/`minecraft:nether`) + optioneel `replace` (vanilla-key → eigen key, voor biomes zonder kopie) + biome-getter uit de `RegistryOps`. Bouwt bij het decoden eenmalig de `Climate.ParameterList` (R-tree) met `replace` of `copyOf`; ontbrekende biome of onbekende `replace`-key → `DataResult.error`. Slaat alleen `preset` + `replace` op | preset `corrupted_world` (ring 1) |
| `BiomeAmbience` | `…worldgen.biome` | pure datagen-hulp. `SILENT` = geen `mood_sound`; `STRANGE` = `ambient.ring_1.strange_cave`, `tick_delay` 1800, extent 12, offset 3. `withMood` bouwt de biome opnieuw (klimaat incl. temperatuur-modifier, generation en spawns als hetzelfde object, effecten via een `BiomeSpecialEffectsBuilder`-subklasse, omdat vanilla's builder een mood niet kan wissen). `desaturate(rgb, amount)` mengt naar de luma-grijswaarde; `Palette.desaturated` per kleur | `VanillaBiomeCopies`, `TransitionBiomes` |
| `PlaceholderBiomes` | `…worldgen.biome` | bootstrap voor `Registries.BIOME` (datagen): alleen kleuren + neerslag/temperatuur; geen features, geen mob-spawns, geen tags | `DataGenerators` |
| `RingBlend` | `…worldgen.densityfunction` | `corruptedadventure:ring_blend`. Codec: `dimension` + `rings` (per `Ring` een density function, alle 6 verplicht). Rekent alleen de 1–2 nodige inputs; `mapAll` geeft de visitor door aan alle inputs | `ring_blend/*.json` |
| `VoidEdge` | `…worldgen.densityfunction` | `corruptedadventure:void_edge` (`dimension`); 2D, 0–1 | `void/*.json` |
| `RadialFalloff` | `…worldgen.densityfunction` | `corruptedadventure:radial_falloff` (`inner_radius`, `outer_radius`, blokken in de eigen dimensie; 0 ≤ inner < outer); 2D, 0–1, stateless, geen `RingLayout`. Waarden in JSON, niet in config (wijziging = naden in bestaande werelden) | `ring_1/overworld/continents.json` |
| `RingChunkGenerator` | `…worldgen.chunk` | `corruptedadventure:ring_noise`: codec van vanilla `noise` + `dimension`; vervangt in de constructor de globale vloeistof-kiezer door `RingFluidPicker` en maakt in `createBiomes`/`fillFromNoise`/`buildSurface`/`applyCarvers` zelf de noise chunk (met `RingAquifer`), omdat vanilla's fabriek private is | preset `corrupted_world` |
| `RingFluidPicker` | `…worldgen.chunk` | `noFluid(position)` = `outer == VOID` (één regel voor beide lagen); anders de vanilla-kiezer (zeeniveau / lava onder -54). Ook gebruikt door hoogtevragen (`getBaseHeight`, structure-plaatsing) | `RingChunkGenerator`, `RingAquifer` |
| `RingAquifer` | `…worldgen.chunk` | wrapt vanilla's aquifer: open ruimte in de void-zone → lucht, geen fluid-update; anders vanilla | `RingChunkGenerator` |
| `RingWorldBorder` | `…worldgen.border` | `ServerStartingEvent` (ná `loadLevel()`, dat de opgeslagen border pas aan het eind toepast): per dimensie met `coordinate_scale` ≠ 1 een `ScaledBorderListener` op de overworld-border (ná vanilla's delegate); daarna overworld → midden 0,0, diameter 2 × `voidOuterRadius` als hij nog op de vanilla-standaard staat (grootte 59999968, midden 0,0, geen lerp). Bekende beperking: een admin die bewust terugzet naar 59999968 op 0,0 krijgt na een herstart weer 10.000 (geaccepteerd; geen SavedData-vlag). Na login/dimensiewissel/respawn een `ClientboundInitializeBorderPacket` naar spelers in een geschaalde dimensie. Preset-check: overworld-generator is `RingChunkGenerator` | `CorruptedAdventure` |
| `ScaledBorderListener` | `…worldgen.border` | past alleen de grootte aan (set/lerp); midden, waarschuwing en schade komen van vanilla's delegate; broadcast via callback naar spelers in die dimensie | `RingWorldBorder` |
| `ModBiomes` / `ModBiomeSources` | `…registry` | biome-keys / DeferredRegister voor `Registries.BIOME_SOURCE` | worldgen, datagen |
| `DataGenerators` | `…datagen` | mod-bus-listener voor `GatherDataEvent`; nieuwe providers en datapack-registries hier aanhaken (configured/placed features vóór de biomes, biome modifiers erna). Alle bootstraps van één registry in één lambda (een registry mag maar één keer in de `RegistrySetBuilder`). Mod-ids `corruptedadventure` + `minecraft` (de wolf-varianten overschrijven vanilla-entries) | `CorruptedAdventure` |
| `ModBiomeTagsProvider` | `…datagen` | draait vanilla's `BiomeTagsProvider.addTags` en houdt per tag in `MIRRORED_TAGS` alleen de directe entries van gekopieerde biomes over (als `copyOf`); `#tag`-entries lossen vanzelf op. Tags zonder kopieën → geen bestand. Daarna eigen tags: `has_feature/bare_patch` (`BARE_PATCH_BIOMES`: 21 begroeide land-kopieën), `has_feature/unnatural_tunnel` (alle 52 kopieën, gesorteerd) | `DataGenerators` |
| `ModDataMapProvider` | `…datagen` | `NeoForgeDataMaps.VILLAGER_TYPES` voor de kopieën, gelijk aan vanilla's `VillagerType.BY_BIOME` (29 entries) | `DataGenerators` |

---

## Registries

### Blocks
Volgorde = creative-tab-volgorde. Properties via `ofFullCopy` van het vanilla-equivalent.

| Registry-naam | Ring | Class | Opmerking |
|---|---|---|---|
| `dead_grass_block` | 1 | `DeadGrassBlock` | kopie `grass_block`, `MapColor.WOOD`; `snowy`; loot → `withered_dirt` (Silk Touch → zichzelf); `#dirt`, `#mineable/shovel` |
| `withered_dirt` | 1 | `WitheredDirtBlock` | kopie `dirt`; `#dirt`, `#mineable/shovel` |
| `dead_log`, `dead_wood` | 1 | `StrippableLogBlock` | bijl → `stripped_dead_log` / `stripped_dead_wood`; `#corruptedadventure:dead_logs` |
| `stripped_dead_log`, `stripped_dead_wood` | 1 | `RotatedPillarBlock` | `#dead_logs`, `c:stripped_logs` / `c:stripped_woods` |
| `dead_planks` | 1 | `Block` | `#planks` |
| `dead_stairs`, `dead_slab`, `dead_fence`, `dead_fence_gate` | 1 | vanilla-classes | `#wooden_*` / `#fence_gates` (+ `c:fence_gates/wooden`); poort met `ModWoodTypes.DEAD` |
| `dead_door`, `dead_trapdoor`, `dead_pressure_plate`, `dead_button` | 1 | vanilla-classes | `BlockSetType` `corruptedadventure:dead`; knop 30 ticks; deur en valluik cutout; deur → `DoubleHighBlockItem` |

### Items
| Registry-naam | Ring | Class | Opmerking |
|---|---|---|---|
| alle blokken hierboven | 1 | `BlockItem` (deur: `DoubleHighBlockItem`) | via `ModBlocks`; geen losse items (fase 2) |

### Biomes
| Registry-naam | Ring | Dimensie | Opmerking |
|---|---|---|---|
| 52 kopieën: `plains`, `desert`, `forest`, … `deep_dark` (= vanilla-pad) | 1 | overworld | change 8; `VanillaBiomeCopies.copyOf(Biomes.X)`, geen constanten in `ModBiomes`. Geen `mushroom_fields` (→ `plains` via `replace`) |
| `placeholder_ring_2` … `placeholder_ring_5` | 2–5 | overworld | **tijdelijk** (change 5); verdwijnen als de echte biomes van die ring er zijn. Ring 4 met neerslag. `placeholder_ring_1` is weg sinds change 8 |
| `withered_expanse` | overgang 1→2 | overworld | change 11, `TransitionBiomes`; hele zone van grens 1 via `transitions`. Droog (2,0, geen neerslag), vanilla-monsters + konijnen; eigen surface rule in `overworld.json`. Change 13: lucht/fog/water = `Palette.RING_1` 40 % naar grijs, grotgeluid `STRANGE` |
| `void_edge` | void | overworld | blijvend; voorbij ring 5 (zwevende eilanden → void) |
| `placeholder_nether_ring_1` … `placeholder_nether_ring_5` | 1–5 | Nether | **tijdelijk** (change 5); fog-kleur per Nether-tegenpool |
| `nether_void_edge` | void | Nether | blijvend |

### Overige (biome sources, density functions, payloads, attachments, …)
| Naam | Type | Waar |
|---|---|---|
| `corruptedadventure:ring` | biome source (`RingBiomeSource`) | `ModBiomeSources`; gebruikt in `corrupted_world.json` (overworld + Nether) |
| `corruptedadventure:vanilla_climate` | biome source (`VanillaClimateBiomeSource`) | `ModBiomeSources`; ring 1 van de overworld in `corrupted_world.json` |
| `minecraft:*` biome-tags (gespiegeld) | tags (datagen) | `ModBiomeTagsProvider`; klasse-, gedrag- en `has_structure/*`-tags; **nooit** `is_overworld`, `has_structure/stronghold`, `stronghold_biased_to`, `has_structure/ancient_city` |
| `neoforge:villager_types` | data map (datagen) | `ModDataMapProvider` |
| `minecraft:pale`, `snowy`, `black`, `ashen`, `woods`, `chestnut` | wolf variant (datapack-override, datagen) | `VanillaBiomeCopies.bootstrapWolfVariants` |
| `corruptedadventure:ring_blend` | density function type (`RingBlend`) | `ModDensityFunctionTypes`; `ring_blend/overworld.json`, `ring_blend/nether.json` |
| `corruptedadventure:void_edge` | density function type (`VoidEdge`) | `ModDensityFunctionTypes`; `void/*.json` |
| `corruptedadventure:radial_falloff` | density function type (`RadialFalloff`) | `ModDensityFunctionTypes`; `ring_1/overworld/continents.json` |
| `corruptedadventure:main` | creative tab | `ModCreativeTabs`; titel `itemGroup.corruptedadventure.main` |
| `corruptedadventure:dead` | `BlockSetType` (+ niet-geregistreerde `WoodType`) | `ModWoodTypes` |
| `corruptedadventure:dead_logs` | blok- en item-tag | `ModTags`; zit in `minecraft:logs_that_burn` |
| `corruptedadventure:ring_chance` | placement modifier type (`RingChanceFilter`) | `ModPlacementModifierTypes`; signalen `transition`, `progress` |
| `corruptedadventure:bare_patch`, `unnatural_tunnel` | feature types (`BarePatchFeature`, `UnnaturalTunnelFeature`) | `ModFeatures` |
| `bare_patch`, `unnatural_tunnel` | configured + placed features (datagen) | `ModConfiguredFeatures`, `ModPlacedFeatures`, `Ring1HorrorFeatures`; testen met `/place feature corruptedadventure:…` (slaat de placement modifiers over) |
| `corruptedadventure:add_bare_patch`, `add_unnatural_tunnel` | biome modifiers (`neoforge:add_features`, datagen) | `Ring1BiomeModifiers` |
| `corruptedadventure:has_feature/bare_patch`, `has_feature/unnatural_tunnel` | biome-tags (datagen) | `ModTags.Biomes`, `ModBiomeTagsProvider`; niet in de Nether of `withered_expanse` |
| `dead_tree`, `dead_tree_forked`, `withered_trees`, `withered_vegetation`, `withered_sandstone_rock`, `withered_sand_patch` | configured features (datagen) | `ModConfiguredFeatures`, `WitheredExpanseFeatures`; `dead_tree` herbruikbaar (change 12) |
| `withered_expanse_trees`, `…_vegetation` (`VEGETAL_DECORATION`), `…_rocks`, `…_sand_patches` (`LOCAL_MODIFICATIONS`) | placed features (datagen) | `ModPlacedFeatures`, `WitheredExpanseFeatures`; alleen in `withered_expanse` |
| `corruptedadventure:ambient.ring_1.strange_cave` | sound event (variabel bereik) | `ModSoundEvents`; `mood_sound` van `withered_expanse`; `sounds.json` uit `ModSoundDefinitionsProvider` (vanilla `ambient.cave`, pitch 0,5–0,7, volume 0,8); testen met `/playsound corruptedadventure:ambient.ring_1.strange_cave ambient @s` |
| `corruptedadventure:ring_noise` | chunk generator (`RingChunkGenerator`) | `ModChunkGenerators`; gebruikt in `corrupted_world.json` (overworld + Nether) |

---

## Build en configuratie

| Bestand | Inhoud |
|---|---|
| `gradle.properties` | MC 1.21.1, NeoForge 21.1.252, parchment 2024.11.17, `mod_id`, `mod_group_id=nl.timtoys.corruptedadventure`, versie |
| `build.gradle` | ModDevGradle; runs: client, server, gameTestServer, data (output naar `src/generated/resources`); JUnit 5 via `neoForge.unitTest` (tests draaien in een FML-omgeving, Minecraft-classes bruikbaar) |
| `.github/workflows/build.yml` | CI-build uit de MDK |
| `run/config/corruptedadventure-common.toml` | (niet in git) COMMON-config, ontstaat bij de eerste start |
| `tools/textures/Ring1Textures.java` | texturescript ring 1 (single-file Java, alleen JDK, buiten Gradle en de jar): `java tools/textures/Ring1Textures.java` vanuit de repo-root. Eigen pixels, geen vanilla; vaste seed per bestandsnaam (twee keer draaien = byte-identiek). Per texture een tekenmethode, paletten bovenin, gedeelde helpers (ruis, palet) in hetzelfde bestand. Change 10 = eenvoudige werkversie; change 15 voegt het zielkristal toe, change 17 verfijnt |
| `tools/server-smoke-test.sh` | test S: start `runServer`, wacht op `Done (` of een fout, stopt de server-JVM; log in `build/runServer.log` |

## Databestanden (handgeschreven)

| Pad | Doel |
|---|---|
| `data/corruptedadventure/worldgen/world_preset/corrupted_world.json` | preset `corruptedadventure:corrupted_world`: overworld (vanilla type) en `minecraft:the_nether` met type `corrupted_nether`, beide met generator `corruptedadventure:ring_noise` (+ `dimension`), noise settings `corruptedadventure:overworld` / `corruptedadventure:nether` en biome-source `corruptedadventure:ring` (ring 1 overworld: `vanilla_climate` met preset `minecraft:overworld` en `replace` mushroom fields → `corruptedadventure:plains`; verder per ring een `minecraft:fixed` met de placeholder / void-rand; overworld `transitions`: `ring_1` → `fixed` `withered_expanse`, Nether geen); The End vanilla |
| `data/corruptedadventure/dimension_type/corrupted_nether.json` | kopie van vanilla `the_nether` met `coordinate_scale` 4 (portals ÷4) |
| `data/minecraft/tags/worldgen/world_preset/normal.json` | preset zichtbaar in "World Type" (niet de standaard) |
| `data/corruptedadventure/worldgen/noise_settings/overworld.json` / `nether.json` | kopie van vanilla (incl. surface rules, aquifers, ore veins); `final_density` → `ring_blend/<dim>`. Overworld daarnaast: `continents` → `ring_1/overworld/continents` (geboost), `depth` → `ring_1/overworld/depth`, `initial_density_without_jaggedness` → `ring_blend/overworld_initial`, `spawn_target` = één punt met alle klimaatparameters `[-2, 2]` (spawn altijd rond 0,0). Overworld-surface rules: alle `biome_is`-keys → `corruptedadventure:<pad>` (ring 1-kopieën), mycelium-tak (mushroom fields) weg. **Eerste element van `above_preliminary_surface`** = eigen tak voor `withered_expanse` (change 11: zandsteen op `steep`, grove/verdorde aarde via surface-ruis, dood gras, verdorde aarde eronder en onder water). Volgende eigen takken (ring 2: change 23) ook daar, als losse blokken bovenaan |
| `data/corruptedadventure/worldgen/density_function/ring_blend/{overworld,nether}.json` | `ring_blend` met per ring een verwijzing naar `ring_N/<dim>` en `void/<dim>` |
| `data/corruptedadventure/worldgen/density_function/ring_blend/overworld_initial.json` | `ring_blend` voor `initial_density_without_jaggedness`: ring 1 → `ring_1/overworld/initial_density`, ring 2–5 en void → `0.390625 + ring_N/overworld` |
| `data/corruptedadventure/worldgen/density_function/ring_1/overworld.json` | ring 1 overworld = vanilla `final_density` (inline), `sloped_cheese` → `ring_1/overworld/sloped_cheese` |
| `data/corruptedadventure/worldgen/density_function/ring_1/overworld/*.json` | kopie van de continentalness-afhankelijke vanilla-keten: `offset`, `factor`, `jaggedness`, `depth`, `sloped_cheese`, `initial_density` (vanilla's inline `initial_density_without_jaggedness`), onderling verwijzend naar `ring_1/overworld/…`; grotten, `erosion`, `ridges(_folded)` en noises blijven `minecraft:`. `continents` = `flat_cache(c + radial_falloff(300, 450) · max(0, 0,03 − c))` met `c` = vanilla-continentalness |
| `data/corruptedadventure/worldgen/density_function/ring_{2..5}/overworld.json`, `ring_{1..5}/nether.json` | **placeholder-terrein** (tijdelijk): `y_clamped_gradient` + 2D-ruis; hoogtes in `fase-1-wereld.md` §1.15 |
| `data/corruptedadventure/worldgen/density_function/void/{overworld,nether}.json` | zwevende eilanden in een hoogteband, uitdunnend met `void_edge`, leeg vanaf `void_edge` 0,8 |
| `data/corruptedadventure/worldgen/noise/placeholder_terrain.json`, `void_islands.json` | noise-parameters voor placeholder-terrein en void-eilanden |
| `META-INF/accesstransformer.cfg` | wordt automatisch opgepikt door ModDevGradle en FML (geen extra config nodig) |

Testseed voor test W: **`20260930`**. Test S: `run/server.properties` staat op `level-type=corruptedadventure:corrupted_world`, `level-seed=20260930`, `level-name=smoke-change13` (nieuwe wereldmap per test = andere `level-name`). Werelden van vóór change 8 laden niet meer (`placeholder_ring_1` bestaat niet meer); werelden van vóór change 9 krijgen naden tussen oud en nieuw ring 1-terrein.

---

## Netwerk en persistente staat

| Naam | Soort (payload / SavedData / attachment) | Richting / scope | Doel |
|---|---|---|---|
| `ring_settings.json` | bestand in de wereldmap (`<wereld>/corruptedadventure/`) | server | ringgrenzen bevroren per wereld (geen SavedData: moet er zijn vóór `loadLevel()`) |
| world border | vanilla (`level.dat`, alleen de overworld) + vanilla-border-packets | server → client | Nether-border wordt niet opgeslagen maar bij elke start afgeleid; Nether-spelers krijgen een eigen `ClientboundInitializeBorderPacket` |

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
- **Density functions:** `NoiseChunk` vult bij elke chunk *alle* `interpolated`-markers die via `mapAll` bereikbaar zijn, ook van ringen die ver weg liggen. Dure terreininvoer kost dus in elke chunk; meet dat bij elke zware nieuwe ringinvoer. **Meting change 9** (seed 20260930, JFR `minecraft.ChunkGeneration`, 16×16 chunks per ring, tweede (warme) ronde, ms per chunk, stap `noise` gemiddeld / mediaan / p95): vóór change 9 ring 1 34,9 / 31,5 / 49,4, ring 3 31,2 / 29,0 / 41,8; na change 9 ring 1 40,9 / 40,8 / 55,0, ring 3 31,0 / 29,6 / 50,2; vanilla-preset 35,1 / 33,7 / 50,5 en 33,5 / 30,2 / 63,5. Dus ring 1 = 1,17× vanilla, ring 3 0,92× (de vanilla-invoer is in ring 3 niet meetbaar: de noise-stap wordt gedomineerd door per-blok-interpolatie, aquifer en ore veins). Budget 1,3×; ring gate nog niet nodig (backlog, change 23). Werkwijze: via RCON `jfr start` → `forceload add` → `execute if loaded` pollen → `jfr stop` → `jfr print --json --events minecraft.ChunkGeneration run/debug/<bestand>.jfr`, filteren op `chunkPosX/Z`.
- **Density functions** worden gedecodeerd vóór `ServerAboutToStartEvent` (worldgen-registries laden eerst): `RingLayout.current()` nooit in een codec of constructor aanroepen, alleen in `compute`. `initial_density_without_jaggedness` wordt ongewrapt en vanaf meerdere threads aangeroepen: geen veranderlijke instantiestaat in density functions; cache via `RingColumnCache` (ThreadLocal).
- Vanilla vindt het "preliminary surface" (aquifers, `above_preliminary_surface`) waar `initial_density_without_jaggedness` > 0,390625 is; tel die drempel op bij een terrein met zijn oppervlak op dichtheid 0.
- Vanilla vult **alle** open ruimte onder `sea_level` met de standaardvloeistof (ook zonder aquifers), en de noise-aquifer zet daarnaast eigen lokale vloeistofniveaus (onder Y -10 vaak lava) zodra er een preliminary surface is, bijv. onder zwevende eilanden. Daarom `ring_noise` met eigen vloeistof-kiezer **én** aquifer-wrapper. De `lava`-router-noise helpt niet: de aquifer sampelt die op rastercoördinaten (x/64, y/40, z/64), niet op blokposities.
- Terreincheck zonder client: RCON aanzetten in `run/server.properties` (`enable-rcon`, `rcon.password`), dan `forceload add` + `execute if block …` per Y via een klein RCON-script. Vloeistof tellen per chunk: `fill <16×128×16> minecraft:air replace minecraft:lava` (het aantal staat in de feedback; `replace lava` door lava telt 0). Daarna RCON weer uitzetten.
- **World border:** vanilla kent in feite één border (overworld, in `level.dat`). Andere dimensies krijgen via `DelegateBorderChangeListener` de **grootte 1-op-1**; alleen het midden deelt `Level` door `coordinate_scale`. De client krijgt in elke dimensie de overworld-border (`PlayerList.sendLevelInfo`, `broadcastAll`), en `/worldborder get/set/center` werkt altijd op de overworld, ook uitgevoerd in de Nether. Test de Nether-border dus met `/ca border`. `loadLevel()` roept aan het eind `applySettings` aan: een border die je eerder zet (bijv. in `LevelEvent.Load`) wordt overschreven; zet hem in `ServerStartingEvent`. Stuur je zelf een border-packet naar een geschaalde dimensie, geef dan het **ongeschaalde** midden mee (de client deelt zelf).
- Border en commando's testen zonder client: RCON (zie hierboven) — `/ca` werkt via RCON (permissieniveau 4), bijv. `execute in minecraft:the_nether run ca border`; `stop` via RCON slaat netjes op.
- **Biome-keys in vanilla-data:** surface rules (`biome_is`), villager-types (`VillagerType.BY_BIOME`), wolf-varianten, advancements (*Adventuring Time*) en visloot werken op de biome-**key**. Eigen kopieën krijgen dat alleen via een eigen surface-rule-tak, de `villager_types`-data map, overschreven wolf-varianten enz. (advancement en visloot: backlog). Nieuwe ringbiomes (ring 2–5) hebben hun eigen surface-rule-takken nodig.
- `minecraft:is_overworld` bevat de kopieën bewust niet: vanilla's `has_structure/stronghold` = `#is_overworld`, dus lidmaatschap geeft strongholds (en daarmee een End-portal).
- Een tag-provider die naar eigen datapack-biomes verwijst moet ná `createDatapackRegistryObjects` via `event.createProvider((output, lookup) -> …)` gemaakt worden; anders kent zijn lookup de biomes niet en faalt de tagcontrole.
- **3D-biomes:** de biome hangt ook van Y af (klimaat-`depth`); sinds change 9 volgt `depth` in ring 1 het echte terrein. Op één x,z kan de biome per Y toch verschillen (bijv. river op de bodem, forest op Y 62). Biome checken via RCON: `execute if biome x y z <key>` op de juiste hoogte (`execute positioned x 0 z positioned over ocean_floor …`).
- NeoForge logt bij de start "ocelot … MONSTER mob category" voor `corruptedadventure:jungle` / `bamboo_jungle`: dat is vanilla's eigen jungle-spawnlijst, 1-op-1 gekopieerd; onschuldig.
- **Feature-volgorde:** `RingBiomeSource.possibleBiomes` is de unie van alle ringen. Eigen ringbiomes die vanilla-features per decoratiestap in een andere volgorde gebruiken dan de kopieën, geven vanilla's "Feature order cycle"-crash bij het laden. Houd per stap de vanilla-volgorde aan.
- `WolfVariant.wildTexture()` (en tame/angry) geeft het volledige pad `textures/…png`, de constructor wil het korte pad; bij het namaken van een variant terugrekenen.
- `/summon` mét NBT slaat `finalizeSpawn` over: een wolf krijgt dan altijd `pale`. Test biome-afhankelijke varianten met `/summon minecraft:wolf x y z` zonder NBT.
- **Vanilla worldgen-JSON** (density functions, noise settings) staat alleen in `build/moddev/artifacts/neoforge-21.1.252-client-extra-aka-minecraft-resources.jar`, niet in de merged- of sources-jar. De ring 1-keten (`ring_1/overworld/*`, `ring_1/overworld.json`) is daar eenmalig uit gekopieerd met alleen de verwijzingen naar `continents`, `offset`, `factor`, `jaggedness`, `depth`, `sloped_cheese` vervangen; bij een MC-upgrade opnieuw kopiëren en controleren (terugvervangen = vanilla).
- **Continentalness zit op drie plekken:** router-veld `continents` (klimaat → biomes en spawn-finder) én de splines `overworld/offset`, `factor`, `jaggedness` (terreinvorm, via `depth`/`sloped_cheese`). Wie continentalness aanpast, moet de hele keten meenemen; anders krijg je landbiomes op oceaanterrein. Overschrijf nooit `minecraft:overworld/…`: dat raakt alle vanilla-werelden.
- **Spawn:** vanilla zoekt het spawnpunt op het klimaat (`spawn_target` + afstandsboete, tot ~2.500 blokken ver). Met `spawn_target` = alle parameters `[-2, 2]` is de klimaat-fitness overal 0 en wint 0,0; daarna kiest vanilla's 11×11-chunkspiraal de eerste vaste plek zonder vloeistof (in chunk 0,0 vaak exact 0,0). Spawn op bladeren kan (vanilla). Gemeten over 10 seeds (change 9): altijd ring 1, ≤ 32 blokken van 0,0, nooit in water/lava.
- **Land rond spawn** (change 9): de opgetilde zone ligt op de ondergrens (continentalness 0,03) en is daar laag, rond zeeniveau. Naast rivieren komen ondiepe plassen (1–6 blokken) in bos-/vlaktebiomes voor: 15 van de 240 meetpunten (r = 100/200/300, 8 richtingen, 10 seeds), rivieren 28.
- RCON-commando's draaien op de world spawn (`RconConsoleSource` = `getSharedSpawnPos`). Spawnpositie uitlezen: `summon minecraft:marker ~ ~ ~ {Tags:["s"]}` + `data get entity @e[tag=s,limit=1] Pos`; hoogte van een kolom: `execute positioned x 0 z positioned over world_surface run summon …`.
- **Blokken toevoegen** (sjabloon change 10): blok in `ModBlocks` via `register` (blockitem komt vanzelf, deur via `registerDoor`); daarna blockstate/model in `ModBlockStateProvider`, itemmodel in `ModItemModelProvider`, loot in `ModBlockLootProvider` (anders faalt `runData`), tags, lang (en + nl), textures via het script. Houtsets: `StrippableLogBlock` + `BlockSetType`/`WoodType` in `ModWoodTypes` + vuurkansen in `ModFlammables`.
- **Textures vóór `runData`:** de model-providers controleren via `ExistingFileHelper` (`--existing src/main/resources`) of elke texture bestaat. Eerst het texturescript draaien. Cutout-textures (deur, valluik) alleen alpha 0 of 255.
- `WoodType.register` **niet** aanroepen zolang er geen bord-textures zijn (de client bouwt dan bord-materialen); `FenceGateBlock` gebruikt de `WoodType` alleen voor geluid. `BlockSetType.register` wel (de `BlockSetType`-codec zoekt op naam).
- `FireBlock#setFlammable` is in NeoForge al `public`: geen access transformer nodig. Aanroepen in `enqueueWork` (de map is niet thread-safe).
- Vanilla-tagketens dekken veel al: `#mineable/axe` bevat `#logs`, `#planks` en alle `#wooden_*`; NeoForge's `furnace_fuels`-data map werkt op `#logs`, `#planks`, `#wooden_*` en `#fence_gates`; `c:fences/wooden` bevat `#wooden_fences`. Alleen `c:stripped_logs`, `c:stripped_woods` en `c:fence_gates/wooden` noemen blokken los.
- **Random ticks testen:** in 1.21.1 krijgen chunks alleen random ticks als er een speler binnen ~128 blokken is; geforceloade chunks via RCON zonder speler ticken niet (ook vanilla-gras niet). Gedrag van dood gras dus in-game testen, niet via RCON. Tags en loot wel via RCON: `execute if block … #tag`, `loot insert <kist> mine <pos> <tool>`, `execute if items block … container.N #tag`.
- **Features tellen via RCON** met `fill … replace <blok>` verandert de wereld: vervang je de **grond** (bijv. dood gras → glas), dan vallen de planten erop meteen weg (blockupdate). Tel begroeiing (`short_grass`, `dead_bush`) dus eerst, of in verse chunks. Na `forceload add` staan de features er wel (decoratie heeft geen speler nodig, random ticks wel).
- **Boom zonder blad:** `minecraft:tree` met lucht als `foliage_provider` en `BlobFoliagePlacer(0, 0, 0)`. Vanilla's `TreeFeature` kijkt niet naar de grond; het placed feature moet zelf op `#dirt` eronder filteren (er is geen sapling voor `would_survive`).
- **Features schrijven alleen in de 3×3 chunks** rond de chunk van hun herkomst (vanilla `WorldGenRegion`, `blockStateWriteRadius` 1); daarbuiten wordt `setBlock` genegeerd met "Detected setBlock in a far chunk" in de log, en lezen daarbuiten gooit. Reken met de chunkgrenzen van de herkomst (zoals `TunnelPath.fits`), niet met ±16 rond de herkomst, en houd 1 blok marge als je buren leest.
- **Kale plekken en latere buurchunks:** `bare_patch` reikt tot ~12 blokken in de buurchunks. Een buur die zijn `VEGETAL_DECORATION` pas later draait, zet er alsnog vanilla-bomen, bloemen en gras op (dood gras zit in `#dirt`); een buur die al eerder decoreerde verdort wel. Bekende, geaccepteerde beperking (review change 12; opties in de backlog). Schrijven alleen in "al gedecoreerde" buren is geen oplossing: dat hangt af van de generatievolgorde en is dus niet deterministisch per seed.
- De `biome`-placement-filter kijkt naar de **3D-biome op de hoogte van de herkomst**: ondergrondse features (tunnels) moeten ook in de cave biomes (lush/dripstone caves, deep dark) staan, anders vallen ze daar weg.
- `/place feature` plaatst het **configured** feature en slaat alle placement modifiers (`ring_chance`, `biome`, `rarity`) over: test daarmee de vorm, niet de verdeling.
- **Biome modifiers** (NeoForge `add_features`) voegen features achteraan een stap toe; zolang elke feature in alle biomes op dezelfde plek staat, geen "Feature order cycle". Features voor de ring 1-kopieën gaan via modifiers (`Ring1BiomeModifiers`), niet in `VanillaBiomeCopies`.
- **Blokken tellen via RCON met `fill … replace`:** `fill` roept `updateFromNeighbourShapes` aan, dus "dood gras → dood gras[snowy=true]" telt 0 (de state wordt teruggezet). Tel met een ander doelblok (glas) of een echt andere blok-state (`dead_log` → `stripped_dead_log`). `forceload add` weigert gebieden van meer dan 256 chunks; `fill` max. 32.768 blokken (16×16×128).
- **`effectiveDistance` is langs een straal niet strikt monotoon:** de grensruis kan lokaal steiler zijn dan 1 blok per blok. Tests op "loopt op richting de rand" (`ringProgress`, `ring_chance`) hebben een kleine tolerantie nodig.
- **Konijnen** spawnen alleen op `#minecraft:rabbits_spawnable_on` (gras, sneeuw, zand), los van `#animals_spawnable_on`: in `withered_expanse` dus alleen op de zandvlekken, niet op dood gras.
- **Grotgeluid (`mood_sound`)** is vanilla client-code (`BiomeAmbientSoundsHandler`): elke tick één willekeurig blok binnen `block_search_extent` rond de ogen; zonder hemellicht telt het `(1 − bloklicht) / tick_delay` op (ook vaste rots telt mee), met hemellicht trekt het af. Bij 1 speelt het geluid. Daarom alleen ondergronds, fakkels vertragen het. Biome = die op de positie van de speler. Vanilla's builder kan een mood zetten maar niet wissen: zie `BiomeAmbience.withMood`.
- **`sounds.json` met type `event`** verwijst naar een ander sound event; pitch en volume vermenigvuldigen die van dat event. `SoundDefinitionsProvider` controleert zo'n verwijzing alleen tegen de sound-event-registry, niet tegen `.ogg`-bestanden. Eigen sound events moeten common geregistreerd zijn: een biome-JSON die ze noemt, laadt anders niet op de dedicated server.
