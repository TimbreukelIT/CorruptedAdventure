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

## CODEBASE-delta

## Beslissingen

## Backlog
- `atmosphere`: geluid en deeltjes bij een void-teleport (A, 98: bv. `ambient.ring_5.glitch`).
