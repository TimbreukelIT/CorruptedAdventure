# Ring 4 — grens 3→4 `verdant_falls` + heuvelfix (plan, vóór de volgende teststop)

Scope per stroom: `infra`, `worldgen` (terrein), `worldgen` (biome), `atmosphere`, en afhankelijk van de open vragen `conditions` en `blocks` (één categorie per commit, §5). Basis `main` 1521bfa. Bron van waarheid: `fase-1-wereld.md` → Ring 4 → "Grens 3→4: `verdant_falls`" en "Ring 4-heuvels (sparring 2026-10-15)"; contract `contract.md`; werkwijze `waves.md` (worktrees, licht per pakket, zwaar één keer bij integratie). Testseed `20260930`, preset `corrupted_world`, altijd een verse wereld.

Besluiten van de gebruiker (vast): zone 3→4 256 breed (`transitionWidthOverrides` [0, 0, 256, 256, 0], Nether 64); `steam_fens` als smalle bovenrand (~60) aan de ring 3-kant, daarna het ring 4-overgangsbiome `verdant_falls` op de afdaling; geen rechte klifwand meer; per stuk langs de ring wisselend getrapte bassins (Plitvice) en hoefijzerkloven (Iguazu) om de ~700 blokken; overal afgeronde randen; beken in geulen met vastgelegde vallen (waterscan-regel); richels, rotsbruggen en paden langs en achter de vallen, grotten achter vallen, lianen en wortels om te klimmen (biome-features); `verdant_falls` kaal tot de eigen flora er is; `ring3Seal` en de dammen onder Y 63/−54 blijven; heuvels ring 4: karakter houden, randen zachter, traplijnen golvend, treden egaler. Alles in één ronde.

---

## 1. Diagnose heuvels ring 4

### 1.1 Hoe het terrein nu per blok ontstaat
- `ring_4/overworld/surface` is een `flat_cache` van `island_output surface`: de 2D-hoogte wordt alleen op de **hoekpunten van het 4×4-raster** (quart-hoeken) gelezen. De stapel in `ring_4/overworld.json` zit in `interpolated` met cellen van 4 × 8 × 4 (`size_horizontal` 1, `size_vertical` 2). De dichtheid is `0,125 · (surface − y)` (lineair in y, dus verticaal exact); horizontaal is het oppervlak dus de **bilineaire interpolatie** van de vier hoekwaarden.
- Bilineair reproduceert vlakke hellingen exact. Fouten ontstaan alleen waar de hoogtefunctie binnen één cel van 4 blokken **knikt of sterk kromt**: `min`/`max`-knikken, het begin en einde van een terrastrede, smalle vormen.
- Per blok exact (buiten de interpolatie, via `thin` in de `range_choice`) zijn nu alleen: eilanden, bronnen, zinkgatkommen en de klifwand 3→4. Belangrijk voor de prestatie: `thin` roept per kolom `ensureIslands` → `ensureSurface` aan (`IslandField` r. 912), dus het **exacte reliëf wordt al voor elke kolom berekend**. Een exacte oppervlakteband per kolom kost geen extra ruis.

### 1.2 Meting (wegwerptest, niet gecommit, opgeruimd)
405 vensters van 48 × 48 blokken over ring 4 (buiten de zones), testseed. Per blok de exacte hoogte (`IslandField.baseHeight`) tegen de bilineaire weergave van de 4-blokhoeken; "top anders" = het bovenste vaste blok verschilt van de exacte hoogte. Indeling naar de dominante landvorm (gewicht > 0,9).

| Landvorm | gemiddelde \|fout\| | p99 | max | top anders ≥ 1 | ≥ 2 |
|---|---|---|---|---|---|
| `cascade_vale` (terrassen) | 0,07 | 1,06 | 2,77 | 8,1 % | 0,37 % |
| `mist_fjords` totaal | 0,09 | 1,50 | 6,49 | 8,5 % | 1,2 % |
| — fjordrand (\|cut − plateau\| ≤ 4) | **0,72** | **4,21** | **7,20** | **43 %** | **18 %** |
| — fjordwand | 0,09 | 1,66 | 5,21 | 9,3 % | 1,8 % |
| — fjordplateau | 0,02 | 0,06 | 5,05 | 1,6 % | 0,06 % |
| moeras, lotus, lagune, bedorven band | ≤ 0,03 | ≤ 0,5 | 1,1–2,5 | 1–3 % | ≈ 0 |
| mengzones (geen gewicht > 0,9) | 0,06 | 0,83 | 6,49 | 5,1 % | 0,30 % |

Terrassen (`cascade_vale`, alleen het terrasdeel):
- Stootbordbreedte (trede 8 hoog, `TERRACE_TREAD` 0,8 → het hele stootbord in de laatste 20 %): p10 5,7 · p25 7,3 · mediaan 10,6 · p90 36 blokken; **32 % smaller dan 8 blokken** (2 cellen), 1,3 % smaller dan 4.
- De traplijnen zijn iso-lijnen van `ring_4_hills` (firstOctave −8, golven van ~256 blokken): lange, nauwelijks gebogen lijnen.
- De detailruis zit ín `terrace()` (`+ 1,5 · d`, octaven van 32 en 16 blokken): op 4,6 % van de cascadeblokken verspringt de trede, als vlekjes en schiereilandjes van de volgende trede.
- **Kantelende treden:** 31 % van het cascadegebied (gewicht > 0,1) ligt in de mengzone (`BLEND` 0,045 ≈ 25 blokken) waar `cascadeW · terrace(…)` lineair mengt met moeras/lotus/fjord. Gemeten helling (zonder rivieren): vlak (< 0,05) 68 %, scheef (0,05–0,6) 18 %, steil 13 %; in de kern (gewicht > 0,9) vlak 76 %, scheef 11 % (de schouders van de smoothstep).
- Fjordplateau: `+ 2,0 · d` geeft binnen 16 × 16 blokken een standaardafwijking van mediaan 0,48, p90 0,95 blok: zachte bulten op wat vlak hoort te lijken.

Varianten (zelfde meting, alleen het terrasdeel):

| Variant | p99 | max | top anders ≥ 1 | ≥ 2 | treeblokken met bult ≥ 0,5 |
|---|---|---|---|---|---|
| nu (0,8, smoothstep, `d` in de trede) | 1,05 | 2,77 | 6,7 % | 0,32 % | 0,8 % |
| `d` buiten de trede | 0,97 | 2,48 | 6,0 % | 0,24 % | — |
| **A: trede 0,55, smootherstep, `d` buiten** | **0,45** | **1,07** | 4,6 % | **0 %** | **0 %** |
| A + golvende lijnen (±0,35 trede, ~100 blokken) | 0,46 | 1,16 | 4,9 % | 0 % | 0 % |
| fjordrand met smooth-min (k = 6) | 3,62 | 5,88 | 39 % | 9,2 % | — |

