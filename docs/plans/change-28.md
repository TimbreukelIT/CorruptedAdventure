# Change 28 — Ring 2-weer: zandstorm en hittegolf

Scope: `atmosphere` — change 28 (de koppeling hittegolf → uitdroging ×2 raakt `conditions` minimaal; zie "Scope en de koppeling met `conditions`")

**Doel:** de twee ringweertypen van ring 2 (`fase-1-wereld.md` §2 Ring 2, "Weer (ringweer)") krijgen hun effect, gebouwd op het ringweer-systeem van change 20:
- **Zandstorm:** het zicht wordt sterk beperkt (fog in de client, particles), en een lichte wind duwt spelers (beslist door de server).
- **Hittegolf:** de lucht trilt (client), en uitdroging loopt 2× sneller op (server, via de rate-pijplijn van change 19/27).

Testcriterium uit het bouwplan: B, S, en het effect is op twee clients tegelijk gelijk.

---

## Afstemming (2026-10-01)

Afgestemd met 18, 19, 20, 27, 32, 36 en 37 (basis: de aanbevolen opties). Waar de tekst hieronder iets anders zegt, geldt dit blok.
- **`seed` zit al in `RingWeatherState` (change 20):** §1 wijzigt de staat niet meer; `PROTOCOL_VERSION` gaat alleen omhoog voor `WindGustPayload`. De tests "NBT zonder `seed`" en "nieuw weer krijgt een `seed`" horen bij 20.
- **Config:** `[weather.ring2]` in `config/ServerConfig` (SERVER, per wereld, live; 19/20). Vraag 7 is daardoor routine: A en B verschillen alleen in de sectie.
- **Modifier-registratie via `ConditionDefinitions.addModifier` (19):** `conditions` blijft onaangeroerd. De backlogregel "[conditions] Ringweer-modifiers … met één regel in de pijplijn" wordt alleen nog een volgorde-notitie voor change 43.
- **`registry/ModParticleTypes` en `datagen/ModParticleDescriptionProvider` worden hier aangemaakt;** 36 (vraag 4 B) en 37 (vraag 7) breiden ze uit.
- **Fog: basis vs. overlay.** Biome-kleuren zijn van change 32; de zandstorm- en hittegolfkleur zijn constanten in deze change (overlay, gemengd met `strength × exposure`). 37 levert alleen de `sand_gust`-sprite.
- **Twee clients:** `client2`/`Dev2` en quick-play komen uit change 18. F3: extra regel in `RingDebugOverlay` (18) naast de weer-regel van 20.
- **Geluid (vraag 8 A):** vanilla-verwijzing, geen audiobestand; de synthese-pijplijn van change 32 (`Ring2Sounds.java`) kan de wind later vervangen (backlog, geen blokkade).
- **Teststop (besloten 2026-10-01):** W-achteraf in de gebundelde eindcheck ring 2 + Nether-2 (teststop 3) ná change 37 (ruim vóór change 45). Tests gebruiken `/ca weather 2 <type> <seconden>`; het vanilla-ritme van het ringweer (20 vraag 3 = B) hoeft niet afgewacht te worden.

---

## Afhankelijkheden en aannames

Changes 18–27 zijn bij het schrijven van dit plan **nog niet gebouwd**. Hun plannen zijn de bron. Alles met *(aanname)* leg je bij de start naast `CODEBASE.md` en de echte code. Wijkt de code af, dan pas je dit plan aan (niet andersom).

- **Change 18 (`infra`)** *(aanname, namen uit `docs/plans/change-18.md`)*:
  - `network/ModNetwork` met `PROTOCOL_VERSION` (ophogen bij elke nieuwe of gewijzigde payload) en één registratieregel per payload.
  - `network/ClientboundHandlers` (`set`/`forward`): de handler staat in `client/network/` en wordt gekoppeld in `CorruptedAdventureClient`. Common code noemt nooit een `client/`-class.
  - `ring/RingWorld.isActive(server)`: alleen in een ringwereld.
  - `network/RingPacketDistributor` met `RingMembership.CORE`/`TOUCHING`, `matches(...)` en `playersInRing(level, ring, membership)`. Het plan van 18 noemt de wind-duw van 28 al als gebruiker van `CORE`.
  - `client/ClientRingLayout`: client-code gebruikt nooit `RingLayout.current()`.
  - `ClientIsolationTest` bewaakt de client/server-scheiding.
  - **Tweede dev-client** (`client2`, `--username Dev2`; aanname uit change 19). Zonder die client kan de S-test met twee clients niet: stop dan en vraag de hoofdsessie.
- **Change 19 (`conditions`)** *(aanname)*:
  - `Condition` en `ConditionContext` (record: `ServerPlayer`, `ServerLevel`, `RingPosition`, `Holder<Biome>`, met lui berekende checks zoals `canSeeSky()`).
  - `RateModifier.modify(Condition, ConditionContext, float rate)` en `ConditionDefinitions.modifiers()`. De modifiers draaien op volgorde ná de regel.
  - `config/ServerConfig` (SERVER, per wereld).
  - `/ca condition get/set`.
- **Change 20 (`atmosphere`)** *(aanname, namen uit `docs/plans/change-20.md`)*:
  - `weather/RingWeatherType` met `SANDSTORM` (`sandstorm`) en `HEAT_WAVE` (`heat_wave`). Dat geldt als vraag 1 van 20 → A is beantwoord. Bij B/C voegt **deze** change de twee typen toe, plus de configsectie `[weather.ring2]` (zelfde categorie).
  - `RingWeatherState(type, remainingTicks, durationTicks)` met `intensity()` (fade in de tijd), `CODEC` en `STREAM_CODEC`.
  - `RingWeatherCycle`, `RingWeatherSettings`, `RingWeatherData` (`SavedData`) en `RingWeather` (`stateAt(ServerLevel, x, z)`, `state(ServerLevel, Ring)`, `set(...)`).
  - `network/RingWeatherPayload` en `command/RingWeatherCommand` (`/ca weather`).
  - Aan de client-kant: `client/weather/ClientRingWeather` en de interface `RingWeatherRenderer` met de default-methodes `clientTick(ClientLevel, LocalPlayer, strength)`, `renderFog(ViewportEvent.RenderFog, strength)`, `fogColor(ViewportEvent.ComputeFogColor, strength)` en `renderLevel(RenderLevelStageEvent, strength)`.
  - Verder `RingWeatherRenderers.register(type, renderer)` en `RingWeatherClientHooks`. De hooks rekenen per frame `strength` = `intensity()` × ruimtelijk gewicht (vraag 4 van 20) en tonen de F3-regel `Ring weather: …`.
  - Gameplay wisselt hard op de ringgrens (`ringAt(...).ring()`, 0,5).
