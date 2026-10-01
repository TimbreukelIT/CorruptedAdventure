# Change 18 — Netwerk-basis en ring-layout naar de client

**Scope: `infra` — change 18**

**Doel:** de mod krijgt zijn netwerkfundament: payload-registratie via `RegisterPayloadHandlersEvent` met een eigen protocolversie, een vast patroon voor clientbound handlers dat de dedicated server nooit een client-class laat laden, een helper om payloads naar spelers in een ring te sturen, en als eerste payload de **ring-layout** (snapshot uit change 1 + grensruis) die de client bij het joinen krijgt. Daarna kan de client zelf `ringAt` rekenen, exact gelijk aan de server. Changes 19 (meters/HUD), 20 (ringweer), 27 (uitdroging) en 28 (ring 2-weer) bouwen hierop.

---

## Afstemming (2026-10-01)

Afgestemd met de plannen 19–37 (basis: de aanbevolen opties).
- **Tweede dev-client hoort hier (`infra`).** Nieuw stap 11: in `build.gradle` een run `client2` (eigen `gameDirectory` `run/client2`, `--username Dev2`) en `runClient` met vaste `--username Dev`; optioneel quick-play (`--quickPlayMultiplayer localhost:25565`, aan te zetten via een Gradle-property), zodat een agent clients automatisch tegen `runServer` laat joinen en de client-logs controleert zonder gebruiker. Gebruikers: 19, 20, 28 (S met twee clients). De `[infra]`-backlogregel uit change 19 vervalt daarmee. Documenteren in `CODEBASE.md` (Build en configuratie).
- **Bindende patronen voor latere changes:** `ClientboundHandlers` (stap 2) voor elke clientbound payload (19 `ConditionSyncHandler`, 20 `RingWeatherHandler`, 28 `WindGustHandler`); `RingWorld.isActive(server)` (stap 5) is de enige preset-check (19 `ConditionTicker`, 20 weer, 28 wind-duw); `client/RingDebugOverlay` is de enige F3-hook — 20 en 28 voegen er regels aan toe.
- **Vraag 3 (`CORE`/`TOUCHING`):** change 20 stuurt de volledige weerstaat naar de hele dimensie en gebruikt de helper niet. De enige gebruiker in 18–37 is 28 (`CORE`: `matches` + `playersInRing`). `TOUCHING` heeft dus (nog) geen gebruiker. **Besloten 2026-10-01: A** (aanbevolen optie) — beide varianten blijven.
- **Vraag 1 A — belangrijk:** de nieuwe ruis-seed verschuift de verruisde ringgrenzen (binnen ±96) bij elke seed, ook bij testseed `20260930`. Ring 1 (incl. de ligging van grens 1 en `withered_expanse`) is in-game goedgekeurd; vorm en karakter blijven gelijk, alleen de exacte ligging schuift. Bij de gebundelde teststop grens 1 even opnieuw bekijken.
- **Teststop (besloten 2026-10-01, teststop 1 van 3):** de blokkerende handtest van deze change is **onderdeel van teststop 1 ná change 20** (gebundeld met 19 en 20; dedicated server + `Dev` + `Dev2`; checklist in change 20). Per change blijven B, S, RCON en (via quick-play) de automatische join- en payloadcheck in de client-logs verplicht vóór de volgende change.
- `PROTOCOL_VERSION`: 19, 20 en 28 hogen elk één keer op (nieuwe payloads). `RingWeatherState` krijgt zijn `seed`-veld al in 20, dus 28 verandert geen bestaand formaat.

---

## Afhankelijkheden en aannames

Gecontroleerd in de code (stand na change 17):

