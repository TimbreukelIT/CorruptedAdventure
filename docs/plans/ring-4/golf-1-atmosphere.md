# Ring 4 — golf 1, stroom A (atmosphere): pakket 70 ringweer ring 4

Scope: `atmosphere`. Branch `ring4/atmosphere-g1` (poort n = 5), basis `main` 7ffbfeb. Contract: `contract.md` §6 (weer, geluid, particles), §8 (lang); band: `59-worldgen.md` (`Ring4Band`); ontwerp: `fase-1-wereld.md` → Ring 4 → Weer.

## Plan

**Weertypes en band-regel**
- `RingWeatherType.STORM_WIND` (ring 4, overworld) erbij, **achteraan** de enum: de wire-indices van de bestaande typen blijven gelijk (de payload verandert niet). `DENSE_FOG` en `TOXIC_RAIN` bestonden al.
- Eén cyclus voor ring 4 (bestaande `RingWeatherCycle`, geen wijziging aan `RingWeatherData` of `RingWeatherPayload`). Het type bepaalt de band: nieuwe pure klasse `weather/Ring4WeatherBands`:
  - `bandOf(type)`: `DENSE_FOG`, `STORM_WIND` → `MYTHIC`; `TOXIC_RAIN` → `BLIGHTED`; de rest `ANY`;
  - `appliesIn(type, blighted)` en `felt(ring, state, blighted)`: gameplay, harde grens = `Ring4Band.blighted` (value ≥ 0, het midden van de overgangsstrook);
  - `renderShare(type, bandValue)`: rendering, smoothstep over de hele overgang (value −1 … 1), ½ in het midden.
- **`RingWeather.stateAt(level, x, z)` is band-bewust** (semantiek, ook voor stroom C): eerst de ring van `ringAt(…).ring()` (harde wissel op 0,5, void → ring 5); is dat ring 4 en hoort het type niet bij de band van de plek (`Ring4Band.of(layout).blighted(x, z)`), dan geeft `stateAt` een **`clear`-staat met dezelfde resterende en totale duur** (intensiteit 0, `isStill()` false). Typen van andere ringen en `clear` komen ongewijzigd terug. `RingWeather.state(level, ring)` blijft de ruwe staat van de ring (geen band).

**Mist-hook fase 4** (vorm bepaald in 70): `RingWeather.denseFogAt(level, x, z)` → sterkte 0–1 van dikke mist op die plek (na ring- en bandregel van `stateAt`, volgt de fade), en `RingWeather.isDenseFogAt(level, x, z)` (= `denseFogAt` > 0). Puur server-side staat, geen client-logica.

**Stormduw** (server, `weather/StormWind`, patroon zandstorm change 28): elke 10 ticks (`GUST_TICKS`, code-constante: het contract heeft geen sleutel) per speler in de overworld van een ringwereld: `stateAt` op de speler = `STORM_WIND` met intensiteit > 0, voeten op of boven `stormPushMinY` (120), niet creatief/spectator, geen passagier, hemel zichtbaar op ooghoogte (`canSeeSky`) → additieve stoot `richting × stormPushStrength × intensiteit` via de bestaande **`WindGustPayload`** (geen nieuwe payload). Richting = `SandstormWind.direction(state)` (uit de `seed`; server, client-regen en F3 gebruiken dezelfde functie). Ring 4 en de mythische band volgen uit `stateAt`.

**Config `[weather.ring4]`** (precies de contractsleutels): `clearMinSeconds`, `clearMaxSeconds`, `weatherMinSeconds`, `weatherMaxSeconds`, `denseFogWeight`, `stormWindWeight` (komt automatisch met het nieuwe type), `toxicRainWeight`, en nieuw blok `ServerConfig.Ring4WeatherEffects` (`ServerConfig.RING_4_WEATHER`): `toxicRainPoisonMultiplier` (1,0; 0–10; C leest hem in zijn gif-modifier), `stormPushStrength` (0,05; 0–0,5), `stormPushMinY` (120; −64–320).

**Client** (alleen `client/weather/`, via `Dist.CLIENT` geregistreerd in `CorruptedAdventureClient` → `RingWeatherClientHooks.registerRing4()`):
- `RingWeatherClientHooks.shares` schaalt een ring 4-weer met `Ring4WeatherBands.renderShare` op `Ring4Band.of(ClientRingLayout)` (de gesyncte layout, geen payload). Daardoor volgen fog, fogkleur, luchtkleur (mixin), regen, geluid en F3 de band.
- `RainSheet`: vanilla's regenstrepen (texture `rain.png`, vanilla-geometrie per kolom boven `MOTION_BLOCKING`) met eigen tint, sterkte en scheefstand, op `AFTER_WEATHER` (ook in biomes zonder neerslag; ook in "Fabulous!" in het weather-target).
- `DenseFogRenderer`: fog naar ~32 blokken (terrein en lucht), witgrijs `#D9DFE4`, lucht 80 % naar de mistkleur, zachte hum-loop.
- `StormWindRenderer`: grijze lucht `#5F6A73` (85 %), terreinfog ×0,6, schuine bleekblauwe regen (`#C8D4E0`, scheefstand 0,6 × sterkte met de wind mee), schuim (vanilla `splash`) van open water met de wind mee, wind-loop; F3-regel `CA wind` toont ook de storm.
- `ToxicRainRenderer`: rechte gifgroene regen `#7FD13B`, groenige waas `#6F8A3C` (terreinfog ×0,7, 60 %), lucht 35 % groenig, regen-loop.
- Alles × `SkyExposure` behalve de lucht (de regen stopt bovendien per kolom zelf onder een dak). In water/lava/blindheid blijft vanilla-fog.

