# Teststop 5b — overleving + sfeer (ring 4 golf 2, verse wereld `ring4-5b`)

Checklist voor de gebruiker. Basis: main na integratie 2 van ring 4 (`ring4/int2`, `golf-2.md` §3).
Wereld: seed `20260930`, preset `corrupted_world`, **altijd vers** (golf 2 verandert terrein, tunnels, flora en de
vlakke zee; oude werelden hebben die niet). Wat er beoordeeld wordt: `waves.md` §2 "Teststop 5b" + `golf-2.md` §3.
Bij voorkeur **dedicated server + 2 clients** (gas, opwaartse stroom en sfeer op beide clients).

Coördinaten: uit het terreinveld op de testseed (`IslandFieldTest`-layout), `/locate biome` en de offline scans van INT2
(`tools/test/areas/int2.txt`, wereld `int2-ws`); in-game gecontroleerd in `ring4-wave2`: de portals, de grenzen en het
Nether-fossiel. Wijkt een plek af, ga dan uit van wat je ter plaatse ziet en noteer de F3-coördinaat.

**INT2-bevindingen §4.1–4.3 zijn beslist en opgelost (liften, zuigmodder, bomen; main `9c234799`).

## 1. Server starten

Zoals bij 5a2. INT zet het speel-lock `C:/Temp/ca-user-test.lock`. Start de server met Gradle vanuit main:

```powershell
cd "P:\Projecten\Java\The Corrupted Adventure"
.\gradlew.bat runServer --no-daemon
```

- `run/server.properties`: `level-name=ring4-5b`, `level-seed=20260930`,
  `level-type=corruptedadventure\:corrupted_world`, `server-port=25577`, `online-mode=false`, RCON uit.
- Wacht op `Done (…s)!`. Verbinden: Multiplayer → Direct verbinden → `127.0.0.1:25577` (tweede client idem).
- Server-console: `op <naam>` (ook voor de tweede speler). `/gamemode creative` om te reizen, **`/gamemode survival`
  voor gif, gas, modder en schade** (creatief krijgt geen schade).
- Stoppen: `stop` in de server-console.

## 2. Hulpmiddelen

| Commando | Wat |
|---|---|
| `/tp @s x y z` | naar een plek hieronder (Y 1–3 boven de grond; zit je vast, vlieg omhoog) |
| `/ca condition get @s` | meters (gif, hitte, kou) met de huidige bron (`pool`, `gas`, `rain`, `clean`, …) |
| `/ca condition set @s poison 90` | gifmeter zetten (zuiverend, drempels) |
| `/ca weather 4 dense_fog` (`storm_wind`, `toxic_rain`, `clear`) `[seconden]` | ring 4-weer zetten (in de overworld) |
| `/time set night` / `/time set day` | nachtkoor, nachtgloed, vuurvliegjes |
| `/gamerule mobGriefing false` | gasontploffing zonder blokschade (het gas brandt nog wel door) |
| `/ca ring` | ring en blend van je positie |
| `/ca tpring 4`, `/ca tpring 4 nether`, `/ca tpboundary 4` | naar ring 4, Nether-4, grens 4\|5 |
| `/ca falls x0 z0 x1 z1` | geplande vallen in een vak (lekzoeker) |
| `/ca terrainmap x z r [schaal]` | kaart → `<wereld>/corruptedadventure/maps/` (ring 4 en Nether-4 nu met vaste kleuren) |

F3 toont het biome. Noteer bij elke bevinding de F3-coördinaat.

## 3. Plekken

### 3.0 Checklist (één plek per onderdeel; details in 3.1–3.13)

| Onderdeel | Eerste `/tp` | § |
|---|---|---|
| Gifpoel + gifmeter (survival) | `/tp @s 71 67 7124` | 3.1 |
| Moerasgas (in de gashal, daarna ontsteken) | `/tp @s -342 17 7249` | 3.1, 3.2 |
| Zuigmodder | `/tp @s -123 64 7365` | 3.3 |
| Opwaartse stroom (handmatig geplaatst) | `/fill` uit 3.4 bij `7010 64 10` | 3.4 |
| Weer (mist, storm, gifregen) | `/tp @s 7000 90 0` | 3.5 |
| Tunnelnet overstroomd (koepel) | `/tp @s 6625 10 -337` | 3.6 |
| Tunnelnet droog (poelhal, sporen) | `/tp @s -430 24 7239` | 3.6 |
| Gifkristal (droge tunnels) | `/tp @s -436 22 7248` | 3.6 |
| Wereldboom | `/tp @s 6436 230 2270` | 3.7 |
| Hemelmeer, rotsboog, ranken | `/tp @s 206 185 7087`, `/tp @s 7345 125 187`, `/tp @s 115 175 6832` | 3.8 |
| Flora mythisch (moeras, lelies, bloesem) | `/tp @s -7221 66 253`, `/tp @s 6932 66 -3`, `/tp @s -7041 70 -60` | 3.9 |
| Flora bedorven (veen, riet, dode stammen) | `/tp @s 64 66 7128`, `/tp @s -418 66 7370`, `/tp @s -127 66 7096` | 3.9 |
| Sfeer en zone 4→5 | `/tp @s 7832 90 -480` (dag en nacht) | 3.9, 3.10 |
| Flora `verdant_falls` + afdaling | `/tp @s 5893 100 -223`, `/tp @s 6056 81 25` | 3.11 |
| Nether-4 (zuiverend, fossiel, geode, zuilen) | `/execute in minecraft:the_nether run tp @s 1699 60 -429` | 3.12 |
| Aquamarijn-geode onder water | `/tp @s -19 20 7000` | 3.13 |

