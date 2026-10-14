# Golf 1 — stroom C (condities): 69 gif

Scope: `conditions`, één commit, branch `ring4/conditions-g1` (basis `main` 7ffbfeb), poort 4 (server 25574, RCON 25584; niet gebruikt: licht testen). Bron: `contract.md` §2, §3 (`poison_sources`, `toxic_pool`), §5, §6 (factor gifregen), §8; `waves.md` §2b (C bouwt de gifregen-modifier, A levert de factor); `fase-1-wereld.md` → Ring 4 "Conditie: gif"; de hitte van ring 3 als voorbeeld (`HeatRule`, `HeatWeatherModifier`, `HeatBurn`, `ColdFreeze`).

## 1. Plan (gebouwd)

1. **`Condition.POISON`** (`poison`), HUD-kleur gifgroen `0xFF7BC832`. De HUD (`ConditionHudLayer`) en de sync (`ConditionSyncPayload`, per positie in `Condition.values()`) nemen hem vanzelf mee. Geen client-code.
2. **`PoisonRule`** + **`PoisonExposure`** (pure regeltabel zoals `HeatRule.evaluate`, testbaar zonder server):
   - **Zone:** overworld, `ringAt(...).ring() == RING_4` (hard op 0,5, `&` zoals `HeatRule.inZone`). Daarbuiten valt de meter met `outsideFall`, ook naast een gifbron (een emmer gif in ring 1 doet niets).
   - **Bronnen** (de sterkste wint, niets stapelt; bij gelijke rate houdt de poel het label):
     - **gifpoel:** een blok uit `#corruptedadventure:poison_sources` binnen `poolRadius` (1) rond voeten en hoofd (`NearbyBlocks`, de box van de hitte- en warmtebronnen, met palette-voorcheck; "in of naast"). Rate `poolRise` +3/s.
     - **moerasgas:** het blok op ooghoogte zit in `#corruptedadventure:poison_gases` (nieuwe tag, V3). Rate `gasRise` +2/s. Het blok komt in 72 (B); hier alleen de haak.
     - **gifregen:** in de modifier `ToxicRainPoisonModifier` (V2), niet in de regel. De regel leest geen weer (afspraak `ConditionContext`).
   - **Geen bron:** water (ook gifgroen) en gifzwammen doen niets; zonder bron valt de meter met `fall` (−1/s). Labels voor `/ca condition get`: `pool`, `gas`, `clean`, `outside` (in de regen: `clean` met een positieve rate).
3. **`ToxicRainPoisonModifier`** (stage `WEATHER`): rate = max(regel, `rainRise × m × intensiteit`) bij `TOXIC_RAIN` in `RingWeather.stateAt(level, x, z)`, onder open lucht (`canSeeSky` en oog niet onder water) en in de bedorven band (`Ring4Band.of(RingLayout.current()).blighted(x, z)`). Lucht en band worden pas gevraagd als het gifregent, de band als laatste. `m` = `toxicRainPoisonMultiplier` van A: tot INT1 `DEFAULT_RAIN_MULTIPLIER` (1,0) via `rainMultiplier()`.
4. **Drempels** (cumulatief, `threshold1..3` = 30 / 60 / 90):
   - 30: misselijkheid (`MobEffects.CONFUSION`, `MobEffectThreshold`, `effect1Amplifier`);
   - 60: vanilla-gif (`MobEffects.POISON`, `effect2Amplifier`; doodt niet);
   - 90: **`PoisonWither`** (kan doden vanaf normal). Het vanilla Wither-effect geeft de zwarte harten. "Onze" Wither is een korte instantie (40 ticks, elke seconde ververst) die ambient is, zonder particles en zonder icoon. Elke `damageInterval` (2) s volgt `damage` (1,0) als `corruptedadventure:poison`, met `DifficultyFloor.LETHAL_FROM_NORMAL` (peaceful niets, easy tot 10 HP, normal/hard dodelijk) en een eigen doodsbericht. De vanilla witherschade wordt geannuleerd (`LivingIncomingDamageEvent`) zolang de actieve Wither de onze is, ook direct na het inloggen. Een vreemde Wither (witherskelet, witherroos, drankje) wordt gerespecteerd: vanilla voegt hem in de onze samen en neemt daarbij zijn vlaggen over, waardoor hij niet meer de onze is. We verversen er dan niet overheen, zijn vanilla-schade telt (bovenop de onze: twee oorzaken) en `clear()` laat hem staan. Van de onze blijft na zo'n samenvoeging hooguit 40 ticks over.
