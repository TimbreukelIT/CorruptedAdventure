# Ring 4 golf 1 — stroom W-terrein (60–63)

Scope: `worldgen`. Branch `ring4/terrain-g1` (worktree, poort n = 1: server 25571, RCON 25581), basis `main` 7ffbfeb. Contract: `contract.md` §1 en §4; golf: `golf-1.md`; werkwijze: `waves.md` §2, §4, §5. Eén commit per pakket (`worldgen:`).

## Architectuur (alle pakketten)

Het ring 4-terrein is één Java-veld, zoals ring 3: **`IslandField`** (type `island_field`, JSON `ring_4/overworld/islands.json`) wordt het veld van het hele ring 4-overworldterrein. Het rekent per kolom (`ColumnTable`, per thread rond de chunk) en per cel (`Decisions`, eenmaal per wereld):
- `Ring4Relief` (60): het basisterrein uit de regio-assen (exact gelezen, nooit via een `flat_cache`), de band (`Ring4Band`) en eigen ruis;
- de eilanden met hun gaten, meren en vallen (61), de klif 3→4 (62) en `sundered_reach` (63).

Uitgangen via een nieuw type **`island_output`** (`IslandOutput`, patroon `volcanic_output`): per kolom `height` (zonder gaten), `surface` (met gaten), `initial_height`, `island_mask`, `island_hole`, `cliff`, `sundered`; per blok `thin` (+1 eilandblok, −1 open (bron), 0 = het terrein beslist) en `cave_lift`. De gedeelde DF's uit contract §4 (`height`, `island_mask`, `island_hole`, `cliff`, `sundered`) worden `flat_cache`'s van die uitgangen.

De dichtheid `ring_4/overworld.json` volgt het ring 3-schema: `range_choice` op `thin` (eilanden exact per blok, buiten de interpolatie) → anders de geïnterpoleerde stapel `0,125 · (surface − y)` + vanilla-grotten (+ `cave_lift`), met de bekende slides. `initial_height` = ondergrens van `surface` rond de quart (−2), per wereld onthouden zoals ring 3 (valkuil Y 55: de preliminary surface moet ook in gatmeren en de zee kloppen, anders krijgen meren lucht of grotten de verkeerde vloeistof).

De publieke records (`Island`, `Lift`, `Waterfall`, `WorldTree`) en accessors van 59 blijven; de volledige eilandvorm zit in een interne record.

## Pakket 60 — basisterrein
- Laag rond Y 63; water ~50/50 (gemeten: 49 %, mythisch 49 %, bedorven 48 %, testseed).
- Landvorm per biome op de drempels van W-biomes (Q1 = B, afgestemd met `r4-biomes`): conventie region_water HOOG = open water, region_growth HOOG = begroeid, band < 0 mythisch. Mythisch: growth < −0,075 → `crystal_lagoon` (water ≥ 0: zandbanken/atollen tot Y ~64, troggen tot Y 42) | `mist_fjords` (water < 0: plateau Y ~71–103, fjordgeulen tot Y 40); growth ≥ −0,075 → `lotus_shallows` (water ≥ 0,125: ondiep Y ~58–62) | `glowroot_swamp` (±0,125: rond Y 63) | `cascade_vale` (< −0,125: treden van 6 met steile stootborden, tot Y ~110). Mythische helft van de bandovergang (`mist_fjords`): lage fjordplateau-rand, nooit lagune. Bedorven: water ≥ 0,125 → `drowned_forest` (Y ~51–59); daaronder growth ≥ 0 `reed_marsh` (rond Y 63), < 0 `peat_moors` (vlak Y ~65–68 met zinkgaten, 1 per cel van 40 met kans 0,5, straal 3–7, diepte 7–16). Overvloeien ±0,045 (~25 blokken) rond elke drempel; mythisch ↔ bedorven over band −0,3..0,3.
- Rivieren (Q3-A): geribde ruis (−9), bed Y 57 (~11 breed + oevers), buiten open water; op zeeniveau, bevaarbaar.
- `initial_height`: laagste `surface` op het midden en twee ringen (6, 12) rond de quart, zinkgaten erbij, −2; per wereld onthouden. Test: ondergrens van elke kolom van de quart.
- Surface vanilla (W-biomes).
- Gemeten (testseed): surface 40–112, 6 % boven Y 85.