### 3.1 Gif: bronnen, drempels, remedies (survival)
- **Gifpoel aan het oppervlak** (`peat_moors`): `/tp @s 71 67 7124` (kom gifslijk, "Toxic Sludge"/"Gifslijk", r 1–3). Ook `-59 63 7786` (bij zone 4→5).
- **Gifpoel ondergronds** (poelhal in het droge tunnelnet): `/tp @s -430 24 7239` (hal r ~8, gif tot Y 17). Zuid: `-353 9 -7206`.
- **Moerasgas** (kamer vol gas): `/tp @s -342 17 7249` (midden van de gashal: je hoofd zit in het gas, gele deeltjes). Gasbellen onder de veenkorst: graaf in `peat_moors` rond `64 63 7128`.
- **Gifregen:** `/ca weather 4 toxic_rain` in de bedorven band (bv. bij `64 70 7128`).
- **Let op:**
  - Stijgt de meter naast de poel (`pool`, ~+3/s) en met het hoofd in gas (`gas`, ~+2/s)? Niet naast `sucking_mud`, `slime_trail`, gifzwammen of gifkristal (`clean`)?
  - Drempels: wat merk je bij 25, 50, 75 en 100 (effecten, wither)? Werken melk en honing?
  - Water (meren, zee, regen buiten de gifregen) blijft veilig?

### 3.2 Moerasgas ontsteken (geen lag-spike)
`/tp @s -342 17 7249` (midden van de gashal, r ~10; creatief, vlieg naar de tunnelmond aan de rand). Groter: `/tp @s -293 7 -7359` (zuid).
- Eerst `/gamerule mobGriefing false`, daarna nog een keer met `true`.
- Steek het gas aan vanaf de rand (buiten het gas) met vuursteen, een fakkel of een vuurbal.
- **Let op:** loopt de brand als een golf door de kamer (knallen, vlammen) zonder hapering (F3: ms/tick)? Met `mobGriefing true`: hoeveel schade? Steekt een windlading het **niet** aan (besluit)? Zien beide clients hetzelfde?

### 3.3 Zuigmodder (drijfzand)
Plekken op de testseed (mud-5b; alleen in chunks die na 6abf60c2 zijn gegenereerd, dus een verse wereld): lage waterrand `/tp @s -123 64 7365`, `/tp @s -311 64 7248`, `/tp @s 55 64 7104`; hoge veenrand `/tp @s 91 68 7275` (de grootste, 12 kolommen), `/tp @s 39 68 7315`. De plek ligt op de oever zelf; sta je er niet in, kijk dan rond naar de glans. Daarna: loop ~2 minuten langs de oevers van `peat_moors` (bv. vanaf `-123 64 7365` oost- of westwaarts) en tel hoeveel plekken je tegenkomt (verwacht een paar).
- **Let op:** zak je ~1 blok per 2 s weg? Hoofd boven: kom je er springend uit? Hoofd onder: ~3 s adem, dan 2 HP/s; brengt blijven springen je eruit? Herken je de modder (glans, bel, slurpgeluid)? Peaceful/easy: geen dood (easy stopt op 10 HP). Een mob (`/summon cow`) zakt ook weg; items zinken niet.

### 3.4 Opwaartse stroom (lift)
Liftkolommen staan sinds het liftpakket (§4 punt 1) in de wereld: één kolom `updraft` 2 blokken naast de eilandrand, van het bovenste waterblok tot 2 boven de rand.
| Wat | Plek | Kolom |
|---|---|---|
| Lift uit de zee (mythische band west) | `/tp @s -6806.5 63 -883.5` | x −6807 z −884, Y 62–128 |
| Hoge lift uit de zee (bedorven band zuid) | `/tp @s -339.5 63 -7206.5` | x −340 z −7207, Y 62–214 |
| Lift uit het meer onder de wereldboom | `/tp @s 6405.5 71 2261.5` | x 6405 z 2261, Y 70–119 |
- **Let op:** ~8 blok/s omhoog, geen valschade, items gaan mee? Kom je bovenin makkelijk op het eiland (2 blokken opzij)? Herken je de kolom van een afstand (bellen, geluid)? Ziet de tweede client je soepel stijgen?

