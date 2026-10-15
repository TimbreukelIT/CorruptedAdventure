# Teststop 5a2 — `verdant_falls` + heuvelfix (ring 4, verse wereld `ring4-vf`)

Checklist voor de gebruiker. Basis: main na de integratie van de ronde `verdant_falls` (`verdant-falls.md` §5).
Wereld: seed `20260930`, preset `corrupted_world`, **altijd vers** (oude werelden hebben een zone van 128).
Wat er beoordeeld wordt: `verdant-falls.md` §7. Dit document is alleen de uitvoering.

Coördinaten: offline berekend uit het terreinveld op de testseed (`IslandFieldTest`-layout, standaardringen).
INT controleert ze in `ring4-vf`. Wijkt een plek af, ga dan uit van wat je ter plaatse ziet en noteer de F3-coördinaat.
In-game gecontroleerd op 8233e40 (wereld `ring4-val`, 2026-10-15): alle bronnen (3.3–3.5, 3.9), de padopeningen van
kloof 14/26/40/53, de lipvallen en de voetrivier (6097 0, 2092 5755) staan waar beschreven; gecorrigeerd zijn de biomewissel
(3.1), het meer op trede 72 (3.3) en de klimstrengen van kloof 53 (3.9).

## 1. Server starten

Zoals bij 5a. INT zet het speel-lock `C:/Temp/ca-user-test.lock`, dus de `tools/test`-scripts weigeren te starten.
Start de server zelf met Gradle vanuit de checkout die INT opgeeft (na de merge: main). In PowerShell:

```powershell
cd "P:\Projecten\Java\The Corrupted Adventure"
.\gradlew.bat runServer --no-daemon
```

- `run/server.properties`: `level-name=ring4-vf`, `level-seed=20260930`,
  `level-type=corruptedadventure\:corrupted_world`, `server-port=25577`, `online-mode=false`, RCON uit.
- Wacht op `Done (…s)!` in de console.
- Verbinden: Multiplayer → Direct verbinden → `127.0.0.1:25577`.
- Eerste keer in de server-console: `op <jouw naam>`. De `/ca`-commando's vereisen op-niveau 2.
- Daarna in de chat: `/gamemode creative`. In survival kick de server je als je na een `/tp` in de lucht hangt.
- Stoppen: `stop` in de server-console.

## 2. Hulpmiddelen

| Commando | Wat |
|---|---|
| `/tp @s x y z` | naar een plek hieronder. Y ligt 1–3 boven de grond; zit je vast, vlieg dan omhoog |
| `/ca ring` | ring en blend van je positie (rand `verdant_falls` ≈ 0,23, middellijn 0,5) |
| `/ca tpboundary 3` | grens 3\|4 langs de richting vanaf (0,0); sta eerst in die richting (`/tp @s 100 120 0` = oost) |
| `/ca falls x0 z0 x1 z1` | geplande vallen in een vak → `<wereld>/ca_falls/<vak>.tsv`. Stroomt er water op een plek die er niet in staat, dan is dat een lek |
| `/ca terrainmap x z r` | bovenaanzicht → `<wereld>/corruptedadventure/maps/` |
| `/ca terrainmap section x1 z1 x2 z2` | dwarsprofiel, dezelfde map |

F3 toont het biome. Noteer bij elke bevinding de F3-coördinaat.

## 3. Plekken

### 3.1 Bovenrand `steam_fens` → `verdant_falls`
`/tp @s 5947 111 0` (oost, u 40, nog `steam_fens`)
- **Wat je ziet:** de `steam_fens`-strook met geisers en kookpoelen. Na ~20 blokken naar oost volgt het vlakke boventerras op Y ~110, met de biomewissel rond 5974 0 (in-game: 5972 nog `steam_fens`, 5976 `verdant_falls`; de trede valt bij 5980 naar 104).
- **Let op:**
  - Wisselt het biome op de vlakke bovenste trede, en niet midden op een helling?
  - Loopt de strook (~60 breed) mooi over in het terras, zonder naad of richel?
  - Is er nergens een rechte wand rondom (de oude klif)?