- **Change 27 (`conditions`)** *(aanname)*:
  - `Condition.DEHYDRATION` en `DehydrationRule`. Die regel geeft **alleen** in `SUN` (direct zonlicht, overdag, ring 2) een positieve rate, zodat een modifier op `rate > 0` precies de opbouw verdubbelt.
  - `/ca condition get` toont de rate ná alle modifiers en het label (bijv. `+0.40/s, sun`).
  - De "schaduw"-definitie (vraag 2 van 27) hergebruikt deze change voor de blootstelling aan de wind (vraag 3 hieronder).
  - Change 27 zegt: "28 voegt de weerstaat aan `ConditionContext` toe". **Dit plan doet dat bewust niet**, om binnen de scope te blijven. Zie hieronder.
- **Bestaand (gecontroleerd in `CODEBASE.md`):**
  - `RingLayout`/`RingPosition`, `Ring.RING_2` en `CaCommand`.
  - `ModSoundEvents` en `ModSoundDefinitionsProvider`, met als patroon een eigen event dat verwijst naar een vanilla-geluid. Alleen nodig bij vraag 8 → A/B.
  - `DataGenerators`, `EnglishLanguageProvider` en `DutchLanguageProvider`.
  - `client/` bevat nu alleen `CorruptedAdventureClient`.
- **Geen Nether-ringweer** (beslissingenlog en change 20). In de Nether doet deze change niets. In werelden met de vanilla-preset ook niet (`RingWorld.isActive`).

---

## Scope en de koppeling met `conditions`

Deze change valt onder `atmosphere`: weer, fog, particles en (optioneel) geluid. Het design legt de verdubbeling van uitdroging bij het **weer** ("hittegolf: uitdroging loopt 2× sneller"). Zo houden we de ingreep in `conditions` minimaal:

- De modifier **`weather/HeatWaveDehydrationModifier implements RateModifier`** staat in het package `weather/` en valt dus onder `atmosphere`. Hij leest de weerstaat zelf via `RingWeather.state(ctx.level(), ctx.position().ring())`. Daarvoor is geen nieuw veld in `ConditionContext` nodig en geen wijziging in `DehydrationRule`.
- **Enige aanraking van `conditions`:** de modifier wordt in de pijplijn gehangen.
  - Heeft change 19 een registratie-API (bijv. `ConditionDefinitions.addModifier(RateModifier)`), dan doet `RingWeather.register` dat. `conditions` blijft dan onaangeroerd.
  - Zo niet, dan komt er **één regel** bij in `ConditionDefinitions.modifiers()`, plus een backlog-notitie.
- De configsleutel voor de vermenigvuldiger staat volgens vraag 7 in de weer-sectie. Bij B komt hij in de conditions-config, dan wel met een backlog-notitie.
- Backlog-regel (altijd): `- [conditions] Ringweer-modifiers (28 hittegolf; later 45 asregen/uitbarsting, 66 gifregen, 85) hangen elk met één regel in de condities-pijplijn. Bij change 43 (hitte) nagaan of een registratie-API of een vaste volgorde (weer vóór gear, zuiverend als laatste) nodig is. (bron: change 28)`

De wind-duw is weer-gameplay. Hij staat in `weather/` en raakt de condities niet.

---

## Plan

### 1. Gedeelde weerdata (common, `weather/`)

- **Windrichting** (vraag 2). Aanbevolen: A, een richting per storm, vastgelegd in de weerstaat:
  - **Afgestemd 2026-10-01:** het veld `int seed` zit **al sinds change 20** in `RingWeatherState` (getrokken bij elk nieuw weer en door `/ca weather`, in `CODEC` en `STREAM_CODEC`). Deze change gebruikt het alleen; het staatformaat verandert hier niet. `PROTOCOL_VERSION` gaat in deze change alleen omhoog voor de nieuwe `WindGustPayload`.
- **`weather/SandstormWind`** (common, puur rekenwerk plus de server-tick):
  - `static Vec3 direction(RingWeatherState state)`: een horizontale eenheidsvector, afgeleid van `seed`. Server (duw) en client (particles, F3) roepen **dezelfde** functie aan, dus twee clients zien dezelfde richting.
  - `static Vec3 impulse(direction, strength, intensity, exposure)`: pure functie, getest.
  - De server-tick volgt in §3.

### 2. Zandstorm in de client (`client/weather/`, alleen `Dist.CLIENT`)

- **`SandstormRenderer implements RingWeatherRenderer`**, geregistreerd in `CorruptedAdventureClient` via `RingWeatherRenderers.register(SANDSTORM, …)`:
  - `renderFog`:
    - Afstanden: near van vanilla → 0, far van vanilla → `SANDSTORM_FOG_FAR` (vraag 4), gemengd met `strength × exposure` (vraag 3).
    - `event.setCanceled(true)` is nodig om de nieuwe afstanden toe te passen; verifiëren in NeoForge 21.1.
    - Geldt voor `FOG_TERRAIN` én `FOG_SKY`, zodat de lucht in het zand opgaat.
    - Niet als de camera in een vloeistof zit of de speler blindness/darkness heeft. Dan blijft vanilla-fog staan. Dat is een technische keuze, geen ontwerpkeuze.
  - `fogColor`: mengen naar zandkleur (constante `#C9A46B` van deze change, af te stemmen in de W-achteraf; afgestemd 2026-10-01: change 32 zet alleen de biome-basiskleuren, de weerkleur blijft hier, change 37 levert alleen een sprite) met dezelfde factor.
  - `clientTick`:
    - Aantal: `SANDSTORM_PARTICLES_PER_TICK × strength × exposure` (vraag 4).
    - Plek: willekeurig in een kader van ±16 blokken rond de camera, alleen boven de `MOTION_BLOCKING`-heightmap (dus niet in huizen of grotten).
    - Snelheid: windrichting × windsnelheid, plus een beetje ruis.
    - Via `level.addParticle`, zodat de vanilla-instelling "Particles: Decreased/Minimal" vanzelf meetelt.
