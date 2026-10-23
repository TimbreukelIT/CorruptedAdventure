# Ring 5 golf 2 — stroom C (condities): 97 stilte (Nether-5) + void-koppelingen

Scope: `conditions` — commits `conditions:`, branch `ring5/97`, worktree `P:/Projecten/Java/wt/r5-97`, `setup.sh C 5`
(server 25575, RCON 25585), basis `main` `1a31dbd6`.

Bronnen: `fase-1-wereld.md` → Ring 5, "Nether-tegenpool — Nether-ring 5" → **Conditie: stilte** en "Client/server-
risico's"; §1.9 (systeem); `contract.md` §5 (tabel void/stilte) en **§11** (getallen: 0→100 in ~5 min (~0,33/s), open
ruimte en `hollow_cathedral` ×1,5, bel/goat horn/note block/jukebox binnen ~16 laten zakken (`soundRelief`-bereik 16),
spelers binnen 8 halveren de opbouw (`companionRadius` 8), drempels 30/60/90); beslissingenlog 2026-09-30 (r. 97) en
2026-10-21 (r. 926); `waves.md` §2 (golf 2: C 97, A 98 "stilte-client (volume, HUD)"), §3 ("de blokbronnen (glitchblok,
voidsteen) koppelt C in golf 2 (97)"), §5; voorbeeld: kou van Nether-3 (`ColdRule`, `ColdExposure`, `ColdFreeze`) en de
void van 91 (`golf-1-conditions.md`).

**Bezit** (`waves.md` §1/§5): `condition/*` (stilte), `ServerConfig` `[conditions.silence]` (eigen klasse
`SilenceCondition`, één regel in het static-blok, `ALL_THRESHOLDS`), `Ring5ConditionText`, `Ring5DamageTypes`,
`datagen/tags/Ring5DamageTypeTags`. **Niet:** de client (volume, HUD verbergen, hartslaggeluid: A, 98), `ModTags`/biome-
tags (W-nether), `ModNetwork.PROTOCOL_VERSION` (INT2), `CODEBASE.md` en `docs/design/*` (INT).

**Stand (2026-10-23):** de vier open punten (§0) zijn door de gebruiker beantwoord: **V1 = A, V2 = A, V3 = A, V4 = B**;
P1–P6 gelden. §1 is daarnaar gebouwd.

### Besluiten gebruiker (2026-10-23; door INT naar `fase-1-wereld.md` en het beslissingenlog)
- **V1 = A:** 30 geluid gedempt · 60 HUD weg behalve hotbar (beide client, 98) · 90 alle geluid weg + darkness +
  hartslagschade.
- **V2 = A:** elk geluid (bel, goat horn, note block, jukebox; ook via redstone) binnen 16 blokken → de meter zakt 10 s
  lang met 2,0/s; geluid wint van alle opbouw.
- **V3 = A:** open ruimte = biome `hollow_cathedral` of staan op `polished_white_stone` (de witte vlaktes), ×1,5.
- **V4 = B:** hartslag 1,0 per 2 s, **nooit dodelijk** (stopt op 1 HP, op elke moeilijkheidsgraad); damage type
  `corruptedadventure:silence` voor het hurt-effect; geen eigen doodsbericht (vanilla-terugval volstaat).
- P1–P6 gelden zoals voorgesteld.

---

## 0. Open vragen (voor de gebruiker, via main)

