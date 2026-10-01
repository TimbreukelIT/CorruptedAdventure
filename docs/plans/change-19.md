# Change 19 — Meter-systeem voor omgevingscondities

Scope: `conditions` — change 19

**Doel:** het gedeelde fundament voor alle omgevingscondities (uitdroging, hitte, gif, void, kou, stilte): een meter 0–100 per speler per conditie in een player attachment, een server-side tick-handler (elke 20 ticks) die de meters laat oplopen of afnemen, drempel-effecten met waarden uit config, sync naar alleen de eigen client bij verandering, en een HUD-balk (client-only). Er komt nog **geen echte conditie**: een `debug`-conditie, aangestuurd met een debugcommando, test het hele pad. Ring 1 blijft conditievrij (§1.9).

---

## Afstemming (2026-10-01)

Afgestemd met 18, 20, 27 en 28 (basis: de aanbevolen opties). Waar de tekst hieronder iets anders zegt, geldt dit blok.
- **Handlerpatroon van 18 (vervangt de aanname "handler in common code"):** payload `network/ConditionSyncPayload` (common); handler `client/network/ConditionSyncHandler` (`context.enqueueWork` → attachment van de lokale speler), gekoppeld in `CorruptedAdventureClient` via `ClientboundHandlers.set`. Geen client-class in common code; `ClientIsolationTest` (18) bewaakt het.
- **Tweede dev-client** (`client2`, `Dev2`) en quick-play levert change 18; de `[infra]`-backlogregel hieronder vervalt.
- **`ConditionTicker`** draait alleen als `RingWorld.isActive(server)` (18).
- **Drempels generiek (N per conditie):** `ConditionDefinitions` houdt per conditie een lijst drempels; de config-sectie per conditie krijgt `threshold1..N` volgens die lijst (debug 3, uitdroging 4 in change 27). Change 27 hoeft niets te generaliseren.
- **`DamageThreshold(damageType, amount, intervalSeconds, DifficultyFloor floor)`** direct in deze change: `NONE` (altijd), `VANILLA_STARVATION` (peaceful geen schade, easy tot 10 HP, normal tot 1 HP, hard dodelijk), `NEVER_LETHAL` (stopt op 1 HP). Debug: interval 1, `NONE`. Welke floor een conditie gebruikt beslist die conditie (change 27 vraag 6). **Vraag 6 optie D vervalt** (verplaatst naar 27 vraag 6).
- **Modifier-registratie:** `ConditionDefinitions.addModifier(RateModifier)` (common, aanroepbaar vanuit `weather/`), volgorde = registratievolgorde. Change 28 hangt zijn hittegolf-modifier zo in zonder `conditions` te raken.
- **`ConditionContext` minimaal:** alleen `player`, `level`, `position` (`RingPosition`), `biome`. Afgeleide checks (`isDay`/`isSunUp`, `canSeeSky`, water) voegt de eerste gebruiker toe (change 27), zodat hun betekenis daar beslist wordt (27 vraag 2, 3, 7). **Geen weer in de context** (28 leest de weerstaat zelf).
- **`config/ServerConfig` (bij vraag 4 A) is de enige plek voor alle balanswaarden:** condities hier, ringweer (`[weather]`, `[weather.ring2..5]`) in change 20 en de effecten van change 28.
- **Teststop (besloten 2026-10-01, teststop 1 van 3):** de blokkerende HUD-check is **onderdeel van teststop 1 ná change 20** (gebundeld met 18 en 20; dedicated server + `Dev` + `Dev2`; checklist in change 20). De RCON-stappen 1–7 en de automatische join- en payloadcheck in de client-logs (twee clients via quick-play) blijven per change verplicht vóór change 20.

---

## Afhankelijkheden en aannames

- **Change 18 (`infra`, netwerk-basis)** wordt parallel gepland en is nog niet gebouwd. **Aangenomen namen — bij de start controleren in `CODEBASE.md` en het plan/de code van change 18, en dit plan daarop aanpassen:**
  - `nl.timtoys.corruptedadventure.network.ModNetwork` met `register(IEventBus modBus)`, luistert naar `RegisterPayloadHandlersEvent` en maakt `event.registrar(PROTOCOL_VERSION)`. Nieuwe payloads worden daar met één regel aangehaakt (`registrar.playToClient(TYPE, STREAM_CODEC, handler)`). *(aanname)*
  - Een vast patroon voor client-handlers dat **geen client-class** op de server laadt. Dit plan gaat uit van: de handler staat in common code, doet `context.enqueueWork(...)` en schrijft alleen in de **attachment van `context.player()`** (op de client is dat de lokale speler); de HUD in `client/` leest die attachment. Kiest change 18 een ander patroon (bijv. een indirectie naar `client/`), dan volgt deze change **dat** patroon. *(aanname)*
  - Een helper "stuur naar spelers in ring N" (`ModNetwork.sendToPlayersInRing` of vergelijkbaar) is hier **niet** nodig: meters gaan alleen naar de eigen speler via NeoForge's `PacketDistributor.sendToPlayer`.
  - **Tweede dev-client:** "twee spelers zien elk hun eigen meter" vraagt twee clients met verschillende gebruikersnamen tegen `runServer` (`online-mode=false`). Twee keer `runClient` met dezelfde naam (`Dev`) kickt de eerste. Een extra run (`client2`, eigen `gameDirectory`, `--username Dev2`) in `build.gradle` is `infra`. **Aanname:** change 18 levert die (ook change 28 heeft hem nodig). Zo niet: in `docs/backlog.md` [infra] en de hoofdsessie laten beslissen vóór de test van deze change (zie Tests).