"Terras na het mengen" (eerst ongetrapt mengen, dan `terrace()` met sterkte `smooth(cascadeW, 0,2, 0,6)`): vlak 68 → 77 %, scheef 18 → 13 %, steil 13 → 10 % in het cascadegebied.

### 1.3 Conclusie: waar de hoeken vandaan komen
1. **Fjordranden (grootste bron):** `min(plateau, cut)` knikt van vlak naar een wand van 2:1 binnen één cel. De bilineaire weergave maakt daar een zaagtand op het 4-raster: 43 % van de randblokken ligt anders dan bedoeld, 18 % zelfs 2 tot 7 blokken. Smooth-min alleen halveert dat (9 %). De wand zelf is bijna vlak en klopt goed.
2. **Terrasranden:** de hele trede-hoogte zit in 20 % van de trede, dus de knik vlak → steil ligt binnen één cel (32 % van de stootborden is smaller dan 2 cellen). Gevolg: hoekige trederanden op het raster. De traplijnen volgen de heuvelruis van ~256 blokken en lopen daardoor lang en recht.
3. **Niet egaal:** kantelende treden in de mengzones (31 % van het terrasgebied), vlekjes van de detailruis op de treden (4,6 %) en bulten van ±0,5–1 op de fjordplateaus.

### 1.4 Fixvoorstellen (stroom W-terrein, commit "heuvelfix")
| # | Waar | Wijziging (startwaarden) | Verwacht effect (gemeten) |
|---|---|---|---|
| H1 | `Ring4Relief.terrace` | `TERRACE_TREAD` 0,8 → **0,6** (gemeten bij 0,55), stootbord met smootherstep (`6t⁵ − 15t⁴ + 10t³`): afgeronde trederand én voet | p99 1,05 → ~0,45, max 2,8 → ~1,1, top ≥ 2 → 0 %; stootborden ~2× breder (p10 ~11) |
| H2 | cascade-term | detail uit de trede: `terrace(rise)` zonder `d`; `0,6 · d · riser` alleen op het stootbord (`riser` = 0 op de trede, 1 midden op het stootbord) | treden exact vlak op gehele hoogte; geen vlekjes (4,6 % → 0) |
| H3 | cascade-term | **golvende traplijnen:** `rise + 0,35 · 8 · warp(1,3x + 777, 1,3z − 333)` (bestaande `ring_4_warp`, ~100 blokken) | lijnen slingeren ±5–15 blokken; geen extra rasterfout (p99 0,46) |
| H4 | `Ring4Relief.sample` | **terras na het mengen:** cascade mengt eerst ongetrapt (`SEA + 1 + rise`), daarna `h = lerp(smooth(cascadeW, 0,2, 0,6), h, SEA + 1 + terrace(h − SEA − 1))` | vlak 68 → 77 %, scheef 18 → 13 % |
| H5 | fjordterm | rand: smooth-min (k = 4) + **exacte rand-/wandband** per blok (zie Q6): kolommen met \|cut − plateau\| ≤ 6 of wandhelling > 1 krijgen in `thin` ±1 rond de exacte hoogte, van `min(exact, bilineair) − 1` tot `max(…) + 1` | randfout naar 0 (per blok exact); ~27 % van de fjordkolommen, geen extra ruis (relief is er al) |
| H6 | fjordplateau | detail `2,0 · d` → `1,0 · d`, alleen het grove octaaf (`d` op halve frequentie) | plateaubulten ~gehalveerd, nog "levend" |

Het karakter blijft: treden van 8 met dezelfde niveaus (`SEA + 1 + 8n`), fjordwanden 2:1. Tests (in `Ring4ReliefTest`, de rastermeting van de wegwerptest als blijvende helper): cascadekern p99 ≤ 0,6 en max ≤ 1,5; ≥ 75 % vlak in het cascadegebied; fjordrand top ≥ 2 anders ≤ 1 % (na de exacte band); waterandeel blijft 42–58 %, hooglandaandeel 2–30 %. Gevolg: eilanden met hun onderkant "15 boven de grond" verschuiven licht (deterministisch).

---

## 2. Ontwerp terrein `verdant_falls`

### 2.1 Coördinaten en profiel
- Over de zone: `u` = effectieve afstand − (r₃ − Z/2), 0…Z met Z = `transitionWidth(2)` (256). De verruisde grens (±96) zit al in de effectieve afstand, dus de hele afdaling slingert mee. Langs de ring: `s` = boog op de middellijn (periodiek over de naad, zoals `Ring4Cliff.arc`/`wrap`/`loopDistance`, naar een kleine helper `RingArc`).
- Gemeten (testseed, 720 stralen): de ring 3-hoogte aan de rand (u = 60) is p10 92 · mediaan 100 · p90 108. De ring 4-basis net buiten de zone is mediaan 63 (49 % onder zee, p90 80). Val van de afdaling naar zeeniveau: ~28–45 blokken.

| u (blokken, Z = 256) | Deel | Inhoud |
|---|---|---|
| 0 – 60 (`EDGE_SHARE` 0,234375) | `steam_fens` (ring 3) | ring 3-terrein; de ring 4-invoer is daar de ring 3-hoogte van dezelfde kolom (`VolcanicField.column`), dus de menging klopt |
| 60 – ~216 | afdaling `verdant_falls` | bovenste trede op T = ring 3-hoogte op de rand (zelfde straal), geklemd [64 + 24, 64 + 56]; treden op de niveaus **64 + 8k** (dezelfde als de cascades); smalle treden boven en bredere onder (`v^0,8`); stootborden als H1–H3 (afgerond, golvend: de stootbordpositie slingert ±14 blokken langs `s`) |
| ~216 – 236 | voet | **voetrivier op zeeniveau** (Q1-A): bed Y 57, 12–18 breed, slingerend ±8, oevers 0,6 zoals de ring 4-rivieren |
| 236 – 256 | aansluiting | menging naar de ring 4-basis (`Ring4Relief`); de ring 4-rivieren die de zonerand raken monden vanzelf in de voetrivier uit |

