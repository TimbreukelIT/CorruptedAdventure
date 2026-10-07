# Golf 1 — stroom A (sfeer): 45 ring 3-weer

Scope: `atmosphere` — werkpakket 45; branch `ring3/atmosphere-g1`. (50 en 56, sfeer en geluid, volgen in golf 2.)

**Doel:** de drie weertypes van ring 3 (antwoorden 10 en 11), gameplay server-side en beeld client-side:
- **asregen:** zicht omlaag, `ash_flake`-particles, hitte ×1,5;
- **uitbarsting:** rode lucht, gerommel, lavabommen, hitte ×2;
- **aswolk** (nieuw `ASH_CLOUD`): donker, hitte ×3, schuilen; zeldzaam (V6 = A).

Bron: `fase-1-wereld.md` §1.6 (weermodel, client-hook), Ring 3 "Weer" + sparring-aanvulling; `contract.md` §5, §6; sjabloon change 28 (`SandstormWind`, `HeatWaveDehydrationModifier`, `client/weather/SandstormRenderer`, `HeatWaveRenderer`, `HeatHazeEffect`, `FogBlend`, `SkyExposure`, `RingWeatherRenderers`).

## Werk
1. `RingWeatherType.ASH_CLOUD` (ring 3, overworld); `[weather.ring3]` met gewichten asregen 45 / uitbarsting 40 / aswolk 15 (V6 = A), duur (aswolk 120–240 s) en hitte-factoren.
   - Een waarschuwing vooraf (aswolk ~20 s gerommel/verdonkering, V6 = A) via de bestaande fade-in (`intensity`), zonder extra payload.
2. **Hitte-factoren:** alleen de config-waarden (`ashRainHeatMultiplier` 1,5, `eruptionHeatMultiplier` 2,0, `ashCloudHeatMultiplier` 3,0) en een publieke getter. De modifiers zelf bouwt C (`contract.md` §5), zodat A en C in golf 1 niet op elkaar wachten.
3. **Lavabommen (server):** tijdens een uitbarsting vallen vanilla `SmallFireball`s op willekeurige plekken rond spelers in ring 3 onder open lucht, met een hogere dichtheid in `#heat_peaks`.
   - Vuur volgt `mobGriefing`; geen explosie.
   - Hoeveelheid uit de config, met een cap per speler per minuut.
   - Alle clients zien dezelfde entities.
4. **Client** (`client/weather/`):
   - `AshRainRenderer`: fog dichter en grijs, `ash_flake`-particles boven de heightmap, geluid `weather.ring_3.ash_rain`;
   - `EruptionRenderer`: rode lucht en fog, gerommel, lichte camera-shake;
   - `AshCloudRenderer`: zeer donker, fog naar ~16, vonken.
   - Alles × sterkte × blootstelling (`SkyExposure`).
   - **Lucht- en neerslagkleur:** NeoForge 1.21.1 heeft hier geen event. Kies een client-aanpak (mixin op `ClientLevel#getSkyColor` of een `DimensionSpecialEffects`-wrapper) en leg het vast in de delta; dit is de eerste weer-change die het nodig heeft (§1.6).
5. **Particles** `ash_flake`, `ember` registreren (`ModParticleTypes` + provider); sprites van T (tot dan tijdelijk een bestaande sprite, niet gecommit).
6. **Sound events** `weather.ring_3.*` registreren. Audio: vanilla-verwijzingen in golf 1 (zoals de zandstorm-wind); eigen synthese in golf 2 (`Ring3Sounds.java`).

## Afhankelijk van open vragen
- Geen meer: V6 = A (2026-10-06), verwerkt hierboven.

## Client/server-impact
- **Server:** staat, cyclus, modifiers, bommen; geen nieuwe payloads (de ringweerstaat wordt al volledig gesynct).
- **Client:** renderers, fog/lucht, particles, geluid, camera-shake alleen in `client/` (`ClientIsolationTest`).
- Twee clients moeten dezelfde staat en dezelfde bommen zien; particles en shake mogen per client verschillen.

