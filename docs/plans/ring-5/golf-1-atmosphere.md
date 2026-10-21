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

## CODEBASE-delta

## Beslissingen

## Backlog