### 3.5 Weer en storm
In ring 4 (`/tp @s 7000 90 0`): `/ca weather 4 dense_fog`, `storm_wind`, `toxic_rain`, `clear`.
- **Let op:** dikke mist (zicht); storm: duw boven Y ~120 (vlieg naar Y 150) en gekraak in de stormloop; gifregen alleen in de bedorven band giftig. Op "Fast" en "Fancy".

### 3.6 Tunnelnet
| Wat | Plek | Let op |
|---|---|---|
| Luchtkoepel (hal), overstroomd net | `/tp @s 6625 10 -337` | water tot Y 8, 4–6 lucht erboven; adem halen en verder duiken |
| Topkoepels op een overstroomde rand | `6617 6 -296`, `6607 5 -250`, `6643 12 -451` | ~44 blokken zwemmen tussen twee koepels: haalbaar? |
| Ingang via een gatmeer op zeeniveau | gatmeer rond `6620 64 -261` (reuzenlelies) | de tunnel opent 3 blokken onder water in het gat (x 6613–6677, z −348…−229) |
| Prop (gesloten bandovergang) | `/tp @s 6980 30 110` | overstroomde helft x 6960–7000 z 37–114, droge helft x 6966–6999 z 108–185: ≥ 6 steen ertussen |
| Gashal en droge hal (overgang) | `6986 30 172` (gas), `7129 25 45` (droog) | gas blijft in de hal; geen water ernaast |
| Poelhal | `/tp @s -430 24 7239` | gif 2–3 diep onder de tunnelvloeren |
| Droge ingang via een veen-zinkgat | x −108…7, z 7236…7328 | de tunnel stijgt naar de bodem van een droog zinkgat (lucht tot Y 62) |
| Sporen van wezens | slijmspoor `-437 20 7218`, nest `-434 20 7259`, klauwkras `-439 21 7224`, botten `-444 14 7370` | zichtbaar zonder te overdrijven? |
| Gifkristal (droge tunnels) | budding `-436 22 7248`, cluster `-438 28 7280` | gifgrot-gevoel |
| Aquamarijn/tidesilver/jade (nat net) | budding aquamarijn `6586 4 -215` | |
- **Let op:** diepe grotten in ring 4 staan nu onder water tot Y 62 (vlakke zee, besluit). Geen watergordijnen meer (waterscan §J).

### 3.7 Wereldboom
`/tp @s 6436 230 2270` (top 213, basis 120, kruin r ~52). Andere: `6378 200 -1115`, `381 250 -6724`. Alle 15: 6436 2270, 5930 3472, 3098 5861, 1524 6234, −2344 6433, −4111 5159, −6240 2535, −6610 549, −5301 −3273, −3834 −5698, −2617 −6265, 381 −6724, 2822 −5577, 5385 −4170, 6378 −1115.
- **Let op:** maat en silhouet (stam 8–12, kale stam ~40 % onder de kruin), wortels die het eiland boven het gatmeer houden, vallen naast de wortels, rafelige kruin. Keuzes: §4 (74).

### 3.8 Hemelmeren, beken, bogen, ranken
- Hemelmeer met beek naar een val: `/tp @s 206 185 7087` (r 14, water Y 179).
- Groot hemelmeer: `/tp @s 6795 160 -95` (r 26, water Y 154). Droge kom (`withered_gardens`): `/tp @s 7002 145 83`.
- Rotsboog: `/tp @s 7345 125 187` (tussen de eilanden bij 7335 181 en 7355 193).
- Ranken van mangrovewortels tussen eilanden: rond `115 175 6832`.
- **Let op:** sluit elk meer (geen lek)? Is de boog beloopbaar? Hangen de ranken natuurlijk door?

### 3.9 Levendigheid per biome (~2 min lopen, dag en nacht)
| Biome | `/tp` | Kijk naar |
|---|---|---|
| `lotus_shallows` | `6932 66 -3` | reuzenlelies, lotus, riet |
| `glowroot_swamp` | `-7221 66 253` | reuzenmangroves met gloeizwammen |
| `mist_fjords` | `7000 80 32` | spar/den/donkere eik, lage mist |
| `crystal_lagoon` | `0 66 7000` | koraal, zeegras, geodes onder water |
| `cascade_vale` | `-7041 70 -60` | moonpetal-bomen, bloesem-particles |
| `sky_gardens` | `7032 170 0` | bomen en bloemen op hoge eilanden |
| `peat_moors` | `64 66 7128` | veenkorst, gifpoelen, dwaallichten 's nachts |
| `reed_marsh` | `-418 66 7370` | dicht riet, geritsel achter je |
| `drowned_forest` | `-127 66 7096` | verdronken kale stammen |
| `withered_gardens` | `7000 170 64` | kale bomen, dode struiken |
| `sundered_reach` | `7832 80 -480` | lege zone, drone, grijze fade, `glitch_speck` |
| `verdant_falls` | zie 3.11 | flora |
- Gloed onder eilanden: 's nachts in de schaduw van een eiland op droog land (licht ≥ 1, gloeibloemen/-zwammen).
- Dieren per biome; 's nachts nachtkoor, vuurvliegjes (moeras), dwaallichten (`peat_moors`, `drowned_forest`).

