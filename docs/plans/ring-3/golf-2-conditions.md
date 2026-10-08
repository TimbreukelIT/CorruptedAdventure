# Golf 2 — stroom C (condities): 55 kou Nether-3, 43-afstemming hitte

Scope: `conditions` — één commit per pakket, branch `ring3/conditions-g2`, poort 4 (server 25574, RCON 25584). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:**
- **55:** Nether-3 krijgt zijn overlevingsconditie: kou. Die stijgt overal in Nether-3 en daalt bij warmte. Leren armor remt. De hoogste drempel is vanilla-bevriezing (`ticksFrozen`: frost-overlay en schade, door vanilla gesynct).
- **43-afstemming:** de hitte nalopen op het terrein van 39d (lavarivieren overal, archipels, afgronden) en klaarzetten voor 4b.

**Bron:**
- `fase-1-wereld.md` Nether-ring 3 "Conditie: kou/bevriezing", Ring 3 "Conditie: hitte" + uitwerking 43;
- `contract.md` §5 (`Condition.COLD`, `[conditions.cold]`, `warmth_sources`);
- `golf-1-conditions.md` (opzet `HeatRule`/`HeatExposure`, `HeatSources`-scan, `RateModifier.Stage`, `HeatBurn`, valkuilen);
- `CODEBASE.md` "Meter-systeem: haken";
- backlog r. 198/199/200/203.

## 1. Pakketten

### 55 — Kou in Nether-3
1. **`Condition.COLD`** (`cold`, HUD ijsblauw, bijv. `0xFF7FC8F0`). Pure regeltabel `ColdRule` (+ `ColdExposure`), naar het voorbeeld van `HeatRule.evaluate`.
   - **Zone:** Nether, `ringAt(...).ring() == RING_3` (hard op 0,5, zoals `HeatRule.inZone`); daarbuiten ↓.
   - **Op:**
     - overal in de zone (basis);
     - sneller op ijs (`#ice` onder de voeten: Nether-ijs, verpakt, oerijs, vanilla-ijs) en in de "open vlakte" (G2-C1).
   - **Af:** binnen een straal van een blok in `#corruptedadventure:warmth_sources` (lava, vuur, zielvuur, kampvuren, magma, gloeiende scheur, smeulsteen). Ook in lava of vuur zelf, uiteraard.
   - **Leren armor remt** (G2-C3): per stuk uit de vanilla-tag `#freeze_immune_wearables`.
   - Fire resistance doet niets tegen kou (zoals vanilla); noteren.
2. **Warmtebronnen zoeken:** `HeatSources` generaliseren naar een scan met een tag-parameter (dezelfde box-scan, palette-voorcheck, `getChunkNow`; laadt nooit chunks). Gedrag-neutraal voor de hitte; meten met 2 spelers.
3. **Drempels** volgens G2-C2. De hoogste drempel stuurt vanilla-bevriezing aan.
   - **Valkuil vanilla:** `LivingEntity.aiStep` laat `ticksFrozen` buiten poedersneeuw elke tick met 2 zakken, en vriesschade valt alleen als `canFreeze()` waar is. Draag je één leren stuk, dan is dat onwaar: geen vriesschade.
   - Dus: na `aiStep` (`PlayerTickEvent.Post`) `ticksFrozen` zelf zetten volgens de meter.
   - De schade bij 100 komt van een eigen regel (zoals `HeatBurn`, damage type `minecraft:freeze` of eigen `corruptedadventure:cold` voor het doodsbericht; implementatiekeuze), zodat leer remt maar niet immuun maakt.
   - De frost-overlay rendert de client uit de gesyncte `ticksFrozen` (geen eigen client-code).
4. **Config `[conditions.cold]`** (ServerConfig, eigen blok): `riseInZone`, `iceMultiplier`, `exposedMultiplier`, `warmthRadius`, `warmthFall`, `outsideFall`, `leatherFactorPerPiece`, `threshold1–3`, effect-amplifiers. Startwaarden uit G2-C1..C3.
5. **Lang** (`Ring3ConditionText`): meternaam en eventueel het doodsbericht (en + nl).
6. **Netwerk:** `condition_sync` wordt 1 byte langer (net als bij hitte), dus INT verhoogt `ModNetwork.PROTOCOL_VERSION` (één keer per golf). Geen nieuwe payload.

