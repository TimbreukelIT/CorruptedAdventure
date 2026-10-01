# Change 20 — Ringweer-systeem (basis, nog geen weertypes met effect)

Scope: `atmosphere` — change 20

**Doel:** de basis voor het ringweer uit `fase-1-wereld.md` §1.6: per dimensie een `SavedData` met per ring een weerstaat (type + resterende duur), een server-side cyclus met waarden uit de config, sync naar alle clients en een client-hook waar de weer-changes van ring 2–5 (28, 45, 66, 85) hun rendering aan hangen. Nog **geen** zichtbare of voelbare effecten: geen fog, particles, geluid of gameplay. Test: `/ca weather <ring> <type>` synct naar alle clients.

---

## Afstemming (2026-10-01)

Afgestemd met 18, 19, 27, 28 en 32 (basis: de aanbevolen opties). Waar de tekst hieronder iets anders zegt, geldt dit blok.
- **Preset-check:** `RingWorld.isActive(server)` (change 18) + per dimensie `RingWeatherType.hasWeather(dimension)`; geen eigen generator-check.
- **Client:** handler `client/network/RingWeatherHandler` via `ClientboundHandlers` (18); de F3-regel komt als extra regel in `client/RingDebugOverlay` (18), geen tweede F3-listener. `ClientRingLayout` (18) staat in het plan van 18: de harde afhankelijkheid is afgedekt.
- **`RingWeatherState` krijgt nu al `int seed`** (getrokken bij elk nieuw weer uit `level.getRandom()` en door `/ca weather`; `CODEC` `optionalFieldOf("seed", 0)`, `STREAM_CODEC` `VAR_INT`; test in `RingWeatherStateTest`/`RingWeatherCycleTest`). Gebruiker: 28 (windrichting), later 45. Change 28 hoeft het staat- en payloadformaat dan niet te wijzigen.
- **Config:** `[weather]` en `[weather.ring2]`…`[weather.ring5]` in `config/ServerConfig` van change 19 (SERVER, per wereld, live gelezen), bij 19 vraag 4 A. Kiest 19 iets anders, dan volgt deze change dat patroon. De beslissingenlog-regel "per ring in de COMMON-config" wordt "SERVER-config".
- **Gameplay-API:** condities lezen het weer via `RingWeather.state(...)`/`stateAt(...)`; er komt **geen** weer-veld in `ConditionContext` (afspraak 19/27/28).
- **Basis vs. overlay:** biome-kleuren zijn biome-data (32); `RingWeatherRenderer.fogColor`/`renderFog` legt het weer er alleen overheen.
- **Teststop (besloten 2026-10-01, teststop 1 van 3):** deze change draagt de **gebundelde blokkerende teststop 1 voor 18 + 19 + 20** (dedicated server + `Dev` + `Dev2`, ~20 min): de handtest van 18 (stap 1–3), de HUD-checklist van 19 en de F3/weer-checklist hieronder (zie Tests → "Teststop 1"). Change 21 begint pas na akkoord van de gebruiker op deze stop.
- **Ritme (vraag 3 = B, keuze gebruiker):** vanilla-ritme — helder 12.000–180.000 ticks, weer 12.000–24.000 ticks, per ring in de SERVER-config, gelijke gewichten. Gevolg: een speler ziet ringweer zeldzamer; de cyclus wordt getest via korte config-waarden (zie S), het weer zelf via `/ca weather`.

---

## Afhankelijkheden en aannames

- **Gebouwd (gecontroleerd in de code):**
  - `RingLayout.ringAt(dimension, x, z)` → `RingPosition(inner, outer, blend)` met `ring()` (wissel op 0,5) en `inTransition()`; `RingLayout.current()` alleen zolang er een server draait. `Ring` heeft `CODEC`, `byIndex`, `number()`.
  - `CaCommand` (`/ca`, permissieniveau 2) met `.then(...)` per subcommando; `CaCommand.register(NeoForge.EVENT_BUS)` in `CorruptedAdventure`. CODEBASE.md noemt `/ca weather` al als haakje voor change 20.
  - `CommonConfig` (COMMON, `corruptedadventure-common.toml`), nu alleen sectie `rings`; "🔲 balanswaarden" staat gepland.
  - `CorruptedAdventureClient` (`@Mod(dist = Dist.CLIENT)`) is leeg op een log-regel na; `client/` bestaat verder niet.
  - Preset-herkenning: `RingWorldBorder.isCorruptedWorld` (private) = overworld-generator `instanceof RingChunkGenerator`. *(Afgestemd 2026-10-01: change 18 verhuist die check naar `ring/RingWorld.isActive(server)`; deze change gebruikt **die** — geen eigen generator-check.)*
  - `weather/`, `network/`, `data/` staan als 🔲 in CODEBASE.md; ringweer is er nog niet. Ring 1 heeft volgens het design **geen** ringweer (alleen vanilla regen/onweer); het ring 1-sfeerwerk (change 13) is puur biome-data (`mood_sound`, kleuren) en raakt dit systeem niet.
- **Aanname change 18 (`infra`, nog niet gebouwd — werktitels, bij de start tegen `CODEBASE.md` leggen):**
  - Package `network/` met een registratieklasse (werktitel `ModNetwork`) die naar `RegisterPayloadHandlersEvent` luistert, een `PayloadRegistrar` met protocolversie, en een **vast patroon voor clientbound payloads** waarbij de client-handler in `client/` staat en een dedicated server die class nooit laadt. Deze change volgt dat patroon letterlijk.
  - Een **client-side ringlayout** (werktitel `ClientRingLayout` in `client/`, gebouwd uit de gesyncte `RingSettings` + een afgeleide ruis-seed, backlog-regel bron change 2) met een `ringAt(dimension, x, z)` → `RingPosition`. Nodig voor de vraag "in welke ring staat de lokale speler" op de client. **Harde afhankelijkheid:** bestaat dit niet, dan stopt de bouw en gaat de vraag naar de hoofdsessie.
  - Helper "naar spelers in een ring sturen": hier niet nodig (zie Sync), wel beschikbaar.
