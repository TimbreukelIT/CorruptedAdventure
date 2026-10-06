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
2. **Biome-keys** in `ModBiomes` (alleen `ResourceKey`-constanten, geen bootstrap): `ASHEN_WASTES`, `VOLCANIC_PEAKS`, `EMBER_WOODS`, `BASALT_FLOWS`, `SULFUR_FIELDS`, `DRIED_SEABED`, `TITAN_FIELDS`, `DORMANT_CALDERA` (*alleen als vraag V8 = A of B*), `STEAM_FENS`, `FROZEN_LAVA_SEA`, `RIME_FOREST`, `GLACIER_RIFTS`. Een key zonder biome in de registry is geen fout zolang niemand hem in data gebruikt.
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
| (bouwdatum) | infra | Standaard-ringradii 1.500 / 3.500 / 6.000 / 7.800 / 9.500 in `CommonConfig` | Uitvoering sparring ring 3 (antwoord 8) |
| (bouwdatum) | infra | Lang- en bootstrap-raamwerk per stroom voor parallel werken | Geen gedeelde provider-edits tussen stromen |

Commit: `infra: prepare ring 3 contract keys and wider ring 3 defaults`.