### 43-afstemming — hitte na 39d (klein; daarna de 4b-feedback)
1. **Meten:**
   - welk deel van het ring 3-oppervlak ligt binnen 4 blokken van lava (rivieren 84 km, 6–16 breed; kratermeren; afgrondzee)?
   - hoe vaak doet de bronscan daardoor een volle scan (palette-voorcheck)? µs per scan met 2 spelers.
   - Blijkt "bron ×1,5" bijna overal te gelden: melden met cijfers, niet zelf de regel wijzigen.
2. **Schaduw van eilanden, bogen en archipels:** een vast blok boven het hoofd telt als dak (−0,5); controleren dat dat ook onder eilanden op Y 150–210 werkt (heightmap `MOTION_BLOCKING_NO_LEAVES`).
3. **`HeatRule.inZone`** (backlog r. 203, JIT-deopt op een servertick-pad): vertakkingsarm maken (één ringlookup per speler per seconde, gedeeld met verdamping). Gedrag-neutraal.
4. **Zwaveldamp** (G2-C4): alleen bouwen als de gebruiker B of C kiest.
5. Daarna wacht 43 op de feedback van 4b (afstemmen in config, geen nieuwe regels zonder vraag).

## 2. Bestanden
- **Eigen:** `condition/*` (kou: `ColdRule`, `ColdExposure`, `ColdBurn`/vriesregel, gegeneraliseerde bronscan), `ServerConfig` blok `[conditions.cold]` (+ `[conditions.heat]`), `Ring3ConditionText`, `Ring3DamageTypes`/`Ring3DamageTypeTags` (als er een eigen damage type komt), `Ring3Conditions.register`.
- **Leest:** de tags `warmth_sources` (B), `#ice`, `#freeze_immune_wearables` (vanilla), eventueel een biome-tag voor de open vlakte (W-nether via main).

## 3. Afhankelijkheden
- **← B:** `warmth_sources` is compleet (51). Geen wachtrelatie.
- **← W-nether** (zacht): "open vlakte" als biome-tag (G2-C1 = A) komt van W-nether in 53b; de naam via main in het contract.
  - Tot dan test C met een testdatapack (patroon golf 1: een lege tag is geen fout).
- **→ A:** geen. Frost-overlay is vanilla; eventuele adem-wolkjes (G2-A5) zijn van A.
- **→ W-terrein 42:** of lavabuizen een schuilroute zijn, hangt af van G2-WT1 (lava in de buis = hittebron).

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-C1 — Kou: hoe snel, en waar sneller?**
- **A. 0 → 100 in ~4 min in de zone; ×1,5 op ijs en in de open vlakte (biome `frozen_lava_sea`, via een biome-tag); in de hallen en kloven basis (aanbevolen).** C laag/middel (één tag via het contract). S: de bevroren zee is de koude oversteek; de hallen geven adem. MP geen.
- **B. Sneller: ~2,5 min, ×1,5 op ijs.** C laag. S: Nether-3 wordt een sprint van warmtepunt naar warmtepunt; zwaar zonder fase 2-gear.
- **C. Trager: ~6 min, alleen ijs telt extra.** C laag. S: mild; Nether-3 is vooral een doorgang.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-C2 — Kou: welke drempels?** (design: traagheid → mining fatigue → vanilla-bevriezing)
- **A. 33 traagheid I · 66 mining fatigue I · 100 bevriezing (frost-overlay, ~1 schade per 2 s, eigen doodsbericht) (aanbevolen).** C laag. S: drie heldere stappen zoals het ontwerp. MP: `ticksFrozen` is vanilla-gesynct.
- **B. Vier stappen zoals hitte: 25 traagheid · 50 zwakte · 75 mining fatigue · 100 bevriezing.** C laag. S: symmetrisch met hitte, iets zwaarder.
- **C. Al vanaf 50 een lichte frost-overlay (alleen beeld), schade pas bij 100.** C middel (de overlay los van de schade sturen). S: je ziet de kou eerder (sfeervol, ook als waarschuwing).
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-C3 — Leren armor en warmte: hoe sterk?**
- **A. Per leren stuk −15 % opbouw (vol leer −60 %); warmte binnen 4 blokken laat de meter met 2/s zakken (zoals water bij hitte) (aanbevolen).** C laag. S: leer is een echte keuze (zwak tegen monsters); bij de gloeiende scheuren kom je snel op adem.
- **B. Vol leer halveert (−50 %, zoals fire resistance bij hitte); warmte −1/s binnen 4 blokken.** C laag. S: consistent met hitte; warmtepunten minder krachtig.
- **C. Elk leren stuk telt volledig (één stuk = −50 %), warmte binnen 6 blokken.** C laag. S: mild; één leren muts lost veel op.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-C4 — Giftige damp in `sulfur_fields`** (contractrol "zwavelkorst, fumarolen, giftige damp"; gif is het thema van ring 4)
- **A. Alleen sfeer: geelgroene stoom-particles (A), geen effect (aanbevolen).** C geen. S: dreigend zonder te vergiftigen; gif blijft het nieuwe gevaar van ring 4.
- **B. Korte misselijkheid (nausea, 5 s) als je > 5 s in de damp boven een fumarole staat.** C laag/middel (server-side check per fumarole in de bronscan). S: een extra "blijf in beweging"; misselijkheid is hinderlijk.
- **C. Vergiftiging I bij lang staan in de damp.** C laag/middel. S: gevaarlijker; loopt vooruit op ring 4.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
- **Alles server-side:** meter, regels en `ticksFrozen`. Sync via de bestaande `condition_sync` (1 byte langer → protocolversie via INT) en vanilla's entity-data voor `ticksFrozen`.
- HUD-balk (bestaande laag) en frost-overlay (vanilla) op de client; geen nieuwe client-classes.