### 3.10 Spanning en zone 4→5
- Silhouet onder water (zeldzaam, ≤ 1 per ~20 min): duik in diep water (≥ 20) in `crystal_lagoon` (`0 50 7000`) en kijk vooruit.
- Bellen uit de diepte, geritsel in het riet, dwaallichten die wegdrijven.
- `/ca tpboundary 4` of `/tp @s 7832 90 -480`: lucht, fog en water worden geleidelijk grijs over de zone (tot 0,9), drone, gekraak, glitch-deeltjes. Geen flikkerende lucht (besluit Q20).

### 3.11 Flora `verdant_falls` (75b)
- Kloof 53: `/tp @s 5893 100 -223` (richelpad): lianen op de wanden (deels bloeiend), orchideeën en wandbromelia's, `verdant_water_bloom` en lotus in het kloofmeer (`5901 87 -242`).
- Afdaling oost: `/tp @s 6056 81 25`: emergente bomen aan de meeroever (vruchtpeul bij `6054 88 41`), boomvarens op de stootborden.
- **Let op:** blijven vallen, lippen, richelpad, klimroutes en de voetrivier vrij? Klim een route met lianen tot boven (100 % beklimbaar)? Papegaaien? Kleuren (pastel mint/zeegroen, perzik/abrikoos/lila) en onderscheid met `cascade_vale`.

### 3.12 Nether-4
- `/ca tpring 4 nether`. **Zuiverend:** `/ca condition set @s poison 90`, dan `/ca condition get @s`: daalt de meter ~3/s?
- **Zeewezen-fossiel:** `/execute in minecraft:the_nether run tp @s 1699 60 -429` (ribbenkast x 1652–1746, z −440…−418, bot tot Y ~72 boven een duinvloer van ~45; schedel aan de oostkant ~1735). Loop door een ribbengang naar binnen en de schedel in: vlakke vloer, gangen ≥ 4 breed, half begraven?
- **Geode:** `/execute in minecraft:the_nether run tp @s 1750 60 0` (`geode_hollows`): zuiverkristal → calciet → gebleekt netherrack, clusters op wand en koepel, niet op de vloer.
- **Kristalzuilen:** `/execute in minecraft:the_nether run tp @s 1910 90 160`: clusters op de zijkanten, niet op richels of bruggen; lampen.
- Sfeer: pulserende fog, droge wind, kristalgezoem, botstof; ver zicht.
- Portal ring 4 ↔ Nether-4 (getest door INT, zie §J); probeer er zelf een vanuit `verdant_falls`.

### 3.13 Ertsen en geodes
- Aquamarijn-geode half open onder diep water: `crystal_lagoon` rond `-19 20 7000` (budding aquamarijn op Y 15).
- Vanilla-ertsen ×0,2/0,15/0,1, tidesilver in de tunnelwanden, jade diep in het natte net.

## 4. Open punten om te beoordelen