### 3.2 Afkoelplek (binnenste ~70 van `verdant_falls`)
`/tp @s 6003 97 0` (u 100: het ring 3-deel, maar al `verdant_falls`)
- **Wat je ziet:** een droge trede op Y 96.
- **Let op:**
  - Toont de conditie-HUD afkoelen in plaats van hitte (Q2)?
  - Plaats water met een emmer. Blijft het liggen en verdampt het niet?
  - Spons: wordt een natte spons hier niet droog?
  - Loop naar ~6030 0 (middellijn): wisselt daar het weer van ring 3 naar ring 4?

### 3.3 De afdaling met getrapte bassins (zuid, stroom 202)
`/tp @s -294 100 5925` → loop naar het zuiden, langs x ≈ −300, omlaag.

| Trede (grond) | Plek (x y z) | Wat |
|---|---|---|
| 88 (bovenste) | meer rond −294 86 5959 | meer met bronpoel; val bij bron −292 86 5975, tweede uitloop bij −310 86 5984 |
| 80 | bron −292 78 6004 | plonspoel + beek in een geul; geen meer |
| 72 | meer rond −299 70 6020 | smal meer (4–8 breed, slingert van −296 6002 naar −314 6028; op −303 6020 zelf ligt gras); val bij bron −303 70 6036 naar de voetrivier (63) |

- **Wat je ziet:** treden van 8 met afgeronde stootborden, meren tegen het stootbord aan, calciet op de drempels en rond elke val, beken als stil water in geulen, en vallen die van meer naar meer storten.
- **Let op:**
  - Is elk meer ingesloten? Loopt er nergens water weg?
  - Landt elke val in water (meer, plonspoel of rivier)?
  - Kom je te voet omlaag zonder te bouwen?
  - **Omhoog:** op ~6 % van de plekken ligt de buur 2 blokken hoger. Moet je vaak springen of schuin lopen? Hinderlijk of prima?
  - **Smalle meren:** bovenaan zijn de treden smal, dus de meren daar zijn soms maar 5–8 breed. Mooi, of rommelig?
  - Ligt het calciet goed, of is het te veel of te weinig?

Andere afdalingen om te vergelijken:
- noord, stroom 594: `/tp @s 275 82 -5991` (meer op trede 80, tweede uitloop bij bron 263 78 −6001);
- oost, stroom 0: zie 3.4.

### 3.4 Lang meer met tweede uitloop (oost, stroom 0)
`/tp @s 6056 81 25` (meer op trede 80, ~36 lang)
- **Wat je ziet:** één meer met twee bronnen aan de rand: de hoofdval bij 6067 78 23 en het spoor bij 6064 78 38. Beide vallen storten in een eigen poel op trede 72.
- **Let op:**
  - Zie je de tweede uitloop als een eigen val, met een eigen poel eronder?
  - Valt het meer niet droog tussen de twee uitlopen?
- Vergelijk: zuid `/tp @s -158 69 6023` (stroom 199, meer op trede 72, ~44 lang, sporen bij −158 70 6049 en −142 70 6055).

### 3.5 Bassinnis met tunnel
`/tp @s 6054 81 74` (boven de bron 6054 78 74)
- **Wat je ziet:** een val. Daarachter zit een nis op waterhoogte van de poel eronder. Uit de nis loopt een tunnel van 3 × 3 schuin omhoog naar de trede erboven.
- **Let op:**
  - Is de nis vanaf de poel te vinden (achter het water door)?
  - Is de tunnel beloopbaar (1 op 2)?
  - Staat er nergens water naast lucht in de nis?

### 3.6 Brug over een beek
`/tp @s 6063 82 18`
- **Wat je ziet:** een rotsbrug van 3 breed over een beekgeul, met een overspanning van 4–8.
- **Let op:** is de brug als brug herkenbaar? Is hij te laag, te dik of te kort?