- **Particle `corruptedadventure:sand_gust`**:
  - `registry/ModParticleTypes`: een `DeferredRegister` op `Registries.PARTICLE_TYPE`, met `SAND_GUST` als `SimpleParticleType`. Common registratie (standaard; een dedicated server laadt hierdoor geen client-class). Aanhaken in `CorruptedAdventure`.
  - `client/particle/SandGustParticle` (`TextureSheetParticle` + `Provider`):
    - Vrijwel horizontaal, korte levensduur (~20–40 ticks), zandkleurig getint, met `hasPhysics` zodat hij op muren stopt.
    - Registratie via `RegisterParticleProvidersEvent` in `CorruptedAdventureClient`.
  - `datagen/ModParticleDescriptionProvider` (NeoForge `ParticleDescriptionProvider`) maakt `assets/corruptedadventure/particles/sand_gust.json` met de **vanilla-sprites** `minecraft:generic_0` … `generic_7`. Er komt dus geen nieuwe PNG en de categorie `textures` blijft buiten schot. Een eigen sprite (zandkorrels) gaat naar de backlog voor change 37.
- **`client/weather/SkyExposure`**, gedeeld door beide renderers (vraag 3 → A):
  - Blootstelling 0–1 = hemellicht op camerapositie ÷ 15, gladgestreken over ~2 s, zodat je bij het binnenlopen geleidelijk overgaat.
  - Berekend één keer per client-tick.
- **F3-regel** (de regel van change 20 uitbreiden): bij een zandstorm ook `wind <graden>° (from SW)` en `exp 0.85`. Zo kun je beide clients en `/ca weather` naast elkaar leggen.
- **Log op DEBUG** bij elke wissel van actieve typen of windrichting, bijv. `[ring weather] sandstorm strength 1.00 wind 225`. Dit is de geautomatiseerde vergelijking tussen twee clients in test S.

### 3. Wind-duw (server, common, `weather/SandstormWind`)

Techniek volgens vraag 1. Aanbevolen: A, additieve windstoten via een eigen payload.
- Een game-bus-listener op `PlayerTickEvent.Post`, alleen voor `ServerPlayer` (geen `FakePlayer`), met de spreiding `player.tickCount % SANDSTORM_GUST_TICKS == 0`. Hij doet alleen iets als:
  - `RingWorld.isActive`, de overworld, en
  - `RingPacketDistributor.matches(ringAt(...), RING_2, CORE)` (harde grens, zoals alle gameplay sinds change 20), en
  - de staat van ring 2 = `SANDSTORM` met `intensity() > 0`, en
  - de speler is niet in creatief of spectator, en geen passagier (de opties van vraag 1 gaan daar allemaal van uit).
- Blootstelling op de server (vraag 3 → A): duwen alleen als de speler de hemel ziet op ooghoogte, met dezelfde definitie als "direct zonlicht" bij uitdroging (antwoord op vraag 2 van change 27). Gewone `canSeeSky`: aan of uit, niet gladgestreken.
- Impuls = `direction × pushStrength × intensity` (vraag 6: dezelfde tijdsregel als de ×2). Hij gaat via **`network/WindGustPayload`** (`corruptedadventure:wind_gust`, clientbound, `record(float x, float z)`, `StreamCodec` met twee `FLOAT`s) naar die ene speler (`PacketDistributor.sendToPlayer`).
- Client-handler **`client/network/WindGustHandler`** volgens het patroon van 18, gekoppeld met `ClientboundHandlers.set(...)`: `context.enqueueWork(() -> context.player().push(x, 0, z))`. Dat is additief, zoals vanilla's knockback bij explosies (`ClientboundExplodePacket`), en overschrijft de beweging van de speler dus niet.
- Kosten: per blootgestelde speler één klein pakketje per `SANDSTORM_GUST_TICKS` (standaard 10 → 2/s), alleen tijdens een zandstorm.

### 4. Hittegolf in de client (`client/weather/`)

Techniek volgens vraag 5. Aanbevolen: A, een post-processing-shader plus warme waas.
- **`HeatWaveRenderer implements RingWeatherRenderer`**:
  - `fogColor`: lichte menging naar warm wit-geel.
  - `renderFog`: far × ~0,85 (lichte waas; geldt in alle opties).
  - `renderLevel`: vraagt `HeatHazeEffect` het beeld te vervormen.
- **`client/weather/HeatHazeEffect`**:
  - Beheert een eigen `PostChain` op de main render target: `assets/corruptedadventure/shaders/post/heat_haze.json` met het programma `shaders/program/heat_haze.json` + `heat_haze.fsh`. Dat zijn handgeschreven client-assets (geen datagen).
  - De fragmentshader verschuift de UV met een zachte sinus op schermhoogte en tijd, het sterkst in de onderste helft (de lucht boven de grond).
  - Uniform `Strength` = `strength × exposure × options.screenEffectScale()` (vanilla-toegankelijkheid "Distortion Effects": 0 = geen trilling). Uniform `Time` uit de game-tijd.
  - Laden bij de eerste keer dat hij nodig is. Opnieuw laden bij resource-reload, `resize` bij een andere venstergrootte, sluiten bij uitloggen.
  - Uitgeschakeld als de camera in een vloeistof zit, of bij `strength × exposure < 0,01`.
  - **Bij de bouw verifiëren** in 1.21.1:
    - de constructor van `PostChain` en `setUniform(String, float)` (vanilla gebruikt die voor de menu-blur);
    - of een post-programma met namespace (`corruptedadventure:heat_haze`) laadt;
    - welke `RenderLevelStageEvent.Stage` de juiste plek is (verwacht `AFTER_LEVEL`; hand en GUI niet vervormd).

    Lukt dit niet zonder access transformer of mixin, **stop dan en vraag de hoofdsessie** (alternatief: vraag 5 → B).