- **Ring 3-helft** (u < 128): de grond blijft ≥ Y 65 (seal onder 63, zie §3); test `theDescentStaysAboveTheSealInRing3sHalf`.
- **Treden:** het verloop is continu langs `s` (geen stappen op celgrenzen). Een stootbord heeft een helling van ~0,6–1 (te voet te nemen met 1-bloksprongen), behalve bij een val (§2.4).

### 2.2 Getrapte bassins (Plitvice)
- Per trede een eigen periodieke celreeks langs `s` (cellen van ~90 blokken, salt per trede). Een cel is een **meersegment** (55 %) of een **weidesegment met beek** (45 %). Tussen meren ligt minstens 8 blokken droge trede (2 cellen), zodat elk meer los ligt en ingesloten is.
- **Meer:** vult de trede vanaf 3 blokken na de voet van het stootbord erboven tot een **drempelrand van 6–10 blokken** voor de rand naar de volgende trede; kom 2–5 diep met oevers over 4 blokken; `lakeTop` = trede-niveau − 1 en nooit hoger dan de laagste rasterwaarde van de rand − 1 (de regel van de eilandmeren: geen water tegen lucht door de interpolatie). Water en guard-band via de bestaande meertak (`Column.lakeTop`, `caveLiftFrom/To`, de carver-guard).
- **Uitloop:** 1–3 inkepingen per meer: een geul van 2 breed op meerhoogte door de drempel tot de trederand. Aan het eind een **bron** (stromende bron, aan drie kanten ommuurd) en een **vastgelegde val** in de kolom erbuiten, die landt in het meer of de plonspoel van de trede eronder (`Fall.bottom` = de `lakeTop` daar).
- **Weidesegment:** de val van boven landt in een **plonspoel** (straal 3–5, eigen meer). Van daar loopt een **beek in een geul** (2–3 breed, 1–2 diep, stil water op poolhoogte, slingerend ±10 langs `s`) over de trede naar de rand, met daar weer bron + val. Zo is elke beek stilstaand water per trede, en stroomt water alleen in vastgelegde vallen (waterscan-regel).
- **Laatste trede:** de vallen storten in de voetrivier (zee, `Fall.bottom` 63).
- De geulen zijn 2–3 breed, smaller dan een cel. Ze worden daarom **per blok gesneden** (`thin` −1 van de geulbodem tot de trede). Voor die kolommen moet de aquifer de exacte geulbodem als `lakeFloor` krijgen, niet de geïnterpoleerde surface (nieuw veld `Column.exactFloor`).

### 2.3 Hoefijzerkloven (Iguazu)
- **Cellen:** één kloof per boogcel van ~700 blokken op de middellijn (`round(omtrek / 700)` ≈ 54 rondom), midden op 25–75 % van de cel, overgeslagen waar T − G < 20 (verwacht ~50). Periodiek over de naad.
- **Vorm in (s, u):** een halfrond amfitheater met straal R 45–75, de achterwand 25–40 blokken na de rand (u ≈ 85–100). Daarna twee rechte armen naar buiten tot de voet; de armen worden lager waar de treden ernaast zakken.
- **Bodem:** een kloofmeer op G = een trede-niveau, zodat de wandhoogte H = 20–40 is (typisch G 72, H ~28). Het meer ligt in de ring 3-helft en is dus een eigen meer boven Y 64. De bodem daalt naar buiten getrapt met de bassinmachinerie (meer → lip met 2–4 vallen → volgend meer) tot de voetrivier. Langs de armen ligt een droge oever van 4–6 breed.
- **Wanden:** per blok exact in een band van ±6 rond de wandlijn, met de ruwheid van 5a (`Ring4Relief.rough`, richels en overhangen ±3, afbrokkelende bovenrand). Binnen 3 blokken van een val is de wand glad.
- **Vallen:** langs de hele boog van het amfitheater om de 10–14 blokken (~12–20 per kloof). Ze worden gevoed door een **randmeer** op de bovenste trede rond de kloof, met inkepingen = bronnen. Waar het amfitheater breed is, staan 4–6 vallen per chunk; zie Q4 (cap).
- **Richelpad achter de vallen:** op halve wandhoogte een richel van 3 breed, 3 blokken in de wand (y-band 4 hoog), langs het hele amfitheater. De valkolommen staan op +1 buiten de wandlijn, dus je loopt achter het water. Langs beide armen loopt de richel als helling (1 op 2) naar de oever en naar de rand.
- **Grotten:** achter 1 op 3 kloofvallen een nis op richelhoogte (3 breed, 4 hoog, 5–8 diep).
- **Rotsboog:** in 50 % van de kloven een natuurlijke brug over de uitloop (overspanning 14–30, dikte 3–4, dek 3 breed, 8–14 boven het water).

### 2.4 Paden, richels, grotten en lianen in de bassins
- **Te voet:** de afgeronde stootborden zijn overal te nemen, behalve op de vallippen.
- **Vallip:** ±3 langs `s` rond elke val een exacte verticale lip, van 2 blokken binnen tot 3 buiten de rand, van het trede-niveau tot het water eronder. Zo hangt de val vrij en heeft de bron zijn muren. Dit lost het probleem van golf 1 op ("een val van een geïnterpoleerde terrasrand landt droog").
- **Grotten achter vallen:** bij 1 op 4 bassinvallen met een lip ≥ 6 een nis achter de val op waterhoogte + 1. De nisvloer is gesteente tot `lakeTop` − 1, zodat het water nooit naast lucht staat. Bij 1 op 3 daarvan loopt een **korte tunnel** (3 × 3, helling ≤ 1:2) naar de trede erboven (uitgang ≥ 6 van de rand): "soms een korte route". Tunnels houden 2 blokken gesteente afstand van elk meer en elke geul.
- **Rotsbruggen in de bassins:** over 1 op 5 beken (overspanning 4–8, dikte 2–3).
- **Lianen en wortels:** feature, niet terrein (W-biomes). De vorm volgt Q3.

### 2.5 Terrein of feature (contract §4: grote vormen zijn terrein)
- **Terrein (veld/DF):** profiel, treden, meren, geulen, plonspoelen, voetrivier, kloven, wanden, richels, nissen/tunnels, rotsbogen, vallen (water-tak).
- **Feature (W-biomes):** begroeiing, klimhulp (Q3), placeholder-aankleding (mos). Geen water via features, behalve wat de waterscan als vastgelegde val kent: niets.