- **Change 19 (`conditions`, nog niet gebouwd; 19 vraag 4 = A besloten):** balanswaarden staan in de nieuwe SERVER-config `config/ServerConfig` (per wereld); deze change voegt daar `[weather]` aan toe. De conditie-tick van 19 leest nog **geen** weer; changes 27/28 e.v. koppelen dat via de server-API hieronder.
- **Nether:** het design heeft geen Nether-ringweer (`fase-1-wereld.md` Nether-3, beslissingenlog 2026-09-30 "geen Nether-ringweer"; ook Nether-2/4/5 noemen alleen biome-sfeer). Het systeem is per dimensie generiek, maar alleen dimensies met minstens één weertype krijgen staat, cyclus en `SavedData` — nu dus alleen de overworld.

---

## Plan

### 1. Datamodel (common, `weather/`)

- **`RingWeatherType`** — enum (vraag 2 → A), `StringRepresentable` + `CODEC` + `STREAM_CODEC` (op naam/index). Velden: `ring` (`Ring`, `null` voor `CLEAR`) en `dimension` (`ResourceKey<Level>`, nu altijd de overworld). Waarden: `CLEAR` + de typen volgens vraag 1. Hulpfuncties: `typesFor(dimension, ring)`, `hasWeather(dimension)`.
  - Bij vraag 1 → A: `SANDSTORM`, `HEAT_WAVE` (ring 2), `ASH_RAIN`, `ERUPTION` (ring 3), `DENSE_FOG`, `TOXIC_RAIN` (ring 4), `GLITCH_STORM`, `REVERSE_RAIN` (ring 5); geserialiseerd als `sandstorm`, `heat_wave`, `ash_rain`, `eruption`, `dense_fog`, `toxic_rain`, `glitch_storm`, `reverse_rain`.
- **`RingWeatherState`** — record `(type, remainingTicks, durationTicks)`; `CODEC` (opslag) en `STREAM_CODEC` (netwerk). `intensity()` = `min(1, verstreken / FADE_TICKS, resterend / FADE_TICKS)` voor niet-`CLEAR` (0 voor `CLEAR`), `FADE_TICKS` volgens vraag 4. Dezelfde formule op server (gameplay later) en client (rendering), dus geen per-tick-packets.
- **`RingWeatherCycle`** — pure logica, geen Minecraft-level: `advance(state, ticks, settings, random)` telt af en kiest bij 0 het volgende: na weer altijd `CLEAR`, na `CLEAR` een type van de ring op gewicht; duur uniform tussen min en max. Ring zonder typen (ring 1, void, Nether) → blijft `CLEAR` zonder einde. Random = `level.getRandom()` (geen worldgen, dus geen seed-eis); tests geven een geseede `RandomSource`.
- **`RingWeatherSettings`** — record per ring: `clearMin/MaxTicks`, `weatherMin/MaxTicks`, `Map<RingWeatherType, Integer> weights`. Gevuld uit de config.

### 2. Config

Nieuwe sectie `[weather]` in `config/ServerConfig` van change 19 (SERVER; standaard globaal in `config/corruptedadventure-server.toml`, per wereld te overschrijven met een kopie in `<wereld>/serverconfig/` — bouwnotitie change 19), per ring met typen een subsectie `[weather.ring2]` … `[weather.ring5]`:
- `clearMinSeconds`, `clearMaxSeconds`, `weatherMinSeconds`, `weatherMaxSeconds` — standaard (vraag 3 = B, vanilla-ritme) **600 / 9000 / 600 / 1200** s (= 12.000 / 180.000 / 12.000 / 24.000 ticks), voor elke ring gelijk; validatie min ≤ max, ≥ 1 (bovengrens bijv. 86.400 s);
- per type `<type>Weight` (0–100, standaard 1 = gelijke gewichten; 0 = komt in de cyclus niet voor, `/ca weather` kan het nog wel zetten).
- Accessor `ringWeather(Ring)` → `RingWeatherSettings`, live gelezen (geen snapshot: een gewijzigde config geldt vanaf de volgende wissel; er ontstaan geen breuklijnen zoals bij ringgrenzen).
- Ring 1 krijgt geen sectie zolang hij geen typen heeft.

### 3. Persistente staat (`weather/RingWeatherData`)

- `SavedData` per dimensie via `level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(…), "corruptedadventure_ring_weather")` → `<wereld>/data/corruptedadventure_ring_weather.dat` (overworld). Alleen aangemaakt voor dimensies met `hasWeather` en alleen in `ring_noise`-werelden.
- Inhoud: `EnumMap<Ring, RingWeatherState>` voor de ringen met typen; opslag via `RingWeatherState.CODEC` in NBT. Ontbrekende ring of onbekende typenaam (bijv. na een hernoeming) → `CLEAR` met een nieuw getrokken duur + waarschuwing in de log; nooit crashen.
- `setDirty()` bij elke wissel en elke cyclusstap (goedkoop: geschreven bij autosave).

### 4. Server-logica (`weather/RingWeather`)

- `register(IEventBus gameBus)` in `CorruptedAdventure`.
- `LevelTickEvent.Post` (alleen `ServerLevel`, alleen `ring_noise` + `hasWeather`): elke 20 ticks `RingWeatherCycle.advance(…, 20, …)` per ring; alleen als gamerule `doWeatherCycle` aan staat (vraag 5). Wisselt een type → sync.
- **Server-API voor latere changes** (condities 27, 28, 45, 66, 85):
  - `RingWeather.stateAt(ServerLevel, x, z)` → `RingWeatherState` van `RingLayout.ringAt(dim, x, z).ring()` (harde wissel op 0,5, zelfde regel als de biome); de void-zone (`VOID`) volgt ring 5.
  - `RingWeather.state(ServerLevel, Ring)`, `RingWeather.set(ServerLevel, Ring, type, durationTicks)` (gebruikt door het commando; synct).
