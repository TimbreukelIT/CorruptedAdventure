# Change 27 — Uitdroging (ring 2)

Scope: `conditions` — change 27

**Doel:** de eerste echte omgevingsconditie op het meter-systeem van change 19: **uitdroging** in ring 2 (`fase-1-wereld.md` §2 Ring 2, "Conditie: uitdroging"). De meter loopt op overdag in direct zonlicht en zakt in schaduw, in water, in het oase-biome, 's nachts en buiten ring 2; een vanilla-waterfles haalt er ~30 punten af. Drempels (config): sneller honger → traagheid → mining fatigue → lichte schade. Geen weer (hittegolf ×2 is change 28), geen gear (fase 2: veldfles/waterzak), geen nieuwe HUD-code (alleen een kleur).

---

## Afstemming (2026-10-01)

Afgestemd met 18, 19, 20, 23, 28 en 30 (basis: de aanbevolen opties).
- **Geen weer in `ConditionContext`.** Change 28 hangt `weather/HeatWaveDehydrationModifier` (leest de weerstaat zelf) in via `ConditionDefinitions.addModifier` (19). De sectie "Change 28" hieronder is aangepast.
- **Uit change 19:** N drempels per conditie (geen generalisatie nodig), `DamageThreshold` met `intervalSeconds` en `DifficultyFloor` (`NONE`/`VANILLA_STARVATION`/`NEVER_LETHAL`). Vraag 6 kiest alleen de floor (A = `VANILLA_STARVATION`); de peaceful-optie D van 19 vraag 6 is hierheen verplaatst.
- **Context-checks:** change 19 zet geen `isDay()`/`canSeeSky()`/`isInWater()` in de context; deze change voegt `isSunUp()` (vraag 3), `canSeeSky()` op ooghoogte (vraag 2) en de water-check (vraag 7) toe met de gekozen betekenis. Change 28 hergebruikt `canSeeSky()` voor de wind-duw.
- **Oase:** biome-key `corruptedadventure:oasis_flats` (23 maakt geen `is_oasis`-tag). Na change 30 is de oase ~12 % van ring 2; `locate` vindt hem nog.
- `RingWorld.isActive` (18) zit in de ticker van 19; `client2` (18) bestaat, maar deze S-test heeft genoeg aan één client.
- **Ringweer-ritme (20 vraag 3 = B, keuze gebruiker):** vanilla-ritme, dus een hittegolf (change 28) is zeldzaam; het basistempo van vraag 1 draagt de conditie zelf.
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37).

## Afstemming na teststop 2 (2026-10-01)

Afgestemd met de plannen 23b en 23c (antwoorden 2026-10-01). **Geen wijziging aan de regels van deze change**; alleen feiten om te weten bij de bouw en de tests.
- **Schaduw in het nieuwe terrein (23b):** spleetkloven (3–6 breed, 25–45 diep), wandnissen en de overhangende kaprots aan de plateaurand blokkeren de hemel vaak (`canSeeSky` = false). De regel van vraag 2 A werkt daar vanzelf: het zijn natuurlijke schuilplekken. Onderin een open spleet kan de hemel nog zichtbaar zijn (dan is het "zon"); dat is bewust. Testplek voor "schaduw" in S/W mag een nis of overdekt spleetstuk zijn.
- **Wandbron (23c-4 C, keuze gebruiker):** zeldzaam (~1 per 8–15 kloofchunks) echt water in de kloven: bron in de wand, val, stroompje (geul 1 diep), kolk (tot 2 diep). Met vraag 7 A ("ondergedompeld tot boven het middel") telt het stroompje vermoedelijk **niet**, de kolk en de val vermoedelijk **wel**; de wandvoet geeft ook schaduw. Dat is een tweede, zeldzame koelplek naast de oase. **Alleen noteren:** of dat zo mag, staat als `[conditions]`-regel in de backlog; deze change verandert er niets aan. Bij de bouw in S één keer meten wat de water-check in een wandbron-kolk en in het stroompje geeft en het in `CODEBASE.md` zetten.
- **Oase (23b-8):** de waterfractie blijft ~6–8 % en de grasoever wordt breder; de oase-regel werkt op de biome-key en verandert niet.

---

## Afhankelijkheden en aannames

Changes 18–26 zijn bij het schrijven van dit plan **nog niet gebouwd**. Hun plannen zijn de bron; alles hieronder met *(aanname)* bij de start tegen `CODEBASE.md` en de echte code leggen en dit plan daarop aanpassen (niet andersom).

- **Change 19 (`conditions`, meter-systeem)** — *(aanname, namen uit `docs/plans/change-19.md`)*:
  - `condition/Condition` (enum, `StringRepresentable`, opslag op naam, sync op index, per waarde een HUD-kleur); nu alleen `DEBUG`.
  - `ConditionContext` (record per speler per tick: `ServerPlayer`, `ServerLevel`, `RingPosition`, `Holder<Biome>`; afgeleid `isDay()`, `canSeeSky()`, `isInWater()`).
  - `ConditionRule` (`float ratePerSecond(ConditionContext)`; positief = op, negatief = af; `ConditionRule.recover(fallRate)`), `RateModifier` (pijplijn na de regel), `ThresholdEffect` met `MobEffectThreshold` en `DamageThreshold`, `ConditionDefinitions` (regel + drempels per conditie), `ConditionTicker` (elke 20 ticks, alleen `ServerPlayer`, geen `FakePlayer`, regels voor dood/creatief volgens vraag 6 van change 19).
  - Instant-API `PlayerConditions.add/set/get(ServerPlayer, Condition, float)` — synct direct.
  - `config/ServerConfig` (SERVER-config per wereld, door NeoForge naar de client gesynct; per conditie een sectie). In change 19 heeft `debug` **drie** drempels (`threshold1..3`); uitdroging heeft er **vier** nodig (vraag 5). *(Afgestemd 2026-10-01: change 19 bouwt de drempels al generiek — N per conditie — en `DamageThreshold` met interval en `DifficultyFloor`; deze change hoeft niets te generaliseren.)*
  - Debugcommando `/ca condition get [<targets>]`, `set <targets> <condition> <0–100>`, `debug …` in `command/ConditionCommand`.
  - Antwoorden op de vragen 3 (effect-verversing), 4 (config), 5 (HUD) en 6 (dood/creatief/peaceful) van change 19 gelden hier ongewijzigd. **Vraag 6 van 19 raakt vraag 6 hieronder** (schade in peaceful): als 19 al "geen schade in peaceful" koos, is dat het uitgangspunt.