### 2.6 Klassen, determinisme, prestatie
- **Klassen** (`worldgen/densityfunction`, puur):
  - `VerdantDescent`: profiel, randhoogte T op een raster langs `s` (elke 4 blokken boog, per wereld onthouden; vervangt de dure `plateauAt` per kolom, backlog-prestatiepunt 5), treden;
  - `VerdantBasins`: segmenten, meren, geulen, pools, vallen per trede;
  - `VerdantGorges`: kloofcellen, vorm, wand, richel, nissen, bogen, vallen;
  - `RingArc`: boog, wrap, loop-afstand, celtelling (uit `Ring4Cliff`).
  - `IslandField`: `cliffed()` → `descended()`; `Column` krijgt de verdant-velden (exacte-bandsoort, wandafstand, richel, `exactFloor`, nis/tunnel, boog). `thin` krijgt de per-blokvormen. `fallsOfChunk` neemt de verdant-vallen op (eigen cap, Q4), en `waterfallsWithin`/`/ca falls` dus ook. `initialHeight` telt meer-, geul- en kloofbodems mee (`verdantFloorNear`, valkuil Y 55).
- **`Ring4Cliff` verdwijnt.** `Ring4CliffTest` wordt vervangen door `VerdantDescentTest`, `VerdantBasinsTest` en `VerdantGorgesTest`. De DF `ring_4/overworld/cliff` blijft bestaan, met als nieuwe betekenis "exacte steile wand van `verdant_falls`" (kloofwanden, vallippen; 0..1). Er waren geen lezers.
- **Determinisme:** cellen op `s` modulo het aantal (periodiek over ±π), hash `CellShapes.cell(salt(), k, …)` met eigen salts (kloof 6301, segment 6302 + trede, val 6303, nis 6304, boog 6305). Ruis via de `relief`-noises. Beslissingen eenmaal per wereld in `Decisions`; nooit via een `flat_cache`; geen `Random`. Vallen per chunk gesorteerd op (x, z, springY) en dan afgekapt. Tests: twee keer gebouwd en omgekeerd gevraagd → gelijk; een kloof en een segment over de naad.
- **Prestatie:**
  - Per kolom: 1 hash per celsoort, 2 ruiswaarden (lijngolf, boogmodulatie) en de T-memo.
  - Per blok alleen in gevlagde kolommen (wandband, lippen, geulen, nissen, bogen, fjordband H5); elders keert `thin` na één vlagcheck terug.
  - Winst: met de menginstelling (§3) wordt de ring 3-dichtheid voorbij de rand niet meer berekend (196 van de 256 blokken).
  - Meten bij integratie (budget ≤ 1,75×).

---

## 3. Gevolgen van zone 3→4 = 256

| Lezer | Gevolg | Actie |
|---|---|---|
| `RingLayout.positionAt` (en dus `ringAt`, biome, condities, weer, HUD/`/ca ring`, later kompas) | zone 3→4 = 5.872–6.128 (+ grensruis), Nether 1.468–1.532; middellijn ongewijzigd | test `RingLayoutTest` (`transitionWidth(2)` = 256) |
| `RingBlend` (alle `ring_blend/overworld*`) | nu een smoothstep over 256: ring 3 weegt op de afdaling nog fors (u = 128 → 50 %), zodat exacte vormen (`thin` ±1) met ring 3-dichtheid mengen | **nieuw optioneel codecveld `mix`** (per grens het deel van de zone waarover gemengd wordt): `"mix": {"ring_3": 0.234375}` → ring 3 ↔ 4 mengt alleen in de `steam_fens`-strook, daarna puur ring 4. In alle overworld-`ring_blend`-bestanden gelijk (test: JSON = `IslandField.VERDANT_EDGE_SHARE`) |
| `RingBiomeSource` `transitions.ring_3` | `steam_fens` zou de hele 256 vullen | **nieuw optioneel veld `transition_splits`**: `{"ring_3": {"at": 0.234375, "outer": <verdant_falls>}}`; zelfde aandeel als het terrein, dus biome-grens = terreinrand (deterministisch op de zone-as) |
| `VolcanicField.regions`, `VolcanoPlacement.place` (lezen `transitionWidth(2)`) | ring 3 stopt 64 blokken eerder: reliëf vervaagt eerder (gemeten op u = −72: p10–p90 72–150 → 85–124), vulkanen met hun midden in 5.872–5.936 vallen weg; deterministisch, alleen nieuwe werelden | geen code; `VolcanicFieldTest.ring3ReadsOnlyTheWidthsOfItsOwnBoundaries` herschrijven (vergelijk standaard met dezelfde lijst met alleen 4\|5 anders) |
| `ring3Seal` (`RingFluidPicker.ring3SealBelow`) | ring 3-helft nu 128: onder Y 63 dicht onder het bovenste deel van de afdaling | geen (besluit); de afdaling blijft daar ≥ 65 (test) |
| `ring4DamBelow` | dam onder −54 over de ring 4-helft van 128 | geen |
| `ring3DryFrom`, ring 3-vloeistofniveau (zee onder Y 0), `ring3LavaColumn` | in de ring 3-helft van `verdant_falls` kan geen water staan | **nieuw predicaat `verdantWater`** (overworld, zone 3→4, blend ≥ `VERDANT_EDGE_SHARE`): de water-tak (bronnen, vallen, meren) van `RingAquifer` geldt ook daar (`ring4FallColumn` → `ring4WaterColumn`); geen ring 3-lavakolom daar. Seal, droogte van vanilla-water (> Y 0) en dammen blijven. Tests in `RingAquiferTest`/`RingFluidPickerTest` |
| `HeatRule`/`HeatZone` (verdamping, ijs, spons; hard op 0,5) | u 60–128 van `verdant_falls` is ring 3: hitte, geplaatst water verdampt | Q2 |
| `PoisonRule` (ring 4, hard op 0,5) | ring 4-helft van `verdant_falls`: geen bronnen, meter zakt | geen |
| `RingWeather.stateAt` (hard 0,5); `RingWeatherMix` (weergave, smoothstep) | ring 3-weer tot de middellijn; de weergave van aswolken en mist vervaagt over 256 i.p.v. 128 | geen (besluit "weer volgt dezelfde grens") |
| `IslandPlacement` (eilanden buiten de zones) | binnenrand van de eilanden 64 blokken verder naar buiten | geen |
| `Ring4Band`, `Ring4Sundered` (`transitionWidth(3)`) | ongewijzigd | — |
| Nether trede 3\|4 (lava 22 → 10, `ring_edge`-dammen 48 breed) | Nether-zone 32 → 64 (±32 < 48): op de middellijn blijven beide dammen 1, de trede dus afgedekt | `Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround` moet slagen; INT: `lavascan.py` op `areas/nether-walls.txt` / `tools/nether-lava-walls.py … 1500` → 0 wanden |
| Config | een bestaand `corruptedadventure-common.toml` houdt [0, 0, 0, 256, 0] | **migratie** zoals 37c/58: precies de oude standaardlijst → [0, 0, 256, 256, 0] + opslaan + INFO; anders WARN. `CommonConfigTest` |
| Snapshots / bestaande werelden | houden hun zone van 128 (`ring_settings.json`); de verdant-code werkt op aandelen en klemt R (bij 128: strook 30, afdaling ~78, steiler) | testwerelden `ring4-wave1` e.d. niet hergebruiken: nieuwe chunks naast oude geven naden; altijd een verse wereld |
| `RingLayoutPayload` / `PROTOCOL_VERSION` | de lijst zit al in de payload (var-ints, vaste lengte 5); alleen een standaardwaarde verandert | **geen bump** (blijft `"8"`), geverifieerd in `RingLayoutPayload` r. 26 |
| Tests met `DEFAULT_TRANSITION_WIDTH_OVERRIDES` (~15 klassen, o.a. `RingSettingsTest` JSON-literal, `RingLayoutTest`, `IslandFieldTest.LAYOUT`) | zien 256 | volledige suite in de I-stroom; alleen verwachtingen aanpassen |
| `tools/test/areas/ring4.txt` b34* (9 chunks dwars) | zone + ruis = 5.776–6.224 | b34* naar chunks 361–389 dwars (29) × 9 langs; `.gitattributes` `tools/test/areas/*.txt eol=lf` (lost de CRLF-backlog op) |

