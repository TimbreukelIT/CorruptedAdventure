# Golf 1 — stroom C (condities): 43 hitte, 44 water verdampt

Scope: `conditions` — één commit per pakket; branch `ring3/conditions-g1`.

**Doel:** de overlevingskern van ring 3 (antwoorden 4 en 9):
- **43** een hitte-meter met een sterk dag/nacht-ritme, hittebronnen, pieken en schuilplekken;
- **44** geplaatst water verdampt bovengronds in ring 3.

Bron: `fase-1-wereld.md` Ring 3 "Conditie: hitte", "Water" + "Uitwerking sparring ring 3"; `contract.md` §5; haken in `CODEBASE.md` ("Meter-systeem: haken"); sjabloon `DehydrationRule`, `DehydrationExposure`, `WaterBottleDrink`, `HeatWaveDehydrationModifier`.

## 43 — Hitte
- `Condition.HEAT`: pure regeltabel naar het voorbeeld van `DehydrationRule.evaluate`.
  - **Op:** zon overdag onder open lucht +0,55/s; ×1,5 binnen `sourceRadius` (4) van een blok in `#heat_sources`; ×2 in `#heat_peaks`; de sterkste factor wint, ze stapelen niet (*voorstel; anders melden*).
  - **Af:** 's nachts −0,5; ondergronds/onder een dak −0,5; schaduw overdag −0,25 (dus nooit echt koel overdag); water −2,0; `#heat_refuges` −2,0; buiten ring 3 −0,5.
  - Een hittebron telt ook 's nachts en ondergronds (lava in een grot is heet).
- **Fire resistance:** factor 0,5 op een positieve rate (`RateModifier`).
- **Ringweer-modifiers** (`contract.md` §5): asregen ×1,5 en uitbarsting ×2 als `1 + (m − 1) × intensiteit` op een positieve rate, hard op de ringgrens (sjabloon `HeatWaveDehydrationModifier`). Factoren uit `[weather.ring3]` (A); tot A gemerged is, constanten met dezelfde startwaarden. De aswolk (×3) volgt in golf 2.
- **Volgorde van de modifiers** (backlog r. 95): weer → gear (fase 2) → fire resistance → zuiverend (Nether-4) als laatste. Vastleggen in `ConditionDefinitions` en in de delta.
- **Drempels:** 25 traagheid I, 50 zwakte I, 75 mining fatigue I, 100 in brand (eigen `ThresholdEffect` ignite: `setRemainingFireTicks`, elke seconde ververst, niet in water). Damage type `corruptedadventure:heat` voor het doodsbericht als dat van toepassing is (implementatiedetail).
- **Bronnen zoeken:** een box-scan van 4 blokken elke 20 ticks per speler is ~729 blokken. Goedkoop houden (vroeg stoppen, eerst de blokken direct rond de speler). Meten met 2 spelers.
- **Config** `[conditions.heat]` (`contract.md` §5); HUD-kleur in de enum; lang via `Ring3ConditionText`.
- Een lege `heat_sources`-tag in de worktree is normaal: test met een testdatapack of `/ca condition`.

## 44 — Water verdampt
- In ring 3 (`ringAt(...).ring() == RING_3`, overworld) verdampt geplaatst water direct waar de hemel zichtbaar is (`canSeeSky` op de plaatsing), met vanilla's verdampingseffect (rook + sis, zoals in de Nether).
- **Routes:** emmer (speler), dispenser, gebroken ijs dat water wordt, smeltend ijs (random tick), en een waterfles op modder (vanilla-gedrag) indien relevant.
- Ondergronds en onder een dak werkt water normaal. Buiten ring 3 is er niets anders.
- **Implementatie:** NeoForge-events of een mixin per route (de agent kiest). Elke route krijgt een test; noteer de routes die niet af te vangen zijn.
- Natuurlijke vloeistoffen uit worldgen (W) vallen hier niet onder.

## Client/server-impact
Alles server-side; meter-sync via de bestaande `condition_sync` (geen nieuwe payload). HUD client-only (bestaande laag).