### 5. Hittegolf → uitdroging ×2 (`weather/HeatWaveDehydrationModifier`)

- `modify(condition, ctx, rate)`:
  - `condition == DEHYDRATION && rate > 0` en de staat van `ctx.position().ring()` is `HEAT_WAVE` → `rate × factor`.
  - Anders: `rate` ongewijzigd.
  - De Nether, de void en een vanilla-preset leveren nooit een `HEAT_WAVE`. Geen aparte check nodig behalve "dimensie heeft ringweer" (via de API van 20).
- `factor` volgens vraag 6 → A: `1 + (heatWaveDehydrationMultiplier − 1) × intensity()`. Standaard is de vermenigvuldiger 2,0; waar die staat bepaalt vraag 7.
- Pure functie `static float factor(RingWeatherState, float multiplier)`, getest.
- Registratie: zie "Scope en de koppeling met `conditions`".

### 6. Config

Volgens vraag 7 → A: in `[weather.ring2]` van `config/ServerConfig` (SERVER, per wereld, live gelezen; afgestemd: change 20 zet het weer daar bij 19 vraag 4 A). Met vraag 4 → A als standaard:

| Sleutel | Standaard | Betekenis |
|---|---|---|
| `heatWaveDehydrationMultiplier` | 2,0 (1,0–5,0) | factor op de opbouw van uitdroging tijdens een volle hittegolf |
| `sandstormPushStrength` | 0,08 (0–0,5) | impuls per windstoot in blokken/tick (bij 0,08 en een stoot per 10 ticks ≈ 0,35 blok/s drift op de grond; vraag 4) |
| `sandstormGustTicks` | 10 (5–100) | ticks tussen twee windstoten |

De visuele waarden (fog-afstand, aantal particles, kleuren) zijn **Java-constanten** in `client/weather/`. Een client-config zou een speler het zicht laten "terugzetten".

### 7. Geluid (alleen bij vraag 8 → A/B)

- Een sound event `corruptedadventure:weather.ring_2.sandstorm` in `ModSoundEvents` (common; dat moet, zie de valkuil in CODEBASE). In `ModSoundDefinitionsProvider` komt het als `event`-verwijzing naar een vanilla-geluid; kandidaten zijn `item.elytra.flying` met lage pitch of `ambient.basalt_deltas.loop`, de keuze valt in W-achteraf. Ondertitel in en + nl.
- `client/weather/SandstormSound` (`AbstractTickableSoundInstance`, loop): volume = `strength × exposure`, stopt bij 0.
- Bij B ook `weather.ring_2.heat_wave` (laag zoemen) op dezelfde manier.

### 8. Debugcommando

- `/ca weather` (change 20) toont bij een zandstorm ook de windrichting (`sandstorm 0.83 (412 s) wind 225° from SW`). Zo kun je de S-test via RCON lezen.
- `/ca weather 2 sandstorm [seconds]` trekt een nieuwe `seed`. Er komt geen extra argument voor de richting; voor testen is lezen genoeg.

### 9. Documentatie

- `CODEBASE.md`:
  - `weather/SandstormWind` en `weather/HeatWaveDehydrationModifier`, plus het veld `seed` in `RingWeatherState`.
  - `network/WindGustPayload` met de protocolversie.
  - `registry/ModParticleTypes`.
  - `client/weather/SandstormRenderer`, `HeatWaveRenderer`, `HeatHazeEffect` en `SkyExposure`, plus `client/particle/SandGustParticle` en `client/network/WindGustHandler`.
  - `datagen/ModParticleDescriptionProvider`, de shader-bestanden en de config-sleutels.
  - De tabel "Netwerk en persistente staat" (`wind_gust`; `ring_weather` met `seed`).
  - Valkuilen die bij de bouw opduiken (fog alleen met `setCanceled`, PostChain-levenscyclus, additieve impuls via `push` op de client).
- `fase-1-wereld.md` §2 Ring 2: onder "Weer (ringweer)" een kopje **Uitwerking (change 28)** met de windrichting, de duw, de blootstelling, de zichtafstand, de trillingstechniek, de ×2-regel en (optioneel) het geluid.
- `02-beslissingenlog.md`: de regels onderaan.
- `docs/backlog.md`:
  - de [conditions]-regel hierboven;
  - `[textures] Eigen sprite voor de sand_gust-particle (nu vanilla generic_*), bij de ring 2-textures (change 37). (bron: change 28)`;
  - eventueel `[atmosphere] Wind-duw ook voor items/mobs` als de gebruiker dat later wil;
  - bij vraag 8 → C: `[atmosphere] Windgeluid zandstorm in change 32`.

---

## Client/server-impact

- **Server:**
  - De staat (type, duur, `seed`) blijft van change 20.
  - Nieuw zijn de tick van de wind-duw (alleen spelers in ring 2 tijdens een zandstorm: één `ringAt` + één `canSeeSky` per stoot) en de modifier in de condities-pijplijn (één staat-lookup per speler per seconde).
  - Er wordt geen client-class geladen: de particle-type-registratie en de payload staan in common code. Renderers, particle-provider, shader-effect, geluid en de gust-handler staan in `client/` en worden alleen via `CorruptedAdventureClient` gekoppeld (patroon van change 18). `ClientIsolationTest` en de smoke-test bewaken dat.
- **Netwerk:**
  - `RingWeatherPayload` krijgt `seed` erbij.
  - Nieuwe `WindGustPayload` (twee floats, ≤ 2/s per blootgestelde speler, alleen tijdens een zandstorm).
  - `PROTOCOL_VERSION` gaat één omhoog.
  - De ×2 heeft geen eigen sync nodig: de meter-sync van change 19 dekt hem.