## 6. Tests
- **B:**
  - unit tests `ColdRule` (zone, ijs, open vlakte, warmte, leer per stuk, buiten de zone), drempels, `ticksFrozen`-sturing (puur deel), gegeneraliseerde bronscan (hitte gedrag-neutraal: de bestaande `HeatSourcesTest` blijft groen);
  - `ClientIsolationTest`.
- **D:** `runData` (lang, eventueel damage type + tags).
- **S** (server 25574 + 2 clients, quick-play `Dev`/`Dev2`):
  - kou stijgt in Nether-3 (`/ca tpring 3 nether`), sneller op Nether-ijs;
  - zakt bij een gloeiende scheur en een kampvuur;
  - leer per stuk remt;
  - bij 100 frost-overlay + schade op beide clients (de ander ziet het bevroren-effect ook);
  - buiten Nether-3 ↓;
  - eigen doodsbericht.
  - Hitte: de metingen van 43-afstemming (aandeel binnen 4 blokken van lava, µs per scan met 2 spelers), dak onder een eiland.
- **Review:** "klaar voor review-doorsturen: <pakket>" aan main; main draait `/code-review medium`; daarna committen.

## 7. W-checklist (voor 4b; dedicated server + 2 clients)
- [ ] Nether-3 (`/ca tpring 3 nether`): HUD-balk kou ijsblauw; stijgt overal, sneller op ijs en in de open vlakte; zakt bij een gloeiende scheur, magma, kampvuur, lava.
- [ ] Leren armor per stuk merkbaar trager; volledig leer remt, maar maakt niet immuun.
- [ ] Bij 100: frost-overlay, schade, eigen doodsbericht; op beide clients zichtbaar.
- [ ] Portal terug naar ring 3: de kou zakt, de hitte begint (geen dubbele meter-rare dingen bij de wissel).
- [ ] Hitte-ritme over een dag/nacht in ring 3 op het 39d-terrein: rivieren, archipels (schaduw), afgronden, slapende krater, buizen (na 42).