## Tests
- **B**: unit tests regeltabel (dag/nacht/dak/water/bron/piek/refuge), drempels, fire resistance, modifier-volgorde.
- **S** (RCON, 2 clients):
  - meter loopt op in de zon en zakt 's nachts (`/time set`);
  - lava binnen 4 blokken;
  - water;
  - fire resistance halveert;
  - ignite bij 100;
  - verdamping per route bovengronds en geen verdamping onder een dak of ondergronds;
  - buiten ring 3 normaal.
- **W-checklist** voor 4b.

## Bij afronding
`waves.md` §4.

## Voortgang
- Voortgang: 20:45 start: branch `ring3/conditions-g1` (op 8e0c637), docs gelezen; 43 in bouw.
- Voortgang: 21:41 43 gebouwd, B+D groen, S deels (zon/nacht/bron/dak/schaduw/piek/refuge/water/fire res/weer/drempels/brand/doodsbericht OK); fix: bronscan laadt geen chunks meer; 44 gebouwd, test volgt.
- Voortgang: 22:06 S 44 gedaan; bronscan met palette-voorcheck; tijdelijke meetcode verwijderd; laatste `build` groen (incl. unit tests, ClientIsolationTest). Afronden op verzoek van main.
- Voortgang: 08:14 (2026-10-07) hervat; gebruikersbesluit generatoren vastgelegd; 44-sanity op de definitieve build (open lucht verdampt, lava blijft lava); dak + water vlak na elkaar bleef één keer niet staan (lichtengine liep achter) → verdamping vraagt nu ook de heightmap.
- Voortgang: 09:00 review 43 binnen, bevindingen verwerkt (zie "Review 43").
- Voortgang: 10:20 build + `runData` groen met de fixes; laatste S: bron op 4 blokken wel/op 5 niet (palette-voorcheck), lava boven het hoofd telt; branden bij 100: vlam blijft op 9 vuurticks, ~1 schade/s, dood → "Dev succumbed to the heat", onder 100 dooft de vlam; 44: vers dak houdt water (heightmap), open lucht verdampt, slab droogt, bladeren houden water. Stroom uit een overdekte bron die de open lucht in loopt, blijft staan (alleen bronnen verdampen). Commit 43 (7f1aca5).
- Voortgang: 10:35 review 44 binnen, fixes verwerkt (zie "Review 44"); build + `runData` groen; S 44: gat tussen twee overdekte bronnen blijft stromend water (geen bronvorming, geen sis-lus), open lucht verdampt, slab droogt, dispenser houdt een lege emmer. Commit 44.

## Status bij onderbreking (2026-10-06 22:10) — afgehandeld op 2026-10-07
- **Gecommit:** niets (geen pakket gereviewd). Branch `ring3/conditions-g1` staat op 8e0c637.
- **Ongecommit op schijf, klaar voor review:** 43 (hitte) en 44 (water verdampt) samen in de werkmap; beide gebouwd en getest (zie hieronder). `runData`-output (lang, damage type `heat`, damage-type-tags) staat ongecommit in `src/generated/`.
- **Ontbreekt nog:**
  1. Laatste S-ronde met de definitieve build: de palette-voorcheck in `HeatSources` is alleen gecompileerd en unit-getest, nog niet in-game (bron binnen/buiten 4 blokken opnieuw checken, plus 44-sanity). Wereld `c-heat-4` staat klaar met testdatapacks; RCON staat uit in `run/server.properties` (weer aanzetten: `enable-rcon=true`, wachtwoord `ca-c-test`, poort 25584). Hulpscripts (niet gecommit): `build/c-slot.sh`, `build/c-client.sh`, `build/c-rcon.py`.
  2. `runData` opnieuw na de laatste wijzigingen (alleen Java-logica gewijzigd sinds de vorige runData; geen diff verwacht).
  3. Review 43 (`/code-review` met args `medium conditions-g1 43`), oplossen, commit 43 met alleen de 43-bestanden; `Ring3Conditions` dan tijdelijk zonder de regel `WaterEvaporation.register` (die hoort bij 44).
  4. Review 44, commit 44.
  5. Delta-secties hieronder nalopen; eindrapport aan main.