**INT2-bevindingen (eerst beslissen):**
1. **Opgelost in 0221e49b + b075b848** (liftpakket, branch `ring4/lifts-5b`, besluit 2026-10-18 = A): feature `ring_4/island_lifts` vult elke liftkolom (geen lift binnen 6 blokken van een waterval). In vier testgebieden (mythisch, bedorven noord/zuid, wereldboommeer) staan 7/7 geplande kolommen heel en op de juiste hoogte, in beide laadvolgordes gelijk (0 verschillen binnen 10 blokken van een kolom; elders alleen de bekende erts- en boomverschillen). Prestatie `lotus`/`reed`: noise 0,88/0,94 × vanilla (§3.4). Was: **Geen opwaartse stromen in de wereld.** `IslandField.liftsWithin` kent liftposities (≈ 1 op 4 middel- en reuzeneilanden), maar geen feature zet er `updraft`-blokken in: 0 `updraft` in alle INT2-scangebieden. Opties: (A, aanbevolen) een klein `worldgen`-pakket dat de liftkolommen vult (van het water tot 2 boven de top), vóór 5b of meteen erna; (B) 5b test het blok handmatig (3.4), plaatsing in golf 3.
2. ~~**Zuigmodder is zeldzaam:** 8 blokken (één plek, `51 62 7101`) in ~4.400 gescande chunks, 0 in de gebieden met veel `peat_moors`-/`drowned_forest`-oever (tdn, tds). Genoeg (G1: kleine plekken op oevers), of dichter in 76?~~ **Opgelost in a38314ac + 6abf60c2** (branch `ring4/mud-5b`, besluit 2026-10-18: 1A dichtheid 0,25 per oeverchunk, 2B ook hoge veenranden): de feature zoekt de oever zelf in de eigen chunk. Telling (vóór → na, blokken / plekken): tdn 0 → 183 / 23, lagn 8 / 1 → 24 / 2, tmix 0 → 16 / 2, tds 0 → 0; determinisme (`det.sh`, wereld b omgekeerd) voor de modder identiek (alleen de bekende moonpetal-bomen in tds en 2 ertswissels verschillen); prestatie (`perf.sh`, 2 warme rondes) noise peat 0,99, lagoon 0,94, features 0,52/0,40 tegen vanilla (tds heeft vrijwel geen `peat_moors`/`drowned_forest`-land aan het oppervlak). Plekken: §3.3.
3. **Opgelost in d9a0dfdd** (boom-determinisme, branch `ring4/trees-det`, besluit 2026-10-18 = A): alle ring 4-bomen zijn `isolated_tree` (eigen random per boom, groeit tegen het terrein zonder andere bomen, voorrang stam > wortel > blad > mos/zwam/propagule; een liftkolom wint altijd), de gewogen grondplanten `ground_plant`. Omgekeerd geladen: 0 boomverschillen (§J). *Let op bij het spelen:* zelfde soort bomen op dezelfde plekken maar elke boom anders getekend; moerasbomen ~6 % minder (backlog). Oorspronkelijke bevinding: **Boomkruinen en determinisme** (§J): moonpetal-bomen (`cascade_vale`, `sky_gardens`, rond de wereldbomen) en reuzenmangroves (`glowroot_swamp`) krijgen een andere vorm, of staan er niet, als de chunks in een andere volgorde laden. De wereld is per seed en per laadvolgorde gelijk; alleen een andere route door de wereld geeft hier en daar een andere boom. **Oorzaak** (INT2, uit de vanilla-code): een `TreeFeature`-boom leest wat er in zijn voetafdruk staat. Staat daar al de stam van een boom uit de buurchunk (of een plant die bomen niet vervangen), dan valt er een blad, wortel of tak weg, en daarna trekt de boom een ander aantal getallen uit zijn random (hangbladeren alleen onder gezette bladeren, de wortelsimulatie van de mangrove stopt bij een vreemde stam, `glowcap`/mos alleen boven een wortel in lucht): de rest van de boom verandert. Alle bomen van één feature in een chunk delen die random, dus ook elke volgende boom in die chunk verandert mee. Kronen en wortels reiken tot ~8 blokken over de chunkgrens (de cellen van 8 houden de stammen uit elkaar, niet de kronen). Vanilla-bossen (ring 1) doen hetzelfde; 47e loste in ring 3 alleen de grondplanten op. Opties:
   - (A, aanbevolen) **fixpakket W-biomes**: elke boom een eigen random op zijn positie (stopt de doorwerking naar de volgende bomen; niet zichtbaar) én een eigen boomfeature die de vanilla-vormen tekent tegen de grond zonder andere bomen, met een vaste voorrang waar twee bomen hetzelfde blok willen (stam > wortel > blad > mos/zwam/propagule): volgorde-onafhankelijk. Middelgroot (eigen variant van `TreeFeature` + leesbeeld van het niveau); daarna determinisme en prestatie opnieuw.
   - (B) alleen de eigen random per boom: klein, vermindert de verschillen (vooral midden in een gebied), maar een boom die zelf een buurkruin raakt blijft volgorde-afhankelijk.
   - (C) bomen laten niet overlappen (grotere cellen of kleinere kronen): zichtbaar ijlere moerassen en dalen.
   - (D) accepteren als vanilla-gedrag.