### 3.7 Voetrivier, gewone plek
`/tp @s 6097 66 0` (oost, rivier op u 224, ~14 breed)
- **Wat je ziet:** de rivier op zeeniveau (Y 63) aan de voet van de afdaling, met een zandoever aan de buitenkant.
- **Let op:**
  - Is hij bevaarbaar? Vaar een stuk met een boot.
  - Is de buitenoever een helling naar ring 4, zonder wand?
  - Monden de ring 4-rivieren erin uit?

### 3.8 Voetrivier die naar binnen schuift
`/tp @s 2102 85 5780` (zuid-zuidoost; de buitenoever op het reviewpunt, rivier bij 2092 5755)
- **Wat je ziet:** ring 4 ligt hier hoog aan de zonerand, dus de rivier schuift naar binnen (u 201 i.p.v. 224). Tussen de rivier en de rand ligt een helling van hooguit 0,8 per blok.
- Ook: `/tp @s 1220 66 -5956` (noord, de meest naar binnen geschoven plek, u 175).
- **Let op:**
  - Is de oever een natuurlijke, beloopbare helling (geen wand)?
  - **Stootborden < 8:** de afdaling is hier korter, dus de stootborden zijn smaller (min. 6,3 i.p.v. 8,3). Ziet dat er nog afgerond uit, of te steil en hoekig?

### 3.9 Hoefijzerkloven
| Kloof | Midden | Rand / meer / pad | `/tp` (pad achter de achterste val) |
|---|---|---|---|
| 53 oost | 5946 −224 | 112 / 86 / 100 | `/tp @s 5893 100 -223` |
| 14 zuid | −629 6004 | 96 / 70 / 84 | `/tp @s -625 84 5968` |
| 26 west | −5983 446 | 96 / 70 / 84 | `/tp @s -5946 84 443` |
| 40 noord | 85 −6034 | 96 / 70 / 84 | `/tp @s 84 84 -5986` |

Randmeren: 53 bij 5886 −223 (water Y 110–111), 14 bij −625 5961 (water Y 94–95). Verder ligt er om de ~700 blokken langs de rand een kloof.

**Kloof 53 in detail:**
- **Richelpad achter de vallen:** start bij `/tp @s 5893 100 -223`. Loop het pad rond de hele boog; het water hangt vóór je.
- **Nis:** volg het pad naar de val bij 5919 −288 (bron 5919 110 −289). Achter die val zit een nis van 5–8 diep.
- **Klimroute van knop naar knop:**
  - `/tp @s 5897 88 -251` (in het kloofmeer, bij de wand);
  - de richel heeft zijn onderkant op Y 91 (bovenkant 93);
  - de knop van de richel op 5895 95 −249: de streng eronder hangt tot het water;
  - klim omhoog en stap opzij op de richel;
  - boven de richel hangt een knop vóór de padopening (Y 102); de streng daaronder leidt het pad in (Y 100);
  - tweede route: `/tp @s 5897 88 -213` (knop 5896 95 −210).
  - In-game (`ring4-val`): onder beide knoppen (5895 95 −249 en 5896 95 −210) hangt **geen** streng; tot het water is het lucht (kans 0,5 per richel, backlog). Bij kloof 14 hangen wel strengen onder de knoppen.
- **Getrapte uitloop:** de lipval bij bron 5940 86 −255 stort naar het meer op trede 80; daarna volgen meren tot de voetrivier.
- **Hellingen langs de armen:** op de ene arm loopt het pad 1 op 2 omlaag naar de oever, op de andere 1 op 2 omhoog naar de rand.

**Kloof 14:**
- klimroute: `/tp @s -589 72 5991` (richel met onderkant 75, knop −589 79 5988, pad 84);
- nis achter de val bij bron −582 94 5991;
- lipval naar zee bij −600 70 6047.

**Let op:**
- Oogt het amfitheater met ~12 vallen (6–19 per kloof) als Iguazu? Te vol, te leeg?
- Loop je echt achter het water langs? Is het pad (3 breed, 4 hoog) ruim genoeg?
- **Klimroute:** kom je van het water tot in het pad zonder te bouwen?
  - De strengen staan er met kans 0,5 per richel (backlog), dus er kan er een missen. Hoe vaak mis je er een?