---

## 4. Biome `verdant_falls`

- **Registratie:** key `corruptedadventure:verdant_falls` (`ModBiomes.VERDANT_FALLS`, I-stroom). `Ring4Biomes`: `TRANSITIONS` = `sundered_reach`, `verdant_falls`; `WITH_TRANSITION` bevat beide (bootstrap, `is_ring_4`, surface-tak).
- **Look** (startwaarden, W-biomes zet ze in `LOOKS`): lucht `0x86D6E0` (helder turquoise), mist `0xD2F0E4` (zacht wit-groen), water `0x3FD0C0` (helder turquoise-groen), waterfog `0x0F4F4A`, gras `0x6CC24A`, blad `0x55B03A`; downfall 0,95, regen ja.
- **Plaatsing:** `transitions.ring_3` blijft `fixed steam_fens`; nieuw `transition_splits.ring_3 = {at: 0.234375, outer: fixed verdant_falls}`. De grens loopt deterministisch op de zone-as (blend). Omdat die as de verruisde grens volgt, slingert hij mee. Hij valt samen met de terreinrand: de bovenste, vlakke trede. Biomes per quart (4 blokken). Hoogte-biomehaak ongewijzigd (alleen eilanden uit ring 4 zelf).
- **Tags:** `is_ring_4`; `c:is_wet/overworld`, `c:is_magical`, `c:is_lush`, `c:is_temperate/overworld` (via `WITH_TRANSITION`). Niet: `ring_4_blighted`, `is_ring_3`, `c:is_jungle`. `heat_refuges` volgens Q2.
- **Condities/gif (contract §5):** zone overworld ring 4 hard op 0,5. `verdant_falls` valt dus in de buitenste helft onder ring 4 (gif aan, maar geen bronnen) en in de binnenste helft (u 60–128) onder ring 3-hitte (Q2).
- **Surface** (`tools/worldgen/ring4_surface.py`, `verdant_falls` in `RING_4`): treden `grass_block` op `dirt` met mosplekken (`moss_block` via `noise_threshold`); steile en exacte wanden steen + bemoste kei (+ `tidestone`-lagen); meer- en geulbodems klei/zand/grind (`water`-conditie); drempelranden volgens Q7. Script draaien → `noise_settings/overworld.json`.
- **Placeholder-aankleding (geen flora):** mostapijt en mosplekken op de treden, natte klei langs oevers; klimhulp volgens Q3. Geen bomen, geen jungleblokken.
- **Spawns:** volgens Q9.
- **Carvers:** zoals ring 4 (`Ring2Carvers`-kopieën); de meer-guard beschermt meren, geulen en kloofmeren.
- **Lang:** `Ring4BiomeNames`: en "Verdant Falls", nl voorstel "Smaragdvallen" (gebruiker mag bijsturen).
- **Sfeer (stroom A):**
  - hook `BiomeAmbience.ring4(key)` → nieuw `Ring4Atmosphere` (basis voor 80): particle, ambient-loop, additions en mood voor `verdant_falls`, de rest leeg tot 80;
  - de W-biomes-bootstrap past de hook toe;
  - geluid en spray volgens Q8;
  - ondertitels in `Ring4AmbienceText` (nieuwe helper, A).
- **Kaartkleur:** `MapPalette` geeft ring 4-biomes automatisch een hash-kleur.
- **Controlelijst (nieuw in CODEBASE, INT):** "Plekken die ring 4-biomes opsommen": preset (`rings.ring_4`, `transitions`, `transition_splits`), `ring4_surface.py` (`RING_4`), `Ring4Biomes`-lijsten, `Ring4BiomeTags`, `Ring4BiomeNames`, `Ring4Atmosphere`.

---

## 5. Stromen en volgorde

Worktrees en poorten zoals `waves.md` §4 (branches `ring4/vf-<stroom>`). Licht per stroom: B (build), D (runData + `git diff --stat src/generated`), unit tests. Zwaar één keer bij INT. Gradle altijd `--no-daemon`; speel-lock `C:/Temp/ca-user-test.lock` respecteren.