- **Change 20 (ringweer)** komt ná deze change: de context krijgt in change 19 nog **geen** weerstaat; change 28 voegt het weer-veld en de vermenigvuldiger toe (zie "Latere gebruikers").
- Bestaand en gecontroleerd in de code:
  - `RingLayout.ringAt(dimension, x, z)` (static, via `current()`, alleen zolang er een server draait) en `RingPosition` (`ring()`, `inTransition()`, `blend`). De tick-handler draait altijd op de server, dus `current()` is veilig.
  - `CaCommand` (`/ca`-root, permissieniveau 2) — nieuwe subcommando's met `.then(...)` aanhaken.
  - `CommonConfig` (COMMON, alleen ringgrenzen; "🔲 balanswaarden" in `CODEBASE.md`) en de registratie in `CorruptedAdventure` (`modContainer.registerConfig(...)`).
  - `CorruptedAdventureClient` (`@Mod(dist = CLIENT)`, krijgt de mod-bus): daar de HUD-layer registreren.
  - De packages `condition/`, `network/`, `data/` staan als 🔲 in `CODEBASE.md`; deze change vult `condition/` en zet de attachment in `registry/ModAttachments`.
- Geen worldgen, geen blokken, geen datagen nodig (commando-tekst is vast Engels, de HUD heeft geen tekst). Tenzij een antwoord hieronder iets anders vraagt.

---

## Plan

### 1. Model (common, package `condition/`)

- **`Condition`** — enum (vraag 1), nu alleen `DEBUG` (`debug`). `StringRepresentable` + `CODEC` op de naam (opslag op naam, dus volgorde-onafhankelijk) en `STREAM_CODEC` (index). Per conditie pure data: HUD-kleur (int ARGB). Latere changes voegen waarden toe; **voorgestelde namen** (beslissen die changes zelf): `dehydration` (27), `heat` (43), `cold` (55), `toxin` (65; niet `poison`, om verwarring met het vanilla-effect te voorkomen), `void` (84), `silence` (93).
- **`ConditionMeters`** — de attachment-data: per `Condition` een `float` 0–100 (fractionele snelheden zoals 0,25/s moeten kunnen), plus de laatst gesyncte afgeronde waarden (niet opgeslagen). Methodes `get`, `set`, `add` (klemmen 0–100), `roundedChanged()` / `markSynced()`. Codec: map naam → float, **lenient**: onbekende namen (een conditie die later hernoemd/verwijderd wordt) worden overgeslagen, niet de hele attachment verworpen (`Codec.unboundedMap` faalt op een onbekende sleutel — eigen decode of `Codec.simpleMap` met `StringRepresentable.keys`; bij de bouw kiezen en testen).
- **`ConditionContext`** — record, één keer per speler per tick gebouwd en gedeeld door alle condities: `ServerPlayer`, `ServerLevel`, `RingPosition` (via `RingLayout.ringAt(level.dimension(), x, z)`), `Holder<Biome>` op de voeten. Afgeleide checks als methodes (alleen berekend als een regel ze vraagt): `isDay()`, `canSeeSky()` (op ooghoogte), `isInWater()`. Later: weerstaat (change 28).
- **`ConditionRule`** — interface `float ratePerSecond(ConditionContext ctx)`: positief = oplopen, negatief = afnemen. Standaardregel `ConditionRule.recover(fallRate)` = altijd `-fallRate` ("buiten de zone zakt de meter", zoals §2 Ring 5 "daalt ook buiten ring 5"). Elke conditie heeft precies één regel.
- **`RateModifier`** — interface `float modify(Condition c, ConditionContext ctx, float rate)`. Hier komen later alle vermenigvuldigers (ringweer ×1,5/×2, fire resistance ÷2, leren armor, Nether-4 zuiverend, spelers in de buurt, fase 2-gear). In deze change nog **geen** modifiers; alleen het haakje.
- **`ThresholdEffect`** — interface `void apply(ServerPlayer p)` / `void clear(ServerPlayer p)` (clear = als de meter onder de drempel zakt). Twee generieke implementaties nu:
  - `MobEffectThreshold(Holder<MobEffect>, amplifier)` — vraag 3.
  - `DamageThreshold(ResourceKey<DamageType>, amount)` — schade per tick van de handler (dus per seconde). Debug gebruikt vanilla `minecraft:generic`; eigen damage types met doodsberichten per conditie komen in hun eigen change (27, 43, …).
  - Later eigen implementaties (in brand vliegen 43, `ticksFrozen` 55, teleport 84, darkness 84/93).
- **`ConditionDefinitions`** (naam bij de bouw: één klasse die per `Condition` de regel en de drempels bijeenhoudt, geen Utils-dumpbak) — `rule(c)`, `thresholds(c)` (lijst `Threshold(level, ThresholdEffect)` gesorteerd), `modifiers()`. Drempelwaarden en sterktes komen uit config (§3); **welk** effect bij welke drempel hoort staat in Java (vraag 4).

### 2. Tick-handler (common, server-side)

- **`ConditionTicker`** op de game-bus: `PlayerTickEvent.Post`, alleen `ServerPlayer` (geen `FakePlayer`), alleen als `player.tickCount % 20 == 0` (spreidt de last vanzelf over spelers). Overslaan: dood, spectator. Creatief: vraag 6.
- Per tick:
  1. Context bouwen (één `ringAt` + één biome-lookup).
  2. Per conditie: `rate = rule.ratePerSecond(ctx)` → alle `RateModifier`s op volgorde → `meters.add(c, rate)` (klemmen 0–100).
  3. Drempels toepassen volgens vraag 3 (cumulatief: alle drempels ≤ meter actief; drempels die net niet meer gehaald worden → `clear`).
  4. Is een afgeronde waarde veranderd → sync (§4).