## Voortgang
- Worktree: `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a4c1e43b7ad6474db`, branch `ring3/conditions-g2` (vanaf `dbb1cb9`).
- Voortgang: 14:30 start, plan en code gelezen; tagnaam `cold_exposed` via main vastgelegd (contract §2, a1648e7).
- Voortgang: 14:40 55 gebouwd (COLD, ColdRule, LeatherColdModifier, ColdFreeze, NearbyBlocks, config, lang, damage type).
- Voortgang: 14:43 55 B groen (incl. `ColdRuleTest` 11, `ClientIsolationTest`), D groen (damage type `cold`, 4 tags, lang en + nl).
- Voortgang: 15:02 55 S groen (dedicated server 25574 + Dev/Dev2 quick-play, wereld `c-g2-a`, testdatapack `cold-test` met `placeholder_nether_ring_3` in `cold_exposed`): in Nether-3 `+0,42/s zone`; op verpakt ijs `+0,63/s ice`; met het testdatapack `+0,63/s exposed` (gelijk: `ice` wint); gedoofd kampvuur op 2 blokken → `zone`, brandend → `−2,00/s warmth`; `glowing_fissure` op de hoek (4, −1, 4) → `warmth`, magma op 5 blokken → `zone`; leer 1 stuk `+0,36`, 4 stukken `+0,17`, 3 leer + ijzer `+0,23`; fire resistance verandert niets; 40 → traagheid, 70 → + mining fatigue (ambient); bij 100 `TicksFrozen` 140 voor beide spelers, schade precies 5 in 10 s voor beide (met en zonder leer; geen extra vanilla-vriesschade, `naturalRegeneration` uit); frost-overlay + bevroren hartjes op Dev (`build/c55/freeze_dev.png`), Dev2 ziet Dev (`freeze_dev2_sees_dev*.png`); doodsbericht "Dev froze in the cold of the Nether" (server-log en chat van de clients); na respawn kou 0 en geen `TicksFrozen`; Nether-3 → ring 3 met kou 80: `cold −0,50/s outside`, `heat +0,55/s sun`, drempels blijven tot ze zakken; 0 ERROR/Exception in de serverlog.
- Voortgang: 15:30 55 naar main voor review (nog niet gecommit). 43-afstemming: eerste meting (320 punten, 20 gebieden ring 3, seed 20260930): 8,8 % van het oppervlak binnen 4 blokken van een hittebron, 0,0 % van lava; palette-voorcheck slaagt bij 19,1 %; scan mediaan 0,8 µs, p90 32 µs, max 159 µs; met kandidaat mediaan 32 µs, p90 131 µs. Grotere meting (1280) gaf OutOfMemory op de server (de probe genereerde te veel chunks in één tick); opnieuw in kleinere porties. Dak onder een eiland nog niet met een speler getest (kandidaat −3645 135 −4085, top 160).
- Voortgang: 15:55 review 55 (main, 9 bevindingen) verwerkt: (1) protocolversie blijft voor INT; (2) geen bevriezing en geen koudeschade terwijl de speler brandt; (3) `leatherReductionPerPiece` max 0,2 (nooit immuun); (4) tick-churn van `ticksFrozen` vastgelegd, niet opgelost (zie `ColdFreeze`/Beslissingen); (5) `ColdFreeze.apply` bouwt geen `DamageThreshold` meer per aanroep (eigen schade vanwege de brandcheck); (6/7) hernoeming expliciet in de delta, backlogregel kampvuur staat; (8) `standsOn` kijkt in de lucht tot 2 blokken omlaag; (9) `&` in `HeatRule.inZone` volgt in 43.
- Voortgang: 15:45 reviewfixes getest: B groen (11 + alle tests); S (server + Dev): 1,2 blok boven het ijs nog `ice`, 2,5 blok erboven `zone`; brandend (fire resistance, eeuwig vuur, `warmthFall` tijdelijk 0) bij kou 100 in 10 s geen schade en `TicksFrozen` 0; vuur uit → weer bevroren en schade; 0 ERROR in de serverlog.

## CODEBASE-delta