**Implementatiekeuzes van golf 2 ("zo laten" of "anders: …"):**
- **74 wereldboom, meren, bogen:** 15 bomen op de binnenstraal (r 6.230–6.900), ≥ 24 van de zones; eigen eiland 52–68 breed laag boven het gat, 7–10 wortels; kruin = koepel + 6–8 takken met bloesemtrossen, rafelige schil; valcap = eilandcap; hemelmeren op eilanden met top ≥ 140 en ≥ 36 breed (beek alleen naar een bestaande val: 4 van 26); rotsboog bij ~1 op 2 geschikte paren (8 op 190).
- **W-biomes:** ranken tussen eilandparen van `mangrove_roots` (40 % dubbel); hangorchidee als accent onder de richels van `verdant_falls`; verdronken stammen 9–15 (recht) / 8–14 (gevorkt); gloed onder eilanden als vast rooster (licht ≥ 1); moerasboom = vanilla hoge mangrove een maat groter; moonpetal-boom = kersvorm; fjordbomen spar, den 40 %, donkere eik 10 %; veen als korst van `peat_moors`; gevaren dichter richting ring 5 (0,35 → 1); emergente boom stam 14–22 met schermkroon r 5–6; liaan 35 % bloeiend.
- **Blokken:** zakken 0,025/tick, springen +0,04/tick; gasketting (buren 2 ticks later, knal per ~8 blokken, max. 4 per tick); opwaartse stroom tot ~8 blok/s, ook in de lucht; groene mist in gifslijk (4 blokken).
- **Condities:** `[conditions.purifying] fallMultiplier` 3,0; zuigmodder-ritme (3 s adem, daarna 2 HP/s).
- **Textures:** de lage varen op 16 px (druk?); de schermkroon; de gloed van orchidee en bromelia 's nachts; het halfrijpe peulstadium (olijf); het reuzenblad naast de boomvarens; de glans van `sucking_mud` naast modder en veen; `bone_block`-fossielen naast `fossil_stone`-paden.
- **Afdaling `verdant_falls` (nog open uit 5a2, backlog):** *smalle meren* bovenaan de afdaling (treden ~15 breed, meren 5–8 breed en 25–40 lang; smallere drempel bij smalle treden?) en de *treden zelf*: midden op een stootbord ligt op ~6 % van de kolommen de buur 2 blokken hoger (omhoog schuin lopen of springen; optie: zachtere stootbordvorm alleen voor `verdant_falls`). Plekken: 5a2 §3.3 (`/tp @s -294 100 5925`, naar het zuiden omlaag).
- **Nether-4:** fossieldichtheid 0,75 per 256² bottenduin (geaccepteerd: hoe voelt het?); kansen bekleding (clusters 25 %, lampen 2 %), cladding (18 %, 1/150) en puin.
- **Sfeer:** volumes en klank van de 32 nieuwe geluiden; dichtheid nachtgloed, dwaallichten, bloesem, glitch; grijsfade (0,9); pulsering en zicht Nether-4; frequentie van het silhouet.

## 5. Terugmelden

Per plek één blok, kort:

```
Plek: 3.x <naam>          F3: x y z
Klopt:     …
Mis/lelijk: …
Vraag(en): <antwoord op de "let op"-vragen>
Screenshot: ja/nee
```

Tot slot: de drie INT2-bevindingen van §4, de drie dingen die het meest moeten veranderen, en per punt van §4
"zo laten" of "anders: …". Het speel-lock blijft staan tot je de teststop afrondt.

### 5.1 Feedback gebruiker (2026-10-18, eerste indruk)

- **Het hele ring 4-gebied voelt leeg en saai aan, vooral `mist_fjords`.** Overal staan vooral bomen; er is weinig aankleding of iets dat het gebied voller maakt.
- **Screenshot** `run/screenshots/2026-10-18_22.05.42.png` (overzicht van boven; de gebruiker: "toont goed aan hoe leeg en saai de ring er tot nu toe uit ziet. Hier moeten we grondig naar kijken"). Wat erop te zien is:
  - grote vlakke bruine veenvlakten (`peat_moors`) met alleen losse lichtgroene vlekjes; geen planten, riet, plassen of reliëf erop;
  - grasvlakten met moonpetal-bomen in een gelijkmatig raster, allemaal even groot; daartussen kaal gras zonder ondergroei, bloemen, rotsen of struiken;
  - de sparren in `mist_fjords` staan los op kaal gras;
  - de rivieren en oevers zijn kaal: geen riet, lelies of oeverplanten zichtbaar;
  - het terrein is overal vlak en in strepen, met weinig hoogteverschil;
  - de zwevende eilanden zijn klein en ver uit elkaar, met dunne ranken; het grote eiland heeft een kale, donkere onderkant;
  - de lucht is vlak wit en grijs, en de mist en sfeer voegen weinig toe.
- Aanpak volgende sessie: grondig. Neem per biome de huidige features en dichtheden door (bron: `Ring4MythicFeatures`, `Ring4BlightedFeatures`, `VerdantFallsFeatures`, `golf-2.md` §6b-flora). Vergelijk met de visie in `00-overzicht.md` en de ring 4-design. Maak daarna een vergelijking per biome: wat er nu staat en wat er moet komen (lagen ondergroei, rotsen, plassen, reliëf, oeverplanten, variatie in boomgrootte en -soort, meer eilanden en vulling in de lucht). Leg dat voor in een sparringronde.
- Volgende sessie: eerst een sparringsessie (CLAUDE.md §3) over aankleding en vulling per biome (te beginnen met `mist_fjords`), daarna een pakket. Niet bouwen zonder die ronde.

## J. INT2-resultaten (2026-10-16 – 2026-10-18)

