# Case study: structure engine (voorbereiding fase 3)

Scope: `structures` (alleen onderzoek en documentatie: geen code, geen beslissingen). Status: **sparringronde 1 gedaan** (zie §7). Datum: 2026-10-23.

Doel van de engine: één duidelijke manier om structures te maken die **gedetailleerd** zijn, **diepte** hebben en er
als geheel goed uitzien, niet als een in elkaar gegooid zooitje. Deze case study kijkt wat Claude daar nu wel en niet
bij kan, welke gereedschappen het gat dichten, en stelt een architectuur en een proefproject voor.

---

## 1. Wat Claude nu wel en niet kan

### Sterk
- **Systemen en data.** Jigsaw-pools, processors, structure sets, een eigen `StructurePlacement`, eigen
  `Structure`-types en tests: goed te bouwen en te onderhouden.
- **Procedurele vormen in code.** Bogen, koepels, spitsen, gewelven, spiralen, verval volgens een ruis: alles wat met
  wiskunde te beschrijven is, wordt nauwkeurig en deterministisch. Voorbeeld uit de repo: `tools/worldgen/Ring3Templates.java`
  (fossielen, titanen, asafgietsels als `.nbt` uit vaste seeds, byte-identiek per run).
- **Regels consequent toepassen.** Een stijlregel ("muren altijd twee lagen", "dak steekt 1 uit") houdt Claude over
  honderd varianten vol, waar een mens slordig wordt.
- **Controleren.** Zwevende blokken, onbereikbare deuren, donkere hoeken waar mobs spawnen, lekkende vloeistof,
  symmetriefouten: dat laat zich automatisch toetsen.
- **Kijken naar screenshots.** Claude kan beelden lezen en grove fouten zien (vlakke muren, kale daken, kapotte
  aansluiting op het terrein, verkeerde kleuren).

### Zwak
- **Vrij 3D-componeren blok voor blok.** Een gedetailleerd gebouw als een lange lijst coördinaten schrijven gaat mis:
  Claude heeft geen ruimtelijk "gevoel", fouten stapelen zich op en het kost veel tokens. Dit is de belangrijkste
  beperking.
- **Fijne esthetiek beoordelen.** Eén screenshot zegt weinig over een interieur, over de diepte van een gevel of over
  hoe het eruitziet in het spel met licht en mist. Een ervaren bouwer ziet meer.
- **Interactief rondvliegen.** Claude kan niet in realtime rondkijken. Elke blik is een losse actie (teleporteren,
  screenshot maken, lezen), en het starten van een client duurt minuten.

