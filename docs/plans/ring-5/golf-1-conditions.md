# Ring 5 golf 1 — stroom C (condities): 91 void-meter

Scope: `conditions` — één commit (`conditions:`), branch `ring5/conditions-g1`, worktree
`P:/Projecten/Java/wt/r5-conditions-g1`, poort n = 4 (server 25574, RCON 25584), basis = de startcommit van `golf-1.md`.

Bronnen: `contract.md` §5 en **§11** (getallen: 0→100 in ~6 min binnen, ~3 min buiten/bij `void_sources`, ×1,5–2 's nachts
en bij ringweer, drempels 30/60/90, licht ≥ 12 laat zakken; teleports 1–5 blokken elke ~20 s bij ≥ 60, alleen vaste grond
in de binnenband); `waves.md` §2b (de void-modifier van het ringweer bouwt C, A levert de factoren), §3; `fase-1-wereld.md`
→ Ring 5 "Conditie: void/glitches" en "Client/server-risico's"; voorbeeld: gif van ring 4 (`docs/plans/ring-4/
golf-1-conditions.md`: `PoisonRule`, `PoisonExposure`, `ToxicRainPoisonModifier`, `PoisonWither`, `Ring4Conditions`,
`Ring4DamageTypes`, `Ring4DamageTypeTags`). `CODEBASE.md` kent golf 0 nog niet: lees de CODEBASE-delta van
`82-worldgen.md` (`Ring5Band`) erbij. Voorstellen ter bevestiging: `golf-1.md` §7 P3, P4, P5 (gelden tenzij de gebruiker
anders zegt).

**Bezit** (`waves.md` §1/§5): `condition/*` (void), `ServerConfig` `[conditions.void]` (eigen klasse `VoidCondition` + de
regels in het static-blok en `ALL_THRESHOLDS`), `Ring5ConditionText`, `Ring5DamageTypes`, nieuw
`datagen/tags/Ring5DamageTypeTags` (één aanroep in `ModDamageTypeTagsProvider`, zoals ring 4). **Niet:** `[weather.ring5]`
(A), `RingWeather*` (A), `Ring5Band` (W-terrein), de HUD-code (bestaat; de nieuwe balk komt vanzelf).

---

## 1. Plan

1. **`Condition.VOID`** (`void`), HUD-kleur violet `0xFF8A4FD8`, **achteraan** de enum (sync per positie; de bestaande
   posities blijven). HUD en `condition_sync` nemen hem vanzelf mee.
2. **`VoidRule`** + **`VoidExposure`** (pure regeltabel zoals `PoisonRule.evaluate`, testbaar zonder server):
   - **Zone:** overworld van een ringwereld, `ringAt(...).ring()` ∈ {`RING_5`, `VOID`} (hard op 0,5; de void-rand
     9.700–10.200 telt als buitenband, P4 — zoals het weer, waar `void` ring 5 volgt). Daarbuiten valt de meter met
     `outsideFall`; in Nether-4 sneller via de bestaande `PurifyingModifier`.
   - **Band:** buiten = `Ring5Band.of(RingLayout.current()).outer(x, z)` of ring `VOID`; binnen = `inner`. Overworld-x, z.
   - **Opbouw** (de sterkste wint, niets stapelt, zoals gif): binnenband `innerRise` **0,28/s** (~6 min); buitenband
     `outerRise` **0,55/s** (~3 min); een blok uit `#corruptedadventure:void_sources` binnen `sourceRadius` (4) rond voeten
     en hoofd (`NearbyBlocks`, palette-voorcheck) `sourceRise` **0,55/s**.
   - **Licht:** bloklicht op ooghoogte ≥ `lightLevel` (12) → de meter **zakt** met `lightFall` (1,0/s), ook als er een bron
     of de buitenband is ("licht houdt de void weg"). Alleen bloklicht, geen daglicht.
   - Labels voor `/ca condition get`: `inner`, `outer`, `source`, `light`, `outside`.