### V1 — Drempels: drie getallen, vier stappen
Het ontwerp noemt vier stappen ("geluid gedempt → HUD weg (behalve hotbar) → alle geluid weg + darkness → schade
(hartslag)"), het besluit van golf 1 drie drempels (30/60/90).
- **A (aanbevolen):** 30 geluid gedempt (client) · 60 HUD weg behalve hotbar (client) · **90 alle geluid weg + darkness
  + hartslagschade** (server: darkness en schade; het geluid en de hartslag zelf zijn client, 98). *Gevolg:* past op
  30/60/90 en het patroon van void/gif (drie drempels, de laatste doet pijn); de stap "stil + donker zonder schade" valt
  weg. Eenvoudigst.
- **B:** vier drempels **30/60/90/100**: 90 alle geluid weg + darkness, 100 schade. *Gevolg:* de vier stappen blijven
  apart; schade alleen bij een volle meter (de meter staat dan op 100 geklemd, dus de schade loopt tot hij zakt). Eén
  drempel meer in config, HUD en 98.
- **C:** vier gelijke drempels 25/50/75/100. *Gevolg:* wijkt af van het besluit 30/60/90.

### V2 — Hoe laat geluid de meter zakken?
Het bereik (16) staat vast; hoe sterk en hoe lang niet. Server-side via NeoForge `VanillaGameEvent` (vanilla game
events: `NOTE_BLOCK_PLAY`, `INSTRUMENT_PLAY` (goat horn), `JUKEBOX_PLAY` (elke seconde zolang de plaat speelt),
`BLOCK_CHANGE` op een bel).
- **A (aanbevolen): hersteltijd.** Na elk geluid binnen 16 blokken zakt de meter **10 s** lang met **2,0/s**
  (`soundReliefSeconds` 10, `soundFall` 2,0): één keer de bel luiden ≈ −20, een spelende jukebox houdt hem laag
  (100 → 0 in 50 s). **Elke bron telt, ook door redstone** (een note-blockklok of jukebox in je basis = veilige kamer).
  *Gevolg:* eenvoudig, voorspelbaar; geluid wint van alle opbouw (zoals licht bij de void). Een AFK-veilige kamer kan.
- **B:** als A, maar **alleen geluid met een speler als oorzaak** (bel, goat horn, note block met de hand); redstone telt
  niet; de jukebox wel (vanilla geeft hem geen oorzaak, dus een uitzondering). *Gevolg:* geen redstoneklok-kamers;
  "zelf geluid maken" letterlijker; iets meer code.
- **C: vaste sprong.** Elk geluid −10 direct, elke bron hooguit één keer per 5 s. *Gevolg:* korter en hakkeliger; een
  jukebox geeft −2/s gemiddeld.

### V3 — Wat is "open ruimte"?
Contract: "sneller in open ruimte (`hollow_cathedral`, vlaktes)". In `white_halls` zijn de **witte vlaktes** sinds 87 de
lage verhogingen (2–6 hoog, bovenblok `polished_white_stone`, ~30 % van de vloer); de hallen zelf zijn overal ~70 hoog.
- **A (aanbevolen): `hollow_cathedral` + op een witte vlakte** (biome aan de voeten = `hollow_cathedral`, of het blok
  onder de voeten = `polished_white_stone`), ×1,5, niet stapelend. *Gevolg:* letterlijke lezing van het contract; even
  goedkoop als het ijs bij de kou; op de vloer tussen de vlaktes en ondergronds 5 min, op de vlaktes en in de kathedraal
  ~3,3 min. Een geplaatst `polished_white_stone` telt ook (alleen in de zone).
- **B: echte openheid**: ×1,5 als er binnen 8 blokken boven het hoofd geen vast blok is (kathedraal en hallen bijna
  overal); 5 min alleen onder een dak, in een tunnel of in een eigen hut. *Gevolg:* schuilen helpt (zoals het dak bij de
  hitte), maar dan is ~3,3 min het gewone tempo in de hallen en is `hollow_cathedral` niet sneller dan de rest.
- **C: alleen `hollow_cathedral`**. *Gevolg:* eenvoudigst; de vlaktes doen niets.

### V4 — Hartslagschade
Niet in het ontwerp: hoeveel, hoe vaak, dodelijk of niet.
- **A (aanbevolen): zoals de void:** 1,0 per 2 s, `DifficultyFloor.LETHAL_FROM_NORMAL` (peaceful niets, easy tot 10 HP,
  normal/hard dodelijk), darkness amplifier 0; eigen damage type `corruptedadventure:silence` (armor-bypass, geen
  knockback); doodsbericht "%1$s's heart stopped in the silence" / "Het hart van %1$s stond stil in de stilte".
  *Gevolg:* consistent met ring 5; met een tweede speler of een bel ben je er snel uit.
- **B: nooit dodelijk** (`NEVER_LETHAL`, tot 1 HP): de stilte is druk, geen moordenaar. *Gevolg:* minder spanning; de
  horror zit in het donker en de stilte.
- **C: zwaardere hartslag:** 2,0 per 4 s, dodelijk vanaf normal. *Gevolg:* zelfde gemiddelde, maar voelbaarder per tik
  (past bij een hartslag-ritme van 98).

### Voorstellen (gelden tenzij de gebruiker anders zegt)
- **P1 Zone:** Nether, `ringAt(...).ring() == RING_5` (hard op 0,5, zoals de kou). Buiten de zone zakt de meter met
  `outsideFall` **1,0/s** (zoals de void); in Nether-4 ×3 via de bestaande `PurifyingModifier`.
- **P2 Samen:** minstens één andere levende speler (niet spectator; creatief telt) in dezelfde dimensie binnen 8 blokken
  (3D) → opbouw ×0,5 (`companionMultiplier` 0,5); meer spelers stapelen niet.
- **P3 Volgorde:** zone → geluid (wint, zakken) → open ruimte ×1,5 → samen ×0,5; open en samen vermenigvuldigen
  (open + samen = 0,25/s, ~6,7 min).
- **P4 Bereik geluid:** 3D-afstand van de gebeurtenis tot de voeten van de speler ≤ 16 (`soundReliefRadius` 16).
- **P5 Void en stilte** zijn onafhankelijk (andere dimensies); geen wisselwerking.
- **P6 Geen nieuwe payload:** `Condition.SILENCE` achteraan → `condition_sync` 1 byte langer → **INT2 verhoogt
  `ModNetwork.PROTOCOL_VERSION`** (`"9"` → `"10"`, één keer voor golf 2).

---

## 1. Plan (gebouwd; V1-A, V2-A, V3-A, V4-B)

1. **`Condition.SILENCE`** (`silence`, HUD wit `0xFFE8E8EC`), **achteraan** de enum (sync per positie).
2. **Zone** (Nether + ring 5, P1; `SilenceRule.inZone`, geen eigen klasse: alleen de regel gebruikt hem) en **`SilenceRule`** + **`SilenceExposure`** (pure regeltabel zoals
   `ColdRule.evaluate`, testbaar zonder server): `rise` 0,33/s; `open` ×`openMultiplier` 1,5 (V3); samen
   ×`companionMultiplier` 0,5 (P2); geluid → `soundFall` 2,0/s (V2, wint van alles); buiten `outsideFall` 1,0/s.
   Labels voor `/ca condition get`: `zone`, `open`, `together` (een metgezel remt; de open-factor telt wel mee in de
   rate), `sound`, `outside`. Goedkoopste eerst: zone → geluid (één map-lookup) → open (biome + blok onder de voeten) →
   metgezellen (`level.players()` binnen 8, alleen als de rate positief is).
3. **`SilenceSounds`** (server, `VanillaGameEvent` op `NeoForge.EVENT_BUS`, via `Ring5Conditions.register`): alleen in
   een `ServerLevel` met dimensie Nether; telt `NOTE_BLOCK_PLAY`, `INSTRUMENT_PLAY`, `JUKEBOX_PLAY` en `BLOCK_CHANGE` op
   een bel (`Blocks.BELL` op de gebeurtenispositie). Per gebeurtenis: elke speler in dat level binnen
   `soundReliefRadius` krijgt de huidige game-tijd als "laatste geluid" (`WeakHashMap<ServerPlayer, Long>`, niet
   opgeslagen; na inloggen geen hersteltijd). De regel vraagt "laatste geluid ≤ `soundReliefSeconds` geleden". Annuleert
   de gebeurtenis nooit.
4. **Drempels** (`threshold1..3` = 30/60/90, V1-A; `ThresholdLevels`):
   - 30 `SilenceMuffle` en 60 `SilenceHudLoss`: server niets (no-op-effect, zoals `VoidGlitchVisuals`); de client leest de
     gesyncte meter (A, 98).
   - 90 `SilenceHeartbeat`: darkness via het nieuwe gedeelde **`ConditionDarkness`** (eigen ambient instance, elke
     seconde ververst, nooit over een vreemde darkness van de warden; void en stilte vragen samen de sterkste, zodat de
     een de darkness van de ander nooit weghaalt) + `DamageThreshold` (`Ring5DamageTypes.SILENCE`, `damage` 1,0 per
     `damageInterval` 2 s, **`NEVER_LETHAL`**; V4-B).
5. **Damage type** `corruptedadventure:silence` (`Ring5DamageTypes.bootstrap`, geen exhaustion); tags in
   `Ring5DamageTypeTags` zoals `void`: `minecraft:bypasses_armor`, `minecraft:no_knockback`,
   `minecraft:bypasses_cooldown`, `neoforge:is_environment`. Geen doodsbericht (V4-B: nooit dodelijk).
6. **Config `[conditions.silence]`** (`ServerConfig.SilenceCondition`): `rise` 0,33, `openMultiplier` 1,5 (1–4),
   `companionRadius` 8 (1–32), `companionMultiplier` 0,5 (0–1), `soundReliefRadius` 16 (1–64), `soundReliefSeconds` 10
   (1–120), `soundFall` 2,0, `outsideFall` 1,0, `threshold1..3` 30/60/90, `darknessAmplifier` 0, `damage` 1,0,
   `damageInterval` 2. Commentaar per sleutel met de tijd 0→100. In `ALL_THRESHOLDS`.
7. **Registratie:** `Ring5Conditions.register(gameBus)`: de `VanillaGameEvent`-listener erbij. `ConditionDefinitions`: de
   `SILENCE`-takken in `rule`, `thresholdLevels`, `effects`.

## 2. Void-koppelingen met de blokken van B (89)

Stand: **al gekoppeld, geen code nodig.** B vulde in 89 `#corruptedadventure:void_sources` met precies `glitch_block` en
`voidstone` (`Ring5BlockDataTest` toetst de gegenereerde tag op precies die twee, dus ook: geen geneste tags, geen
nulliet-ertsen, geen corrupted- of Nether-5-blokken); `VoidRule` (91) leest die tag via `NearbyBlocks`
(`sourceRadius` 4, palette-voorcheck). De testdatapack-noot van 91 (§6) vervalt. Een eigen `VoidCouplingsTest` zoals
`PoisonCouplingsTest` zou `Ring5BlockDataTest` alleen herhalen; die komt er niet.
Wat blijft is de **in-game check** (gebruiker, 6b): naast een glitchblok of voidsteen in de binnenband `source` +0,55/s;
met een fakkel (bloklicht ≥ 12) `light` −1,0/s; de voidsteenranden van de scheuren (95) tellen als bron.

## 3. Client/server-impact
- Alles server-side: regel, geluidsdetectie (vanilla game events via NeoForge `VanillaGameEvent`, nooit client-geluid),
  metgezellen, drempels, darkness, schade. Geen client-classes in `condition/`, geen `Minecraft.getInstance()`.
- Sync via het bestaande `condition_sync` (één waarde langer; P6). Geen nieuwe payload. Volume, HUD verbergen en het
  hartslaggeluid leest A (98) uit de gesyncte meter en `ConditionDefinitions.thresholdLevels(SILENCE)` (gesyncte config).
- Multiplayer: de metgezel-regel kijkt naar andere spelers op de server; een tweede speler ziet de darkness van een
  ander niet (vanilla-effect alleen op de eigen speler).
- Prestatie: per speler per seconde alleen in Nether-5: één map-lookup (geluid), één biome- en één blokcheck (open),
  hooguit één spelerslijst-doorloop (metgezellen); per geluidsgebeurtenis in de Nether één doorloop van de spelers van
  dat level (een jukebox: 1/s).

## 4. Tests (licht: B, D, unit; S-smoke)
- `SilenceRuleTest`: 0→100 in 300 s ± 10 %; open/kathedraal 200 s; samen 600 s; open + samen 400 s; geluid wint (ook
  open en samen); buiten de zone zakken; zone hard op 0,5 (Nether 4|5 aan de ring 4-kant valt); overworld ring 5 valt.
- `SilenceSoundsTest`: welke game events tellen (bel alleen op een bel; `BLOCK_CHANGE` elders niet); bereik 16 op de
  grens; hersteltijd 10 s; alleen de Nether; meerdere spelers.
- `SilenceThresholdsTest`: 30/60 niets op de server; 90 darkness + schade; warden-darkness blijft staan;
  `ThresholdLevelsTest` +1.
- `Ring5DamageTypeTagsTest` +`silence`; `config/SilenceConfigTest` (precies de sleutels van §1.6 met hun standaarden).
- D: `damage_type/silence.json` en de vier damage-type-tags (gecommit); geen lang.
- S-smoke `r5-97` (verse wereld): log zonder ERROR en zonder client-class-fout; `/ca condition` via RCON beschikbaar en
  `silence` als conditienaam geparst (`/ca condition set` zonder speler geeft de gewone "geen speler"-fout).
- **Gebruiker in-game (6b, dedicated server + 2 clients):** opbouw alleen en samen (binnen/buiten 8), op een vlakte en in
  `hollow_cathedral`, elk van de vier geluidsbronnen (ook via redstone), de drempels met 98's client-effecten, de schade
  op easy/normal; de void-bronnen van §2.

**Af als:** B, D, unit tests en S-smoke groen; `/code-review` medium verwerkt; de koppelpunten voor INT2 (§5) staan hier.

## 5. Koppeling met A (98) en INT2
- A leest `Condition.SILENCE` (naam `silence`, positie achteraan) en `ServerConfig.SILENCE.thresholds`; de client beslist
  niets (V1 bepaalt welke drempel wat doet; bij V1-B is er een vierde drempel).
- Het hartslaggeluid speelt A client-side vanaf de schadedrempel (geen server-geluid in 97).
- INT2: `ModNetwork.PROTOCOL_VERSION` `"9"` → `"10"`; `ServerConfig` static-blok: `SILENCE = new SilenceCondition(builder);`
  na `VOID`, `SILENCE.thresholds` in `ALL_THRESHOLDS`.

## Voortgang
- Voortgang: 2026-10-23 15:12 worktree `P:/Projecten/Java/wt/r5-97` (branch `ring5/97` vanaf `1a31dbd6`), `setup.sh C 5`.
- Voortgang: 2026-10-23 15:30 ontwerp gelezen; vier open punten (V1–V4) + voorstellen P1–P6; plan gecommit, bouwen
  wacht op de antwoorden. Void-koppeling: geen code nodig (§2).
- Voortgang: 2026-10-23 15:20 antwoorden V1 A, V2 A, V3 A, V4 B; code + tests 97.
- Voortgang: 2026-10-23 15:28 runData groen (alleen eigen output: `silence.json` + 4 tags); build groen (1.304 tests,
  0 fouten).
- Voortgang: 2026-10-23 15:30 S-smoke `r5-97` (vers): 0 ERROR, geen client-class; `[conditions.silence]` in de
  server-config; jukebox met plaat, bel en note block via redstone in Nether-5 via RCON: geen fout in de listener
  (zonder speler geen meterwaarde te zien); `/ca condition` beschikbaar (zonder speler "No player was found").
- Voortgang: 2026-10-23 15:41 `/code-review` medium verwerkt (zie Beslissingen); build groen (1.308 tests, 0 fouten),
  S-smoke opnieuw schoon; commit.

## CODEBASE-delta
Package `condition` (alles server-side; geen client-class):
- `Condition.SILENCE` (`silence`, HUD `0xFFE8E8EC`), achteraan de enum (sync per positie).
- `SilenceRule` + `SilenceExposure` (`zone`/`open`/`together`/`sound`/`outside`): zone = Nether, `ring() == RING_5`
  (hard op 0,5; de Nether-voidrand telt niet); volgorde zone → geluid (`SilenceSounds.relieved`, wint, `soundFall`) →
  open (`hollow_cathedral` of staan op `polished_white_stone`, ×`openMultiplier`) → metgezel (andere levende
  niet-spectator binnen `companionRadius`, ×`companionMultiplier`, niet stapelend); open en samen vermenigvuldigen.
- `SilenceSounds`: NeoForge `VanillaGameEvent` (via `Ring5Conditions`), alleen de Nether; `NOTE_BLOCK_PLAY`,
  `INSTRUMENT_PLAY`, `JUKEBOX_PLAY` (vanilla elke seconde zolang de plaat speelt), `BLOCK_CHANGE` alleen op een bel;
  elke speler van dat level binnen `soundReliefRadius` (3D) onthoudt de server-tick (`WeakHashMap`, niet opgeslagen).
  Annuleert nooit.
- Drempels (`ConditionDefinitions` `SILENCE`): 30 `SilenceCue.MUFFLED`, 60 `SilenceCue.HUD_HIDDEN` (no-op, de client
  leest de meter), 90 `SilenceHeartbeat` (darkness + `DamageThreshold` `Ring5DamageTypes.SILENCE`, `NEVER_LETHAL`).
- `ConditionDarkness` (nieuw, gedeeld): de ene darkness van de condities (`ours`/`isOurs`/`update(player, amplifier)`;
  `wanted`/`combine` = max van void en stilte, de enige plek; `decide` = pure keuze `REFRESH`/`LOWER` (verwijderen +
  opnieuw, alleen bij een lagere sterkte)/`REMOVE`/`NOTHING` (vreemde darkness)); `VoidDarkness` beslist alleen nog wanneer de void hem wil
  (`amplifier(player)`, de band alleen vanaf drempel 2) en laat hem staan zolang de stilte hem wil.
  `VoidSlip.zoneOf` package-private.
- `ConditionContext.standsOn(Predicate<BlockState>)` naast de tag-variant.
- `Ring5DamageTypes.SILENCE` (`corruptedadventure:silence`, msgId `corruptedadventure.silence`, exhaustion 0; geen
  doodsbericht).
- `Ring5Conditions.register`: ook de `VanillaGameEvent`-listener.
Config: `ServerConfig.SILENCE` = `[conditions.silence]` (`SilenceCondition`, 14 sleutels: `rise` 0,33, `openMultiplier`
1,5, `companionRadius` 8, `companionMultiplier` 0,5, `soundReliefRadius` 16, `soundReliefSeconds` 10, `soundFall` 2,0,
`outsideFall` 1,0, `threshold1..3` 30/60/90, `darknessAmplifier` 0, `damage` 1,0, `damageInterval` 2). In
`ALL_THRESHOLDS`.
Datagen: `Ring5DamageTypeTags` + `silence` (`bypasses_armor`, `no_knockback`, `bypasses_cooldown`,
`neoforge:is_environment`).
Gegenereerd: `data/corruptedadventure/damage_type/silence.json`, de vier damage-type-tags.
Tests: `SilenceRuleTest`, `SilenceSoundsTest`, `SilenceThresholdsTest`, `config/SilenceConfigTest`,
`Ring5DamageTypeTagsTest` +1, `ThresholdLevelsTest` +1; `VoidThresholdsTest` leest `ConditionDarkness`.
Docs voor INT2: `fase-1-wereld.md` Nether-5 "Conditie: stilte" en beslissingenlog (de vier besluiten hierboven);
`ModNetwork.PROTOCOL_VERSION` `"9"` → `"10"` (`condition_sync` + `silence`).

## Beslissingen
(implementatiedetails, door de stroom beslist)
- Void-koppeling zonder eigen test: `Ring5BlockDataTest` (B) toetst de tag al exact (§2).
- De Nether-voidrand (voorbij Nether-5) hoort niet bij de zone: het contract noemt alleen Nether-5, en de rand is de
  zwevende-eilanden-invoer, geen witte hal (anders dan de overworld-voidrand bij de void, P4 van 91).
- Drempel 3 werkt overal waar de meter is (zoals void en gif); buiten de zone zakt de meter 1,0/s, dus vanaf 100
  stopt de hartslag na ~10 s (met een bel na ~5 s).
- Hersteltijd in server-ticks (`server.getTickCount()`, zoals `SlipSchedule`), niet in de game-tijd van het level (die
  kan met `/time` springen).
- **Review (medium, 2026-10-23, op main...ring5/97 + werkmap; 8 bevindingen):** opgelost — (1) onze darkness werd
  nooit lager gezet (vanilla `addEffect` houdt de sterkere en verstopt de zwakkere; nu `ConditionDarkness.decide` →
  `LOWER`); (3) de max van void en stilte stond op twee plekken (nu alleen `ConditionDarkness.wanted`/`combine`) en de
  javadoc van `VoidDarkness` was verouderd; (6) de hersteltijd rekende met `ConditionTicker.INTERVAL_TICKS` als "ticks
  per seconde" (nu `SharedConstants.TICKS_PER_SECOND`); (8) tests voor combineren, verlagen en de vreemde
  (warden-)darkness (`SilenceThresholdsTest` +4). Niet opgelost: (2) dubbele verversing per seconde als void ≥ 60 én
  stilte ≥ 90 tegelijk gelden — kan alleen kort na een portal met beide meters hoog (de void zakt in de Nether 1/s);
  twee effect-packets per seconde, verwaarloosbaar; (4) heel kleine hartslagschade vlak boven 1 HP (bv. 1,05) geeft een
  hurt-flits — vanilla-regeneratie heelt in hele punten, de flits past bij een hartslag, en `DifficultyFloor` is gedeeld;
  (5) geen doodsbericht: besluit van de gebruiker (V4-B); de javadoc van `Ring5ConditionText` zegt het nu; (7)
  hersteltijd per `ServerPlayer`-object, weg na respawn — na de dood staan alle meters toch op 0, oude objecten vallen
  weg via de zwakke sleutels. "Alleen de Nether, meerdere spelers" van `SilenceSounds` niet als unittest (de haak vraagt
  een echt level): de filter is één regel; bereik, gebeurtenissen en hersteltijd zijn wel getest.
- Geluidsdetectie via NeoForge `VanillaGameEvent` (vuurt in `ServerLevel.gameEvent` vóór de vanilla-dispatcher) in
  plaats van een eigen `GameEventListener` per speler: één globale haak, geen registratie per speler of chunk.

## Backlog
- `atmosphere` (98): hartslaggeluid vanaf de schadedrempel van de stilte; volume en HUD volgens V1.