5. **Noodmiddelen** (`PoisonRemedies`, zoals `WaterBottleDrink`): melkemmer −40 (`milkAmount`), honingfles −20 (`honeyAmount`), via `LivingEntityUseItemEvent.Finish`, overal, zonder cooldown, alleen echte spelers. Melk wist (vanilla) alle effecten, ook de onze. Staat de meter nog boven een drempel, dan zet de volgende tick (≤ 1 s) ze terug.
6. **Config `[conditions.poison]`** (`ServerConfig.PoisonCondition`, een eigen klasse plus 3 regels in het static-blok): `poolRise` 3,0, `poolRadius` 1 (1–4), `gasRise` 2,0, `rainRise` 0,5, `fall` 1,0, `outsideFall` 1,0, `milkAmount` 40, `honeyAmount` 20, `threshold1..3` 30/60/90, `effect1..3Amplifier` 0, `damage` 1,0, `damageInterval` 2.
7. **Lang** (`Ring4ConditionText`, en + nl): het doodsbericht `death.attack.corruptedadventure.poison` (+ `.player`): "succumbed to the poison of the blighted marsh" / "bezweek aan het gif van het bedorven moeras".
8. **Damage type** `corruptedadventure:poison` (`Ring4DamageTypes`, zoals vanilla `wither`: geen exhaustion). Tags in `datagen/tags/Ring4DamageTypeTags` (nieuw): `minecraft:bypasses_armor`, `minecraft:no_knockback`, `minecraft:bypasses_cooldown` (de vanilla-gif van drempel 2 tikt vaak net ervoor), `c:is_poison`, `c:is_environment`.
9. **Registratie:** `Ring4Conditions.register(gameBus)` vanuit `ConditionTicker.register` (patroon `Ring3Conditions`): de modifier, de noodmiddelen en de witherhaak.

## 2. Client/server-impact

- Alles server-side: regel, modifier, drempels, noodmiddelen, schade, de annulering van witherschade. Geen client-classes in `condition/`; geen `Minecraft.getInstance()`.
- Sync via het bestaande `condition_sync`: de payload wordt één int langer (een nieuwe enum-positie). **INT verhoogt `ModNetwork.PROTOCOL_VERSION`** (één keer per golf).
- HUD: de nieuwe balk komt vanzelf (kleur uit de enum, drempelstreepjes uit de gesyncte config). Zwarte harten en het gif-effect: vanilla, via de effect-sync.
- Prestatie: per speler per seconde, alleen in ring 4: één `NearbyBlocks`-scan met radius 1 (palette-voorcheck, meestal 0 blokken gelezen) en één blockstate op ooghoogte. Overal één `ringAt` extra in `stateAt`; de lucht en de band alleen tijdens gifregen.

## 3. Tests (licht, 2026-10-13)