- **Wachtende review:** geen gestart.
- Processen: alle eigen servers/clients gestopt; geen eigen JVM-slots of locks meer.

## Testresultaten tot nu toe
- **B:** `./gradlew build` groen; nieuwe tests `HeatRuleTest` (15), `HeatModifiersTest` (8), `HeatSourcesTest` (5).
- **D:** `runData` groen (vóór de laatste Java-only-wijzigingen).
- **S 43** (dedicated server poort 25574, client Dev quick-play; ook 2 clients Dev + Dev2 tegelijk). Testdatapacks: `heat_sources` = lava/magma/vuur/kampvuur, `heat_peaks`/`heat_refuges` = `placeholder_ring_3` (aan/uit per test).
  - zon +0,55/s (`sun`), nacht −0,50 (`night`); magma op 4 blokken +0,83 (`source`) dag en nacht, op 5 blokken niet; lava 4 onder de voeten (ondergronds) +0,83;
  - steen boven het hoofd −0,50 (`roof`), bladeren −0,25 (`shade`), glas = zon (+0,55);
  - piek +1,10 (`peak`), refuge −2,00, water −2,00, buiten ring 3 (ring 2) −0,50 (`outside`);
  - fire resistance ×0,5 (+0,28; met asregen +0,41); uitbarsting ×2 (+1,10) en asregen ×1,5 (+0,83) na de fade (met `doWeatherCycle false` blijft de intensiteit 0);
  - drempels 25 traagheid, 50 zwakte, 75 mining fatigue (ambient), 100 in brand (vuur ververst); dood → "Dev succumbed to the heat";
  - 2 spelers: elk eigen tempo (Dev zon+FR +0,28, Dev2 bron +0,83).
  - Meting bronscan (versie zonder palette-voorcheck, volle 810-blokscan zonder bron): warm ~36 µs per scan (1×/s per speler), koud ~200 µs. Een eerste versie laadde chunks synchroon na een teleport (piek 2,4 s) → opgelost met `getChunkNow`.
- **S 44** (forceload, zonder speler tenzij vermeld): `/setblock` water onder open lucht verdampt; onder een dak blijft het; dispenser met waterbucket omhoog verdampt en houdt een lege emmer; dispenser in een treeslab → `waterlogged=false`; water naast lava → lava blijft lava (geen obsidiaan); ijs naast een zeelantaarn smelt (speler in de buurt, randomTickSpeed 3000) en verdampt, onder een dak blijft het water; ring 2 → water blijft.
  - Niet via RCON te testen (W): emmer door een speler, ijs breken door een speler, rook/sis zien en horen.

## W-checklist (voor 4b)
- Hitte-ritme over een dag/nacht; HUD-balk oranjerood met streepjes op 25/50/75/100.
- Emmer water bovengronds in ring 3 → rook + sis, emmer leeg; onder een dak/ondergronds normaal; kort flitsen van water bij de speler zelf (client-voorspelling) acceptabel?
- IJs breken in ring 3 onder open lucht → geen water; onder een dak wel.
- In brand bij 100, niet in water; eigen doodsbericht.

