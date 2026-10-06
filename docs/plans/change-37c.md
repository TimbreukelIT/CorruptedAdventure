# Change 37c — Ring 3 fundament: ringradii, contract-sleutels en lang-raamwerk

Scope: `infra` — werkpakket 37c (golf 0, sequentieel, in de hoofdwerkmap; vóór 39a)

**Doel:** de ring 3-golven parallel kunnen laten starten zonder dat stromen elkaars gedeelde bestanden aanraken. Daarvoor drie dingen:
1. de nieuwe standaard-ringradii (sparring ring 3, antwoord 8);
2. alle sleutels uit `docs/plans/ring-3/contract.md` als Java-constanten;
3. een lang-raamwerk met één helper per stroom.

Geen gedrag, geen nieuwe blokken of biomes, geen worldgen-data.

Bron: `docs/plans/ring-3/contract.md` §1, §2, §3 (tags), §8; `docs/plans/ring-3/waves.md` §2 golf 0, §5.

## Plan
1. **Ringradii** in `config/CommonConfig.java`: `defaultRadii` → `{1500, 3500, 6000, 7800, 9500}`. Geldt alleen voor nieuwe werelden (snapshot `ring_settings.json`). Bestaande testwerelden houden hun waarden.
   - Unit tests: `RingSettings`-validatie met de nieuwe waarden (boundaryReach 160 past ruim).
   - `RingTargets.middleRadius` geeft 4.750/6.900/8.650.
   - Tests die vaste coördinaten van ring 3–5 gebruiken, nalopen.
   - `fase-1-wereld.md` §1.13 is al bijgewerkt.
2. **Biome-keys** in `ModBiomes` (alleen `ResourceKey`-constanten, geen bootstrap): `ASHEN_WASTES`, `VOLCANIC_PEAKS`, `EMBER_WOODS`, `BASALT_FLOWS`, `SULFUR_FIELDS`, `DRIED_SEABED`, `TITAN_FIELDS`, `DORMANT_CALDERA` (V8 = A), `STEAM_FENS`, `FROZEN_LAVA_SEA`, `RIME_FOREST`, `GLACIER_RIFTS`. Een key zonder biome in de registry is geen fout zolang niemand hem in data gebruikt.
3. **TagKeys** in `ModTags`:
   - blok: `heat_sources`, `warmth_sources`, `ring_3_stones`, `charred_logs`, `rimeshroom_stems`;
   - item: `charred_logs`, `rimeshroom_stems`;
   - biome: `is_ring_3`, `is_nether_ring_3`, `heat_peaks`, `heat_refuges`;
   - `c:`-ertstags zoals ring 2.
   - Inhoud: niet hier (B en de worldgen-stromen via datagen).
4. **Lang-raamwerk:**
   - pakket `datagen/lang/` met per stroom een helper (`Ring3BlockNames`, `Ring3BiomeNames`, `Ring3ConditionText`, `Ring3WeatherText`), elk met `addEnglish(…)` / `addDutch(…)` en voorlopig leeg;
   - `EnglishLanguageProvider` en `DutchLanguageProvider` roepen ze aan;
   - de provider-hulpmethodes die de helpers nodig hebben (`add`, `addBlock`, `biome`, …) maken we bereikbaar via een smal interface. Implementatiedetail; geen "Utils"-klasse.
5. **Bootstrap-haken:** in `DataGenerators` lege bootstrap-klassen per stroom klaarzetten en aanroepen (bijv. `Ring3Biomes.bootstrap`, `NetherRing3Biomes.bootstrap`, `Ring3Features.bootstrap`, `NetherRing3Features.bootstrap`), zodat stromen alleen de eigen klasse vullen. Exacte namen in het rapport; het contract volgt.

## Client/server-impact
Geen. Alleen constanten, config-standaardwaarden en datagen-raamwerk.