- `./gradlew build`: geslaagd (2 × ~2 m 45 s). `./gradlew runData`: geslaagd. `git diff --stat src/generated` bevat alleen eigen output: lang en+nl (+3 elk), `damage_type/poison.json`, tags `bypasses_armor`, `no_knockback`, `is_environment` (+1 elk), `neoforge:is_poison` (nieuw).
- Unit tests: `PoisonRuleTest` (8: contract-startwaarden, poel ~33 s, gas 50 s, de sterkste wint en niets stapelt, zonder bron dalen, buiten de zone dalen ook bij een bron, +0,0, zone hard op 0,5), `PoisonModifiersTest` (9: regen 0,5/s ≈ 3,3 min, fade en factor, sterkere bron wint, geen regen of intensiteit 0 = geen lucht- of bandvraag, alleen open lucht en bedorven band, band met de echte `Ring4Band` (6.100 mythisch, 7.900 bedorven), alleen melk en honing, annulering alleen bij onze ambient Wither, andere condities onaangeroerd), `ThresholdLevelsTest` +1 (30/60/90). Allemaal groen.
- Geen runServer, determinisme of perf (INT).

## Code-review (c77a738, 2026-10-13)

Opgelost in de vervolgcommit:
1. **Hurt-cooldown:** `corruptedadventure:poison` staat in `minecraft:bypasses_cooldown`. De vanilla-gif (1,0 per 25 ticks) tikt vaak een paar ticks eerder, en binnen de cooldown slikt vanilla een even grote hit in. Test: `PoisonDamageTypeTagsTest` op de gegenereerde tags.
2. **Vreemde Wither:** onze Wither wordt herkend aan ambient én geen particles én geen icoon, en is kort (40 ticks). Semantiek: zie §1.4. Test: samenvoegen met witherroos, witherskelet en een sterkere Wither via `MobEffectInstance.update`.
3. **Na het inloggen:** de annulering hangt niet meer af van `appliedThresholds` (UNKNOWN na het laden), alleen van de vlaggen van de actieve instantie. Een opgeslagen Wither van ons doet dus ook in de eerste seconde geen vanilla-schade; de eerste tick ruimt hem op als de meter onder 90 staat.
4. **Moeilijkheid:** nieuwe vloer `DifficultyFloor.LETHAL_FROM_NORMAL` (peaceful niets, easy tot 10 HP zoals honger, normal en hard dodelijk). `VANILLA_STARVATION` zou op normal bij 1 HP stoppen, en dat botst met "kan doden" in contract §5. Bevestigd door de gebruiker (2026-10-13).
5. De ongebruikte lang-key `condition.corruptedadventure.poison` is weg; de doodsbericht-keys blijven.

Bewust niet opgepakt:
- `stateAt` in plaats van `state(level, ring)`: bewust, voor de band-semantiek van A.
- `ConditionContext`-helpers en een gedeelde `inZone`-helper: backlog (INT).
- De hardgecodeerde factor: INT1 koppelt hem aan `[weather.ring4]`. Het commentaar bij `rainRise` zegt nu "linked in integration 1".
- `CODEBASE.md`: INT.

## Koppeling met A (INT1)

- `ToxicRainPoisonModifier.rainMultiplier()` geeft `DEFAULT_RAIN_MULTIPLIER` (1,0). INT1 laat hem `ServerConfig.RING_4_WEATHER.toxicRainPoisonMultiplier.get()` lezen (A: standaard 1,0, bereik 0–10), daarna kan de constante weg (de test gebruikt hem nu als m).
- De modifier leest het weer via `RingWeather.stateAt(level, x, z)` (A maakt die band-bewust) **plus** een expliciete `Ring4Band.blighted(x, z)`. Na de merge van A controleert INT1 dat beide dezelfde grens gebruiken (`value ≥ 0`, verzoek van main).

## Testdatapack voor INT (S-smoke)

`<world>/datapacks/poison-test/pack.mcmeta` (`pack_format` 48) met `data/corruptedadventure/tags/block/poison_sources.json` = `{"values":["minecraft:slime_block"]}` en `poison_gases.json` = `{"values":["minecraft:cobweb"]}`. Daarna in ring 4 controleren:
- naast slijm +3/s (`/ca condition get` → `pool`);
- hoofd in spinnenweb +2/s (`gas`);
- gifregen (`/ca weather`) op een bedorven plek onder open lucht +0,5/s, op een mythische plek niets;
- melk −40, honing −20;
- vanaf 90 Wither-harten, schade alleen als `corruptedadventure:poison` (geen dubbele hits) en het eigen doodsbericht.