**Geluid en lang**: sound events `weather.ring_4.dense_fog`, `weather.ring_4.storm_wind`, `weather.ring_4.toxic_rain` in `ModSoundEvents`; `sounds.json` met **vanilla-placeholders** (mist: `ambient.underwater.loop` pitch 0,6; storm: `item.elytra.flying` pitch 0,5; gifregen: `weather.rain` pitch 0,8); de echte audio komt uit het ring 4-geluidsscript in 80. Ondertitels in `Ring4WeatherText` (en + nl). Geen nieuwe particle types in 70 (de contractnamen zijn voor 80; schuim gebruikt vanilla `splash`).

## Client/server-impact
- **Server:** cyclus (ongewijzigd), band-bewuste `stateAt`, mist-hook, stormduw (stoot via de bestaande `WindGustPayload`). `Ring4Band` is common code op de layout-noiseSeed.
- **Client:** alle rendering en geluid in `client/weather/`, geregistreerd vanuit `CorruptedAdventureClient` (alleen `Dist.CLIENT`). **Geen client-class op de server**: `weather/*` (`Ring4WeatherBands`, `StormWind`, `RingWeather`) gebruikt geen `client`-klassen; **geen `Minecraft.getInstance()` buiten `client/`**. De client bepaalt de band zelf met `Ring4Band` op de gesyncte layout.
- **Netwerk:** geen nieuwe payload, geen wijziging van een bestaande; `STORM_WIND` achteraan de enum, dus de indices van de bestaande typen blijven. `PROTOCOL_VERSION` ongemoeid.

## Tests (licht, golf 1)
- `./gradlew build` (incl. unit tests), `./gradlew runData` + `git diff --stat src/generated` (eigen output gecommit). Geen runServer/determinisme/perf (INT).
- Unit tests: `weather/Ring4WeatherTest` (typen en wire-index, band per type, `felt` = clear met zelfde timing, gameplaygrens = `Ring4Band.blighted` op de echte band van seed 20260930, `renderShare` blend en som 1, één cyclus met alle drie typen en altijd `clear` ertussen, duw-drempel Y/creatief/passagier/type, richting uit de seed en stootgrootte), `config/Ring4WeatherConfigTest` (precies de contractsleutels, standaarden), `client/weather/Ring4WeatherRenderTest` (mist-fog 32, scheefstand langs de wind).
- Voor INT/5b (niet nu): `/ca weather 4 dense_fog|storm_wind|toxic_rain` in beide banden; stormduw op een eiland > Y 120 en niet eronder; F3 `CA weather`/`CA wind`; twee clients zien dezelfde stormrichting.

## Voortgang
- 2026-10-13: plan, vragen Q1–Q4 aan main; antwoorden alle A (zie Beslissingen). Code, tests. Commit 00de6ea.
- 2026-10-13: code-review verwerkt (zie "Code-review"), vervolgcommit.

## Code-review (00de6ea)
Opgelost:
1. `RainSheet` zet de depth mask na het tekenen terug op `true` (vanilla zet hem alleen als zijn eigen regen valt).
2. `StormWindRenderer.renderLevel` kijkt eerst naar de stage (`AFTER_WEATHER`), pas dan `felt()`; `ToxicRainRenderer` doet geen `felt()` (de stage-check zit in `RainSheet`), `DenseFogRenderer` heeft geen `renderLevel`.
3. Storm- en gifregen volgen `SkyExposure` (sterkte × blootstelling), zoals waas en geluid.
4. Windrichting van `storm_wind` in `RingWeatherDebugLog` en in `/ca weather` via `SandstormWind.hasWind(type)` (zandstorm en storm). **Afwijking:** `command/RingWeatherCommand` is van I (`waves.md` §5); in golf 1 is er geen I-stroom, main gaf toestemming voor deze minimale uitbreiding (één regel).
5. `spawnFoam` slaat water over dat meer dan 32 blokken boven of onder de camera ligt.
6. `RingWeatherClientHooks.shares` gebruikt `ClientRingLayout.ringAt` en checkt expliciet de overworld vóór `Ring4Band`.

Niet opgepakt (besluit main):
- De band-regel zit alleen in `stateAt` (niet in `state(level, ring)`): C leest gifregen via `stateAt` én een eigen `Ring4Band`-check; INT1 controleert de samenhang.
- `CODEBASE.md`: volgens het worktree-protocol werkt INT het bij (delta hieronder).