## Tests
- **B** (inclusief unit tests en `ClientIsolationTest`).
- **D**: `runData`; de diff in `src/generated` moet leeg zijn (lege helpers, lege bootstraps).
- **S**: verse wereld `smoke-change37c`; `/ca tpring 3` landt op ~4.750; `/ca ring` op 5.800 zegt ring 3.

## Beslissingenlog-regels
| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-06 | infra | Standaard-ringradii 1.500 / 3.500 / 6.000 / 7.800 / 9.500 in `CommonConfig` | Uitvoering sparring ring 3 (antwoord 8) |
| 2026-10-06 | infra | Lang- en bootstrap-raamwerk per stroom voor parallel werken | Geen gedeelde provider-edits tussen stromen |

Commit: `infra: prepare ring 3 contract keys and wider ring 3 defaults`.

## Uitvoering (2026-10-06)

**Gebouwd**
- `CommonConfig`: `DEFAULT_OUTER_RADII` = 1500/3500/6000/7800/9500 (ook de testfixture); migratie bij het laden (`register`, `ModConfigEvent.Loading`): precies de oude standaard 5500/7500 → nieuwe standaard, opgeslagen, INFO-log; andere afwijkende waarden blijven staan met een WARN.
- `ModBiomes`: 9 overworld-keys (7 sub-biomes + `dormant_caldera` + `steam_fens`) en 3 Nether-keys; geen biomes in de registry.
- `ModTags`: blok `heat_sources`, `warmth_sources`, `ring_3_stones`, `charred_logs`, `rimeshroom_stems`, `c:ores/volcanite|sulfur|fire_ruby`; item `charred_logs`, `rimeshroom_stems`, dezelfde `c:ores/*`, `c:raw_materials/volcanite`, `c:dusts/sulfur`, `c:gems/fire_ruby`, `c:gems/fire_crystal`; biome `is_ring_3`, `is_nether_ring_3`, `heat_peaks`, `heat_refuges`.
- `datagen/lang/`: `LangOutput` (smal interface: `add`, `addBlock`, `addItem`, default `biome`), helpers `Ring3BlockNames` (B), `Ring3BiomeNames` (W-biomes), `NetherRing3BiomeNames` (W-nether), `Ring3ConditionText` (C), `Ring3WeatherText` (A), elk `addEnglish`/`addDutch`. Beide providers implementeren `LangOutput` (hun private `biome` is vervangen door de default) en roepen alle helpers aan.
- `datagen/tags/`: `Ring3BiomeTags` (W-biomes), `NetherRing3BiomeTags` (W-nether), `addTags(Function<TagKey<Biome>, TagAppender<Biome>>)`; `ModBiomeTagsProvider` roept ze aan met `this::tag`.
- Lege instap-bootstraps, aangeroepen in `DataGenerators`: `worldgen.feature.Ring3Features`/`NetherRing3Features` (`configured`, `placed`), `worldgen.carver.Ring3Carvers`/`NetherRing3Carvers`, `worldgen.biome.Ring3Biomes`/`NetherRing3Biomes`, `worldgen.biome.Ring3BiomeModifiers`/`NetherRing3BiomeModifiers`, `condition.Ring3DamageTypes`; plus `datagen.tags.Ring3DamageTypeTags` (via `ModDamageTypeTagsProvider`).
- Tests: `CommonConfigTest` (nieuw: standaard geldig, alleen de oude standaard migreert); 9 ringtests gebruiken `CommonConfig.DEFAULT_OUTER_RADII` als fixture (o.a. `RingTargetsTest` 4.750/6.900/8.650, `RingBiomeSourceTest`-middens, `RingLayoutTest` grens 6.000).