## Afwijkingen

- **`ModDamageTypeTagsProvider`**: één import plus de aanroep `Ring4DamageTypeTags.addTags(this::tag)`, zoals de ring 3-aanroep. Er was geen lege haak voor damage-type-tags in 58.
- **`ModTags`**: de TagKey `Blocks.POISON_GASES` naast `POISON_SOURCES`, in opdracht van main (V3). Main past het contract aan.
- **`ThresholdLevelsTest`**: één test (`poisonHasTheContractThresholds`), omdat `ThresholdLevels.defaults()` package-private is in `config`.

## Voortgang
- 2026-10-13 19:43: branch aangemaakt op 7ffbfeb (de worktree stond eerst op 17c9eb4 en is gereset); plan geschreven; de naamvraag en V1–V3 naar team-lead.
- 2026-10-13 19:50: alles gebouwd; build groen.
- 2026-10-13 19:55: antwoorden V1–V3 verwerkt (gas-tag naar `ModTags`); runData en build groen; gecommit als c77a738.
- 2026-10-13 20:06: code-review 1–5 verwerkt; runData (alleen eigen output: `bypasses_cooldown.json` nieuw, de meter-key weg uit en+nl) en build groen. Tests: PoisonRuleTest 8, PoisonModifiersTest 11, PoisonDamageTypeTagsTest 1, ThresholdLevelsTest 6.

## CODEBASE-delta

- `condition/Condition`: `POISON` (`poison`, HUD `0xFF7BC832`), ring 4.
- `condition/PoisonRule` (+ `Input`, `Rates`, `Result`), `condition/PoisonExposure` (`pool`, `gas`, `clean`, `outside`): de regel van gif. Zone overworld ring 4 (hard op 0,5). Gifpoel (`#poison_sources` binnen `poolRadius`, `NearbyBlocks`) +3/s, hoofd in gas (`#poison_gases` op ooghoogte) +2/s, de sterkste wint; anders −`fall`, buiten de zone −`outsideFall`.
- `condition/ToxicRainPoisonModifier` (stage `WEATHER`): gifregen als bron, max(rate, `rainRise × toxicRainPoisonMultiplier × intensiteit`) onder open lucht (oog niet onder water) in de bedorven band (`RingWeather.stateAt` + `Ring4Band.blighted`).
- `condition/PoisonWither` (drempel 3): eigen korte Wither (ambient, geen particles, geen icoon; `isOurs`) voor de harten, schade `Ring4DamageTypes.POISON` elke `damageInterval` s met `DifficultyFloor.LETHAL_FROM_NORMAL`; annuleert `minecraft:wither`-schade zolang de actieve Wither de onze is; een vreemde Wither blijft ongemoeid.
- `condition/DifficultyFloor.LETHAL_FROM_NORMAL`: peaceful geen schade, easy tot 10 HP, normal en hard dodelijk.
- `condition/PoisonRemedies`: melkemmer −`milkAmount` (40), honingfles −`honeyAmount` (20), via `LivingEntityUseItemEvent.Finish`.
- `condition/Ring4Conditions`: registreert de modifier, de noodmiddelen en de witherhaak; aangeroepen vanuit `ConditionTicker.register`.
- `condition/Ring4DamageTypes`: `POISON` = `corruptedadventure:poison` (msgId `corruptedadventure.poison`).
- `condition/ConditionDefinitions`: `POISON` → `PoisonRule`, drempels misselijkheid / gif / `PoisonWither`.
- `config/ServerConfig`: `POISON` = `PoisonCondition` (`[conditions.poison]`: `poolRise`, `poolRadius`, `gasRise`, `rainRise`, `fall`, `outsideFall`, `milkAmount`, `honeyAmount`, `threshold1..3`, `effect1..3Amplifier`, `damage`, `damageInterval`).
- `registry/ModTags.Blocks.POISON_GASES` = `#corruptedadventure:poison_gases` (hoofd in gas; inhoud B in 72).
- `datagen/tags/Ring4DamageTypeTags`: `poison` in `bypasses_armor`, `no_knockback`, `bypasses_cooldown`, `c:is_poison`, `c:is_environment`; aangeroepen vanuit `ModDamageTypeTagsProvider`.
- `datagen/lang/Ring4ConditionText`: `death.attack.corruptedadventure.poison` (+ `.player`), en + nl. Geen meternaam (zoals ring 3: de HUD tekent een balk).
- Gegenereerd: `data/corruptedadventure/damage_type/poison.json`, `data/neoforge/tags/damage_type/is_poison.json`, regels in `bypasses_armor`, `no_knockback`, `is_environment` en de lang-bestanden.
- Netwerk: `condition_sync` één int langer → `PROTOCOL_VERSION` +1 (INT).