**55 (kou Nether-3):**
- `condition/Condition.java`: nieuwe waarde `COLD` (`cold`, ijsblauw `0xFF7FC8F0`, 55). `ConditionSyncPayload` wordt daardoor 1 byte langer (4 condities) → **INT: `ModNetwork.PROTOCOL_VERSION` 5 → 6** (niet door C opgehoogd).
- `condition/ColdRule.java` ✅ (55) regel van `COLD`: zone = Nether, `ringAt(...).ring() == RING_3` (hard op 0,5; `inZone` met `&`, zonder kortsluiting); in de zone stijgt de meter overal met `riseInZone` (0,42/s: 0 → 100 in ~4 min), × `iceMultiplier` (1,5) op `#minecraft:ice` (het blok onder de voeten, vanilla's ondersteunende blok; ook Nether-ijs, verpakt en oerijs via de tag van B) of × `exposedMultiplier` (1,5) in `#corruptedadventure:cold_exposed`; de sterkste factor wint, ze stapelen niet (gelijk: ijs vóór open vlakte). Binnen `warmthRadius` (4) van een brandende warmtebron (`#warmth_sources`, ook in lava of vuur zelf) daalt hij met `warmthFall` (2/s), ook op ijs; buiten de zone met `outsideFall` (0,5/s, zonder bronscan). Pure delen `input`/`evaluate`, `Rates`, `Input`, `Result`; `isRisingCold`. Fire resistance doet niets.
- `condition/ColdExposure.java` ✅ (55) label in `/ca condition get`: `zone`, `ice`, `exposed` (stijgen), `warmth`, `outside` (dalen).
- `condition/LeatherColdModifier.java` ✅ (55) `RateModifier` (`Stage.GEAR`): stijgende kou × `max(0, 1 − n × leatherReductionPerPiece)`, n = gedragen stukken (hoofd, borst, benen, voeten) in vanilla's `#freeze_immune_wearables` (vol leer −60 %).
- `condition/ColdFreeze.java` ✅ (55) derde koudrempel (`FREEZE`): `PlayerTickEvent.Post` (na vanilla's `aiStep`) zet `ticksFrozen` op volledig bevroren (140; nooit lager dan vanilla's eigen waarde) zolang de drempel bereikt is en de speler niet brandt → vanilla's frost-overlay, bevroren hartjes, rillen (gesyncte entity-data, ook zichtbaar voor anderen) en de poedersneeuw-traagheid. Schade in `apply` (`Ring3DamageTypes.COLD`, `damage` 1,0 per `damageInterval` 2 s via `DamageThreshold.isDue`, geen vloer), ook met leer (vanilla's `canFreeze()` is met één leren stuk onwaar), niet terwijl de speler brandt (daarom geen `DamageThreshold`). Vanilla's eigen vriesschade valt niet extra (`aiStep` dooit eerst 2 ticks), behalve in echte poedersneeuw (vanilla). Bekende kost: de entity-data van een bevroren speler gaat elke tick mee (1 int; vanilla doet hetzelfde met de luchtvoorraad onder water); vermijden kan alleen met een mixin in `aiStep`.
- **Hernoemd:** `condition/HeatSources.java` → `condition/NearbyBlocks.java` en `HeatSourcesTest` → `NearbyBlocksTest` (55): dezelfde box-scan (dichtstbij eerst, palette-voorcheck, `getChunkNow`) met een `Predicate<BlockState>`; gebruikt door hitte (`#heat_sources`, gedrag ongewijzigd) en kou (`#warmth_sources`, alleen brandend: een gedoofd kampvuur telt niet). Test: `NearbyBlocksTest` (was `HeatSourcesTest`, ongewijzigd op de naam na).
- `condition/ConditionContext.java`: + `isNearWarmth(radius)`, `standsOn(TagKey<Block>)` (op de grond `player.getBlockStateOn()`, in de lucht het eerste niet-luchtblok tot `AIRBORNE_LOOKDOWN` = 2 blokken onder de voeten: een sprong verlaat het ijs niet), `isWarmthSource(state)`; `isNearHeatSource` via `NearbyBlocks`.
- `condition/ConditionDefinitions.java`: `case COLD` (regel, drempels 33/66/100, effecten traagheid · mining fatigue · `ColdFreeze.FREEZE`).
- `condition/Ring3Conditions.java`: registreert ook `LeatherColdModifier` (`GEAR`) en `ColdFreeze`.
- `condition/Ring3DamageTypes.java`: + `COLD` (`corruptedadventure:cold`, als vanilla `freeze`: exhaustion 0, effect `freezing`). `datagen/tags/Ring3DamageTypeTags`: `cold` in `is_freezing` (vriesgeluid; gamerule `freezeDamage` zet hem uit), `bypasses_armor`, `no_knockback`, `neoforge:is_environment`.
- `datagen/lang/Ring3ConditionText`: doodsbericht `death.attack.corruptedadventure.cold` (+ `.player`): "%1$s froze in the cold of the Nether" / "%1$s bevroor in de kou van de Nether".
- `config/ServerConfig.java`: `[conditions.cold]` (`COLD`, `ColdCondition`): `riseInZone` 0,42, `iceMultiplier` 1,5, `exposedMultiplier` 1,5, `warmthRadius` 4 (1–8), `warmthFall` 2,0, `outsideFall` 0,5, `leatherReductionPerPiece` 0,15 (0–0,2: vol leer hoogstens −80 %, nooit immuun), `threshold1–3` 33/66/100, `effect1Amplifier`/`effect2Amplifier` 0, `damage` 1,0, `damageInterval` 2 (1–60); in `ALL_THRESHOLDS`.
- `registry/ModTags.java` (`Biomes`, blok `// ring 3 — conditions g2`): `COLD_EXPOSED` = `corruptedadventure:cold_exposed` (inhoud `frozen_lava_sea` door W-nether in 53b; tot dan leeg, geen fout).
- Gegenereerd: `data/corruptedadventure/damage_type/cold.json`, `data/minecraft/tags/damage_type/is_freezing.json` (+ `cold` in `bypasses_armor`, `no_knockback`, `neoforge:is_environment`), lang en + nl.
- Tests: `condition/ColdRuleTest.java` (zone + grens 0,49/0,51 aan beide kanten, ~4 min, ijs/open vlakte ×1,5, sterkste factor zonder stapelen, warmte overal in de zone, buiten de zone, +0,0, alle 16 invoercombinaties, `isRisingCold`, leer per stuk en nooit < 0, `frozenTicks`); `ThresholdLevelsTest.coldHasThreeRisingThresholds`.