- **`config/RingSettings`** (record: `ringOuterRadii`, `voidOuterRadius`, `boundaryNoise`, `transitionWidth`; `CODEC`, `validate()`) en **`config/RingSettingsSnapshot`** (`current()`, gezet bij `ServerAboutToStartEvent`, gewist bij `ServerStoppedEvent`; javadoc noemt "sync in change 18").
- **`ring/RingLayout`**: constructor `RingLayout(RingSettings settings, long seed)` bouwt de grensruis met `new XoroshiroRandomSource(seed).forkPositional().fromHashOf(id("ring_boundary"))`; de server roept hem aan met de **wereldseed** (`worldGenOptions().seed()`) in `onServerAboutToStart` (prioriteit LOW). Static `current()`/`ringAt` bestaan alleen server-side. Instantie-API `sample(dimension, x, z)` en `sampleOverworld` is puur en dus ook op de client bruikbaar. Tests bouwen layouts met vaste seeds (`12345L`, `SEED`).
- **`ring/Ring`** (`RING_1`…`RING_5`, `VOID`, `CODEC`, `byIndex`, `number()`), **`ring/RingPosition`** (`inner`, `outer`, `blend`; `ring()`, `inTransition()`).
- **`worldgen/border/RingWorldBorder`**: private `isCorruptedWorld(server)` = overworld-generator is `RingChunkGenerator`; stuurt al vanilla-border-packets na login/dimensiewissel/respawn (patroon `PlayerEvent.PlayerLoggedInEvent`).
- **`CorruptedAdventure`** registreert alles in de constructor (mod-bus `modEventBus`, game-bus `NeoForge.EVENT_BUS`); **`client/CorruptedAdventureClient`** is de enige client-class (`@Mod(dist = Dist.CLIENT)`), nog leeg.
- `network/` en `data/` bestaan nog niet (🔲 in `CODEBASE.md`). Geen payloads, geen attachments, geen SavedData.
- Alleen `client/` en `datagen/` (NeoForge model-generators) importeren nu client-packages; de rest van `src/main` is schoon.
- **Backlog (change 2):** "de client kent de wereldseed niet … sync een afgeleide ruis-seed, nooit de seed zelf." Deze change lost dat op (vraag 1).
- NeoForge 21.1.252: `RegisterPayloadHandlersEvent#registrar(version)` → `PayloadRegistrar` (`playToClient`, `playToServer`, `playBidirectional`, `optional()`, `executesOn(HandlerThread)`); `PacketDistributor.sendToPlayer/sendToPlayersInDimension/sendToAllPlayers`. **Bij de bouw verifiëren:** standaard-handlerthread (verwacht: main) en of `RegisterPayloadHandlersEvent` op de mod-bus vuurt (verwacht: ja).

---

## Plan

### 1. Payload-registratie en versiebeheer (`network/ModNetwork.java`)
- `public static final String PROTOCOL_VERSION = "1";` met javadoc: **ophogen bij elke wijziging aan een bestaande payload of codec** (nieuwe payloads ook, want een oude client kent ze niet). Vraag 2.
- Listener op de mod-bus voor `RegisterPayloadHandlersEvent`: `event.registrar(PROTOCOL_VERSION)` — **verplicht** (niet `optional()`), dus een client met een andere versie of zonder de mod krijgt NeoForge's eigen disconnect-melding. (Een vanilla-client kan sowieso niet joinen: onze blokken en biomes staan in de registry-sync.)
- Eén plek waar alle payloads staan: `register(PayloadRegistrar)` met per payload één regel. Latere changes voegen hier hun regel toe (19: meters, 20: weer).
- Registratie in `CorruptedAdventure` (`ModNetwork.register(modEventBus)`).

### 2. Clientbound handlers zonder client-class op de server (`network/ClientboundHandlers.java`) — vraag 5
Patroon (aanbevolen optie A):
- Common class met een `ConcurrentHashMap<CustomPacketPayload.Type<?>, IPayloadHandler<?>>`.
- `public static <T extends CustomPacketPayload> void set(Type<T> type, IPayloadHandler<T> handler)` — alleen aangeroepen door `CorruptedAdventureClient` (bij mod-constructie, dus vóór elke verbinding).
- `static <T extends CustomPacketPayload> IPayloadHandler<T> forward(Type<T> type)` — gebruikt door `ModNetwork` bij `playToClient(TYPE, STREAM_CODEC, ClientboundHandlers.forward(TYPE))`. Zoekt bij ontvangst de handler op; ontbreekt die → één waarschuwing in de log, payload genegeerd.
- Gevolg: common code noemt **nooit** een class uit `client/`, ook niet in een lambda of method reference. Elke volgende clientbound payload volgt dit patroon: payload + codec in `network/`, handler in `client/network/`, koppeling in `CorruptedAdventureClient`.