## Tests
- **B**, **D**, **S** met 2 clients:
  - `/ca weather 3 ash_rain|eruption|ash_cloud`;
  - bommen zichtbaar op beide clients;
  - `mobGriefing false` geeft geen vuur;
  - onder een dak geen effect;
  - de grens 2→3 en 3→4 mengt;
  - deeltjes-instelling Minimal;
  - FPS.
- **W-checklist** voor 4b.

## Bij afronding
`waves.md` §4.

## Uitvoering (implementatiekeuzes van A)
- **Luchtkleur: mixin** op `ClientLevel#getSkyColor` (`@Inject` RETURN), het eerste en enige mixin van de mod: `corruptedadventure.mixins.json` (alleen `"client"`, dus nooit op een dedicated server), `[[mixins]]` in `neoforge.mods.toml`. De mixin roept `RingWeatherClientHooks.skyColor` aan, die per gevoeld weer `RingWeatherRenderer#skyColor` toepast (nieuwe default-methode). Reden: een `DimensionSpecialEffects`-wrapper kan de luchtkleur niet zetten, alleen de hele lucht zelf tekenen (`renderSky`); de mixin is één regel op een stabiele methode en vanilla's fogkleur volgt vanzelf. Neerslagkleur is niet nodig: as valt als particles.
- **Waarschuwing aswolk:** `RingWeatherType` kreeg `fadeTicks()` (standaard 600 = 30 s, `ASH_CLOUD` 400 = 20 s); `RingWeatherState.intensity()` blijft de enige formule (server en client). De opbouw (gerommel, verdonkering, hitte-factor via C) is de waarschuwing.
- **Duur aswolk:** `RingWeatherSettings` kreeg `durations` (eigen bereik per type) + `weatherRange(type)`; `[weather.ring3] ashCloudMinSeconds`/`ashCloudMaxSeconds` (120/240), min > max → standaard (met fout-log, zoals de andere paren).
- **Lavabommen:** accumulator per speler (elke seconde `lavaBombsPerMinute`/60 × intensiteit × `lavaBombPeakFactor` in `#heat_peaks`), dus nooit meer dan `lavaBombsPerMinute × lavaBombPeakFactor` per speler per minuut. Landingsplek uniform in de schijf van `lavaBombRadius`, alleen in ring 3 en geladen; start 40–60 boven de grond, tot 16 opzij, recht naar de plek. Entity-tag `corruptedadventure.lava_bomb`; `ProjectileImpactEvent` op een blok: lava- en rookparticles voor iedereen, bij `mobGriefing false` geannuleerd en weg (vanilla zet zonder eigenaar altijd vuur). Een geraakte entity krijgt vanilla-schade en brand. Spectators en `FakePlayer`s krijgen geen bommen; creative wel (zichtbaar testen).
- **Gerommel + camera-shake** (`Rumble`): one-shots `weather.ring_3.eruption` om de 100–300 ticks (aswolk 80–200), elk met een korte schok (≈ 2 s uitsterven) × "Screen Effects"; de eerste komt snel. Binnen 60 % volume.
- **Ring 2-refactor:** `SandstormSound` → `WeatherLoopSound` (event als parameter) + `WeatherLoop` (start/herstart/feed); `SandstormRenderer` gebruikt die. Geen gedragsverandering.
- **Geluid golf 1 (vanilla-verwijzingen):** asregen = `ambient.basalt_deltas.loop`; uitbarsting = `entity.lightning_bolt.thunder` pitch 0,5/0,6; aswolk = `item.elytra.flying` pitch 0,45.

## Review (`/code-review medium`, 10 bevindingen)
- Opgelost: rommel-waarschuwing bijna onhoorbaar (vroeg in de fade) → `Rumble` speelt minstens op 0,5 (`MIN_LOUDNESS`); aswolk 's nachts lichter dan de nachtlucht → `MIN_LIGHT` 0,3 i.p.v. 1; pitch 0,45 wordt door de engine op 0,5 geklemd → 0,5; `skyColor` dupliceerde de lus → `forEachFelt(level, …)`; `SkyExposure` alleen via `registerRing2` geregistreerd → naar `register()`; `WeatherLoop` wachtte ook na een normaal uitgefade loop 5 s → alleen na een mislukte start.
- Bewust niet: `runData`-output ontbrak (draaide nog, zie D); hitte-factoren nog door niets gelezen (C koppelt ze, contract §5); CODEBASE.md niet bijgewerkt (waves.md §4.8: delta hieronder); bommen stapelen bij spelers die dicht bij elkaar staan (plan: cap per speler; zie Backlog).