## Beslissingen
- 2026-10-13, conditions: de gifconditie heet `poison` (`Condition.POISON`, `[conditions.poison]`, lang `condition.corruptedadventure.poison`, damage type `corruptedadventure:poison`). Besluit van de gebruiker.
- 2026-10-13, conditions: wither-drempel (90) = het vanilla Wither-effect voor het beeld plus eigen gifschade `corruptedadventure:poison` (eigen doodsbericht, `damage`/`damageInterval` in de config). De vanilla witherschade wordt geannuleerd zolang onze ambient Wither actief is. Besluit van de gebruiker (V1 a).
- 2026-10-13, conditions: gifregen is een bron met rate `rainRise × toxicRainPoisonMultiplier × intensiteit` (m standaard 1,0, van A); de sterkste bron wint, niets stapelt. Besluit van de gebruiker (V2).
- 2026-10-13, conditions: nieuwe blok-tag `corruptedadventure:poison_gases` voor het moerasgas (hoofd in gas = +2/s), TagKey in `ModTags`; B vult hem in 72. Besluit van de gebruiker (V3 a); main past het contract aan.
- 2026-10-13, conditions: "naast" de gifpoel = binnen 1 blok rond voeten en hoofd (`poolRadius`); gifregen telt niet met het oog onder water; aparte daling `fall` (in ring 4 zonder bron) en `outsideFall` (buiten ring 4), allebei 1,0/s. Implementatiekeuzes, akkoord van de gebruiker.

- 2026-10-13, conditions (review 69, besluit van de gebruiker): de gifschade van de wither volgt `DifficultyFloor.LETHAL_FROM_NORMAL` (peaceful niets, easy tot 10 HP, normal en hard dodelijk), omdat "kan doden" uit contract §5 anders alleen op hard zou gelden. Bevestigd door de gebruiker. Onze Wither = kort, ambient, zonder particles en zonder icoon; een vreemde Wither wordt gerespecteerd.

## Backlog
- conditions: na INT1 `ToxicRainPoisonModifier.rainMultiplier()` aan `[weather.ring4] toxicRainPoisonMultiplier` koppelen en `DEFAULT_RAIN_MULTIPLIER` weghalen (INT1).
- conditions (79): zuiverend in Nether-4 (`RateModifier.Stage.PURIFYING`, ×3 op de daling) en de gifkoppelingen met de blokken van 72 (`toxic_pool` in `poison_sources`, `swamp_gas` in `poison_gases`; B vult de tags).
- conditions (INT): de `ConditionContext`-helpers (oog in blok, onder water) en een gedeelde `inZone`-helper voor hitte, kou en gif (review 69).
- conditions (5b): de startwaarden afstemmen (poel, gas, regen, daling, schade van de wither).