- **Change 18 (`infra`)** — *(aanname)* `ring/RingWorld.isActive(server)`: de condities-tick van 19 draait alleen in ringwerelden. De waterfles-hook (hieronder) heeft die check niet nodig: in een niet-ringwereld is de meter altijd 0 en doet −30 niets (geen sync bij geen verandering).
- **Change 20 (`atmosphere`, ringweer)** — wordt hier **niet** gebruikt. Change 28 koppelt de hittegolf via een `RateModifier` (zie "Change 28").
- **Change 23 (`worldgen`)** — *(plan, afgestemd 2026-10-01)*: levert het biome `corruptedadventure:oasis_flats` met de key `ModBiomes.OASIS_FLATS`. Change 23 maakt **geen** oase-tag; deze change gebruikt de key. Een nieuwe biome-tag maken hoort bij `worldgen` → niet hier; bij behoefte (bijv. als `salt_flats`-bronnen in change 30 ook moeten tellen) naar de backlog.
- **Change 26 (`worldgen`)** — ondergrondse waterreservoirs: gewoon vanilla-water, dus vallen onder de waterregel (vraag 7). Geen afhankelijkheid in code.
- **Bestaand (gecontroleerd in `CODEBASE.md`):** `RingLayout.ringAt(dimension, x, z)` → `RingPosition` (`ring()` wisselt op blend 0,5, `inTransition()`, `blend`); `Ring.RING_2`; `CaCommand`; `DataGenerators` (één lambda per datapack-registry in de `RegistrySetBuilder`; tag-providers voor datapack-entries via `event.createProvider(...)` ná `createDatapackRegistryObjects`); `EnglishLanguageProvider`/`DutchLanguageProvider`.
- Nether-ring 2 heeft **geen** conditie (design, Nether-tegenpool ring 2): uitdroging geldt alleen in de overworld.

---

## Plan

### 1. Conditie en regel (common, `condition/`)

- **`Condition.DEHYDRATION`** (`dehydration`), HUD-kleur zandgeel/oker (bijv. `0xFFD9A441`; placeholder, definitief in de ring 2-`textures`-change of de HUD-backlog van 19).
- **`DehydrationExposure`** — enum + pure classificatie, los te testen zonder server:
  - Waarden: `SUN` (op), `SHADE`, `NIGHT`, `WATER`, `OASIS`, `OUTSIDE` (af). Het label staat ook in de debug-uitvoer (§4).
  - Invoer als klein record `DehydrationInput(boolean inZone, float zoneWeight, boolean sunUp, boolean directSun, boolean inWater, boolean inOasis)` (of direct uit `ConditionContext`, maar zo is de tabel unit-testbaar).
  - `inZone`: overworld **en** de zoneregel uit vraag 4 (aanbevolen: `ringAt(...).ring() == RING_2`). Nether → altijd `OUTSIDE`.
  - `sunUp`: vraag 3 (aanbevolen: zon boven de horizon op basis van de tijd van de dag, vanilla-regen/onweer genegeerd).
  - `directSun`: vraag 2 (aanbevolen: `level.canSeeSky(eyePos)` = hemellicht 15 op ooghoogte).
  - `inWater`: vraag 7 (aanbevolen: ondergedompeld tot boven het middel, niet "voeten in een plasje").
  - `inOasis`: biome op de voeten is `oasis_flats` (key of tag, zie aannames).
- **`DehydrationRule implements ConditionRule`**:
  - Is één of meer van `WATER`, `OASIS`, `NIGHT`, `SHADE`, `OUTSIDE` van toepassing → rate = **−(grootste daarvan)** (afnames stapelen niet, de sterkste wint; vraag 1). Label = die bron.
  - Anders (in de zone, zon op, direct zonlicht, droog, geen oase) → rate = **+riseRate** (bij vraag 4 → B: × `zoneWeight`). Label `SUN`.
  - **De regel geeft alleen in `SUN` een positieve rate.** Daar rekent change 28 op (hittegolf ×2 alleen op de opbouw); de regel bakt zelf geen vermenigvuldigers in.
  - `OUTSIDE` gebruikt de algemene afname (zelfde gedrag als `ConditionRule.recover`): buiten ring 2 (ring 1, 3–5, void, Nether) zakt de meter gewoon.