## Beslissingen

- 2026-10-08 · conditions · 55: kou in Nether-3 = G2-C1..C4 A (gebruiker 2026-10-08): 0 → 100 in ~4 min, ×1,5 op ijs en in de biome-tag `cold_exposed` (open vlakte `frozen_lava_sea`), drempels 33 traagheid I · 66 mining fatigue I · 100 bevriezing, −15 % per leren stuk, warmte binnen 4 blokken −2/s, zwaveldamp alleen sfeer.
- 2026-10-08 · conditions · 55: tagnaam `corruptedadventure:cold_exposed` (via main in contract §2, a1648e7); beschrijft de rol, niet de biome.
- 2026-10-08 · conditions · 55 (implementatie): bevriezing = vanilla's `ticksFrozen` elke tick na `aiStep` op 140 zetten (overlay, bevroren hartjes, rillen en poedersneeuw-traagheid van vanilla); de schade komt van een eigen drempel (1 per 2 s, damage type `cold` met eigen doodsbericht) zodat leer remt maar niet immuun maakt. Gevolg om in 4b te beoordelen: vanilla's bevriezing halveert ook de loopsnelheid (bovenop traagheid I).
- 2026-10-08 · conditions · 55 (implementatie): ijs = het blok onder de voeten in `#minecraft:ice`; factoren stapelen niet (sterkste wint, zoals hitte); warmte telt ook op ijs en in de open vlakte; buiten de zone geen warmtescan (alleen −0,5/s).
- 2026-10-08 · conditions · 55 (implementatie): een gedoofd kampvuur is geen warmtebron (blok-tag + `lit`); bij de hitte ongewijzigd (gedrag-neutraal).
- 2026-10-08 · conditions · 55 (review): de entity-data van een bevroren speler gaat elke tick mee (`aiStep` dooit 2, `ColdFreeze` zet 140 terug); geaccepteerd, want vanilla doet hetzelfde met de luchtvoorraad onder water. Vermijden vraagt een mixin in `LivingEntity.aiStep` en het onderdrukken van vanilla's vriesschade bij 140; niet gedaan.
- 2026-10-08 · conditions · 55 (review): `leatherReductionPerPiece` hoogstens 0,2, zodat vol leer nooit immuun maakt; een brandende speler bevriest niet en krijgt geen koudeschade.
- 2026-10-08 · conditions · 55 (implementatie): damage type `cold` heeft de tags van vanilla's `freeze` (`is_freezing`: vriesgeluid, gamerule `freezeDamage` geldt; door armor; geen knockback; environment). Branden ontdooit (vanilla) en houdt de bevriezing tegen.

## Backlog

- [conditions] Hitte: een gedoofd kampvuur telt nog als hittebron (`#heat_sources` zonder `lit`-check); bij de kou wel gecheckt. Gelijktrekken als de gebruiker dat wil. (bron: 55)