| # | Stroom | Categorie | Inhoud en bestanden | Tests | Afhankelijk | Parallel |
|---|---|---|---|---|---|---|
| 1 | **I** (`ring4/vf-infra`, poort 7) | `infra` | `CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES` → [0, 0, 256, 256, 0] + migratie van de oude lijst; `ModBiomes.VERDANT_FALLS` (key); `tools/test/areas/ring4.txt` b34* breder + `.gitattributes`; testverwachtingen die 128 aannemen (incl. de herschreven `VolcanicFieldTest`-vergelijking, alleen test) | B, volledige suite, `CommonConfigTest`, `RingSettingsTest`, `RingLayoutTest`, `RingLayoutPayloadTest` | — | eerst (klein); de rest takt af na de merge |
| 2a | **W-terrein** commit 0 (`ring4/vf-terrain`, poort 1) | `worldgen` | `RingBlend` veld `mix`; `RingBiomeSource` veld `transition_splits` (code); `RingFluidPicker.verdantWater` + `RingAquifer` water-tak; `IslandField.VERDANT_EDGE_SHARE`; `ring_blend/overworld*.json` `mix.ring_3` | B, D, `RingBlendTest`, `RingBiomeSourceTest`, `RingAquiferTest`, `RingFluidPickerTest`, `Ring4JsonTest` | 1 | vroeg naar main mergen (vooruit), zodat W-biomes het preset kan zetten |
| 2b | W-terrein commit 1 "heuvelfix" | `worldgen` | `Ring4Relief` (H1–H4, H6), `IslandField.thin` fjordband (H5) | B, D, `Ring4ReliefTest` (rastermeting, vlakaandeel, waterandeel), `Ring4IslandsTest` | 2a | ∥ 3, 4, 5, 6 |
| 2c | W-terrein commit 2 "verdant-terrein" | `worldgen` | `VerdantDescent`, `VerdantBasins`, `VerdantGorges`, `RingArc`; `IslandField` (descended, Column, thin, falls, initialHeight); `IslandOutput` (`cliff`-betekenis); `Ring4Cliff` + `Ring4CliffTest` weg | B, D, `VerdantDescentTest` (profiel, ≥ 65 in de ring 3-helft, stootborden ≥ 8 breed buiten de lippen, naad), `VerdantBasinsTest` (elk meer ingesloten op het 4-raster, elke bron 3 muren, elke val landt in water, geulen vlak per trede, cap per chunk), `VerdantGorgesTest` (~50 kloven, wand exact en begrensd ruw, richel 3 breed, vallen in het kloofmeer, bogen boven water), `RingAquiferTest`, `IslandFieldTest` (determinisme) | 2b (zelfde bestanden) | ∥ 3, 4, 5, 6 |
| 3 | **W-biomes** (`ring4/vf-biomes`, poort 2) | `worldgen` | `Ring4Biomes` (lijsten, Look, spawns, generation), `Ring4BiomeTags`, `Ring4BiomeNames`, `Ring4Features` (placeholder-aankleding, klimhulp), preset `transition_splits`, `ring4_surface.py` + `noise_settings/overworld.json`; bij Q2-A `heat_refuges`; bij Q7-A de surface-conditie `ring4_threshold` (patroon `volcanic_threshold`, leest `island_output`) | B, D (biome-, tag-, lang-JSON), `Ring4BiomesTest`, `RingBiomeSourceTest` (preset), `Ring4JsonTest` (aandeel = Java) | 1; preset na 2a | ∥ 2b/2c, 4, 5, 6 |
| 4 | **A** (`ring4/vf-atmosphere`, poort 5) | `atmosphere` | `Ring4Atmosphere` + `BiomeAmbience.ring4`, `ModSoundEvents` (`ambient.verdant_falls.*`), geluid (synthscript `tools/sounds/Ring4Sounds.java` of placeholder, Q8), particle (Q8), bij Q8-B `client/ambient/WaterfallSpray` (alleen `Dist.CLIENT`), `Ring4AmbienceText` | B, D (sounds.json, lang), unit (Look/hook-test) | 1 | ∥ |
| 5 | **C** (alleen bij Q2-A, `ring4/vf-conditions`, poort 4) | `conditions` | `WaterEvaporation`/`HeatZone.exposed` slaan `heat_refuges` over (zoals `IceMelting`) | B, unit | 1 | ∥ |
| 6 | **B** (alleen bij Q3-B/C, `ring4/vf-blocks`, poort 0) | `blocks` | `minecraft:climbable`-tag voor de wortels/het mos van ring 4 (Q3-B) of een placeholder-lianeblok (Q3-C) | B, D | 1 | ∥ |
| 7 | **T** (alleen bij een nieuwe particle) | `textures` | sprite `waterfall_mist` in `Ring4Textures.java` | contactblad | 4 (naam) | ∥ |

**Integratie** (snel protocol `waves.md` §6), merge-volgorde: I → W-terrein (2a, 2b, 2c) → W-biomes → B → T → A → C. Daarna:
- `runData` + `git diff --exit-code src/generated`;
- **S-smoke:** dedicated server + 1 client, verse wereld `ring4-vf`;
- **W-smoke:** `/ca tpboundary 3` in 4 richtingen, portal vanuit `verdant_falls` ↔ Nether;
- **offline:**
  - waterscan `tools/test/waterscan.sh ring4-vf tools/test/areas/ring4.txt` (b34* breed + `vf*`-gebieden rond 4 kloven uit `VerdantGorgesTest`) → 0 over in b34/vf;
  - determinisme omgekeerd (`det.sh`);
  - feature-index ring 1–3 (ring 3 verandert verwacht aan de buitenrand: vergelijk r < 5.800);
  - Nether-lavawanden r 1.500 → 0;
  - terreinkaarten (`/ca terrainmap`) van de zone en van cascades/fjorden vóór en na;
- **prestatie** (`perf.sh`, meetpunten: bassins, kloof, cascades, fjorden, plus de bestaande; ≤ 1,75×);
- `/code-review` medium op de integratiediff;
- docs: CODEBASE, contract §1/§2/§4, `fase-1-wereld.md`, beslissingenlog, backlog, `teststop-5a2.md`.

---

## 6a. Antwoorden gebruiker (2026-10-15) — bindend