- Pure rekenstappen (rate-pijplijn, klemmen, drempelselectie, "afgerond veranderd") in kleine pure functies, zodat ze zonder server te unit-testen zijn.
- **Instant-API** voor latere item-hooks en het commando: `PlayerConditions.add(ServerPlayer, Condition, float)` / `set(...)` / `get(...)` → past de attachment aan en synct direct (waterfles −30 in 27, melk −40 en honing −20 in 65).

### 3. Config (balanswaarden)

Vorm volgens vraag 4. Aanbevolen: nieuwe **SERVER-config** `config/ServerConfig.java` (`serverconfig/corruptedadventure-server.toml` per wereld; NeoForge synct SERVER-configs bij het joinen naar de client, zodat de HUD de drempelstreepjes kan tekenen). Per conditie een sectie; nu alleen `conditions.debug`:

| Sleutel | Standaard | Betekenis |
|---|---|---|
| `riseRate` | 5,0 | punten per seconde als het debugcommando de meter laat oplopen |
| `fallRate` | 5,0 | punten per seconde afname (ook de standaard-afname buiten de zone) |
| `threshold1` / `effect1Amplifier` | 50 / 0 | traagheid I |
| `threshold2` / `effect2Amplifier` | 75 / 0 | zwakte I |
| `threshold3` / `damage` | 100 / 1,0 | schade per seconde |

Getallen `defineInRange` (drempel 1–100, oplopend gevalideerd; ongeldig → NeoForge zet de standaard terug en logt). Registratie in `CorruptedAdventure` naast de COMMON-config. `CommonConfig` blijft alleen ringgrenzen.

### 4. Attachment, persistentie en sync