- **Sync** (geen vraag, besluit in dit plan): elke speler krijgt de **volledige** staat van zijn dimensie (max. 5 × type + 2 ints), bij login, dimensiewissel en respawn, en na elke wissel naar alle spelers in die dimensie (`PacketDistributor.sendToPlayersInDimension`). Geen per-ring-tracking op de server; de client kiest zelf de ring (nodig om over een grens heen te mengen, vraag 4) en de grensmuren in fase 5 kunnen dezelfde staat lezen. Dimensie zonder ringweer → payload met lege lijst (de client wist dan zijn staat).

### 5. Netwerk (`network/`)

- **`RingWeatherPayload`** (`corruptedadventure:ring_weather`), clientbound, `StreamCodec`: dimensie-key + lijst `(Ring, RingWeatherState)`. Registratie in de netwerkklasse van change 18; client-handler in `client/` volgens het patroon van change 18.
- Protocolversie ophogen volgens de afspraak van change 18 (nieuwe payload).

### 6. Debugcommando (`command/RingWeatherCommand`)

Aangehaakt in `CaCommand`; vaste Engelse tekst; werkt in de dimensie van de bron (`execute in …`).
- `/ca weather` — per ring: type, resterende tijd (s), intensiteit; in een dimensie zonder ringweer: "No ring weather in this dimension".
- `/ca weather <ring 1–5> <type> [seconds]` — zet het weer (suggesties: `clear` + de typen van die ring; een type van een andere ring of ring 1 met een niet-`clear`-type → fout). Zonder `seconds`: duur uit de config. `clear` met een korte duur = cyclus direct laten doorschuiven.

### 7. Client-hook (`client/weather/`, alleen `Dist.CLIENT`)

- **`ClientRingWeather`** — staat van de huidige dimensie (`EnumMap<Ring, RingWeatherState>`), gezet door de payload-handler, lokaal afgeteld per client-tick (`ClientTickEvent.Post`, niet tijdens pauze); gewist bij uitloggen (`ClientPlayerNetworkEvent.LoggingOut`).
- **`RingWeatherRenderer`** — interface met alleen `default`-methodes (latere changes voegen er methodes bij zonder bestaande renderers te breken): `clientTick(ClientLevel, LocalPlayer, strength)` (particles, geluid), `renderFog(ViewportEvent.RenderFog, strength)`, `fogColor(ViewportEvent.ComputeFogColor, strength)`, `renderLevel(RenderLevelStageEvent, strength)` (bijv. stage `AFTER_WEATHER`).
- **`RingWeatherRenderers`** — `register(RingWeatherType, RingWeatherRenderer)`; aangeroepen vanuit `CorruptedAdventureClient` door de weer-changes. In change 20 leeg.
- **`RingWeatherClientHooks`** — luistert naar de vier client-events, bepaalt per frame/tick de **actieve typen met sterkte** voor de lokale speler: `ClientRingLayout.ringAt` op de spelerpositie → per betrokken ring `strength = state.intensity() × ruimtelijk gewicht` (vraag 4), void → ring 5, en roept de geregistreerde renderers aan. Eén centrale plek voor de berekening, zodat 28/45/66/85 alleen hun renderer schrijven.
- **F3-regel** (`CustomizeGuiOverlayEvent.DebugText`, rechts): `Ring weather: ring 2 sandstorm 0.83 (412 s)` (+ tweede ring in een overgangszone). Dit is het testbare effect van deze change.

### 8. Documentatie