3. **`VoidAmplifierModifier`** (stage `WEATHER`, één modifier voor nacht en weer, P5): alleen op een positieve void-rate,
   factor = max(nacht, weer), **niet** het product:
   - nacht (`Sun`/de dag-tijd van het level, zoals de andere condities) → `nightMultiplier` (1,5);
   - ringweer: `RingWeather.stateAt(level, x, z)` (A maakt hem band-bewust; C vraagt nooit zelf de band voor het weer) →
     `1 + (factor(type) − 1) × intensiteit` met `factor` = glitchstorm 2,0, omgekeerde regen 1,5, stilstand 1,5.
     **Tot INT1** komen die drie uit constanten `DEFAULT_*_VOID_FACTOR` via `weatherFactor(type)`; INT1 laat ze
     `ServerConfig.RING_5_WEATHER.{glitchStorm,reverseRain,standstill}VoidMultiplier` lezen (A, `[weather.ring5]`) en
     haalt de constanten weg. Gaat `STANDSTILL` pas met A bestaan: `weatherFactor` kent het type tot dan niet (`default`
     → 1); INT1 voegt de regel toe als dat nodig is.
   - Het weer wordt pas gevraagd als de rate positief is en de speler in de zone staat.
4. **Drempels** (cumulatief, `threshold1..3` = 30 / 60 / 90; `ThresholdLevels`):
   - **30 — glitch-visuals:** server niets (no-op-effect); de client leest de gesyncte meter (A, 98).
   - **60 — `VoidSlip`** (één effect dat per tick de band bekijkt):
     - **binnenband:** elke `teleportIntervalSeconds` (20) een korte teleport (§2), alleen als de speler op vaste grond
       staat; de timer is per speler in het geheugen (niet opgeslagen; na inloggen opnieuw 20 s);
     - **buitenband (en `VOID`):** in plaats van teleports **darkness** (`MobEffects.DARKNESS`, `darknessAmplifier` 0,
       elke seconde ververst zoals de andere `MobEffectThreshold`'s).
   - **90 — darkness + void-schade** (overal in de zone, dus ook binnen; de teleports van drempel 2 lopen binnen door):
     darkness als bij 60 en elke `damageInterval` (2) s `damage` (1,0) als `corruptedadventure:void` met
     `DifficultyFloor.LETHAL_FROM_NORMAL` (peaceful niets, easy tot 10 HP, normal/hard dodelijk; P3, zoals gif).
5. **Damage type** `corruptedadventure:void` (`Ring5DamageTypes.bootstrap`, geen exhaustion); tags in
   `datagen/tags/Ring5DamageTypeTags`: `minecraft:bypasses_armor`, `minecraft:no_knockback`, `minecraft:bypasses_cooldown`
   (andere schade kan net ervoor tikken, zoals bij gif), `c:is_environment`. Doodsbericht `death.attack.corruptedadventure.void`
   (+ `.player`): "was unmade by the void" / "werd door de void uitgewist" (`Ring5ConditionText`, en + nl).
6. **Config `[conditions.void]`** (`ServerConfig.VoidCondition`): `innerRise` 0,28, `outerRise` 0,55, `sourceRise` 0,55,
   `sourceRadius` 4 (1–8), `nightMultiplier` 1,5 (1–4), `lightLevel` 12 (1–15), `lightFall` 1,0, `outsideFall` 1,0,
   `threshold1..3` 30/60/90, `teleportMinBlocks` 1, `teleportMaxBlocks` 5 (≥ min), `teleportIntervalSeconds` 20 (5–600),
   `darknessAmplifier` 0, `damage` 1,0, `damageInterval` 2. Commentaar per sleutel met de tijd 0→100.
7. **Registratie:** `Ring5Conditions.register(gameBus)` vanuit `ConditionTicker.register` (patroon `Ring4Conditions`):
   de modifier, het teleport-effect (zijn tick/timer) en de schade-haak. `ConditionDefinitions`: de `VOID`-takken in
   `rule`, `thresholdLevels`, `effects`.

## 2. Veilige teleport (`VoidTeleport`, server)

Alleen als: overleven/avontuur (niet creatief/spectator), `onGround`, geen passagier, niet slapend, niet zwevend met
elytra, niet in een vloeistof, voeten in `Ring5Band.inner` en ring 5. Zoeken (`VoidTeleportTarget`, puur achter een klein
interface naar het level zodat het testbaar is):
1. tot 12 pogingen: richting uniform, horizontale afstand uniform in [`teleportMinBlocks`, `teleportMaxBlocks`]
   (gameplay-random van het level; geen worldgen, dus geen seed nodig);
2. per poging een Y van voeten + 2 omlaag tot voeten − 3: de eerste plek waar
   - het doel in `Ring5Band.inner` ligt, de chunk geladen is en binnen de world border;
   - voeten- en hoofdblok geen botsvorm en geen vloeistof hebben;
   - het blok eronder een stevige bovenkant heeft en het pad-type van vanilla `WALKABLE` is (geen magma, vuur, kampvuur,
     cactus, bessenstruik, poedersneeuw, lava ernaast);
   - **nooit richting void:** onder het doel én onder elk van de 8 buren ligt binnen 4 blokken onder de doelvloer een vast
     blok (geen scheurrand, geen afgrond > 3).
3. geen geldige plek → deze keer geen teleport (geen fout, geen melding); de timer loopt door.
4. teleport: `teleportTo` naar het blokmidden, rotatie behouden, `resetFallDistance`. **Geen geluid of deeltjes in 91**
   (A koppelt die in 98; backlog).

## 3. Client/server-impact
- Alles server-side: regel, modifier, drempels, teleports, schade. Geen client-classes in `condition/`; geen
  `Minecraft.getInstance()`. De band rekent `Ring5Band` op de server-layout.
- Sync via het bestaande `condition_sync`: de payload wordt één waarde langer (nieuwe enum-positie). **INT1 verhoogt
  `ModNetwork.PROTOCOL_VERSION`** (één keer voor golf 1, samen met A's `STANDSTILL`).
- HUD: de nieuwe balk komt vanzelf (kleur uit de enum, drempelstreepjes uit de gesyncte config). Darkness: vanilla.
- Teleports: een normale server-teleport (vanilla positie-sync); een tweede speler ziet de sprong als een gewone
  positiewijziging.
- Prestatie: per speler per seconde, alleen in ring 5/void: één `ringAt`, één band, één `NearbyBlocks`-scan (radius 4,
  palette-voorcheck), één lichtwaarde; het weer alleen bij een positieve rate; de teleport-zoektocht hooguit elke 20 s
  (≤ 12 × 6 posities + buren).

## 4. Tests (licht: B, D, unit)
- `VoidRuleTest`: contract-startwaarden; 0→100 binnen 360 s ± 10 %, buiten en bij een bron 180 s ± 10 %; de sterkste wint;
  bloklicht ≥ 12 laat zakken, ook bij een bron en buiten; daglicht telt niet; buiten de zone zakken; `VOID` = buitenband;
  zone hard op 0,5 (zone 4→5 aan de ring 4-kant valt).
- `VoidAmplifierModifierTest`: nacht 1,5; glitchstorm 2,0 × intensiteit (fade), omgekeerde regen 1,5; nacht + glitchstorm =
  2,0 (max, geen 3,0); geen effect op een dalende rate; geen weervraag buiten de zone of bij rate ≤ 0.
- `VoidTeleportTargetTest` (nep-level): nooit in een blok of vloeistof, altijd op een stevige, `WALKABLE` vloer, afstand
  1–5, nooit in de buitenband, nooit naast een val > 3 of boven de void, geen doel → geen teleport; interval 20 s; niet
  bij creatief/passagier/in de lucht.
- Drempels: binnen ≥ 60 → teleports zonder darkness; buiten ≥ 60 → darkness zonder teleports; ≥ 90 → darkness + schade
  (binnen ook teleports); `ThresholdLevelsTest` +1.
- `Ring5DamageTypeTagsTest` op de gegenereerde tags; `VoidConfigTest` (precies de sleutels van §1.6 en hun standaarden).
- `#void_sources` is tot de merge van B leeg: de unit tests geven de bron als predicaat mee; geen fout.
- D: `damage_type/void.json`, de damage-type-tags, lang en + nl (gecommit).
- Geen runServer-gameplaytest (geen speler in een headless server); wel een **S-smoke** (verse wereld `r5-c91`: log
  zonder ERROR en zonder client-class-fout, `/ca condition` via RCON beschikbaar).

**Af als:** B, D, unit tests en S-smoke groen; `/code-review` verwerkt; de koppelpunten voor INT1 (factoren uit
`[weather.ring5]`, `STANDSTILL`-regel, `PROTOCOL_VERSION`) staan in "Koppeling met A" hieronder.

## 5. Koppeling met A (voor INT1)
- `VoidAmplifierModifier.weatherFactor(type)` → `ServerConfig.RING_5_WEATHER.*VoidMultiplier` (A: standaard 2,0 / 1,5 /
  1,5, bereik 0–10); daarna de constanten en hun test-asserts weg.
- De modifier leest het weer alleen via `RingWeather.stateAt`; na de merge van A controleert INT1 dat `stateAt` voor ring 5
  dezelfde bandgrens gebruikt als `VoidRule` (`Ring5Band.outer`, `value ≥ 0`).

## 6. Testdatapack voor INT1 (optioneel)
Als `#void_sources` bij de S-smoke van INT1 nog leeg is (B niet gemerged): een datapack in `build/test/datapacks/`
(niet gecommit) dat `minecraft:obsidian` in de tag zet, om de bronregel met een speler te kunnen proberen (main/gebruiker).

## Voortgang
- Voortgang: 2026-10-21 20:46 worktree `P:/Projecten/Java/wt/r5-conditions-g1` (branch `ring5/conditions-g1` vanaf
  `757523bf`), `setup.sh C 4`; eerste build groen.
- Voortgang: 2026-10-21 21:12 code + tests 91, runData, build groen; S-smoke `r5-c91` schoon.
- Voortgang: 2026-10-21 21:16 `/code-review` medium op de worktree-diff (10 bevindingen, zie Beslissingen).
- Voortgang: 2026-10-21 21:35 reviewfixes; build (1181 tests) + runData groen, S-smoke opnieuw schoon; commit.

## CODEBASE-delta
Package `condition` (alles server-side; geen client-class):
- `Condition.VOID` (`void`, HUD `0xFF8A4FD8`), achteraan de enum (sync per positie).
- `VoidZone` (INNER/OUTER/OUTSIDE): zone = overworld, `ring()` ∈ {`RING_5`, `VOID`} (hard op 0,5); `VOID` = buitenband
  (P4); band = `Ring5Band.outer` (`value ≥ 0`). Eén plek voor regel, drempels en teleport-zone.
- `VoidRule` + `VoidExposure` (`inner`/`outer`/`source`/`light`/`outside`): regeltabel zoals `PoisonRule`; volgorde
  zone → bloklicht op ooghoogte (≥ `lightLevel` = zakken, wint van alles) → band → `NearbyBlocks`-scan
  `#void_sources` (`sourceRadius`). De sterkste opbouw wint, niets stapelt.
- `VoidAmplifierModifier` (stage `WEATHER`, via `Ring5Conditions`): alleen op een positieve void-rate in de zone,
  × max(nacht, weer); weer = `RingWeather.stateAt`, `1 + (factor − 1) × intensiteit`. Factoren tot INT1 als constanten
  `DEFAULT_*_VOID_FACTOR` in `weatherFactor(type)`.
- Drempels (`ConditionDefinitions` `VOID`): 30 `VoidGlitchVisuals` (no-op, client leest de meter), 60 `VoidSlip`,
  90 een gewone `DamageThreshold` (`Ring5DamageTypes.VOID`, 1,0 per 2 s, `LETHAL_FROM_NORMAL`); de darkness van 90 komt
  via `VoidSlip` (drempel 2 is altijd bereikt als 3 dat is).
- `VoidDarkness`: één beslissing voor darkness van drempel 2 (buitenband/rand) en 3 (overal), gelezen uit
  `appliedThresholds` (die `ConditionTicker` vóór alle effecten zet; commentaar daar); alleen `VoidSlip.apply/clear`
  roept hem aan (één verversing per seconde, geen remove+add in één tick → geen herstart van de vanilla-fade). Eigen
  instance = ambient, geen deeltjes, wel icoon (`isOurs`, zoals `PoisonWither`); over een vreemde darkness (warden) wordt
  niet ververst en die wordt nooit verwijderd.
- `VoidSlip` + `SlipSchedule` (tijd sinds de laatste teleport per speler in een `WeakHashMap`, server-ticks, niet
  opgeslagen; het interval wordt live gelezen) + `VoidTeleport`
  (`Stance`: overleven/avontuur, op de grond, niet rijdend/dragend/slapend/zwevend, niet in vloeistof of poedersneeuw;
  `teleportTo` + `resetFallDistance`) + `VoidTeleportTarget` (pure zoektocht achter `Terrain`: 12 richtingen,
  afstand tussen blokmiddens in [min, max], Y voeten+2 … voeten−3; vrij = geen botsvorm/vloeistof en pad-type
  `OPEN`; vloer stevige bovenkant en vanilla-`WALKABLE` (nagebouwd met een verse `PathTypeCache`, incl. NeoForge-
  buurhaken); geen val > 3 en geen vloeistof op weg omlaag onder doel en 8 buren). `LevelTerrain`: overworld, world border, 3 × 3 kolommen geladen
  (`getChunkNow`, laadt nooit), doel in ring 5 én `Ring5Band.inner`.
- `Ring5Conditions.register` (vanuit `ConditionTicker.register`): de modifier.
- `Ring5DamageTypes.VOID` (`corruptedadventure:void`, msgId `corruptedadventure.void`, exhaustion 0).
Config: `ServerConfig.VOID` = `[conditions.void]` (`VoidCondition`, 17 sleutels: `innerRise` 0,28, `outerRise` 0,55,
`sourceRise` 0,55, `sourceRadius` 4, `nightMultiplier` 1,5, `lightLevel` 12, `lightFall` 1,0, `outsideFall` 1,0,
`threshold1..3` 30/60/90, `teleportMinBlocks` 1, `teleportMaxBlocks` 5, `teleportIntervalSeconds` 20,
`darknessAmplifier` 0, `damage` 1,0, `damageInterval` 2); `teleportMax()` = max(min, max). In `ALL_THRESHOLDS`.
Datagen: `datagen/tags/Ring5DamageTypeTags` (`bypasses_armor`, `no_knockback`, `bypasses_cooldown`,
`neoforge:is_environment` — het plan noemde `c:is_environment`, maar `Tags.DamageTypes.IS_ENVIRONMENT` is in 1.21.1 `neoforge:`, zoals bij ring 4; aangeroepen in `ModDamageTypeTagsProvider`), `Ring5ConditionText` (doodsbericht en + nl).
Gegenereerd: `data/corruptedadventure/damage_type/void.json`, de vier damage-type-tags, lang en/nl.
Tests: `VoidRuleTest`, `VoidAmplifierModifierTest`, `VoidTeleportTargetTest` (nep-`BlockGetter`), `VoidThresholdsTest`,
`Ring5DamageTypeTagsTest`, `config/VoidConfigTest`, `ThresholdLevelsTest` +1.

## Koppeling met A — stand voor INT1
- `VoidAmplifierModifier.weatherFactor`: `GLITCH_STORM`/`REVERSE_RAIN` → `ServerConfig.RING_5_WEATHER.*VoidMultiplier`;
  `STANDSTILL` toevoegen (constante `DEFAULT_STANDSTILL_VOID_FACTOR` 1,5 bestaat al, nu ongebruikt); daarna de drie
  constanten en hun asserts in `VoidAmplifierModifierTest.theFactorsAreTheConfirmedOnes` weg.
- `ModNetwork.PROTOCOL_VERSION` `"8"` → `"9"` (niet in deze branch).
- Controle: `RingWeather.stateAt` voor ring 5 gebruikt dezelfde bandgrens als `VoidZone` (`Ring5Band.outer`, `value ≥ 0`).
- `ServerConfig` static-blok: C voegt één regel `VOID = new VoidCondition(builder);` na `PURIFYING` toe en
  `VOID.thresholds` in `ALL_THRESHOLDS`.

## Beslissingen
(implementatiedetails, door de stroom beslist)
- Drempel 2 **buiten de zone** (na het verlaten van ring 5 met meter ≥ 60): geen teleport en geen darkness; de meter zakt
  daar. Drempel 3 werkt overal (zoals het gif: drempels hangen aan de meter, niet aan de plek).
- Timer: start bij de eerste seconde ≥ 60 in de binnenband (eerste teleport na 20 s); bij verlaten van de binnenband of
  zakken onder 60 begint hij opnieuw. Is hij verlopen terwijl de speler niet op de grond staat, dan wacht de teleport op
  de grond (geen nieuwe 20 s); zonder veilige plek start de wachttijd opnieuw (§2.3).
- Afstand 1–5 gemeten tussen het voetblok van de speler en het doelblok (blokmiddens), zodat ook min = max werkt.
- Gelijke opbouw van band en bron (standaard buiten 0,55 = bron 0,55): het label blijft `outer`; een bron wint alleen
  als hij strikt sneller is.
- Licht ≥ 12 wordt vóór band en bronscan bepaald; bij licht worden band en scan overgeslagen.
- Teleport: geen NeoForge-teleport-event (geen passend type; chorus/ender-pearl dekken het niet); vanilla
  `ServerPlayer.teleportTo` binnen hetzelfde level.
- **Review (medium, 2026-10-21, op de worktree-diff):** opgelost — warden-darkness die na samenvoegen als de onze werd
  verwijderd (nu `isOurs` + niet verversen over een vreemde); vloeistof onder een buurkolom (lava één blok lager telde
  als veilige stap); dubbele darkness-verversing en dubbele schadecode bij 90 (nu `DamageThreshold`, darkness alleen via
  `VoidSlip`); live intervalwijziging werkte pas na de volgende teleport; volgorde-afhankelijkheid van
  `appliedThresholds` gedocumenteerd in `ConditionTicker`. Niet opgelost: `zoneOf` rekent de band nog los van de regel
  (nu één keer per seconde per speler ≥ 60, verwaarloosbaar); geen teleport-event (zie hierboven, backlog);
  `CODEBASE.md` niet bijgewerkt (protocol: alleen de delta hier, INT doet het). **Open vraag** (zie eindrapport):
  teleports door muren heen (in een afgesloten holte of een gebouw); nu toegestaan.

## Backlog
- `atmosphere`: geluid en deeltjes bij een void-teleport (A, 98: bv. `ambient.ring_5.glitch`).
- `conditions`/`infra`: een annuleerbaar teleport-event voor de void-teleport (claims-/protectiemods), als de mod ooit
  op servers met zulke mods draait.