- **Client:** fog, kleur, particles, shader en (optioneel) geluid rekenen alleen vanuit de gesyncte staat, `ClientRingLayout` en de lokale blootstelling. De client beslist niets voor de gameplay; de duw komt van de server.
- **Multiplayer:**
  - Alle clients in een dimensie hebben dezelfde staat en dezelfde windrichting (zelfde `seed`, zelfde functie). Ze zien dus hetzelfde weer met dezelfde richting.
  - Verschillen komen alleen door de eigen plek (ring, overgangszone, binnen of buiten) en door de lokale grafische instellingen (particles, Distortion Effects). Particles zijn per client willekeurig (cosmetisch).
  - De duw en de ×2 gelden per speler, op de server.
- **Persistentie:** alleen het veld `seed` in de bestaande `SavedData`, met een standaardwaarde voor oude bestanden.
- Geen worldgen; geen invloed op determinisme.

---

## Tests

**B**: `./gradlew build` slaagt, met nieuwe of uitgebreide unit tests:
- `weather/SandstormWindTest`:
  - `direction` is een eenheidsvector, horizontaal en deterministisch per `seed`; 1.000 seeds leveren richtingen in alle vier kwadranten.
  - `impulse` is 0 bij intensiteit 0 of blootstelling 0, en schaalt lineair met sterkte × intensiteit.
  - De selectie (pure functie) laat alleen `CORE` ring 2 door, bij blend 0,49 vs. 0,51 op grens 1 en grens 2, en sluit creatief, spectator en passagiers uit.
- `weather/HeatWaveDehydrationModifierTest`:
  - ×1 bij intensiteit 0, ×2 bij 1 en ×1,5 bij 0,5 (vraag 6 → A).
  - Een negatieve rate blijft ongewijzigd.
  - Een andere conditie (`DEBUG`) blijft ongewijzigd.
  - Een zandstorm of `clear` blijft ongewijzigd.
  - Een vermenigvuldiger van 1,0 geeft altijd ×1.
- `weather/RingWeatherStateTest` (uitbreiden): `CODEC`- en `STREAM_CODEC`-rondreis met `seed`; NBT zonder `seed` (formaat van change 20) laadt met `seed` 0.
- `weather/RingWeatherCycleTest` (uitbreiden): een nieuw weer krijgt een `seed` uit de random; zelfde random → zelfde `seed`.
- `network/WindGustPayloadTest`: `StreamCodec`-rondreis.
- Pure client-wiskunde zonder client-imports (fog-afstand bij sterkte/blootstelling, het gladstrijken van `SkyExposure`) als statische functies, getest in `client/weather/…Test`.
- `ClientIsolationTest` blijft groen.

**D**: `./gradlew runData` slaagt: `particles/sand_gust.json` (en bij vraag 8 → A/B `sounds.json` + ondertitels en + nl). De gegenereerde output wordt gecommit.

**S**: `tools/server-smoke-test.sh` met `level-name=smoke-change28`. De server start zonder client-class-fouten en zonder registratiefout voor de payload of de particle.

Daarna een dedicated server met RCON en `online-mode=false`, seed `20260930`, een nieuwe `level-name` en **twee clients** (`Dev`, `Dev2`). De agent bedient alles via RCON; de clients staan stil.
1. **Zandstorm, staat:**
   - `ca weather 2 sandstorm 300` → `ca weather` toont `sandstorm` met een windrichting.
   - `gamerule doDaylightCycle false`, `time set noon`.
   - Beide spelers naar het midden van ring 2 (`execute as Dev at Dev run ca tpring 2`, idem voor Dev2), in de open lucht. 35 s wachten (fade-in van change 20).
2. **Duw:**
   - Meet `data get entity Dev Pos` twee keer met 10 s ertussen. De verplaatsing ligt in de windrichting (hoek ± 15°), en de grootte past bij `sandstormPushStrength` (orde 3–4 blokken bij de standaard; de echte waarde in de W-check vastleggen).
   - Dev2 meet hetzelfde: dezelfde richting.
3. **Blootstelling (vraag 3 → A):** een dak van steen boven Dev2 (`fill ~-3 ~3 ~-3 ~3 ~3 ~3 stone`), 2 s wachten → Dev2 verplaatst niet meer, Dev wel. Dak weg.
4. **Uitzonderingen:**
   - `gamemode creative Dev` → geen verplaatsing; terug naar survival.
   - Dev naar ring 3 (`ca tpring 3`) → geen verplaatsing.
   - `ca weather 2 clear` → na de fade-out geen verplaatsing.
   - `execute in minecraft:the_nether` → niets.
5. **Hittegolf ×2:**
   - `ca weather 2 heat_wave 300`, middag, open lucht. `ca condition set Dev dehydration 0` en `Dev2` ook.
   - Na ≥ 30 s toont `ca condition get Dev` `+0.80/s, sun` (bij 27 vraag 1 → A), en Dev2 hetzelfde.
   - Direct na het zetten (fade-in) ligt de waarde ertussen.
   - Dev2 onder een dak → een negatieve rate (`shade`), net zo groot als zonder hittegolf.
   - `ca weather 2 sandstorm` → `+0.40/s`.
   - Dev in ring 3 tijdens een hittegolf in ring 2 → geen ×2.
6. **Twee clients gelijk** (geautomatiseerde benadering van "effect op twee clients tegelijk gelijk"): in beide client-logs staan op DEBUG dezelfde regels voor de actieve typen, dezelfde windrichting (graden gelijk) en dezelfde sterkte (± 0,02) zolang beide spelers op dezelfde plek staan. Geen fouten, geen disconnect, geen `unknown payload`.
7. **Persistentie van `seed`:** `ca weather 2 sandstorm 600`, de richting noteren, `stop` via RCON, opnieuw starten → `ca weather` toont dezelfde richting.
8. **Vanilla-preset** (aparte `level-name`): geen ringweer, geen duw, geen fouten.
9. Daarna RCON uit en de testwereld weg.