## CODEBASE-delta
- `condition.Condition.HEAT` (`heat`, HUD `0xFFE8501E`). `ConditionSyncPayload` wordt 1 byte langer: netwerkformaat verandert, INT verhoogt `ModNetwork.PROTOCOL_VERSION` (geen nieuwe payload).
- `condition.HeatRule` (+ `HeatExposure`: sun/source/peak/water/refuge/night/roof/shade/outside), pure regeltabel; `ConditionContext.isUnderRoof()` (heightmap `MOTION_BLOCKING_NO_LEAVES`) en `isNearHeatSource(radius)`.
- `condition.HeatSources`: box-scan (voeten+hoofd ± radius, dichtstbij eerst, stopt bij de eerste hit), palette-voorcheck per sectie, laadt nooit chunks.
- `condition.HeatBurn`: drempel 4: elke seconde 1,0 schade als `corruptedadventure:heat` (eigen doodsbericht) en elke tick (`PlayerTickEvent.Pre`) een korte vlam (1–10 vuurticks: zichtbaar in brand, maar vanilla's eigen vuurschade valt nooit, want die komt alleen op een veelvoud van 20); niet in water, regen, bubbelzuil of poedersneeuw. Echt vuur (lava, vuurblok, Fire Aspect) brandt en telt op de vanilla-manier.
- `condition.Ring3DamageTypes.HEAT` (key + bootstrap); tags in `datagen.tags.Ring3DamageTypeTags` (`is_fire`, `bypasses_armor`, `no_knockback`, `c:is_environment`); lang `death.attack.corruptedadventure.heat(.player)` via `Ring3ConditionText` (en + nl).
- `RateModifier.Stage` (WEATHER → GEAR → FIRE_RESISTANCE → PURIFYING) + `RateModifierPipeline`; `ConditionDefinitions.addModifier(Stage, …)`; de oude `addModifier(modifier)` = WEATHER.
- `condition.HeatWeatherModifier` (asregen ×1,5, uitbarsting ×2, met fade). **Constanten tot A gemerged is**; INT: `multiplier(type)` → `(float) ServerConfig.RING_3_WEATHER.heatMultiplier(type)` (dan doet ASH_CLOUD ×3 automatisch mee; dat stond voor golf 2). `condition.FireResistanceHeatModifier`.
- `condition.WaterEvaporation` (44): `BlockEvent.NeighborNotifyEvent` (+ `CreateFluidSourceEvent`: een bron telt niet mee voor een nieuwe bron naast een verdampende cel, anders droogt en herstelt een gat tussen twee overdekte bronnen zich eindeloos); droogt een nieuwe waterbron of een nieuw waterlogged blok in ring 3 onder open lucht (geen vast blok/vloeistof erboven volgens de heightmap `MOTION_BLOCKING_NO_LEAVES` én `canSeeSky(pos.above())`) met vanilla-rook en -sis. Bewust synchroon in de notificatie (anders wordt lava eerst obsidiaan).
- `condition.Ring3Conditions.register` (modifiers, burn, verdamping), aangeroepen uit `ConditionTicker.register` zodat `CorruptedAdventure` niet geraakt wordt (INT mag het naar de entrypoint verplaatsen).
- `ServerConfig.HEAT` `[conditions.heat]`: `sunRisePerSecond`, `sourceMultiplier`, `sourceRadius` (1–8), `peakMultiplier`, `nightFall`, `shadeFall`, `undergroundFall`, `waterFall`, `refugeFall`, `outsideFall` (0,5; contract "daarbuiten ↓ 0,5"), `fireResistanceFactor`, `threshold1–4`, `effect1–3Amplifier`.

## Beslissingen (implementatie, voor de beslissingenlog)
- Hitte: blootstelling = directe zon of een bron binnen 4 blokken (ook 's nachts en ondergronds); stijging = 0,55 × sterkste factor (bron 1,5, piek 2; stapelen niet); een piek alleen verwarmt nooit zonder zon of bron. Water en refuge koelen altijd (ook in de zon of naast een bron).
- "Dak" = een vast blok boven het hoofd (heightmap zonder bladeren, dus ook overhang, eiland, ondergronds) −0,5; "schaduw" = geen volle hemel, maar geen vast blok erboven (bladeren) −0,25.
- Modifiervolgorde: weer → gear → fire resistance → zuiverend (`RateModifier.Stage`).
- In brand bij 100 = eigen hitteschade (1/s, `corruptedadventure:heat`, vuurtype) + een vlam zonder vanilla-vuurschade; vuur van anderen (Fire Aspect, lava) blijft vanilla en wordt aan hen toegeschreven.
- Water verdampt via één hook (NeighborNotify) voor alle routes. Waterfles op aarde maakt modder en plaatst geen water: niet relevant.
- **Gebruikersbesluit (2026-10-07):** water verdampt onder open lucht in ring 3 ook als dat obsidiaan- en cobble-generatoren buiten onmogelijk maakt (zoals in de Nether); ondergronds of onder een dak werken ze wel.

## Review 43 (2026-10-07, `/code-review medium`)
- Protocolversie niet verhoogd terwijl `condition_sync` 1 byte langer wordt → **niet opgelost, bewust**: afspraak golf 1 is dat INT `ModNetwork.PROTOCOL_VERSION` één keer per golf verhoogt (staat in de CODEBASE-delta).
- `HeatBurn` annuleerde `LivingIncomingDamageEvent` (NeoForge laat dan een `DamageContainer` op de stack staan) en hurt opnieuw binnen het event; ook Fire Aspect-vuur van een andere speler werd "hitte" → **opgelost**: geen schade-ruil meer; eigen hitteschade per seconde + vlam onder de vanilla-schadegrens.
- Alleen `isInWater()` doofde → **opgelost**: `isInWaterRainOrBubble()` of poedersneeuw.
- Verdamping binnen `NeighborNotifyEvent` met verouderde staat in de buitenste update → **bewust zo gelaten** (gebruikersbesluit: geen obsidiaan onder open lucht; een latere tick zou lava eerst laten stollen); de buren behandelen water hier als lucht en de client krijgt alleen de eindstaat. Uitgelegd in de javadoc.
- `isUnderRoof` telt ook vloeistof boven het hoofd → **javadoc aangepast** (water op glas telt als dak; randgeval, geen gedragswijziging).
- `CODEBASE.md`/beslissingenlog niet bijgewerkt → **bewust**: `waves.md` §4.8, de stroom schrijft delta's, INT voegt ze samen.
- 43-diff bevat 44 en de `heat_sources`-tag is leeg → 44 komt in een eigen commit (registratie pas in de 44-commit); de lege tag is contract (B vult hem), test met testdatapack.
- Dubbele guard in de modifiers → **opgelost**: één `HeatRule.isRisingHeat`.
- Lock in `HeatSources.offsets` en geen kortsluiting in de palette-check → **opgelost** (`AtomicReferenceArray`, kortsluiting).

## Review 44 (2026-10-07, `/code-review medium`)
- Eindeloze lus: een open gat tussen twee overdekte bronnen werd steeds opnieuw een bron, verdampte, enz. → **opgelost** met `CreateFluidSourceEvent` (S: het gat blijft stromend water).
- `NeighborNotifyEvent` vuurt ook als een buur (redstone, observer) de positie van bestaand water bijwerkt; later blootgesteld water verdampt dan alsnog → **bewust zo gelaten en gedocumenteerd**: water onder open lucht in ring 3 verdampt zodra het "aangeraakt" wordt; natuurlijk water uit worldgen vormt geen nieuwe bronnen meer naast open lucht (zelfde fix als de lus), en stromend water verdampt nooit.
- Verouderde staat in de buitenste update → **bewust** (zie review 43; gebruikersbesluit geen obsidiaan).
- Dure ringmeting vóór de goedkope checks → **opgelost** (heightmap en hemellicht eerst).
- Zone dubbel gedefinieerd → **opgelost**: `HeatRule.inZone`.
- Geen unit test voor `dried`/`isWaterSource` → **niet gedaan**: blockstates vragen een Minecraft-bootstrap die de unit tests van dit project niet hebben; de routes zijn in S getest (setblock, dispenser, slab, ijs smelten, lava, dak, bladeren, ring 2, lus).
- Lichtvertraging na het weghakken van een dak (water blijft dan) → **bewust**: enkele ticks, staat bij de bekende grenzen in de javadoc.
- `CODEBASE.md`/backlog niet bijgewerkt → **bewust**: `waves.md` §4.8 (delta's in dit plan, INT voegt samen).

## Backlog
- `conditions`: ijs smelt in ring 3 alleen door bloklicht (vanilla), niet door zon/hitte — nog te beslissen.
- `conditions`: een natte spons droogt in de Nether direct, in ring 3 niet (niet gebouwd).
- `infra`: `Ring3Conditions.register` hangt aan `ConditionTicker.register`; bij integratie eventueel naar `CorruptedAdventure`.