### 3. Payload `RingLayoutPayload` (`network/RingLayoutPayload.java`)
- `record RingLayoutPayload(RingSettings settings, long noiseSeed) implements CustomPacketPayload`, `TYPE` = `corruptedadventure:ring_layout`.
- `STREAM_CODEC` als `StreamCodec<ByteBuf, …>` (geen registry-toegang nodig, testbaar met `Unpooled.buffer()`): `VAR_INT` lijst van de radii, `VAR_INT` void-rand, ruis, overgangsbreedte, `LONG` ruis-seed (`StreamCodec.composite`, ≤ 6 velden). Geen NBT via `fromCodec`.
- `static RingLayoutPayload of(RingLayout layout)` — haalt settings en ruis-seed uit de layout (stap 4).

### 4. Ruis-seed die niets over de wereldseed verraadt (`ring/RingLayout`) — vraag 1
Aanbevolen optie A (kleine, noodzakelijke ingreep in de ringfunctie):
- Nieuw: `public static long noiseSeed(long worldSeed)` = eerste 8 bytes van **SHA-256**(wereldseed ‖ `"corruptedadventure:ring_boundary"`) (Guava `Hashing.sha256()`, zoals vanilla's `BiomeManager.obfuscateSeed`, maar met eigen zout zodat hij níet gelijk is aan de seed-hash die vanilla al naar de client stuurt).
- Constructorparameter heet voortaan `noiseSeed`; de bouw van de ruis zelf blijft gelijk. Server: `new RingLayout(snapshot, noiseSeed(worldSeed))`. Tests die layouts met vaste seeds bouwen blijven ongewijzigd geldig.
- Nieuwe getters `settings()` en `noiseSeed()` (voor de payload en de client).
- **Gevolg:** de grensruis is per seed anders dan vóór change 18 → grenzen verschuiven (binnen ±96 blokken) in bestaande testwerelden; nieuw gegenereerde chunks rond een grens geven daar naden. Verse werelden zijn deterministisch zoals voorheen. Metingen uit eerdere changes (spawn, erts per chunk) blijven inhoudelijk geldig, maar de exacte grensposities bij seed `20260930` veranderen.
- De client krijgt de ruis-seed en de settings en bouwt `new RingLayout(settings, noiseSeed)`: exact dezelfde `sample`-uitkomsten als de server, in overworld én Nether.

### 5. Gedeelde check "is dit een ringwereld?" (`ring/RingWorld.java`) — vraag 4
Aanbevolen optie A:
- `public static boolean isActive(MinecraftServer server)` = overworld-generator is `RingChunkGenerator` (verhuisd uit `RingWorldBorder.isCorruptedWorld`, die hem daarna aanroept; gedrag van de border ongewijzigd).
- De ring-layout gaat alleen naar de client in een ringwereld. Changes 19/20 gebruiken dezelfde check voor tick-handlers en weer, zodat een vanilla-presetwereld met de mod erin vanilla blijft.

### 6. Versturen bij het joinen (`network/RingLayoutSync.java`)
- Game-bus-listener op `PlayerEvent.PlayerLoggedInEvent`: `if (RingWorld.isActive(server)) PacketDistributor.sendToPlayer(player, RingLayoutPayload.of(RingLayout.current()))`.
- Geen herhaling bij dimensiewissel of respawn: één layout dekt overworld én Nether (`sample(dimension, …)` schaalt zelf), en de snapshot is per wereld bevroren.
- Werkt ook voor de host van een geïntegreerde server (singleplayer, LAN): die doorloopt dezelfde login.

### 7. Helper "naar spelers in een ring" (`network/RingPacketDistributor.java`) — vraag 3
- `enum RingMembership { CORE, TOUCHING }` (aanbevolen optie A: beide, keuze per aanroep):
  - `CORE`: `position.ring() == ring` (zelfde harde wissel op gewicht 0,5 als biomes en `/ca ring`).
  - `TOUCHING`: in de ring zelf **of** in een overgangszone die aan die ring grenst (`inner == ring || outer == ring`), dus een speler in de zone 2→3 telt voor ring 2 én ring 3.
- `static boolean matches(RingPosition position, Ring ring, RingMembership membership)` — puur, getest.
- `static List<ServerPlayer> playersInRing(ServerLevel level, Ring ring, RingMembership membership)` — via `RingLayout.ringAt(level.dimension(), player.getX(), player.getZ())`; ook bruikbaar voor server-side gameplay (bijv. de wind-duw van change 28).
- `static void sendToPlayersInRing(ServerLevel level, Ring ring, RingMembership membership, CustomPacketPayload payload)` — per speler `PacketDistributor.sendToPlayer`.
- Voor de rest gewoon NeoForge's `PacketDistributor` (één speler, dimensie, iedereen); geen eigen wrappers daarvoor.

### 8. Client-kant (`client/`)
- `client/ClientRingLayout.java`: `volatile RingLayout` + `static Optional<RingLayout> get()` en `static Optional<RingPosition> ringAt(ResourceKey<Level> dimension, double x, double z)`. Leeg zolang er niets ontvangen is of in een niet-ringwereld. **Gewist bij `ClientPlayerNetworkEvent.LoggingOut`**, zodat een volgende wereld (bijv. vanilla-preset in singleplayer) geen oude layout houdt.
- `client/network/RingLayoutHandler.java`: valideert `settings.validate()` (fout → error in de log, layout blijft leeg), bouwt `new RingLayout(settings, noiseSeed)`, zet hem in `ClientRingLayout`. Schrijft alleen één volatile veld, dus veilig op elke handlerthread.
- `CorruptedAdventureClient`: `ClientboundHandlers.set(RingLayoutPayload.TYPE, RingLayoutHandler::handle)` en de game-bus-listeners (logout, F3).
- **Debugweergave (vraag 6, aanbevolen A):** `client/RingDebugOverlay.java` op `CustomizeGuiOverlayEvent.DebugText`: één regel rechts in F3, bijv. `CA ring (client): ring_2 → ring_3 @ 0.43` of `CA ring (client): ring_1`, of `CA ring (client): —` als er geen layout is. Vaste Engelse tekst (zoals `/ca`), geen lang-keys. Herbruikbaar in 20/28 om weer-rendering tegen de ring te controleren.
- Regel voor later: client-code gebruikt **altijd** `ClientRingLayout`, nooit `RingLayout.current()`/`ringAt` (die werken in singleplayer per ongeluk, maar crashen op een client bij een dedicated server).

### 9. Automatische bewaking van de client/server-scheiding (`src/test/.../ClientIsolationTest.java`)
- Unit test die alle `.java`-bestanden onder `src/main/java` leest en faalt als een bestand buiten `client/` en `datagen/` een import heeft uit `net.minecraft.client`, `com.mojang.blaze3d`, `net.neoforged.neoforge.client` of `nl.timtoys.corruptedadventure.client`. (`datagen/` is uitgezonderd: de NeoForge model-generators zitten in een client-package, maar draaien alleen in `runData`.) Klein, goedkoop, en het bewaakt de harde regel voor alle volgende changes.

### 10. Documentatie
- `CODEBASE.md`: `network/` ✅ (`ModNetwork`, `ClientboundHandlers`, `RingLayoutPayload`, `RingLayoutSync`, `RingPacketDistributor`), `ring/RingWorld`, `client/ClientRingLayout`, `client/network/RingLayoutHandler`, `client/RingDebugOverlay`, tabel "Netwerk en persistente staat" (`corruptedadventure:ring_layout`, server → client, bij login, alleen ringwereld), `RingLayout` (`noiseSeed`, getters), `RingWorldBorder` (gebruikt `RingWorld`), tests; valkuil r. 338 vervangen door "client: `ClientRingLayout`, nooit `RingLayout.current()`"; nieuwe valkuil: **protocolversie ophogen bij elke payloadwijziging** en het handler-patroon (stap 2). Status change 18.
- `01-techniek.md` §Multiplayer-regels: protocolversie, clientbound-handlerpatroon, ruis-seed via SHA-256 (nooit de wereldseed), check `RingWorld.isActive`.
- `fase-1-wereld.md` §1.3: sync gebouwd (wat, wanneer, ruis-seed). Bouwplanrij 1 en 18 hoeven niet te veranderen.
- `backlog.md`: regel "[infra] Change 18 (sync naar client) …" afvinken/verwijderen; regel change 13 (client-side verloop ring 1) blijft, maar is nu niet meer geblokkeerd.
- `02-beslissingenlog.md`: zie onderaan.
- Javadoc `RingSettingsSnapshot` ("sync in change 18") bijwerken.

---

## Client/server-impact

- **Server:** registreert de payload (common), stuurt bij login één klein pakket (~30 bytes) per speler, alleen in een ringwereld. `RingLayout` rekent met een gehashte ruis-seed in plaats van de wereldseed: grenzen verschuiven eenmalig, verder deterministisch per seed zoals voorheen.
- **Client:** `client/`-classes ontvangen, bewaren en tonen (F3) de layout. Gameplay rekent nooit op de client; de client-layout is alleen voor HUD, weer-rendering en debug.
- **Dedicated server laadt nooit een client-class:** common code verwijst nergens naar `client/` (stap 2); `ClientIsolationTest` bewaakt dat.
- **Beveiliging:** de client krijgt alleen een SHA-256-afgeleide van de seed, met eigen zout (zelfde niveau als vanilla's gehashte seed).
- **Compatibiliteit:** client en server moeten dezelfde `PROTOCOL_VERSION` hebben; anders weigert NeoForge de verbinding met een duidelijke melding.

---

## Tests

**B** — `./gradlew build` slaagt, met nieuwe unit tests:
- `RingLayoutTest` (uitbreiden): `noiseSeed` is deterministisch, verschilt per wereldseed, is niet gelijk aan de wereldseed en niet gelijk aan `BiomeManager.obfuscateSeed(worldSeed)`; `settings()`/`noiseSeed()` geven terug wat erin ging.
- `network/RingLayoutPayloadTest`: `STREAM_CODEC`-rondreis via `Unpooled.buffer()` (alle velden gelijk, buffer daarna leeg); een layout gebouwd uit de **gedecodeerde** payload geeft op een raster (incl. punten in elke overgangszone, de void-rand en Nether-coördinaten) exact dezelfde `RingPosition` als de originele layout.
- `network/RingPacketDistributorTest`: `matches` voor `CORE` en `TOUCHING`: midden in de ring, net binnen/buiten de zone, in de zone aan de binnen- en buitenkant, buurring zonder zone.
- `network/ClientboundHandlersTest`: gezette handler wordt aangeroepen door `forward`; zonder handler geen exception.
- `ClientIsolationTest`: slaagt op de huidige code (en faalt aantoonbaar op een tijdelijke testimport, eenmalig met de hand gecontroleerd).

**D** — niet nodig (geen datagen-wijziging; F3-tekst is vaste Engelse tekst).

**S** — `tools/server-smoke-test.sh`: dedicated server start zonder client-class-fouten (`NoClassDefFoundError`, `RuntimeDistCleaner`), log toont geen netwerkregistratiefout. Via RCON (`level-name` nieuw, seed `20260930`): `/ca ring` en `/ca tpboundary 1` werken nog (grens op gewicht ~0,5 met de nieuwe ruis-seed).

**Blokkerende handmatige test: JA, als onderdeel van teststop 1 ná change 20** (niet los na deze change; de gebundelde checklist staat in change 20). Vóór change 19 controleert de bouwer automatisch: `runServer` + `runClient` (quick-play, `Dev`) joint zonder disconnect, client-log toont ontvangst van `ring_layout` en geen fouten. Reden voor blokkerend: of de payload echt aankomt (versie-onderhandeling, verbinding, handler op de client, `Dist`-scheiding bij een echte join) is niet met build, unit tests of server + RCON te controleren, en changes 19, 20, 27 en 28 bouwen rechtstreeks op dit kanaal en op `ClientRingLayout`. De unit tests dekken de logica (codec, gelijke layout); de handtest dekt alleen het echte pad en is kort:
1. `runServer` (dedicated, `corrupted_world`, seed `20260930`, nieuwe `level-name`) + `runClient`, join `localhost`. Geen disconnect, geen fouten in beide logs.
2. F3 toont `CA ring (client): ring_1` bij spawn. Vergelijk met `/ca ring` (server) op: spawn, `/ca tpboundary 1` (beide tonen ring 1 → ring 2 met hetzelfde gewicht, ±0,01), `/ca tpring 3`, `/ca tpboundary 2 nether`, `/ca tpring 6` (void).
3. Uitloggen, in de client een **singleplayer**-wereld met de vanilla-preset openen → F3 toont `—`. Daarna een singleplayer-`corrupted_world` → regel is er weer en klopt met `/ca ring`.

**W-achteraf** (niet blokkerend): versie-mismatch eenmalig proberen (client lokaal `PROTOCOL_VERSION = "2"` → nette NeoForge-disconnectmelding, daarna terugzetten); tweede client tegelijk joinen → beide zien hun eigen correcte F3-regel.

**Checklist (concept)**
- [ ] Scope `infra` benoemd
- [ ] B groen (incl. nieuwe tests en `ClientIsolationTest`)
- [ ] S groen; `/ca ring` via RCON werkt met de nieuwe ruis-seed
- [ ] Automatische join- en payloadcheck (client-log via quick-play) groen
- [ ] Handtest 1–3 opgenomen in teststop 1 (na change 20); akkoord van de gebruiker daar
- [ ] `CODEBASE.md`, `01-techniek.md`, `fase-1-wereld.md` §1.3, backlog, beslissingenlog bijgewerkt
- [ ] `/code-review` gedaan, bevindingen afgehandeld
- [ ] Commit `infra: add network base and sync ring layout to clients`

---

## Risico's

- **Grenzen verschuiven** door de nieuwe ruis-seed (alleen bij optie 1A): oude testwerelden krijgen naden bij nieuwe chunks rond een grens. Mitigatie: altijd verse werelden (al de werkwijze).
- **Scope:** stap 4 (ruis-seed) en stap 5 (`RingWorld` uit `RingWorldBorder` halen) raken code in `ring/` en `worldgen/border/`. Beide zijn minimaal en nodig om de layout veilig te kunnen delen; geen ander worldgen-gedrag verandert.
- **Handlerthread:** als NeoForge 21.1 de handler toch op de netwerkthread draait, is dat veilig (één volatile schrijfactie); latere handlers die wereld- of GUI-staat raken moeten `context.enqueueWork` gebruiken — valkuil vastleggen bij de bouw.
- **Singleplayer-valkuil:** `RingLayout.current()` werkt in singleplayer ook op de client-thread (zelfde JVM) en lijkt dus goed; op een dedicated server crasht het. Mitigatie: regel in `CODEBASE.md`, client-code gebruikt `ClientRingLayout`.
- **Vergeten protocolversie op te hogen** bij een latere payloadwijziging → oude clients joinen en decoderen onzin. Mitigatie: javadoc op `PROTOCOL_VERSION` + valkuil in `CODEBASE.md`; review-checkpunt in 19/20.
- **`ClientIsolationTest` op tekst** (imports) mist volledig gekwalificeerde namen zonder import. Acceptabel als vangnet; de handtest en S dekken de rest.

---

## Open ontwerpvragen

### 1. Hoe krijgt de client de grensruis (de client kent de wereldseed niet)?
- **A — Ruis-seed = SHA-256 van wereldseed + eigen zout, server en client gebruiken die (aanbevolen).** Client rekent `ringAt` exact gelijk aan de server, overal en in beide dimensies; uit de hash is de seed niet terug te rekenen (zelfde niveau als vanilla). Eén kleine ingreep in `RingLayout`; grenzen in bestaande testwerelden verschuiven eenmalig (naden bij nieuwe chunks).
- **B — Huidige afleiding houden en de interne ruis-state syncen.** Geen verschuiving van grenzen; maar die state is met wat moeite terug te rekenen naar de wereldseed (geen cryptografische afleiding): seed-kraken op een server wordt makkelijker. Strijdig met de backlog-afspraak "nooit de seed zelf".
- **C — Geen layout op de client; de server stuurt elke speler alleen zijn eigen ringpositie bij verandering.** Geen seed-risico, geen ingreep in `RingLayout`; maar de client kent alleen de ring onder zijn voeten (genoeg voor HUD, niet voor weer per kolom, grensmuren of een kompas op de client) en er gaat doorlopend verkeer per speler.
- **D — Alleen de radii syncen, client rekent met cirkels zonder ruis.** Simpelst; tot ±96 blokken verkeerd, dus weer/HUD wisselt op de verkeerde plek.

Antwoord: **A** — ruis-seed = eerste 8 bytes van SHA-256(wereldseed ‖ `corruptedadventure:ring_boundary`); server en client bouwen daarmee de layout. Grens 1 (en alle verruisde grenzen) verschuift eenmalig binnen ±96 blokken — door de gebruiker geaccepteerd (2026-10-01).

### 2. Hoe regelen we versiebeheer van het netwerk?
- **A — Eigen protocolnummer (`"1"`), ophogen bij elke payloadwijziging; verplicht voor de client (aanbevolen).** Client en server met dezelfde netwerkversie kunnen samen, ook bij verschillende mod-versies zonder netwerkwijziging; afwijking → nette NeoForge-melding. Vraagt discipline (vastgelegd als valkuil).
- **B — Netwerkversie = `mod_version`.** Nooit vergeten op te hogen, maar elke release (ook zonder netwerkwijziging) maakt client en server onverenigbaar.
- **C — Optionele payloads (`optional()`).** Zou spelers zonder de mod toelaten, maar dat kan toch niet (onze blokken/biomes in de registry-sync); geeft alleen stille fouten bij een versieverschil. Niet zinvol.

Antwoord: **A** — eigen `PROTOCOL_VERSION` (`"1"`), verplicht, ophogen bij elke payloadwijziging (19, 20 en 28 hogen elk één keer op).

### 3. Wie telt als "speler in een ring" voor de helper?
- **A — Beide varianten, keuze per aanroep: `CORE` (ring na de harde wissel op 0,5) en `TOUCHING` (ring zelf of een aangrenzende overgangszone) (aanbevolen).** Ringweer (20/28) kan met `TOUCHING` beide kanten van een zone bedienen voor een vloeiende overgang; gameplay zoals wind-duw kiest `CORE`. Iets meer API, één pure functie getest.
- **B — Alleen `TOUCHING`.** Eén variant; spelers in de zone krijgen altijd beide ringen, gameplay moet zelf nog filteren.
- **C — Alleen `CORE`.** Eén variant, gelijk aan biome en `/ca ring`; weer-rendering in de zone mist dan de staat van de buurring (harde wissel midden in de zone, of change 20 moet alles naar de hele dimensie sturen).

Antwoord: **A** — beide varianten (`CORE` en `TOUCHING`), keuze per aanroep. `TOUCHING` heeft in 18–37 nog geen gebruiker; blijft als getest API-deel bestaan.

### 4. Wat doen ringsystemen in werelden zonder de `corrupted_world`-preset?
- **A — Niets: ring-layout, en later meters en ringweer, alleen in ringwerelden (check `RingWorld.isActive`) (aanbevolen).** Een gewone wereld met de mod erin blijft vanilla; zelfde regel als de world border. Client zonder layout toont geen HUD/weer.
- **B — Overal actief.** De ringen bestaan wiskundig overal; condities en ringweer zouden ook in een vanilla-terreinwereld gelden (uitdroging in een jungle op 2.000 blokken): verwarrend, niet ontworpen.
- **C — Layout altijd sturen met een vlag `ringWorld`; elk systeem beslist zelf.** Flexibel, maar elke latere change moet de vlag zelf goed afhandelen; meer kans op fouten.

Antwoord: **A** — ringsystemen alleen in ringwerelden; `RingWorld.isActive(server)` is de enige preset-check.

### 5. Hoe koppelen we clientbound handlers zonder dat de server client-code laadt?
- **A — Common dispatch-tabel (`ClientboundHandlers`), de client-entrypoint vult hem met zijn handlers (aanbevolen).** Common code noemt nooit een `client/`-class; afdwingbaar met `ClientIsolationTest`; vast patroon voor 19/20 en verder. Eén kleine extra class.
- **B — Lambda in common code: `(p, c) -> ClientPayloadHandlers.handle(p, c)`.** Gangbaar en in de praktijk veilig (de JVM laadt de client-class pas bij uitvoeren), maar common code noemt een client-class; de isolatietest kan dat niet onderscheiden.
- **C — Method reference zoals in de NeoForge-docs (`ClientPayloadHandlers::handle`).** Kortst, maar de JVM laadt de class al bij het registreren, ook op de dedicated server: strijdig met de harde regel.

Antwoord: **A** — common dispatch-tabel `ClientboundHandlers`, gevuld door `CorruptedAdventureClient`; bewaakt door `ClientIsolationTest`.

### 6. Hoe zien we op de client wat hij over de ringen weet?
- **A — Eén F3-regel rechts (`CA ring (client): …`) (aanbevolen).** Altijd bij de hand, direct naast `/ca ring` te vergelijken; herbruikbaar bij het testen van weer-rendering (20/28). Client-only, ~20 regels.
- **B — Client-commando `/caclient ring`.** Expliciet, maar elke keer typen; geen live-weergave tijdens het lopen.
- **C — Alleen een logregel bij ontvangst.** Geen UI-code; bewijst ontvangst, niet dat `ringAt` op de client klopt per positie.

Antwoord: **A** — één F3-regel rechts `CA ring (client): …` in `client/RingDebugOverlay` (enige F3-hook; 20 en 28 voegen regels toe).

---

## Beslissingenlog (in te vullen na de antwoorden, `02-beslissingenlog.md`)

Concept-regels bij de aanbevolen opties (datum = dag van beantwoorden):

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | infra | Grensruis gebruikt een ruis-seed = SHA-256(wereldseed + `corruptedadventure:ring_boundary`); die (niet de seed) gaat naar de client, die zelf `ringAt` rekent | Client exact gelijk aan de server zonder de seed prijs te geven; eenmalige grensverschuiving in testwerelden geaccepteerd |
| 2026-10-01 | infra | Netwerk: eigen `PROTOCOL_VERSION`, verplicht, ophogen bij elke payloadwijziging | Mod-versies zonder netwerkwijziging blijven compatibel; duidelijke melding bij verschil |
| 2026-10-01 | infra | Ring-helper kent `CORE` (harde wissel 0,5) en `TOUCHING` (incl. aangrenzende overgangszone) | Weer kan vloeiend over de zone, gameplay blijft strikt |
| 2026-10-01 | infra | Ringsystemen (layout-sync, later meters en ringweer) alleen in `corrupted_world`-werelden (`RingWorld.isActive`) | Gewone werelden met de mod blijven vanilla; zelfde regel als de world border |
| 2026-10-01 | infra | Clientbound handlers via common dispatch-tabel die de client-entrypoint vult; common code noemt nooit `client/`; `ClientIsolationTest` bewaakt dat | Harde regel dedicated server afdwingbaar; vast patroon voor alle latere payloads |
| 2026-10-01 | infra | Client-debug: F3-regel `CA ring (client)` | Snel vergelijken met `/ca ring`; herbruikbaar voor weer-tests |

---

## Bouwnotities (change 18, 2026-10-01)

- Geverifieerd in NeoForge 21.1.252: `RegisterPayloadHandlersEvent` is een mod-bus-event (`IModBusEvent`); `PayloadRegistrar` draait handlers standaard op de main thread (`HandlerThread.MAIN`); payloads worden ook bij een geheugenverbinding (singleplayer) geserialiseerd.
- Afwijking: 1.21.1 heeft geen `ByteBufCodecs.LONG`, alleen `VAR_LONG`. De ruis-seed gaat als vaste 8 bytes via een eigen `StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong)` (een hash is als var-long meestal langer dan 8 bytes).
- Afwijking: de javadoc van `RingSettingsSnapshot` noemde "sync in change 18" niet (alleen `CODEBASE.md`); er is een zin over de client-sync toegevoegd.
- `ClientIsolationTest` controleert naast imports ook volledig gekwalificeerde namen (elke niet-commentaarregel).
- `RingMembership` is een geneste enum van `RingPacketDistributor` (`RingPacketDistributor.RingMembership`).
- Quick-play: een nieuwe gamemap (`run/client2`) toont eerst het toegankelijkheidsscherm en quick-play wacht daarop; `run/client2/options.txt` met `onboardAccessibility:false` lost dat op (valkuil in `CODEBASE.md`).
