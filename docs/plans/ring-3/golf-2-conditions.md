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

## CODEBASE-delta

## Beslissingen

## Backlog