## Pakket 61 — eilanden
Twee pure stappen (`IslandPlacement`):
1. **Plaatsing per cel** (128): verwacht aantal 0,25 + 3,25 · p^1,5 per cel (p = ringvoortgang; max 5, overlap binnen een cel alleen gestapeld met 6 blokken ruimte). Soort: reus (alleen p > 0,3, kans 1,5 % + 6 % · p), middel (40 % + 20 % · p), anders kluit; diameters 5–15 / 20–60 / 80–150 (Q4 = A), uitrekking tot 1,4 (reuzen 1,15). Kluitdikte 0,7 / 0,55 / 0,45 × diameter. Onderkant minstens 15 boven de hoogste grond onder het eiland: binnen 15–40, buiten tot ~200 (top ≤ 290). Alleen in ring 4 zelf (eiland + gat + band volledig buiten de zones; 62/63 doen de zones). `groundReach` (gat, meerband, grottenband, uitloop) ≤ `cell_size` (notitie #6, getest).
2. **Details per eiland** (lui, per wereld onthouden): meerpeil boven zee = laagste surface van het 4-blokraster in een band 2–14 blokken buiten de gatrand, −1 (dat raster is wat het terrein interpoleert, dus naast de meerband ligt nergens open ruimte onder het water); vallen (middel 1–2, reus 2–3) en ~1 op 4 een lift.
- **Vorm** (`IslandShape`): rafelige ellips (`Volcano.ragged`), bijna vlakke top, onderkant = kluit met lobben (`Volcano.underside`) maar overal minstens de gescheurde rand (¼ dikte, max 5): een steile zijkant i.p.v. een scherpe wig. Exact per blok via `thin` (max 8 eilanden per kolom).
- **Gat** = spiegelbeeld: omtrek ×1,12 met eigen rafeling, diepte = kluitdiepte vanaf de grond van het midden, bodem nooit onder Y 0; als aftrek in `surface`. Water: zee (≤ 63) of eigen meer (water-tak, stil water) met `cave_lift` 2 eronder en ernaast (vanilla-grotten dicht) en carvers die die band niet snijden.
- **Uitloop** (Q3-A): laag gat (grond 63–75, water op zeeniveau) krijgt een geul (bed Y 59, 5 breed, oevers) naar het dichtstbijzijnde open water binnen 60 blokken (16 richtingen).
- **Watervallen**: bron = blok onder de top van de buitenste eilandkolom in een windrichting (eiland erboven, eronder en aan drie kanten), val in de kolom ernaast tot het water (die moet onder water staan; geen ander eiland ertussen); de aquifer zet de bron als stromende bron, de val als vallend water (alleen de onderste 2 stromen); **max 3 vallen per chunk** (eerste op x, z — onafhankelijk van de volgorde). `waterfallsWithin`.
- **Liften**: kolom 2 blokken buiten de rand, van het water tot 2 boven de top, vrij van eilanden, niet bij een val. `liftsWithin`.
- `island_mask`, `island_hole` als `island_output`; de haak in `RingBiomeSource` onthoudt per thread de eilanden van de laatste kolom (notitie #5).
- Gemeten (testseed, wig x 5.900–8.100, z ±1.400): 438 eilanden (binnenste derde 60, buitenste 243, 14 reuzen), hoogste top binnen 156 / buiten 259, 278 vallen, 56 liften, 20 meren boven zee.
- Terrasvallen (Q3-A "vallen van terrasranden"): niet in 61 — het terrein is per 4 blokken geïnterpoleerd, dus een terrasrand is nooit verticaal; een val van een rand landt op de helling en stroomt uit (water tegen lucht). Komt in 62 samen met de klifvallen, die hetzelfde probleem hebben (exacte wand per blok).
- Teststop 5-pre: `/ca terrainmap`-PNG van ring 4.

## Pakket 62 — klif 3→4 (gebouwd; test en waterscan bij integratie)
- **Lijn** op blend 0,9 van zone 3→4 (effectieve afstand 6.051; ring 4-helft), ±3 blokken gerafeld met de detailruis. De ring 4-invoer volgt in de zone tot de lijn een plateau op ring 3-hoogte (`plateau`: het `VolcanicField` van ring 3 via het nieuwe optionele codecveld `plateau` van `island_field`), 40–80 boven de voet geklemd, en valt daar naar het ring 4-terrein; `ring_blend` weegt ring 4 op de lijn 97 %, dus het plateau is ring 3-terrein (`steam_fens`) en de voet echte ring 4.
- **Exacte wand**: binnen 6 blokken van de lijn per blok via `thin` (binnen gesteente tot de plateautop, buiten open vanaf de voet), zodat de klif verticaal staat ondanks het 4-blokraster.
- **Gaten** (`Ring4Cliff.GAP_CELL` 280, kans 0,8): half **kloof** (10–14 breed, bodem van het plateau 48 blokken binnen de lijn naar de voet) en half **puinhelling** (30–50 breed, helling van 30 binnen tot 12 buiten de lijn); daar geen exacte wand.
- **Dampende watervallen** (`FALL_CELL` 160, kans 0,6): bron één blok onder de plateautop in de kolom vóór de lijn (plateau eromheen), val in de kolom erbuiten tot Y 63 in een **plonsbekken** (straal 6, bed Y 56, zee); via de water-tak en `fallsOfChunk` (cap 3/chunk samen met de eilandvallen). Ring 4-helft, dus `ring4FallColumn` geldt (notitie #8: geen herziening nodig). Stoom/deeltjes: A/W-biomes.
- `cliff` 0..1 (wand en puinhelling) als `island_output`. `ring3Seal` ongemoeid. Geen terrasvallen (besluit A).

## Pakket 63 — `sundered_reach`
- Zone 4→5 (256): de grond breekt op in brokken boven een afgrond met een stilstaande zee op Y 0; landbruggen om de ~500–800 blokken; alleen vastgelegde vallen; `sundered` 0..1. Afhankelijk van Q2.
- **Gebouwd:** `Ring4Sundered` (zone-sterkte 0 aan de binnenrand, vol vanaf 55 % van de zone; chunks van 36 blokken met scheuren die breder worden (half 0,5 → 9); losse chunks vanaf 25–95 % sterkte, zwevend op een offset en dikte 8–30; landbruggen om de ~650 blokken, ±100, 8–12 breed). Koppeling: scheur-kolommen krijgen de bodem op Y −20 (`ABYSS_FLOOR`), losse chunks een vaste solid-interval boven hun bodem; `Column.abyssSeaTop()` = Y 0 voor afgrondkolommen, en de aquifer vult daar open ruimte tot Y 0 met stilstaand water (geen flow) en lucht erboven; `sundered` als `island_output`.
- **Afwijkingen (eerste versie):** geen aparte rand (`RIM`) bij de binnenkant (de zone begint met sterkte 0, dus geen bruuske afgrond); de afgrondkolommen zijn in het buitenste deel van de zone ruim 75 % (test: 0,77) — te veel, mogelijk bijstellen (chunkgrootte of schaal van de scheuren); de losse chunks zijn blokkig (vlakke bodem van de chunk); geen vallen in de afgrond (de water-tak van 61 vult alleen de eilanden); waterscan en terreinkaart bij integratie.
- Tests: `Ring4SunderedTest` (sterkte over de zone, bruggen 450–850, afgrondaandeel en zee op Y 0, niets buiten de zone, losse solids).

## Code-review 60 (c463272), verwerkt
1. Rivieren en fjorden als dal i.p.v. sleuf: de lijnruis wordt een afstand in blokken (mediaanhelling 0,0018/blok, gemeten en getest); rivier: bed 5,5 breed per kant, dan hellingen 0,6 (het dal verbreedt met de hoogte van het land; mediaan +6,9 op 17,5 blokken van het midden); fjord: bodem 12 per kant, wanden 2,0.
2. `sinkholeFloorNear`: alleen zinkgaten die het veen echt snijdt (`peat > 0`) en waarvan de kom binnen de ringen (12) valt.
3. `overworld.json`: grotingangen mogen het oppervlak breken zoals vanilla: `min(terrain, 5 · entrances + cave_lift)` (de ring 3-sluiting eruit).
4. Kleine vormen: zinkgaten exact per blok (`thin` −1 in de kom, niet meer in de 2D-`surface`; `initial_height` telt ze via `sinkholeFloorNear`); terrastreden 8 hoog, 80 % trede (breed genoeg voor het 4-blokraster; waar het land steiler is dan één trede per 4 blokken wordt het een helling).
5. `IslandField.islandColumn` (alleen eilandwaarden) voor de aquifer en `island_mask`; `column` blijft alles.
6. Warp één keer per kolom.
7. `thin.json`, `cave_lift.json` in `cache_once`.
8. (61) `island_mask`/`island_hole` aangesloten.
Gemeten na de fixes: water 54 % (mythisch 49, bedorven 63). Niet opgepakt: dedup `IslandOutput`/`VolcanicOutput` (backlog INT).

## Code-review 61 (7414ec5), verwerkt
1. Zee off-by-one: het hoogste zeewaterblok is Y 62; zee-vallen beginnen op 63 (`Fall.bottom`), liften op 62; alleen het onderste valblok stroomt (`fallFlowBelow = bottom`); een eigen meer alleen als zijn water boven 62 komt (`lakeTop` ≥ 64). Test: elke zeeval heeft bottom 63 en landt in water.
2. Meerwater alleen in de kom: `RingAquifer` vult van net onder het geïnterpoleerde oppervlak (4 celhoeken, zoals `groundAt` van ring 3) tot `lakeTop`; open ruimte dieper (een grot) blijft vanilla. Test: 6 onder de bodem geen meerwater.
3. Meer per kolom: in een gat het meer van dat gat (de laagste bij overlap; een zee-gat heeft er geen), alleen in meerbanden het laagste meer; de rand van een meer wordt gemeten vanaf de gatrand (incl. andere gaten) en onder zinkgatbodems in de buurt. Vallen/liften niet in het gat van een ander eiland. Test: geen kolom in een gat met een hoger meer dan dat gat.
4. Overlap over cellen: per cel eerst kandidaten (`candidates`), dan valt een kandidaat weg als hij een kandidaat van een buurcel raakt die zwaarder weegt (`OUTRANK`: soort, grootte, midden); puur. Test: geen enkel paar eilanden groeit in elkaar.
5. `RingBiomeSource.LastColumn`: geen `ThreadLocal` meer; een kleine lock-vrije cache (`KeyedSlots`), opnieuw per veld.
6. `IslandField`: `Neighbourhood`/`reliefScratch` via `ThreadOwned` weg; buurtlijsten in `KeyedSlots` (lock-vrij, gedeeld), relief-samples per aanroep.
Gemeten na de fixes: 414 eilanden (binnen 58 / buiten 230, 13 reuzen), 224 vallen, 42 liften, 28 meren boven zee.

## Code-review 61b (fixes 7414ec5..129557f), verwerkt
1. Grotingangen: `min(terrain, 5 · entrances + 4 · cave_lift)` — buiten meren open zoals vanilla, in de band rond een meer boven zee (lift 2 → +8) dicht.
2. Geen lucht onder meerwater: in de band rond een meer boven zee (`caveLiftFrom..To`) is alle open ruimte onder het geïnterpoleerde oppervlak (−1) gesteente (aquifer → null); het meer vult de kom erboven. Test: onder de bodem en op de bandbodem gesteente.
3. Gestapelde gaten: een eigen meer boven zee alleen als geen ander gat in de buurt van zijn gat + grottenband komt (`alone`); per kolom dus hooguit één meer, nooit naast een lager gat. Test: elk meer ligt alleen.
4. Plaatsing over cellen op afstand 2 (een eiland reikt ≤ ~97, twee ≤ 2 cellen); test over 4 seeds.
5. Greedy op rang: een kandidaat valt alleen weg voor een zwaardere kandidaat die zelf blijft (`kept`, recursie stijgt in rang; per wereld onthouden).
6. Zinkgaten voor lezers: `surfaceUncached` (meerrand, onder-water-check, `initial_height`) en `Column.lowestSurface()` nemen de zinkgatbodem mee; de terreinstapel (`surface`) niet (die snijdt ze per blok).
7. `RingAquifer.load` leest alle eilandkolomvelden vóór `lakeSurface`.
8. `LastColumn` leegt zijn cache zonder veld.
9. `KeyedSlots.key` in `IslandField`; `Decisions.once` voor alle memo's.

## Client/server-impact
Alles worldgen op de server; geen client-class, geen payload, geen `PROTOCOL_VERSION`. Nieuwe DF-types worden alleen in `ModDensityFunctionTypes` (blok `// ring 4 — W-terrein`) toegevoegd.

## Determinisme en prestatie
- Puur per seed: ruis via de noise-registry (`forkPositional` per sleutel), celbeslissingen uit de hash-ruis en de cel, nooit via een `flat_cache`; geen `Random` zonder seed. Unit test: veld twee keer gebouwd (en een kopie via `mapAll`) geeft dezelfde kolommen en cellen, ook in omgekeerde volgorde gevraagd.
- Kolommen lui per onderdeel (eilanden apart van het reliëf), zodat de per-blok-uitgang `thin` geen reliëfruis rekent. Buiten ring 4 en zijn zones rekent niets (`RingBlend`-gates). Indicatief ≤ 1,75× vanilla.

## Tests (licht, per pakket)
`./gradlew build`, `./gradlew runData` + `git diff --stat src/generated` (verwacht leeg), unit tests: reliëf (waterfractie, hoogtebereik per stijl, rivieren onder Y 63), determinisme, JSON-structuur, eilanden (dichtheid naar buiten, straal ≤ cel, gaten onder elk eiland, meerpeil < rand), water-tak met echte vallen in `RingAquiferTest`, haak. Alleen na 61 een korte terrainmap.

## Vragen aan team-lead (2026-10-13)
Q1 terrein ↔ biome-indeling op de assen; Q2 zone 4→5 volledig uit ring 4 (`RingBlend`); Q3 rivieren op zeeniveau; Q4 eilandmaten als diameter. Antwoorden (team-lead 2026-10-13): **Q1 = B** (W-biomes is eigenaar van de drempels, terrein volgt; conventie afgestemd met `r4-biomes`), **Q2 = B** (menging ring 4 ↔ ring 5 over zone 4→5 blijft, geen `RingBlend`-wijziging; ring 4 bouwt de hele zone, ring 5 stemt af), **Q3 = A**, **Q4 = A**.

## Voortgang
- 2026-10-13 19:45: branch aangemaakt, plan geschreven, vragen Q1–Q4 verstuurd.
- 2026-10-13 20:10: 60 gebouwd (build, runData groen; `src/generated` ongewijzigd), commit c463272.
- 2026-10-13 20:28: 61 gebouwd (build 776 tests, runData groen; `src/generated` ongewijzigd).

## CODEBASE-delta
**60**
- `worldgen/densityfunction/IslandField` (type `island_field`) is nu het hele ring 4-overworldterrein: codec `hash`, `relief` (`Ring4Relief`), `settings`; ringlayout via `Supplier<RingLayout>` (`RingLayout::current`; tests eigen layout); kolom (`Column`: `height`, `surface`, `river`, `fjord`, `islandMask`, `islandHole`, valbereik) lui gevuld; `initialHeight` (per wereld op het 4-blokraster), `resolve`/`resolveIfBound` (door holders, markers en ring gates), `salt`.
- Nieuw `worldgen/densityfunction/Ring4Relief`: basisterrein ring 4 uit de klimaatassen (exact), band en eigen ruis; drempels van W-biomes (`WATER_SPLIT` 0,125, `MYTHIC_GROWTH` −0,075, `BLIGHTED_GROWTH` 0, `BLEND` 0,045); landvormen lagune, lotus, moeras, terrassen, fjordplateau + fjorden, diep water, rietmoeras, veen + zinkgaten; rivieren op zeeniveau.
- Nieuw `worldgen/densityfunction/IslandOutput` (type `island_output`, `ModDensityFunctionTypes.ISLAND_OUTPUT` in het blok `// ring 4 — W-terrein`): uitgangen `height`, `surface`, `initial_height`, `island_mask`, `island_hole`, `river`, `fjord`, `thin`, `cave_lift`.
- JSON `density_function/ring_4/overworld.json` (stapel als ring 3: `thin` per blok, daarna geïnterpoleerd `terrain` met vanilla-grotten + `cave_lift`); `ring_4/overworld/{islands, height, surface, terrain, thin, cave_lift, initial_height}.json`; noises `ring_4_{hills, detail, shoals, rivers, fjords, warp}`.
- Tests: `Ring4ReliefTest` (nieuw), `IslandFieldTest` (testveld uit de JSON, `field()` publiek; determinisme), `Ring4JsonTest`, `RingAquiferTest` (testveld).

**61**
- Nieuw `worldgen/densityfunction/IslandShape` (eilandvorm + gat + uitloop) en `IslandPlacement` (plaatsing per cel, details per eiland: meer, vallen, lift).
- `IslandField`: cellen houden `IslandShape`s (publiek nog steeds `Island`-views); `shapesNear` (3 × 3 cellen, per thread), `details`, `fallsOfChunk` (cap 3), `surfaceUncached`/`baseHeight`; `Column` + `springY`, `lakeTop`, `caveLiftFrom/To`, eilandintervallen; `thin`, `caveLift`, `liftsWithin`, `waterfallsWithin` echt; `initialHeight` telt gatbodems mee; records `Fall`, `Details`.
- `RingAquifer`: water-tak met bron (stromend) en meer boven zee (stil water), carvers niet in de meerband.
- `RingBiomeSource`: `LastColumn` (één eilandopzoeking per kolom per thread).
- JSON `ring_4/overworld/island_mask.json`, `island_hole.json` → `island_output`.
- Tests: `Ring4IslandsTest` (nieuw), `RingAquiferTest.ring4FallsSpringsAndLakesComeFromTheIslandField`, `IslandFieldTest`, `Ring4JsonTest`, `Ring4ReliefTest`.

**62**
- Nieuw `worldgen/densityfunction/Ring4Cliff` (lijn, plateau, gaten, valcellen, oppervlak over de lijn).
- `IslandField`: optioneel codecveld `plateau` (DF-verwijzing, `ring_3/overworld/field`); `cliffed` in de surface, exacte wand in `thin`, klifvallen (`cliffFall`, per wereld) in `fallsOfChunk` en de bronkolommen, plonsbekkens ook in `initial_height`; `Column.cliff()`.
- `IslandOutput.CLIFF`; JSON `ring_4/overworld/cliff.json` → `island_output cliff`; `islands.json` + `"plateau"`.
- `Ring4Relief.detailAt`.
- Tests: `Ring4CliffTest` (nieuw), `Ring4JsonTest`, `IslandFieldTest` (testveld met ring 3-plateau).
- Plateau per ray: de voet wordt gemeten op de lijn op dezelfde straal (`plateauAt`); de voet naast de wand loopt over 12 blokken terug naar plateau − 40 (ring 4-heuvels verder weg mogen de wand niet tot helling maken).
- Waterscan (water tegen lucht buiten de vastgelegde vallen) en een terreinkaart van de klif: na integratie (server-test, niet nu).

## Beslissingen
- 2026-10-14 | worldgen | Afgrondzee op Y 0 alleen in de zone 4→5 (per kolom, aquifer); geen vrij water tegen lucht buiten de vastgelegde vallen, de zee-oppervlakte is de enige plas | Contract §4 (63); zee-oppervlak op één hoogte is het stilstaande water, geen val.
- 2026-10-13 | worldgen | Ring 4-terrein volgt de biome-drempels van W-biomes (region_water hoog = open water, region_growth hoog = begroeid, band < 0 mythisch; water ±0,125/0, growth −0,075/0); de mythische helft van de bandovergang (`mist_fjords`) krijgt een lage fjordplateau-rand | Q1 = B: één indeling voor biome en landvorm, eigenaar W-biomes.
- 2026-10-13 | worldgen | Zone 4→5: de menging ring 4 ↔ ring 5 blijft (geen `RingBlend`-wijziging); ring 4 bouwt de hele zone, ring 5 stemt zijn invoer af | Q2 = B.
- 2026-10-13 | worldgen | Rivieren op zeeniveau (bevaarbaar); vallen van terrasranden via de water-tak; gatmeren met uitloop | Q3 = A.
- 2026-10-13 | worldgen | Eilandmaten (kluiten 5–15, middel 20–60, reuzen 80–150) zijn diameters | Q4 = A.
- 2026-10-13 | worldgen | Geen terrasvallen in het terrein; W-biomes zet in 75 eventueel kleine bronnen/watervalletjes op terrasranden als feature (de waterscan telt ze apart); terreinvallen blijven eilanden (61) en klif 3→4 (62) | Besluit gebruiker (A): een val van een geïnterpoleerde terrasrand landt droog en stroomt uit.

## Backlog
- [infra] `IslandOutput`/`IslandField.resolve` dupliceren het patroon van `VolcanicOutput`/`VolcanicField.resolve` (review 60; ring 3 is vergrendeld) → samen met de generieke `FieldLookup` (review 59 #9).
- [worldgen] Zone 4→5: de buitenkant van de afgrond mengt tot ring 5 met de placeholder-grond van ring 5 (Q2 = B); ring 5 (81–99) stemt zijn invoer af op `sundered_reach`.