### Gevolg voor het ontwerp
Claude moet structures niet als losse blokken beschrijven, maar als **bedoeling op een hoger niveau** ("toren, 3
verdiepingen, ring 3-palet, verval 0,4, ingang op het zuiden"). De engine vertaalt dat naar blokken volgens vaste
bouwregels. Wat er goed uitziet, ligt dan vast in **de regels van de engine** en niet in elke structure apart. Daarnaast
is een **snelle manier om te kijken** nodig: zonder kijken geen kwaliteit.

---

## 2. Wat er al is (geen nieuwe add-ins nodig)

| Gereedschap | Waar | Nut voor structures |
|---|---|---|
| NBT-generator, alleen JDK | `tools/worldgen/Ring3Templates.java` | het patroon voor het schrijven van `.nbt`-sjablonen |
| Server + RCON | `tools/test/server.sh`, `py/rcon.py` | `/place template`, `/place structure`, `/tp` met kijkrichting |
| Client met quick play + screenshot | `tools/test/client.sh start` / `shot` / `key F1` | **ogen**: echte weergave van het spel (Windows; niet in de cloud) |
| Python met numpy en Pillow | `tools/test/py/` | beeldbewerking, contactsheets, offline renders |
| Terreinkaarten en vaste plekken | `/ca terrainmap`, `/ca castles`, `Ring5Castles`, `NetherRing4Terrain.fossils` | plekken waar structures moeten komen |

Er bestaat dus al een volledige kijklus: server starten, sjabloon plaatsen, speler met kijkrichting teleporteren, HUD
uit, screenshot maken en lezen. Wat nog ontbreekt is een vaste opstelling ("fotostudio") en snelheid.

---

## 3. Add-ins: wat helpt om het volle potentieel te halen

| # | Add-in | Wat het oplost | Kosten / risico | Advies |
|---|---|---|---|---|
| A | **Fotostudio-commando** (eigen dev-commando + script): sjabloon op een leeg platform, vaste camerastandpunten (4 iso-hoeken, voorkant, bovenaanzicht, doorsnede per verdieping), dag en nacht, contactsheet als één PNG | Claude ziet een structure van alle kanten in één beeld; vergelijkingen vóór en na | klein; server-side commando + bestaande `client.sh`; dev-only | **ja, eerst** |
| B | **Offline renderer** (Python: `.nbt` → isometrische PNG met de echte blocktextures uit de client-jar) | iteratie in seconden zonder JVM; werkt ook in de cloud | middel (~1 dag); minder getrouw (geen licht, geen mist, simpele modellen) | **ja**, voor de snelle lus; de studio (A) blijft de eindcontrole |
| C | **WorldEdit 7.3.x (NeoForge 1.21.1)** alleen als dev-runtime (nooit meegeleverd) | jij (of een bouwer) kunt met de hand bouwen en bijwerken; grote selecties, `//copy`, `//schem`; daarna omzetten naar `.nbt` | klein; `runtimeOnly`-dependency in de dev-run; schematic → NBT-omzetter nodig | **ja, als je zelf wilt bouwen** |
| D | Vanilla structure blocks | sjablonen opslaan zonder add-in | limiet 48×48×48 per blok; prima voor losse jigsaw-stukken | al beschikbaar |
| E | Beoordeling door een tweede agent (reviewer zonder de bedoeling van de bouwer, met een vaste checklist) | minder "ziet er goed uit"-zelfbevestiging | tokens | ja, bij hero-structures |
| F | Referentiebeelden (screenshots van bouwwerken die je mooi vindt, per ring) | geeft Claude een maatstaf voor stijl | jouw tijd | **sterk aanbevolen** |

Niet nodig of niet beschikbaar: Litematica en Axiom (alleen Fabric), MCP-servers voor Minecraft (op basis van
mineflayer; die kennen de registries van onze mod niet), aparte NBT-bibliotheken (de JDK-aanpak werkt al).

---

## 4. Voorstel voor de architectuur (ter bespreking)

Drie lagen, van bedoeling naar wereld:

### Laag 1: bouwstenen (authoring, tijdens de build)
Een Java-DSL die **componenten** kent in plaats van blokken:
- *Massa*: `Volume`, `Footprint` (rechthoek, L, cirkel, polygoon), verdiepingen.
- *Elementen*: `Wall`, `Floor`, `Roof` (zadel, schild, koepel, spits, plat met kantelen), `Arch`, `Column`, `Buttress`,
  `Stair`, `Window`, `Door`, `Balcony`.
- *Detailregels* (de "diepte"): muren minstens twee lagen waar ze te zien zijn; zuilen en lijsten 1 blok uit de gevel;
  ramen 1 blok terug; dakoverstek; overgangen met trappen, platen en muurtjes; micro-detail met trapdoors, knoppen,
  kettingen en lantaarns; geen vlakke muren langer dan N blokken zonder onderbreking.
- *Paletten per ring*: rollen (hoofd, secundair, accent, rand, vloer, dak) met **gerichte overgangen** (onderaan
  vuiler, bovenaan verweerd) in plaats van willekeurige spikkels.
- *Verval*: één parameter (0 = intact, 1 = ruïne) die blokken weghaalt, verweert en puin neerlegt volgens regels
  (dragende delen blijven het langst staan).
- *Interieurs*: kamersjablonen per functie (bibliotheek, smidse, crypte, wachtpost) met rekwisieten en lootplekken.

Uitvoer: `.nbt`-sjablonen plus jigsaw-pools. Twee mogelijke plekken (open punt V2):
1. Single-file tool in `tools/structures/` (alleen JDK, zoals `Ring3Templates`): snel, geen Minecraft-classes, maar
   blocknamen worden pas in het spel gecontroleerd.
2. Datagen-provider in `runData`: echte `BlockState`s en `StructureTemplate`, alles gecontroleerd tijdens de build,
   uitvoer in `src/generated/` (past bij "datagen voor assets").

### Laag 2: variatie tijdens worldgen
- Vanilla-jigsaw voor samenstelling (stukken, verbindingen, `pool_aliases` voor varianten, maximaal 20 diep en 128
  blokken van het midden).
- Eigen **processors**: verwering per ring, as of mos, verval per plek, en later (fase 5) *corruptie*. Dezelfde
  processorketen kan de corruptie van de wereld dragen.
- `terrain_adaptation` (`beard_thin`, `beard_box`, `bury`, `encapsulate`) plus eigen funderingen tot op de grond.

### Laag 3: plaatsing
- Eigen `StructurePlacement` voor vaste posities (bewakers: 4 per grens; kastelen: 4 in ring 5; Nether-1 fortress
  gegarandeerd), dezelfde berekening voor kompas en `/locate` (zie `01-techniek.md`).
- Eigen `Structure`-types waar het terrein leidt: klifnissen in ring 2, ribbenkasten in Nether-4, pinakels en bruggen
  in ring 5. De plekken komen uit de bestaande velden (`Ring5Castles`, `NetherRing4Terrain.fossils`, `canyon_details`).

### Controles (automatisch, in tests)
Geen zwevende blokken (26-buren, zie de les van change 30c); elke deur en elke kamer bereikbaar; geen spawnbare
donkere plekken binnen (tenzij bedoeld); geen vloeistoflekken; het sjabloon past binnen de jigsaw-limieten; vaste seed
geeft dezelfde bytes.

---

## 5. Proefproject (de eigenlijke case study)

Eén kleine maar echte structure door de hele keten halen, om te meten wat werkt:

1. Studio (A) en offline renderer (B) bouwen.
2. Een **eerste versie zonder engine** (Claude schrijft de blokken rechtstreeks) als nulmeting.
3. Dezelfde structure **met een minimale engine** (massa, muren met diepte, dak, palet, verval).
4. Beide in de studio fotograferen; jij beoordeelt. Meten: aantal rondes tot "goed", tokens, welke fouten Claude zelf
   op de screenshots zag en welke niet.
5. Uitkomst: welke componenten de engine echt nodig heeft, en of de kijklus genoeg is of dat met de hand bouwen (C)
   nodig blijft voor hero-structures.

Kandidaat-onderwerpen (open punt V3): een uitkijkpost op een tafelberg in ring 2, een verdroogde put met overkapping
in ring 3, een verzonken kapel in ring 4, of één bewakersstructure.

Dit proefproject is werk van fase 3 vooruit. Het mag alleen starten na jouw akkoord (CLAUDE.md §5).

---

## 6. Open punten voor de sparring

- **V1 Werkverdeling:** bouwt alles procedureel via Claude, of bouw jij (of een bouwer) de hero-structures met de hand
  en maakt Claude de engine, de varianten en de rest?
- **V2 Plek van de authoring-laag:** single-file tool (JDK) of datagen-provider.
- **V3 Proefonderwerp** voor de case study.
- **V4 Add-ins:** welke van A–F we installeren of bouwen.
- **V5 Stijlreferenties:** lever jij per ring referentiebeelden aan?
- **V6 Wanneer:** proefproject nu (vóór teststop 5c / het afronden van ring 5) of pas bij de start van fase 3.

---

## 7. Besluiten sparringronde 1 (2026-10-23)

| # | Besluit | Gevolg |
|---|---|---|
| V1 | **Alles procedureel**: Claude maakt alle structures via de engine, de gebruiker beoordeelt op screenshots | geen handwerk; de kwaliteit hangt volledig af van de bouwregels van de engine en van de kijklus. Hero-structures (bewakers, kastelen) vragen dus een rijkere engine en meer review-rondes |
| V4 | Add-ins: **fotostudio-commando (A)** en **reviewer-agent (E)** | geen offline renderer (B) en geen WorldEdit (C): elke blik loopt via de echte client (alleen Windows, een client-slot, enkele minuten per start; meerdere sjablonen per sessie fotograferen) |
| V3 | Proefonderwerp: **verzonken kapel in ring 4** (`drowned_forest`) | toetst gewelven, ramen, interieur, water en verval; meer rondes dan een kleine structure |
| V6 | **Nu, parallel** aan ring 5 | de studio is een `infra`-change en de proef een `structures`-change, los van de ring 5-stromen; ze delen de testmachine (slots/lock uit `tools/test/README.md`) |

Nog open: V2 (single-file tool of datagen-provider; komt bij de bouw van de minimale engine), V5 (stijlreferenties;
zonder referenties gaat de reviewer uit van de ring 4-sfeer uit `docs/plans/ring-4/` en de bouwregels in §4).

Volgende stappen:
1. **S0 (`infra`)**: fotostudio. Een dev-commando `/ca studio` (server-side) plaatst een sjabloon op een leeg platform
   en geeft de camerastandpunten; een script fotografeert die via `client.sh` en plakt ze tot één contactsheet.
2. **S1 (`structures`)**: korte sparring over de kapel (grootte, staat, wat er binnen is), daarna de nulmeting zonder
   engine.
3. **S2 (`structures`)**: minimale engine en dezelfde kapel opnieuw; vergelijken met een reviewer-agent en jouw oordeel.

### Besluiten S0 fotostudio (2026-10-23)

| # | Besluit |
|---|---|
| S0-1 | Plek: een **eigen dev-dimensie** `corruptedadventure:studio` (leeg, vaste tijd, geen weer, neutrale lucht) uit een datapack dat alleen in `run/` staat en **nooit in de mod-jar** zit; plus een modus **in-situ** (`here`): de structure op het echte terrein, om de aansluiting te toetsen |
| S0-2 | Camera: **volledige set**: 4 iso-hoeken, 4 gevels, een bovenaanzicht, een doorsnede per verdieping (de lagen erboven tijdelijk weg), overdag; 's nachts de 4 iso-hoeken (licht en spawnplekken); alles op één contactsheet met labels |
| S0-3 | Inhoud: losse **`.nbt`-sjablonen** én **geregistreerde (jigsaw-)structures**, met een vaste seed |