**Blokkerende handmatige test: NEE.** Reden:
- De gameplay (duw, blootstelling, grens, ×2) is server-side en via RCON meetbaar (stap 2–5).
- "Gelijk op twee clients" volgt uit de constructie: dezelfde gesyncte staat (blokkerend getest in change 20) en een pure, geteste richtingsfunctie. Stap 6 controleert het via de client-logs.
- Wat niet automatisch te controleren is, is alleen het **uiterlijk**: fog-dichtheid, particles en de shader-trilling. Daar bouwt in ring 2 geen latere change op. Change 32 (ring 2-sfeer) zet de biome-kleuren; de zandstormkleur mengt daar alleen overheen.
- Het eerste dat dit patroon hergebruikt (renderer, particle en post-shader), is change 45 (ring 3-weer). Daarom: **W-achteraf, in teststop 3 ná change 37 (dus ruim vóór change 45).**

**W-achteraf** (niet blokkerend, ~10 min, seed `20260930`, dedicated server + twee clients):
- [ ] **Zandstorm:**
  - [ ] Zicht sterk beperkt volgens vraag 4.
  - [ ] Zandkleurige lucht.
  - [ ] Particles waaien in dezelfde richting als de duw, op beide clients dezelfde kant op.
  - [ ] Opbouw en afbouw in ~30 s.
- [ ] Langzaam over grens 2→3 en grens 1→2 lopen: de fog dunt vloeiend uit (vraag 4 van 20); de duw stopt op de grens.
- [ ] Een grot of huis inlopen: fog en particles verdwijnen geleidelijk (~2 s); geen particles binnen; de duw stopt.
- [ ] In water of lava en met blindness: vanilla-fog, geen zandfog.
- [ ] De duw voelt "licht": oversteken kan, tegen de wind in lopen merkbaar trager; geen haperingen of rubber-banding.
- [ ] **Hittegolf:**
  - [ ] Beeld trilt zacht, sterkst laag in beeld.
  - [ ] Hand en GUI niet vervormd.
  - [ ] Distortion Effects op 0 → geen trilling.
  - [ ] Venster vergroten/verkleinen en F3+T zonder zwart beeld of fouten.
  - [ ] Geen merkbaar FPS-verlies.
- [ ] HUD-balk van uitdroging loopt in een hittegolf zichtbaar sneller vol.
- [ ] Particles op "Minimal" → nauwelijks zandkorrels, wel fog.
- [ ] Geluid (vraag 8 → A/B): de keuze voor het vanilla-geluid valt hier; het volume volgt de storm en is binnen zachter.
- [ ] Met Sodium/Iris (indien beschikbaar): geen crash. Dat het shader-effect daar mogelijk ontbreekt, is geaccepteerd.

**W**: geen worldgen; de standaard portal-test is niet nodig.

**Checklist (concept)**
- [ ] Scope `atmosphere` benoemd; namen van 18, 19, 20 en 27 gecontroleerd en dit plan aangepast
- [ ] Aanraking `conditions` beperkt tot de registratie van de modifier (+ backlog-regel)
- [ ] B groen (incl. nieuwe tests en `ClientIsolationTest`); D groen en output gecommit
- [ ] S: smoke-test + RCON-stappen 1–8 groen; `PROTOCOL_VERSION` opgehoogd
- [ ] `CODEBASE.md`, `fase-1-wereld.md` (Ring 2, uitwerking change 28), beslissingenlog en backlog bijgewerkt
- [ ] `/code-review` gedaan, bevindingen afgehandeld
- [ ] Commit `atmosphere: add sandstorm and heat wave effects for ring 2`
- [ ] W-achteraf-checklist doorgegeven voor teststop 3 (na change 37)

---

## Risico's

- **Changes 18–20 of 27 wijken af** (andere namen, geen registratie-API voor modifiers, andere F3-hook, geen `client2`): bij de start naast de code leggen. Ontbreekt een tweede dev-client, dan stoppen en vragen.
- **Fog-event:** `ViewportEvent.RenderFog` past de afstanden alleen toe als het event gecanceld wordt. Andere mods of vanilla-situaties (lava, poedersneeuw) kunnen botsen; daarom alleen zonder vloeistof of blindheid.
- **Post-shader (vraag 5 → A):**
  - API-details in 1.21.1 (constructor van `PostChain`, `setUniform`, namespace van het programma, de juiste render-stage) zijn niet zeker.
  - Shaderpacks (Iris) en sommige performance-mods slaan vanilla-post-effecten over of breken ze.
  - Mitigaties: verifiëren bij de bouw; bij blokkades stoppen en optie B voorleggen; altijd netjes uitschakelen (geen zwart scherm).
  - Vanilla's eigen post-effect (spectator-mob-view) kan er tegelijk ook zijn: geaccepteerd.
- **Wind-duw:**
  - De client beweegt de speler zelf, dus de server ziet de verplaatsing pas via de bewegingspakketten.
  - Een te sterke impuls of te veel stoten geeft "moved too quickly"-waarschuwingen of rubber-banding. Begrenzing via de config (max. 0,5) en de check in W-achteraf.
  - Een aangepaste client kan de impuls negeren; geaccepteerd (coöperatieve mod).
- **Blootstelling:** server (`canSeeSky`, aan of uit) en client (gladgestreken hemellicht) zijn bewust niet identiek: visueel zacht, gameplay hard. Onder één blok overhang is er dus geen duw, terwijl de fog gedeeltelijk blijft. Geaccepteerd, en in de plan-uitwerking benoemd.
- **Veld `seed` in de staat:** het formaat van de payload verandert, dus de protocolversie moet omhoog. Vergeten → een oude client decodeert onzin. Review-punt.
- **Particles en performance:** veel particles in een groot kader rond de camera kosten FPS op zwakke machines. Het aantal is begrensd door een constante en door de vanilla-instelling "Particles".
- **Scope:** wil de bouwer toch `ConditionContext` uitbreiden (zoals change 27 voorstelde), dan raakt dat `conditions`. Niet doen; de modifier leest de staat zelf.

---

## Open ontwerpvragen

Het design legt vast **dat** de zandstorm fog, particles en een lichte wind-duw (server) heeft, en **dat** de hittegolf de lucht laat trillen (client) en uitdroging 2× sneller maakt. Deze vragen gaan over wat het design openlaat. Ronde 1 (zandstorm): vraag 1–4. Ronde 2 (hittegolf, koppeling, geluid): vraag 5–8.