- **Q1 = A:** voetrivier op zeeniveau (Y 63) rondom langs de buitenrand van de zone; alle vallen en kloven monden erin uit, ring 4-rivieren sluiten aan, bevaarbaar rond de ring; afdaling ~28–45.
- **Q2 = A:** `verdant_falls` in `heat_refuges`; verdamping en `HeatZone.exposed` slaan schuilplekken over (stroom **C** doet mee). Weer en kompas houden de middellijn.
- **Q3 = B + richels:** de ring 4-wortels en het hangmos (`glowing_hanging_roots`, `hanging_swamp_moss`, en wat er verder als hangende ring 4-wortel bestaat) krijgen `minecraft:climbable` (stroom **B** doet mee; geldt in heel ring 4). **Extra besluit:** op de kloofwanden en naast de vallen maakt het **terrein** (W-terrein 2c) korte **overhangende richels** op klimafstand onder elkaar; een **feature** (W-biomes) hangt de wortels eronder tot de volgende richel, zodat een echte klimroute van richel naar richel ontstaat. Geen vanilla `vine`.
- **Q4 = A:** eigen cap van **6 vallen per chunk** voor `verdant_falls` (los van de 3 voor eilanden); meten bij INT.
- **Q5 = A:** trede **60 % / stootbord 40 %**, afgeronde rand en voet (geldt voor `cascade_vale` én de treden van `verdant_falls`).
- **Q6 = A:** fjordrand en -wand **per blok exact** (band in `thin`) plus smalle afronding (k = 4); kliffen blijven 2:1.
- **Q7 = A:** **calciet** op drempelranden en vallippen, mos op de treden (surface-conditie `ring4_threshold`, patroon `volcanic_threshold`).
- **Q8 = A:** biome-particle **`waterfall_mist`** (nieuwe particle, sprite door stroom **T**), eigen ambient-loop (verre watervallen) en additions (tropische vogels/kikkers) uit een synthscript. Alleen biome-data, geen client-scan.
- **Q9 = A:** kikkers en schildpadden bij de meren; geen papegaaien.

Daarmee doen alle stromen mee: I, W-terrein (2a–2c), W-biomes, A, C, B, T.

## 6. Open ontwerpvragen (beantwoord, zie §6a)

**Q1 — De voet van de afdaling.**
- **A (aanbevolen):** overal een **voetrivier op zeeniveau** langs de buitenrand van de zone, waar alle vallen en kloven in uitmonden en waar ring 4-rivieren op aansluiten. Het water "keert terug" zichtbaar, per boot te bevaren rond de hele ring. Afdaling altijd ~28–45 hoog. Waar ring 4 net buiten de zone hoog ligt (cascades), ontstaat een dal aan de voet.
- **B:** de voet volgt het ring 4-terrein. Geen dal, maar waar ring 4 hoog is wordt de afdaling kort (~15–20) en eindigt het water in een laatste meer boven zee. Minder samenhang, iets eenvoudiger.

**Q2 — Hitte in het ring 3-deel van `verdant_falls` (u 60–128, ~70 blokken).**
- **A (aanbevolen):** `verdant_falls` in `heat_refuges`, en verdamping slaat schuilplekken over (kleine C-stroom). Op de afdaling koel je af, zoals in de slapende krater. Geplaatst water blijft liggen. Weer en kompas houden de middellijn.
- **B:** ring 3-hitte tot de middellijn, zoals nu. Consequent met "condities volgen dezelfde grens", maar je verhit in een tropisch watervalgebied en geplaatst water verdampt daar. Geen extra werk.
- **C:** alleen `heat_refuges` (afkoelen), maar geplaatst water verdampt nog wel. Geen C-stroom.

**Q3 — Klimhulp (lianen en wortels) tot de eigen flora er is.**
- **A (aanbevolen):** vanilla `vine` aan de kloofwanden en vallippen als placeholder-klimhulp. Klimbaar zonder nieuw blok; oogt licht jungle-achtig, maar zonder jungle-bomen of -blokken. Later vervangen door de eigen liaan.
- **B:** de ring 4-wortels/het hangmos (`glowing_hanging_roots`, `hanging_swamp_moss`) klimbaar maken (`minecraft:climbable`, kleine B-stroom). Past bij "ranken onder eilanden klimbaar", maar die blokken hangen alleen onder een overhang, dus weinig plekken op wanden. Raakt ook de rest van ring 4.
- **C:** een eigen placeholder-liaanblok nu (B + T): meer werk, wordt later toch de echte flora.
- **D:** geen klimhulp tot de flora er is; afdalen via stootborden, richels en tunnels.

**Q4 — Hoeveel vallen per chunk.**
- **A (aanbevolen):** verdant-vallen een **eigen cap van 6 per chunk** (los van de 3 voor eilanden). Hoefijzers krijgen ~12–20 vallen. Elke val kost bij generatie 2 vloeistof-ticks; meten bij INT.
- **B:** gedeelde cap 3. Waar de boog veel vallen in één chunk heeft, vallen er af: ~8–12 per kloof, en minder bij eilanden in de buurt. Geen prestatierisico.
- **C:** cap 8: het voller Iguazu-beeld, meer generatiewerk.

**Q5 — Terrasvorm na de heuvelfix (`cascade_vale`, ook de treden van `verdant_falls`).**
- **A (aanbevolen):** trede 60 % / stootbord 40 % met afgeronde rand en voet. Gemeten bij 55 %: rasterfout p99 0,45 (was 1,05), nooit 2 blokken mis. Stootborden ~2× breder (zachter, beloopbaarder), treden smaller.
- **B:** trede 70 %: tussenweg, hoekiger dan A, bredere treden.
- **C:** trede 80 % houden, alleen afronden en golven. Brede treden met steile stootborden; de hoeken blijven deels.

**Q6 — Fjordkliffen (randen).**
- **A (aanbevolen):** rand en wand **per blok exact** (band rond de exacte hoogte in `thin`), plus een smalle afronding (k = 4). Strakke, zachte rand zonder zaagtand; kliffen blijven 2:1. Op die wanden kunnen grotingangen het oppervlak niet breken; geen extra ruiskosten.
- **B:** alleen smooth-min (k = 6). Zachter, maar nog 9 % van de randblokken 2+ blokken mis.
- **C:** wanden minder steil (1,2 i.p.v. 2,0). Rasterfouten kleiner, maar de fjorden verliezen hun "steile kliffen".

**Q7 — Randen van de bassins (surface).**
- **A (aanbevolen):** **calciet** op de drempelranden en vallippen (travertijn zoals Plitvice), mos op de treden. Vraagt een kleine surface-conditie die de terreinwaarden leest (patroon `volcanic_threshold`).
- **B:** geen eigen randmateriaal: gras/mos, steen op steile delen (vanilla `steep`). Eenvoudiger, minder herkenbaar.
- **C:** `tidestone` op de randen: past bij ring 4, maar donker en grijs naast het turquoise water.

**Q8 — Sfeer bij de vallen.**
- **A (aanbevolen):** biome-particle "waterfall_mist" (fijne witte nevel, nieuwe particle met sprite van T), een eigen ambient-loop (verre watervallen) en additions (tropische vogels/kikkers) uit een synthscript. Alles biome-data, geen client-code.
- **B:** A plus **nevel aan de voet van echte vallen**: client-only scan van vallend water rond de camera (straal 32, budget per tick). Mooier, meer client-code in `client/`, geen sync nodig.
- **C:** minimaal: de bestaande `steam_puff` spaarzaam en vanilla-placeholdergeluiden. Echt geluid en particles in pakket 80.

