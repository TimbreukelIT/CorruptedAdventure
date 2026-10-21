# Ring 5 golf 1 — stroom A (atmosphere): 92 ringweer ring 5

Scope: `atmosphere` — één commit (`atmosphere:`), branch `ring5/atmosphere-g1`, worktree
`P:/Projecten/Java/wt/r5-atmosphere-g1`, poort n = 5 (server 25575, RCON 25585), basis = de startcommit van `golf-1.md`.
**De kern (§1) hangt af van het antwoord op `golf-1.md` §6 OV1**; hieronder is optie A (aanbevolen) uitgewerkt. Bij
OV1 = B vervalt het afgeleide buitentype (§1.3): dan één type voor de ring en een kans per band om het te voelen (patroon
`Ring4WeatherBands`); de rest van het plan blijft.

Bronnen: `contract.md` §4 (`Ring5Band` op de gesyncte layout, geen payload), §6 en **§11** ("één cyclus; het type hangt
af van de band van de speler via gewichten per band: binnenband vooral `GLITCH_STORM` en `STANDSTILL`, buitenband vooral
`REVERSE_RAIN`; geen payload-wijziging"); `waves.md` §2b (de void-modifier bouwt C; A levert de factoren); `fase-1-wereld.md`
→ Ring 5 "Weer (ringweer)" en "Client/server-risico's"; sjabloon ring 4 (`docs/plans/ring-4/golf-1-atmosphere.md`:
`Ring4WeatherBands`, band-bewuste `RingWeather.stateAt`, `RainSheet`, `RingWeatherClientHooks.registerRing4()`,
`[weather.ring4]`). `CODEBASE.md` kent golf 0 nog niet: lees de CODEBASE-delta van `82-worldgen.md` (`Ring5Band`) erbij.
Voorstel standaardgewichten: `golf-1.md` §7 P2.

**Bezit** (`waves.md` §1/§5): `weather/*` (ring 5), `client/weather/*` (ring 5), `client/mixin/*` alleen waar het ring 5-
weer een bestaande A-mixin uitbreidt (`LevelRendererCloudsMixin`), `RingWeatherType`, `ServerConfig` `[weather.ring5]`
(eigen klasse `Ring5WeatherEffects` + de regel in het static-blok), `ModSoundEvents` (blok ring 5), `Ring5WeatherText`,
`assets/corruptedadventure/sounds.json` via `ModSoundDefinitionsProvider` (ring 5-regels). **Niet:** de void-modifier (C),
`command/*` (I; uitzondering hieronder), particle types (98).

---

## 1. Server: weertypes, band-regel, cyclus

1. **`RingWeatherType.STANDSTILL`** (`standstill`, ring 5, overworld) **achteraan** de enum (na `STORM_WIND`): de
   wire-indices van de bestaande typen blijven. `GLITCH_STORM` en `REVERSE_RAIN` bestaan al. Fade: `STANDSTILL` krijgt een
   korte eigen fade (voorstel 100 ticks: het bevriest bijna ineens), de andere twee de standaard (600).
2. **Eén cyclus voor ring 5** (bestaande `RingWeatherCycle`, geen wijziging aan `RingWeatherData` of
   `RingWeatherPayload`). De cyclus trekt het **binnenbandtype** met de gewichten `glitchStormWeight`/`reverseRainWeight`/
   `standstillWeight` van `[weather.ring5]` (de bestaande generieke sleutels, die voor ring 5 "binnenband" betekenen;
   commentaar aanpassen) en de duur van dat type (`STANDSTILL` met zijn eigen bereik `standstillMinSeconds`–
   `standstillMaxSeconds`, zoals de aswolk van ring 3).
3. **Nieuwe pure klasse `weather/Ring5WeatherBands`** (common):
   - `Band { INNER, OUTER }`, `bandAt(layout, x, z)` = `Ring5Band.outer` (en ring `VOID` = buiten);
   - **`typeIn(state, band, outerWeights)`**: `CLEAR` blijft `CLEAR`; binnen = `state.type()`; buiten = een gewogen
     trekking uit `outer*Weight` met `state.seed()` als bron (deterministisch, dezelfde uitkomst op server en client: de
     SERVER-config wordt door NeoForge naar de client gesynct); **seed 0 = "gezet"**: dan geldt `state.type()` in beide
     banden (zie punt 5);
   - **`felt(state, band, …)`**: de staat zoals gevoeld in die band: zelfde timing, het type van `typeIn`. Heeft het
     afgeleide type een eigen, kortere duur (stilstand ≤ `standstillMaxSeconds`) dan het venster, dan geldt het alleen in
     het eerste deel van het venster (`felt` maakt daar een eigen staat met die duur, zodat de fade klopt) en daarna
     `clear` met dezelfde resterende tijd. Puur, dezelfde formule op beide kanten;
   - `renderShares(state, bandValue)`: aandeel binnen/buiten voor de rendering, smoothstep over `Ring5Band.value`
     (−1 … 1), ½ in het midden, som 1.
4. **`RingWeather.stateAt(level, x, z)`** wordt band-bewust voor ring 5 (en `void`, dat ring 5 volgt): gameplay wisselt
   hard op `Ring5Band.outer` (`value ≥ 0`). Dit is de API voor C (void-modifier). `RingWeather.state(level, ring)` blijft
   de ruwe staat (= het binnentype).
5. **`RingWeather.set`** voor ring 5 (`/ca weather 5 <type>`) start de staat met **seed 0**, zodat het gezette type in
   beide banden geldt (testen bij 6b). De cyclus vermijdt seed 0 (een getrokken 0 wordt 1).
6. **Geen serverkant-gameplay** behalve de staat: geen duw, geen schade (de void-factoren past C toe).

## 2. Config `[weather.ring5]`

- Bestaand/automatisch: `clearMinSeconds`, `clearMaxSeconds`, `weatherMinSeconds`, `weatherMaxSeconds`,
  `glitchStormWeight`, `reverseRainWeight`, `standstillWeight` (binnenband).
- Nieuw blok `ServerConfig.Ring5WeatherEffects` (`ServerConfig.RING_5_WEATHER`):
  - `outerGlitchStormWeight`, `outerReverseRainWeight`, `outerStandstillWeight` (buitenband);
  - `standstillMinSeconds` (40), `standstillMaxSeconds` (120) — kort en zeldzaam (contract §6);
  - `glitchStormVoidMultiplier` (2,0), `reverseRainVoidMultiplier` (1,5), `standstillVoidMultiplier` (1,5); bereik 0–10;
    commentaar: "applied by stream C's void modifier; the largest of night and weather counts".
- Standaardgewichten (P2): binnen glitchstorm **55**, stilstand **30**, omgekeerde regen **15**; buiten omgekeerde regen
  **70**, glitchstorm **20**, stilstand **10** (`RingWeather.defaultWeight` voor de ring 5-typen).
- Validatie: min ≤ max voor stilstand (terugval op de standaard, ERROR-log zoals de aswolk).

## 3. Client (alleen `client/weather/`, via `Dist.CLIENT` in `CorruptedAdventureClient` → `RingWeatherClientHooks.registerRing5()`)

- **Aandelen:** `RingWeatherClientHooks.shares` geeft voor ring 5 twee aandelen (binnentype × (1 − s), buitentype × s)
  met `Ring5WeatherBands.renderShares` op `Ring5Band.of(ClientRingLayout)` en de gesyncte staat; daardoor volgen fog,
  lucht, neerslag, geluid en F3 de band. Geen payload.
- **`GlitchStormRenderer`:** beeldruis-overlay (HUD-laag met korte flikkerende ruisblokjes en scanlines, sterkte ×
  intensiteit × aandeel; respecteert F1), af en toe een **HUD-hapering** (de hotbar 1–2 frames verschoven of weg, nooit
  langer dan ~0,1 s), **haperend geluid** (korte dips van het volume van weer-/ambient-geluid), violet-zwarte flitsen in
  de fog-kleur; deeltjes tijdelijk de bestaande `glitch_speck` (ring 4) — `glitch_pixel` komt in 98.
- **`ReverseRainRenderer`:** `RainSheet` met omgekeerde richting (druppels van de grond omhoog), zwart-violette tint
  (voorstel `#1A1022`), ook in biomes zonder neerslag; stopt per kolom onder een dak; × `SkyExposure`.
- **`StandstillRenderer`:** regenstrepen die **stilstaan** (de `RainSheet`-geometrie bevroren op het moment van
  invallen, tijd uit de seed), **wolken stil** (uitbreiding van `LevelRendererCloudsMixin`: de wolkentijd bevriest zolang
  het aandeel > 0), **geluid valt weg**: alle wereldgeluid en muziek (niet de UI) gedempt met factor
  1 − 0,85 · aandeel · intensiteit (vol = ~15 %) via een client-luisteraar op afgespeelde geluiden; iets wittere, vlakke
  fog.
- Alles × `SkyExposure` behalve de lucht en het geluid-dempen van stilstand; in water/lava/blindheid blijft vanilla-fog.
- **F3:** `CA weather` toont voor ring 5 het type binnen en buiten.
- **`/ca weather`:** één regel die voor ring 5 ook het buitentype toont (`command/RingWeatherCommand`, van I: minimale
  uitbreiding met toestemming van main volgens dit plan, zoals in ring 4).

## 4. Geluid en lang
Sound events `weather.ring_5.glitch_storm`, `weather.ring_5.reverse_rain`, `weather.ring_5.standstill` in
`ModSoundEvents`; `sounds.json` met **vanilla-placeholders** (voorstel: glitchstorm `block.respawn_anchor.ambient` pitch
0,5; omgekeerde regen `weather.rain` pitch 0,6 met lage volume; stilstand een bijna stille `ambient.basalt_deltas.loop`
pitch 0,5 op volume 0,1); de echte audio komt uit het ring 5-geluidsscript in 98. Ondertitels in `Ring5WeatherText`
(en + nl). **Geen nieuwe particle types in 92** (98; T levert de sprites al in 99a).

## 5. Client/server-impact
- **Server:** cyclus (ongewijzigd), band-bewuste `stateAt` (ring 5), `Ring5WeatherBands` (puur, common). `Ring5Band` is
  common code op de layout-`noiseSeed`.
- **Client:** alle rendering en geluid in `client/weather/` (+ de bestaande wolken-mixin), geregistreerd vanuit
  `CorruptedAdventureClient` (alleen `Dist.CLIENT`). **Geen client-class op de server:** `weather/*` gebruikt geen
  `client`-klassen; geen `Minecraft.getInstance()` buiten `client/`.
- **Netwerk:** geen nieuwe payload en geen wijziging van een bestaande; `STANDSTILL` achteraan (nieuwe index). Omdat C in
  dezelfde golf `Condition.VOID` toevoegt, verhoogt **INT1** `PROTOCOL_VERSION` één keer.
- **Multiplayer:** spelers in verschillende banden zien tegelijk ander weer (OV1-A); twee clients in dezelfde band zien
  hetzelfde (zelfde seed, zelfde gesyncte config).

## 6. Tests (licht: B, D, unit)
- `Ring5WeatherTest`: `STANDSTILL` achteraan, wire-indices van de oude typen gelijk; binnentype = staat; buitentype uit de
  seed deterministisch en volgens de gewichten (verdeling over 10.000 seeds ± 2 pt); seed 0 → beide banden het gezette
  type; afgeleide stilstand ingekort tot ≤ `standstillMaxSeconds` met daarna `clear` en dezelfde resterende tijd;
  `stateAt` wisselt op `Ring5Band.outer` van seed 20260930; `void` = buiten; `renderShares` som 1 en ½ op de lijn; één
  cyclus met alle drie typen en altijd `clear` ertussen; stilstand duurt 40–120 s.
- `Ring5WeatherConfigTest`: precies de sleutels van §2 en de standaarden; validatie min/max.
- `client/weather/Ring5WeatherRenderTest` (pure delen): omgekeerde richting van de `RainSheet`, bevroren tijd bij
  stilstand, volumefactor.
- D: `sounds.json` (ring 5-regels), lang en + nl (gecommit).
- **S-smoke** (verse wereld `r5-a92`): log zonder ERROR en zonder client-class-fout; RCON `/ca weather 5 glitch_storm`,
  `reverse_rain`, `standstill` en `/ca weather` (status). Geen client.
- Voor INT1/6b (niet nu): de drie typen in beide banden met een client; twee clients in verschillende banden.

**Af als:** B, D, unit tests en S-smoke groen; `/code-review` verwerkt; de koppelpunten voor C (factoren, `stateAt`) staan
hieronder.

## 7. Koppeling met C (voor INT1)
- C's `VoidAmplifierModifier` leest `ServerConfig.RING_5_WEATHER.{glitchStorm,reverseRain,standstill}VoidMultiplier`.
- C vraagt het weer alleen via `RingWeather.stateAt`; dezelfde bandgrens als `VoidRule` (`Ring5Band.outer`).

## Voortgang
- Voortgang: 2026-10-21 20:47 worktree `P:/Projecten/Java/wt/r5-atmosphere-g1` op `ring5/atmosphere-g1` vanaf `757523bf`, `setup.sh A 5`.
- Voortgang: 2026-10-21 21:00 code + tests geschreven; wacht op een slot voor de eerste build.
- Voortgang: 2026-10-21 21:15 `build` + `runData` groen (1165+ tests; één oude assert in `RingWeatherConfigTest` aangepast: ring 5 heeft eigen gewichten); `src/generated` alleen lang + `sounds.json`.
- Voortgang: 2026-10-21 21:19 S-smoke `r5-a92` (poort 5): log zonder ERROR/client-class-fout; RCON `/ca weather`, `/ca weather 5 glitch_storm|reverse_rain|standstill|clear` (stilstand 69 s uit 40–120, beide banden gelijk bij seed 0), fout-melding voor een ring 4-type in ring 5. Client-mixin-doelen gecontroleerd met `javap` (twee `GETFIELD ticks` + één float-argument in `renderClouds`; `SoundEngine#getVolume(SoundSource)`), niet in een client geladen.
- Voortgang: 2026-10-21 21:20 eigen review op de worktree-diff (zie Beslissingen); commit.

## CODEBASE-delta

- `weather/RingWeatherType`: **`STANDSTILL`** (`standstill`, ring 5, overworld, fade 100 ticks) achteraan (index 11; de
  oude indices blijven). Ring 5 heeft nu `glitch_storm`, `reverse_rain`, `standstill`.
- Nieuw `weather/Ring5WeatherBands` (common, puur): `Band {INNER, OUTER}`, `bandAt(layout, ring, x, z)` (=
  `Ring5Band.outer`, ring `VOID` = buiten), `typeIn`/`felt(state, band, Settings)` (binnen = de cyclus; buiten een
  gewogen trekking uit `state.seed()` met de buitengewichten; seed 0 = gezet type in beide banden; een afgeleide
  stilstand korter dan het venster geldt alleen aan het begin, daarna `clear`), `outerShare(bandValue)` (smoothstep, ½
  op de lijn), record `Settings(outerWeights, standstillTicks)`.
- `weather/RingWeather.stateAt`: band-bewust voor ring 5 en `void` (hard op `Ring5Band.outer`) — de API voor C.
  `RingWeather.set` voor ring 5 zet seed 0. `RingWeatherCycle.seed(random)`: nooit 0 (alle ringen, één trekking zoals
  voorheen).
- `config/ServerConfig`: `RING_5_WEATHER` = `Ring5WeatherEffects` in `[weather.ring5]`: `outerGlitchStormWeight` 20,
  `outerReverseRainWeight` 70, `outerStandstillWeight` 10, `standstillMinSeconds` 40, `standstillMaxSeconds` 120,
  `glitchStormVoidMultiplier` 2,0, `reverseRainVoidMultiplier` 1,5, `standstillVoidMultiplier` 1,5 (+ `voidMultiplier(type)`
  voor C); binnenbandgewichten (de bestaande sleutels) 55/15/30 via `RingWeather.defaultWeight`. `ServerConfig.ring5Bands()`
  (ook client-side; standaarden zolang de gesyncte config er niet is). Stilstand: eigen duurbereik in de cyclus, min > max
  → standaard + ERROR-log.
- `command/RingWeatherCommand.describe`: ring 5 met weer toont ook de buitenband (`… (inner band) | outer band: …`).
- `registry/ModSoundEvents`: `weather.ring_5.{glitch_storm,reverse_rain,standstill}`; `sounds.json` met vanilla-
  placeholders (`block.respawn_anchor.ambient` p 0,5; `weather.rain` p 0,6 v 0,5; `ambient.basalt_deltas.loop` p 0,5
  v 0,1). `datagen/lang/Ring5WeatherText`: ondertitels en + nl.
- Client (`client/weather/`): `RingWeatherClientHooks.registerRing5(modBus, gameBus)` (vanuit `CorruptedAdventureClient`,
  vóór `register`); `shares` splitst ring 5 in binnen- × (1 − s) en buitentype × s (`addRing5`), één aandeel bij gelijk
  type; F3 `CA weather` krijgt ` [ring_5 inner …, outer …]`. Nieuw: `GlitchStormRenderer` (fog/flitsen/specks/geluid),
  `GlitchStormOverlay` (HUD-laag `corruptedadventure:glitch_storm` boven `camera_overlays`: ruisblokjes + scanlines;
  hotbar-hapering 1–2 ticks via `RenderGuiLayerEvent` LOWEST), `ReverseRainRenderer` (`RainSheet` met snelheid −0,8,
  `#1A1022`), `StandstillRenderer` (bevroren `RainSheet` op een tijd uit de seed, witte vlakke fog), `StandstillClouds`
  (wolkenklok vertraagt tot stilstand, geen sprong daarna), `Ring5SoundDamping` (stilstand: alle categorieën behalve
  `MASTER` × (1 − 0,85 · sterkte); glitchstorm: korte dips van `WEATHER`/`AMBIENT`; ververst lopende kanalen bij een
  wijziging). `RainSheet.render(…, time)` + `scroll(...)` (negatieve snelheid = omhoog).
- Client-mixins: `LevelRendererCloudsMixin` uitgebreid (`@ModifyVariable` partialTick + `@Redirect` op `ticks` in
  `renderClouds`); **nieuw `SoundEngineVolumeMixin`** (`SoundEngine#getVolume` RETURN × `Ring5SoundDamping.volume`), in
  `corruptedadventure.mixins.json` → `client`.
- Tests: `weather/Ring5WeatherTest`, `config/Ring5WeatherConfigTest`, `client/weather/Ring5WeatherRenderTest`;
  `Ring4WeatherTest` (index `STORM_WIND` = 10).

## Beslissingen

Gebruiker (2026-10-21): OV1 = A (gedeelde tijden, eigen type per band, overvloei over de ~200-blok-overgang, geen
payload); P2-gewichten binnen 55/30/15, buiten 70/20/10, stilstand 40–120 s. Implementatiekeuzes van de stroom:
- **Buitentype uit de seed:** een 32-bit hash (murmur3-finaliser) van `state.seed()` met een vaste salt, `floorMod` over
  de som van de buitengewichten in enum-volgorde; de lengte van een afgeleide stilstand met een tweede salt (uniform in
  40–120 s). Server en client rekenen hetzelfde (zelfde seed, gesyncte SERVER-config; vóór de sync de standaarden).
- **Seed 0 = gezet:** `RingWeather.set` zet voor ring 5 seed 0; `RingWeatherCycle.seed` maakt van een getrokken 0 een 1
  voor alle ringen (één trekking zoals voorheen, dus dezelfde reeks; windrichting bij seed 0 vs 1 is verwaarloosbaar).
- **Geluid dempen via een nieuwe client-mixin** `SoundEngineVolumeMixin` (+ regel in `corruptedadventure.mixins.json`)
  i.p.v. alleen een luisteraar op afgespeelde geluiden: `PlaySoundEvent` bereikt lopende geluiden (muziek!) niet, en een
  wrapper-instantie breekt de `isActive`-checks van `MusicManager` en `WeatherLoop`. De mixin vermenigvuldigt de
  categorievolume (niet `MASTER` = UI); bij een wijziging ververst `Ring5SoundDamping` de lopende kanalen via
  `SoundManager.updateSourceVolume(AMBIENT, …)`. Afwijking van het bezit (§ "Bezit": alleen bestaande mixins): nieuw
  bestand, raakt niemand anders.
- **Wolken:** geen harde bevriezing maar een vertraagde klok (`lag += sterkte` per tick): ze remmen af met de fade (100
  ticks) en drijven daarna verder vanaf waar ze stonden, zonder sprong.
- **Glitchstorm-HUD:** laag boven `camera_overlays` (onder de HUD-elementen), max. 48 ruisblokjes + scanlines om de 3 px,
  patroon wisselt elke 2 ticks; hotbar-hapering kans 2 %/tick bij vol, 1–2 ticks, ⅓ weg, anders 4–15 px verschoven.
  Geluidsdips 5 %/tick, 2–5 ticks, diepte 0,7; flitsen 3 %/tick, 2–4 ticks, `#120818` (ongeacht daglicht).
- **Kleuren/waarden (constanten, bij 6b bij te sturen):** glitchstorm-waas `#2A2233`, fog ×0,6; omgekeerde regen
  `#1A1022`, snelheid −0,8, waas `#2B1E38`, fog ×0,75; stilstand-strepen `#C4C8D2` op 55 %, bevroren tijd =
  `floorMod(seed, 4096)`, fog `#D6D7DB` ×0,8 ("vlak" = kleurloos; geen near-plane-truc).
- **Stilstand-loop** wordt zelf ook gedempt (blijft dus bijna stil); dat is de bedoeling.
- **`/ca weather`:** `ring_5: <type>, … (inner band) | outer band: <type>, …`; F3: ` [ring_5 inner …, outer …]` achter de
  aandelen.

**Open punt voor 6b (niet blokkerend, volgt uit OV1 = A + gedeelde tijden):** trekt de binnenband een stilstand (venster
40–120 s), dan krijgt de buitenband in dat korte venster zijn eigen type (meestal omgekeerde regen) met de gewone fade
van 30 s: een korte, zwakke bui (piek ~0,67–1,0). Opties: **A (huidig)** zo laten; **B** buiten `clear` als het venster
een stilstand is en het afgeleide type niet; **C** een kortere fade voor afgeleide typen in een kort venster.

## Backlog

- `atmosphere` (98): `glitch_pixel` en `reverse_raindrop` als particles in de renderers (nu tijdelijk `glitch_speck`, geen
  druppel-particles); echte audio voor de drie weer-loops (nu vanilla-placeholders).
- `conditions` (C, INT1): `VoidAmplifierModifier` leest `ServerConfig.RING_5_WEATHER.voidMultiplier(type)` op
  `RingWeather.stateAt(...)` (de felt-staat, met zijn fade); dezelfde bandgrens (`Ring5Band.outer`, void = buiten).
- `infra` (INT1): `ModNetwork.PROTOCOL_VERSION` "8" → "9" (`STANDSTILL` is een nieuwe enum-index).