## CODEBASE-delta
- `weather/RingWeatherType`: `STORM_WIND` (ring 4, achteraan, wire-indices ongewijzigd).
- `weather/Ring4WeatherBands` (nieuw): band per type (`bandOf`, `appliesIn`, `felt`, `renderShare`); de band-regel van het ring 4-weer.
- `weather/RingWeather`: `stateAt` band-bewust in ring 4 (clear-staat met dezelfde timing als het type niet bij de band hoort); mist-hook `denseFogAt` / `isDenseFogAt`; registreert `StormWind`.
- `weather/StormWind` (nieuw): stormduw boven `stormPushMinY` via `WindGustPayload`, richting `SandstormWind.direction`.
- `config/ServerConfig`: `Ring4WeatherEffects` (`RING_4_WEATHER`) in `[weather.ring4]`: `toxicRainPoisonMultiplier`, `stormPushStrength`, `stormPushMinY`; `stormWindWeight` via het nieuwe type.
- `registry/ModSoundEvents`: `RING_4_DENSE_FOG`, `RING_4_STORM_WIND`, `RING_4_TOXIC_RAIN` (`weather.ring_4.*`).
- `datagen/ModSoundDefinitionsProvider`: de drie ring 4-weerloops (vanilla-placeholders); `datagen/lang/Ring4WeatherText`: ondertitels en + nl.
- `client/weather/RingWeatherClientHooks`: `registerRing4()`; `shares` schaalt ring 4-weer met de band (`Ring4Band` op `ClientRingLayout`); `describeWind` ook voor de storm. `client/CorruptedAdventureClient`: roept `registerRing4()` aan.
- `client/weather/RainSheet` (nieuw): getinte, schuine vanilla-regenstrepen op `AFTER_WEATHER`.
- `weather/SandstormWind.hasWind(type)`: zandstorm en storm hebben een wind uit de seed; gebruikt door `client/weather/RingWeatherDebugLog` en `command/RingWeatherCommand` (`/ca weather` toont nu ook de stormrichting).
- `client/weather/DenseFogRenderer`, `StormWindRenderer`, `ToxicRainRenderer` (nieuw).
- Gegenereerd: `assets/corruptedadventure/sounds.json` (3 events), `lang/en_us.json`, `lang/nl_nl.json` (3 ondertitels).
- Tests: `weather/Ring4WeatherTest`, `config/Ring4WeatherConfigTest`, `client/weather/Ring4WeatherRenderTest`, `command/RingWeatherCommandWindTest`.

## Beslissingen
- 2026-10-13 | atmosphere | Ring 4-cyclus: standaard van change 20 (helder 600–9000 s, weer 600–1200 s), gewichten dikke mist 1 / stormwind 1 / gifregen 1 | gebruiker Q1 A: consistent met de andere ringen.
- 2026-10-13 | atmosphere | `stormPushStrength` 0,05 per stoot (elke 10 ticks, ~0,2 blok/s drift) | gebruiker Q2 A: merkbaar maar nooit van een eiland af met een paar blokken marge.
- 2026-10-13 | atmosphere | Zicht in volle dikke mist ~32 blokken | gebruiker Q3 A.
- 2026-10-13 | atmosphere | Storm en gifregen via één gedeelde regenrenderer (vanilla-regenstrepen, getint; storm schuin met de wind + grijze lucht + schuim, gifregen recht en groen), geen nieuwe particle-namen | gebruiker Q4 A.
- 2026-10-13 | atmosphere | `RingWeather.stateAt` is band-bewust: in ring 4 een `clear`-staat (zelfde timing) als het type niet bij de band van de plek hoort; `state(level, ring)` blijft ruw | implementatiekeuze 70, akkoord main; C leest gifregen hiermee.
- 2026-10-13 | atmosphere | Mist-hook fase 4: `RingWeather.denseFogAt(level, x, z)` (0–1) en `isDenseFogAt` (> 0) | vorm bepaald in 70, akkoord main.
- 2026-10-13 | atmosphere | Stoot-interval storm 10 ticks als code-constante; `WindGustPayload` hergebruikt; `STORM_WIND` achteraan de enum | geen config-sleutel of payload buiten het contract.
- 2026-10-13 | atmosphere | Rendering mengt over de bandovergang (smoothstep op `Ring4Band.value`), gameplay hard op value ≥ 0 | zoals ringweer over ringzones.
- 2026-10-13 | atmosphere | Placeholder-audio ring 4-weer: vanilla-events (mist `ambient.underwater.loop` 0,6; storm `item.elytra.flying` 0,5; gifregen `weather.rain` 0,8) tot het geluidsscript van 80 | akkoord main.

## Backlog
- `atmosphere` (80): echte audio voor de drie ring 4-weerloops uit het ring 4-geluidsscript; krakende eilanden in de storm; eventueel eigen schuim-particle i.p.v. vanilla `splash`.
- `atmosphere` (5b): kleuren, fog-afstanden en volumes van het ring 4-weer in-game afstemmen; scheefstand en dichtheid van de regen bekijken bij "Fast" (straal 5) en "Fancy" (10).