- `ConditionContext` uitbreiden waar nodig (zelfde categorie): `isSunUp()` (vraag 3; als 19's `isDay()` vanilla `level.isDay()` is, krijgt de context een aparte methode — `isDay()` niet stilzwijgend van betekenis veranderen), `waterDepth()`/`isSubmerged()` (vraag 7), `eyeBlockPos()`. Alles lui berekend, zoals in 19.
- **`ConditionDefinitions`**: `DEHYDRATION` → `DehydrationRule` + drempels (§3).

### 2. Waterfles (common, `condition/WaterBottleDrink`)

- Game-bus-listener op `LivingEntityUseItemEvent.Finish`, alleen `ServerPlayer` (geen `FakePlayer`, niet client-side).
- Pure check `isWaterBottle(ItemStack)`: `Items.POTION` met `DataComponents.POTION_CONTENTS` waarvan `potion` = `Potions.WATER`. Op het **originele** stack van het event (niet het resultaat, dat is de lege fles). Splash/lingering-flessen worden niet gedronken en vallen er dus vanzelf buiten; awkward/mundane/thick tellen niet (design: "vanilla-waterfles").
- Effect: `PlayerConditions.add(player, DEHYDRATION, -waterBottleAmount)` (standaard 30, config). Werkt overal (ook buiten ring 2): wie ring 2 verlaat met een hoge meter kan nog drinken. Geen extra effecten, geen cooldown.
- Event-API (`LivingEntityUseItemEvent.Finish#getItem()` vs. `getResultStack()`) bij de bouw verifiëren in NeoForge 21.1.252.

### 3. Drempels, schade en config

- Drempels volgens vraag 5 (aanbevolen 25 / 50 / 75 / 100):
  1. `MobEffectThreshold(MobEffects.HUNGER, 0)` — "sneller honger" (vanilla-hongereffect: groene hongerbalk, zichtbaar signaal).
  2. `MobEffectThreshold(MobEffects.MOVEMENT_SLOWDOWN, 0)` — traagheid I.
  3. `MobEffectThreshold(MobEffects.DIG_SLOWDOWN, 0)` — mining fatigue I.
  4. `DamageThreshold` met eigen damage type `corruptedadventure:dehydration` — volgens vraag 6.
- Cumulatief en ververst zoals change 19 beslist (vraag 3 van 19).
- **Schade (vraag 6, aanbevolen A):** zoals vanilla-verhongeren — 1 HP per 4 s (interval in config), en de moeilijkheidsgraad bepaalt de ondergrens: peaceful geen schade, easy stopt op 10 HP, normal op 1 HP, hard dodelijk. `DamageThreshold` uit 19 krijgt daarvoor een interval (seconden tussen twee tikken) en een optionele "vanilla-honger-ondergrens"; generiek, zodat latere condities hem kunnen hergebruiken.
- **Damage type** (datapack-registry, datagen):
  - `registry/ModDamageTypes` — `ResourceKey<DamageType> DEHYDRATION`.
  - `condition/ConditionDamageTypes.bootstrap(BootstrapContext<DamageType>)` — `new DamageType("corruptedadventure.dehydration", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0f)` (exhaustion 0, zoals `starve`; waarden bij de bouw naast vanilla `starve` leggen). Aangehaakt in `DataGenerators` (`Registries.DAMAGE_TYPE`, één lambda). Latere condities voegen hun damage type in dezelfde bootstrap toe.
  - `datagen/ModDamageTypeTagsProvider` — dezelfde damage-type-tags als vanilla `starve` (lijst bij de bouw uitlezen uit vanilla's `DamageTypeTagsProvider`; verwacht o.a. `bypasses_armor`, `bypasses_effects`, `no_knockback`). Via `event.createProvider(...)` ná de datapack-registries (valkuil uit CODEBASE).
  - Lang (en + nl): `death.attack.corruptedadventure.dehydration` = "%1$s dried out" / "%1$s is uitgedroogd"; `death.attack.corruptedadventure.dehydration.player` = "%1$s dried out whilst fighting %2$s" / "%1$s droogde uit tijdens het gevecht met %2$s". (Vanilla toont `.player` alleen bij een recente aanvaller.)
- **Config** — sectie `conditions.dehydration` in `ServerConfig` (vorm volgens change 19). Standaarden volgens vraag 1 → A en vraag 5/6 → aanbevolen:

| Sleutel | Standaard | Betekenis |
|---|---|---|
| `riseRate` | 0,4 | punten/s in direct zonlicht overdag in ring 2 (0 → 100 in ~4 min) |
| `fallRateShade` | 0,5 | punten/s in schaduw (overdag, ring 2) |
| `fallRateNight` | 0,5 | punten/s 's nachts (ring 2) |
| `fallRateWater` | 2,0 | punten/s in water (definitie: vraag 7) |
| `fallRateOasis` | 2,0 | punten/s in het oase-biome (ook in de zon) |
| `fallRateOutside` | 0,5 | punten/s buiten ring 2 |
| `waterBottleAmount` | 30 | punten minder per gedronken vanilla-waterfles |
| `threshold1..4` | 25 / 50 / 75 / 100 | honger / traagheid / mining fatigue / schade (oplopend gevalideerd) |
| `effect1..3Amplifier` | 0 / 0 / 0 | sterkte van de drie effecten |
| `damage` / `damageIntervalSeconds` | 1,0 / 4 | schade per tik / seconden tussen tikken (vraag 6) |

  Geen `heatWaveMultiplier` hier: die sleutel hoort bij change 28.

### 4. Debug-uitvoer

- `/ca condition get [<targets>]` (uit 19) toont per meter ook de **huidige rate** (regel + alle modifiers, droog doorgerekend zonder toe te passen) en, als de regel er een heeft, een **label**: bijv. `dehydration 42.3 (+0.40/s, sun)`. Generiek via een `default String debugLabel(ConditionContext)` op `ConditionRule` (leeg = geen label). Nodig om de S-test via RCON te kunnen lezen, en change 28 ziet er direct zijn ×2 in terug. Vaste Engelse tekst.

### 5. Documentatie

- `CODEBASE.md`: `condition/` (nieuwe classes, `DEHYDRATION` in de enum), `registry/ModDamageTypes`, `datagen/ModDamageTypeTagsProvider`, damage type in "Overige registries" en `src/generated`, configsectie, `/ca condition get` met rate/label; valkuilen die bij de bouw opduiken (bijv. `canSeeSky` en glas/bladeren, `Finish`-event en het originele stack).
- `fase-1-wereld.md` §2 Ring 2: onder "Conditie: uitdroging" een kopje **Uitwerking (change 27)** met de definitieve regels (zone, dag, schaduw, water, oase, stapeling, waterfles, drempels, schade, standaardwaarden).
- `02-beslissingenlog.md`: regels onderaan.
- `docs/backlog.md`: alleen als er bij de bouw iets buiten `conditions` opduikt (bijv. [worldgen] biome-tag `is_oasis` als meer biomes moeten verkoelen; [textures] HUD-icoon uitdroging valt onder de HUD-regel van 19).

---

## Change 28 (hittegolf ×2) — wat deze change klaarzet

- De regel geeft **alleen** in `SUN` een positieve rate; schaduw, nacht, water en oase blijven negatief. Een `RateModifier` van change 28 (`condition == DEHYDRATION && rate > 0 && ringweer op de positie == HEAT_WAVE` → `rate × heatWaveMultiplier`) verdubbelt dus precies de opbouw in de zon, zoals het design zegt.
- **Afgestemd 2026-10-01:** change 28 voegt de weerstaat **niet** aan `ConditionContext` toe. Zijn `weather/HeatWaveDehydrationModifier` leest de staat zelf (`RingWeather.state(level, ring)`) en wordt geregistreerd via `ConditionDefinitions.addModifier` (change 19); de configsleutel staat in de weer-sectie (28 vraag 7) en de factor volgt `intensity()` (28 vraag 6).
- De debug-uitvoer van §4 toont de rate ná de modifiers, dus de S-test van 28 kan `+0.80/s, sun` direct aflezen.

---

## Client/server-impact

- **Server:** alle regels, de waterfles-hook, drempels, effecten en schade draaien in common code, alleen voor `ServerPlayer`. Extra kosten per speler per seconde: één `canSeeSky`-lichtlookup, één vloeistofcheck en één biome-vergelijking (biome en ring zitten al in de context van 19). Verwaarloosbaar.
- **Client:** geen nieuwe client-code; de HUD van 19 toont de nieuwe meter met zijn kleur. Het damage type en de doodsberichten komen via datapack-sync en lang naar de client.
- **Netwerk:** geen nieuwe payload; de meter-sync van 19 (≤ 1/s, alleen bij verandering van de afgeronde waarde, alleen naar de eigen speler) dekt het.
- **Persistentie:** geen nieuwe staat; de meter zit in de attachment van 19.
- **Multiplayer:** elke speler heeft zijn eigen meter; schaduw/water/oase gelden per speler. Geen invloed op worldgen of determinisme.

---

## Tests

**B** — `./gradlew build` slaagt, met nieuwe unit tests:
- `condition/DehydrationRuleTest`: tabel over `DehydrationInput` — zon in de zone → `+riseRate`; elk van schaduw/nacht/water/oase → de juiste negatieve rate; combinaties (nacht + water, oase + zon) → sterkste afname wint; buiten de zone (ring 1, ring 3, Nether-ring 2) → `-fallRateOutside`, nooit positief; **alleen `SUN` is positief** (borgt de aanname van change 28); zoneregel volgens vraag 4 (A: blend 0,49 vs. 0,51 op grens 1 en 2; B: rate schaalt met het gewicht).
- `condition/DehydrationExposureTest` (of in dezelfde test): labels kloppen met de gekozen bron.
- `condition/WaterBottleDrinkTest`: `isWaterBottle` — waterfles ja; awkward, mundane, thick, splash-water, lingering-water, lege fles, melkemmer nee.
- `condition/SunTest` (vraag 3 → A): `isSunUp` op `dayTime` 0, 6000, 12000, 13000, 18000, 23500 en met `dayTime` > 24000 (meerdere dagen).
- `DamageThreshold` (vraag 6): interval (schade alleen elke 4e seconde) en moeilijkheidsondergrens (peaceful 0, easy stopt op 10 HP, normal op 1, hard niet) als pure functie.
- Config: vier drempels oplopend gevalideerd.

**D** — `./gradlew runData` slaagt: damage type `corruptedadventure:dehydration`, damage-type-tags, lang en + nl; gegenereerde output gecommit.

**S** — `tools/server-smoke-test.sh` met `level-name=smoke-change27`: server start zonder client-class-fouten. Daarna dedicated server met RCON + `online-mode=false`, seed `20260930`, nieuwe `level-name`, één client (`Dev`) joint (zelfde opzet als de S-test van 19; de agent bedient alles via RCON, de client staat stil). Per stap: `ca condition set Dev dehydration <start>`, 10 s wachten, `ca condition get Dev` → verwachte waarde ± 1 en het juiste label.
1. **Zon:** `gamerule doDaylightCycle false`, `time set noon`, `execute as Dev at Dev run ca tpring 2`; controleren dat het geen oase is (`execute at Dev if biome ~ ~ ~ corruptedadventure:oasis_flats` → faalt) en niets boven het hoofd. Start 0 → ≈ 4 (`+0.40/s, sun`).
2. **Schaduw:** dak van steen op 3 blokken boven het hoofd (`execute at Dev run fill ~-3 ~3 ~-3 ~3 ~3 ~3 minecraft:stone`). Start 50 → ≈ 45 (`shade`). Variant vraag 2 → A: dak van `oak_leaves[persistent=true]` → schaduw; dak van glas → zon. Dak weer weg (`fill … air`).
3. **Nacht:** `time set midnight`, open lucht. Start 50 → ≈ 45 (`night`). `time set 13000` / `23500` controleren volgens vraag 3.
4. **Water:** `time set noon`; bak van glas met 2 blokken water rond Dev (`fill … glass hollow`, dan `fill … water`). Start 50 → ≈ 30 (`water`). Variant vraag 7 → A: alleen 1 laag water op voethoogte → blijft `sun` (stijgt).
5. **Oase:** `locate biome corruptedadventure:oasis_flats` (RCON draait op de spawn; ring 2 ligt binnen het zoekbereik), Dev erheen (open lucht, middag). Start 50 → ≈ 30 (`oasis`), ook in de volle zon.
6. **Buiten ring 2:** `ca tpring 1`, `ca tpring 3` en `ca tpring 2 nether` (middag, open lucht) → telkens dalend met `fallRateOutside` (`outside`), nooit stijgend.
7. **Grens:** `ca tpboundary 1` en `ca tpboundary 2`, Dev telkens ~20 blokken naar binnen en naar buiten zetten (`tp` langs de richting) → teken volgens vraag 4 (A: binnen ring 2 stijgend, erbuiten dalend).
8. **Drempels:** `ca condition set Dev dehydration 30` → `data get entity Dev active_effects` bevat `minecraft:hunger`; 55 → + `slowness`; 80 → + `mining_fatigue`; 40 → alleen nog honger. 100 met `difficulty hard` → `data get entity Dev Health` daalt elke ~4 s; Dev sterft → serverlog toont "Dev dried out". `difficulty normal` → Health stopt op 1; `easy` → op 10; `peaceful` → geen schade (vraag 6 → A).
9. **Creatief:** `gamemode creative Dev` in de zon → geen opbouw (volgens 19 vraag 6).
10. **Waterfles:** niet via RCON te doen (geen commando laat een speler drinken). Gedekt door `WaterBottleDrinkTest` + `PlayerConditions.add` (getest in 19); de eventkoppeling zelf in W-achteraf.
11. Daarna RCON uit, testwereld weg (`level-name` terug).

**Blokkerende handmatige test: NEE** — alle gameplay draait server-side en is via RCON meetbaar (stap 1–9). De enige client-kant (HUD-balk, sync, resync na respawn) is in change 19 blokkerend getest en verandert hier niet; deze change voegt alleen een enumwaarde met kleur toe. Het enige niet-geautomatiseerde stuk (de event-koppeling van de waterfles) is één regel registratie op een standaard-NeoForge-event; een fout daar raakt geen latere change en valt in W-achteraf direct op.

**W-achteraf** (niet blokkerend, ~5 min, seed `20260930`, client in ring 2):
- [ ] Waterfles drinken bij meter ~50 → balk zakt direct ~30; lege fles terug; geen effect bij meter 0.
- [ ] HUD-balk in de kleur van uitdroging verschijnt in de zon, streepjes op 25/50/75/100.
- [ ] Onder palmbladeren in de oase / onder een overhang in een canyon voelt "schaduw" logisch (vraag 2); glas geeft geen schaduw.
- [ ] Tempo voelt goed (vraag 1): een middag in de zon is spannend, niet frustrerend; een waterfles helpt merkbaar.
- [ ] Doodsbericht "dried out" in de chat bij sterven op hard.

**W** — geen worldgen-wijziging; standaard portal-test niet nodig.

**Checklist (concept)**
- [ ] Scope `conditions` benoemd; namen van 19 (en 18, 20, 23) gecontroleerd en dit plan aangepast
- [ ] B groen (incl. nieuwe unit tests); D groen en output gecommit
- [ ] S: smoke-test + RCON-stappen 1–9 groen
- [ ] `CODEBASE.md`, `fase-1-wereld.md` (Ring 2, uitwerking change 27), beslissingenlog, backlog bijgewerkt
- [ ] `/code-review` gedaan, bevindingen afgehandeld
- [ ] Commit `conditions: add dehydration condition for ring 2`
- [ ] W-achteraf gemeld aan de hoofdsessie

---

## Risico's

- **Change 19 wijkt af** (andere namen, drie vaste drempels, `isDay()` = vanilla, ander handlerpatroon): bij de start tegen de code leggen; deze change volgt 19 en breidt binnen `conditions` uit waar nodig.
- **Change 23 levert geen `oasis_flats`-key of een andere naam:** dan die naam gebruiken; ontbreekt het biome nog helemaal, dan stoppen en de hoofdsessie vragen (de oase-regel is verplicht in de S-test).
- **`canSeeSky` en lichtbronnen:** sky light 15 betekent "geen lichtdempend blok erboven". Glas laat hemellicht door (= zon), bladeren dempen 1 (= schaduw), water dempt (onder water = toch al `WATER`). Ook 1 blok onder een overhang telt als schaduw (hemellicht 14 zijwaarts). Bij de bouw verifiëren en in de valkuilen zetten.
- **Lichtupdates lopen achter** na `fill` in de S-test: na het plaatsen van een dak 1–2 s wachten vóór de meting.
- **Meting in stappen van 20 ticks:** 10 s wachten = 9–11 handler-tikken; tolerantie ± 1 punt (bij 2,0/s ± 2).
- **Emmer als draagbare oase** (vraag 7): bij optie B trivialiseert een waterbak/emmer de conditie en wordt de fase 2-veldfles minder zinvol.
- **Doodsspiraal bij een bed in ring 2:** afhankelijk van 19 vraag 6 (meters naar 0 bij dood = geen spiraal).
- **Hongereffect + al lage honger** versnelt verhongeren; samen met de uitdrogingsschade kan dat op hard snel dodelijk zijn. Bewust (survival), maar meenemen in de W-achteraf-check "tempo".

---

## Open ontwerpvragen

Ronde 1: vraag 1–4. Ronde 2: vraag 5–7. Het design legt vast **dat** zon/schaduw/water/oase/nacht/waterfles werken en welke effecten de drempels hebben; deze vragen gaan over wat het design open laat.

### 1. Tempo: hoe snel loopt de meter op en af?
In alle opties: afnames stapelen niet (de sterkste bron wint), water en oase koelen 4× sneller dan schaduw/nacht, waterfles −30; alles per wereld in de config bij te stellen. Hittegolf (change 28) verdubbelt de opbouw.
- **A. Opbouw 0,4/s (0 → 100 in ~4 min volle zon; hittegolf ~2 min), afname 0,5/s, water/oase 2/s (aanbevolen).** Een ring 2-dag (~10 min) kun je niet in de volle zon doorlopen, maar een paar minuten oversteken kan; een waterfles = ~75 s extra zon. Voelbaar als eerste conditie zonder te straffen.
- **B. Mild: opbouw 0,25/s (~6,7 min; hittegolf ~3,3 min), afname 0,5/s, water/oase 2/s.** Introductievriendelijk; risico dat spelers de conditie nauwelijks merken buiten hittegolven.
- **C. Streng: opbouw 0,8/s (~2 min; hittegolf ~1 min), afname 0,5/s, water/oase 3/s.** Echte survival-druk; waterflessen en schaduw worden een constante bezigheid, kan in multiplayer frustreren.

Antwoord: **A** — opbouw 0,4/s (0 → 100 in ~4 min volle zon; hittegolf ~2 min), afname 0,5/s, water/oase 2/s; afnames stapelen niet.

### 2. Wat telt als "direct zonlicht" en wat als schaduw?
- **A. Hemellicht 15 op ooghoogte (`canSeeSky`) (aanbevolen).** Elk blok dat licht dempt geeft schaduw: daken, overhangen (ook 1 blok eronder), bladeren (palmen in de oase). Glas geeft géén schaduw (kas-effect). Goedkoop (één lichtlookup), voorspelbaar, geen zonnestand.
- **B. Iets vasts recht boven het hoofd (heightmap `MOTION_BLOCKING`).** Simpel uit te leggen ("dak = schaduw"), ook glas telt als dak; maar naast een overhang of in een smalle kloof sta je in de "zon" zolang er niets recht boven je is.
- **C. Raycast richting de zon.** Realistisch: canyonwanden geven 's ochtends/'s avonds schaduw aan één kant. Duurder (één raycast per speler per seconde) en verwarrend, want Minecraft tekent geen schaduwen: de speler ziet niet waar het "schaduw" is.

Antwoord: **A** — hemellicht 15 op ooghoogte (`canSeeSky`); glas geeft geen schaduw.

### 3. Wanneer is het "overdag"?
- **A. Zon boven de horizon, alleen op de tijd van de dag; vanilla-regen/onweer telt niet (aanbevolen).** Ring 2 heeft geen neerslag, dus vanilla-regen (globaal, alleen zichtbaar in ring 1) mag de uitdroging niet stilleggen. Harde wissel bij zonsopgang/-ondergang; met `doDaylightCycle false` blijft het wat het is.
- **B. Opbouw schaalt met de zonnestand** (maximaal rond de middag, 0 bij zonsopgang/-ondergang). Vloeiender en "logischer", maar lastiger uit te leggen en te testen; de rate in de config is dan een maximum.
- **C. Vanilla `level.isDay()`.** Eén regel code, maar vanilla rekent regen- en onweerverduistering mee: een onweersbui (onzichtbaar in ring 2) stopt de uitdroging.

Antwoord: **A** — zon boven de horizon, alleen op de tijd van de dag (`isSunUp()`); vanilla-regen/onweer telt niet.

### 4. Waar geldt de conditie aan de ringgrenzen?
In alle opties: alleen overworld (Nether-ring 2 heeft geen conditie); buiten de zone zakt de meter met `fallRateOutside`.
- **A. Hard op de ringgrens: `ringAt(...).ring() == RING_2` (wissel op blend 0,5) (aanbevolen).** Zelfde regel als de biome-wissel en het gameplay-weer van change 20; eenvoudig en testbaar. De buitenste helft van `withered_expanse` en de binnenste helft van `dry_riverbeds` drogen uit.
- **B. Geleidelijk over de overgangszone** (opbouw × smoothstep van het ringgewicht, ~128 blokken). Geen harde rand, voelt natuurlijk; maar een speler kan niet zien waar hij staat, en de rate is minder voorspelbaar (afwijkend van change 20).
- **C. Hele overgangszones meetellen** (ook `withered_expanse` volledig en `dry_riverbeds` volledig). De droge overgangsbiomes "kloppen" thematisch; maar uitdroging loopt dan ook in een deel van ring 1 en ring 3 (waar hitte begint) op.

Antwoord: **A** — hard op de ringgrens: `ringAt(...).ring() == RING_2` (wissel op 0,5); alleen overworld.

### 5. Op welke meterstand vallen de vier drempels?
Effecten liggen vast in het design: sneller honger → traagheid → mining fatigue → lichte schade. Getallen staan in de config.
- **A. 25 / 50 / 75 / 100 (aanbevolen).** Gelijkmatig; het hongereffect (groene hongerbalk) is een vroeg, mild waarschuwingssignaal, schade pas bij een volle meter.
- **B. 40 / 60 / 80 / 100.** Langer niets aan de hand, daarna snel achter elkaar; minder waarschuwing vooraf.
- **C. 50 / 50 / 75 / 100** (honger en traagheid samen, zoals het voorbeeld in §1.9). Eén drempel minder om te onthouden; de eerste helft van de meter heeft geen gevolg.

Antwoord: **A** — drempels 25 / 50 / 75 / 100.

### 6. Hoe werkt de "lichte schade" bij 100?
In alle opties: eigen damage type `corruptedadventure:dehydration` met doodsbericht "dried out" / "is uitgedroogd", schade gaat door harnas heen (zoals verhongeren).
- **A. Zoals vanilla-verhongeren: 1 HP per 4 s, ondergrens volgens de moeilijkheid — peaceful geen schade, easy tot 10 HP, normal tot 1 HP, hard dodelijk (aanbevolen).** Vertrouwd voor spelers, de moeilijkheidsgraad regelt de straf; past bij "sneller honger". `DamageThreshold` krijgt een interval en een moeilijkheidsondergrens (herbruikbaar voor latere condities). Raakt vraag 6 van change 19 (peaceful).
- **B. 1 HP per 4 s, altijd dodelijk (ook op easy/normal).** Strengst en meest "horror"; uitdroging wordt een echte doodsoorzaak, ook voor casual spelers.
- **C. 1 HP per 4 s, nooit dodelijk (stopt op 1 HP), op elke moeilijkheid.** Veilig; maar dan is een volle meter na een tijd alleen nog hinderlijk, en een mob doet de rest.

Antwoord: **A** — zoals vanilla-verhongeren: 1 HP per 4 s, `DifficultyFloor.VANILLA_STARVATION` (peaceful geen schade, easy tot 10 HP, normal tot 1 HP, hard dodelijk).

### 7. Wat telt als "in water"?
- **A. Ondergedompeld tot boven het middel (water ≥ ~1 blok hoog rond de speler; zwemmen telt) (aanbevolen).** Oasepoelen, rivieren en ondergrondse reservoirs werken; één emmer onder je voeten niet. Houdt water schaars (design: oase = "het enige veilige water") en de fase 2-veldfles zinvol. Een put graven met twee emmers kan nog steeds (bewuste moeite).
- **B. Elk contact met water (`isInWater`, ook een plasje op voethoogte).** Eenvoudigst; maar een emmer water is dan een draagbare oase en trivialiseert de conditie.
- **C. Alleen water in het oase-biome.** Strengst en thematisch; maar hetzelfde water gedraagt zich elders anders (onlogisch), en de ondergrondse reservoirs van change 26 tellen dan niet mee.

Antwoord: **A** — ondergedompeld tot boven het middel (water ≥ ~1 blok hoog rond de speler; zwemmen telt).

---

## Beslissingenlog (bij de bouw invullen met de datum van vandaag, één regel per antwoord)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | conditions | Uitdroging: tempo opbouw 0,4/s, afname 0,5/s, water/oase 2/s; afnames stapelen niet (sterkste bron wint); alleen direct zonlicht overdag in de zone geeft opbouw | Voelbaar als eerste conditie zonder te straffen; change 28 kan de opbouw zo zuiver ×2 doen |
| 2026-10-01 | conditions | Schaduw = geen hemellicht 15 op ooghoogte (`canSeeSky`) | Goedkoop en voorspelbaar; daken, overhangen en bladeren geven schaduw |
| 2026-10-01 | conditions | Dag voor uitdroging = zon boven de horizon (tijd van de dag), vanilla-regen telt niet | Ring 2 heeft geen neerslag |
| 2026-10-01 | conditions | Zone uitdroging aan de ringgrenzen: hard op `ring() == RING_2` (0,5); alleen overworld, Nether-ring 2 geen conditie | Zelfde regel als biome en ringweer |
| 2026-10-01 | conditions | Drempels uitdroging 25/50/75/100: honger → traagheid → mining fatigue → schade | Gelijkmatig, vroege milde waarschuwing |
| 2026-10-01 | conditions | Schade uitdroging: 1 HP per 4 s met `VANILLA_STARVATION`-ondergrens; eigen damage type `corruptedadventure:dehydration` met doodsbericht | Vertrouwd als verhongeren; moeilijkheid regelt de straf |
| 2026-10-01 | conditions | "In water" voor uitdroging = ondergedompeld tot boven het middel | Water blijft schaars; geen draagbare oase met één emmer |
| 2026-10-01 | conditions | Waterfles: alleen vanilla-waterfles (`Potions.WATER`), −30 (config), werkt overal, geen cooldown | Volgt het design; directe tegenmaatregel vóór de fase 2-veldfles |

---

## Bouwnotities (change 27, 2026-10-02)

- **Aannames over 18–26 klopten:** `Condition`, `ConditionContext` (record zonder afgeleide checks), `ConditionRule.recover`, `RateModifier`, `MobEffectThreshold`, `DamageThreshold` (met `intervalSeconds`) en `DifficultyFloor.VANILLA_STARVATION` uit 19; `ServerConfig` met een geneste klasse per conditie + `ThresholdLevels.define` (N drempels); `/ca condition get/set/debug`; `ModBiomes.OASIS_FLATS` (geen oase-tag); `RingWorld.isActive` in de ticker. Niets te generaliseren.
- **Context (vraag 2, 3, 7):** `ConditionContext.isSunUp()` (`Sun.isUp(dimensionType, dayTime)` = vanilla's zonnehoek `DimensionType.timeOfDay`; op = hoek < 0,25 of > 0,75 → zon op van dagtijd 23.215 t/m 12.785; regen/onweer genegeerd; vaste tijd (Nether) nooit), `canSeeSky()` (`level.canSeeSky` op het blok van de ogen = hemellicht 15), `isSubmerged()` (NeoForge `getFluidTypeHeight(WATER_TYPE)` ≥ 1,0 blok boven de voeten). Elke aanroep rekent opnieuw (één keer per tick per conditie).
- **Invoer-record:** `DehydrationRule.Input(inZone, sunUp, directSun, inWater, inOasis)` (genest, i.p.v. los `DehydrationInput`); `zoneWeight` vervallen (vraag 4 = A). Pure tabel `DehydrationRule.evaluate(Input, Rates)` → `Result(rate, exposure)`.
- **Bronnen per zone (uitwerking van "sterkste wint"):** water en oase koelen overal; nacht en schaduw tellen alleen in de zone (hun configsleutels zijn ring 2-waarden); buiten de zone geldt `fallRateOutside`. Dus water in ring 1 koelt met 2,0/s (sterkste van water en buiten). Bij gelijke rates wint de eerste in de volgorde water, oase, buiten, nacht, schaduw (alleen het label verschilt). Een `fallRate` van 0 geeft +0,0 (geen "−0.00/s").
- **Configsleutel:** `damageInterval` (zoals de `debug`-sectie) i.p.v. `damageIntervalSeconds` uit de tabel; `waterBottleAmount` is een double (30,0). Sectie `[conditions.dehydration]` staat in `config/corruptedadventure-server.toml`.
- **Damage type** `corruptedadventure:dehydration` = vanilla `starve` (`exhaustion` 0, scaling `when_caused_by_living_non_player`, msgId `corruptedadventure.dehydration`). Tags = die van `starve`: `minecraft:bypasses_armor` (→ ook `bypasses_shield`), `bypasses_effects`, `no_knockback`, `bypasses_wolf_armor` en NeoForge `neoforge:is_environment`.
- **Waterfles:** `LivingEntityUseItemEvent.Finish#getItem()` is in NeoForge 21.1.252 een **kopie van het stack vóór het drinken** (`LivingEntity.completeUsingItem`); `getResultStack()` is de lege fles. Check = `Items.POTION` met `PotionContents.is(Potions.WATER)`.
- **Debug-uitvoer:** `ConditionRule.debugLabel(ctx)` (default leeg) en `ConditionTicker.rate(player, condition, ctx)` (public, dezelfde berekening als de tick: regel → modifiers → creatief/spectator nooit omhoog). `/ca condition get` toont `dehydration 42.3 (+0.40/s, sun)`; in een niet-ringwereld alleen de waarden.
- **Extra tests** naast het plan: `ConditionMetersTest` (applySync met te lange array, nu lengte-onafhankelijk), `DamageThresholdTest` (40 s → 10 hits, 4 s uit elkaar), `ThresholdLevelsTest` (uitdroging 25/50/75/100).

### Testresultaten
- **B:** `./gradlew build` groen, 279 tests (nieuw: `DehydrationRuleTest`, `SunTest`, `WaterBottleDrinkTest`; uitgebreid: `DamageThresholdTest`, `ThresholdLevelsTest`, `ConditionMetersTest`); `ClientIsolationTest` groen.
- **D:** `./gradlew runData` groen: `data/corruptedadventure/damage_type/dehydration.json`, `data/minecraft/tags/damage_type/{bypasses_armor,bypasses_effects,no_knockback,bypasses_wolf_armor}.json`, `data/neoforge/tags/damage_type/is_environment.json`, lang en + nl (2 doodsberichten).
- **S:** `tools/server-smoke-test.sh` met `level-name=smoke-change27`: `Done (10.7 s)`, geen fouten. Daarna RCON + quick-play-client `Dev` (seed 20260930, `doDaylightCycle false`, `doMobSpawning false`). Elke meting: `ca condition set`, 10 s, `ca condition get`:
  1. **Zon** (canyonlands-kloofbodem 2307 85 204, geen oase): 0 → 4,0 `(+0.40/s, sun)`.
  2. **Schaduw** (dak 3 boven het hoofd): steen 50 → 45,0 `shade`; `oak_leaves[persistent=true]` 50 → 45,0 `shade`; glas 50 → 54,0 `sun`.
  3. **Nacht:** `time set midnight` 50 → 45,0 `night`. Wissel: 12000 en 12780 `sun`, 12830 en 13000 `night`, 23190 `night`, 23240/23500/0 `sun`. `weather thunder` (12 s) → blijft `sun` (+0,4/s).
  4. **Water** (glazen bak, 2 lagen): 50 → 30,0 `water`; 1 laag op voethoogte → 50 → 54,0 `sun`.
  5. **Oase** (`ca tpring 2` = 2499 66 12, `oasis_flats`, open lucht, middag): 50 → 30,0 `(-2.00/s, oasis)`.
  6. **Buiten ring 2:** ring 1 droog (700 88 60) 50 → 45,0 `outside`; ring 3 (4499 121 22) 50 → 45,0 `outside`; Nether-ring 2 (624 38 3) 50 → 45,0 `outside`. Het ringmidden van ring 1 (749 46 3) ligt in een meer: `water`, −2,0/s.
  7. **Grens:** grens 1 (1469 63 7): 20 blokken naar binnen (1449, ring 1, gewicht 0,35) `water` (meer), naar buiten (1489 68 7, ring 2, 0,66) in survival 0 → 4,0 `sun`. Grens 2 (3457 125 17): binnen (3437 129 17, ring 2, 0,37) 0 → 4,0 `sun`; buiten (3477 124 17, ring 3, 0,58) `outside`.
  8. **Drempels:** 30 → `minecraft:hunger` (ambient, geen particles, 240 ticks); 55 → + `slowness`; 80 → + `mining_fatigue`; 40 → alleen `hunger`. Bij 100: peaceful 10 s Health 20 → 20; easy (zonder `naturalRegeneration`) 12 → 11 → 10, dan 30 s op 10; normal 6,17 → 0,17 en daar blijven (vanilla-formule: schade zolang Health > 1, dus een gebroken waarde zakt onder 1, net als bij verhongeren); hard 3 → dood, serverlog en chat van de client `Dev dried out`; na respawn meter 0.
  9. **Creatief** in de zon: 0 → 0,0 `(+0.00/s, sun)`, geen effecten.
  10. **Wandbron** (2427 66 530, change 23c): in de kolk (2427 66 530, hier 1 diep) `sun`; in het stroompje (2426 66 533) `sun` (de stroming duwt de speler mee); onder de val (2425 66 534) `water` (vallend water heeft hoogte 1,0). Een kolk van 2 diep telt dus wel, deze 1 diepe niet.
  11. Daarna RCON uit, server via RCON `stop`, client-JVM gestopt.
  - Bij-meldingen in de serverlog: alleen `Unprimed heightmap: WORLD_SURFACE_WG` van een mislukte `place feature corruptedadventure:canyon_spring` op een geladen chunk (testpoging, backlog `[worldgen]`). Client-log zonder fouten.

### Code review (`/code-review medium`)
Geen correctheidsfouten. Twee opmerkingen zonder wijziging: (1) `/ca condition get` toont ook een rate voor dode spelers en fake players, die de tick overslaat (alleen debug-uitvoer); (2) de invoer van de regel (incl. de hemellicht-lookup) wordt per `get` twee keer berekend, voor rate en label (verwaarloosbaar, alleen bij het commando).

## In-game checklist (W)

Niet blokkerend (W-achteraf in de gebundelde ring 2-eindcheck na change 37), ~10 min. Wereld `corrupted_world`, seed `20260930`, survival, `/difficulty normal`. Eerst `/tp @s 100 ~ 0` (richting oost), dan landen `/ca tp…` op de punten hieronder.

1. **Opbouw + HUD:** `/time set noon`, `/gamerule doDaylightCycle false`, `/tp @s 2307 85 204` (kloofbodem canyonlands, open lucht). Een zandgele balk verschijnt boven de hongerbalk en loopt in ~4 min vol (`/ca condition get` toont `(+0.40/s, sun)`); streepjes op 25/50/75/100 %. Bij 25 groene hongerbalk (honger), bij 50 traagheid, bij 75 mining fatigue (geen particles, iconen knipperen niet), bij 100 elke 4 s een half hartje (stopt op normal bij een half hartje).
2. **Waterfles:** `/ca condition set @s dehydration 50`, `/give @s minecraft:potion[potion_contents={potion:"minecraft:water"}]` en opdrinken → balk zakt direct naar ~20 (`/ca condition get`: 20,x), lege fles terug in de hand. Nog een fles bij meter 0 → niets bijzonders. Een `splash_potion` van water gooien en een awkward potion drinken → geen verandering.
3. **Schaduw:** in de kloof bij 2307 85 204 onder een overhang of in een nis staan → `shade` (balk zakt 0,5/s). Een dak van glas boven je → blijft `sun`; een dak van bladeren → `shade`. Voelt "schaduw" logisch?
4. **Oase:** `/ca tpring 2` (2499 66 12, oase) → in de volle zon `oasis`, balk zakt 2/s. Onder palmbladeren idem (oase wint).
5. **Water:** in een oasepoel of de val van de wandbron bij 2425 66 534 → `water` (2/s); in het stroompje (2426 66 533) of de 1 diepe kolk (2427 66 530) blijft het `sun`.
6. **Nacht en buiten:** `/time set midnight` → `night`; `/ca tpring 1` en `/ca tpring 3` → `outside`.
7. **Doodsbericht:** `/difficulty hard`, `/ca condition set @s dehydration 100`, in de zon blijven (2307 85 204) → na ~80 s dood, chat "Dev dried out" (`nl_nl`: "Dev is uitgedroogd"); na respawn geen balk.
8. **Tempo:** een paar minuten door de kloven lopen in de middagzon: spannend maar niet frustrerend? Helpt één waterfles merkbaar (~75 s extra zon)?