Branch `ring4/int2` op main `8cc10438` (alle golf 2-stromen al vooruit gemerged). Werelden in de worktree van INT2
(`run/`, en `build/new-8cc` = 8cc10438 voor determinisme, `build/ref-preg2` = c4b97e24 als feature-index-referentie).

**B/D:** `./gradlew build` groen, `runData` + `git diff --exit-code src/generated` schoon (golf 2 was al geregenereerd).
`ModNetwork.PROTOCOL_VERSION` blijft `"8"`: golf 2 voegt geen payload toe. Op 76dacc43 (2026-10-18) opnieuw: build
groen (incl. tests), `runData` zonder diff, server-smoke op een verse wereld `int2-smoke`: 0 ERROR/Exception, geen client-class.

**S-smoke** (dedicated server + client Dev, verse wereld `ring4-wave2`): `/ca tpring 4` (5343 73 −4522), `tpring 4 nether`
(1335 45 −1131), `tpboundary 3` (6032 88 30), `tpboundary 4` (8006 61 40); portal ring 4 (7000 121 0) → Nether-4
(1754 91 16) en terug naar dezelfde portal; portal vanuit `verdant_falls` (6060 111 0) → Nether-4 (1527 46 3).
Serverlog: 0 ERROR/Exception, geen client-class-fout (de nieuwe client-mixin `BiomeColorsWaterMixin` laadt niet op de
server), alleen de bekende WARN's (ocelot-categorie, "moved too quickly", "Can't keep up"). Ook de generatielogs van
waterscan en determinisme: 0 ERROR, 0 "far chunk". (Screenshots van de client kwamen wit terug: PrintWindow-probleem, niet
gebruikt.) Kaarten met de vaste kleuren: `build/int2/maps/` (ring 4 oost, Nether-4).

**Waterscan + gifscan** (`waterscan.sh int2-ws tools/test/areas/int2.txt`, settle 10 s): **water tegen lucht buiten de
vallen 0** in alle 24 overworldgebieden (13,8 mln. waterblokken, 8.063 blootgesteld, alle in 213 vastgelegde vallen of op
het zeevlak). Ook 0 in r4e/r4w/b45in, waar 5a/vf-INT nog 162/353/159 grotwater en zee-naad vonden (vlakke zee, 73), en 0
in vf*/vg* (was 633). **Gifscan: 0** (2.931 gifslijk, 15.327 moerasgas; gif tegen lucht/gas 0, gif tegen water 0, gas
tegen water 0, stromend gifslijk 0; gas tegen lucht 1.008 = de gashallen en zinkgatlagen, by design). Die gifscan
draaide op de gesettelde wereld, waar gif dat water raakte al modder kan zijn; daarom opnieuw **bevroren**
(`waterscan.sh int2-gif <tmix, tdn, tds, lagn> --freeze`, `/tick freeze` tijdens het genereren en opslaan, op 76dacc43):
**gifscan 0** (2.890 gifslijk, 15.272 moerasgas, gif tegen lucht/water 0, gas tegen water 0, stromend 0; gas tegen lucht
1.009) en waterscan 0 buiten de vallen (2.229 blootgesteld, alle in 23 vallen). De scan telt sinds INT2 ook planten en
tapijten naast gif als open en waterlogged blokken, zeegras, kelp en bubble columns als water.

**Determinisme** (`det.sh`, 29 gebieden incl. 3 ring 3 en Nether-3/-4, wereld b omgekeerd geladen; run `int2-det-head`
op 76dacc43 = het terrein van bdea3279, 2026-10-18): **5.230/5.423 chunks identiek** (op 8cc10438: 5.227). Nether-3 256/256
en Nether-4 400/400 (fossielen, bekleding, cladding, puin) identiek; tunnelnet (tme, tmix, tdn), hemelmeren (sky, skyw*),
boog, lagune/geodes (lagn), `steam_fens` en `b45in` op hooguit 1 ertswissel na identiek. bdea3279 zelf verandert het
terrein niet: bevroren wereld op 8cc10438 tegen bdea3279 (vfe, r4e, tme, tree1, tdn) identiek op boomvormen na.
Verschillen per gebied (bdea3279 | 8cc10438):

| Gebied | Wat | bdea3279 | 8cc10438 |
|---|---|---|---|
| r4w (`cascade_vale`) | moonpetal-bomen (`own`) | 530 | 560 |
| tds (`sky_gardens` boven de bedorven band) | moonpetal-bomen (`own`) | 588 | 587 |
| tree1 (wereldboom 6.436 2.270) | moonpetal-bomen rond de wereldboom (`own`) | 958 | 1.697 |
| vfn / vfw | moonpetal-bomen | 74 / 3 | 74 / 3 |
| r4e (`lotus_shallows`/`glowroot_swamp`) | reuzenmangroves (`other`) + `glowcap` (`own`) | 1.463 + 10 | 1.486 + 10 |
| tree3 (wereldboom 381 −6.724, moeras) | reuzenmangroves + `glowcap` | 4.056 + 41 | 2.990 + 37 |
| lotusw (`glowroot_swamp` −7.221 253) | reuzenmangroves + `glowcap` | 4.368 + 41 | 5.434 + 46 |
| overige ring 4 | ertswissels tidesilver/jade ↔ vanilla-erts | 1–2 per gebied | idem |
| caldera, archipel (ring 3, bekend) | ertswissels / vloeistof bij lavavallen | 2 / 5 + 285 | 1 / 5 + 281 |