- **`registry/ModAttachments`** — `DeferredRegister<AttachmentType<?>>` op `NeoForgeRegistries.ATTACHMENT_TYPES`; `CONDITION_METERS` = `AttachmentType.builder(ConditionMeters::new).serialize(ConditionMeters.CODEC)` (+ `copyOnDeath()` alleen als vraag 6 dat kiest). Registratie in `CorruptedAdventure`. Blijft bewaard bij uitloggen (speler-NBT).
- **`network/ConditionSyncPayload`** — `CustomPacketPayload`, server → client, `StreamCodec` met **alle** meters als afgeronde ints (0–100; nu 1 byte, straks 7: altijd een volledige momentopname, geen delta's — klein en robuust tegen gemiste pakketten). Registratie in `ModNetwork` (change 18).
  - Verzenden: `PacketDistributor.sendToPlayer(player, …)`, **alleen naar de eigen speler**; alleen als een afgeronde waarde veranderd is (dus max. 1 pakket per seconde per speler).
  - Volledige sync ook bij `PlayerLoggedInEvent`, `PlayerRespawnEvent` en `PlayerChangedDimensionEvent`: de client maakt bij respawn en dimensiewissel een nieuwe `LocalPlayer` aan, en diens (client-)attachment is dan leeg.
  - Handler (common, volgens het patroon van change 18): `context.enqueueWork(() -> context.player().getData(CONDITION_METERS).applySync(values))`. Geen client-class, geen `Minecraft.getInstance()`.
- Andere spelers krijgen andermans meters nooit (niet nodig; stilte-"samen sterker" in change 93 rekent op de server).

### 5. HUD (alleen client, `client/hud/`)

- **`ConditionHudLayer`** — `LayeredDraw.Layer`, geregistreerd in `CorruptedAdventureClient` via `RegisterGuiLayersEvent` (`registerAbove(VanillaGuiLayers.AIR_LEVEL, id("conditions"), …)`). Leest `Minecraft.getInstance().player.getData(CONDITION_METERS)`.
- Weergave volgens vraag 5. Aanbevolen: per meter > 0 een balk van 81 × 5 px (breedte van de hongerrij) rechts boven de hongerbalk/luchtbellen, gestapeld via NeoForge's `gui.rightHeight` (zodat hij meeschuift met luchtbellen en rijdier-hartjes). Donkere achtergrond, vulling in de conditiekleur, 1-px-streepjes op de drempels (uit de gesyncte SERVER-config). Verborgen met F1, in spectator en (zoals de hongerbalk) in creatief.
- **Placeholder-look:** alleen `GuiGraphics.fill`, geen textures. Definitieve balk/iconen per conditie → backlog [textures].
- API's (`RegisterGuiLayersEvent`, `VanillaGuiLayers.AIR_LEVEL`, `Gui#rightHeight`) bij de bouw verifiëren in NeoForge 21.1.252.

### 6. Debugcommando en test-conditie

- **`command/ConditionCommand`**, aangehaakt in `CaCommand`, vaste Engelse tekst, werkt via RCON:
  - `/ca condition get [<targets>]` — alle meters per speler (één decimaal).
  - `/ca condition set <targets> <condition> <0–100>` — via de instant-API (synct direct). Bruikbaar voor élke latere conditie (drempels testen in 27, 43, …).
  - `/ca condition debug <targets> <rise|fall|off>` — stuurt de `debug`-conditie (vraag 2).
- **`condition/DebugConditionDriver`** — server-side, niet opgeslagen: `UUID → rise|fall|off`, gewist bij `ServerStoppedEvent` en bij uitloggen. Regel van `DEBUG`: `rise` → `+riseRate`, anders → `-fallRate`. Zonder commando loopt de debug-meter dus nooit op.

### 7. Documentatie

- `CODEBASE.md`: `condition/` ✅ met de nieuwe classes; `registry/ModAttachments`; `network/ConditionSyncPayload`; `config/ServerConfig`; `client/hud/ConditionHudLayer`; `command/ConditionCommand`; tabel "Netwerk en persistente staat" (attachment `condition_meters`, payload `condition_sync`); valkuilen (resync na respawn/dimensiewissel; lenient codec; SERVER-config op de client pas leesbaar in een wereld; effect-icoon knippert onder 200 ticks — zie vraag 3); sectie "Latere gebruikers" van het meter-systeem kort (welke haak voor wat).
- `fase-1-wereld.md` §1.9: uitwerking (rate-pijplijn, cumulatieve drempels, sync-regel, HUD-plek, dood/creatief, debugcommando).
- `02-beslissingenlog.md`: zie onderaan.
- `docs/backlog.md`: [textures] definitieve HUD-balk + iconen per conditie; [infra] tweede dev-client-run (als change 18 die niet levert).

---

## Latere gebruikers (ontwerp-check, niet bouwen)

| Change | Gebruikt | Haak in change 19 |
|---|---|---|
| 27 uitdroging | ring 2 + zon/schaduw/nacht/water/oase; waterfles −30; honger → traagheid → mining fatigue → lichte schade | `ConditionRule` (context: ring, biome, `isDay`, `canSeeSky`, `isInWater`), `PlayerConditions.add`, `MobEffectThreshold`, `DamageThreshold` |
| 28 hittegolf ×2 | uitdroging sneller tijdens ringweer | `RateModifier` in `weather/` die de weerstaat zelf leest (`RingWeather.state`), geregistreerd via `ConditionDefinitions.addModifier`; **geen** weer-veld in `ConditionContext` (afstemming 2026-10-01) |
| 43 hitte / 45 asregen, uitbarsting | hittebronnen ~4 blokken, fire resistance ÷2, ×1,5/×2, in brand vliegen | regel, `RateModifier`s, nieuwe `ThresholdEffect` (ignite) |
| 55 kou (Nether-3) | leren armor remt, `ticksFrozen` | `RateModifier`, nieuwe `ThresholdEffect` (freeze); werkt in de Nether via `ringAt(NETHER, …)` |
| 65 gif / 66 gifregen | melk −40, honing −20, gif/wither | instant-API, `MobEffectThreshold` |
| 74 zuiverend (Nether-4) | **alle** meters zakken extra snel | één globale `RateModifier` op negatieve rates; de pijplijn draait voor elke conditie, ook buiten haar zone |
| 84 void / 85 ringweer 5 | licht ≥ 12 remt, teleport, darkness; glitch-visuals client | regel, nieuwe `ThresholdEffect` (teleport); client leest de gesyncte meter |
| 93 stilte (Nether-5) | spelers binnen ~8 blokken remmen, game events, HUD verbergen | `RateModifier` (server); client leest de meter |
| Fase 2 gear | remt opbouw | `RateModifier` |

---

## Client/server-impact

- **Server:** alle logica (tick, regels, drempels, effecten, schade, opslag) in common code, alleen uitgevoerd voor `ServerPlayer`. Kost per speler per seconde één `ringAt` + één biome-lookup + een paar floats.
- **Client:** alleen `client/hud/ConditionHudLayer` (geregistreerd vanuit `CorruptedAdventureClient`, `Dist.CLIENT`). De payload-handler staat in common code en raakt geen client-class (schrijft in de attachment van de speler uit de context). Dedicated server laadt geen client-class: gecontroleerd met test S.
- **Netwerk:** één S2C-payload, alleen naar de eigen speler, alleen bij verandering (≤ 1/s per speler) + bij login/respawn/dimensiewissel. SERVER-config wordt door NeoForge zelf gesynct.
- **Persistentie:** player attachment (speler-NBT). Geen `SavedData`.
- Geen invloed op worldgen of determinisme.

---

## Tests

- **B** — `./gradlew build` slaagt, met unit tests:
  - `condition/ConditionMetersTest`: klemmen 0–100, `add`/`set`, "afgerond veranderd" (0,4 → 0,6 = sync; 0,6 → 0,9 = geen sync), codec-rondreis, onbekende conditienaam wordt overgeslagen (rest blijft).
  - `condition/ConditionTickerTest` (pure delen): rate-pijplijn (regel → modifiers op volgorde → klemmen), drempelselectie cumulatief (49 → geen, 50 → 1, 100 → 3; zakken 76 → 74 = drempel 2 `clear`).
  - `network/ConditionSyncPayloadTest`: `StreamCodec`-rondreis.
  - Config: drempels oplopend gevalideerd.
- **D** — niet nodig (geen datagen), tenzij een antwoord lang-keys of een damage type toevoegt.
- **S** — `tools/server-smoke-test.sh`: server start zonder client-class-fouten. Daarna server met RCON + `online-mode=false`, twee clients (`Dev`, `Dev2`) joinen:
  1. RCON `ca condition debug Dev rise` → `ca condition get Dev` loopt ~5/s op; `ca condition get Dev2` blijft 0.
  2. Bij ≥ 50: `Dev` heeft traagheid (`execute if entity Dev[nbt=…]` of `effect`-feedback / `data get entity Dev active_effects`), bij ≥ 75 ook zwakte, bij 100 schade (`data get entity Dev Health` daalt). `Dev2` niets.
  3. `debug Dev fall` → meter zakt, effecten verdwijnen direct onder hun drempel.
  4. `ca condition set Dev2 debug 60` → alleen `Dev2` krijgt traagheid.
  5. Persistentie: `Dev` uitloggen en weer in → waarde gelijk; server herstarten (RCON `stop`) → waarde gelijk. Dood (`kill Dev`) → gedrag volgens vraag 6.
  6. Dimensiewissel (`execute in minecraft:the_nether run tp Dev …` en terug) → meter blijft, client krijgt een volledige sync (client-log, zie hieronder).
  7. Payload-handler logt op DEBUG de ontvangen waarden; de agent controleert in beide client-logs dat elke client **alleen** zijn eigen waarden ontvangt en dat er geen fouten/kicks ("Disconnected", "unknown payload") zijn.
- **Handmatig, BLOKKEREND als onderdeel van teststop 1 ná change 20 (≈ 5 min, gebruiker kijkt mee bij stap 1–6; niet los na deze change):**
  - [ ] HUD: `Dev` ziet zijn eigen balk vollopen, `Dev2` ziet bij `Dev`'s meter niets en alleen zijn eigen balk bij stap 4.
  - [ ] Balk staat boven de hongerbalk, schuift mee met luchtbellen (onder water), streepjes op 50/75/100, verdwijnt bij 0.
  - [ ] Verborgen met F1, in spectator en (volgens vraag 6) in creatief.
  - [ ] Na respawn en dimensiewissel klopt de balk direct (geen lege balk tot de volgende verandering).
  - [ ] Effect-iconen knipperen niet (vraag 3).
  
  **Waarom blokkerend:** de client-helft (payload → client-attachment → HUD-layer, resync na respawn/dimensiewissel, per-speler-isolatie op het scherm) is niet met build, unit tests of server + RCON te controleren, en alle latere conditie-changes (27, 28, 43, 55, 65, 74, 84, 93) bouwen rechtstreeks op dit pad en deze HUD-layer. Een fout die pas in change 27 opvalt, betekent een `conditions`-fix midden in een andere change. De agent bereidt alles voor (server, twee clients, RCON-script, logcheck); de gebruiker kijkt alleen naar de HUD.
- **W** — n.v.t. (geen worldgen; de debug-conditie hangt niet af van ring of biome). Geen W-achteraf nodig.

**Checklist (concept)**
- [ ] Scope `conditions` benoemd; namen van change 18 gecontroleerd en dit plan aangepast
- [ ] B groen (incl. nieuwe unit tests)
- [ ] S: server start, RCON-stappen 1–7 groen
- [ ] Automatische join- en payloadcheck in beide client-logs groen (RCON-stap 7)
- [ ] Handmatige HUD-check opgenomen in teststop 1 (na change 20); akkoord van de gebruiker daar
- [ ] `CODEBASE.md`, `fase-1-wereld.md` §1.9, beslissingenlog, backlog bijgewerkt
- [ ] `/code-review` gedaan, bevindingen afgehandeld
- [ ] Commit `conditions: add condition meter system with debug condition and HUD`

---

## Risico's

- **Client-handler laadt een client-class** (bijv. een method reference naar een klasse in `client/` bij de registratie): server crasht pas bij `runServer`. Mitigatie: handler in common, schrijft alleen in de attachment; test S.
- **Lege HUD na respawn/dimensiewissel:** nieuwe `LocalPlayer` = lege client-attachment. Mitigatie: volledige sync op die drie events; handmatige check.
- **Knipperende effect-iconen:** vanilla laat een effect-icoon knipperen als er < 200 ticks over zijn. Korte duur + elke seconde verversen = constant knipperen. Zie vraag 3.
- **Effect opruimen raakt een drankje:** `removeEffect` haalt het hele effect weg, ook een onderliggend drankje van hetzelfde type. Alleen weghalen als het actieve exemplaar "van ons" is (ambient); onze effecten (traagheid, zwakte, honger, mining fatigue, misselijkheid, gif, wither, darkness) komen niet van bakens. Kleine rest-kans geaccepteerd.
- **Meter schommelt rond een drempel:** effect gaat elke seconde aan/uit. Geaccepteerd (geen hysterese), tenzij de W-tests van latere condities het storend vinden.
- **Codec faalt op onbekende conditie:** een hernoemde conditie zou alle meters van een speler wissen. Mitigatie: lenient codec + test.
- **SERVER-config op de client** is pas geladen in een wereld; lezen in het hoofdmenu gooit. De HUD tekent alleen met een speler in een wereld.
- **Tweede dev-client ontbreekt** (zie aannames): zonder kan de isolatie-test niet; dan eerst [infra] regelen.
- **Afhankelijkheid change 18:** wijken namen of het handlerpatroon af, dan volgen we change 18 (niet andersom).

---

## Doorgeschoven (naar `docs/backlog.md`)

- [textures] Definitieve HUD-balk en icoon per conditie (nu `fill`-placeholder).
- [infra] Tweede dev-client-run (`client2`, eigen `gameDirectory`, `--username Dev2`) — alleen als change 18 hem niet levert.
- [conditions] Eigen damage types + doodsberichten per conditie (in de change van die conditie, niet hier).

---

## Open ontwerpvragen

Ronde 1 (vraag 1–4), ronde 2 (vraag 5–6).

### 1. Hoe modelleren we condities?
- **A. Java-enum `Condition` + per conditie een regel-object in Java (aanbevolen).** Eenvoudigst; vaste set van 6 (+ debug) condities die allemaal in deze mod zitten. Opslag op naam, sync op index (client en server hebben altijd dezelfde mod-versie). Nieuwe conditie = enumwaarde + regel. Geen extra registry-werk.
- **B. Eigen NeoForge-registry `corruptedadventure:condition`.** Netjes uitbreidbaar (ook door addons), registry-sync en codecs gratis; meer boilerplate (`NewRegistryEvent`, `DeferredRegister`) voor een set die niet groeit buiten de mod.
- **C. Datapack-gedreven condities (JSON).** Regels zijn te veel Java-gedrag (zon, water, licht, game events) om in data te vangen; hoge complexiteit, weinig winst.

Antwoord: **A** — Java-enum `Condition` (nu alleen `DEBUG`) + per conditie één regel-object in Java; opslag op naam, sync op index.

### 2. Hoe ziet de test-conditie eruit?
- **A. Blijvende `debug`-conditie, alleen actief via `/ca condition debug <speler> rise|fall|off` (aanbevolen).** Test het hele pad (opbouw, afbouw, drempels, sync, HUD) zonder ring of biome; blijft bruikbaar als debugtool. Komt nooit vanzelf op. Plus `/ca condition set/get` voor alle latere condities.
- **B. Tijdelijke test-conditie die change 27 weer verwijdert.** Geen "debug" in de code op termijn, maar dan moet 27 een stuk van 19 weghalen (en de lenient codec opvangen).
- **C. Geen test-conditie, alleen `/ca condition set`.** Minste code, maar opbouw/afbouw via de tick-handler wordt pas in change 27 echt getest.

Antwoord: **A** — blijvende `debug`-conditie, alleen actief via `/ca condition debug <speler> rise|fall|off`; plus `/ca condition get/set` voor alle latere condities.

### 3. Hoe werken drempel-effecten?
Uitgangspunt alle opties: cumulatief (bij 75 heb je ook het effect van 50), zoals §1.9 "oplopende effecten".
- **A. Elke seconde verversen met lange duur (~11 s), ambient, zonder particles; onder de drempel haalt de server "ons" effect direct weg (aanbevolen).** Icoon knippert niet (vanilla knippert onder 200 ticks), effect stopt meteen als de meter zakt, melk wist het maar de volgende seconde komt het terug. Iets meer code (opruimen), kleine kans een gelijk drankje mee te wissen.
- **B. Elke seconde verversen met korte duur (~3 s), niet opruimen.** Eenvoudigst; maar het icoon knippert continu en het effect blijft tot 3 s na het zakken.
- **C. Alleen de hoogste gehaalde drempel actief (niet cumulatief).** Bij 75 alleen zwakte, geen traagheid meer; minder "oplopend", wijkt af van het design.
- **D. Oneindig effect bij overschrijden, weghalen bij zakken.** Geen verversen; botst met melk (wist het tot de volgende overschrijding) en met drankjes van hetzelfde type.

Antwoord: **A** — cumulatief; elke seconde verversen met lange duur (~11 s), ambient, zonder particles; onder de drempel haalt de server "ons" effect direct weg.

### 4. Waar staan de balanswaarden?
- **A. Nieuwe SERVER-config (per wereld), alleen getallen; welk effect bij welke drempel staat in Java (aanbevolen).** Per wereld instelbaar (`serverconfig/`), door NeoForge naar de client gesynct (HUD-streepjes kloppen altijd), geen kans op ongeldige effect-namen. Effect wisselen = code.
- **B. SERVER-config met vrije effectlijst als tekst (`"50:minecraft:slowness:0"`) naast Java-acties (schade, vuur, teleport).** Admins kunnen effecten wisselen zonder code; wel parsen, valideren en twee soorten drempels uitleggen.
- **C. COMMON-config, alleen getallen (naast de ringgrenzen).** Eén configbestand; geldt voor alle werelden tegelijk, niet gesynct (client kent de drempels niet voor de HUD), en geen snapshot per wereld zoals bij de ringgrenzen.
- **D. Datapack-JSON per conditie.** Herlaadbaar met `/reload`, per wereld; meer code (reload-listener, codec, sync naar de client) voor waarden die zelden wijzigen.

Antwoord: **A** — nieuwe SERVER-config `config/ServerConfig` (per wereld, door NeoForge gesynct), alleen getallen; welk effect bij welke drempel staat in Java. Dezelfde config bevat ook het ringweer (`[weather]`, change 20) en de effecten van change 28.

### 5. Hoe en waar toont de HUD de meters?
Placeholder-look in alle opties (`fill`, geen textures; definitief → backlog [textures]).
- **A. Een balk per meter > 0, rechts boven de hongerbalk, gestapeld met luchtbellen, met drempelstreepjes (aanbevolen).** Vanilla-gevoel (zoals luchtbellen: verschijnt alleen als het speelt), schuift mee met andere rijen; bij meerdere actieve meters (bijv. hitte die nog zakt in ring 4) meerdere rijen.
- **B. Alleen de meter van de conditie van de huidige ring (+ meters die nog zakken).** Rustiger; maar een meter die buiten de zone nog hoog is (gevaarlijk) kan uit beeld raken als we dat verkeerd afbakenen.
- **C. Vaste plek linksboven met naam en getal per meter.** Duidelijkst voor debuggen, maar voelt niet vanilla en vraagt lang-keys; botst met F3 en mods met eigen overlays.

Antwoord: **A** — een balk per meter > 0, rechts boven de hongerbalk, gestapeld via `gui.rightHeight`, met drempelstreepjes; placeholder-look met `fill`.

### 6. Wat gebeurt er bij dood en in creatief?
- **A. Bij dood alle meters naar 0; in creatief/spectator geen opbouw (wel afbouw) en geen effecten; bewaard bij uitloggen en dimensiewissel (aanbevolen).** Geen doodsspiraal (respawnen met meter 100 = direct weer schade), zoals vanilla honger na dood. Bouwers in creatief worden niet gestoord. Moeilijkheidsgraad heeft geen invloed (ook peaceful).
- **B. Zoals A, maar meters blijven bij dood (`copyOnDeath`).** Strenger en "horror"; risico op een doodsspiraal als de spawn in een conditiezone ligt (bed in ring 2–5).
- **C. Zoals A, maar meters halveren bij dood.** Middenweg; iets meer code, minder voorspelbaar voor spelers.
- ~~**D. Zoals A, maar in peaceful geen schade-drempels.**~~ *Vervallen (afstemming 2026-10-01): `DamageThreshold` krijgt in deze change een generieke `DifficultyFloor` (`NONE`, `VANILLA_STARVATION`, `NEVER_LETHAL`); welke floor een conditie gebruikt beslist die conditie (eerst: change 27 vraag 6).*

Antwoord: **A** — bij dood alle meters naar 0 (geen `copyOnDeath`); in creatief/spectator geen opbouw (wel afbouw) en geen effecten; bewaard bij uitloggen en dimensiewissel.

---

## Beslissingenlog (bij de bouw invullen, één regel per antwoord)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | conditions | Condities als Java-enum `Condition` + regel-object per conditie; per conditie één regel + `RateModifier`-pijplijn (rule → modifiers → klemmen) en `ThresholdEffect`s | Vaste set condities binnen de mod; geen registry-boilerplate |
| 2026-10-01 | conditions | Test-conditie: blijvende `debug`-conditie, alleen via commando aangestuurd; debugcommando `/ca condition get/set/debug` | Test het hele pad zonder ring; blijft bruikbaar als debugtool |
| 2026-10-01 | conditions | Drempel-effecten cumulatief, elke seconde ververst met ~11 s duur (ambient, geen particles), direct opgeruimd onder de drempel | Geen knipperende iconen; effect stopt meteen bij zakken |
| 2026-10-01 | conditions | Balanswaarden condities (en ringweer) in SERVER-config `config/ServerConfig`, alleen getallen; effectkeuze in Java | Per wereld instelbaar, naar de client gesynct voor de HUD-streepjes |
| 2026-10-01 | conditions | HUD: balk per meter > 0 rechts boven de hongerbalk met drempelstreepjes; placeholder-look, definitief in een `textures`-change | Vanilla-gevoel; verschijnt alleen als het speelt |
| 2026-10-01 | conditions | Dood/creatief: meters naar 0 bij dood; geen opbouw en geen effecten in creatief/spectator | Geen doodsspiraal; bouwers niet gestoord |
| 2026-10-01 | conditions | Meters gesynct als volledige momentopname, alleen naar de eigen speler, alleen bij verandering van de afgeronde waarde + bij login/respawn/dimensiewissel | Klein, robuust tegen gemiste pakketten; HUD na respawn direct correct |

---

## Bouwnotities (change 19, 2026-10-01)

- Aannames over change 18 klopten: `ModNetwork.register(PayloadRegistrar)` (één regel per payload), `ClientboundHandlers.forward/set`, `RingWorld.isActive`, `client2` + `-PquickPlay`. Handlerpatroon volgens de afstemming (handler in `client/network/`). `PROTOCOL_VERSION` → `"2"`.
- **Afwijking config-locatie:** NeoForge 21.1 zet SERVER-configs standaard in `config/corruptedadventure-server.toml` (globaal); `<wereld>/serverconfig/` is alleen een override-map per wereld. Het plan (en plan 20) noemde `serverconfig/…` als standaardplek. Gedrag "per wereld instelbaar" blijft (via de override), de standaard geldt voor alle werelden. Op de client is de config ook in het hoofdmenu geladen (standaardwaarden) en wordt hij bij het joinen vervangen door de servercopy.
- Handler zonder `enqueueWork`: payload-handlers draaien al op de main thread (geverifieerd in 18); zelfde als `RingLayoutHandler`.
- `Condition` heeft geen `STREAM_CODEC`: de payload stuurt alle meters op enum-volgorde, er wordt nergens één conditie over het netwerk gestuurd.
- Effectduur 240 ticks (12 s) i.p.v. ~11 s: na 20 ticks staat er nog 220 over, ruim boven vanilla's knippergrens van 200.
- Extra configsleutel `damageInterval` (1 s) voor de schade-drempel (`DamageThreshold.intervalSeconds`, afstemming). Drempels moeten **strikt** oplopen; anders standaardwaarden + error-log.
- `DifficultyFloor.VANILLA_STARVATION` sluit peaceful expliciet uit (vanilla's formule zelf zou boven 10 HP ook in peaceful schaden; daar raakt honger alleen nooit op).
- Drempel-opruiming: per speler per conditie het aantal toegepaste drempels (transient). Na laden "onbekend" → de eerste tick wist alle niet-bereikte drempels (effecten worden met de speler opgeslagen). Eerst alle clears van alle condities, dan alle applies. `clear` haalt een effect alleen weg als het actieve exemplaar ambient is (na review: geen amplifier-check meer, zodat een live gewijzigde amplifier niet blijft hangen). Bekende beperking: een zwakker drankje onder ons sterkere effect (vanilla `hiddenEffect`) verdwijnt mee.
- Instant-wijzigingen (`/ca condition set`, latere items) syncen direct; de drempeleffecten volgen bij de volgende tick (≤ 1 s).
- Volledige sync bij login/respawn/dimensiewissel ook in niet-ringwerelden (meters zijn spelerdata); alleen de ticker is aan `RingWorld.isActive` gebonden.
- `debug fall` en `debug off` gedragen zich hetzelfde (−`fallRate`); `off` haalt de speler alleen uit de map.

### Testresultaten
- **B:** `./gradlew build` groen, 122 tests (nieuw: `ConditionMetersTest`, `ConditionTickerTest`, `DamageThresholdTest`, `ConditionSyncPayloadTest`, `ThresholdLevelsTest`); `ClientIsolationTest` groen.
- **D:** n.v.t. (geen datagen).
- **S:** `tools/server-smoke-test.sh` met `level-name=smoke-change19`: `Done (5.5 s)`, geen fouten. Daarna RCON + `Dev` en `Dev2` via quick-play:
  1. `ca condition debug Dev rise` → `Dev` 80 → 90 → 100 in ~2 s-stappen (~5/s); `Dev2` bleef 0.
  2. Bij 100: `Dev` had slowness + weakness (ambient, `show_particles: 0b`, duur 231); Health 13 → 11 in 3 s (normal). `Dev2` geen effecten, Health 20.
  3. `debug Dev fall`: 85/80 beide effecten, 70 alleen slowness, 50 slowness, 45 geen effecten.
  4. `ca condition set Dev2 debug 60` → alleen `Dev2` slowness.
  5. Relog `Dev` (kick + client opnieuw): login-sync `[42]` = waarde vóór het uitloggen. Server `stop` + herstart: `Dev [30]`, `Dev2 [63]` = waarden vóór de stop. `kill Dev` (met `doImmediateRespawn`) → meter 0, client kreeg `[80]` → `[0]`.
  6. Dimensiewissel (creatief + `rise` = meter staat stil op 42): Nether en terug, meter 42, client kreeg na elke wissel `[42]`.
  7. Client-logs: elke client ontving alleen zijn eigen meters (Dev: 107 berichten, 0 van Dev2; Dev2: 19, 0 van Dev); geen "unknown payload", geen kicks buiten de geplande (kick/stop).
  Extra: een slowness-drankje (60 s) bleef staan toen de meter onder 50 zakte; creatief: geen effecten, meter stijgt niet.
- Blokkerende HUD-check: onderdeel van teststop 1 na change 20 (zie hieronder en `change-20.md`).

## In-game checklist (W)

Wereld: `corrupted_world`, seed `20260930` (dedicated server met `Dev` + `Dev2`, of singleplayer voor stap 1–5). Spawn ligt in ring 1 bij ~0,0; de condities hangen niet van de ring af, alleen van "ringwereld" (F3 rechts: `CA ring (client): ring_1`). Survival, `/difficulty normal`.

1. **Opbouw + HUD:** `/ca condition debug Dev rise`. Verwacht: rechts boven de hongerbalk verschijnt een paarse balk (81 px breed, even breed als de hongerbalk) die in ~20 s volloopt; drie witte streepjes op 50/75/100 %. Bij 50 traagheid-icoon, bij 75 zwakte-icoon (beide zonder particles, knipperen **niet**), bij 100 elke seconde een halve hartje schade.
2. **Afbouw:** `/ca condition debug Dev fall`. Zwakte verdwijnt direct onder 75, traagheid onder 50, de balk verdwijnt bij 0. `/ca condition get Dev` toont de waarde met één decimaal.
3. **Stapeling met luchtbellen:** `/ca condition set Dev debug 60` + `/ca condition debug Dev rise` + `/effect give Dev minecraft:resistance 120 4 true`, dan `/fill ~-1 ~ ~-1 ~1 ~2 ~1 minecraft:water replace minecraft:air` en kopje onder. Luchtbellen staan direct boven de hongerbalk, de conditiebalk erboven (schuift mee); weer boven water schuift hij terug.
4. **Verbergen:** F1 → balk weg. `/gamemode creative` → balk weg, effecten weg, meter stijgt niet (`/ca condition get Dev`). `/gamemode spectator` → balk weg. `/gamemode survival` → balk direct terug.
5. **Respawn en dimensiewissel:** met de meter op 100 en resistance (stap 3): `/ca tpring 1 nether` → balk staat **direct** vol na het laden (niet eerst leeg). `/ca tpring 1` terug → idem. Daarna `/kill Dev` → na respawn geen balk (meter 0), geen effecten.
6. **Per speler (dedicated):** `/ca condition set Dev2 debug 60` → alleen `Dev2` ziet een balk + traagheid; `Dev` ziet niets van `Dev2`. `/ca condition debug Dev rise` → `Dev2` ziet niets van `Dev`.
7. **Persistentie:** `/gamemode creative`, `/ca condition set Dev debug 40`, uitloggen (in creatief zakt de meter alleen met `fall`/`off`; zet daarom eerst `/ca condition debug Dev rise`, dan blijft hij staan). Opnieuw joinen, `/gamemode survival` → balk staat direct op ~40 en zakt daarna (de debug-modus wordt bij uitloggen vergeten).
8. **Drankje blijft:** `/effect give Dev minecraft:slowness 60 0`, `/ca condition set Dev debug 60`, dan `debug fall` → onder 50 blijft het traagheid-drankje (met particles) staan.

### Code review (`/code-review medium`)
Geen correctheidsfouten. Twee lage bevindingen in `MobEffectThreshold.clear`: (1) een zwakker drankje dat vanilla onder ons sterkere effect verbergt (`hiddenEffect`) verdwijnt mee — vastgelegd als bekende beperking (alleen bij een amplifier > 0 in de config; voor traagheid/zwakte is dat voor de speler gunstig; de verborgen instantie is private); (2) een live verlaagde amplifier bleef na het zakken hangen — opgelost: `clear` checkt alleen nog "ambient", niet de amplifier.