### 1. Hoe werkt de wind-duw technisch?
In alle opties beslist de server wanneer en hoe hard er geduwd wordt. Alleen spelers worden geduwd (geen mobs of items), niet in creatief of spectator, en niet als passagier. Spelersbeweging wordt in vanilla door de client uitgerekend; daarom verschillen de opties in hoe de duw daar aankomt.
- **A. Additieve windstoten via een eigen payload (aanbevolen).** De server stuurt elke ~10 ticks een kleine impuls naar elke blootgestelde speler in ring 2; de client telt die op bij zijn snelheid, net zoals vanilla de knockback van een explosie doet. Vloeiend (overschrijft je eigen beweging niet) en strikt server-gestuurd. Kost één kleine payload en een client-handler, ~2 pakketjes/s per speler, alleen tijdens een storm.
- **B. Gedeelde physics: dezelfde common functie draait op de client voor de eigen speler, uit de gesyncte staat.** Het vloeiendst (elke tick, geen pakketten), zoals vanilla-waterstromingen. Maar de duw wordt dan niet per stoot door de server beslist, en dat wijkt af van "wind-duw (server)" in het design.
- **C. De server zet de snelheid (`push` + `hurtMarked`, zoals gewone knockback).** Geen nieuwe payload. Maar dit overschrijft de snelheid van de speler bij elke stoot: lopen hapert en voelt als steeds een duwtje van een mob.

Antwoord: **A** — additieve windstoten via een eigen payload (`WindGustPayload`, ~elke 10 ticks naar blootgestelde spelers in ring 2); server beslist.

### 2. Uit welke richting waait de wind?
- **A. Eén willekeurige richting per storm, vastgelegd in de weerstaat (aanbevolen).** Elke storm voelt anders. Alle spelers en clients hebben dezelfde richting (gesynct via een `seed` in de staat, die ook voor latere typen bruikbaar is). Kost een veld in de staat, de payload en de opslag, plus een ophoging van de protocolversie.
- **B. Vast radiaal: altijd vanaf ring 3 naar binnen (richting 0,0).** Thematisch ("het zand van de verschroeide ring waait naar binnen"), zonder extra staat of sync. Wel overal anders gericht en voorspelbaar; aan de andere kant van de wereld waait het de andere kant op.
- **C. Langzaam draaiend met de game-tijd (dezelfde formule op server en client).** Levendig zonder extra veld. Lastiger te testen en uit te leggen; de richting verandert terwijl je loopt.

Antwoord: **A** — één willekeurige richting per storm, afgeleid uit de `seed` in `RingWeatherState` (zit al in change 20).

### 3. Hoe werkt het weer binnen, onder een overkapping en ondergronds?
Geldt voor beide weertypen (fog, particles, trilling, duw). De ×2 voor uitdroging volgt toch al de zon/schaduw-regel van change 27.
- **A. Naar blootstelling (aanbevolen).** Visueel schaalt het met het hemellicht op de camera, gladgestreken over ~2 s: onder een overkapping zwakker, in een grot of huis niets, geen particles binnen. De duw werkt alleen als je de hemel ziet (dezelfde regel als "direct zonlicht" bij uitdroging). Schuilen werkt, en het voelt logisch. Een kleine extra berekening per tick op de client.
- **B. Overal in ring 2 even sterk, ook ondergronds.** Simpelst. Maar mijnen in zandfog en windduw in een afgesloten huis voelt fout, en schuilen helpt niet.
- **C. Op hoogte: alleen boven het terrein (heightmap); verder geen verschil.** Grotten zijn vrij, maar in een huis met dak heb je nog steeds volle fog (de heightmap ziet het dak wel, het binnen-/buitengevoel niet), en onder een overhang ook.

Antwoord: **A** — naar blootstelling: visueel met het gladgestreken hemellicht op de camera, duw alleen met `canSeeSky`.

### 4. Hoe hevig is een volle zandstorm?
Alle waarden zijn later bij te stellen: duw in de config, zicht en particles als constanten.
- **A. Stevig: zicht ~24 blokken, ~40 particles per tick, duw ≈ 0,35 blok/s drift (aanbevolen).** Duidelijk "zicht sterk beperkt": je verliest herkenningspunten en kunt verdwalen, maar navigeren op korte afstand kan nog. Tegen de wind in loop je ~8 % trager.
- **B. Zwaar: zicht ~12 blokken, ~80 particles per tick, duw ≈ 0,7 blok/s.** Echt gevaarlijk en eng (mobs zie je pas vlak voor je neus). Kan in combinatie met de duw frustreren, en particles kosten op zwakke pc's merkbaar FPS.
- **C. Mild: zicht ~48 blokken, ~20 particles per tick, duw ≈ 0,15 blok/s.** Sfeervol, nauwelijks hinderlijk; de duw is bijna niet te merken.

Antwoord: **A** — stevig: zicht ~24 blokken, ~40 particles per tick, duw ≈ 0,35 blok/s drift.

### 5. Hoe laten we de lucht trillen bij een hittegolf?
In alle opties komt er een lichte warme waas bij (fog-kleur iets warmer, zicht iets korter), en volgt de sterkte de vanilla-toegankelijkheid "Distortion Effects".
- **A. Post-processing-shader: het beeld golft zacht, het sterkst laag in beeld (aanbevolen).** Lijkt echt op hittewaas boven zand. Kost drie shader-bestanden en een eigen `PostChain` (laden, schalen, opruimen); de API in 1.21.1 wordt bij de bouw geverifieerd (lukt het niet zonder mixin, dan terug naar de gebruiker). Met shaderpacks (Iris) valt het effect mogelijk weg.
- **B. Geen shader: warme waas + opstijgende, flikkerende hitte-particles bij de grond.** Robuust, goedkoop en werkt met elke render-mod. Maar het "trilt" niet echt; het is eerder nevelig.
- **C. Camera-zweving (kleine golvende kanteling van het beeld) + warme waas.** Goedkoop en voelbaar. Kan misselijk maken en lijkt meer op het nausea-effect dan op hitte.