## W-checklist (test S, voor 4b)
- `/ca weather 3 ash_rain` → beide clients: grijze fog (~48), grijze lucht, vallende as, knisperen; onder een dak binnen ~2 s weg.
- `/ca weather 3 eruption` → rode lucht (ook 's nachts gloed), roodbruine waas, vonken, gerommel met lichte schok; lavabommen vallen rond beide spelers en beide zien dezelfde (zelfde plek, zelfde vuur).
- `/gamerule mobGriefing false` → bommen landen met lava-spat maar zonder vuur; `true` → vuur.
- `/ca weather 3 ash_cloud` (zonder seconden: 120–240 s) → 20 s opbouw (gerommel, verdonkering), daarna bijna zwart (fog ~16), vonken + as, gebrul; onder een dak licht.
- Grens 2→3 en 3→4: de weer-renderers mengen over de zone (smoothstep); bommen alleen aan de ring 3-kant.
- Video-instelling Particles: Minimal → geen as/vonken; fog en lucht blijven.
- F3 `CA weather (client)` gelijk op beide clients.

## CODEBASE-delta
- **Status/kop:** pakket 45 (golf 1 ring 3, `atmosphere`): ringweer ring 3 compleet (asregen, uitbarsting met lavabommen, nieuw type aswolk), eerste mixin (luchtkleur).
- **Mappenstructuur `weather/`:**
  - `RingWeatherType.java`: + `ASH_CLOUD` (ring 3, overworld; tussen `ERUPTION` en `DENSE_FOG`) en `fadeTicks()` (standaard 600; `ASH_CLOUD` 400 = de waarschuwing van 20 s). **Index-codering verschuift → `ModNetwork.PROTOCOL_VERSION` verhogen bij de integratie** (geen nieuwe payload).
  - `RingWeatherState.java`: `intensity()` gebruikt `type.fadeTicks()`.
  - `RingWeatherSettings.java`: + `durations` (eigen duurbereik per type), `Range(min, max)`, `weatherRange(type)`; `ofSeconds(…, durationSeconds)`.
  - `RingWeatherCycle.java`: `start` trekt de duur uit `weatherRange(type)`.
  - `LavaBombs.java` ✅ server: lavabommen van een uitbarsting (`PlayerTickEvent.Post` elke 20 ticks, accumulator per speler, `SmallFireball` met tag `corruptedadventure.lava_bomb`, `ProjectileImpactEvent`: lava-/rookparticles, `mobGriefing false` → geannuleerd en weg). Geregistreerd in `RingWeather.register`.
- **`config/ServerConfig.java`:** `RING_3_WEATHER` (`Ring3WeatherEffects`, `[weather.ring3]`): `ashCloudMinSeconds`/`ashCloudMaxSeconds` 120/240, `ashRainHeatMultiplier` 1,5, `eruptionHeatMultiplier` 2,0, `ashCloudHeatMultiplier` 3,0, `heatMultiplier(type)` (voor C), `lavaBombsPerMinute` 12, `lavaBombRadius` 40, `lavaBombPeakFactor` 2,0; standaardgewichten ring 3 `ashRainWeight` 45 / `eruptionWeight` 40 / `ashCloudWeight` 15 (`RingWeather.defaultWeight`).
- **`client/`:**
  - `mixin/ClientLevelSkyColorMixin.java` ✅ de enige mixin: `ClientLevel#getSkyColor` RETURN → `RingWeatherClientHooks.skyColor`.
  - `particle/AshFlakeParticle.java` ✅ asvlok (langzaam vallend, zwaaiend, asgrijs, weg bij landen); `particle/EmberParticle.java` ✅ vonk (fullbright, geel-oranje → donkerrood, krimpt en vervaagt).
  - `weather/RingWeatherRenderer.java`: + `skyColor(level, colour, partialTick, strength)` en `cameraAngles(event, strength)`.
  - `weather/RingWeatherClientHooks.java`: + `registerRing3()`, `skyColor(…)` (voor de mixin), `ComputeCameraAngles`-listener; `SkyExposure` nu in `register()` (gedeeld).
  - `weather/AshRainRenderer.java` ✅ fog → 48 (terrein + lucht) `#8A8580`, ~30 `ash_flake`/tick, knisperloop; × sterkte × blootstelling.
  - `weather/EruptionRenderer.java` ✅ lucht 85 % naar `#B3361A` (gloed ≥ 0,45 's nachts, alleen sterkte), terreinfog × 0,6 naar `#7A2A14`, ~8 `ember`/tick, `Rumble` (100–300 ticks, schok 0,8°).
  - `weather/AshCloudRenderer.java` ✅ fog → 16 `#241F1C` (daglicht ≥ 0,3), lucht 95 % donker, vonken + as die dwarrelen, brulloop, `Rumble` (80–200 ticks, 0,6°).
  - `weather/RingWeatherFog.java` ✅ gedeelde fog-/fogkleur-/luchtkleurstappen ring 3 (op `FogBlend`).
  - `weather/WeatherParticles.java` ✅ particles in open lucht rond de camera (boven `MOTION_BLOCKING`).
  - `weather/Rumble.java` ✅ gerommel-one-shots + camera-schok (× "Screen Effects"), per client.
  - `weather/WeatherLoop.java` ✅ start/herstart/feed van een weerloop; `weather/WeatherLoopSound.java` (was `SandstormSound`, event als parameter).
- **Registries:** `ModParticleTypes` + `ash_flake`, `ember`; `ModSoundEvents` + `weather.ring_3.ash_rain`, `weather.ring_3.eruption`, `weather.ring_3.ash_cloud`.
- **Datagen:** `ModSoundDefinitionsProvider` (vanilla-verwijzingen, zie Uitvoering), `ModParticleDescriptionProvider` (`ash_flake_0..3`, `ember_0..3`, sprites van T), `Ring3WeatherText` (ondertitels en/nl).
- **Databestanden:** `src/main/resources/corruptedadventure.mixins.json`; `neoforge.mods.toml` `[[mixins]]`.
- **Tests:** `LavaBombsTest` (rate, accumulator-cap, meerdere per seconde); `RingWeatherConfigTest` (+ ring 3-gewichten, `[weather.ring3]`-paden en standaarden, eigen duur aswolk); `RingWeatherCycleTest` (+ eigen bereik, aswolk zeldzaam; alle typen van ring 3 komen voor); `RingWeatherStateTest` (+ fade 20 s aswolk).
- **Valkuil:** `ParticleDescriptionProvider` eist de PNG's bij `runData`; zonder T's sprites tijdelijke kopieën neerleggen (niet committen). SoundEngine klemt pitch op [0,5; 2].

## Beslissingen
- 2026-10-06 | atmosphere | Luchtkleur van het ringweer via één client-mixin op `ClientLevel#getSkyColor` (renderers krijgen `skyColor`); geen `DimensionSpecialEffects`-wrapper | NeoForge 1.21.1 heeft geen event; een wrapper kan de kleur niet zetten, alleen de hele lucht tekenen.
- 2026-10-06 | atmosphere | Waarschuwing aswolk = eigen fade-in van 20 s per type (`RingWeatherType#fadeTicks`), geen extra payload | V6: ~20 s gerommel en verdonkering; één `intensity()`-formule op server en client.
- 2026-10-06 | atmosphere | Lavabommen: accumulator per speler (`lavaBombsPerMinute` × fade × piekfactor), landing in ring 3 binnen `lavaBombRadius`, vuur alleen als `mobGriefing` aan staat | Plan 45: cap per speler per minuut, vuur volgt `mobGriefing`.

## Backlog
- [atmosphere] Lavabommen stapelen als spelers dicht bij elkaar staan (elke speler zijn eigen rate, 4 spelers = 4× de dichtheid). Eventueel delen door het aantal spelers binnen `lavaBombRadius`; afstemmen in teststop 4b.
- [atmosphere] Eigen geluiden ring 3-weer (asregen, gerommel, gebrul) in golf 2 (`Ring3Sounds.java`); nu vanilla-verwijzingen.

## Voortgang
- Voortgang: 20:25 branch `ring3/atmosphere-g1` op 8e0c637 (de worktree stond op 17c9eb4; reset op de verse branch), main gemeld.
- Voortgang: 20:53 code af (ASH_CLOUD, `[weather.ring3]`, lavabommen, mixin luchtkleur, drie renderers, particles, geluid, unit tests); wacht op een JVM-slot voor B.
- Voortgang: 21:10 B groen (428 tests); review gestart (10 bevindingen, 6 opgelost, zie Review).
- Voortgang: 21:57 D groen (sounds, particles, lang), B opnieuw groen na de reviewfixes; start S.
- Voortgang: 22:04 S deels: dedicated server (poort 25575) start zonder client-class-fouten (mixin-config alleen `"client"`, server laadt hem niet), `/ca weather` toont ring 3; de twee clients kregen pas om 22:05/22:06 een slot en waren nog niet gejoind toen de afrondopdracht kwam (22:14). Alles gestopt.

## Status bij onderbreking (2026-10-06 22:15)
- **Gecommit:** pakket 45 als één commit `atmosphere: add ring 3 weather …` op `ring3/atmosphere-g1` (hash in het eindrapport aan main). Gereviewd (`/code-review medium`, bevindingen hierboven), B en D groen.
- **Niet gecommit (bewust):** de tijdelijke particle-sprites `textures/particle/ash_flake_0..3.png` en `ember_0..3.png` (kopieën van `sand_gust_*`, alleen voor `runData`; T levert de echte).
- **Ontbreekt nog:** test S met twee clients (W-checklist hierboven: beide clients dezelfde bommen, `mobGriefing false`, dak, grenzen 2→3/3→4, Particles Minimal, FPS). Kan in de eigen worktree of bij integratie 1; `run/server.properties` staat klaar (RCON uit gezet, weer aanzetten: `enable-rcon=true`, poort 25585).
- Geen wachtende review; geen draaiende processen, slots of locks van A.

## Hervat 2026-10-07 — test S (optie B van main)
- Server (25575, wereld `ring3-atmosphere-a`) startte opnieuw zonder fouten. Via RCON gaf `/ca weather 3 eruption 60` `Set ring_3: eruption, 60 s …`; `/ca weather` toont ring 3 met de nieuwe types; `stop` werkte netjes.
- De client lukte niet. Hij laadde volledig zonder crash: de mixin-config werd geladen, de resources en particle-atlas ook (met de tijdelijke sprites). Daarna bleef hij in de frame-loop staan zonder verbinding: in een verse `run/` toont 1.21.1 eerst het onboarding-scherm voor toegankelijkheid, en quick-play wacht daarachter (onopgemerkt; gisteren gebeurde hetzelfde). Fix voor wie verder test: `run/options.txt` met `onboardAccessibility:false` en `skipMultiplayerWarning:true` (staat nu klaar in deze worktree). De tweede poging kreeg geen JVM-slot meer; daarna afgebroken op verzoek van main (lock niet langer vasthouden).
- **Nog open, naar de integratie-S (waves.md §6.5):** de in-game W-checklist met 2 clients (zelfde bommen, `mobGriefing`, dak, grenzen, Particles Minimal, FPS) en de visuele check van de mixin (rode lucht).
- Les voor het protocol: neem eerst het lock en pas dan de slots, en wacht nooit met het lock vast op meer slots. Een server + 2 clients vraagt alle 3 slots, dus dat lukt alleen als main de machine reserveert.
- Voortgang: 09:15 S (server) afgerond, client naar INT; lock/slot vrij, RCON uit.