De boomverschillen zijn geen terreinfout maar volgorde-afhankelijkheid van vanilla's `TreeFeature` over chunkgrenzen
(oorzaak en opties: §4 punt 3; backlog). Buiten de bomen en ertswissels is ring 4 blok-voor-blok gelijk.

**Boom-determinisme (2026-10-18, branch `ring4/trees-det`, optie A; met de liften en de zuigmodder van main 08cc6c63):** `det.sh --areas tools/test/areas/trees.txt` (r4w, r4e, tds, tree1–3, lotusw, vfn, vfe, fjord, drowned + controles lagn, skyw1, arch4; 1.780 chunks, wereld b omgekeerd geladen), run `trees3`: **0 boomverschillen** (r4w, tds, tree1, tree3, lotusw, vfn: 0 tegen 530 / 588 / 958 / 4.056 + 41 / 4.368 + 41 / 74 in INT2), 1.768/1.780 chunks identiek, de rest alleen de bekende ertswissels (tidesilver/jade ↔ koper/diamant, 5 blokken); ook 0 met liftkolommen tussen de bomen (tds 363, tree1 50 `updraft`). Zonder `ground_plant` bleven er gras/varen-wissels over (run `trees1`, 8 `other`). Vorm (blokstatistiek tegen `int2-det-head-a`, zelfde gebieden): moonpetal-hout/-blad 0,99, spar 1,02/1,11, verdronken stammen 1,03, verdant 0,97/1,06, mangrove-hout/-blad 0,91/0,92 (183 tegen 194 moerasbomen); aantallen bomen per gebied verder gelijk. **Prestatie** (`perf.sh --only "tree lotus cascade reed"`, 3 warme rondes, run `perf-trees`): noise 0,96–1,02 (budget 1,75), features 0,50–0,77 van vanilla.

**Feature-index ring 1–3** (`fi3.txt`, branch tegen c4b97e24, beide `--freeze`): biomes overal identiek (ring 1–3,
Nether 1–3). Bovenste terreinblok: ring 1 0, z12/ring 2/ring 3 op 0–6 % van de kolommen anders (max. 11, verplaatste
keien/puin/geodes); `detcmp`: in elke overworldchunk van ring 1–3 verschuift de decoratie (ertsen, bomen, geodes, puin,
`withered_dirt`-plekken); Nether 2/3 identiek, Nether 1 alleen vanilla-features. Gevolg van de nieuwe placed features
van golf 2; niets besloten (backlog).

**Nether:** `nether-lava-walls.py` r 1.500: lava 0, wanden 0; r 2.000: lava 227 (Nether-5), **wanden 0**. Ruimte-check
(`build/int2/n4space.py`): n4e (1.750 0, 102.400 kolommen) 96,9 % beloopbaar, duinvloer p5–p95 Y 42–91, buren met stap ≤ 1
97,3 %; fossielgebied (n4fossil) 100 % beloopbaar, 97,6 % stap ≤ 1; fossiel bij 1.699 −429 (704 botkolommen, x 1.652–1.746,
bot tot Y 72 boven een duinvloer ~45, ≥ 48 tot het plafond op p5).

**Prestatie** (`perf.sh`, `perf-r4.txt`, mod tegen vanilla in één server, budget noise ≤ 1,75): run `perf-int2`
(3 warme rondes, 2026-10-16, vóór bdea3279), mediaan noise: zone 3→4 `steam_fens` 1,14, `verdant_falls` 1,09 / kloof
1,02; ring 4 0,59–0,74 (cascade 0,61, fjord 0,64, lotus 0,64, lagune 0,60, veen 0,65, verdronken 0,65, riet 0,68,
tunnels 0,59/0,74, wereldboom 0,70, `sundered_reach` 0,74); Nether-4 0,97 / 1,33 / 1,34. Run `perf-int2b` op bdea3279
(2 warme rondes; de machine was zwaar belast, de derde ronde viel uit op een fork-fout): ring 4 0,84–1,08, zone 3→4 ≤ 1,29,
Nether-4 1,36–1,45 (n4dunes spreiding 1,15–1,67). Ook Nether-4 werd trager terwijl bdea3279 daar niets raakt, dus
belasting; alles blijft onder 1,75. Na bdea3279 is geen worldgen-code meer veranderd.