Antwoord: **A** — post-processing-shader (zacht golvend beeld, sterkst laag in beeld) + warme waas; volgt "Distortion Effects"; lukt het niet zonder mixin, dan stop-en-vraag (optie B).

### 6. Hoe volgt de ×2 (en de duw) het weer in tijd en ruimte?
- **A. Mee met de fade in de tijd, hard op de ringgrens (aanbevolen).** Factor = 1 + (2 − 1) × intensiteit: in ~30 s van ×1 naar ×2 en terug, gelijk met wat je ziet. Ruimtelijk telt alleen ring 2 (wissel op 0,5), zoals alle gameplay sinds change 20. De duw volgt dezelfde regel.
- **B. Direct ×2 zodra het type actief is, ook tijdens het in- en uitfaden.** Simpelst. Maar je voelt de hittegolf al 30 s voordat je hem ziet, en na afloop nog even zonder zichtbaar effect.
- **C. Ook ruimtelijk gemengd (× gewicht over de overgangszone, zoals de rendering).** Heel vloeiend. Wijkt af van de harde gameplay-grens van change 20 en de zoneregel van uitdroging (27); de rate wordt minder voorspelbaar.

Antwoord: **A** — mee met de fade in de tijd (factor 1 + intensiteit), hard op de ringgrens (0,5); de duw volgt dezelfde regel.

### 7. Waar staan de getallen van het weer-effect in de config?
- **A. In de weer-sectie `[weather.ring2]` van de SERVER-config (per wereld, live gelezen): ×2-factor, duwsterkte, stootinterval (aanbevolen).** Het weer beheert zijn eigen effecten; de conditiecode blijft onaangeroerd (registratie via `ConditionDefinitions.addModifier`), en changes 45/66/85 volgen hetzelfde patroon. Nadeel: de uitdrogingsbalans staat in twee secties van hetzelfde bestand. *(Afgestemd 2026-10-01: het oude nadeel "geldt voor alle werelden" vervalt, want 19/20 zetten alle balans in `ServerConfig`.)*
- **B. ×2-factor bij `conditions.dehydration` in dezelfde SERVER-config (naast de andere uitdrogingsgetallen); de duw in `[weather.ring2]`.** Alle uitdrogingsbalans bij elkaar. Wel een sleutel in de conditions-sectie (andere categorie; klein, met backlog-notitie).
- **C. Niet instelbaar: vaste ×2 en een vaste duw in code.** Simpelst; het design zegt letterlijk "2×". Wijkt wel af van §1.9 ("sterktes en snelheden in config").

Antwoord: **A** — in `[weather.ring2]` van de SERVER-config: ×2-factor, duwsterkte, stootinterval.

### 8. Krijgt het weer geluid?
Het design noemt voor ring 2-weer geen geluid. Change 32 (ring 2-sfeer) doet ambient-geluid per biome.
- **A. Ja, een wind-loop bij de zandstorm; de hittegolf blijft stil (aanbevolen).** Een storm zonder wind voelt kaal. Eigen sound event als verwijzing naar een vanilla-geluid (geen eigen `.ogg`), volume volgens sterkte × blootstelling (binnen zachter), met ondertitel. Kleine extra: event, `sounds.json`, lang, client-loop.
- **B. Wind-loop bij de zandstorm én een laag hittezoemen bij de hittegolf.** Beide typen zijn ook met gesloten ogen herkenbaar. Twee loops; zoemen kan irriteren bij lange hittegolven.
- **C. Nu geen geluid; naar de backlog voor change 32.** Strikt het design. De storm is in deze change alleen visueel.

Antwoord: **A** — wind-loop bij de zandstorm (vanilla-geluid via eigen sound event); de hittegolf blijft stil.

---

## Beslissingenlog (bij de bouw invullen met de datum van vandaag, één regel per antwoord)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | atmosphere | Wind-duw zandstorm: additieve windstoten via `WindGustPayload`, door de server beslist; alleen spelers, niet in creatief/spectator of als passagier | Vloeiend, overschrijft de eigen beweging niet |
| 2026-10-01 | atmosphere | Windrichting zandstorm: één willekeurige richting per storm uit de `seed` van de weerstaat | Elke storm anders; alle clients gelijk |
| 2026-10-01 | atmosphere | Ringweer binnen/ondergronds: naar blootstelling (hemellicht; duw alleen met `canSeeSky`) | Schuilen werkt en voelt logisch |
| 2026-10-01 | atmosphere | Hevigheid volle zandstorm: zicht ~24, ~40 particles/tick, duw ≈ 0,35 blok/s (zicht, particles, duw) | Zicht sterk beperkt, navigeren op korte afstand blijft mogelijk |
| 2026-10-01 | atmosphere | Trilling hittegolf: post-processing-shader; altijd warme waas, sterkte volgt "Distortion Effects" | Lijkt echt op hittewaas |
| 2026-10-01 | atmosphere | Hittegolf-×2 en duw volgen het weer: mee met de fade in de tijd, hard op de ringgrens | Gelijk met wat je ziet; harde gameplay-grens zoals change 20 |
| 2026-10-01 | atmosphere | Config weer-effecten ring 2: `[weather.ring2]` in de SERVER-config | Weer beheert zijn eigen effecten; `conditions` onaangeroerd |
| 2026-10-01 | atmosphere | Geluid ring 2-weer: wind-loop bij de zandstorm (vanilla-geluid), hittegolf stil | Storm zonder wind voelt kaal; geen audiobestand nodig |
| 2026-10-01 | atmosphere | Hittegolf ×2 als `RateModifier` in `weather/` die de weerstaat zelf leest; `ConditionContext` en `DehydrationRule` blijven ongewijzigd, alleen de registratie raakt `conditions` | Scope `atmosphere` houden; latere weer-changes volgen hetzelfde patroon |
| 2026-10-01 | atmosphere | Zandstorm-particle `corruptedadventure:sand_gust` met vanilla `generic_*`-sprites; visuele waarden als constanten, geen client-config | Geen textures-werk in deze change; spelers kunnen het zicht niet terugzetten |
