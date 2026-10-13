# Pakket 58 — golf 0 ring 4 (infra): ringgrenzen, overgang per grens, contractraamwerk

Scope: `infra` — pakket 58, in de hoofdwerkmap op `main` (`waves.md` §2 golf 0, §2b). Contract: `contract.md` §1, §2, §3, §8.

## Wat gedaan
1. **Standaard-ringgrenzen** (`CommonConfig`): 1.500 / 3.500 / 6.000 / 8.000 / 9.700, void-rand 10.200 (alleen nieuwe werelden; Nether ÷4).
2. **Migratie** bij `ModConfigEvent.Loading`: de pre-37c-set (5.500 / 7.500 / 9.500) en de 37c-set (6.000 / 7.800 / 9.500), beide met void 10.000, gaan — alleen als radii én void exact die set zijn — naar de nieuwe standaard + `SPEC.save()` + INFO-log; andere waarden blijven (WARN). Testbaar via `migrated(OuterBoundaries)`.
3. **Overgangsbreedte per grens:** config-sleutel `transitionWidthOverrides` (5 ints, 1|2, 2|3, 3|4, 4|5, 5|void; 0 = `transitionWidth`), standaard [0, 0, 0, 256, 0]. `RingSettings` kreeg het veld + `transitionWidth(boundary)`; snapshot-sleutel `transition_width_overrides` is optioneel bij het lezen (oude `ring_settings.json`: elke grens 128) en wordt altijd geschreven; `validate()` rekent per grens. `RingLayout` gebruikt per grens zijn eigen halve breedte (`positionAt`, `reaches`); de Nether volgt vanzelf (zone 4→5 = 64).
4. **Ring 3-code** (`VolcanicField.regions`, `VolcanoPlacement.place`, `Ring3LandformsTest`) leest `transitionWidth(1)` / `transitionWidth(2)`; bij de standaard bit-voor-bit gelijk.
5. **Payload:** `RingLayoutPayload` stuurt de 5 overrides mee (var-int-lijst); `ModNetwork.PROTOCOL_VERSION` 6 → 7 (de enige ophoging van golf 0).
6. **Contractraamwerk** (geen inhoud): ring 4-/Nether-4-keys in `ModBiomes`; tag-keys in `ModTags` (`Blocks`: `poison_sources`, `ring_4_stones`, `moonpetal_logs`, `withered_moonpetal_logs`; `Biomes`: `is_ring_4`, `is_nether_ring_4`, `ring_4_blighted`, `purifying`); lege lang-helpers `Ring4BlockNames`, `Ring4BiomeNames`, `NetherRing4BiomeNames`, `Ring4ConditionText`, `Ring4WeatherText`; lege bootstraps `Ring4Biomes`, `Ring4BiomeModifiers`, `Ring4Features`, `Ring4Carvers`, `Ring4BiomeTags`, de Nether-4-tegenhangers en `Ring4DamageTypes`, aangeroepen zoals ring 3.
7. `tools/test/py/perf.py`: terugvalwaarde ring 4 7.800 → 8.000 (wordt alleen gebruikt zonder config).

## Client/server-impact
Snapshot en config server-side; oude snapshots blijven geldig (zonder sleutel = elke grens 128). Het payload-formaat wijzigt (`PROTOCOL_VERSION` +1): oude clients worden geweigerd. Geen nieuwe client-class, geen `Minecraft.getInstance()` buiten `client/`.

## Tests
`./gradlew build` groen (733 tests), `./gradlew runData` groen. Nieuw/uitgebreid: `CommonConfigTest`, `RingSettingsTest` (nieuw: snapshot-codec, validatie), `RingLayoutTest` (zone 4|5 256/64, regressie oude formule), `RingLayoutPayloadTest` (rondreis overrides), `VolcanicFieldTest` (ring 3 gelijk met/zonder 4|5-override). Aangepast op de nieuwe standaard: `RingTargetsTest` (middens 7.000 / 8.850 / 9.950), `RingWorldBorderTest` (20.400), `RingBiomeSourceTest`, en de fixtures van de overige ringtests (void 10.200 + overrides). `voidEdgeProgress`-test staat nu kleine dips toe (zoals `ringProgress`): de grensruis kan lokaal steiler zijn dan de straal; op de nieuwe radius 9.700 trof de vaste test er één.

`src/generated`: 58 zelf levert geen wijziging (lege tags en lang-helpers schrijven niets). `runData` regenereert wel `assets/corruptedadventure/sounds.json` — achterstand van 8753843 (atmosphere), niet gecommit (backlog).

## CODEBASE-delta
`CommonConfig`, `RingSettings`, `RingLayout`, `RingLayoutPayload`, `ModNetwork` (`"7"`), `ModBiomes`, `ModTags`, lang-/tag-helpers en bootstrap-haken ring 4 (nieuwe sectie "Ring 4-haken per stroom (58)"), tests; waarden 8.000 / 9.700 / 10.200.

## Beslissingen (gebruiker, 2026-10-13; `02-beslissingenlog.md`)
- Overgangsbreedte per grens als config-lijst `transitionWidthOverrides`, standaard [0, 0, 0, 256, 0].
- Migratie van beide oude standaardsets (radii + void).
- Ring 3-code naar de per-grens-accessor.

Implementatiekeuzes (niet door de gebruiker besloten): snapshot-sleutel `transition_width_overrides` (snake_case zoals de andere snapshot-sleutels); de void-rand houdt in `validate()` de oude marge van twee keer de reach van 5|void (hij heeft geen eigen buitengrens).

## Backlog
- [atmosphere] `sounds.json` niet geregenereerd na 8753843; committen bij de integratie.