- `CODEBASE.md`: `weather/`, `network/RingWeatherPayload`, `client/weather/` ✅ met de classes; command `/ca weather`; tabel "Netwerk en persistente staat" (payload + `SavedData`); configsectie; valkuilen die bij de bouw opduiken (bijv. payload-thread, F3-event).
- `fase-1-wereld.md` §1.6: "Uitwerking (change 20)" met datamodel, cyclus, sync, client-hook, grensgedrag, relatie met vanilla-weer, void → ring 5, geen Nether-ringweer.
- `02-beslissingenlog.md`: regels hieronder.
- Backlog alleen als er bij de bouw iets buiten `atmosphere` opduikt (bijv. lucht-kleur-hook voor change 45, zie risico's).

---

## Client/server-impact

- **Server:** staat, cyclus, `SavedData`, commando en de API voor condities leven op de server; ze laden geen client-class. Per level elke 20 ticks een paar optellingen; verwaarloosbaar.
- **Netwerk:** één kleine clientbound payload bij login/dimensiewissel/respawn en bij elke weerwissel (orde: een paar per uur per dimensie). Geen per-tick-sync: de client telt zelf af met dezelfde formule.
- **Client:** alles in `client/weather/` en de payload-handler (patroon change 18). Zonder geregistreerde renderers doet de hook niets zichtbaars behalve de F3-regel.
- **Multiplayer:** alle spelers in een dimensie krijgen dezelfde staat, dus zien hetzelfde weer; de ring per speler bepaalt de client zelf. Gameplay (later) leest alleen de server-staat.
- Geen worldgen, geen datagen, geen invloed op determinisme.

---

## Tests

**B** — `./gradlew build` slaagt, met nieuwe unit tests:
- `weather/RingWeatherCycleTest`: duur altijd binnen [min, max], zowel met de vanilla-standaardwaarden (12.000–180.000 / 12.000–24.000 ticks) als met korte testwaarden (5–10 s); na weer altijd `clear`, na `clear` alleen typen van die ring; gewicht 0 → nooit gekozen; ring zonder typen blijft `clear`; aftellen in stappen van 20; zelfde seed → zelfde reeks.
- `weather/RingWeatherStateTest`: `intensity()` (0 bij begin, 1 in het midden, 0 aan het eind, `clear` altijd 0, korte duur < 2 × fade); `CODEC`- en `STREAM_CODEC`-rondreis.
- `weather/RingWeatherDataTest`: opslaan/laden-rondreis; ontbrekende ring en onbekende typenaam → `clear`, geen exception.
- `config/ServerConfig` (weer): standaardwaarden per ring zijn 600/9000/600/1200 s en gewicht 1 per type; `ringWeather(Ring)` zet seconden correct om naar ticks (×20).
- `weather/RingWeatherTypeTest`: geserialiseerde namen uniek; elk type behalve `clear` hoort bij precies één ring 2–5 in de overworld; `typesFor(the_nether, …)` leeg.

**D** — n.v.t. (geen datagen-wijziging; lang blijft ongewijzigd: vaste Engelse tekst).

**S** — `tools/server-smoke-test.sh` met `level-name=smoke-change20`: start zonder client-class-fouten. Daarna met RCON:
- `ca weather` → vier ringen (2–5) met `clear` of een type en een resterende tijd; `execute in minecraft:the_nether run ca weather` → "No ring weather in this dimension".
- `ca weather 2 sandstorm 120` → bevestiging; `ca weather` → ring 2 `sandstorm`, ~120 s; `ca weather 2 eruption` → fout; `ca weather 1 sandstorm` → fout.
- `gamerule doWeatherCycle false` → twee keer `ca weather` met 10 s ertussen: resterende tijd gelijk; weer `true` → loopt af (vraag 5 → A).
- Standaardritme: na de eerste start staan in `config/corruptedadventure-server.toml` (globaal) per ring 600/9000/600/1200; `ca weather` direct na de start toont per ring `clear` met een resterende tijd tussen 600 en 9000 s (of een type met 600–1200 s).
- Cyclus (het vanilla-ritme is te traag om af te wachten): per-wereld-override `<wereld>/serverconfig/corruptedadventure-server.toml` (kopie van de globale file) met alle duren op 5–10 s → server herstarten → per ring `ca weather <ring> clear 1` (de config geldt pas vanaf het volgende weer) → `ca weather` een minuut pollen: elke ring wisselt `clear` ↔ type, nooit een type van een andere ring. Daarna de override weghalen.
- Persistentie: `ca weather 3 ash_rain 600`, `stop` via RCON, opnieuw starten → ring 3 nog `ash_rain` met ~dezelfde resterende tijd; `<wereld>/data/corruptedadventure_ring_weather.dat` bestaat, `<wereld>/DIM-1/data/` heeft er geen.
- Vanilla-preset (`level-type=minecraft:normal`, aparte `level-name`): `ca weather` → geen ringweer, geen `.dat`, geen fouten.
- Daarna RCON uit, server stoppen.

**Blokkerende handmatige test: JA — Teststop 1 (gebundeld 18 + 19 + 20, besloten 2026-10-01).** Sync en client-hook zijn alleen met echte clients te zien (RCON heeft geen client), en de weer-changes 28, 45, 66 en 85 bouwen direct op deze sync en hook. Eén teststop na change 20 voor 18 (ringdata op de client), 19 (HUD/meter) en 20 (ringweer), met een dedicated server (`runServer`, `corrupted_world`, seed `20260930`, nieuwe `level-name`) en **twee** clients (`runClient` als `Dev`, `runClient2` als `Dev2`, via quick-play uit change 18). De agent bereidt alles voor (server, clients, RCON, logcheck); de gebruiker loopt de drie delen af (~20 min):

*Deel 1 — change 18 (ringdata op de client):* handtest-stappen 1–3 uit `change-18.md` (Tests → Blokkerende handmatige test): join zonder fouten; F3 `CA ring (client)` gelijk aan `/ca ring` op spawn, grens 1, ring 3, Nether-grens 2 en void; singleplayer vanilla-preset → `—`. Grens 1 met de nieuwe ruis-seed even bekijken (18 vraag 1).

*Deel 2 — change 19 (meters/HUD):* de HUD-checklist uit `change-19.md` (Tests → Handmatig): eigen balk per speler, plek boven de hongerbalk, streepjes, verborgen met F1/spectator/creatief, correct na respawn en dimensiewissel, geen knipperende iconen.

*Deel 3 — change 20 (ringweer), checklist (seed `20260930`):*
- [ ] Beide clients in ring 2 (`/ca tpring 2`): F3 toont dezelfde regel (type, sterkte, resterende tijd binnen ~1 s gelijk).
- [ ] `/ca weather 2 sandstorm 60` door client A (expliciete duur; het vanilla-ritme hoeft niet afgewacht te worden) → F3 van A **en** B binnen 1 s bijgewerkt; sterkte loopt in ~30 s op naar 1 (vraag 4) en zakt aan het eind weer naar 0.
- [ ] Client B in ring 3 (`/ca tpring 3`): F3 toont het weer van ring 3, niet dat van ring 2.
- [ ] Grens 2→3 (`/ca tpboundary 2`), langzaam over de grens lopen: F3 toont het ruimtelijk gewicht volgens vraag 4 (A: twee ringen, vloeiend; B/C: harde wissel op de grens).
- [ ] Void-rand (`/ca tpring 6`): F3 toont het weer van ring 5.
- [ ] Client B naar de Nether (portal of `/ca tpring 1 nether`): F3 toont geen ringweer; terug → weer direct correct.
- [ ] Client B uitloggen en weer inloggen → zelfde staat; geen fouten in client- of serverlog.
- [ ] `/weather rain` in ring 1: vanilla regen zoals altijd, ringweer onveranderd (vraag 5 → A).

**W** — geen worldgen. Standaard portal-test niet nodig; de F3-checks hierboven vormen de in-game checklist.

**Checklist (concept)**
- [ ] Scope `atmosphere` benoemd
- [ ] B groen (incl. nieuwe tests); S groen (ook vanilla-preset); geen client-class op de server
- [ ] Teststop 1 (gebundeld 18 + 19 + 20, deel 1–3) door de gebruiker gedaan en akkoord
- [ ] `CODEBASE.md`, `fase-1-wereld.md` §1.6, beslissingenlog bijgewerkt
- [ ] `/code-review` gedaan, bevindingen afgehandeld
- [ ] Commit `atmosphere: add ring weather system with sync and client hook`

---

## Risico's

- **Change 18 wijkt af** (geen client-ringlayout, ander payload-patroon): bij de start tegen `CODEBASE.md` leggen. Zonder client-ringlayout kan de hook de ring van de speler niet bepalen → stoppen en vragen (alternatief zou een per-speler ring-tracking op de server zijn, met pop-in aan grenzen).
- **Client-class via de payload-registratie:** een methodreferentie naar een `client/`-handler in common code kan bij het linken de client-class laden. Precies het patroon van change 18 volgen en met de smoke-test bevestigen.
- **Payload-thread:** NeoForge 21.1 voert handlers standaard op de hoofdthread uit; bij de bouw controleren, anders `context.enqueueWork`.
- **Lucht- en neerslagkleur bestaan niet als NeoForge-event in 1.21.1** (`ComputeFogColor` wel, luchtkleur niet). Rode lucht (45), groene/omgekeerde regen (66, 85) en het verbergen van vanilla-regen vragen dan een eigen `DimensionSpecialEffects` of een mixin. Niet in deze change oplossen; de interface krijgt die methodes pas als een weer-change ze nodig heeft (eerste die het raakt noteert de keuze).
- **Client-aftelling loopt uit de pas** bij lag of een gepauzeerde singleplayer-server: hooguit seconden, en bij elke wissel komt een verse staat. Geaccepteerd.
- **Hernoemen van een enumwaarde** maakt opgeslagen staat ongeldig: valt terug op `clear` (getest). Namen dus pas hernoemen met een reden.
- **Sync-volume** is verwaarloosbaar; wel geen packet sturen naar spelers in dimensies zonder ringweer behalve één lege payload bij binnenkomst (wist de client-staat).
- **Typen bestaan vóór hun effect** (bij vraag 1 → A): tussen change 20 en 28/45/66/85 "regent" het onzichtbaar. F3 toont het; spelers merken niets.

---

## Open ontwerpvragen

### 1. Welke weertypes bestaan na change 20?
- **A — `clear` + alle 8 ontworpen typen, zonder effect (aanbevolen).** Namen liggen al vast in het design (zandstorm, hittegolf, asregen, uitbarsting, dikke mist, gifregen, glitchstorm, omgekeerde regen). De cyclus, config (gewichten per type) en sync zijn nu al volledig te testen; de weer-changes voegen alleen een renderer en gameplay toe. Nadeel: tot hun change draait er "onzichtbaar" weer (alleen in F3).
- **B — Alleen `clear` + één debug-type `test` dat de cyclus nooit kiest.** Geen onzichtbaar weer; de sync is testbaar via het commando. Elke weer-change voegt zijn typen + configsectie toe (meer werk per change), en het debug-type moet later weer weg of blijven als testtype.
- **C — Alleen `clear`.** Strikt "nog geen weertypes", maar de verplichte test (`/ca weather <ring> <type>` synct) is dan niet zinvol en de cyclus pas in change 28 echt te testen.

Antwoord: **A** — `clear` + alle 8 ontworpen typen (`sandstorm`, `heat_wave`, `ash_rain`, `eruption`, `dense_fog`, `toxic_rain`, `glitch_storm`, `reverse_rain`), zonder effect tot hun eigen change.

### 2. Hoe definiëren we een weertype in code?
- **A — Java-enum `RingWeatherType` met ring en dimensie per waarde (aanbevolen).** Eenvoudig, type-veilig, makkelijk in `switch`/`EnumMap`, codec op naam. Nieuwe typen = nieuwe enumwaarde. Geen uitbreiding door datapacks of andere mods (niet nodig in dit project).
- **B — Eigen NeoForge-registry (`corruptedadventure:ring_weather`, gesynct).** Uitbreidbaar door andere mods, registry-id's over het netwerk; meer boilerplate (registry, sync, `DeferredRegister`) zonder concrete winst nu.
- **C — Datapack-JSON per type** (ring, duur, gewicht in data). Instelbaar zonder code, maar dubbel met "cyclus uit config" (design §1.6) en de effecten blijven toch Java.

Antwoord: **A** — Java-enum `RingWeatherType` met ring en dimensie per waarde; codec op naam.

### 3. Standaardritme van de cyclus (config, per ring instelbaar)?
- **A — Helder 20–60 min, weer 5–10 min (aanbevolen).** Een speler die een avond in ring 2 speelt, maakt 2–4 weerbuien mee; condities met ×1,5/×2 blijven een gebeurtenis, geen dauerzustand. Gewichten per type standaard gelijk.
- **B — Vanilla-ritme: helder 10 min–2,5 uur, weer 10–20 min.** Vertrouwd en zeldzaam; kans dat je een ring doorkruist zonder het weer ooit te zien, terwijl het een kernkenmerk van de ring is.
- **C — Vaak: helder 5–15 min, weer 2–5 min.** Veel afwisseling; kan onrustig en uitputtend worden zodra condities ×2 gaan, en grensmuur-/zichteffecten wisselen vaak.

Antwoord: **B (keuze gebruiker)** — vanilla-ritme: helder 12.000–180.000 ticks (zoals vanilla's rain-delay; 0,5–7,5 MC-dag = 10 min–2,5 uur speeltijd; bevestigd door de gebruiker 2026-10-01), weer 12.000–24.000 ticks (zoals vanilla's regenduur; 10–20 min = 0,5–1 MC-dag); per ring instelbaar in de SERVER-config; gelijke gewichten per type. Config-standaardwaarden: `clearMinSeconds` 600, `clearMaxSeconds` 9000, `weatherMinSeconds` 600, `weatherMaxSeconds` 1200.

### 4. Overgang in tijd en ruimte (rendering; gameplay wisselt altijd hard op de ringgrens)?
In alle opties: gameplay (condities, later) gebruikt de ring van `ringAt(…).ring()` (wissel op 0,5, zoals de biome); de void-rand volgt het weer van ring 5.
- **A — Vloeiend: 30 s fade-in/-out in de tijd + menging over de overgangszone (aanbevolen).** Sterkte = tijd-fade × smoothstep-gewicht over de zone (~128 blokken): een zandstorm "hangt" boven ring 2 en dunt uit richting ring 3. Natuurlijk gevoel, past bij de grensmuren in fase 5; de hook rekent twee ringen tegelijk (al voorzien).
- **B — Tijd-fade 30 s, ruimtelijk hard op de grens.** Eenvoudiger; aan de grens stap je abrupt een storm in of uit (zichtbare "muur" van fog).
- **C — Alles hard (geen fade).** Eenvoudigst; weer springt aan/uit, ook bij een wissel vlak voor je neus. Voelt goedkoop.

Antwoord: **A** — vloeiend: 30 s fade-in/-out in de tijd + smoothstep-menging over de overgangszone; gameplay wisselt hard op 0,5, void-rand volgt ring 5.

### 5. Verhouding met vanilla-weer?
- **A — Onafhankelijk, wel `doWeatherCycle` volgen (aanbevolen).** Vanilla regen/onweer en `/weather` blijven ongewijzigd (zichtbaar alleen in biomes met neerslag: ring 1, deels ring 4); het ringweer heeft een eigen cyclus die stilstaat als `doWeatherCycle` uit staat (vertrouwd voor admins en handig bij testen). Of gifregen in ring 4 vanilla-regen lokaal vervangt, beslist change 66.
- **B — Volledig onafhankelijk, ook van de gamerule.** Ringweer loopt altijd; admins kunnen het alleen per ring met `/ca weather` stilzetten (lange `clear`).
- **C — Gekoppeld: `/weather clear` wist ook het ringweer, vanilla-onweer forceert in ring 2–5 een ringweertype.** Eén knop voor admins, maar vermengt twee systemen; vanilla-weer (één staat per wereld) zou alle ringen tegelijk sturen, tegen "eigen cyclus per ring" in.

Antwoord: **A** — onafhankelijk van vanilla-weer, maar de cyclus staat stil als `doWeatherCycle` uit staat.

---

## Beslissingenlog-regels (bij de bouw invullen met de datum van vandaag)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | atmosphere | Ringweer-typen na change 20: `clear` + alle 8 ontworpen typen, zonder effect tot hun eigen change | Cyclus, config en sync nu al volledig testbaar; weer-changes voegen alleen renderer en gameplay toe |
| 2026-10-01 | atmosphere | Weertype = Java-enum `RingWeatherType` (ring + dimensie per waarde), opslag/netwerk op naam | Eenvoudig en type-veilig; geen externe uitbreiding nodig |
| 2026-10-01 | atmosphere | Standaardritme ringweer = vanilla-ritme (keuze gebruiker): helder 12.000–180.000 ticks, weer 12.000–24.000 ticks, gelijke gewichten; per ring in de SERVER-config, live gelezen (geen snapshot) | Vertrouwd en zeldzaam zoals vanilla-regen; wijziging geeft geen breuklijnen |
| 2026-10-01 | atmosphere | Weerovergang: 30 s fade in de tijd + smoothstep-menging over de overgangszone; gameplay wisselt hard op de ringgrens (0,5), void-rand volgt ring 5 | Natuurlijk gevoel, geen fog-muur op de grens |
| 2026-10-01 | atmosphere | Ringweer t.o.v. vanilla-weer: onafhankelijk, cyclus volgt `doWeatherCycle` | Vanilla blijft vertrouwd; admins en tests kunnen het weer stilzetten |
| 2026-10-01 | atmosphere | Sync: elke speler krijgt de volledige ringweerstaat van zijn dimensie (login, dimensiewissel, respawn, elke wissel); de client telt zelf af en kiest zelf de ring | Geen per-tick-packets, geen pop-in aan grenzen, basis voor grensmuren |
| 2026-10-01 | atmosphere | Geen Nether-ringweer: systeem per dimensie generiek, maar alleen dimensies met typen krijgen staat en `SavedData` (nu alleen de overworld) | Volgt het design; geen lege bestanden of ticks |

---

## Bouwnotities (change 20, 2026-10-01)

- Aannames over 18/19 klopten: `RingWorld.isActive`, `ClientRingLayout`, `ClientboundHandlers` (handler in `client/network/`), `RingDebugOverlay` (enige F3-hook), `ServerConfig`. `PROTOCOL_VERSION` `"2"` → `"3"` (nieuwe payload).
- **Afwijking config-locatie** (zelfde als 19): de SERVER-config staat standaard globaal in `config/corruptedadventure-server.toml`; `<wereld>/serverconfig/` is een override per wereld. Tekst in Plan §2 en Tests S aangepast; de cyclus-test gebruikte een override in `run/smoke-change20/serverconfig/`.
- **Extra veld in de payload:** `cycleRunning` (stand van `doWeatherCycle`). Nodig omdat de client zelf aftelt: zonder dat veld zou de client doortellen terwijl de server stilstaat. De server stuurt de staat opnieuw als de gamerule omslaat (gecontroleerd elke 20 ticks). Gevolg van vraag 4 + 5: met `doWeatherCycle false` staat ook de fade stil (een net gezet weer blijft op intensiteit 0).
- **`/tick freeze`:** de server-cyclus loopt alleen als `tickRateManager().runsNormally()` (anders zou `gameTime % 20` elke tick waar zijn).
- **Afwijking client-aftelling (na review):** niet "lokaal afgeteld per client-tick" (plan §7), maar uit de game time: de payload draagt de game time van het level bij verzending, de client trekt het verschil met de game time van zijn level af. Reden: met eigen ticks loopt de client bij serverlag voor (weer op de client voorbij terwijl gameplay het nog toepast). Payloadformaat daarmee: dimensie, game time, `cycleRunning`, staat per ring (nog steeds protocol `"3"`, niet eerder uitgebracht).
- **`/ca weather 1 clear`** geeft ook een fout ("ring_1 has no ring weather"), niet alleen ring 1 met een weertype: ring 1 heeft geen staat om te zetten.
- **Ruimtelijke menging** staat als pure common-klasse `weather/RingWeatherMix` (getest, `RingWeatherMixTest`); de client-hook en later de grensmuren gebruiken hem. `RingWeather.weatherRing` (void → ring 5) is de gedeelde regel.
- **Extra tests** naast het plan: `RingWeatherMixTest`, `network/RingWeatherPayloadTest`; de configtest heet `config/RingWeatherConfigTest`.
- `seed` wordt alleen bij een weertype getrokken; `clear` heeft seed 0.
- F3-formaat: `CA weather (client): ring_2 sandstorm 0.83 (412 s)`; in een overgangszone `ring_2 sandstorm 0.40 (weight 0.48, 412 s) | ring_3 clear (weight 0.52, 1200 s)`; ring 1 `ring_1 none`; geen layout of geen ringweer in deze dimensie (Nether) `—`; met `doWeatherCycle false` `(cycle paused)` erachter. De sterkte = intensiteit × ruimtelijk gewicht.
- `run/ops.json` (niet in git) bevat nu `Dev` en `Dev2` (niveau 4), zodat beide clients bij de teststop `/ca …` kunnen gebruiken; `run/server.properties` staat op `level-name=teststop1` (verse wereld), RCON uit.

### Testresultaten
- **B:** `./gradlew build` groen, 166 tests (was 122; nieuw: `RingWeatherTypeTest`, `RingWeatherStateTest`, `RingWeatherCycleTest`, `RingWeatherDataTest`, `RingWeatherMixTest`, `RingWeatherPayloadTest`, `RingWeatherConfigTest`); `ClientIsolationTest` groen.
- **D:** n.v.t. (geen datagen).
- **S:** `tools/server-smoke-test.sh` met `level-name=smoke-change20`: `Done (5.9 s)`, geen fouten. Daarna RCON:
  - `ca weather` → ring 2–5 `clear` met 1644–6391 s (binnen 600–9000); Nether → "No ring weather in this dimension".
  - `ca weather 2 sandstorm 120` → gezet; `ca weather 2 eruption` → "eruption is not a weather of ring_2; use one of: clear, sandstorm, heat_wave"; `ca weather 1 sandstorm` en `ca weather 1 clear` → "ring_1 has no ring weather"; zonder seconden: `glitch_storm` 1195 s (600–1200).
  - Aftellen 10 s → −10 s, intensiteit 0,17 → 0,50 (fade 30 s); `doWeatherCycle false` → 2× 10 s later gelijk ("cycle paused"); `true` → loopt weer. `dense_fog 30` (korter dan 2 × fade) piekte op 0,50 en werd daarna `clear` met een nieuwe duur.
  - Persistentie: `ca weather 3 ash_rain 600`, RCON `stop`, herstart → `ash_rain 594 s of 600`; `data/corruptedadventure_ring_weather.dat` bestaat, `DIM-1/data/` heeft er geen.
  - Cyclus (override 5–10 s + per ring `clear 1`, 15× pollen in 60 s): elke ring wisselde `clear` ↔ eigen type (ring 2: sandstorm/heat_wave, 3: ash_rain/eruption, 4: dense_fog, 5: glitch_storm), nooit een vreemd type, duren 5–10 s. Override daarna verwijderd.
  - Vanilla-preset (`smoke-change20-vanilla`): `ca weather` → "No ring weather: this is not a ring world", geen `.dat`, geen fouten.
  - Twee clients (`runClient -PquickPlay`, `runClient2 -PquickPlay`): beide joinen en krijgen bij login layout + ringweer (DEBUG `Received ring weather for Dev in minecraft:overworld (cycle running): [ring_2=sandstorm 920/2400, …]`). `ca weather 2 sandstorm 60` → beide in dezelfde seconde `ring_2=sandstorm 1200/1200`. Dev2 naar de Nether → `[]`; terug → volledige staat. `doWeatherCycle false/true` → beide `cycle paused` / `cycle running`. `kill Dev` (immediate respawn) → Dev kreeg de staat opnieuw. Dev2 in de Nether kreeg geen overworld-updates. Geen fouten in client- of serverlog (alleen de bekende log4j-rotatiemelding).
  - Coördinaten seed 20260930 richting oost (na `tp @s 100 ~ 0`): ring 2 midden 2499 94 12, grens 1 1469 73 7, grens 2 3457 107 17, ring 3 midden 4499 121 22, void 9749 100 48, Nether-grens 2 864 43 4, ring 1 midden 749 63 3.
  - RCON daarna uit, server gestopt.

## In-game checklist (W) — teststop 1

**Blokkerend: change 21 begint pas na akkoord.** Gebundeld voor 18 (ringdata op de client), 19 (meters/HUD) en 20 (ringweer). ~20 min. Wereld: `corrupted_world`, seed `20260930`, verse map `teststop1` (staat al in `run/server.properties`). `Dev` en `Dev2` zijn op (`run/ops.json`).

**Opstarten** (drie terminals in de repo-root):
1. `./gradlew runServer` — wacht op `Done (…)`.
2. `./gradlew runClient -PquickPlay` — client **Dev** joint vanzelf `localhost:25565`.
3. `./gradlew runClient2 -PquickPlay` — client **Dev2** joint vanzelf.
4. Op beide clients F3 aan; rechts staan `CA ring (client): …` en `CA weather (client): …`.
5. Stoppen: `/stop` in de chat van Dev, daarna de clients sluiten.

Vaste coördinaten: eerst `/tp @s 100 ~ 0` (richting vanaf 0,0 = oost), dan landen de `/ca tp…`-commando's op de punten hieronder (±1 blok).

### Deel 1 — change 18: ringdata op de client
- [ ] Beide clients joinen zonder disconnect; F3 bij spawn: `CA ring (client): ring_1`; `/ca ring` zegt `Ring 1`.
- [ ] Dev: `/gamemode creative`, `/tp @s 100 ~ 0`, dan F3 vergelijken met `/ca ring` (zelfde ring, gewicht ±0,01) op:
  - `/ca tpboundary 1` → ~1469 73 7: `ring_1 → ring_2 @ 0.50`;
  - `/ca tpring 3` → ~4499 121 22: `ring_3`;
  - `/ca tpboundary 2 nether` → ~864 43 4 (Nether): `ring_2 → ring_3 @ 0.50`;
  - `/ca tpring 6` → ~9749 100 48: `void`.
- [ ] Grens 1 (`/ca tpboundary 1`): even rondkijken; de overgang (withered expanse) ziet er normaal uit met de nieuwe ruis-seed.
- [ ] Dev: Disconnect → Singleplayer → nieuwe wereld met World Type **Default** → F3 `CA ring (client): —` en `CA weather (client): —`. Daarna een singleplayer-wereld met World Type **Corrupted World** → beide regels zijn er weer en `CA ring` klopt met `/ca ring`. Dan Dev terug naar de server (Multiplayer → Direct Connection → `localhost`).

### Deel 2 — change 19: meters en HUD (Dev in survival, `/difficulty normal`)
- [ ] `/ca condition debug Dev rise` → rechts boven de hongerbalk een paarse balk (even breed als de hongerbalk) die in ~20 s volloopt; streepjes op 50/75/100 %. Bij 50 traagheid-, bij 75 zwakte-icoon (geen particles, **knipperen niet**), bij 100 elke seconde een half hartje schade.
- [ ] `/ca condition debug Dev fall` → zwakte weg onder 75, traagheid onder 50, balk weg bij 0.
- [ ] Luchtbellen: `/ca condition set Dev debug 60`, `/ca condition debug Dev rise`, `/effect give Dev minecraft:resistance 120 4 true`, `/fill ~-1 ~ ~-1 ~1 ~2 ~1 minecraft:water replace minecraft:air`, kopje onder → bellen direct boven de hongerbalk, conditiebalk erboven; boven water schuift hij terug.
- [ ] F1 → balk weg. `/gamemode creative` → balk en effecten weg, meter stijgt niet (`/ca condition get Dev`). `/gamemode spectator` → weg. `/gamemode survival` → direct terug.
- [ ] Met meter 100 (+ resistance): `/ca tpring 1 nether` → balk staat **direct** vol na het laden; `/ca tpring 1` terug → idem. `/kill Dev` → na respawn geen balk, geen effecten.
- [ ] Per speler: `/ca condition set Dev2 debug 60` → alleen Dev2 ziet een balk + traagheid; `/ca condition debug Dev rise` → Dev2 ziet niets van Dev.
- [ ] Persistentie: `/ca condition set Dev debug 40`, `/ca condition debug Dev rise`, `/gamemode creative`, Dev uitloggen en opnieuw joinen, `/gamemode survival` → balk direct op ~40, zakt daarna.
- [ ] Drankje: `/effect give Dev minecraft:slowness 60 0`, `/ca condition set Dev debug 60`, `/ca condition debug Dev fall` → onder 50 blijft het traagheid-drankje (met particles) staan.
- [ ] Opruimen: `/ca condition debug @a off`, `/ca condition set @a debug 0`, `/effect clear @a`.

### Deel 3 — change 20: ringweer (beide in creatief: `/gamemode creative @a`)
- [ ] Dev: `/tp @s 100 ~ 0`, `/ca tpring 2` (~2499 94 12); Dev2: `/tp Dev2 Dev`. F3 op beide: `CA weather (client): ring_2 clear (N s)`, N gelijk (±1 s) en gelijk aan `/ca weather`.
- [ ] Ring 1 (`/ca tpring 1`, ~749 63 3): F3 `ring_1 none`. Terug met `/ca tpring 2`.
- [ ] Dev: `/ca weather 2 sandstorm 60` → F3 van Dev **en** Dev2 binnen 1 s `ring_2 sandstorm 0.00 (60 s)`; de sterkte loopt in 30 s op naar `1.00`, zakt in de laatste 30 s naar `0.00`; daarna op beide `ring_2 clear (…)` met een nieuwe duur.
- [ ] Dev2: `/ca tpring 3` (~4499 121 22) → F3 van Dev2 toont `ring_3 …` (zelfde als ring 3 in `/ca weather`), niet ring 2; Dev ziet nog ring 2.
- [ ] Overgang: `/ca weather 2 sandstorm 600`, 30 s wachten, Dev `/ca tpboundary 2` (~3457 107 17), langzaam naar het oosten lopen tot het tweede deel verdwijnt, en terug: F3 toont `ring_2 sandstorm S (weight W, …) | ring_3 … (weight 1−W, …)`; W loopt vloeiend van ~1 naar ~0 over ~128 blokken, S = W; buiten de zone één deel.
- [ ] Void: `/ca weather 5 glitch_storm 300`, Dev `/ca tpring 6` (~9749 100 48) → F3 `ring_5 glitch_storm …` (void volgt ring 5).
- [ ] Dev2: `/ca tpring 1 nether` → F3 `CA weather (client): —`; `/ca tpring 2` terug → direct de juiste ring 2-regel.
- [ ] Dev2 uitloggen en opnieuw joinen → zelfde staat in F3 en `/ca weather`.
- [ ] `/gamerule doWeatherCycle false` → F3 op beide `… (cycle paused)`, resterende tijd staat stil; `/gamerule doWeatherCycle true` → loopt weer.
- [ ] Vanilla-weer: beide `/ca tpring 1`, `/weather rain` → gewone vanilla-regen; `/ca weather` en F3 (`ring_1 none`) onveranderd. Daarna `/weather clear`.
- [ ] Geen fouten of disconnects in de drie terminals.

### Code review (`/code-review medium`)
Eén bevinding (medium): de client telde af op zijn eigen 20 TPS terwijl de server alleen bij een wissel synct, dus bij serverlag liep het weer op de client voor (eerder afgelopen dan op de server, gameplay paste het nog toe). **Opgelost:** `RingWeatherPayload` draagt de game time bij verzending; `ClientRingWeather.states(level)` rekent `ontvangen − (level.getGameTime() − gameTime)`. Build (166 tests) en een join-hercontrole (server + `Dev`, `ca weather 2 heat_wave 90` → client ontving `heat_wave 1800/1800`) daarna groen. Verder geen bevindingen.