- **Wandhoogte:** de kloofwanden zijn bijna altijd 24 hoog (44 van 45 kloven, één van 32). Genoeg, of wil je meer variatie of diepere kloven?
- **Eén helling per arm:** de ene arm gaat omlaag naar de oever, de andere omhoog naar de rand. Klopt dat, of wil je op beide armen beide richtingen?
- Ruwe wanden met een afbrokkelende bovenrand: natuurlijk genoeg?

### 3.10 Rotsboog en uitloopgeul
Deze worden nog gebouwd. **TODO-coördinaten (INT vult in):**
- Rotsboog over de uitloop van een kloof: `/tp @s TODO`
- Uitloopgeul: `/tp @s TODO`
- **Let op:** TODO (INT vult in na de bouw).

### 3.11 Heuvels van ring 4 (heuvelfix)
| Wat | `/tp` |
|---|---|
| cascade-treden, kern | `/tp @s 2171 91 6759`, `/tp @s 5193 85 4105` |
| cascade-treden vlak naast de zone | `/tp @s 301 91 6292` |
| cascade aan de biomerand (mengzone) | `/tp @s -5574 83 3570`, `/tp @s -6120 76 1937` |
| fjordrand | `/tp @s 6522 93 2804`, `/tp @s 4052 74 5926`, `/tp @s 7079 74 925` |

Hoogtes zonder eilanden gerekend: staat er een eiland op de plek, vlieg dan omhoog.
`/locate biome corruptedadventure:cascade_vale` en `…:mist_fjords` vinden er meer.
- **Let op:**
  - **Treden:** zijn de randen en voeten afgerond? Lopen de traplijnen golvend? Zijn de treden vlak, ook aan de biomerand (geen kantelende treden)?
  - **Fjorden:** zijn de randen zonder zaagtand? Zijn de plateaus egaler? Zijn de kliffen nog steil (2:1)?

### 3.12 Sfeer
- `/tp @s 5893 100 -223` (kloof 53, veel vallen): nevel (`waterfall_mist`), het geluid van verre watervallen, vogels en kikkers.
- `/tp @s 6056 81 25` (meer, oost): kikkers bij het meer.
- `/tp @s 6110 66 0` (zandoever langs de voetrivier): schildpadden.
- **Let op:**
  - Lucht, mist en water turquoise? Mooi, of te fel?
  - Is de nevel te zien, maar niet storend?
  - Is het geluid niet te luid of te eentonig?
  - Zijn er kikkers en schildpadden, en geen papegaaien?
  - Zie je aan weerskanten van de middellijn ring 3-weer en ring 4-mist?

## 4. Overig (kort)
- **Water:** sta ~1 minuut stil bij een kloof en bij een bassin. Komt er nergens nieuw stromend water naast de vallen?
- **Nether:** `/ca tpboundary 3 nether` → is de trede 3\|4 dicht (geen lavawand)?
- **Portal:** bouw er een in `verdant_falls`, ga naar de Nether en terug.
- **Waterscan en kaarten:** INT voegt hier de tabel (b34*, vf*, vg*) en de kaarten toe, zoals in 5a §3.
  - INT-noot: stroom 8 (`vfe`) en 401 (`vfw`) uit `tools/test/areas/ring4.txt` bestaan sinds deel 2 niet meer; ze wijken voor een kloof.
  - Vervangers met meren: oost stroom 0 (rond 6056 25) en west stroom 394/395 (rond −5928 −86).

## 5. Terugmelden

Per plek één blok, kort:

```
Plek: 3.x <naam>          F3: x y z
Klopt:     …
Mis/lelijk: …
Vraag(en): <antwoord op de "let op"-vragen>
Screenshot: ja/nee
```

Tot slot: drie dingen die het meest moeten veranderen, en of `verdant_falls` "af" voelt voor fase 1.
Het speel-lock blijft staan tot je de teststop afrondt.