**Q9 — Dieren in `verdant_falls`.**
- **A (aanbevolen):** kikkers en schildpadden bij de meren, papegaaien nog niet (die horen bij de eigen bomen). Levendig zonder flora.
- **B:** zoals `cascade_vale` (vossen, konijnen).
- **C:** leeg tot de flora er is.

---

## 7. Teststop-checklist (werktitel `teststop-5a2`, verse wereld `ring4-vf`, seed `20260930`)

INT vult na de bouw de exacte coördinaten in (kloven uit `VerdantGorgesTest`, de eerste vier per richting) en zet de waterscan-tabel erbij, zoals in `teststop-5a.md`.

1. **Starten:** `server.sh` of `gradlew runServer --no-daemon` vanuit main, `op <naam>`, verbinden op `127.0.0.1:<poort>`. `/ca tpring`/`tpboundary` werken langs de richting vanaf (0,0): eerst bv. `/tp @s 100 120 0` (oost), `0 120 100` (zuid), `-100 120 0` (west), `0 120 -100` (noord).
2. **Profiel 3→4** (per richting): `/ca tpboundary 3` (landt op de middellijn, in het ring 3-deel van de afdaling).
   - Loop ~70 blokken naar binnen: `steam_fens` (geisers, kookpoelen) tot de rand; F3 → biome-wissel op de bovenste vlakke trede.
   - Loop ~130 blokken naar buiten: treden, meren, beken, vallen, voet (Q1).
   - `/ca ring` laat de blend zien (rand ≈ 0,23, middellijn 0,5).
   - Klopt de hoogte (~30–45 val)? Is er nergens een rechte wand rondom? Zijn de randen afgerond?
3. **Bassins:**
   - Elk meer ingesloten (geen weglopend water)?
   - Vallen van meer naar meer en van geul naar plonspoel?
   - Beken als stil water in geulen?
   - Te voet omlaag over de stootborden zonder bouwen?
   - Nissen achter vallen en de korte tunnels naar een hogere trede.
4. **Hoefijzerkloven** (coördinaten van INT; anders langs de rand lopen, ~elke 700 blokken):
   - het amfitheater met ~12–20 vallen;
   - het richelpad **achter** de vallen;
   - de hellingen langs de armen;
   - de rotsboog over de uitloop;
   - het kloofmeer en de getrapte uitloop naar de voetrivier.
5. **Water bekijken:** na ~1 minuut stilstaan geen nieuw stromend water naast de vallen; de waterscan van INT (b34* en vf*) → 0 overtredingen.
6. **Sfeer** in `verdant_falls`:
   - lucht, mist en waterkleur (turquoise);
   - particles en geluid (Q8);
   - in het ring 3-deel hitte of afkoelen (Q2, condition-HUD);
   - ring 3-weer en ring 4-mist aan weerskanten van de middellijn.
7. **Heuvels:**
   - `/locate biome corruptedadventure:cascade_vale` → terrassen: afgeronde rand en voet, golvende traplijnen, vlakke treden (ook aan de biomerand)?
   - `/locate biome corruptedadventure:mist_fjords` → fjordranden zonder zaagtand, plateaus egaler, kliffen nog steil?
8. **Kaarten** (INT, vóór de game voorgelegd):
   - `/ca terrainmap 6000 0 400` (bovenaanzicht oost);
   - `/ca terrainmap section 5800 0 6300 0` (dwarsprofiel);
   - een kaart van een kloof;
   - cascade/fjord vóór en na.
9. **Nether:** `/ca tpboundary 3 nether` → de trede 3|4 dicht (geen lavawand), zone 64 breed.
10. **Portal:** vanuit `verdant_falls` naar de Nether en terug.
11. **Prestatie** (INT, tabel): meetpunten bassins, kloof, cascades, fjorden ≤ 1,75×.

---

## Implementatiekeuzes (niet door de gebruiker besloten; uit het plan)
- `RingBlend.mix` per grens (ring 3 ↔ 4 mengt alleen over de `steam_fens`-strook), zodat de afdaling puur ring 4 is en exacte vormen niet met ring 3-dichtheid mengen.
- `RingBiomeSource.transition_splits` voor twee biomes in één zone. Biome-grens = terreinrand = vast aandeel 60/256 van de zone (één Java-constante, JSON getest).
- Verdant-water in de ring 3-helft alleen via de water-tak van `RingAquifer`. `ring3Seal`, de droogte van vanilla-water boven Y 0 en de dammen blijven.
- `ring_4/overworld/cliff` blijft als sleutel, nieuwe betekenis (exacte verdant-wand). `Ring4Cliff` en zijn tests verdwijnen; boog-helpers naar `RingArc`.
- Treden op de niveaus 64 + 8k (gelijk aan de cascades); stootbordvorm gedeeld met de heuvelfix.
- Geulen per blok gesneden met een exacte bodem voor de aquifer (`Column.exactFloor`).
- De rand-T op een raster van 4 blokken boog per wereld onthouden (vervangt `plateauAt` per kolom).
- Config-migratie van de oude standaardlijst; geen `PROTOCOL_VERSION`-bump.

## Contract-delta (INT verwerkt in `contract.md`)
- §1: standaard `transitionWidthOverrides` [0, 0, 256, 256, 0]; migratie van [0, 0, 0, 256, 0].
- §2: `verdant_falls` (overworld, overgang 3→4 buitenste deel, `transition_splits.ring_3`, eigenaar W-biomes; terrein W-terrein), in `is_ring_4`.
- §4: DF `cliff` → "exacte steile wand van `verdant_falls`"; vloeistofregel: water-tak ook in de verdant-strook van de ring 3-helft; `RingBlend.mix.ring_3`; de klif 3→4 vervalt (golf 0-plan 2026-10-13 wordt historie).

## Backlog (buiten deze ronde)
- [blocks/textures] Eigen tropische flora voor `verdant_falls` (bomen, planten, liaan) — staat al in de backlog.
- [worldgen] Waterscan b34 uit 5a (klifbron bij −5998 53) vervalt met de klif; opnieuw meten in de brede b34*.
- [infra] Boog-helpers van `Ring4Sundered` (`along`, `perimeter`) naar `RingArc` (dedup).
- [worldgen] Waterscan r4 (grotopeningen onder ondiepe zee, 5a-diagnose) blijft open.