**Afwijkingen van het plan (implementatiedetails)**
- `Ring3BiomeNames` is gesplitst: W-nether krijgt een eigen `NetherRing3BiomeNames`, zodat twee stromen nooit hetzelfde bestand raken (contract §8 noemde één helper voor beide).
- Extra haken die het plan niet noemde, om `DataGenerators` en `ModBiomeTagsProvider` stroomvrij te houden: biome-tag-helpers (`datagen/tags/`), carver- en biome-modifier-instappen.
- Materiaal-itemtags naar het ring 2-patroon (`c:raw_materials/volcanite`, `c:dusts/sulfur` zoals zout, `c:gems/fire_ruby`); B mag ze in zijn eigen pakket aanpassen.
- Categorie: 37c is `infra`, maar zet ook worldgen-/blocks-keys en lege haken neer (geen inhoud). Bewuste uitzondering (contractraamwerk golf 0; splitsen brengt de conflicten terug); vastgelegd in `waves.md` §2b en de beslissingenlog.

**Review (/code-review medium, 5 bevindingen, alle afgehandeld)**
1. Oude config houdt 5.500/7.500 → migratie in `CommonConfig` + test (zie boven).
2. Haken voor damage types (C) en Nether-biome-modifiers (W-nether) ontbraken → `Ring3DamageTypes`, `Ring3DamageTypeTags`, `NetherRing3BiomeModifiers`.
3. Categorie-uitzondering vastgelegd (zie Afwijkingen).
4. `defaultRingSettings()`/`Function`-refactor vervangen door de constante `DEFAULT_OUTER_RADII`, ook in de tests.
5. Lege klassen blijven (contract); per klasse eigenaar in de javadoc, overzicht "Ring 3-haken per stroom" in `CODEBASE.md`.

**Tests**
- B: `./gradlew build` groen (79 testklassen, incl. `ClientIsolationTest`, `CommonConfigTest`).
- D: `runData` groen; diff in `src/generated` leeg.
- S: `tools/server-smoke-test.sh`, verse wereld `smoke-change37c` (seed 20260930): start zonder fouten; `ring_settings.json` = 1500/3500/6000/7800/9500.
- RCON (`/ca ring` via `execute positioned`): r 3.000 → ring 2 (grens 2 op −458, gelijk aan vóór); r 4.750 → ring 3; r 5.800 → ring 3 (grens 3 op −244, vóór: ring 4); r 6.900 → ring 4; r 8.650 → ring 5 (gelijk). `/ca tpring` vraagt een speler (kan niet via RCON); het doel komt uit `RingTargets.middleRadius` op de snapshot: unit-getest 4.750/6.900/8.650.
- Determinisme ring 1/2: wereld `smoke-37c-base` (HEAD, snapshot 5.500/7.500) tegen `smoke-change37c`; per chunk tellingen van 25 blokken (overworld, Y −64..191) in 8 chunks (ring 1, ring 2, zone grens 2) en 10 blokken in 4 Nether-chunks (ring 1/2): **240/240 identiek** (362.567 blokken). Een eerste ronde gaf alleen verschil in grind/water: meetfout (vallend grind en stromend water na het weghalen van de rest); in de tweede ronde worden die eerst geteld.
- Na de reviewfixes opnieuw B (groen), D (diff leeg), S: config met 5.500/7.500 → log "Moved the ring radii…", config op 6.000/7.800, nieuwe wereld `smoke-change37c-migrate` krijgt 6.000/7.800; config met ring 4 = 7.600 → WARN, waarde blijft (`smoke-change37c-custom`).
- Bestaande werelden: `smoke-37c-base` laadt met de nieuwe code met zijn eigen snapshot (r 5.800 → ring 4, zoals vóór).

## W-checklist (kort; in-game, bij teststop 4a)
- [ ] Verse wereld (seed 20260930): `/ca tpring 3` landt op ~4.750, `/ca tpring 4` ~6.900, `/ca tpring 5` ~8.650; `/ca ring` op r 5.800 zegt ring 3.
- [ ] `/ca tpboundary 3` landt op ~6.000 (±96), `/ca tpboundary 4` op ~7.800; `tpring 3 nether` op ~1.187.
- [ ] Ring 1/2 zien er hetzelfde uit als vóór 37c (zelfde seed).
