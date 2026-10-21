# Pakket 81 — golf 0 ring 5 (infra): contract-raamwerk ring 5

Scope: `infra` — pakket 81, worktree `/home/user/wt/ring5-g0` op `ring5/golf0` (`waves.md` §2 golf 0, §2b; `golf-0.md`
§1). Contract: `contract.md` §2, §3, §8. Antwoorden: `golf-0.md` §7.

## Gedaan
1. **Biome-keys** (`ModBiomes`): `CORRUPTED_WASTES`, `GLITCH_FIELDS`, `FALSE_MEADOW` (binnenband), `VOID_SCARS`,
   `INVERTED_HILLS`, `SHATTERED_EXPANSE` (buitenband; naam blijft, V12-A), `WHITE_HALLS`, `HOLLOW_CATHEDRAL` (Nether-5).
   Alleen keys; `placeholder_ring_5`/`placeholder_nether_ring_5` blijven tot 86/87.
2. **Tag-keys** (`ModTags`): `Blocks.CORRUPTED`, `RING_5_STONES`, `VOID_SOURCES`, `NETHER_RING_5_STONES`;
   `Biomes.IS_RING_5`, `IS_NETHER_RING_5`, `RING_5_INNER`, `RING_5_OUTER`, `SILENT`. Inhoud volgt (B, W-biomes, W-nether).
3. **`registry/ModDataMaps`** (nieuw): data map-type `corruptedadventure:healthy_block` (`DataMapType<Block, Block>`,
   registry `minecraft:block`, codec `BuiltInRegistries.BLOCK.byNameCodec()`), gesynct (niet verplicht), geregistreerd via
   `RegisterDataMapTypesEvent` op de mod-bus (`CorruptedAdventure`). Geen inhoud (B, 88).
4. **Lang-helpers (leeg)**: `datagen.lang.Ring5BlockNames`, `Ring5BiomeNames`, `NetherRing5BiomeNames`,
   `Ring5ConditionText`, `Ring5WeatherText`, `Ring5SignText`; aangeroepen in `EnglishLanguageProvider` en
   `DutchLanguageProvider` (blok "Ring 5 and Nether ring 5").
5. **Lege bootstraps**, aangeroepen zoals ring 4: `worldgen.biome.Ring5Biomes`/`NetherRing5Biomes`,
   `Ring5BiomeModifiers`/`NetherRing5BiomeModifiers`, `worldgen.feature.Ring5Features`/`NetherRing5Features`
   (`configured`, `placed`), `worldgen.carver.Ring5Carvers`/`NetherRing5Carvers` (`DataGenerators`),
   `datagen.tags.Ring5BiomeTags`/`NetherRing5BiomeTags` (`ModBiomeTagsProvider`), `condition.Ring5DamageTypes`.
6. **Config:** geen wijziging (V2-A en V7-A houden de zonebreedtes; `transitionWidthOverrides` blijft [0, 0, 0, 256, 0]).

## Client/server-impact
Keys, tag-keys en een data map-type: common code, het type wordt door NeoForge gesynct (niet verplicht). Geen payload,
geen `PROTOCOL_VERSION`, geen client-class.

## Tests
- `./gradlew runData` groen; `src/generated` ongewijzigd (lege tags en helpers schrijven niets).
- `./gradlew build` groen: 1.120 tests, 0 fouten. Nieuw: `ModDataMapsTest` (id, registry, geregistreerd in
  `RegistryManager`, gesynct/niet verplicht, codec-rondreis `minecraft:grass_block`).

## CODEBASE-delta
- Nieuw: `registry/ModDataMaps` (type `healthy_block`, `register(modBus)` in `CorruptedAdventure`).
- `ModBiomes`: ring 5- en Nether-5-keys. `ModTags`: ring 5-blok- en biome-tags.
- Nieuwe sectie "Ring 5-haken per stroom (81)": B ◻ `Ring5BlockNames`, tags `corrupted`, `ring_5_stones`, `void_sources`,
  `nether_ring_5_stones`, data map `healthy_block` (inhoud); W-biomes ◻ `Ring5Biomes`, `Ring5BiomeModifiers`,
  `Ring5Features`, `Ring5Carvers`, `Ring5BiomeTags`, `Ring5BiomeNames`, `Ring5SignText`; W-nether ◻ `NetherRing5Biomes`,
  `NetherRing5BiomeModifiers`, `NetherRing5Features`, `NetherRing5Carvers`, `NetherRing5BiomeTags`,
  `NetherRing5BiomeNames`; C ◻ `Ring5DamageTypes`, `Ring5ConditionText`; A ◻ `Ring5WeatherText`.

## Beslissingen
Geen nieuwe gebruikersbeslissingen (zie `golf-0.md` §7). Implementatiekeuzes:
- Data map gesynct maar niet verplicht (`synced(codec, false)`): de client kent het type en de inhoud, een client zonder
  de map wordt niet geweigerd (de mod zelf is toch verplicht).
- Het type zit in `ModDataMaps` en niet in de bestaande `ModDataMapProvider` (die is datagen); de inhoud komt in 88 in
  `ModDataMapProvider`.

## Backlog
Geen.
